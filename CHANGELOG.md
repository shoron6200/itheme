# Changelog

All notable user-facing changes. Format: [Keep a Changelog](https://keepachangelog.com/).

## [Unreleased]

### Fixed — follow-up
- Fingerprint unlock now closes the MONO lock screen and opens MONO home automatically.
- Home top row (date · battery) sits in the camera band like a status bar: on the same line as
  the punch-hole camera, starting after it with clear space; battery aligned on the right.
  Lock-screen battery uses the same band.
- A short, light upward swipe now opens the app drawer (≈50dp or a gentle flick).

### Fixed
- After unlocking from the MONO lock screen (PIN/password/pattern, fingerprint or face), MONO
  now goes straight to its home screen — no second swipe. The lock screen no longer re-opens
  on itself when the screen times out while it is showing.

### Added — Phase 5: interactions & motion
- Home screen hides the system status bar (swipe from the top edge reveals it); date and battery
  move up to the top edge, below the camera cutout only on its side.
- Lock screen battery moved up to the top edge; clock/date layout unchanged.
- App drawer follows the finger when swiping up, settles with a spring; home fades/scales behind it.
- Swipe down on home: Quick actions (search, recent apps, Internet, Bluetooth, torch, camera,
  notifications, settings), Search, Notification shade, or nothing — configurable.
- Long-press empty home space: edit mode — drag to reorder, × to remove, Undo remove,
  Add apps, Settings, Done. Long-press menu gains "Edit home".
- Press-in feedback on app rows and a clip-reveal launch animation from the tapped row.
- Recent apps (launched from MONO) in the drawer and quick panel.
- A–Z rail: straight when idle; bends toward the finger with a letter bubble while dragging,
  springs back on release.
- Settings → Home & gestures: swipe-down action, app names, icon size, recent apps.

### Added — Lock screen & drawer upgrade
- Lock screen is fully immersive: status and navigation bars hidden, drawn under the camera
  cutout. Clock, date, weather and wallpaper layout unchanged.
- Subtle battery percentage and charging/full status in the lock screen's top corner.
- Media card on the lock screen, shown only while a music/video session is playing or paused:
  artwork, title, artist/channel, play/pause, previous/next, progress and seeking (when the app
  supports it), animated in/out, dismissible per track. Requires notification access (Android
  rule for seeing other apps' media); MONO does not read notifications.
- Zigzag A–Z rail in the app drawer: letters alternate left/right in equal slots, tap to glide to
  a section, drag to scrub, current section highlighted, haptic + scale feedback.

### Added — Phase 3: native widget engine + MONO lock screen
- **Radial Digital clock** (new default): large hour beside a rotating minute dial with a
  capsule on the current minute, a seconds dot and seconds digits. 24-hour and seconds on by default.
- **Editorial Word Clock** ("**four**thirty eight") as the alternative style.
- **Date**: date line, month headline and script weekday; tap to open your calendar.
- **Weather** from Open-Meteo: temperature, condition, humidity, wind, optional 3-day forecast;
  city search, °C/°F, cached and refreshed hourly. Tap to refresh.
- **Battery**: thin-line battery icon with charging bolt and percentage; tap for battery usage.
- **MONO lock screen** shown over Android's lock screen: big hour, smaller minutes, small
  seconds, vertical date ("13-NOVEMBER"), weather and weekday typography; adjustable position
  and contrast; preview; swipe up for Android's secure PIN/biometric unlock.
- Optional: set the MONO wallpaper as Android's lock-screen wallpaper.
- Settings → Clock & widgets and Settings → Lock screen.
- **Ocean** photo wallpaper (supplied by the owner) is now the default wallpaper.

### Added — Phase 2: ocean visual design
- Wallpaper engine: MONO draws its own full-screen wallpaper behind the home screen.
- Three original monochrome ocean wallpapers: Shoreline (default), Undertow, Swell.
- Use your own photo via the system photo picker (no storage permission).
- Wallpaper editor: live preview at your screen's proportions, drag to reposition,
  dim slider, monochrome switch, reset, apply.
- Typography: Outfit for all text and a Sacramento script weekday with a hairline strike,
  matching the editorial layout. Text shadows keep the home screen legible over foam/photos.
- App drawer is now slightly translucent over the wallpaper.
- Font and artwork credits in Settings → About.

### Added — Phase 1: launcher foundation
- MONO can be set as the default Home app (role request on Android 10+, settings fallback).
- Editorial home screen: date line, month, weekday, time in words, accent bar, and up to
  8 pinned app shortcuts. Adapts to screen size, cutouts and navigation bars; landscape layout.
- First run picks home shortcuts from your real default apps (phone, messages, camera,
  browser, Play Store, settings).
- App drawer (swipe up): alphabetical sections, A–Z fast scroller, accent-insensitive search,
  press Go to open the top result, pull down or Back to close.
- Long-press menus: pin/unpin, app info, MONO settings.
- Settings: default-home status, reorder/remove home apps, version.
- Work-profile apps are listed and labelled.
- Original MONO adaptive app icon (with Android 13 themed-icon layer).
- GitHub Actions: debug APK on every push/PR; signed release on `v*` tags.
