package nv.core.data;

import nv.core.OrthoUBO;
import nv.core.annotations.EngineCore;
import nv.core.errors.ex.EngineEx;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import java.nio.LongBuffer;

import static nv.core.errors.NvLogger.logEngine;
import static org.lwjgl.vulkan.VK10.*;

/**
 * @since 1.0
 * @author Andrea Maruca
 */
@EngineCore
@SuppressWarnings("unused")
public final class DescriptorManager implements AutoCloseable {

    private final VkDevice device;
    private final long descriptorSetLayoutHandle;
    private final long descriptorPoolHandle;
    private final long[] descriptorSetHandles;
    private final TextureImage[] textures = new TextureImage[15];

    public DescriptorManager(VkDevice device, OrthoUBO ubo,
                             TextureImage fontTexture, int imageCount,
                             TextureImage[] existingTextures) {
        this.device = device;
        this.descriptorSetHandles = new long[imageCount];

        // Copia le texture esistenti prima di creare i descriptor sets
        if (existingTextures != null) {
            for (int i = 0; i < Math.min(existingTextures.length, textures.length); i++) {
                this.textures[i] = existingTextures[i];
            }
        }
        // fontTexture sovrascrive sempre slot 0
        this.textures[0] = fontTexture;

        this.descriptorSetLayoutHandle = createDescriptorSetLayout();
        this.descriptorPoolHandle      = createDescriptorPool(imageCount);
        allocateAndUpdateDescriptorSets(ubo, imageCount);
    }
    public DescriptorManager(VkDevice device, OrthoUBO ubo,
                             TextureImage fontTexture, int imageCount) {
        this(device, ubo, fontTexture, imageCount, null);
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

            // binding 1: array di 15 texture sampler — fragment shader
            bindings.get(1)
                    .binding(1)
                    .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(15)
                    .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);

            VkDescriptorSetLayoutCreateInfo layoutInfo =
                    VkDescriptorSetLayoutCreateInfo.calloc(stack)
                            .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                            .pBindings(bindings);

            LongBuffer pLayout = stack.mallocLong(1);
            if (vkCreateDescriptorSetLayout(device, layoutInfo, null, pLayout) != VK_SUCCESS) {
                throw new EngineEx("Impossible to allocate Descriptor Set Layout");
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
                    .descriptorCount(imageCount * 15);

            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                    .pPoolSizes(poolSizes)
                    .maxSets(imageCount);

            LongBuffer pPool = stack.mallocLong(1);
            if (vkCreateDescriptorPool(device, poolInfo, null, pPool) != VK_SUCCESS) {
                throw new EngineEx("Impossible to allocate Descriptor Pool");
            }
            return pPool.get(0);
        }
    }

    private void allocateAndUpdateDescriptorSets(OrthoUBO ubo,
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
                throw new EngineEx("Impossible to allocate Descriptor Sets");
            }

            for (int i = 0; i < imageCount; i++) {
                descriptorSetHandles[i] = pSets.get(i);
                updateFullDescriptorSet(i, ubo);
            }
        }
    }

    private void updateFullDescriptorSet(int imageIndex, OrthoUBO ubo) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Write 0: UBO
            VkDescriptorBufferInfo.Buffer bufferInfo =
                    VkDescriptorBufferInfo.calloc(1, stack)
                            .buffer(ubo.getBuffer(imageIndex))
                            .offset(0)
                            .range(OrthoUBO.SIZE_BYTES);

            // Write 1: Texture Array
            VkDescriptorImageInfo.Buffer imageInfos = VkDescriptorImageInfo.calloc(15, stack);
            for (int i = 0; i < 15; i++) {
                TextureImage tex = textures[i] != null ? textures[i] : textures[0];
                imageInfos.get(i)
                        .imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                        .imageView(tex.getImageViewHandle())
                        .sampler(tex.getSamplerHandle());
            }

            VkWriteDescriptorSet.Buffer writes = VkWriteDescriptorSet.calloc(2, stack);

            // UBO
            writes.get(0)
                    .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                    .dstSet(descriptorSetHandles[imageIndex])
                    .dstBinding(0)
                    .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                    .descriptorCount(1)
                    .pBufferInfo(bufferInfo);

            // Textures
            writes.get(1)
                    .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                    .dstSet(descriptorSetHandles[imageIndex])
                    .dstBinding(1)
                    .dstArrayElement(0)
                    .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(15)
                    .pImageInfo(imageInfos);

            vkUpdateDescriptorSets(device, writes, null);
        }
    }

    public int getImageCount() {
        return descriptorSetHandles.length;
    }

    /**
     * Aggiorna una texture specifica nell'array per tutti i descriptor sets.
     */
    public synchronized void updateTexture(int textureIndex, TextureImage texture) {
        if (textureIndex < 0 || textureIndex >= 15) return;
        textures[textureIndex] = texture;

        logEngine("updateTexture slot=" + textureIndex + " su " + descriptorSetHandles.length + " sets");

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.calloc(1, stack)
                    .imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                    .imageView(texture.getImageViewHandle())
                    .sampler(texture.getSamplerHandle());

            for (long descriptorSetHandle : descriptorSetHandles) {
                VkWriteDescriptorSet.Buffer write = VkWriteDescriptorSet.calloc(1, stack);
                write.get(0)
                        .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                        .dstSet(descriptorSetHandle)
                        .dstBinding(1)
                        .dstArrayElement(textureIndex) // Aggiorna solo lo slot specifico
                        .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                        .descriptorCount(1)
                        .pImageInfo(imageInfo);

                vkUpdateDescriptorSets(device, write, null);
            }
        }
    }

    public long getDescriptorSetLayoutHandle() { return descriptorSetLayoutHandle; }
    public long getDescriptorSet(int imageIndex) { return descriptorSetHandles[imageIndex]; }
    public TextureImage getCurrentTexture() { return textures[0]; }

    @Override
    public void close() {
        vkDestroyDescriptorPool(device, descriptorPoolHandle, null);
        vkDestroyDescriptorSetLayout(device, descriptorSetLayoutHandle, null);
    }
}
