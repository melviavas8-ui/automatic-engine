#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <daamky:shader_hand_material.glsl>

uniform sampler2D Sampler0;
layout(std140) uniform ShaderHandData { vec4 shaderHandParams; };

in vec4 vertexColor;
in vec2 texCoord0;
out vec4 fragColor;

void main() {
    vec4 base = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    if (base.a < 0.10) discard;
    vec2 objectUv = shaderHandObjectUv(texCoord0, vec2(textureSize(Sampler0, 0)));
    float time = shaderHandParams.y * shaderHandParams.w;
    fragColor = vec4(applyShaderHandMaterial(base.rgb, objectUv, shaderHandParams.x, time, shaderHandParams.z), base.a);
}

