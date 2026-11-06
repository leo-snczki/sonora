{
  description = "Claude Code for reverse engineering (REA, replica, reverse-api-engineer), pinned and isolated with Nix";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/nixos-unstable";

    # Claude Code itself, pinned by flake.lock.
    claude-code = {
      url = "github:sadjow/claude-code-nix";
      inputs.nixpkgs.follows = "nixpkgs";
    };

    # agent-browser (one of reverse-api-engineer's browser providers).
    llm-agents.url = "github:numtide/llm-agents.nix";

    # REA skill source; pinned to a release so it matches the rea-agents MCP server.
    rea = {
      url = "github:morluto/rea/rea-agents-6.8.0";
      flake = false;
    };

    # replica-* app-cloning skills (no release tags).
    replica-skill = {
      url = "github:Jakeschincariol/replica-skill";
      flake = false;
    };

    # reverse-api-engineer CLI and skill. The skill is newer than the last
    # release (v0.14.0), so follow main; flake.lock pins the commit.
    reverse-api-engineer = {
      url = "github:kalil0321/reverse-api-engineer";
      flake = false;
    };

    # Android app development skill (Kotlin, Compose, NowInAndroid
    # architecture). No release tags; flake.lock pins the commit.
    android-skill = {
      url = "github:dpconde/claude-android-skill";
      flake = false;
    };

    # Google's Claude plugin for the Android CLI (the `android-cli` skill).
    # No release tags; flake.lock pins the commit. Keep it in step with the
    # android-cli package from nixpkgs.
    android-cli-plugin = {
      url = "github:android/android-cli-plugin";
      flake = false;
    };

    # Matt Pocock's engineering skills (TDD, diagnosing bugs, code review, ...),
    # loaded as their own plugin.
    mattpocock-skills = {
      url = "github:mattpocock/skills/v1.3.2";
      flake = false;
    };
  };

  outputs = { self, nixpkgs, claude-code, llm-agents, rea, replica-skill, reverse-api-engineer, android-skill, android-cli-plugin, mattpocock-skills }:
    let
      lib = nixpkgs.lib;
      systems = [ "x86_64-linux" "aarch64-linux" "aarch64-darwin" ];
      forAllSystems = f: lib.genAttrs systems (system: f (import nixpkgs {
        inherit system;
        overlays = [ claude-code.overlays.default ];
        config.allowUnfreePredicate = pkg: lib.elem (lib.getName pkg) [ "claude-code" "android-cli" ];
      }));

      name = "claude-reverse-engineer";
    in
    {
      packages = forAllSystems (pkgs:
        let
          py = pkgs.python3Packages;
          agent-browser = llm-agents.packages.${pkgs.stdenv.hostPlatform.system}.agent-browser;

          # Not in nixpkgs. Its version floors run a little ahead of nixpkgs,
          # so relax them.
          rae = py.buildPythonApplication {
            pname = "reverse-api-engineer";
            version = (lib.importTOML "${reverse-api-engineer}/pyproject.toml").project.version;
            pyproject = true;
            src = reverse-api-engineer;
            build-system = [ py.hatchling ];
            pythonRelaxDeps = true;
            dependencies = with py; [
              click claude-agent-sdk rich markdown-it-py prompt-toolkit questionary
              watchdog setproctitle requests aiohttp pygments cryptography httpx2
              anyio certifi multidict opentelemetry-api pyjwt python-multipart
              starlette urllib3
              # The `manual` extra: capture with a local Playwright browser.
              playwright playwright-stealth
            ];
            doCheck = false;
          };

          # rea-agents (the MCP server) is not in nixpkgs; npx runs the release
          # that matches the pinned skill source.
          reaVersion = (lib.importJSON "${rea}/package.json").version;
          rea-mcp = pkgs.writeShellScriptBin "rea-mcp" ''
            # rea's scripts use `#!/usr/bin/env node`, so node must be on PATH.
            export PATH=${pkgs.nodejs_26}/bin:$PATH
            exec npx -y rea-agents@${reaVersion} mcp "$@"
          '';

          # One plugin with every skill, copied in as a real directory tree.
          plugin = pkgs.runCommand "${name}-plugin" { } ''
            mkdir -p $out/.claude-plugin $out/skills
            echo '{"name": "${name}", "description": "Reverse-engineering skills"}' > $out/.claude-plugin/plugin.json
            cp -r ${rea}/.agents/skills/reverse-engineer-anything $out/skills/
            cp -r ${reverse-api-engineer}/skills/reverse-api-engineer $out/skills/
            cp -r ${android-skill} $out/skills/android-development
            for skill in ${replica-skill}/*/SKILL.md; do
              cp -r "$(dirname "$skill")" $out/skills/
            done
          '';

          mcpConfig = pkgs.writeText "mcp.json" (builtins.toJSON {
            mcpServers.rea = {
              type = "stdio";
              command = "${rea-mcp}/bin/rea-mcp";
            };
          });

          inherit (pkgs.stdenv.hostPlatform) isDarwin;

          # What the skills and MCP servers call, on Claude's PATH. REA drives
          # the native analysis tools it finds.
          tools = with pkgs; [
            rae
            rea-mcp
            agent-browser
            nodejs_26
            python3
            file
            # rizin with the Ghidra decompiler (`pdg`)
            (rizin.withPlugins (ps: [ ps.rz-ghidra ]))
            frida-tools
            # Google's Android CLI (`android`): SDK, emulators, builds
            android-cli
          ] ++ (if isDarwin then [
            # Mach-O: otool, lipo, install_name_tool; llvm-objdump, llvm-nm
            cctools
            llvm
            # Objective-C and Swift metadata (class-dump, swift-dump), Mach-O,
            # dyld shared cache
            ipsw
            # gdb can't debug on Apple Silicon
            lldb
            # Compile C, C++ and Objective-C
            clang
            # Compile Swift (swiftc) and Swift packages (swift build)
            swift
            swiftpm
          ] else [
            chromium
            binutils
            gdb
          ]);

          # Notes for Claude on this platform's tools, after the shared ones.
          platformNotes = if isDarwin then ./platforms/darwin.md else ./platforms/linux.md;

          # Claude Code with only this flake's CLAUDE.md, plugin, MCP servers
          # and settings. Its own config dir keeps ~/.claude (global CLAUDE.md,
          # skills, MCP servers, hooks) out, and holds a separate login.
          # extraNotes are appended to CLAUDE.md (the Docker image adds its own).
          mkClaude = extraNotes: pkgs.writeShellScriptBin "claude" ''
            export CLAUDE_CONFIG_DIR="''${XDG_DATA_HOME:-$HOME/.local/share}/${name}"
            mkdir -p "$CLAUDE_CONFIG_DIR"
            ln -sfn ${pkgs.concatText "CLAUDE.md" ([ ./CLAUDE.md platformNotes ] ++ extraNotes)} "$CLAUDE_CONFIG_DIR/CLAUDE.md"
            export PATH=${lib.makeBinPath tools}:$PATH
            export PLAYWRIGHT_BROWSERS_PATH=${pkgs.playwright-driver.browsers-chromium}
            export PLAYWRIGHT_SKIP_VALIDATE_HOST_REQUIREMENTS=true
            ${if isDarwin then ''
              # No Chromium in nixpkgs for macOS: use Google Chrome when installed.
              chrome="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
              [ -x "$chrome" ] && export CHROME_PATH="$chrome"
            '' else ''
              export CHROME_PATH=${lib.getExe pkgs.chromium}
            ''}
            exec ${pkgs.claude-code}/bin/claude \
              --plugin-dir ${plugin} \
              --plugin-dir ${mattpocock-skills} \
              --plugin-dir ${android-cli-plugin}/plugins/android-cli-claude \
              --strict-mcp-config --mcp-config ${mcpConfig} \
              --settings ${./settings.json} \
              "$@"
          '';

          claude = mkClaude [ ];
        in
        {
          inherit claude plugin rae rea-mcp;
          default = claude;
        }
        # Optional: the same Claude in a Docker container (docker/), Linux only.
        // lib.optionalAttrs (!isDarwin) {
          docker = import ./docker { inherit pkgs lib name mkClaude tools; };
        });

      devShells = forAllSystems (pkgs: {
        default = pkgs.mkShell {
          packages = [ self.packages.${pkgs.stdenv.hostPlatform.system}.claude ];
        };
      });

      apps = forAllSystems (pkgs:
        let packages = self.packages.${pkgs.stdenv.hostPlatform.system}; in
        {
          default = {
            type = "app";
            program = "${packages.claude}/bin/claude";
          };
        }
        // lib.optionalAttrs (packages ? docker) {
          docker = {
            type = "app";
            program = "${packages.docker}/bin/${name}-docker";
          };
        });
    };
}
