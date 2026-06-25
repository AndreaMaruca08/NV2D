package nv.test.game.example;

import nv.core.NvContext;
import nv.core.collision.Collidable;
import nv.core.collision.CollisionSystem;
import nv.core.components.NvComp;
import nv.core.components.NvRgbComp;
import nv.core.components.Vector2D;
import nv.core.graphic.NvGraphic;

public class Projectile extends NvRgbComp implements Collidable {
    private int damage;
    public Vector2D direction;
    private NvContext ctx = NvContext.getInstance();
    
    private float floatX;
    private float floatY;
    private float speed = 300f; // Movement speed in pixels per second

    public Projectile(int x, int y, int w, int h, int damage, Vector2D direction) {
        super(x, y, w, h);
        this.damage = damage;
        this.direction = direction;
        this.floatX = x;
        this.floatY = y;
        setWeight(CollisionSystem.NO_WEIGHT);
    }

    @Override
    public void drawIntern(NvGraphic g) {
        g.drawOval(0, 0, w, r, this.g, b);
    }

    @Override
    public void update(float dt) {
        floatX += direction.x * speed * dt;
        floatY += direction.y * speed * dt;
        x = (int) floatX;
        y = (int) floatY;
        NvContext.markSceneDirty();
        
        if(x < -ctx.getWidth() || x > ctx.getWidth()*2 ||
                y < -ctx.getHeight() || y > ctx.getHeight()*2){
            destroy();
        }
    }
    @Override
    public void whenCollide(NvComp other){
        if(other instanceof CustomCharacter c){
            c.takeDamage(damage);
            destroy();
        }
    }
}
