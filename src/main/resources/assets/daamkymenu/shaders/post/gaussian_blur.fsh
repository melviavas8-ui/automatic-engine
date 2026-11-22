#version 150

uniform sampler2D InSampler;

layout(std140) uniform BlurConfig {
    vec2 BlurDir;
    float Radius;
    float Spread;
};

in vec2 texCoord;
in vec2 sampleStep;
out vec4 fragColor;

void main() {
    float r = clamp(Radius, 1.0, 64.0);
    float sigma = r * 0.5;
    float twoSigmaSq = 2.0 * sigma * sigma;
    vec4 sum = texture(InSampler, texCoord);
    float wSum = 1.0;
    for (float i = 1.0; i <= r; i += 2.0) {
        float j = i + 1.0;
        float w1 = exp(-(i * i) / twoSigmaSq);
        float w2 = (j <= r) ? exp(-(j * j) / twoSigmaSq) : 0.0;
        float w = w1 + w2;
        float offset = (i * w1 + j * w2) / max(w, 1e-6);
        vec2 d = sampleStep * offset;
        sum += (texture(InSampler, texCoord + d) + texture(InSampler, texCoord - d)) * w;
        wSum += 2.0 * w;
    }
    fragColor = sum / wSum;
}
