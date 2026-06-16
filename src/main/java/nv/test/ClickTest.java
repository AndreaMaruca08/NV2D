package nv.test;

import nv.components.NvComp;
import nv.core.NvGraphic;
import nv.core.Clickable;
import nv.core.collision.Collidable;

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
