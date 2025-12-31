#version 120

// ============================================================================
// World3D Lit Vertex Shader
// Extends world3d.vert with MC lightmap coordinate and normal support.
// MC 1.7.10 right-hand coordinate system: +X=East, +Y=Up, +Z=South(toward player)
// ============================================================================

// Vertex attributes
attribute vec3 aPos;
attribute vec4 aColor;
attribute vec2 aLightCoord;  // MC lightmap coordinates (blockLight, skyLight)
attribute vec3 aNormal;      // Surface normal (MC right-hand coords)

// Output to fragment shader
varying vec4 vColor;
varying vec2 vLightCoord;
varying vec3 vNormal;
varying vec3 vWorldPos;

// Transform matrices
uniform mat4 uModelView;      // MC ModelView matrix
uniform mat4 uProjection;     // MC Projection matrix

void main()
{
    vec4 worldPos = uModelView * vec4(aPos, 1.0);
    gl_Position = uProjection * worldPos;

    vColor = aColor;
    vLightCoord = aLightCoord;
    // Transform normal to view space (using upper-left 3x3 of modelview)
    vNormal = normalize(mat3(uModelView) * aNormal);
    vWorldPos = worldPos.xyz;
}
