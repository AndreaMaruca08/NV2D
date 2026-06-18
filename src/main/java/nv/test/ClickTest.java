package nv.test;

import nv.core.collision.Collidable;
import nv.core.components.NvComp;
import nv.core.graphic.NvGraphic;
import nv.core.input.Clickable;

public class ClickTest extends NvComp implements Clickable, Collidable {
    float r,gr,b;
    public ClickTest(int x, int y, int w, int h) {
        super(x, y, w, h);
        r = gr = b = 0;
    }

    @Override
    public void drawIntern(NvGraphic g) {
        g.drawRect(0,0, w, h, r, gr, b);
    }

    @Override
    public void onClick() {
        r = 1;
    }

    @Override
    public void onClickRelease() {
        r = 0;
    }

    @Override
    public void update(float dt) {
        if(isHovered){
            gr = 1;
        }else{
            gr = 0;
        }
    }
}
