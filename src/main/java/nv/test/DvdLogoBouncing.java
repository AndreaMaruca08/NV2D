package nv.test;

import nv.components.NvComp;
import nv.components.NvGraphic;
import nv.core.Nv2DApp;
import nv.core.data.NvImage;

public class DvdLogoBouncing extends NvComp {
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
            image = app.loadImageFromResource("/textures/dvdLogo.png");
        }
    }

    @Override
    public void drawIntern(NvGraphic g) {
        g.drawImage(image, x, y, w, h);
    }

    @Override
    public void update(float dt) {
        preciseX += velocityX * dt;
        preciseY += velocityY * dt;

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
}