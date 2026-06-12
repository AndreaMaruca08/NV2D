import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class CommandBuffers {

    private final VkDevice device;
    private final long commandPoolHandle;
    private final VkCommandBuffer commandBuffer; // Gestito come final per consistenza dell'oggetto

    public CommandBuffers(VkDevice device, GraphicsPipeline pipeline, Swapchain swapchain) {
        this.device = device;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            // 1. Creazione del Command Pool (L'allocatore di memoria per i comandi sulla GPU)
            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
            poolInfo.queueFamilyIndex(0); // Assumiamo la coda grafica principale 0
            poolInfo.flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT); // Permette di svuotare e riscrivere il buffer

            LongBuffer pCommandPool = stack.mallocLong(1);
            if (vkCreateCommandPool(device, poolInfo, null, pCommandPool) != VK_SUCCESS) {
                throw new RuntimeException("Errore nella creazione del Command Pool");
            }
            this.commandPoolHandle = pCommandPool.get(0);

            // 2. Allocazione del Command Buffer effettivo dal Pool
            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            allocInfo.commandPool(commandPoolHandle);
            allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            allocInfo.commandBufferCount(1);

            PointerBuffer pCommandBuffer = stack.mallocPointer(1);
            if (vkAllocateCommandBuffers(device, allocInfo, pCommandBuffer) != VK_SUCCESS) {
                throw new RuntimeException("Errore nell'allocazione del Command Buffer");
            }

            // Inizializziamo l'istanza del buffer agganciandola al puntatore nativo ottenuto
            this.commandBuffer = new VkCommandBuffer(pCommandBuffer.get(0), device);
        }
    }

    /**
     * Registra la lista di comandi grafici da inviare alla GPU.
     * * @param renderPass L'handle nativo del Render Pass
     * @param framebuffer L'handle del Framebuffer associato all'immagine corrente della Swapchain
     * @param pipelineHandle L'handle della GraphicsPipeline compilata
     * @param vertexBufferHandle L'handle del VertexBuffer contenente i dati geometrici
     * @param width Larghezza della finestra
     * @param height Altezza della finestra
     */
    public void record(long renderPass, long framebuffer, long pipelineHandle, long vertexBufferHandle, int width, int height) {
        try (MemoryStack stack = MemoryStack.stackPush()) {

            // A. Inizio registrazione del Command Buffer
            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
            beginInfo.flags(VK_COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT); // Sicuro per il riutilizzo continuo

            int result = vkBeginCommandBuffer(commandBuffer, beginInfo);
            if (result != VK_SUCCESS) {
                throw new RuntimeException("Impossibile iniziare la registrazione del Command Buffer. Errore: " + result);
            }

            // B. Configurazione del Render Pass (Pulisce lo schermo prima di disegnare)
            VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.calloc(stack);
            renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO);
            renderPassInfo.renderPass(renderPass);
            renderPassInfo.framebuffer(framebuffer);
            renderPassInfo.renderArea().offset().set(0, 0);
            renderPassInfo.renderArea().extent().set(width, height);

            // Impostiamo il colore di sfondo (es. un grigio scuro antiriflesso: RGBA)
            VkClearValue.Buffer clearValues = VkClearValue.calloc(1, stack);
            clearValues.color().float32(0, 0.1f); // R
            clearValues.color().float32(1, 0.1f); // G
            clearValues.color().float32(2, 0.1f); // B
            clearValues.color().float32(3, 1.0f); // A
            renderPassInfo.pClearValues(clearValues);

            // C. Esecuzione dei comandi grafici veri e propri
            vkCmdBeginRenderPass(commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);

            // 1. Attiviamo la pipeline grafica (stati hardware + shader)
            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineHandle);

            // 2. Agganciamo fisicamente il Vertex Buffer alla GPU
            LongBuffer buffers = stack.longs(vertexBufferHandle);
            LongBuffer offsets = stack.longs(0); // Legge dall'inizio del buffer (offset 0)
            vkCmdBindVertexBuffers(commandBuffer, 0, buffers, offsets);

            // 3. COMANDO DI DISEGNO AVANZATO: 3 vertici, 1 istanza, partendo dal vertice 0
            vkCmdDraw(commandBuffer, 3, 1, 0, 0);

            vkCmdEndRenderPass(commandBuffer);

            // D. Chiudiamo la registrazione del buffer
            result = vkEndCommandBuffer(commandBuffer);
            if (result != VK_SUCCESS) {
                throw new RuntimeException("Impossibile terminare la registrazione del Command Buffer. Errore: " + result);
            }
        }
    }

    /**
     * Restituisce l'oggetto di comando pronto per essere sottomesso alla VkQueue nel ciclo principale.
     */
    public VkCommandBuffer getCommandBuffer() {
        return commandBuffer;
    }

    /**
     * Distrugge il Pool principale, liberando istantaneamente la memoria di tutti i buffer associati.
     */
    public void free() {
        vkDestroyCommandPool(device, commandPoolHandle, null);
    }
}