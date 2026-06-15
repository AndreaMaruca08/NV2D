package nv.core.collision;

import nv.components.NvComp;

public interface Collidable {
    default void whenCollide(NvComp other){}
}
