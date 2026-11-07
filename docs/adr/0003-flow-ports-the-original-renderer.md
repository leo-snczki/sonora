# The Flow style ports the original's renderer

The Flow Fluid Style is a port of Diffuse's fluid (and, since #10, the Fluted Glass effect and Flow's brightness multiply): its noise shader, the Kawase pre-blur of the Album Art and the final blur tap, read from the original APK (shaders in `assets/`, the renderer decompiled with jadx) and rewritten for OpenGL ES 3.0 (ADR-0001). This reverses the clean-room rule for the renderer only: the rest of Sonora (UI, copy, Media Sessions, Backdrops) stays written from scratch. The user chose it because the fluid is the heart of the app and the original's works very well; "faithful to the original" here means the same algorithm and constants, not a look-alike.

Consequence: this code is derived from Depthware's copyrighted work. That is acceptable for a private repository shared with friends, but it **blocks a Play Store release or open-sourcing Sonora** until Flow is rewritten from a behaviour-only description. The Cover style's own passes remain our own work, but with the Fluted Glass effect Cover also goes through the ported shader, so a rewrite must cover Fluted Glass in both styles.

## Considered Options

- Behaviour spec written by an isolated agent, implemented blind (clean room): rejected by the user, too far from "exact".
- Black-box imitation from screen recordings: rejected, further still.
- Port the shaders and logic: chosen. Known quirks of the original are kept where they shape the look (the final tap's offset in 192px units, the 2.5 density fallback) and fixed where they only depend on the device (the track transition and speed ramp become time-based instead of per frame).

Fluted Glass keeps the original's quirks: the strip count (16 × width / height) is not rounded, so the last strip is cut short, and the last strip reads past the fluid's right edge, which the texture clamps; and Flow skips the final brightness multiply under Fluted Glass, as the original does.

Other deliberate differences: the fluid's clock is not wrapped at 10000 like the original's (it stays precise in highp for days, and wrapping would jump the fluid); the speed starts at the paused speed, not at 1.0; Performance Mode (`rand.png`) is not ported; and Flow's pre-blur and fade also run while Cover is shown (one 192px pass per frame), so switching to Flow shows current colours at once.
