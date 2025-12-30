#version 330 core

// particle_mesh.frag - Mesh particle fragment shader
// Simple diffuse lighting for 3D mesh particles

in vec3 vNormal;
in vec4 vColor;
in float vLifePercent;

out vec4 fragColor;

void main() {
    // Normalize the interpolated normal
    vec3 normal = normalize(vNormal);

    // Simple directional light (sun-like)
    vec3 lightDir = normalize(vec3(0.3, 1.0, 0.5));

    // Diffuse lighting
    float diff = max(dot(normal, lightDir), 0.0);

    // Ambient + diffuse
    float ambient = 0.3;
    float lighting = ambient + diff * 0.7;

    // Apply lighting to color
    vec3 litColor = vColor.rgb * lighting;

    // Output with original alpha
    fragColor = vec4(litColor, vColor.a);

    // Discard transparent pixels
    if (fragColor.a < 0.01) discard;
}
