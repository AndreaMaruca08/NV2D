#version 450

layout(set = 0, binding = 0) uniform UniformBufferObject {
    mat4 ortho;
} ubo;

layout(location = 0) in vec2 inPosition;
layout(location = 1) in vec3 inColor;
layout(location = 2) in vec2 inUV;
layout(location = 3) in float inTexIndex;

layout(location = 0) out vec3 fragColor;
layout(location = 1) out vec2 fragUV;
layout(location = 2) flat out int fragTexIndex;

void main() {
    gl_Position  = ubo.ortho * vec4(inPosition, 0.0, 1.0);
    fragColor    = inColor;
    fragUV       = inUV;
    fragTexIndex = int(inTexIndex + 0.5);
}
