#version 150

uniform sampler2D SceneSampler;
uniform sampler2D DepthSampler;

layout(std140) uniform CircleDistortionData {
    mat4 InverseViewProjection;
    vec4 CameraTime;
    vec4 ScreenData;
    vec4 GlobalData;
    vec4 CircleCenterRadius[8];
    vec4 CircleMotion[8];
    vec4 CircleColor[8];
};

in vec2 texCoord;
out vec4 fragColor;

#define TAU 6.28318530718

float hash21(vec2 point) {
    point = fract(point * vec2(123.34, 456.21));
    point += dot(point, point + 45.32);
    return fract(point.x * point.y);
}

float smoothPulse(float center, float halfWidth, float value) {
    return 1.0 - smoothstep(halfWidth * 0.45, halfWidth, abs(value - center));
}

void main() {
    vec3 original = texture(SceneSampler, texCoord).rgb;
    float rawDepth = texture(DepthSampler, texCoord).r;
    if (rawDepth >= 0.999995 || ScreenData.z < 0.5) {
        fragColor = vec4(original, 1.0);
        return;
    }

    vec4 clip = vec4(texCoord * 2.0 - 1.0, rawDepth * 2.0 - 1.0, 1.0);
    vec4 relativePosition = InverseViewProjection * clip;
    relativePosition.xyz /= max(abs(relativePosition.w), 0.00001);
    vec3 worldPosition = CameraTime.xyz + relativePosition.xyz;

    vec2 texel = 1.0 / max(ScreenData.xy, vec2(1.0));
    vec2 displacement = vec2(0.0);
    vec3 projectedColor = vec3(0.0);
    float projectedWeight = 0.0;
    float blurMask = 0.0;
    float edgeLight = 0.0;

    for (int index = 0; index < 8; index++) {
        if (float(index) >= ScreenData.z) {
            break;
        }

        vec4 centerRadius = CircleCenterRadius[index];
        vec4 motion = CircleMotion[index];
        vec4 colorStyle = CircleColor[index];
        float radius = max(centerRadius.w, 0.001);
        float alpha = clamp(motion.y, 0.0, 1.0);
        float thickness = max(motion.z, 0.025);
        if (alpha <= 0.001) {
            continue;
        }

        vec2 local = worldPosition.xz - centerRadius.xz;
        float distanceFromCenter = length(local);
        float angle = atan(local.y, local.x);
        float style = colorStyle.w;
        float phase = motion.w;
        float time = CameraTime.w;

        float broadWarp = sin(angle * 3.0 + phase + time * 0.72);
        float fineWarp = sin(angle * 7.0 - phase * 1.37 - time * 0.41);
        float irregularity = mix(0.035, 0.085, step(0.5, style));
        irregularity = mix(irregularity, 0.020, step(3.5, style));
        float warpedRadius = radius * (
                1.0
                + broadWarp * irregularity
                + fineWarp * irregularity * 0.34
        );

        float floorDelta = abs(worldPosition.y - centerRadius.y);
        float floorMask = 1.0 - smoothstep(0.055, 0.72, floorDelta);
        float edgeDistance = abs(distanceFromCenter - warpedRadius);
        float edge = 1.0 - smoothstep(thickness * 0.45, thickness * 2.35, edgeDistance);
        float inner = (1.0 - smoothstep(warpedRadius * 0.74, warpedRadius * 1.04, distanceFromCenter))
                * smoothstep(0.02, warpedRadius * 0.30, distanceFromCenter);
        float outerRipple = smoothPulse(
                warpedRadius + thickness * 2.65,
                thickness * 2.4,
                distanceFromCenter
        );

        float activeEdge = edge * floorMask * alpha;
        float activeInner = inner * floorMask * alpha;
        float activeRipple = outerRipple * floorMask * alpha;

        vec2 worldFlow = vec2(
            sin(angle * 2.0 + phase + time * 0.83 + distanceFromCenter * 4.1),
            cos(angle * 3.0 - phase * 0.73 - time * 0.57 + distanceFromCenter * 3.2)
        );
        worldFlow += vec2(
            sin(worldPosition.z * 2.7 + phase),
            cos(worldPosition.x * 2.4 - phase)
        ) * 0.34;
        worldFlow = normalize(worldFlow + vec2(0.0001));

        float displacementPixels = GlobalData.x
                * (activeEdge * 4.6 + activeInner * 1.35 + activeRipple * 1.2);
        displacement += worldFlow * texel * displacementPixels;
        blurMask = max(blurMask, clamp(activeInner * 0.30 + activeEdge * 0.58, 0.0, 0.72));

        float colorMask = activeInner * 0.105 + activeEdge * 0.20 + activeRipple * 0.055;
        projectedColor += colorStyle.rgb * colorMask;
        projectedWeight += colorMask;
        edgeLight += activeEdge * 0.085 + activeRipple * 0.025;
    }

    float distortionAmount = clamp(length(displacement) / max(length(texel), 0.000001), 0.0, 8.0);
    if (distortionAmount <= 0.0001 && projectedWeight <= 0.0001) {
        fragColor = vec4(original, 1.0);
        return;
    }

    vec2 refractedUv = clamp(texCoord + displacement, vec2(0.001), vec2(0.999));
    float chroma = clamp(distortionAmount * 0.055, 0.0, 0.32);
    vec3 refracted;
    refracted.r = texture(SceneSampler, clamp(refractedUv + displacement * chroma, vec2(0.001), vec2(0.999))).r;
    refracted.g = texture(SceneSampler, refractedUv).g;
    refracted.b = texture(SceneSampler, clamp(refractedUv - displacement * chroma, vec2(0.001), vec2(0.999))).b;

    vec2 blurStep = texel * (1.0 + distortionAmount * 0.32);
    vec3 softened = (
        refracted
        + texture(SceneSampler, refractedUv + vec2(blurStep.x, 0.0)).rgb
        + texture(SceneSampler, refractedUv - vec2(blurStep.x, 0.0)).rgb
        + texture(SceneSampler, refractedUv + vec2(0.0, blurStep.y)).rgb
        + texture(SceneSampler, refractedUv - vec2(0.0, blurStep.y)).rgb
    ) * 0.2;
    refracted = mix(refracted, softened, blurMask);

    vec3 floorColor = projectedWeight > 0.0001
            ? projectedColor / projectedWeight
            : vec3(0.0);
    float tint = clamp(projectedWeight * GlobalData.y, 0.0, 0.34);
    vec3 result = mix(refracted, floorColor, tint);
    result += floorColor * edgeLight * GlobalData.y;

    float dither = (hash21(gl_FragCoord.xy) - 0.5) / 510.0;
    fragColor = vec4(clamp(result + dither, 0.0, 1.0), 1.0);
}
