package nv.core;

import nv.core.annotations.EngineCore;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

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
    private final VkCommandBuffer commandBuffer;

    public CommandBuffers(VkDevice device, GraphicsPipeline pipeline, Swapchain swapchain) {
        this.device = device;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                    .queueFamilyIndex(0)
                    .flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);

            LongBuffer pCommandPool = stack.mallocLong(1);
            if (vkCreateCommandPool(device, poolInfo, null, pCommandPool) != VK_SUCCESS) {
                throw new RuntimeException("Errore nella creazione del Command Pool");
            }
            this.commandPoolHandle = pCommandPool.get(0);

            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .commandPool(commandPoolHandle)
                    .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                    .commandBufferCount(1);

            PointerBuffer pCommandBuffer = stack.mallocPointer(1);
            if (vkAllocateCommandBuffers(device, allocInfo, pCommandBuffer) != VK_SUCCESS) {
                throw new RuntimeException("Errore nell'allocazione del Command Buffer");
            }
            this.commandBuffer = new VkCommandBuffer(pCommandBuffer.get(0), device);
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

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                    .flags(0);

            int err = vkBeginCommandBuffer(commandBuffer, beginInfo);
            if (err != VK_SUCCESS) {
                throw new RuntimeException("vkBeginCommandBuffer fallita: " + err);
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

            // Set Dynamic Viewport and Scissor
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
                throw new RuntimeException("vkEndCommandBuffer fallita: " + err);
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

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                    .flags(0);

            int err = vkBeginCommandBuffer(commandBuffer, beginInfo);
            if (err != VK_SUCCESS) {
                throw new RuntimeException("vkBeginCommandBuffer fallita: " + err);
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

            // Set Dynamic Viewport and Scissor
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

            // PASS 1: Geometria (Sotto)
            if (graphicsIndexCount > 0) {
                vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipelineHandle);
                vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipelineLayoutHandle, 0, pDescriptorSets, null);
                vkCmdBindVertexBuffers(commandBuffer, 0, buffers, offsets);
                vkCmdBindIndexBuffer(commandBuffer, indexBufferHandle, 0, VK_INDEX_TYPE_UINT32);
                vkCmdDrawIndexed(commandBuffer, graphicsIndexCount, 1, 0, 0, 0);
            }

            // PASS 2: Immagini (Sopra)
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
                throw new RuntimeException("vkEndCommandBuffer fallita: " + err);
            }
        }
    }

    public VkCommandBuffer getCommandBuffer() {
        return commandBuffer;
    }

    public void free() {
        vkDestroyCommandPool(device, commandPoolHandle, null);
    }
}
