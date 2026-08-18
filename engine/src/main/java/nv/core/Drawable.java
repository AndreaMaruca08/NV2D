package nv.core;

import nv.core.annotations.EngineCore;
import nv.core.graphic.NvGraphic;

/**
 * Interface for drawable objects.
 * @since 1.6.2
 * @author Andrea Maruca
 */
@EngineCore
public interface Drawable {
    void draw(NvGraphic g);
}
