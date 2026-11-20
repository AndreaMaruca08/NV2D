import nv.core.PostProcessSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.Assertions;

@DisplayName("PostProcessSettings Unit Tests")
public class PostProcessSettingsTest {

    @Test
    @DisplayName("Test default settings state")
    void testDefaultState() {
        PostProcessSettings settings = new PostProcessSettings();
        Assertions.assertFalse(settings.isEnabled());
        Assertions.assertEquals(0.0f, settings.getCRTCurvature(), 0.0001f);
        Assertions.assertEquals(0.0f, settings.getScanlineStrength(), 0.0001f);
        Assertions.assertEquals(0.0f, settings.getChromaticAberration(), 0.0001f);
        Assertions.assertEquals(0.0f, settings.getVignetteStrength(), 0.0001f);
        Assertions.assertEquals(0.0f, settings.getFilmGrainStrength(), 0.0001f);
        Assertions.assertEquals(1.0f, settings.getContrast(), 0.0001f);
        Assertions.assertEquals(1.0f, settings.getSaturation(), 0.0001f);
        Assertions.assertEquals(0.0f, settings.getBrightness(), 0.0001f);
    }

    @Test
    @DisplayName("Test CRT TV preset and curvature distortion")
    void testRetroCRTPreset() {
        PostProcessSettings settings = new PostProcessSettings();
        settings.presetRetroCRT();

        Assertions.assertTrue(settings.isEnabled());
        Assertions.assertTrue(settings.getCRTCurvature() > 0.0f);
        Assertions.assertTrue(settings.getScanlineStrength() > 0.0f);
        Assertions.assertTrue(settings.getChromaticAberration() > 0.0f);
        Assertions.assertTrue(settings.getVignetteStrength() > 0.0f);
    }

    @Test
    @DisplayName("Test Cyberpunk and Noir presets")
    void testPresets() {
        PostProcessSettings settings = new PostProcessSettings();

        settings.presetCyberpunk();
        Assertions.assertTrue(settings.isEnabled());
        Assertions.assertTrue(settings.getBloomStrength() > 0.0f);
        Assertions.assertTrue(settings.getSaturation() > 1.0f);

        settings.presetNoir();
        Assertions.assertTrue(settings.isEnabled());
        Assertions.assertEquals(0.0f, settings.getSaturation(), 0.0001f);
        Assertions.assertTrue(settings.getFilmGrainStrength() > 0.0f);
    }

    @Test
    @DisplayName("Test manual property configuration and reset")
    void testManualAndReset() {
        PostProcessSettings settings = new PostProcessSettings();
        settings.setCRTCurvature(5.0f)
                .setScanlineStrength(0.5f)
                .setChromaticAberration(0.01f)
                .setBloom(0.8f, 0.5f)
                .setColorGrading(0.1f, 1.2f, 1.3f)
                .setTint(1.0f, 0.8f, 0.6f);

        Assertions.assertTrue(settings.isEnabled());
        Assertions.assertEquals(5.0f, settings.getCRTCurvature(), 0.0001f);
        Assertions.assertEquals(0.5f, settings.getScanlineStrength(), 0.0001f);
        Assertions.assertEquals(0.01f, settings.getChromaticAberration(), 0.0001f);
        Assertions.assertEquals(0.8f, settings.getBloomStrength(), 0.0001f);
        Assertions.assertEquals(1.0f, settings.getTintR(), 0.0001f);
        Assertions.assertEquals(0.8f, settings.getTintG(), 0.0001f);
        Assertions.assertEquals(0.6f, settings.getTintB(), 0.0001f);

        settings.reset();
        Assertions.assertFalse(settings.isEnabled());
        Assertions.assertEquals(0.0f, settings.getCRTCurvature(), 0.0001f);
        Assertions.assertEquals(1.0f, settings.getTintG(), 0.0001f);
    }
}
