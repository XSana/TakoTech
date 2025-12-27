#version 120

// 从顶点着色器接收
varying vec4 vColor;

// 参数
uniform float uAlpha;

void main()
{
    vec4 finalColor = vColor;
    finalColor.a *= uAlpha;
    gl_FragColor = finalColor;
}
