#version 410 core

layout(std140) uniform SkyData {
    vec4 SkyColor;
    vec4 SkyParams;
    vec3 CameraPos;
};

in vec3 SkyDirection;
out vec4 fragColor;

float hash31(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float noise3(vec3 p) {
    vec3 cell = floor(p);
    vec3 local = fract(p);
    local = local * local * (3.0 - 2.0 * local);

    float n000 = hash31(cell + vec3(0.0, 0.0, 0.0));
    float n100 = hash31(cell + vec3(1.0, 0.0, 0.0));
    float n010 = hash31(cell + vec3(0.0, 1.0, 0.0));
    float n110 = hash31(cell + vec3(1.0, 1.0, 0.0));
    float n001 = hash31(cell + vec3(0.0, 0.0, 1.0));
    float n101 = hash31(cell + vec3(1.0, 0.0, 1.0));
    float n011 = hash31(cell + vec3(0.0, 1.0, 1.0));
    float n111 = hash31(cell + vec3(1.0, 1.0, 1.0));

    float nx00 = mix(n000, n100, local.x);
    float nx10 = mix(n010, n110, local.x);
    float nx01 = mix(n001, n101, local.x);
    float nx11 = mix(n011, n111, local.x);
    return mix(mix(nx00, nx10, local.y), mix(nx01, nx11, local.y), local.z);
}

float fbm(vec3 p) {
    float val = 0.0;
    float amplitude = 0.55;
    for (int i = 0; i < 5; i++) {
        val += noise3(p) * amplitude;
        p = p * 2.03 + vec3(1.7, -2.1, 0.8);
        amplitude *= 0.5;
    }
    return val;
}

float fbmLow(vec3 p) {
    float val = 0.0;
    float amplitude = 0.58;
    for (int i = 0; i < 3; i++) {
        val += noise3(p) * amplitude;
        p = p * 2.03 + vec3(1.7, -2.1, 0.8);
        amplitude *= 0.48;
    }
    return val;
}

vec3 starField(vec3 dir, vec3 tint, float density, float strength) {
    vec3 cell = floor(dir * 980.0);
    float star = step(density, hash31(cell));
    float sparkle = 0.55 + 0.45 * sin(hash31(cell + 17.0) * 6.2831 + SkyParams.x * 1.8);
    star *= smoothstep(-0.18, 0.55, dir.y);
    return mix(vec3(1.0), tint, 0.34) * star * sparkle * strength;
}

vec3 auroraSky(vec3 dir, vec3 baseColor, float time, float intensity) {
    vec3 flow = dir * 2.6 + vec3(time * 0.055, -time * 0.025, time * 0.04);
    float broad = fbm(flow * 1.15);
    vec3 warped = flow + vec3(
        broad * 1.4,
        fbm(flow * 0.85 + 7.2) * 0.8,
        fbm(flow * 0.95 - 4.6) * 1.2
    );

    float detail = fbm(warped * 1.75 + vec3(-time * 0.08, time * 0.035, time * 0.06));
    float ribbon = 1.0 - abs(sin(warped.y * 4.2 + warped.x * 1.35 - warped.z * 1.1 + detail * 6.0 - time * 0.55));
    float secondary = 1.0 - abs(sin(warped.y * 6.5 - warped.x * 1.8 + broad * 7.5 + time * 0.32));
    ribbon = pow(clamp(ribbon, 0.0, 1.0), 3.4);
    secondary = pow(clamp(secondary, 0.0, 1.0), 5.0);

    float cloud = smoothstep(0.28, 0.82, detail);
    float valleys = 1.0 - smoothstep(0.18, 0.68, broad);
    float waveLight = clamp(ribbon * 0.88 + secondary * 0.42, 0.0, 1.0);

    vec3 color = mix(baseColor * 0.12, baseColor * (0.42 + cloud * 0.48), cloud);
    color *= 0.68 + valleys * 0.48;
    color = mix(color, mix(baseColor, vec3(1.0), 0.58), waveLight * 0.72 * intensity);
    return color;
}

vec3 nebulaSky(vec3 dir, vec3 baseColor, float time, float intensity) {
    vec3 p = dir * 3.0 + vec3(time * 0.025, time * 0.012, -time * 0.018);
    float cloudA = fbm(p * 1.25);
    float cloudB = fbm(p * 2.2 + vec3(4.0, -2.3, 6.1));
    float core = smoothstep(0.38, 0.88, cloudA + cloudB * 0.35);
    float dust = smoothstep(0.22, 0.72, fbm(p * 4.0 - vec3(time * 0.04)));
    vec3 hot = mix(baseColor, vec3(1.0), 0.62);
    vec3 cool = mix(baseColor * vec3(0.42, 0.36, 0.72), vec3(0.08, 0.02, 0.18), 0.36);
    vec3 color = mix(cool, baseColor * 0.85, dust * 0.42);
    color = mix(color, hot, core * 0.78 * intensity);
    color += starField(dir, hot, 0.9978, 0.75);
    return color;
}

vec3 voidSky(vec3 dir, vec3 baseColor, float time, float intensity) {
    float vignette = smoothstep(-0.65, 0.85, dir.y);
    vec3 deep = mix(vec3(0.005, 0.006, 0.014), baseColor * 0.16, 0.45 + vignette * 0.35);
    float haze = pow(clamp(1.0 - abs(dir.y), 0.0, 1.0), 4.0);
    float slowNoise = fbm(dir * 2.8 + vec3(time * 0.01, 0.0, -time * 0.015));
    vec3 color = deep + baseColor * haze * (0.08 + slowNoise * 0.08) * intensity;
    color += starField(dir, mix(baseColor, vec3(1.0), 0.78), 0.9969, 0.95 * intensity);
    color += starField(dir * 1.7 + vec3(3.0), vec3(1.0), 0.9991, 1.3);
    return color;
}

vec3 prismSky(vec3 dir, vec3 baseColor, float time, float intensity) {
    float facets = fbm(floor((dir + 1.0) * 7.0) * 0.42 + time * 0.025);
    float bands = sin((dir.x + dir.y * 0.7 - dir.z * 0.35) * 18.0 + facets * 4.5 + time * 0.35);
    bands = pow(clamp(0.5 + 0.5 * bands, 0.0, 1.0), 2.2);
    vec3 shiftedA = baseColor.brg;
    vec3 shiftedB = baseColor.gbr;
    vec3 glass = mix(baseColor * 0.18, shiftedA * 0.75, bands);
    glass = mix(glass, mix(shiftedB, vec3(1.0), 0.44), smoothstep(0.55, 0.92, facets) * intensity);
    float horizon = pow(clamp(1.0 - abs(dir.y), 0.0, 1.0), 2.8);
    return glass + mix(baseColor, vec3(1.0), 0.5) * horizon * 0.16 * intensity;
}

vec3 stormSky(vec3 dir, vec3 baseColor, float time, float intensity) {
    vec3 p = dir * 3.4 + vec3(time * 0.045, -time * 0.02, time * 0.018);
    float cloud = fbm(p * 1.45);
    float heavy = smoothstep(0.18, 0.85, cloud);
    vec3 color = mix(vec3(0.018, 0.02, 0.03), baseColor * 0.34, heavy);
    float flashSeed = hash31(vec3(floor(time * 0.75), 13.0, 4.0));
    float flashTime = pow(max(0.0, sin(time * 18.0 + flashSeed * 6.2831)), 20.0);
    float boltMask = 1.0 - abs(sin(dir.x * 42.0 + dir.y * 28.0 + fbm(p * 4.5) * 7.0));
    float lightning = pow(clamp(boltMask, 0.0, 1.0), 18.0) * step(0.72, flashSeed) * flashTime;
    color += mix(baseColor, vec3(0.86, 0.92, 1.0), 0.78) * lightning * (1.4 + intensity);
    color += baseColor * pow(clamp(1.0 - abs(dir.y), 0.0, 1.0), 3.2) * 0.13 * intensity;
    return color;
}

float segmentDistance(vec2 p, vec2 a, vec2 b, out float along) {
    vec2 ba = b - a;
    along = clamp(dot(p - a, ba) / max(dot(ba, ba), 0.00001), 0.0, 1.0);
    return length(p - (a + ba * along));
}

float featherStroke(vec2 p, vec2 root, vec2 tip, float rootWidth, float tipWidth) {
    float along;
    float distanceToShaft = segmentDistance(p, root, tip, along);
    float width = mix(rootWidth, tipWidth, smoothstep(0.05, 0.92, along));
    float body = 1.0 - smoothstep(width * 0.72, width, distanceToShaft);
    float pointedTip = 1.0 - smoothstep(0.91, 1.0, along);
    return body * mix(1.0, pointedTip, smoothstep(0.72, 1.0, along));
}

float driftingLightDust(vec3 dir, float time, float scale, float seed) {
    vec2 sphericalUv = vec2(
        atan(dir.x, dir.z) / 6.2831853 + 0.5,
        dir.y * 0.5 + 0.5
    );
    vec2 movingUv = sphericalUv * vec2(42.0, 24.0) * scale;
    movingUv += vec2(time * 0.018, -time * 0.034) * (0.7 + seed * 0.13);

    vec2 cell = floor(movingUv);
    vec2 local = fract(movingUv) - 0.5;
    vec2 randomOffset = vec2(
        hash31(vec3(cell, 11.7 + seed)),
        hash31(vec3(cell.yx, 37.1 + seed))
    ) - 0.5;
    float distanceToParticle = length(local - randomOffset * 0.74);
    float particle = 1.0 - smoothstep(0.018, 0.070, distanceToParticle);
    float exists = step(0.62, hash31(vec3(cell + 4.3, 71.0 + seed)));
    float twinkle = 0.42 + 0.58 * sin(
        time * (1.4 + seed * 0.11) + hash31(vec3(cell, seed + 9.0)) * 6.2831853
    );
    return particle * exists * max(twinkle, 0.12);
}

vec3 heavenlyWrathSky(vec3 dir, vec3 baseColor, float time, float intensity) {
    // A wine-red dome with slowly advecting, internally lit cloud banks.
    float horizon = pow(clamp(1.0 - abs(dir.y), 0.0, 1.0), 2.0);
    float upper = smoothstep(-0.12, 0.82, dir.y);
    vec3 wine = vec3(0.075, 0.003, 0.035);
    vec3 crimson = mix(max(baseColor, vec3(0.38, 0.025, 0.15)), vec3(0.92, 0.025, 0.30), 0.48);
    vec3 color = mix(crimson * 0.66, wine, upper * 0.82);
    color += crimson * horizon * 0.42;

    // A slow high-altitude veil makes the dome move even where there are no clouds.
    float veilNoise = fbmLow(dir * 2.15 + vec3(time * 0.010, -time * 0.006, time * 0.014));
    float veilBands = 0.5 + 0.5 * sin(
        dir.x * 9.5 - dir.z * 5.2 + dir.y * 12.0 + veilNoise * 5.8 - time * 0.075
    );
    veilBands = pow(clamp(veilBands, 0.0, 1.0), 5.0) * smoothstep(-0.05, 0.72, dir.y);
    color += mix(crimson, vec3(1.0, 0.15, 0.47), 0.38) * veilBands * 0.065;

    vec3 cloudFlow = dir * 3.35 + vec3(time * 0.022, time * 0.004, -time * 0.031);
    float cloudWarp = fbm(cloudFlow * 0.72 + vec3(1.7, -0.8, 3.1));
    float cloudFine = fbm(cloudFlow * 1.48 + vec3(cloudWarp * 1.7, 2.6, -cloudWarp));
    float cloudBody = smoothstep(0.52, 0.79, cloudFine + cloudWarp * 0.26);
    cloudBody *= smoothstep(-0.28, 0.42, dir.y);
    float cloudEdge = smoothstep(0.43, 0.63, cloudFine) - smoothstep(0.67, 0.84, cloudFine);
    vec3 cloudShadow = mix(vec3(0.18, 0.006, 0.075), crimson * 0.58, cloudWarp);
    vec3 cloudLight = mix(crimson, vec3(1.0, 0.20, 0.48), 0.38 + cloudWarp * 0.28);
    color = mix(color, cloudShadow, cloudBody * 0.48);
    color += cloudLight * cloudEdge * (0.12 + horizon * 0.12);

    // A second, faster and thinner layer produces parallax against the main banks.
    vec3 wispyFlow = dir * 5.6 + vec3(-time * 0.041, time * 0.008, time * 0.026);
    float wispyNoise = fbmLow(wispyFlow + vec3(cloudWarp * 0.8));
    float wisps = smoothstep(0.56, 0.76, wispyNoise);
    wisps *= smoothstep(-0.38, 0.30, dir.y) * (1.0 - smoothstep(0.72, 0.94, dir.y));
    color = mix(color, vec3(0.72, 0.035, 0.25), wisps * 0.16);
    color += vec3(1.0, 0.19, 0.43) * wisps * cloudEdge * 0.10;

    float horizonShimmerNoise = fbmLow(vec3(dir.xz * 8.0, time * 0.018));
    float horizonShimmer = horizon * pow(clamp(
        0.5 + 0.5 * sin(dir.x * 37.0 + dir.z * 29.0 + horizonShimmerNoise * 4.0 - time * 0.19),
        0.0,
        1.0
    ), 8.0);
    color += vec3(1.0, 0.08, 0.32) * horizonShimmer * 0.075;

    // The apparition replaces the sun. Its tiny drift and breathing keep it alive
    // while the world-space direction keeps it fixed in the sky as the camera turns.
    vec3 sourceDirection = normalize(vec3(
        sin(time * 0.035) * 0.018,
        0.13 + sin(time * 0.057) * 0.012,
        1.0
    ));
    vec3 wingRight = normalize(cross(vec3(0.0, 1.0, 0.0), sourceDirection));
    vec3 wingUp = normalize(cross(sourceDirection, wingRight));
    float facing = dot(dir, sourceDirection);
    vec2 wingUv = vec2(dot(dir, wingRight), dot(dir, wingUp)) / max(facing, 0.16);
    float visible = smoothstep(0.42, 0.78, facing);
    float flap = sin(time * 1.18) * 0.012 + sin(time * 0.41) * 0.006;

    vec2 p = vec2(abs(wingUv.x), wingUv.y);
    float wing = 0.0;
    float featherDetail = 0.0;

    // Long primaries fan upward and outward; each has its own subtle phase.
    for (int i = 0; i < 9; i++) {
        float fi = float(i);
        float spread = fi / 8.0;
        vec2 root = vec2(0.030 + spread * 0.105, -0.018 + spread * 0.040);
        vec2 tip = vec2(
            0.145 + spread * 0.385,
            0.105 + spread * 0.335 + flap * (0.25 + spread)
                + sin(time * 0.72 + fi * 0.83) * 0.004
        );
        float feather = featherStroke(p, root, tip, 0.034 - spread * 0.010, 0.011 - spread * 0.003);
        wing = max(wing, feather);
        featherDetail += feather * (0.48 + 0.52 * sin(fi * 2.17 + p.x * 62.0 - p.y * 31.0));
    }

    // Downward coverts create the layered lower edge visible in the reference.
    for (int i = 0; i < 7; i++) {
        float fi = float(i);
        float spread = fi / 6.0;
        vec2 root = vec2(0.022 + spread * 0.105, -0.026 + spread * 0.012);
        vec2 tip = vec2(
            0.125 + spread * 0.315,
            0.015 - spread * 0.185 + flap * (0.18 + spread * 0.45)
        );
        float feather = featherStroke(p, root, tip, 0.030 - spread * 0.009, 0.010);
        wing = max(wing, feather * (0.96 - spread * 0.10));
        featherDetail += feather * (0.36 + 0.28 * cos(fi * 1.73 + p.y * 54.0));
    }

    float shoulderAlong;
    float shoulder = segmentDistance(p, vec2(0.015, -0.010), vec2(0.225, 0.135 + flap), shoulderAlong);
    shoulder = 1.0 - smoothstep(0.070, 0.094, shoulder);
    wing = max(wing, shoulder);
    wing *= visible;

    float coreDistance = length(wingUv * vec2(1.0, 1.32));
    float breath = 1.0 + sin(time * 1.55) * 0.045;
    float core = exp(-coreDistance * coreDistance * 165.0 / breath);
    float innerAura = exp(-coreDistance * coreDistance * 34.0);
    float outerAura = exp(-coreDistance * coreDistance * 7.5);
    float wingAura = exp(-max(abs(p.y - 0.07) - 0.12, 0.0) * 13.0)
        * exp(-max(p.x - 0.48, 0.0) * 10.0)
        * (1.0 - smoothstep(0.02, 0.52, p.x)) * 0.12;

    float raysAngle = atan(wingUv.y, wingUv.x);
    float rays = pow(max(0.0, 0.5 + 0.5 * sin(raysAngle * 18.0 + time * 0.22)), 13.0);
    rays *= exp(-coreDistance * 5.2) * (1.0 - smoothstep(0.055, 0.54, coreDistance));

    // Long volumetric spokes continue through the clouds instead of stopping at the halo.
    float rayWarp = fbmLow(vec3(wingUv * 3.4, time * 0.025));
    float longRays = pow(max(0.0, 0.5 + 0.5 * sin(
        raysAngle * 13.0 + rayWarp * 3.5 - time * 0.09
    )), 16.0);
    longRays *= exp(-coreDistance * 1.75) * visible;
    longRays *= 0.35 + cloudEdge * 0.9 + wisps * 0.4;

    // Two expanding energy fronts make the source feel powerful without hiding the wings.
    float ringPhaseA = fract(time * 0.105);
    float ringPhaseB = fract(time * 0.105 + 0.5);
    float ringRadiusA = mix(0.10, 0.92, ringPhaseA);
    float ringRadiusB = mix(0.10, 0.92, ringPhaseB);
    float ringA = exp(-abs(coreDistance - ringRadiusA) * 72.0) * (1.0 - ringPhaseA);
    float ringB = exp(-abs(coreDistance - ringRadiusB) * 72.0) * (1.0 - ringPhaseB);
    float energyRings = (ringA + ringB) * visible;

    // Rare distant discharges illuminate a cloud column for a fraction of a second.
    float flashCycle = floor(time * 0.16);
    float flashProgress = fract(time * 0.16);
    float flashSeed = hash31(vec3(flashCycle, 83.2, 19.7));
    float distantFlash = step(0.76, flashSeed) * exp(-flashProgress * 22.0);
    distantFlash *= 0.78 + 0.22 * sin(flashProgress * 95.0);
    float boltSide = mix(-0.88, 0.88, step(0.5, hash31(vec3(flashCycle, 4.8, 61.0))));
    float boltBend = sin(wingUv.y * 29.0 + flashSeed * 9.0) * 0.027;
    float boltLine = 1.0 - smoothstep(0.006, 0.017, abs(wingUv.x - boltSide - boltBend));
    float boltHeight = smoothstep(-0.42, -0.31, wingUv.y)
        * (1.0 - smoothstep(0.06, 0.18, wingUv.y));
    float lightning = boltLine * boltHeight * distantFlash * visible;

    // Tiny lights drift at two depths and twinkle independently.
    float dustNear = driftingLightDust(dir, time, 1.0, 2.0);
    float dustFar = driftingLightDust(dir, time, 1.73, 7.0) * 0.58;
    float lightDust = (dustNear + dustFar) * smoothstep(-0.30, 0.72, dir.y);
    lightDust *= 1.0 - cloudBody * 0.58;

    vec3 roseGlow = vec3(1.0, 0.08, 0.38);
    vec3 warmWhite = vec3(1.0, 0.88, 0.94);
    color += roseGlow * outerAura * 0.92;
    color += roseGlow * wingAura;
    color += mix(vec3(1.0, 0.48, 0.68), warmWhite, clamp(wing + featherDetail * 0.08, 0.0, 1.0))
        * wing * (1.55 + intensity * 0.48);
    color += warmWhite * rays * 0.48;
    color += mix(roseGlow, warmWhite, 0.66) * longRays * (0.14 + intensity * 0.07);
    color += roseGlow * energyRings * (0.20 + intensity * 0.06);
    color += warmWhite * lightning * (1.2 + intensity * 0.55);
    color += vec3(1.0, 0.42, 0.67) * distantFlash * cloudBody * 0.12;
    color += mix(roseGlow, warmWhite, 0.72) * lightDust * (0.52 + intensity * 0.14);
    color += mix(roseGlow, warmWhite, innerAura) * innerAura * 1.65;
    color += warmWhite * core * (3.2 + intensity);

    // The apparition softly illuminates nearby cloud vapour.
    float sourceCloudLight = outerAura * cloudBody * (0.46 + cloudEdge * 0.5);
    color += vec3(1.0, 0.16, 0.42) * sourceCloudLight;
    return color;
}

void main() {
    vec3 dir = normalize(SkyDirection);
    float time = SkyParams.x * max(SkyParams.w, 0.01);
    float intensity = max(SkyParams.z, 0.0);
    int mode = int(floor(SkyParams.y + 0.5));
    vec3 baseColor = max(SkyColor.rgb, vec3(0.015));
    vec3 color;

    if (mode == 5) {
        color = heavenlyWrathSky(dir, baseColor, time, intensity);
    } else if (mode == 1) {
        color = nebulaSky(dir, baseColor, time, intensity);
    } else if (mode == 2) {
        color = voidSky(dir, baseColor, time, intensity);
    } else if (mode == 3) {
        color = prismSky(dir, baseColor, time, intensity);
    } else if (mode == 4) {
        color = stormSky(dir, baseColor, time, intensity);
    } else {
        color = auroraSky(dir, baseColor, time, intensity);
    }

    float horizonGlow = pow(clamp(1.0 - abs(dir.y), 0.0, 1.0), 3.0);
    color += baseColor * horizonGlow * 0.08 * intensity;

    color *= mix(0.72, 1.25, clamp(intensity, 0.0, 2.5) / 2.5);
    if (mode == 5) {
        color = vec3(1.0) - exp(-max(color, vec3(0.0)) * 1.18);
        color = pow(color, vec3(0.88));
    }
    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
