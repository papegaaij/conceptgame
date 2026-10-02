#!/usr/bin/env python3
"""Production art: the fireball size ladder up to large (design/art-direction, animation rules:
explosions; the chosen ladder of explosions-r09-a).

Outputs (assets/sprites/, additive, premultiplied on black):
  explosion-tiny_0..11.png     24x24, 12 frames (Skitter)
  explosion-small_0..11.png    40x40, 12 frames (Needler, loot targets)
  explosion-medium_0..13.png   64x64, 14 frames
  explosion-large_0..13.png    96x96, 14 frames (the ship's death), with the shockwave ring
  design/art-direction/concept/explosions-final-r12-a.png/.gif

Every frame of the round-09 fireball (tools/concept/vfx_r09.py: explosion_frames9, white flash ->
fireball -> cooling smoke, sparks and chunks) with the seeds of the chosen sheet; each rung
shares one palette.

Run: python3 tools/art/explosions.py [--review]   (~30 s)
"""
import sys
from concurrent.futures import ProcessPoolExecutor

from PIL import Image

import artkit
from artkit import DESIGN, sprite

import vfx_r09 as v9  # noqa: E402  (concept script, imported unchanged)

SCRIPT = "explosions.py"
SOURCE = artkit.source_note(SCRIPT)
# rung -> (size px, frames, seed of the chosen sheet, colours)
LADDER = {"tiny": (24, 12, 10, 32), "small": (40, 12, 11, 40), "medium": (64, 14, 12, 48),
          "large": (96, 14, 13, 48)}


def render(rung):
    size, frames, seed, colours = LADDER[rung]
    raw = v9.explosion_frames9(size, frames, seed)
    return artkit.quantize_set([artkit.additive(f) for f in raw], colours)


def build():
    with ProcessPoolExecutor() as pool:
        for rung, frames in zip(LADDER, pool.map(render, LADDER)):
            artkit.write_frames(f"explosion-{rung}", frames, SOURCE)


def review():
    sets = {rung: artkit.load_frames(f"explosion-{rung}") for rung in LADDER}
    rows = [(f"{rung.upper()}", frames, 2 if frames[0].width <= 40 else 1, True) for rung, frames in sets.items()]
    sheet = artkit.review_sheet("EXPLOSION LADDER - FINAL FRAMES", rows, width=1400)
    gif = []
    longest = max(len(f) for f in sets.values())
    for i in range(longest + 4):
        img = Image.new("RGBA", (24 + 40 + 64 + 96 + 50, 106), artkit.PLATE)
        x = 10
        for frames in sets.values():
            if i < len(frames):
                img = artkit.add_light(img, frames[i], (x, 53 - frames[0].height // 2))
            x += frames[0].width + 10
        gif.append(sprite.enlarge(img, 3))
    artkit.save_review(sheet, gif, DESIGN / "art-direction" / "concept", "explosions", fps=15)


if __name__ == "__main__":
    if "--review" not in sys.argv[1:]:
        build()
    review()
