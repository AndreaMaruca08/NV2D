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

        // Note: atlas "tiles" must be loaded in NvContext before this component is created
        atlas = app.assets().getAtlas("tiles");
        if (atlas == null) {
            // If not loaded, try to load it now from the root textures folder
            atlas = app.assets().loadAtlas("tiles", "");
        }
        region = app.assets().getRegion("tiles", "dvdLogo");
    }

    @Override
    public void drawIntern(NvGraphic g) {
        // We use 0, 0 as local coordinates because draw(g) already applies component.x, component.y transformation in NvPixelGraphic.tx/ty
        g.drawImageRegion(
                atlas.image(), 0, 0, w, h,
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
            w += amount;
            h += amount;

            if (w >= 300) {
                isGettingBigger = false;
            }
        } else {
            w -= amount;
            h -= amount;

            if (w <= 150) {
                isGettingBigger = true;
            }
        }

        // The world is infinite, but we want to bounce within the current camera VIEWPORT.
        // camera.x/y is the world position of the top-left corner of the screen.
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
        } else if (preciseX + w >= maxX) {
            preciseX = maxX - w;
            velocityX = -Math.abs(velocityX);
        }

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
