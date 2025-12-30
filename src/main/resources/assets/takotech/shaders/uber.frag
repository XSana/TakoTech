#version 420 core

// Render mode constants
const int MODE_COLOR = 0;           // Solid color
const int MODE_TEXTURE = 1;         // Texture only
const int MODE_TEXTURE_COLOR = 2;   // Texture * Color
const int MODE_BLUR_H = 3;          // Horizontal blur
const int MODE_BLUR_V = 4;          // Vertical blur

// From vertex shader
in vec2 vTexCoord;
in vec4 vColor;

// Material parameters
uniform int uRenderMode;
uniform vec4 uBaseColor;
uniform float uAlpha;

// Texture
uniform sampler2D uMainTexture;
uniform bool uUseTexture;

// Blur parameters
uniform float uBlurScale;

// PBR reserved parameters (future extension)
uniform float uMetallic;
uniform float uRoughness;
uniform float uAO;
uniform vec4 uEmissive;

// Output
layout(location = 0) out vec4 FragColor;

// Gaussian blur weights
const float BLUR_WEIGHTS[5] = float[](0.227027, 0.1945946, 0.1216216, 0.054054, 0.016216);

vec4 sampleBlur(bool horizontal) {
    vec2 texOffset = uBlurScale / vec2(textureSize(uMainTexture, 0));
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
            // Color mode: use vertex color or base color
            finalColor = vColor.a > 0.0 ? vColor : uBaseColor;
            break;

        case MODE_TEXTURE:
            // Texture mode: pure texture sampling
            finalColor = texture(uMainTexture, vTexCoord);
            break;

        case MODE_TEXTURE_COLOR:
            // Texture + color modulation
            vec4 texColor = texture(uMainTexture, vTexCoord);
            vec4 modColor = vColor.a > 0.0 ? vColor : uBaseColor;
            finalColor = texColor * modColor;
            break;

        case MODE_BLUR_H:
            // Horizontal blur
            finalColor = sampleBlur(true);
            break;

        case MODE_BLUR_V:
            // Vertical blur
            finalColor = sampleBlur(false);
            break;

        default:
            finalColor = vec4(1.0, 0.0, 1.0, 1.0); // Error: magenta
            break;
    }

    // Apply global alpha
    finalColor.a *= uAlpha;

    FragColor = finalColor;
}
