# Security policy

## Supported versions

Only the latest release gets security fixes. Please update to the [latest release](https://github.com/leo-snczki/sonora/releases/latest) before reporting.

| Version | Supported |
|---|---|
| Latest release | ✅ |
| Older releases | ❌ |

## Reporting a vulnerability

**Please don't open a public issue for security problems.**

Report them privately through GitHub instead:

1. Go to the [Security tab](https://github.com/leo-snczki/sonora/security) of this repository.
2. Click **Report a vulnerability**.
3. Describe the problem, the affected version, the device and Android version, and the steps to reproduce it.

Only the maintainer can see the report. You can expect:

- an acknowledgement within **7 days**;
- an assessment and, if confirmed, a fix plan within **30 days**;
- credit in the release notes, unless you prefer to stay anonymous.

## Scope

Sonora runs entirely on the device and has no internet permission. Issues of particular interest include:

- reading more from notifications or media sessions than the Album Art, title, artist and play state;
- Beat Pulse capturing anything other than the device's audio output, or keeping audio data;
- other apps abusing Sonora's exported services (the wallpaper and the notification listener);
- saved Backdrops or settings being readable by other apps;
- leaks or tampering in the release build and its signing pipeline.
