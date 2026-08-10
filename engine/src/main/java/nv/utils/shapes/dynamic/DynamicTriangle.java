package nv.utils.shapes.dynamic;

import nv.core.annotations.ReadyComponent;
import nv.core.collision.Collidable;
import nv.core.components.NvRgbComp;
import nv.core.graphic.NvGraphic;

/**
 * <p>Dynamic triangle with collisions</p>
 * @since 1.0
 * @author Andrea Maruca
 */
@ReadyComponent
@SuppressWarnings("unused")
public class DynamicTriangle extends NvRgbComp implements Collidable {
    public DynamicTriangle(int x, int y, int w, int h) {
        super(x, y, w, h);
    }

    @Override
    public void drawIntern(NvGraphic g) {
        g.drawTri(getX(), getX()+getW(), getY());
    }

    @Override
    public void update(float dt) {}
}
