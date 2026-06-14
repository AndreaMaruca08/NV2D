package nv.test;

import nv.components.NvGraphic;
import nv.components.NvStateless;

public class Stateless extends NvStateless {
    public Stateless(int x, int y, int w, int h) {
        super(x, y, w, h);
    }
    @Override
    public void drawIntern(NvGraphic g) {
        g.drawRect(0,0,200,100, this);
        g.drawRect(500,0,200,100, this);
        g.drawRect(1000,0,200,100, this);
    }
}
