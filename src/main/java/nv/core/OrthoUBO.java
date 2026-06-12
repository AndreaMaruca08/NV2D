package nv.core;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import java.nio.LongBuffer;
import static org.lwjgl.vulkan.VK10.*;

/**
 * Gestisce un Uniform Buffer Object contenente la matrice di proiezione ortografica.
 * Alloca un buffer per ogni immagine della swapchain per evitare race conditions GPU/CPU.
 */
public class OrthoUBO implements AutoCloseable {

    // Dimensione del payload: mat4 = 16 float = 64 byte
    public static final int SIZE_BYTES = 16 * Float.BYTES;

    private final VkDevice device;
    private final int imageCount;

    private final long[] bufferHandles;
    private final long[] memoryHandles;

    // Indirizzi mappati in memoria host-visible (scrittura diretta senza staging)
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
                throw new RuntimeException("Impossibile creare il buffer UBO all'indice " + index);
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
                throw new RuntimeException("Impossibile allocare memoria UBO all'indice " + index);
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
    public void update(int imageIndex, float left, float right, float bottom, float top) {
        // Matrice ortografica column-major (convenzione GLSL/Vulkan)
        // Mappa il rettangolo [left,right] x [top,bottom] nel cubo NDC [-1,1]^3
        float[] ortho = buildOrthoMatrix(left, right, bottom, top);

        // Scrittura diretta in memoria GPU tramite indirizzo mappato (zero-copy)
        org.lwjgl.system.MemoryUtil.memFloatBuffer(mappedAddresses[imageIndex], 16)
                .put(ortho)
                .flip();
    }

    /**
     * Costruisce una matrice di proiezione ortografica 4x4 column-major.
     * Clip space Vulkan: X in [-1,1], Y in [-1,1], Z in [0,1] (depth range diverso da OpenGL).
     */
    private float[] buildOrthoMatrix(float left, float right, float bottom, float top) {
        float near = 0.0f;
        float far  = 1.0f;

        float rml = right - left;   // right minus left
        float tmb = top   - bottom; // top minus bottom
        float fmn = far   - near;   // far  minus near

        // Matrice column-major: [col0_row0, col0_row1, col0_row2, col0_row3, col1_row0, ...]
        return new float[] {
                2.0f / rml,           0.0f,                 0.0f,          0.0f,  // colonna 0
                0.0f,                 2.0f / tmb,           0.0f,          0.0f,  // colonna 1
                0.0f,                 0.0f,                 1.0f / fmn,    0.0f,  // colonna 2
                -(right + left) / rml, -(top + bottom) / tmb, -near / fmn, 1.0f  // colonna 3
        };
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
        throw new RuntimeException("Nessun tipo di memoria GPU compatibile trovato per l'UBO.");
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