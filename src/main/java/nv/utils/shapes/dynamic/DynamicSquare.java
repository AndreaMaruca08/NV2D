package nv.utils.shapes.dynamic;

import nv.core.annotations.ReadyComponent;
import nv.core.collision.Collidable;
import nv.core.components.NvRgbComp;
import nv.core.graphic.NvGraphic;

/**
 * Simple square with collision
 *
 * @since 1.0
 * @author Andrea Maruca
 */
@ReadyComponent
@SuppressWarnings("unused")
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
