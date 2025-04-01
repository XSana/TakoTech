#version 330 core

in vec2 TexCoord;

out vec4 FragColor;

uniform sampler2D blurredTexture;
uniform float brightnessBoost = 0.1;
uniform vec3 tint = vec3(1.0);

void main() {
    vec4 blurred = texture(blurredTexture, TexCoord);
    vec3 brightened = blurred.rgb + tint * brightnessBoost;

    // 计算到中心的距离（TexCoord 范围是 [0, 1]）
    float dist = distance(TexCoord, vec2(0.5, 0.5));

    // 距离中心越远，透明度越高，范围大约 [0.2, 0.6]
    float alpha = mix(0.2, 0.6, smoothstep(0.0, 0.5, dist));

    FragColor = vec4(brightened, alpha);
}
