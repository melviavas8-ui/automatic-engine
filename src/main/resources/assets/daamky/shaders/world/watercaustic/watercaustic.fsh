#version 410 core

in vec2 vUV;
out vec4 fragColor;

layout(std140) uniform WaterData {
    vec4 iColor;
    vec4 iPatternColor;
    float iTime;
    float iIntensity;
};

void main() {
    float time = iTime;
    vec2 uv = vUV * iPatternColor.w;

    vec3 base = iColor.rgb * 0.78;
    vec3 glow = mix(iPatternColor.rgb, vec3(1.0), 0.35);
    vec3 color = base;

    float wave = sin((uv.x + uv.y) * 8.0 - time * 2.0) * 0.5 + 0.5;
    wave = pow(wave, 3.0);

    float edge = 1.0 - min(min(vUV.x, 1.0 - vUV.x), min(vUV.y, 1.0 - vUV.y)) * 2.0;
    edge = pow(clamp(edge, 0.0, 1.0), 4.0);

    float pulse = sin(time * 1.5) * 0.5 + 0.5;
    float vertical = smoothstep(0.0, 1.0, vUV.y);
    float intensity = clamp(iIntensity, 0.2, 2.5);

    color = mix(color, glow, wave * 0.35 * intensity);
    color = mix(color, vec3(1.0), wave * 0.18 * intensity);
    color += iPatternColor.rgb * edge * 0.35 * intensity;
    color *= 0.92 + pulse * 0.08;
    color *= 0.85 + vertical * 0.25;

    float finalAlpha = clamp(iColor.w * (0.9 + edge * 0.1), 0.0, 1.0);
    fragColor = vec4(clamp(color, 0.0, 1.0), finalAlpha);
}
