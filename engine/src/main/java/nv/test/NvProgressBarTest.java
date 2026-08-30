package nv.test;

import nv.utils.NvProgressBar;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NvProgressBar Unit Tests")
public class NvProgressBarTest {

    @Test
    @DisplayName("Test vertical progress bar default and orientation toggling")
    void testOrientation() {
        NvProgressBar bar = new NvProgressBar(10, 20, 100, 20, 100f, 0f, 50f);
        assertFalse(bar.isHorizontal());
        assertTrue(bar.isVertical());

        bar.setHorizontal(true);
        assertTrue(bar.isHorizontal());
        assertFalse(bar.isVertical());

        bar.setVertical(true);
        assertFalse(bar.isHorizontal());
        assertTrue(bar.isVertical());
    }

    @Test
    @DisplayName("Test constructor with horizontal boolean flag")
    void testHorizontalConstructor() {
        NvProgressBar hBar = new NvProgressBar(0, 0, 200, 30, 100f, 0f, 75f, true);
        assertTrue(hBar.isHorizontal());
        assertFalse(hBar.isVertical());
        assertEquals(75f, hBar.getValue(), 0.0001f);
        assertEquals(100f, hBar.getMaxValue(), 0.0001f);
        assertEquals(0f, hBar.getMinValue(), 0.0001f);
    }

    @Test
    @DisplayName("Test progress calculation and clamping")
    void testProgressClamping() {
        NvProgressBar bar = new NvProgressBar(0, 0, 100, 20, 100f, 0f, 50f);
        assertEquals(0.5f, bar.getProgress(), 0.0001f);

        bar.setValue(150f);
        assertEquals(1.0f, bar.getProgress(), 0.0001f);

        bar.setValue(-50f);
        assertEquals(0.0f, bar.getProgress(), 0.0001f);
    }
}
