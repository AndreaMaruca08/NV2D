package data;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class VertexBuffer implements AutoCloseable {

    private final VkDevice device;
    private final long bufferHandle;
    private final long memoryHandle;

    public VertexBuffer(VkDevice device, VkPhysicalDevice physicalDevice, float[] vertexData) {
        this.device = device;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            long bufferSize = (long) vertexData.length * Float.BYTES;

            // 1. Creazione del Buffer Logico
            VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc(stack);
            bufferInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
            bufferInfo.size(bufferSize);
            bufferInfo.usage(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT); // Diciamo che conterrà vertici
            bufferInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            LongBuffer pBuffer = stack.mallocLong(1);
            if (vkCreateBuffer(device, bufferInfo, null, pBuffer) != VK_SUCCESS) {
                throw new RuntimeException("Impossibile creare il Vertex Buffer");
            }
            this.bufferHandle = pBuffer.get(0);

            // 2. Richiesta dei requisiti di memoria alla GPU
            VkMemoryRequirements memRequirements = VkMemoryRequirements.malloc(stack);
            vkGetBufferMemoryRequirements(device, bufferHandle, memRequirements);

            // 3. Allocazione della memoria fisica sulla GPU
            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
            allocInfo.allocationSize(memRequirements.size());
            
            // Cerchiamo una memoria che la CPU possa scrivere (HOST_VISIBLE) e che sia sincrona (HOST_COHERENT)
            allocInfo.memoryTypeIndex(findMemoryType(physicalDevice, memRequirements.memoryTypeBits(),
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT));

            LongBuffer pBufferMemory = stack.mallocLong(1);
            if (vkAllocateMemory(device, allocInfo, null, pBufferMemory) != VK_SUCCESS) {
                throw new RuntimeException("Impossibile allocare la memoria per il Vertex Buffer");
            }
            this.memoryHandle = pBufferMemory.get(0);

            // 4. Sposiamo il Buffer con la sua Memoria Fisica
            vkBindBufferMemory(device, bufferHandle, memoryHandle, 0);

            // 5. MAPPATURA: Copiamo i dati Java (RAM) nella memoria GPU (VRAM)
            PointerBuffer pData = stack.mallocPointer(1); // Alloca un puntatore vuoto
            vkMapMemory(device, memoryHandle, 0, bufferSize, 0, pData);
            
            // Creiamo un FloatBuffer virtuale che punta direttamente alla memoria della GPU
            FloatBuffer data = pData.getFloatBuffer(0, vertexData.length);
            data.put(vertexData); // Copia i dati
            
            vkUnmapMemory(device, memoryHandle); // Chiudiamo la mappatura
        }
    }

    public long getHandle() { return bufferHandle; }

    // Algoritmo helper per chiedere alla GPU quale dei suoi banchi di RAM supporta i nostri requisiti
    private int findMemoryType(VkPhysicalDevice physicalDevice, int typeFilter, int properties) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceMemoryProperties memProperties = VkPhysicalDeviceMemoryProperties.malloc(stack);
            vkGetPhysicalDeviceMemoryProperties(physicalDevice, memProperties);

            for (int i = 0; i < memProperties.memoryTypeCount(); i++) {
                if ((typeFilter & (1 << i)) != 0 && 
                    (memProperties.memoryTypes(i).propertyFlags() & properties) == properties) {
                    return i;
                }
            }
            throw new RuntimeException("Impossibile trovare un tipo di memoria GPU adatto!");
        }
    }

    @Override
    public void close() {
        vkDestroyBuffer(device, bufferHandle, null);
        vkFreeMemory(device, memoryHandle, null);
    }
}