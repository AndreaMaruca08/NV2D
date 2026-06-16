package nv.test;

import nv.components.NvComp;
import nv.core.NvContext;
import nv.core.NvGraphic;
import nv.core.collision.Collidable;
import nv.core.data.NvImage;

import static nv.core.NvGraphic.camera;

public class DvdLogoBouncing extends NvComp {
    private float preciseX;
    private float preciseY;

    private float velocityX = 1000f;
    private float velocityY = 1000f;

    private final NvContext app = NvContext.getInstance();
    private final NvImage image;

    public DvdLogoBouncing(int x, int y) {
        super(x, y, 200, 200);
        this.preciseX = x;
        this.preciseY = y;
        image = app.loadImage("dvdLogo.png");
    }

    @Override
    public void drawIntern(NvGraphic g) {
        g.drawImage(image, 0, 0, w, h);
    }

    private boolean isGettingBigger = false;

    @Override
    public void update(float dt) {
        preciseX += velocityX * dt;
        preciseY += velocityY * dt;

        int amount = (int) (1000 * dt);
        if(isGettingBigger){
            x -= amount;
            w += amount;
            h += amount;
            if(w >= 300){
                isGettingBigger = false;
            }
        } else {
            x += amount;
            w -= amount;
            h -= amount;
            if(w <= 150){
                isGettingBigger = true;
            }
        }

        float zoom = Math.max(camera.zoom, 0.0001f);
        float viewW = app.getWidth() / zoom;
        float viewH = app.getHeight() / zoom;
        
        float minX = camera.x;
        float maxX = camera.x + viewW;
        float minY = camera.y;
        float maxY = camera.y + viewH;

        // Rimbalzo X
        if (preciseX <= minX) {
            preciseX = minX;
            velocityX = Math.abs(velocityX);
        } else if (preciseX + w >= maxX) {
            preciseX = maxX - w;
            velocityX = -Math.abs(velocityX);
        }

        // Rimbalzo Y
        if (preciseY <= minY) {
            preciseY = minY;
            velocityY = Math.abs(velocityY);
        } else if (preciseY + h >= maxY) {
            preciseY = maxY - h;
            velocityY = -Math.abs(velocityY);
        }

        x = Math.round(preciseX);
        y = Math.round(preciseY);
    }
}
