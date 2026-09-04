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
    public DynamicTriangle(float x, float y, float w, float h) {
        super(x, y, w, h);
    }

    @Override
    public void drawIntern(NvGraphic g) {
        g.drawTri(0, getW(), getH(), r, this.g, b);
    }

    @Override
    public void update(float dt) {}
}
