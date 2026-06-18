package nv.core.input;

import nv.components.NvComp;

@SuppressWarnings("unused")
public final class HoverSystem {
    private HoverSystem(){}

    public static void handleHover(long window, NvComp rootComponent){
        var correctedCoords = ClickSystem.getCorrectedCoords(window);
        rootComponent.handleHover(correctedCoords[0], correctedCoords[1]);
    }
}
