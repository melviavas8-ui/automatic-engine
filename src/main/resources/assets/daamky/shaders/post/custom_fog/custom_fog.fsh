#version 150

uniform sampler2D SceneSampler;
uniform sampler2D DepthSampler;

layout(std140) uniform CustomFogData {
    vec4 FogColor;      // RGB цвет тумана + интенсивность в alpha
    vec4 FogParams;     // x: near, y: far, z: density, w: time
    vec4 WaveParams;    // x: wave speed, y: wave scale, z: wave strength, w: glow intensity
    vec4 WeatherParams; // x: weather type (0=sunny, 1=rain, 2=thunder, 3=snow), y: rain intensity, z: thunder flash, w: unused
};

in vec2 texCoord;
out vec4 fragColor;


float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    float frequency = 1.0;
    
    for (int i = 0; i < 4; i++) {
        value += amplitude * noise(p * frequency);
        frequency *= 2.0;
        amplitude *= 0.5;
    }
    
    return value;
}


float linearizeDepth(float depth, float nearPlane, float farPlane) {
    return (2.0 * nearPlane * farPlane) / (farPlane + nearPlane - depth * (farPlane - nearPlane));
}


float waves(vec2 uv, float time) {
    float wave1 = fbm(uv * WaveParams.y + time * WaveParams.x);
    float wave2 = fbm(uv * WaveParams.y * 1.5 - time * WaveParams.x * 0.7);
    float wave3 = noise(uv * WaveParams.y * 0.5 + time * WaveParams.x * 0.3);
    return (wave1 + wave2 + wave3) / 3.0;
}

float rainEffect(vec2 uv, float time) {
    vec2 rainUv = uv * vec2(30.0, 50.0);
    float rain = 0.0;
    
    for (int i = 0; i < 5; i++) {
        float offset = float(i) * 0.2;
        vec2 pos = rainUv + vec2(sin(time * 2.0 + offset) * 0.3, time * 8.0 + offset);
        float drop = noise(pos);
        
        float streak = smoothstep(0.4, 0.6, drop) * smoothstep(0.6, 0.4, fract(pos.y * 0.5));
        rain += streak * 0.2;
    }
    
    vec2 dropUv = uv * 100.0 + time * 5.0;
    float drops = smoothstep(0.95, 1.0, noise(dropUv)) * 0.3;
    
    return rain + drops;
}

vec3 thunderFlash(vec3 color, float intensity, vec2 uv, float time) {
    if (intensity < 0.01) return color;
    
    float flash = intensity;
    
    flash *= 0.7 + 0.3 * sin(time * 50.0);
    
    vec2 lightningUv = uv * vec2(10.0, 20.0);
    float lightning = 0.0;
    
    float mainBolt = abs(lightningUv.x - 5.0 + sin(lightningUv.y * 2.0 + time * 10.0) * 0.5);
    lightning += smoothstep(0.3, 0.0, mainBolt) * flash;
    
    for (int i = 0; i < 3; i++) {
        float offset = float(i) * 2.0;
        float sideBolt = abs(lightningUv.x - 5.0 + sin(lightningUv.y * 3.0 + offset) * 1.0);
        lightning += smoothstep(0.5, 0.0, sideBolt) * flash * 0.3;
    }
    
    vec3 lightningColor = vec3(0.8, 0.9, 1.0) * 2.0;
    
    vec3 ambientFlash = vec3(0.7, 0.8, 1.0) * flash * 0.5;
    
    return mix(color, lightningColor, lightning) + ambientFlash;
}

float snowEffect(vec2 uv, float time) {
    float snow = 0.0;
    
    vec2 snowUv1 = uv * 20.0;
    for (int i = 0; i < 3; i++) {
        float offset = float(i) * 0.33;
        vec2 pos = snowUv1 + vec2(sin(time * 0.5 + offset) * 2.0, time * 1.5 + offset);
        float flake = noise(pos);
        snow += smoothstep(0.8, 0.95, flake) * 0.3;
    }
    
    vec2 snowUv2 = uv * 40.0;
    for (int i = 0; i < 3; i++) {
        float offset = float(i) * 0.33 + 0.5;
        vec2 pos = snowUv2 + vec2(sin(time * 0.7 + offset) * 1.5, time * 2.0 + offset);
        float flake = noise(pos);
        snow += smoothstep(0.85, 0.98, flake) * 0.2;
    }
    
    vec2 snowUv3 = uv * 80.0;
    vec2 pos3 = snowUv3 + vec2(sin(time + uv.y * 5.0) * 3.0, time * 3.0);
    float blizzard = smoothstep(0.9, 1.0, noise(pos3)) * 0.15;
    snow += blizzard;
    
    return snow;
}

float fogSwirls(vec2 uv, float time) {
    vec2 swirlUv = uv * 3.0;
    float swirl1 = fbm(swirlUv + time * 0.1);
    float swirl2 = fbm(swirlUv * 1.5 - time * 0.15);
    return (swirl1 + swirl2) * 0.5;
}


void main() {
    vec3 scene = texture(SceneSampler, texCoord).rgb;
    float depth = texture(DepthSampler, texCoord).r;
    
    float linearDistance = linearizeDepth(depth, FogParams.x, FogParams.y) / FogParams.y;
    
    float fogMask = 1.0 - exp(-linearDistance * FogParams.z);
    fogMask = clamp(fogMask, 0.0, 1.0);
    
    float time = FogParams.w;
    int weatherType = int(WeatherParams.x);
    vec3 finalColor = scene;
    
    
    if (weatherType == 0) {
        float waveEffect = waves(texCoord, time);
        float swirls = fogSwirls(texCoord, time);
        
        fogMask = fogMask * (0.75 + waveEffect * WaveParams.z * 0.3 + swirls * 0.2);
        
        float glowEdge = smoothstep(0.2, 0.6, fogMask) * (1.0 - smoothstep(0.6, 0.95, fogMask));
        vec3 glow = FogColor.rgb * glowEdge * WaveParams.w * 1.5;
        
        float rays = abs(sin(texCoord.x * 20.0 + time * 0.5)) * 0.1 * glowEdge;
        glow += vec3(1.0, 0.95, 0.8) * rays * WaveParams.w;
        
        finalColor = mix(scene, FogColor.rgb + glow, fogMask * FogColor.a);
    }
    else if (weatherType == 1) {
        float waveEffect = waves(texCoord, time * 1.5);
        float rain = rainEffect(texCoord, time);
        
        fogMask = fogMask * (0.65 + waveEffect * 0.2 + rain * WeatherParams.y * 0.15);
        
        vec3 rainFogColor = FogColor.rgb * 0.6;
        
        rainFogColor = mix(rainFogColor, vec3(0.5, 0.55, 0.65), 0.3);
        
        finalColor = mix(scene, rainFogColor, fogMask * FogColor.a * 1.3);
        
        if (linearDistance < 0.4) {
            float rainOverlay = rain * WeatherParams.y * 0.2;
            finalColor = mix(finalColor, rainFogColor * 0.8, rainOverlay);
        }
        
        float ripples = sin(texCoord.x * 50.0 + time * 10.0) * sin(texCoord.y * 50.0 + time * 8.0);
        ripples = smoothstep(0.8, 1.0, ripples) * 0.05 * WeatherParams.y;
        finalColor += vec3(0.6, 0.65, 0.7) * ripples;
    }
    else if (weatherType == 2) {
        float waveEffect = waves(texCoord, time * 2.0);
        float rain = rainEffect(texCoord, time * 1.2);
        
        fogMask = fogMask * (0.55 + waveEffect * 0.25 + rain * 0.15);
        
        vec3 stormFogColor = FogColor.rgb * 0.4;
        stormFogColor = mix(stormFogColor, vec3(0.3, 0.3, 0.4), 0.5);
        
        finalColor = mix(scene, stormFogColor, fogMask * FogColor.a * 1.5);
        
        finalColor = thunderFlash(finalColor, WeatherParams.z, texCoord, time);
        
        if (linearDistance < 0.5) {
            float stormRain = rain * 0.3;
            finalColor = mix(finalColor, stormFogColor * 0.7, stormRain);
        }
        
        float darkGlow = smoothstep(0.3, 0.7, fogMask) * (1.0 - smoothstep(0.7, 1.0, fogMask));
        finalColor += vec3(0.2, 0.2, 0.3) * darkGlow * 0.3;
    }
    else if (weatherType == 3) {
        float waveEffect = waves(texCoord, time * 0.4);
        float snow = snowEffect(texCoord, time);
        float swirls = fogSwirls(texCoord, time * 0.5);
        
        fogMask = fogMask * (0.8 + waveEffect * 0.1 + swirls * 0.15);
        
        vec3 snowFogColor = mix(FogColor.rgb, vec3(0.9, 0.95, 1.0), 0.4);
        
        finalColor = mix(scene, snowFogColor, fogMask * FogColor.a);
        
        finalColor = mix(finalColor, vec3(1.0, 1.0, 1.0), snow * 0.5);
        
        float coldGlow = smoothstep(0.3, 0.7, fogMask) * (1.0 - smoothstep(0.7, 0.95, fogMask));
        vec3 iceGlow = vec3(0.7, 0.85, 1.0) * coldGlow * WaveParams.w * 0.5;
        finalColor += iceGlow;
        
        if (linearDistance < 0.2) {
            float frost = noise(texCoord * 100.0 + time * 0.1);
            frost = smoothstep(0.6, 0.8, frost) * 0.1;
            finalColor = mix(finalColor, vec3(0.95, 0.97, 1.0), frost);
        }
    }
    
    fragColor = vec4(finalColor, 1.0);
}

