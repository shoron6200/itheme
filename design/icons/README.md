# MONO Thin Line icons

Original vector icons for MONO. Source of truth: `svg/*.svg` + `manifest.json`.
Everything under `app/src/main/res/drawable/ic_mono_*.xml` and
`app/src/main/java/com/shoron/mono/icons/generated/IconManifest.kt` is **generated**.

## Style
- 48×48 canvas, `viewBox="0 0 48 48"`, transparent background.
- White outline only: `stroke="#FFFFFF"`, `stroke-width` 2 (1.5–2.5 allowed),
  `stroke-linecap="round"`, `stroke-linejoin="round"`, `fill="none"`.
- Solid white only for small dots (circle, r ≤ 2, no stroke).
- Keep geometry inside 2…46; aim for ~6…42 so icons share one optical size.
- Simple geometric shapes that keep the app recognisable — not copies of official logos.
- Allowed elements: `path` (absolute commands), `circle`, `ellipse`, `rect`, `line`,
  `polyline`, `polygon`. No text, raster images, gradients or groups.

## Add an icon
1. Draw `svg/<name>.svg` following the style above.
2. Add an entry to `manifest.json`:
   ```json
   { "name": "notes", "packages": ["com.example.notes"], "labels": ["notes"] }
   ```
   - `packages`: every launcher activity of the package uses the icon.
   - `components`: exact `package/fully.qualified.Activity` (for apps with several entries).
   - `role`: `dialer`, `sms`, `camera`, `settings`, `clock` or `calendar` — also applies to
     whatever app fills that role on the device (OEM system apps).
   - `labels`: when a package has several launcher activities, only those whose label contains
     one of these words get the icon.
3. Run `python3 tools/icons/build_icons.py` (add `--preview icons.html` for a contact sheet).
4. Commit the SVG, manifest and generated files. CI runs `--check` and fails if anything is
   invalid, missing or stale.

## Batches from your missing-app list

Produced batches and their exact package mappings: [BATCHES.md](BATCHES.md).
1. On the phone: MONO settings → Icons → (filter/select) → export TXT, CSV or JSON.
2. `python3 tools/icons/plan_batch.py mono-missing-icons.json --size 20` — removes packages that
   are already mapped and merges duplicate packages; `--write` saves
   `batches/batch-NN.json` plans (name, label, packages).
3. Draw each planned icon as `svg/<name>.svg`, move its entry into `manifest.json`, run
   `build_icons.py`, review the `--preview` sheet, commit. Existing icons are never touched.

MONO settings → Icons lists installed apps that still use their original icon.
