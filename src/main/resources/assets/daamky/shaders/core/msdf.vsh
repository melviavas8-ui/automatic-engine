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
flat out vec2 params; // x: range px, y: weight

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position.xy, 0.0, 1.0);
    texCoord0 = UV0;
    vertexColor = Color;
    params = vec2(float(UV1.x) / 16.0, float(UV2.x) / 10000.0);
}
