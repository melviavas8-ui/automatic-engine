#version 410 core

layout(std140) uniform BlockOverlayData {
    vec4 BaseColorAlpha;
    vec4 Params;
};

in vec2 TexCoord;
out vec4 fragColor;

void main() {
    float time = Params.x * max(Params.y, 0.01);
    float intensity = max(Params.z, 0.0);
    float alpha = clamp(BaseColorAlpha.a, 0.0, 1.0);
    vec2 uv = TexCoord;
    vec3 baseColor = BaseColorAlpha.rgb;

    vec3 base = baseColor * 0.78;
    vec3 glow = mix(baseColor, vec3(1.0), 0.35);
    vec3 color = base;

    float wave = sin((uv.x + uv.y) * 8.0 - time * 2.0) * 0.5 + 0.5;
    wave = pow(wave, 3.0);

    float edge = 1.0 - min(min(uv.x, 1.0 - uv.x), min(uv.y, 1.0 - uv.y)) * 2.0;
    edge = pow(clamp(edge, 0.0, 1.0), 4.0);

    float pulse = sin(time * 1.5) * 0.5 + 0.5;
    float vertical = smoothstep(0.0, 1.0, uv.y);

    color = mix(color, glow, wave * 0.35 * intensity);
    color = mix(color, vec3(1.0), wave * 0.18 * intensity);
    color += baseColor * edge * 0.35 * intensity;
    color *= 0.92 + pulse * 0.08;
    color *= 0.85 + vertical * 0.25;

    float finalAlpha = clamp(alpha * (0.9 + edge * 0.1), 0.0, 1.0);
    fragColor = vec4(clamp(color, 0.0, 1.0), finalAlpha);
}
