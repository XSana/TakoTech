#version 330 core

// particle.vert - Particle vertex shader
// Billboard rendering with rotation and size variation

// Quad vertex attributes
layout(location = 0) in vec2 aQuadPos;   // Quad local position (-0.5 to 0.5)
layout(location = 1) in vec2 aQuadUV;    // Texture coordinates

// Instance attributes (from SSBO or VBO)
layout(location = 2) in vec4 aPosition;  // xyz: world position, w: current lifetime
layout(location = 3) in vec4 aVelocity;  // xyz: velocity, w: max lifetime
layout(location = 4) in vec4 aColor;     // rgba
layout(location = 5) in vec4 aParams;    // x: size, y: rotation, z: type, w: angular velocity

// Uniforms
uniform mat4 uViewMatrix;
uniform mat4 uProjMatrix;
uniform vec3 uCameraPos;
uniform int uRenderMode;  // 0: billboard, 1: stretched billboard, 2: horizontal

// Output to fragment shader
out vec2 vUV;
out vec4 vColor;
out float vLifePercent;
out float vParticleType;

// Render mode constants
const int MODE_BILLBOARD = 0;
const int MODE_STRETCHED = 1;
const int MODE_HORIZONTAL = 2;

void main() {
    // Check if alive
    if (aPosition.w <= 0.0) {
        // Move dead particles far away (will be clipped)
        gl_Position = vec4(0.0, 0.0, -1000.0, 1.0);
        return;
    }

    float size = aParams.x;
    float rotation = aParams.y;
    float lifePercent = 1.0 - (aPosition.w / aVelocity.w);

    // Rotate quad vertex
    float c = cos(rotation);
    float s = sin(rotation);
    vec2 rotatedPos = vec2(
        aQuadPos.x * c - aQuadPos.y * s,
        aQuadPos.x * s + aQuadPos.y * c
    );

    // Convert particle world position to camera-relative position
    // (MC's ModelView matrix in RenderWorldLastEvent only contains rotation, not translation)
    vec3 particleRelativePos = aPosition.xyz - uCameraPos;

    // Transform particle center to view space
    vec4 viewCenter = uViewMatrix * vec4(particleRelativePos, 1.0);

    // In view space: right = (1,0,0), up = (0,1,0)
    // This ensures billboard always faces camera correctly
    vec3 viewPos;

    if (uRenderMode == MODE_BILLBOARD) {
        // Standard billboard in view space
        viewPos = viewCenter.xyz;
        viewPos.x += rotatedPos.x * size;
        viewPos.y += rotatedPos.y * size;
    }
    else if (uRenderMode == MODE_STRETCHED) {
        // Stretched billboard: stretch along velocity direction
        vec3 velocity = aVelocity.xyz;
        float speed = length(velocity);

        if (speed > 0.001) {
            // Transform velocity to view space for stretching
            vec3 viewVelocity = mat3(uViewMatrix) * velocity;
            vec3 stretchDir = normalize(viewVelocity);
            vec3 perpDir = normalize(vec3(-stretchDir.y, stretchDir.x, 0.0));

            float stretchFactor = 1.0 + speed * 0.1;
            viewPos = viewCenter.xyz;
            viewPos += perpDir * rotatedPos.x * size;
            viewPos += stretchDir * rotatedPos.y * size * stretchFactor;
        } else {
            // Speed too small, use standard billboard
            viewPos = viewCenter.xyz;
            viewPos.x += rotatedPos.x * size;
            viewPos.y += rotatedPos.y * size;
        }
    }
    else if (uRenderMode == MODE_HORIZONTAL) {
        // Horizontal particles (ground effects) - transform world axes to view space
        vec3 worldRight = vec3(1.0, 0.0, 0.0);
        vec3 worldForward = vec3(0.0, 0.0, 1.0);
        vec3 viewRight = mat3(uViewMatrix) * worldRight;
        vec3 viewForward = mat3(uViewMatrix) * worldForward;

        viewPos = viewCenter.xyz;
        viewPos += viewRight * rotatedPos.x * size;
        viewPos += viewForward * rotatedPos.y * size;
    }
    else {
        // Default billboard
        viewPos = viewCenter.xyz;
        viewPos.x += rotatedPos.x * size;
        viewPos.y += rotatedPos.y * size;
    }

    // Apply projection only (view transform already applied)
    gl_Position = uProjMatrix * vec4(viewPos, 1.0);

    // Pass to fragment shader
    vUV = aQuadUV;
    vColor = aColor;
    vLifePercent = lifePercent;
    vParticleType = aParams.z;
}
