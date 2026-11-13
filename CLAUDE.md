# CLAUDE.md — working rules for Project MONO

## Product
Standalone monochrome Android launcher + native widget system. Package `com.shoron.mono`.
Target device: Realme 9 SE. Kotlin, Jetpack Compose, Material 3, MVVM, repositories,
DataStore, coroutines/Flow. Read ARCHITECTURE.md and docs/DESIGN.md before changing code.

## Process
- Implement **one phase at a time** (phases listed in README.md). Stop and report after each.
- Builds happen in GitHub Actions only. **Never claim a build or test passed without reading the
  actual workflow run result.** Record results in DEVELOPMENT_LOG.md.
- Update CHANGELOG.md (user-facing) and DEVELOPMENT_LOG.md (engineering notes) with every change.
- Keep existing working features; do not rewrite modules wholesale without approval.

## Hard rules
- No root. No paid or mandatory third-party dependencies. No KWGT or external launcher.
- No hard-coded screen dimensions — use `LayoutMetrics` / window insets.
- No proprietary wallpapers, icons or fonts (see docs/DESIGN.md asset policy).
- No unnecessary permissions; justify every new one in DEVELOPMENT_LOG.md.
- No polling, no always-on services, no continuous animation by default.
- No secrets in the repo. Release signing comes only from GitHub Actions secrets.
- Pure logic lives in Android-free objects (e.g. `PinList`, `AppSearch`, `TimeInWords`)
  and gets JVM unit tests under `app/src/test`.

## Commands
- `./gradlew testDebugUnitTest` — unit tests
- `./gradlew assembleDebug` — debug APK (`app/build/outputs/apk/debug/`)
- Release: push tag `vX.Y.Z` (needs secrets `MONO_KEYSTORE_BASE64`, `MONO_KEYSTORE_PASSWORD`,
  `MONO_KEY_ALIAS`, `MONO_KEY_PASSWORD`).
