#version 120

// Vertex attributes
attribute vec3 aPos;
attribute vec4 aColor;

// Output to fragment shader
varying vec4 vColor;

// Transform matrices
uniform mat4 uModelView;      // MC ModelView matrix
uniform mat4 uProjection;     // MC Projection matrix

void main()
{
    gl_Position = uProjection * uModelView * vec4(aPos, 1.0);
    vColor = aColor;
}
