#version 410 core

in vec2 localUv;
out vec4 fragColor;

uniform sampler2D SceneSampler;

layout(std140) uniform GlassData {
    vec4 resolutionTime;
    vec4 tintColor;
    vec4 settings;
};

void main() {
    vec2 local = localUv * 2.0 - 1.0;
    float distanceFromCenter = length(local);
    if (distanceFromCenter >= 1.0) {
        discard;
    }

    vec2 resolution = resolutionTime.xy;
    float time = resolutionTime.z;
    float progress = resolutionTime.w;
    float alpha = tintColor.a;
    vec2 screenUv = gl_FragCoord.xy / resolution;

    // Flat liquid-glass distortion: only the image of the ground is displaced.
    float body = 1.0 - smoothstep(0.0, 1.0, distanceFromCenter);
    float circularWave = sin(distanceFromCenter * 27.0 - time * 7.0 + progress * 10.0);
    vec2 radial = distanceFromCenter > 0.0001 ? local / distanceFromCenter : vec2(0.0);
    vec2 tangent = vec2(-radial.y, radial.x);
    float lensStrength = body * (0.85 + 0.15 * sin(time * 4.0));
    vec2 pixelOffset = radial * lensStrength * settings.x * 3.0;
    pixelOffset += tangent * circularWave * body * settings.x * 0.65;
    vec2 refractedUv = clamp(screenUv + pixelOffset * alpha / resolution, vec2(0.001), vec2(0.999));
    vec3 color = texture(SceneSampler, refractedUv).rgb;

    float flowingGlass = sin(local.x * 8.0 + time * 2.4)
            * cos(local.y * 9.0 - time * 2.0) * 0.5 + 0.5;
    flowingGlass *= body;
    float edgeFeather = 1.0 - smoothstep(0.92, 1.0, distanceFromCenter);
    vec3 original = texture(SceneSampler, screenUv).rgb;

    // The interior stays neutral: it only bends the actual world image.
    color = mix(original, color, alpha * edgeFeather);
    color += vec3(1.0) * flowingGlass * body * alpha * 0.025;

    // Purple exists only on the border, like the thick glowing rim in the reference.
    float rimCore = smoothstep(0.76, 0.88, distanceFromCenter)
            * (1.0 - smoothstep(0.94, 0.995, distanceFromCenter));
    float rimGlow = smoothstep(0.58, 0.88, distanceFromCenter)
            * (1.0 - smoothstep(0.88, 1.0, distanceFromCenter));
    float rimPulse = 0.88 + sin(time * 3.6 + progress * 7.0) * 0.12;
    color += tintColor.rgb * alpha * rimPulse * (rimCore * 0.82 + rimGlow * 0.22);
    color += vec3(0.82, 0.66, 1.0) * alpha * rimCore * 0.22;

    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
