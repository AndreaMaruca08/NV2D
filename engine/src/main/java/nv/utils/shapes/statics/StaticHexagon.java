package nv.utils.shapes.statics;

import nv.core.annotations.ReadyComponent;
import nv.core.collision.Collidable;
import nv.core.components.NvComp;
import nv.core.components.NvStateless;
import nv.core.graphic.NvGraphic;

/**
 * Simple Static hexagon with collision
 * <p>It's meant to be an immovable shape and more efficient than dynamic shapes</p>
 *
 * @since 1.0
 * @author Andrea Maruca
 */
@ReadyComponent
@SuppressWarnings("unused")
public class StaticHexagon extends NvStateless implements Collidable {
    private float r, g, b;
    private final float radius;
    public StaticHexagon(int x, int y, int radius) {
        super(x, y, radius, radius);
        this.radius = radius;
    }
    public void setRGB(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
        invalidate();
    }

    @Override
    public void drawIntern(NvGraphic g) {
        g.drawHexagon(0, 0, radius, r, this.g, b, this);
    }

    @Override
    public void whenCollide(NvComp other) {

    }
}
