package nv.core.data;

import nv.core.annotations.EngineCore;
import nv.core.errors.ex.EngineEx;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

@EngineCore
@SuppressWarnings("unused")
public final class DynamicVertexBuffer implements AutoCloseable {

    private final VkDevice device;
    private final long buffer;
    private final long bufferMemory;
    private final ByteBuffer mappedData;
    private final long bufferSize;

    public DynamicVertexBuffer(VkDevice device, VkPhysicalDevice physicalDevice, long sizeInBytes) {
        this.device = device;
        this.bufferSize = sizeInBytes;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                    .size(bufferSize)
                    .usage(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            LongBuffer pBuffer = stack.mallocLong(1);
            if (vkCreateBuffer(device, bufferInfo, null, pBuffer) != VK_SUCCESS) {
                throw new EngineEx("Impossible to allocate Dynamic Vertex Buffer!");
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

            LongBuffer pBufferMemory = stack.mallocLong(1);
            if (vkAllocateMemory(device, allocInfo, null, pBufferMemory) != VK_SUCCESS) {
                throw new EngineEx("Impossible to allocate memory for Dynamic Vertex Buffer!");
            }
            this.bufferMemory = pBufferMemory.get(0);

            vkBindBufferMemory(device, buffer, bufferMemory, 0);

            // Persistent mapping: la memoria rimane mappata per tutta la vita del buffer
            PointerBuffer pData = stack.mallocPointer(1);
            if (vkMapMemory(device, bufferMemory, 0, bufferSize, 0, pData) != VK_SUCCESS) {
                throw new EngineEx("Impossible to map Dynamic Vertex Buffer memory!");
            }
            this.mappedData = MemoryUtil.memByteBuffer(pData.get(0), (int) bufferSize);
        }
    }

    /**
     * Carica nuovi vertici nella GPU. Chiamabile ogni frame per geometria dinamica.
     * @param vertices array di float nel formato: x, y, r, g, b, u, v (7 float per vertice)
     */
    public void update(float[] vertices) {
        long requiredSize = (long) vertices.length * Float.BYTES;
        if (requiredSize > bufferSize) {
            throw new EngineEx(
                    "Vertices (" + requiredSize + " bytes) exceed the buffer capacity (" + bufferSize + " bytes)!");
        }
        mappedData.clear();
        mappedData.asFloatBuffer().put(vertices);
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
        throw new EngineEx("Memory type not supported on Vulkan");
    }
}
