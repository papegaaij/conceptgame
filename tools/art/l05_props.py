#!/usr/bin/env python3
"""Production art: Level 05's small props, the concepts chosen in concept round 21
(design/campaign/act-1-first-contact/level-05-crater-nest, "Hazards" and "Secrets and pickups";
design/enemies/ground/polyp-mortar, "Death").

Outputs (assets/sprites/, the sizes of Level 05's data.yaml):
  rock_0..11.png           16x16 the low-gravity rocks (concept a: plain regolith chunks with a
                           bright rim), three shapes of 4 tumble frames each, indexed
                           shape * 4 + tumble; the game picks a shape per rock
  ore-canister_0..2.png    40x28 the stuck sled's ore canister (concept a: a steel ore cylinder,
                           hazard bands, a clamp yoke with the beacon): beacon dark, beacon lit
                           (the clamp hittable), clamp shot
  polyp-mortar-splash_0..2.png  40x40 the Polyp Mortar's acid splash decal on the ground layer
                           (concept b: an etched burn, a scorched pit with a teal-green bubbling
                           pool and a pale ring): fresh, after 1 s, fading
  design/campaign/act-1-first-contact/level-05-crater-nest/concept/l05-props-final-r21-c.png

The models are the concept round's (tools/concept/props_r21.py, imported unchanged). The solid props
are rendered at the quality bar's 8x with the concept's light rim (and scorch on the shot clamp);
the decal is the concept's 2D field at its native size, kept with its soft alpha. One palette per
sprite.

Run: python3 tools/art/l05_props.py [--review]   (a few seconds)
"""
import sys
from concurrent.futures import ProcessPoolExecutor

import numpy as np
from PIL import Image

import artkit
from artkit import DESIGN

import ground_targets as gt  # noqa: E402  (concept script: light rim, scorch)
import props_r21 as r21  # noqa: E402  (concept script, imported unchanged)

SCRIPT = "l05_props.py"
SOURCE = artkit.source_note(SCRIPT, "M4 part E batch")
LEVEL_DIR = DESIGN / "campaign" / "act-1-first-contact" / "level-05-crater-nest"
ROCK_SHAPES = (5, 11, 23)          # the concept's seeds: the tumbling rock and the two other shapes
TUMBLE = (0.0, 0.8, 1.6, 2.4)
ROCK = (16, 16)
CANISTER = (40, 28)
SPLASH_STAGES = (0.0, 0.5, 0.9)


def render(scene, mats, size, rim=True):
    hi, factor = artkit.render_hi(scene, mats, size, float(size[0]), z_top=float(max(size)), steps=150)
    arr = np.array(artkit.native(hi, factor, crisp=60)).astype(np.float64)
    return gt.light_rim(arr) if rim else arr


def to_img(arr):
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")


def rock(job):
    seed, turn = job
    mat = r21.Material((0.66, 0.64, 0.61), metal=0.0, shininess=8, spec=0.08)   # concept a
    return to_img(render(r21.rock_scene(seed, turn, False), [mat, mat], ROCK))


def canister(state):
    """0 beacon dark, 1 beacon lit, 2 clamp shot (scorched as the concept's)."""
    arr = render(r21.canister_a(state < 2), r21.canister_mats(state == 1, "a"), CANISTER)
    if state == 2:
        arr = gt.scorch(arr, [(24, 12, 5), (30, 18, 4)], 21)
    return to_img(arr)


def build():
    with ProcessPoolExecutor() as pool:
        rocks = list(pool.map(rock, [(s, t) for s in ROCK_SHAPES for t in TUMBLE]))
        canisters = list(pool.map(canister, range(3)))
    splashes = [r21.splash_b(s) for s in SPLASH_STAGES]
    for name, frames, colours in (("rock", rocks, 16), ("ore-canister", canisters, 32),
                                  ("polyp-mortar-splash", splashes, 24)):
        done = artkit.quantize_set(frames, colours)
        artkit.write_frames(name, done, SOURCE)
        print(f"{name}: {len(done)} frames {done[0].size}, {artkit.colour_count(done)} colours")


def review():
    artkit.REVIEW_ROUND = "r21"
    rows = [("ROCKS: THREE SHAPES, 4 TUMBLE FRAMES EACH", artkit.load_frames("rock"), 4, False),
            ("ORE CANISTER: BEACON DARK, BEACON LIT, CLAMP SHOT", artkit.load_frames("ore-canister"), 5, False),
            ("ACID SPLASH DECAL: FRESH, AFTER 1 S, FADING", artkit.load_frames("polyp-mortar-splash"), 5, False)]
    sheet = artkit.review_sheet("LEVEL 05 PROPS - FINAL SPRITES", rows, width=900, batch="M4 part E batch")
    png, _ = artkit.review_paths(LEVEL_DIR / "concept", "l05-props", variant="c")
    sheet.convert("RGB").save(png, optimize=True)
    print(f"review: {png.relative_to(artkit.ROOT)}")


if __name__ == "__main__":
    if "--review" not in sys.argv[1:]:
        build()
    review()
