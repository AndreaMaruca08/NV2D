package nv.test;

import nv.core.components.Vector2D;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Vector2D Unit Tests")
public class Vector2DTest {

    @Test
    @DisplayName("Test cardinal direction coordinates")
    void testCardinalCoordinates() {
        assertEquals(0f, Vector2D.UP.x, 0.0001f);
        assertEquals(-1f, Vector2D.UP.y, 0.0001f);

        assertEquals(1f, Vector2D.RIGHT.x, 0.0001f);
        assertEquals(0f, Vector2D.RIGHT.y, 0.0001f);

        assertEquals(0f, Vector2D.DOWN.x, 0.0001f);
        assertEquals(1f, Vector2D.DOWN.y, 0.0001f);

        assertEquals(-1f, Vector2D.LEFT.x, 0.0001f);
        assertEquals(0f, Vector2D.LEFT.y, 0.0001f);
    }

    @Test
    @DisplayName("Test opposite direction calculation")
    void testOpposite() {
        assertEquals(Vector2D.DOWN, Vector2D.UP.opposite());
        assertEquals(Vector2D.UP, Vector2D.DOWN.opposite());
        assertEquals(Vector2D.LEFT, Vector2D.RIGHT.opposite());
        assertEquals(Vector2D.RIGHT, Vector2D.LEFT.opposite());
        assertEquals(Vector2D.DOWN_LEFT, Vector2D.UP_RIGHT.opposite());
    }

    @Test
    @DisplayName("Test clockwise rotation order")
    void testClockwise() {
        assertEquals(Vector2D.UP_RIGHT_UP, Vector2D.UP.clockwise());
        assertEquals(Vector2D.UP_RIGHT, Vector2D.UP_RIGHT_UP.clockwise());
        assertEquals(Vector2D.RIGHT_UP, Vector2D.UP_RIGHT.clockwise());
        assertEquals(Vector2D.RIGHT, Vector2D.RIGHT_UP.clockwise());
    }

    @Test
    @DisplayName("Test counter-clockwise rotation order")
    void testCounterClockwise() {
        assertEquals(Vector2D.UP_LEFT_UP, Vector2D.UP.counterClockwise());
        assertEquals(Vector2D.UP, Vector2D.UP_RIGHT_UP.counterClockwise());
        assertEquals(Vector2D.RIGHT_UP, Vector2D.RIGHT.counterClockwise());
    }

    @Test
    @DisplayName("Test full rotation returns to starting vector")
    void testFullClockwiseCycle() {
        Vector2D current = Vector2D.UP;
        for (int i = 0; i < Vector2D.values().length; i++) {
            current = current.clockwise();
        }
        assertEquals(Vector2D.UP, current);
    }

    @Test
    @DisplayName("Test fromVector lookup")
    void testFromVector() {
        assertEquals(Vector2D.UP, Vector2D.fromVector(0f, -1f));
        assertEquals(Vector2D.RIGHT, Vector2D.fromVector(1f, 0f));
        assertEquals(Vector2D.DOWN, Vector2D.fromVector(0f, 1f));
        assertEquals(Vector2D.LEFT, Vector2D.fromVector(-1f, 0f));
        assertNull(Vector2D.fromVector(0.5f, 0.5f));
    }
}
