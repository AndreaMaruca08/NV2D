package nv.test;

import nv.core.annotations.Example;
import nv.core.collision.Collidable;
import nv.core.components.NvComp;
import nv.core.components.NvRgbComp;
import nv.core.graphic.NvGraphic;
import nv.utils.shapes.dynamic.DynamicSquare;

@Example
public class WhenOverCollision extends NvRgbComp implements Collidable {
    protected boolean isOver = false;
    public WhenOverCollision(int x, int y, int w, int h) {
        super(x, y, w, h);
        setPhaseThrough(true);
        g = 0;
        b = 0;
    }

    @Override
    public void drawIntern(NvGraphic g) {
        g.setRGB(r,this.g,b);
        g.drawRect(0,0,w,h);
    }

    @Override
    public void update(float dt) {
        r = isOver ? 1 : 0;
        isOver = false;
    }

    @Override
    public void whenCollide(NvComp other){
        if(other instanceof DynamicSquare)
            isOver = true;
    }
}
