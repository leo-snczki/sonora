# Reverse-engineering workspace

This Claude Code comes from the claude-reverse-engineer Nix flake: its
skills, MCP servers and tools are pinned there, and nothing from the user's
~/.claude is loaded. The tools below are
on PATH; don't install others globally (pip, npm -g): add them there
instead.

Skills (from this flake's plugin):

- `reverse-engineer-anything`: reverse engineer binaries, packaged and
  Electron/JS apps, firmware and web apps when the answer depends on the
  shipped artifact rather than source code. The skill gives the workflow;
  the `rea` MCP server gives the tools, so use both together.
- `reverse-api-engineer`: turn a website's private HTTP API into a typed
  client with the `reverse-api-engineer` CLI (installed).
- `replica-*`: clone an app end to end (recon, architecture, design,
  build, backend, tests, parity diff, reviews, rebranding, launch, deploy).
- `android-development`: build Android apps (Kotlin, Jetpack Compose,
  MVVM, Hilt, Room, multi-module) following the NowInAndroid architecture.
- `android-cli:android-cli` (Google's plugin): how to use the `android`
  CLI. It is already on PATH from Nix: skip its install step.
- `mattpocock-skills:*` (separate plugin): engineering workflows
  (diagnosing bugs, TDD, prototyping, research, code review, grilling).

Tools on every platform: `file`, `rizin` with the Ghidra decompiler
(`pdg` decompiles the current function), Frida (`frida`, `frida-trace`),
Node, Python and Google's Android CLI (`android`: SDK, emulators, builds,
`android analyze` for APK/AAB/JAR/AAR; pass `--no-metrics`). Playwright's browsers are at `$PLAYWRIGHT_BROWSERS_PATH`:
install `@playwright/test` at the version matching them and skip
`npx playwright install`.
