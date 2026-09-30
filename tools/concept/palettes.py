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

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "design" / "art-direction" / "concept"

PALETTES = {
    "a": {
        "name": "COLD MILITARY STEEL",
        "mood": ["DESATURATED STEELS AND COOL BLUES, LIKE A 1997 MILITARY SIM RENDER.",
                 "SERIOUS AND GRITTY; COLOUR IS RESERVED FOR ACCENTS, GLOWS AND BULLETS."],
        "factions": [
            ("UTC HULL", "1c2229 353f4a 56626f 7d8a97 aab5bf dde4ea"),
            ("UTC ACCENTS", "1f3f7a 2f6bd8 7fb0ff b85a12 f08c24 ffd08a"),
            ("VRELL CHITIN", "140a18 2e1433 4d2152 6e3a6e 94608a c290ae"),
            ("VRELL GLOW", "0c3f33 1f8f6e 44f0b8 7a1640 ff3f99 ffc0dd"),
            ("ASCENDANCY", "08080a 16161b 26262e 3a3a44 555562 7a7a88"),
            ("ASCEND. GOLD", "4a3208 7d560f b8861e e0b440 f6d77a fff2c4"),
            ("UI PANELS", "0e1116 1b222b 2b3642 3f4d5c 6a7d8f a9bccb"),
            ("UI TEXT", "0f2a16 2f8a46 7cff9a ffd24a ff7a2a ff3b30"),
            ("ENEMY SHOTS", "200018 a0106a ff3fa8 ffb0dc ffffff ff8a00"),
            ("PLAYER SHOTS", "0a2a5a 2f7ff0 8fd0ff e8f8ff 9affd0 ffffff"),
        ],
        "settings": [
            ("EARTH ORBIT", "05070f 0f2344 1f4a7a 3f78a8 8fb4d0 e6eef6"),
            ("EARTH SURFACE", "0e1a12 22402a 3f6a3a 7a8a52 b0a476 d9d2b0"),
            ("MARS", "1a0b08 3d1a10 6e3420 a4583a cf8a5e f0c9a0"),
            ("EUROPA (ICE)", "020a14 06243a 0b4a66 1f7a94 5fb8c8 c8f4f8"),
            ("ASTEROID BELT", "0a0a0c 1f1d1c 3a3532 5e5650 8c8278 c4bab0"),
            ("JOVIAN", "140c08 3a2214 6e4428 a8703c d8a868 f4dcb0"),
            ("ALIEN SPACE", "06020c 1a0826 3a1050 6a2080 a050b0 f0a0f0"),
        ],
    },
    "b": {
        "name": "90S NEON CGI",
        "mood": ["SATURATED, GLOSSY, VIOLET-SHADOWED CHROME. LOUD 1998 CD-ROM COVER ENERGY.",
                 "EVERYTHING GLOWS; NEEDS CAREFUL BULLET CONTRAST RULES."],
        "factions": [
            ("UTC HULL", "121632 2a3068 4e5aa0 8a96d0 c8d0f4 ffffff"),
            ("UTC ACCENTS", "0050ff 00a8ff 7ff0ff ff2a6a ff7a2a ffe04a"),
            ("VRELL CHITIN", "1a0020 40004a 6e0a78 a020a8 d050d0 ff9aff"),
            ("VRELL GLOW", "003a20 00a060 00ff9a 600080 c000ff f0a0ff"),
            ("ASCENDANCY", "000000 100818 201430 34244a 4c3a68 6c5890"),
            ("ASCEND. GOLD", "402000 804000 c07000 ffa800 ffd84a ffffa0"),
            ("UI PANELS", "06061a 101438 1c2460 283890 4058c8 80a0ff"),
            ("UI TEXT", "00ffff 00ff66 ffff00 ff00aa ff4400 ffffff"),
            ("ENEMY SHOTS", "300030 c000c0 ff40ff ffc0ff ffffff ffff40"),
            ("PLAYER SHOTS", "002060 0060ff 00c0ff a0ffff ffffff 40ff80"),
        ],
        "settings": [
            ("EARTH ORBIT", "000010 001050 0030a0 2070e0 60b0ff c0f0ff"),
            ("EARTH SURFACE", "001008 003818 007030 40a040 a0d060 f0ffa0"),
            ("MARS", "200000 600800 a02000 e04800 ff8830 ffd080"),
            ("EUROPA (ICE)", "000818 002050 004890 0080c0 20d0e0 b0ffff"),
            ("ASTEROID BELT", "080410 201830 403850 686080 9890b0 d0d0e8"),
            ("JOVIAN", "200808 602010 a04818 e08020 ffb840 fff0a0"),
            ("ALIEN SPACE", "100010 300040 600080 a000c0 e040ff ffb0ff"),
        ],
    },
    "c": {
        "name": "WARM CINEMATIC",
        "mood": ["TEAL SHADOWS, AMBER LIGHT, WARM GREYS: A 90S SCI-FI FILM TRANSFERRED TO SPRITES.",
                 "MOODY AND COHESIVE; ORANGE/TEAL CONTRAST CARRIES READABILITY."],
        "factions": [
            ("UTC HULL", "1a2224 34403f 5a6560 8a918a bcbdb2 ece6d6"),
            ("UTC ACCENTS", "0f4c5c 1f8a99 7fd0cc 8a3a10 d8762a ffc27a"),
            ("VRELL CHITIN", "140c10 2e1a20 4e2c30 74443e 9c6a58 caa088"),
            ("VRELL GLOW", "083a3a 0f8a86 3ff0e0 6a1a08 ff5a1a ffd0a0"),
            ("ASCENDANCY", "0a0908 1a1714 2c2722 423a32 5e544a 837666"),
            ("ASCEND. GOLD", "3c2606 6e4810 a8721e d8a23c f2cc74 fff0c8"),
            ("UI PANELS", "121414 202424 323838 4a5250 6e7872 a0aaa2"),
            ("UI TEXT", "ffb347 ffd98a fff4dc 6fe0d0 ff6a3a ff3a2a"),
            ("ENEMY SHOTS", "2a0808 b8200a ff5a1a ffb080 fff0e0 ff2a6a"),
            ("PLAYER SHOTS", "06303a 0fa0b0 6ff0e8 d8fffa ffffff ffd84a"),
        ],
        "settings": [
            ("EARTH ORBIT", "04080c 0c1e2a 1c3c4c 3a6670 7aa8a4 e0e8dc"),
            ("EARTH SURFACE", "10140c 283220 4a5634 7a7a4a b0a06a e6d6a8"),
            ("MARS", "1c0c06 42200e 74381a b0602c e09450 ffd6a0"),
            ("EUROPA (ICE)", "040c10 0a2228 144448 266e6c 52a8a0 c0ece0"),
            ("ASTEROID BELT", "0c0a08 221e1a 3e3630 625648 8e7e6a c4b49c"),
            ("JOVIAN", "1a0e06 42260e 744418 b0702a e0a650 fff0c0"),
            ("ALIEN SPACE", "0a0610 221230 402050 6a3070 a45890 f0a8c0"),
        ],
    },
}

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
        path = OUT / f"palette-r01-{key}.png"
        make_sheet(key).convert("RGB").save(path, optimize=True)
        print("wrote", path.relative_to(ROOT))


if __name__ == "__main__":
    main()
