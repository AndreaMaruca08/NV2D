package nv.components;

import nv.core.Scene;

public class NvPixelGraphic extends NvGraphic {
    @Override
    public void drawTri(float base1, float base2, float y, float r, float g, float b){
        short[] triInds = { 0, 1, 2 };

        float b1 = component.getX() + base1;
        float b2 = component.getX() + base2;

        float[] triVerts = {
                b1, y,   r, g, b, wu, wv,
                b2, y,   r, g, b,   wu, wv,
                (b1 + b2)/2f, b1 + y/2,   r, g, b,   wu, wv,
        };

        appendGeometry(triVerts, triInds);
    }
    @Override
    public void drawRect(float xTopLeftBottomLeft, float xTopRightBottomRight, float yTops, float yBottoms, float r, float g, float b){
        float x1 = component.getX() + xTopLeftBottomLeft;
        float x2 = component.getX() + xTopRightBottomRight;
        float y1 = component.getY() + yTops;
        float y2 = component.getY() + yBottoms;

        float[] quadVerts = {
                x1, y1, r, g, b, wu, wv,
                x2, y1, r, g, b, wu, wv,
                x2, y2, r, g, b, wu, wv,
                x1, y2, r, g, b, wu, wv,
        };

        short[] quadInds = { 0, 1, 2,  2, 3, 0 };

        appendGeometry(quadVerts, quadInds);
    }
    @Override
    public void drawText(String text, float textX, float textY){
        float x = component.getX() + textX;
        float y = component.getY() + textY;
        Scene textGeo  = generateTextGeometry(text, x, y, fontAtlas);
        appendGeometry(textGeo.vertices(), textGeo.indices());
    }
}
