#version 450

// Input ricevuto dal Vertex Shader (il nome deve coincidere, la location pure)
layout(location = 0) in vec3 fragColor;

// Output finale: il colore del pixel sul framebuffer (RGBA)
layout(location = 0) out vec4 outColor;

void main() {
    // Costruiamo il colore finale aggiungendo il canale Alpha (trasparenza) al massimo (1.0)
    outColor = vec4(fragColor, 1.0);
}