package nv.core.components;

import nv.core.annotations.ReadyComponent;

/**
 * Vectors constants for 16 directions
 * @since 1.0
 * @author Andrea Maruca
 */
@ReadyComponent
@SuppressWarnings("unused")
public enum Vector2D {

    UP(0f, -1f),

    UP_RIGHT_UP(0.38268343f, -0.9238795f),
    UP_RIGHT(0.70710677f, -0.70710677f),
    RIGHT_UP(0.9238795f, -0.38268343f),

    RIGHT(1f, 0f),

    RIGHT_DOWN(0.9238795f, 0.38268343f),
    DOWN_RIGHT(0.70710677f, 0.70710677f),
    DOWN_RIGHT_DOWN(0.38268343f, 0.9238795f),

    DOWN(0f, 1f),

    DOWN_LEFT_DOWN(-0.38268343f, 0.9238795f),
    DOWN_LEFT(-0.70710677f, 0.70710677f),
    LEFT_DOWN(-0.9238795f, 0.38268343f),

    LEFT(-1f, 0f),

    LEFT_UP(-0.9238795f, -0.38268343f),
    UP_LEFT(-0.70710677f, -0.70710677f),
    UP_LEFT_UP(-0.38268343f, -0.9238795f);


    public final float x;
    public final float y;

    Vector2D(float x, float y) {
        this.x = x;
        this.y = y;
    }

    private static final Vector2D[] VALUES = values();

    public Vector2D opposite() {
        return fromVector(-x, -y);
    }

    public Vector2D clockwise() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    public Vector2D counterClockwise() {
        return VALUES[(ordinal() - 1 + VALUES.length) % VALUES.length];
    }

    public static Vector2D fromVector(float x, float y) {
        for (Vector2D dir : values()) {
            if (dir.x == x && dir.y == y) {
                return dir;
            }
        }

        return null;
    }
}