package nv.core;

import nv.core.annotations.EngineCore;
import nv.core.assets.AssetsManager;
import nv.core.collision.CollisionManager;
import nv.core.components.NvComp;
import nv.core.components.NvCont;
import nv.core.data.*;
import nv.core.data.DescriptorManager;
import nv.core.errors.NvLogger;
import nv.core.errors.ex.EngineEx;
import nv.core.graphic.NvGraphic;
import nv.core.graphic.NvPixelGraphic;
import nv.core.io.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.*;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

import static nv.core.errors.NvLogger.logEngine;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

/**
 * <h3>Entry point for the NV2D game engine</h3>
 * <p>SingleTone class responsible for managing the application's Vulkan resources and rendering pipeline</p>
 * @since 1.0
 * @author Andrea Maruca
 */
@EngineCore
@SuppressWarnings("unused")
public final class NvContext implements Runnable {
    private static final int MAJOR_VERSION = 1;
    private static final int MINOR_VERSION = 2;
    private static final int PATCH = 0;
    private static final String ENGINE_NAME = "NV2D";

    private long window;
    private VkInstance instance;
    private VkDevice device;
    private long surface;
    private Swapchain swapchain;
    private GraphicsPipeline pipeline;
    private TexturePipeline texturePipeline;
    private CommandBuffers commandBuffers;
    private VkPhysicalDevice physicalDevice;
    private VkQueue graphicsQueue;
    private long renderPass;
    private InternalRenderTarget internalRenderTarget;
    private UpdateCycle currentCameraUpdateCycle;

    private final int MAX_VERTICES;
    private final int MAX_INDICES;

    private static final int DEF_MAX_VERTICES = 500_000;
    private static final int DEF_MAX_INDICES  = 850_000;

    private static final int MAX_TEXTURES = 15;
    private final NvImage[] loadedTextures = new NvImage[MAX_TEXTURES];
    private int textureCount = 1;

    public synchronized int getNextTextureSlot() {
        if (textureCount >= MAX_TEXTURES) {
            throw new EngineEx("Max texture slots reached (" + MAX_TEXTURES + ")");
        }
        return textureCount++;
    }

    // Geometria dinamica
    private DynamicVertexBuffer dynamicVertexBuffer;
    private DynamicIndexBuffer  dynamicIndexBuffer;
    private int totalIndexCount;

    // Risorse Font
    private FontAtlas fontAtlas;
    private TextureImage fontTexture;

    // UBO e Descrittori
    private OrthoUBO ubo;
    private DescriptorManager descriptorManager;

    // Sincronizzazione CPU <-> GPU
    private long imageAvailableSemaphore;
    private long renderFinishedSemaphore;
    private long inFlightFence;

    //Input
    private GLFWMouseButtonCallback mouseButtonCallback;
    private GLFWKeyCallback keyboardCallback;

    private boolean framebufferResized = false;

    private static final Dimension SCREEN = Toolkit.getDefaultToolkit().getScreenSize();

    private final Map<String, NvCont> pages = new HashMap<>(10);

    private final List<UpdateCycle> updatable = new ArrayList<>(10);

    public static NvCont rootComponent;

    public float fps = -1;
    private boolean showFPS = false;
    public int targetFps = -1;
    private boolean vsync = true;
    private final float[] backgroundColor = {0.1f, 0.1f, 0.1f, 1.0f};
    private int internalResolutionWidth = -1;
    private int internalResolutionHeight = -1;
    private float renderScale = 1.0f;
    private boolean pixelPerfect = false;

    private long gpuSubmitTime = 0;
    public float gpuTotalMs = 0f;
    public float gpuRenderPassMs = 0f;
    public float gpuBlitMs = 0f;

    public static final int UNLIMITED = -1;
    public void setFpsLimit(int fps) {
        this.targetFps = fps;
    }

        public void setBackgroundColor(float r, float g, float b) {
        this.backgroundColor[0] = r;
        this.backgroundColor[1] = g;
        this.backgroundColor[2] = b;
        sceneDirty = true;
    }

    private boolean[] commandBufferDirty;

    private void initCommandBufferDirtyFlags() {
        commandBufferDirty = new boolean[swapchain.getImageCount()];
        Arrays.fill(commandBufferDirty, true);
    }

    public void setVsync(boolean enabled) {
        if (this.vsync != enabled) {
            this.vsync = enabled;
            this.framebufferResized = true; // Trigger swapchain recreation
            sceneDirty = true;
        }
    }

    public void setCurrentCameraUpdateCycle(UpdateCycle updateCycle){
        currentCameraUpdateCycle = updateCycle;
    }

    public void setInternalResolution(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new EngineEx("Internal resolution must be greater than zero.");
        }
        this.internalResolutionWidth = width;
        this.internalResolutionHeight = height;
        this.renderScale = -1.0f;
        syncRootSizeToRenderTarget();
        sceneDirty = true;
    }
    public void setInternalResolution(ScreenSize size) {
        setInternalResolution(size.getWidth(), size.getHeight());
    }

    public void setRenderScale(float renderScale) {
        if (renderScale <= 0.0f) {
            throw new EngineEx("Render scale must be greater than zero.");
        }
        this.renderScale = renderScale;
        this.internalResolutionWidth = -1;
        this.internalResolutionHeight = -1;
        syncRootSizeToRenderTarget();
        sceneDirty = true;
    }

    public void setPixelPerfect(boolean pixelPerfect) {
        this.pixelPerfect = pixelPerfect;
        syncRootSizeToRenderTarget();
        sceneDirty = true;
    }

    private final Map<String, NvImage> images = new HashMap<>(16);

    public void setShowFPS(boolean shouldShow) {
        this.showFPS = shouldShow;
        sceneDirty = true;
    }

    public NvCont getPage(String key){
        return pages.get(key);
    }

    public Map<String, NvCont> getPages() {
        return pages;
    }

    public int getWidth(){
        return swapchain.getWidth();
    }

    public int getHeight(){
        return swapchain.getHeight();
    }

    public float getRenderWidth() {
        return getEffectiveRenderSize()[0];
    }

    public float getRenderHeight() {
        return getEffectiveRenderSize()[1];
    }

    public NvCont getCurrentPage() {
        return rootComponent;
    }

    /**
     *  <h2>To create a new page, use: NvCont.newPage()</h2>
     * @param key key to get or change the page
     * @param page page to add
     */
    public NvCont addPage(String key, NvCont page){
        pages.put(key, page);
        sceneDirty = true;
        return page;
    }

    public NvCont addAndSetPage(String key, NvCont page){
        pages.put(key, page);
        rootComponent = page;
        syncRootSizeToRenderTarget();
        sceneDirty = true;
        return page;
    }

    public void remove(String key){
        pages.remove(key);
        sceneDirty = true;
    }

    private static NvContext appInstance;

    public void setCurrentPage(String key){
        rootComponent = pages.get(key);
        syncRootSizeToRenderTarget();
        sceneDirty = true;
    }

    public static void invalidateInstance(){
        appInstance = null;
    }

    public static NvContext getInstance(){
        if(appInstance == null)
            appInstance = new NvContext("NV2D game");
        return appInstance;
    }
    public static NvContext createInstance(String name, Dimension windowDimension){
        if(appInstance == null)
            appInstance = new NvContext(name, windowDimension);
        return appInstance;
    }
    public static NvContext createInstance(String name, int maxVertices, int maxIndices){
        if(appInstance == null)
            appInstance = new NvContext(name, maxVertices, maxIndices, SCREEN);
        return appInstance;
    }
    public static NvContext createInstance(String name){
        if(appInstance == null)
            appInstance = new NvContext(name);
        return appInstance;
    }

    private NvContext(String name, int maxVertices, int maxIndices, Dimension windowDim) {
        this.MAX_VERTICES = maxVertices;
        this.MAX_INDICES = maxIndices;
        this.currentCameraUpdateCycle = (_) -> {};

        NvLogger.initialize(name, MAJOR_VERSION, MINOR_VERSION, PATCH);
        initWindow(name, windowDim);


        logEngine("Window initialized");
        initVulkan();
        logEngine("Vulkan initialized");
        CollisionManager.initialize();
        logEngine("Collisions initialized");
        AudioManager.init();
        logEngine("OpenAL Audio Engine initialized successfully.");
        GameSaveManager.initialize("save/"+name + "_save.bin");
        logEngine("GameSaveManager initialized successfully");


        logEngine("-----------Game started successfully-------------");
    }

    private NvContext(String name) {
        this(name, DEF_MAX_VERTICES, DEF_MAX_INDICES, SCREEN);
    }

    private NvContext(Dimension windowDimension) {
        this("NV2D game", DEF_MAX_VERTICES, DEF_MAX_INDICES, windowDimension);
    }

    private NvContext(String name, Dimension windowDimension) {
        this(name, DEF_MAX_VERTICES, DEF_MAX_INDICES, windowDimension);
    }
    public void addUpdatable(UpdateCycle updateCycle){
        updatable.add(updateCycle);
    }
    public void removeUpdatable(UpdateCycle updateCycle){
        updatable.remove(updateCycle);
    }

    /**
     * Adds a new component to the current root component
     * @param component to add
     */
    public void addTreeComponent(NvComp component){
        rootComponent.addChild(component);
        sceneDirty = true;
    }
    public void removeComponent(NvComp component){
        rootComponent.removeChild(component);
        sceneDirty = true;
    }

    private int graphicsIndexCount;
    private int imageIndexCount;
    private int imageIndexOffset;

    private final NvGraphic graphic = new NvPixelGraphic();
    private volatile boolean sceneDirty = true;
    private float[] combinedVertices = new float[0];
    private int[] combinedIndices = new int[0];
    private int combinedVertexFloatCount;
    private int combinedIndexCount;
    private final ExecutorService updatePool = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() - 1),
            runnable -> {
                Thread thread = new Thread(runnable, "nv-update-pool");
                thread.setDaemon(true);
                return thread;
            }
    );

    public static void markSceneDirty() {
        if (appInstance != null) {
            appInstance.sceneDirty = true;
            if (appInstance.commandBufferDirty != null) {
                Arrays.fill(appInstance.commandBufferDirty, true);
            }
        }
    }

    private final NvComp fpsDisplay = new NvComp(10, 10, 320, 130) {
        private String displayString = "FPS: NONE";
        private String gpuString     = "GPU: --";
        private int localFrameCount  = 0;
        private float localFpsSum    = 0;

        @Override
        public void update(float dt) {
            localFrameCount++;
            if (dt > 0) localFpsSum += (1.0f / dt);
            if (localFrameCount >= 30) {
                float averageFps = localFpsSum / localFrameCount;
                displayString = String.format("FPS: %.1f", averageFps);
                gpuString = String.format(
                        "GPU tot: %.2f ms | pass: %.2f ms | blit: %.2f ms",
                        gpuTotalMs, gpuRenderPassMs, gpuBlitMs
                );
                localFrameCount = 0;
                localFpsSum = 0;
                NvContext.markSceneDirty();
            }
        }

        @Override
        public void drawIntern(NvGraphic g) {
            setHUD(true);
            g.setRGB(0, 0, 0);
            g.drawRect(0, 0, 320, 130);
            g.setRGB(1, 1, 1);
            g.drawText(displayString, 10, 10);
            g.drawText(gpuString, 10, 55);
        }
    };

    private void rebuildScene() {
        if (!sceneDirty) {
            return;
        }

        float[] renderSize = getEffectiveRenderSize();
        final float w = renderSize[0];
        final float h = renderSize[1];
        final float wu = 8.0f / 512.0f;
        final float wv = 8.0f / 512.0f;

        graphic.initialize(w, h, wu, wv, fontAtlas);

        CollisionManager.handleCollisions();

        rootComponent.draw(graphic);

        if(showFPS)
            fpsDisplay.draw(graphic);

        graphicsIndexCount = graphic.getIndexCount();
        imageIndexCount    = graphic.getImageIndexCount();
        imageIndexOffset   = graphicsIndexCount;

        combinedVertexFloatCount = graphic.getVertexFloatCount() + graphic.getImageVertexFloatCount();
        combinedIndexCount = graphicsIndexCount + imageIndexCount;

        ensureCombinedVertexCapacity(combinedVertexFloatCount);
        ensureCombinedIndexCapacity(combinedIndexCount);

        graphic.copyVerticesTo(combinedVertices, 0);
        graphic.copyImageVerticesTo(combinedVertices, graphic.getVertexFloatCount());

        graphic.copyIndicesTo(combinedIndices, 0);
        graphic.copyImageIndicesTo(combinedIndices, graphicsIndexCount, graphic.getVertexFloatCount() / NvGraphic.FLOATS_PER_VERTEX);

        dynamicVertexBuffer.update(combinedVertices, combinedVertexFloatCount);
        dynamicIndexBuffer.update(combinedIndices, combinedIndexCount);
        sceneDirty = false;
    }

    private void ensureCombinedVertexCapacity(int requiredFloatCount) {
        if (combinedVertices.length >= requiredFloatCount) {
            return;
        }

        int newCapacity = Math.max(2048, combinedVertices.length);
        while (newCapacity < requiredFloatCount) {
            newCapacity *= 2;
        }
        combinedVertices = Arrays.copyOf(combinedVertices, newCapacity);
    }

    private void ensureCombinedIndexCapacity(int requiredCount) {
        if (combinedIndices.length >= requiredCount) {
            return;
        }

        int newCapacity = Math.max(2048, combinedIndices.length);
        while (newCapacity < requiredCount) {
            newCapacity *= 2;
        }
        combinedIndices = Arrays.copyOf(combinedIndices, newCapacity);
    }

    @Override
    public void run() {
        mainLoop();
        cleanup();
    }

    public void changeFont(Font font){
        FontAtlas oldAtlas = this.fontAtlas;
        TextureImage oldTexture = this.fontTexture;

        this.fontAtlas = new FontAtlas(font);
        this.fontTexture = new TextureImage(
                device, physicalDevice, graphicsQueue,
                fontAtlas.getPixelBuffer(), fontAtlas.getWidth(), fontAtlas.getHeight()
        );
        descriptorManager.updateTexture(0, fontTexture);

        if (oldTexture != null) {
            oldTexture.close();
        }
        if (oldAtlas != null) {
            oldAtlas.close();
        }

        sceneDirty = true;
    }

    private void initWindow(String name, Dimension windowDimension) {
        if (!glfwInit()) throw new EngineEx("Impossibile inizializzare GLFW");

        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        window = glfwCreateWindow(windowDimension.width, windowDimension.height, name, 0, 0);
        if (window == 0) throw new EngineEx("Impossibile creare la finestra GLFW");

        glfwSetFramebufferSizeCallback(window, (windowHandle, width, height) -> {
            framebufferResized = true;
        });

        mouseButtonCallback = GLFWMouseButtonCallback.create(ClickSystem.inputCallback(window));
        keyboardCallback = GLFWKeyCallback.create(KeyboardSystem.keyboardCallBack());

        glfwSetKeyCallback(window, keyboardCallback);
        glfwSetMouseButtonCallback(window, mouseButtonCallback);

        glfwSetCursorPosCallback(window, (windowHandle, x, y) -> {
            mouseX = (int) x;
            mouseY = (int) y;
            mouseMoved = true;
        });
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            ByteBuffer icon = STBImage.stbi_load("src/main/resources/gameicon/gameIcon.png", w, h, channels, 4);

            if (icon != null) {
                GLFWImage.Buffer iconBuffer = GLFWImage.malloc(1, stack);
                iconBuffer.position(0).width(w.get(0)).height(h.get(0)).pixels(icon);
                org.lwjgl.glfw.GLFW.glfwSetWindowIcon(window, iconBuffer);
                STBImage.stbi_image_free(icon);
            }
        }

        addUpdatable(fpsDisplay);
    }



    public void setKeyboardFocus(KeyboardListener focused) {
        KeyboardSystem.focused = focused;
    }


    private void initVulkan() {
        createInstance();
        logEngine("Vulkan instanced");
        createSurface();
        logEngine("Vulkan surface created");
        pickPhysicalDeviceAndCreateLogicalDevice();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pWidth  = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            glfwGetFramebufferSize(window, pWidth, pHeight);
            int fbW = pWidth.get(0);
            int fbH = pHeight.get(0);

            this.swapchain = new Swapchain(device, surface, fbW, fbH, vsync);
            rootComponent = new NvCont(0, 0, fbW, fbH);
        }
        logEngine("Swapchain created");
        currentImageCount = swapchain.getImageCount();
        initCommandBufferDirtyFlags();

        createRenderPass();

        this.fontAtlas   = new FontAtlas(new Font("SansSerif", Font.PLAIN, 42));
        logEngine("Font atlas created");
        this.fontTexture = new TextureImage(
                device, physicalDevice, graphicsQueue,
                fontAtlas.getPixelBuffer(), fontAtlas.getWidth(), fontAtlas.getHeight()
        );
        logEngine("Font texture created");

        int imageCount = swapchain.getImageCount(); // usa il conteggio reale
        this.ubo               = new OrthoUBO(device, physicalDevice, imageCount);
        this.descriptorManager = new DescriptorManager(device, ubo, fontTexture, imageCount);

        this.pipeline = new GraphicsPipeline(
                device, swapchain, renderPass,
                descriptorManager.getDescriptorSetLayoutHandle()
        );
        logEngine("Graphics pipeline created");

        this.texturePipeline = new TexturePipeline(
                device, swapchain, renderPass,
                descriptorManager.getDescriptorSetLayoutHandle()
        );
        logEngine("Texture pipeline created");


        createInternalRenderTarget();
        buildCombinedGeometry();

        this.commandBuffers = new CommandBuffers(device, pipeline, swapchain);
        createSyncObjects();

        assets = new AssetsManager(
                device,
                physicalDevice,
                graphicsQueue,
                (slot, texture) -> descriptorManager.updateTexture(slot, texture)
        );
    }
    private AssetsManager assets;

    public AssetsManager assets() {
        return assets;
    }

    private void buildCombinedGeometry() {
        long vertexBufferSize = (long) MAX_VERTICES * 8 * Float.BYTES;
        this.dynamicVertexBuffer = new DynamicVertexBuffer(device, physicalDevice, vertexBufferSize);
        this.dynamicIndexBuffer  = new DynamicIndexBuffer(device, physicalDevice, MAX_INDICES);
        rebuildScene();
    }

    private void createSyncObjects() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
            VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                    .flags(VK_FENCE_CREATE_SIGNALED_BIT);

            LongBuffer pImgAvail = stack.mallocLong(1);
            LongBuffer pRndDone  = stack.mallocLong(1);
            LongBuffer pFence    = stack.mallocLong(1);

            if (vkCreateSemaphore(device, semaphoreInfo, null, pImgAvail) != VK_SUCCESS ||
                vkCreateSemaphore(device, semaphoreInfo, null, pRndDone)  != VK_SUCCESS ||
                vkCreateFence(device, fenceInfo, null, pFence)            != VK_SUCCESS) {
                throw new EngineEx("Impossibile creare gli oggetti di sincronizzazione.");
            }

            this.imageAvailableSemaphore = pImgAvail.get(0);
            this.renderFinishedSemaphore = pRndDone.get(0);
            this.inFlightFence           = pFence.get(0);
        }
    }
    private int mouseX;
    private int mouseY;
    private boolean mouseMoved;

    private void tickHandler(float dt) {
        currentCameraUpdateCycle.update(dt);
        rootComponent.tick(dt);
        runUpdatablesParallel(dt);
        if (mouseMoved) {
            HoverSystem.handleHover(window, rootComponent);
            mouseMoved = false;
        }

        if (dt > 0) {
            fps = 1.0F / dt;
        }
    }

    private void runUpdatablesParallel(float dt) {
        List<UpdateCycle> snapshot = new ArrayList<>(updatable);
        int size = snapshot.size();
        if (size == 0) {
            return;
        }
        if (size == 1) {
            snapshot.get(0).update(dt);
            return;
        }

        List<Future<?>> futures = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            UpdateCycle updateCycle = snapshot.get(i);
            futures.add(updatePool.submit(() -> updateCycle.update(dt)));
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new EngineEx("Interrupted while waiting for update workers: " + e);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException runtime) {
                    throw runtime;
                }
                throw new EngineEx("Update worker failed: " + cause);
            }
        }
    }



    private void mainLoop() {
        double lastFrameTime = glfwGetTime();
        while (!glfwWindowShouldClose(window)) {


            double now = glfwGetTime();
            float deltaTime = (float) (now - lastFrameTime);
            lastFrameTime = now;

            glfwPollEvents();
            tickHandler(deltaTime);

            drawFrame();

            if (targetFps > 0) {
                double targetFrameTime = 1.0 / targetFps;
                while (glfwGetTime() - now < targetFrameTime) {
                    double remaining = targetFrameTime - (glfwGetTime() - now);
                    if (remaining > 0.001) {
                        try {
                            Thread.sleep((long) (remaining * 1000 - 1));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    } else {
                        // Busy-wait for precision during the last millisecond
                        Thread.onSpinWait();
                    }
                }
            }
        }
        vkDeviceWaitIdle(device);
    }

    private void drawFrame() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            vkWaitForFences(device, inFlightFence, true, Long.MAX_VALUE);

            // Calcola il tempo GPU approssimato: quanto ha impiegato la GPU
            // dall'ultimo submit al fence signaled (misurato lato CPU)
            long fenceSignaledAt = System.nanoTime();
            if (gpuSubmitTime > 0) {
                gpuTotalMs = (fenceSignaledAt - gpuSubmitTime) / 1_000_000f;
            }

            vkResetFences(device, inFlightFence);

            IntBuffer pImageIndex = stack.mallocInt(1);
            int acquireResult = vkAcquireNextImageKHR(device, swapchain.getHandle(), Long.MAX_VALUE,
                    imageAvailableSemaphore, VK_NULL_HANDLE, pImageIndex);

            if (acquireResult == VK_ERROR_OUT_OF_DATE_KHR) {
                recreateSwapchain();
                return;
            }
            if (acquireResult != VK_SUCCESS && acquireResult != VK_SUBOPTIMAL_KHR) {
                throw new EngineEx("Errore durante l'acquisizione dell'immagine della swapchain.");
            }

            int imageIndex = pImageIndex.get(0);

            if (sceneDirty || framebufferResized || commandBufferDirty[imageIndex]) {
                float[] renderSize = getEffectiveRenderSize();
                rebuildScene();
                ubo.update(imageIndex, 0f, renderSize[0], renderSize[1], 0f);

                commandBuffers.recordOffscreen(
                        imageIndex,
                        backgroundColor,
                        renderPass,
                        internalRenderTarget.getFramebufferHandle(),
                        pipeline.getHandle(),
                        pipeline.getPipelineLayoutHandle(),
                        texturePipeline.getHandle(),
                        texturePipeline.getPipelineLayoutHandle(),
                        dynamicVertexBuffer.getHandle(),
                        dynamicIndexBuffer.getHandle(),
                        graphicsIndexCount,
                        imageIndexCount,
                        imageIndexOffset,
                        descriptorManager.getDescriptorSet(imageIndex),
                        internalRenderTarget.getImageHandle(),
                        swapchain.getImages()[imageIndex],
                        internalRenderTarget.getWidth(),
                        internalRenderTarget.getHeight(),
                        swapchain.getWidth(),
                        swapchain.getHeight(),
                        pixelPerfect
                );

                commandBufferDirty[imageIndex] = false;
                boolean allClean = true;
                for (boolean dirty : commandBufferDirty) {
                    if (dirty) { allClean = false; break; }
                }
                if (allClean) sceneDirty = false;
            }

            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pWaitSemaphores(stack.longs(imageAvailableSemaphore))
                    .pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT))
                    .pCommandBuffers(stack.pointers(commandBuffers.getCommandBuffer(imageIndex)))
                    .pSignalSemaphores(stack.longs(renderFinishedSemaphore));

            gpuSubmitTime = System.nanoTime(); // ← segna quando submittiamo
            if (vkQueueSubmit(graphicsQueue, submitInfo, inFlightFence) != VK_SUCCESS) {
                throw new EngineEx("Errore nel submit della lista comandi alla GPU.");
            }

            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                    .pWaitSemaphores(stack.longs(renderFinishedSemaphore))
                    .swapchainCount(1)
                    .pSwapchains(stack.longs(swapchain.getHandle()))
                    .pImageIndices(pImageIndex);

            int presentResult = vkQueuePresentKHR(graphicsQueue, presentInfo);

            if (presentResult == VK_ERROR_OUT_OF_DATE_KHR || framebufferResized) {
                framebufferResized = false;
                recreateSwapchain();
            } else if (presentResult != VK_SUCCESS && presentResult != VK_SUBOPTIMAL_KHR) {
                throw new EngineEx("Errore durante la presentazione della swapchain.");
            }
        }
    }

    private float[] getEffectiveRenderSize() {
        int windowWidth = swapchain.getWidth();
        int windowHeight = swapchain.getHeight();

        if (internalResolutionWidth > 0 && internalResolutionHeight > 0) {
            return new float[]{internalResolutionWidth, internalResolutionHeight};
        }

        float scale = renderScale;
        if (scale <= 0.0f) {
            scale = 1.0f;
        }

        if (pixelPerfect) {
            scale = Math.max(1.0f, Math.round(scale));
        }

        return new float[]{
                Math.max(1.0f, windowWidth * scale),
                Math.max(1.0f, windowHeight * scale)
        };
    }


    private int currentImageCount = 0;

    private void recreateSwapchain() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width  = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            glfwGetFramebufferSize(window, width, height);
            while (width.get(0) == 0 || height.get(0) == 0) {
                glfwWaitEvents();
                glfwGetFramebufferSize(window, width, height);
            }
        }

        vkDeviceWaitIdle(device);
        cleanupSwapchainResources();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pWidth  = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            glfwGetFramebufferSize(window, pWidth, pHeight);
            this.swapchain = new Swapchain(device, surface, pWidth.get(0), pHeight.get(0), vsync);
        }

        int newImageCount = swapchain.getImageCount();

        if (newImageCount != descriptorManager.getImageCount()) {
            if (ubo != null) ubo.close();
            if (descriptorManager != null) descriptorManager.close();

            TextureImage[] existingTextures = new TextureImage[loadedTextures.length];
            existingTextures[0] = fontTexture;
            for (int i = 1; i < loadedTextures.length; i++) {
                if (loadedTextures[i] != null) {
                    existingTextures[i] = loadedTextures[i].getTextureImage();
                }
            }

            this.ubo = new OrthoUBO(device, physicalDevice, newImageCount);
            this.descriptorManager = new DescriptorManager(
                    device, ubo, fontTexture, newImageCount, existingTextures
            );
        }

        createInternalRenderTarget();
        syncRootSizeToRenderTarget();

        if (commandBuffers  != null) commandBuffers.free();
        if (pipeline        != null) pipeline.close();
        if (texturePipeline != null) texturePipeline.close();

        this.pipeline = new GraphicsPipeline(
                device, swapchain, renderPass,
                descriptorManager.getDescriptorSetLayoutHandle()
        );
        this.texturePipeline = new TexturePipeline(
                device, swapchain, renderPass,
                descriptorManager.getDescriptorSetLayoutHandle()
        );
        this.commandBuffers = new CommandBuffers(device, pipeline, swapchain);
        sceneDirty = true;
    }

    private void cleanupSwapchainResources() {
        if (swapchain != null) {
            swapchain.close();
            swapchain = null;
        }

        if (internalRenderTarget != null) {
            internalRenderTarget.close();
            internalRenderTarget = null;
        }
    }


    // -------------------------------------------------------------------------
    // Inizializzazione Vulkan (instance, surface, device, renderpass, target)
    // -------------------------------------------------------------------------

    private void createInstance() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
                    .pApplicationName(stack.UTF8(ENGINE_NAME))
                    .applicationVersion(VK_MAKE_VERSION(MAJOR_VERSION, MINOR_VERSION, PATCH))
                    .pEngineName(stack.UTF8(ENGINE_NAME))
                    .engineVersion(VK_MAKE_VERSION(MAJOR_VERSION, MINOR_VERSION, PATCH))
                    .apiVersion(VK_API_VERSION_1_0);

            PointerBuffer glfwExtensions = GLFWVulkan.glfwGetRequiredInstanceExtensions();
            if (glfwExtensions == null) throw new EngineEx("Estensioni Vulkan GLFW non trovate.");

            VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                    .pApplicationInfo(appInfo)
                    .ppEnabledExtensionNames(glfwExtensions);

            PointerBuffer pInstance = stack.mallocPointer(1);
            if (vkCreateInstance(createInfo, null, pInstance) != VK_SUCCESS) {
                throw new EngineEx("Impossibile creare l'istanza Vulkan.");
            }
            this.instance = new VkInstance(pInstance.get(0), createInfo);
        }
    }

    private void createSurface() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pSurface = stack.mallocLong(1);
            if (GLFWVulkan.glfwCreateWindowSurface(instance, window, null, pSurface) != VK_SUCCESS) {
                throw new EngineEx("Impossibile creare la surface Vulkan.");
            }
            this.surface = pSurface.get(0);
        }
    }

    private void pickPhysicalDeviceAndCreateLogicalDevice() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pDeviceCount = stack.mallocInt(1);
            vkEnumeratePhysicalDevices(instance, pDeviceCount, null);
            if (pDeviceCount.get(0) == 0) throw new EngineEx("Nessuna GPU Vulkan trovata.");

            PointerBuffer pPhysicalDevices = stack.mallocPointer(pDeviceCount.get(0));
            vkEnumeratePhysicalDevices(instance, pDeviceCount, pPhysicalDevices);
            this.physicalDevice = new VkPhysicalDevice(pPhysicalDevices.get(0), instance);

            IntBuffer pQueueFamilyCount = stack.mallocInt(1);
            vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, pQueueFamilyCount, null);
            VkQueueFamilyProperties.Buffer queueFamilies =
                    VkQueueFamilyProperties.calloc(pQueueFamilyCount.get(0), stack);
            vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, pQueueFamilyCount, queueFamilies);

            int graphicsQFI = -1;
            for (int i = 0; i < queueFamilies.capacity(); i++) {
                if ((queueFamilies.get(i).queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0) {
                    graphicsQFI = i;
                    break;
                }
            }
            if (graphicsQFI == -1) throw new EngineEx("Nessuna queue grafica trovata.");
            VkDeviceQueueCreateInfo.Buffer queueCreateInfo = VkDeviceQueueCreateInfo.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                    .queueFamilyIndex(graphicsQFI)
                    .pQueuePriorities(stack.floats(1.0f));

            IntBuffer extCount = stack.mallocInt(1);
            vkEnumerateDeviceExtensionProperties(physicalDevice, (String) null, extCount, null);
            VkExtensionProperties.Buffer availableExtensions = VkExtensionProperties.malloc(extCount.get(0));
            vkEnumerateDeviceExtensionProperties(physicalDevice, (String) null, extCount.rewind(), availableExtensions);

            boolean hasPortabilitySubset = false;
            for (VkExtensionProperties ext : availableExtensions) {
                if (ext.extensionNameString().equals("VK_KHR_portability_subset")) {
                    hasPortabilitySubset = true;
                    break;
                }
            }

            PointerBuffer deviceExtensions = stack.mallocPointer(hasPortabilitySubset ? 2 : 1);
            deviceExtensions.put(stack.UTF8(VK_KHR_SWAPCHAIN_EXTENSION_NAME));
            if (hasPortabilitySubset) {
                deviceExtensions.put(stack.UTF8("VK_KHR_portability_subset"));
            }
            deviceExtensions.flip();
            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                    .pQueueCreateInfos(queueCreateInfo)
                    .ppEnabledExtensionNames(deviceExtensions);

            PointerBuffer pDevice = stack.mallocPointer(1);
            if (vkCreateDevice(physicalDevice, createInfo, null, pDevice) != VK_SUCCESS) {
                throw new EngineEx("Impossibile creare il Logical Device.");
            }
            this.device = new VkDevice(pDevice.get(0), physicalDevice, createInfo);

            PointerBuffer pQueue = stack.mallocPointer(1);
            vkGetDeviceQueue(device, graphicsQFI, 0, pQueue);
            this.graphicsQueue = new VkQueue(pQueue.get(0), device);
        }
    }

    private void createRenderPass() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkAttachmentDescription.Buffer colorAttachment = VkAttachmentDescription.calloc(1, stack)
                    .format(swapchain.getFormat())
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                    .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                    .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                    .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                    .finalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

            VkAttachmentReference.Buffer colorRef = VkAttachmentReference.calloc(1, stack)
                    .attachment(0)
                    .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

            VkSubpassDescription.Buffer subpass = VkSubpassDescription.calloc(1, stack)
                    .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                    .colorAttachmentCount(1)
                    .pColorAttachments(colorRef);

            VkSubpassDependency.Buffer dependency = VkSubpassDependency.calloc(1, stack)
                    .srcSubpass(VK_SUBPASS_EXTERNAL)
                    .dstSubpass(0)
                    .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                    .srcAccessMask(0)
                    .dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                    .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);

            VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                    .pAttachments(colorAttachment)
                    .pSubpasses(subpass)
                    .pDependencies(dependency);

            LongBuffer pRenderPass = stack.mallocLong(1);
            if (vkCreateRenderPass(device, renderPassInfo, null, pRenderPass) != VK_SUCCESS) {
                throw new EngineEx("Impossibile creare il Render Pass.");
            }
            this.renderPass = pRenderPass.get(0);
        }
    }

    public synchronized void registerTexture(int slot, NvImage image) {
        loadedTextures[slot] = image;
        descriptorManager.updateTexture(slot, image.getTextureImage());
    }

    private void createInternalRenderTarget() {
        if (internalRenderTarget != null) {
            internalRenderTarget.close();
        }

        float[] renderSize = getEffectiveRenderSize();
        this.internalRenderTarget = new InternalRenderTarget(
                device,
                physicalDevice,
                renderPass,
                Math.max(1, Math.round(renderSize[0])),
                Math.max(1, Math.round(renderSize[1])),
                swapchain.getFormat()
        );
    }

    private void syncRootSizeToRenderTarget() {
        if (rootComponent == null) {
            return;
        }
        float[] renderSize = getEffectiveRenderSize();
        rootComponent.setW(Math.max(1, Math.round(renderSize[0])));
        rootComponent.setH(Math.max(1, Math.round(renderSize[1])));
    }

    private void cleanup() {
        logEngine("Cleaning up allocated memory before exiting");
        if (mouseButtonCallback != null) {
            mouseButtonCallback.free();
            mouseButtonCallback = null;
        }
        if (keyboardCallback != null) {
            keyboardCallback.free();
            keyboardCallback = null;
        }

        if (device != null) {
            vkDestroySemaphore(device, imageAvailableSemaphore, null);
            vkDestroySemaphore(device, renderFinishedSemaphore, null);
            vkDestroyFence(device, inFlightFence, null);
        }

        if (commandBuffers      != null) commandBuffers.free();
        if (internalRenderTarget != null) {
            internalRenderTarget.close();
            internalRenderTarget = null;
        }
        if (descriptorManager   != null) descriptorManager.close();
        if (ubo                 != null) ubo.close();
        if (fontTexture         != null) fontTexture.close();
        if (fontAtlas           != null) fontAtlas.close();

        for (int i = 1; i < loadedTextures.length; i++) {
            if (loadedTextures[i] != null) {
                loadedTextures[i].close();
                loadedTextures[i] = null;
            }
        }
        
        if (pipeline            != null) pipeline.close();
        if (renderPass          != 0)    vkDestroyRenderPass(device, renderPass, null);
        if (dynamicVertexBuffer != null) dynamicVertexBuffer.close();
        if (dynamicIndexBuffer  != null) dynamicIndexBuffer.close();
        if (swapchain           != null) swapchain.close();
        updatePool.shutdownNow();
        if (surface             != 0)    vkDestroySurfaceKHR(instance, surface, null);
        if (device              != null) vkDestroyDevice(device, null);
        if (instance            != null) vkDestroyInstance(instance, null);

        AudioManager.cleanup();

        glfwDestroyWindow(window);
        logEngine("Memory cleaned up successfully");
        glfwTerminate();
    }


}
