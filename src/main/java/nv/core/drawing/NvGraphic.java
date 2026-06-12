package nv.core.drawing;

import nv.core.components.NvComponent;
import nv.core.data.FontAtlas;

import java.util.Arrays;

public class NvGraphic {
    private static final int FLOATS_PER_VERTEX = 7;

    private NvComponent component;
    private FontAtlas fontAtlas;

    private float[] vertices;
    private short[] indices;

    private int vertexFloatCount;
    private int indexCount;

    private float w, h;
    private float wu, wv;

    public NvGraphic() {
        this.component = null;
        this.vertices = new float[1024 * FLOATS_PER_VERTEX];
        this.indices = new short[1024];
        this.fontAtlas = null;

        this.vertexFloatCount = 0;
        this.indexCount = 0;
    }

    public void initialize(float w, float h, float wu, float wv, FontAtlas fontAtlas){
        this.w = w;
        this.h = h;
        this.wu = wu;
        this.wv = wv;
        this.fontAtlas = fontAtlas;

        this.vertexFloatCount = 0;
        this.indexCount = 0;
    }

    public void setComponent(NvComponent component){
        this.component = component;
    }

    private void appendGeometry(float[] newVertices, short[] newIndices) {
        int vertexOffset = vertexFloatCount / FLOATS_PER_VERTEX;

        ensureVertexCapacity(vertexFloatCount + newVertices.length);
        ensureIndexCapacity(indexCount + newIndices.length);

        System.arraycopy(newVertices, 0, vertices, vertexFloatCount, newVertices.length);

        for (int i = 0; i < newIndices.length; i++) {
            indices[indexCount + i] = (short) (newIndices[i] + vertexOffset);
        }

        vertexFloatCount += newVertices.length;
        indexCount += newIndices.length;
    }

    private void ensureVertexCapacity(int requiredCapacity) {
        if (requiredCapacity <= vertices.length) {
            return;
        }

        int newCapacity = vertices.length;

        while (newCapacity < requiredCapacity) {
            newCapacity *= 2;
        }

        vertices = Arrays.copyOf(vertices, newCapacity);
    }

    private void ensureIndexCapacity(int requiredCapacity) {
        if (requiredCapacity <= indices.length) {
            return;
        }

        int newCapacity = indices.length;

        while (newCapacity < requiredCapacity) {
            newCapacity *= 2;
        }

        indices = Arrays.copyOf(indices, newCapacity);
    }

    public void drawTri(float base1, float base2, float r, float g, float b){
        short[] triInds = { 0, 1, 2 };

        float b1 = component.getX() + base1;
        float b2 = component.getX() + base2;

        float[] triVerts = {
                b1, b2,   r, g, b, wu, wv,
                b2, b2,   r, g, b,   wu, wv,
                (b1 + b2)/2f, b1,   r, g, b,   wu, wv,
        };

        appendGeometry(triVerts, triInds);
    }

    public void drawRect(float xTopLeftBottomLeft, float xTopRightBottomRight, float yTops, float yBottoms, float r, float g, float b){
        float[] quadVerts = {
                xTopLeftBottomLeft,     yTops,    r, g, b, wu, wv,
                xTopRightBottomRight,   yTops,    r, g, b, wu, wv,
                xTopRightBottomRight,   yBottoms, r, g, b, wu, wv,
                xTopLeftBottomLeft,     yBottoms, r, g, b, wu, wv,
        };

        short[] quadInds = { 0, 1, 2,  2, 3, 0 };

        appendGeometry(quadVerts, quadInds);
    }

    public void drawText(String text, float textX, float textY){
        Scene textGeo  = generateTextGeometry(text, textX, textY, fontAtlas);
        appendGeometry(textGeo.vertices(), textGeo.indices());
    }

    public float[] getVertices(){
        return Arrays.copyOf(vertices, vertexFloatCount);
    }

    public short[] getIndices(){
        return Arrays.copyOf(indices, indexCount);
    }


    //LOW LEVEL
    public static Scene generateTextGeometry(String text, float startX, float startY, FontAtlas atlas) {
        int n = text.length();
        float[] vertices = new float[n * 4 * 7]; // 4 vertici × 7 float
        short[] indices  = new short[n * 6];
        float cursorX = startX;

        for (int i = 0; i < n; i++) {
            FontAtlas.Glyph g = atlas.getGlyph(text.charAt(i));

            float x0 = cursorX,            y0 = startY;
            float x1 = cursorX + g.width,  y1 = startY + g.height;

            int v = i * 4 * 7;
            // top-left
            vertices[v] = x0; vertices[v +  1] = y0;
            vertices[v +  2] = 1f; vertices[v +  3] = 1f; vertices[v +  4] = 1f;
            vertices[v +  5] = g.uMin; vertices[v +  6] = g.vMin;
            // top-right
            vertices[v +  7] = x1; vertices[v +  8] = y0;
            vertices[v +  9] = 1f; vertices[v + 10] = 1f; vertices[v + 11] = 1f;
            vertices[v + 12] = g.uMax; vertices[v + 13] = g.vMin;
            // bottom-right
            vertices[v + 14] = x1; vertices[v + 15] = y1;
            vertices[v + 16] = 1f; vertices[v + 17] = 1f; vertices[v + 18] = 1f;
            vertices[v + 19] = g.uMax; vertices[v + 20] = g.vMax;
            // bottom-left
            vertices[v + 21] = x0; vertices[v + 22] = y1;
            vertices[v + 23] = 1f; vertices[v + 24] = 1f; vertices[v + 25] = 1f;
            vertices[v + 26] = g.uMin; vertices[v + 27] = g.vMax;

            int idx = i * 6, base = i * 4;
            indices[idx]     = (short)  base;
            indices[idx + 1] = (short) (base + 1);
            indices[idx + 2] = (short) (base + 2);
            indices[idx + 3] = (short) (base + 2);
            indices[idx + 4] = (short) (base + 3);
            indices[idx + 5] = (short)  base;

            cursorX += g.advance;
        }
        return new Scene(vertices, indices);
    }
}