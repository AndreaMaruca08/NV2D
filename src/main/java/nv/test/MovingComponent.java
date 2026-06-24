package nv.test;

import nv.core.annotations.Example;
import nv.core.collision.Collidable;
import nv.core.components.Vector2D;
import nv.core.graphic.NvGraphic;
import nv.utils.NvMovingComp;

@Example
public class MovingComponent extends NvMovingComp implements Collidable {

    public MovingComponent(int x, int y, int w, int h, float velocityX, float velocityY, Vector2D direction, boolean gravity) {
        super(x, y, w, h, velocityX, velocityY, gravity, direction);

        setWeight(1);
    }

    @Override
    public void drawIntern(NvGraphic g) {
        g.setRGB(0,0,0);
        g.drawRect(0, 0, w,h);
        g.setRGB(1,0,0);
        g.drawText("TEST MOVING",0,0);
    }
}
