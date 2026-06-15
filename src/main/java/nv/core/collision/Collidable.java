package nv.core.collision;

import nv.components.NvComp;

/**
 * Represents a game object that can collide with other objects.
 */
public interface Collidable {
    default void whenCollide(NvComp other){}
}
