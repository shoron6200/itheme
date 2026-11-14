# MONO — Ocean Edition

A standalone, monochrome Android launcher with its own native widget system.
Package `com.shoron.mono` · Kotlin · Jetpack Compose · Material 3 · no root · no KWGT.

Target device: Realme 9 SE (works on any Android 8.0+ phone).

## Status

Phase 3 — **native widget engine + MONO lock screen** (see [CHANGELOG.md](CHANGELOG.md) and
[DEVELOPMENT_LOG.md](DEVELOPMENT_LOG.md)).

| Phase | Scope | State |
|---|---|---|
| 1 | Launcher foundation: HOME activity, app list, drawer, pins, settings, CI | built in CI; awaiting device test |
| 2 | Ocean visual design, wallpaper engine, typography | built in CI; awaiting device test |
| 3 | Radial/Word clocks, date, battery, weather (Open-Meteo), MONO lock screen | built in CI; awaiting device test |
| 4 | Weather refinements (location options, alerts) | planned |
| 5 | Thin Line icon engine (20 original icons, manifest, CI validation) | built in CI; awaiting device test |
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
- **Tap** the clock → alarms, the date/month → calendar, battery → battery usage, weather → refresh.
- **Settings → Clock & widgets**: Radial digital / Editorial words, 24-hour, seconds, battery,
  weather location search (Open-Meteo), °F, 3-day forecast.
- **Settings → Lock screen**: enable the MONO lock screen, position, contrast, preview, and
  "use this wallpaper on Android's lock screen". See the lock screen notes below.
- **Settings → Wallpaper**: pick Shoreline / Undertow / Swell or your own photo, drag to
  reposition, adjust dim and monochrome, then Apply.
- **Back / Home** in the drawer: return to home.

## Lock screen: what is and isn't possible

Android does not allow any app (without root) to replace the secure lock screen or the
always-on display. MONO therefore:

- shows its own lock screen **on top of** Android's keyguard when the screen turns off
  (`showWhenLocked`), with live time, seconds, vertical date, weekday and weather;
- hands every unlock to Android: swipe up → the system PIN / pattern / password / biometric
  prompt (`requestDismissKeyguard`). Fingerprint unlock closes MONO's screen automatically;
- never disables the keyguard, never reads notifications, and stays out of the way during calls;
- can optionally set the selected wallpaper as Android's own lock-screen wallpaper;
- shows a media card while music/video plays (needs notification access, which Android
  requires for seeing other apps' media sessions; MONO does not read notifications).

Limits: to appear after *other* apps on Android 10+ it needs "Display over other apps"
(user-granted, optional). It relies on MONO's process being alive (normally true for the Home
app; no foreground service is used). Notifications, emergency call and camera shortcuts remain
on Android's lock screen, one swipe away. Some OEM builds may still show their own lock screen
clock behind the bouncer.

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
