#version 330 core

in vec2 TexCoord;

uniform sampler2D mainTexture;
uniform bool isHorizontal;
uniform float blurScale;

out vec4 FragColor;

void main()
{
    float weight[5] = float[] (0.227027, 0.1945946, 0.1216216, 0.054054, 0.016216);

    vec2 tex_offset = blurScale / textureSize(mainTexture, 0);
    vec3 result = texture(mainTexture, TexCoord).rgb * weight[0];

    if (isHorizontal)
    {
        for (int i = 1; i < 5; ++i)
        {
            result += texture(mainTexture, TexCoord + vec2(tex_offset.x * i, 0.0)).rgb * weight[i];
            result += texture(mainTexture, TexCoord - vec2(tex_offset.x * i, 0.0)).rgb * weight[i];
        }
    }
    else
    {
        for (int i = 1; i < 5; ++i)
        {
            result += texture(mainTexture, TexCoord + vec2(0.0, tex_offset.y * i)).rgb * weight[i];
            result += texture(mainTexture, TexCoord - vec2(0.0, tex_offset.y * i)).rgb * weight[i];
        }
    }

    FragColor = vec4(result, 1.0);
}
