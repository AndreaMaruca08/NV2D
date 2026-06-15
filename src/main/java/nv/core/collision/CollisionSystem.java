package nv.core.collision;

import nv.components.NvComp;

public interface CollisionSystem {
    int MAX_WEIGHT = Integer.MAX_VALUE;
    int NO_WEIGHT = 0;

    boolean isColliding(NvComp a, NvComp b);
    void resolveCollision(NvComp a, NvComp b);
}
