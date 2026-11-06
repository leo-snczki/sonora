
# Platform: macOS

- Mach-O: `otool`, `lipo`, `install_name_tool`, `nm` (cctools),
  `llvm-objdump`, `llvm-nm`.
- Objective-C and Swift: `ipsw class-dump <binary>` (Objective-C headers),
  `ipsw swift-dump <binary>`, `ipsw macho info <binary>` (load commands,
  signature, entitlements). `ipsw` also reads the dyld shared cache.
- Decompile: `rizin -A <binary>`, then `pdg @ <function>`.
- Debugger: `lldb` (gdb can't debug on Apple Silicon). It uses the
  debugserver from Xcode or the Command Line Tools; if it can't attach,
  try `/usr/bin/lldb`. Attaching to other processes needs Developer Mode
  (`DevToolsSecurity -enable`), and apps with the hardened runtime can't be
  attached or injected (lldb, Frida) unless re-signed with
  `get-task-allow` or with SIP off: ask the user before either.
- Compile: `clang` for C, C++ and Objective-C, `swiftc` and `swift build`
  for Swift (all from Nix). Xcode projects (`xcodebuild`) need the system
  Xcode, which can't be in Nix. Signing: the system `codesign`.
- Browsers: there is no Chromium in Nix for macOS. `$CHROME_PATH` points to
  Google Chrome when it is installed.
