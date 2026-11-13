#version 330
#moj_import <minecraft:dynamictransforms.glsl>

const vec4 GradA = vec4(0.88, 0.78, 1.0, 1.0);
const vec4 GradB = vec4(0.66, 0.47, 0.95, 1.0);
const vec4 GradC = vec4(0.46, 0.27, 0.84, 1.0);

in vec2 texCoord0; // локальные координаты от центра (px)
in vec4 vertexColor;
flat in vec4 rectP;
flat in vec4 extraP;
out vec4 fragColor;

float sdRoundBox(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

// Круговой градиент: направление мягкой линейной рампы плавно вращается по кругу (полный оборот ~11 c).
// Нет сингулярности в центре (как у конического), нет швов - переход всегда широкий и гладкий.
vec3 flow(vec2 local, vec2 half_, vec2 center, float t, vec3 ca, vec3 cb, vec3 cc) {
    vec2 n = local / max(half_, vec2(1.0));                       // -1..1 по обеим осям, форма не важна
    float th = t * 0.57 + center.x * 0.0012 + center.y * 0.0009; // угол вращения
    vec2 dir = vec2(cos(th), sin(th));
    float u = dot(n, dir) * 0.36 + 0.5;                           // 0..1 вдоль рампы
    u += 0.05 * sin(6.2831853 * (dot(n, vec2(-dir.y, dir.x)) * 0.25 + t * 0.09)); // лёгкая «живость» поперёк
    u = clamp(u, 0.0, 1.0);
    u = u * u * (3.0 - 2.0 * u);
    vec3 c = mix(cc, cb, smoothstep(0.0, 0.55, u));
    c = mix(c, ca, smoothstep(0.45, 1.0, u));
    return c;
}

void main() {
    vec4 Rect = rectP;
    vec4 Extra = extraP;
    float d = sdRoundBox(texCoord0, Rect.xy, Rect.z);
    float aa = max(fwidth(d), 1e-4);
    float a;
    float mode = Rect.w;
    if (mode < 0.5 || (mode > 2.5 && mode < 3.5)) {
        a = clamp(0.5 - d / aa, 0.0, 1.0);
    } else if (mode < 1.5) {
        float t = Extra.x;
        a = clamp(0.5 - (abs(d + t * 0.5) - t * 0.5) / aa, 0.0, 1.0);
    } else if (mode > 4.5) {
        a = clamp(0.5 + d / aa, 0.0, 1.0); // вырез: закрашиваем только углы/всё снаружи формы
    } else {
        float k = clamp(1.0 - max(d, 0.0) / Extra.x, 0.0, 1.0);
        a = k * k;
    }
    vec3 rgb = vertexColor.rgb;
    if (mode > 2.5 && mode < 4.5) {
        // белый цвет вершины = тема по умолчанию; иначе цвет вершины - основной цвет темы (светлый/тёмный выводятся из него)
        vec3 b = vertexColor.rgb;
        bool custom = !(b.r > 0.999 && b.g > 0.999 && b.b > 0.999);
        rgb = flow(texCoord0, Rect.xy, Extra.yz, Extra.w,
                custom ? mix(b, vec3(1.0), 0.55) : GradA.rgb,
                custom ? b : GradB.rgb,
                custom ? b * vec3(0.70, 0.57, 0.88) : GradC.rgb);
    }
    vec4 color = vec4(rgb, vertexColor.a * a) * ColorModulator;
    if (color.a < 0.003) discard;
    fragColor = color;
}
