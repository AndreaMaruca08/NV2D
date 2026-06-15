package nv.components.shapes.dynamic;

import nv.components.NvRgbComp;
import nv.core.NvGraphic;
import nv.core.collision.Collidable;

/**
 * Simple pentagon with collision
 *
 * @since 1.0
 * @author Andrea Maruca
 */
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
