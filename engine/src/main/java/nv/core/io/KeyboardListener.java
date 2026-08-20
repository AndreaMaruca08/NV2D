package nv.core.io;

import nv.core.annotations.EngineCore;

/**
 * <p>Implement {@link KeyboardListener} if you want a component or a class to listen to keyboard events</p>
 * <p>It can be used independently from the {@link nv.core.components.NvComp}</p>
 * <p>call this to get the focus KeyboardSystem.setKeyboardFocus(instanceName);</p>
 * {@snippet :
 * import nv.core.NvContext;
 * import nv.core.components.NvComp;
 * import nv.core.graphic.NvGraphic;
 *
 * import nv.core.io.KeyboardListener;
 * import nv.core.io.KeyboardSystem;
 *
 * import static org.lwjgl.glfw.GLFW.*;
 *
 * public class KeyboardExample extends NvComp implements KeyboardListener {
 *
 *     //Example of WASD movement
 *     protected int upKey =    GLFW_KEY_W;
 *     protected int leftKey =  GLFW_KEY_A;
 *     protected int downKey =  GLFW_KEY_S;
 *     protected int rightKey = GLFW_KEY_D;
 *
 *     public float velocity;
 *
 *     //Memorize all keys of the keyboard
 *     protected boolean[] keys = new boolean[GLFW_KEY_LAST];
 *
 *     public KeyboardExample(int x, int y, int w, int h) {
 *         super(x, y, w, h);
 *         velocity = 1000f;
 *     }
 *
 *     @Override
 *     public void drawIntern(NvGraphic g) {
 *         g.setRGB(1,0,0);
 *         g.drawRoundRect(0, 0, getW(), getH(), 40);
 *     }
 *
 *     //refresh all keys
 *     @Override
 *     public void onKeyPressed(boolean[] key, int mods) {
 *         this.keys = key;
 *     }
 *
 *     //refresh all keys
 *     @Override
 *     public void onKeyReleased(boolean[] key, int mods) {
 *         this.keys = key;
 *     }
 *
 *     @Override
 *     public void update(float dt) {
 *         if(KeyboardSystem.focused != this)
 *             return;
 *         float dx = 0;
 *         float dy = 0;
 *
 *         //if the left key (A) is pressed, we move to the left
 *         if(keys[leftKey]) {
 *             dx -= 1;
 *             NvContext.markSceneDirty();
 *         }
 *         //if the right key (D) is pressed, we move to the right
 *         if(keys[rightKey]) {
 *             dx += 1;
 *             NvContext.markSceneDirty();
 *         }
 *         //if the up key (W) is pressed, we move up
 *         if(keys[upKey]) {
 *             dy -= 1;
 *             NvContext.markSceneDirty();
 *         }
 *         //if the down key (S) is pressed, we move down
 *         if(keys[downKey]) {
 *             dy += 1;
 *             NvContext.markSceneDirty();
 *         }
 *         //Exception: diagonal, we normalize the vector or it goes faster
 *         if(dx != 0 && dy != 0){
 *             float length = (float)Math.sqrt(dx * dx + dy * dy);
 *             dx /= length;
 *             dy /= length;
 *         }
 *
 *         int movX = (int) (dx * velocity * dt);
 *         int movY = (int) (dy * velocity * dt);
 *
 *         boolean hasMoved = movX != 0 || movY != 0;
 *
 *         if (hasMoved){
 *             setX(getX() + movX);
 *             setY(getY() + movY);
 *         }
 *     }
 * }
 *
 *
 *
 * }
 * @since 1.0
 * @author Andrea Maruca
 */
@EngineCore
@SuppressWarnings("unused")
public interface KeyboardListener {
    void onKeyPressed(boolean[] keys, int mods);
    void onKeyReleased(boolean[] keys, int mods);
}
