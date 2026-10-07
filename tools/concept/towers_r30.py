#!/usr/bin/env python3
"""Concept round 30 - placeholder kit for the perspective towers' renderer test (M5 part B, B2).

Not production art: flat stand-ins in the parallax B palette (UI PANELS ramp, windows in JOVIAN and
EARTH ORBIT), made to check the runtime projection of design/tech/architecture (tower pieces) in a
scratch copy of a level, never in the repository's level data. Writes into <out_dir>:
  city-streets.png      480x960 opaque ground tile: dark blocks, 8 px cross streets every 80 px,
                        a 140 px central avenue (x 170-310) and a lane at x 80 and x 400
  tower-h<NN>-<v>.png   roofs for heights 0.30 0.50 0.70 0.90 1.10 1.35 (two variants each), drawn
                        at the roof's scale k = 6 / (6 - h): footprint 62x64 times k, rounded
  wall-low.png, wall-mid.png, wall-tall.png
                        24 px wide wall textures with 3, 6 and 10 storeys of window rows (4 px
                        each), bottom row = the foot; few lit windows
and prints the backdrop YAML (pieces and placements, one tower per city block on both sides of
the avenue, t0..t1 s at the given ground speed) for the scratch level.
The capture design/art-direction/concept/towers-capture-r30-a.png was taken from the game running
that scratch level (Level 04's first section with these images, see concept/prompts.md).

Run: python3 tools/concept/towers_r30.py <out_dir> [--t0 3] [--t1 24] [--speed 120] [--seed 30]
"""
import argparse
from pathlib import Path
import sys

import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
from render.palette import B  # noqa: E402

W = 480
TILE_H = 960
BLOCK = 80          # ground px from one cross street to the next
STREET = 8
FOOT = (62, 64)     # a tower's footprint
CAMERA = 6.0
HEIGHTS = (0.30, 0.50, 0.70, 0.90, 1.10, 1.35)
# The blocks' x ranges: two on each side of the central avenue.
BLOCKS = ((6, 74), (86, 154), (326, 394), (406, 474))
PAL = B["UI PANELS"]


def scale(h):
    return CAMERA / (CAMERA - h)


def wall_for(h):
    return "wall-low" if h < 0.6 else "wall-mid" if h < 1.0 else "wall-tall"


def streets():
    rng = np.random.default_rng(5)
    arr = np.zeros((TILE_H, W, 3))
    arr[:] = PAL[0]
    arr *= (0.85 + 0.3 * rng.random((TILE_H, W)))[..., None]
    yy, xx = np.mgrid[0:TILE_H, 0:W]
    road = ((yy % BLOCK) < STREET) | ((xx >= 170) & (xx < 310)) | (np.abs(xx - 80) < 6) | (np.abs(xx - 400) < 6)
    arr[road] = PAL[1]
    lane = ((np.abs(xx - 240) < 1) & ((yy // 10) % 2 == 0))
    arr[lane] = np.array(PAL[3]) * 0.8
    lamps = np.zeros((TILE_H, W))
    for lx in (172, 308):
        for ly in range(0, TILE_H, 40):
            d2 = (xx - lx) ** 2 + (yy - ly) ** 2
            lamps += np.exp(-d2 / 40.0)
    arr += lamps[..., None] * np.array(B["JOVIAN"][4]) * 0.5
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGB").convert("RGBA")


def roof(h, variant):
    k = scale(h)
    w, hh = round(FOOT[0] * k), round(FOOT[1] * k)
    img = Image.new("RGBA", (w, hh), PAL[1] + (255,))
    d = ImageDraw.Draw(img)
    d.rectangle([3, 3, w - 4, hh - 4], fill=PAL[2] + (255,))
    d.line([3, 3, w - 4, 3], fill=PAL[3] + (255,))             # lit parapet (light from the top left)
    d.line([3, 3, 3, hh - 4], fill=PAL[3] + (255,))
    rng = np.random.default_rng(int(h * 100) * 7 + variant)
    for _ in range(1 + variant):                               # plant boxes and AC units
        ax, ay = int(rng.integers(6, w - 18)), int(rng.integers(6, hh - 18))
        d.rectangle([ax, ay, ax + 9, ay + 7], fill=PAL[1] + (255,))
        d.line([ax, ay, ax + 9, ay], fill=PAL[4] + (255,))
    if variant == 1:
        d.ellipse([w // 2 - 8, hh // 2 - 8, w // 2 + 8, hh // 2 + 8], outline=PAL[4] + (255,))   # a pad ring
    if h > 1.0:
        d.rectangle([w - 9, 5, w - 6, 8], fill=(255, 50, 60, 255))  # beacon
    return img


def wall(floors):
    width, storey = 24, 4
    img = Image.new("RGBA", (width, floors * storey), tuple(int(c * 1.25) for c in PAL[1]) + (255,))
    px = img.load()
    rng = np.random.default_rng(floors)
    for f in range(floors):
        top = (floors - 1 - f) * storey                       # image rows count from the top
        for c in range(0, width, 4):
            lit = rng.random() < 0.16
            colour = (B["JOVIAN"][4] if rng.random() < 0.7 else B["EARTH ORBIT"][5]) if lit else PAL[0]
            colour = tuple(int(v * 0.8) for v in colour) if lit else colour
            for y in (top + 1, top + 2):
                for x in (c + 1, c + 2):
                    px[x, y] = colour + (255,)
    return img


def main():
    p = argparse.ArgumentParser()
    p.add_argument("out")
    p.add_argument("--t0", type=float, default=3)
    p.add_argument("--t1", type=float, default=24)
    p.add_argument("--speed", type=float, default=120)
    p.add_argument("--seed", type=int, default=30)
    a = p.parse_args()
    out = Path(a.out)
    out.mkdir(parents=True, exist_ok=True)
    streets().save(out / "city-streets.png")
    for name, floors in (("wall-low", 3), ("wall-mid", 6), ("wall-tall", 10)):
        wall(floors).save(out / f"{name}.png")
    pieces, placed = [], []
    for h in HEIGHTS:
        for v in (0, 1):
            pid = f"tower-h{round(h * 100):03d}-{'ab'[v]}"
            roof(h, v).save(out / f"{pid}.png")
            pieces.append(f"    {pid}: {{layer: ground, size: [{FOOT[0]}, {FOOT[1]}],"
                          f" tower: {{height: {h}, wall: {wall_for(h)}}}}}")
    rng = np.random.default_rng(a.seed)
    t = a.t0
    while t <= a.t1:
        for x0, x1 in BLOCKS:
            if rng.random() < 0.12:                           # a park: no tower
                continue
            h = HEIGHTS[min(len(HEIGHTS) - 1, int(rng.uniform(0.25, 1.0) ** 1.4 * len(HEIGHTS)))]
            pid = f"tower-h{round(h * 100):03d}-{'ab'[int(rng.integers(2))]}"
            placed.append(f"    - {{piece: {pid}, t: {t:.3f}, x: {(x0 + x1) / 2:g}}}")
        t += BLOCK / a.speed
    print("  # pieces")
    print("\n".join(pieces))
    print("  # placed")
    print("\n".join(placed))


if __name__ == "__main__":
    main()
