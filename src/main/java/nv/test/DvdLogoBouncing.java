package nv.test;

import nv.core.NvContext;
import nv.core.assets.AtlasConverter;
import nv.core.components.NvComp;
import nv.core.graphic.NvGraphic;

import static nv.core.graphic.NvGraphic.camera;

public class DvdLogoBouncing extends NvComp {

    private float preciseX;
    private float preciseY;

    private float velocityX = 400f;
    private float velocityY = 400f;

    private final NvContext app = NvContext.getInstance();

    private AtlasConverter.Region region;
    private AtlasConverter.Atlas atlas;

    public DvdLogoBouncing(int x, int y) {
        super(x, y, 200, 200);

        this.preciseX = x;
        this.preciseY = y;

        atlas = app.assets().getAtlas("tiles");
        if (atlas == null) {
            atlas = app.assets().loadAtlas("tiles", "");
        }
        region = app.assets().getRegion("tiles", "dvdLogo");
    }

    @Override
    public void drawIntern(NvGraphic g) {
        g.drawImageRegion(
                atlas.image(), 0, 0, getW(), getH(),
                region.u1(), region.v1(),
                region.u2(), region.v2()
        );
    }

    private boolean isGettingBigger = false;

    @Override
    public void update(float dt) {

        preciseX += velocityX * dt;
        preciseY += velocityY * dt;

        int amount = (int) (100 * dt);

        if (isGettingBigger) {
            setW(getW() + amount);
            setH(getH() + amount);

            if (getW() >= 300) {
                isGettingBigger = false;
            }
        } else {
            setW(getW() - amount);
            setH(getH() - amount);

            if (getW() <= 150) {
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

        if (preciseX <= minX) {
            preciseX = minX;
            velocityX = Math.abs(velocityX);
        } else if (preciseX + getW() >= maxX) {
            preciseX = maxX - getW();
            velocityX = -Math.abs(velocityX);
        }

        if (preciseY <= minY) {
            preciseY = minY;
            velocityY = Math.abs(velocityY);
        } else if (preciseY + getH() >= maxY) {
            preciseY = maxY - getH();
            velocityY = -Math.abs(velocityY);
        }

        setX(Math.round(preciseX));
        setY(Math.round(preciseY));
        NvContext.markSceneDirty();
    }
}
