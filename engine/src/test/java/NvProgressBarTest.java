import nv.utils.NvProgressBar;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.Assertions;

@DisplayName("NvProgressBar Unit Tests")
public class NvProgressBarTest {

    @Test
    @DisplayName("Test vertical progress bar default and orientation toggling")
    void testOrientation() {
        NvProgressBar bar = new NvProgressBar(10, 20, 100, 20, 100f, 0f, 50f);
        Assertions.assertFalse(bar.isHorizontal());
        Assertions.assertTrue(bar.isVertical());

        bar.setHorizontal(true);
        Assertions.assertTrue(bar.isHorizontal());
        Assertions.assertFalse(bar.isVertical());

        bar.setVertical(true);
        Assertions.assertFalse(bar.isHorizontal());
        Assertions.assertTrue(bar.isVertical());
    }

    @Test
    @DisplayName("Test constructor with horizontal boolean flag")
    void testHorizontalConstructor() {
        NvProgressBar hBar = new NvProgressBar(0, 0, 200, 30, 100f, 0f, 75f, true);
        Assertions.assertTrue(hBar.isHorizontal());
        Assertions.assertFalse(hBar.isVertical());
        Assertions.assertEquals(75f, hBar.getValue(), 0.0001f);
        Assertions.assertEquals(100f, hBar.getMaxValue(), 0.0001f);
        Assertions.assertEquals(0f, hBar.getMinValue(), 0.0001f);
    }

    @Test
    @DisplayName("Test progress calculation and clamping")
    void testProgressClamping() {
        NvProgressBar bar = new NvProgressBar(0, 0, 100, 20, 100f, 0f, 50f);
        Assertions.assertEquals(0.5f, bar.getProgress(), 0.0001f);

        bar.setValue(150f);
        Assertions.assertEquals(1.0f, bar.getProgress(), 0.0001f);

        bar.setValue(-50f);
        Assertions.assertEquals(0.0f, bar.getProgress(), 0.0001f);
    }
}
