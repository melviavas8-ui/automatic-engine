#version 330
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

out vec2 texCoord0;
out vec4 vertexColor;
flat out vec4 rectP;   // hw, hh, r, mode
flat out vec4 extraP;  // extra, cx, cy, time

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position.xy, 0.0, 1.0);
    texCoord0 = UV0;
    vertexColor = Color;
    rectP = vec4(vec2(UV1) / 32.0, float(UV2.x) / 64.0, floor(Normal.x * 127.0 + 0.5));
    extraP = vec4(float(UV2.y) / 64.0, Position.xy - UV0, Position.z);
}
