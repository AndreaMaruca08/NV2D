import data.VertexBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.*;
import org.lwjgl.vulkan.*;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanApp implements Runnable {

    private long window; // Puntatore nativo alla finestra GLFW
    private VkInstance instance;
    private VkDevice device;
    private long surface; // Connette la finestra a Vulkan
    private Swapchain swapchain;
    private GraphicsPipeline pipeline;
    private CommandBuffers commandBuffers;
    private VkPhysicalDevice physicalDevice;
    private VkQueue graphicsQueue;
    private long renderPass;
    private long[] framebuffers;

    // Il nostro Vertex Buffer geometrico
    private VertexBuffer vertexBuffer;

    // Oggetti di Sincronizzazione nativi (CPU <-> GPU)
    private long imageAvailableSemaphore;
    private long renderFinishedSemaphore;
    private long inFlightFence;

    // Array Interleaved dei vertici: X, Y, R, G, B
    private final float[] vertices = {
            0.0f, -0.5f,  1.0f, 0.0f, 0.0f, // Vertice 1: Alto Centro (Rosso)
            0.5f,  0.5f,  0.0f, 1.0f, 0.0f, // Vertice 2: Basso Destra (Verde)
            -0.5f,  0.5f,  0.0f, 0.0f, 1.0f  // Vertice 3: Basso Sinistra (Blu)
    };

    @Override
    public void run() {
        initWindow();
        initVulkan();
        mainLoop();
        cleanup();
    }

    private void initWindow() {
        if (!glfwInit()) {
            throw new IllegalStateException("Impossibile inizializzare GLFW");
        }

        // Configurazione per Vulkan: disattiviamo OpenGL
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

        window = glfwCreateWindow(800, 600, "Il mio primo triangolo Vulkan", 0, 0);
        if (window == 0) {
            throw new RuntimeException("Impossibile creare la finestra GLFW");
        }
    }

    private void initVulkan() {
        createInstance();
        createSurface();
        pickPhysicalDeviceAndCreateLogicalDevice();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);

            // Chiediamo a GLFW la dimensione reale del frame buffer (in pixel)
            glfwGetFramebufferSize(window, pWidth, pHeight);

            int width = pWidth.get(0);
            int height = pHeight.get(0);

            this.swapchain = new Swapchain(device, surface, width, height);
        }

        // Creazione dell'architettura grafica nell'ordine corretto
        createRenderPass();
        this.pipeline = new GraphicsPipeline(device, swapchain, renderPass);
        createFramebuffers();

        // Istanziamo il Vertex Buffer iniettando l'array dei vertici sulla GPU
        this.vertexBuffer = new VertexBuffer(device, physicalDevice, vertices);

        // Inizializziamo l'allocatore dei comandi
        this.commandBuffers = new CommandBuffers(device, pipeline, swapchain);

        // Creiamo i semafori di sincronizzazione prima di iniziare il loop
        createSyncObjects();
    }

    private void createSyncObjects() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.calloc(stack);
            semaphoreInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

            VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.calloc(stack);
            fenceInfo.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
            fenceInfo.flags(VK_FENCE_CREATE_SIGNALED_BIT); // Inizia segnalato per sbloccare subito il primo frame

            LongBuffer pImageAvailableSemaphore = stack.mallocLong(1);
            LongBuffer pRenderFinishedSemaphore = stack.mallocLong(1);
            LongBuffer pInFlightFence = stack.mallocLong(1);

            if (vkCreateSemaphore(device, semaphoreInfo, null, pImageAvailableSemaphore) != VK_SUCCESS ||
                    vkCreateSemaphore(device, semaphoreInfo, null, pRenderFinishedSemaphore) != VK_SUCCESS ||
                    vkCreateFence(device, fenceInfo, null, pInFlightFence) != VK_SUCCESS) {
                throw new RuntimeException("Impossibile creare gli oggetti di sincronizzazione hardware.");
            }

            this.imageAvailableSemaphore = pImageAvailableSemaphore.get(0);
            this.renderFinishedSemaphore = pRenderFinishedSemaphore.get(0);
            this.inFlightFence = pInFlightFence.get(0);
        }
    }

    private void mainLoop() {
        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();
            drawFrame();
        }

        // Attende che la GPU abbia finito di elaborare tutto prima di distruggere le risorse
        vkDeviceWaitIdle(device);
    }

    private void drawFrame() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // 1. Aspettiamo che la GPU abbia completato il frame precedente prima di riusare il buffer
            vkWaitForFences(device, inFlightFence, true, Long.MAX_VALUE);
            vkResetFences(device, inFlightFence);

            // 2. Chiediamo alla swapchain l'indice della prossima immagine disponibile sullo schermo
            IntBuffer pImageIndex = stack.mallocInt(1);
            vkAcquireNextImageKHR(device, swapchain.getHandle(), Long.MAX_VALUE, imageAvailableSemaphore, VK_NULL_HANDLE, pImageIndex);
            int imageIndex = pImageIndex.get(0);

            // 3. Registriamo i comandi grafici dinamici per l'immagine corrente della swapchain
            commandBuffers.record(
                    renderPass,
                    framebuffers[imageIndex],
                    pipeline.getHandle(),
                    vertexBuffer.getHandle(),
                    swapchain.getWidth(),
                    swapchain.getHeight()
            );

            // 4. Inviamo il Command Buffer registrato alla coda della GPU (Submit)
            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);

            // Sincronizzazione: Aspetta che l'immagine sia disponibile prima di scriverci il colore sopra
            LongBuffer waitSemaphores = stack.longs(imageAvailableSemaphore);
            IntBuffer waitStages = stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
            submitInfo.pWaitSemaphores(waitSemaphores);
            submitInfo.pWaitDstStageMask(waitStages);

            PointerBuffer pCommandBuffers = stack.pointers(commandBuffers.getCommandBuffer());
            submitInfo.pCommandBuffers(pCommandBuffers);

            // Sincronizzazione: Segnala questo semaforo quando la GPU ha finito di disegnare il triangolo
            LongBuffer signalSemaphores = stack.longs(renderFinishedSemaphore);
            submitInfo.pSignalSemaphores(signalSemaphores);

            if (vkQueueSubmit(graphicsQueue, submitInfo, inFlightFence) != VK_SUCCESS) {
                throw new RuntimeException("Errore nell'invio (Submit) della lista comandi alla GPU!");
            }

            // 5. Presentiamo l'immagine renderizzata sul display del Mac
            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack);
            presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);
            presentInfo.pWaitSemaphores(signalSemaphores); // Aspetta che il rendering sia terminato

            LongBuffer swapchains = stack.longs(swapchain.getHandle());
            presentInfo.swapchainCount(1);
            presentInfo.pSwapchains(swapchains);
            presentInfo.pImageIndices(pImageIndex);

            vkQueuePresentKHR(graphicsQueue, presentInfo);
        }
    }

    private void cleanup() {
        // Deallocazione esplicita al contrario rispetto all'ordine di creazione
        if (device != null) {
            vkDestroySemaphore(device, imageAvailableSemaphore, null);
            vkDestroySemaphore(device, renderFinishedSemaphore, null);
            vkDestroyFence(device, inFlightFence, null);
        }

        if (commandBuffers != null) commandBuffers.free();

        if (framebuffers != null) {
            for (long framebuffer : framebuffers) {
                vkDestroyFramebuffer(device, framebuffer, null);
            }
        }

        if (pipeline != null) pipeline.close();

        if (renderPass != 0) {
            vkDestroyRenderPass(device, renderPass, null);
        }

        if (vertexBuffer != null) vertexBuffer.close();
        if (swapchain != null) swapchain.close();

        if (surface != 0) vkDestroySurfaceKHR(instance, surface, null);
        if (device != null) vkDestroyDevice(device, null);
        if (instance != null) vkDestroyInstance(instance, null);

        glfwDestroyWindow(window);
        glfwTerminate();
    }

    private void createInstance() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack);
            appInfo.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO);
            appInfo.pApplicationName(stack.UTF8("Vulkan Java App"));
            appInfo.applicationVersion(VK_MAKE_VERSION(1, 0, 0));
            appInfo.pEngineName(stack.UTF8("No Engine"));
            appInfo.engineVersion(VK_MAKE_VERSION(1, 0, 0));
            appInfo.apiVersion(VK_API_VERSION_1_0);

            VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
            createInfo.pApplicationInfo(appInfo);

            PointerBuffer glfwExtensions = GLFWVulkan.glfwGetRequiredInstanceExtensions();
            if (glfwExtensions == null) {
                throw new RuntimeException("GLFW non è riuscito a trovare le estensioni Vulkan necessarie.");
            }

            createInfo.ppEnabledExtensionNames(glfwExtensions);

            PointerBuffer pInstance = stack.mallocPointer(1);
            int result = vkCreateInstance(createInfo, null, pInstance);
            if (result != VK_SUCCESS) {
                throw new RuntimeException("Impossibile creare l'istanza Vulkan. Codice errore: " + result);
            }

            this.instance = new VkInstance(pInstance.get(0), createInfo);
        }
    }

    private void createSurface() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pSurface = stack.mallocLong(1);
            int result = GLFWVulkan.glfwCreateWindowSurface(instance, window, null, pSurface);
            if (result != VK_SUCCESS) {
                throw new RuntimeException("Impossibile creare la superficie della finestra. Errore: " + result);
            }
            this.surface = pSurface.get(0);
        }
    }

    private void createRenderPass() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkAttachmentDescription.Buffer colorAttachment = VkAttachmentDescription.calloc(1, stack);
            colorAttachment.format(swapchain.getFormat());
            colorAttachment.samples(VK_SAMPLE_COUNT_1_BIT);
            colorAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
            colorAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
            colorAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
            colorAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
            colorAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            colorAttachment.finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

            VkAttachmentReference.Buffer colorAttachmentRef = VkAttachmentReference.calloc(1, stack);
            colorAttachmentRef.attachment(0);
            colorAttachmentRef.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

            VkSubpassDescription.Buffer subpass = VkSubpassDescription.calloc(1, stack);
            subpass.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS);
            subpass.colorAttachmentCount(1);
            subpass.pColorAttachments(colorAttachmentRef);

            VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc(stack);
            renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO);
            renderPassInfo.pAttachments(colorAttachment);
            renderPassInfo.pSubpasses(subpass);

            LongBuffer pRenderPass = stack.mallocLong(1);
            if (vkCreateRenderPass(device, renderPassInfo, null, pRenderPass) != VK_SUCCESS) {
                throw new RuntimeException("Impossibile creare il Render Pass nativo");
            }
            this.renderPass = pRenderPass.get(0);
        }
    }

    private void createFramebuffers() {
        long[] imageViews = swapchain.getImageViews();
        this.framebuffers = new long[imageViews.length];

        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer attachments = stack.mallocLong(1);

            VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.calloc(stack);
            framebufferInfo.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO);
            framebufferInfo.renderPass(renderPass);
            framebufferInfo.width(swapchain.getWidth());
            framebufferInfo.height(swapchain.getHeight());
            framebufferInfo.layers(1);

            for (int i = 0; i < imageViews.length; i++) {
                attachments.put(0, imageViews[i]);
                framebufferInfo.pAttachments(attachments);

                LongBuffer pFramebuffer = stack.mallocLong(1);
                if (vkCreateFramebuffer(device, framebufferInfo, null, pFramebuffer) != VK_SUCCESS) {
                    throw new RuntimeException("Impossibile creare il Framebuffer all'indice: " + i);
                }
                this.framebuffers[i] = pFramebuffer.get(0);
            }
        }
    }

    private void pickPhysicalDeviceAndCreateLogicalDevice() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pDeviceCount = stack.mallocInt(1);
            vkEnumeratePhysicalDevices(instance, pDeviceCount, null);
            if (pDeviceCount.get(0) == 0) {
                throw new RuntimeException("Nessuna GPU compatibile con Vulkan trovata!");
            }

            PointerBuffer pPhysicalDevices = stack.mallocPointer(pDeviceCount.get(0));
            vkEnumeratePhysicalDevices(instance, pDeviceCount, pPhysicalDevices);
            this.physicalDevice = new VkPhysicalDevice(pPhysicalDevices.get(0), instance);

            IntBuffer pQueueFamilyCount = stack.mallocInt(1);
            vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, pQueueFamilyCount, null);
            VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.calloc(pQueueFamilyCount.get(0), stack);
            vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, pQueueFamilyCount, queueFamilies);

            int graphicsQueueFamilyIndex = -1;
            for (int i = 0; i < queueFamilies.capacity(); i++) {
                if ((queueFamilies.get(i).queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0) {
                    graphicsQueueFamilyIndex = i;
                    break;
                }
            }
            if (graphicsQueueFamilyIndex == -1) {
                throw new RuntimeException("Impossibile trovare una coda grafica sulla GPU.");
            }

            VkDeviceQueueCreateInfo.Buffer queueCreateInfo = VkDeviceQueueCreateInfo.calloc(1, stack);
            queueCreateInfo.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
            queueCreateInfo.queueFamilyIndex(graphicsQueueFamilyIndex);
            queueCreateInfo.pQueuePriorities(stack.floats(1.0f));

            PointerBuffer deviceExtensions = stack.mallocPointer(2);
            deviceExtensions.put(stack.UTF8(VK_KHR_SWAPCHAIN_EXTENSION_NAME));
            deviceExtensions.put(stack.UTF8("VK_KHR_portability_subset")); // Requisito fondamentale MoltenVK Mac
            deviceExtensions.flip();

            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
            createInfo.pQueueCreateInfos(queueCreateInfo);
            createInfo.ppEnabledExtensionNames(deviceExtensions);

            PointerBuffer pDevice = stack.mallocPointer(1);
            int result = vkCreateDevice(physicalDevice, createInfo, null, pDevice);
            if (result != VK_SUCCESS) {
                throw new RuntimeException("Impossibile creare il Logical Device. Errore: " + result);
            }

            this.device = new VkDevice(pDevice.get(0), physicalDevice, createInfo);

            PointerBuffer pQueue = stack.mallocPointer(1);
            vkGetDeviceQueue(device, graphicsQueueFamilyIndex, 0, pQueue);
            this.graphicsQueue = new VkQueue(pQueue.get(0), device);
        }
    }
}