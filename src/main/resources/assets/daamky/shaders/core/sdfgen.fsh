#version 330

// GPU-генерация SDF: на каждый тексель ячейки глифа считаем расстояние до всех
// отрезков контура + знак по числу обмоток (nonzero winding).
uniform sampler2D Sampler0; // контуры: 2 текселя на отрезок, 16-бит координаты в RGBA8
flat in vec4 genA; // x: start, y: count, z: ox, w: oy
flat in vec4 genB; // x: pad, y: base, z: range, w: tw

out vec4 fragColor;

vec2 dec(vec4 t) {
    vec4 b = floor(t * 255.0 + 0.5);
    return vec2(b.r * 256.0 + b.g, b.b * 256.0 + b.a) / 256.0 - 128.0;
}

void main() {
    vec2 p = gl_FragCoord.xy - genA.zw - vec2(genB.x, genB.y);
    int start = int(genA.x + 0.5);
    int count = int(genA.y + 0.5);
    int tw = int(genB.w + 0.5);

    float dmin = 1e9;
    int wn = 0;
    for (int i = 0; i < count; i++) {
        int t = 2 * (start + i);
        vec2 a = dec(texelFetch(Sampler0, ivec2(t % tw, t / tw), 0));
        vec2 b = dec(texelFetch(Sampler0, ivec2((t + 1) % tw, (t + 1) / tw), 0));
        vec2 e = b - a;
        float l2 = dot(e, e);
        float h = l2 > 0.0 ? clamp(dot(p - a, e) / l2, 0.0, 1.0) : 0.0;
        dmin = min(dmin, length(p - a - e * h));

        float left = e.x * (p.y - a.y) - (p.x - a.x) * e.y;
        if (a.y <= p.y) { if (b.y > p.y && left > 0.0) wn++; }
        else            { if (b.y <= p.y && left < 0.0) wn--; }
    }
    float s = wn != 0 ? 1.0 : -1.0;
    float v = clamp(0.5 + s * dmin / genB.z, 0.0, 1.0);
    fragColor = vec4(v, v, v, 1.0);
}
