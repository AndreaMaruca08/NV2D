package nv.components;

import nv.core.Scene;
import nv.core.data.FontAtlas;

import java.util.Arrays;

/**
 * <h3>Graphic drawer</h3>
 * <p>Abstract class used for drawing geometrical figures, writes the vertices and indices in 2 arrays</p>
 * <h5>Can be extended for specific vertices and indices calculation</h5>
 * @since 1.0
 * @author Andrea Maruca
 */
public abstract class NvGraphic {
    protected static final int FLOATS_PER_VERTEX = 7;

    protected NvComp component;
    protected FontAtlas fontAtlas;

    protected float[] vertices;
    protected short[] indices;

    protected int vertexFloatCount;
    protected int indexCount;

    protected float w, h;
    protected float wu, wv;

    protected float r=0, g=0, b=0, a=0;

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

    public void setRGB(float r, float g, float b){
        this.r = r;
        this.g = g;
        this.b = b;
    }
    public void setTransparency(float alpha){
        this.a = alpha;
    }

    public void setComponent(NvComp component){
        this.component = component;
    }

    protected void appendGeometry(float[] newVertices, short[] newIndices) {
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

    public abstract void drawTri(float base1, float base2, float y, float r, float g, float b);
    public void drawTri(float base1, float base2, float y){
        drawTri(base1, base2, y, r, g, b);
    }

    public abstract void drawRect(float x, float y, float w, float h,   float r, float g, float b);
    public void drawRect(float x, float y, float w, float h){
        drawRect(x, y, w, h, r, g, b);
    }

    public abstract void drawText(String text, float textX, float textY);

    public void drawRectBorder(float x, float y, float w, float h, float thickness, float r, float g, float b) {
        drawRect(x, y, w, thickness, r, g, b);
        drawRect(x, y + h - thickness, w, thickness, r, g, b);
        drawRect(x, y, thickness, h, r, g, b);
        drawRect(x + w - thickness, y, thickness, h, r, g, b);
    }

    public void drawRectBorder(float x, float y, float w, float h, float thickness) {
        drawRectBorder(x, y, w, h, thickness, r, g, b);
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