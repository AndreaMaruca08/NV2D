package nv.core.data;

import nv.core.annotations.EngineCore;
import nv.core.errors.ex.EngineEx;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import java.nio.LongBuffer;
import static org.lwjgl.vulkan.VK10.*;

/**
 * GPU texture image (Vulkan)
 * @since 1.0
 * @author Andrea Maruca
 */
@EngineCore
@SuppressWarnings("unused")
public final class TextureImage implements AutoCloseable {

    private final VkDevice device;
    private final long imageHandle;
    private final long memoryHandle;
    private final long imageViewHandle;
    private final long samplerHandle;

    public TextureImage(VkDevice device, VkPhysicalDevice physicalDevice, VkQueue graphicsQueue,
                        java.nio.ByteBuffer pixels, int width, int height) {
        this.device = device;
        long imageSize = (long) width * height * 4; // RGBA (4 byte per pixel)

        try (MemoryStack stack = MemoryStack.stackPush()) {
            // 1. Creazione della VkImage ottimizzata per la GPU
            VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                    .imageType(VK_IMAGE_TYPE_2D)
                    .extent(e -> e.set(width, height, 1))
                    .mipLevels(1)
                    .arrayLayers(1)
                    .format(VK_FORMAT_R8G8B8A8_UNORM)
                    .tiling(VK_IMAGE_TILING_OPTIMAL)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                    .usage(VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
                    .samples(VK_SAMPLE_COUNT_1_BIT);

            LongBuffer pImage = stack.mallocLong(1);
            if (vkCreateImage(device, imageInfo, null, pImage) != VK_SUCCESS) {
                throw new EngineEx("Impossibile creare la VkImage per la texture");
            }
            this.imageHandle = pImage.get(0);

            // 2. Allocazione memoria tramite l'utility VulkanMemory condivisa
            VkMemoryRequirements memReqs = VkMemoryRequirements.malloc(stack);
            vkGetImageMemoryRequirements(device, imageHandle, memReqs);

            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    .allocationSize(memReqs.size())
                    .memoryTypeIndex(VulkanMemory.findMemoryType(physicalDevice,
                            memReqs.memoryTypeBits(), VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT));

            LongBuffer pMemory = stack.mallocLong(1);
            if (vkAllocateMemory(device, allocInfo, null, pMemory) != VK_SUCCESS) {
                throw new EngineEx("Impossibile allocare memoria per la texture");
            }
            this.memoryHandle = pMemory.get(0);
            vkBindImageMemory(device, imageHandle, memoryHandle, 0);

            // 3. Upload dei dati pixel tramite StagingBuffer transitorio
            try (nv.core.StagingBuffer stagingBuffer = new nv.core.StagingBuffer(device, physicalDevice, imageSize)) {
                stagingBuffer.upload(pixels);

                // Esecuzione dei comandi immediati sulla coda grafica per spostare i dati
                executeOneTimeCommands(graphicsQueue, cmd -> {
                    transitionImageLayout(cmd, imageHandle, VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);

                    VkBufferImageCopy.Buffer region = VkBufferImageCopy.calloc(1, stack)
                            .bufferOffset(0)
                            .imageSubresource(sub -> sub.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).mipLevel(0).baseArrayLayer(0).layerCount(1))
                            .imageExtent(e -> e.set(width, height, 1));

                    vkCmdCopyBufferToImage(cmd, stagingBuffer.getHandle(), imageHandle, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region);
                    transitionImageLayout(cmd, imageHandle, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
                });
            }

            // 4. Creazione della VkImageView
            VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                    .image(imageHandle)
                    .viewType(VK_IMAGE_VIEW_TYPE_2D)
                    .format(VK_FORMAT_R8G8B8A8_UNORM)
                    .subresourceRange(r -> r.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).baseMipLevel(0).levelCount(1).baseArrayLayer(0).layerCount(1));

            LongBuffer pView = stack.mallocLong(1);
            if (vkCreateImageView(device, viewInfo, null, pView) != VK_SUCCESS) {
                throw new EngineEx("Impossibile creare la VkImageView della texture");
            }
            this.imageViewHandle = pView.get(0);

            // 5. Creazione del Sampler lineare per evitare pixelature nei font
            VkSamplerCreateInfo samplerInfo = VkSamplerCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
                    .magFilter(VK_FILTER_LINEAR)
                    .minFilter(VK_FILTER_LINEAR)
                    .addressModeU(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
                    .addressModeV(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
                    .addressModeW(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
                    .borderColor(VK_BORDER_COLOR_INT_OPAQUE_BLACK)
                    .unnormalizedCoordinates(false);

            LongBuffer pSampler = stack.mallocLong(1);
            if (vkCreateSampler(device, samplerInfo, null, pSampler) != VK_SUCCESS) {
                throw new EngineEx("Impossibile creare la VkSampler");
            }
            this.samplerHandle = pSampler.get(0);
        }
    }

    private void executeOneTimeCommands(VkQueue queue, java.util.function.Consumer<VkCommandBuffer> action) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                    .flags(VK_COMMAND_POOL_CREATE_TRANSIENT_BIT);
            LongBuffer pPool = stack.mallocLong(1);
            vkCreateCommandPool(device, poolInfo, null, pPool);
            long pool = pPool.get(0);

            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .commandPool(pool)
                    .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                    .commandBufferCount(1);
            org.lwjgl.PointerBuffer pCmd = stack.mallocPointer(1);
            vkAllocateCommandBuffers(device, allocInfo, pCmd);
            VkCommandBuffer cmd = new VkCommandBuffer(pCmd.get(0), device);

            vkBeginCommandBuffer(cmd, VkCommandBufferBeginInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO).flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT));
            action.accept(cmd);
            vkEndCommandBuffer(cmd);

            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_SUBMIT_INFO).pCommandBuffers(pCmd);
            vkQueueSubmit(queue, submitInfo, VK_NULL_HANDLE);
            vkQueueWaitIdle(queue);

            vkDestroyCommandPool(device, pool, null);
        }
    }

    private void transitionImageLayout(VkCommandBuffer cmd, long image, int oldLayout, int newLayout) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                    .oldLayout(oldLayout)
                    .newLayout(newLayout)
                    .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .image(image)
                    .subresourceRange(r -> r.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).baseMipLevel(0).levelCount(1).baseArrayLayer(0).layerCount(1));

            int sourceStage;
            int destinationStage;

            if (oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {
                barrier.srcAccessMask(0).dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
                sourceStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
                destinationStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
            } else if (oldLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL && newLayout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) {
                barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT).dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
                sourceStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
                destinationStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
            } else {
                throw new EngineEx("Transizione layout non supportata!");
            }

            vkCmdPipelineBarrier(cmd, sourceStage, destinationStage, 0, null, null, barrier);
        }
    }

    public long getImageViewHandle() { return imageViewHandle; }
    public long getSamplerHandle() { return samplerHandle; }

    @Override
    public void close() {
        vkDestroySampler(device, samplerHandle, null);
        vkDestroyImageView(device, imageViewHandle, null);
        vkDestroyImage(device, imageHandle, null);
        vkFreeMemory(device, memoryHandle, null);
    }
}