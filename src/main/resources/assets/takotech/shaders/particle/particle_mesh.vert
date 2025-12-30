#version 330 core

// particle_mesh.vert - Mesh particle vertex shader
// Renders 3D mesh geometry for each particle instance

// Mesh vertex attributes
layout(location = 0) in vec3 aPos;      // Mesh vertex position
layout(location = 1) in vec3 aNormal;   // Mesh vertex normal

// Instance attributes (from ParticleBuffer SSBO)
layout(location = 2) in vec4 aPosition;  // xyz: world position, w: current lifetime
layout(location = 3) in vec4 aVelocity;  // xyz: velocity, w: max lifetime
layout(location = 4) in vec4 aColor;     // rgba
layout(location = 5) in vec4 aParams;    // x: size, y: rotation, z: type, w: angular velocity

// Uniforms
uniform mat4 uViewMatrix;
uniform mat4 uProjMatrix;
uniform vec3 uCameraPos;

// Output to fragment shader
out vec3 vNormal;
out vec4 vColor;
out float vLifePercent;

void main() {
    // Check if alive
    if (aPosition.w <= 0.0) {
        gl_Position = vec4(0.0, 0.0, -1000.0, 1.0);
        return;
    }

    float size = aParams.x;
    float rotation = aParams.y;
    float lifePercent = 1.0 - (aPosition.w / aVelocity.w);

    // Build rotation matrix (Y-axis rotation)
    float c = cos(rotation);
    float s = sin(rotation);
    mat3 rotMat = mat3(
        c,  0.0, s,
        0.0, 1.0, 0.0,
        -s, 0.0, c
    );

    // Transform vertex: scale -> rotate -> translate
    vec3 scaledPos = aPos * size;
    vec3 rotatedPos = rotMat * scaledPos;

    // Convert to camera-relative position (MC's ModelView only has rotation)
    vec3 worldPos = rotatedPos + aPosition.xyz - uCameraPos;

    // Apply view and projection
    gl_Position = uProjMatrix * uViewMatrix * vec4(worldPos, 1.0);

    // Transform normal (rotation only, no translation)
    vNormal = rotMat * aNormal;
    vColor = aColor;
    vLifePercent = lifePercent;
}
