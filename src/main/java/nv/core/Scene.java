package nv.core;

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

