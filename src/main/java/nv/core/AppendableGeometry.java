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
}
