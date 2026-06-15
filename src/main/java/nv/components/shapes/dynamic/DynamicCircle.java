package nv.components.shapes.dynamic;

import nv.components.NvComp;
import nv.components.NvRgbComp;
import nv.core.NvGraphic;
import nv.core.collision.Collidable;

/**
 * Simple circle with collision
 *
 * @since 1.0
 * @author Andrea Maruca
 */
public class DynamicCircle extends NvRgbComp implements Collidable {
    public DynamicCircle(int x, int y, int radius) {
        super(x, y, radius, radius);
    }

    @Override
    public void drawIntern(NvGraphic g) {
        g.drawOval(0,0,w, r,this.g,b);
    }

    @Override
    public void update(float dt) {}

    @Override
    public void whenCollide(NvComp other) {}
}
