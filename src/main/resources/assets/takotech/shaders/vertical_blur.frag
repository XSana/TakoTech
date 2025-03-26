#version 330 core
in vec2 fragTexCoord;
out vec4 color;

uniform sampler2D image;
uniform float height; // 纹理高度
const float weights[5] = float[](0.227027, 0.1945946, 0.1216216, 0.054054, 0.016216);

void main() {
    vec2 tex_offset = 1.0 / vec2(1.0, height); // 垂直偏移
    vec3 result = texture(image, fragTexCoord).rgb * weights[0];

    for (int i = 1; i < 5; ++i) {
        result += texture(image, fragTexCoord + vec2(0.0, tex_offset.y * i)).rgb * weights[i];
        result += texture(image, fragTexCoord - vec2(0.0, tex_offset.y * i)).rgb * weights[i];
    }
    color = vec4(result, 1.0);
}
