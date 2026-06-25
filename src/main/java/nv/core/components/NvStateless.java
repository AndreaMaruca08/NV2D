package nv.core.components;

import nv.core.AppendableGeometry;
import nv.core.annotations.EngineCore;
import nv.core.collision.CollisionSystem;
import nv.core.graphic.NvGraphic;

import java.util.Arrays;

import static nv.core.graphic.NvGraphic.FLOATS_PER_VERTEX;

@EngineCore
@SuppressWarnings("unused")
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
        this.weight = CollisionSystem.MAX_WEIGHT;
    }
    public void invalidate() {
        super.invalidate();
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
        g.append(vertices, vertexFloatCount, indices, indexCount);
    }
    @Override
    public void update(float dt) {}
    @Override
    public void append(float[] newVertices, int[] newIndices) {
        append(newVertices, newVertices.length, newIndices, newIndices.length);
    }

    @Override
    public void append(float[] newVertices, int vertexFloatCount, int[] newIndices, int indexCount) {
        int vertexOffset = this.vertexFloatCount / FLOATS_PER_VERTEX;

        ensureVertexCapacity(this.vertexFloatCount + vertexFloatCount);
        ensureIndexCapacity(this.indexCount + indexCount);

        System.arraycopy(newVertices, 0, vertices, this.vertexFloatCount, vertexFloatCount);
        for (int i = 0; i < indexCount; i++) {
            indices[this.indexCount + i] = newIndices[i] + vertexOffset;
        }
        this.vertexFloatCount += vertexFloatCount;
        this.indexCount += indexCount;
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
