#version 330

// Glow pass for the target ESP lightning.
//
// Bloom cannot be faked inside the core shader: widening its falloff only
// widens the white filament along with it, and the light has to spread past
// the geometry it comes from. So the halo gets its own geometry, drawn before
// the stroke.
//
// That geometry is a chain of camera facing sprites strung along the bolt, not
// a wide ribbon. A ribbon was tried first and breaks down: at four or five
// times the stroke width, neighbouring quads splay apart around every sharp
// bend and the halo turns into a visible run of flat polygons. Overlapping
// round sprites have no joints to come apart, and under additive blending they
// merge into one smooth glow.
//
// Hence a radial falloff here rather than a falloff across the ribbon.

#moj_import <minecraft:dynamictransforms.glsl>

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    float dist = length(texCoord0 * 2.0 - 1.0);
    if (dist > 1.0) {
        discard;
    }

    // Squaring a gaussian keeps the centre bright while pulling the skirt in,
    // which reads as light bleeding off the bolt rather than a soft blob.
    float falloff = exp(-dist * dist * 3.4);
    falloff *= falloff;
    // Hard zero at the sprite edge, so a low alpha cannot leave a visible
    // square where the quad ends.
    falloff *= smoothstep(1.0, 0.72, dist);

    float alpha = falloff * vertexColor.a;
    if (alpha < 0.003) {
        discard;
    }

    fragColor = vec4(vertexColor.rgb, min(alpha, 1.0)) * ColorModulator;
}

