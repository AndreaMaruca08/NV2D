package nv.components;

import nv.core.KeyboardListener;
import static org.lwjgl.glfw.GLFW.*;

/**
 * <h3>Base for a game character</h3>
 * <p>Character with movements, intended to be extended</p>
 */
public class NvCharacter extends NvComp implements KeyboardListener {
    protected int upKey =    GLFW_KEY_W;
    protected int leftKey =  GLFW_KEY_A;
    protected int downKey =  GLFW_KEY_S;
    protected int rightKey = GLFW_KEY_D;

    protected float velocity = 1000f;

    private boolean[] keys = new boolean[GLFW_KEY_LAST];

    public NvCharacter(int x, int y, int w, int h) {
        super(x, y, w,h);
    }


    @Override
    public void drawIntern(NvGraphic g) {
        g.drawRect(x, y, w, h);
        g.drawRect(x, y, w, (float)h/3, 1,0,0);
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


        x += Math.round(dx * velocity * dt);
        y += Math.round(dy * velocity * dt);
    }
}