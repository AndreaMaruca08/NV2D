package nv.core.data;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import static org.lwjgl.vulkan.VK10.*;

public class IndexBuffer implements AutoCloseable {
    
    private final VkDevice device;
    private final long bufferHandle;
    private final long memoryHandle;

    public IndexBuffer(VkDevice device, VkPhysicalDevice physicalDevice, short[] indices) {
        this.device = device;
        long bufferSize = indices.length * Short.BYTES;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            // 1. Creiamo il Buffer
            VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc(stack);
            bufferInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
            bufferInfo.size(bufferSize);
            // IMPORTANTE: Diciamo a Vulkan che questo è un Buffer di Indici!
            bufferInfo.usage(VK_BUFFER_USAGE_INDEX_BUFFER_BIT); 
            bufferInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            LongBuffer pBuffer = stack.mallocLong(1);
            if (vkCreateBuffer(device, bufferInfo, null, pBuffer) != VK_SUCCESS) {
                throw new RuntimeException("Impossibile creare l'Index Buffer");
            }
            bufferHandle = pBuffer.get(0);

            // 2. Allochiamo la memoria (semplificata, visibile dalla CPU per scriverci dentro)
            VkMemoryRequirements memRequirements = VkMemoryRequirements.calloc(stack);
            vkGetBufferMemoryRequirements(device, bufferHandle, memRequirements);

            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
            allocInfo.allocationSize(memRequirements.size());
            // Nota: Nella realtà dovresti cercare il MemoryTypeIndex corretto per VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
            // Presumo tu abbia già una funzione simile nel tuo VertexBuffer, usa quella logica qui.
            allocInfo.memoryTypeIndex(getMemoryTypeIndex(physicalDevice, memRequirements.memoryTypeBits(), VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT));

            LongBuffer pMemory = stack.mallocLong(1);
            if (vkAllocateMemory(device, allocInfo, null, pMemory) != VK_SUCCESS) {
                throw new RuntimeException("Impossibile allocare memoria per l'Index Buffer");
            }
            memoryHandle = pMemory.get(0);

            // 3. Leghiamo la memoria al buffer
            vkBindBufferMemory(device, bufferHandle, memoryHandle, 0);

            // 4. Copiamo i dati dell'array Java nella memoria della GPU
            var ppData = stack.mallocPointer(1);
            vkMapMemory(device, memoryHandle, 0, bufferSize, 0, ppData);
            
            ShortBuffer data = ppData.getShortBuffer(0, indices.length);
            data.put(indices);
            data.flip();
            
            vkUnmapMemory(device, memoryHandle);
        }
    }

    // Funzione helper per trovare il tipo di memoria (probabilmente l'hai già in VulkanApp o VertexBuffer)
    private int getMemoryTypeIndex(VkPhysicalDevice physicalDevice, int typeFilter, int properties) {
        VkPhysicalDeviceMemoryProperties memProperties = VkPhysicalDeviceMemoryProperties.calloc();
        vkGetPhysicalDeviceMemoryProperties(physicalDevice, memProperties);
        for (int i = 0; i < memProperties.memoryTypeCount(); i++) {
            if ((typeFilter & (1 << i)) != 0 && (memProperties.memoryTypes(i).propertyFlags() & properties) == properties) {
                memProperties.free();
                return i;
            }
        }
        memProperties.free();
        throw new RuntimeException("Impossibile trovare un tipo di memoria adatto!");
    }

    public long getHandle() { return bufferHandle; }

    @Override
    public void close() {
        vkDestroyBuffer(device, bufferHandle, null);
        vkFreeMemory(device, memoryHandle, null);
    }
}