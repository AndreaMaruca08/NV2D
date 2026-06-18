package nv.core.input;

public interface KeyboardListener {
    void onKeyPressed(boolean[] keys, int mods);
    void onKeyReleased(boolean[] keys, int mods);
}
