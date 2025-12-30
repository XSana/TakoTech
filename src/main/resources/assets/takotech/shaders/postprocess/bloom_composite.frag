#version 330 core

// From vertex shader
in vec2 vTexCoord;

// Output
out vec4 FragColor;

// Textures
uniform sampler2D uSceneTexture;   // Original scene
uniform sampler2D uBloomTexture;   // Blurred bloom

// Parameters
uniform float uBloomIntensity;     // Bloom intensity (default 1.0)
uniform float uExposure;           // Exposure (default 1.0)
uniform int uEnableTonemap;        // Enable tonemapping (0 or 1)
uniform float uBloomAlphaScale;    // Bloom alpha scale for background blending (default 2.0)

// ACES tonemapping
vec3 toneMapACES(vec3 x)
{
    float a = 2.51;
    float b = 0.03;
    float c = 2.43;
    float d = 0.59;
    float e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}

void main()
{
    vec4 sceneTexel = texture(uSceneTexture, vTexCoord);
    vec3 sceneColor = sceneTexel.rgb;
    float sceneAlpha = sceneTexel.a;

    vec3 bloomColor = texture(uBloomTexture, vTexCoord).rgb;

    // Composite
    vec3 result = sceneColor + bloomColor * uBloomIntensity;

    // Exposure
    result *= uExposure;

    // Tonemapping
    if (uEnableTonemap == 1)
    {
        result = toneMapACES(result);
    }

    // Calculate bloom contribution to alpha
    // This allows bloom glow to spread beyond object boundaries onto the MC world
    float bloomLuminance = dot(bloomColor, vec3(0.2126, 0.7152, 0.0722));
    float bloomAlpha = clamp(bloomLuminance * uBloomIntensity * uBloomAlphaScale, 0.0, 1.0);
    float finalAlpha = max(sceneAlpha, bloomAlpha);

    // Output final color with combined alpha
    FragColor = vec4(result, finalAlpha);
}
