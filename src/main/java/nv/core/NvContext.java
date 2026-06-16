package nv.core;

import nv.components.*;
import nv.components.camera.NvCamera;
import nv.core.collision.AABB;
import nv.core.collision.Collidable;
import nv.core.collision.CollisionSystem;
import nv.core.data.DynamicVertexBuffer;
import nv.core.data.DynamicIndexBuffer;
import nv.core.data.FontAtlas;
import nv.core.data.TextureImage;
import nv.core.data.DescriptorManager;
import nv.core.data.NvImage;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.*;
import org.lwjgl.vulkan.*;
import org.lwjgl.system.MemoryStack;

import java.awt.Font;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static nv.core.NvGraphic.camera;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

/**
 * <h3>Entry point for the NV2D game engine</h3>
 * <p>SingleTone class responsible for managing the application's Vulkan resources and rendering pipeline</p>
 * @since 1.0
 */
public final class NvContext implements Runnable {
    private static final int MAJOR_VERSION = 1;
    private static final int MINOR_VERSION = 0;
    private static final int PATCH = 0;
    private static final String ENGINE_NAME = "NV2Dlib";

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
    private long[] framebuffers;
    private UpdateCycle currentCameraUpdateCycle;
    private CollisionSystem collisionSystem;

    // Capacità massima pre-allocata (aumenta se servi più geometria)
    private int maxVertices = 300_000; // vertici × 8 float
    private int maxIndices  = 550_000; // indici short
    
    // Texture images caricati (massimo 15 per il shader)
    private static final int MAX_TEXTURES = 15;
    private final NvImage[] loadedTextures = new NvImage[MAX_TEXTURES];
    private int textureCount = 1;

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

    private final List<UpdateCycle> updatables = new ArrayList<>(10);

    private NvCont rootComponent;

    private float fps = -1;
    private boolean showFPS = false;

    public void setCurrentCameraUpdateCycle(UpdateCycle updateCycle){
        currentCameraUpdateCycle = updateCycle;
    }

    private final Map<String, NvImage> images = new HashMap<>(16);
    /**
     * Carica un'immagine da file e la registra nel motore di rendering.
     * Le immagini caricate possono essere disegnate usando {@link NvGraphic#drawImage} e {@link NvGraphic#drawImageRegion}.
     *
     * @param filePath percorso del file immagine (relativo o assoluto)
     * @return NvImage registrato con un indice di texture assegnato
     * @throws RuntimeException se il numero massimo di texture (8) è stato raggiunto o se il file non può essere caricato
     */
    public synchronized NvImage loadImageAbsolute(String filePath) {
        var cached = images.get(filePath);
        if(cached != null)
            return cached;
        if (textureCount >= MAX_TEXTURES) {
            throw new RuntimeException("Limite massimo di texture raggiunto (" + MAX_TEXTURES + ")");
        }
        NvImage image = NvImage.fromFile(device, physicalDevice, graphicsQueue, filePath);
        image.setTextureIndex(textureCount);
        loadedTextures[textureCount] = image;
        descriptorManager.updateTexture(textureCount, image.getTextureImage());
        textureCount++;
        images.put(filePath, image);
        return image;
    }

    /**
     * Carica un'immagine dal classpath e la registra nel motore di rendering.
     *
     * @param resourcePath percorso della risorsa nel classpath (es. "/images/sprite.png")
     * @return NvImage registrato con un indice di texture assegnato
     * @throws RuntimeException se il numero massimo di texture (8) è stato raggiunto o se la risorsa non esiste
     */
    public synchronized NvImage loadImage(String resourcePath) {
        var cached = images.get(resourcePath);
        if(cached != null)
            return cached;
        if (textureCount >= MAX_TEXTURES) {
            throw new RuntimeException("Limite massimo di texture raggiunto (" + MAX_TEXTURES + ")");
        }
        NvImage image = NvImage.fromResource(device, physicalDevice, graphicsQueue, "/textures/"+resourcePath);
        image.setTextureIndex(textureCount);
        loadedTextures[textureCount] = image;
        descriptorManager.updateTexture(textureCount, image.getTextureImage());
        textureCount++;
        images.put(resourcePath, image);
        return image;
    }

    /**
     * Standard is AABB
     * @param collisionSystem new CollisionSystem
     */
    public void setCollisionSystem(CollisionSystem collisionSystem) {
        this.collisionSystem = collisionSystem;
    }

    public void setShowFPS(boolean shouldShow) {
        this.showFPS = shouldShow;
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
        return page;
    }

    public NvCont addAndSetPage(String key, NvCont page){
        pages.put(key, page);
        this.rootComponent = page;
        rootComponent.setW(swapchain.getWidth());
        rootComponent.setH(swapchain.getHeight());
        return page;
    }

    public void remove(String key){
        pages.remove(key);
    }

    private static NvContext appInstance;

    public void setCurrentPage(String key){
        rootComponent = pages.get(key);
        rootComponent.setW(swapchain.getWidth());
        rootComponent.setH(swapchain.getHeight());
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
        this.maxVertices = maxVertices;
        this.maxIndices  = maxIndices;
        this.currentCameraUpdateCycle = (_) -> {};
        this.collisionSystem = new AABB();
        initWindow(name, windowDim);
        initVulkan();

    }
    private NvContext(String name) {
        this.currentCameraUpdateCycle = (_) -> {};
        initWindow(name, SCREEN);
        initVulkan();
        this.collisionSystem = new AABB();
    }
    private NvContext(Dimension windowDimension) {
        this.currentCameraUpdateCycle = (_) -> {};
        initWindow("NV2D game", windowDimension);
        initVulkan();
        this.collisionSystem = new AABB();
    }
    private NvContext(String name, Dimension windowDimension) {
        this.currentCameraUpdateCycle = (_) -> {};
        initWindow(name, windowDimension);
        initVulkan();
        this.collisionSystem = new AABB();
    }

    public void addUpdatable(UpdateCycle updateCycle){
        updatables.add(updateCycle);
    }
    public void removeUpdatable(UpdateCycle updateCycle){
        updatables.remove(updateCycle);
    }

    /**
     * Adds a new component to the current root component
     * @param component to add
     */
    public void addTreeComponent(NvComp component){
        rootComponent.addChild(component);
    }
    public void removeComponent(NvComp component){
        rootComponent.removeChild(component);
    }

    private int graphicsIndexCount;
    private int imageIndexCount;
    private int imageIndexOffset;

    private final NvGraphic graphic = new NvPixelGraphic();
    private final List<NvComp> canCollide = new ArrayList<>(20);

    public void addCanCollide(NvComp component){
        canCollide.add(component);
    }
    public void removeCanCollide(NvComp component){
        canCollide.remove(component);
    }

    private int frameCount = 0;
    private float fpsSum = 0.0F;
    private float oldFps;

    private final NvComp fpsDisplay = new NvComp(100,100,200,50){

        @Override
        public void update(float dt) {
            frameCount++;
            fpsSum += fps;
            if(frameCount % 60 == 0){
                oldFps = fpsSum / frameCount;
                frameCount = 0;
                fpsSum = 0;
            }
        }
        @Override
        public void drawIntern(NvGraphic g) {
            setHUD(true);
            graphic.setRGB(0,0,0);
            graphic.drawRect(0, 0, 260, 70);
            if(frameCount % 60 == 0){
                graphic.drawText(String.format("FPS: %.2f", fps), 0, 0);
            }else {
                graphic.drawText(String.format("FPS: %.2f", oldFps), 0, 0);
            }
        }
    };

    private void rebuildScene() {
        final float w = swapchain.getWidth();
        final float h = swapchain.getHeight();
        final float wu = 8.0f / 512.0f;
        final float wv = 8.0f / 512.0f;

        graphic.initialize(w, h, wu, wv, fontAtlas);

        handleCollisions();

        graphic.setComponent(rootComponent);
        graphic.drawRect(camera.x, camera.y,
                    rootComponent.getW(),
                    rootComponent.getH(),
                    rootComponent.getR(),
                    rootComponent.getG(),
                    rootComponent.getB());

        rootComponent.draw(graphic);

        if(showFPS)
            fpsDisplay.draw(graphic);

        float[] gVerts = graphic.getVertices();
        int[]   gInds  = graphic.getIndices();
        float[] iVerts = graphic.getImageVertices();
        int[]   iInds  = graphic.getImageIndices();

        graphicsIndexCount = gInds.length;
        imageIndexCount    = iInds.length;
        imageIndexOffset   = graphicsIndexCount;

        float[] combinedVerts = new float[gVerts.length + iVerts.length];
        System.arraycopy(gVerts, 0, combinedVerts, 0, gVerts.length);
        System.arraycopy(iVerts, 0, combinedVerts, gVerts.length, iVerts.length);

        int[] combinedInds = new int[gInds.length + iInds.length];
        System.arraycopy(gInds, 0, combinedInds, 0, gInds.length);
        int vertexOffset = gVerts.length / 8;
        for (int i = 0; i < iInds.length; i++) {
            combinedInds[graphicsIndexCount + i] = iInds[i] + vertexOffset;
        }

        dynamicVertexBuffer.update(combinedVerts);
        dynamicIndexBuffer.update(combinedInds);
    }
    private void handleCollisions(){
        for(int i = 0; i < canCollide.size(); i++){
            var a = canCollide.get(i);
            for(int j = 0; j < canCollide.size(); j++){
                var b = canCollide.get(j);
                if(i != j && collisionSystem.isColliding(a, b)){
                    var collided1 = (Collidable) a;
                    var collided2 = (Collidable) b;
                    collided1.whenCollide(b);
                    collided2.whenCollide(a);

                    collisionSystem.resolveCollision(a,b);
                }
            }
        }
    }

    @Override
    public void run() {
        mainLoop();
        cleanup();
    }
    public void changeFont(Font font){
        this.fontAtlas = new FontAtlas(font);
    }

    private void initWindow(String name, Dimension windowDimension) {
        if (!glfwInit()) throw new IllegalStateException("Impossibile inizializzare GLFW");

        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        window = glfwCreateWindow(windowDimension.width, windowDimension.height, name, 0, 0);
        if (window == 0) throw new RuntimeException("Impossibile creare la finestra GLFW");

        glfwSetFramebufferSizeCallback(window, (windowHandle, width, height) -> {
            framebufferResized = true;
        });

        mouseButtonCallback = GLFWMouseButtonCallback.create(inputCallback());
        keyboardCallback = GLFWKeyCallback.create(keyboardCallBack());

        glfwSetKeyCallback(window, keyboardCallback);
        glfwSetMouseButtonCallback(window, mouseButtonCallback);

        glfwSetCursorPosCallback(window, (windowHandle, x, y) -> {
            mouseX = (int) x;
            mouseY = (int) y;
            mouseMoved = true;
        });
    }

    private KeyboardListener focused;

    public void setKeyboardFocus(KeyboardListener focused) {
        this.focused = focused;
    }

    private GLFWMouseButtonCallbackI inputCallback(){
        return (windowHandle, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
                var correctedCoords = getCorrectedCoords();
                for (var comp : rootComponent.getChildren()) {
                    if (comp instanceof Clickable clickable) {
                        if (comp.isInside(correctedCoords[0], correctedCoords[1])) {
                            clickable.onClick();
                        }
                    }
                }
            }
            else if(button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_RELEASE){
                var correctedCoords = getCorrectedCoords();
                for (var comp : rootComponent.getChildren()) {
                    if (comp instanceof Clickable clickable) {
                        if (comp.isInside(correctedCoords[0], correctedCoords[1])) {
                            clickable.onClickRelease();
                        }
                    }
                }
            }
        };
    }
    private final boolean[] keys = new boolean[GLFW_KEY_LAST + 1];

    private GLFWKeyCallbackI keyboardCallBack(){
        return (window, key, scancode, action, mods) -> {
            if(action == GLFW_PRESS || action == GLFW_REPEAT){
                keys[key] = true;
                focused.onKeyPressed(keys, mods);
            }
            else if (action == GLFW_RELEASE) {
                focused.onKeyReleased(keys, mods);
                keys[key] = false;
            }
        };
    }

    private void initVulkan() {
        createInstance();
        createSurface();
        pickPhysicalDeviceAndCreateLogicalDevice();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pWidth  = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            glfwGetFramebufferSize(window, pWidth, pHeight);
            this.swapchain = new Swapchain(device, surface, pWidth.get(0), pHeight.get(0));
        }
        this.rootComponent = new NvCont(0,0,swapchain.getWidth(), swapchain.getHeight());

        createRenderPass();

        this.fontAtlas   = new FontAtlas(new Font("SansSerif", Font.PLAIN, 42));
        this.fontTexture = new TextureImage(
                device, physicalDevice, graphicsQueue,
                fontAtlas.getPixelBuffer(), fontAtlas.getWidth(), fontAtlas.getHeight()
        );

        int imageCount = swapchain.getImageViews().length;
        this.ubo               = new OrthoUBO(device, physicalDevice, imageCount);
        this.descriptorManager = new DescriptorManager(device, ubo, fontTexture, imageCount);

        this.pipeline = new GraphicsPipeline(
                device, swapchain, renderPass,
                descriptorManager.getDescriptorSetLayoutHandle()
        );

        this.texturePipeline = new TexturePipeline(
                device, swapchain, renderPass,
                descriptorManager.getDescriptorSetLayoutHandle()
        );

        createFramebuffers();
        buildCombinedGeometry();

        this.commandBuffers = new CommandBuffers(device, pipeline, swapchain);
        createSyncObjects();
    }

    // -------------------------------------------------------------------------
    // Scena demo: quadrato, triangolo e testo centrato
    // -------------------------------------------------------------------------

    /**
     * Pre-alloca i buffer GPU con la capacità massima dichiarata.
     * I dati vengono caricati subito tramite rebuildScene().
     */
    private void buildCombinedGeometry() {
        long vertexBufferSize = (long) maxVertices * 8 * Float.BYTES;
        this.dynamicVertexBuffer = new DynamicVertexBuffer(device, physicalDevice, vertexBufferSize);
        this.dynamicIndexBuffer  = new DynamicIndexBuffer(device, physicalDevice, maxIndices);
        rebuildScene();
    }

    // -------------------------------------------------------------------------
    // Sincronizzazione, loop principale e frame rendering
    // -------------------------------------------------------------------------

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
                throw new RuntimeException("Impossibile creare gli oggetti di sincronizzazione.");
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

        for(UpdateCycle updateCycle : updatables){
            updateCycle.update(dt);
        }

        if (mouseMoved) {
            handleHover();
            mouseMoved = false;
        }

        if(showFPS){
            fps = 1.0F / dt;
        }
    }

    private int[] getCorrectedCoords(){
        double[] x1 = new double[1];
        double[] y1 = new double[1];

        glfwGetCursorPos(window, x1, y1);

        int[] windowWidth = new int[1];
        int[] windowHeight = new int[1];
        int[] framebufferWidth = new int[1];
        int[] framebufferHeight = new int[1];

        glfwGetWindowSize(window, windowWidth, windowHeight);
        glfwGetFramebufferSize(window, framebufferWidth, framebufferHeight);

        int convertedMouseX = (int) (x1[0] * framebufferWidth[0] / windowWidth[0]);
        int convertedMouseY = (int) (y1[0] * framebufferHeight[0] / windowHeight[0]);

        rootComponent.handleHover(convertedMouseX, convertedMouseY);

        return new int[]{convertedMouseX, convertedMouseY};
    }

    private void handleHover(){
        var correctedCoords = getCorrectedCoords();
        rootComponent.handleHover(correctedCoords[0], correctedCoords[1]);
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
        }
        vkDeviceWaitIdle(device);
    }

    private void drawFrame() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            vkWaitForFences(device, inFlightFence, true, Long.MAX_VALUE);
            vkResetFences(device, inFlightFence);

            rebuildScene();

            IntBuffer pImageIndex = stack.mallocInt(1);
            int acquireResult = vkAcquireNextImageKHR(device, swapchain.getHandle(), Long.MAX_VALUE,
                    imageAvailableSemaphore, VK_NULL_HANDLE, pImageIndex);

            if (acquireResult == VK_ERROR_OUT_OF_DATE_KHR) {
                recreateSwapchain();
                return;
            }

            if (acquireResult != VK_SUCCESS && acquireResult != VK_SUBOPTIMAL_KHR) {
                throw new RuntimeException("Errore durante l'acquisizione dell'immagine della swapchain.");
            }

            int imageIndex = pImageIndex.get(0);

            ubo.update(imageIndex, 0f, (float) swapchain.getWidth(), (float) swapchain.getHeight(), 0f);

            commandBuffers.recordDual(
                    renderPass,
                    framebuffers[imageIndex],
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
                    swapchain.getWidth(),
                    swapchain.getHeight()
            );

            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pWaitSemaphores(stack.longs(imageAvailableSemaphore))
                    .pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT))
                    .pCommandBuffers(stack.pointers(commandBuffers.getCommandBuffer()))
                    .pSignalSemaphores(stack.longs(renderFinishedSemaphore));

            if (vkQueueSubmit(graphicsQueue, submitInfo, inFlightFence) != VK_SUCCESS) {
                throw new RuntimeException("Errore nel submit della lista comandi alla GPU.");
            }

            LongBuffer signalSemaphores = stack.longs(renderFinishedSemaphore);
            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                    .pWaitSemaphores(signalSemaphores)
                    .swapchainCount(1)
                    .pSwapchains(stack.longs(swapchain.getHandle()))
                    .pImageIndices(pImageIndex);

            int presentResult = vkQueuePresentKHR(graphicsQueue, presentInfo);

            if (presentResult == VK_ERROR_OUT_OF_DATE_KHR || presentResult == VK_SUBOPTIMAL_KHR || framebufferResized) {
                framebufferResized = false;
                recreateSwapchain();
            } else if (presentResult != VK_SUCCESS) {
                throw new RuntimeException("Errore durante la presentazione della swapchain.");
            }
        }
    }

    private void recreateSwapchain() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
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
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);

            glfwGetFramebufferSize(window, pWidth, pHeight);

            this.swapchain = new Swapchain(device, surface, pWidth.get(0), pHeight.get(0));
            this.rootComponent.setW(swapchain.getWidth());
            this.rootComponent.setH(swapchain.getHeight());
        }


        createFramebuffers();

        if (commandBuffers != null) {
            commandBuffers.free();
        }

        if (pipeline != null) pipeline.close();
        if (texturePipeline != null) texturePipeline.close();

        this.pipeline = new GraphicsPipeline(
                device,
                swapchain,
                renderPass,
                descriptorManager.getDescriptorSetLayoutHandle()
        );

        this.texturePipeline = new TexturePipeline(
                device,
                swapchain,
                renderPass,
                descriptorManager.getDescriptorSetLayoutHandle()
        );

        this.commandBuffers = new CommandBuffers(device, pipeline, swapchain);
    }

    private void cleanupSwapchainResources() {
        if (framebuffers != null) {
            for (long framebuffer : framebuffers) {
                vkDestroyFramebuffer(device, framebuffer, null);
            }
            framebuffers = null;
        }

        if (swapchain != null) {
            swapchain.close();
            swapchain = null;
        }
    }


    // -------------------------------------------------------------------------
    // Inizializzazione Vulkan (instance, surface, device, renderpass, framebuffers)
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
            if (glfwExtensions == null) throw new RuntimeException("Estensioni Vulkan GLFW non trovate.");

            VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                    .pApplicationInfo(appInfo)
                    .ppEnabledExtensionNames(glfwExtensions);

            PointerBuffer pInstance = stack.mallocPointer(1);
            if (vkCreateInstance(createInfo, null, pInstance) != VK_SUCCESS) {
                throw new RuntimeException("Impossibile creare l'istanza Vulkan.");
            }
            this.instance = new VkInstance(pInstance.get(0), createInfo);
        }
    }

    private void createSurface() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pSurface = stack.mallocLong(1);
            if (GLFWVulkan.glfwCreateWindowSurface(instance, window, null, pSurface) != VK_SUCCESS) {
                throw new RuntimeException("Impossibile creare la surface Vulkan.");
            }
            this.surface = pSurface.get(0);
        }
    }

    private void pickPhysicalDeviceAndCreateLogicalDevice() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pDeviceCount = stack.mallocInt(1);
            vkEnumeratePhysicalDevices(instance, pDeviceCount, null);
            if (pDeviceCount.get(0) == 0) throw new RuntimeException("Nessuna GPU Vulkan trovata.");

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
            if (graphicsQFI == -1) throw new RuntimeException("Nessuna queue grafica trovata.");

            VkDeviceQueueCreateInfo.Buffer queueCreateInfo = VkDeviceQueueCreateInfo.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                    .queueFamilyIndex(graphicsQFI)
                    .pQueuePriorities(stack.floats(1.0f));

            PointerBuffer deviceExtensions = stack.mallocPointer(2);
            deviceExtensions.put(stack.UTF8(VK_KHR_SWAPCHAIN_EXTENSION_NAME));
            deviceExtensions.put(stack.UTF8("VK_KHR_portability_subset"));
            deviceExtensions.flip();

            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                    .pQueueCreateInfos(queueCreateInfo)
                    .ppEnabledExtensionNames(deviceExtensions);

            PointerBuffer pDevice = stack.mallocPointer(1);
            if (vkCreateDevice(physicalDevice, createInfo, null, pDevice) != VK_SUCCESS) {
                throw new RuntimeException("Impossibile creare il Logical Device.");
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
                    .finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

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
                throw new RuntimeException("Impossibile creare il Render Pass.");
            }
            this.renderPass = pRenderPass.get(0);
        }
    }

    private void createFramebuffers() {
        long[] imageViews = swapchain.getImageViews();
        this.framebuffers = new long[imageViews.length];

        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer attachments = stack.mallocLong(1);

            VkFramebufferCreateInfo fbInfo = VkFramebufferCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                    .renderPass(renderPass)
                    .width(swapchain.getWidth())
                    .height(swapchain.getHeight())
                    .layers(1);

            for (int i = 0; i < imageViews.length; i++) {
                attachments.put(0, imageViews[i]);
                fbInfo.pAttachments(attachments);

                LongBuffer pFb = stack.mallocLong(1);
                if (vkCreateFramebuffer(device, fbInfo, null, pFb) != VK_SUCCESS) {
                    throw new RuntimeException("Impossibile creare il Framebuffer [" + i + "]");
                }
                this.framebuffers[i] = pFb.get(0);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Cleanup
    // -------------------------------------------------------------------------

    private void cleanup() {
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
        if (framebuffers        != null) for (long fb : framebuffers) vkDestroyFramebuffer(device, fb, null);
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
        if (surface             != 0)    vkDestroySurfaceKHR(instance, surface, null);
        if (device              != null) vkDestroyDevice(device, null);
        if (instance            != null) vkDestroyInstance(instance, null);

        glfwDestroyWindow(window);
        glfwTerminate();
    }


}
