package nv.core.graphic;

import nv.core.AppendableGeometry;
import nv.core.annotations.DefaultChose;
import nv.core.annotations.EngineCore;
import nv.core.data.NvImage;

import java.util.Arrays;

/**
 * Default implementation of {@link NvGraphic} that uses pixel as coordinates.
 * @since 1.0
 * @author Andrea Maruca
 */
@DefaultChose
@EngineCore
@SuppressWarnings("unused")
public class NvPixelGraphic extends NvGraphic {

    private static final int[] TRIANGLE_INDICES = {0, 1, 2};
    private static final int[] QUAD_INDICES = {0, 1, 2, 2, 3, 0};
    private static final double TWO_PI = 2.0 * Math.PI;

    private final float[] triangleVertices = new float[3 * FLOATS_PER_VERTEX];
    private final float[] quadVertices = new float[4 * FLOATS_PER_VERTEX];
    private float[] dynamicVertices = new float[0];
    private int[] dynamicIndices = new int[0];

    private float cachedCompX;
    private float cachedCompY;
    private float cachedCompW;
    private float cachedCompH;
    private boolean cachedIsHUD;
    private float cachedTransformX;
    private float cachedTransformY;

    private void cacheComponentTransforms() {
        if (component == null) return;
        cachedCompX = component.getX();
        cachedCompY = component.getY();
        cachedCompW = component.getW();
        cachedCompH = component.getH();
        cachedIsHUD = component.isHUD();
        if (cachedIsHUD) {
            cachedTransformX = 0;
            cachedTransformY = 0;
        } else {
            cachedTransformX = camera.x;
            cachedTransformY = camera.y;
        }
    }

    private float tx(float worldX) {
        if (cachedIsHUD)
            return worldX;
        return (worldX - cachedTransformX) * camera.zoom;
    }

    private float ty(float worldY) {
        if (cachedIsHUD)
            return worldY;
        return (worldY - cachedTransformY) * camera.zoom;
    }

    @Override
    public void drawTri(float base1, float base2, float y,
                        float r, float g, float b,
                        AppendableGeometry comp) {

        cacheComponentTransforms();

        float x1 = tx(cachedCompX + base1);
        float x2 = tx(cachedCompX + base2);
        float y1 = ty(cachedCompY + y);
        float apexY = ty(cachedCompY + y - cachedCompH);

        setVertex(triangleVertices, 0, x1, y1, r, g, b, wu, wv, 0f, a);
        setVertex(triangleVertices, 1, x2, y1, r, g, b, wu, wv, 0f, a);
        setVertex(triangleVertices, 2, (x1 + x2) * 0.5f, apexY, r, g, b, wu, wv, 0f, a);
        comp.append(triangleVertices, TRIANGLE_INDICES);
    }

    @Override
    public void drawPolygon(float[] vertices, int[] indices, float[] colors, AppendableGeometry comp) {
        cacheComponentTransforms();

        int numVertices = vertices.length / 2;
        int vertexFloatCount = numVertices * FLOATS_PER_VERTEX;
        ensureDynamicCapacity(vertexFloatCount, indices.length);

        for (int i = 0; i < numVertices; i++) {
            float vx = tx(cachedCompX + vertices[i * 2]);
            float vy = ty(cachedCompY + vertices[i * 2 + 1]);

            float vr = r, vg = g, vb = b;
            if (colors != null) {
                if (colors.length >= numVertices * 4) {
                    vr = colors[i * 4];
                    vg = colors[i * 4 + 1];
                    vb = colors[i * 4 + 2];
                } else if (colors.length >= numVertices * 3) {
                    vr = colors[i * 3];
                    vg = colors[i * 3 + 1];
                    vb = colors[i * 3 + 2];
                } else if (colors.length == 4 || colors.length == 3) {
                    vr = colors[0];
                    vg = colors[1];
                    vb = colors[2];
                }
            }

            setVertex(dynamicVertices, i, vx, vy, vr, vg, vb, wu, wv, 0f, a);
        }

        comp.append(dynamicVertices, vertexFloatCount, indices, indices.length);
    }

    @Override
    public void drawOval(float x, float y, float radius, int accuracy,
                         float r, float g, float b,
                         AppendableGeometry comp) {

        cacheComponentTransforms();

        int vertexFloatCount = (accuracy + 1) * FLOATS_PER_VERTEX;
        int indexCount = accuracy * 3;
        ensureDynamicCapacity(vertexFloatCount, indexCount);

        float cx = tx(cachedCompX + x);
        float cy = ty(cachedCompY + y);

        float rScaled = cachedIsHUD ? radius : radius * camera.zoom;
        setVertex(dynamicVertices, 0, cx, cy, r, g, b, wu, wv, 0f, a);

        for (int i = 0; i < accuracy; i++) {
            float angle = (float) (i * TWO_PI / accuracy);

            int vi = i + 1;
            float lx = (float) Math.cos(angle) * rScaled;
            float ly = (float) Math.sin(angle) * rScaled;
            setVertex(dynamicVertices, vi, cx + lx, cy + ly, r, g, b, wu, wv, 0f, a);

            int idx = i * 3;
            int cur = i + 1;
            int next = (i + 1) % accuracy + 1;

            dynamicIndices[idx] = 0;
            dynamicIndices[idx + 1] = cur;
            dynamicIndices[idx + 2] = next;
        }

        comp.append(dynamicVertices, vertexFloatCount, dynamicIndices, indexCount);
    }

    @Override
    public void drawLine(
            float x1,
            float y1,
            float x2,
            float y2,
            float thickness,
            float r,
            float g,
            float b,
            AppendableGeometry comp
    ) {
        cacheComponentTransforms();

        x1 = tx(cachedCompX + x1);
        y1 = ty(cachedCompY + y1);
        x2 = tx(cachedCompX + x2);
        y2 = ty(cachedCompY + y2);

        float dx = x2 - x1;
        float dy = y2 - y1;

        float length = (float)Math.sqrt(dx * dx + dy * dy);

        if(length == 0)
            return;

        float nx = -dy / length;
        float ny = dx / length;

        float half = (cachedIsHUD ? thickness : thickness * camera.zoom) * 0.5f;

        nx *= half;
        ny *= half;

        setVertex(quadVertices, 0, x1 + nx, y1 + ny, r, g, b, wu, wv, 0f, a);
        setVertex(quadVertices, 1, x2 + nx, y2 + ny, r, g, b, wu, wv, 0f, a);
        setVertex(quadVertices, 2, x2 - nx, y2 - ny, r, g, b, wu, wv, 0f, a);
        setVertex(quadVertices, 3, x1 - nx, y1 - ny, r, g, b, wu, wv, 0f, a);
        comp.append(quadVertices, QUAD_INDICES);
    }

    @Override
    public void drawRect(float x, float y, float w, float h,
                         float r, float g, float b,
                         AppendableGeometry comp) {

        cacheComponentTransforms();

        float x1 = tx(cachedCompX + x);
        float y1 = ty(cachedCompY + y);
        float x2 = tx(cachedCompX + x + w);
        float y2 = ty(cachedCompY + y + h);

        setVertex(quadVertices, 0, x1, y1, r, g, b, wu, wv, 0f, a);
        setVertex(quadVertices, 1, x2, y1, r, g, b, wu, wv, 0f, a);
        setVertex(quadVertices, 2, x2, y2, r, g, b, wu, wv, 0f, a);
        setVertex(quadVertices, 3, x1, y2, r, g, b, wu, wv, 0f, a);
        comp.append(quadVertices, QUAD_INDICES);
    }

    @Override
    public void drawRoundRect(float x, float y, float w, float h, float radius, float r, float g, float b, AppendableGeometry comp) {
        cacheComponentTransforms();

        int segments = 8;

        float x1 = tx(cachedCompX + x);
        float y1 = ty(cachedCompY + y);
        float x2 = tx(cachedCompX + x + w);
        float y2 = ty(cachedCompY + y + h);
        float rScaled = cachedIsHUD ? radius : radius * camera.zoom;

        float maxR = Math.min(w, h) / 2f * (cachedIsHUD ? 1f : camera.zoom);
        if (rScaled > maxR) rScaled = maxR;

        int numVerts = 4 + 4 * (segments + 1);
        int vertexFloatCount = numVerts * FLOATS_PER_VERTEX;
        int numIndices = 30 + 12 * segments;

        ensureDynamicCapacity(vertexFloatCount, numIndices);

        float cx0 = x1 + rScaled, cy0 = y1 + rScaled;
        float cx1 = x2 - rScaled, cy1 = y1 + rScaled;
        float cx2 = x2 - rScaled, cy2 = y2 - rScaled;
        float cx3 = x1 + rScaled, cy3 = y2 - rScaled;

        setVertex(dynamicVertices, 0, cx0, cy0, r, g, b, wu, wv, 0f, a);
        setVertex(dynamicVertices, 1, cx1, cy1, r, g, b, wu, wv, 0f, a);
        setVertex(dynamicVertices, 2, cx2, cy2, r, g, b, wu, wv, 0f, a);
        setVertex(dynamicVertices, 3, cx3, cy3, r, g, b, wu, wv, 0f, a);

        int vIdx = 4;
        int iIdx = 0;
        dynamicIndices[iIdx++] = 0; dynamicIndices[iIdx++] = 1; dynamicIndices[iIdx++] = 2;
        dynamicIndices[iIdx++] = 2; dynamicIndices[iIdx++] = 3; dynamicIndices[iIdx++] = 0;

        for (int c = 0; c < 4; c++) {
            float cornerX = (c == 0 || c == 3) ? cx0 : cx1;
            float cornerY = (c == 0 || c == 1) ? cy0 : cy2;

            float startAngle = (float) (Math.PI + c * Math.PI / 2);
            int cornerCenterIdx = c;

            for (int s = 0; s <= segments; s++) {
                float angle = startAngle + (float) (s * (Math.PI / 2) / segments);
                float vx = cornerX + (float) Math.cos(angle) * rScaled;
                float vy = cornerY + (float) Math.sin(angle) * rScaled;
                setVertex(dynamicVertices, vIdx, vx, vy, r, g, b, wu, wv, 0f, a);

                if (s > 0) {
                    dynamicIndices[iIdx++] = cornerCenterIdx;
                    dynamicIndices[iIdx++] = vIdx - 1;
                    dynamicIndices[iIdx++] = vIdx;
                }
                vIdx++;
            }
        }

        int a0_end = 4 + segments;
        int a1_start = 4 + segments + 1;
        dynamicIndices[iIdx++] = 0; dynamicIndices[iIdx++] = 1; dynamicIndices[iIdx++] = a1_start;
        dynamicIndices[iIdx++] = a1_start; dynamicIndices[iIdx++] = a0_end; dynamicIndices[iIdx++] = 0;

        int a1_end = 4 + 2 * segments + 1;
        int a2_start = 4 + 2 * segments + 2;
        dynamicIndices[iIdx++] = 1; dynamicIndices[iIdx++] = 2; dynamicIndices[iIdx++] = a2_start;
        dynamicIndices[iIdx++] = a2_start; dynamicIndices[iIdx++] = a1_end; dynamicIndices[iIdx++] = 1;

        int a2_end = 4 + 3 * segments + 2;
        int a3_start = 4 + 3 * segments + 3;
        dynamicIndices[iIdx++] = 2; dynamicIndices[iIdx++] = 3; dynamicIndices[iIdx++] = a3_start;
        dynamicIndices[iIdx++] = a3_start; dynamicIndices[iIdx++] = a2_end; dynamicIndices[iIdx++] = 2;

        int a3_end = 4 + 4 * segments + 3;
        int a0_start = 4;
        dynamicIndices[iIdx++] = 3; dynamicIndices[iIdx++] = 0; dynamicIndices[iIdx++] = a0_start;
        dynamicIndices[iIdx++] = a0_start; dynamicIndices[iIdx++] = a3_end; dynamicIndices[iIdx++] = 3;

        comp.append(dynamicVertices, vertexFloatCount, dynamicIndices, numIndices);
    }

    @Override
    public void drawText(String text, float textX, float textY, float fontScale,
                         AppendableGeometry comp) {

        cacheComponentTransforms();

        int charCount = text.length();
        int vertexFloatCount = charCount * 4 * FLOATS_PER_VERTEX;
        int indexCount = charCount * 6;
        ensureDynamicCapacity(vertexFloatCount, indexCount);

        float cursorX = tx(cachedCompX + textX);
        float startY = ty(cachedCompY + textY);

        float effectiveScale = cachedIsHUD ? fontScale : fontScale * camera.zoom;

        for (int i = 0; i < charCount; i++) {
            var glyph = fontAtlas.getGlyph(text.charAt(i));
            float scaledWidth = glyph.width * effectiveScale;
            float scaledHeight = glyph.height * effectiveScale;
            float x0 = cursorX;
            float x1 = cursorX + scaledWidth;
            float y1 = startY + scaledHeight;
            int vertex = i * 4;
            setVertex(dynamicVertices, vertex, x0, startY, r, g, b, glyph.uMin, glyph.vMin, 0f, a);
            setVertex(dynamicVertices, vertex + 1, x1, startY, r, g, b, glyph.uMax, glyph.vMin, 0f, a);
            setVertex(dynamicVertices, vertex + 2, x1, y1, r, g, b, glyph.uMax, glyph.vMax, 0f, a);
            setVertex(dynamicVertices, vertex + 3, x0, y1, r, g, b, glyph.uMin, glyph.vMax, 0f, a);

            int index = i * 6;
            dynamicIndices[index] = vertex;
            dynamicIndices[index + 1] = vertex + 1;
            dynamicIndices[index + 2] = vertex + 2;
            dynamicIndices[index + 3] = vertex + 2;
            dynamicIndices[index + 4] = vertex + 3;
            dynamicIndices[index + 5] = vertex;
            cursorX += glyph.advance * effectiveScale;
        }
        comp.append(dynamicVertices, vertexFloatCount, dynamicIndices, indexCount);
    }

    @Override
    public void drawImage(NvImage image, float x, float y, float w, float h) {
        drawImageRegion(image, x, y, w, h, 0.0f, 0.0f, 1.0f, 1.0f);
    }

    @Override
    public void drawImageRegion(NvImage image,
                                float x, float y, float w, float h,
                                float u0, float v0, float u1, float v1) {

        cacheComponentTransforms();

        float x1 = tx(cachedCompX + x);
        float y1 = ty(cachedCompY + y);
        float x2 = tx(cachedCompX + x + w);
        float y2 = ty(cachedCompY + y + h);

        float texIndex = (float) image.getTextureIndex();

        float dr = r, dg = g, db = b;
        if (dr == 0 && dg == 0 && db == 0) {
            dr = dg = db = 1f;
        }

        setVertex(quadVertices, 0, x1, y1, dr, dg, db, u0, v0, texIndex, a);
        setVertex(quadVertices, 1, x2, y1, dr, dg, db, u1, v0, texIndex, a);
        setVertex(quadVertices, 2, x2, y2, dr, dg, db, u1, v1, texIndex, a);
        setVertex(quadVertices, 3, x1, y2, dr, dg, db, u0, v1, texIndex, a);
        appendImageGeometry(quadVertices, QUAD_INDICES);
    }

    private static void setVertex(float[] target, int vertexIndex,
                                  float x, float y, float r, float g, float b,
                                  float u, float v, float textureIndex, float alpha) {
        int offset = vertexIndex * FLOATS_PER_VERTEX;
        target[offset] = x;
        target[offset + 1] = y;
        target[offset + 2] = r;
        target[offset + 3] = g;
        target[offset + 4] = b;
        target[offset + 5] = u;
        target[offset + 6] = v;
        target[offset + 7] = textureIndex;
        target[offset + 8] = alpha;
    }

    private void ensureDynamicCapacity(int vertexFloatCount, int indexCount) {
        if (dynamicVertices.length < vertexFloatCount) {
            dynamicVertices = Arrays.copyOf(dynamicVertices, vertexFloatCount);
        }
        if (dynamicIndices.length < indexCount) {
            dynamicIndices = Arrays.copyOf(dynamicIndices, indexCount);
        }
    }
}