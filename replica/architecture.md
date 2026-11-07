# Architecture: Sonora (a rebuild of Diffuse's core features)

Source: `replica/recon.md` and `replica/features.csv` (2026-11-06).

Diffuse has no server: everything happens on the phone. So Sonora has no
backend, no accounts, no email and no hosting. The skill's web stack does not
apply. Each layer below is the Android equivalent, and "API" means the
Android components and system APIs the app exposes and uses.

## Stack

| layer | choice | why |
| --- | --- | --- |
| language / UI | Kotlin 2.x + Jetpack Compose (Material 3) | the toolchain already in the flake; the `android-development` skill targets it |
| architecture | NowInAndroid-style MVVM, small module set (below), Hilt | the skill's default; keeps the renderer testable away from the UI |
| render | raw OpenGL ES 3.0 in a `WallpaperService.Engine` with its own EGL surface | LibGDX (the original's choice) is a whole game engine for 3 shader passes; plain GLES is smaller, starts faster and gives full control of the frame loop |
| settings | Preferences DataStore | about 20 typed keys, a `Flow` the wallpaper and the UI both observe |
| local data | Room (SQLite), 1 table | the user's own Backdrops only; built-in Backdrops are gradients in code |
| files | app-private `filesDir` (Backdrop images, preserved Album Art) | no permission needed; deleted with the app |
| now playing | `MediaSessionManager.getActiveSessions()` + `NotificationListenerService` | the listener grants the access; sessions give metadata, art and play state without guessing from notification flags (fixes the original's ReVanced / Android 16 bug) |
| beats | `android.media.audiofx.Visualizer` on the output mix (session 0), FFT | works without a per-session capture prompt; needs only `RECORD_AUDIO` |
| image picking / crop | Android Photo Picker + an in-house Compose cropper (square crop, rotate) | no storage permission; the original's cropper library is not needed for a square crop |
| sharing | `FileProvider` + `ACTION_SEND`; video via `MediaProjection` + `MediaRecorder` (could-have) | official APIs only |
| payments | none | Sonora is not sold |
| analytics / crashes | none | Sonora ships **without the `INTERNET` permission** (the original has Firebase, Sentry, ad ID) |
| strings | English default + Portuguese (pt-BR) | all text in resources, never hardcoded |
| min / target SDK | minSdk 26, targetSdk 35 | 26 covers notification channels and adaptive icons; `WallpaperColors` (27) is guarded at runtime |
| build / CI | Gradle (Kotlin DSL) + version catalog; GitHub Actions runs only `./gradlew check` (JVM tests + lint) | private repo, keep CI minutes low; the GL smoke test runs locally on an API 36 emulator |
| distribution | release APK signed with `work/sonora.jks`, installed with adb / `android` CLI | not sold; built to a publishable bar so it can be handed to friends |
| reference devices | the tablet in `replica/screens/` + an API 36 phone emulator | "works" means works on both |

Name **Sonora**, applicationId `dev.leosnczki.sonora` (no rebrand step).
Domain terms follow `GLOSSARY.md`; ADR-0001 (plain GLES) and ADR-0002 (Now
Playing from Media Sessions) apply.

### Modules

```
:app                     MainActivity (S01 host), Hilt app, manifest, nav
:core:model              Settings, NowPlaying, Backdrop, Effect (pure Kotlin)
:core:data               DataStore settings repo, Room (BackdropDao), file store
:core:media              NowPlayingSource (sessions + listener), BeatDetector (Visualizer)
:core:render             GL renderer: art texture, fluid warp, blur, fluted glass, post; no Android UI
:core:designsystem       Compose theme and components (tokens from /replica-design)
:feature:home            S01 cards: setup, now playing, configuration, help, about
:feature:backdrops       S04 Backdrop Library, S05 crop
:feature:share           S07/S08 image share, S09 video (could)
:wallpaper               SonoraWallpaperService: binds render + media + settings
```

`:wallpaper` and `:app` run in **one process**, so the settings `Flow` and the
now-playing `StateFlow` are shared in memory, without IPC.

## Schema

Tables: 1 (Room). Access rules: app-private storage. There is a single local
user, so there are no owner columns and no RLS. Settings live in DataStore,
not SQL (key list below). SQL is in `replica/schema.sql`.

```sql
-- Room entity BackdropEntity; SQLite has no timestamptz, so times are UTC epoch millis
create table backdrops (
  id          text    primary key not null,          -- UUID string
  name        text    not null,
  source      text    not null check (source in ('album_art','upload')),
  image_path  text    not null,                      -- filesDir-relative
  sort_order  integer not null default 0,
  created_at  integer not null,
  updated_at  integer not null
);
create index backdrops_order on backdrops (sort_order, created_at);
```

Built-in Backdrops (about 8 gradients of 2 to 4 colours) are defined in code
and never stored. A Backdrop id is either `builtin:<key>` or the UUID of a
row.

On delete: deleting a Backdrop deletes its image file in the same repository
call (file first, then row; an orphan file is harmless, a row pointing at
nothing is not). If it was the chosen Backdrop, the choice falls back to the
first built-in one.

### DataStore keys (`settings.preferences_pb`)

| key | type | default | feature |
| --- | --- | --- | --- |
| fluid_override | bool | false | fluid settings group |
| fluid_scale | float 0..1 | 0.5 | |
| speed_min / speed_max | float 0..1 | 0.15 / 0.6 | speed drops to min on pause |
| graphics_override | bool | false | |
| effect | enum `blur` / `fluted_glass` | blur | |
| beat_pulse | bool | false | needs RECORD_AUDIO |
| beat_strength | float 0..1 | 0.5 | |
| album_override | bool | false | |
| preserve_after_reboot | bool | false | |
| preserve_after_pause | bool | true | |
| backdrop_id | string | `builtin:<first>` | chosen Backdrop |
| misc_override | bool | false | |
| adaptive_launcher_colors | bool | false | WallpaperColors |
| brightness | float -1..1 | 0 | |
| performance_mode | bool | false | |
| debug_view | bool | false | |
| preserved_art_path | string? | null | last art saved to `filesDir/preserved.webp` |

Not persisted: `NowPlaying(packageName, title, artist, album, art, isPlaying)`
is an in-memory `StateFlow`. Only its art is written to disk, and only when a
preserve option is on.

## API (components, intents and system calls)

### Exposed by Sonora (manifest)

| component | does | who calls it | input | output | flow |
| --- | --- | --- | --- | --- | --- |
| `MainActivity` (LAUNCHER, also wallpaper `settingsActivity`) | S01 | user, system wallpaper picker | — | — | F01, F03, F07 |
| `SonoraWallpaperService` (`BIND_WALLPAPER`) | renders S03 | system | surface, visibility, offsets | frames, `onComputeColors()` | F02 |
| `res/xml/wallpaper.xml` | label, author, description, thumbnail, settings activity | system picker (S02) | — | — | F01 |
| `NowPlayingListenerService` (`BIND_NOTIFICATION_LISTENER_SERVICE`) | grants session access; notification fallback | system | `StatusBarNotification` | `NowPlaying` | F02 |
| `BackdropsActivity` (internal, transparent) | S04 / S05 | S01 | — | `backdrop_id` | F04 |
| `MusicAppsActivity` (internal) | S06 | S01 | — | launches the chosen player | F02 |
| `FileProvider` | shares generated images / videos | share targets | — | content URI | F05, F06 |
| `ScreenRecordService` (`foregroundServiceType=mediaProjection`) | S09 | S01 | projection token | mp4 | F06 (could) |

### Used by Sonora (system APIs)

| call | does | permission | flow |
| --- | --- | --- | --- |
| `Intent(ACTION_CHANGE_LIVE_WALLPAPER)` with our component | opens S02 directly on Sonora | — | F01 |
| `WallpaperManager.getWallpaperInfo()` | "wallpaper set?" for the checklist | — | F01 |
| `Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS)` (+ detail settings on API 30+) | notification access | user toggle | F01 |
| `NotificationManagerCompat.getEnabledListenerPackages()` | "access granted?" | — | F01 |
| `MediaSessionManager.getActiveSessions(listener)` + `addOnActiveSessionsChangedListener` + `MediaController.Callback` | metadata, art, play state | notification access | F02 |
| `NotificationListenerService.requestRebind()` | "Restart listener" | — | F07 |
| `requestPermissions(RECORD_AUDIO)` | Beat Pulse | runtime | F01, F03 |
| `Visualizer(0)` FFT capture | beat energy | RECORD_AUDIO | F02 |
| `queryIntentActivities(CATEGORY_APP_MUSIC)` (+ manifest `<queries>`) | list players | — | F02 |
| `PickVisualMedia` | upload a Backdrop image | — | F04 |
| `WallpaperColors` / `notifyColorsChanged()` | adaptive launcher colours | — | F03 |
| `MediaProjectionManager` + `AudioPlaybackCaptureConfiguration` | video with device audio | consent each time | F06 |

Webhooks in/out: none. Jobs: none scheduled. The listener re-binds on boot by
itself; if `preserve_after_reboot` is on, the engine reads the preserved art
when it starts, so no boot receiver is needed.

## The parts that bite

- **Several engines at once**: the preview (S02), the home screen and the lock
  screen can each create an `Engine`. Each one gets its own EGL context, but
  they share one `NowPlaying` / settings source and one decoded art bitmap, and
  only one `Visualizer`, reference-counted.
- **Battery**: render only while `onVisibilityChanged(true)`. Cap at 30 fps
  when idle or paused, 60 when playing. Blur at 1/4 resolution (blur hides
  it). Performance mode = 1/8 resolution and 30 fps. Release GL and the
  Visualizer when nothing is visible.
- **GL context loss / surface resize**: rotation and foldables recreate the
  surface. Rebuild FBOs on `onSurfaceChanged`, never assume the size.
- **Art arrives late or twice**: metadata often comes before the bitmap, and
  players re-post the same art. Hash the downscaled art (64×64) and ignore
  repeats; crossfade old → new over ~1.5 s, never swap hard.
- **Which session wins**: several players can hold sessions. Prefer the one
  with `STATE_PLAYING`, then the most recently active. Never filter on
  notification flags (`isOngoing` / `isClearable`): that is the original's
  bug.
- **OEM process killers**: Xiaomi, Samsung and others kill the listener. Offer
  "Restart listener" (`requestRebind`) plus a link to the battery optimisation
  exemption. Detect "access granted but no callbacks for N minutes" and show
  the hint.
- **Visualizer quirks**: deprecated output-mix use, silence on some OEMs or
  with some streaming DRM, and it must be `release()`d or it leaks
  system-wide. Beat Pulse must degrade to "no beat" quietly, not crash. The
  detector uses spectral flux on the bass bands with an adaptive threshold, so
  loud and quiet tracks both pulse.
- **Large art**: players hand over up to 1500 px bitmaps. Downscale to 512 px
  on a background dispatcher before uploading the texture.
- **Privacy**: no `INTERNET`, no analytics. Notification content is read in
  memory and never stored, except the preserved art.
- Not relevant here: time zones, webhook idempotency, multi-tenancy, email,
  search, realtime sync. GDPR deletion = uninstall (no data leaves the device).

## Build order

1. **Vertical slice (proves the stack)**: S01 with only "Set wallpaper" and
   "Allow notification access" → S02 → S03 renders the **current album art**
   from `MediaSessionManager`, warped + blurred, crossfading on track change.
   Modules: app, core:model, core:media, core:render, wallpaper. No settings
   yet (hardcoded defaults), no Room.
2. **Must-haves** (`features.csv` must rows):
   - render: pause when invisible, speed min/max on play/pause, idle with the
     Backdrop.
   - S01 setup checklist with all three states, Current Playing card (3
     states), wallpaper metadata.
   - DataStore + Fluid settings group (override toggle, like the original) applied live.
   - Now Playing selection rule + 10 s pause grace, two-column layout from 600dp, en + pt-BR strings.
3. **Should-haves**:
   - Beat Pulse (`BeatDetector` + strength slider + permission states + 30 s silence hint).
   - Fluted glass effect, brightness.
   - Preserve after pause / reboot.
   - Room + S04 Backdrop Library (built-in gradients in code) + S05 crop + save current
     art.
   - S06 Player chooser, help card with Restart listener + battery hint on known OEMs, About card with licences.
4. **Could-haves**: performance mode, WallpaperColors, debug overlay (fps,
   beat meter), S07/S08 share image, then
   S09 video last (most platform friction, experimental in the original too).
5. **Fixes from `/replica-entrepreneur`**: once it has run. The listener fix
   is already in milestone 1.
