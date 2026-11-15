#!/usr/bin/env python3
"""
Plan the next Thin Line icon batches from MONO's missing-icon export.

Input: the TXT, CSV or JSON exported from MONO → Settings → Icons (or a plain list of
package names, one per line).

    python3 tools/icons/plan_batch.py mono-missing-icons.json [--size 20] [--write]

It drops packages that are already mapped in design/icons/manifest.json, merges duplicate
packages (several launcher activities → one icon), sorts by label and splits the rest into
batches. With --write it saves design/icons/batches/batch-NN.json plans (name suggestion,
label, packages) that are later turned into SVGs + manifest entries. It never creates icons.
"""

from __future__ import annotations

import csv
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MANIFEST = ROOT / "design" / "icons" / "manifest.json"
BATCH_DIR = ROOT / "design" / "icons" / "batches"
PACKAGE_RE = re.compile(r"^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)+$")


def read_export(path: Path) -> list[tuple[str, str]]:
    """(label, package) pairs from MONO's TXT / CSV / JSON export or a package list."""
    text = path.read_text(encoding="utf-8")
    if path.suffix.lower() == ".json":
        return [(o.get("label", o["package"]), o["package"]) for o in json.loads(text)]
    if path.suffix.lower() == ".csv":
        return [(r["label"], r["package"]) for r in csv.DictReader(text.splitlines())]
    pairs = []
    for line in text.splitlines():
        parts = [p.strip() for p in line.split("\t") if p.strip()]
        if len(parts) == 2:
            pairs.append((parts[0], parts[1]))
        elif len(parts) == 1 and PACKAGE_RE.match(parts[0]):
            pairs.append((parts[0], parts[0]))
    return pairs


def suggest_name(label: str, taken: set[str]) -> str:
    base = re.sub(r"[^a-z0-9]+", "_", label.lower()).strip("_") or "app"
    if not base[0].isalpha():
        base = "app_" + base
    name, n = base, 2
    while name in taken:
        name, n = f"{base}_{n}", n + 1
    taken.add(name)
    return name


def plan(pairs: list[tuple[str, str]], manifest: dict, size: int) -> list[list[dict]]:
    mapped = {p for icon in manifest["icons"] for p in icon.get("packages", [])}
    taken = {icon["name"] for icon in manifest["icons"]}
    by_package: dict[str, str] = {}
    for label, pkg in pairs:
        if PACKAGE_RE.match(pkg) and pkg not in mapped and pkg not in by_package:
            by_package[pkg] = label
    entries = [
        {"name": suggest_name(label, taken), "label": label, "packages": [pkg]}
        for pkg, label in sorted(by_package.items(), key=lambda kv: kv[1].lower())
    ]
    return [entries[i:i + size] for i in range(0, len(entries), size)]


def main() -> int:
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    if not args:
        print(__doc__)
        return 2
    size = int(sys.argv[sys.argv.index("--size") + 1]) if "--size" in sys.argv else 20
    manifest = json.loads(MANIFEST.read_text())
    batches = plan(read_export(Path(args[0])), manifest, size)
    total = sum(len(b) for b in batches)
    print(f"{total} apps need icons → {len(batches)} batch(es) of up to {size}.")
    for i, batch in enumerate(batches, 1):
        print(f"  batch {i}: " + ", ".join(e["label"] for e in batch))
    if "--write" in sys.argv:
        BATCH_DIR.mkdir(parents=True, exist_ok=True)
        start = len(list(BATCH_DIR.glob("batch-*.json"))) + 3  # batches 1-2 already shipped
        for i, batch in enumerate(batches):
            out = BATCH_DIR / f"batch-{start + i:02d}.json"
            out.write_text(json.dumps({"status": "planned", "icons": batch}, indent=2, ensure_ascii=False) + "\n")
            print(f"wrote {out.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
