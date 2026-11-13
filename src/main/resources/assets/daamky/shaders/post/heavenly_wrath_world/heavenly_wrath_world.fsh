#version 150

uniform sampler2D SceneSampler;
uniform sampler2D DepthSampler;

layout(std140) uniform HeavenlyWorldData {
    mat4 InverseViewProjection;
    vec4 CameraTime;
    vec4 Params;
    vec4 SkyTint;
};

in vec2 texCoord;
out vec4 fragColor;

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise2(vec2 p) {
    vec2 cell = floor(p);
    vec2 local = fract(p);
    local = local * local * (3.0 - 2.0 * local);
    float a = hash21(cell);
    float b = hash21(cell + vec2(1.0, 0.0));
    float c = hash21(cell + vec2(0.0, 1.0));
    float d = hash21(cell + vec2(1.0, 1.0));
    return mix(mix(a, b, local.x), mix(c, d, local.x), local.y);
}

float cloudNoise(vec2 p) {
    float value = 0.0;
    float amplitude = 0.55;
    for (int i = 0; i < 4; i++) {
        value += noise2(p) * amplitude;
        p = p * 2.03 + vec2(5.7, -3.4);
        amplitude *= 0.48;
    }
    return value;
}

vec3 reconstructWorldPosition(float depth) {
    vec4 clip = vec4(texCoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 relativePosition = InverseViewProjection * clip;
    relativePosition /= max(abs(relativePosition.w), 0.00001);
    return relativePosition.xyz + CameraTime.xyz;
}

void main() {
    vec3 scene = texture(SceneSampler, texCoord).rgb;
    float depth = texture(DepthSampler, texCoord).r;
    if (depth >= Params.w) {
        discard;
    }

    vec3 worldPosition = reconstructWorldPosition(depth);
    vec3 cameraRelative = worldPosition - CameraTime.xyz;
    float distanceFromCamera = length(cameraRelative);

    vec3 dx = dFdx(worldPosition);
    vec3 dy = dFdy(worldPosition);
    vec3 normal = normalize(cross(dx, dy));
    if (normal.y < 0.0) {
        normal = -normal;
    }
    float upwardSurface = smoothstep(0.14, 0.82, normal.y);

    float time = CameraTime.w * Params.x;
    vec2 wind = vec2(time * 0.78, -time * 0.46);
    vec2 shadowPosition = worldPosition.xz * 0.0125 + wind * 0.022;
    float broadCloud = cloudNoise(shadowPosition);
    float detailCloud = cloudNoise(shadowPosition * 2.17 + vec2(13.1, -7.4));
    float cloud = smoothstep(0.48, 0.78, broadCloud * 0.78 + detailCloud * 0.32);
    float cloudEdge = smoothstep(0.42, 0.58, broadCloud) - smoothstep(0.67, 0.82, broadCloud);
    float movingShadow = cloud * upwardSurface * Params.z;

    vec3 crimson = mix(max(SkyTint.rgb, vec3(0.34, 0.015, 0.10)), vec3(0.94, 0.035, 0.28), 0.42);
    vec3 shadowColor = vec3(0.105, 0.012, 0.075);
    vec3 litColor = mix(crimson, vec3(1.0, 0.30, 0.43), 0.22);

    float luminance = dot(scene, vec3(0.2126, 0.7152, 0.0722));
    float gradeAmount = 0.10 + Params.y * 0.045;
    vec3 graded = mix(scene, scene * (0.72 + litColor * 0.48), gradeAmount);

    float lightFacing = max(dot(normal, normalize(vec3(0.0, 0.38, 1.0))), 0.0);
    graded += litColor * lightFacing * upwardSurface * (0.022 + Params.y * 0.012);
    graded = mix(graded, shadowColor * (0.42 + luminance * 0.70), movingShadow * 0.44);

    // Silver-pink edges travel with the cloud front rather than uniformly
    // brightening the terrain.
    graded += litColor * cloudEdge * upwardSurface * (0.020 + Params.y * 0.010);

    // Low, distance-weighted rose mist ties terrain silhouettes into the sky.
    float distanceHaze = smoothstep(24.0, 176.0, distanceFromCamera);
    float grazingView = pow(1.0 - abs(normalize(cameraRelative).y), 2.2);
    float mistNoise = noise2(worldPosition.xz * 0.035 - wind * 0.018);
    float roseMist = distanceHaze * grazingView * (0.28 + mistNoise * 0.34);
    graded = mix(graded, crimson * (0.34 + luminance * 0.32), roseMist * 0.16);

    // A restrained pulse from the apparition briefly washes exposed terrain.
    float pulse = pow(max(sin(time * 0.34), 0.0), 22.0);
    graded += litColor * pulse * upwardSurface * (0.018 + Params.y * 0.014);

    fragColor = vec4(max(graded, vec3(0.0)), 1.0);
}
