#version 150

uniform sampler2D Sampler0;
uniform vec2 rectSize;
uniform vec2 screenSize;
uniform float radius;
uniform float opacity;
uniform float distortion;
uniform float edgeLight;
uniform float shine;
uniform float gameTime;
uniform vec4 tintColor;
uniform vec4 edgeColor;
uniform float hover;

in vec2 texCoord;
out vec4 fragColor;

float roundedBoxSDF(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float smoothNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash21(i), hash21(i + vec2(1.0, 0.0)), f.x),
               mix(hash21(i + vec2(0.0, 1.0)), hash21(i + vec2(1.0, 1.0)), f.x), f.y);
}

// Low-frequency smooth continuous domain-warped field (no wave stripes or high-frequency veins)
float lowFreqField(vec2 p, float time) {
    vec2 q = vec2(smoothNoise(p + vec2(time * 0.04, time * 0.025)),
                  smoothNoise(p + vec2(-time * 0.03, time * 0.035) + vec2(2.4, 1.9)));
    return smoothNoise(p + 1.5 * q + vec2(time * 0.02, -time * 0.02));
}

void main() {
    vec2 halfSize = rectSize * 0.5;
    vec2 localPoint = texCoord * rectSize - halfSize;
    float dist = roundedBoxSDF(localPoint, max(halfSize - 0.5, 0.0), radius);
    float aa = clamp(fwidth(dist), 0.75, 1.5);
    float mask = 1.0 - smoothstep(0.0, aa, dist);
    if (mask <= 0.0001) {
        discard;
    }

    // Normalized coordinates from center (-1.0 to 1.0)
    vec2 normPos = localPoint / max(halfSize, vec2(1.0));
    vec2 dir = normalize(normPos + vec2(0.00001));
    float distFromCenter = length(normPos);

    // Distance to inner edge (0 at boundary, increases inwards)
    float innerDist = max(0.0, -dist);
    float edgeZone = max(radius * 0.6, 6.0);
    float edgeFactor = 1.0 - clamp(innerDist / edgeZone, 0.0, 1.0);
    float edgeCurve = pow(edgeFactor, 1.8);

    // Smooth low-frequency liquid displacement field
    float n = lowFreqField(normPos * 0.75, gameTime * 0.05);
    vec2 waveGrad = vec2(dFdx(n), dFdy(n)) * 1.5;

    // Center has almost zero displacement; perimeter has subtle lens refraction
    float lensGradient = smoothstep(0.08, 0.92, distFromCenter) * edgeCurve;
    float refrMagnitude = distortion * (0.12 + 0.88 * lensGradient);
    vec2 uvOffset = (dir * refrMagnitude * 0.65 + waveGrad * (refrMagnitude * 0.35));

    // Sample real framebuffer behind the component with clamped coordinates
    vec2 screenUV = gl_FragCoord.xy / screenSize;
    vec2 sampleUV = clamp(screenUV + uvOffset, vec2(0.001), vec2(0.999));
    vec3 bgSample = texture(Sampler0, sampleUV).rgb;

    // Dark neutral graphite glass base (preserves warm lights, sky, and scene detail)
    vec3 baseGlassColor = tintColor.rgb;
    float baseAlpha = clamp(tintColor.a, 0.20, 0.75);

    // Slight glass thickness impression (edge darkening)
    float thicknessDarkening = edgeCurve * 0.08;
    vec3 surface = mix(bgSample, baseGlassColor, clamp(baseAlpha + thicknessDarkening, 0.0, 0.85));

    // Crisp 1px inner border outline derived from SDF distance
    float borderDist = abs(dist + 0.6);
    float borderFactor = 1.0 - smoothstep(0.0, aa, borderDist);
    vec3 edgeTint = edgeColor.a > 0.0 ? edgeColor.rgb : vec3(0.95, 0.97, 1.0);
    float borderAlpha = clamp(edgeColor.a * (0.85 + 0.35 * hover), 0.05, 0.65);
    surface = mix(surface, edgeTint, borderFactor * borderAlpha);

    // Restrained Fresnel edge & subtle top specular rim
    float fresnel = pow(edgeFactor, 2.6);
    float topRim = smoothstep(1.8, 0.0, abs(localPoint.y + halfSize.y - 1.0)) * (1.0 - abs(normPos.x) * 0.45) * 0.075;
    vec2 lightDir = normalize(vec2(-0.35, -0.92));
    float edgeSpecular = pow(max(0.0, dot(dir, lightDir)), 8.0) * edgeFactor * 0.085;

    float totalEdgeLight = (fresnel * 0.055 + topRim + edgeSpecular) * edgeLight;
    surface += edgeTint * totalEdgeLight;

    // Subtle, slow liquid specular sheen (alive surface without visible waves)
    float specNoise = smoothstep(0.40, 0.85, n);
    float specIntensity = specNoise * shine * 0.035 * (1.0 - edgeFactor * 0.45);
    surface += vec3(1.0, 1.0, 1.0) * specIntensity;

    fragColor = vec4(clamp(surface, 0.0, 1.0), mask * opacity);
}
