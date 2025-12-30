#version 330 core

// Vertex attributes - must match VAO layout
layout(location = 0) in vec2 aPos;
layout(location = 1) in vec2 aTexCoord;

// Output to fragment shader
out vec2 vTexCoord;

void main()
{
    gl_Position = vec4(aPos, 0.0, 1.0);
    vTexCoord = aTexCoord;
}
