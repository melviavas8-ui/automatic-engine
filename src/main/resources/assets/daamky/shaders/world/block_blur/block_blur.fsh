#version 410 core

in vec2 vUV;
in vec4 vClipPos;

out vec4 fragColor;

uniform sampler2D BlurSampler;

layout(std140) uniform BlockBlurData {
    vec4 params;
    vec4 unused;
};

void main() {
    vec2 screenUV = vClipPos.xy / vClipPos.w * 0.5 + 0.5;
    vec3 blurColor = texture(BlurSampler, clamp(screenUV, vec2(0.0), vec2(1.0))).rgb;

    float opacity = params.z;
    vec2 edgeUv = abs(vUV - 0.5) * 2.0;
    float edge = smoothstep(0.78, 1.0, max(edgeUv.x, edgeUv.y));
    vec3 edgeColor = vec3(0.82, 0.95, 1.0);
    vec3 color = mix(blurColor, edgeColor, edge * 0.20);

    fragColor = vec4(color, opacity);
}
