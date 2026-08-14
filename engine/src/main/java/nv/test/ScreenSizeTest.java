package nv.test;

import nv.core.ScreenSize;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ScreenSize Enum Unit Tests")
public class ScreenSizeTest {

    @Test
    @DisplayName("Test standard resolution dimensions")
    void testScreenSizeDimensions() {
        assertEquals(320, ScreenSize._320x240.getWidth());
        assertEquals(240, ScreenSize._320x240.getHeight());

        assertEquals(1280, ScreenSize._1280x720.getWidth());
        assertEquals(720, ScreenSize._1280x720.getHeight());

        assertEquals(1920, ScreenSize._1920x1080.getWidth());
        assertEquals(1080, ScreenSize._1920x1080.getHeight());

        assertEquals(2560, ScreenSize._2560x1440.getWidth());
        assertEquals(1440, ScreenSize._2560x1440.getHeight());

        assertEquals(3840, ScreenSize._3840x2160.getWidth());
        assertEquals(2160, ScreenSize._3840x2160.getHeight());

        assertEquals(7680, ScreenSize._7680x4320.getWidth());
        assertEquals(4320, ScreenSize._7680x4320.getHeight());
    }

    @Test
    @DisplayName("Test positive dimensions across all defined resolutions")
    void testAllDimensionsPositive() {
        for (ScreenSize size : ScreenSize.values()) {
            assertTrue(size.getWidth() > 0, "Width of " + size.name() + " should be positive");
            assertTrue(size.getHeight() > 0, "Height of " + size.name() + " should be positive");
        }
    }
}
