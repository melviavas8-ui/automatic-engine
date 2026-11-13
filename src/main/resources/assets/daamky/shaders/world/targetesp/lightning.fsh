#version 330

// Target ESP lightning.
//
// The geometry is a camera facing ribbon laid along each bolt path. All the
// shape lives here: UV.x runs from 0 at the head of the stroke to 1 at the
// tail, UV.y runs across the ribbon. Everything is derived from the distance
// to the ribbon centre line, so the stroke gets a hard white filament, a
// coloured discharge sheath around it and a wide soft halo, all without a
// texture. Per vertex alpha carries the CPU side flicker and the travelling
// energy pulse.

#moj_import <minecraft:dynamictransforms.glsl>

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    float along = texCoord0.x;
    float across = texCoord0.y * 2.0 - 1.0;
    float dist = abs(across);

    // Three nested falloffs: filament, sheath, halo.
    float filament = exp(-dist * dist * 30.0);
    float sheath = exp(-dist * dist * 5.5) * 0.50;
    float halo = exp(-dist * dist * 0.85) * 0.20;

    // Strokes fade in and out along their length instead of ending on a hard
    // rectangular cap.
    float ends = smoothstep(0.0, 0.12, along) * smoothstep(1.0, 0.88, along);

    float energy = (filament + sheath + halo) * ends;
    float alpha = energy * vertexColor.a;
    if (alpha < 0.004) {
        discard;
    }

    // The core burns out to white while the outer discharge keeps the tint.
    vec3 tint = vertexColor.rgb;
    vec3 color = mix(tint, vec3(1.0), clamp(filament * 1.4, 0.0, 1.0));
    color += tint * halo * 1.8;

    fragColor = vec4(color, min(alpha, 1.0)) * ColorModulator;
}

