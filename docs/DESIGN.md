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

## Tokens (Phase 1)

| Token | Value |
|---|---|
| Ink (background) | `#0A0A0B` |
| Foreground | `#F5F5F2` |
| Muted | Foreground @ 70% |
| Faint | Foreground @ 45% |
| Hairline | Foreground @ 20% |
| Editorial gutter | 12% of usable width |
| Type scale | proportional to usable width (1.0 at 392dp, clamped 0.85–1.25) |
