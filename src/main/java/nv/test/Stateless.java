package nv.test;

import nv.core.components.NvStateless;
import nv.core.graphic.NvGraphic;

public class Stateless extends NvStateless {
    public Stateless(int x, int y, int w, int h) {
        super(x, y, w, h);
    }
    @Override
    public void drawIntern(NvGraphic g) {
        g.drawRect(0,0,200,100, this);
    }
}
