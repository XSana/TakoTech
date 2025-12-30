#version 420 core

// Vertex attributes
layout (location = 0) in vec2 aPos;
layout (location = 1) in vec2 aTexCoord;
layout (location = 2) in vec4 aColor;

// Output to fragment shader
out vec2 vTexCoord;
out vec4 vColor;

// GlobalUniforms UBO (binding = 0)
layout(std140, binding = 0) uniform GlobalUniforms {
    mat4 uProjection;    // offset 0,   size 64
    mat4 uView;          // offset 64,  size 64
    vec4 uScreenSize;    // offset 128, size 16 [width, height, 1/width, 1/height]
    vec4 uTime;          // offset 144, size 16 [totalTime, deltaTime, frameCount, 0]
};

// Per-object/material uniforms
uniform mat4 uModel;
uniform bool uUseProjection;
uniform bool uUseView;

void main()
{
    vec4 position = vec4(aPos, 0.0, 1.0);

    if (uUseProjection) {
        mat4 transform = uProjection;
        if (uUseView) {
            transform = transform * uView;
        }
        gl_Position = transform * uModel * position;
    } else {
        gl_Position = uModel * position;
    }

    vTexCoord = aTexCoord;
    vColor = aColor;
}
