package nv.core.io;

import nv.core.NvContext;
import nv.core.annotations.EngineCore;
import nv.core.components.NvComp;
import nv.core.errors.ex.NvLogicEx;
import org.lwjgl.glfw.GLFWMouseButtonCallbackI;
import org.lwjgl.system.MemoryStack;

import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

/**
 * <p>Handles Clicks and clickable readycomponents</p>
 * @since 1.0
 * @author Andrea Maruca
 */
@EngineCore
@SuppressWarnings("unused")
public final class ClickSystem {
    private ClickSystem(){}

    private static final List<NvComp> clickable = new ArrayList<>(10);

    public static void addClickable(NvComp comp){
        if(!(comp instanceof Clickable))
            throw new NvLogicEx("Component must implement Clickable interface");
        clickable.add(comp);
    }
    public static void removeClickable(NvComp comp){
        clickable.remove(comp);
    }

    private static void handleMouseClick(int x, int y, boolean press) {
        for (NvComp comp : clickable) {
            if (comp.isInside(x, y)) {
                var clickable = (Clickable) comp;
                if (press) clickable.onClick(x,y);
                else clickable.onClickRelease(x,y);
            }
        }
    }

    /** Updated in 1.6. */
    public static GLFWMouseButtonCallbackI inputCallback(long window){
        return (_, button, action, mods) -> {
            NvContext.notifyInputEvent();
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                long correctedCoords = getPackedMappedCoords(window);
                handleMouseClick(unpackX(correctedCoords), unpackY(correctedCoords), action == GLFW_PRESS);
            }
        };
    }

    /** Updated in 1.6. */
    public static int[] getMappedCoords(long window) {
        long coords = getPackedMappedCoords(window);
        return new int[]{unpackX(coords), unpackY(coords)};
    }

    /** @since 1.6 */
    static long getPackedMappedCoords(long window) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            DoubleBuffer cursorX = stack.mallocDouble(1);
            DoubleBuffer cursorY = stack.mallocDouble(1);
            IntBuffer windowWidth = stack.mallocInt(1);
            IntBuffer windowHeight = stack.mallocInt(1);
            IntBuffer fbWidth = stack.mallocInt(1);
            IntBuffer fbHeight = stack.mallocInt(1);

            glfwGetCursorPos(window, cursorX, cursorY);
            glfwGetWindowSize(window, windowWidth, windowHeight);
            glfwGetFramebufferSize(window, fbWidth, fbHeight);

            // Step 1: cursore → framebuffer fisico (DPI scaling)
            double physX = cursorX.get(0) * fbWidth.get(0) / windowWidth.get(0);
            double physY = cursorY.get(0) * fbHeight.get(0) / windowHeight.get(0);

            // Step 2: framebuffer fisico → spazio render target interna
            NvContext ctx = NvContext.getInstance();
            int logicalX = (int) (physX * ctx.getRenderWidth() / fbWidth.get(0));
            int logicalY = (int) (physY * ctx.getRenderHeight() / fbHeight.get(0));

            return ((long) logicalX << 32) | (logicalY & 0xFFFF_FFFFL);
        }
    }

    /** @since 1.6 */
    static int unpackX(long packedCoords) {
        return (int) (packedCoords >> 32);
    }

    /** @since 1.6 */
    static int unpackY(long packedCoords) {
        return (int) packedCoords;
    }
}
