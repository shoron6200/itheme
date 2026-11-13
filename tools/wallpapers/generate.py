#!/usr/bin/env python3
"""
MONO ocean wallpaper generator.

Produces MONO's original, procedurally generated black-and-white ocean artwork.
No photographs or third-party images are used: every pixel comes from seeded noise,
so the output is reproducible and fully owned by the project.

Usage:
    python3 tools/wallpapers/generate.py [output_dir] [--preview]

Requires: numpy, Pillow (with WebP support).
"""

from __future__ import annotations

import sys
from pathlib import Path

import numpy as np
from PIL import Image

WIDTH, HEIGHT = 1600, 3040  # taller than 20:9 phones, with horizontal room for repositioning
WEBP_QUALITY = 80


# ---------------------------------------------------------------- noise primitives

def _resize(field: np.ndarray, w: int, h: int) -> np.ndarray:
    img = Image.fromarray(field.astype(np.float32), mode="F")
    return np.asarray(img.resize((w, h), Image.BICUBIC), dtype=np.float32)


def value_noise(rng: np.random.Generator, w: int, h: int, cells_x: float, cells_y: float) -> np.ndarray:
    gx, gy = max(2, int(round(cells_x)) + 3), max(2, int(round(cells_y)) + 3)
    grid = rng.random((gy, gx), dtype=np.float32)
    big = _resize(grid, int(w * gx / max(cells_x, 1)), int(h * gy / max(cells_y, 1)))
    ox = int(rng.integers(0, max(1, big.shape[1] - w)))
    oy = int(rng.integers(0, max(1, big.shape[0] - h)))
    return big[oy:oy + h, ox:ox + w]


def fbm(rng, w, h, cells_x, cells_y, octaves=5, gain=0.5, lacunarity=2.0) -> np.ndarray:
    total = np.zeros((h, w), np.float32)
    amp, norm = 1.0, 0.0
    cx, cy = cells_x, cells_y
    for _ in range(octaves):
        total += amp * value_noise(rng, w, h, cx, cy)
        norm += amp
        amp *= gain
        cx *= lacunarity
        cy *= lacunarity
    return total / norm


def warp(field: np.ndarray, dx: np.ndarray, dy: np.ndarray) -> np.ndarray:
    """Bilinear resample `field` at (x + dx, y + dy), in pixels."""
    h, w = field.shape
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    x = np.clip(xx + dx, 0, w - 1.001)
    y = np.clip(yy + dy, 0, h - 1.001)
    x0, y0 = x.astype(np.int32), y.astype(np.int32)
    fx, fy = x - x0, y - y0
    a = field[y0, x0]
    b = field[y0, x0 + 1]
    c = field[y0 + 1, x0]
    d = field[y0 + 1, x0 + 1]
    return (a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy


def _box(field: np.ndarray, r: int, axis: int) -> np.ndarray:
    pad = [(0, 0), (0, 0)]
    pad[axis] = (r + 1, r)
    c = np.cumsum(np.pad(field, pad, mode="edge"), axis=axis, dtype=np.float64)
    n = field.shape[axis]
    hi = np.take(c, np.arange(2 * r + 1, 2 * r + 1 + n), axis=axis)
    lo = np.take(c, np.arange(0, n), axis=axis)
    return ((hi - lo) / (2 * r + 1)).astype(np.float32)


def blur(field: np.ndarray, radius: float) -> np.ndarray:
    """Approximate Gaussian blur: three separable box passes."""
    r = max(1, int(round(radius)))
    for _ in range(3):
        field = _box(_box(field, r, 0), r, 1)
    return field


def smoothstep(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0.0, 1.0)
    return t * t * (3 - 2 * t)


def stretch(n: np.ndarray, lo=None, hi=None) -> np.ndarray:
    lo = np.percentile(n, 1) if lo is None else lo
    hi = np.percentile(n, 99) if hi is None else hi
    return np.clip((n - lo) / (hi - lo), 0, 1)


# ---------------------------------------------------------------- ocean building blocks

def water(rng, w, h, base=0.075) -> np.ndarray:
    """Dark water: long diagonal swell streaks with very low contrast."""
    swell = stretch(fbm(rng, w, h, 3, 28, octaves=5))
    chop = stretch(fbm(rng, w, h, 18, 60, octaves=3))
    tex = 0.65 * swell + 0.35 * chop
    return base + 0.05 * (tex - 0.5)


def foam(rng, w, h, density, scale=1.0):
    """Build a foam layer (0..1 coverage) and its brightness from a density field.

    density 1 -> nearly solid whitewater with bubbly holes,
    density ~0.3 -> lace, veins and drifting fragments,
    density 0 -> open water.
    Returns (coverage, brightness).
    """
    wx = (fbm(rng, w, h, 3 * scale, 5 * scale, octaves=4) - 0.5) * 200 * scale
    wy = (fbm(rng, w, h, 3 * scale, 5 * scale, octaves=4) - 0.5) * 200 * scale
    mid = stretch(warp(fbm(rng, w, h, 18 * scale, 24 * scale, octaves=6, gain=0.55), wx, wy))
    fine = stretch(warp(fbm(rng, w, h, 60 * scale, 80 * scale, octaves=4, gain=0.6), wx * 0.4, wy * 0.4))
    micro = stretch(fbm(rng, w, h, 180 * scale, 240 * scale, octaves=2))
    pattern = 0.7 * mid + 0.3 * fine

    # 1. Solid whitewater: thresholded pattern, threshold falls as density rises.
    threshold = 0.84 - 0.60 * density
    solid = smoothstep(threshold - 0.07, threshold + 0.05, pattern)
    # Bubbly holes punched into the solid mass.
    holes = smoothstep(0.62, 0.82, micro) * smoothstep(0.55, 0.75, 1 - fine)
    solid *= 1 - 0.75 * holes

    # 2. Lace veins: ridged warped noise, broken up so they read as drifting foam
    #    strands rather than contour lines. Strongest at medium density.
    ridge = 1.0 - np.abs(2.0 * mid - 1.0)
    breakup = smoothstep(0.35, 0.70, fbm(rng, w, h, 30 * scale, 40 * scale, octaves=3))
    veins = smoothstep(0.78, 0.96, ridge) * breakup
    veins *= smoothstep(0.08, 0.35, density) * (1 - 0.7 * density)

    # 3. Translucent film: a grey veil that softens the transition to water.
    film = 0.35 * smoothstep(0.35, 0.85, mid) * smoothstep(0.05, 0.6, density)

    coverage = np.clip(np.maximum(solid, np.maximum(veins * 0.6, film)), 0, 1)
    coverage *= smoothstep(0.02, 0.12, density)
    coverage = blur(coverage, 1.4)

    volume = stretch(fbm(rng, w, h, 8 * scale, 12 * scale, octaves=5))
    texture = stretch(warp(fbm(rng, w, h, 40 * scale, 55 * scale, octaves=4, gain=0.6), wx * 0.2, wy * 0.2))
    brightness = 0.60 + 0.24 * volume + 0.30 * texture
    return coverage, np.clip(brightness, 0, 1)


def grain(rng, w, h, amount=0.018) -> np.ndarray:
    return (rng.random((h, w), dtype=np.float32) - 0.5) * 2 * amount


def vignette(w, h, strength=0.35) -> np.ndarray:
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    u = (xx / w - 0.5) * 1.1
    v = (yy / h - 0.5)
    r = np.sqrt(u * u + v * v)
    return 1.0 - strength * smoothstep(0.35, 0.85, r)


def compose(water_layer, density, coverage, brightness) -> np.ndarray:
    # Water under and between foam is shallow and aerated, so it reads lighter.
    under = water_layer + 0.10 * smoothstep(0.1, 0.9, density)
    return under * (1 - coverage) + brightness * coverage


# ---------------------------------------------------------------- wallpapers

def shoreline(seed=11) -> np.ndarray:
    """Hero image: dark water on the left, foam breaking down the right edge."""
    rng = np.random.default_rng(seed)
    w, h = WIDTH, HEIGHT
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    u, v = xx / w, yy / h

    boundary = 0.60 - 0.10 * np.sin(v * 2.4 + 0.4) + 0.06 * v
    ragged = (fbm(rng, w, h, 4, 10, octaves=5) - 0.5) * 0.26
    d = u - (boundary + ragged)
    density = smoothstep(-0.10, 0.28, d)

    coverage, brightness = foam(rng, w, h, density)
    img = compose(water(rng, w, h), density, coverage, brightness)
    img *= vignette(w, h, 0.30)
    return img + grain(rng, w, h)


def undertow(seed=29) -> np.ndarray:
    """Foam rolling in from the lower right corner; generous dark space above."""
    rng = np.random.default_rng(seed)
    w, h = WIDTH, HEIGHT
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    u, v = xx / w, yy / h

    diag = (u * 0.85 + v * 1.0) - 1.18
    ragged = (fbm(rng, w, h, 5, 9, octaves=5) - 0.5) * 0.28
    density = smoothstep(-0.10, 0.30, diag + ragged)
    # A second, thinner line of breaking foam further out.
    crest = np.abs(diag + 0.26 + (fbm(rng, w, h, 6, 8, octaves=4) - 0.5) * 0.10)
    density = np.maximum(density, 0.55 * (1 - smoothstep(0.0, 0.035, crest)))

    coverage, brightness = foam(rng, w, h, density, 1.1)
    img = compose(water(rng, w, h, 0.07), density, coverage, brightness)
    img *= vignette(w, h, 0.35)
    return img + grain(rng, w, h)


def swell(seed=47) -> np.ndarray:
    """Minimal: deep water crossed by three thin, broken wave crests."""
    rng = np.random.default_rng(seed)
    w, h = WIDTH, HEIGHT
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    u, v = xx / w, yy / h

    density = np.zeros((h, w), np.float32)
    for offset, width, strength in ((0.50, 0.030, 0.45), (0.68, 0.045, 0.55), (0.86, 0.065, 0.70)):
        line = v - offset - 0.10 * (u - 0.5) - 0.025 * np.sin(u * 5.0 + offset * 9)
        line += (fbm(rng, w, h, 6, 5, octaves=4) - 0.5) * 0.05
        density = np.maximum(density, strength * (1 - smoothstep(0.0, width, np.abs(line))))

    coverage, brightness = foam(rng, w, h, density, 1.3)
    img = compose(water(rng, w, h, 0.06), density, coverage, brightness)
    img *= vignette(w, h, 0.4)
    return img + grain(rng, w, h, 0.014)


WALLPAPERS = {
    "wallpaper_shoreline": shoreline,
    "wallpaper_undertow": undertow,
    "wallpaper_swell": swell,
}


def to_image(img: np.ndarray) -> Image.Image:
    # Gentle filmic curve: deep blacks, soft highlight roll-off.
    img = np.clip(img, 0, 1)
    img = img ** 1.08
    return Image.fromarray((img * 255 + 0.5).astype(np.uint8), mode="L")


def main() -> None:
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    preview = "--preview" in sys.argv
    out = Path(args[0]) if args else Path(__file__).resolve().parents[2] / "app/src/main/res/drawable-nodpi"
    out.mkdir(parents=True, exist_ok=True)
    for name, build in WALLPAPERS.items():
        image = to_image(build())
        path = out / f"{name}.webp"
        image.save(path, "WEBP", quality=WEBP_QUALITY, method=6)
        print(f"{path}  {path.stat().st_size // 1024} KiB")
        if preview:
            image.resize((WIDTH // 4, HEIGHT // 4), Image.LANCZOS).save(out / f"{name}_preview.png")


if __name__ == "__main__":
    main()
