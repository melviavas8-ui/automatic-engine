#version 330

uniform sampler2D EdgeSampler;  // antialiased filled silhouette
uniform sampler2D CoreSampler;  // tight blur   - hot rim
uniform sampler2D WideSampler;  // medium blur  - flame base
uniform sampler2D HazeSampler;  // wide blur    - distance field and outer glow
uniform sampler2D MaskSampler;  // raw encoded outline target

layout(std140) uniform BlitConfig {
    vec4 ColorModulate;  // rgb: light multiplier, a: master opacity
    vec4 BloomShape;     // x: rim, y: body, z: halo, w: hit fill power
    vec4 BloomMotion;    // x: time, y: flame amount, z: flame speed, w: detail
    vec4 BloomView;      // xy: camera anchor for the noise, in screen heights
};

in vec2 texCoord;

out vec4 fragColor;

float coverageOf(vec4 value) {
    return max(value.r, max(value.g, value.b));
}

float hash12(vec2 point) {
    vec3 scattered = fract(vec3(point.xyx) * 0.1031);
    scattered += dot(scattered, scattered.yzx + 33.33);
    return fract((scattered.x + scattered.y) * scattered.z);
}

// Quintic interpolation. The first and second derivatives vanish at the lattice
// points, so neither the noise lattice nor the screen texel grid can show up as
// a straight line anywhere in the flame.
float valueNoise(vec2 point) {
    vec2 cell = floor(point);
    vec2 local = fract(point);
    vec2 smoothed = local * local * local * (local * (local * 6.0 - 15.0) + 10.0);

    float corner00 = hash12(cell);
    float corner10 = hash12(cell + vec2(1.0, 0.0));
    float corner01 = hash12(cell + vec2(0.0, 1.0));
    float corner11 = hash12(cell + vec2(1.0, 1.0));

    return mix(
        mix(corner00, corner10, smoothed.x),
        mix(corner01, corner11, smoothed.x),
        smoothed.y
    );
}

// Doubles the frequency and rotates by ~37 degrees per octave. Without the
// rotation every octave shares the same axes and the sum develops horizontal
// and vertical ridges - the classic "square noise" look.
const mat2 OCTAVE_STEP = mat2(1.6, 1.2, -1.2, 1.6);

/**
 * Band limited fbm. Every octave is faded out as its wavelength approaches the
 * pixel grid, and the sum is renormalized by the surviving amplitude.
 *
 * This is the single most important part of the whole effect. Value noise whose
 * period approaches one pixel degenerates into a per-pixel hash, which reads as
 * hard little rectangles and shimmers under motion - exactly the blockiness the
 * flame boundary used to inherit. Fading those octaves out means the flame stays
 * smooth at any resolution, and a high resolution screen automatically gets more
 * detail rather than more noise.
 *
 * @param basePeriodPixels wavelength of the first octave, measured in screen
 *                         pixels along whichever axis is compressed the most.
 */
float fbmBanded(vec2 point, float basePeriodPixels, int octaves) {
    float sum = 0.0;
    float normalization = 0.0;
    float amplitude = 0.5;
    float period = basePeriodPixels;

    for (int octave = 0; octave < 5; ++octave) {
        if (octave >= octaves) {
            break;
        }
        float fade = smoothstep(1.8, 4.5, period);
        if (fade <= 0.0) {
            break;
        }
        sum += valueNoise(point) * amplitude * fade;
        normalization += amplitude * fade;
        point = OCTAVE_STEP * point;
        amplitude *= 0.5;
        period *= 0.5;
    }

    return normalization > 1.0e-5 ? sum / normalization : 0.5;
}

void main() {
    vec4 shape = texture(EdgeSampler, texCoord);
    vec4 coreCenter = texture(CoreSampler, texCoord);
    vec4 wideCenter = texture(WideSampler, texCoord);
    vec4 hazeCenter = texture(HazeSampler, texCoord);

    float shapeCoverage = coverageOf(shape);
    float coreCoverage = coverageOf(coreCenter);
    float wideCoverage = coverageOf(wideCenter);
    float hazeCoverage = coverageOf(hazeCenter);

    if (max(max(shapeCoverage, coreCoverage), max(wideCoverage, hazeCoverage)) <= 0.0006) {
        fragColor = vec4(0.0);
        return;
    }

    float weightSum =
        shapeCoverage * 1.6
        + coreCoverage * 1.2
        + wideCoverage * 0.8
        + hazeCoverage * 0.5;
    vec3 tint = (
        shape.rgb * 1.6
        + coreCenter.rgb * 1.2
        + wideCenter.rgb * 0.8
        + hazeCenter.rgb * 0.5
    ) / max(weightSum, 0.0001);
    tint /= max(max(tint.r, tint.g), max(tint.b, 0.0001));

    // Alpha holds coverage times the source distance factor, so the ratio of the
    // sums recovers the distance of whichever entity lit this pixel.
    float coverageTotal = shapeCoverage + coreCoverage + wideCoverage + hazeCoverage;
    float alphaTotal = shape.a + coreCenter.a + wideCenter.a + hazeCenter.a;
    float distanceScale = coverageTotal > 0.0001
        ? clamp(alphaTotal / coverageTotal, 0.12, 1.0)
        : 0.5;

    vec2 resolution = vec2(textureSize(HazeSampler, 0));
    vec2 texel = 1.0 / resolution;

    float flameStrength = clamp(BloomMotion.y, 0.0, 1.5);
    float detail = clamp(BloomMotion.w, 0.2, 1.5);
    float time = BloomMotion.x * clamp(BloomMotion.z, 0.1, 5.0);

    // The widest blur behaves like a signed distance field around the
    // silhouette: it sits near its own peak on the boundary and decays smoothly
    // outward. Normalizing it gives an outward distance in [0, 1] that follows
    // the real curvature of the model, which is what the flame is built on
    // instead of any kind of edge detection.
    // The reference peak is kept high so the field has to be genuinely close to
    // a silhouette to count as "just outside" it. A low reference lets the
    // overlapping tails of several entities pass for a surface and the flame
    // spreads across the gaps between them.
    float fieldPeak = mix(0.30, 0.55, pow(distanceScale, 0.8));
    float distanceOut = 1.0 - pow(clamp(hazeCoverage / fieldPeak, 0.0, 1.0), 0.38);

    // Fire only climbs where the silhouette is below, which is what makes it
    // wrap the model like real flame instead of forming an even ring around it.
    //
    // This is deliberately a normalized *ratio* of the field above and below,
    // not a normalized gradient vector. Dividing a two component difference by
    // its own tiny length amplifies the C1 kinks that bilinear sampling leaves
    // on the texel grid, and those kinks then print a regular lattice across the
    // entire flame. A ratio of two positive samples has no such amplification.
    float probe = mix(3.0, 10.0, distanceScale);
    float fieldBelow = coverageOf(texture(HazeSampler, texCoord - vec2(0.0, probe) * texel));
    float fieldAbove = coverageOf(texture(HazeSampler, texCoord + vec2(0.0, probe) * texel));
    float vertical = (fieldBelow - fieldAbove) / max(fieldBelow + fieldAbove, 0.004);
    // Zero means the silhouette extends equally far up and down, which is the
    // case along the flanks of a tall model. Those flanks still need flame, so
    // the range is biased to give them a solid share of the full height.
    float upness = smoothstep(-0.22, 0.60, vertical);

    // Aspect corrected and resolution independent, so the flame keeps its shape
    // at any window size or GUI scale. A distant entity covers fewer pixels, so
    // its noise frequency goes up to keep the same amount of detail per entity.
    // Offsetting by the camera angle pins the pattern to the world instead of to
    // the monitor. Without it every camera turn drags all the entities across a
    // stationary noise field and the flame is seen crawling over them.
    vec2 noiseUV = vec2(texCoord.x * resolution.x / resolution.y, texCoord.y)
        + BloomView.xy;
    float noiseScale = mix(10.0, 28.0, clamp((detail - 0.2) / 1.3, 0.0, 1.0))
        * mix(2.1, 1.0, distanceScale);

    // Anisotropic on purpose: compressed horizontally and stretched vertically,
    // so the features are tall and narrow. Isotropic noise produces round blobs,
    // which is what makes most "fire" shaders read as a lava lamp.
    vec2 flameScale = vec2(noiseScale * 1.55, noiseScale * 0.52);

    // One unit of noiseUV spans resolution.y pixels on both axes, so the
    // shortest wavelength of the first octave is measured off the larger scale.
    float basePeriodPixels = resolution.y / max(flameScale.x, flameScale.y);

    // Domain warp: a slow large scale swirl bends the fine octaves, which is
    // what makes the tongues curl and shear instead of sliding straight up.
    vec2 warpUV = noiseUV * flameScale * 0.42 + vec2(time * 0.09, -time * 0.22);
    float warpPeriod = basePeriodPixels / 0.42;
    vec2 warp = vec2(
        fbmBanded(warpUV, warpPeriod, 3),
        fbmBanded(warpUV + vec2(4.7, 1.3), warpPeriod, 3)
    ) - 0.5;

    vec2 flameUV = noiseUV * flameScale
        + vec2(warp.x * 2.4, warp.y * 0.9)
        + vec2(sin(time * 0.55) * 0.25, -time * 1.35);
    float flameNoise = fbmBanded(flameUV, basePeriodPixels, 5);
    float tipNoise = fbmBanded(
        flameUV * vec2(2.1, 1.7) + vec2(3.3, -time * 1.9),
        basePeriodPixels / 2.1,
        3
    );
    float streakNoise = fbmBanded(
        flameUV * vec2(2.55, 1.9) + vec2(1.7, -time * 2.4),
        basePeriodPixels / 2.55,
        3
    );
    float heightNoise = clamp(flameNoise * 0.70 + tipNoise * 0.30, 0.0, 1.0);

    // Flame height per pixel. Both the height and the distance it is compared
    // against are smooth fields, so the tongue boundary is a smooth curve at any
    // zoom level - there is nothing left that can quantize to the pixel grid.
    // Steep on purpose. Height is spent almost entirely upward, where there is
    // nothing to collide with, and the flanks stay tight - that is what keeps a
    // packed group of mobs from growing sideways into each other.
    float rise = mix(0.15, 1.0, pow(upness, 1.5));
    // The ceiling stops well short of 1.0 on purpose: the last stretch of the
    // distance field is its long faint tail, which is exactly where neighbouring
    // entities overlap, so flame allowed to reach it bridges between them.
    float flameHeight = clamp(
        (0.12 + 0.96 * pow(heightNoise, 1.25)) * rise * min(1.3, flameStrength),
        0.0,
        0.88
    );
    float feather = 0.055 + 0.075 * (1.0 - upness);
    float flame = 1.0 - smoothstep(
        flameHeight - feather,
        flameHeight + feather,
        distanceOut
    );
    // At zero height the smoothstep is evaluated at its own midpoint and returns
    // a half covered flame instead of none, which pools into every concave
    // pocket of the model. Flame Shape 0 has to mean exactly no flame.
    flame *= smoothstep(0.0, 0.05, flameHeight);

    // Antialiased silhouette, also used to keep the flame outside the model.
    float interior = smoothstep(0.42, 0.86, shapeCoverage);
    flame *= 1.0 - interior;

    float alongFlame = clamp(distanceOut / max(flameHeight, 0.0001), 0.0, 1.0);
    float baseFalloff = pow(1.0 - alongFlame, 0.70);

    float flameBody = flame * mix(0.30, 1.0, baseFalloff) * mix(0.80, 1.22, streakNoise);
    float flameTips = flame * smoothstep(0.52, 1.0, alongFlame);

    // Thin hot line hugging the silhouette, taken as a band just outside the
    // 0.5 iso-line of the tight blur. It follows the real curvature of the
    // model and is antialiased by construction.
    float rim = smoothstep(0.06, 0.28, coreCoverage)
        * (1.0 - smoothstep(0.28, 0.66, coreCoverage));

    // Warm base glow packed against the model, plus the broad ambient halo.
    // Deliberately slow to saturate: a narrow gap such as the space between two
    // legs collects field from both sides, and a fast saturating curve turns
    // that pocket into a flat wedge with a hard diagonal border.
    float base = pow(clamp(wideCoverage / mix(0.28, 0.62, distanceScale), 0.0, 1.0), 1.6);
    // Exponent above one on purpose. A root curve lifts the long thin tail of
    // the blur into a visible film that covers everything near the entity; this
    // keeps the halo tight against the model and lets it die out quickly.
    float halo = pow(clamp(hazeCoverage / fieldPeak, 0.0, 1.0), 1.0);

    // Sparse embers drifting off the tips. Deliberately rare and small - they
    // read as motion, not as dots.
    float emberNoise = valueNoise(flameUV * vec2(2.2, 1.7) + vec2(11.3, -time * 3.1))
        * smoothstep(1.8, 4.5, basePeriodPixels / 2.2);
    float embers = smoothstep(0.945, 0.999, emberNoise)
        * smoothstep(0.12, 0.55, distanceOut)
        * (1.0 - smoothstep(0.72, 0.97, distanceOut))
        * upness
        * step(0.05, flameStrength);

    float energy = 1.0 - exp(-(
        rim * 1.35 * BloomShape.x
        + flameBody * 0.95 * BloomShape.y
        + flameTips * 0.22
        + base * 0.18 * BloomShape.y
        + halo * 0.15 * BloomShape.z
        + embers * 0.40
    ));

    // Hottest against the model, coolest at the tips - the gradient that makes
    // a flame read as a flame rather than as a coloured blur.
    float heat = clamp(
        rim * 0.85
        + base * 0.30
        + flameBody * 0.75 * baseFalloff
        + embers * 1.10
        - flameTips * 0.35,
        0.0,
        1.0
    );

    // The cool end of the ramp is the full tint, never a darkened one. Fire is
    // emissive: a colour darker than the scenery behind it subtracts light and
    // shows up as a grey smear over everything near the entity, which is very
    // obvious against bright terrain. Depth comes from opacity and brightness
    // below, not from mixing towards black.
    vec3 hotColor = mix(tint, vec3(1.0), 0.45);
    vec3 flameColor = mix(tint, hotColor, smoothstep(0.30, 1.0, heat));

    // Kept deliberately close to 1.0. The outline target is 8 bit, so anything
    // that pushes the tint past full scale clips channel by channel and the
    // selected colour turns into plain white - worst of all on a distant entity,
    // whose glow is almost entirely rim and which is exactly where the colour
    // coding needs to survive.
    float brightness = 1.0
        + rim * 0.18
        + flameBody * 0.40
        + flameTips * 0.10
        + embers * 0.90
        + halo * 0.05;

    // Fire has to read as light, not as paint. Opacity is traded away with
    // distance from the model while brightness carries the look instead, so the
    // scenery around an entity stays readable rather than being veiled over.
    float alpha = clamp(energy * ColorModulate.a, 0.0, 0.96)
        * mix(1.0, 0.42, alongFlame);

    float encodedMask = texture(MaskSampler, texCoord).a;
    bool hitInterior = encodedMask >= 0.69 && encodedMask < 0.98;

    if (hitInterior) {
        // Hit Color is an intentionally intense fill, faded in along the same
        // antialiased silhouette.
        float hitAlpha = min(0.96, 0.80 + ColorModulate.a * 0.16);
        alpha = max(alpha, hitAlpha * smoothstep(0.15, 0.60, shapeCoverage));
        brightness *= BloomShape.w;
        flameColor = mix(flameColor, hotColor, 0.35);
    } else {
        // Normal entities and world-space afterimages keep a see-through
        // interior so the model, scenery and particles stay readable.
        alpha *= mix(1.0, 0.035, interior);
    }

    fragColor = vec4(flameColor * ColorModulate.rgb * brightness, alpha);
}

