#version 330 core

in vec2 texCoord;
out vec4 fragColor;

layout(std140) uniform ShaderSkyData {
    vec4 uParams;

    vec4 uColorA;

    vec4 uColorB;

    vec4 uGeo;

    vec4 uRight;

    vec4 uUp;

    vec4 uFwd;
};

float hash3(vec3 p) {
    p = fract(p * 0.3183099 + 0.1);
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

float valueNoise3(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    vec3 u = f * f * (3.0 - 2.0 * f);
    float n000 = hash3(i + vec3(0.0, 0.0, 0.0));
    float n100 = hash3(i + vec3(1.0, 0.0, 0.0));
    float n010 = hash3(i + vec3(0.0, 1.0, 0.0));
    float n110 = hash3(i + vec3(1.0, 1.0, 0.0));
    float n001 = hash3(i + vec3(0.0, 0.0, 1.0));
    float n101 = hash3(i + vec3(1.0, 0.0, 1.0));
    float n011 = hash3(i + vec3(0.0, 1.0, 1.0));
    float n111 = hash3(i + vec3(1.0, 1.0, 1.0));
    float x00 = mix(n000, n100, u.x);
    float x10 = mix(n010, n110, u.x);
    float x01 = mix(n001, n101, u.x);
    float x11 = mix(n011, n111, u.x);
    float y0 = mix(x00, x10, u.y);
    float y1 = mix(x01, x11, u.y);
    return mix(y0, y1, u.z);
}

float fbm3_lo(vec3 p) {
    float v = valueNoise3(p) * 0.5;
    v += valueNoise3(p * 2.02) * 0.25;
    return v;
}

float fbm3(vec3 p) {
    float v = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 6; i++) {
        v += amp * valueNoise3(p);
        p *= 2.02;
        amp *= 0.5;
    }
    return v;
}

mat3 rotY(float a) {
    float s = sin(a);
    float c = cos(a);
    return mat3(c, 0.0, -s, 0.0, 1.0, 0.0, s, 0.0, c);
}

vec3 modeClouds(vec3 d, float t, float scale) {
    vec3 p = d * scale;

    vec3 q = vec3(fbm3_lo(p + vec3(0.0, t * 0.15, 0.0)),
                  fbm3_lo(p + vec3(5.2, 1.3, 2.1)),
                  fbm3_lo(p + vec3(1.7, 8.3, 4.4)));
    vec3 r = vec3(fbm3_lo(p + 4.0 * q + vec3(1.7, 9.2, 0.0) + t * 0.12),
                  fbm3_lo(p + 4.0 * q + vec3(8.3, 2.8, 5.1)),
                  fbm3_lo(p + 4.0 * q + vec3(3.1, 6.7, 1.9)));
    float f = fbm3(p + 4.0 * r);
    float density = clamp(f * 1.4, 0.0, 1.0);

    vec3 lightDir = normalize(vec3(-0.6, 0.5, -0.8));
    float eps = 0.35;
    vec3 ps = p + lightDir * eps;
    vec3 q2 = vec3(fbm3_lo(ps + vec3(0.0, t * 0.15, 0.0)),
                   fbm3_lo(ps + vec3(5.2, 1.3, 2.1)),
                   fbm3_lo(ps + vec3(1.7, 8.3, 4.4)));
    vec3 r2 = vec3(fbm3_lo(ps + 4.0 * q2 + vec3(1.7, 9.2, 0.0) + t * 0.12),
                   fbm3_lo(ps + 4.0 * q2 + vec3(8.3, 2.8, 5.1)),
                   fbm3_lo(ps + 4.0 * q2 + vec3(3.1, 6.7, 1.9)));
    float fLit = fbm3(ps + 4.0 * r2);
    float shade = clamp((f - fLit) * 3.0, -1.0, 1.0);

    vec3 col = mix(uColorB.rgb, uColorA.rgb, density);
    col = mix(col, uColorA.rgb, smoothstep(0.55, 1.0, density));

    vec3 litColor = min(uColorA.rgb * 1.6 + 0.15, vec3(1.0));
    vec3 darkColor = uColorB.rgb * 0.45;
    col = mix(col, darkColor, clamp(-shade, 0.0, 1.0) * density);
    col = mix(col, litColor, clamp(shade, 0.0, 1.0) * density);

    float rim = smoothstep(0.7, 1.0, density) * clamp(shade, 0.0, 1.0);
    col += litColor * rim * 0.4;

    return col;
}

vec3 modePlasma(vec3 d, float t, float scale) {
    vec3 p = d * scale;
    float v = sin(p.x + t);
    v += sin((p.z + t) * 0.5);
    v += sin((p.x + p.z + t) * 0.5);
    v += sin(length(p.xz) + t);
    v += sin(p.y * 1.3 + t * 0.7);
    v *= 0.2;
    float m = 0.5 + 0.5 * sin(v * 3.14159);
    return mix(uColorB.rgb, uColorA.rgb, m);
}

vec3 modeAurora(vec3 d, float t, float scale) {
    vec3 col = vec3(0.0);

    float height = d.y;
    for (int i = 0; i < 3; i++) {
        float fi = float(i);
        float wave = sin(d.x * (scale * 0.5) + d.z * (scale * 0.4) + t * (0.6 + fi * 0.2) + fi * 1.7) * 0.15;
        float band = 0.02 / abs(height - 0.35 - wave + fi * 0.08);
        band *= (0.4 + 0.3 * sin(t + fi));
        vec3 c = mix(uColorA.rgb, uColorB.rgb, fi / 3.0);
        col += c * band;
    }
    return col;
}

vec3 modeNebula(vec3 d, float t, float scale) {
    vec3 p = d * scale;
    p = rotY(t * 0.05) * p;
    float dd = length(p.xy);
    float n = fbm3(p + vec3(fbm3_lo(p * 1.5 + t * 0.1), fbm3_lo(p * 1.5 - t * 0.1), fbm3_lo(p * 1.3)));
    float glow = pow(clamp(1.0 - dd * 0.12, 0.0, 1.0), 2.0);
    vec3 col = mix(uColorB.rgb, uColorA.rgb, n);
    col += uColorA.rgb * glow * n;
    return col;
}

vec3 modeWaves(vec3 d, float t, float scale) {
    vec3 p = d * scale;
    float v = 0.0;
    for (int i = 1; i <= 4; i++) {
        float fi = float(i);
        v += sin(p.x * fi * 0.5 + p.z * fi * 0.3 + t * fi * 0.4) / fi;
    }
    float m = 0.5 + 0.5 * sin(p.y + v * 2.0 + t);
    return mix(uColorB.rgb, uColorA.rgb, m);
}

void main() {
    float t = uParams.x;
    float scale = max(0.1, uParams.y);
    float intensity = uParams.z;
    int mode = int(uParams.w + 0.5);

    vec2 ndc = texCoord * 2.0 - 1.0;
    vec3 d = normalize(
        uRight.xyz * (ndc.x * uGeo.y) +
        uUp.xyz    * (ndc.y * uGeo.z) +
        uFwd.xyz);

    vec3 col;
    if (mode == 0)      col = modeClouds(d, t, scale);
    else if (mode == 1) col = modePlasma(d, t, scale);
    else if (mode == 2) col = modeAurora(d, t, scale);
    else if (mode == 3) col = modeNebula(d, t, scale);
    else                col = modeWaves(d, t, scale);

    col *= intensity;

    float opacity = clamp(uColorA.a, 0.0, 1.0);
    fragColor = vec4(col, opacity);
}
