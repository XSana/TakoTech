#version 330 core

// From vertex shader
in vec2 vTexCoord;

// Output
out vec4 FragColor;

// Texture
uniform sampler2D uSourceTexture;

// Parameters
uniform vec2 uTexelSize;      // 1.0 / texture size
uniform int uHorizontal;      // Horizontal blur (1) or vertical (0)
uniform float uBlurScale;     // Blur scale (default 1.0)

void main()
{
    // 9-tap Gaussian weights
    float weight0 = 0.227027;
    float weight1 = 0.1945946;
    float weight2 = 0.1216216;
    float weight3 = 0.054054;
    float weight4 = 0.016216;

    vec2 texOffset = uTexelSize * uBlurScale;
    vec3 result = texture(uSourceTexture, vTexCoord).rgb * weight0;

    if (uHorizontal == 1)
    {
        result += texture(uSourceTexture, vTexCoord + vec2(texOffset.x * 1.0, 0.0)).rgb * weight1;
        result += texture(uSourceTexture, vTexCoord - vec2(texOffset.x * 1.0, 0.0)).rgb * weight1;
        result += texture(uSourceTexture, vTexCoord + vec2(texOffset.x * 2.0, 0.0)).rgb * weight2;
        result += texture(uSourceTexture, vTexCoord - vec2(texOffset.x * 2.0, 0.0)).rgb * weight2;
        result += texture(uSourceTexture, vTexCoord + vec2(texOffset.x * 3.0, 0.0)).rgb * weight3;
        result += texture(uSourceTexture, vTexCoord - vec2(texOffset.x * 3.0, 0.0)).rgb * weight3;
        result += texture(uSourceTexture, vTexCoord + vec2(texOffset.x * 4.0, 0.0)).rgb * weight4;
        result += texture(uSourceTexture, vTexCoord - vec2(texOffset.x * 4.0, 0.0)).rgb * weight4;
    }
    else
    {
        result += texture(uSourceTexture, vTexCoord + vec2(0.0, texOffset.y * 1.0)).rgb * weight1;
        result += texture(uSourceTexture, vTexCoord - vec2(0.0, texOffset.y * 1.0)).rgb * weight1;
        result += texture(uSourceTexture, vTexCoord + vec2(0.0, texOffset.y * 2.0)).rgb * weight2;
        result += texture(uSourceTexture, vTexCoord - vec2(0.0, texOffset.y * 2.0)).rgb * weight2;
        result += texture(uSourceTexture, vTexCoord + vec2(0.0, texOffset.y * 3.0)).rgb * weight3;
        result += texture(uSourceTexture, vTexCoord - vec2(0.0, texOffset.y * 3.0)).rgb * weight3;
        result += texture(uSourceTexture, vTexCoord + vec2(0.0, texOffset.y * 4.0)).rgb * weight4;
        result += texture(uSourceTexture, vTexCoord - vec2(0.0, texOffset.y * 4.0)).rgb * weight4;
    }

    FragColor = vec4(result, 1.0);
}
