# Changelog

All notable user-facing changes. Format: [Keep a Changelog](https://keepachangelog.com/).

## [Unreleased]

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
