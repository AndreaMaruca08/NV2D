package nv.test;

import nv.core.collision.Collidable;
import nv.core.components.NvComp;
import nv.core.graphic.NvGraphic;

/**
 * Concrete NvComp implementation for testing purposes.
 */
public class MockComp extends NvComp implements Collidable {
    private boolean collided = false;
    private NvComp collidedWith = null;

    public MockComp(int x, int y, int w, int h) {
        super(x, y, w, h);
        TestHelper.attachDummyContext(this);
    }

    public boolean isCollided() {
        return collided;
    }

    public NvComp getCollidedWith() {
        return collidedWith;
    }

    public void resetCollisionState() {
        this.collided = false;
        this.collidedWith = null;
    }

    @Override
    public void whenCollide(NvComp other) {
        this.collided = true;
        this.collidedWith = other;
    }

    @Override
    public void drawIntern(NvGraphic g) {}

    @Override
    public void update(float dt) {}
}
