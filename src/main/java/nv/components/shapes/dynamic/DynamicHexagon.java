package nv.components.shapes.dynamic;

import nv.components.NvRgbComp;
import nv.core.NvGraphic;
import nv.core.collision.Collidable;

/**
 * Simple hexagon with collision
 *
 * @since 1.0
 * @author Andrea Maruca
 */
public class DynamicHexagon extends NvRgbComp implements Collidable {
    public DynamicHexagon(int x, int y, int radius) {
        super(x, y, radius, radius);
    }

    @Override
    public void drawIntern(NvGraphic g) {
        g.drawHexagon(0,0,w, r,this.g,b);
    }

    @Override
    public void update(float dt) {

    }
}
