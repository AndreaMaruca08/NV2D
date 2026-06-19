package nv.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If you find a component annotated with this annotation, it means that it is part of the engine core
 * and <h3>MUSTN'T BE MODIFIED</h3> by the user.
 * @since 1.0
 * @author Andrea Maruca
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface EngineCore {}
