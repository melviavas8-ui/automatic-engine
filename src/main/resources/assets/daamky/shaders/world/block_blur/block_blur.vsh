#version 410 core

layout(std140) uniform Uniforms {
    mat4 uProjection;
    mat4 uModelView;
};

layout(location = 0) in vec3 inPosition;
layout(location = 2) in vec2 inUV;

out vec2 vUV;
out vec4 vClipPos;

void main() {
    vUV = inUV;
    vClipPos = uProjection * vec4(inPosition, 1.0);
    gl_Position = vClipPos;
}
