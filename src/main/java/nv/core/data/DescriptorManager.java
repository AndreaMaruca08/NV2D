package nv.core.data;

import nv.core.OrthoUBO;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import java.nio.LongBuffer;
import static org.lwjgl.vulkan.VK10.*;

/**
 * Versione Sprint 2: gestisce due binding:
 *   binding 0 → Uniform Buffer (matrice ortografica) — vertex shader
 *   binding 1 → Combined Image Sampler (texture)     — fragment shader
 */
public class DescriptorManager implements AutoCloseable {

    private final VkDevice device;
    private final long descriptorSetLayoutHandle;
    private final long descriptorPoolHandle;
    private final long[] descriptorSetHandles;

    public DescriptorManager(VkDevice device, OrthoUBO ubo,
                             TextureImage texture, int imageCount) {
        this.device = device;
        this.descriptorSetHandles = new long[imageCount];

        this.descriptorSetLayoutHandle = createDescriptorSetLayout();
        this.descriptorPoolHandle      = createDescriptorPool(imageCount);
        allocateAndUpdateDescriptorSets(ubo, texture, imageCount);
    }

    private long createDescriptorSetLayout() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDescriptorSetLayoutBinding.Buffer bindings =
                    VkDescriptorSetLayoutBinding.calloc(2, stack);

            // binding 0: UBO — vertex shader legge la matrice ortografica
            bindings.get(0)
                    .binding(0)
                    .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                    .descriptorCount(1)
                    .stageFlags(VK_SHADER_STAGE_VERTEX_BIT);

            // binding 1: texture — fragment shader campiona il font atlas
            bindings.get(1)
                    .binding(1)
                    .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(1)
                    .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);

            VkDescriptorSetLayoutCreateInfo layoutInfo =
                    VkDescriptorSetLayoutCreateInfo.calloc(stack)
                            .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                            .pBindings(bindings);

            LongBuffer pLayout = stack.mallocLong(1);
            if (vkCreateDescriptorSetLayout(device, layoutInfo, null, pLayout) != VK_SUCCESS) {
                throw new RuntimeException("Impossibile creare il Descriptor Set Layout");
            }
            return pLayout.get(0);
        }
    }

    private long createDescriptorPool(int imageCount) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.calloc(2, stack);

            poolSizes.get(0)
                    .type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                    .descriptorCount(imageCount);

            poolSizes.get(1)
                    .type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
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

    private void allocateAndUpdateDescriptorSets(OrthoUBO ubo,
                                                 TextureImage texture,
                                                 int imageCount) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer layouts = stack.mallocLong(imageCount);
            for (int i = 0; i < imageCount; i++) layouts.put(i, descriptorSetLayoutHandle);

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

                // Write 0: UBO
                VkDescriptorBufferInfo.Buffer bufferInfo =
                        VkDescriptorBufferInfo.calloc(1, stack)
                                .buffer(ubo.getBuffer(i))
                                .offset(0)
                                .range(OrthoUBO.SIZE_BYTES);

                // Write 1: texture sampler
                VkDescriptorImageInfo.Buffer imageInfo =
                        VkDescriptorImageInfo.calloc(1, stack)
                                .imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                                .imageView(texture.getImageViewHandle())
                                .sampler(texture.getSamplerHandle());

                VkWriteDescriptorSet.Buffer writes = VkWriteDescriptorSet.calloc(2, stack);

                writes.get(0)
                        .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                        .dstSet(descriptorSetHandles[i])
                        .dstBinding(0)
                        .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                        .descriptorCount(1)
                        .pBufferInfo(bufferInfo);

                writes.get(1)
                        .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                        .dstSet(descriptorSetHandles[i])
                        .dstBinding(1)
                        .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                        .descriptorCount(1)
                        .pImageInfo(imageInfo);

                vkUpdateDescriptorSets(device, writes, null);
            }
        }
    }

    public long getDescriptorSetLayoutHandle() { return descriptorSetLayoutHandle; }
    public long getDescriptorSet(int imageIndex) { return descriptorSetHandles[imageIndex]; }

    @Override
    public void close() {
        vkDestroyDescriptorPool(device, descriptorPoolHandle, null);
        vkDestroyDescriptorSetLayout(device, descriptorSetLayoutHandle, null);
    }
}