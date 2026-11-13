#version 410 core

layout(std140) uniform Uniforms {
    mat4 uProjection;
    mat4 uModelView;
};

layout(location = 0) in vec3 inPosition;
layout(location = 2) in vec2 inUV;

out vec2 TexCoord;
out vec3 SkyDirection;

void main() {
    TexCoord = inUV;
    SkyDirection = normalize(vec3(inUV * 2.0 - 1.0, 1.35));
    gl_Position = uProjection * vec4(inPosition, 1.0);
}
