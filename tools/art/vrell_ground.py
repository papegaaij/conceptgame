#!/usr/bin/env python3
"""Production art: the Vrell ground units of Level 02, the Spine Turret (design/enemies/ground;
M4 part B batch).

Outputs (assets/sprites/, sizes from the parts' data.yaml):
  spine-turret_0..31.png      40x40, `orientation: 32 angles`: the whole turret with its barrel
                              at 32 headings, k x 11.25 degrees clockwise from straight down
                              (heading 0 aims down the screen, its facing); every heading is its
                              own render with the key light fixed, the body never turning
  spine-turret-stump.png      40x40, the scorched stump it leaves on the ground layer
  design/enemies/ground/spine-turret/concept/spine-turret-final-r15-a.png/.gif

The model and its role colours are the chosen round-04 Spine Turret (tools/concept/enemies_r04.py,
render/enemy_models.spine_turret_a, whose `anim` is the barrel's aim). The stump is that model's
root collar and a burst, darkened bulb without petals or barrel.

Run: python3 tools/art/vrell_ground.py [--review]   (~20 s)
"""
import sys
from concurrent.futures import ProcessPoolExecutor

import numpy as np
import yaml
from PIL import Image

import artkit
from artkit import DESIGN, TAU, sprite

import enemies_r04 as e4  # noqa: E402  (concept script, imported unchanged)
from render import enemy_models as em  # noqa: E402
from render.sdf import rotate_z, sd_capsule, sd_ellipsoid, sd_sphere, union  # noqa: E402

SCRIPT = "vrell_ground.py"
SOURCE = artkit.source_note(SCRIPT, "M4 part B batch")
EXTENT = 2.3
HEADINGS = 32
COLOURS = 32


def size_of(slug):
    data = yaml.safe_load((DESIGN / "enemies" / "ground" / slug / "data.yaml").read_text(encoding="utf-8"))
    return tuple(data["size"])


def render_turret(k):
    # The concept model's aim turns the barrel clockwise as seen on screen, like the game's headings.
    scene, mats = e4.R04["spine-turret-a"][4](anim=TAU * k / HEADINGS, glow=1.0)
    return artkit.native(*artkit.render_hi(scene, mats, size_of("spine-turret"), EXTENT))


def stump_model():
    """The root collar and a low, burst bulb, scorched: no petals, no barrel."""
    def scene(p):
        roots = [(sd_capsule(rotate_z(p, a), (0, 0.3, -0.05), (0.0, 0.85, -0.2), 0.11, 0.03), em.V_DARK)
                 for a in np.linspace(0.4, 2 * np.pi + 0.4, 5, endpoint=False)]
        shards = [(sd_ellipsoid(rotate_z(p, a), (0, 0.3, 0.05), (0.1, 0.18, 0.12)), em.V_SEAM)
                  for a in np.linspace(0.2, 2 * np.pi + 0.2, 6, endpoint=False)]
        d, m = union(*roots, k=0.08)
        return union((d, m), (sd_sphere(p, (0, 0, 0.0), 0.3), em.V_DARK), *shards, k=0.06)
    return scene, em.vrell_scheme_mats("spine-turret", "a", 0.15)


def render_stump():
    scene, mats = stump_model()
    return artkit.native(*artkit.render_hi(scene, mats, size_of("spine-turret"), EXTENT))


def build():
    with ProcessPoolExecutor() as pool:
        frames = artkit.quantize_set(list(pool.map(render_turret, range(HEADINGS))), COLOURS)
    artkit.write_frames("spine-turret", frames, SOURCE)
    artkit.write_frames("spine-turret-stump", artkit.quantize_set([render_stump()], 16), SOURCE, single=True)


def review():
    frames = artkit.load_frames("spine-turret")
    stump = artkit.load_frames("spine-turret-stump")
    artkit.REVIEW_ROUND = "r15"
    sheet = artkit.review_sheet("SPINE TURRET - FINAL SPRITES", [
        ("BARREL HEADINGS 0-7 (CLOCKWISE FROM DOWN: 8 AIMS LEFT, 24 RIGHT)", frames[:8], 3, False),
        ("HEADINGS 8-15", frames[8:16], 3, False),
        ("HEADINGS 16-23", frames[16:24], 3, False),
        ("HEADINGS 24-31", frames[24:], 3, False),
        ("STUMP", stump, 5, False),
        ("1X", frames[::4] + stump, 1, False)], batch="M4 part B batch")
    w, h = frames[0].size
    gif = []
    for i in range(64):                          # the barrel tracking a ship sweeping past below it
        cell = Image.new("RGBA", (w * 4, h * 3), artkit.PLATE)
        tx = w * 2 + w * 1.6 * np.sin(TAU * i / 64)
        ax, ay = w * 2, h
        angle = np.arctan2(-(tx - ax), (h * 2.5 - ay))      # clockwise from down (screen y down)
        k = int(round(angle / TAU * HEADINGS)) % HEADINGS
        sprite.paste_center(cell, frames[k], ax, ay)
        sprite.paste_center(cell, artkit.load_frames("ship")[2], tx, h * 2.5)
        gif.append(sprite.enlarge(cell, 3))
    artkit.save_review(sheet, gif, DESIGN / "enemies" / "ground" / "spine-turret" / "concept", "spine-turret", fps=16)


if __name__ == "__main__":
    if "--review" not in sys.argv[1:]:
        build()
    review()
