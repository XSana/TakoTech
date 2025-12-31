#version 120

// ============================================================================
// World3D Lit Fragment Shader
// Extends world3d.frag with MC lightmap sampling for realistic world lighting.
// Supports day/night cycle, block light sources, and normal-based shading.
// MC 1.7.10 right-hand coordinate system: +X=East, +Y=Up, +Z=South(toward player)
// ============================================================================

// From vertex shader
varying vec4 vColor;
varying vec2 vLightCoord;  // blockLight(U), skyLight(V) in 0-1 range
varying vec3 vNormal;      // Surface normal in view space
varying vec3 vWorldPos;    // Position in view space

// Parameters
uniform float uAlpha;
uniform sampler2D uLightmap;      // MC lightmap texture (16x16)
uniform bool uUseLighting;        // Enable/disable MC lighting
uniform float uLightIntensity;    // Light intensity multiplier (default 1.0)
uniform float uMinBrightness;     // Minimum brightness floor (default 0.1)
uniform bool uUseNormalShading;   // Enable normal-based directional shading

// Constants for texel center sampling
const float LIGHTMAP_SCALE = 15.0 / 16.0;
const float LIGHTMAP_OFFSET = 0.5 / 16.0;

// Luminance weights (Rec. 709)
const vec3 LUMINANCE_WEIGHTS = vec3(0.2126, 0.7152, 0.0722);

// Simulated sun/sky light direction (from upper-south, matching MC default)
// In view space, this approximates daylight direction
const vec3 SUN_DIR = normalize(vec3(0.2, 0.8, 0.4));

void main()
{
    vec4 finalColor = vColor;
    vec3 normal = normalize(vNormal);

    // Normal-based face culling using view direction
    // viewDir: from fragment to camera (in view space, camera is at origin)
    vec3 viewDir = normalize(-vWorldPos);
    // If normal faces away from camera (dot < 0), it's a back face
    if (dot(normal, viewDir) < 0.0) {
        discard;
    }

    if (uUseLighting) {
        // Sample MC lightmap
        // vLightCoord needs to be offset to sample texel centers
        // This prevents edge artifacts from bilinear filtering
        vec2 lightUV = vLightCoord * LIGHTMAP_SCALE + LIGHTMAP_OFFSET;
        vec3 lightColor = texture2D(uLightmap, lightUV).rgb;

        // Calculate luminance for intensity scaling
        float luminance = dot(lightColor, LUMINANCE_WEIGHTS);

        // Apply intensity multiplier (default 1.0 if not set)
        float intensity = uLightIntensity > 0.0 ? uLightIntensity : 1.0;

        // Apply minimum brightness floor (default 0.1 if not set)
        float minBright = uMinBrightness > 0.0 ? uMinBrightness : 0.1;

        // Scale light with intensity and clamp to min brightness
        vec3 scaledLight = lightColor * intensity;
        float scaledLuminance = max(dot(scaledLight, LUMINANCE_WEIGHTS), minBright);

        // Preserve color tint while applying brightness
        vec3 finalLight = luminance > 0.001
            ? scaledLight * (scaledLuminance / max(dot(scaledLight, LUMINANCE_WEIGHTS), 0.001))
            : vec3(minBright);

        // Apply normal-based directional shading if enabled
        if (uUseNormalShading) {
            // Simple Lambertian diffuse with hemisphere ambient
            float NdotL = max(dot(normal, SUN_DIR), 0.0);

            // Hemisphere ambient: surfaces facing up get more sky light
            float skyFactor = normal.y * 0.5 + 0.5;  // 0.0 (down) to 1.0 (up)

            // Combine directional and ambient (weighted by sky light level)
            float skyLightLevel = vLightCoord.y;  // 0-1 sky light
            float directional = mix(0.6, 1.0, NdotL * skyLightLevel);
            float ambient = mix(0.4, 0.7, skyFactor);

            // Final shading factor
            float shadingFactor = mix(ambient, directional, skyLightLevel * 0.5);
            finalLight *= shadingFactor;
        }

        finalColor.rgb *= finalLight;
    }

    finalColor.a *= uAlpha;
    gl_FragColor = finalColor;
}
