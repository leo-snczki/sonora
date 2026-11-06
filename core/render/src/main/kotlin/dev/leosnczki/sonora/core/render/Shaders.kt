package dev.leosnczki.sonora.core.render

internal object Shaders {
    /** One full-screen triangle; uv covers 0..1 on screen. */
    const val VERTEX = """#version 300 es
out vec2 vUv;
void main() {
    vec2 p = vec2(float((gl_VertexID << 1) & 2), float(gl_VertexID & 2));
    vUv = p;
    gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);
}
"""

    /**
     * The fluid: the image sampled through a twice-warped fractal noise field that drifts with
     * [uPhase]. Two images are mixed for the crossfade. [uCrop] centre-crops the image to the screen.
     */
    const val WARP = """#version 300 es
precision highp float;
in vec2 vUv;
out vec4 outColor;
uniform sampler2D uCurrent;
uniform sampler2D uPrevious;
uniform float uMix;
uniform float uPhase;
uniform float uScale;
uniform vec2 uCrop;

float hash(vec2 p) {
    p = fract(p * vec2(234.34, 435.345));
    p += dot(p, p + 34.23);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
}

float fbm(vec2 p) {
    float sum = 0.0;
    float amplitude = 0.5;
    for (int octave = 0; octave < 4; octave++) {
        sum += amplitude * noise(p);
        p = p * 2.02 + vec2(3.1, 1.7);
        amplitude *= 0.5;
    }
    return sum;
}

vec2 mirror(vec2 uv) {
    return 1.0 - abs(mod(uv, 2.0) - 1.0);
}

void main() {
    vec2 p = vUv * mix(1.2, 3.5, uScale);
    float t = uPhase;
    vec2 first = vec2(fbm(p + vec2(0.0, t)), fbm(p + vec2(4.7, -t * 0.8)));
    vec2 second = vec2(fbm(p + 3.0 * first + vec2(2.3, 7.1) + t * 0.4),
                       fbm(p + 3.0 * first + vec2(6.1, 1.9) - t * 0.3));
    vec2 uv = (vUv - 0.5) * uCrop + 0.5;
    uv = mirror(uv + (second - 0.5) * mix(0.35, 0.8, uScale));
    vec3 color = mix(texture(uPrevious, uv).rgb, texture(uCurrent, uv).rgb, uMix);
    outColor = vec4(color, 1.0);
}
"""

    /** One Kawase blur step: four diagonal taps at a growing distance. */
    const val KAWASE = """#version 300 es
precision mediump float;
in vec2 vUv;
out vec4 outColor;
uniform sampler2D uSource;
uniform vec2 uTexel;
uniform float uDistance;

void main() {
    vec2 d = (uDistance + 0.5) * uTexel;
    vec4 sum = texture(uSource, vUv + vec2(-d.x, -d.y));
    sum += texture(uSource, vUv + vec2(d.x, -d.y));
    sum += texture(uSource, vUv + vec2(-d.x, d.y));
    sum += texture(uSource, vUv + vec2(d.x, d.y));
    outColor = sum * 0.25;
}
"""

    /** Final pass to the screen: brightness, plus a little noise so gradients don't band. */
    const val COMPOSE = """#version 300 es
precision mediump float;
in vec2 vUv;
out vec4 outColor;
uniform sampler2D uSource;
uniform float uBrightness;

float dither(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453) - 0.5;
}

void main() {
    vec3 color = texture(uSource, vUv).rgb;
    color = uBrightness >= 0.0 ? mix(color, vec3(1.0), uBrightness * 0.5) : color * (1.0 + uBrightness * 0.8);
    color += dither(gl_FragCoord.xy) / 255.0;
    outColor = vec4(color, 1.0);
}
"""
}
