#version 330 core

in vec2 TexCoord;

uniform sampler2D mainTexture;

layout(location = 0) out vec4 FragColor;

void main()
{
    FragColor = texture(mainTexture, TexCoord);
}
