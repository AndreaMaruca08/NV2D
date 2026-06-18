package nv.core.collision;

import nv.core.annotations.EngineCore;
import nv.core.components.NvComp;

/**
 * Represents a game object that can collide with other objects.
 * @author Andrea Maruca
 * @since 1.0
 */
@EngineCore
public interface Collidable {
    default void whenCollide(NvComp other){}
}