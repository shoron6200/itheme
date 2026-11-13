# MONO — Ocean Edition

A standalone, monochrome Android launcher with its own native widget system.
Package `com.shoron.mono` · Kotlin · Jetpack Compose · Material 3 · no root · no KWGT.

Target device: Realme 9 SE (works on any Android 8.0+ phone).

## Status

Phase 2 — **ocean visual design** (see [CHANGELOG.md](CHANGELOG.md) and
[DEVELOPMENT_LOG.md](DEVELOPMENT_LOG.md)).

| Phase | Scope | State |
|---|---|---|
| 1 | Launcher foundation: HOME activity, app list, drawer, pins, settings, CI | built in CI; awaiting device test |
| 2 | Ocean visual design, wallpaper engine, typography | built in CI; awaiting device test |
| 3 | Clock (incl. radial dial), date and battery widgets | planned |
| 4 | Weather engine (Open-Meteo, cached) | planned |
| 5 | Thin Line icon engine | planned |
| 6 | Gestures and interactions | planned |
| 7 | Personalization | planned |
| 8 | Performance and device testing | planned |
| 9 | Signed release | planned |

## Install the debug APK (no Android Studio needed)

1. Open the repository's **Actions** tab → **Android debug build** → the latest green run.
2. Download the `mono-debug-<n>` artifact (a .zip) and extract `app-debug.apk`.
3. On the phone, allow your browser/file manager to *install unknown apps* and open the APK.
4. Press **Home** and choose **MONO dev**, or open MONO → long-press the home screen →
   **MONO settings** → **Set MONO as Home**.
   On Realme UI you can also go to *Settings → App management → Default apps → Home app*.

Debug builds use the id `com.shoron.mono.debug`, so they install next to future release builds.

## Using MONO

- **Swipe up** on home: app drawer (search, A–Z rail).
- **Long-press an app**: pin/unpin, app info.
- **Long-press empty home space**: MONO settings (wallpaper, reorder or remove home apps).
- **Settings → Wallpaper**: pick Shoreline / Undertow / Swell or your own photo, drag to
  reposition, adjust dim and monochrome, then Apply.
- **Back / Home** in the drawer: return to home.

## Building

Builds run in GitHub Actions; see [.github/workflows](.github/workflows).

- Every push to `main` (and to `claude/**` work branches) and every pull request runs unit tests
  and produces a debug APK artifact.
- Pushing a tag `vX.Y.Z` produces a signed release APK attached to a GitHub Release
  (requires the four `MONO_*` repository secrets — set up in Phase 9).

Local builds work too with JDK 17 + Android SDK 35: `./gradlew testDebugUnitTest assembleDebug`.

## Documentation

- [ARCHITECTURE.md](ARCHITECTURE.md) — modules, data flow, performance rules
- [docs/DESIGN.md](docs/DESIGN.md) — visual direction and asset policy
- [CLAUDE.md](CLAUDE.md) — rules for AI-assisted development in this repo
