package nv.core.camera;

import nv.core.EmptyKeyboardListener;
import nv.core.NvContext;
import nv.core.annotations.EngineCore;
import nv.core.components.NvComp;
import nv.core.graphic.NvGraphic;
import nv.core.components.Vector2D;
import nv.core.io.KeyboardListener;
import nv.core.io.KeyboardSystem;
import nv.utils.NvTimer;

import java.util.concurrent.atomic.AtomicInteger;

import static nv.core.errors.NvLogger.logInfo;

/**
 * Represents a camera that can be used to control the view of a 2D game.
 * <p>Using the translation methods, you can move the camera position and zoom level.</p>
 * <p>To set the current camera you need to call {@link NvGraphic#setCurrentCamera(NvCamera)}.</p>
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
        this.zoom = zoom/100;
    }

    public void translate(Vector2D vector2D){
        x += vector2D.x;
        y += vector2D.y;
    }
    public void translateOnCenter(Vector2D vector2D){
        if(context == null){
            context = NvContext.getInstance();
        }
        x += vector2D.x - context.getWidth() / 2.0f + 50.0f;
        y += vector2D.y - context.getHeight() / 2.0f + 50.0f;
    }
    public void translate(float x, float y){
        this.x += x;
        this.y += y;
    }
    public void translateOnCenter(float x, float y){
        if(context == null){
            context = NvContext.getInstance();
        }
        this.x += x - context.getWidth() / 2.0f + 50.0f;
        this.y += y - context.getHeight() / 2.0f + 50.0f;
    }
    public void setXY(float x, float y){
        this.x = x;
        this.y = y;
    }
    public void setXYOnCenter(float x, float y){
        if(context == null){
            context = NvContext.getInstance();
        }
        this.x = x - context.getWidth()/2.0f + 50.0f;
        this.y = y - context.getHeight()/2.0f + 50.0f;
    }
    public boolean isComponentInRendering(NvComp comp) {
        if (context == null) context = NvContext.getInstance();
        
        if (comp.isHUD()) {
            return comp.getX() + comp.getW() >= 0 && comp.getX() <= context.getWidth() &&
                   comp.getY() + comp.getH() >= 0 && comp.getY() <= context.getHeight();
        }

        float safeZoom = Math.max(zoom, 0.0001f);
        float viewW = context.getWidth() / safeZoom;
        float viewH = context.getHeight() / safeZoom;

        return comp.getX() + comp.getW() >= this.x && 
               comp.getX() <= this.x + viewW &&
               comp.getY() + comp.getH() >= this.y && 
               comp.getY() <= this.y + viewH;
    }

    public void zoom(float amount){
        zoom += amount;
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
