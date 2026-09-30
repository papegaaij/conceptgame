#!/usr/bin/env python3
"""Concept round 02 - player ship and Rook's craft at 960x540 scale, palette B.

Outputs:
  design/player/ship/concept/player-ship-r02-a.png     AF-12 Stormhawk (ship A), 48x48, palette B
  design/player/wingmen/concept/rook-craft-r02-a.png   Rook's craft (ship C geometry), "Ember"
  design/player/wingmen/concept/rook-craft-r02-b.png   Rook's craft (ship C geometry), "Jade"

Each sheet: source render, native sprite (1x, 2x, 6x) with its palette, banking frames (4x),
the sprite in game over a palette-B coastline (2x and 1:1). Rook's sheets add an
"at a glance" panel with Rook next to the player, in colour and in greyscale.
Run: python3 tools/concept/ships_r02.py
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageOps

sys.path.insert(0, str(Path(__file__).resolve().parent))
from render import models, raster, sdf, sprite, terrain  # noqa: E402
from render.config import ROOT, SHIP_SIZE, WINGMAN_SIZE  # noqa: E402
from render.palette import B  # noqa: E402

SHIP_OUT = ROOT / "design" / "player" / "ship" / "concept"
WING_OUT = ROOT / "design" / "player" / "wingmen" / "concept"
FACTOR = 8
EXTENT = 2.3
BANK = np.radians(28)
COLORS = 28


def f(rgb):
    return tuple(c / 255 for c in rgb)


# Rook's schemes: both keep the UTC family look but differ from the player in value and hue.
# The player is a light blue-white hull with cyan and orange accents.
ROOK_SCHEMES = {
    "a": ("EMBER", {"hull": f(B["UTC HULL"][2]), "hull_dark": f(B["UTC HULL"][0]),
                    "accent": f(B["UTC ACCENTS"][4]), "accent2": f(B["UTC ACCENTS"][5]),
                    "glass": f(B["EARTH ORBIT"][1]), "engine": f(B["MARS"][4])},
          ["DARK SLATE HULL, ORANGE WING PANELS, YELLOW NOSE AND TIPS, WARM ENGINES.",
           "READS DARKER AND WARMER THAN THE PLAYER: A VALUE AND HUE DIFFERENCE."]),
    "b": ("JADE", {"hull": f(B["EARTH SURFACE"][5]), "hull_dark": f(B["EARTH SURFACE"][1]),
                   "accent": f(B["EARTH SURFACE"][2]), "accent2": f(B["UTC ACCENTS"][5]),
                   "glass": f(B["EARTH ORBIT"][1]), "engine": f(B["PLAYER SHOTS"][5])},
          ["PALE LIME HULL, DEEP GREEN PANELS, YELLOW TIPS, GREEN ENGINES.",
           "SAME BRIGHTNESS AS THE PLAYER BUT A CLEARLY DIFFERENT HUE."]),
}


def render(model_fn, native, palette, bank=0.0):
    scene, mats = model_fn(bank, palette=palette)
    hi = sdf.render(scene, mats, (native * FACTOR, native * FACTOR), EXTENT)
    return hi, sprite.make_sprite(hi, FACTOR, COLORS)


def frames(model_fn, native, palette):
    hi, centre = render(model_fn, native, palette)
    _, left = render(model_fn, native, palette, -BANK)
    _, right = render(model_fn, native, palette, BANK)
    return hi, (left, centre, right)


def checker(w, h, cell=8):
    img = Image.new("RGBA", (w, h))
    d = ImageDraw.Draw(img)
    for y in range(0, h, cell):
        for x in range(0, w, cell):
            c = (30, 34, 46) if (x // cell + y // cell) % 2 == 0 else (38, 42, 56)
            d.rectangle([x, y, x + cell - 1, y + cell - 1], fill=c + (255,))
    return img


def label(img, x, y, text, color=raster.LABEL_DIM):
    raster.draw_text(img, x, y, text, color)


def coast(w, h, seed=21):
    stops = terrain.coast_stops_from(B["EARTH ORBIT"], B["EARTH SURFACE"])
    return terrain.recede(terrain.earth_coast(w, h, seed=seed, period=False, stops=stops,
                                              cell=96), amount=0.35, darken=0.72)


def bolt(img, x, y):
    d = ImageDraw.Draw(img)
    c = B["PLAYER SHOTS"]
    d.rectangle([x - 1, y - 7, x + 1, y + 6], fill=c[2] + (230,))
    d.rectangle([x, y - 5, x, y + 4], fill=c[4] + (255,))


def place(bg, sp, x, y, shadow=True):
    if shadow:
        sh = sprite.shadow_of(sp, opacity=0.5, blur=1.2, scale=0.85)
        sprite.paste_center(bg, sh, x + 18, y + 26)
    sprite.paste_center(bg, sp, x, y)


def in_game(sp, escort=None):
    w, h = 210, 270
    bg = coast(w, h)
    x, y = w / 2, 190
    if escort is not None:
        x = w / 2 + 22
        place(bg, escort, w / 2 - 44, y + 26)
    place(bg, sp, x, y)
    for i, yy in enumerate((30, 72, 114)):
        bolt(bg, int(x) - 9, yy + (i % 2) * 6)
        bolt(bg, int(x) + 9, yy + (i % 2) * 6)
    return bg


def sheet(title, hi, frames_, notes, game, extra=None):
    """Presentation sheet. ``extra`` = (label, image) replaces the 1:1 in-game view."""
    left, centre, right = frames_
    n = centre.width
    R = n * FACTOR
    bs = 3                                   # banking frame enlargement
    by = 50 + R + 22
    slot_x = 16 + 3 * (n * bs + 12) + 6
    slot_label, slot = extra if extra is not None else ("IN GAME 1:1", game)
    bottom = max(by + 12 + n * bs, by + 12 + slot.height, 50 + game.height * 2)
    img = raster.sheet(1270, bottom + 50, title, "CONCEPT ROUND 02 - 960X540")
    label(img, 16, 38, f"SOURCE RENDER ({R} PX, SDF RAY-MARCHED)")
    src = checker(R, R)
    src.alpha_composite(sprite.to_image(hi))
    img.alpha_composite(src, (16, 50))
    x2 = 16 + R + 16
    label(img, x2, 38, f"NATIVE {n}X{n}, {len(sprite.palette_of(centre))} COLOURS (6X)")
    big = checker(n * 6, n * 6, cell=12)
    big.alpha_composite(sprite.enlarge(centre, 6))
    img.alpha_composite(big, (x2, 50))
    x3 = x2 + n * 6 + 12
    label(img, x3, 50, "1X")
    img.alpha_composite(centre, (x3, 62))
    label(img, x3, 70 + n, "2X")
    img.alpha_composite(sprite.enlarge(centre, 2), (x3, 82 + n))
    py = 50 + n * 6 + 14
    label(img, x2, py, "SPRITE PALETTE")
    d = ImageDraw.Draw(img)
    for i, c in enumerate(sprite.palette_of(centre)):
        x = x2 + (i % 14) * 16
        y = py + 12 + (i // 14) * 16
        d.rectangle([x, y, x + 13, y + 13], fill=tuple(c) + (255,))
    label(img, 16, by, f"BANKING FRAMES ({bs}X): LEFT / CENTRE / RIGHT")
    for i, s in enumerate((left, centre, right)):
        cell = checker(n * bs, n * bs)
        cell.alpha_composite(sprite.enlarge(s, bs))
        img.alpha_composite(cell, (16 + i * (n * bs + 12), by + 12))
    label(img, slot_x, by, slot_label)
    img.alpha_composite(slot, (slot_x, by + 12))
    gx = 830
    label(img, gx, 38, "IN GAME (2X) OVER A PALETTE-B COASTLINE")
    img.alpha_composite(sprite.enlarge(game, 2), (gx, 50))
    ny = max(by + 12 + n * bs, by + 12 + slot.height) + 14
    for i, line in enumerate(notes):
        raster.draw_text(img, 16, ny + i * 12, line, raster.LABEL)
    return img


def glance_panel(player, rook):
    """Rook beside the player at 1x, in colour and in greyscale (value check)."""
    w, h = 170, 130
    bg = coast(w, h, seed=34)
    place(bg, rook, 50, 74)
    place(bg, player, 116, 60)
    grey = ImageOps.grayscale(bg.convert("RGB")).convert("RGBA")
    panel = Image.new("RGBA", (w * 2 + 10, h), raster.SHEET_BG)
    panel.alpha_composite(bg, (0, 0))
    panel.alpha_composite(grey, (w + 10, 0))
    return panel


def main():
    SHIP_OUT.mkdir(parents=True, exist_ok=True)
    WING_OUT.mkdir(parents=True, exist_ok=True)
    pal = B.ship_colors()
    hi, player = frames(models.ship_a_model, SHIP_SIZE, pal)
    img = sheet("AF-12 STORMHAWK - SHIP A, PALETTE B, 48X48", hi, player,
                ["CHOSEN SHIP A RE-RENDERED FOR 960X540 (48X48) IN PALETTE B '90S NEON CGI'.",
                 "HULL C8D0F4, SHADOWS 2A3068, ACCENTS 00A8FF / FF7A2A, ENGINES 00C0FF."],
                in_game(player[1]))
    path = SHIP_OUT / "player-ship-r02-a.png"
    img.convert("RGB").save(path, optimize=True)
    print("wrote", path.relative_to(ROOT))

    for key, (name, colours, notes) in ROOK_SCHEMES.items():
        rhi, rook = frames(models.ship_c_model, WINGMAN_SIZE, colours)
        panel = glance_panel(player[1], rook[1])
        img = sheet(f"ROOK'S CRAFT - SCHEME {key.upper()}: {name} ({WINGMAN_SIZE}X{WINGMAN_SIZE})",
                    rhi, rook, notes, in_game(player[1], escort=rook[1]),
                    extra=("AT A GLANCE 1:1 - ROOK LEFT, PLAYER RIGHT; COLOUR / GREYSCALE",
                           panel))
        path = WING_OUT / f"rook-craft-r02-{key}.png"
        img.convert("RGB").save(path, optimize=True)
        print("wrote", path.relative_to(ROOT))


if __name__ == "__main__":
    main()
