package nv.utils;

import nv.core.annotations.ReadyComponent;
import nv.core.components.NvComp;
import nv.core.components.Vector2D;
import nv.core.NvContext;


/**
 * <p>Component used for moving objects, if you follow gravity remember to reset the speed</p>
 * @since 1.1
 * @author Andrea Maruca
 */
@ReadyComponent
@SuppressWarnings("unused")
public abstract class NvMovingComp extends NvComp {
    protected float velocityX;
    protected float velocityY;
    protected float accelerationX;
    protected float accelerationY;
    protected float gravityMultiplier = 20f;
    protected Vector2D direction;
    protected boolean gravity;

    public NvMovingComp(int x, int y, int w, int h,
                        float velocityX, float velocityY,
                        boolean followGravity, Vector2D direction
    ) {
        super(x, y, w, h);
        this.velocityX = velocityX;
        this.accelerationX = 0;
        this.accelerationY = followGravity ? gravityMultiplier : 0;
        this.gravity = followGravity;
        this.direction = direction;
    }

    @Override
    public void update(float dt) {
        this.velocityX += accelerationX * dt;
        this.velocityY += accelerationY * dt * (gravity ? gravityMultiplier : 1);

        x += Math.round(velocityX * direction.x * dt);
        y += Math.round(velocityY * (gravity ? 1 : direction.y) * dt);
        NvContext.markSceneDirty();
    }

    public void setAccelerationX(float accelerationX) {
        this.accelerationX = accelerationX;
    }

    public void setAccelerationY(float accelerationY) {
        this.accelerationY = accelerationY;
    }

    public void setDirection(Vector2D direction) {
        this.direction = direction;
    }

    public void setVelocityX(float velocityX) {
        this.velocityX = velocityX;
    }

    public void setVelocityY(float velocityY) {
        this.velocityY = velocityY;
    }
}
