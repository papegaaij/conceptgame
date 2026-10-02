#!/usr/bin/env python3
"""Production art: the Vrell `small` bullets of Acts 1-2 (design/enemies, bullet readability
rules; art direction, readability rule 1).

Outputs (assets/sprites/, drawn with normal blending above every layer):
  orb_0..3.png      15x15 standard orb (9 px body, the Needler's thorn): magenta FF40FF ring on a
                    300030 rim, white core pulsing over the 4 frames
  needle_0..15.png  23x23 yellow FFFF40 needle, 16 headings: needle_k points k x 22.5 degrees
                    clockwise from straight down (an angle set, each heading drawn on its own)
  design/enemies/concept/enemy-bullets-final-r12-a.png/.gif

The bullets are the chosen round-09 light fields (tools/concept/vfx_r09.py: b_orb, b_needle) in
the Vrell colours. The needle is brightened so the fast class glows as strongly as the orb: its
deep band under the yellow ring is a saturated gold instead of the concept's C8AA00, and its halo
keeps only its inner part at 2/3 opacity (the wide faint band, yellow at 1/3 over the dark
backdrops, read as a dull olive rim). The body keeps 1-bit alpha; the orb's soft outer glow is
stepped to four translucency levels (artkit.stepped_alpha).

Run: python3 tools/art/enemy_bullets.py [--review]   (a few seconds)
"""
import sys

import numpy as np
from PIL import Image

import artkit
from artkit import DESIGN, TAU, sprite

import vfx_r09 as v9  # noqa: E402  (concept script, imported unchanged)

SCRIPT = "enemy_bullets.py"
SOURCE = artkit.source_note(SCRIPT)
VRELL = v9.FAC["vrell"]
NEEDLE = dict(VRELL, needle_deep=(255, 200, 0))
NEEDLE_HALO = 0.2                 # halo alpha the needle keeps (at 2/3); fainter is dropped
ORB_RADIUS = 4.5
ORB_FRAMES = 4
NEEDLE_HEADINGS = 16
COLOURS = 24


def halo(img, cut):
    """Only the inner glow around the body, as one 2/3 translucency step: halo pixels at least
    ``cut`` opaque; the body is untouched."""
    a = np.array(img)
    alpha = a[..., 3]
    soft = alpha < 255
    alpha[soft & (alpha >= cut * 255)] = 170
    alpha[soft & (alpha < cut * 255)] = 0
    a[alpha == 0] = 0
    return Image.fromarray(a, "RGBA")


def build():
    orbs = [v9.b_orb(VRELL, ORB_RADIUS, TAU * k / ORB_FRAMES) for k in range(ORB_FRAMES)]
    needles = artkit.angle_set(lambda heading, _: halo(v9.b_needle(NEEDLE, angle=heading), NEEDLE_HALO),
                               NEEDLE_HEADINGS)
    for name, frames in (("orb", orbs), ("needle", needles)):
        frames = artkit.quantize_set([artkit.stepped_alpha(f) for f in frames], COLOURS)
        artkit.write_frames(name, frames, SOURCE)


def review():
    orbs = artkit.load_frames("orb")
    needles = artkit.load_frames("needle")
    sheet = artkit.review_sheet("VRELL BULLETS - FINAL SPRITES", [
        ("ORB, CORE PULSE", orbs, 6, False),
        ("NEEDLE, 16 HEADINGS (CLOCKWISE FROM DOWN)", needles, 3, False),
        ("1X", orbs + needles, 1, False)])
    rng = np.random.default_rng(4)
    shots = [(rng.uniform(10, 150), rng.uniform(-40, 160), rng.uniform(0, TAU)) for _ in range(14)]
    gif = []
    for i in range(32):
        img = Image.new("RGBA", (160, 160), (20, 28, 60, 255))
        for j, (x, y, a) in enumerate(shots):
            dx, dy = -np.sin(a) * 2.5, np.cos(a) * 2.5
            px, py = (x + dx * i) % 170 - 5, (y + dy * i) % 200 - 20
            if j % 2:
                k = int(round(a / TAU * NEEDLE_HEADINGS)) % NEEDLE_HEADINGS
                sprite.paste_center(img, needles[k], px, py)
            else:
                sprite.paste_center(img, orbs[(i // 2 + j) % ORB_FRAMES], px, py)
        gif.append(sprite.enlarge(img, 3))
    artkit.save_review(sheet, gif, DESIGN / "enemies" / "concept", "enemy-bullets", fps=20)


if __name__ == "__main__":
    if "--review" not in sys.argv[1:]:
        build()
    review()
