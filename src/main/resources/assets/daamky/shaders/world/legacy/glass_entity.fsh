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

    float time = tint.r * 120.0;
    vec3 tintColor = max(vec3(ColorModulator.g * 0.85, ColorModulator.b * 0.65 + 0.20, 0.90), vec3(0.08, 0.20, 0.35));
    vec3 flowPos = modelPos * 2.4 + vec3(texCoord0 * 4.0, 0.0);
    flowPos.y -= time * 0.075;
    flowPos.x += sin(time * 0.055 + modelPos.y * 2.0) * 0.28;
    flowPos.z += cos(time * 0.045 + modelPos.x * 2.3) * 0.28;
    float shell = noise3(flowPos);
    float wave = modelPos.y * 8.0 + shell * 6.0 + texCoord0.x * 12.0 - time * 0.42;
    float veins = pow(clamp(1.0 - abs(sin(wave)), 0.0, 1.0), 4.0);
    float secondary = pow(clamp(1.0 - abs(sin(modelPos.y * 13.0 - texCoord0.y * 18.0 + shell * 5.0 + time * 0.32)), 0.0, 1.0), 5.0);
    float fresnel = pow(1.0 - clamp(dot(normalize(viewNormal), normalize(-viewPos)), 0.0, 1.0), 2.1);

    vec3 skinGhost = textureColor.rgb * 0.18;
    vec3 glass = mix(tintColor * 0.45, vec3(0.72, 0.92, 1.0), fresnel * 0.75 + veins * 0.35 + secondary * 0.22);
    vec3 color = skinGhost + glass + tintColor * veins * 0.45 + vec3(0.58, 0.72, 1.0) * secondary * 0.28;
    float alpha = textureColor.a * clamp(tint.a * 0.95 + fresnel * 0.28 + veins * 0.18 + secondary * 0.10, 0.0, 0.86);

    fragColor = apply_fog(vec4(clamp(color, 0.0, 1.0), alpha), sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
