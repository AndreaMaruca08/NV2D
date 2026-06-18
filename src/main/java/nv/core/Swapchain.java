package nv.core;

import nv.core.annotations.EngineCore;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkImageViewCreateInfo;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

/**
 * <p>Represents a GPU swapchain with images and image views</p>
 * @since 1.0
 * @author Andrea Maruca
 */
@EngineCore
@SuppressWarnings("unused")
public final class Swapchain implements AutoCloseable {

    private final VkDevice device;
    private final long swapchainHandle;
    private final int width;
    private final int height;

    // Nuovi campi necessari per far comunicare la nv.core.Swapchain con la Pipeline e i Framebuffer
    private final int imageFormat;
    private final long[] imageViews;

    public Swapchain(VkDevice device, long surfaceHandle, int width, int height, boolean vsync) {
        this.device = device;
        this.width = width;
        this.height = height;
        this.imageFormat = VK_FORMAT_B8G8R8A8_SRGB; // Formato colore standard (BGRA 32-bit)

        try (MemoryStack stack = MemoryStack.stackPush()) {

            // 1. Configurazione e creazione della nv.core.Swapchain
            VkSwapchainCreateInfoKHR createInfo = VkSwapchainCreateInfoKHR.calloc(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR);
            createInfo.surface(surfaceHandle);

            createInfo.minImageCount(3); // Triple Buffering
            createInfo.imageFormat(imageFormat);
            createInfo.imageColorSpace(VK_COLOR_SPACE_SRGB_NONLINEAR_KHR);

            createInfo.imageExtent().width(width);
            createInfo.imageExtent().height(height);

            createInfo.imageArrayLayers(1);
            createInfo.imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);
            createInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
            createInfo.preTransform(VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR);
            createInfo.compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);
            createInfo.presentMode(vsync ? VK_PRESENT_MODE_FIFO_KHR : VK_PRESENT_MODE_IMMEDIATE_KHR); // Unlocked if available, otherwise would fallback
            createInfo.clipped(true);

            LongBuffer pSwapchain = stack.mallocLong(1);
            int result = vkCreateSwapchainKHR(device, createInfo, null, pSwapchain);
            if (result != VK_SUCCESS) {
                throw new RuntimeException("Impossibile creare la nv.core.Swapchain Vulkan. Codice errore: " + result);
            }
            this.swapchainHandle = pSwapchain.get(0);

            // ============================================================
            // 2. RECUPERO DELLE IMMAGINI DELLA SWAPCHAIN
            // ============================================================
            IntBuffer pImageCount = stack.mallocInt(1);
            vkGetSwapchainImagesKHR(device, swapchainHandle, pImageCount, null);
            int imageCount = pImageCount.get(0);

            LongBuffer pSwapchainImages = stack.mallocLong(imageCount);
            vkGetSwapchainImagesKHR(device, swapchainHandle, pImageCount, pSwapchainImages);

            long[] swapchainImages = new long[imageCount];
            for (int i = 0; i < imageCount; i++) {
                swapchainImages[i] = pSwapchainImages.get(i);
            }

            // ============================================================
            // 3. CREAZIONE DELLE IMAGE VIEW (Interfacce per scrivere sulle immagini)
            // ============================================================
            this.imageViews = new long[imageCount];
            for (int i = 0; i < imageCount; i++) {
                VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack);
                viewInfo.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
                viewInfo.image(swapchainImages[i]);
                viewInfo.viewType(VK_IMAGE_VIEW_TYPE_2D);
                viewInfo.format(imageFormat);

                // Mappatura dei canali colore (Identica/Standard)
                viewInfo.components().r(VK_COMPONENT_SWIZZLE_IDENTITY);
                viewInfo.components().g(VK_COMPONENT_SWIZZLE_IDENTITY);
                viewInfo.components().b(VK_COMPONENT_SWIZZLE_IDENTITY);
                viewInfo.components().a(VK_COMPONENT_SWIZZLE_IDENTITY);

                // Definiamo l'uso dell'immagine (Destinazione colore, 1 livello di Mipmap, 1 Layer)
                viewInfo.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                viewInfo.subresourceRange().baseMipLevel(0);
                viewInfo.subresourceRange().levelCount(1);
                viewInfo.subresourceRange().baseArrayLayer(0);
                viewInfo.subresourceRange().layerCount(1);

                LongBuffer pImageView = stack.mallocLong(1);
                if (vkCreateImageView(device, viewInfo, null, pImageView) != VK_SUCCESS) {
                    throw new RuntimeException("Impossibile creare la Image View all'indice: " + i);
                }
                this.imageViews[i] = pImageView.get(0);
            }
        }
    }

    // Metodi Getter richiesti da nv.core.NvContext e nv.core.GraphicsPipeline
    public long getHandle() {
        return swapchainHandle;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getFormat() {
        return imageFormat;
    }

    public long[] getImageViews() {
        return imageViews;
    }

    @Override
    public void close() {
        // Distruggiamo prima le viste logiche (Image Views)
        if (imageViews != null) {
            for (long imageView : imageViews) {
                vkDestroyImageView(device, imageView, null);
            }
        }
        // Infine abbattiamo la nv.core.Swapchain nativa sulla GPU
        vkDestroySwapchainKHR(device, swapchainHandle, null);
    }
}