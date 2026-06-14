package nv.components;

import nv.core.AppendableGeometry;

import java.util.Arrays;
import static nv.components.NvGraphic.FLOATS_PER_VERTEX;

public abstract class NvStateless extends NvComp implements AppendableGeometry {
    private float[] vertices;
    private int[] indices;
    protected int vertexFloatCount;
    protected int indexCount;
    public boolean initialized = false;

    public NvStateless(int x, int y, int w, int h) {
        super(x, y, w, h);
        this.vertices = new float[1024 * FLOATS_PER_VERTEX];
        this.indices = new int[1024];
    }
    public void invalidate() {
        this.initialized = false;
        this.vertexFloatCount = 0;
        this.indexCount = 0;
    }
    @Override
    public void draw(NvGraphic g) {
        if (!initialized) {
            super.draw(g);
            initialized = true;
        }
        g.append(
                Arrays.copyOf(vertices, vertexFloatCount),
                Arrays.copyOf(indices, indexCount)
        );
    }
    @Override
    public void update(float dt) {}
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
    private void ensureVertexCapacity(int requiredCapacity) {
        if (requiredCapacity > vertices.length) {
            vertices = Arrays.copyOf(vertices, Math.max(vertices.length * 2, requiredCapacity));
        }
    }

    private void ensureIndexCapacity(int requiredCapacity) {
        if (requiredCapacity > indices.length) {
            indices = Arrays.copyOf(indices, Math.max(indices.length * 2, requiredCapacity));
        }
    }
}
