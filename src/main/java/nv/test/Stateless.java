package nv.test;

import nv.core.components.NvStateless;
import nv.core.graphic.NvGraphic;

public class Stateless extends NvStateless {
    public Stateless(int x, int y, int w, int h) {
        super(x, y, w, h);
    }
    @Override
    public void drawIntern(NvGraphic g) {
        // Triangle vertices (relative to component position)
        float[] vertices = {
            100f, 0f,      // Vertex 0 (Top Center)
            0f, 200f,      // Vertex 1 (Bottom Left)
            200f, 200f     // Vertex 2 (Bottom Right)
        };

        // Indices to form the triangle
        int[] indices = { 0, 1, 2 };

        // Colors per vertex: Red for top, Green for bottom-left, Blue for bottom-right
        float[] colors = {
            1.0f, 0.0f, 0.0f, 1.0f, // Red (RGBA)
            0.0f, 1.0f, 0.0f, 1.0f, // Green (RGBA)
            0.0f, 0.0f, 1.0f, 1.0f  // Blue (RGBA)
        };

        // Draw the polygon using the new method (without 'comp' overload)
        g.drawPolygon(vertices, indices, colors);
    }
}
