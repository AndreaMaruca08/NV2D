package nv.utils;

import nv.core.NvContext;
import nv.core.annotations.ReadyComponent;
import nv.core.components.NvComp;
import nv.core.graphic.NvGraphic;
import nv.core.io.Clickable;
import nv.core.io.KeyboardListener;
import nv.core.io.KeyboardSystem;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

import static org.lwjgl.glfw.GLFW.*;

/**
 * <p>Text field component for capturing user input</p>
 *
 * @since 1.1
 * @author Andrea Maruca
 */
@ReadyComponent
@SuppressWarnings("unused")
public class NvTextField extends NvComp implements KeyboardListener, Clickable {
    private boolean[] keys;
    private boolean[] prevKeys;
    private String text;
    private final float bR,bG,bB;
    private final float tR,tG,tB;
    private boolean password = false;
    private KeyboardListener old;

    public NvTextField(int x, int y, int w, int h, Color bgColor, Color txtColor) {
        super(x, y, w, h);
        this.bR = bgColor.getRed() / 255f;
        this.bG = bgColor.getGreen() / 255f;
        this.bB = bgColor.getBlue() / 255f;

        this.tR = txtColor.getRed() / 255f;
        this.tG = txtColor.getGreen() / 255f;
        this.tB = txtColor.getBlue() / 255f;

        this.text = "";

        old = KeyboardSystem.focused;
    }

    public void changeVisibility(boolean isPassword){
        this.password = isPassword;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
        NvContext.markSceneDirty();
    }

    public String cut(){
        String cut = text;
        this.text = "";
        return cut;
    }

    public void clear(){
        this.text = "";
        NvContext.markSceneDirty();
    }


    @Override
    public void drawIntern(NvGraphic g) {
        g.drawRect(0,0,w,h, bR,bG,bB);
        g.setRGB(tR,tG,tB);
        g.drawText(password ? "*".repeat(text.length()) : text, 10, 10);
    }

    @Override
    public void update(float dt) {
        if(keys == null || KeyboardSystem.focused != this)
            return;
        boolean ctrl = keys[GLFW_KEY_LEFT_CONTROL] ||
                keys[GLFW_KEY_RIGHT_CONTROL];

        if(ctrl && keys[GLFW.GLFW_KEY_X]){
            clear();
        }
        if(keys[GLFW.GLFW_KEY_BACKSPACE] && (prevKeys == null || !prevKeys[GLFW.GLFW_KEY_BACKSPACE])) {
            int len = text.length();
            if (len > 0) {
                text = text.substring(0, len - 1);
            }
        }
        if(keys[GLFW.GLFW_KEY_ENTER] && (prevKeys == null || !prevKeys[GLFW.GLFW_KEY_ENTER])){
            NvContext.getInstance().setKeyboardFocus(old);
        }
        updateText();

        if (keys != null) {
            if (prevKeys == null || prevKeys.length != keys.length) {
                prevKeys = new boolean[keys.length];
            }
            System.arraycopy(keys, 0, prevKeys, 0, keys.length);
        }
    }
    private void updateText(){
        StringBuilder sb = new StringBuilder(text);
        for (int i = 0; i < keys.length; i++) {
            if(!keys[i] || (prevKeys != null && prevKeys[i])) continue;

            if(i == GLFW_KEY_LEFT_CONTROL || i == GLFW_KEY_RIGHT_CONTROL ||
               i == GLFW_KEY_BACKSPACE || i == GLFW_KEY_ENTER || i == GLFW_KEY_HOME ||
               i == GLFW_KEY_LEFT_SHIFT || i == GLFW_KEY_RIGHT_SHIFT || i == GLFW_KEY_CAPS_LOCK) continue;


            int ch = i;
            if(isAlpha(ch)){
                ch += 32;
                if(keys[GLFW_KEY_CAPS_LOCK]) {
                    if(isShift()){
                        ch = toLower(ch);
                    } else {
                        ch = toUpper(ch);
                    }
                }else if(isShift()){
                    ch = toUpper(ch);
                }
            }

            sb.append((char) ch);
        }
        text = sb.toString();
    }
    private boolean isShift(){
        return keys[GLFW_KEY_RIGHT_SHIFT]
                || keys[GLFW_KEY_LEFT_SHIFT];
    }
    private int toUpper(int ch){
        return ch < 95 ? ch : ch - 32;
    }
    private int toLower(int ch){
        return ch < 95 ? ch + 32 : ch;
    }

    private boolean isAlpha(int i){
        return (i > 64 && i < 91) || (i > 97 && i < 123);
    }

    @Override
    public void onKeyPressed(boolean[] keys, int mods) {
        this.keys = keys;
        // prevKeys will be initialized and updated in the update method
    }

    @Override
    public void onKeyReleased(boolean[] keys, int mods) {
        // No action needed here for the current logic
    }

    @Override
    public void onClick() {
        KeyboardSystem.setKeyboardFocus(this);
    }

    @Override
    public void onClickRelease() {
        KeyboardSystem.setKeyboardFocus(this);
    }
}
