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
 * <h1>Main graphic API<br></h1>
 * <br>
 * <h3>Normal Usage (Single shapes):</h3>
 * {@snippet :
 * g.drawRect(10, 10, 100, 50, 1f, 0f, 0f);
 * g.drawOval(200, 200, 30, 16, 0f, 1f, 0f);
 * g.drawLine(0, 0, 100, 100, 2f, 0f, 0f, 1f);
 * }
 * <br>
 * <h3>Batch Usage (Thousands of shapes):</h3>
 * For high-volume rendering (300k+ shapes per frame), wrap shapes in batch:
 * {@snippet :
 * g.beginBatch();
 * for (int i = 0; i < 300000; i++) {
 *     g.batchDrawOval(x[i], y[i], radius[i], 6, r[i], g[i], b[i]);
 * }
 * g.endBatch();
 * }
 * Batch methods use identical parameters to normal methods. Performance gain: +20-30% FPS on high volume.
 *
 * @since 1.0
 * @author Andrea Maruca
 */
@EngineCore
@SuppressWarnings("unused")
public abstract class NvGraphic implements AppendableGeometry {
    /**
     * Floats per vertex: {@code x, y, r, g, b, u, v, texIndex, a}.
     * Alpha is the last component so the shader reads it at {@code location = 4}.
     */
    public static final int FLOATS_PER_VERTEX = 9;
    private static final double TWO_PI = 2.0 * Math.PI;

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

    protected float r=0, g=0, b=0, a=1;

    protected float currentFontScale = 1.0f;

    private float cachedRotationRad = 0;
    private float cachedCos = 1;
    private float cachedSin = 0;
    private float lastCachedRotation = Float.NaN;

    private final float[] batchVertices = new float[65536 * FLOATS_PER_VERTEX];
    private final int[] batchIndices = new int[65536];
    private int batchVertexCount = 0;
    private int batchIndexCount = 0;
    private static final int BATCH_THRESHOLD = 32768;

    private boolean batchMode = false;

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
        this.batchVertexCount = 0;
        this.batchIndexCount = 0;
        this.batchMode = false;
        this.a = 1f;
    }

    public void setRGB(float r, float g, float b){
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public void setRGBA(float r, float g, float b, float alpha){
        setRGB(r, g, b);
        setTransparency(alpha);
    }

    public void setFontScale(float scale) {
        this.currentFontScale = scale;
    }

    /**
     * Sets the alpha applied to every shape, glyph and image emitted afterwards.
     * <p>
     * {@code 1} is fully opaque, {@code 0} fully invisible. The value is sticky exactly like
     * {@link #setRGB(float, float, float)} and is reset to {@code 1} at the start of each frame.
     * <p>
     * Blending happens in submission order, so a translucent shape only blends with what was
     * drawn <em>before</em> it. Components extending {@code NvStateless} cache their geometry:
     * call {@code invalidate()} on them after changing the alpha, otherwise the cached
     * (old-alpha) vertices keep being reused.
     *
     * @param alpha opacity, clamped to {@code [0, 1]}
     */
    public void setTransparency(float alpha){
        this.a = alpha < 0f ? 0f : (alpha > 1f ? 1f : alpha);
    }

    public float getTransparency(){
        return a;
    }

    public void setComponent(NvComp component){
        this.component = component;
    }

    public void applyTransformsToBatch(int vStart, int iStart) {
        if (component == null || component.rotation == 0) return;

        if (component.rotation != lastCachedRotation) {
            cachedRotationRad = (float) Math.toRadians(component.rotation);
            cachedCos = (float) Math.cos(cachedRotationRad);
            cachedSin = (float) Math.sin(cachedRotationRad);
            lastCachedRotation = component.rotation;
        }

        float compW = component.getW();
        float compH = component.getH();
        float compX = component.getX();
        float compY = component.getY();

        float pivotX, pivotY;
        if (component.isHUD()) {
            pivotX = compX + compW * component.pivotX;
            pivotY = compY + compH * component.pivotY;
        } else {
            pivotX = (compX + compW * component.pivotX - camera.x) * camera.zoom;
            pivotY = (compY + compH * component.pivotY - camera.y) * camera.zoom;
        }

        rotateVertArray(vStart, cachedCos, cachedSin, vertexFloatCount, vertices, pivotX, pivotY);
        rotateVertArray(iStart, cachedCos, cachedSin, imageVertexFloatCount, imageVertices, pivotX, pivotY);
    }

    private void rotateVertArray(int iStart, float cos, float sin, int vertexCount, float[] targetVertices, float pivotX, float pivotY) {
        for (int i = iStart; i < vertexCount; i += FLOATS_PER_VERTEX) {
            float vx = targetVertices[i];
            float vy = targetVertices[i+1];

            float dx = vx - pivotX;
            float dy = vy - pivotY;

            float rotatedVx = dx * cos - dy * sin;
            float rotatedVy = dx * sin + dy * cos;

            targetVertices[i]     = rotatedVx + pivotX;
            targetVertices[i + 1] = rotatedVy + pivotY;
        }
    }

    public int getVertexFloatCount() {
        return vertexFloatCount;
    }

    public int getIndexCount() {
        return indexCount;
    }

    public int getImageVertexFloatCount() {
        return imageVertexFloatCount;
    }

    public int getImageIndexCount() {
        return imageIndexCount;
    }

    @Override
    public void append(float[] newVertices, int[] newIndices) {
        append(newVertices, newVertices.length, newIndices, newIndices.length);
    }

    @Override
    public void append(float[] newVertices, int vertexFloatCount, int[] newIndices, int indexCount) {
        if (batchMode) {
            appendToBatch(newVertices, vertexFloatCount, newIndices, indexCount);
        } else {
            appendDirectly(newVertices, vertexFloatCount, newIndices, indexCount);
        }
    }

    private void appendDirectly(float[] newVertices, int vertexFloatCount, int[] newIndices, int indexCount) {
        int vertexOffset = this.vertexFloatCount / FLOATS_PER_VERTEX;

        ensureCapacity(this.vertexFloatCount + vertexFloatCount, this.indexCount + indexCount, false);

        System.arraycopy(newVertices, 0, vertices, this.vertexFloatCount, vertexFloatCount);

        for (int i = 0; i < indexCount; i++) {
            indices[this.indexCount + i] = newIndices[i] + vertexOffset;
        }

        this.vertexFloatCount += vertexFloatCount;
        this.indexCount += indexCount;
    }

    private void appendToBatch(float[] newVertices, int vertexFloatCount, int[] newIndices, int indexCount) {
        if (batchVertexCount + vertexFloatCount > BATCH_THRESHOLD) {
            flushBatch();
        }

        int vertexOffset = batchVertexCount / FLOATS_PER_VERTEX;

        System.arraycopy(newVertices, 0, batchVertices, batchVertexCount, vertexFloatCount);

        for (int i = 0; i < indexCount; i++) {
            batchIndices[batchIndexCount + i] = newIndices[i] + vertexOffset;
        }

        batchVertexCount += vertexFloatCount;
        batchIndexCount += indexCount;
    }

    public void beginBatch() {
        if (batchMode) return;
        batchMode = true;
        batchVertexCount = 0;
        batchIndexCount = 0;
    }

    public void endBatch() {
        if (!batchMode) return;
        flushBatch();
        batchMode = false;
    }

    private void flushBatch() {
        if (batchVertexCount == 0) return;
        appendDirectly(batchVertices, batchVertexCount, batchIndices, batchIndexCount);
        batchVertexCount = 0;
        batchIndexCount = 0;
    }

    public void batchDrawOval(float x, float y, float radius, int accuracy, float r, float g, float b, AppendableGeometry comp) {
        drawOval(x, y, radius, accuracy, r, g, b, comp);
    }

    public void batchDrawOval(float x, float y, float radius, int accuracy, float r, float g, float b) {
        batchDrawOval(x, y, radius, accuracy, r, g, b, this);
    }

    public void batchDrawOval(float x, float y, float radius, float r, float g, float b) {
        batchDrawOval(x, y, radius, 6, r, g, b, this);
    }

    public void batchDrawRect(float x, float y, float w, float h, float r, float g, float b, AppendableGeometry comp) {
        drawRect(x, y, w, h, r, g, b, comp);
    }

    public void batchDrawRect(float x, float y, float w, float h, float r, float g, float b) {
        batchDrawRect(x, y, w, h, r, g, b, this);
    }

    public void batchDrawRect(float x, float y, float w, float h) {
        batchDrawRect(x, y, w, h, r, g, b, this);
    }

    public void batchDrawLine(float x1, float y1, float x2, float y2, float thickness, float r, float g, float b, AppendableGeometry comp) {
        drawLine(x1, y1, x2, y2, thickness, r, g, b, comp);
    }

    public void batchDrawLine(float x1, float y1, float x2, float y2, float thickness, float r, float g, float b) {
        batchDrawLine(x1, y1, x2, y2, thickness, r, g, b, this);
    }

    public void batchDrawLine(float x1, float y1, float x2, float y2, float thickness) {
        batchDrawLine(x1, y1, x2, y2, thickness, r, g, b, this);
    }

    public void batchDrawImage(NvImage image, float x, float y, float w, float h) {
        drawImage(image, x, y, w, h);
    }

    protected void appendImageGeometry(float[] newVertices, int[] newIndices) {
        appendImageGeometry(newVertices, newVertices.length, newIndices, newIndices.length);
    }

    protected void appendImageGeometry(float[] newVertices, int vertexFloatCount, int[] newIndices, int indexCount) {
        int vertexOffset = imageVertexFloatCount / FLOATS_PER_VERTEX;

        ensureCapacity(this.vertexFloatCount, this.indexCount, true);

        System.arraycopy(newVertices, 0, imageVertices, imageVertexFloatCount, vertexFloatCount);

        for (int i = 0; i < indexCount; i++) {
            imageIndices[imageIndexCount + i] = newIndices[i] + vertexOffset;
        }

        imageVertexFloatCount += vertexFloatCount;
        imageIndexCount += indexCount;
    }

    private void ensureCapacity(int requiredVertexFloats, int requiredIndices, boolean isImage) {
        if (isImage) {
            if (imageVertexFloatCount + requiredVertexFloats > imageVertices.length) {
                int newCapacity = imageVertices.length;
                while (newCapacity < imageVertexFloatCount + requiredVertexFloats) {
                    newCapacity *= 2;
                }
                imageVertices = Arrays.copyOf(imageVertices, newCapacity);
            }
            if (imageIndexCount + requiredIndices > imageIndices.length) {
                int newCapacity = imageIndices.length;
                while (newCapacity < imageIndexCount + requiredIndices) {
                    newCapacity *= 2;
                }
                imageIndices = Arrays.copyOf(imageIndices, newCapacity);
            }
        } else {
            if (requiredVertexFloats > vertices.length) {
                int newCapacity = vertices.length;
                while (newCapacity < requiredVertexFloats) {
                    newCapacity *= 2;
                }
                vertices = Arrays.copyOf(vertices, newCapacity);
            }
            if (requiredIndices > indices.length) {
                int newCapacity = indices.length;
                while (newCapacity < requiredIndices) {
                    newCapacity *= 2;
                }
                indices = Arrays.copyOf(indices, newCapacity);
            }
        }
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

    public abstract void drawLine(float x1, float y1, float x2, float y2, float thickness, float r, float g, float b, AppendableGeometry comp);

    public void drawLine(float x1, float y1, float x2, float y2, float thickness){
        drawLine(x1, y1, x2, y2, thickness, r, g, b, this);
    }

    public void drawLine(float x1, float y1, float x2, float y2, float thickness, float r, float g, float b){
        drawLine(x1, y1, x2, y2, thickness, r, g, b, this);
    }

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

    public abstract void drawPolygon(float[] vertices, int[] indices, float[] colors, AppendableGeometry comp);

    public void drawPolygon(float[] vertices, int[] indices, float[] colors) {
        drawPolygon(vertices, indices, colors, this);
    }

    public void drawPolygon(float[] vertices, int[] indices, AppendableGeometry comp) {
        drawPolygon(vertices, indices, null, comp);
    }

    public void drawPolygon(float[] vertices, int[] indices) {
        drawPolygon(vertices, indices, null, this);
    }

    public abstract void drawOval(float x, float y, float radius, int accuracy, float r, float g, float b, AppendableGeometry comp);
    public void drawOval(float x, float y, float radius, int accuracy){
        drawOval(x, y, radius, accuracy, r, g, b, this);
    };
    public void drawOval(float x, float y, float radius, float r, float g, float b){
        drawOval(x, y, radius, 16, r, g, b, this);
    };
    public void drawOval(float x, float y, float radius, int accuracy, float r, float g, float b){
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

    public abstract void drawText(String text, float textX, float textY, float fontScale, AppendableGeometry comp);
    public void drawText(String text, float textX, float textY) {
        drawText(text, textX, textY, 1.0f, this);
    }
    public void drawText(String text, float textX, float textY, AppendableGeometry comp) {
        drawText(text, textX, textY, 1.0f, comp);
    }
    public void drawText(String text, float textX, float textY, float fontScale) {
        drawText(text, textX, textY, fontScale, this);
    }

    public abstract void drawImage(NvImage image, float x, float y, float w, float h);

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

    public void copyVerticesTo(float[] target, int targetOffset) {
        System.arraycopy(vertices, 0, target, targetOffset, vertexFloatCount);
    }

    public void copyIndicesTo(int[] target, int targetOffset) {
        System.arraycopy(indices, 0, target, targetOffset, indexCount);
    }

    public void copyImageVerticesTo(float[] target, int targetOffset) {
        System.arraycopy(imageVertices, 0, target, targetOffset, imageVertexFloatCount);
    }

    public void copyImageIndicesTo(int[] target, int targetOffset, int vertexOffset) {
        for (int i = 0; i < imageIndexCount; i++) {
            target[targetOffset + i] = imageIndices[i] + vertexOffset;
        }
    }

    public static Scene generateTextGeometry(String text, float startX, float startY, FontAtlas atlas, float r, float g, float b) {
        return generateTextGeometry(text, startX, startY, atlas, r, g, b, 1f);
    }

    public static Scene generateTextGeometry(String text, float startX, float startY, FontAtlas atlas, float r, float g, float b, float a) {
        int n = text.length();
        float[] vertices = new float[n * 4 * FLOATS_PER_VERTEX];
        int[] indices  = new int[n * 6];
        float cursorX = startX;

        for (int i = 0; i < n; i++) {
            FontAtlas.Glyph glyph = atlas.getGlyph(text.charAt(i));

            float x0 = cursorX,            y0 = startY;
            float x1 = cursorX + glyph.width,  y1 = startY + glyph.height;

            int v = i * 4 * FLOATS_PER_VERTEX;
            vertices[v     ] = x0;    vertices[v +  1] = y0;
            vertices[v +  2] = r;    vertices[v +  3] = g;    vertices[v +  4] = b;
            vertices[v +  5] = glyph.uMin; vertices[v +  6] = glyph.vMin; vertices[v +  7] = 0f;
            vertices[v +  8] = a;
            vertices[v +  9] = x1;    vertices[v + 10] = y0;
            vertices[v + 11] = r;    vertices[v + 12] = g;    vertices[v + 13] = b;
            vertices[v + 14] = glyph.uMax; vertices[v + 15] = glyph.vMin; vertices[v + 16] = 0f;
            vertices[v + 17] = a;
            vertices[v + 18] = x1;    vertices[v + 19] = y1;
            vertices[v + 20] = r;    vertices[v + 21] = g;    vertices[v + 22] = b;
            vertices[v + 23] = glyph.uMax; vertices[v + 24] = glyph.vMax; vertices[v + 25] = 0f;
            vertices[v + 26] = a;
            vertices[v + 27] = x0;    vertices[v + 28] = y1;
            vertices[v + 29] = r;    vertices[v + 30] = g;    vertices[v + 31] = b;
            vertices[v + 32] = glyph.uMin; vertices[v + 33] = glyph.vMax; vertices[v + 34] = 0f;
            vertices[v + 35] = a;

            int idx = i * 6, base = i * 4;
            indices[idx]     = base;
            indices[idx + 1] = (base + 1);
            indices[idx + 2] = (base + 2);
            indices[idx + 3] = (base + 2);
            indices[idx + 4] = (base + 3);
            indices[idx + 5] = base;

            cursorX += glyph.advance;
        }
        return new Scene(vertices, indices);
    }

    public float getRenderWidth() {
        return w;
    }

    public float getRenderHeight() {
        return h;
    }
}