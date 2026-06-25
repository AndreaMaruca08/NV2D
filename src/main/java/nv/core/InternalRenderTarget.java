package nv.core;

import nv.core.annotations.EngineCore;
import nv.core.data.VulkanMemory;
import nv.core.errors.ex.EngineEx;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

@EngineCore
@SuppressWarnings("unused")
public final class InternalRenderTarget implements AutoCloseable {

    private final VkDevice device;
    private final VkPhysicalDevice physicalDevice;
    private final int width;
    private final int height;
    private final int format;

    private final long imageHandle;
    private final long memoryHandle;
    private final long imageViewHandle;
    private final long framebufferHandle;

    public InternalRenderTarget(VkDevice device,
                                VkPhysicalDevice physicalDevice,
                                long renderPass,
                                int width,
                                int height,
                                int format) {
        this.device = device;
        this.physicalDevice = physicalDevice;
        this.width = width;
        this.height = height;
        this.format = format;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                    .imageType(VK_IMAGE_TYPE_2D)
                    .extent(e -> e.set(width, height, 1))
                    .mipLevels(1)
                    .arrayLayers(1)
                    .format(format)
                    .tiling(VK_IMAGE_TILING_OPTIMAL)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                    .usage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_TRANSFER_SRC_BIT)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
                    .samples(VK_SAMPLE_COUNT_1_BIT);

            LongBuffer pImage = stack.mallocLong(1);
            if (vkCreateImage(device, imageInfo, null, pImage) != VK_SUCCESS) {
                throw new EngineEx("Unable to create internal render target image");
            }
            this.imageHandle = pImage.get(0);

            VkMemoryRequirements memReqs = VkMemoryRequirements.malloc(stack);
            vkGetImageMemoryRequirements(device, imageHandle, memReqs);

            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    .allocationSize(memReqs.size())
                    .memoryTypeIndex(VulkanMemory.findMemoryType(
                            physicalDevice,
                            memReqs.memoryTypeBits(),
                            VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT));

            LongBuffer pMemory = stack.mallocLong(1);
            if (vkAllocateMemory(device, allocInfo, null, pMemory) != VK_SUCCESS) {
                throw new EngineEx("Unable to allocate internal render target memory");
            }
            this.memoryHandle = pMemory.get(0);
            vkBindImageMemory(device, imageHandle, memoryHandle, 0);

            VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                    .image(imageHandle)
                    .viewType(VK_IMAGE_VIEW_TYPE_2D)
                    .format(format)
                    .subresourceRange(r -> r
                            .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                            .baseMipLevel(0)
                            .levelCount(1)
                            .baseArrayLayer(0)
                            .layerCount(1));

            LongBuffer pView = stack.mallocLong(1);
            if (vkCreateImageView(device, viewInfo, null, pView) != VK_SUCCESS) {
                throw new EngineEx("Unable to create internal render target image view");
            }
            this.imageViewHandle = pView.get(0);

            VkFramebufferCreateInfo fbInfo = VkFramebufferCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                    .renderPass(renderPass)
                    .pAttachments(stack.longs(imageViewHandle))
                    .width(width)
                    .height(height)
                    .layers(1);

            LongBuffer pFb = stack.mallocLong(1);
            if (vkCreateFramebuffer(device, fbInfo, null, pFb) != VK_SUCCESS) {
                throw new EngineEx("Unable to create internal render target framebuffer");
            }
            this.framebufferHandle = pFb.get(0);
        }
    }

    public long getFramebufferHandle() {
        return framebufferHandle;
    }

    public long getImageHandle() {
        return imageHandle;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getFormat() {
        return format;
    }

    @Override
    public void close() {
        vkDestroyFramebuffer(device, framebufferHandle, null);
        vkDestroyImageView(device, imageViewHandle, null);
        vkDestroyImage(device, imageHandle, null);
        vkFreeMemory(device, memoryHandle, null);
    }
}
