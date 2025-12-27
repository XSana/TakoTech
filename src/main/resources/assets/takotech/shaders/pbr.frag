#version 330 core

// 常量
const float PI = 3.14159265359;

// 从顶点着色器接收
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

// 材质参数
uniform vec4 uAlbedo;
uniform float uMetallic;
uniform float uRoughness;
uniform float uAO;
uniform vec3 uEmissive;
uniform float uEmissiveIntensity;
uniform float uNormalStrength;
uniform float uF0;
uniform bool uUseVertexColor;

// 纹理开关
uniform bool uHasAlbedoMap;
uniform bool uHasNormalMap;
uniform bool uHasMetallicMap;
uniform bool uHasRoughnessMap;
uniform bool uHasAOMap;
uniform bool uHasEmissiveMap;
uniform bool uUseIBL;

// 纹理采样器
uniform sampler2D uAlbedoMap;
uniform sampler2D uNormalMap;
uniform sampler2D uMetallicMap;
uniform sampler2D uRoughnessMap;
uniform sampler2D uAOMap;
uniform sampler2D uEmissiveMap;

// IBL 纹理
uniform samplerCube uIrradianceMap;
uniform samplerCube uPrefilterMap;
uniform sampler2D uBrdfLUT;

// 光源 (简单点光源支持)
uniform vec3 uLightPositions[4];
uniform vec3 uLightColors[4];
uniform int uLightCount;

// 相机位置
uniform vec3 uCameraPos;

// 输出
layout(location = 0) out vec4 FragColor;

// ==================== PBR 函数 ====================

// 法线分布函数 (GGX/Trowbridge-Reitz)
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

// 几何函数 (Schlick-GGX)
float GeometrySchlickGGX(float NdotV, float roughness)
{
    float r = (roughness + 1.0);
    float k = (r * r) / 8.0;

    float num = NdotV;
    float denom = NdotV * (1.0 - k) + k;

    return num / denom;
}

// 几何函数 (Smith)
float GeometrySmith(vec3 N, vec3 V, vec3 L, float roughness)
{
    float NdotV = max(dot(N, V), 0.0);
    float NdotL = max(dot(N, L), 0.0);
    float ggx2 = GeometrySchlickGGX(NdotV, roughness);
    float ggx1 = GeometrySchlickGGX(NdotL, roughness);

    return ggx1 * ggx2;
}

// 菲涅尔方程 (Schlick 近似)
vec3 fresnelSchlick(float cosTheta, vec3 F0)
{
    return F0 + (1.0 - F0) * pow(clamp(1.0 - cosTheta, 0.0, 1.0), 5.0);
}

// 菲涅尔方程 (带粗糙度，用于 IBL)
vec3 fresnelSchlickRoughness(float cosTheta, vec3 F0, float roughness)
{
    return F0 + (max(vec3(1.0 - roughness), F0) - F0) * pow(clamp(1.0 - cosTheta, 0.0, 1.0), 5.0);
}

// 从法线贴图获取法线
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
    // 采样材质属性
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
    roughness = max(roughness, 0.04); // 防止除零

    float ao = uAO;
    if (uHasAOMap) {
        ao = texture(uAOMap, vTexCoord).r;
    }

    vec3 emissive = uEmissive;
    if (uHasEmissiveMap) {
        emissive = texture(uEmissiveMap, vTexCoord).rgb;
    }
    emissive *= uEmissiveIntensity;

    // 计算向量
    vec3 N = getNormalFromMap();
    vec3 V = normalize(uCameraPos - vWorldPos);
    vec3 R = reflect(-V, N);

    // 计算 F0 (表面反射率)
    vec3 F0 = vec3(uF0);
    F0 = mix(F0, albedo, metallic);

    // 直接光照
    vec3 Lo = vec3(0.0);
    for (int i = 0; i < uLightCount && i < 4; ++i)
    {
        // 计算每个光源的贡献
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

        // kS 是菲涅尔
        vec3 kS = F;
        // kD 是漫反射部分
        vec3 kD = vec3(1.0) - kS;
        // 金属没有漫反射
        kD *= 1.0 - metallic;

        float NdotL = max(dot(N, L), 0.0);

        Lo += (kD * albedo / PI + specular) * radiance * NdotL;
    }

    // 环境光照
    vec3 ambient;
    if (uUseIBL)
    {
        // IBL 漫反射
        vec3 F = fresnelSchlickRoughness(max(dot(N, V), 0.0), F0, roughness);
        vec3 kS = F;
        vec3 kD = 1.0 - kS;
        kD *= 1.0 - metallic;

        vec3 irradiance = texture(uIrradianceMap, N).rgb;
        vec3 diffuse = irradiance * albedo;

        // IBL 高光
        const float MAX_REFLECTION_LOD = 4.0;
        vec3 prefilteredColor = textureLod(uPrefilterMap, R, roughness * MAX_REFLECTION_LOD).rgb;
        vec2 brdf = texture(uBrdfLUT, vec2(max(dot(N, V), 0.0), roughness)).rg;
        vec3 specular = prefilteredColor * (F * brdf.x + brdf.y);

        ambient = (kD * diffuse + specular) * ao;
    }
    else
    {
        // 简单环境光
        ambient = vec3(0.03) * albedo * ao;
    }

    vec3 color = ambient + Lo + emissive;

    // HDR 色调映射 (Reinhard)
    color = color / (color + vec3(1.0));

    // Gamma 校正
    color = pow(color, vec3(1.0 / 2.2));

    FragColor = vec4(color, uAlbedo.a);
}
