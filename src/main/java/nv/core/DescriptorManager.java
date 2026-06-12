package nv.core;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import java.nio.LongBuffer;
import static org.lwjgl.vulkan.VK10.*;

/**
 * Gestisce il Descriptor Set Layout, il Descriptor Pool e i Descriptor Sets.
 * I descriptor sets sono il meccanismo con cui Vulkan lega i buffer agli shader.
 *
 * Architettura:
 *   Layout   → "firma" di cosa si aspetta lo shader (binding 0 = uniform buffer)
 *   Pool     → memoria da cui allocare i descriptor sets
 *   Sets     → istanze concrete, una per immagine swapchain
 */
public class DescriptorManager implements AutoCloseable {

    private final VkDevice device;
    private final long descriptorSetLayoutHandle;
    private final long descriptorPoolHandle;
    private final long[] descriptorSetHandles;

    public DescriptorManager(VkDevice device, OrthoUBO ubo, int imageCount) {
        this.device = device;
        this.descriptorSetHandles = new long[imageCount];

        this.descriptorSetLayoutHandle = createDescriptorSetLayout();
        this.descriptorPoolHandle      = createDescriptorPool(imageCount);
        allocateAndUpdateDescriptorSets(ubo, imageCount);
    }

    private long createDescriptorSetLayout() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Descriviamo il binding 0: un Uniform Buffer accessibile dal Vertex Shader
            VkDescriptorSetLayoutBinding.Buffer binding = VkDescriptorSetLayoutBinding.calloc(1, stack);
            binding.get(0)
                    .binding(0)
                    .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                    .descriptorCount(1)
                    .stageFlags(VK_SHADER_STAGE_VERTEX_BIT); // Solo il vertex shader legge la matrice

            VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                    .pBindings(binding);

            LongBuffer pLayout = stack.mallocLong(1);
            if (vkCreateDescriptorSetLayout(device, layoutInfo, null, pLayout) != VK_SUCCESS) {
                throw new RuntimeException("Impossibile creare il Descriptor Set Layout");
            }
            return pLayout.get(0);
        }
    }

    private long createDescriptorPool(int imageCount) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Pool dimensionato per imageCount uniform buffers
            // +1 per texture futura (combined image sampler) — evita di ricreare il pool allo Sprint 2
            VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.calloc(1, stack);
            poolSizes.get(0)
                    .type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                    .descriptorCount(imageCount);

            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                    .pPoolSizes(poolSizes)
                    .maxSets(imageCount);

            LongBuffer pPool = stack.mallocLong(1);
            if (vkCreateDescriptorPool(device, poolInfo, null, pPool) != VK_SUCCESS) {
                throw new RuntimeException("Impossibile creare il Descriptor Pool");
            }
            return pPool.get(0);
        }
    }

    private void allocateAndUpdateDescriptorSets(OrthoUBO ubo, int imageCount) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Creiamo un buffer con imageCount copie dello stesso layout
            LongBuffer layouts = stack.mallocLong(imageCount);
            for (int i = 0; i < imageCount; i++) {
                layouts.put(i, descriptorSetLayoutHandle);
            }

            VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                    .descriptorPool(descriptorPoolHandle)
                    .pSetLayouts(layouts);

            LongBuffer pSets = stack.mallocLong(imageCount);
            if (vkAllocateDescriptorSets(device, allocInfo, pSets) != VK_SUCCESS) {
                throw new RuntimeException("Impossibile allocare i Descriptor Sets");
            }

            for (int i = 0; i < imageCount; i++) {
                descriptorSetHandles[i] = pSets.get(i);

                // Colleghiamo il buffer UBO al descriptor set con vkUpdateDescriptorSets
                VkDescriptorBufferInfo.Buffer bufferInfo = VkDescriptorBufferInfo.calloc(1, stack)
                        .buffer(ubo.getBuffer(i))
                        .offset(0)
                        .range(OrthoUBO.SIZE_BYTES);

                VkWriteDescriptorSet.Buffer descriptorWrite = VkWriteDescriptorSet.calloc(1, stack);
                descriptorWrite.get(0)
                        .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                        .dstSet(descriptorSetHandles[i])
                        .dstBinding(0)
                        .dstArrayElement(0)
                        .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                        .descriptorCount(1)
                        .pBufferInfo(bufferInfo);

                vkUpdateDescriptorSets(device, descriptorWrite, null);
            }
        }
    }

    public long getDescriptorSetLayoutHandle() {
        return descriptorSetLayoutHandle;
    }

    public long getDescriptorSet(int imageIndex) {
        return descriptorSetHandles[imageIndex];
    }

    @Override
    public void close() {
        // Il pool distrugge automaticamente i sets allocati da esso
        vkDestroyDescriptorPool(device, descriptorPoolHandle, null);
        vkDestroyDescriptorSetLayout(device, descriptorSetLayoutHandle, null);
    }
}