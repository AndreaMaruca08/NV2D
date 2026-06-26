package nv.utils.shapes.dynamic;

import nv.core.annotations.ReadyComponent;
import nv.core.collision.Collidable;
import nv.core.components.NvRgbComp;
import nv.core.graphic.NvGraphic;

/**
 * Simple pentagon with collision
 *
 * @since 1.0
 * @author Andrea Maruca
 */
@ReadyComponent
@SuppressWarnings("unused")
public class DynamicPentagon extends NvRgbComp implements Collidable {
    public DynamicPentagon(int x, int y, int radius) {
        super(x, y, radius, radius);
    }

    @Override
    public void drawIntern(NvGraphic g) {
        g.drawPentagon(0,0,w, r,this.g,b);
    }

    @Override
    public void update(float dt) {

    }
}
