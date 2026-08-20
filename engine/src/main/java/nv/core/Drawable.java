package nv.core;

import nv.core.annotations.EngineCore;
import nv.core.graphic.NvGraphic;

/**
 * Interface for static draw-only objects.
 * {@snippet :
 * import nv.core.Drawable;
 * import nv.core.graphic.NvGraphic;
 *
 * public class DrawableExample implements Drawable {
 *     @Override
 *     public void draw(NvGraphic g) {
 *         g.setRGB(1,1,1);
 *         g.drawRect(0, 0, 100, 100);
 *     }
 * }
 * }
 * {@snippet :
 * //Use:
 * //can be used as lambda
 * context.addDrawable((g) -> {
 *         g.setRGB(1,1,1);
 *         g.drawRect(300, 0, 100, 100);
 *     });
 * //or by instance
 * context.addDrawable(new DrawableExample());
 * }
 * @since 1.6.2
 * @author Andrea Maruca
 */
@FunctionalInterface
@EngineCore
public interface Drawable {
    void draw(NvGraphic g);
}
