#version 330

#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0);
    if (color.a == 0.0) {
        discard;
    }

    // Vertex alpha is supplied by EntityBloom as a distance factor. Vanilla
    // glowing entities still use opaque alpha and retain their normal shape.
    fragColor = vec4(ColorModulator.rgb * vertexColor.rgb, vertexColor.a);
}

