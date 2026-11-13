#version 150

layout(std140) uniform MenuData {
    vec4 ResolutionTime;
    vec4 AccentColor;
};

in vec2 uv;

out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);

    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));

    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 5; i++) {
        value += noise(p) * amp;
        p = p * 2.04 + vec2(7.1, -3.4);
        amp *= 0.5;
    }
    return value;
}

float ray(vec2 p, float x, float width, float power) {
    float beam = exp(-abs(p.x - x) / width);
    float fade = smoothstep(1.02, 0.28, p.y);
    return beam * fade * power;
}

float star(vec2 p, vec2 id, float time) {
    vec2 pos = vec2(hash(id), hash(id + 8.17));
    float twinkle = 0.45 + 0.55 * sin(time * (0.8 + hash(id + 3.0) * 1.8) + hash(id + 5.0) * 6.283);
    float d = distance(p, pos);
    return smoothstep(0.0065, 0.0, d) * twinkle;
}

float energyLine(vec2 p, float offset, float time) {
    float y = p.y;
    float x = 0.5 + sin(y * 7.0 + time * 0.34 + offset) * 0.055 + offset * 0.035;
    float d = abs(p.x - x);
    float sideMask = smoothstep(0.52, 0.05, abs(p.x - 0.5));
    return smoothstep(0.010, 0.0, d) * sideMask * smoothstep(0.05, 0.45, y);
}

void main() {
    vec2 resolution = max(ResolutionTime.xy, vec2(1.0));
    float time = ResolutionTime.z;
    float intensity = max(ResolutionTime.w, 0.0);

    vec2 p = uv;
    vec2 aspect = vec2(resolution.x / resolution.y, 1.0);
    vec2 centered = (uv - 0.5) * aspect;

    vec3 accent = max(AccentColor.rgb, vec3(0.46, 0.22, 0.78));
    vec3 blackPurple = vec3(0.010, 0.006, 0.016);
    vec3 velvet = vec3(0.040, 0.018, 0.060);
    vec3 royal = vec3(0.120, 0.050, 0.185);

    float mist = fbm(centered * 2.1 + vec2(time * 0.025, -time * 0.018));
    float deepMist = fbm(centered * 4.2 - vec2(time * 0.018, time * 0.015));
    float vignette = smoothstep(0.95, 0.18, length(centered));

    vec3 color = mix(blackPurple, velvet, mist * 0.75);
    color = mix(color, royal, deepMist * 0.20);

    float rays = 0.0;
    rays += ray(p, 0.43 + sin(time * 0.19) * 0.03, 0.030, 1.00);
    rays += ray(p, 0.55 + sin(time * 0.16 + 2.0) * 0.04, 0.026, 0.82);
    rays += ray(p, 0.64 + sin(time * 0.13 + 4.0) * 0.035, 0.034, 0.64);
    rays *= smoothstep(1.0, 0.54, p.y);

    float dust = 0.0;
    for (int i = 0; i < 44; i++) {
        float fi = float(i);
        vec2 id = vec2(fi, fi * 2.37);
        vec2 drift = vec2(time * (0.004 + hash(id) * 0.008), -time * 0.006);
        dust += star(fract(p + drift), id, time);
    }

    float lines = energyLine(p, -3.2, time) + energyLine(p, 2.6, time + 1.4);
    float orb1 = smoothstep(0.11, 0.0, length(centered - vec2(-0.16, -0.18))) * 0.42;
    float orb2 = smoothstep(0.16, 0.0, length(centered - vec2(0.22, 0.11))) * 0.22;
    float sigil = smoothstep(0.045, 0.038, abs(length(centered - vec2(0.0, 0.16)) - 0.055)) * 0.30;

    color += accent * rays * 0.32 * intensity;
    color += accent * dust * 0.75 * intensity;
    color += accent * lines * 0.20 * intensity;
    color += accent * (orb1 + orb2 + sigil) * intensity;
    color *= 0.36 + vignette * 0.76;

    float grain = hash(uv * resolution + time) - 0.5;
    color += grain * 0.018;

    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
