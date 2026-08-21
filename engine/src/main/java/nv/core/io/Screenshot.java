package nv.core.io;

import nv.core.InternalRenderTarget;
import nv.core.NvContext;
import nv.core.annotations.EngineCore;
import nv.core.data.VulkanMemory;
import nv.core.errors.ex.EngineEx;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.file.Path;

import static nv.core.errors.NvLogger.logEngine;
import static nv.core.errors.NvLogger.logWarn;
import static org.lwjgl.vulkan.VK10.*;

/**
 * Utility for capturing screenshots of the Vulkan framebuffer / render target.
 *
 * @since 1.6.2
 * @author Andrea Maruca
 */
@EngineCore
@SuppressWarnings("unused")
public final class Screenshot {

    private Screenshot() {}

    /**
     * Captures the current frame as a {@link BufferedImage}.
     *
     * @return BufferedImage containing the rendered frame pixels.
     */
    public static BufferedImage capture() {
        NvContext context = NvContext.getInstance();
        if (context == null) {
            throw new EngineEx("NvContext not initialized.");
        }
        return capture(context);
    }

    /**
     * Captures the current frame and saves it to a PNG file.
     * The path is automatically resolved via {@link AppPathUtils#resolvePath(String)}.
     *
     * @param fileName Path or filename (e.g. "screenshots/screenshot.png")
     * @return true if the image was successfully saved, false otherwise.
     */
    public static boolean savePNG(String fileName) {
        return save(fileName, "PNG");
    }

    /**
     * Captures the current frame and saves it in the specified image format.
     * The path is automatically resolved via {@link AppPathUtils#resolvePath(String)}.
     *
     * @param fileName Path or filename (e.g. "screenshots/screen.jpg")
     * @param format   Image format (e.g. "PNG", "JPG")
     * @return true if the image was successfully saved, false otherwise.
     */
    public static boolean save(String fileName, String format) {
        BufferedImage image = capture();
        if (image == null) {
            return false;
        }

        Path path = AppPathUtils.resolvePath(fileName);
        File file = path.toFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try {
            boolean success = ImageIO.write(image, format, file);
            if (success) {
                logEngine("Screenshot saved to: " + file.getAbsolutePath());
            } else {
                logWarn("No appropriate writer found for format: " + format);
            }
            return success;
        } catch (IOException e) {
            logWarn("Failed to save screenshot: " + e.getMessage());
            return false;
        }
    }

    /**
     * Internal implementation of the Vulkan GPU to CPU frame grabber.
     *
     * @param context the active NvContext instance
     * @return BufferedImage with the captured pixels
     */
    public static BufferedImage capture(NvContext context) {
        VkDevice device = context.getDevice();
        VkPhysicalDevice physicalDevice = context.getPhysicalDevice();
        VkQueue queue = context.getGraphicsQueue();
        InternalRenderTarget target = context.getInternalRenderTarget();

        if (device == null || physicalDevice == null || queue == null || target == null) {
            throw new EngineEx("Vulkan graphics resources are not ready for screenshot.");
        }

        int width = target.getWidth();
        int height = target.getHeight();
        long imageHandle = target.getImageHandle();
        long imageSize = (long) width * height * 4L; // 4 bytes per pixel (BGRA8)

        // 1. Sincronizzazione: attende il completamento del rendering sulla GPU
        vkDeviceWaitIdle(device);

        // 2. Alloca uno Staging Buffer host-visible di destinazione
        long[] bufferHandles = VulkanMemory.createBuffer(
                device,
                physicalDevice,
                imageSize,
                VK_BUFFER_USAGE_TRANSFER_DST_BIT,
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
        );
        long dstBuffer = bufferHandles[0];
        long dstMemory = bufferHandles[1];

        try {
            // 3. Esegue il comando di copia VkImage -> VkBuffer
            executeCopyCommand(device, queue, imageHandle, dstBuffer, width, height);

            // 4. Mappa la memoria CPU/GPU ed estrae i pixel convertendo BGRA -> ARGB
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer ppData = stack.mallocPointer(1);
                int mapResult = vkMapMemory(device, dstMemory, 0, imageSize, 0, ppData);
                if (mapResult != VK_SUCCESS) {
                    throw new EngineEx("Failed to map screenshot memory. Error: " + mapResult);
                }

                ByteBuffer buffer = MemoryUtil.memByteBuffer(ppData.get(0), (int) imageSize);
                BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                int[] pixels = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();

                // Conversione da Vulkan BGRA a Java ARGB
                for (int i = 0; i < width * height; i++) {
                    int b = buffer.get() & 0xFF;
                    int g = buffer.get() & 0xFF;
                    int r = buffer.get() & 0xFF;
                    int a = buffer.get() & 0xFF;
                    pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
                }

                vkUnmapMemory(device, dstMemory);
                return img;
            }
        } finally {
            // 5. Cleanup delle risorse temporanee
            vkDestroyBuffer(device, dstBuffer, null);
            vkFreeMemory(device, dstMemory, null);
        }
    }

    private static void executeCopyCommand(VkDevice device, VkQueue queue, long image, long buffer, int width, int height) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                    .flags(VK_COMMAND_POOL_CREATE_TRANSIENT_BIT);

            LongBuffer pPool = stack.mallocLong(1);
            if (vkCreateCommandPool(device, poolInfo, null, pPool) != VK_SUCCESS) {
                throw new EngineEx("Failed to create transient command pool for screenshot.");
            }
            long pool = pPool.get(0);

            try {
                VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                        .commandPool(pool)
                        .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                        .commandBufferCount(1);

                PointerBuffer pCmd = stack.mallocPointer(1);
                if (vkAllocateCommandBuffers(device, allocInfo, pCmd) != VK_SUCCESS) {
                    throw new EngineEx("Failed to allocate command buffer for screenshot.");
                }
                VkCommandBuffer cmd = new VkCommandBuffer(pCmd.get(0), device);

                VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                        .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

                vkBeginCommandBuffer(cmd, beginInfo);

                VkBufferImageCopy.Buffer region = VkBufferImageCopy.calloc(1, stack)
                        .bufferOffset(0)
                        .bufferRowLength(0)
                        .bufferImageHeight(0)
                        .imageSubresource(sub -> sub
                                .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                                .mipLevel(0)
                                .baseArrayLayer(0)
                                .layerCount(1)
                        )
                        .imageOffset(offset -> offset.set(0, 0, 0))
                        .imageExtent(extent -> extent.set(width, height, 1));

                // Copia i pixel dall'immagine al buffer di destinazione
                vkCmdCopyImageToBuffer(cmd, image, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, buffer, region);

                vkEndCommandBuffer(cmd);

                VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                        .pCommandBuffers(pCmd);

                if (vkQueueSubmit(queue, submitInfo, VK_NULL_HANDLE) != VK_SUCCESS) {
                    throw new EngineEx("Failed to submit copy command buffer for screenshot.");
                }
                vkQueueWaitIdle(queue);

            } finally {
                vkDestroyCommandPool(device, pool, null);
            }
        }
    }
}
