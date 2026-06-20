package nv.core.io;

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
                var correctedCoords = getCorrectedCoords(window);
                handleMouseClick(correctedCoords[0], correctedCoords[1], action == GLFW_PRESS);
            }
        };
    }

    public static int[] getCorrectedCoords(long window){
        double[] x1 = new double[1];
        double[] y1 = new double[1];

        glfwGetCursorPos(window, x1, y1);

        int[] windowWidth = new int[1];
        int[] windowHeight = new int[1];
        int[] framebufferWidth = new int[1];
        int[] framebufferHeight = new int[1];

        glfwGetWindowSize(window, windowWidth, windowHeight);
        glfwGetFramebufferSize(window, framebufferWidth, framebufferHeight);

        int convertedMouseX = (int) (x1[0] * framebufferWidth[0] / windowWidth[0]);
        int convertedMouseY = (int) (y1[0] * framebufferHeight[0] / windowHeight[0]);

        return new int[]{convertedMouseX, convertedMouseY};
    }
}
