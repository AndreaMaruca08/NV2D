package nv.core.graphic;

import nv.core.AppendableGeometry;
import nv.core.Scene;
import nv.core.annotations.EngineCore;
import nv.core.camera.NvCamera;
import nv.core.components.NvComp;
import nv.core.data.FontAtlas;
import nv.core.data.NvImage;

import java.util.Arrays;

/**
 * <h3>Graphic drawer</h3>
 * <p>Abstract class used for drawing geometrical figures, writes the vertices and indices in 2 arrays</p>
 * <h5>Can be extended for specific vertices and indices calculation</h5>
 * @since 1.0
 * @author Andrea Maruca
 */
@EngineCore
@SuppressWarnings("unused")
public abstract class NvGraphic implements AppendableGeometry {
    public static final int FLOATS_PER_VERTEX = 8;

    protected NvComp component;
    protected FontAtlas fontAtlas;

    protected float[] vertices;
    protected int[] indices;

    protected float[] imageVertices;
    protected int[] imageIndices;

    protected int vertexFloatCount;
    protected int indexCount;
    protected int imageVertexFloatCount;
    protected int imageIndexCount;

    protected float w, h;
    protected float wu, wv;

    protected float r=0, g=0, b=0, a=0;

    public static NvCamera camera = new NvCamera(0,0,1);
    public static void setCurrentCamera(NvCamera camera){
        NvGraphic.camera = camera;
    }

    public NvGraphic() {
        this.component = null;
        this.vertices = new float[1024 * FLOATS_PER_VERTEX];
        this.indices = new int[1024];
        this.imageVertices = new float[1024 * FLOATS_PER_VERTEX];
        this.imageIndices = new int[1024];
        this.fontAtlas = null;

        this.vertexFloatCount = 0;
        this.indexCount = 0;
        this.imageVertexFloatCount = 0;
        this.imageIndexCount = 0;
    }

    public void initialize(float w, float h, float wu, float wv, FontAtlas fontAtlas){
        this.w = w;
        this.h = h;
        this.wu = wu;
        this.wv = wv;
        this.fontAtlas = fontAtlas;

        this.vertexFloatCount = 0;
        this.indexCount = 0;
        this.imageVertexFloatCount = 0;
        this.imageIndexCount = 0;
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

    public void applyTransformsToBatch(int vStart, int iStart) {
        if (component == null || component.rotation == 0) return;

        float cos = (float) Math.cos(component.rotation);
        float sin = (float) Math.sin(component.rotation);

        rotateVertArray(vStart, cos, sin, vertexFloatCount, vertices);

        rotateVertArray(iStart, cos, sin, imageVertexFloatCount, imageVertices);
    }

    private void rotateVertArray(int iStart, float cos, float sin, int vertexCount, float[] imageVertices) {
        for (int i = iStart; i < vertexCount; i += FLOATS_PER_VERTEX) {
            float lx = vertices[i] - (component.getX() + w+ component.getW()/2f);
            float ly = vertices[i+1] - (component.getY() + component.getH()/2f);

            imageVertices[i]     = (lx * cos - ly * sin) + component.getX();
            imageVertices[i + 1] = (lx * sin + ly * cos) + component.getY();
        }
    }

    public int getVertexFloatCount() {
        return vertexFloatCount;
    }

    public int getImageVertexFloatCount() {
        return imageVertexFloatCount;
    }

    @Override
    public void append(float[] newVertices, int[] newIndices) {
        int vertexOffset = vertexFloatCount / FLOATS_PER_VERTEX;

        ensureVertexCapacity(vertexFloatCount + newVertices.length);
        ensureIndexCapacity(indexCount + newIndices.length);

        System.arraycopy(newVertices, 0, vertices, vertexFloatCount, newVertices.length);

        for (int i = 0; i < newIndices.length; i++) {
            indices[indexCount + i] = newIndices[i] + vertexOffset;
        }

        vertexFloatCount += newVertices.length;
        indexCount += newIndices.length;
    }

    protected void appendImageGeometry(float[] newVertices, int[] newIndices) {
        int vertexOffset = imageVertexFloatCount / FLOATS_PER_VERTEX;

        ensureImageVertexCapacity(imageVertexFloatCount + newVertices.length);
        ensureImageIndexCapacity(imageIndexCount + newIndices.length);

        System.arraycopy(newVertices, 0, imageVertices, imageVertexFloatCount, newVertices.length);

        for (int i = 0; i < newIndices.length; i++) {
            imageIndices[imageIndexCount + i] = newIndices[i] + vertexOffset;
        }

        imageVertexFloatCount += newVertices.length;
        imageIndexCount += newIndices.length;
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

    private void ensureImageVertexCapacity(int requiredCapacity) {
        if (requiredCapacity <= imageVertices.length) {
            return;
        }

        int newCapacity = imageVertices.length;

        while (newCapacity < requiredCapacity) {
            newCapacity *= 2;
        }

        imageVertices = Arrays.copyOf(imageVertices, newCapacity);
    }

    private void ensureImageIndexCapacity(int requiredCapacity) {
        if (requiredCapacity <= imageIndices.length) {
            return;
        }

        int newCapacity = imageIndices.length;

        while (newCapacity < requiredCapacity) {
            newCapacity *= 2;
        }

        imageIndices = Arrays.copyOf(imageIndices, newCapacity);
    }

    public void drawPentagon(float x, float y, float radius, float r, float g, float b, AppendableGeometry comp){
        drawOval(x, y, radius, 5, r, g, b, comp);
    };
    public void drawPentagon(float x, float y, float radius, float r, float g, float b){
        drawPentagon(x, y, radius, r, g, b, this);
    };
    public void drawPentagon(float x, float y, float radius){
        drawPentagon(x, y, radius, r, g, b, this);
    };
    public void drawPentagon(float x, float y, float radius, AppendableGeometry comp){
        drawPentagon(x, y, radius, r, g, b, comp);
    };

    public void drawHexagon(float x, float y, float radius, float r, float g, float b, AppendableGeometry comp){
        drawOval(x, y, radius, 6, r, g, b, comp);
    };
    public void drawHexagon(float x, float y, float radius, float r, float g, float b){
        drawHexagon(x, y, radius, r, g, b, this);
    };
    public void drawHexagon(float x, float y, float radius){
        drawHexagon(x, y, radius, r, g, b, this);
    };
    public void drawHexagon(float x, float y, float radius, AppendableGeometry comp){
        drawHexagon(x, y, radius, r, g, b, comp);
    };


    public abstract void drawTri(float base1, float base2, float y, float r, float g, float b, AppendableGeometry comp);

    public void drawTri(float base1, float base2, float y) {
        drawTri(base1, base2, y, r, g, b, this);
    }

    public void drawTri(float base1, float base2, float y, float r, float g, float b) {
        drawTri(base1, base2, y, r, g, b, this);
    }

    public void drawTri(float base1, float base2, float y, AppendableGeometry comp) {
        drawTri(base1, base2, y, r, g, b, comp);
    }

    public abstract void drawOval(float x, float y, float radius, int accuracy, float r, float g, float b, AppendableGeometry comp);
    public void drawOval(float x, float y, float radius, int accuracy){
        drawOval(x, y, radius, accuracy, r, g, b, this);
    };
    public void drawOval(float x, float y, float radius, float r, float g, float b){
        drawOval(x, y, radius, 16, r, g, b, this);
    };    public void drawOval(float x, float y, float radius, int accuracy, float r, float g, float b){
        drawOval(x, y, radius, accuracy, r, g, b, this);
    };
    public void drawOval(float x, float y, float radius, float r, float g, float b, AppendableGeometry comp){
        drawOval(x, y, radius, 16, r, g, b, this);
    };
    public void drawOval(float x, float y, float radius, int accuracy, AppendableGeometry comp){
        drawOval(x, y, radius, accuracy, r, g, b, comp);
    };
    public void drawOval(float x, float y, float radius){
        drawOval(x, y, radius, 16, r, g, b, this);
    };
    public void drawOval(float x, float y, float radius, AppendableGeometry comp){
        drawOval(x, y, radius, 16, r, g, b, comp);
    };

    public abstract void drawRect(float x, float y, float w, float h, float r, float g, float b, AppendableGeometry comp);
    public void drawRect(float x, float y, float w, float h) {
        drawRect(x, y, w, h, r, g, b, this);
    }
    public void drawRect(float x, float y, float w, float h, float r, float g, float b) {
        drawRect(x, y, w, h, r, g, b, this);
    }
    public void drawRect(float x, float y, float w, float h, AppendableGeometry comp) {
        drawRect(x, y, w, h, r, g, b, comp);
    }

    public abstract void drawRoundRect(float x, float y, float w, float h, float radius, float r, float g, float b, AppendableGeometry comp);
    public void drawRoundRect(float x, float y, float w, float h, float radius) {
        drawRoundRect(x, y, w, h, radius, r, g, b, this);
    }
    public void drawRoundRect(float x, float y, float w, float h, float radius, float r, float g, float b) {
        drawRoundRect(x, y, w, h, radius, r, g, b, this);
    }
    public void drawRoundRect(float x, float y, float w, float h, float radius, AppendableGeometry comp) {
        drawRoundRect(x, y, w, h, radius, r, g, b, comp);
    }

    public abstract void drawText(String text, float textX, float textY, AppendableGeometry comp);
    public void drawText(String text, float textX, float textY) {
        drawText(text, textX, textY, this);
    }

    public abstract void drawImage(NvImage image, float x, float y, float w, float h);

    /**
     * Disegna una regione di un'immagine (utile per texture atlas).
     * Le coordinate UV vanno da 0.0 a 1.0 rispetto all'immagine intera.
     *
     * @param image  immagine registrata (anche un atlas con più sprite)
     * @param x      posizione X in coordinate componente
     * @param y      posizione Y in coordinate componente
     * @param w      larghezza disegnata sullo schermo
     * @param h      altezza disegnata sullo schermo
     * @param u0     UV X sinistra (0.0 = bordo sinistro)
     * @param v0     UV Y superiore (0.0 = bordo superiore)
     * @param u1     UV X destra (1.0 = bordo destro)
     * @param v1     UV Y inferiore (1.0 = bordo inferiore)
     */
    public abstract void drawImageRegion(NvImage image, float x, float y, float w, float h,
                                         float u0, float v0, float u1, float v1);

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

    public int[] getIndices(){
        return Arrays.copyOf(indices, indexCount);
    }

    public float[] getImageVertices(){
        return Arrays.copyOf(imageVertices, imageVertexFloatCount);
    }

    public int[] getImageIndices(){
        return Arrays.copyOf(imageIndices, imageIndexCount);
    }


    //LOW LEVEL
    public static Scene generateTextGeometry(String text, float startX, float startY, FontAtlas atlas) {
        int n = text.length();
        float[] vertices = new float[n * 4 * FLOATS_PER_VERTEX]; // 4 vertici × 8 float
        int[] indices  = new int[n * 6];
        float cursorX = startX;

        for (int i = 0; i < n; i++) {
            FontAtlas.Glyph g = atlas.getGlyph(text.charAt(i));

            float x0 = cursorX,            y0 = startY;
            float x1 = cursorX + g.width,  y1 = startY + g.height;

            int v = i * 4 * FLOATS_PER_VERTEX;
            // top-left
            vertices[v     ] = x0;    vertices[v +  1] = y0;
            vertices[v +  2] = 1f;    vertices[v +  3] = 1f;    vertices[v +  4] = 1f;
            vertices[v +  5] = g.uMin; vertices[v +  6] = g.vMin; vertices[v +  7] = 0f;
            // top-right
            vertices[v +  8] = x1;    vertices[v +  9] = y0;
            vertices[v + 10] = 1f;    vertices[v + 11] = 1f;    vertices[v + 12] = 1f;
            vertices[v + 13] = g.uMax; vertices[v + 14] = g.vMin; vertices[v + 15] = 0f;
            // bottom-right
            vertices[v + 16] = x1;    vertices[v + 17] = y1;
            vertices[v + 18] = 1f;    vertices[v + 19] = 1f;    vertices[v + 20] = 1f;
            vertices[v + 21] = g.uMax; vertices[v + 22] = g.vMax; vertices[v + 23] = 0f;
            // bottom-left
            vertices[v + 24] = x0;    vertices[v + 25] = y1;
            vertices[v + 26] = 1f;    vertices[v + 27] = 1f;    vertices[v + 28] = 1f;
            vertices[v + 29] = g.uMin; vertices[v + 30] = g.vMax; vertices[v + 31] = 0f;

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
