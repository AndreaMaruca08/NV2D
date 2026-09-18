package nv.core.gpu;

import nv.core.annotations.EngineCore;
import nv.core.errors.ex.EngineEx;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.lwjgl.vulkan.VK10.*;

/**
 * Small synchronous Vulkan compute facade. A dispatch binds its buffers as
 * storage buffers 0..N-1 and optionally passes a push-constant block.
 */
@EngineCore
public final class GpuCompute implements AutoCloseable {
    private final VkDevice device;
    private final VkPhysicalDevice physicalDevice;
    private final VkQueue queue;
    private final int queueFamily;
    private final long commandPool;
    private final Map<String, PipelineResources> pipelines = new HashMap<>();
    private final List<Long> pendingCommandBuffers = new ArrayList<>();
    private final List<Long> pendingDescriptorPools = new ArrayList<>();

    public GpuCompute(VkDevice device, VkPhysicalDevice physicalDevice, VkQueue queue, int queueFamily) {
        this.device = device; this.physicalDevice = physicalDevice; this.queue = queue; this.queueFamily = queueFamily;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer p = stack.mallocLong(1);
            VkCommandPoolCreateInfo info = VkCommandPoolCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                    .queueFamilyIndex(queueFamily).flags(VK_COMMAND_POOL_CREATE_TRANSIENT_BIT);
            if (vkCreateCommandPool(device, info, null, p) != VK_SUCCESS) throw new EngineEx("Unable to create GPU compute command pool");
            commandPool = p.get(0);
        }
    }

    public GpuBuffer createBuffer(long size) { return new GpuBuffer(device, physicalDevice, size, queueFamily); }

    /** Creates a device-local buffer and uploads the supplied bytes. */
    public GpuBuffer upload(ByteBuffer data) {
        if (data == null) throw new IllegalArgumentException("data cannot be null");
        GpuBuffer buffer = createBuffer(data.remaining());
        try {
            buffer.upload(data, physicalDevice, queue);
            return buffer;
        } catch (RuntimeException e) {
            buffer.close();
            throw e;
        }
    }

    /** Downloads a buffer through a temporary host-visible staging allocation. */
    public ByteBuffer download(GpuBuffer buffer) {
        if (buffer == null) throw new IllegalArgumentException("buffer cannot be null");
        return buffer.download(physicalDevice, queue);
    }

    /** Dispatches {@code shaderResource} with storage buffers bound in array order. */
    public void dispatch(String shaderResource, GpuBuffer[] buffers, ByteBuffer pushConstants, int groupsX, int groupsY, int groupsZ) {
        dispatchInternal(shaderResource, buffers, pushConstants, groupsX, groupsY, groupsZ, true);
    }

    /**
     * Submits a compute dispatch without waiting for the queue. Dispatches on
     * the same queue remain ordered, so this is useful for multi-pass filters.
     * Call {@link #synchronize()} before reading or releasing their buffers.
     */
    public synchronized void dispatchAsync(String shaderResource, GpuBuffer[] buffers, ByteBuffer pushConstants,
                                            int groupsX, int groupsY, int groupsZ) {
        dispatchInternal(shaderResource, buffers, pushConstants, groupsX, groupsY, groupsZ, false);
    }

    /** Waits for all asynchronous dispatches and releases their transient resources. */
    public synchronized void synchronize() {
        if (pendingCommandBuffers.isEmpty()) return;
        check(vkQueueWaitIdle(queue), "wait for compute queue");
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer commands = stack.mallocPointer(pendingCommandBuffers.size());
            for (long address : pendingCommandBuffers) commands.put(address);
            commands.flip();
            vkFreeCommandBuffers(device, commandPool, commands);
        }
        for (long pool : pendingDescriptorPools) vkDestroyDescriptorPool(device, pool, null);
        pendingCommandBuffers.clear();
        pendingDescriptorPools.clear();
    }

    private void dispatchInternal(String shaderResource, GpuBuffer[] buffers, ByteBuffer pushConstants,
                                   int groupsX, int groupsY, int groupsZ, boolean wait) {
        if (buffers == null || buffers.length == 0 || groupsX <= 0 || groupsY <= 0 || groupsZ <= 0)
            throw new IllegalArgumentException("Compute dispatch requires buffers and positive workgroup counts");
        byte[] code;
        try { code = readShader(shaderResource); } catch (IOException e) { throw new EngineEx("Unable to read compute shader " + shaderResource + ": " + e); }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PipelineResources resources = getPipeline(shaderResource, code, buffers.length,
                    pushConstants == null ? 0 : pushConstants.remaining());
            long pool = createDescriptorPool(buffers.length, stack);
            long set = allocateDescriptorSet(pool, resources.descriptorSetLayout, stack);
            writeDescriptors(set, buffers, stack);
            VkCommandBuffer cmd = allocateCommandBuffer(stack);
            VkCommandBufferBeginInfo begin = VkCommandBufferBeginInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                    .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
            check(vkBeginCommandBuffer(cmd, begin), "begin compute command buffer");
            vkCmdBindPipeline(cmd, VK_PIPELINE_BIND_POINT_COMPUTE, resources.pipeline);
            vkCmdBindDescriptorSets(cmd, VK_PIPELINE_BIND_POINT_COMPUTE, resources.pipelineLayout, 0, stack.longs(set), null);
            if (pushConstants != null && pushConstants.remaining() > 0)
                vkCmdPushConstants(cmd, resources.pipelineLayout, VK_SHADER_STAGE_COMPUTE_BIT, 0, pushConstants);
            vkCmdDispatch(cmd, groupsX, groupsY, groupsZ);
            check(vkEndCommandBuffer(cmd), "end compute command buffer");
            VkSubmitInfo submit = VkSubmitInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_SUBMIT_INFO).pCommandBuffers(stack.pointers(cmd.address()));
            check(vkQueueSubmit(queue, submit, VK_NULL_HANDLE), "submit compute command buffer");
            if (wait) {
                check(vkQueueWaitIdle(queue), "wait for compute queue");
                // The command pool is persistent, so command buffers must be explicitly freed.
                vkFreeCommandBuffers(device, commandPool, stack.pointers(cmd.address()));
                vkDestroyDescriptorPool(device, pool, null);
            } else {
                pendingCommandBuffers.add(cmd.address());
                pendingDescriptorPools.add(pool);
            }
        }
    }

    private synchronized PipelineResources getPipeline(String resource, byte[] code, int bindings, int pushSize) {
        String key = resource + "#" + bindings + "#" + pushSize;
        PipelineResources cached = pipelines.get(key);
        if (cached != null) return cached;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long shader = createShader(code, stack);
            long setLayout = createSetLayout(bindings, stack);
            long pipelineLayout = createPipelineLayout(setLayout, pushSize, stack);
            long pipeline = createPipeline(shader, pipelineLayout, stack);
            cached = new PipelineResources(shader, setLayout, pipelineLayout, pipeline);
            pipelines.put(key, cached);
            return cached;
        }
    }

    private VkCommandBuffer allocateCommandBuffer(MemoryStack stack) {
        PointerBuffer p = stack.mallocPointer(1);
        VkCommandBufferAllocateInfo info = VkCommandBufferAllocateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                .commandPool(commandPool).level(VK_COMMAND_BUFFER_LEVEL_PRIMARY).commandBufferCount(1);
        check(vkAllocateCommandBuffers(device, info, p), "allocate compute command buffer");
        return new VkCommandBuffer(p.get(0), device);
    }
    private long createShader(byte[] bytes, MemoryStack stack) {
        ByteBuffer code = MemoryUtil.memAlloc(bytes.length); code.put(bytes).flip();
        VkShaderModuleCreateInfo info = VkShaderModuleCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO).pCode(code);
        LongBuffer p = stack.mallocLong(1); check(vkCreateShaderModule(device, info, null, p), "create compute shader module"); MemoryUtil.memFree(code); return p.get(0);
    }
    private long createSetLayout(int count, MemoryStack stack) {
        VkDescriptorSetLayoutBinding.Buffer b = VkDescriptorSetLayoutBinding.calloc(count, stack);
        for (int i=0;i<count;i++) b.get(i).binding(i).descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER).descriptorCount(1).stageFlags(VK_SHADER_STAGE_COMPUTE_BIT);
        VkDescriptorSetLayoutCreateInfo i = VkDescriptorSetLayoutCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO).pBindings(b);
        LongBuffer p=stack.mallocLong(1); check(vkCreateDescriptorSetLayout(device,i,null,p),"create compute descriptor layout"); return p.get(0);
    }
    private long createPipelineLayout(long layout, int pushSize, MemoryStack stack) {
        VkPipelineLayoutCreateInfo i=VkPipelineLayoutCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO).pSetLayouts(stack.longs(layout));
        if(pushSize > 0) i.pPushConstantRanges(VkPushConstantRange.calloc(1,stack).stageFlags(VK_SHADER_STAGE_COMPUTE_BIT).offset(0).size(pushSize));
        LongBuffer p=stack.mallocLong(1); check(vkCreatePipelineLayout(device,i,null,p),"create compute pipeline layout"); return p.get(0);
    }
    private long createPipeline(long shader, long layout, MemoryStack stack) {
        VkPipelineShaderStageCreateInfo stage = VkPipelineShaderStageCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                .stage(VK_SHADER_STAGE_COMPUTE_BIT)
                .module(shader)
                .pName(stack.UTF8("main"));
        VkComputePipelineCreateInfo.Buffer info = VkComputePipelineCreateInfo.calloc(1, stack)
                .sType(VK_STRUCTURE_TYPE_COMPUTE_PIPELINE_CREATE_INFO)
                .stage(stage)
                .layout(layout);
        LongBuffer p = stack.mallocLong(1);
        check(vkCreateComputePipelines(device, VK_NULL_HANDLE, info, null, p), "create compute pipeline");
        return p.get(0);
    }
    private long createDescriptorPool(int count,MemoryStack s){ VkDescriptorPoolSize.Buffer z=VkDescriptorPoolSize.calloc(1,s).type(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER).descriptorCount(count); VkDescriptorPoolCreateInfo i=VkDescriptorPoolCreateInfo.calloc(s).sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO).maxSets(1).pPoolSizes(z); LongBuffer p=s.mallocLong(1); check(vkCreateDescriptorPool(device,i,null,p),"create compute descriptor pool"); return p.get(0); }
    private long allocateDescriptorSet(long pool,long layout,MemoryStack s){ LongBuffer p=s.mallocLong(1); check(vkAllocateDescriptorSets(device,VkDescriptorSetAllocateInfo.calloc(s).sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO).descriptorPool(pool).pSetLayouts(s.longs(layout)),p),"allocate compute descriptor set"); return p.get(0); }
    private void writeDescriptors(long set,GpuBuffer[] bs,MemoryStack s){ VkWriteDescriptorSet.Buffer w=VkWriteDescriptorSet.calloc(bs.length,s); VkDescriptorBufferInfo.Buffer infos=VkDescriptorBufferInfo.calloc(bs.length,s); for(int i=0;i<bs.length;i++){infos.get(i).buffer(bs[i].handle()).offset(0).range(bs[i].size()); w.get(i).sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET).dstSet(set).dstBinding(i).descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER).descriptorCount(1).pBufferInfo(infos.slice(i,1));} vkUpdateDescriptorSets(device,w,null); }
    private static byte[] readShader(String resource) throws IOException { if(resource.startsWith("/")){try(var in=GpuCompute.class.getResourceAsStream(resource)){if(in==null)throw new IOException("resource not found");return in.readAllBytes();}} return Files.readAllBytes(Paths.get(resource)); }
    static void copyBuffer(VkDevice d,VkQueue q,int family,long src,long dst,long size){try(MemoryStack s=MemoryStack.stackPush()){LongBuffer p=s.mallocLong(1); VkCommandPoolCreateInfo pi=VkCommandPoolCreateInfo.calloc(s).sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO).queueFamilyIndex(family).flags(VK_COMMAND_POOL_CREATE_TRANSIENT_BIT); check(vkCreateCommandPool(d,pi,null,p),"create copy pool");long pool=p.get(0);PointerBuffer cb=s.mallocPointer(1);check(vkAllocateCommandBuffers(d,VkCommandBufferAllocateInfo.calloc(s).sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO).commandPool(pool).level(VK_COMMAND_BUFFER_LEVEL_PRIMARY).commandBufferCount(1),cb),"allocate copy command");VkCommandBuffer c=new VkCommandBuffer(cb.get(0),d);check(vkBeginCommandBuffer(c,VkCommandBufferBeginInfo.calloc(s).sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO).flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)),"begin copy");vkCmdCopyBuffer(c,src,dst,VkBufferCopy.calloc(1,s).size(size));check(vkEndCommandBuffer(c),"end copy");check(vkQueueSubmit(q,VkSubmitInfo.calloc(s).sType(VK_STRUCTURE_TYPE_SUBMIT_INFO).pCommandBuffers(cb),VK_NULL_HANDLE),"submit copy");check(vkQueueWaitIdle(q),"wait copy");vkDestroyCommandPool(d,pool,null);}}
    private static void check(int r,String what){if(r!=VK_SUCCESS)throw new EngineEx("Unable to "+what+" (VkResult="+r+")");}
    @Override public synchronized void close(){
        synchronize();
        for (PipelineResources p : pipelines.values()) {
            vkDestroyPipeline(device, p.pipeline, null);
            vkDestroyPipelineLayout(device, p.pipelineLayout, null);
            vkDestroyDescriptorSetLayout(device, p.descriptorSetLayout, null);
            vkDestroyShaderModule(device, p.shader, null);
        }
        pipelines.clear();
        vkDestroyCommandPool(device,commandPool,null);
    }
    private record PipelineResources(long shader, long descriptorSetLayout, long pipelineLayout, long pipeline) {}
}
