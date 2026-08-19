package nv.test;

import nv.core.camera.NvCamera;
import nv.core.components.Vector2D;
import nv.core.graphic.NvGraphic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NvCamera Unit Tests")
public class NvCameraTest {

    private NvCamera camera;

    @BeforeEach
    void setUp() {
        // Constructor converts zoom from percentage (
        camera = new NvCamera(100f, 200f, 1f);
        NvGraphic.camera = camera;
    }

    @Test
    @DisplayName("Test camera initial state")
    void testInitialState() {
        assertEquals(100f, camera.x, 0.0001f);
        assertEquals(200f, camera.y, 0.0001f);
        assertEquals(1.0f, camera.zoom, 0.0001f);
    }

    @Test
    @DisplayName("Test camera translation by values and vector")
    void testTranslation() {
        camera.translate(50f, -30f);
        assertEquals(150f, camera.x, 0.0001f);
        assertEquals(170f, camera.y, 0.0001f);

        camera.translate(Vector2D.RIGHT);
        assertEquals(151f, camera.x, 0.0001f);
        assertEquals(170f, camera.y, 0.0001f);
    }

    @Test
    @DisplayName("Test setXY explicit position setting")
    void testSetXY() {
        camera.setXY(500f, 600f);
        assertEquals(500f, camera.x, 0.0001f);
        assertEquals(600f, camera.y, 0.0001f);
    }

    @Test
    @DisplayName("Test zoom modification")
    void testZoom() {
        camera.zoom(0.5f);
        assertEquals(1.5f, camera.zoom, 0.0001f);

        camera.zoom(-0.2f);
        assertEquals(1.3f, camera.zoom, 0.0001f);
    }

    @Test
    @DisplayName("Test camera toString representation")
    void testToString() {
        String str = camera.toString();
        assertTrue(str.contains("100.0"));
        assertTrue(str.contains("200.0"));
        assertTrue(str.contains("1.0"));
    }
}
