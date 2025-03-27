#version 120

uniform sampler2D image;
uniform float blurScale;
uniform vec2 texSize;

const float weights[5] = float[](0.227027, 0.1945946, 0.1216216, 0.054054, 0.016216);

void main() {
    vec2 texCoord = gl_TexCoord[0].st;
    vec2 tex_offset = blurScale / vec2(texSize.x, 1.0); // 水平偏移

    vec3 result = texture2D(image, texCoord).rgb * weights[0];
    for (int i = 1; i < 5; ++i) {
        result += texture2D(image, texCoord + vec2(tex_offset.x * i, 0.0)).rgb * weights[i];
        result += texture2D(image, texCoord - vec2(tex_offset.x * i, 0.0)).rgb * weights[i];
    }

    gl_FragColor = vec4(result, 1.0);
}
