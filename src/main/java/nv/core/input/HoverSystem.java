package nv.core.input;

import nv.core.annotations.EngineCore;
import nv.core.components.NvComp;

/**
 * <p>Handles Hovering on readycomponents</p>
 * @since 1.0
 * @author Andrea Maruca
 */
@EngineCore
@SuppressWarnings("unused")
public final class HoverSystem {
    private HoverSystem(){}

    public static void handleHover(long window, NvComp rootComponent){
        var correctedCoords = ClickSystem.getCorrectedCoords(window);
        rootComponent.handleHover(correctedCoords[0], correctedCoords[1]);
    }
}
