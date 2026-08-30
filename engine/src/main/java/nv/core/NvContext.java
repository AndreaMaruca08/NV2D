package nv.core;

import nv.core.annotations.EngineCore;
import nv.core.assets.AssetsManager;
import nv.core.camera.NvCamera;
import nv.core.collision.CollisionManager;
import nv.core.components.NvComp;
import nv.core.components.NvCont;
import nv.core.data.*;
import nv.core.errors.NvLogger;
import nv.core.errors.ex.EngineEx;
import nv.core.events.EventSystem;
import nv.core.graphic.NvGraphic;
import nv.core.graphic.NvPixelGraphic;
import nv.core.io.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static nv.core.errors.NvLogger.logEngine;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.KHRGetPhysicalDeviceProperties2.*;
import static org.lwjgl.vulkan.VK12.*;

import static org.lwjgl.vulkan.EXTDescriptorIndexing.*;

/**
 * <h3>Entry point for the NV2D game engine</h3>
 * <p>SingleTone class responsible for managing the application's Vulkan resources and rendering pipeline</p>
 * @since 1.0
 * @author Andrea Maruca
 */
@EngineCore
@SuppressWarnings("all")
public final class NvContext implements Runnable {
    private static final int MAJOR_VERSION = 1;
    private static final int MINOR_VERSION = 7;
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
    private boolean internalRenderTargetInitialized;
    private Updatable currentCameraUpdatable;
    private PostProcessSettings postProcessSettings = new PostProcessSettings();
    private PostProcessPipeline postProcessPipeline;

    private final EventSystem eventSystem;

    private final int MAX_VERTICES;
    private final int MAX_INDICES;

    private static final int DEF_MAX_VERTICES = 1_000_000;
    private static final int DEF_MAX_INDICES  = 1_300_000;

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

    private static final Dimension SCREEN;
    static {
        Dimension s;
        try {
            s = GraphicsEnvironment.isHeadless()
                    ? new Dimension(1920, 1080)
                    : Toolkit.getDefaultToolkit().getScreenSize();
        } catch (Throwable e) {
            s = new Dimension(1920, 1080);
        }
        SCREEN = s;
    }

    private final Map<String, NvCont> pages = new HashMap<>(10);

    private static final int PARALLEL_UPDATE_THRESHOLD = 64;
    private final List<Updatable> updatable = new CopyOnWriteArrayList<>();
    private final List<Drawable> drawables = new CopyOnWriteArrayList<>();

    public static NvCont rootComponent;

    public float fps = -1;
    private boolean showFPS = false;
    public int targetFps = -1;
    private int idleFps = 5;
    private boolean idleWhenUnfocused = false;
    private volatile boolean windowFocused = true;
    private volatile boolean inputPending = false;
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

    public void setIdleFpsLimit(int fps) {
        if (fps <= 0) {
            throw new EngineEx("Idle FPS limit must be greater than zero.");
        }
        this.idleFps = fps;
    }

    public void setIdleWhenUnfocused(boolean idleWhenUnfocused) {
        this.idleWhenUnfocused = idleWhenUnfocused;
    }

    public static void notifyInputEvent() {
        if (appInstance != null) {
            appInstance.inputPending = true;
        }
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

    public void setCurrentCameraUpdateCycle(Updatable updatable){
        currentCameraUpdatable = updatable;
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
        if (!updatable.contains(fpsDisplay)){
            addUpdatable(fpsDisplay);
        }
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

    /** Updated in 1.6. */
    public float getRenderWidth() {
        return getEffectiveRenderWidth();
    }

    /** Updated in 1.6. */
    public float getRenderHeight() {
        return getEffectiveRenderHeight();
    }

    public NvCont getCurrentPage() {
        return rootComponent;
    }

    public VkDevice getDevice() {
        return device;
    }

    public VkPhysicalDevice getPhysicalDevice() {
        return physicalDevice;
    }

    public VkQueue getGraphicsQueue() {
        return graphicsQueue;
    }

    public InternalRenderTarget getInternalRenderTarget() {
        return internalRenderTarget;
    }

    public Swapchain getSwapchain() {
        return swapchain;
    }

    public PostProcessSettings getPostProcess() {
        return postProcessSettings;
    }

    public EventSystem events() {
        return eventSystem;
    }

    public void setPostProcess(PostProcessSettings settings) {
        this.postProcessSettings = settings != null ? settings : new PostProcessSettings();
        markSceneDirty();
    }

    public void enableRetroCRT() {
        this.postProcessSettings.presetRetroCRT();
        markSceneDirty();
    }

    public void disablePostProcess() {
        this.postProcessSettings.setEnabled(false);
        markSceneDirty();
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

    /**
     * creates and set a new page by passing a name
     * @return the new page
     * @since 1.6
     */
    public NvCont newPage(String name){
        var page = NvCont.newPage();
        pages.put(name, page);
        rootComponent = page;
        syncRootSizeToRenderTarget();
        sceneDirty = true;
        return page;
    }

    int pageNum = 0;

    /**
     * creates and set a new page<br>
     * a new page will have the name "page" + pageNum starting from 1
     * @return the new page
     * @since 1.6
     */
    public NvCont newPage(){
        pageNum++;
        var page = NvCont.newPage();
        pages.put("page" + pageNum, page);
        rootComponent = page;
        syncRootSizeToRenderTarget();
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
            appInstance = new NvContext("NV2D game", true);
        return appInstance;
    }
    public static NvContext createInstance(String name, boolean windowResizable, Dimension windowDimension){
        if(appInstance == null)
            appInstance = new NvContext(name, windowResizable, windowDimension);
        return appInstance;
    }
    public static NvContext createInstance(String name, boolean windowResizable, int maxVertices, int maxIndices){
        if(appInstance == null)
            appInstance = new NvContext(name, maxVertices, maxIndices, windowResizable, SCREEN);
        return appInstance;
    }
    public static NvContext createInstance(String name, boolean windowResizable, Dimension windowDimension, int maxVertices, int maxIndices){
        if(appInstance == null)
            appInstance = new NvContext(name, maxVertices, maxIndices, windowResizable, windowDimension);
        return appInstance;
    }
    public static NvContext createInstance(String name, boolean windowResizable){
        if(appInstance == null)
            appInstance = new NvContext(name, windowResizable);
        return appInstance;
    }

    private void handleMacPath(){
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            if (System.getProperty("org.lwjgl.librarypath") == null) {
                System.setProperty(
                        "org.lwjgl.librarypath",
                        "/opt/homebrew/lib"
                );
            }
        }
    }

    private NvContext(String name, int maxVertices, int maxIndices, boolean resizable, Dimension windowDim) {
        appInstance = this;
        handleMacPath();
        MoltenVKBootstrap.setup();

        this.MAX_VERTICES = maxVertices;
        this.MAX_INDICES = maxIndices;
        this.currentCameraUpdatable = (_) -> {};

        var nano = System.nanoTime();

        NvLogger.initialize(name, MAJOR_VERSION, MINOR_VERSION, PATCH);
        initWindow(name, resizable, windowDim);
        logEngine("Window initialized");
        initVulkan();
        logEngine("Vulkan initialized");
        CollisionManager.initialize();
        logEngine("Collisions initialized");
        AudioManager.init();
        logEngine("OpenAL Audio Engine initialized successfully.");
        this.eventSystem = new EventSystem();
        logEngine("Event system initialized");
        GameSaveManager.initialize("save/"+name + "_save.bin");
        logEngine("GameSaveManager initialized successfully");

        float finish = (System.nanoTime() - nano) / 1_000_000f;
        logEngine("STARTED in " + finish + " milliseconds");
        logEngine("-----------Program started successfully-------------");
    }

    private NvContext(String name, boolean windowResizable) {
        this(name, DEF_MAX_VERTICES, DEF_MAX_INDICES, windowResizable, SCREEN);
    }

    private NvContext(boolean windowResizable, Dimension windowDimension) {
        this("NV2D project", DEF_MAX_VERTICES, DEF_MAX_INDICES, windowResizable, windowDimension);
    }

    private NvContext(String name, boolean windowResizable, Dimension windowDimension) {
        this(name, DEF_MAX_VERTICES, DEF_MAX_INDICES, windowResizable, windowDimension);
    }

    /**
     * Adds an updatable object to the context.
     * @param updatable
     */
    public void addUpdatable(Updatable updatable){
        this.updatable.add(updatable);
    }

    /**
     * Removes an updatable object from the context.
     * @param updatable
     */
    public void removeUpdatable(Updatable updatable){
        this.updatable.remove(updatable);
    }

    /**
     * Adds a drawable object to the context.
     * @param drawable
     */
    public void addDrawable(Drawable drawable){
        drawables.add(drawable);
    }

    /**
     * Removes a drawable object from the context.
     * @param drawable
     */
    public void removeDrawable(Drawable drawable){
        drawables.remove(drawable);
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
        private int localFrameCount  = 0;
        private float localFpsSum    = 0;

        @Override
        public void update(float dt) {
            localFrameCount++;
            if (dt > 0) localFpsSum += (1.0f / dt);
            if (localFrameCount >= 30) {
                float averageFps = localFpsSum / localFrameCount;
                displayString = String.format("FPS: %.1f", averageFps);
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
        }
    };

    /** Updated in 1.6.2 */
    private void rebuildScene() {
        final float w = getEffectiveRenderWidth();
        final float h = getEffectiveRenderHeight();
        final float wu = 8.0f / 512.0f;
        final float wv = 8.0f / 512.0f;

        graphic.initialize(w, h, wu, wv, fontAtlas);

        CollisionManager.handleCollisions();

        rootComponent.drawAllComponents(graphic);
        for(Drawable drawable : drawables)
            drawable.draw(graphic);

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

    private void initWindow(String name, boolean resizable, Dimension windowDimension) {
        if (!glfwInit()) throw new EngineEx("Impossibile inizializzare GLFW");

        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        glfwWindowHint(GLFW_RESIZABLE, resizable ? GLFW_TRUE : GLFW_FALSE);

        window = glfwCreateWindow(windowDimension.width, windowDimension.height, name, 0, 0);
        if (window == 0) throw new EngineEx("Impossibile creare la finestra GLFW");

        glfwSetFramebufferSizeCallback(window, (windowHandle, width, height) -> {
            framebufferResized = true;
            markSceneDirty(); // Mark scene dirty on resize
        });

        glfwSetWindowFocusCallback(window, (windowHandle, focused) -> {
            windowFocused = focused;
            inputPending = true;
            if (focused) {
                markSceneDirty();
            }
        });

        mouseButtonCallback = GLFWMouseButtonCallback.create(ClickSystem.inputCallback(window));
        keyboardCallback = GLFWKeyCallback.create(KeyboardSystem.keyboardCallBack());

        glfwSetKeyCallback(window, keyboardCallback);
        glfwSetMouseButtonCallback(window, mouseButtonCallback);

        glfwSetCursorPosCallback(window, (windowHandle, x, y) -> {
            mouseX = (int) x;
            mouseY = (int) y;
            mouseMoved = true;
            inputPending = true;
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

        if (showFPS){
            addUpdatable(fpsDisplay);
        }
    }



    public void setKeyboardFocus(KeyboardListener focused) {
        KeyboardSystem.focused = focused;
    }

    public void setCamera(NvCamera camera){
        NvGraphic.setCurrentCamera(camera);
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

        int imageCount = swapchain.getImageCount();
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

        this.postProcessPipeline = new PostProcessPipeline(device, swapchain, internalRenderTarget);
        logEngine("PostProcess pipeline created");

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
        long vertexBufferSize = (long) MAX_VERTICES * NvGraphic.FLOATS_PER_VERTEX * Float.BYTES;
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
        currentCameraUpdatable.update(dt);
        rootComponent.tickAllComponents(dt);
        runUpdatablesParallel(dt);
        if (mouseMoved) {
            HoverSystem.handleHover(window, rootComponent);
            mouseMoved = false;
        }

        if (dt > 0) {
            fps = 1.0F / dt;
        }
    }

    /** Updated in 1.6. */
    private void runUpdatablesParallel(float dt) {
        int size = updatable.size();
        if (size == 0) {
            return;
        }
        if (size < PARALLEL_UPDATE_THRESHOLD) {
            for (int i = 0; i < size; i++) {
                updatable.get(i).update(dt);
            }
            return;
        }

        List<Future<?>> futures = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Updatable updatable = this.updatable.get(i);
            futures.add(updatePool.submit(() -> updatable.update(dt)));
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
            double frameStart = glfwGetTime();
            float deltaTime = (float) (frameStart - lastFrameTime);
            lastFrameTime = frameStart;

            glfwPollEvents();
            boolean hadInput = inputPending;
            inputPending = false;
            tickHandler(deltaTime);

            boolean hasAnimatedPostProcess = postProcessSettings.isEnabled() &&
                    (postProcessSettings.getFilmGrainStrength() > 0 || postProcessSettings.getScanlineStrength() > 0);

            boolean canRender = !idleWhenUnfocused || windowFocused;
            boolean shouldDraw = canRender && (sceneDirty || framebufferResized || hasAnimatedPostProcess);
            if (shouldDraw) {
                drawFrame();
            }

            throttleFrame(frameStart, !canRender || canThrottleIdle(shouldDraw, hadInput));
            if (inputPending) {
                lastFrameTime = glfwGetTime();
            }
        }
        vkDeviceWaitIdle(device);
    }

    private boolean canThrottleIdle(boolean frameHadWork, boolean hadInput) {
        return !frameHadWork && !hadInput && !currentCameraUpdatable.isActive() && !hasActiveUpdatables();
    }

    /** Updated in 1.6. */
    private boolean hasActiveUpdatables() {
        int size = updatable.size();
        for (int i = 0; i < size; i++) {
            if (updatable.get(i).isActive()) {
                return true;
            }
        }
        return false;
    }

    private void throttleFrame(double frameStart, boolean idle) {
        int frameLimit = getEffectiveFrameLimit(idle);
        if (frameLimit <= 0) {
            return;
        }

        double targetFrameTime = 1.0 / frameLimit;
        boolean preciseLimit = !idle;
        while (glfwGetTime() - frameStart < targetFrameTime) {
            double remaining = targetFrameTime - (glfwGetTime() - frameStart);
            if (remaining > 0.001) {
                if (preciseLimit) {
                    try {
                        Thread.sleep(Math.max(1, (long) (remaining * 1000 - 1)));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                } else {
                    glfwWaitEventsTimeout(remaining);
                    if (inputPending) {
                        return;
                    }
                }
            } else if (preciseLimit) {
                Thread.onSpinWait();
            } else {
                return;
            }
        }
    }

    private int getEffectiveFrameLimit(boolean idle) {
        if (!idle) {
            return targetFps;
        }
        if (targetFps > 0) {
            return Math.min(targetFps, idleFps);
        }
        return idleFps;
    }

    /** Updated in 1.6.2 with PostProcessPipeline */
    private void drawFrame() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            vkWaitForFences(device, inFlightFence, true, Long.MAX_VALUE);
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

            boolean hasAnimatedPostProcess = postProcessSettings.isEnabled() &&
                    (postProcessSettings.getFilmGrainStrength() > 0 || postProcessSettings.getScanlineStrength() > 0);

            // Record commands if scene is dirty, resized, dirty buffer, or animated post-processing
            if (sceneDirty || framebufferResized || commandBufferDirty[imageIndex] || hasAnimatedPostProcess) {
                long fenceSignaledAt = System.nanoTime();
                if (gpuSubmitTime > 0) {
                    gpuTotalMs = (fenceSignaledAt - gpuSubmitTime) / 1_000_000f;
                }

                if (sceneDirty || framebufferResized) {
                    rebuildScene(); // This will set sceneDirty = false
                }
                ubo.update(imageIndex, 0f, getEffectiveRenderWidth(), getEffectiveRenderHeight(), 0f);

                if (postProcessSettings.isEnabled()) {
                    commandBuffers.recordWithPostProcess(
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
                            internalRenderTarget.getWidth(),
                            internalRenderTarget.getHeight(),
                            swapchain.getWidth(),
                            swapchain.getHeight(),
                            internalRenderTargetInitialized,
                            postProcessPipeline,
                            postProcessSettings,
                            (float) glfwGetTime()
                    );
                } else {
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
                            pixelPerfect,
                            internalRenderTargetInitialized
                    );
                }

                internalRenderTargetInitialized = true;
                commandBufferDirty[imageIndex] = false;
            }

            // Always submit and present to keep the swapchain alive and display the latest recorded frame
            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pWaitSemaphores(stack.longs(imageAvailableSemaphore))
                    .pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT))
                    .pCommandBuffers(stack.pointers(commandBuffers.getCommandBuffer(imageIndex)))
                    .pSignalSemaphores(stack.longs(renderFinishedSemaphore));

            gpuSubmitTime = System.nanoTime();
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

    /** @since 1.6 */
    private float getEffectiveRenderWidth() {
        if (internalResolutionWidth > 0 && internalResolutionHeight > 0) {
            return internalResolutionWidth;
        }
        return Math.max(1.0f, swapchain.getWidth() * getEffectiveRenderScale());
    }

    /** @since 1.6 */
    private float getEffectiveRenderHeight() {
        if (internalResolutionWidth > 0 && internalResolutionHeight > 0) {
            return internalResolutionHeight;
        }
        return Math.max(1.0f, swapchain.getHeight() * getEffectiveRenderScale());
    }

    /** @since 1.6 */
    private float getEffectiveRenderScale() {
        float scale = renderScale <= 0.0f ? 1.0f : renderScale;
        return pixelPerfect ? Math.max(1.0f, Math.round(scale)) : scale;
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
        if (postProcessPipeline != null) {
            postProcessPipeline.recreateSwapchain(swapchain, internalRenderTarget);
        }
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
                    .applicationVersion(
                            VK_MAKE_VERSION(
                                    MAJOR_VERSION,
                                    MINOR_VERSION,
                                    PATCH
                            )
                    )
                    .pEngineName(stack.UTF8(ENGINE_NAME))
                    .engineVersion(
                            VK_MAKE_VERSION(
                                    MAJOR_VERSION,
                                    MINOR_VERSION,
                                    PATCH
                            )
                    )
                    .apiVersion(VK_API_VERSION_1_0);

            // ---------------------------------------------------------
            // GLFW instance extensions
            // ---------------------------------------------------------

            PointerBuffer glfwExtensions =
                    GLFWVulkan.glfwGetRequiredInstanceExtensions();

            if (glfwExtensions == null) {
                throw new EngineEx(
                        "Estensioni Vulkan GLFW non trovate."
                );
            }

            // ---------------------------------------------------------
            // Enumerate available instance extensions
            // ---------------------------------------------------------

            IntBuffer extensionCount =
                    stack.mallocInt(1);

            if (vkEnumerateInstanceExtensionProperties(
                    (String) null,
                    extensionCount,
                    null
            ) != VK_SUCCESS) {
                throw new EngineEx(
                        "Impossibile enumerare le instance extensions Vulkan."
                );
            }

            VkExtensionProperties.Buffer availableExtensions =
                    VkExtensionProperties.malloc(
                            extensionCount.get(0)
                    );

            if (vkEnumerateInstanceExtensionProperties(
                    (String) null,
                    extensionCount.rewind(),
                    availableExtensions
            ) != VK_SUCCESS) {
                throw new EngineEx(
                        "Impossibile leggere le instance extensions Vulkan."
                );
            }

            boolean hasProperties2 = false;

            for (VkExtensionProperties extension :
                    availableExtensions) {

                String name = extension.extensionNameString();

                if (name.equals(
                        VK_KHR_GET_PHYSICAL_DEVICE_PROPERTIES_2_EXTENSION_NAME
                )) {
                    hasProperties2 = true;
                    break;
                }
            }

            if (!hasProperties2) {
                throw new EngineEx(
                        "La implementazione Vulkan non espone " +
                                "VK_KHR_get_physical_device_properties2."
                );
            }

            // ---------------------------------------------------------
            // Build enabled extension list
            // ---------------------------------------------------------

            int glfwExtensionCount =
                    glfwExtensions.remaining();

            PointerBuffer enabledExtensions =
                    stack.mallocPointer(
                            glfwExtensionCount + 1
                    );

            for (int i = 0; i < glfwExtensionCount; i++) {
                enabledExtensions.put(
                        glfwExtensions.get(i)
                );
            }

            enabledExtensions.put(
                    stack.UTF8(
                            VK_KHR_GET_PHYSICAL_DEVICE_PROPERTIES_2_EXTENSION_NAME
                    )
            );

            enabledExtensions.flip();

            // ---------------------------------------------------------
            // Create instance
            // ---------------------------------------------------------

            VkInstanceCreateInfo createInfo =
                    VkInstanceCreateInfo.calloc(stack)
                            .sType(
                                    VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO
                            )
                            .pApplicationInfo(appInfo)
                            .ppEnabledExtensionNames(
                                    enabledExtensions
                            );

            PointerBuffer pInstance =
                    stack.mallocPointer(1);

            int result = vkCreateInstance(
                    createInfo,
                    null,
                    pInstance
            );

            if (result != VK_SUCCESS) {
                throw new EngineEx(
                        "Impossibile creare l'istanza Vulkan. VkResult="
                                + result
                );
            }

            this.instance =
                    new VkInstance(
                            pInstance.get(0),
                            createInfo
                    );

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

            // ---------------------------------------------------------
            // 1. Enumerate physical devices
            // ---------------------------------------------------------

            IntBuffer pDeviceCount = stack.mallocInt(1);

            if (vkEnumeratePhysicalDevices(
                    instance,
                    pDeviceCount,
                    null
            ) != VK_SUCCESS) {
                throw new EngineEx("Impossibile enumerare le GPU Vulkan.");
            }

            if (pDeviceCount.get(0) == 0) {
                throw new EngineEx("Nessuna GPU Vulkan trovata.");
            }

            PointerBuffer pPhysicalDevices =
                    stack.mallocPointer(pDeviceCount.get(0));

            if (vkEnumeratePhysicalDevices(
                    instance,
                    pDeviceCount,
                    pPhysicalDevices
            ) != VK_SUCCESS) {
                throw new EngineEx("Impossibile ottenere le GPU Vulkan.");
            }

            this.physicalDevice =
                    new VkPhysicalDevice(
                            pPhysicalDevices.get(0),
                            instance
                    );

            // ---------------------------------------------------------
            // 2. Queue family
            // ---------------------------------------------------------

            IntBuffer pQueueFamilyCount =
                    stack.mallocInt(1);

            vkGetPhysicalDeviceQueueFamilyProperties(
                    physicalDevice,
                    pQueueFamilyCount,
                    null
            );

            VkQueueFamilyProperties.Buffer queueFamilies =
                    VkQueueFamilyProperties.calloc(
                            pQueueFamilyCount.get(0),
                            stack
                    );

            vkGetPhysicalDeviceQueueFamilyProperties(
                    physicalDevice,
                    pQueueFamilyCount,
                    queueFamilies
            );

            int graphicsQFI = -1;

            for (int i = 0; i < queueFamilies.capacity(); i++) {

                if ((queueFamilies.get(i).queueFlags()
                        & VK_QUEUE_GRAPHICS_BIT) != 0) {

                    graphicsQFI = i;
                    break;
                }
            }

            if (graphicsQFI == -1) {
                throw new EngineEx("Nessuna queue grafica trovata.");
            }

            VkDeviceQueueCreateInfo.Buffer queueCreateInfo =
                    VkDeviceQueueCreateInfo.calloc(1, stack)
                            .sType(
                                    VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO
                            )
                            .queueFamilyIndex(graphicsQFI)
                            .pQueuePriorities(
                                    stack.floats(1.0f)
                            );

            // ---------------------------------------------------------
            // 3. Enumerate device extensions
            // ---------------------------------------------------------

            IntBuffer extCount = stack.mallocInt(1);

            vkEnumerateDeviceExtensionProperties(
                    physicalDevice,
                    (String) null,
                    extCount,
                    null
            );

            VkExtensionProperties.Buffer availableExtensions =
                    VkExtensionProperties.malloc(
                            extCount.get(0)
                    );

            vkEnumerateDeviceExtensionProperties(
                    physicalDevice,
                    (String) null,
                    extCount.rewind(),
                    availableExtensions
            );

            boolean hasPortabilitySubset = false;
            boolean hasDescriptorIndexing = false;

            for (VkExtensionProperties ext : availableExtensions) {

                String name = ext.extensionNameString();

                if (name.equals("VK_KHR_portability_subset")) {
                    hasPortabilitySubset = true;
                }

                if (name.equals("VK_EXT_descriptor_indexing")) {
                    hasDescriptorIndexing = true;
                }
            }

            // ---------------------------------------------------------
            // 4. Descriptor indexing is required by NV2D
            // ---------------------------------------------------------

            if (!hasDescriptorIndexing) {
                throw new EngineEx(
                        "La GPU non espone VK_EXT_descriptor_indexing. " +
                                "NV2D richiede il non-uniform indexing delle texture."
                );
            }

            // ---------------------------------------------------------
            // 5. Device extensions
            // ---------------------------------------------------------

            int extensionCount =
                    hasPortabilitySubset ? 3 : 2;

            PointerBuffer deviceExtensions =
                    stack.mallocPointer(extensionCount);

            deviceExtensions.put(
                    stack.UTF8(VK_KHR_SWAPCHAIN_EXTENSION_NAME)
            );

            deviceExtensions.put(
                    stack.UTF8("VK_EXT_descriptor_indexing")
            );

            if (hasPortabilitySubset) {
                deviceExtensions.put(
                        stack.UTF8("VK_KHR_portability_subset")
                );
            }

            deviceExtensions.flip();

            // ---------------------------------------------------------
            // 6. Query descriptor indexing feature
            // ---------------------------------------------------------

            VkPhysicalDeviceDescriptorIndexingFeaturesEXT supportedFeatures =
                    VkPhysicalDeviceDescriptorIndexingFeaturesEXT.calloc(stack)
                            .sType(
                                    VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DESCRIPTOR_INDEXING_FEATURES_EXT
                            );

            VkPhysicalDeviceFeatures2 features2 =
                    VkPhysicalDeviceFeatures2.calloc(stack)
                            .sType(
                                    VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2
                            )
                            .pNext(
                                    supportedFeatures.address()
                            );

            vkGetPhysicalDeviceFeatures2KHR(
                    physicalDevice,
                    features2
            );

            if (!supportedFeatures.shaderSampledImageArrayNonUniformIndexing()) {
                throw new EngineEx(
                        "La GPU espone VK_EXT_descriptor_indexing ma " +
                                "non supporta shaderSampledImageArrayNonUniformIndexing."
                );
            }

            // ---------------------------------------------------------
            // 7. Enable required descriptor indexing feature
            // ---------------------------------------------------------

            VkPhysicalDeviceDescriptorIndexingFeaturesEXT enabledFeatures =
                    VkPhysicalDeviceDescriptorIndexingFeaturesEXT.calloc(stack)
                            .sType(
                                    VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DESCRIPTOR_INDEXING_FEATURES_EXT
                            )
                            .shaderSampledImageArrayNonUniformIndexing(true);

            // ---------------------------------------------------------
            // 8. Device creation
            // ---------------------------------------------------------

            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                    .pQueueCreateInfos(queueCreateInfo)
                    .ppEnabledExtensionNames(deviceExtensions);

            PointerBuffer pDevice =
                    stack.mallocPointer(1);

            if (vkCreateDevice(
                    physicalDevice,
                    createInfo,
                    null,
                    pDevice
            ) != VK_SUCCESS) {

                throw new EngineEx(
                        "Impossibile creare il Logical Device."
                );
            }

            this.device =
                    new VkDevice(
                            pDevice.get(0),
                            physicalDevice,
                            createInfo
                    );

            // ---------------------------------------------------------
            // 9. Graphics queue
            // ---------------------------------------------------------

            PointerBuffer pQueue =
                    stack.mallocPointer(1);

            vkGetDeviceQueue(
                    device,
                    graphicsQFI,
                    0,
                    pQueue
            );

            this.graphicsQueue =
                    new VkQueue(
                            pQueue.get(0),
                            device
                    );
        }
    }
    /** Updated in 1.6. */
    private void createRenderPass() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkAttachmentDescription.Buffer colorAttachment = VkAttachmentDescription.calloc(1, stack)
                    .format(swapchain.getFormat())
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                    .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                    .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                    .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                    .initialLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
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

    /** Updated in 1.6. */
    private void createInternalRenderTarget() {
        if (internalRenderTarget != null) {
            internalRenderTarget.close();
        }

        this.internalRenderTarget = new InternalRenderTarget(
                device,
                physicalDevice,
                renderPass,
                Math.max(1, Math.round(getEffectiveRenderWidth())),
                Math.max(1, Math.round(getEffectiveRenderHeight())),
                swapchain.getFormat()
        );
        this.internalRenderTargetInitialized = false;
    }

    /** Updated in 1.6. */
    private void syncRootSizeToRenderTarget() {
        if (rootComponent == null) {
            return;
        }
        rootComponent.setW(Math.max(1, Math.round(getEffectiveRenderWidth())));
        rootComponent.setH(Math.max(1, Math.round(getEffectiveRenderHeight())));
    }

    private void cleanup() {
        logEngine("-----------Clean up-----------");
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
        if (postProcessPipeline != null) {
            postProcessPipeline.close();
            postProcessPipeline = null;
        }
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
        logEngine("-----------Program ended successfully-----------");
        glfwTerminate();
    }


}
