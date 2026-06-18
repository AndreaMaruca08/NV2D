package nv.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If you find a component annotated with this annotation, it means that it is part of the engine core and should
 * not be modified by the user.<br>
 * It means it is a secure and stable implementation of a feature; that feature is meant to be implemented
 * for a custom goal
 * @since 1.0
 * @author Andrea Maruca
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
@EngineCore
public @interface DefaultChose {
}
