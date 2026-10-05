#!/usr/bin/env python3
"""Production art: the Vrell `medium` bullet, the large pulsing orb (design/enemies, bullet
readability rules: "large pulsing = slow and heavy"; art direction, readability rule 1; M4 part H
batch, concept round 26).

Outputs (assets/sprites/, drawn with normal blending above every layer, like the small orb):
  orb-medium_0..5.png   23x23 large orb (13 px body): magenta FF40FF ring on a 300030 rim, a white
                        core and a darker swirl band, the body swelling by about 1 px and its soft
                        glow breathing over the 6 frames
  design/enemies/concept/enemy-bullet-medium-final-r26-a.png/.gif

The look is the round-09 large orb (tools/concept/vfx_r09.py: b_large) in the Vrell colours of the
final small orb (tools/art/enemy_bullets.py), at the largest bullet body the art direction allows
(13 px). As for the small orb, the body keeps 1-bit alpha and the outer glow is stepped to four
translucency levels (artkit.stepped_alpha); frames share one palette.

Run: python3 tools/art/bullet_medium.py [--review]   (a few seconds)
"""
import sys

import numpy as np
from PIL import Image

import artkit
from artkit import DESIGN, TAU, sprite

import vfx_r09 as v9  # noqa: E402  (concept script, imported unchanged)

SCRIPT = "bullet_medium.py"
BATCH = "M4 part H batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
REVIEW_ROUND = "r26"
VRELL = v9.FAC["vrell"]
RADIUS = 6.5
FRAMES = 6
COLOURS = 24


def build():
    frames = [artkit.stepped_alpha(v9.b_large(VRELL, RADIUS, TAU * k / FRAMES)) for k in range(FRAMES)]
    artkit.write_frames("orb-medium", artkit.quantize_set(frames, COLOURS), SOURCE)


def review():
    artkit.REVIEW_ROUND = REVIEW_ROUND
    large = artkit.load_frames("orb-medium")
    small = artkit.load_frames("orb")
    sheet = artkit.review_sheet("VRELL MEDIUM BULLET - FINAL SPRITES", [
        ("LARGE ORB, PULSE (6 FRAMES, 4 STEPS EACH)", large, 6, False),
        ("NEXT TO THE SMALL ORB (FINAL, ROUND 12)", large[:1] + small[:1], 6, False),
        ("1X", large + small, 1, False)], batch=BATCH)
    rng = np.random.default_rng(9)
    shots = [(rng.uniform(10, 150), rng.uniform(-40, 160), rng.uniform(0, TAU), k % 3 == 0) for k in range(14)]
    gif = []
    for i in range(48):
        img = Image.new("RGBA", (160, 160), (20, 28, 60, 255))
        for j, (x, y, a, heavy) in enumerate(shots):
            speed = 1.6 if heavy else 2.5
            px = (x - np.sin(a) * speed * i) % 170 - 5
            py = (y + np.cos(a) * speed * i) % 200 - 20
            frames = large if heavy else small
            sprite.paste_center(img, frames[(i // 4 + j) % len(frames)], px, py)
        gif.append(sprite.enlarge(img, 3))
    artkit.save_review(sheet, gif, DESIGN / "enemies" / "concept", "enemy-bullet-medium", fps=15)


if __name__ == "__main__":
    if "--review" not in sys.argv[1:]:
        build()
    review()
