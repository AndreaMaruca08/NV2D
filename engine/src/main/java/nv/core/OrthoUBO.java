package nv.core;

import nv.core.annotations.EngineCore;
import nv.core.errors.ex.EngineEx;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import java.nio.LongBuffer;
import static org.lwjgl.vulkan.VK10.*;

/**
 * @since 1.0
 * @author Andrea Maruca
 */
@EngineCore
@SuppressWarnings("unused")
public final class OrthoUBO implements AutoCloseable {

    public static final int SIZE_BYTES = 16 * Float.BYTES;

    private final VkDevice device;
    private final int imageCount;

    private final long[] bufferHandles;
    private final long[] memoryHandles;

    private final long[] mappedAddresses;

    public OrthoUBO(VkDevice device, VkPhysicalDevice physicalDevice, int imageCount) {
        this.device = device;
        this.imageCount = imageCount;
        this.bufferHandles = new long[imageCount];
        this.memoryHandles = new long[imageCount];
        this.mappedAddresses = new long[imageCount];

        for (int i = 0; i < imageCount; i++) {
            createBuffer(physicalDevice, i);
        }
    }

    private void createBuffer(VkPhysicalDevice physicalDevice, int index) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                    .size(SIZE_BYTES)
                    .usage(VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            LongBuffer pBuffer = stack.mallocLong(1);
            if (vkCreateBuffer(device, bufferInfo, null, pBuffer) != VK_SUCCESS) {
                throw new EngineEx("Error creating UBO buffer at index " + index);
            }
            bufferHandles[index] = pBuffer.get(0);

            // Interroghiamo la GPU per sapere quanta memoria serve e quale tipo usare
            VkMemoryRequirements memReqs = VkMemoryRequirements.malloc(stack);
            vkGetBufferMemoryRequirements(device, bufferHandles[index], memReqs);

            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    .allocationSize(memReqs.size())
                    .memoryTypeIndex(findMemoryType(
                            physicalDevice,
                            memReqs.memoryTypeBits(),
                            // HOST_VISIBLE: CPU può scriverci direttamente
                            // HOST_COHERENT: niente flush manuale necessario
                            VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                            stack
                    ));

            LongBuffer pMemory = stack.mallocLong(1);
            if (vkAllocateMemory(device, allocInfo, null, pMemory) != VK_SUCCESS) {
                throw new EngineEx("Error allocating UBO memory at index " + index);
            }
            memoryHandles[index] = pMemory.get(0);

            vkBindBufferMemory(device, bufferHandles[index], memoryHandles[index], 0);

            // Mappiamo la memoria una volta sola e teniamo il puntatore aperto per tutta la vita del buffer
            // (Persistent mapping: evita il costo di map/unmap ad ogni frame)
            org.lwjgl.PointerBuffer ppData = stack.mallocPointer(1);
            vkMapMemory(device, memoryHandles[index], 0, SIZE_BYTES, 0, ppData);
            mappedAddresses[index] = ppData.get(0);
        }
    }

    /**
     * Aggiorna la matrice ortografica nel buffer corrispondente all'immagine corrente.
     * Chiama questo metodo ogni frame PRIMA di submitmare il command buffer.
     *
     * @param imageIndex  indice corrente della swapchain
     * @param left        bordo sinistro (tipicamente 0)
     * @param right       bordo destro (tipicamente larghezza finestra)
     * @param bottom      bordo inferiore (tipicamente altezza finestra, asse Y verso il basso)
     * @param top         bordo superiore (tipicamente 0)
     */
    /** Updated in 1.6. */
    public void update(int imageIndex, float left, float right, float bottom, float top) {
        float near = 0.0f;
        float far  = 1.0f;
        float rml = right - left;
        float bmt = bottom - top;
        float fmn = far - near;
        long address = mappedAddresses[imageIndex];

        org.lwjgl.system.MemoryUtil.memPutFloat(address,      2.0f / rml);
        org.lwjgl.system.MemoryUtil.memPutFloat(address +  4, 0.0f);
        org.lwjgl.system.MemoryUtil.memPutFloat(address +  8, 0.0f);
        org.lwjgl.system.MemoryUtil.memPutFloat(address + 12, 0.0f);
        org.lwjgl.system.MemoryUtil.memPutFloat(address + 16, 0.0f);
        org.lwjgl.system.MemoryUtil.memPutFloat(address + 20, 2.0f / bmt);
        org.lwjgl.system.MemoryUtil.memPutFloat(address + 24, 0.0f);
        org.lwjgl.system.MemoryUtil.memPutFloat(address + 28, 0.0f);
        org.lwjgl.system.MemoryUtil.memPutFloat(address + 32, 0.0f);
        org.lwjgl.system.MemoryUtil.memPutFloat(address + 36, 0.0f);
        org.lwjgl.system.MemoryUtil.memPutFloat(address + 40, 1.0f / fmn);
        org.lwjgl.system.MemoryUtil.memPutFloat(address + 44, 0.0f);
        org.lwjgl.system.MemoryUtil.memPutFloat(address + 48, -(right + left) / rml);
        org.lwjgl.system.MemoryUtil.memPutFloat(address + 52, -(bottom + top) / bmt);
        org.lwjgl.system.MemoryUtil.memPutFloat(address + 56, -near / fmn);
        org.lwjgl.system.MemoryUtil.memPutFloat(address + 60, 1.0f);
    }

    public long getBuffer(int imageIndex) {
        return bufferHandles[imageIndex];
    }

    private int findMemoryType(VkPhysicalDevice physicalDevice, int typeFilter,
                               int properties, MemoryStack stack) {
        VkPhysicalDeviceMemoryProperties memProperties = VkPhysicalDeviceMemoryProperties.malloc(stack);
        vkGetPhysicalDeviceMemoryProperties(physicalDevice, memProperties);

        for (int i = 0; i < memProperties.memoryTypeCount(); i++) {
            boolean typeMatch = (typeFilter & (1 << i)) != 0;
            boolean propMatch = (memProperties.memoryTypes(i).propertyFlags() & properties) == properties;
            if (typeMatch && propMatch) return i;
        }
        throw new EngineEx("No compatible GPU memory type found for UBO.");
    }

    @Override
    public void close() {
        for (int i = 0; i < imageCount; i++) {
            // Unmap prima di distruggere la memoria
            if (mappedAddresses[i] != 0) {
                vkUnmapMemory(device, memoryHandles[i]);
            }
            if (bufferHandles[i] != 0) vkDestroyBuffer(device, bufferHandles[i], null);
            if (memoryHandles[i] != 0) vkFreeMemory(device, memoryHandles[i], null);
        }
    }
}
