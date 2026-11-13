# Development log

Engineering notes, decisions and **verified** build results. Newest first.

## 2026-11-13 — Phase 1: launcher foundation

### Starting point
- Repository `shoron6200/itheme` was empty (no commits). Project created from scratch.
- Design reference: user video of a black & white ocean-wave Android setup (see docs/DESIGN.md).
  No reference assets are used.
- The development container has no Android SDK and cannot reach Google's SDK servers,
  so **all compilation happens in GitHub Actions**.

### Decisions
- Single `:app` module, package-per-area. Manual DI (`AppContainer`) instead of Hilt.
- Toolchain pinned to known-stable versions: AGP 8.7.3, Gradle 8.11.1, Kotlin 2.1.0,
  Compose BOM 2024.12.01, compileSdk/targetSdk 35, minSdk 26, JDK 17.
- `AppEntry` is Android-free so search, sections, pins and defaults are JVM-tested.
- App list driven by `LauncherApps.Callback`; clock by `TIME_TICK`; no polling anywhere.
- Uninstall action intentionally omitted (would need `REQUEST_DELETE_PACKAGES`);
  users uninstall from App info.
- Phase 1 icons are the apps' own icons desaturated; Thin Line icons arrive in Phase 5.

### Permissions
- None requested. `<queries>` limited to launcher + default-app intents.

### Build results (from GitHub Actions)
- Run #1 (`e72ebe4`, run id 43462907486) — **failed**: `compileDebugKotlin`, 3 errors in
  `HomeScreen.kt` (`maxHeight` read inside a nested `Column` scope).
- Run #2 (`db40f7b`, run id 43463651620) — **success**: `testDebugUnitTest` passed,
  `assembleDebug` passed, `lintDebug` completed (report-only), artifact `mono-debug-2`
  (9.8 MB zip) uploaded. One compiler warning: deprecated `TRIM_MEMORY_MODERATE` constant.
- CI notice: GitHub forces the v4 actions onto Node 24 (Node 20 deprecation warning); to be
  addressed by bumping action majors in a later phase.
- **Not yet verified on a physical device.** On-device checks for Phase 1: set as Home, launch
  apps, search, pin/unpin, reorder in settings, install/uninstall an app while MONO is running.
