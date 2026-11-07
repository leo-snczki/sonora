# Optional: the flake's Claude in a Docker container, for machines where you
# want the host kept out of reach. Run it with `nix run .#docker`.
{ pkgs, lib, name, mkClaude, tools }:

let
  # The flake's CLAUDE.md plus notes on the container.
  claude = mkClaude [ ./CLAUDE.md ];

  contents = with pkgs; [
    claude
    dockerTools.fakeNss dockerTools.usrBinEnv dockerTools.binSh
    bashInteractive coreutils findutils gnugrep gnused gawk diffutils
    less procps which gnutar gzip unzip curl cacert git jq ripgrep
  ] ++ tools;

  # Root in a rootless Docker container maps to the host user, so files
  # written to /work stay yours.
  image = pkgs.dockerTools.streamLayeredImage {
    inherit name contents;
    extraCommands = ''
      mkdir -p -m 1777 tmp
      mkdir -p root work
    '';
    config = {
      Entrypoint = [ "${claude}/bin/claude" ];
      WorkingDir = "/work";
      Env = [
        "HOME=/root"
        "SSL_CERT_FILE=${pkgs.cacert}/etc/ssl/certs/ca-bundle.crt"
        "NODE_EXTRA_CA_CERTS=${pkgs.cacert}/etc/ssl/certs/ca-bundle.crt"
        # Claude Code refuses --dangerously-skip-permissions as root unless
        # it knows it is sandboxed.
        "IS_SANDBOX=1"
      ];
    };
  };
in

# Run Claude in the container on the current directory. The image is loaded
# once per build (its tag is the Nix hash). The container's home (Claude's
# config and login, npm cache) persists in $XDG_DATA_HOME/${name}-docker.
pkgs.writeShellScriptBin "${name}-docker" ''
  set -euo pipefail
  docker=${pkgs.docker-client}/bin/docker
  ref="${image.imageName}:${image.imageTag}"
  if ! $docker image inspect "$ref" >/dev/null 2>&1; then
    ${image} | $docker load
  fi
  home="''${XDG_DATA_HOME:-$HOME/.local/share}/${name}-docker"
  mkdir -p "$home"
  exec $docker run --rm -it \
    --hostname ${name} \
    -v "$PWD:/work" \
    -v "$home:/root" \
    ''${ANTHROPIC_API_KEY:+-e ANTHROPIC_API_KEY} \
    --shm-size 1g \
    --cap-add SYS_PTRACE \
    "$ref" "$@"
''
