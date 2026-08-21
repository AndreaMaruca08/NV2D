package nv.core;

import nv.core.annotations.EngineCore;
import nv.core.errors.ex.EngineEx;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.VK10.*;

/**
 * Vulkan command buffer manager.
 *
 * @since 1.0
 * @author Andrea Maruca
 */
@EngineCore
@SuppressWarnings("unused")
public final class CommandBuffers {

    private final VkDevice device;
    private final long commandPoolHandle;
    private final VkCommandBuffer[] commandBuffers;

    public CommandBuffers(
            VkDevice device,
            GraphicsPipeline pipeline,
            Swapchain swapchain
    ) {
        this.device = device;

        try (MemoryStack stack = MemoryStack.stackPush()) {

            VkCommandPoolCreateInfo poolInfo =
                    VkCommandPoolCreateInfo.calloc(stack)
                            .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                            .queueFamilyIndex(0)
                            .flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);

            LongBuffer pCommandPool = stack.mallocLong(1);

            if (vkCreateCommandPool(
                    device,
                    poolInfo,
                    null,
                    pCommandPool
            ) != VK_SUCCESS) {
                throw new EngineEx("Error creating the Command Pool");
            }

            this.commandPoolHandle = pCommandPool.get(0);

            int imageCount = swapchain.getImageCount();

            VkCommandBufferAllocateInfo allocInfo =
                    VkCommandBufferAllocateInfo.calloc(stack)
                            .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                            .commandPool(commandPoolHandle)
                            .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                            .commandBufferCount(imageCount);

            PointerBuffer pCommandBuffers =
                    stack.mallocPointer(imageCount);

            if (vkAllocateCommandBuffers(
                    device,
                    allocInfo,
                    pCommandBuffers
            ) != VK_SUCCESS) {
                throw new EngineEx("Error allocating the Command Buffers");
            }

            this.commandBuffers = new VkCommandBuffer[imageCount];

            for (int i = 0; i < imageCount; i++) {
                this.commandBuffers[i] =
                        new VkCommandBuffer(pCommandBuffers.get(i), device);
            }
        }
    }

    public void record(
            float[] bgColor,
            long renderPass,
            long framebuffer,
            long pipelineHandle,
            long pipelineLayoutHandle,
            long vertexBufferHandle,
            long indexBufferHandle,
            int indexCount,
            long descriptorSet,
            int width,
            int height
    ) {

        VkCommandBuffer commandBuffer = commandBuffers[0];

        try (MemoryStack stack = MemoryStack.stackPush()) {

            beginCommandBuffer(commandBuffer, stack);

            beginRenderPass(
                    commandBuffer,
                    stack,
                    bgColor,
                    renderPass,
                    framebuffer,
                    width,
                    height
            );

            setViewportAndScissor(
                    commandBuffer,
                    stack,
                    width,
                    height
            );

            bindCommonResources(
                    commandBuffer,
                    stack,
                    pipelineLayoutHandle,
                    descriptorSet,
                    vertexBufferHandle,
                    indexBufferHandle
            );

            vkCmdBindPipeline(
                    commandBuffer,
                    VK_PIPELINE_BIND_POINT_GRAPHICS,
                    pipelineHandle
            );

            vkCmdDrawIndexed(
                    commandBuffer,
                    indexCount,
                    1,
                    0,
                    0,
                    0
            );

            vkCmdEndRenderPass(commandBuffer);

            endCommandBuffer(commandBuffer);
        }
    }

    /**
     * Records a frame containing:
     *
     * 1. normal geometry
     * 2. image/textured geometry
     *
     * The common descriptor set and vertex/index buffers are bound only once.
     */
    public void recordDual(
            float[] bgColor,
            long renderPass,
            long framebuffer,
            long graphicsPipelineHandle,
            long graphicsPipelineLayoutHandle,
            long texturePipelineHandle,
            long texturePipelineLayoutHandle,
            long vertexBufferHandle,
            long indexBufferHandle,
            int graphicsIndexCount,
            int imageIndexCount,
            int imageIndexOffset,
            long descriptorSet,
            int width,
            int height
    ) {

        VkCommandBuffer commandBuffer = commandBuffers[0];

        try (MemoryStack stack = MemoryStack.stackPush()) {

            beginCommandBuffer(commandBuffer, stack);

            beginRenderPass(
                    commandBuffer,
                    stack,
                    bgColor,
                    renderPass,
                    framebuffer,
                    width,
                    height
            );

            setViewportAndScissor(
                    commandBuffer,
                    stack,
                    width,
                    height
            );

            /*
             * Both pipelines use the same descriptor set layout and
             * identical vertex/index buffer layout.
             *
             * Bind common resources once.
             */
            if (graphicsIndexCount > 0 || imageIndexCount > 0) {

                LongBuffer pDescriptorSets =
                        stack.longs(descriptorSet);

                LongBuffer buffers =
                        stack.longs(vertexBufferHandle);

                LongBuffer offsets =
                        stack.longs(0L);

                if (graphicsIndexCount > 0) {

                    vkCmdBindPipeline(
                            commandBuffer,
                            VK_PIPELINE_BIND_POINT_GRAPHICS,
                            graphicsPipelineHandle
                    );

                    vkCmdBindDescriptorSets(
                            commandBuffer,
                            VK_PIPELINE_BIND_POINT_GRAPHICS,
                            graphicsPipelineLayoutHandle,
                            0,
                            pDescriptorSets,
                            null
                    );

                    vkCmdBindVertexBuffers(
                            commandBuffer,
                            0,
                            buffers,
                            offsets
                    );

                    vkCmdBindIndexBuffer(
                            commandBuffer,
                            indexBufferHandle,
                            0,
                            VK_INDEX_TYPE_UINT32
                    );

                    vkCmdDrawIndexed(
                            commandBuffer,
                            graphicsIndexCount,
                            1,
                            0,
                            0,
                            0
                    );
                }

                if (imageIndexCount > 0) {

                    /*
                     * The vertex/index/descriptor bindings remain active.
                     * We only switch graphics pipeline.
                     */
                    vkCmdBindPipeline(
                            commandBuffer,
                            VK_PIPELINE_BIND_POINT_GRAPHICS,
                            texturePipelineHandle
                    );

                    /*
                     * The descriptor set/layout is compatible with the
                     * second pipeline as well. Rebinding is unnecessary
                     * when the layout is compatible.
                     */
                    vkCmdDrawIndexed(
                            commandBuffer,
                            imageIndexCount,
                            1,
                            imageIndexOffset,
                            0,
                            0
                    );
                }
            }

            vkCmdEndRenderPass(commandBuffer);

            endCommandBuffer(commandBuffer);
        }
    }

    /**
     * Records rendering into the internal offscreen target and then blits
     * the result into the swapchain image.
     */
    public void recordOffscreen(
            int imageIndex,
            float[] bgColor,
            long renderPass,
            long framebuffer,
            long pipelineHandle,
            long pipelineLayoutHandle,
            long texturePipelineHandle,
            long texturePipelineLayoutHandle,
            long vertexBufferHandle,
            long indexBufferHandle,
            int graphicsIndexCount,
            int imageIndexCount,
            int imageIndexOffset,
            long descriptorSet,
            long sourceImageHandle,
            long destinationImageHandle,
            int width,
            int height,
            int swapchainWidth,
            int swapchainHeight,
            boolean pixelPerfect,
            boolean sourceImageInitialized
    ) {

        VkCommandBuffer commandBuffer =
                commandBuffers[imageIndex];

        try (MemoryStack stack = MemoryStack.stackPush()) {

            beginCommandBuffer(commandBuffer, stack);

            /*
             * Transition internal render target:
             *
             * previous:
             *   TRANSFER_SRC
             *
             * first frame:
             *   UNDEFINED
             *
             * -> COLOR_ATTACHMENT
             */
            VkImageMemoryBarrier.Buffer targetBarrier =
                    VkImageMemoryBarrier.calloc(1, stack)
                            .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                            .oldLayout(
                                    sourceImageInitialized
                                            ? VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL
                                            : VK_IMAGE_LAYOUT_UNDEFINED
                            )
                            .newLayout(
                                    VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL
                            )
                            .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                            .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                            .image(sourceImageHandle)
                            .subresourceRange(r -> r
                                    .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                                    .baseMipLevel(0)
                                    .levelCount(1)
                                    .baseArrayLayer(0)
                                    .layerCount(1)
                            )
                            .srcAccessMask(
                                    sourceImageInitialized
                                            ? VK_ACCESS_TRANSFER_READ_BIT
                                            : 0
                            )
                            .dstAccessMask(
                                    VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT
                            );

            vkCmdPipelineBarrier(
                    commandBuffer,
                    sourceImageInitialized
                            ? VK_PIPELINE_STAGE_TRANSFER_BIT
                            : VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,
                    VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
                    0,
                    null,
                    null,
                    targetBarrier
            );

            beginRenderPass(
                    commandBuffer,
                    stack,
                    bgColor,
                    renderPass,
                    framebuffer,
                    width,
                    height
            );

            setViewportAndScissor(
                    commandBuffer,
                    stack,
                    width,
                    height
            );

            /*
             * Common bindings.
             */
            LongBuffer pDescriptorSets =
                    stack.longs(descriptorSet);

            LongBuffer buffers =
                    stack.longs(vertexBufferHandle);

            LongBuffer offsets =
                    stack.longs(0L);

            if (graphicsIndexCount > 0 || imageIndexCount > 0) {

                /*
                 * Bind descriptor + vertex/index resources once.
                 *
                 * The graphics pipeline is bound first if there is
                 * regular geometry.
                 */
                if (graphicsIndexCount > 0) {

                    vkCmdBindPipeline(
                            commandBuffer,
                            VK_PIPELINE_BIND_POINT_GRAPHICS,
                            pipelineHandle
                    );

                    vkCmdBindDescriptorSets(
                            commandBuffer,
                            VK_PIPELINE_BIND_POINT_GRAPHICS,
                            pipelineLayoutHandle,
                            0,
                            pDescriptorSets,
                            null
                    );

                    vkCmdBindVertexBuffers(
                            commandBuffer,
                            0,
                            buffers,
                            offsets
                    );

                    vkCmdBindIndexBuffer(
                            commandBuffer,
                            indexBufferHandle,
                            0,
                            VK_INDEX_TYPE_UINT32
                    );

                    vkCmdDrawIndexed(
                            commandBuffer,
                            graphicsIndexCount,
                            1,
                            0,
                            0,
                            0
                    );
                }

                if (imageIndexCount > 0) {

                    vkCmdBindPipeline(
                            commandBuffer,
                            VK_PIPELINE_BIND_POINT_GRAPHICS,
                            texturePipelineHandle
                    );

                    /*
                     * If graphicsIndexCount == 0 there was no opportunity
                     * to bind the common resources yet.
                     */
                    if (graphicsIndexCount == 0) {

                        vkCmdBindDescriptorSets(
                                commandBuffer,
                                VK_PIPELINE_BIND_POINT_GRAPHICS,
                                texturePipelineLayoutHandle,
                                0,
                                pDescriptorSets,
                                null
                        );

                        vkCmdBindVertexBuffers(
                                commandBuffer,
                                0,
                                buffers,
                                offsets
                        );

                        vkCmdBindIndexBuffer(
                                commandBuffer,
                                indexBufferHandle,
                                0,
                                VK_INDEX_TYPE_UINT32
                        );
                    }

                    vkCmdDrawIndexed(
                            commandBuffer,
                            imageIndexCount,
                            1,
                            imageIndexOffset,
                            0,
                            0
                    );
                }
            }

            vkCmdEndRenderPass(commandBuffer);

            /*
             * Internal target:
             * COLOR_ATTACHMENT -> TRANSFER_SRC
             */
            try (MemoryStack bs = MemoryStack.stackPush()) {

                VkImageMemoryBarrier.Buffer srcBarrier =
                        VkImageMemoryBarrier.calloc(1, bs)
                                .sType(
                                        VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER
                                )
                                .oldLayout(
                                        VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL
                                )
                                .newLayout(
                                        VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL
                                )
                                .srcQueueFamilyIndex(
                                        VK_QUEUE_FAMILY_IGNORED
                                )
                                .dstQueueFamilyIndex(
                                        VK_QUEUE_FAMILY_IGNORED
                                )
                                .image(sourceImageHandle)
                                .subresourceRange(r -> r
                                        .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                                        .baseMipLevel(0)
                                        .levelCount(1)
                                        .baseArrayLayer(0)
                                        .layerCount(1)
                                )
                                .srcAccessMask(
                                        VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT
                                )
                                .dstAccessMask(
                                        VK_ACCESS_TRANSFER_READ_BIT
                                );

                vkCmdPipelineBarrier(
                        commandBuffer,
                        VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
                        VK_PIPELINE_STAGE_TRANSFER_BIT,
                        0,
                        null,
                        null,
                        srcBarrier
                );
            }

            /*
             * Swapchain image:
             * UNDEFINED -> TRANSFER_DST
             */
            try (MemoryStack bs = MemoryStack.stackPush()) {

                VkImageMemoryBarrier.Buffer dstBarrier =
                        VkImageMemoryBarrier.calloc(1, bs)
                                .sType(
                                        VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER
                                )
                                .oldLayout(
                                        VK_IMAGE_LAYOUT_UNDEFINED
                                )
                                .newLayout(
                                        VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL
                                )
                                .srcQueueFamilyIndex(
                                        VK_QUEUE_FAMILY_IGNORED
                                )
                                .dstQueueFamilyIndex(
                                        VK_QUEUE_FAMILY_IGNORED
                                )
                                .image(destinationImageHandle)
                                .subresourceRange(r -> r
                                        .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                                        .baseMipLevel(0)
                                        .levelCount(1)
                                        .baseArrayLayer(0)
                                        .layerCount(1)
                                )
                                .srcAccessMask(0)
                                .dstAccessMask(
                                        VK_ACCESS_TRANSFER_WRITE_BIT
                                );

                vkCmdPipelineBarrier(
                        commandBuffer,
                        VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,
                        VK_PIPELINE_STAGE_TRANSFER_BIT,
                        0,
                        null,
                        null,
                        dstBarrier
                );
            }

            /*
             * Internal target -> swapchain.
             */
            int filter =
                    pixelPerfect
                            ? VK_FILTER_NEAREST
                            : VK_FILTER_LINEAR;

            VkImageBlit.Buffer blit =
                    VkImageBlit.calloc(1, stack);

            blit.get(0)
                    .srcOffsets(0)
                    .set(0, 0, 0);

            blit.get(0)
                    .srcOffsets(1)
                    .set(width, height, 1);

            blit.get(0)
                    .srcSubresource(s -> s
                            .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                            .mipLevel(0)
                            .baseArrayLayer(0)
                            .layerCount(1)
                    );

            blit.get(0)
                    .dstOffsets(0)
                    .set(0, 0, 0);

            blit.get(0)
                    .dstOffsets(1)
                    .set(
                            swapchainWidth,
                            swapchainHeight,
                            1
                    );

            blit.get(0)
                    .dstSubresource(s -> s
                            .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                            .mipLevel(0)
                            .baseArrayLayer(0)
                            .layerCount(1)
                    );

            vkCmdBlitImage(
                    commandBuffer,
                    sourceImageHandle,
                    VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                    destinationImageHandle,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    blit,
                    filter
            );

            /*
             * Swapchain:
             * TRANSFER_DST -> PRESENT_SRC
             */
            try (MemoryStack bs = MemoryStack.stackPush()) {

                VkImageMemoryBarrier.Buffer presentBarrier =
                        VkImageMemoryBarrier.calloc(1, bs)
                                .sType(
                                        VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER
                                )
                                .oldLayout(
                                        VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL
                                )
                                .newLayout(
                                        VK_IMAGE_LAYOUT_PRESENT_SRC_KHR
                                )
                                .srcQueueFamilyIndex(
                                        VK_QUEUE_FAMILY_IGNORED
                                )
                                .dstQueueFamilyIndex(
                                        VK_QUEUE_FAMILY_IGNORED
                                )
                                .image(destinationImageHandle)
                                .subresourceRange(r -> r
                                        .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                                        .baseMipLevel(0)
                                        .levelCount(1)
                                        .baseArrayLayer(0)
                                        .layerCount(1)
                                )
                                .srcAccessMask(
                                        VK_ACCESS_TRANSFER_WRITE_BIT
                                )
                                .dstAccessMask(0);

                vkCmdPipelineBarrier(
                        commandBuffer,
                        VK_PIPELINE_STAGE_TRANSFER_BIT,
                        VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT,
                        0,
                        null,
                        null,
                        presentBarrier
                );
            }

            endCommandBuffer(commandBuffer);
        }
    }

    /**
     * Records scene rendering into offscreen target and then executes the
     * fullscreen post-processing pipeline directly into the swapchain image.
     */
    public void recordWithPostProcess(
            int imageIndex,
            float[] bgColor,
            long renderPass,
            long framebuffer,
            long pipelineHandle,
            long pipelineLayoutHandle,
            long texturePipelineHandle,
            long texturePipelineLayoutHandle,
            long vertexBufferHandle,
            long indexBufferHandle,
            int graphicsIndexCount,
            int imageIndexCount,
            int imageIndexOffset,
            long descriptorSet,
            long sourceImageHandle,
            int width,
            int height,
            int swapchainWidth,
            int swapchainHeight,
            boolean sourceImageInitialized,
            PostProcessPipeline postProcessPipeline,
            PostProcessSettings postProcessSettings,
            float time
    ) {

        VkCommandBuffer commandBuffer = commandBuffers[imageIndex];

        try (MemoryStack stack = MemoryStack.stackPush()) {

            beginCommandBuffer(commandBuffer, stack);

            /*
             * Transition internal render target:
             * TRANSFER_SRC / UNDEFINED -> COLOR_ATTACHMENT
             */
            VkImageMemoryBarrier.Buffer targetBarrier =
                    VkImageMemoryBarrier.calloc(1, stack)
                            .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                            .oldLayout(
                                    sourceImageInitialized
                                            ? VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL
                                            : VK_IMAGE_LAYOUT_UNDEFINED
                            )
                            .newLayout(
                                    VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL
                            )
                            .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                            .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                            .image(sourceImageHandle)
                            .subresourceRange(r -> r
                                    .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                                    .baseMipLevel(0)
                                    .levelCount(1)
                                    .baseArrayLayer(0)
                                    .layerCount(1)
                            )
                            .srcAccessMask(
                                    sourceImageInitialized
                                            ? VK_ACCESS_TRANSFER_READ_BIT
                                            : 0
                            )
                            .dstAccessMask(
                                    VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT
                            );

            vkCmdPipelineBarrier(
                    commandBuffer,
                    sourceImageInitialized
                            ? VK_PIPELINE_STAGE_TRANSFER_BIT
                            : VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,
                    VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
                    0,
                    null,
                    null,
                    targetBarrier
            );

            beginRenderPass(
                    commandBuffer,
                    stack,
                    bgColor,
                    renderPass,
                    framebuffer,
                    width,
                    height
            );

            setViewportAndScissor(
                    commandBuffer,
                    stack,
                    width,
                    height
            );

            LongBuffer pDescriptorSets = stack.longs(descriptorSet);
            LongBuffer buffers = stack.longs(vertexBufferHandle);
            LongBuffer offsets = stack.longs(0L);

            if (graphicsIndexCount > 0 || imageIndexCount > 0) {

                if (graphicsIndexCount > 0) {
                    vkCmdBindPipeline(
                            commandBuffer,
                            VK_PIPELINE_BIND_POINT_GRAPHICS,
                            pipelineHandle
                    );

                    vkCmdBindDescriptorSets(
                            commandBuffer,
                            VK_PIPELINE_BIND_POINT_GRAPHICS,
                            pipelineLayoutHandle,
                            0,
                            pDescriptorSets,
                            null
                    );

                    vkCmdBindVertexBuffers(
                            commandBuffer,
                            0,
                            buffers,
                            offsets
                    );

                    vkCmdBindIndexBuffer(
                            commandBuffer,
                            indexBufferHandle,
                            0,
                            VK_INDEX_TYPE_UINT32
                    );

                    vkCmdDrawIndexed(
                            commandBuffer,
                            graphicsIndexCount,
                            1,
                            0,
                            0,
                            0
                    );
                }

                if (imageIndexCount > 0) {
                    vkCmdBindPipeline(
                            commandBuffer,
                            VK_PIPELINE_BIND_POINT_GRAPHICS,
                            texturePipelineHandle
                    );

                    if (graphicsIndexCount == 0) {
                        vkCmdBindDescriptorSets(
                                commandBuffer,
                                VK_PIPELINE_BIND_POINT_GRAPHICS,
                                texturePipelineLayoutHandle,
                                0,
                                pDescriptorSets,
                                null
                        );

                        vkCmdBindVertexBuffers(
                                commandBuffer,
                                0,
                                buffers,
                                offsets
                        );

                        vkCmdBindIndexBuffer(
                                commandBuffer,
                                indexBufferHandle,
                                0,
                                VK_INDEX_TYPE_UINT32
                        );
                    }

                    vkCmdDrawIndexed(
                            commandBuffer,
                            imageIndexCount,
                            1,
                            imageIndexOffset,
                            0,
                            0
                    );
                }
            }

            vkCmdEndRenderPass(commandBuffer);

            /*
             * Transition internal target:
             * COLOR_ATTACHMENT -> SHADER_READ_ONLY
             */
            try (MemoryStack bs = MemoryStack.stackPush()) {
                VkImageMemoryBarrier.Buffer readBarrier =
                        VkImageMemoryBarrier.calloc(1, bs)
                                .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                                .oldLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                                .newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                                .image(sourceImageHandle)
                                .subresourceRange(r -> r
                                        .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                                        .baseMipLevel(0)
                                        .levelCount(1)
                                        .baseArrayLayer(0)
                                        .layerCount(1)
                                )
                                .srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
                                .dstAccessMask(VK_ACCESS_SHADER_READ_BIT);

                vkCmdPipelineBarrier(
                        commandBuffer,
                        VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
                        VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                        0,
                        null,
                        null,
                        readBarrier
                );
            }

            /*
             * Swapchain Post-Processing Render Pass:
             * Renders fullscreen quad with postprocessing directly into swapchain.
             */
            VkRenderPassBeginInfo ppPassInfo =
                    VkRenderPassBeginInfo.calloc(stack)
                            .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                            .renderPass(postProcessPipeline.getSwapchainRenderPassHandle())
                            .framebuffer(postProcessPipeline.getFramebuffer(imageIndex))
                            .renderArea(ra -> ra
                                    .offset(o -> o.set(0, 0))
                                    .extent(e -> e.set(swapchainWidth, swapchainHeight))
                            );

            vkCmdBeginRenderPass(
                    commandBuffer,
                    ppPassInfo,
                    VK_SUBPASS_CONTENTS_INLINE
            );

            setViewportAndScissor(
                    commandBuffer,
                    stack,
                    swapchainWidth,
                    swapchainHeight
            );

            vkCmdBindPipeline(
                    commandBuffer,
                    VK_PIPELINE_BIND_POINT_GRAPHICS,
                    postProcessPipeline.getPipelineHandle()
            );

            LongBuffer pPPDescriptorSets =
                    stack.longs(postProcessPipeline.getDescriptorSetHandle());

            vkCmdBindDescriptorSets(
                    commandBuffer,
                    VK_PIPELINE_BIND_POINT_GRAPHICS,
                    postProcessPipeline.getPipelineLayoutHandle(),
                    0,
                    pPPDescriptorSets,
                    null
            );

            postProcessSettings.pushConstants(
                    commandBuffer,
                    postProcessPipeline.getPipelineLayoutHandle(),
                    time
            );

            // Draw fullscreen triangle (3 vertices, 0 vertex buffer)
            vkCmdDraw(commandBuffer, 3, 1, 0, 0);

            vkCmdEndRenderPass(commandBuffer);

            /*
             * Transition internal target:
             * SHADER_READ_ONLY -> TRANSFER_SRC (ready for Screenshot / next frame)
             */
            try (MemoryStack bs = MemoryStack.stackPush()) {
                VkImageMemoryBarrier.Buffer transferSrcBarrier =
                        VkImageMemoryBarrier.calloc(1, bs)
                                .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                                .oldLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                                .newLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL)
                                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                                .image(sourceImageHandle)
                                .subresourceRange(r -> r
                                        .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                                        .baseMipLevel(0)
                                        .levelCount(1)
                                        .baseArrayLayer(0)
                                        .layerCount(1)
                                )
                                .srcAccessMask(VK_ACCESS_SHADER_READ_BIT)
                                .dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT);

                vkCmdPipelineBarrier(
                        commandBuffer,
                        VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                        VK_PIPELINE_STAGE_TRANSFER_BIT,
                        0,
                        null,
                        null,
                        transferSrcBarrier
                );
            }

            endCommandBuffer(commandBuffer);
        }
    }

    private void beginCommandBuffer(
            VkCommandBuffer commandBuffer,
            MemoryStack stack
    ) {

        VkCommandBufferBeginInfo beginInfo =
                VkCommandBufferBeginInfo.calloc(stack)
                        .sType(
                                VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO
                        )
                        .flags(0);

        int err =
                vkBeginCommandBuffer(
                        commandBuffer,
                        beginInfo
                );

        if (err != VK_SUCCESS) {
            throw new EngineEx(
                    "Error starting vkBeginCommandBuffer: " + err
            );
        }
    }

    private void endCommandBuffer(
            VkCommandBuffer commandBuffer
    ) {

        int err =
                vkEndCommandBuffer(commandBuffer);

        if (err != VK_SUCCESS) {
            throw new EngineEx(
                    "vkEndCommandBuffer failed: " + err
            );
        }
    }

    private void beginRenderPass(
            VkCommandBuffer commandBuffer,
            MemoryStack stack,
            float[] bgColor,
            long renderPass,
            long framebuffer,
            int width,
            int height
    ) {

        VkClearValue.Buffer clearValues =
                VkClearValue.calloc(1, stack);

        clearValues.color()
                .float32(0, bgColor[0])
                .float32(1, bgColor[1])
                .float32(2, bgColor[2])
                .float32(3, bgColor[3]);

        VkRenderPassBeginInfo renderPassInfo =
                VkRenderPassBeginInfo.calloc(stack)
                        .sType(
                                VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO
                        )
                        .renderPass(renderPass)
                        .framebuffer(framebuffer)
                        .renderArea(ra -> ra
                                .offset(o -> o.set(0, 0))
                                .extent(e -> e.set(width, height))
                        )
                        .pClearValues(clearValues);

        vkCmdBeginRenderPass(
                commandBuffer,
                renderPassInfo,
                VK_SUBPASS_CONTENTS_INLINE
        );
    }

    private void setViewportAndScissor(
            VkCommandBuffer commandBuffer,
            MemoryStack stack,
            int width,
            int height
    ) {

        VkViewport.Buffer viewport =
                VkViewport.calloc(1, stack)
                        .x(0.0f)
                        .y(0.0f)
                        .width((float) width)
                        .height((float) height)
                        .minDepth(0.0f)
                        .maxDepth(1.0f);

        vkCmdSetViewport(
                commandBuffer,
                0,
                viewport
        );

        VkRect2D.Buffer scissor =
                VkRect2D.calloc(1, stack);

        scissor.offset().set(0, 0);
        scissor.extent().set(width, height);

        vkCmdSetScissor(
                commandBuffer,
                0,
                scissor
        );
    }

    private void bindCommonResources(
            VkCommandBuffer commandBuffer,
            MemoryStack stack,
            long pipelineLayoutHandle,
            long descriptorSet,
            long vertexBufferHandle,
            long indexBufferHandle
    ) {

        LongBuffer pDescriptorSets =
                stack.longs(descriptorSet);

        vkCmdBindDescriptorSets(
                commandBuffer,
                VK_PIPELINE_BIND_POINT_GRAPHICS,
                pipelineLayoutHandle,
                0,
                pDescriptorSets,
                null
        );

        LongBuffer buffers =
                stack.longs(vertexBufferHandle);

        LongBuffer offsets =
                stack.longs(0L);

        vkCmdBindVertexBuffers(
                commandBuffer,
                0,
                buffers,
                offsets
        );

        vkCmdBindIndexBuffer(
                commandBuffer,
                indexBufferHandle,
                0,
                VK_INDEX_TYPE_UINT32
        );
    }

    public VkCommandBuffer getCommandBuffer(int imageIndex) {
        return commandBuffers[imageIndex];
    }

    public void free() {
        vkDestroyCommandPool(
                device,
                commandPoolHandle,
                null
        );
    }
}