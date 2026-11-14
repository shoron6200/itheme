# Development log

Engineering notes, decisions and **verified** build results. Newest first.

## 2026-11-14 — Fingerprint unlock, camera band, drawer threshold

- **Fingerprint root cause**: `ACTION_USER_PRESENT` is broadcast by SystemUI, which runs as an
  app uid. Registered with `RECEIVER_NOT_EXPORTED` (Android 13+), the receiver never got it, so
  after a fingerprint unlock the MONO lock screen stayed up. Now `RECEIVER_EXPORTED` (safe: it
  is a protected broadcast). Safety net: while the lock screen is RESUMED, check
  `isKeyguardLocked` every 250 ms and `goHome()` once unlocked; stops on pause/screen off.
- **Top band**: `CutoutMath.band()` classifies top cutouts into band top/height and left/right
  obstacles (left third / right third). Home `TopRow` is one line centred in that band; the date
  is shifted to start 14dp after a left punch-hole; battery shifted before a right one.
  Previously the date was pushed *below* the camera, wasting the band.
- **Drawer**: drag maps half a screen of travel to fully open; opens past 9 % (~50dp) or on an
  upward release > 350 px/s; drag cancel (system navigation gesture pilfering the pointer)
  snaps closed. The bottom system gesture zone stays Android's — it cannot be overridden.

### Build results
- _Pending GitHub Actions run._

## 2026-11-14 — Unlock fix, status bar, rail animation, Phase 5

### Unlock → home fix
- Cause: on unlock the MONO lock screen simply `finish()`ed (or stayed up after a fingerprint
  unlock until resumed), returning to whatever task was underneath — sometimes the system
  keyguard/another screen needing a second swipe.
- Now every success path (`onDismissSucceeded`, `ACTION_USER_PRESENT`, resume while already
  unlocked) calls `goHome()`: starts MONO's HOME intent (package-scoped, no animation) and then
  finishes, guarded by a `leaving` flag. Bouncer cancel keeps the MONO lock screen; a dismiss
  error reveals Android's lock screen (still secure). `LockScreenActivity.isVisible` stops the
  controller from relaunching it when the screen times out on it.

### Status bar / insets
- Home hides `statusBars()` (transient by swipe), re-applied on resume/focus. Top content uses
  `rememberTopCutout()` (display-cutout bounding rects per screen half) instead of the
  status-bar inset; horizontal + bottom still use `safeDrawing`.
- Lock battery moved outside the layout box (clock/date box unchanged), top padding from cutout.

### Phase 5
- Drawer progress is an `Animatable` driven by home drag deltas + `VelocityTracker`; visual
  motion in `graphicsLayer`, visibility via `derivedStateOf` (no per-frame recomposition).
- Edit mode reorder: local working list while dragging, row swap when the offset crosses a
  row height, optimistic `pendingOrder` until DataStore emits.
- Recents: MRU of launches through MONO (`RecentList`), stored in DataStore. Usage-stats access
  intentionally not requested.
- Swipe-down "Notification shade" uses `StatusBarManager.expandNotificationsPanel` via
  reflection (EXPAND_STATUS_BAR, normal permission); falls back to the quick panel if blocked.
- Rail: per-letter `graphicsLayer` reading finger Y + intensity `Animatable`; `RailMath` tested.

### Build results (from GitHub Actions)
- Run #10 (`9e3df64`, run id 43558862542) — **success** on first attempt: `testDebugUnitTest`
  (incl. new `HomeLayoutTest`, `RailMathTest`) passed, `assembleDebug` passed, `lintDebug`
  completed (report-only), artifact `mono-debug-10` (11.3 MB zip) uploaded.
- **Not yet verified on a physical device** (lock → authenticate → home flow, gestures,
  edit-mode reorder, rail animation need on-device checks).

## 2026-11-13 — Lock screen immersive, media card, zigzag rail

- Reference images for this request did not arrive with the message; implemented against the
  existing design and the drawer rail seen in the original reference video.
- **Immersive lock**: `systemBars()` hidden with transient-by-swipe behaviour; cutout mode
  ALWAYS/SHORT_EDGES. Layout box now uses `statusBarsIgnoringVisibility ∪ displayCutout`,
  which equals the previous `safeDrawing` area (status bar visible, nav bar hidden), so the
  clock/date positions are unchanged.
- **Battery** reuses `BatteryIndicator` (new optional status label) and the engine's battery flow.
- **Media**: `MediaSessionManager.getActiveSessions` needs an enabled NotificationListenerService
  (Android rule for non-system apps). `MediaAccessService` is an empty listener — no notification
  reading, no playback service. `MediaSessionRepository.media` is a cold flow collected only while
  the lock screen is visible; it prefers a playing session, then a paused one, and drops
  stopped/none/error sessions. Progress is extrapolated from `PlaybackState` and ticks every
  500 ms only while playing and STARTED. Artwork is downscaled to ≤256 px and cached per source.
- **Zigzag rail**: equal slots, alternating ±amplitude offset, tap = `animateScrollToItem`,
  drag = `scrollToItem`; current section from `firstVisibleItemIndex` via `derivedStateOf`.
- No new dependencies. New manifest entry: notification listener service (user-enabled).

### Build results (from GitHub Actions)
- Run #8 (`38f54d2`, run id 43514489468) — **success** on first attempt: `testDebugUnitTest`
  (incl. new `MediaInfoTest`) passed, `assembleDebug` passed, `lintDebug` completed
  (report-only), artifact `mono-debug-8` (11.2 MB zip) uploaded.
- **Not yet verified on a physical device.**

## 2026-11-13 — Phase 3: widget engine + lock screen

### Lock screen investigation (no root)
- Third-party apps cannot replace Android's keyguard or AOD. Device-admin keyguard APIs only
  *disable* features; `KeyguardManager.disableKeyguard` is deprecated and would weaken security.
- Chosen: an activity with `showWhenLocked` displayed over the locked keyguard, started on
  `ACTION_SCREEN_OFF` by a receiver in MONO's process. Unlock is always
  `requestDismissKeyguard` → system bouncer (PIN/pattern/password/biometric).
  `ACTION_USER_PRESENT` and an `isKeyguardLocked` check on resume close it after any unlock.
- Not shown during calls (AudioManager mode check, no phone permission).
- Background-activity-start rules: on Android 10+ the launch from SCREEN_OFF is only allowed
  when MONO is in the foreground or the user granted "Display over other apps"
  (`SYSTEM_ALERT_WINDOW`, optional). Android 15+ tightens this further for apps targeting 35+;
  the Realme 9 SE is not expected to receive Android 15, but it is a known limit.
- No foreground service: if the OS kills MONO's process, the glance is skipped until MONO runs.
- Optional `SET_WALLPAPER` sync renders the current wallpaper (crop, dim, greyscale) onto the
  system lock screen so the secure bouncer matches.

### Widget decisions
- Single `WidgetEngine` feeds home and lock screen (no duplicated widget logic or settings).
- Defaults per request: Radial Digital, 24-hour, seconds on.
- Seconds: cold per-second flow, only while visible and enabled; consumers read it in draw
  phase / a leaf composable. Minute data stays on TIME_TICK (time-zone changes handled there,
  and `LocalTime.now()` per tick picks up zone changes immediately).
- Weather: Open-Meteo forecast + geocoding, `HttpURLConnection` + platform `org.json`
  (JVM tests use the `org.json:json` artifact). WorkManager 2.9.1 hourly job with constraints.
- New dependencies: `androidx.work:work-runtime-ktx:2.9.1`; test-only `org.json:json:20240303`.

### Wallpaper
- Owner-supplied ocean photo added as `builtin:ocean` and made the default; existing wallpaper
  engine untouched apart from a `decodeSoftware()` helper used by lock-wallpaper sync.

### Permissions
- Added `INTERNET`, `SET_WALLPAPER` (both normal), `SYSTEM_ALERT_WINDOW` (special, optional,
  user-granted). Justified above.

### Build results (from GitHub Actions)
- Run #6 (`dee3f5a`, run id 43508200155) — **success** on first attempt: `testDebugUnitTest`
  (incl. new ClockFormatter, DialGeometry, BatteryState and OpenMeteo tests) passed,
  `assembleDebug` passed, `lintDebug` completed (report-only), artifact `mono-debug-6`
  (11.2 MB zip) uploaded.
- **Not yet verified on a physical device.** Device checks: radial clock seconds/rotation,
  24h toggle, weather city search + display, battery charging bolt, lock screen after screen
  off (with and without "Display over other apps"), swipe-up → system PIN/biometric,
  fingerprint unlock closes MONO lock screen, no lock screen during calls, lock wallpaper sync.

## 2026-11-13 — Phase 2: ocean visual design

### Decisions
- **Own wallpaper rendering** instead of the system wallpaper: no `SET_WALLPAPER` permission,
  full control over focal point / dim / greyscale, and the editorial layout can rely on it.
  Cost: one display-sized hardware bitmap (GPU memory), decoded once per source and size.
- **Artwork**: original, procedural, reproducible (`tools/wallpapers/generate.py`, fixed seeds).
  The reference pack's wallpapers are proprietary and are not used or traced.
- **Fonts**: Outfit and Sacramento from the Google Fonts catalogue (SIL OFL 1.1), obtained via
  the `@expo-google-fonts` npm packages (TTF) with license texts from `@fontsource`.
  Shipped **unmodified** — Sacramento has a Reserved Font Name, so subsetting would require a
  rename. ~390 KB total.
- **Photo import** copies a downscaled WebP into `filesDir/wallpapers/custom_<version>.webp`;
  old imports are deleted on apply. Avoids persistable URI grants, which some OEM pickers lack.
- `ImageDecoder` (API 28+) for EXIF-correct, size-targeted decoding; `BitmapFactory` with
  power-of-two sampling on API 26–27 (no EXIF rotation there — documented limitation).
- Settings navigation is a two-state enum + `BackHandler`; no navigation library needed yet.

### Permissions
- None added. The Android photo picker (`PickVisualMedia`) needs no permission.

### Build results (from GitHub Actions)
- Run #4 (`b67f655`, run id 43500820551) — **success** on first attempt: `testDebugUnitTest`
  (incl. new `WallpaperMathTest`) passed, `assembleDebug` passed, `lintDebug` completed
  (report-only), artifact `mono-debug-4` (10.4 MB zip) uploaded.
- **Not yet verified on a physical device.** On-device checks for Phase 2: wallpaper appears
  behind home, drawer translucency, picking each built-in, importing a camera photo (orientation),
  drag-to-reposition matches home, dim/monochrome, fonts render (month, script weekday).

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
