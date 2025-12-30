#version 120

// From vertex shader
varying vec4 vColor;

// Parameters
uniform float uAlpha;         // Global alpha multiplier (default 1.0)
uniform float uEmissive;      // Emissive intensity for Bloom (default 1.0)

void main()
{
    vec4 finalColor = vColor;
    finalColor.a *= uAlpha;

    // Emissive effect - increase brightness to trigger Bloom
    finalColor.rgb *= uEmissive;

    gl_FragColor = finalColor;
}
