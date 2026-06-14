package nv.test;

import nv.components.NvClickable;
import nv.components.NvGraphic;
import nv.core.Nv2DApp;
import nv.core.data.NvImage;

public class DvdLogoBouncing extends NvClickable {
    private float preciseX;
    private float preciseY;

    private float velocityX = 1000f;
    private float velocityY = 1000f;

    private final Nv2DApp app = Nv2DApp.getInstance();
    private NvImage image;

    public DvdLogoBouncing(int x, int y) {
        super(x, y, 200, 200);
        this.preciseX = x;
        this.preciseY = y;
        if(image == null){
            image = app.loadImage("dvdLogo.png");
        }
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

        int amount = (int) (500 * dt);
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

        if (preciseX <= 0) {
            preciseX = 0;
            velocityX = Math.abs(velocityX);
        } else if (preciseX + w >= app.getWidth()) {
            preciseX = app.getWidth() - w;
            velocityX = -Math.abs(velocityX);
        }

        if (preciseY <= 0) {
            preciseY = 0;
            velocityY = Math.abs(velocityY);
        } else if (preciseY + h >= app.getHeight()) {
            preciseY = app.getHeight() - h;
            velocityY = -Math.abs(velocityY);
        }

        x = Math.round(preciseX);
        y = Math.round(preciseY);
    }

    @Override
    public void onClick() {

    }
}