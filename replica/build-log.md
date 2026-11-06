# Build log

One line per ticket / screen: ID, date, status, what is missing, what was harder than expected.

| ticket | screens | date | status | missing | harder than expected |
| --- | --- | --- | --- | --- | --- |
| #3 scaffold + design system | catalog | 2026-11-06 | done | Cropper and debug overlay components (belong to #13 and a could-have); on-device check waits for the tablet to be unlocked | Robolectric `captureToImage` never settles with a running spinner: the catalog screenshot draws the view instead. The SDK emulator binary doesn't run on NixOS (missing X11 libs) |
| #4 settings shell | S01 | 2026-11-06 | done | Setup, Now Playing and settings cards are filled by #5–#9 | — |
| #5 GL wallpaper (tracer) | S02, S03 | 2026-11-06 | done | visual check on the tablet (it was locked); the smoke test passed on its GPU | none; one GL thread per engine keeps preview + home + lock independent |
