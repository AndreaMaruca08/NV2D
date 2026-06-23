package nv.test;

import nv.core.collision.Collidable;
import nv.core.components.NvComp;
import nv.core.graphic.NvGraphic;
import nv.core.io.Clickable;
import nv.core.io.Hoverable;
import nv.core.io.KeyboardListener;

public class Esempio extends NvComp implements Collidable, Clickable, Hoverable {
    private float r = 1;
    public Esempio(int x, int y, int w, int h) {
        super(x, y, w, h);
    }

    @Override
    public void drawIntern(NvGraphic g) {
        g.setRGB(r,0,0);
        g.drawRect(0,0,w,h);
    }

    @Override
    public void update(float dt) {
        if(isHovered){
            r = 0;
        }else {
            r = 1;
        }
    }

    @Override
    public void onClick() {
        r = 0;
    }

    @Override
    public void onClickRelease() {
        r = 1;
    }

    @Override
    public void whenCollide(NvComp other){

    }
}
