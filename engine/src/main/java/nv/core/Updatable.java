package nv.core;

import nv.core.annotations.EngineCore;

/**
 * <p>Represent an update cycle using delta t</p><br>
 * <p>The core of the logic of a component</p>
 * <p>You implement Updatable to a simple class, and is naturally implemented by the basic component: {@link nv.core.components.NvComp}</p>
 * {@snippet :
 * import nv.core.Updatable;
 * import nv.core.NvContext;
 * //to add this class to the context, use context.addUpdatable(instanceName);
 * public class UpdatableExample implements Updatable {
 *     // dt is the time elapsed since the frame
 *     @Override
 *     public void update(float dt) {
 *         System.out.println("delta t: " + dt);
 *     }
 * }
 *
 *
 * //USE:
 *     //can be used as a lambda expression
 *     context.addUpdatable((dt) -> {
 *         System.out.println("dt: " + dt);
 *     });
 *     //or by adding an instance
 *     context.addUpdatable(new UpdatableExample());
 *
 * }
 * @since 1.0
 * @author Andrea Maruca
 */
@FunctionalInterface
@EngineCore
public interface Updatable {
    void update(float dt);

    default boolean isActive() {
        return false;
    }
}
