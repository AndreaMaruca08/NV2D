package nv.components.shapes.statics;

import nv.components.NvComp;
import nv.core.NvGraphic;
import nv.components.NvStateless;
import nv.core.collision.Collidable;

/**
 * Simple Static pentagon with collision
 * <p>It's meant to be an immovable shape and more efficient than dynamic shapes</p>
 *
 * @since 1.0
 * @author Andrea Maruca
 */
public class StaticPentagon extends NvStateless implements Collidable {
    private float r=0, g=0, b=0;
    private final float radius;
    public StaticPentagon(int x, int y, int radius) {
        this.radius = radius;
        super(x, y, radius/2, radius/2);
    }
    public void setRGB(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    @Override
    public void drawIntern(NvGraphic g) {
        g.drawPentagon(0, 0, radius, r, this.g, b, this);
    }

    @Override
    public void whenCollide(NvComp other) {

    }
}
