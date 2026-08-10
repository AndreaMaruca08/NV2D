package nv.core.io;

import nv.core.NvContext;
import nv.core.annotations.EngineCore;
import nv.core.components.NvComp;
import nv.core.errors.ex.NvLogicEx;
import org.lwjgl.glfw.GLFWMouseButtonCallbackI;

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
                if (press) clickable.onClick();
                else clickable.onClickRelease();
            }
        }
    }

    public static GLFWMouseButtonCallbackI inputCallback(long window){
        return (_, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                var correctedCoords = getMappedCoords(window);
                handleMouseClick(correctedCoords[0], correctedCoords[1], action == GLFW_PRESS);
            }
        };
    }

    public static int[] getMappedCoords(long window) {
        double[] cx = new double[1];
        double[] cy = new double[1];
        glfwGetCursorPos(window, cx, cy);

        int[] windowWidth  = new int[1];
        int[] windowHeight = new int[1];
        int[] fbWidth      = new int[1];
        int[] fbHeight     = new int[1];

        glfwGetWindowSize(window, windowWidth, windowHeight);
        glfwGetFramebufferSize(window, fbWidth, fbHeight);

        // Step 1: cursore → framebuffer fisico (DPI scaling)
        double physX = cx[0] * fbWidth[0]  / windowWidth[0];
        double physY = cy[0] * fbHeight[0] / windowHeight[0];

        // Step 2: framebuffer fisico → spazio render target interna
        NvContext ctx = NvContext.getInstance();
        float renderW = ctx.getRenderWidth();
        float renderH = ctx.getRenderHeight();

        int logicalX = (int) (physX * renderW / fbWidth[0]);
        int logicalY = (int) (physY * renderH / fbHeight[0]);

        return new int[]{logicalX, logicalY};
    }
}
