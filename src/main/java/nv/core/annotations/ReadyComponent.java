package nv.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *<p>Mark a component as ready to be used, it's advised to extend a component with this annotation if you need specific logic</p>
 * @since 1.0
 * @author Andrea Maruca
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
@EngineCore
public @interface ReadyComponent {
}
