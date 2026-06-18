package nv.utils.shapes.statics;

import nv.core.annotations.ReadyComponent;
import nv.core.collision.Collidable;
import nv.core.components.NvComp;
import nv.core.components.NvStateless;
import nv.core.graphic.NvGraphic;

/**
 * Simple Static square with collision
 * <p>It's meant to be an immovable shape and more efficient than dynamic shapes</p>
 *
 * @since 1.0
 * @author Andrea Maruca
 */
@ReadyComponent
@SuppressWarnings("unused")
public class StaticSquare extends NvStateless implements Collidable {
    protected float r, g, b;
    public StaticSquare(int x, int y, int w, int h) {
        super(x, y, w, h);

    }
    public void setRGB(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    @Override
    public void drawIntern(NvGraphic g) {
        g.drawRect(0,0,w,h,r,this.g,b, this);
    }

    @Override
    public void whenCollide(NvComp other) {
    }
}
