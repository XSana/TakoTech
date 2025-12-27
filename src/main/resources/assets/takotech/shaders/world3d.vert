#version 120

// 顶点属性
attribute vec3 aPos;
attribute vec4 aColor;

// 输出到片元着色器
varying vec4 vColor;

// 变换矩阵
uniform mat4 uModelView;      // MC 的 ModelView 矩阵
uniform mat4 uProjection;     // MC 的投影矩阵

void main()
{
    gl_Position = uProjection * uModelView * vec4(aPos, 1.0);
    vColor = aColor;
}
