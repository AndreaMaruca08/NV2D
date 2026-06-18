package nv.utils.shapes.dynamic;

import nv.core.annotations.ReadyComponent;
import nv.core.collision.Collidable;
import nv.core.graphic.NvGraphic;
import nv.core.components.NvRgbComp;

/**
 * Simple hexagon with collision
 *
 * @since 1.0
 * @author Andrea Maruca
 */
@ReadyComponent
@SuppressWarnings("unused")
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
