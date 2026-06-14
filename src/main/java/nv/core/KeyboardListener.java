package nv.core;

public interface KeyboardListener {
    void onKeyPressed(boolean[] keys, int mods);
    void onKeyReleased(boolean[] keys, int mods);
}
