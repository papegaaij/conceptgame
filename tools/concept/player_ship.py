#!/usr/bin/env python3
"""Concept round 01 - player ship (AF-12 Stormhawk), three silhouette variants.

Outputs (design/player/ship/concept/):
  player-ship-r01-a.png   variant A, forward-swept wing
  player-ship-r01-b.png   variant B, twin-boom
  player-ship-r01-c.png   variant C, blended wing with canards

Each sheet shows the 256 px source render, the native 32x32 palettised sprite (1x, 2x, 6x),
banking frames (left / centre / right) and the sprite in game over a sample background.
Run: python3 tools/concept/player_ship.py
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
from render import models, raster, sdf, sprite, terrain  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "design" / "player" / "ship" / "concept"

NATIVE = 32          # sprite size in native pixels
FACTOR = 8           # render supersampling
EXTENT = 2.3         # world units across the sprite
BANK = np.radians(28)
COLORS = 24

NOTES = {
    "a": ["FORWARD-SWEPT WING, CANARDS, TWIN ENGINES IN NACELLES.",
          "AGGRESSIVE, NEEDLE-LIKE. WINGTIP GUN RAILS."],
    "b": ["TWIN-BOOM: COCKPIT POD, ENGINE BOOMS, STRAIGHT WING, TAILPLANE.",
          "WIDE, STABLE, WORKHORSE LOOK. VERY READABLE SILHOUETTE."],
    "c": ["BLENDED LIFTING BODY WITH CANARDS AND BURIED ENGINES.",
          "SLEEK AND ADVANCED, THE MOST 'SCI-FI' OF THE THREE."],
}


def render_ship(model_fn, bank=0.0, native=NATIVE, colors=COLORS):
    scene, mats = model_fn(bank)
    hi = sdf.render(scene, mats, (native * FACTOR, native * FACTOR), EXTENT)
    return hi, sprite.make_sprite(hi, FACTOR, colors)


def label(img, x, y, text):
    raster.draw_text(img, x, y, text, raster.LABEL_DIM)


def checker(w, h, a=(30, 34, 46), b=(38, 42, 56), cell=8):
    img = Image.new("RGBA", (w, h))
    d = ImageDraw.Draw(img)
    for yy in range(0, h, cell):
        for xx in range(0, w, cell):
            d.rectangle([xx, yy, xx + cell - 1, yy + cell - 1],
                        fill=(a if (xx // cell + yy // cell) % 2 == 0 else b) + (255,))
    return img


def bolt(img, x, y):
    d = ImageDraw.Draw(img)
    d.rectangle([x - 1, y - 4, x, y + 3], fill=(150, 210, 255, 255))
    d.rectangle([x - 1, y - 2, x, y + 1], fill=(255, 255, 255, 255))


def in_game(sp, variant):
    """Sprite over a sample coastline background, with shadow and a few shots (native size)."""
    w, h = 140, 170
    bg = terrain.recede(terrain.earth_coast(w, h, seed=21, period=False))
    shadow = sprite.shadow_of(sp, opacity=0.5, blur=1.0, scale=0.85)
    sprite.paste_center(bg, shadow, w / 2 + 12, 120 + 16)
    sprite.paste_center(bg, sp, w / 2, 120)
    for i, yy in enumerate((20, 48, 76)):
        bolt(bg, int(w / 2) - 6, yy + (i % 2) * 4)
        bolt(bg, int(w / 2) + 6, yy + (i % 2) * 4)
    return bg


def make_sheet(key):
    name, fn = models.PLAYER_SHIPS[key]
    hi, sp = render_ship(fn)
    _, left = render_ship(fn, -BANK)
    _, right = render_ship(fn, BANK)

    img = raster.sheet(1040, 560, f"AF-12 STORMHAWK - VARIANT {key.upper()}: {name.upper()}",
                       "CONCEPT ROUND 01")
    # source render
    label(img, 16, 38, "SOURCE RENDER (256 PX, SDF RAY-MARCHED)")
    src = checker(256, 256)
    src.alpha_composite(sprite.to_image(hi))
    img.alpha_composite(src, (16, 50))
    # native sprite
    label(img, 290, 38, f"NATIVE {NATIVE}X{NATIVE}, {len(sprite.palette_of(sp))} COLOURS (6X)")
    big = checker(NATIVE * 6, NATIVE * 6, cell=12)
    big.alpha_composite(sprite.enlarge(sp, 6))
    img.alpha_composite(big, (290, 50))
    label(img, 494, 50, "1X")
    img.alpha_composite(sp, (494, 62))
    label(img, 494, 104, "2X")
    img.alpha_composite(sprite.enlarge(sp, 2), (494, 116))
    # banking frames
    label(img, 16, 322, "BANKING FRAMES (4X): LEFT / CENTRE / RIGHT")
    for i, s in enumerate((left, sp, right)):
        cell = checker(NATIVE * 4, NATIVE * 4, cell=8)
        cell.alpha_composite(sprite.enlarge(s, 4))
        img.alpha_composite(cell, (16 + i * 140, 334))
    # palette
    label(img, 290, 256, "SPRITE PALETTE")
    for i, c in enumerate(sprite.palette_of(sp)):
        x = 290 + (i % 12) * 16
        y = 268 + (i // 12) * 16
        ImageDraw.Draw(img).rectangle([x, y, x + 13, y + 13], fill=tuple(c) + (255,))
    # in game
    game = in_game(sp, key)
    label(img, 590, 38, "IN GAME (2X) OVER A SAMPLE BACKGROUND")
    img.alpha_composite(sprite.enlarge(game, 2), (590, 50))
    label(img, 884, 38, "1:1")
    img.alpha_composite(game, (884, 50))
    # notes
    for i, line in enumerate(NOTES[key]):
        raster.draw_text(img, 16, 490 + i * 12, line, raster.LABEL)
    raster.draw_text(img, 16, 530, "PIPELINE: RENDER 8X -> BOX DOWNSAMPLE -> 1-BIT ALPHA -> "
                     f"MEDIAN-CUT {COLORS} COLOURS", raster.LABEL_DIM)
    return img


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for key in models.PLAYER_SHIPS:
        path = OUT / f"player-ship-r01-{key}.png"
        make_sheet(key).convert("RGB").save(path, optimize=True)
        print("wrote", path.relative_to(ROOT))


if __name__ == "__main__":
    main()
