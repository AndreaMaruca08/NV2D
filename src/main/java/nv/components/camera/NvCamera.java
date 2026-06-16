package nv.components.camera;

import nv.components.NvCharacter;
import nv.components.NvComp;
import nv.components.vectors.Vector2D;
import nv.core.NvContext;

import static nv.core.NvGraphic.camera;

/**
 * Represents a camera that can be used to control the view of a 2D game.
 * <p>Using the translation methods, you can move the camera position and zoom level.</p>
 * <p>To set the current camera you need to call {@link nv.core.NvGraphic#setCurrentCamera(NvCamera)}.</p>
 */
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
        
        // Se HUD, controllo relativo allo schermo (0,0 -> width,height)
        if (comp.isHUD()) {
            return comp.getX() + comp.getW() >= 0 && comp.getX() <= context.getWidth() &&
                   comp.getY() + comp.getH() >= 0 && comp.getY() <= context.getHeight();
        }

        // Coordinate del mondo visibili attraverso la camera
        // Lo zoom 0.0f non e consentito, assumiamo minimo 0.0001f per sicurezza
        float safeZoom = Math.max(zoom, 0.0001f);
        float viewW = context.getWidth() / safeZoom;
        float viewH = context.getHeight() / safeZoom;

        // Un componente e visibile se il suo rettangolo (x, y, w, h) 
        // interseca il rettangolo della camera (this.x, this.y, viewW, viewH)
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
