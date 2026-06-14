package nv.test;

import nv.components.NvComp;
import nv.components.NvGraphic;

public class CircleTest extends NvComp {

    public CircleTest(int x, int y, int w, int h) {
        super(x, y, w, h);
    }

    @Override
    public void drawIntern(NvGraphic g) {
        g.setRGB(0.5f,0.4f,0);
        g.drawOval(0, 0, 200, 32);
    }

    @Override
    public void update(float dt) {

    }
}
