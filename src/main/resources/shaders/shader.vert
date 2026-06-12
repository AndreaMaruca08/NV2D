#version 450

// Input dal buffer dei vertici (definiti in GraphicsPipeline)
layout(location = 0) in vec2 inPosition; // X, Y
layout(location = 1) in vec3 inColor;    // R, G, B

// Output inviato al Fragment Shader
layout(location = 0) out vec3 fragColor;

void main() {
    // gl_Position richiede un vec4 (X, Y, Z, W). Impostiamo Z a 0.0 e W a 1.0 per il 2D
    gl_Position = vec4(inPosition, 0.0, 1.0);

    // Passiamo il colore così com'è al fragment shader
    fragColor = inColor;
}