#version 150

in vec2 TexCoord;

uniform sampler2D Sampler0;
uniform vec2 location;
uniform vec2 rectSize;
uniform vec4 color1;
uniform vec4 color2;
uniform vec4 color3;
uniform vec4 color4;

out vec4 fragColor;

#define NOISE (0.5 / 255.0)

vec3 createGradient(vec2 coords) {
    vec3 color = mix(
            mix(color1.rgb, color2.rgb, coords.y),
            mix(color3.rgb, color4.rgb, coords.y),
            coords.x
    );

    color += mix(NOISE, -NOISE, fract(sin(dot(coords.xy, vec2(12.9898, 78.233))) * 43758.5453));
    return color;
}

void main() {
    vec2 coords = (gl_FragCoord.xy - location) / rectSize;
    float texAlpha = texture(Sampler0, TexCoord).a;
    fragColor = vec4(createGradient(coords), texAlpha);
}
