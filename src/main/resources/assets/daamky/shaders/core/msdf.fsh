#version 330
#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;
flat in vec2 params;
out vec4 fragColor;

float median(vec3 v) {
    return max(min(v.r, v.g), min(max(v.r, v.g), v.b));
}

void main() {
    vec3 s = texture(Sampler0, texCoord0).rgb;
    float d = median(s) + params.y; // z = толщина (сдвиг порога)

    vec2 unitRange = vec2(params.x) / vec2(textureSize(Sampler0, 0));
    vec2 screenTexSize = vec2(1.0) / max(fwidth(texCoord0), vec2(1e-6));
    float screenPxRange = max(0.5 * dot(unitRange, screenTexSize), 1.0);

    float alpha = clamp((d - 0.5) * screenPxRange + 0.5, 0.0, 1.0);
    vec4 color = vec4(vertexColor.rgb, vertexColor.a * alpha) * ColorModulator;
    if (color.a < 0.004) discard;
    fragColor = color;
}
