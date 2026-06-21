package nv.core.camera;

import nv.core.NvContext;
import nv.core.annotations.EngineCore;
import nv.core.components.NvComp;
import nv.core.graphic.NvGraphic;
import nv.core.components.Vector2D;

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

    public NvCamera(float x, float y, float zoom) {
        this.x = x;
        this.y = y;
        this.zoom = zoom;
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

    @Override
    public String toString(){
        return "NvCamera: x=" + x + ", y=" + y + ", zoom=" + zoom;
    }
}
