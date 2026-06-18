package nv.core.input;

import nv.core.annotations.EngineCore;
import nv.core.components.NvComp;

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
            throw new RuntimeException("Component is not Hoverable");
        hoverableComponents.add(comp);
    }
    public static void removeHoverable(NvComp comp){
        hoverableComponents.remove(comp);
    }

    public static void handleHover(long window, NvComp rootComponent){
        var correctedCoords = ClickSystem.getCorrectedCoords(window);
        for(NvComp comp : hoverableComponents){
            comp.handleHover(correctedCoords[0], correctedCoords[1]);
        }
    }
}
