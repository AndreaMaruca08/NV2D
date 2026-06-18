package nv.core;

import nv.core.annotations.EngineCore;
import nv.core.data.VulkanMemory;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPhysicalDevice;

import java.nio.ByteBuffer;

import static org.lwjgl.vulkan.VK10.*;

/**
 * <p>Buffer used to transfer data from RAM to VRAM</p>
 * @since 1.0
 * @author Andrea Maruca
 */
@EngineCore
@SuppressWarnings("unused")
public final class StagingBuffer implements AutoCloseable {

    private final VkDevice device;
    private final long bufferHandle;
    private final long memoryHandle;
    private final long size;

    public StagingBuffer(VkDevice device, VkPhysicalDevice physicalDevice, long size) {
        this.device = device;
        this.size   = size;

        long[] handles = VulkanMemory.createBuffer(
                device,
                physicalDevice,
                size,
                VK_BUFFER_USAGE_TRANSFER_SRC_BIT,  // Sorgente di trasferimento
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
        );
        this.bufferHandle = handles[0];
        this.memoryHandle = handles[1];
    }

    /**
     * Copia i byte del ByteBuffer direttamente in memoria GPU mappata.
     * Il ByteBuffer deve essere in modalità lettura (flip() già chiamato).
     */
    public void upload(ByteBuffer data) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            org.lwjgl.PointerBuffer ppData = stack.mallocPointer(1);
            vkMapMemory(device, memoryHandle, 0, size, 0, ppData);
            ByteBuffer mapped = MemoryUtil.memByteBuffer(ppData.get(0), (int) size);
            MemoryUtil.memCopy(
                    MemoryUtil.memAddress(data),
                    MemoryUtil.memAddress(mapped),
                    data.remaining()
            );
            vkUnmapMemory(device, memoryHandle);
        }
    }

    public long getHandle() { return bufferHandle; }

    @Override
    public void close() {
        vkDestroyBuffer(device, bufferHandle, null);
        vkFreeMemory(device, memoryHandle, null);
    }
}