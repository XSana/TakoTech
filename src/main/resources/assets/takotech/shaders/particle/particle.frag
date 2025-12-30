#version 330 core

// particle.frag - Particle fragment shader
// Supports textures, color gradients, and soft particles

in vec2 vUV;
in vec4 vColor;
in float vLifePercent;
in float vParticleType;

// Uniforms
uniform sampler2D uTexture;
uniform int uHasTexture;
uniform int uSoftParticles;
uniform float uSoftDistance;
uniform sampler2D uDepthTexture;
uniform vec2 uScreenSize;
uniform float uNearPlane;
uniform float uFarPlane;

// Color gradient LUT
uniform sampler1D uColorLUT;
uniform int uUseColorLUT;

// Output
out vec4 fragColor;

// Linearize depth
float linearizeDepth(float depth) {
    float z = depth * 2.0 - 1.0;
    return (2.0 * uNearPlane * uFarPlane) / (uFarPlane + uNearPlane - z * (uFarPlane - uNearPlane));
}

void main() {
    vec4 texColor = vec4(1.0);

    if (uHasTexture == 1) {
        // Use texture
        texColor = texture(uTexture, vUV);
    } else {
        // Draw soft circle when no texture
        vec2 center = vUV - vec2(0.5);
        float dist = length(center);

        if (dist > 0.5) {
            discard;
        }

        // Soft edge
        texColor.a = smoothstep(0.5, 0.2, dist);

        // Optional: add radial gradient to make center brighter
        texColor.rgb = vec3(1.0 - dist * 0.3);
    }

    // Apply color
    vec4 finalColor;

    if (uUseColorLUT == 1) {
        // Use color LUT
        vec4 lutColor = texture(uColorLUT, vLifePercent);
        finalColor = texColor * lutColor;
    } else {
        // Use vertex color
        finalColor = texColor * vColor;
    }

    // Soft particles (requires depth buffer)
    if (uSoftParticles == 1 && uSoftDistance > 0.0) {
        vec2 screenCoord = gl_FragCoord.xy / uScreenSize;
        float sceneDepth = linearizeDepth(texture(uDepthTexture, screenCoord).r);
        float particleDepth = linearizeDepth(gl_FragCoord.z);

        float depthDiff = sceneDepth - particleDepth;
        float softFactor = smoothstep(0.0, uSoftDistance, depthDiff);

        finalColor.a *= softFactor;
    }

    // Discard nearly transparent pixels
    if (finalColor.a < 0.01) {
        discard;
    }

    fragColor = finalColor;
}
