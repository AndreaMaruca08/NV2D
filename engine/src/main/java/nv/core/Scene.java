package nv.core;

import nv.core.annotations.EngineCore;

/**
 * <p>Represents a GPU scene with vertices and indices for both shaders</p>
 * @since 1.0
 * @author Andrea Maruca
 */
@EngineCore
@SuppressWarnings("unused")
public record Scene(
        float[] vertices,
        int[] indices,
        float[] imageVertices,
        int[] imageIndices
) {
    public Scene(float[] vertices, int[] indices) {
        this(vertices, indices, new float[0], new int[0]);
    }
}

