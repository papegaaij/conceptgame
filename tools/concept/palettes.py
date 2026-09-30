#!/usr/bin/env python3
"""Concept round 01 - three palette / mood options.

Outputs (design/art-direction/concept/):
  palette-r01-a.png   "Cold Military Steel"
  palette-r01-b.png   "90s Neon CGI"
  palette-r01-c.png   "Warm Cinematic"

Each sheet shows 6-step ramps per faction, UI, bullets and per setting (dark background ->
highlight), with hex codes, plus the player ship and a Vrell dart rendered in that palette.
Run: python3 tools/concept/palettes.py
"""
import sys
from pathlib import Path

from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
from render import models, raster, sdf, sprite  # noqa: E402
from render.config import out_path  # noqa: E402
from render.palette import PALETTES  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "design" / "art-direction" / "concept"

SW, SH, GAP = 44, 22, 2


def ramp_of(pal, group, name):
    for n, codes in pal[group]:
        if n == name:
            return [raster.hexrgb(c) for c in codes.split()]
    raise KeyError(name)


def f3(rgb):
    return tuple(c / 255 for c in rgb)


def draw_rows(img, x, y, rows, heading):
    raster.draw_text(img, x, y, heading, raster.ACCENT)
    y += 14
    d = ImageDraw.Draw(img)
    for name, codes in rows:
        raster.draw_text(img, x, y + 7, name, raster.LABEL)
        for i, code in enumerate(codes.split()):
            sx = x + 100 + i * (SW + GAP)
            d.rectangle([sx, y, sx + SW - 1, y + SH - 1], fill=raster.hexrgb(code) + (255,))
            raster.draw_text(img, sx + 4, y + SH + 3, code.upper(), raster.LABEL_DIM)
        y += SH + 14
    return y


def previews(pal):
    hull = ramp_of(pal, "factions", "UTC HULL")
    acc = ramp_of(pal, "factions", "UTC ACCENTS")
    shots = ramp_of(pal, "factions", "PLAYER SHOTS")
    orbit = ramp_of(pal, "settings", "EARTH ORBIT")
    chitin = ramp_of(pal, "factions", "VRELL CHITIN")
    glow = ramp_of(pal, "factions", "VRELL GLOW")
    ship_pal = {"hull": f3(hull[4]), "hull_dark": f3(hull[1]), "accent": f3(acc[1]),
                "accent2": f3(acc[4]), "glass": f3(orbit[1]), "engine": f3(shots[2])}
    scene, mats = models.ship_a_model(palette=ship_pal)
    ship = sprite.make_sprite(sdf.render(scene, mats, (256, 256), 2.3), 8, 24)
    vpal = {"chitin": f3(chitin[3]), "chitin2": f3(glow[1]), "glow": f3(glow[2]),
            "glow2": f3(glow[4])}
    scene, mats = models.vrell_dart_model(palette=vpal)
    dart = sprite.make_sprite(sdf.render(scene, mats, (224, 224), 2.3), 8, 24)
    return ship, dart


def make_sheet(key):
    pal = PALETTES[key]
    img = raster.sheet(1000, 540, f"PALETTE {key.upper()}: {pal['name']}", "CONCEPT ROUND 01")
    for i, line in enumerate(pal["mood"]):
        raster.draw_text(img, 16, 38 + i * 11, line, raster.LABEL)
    draw_rows(img, 16, 70, pal["factions"], "FACTIONS, UI AND BULLETS")
    y = draw_rows(img, 520, 70, pal["settings"], "SETTINGS (BACKGROUND DARK -> HIGHLIGHT)")
    # previews on a setting-coloured backdrop
    ship, dart = previews(pal)
    earth = ramp_of(pal, "settings", "EARTH SURFACE")
    raster.draw_text(img, 520, y + 4, "PREVIEW (3X): AF-12 AND VRELL DART IN THIS PALETTE",
                     raster.ACCENT)
    n = raster.fbm(420, 120, 32, 5, octaves=4, period=False)
    box = raster.to_rgba_image(raster.ramp([(0.0, earth[0]), (0.45, earth[1]), (0.75, earth[2]),
                                            (1.0, earth[3])], n))
    box.alpha_composite(sprite.shadow_of(sprite.enlarge(ship, 3), 0.45, 2), (40 + 18, 12 + 24))
    box.alpha_composite(sprite.enlarge(ship, 3), (40, 12))
    box.alpha_composite(sprite.enlarge(dart, 3), (260, 16))
    img.alpha_composite(box, (520, y + 18))
    return img


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for key in PALETTES:
        path = out_path(OUT, f"palette-r01-{key}.png")
        make_sheet(key).convert("RGB").save(path, optimize=True)
        print("wrote", path.relative_to(ROOT))


if __name__ == "__main__":
    main()
