#version 150

in vec2 TexCoord;

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec2 texelSize;
uniform vec2 direction;
uniform float radius;
uniform float kernel[64];

out vec4 fragColor;

void main() {
    vec2 uv = TexCoord;

    if (direction.x == 0.0 && texture(Sampler1, uv).a > 0.0) {
        discard;
    }

    vec4 pixelColor = texture(Sampler0, uv);
    pixelColor.rgb *= pixelColor.a;
    pixelColor *= kernel[0];

    int intRadius = int(floor(radius + 0.5));
    for (int i = 1; i < 64; i++) {
        if (i > intRadius) {
            break;
        }

        vec2 offset = float(i) * texelSize * direction;
        vec4 left = texture(Sampler0, uv - offset);
        vec4 right = texture(Sampler0, uv + offset);
        left.rgb *= left.a;
        right.rgb *= right.a;
        pixelColor += (left + right) * kernel[i];
    }

    if (pixelColor.a <= 0.0) {
        discard;
    }

    fragColor = vec4(pixelColor.rgb / pixelColor.a, pixelColor.a);
}
