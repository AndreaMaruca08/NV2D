package nv.core.camera;

import nv.core.io.EmptyKeyboardListener;
import nv.core.NvContext;
import nv.core.annotations.EngineCore;
import nv.core.components.NvComp;
import nv.core.components.Vector2D;
import nv.core.graphic.NvGraphic;
import nv.core.io.KeyboardListener;
import nv.core.io.KeyboardSystem;
import nv.utils.NvTimer;
import nv.utils.camera.NvControlledCamera;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a camera that can be used to control the view of a 2D game.
 * <p>Using the translation methods, you can move the camera position and zoom level.</p>
 * <p>To set the current camera you need to call {@link NvGraphic#setCurrentCamera(NvCamera)}.</p>
 *
 * SEE ALSO {@link NvControlledCamera}
 * @since 1.0
 * @author Andrea Maruca
 */
@EngineCore
@SuppressWarnings("unused")
public class NvCamera {
    public float x;
    public float y;
    public float zoom;
    public NvContext context;
    private final NvTimer shakeTimer = new NvTimer(100);

    public NvCamera(float x, float y, float zoom) {
        this.x = x;
        this.y = y;
        this.zoom = zoom;
    }

    public void translate(Vector2D vector2D){
        x += vector2D.x;
        y += vector2D.y;
        NvContext.markSceneDirty();
    }
    public void translateOnCenter(Vector2D vector2D){
        if(context == null){
            context = NvContext.getInstance();
        }
        float safeZoom = Math.max(zoom, 0.0001f);
        x += vector2D.x - context.getRenderWidth() / safeZoom / 2.0f + 50.0f;
        y += vector2D.y - context.getRenderHeight() / safeZoom / 2.0f + 50.0f;
        NvContext.markSceneDirty();
    }
    public void translate(float x, float y){
        this.x += x;
        this.y += y;
        NvContext.markSceneDirty();
    }
    public void translateOnCenter(float x, float y){
        if(context == null){
            context = NvContext.getInstance();
        }
        float safeZoom = Math.max(zoom, 0.0001f);
        this.x += x - context.getRenderWidth() / safeZoom / 2.0f + 50.0f;
        this.y += y - context.getRenderHeight() / safeZoom / 2.0f + 50.0f;
        NvContext.markSceneDirty();
    }
    public void setXY(float x, float y){
        this.x = x;
        this.y = y;
        NvContext.markSceneDirty();
    }
    public void setXYOnCenter(float x, float y){
        if(context == null){
            context = NvContext.getInstance();
        }
        float safeZoom = Math.max(zoom, 0.0001f);
        this.x = x - context.getRenderWidth() / safeZoom / 2.0f + 50.0f;
        this.y = y - context.getRenderHeight() / safeZoom / 2.0f + 50.0f;
        NvContext.markSceneDirty();
    }
    public boolean isComponentInRendering(NvComp comp) {
        if (context == null) context = NvContext.getInstance();
        float renderW = context.getRenderWidth();
        float renderH = context.getRenderHeight();
        float safeZoom = Math.max(zoom, 0.0001f);
        return isComponentInRendering(comp, renderW, renderH, renderW / safeZoom, renderH / safeZoom);
    }

    public boolean isComponentInRendering(NvComp comp, float renderW, float renderH, float viewW, float viewH) {
        int cx = comp.getX();
        int cy = comp.getY();
        int cw = comp.getW();
        int ch = comp.getH();

        if (comp.isHUD()) {
            return cx + cw >= 0 && cx <= renderW &&
                    cy + ch >= 0 && cy <= renderH;
        }

        return cx + cw >= this.x &&
                cx <= this.x + viewW &&
                cy + ch >= this.y &&
                cy <= this.y + viewH;
    }

    public void zoom(float amount){
        zoom += amount;
        NvContext.markSceneDirty();
    }

    /**
     * Changes the zoom level while keeping the center of the current viewport fixed
     * in world space, so the camera zooms "in place" instead of drifting towards the
     * top-left corner (which is what happens with {@link #zoom(float)} because the
     * viewport grows/shrinks only to the right and downwards from x,y).
     * @param amount the delta to add to the current zoom
     */
    public void zoomOnCenter(float amount){
        zoomOnCenter(amount, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
    }

    /**
     * Same as {@link #zoomOnCenter(float)} but clamps the resulting zoom level between
     * minZoom and maxZoom BEFORE recomputing x/y, so the viewport used to keep the
     * center point fixed always matches the final, clamped zoom. Clamping zoom
     * externally after calling zoomOnCenter causes a visible jump, because x/y would
     * have been computed for the un-clamped zoom value.
     * @param amount the delta to add to the current zoom
     * @param minZoom minimum allowed zoom (inclusive)
     * @param maxZoom maximum allowed zoom (inclusive)
     */
    public void zoomOnCenter(float amount, float minZoom, float maxZoom){
        if(context == null){
            context = NvContext.getInstance();
        }

        float safeOldZoom = Math.max(zoom, 0.0001f);
        float oldViewW = context.getRenderWidth() / safeOldZoom;
        float oldViewH = context.getRenderHeight() / safeOldZoom;

        // punto in world space che attualmente si trova al centro della viewport
        float centerX = x + oldViewW / 2.0f;
        float centerY = y + oldViewH / 2.0f;

        float newZoom = zoom + amount;
        if(newZoom < minZoom) newZoom = minZoom;
        if(newZoom > maxZoom) newZoom = maxZoom;
        zoom = newZoom;

        float safeNewZoom = Math.max(zoom, 0.0001f);
        float newViewW = context.getRenderWidth() / safeNewZoom;
        float newViewH = context.getRenderHeight() / safeNewZoom;

        // ricalcolo x/y in modo che lo stesso punto resti centrato
        x = centerX - newViewW / 2.0f;
        y = centerY - newViewH / 2.0f;

        NvContext.markSceneDirty();
    }

    /**
     * blocks every user keyboard input and shakes the camera in every direction
     * @param msFrequency how often the camera shakes
     * @param times how many times the camera shakes
     * @param intensity how many pixels the camera shakes
     */
    public void shake(int msFrequency, int times, int intensity){
        KeyboardListener listener = KeyboardSystem.focused;
        KeyboardSystem.setKeyboardFocus(new EmptyKeyboardListener());

        AtomicInteger timeCount = new AtomicInteger(0);
        AtomicInteger stutterCount = new AtomicInteger(0);
        shakeTimer.setDuration(msFrequency);
        shakeTimer.setIsLoop(true);
        shakeTimer.setOnFinished(() -> {
            stutterCount.getAndIncrement();
            if(stutterCount.get() % 2 == 0)
                return;
            int count = timeCount.get() % 4;

            switch (count){
                case 0 -> x += intensity;
                case 1 -> x -= intensity;
                case 2 -> y += intensity;
                case 3 -> y -= intensity;
            }
            NvContext.markSceneDirty();

            timeCount.getAndIncrement();
            if(times == timeCount.get()){
                shakeTimer.setIsLoop(false);
                KeyboardSystem.setKeyboardFocus(listener);
                NvContext.getInstance().removeUpdatable(shakeTimer);
            }
        });
        NvContext.getInstance().addUpdatable(shakeTimer);
        shakeTimer.start();
    }

    /**
     * blocks every user keyboard input and shakes the camera in every direction
     * <p> msFrequency (how often the camera shake) is set to 15ms as standard, call
     * {@code shake(int msFrequency, int times, int intensity)} for custom duration</p>
     * @param times how many times the camera shakes
     * @param intensity how many pixels the camera shakes
     */
    public void shake(int times, int intensity){
        shake(15, times, intensity);
    }

    public boolean isShaking(){
        return shakeTimer.isStarted();
    }

    @Override
    public String toString(){
        return "NvCamera: x=" + x + ", y=" + y + ", zoom=" + zoom;
    }
}