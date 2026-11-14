#!/usr/bin/env python3
"""
MONO Thin Line icon pipeline.

Source of truth:  design/icons/svg/<name>.svg  +  design/icons/manifest.json
Generates:        app/src/main/res/drawable/ic_mono_<name>.xml          (VectorDrawable)
                  app/src/main/java/com/shoron/mono/icons/generated/IconManifest.kt

Usage:
  python3 tools/icons/build_icons.py              # validate + regenerate
  python3 tools/icons/build_icons.py --check      # validate + fail if generated files are stale (CI)
  python3 tools/icons/build_icons.py --preview out.html   # contact sheet for review

Validation rules (the MONO Thin Line style):
  * 48x48 canvas (width, height and viewBox "0 0 48 48"); transparent background.
  * Only path, circle, ellipse, rect, line, polyline, polygon (no raster, text, gradients, groups).
  * Outlines: stroke #FFFFFF, stroke-width 1.5-2.5, round caps and joins, no fill.
  * Solid accents allowed only as small dots: fill #FFFFFF, no stroke, radius <= 2.
  * All geometry inside the 2..46 safe area. Absolute path commands only.
  * Manifest: unique icon names and package names, valid package/component syntax, known roles,
    every icon has an SVG and every SVG is listed.
"""

from __future__ import annotations

import json
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DESIGN = ROOT / "design" / "icons"
SVG_DIR = DESIGN / "svg"
MANIFEST = DESIGN / "manifest.json"
DRAWABLE_DIR = ROOT / "app" / "src" / "main" / "res" / "drawable"
KOTLIN_OUT = ROOT / "app" / "src" / "main" / "java" / "com" / "shoron" / "mono" / "icons" / "generated" / "IconManifest.kt"

SVG_NS = "{http://www.w3.org/2000/svg}"
ALLOWED = {"path", "circle", "ellipse", "rect", "line", "polyline", "polygon"}
ROLES = {"dialer", "sms", "camera", "settings", "clock", "calendar"}
NAME_RE = re.compile(r"^[a-z][a-z0-9_]*$")
PACKAGE_RE = re.compile(r"^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)+$")
SAFE_MIN, SAFE_MAX = 2.0, 46.0
WHITE = {"#fff", "#ffffff"}

errors: list[str] = []


def err(msg: str) -> None:
    errors.append(msg)


def num(v: float) -> str:
    s = f"{v:.3f}".rstrip("0").rstrip(".")
    return "0" if s in ("-0", "") else s


# ----------------------------------------------------------------------------- path parsing

TOKEN = re.compile(r"[A-Za-z]|-?\d*\.?\d+(?:[eE][-+]?\d+)?")
ARGC = {"M": 2, "L": 2, "H": 1, "V": 1, "C": 6, "S": 4, "Q": 4, "T": 2, "A": 7, "Z": 0}


def path_points(d: str, where: str) -> list[tuple[float, float]]:
    """Absolute coordinates touched by a path (end and control points)."""
    tokens = TOKEN.findall(d)
    points: list[tuple[float, float]] = []
    i, cmd = 0, None
    x = y = 0.0
    while i < len(tokens):
        t = tokens[i]
        if t.isalpha():
            if t.islower() and t != "z":
                err(f"{where}: relative path command '{t}' — use absolute commands")
                return points
            cmd = t.upper()
            i += 1
            if cmd == "Z":
                continue
        if cmd is None:
            err(f"{where}: path data must start with a command")
            return points
        n = ARGC[cmd]
        args = tokens[i:i + n]
        if len(args) < n or any(a.isalpha() for a in args):
            err(f"{where}: malformed path near '{' '.join(tokens[i:i + 8])}'")
            return points
        vals = [float(a) for a in args]
        i += n
        if cmd in ("M", "L", "T"):
            x, y = vals
            points.append((x, y))
        elif cmd == "H":
            x = vals[0]
            points.append((x, y))
        elif cmd == "V":
            y = vals[0]
            points.append((x, y))
        elif cmd == "C":
            points += [(vals[0], vals[1]), (vals[2], vals[3]), (vals[4], vals[5])]
            x, y = vals[4], vals[5]
        elif cmd in ("S", "Q"):
            points += [(vals[0], vals[1]), (vals[2], vals[3])]
            x, y = vals[2], vals[3]
        elif cmd == "A":
            x, y = vals[5], vals[6]
            points.append((x, y))
        if cmd == "M":
            cmd = "L"  # implicit lineto after moveto
    return points


# ----------------------------------------------------------------------------- shapes → path

def shape_to_path(el: ET.Element, where: str) -> tuple[str, list[tuple[float, float]]]:
    tag = el.tag.replace(SVG_NS, "")
    g = lambda k, d=0.0: float(el.get(k, d))  # noqa: E731
    if tag == "path":
        d = " ".join(el.get("d", "").split())
        return d, path_points(d, where)
    if tag in ("circle", "ellipse"):
        cx, cy = g("cx"), g("cy")
        rx = g("r") if tag == "circle" else g("rx")
        ry = g("r") if tag == "circle" else g("ry")
        d = (f"M{num(cx - rx)} {num(cy)} A{num(rx)} {num(ry)} 0 1 0 {num(cx + rx)} {num(cy)} "
             f"A{num(rx)} {num(ry)} 0 1 0 {num(cx - rx)} {num(cy)} Z")
        return d, [(cx - rx, cy - ry), (cx + rx, cy + ry)]
    if tag == "rect":
        x, y, w, h = g("x"), g("y"), g("width"), g("height")
        r = min(g("rx", el.get("ry", 0)), w / 2, h / 2)
        if r <= 0:
            d = f"M{num(x)} {num(y)} H{num(x + w)} V{num(y + h)} H{num(x)} Z"
        else:
            d = (f"M{num(x + r)} {num(y)} H{num(x + w - r)} A{num(r)} {num(r)} 0 0 1 {num(x + w)} {num(y + r)} "
                 f"V{num(y + h - r)} A{num(r)} {num(r)} 0 0 1 {num(x + w - r)} {num(y + h)} "
                 f"H{num(x + r)} A{num(r)} {num(r)} 0 0 1 {num(x)} {num(y + h - r)} "
                 f"V{num(y + r)} A{num(r)} {num(r)} 0 0 1 {num(x + r)} {num(y)} Z")
        return d, [(x, y), (x + w, y + h)]
    if tag == "line":
        p = [(g("x1"), g("y1")), (g("x2"), g("y2"))]
        return f"M{num(p[0][0])} {num(p[0][1])} L{num(p[1][0])} {num(p[1][1])}", p
    if tag in ("polyline", "polygon"):
        nums = [float(v) for v in re.split(r"[\s,]+", el.get("points", "").strip()) if v]
        if len(nums) < 4 or len(nums) % 2:
            err(f"{where}: invalid points")
            return "", []
        p = list(zip(nums[0::2], nums[1::2]))
        d = f"M{num(p[0][0])} {num(p[0][1])} " + " ".join(f"L{num(a)} {num(b)}" for a, b in p[1:])
        return d + (" Z" if tag == "polygon" else ""), p
    err(f"{where}: unsupported element <{tag}>")
    return "", []


# ----------------------------------------------------------------------------- svg → vector

def convert(svg_path: Path) -> str | None:
    name = svg_path.stem
    try:
        root = ET.parse(svg_path).getroot()
    except ET.ParseError as e:
        err(f"{svg_path.name}: not valid XML ({e})")
        return None
    if root.tag != SVG_NS + "svg":
        err(f"{svg_path.name}: root element must be <svg> in the SVG namespace")
        return None
    if root.get("viewBox", "").split() != ["0", "0", "48", "48"] or root.get("width") != "48" or root.get("height") != "48":
        err(f"{svg_path.name}: canvas must be 48x48 with viewBox '0 0 48 48'")

    inherited = {k: root.get(k) for k in ("fill", "stroke", "stroke-width", "stroke-linecap", "stroke-linejoin")}
    paths: list[str] = []
    for i, el in enumerate(root):
        tag = el.tag.replace(SVG_NS, "")
        where = f"{svg_path.name} <{tag}> #{i + 1}"
        if tag not in ALLOWED:
            err(f"{where}: element not allowed (only {', '.join(sorted(ALLOWED))})")
            continue
        if len(el):
            err(f"{where}: nested elements are not allowed")
        attr = {k: el.get(k, v) for k, v in inherited.items()}
        d, points = shape_to_path(el, where)
        if not d:
            continue
        for (px, py) in points:
            if not (SAFE_MIN <= px <= SAFE_MAX and SAFE_MIN <= py <= SAFE_MAX):
                err(f"{where}: point ({num(px)}, {num(py)}) outside the {SAFE_MIN:g}..{SAFE_MAX:g} safe area")
                break
        fill = (attr["fill"] or "none").lower()
        stroke = (attr["stroke"] or "none").lower()
        if stroke != "none":
            if stroke not in WHITE:
                err(f"{where}: stroke must be #FFFFFF")
            width = float(attr["stroke-width"] or 0)
            if not 1.5 <= width <= 2.5:
                err(f"{where}: stroke-width {width:g} outside 1.5..2.5")
            if attr["stroke-linecap"] != "round" or attr["stroke-linejoin"] != "round":
                err(f"{where}: strokes need round caps and joins")
            if fill != "none":
                err(f"{where}: outlined shapes must not be filled")
            paths.append(
                f'    <path\n        android:pathData="{d}"\n        android:fillColor="#00000000"\n'
                f'        android:strokeColor="#FFFFFFFF"\n        android:strokeWidth="{num(width)}"\n'
                f'        android:strokeLineCap="round"\n        android:strokeLineJoin="round" />'
            )
        elif fill in WHITE:
            radius = float(el.get("r", 99)) if tag == "circle" else 99
            if radius > 2:
                err(f"{where}: solid fills are only allowed for small dots (circle r <= 2)")
            paths.append(f'    <path\n        android:pathData="{d}"\n        android:fillColor="#FFFFFFFF" />')
        else:
            err(f"{where}: needs a white stroke or (for dots) a white fill")

    if not paths:
        err(f"{svg_path.name}: no drawable shapes")
        return None
    return (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        f"<!-- Generated by tools/icons/build_icons.py from design/icons/svg/{name}.svg. Do not edit. -->\n"
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    android:width="48dp"\n    android:height="48dp"\n'
        '    android:viewportWidth="48"\n    android:viewportHeight="48">\n'
        + "\n".join(paths)
        + "\n</vector>\n"
    )


# ----------------------------------------------------------------------------- manifest

def load_manifest() -> list[dict]:
    data = json.loads(MANIFEST.read_text())
    icons = data.get("icons", [])
    names: set[str] = set()
    owners: dict[str, str] = {}
    for icon in icons:
        name = icon.get("name", "")
        if not NAME_RE.match(name):
            err(f"manifest: invalid icon name '{name}'")
        if name in names:
            err(f"manifest: duplicate icon '{name}'")
        names.add(name)
        if not (SVG_DIR / f"{name}.svg").exists():
            err(f"manifest: icon '{name}' has no svg/{name}.svg (missing icon)")
        for pkg in icon.get("packages", []):
            if not PACKAGE_RE.match(pkg):
                err(f"manifest: '{name}' has invalid package '{pkg}'")
            if pkg in owners and owners[pkg] != name:
                err(f"manifest: package '{pkg}' mapped to both '{owners[pkg]}' and '{name}'")
            owners[pkg] = name
        for comp in icon.get("components", []):
            pkg, _, cls = comp.partition("/")
            if not PACKAGE_RE.match(pkg) or not cls:
                err(f"manifest: '{name}' has invalid component '{comp}' (use package/fully.qualified.Activity)")
        role = icon.get("role")
        if role is not None and role not in ROLES:
            err(f"manifest: '{name}' has unknown role '{role}' (known: {', '.join(sorted(ROLES))})")
        if not icon.get("packages") and not icon.get("components") and not role:
            err(f"manifest: '{name}' maps to nothing")
    for svg in SVG_DIR.glob("*.svg"):
        if svg.stem not in names:
            err(f"svg/{svg.name} is not listed in manifest.json")
    return icons


def kotlin(icons: list[dict]) -> str:
    def strings(values: list[str]) -> str:
        return "emptySet()" if not values else "setOf(" + ", ".join(f'"{v}"' for v in values) + ")"

    def string_list(values: list[str]) -> str:
        return "emptyList()" if not values else "listOf(" + ", ".join(f'"{v.lower()}"' for v in values) + ")"

    entries = []
    for icon in icons:
        role = f'"{icon["role"]}"' if icon.get("role") else "null"
        entries.append(
            "        IconSpec(\n"
            f'            name = "{icon["name"]}",\n'
            f'            drawable = R.drawable.ic_mono_{icon["name"]},\n'
            f'            packages = {strings(icon.get("packages", []))},\n'
            f'            components = {strings(icon.get("components", []))},\n'
            f"            role = {role},\n"
            f'            labels = {string_list(icon.get("labels", []))},\n'
            "        ),"
        )
    return (
        "// Generated by tools/icons/build_icons.py from design/icons/manifest.json. Do not edit.\n"
        "package com.shoron.mono.icons.generated\n\n"
        "import com.shoron.mono.R\n"
        "import com.shoron.mono.icons.IconSpec\n\n"
        "object IconManifest {\n"
        "    val icons: List<IconSpec> = listOf(\n"
        + "\n".join(entries)
        + "\n    )\n}\n"
    )


def preview(icons: list[dict], vectors: dict[str, str], out: Path) -> None:
    cells = []
    for icon in icons:
        svg = (SVG_DIR / f"{icon['name']}.svg").read_text()
        cells.append(f'<figure><div class="i">{svg}</div><figcaption>{icon["name"]}</figcaption></figure>')
    out.write_text(
        "<!doctype html><meta charset=utf-8><style>body{background:#0a0a0b;color:#bbb;font:12px sans-serif;"
        "display:flex;flex-wrap:wrap;gap:18px;padding:24px;margin:0}figure{margin:0;text-align:center}"
        ".i svg{width:96px;height:96px;display:block}</style>" + "".join(cells)
    )


def main() -> int:
    check = "--check" in sys.argv
    preview_out = None
    if "--preview" in sys.argv:
        preview_out = Path(sys.argv[sys.argv.index("--preview") + 1])

    icons = load_manifest()
    listed = {icon["name"] for icon in icons}
    vectors = {}
    # Validate every SVG in the library (listed or not); generate only listed ones.
    for svg in sorted(SVG_DIR.glob("*.svg")):
        xml = convert(svg)
        if xml and svg.stem in listed:
            vectors[svg.stem] = xml
    expected = {DRAWABLE_DIR / f"ic_mono_{n}.xml": x for n, x in vectors.items()}
    expected[KOTLIN_OUT] = kotlin(icons)

    stray = [p for p in DRAWABLE_DIR.glob("ic_mono_*.xml") if p not in expected]
    for p in stray:
        err(f"{p.relative_to(ROOT)} has no source icon in the manifest")

    if errors:
        print("Icon validation failed:", *errors, sep="\n  - ", file=sys.stderr)
        return 1

    stale = [p for p, text in expected.items() if not p.exists() or p.read_text() != text]
    if check:
        if stale:
            print("Generated icon files are out of date; run python3 tools/icons/build_icons.py:",
                  *[str(p.relative_to(ROOT)) for p in stale], sep="\n  - ", file=sys.stderr)
            return 1
        print(f"OK: {len(vectors)} icons valid and up to date.")
        return 0

    for path, text in expected.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text)
    if preview_out:
        preview(icons, vectors, preview_out)
    print(f"Generated {len(vectors)} icons ({len(stale)} changed).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
