package nv.components;

import nv.core.AppendableGeometry;
import nv.core.NvGraphic;
import nv.core.Scene;
import nv.core.data.NvImage;

/**
 * concrete implementation of NvGraphic
 * that specializes in rendering geometric shapes and text as direct pixel graphics.
 */
public class NvPixelGraphic extends NvGraphic {

    private float tx(float worldX) {
        if(component.isHUD())
            return worldX;
        return (worldX - camera.x) * camera.zoom;
    }

    private float ty(float worldY) {
        if(component.isHUD())
            return worldY;
        return (worldY - camera.y) * camera.zoom;
    }

    @Override
    public void drawTri(float base1, float base2, float y,
                        float r, float g, float b,
                        AppendableGeometry comp) {

        float x1 = tx(component.x + base1);
        float x2 = tx(component.x + base2);
        float y1 = ty(component.y + y);
        float apexY = ty(component.y + y - component.h);

        float[] triVerts = {
                x1, y1, r, g, b, wu, wv, 0f,
                x2, y1, r, g, b, wu, wv, 0f,
                (x1 + x2) * 0.5f, apexY, r, g, b, wu, wv, 0f,
        };

        int[] triInds = {0, 1, 2};
        comp.append(triVerts, triInds);
    }

    @Override
    public void drawOval(float x, float y, float radius, int accuracy,
                         float r, float g, float b,
                         AppendableGeometry comp) {

        float[] ovalVerts = new float[(accuracy + 1) * FLOATS_PER_VERTEX];
        int[] ovalInds = new int[accuracy * 3];

        x += radius/2;
        y += radius/2;

        float cx = tx(component.x + x);
        float cy = ty(component.y + y);

        float rScaled = radius * camera.zoom;

        // center
        ovalVerts[0] = cx;
        ovalVerts[1] = cy;
        ovalVerts[2] = r;
        ovalVerts[3] = g;
        ovalVerts[4] = b;
        ovalVerts[5] = wu;
        ovalVerts[6] = wv;
        ovalVerts[7] = 0f;

        for (int i = 0; i < accuracy; i++) {

            float angle = (float) (i * 2 * Math.PI / accuracy);

            int vi = i + 1;
            int off = vi * FLOATS_PER_VERTEX;

            float lx = (float) Math.cos(angle) * rScaled;
            float ly = (float) Math.sin(angle) * rScaled;

            ovalVerts[off]     = cx + lx;
            ovalVerts[off + 1] = cy + ly;
            ovalVerts[off + 2] = r;
            ovalVerts[off + 3] = g;
            ovalVerts[off + 4] = b;
            ovalVerts[off + 5] = wu;
            ovalVerts[off + 6] = wv;
            ovalVerts[off + 7] = 0f;

            int idx = i * 3;
            int cur = i + 1;
            int next = (i + 1) % accuracy + 1;

            ovalInds[idx] = 0;
            ovalInds[idx + 1] = cur;
            ovalInds[idx + 2] = next;
        }

        comp.append(ovalVerts, ovalInds);
    }

    @Override
    public void drawRect(float x, float y, float w, float h,
                         float r, float g, float b,
                         AppendableGeometry comp) {

        float x1 = tx(component.x + x);
        float y1 = ty(component.y + y);
        float x2 = tx(component.x + x + w);
        float y2 = ty(component.y + y + h);

        float[] quadVerts = {
                x1, y1, r, g, b, wu, wv, a,
                x2, y1, r, g, b, wu, wv, a,
                x2, y2, r, g, b, wu, wv, a,
                x1, y2, r, g, b, wu, wv, a,
        };

        int[] quadInds = {0, 1, 2, 2, 3, 0};
        comp.append(quadVerts, quadInds);
    }

    @Override
    public void drawText(String text, float textX, float textY,
                         AppendableGeometry comp) {

        float x = tx(component.x + textX);
        float y = ty(component.y + textY);

        Scene textGeo = generateTextGeometry(text, x, y, fontAtlas);
        comp.append(textGeo.vertices(), textGeo.indices());
    }

    @Override
    public void drawImage(NvImage image, float x, float y, float w, float h) {
        drawImageRegion(image, x, y, w, h, 0.0f, 0.0f, 1.0f, 1.0f);
    }

    @Override
    public void drawImageRegion(NvImage image,
                                float x, float y, float w, float h,
                                float u0, float v0, float u1, float v1) {

        float x1 = tx(component.x + x);
        float y1 = ty(component.y + y);
        float x2 = tx(component.x + x + w);
        float y2 = ty(component.y + y + h);

        float texIndex = (float) image.getTextureIndex();

        float dr = r, dg = g, db = b;
        if (dr == 0 && dg == 0 && db == 0) {
            dr = dg = db = 1f;
        }

        float[] quadVerts = {
                x1, y1, dr, dg, db, u0, v0, texIndex,
                x2, y1, dr, dg, db, u1, v0, texIndex,
                x2, y2, dr, dg, db, u1, v1, texIndex,
                x1, y2, dr, dg, db, u0, v1, texIndex,
        };

        int[] quadInds = {0, 1, 2, 2, 3, 0};

        appendImageGeometry(quadVerts, quadInds);
    }
}