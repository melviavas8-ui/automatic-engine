#version 150

uniform sampler2D InSampler;

layout(std140) uniform BlurStyle {
    vec4 Tint;
    float Brightness;
    float Saturation;
    float Vignette;
    float Grain;
    float GameTime;
};

in vec2 texCoord;
out vec4 fragColor;

float hashNoise(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233)) + GameTime) * 43758.5453);
}

void main() {
    vec3 color = texture(InSampler, texCoord).rgb;
    float luma = dot(color, vec3(0.2126, 0.7152, 0.0722));
    color = mix(vec3(luma), color, Saturation);
    color = mix(color, Tint.rgb, Tint.a);
    color *= Brightness;
    vec2 vUv = texCoord - 0.5;
    color *= 1.0 - Vignette * smoothstep(0.0, 0.75, dot(vUv, vUv) * 2.0);
    color += (hashNoise(gl_FragCoord.xy) - 0.5) * Grain;
    fragColor = vec4(color, 1.0);
}
