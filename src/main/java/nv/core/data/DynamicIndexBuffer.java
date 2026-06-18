package nv.core.data;

import nv.core.annotations.EngineCore;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

@EngineCore
@SuppressWarnings("unused")
public final class DynamicIndexBuffer implements AutoCloseable {

    private final VkDevice device;
    private final long buffer;
    private final long bufferMemory;
    private final ByteBuffer mappedData;
    private final long bufferSize;
    private final int maxIndexCount;

    public DynamicIndexBuffer(VkDevice device, VkPhysicalDevice physicalDevice, int maxIndexCount) {
        this.device        = device;
        this.maxIndexCount = maxIndexCount;
        this.bufferSize    = (long) maxIndexCount * Integer.BYTES;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                    .size(bufferSize)
                    .usage(VK_BUFFER_USAGE_INDEX_BUFFER_BIT)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            LongBuffer pBuffer = stack.mallocLong(1);
            if (vkCreateBuffer(device, bufferInfo, null, pBuffer) != VK_SUCCESS) {
                throw new RuntimeException("Errore nella creazione del Dynamic Index Buffer!");
            }
            this.buffer = pBuffer.get(0);

            VkMemoryRequirements memRequirements = VkMemoryRequirements.calloc(stack);
            vkGetBufferMemoryRequirements(device, buffer, memRequirements);

            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    .allocationSize(memRequirements.size())
                    .memoryTypeIndex(findMemoryType(
                            physicalDevice,
                            memRequirements.memoryTypeBits(),
                            VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
                    ));

            LongBuffer pMemory = stack.mallocLong(1);
            if (vkAllocateMemory(device, allocInfo, null, pMemory) != VK_SUCCESS) {
                throw new RuntimeException("Errore nell'allocazione memoria del Dynamic Index Buffer!");
            }
            this.bufferMemory = pMemory.get(0);

            vkBindBufferMemory(device, buffer, bufferMemory, 0);

            PointerBuffer pData = stack.mallocPointer(1);
            if (vkMapMemory(device, bufferMemory, 0, bufferSize, 0, pData) != VK_SUCCESS) {
                throw new RuntimeException("Impossibile mappare la memoria del Dynamic Index Buffer!");
            }
            this.mappedData = MemoryUtil.memByteBuffer(pData.get(0), (int) bufferSize);
        }
    }

    /**
     * Carica nuovi indici nella GPU. Chiamabile ogni frame.
     * @return il numero di indici effettivamente caricati, da passare a vkCmdDrawIndexed
     */
    public int update(int[] indices) {
        if (indices.length > maxIndexCount) {
            throw new IllegalArgumentException(
                    "Indici (" + indices.length + ") superano la capacità del buffer (" + maxIndexCount + ")!");
        }

        mappedData.clear();
        mappedData.asIntBuffer().put(indices);

        return indices.length;
    }

    public int getMaxIndexCount() {
        return maxIndexCount;
    }

    public long getHandle() {
        return buffer;
    }

    @Override
    public void close() {
        vkUnmapMemory(device, bufferMemory);
        vkDestroyBuffer(device, buffer, null);
        vkFreeMemory(device, bufferMemory, null);
    }

    private int findMemoryType(VkPhysicalDevice physicalDevice, int typeFilter, int properties) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceMemoryProperties memProperties = VkPhysicalDeviceMemoryProperties.calloc(stack);
            vkGetPhysicalDeviceMemoryProperties(physicalDevice, memProperties);

            for (int i = 0; i < memProperties.memoryTypeCount(); i++) {
                if ((typeFilter & (1 << i)) != 0
                        && (memProperties.memoryTypes(i).propertyFlags() & properties) == properties) {
                    return i;
                }
            }
        }

        throw new RuntimeException("Tipo di memoria Vulkan non supportato!");
    }
}