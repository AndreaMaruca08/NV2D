import nv.core.ScreenSize;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.Assertions;

@DisplayName("ScreenSize Enum Unit Tests")
public class ScreenSizeTest {

    @Test
    @DisplayName("Test standard resolution dimensions")
    void testScreenSizeDimensions() {
        Assertions.assertEquals(320, ScreenSize._320x240.getWidth());
        Assertions.assertEquals(240, ScreenSize._320x240.getHeight());

        Assertions.assertEquals(1280, ScreenSize._1280x720.getWidth());
        Assertions.assertEquals(720, ScreenSize._1280x720.getHeight());

        Assertions.assertEquals(1920, ScreenSize._1920x1080.getWidth());
        Assertions.assertEquals(1080, ScreenSize._1920x1080.getHeight());

        Assertions.assertEquals(2560, ScreenSize._2560x1440.getWidth());
        Assertions.assertEquals(1440, ScreenSize._2560x1440.getHeight());

        Assertions.assertEquals(3840, ScreenSize._3840x2160.getWidth());
        Assertions.assertEquals(2160, ScreenSize._3840x2160.getHeight());

        Assertions.assertEquals(7680, ScreenSize._7680x4320.getWidth());
        Assertions.assertEquals(4320, ScreenSize._7680x4320.getHeight());
    }

    @Test
    @DisplayName("Test positive dimensions across all defined resolutions")
    void testAllDimensionsPositive() {
        for (ScreenSize size : ScreenSize.values()) {
            Assertions.assertTrue(size.getWidth() > 0, "Width of " + size.name() + " should be positive");
            Assertions.assertTrue(size.getHeight() > 0, "Height of " + size.name() + " should be positive");
        }
    }
}
