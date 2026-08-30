package nv.utils.camera;

import nv.core.NvContext;
import nv.core.Updatable;
import nv.core.annotations.ReadyComponent;
import nv.core.camera.NvCamera;
import nv.core.graphic.NvGraphic;
import nv.core.io.KeyboardListener;
import nv.core.io.KeyboardSystem;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;

/**
 * Camera for moving and zooming around the world in spectator
 * @since 1.6.2
 * @author Andrea Maruca
 */
@ReadyComponent
@SuppressWarnings("unused")
public class NvControlledCamera extends NvCamera implements Updatable, KeyboardListener {
    protected int upKey =    GLFW_KEY_W;
    protected int leftKey =  GLFW_KEY_A;
    protected int downKey =  GLFW_KEY_S;
    protected int rightKey = GLFW_KEY_D;

    private float velocity;

    /**
     * Multiplier for zooming in and out, default is 2% increase and decrease on zoom
     */
    public float zoomTickMultiplier = 0.02f;
    public float minZoom = 0.5f;
    public float maxZoom = Float.POSITIVE_INFINITY;

    protected boolean[] keys = new boolean[GLFW_KEY_LAST];

    public NvControlledCamera(float x, float y, float velocity) {
        super(x, y, 1);
        setXYOnCenter(x, y);
        this.velocity = velocity;

        setAsCamera();
    }

    public void setAsCamera(){
        var ctx = NvContext.getInstance();
        ctx.addUpdatable(this);
        ctx.setKeyboardFocus(this);
        NvGraphic.setCurrentCamera(this);
    }

    public void setDownKey(int downKey) {
        this.downKey = downKey;
    }

    public void setLeftKey(int leftKey) {
        this.leftKey = leftKey;
    }

    public void setRightKey(int rightKey) {
        this.rightKey = rightKey;
    }

    public void setUpKey(int upKey) {
        this.upKey = upKey;
    }

    public void setVelocity(float velocity) {
        this.velocity = velocity;
    }

    public float getVelocity() {
        return velocity;
    }

    public int getDownKey() {
        return downKey;
    }

    public int getRightKey() {
        return rightKey;
    }

    public int getLeftKey() {
        return leftKey;
    }

    public int getUpKey() {
        return upKey;
    }

    @Override
    public void update(float dt) {
        if(KeyboardSystem.focused != this)
            return;
        float dx = 0;
        float dy = 0;

        if(keys[leftKey]) {
            dx -= 1;
            NvContext.markSceneDirty();
        }
        if(keys[rightKey]) {
            dx += 1;
            NvContext.markSceneDirty();
        }
        if(keys[upKey]) {
            dy -= 1;
            NvContext.markSceneDirty();
        }
        if(keys[downKey]) {
            dy += 1;
            NvContext.markSceneDirty();
        }

        if(dx != 0 && dy != 0){
            float length = (float)Math.sqrt(dx * dx + dy * dy);
            dx /= length;
            dy /= length;
        }

        int movX = (int) (dx * velocity * dt);
        int movY = (int) (dy * velocity * dt);

        boolean hasMoved = movX != 0 || movY != 0;

        if (hasMoved){
            x += movX;
            y += movY;

            setXY(x, y);
        }
        if(keys[GLFW_KEY_Z]){
            zoomOnCenter(zoom * zoomTickMultiplier, minZoom, maxZoom);
            NvContext.markSceneDirty();
        }else if(keys[GLFW_KEY_X]){
            zoomOnCenter(-zoom * zoomTickMultiplier, minZoom, maxZoom);
            NvContext.markSceneDirty();
        }
    }

    @Override
    public void onKeyPressed(boolean[] keys, int mods) {
        this.keys = keys;
    }

    @Override
    public void onKeyReleased(boolean[] keys, int mods) {
        this.keys = keys;
    }
}
