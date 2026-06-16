package nv.components.shapes.dynamic;

import nv.components.NvRgbComp;
import nv.core.NvGraphic;

public class DynamicTriangle extends NvRgbComp {
    public DynamicTriangle(int x, int y, int w, int h) {
        super(x, y, w, h);
    }

    @Override
    public void drawIntern(NvGraphic g) {
        g.drawTri(x, x+w, y);
    }

    @Override
    public void update(float dt) {}
}
