package dev.leosnczki.sonora.core.render

internal object Shaders {
    /** Interleaved gradient noise: a cheap, well spread dither that never leaves 0..1. */
    private const val DITHER = """
float dither(vec2 p) {
    return fract(52.9829189 * fract(dot(p, vec2(0.06711056, 0.00583715)))) - 0.5;
}
"""

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
/** 1 to mirror that axis: x for Mirror horizontal, y for Flip vertical. */
uniform vec2 uFlip;
uniform float uBrightness;

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
    // Textures hold the bitmap top row at t = 0, while vUv.y = 0 is the bottom of the screen.
    uv.y = 1.0 - uv.y;
    uv = mix(uv, 1.0 - uv, uFlip);
    vec3 color = mix(texture(uPrevious, uv).rgb, texture(uCurrent, uv).rgb, uMix);
    // Brightness here, before the blur and whatever final effect, so every effect shows it.
    color = uBrightness >= 0.0 ? mix(color, vec3(1.0), uBrightness * 0.5) : color * (1.0 + uBrightness * 0.8);
    outColor = vec4(color, 1.0);
}
"""

    /**
     * One Kawase blur step: four diagonal taps at a growing distance, times [uGain] (the
     * original's brightness multiply in Flow's pre-blur; 1 elsewhere).
     */
    const val KAWASE = """#version 300 es
precision mediump float;
in vec2 vUv;
out vec4 outColor;
uniform sampler2D uSource;
uniform vec2 uTexel;
uniform float uDistance;
uniform float uGain;

void main() {
    vec2 d = (uDistance + 0.5) * uTexel;
    vec4 sum = texture(uSource, vUv + vec2(-d.x, -d.y));
    sum += texture(uSource, vUv + vec2(d.x, -d.y));
    sum += texture(uSource, vUv + vec2(-d.x, d.y));
    sum += texture(uSource, vUv + vec2(d.x, d.y));
    outColor = vec4(sum.rgb * 0.25 * uGain, 1.0);
}
"""

    /**
     * Cover's final pass to the screen with the Blur effect: a little noise so gradients don't band.
     * highp, and a noise whose inputs stay small: in mediump (16-bit on Mali) a classic
     * sin(dot(fragCoord, ...)) hash overflows past ~65k and turns pixels black along a line.
     */
    const val COMPOSE = """#version 300 es
precision highp float;
in vec2 vUv;
out vec4 outColor;
uniform sampler2D uSource;

$DITHER
void main() {
    vec3 color = texture(uSource, vUv).rgb;
    color += dither(gl_FragCoord.xy) / 255.0;
    outColor = vec4(color, 1.0);
}
"""

    /**
     * Flow, ported from the original (ADR-0003): a twice domain-warped fbm whose value is the
     * coordinate to read the pre-blurred image at, so the image dissolves into its own colours.
     * [uScale] widens the window of the image that is read. v runs top-down here, as in the
     * original, so the fbm field keeps its orientation. highp: [uTime] grows without bound.
     */
    const val FLOW_NOISE = """#version 300 es
precision highp float;
in vec2 vUv;
out vec4 outColor;
uniform sampler2D uImage;
uniform float uTime;
uniform vec2 uResolution;
uniform float uScale;

float random(vec2 st) {
    float dt = dot(st, vec2(12.9898, 78.233));
    float sn = mod(dt, 3.14);
    return fract(sin(sn) * 43758.5453);
}

float noise(vec2 st) {
    vec2 i = floor(st);
    vec2 f = fract(st);
    float a = random(i);
    float b = random(i + vec2(1.0, 0.0));
    float c = random(i + vec2(0.0, 1.0));
    float d = random(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

float fbm(vec2 st) {
    float v = 0.0;
    float a = 0.5;
    vec2 shift = st * 0.5;
    mat2 rot = mat2(cos(0.5), sin(0.5), -sin(0.5), cos(0.5));
    for (int i = 0; i < 3; ++i) {
        v += a * noise(st);
        st = rot * st * 1.3 + shift;
        a *= 0.5;
    }
    return v;
}

void main() {
    vec2 uv = vec2(vUv.x, 1.0 - vUv.y);
    if (uResolution.x > uResolution.y) {
        uv.x = uv.x * (uResolution.x / uResolution.y);
    } else {
        uv.y = uv.y / (uResolution.x / uResolution.y);
    }
    vec2 q = vec2(fbm(uv + vec2(uTime * 0.1, cos(uTime))), fbm(uv + vec2(uTime * 0.1, uTime)));
    vec2 r = vec2(fbm(0.5 * uv + 2.0 * q + vec2(uTime * 0.05, 0.0)),
                  fbm(0.5 * uv + 2.0 * q + vec2(uTime * 0.1, 0.0)));
    r = (r - 0.5) * uScale + 0.5;
    outColor = vec4(texture(uImage, r).rgb, 1.0);
}
"""

    /**
     * Flow's track change, as in the original: each texel walks a straight line in RGB towards
     * the new image, [uStep] at a time, and lands on it once closer than one step.
     */
    const val FLOW_FADE = """#version 300 es
precision highp float;
in vec2 vUv;
out vec4 outColor;
uniform sampler2D uShown;
uniform sampler2D uTarget;
uniform float uStep;

void main() {
    vec3 shown = texture(uShown, vUv).rgb;
    vec3 target = texture(uTarget, vUv).rgb;
    float distance = length(target - shown);
    float amount = distance <= uStep ? 1.0 : uStep / distance;
    outColor = vec4(mix(shown, target, amount), 1.0);
}
"""

    /**
     * Flow's final pass to the screen, as in the original: one light Kawase tap (its offset is in
     * units of the 192px image, not of the screen), the brightness multiply [uGain], then our dither.
     */
    const val FLOW_COMPOSE = """#version 300 es
precision highp float;
in vec2 vUv;
out vec4 outColor;
uniform sampler2D uSource;
uniform vec2 uStep;
uniform float uGain;

$DITHER
void main() {
    vec2 flipped = vec2(-uStep.x, uStep.y);
    vec3 color = (texture(uSource, vUv + uStep).rgb + texture(uSource, vUv - uStep).rgb
        + texture(uSource, vUv + flipped).rgb + texture(uSource, vUv - flipped).rgb) * 0.25;
    color *= uGain;
    color += dither(gl_FragCoord.xy) / 255.0;
    outColor = vec4(color, 1.0);
}
"""

    /**
     * The Fluted Glass effect, ported from the original (ADR-0003), for both styles: [uSegments]
     * vertical strips of reeded glass, each showing a log-compressed, wider slice of the fluid,
     * dark on its right edge and light on its left, with the top fifth of the screen shaded. No
     * brightness here, as in the original. Then our dither.
     */
    const val FLUTED_GLASS = """#version 300 es
precision highp float;
in vec2 vUv;
out vec4 outColor;
uniform sampler2D uSource;
uniform float uSegments;

$DITHER
void main() {
    const float inputOutputRatio = 1.8;
    const float overlap = 0.8;
    const float lightStrength = 0.10;

    float segmentWidth = 1.0 / uSegments;
    float inputSegmentWidth = segmentWidth * inputOutputRatio;
    float overlapWidth = segmentWidth * overlap;
    float segmentIndex = floor(vUv.x / segmentWidth);
    float localX = (vUv.x - segmentIndex * segmentWidth) / segmentWidth;
    float compressedX = log(1.0 + localX * 9.0) / log(10.0);
    float inputStart = segmentIndex * (inputSegmentWidth - overlapWidth);
    vec4 color = texture(uSource, vec2(inputStart + compressedX * inputSegmentWidth, vUv.y));

    float top = smoothstep(0.8, 1.0, vUv.y);
    color = mix(color, vec4(0.0, 0.0, 0.0, 0.5), top * 0.5);
    float right = smoothstep(0.8, 1.0, localX);
    color = mix(color, vec4(0.0, 0.0, 0.0, right), right * lightStrength);
    float left = smoothstep(0.1, 0.0, localX);
    color = mix(color, vec4(1.0, 1.0, 1.0, left), left * lightStrength);

    outColor = vec4(color.rgb + dither(gl_FragCoord.xy) / 255.0, 1.0);
}
"""
}
