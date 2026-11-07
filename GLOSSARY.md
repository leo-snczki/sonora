# Sonora

A personal Android live wallpaper that turns the music playing on the phone
into a flowing, blurred image of its album art, and pulses with the beat.

## Language

### Music

**Player**:
An app on the phone that plays music, such as Spotify or YouTube Music.
_Avoid_: music app (in the model), source

**Media Session**:
The playback session a Player exposes to the system: its track info, Album Art and playing or paused state.
_Avoid_: notification, media notification, controller

**Now Playing**:
The one Media Session Sonora has chosen to show: the most recent one that is playing or, when none is playing, the last one that played.
_Avoid_: current track, current song, last playing

**Album Art**:
The cover image a Media Session carries for what it is playing.
_Avoid_: artwork, cover, album image

### Wallpaper

**Backdrop**:
The image the wallpaper shows when there is no Album Art to show.
_Avoid_: default palette, default color, palette, texture

**Backdrop Library**:
The set of Backdrops to choose from: the built-in ones and the ones the user saved.
_Avoid_: palettes, saved palettes

**Beat Pulse**:
The wallpaper's motion reacting to beats detected in the audio that is playing.
_Avoid_: Live Beats, visualizer, volume reaction
