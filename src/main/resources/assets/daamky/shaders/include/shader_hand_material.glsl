float shHash(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float shNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(shHash(i), shHash(i + vec2(1.0, 0.0)), u.x),
               mix(shHash(i + vec2(0.0, 1.0)), shHash(i + vec2(1.0, 1.0)), u.x), u.y);
}

float shFbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 4; i++) {
        value += shNoise(p) * amplitude;
        p = p * 2.03 + vec2(7.1, 13.7);
        amplitude *= 0.5;
    }
    return value;
}

vec2 shaderHandObjectUv(vec2 textureUv, vec2 texturePixels) {
    // Convert atlas UVs to 16-pixel texture-tile units. These coordinates are
    // attached to the model texture and never depend on the camera transform.
    return textureUv * texturePixels / 16.0;
}

vec3 shCosmic(vec2 uv, float time) {
    float cloud = shFbm(uv * vec2(2.8, 3.7) + vec2(time * 0.018, -time * 0.012));
    vec3 color = mix(vec3(0.003, 0.009, 0.032), vec3(0.012, 0.070, 0.190), cloud);
    float sweep = fract(uv.y + uv.x * 0.24 + time * 0.19);
    float core = exp(-pow((sweep - 0.54) / 0.018, 2.0));
    float glow = exp(-pow((sweep - 0.54) / 0.075, 2.0));
    return color + vec3(0.12, 0.38, 0.94) * glow * 0.72 + vec3(0.90, 0.97, 1.0) * core * 1.25;
}

vec3 shPlasma(vec2 uv, float time) {
    vec2 p = uv * vec2(5.2, 7.4) + vec2(time * 0.16, -time * 0.22);
    float field = shFbm(p + shFbm(p * 0.72) * 2.6);
    float veins = 1.0 - smoothstep(0.020, 0.095, abs(field - 0.52));
    vec3 base = mix(vec3(0.002, 0.025, 0.040), vec3(0.010, 0.130, 0.180), field);
    return base + vec3(0.08, 0.74, 1.0) * veins * (0.68 + 0.32 * sin(time * 2.8 + uv.y * 16.0));
}

vec3 shMolten(vec2 uv, float time) {
    vec2 p = uv * vec2(4.2, 6.0);
    p += vec2(shFbm(p + time * 0.10), shFbm(p - time * 0.13)) * 1.8;
    float cells = shFbm(p * 1.35 + vec2(0.0, -time * 0.28));
    float cracks = 1.0 - smoothstep(0.035, 0.16, abs(cells - 0.5));
    return mix(vec3(0.015, 0.008, 0.006), vec3(0.16, 0.025, 0.008), cells)
            + vec3(0.94, 0.16, 0.012) * cracks + vec3(1.0, 0.62, 0.16) * pow(cracks, 5.0) * 0.64;
}

vec3 shViolet(vec2 uv, float time, out float amount) {
    // Keep the material attached to UV, but make the visible flow travel
    // mostly along texture Y instead of sliding horizontally across the item.
    vec2 p = uv + vec2(0.0, -time * 0.060);
    float mist = shFbm(p * vec2(3.1, 4.0));
    float sx = p.x * 7.0;
    float cell = floor(sx);
    float bend = mix(shHash(vec2(cell, 4.17)), shHash(vec2(cell + 1.0, 4.17)), fract(sx)) - 0.5;
    float phase = uv.y * 2.05 + uv.x * 0.36 - time * 0.40 + mist * 0.16 + bend * 0.13;
    float d = abs(fract(phase) - 0.5);
    float glow = 1.0 - smoothstep(0.045, 0.19, d);
    float core = 1.0 - smoothstep(0.012, 0.050, d);
    amount = 0.13 + mist * 0.13 + glow * 0.20 + core * 0.20;
    return mix(vec3(0.30, 0.035, 0.68), vec3(0.78, 0.25, 1.0), glow)
            + vec3(0.60, 0.20, 1.0) * glow * 0.24 + vec3(0.94, 0.70, 1.0) * core * 0.32;
}

vec2 shBand(vec2 uv, float time, float slope, float segments, float seed) {
    float sx = (uv.x + time * 0.018) * segments;
    float cell = floor(sx);
    float bend = mix(shHash(vec2(cell + seed, seed * 1.71)),
                     shHash(vec2(cell + 1.0 + seed, seed * 1.71)), fract(sx)) - 0.5;
    float mist = shFbm(uv * vec2(2.7, 3.6) + vec2(seed, -time * 0.035));
    float phase = uv.y * 1.55 + uv.x * slope - time * 0.29 + bend * 0.15 + mist * 0.11;
    float d = abs(fract(phase) - 0.5);
    return vec2(1.0 - smoothstep(0.040, 0.185, d), 1.0 - smoothstep(0.010, 0.046, d));
}

vec3 shFrost(vec2 uv, float time, out float amount) {
    float crystal = shFbm(uv * vec2(4.5, 5.8) + vec2(-time * 0.06, time * 0.08));
    float diagonal = pow(0.5 + 0.5 * sin((uv.x + uv.y * 0.72) * 17.0 - time * 2.2 + crystal * 3.0), 11.0);
    amount = 0.12 + crystal * 0.12 + diagonal * 0.36;
    return mix(vec3(0.12, 0.54, 0.78), vec3(0.72, 0.98, 1.0), diagonal);
}

vec3 shArcane(vec2 uv, float time, out float amount) {
    vec2 a = shBand(uv, time, 0.92, 8.0, 2.3);
    vec2 b = shBand(vec2(1.0 - uv.x, uv.y + 0.19), time * 0.82, 0.58, 6.0, 7.1);
    float glow = max(a.x, b.x * 0.72);
    float core = max(a.y, b.y * 0.62);
    amount = 0.12 + glow * 0.23 + core * 0.20;
    return mix(vec3(0.055, 0.18, 0.58), vec3(0.34, 0.82, 1.0), glow)
            + vec3(0.72, 0.44, 1.0) * b.x * 0.25 + vec3(0.88, 0.96, 1.0) * core * 0.35;
}

vec3 shEmerald(vec2 uv, float time, out float amount) {
    vec2 band = shBand(uv, time, 0.68, 7.0, 12.4);
    float shimmer = pow(0.5 + 0.5 * sin(shFbm(uv * vec2(5.0, 6.2) + vec2(time * 0.055, -time * 0.08)) * 6.0 + uv.y * 5.0 - time), 8.0);
    amount = 0.11 + band.x * 0.24 + band.y * 0.19 + shimmer * 0.10;
    return mix(vec3(0.015, 0.24, 0.15), vec3(0.12, 0.92, 0.52), band.x)
            + vec3(0.48, 1.0, 0.72) * band.y * 0.38 + vec3(0.10, 0.72, 0.46) * shimmer * 0.18;
}

vec3 shGolden(vec2 uv, float time, out float amount) {
    vec2 band = shBand(uv, time * 0.72, 1.12, 6.0, 21.7);
    float crossing = 1.0 - smoothstep(0.018, 0.095, abs(fract(uv.y * 1.20 - uv.x * 0.54 - time * 0.21) - 0.5));
    float rune = band.y * crossing;
    amount = 0.10 + band.x * 0.21 + crossing * 0.11 + rune * 0.22;
    return mix(vec3(0.34, 0.12, 0.015), vec3(1.0, 0.62, 0.10), band.x)
            + vec3(1.0, 0.88, 0.42) * band.y * 0.34 + vec3(1.0, 0.97, 0.72) * rune * 0.30;
}

float shBrokenWave(vec2 uv, vec2 travel, float frequency, float time, float velocity, float width, float seed) {
    vec2 direction = normalize(travel);
    vec2 across = vec2(-direction.y, direction.x);
    vec2 p = uv + direction * time * velocity;
    float largeWarp = shFbm(p * vec2(2.2, 2.8) + vec2(seed, seed * 0.37));
    float smallWarp = shFbm(p.yx * vec2(4.3, 3.5) - direction * time * velocity * 0.7 + vec2(seed * 1.9));
    float phase = dot(p, across) * frequency + (largeWarp - 0.5) * 5.4
            + sin(dot(p, direction) * 4.2 + time * velocity * 8.0 + seed) * 0.72;
    float body = smoothstep(1.0 - width, 1.0, 0.5 + 0.5 * sin(phase));
    body = pow(body * body * (3.0 - 2.0 * body), 1.35);
    return body * mix(0.08, 1.0, smoothstep(0.24, 0.68, smallWarp + largeWarp * 0.24));
}

vec3 shAzure(vec2 uv, float time, out float amount) {
    float a = shBrokenWave(uv, vec2( 1.00,  0.28), 8.0, time,  0.045, 0.48, 1.2);
    float b = shBrokenWave(uv, vec2(-0.62,  1.00), 9.5, time,  0.038, 0.40, 3.8);
    float c = shBrokenWave(uv, vec2( 0.48, -1.00), 7.2, time,  0.052, 0.44, 6.4);
    float d = shBrokenWave(uv, vec2(-1.00, -0.46), 6.4, time,  0.031, 0.52, 8.9);
    float e = shBrokenWave(uv, vec2( 0.12,  1.00), 11.0, time, 0.058, 0.36, 11.3);
    float f = shBrokenWave(uv, vec2( 1.00, -0.82), 8.7, time,  0.041, 0.42, 14.7);
    float rear = a * 0.35 + d * 0.30;
    float crossFlow = b * 0.38 + c * 0.36;
    float front = e * 0.32 + f * 0.34;
    float combined = clamp(rear + crossFlow + front, 0.0, 1.0);
    float meeting = clamp((a * b + c * f + d * e) * 0.42, 0.0, 0.62);
    amount = 0.07 + combined * 0.32 + meeting * 0.05;
    return vec3(0.34, 0.62, 0.86) + vec3(0.24, 0.62, 0.96) * rear * 0.32
            + vec3(0.60, 0.87, 1.0) * crossFlow * 0.42 + vec3(0.82, 0.96, 1.0) * front * 0.46;
}

vec3 applyShaderHandMaterial(vec3 original, vec2 uv, float mode, float time, float intensity) {
    if (mode < 2.5) {
        vec3 effect = mode < 0.5 ? shCosmic(uv, time)
                    : mode < 1.5 ? shPlasma(uv, time)
                    : shMolten(uv, time);
        float light = clamp(dot(original, vec3(0.299, 0.587, 0.114)), 0.0, 1.0);
        effect *= mix(0.68, 1.12, light) * intensity;
        return mix(original, clamp(effect, 0.0, 1.0), clamp(0.72 * intensity, 0.25, 0.92));
    }

    float amount;
    vec3 overlay = mode < 3.5 ? shViolet(uv, time, amount)
                 : mode < 4.5 ? shFrost(uv, time, amount)
                 : mode < 5.5 ? shArcane(uv, time, amount)
                 : mode < 6.5 ? shEmerald(uv, time, amount)
                 : mode < 7.5 ? shGolden(uv, time, amount)
                 : shAzure(uv, time, amount);
    float maxStrength = mode > 7.5 ? 0.44 : 0.68;
    float strength = clamp(amount * intensity, 0.0, maxStrength);
    vec3 tinted = mix(original, original * 0.84 + overlay * 0.42, strength);
    tinted += overlay * strength * (mode > 7.5 ? 0.08 : 0.15);
    return tinted;
}

