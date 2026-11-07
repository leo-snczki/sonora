# The Flow style ports the original's renderer

The Flow Fluid Style is a port of Diffuse's fluid: its noise shader, the Kawase pre-blur of the Album Art and the final blur tap, read from the original APK (shaders in `assets/`, the renderer decompiled with jadx) and rewritten for OpenGL ES 3.0 (ADR-0001). This reverses the clean-room rule for the renderer only: the rest of Sonora (UI, copy, Media Sessions, Backdrops) stays written from scratch. The user chose it because the fluid is the heart of the app and the original's works very well; "faithful to the original" here means the same algorithm and constants, not a look-alike.

Consequence: this code is derived from Depthware's copyrighted work. That is acceptable for a private repository shared with friends, but it **blocks a Play Store release or open-sourcing Sonora** until Flow is rewritten from a behaviour-only description. The Cover style remains our own work.

## Considered Options

- Behaviour spec written by an isolated agent, implemented blind (clean room): rejected by the user, too far from "exact".
- Black-box imitation from screen recordings: rejected, further still.
- Port the shaders and logic: chosen. Known quirks of the original are kept where they shape the look (the final tap's offset in 192px units, the 2.5 density fallback) and fixed where they only depend on the device (the track transition and speed ramp become time-based instead of per frame).
