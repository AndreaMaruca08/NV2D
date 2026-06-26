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
 * @since 1.0
 * @author Andrea Maruca
 */
@EngineCore
@SuppressWarnings("unused")
public final class CommandBuffers {

    private final VkDevice device;
    private final long commandPoolHandle;
    private final VkCommandBuffer[] commandBuffers;

    public CommandBuffers(VkDevice device, GraphicsPipeline pipeline, Swapchain swapchain) {
        this.device = device;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                    .queueFamilyIndex(0)
                    .flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);

            LongBuffer pCommandPool = stack.mallocLong(1);
            if (vkCreateCommandPool(device, poolInfo, null, pCommandPool) != VK_SUCCESS) {
                throw new EngineEx("Error creating the Command Pool");
            }
            this.commandPoolHandle = pCommandPool.get(0);

            int imageCount = swapchain.getImageCount();

            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .commandPool(commandPoolHandle)
                    .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                    .commandBufferCount(imageCount);

            PointerBuffer pCommandBuffers = stack.mallocPointer(imageCount);
            if (vkAllocateCommandBuffers(device, allocInfo, pCommandBuffers) != VK_SUCCESS) {
                throw new EngineEx("Error allocating the Command Buffers");
            }

            this.commandBuffers = new VkCommandBuffer[imageCount];
            for (int i = 0; i < imageCount; i++) {
                this.commandBuffers[i] = new VkCommandBuffer(pCommandBuffers.get(i), device);
            }
        }
    }

    public void record(float[] bgColor, long renderPass,
                       long framebuffer,
                       long pipelineHandle,
                       long pipelineLayoutHandle,
                       long vertexBufferHandle,
                       long indexBufferHandle,
                       int indexCount,
                       long descriptorSet,
                       int width, int height) {

        VkCommandBuffer commandBuffer = commandBuffers[0];

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                    .flags(0);

            int err = vkBeginCommandBuffer(commandBuffer, beginInfo);
            if (err != VK_SUCCESS) {
                throw new EngineEx("Error starting vkBeginCommandBuffer: " + err);
            }

            VkClearValue.Buffer clearValues = VkClearValue.calloc(1, stack);
            clearValues.color()
                    .float32(0, bgColor[0])
                    .float32(1, bgColor[1])
                    .float32(2, bgColor[2])
                    .float32(3, bgColor[3]);

            VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                    .renderPass(renderPass)
                    .framebuffer(framebuffer)
                    .renderArea(ra -> ra
                            .offset(o -> o.set(0, 0))
                            .extent(e -> e.set(width, height)))
                    .pClearValues(clearValues);

            vkCmdBeginRenderPass(commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);

            VkViewport.Buffer viewport = VkViewport.calloc(1, stack);
            viewport.x(0.0f);
            viewport.y(0.0f);
            viewport.width((float) width);
            viewport.height((float) height);
            viewport.minDepth(0.0f);
            viewport.maxDepth(1.0f);
            vkCmdSetViewport(commandBuffer, 0, viewport);

            VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
            scissor.offset().set(0, 0);
            scissor.extent().set(width, height);
            vkCmdSetScissor(commandBuffer, 0, scissor);

            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineHandle);

            LongBuffer pDescriptorSets = stack.longs(descriptorSet);
            vkCmdBindDescriptorSets(
                    commandBuffer,
                    VK_PIPELINE_BIND_POINT_GRAPHICS,
                    pipelineLayoutHandle,
                    0,
                    pDescriptorSets,
                    null
            );

            LongBuffer buffers = stack.longs(vertexBufferHandle);
            LongBuffer offsets = stack.longs(0L);
            vkCmdBindVertexBuffers(commandBuffer, 0, buffers, offsets);
            vkCmdBindIndexBuffer(commandBuffer, indexBufferHandle, 0, VK_INDEX_TYPE_UINT32);
            vkCmdDrawIndexed(commandBuffer, indexCount, 1, 0, 0, 0);

            vkCmdEndRenderPass(commandBuffer);

            err = vkEndCommandBuffer(commandBuffer);
            if (err != VK_SUCCESS) {
                throw new EngineEx("vkEndCommandBuffer failed: " + err);
            }
        }
    }

    public void recordDual(float[] bgColor, long renderPass,
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
                           int width, int height) {

        VkCommandBuffer commandBuffer = commandBuffers[0];

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                    .flags(0);

            int err = vkBeginCommandBuffer(commandBuffer, beginInfo);
            if (err != VK_SUCCESS) {
                throw new EngineEx("Error starting vkBeginCommandBuffer: " + err);
            }

            VkClearValue.Buffer clearValues = VkClearValue.calloc(1, stack);
            clearValues.color()
                    .float32(0, bgColor[0])
                    .float32(1, bgColor[1])
                    .float32(2, bgColor[2])
                    .float32(3, bgColor[3]);

            VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                    .renderPass(renderPass)
                    .framebuffer(framebuffer)
                    .renderArea(ra -> ra
                            .offset(o -> o.set(0, 0))
                            .extent(e -> e.set(width, height)))
                    .pClearValues(clearValues);

            vkCmdBeginRenderPass(commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);

            VkViewport.Buffer viewport = VkViewport.calloc(1, stack);
            viewport.x(0.0f);
            viewport.y(0.0f);
            viewport.width((float) width);
            viewport.height((float) height);
            viewport.minDepth(0.0f);
            viewport.maxDepth(1.0f);
            vkCmdSetViewport(commandBuffer, 0, viewport);

            VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
            scissor.offset().set(0, 0);
            scissor.extent().set(width, height);
            vkCmdSetScissor(commandBuffer, 0, scissor);

            LongBuffer pDescriptorSets = stack.longs(descriptorSet);
            LongBuffer buffers = stack.longs(vertexBufferHandle);
            LongBuffer offsets = stack.longs(0L);

            if (graphicsIndexCount > 0) {
                vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipelineHandle);
                vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipelineLayoutHandle, 0, pDescriptorSets, null);
                vkCmdBindVertexBuffers(commandBuffer, 0, buffers, offsets);
                vkCmdBindIndexBuffer(commandBuffer, indexBufferHandle, 0, VK_INDEX_TYPE_UINT32);
                vkCmdDrawIndexed(commandBuffer, graphicsIndexCount, 1, 0, 0, 0);
            }

            if (imageIndexCount > 0) {
                vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, texturePipelineHandle);
                vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, texturePipelineLayoutHandle, 0, pDescriptorSets, null);
                vkCmdBindVertexBuffers(commandBuffer, 0, buffers, offsets);
                vkCmdBindIndexBuffer(commandBuffer, indexBufferHandle, 0, VK_INDEX_TYPE_UINT32);
                vkCmdDrawIndexed(commandBuffer, imageIndexCount, 1, imageIndexOffset, 0, 0);
            }

            vkCmdEndRenderPass(commandBuffer);

            err = vkEndCommandBuffer(commandBuffer);
            if (err != VK_SUCCESS) {
                throw new EngineEx("vkEndCommandBuffer failed: " + err);
            }
        }
    }

    public void recordOffscreen(int imageIndex,
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
                                boolean pixelPerfect) {

        VkCommandBuffer commandBuffer = commandBuffers[imageIndex];

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                    .flags(0);

            int err = vkBeginCommandBuffer(commandBuffer, beginInfo);
            if (err != VK_SUCCESS) {
                throw new EngineEx("Error starting vkBeginCommandBuffer: " + err);
            }

            VkClearValue.Buffer clearValues = VkClearValue.calloc(1, stack);
            clearValues.color()
                    .float32(0, bgColor[0])
                    .float32(1, bgColor[1])
                    .float32(2, bgColor[2])
                    .float32(3, bgColor[3]);

            VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                    .renderPass(renderPass)
                    .framebuffer(framebuffer)
                    .renderArea(ra -> ra
                            .offset(o -> o.set(0, 0))
                            .extent(e -> e.set(width, height)))
                    .pClearValues(clearValues);

            vkCmdBeginRenderPass(commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);

            VkViewport.Buffer viewport = VkViewport.calloc(1, stack);
            viewport.x(0.0f);
            viewport.y(0.0f);
            viewport.width((float) width);
            viewport.height((float) height);
            viewport.minDepth(0.0f);
            viewport.maxDepth(1.0f);
            vkCmdSetViewport(commandBuffer, 0, viewport);

            VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
            scissor.offset().set(0, 0);
            scissor.extent().set(width, height);
            vkCmdSetScissor(commandBuffer, 0, scissor);

            LongBuffer pDescriptorSets = stack.longs(descriptorSet);
            LongBuffer buffers = stack.longs(vertexBufferHandle);
            LongBuffer offsets = stack.longs(0L);

            if (graphicsIndexCount > 0) {
                vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineHandle);
                vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineLayoutHandle, 0, pDescriptorSets, null);
                vkCmdBindVertexBuffers(commandBuffer, 0, buffers, offsets);
                vkCmdBindIndexBuffer(commandBuffer, indexBufferHandle, 0, VK_INDEX_TYPE_UINT32);
                vkCmdDrawIndexed(commandBuffer, graphicsIndexCount, 1, 0, 0, 0);
            }

            if (imageIndexCount > 0) {
                vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, texturePipelineHandle);
                vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, texturePipelineLayoutHandle, 0, pDescriptorSets, null);
                vkCmdBindVertexBuffers(commandBuffer, 0, buffers, offsets);
                vkCmdBindIndexBuffer(commandBuffer, indexBufferHandle, 0, VK_INDEX_TYPE_UINT32);
                vkCmdDrawIndexed(commandBuffer, imageIndexCount, 1, imageIndexOffset, 0, 0);
            }

            vkCmdEndRenderPass(commandBuffer);

            // Barrier: internal target COLOR_ATTACHMENT → TRANSFER_SRC
            try (MemoryStack bs = MemoryStack.stackPush()) {
                VkImageMemoryBarrier.Buffer srcBarrier = VkImageMemoryBarrier.calloc(1, bs)
                        .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                        .oldLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                        .newLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL)
                        .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                        .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                        .image(sourceImageHandle)
                        .subresourceRange(r -> r
                                .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                                .baseMipLevel(0).levelCount(1)
                                .baseArrayLayer(0).layerCount(1))
                        .srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
                        .dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT);

                vkCmdPipelineBarrier(commandBuffer,
                        VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
                        VK_PIPELINE_STAGE_TRANSFER_BIT,
                        0, null, null, srcBarrier);
            }

            // Barrier: swapchain image UNDEFINED → TRANSFER_DST
            try (MemoryStack bs = MemoryStack.stackPush()) {
                VkImageMemoryBarrier.Buffer dstBarrier = VkImageMemoryBarrier.calloc(1, bs)
                        .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                        .oldLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                        .newLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                        .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                        .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                        .image(destinationImageHandle)
                        .subresourceRange(r -> r
                                .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                                .baseMipLevel(0).levelCount(1)
                                .baseArrayLayer(0).layerCount(1))
                        .srcAccessMask(0)
                        .dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);

                vkCmdPipelineBarrier(commandBuffer,
                        VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,
                        VK_PIPELINE_STAGE_TRANSFER_BIT,
                        0, null, null, dstBarrier);
            }

            // Clear della swapchain image
            VkClearColorValue clearColor = VkClearColorValue.calloc(stack);
            clearColor.float32(0, bgColor[0]);
            clearColor.float32(1, bgColor[1]);
            clearColor.float32(2, bgColor[2]);
            clearColor.float32(3, bgColor[3]);
            VkImageSubresourceRange clearRange = VkImageSubresourceRange.calloc(stack)
                    .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .baseMipLevel(0).levelCount(1)
                    .baseArrayLayer(0).layerCount(1);
            vkCmdClearColorImage(commandBuffer,
                    destinationImageHandle,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    clearColor, clearRange);

            // Blit: internal → swapchain
            int filter = pixelPerfect ? VK_FILTER_NEAREST : VK_FILTER_LINEAR;
            VkImageBlit.Buffer blit = VkImageBlit.calloc(1, stack);
            blit.get(0).srcOffsets(0).set(0, 0, 0);
            blit.get(0).srcOffsets(1).set(width, height, 1);
            blit.get(0).srcSubresource(s -> s
                    .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .mipLevel(0).baseArrayLayer(0).layerCount(1));
            blit.get(0).dstOffsets(0).set(0, 0, 0);
            blit.get(0).dstOffsets(1).set(swapchainWidth, swapchainHeight, 1);
            blit.get(0).dstSubresource(s -> s
                    .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .mipLevel(0).baseArrayLayer(0).layerCount(1));

            vkCmdBlitImage(commandBuffer,
                    sourceImageHandle, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                    destinationImageHandle, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    blit, filter);

            // Barrier: swapchain image TRANSFER_DST → PRESENT_SRC
            try (MemoryStack bs = MemoryStack.stackPush()) {
                VkImageMemoryBarrier.Buffer presentBarrier = VkImageMemoryBarrier.calloc(1, bs)
                        .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                        .oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                        .newLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)
                        .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                        .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                        .image(destinationImageHandle)
                        .subresourceRange(r -> r
                                .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                                .baseMipLevel(0).levelCount(1)
                                .baseArrayLayer(0).layerCount(1))
                        .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                        .dstAccessMask(0);

                vkCmdPipelineBarrier(commandBuffer,
                        VK_PIPELINE_STAGE_TRANSFER_BIT,
                        VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT,
                        0, null, null, presentBarrier);
            }

            err = vkEndCommandBuffer(commandBuffer);
            if (err != VK_SUCCESS) {
                throw new EngineEx("vkEndCommandBuffer failed: " + err);
            }
        }
    }

    public VkCommandBuffer getCommandBuffer(int imageIndex) {
        return commandBuffers[imageIndex];
    }

    public void free() {
        vkDestroyCommandPool(device, commandPoolHandle, null);
    }
}