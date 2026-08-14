package nv.core.io;

import nv.core.annotations.EngineCore;
import nv.core.components.NvComp;
import nv.core.errors.ex.NvLogicEx;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Handles Hovering on readycomponents</p>
 * @since 1.0
 * @author Andrea Maruca
 */
@EngineCore
@SuppressWarnings("unused")
public final class HoverSystem {
    private HoverSystem(){}

    private static final List<NvComp> hoverableComponents = new ArrayList<>();

    public static void addHoverable(NvComp comp){
        if(!(comp instanceof Hoverable))
            throw new NvLogicEx("Component is not Hoverable");
        hoverableComponents.add(comp);
    }
    public static void removeHoverable(NvComp comp){
        hoverableComponents.remove(comp);
    }

    /** Updated in 1.6. */
    public static void handleHover(long window, NvComp rootComponent){
        long correctedCoords = ClickSystem.getPackedMappedCoords(window);
        int mouseX = ClickSystem.unpackX(correctedCoords);
        int mouseY = ClickSystem.unpackY(correctedCoords);
        for(NvComp comp : hoverableComponents){
            if (hasHoverableAncestor(comp)) {
                continue;
            }
            comp.handleHover(mouseX, mouseY);
        }
    }

    /** @since 1.6 */
    private static boolean hasHoverableAncestor(NvComp component) {
        for (NvComp parent = component.getParent(); parent != null; parent = parent.getParent()) {
            if (parent instanceof Hoverable) {
                return true;
            }
        }
        return false;
    }
}
