# MONO design direction

## Reference

The user supplied a video of the iTheme Design "Black & White Waves" Android setup as the
visual reference. Observed characteristics we are reproducing **in our own original form**:

- Full-screen black & white ocean photograph: dark charcoal water on the left, bright foam
  breaking diagonally down the right edge.
- Left-aligned editorial column on the dark side:
  - small weather line (temperature, condition glyph, location),
  - large bold geometric month name ("November"),
  - condition / humidity / wind / battery detail lines,
  - weekday in a thin handwritten script,
  - time spelled in words with a bold hour and light minutes ("**four**thirty eight"),
  - a short horizontal accent bar.
- Lower half: vertical list of ~6 app shortcuts, thin-line white icons with small bold labels.
- App drawer: alphabetical list with section letters and a right-edge A–Z scroller.
- Lock/AOD style: radial minute dial with a pill-shaped minute marker beside the hour,
  stacked date column, small battery indicator.

## Asset policy

The reference product is a paid, proprietary pack. MONO **does not** include, trace or
redistribute its wallpapers, icons or widget files. All MONO assets are:

- created for this project (procedural or hand-authored SVG → VectorDrawable), or
- openly licensed (e.g. SIL OFL fonts) with the license kept in the repo, or
- chosen by the user from their own device via the system photo picker.

## Wallpapers

**Ocean** (default) is the owner-supplied black & white ocean photograph, bundled at the owner's
request (941×1672, greyscale WebP). Because this repository is public, that image is public too.

Three further original monochrome ocean pieces, generated procedurally (seeded fractal noise, domain
warping, density-driven foam carving) by `tools/wallpapers/generate.py`:

| Id | Name | Composition |
|---|---|---|
| `builtin:shoreline` | Shoreline | dark water left, foam breaking down the right edge (default) |
| `builtin:undertow` | Undertow | foam rolling in from the lower-right corner, outer crest line |
| `builtin:swell` | Swell | deep water with three thin broken crests |

1600×3040 px greyscale WebP (q80), ~60–130 KiB each. Regenerate with
`python3 tools/wallpapers/generate.py`.

## Typography

| Role | Typeface | License |
|---|---|---|
| UI, month, time, labels | Outfit (Light–Bold) | SIL OFL 1.1 |
| Weekday script accent | Sacramento | SIL OFL 1.1 (Reserved Font Name; shipped unmodified) |

Font files are bundled unmodified in `res/font`; full license texts ship in
`assets/licenses/`.

## Tokens

| Token | Value |
|---|---|
| Ink (background) | `#0A0A0B` |
| Foreground | `#F5F5F2` |
| Muted | Foreground @ 70% |
| Faint | Foreground @ 45% |
| Hairline | Foreground @ 20% |
| Editorial gutter | 12% of usable width |
| Type scale | proportional to usable width (1.0 at 392dp, clamped 0.85–1.25) |
