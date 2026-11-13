#version 150

#moj_import <mre:common.glsl>

uniform sampler2D InputSampler;
uniform vec2 InputResolution;
uniform vec2 Size;
uniform vec2 Location;
uniform vec4 Radius;
uniform float Smoothness;
uniform float CornerSmoothness;
uniform vec4 TintColor;
uniform float GlobalAlpha;
uniform float FresnelPower;
uniform vec3 FresnelColor;
uniform float FresnelAlpha;
uniform float BaseAlpha;
uniform int FresnelInvert;
uniform float FresnelMix;
uniform float DistortStrength;
uniform float Time;

in vec2 FragCoord;
out vec4 OutColor;

vec4 sampleScene(vec2 uv, float strength) {
    vec2 blurOffset = (3.10 + strength * 1.45) / InputResolution;
    vec4 color = texture(InputSampler, uv) * 0.8;
    color += texture(InputSampler, clamp(uv + vec2(blurOffset.x, 0.0), vec2(0.0), vec2(1.0))) * 0.14;
    color += texture(InputSampler, clamp(uv - vec2(blurOffset.x, 0.0), vec2(0.0), vec2(1.0))) * 0.14;
    color += texture(InputSampler, clamp(uv + vec2(0.0, blurOffset.y), vec2(0.0), vec2(1.0))) * 0.14;
    color += texture(InputSampler, clamp(uv - vec2(0.0, blurOffset.y), vec2(0.0), vec2(1.0))) * 0.14;
    color += texture(InputSampler, clamp(uv + blurOffset, vec2(0.0), vec2(1.0))) * 0.09;
    color += texture(InputSampler, clamp(uv - blurOffset, vec2(0.0), vec2(1.0))) * 0.09;
    color += texture(InputSampler, clamp(uv + vec2(blurOffset.x, -blurOffset.y), vec2(0.0), vec2(1.0))) * 0.05;
    color += texture(InputSampler, clamp(uv + vec2(-blurOffset.x, blurOffset.y), vec2(0.0), vec2(1.0))) * 0.05;
    return color;
}

void main() {
    float alpha = ralpha(Size, FragCoord, Radius, Smoothness);
    if (alpha <= 0.001) {
        discard;
    }

    vec2 center = Size * 0.5;
    float dist = rdist(center - (FragCoord * Size), center - 2.0, Radius);
    float edgeWidth = max(1.0, CornerSmoothness * 8.0);

    float fresnel = clamp(smoothstep(-edgeWidth, 0.0, dist), 0.0, 1);
    if (FresnelInvert == 1) {
        fresnel = 1.0 - fresnel;
    }
    fresnel = pow(max(fresnel, 0.0001), max(0.01, FresnelPower));

    float distortionEdge = smoothstep(-edgeWidth * 1.4, 0.0, dist);
    vec2 centeredUv = FragCoord - vec2(0.5);

    float waveA = sin((FragCoord.x * 13.0) + (FragCoord.y * 8.0) - Time * 2.4);
    float waveB = sin((FragCoord.x * -7.0) + (FragCoord.y * 17.0) + Time * 1.8);
    float waveC = cos((FragCoord.x + FragCoord.y) * 28.0 - Time * 3.2);
    float waveStrength = 0.0015 * DistortStrength * (0.55 + distortionEdge * 0.75);

    vec2 travellingWaves = vec2(
        waveA + waveB * 0.55,
        waveB + waveC * 0.40
    ) * waveStrength;

    vec2 ripple = vec2(
        sin((FragCoord.y * 22.0) + Time * 2.35),
        cos((FragCoord.x * 21.0) - Time * 2.10)
    ) * (0.0009 * DistortStrength);

    vec2 swirl = vec2(-centeredUv.y, centeredUv.x) * (0.0032 * DistortStrength * distortionEdge);
    vec2 lens = centeredUv * (0.0065 * DistortStrength * (0.25 + fresnel));
    vec2 screenUv = clamp(
        (gl_FragCoord.xy / InputResolution) + travellingWaves + ripple + lens,
        vec2(0.0),
        vec2(1.0)
    );
    screenUv = clamp(screenUv + swirl, vec2(0.0), vec2(1.0));

    vec4 scene = sampleScene(screenUv, DistortStrength);
    float tintAmount = clamp(BaseAlpha * (0.75 + distortionEdge * 0.45) * TintColor.a, 0.0, 1.0);
    vec3 base = mix(scene.rgb, TintColor.rgb, tintAmount);
    float topHighlight = pow(max(0.0, 1.0 - FragCoord.y), 3.0) * 0.28;
    float sideHighlight = pow(clamp(1.0 - abs(FragCoord.x - 0.5) * 1.0, 0.0, 1.0), 5.0) * 0.10;
    float waveHighlight = smoothstep(0.45, 1.0, waveA * 0.5 + 0.5) * 0.10;
    base += FresnelColor * (topHighlight + sideHighlight + waveHighlight) * FresnelAlpha;

    float fresnelAmount = clamp(FresnelAlpha * fresnel, 0.0, 1.0);
    vec3 fresnelLayer = mix(base, FresnelColor, fresnelAmount);
    vec3 finalColor = mix(base, fresnelLayer, clamp(FresnelMix, 0.0, 1.0));

    OutColor = vec4(clamp(finalColor, 0.0, 1.0), GlobalAlpha * alpha);
}
