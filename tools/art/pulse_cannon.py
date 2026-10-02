#!/usr/bin/env python3
"""Production art: the Pulse Cannon's `pulse` effects family (design/player/weapons/pulse-cannon).

Outputs (assets/sprites/, additive, premultiplied on black):
  pulse-bolt.png         14x26 bolt (its 4x12 hit box in data.yaml sits in the bright body)
  pulse-muzzle_0..2.png  20x20 muzzle flash, drawn at the front muzzle for two steps each
  pulse-impact_0..3.png  22x22 impact spark burst, fading over its 4 frames
  design/player/weapons/pulse-cannon/concept/pulse-cannon-final-r12-a.png/.gif

The effects are the chosen round-08 2D light fields (tools/concept/vfx_r08.py: energy_bolt,
muzzle_frames, impact_frames), analytic and supersampled 4x; each set shares one palette. The
impact takes the first 4 of the light field's 5-step fade (t = 0, 1/4, 1/2, 3/4): sampled at 4
steps its last frame was the field's end, almost black.

Run: python3 tools/art/pulse_cannon.py [--review]   (a few seconds)
"""
import sys

from PIL import Image

import artkit
from artkit import DESIGN, sprite

import vfx_r08 as v8  # noqa: E402  (concept script, imported unchanged)

SCRIPT = "pulse_cannon.py"
SOURCE = artkit.source_note(SCRIPT)
COLOURS = 32
IMPACT_FRAMES = 4


def build():
    sets = {"pulse-bolt": [v8.energy_bolt(12, 2.6)],
            "pulse-muzzle": v8.muzzle_frames("energy"),
            "pulse-impact": v8.impact_frames("energy", n=IMPACT_FRAMES + 1)[:IMPACT_FRAMES]}
    for name, frames in sets.items():
        frames = artkit.quantize_set([artkit.additive(f) for f in frames], COLOURS)
        artkit.write_frames(name, frames, SOURCE, single=name == "pulse-bolt")


def review():
    bolt = artkit.load_frames("pulse-bolt")
    muzzle = artkit.load_frames("pulse-muzzle")
    impact = artkit.load_frames("pulse-impact")
    sheet = artkit.review_sheet("PULSE CANNON - FINAL EFFECTS", [
        ("BOLT", bolt, 6, True), ("MUZZLE FLASH", muzzle, 6, True), ("IMPACT", impact, 6, True)])
    ship = artkit.load_frames("ship")[2]
    gif = []
    for i in range(30):                  # one game step per GIF frame: a bolt every 6 steps, 15 px per step
        img = Image.new("RGBA", (96, 200), artkit.PLATE)
        img.alpha_composite(ship, (24, 144))
        muzzle_y = 144 + 3
        for k in range(3):
            y = muzzle_y - 15 * (i % 6 + 6 * k)
            img = artkit.add_light(img, bolt[0], (41, y - 13))
        img = artkit.add_light(img, muzzle[(i % 6) // 2], (38, muzzle_y - 12))
        img = artkit.add_light(img, impact[i % 4], (37, 6))
        gif.append(sprite.enlarge(img, 3))
    artkit.save_review(sheet, gif, DESIGN / "player" / "weapons" / "pulse-cannon" / "concept",
                       "pulse-cannon", fps=20)


if __name__ == "__main__":
    if "--review" not in sys.argv[1:]:
        build()
    review()
