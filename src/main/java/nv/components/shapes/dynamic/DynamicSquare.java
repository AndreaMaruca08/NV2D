package nv.components.shapes.dynamic;

import nv.components.NvRgbComp;
import nv.core.NvGraphic;
import nv.core.collision.Collidable;

/**
 * Simple square with collision
 *
 * @since 1.0
 * @author Andrea Maruca
 */
public class DynamicSquare extends NvRgbComp implements Collidable {
    public DynamicSquare(int x, int y, int w, int h) {
        super(x, y, w, h);

    }
    @Override
    public void drawIntern(NvGraphic g) {
        g.drawRect(0,0,w,h,r,this.g,b);
    }

    @Override
    public void update(float dt) {

    }
}
