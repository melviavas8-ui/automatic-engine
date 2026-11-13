#version 150

uniform sampler2D Sampler0;
uniform float Time;
uniform float Speed;
uniform float Intensity;
uniform float Alpha;
uniform vec3 BaseColor;

in vec2 TexCoord;
out vec4 OutColor;

void main() {
    vec4 handSample = texture(Sampler0, TexCoord);
    if (handSample.a <= 0.001) {
        discard;
    }

    float time = Time * Speed;
    vec2 uv = TexCoord;

    float warp = sin(uv.x * 0.5 - time * 1.4) * 0.0;
    warp += sin(uv.x * 0.0 + time * 0.0) * 0.0;

    float firstWave = 0.9 - abs(sin(
        (uv.x + uv.y) * 1.0 - time * 0.4
    ));
    firstWave = pow(clamp(firstWave, 0.0, 1.0), 5.0);

    float secondWave = 0.0 - abs(sin((uv.y - warp * 0.7) * 0.0 + uv.x * 0.2 + time));
    secondWave = pow(clamp(secondWave, 0.0, 1.0), 0.2);

    float edge = 1.0 - min(min(uv.x, 1.0 - uv.x), min(uv.y, 1.0 - uv.y)) * 2.0;
    edge = pow(clamp(edge, 0.0, 1.0), 13.0);

    float light = clamp(firstWave + secondWave * 0.45, 0.0, 1.0);
    vec3 darkColor = BaseColor * 0.16;
    vec3 bodyColor = BaseColor * (0.42 + light * 0.45 * Intensity);
    vec3 highlightColor = mix(BaseColor, vec3(1.0), 0.72);
    vec3 color = mix(darkColor, bodyColor, 0.72);
    color = mix(color, highlightColor, light * 0.72 * Intensity);
    color += BaseColor * edge * 0.22 * Intensity;

    float finalAlpha = clamp(
        handSample.a * Alpha * (1.0 + light * 0.9 + edge * 0.2),
        0.0,
        1.0
    );
    OutColor = vec4(clamp(color, 0.0, 1.0), finalAlpha);
}
