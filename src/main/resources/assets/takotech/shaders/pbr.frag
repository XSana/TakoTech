#version 420 core

// Constants
const float PI = 3.14159265359;

// From vertex shader
in vec3 vWorldPos;
in vec3 vNormal;
in vec2 vTexCoord;
in vec3 vTangent;
in vec3 vBitangent;
in vec4 vColor;

// GlobalUniforms UBO (binding = 0)
layout(std140, binding = 0) uniform GlobalUniforms {
    mat4 uProjection;
    mat4 uView;
    vec4 uScreenSize;
    vec4 uTime;
};

// Material parameters
uniform vec4 uAlbedo;
uniform float uMetallic;
uniform float uRoughness;
uniform float uAO;
uniform vec3 uEmissive;
uniform float uEmissiveIntensity;
uniform float uNormalStrength;
uniform float uF0;
uniform bool uUseVertexColor;

// Texture flags
uniform bool uHasAlbedoMap;
uniform bool uHasNormalMap;
uniform bool uHasMetallicMap;
uniform bool uHasRoughnessMap;
uniform bool uHasAOMap;
uniform bool uHasEmissiveMap;
uniform bool uUseIBL;

// Texture samplers
uniform sampler2D uAlbedoMap;
uniform sampler2D uNormalMap;
uniform sampler2D uMetallicMap;
uniform sampler2D uRoughnessMap;
uniform sampler2D uAOMap;
uniform sampler2D uEmissiveMap;

// IBL textures
uniform samplerCube uIrradianceMap;
uniform samplerCube uPrefilterMap;
uniform sampler2D uBrdfLUT;

// Lights (simple point light support)
uniform vec3 uLightPositions[4];
uniform vec3 uLightColors[4];
uniform int uLightCount;

// Camera position
uniform vec3 uCameraPos;

// Output
layout(location = 0) out vec4 FragColor;

// ==================== PBR Functions ====================

// Normal Distribution Function (GGX/Trowbridge-Reitz)
float DistributionGGX(vec3 N, vec3 H, float roughness)
{
    float a = roughness * roughness;
    float a2 = a * a;
    float NdotH = max(dot(N, H), 0.0);
    float NdotH2 = NdotH * NdotH;

    float num = a2;
    float denom = (NdotH2 * (a2 - 1.0) + 1.0);
    denom = PI * denom * denom;

    return num / denom;
}

// Geometry Function (Schlick-GGX)
float GeometrySchlickGGX(float NdotV, float roughness)
{
    float r = (roughness + 1.0);
    float k = (r * r) / 8.0;

    float num = NdotV;
    float denom = NdotV * (1.0 - k) + k;

    return num / denom;
}

// Geometry Function (Smith)
float GeometrySmith(vec3 N, vec3 V, vec3 L, float roughness)
{
    float NdotV = max(dot(N, V), 0.0);
    float NdotL = max(dot(N, L), 0.0);
    float ggx2 = GeometrySchlickGGX(NdotV, roughness);
    float ggx1 = GeometrySchlickGGX(NdotL, roughness);

    return ggx1 * ggx2;
}

// Fresnel Equation (Schlick approximation)
vec3 fresnelSchlick(float cosTheta, vec3 F0)
{
    return F0 + (1.0 - F0) * pow(clamp(1.0 - cosTheta, 0.0, 1.0), 5.0);
}

// Fresnel Equation with roughness (for IBL)
vec3 fresnelSchlickRoughness(float cosTheta, vec3 F0, float roughness)
{
    return F0 + (max(vec3(1.0 - roughness), F0) - F0) * pow(clamp(1.0 - cosTheta, 0.0, 1.0), 5.0);
}

// Get normal from normal map
vec3 getNormalFromMap()
{
    if (!uHasNormalMap) {
        return normalize(vNormal);
    }

    vec3 tangentNormal = texture(uNormalMap, vTexCoord).xyz * 2.0 - 1.0;
    tangentNormal.xy *= uNormalStrength;

    vec3 N = normalize(vNormal);
    vec3 T = normalize(vTangent);
    vec3 B = normalize(vBitangent);
    mat3 TBN = mat3(T, B, N);

    return normalize(TBN * tangentNormal);
}

void main()
{
    // Sample material properties
    vec3 albedo = uAlbedo.rgb;
    if (uHasAlbedoMap) {
        albedo = pow(texture(uAlbedoMap, vTexCoord).rgb, vec3(2.2)); // sRGB to linear
    }
    if (uUseVertexColor && vColor.a > 0.0) {
        albedo *= vColor.rgb;
    }

    float metallic = uMetallic;
    if (uHasMetallicMap) {
        metallic = texture(uMetallicMap, vTexCoord).r;
    }

    float roughness = uRoughness;
    if (uHasRoughnessMap) {
        roughness = texture(uRoughnessMap, vTexCoord).r;
    }
    roughness = max(roughness, 0.04); // Prevent division by zero

    float ao = uAO;
    if (uHasAOMap) {
        ao = texture(uAOMap, vTexCoord).r;
    }

    vec3 emissive = uEmissive;
    if (uHasEmissiveMap) {
        emissive = texture(uEmissiveMap, vTexCoord).rgb;
    }
    emissive *= uEmissiveIntensity;

    // Calculate vectors
    vec3 N = getNormalFromMap();
    vec3 V = normalize(uCameraPos - vWorldPos);
    vec3 R = reflect(-V, N);

    // Calculate F0 (surface reflectance)
    vec3 F0 = vec3(uF0);
    F0 = mix(F0, albedo, metallic);

    // Direct lighting
    vec3 Lo = vec3(0.0);
    for (int i = 0; i < uLightCount && i < 4; ++i)
    {
        // Calculate contribution from each light
        vec3 L = normalize(uLightPositions[i] - vWorldPos);
        vec3 H = normalize(V + L);
        float distance = length(uLightPositions[i] - vWorldPos);
        float attenuation = 1.0 / (distance * distance);
        vec3 radiance = uLightColors[i] * attenuation;

        // Cook-Torrance BRDF
        float NDF = DistributionGGX(N, H, roughness);
        float G = GeometrySmith(N, V, L, roughness);
        vec3 F = fresnelSchlick(max(dot(H, V), 0.0), F0);

        vec3 numerator = NDF * G * F;
        float denominator = 4.0 * max(dot(N, V), 0.0) * max(dot(N, L), 0.0) + 0.0001;
        vec3 specular = numerator / denominator;

        // kS is Fresnel
        vec3 kS = F;
        // kD is diffuse component
        vec3 kD = vec3(1.0) - kS;
        // Metals have no diffuse
        kD *= 1.0 - metallic;

        float NdotL = max(dot(N, L), 0.0);

        Lo += (kD * albedo / PI + specular) * radiance * NdotL;
    }

    // Ambient lighting
    vec3 ambient;
    if (uUseIBL)
    {
        // IBL diffuse
        vec3 F = fresnelSchlickRoughness(max(dot(N, V), 0.0), F0, roughness);
        vec3 kS = F;
        vec3 kD = 1.0 - kS;
        kD *= 1.0 - metallic;

        vec3 irradiance = texture(uIrradianceMap, N).rgb;
        vec3 diffuse = irradiance * albedo;

        // IBL specular
        const float MAX_REFLECTION_LOD = 4.0;
        vec3 prefilteredColor = textureLod(uPrefilterMap, R, roughness * MAX_REFLECTION_LOD).rgb;
        vec2 brdf = texture(uBrdfLUT, vec2(max(dot(N, V), 0.0), roughness)).rg;
        vec3 specular = prefilteredColor * (F * brdf.x + brdf.y);

        ambient = (kD * diffuse + specular) * ao;
    }
    else
    {
        // Simple ambient light
        ambient = vec3(0.03) * albedo * ao;
    }

    vec3 color = ambient + Lo + emissive;

    // HDR tone mapping (Reinhard)
    color = color / (color + vec3(1.0));

    // Gamma correction
    color = pow(color, vec3(1.0 / 2.2));

    FragColor = vec4(color, uAlbedo.a);
}
