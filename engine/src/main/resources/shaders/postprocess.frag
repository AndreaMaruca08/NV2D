#version 450

layout(binding = 0) uniform sampler2D sceneTexture;

layout(location = 0) in vec2 inUV;
layout(location = 0) out vec4 outColor;

layout(push_constant) uniform PostProcessPushConstants {
    float time;
    float enabled;
    float crtCurvature;
    float crtCornerSmooth;
    float scanlineStrength;
    float scanlineCount;
    float chromaticAberration;
    float vignetteStrength;
    float vignetteRoundness;
    float filmGrainStrength;
    float brightness;
    float contrast;
    float saturation;
    float tintR;
    float tintG;
    float tintB;
    float bloomStrength;
    float bloomThreshold;
} pc;

vec2 applyCrtCurvature(vec2 uv, float curvature) {
    if (curvature <= 0.001) {
        return uv;
    }
    // Remap UV from [0, 1] to [-1, 1]
    vec2 st = uv * 2.0 - 1.0;
    // Radial barrel distortion formula for CRT monitor curvature
    vec2 offset = abs(st.yx) / curvature;
    st = st + st * offset * offset;
    // Remap back to [0, 1]
    return st * 0.5 + 0.5;
}

void main() {
    if (pc.enabled < 0.5) {
        outColor = texture(sceneTexture, inUV);
        return;
    }

    // 1. CRT Curvature / Barrel Distortion (curved inward corners)
    vec2 uv = applyCrtCurvature(inUV, pc.crtCurvature);

    // If outside [0, 1], draw dark CRT bezel / border
    if (pc.crtCurvature > 0.001) {
        if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) {
            outColor = vec4(0.0, 0.0, 0.0, 1.0);
            return;
        }
    }

    // 2. Chromatic Aberration
    vec3 color;
    if (pc.chromaticAberration > 0.0001) {
        vec2 dir = uv - vec2(0.5);
        vec2 caOffset = dir * pc.chromaticAberration;
        float r = texture(sceneTexture, uv + caOffset).r;
        float g = texture(sceneTexture, uv).g;
        float b = texture(sceneTexture, uv - caOffset).b;
        color = vec3(r, g, b);
    } else {
        color = texture(sceneTexture, uv).rgb;
    }

    // 3. Bloom (fast multi-sample approximation)
    if (pc.bloomStrength > 0.001) {
        vec3 bloom = vec3(0.0);
        float stepSize = 0.004;
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                vec3 sampleColor = texture(sceneTexture, uv + vec2(float(x), float(y)) * stepSize).rgb;
                float luma = dot(sampleColor, vec3(0.2126, 0.7152, 0.0722));
                if (luma > pc.bloomThreshold) {
                    bloom += sampleColor * (luma - pc.bloomThreshold);
                }
            }
        }
        bloom /= 25.0;
        color += bloom * pc.bloomStrength * 3.0;
    }

    // 4. CRT Scanlines
    if (pc.scanlineStrength > 0.001) {
        float count = pc.scanlineCount > 0.0 ? pc.scanlineCount : 400.0;
        float scanline = sin(uv.y * count * 3.14159265 + pc.time * 2.0) * 0.5 + 0.5;
        color *= (1.0 - pc.scanlineStrength * (1.0 - scanline));
    }

    // 5. Vignette & CRT corner shading
    if (pc.vignetteStrength > 0.001) {
        vec2 vCoord = uv * (1.0 - uv.yx);
        float vig = vCoord.x * vCoord.y * 15.0;
        vig = clamp(pow(vig, pc.vignetteStrength), 0.0, 1.0);
        color *= vig;
    }

    if (pc.crtCurvature > 0.001 && pc.crtCornerSmooth > 0.001) {
        vec2 cornerCoord = uv * (1.0 - uv.yx);
        float cornerVig = cornerCoord.x * cornerCoord.y * 40.0;
        cornerVig = clamp(pow(cornerVig, pc.crtCornerSmooth), 0.0, 1.0);
        color *= cornerVig;
    }

    // 6. Film Grain / Animated Noise
    if (pc.filmGrainStrength > 0.001) {
        float noise = fract(sin(dot(uv + vec2(pc.time * 0.05, -pc.time * 0.03), vec2(12.9898, 78.233))) * 43758.5453);
        color += (noise - 0.5) * pc.filmGrainStrength;
    }

    // 7. Color Adjustments: Contrast, Brightness, Saturation, Tint
    color = (color - 0.5) * pc.contrast + 0.5 + pc.brightness;
    float gray = dot(color, vec3(0.299, 0.587, 0.114));
    color = mix(vec3(gray), color, pc.saturation);
    color *= vec3(pc.tintR, pc.tintG, pc.tintB);

    outColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
