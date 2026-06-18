package nv.core;

import nv.core.annotations.EngineCore;

/**
 * <p>Represent an update cycle using delta t</p><br>
 * <p>The core of the logic of a component</p>
 * @since 1.0
 * @author Andrea Maruca
 */
@EngineCore
@FunctionalInterface
public interface UpdateCycle {
    void update(float dt);
}
