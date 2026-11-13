#version 330

in vec3 Position;   // xy: NDC, z: первый отрезок
in vec4 Color;
in vec2 UV0;        // count, ox
in ivec2 UV1;       // oy, pad
in ivec2 UV2;       // base, range*16
in vec3 Normal;

flat out vec4 genA;
flat out vec4 genB;

void main() {
    gl_Position = vec4(Position.xy, 0.0, 1.0);
    genA = vec4(Position.z, UV0.x, UV0.y, float(UV1.x));
    genB = vec4(float(UV1.y), float(UV2.x), float(UV2.y) / 16.0, 1024.0);
}
