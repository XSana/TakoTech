#version 330 core

// From vertex shader
in vec2 vTexCoord;

// Output
out vec4 FragColor;

// Texture
uniform sampler2D uSceneTexture;

// Parameters
uniform float uThreshold;     // Brightness threshold (default 0.8)
uniform float uSoftKnee;      // Soft knee coefficient (default 0.5)

void main()
{
    vec4 color = texture(uSceneTexture, vTexCoord);

    // Calculate brightness (perceptual luminance formula)
    float brightness = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));

    // Soft threshold (smooth transition)
    float knee = uThreshold * uSoftKnee;
    float soft = brightness - uThreshold + knee;
    soft = clamp(soft, 0.0, 2.0 * knee);
    soft = soft * soft / (4.0 * knee + 0.00001);

    float contribution = max(soft, brightness - uThreshold);
    contribution /= max(brightness, 0.00001);

    FragColor = vec4(color.rgb * contribution, color.a);
}
