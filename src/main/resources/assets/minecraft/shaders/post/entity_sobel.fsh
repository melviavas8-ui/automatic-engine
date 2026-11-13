#version 330

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform BloomConfig {
    float HitFill;
};

in vec2 texCoord;

out vec4 fragColor;

// The entity outline target carries per-entity metadata inside the alpha
// channel. The lowest band is a world-space afterimage, the middle band is the
// regular distance marker, the high band means "recently hit" and full alpha is
// a vanilla glowing entity.
float distanceFactor(float encodedAlpha) {
    if (encodedAlpha > 0.001 && encodedAlpha < 0.075) {
        return 0.62;
    }
    float hitMarker = step(0.69, encodedAlpha) * (1.0 - step(0.98, encodedAlpha));
    float regularDistance = clamp((encodedAlpha - 0.08) / 0.54, 0.12, 1.0);
    float hitDistance = clamp((encodedAlpha - 0.72) / 0.24, 0.12, 1.0);
    float vanillaDistance = step(0.98, encodedAlpha);
    return max(vanillaDistance, mix(regularDistance, hitDistance, hitMarker));
}

float trailOpacity(float encodedAlpha) {
    if (encodedAlpha < 0.001) {
        return 0.0;
    }
    if (encodedAlpha < 0.075) {
        return smoothstep(0.006, 0.069, encodedAlpha);
    }
    return 1.0;
}

float hitMarkerOf(float encodedAlpha) {
    return step(0.69, encodedAlpha) * (1.0 - step(0.98, encodedAlpha));
}

// 3x3 tent. The source silhouette is a hard one-bit mask, so it has to be
// prefiltered here: everything downstream treats this target as a continuous
// field, and a single stair-stepped texel would survive every later blur as a
// visible corner.
const float TENT_WEIGHTS[9] = float[9](
    1.0, 2.0, 1.0,
    2.0, 4.0, 2.0,
    1.0, 2.0, 1.0
);

void main() {
    vec2 texel = 1.0 / InSize;

    vec3 colorSum = vec3(0.0);
    float coverageSum = 0.0;
    float distanceSum = 0.0;
    float hitSum = 0.0;
    float weightSum = 0.0;

    for (int y = -1; y <= 1; ++y) {
        for (int x = -1; x <= 1; ++x) {
            float weight = TENT_WEIGHTS[(y + 1) * 3 + (x + 1)];
            // Nearest sampling on purpose: the alpha bands above are decoded
            // per texel and would be destroyed by hardware filtering.
            vec4 sampled = texture(
                InSampler,
                texCoord + vec2(float(x), float(y)) * texel
            );
            float present = step(0.001, sampled.a) * trailOpacity(sampled.a);
            float weighted = present * weight;

            colorSum += sampled.rgb * weighted;
            coverageSum += weighted;
            distanceSum += distanceFactor(sampled.a) * weighted;
            hitSum += hitMarkerOf(sampled.a) * weighted;
            weightSum += weight;
        }
    }

    if (coverageSum <= 0.0001) {
        fragColor = vec4(0.0);
        return;
    }

    vec3 tint = colorSum / coverageSum;
    tint /= max(max(tint.r, tint.g), max(tint.b, 0.0001));

    float averageDistance = clamp(distanceSum / coverageSum, 0.12, 1.0);
    float hitAmount = clamp(hitSum / coverageSum, 0.0, 1.0);

    // Filled, antialiased silhouette instead of the old one-pixel edge cross.
    // Blurring a solid shape produces a smooth monotone falloff that behaves
    // like a distance field; blurring a thin edge produced a ridge that kept
    // the shape of the pixel grid.
    float coverage = coverageSum / weightSum;
    coverage = clamp(
        coverage * mix(1.0, 1.14, hitAmount * clamp(HitFill, 0.0, 1.0)),
        0.0,
        1.0
    );

    // RGB carries the normalized tint premultiplied by coverage, alpha carries
    // the same coverage scaled by the distance factor so the blur passes can
    // keep adapting their radius per entity.
    fragColor = vec4(tint * coverage, coverage * averageDistance);
}

