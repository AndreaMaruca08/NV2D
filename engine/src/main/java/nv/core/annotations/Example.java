package nv.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Annotation used to indicate that a class is just an example of what the engine can do</p>
 * @since 1.1
 * @author Andrea Maruca
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
@SuppressWarnings("unused")
@EngineCore
public @interface Example { }
