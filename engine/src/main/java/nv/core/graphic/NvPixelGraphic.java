package nv.core.graphic;

import nv.core.AppendableGeometry;
import nv.core.Scene;
import nv.core.annotations.DefaultChose;
import nv.core.data.NvImage;

import java.util.Arrays;

/**
 * concrete implementation of NvGraphic
 * that specializes in rendering geometric shapes and text as direct pixel graphics.
 */
@DefaultChose
@SuppressWarnings("unused")
public class NvPixelGraphic extends NvGraphic {

    private static final int[] TRIANGLE_INDICES = {0, 1, 2};
    private static final int[] QUAD_INDICES = {0, 1, 2, 2, 3, 0};
    private final float[] triangleVertices = new float[3 * FLOATS_PER_VERTEX];
    private final float[] quadVertices = new float[4 * FLOATS_PER_VERTEX];
    private float[] dynamicVertices = new float[0];
    private int[] dynamicIndices = new int[0];

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

    /** Updated in 1.6. */
    @Override
    public void drawTri(float base1, float base2, float y,
                        float r, float g, float b,
                        AppendableGeometry comp) {

        float x1 = tx(component.getX() + base1);
        float x2 = tx(component.getX() + base2);
        float y1 = ty(component.getY() + y);
        float apexY = ty(component.getY() + y - component.getH());

        setVertex(triangleVertices, 0, x1, y1, r, g, b, wu, wv, 0f);
        setVertex(triangleVertices, 1, x2, y1, r, g, b, wu, wv, 0f);
        setVertex(triangleVertices, 2, (x1 + x2) * 0.5f, apexY, r, g, b, wu, wv, 0f);
        comp.append(triangleVertices, TRIANGLE_INDICES);
    }

    /** Updated in 1.6. */
    @Override
    public void drawPolygon(float[] vertices, int[] indices, float[] colors, AppendableGeometry comp) {
        int numVertices = vertices.length / 2;
        int vertexFloatCount = numVertices * FLOATS_PER_VERTEX;
        ensureDynamicCapacity(vertexFloatCount, indices.length);

        for (int i = 0; i < numVertices; i++) {
            float vx = tx(component.getX() + vertices[i * 2]);
            float vy = ty(component.getY() + vertices[i * 2 + 1]);

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

            setVertex(dynamicVertices, i, vx, vy, vr, vg, vb, wu, wv, 0f);
        }

        comp.append(dynamicVertices, vertexFloatCount, indices, indices.length);
    }

    /** Updated in 1.6. */
    @Override
    public void drawOval(float x, float y, float radius, int accuracy,
                         float r, float g, float b,
                         AppendableGeometry comp) {

        int vertexFloatCount = (accuracy + 1) * FLOATS_PER_VERTEX;
        int indexCount = accuracy * 3;
        ensureDynamicCapacity(vertexFloatCount, indexCount);

        x += radius/2;
        y += radius/2;

        float cx = tx(component.getX() + x);
        float cy = ty(component.getY() + y);

        float rScaled = radius * camera.zoom;

        // center
        setVertex(dynamicVertices, 0, cx, cy, r, g, b, wu, wv, 0f);

        for (int i = 0; i < accuracy; i++) {

            float angle = (float) (i * 2 * Math.PI / accuracy);

            int vi = i + 1;
            float lx = (float) Math.cos(angle) * rScaled;
            float ly = (float) Math.sin(angle) * rScaled;
            setVertex(dynamicVertices, vi, cx + lx, cy + ly, r, g, b, wu, wv, 0f);

            int idx = i * 3;
            int cur = i + 1;
            int next = (i + 1) % accuracy + 1;

            dynamicIndices[idx] = 0;
            dynamicIndices[idx + 1] = cur;
            dynamicIndices[idx + 2] = next;
        }

        comp.append(dynamicVertices, vertexFloatCount, dynamicIndices, indexCount);
    }

    /** Updated in 1.6. */
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
        x1 = tx(component.getX() + x1);
        y1 = ty(component.getY() + y1);
        x2 = tx(component.getX() + x2);
        y2 = ty(component.getY() + y2);

        float dx = x2 - x1;
        float dy = y2 - y1;

        float length = (float)Math.sqrt(dx * dx + dy * dy);

        if(length == 0)
            return;

        float nx = -dy / length;
        float ny = dx / length;


        float half = thickness * 0.5f;

        nx *= half;
        ny *= half;

        setVertex(quadVertices, 0, x1 + nx, y1 + ny, r, g, b, wu, wv, a);
        setVertex(quadVertices, 1, x2 + nx, y2 + ny, r, g, b, wu, wv, a);
        setVertex(quadVertices, 2, x2 - nx, y2 - ny, r, g, b, wu, wv, a);
        setVertex(quadVertices, 3, x1 - nx, y1 - ny, r, g, b, wu, wv, a);
        comp.append(quadVertices, QUAD_INDICES);
    }

    /** Updated in 1.6. */
    @Override
    public void drawRect(float x, float y, float w, float h,
                         float r, float g, float b,
                         AppendableGeometry comp) {

        float x1 = tx(component.getX() + x);
        float y1 = ty(component.getY() + y);
        float x2 = tx(component.getX() + x + w);
        float y2 = ty(component.getY() + y + h);

        setVertex(quadVertices, 0, x1, y1, r, g, b, wu, wv, a);
        setVertex(quadVertices, 1, x2, y1, r, g, b, wu, wv, a);
        setVertex(quadVertices, 2, x2, y2, r, g, b, wu, wv, a);
        setVertex(quadVertices, 3, x1, y2, r, g, b, wu, wv, a);
        comp.append(quadVertices, QUAD_INDICES);
    }

    @Override
    public void drawRoundRect(float x, float y, float w, float h, float radius, float r, float g, float b, AppendableGeometry comp) {
        int segments = 8;

        float x1 = tx(component.getX() + x);
        float y1 = ty(component.getY() + y);
        float x2 = tx(component.getX() + x + w);
        float y2 = ty(component.getY() + y + h);
        float rScaled = radius * camera.zoom;

        float maxR = Math.min(w, h) / 2f * camera.zoom;
        if (rScaled > maxR) rScaled = maxR;

        int numVerts = 4 + 4 * (segments + 1);
        float[] verts = new float[numVerts * FLOATS_PER_VERTEX];

        float[][] corners = {
                {x1 + rScaled, y1 + rScaled}, // Top-Left
                {x2 - rScaled, y1 + rScaled}, // Top-Right
                {x2 - rScaled, y2 - rScaled}, // Bottom-Right
                {x1 + rScaled, y2 - rScaled}  // Bottom-Left
        };

        int numIndices = 30 + 12 * segments;
        int[] inds = new int[numIndices];

        int vIdx = 0;
        float[] inner = {
                x1 + rScaled, y1 + rScaled,
                x2 - rScaled, y1 + rScaled,
                x2 - rScaled, y2 - rScaled,
                x1 + rScaled, y2 - rScaled
        };

        for(int i=0; i<4; i++) {
            int off = vIdx * 8;
            verts[off] = inner[i*2];
            verts[off + 1] = inner[i*2+1];
            verts[off + 2] = r;
            verts[off + 3] = g;
            verts[off + 4] = b;
            verts[off + 5] = wu;
            verts[off + 6] = wv;
            verts[off + 7] = a;
            vIdx++;
        }

        int iIdx = 0;
        inds[iIdx++] = 0; inds[iIdx++] = 1; inds[iIdx++] = 2;
        inds[iIdx++] = 2; inds[iIdx++] = 3; inds[iIdx++] = 0;

        for(int c=0; c<4; c++) {
            float startAngle = (float) (Math.PI + c * Math.PI/2);
            int cornerCenterIdx = c;

            for(int s=0; s<=segments; s++) {
                float angle = startAngle + (float)(s * (Math.PI/2) / segments);
                int off = vIdx * 8;
                verts[off]     = corners[c][0] + (float)Math.cos(angle) * rScaled;
                verts[off + 1] = corners[c][1] + (float)Math.sin(angle) * rScaled;
                verts[off + 2] = r;
                verts[off + 3] = g;
                verts[off + 4] = b;
                verts[off + 5] = wu;
                verts[off + 6] = wv;
                verts[off + 7] = a;

                if(s > 0) {
                    inds[iIdx++] = cornerCenterIdx;
                    inds[iIdx++] = vIdx - 1;
                    inds[iIdx++] = vIdx;
                }
                vIdx++;
            }
        }

        // Side rectangles
        int a0_end = 4 + segments;
        int a1_start = 4 + segments + 1;
        inds[iIdx++] = 0; inds[iIdx++] = 1; inds[iIdx++] = a1_start;
        inds[iIdx++] = a1_start; inds[iIdx++] = a0_end; inds[iIdx++] = 0;

        int a1_end = 4 + 2*segments + 1;
        int a2_start = 4 + 2*segments + 2;
        inds[iIdx++] = 1; inds[iIdx++] = 2; inds[iIdx++] = a2_start;
        inds[iIdx++] = a2_start; inds[iIdx++] = a1_end; inds[iIdx++] = 1;

        int a2_end = 4 + 3*segments + 2;
        int a3_start = 4 + 3*segments + 3;
        inds[iIdx++] = 2; inds[iIdx++] = 3; inds[iIdx++] = a3_start;
        inds[iIdx++] = a3_start; inds[iIdx++] = a2_end; inds[iIdx++] = 2;

        int a3_end = 4 + 4*segments + 3;
        int a0_start = 4;
        inds[iIdx++] = 3; inds[iIdx++] = 0; inds[iIdx++] = a0_start;
        inds[iIdx++] = a0_start; inds[iIdx++] = a3_end; inds[iIdx++] = 3;

        comp.append(verts, inds);
    }

    /** Updated in 1.6. */
    @Override
    public void drawText(String text, float textX, float textY,
                         AppendableGeometry comp) {

        int charCount = text.length();
        int vertexFloatCount = charCount * 4 * FLOATS_PER_VERTEX;
        int indexCount = charCount * 6;
        ensureDynamicCapacity(vertexFloatCount, indexCount);

        float cursorX = tx(component.getX() + textX);
        float startY = ty(component.getY() + textY);
        for (int i = 0; i < charCount; i++) {
            var glyph = fontAtlas.getGlyph(text.charAt(i));
            float x0 = cursorX;
            float x1 = cursorX + glyph.width;
            float y1 = startY + glyph.height;
            int vertex = i * 4;
            setVertex(dynamicVertices, vertex, x0, startY, r, g, b, glyph.uMin, glyph.vMin, 0f);
            setVertex(dynamicVertices, vertex + 1, x1, startY, r, g, b, glyph.uMax, glyph.vMin, 0f);
            setVertex(dynamicVertices, vertex + 2, x1, y1, r, g, b, glyph.uMax, glyph.vMax, 0f);
            setVertex(dynamicVertices, vertex + 3, x0, y1, r, g, b, glyph.uMin, glyph.vMax, 0f);

            int index = i * 6;
            dynamicIndices[index] = vertex;
            dynamicIndices[index + 1] = vertex + 1;
            dynamicIndices[index + 2] = vertex + 2;
            dynamicIndices[index + 3] = vertex + 2;
            dynamicIndices[index + 4] = vertex + 3;
            dynamicIndices[index + 5] = vertex;
            cursorX += glyph.advance;
        }
        comp.append(dynamicVertices, vertexFloatCount, dynamicIndices, indexCount);
    }

    @Override
    public void drawImage(NvImage image, float x, float y, float w, float h) {
        drawImageRegion(image, x, y, w, h, 0.0f, 0.0f, 1.0f, 1.0f);
    }

    /** Updated in 1.6. */
    @Override
    public void drawImageRegion(NvImage image,
                                float x, float y, float w, float h,
                                float u0, float v0, float u1, float v1) {

        float x1 = tx(component.getX() + x);
        float y1 = ty(component.getY() + y);
        float x2 = tx(component.getX() + x + w);
        float y2 = ty(component.getY() + y + h);

        float texIndex = (float) image.getTextureIndex();

        float dr = r, dg = g, db = b;
        if (dr == 0 && dg == 0 && db == 0) {
            dr = dg = db = 1f;
        }

        setVertex(quadVertices, 0, x1, y1, dr, dg, db, u0, v0, texIndex);
        setVertex(quadVertices, 1, x2, y1, dr, dg, db, u1, v0, texIndex);
        setVertex(quadVertices, 2, x2, y2, dr, dg, db, u1, v1, texIndex);
        setVertex(quadVertices, 3, x1, y2, dr, dg, db, u0, v1, texIndex);
        appendImageGeometry(quadVertices, QUAD_INDICES);
    }

    /** @since 1.6 */
    private static void setVertex(float[] target, int vertexIndex,
                                  float x, float y, float r, float g, float b,
                                  float u, float v, float textureIndex) {
        int offset = vertexIndex * FLOATS_PER_VERTEX;
        target[offset] = x;
        target[offset + 1] = y;
        target[offset + 2] = r;
        target[offset + 3] = g;
        target[offset + 4] = b;
        target[offset + 5] = u;
        target[offset + 6] = v;
        target[offset + 7] = textureIndex;
    }

    /** @since 1.6 */
    private void ensureDynamicCapacity(int vertexFloatCount, int indexCount) {
        if (dynamicVertices.length < vertexFloatCount) {
            dynamicVertices = Arrays.copyOf(dynamicVertices, vertexFloatCount);
        }
        if (dynamicIndices.length < indexCount) {
            dynamicIndices = Arrays.copyOf(dynamicIndices, indexCount);
        }
    }
}
