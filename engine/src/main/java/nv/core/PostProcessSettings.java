package nv.core;

import nv.core.annotations.EngineCore;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.nio.ByteBuffer;

import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_FRAGMENT_BIT;
import static org.lwjgl.vulkan.VK10.vkCmdPushConstants;

/**
 * Settings and real-time parameters for the Vulkan Post-Processing Pipeline.
 *
 * @since 1.6.2
 * @author Andrea Maruca
 */
@EngineCore
@SuppressWarnings("unused")
public final class PostProcessSettings {

    private boolean enabled = false;

    // CRT TV Curvature (barrel distortion with inward curved corners)
    private float crtCurvature = 0.0f;      // 0.0 = flat/disabled, 3.5 .. 6.0 = authentic CRT tube curvature
    private float crtCornerSmooth = 0.35f;  // Corner shadow softness

    // CRT Scanlines
    private float scanlineStrength = 0.0f;  // 0.0 = off, 0.2 .. 0.5 = retro scanlines
    private float scanlineCount = 400.0f;   // Number of vertical scanline rows

    // Chromatic Aberration (RGB channel splitting)
    private float chromaticAberration = 0.0f; // 0.0 = off, 0.003 .. 0.02 = hit/glitch effect

    // Vignette
    private float vignetteStrength = 0.0f;  // 0.0 = off, 0.2 .. 0.6 = dark corners
    private float vignetteRoundness = 1.0f;

    // Film Grain / Animated Noise
    private float filmGrainStrength = 0.0f; // 0.0 = off, 0.05 .. 0.2 = retro noise

    // Color Grading
    private float brightness = 0.0f;        // -1.0 .. 1.0 (default 0.0)
    private float contrast = 1.0f;          // 0.0 .. 2.0 (default 1.0)
    private float saturation = 1.0f;        // 0.0 = black & white, 1.0 = normal, > 1.0 = vibrant
    private float tintR = 1.0f;
    private float tintG = 1.0f;
    private float tintB = 1.0f;

    // Bloom
    private float bloomStrength = 0.0f;     // 0.0 = off, 0.3 .. 1.5 = glowing neon
    private float bloomThreshold = 0.7f;    // 0.0 .. 1.0 luminance threshold for bloom

    public PostProcessSettings() {}

    // -------------------------------------------------------------
    // MASTER TOGGLE
    // -------------------------------------------------------------

    public boolean isEnabled() {
        return enabled;
    }

    public PostProcessSettings setEnabled(boolean enabled) {
        this.enabled = enabled;
        NvContext.markSceneDirty();
        return this;
    }

    // -------------------------------------------------------------
    // CRT CURVATURE & BARREL DISTORTION (OLD TV CORNERS)
    // -------------------------------------------------------------

    /**
     * Sets the CRT screen curvature (barrel distortion).
     *
     * @param curvature 0.0 to disable, 3.5f to 6.0f for authentic retro curved TV tube effect
     */
    public PostProcessSettings setCRTCurvature(float curvature) {
        this.crtCurvature = Math.max(0.0f, curvature);
        if (this.crtCurvature > 0.0f) this.enabled = true;
        NvContext.markSceneDirty();
        return this;
    }

    public float getCRTCurvature() {
        return crtCurvature;
    }

    public PostProcessSettings setCRTCornerSmooth(float smooth) {
        this.crtCornerSmooth = Math.max(0.0f, smooth);
        NvContext.markSceneDirty();
        return this;
    }

    public float getCRTCornerSmooth() {
        return crtCornerSmooth;
    }

    /**
     * Enables full CRT monitor simulation (curvature + scanlines + corner darkness).
     *
     * @param curvature        e.g. 4.0f
     * @param scanlineStrength e.g. 0.35f
     */
    public PostProcessSettings enableCRT(float curvature, float scanlineStrength) {
        this.enabled = true;
        this.crtCurvature = curvature;
        this.scanlineStrength = scanlineStrength;
        this.crtCornerSmooth = 0.35f;
        NvContext.markSceneDirty();
        return this;
    }

    public PostProcessSettings disableCRT() {
        this.crtCurvature = 0.0f;
        this.scanlineStrength = 0.0f;
        NvContext.markSceneDirty();
        return this;
    }

    // -------------------------------------------------------------
    // SCANLINES
    // -------------------------------------------------------------

    public PostProcessSettings setScanlineStrength(float strength) {
        this.scanlineStrength = Math.max(0.0f, Math.min(1.0f, strength));
        if (this.scanlineStrength > 0.0f) this.enabled = true;
        NvContext.markSceneDirty();
        return this;
    }

    public float getScanlineStrength() {
        return scanlineStrength;
    }

    public PostProcessSettings setScanlineCount(float count) {
        this.scanlineCount = count;
        NvContext.markSceneDirty();
        return this;
    }

    public float getScanlineCount() {
        return scanlineCount;
    }

    // -------------------------------------------------------------
    // CHROMATIC ABERRATION
    // -------------------------------------------------------------

    /**
     * Sets RGB color channel splitting offset.
     *
     * @param intensity 0.0 for none, 0.003 to 0.015 for hit/glitch/shake effects
     */
    public PostProcessSettings setChromaticAberration(float intensity) {
        this.chromaticAberration = Math.max(0.0f, intensity);
        if (this.chromaticAberration > 0.0f) this.enabled = true;
        NvContext.markSceneDirty();
        return this;
    }

    public float getChromaticAberration() {
        return chromaticAberration;
    }

    // -------------------------------------------------------------
    // VIGNETTE & FILM GRAIN
    // -------------------------------------------------------------

    public PostProcessSettings setVignette(float strength) {
        this.vignetteStrength = Math.max(0.0f, strength);
        if (this.vignetteStrength > 0.0f) this.enabled = true;
        NvContext.markSceneDirty();
        return this;
    }

    public float getVignetteStrength() {
        return vignetteStrength;
    }

    public PostProcessSettings setFilmGrain(float strength) {
        this.filmGrainStrength = Math.max(0.0f, strength);
        if (this.filmGrainStrength > 0.0f) this.enabled = true;
        NvContext.markSceneDirty();
        return this;
    }

    public float getFilmGrainStrength() {
        return filmGrainStrength;
    }

    // -------------------------------------------------------------
    // BLOOM / NEON GLOW
    // -------------------------------------------------------------

    public PostProcessSettings setBloom(float strength, float threshold) {
        this.bloomStrength = Math.max(0.0f, strength);
        this.bloomThreshold = Math.max(0.0f, Math.min(1.0f, threshold));
        if (this.bloomStrength > 0.0f) this.enabled = true;
        NvContext.markSceneDirty();
        return this;
    }

    public float getBloomStrength() {
        return bloomStrength;
    }

    // -------------------------------------------------------------
    // COLOR ADJUSTMENTS (BRIGHTNESS, CONTRAST, SATURATION, TINT)
    // -------------------------------------------------------------

    public PostProcessSettings setBrightness(float brightness) {
        this.brightness = brightness;
        NvContext.markSceneDirty();
        return this;
    }

    public float getBrightness() {
        return brightness;
    }

    public PostProcessSettings setContrast(float contrast) {
        this.contrast = Math.max(0.0f, contrast);
        NvContext.markSceneDirty();
        return this;
    }

    public float getContrast() {
        return contrast;
    }

    public PostProcessSettings setSaturation(float saturation) {
        this.saturation = Math.max(0.0f, saturation);
        NvContext.markSceneDirty();
        return this;
    }

    public float getSaturation() {
        return saturation;
    }

    public PostProcessSettings setTint(float r, float g, float b) {
        this.tintR = Math.max(0.0f, r);
        this.tintG = Math.max(0.0f, g);
        this.tintB = Math.max(0.0f, b);
        NvContext.markSceneDirty();
        return this;
    }

    public float getTintR() { return tintR; }
    public float getTintG() { return tintG; }
    public float getTintB() { return tintB; }

    public PostProcessSettings setColorGrading(float brightness, float contrast, float saturation) {
        this.brightness = brightness;
        this.contrast = contrast;
        this.saturation = saturation;
        NvContext.markSceneDirty();
        return this;
    }

    // -------------------------------------------------------------
    // PRESETS
    // -------------------------------------------------------------

    /**
     * Retro Arcade / CRT TV Preset:
     * Curved glass corners, scanlines, subtle RGB splitting, corner darkening, slight glow.
     */
    public PostProcessSettings presetRetroCRT() {
        reset();
        this.enabled = true;
        this.crtCurvature = 4.2f;
        this.crtCornerSmooth = 0.35f;
        this.scanlineStrength = 0.35f;
        this.scanlineCount = 420.0f;
        this.chromaticAberration = 0.003f;
        this.vignetteStrength = 0.25f;
        this.bloomStrength = 0.25f;
        this.bloomThreshold = 0.6f;
        this.contrast = 1.1f;
        NvContext.markSceneDirty();
        return this;
    }

    /**
     * Cyberpunk / Neon Glow Preset:
     * Strong bloom, vivid colors, boosted contrast, slight chromatic aberration.
     */
    public PostProcessSettings presetCyberpunk() {
        reset();
        this.enabled = true;
        this.bloomStrength = 0.85f;
        this.bloomThreshold = 0.55f;
        this.chromaticAberration = 0.005f;
        this.contrast = 1.25f;
        this.saturation = 1.35f;
        this.vignetteStrength = 0.2f;
        NvContext.markSceneDirty();
        return this;
    }

    /**
     * Film Noir / Classic Black & White Preset:
     * Full desaturation, high contrast, film grain, vignette.
     */
    public PostProcessSettings presetNoir() {
        reset();
        this.enabled = true;
        this.saturation = 0.0f;
        this.contrast = 1.4f;
        this.brightness = -0.05f;
        this.filmGrainStrength = 0.15f;
        this.vignetteStrength = 0.45f;
        NvContext.markSceneDirty();
        return this;
    }

    /**
     * VHS Tape / Glitch Preset:
     * Noticeable chromatic aberration, scanlines, noise, mild curvature.
     */
    public PostProcessSettings presetVHS() {
        reset();
        this.enabled = true;
        this.crtCurvature = 6.5f;
        this.scanlineStrength = 0.4f;
        this.chromaticAberration = 0.008f;
        this.filmGrainStrength = 0.12f;
        this.contrast = 1.15f;
        NvContext.markSceneDirty();
        return this;
    }

    /**
     * Damage Flash effect (ideal to trigger when the player takes damage).
     *
     * @param intensity e.g. 0.8f
     */
    public PostProcessSettings triggerDamageFlash(float intensity) {
        this.enabled = true;
        this.chromaticAberration = 0.015f * intensity;
        this.vignetteStrength = 0.6f * intensity;
        this.tintR = 1.0f + 0.5f * intensity;
        this.tintG = 1.0f - 0.4f * intensity;
        this.tintB = 1.0f - 0.4f * intensity;
        NvContext.markSceneDirty();
        return this;
    }

    /**
     * Resets all parameters to their default neutral values.
     */
    public PostProcessSettings reset() {
        this.enabled = false;
        this.crtCurvature = 0.0f;
        this.crtCornerSmooth = 0.35f;
        this.scanlineStrength = 0.0f;
        this.scanlineCount = 400.0f;
        this.chromaticAberration = 0.0f;
        this.vignetteStrength = 0.0f;
        this.vignetteRoundness = 1.0f;
        this.filmGrainStrength = 0.0f;
        this.brightness = 0.0f;
        this.contrast = 1.0f;
        this.saturation = 1.0f;
        this.tintR = 1.0f;
        this.tintG = 1.0f;
        this.tintB = 1.0f;
        this.bloomStrength = 0.0f;
        this.bloomThreshold = 0.7f;
        NvContext.markSceneDirty();
        return this;
    }

    // -------------------------------------------------------------
    // PUSH CONSTANTS SERIALIZATION TO VULKAN
    // -------------------------------------------------------------

    public void pushConstants(VkCommandBuffer commandBuffer, long pipelineLayout, float time) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer buffer = stack.malloc(18 * Float.BYTES);
            buffer.putFloat(time);
            buffer.putFloat(enabled ? 1.0f : 0.0f);
            buffer.putFloat(crtCurvature);
            buffer.putFloat(crtCornerSmooth);
            buffer.putFloat(scanlineStrength);
            buffer.putFloat(scanlineCount);
            buffer.putFloat(chromaticAberration);
            buffer.putFloat(vignetteStrength);
            buffer.putFloat(vignetteRoundness);
            buffer.putFloat(filmGrainStrength);
            buffer.putFloat(brightness);
            buffer.putFloat(contrast);
            buffer.putFloat(saturation);
            buffer.putFloat(tintR);
            buffer.putFloat(tintG);
            buffer.putFloat(tintB);
            buffer.putFloat(bloomStrength);
            buffer.putFloat(bloomThreshold);
            buffer.flip();

            vkCmdPushConstants(
                    commandBuffer,
                    pipelineLayout,
                    VK_SHADER_STAGE_FRAGMENT_BIT,
                    0,
                    buffer
            );
        }
    }
}
