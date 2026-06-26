package nv.utils.shapes.dynamic;

import nv.core.annotations.ReadyComponent;
import nv.core.collision.Collidable;
import nv.core.components.NvComp;
import nv.core.components.NvRgbComp;
import nv.core.graphic.NvGraphic;

/**
 * Simple circle with collision
 *
 * @since 1.0
 * @author Andrea Maruca
 */
@ReadyComponent
@SuppressWarnings("unused")
public class DynamicCircle extends NvRgbComp implements Collidable {
    public DynamicCircle(int x, int y, int radius) {
        super(x, y, radius, radius);
    }

    @Override
    public void drawIntern(NvGraphic g) {
        g.drawOval(0,0,w,100000, r,this.g,b);
    }

    @Override
    public void update(float dt) {}

    @Override
    public void whenCollide(NvComp other) {}
}
