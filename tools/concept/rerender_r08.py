#!/usr/bin/env python3
"""Concept round 08 - re-renders of the chosen round-05 Scuttler, Coilwyrm and Leviathan with the
model-space angle renderer.

Outputs (design/enemies/...):
  ground/concept/scuttler-r08-a.{png,gif}
  air/concept/coilwyrm-r08-a.{png,gif}
  space/concept/leviathan-r08-a.{png,gif}

The round-05 sheets were rendered with ``enemy_rigs.AngleSprites``, which evaluates material
patterns (chitin ridges, glowing seams, mottled hide) in screen space, so a seam stayed vertical
at every heading. This script runs the unchanged round-05 builders (tools/concept/enemies_r05.py,
models in render/archetype_models.py) with every angle set of these three units switched to
``enemy_rigs.ModelSpaceAngleSprites``, so the patterns turn with the body. Design, colours,
paths and sheet layouts are identical to round 05; only the round label and file names change.

Run: python3 tools/concept/rerender_r08.py [scuttler] [coilwyrm] [leviathan]
     (no args = all three; ~4-6 min each, independent, so they can run as parallel processes)
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import enemies_r05 as e5  # noqa: E402
from render import enemy_rigs as rig  # noqa: E402
from render.config import ROOT  # noqa: E402

UNITS = {
    "scuttler": ("ground", e5.scuttler),
    "coilwyrm": ("air", e5.coilwyrm),
    "leviathan": ("space", e5.leviathan),
}


def model_space(*angle_sets):
    """Switch existing AngleSprites to the model-space renderer (same parameters, empty cache)."""
    for s in angle_sets:
        s.__class__ = rig.ModelSpaceAngleSprites
        s.cache = {}


def save_unit(category, slug, sheet, frames):
    png = e5.out(category, f"{slug}-r08-a.png")
    sheet.convert("RGB").save(png, optimize=True)
    size = rig.write_gif(frames, e5.out(category, f"{slug}-r08-a.gif"), fps=e5.FPS)
    print(f"wrote {png.relative_to(ROOT)} and .gif ({size / 1e6:.1f} MB, {len(frames)} frames)")


def main(args):
    model_space(e5.coil_head, e5.coil_tail, *e5.coil_segs.values(),
                e5.lev_body, *e5.lev_tail, e5.lev_fluke, *e5.lev_fin.values(),
                e5.scut)
    e5.SUB = "CONCEPT ROUND 08 - 960X540 - MODEL-SPACE RE-RENDER"
    for name, (cat, fn) in UNITS.items():
        if args and name not in args:
            continue
        sheet, frames = fn()
        save_unit(cat, name, sheet, frames)


if __name__ == "__main__":
    main(sys.argv[1:])
