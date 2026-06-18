package nv.test;

import nv.core.collision.Collidable;
import nv.core.graphic.NvGraphic;
import nv.core.components.NvRgbComp;

public class BigOvalTest extends NvRgbComp implements Collidable {
    public BigOvalTest(int x, int y, int w) {
        super(x, y, w, w);
    }

    @Override
    public void drawIntern(NvGraphic g) {
        g.drawOval(0, 0, w, 64, r,this.g,b);
    }

    @Override
    public void update(float dt) {

    }
}
