package nv.core.data;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

/**
 * Utility statica per operazioni di memoria Vulkan condivise tra tutti i buffer.
 * Evita la duplicazione di findMemoryType in VertexBuffer, IndexBuffer, OrthoUBO, ecc.
 */
public final class VulkanMemory {

    private VulkanMemory() {}

    /**
     * Trova il tipo di memoria GPU che soddisfa i requisiti richiesti.
     *
     * @param physicalDevice  GPU fisica
     * @param typeFilter      bitmask restituita da vkGetBufferMemoryRequirements
     * @param properties      flag richiesti (es. HOST_VISIBLE | HOST_COHERENT)
     * @return indice del tipo di memoria compatibile
     */
    public static int findMemoryType(VkPhysicalDevice physicalDevice,
                                     int typeFilter,
                                     int properties) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceMemoryProperties memProps =
                    VkPhysicalDeviceMemoryProperties.malloc(stack);
            vkGetPhysicalDeviceMemoryProperties(physicalDevice, memProps);

            for (int i = 0; i < memProps.memoryTypeCount(); i++) {
                boolean typeMatch = (typeFilter & (1 << i)) != 0;
                boolean propMatch = (memProps.memoryTypes(i).propertyFlags() & properties) == properties;
                if (typeMatch && propMatch) return i;
            }
        }
        throw new RuntimeException(
                "Nessun tipo di memoria GPU compatibile trovato. " +
                "typeFilter=" + typeFilter + " properties=" + properties);
    }

    /**
     * Alloca un VkBuffer generico con memoria associata.
     * Restituisce long[2]: [0] = bufferHandle, [1] = memoryHandle
     *
     * @param device         logical device
     * @param physicalDevice physical device
     * @param size           dimensione in byte
     * @param usage          VK_BUFFER_USAGE_* flags
     * @param memProperties  VK_MEMORY_PROPERTY_* flags
     */
    public static long[] createBuffer(VkDevice device,
                                      VkPhysicalDevice physicalDevice,
                                      long size,
                                      int usage,
                                      int memProperties) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                    .size(size)
                    .usage(usage)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            LongBuffer pBuffer = stack.mallocLong(1);
            if (vkCreateBuffer(device, bufferInfo, null, pBuffer) != VK_SUCCESS) {
                throw new RuntimeException("Impossibile creare il buffer Vulkan (usage=" + usage + ")");
            }
            long bufferHandle = pBuffer.get(0);

            VkMemoryRequirements memReqs = VkMemoryRequirements.malloc(stack);
            vkGetBufferMemoryRequirements(device, bufferHandle, memReqs);

            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    .allocationSize(memReqs.size())
                    .memoryTypeIndex(findMemoryType(physicalDevice,
                            memReqs.memoryTypeBits(), memProperties));

            LongBuffer pMemory = stack.mallocLong(1);
            if (vkAllocateMemory(device, allocInfo, null, pMemory) != VK_SUCCESS) {
                throw new RuntimeException("Impossibile allocare memoria GPU (usage=" + usage + ")");
            }
            long memoryHandle = pMemory.get(0);

            vkBindBufferMemory(device, bufferHandle, memoryHandle, 0);

            return new long[]{ bufferHandle, memoryHandle };
        }
    }
}