package nv.core.gpu;

import nv.core.annotations.EngineCore;
import nv.core.data.VulkanMemory;
import nv.core.errors.ex.EngineEx;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPhysicalDevice;

import java.nio.ByteBuffer;

import static org.lwjgl.vulkan.VK10.*;

/** A device-local storage buffer managed by {@link GpuCompute}. */
@EngineCore
public final class GpuBuffer implements AutoCloseable {
    private final VkDevice device;
    private final long size;
    private final long handle;
    private final long memory;
    private final int queueFamily;

    GpuBuffer(VkDevice device, VkPhysicalDevice physicalDevice, long size, int queueFamily) {
        if (size <= 0 || size > Integer.MAX_VALUE) {
            throw new EngineEx("GPU buffer size must be between 1 and Integer.MAX_VALUE bytes");
        }
        this.device = device;
        this.size = size;
        this.queueFamily = queueFamily;
        long[] handles = VulkanMemory.createBuffer(device, physicalDevice, size,
                VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_SRC_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT,
                VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
        handle = handles[0];
        memory = handles[1];
    }

    public long size() { return size; }
    public long handle() { return handle; }

    /** Uploads the remaining bytes in {@code data} to this buffer. */
    public void upload(ByteBuffer data, VkPhysicalDevice physicalDevice, org.lwjgl.vulkan.VkQueue queue) {
        if (data == null || data.remaining() > size) throw new EngineEx("Data does not fit in GPU buffer");
        long[] staging = VulkanMemory.createBuffer(device, physicalDevice, size,
                VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
        try {
            copyToMapped(staging[1], data);
            GpuCompute.copyBuffer(device, queue, queueFamily, staging[0], handle, data.remaining());
        } finally {
            vkDestroyBuffer(device, staging[0], null);
            vkFreeMemory(device, staging[1], null);
        }
    }

    /** Downloads the complete buffer contents into a native byte buffer. */
    public ByteBuffer download(VkPhysicalDevice physicalDevice, org.lwjgl.vulkan.VkQueue queue) {
        long[] staging = VulkanMemory.createBuffer(device, physicalDevice, size,
                VK_BUFFER_USAGE_TRANSFER_DST_BIT,
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
        try {
            GpuCompute.copyBuffer(device, queue, queueFamily, handle, staging[0], size);
            ByteBuffer result = MemoryUtil.memAlloc((int) size);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer mapped = stack.mallocPointer(1);
                if (vkMapMemory(device, staging[1], 0, size, 0, mapped) != VK_SUCCESS) {
                    throw new EngineEx("Unable to map GPU buffer for download");
                }
                MemoryUtil.memCopy(mapped.get(0), MemoryUtil.memAddress(result), size);
                vkUnmapMemory(device, staging[1]);
            }
            // memCopy does not advance the ByteBuffer position.
            result.position(0).limit((int) size);
            return result;
        } finally {
            vkDestroyBuffer(device, staging[0], null);
            vkFreeMemory(device, staging[1], null);
        }
    }

    private void copyToMapped(long stagingMemory, ByteBuffer data) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer mapped = stack.mallocPointer(1);
            if (vkMapMemory(device, stagingMemory, 0, size, 0, mapped) != VK_SUCCESS)
                throw new EngineEx("Unable to map GPU buffer for upload");
            MemoryUtil.memCopy(MemoryUtil.memAddress(data), mapped.get(0), data.remaining());
            vkUnmapMemory(device, stagingMemory);
        }
    }

    @Override public void close() {
        vkDestroyBuffer(device, handle, null);
        vkFreeMemory(device, memory, null);
    }
}
