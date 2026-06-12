package nv.core;

import nv.core.data.VulkanMemory;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;
import java.nio.ByteBuffer;

import static org.lwjgl.vulkan.VK10.*;

/**
 * Buffer temporaneo CPU-visible usato per trasferire dati dalla RAM alla VRAM.
 *
 * Vulkan non permette scrittura diretta su VkImage ottimizzate per la GPU
 * (DEVICE_LOCAL). Il pattern corretto è:
 * <p>
 *   RAM (Java byte[])
 *     → StagingBuffer (HOST_VISIBLE, HOST_COHERENT)
 *       → vkCmdCopyBufferToImage (comando GPU)
 *         → VkImage (DEVICE_LOCAL, solo GPU)
 * </p>
 * Questo oggetto viene creato, usato per la copia, poi subito distrutto.
 */
public class StagingBuffer implements AutoCloseable {

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