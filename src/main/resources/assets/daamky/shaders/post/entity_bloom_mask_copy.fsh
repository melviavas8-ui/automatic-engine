// FIXED 2026-11-17: kept simple mask copy
#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    fragColor = texture(InSampler, texCoord);
}

