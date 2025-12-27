#version 330 core

// 渲染模式常量
const int MODE_COLOR = 0;           // 纯色渲染
const int MODE_TEXTURE = 1;         // 纹理渲染
const int MODE_TEXTURE_COLOR = 2;   // 纹理 * 颜色
const int MODE_BLUR_H = 3;          // 水平模糊
const int MODE_BLUR_V = 4;          // 垂直模糊

// 从顶点着色器接收
in vec2 vTexCoord;
in vec4 vColor;

// 材质参数
uniform int uRenderMode;
uniform vec4 uBaseColor;
uniform float uAlpha;

// 纹理
uniform sampler2D uMainTexture;
uniform bool uUseTexture;

// 模糊参数
uniform float uBlurScale;

// PBR 预留参数 (未来扩展)
uniform float uMetallic;
uniform float uRoughness;
uniform float uAO;
uniform vec4 uEmissive;

// 输出
layout(location = 0) out vec4 FragColor;

// 高斯模糊权重
const float BLUR_WEIGHTS[5] = float[](0.227027, 0.1945946, 0.1216216, 0.054054, 0.016216);

vec4 sampleBlur(bool horizontal) {
    vec2 texOffset = uBlurScale / textureSize(uMainTexture, 0);
    vec3 result = texture(uMainTexture, vTexCoord).rgb * BLUR_WEIGHTS[0];

    for (int i = 1; i < 5; ++i) {
        vec2 offset = horizontal
            ? vec2(texOffset.x * float(i), 0.0)
            : vec2(0.0, texOffset.y * float(i));
        result += texture(uMainTexture, vTexCoord + offset).rgb * BLUR_WEIGHTS[i];
        result += texture(uMainTexture, vTexCoord - offset).rgb * BLUR_WEIGHTS[i];
    }

    return vec4(result, 1.0);
}

void main()
{
    vec4 finalColor;

    switch (uRenderMode) {
        case MODE_COLOR:
            // 纯色模式：使用顶点颜色或基础颜色
            finalColor = vColor.a > 0.0 ? vColor : uBaseColor;
            break;

        case MODE_TEXTURE:
            // 纹理模式：纯纹理采样
            finalColor = texture(uMainTexture, vTexCoord);
            break;

        case MODE_TEXTURE_COLOR:
            // 纹理 + 颜色调制
            vec4 texColor = texture(uMainTexture, vTexCoord);
            vec4 modColor = vColor.a > 0.0 ? vColor : uBaseColor;
            finalColor = texColor * modColor;
            break;

        case MODE_BLUR_H:
            // 水平模糊
            finalColor = sampleBlur(true);
            break;

        case MODE_BLUR_V:
            // 垂直模糊
            finalColor = sampleBlur(false);
            break;

        default:
            finalColor = vec4(1.0, 0.0, 1.0, 1.0); // 错误：洋红色
            break;
    }

    // 应用全局 alpha
    finalColor.a *= uAlpha;

    FragColor = finalColor;
}
