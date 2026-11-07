# Plain OpenGL ES renderer, no game engine

The wallpaper renders with OpenGL ES 3.0 directly inside the wallpaper engine, with its own EGL surface, instead of LibGDX (the engine Diffuse uses). The scene is a few full-screen shader passes (fluid warp, blur, fluted glass, post), so a game engine would add size, start-up time and its own frame loop that fights the wallpaper lifecycle (several engines at once, pause when invisible), and give nothing back.

## Consequences

EGL setup, surface loss and frame pacing are ours to write and test. The renderer stays a thin consumer of a pure-Kotlin scene state, so all behaviour worth testing lives outside GL.
