#version 410 core

layout(std140) uniform Uniforms {
    mat4 uProjection;
    mat4 uUnused;
};

layout(location = 0) in vec3 inPosition;
layout(location = 2) in vec2 inUV;

out vec2 localUv;

void main() {
    localUv = inUV;
    gl_Position = uProjection * vec4(inPosition, 1.0);
}
