#version 150

// Black hole sky.
//
// Like the storm sky, every fragment is shaded from the world space *direction*
// of its view ray and never from the camera position, so the hole stays pinned
// to one direction in the world: walking or flying cannot shift it, turning the
// camera pans across it.
//
// The hole itself is a real (if cheap) light-bending integration rather than a
// painted disc. Rays start at a virtual camera a fixed number of Schwarzschild
// radii away and are marched with the standard photon deflection term
//
//     d2u/dphi2 = -1.5 * h^2 * p / |p|^5
//
// which is what produces the two signatures you cannot fake with a sprite: the
// far side of the accretion disk lensed up over the top of the hole and back
// under the bottom, and the thin photon ring hugging the shadow.

uniform sampler2D DepthSampler;

layout(std140) uniform BlackHoleData {
    mat4 InverseViewProjection;
    vec4 HoleDir;   // xyz = world direction to the hole, w = camera distance (Rs)
    vec4 DiskHot;   // rgb = inner disk color,  a = inner radius (Rs)
    vec4 DiskCold;  // rgb = outer disk color,  a = outer radius (Rs)
    vec4 Halo;      // rgb = photon ring color, a = halo strength
    vec4 Space;     // rgb = deep space color,  a = star brightness
    vec4 Motion;    // x = time, y = spin speed, z = turbulence, w = inclination
    vec4 Mix0;      // x = opacity, y = march steps, z = brightness, w = doppler
    vec4 Extra;     // x = sky depth threshold, y = disk softness, z = lensing, w = exposure
};

in vec2 texCoord;
out vec4 fragColor;

const float PI = 3.14159265359;

float hash21(vec2 point) {
    point = fract(point * vec2(123.34, 456.21));
    point += dot(point, point + 45.32);
    return fract(point.x * point.y);
}

float hash31(vec3 point) {
    point = fract(point * 0.1031);
    point += dot(point, point.zyx + 31.32);
    return fract((point.x + point.y) * point.z);
}

float valueNoise(vec2 point) {
    vec2 cell = floor(point);
    vec2 local = fract(point);
    local = local * local * (3.0 - 2.0 * local);

    float a = hash21(cell);
    float b = hash21(cell + vec2(1.0, 0.0));
    float c = hash21(cell + vec2(0.0, 1.0));
    float d = hash21(cell + vec2(1.0, 1.0));
    return mix(mix(a, b, local.x), mix(c, d, local.x), local.y);
}

float fbm(vec2 point) {
    float sum = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 4; i++) {
        sum += valueNoise(point) * amplitude;
        point = point * 2.11 + vec2(7.3, -3.1);
        amplitude *= 0.5;
    }
    return sum;
}

// Clip space is unprojected without ever adding the camera position, so the
// result is a pure world space direction.
vec3 viewRay(vec2 uv) {
    vec4 clip = vec4(uv * 2.0 - 1.0, 1.0, 1.0);
    vec4 unprojected = InverseViewProjection * clip;
    return normalize(unprojected.xyz / max(abs(unprojected.w), 0.00001));
}

float starField(vec3 direction) {
    float total = 0.0;
    for (int layer = 0; layer < 2; layer++) {
        // Tight cells keep stars down to a pixel or two instead of blobs.
        float scale = 420.0 + float(layer) * 520.0;
        vec3 cell = floor(direction * scale);
        float seed = hash31(cell + float(layer) * 19.7);
        if (seed > 0.9990) {
            vec3 center = normalize((cell + 0.5) / scale);
            float d = length(center - direction) * scale;
            total += exp(-d * d * 6.0) * (0.30 + 0.70 * hash31(cell + 3.7));
        }
    }
    return total;
}

vec3 deepSpace(vec3 direction) {
    // A gentle vertical ramp keeps the world horizon from meeting a flat wall
    // of colour, and matches how the glow of the disk washes the lower sky.
    float height = clamp(direction.y * 0.5 + 0.5, 0.0, 1.0);
    vec3 base = Space.rgb * mix(0.45, 1.0, height);
    return base + vec3(0.88, 0.92, 1.0) * starField(direction) * Space.a;
}

/**
 * Emission of the accretion disk at a crossing point.
 *
 * The material shears: inner orbits sweep round far faster than outer ones
 * (Keplerian, so the angular rate goes as r^-1.5), and that shear is what
 * stretches the turbulence into the fine spiral striations.
 */
vec3 diskEmission(vec3 hit, float radius, vec3 tangentA, vec3 tangentB, vec3 normal, vec3 rayDir) {
    float phi = atan(dot(hit, tangentB), dot(hit, tangentA));
    float orbit = Motion.x * Motion.y * 5.0 / pow(max(radius, 0.5), 1.5);
    float angle = phi + orbit;

    vec2 q = vec2(angle * 2.3 + radius * 0.40, radius * 1.10);
    float broad = fbm(q * (0.8 + Motion.z * 1.8));
    float fine = fbm(q * 5.5 + 13.0);

    float f = clamp((radius - DiskHot.a) / max(0.001, DiskCold.a - DiskHot.a), 0.0, 1.0);

    // Blazing at the inner edge, cooling outwards.
    float temperature = pow(1.0 - f, 2.4);
    vec3 color = mix(DiskCold.rgb, DiskHot.rgb, temperature);
    // Only the very innermost annulus burns out to white; letting this creep
    // outwards washes the whole disk to a flat tan.
    color = mix(color, vec3(1.0, 0.97, 0.90), pow(temperature, 3.0) * 0.85);

    float density = (0.50 + 0.85 * broad) * (0.70 + 0.50 * fine);
    // Soften both rims so the disk has no cut edge.
    float soft = clamp(Extra.y, 0.02, 0.5);
    density *= smoothstep(0.0, soft, f) * smoothstep(1.0, 1.0 - soft * 1.6, f);

    // Relativistic beaming: the limb rotating towards the camera is brighter.
    vec3 velocity = normalize(cross(normal, hit));
    float beaming = max(1.0 + Mix0.w * dot(velocity, -rayDir), 0.05);

    return color * density * pow(beaming, 2.2) * (0.55 + 2.6 * temperature) * Mix0.z;
}

void main() {
    float rawDepth = texture(DepthSampler, texCoord).r;
    if (rawDepth < Extra.x) {
        // Solid geometry covers this pixel; the sky is not visible here.
        discard;
    }

    vec3 rayDir = viewRay(texCoord);

    vec3 forward = normalize(HoleDir.xyz);
    // The virtual camera sits a fixed number of radii back along that
    // direction. Only the direction of the real camera matters, never where
    // the player is standing.
    vec3 origin = -forward * HoleDir.w;

    // Disk frame: the normal starts perpendicular to the view axis, which puts
    // the disk exactly edge on, then the inclination setting tips it towards
    // or away from the camera.
    vec3 sideAxis = cross(vec3(0.0, 1.0, 0.0), forward);
    if (dot(sideAxis, sideAxis) < 1.0e-6) {
        sideAxis = vec3(1.0, 0.0, 0.0);
    }
    sideAxis = normalize(sideAxis);
    vec3 upAxis = normalize(cross(forward, sideAxis));

    float inclination = Motion.w;
    vec3 normal = normalize(upAxis * cos(inclination) + forward * sin(inclination));
    vec3 tangentA = normalize(sideAxis - normal * dot(sideAxis, normal));
    vec3 tangentB = cross(normal, tangentA);

    vec3 position = origin;
    vec3 direction = rayDir;

    // Conserved specific angular momentum of the photon; the lensing setting
    // just scales how hard the deflection bites.
    vec3 angularMomentum = cross(position, direction);
    float h2 = dot(angularMomentum, angularMomentum) * max(Extra.z, 0.0);

    float escapeRadius = max(DiskCold.a * 2.2, HoleDir.w * 1.6);
    int steps = int(clamp(Mix0.y, 24.0, 220.0));

    vec3 emission = vec3(0.0);
    float transmittance = 1.0;
    bool captured = false;

    for (int i = 0; i < 220; i++) {
        if (i >= steps) {
            break;
        }

        float radius = length(position);
        if (radius < 1.0) {
            captured = true;
            break;
        }
        if (radius > escapeRadius && dot(position, direction) > 0.0) {
            break;
        }

        // Fine steps close in where the bending is violent, coarse far out.
        float stepSize = clamp(radius * 0.15, 0.05, 2.6);

        vec3 previous = position;
        position += direction * stepSize;
        direction += (-1.5 * h2 * position / pow(dot(position, position), 2.5)) * stepSize;

        if (transmittance < 0.02) {
            continue;
        }

        // The disk is treated as a surface rather than a volume. Marching it
        // as an emissive slab was tried and is worse on both counts: rays that
        // skim along the plane pick up a huge path length and wash the shadow
        // out, and the step size aliases into visible concentric banding.
        // Catching the plane crossing keeps the shadow a clean circle and the
        // lensed arcs crisp.
        float sideBefore = dot(previous, normal);
        float sideAfter = dot(position, normal);
        if (sideBefore * sideAfter < 0.0) {
            float k = sideBefore / (sideBefore - sideAfter);
            vec3 hit = mix(previous, position, k);
            float hitRadius = length(hit);
            if (hitRadius > DiskHot.a && hitRadius < DiskCold.a) {
                emission += diskEmission(hit, hitRadius, tangentA, tangentB, normal, normalize(position - previous)) * transmittance;
                // Partly opaque, so the far side shows through the near side
                // instead of replacing it. That layering is what gives the
                // lensed arc its sense of depth.
                transmittance *= 0.55;
            }
        }
    }

    vec3 color = captured ? vec3(0.0) : deepSpace(normalize(direction));
    // Emission picked up before the ray fell in still counts: a ray can cross
    // the near side of the disk and only then cross the horizon.
    color += emission;

    // Nothing may be added on top of the shadow. The whole point of the effect
    // is that the hole is a hole, so the analytic ring and halo below are only
    // laid over rays that actually escaped.
    if (!captured) {
        // The photon ring is razor thin and easy for the march to step
        // straight over, so it is added analytically from the impact parameter
        // of the original ray. 2.6 Rs is where the unstable photon orbit sits.
        float facing = smoothstep(0.0, 0.25, dot(rayDir, forward));
        float impact = length(cross(origin, rayDir));
        float ring = exp(-pow((impact - 2.6) * 2.4, 2.0));
        color += Halo.rgb * ring * Halo.a * 1.35 * facing;

        // Broad halo bleeding off the hole into the surrounding sky.
        float angle = acos(clamp(dot(rayDir, forward), -1.0, 1.0));
        color += Halo.rgb * exp(-angle * 5.5) * Halo.a * 0.30;
    }

    // Exposure tonemap: lets the inner disk blow out to white without every
    // bright pixel clipping to the same flat value.
    color = vec3(1.0) - exp(-color * max(Extra.w, 0.05));

    fragColor = vec4(color, clamp(Mix0.x, 0.0, 1.0));
}

