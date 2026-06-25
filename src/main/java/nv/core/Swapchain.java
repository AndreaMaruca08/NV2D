package nv.core;

import nv.core.annotations.EngineCore;
import nv.core.errors.ex.EngineEx;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkImageViewCreateInfo;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

@EngineCore
@SuppressWarnings("unused")
public final class Swapchain implements AutoCloseable {

    private final VkDevice device;
    private final long swapchainHandle;
    private final int width;
    private final int height;
    private final int imageFormat;
    private final int imageCount;
    private final long[] images;
    private final long[] imageViews;

    public Swapchain(VkDevice device, long surfaceHandle, int width, int height, boolean vsync) {
        this.device = device;
        this.width = width;
        this.height = height;
        this.imageFormat = VK_FORMAT_B8G8R8A8_SRGB;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSwapchainCreateInfoKHR createInfo = VkSwapchainCreateInfoKHR.calloc(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR);
            createInfo.surface(surfaceHandle);
            createInfo.minImageCount(vsync ? 2 : 3);
            createInfo.imageFormat(imageFormat);
            createInfo.imageColorSpace(VK_COLOR_SPACE_SRGB_NONLINEAR_KHR);
            createInfo.imageExtent().width(width);
            createInfo.imageExtent().height(height);
            createInfo.imageArrayLayers(1);
            createInfo.imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT);
            createInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
            createInfo.preTransform(VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR);
            createInfo.compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);
            createInfo.presentMode(vsync ? VK_PRESENT_MODE_FIFO_KHR : VK_PRESENT_MODE_IMMEDIATE_KHR);
            createInfo.clipped(true);

            LongBuffer pSwapchain = stack.mallocLong(1);
            int result = vkCreateSwapchainKHR(device, createInfo, null, pSwapchain);
            if (result != VK_SUCCESS) {
                throw new EngineEx("Error creating Swapchain. Error code: " + result);
            }
            this.swapchainHandle = pSwapchain.get(0);

            // Leggi il conteggio REALE — il driver può allocarne più del minimo richiesto
            IntBuffer pImageCount = stack.mallocInt(1);
            vkGetSwapchainImagesKHR(device, swapchainHandle, pImageCount, null);
            int actualImageCount = pImageCount.get(0);
            this.imageCount = actualImageCount;

            LongBuffer pSwapchainImages = stack.mallocLong(actualImageCount);
            vkGetSwapchainImagesKHR(device, swapchainHandle, pImageCount, pSwapchainImages);

            this.images = new long[actualImageCount];
            for (int i = 0; i < actualImageCount; i++) {
                this.images[i] = pSwapchainImages.get(i);
            }

            this.imageViews = new long[actualImageCount];
            for (int i = 0; i < actualImageCount; i++) {
                VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack);
                viewInfo.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
                viewInfo.image(images[i]);
                viewInfo.viewType(VK_IMAGE_VIEW_TYPE_2D);
                viewInfo.format(imageFormat);
                viewInfo.components().r(VK_COMPONENT_SWIZZLE_IDENTITY);
                viewInfo.components().g(VK_COMPONENT_SWIZZLE_IDENTITY);
                viewInfo.components().b(VK_COMPONENT_SWIZZLE_IDENTITY);
                viewInfo.components().a(VK_COMPONENT_SWIZZLE_IDENTITY);
                viewInfo.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                viewInfo.subresourceRange().baseMipLevel(0);
                viewInfo.subresourceRange().levelCount(1);
                viewInfo.subresourceRange().baseArrayLayer(0);
                viewInfo.subresourceRange().layerCount(1);

                LongBuffer pImageView = stack.mallocLong(1);
                if (vkCreateImageView(device, viewInfo, null, pImageView) != VK_SUCCESS) {
                    throw new EngineEx("Error creating Image View at index: " + i);
                }
                this.imageViews[i] = pImageView.get(0);
            }
        }
    }

    public long getHandle()       { return swapchainHandle; }
    public int  getWidth()        { return width; }
    public int  getHeight()       { return height; }
    public int  getFormat()       { return imageFormat; }
    public int  getImageCount()   { return imageCount; }
    public long[] getImageViews() { return imageViews; }
    public long[] getImages()     { return images; }

    @Override
    public void close() {
        if (imageViews != null) {
            for (long imageView : imageViews) {
                vkDestroyImageView(device, imageView, null);
            }
        }
        vkDestroySwapchainKHR(device, swapchainHandle, null);
    }
}