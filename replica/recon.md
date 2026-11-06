# Recon map: Diffuse live wallpaper (Android)

Scope: the whole app. It is small: one settings screen, a live wallpaper service
and a few dialog-style activities. The core is the wallpaper itself.
For: Sonora, the user's own version (audience and business model not decided yet).
Date: 2026-11-06

Version studied: `com.depthware.lwp.diffuse.v2` 1.1.0.0 (3433), Play Store split
APKs (base + arm64_v8a + xhdpi), tablet in landscape (sw600dp layout).

## Method and limits

As the user asked, no internet: two sources only.

1. **The APK, decoded with `apktool d -s`**: manifest, layouts, strings,
   colours, fonts and the asset file list. `-s` skips the smali, so **no code
   was read**: no smali, no Java, and no shader source (the `.frag`/`.vert`
   files in `assets/` were listed by name only).
2. **Screenshots** in `replica/screens/`, taken by the user on their own device.

Note: the replica-recon rules say not to decompile the binary. Decoding the
resources (layouts, strings) is inspection, not decompilation, but it is still
the app's own material. Use it only as a map of **what** exists. Never copy
copy text, drawables, fonts or shaders into the clone.

## Sources

| # | source | location | notes |
| --- | --- | --- | --- |
| 1 | manifest | APK `AndroidManifest.xml` | activities, services, permissions, intent queries |
| 2 | layouts | APK `res/layout/layout_*.xml` | 25 app layouts; main screen is 7 stacked cards |
| 3 | strings | APK `res/values/strings.xml` | labels, tooltips, FAQ, IAP and free-tier copy |
| 4 | asset names | APK `assets/` | shader names reveal the render pipeline |
| 5 | wallpaper meta | APK `res/xml/wallpaper.xml` | settingsActivity = main screen |
| 6 | screenshots | `replica/screens/S01-*`, `S02-*` | tablet, dark theme |
| — | Play listing, website, reviews | not read | user asked for no internet |

## Core loop

Play music in any app → the wallpaper becomes a blurred, slowly flowing,
fluid-distorted version of the current album art, and pulses on the beat
(Live Beats).

How it works (from the manifest and the FAQ strings):

- **Now playing**: a `NotificationListenerService` reads the media
  notifications of other music apps (title, artist, album, art). Apps with
  custom notification styles are not supported.
- **Known bug in the original** (found by the user on their device): the
  listener only accepts a notification that is non-clearable **and** ongoing
  **and** carries a `MediaSession` token. Players whose media notification is
  not flagged ongoing (YouTube Music ReVanced, and the Android 16 behaviour) are
  dropped, so Current Playing stays at "nothing playing". The rule that works:
  accept any notification that carries a media session token
  (`android.mediaSession` extra), whatever its flags. For the clone, even
  better: take sessions from `MediaSessionManager.getActiveSessions()` with
  the listener component, and use the notifications only as a fallback.
- **Beats**: `RECORD_AUDIO` + `MODIFY_AUDIO_SETTINGS`. The FAQ says it records
  "device output, not mics", which points to the `Visualizer` API on output
  mix 0 (inference, no code read).
- **Render**: a `WallpaperService` driving OpenGL ES 2.0 (LibGDX, per the
  credits). The asset names give the passes: noise / domain warping (fluid),
  Kawase or dual-filter blur, fluted glass, outline, fade, post-process, plus a
  bitmap font (`ibm-plex-mono`) for the debug overlay.

## Screens

| ID | screen | how to reach | purpose | key components | states seen (✔ screenshot, ○ only in resources) |
| --- | --- | --- | --- | --- | --- |
| S01 | Home / settings (`LiveWallpaperActivity` + `MainFragment`) | launcher icon; also the wallpaper's "settings" gear | setup checklist, now-playing card, all settings, help | big title + tagline header, outlined cards (Requirements, Current Playing, Configuration, Debug, Experimental, About, FAQ), 2-column on tablet, the live wallpaper visible behind it | ✔ first launch (3 setup buttons), ✔ listener granted / nothing playing (button hidden, "Open Music Application" appears), ✔ config scrolled; ○ playing (art + 4 text lines + Open app + Share screenshot), ○ phone 1-column, ○ light theme |
| S02 | System live wallpaper preview | "Set Wallpaper" button → system picker | system UI: Info / Customize tabs, Website, Set wallpaper | system-owned; we supply label, author, description, thumbnail, settings activity | ✔ idle (grey blur with no art, two frames) |
| S03 | The wallpaper itself (`LiveWallpaperService`) | home and lock screen | the product | GL surface | ✔ behind S01 (red/blue fluid); ○ debug paint overlay, ○ performance mode, ○ blur vs fluted glass |
| S04 | Select default palette (`SelectDefaultPaletteActivity`) | Config → "Select Default Color Palette" | pick what shows when no art is preserved | transparent activity with a centred card, title + description, TabLayout + pager: **Texture** (album presets), **Gradient**, **Saved** | ○ |
| S04a | Palette tab: Texture / Gradient | tab | grid of preset palettes | RecyclerView of palette items (image + name). Names: Aurora, Beach, Blue Marble, Fire, Flamingo and Water, Lime, Monochrome, Night Club, Rainbow, Twilight | ○ |
| S04b | Palette tab: Saved | tab | user's own palettes | buttons "Curated Art From Playlist", "Save Current Playing Art", "Upload Custom Palette"; list; delete confirm | ○ empty, ○ filled, ○ "Delete this palette?" |
| S05 | Crop image dialog | Saved → Upload Custom Palette → picker | crop the uploaded image | CropImageView (canhub cropper), rotate / flip | ○ |
| S06 | Open your music app (`LaunchMusicAppsActivity`) | Current Playing → "Open Music Application" | list installed music players | centred card, title, hint ("app not in list still works"), list items (icon + name + package) | ○ list, ○ "Cannot find music players" |
| S07 | Share style dialog | Current Playing → "Share Screenshot" | choose A. background only or B. with art + track info | two image buttons with "OR" between | ○ |
| S08 | Save image (`SaveImageActivity`) | after share style | generate the poster / wallpaper image | "Generating, please wait" | ○ |
| S09 | Screen record (`ScreenRecordActivity` + foreground service) | Experimental → "Share Videos" | record a video of the wallpaper with now-playing info | transparent hole view, "recording do not exit", audio source choice: Device Audio / Mic Audio | ○ |
| S10 | Feedback (`FeedbackActivity`) | About → Feedback | contact the author | centred card: Visit Homepage, Write an email, Leave a rating | ○ |
| S11 | Acknowledgement dialog | About → Acknowledgement | credits | libraries list, translators list, "Help Translate" | ○ |
| S12 | Permission dialogs | Requirements buttons | notification access, audio rationale, "Permissions Required" | system settings deep link + rationale dialog (EasyPermissions) | ○ |
| S13 | Full version / free limit dialogs | sync art while on free tier | upsell | "Free Version Limitation", "…Enforcing" with next time, "Get Full Version", restore | ○ (not seen on this device: the account looks unlocked) |
| S14 | Debug / send logs | Debug card (hidden; "Debug Mode … In N Clicks" = tap the version N times) | send logs | notice + "Send Logs" | ○ |

Also present but not a screen: a **Settings Slice provider** (`/settings`)
that exposes settings to system surfaces, and an adaptive launcher colour
scheme (`WallpaperColors` API).

## Flows

```
F01 First setup (the number to beat)
    S01 -> "Set Wallpaper" -> S02 -> "Set wallpaper" (home / lock / both) -> back to S01
        -> "Allow Reading Notification" -> system notification-access settings -> toggle -> back
        -> "Allow Reading Volume (Optional)" -> runtime RECORD_AUDIO prompt -> Allow
    happy path clicks: ~7 (3 buttons, 1 system "set", 1 target choice, 1 toggle + confirm, 1 allow)
    edge: user denies audio (Live Beats checkbox stays disabled, label "Require Volume Permission"),
          notification access never granted (Current Playing says "Allow reading notification first"),
          wallpaper already set (button disappears), OEM kills the listener (FAQ -> Restart Listener)

F02 Wallpaper follows the music (the core loop, zero clicks)
    music app plays -> media notification -> S01 Current Playing fills -> S03 wallpaper re-colours
    edge: player with custom notification style (unsupported), media notification
          that is clearable or not ongoing (ReVanced, Android 16: the original drops it), music paused (fade to min speed;
          keep colour only if "preserve after pause"), reboot (keep only if "preserve after reboot"),
          free tier: one sync per hour

F03 Tune the look
    S01 Configuration: Fluid Settings (scale, speed min-max) / Graphics (Blur | Fluted Glass)
        / Live Beats (strength) / Album Art / Misc (adaptive launcher colours, brightness)
        / Performance Mode / Debug View
    each group is a checkbox that overrides defaults and reveals its controls; long press = tooltip
    happy path clicks: 1-2 per change, applied live to the wallpaper behind

F04 Pick a default palette
    S01 -> "Select Default Color Palette" -> S04 -> tab -> tap item
    happy path clicks: 2-3
    edge: Saved empty; upload custom -> picker -> S05 crop; save current art when nothing plays

F05 Share what's playing
    S01 Current Playing -> "Share Screenshot" -> S07 style A or B -> S08 generate -> Android share sheet
    happy path clicks: 3 + share target

F06 Share a video (experimental)
    S01 -> "Share Videos" -> S09 choose Device / Mic audio -> MediaProjection consent -> record -> share
    edge: streaming apps block device-audio capture

F07 Get help
    S01 FAQ -> "Restart Listener" | "Troubleshoot" (opens web guide)
    S01 About -> S10 Feedback | "Other apps" (store) | S11 Acknowledgement
```

## Components

| component | variants | states | used on |
| --- | --- | --- | --- |
| Header | big app name (Headline3, bold) + overline tagline + hairline | — | S01 |
| Section card | outlined (2dp stroke `colorOnSurface`), transparent fill, no elevation, overline title + hairline divider | collapsed contents via `animateLayoutChanges` | S01 |
| Button | filled (Material3), outlined, with leading icon or leading "—" bullet | default, pressed, hidden once done | everywhere |
| Group toggle | checkbox that enables a settings group and reveals its children | checked, unchecked, disabled (needs permission) | S01 config |
| Checkbox | indented child | checked, unchecked | S01 config |
| Slider | single (scale, strength, brightness −1..1), range (speed min–max 0..1) | default, dragging | S01 config |
| Radio group | Blur / Fluted Glass | selected | S01 config |
| Tooltip | long-press help on each option | — | S01 config |
| Now-playing block | art thumbnail (84–120dp) + 4 text lines + 2 buttons | empty (no permission), empty (nothing playing), filled | S01 |
| Modal card activity | transparent activity, centred card, title + description + content | — | S04, S06, S10 |
| Tabs + pager | Texture / Gradient / Saved | — | S04 |
| Palette tile | image + name card | default, selected, long-press delete | S04 |
| App list row | icon + name + subtitle | — | S06 |
| Dialog | share style (two image choices), acknowledgement, confirm delete, permission rationale, upsell | — | S07, S11, S12, S13 |
| Toast | performance mode on/off, listener restarted, errors | — | global |

Visual language: all text is uppercase overline style with wide tracking, in a
bold geometric sans; monochrome theme (white on black in dark mode, black on
white in light), cards are transparent so the live wallpaper shows through,
Material 3 dynamic colour on buttons and sliders (lavender in the screenshots).

## Inferred data model

No server-side data. Everything is local preferences plus a few images.

```
Settings      fluidOverride bool, fluidScale float, speedMin float, speedMax float,
              graphicsOverride bool, effect (blur | fluted_glass),
              liveBeats bool, beatStrength float,
              albumArtOverride bool, preserveAfterReboot bool, preserveAfterPause bool,
              defaultPaletteId,
              miscOverride bool, adaptiveLauncherColors bool, brightness float (-1..1),
              performanceMode bool, debugView bool
              evidence: S01 config layout + tooltips          confidence: high

NowPlaying    packageName, title, artist, album, artBitmap, isPlaying, updatedAt
              evidence: S01 playing card (4 text lines + art), FAQ    confidence: high (fields: medium)

Palette       id, kind (texture | gradient | saved), name, image (uri or bundled), createdAt
              evidence: S04 tabs, preset names, Saved buttons, delete confirm   confidence: medium

LastSync      lastSyncedAt   (free tier: one sync per hour)
              evidence: free-version strings                    confidence: medium

Entitlement   fullVersion bool   (Play Billing in-app purchase, with restore)
              evidence: IAP strings                             confidence: medium
```

Relationships: Settings 1–1 Palette (default); NowPlaying is transient but its
art can be persisted as the "preserved" image; Palette(saved) n created from
NowPlaying or from an uploaded image.

## Feature matrix

See `features.csv`. Must: 14, should: 12, could: 8, skip: 7.

## Out of scope (cannot or should not be cloned)

- The name "Diffuse", the "Live Beats™" mark, the author's name, the icon and
  the wallpaper thumbnail.
- The tagline "Apple Music like live wallpaper": it uses a third-party
  trademark. Write our own pitch.
- Product Sans: a proprietary Google font. Pick an open one.
- The shaders in `assets/` and the bundled `album.jpg` / `rand.png`: write the
  effects from scratch (domain warping and Kawase blur are public techniques).
- Preset palette images (Aurora, Fire...), the curated playlist art and the
  translations.
- Anti-piracy check (pairip license check) and the "pirate notice": not a
  feature.
- Analytics and crash vendors (Firebase, Sentry, AdServices): pick our own or
  none.

## Size

Screens 14 (1 main, 2 system, 11 small dialogs or modal cards), flows 7,
entities 5.

Hard parts:

1. **A GL fluid renderer that runs inside a `WallpaperService`**, smooth at 60
   fps and easy on the battery: domain-warp fluid + multipass blur, pause when
   not visible, performance mode.
2. **Reliable now-playing**: notification listener / `MediaSessionManager`,
   art extraction, OEM battery killers (hence "Restart Listener").
3. **Live Beats**: real-time beat detection from device output (Visualizer or
   AudioPlaybackCapture), plus the permission UX.

Everything else (settings screen, palettes, sharing) is plain Compose work.
Size: **M** (a few weeks), most of it in the renderer.

## Gaps to fill with the user's own device

S01 "playing" is not needed: on the user's device the original never reaches
that state (see the listener bug above), and the layout already gives its
structure (art + 4 text lines + Open app + Share screenshot).

Screens marked ○ were seen only in the resources. Worth capturing next:
S04 all three tabs, S06, S07, S09, S10, S11, a phone
in portrait and light mode. `S01-home-config-2/3/4.png` are byte-identical:
keep one.
