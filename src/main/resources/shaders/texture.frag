#version 450

layout(binding = 1) uniform sampler2D textures[15];

layout(location = 0) in vec3 fragColor;
layout(location = 1) in vec2 fragUV;
layout(location = 2) flat in int fragTexIndex;

layout(location = 0) out vec4 outColor;

void main() {
    vec4 texColor;
    if (fragTexIndex == 0)      texColor = texture(textures[0], fragUV);
    else if (fragTexIndex == 1) texColor = texture(textures[1], fragUV);
    else if (fragTexIndex == 2) texColor = texture(textures[2], fragUV);
    else if (fragTexIndex == 3) texColor = texture(textures[3], fragUV);
    else if (fragTexIndex == 4) texColor = texture(textures[4], fragUV);
    else if (fragTexIndex == 5) texColor = texture(textures[5], fragUV);
    else if (fragTexIndex == 6) texColor = texture(textures[6], fragUV);
    else if (fragTexIndex == 7) texColor = texture(textures[7], fragUV);
    else if (fragTexIndex == 8) texColor = texture(textures[8], fragUV);
    else if (fragTexIndex == 9) texColor = texture(textures[9], fragUV);
    else if (fragTexIndex == 10) texColor = texture(textures[10], fragUV);
    else if (fragTexIndex == 11) texColor = texture(textures[11], fragUV);
    else if (fragTexIndex == 12) texColor = texture(textures[12], fragUV);
    else if (fragTexIndex == 13) texColor = texture(textures[13], fragUV);
    else if (fragTexIndex == 14) texColor = texture(textures[14], fragUV);
    else                         texColor = texture(textures[0], fragUV);

    outColor = texColor * vec4(fragColor, 1.0);
}
