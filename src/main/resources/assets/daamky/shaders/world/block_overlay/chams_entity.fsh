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

float hash31(vec3 p) { p = fract(p * 0.1031); p += dot(p, p.yzx + 33.33); return fract((p.x + p.y) * p.z); }
float noise3(vec3 p) {
    vec3 cell = floor(p); vec3 local = fract(p); local = local * local * (3.0 - 2.0 * local);
    float n000 = hash31(cell); float n100 = hash31(cell + vec3(1.0, 0.0, 0.0));
    float n010 = hash31(cell + vec3(0.0, 1.0, 0.0)); float n110 = hash31(cell + vec3(1.0, 1.0, 0.0));
    float n001 = hash31(cell + vec3(0.0, 0.0, 1.0)); float n101 = hash31(cell + vec3(1.0, 0.0, 1.0));
    float n011 = hash31(cell + vec3(0.0, 1.0, 1.0)); float n111 = hash31(cell + vec3(1.0));
    return mix(mix(mix(n000, n100, local.x), mix(n010, n110, local.x), local.y), mix(mix(n001, n101, local.x), mix(n011, n111, local.x), local.y), local.z);
}
float fbm(vec3 p) {
    float value = 0.0, amplitude = 0.55;
    for (int i = 0; i < 5; i++) { value += noise3(p) * amplitude; p = p * 2.03 + vec3(1.7, -2.1, 0.8); amplitude *= 0.5; }
    return value;
}

void main() {
    float textureAlpha = texture(Sampler0, texCoord0).a;
#ifdef ALPHA_CUTOUT
    if (textureAlpha < ALPHA_CUTOUT) discard;
#endif
#ifdef PER_FACE_LIGHTING
    vec4 tint = (gl_FrontFacing ? vertexPerFaceColorFront : vertexPerFaceColorBack) * ColorModulator;
#else
    vec4 tint = vertexColor * ColorModulator;
#endif
    float tintRedByte = floor(clamp(tint.r, 0.0, 1.0) * 255.0 + 0.5);
    float tintGreenByte = floor(clamp(tint.g, 0.0, 1.0) * 255.0 + 0.5);
    float encodedTime = mod(tintRedByte, 16.0) + mod(tintGreenByte, 16.0) * 16.0;
    float time = encodedTime * 0.34;
    vec3 decodedTint = vec3(
        floor(tintRedByte / 16.0) * 16.0 / 255.0,
        floor(tintGreenByte / 16.0) * 16.0 / 255.0,
        tint.b
    );
    vec3 baseColor = max(decodedTint, vec3(0.50, 0.17, 0.45));
    vec3 flow = modelPos * 1.65;
    flow += vec3(
        sin(time * 0.21 + modelPos.y * 1.7) * 0.36,
        -time * 0.18,
        cos(time * 0.17 + modelPos.x * 1.9) * 0.36
    );
    float broad = fbm(flow * 1.15);
    vec3 warped = flow + vec3(broad * 1.4, fbm(flow * 0.85 + 7.2 + time * 0.09) * 0.8, fbm(flow * 0.95 - 4.6 - time * 0.07) * 1.2);
    float detail = fbm(warped * 1.75);
    float ribbon = pow(clamp(1.0 - abs(sin(warped.y * 4.2 + warped.x * 1.35 - warped.z * 1.1 + detail * 6.0 - time * 0.62)), 0.0, 1.0), 3.4);
    float secondary = pow(clamp(1.0 - abs(sin(warped.y * 6.5 - warped.x * 1.8 + broad * 7.5 + time * 0.44)), 0.0, 1.0), 5.0);
    float cloud = smoothstep(0.28, 0.82, detail);
    vec3 color = mix(baseColor * 0.12, baseColor * (0.42 + cloud * 0.48), cloud);
    color *= 0.68 + (1.0 - smoothstep(0.18, 0.68, broad)) * 0.48;
    color = mix(color, mix(baseColor, vec3(1.0), 0.58), clamp(ribbon * 0.88 + secondary * 0.42, 0.0, 1.0) * 0.72);
    float facing = clamp(dot(normalize(viewNormal), normalize(-viewPos)), 0.0, 1.0);
    color += mix(baseColor, vec3(1.0), 0.68) * pow(1.0 - facing, 2.35) * 0.46;
    float alpha = max(tint.a, 0.53) * textureAlpha;
    fragColor = apply_fog(vec4(clamp(color, 0.0, 1.0), alpha), sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
