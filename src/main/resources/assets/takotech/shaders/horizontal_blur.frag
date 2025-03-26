#version 330 core
in vec2 fragTexCoord;
out vec4 color;

uniform sampler2D image;
uniform float blurScale; // 新增 uniform，控制模糊强度
const float weights[5] = float[](0.227027, 0.1945946, 0.1216216, 0.054054, 0.016216);

void main() {
    float width = float(textureSize(image, 0).x);
    vec2 tex_offset = blurScale / vec2(width, 1.0); // 水平偏移
    vec3 result = texture(image, fragTexCoord).rgb * weights[0];

    for (int i = 1; i < 5; ++i) {
        result += texture(image, fragTexCoord + vec2(tex_offset.x * i, 0.0)).rgb * weights[i];
        result += texture(image, fragTexCoord - vec2(tex_offset.x * i, 0.0)).rgb * weights[i];
    }
    color = vec4(result, 1.0);
}
