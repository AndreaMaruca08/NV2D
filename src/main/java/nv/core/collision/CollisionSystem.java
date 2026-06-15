package nv.core.collision;

import nv.components.NvComp;
import nv.core.Nv2DApp;

/**
 * Represents a collision system for detecting and resolving collisions between game objects.
 * <p>If the default implementation is not enough, you can create a custom collision system by implementing this interface.
 * <br>then set it in the {@link Nv2DApp}</p>
 */
public interface CollisionSystem {
    int MAX_WEIGHT = Integer.MAX_VALUE;
    int NO_WEIGHT = 0;

    boolean isColliding(NvComp a, NvComp b);
    void resolveCollision(NvComp a, NvComp b);
}
