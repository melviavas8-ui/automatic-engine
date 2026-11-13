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
    float normalization = 0.0;

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
        float referenceWeight = exp(-(offset * offset) / (2.0 * referenceSigma * referenceSigma));

        colorSum += sampled.rgb * weight;
        coverageSum += sourceCoverage * weight;
        distanceSum += distanceFactor * sourceCoverage * weight;
        normalization += referenceWeight;
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

