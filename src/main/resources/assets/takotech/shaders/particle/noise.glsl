// noise.glsl - Noise function library
// For particle system turbulence and curl noise effects

// ==================== Hash Functions ====================

// Simple hash function
uint hash(uint x) {
    x += (x << 10u);
    x ^= (x >> 6u);
    x += (x << 3u);
    x ^= (x >> 11u);
    x += (x << 15u);
    return x;
}

uint hash(uvec2 v) {
    return hash(v.x ^ hash(v.y));
}

uint hash(uvec3 v) {
    return hash(v.x ^ hash(v.y) ^ hash(v.z));
}

// Float hash (0-1 range)
float hashFloat(uint x) {
    return float(hash(x)) / 4294967295.0;
}

float hashFloat(uvec2 v) {
    return float(hash(v)) / 4294967295.0;
}

float hashFloat(uvec3 v) {
    return float(hash(v)) / 4294967295.0;
}

// ==================== Gradient Functions ====================

// 2D gradient
vec2 grad2(vec2 p) {
    uvec2 i = uvec2(floor(p));
    float h = hashFloat(i);
    float angle = h * 6.283185307;
    return vec2(cos(angle), sin(angle));
}

// 3D gradient
vec3 grad3(vec3 p) {
    uvec3 i = uvec3(floor(p));
    uint h = hash(i);
    float theta = hashFloat(h) * 6.283185307;
    float phi = hashFloat(h + 1u) * 3.141592654;
    return vec3(
        sin(phi) * cos(theta),
        sin(phi) * sin(theta),
        cos(phi)
    );
}

// ==================== Simplex Noise ====================

// 2D Simplex noise
float simplex2D(vec2 p) {
    const float K1 = 0.366025404; // (sqrt(3)-1)/2
    const float K2 = 0.211324865; // (3-sqrt(3))/6

    vec2 i = floor(p + (p.x + p.y) * K1);
    vec2 a = p - i + (i.x + i.y) * K2;
    vec2 o = step(a.yx, a.xy);
    vec2 b = a - o + K2;
    vec2 c = a - 1.0 + 2.0 * K2;

    vec3 h = max(0.5 - vec3(dot(a, a), dot(b, b), dot(c, c)), 0.0);
    vec3 n = h * h * h * h * vec3(
        dot(a, grad2(i)),
        dot(b, grad2(i + o)),
        dot(c, grad2(i + 1.0))
    );

    return dot(n, vec3(70.0));
}

// 3D Simplex noise (simplified)
float simplex3D(vec3 p) {
    const float K = 0.333333333;
    const float K2 = 0.166666667;

    vec3 i = floor(p + (p.x + p.y + p.z) * K);
    vec3 d0 = p - i + (i.x + i.y + i.z) * K2;

    vec3 e = step(vec3(0.0), d0 - d0.yzx);
    vec3 i1 = e * (1.0 - e.zxy);
    vec3 i2 = 1.0 - e.zxy * (1.0 - e);

    vec3 d1 = d0 - i1 + K2;
    vec3 d2 = d0 - i2 + K;
    vec3 d3 = d0 - 0.5;

    vec4 h = max(0.6 - vec4(dot(d0, d0), dot(d1, d1), dot(d2, d2), dot(d3, d3)), 0.0);
    vec4 n = h * h * h * h * vec4(
        dot(d0, grad3(i)),
        dot(d1, grad3(i + i1)),
        dot(d2, grad3(i + i2)),
        dot(d3, grad3(i + 1.0))
    );

    return dot(n, vec4(52.0));
}

// ==================== Fractal Brownian Motion (FBM) ====================

float fbm2D(vec2 p, int octaves) {
    float value = 0.0;
    float amplitude = 0.5;
    float frequency = 1.0;

    for (int i = 0; i < octaves; i++) {
        value += amplitude * simplex2D(p * frequency);
        amplitude *= 0.5;
        frequency *= 2.0;
    }

    return value;
}

float fbm3D(vec3 p, int octaves) {
    float value = 0.0;
    float amplitude = 0.5;
    float frequency = 1.0;

    for (int i = 0; i < octaves; i++) {
        value += amplitude * simplex3D(p * frequency);
        amplitude *= 0.5;
        frequency *= 2.0;
    }

    return value;
}

// ==================== Curl Noise ====================

// 2D curl noise
vec2 curlNoise2D(vec2 p) {
    const float e = 0.001;

    float n1 = simplex2D(p + vec2(e, 0.0));
    float n2 = simplex2D(p - vec2(e, 0.0));
    float n3 = simplex2D(p + vec2(0.0, e));
    float n4 = simplex2D(p - vec2(0.0, e));

    float dx = (n3 - n4) / (2.0 * e);
    float dy = -(n1 - n2) / (2.0 * e);

    return vec2(dx, dy);
}

// 3D curl noise (divergence-free)
vec3 curlNoise3D(vec3 p) {
    const float e = 0.001;

    // Calculate gradients of three noise fields
    vec3 dx = vec3(e, 0.0, 0.0);
    vec3 dy = vec3(0.0, e, 0.0);
    vec3 dz = vec3(0.0, 0.0, e);

    // Noise field 1 (used to generate x and y components)
    float n1_px = simplex3D(p + dx);
    float n1_nx = simplex3D(p - dx);
    float n1_py = simplex3D(p + dy);
    float n1_ny = simplex3D(p - dy);
    float n1_pz = simplex3D(p + dz);
    float n1_nz = simplex3D(p - dz);

    // Offset to get second independent noise field
    vec3 offset = vec3(31.341, 57.127, 93.853);
    float n2_py = simplex3D(p + dy + offset);
    float n2_ny = simplex3D(p - dy + offset);
    float n2_pz = simplex3D(p + dz + offset);
    float n2_nz = simplex3D(p - dz + offset);
    float n2_px = simplex3D(p + dx + offset);
    float n2_nx = simplex3D(p - dx + offset);

    // Calculate curl (curl = nabla x F)
    float curlX = (n1_pz - n1_nz - n2_py + n2_ny) / (2.0 * e);
    float curlY = (n2_px - n2_nx - n1_pz + n1_nz) / (2.0 * e);
    float curlZ = (n1_py - n1_ny - n2_px + n2_nx) / (2.0 * e);

    return vec3(curlX, curlY, curlZ);
}

// ==================== Turbulence Noise ====================

float turbulence3D(vec3 p, int octaves) {
    float value = 0.0;
    float amplitude = 1.0;
    float frequency = 1.0;

    for (int i = 0; i < octaves; i++) {
        value += amplitude * abs(simplex3D(p * frequency));
        amplitude *= 0.5;
        frequency *= 2.0;
    }

    return value;
}

// ==================== Periodic Noise ====================

float periodicNoise3D(vec3 p, vec3 period) {
    // Create periodic noise using smooth interpolation
    vec3 pi = mod(floor(p), period);
    vec3 pf = fract(p);

    // 6th order curve interpolation
    vec3 u = pf * pf * pf * (pf * (pf * 6.0 - 15.0) + 10.0);

    // Get gradients at 8 corners
    float n000 = hashFloat(uvec3(mod(pi, period)));
    float n100 = hashFloat(uvec3(mod(pi + vec3(1, 0, 0), period)));
    float n010 = hashFloat(uvec3(mod(pi + vec3(0, 1, 0), period)));
    float n110 = hashFloat(uvec3(mod(pi + vec3(1, 1, 0), period)));
    float n001 = hashFloat(uvec3(mod(pi + vec3(0, 0, 1), period)));
    float n101 = hashFloat(uvec3(mod(pi + vec3(1, 0, 1), period)));
    float n011 = hashFloat(uvec3(mod(pi + vec3(0, 1, 1), period)));
    float n111 = hashFloat(uvec3(mod(pi + vec3(1, 1, 1), period)));

    // Trilinear interpolation
    float nx00 = mix(n000, n100, u.x);
    float nx10 = mix(n010, n110, u.x);
    float nx01 = mix(n001, n101, u.x);
    float nx11 = mix(n011, n111, u.x);

    float nxy0 = mix(nx00, nx10, u.y);
    float nxy1 = mix(nx01, nx11, u.y);

    return mix(nxy0, nxy1, u.z) * 2.0 - 1.0;
}
