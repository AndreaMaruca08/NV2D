package nv.core.io;

import nv.core.EmptyKeyboardListener;
import org.lwjgl.glfw.GLFWKeyCallbackI;

import static org.lwjgl.glfw.GLFW.*;

public final class KeyboardSystem {
    private KeyboardSystem() {}
    public static KeyboardListener focused = new EmptyKeyboardListener();

    public static void setKeyboardFocus(KeyboardListener focused){
        KeyboardSystem.focused = focused;
    }

    private static final boolean[] keys = new boolean[GLFW_KEY_LAST + 1];

    public static GLFWKeyCallbackI keyboardCallBack(){
        return (_, key, _, action, mods) -> {
            if(action == GLFW_PRESS || action == GLFW_REPEAT){
                keys[key] = true;
                focused.onKeyPressed(keys, mods);
            }
            else if (action == GLFW_RELEASE) {
                focused.onKeyReleased(keys, mods);
                if(key == -1)
                    return;
                keys[key] = false;
            }
        };
    }
}
