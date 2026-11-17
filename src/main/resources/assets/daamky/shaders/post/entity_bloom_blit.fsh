// FIXED 2026-11-17: simplified soft glow (removed heavy fbm/noise/flame) for performance
#version 330

uniform sampler2D EdgeSampler;
uniform sampler2D CoreSampler;
uniform sampler2D WideSampler;
uniform sampler2D HazeSampler;
uniform sampler2D MaskSampler;

layout(std140) uniform BlitConfig {
    vec4 ColorModulate;
    vec4 BloomShape;
    vec4 BloomMotion;
    vec4 BloomView;
};

in vec2 texCoord;
out vec4 fragColor;

float coverageOf(vec4 value) {
    return max(value.r, max(value.g, value.b));
}

void main() {
    vec4 edge = texture(EdgeSampler, texCoord);
    vec4 core = texture(CoreSampler, texCoord);
    vec4 wide = texture(WideSampler, texCoord);
    vec4 haze = texture(HazeSampler, texCoord);
    float maskA = texture(MaskSampler, texCoord).a;

    float edgeCov = coverageOf(edge);
    float coreCov = coverageOf(core);
    float wideCov = coverageOf(wide);
    float hazeCov = coverageOf(haze);

    // Simple soft glow: combine layers with decreasing weight
    float energy = edgeCov * 0.55 + coreCov * 0.35 + wideCov * 0.18 + hazeCov * 0.08;
    energy = clamp(energy, 0.0, 1.0);

    if (energy < 0.002) {
        fragColor = vec4(0.0);
        return;
    }

    // Reconstruct approximate tint from the strongest layer that has colour
    vec3 tint = vec3(1.0);
    float tintWeight = 0.0;
    if (edgeCov > 0.01) { tint += edge.rgb; tintWeight += edgeCov; }
    if (coreCov > 0.01) { tint += core.rgb; tintWeight += coreCov; }
    if (wideCov > 0.01) { tint += wide.rgb; tintWeight += wideCov; }
    if (tintWeight > 0.001) {
        tint = (tint - vec3(1.0)) / tintWeight;
        float maxC = max(max(tint.r, tint.g), max(tint.b, 0.001));
        tint /= maxC;
    } else {
        tint = ColorModulate.rgb;
    }

    // Soft alpha, keep interior somewhat transparent
    float interior = smoothstep(0.35, 0.85, edgeCov);
    float alpha = energy * ColorModulate.a * mix(0.85, 0.12, interior);
    alpha = clamp(alpha, 0.0, 0.92);

    // Hit fill (high alpha band)
    bool hitInterior = maskA >= 0.69 && maskA < 0.98;
    if (hitInterior) {
        alpha = max(alpha, 0.75 * smoothstep(0.1, 0.55, edgeCov));
    }

    float brightness = 1.0 + edgeCov * 0.15 + coreCov * 0.1;
    fragColor = vec4(tint * ColorModulate.rgb * brightness, alpha);
}
