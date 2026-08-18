package nv.core.data;

import nv.core.OrthoUBO;
import nv.core.annotations.EngineCore;
import nv.core.errors.ex.EngineEx;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

/**
 * Manages descriptor sets used by the 2D renderer.
 *
 * Descriptor layout:
 *
 * set = 0
 *   binding 0 -> orthographic UBO
 *   binding 1 -> array of 15 combined image samplers
 *
 * @since 1.0
 * @author Andrea Maruca
 */
@EngineCore
@SuppressWarnings("unused")
public final class DescriptorManager implements AutoCloseable {

    public static final int MAX_TEXTURES = 15;

    private final VkDevice device;

    private final long descriptorSetLayoutHandle;
    private final long descriptorPoolHandle;

    private final long[] descriptorSetHandles;
    private final TextureImage[] textures =
            new TextureImage[MAX_TEXTURES];

    public DescriptorManager(
            VkDevice device,
            OrthoUBO ubo,
            TextureImage fontTexture,
            int imageCount,
            TextureImage[] existingTextures
    ) {

        this.device = device;
        this.descriptorSetHandles =
                new long[imageCount];

        /*
         * Copy already registered textures.
         */
        if (existingTextures != null) {
            int count = Math.min(
                    existingTextures.length,
                    MAX_TEXTURES
            );

            System.arraycopy(
                    existingTextures,
                    0,
                    textures,
                    0,
                    count
            );
        }

        /*
         * Font atlas is always texture 0.
         */
        textures[0] = fontTexture;

        this.descriptorSetLayoutHandle =
                createDescriptorSetLayout();

        this.descriptorPoolHandle =
                createDescriptorPool(imageCount);

        allocateAndUpdateDescriptorSets(
                ubo,
                imageCount
        );
    }

    public DescriptorManager(
            VkDevice device,
            OrthoUBO ubo,
            TextureImage fontTexture,
            int imageCount
    ) {

        this(
                device,
                ubo,
                fontTexture,
                imageCount,
                null
        );
    }

    private long createDescriptorSetLayout() {

        try (MemoryStack stack =
                     MemoryStack.stackPush()) {

            VkDescriptorSetLayoutBinding.Buffer bindings =
                    VkDescriptorSetLayoutBinding.calloc(
                            2,
                            stack
                    );

            /*
             * binding 0:
             * orthographic projection UBO
             */
            bindings.get(0)
                    .binding(0)
                    .descriptorType(
                            VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER
                    )
                    .descriptorCount(1)
                    .stageFlags(
                            VK_SHADER_STAGE_VERTEX_BIT
                    );

            /*
             * binding 1:
             * 15 combined image samplers.
             */
            bindings.get(1)
                    .binding(1)
                    .descriptorType(
                            VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER
                    )
                    .descriptorCount(MAX_TEXTURES)
                    .stageFlags(
                            VK_SHADER_STAGE_FRAGMENT_BIT
                    );

            VkDescriptorSetLayoutCreateInfo layoutInfo =
                    VkDescriptorSetLayoutCreateInfo.calloc(stack)
                            .sType(
                                    VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO
                            )
                            .pBindings(bindings);

            LongBuffer pLayout =
                    stack.mallocLong(1);

            if (vkCreateDescriptorSetLayout(
                    device,
                    layoutInfo,
                    null,
                    pLayout
            ) != VK_SUCCESS) {

                throw new EngineEx(
                        "Impossible to create Descriptor Set Layout"
                );
            }

            return pLayout.get(0);
        }
    }

    private long createDescriptorPool(
            int imageCount
    ) {

        try (MemoryStack stack =
                     MemoryStack.stackPush()) {

            VkDescriptorPoolSize.Buffer poolSizes =
                    VkDescriptorPoolSize.calloc(
                            2,
                            stack
                    );

            /*
             * One UBO per descriptor set.
             */
            poolSizes.get(0)
                    .type(
                            VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER
                    )
                    .descriptorCount(imageCount);

            /*
             * 15 combined samplers per descriptor set.
             */
            poolSizes.get(1)
                    .type(
                            VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER
                    )
                    .descriptorCount(
                            imageCount * MAX_TEXTURES
                    );

            VkDescriptorPoolCreateInfo poolInfo =
                    VkDescriptorPoolCreateInfo.calloc(stack)
                            .sType(
                                    VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO
                            )
                            .pPoolSizes(poolSizes)
                            .maxSets(imageCount);

            LongBuffer pPool =
                    stack.mallocLong(1);

            if (vkCreateDescriptorPool(
                    device,
                    poolInfo,
                    null,
                    pPool
            ) != VK_SUCCESS) {

                throw new EngineEx(
                        "Impossible to create Descriptor Pool"
                );
            }

            return pPool.get(0);
        }
    }

    private void allocateAndUpdateDescriptorSets(
            OrthoUBO ubo,
            int imageCount
    ) {

        try (MemoryStack stack =
                     MemoryStack.stackPush()) {

            LongBuffer layouts =
                    stack.mallocLong(imageCount);

            for (int i = 0; i < imageCount; i++) {
                layouts.put(
                        i,
                        descriptorSetLayoutHandle
                );
            }

            VkDescriptorSetAllocateInfo allocInfo =
                    VkDescriptorSetAllocateInfo.calloc(stack)
                            .sType(
                                    VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO
                            )
                            .descriptorPool(
                                    descriptorPoolHandle
                            )
                            .pSetLayouts(layouts);

            LongBuffer pSets =
                    stack.mallocLong(imageCount);

            if (vkAllocateDescriptorSets(
                    device,
                    allocInfo,
                    pSets
            ) != VK_SUCCESS) {

                throw new EngineEx(
                        "Impossible to allocate Descriptor Sets"
                );
            }

            for (int i = 0; i < imageCount; i++) {

                descriptorSetHandles[i] =
                        pSets.get(i);

                updateFullDescriptorSet(
                        i,
                        ubo
                );
            }
        }
    }

    private void updateFullDescriptorSet(
            int imageIndex,
            OrthoUBO ubo
    ) {

        try (MemoryStack stack =
                     MemoryStack.stackPush()) {

            /*
             * UBO descriptor.
             */
            VkDescriptorBufferInfo.Buffer bufferInfo =
                    VkDescriptorBufferInfo.calloc(
                                    1,
                                    stack
                            )
                            .buffer(
                                    ubo.getBuffer(imageIndex)
                            )
                            .offset(0)
                            .range(
                                    OrthoUBO.SIZE_BYTES
                            );

            /*
             * Texture descriptor array.
             *
             * Empty slots point to texture 0 instead of
             * leaving an invalid descriptor.
             */
            VkDescriptorImageInfo.Buffer imageInfos =
                    VkDescriptorImageInfo.calloc(
                            MAX_TEXTURES,
                            stack
                    );

            for (int i = 0; i < MAX_TEXTURES; i++) {

                TextureImage texture =
                        textures[i] != null
                                ? textures[i]
                                : textures[0];

                imageInfos.get(i)
                        .imageLayout(
                                VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL
                        )
                        .imageView(
                                texture.getImageViewHandle()
                        )
                        .sampler(
                                texture.getSamplerHandle()
                        );
            }

            VkWriteDescriptorSet.Buffer writes =
                    VkWriteDescriptorSet.calloc(
                            2,
                            stack
                    );

            /*
             * UBO.
             */
            writes.get(0)
                    .sType(
                            VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET
                    )
                    .dstSet(
                            descriptorSetHandles[imageIndex]
                    )
                    .dstBinding(0)
                    .descriptorType(
                            VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER
                    )
                    .descriptorCount(1)
                    .pBufferInfo(bufferInfo);

            /*
             * Texture array.
             */
            writes.get(1)
                    .sType(
                            VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET
                    )
                    .dstSet(
                            descriptorSetHandles[imageIndex]
                    )
                    .dstBinding(1)
                    .dstArrayElement(0)
                    .descriptorType(
                            VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER
                    )
                    .descriptorCount(MAX_TEXTURES)
                    .pImageInfo(imageInfos);

            vkUpdateDescriptorSets(
                    device,
                    writes,
                    null
            );
        }
    }

    /**
     * Updates a single texture slot in every descriptor set.
     *
     * The caller must ensure that the descriptor sets are not
     * currently being used by an in-flight command buffer unless
     * the corresponding Vulkan update-after-bind features are
     * explicitly enabled.
     */
    public synchronized void updateTexture(
            int textureIndex,
            TextureImage texture
    ) {

        if (textureIndex < 0 ||
                textureIndex >= MAX_TEXTURES) {

            throw new IllegalArgumentException(
                    "Invalid texture index: " +
                            textureIndex +
                            ", valid range: 0-" +
                            (MAX_TEXTURES - 1)
            );
        }

        if (texture == null) {
            throw new IllegalArgumentException(
                    "Texture cannot be null"
            );
        }

        textures[textureIndex] = texture;

        try (MemoryStack stack =
                     MemoryStack.stackPush()) {

            VkDescriptorImageInfo.Buffer imageInfo =
                    VkDescriptorImageInfo.calloc(
                                    1,
                                    stack
                            )
                            .imageLayout(
                                    VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL
                            )
                            .imageView(
                                    texture.getImageViewHandle()
                            )
                            .sampler(
                                    texture.getSamplerHandle()
                            );

            for (long descriptorSetHandle :
                    descriptorSetHandles) {

                VkWriteDescriptorSet.Buffer write =
                        VkWriteDescriptorSet.calloc(
                                1,
                                stack
                        );

                write.get(0)
                        .sType(
                                VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET
                        )
                        .dstSet(descriptorSetHandle)
                        .dstBinding(1)
                        .dstArrayElement(textureIndex)
                        .descriptorType(
                                VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER
                        )
                        .descriptorCount(1)
                        .pImageInfo(imageInfo);

                vkUpdateDescriptorSets(
                        device,
                        write,
                        null
                );
            }
        }
    }

    public int getImageCount() {
        return descriptorSetHandles.length;
    }

    public long getDescriptorSetLayoutHandle() {
        return descriptorSetLayoutHandle;
    }

    public long getDescriptorSet(
            int imageIndex
    ) {
        return descriptorSetHandles[imageIndex];
    }

    public TextureImage getCurrentTexture() {
        return textures[0];
    }

    public TextureImage getTexture(
            int index
    ) {

        if (index < 0 ||
                index >= MAX_TEXTURES) {

            throw new IllegalArgumentException(
                    "Invalid texture index: " + index
            );
        }

        return textures[index];
    }

    @Override
    public void close() {

        vkDestroyDescriptorPool(
                device,
                descriptorPoolHandle,
                null
        );

        vkDestroyDescriptorSetLayout(
                device,
                descriptorSetLayoutHandle,
                null
        );
    }
}