package nv.core;

import nv.core.annotations.EngineCore;

import java.awt.*;

/**
 * <p>The correct way to build the application with multiple build options</p>
 * @since 1.0
 * @author Andrea Maruca
 */
@EngineCore
@SuppressWarnings("unused")
public final class ContextBuilder {
    private final NvContext ctx;

    public ContextBuilder(String name, boolean windowResizable, int vertexBufferSize, int indicesBufferSize){
        ctx = NvContext.createInstance(name, windowResizable, vertexBufferSize, indicesBufferSize);
    }

    public ContextBuilder(String name, boolean windowResizable, Dimension windowSize){
        ctx = NvContext.createInstance(name, windowResizable);
    }

    public ContextBuilder(String name, boolean windowResizable, Dimension windowSize, int vertexBufferSize, int indicesBufferSize){
        ctx = NvContext.createInstance(name, windowResizable);
    }

    public ContextBuilder(String name){
        ctx = NvContext.createInstance(name, true);
    }
    public ContextBuilder(String name, boolean resizable){
        ctx = NvContext.createInstance(name, resizable);
    }

    /**
     * Sets the Vsync mode for the application.
     * Vsync synchronizes the frame rate of the application with the refresh rate of the display
     * to reduce screen tearing and improve visual performance.
     *
     * @param vsync a boolean value where {@code true} enables Vsync and {@code false} disables it
     * @return the current instance of {@code ContextBuilder} to allow method chaining
     */
    public ContextBuilder setVsync(boolean vsync){
        ctx.setVsync(vsync);
        return this;
    }

    public ContextBuilder setInternalResolution(int width, int height) {
        ctx.setInternalResolution(width, height);
        return this;
    }
    public ContextBuilder setInternalResolution(ScreenSize size) {
        ctx.setInternalResolution(size);
        return this;
    }

    public ContextBuilder setRenderScale(float renderScale) {
        ctx.setRenderScale(renderScale);
        return this;
    }

    public ContextBuilder setPixelPerfect(boolean pixelPerfect) {
        ctx.setPixelPerfect(pixelPerfect);
        return this;
    }
    /**
     * Also deactivates Vsync
     * @param maxFps number of fps to limit
     * @return himself
     *
     * @since 1.0
     */
    public ContextBuilder setFpsLimit(int maxFps){
        ctx.setVsync(false);
        ctx.setFpsLimit(maxFps);
        return this;
    }

    public ContextBuilder setIdleFpsLimit(int idleFps){
        ctx.setIdleFpsLimit(idleFps);
        return this;
    }

    public ContextBuilder setIdleWhenUnfocused(boolean idleWhenUnfocused){
        ctx.setIdleWhenUnfocused(idleWhenUnfocused);
        return this;
    }

    /**
     * Enables the display of the frames-per-second (FPS) counter in the application.
     * This is useful for debugging and performance monitoring purposes.
     *
     * @return the current instance of {@code ContextBuilder} to allow method chaining
     */
    public ContextBuilder showFps(){
        ctx.setShowFPS(true);
        return this;
    }

    /**
     * <p>Adds a new class that uses delta t</p>
     * @param updatable class that uses delta t
     * @return himself
     *
     * @since 1.0
     */
    public ContextBuilder addUpdatable(Updatable updatable) {
        ctx.addUpdatable(updatable);
        return this;
    }

    /**
     * Enables the Retro CRT TV simulation preset (curvature, scanlines, vignette, bloom, aberration).
     */
    public ContextBuilder enableRetroCRT() {
        ctx.getPostProcess().presetRetroCRT();
        return this;
    }

    /**
     * Configures the post-processing settings via a lambda.
     */
    public ContextBuilder configurePostProcess(java.util.function.Consumer<PostProcessSettings> config) {
        config.accept(ctx.getPostProcess());
        return this;
    }

    /**
     * Builds the application
     * @return the built context
     */
    public NvContext build() {
        return ctx;
    }
}
