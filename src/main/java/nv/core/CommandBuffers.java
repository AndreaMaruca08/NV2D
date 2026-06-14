package nv.core;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class CommandBuffers {

    private final VkDevice device;
    private final long commandPoolHandle;
    private final VkCommandBuffer commandBuffer;

    public CommandBuffers(VkDevice device, GraphicsPipeline pipeline, Swapchain swapchain) {
        this.device = device;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                    .queueFamilyIndex(0) // assumi queue family grafica = 0 (come nel tuo codice)
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

    public void record(long renderPass,
                       long framebuffer,
                       long pipelineHandle,
                       long pipelineLayoutHandle,
                       long vertexBufferHandle,
                       long indexBufferHandle,
                       int indexCount,
                       long descriptorSet,
                       int width, int height) {

        try (MemoryStack stack = MemoryStack.stackPush()) {

            // A. Begin del command buffer
            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                    .flags(0);

            int err = vkBeginCommandBuffer(commandBuffer, beginInfo);
            if (err != VK_SUCCESS) {
                throw new RuntimeException("vkBeginCommandBuffer fallita: " + err);
            }

            // B. Clear color (sfondo) + begin render pass
            VkClearValue.Buffer clearValues = VkClearValue.calloc(1, stack);
            clearValues.color()
                    .float32(0, 0.1f)  // R
                    .float32(1, 0.1f)  // G
                    .float32(2, 0.1f)  // B
                    .float32(3, 1.0f); // A

            VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                    .renderPass(renderPass)
                    .framebuffer(framebuffer)
                    .renderArea(ra -> ra
                            .offset(o -> o.set(0, 0))
                            .extent(e -> e.set(width, height)))
                    .pClearValues(clearValues);

            vkCmdBeginRenderPass(commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);

            // C. Bind pipeline grafica
            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineHandle);

            // D. Bind descriptor set (UBO ortografico: set = 0)
            LongBuffer pDescriptorSets = stack.longs(descriptorSet);
            vkCmdBindDescriptorSets(
                    commandBuffer,
                    VK_PIPELINE_BIND_POINT_GRAPHICS,
                    pipelineLayoutHandle,
                    0,
                    pDescriptorSets,
                    null
            );

            // E. Bind vertex buffer
            LongBuffer buffers = stack.longs(vertexBufferHandle);
            LongBuffer offsets = stack.longs(0L);
            vkCmdBindVertexBuffers(commandBuffer, 0, buffers, offsets);

            vkCmdBindIndexBuffer(commandBuffer, indexBufferHandle, 0, VK_INDEX_TYPE_UINT32);

            vkCmdDrawIndexed(commandBuffer, indexCount, 1, 0, 0, 0);

            // G. End render pass + end command buffer
            vkCmdEndRenderPass(commandBuffer);

            err = vkEndCommandBuffer(commandBuffer);
            if (err != VK_SUCCESS) {
                throw new RuntimeException("vkEndCommandBuffer fallita: " + err);
            }
        }
    }

    /**
     * Renderizza in due pass: prima shapes/testi con GraphicsPipeline, poi immagini con TexturePipeline.
     * Consente al sistema di immagini di essere completamente indipendente dal rendering normale.
     */
    public void recordDual(long renderPass,
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

            // A. Begin del command buffer
            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                    .flags(0);

            int err = vkBeginCommandBuffer(commandBuffer, beginInfo);
            if (err != VK_SUCCESS) {
                throw new RuntimeException("vkBeginCommandBuffer fallita: " + err);
            }

            // B. Clear color (sfondo) + begin render pass
            VkClearValue.Buffer clearValues = VkClearValue.calloc(1, stack);
            clearValues.color()
                    .float32(0, 0.1f)  // R
                    .float32(1, 0.1f)  // G
                    .float32(2, 0.1f)  // B
                    .float32(3, 1.0f); // A

            VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                    .renderPass(renderPass)
                    .framebuffer(framebuffer)
                    .renderArea(ra -> ra
                            .offset(o -> o.set(0, 0))
                            .extent(e -> e.set(width, height)))
                    .pClearValues(clearValues);

            vkCmdBeginRenderPass(commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);

            LongBuffer pDescriptorSets = stack.longs(descriptorSet);
            LongBuffer buffers = stack.longs(vertexBufferHandle);
            LongBuffer offsets = stack.longs(0L);

            if (imageIndexCount > 0) {
                // PASS 1: Renderizza immagini con TexturePipeline (8-float format)
                vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, texturePipelineHandle);
                vkCmdBindDescriptorSets(
                        commandBuffer,
                        VK_PIPELINE_BIND_POINT_GRAPHICS,
                        texturePipelineLayoutHandle,
                        0,
                        pDescriptorSets,
                        null
                );
                vkCmdBindVertexBuffers(commandBuffer, 0, buffers, offsets);
                vkCmdBindIndexBuffer(commandBuffer, indexBufferHandle, 0, VK_INDEX_TYPE_UINT32);
                vkCmdDrawIndexed(commandBuffer, imageIndexCount, 1, imageIndexOffset, 0, 0);
            }

            if (graphicsIndexCount > 0) {
                // PASS 2: Renderizza shapes e testi con GraphicsPipeline (7-float format)
                vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipelineHandle);
                vkCmdBindDescriptorSets(
                        commandBuffer,
                        VK_PIPELINE_BIND_POINT_GRAPHICS,
                        graphicsPipelineLayoutHandle,
                        0,
                        pDescriptorSets,
                        null
                );
                vkCmdBindVertexBuffers(commandBuffer, 0, buffers, offsets);
                vkCmdBindIndexBuffer(commandBuffer, indexBufferHandle, 0, VK_INDEX_TYPE_UINT32);
                vkCmdDrawIndexed(commandBuffer, graphicsIndexCount, 1, 0, 0, 0);
            }

            // G. End render pass + end command buffer
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