package nv.core.graphic;

import nv.core.AppendableGeometry;
import nv.core.Scene;
import nv.core.annotations.DefaultChose;
import nv.core.data.NvImage;

/**
 * concrete implementation of NvGraphic
 * that specializes in rendering geometric shapes and text as direct pixel graphics.
 */
@DefaultChose
@SuppressWarnings("unused")
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

        float x1 = tx(component.getX() + base1);
        float x2 = tx(component.getX() + base2);
        float y1 = ty(component.getY() + y);
        float apexY = ty(component.getY() + y - component.getH());

        float[] triVerts = {
                x1, y1, r, g, b, wu, wv, 0f,
                x2, y1, r, g, b, wu, wv, 0f,
                (x1 + x2) * 0.5f, apexY, r, g, b, wu, wv, 0f,
        };

        int[] triInds = {0, 1, 2};
        comp.append(triVerts, triInds);
    }

    @Override
    public void drawPolygon(float[] vertices, int[] indices, float[] colors, AppendableGeometry comp) {
        int numVertices = vertices.length / 2;
        float[] polyVerts = new float[numVertices * FLOATS_PER_VERTEX];

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

            int off = i * FLOATS_PER_VERTEX;
            polyVerts[off]     = vx;
            polyVerts[off + 1] = vy;
            polyVerts[off + 2] = vr;
            polyVerts[off + 3] = vg;
            polyVerts[off + 4] = vb;
            polyVerts[off + 5] = wu;
            polyVerts[off + 6] = wv;
            polyVerts[off + 7] = 0f;
        }

        comp.append(polyVerts, indices);
    }

    @Override
    public void drawOval(float x, float y, float radius, int accuracy,
                         float r, float g, float b,
                         AppendableGeometry comp) {

        float[] ovalVerts = new float[(accuracy + 1) * FLOATS_PER_VERTEX];
        int[] ovalInds = new int[accuracy * 3];

        x += radius/2;
        y += radius/2;

        float cx = tx(component.getX() + x);
        float cy = ty(component.getY() + y);

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

        float x1 = tx(component.getX() + x);
        float y1 = ty(component.getY() + y);
        float x2 = tx(component.getX() + x + w);
        float y2 = ty(component.getY() + y + h);

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

    @Override
    public void drawText(String text, float textX, float textY,
                         AppendableGeometry comp) {

        float x = tx(component.getX() + textX);
        float y = ty(component.getY() + textY);
        Scene textGeo = generateTextGeometry(text, x, y, fontAtlas, r,g,b);
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

        float x1 = tx(component.getX() + x);
        float y1 = ty(component.getY() + y);
        float x2 = tx(component.getX() + x + w);
        float y2 = ty(component.getY() + y + h);

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
