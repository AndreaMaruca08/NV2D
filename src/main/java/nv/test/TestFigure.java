package nv.test;

import nv.components.NvComp;
import nv.components.NvGraphic;
import nv.core.Nv2DApp;

public class TestFigure extends NvComp {

    private final float r,g,b;
    private final String text;
    Nv2DApp app = Nv2DApp.getInstance();

    public TestFigure(int x, int y, int w, int h, int random) {
        super(x, y, w, h);
        this.r = (float)((random % 13 > 6 ? 0.1f : 0.8f) * Math.random());
        this.g = (float)((random % 122 > 43 ? 0.1f : 0.8f) * Math.random());
        this.b = (float)((random % 241 > 121 ? 0.1f : 0.8f) * Math.random());
        text = ""+random;
    }

    @Override
    public void drawIntern(NvGraphic g) {
        g.setRGB(r, this.g, b);
        g.drawRect(0, 0, 300, 300);
    }

    boolean front = true;
    @Override
    public void update(float dt) {
        if(front){
            x -= (int) (2000*dt);
        }else {
            x += (int) (2000*dt);
        }
        if(x+w >= app.getWidth() || x <= 0){
            front = !front;
        }
    }

}
