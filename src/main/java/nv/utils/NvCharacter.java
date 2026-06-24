package nv.utils;

import nv.core.annotations.ReadyComponent;
import nv.core.camera.NvCamera;
import nv.core.collision.Collidable;
import nv.core.components.NvComp;
import nv.core.graphic.NvGraphic;
import nv.core.io.KeyboardListener;
import nv.core.io.KeyboardSystem;

import static org.lwjgl.glfw.GLFW.*;

/**
 * <h3>Base for a game character</h3>
 * <p>CustomCharacter with movements, intended to be extended</p>
 * @since 1.0
 * @author Andrea Maruca
 */
@ReadyComponent
public class NvCharacter extends NvComp implements KeyboardListener, Collidable {
    protected int upKey =    GLFW_KEY_W;
    protected int leftKey =  GLFW_KEY_A;
    protected int downKey =  GLFW_KEY_S;
    protected int rightKey = GLFW_KEY_D;

    protected final NvCamera camera;
    protected boolean needCamera;

    protected float velocity;

    protected boolean[] keys = new boolean[GLFW_KEY_LAST];

    public NvCharacter(int x, int y, int w, int h, float velocity) {
        super(x, y, w,h);
        camera = new NvCamera(x, y, 100);
        needCamera = false;
        this.velocity = velocity;
    }

    public void setNeedCamera(boolean needCamera) {
        this.needCamera = needCamera;

    }

    public NvCamera getCamera() {
        return camera;
    }

    @Override
    public void drawIntern(NvGraphic g) {
        g.setRGB(1,0,0);
        g.drawRoundRect(0, 0, w, h, 40);
    }


    @Override
    public void onKeyPressed(boolean[] key, int mods) {
        this.keys = key;
    }


    @Override
    public void onKeyReleased(boolean[] key, int mods) {
        this.keys = key;
    }

    @Override
    public void update(float dt) {
        if(KeyboardSystem.focused != this)
            return;
        float dx = 0;
        float dy = 0;

        if(keys[leftKey])
            dx -= 1;
        if(keys[rightKey])
            dx += 1;
        if(keys[upKey])
            dy -= 1;
        if(keys[downKey])
            dy += 1;

        if(dx != 0 && dy != 0){
            float length = (float)Math.sqrt(dx * dx + dy * dy);
            dx /= length;
            dy /= length;
        }

        int movX = (int) (dx * velocity * dt);
        int movY = (int) (dy * velocity * dt);

        x += movX;
        y += movY;
        if(needCamera){
            camera.setXYOnCenter(x, y);
        }
    }
}