#version 150

#moj_import <minecraft:fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;
in vec3 viewNormal;
in vec3 viewPos;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0);
#ifdef ALPHA_CUTOUT
    if (color.a < ALPHA_CUTOUT) {
        discard;
    }
#endif
    color *= vertexColor * ColorModulator;
#ifndef NO_OVERLAY
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
#endif
#ifndef EMISSIVE
    color *= lightMapColor;
#endif

    vec3 normal = normalize(viewNormal);
    vec3 viewDir = normalize(-viewPos);
    vec3 reflectDir = reflect(-viewDir, normal);
    float facing = clamp(dot(normal, viewDir), 0.0, 1.0);
    float fresnel = pow(1.0 - facing, 2.8);
    float rim = smoothstep(0.08, 1.0, fresnel);
    float innerGlow = pow(facing, 1.45);
    float shimmer = 0.5 + 0.5 * sin((texCoord0.x + texCoord0.y) * 18.0 + viewPos.y * 1.3);
    float verticalSweep = 0.5 + 0.5 * sin(viewPos.y * 5.2 + viewPos.x * 1.1);
    float diagonalSweep = 0.5 + 0.5 * sin((viewPos.x + viewPos.z) * 3.8 - viewPos.y * 2.4);
    float envTop = smoothstep(0.12, 0.95, reflectDir.y * 0.5 + 0.5);
    float envSide = pow(1.0 - abs(reflectDir.x), 2.4);
    float envBack = smoothstep(-0.35, 0.65, reflectDir.z);
    float pearlWaveA = 0.5 + 0.5 * sin(viewPos.y * 4.6 + texCoord0.x * 24.0);
    float pearlWaveB = 0.5 + 0.5 * sin(viewPos.x * 3.2 - viewPos.z * 2.8 + texCoord0.y * 21.0);
    float sparkle = pow(max(0.0, sin((viewPos.x + viewPos.y + viewPos.z) * 9.0 + texCoord0.x * 32.0)), 18.0);
    float ridge = pow(clamp(1.0 - abs(dot(normalize(viewNormal.xzy + vec3(0.18, 0.0, 0.12)), viewDir)), 0.0, 1.0), 6.0);

    vec3 milk = vec3(0.92, 0.975, 1.0);
    vec3 ice = vec3(0.56, 0.84, 1.0);
    vec3 aqua = vec3(0.46, 0.92, 0.98);
    vec3 pearl = mix(vec3(0.78, 0.90, 1.0), vec3(0.66, 0.98, 0.94), pearlWaveA * 0.45 + pearlWaveB * 0.55);
    vec3 frostTint = mix(color.rgb, milk, 0.66);
    vec3 body = mix(frostTint, pearl, (1.0 - innerGlow) * 0.18);
    vec3 rimLight = mix(vec3(0.86, 0.95, 1.0), vec3(1.0), shimmer * 0.38);
    vec3 edgeGlow = rimLight * (0.14 + rim * 0.84);
    vec3 coldScatter = mix(ice, aqua, 0.35) * (0.05 + rim * 0.24);
    vec3 coreLift = milk * (0.02 + innerGlow * 0.10);
    vec3 reflectedSky = mix(vec3(0.56, 0.78, 1.0), vec3(0.96, 0.99, 1.0), envTop);
    vec3 reflectedHorizon = mix(vec3(0.28, 0.46, 0.72), vec3(0.68, 0.90, 1.0), envBack);
    vec3 envReflection = mix(reflectedHorizon, reflectedSky, envTop) * (0.07 + envSide * 0.26);
    vec3 streakReflection = mix(vec3(0.84, 0.95, 1.0), vec3(0.72, 0.98, 0.96), diagonalSweep * 0.5) * (verticalSweep * 0.07 + diagonalSweep * 0.05) * (0.22 + rim * 0.78);
    vec3 mirrorFlash = vec3(1.0) * pow(max(envTop, envBack), 3.2) * (0.04 + rim * 0.16);
    vec3 pearlFlash = pearl * (ridge * 0.12 + sparkle * 0.20);

    color.rgb = body
            + edgeGlow * color.a * 0.32
            + coldScatter * color.a * 0.22
            + coreLift * color.a * 0.10
            + envReflection
            + streakReflection
            + mirrorFlash
            + pearlFlash;
    color.a = min(1.0, color.a * 0.54 + rim * 0.17 + innerGlow * 0.03);

    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
