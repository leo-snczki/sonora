# Now Playing comes from Media Sessions, never from notification flags

Sonora picks Now Playing from the system's active Media Sessions (granted by notification access), and never filters on notification flags such as ongoing or clearable. Diffuse accepts a media notification only if it is ongoing and not clearable, which drops Players such as YouTube Music ReVanced and the Android 16 behaviour, so its Now Playing stays empty. Reading the notifications is only a fallback, and even then any notification that carries a Media Session counts, whatever its flags.

## Considered Options

- Filter notifications by ongoing / clearable flags, like Diffuse: rejected, it is the bug above.
- Accept any notification with a Media Session token: works, kept as the fallback.
- Media Sessions from the system session manager: chosen, gives play state, track info and Album Art directly and tracks session changes.
