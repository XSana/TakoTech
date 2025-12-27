#version 330 core

// 顶点属性
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in vec2 aTexCoord;
layout (location = 3) in vec3 aTangent;
layout (location = 4) in vec4 aColor;

// 输出到片元着色器
out vec3 vWorldPos;
out vec3 vNormal;
out vec2 vTexCoord;
out vec3 vTangent;
out vec3 vBitangent;
out vec4 vColor;

// GlobalUniforms UBO (binding = 0)
layout(std140, binding = 0) uniform GlobalUniforms {
    mat4 uProjection;
    mat4 uView;
    vec4 uScreenSize;
    vec4 uTime;
};

// Per-object uniforms
uniform mat4 uModel;
uniform mat3 uNormalMatrix;

void main()
{
    vec4 worldPos = uModel * vec4(aPos, 1.0);
    vWorldPos = worldPos.xyz;

    gl_Position = uProjection * uView * worldPos;

    // 变换法线到世界空间
    vNormal = normalize(uNormalMatrix * aNormal);
    vTangent = normalize(uNormalMatrix * aTangent);
    vBitangent = cross(vNormal, vTangent);

    vTexCoord = aTexCoord;
    vColor = aColor;
}
