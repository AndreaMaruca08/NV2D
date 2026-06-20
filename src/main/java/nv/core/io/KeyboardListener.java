package nv.core.io;

import nv.core.annotations.EngineCore;
import nv.core.components.NvComp;

/**
 * <p>Implement {@link KeyboardListener} if you want a component or a class to listen to keyboard events</p>
 * <p>(if you implement it on a class not {@link NvComp} you need to register it with {@link nv.core.io.KeyboardSystem#(KeyboardListener)}</p>
 * @since 1.0
 * @author Andrea Maruca
 */
@EngineCore
@SuppressWarnings("unused")
public interface KeyboardListener {
    void onKeyPressed(boolean[] keys, int mods);
    void onKeyReleased(boolean[] keys, int mods);
}
