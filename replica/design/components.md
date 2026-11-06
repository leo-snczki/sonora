# Sonora components

Tokens: `tokens.json` (Compose mapping: `compose/SonoraTokens.kt`). Units are
dp / sp. Screen IDs come from `replica/recon.md`.

The system in one paragraph: the settings UI floats over the **live
wallpaper** behind a black page scrim. Cards are flat, outlined in white,
filled with a second scrim. There is one typeface, uppercase only for card
titles and buttons, and one accent. Everything is monochrome, so the music
artwork carries the colour. This is the original's *pattern* (outlined
transparent cards, caps labels, wallpaper behind). The values, font, copy and
icons are Sonora's own.

Changes from the original, on purpose:

| original | Sonora | why |
| --- | --- | --- |
| text straight on the wallpaper, no scrim | page scrim 0.55 + card scrim 0.50 | white text on a pale album art fails contrast (e.g. S02's light grey idle) |
| everything 10sp bold uppercase | caps for card titles and buttons only, 14sp sentence case for reading | legibility |
| "long press each option to see what it does" | supporting text under every option, always visible | long press is undiscoverable and not reachable with TalkBack |
| checkbox that silently overrides defaults | `Switch` with label "Customise" on each group | a switch reads as on/off for a group; a checkbox reads as "agree" |
| Material You lavender | neutral placeholder accent until `/replica-brand` | not ours to keep, and it drifts with the system |

---

```
Backdrop (page)
  what      the live wallpaper (Sonora's own engine rendered behind the activity:
            windowShowWallpaper=true), then a black scrim
  tokens    scrim.page 0.55 (adaptive: lower to scrim.adaptive_min_page 0.30
            when the current art's 95th-percentile luminance allows 4.5:1)
  states    music playing (art), idle (Backdrop), wallpaper not set
            (render a static blur of the Backdrop inside the activity)
  a11y      decorative, not focusable
  used on   S01
```

```
Header
  parts     display "Sonora" + label tagline + divider (white 40%, 1dp)
  tokens    type display / label, text on bg, space 16 gutter, size.header 112
  a11y      app name is a heading (semantics { heading() })
  used on   S01
```

```
SectionCard
  variants  default (outlined, scrim fill), modal (opaque surface-modal, no stroke, radius lg)
  parts     title (label, caps) + divider + content column, 16 padding, 12 between children
  tokens    border 2dp stroke, scrim.card fill, radius md 12, elevation 0
  states    default, collapsed content animates (motion.expand), hidden (whole card
            gone when it has nothing to show, e.g. setup card once all steps are done)
  a11y      title is a heading; card itself not focusable, children are
  used on   S01 (Setup, Now playing, Look, Help, About, Labs), S04, S06, S10 (modal)
```

```
Button
  variants  primary (filled accent), secondary (outlined border-input), text (no container)
            optional leading icon 18 (Lucide); setup steps use a leading status icon
            (circle = to do, check = done)
  size      height 40, horizontal padding 24 (16 with icon), min touch 48 (padding around)
  states    default, pressed (alpha.pressed overlay), focused (2dp accent ring offset 2),
            disabled (alpha.disabled, not clickable), loading (spinner replaces icon,
            label kept), done (setup steps: primary turns into a text button with a check)
  tokens    accent / on-accent, radius pill, type button
  a11y      Role.Button, label stays in the semantics while loading, disabled reason
            given in the supporting text above it
  used on   all
```

```
SettingGroup
  parts     row: label (body-strong) + supporting text (caption, text-muted) + Switch;
            when on, the children appear indented 24 below
  states    off (children hidden), on (children shown, motion.expand),
            locked (switch disabled, supporting text says why: "Needs microphone
            access", plus a secondary button "Allow")
  a11y      whole row is one toggleable (Modifier.toggleable, Role.Switch); state read as on/off
  used on   S01 Look: Motion, Effect, Beat pulse, Album art memory, Display, Advanced
```

```
Switch
  tokens    track on accent / thumb on-accent; track off transparent with track-colour
            outline, thumb track-colour
  states    on, off, disabled, focused (ring)
  used on   SettingGroup, S01 advanced
```

```
Checkbox (child options)
  size      18 box, 48 touch row
  tokens    checked: accent fill + on-accent check; unchecked: track outline 2dp
  states    checked, unchecked, disabled, focused
  a11y      row toggleable, label + supporting text merged
  used on   S01 album art memory (keep after pause / after reboot), adaptive launcher colours
```

```
Slider
  variants  single (scale, strength, brightness -1..1 with a centre tick), range (speed min-max)
  parts     label (body-strong) above, value read-out (caption) right, track full width
  tokens    active track accent, inactive track `track`, thumb accent 20, height 4 track
  states    default, dragging (thumb grows to 24, value bubble), disabled, focused
  a11y      progressBarRangeInfo + stateDescription with a human value ("Calm", "Lively",
            "+20%"); range slider = two focusable thumbs, named "Minimum speed" / "Maximum speed";
            volume keys / D-pad change by 5%
  used on   S01 Look
```

```
SegmentedChoice
  what      replaces the original's radio pair (Blur | Fluted glass)
  parts     2-3 segments, equal width, label + optional icon
  tokens    selected accent fill / on-accent; unselected outline border-input
  states    selected, unselected, focused, disabled
  a11y      selectableGroup, each Role.RadioButton
  used on   S01 Effect
```

```
NowPlayingCard
  states    no-access ("Turn on notification access so Sonora can see what is playing"
            + primary "Allow"), idle ("Nothing playing" + secondary "Open a music app"),
            playing (art + title / artist / album / source app + secondary "Open player"
            + text "Share"), unsupported hint after 2 min of access without sessions
            ("Not detecting your player?" + text "Restart listener")
  parts     art thumbnail 84-120 square, radius sm, crossfade on change
  tokens    title body-strong, rest body / caption text-muted
  a11y      art has contentDescription "Album art for {album}"; the block merges into one
            node "Now playing: {title} by {artist}"
  used on   S01
```

```
SetupStep
  parts     status icon + label + Button (or "Done" text once complete)
  states    to-do, done, optional (label suffix "optional"), blocked (system denies:
            "Open settings")
  a11y      list semantics, "Step 2 of 3, done"
  used on   S01 Setup
```

```
Tabs + Pager
  parts     2 tabs (Built-in, Mine), HorizontalPager
  tokens    selected label text + 2dp accent indicator, unselected text-muted
  a11y      Role.Tab, swipe and tap both work
  used on   S04
```

```
BackdropTile
  parts     square image (radius md), name caption below, check badge when it is the default
  states    default, selected (2dp accent ring + check), pressed, long-press menu on
            "Mine" (Delete), loading (shimmer while decoding)
  a11y      "Aurora, selected"; delete also reachable through a custom accessibility action
  used on   S04
```

```
EmptyState
  parts     icon 32 + one-line title + body + action
  used on   S04 Mine ("No saved Backdrops yet" + Save current art / Upload image),
            S06 ("No music apps found" + Open Play Store search)
```

```
AppListRow
  parts     app icon 40 + name body-strong + package caption, 64 tall
  states    default, pressed
  a11y      Role.Button "Open {app}"
  used on   S06
```

```
Dialog
  variants  confirm (title + body + 2 text buttons), choice (two big image options,
            S07 share style), content (scrolling text, S11 credits)
  tokens    surface-modal, radius lg 28, padding 24, title type title
  states    open, dismissing; scrim behind = black 0.6
  a11y      focus trapped, Back dismisses, first action focused
  used on   S04 delete, S07, S11, S12 permission rationale
```

```
Cropper
  parts     image + square crop frame, rotate left / right, flip, Cancel / Use
  a11y      rotate and flip are labelled icon buttons; pinch alternatives: zoom +/-
  used on   S05
```

```
Snackbar (replaces the original's toasts)
  tokens    surface-modal, text, action in accent
  used on   performance mode on/off, listener restarted, errors
```

```
DebugOverlay (on the wallpaper)
  parts     fps, frame time, beat meter bar, current effect, mono type
  tokens    mono, text on black 0.6 box
  used on   S03 when Advanced > Show debug info
```

Icons: Lucide (ISC), imported as vectors: `circle`, `check`, `music`,
`share-2`, `rotate-ccw`, `rotate-cw`, `flip-horizontal`, `message-square`,
`info`, `external-link`, `trash-2`, `image-plus`. None of the original's
drawables are used.

## Primitives build (step 4)

There is no Gradle project yet, so the primitives cannot be built and
screenshotted today. The first task of `/replica-build` is to:

1. Scaffold `:core:designsystem`, copy `compose/SonoraTokens.kt` in, bundle
   Manrope and JetBrains Mono under `res/font`.
2. Build every component above in `:core:designsystem`, with one `@Preview` per
   state, plus a `DesignCatalogActivity` (debug build only) over a white and a
   black wallpaper.
3. Screenshot the catalog over the **white** wallpaper (the worst case for
   contrast) into `replica/design/catalog.png`.
