#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
#ifdef PER_FACE_LIGHTING
in vec4 vertexPerFaceColorBack;
in vec4 vertexPerFaceColorFront;
#else
in vec4 vertexColor;
#endif
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;
in vec3 modelPos;
in vec3 viewNormal;
in vec3 viewPos;

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

    float n000 = hash31(cell);
    float n100 = hash31(cell + vec3(1.0, 0.0, 0.0));
    float n010 = hash31(cell + vec3(0.0, 1.0, 0.0));
    float n110 = hash31(cell + vec3(1.0, 1.0, 0.0));
    float n001 = hash31(cell + vec3(0.0, 0.0, 1.0));
    float n101 = hash31(cell + vec3(1.0, 0.0, 1.0));
    float n011 = hash31(cell + vec3(0.0, 1.0, 1.0));
    float n111 = hash31(cell + vec3(1.0));

    return mix(
        mix(mix(n000, n100, local.x), mix(n010, n110, local.x), local.y),
        mix(mix(n001, n101, local.x), mix(n011, n111, local.x), local.y),
        local.z
    );
}

float fbm(vec3 p) {
    float value = 0.0;
    float amplitude = 0.55;
    for (int i = 0; i < 4; i++) {
        value += noise3(p) * amplitude;
        p = p * 2.07 + vec3(2.2, -1.7, 0.9);
        amplitude *= 0.5;
    }
    return value;
}

void main() {
    vec4 textureColor = texture(Sampler0, texCoord0);
    if (textureColor.a <= 0.01) {
        discard;
    }

#ifdef PER_FACE_LIGHTING
    vec4 tint = (gl_FrontFacing ? vertexPerFaceColorFront : vertexPerFaceColorBack) * ColorModulator;
#else
    vec4 tint = vertexColor * ColorModulator;
#endif

    float remaining = clamp(tint.a, 0.0, 1.0);
    float dissolve = 1.0 - remaining;

    vec3 fieldPos = modelPos * 3.2 + vec3(texCoord0 * 5.0, 0.0);
    float particles = fbm(fieldPos);
    float verticalBreak = smoothstep(-1.15, 1.15, modelPos.y);
    float breakup = clamp(dissolve * 1.18 - verticalBreak * 0.18, 0.0, 0.98);

    if (particles < breakup) {
        discard;
    }

    float edge = 1.0 - smoothstep(breakup, breakup + 0.10, particles);
    float fresnel = pow(1.0 - clamp(dot(normalize(viewNormal), normalize(-viewPos)), 0.0, 1.0), 2.0);

    vec3 soulBase = max(textureColor.rgb, vec3(0.20, 0.52, 1.0));
    vec3 glow = vec3(0.55, 0.85, 1.0);
    vec3 color = mix(soulBase * 0.65, glow, edge * 0.85 + fresnel * 0.45);
    color += glow * edge * 0.65;

    float alpha = textureColor.a * clamp(remaining * 1.25 + edge * 0.35, 0.0, 1.0);
    fragColor = apply_fog(vec4(clamp(color, 0.0, 1.0), alpha), sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
