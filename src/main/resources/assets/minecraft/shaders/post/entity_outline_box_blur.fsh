#version 330

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform BlurConfig {
    vec2 BlurDir;
    float Radius;
    float RimStrength;
};

uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

const int TAP_COUNT = 8;

float coverageOf(vec4 value) {
    return max(value.r, max(value.g, value.b));
}

void main() {
    // Lite mode (EntityBloom disabled, e.g. a plain vanilla glow): RimStrength < 0.
    // Only the tight layer is blurred, with 9 taps and one exp() per tap. The
    // wider layers are sent with Radius 0 and are skipped here entirely.
    if (RimStrength < 0.0) {
        if (Radius <= 0.0) {
            fragColor = vec4(0.0);
            return;
        }

        float liteRadius = clamp(Radius, 1.0, 48.0);
        vec2 liteDir = BlurDir / InSize;
        float liteSigma = max(liteRadius * 0.5, 0.65);
        float liteStride = max(0.5, liteRadius / 2.6);
        vec3 liteColor = vec3(0.0);
        float liteCoverage = 0.0;
        float liteDistance = 0.0;
        float liteNorm = 0.0;

        for (int index = -4; index <= 4; ++index) {
            float offset = float(index) * liteStride;
            vec4 sampled = texture(InSampler, texCoord + liteDir * offset);
            float sourceCoverage = coverageOf(sampled);
            float distanceFactor = sourceCoverage > 0.0001
                ? clamp(sampled.a / sourceCoverage, 0.12, 1.0)
                : 1.0;
            float weight = exp(-(offset * offset) / (2.0 * liteSigma * liteSigma));

            liteColor += sampled.rgb * weight;
            liteCoverage += sourceCoverage * weight;
            liteDistance += distanceFactor * sourceCoverage * weight;
            liteNorm += weight;
        }

        float outCoverage = liteCoverage / max(liteNorm, 0.0001);
        if (outCoverage <= 0.0002) {
            fragColor = vec4(0.0);
            return;
        }

        vec3 liteTint = liteColor / max(liteCoverage, 0.0001);
        liteTint /= max(max(liteTint.r, liteTint.g), max(liteTint.b, 0.0001));
        float liteProp = clamp(liteDistance / max(liteCoverage, 0.0001), 0.12, 1.0);
        outCoverage = clamp(outCoverage * mix(0.88, 1.10, 0.35), 0.0, 1.0);
        fragColor = vec4(liteTint * outCoverage, outCoverage * liteProp);
        return;
    }

    float radius = clamp(Radius, 1.0, 48.0);
    vec2 sampleDir = BlurDir / InSize;

    // Fixed tap count with a variable stride. The input is sampled bilinearly,
    // so fractional strides stay smooth and a wide radius costs the same as a
    // narrow one - the alternative, integer taps with a hard cutoff, is exactly
    // what used to leave straight clipped borders around the glow.
    float referenceSigma = max(radius * 0.5, 0.6);
    float stride = max(0.5, radius / 5.2);

    vec3 colorSum = vec3(0.0);
    float coverageSum = 0.0;
    float distanceSum = 0.0;
    // For radius >= 2.6 the reference kernel is the same shape at every radius
    // (stride / sigma is constant), so its sum is a constant. Skips 17 exp()
    // calls per pixel per pass; for smaller radii it is summed as before.
    bool constantNorm = radius >= 2.6;
    float normalization = constantNorm ? 6.510689 : 0.0;

    for (int index = -TAP_COUNT; index <= TAP_COUNT; ++index) {
        float offset = float(index) * stride;
        vec4 sampled = texture(InSampler, texCoord + sampleDir * offset);
        float sourceCoverage = coverageOf(sampled);
        float distanceFactor = sourceCoverage > 0.0001
            ? clamp(sampled.a / sourceCoverage, 0.12, 1.0)
            : 1.0;

        // The radius belongs to the source entity, not to the screen. A distant
        // entity contributes through a narrow kernel so its glow stays a few
        // pixels wide, and because the sigma varies continuously there is no
        // discontinuity anywhere in the kernel.
        float localSigma = max(mix(2.5, radius, pow(distanceFactor, 0.8)) * 0.5, 0.65);
        float weight = exp(-(offset * offset) / (2.0 * localSigma * localSigma));

        colorSum += sampled.rgb * weight;
        coverageSum += sourceCoverage * weight;
        distanceSum += distanceFactor * sourceCoverage * weight;
        if (!constantNorm) {
            normalization += exp(-(offset * offset) / (2.0 * referenceSigma * referenceSigma));
        }
    }

    float coverage = coverageSum / max(normalization, 0.0001);
    if (coverage <= 0.0002) {
        fragColor = vec4(0.0);
        return;
    }

    vec3 tint = colorSum / max(coverageSum, 0.0001);
    tint /= max(max(tint.r, tint.g), max(tint.b, 0.0001));

    float propagatedDistance = clamp(distanceSum / max(coverageSum, 0.0001), 0.12, 1.0);

    // Outline Strength thickens or thins the field itself instead of stamping a
    // second hard contour on top of it.
    float density = mix(0.88, 1.10, clamp(RimStrength, 0.0, 1.0));
    coverage = clamp(coverage * density, 0.0, 1.0);

    fragColor = vec4(tint * coverage, coverage * propagatedDistance);
}

