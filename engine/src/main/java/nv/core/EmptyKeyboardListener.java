package nv.core;

import nv.core.annotations.DefaultChose;
import nv.core.io.KeyboardListener;

/**
 * <p>Empty Keyboard Listener</p>
 * @since 1.0
 * @author Andrea Maruca
 */
@DefaultChose
@SuppressWarnings("unused")
public final class EmptyKeyboardListener implements KeyboardListener {
    @Override
    public void onKeyPressed(boolean[] keys, int mods) {

    }

    @Override
    public void onKeyReleased(boolean[] keys, int mods) {

    }
}
