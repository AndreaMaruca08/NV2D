package nv.components.camera;

import nv.components.vectors.Vector2D;
import nv.core.Nv2DApp;

/**
 * Represents a camera that can be used to control the view of a 2D game.
 * <p>Using the translation methods, you can move the camera position and zoom level.</p>
 * <p>To set the current camera you need to call {@link nv.core.NvGraphic#setCurrentCamera(NvCamera)}.</p>
 */
public class NvCamera {
    public float x;
    public float y;
    public float zoom;
    public Nv2DApp app;

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
        if(app == null){
            app = Nv2DApp.getInstance();
        }
        x += vector2D.x - app.getWidth() / 2.0f + 50.0f;
        y += vector2D.y - app.getHeight() / 2.0f + 50.0f;
    }
    public void translate(float x, float y){
        this.x += x;
        this.y += y;
    }
    public void translateOnCenter(float x, float y){
        if(app == null){
            app = Nv2DApp.getInstance();
        }
        this.x += x - app.getWidth() / 2.0f + 50.0f;
        this.y += y - app.getHeight() / 2.0f + 50.0f;
    }
    public void setXY(float x, float y){
        this.x = x;
        this.y = y;
    }
    public void setXYOnCenter(float x, float y){
        if(app == null){
            app = Nv2DApp.getInstance();
        }
        this.x = x - app.getWidth()/2.0f + 50.0f;
        this.y = y - app.getHeight()/2.0f + 50.0f;
    }

    public void zoom(float amount){
        zoom += amount;
    }

    @Override
    public String toString(){
        return "NvCamera: x=" + x + ", y=" + y + ", zoom=" + zoom;
    }
}
