#version 450

layout(binding = 1) uniform sampler2D uTexture;

layout(location = 0) in vec3 fragColor;
layout(location = 1) in vec2 fragUV;      // nuovo: UV passate dal vertex shader

layout(location = 0) out vec4 outColor;

void main() {
    outColor = texture(uTexture, fragUV) * vec4(fragColor, 1.0);
}