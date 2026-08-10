package nv.core;

import nv.core.annotations.EngineCore;

/**
 * Interface for geometry that can be appended to instead of the normal graphic pipeline
 * @since 1.0
 * @author Andrea Maruca
 */
@EngineCore
public interface AppendableGeometry {
    void append(float[] vertices, int[] indices);

    default void append(float[] vertices, int vertexFloatCount, int[] indices, int indexCount) {
        if (vertexFloatCount == vertices.length && indexCount == indices.length) {
            append(vertices, indices);
            return;
        }

        float[] compactVertices = new float[vertexFloatCount];
        int[] compactIndices = new int[indexCount];
        System.arraycopy(vertices, 0, compactVertices, 0, vertexFloatCount);
        System.arraycopy(indices, 0, compactIndices, 0, indexCount);
        append(compactVertices, compactIndices);
    }
}
