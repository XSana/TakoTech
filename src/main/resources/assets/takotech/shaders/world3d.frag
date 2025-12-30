#version 120

// From vertex shader
varying vec4 vColor;

// Parameters
uniform float uAlpha;

void main()
{
    vec4 finalColor = vColor;
    finalColor.a *= uAlpha;
    gl_FragColor = finalColor;
}
