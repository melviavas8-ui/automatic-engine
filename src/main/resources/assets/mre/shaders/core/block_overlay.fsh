#version 410 core

layout(std140) uniform BlockOverlayData {
    vec4 BaseColorAlpha;
    vec4 HighlightColor;
    vec4 Params;
};

in vec2 TexCoord;
in vec3 SkyDirection;
out vec4 fragColor;

float hash31(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float noise3(vec3 p) {
    vec3 cell = floor(p);
    vec3 local = fract(p);
    local = local * local * (3.0 - 2.0 * local);
    float n000 = hash31(cell);
    float n100 = hash31(cell + vec3(1.0, 0.0, 0.0));
    float n010 = hash31(cell + vec3(0.0, 1.0, 0.0));
    float n110 = hash31(cell + vec3(1.0, 1.0, 0.0));
    float n001 = hash31(cell + vec3(0.0, 0.0, 1.0));
    float n101 = hash31(cell + vec3(1.0, 0.0, 1.0));
    float n011 = hash31(cell + vec3(0.0, 1.0, 1.0));
    float n111 = hash31(cell + vec3(1.0));
    float nx00 = mix(n000, n100, local.x);
    float nx10 = mix(n010, n110, local.x);
    float nx01 = mix(n001, n101, local.x);
    float nx11 = mix(n011, n111, local.x);
    return mix(mix(nx00, nx10, local.y), mix(nx01, nx11, local.y), local.z);
}

float fbm(vec3 p) {
    float value = 0.0;
    float amplitude = 0.55;
    for (int i = 0; i < 5; i++) {
        value += noise3(p) * amplitude;
        p = p * 2.03 + vec3(1.7, -2.1, 0.8);
        amplitude *= 0.5;
    }
    return value;
}

void main() {
    float time = Params.x * max(Params.y, 0.01);
    float intensity = clamp(Params.z, 0.0, 2.5);
    int mode = int(floor(Params.w + 0.5));
    float alpha = clamp(BaseColorAlpha.a, 0.0, 1.0);

    vec2 uv = TexCoord;
    vec3 baseColor = clamp(BaseColorAlpha.rgb, 0.0, 1.0);
    vec3 highlightColor = clamp(HighlightColor.rgb, 0.0, 1.0);
    vec3 accentColor = mix(baseColor, highlightColor, 0.32);
    vec3 shineColor = mix(baseColor, vec3(1.0), 0.14);

    vec3 dir = normalize(SkyDirection);
    vec3 flow = dir * 2.35 + vec3(time * 0.026, time * 0.015, -time * 0.032);

    float n1 = fbm(flow * 1.35);
    float n2 = fbm(flow * 2.55 + vec3(4.2, -1.7, 2.5));
    float n3 = fbm(flow * 5.10 + vec3(-2.0, 6.0, -3.0));

    float crystal = smoothstep(0.07, 0.38, abs(n1 - n2));
    float mist = smoothstep(0.18, 0.88, n1);
    float glowPatches = pow(smoothstep(0.54, 1.0, n2), 2.2);
    float sparks = pow(smoothstep(0.76, 1.0, n3), 10.0);
    float pulse = sin(time * 1.25 + n1 * 6.2831) * 0.5 + 0.5;
    float shimmer = smoothstep(0.64, 1.0, n2 + pulse * 0.14);

    float edge = 1.0 - min(min(uv.x, 1.0 - uv.x), min(uv.y, 1.0 - uv.y)) * 2.0;
    edge = clamp(edge, 0.0, 1.0);
    float edgeGlow = pow(edge, 3.0);
    float innerFrost = pow(1.0 - edge, 2.2) * mist;

    float alphaShape = 0.30;
    vec3 color = baseColor;

    if (mode == 1) {
        float nebula = smoothstep(0.24, 0.90, n1 * 0.65 + n2 * 0.45);
        float dust = pow(smoothstep(0.58, 1.0, n3), 5.0);
        float veil = sin((n1 + n2) * 8.0 + time * 0.75) * 0.5 + 0.5;

        color = baseColor * (0.10 + nebula * 0.42);
        color += accentColor * veil * nebula * 0.28 * intensity;
        color += highlightColor * dust * 0.45 * intensity;
        color += accentColor * edgeGlow * 0.58 * intensity;
        color += shineColor * pow(edge, 5.5) * 0.10 * intensity;

        alphaShape = 0.24 + nebula * 0.28 + dust * 0.18 + veil * 0.08 + edgeGlow * 0.34;
    } else if (mode == 2) {
        float scan = pow(sin(uv.y * 52.0 - time * 7.0) * 0.5 + 0.5, 9.0);
        float bars = smoothstep(0.82, 1.0, sin((uv.y + n1 * 0.08) * 18.0 - time * 1.8) * 0.5 + 0.5);
        float pixel = step(0.94, hash31(vec3(floor(uv * 22.0), floor(time * 4.0))));
        float interference = smoothstep(0.62, 1.0, n2 + scan * 0.14);

        color = baseColor * (0.12 + interference * 0.30);
        color += accentColor * bars * 0.38 * intensity;
        color += highlightColor * scan * 0.42 * intensity;
        color += shineColor * pixel * 0.36 * intensity;
        color += accentColor * edgeGlow * 0.70 * intensity;

        alphaShape = 0.22 + interference * 0.18 + bars * 0.16 + scan * 0.14 + edgeGlow * 0.42 + pixel * 0.10;
    } else if (mode == 3) {
        float pulseRing = sin((1.0 - edge) * 8.0 - time * 2.8) * 0.5 + 0.5;
        pulseRing = pow(pulseRing, 4.0);
        float core = smoothstep(0.20, 0.86, n1);
        float flare = pow(smoothstep(0.62, 1.0, n2 + pulse * 0.16), 2.0);

        color = baseColor * (0.14 + core * 0.34);
        color += baseColor * pulseRing * 0.26 * intensity;
        color += accentColor * flare * 0.40 * intensity;
        color += highlightColor * pulse * edgeGlow * 0.52 * intensity;
        color += shineColor * pow(edge, 7.0) * 0.14 * intensity;

        alphaShape = 0.25 + core * 0.16 + flare * 0.18 + pulseRing * 0.16 + edgeGlow * 0.40;
    } else {
        color = baseColor * (0.16 + mist * 0.26);
        color = mix(color, baseColor * (0.52 + mist * 0.22), crystal * 0.62);
        color += baseColor * glowPatches * 0.24 * intensity;
        color += accentColor * shimmer * 0.22 * intensity;
        color += accentColor * sparks * 0.32 * intensity;
        color += baseColor * innerFrost * 0.12 * intensity;
        color += accentColor * edgeGlow * 0.52 * intensity;
        color += shineColor * pow(edge, 6.0) * 0.12 * intensity;

        alphaShape = 0.26
            + crystal * 0.16
            + mist * 0.10
            + glowPatches * 0.14
            + shimmer * 0.09
            + edgeGlow * 0.34
            + sparks * 0.12;
    }

    float saturationGuard = max(max(baseColor.r, baseColor.g), baseColor.b);
    color = mix(color, normalize(max(baseColor, vec3(0.001))) * length(color), 0.10 * saturationGuard);

    fragColor = vec4(clamp(color, 0.0, 1.0), alpha * clamp(alphaShape, 0.0, 1.0));
}
