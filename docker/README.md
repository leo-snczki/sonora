# Docker

The same Claude as the flake's default (Claude Code, plugin, `rea` MCP
server, tools), run in a Docker container. Claude only sees the current
directory, mounted at `/work`; the rest of the host (your home, other
projects) stays out of reach. Linux only.

`IS_SANDBOX=1` is set, so `--dangerously-skip-permissions` is allowed
inside the container.

Without that need, plain `nix run` (see the [main README](../README.md))
is simpler and faster.

## Run

```sh
nix run github:leo-snczki/claude-reverse-engineer#docker
```

- Needs Docker running (rootless works: root in the container is your
  user, so files in `/work` stay yours).
- The first run builds the image (about 7 GB, mostly Chromium) and loads it
  into Docker; later runs reuse it until the flake changes.
- The container's home persists in
  `~/.local/share/claude-reverse-engineer-docker`. Log in once with
  `/login`, or export `ANTHROPIC_API_KEY`.

## Limits

- The container is Linux: it can analyze Windows and macOS binaries
  statically, but not run or debug them.
- Your own Chrome isn't reachable, so reverse-api-engineer's `chrome-mcp`
  provider can't use your logins: use `--headless` or `auto`.
- On aarch64 Linux the image hasn't been tested yet; Node 26 compiles from
  source there.
