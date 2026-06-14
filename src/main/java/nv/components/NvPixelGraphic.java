package nv.components;

import nv.core.Scene;
import nv.core.data.NvImage;

/**
 * <h3>concrete implementation of NvGraphic</h3
 * <p>that specializes in rendering geometric shapes and text as direct pixel graphics.</p>
 *
 * @since 1.0
 * @author Andrea Maruca
 */
public class NvPixelGraphic extends NvGraphic {
    @Override
    public void drawTri(float base1, float base2, float y, float r, float g, float b){
        float b1 = component.getX() + base1;
        float b2 = component.getX() + base2;
        float y1 = component.getY() + y;
        float apexY = y1 - component.getH(); // Apex verso l'alto rispetto alla base

        float[] triVerts = {
                b1, y1,   r, g, b, wu, wv, 0f,
                b2, y1,   r, g, b, wu, wv, 0f,
                (b1 + b2)/2f, apexY,   r, g, b,   wu, wv, 0f,
        };
        int[] triInds = { 0, 1, 2 };

        appendGeometry(triVerts, triInds);
    }
    @Override
    public void drawRect(float x, float y, float w, float h, float r, float g, float b){
        float x1 = component.getX() + x;
        float y1 = component.getY() + y;
        float x2 = x1 + w;
        float y2 = y1 + h;

        float[] quadVerts = {
                x1, y1, r, g, b, wu, wv, 0f,
                x2, y1, r, g, b, wu, wv, 0f,
                x2, y2, r, g, b, wu, wv, 0f,
                x1, y2, r, g, b, wu, wv, 0f,
        };

        int[] quadInds = { 0, 1, 2,  2, 3, 0 };

        appendGeometry(quadVerts, quadInds);
    }
    @Override
    public void drawText(String text, float textX, float textY){
        float x = component.getX() + textX;
        float y = component.getY() + textY;
        Scene textGeo  = generateTextGeometry(text, x, y, fontAtlas);
        appendGeometry(textGeo.vertices(), textGeo.indices());
    }

    @Override
    public void drawImage(NvImage image, float x, float y, float w, float h) {
        drawImageRegion(image, x, y, w, h, 0.0f, 0.0f, 1.0f, 1.0f);
    }

    @Override
    public void drawImageRegion(NvImage image, float x, float y, float w, float h,
                                float u0, float v0, float u1, float v1) {
        float x1 = component.getX() + x;
        float y1 = component.getY() + y;
        float x2 = x1 + w;
        float y2 = y1 + h;
        
        float texIndex = (float) image.getTextureIndex();

        // Se il colore non è impostato (tutti 0), usiamo bianco per non oscurare l'immagine
        float dr = r, dg = g, db = b;
        if (dr == 0 && dg == 0 && db == 0) {
            dr = 1f; dg = 1f; db = 1f;
        }
        
        // 8-float format per immagini: pos(2) + color(3) + UV(2) + texIndex(1)
        float[] quadVerts = {
                x1, y1, dr, dg, db, u0, v0, texIndex,
                x2, y1, dr, dg, db, u1, v0, texIndex,
                x2, y2, dr, dg, db, u1, v1, texIndex,
                x1, y2, dr, dg, db, u0, v1, texIndex,
        };
        
        int[] quadInds = { 0, 1, 2,  2, 3, 0 };
        
        appendImageGeometry(quadVerts, quadInds);
    }
}
