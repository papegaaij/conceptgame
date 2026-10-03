#!/usr/bin/env python3
"""Production art: the CDF bomber of the Airstrike special (design/player/specials, Airstrike;
M4 part D batch).

Outputs (assets/sprites/, size from design/player/specials/data.yaml `bomber_size`):
  airstrike-bomber_0..3.png      56x64, facing up (it flies straight up the screen): the bomber
                                 with its two engine flames, 4 frames of flame flicker (loop
                                 0-1-2-3). The nose sits 3 px below the top edge, the flames end
                                 about 6 px above the bottom; the hull's centre is 4 px above the
                                 sprite's centre
  airstrike-bomber-shadow.png    56x64, the hull's silhouette shrunk to 85 % about the sprite
                                 centre and softened, near-black at up to 50 % opacity in four
                                 steps: drawn as is (no extra tint) on the ground layer, offset
                                 from the bomber (the concept used +34, +48 px)
  design/player/specials/concept/airstrike-bomber-final-r17-a.png/.gif

The bomber is the chosen round-09 model (tools/concept/vfx_r09.py `bomber_model`, imported
unchanged): a heavy straight-winged twin-engine bomber, grey hull, UTC blue wing bands, dark
canopy, glowing nozzles; rendered at 8x with the default key light, 26 px per model unit (the
concept's 64 px render was 58 px across the wings; this fits the 56 px width). The flames are 2D
light fields (vfx_r08's canvas, like the concept's flame frames) under the nacelles, the hull drawn
over them; the flame's soft edge is stepped to four translucency levels and one 40-colour palette
covers the four frames.

Run: python3 tools/art/airstrike_bomber.py [--review]   (~10 s)
"""
import sys

import numpy as np
from PIL import Image, ImageFilter

import artkit
from artkit import DESIGN, sprite

import allies_r16 as ar16  # noqa: E402  (concept scripts, imported unchanged)
import vfx_r08 as v8  # noqa: E402
import vfx_r09 as v9  # noqa: E402
from render import sdf  # noqa: E402

SCRIPT = "airstrike_bomber.py"
SOURCE = artkit.source_note(SCRIPT, "M4 part D batch")
artkit.REVIEW_ROUND = "r17"
BATCH = "M4 part D batch"
CONCEPT = DESIGN / "player" / "specials" / "concept"
SIZE = (56, 64)
SCALE = 26.0                     # px per model unit
NOSE = 0.97                      # model y of the nose tip
TOP = 3                          # px from the sprite's top edge to the nose
CY = SIZE[1] / 2 - TOP - NOSE * SCALE   # px the hull centre sits above the sprite centre
NACELLE_X, EXHAUST_Y = 0.46, -0.53       # model units
FLICKER = [(13.0, 1.00), (15.5, 0.92), (12.0, 0.85), (14.5, 1.05)]   # flame length px, brightness
SHADOW_OFFSET = (34, 48)


def body():
    """The hull at native size (unquantised), its centre CY px above the sprite's centre."""
    scene, mats = v9.bomber_model()
    w, h = SIZE
    hi = sdf.render(scene, mats, (w * 8, h * 8), w / SCALE, center=(0.0, -CY / SCALE), z_top=3.0, steps=140)
    return artkit.native(hi, 8)


def flames(length, bright):
    """The two engine flames as a straight-alpha light field: orange plume, yellow core, white
    nozzle spot (the concept's flame_frames look)."""
    w, h = SIZE
    cv = v8.Canvas(w, h)
    for sx in (-1, 1):
        x = w / 2 + sx * NACELLE_X * SCALE
        y = h / 2 - CY - EXHAUST_Y * SCALE
        cv.add(v9.PS[2], v8.gauss(cv.seg(x, y, x, y + length), 2.1) * 0.85 * bright)
        cv.over(v9.PS[3], v8.solid(cv.seg(x, y, x, y + length * 0.5), 1.2) * 0.9)
        cv.over(v9.WHITE, v8.solid(cv.dist(x, y + 0.8), 1.2))
    return cv.image()


def build():
    hull = body()
    frames = []
    for length, bright in FLICKER:
        img = flames(length, bright)
        img.alpha_composite(hull)
        frames.append(artkit.stepped_alpha(img))
    artkit.write_frames("airstrike-bomber", artkit.quantize_set(frames, 40), SOURCE)
    artkit.write_frames("airstrike-bomber-shadow", [shadow(hull)], SOURCE, single=True)


def shadow(hull):
    """The hull silhouette at 85 % about the sprite centre, blurred, 50 % at most, four steps."""
    w, h = SIZE
    a = hull.getchannel("A")
    small = a.resize((round(w * 0.85), round(h * 0.85)), Image.LANCZOS)
    mask = Image.new("L", SIZE, 0)
    mask.paste(small, ((w - small.width) // 2, (h - small.height) // 2))
    soft = np.array(mask.filter(ImageFilter.GaussianBlur(1.2))) / 255
    alpha = np.round(np.floor(soft * 3 + 0.5) / 3 * 0.5 * 255)
    out = np.zeros((h, w, 4), np.uint8)
    out[..., :3] = (8, 10, 18)
    out[..., 3] = alpha.astype(np.uint8)
    out[out[..., 3] == 0] = 0
    return Image.fromarray(out, "RGBA")


def review():
    frames = artkit.load_frames("airstrike-bomber")
    shade = artkit.load_frames("airstrike-bomber-shadow")[0]
    sheet = artkit.review_sheet("AIRSTRIKE BOMBER - FINAL SPRITES", [
        ("FLAME FLICKER, FACING UP", frames, 4, False),
        ("GROUND SHADOW (DRAWN AS IS, OFFSET +34, +48)", [shade], 4, False),
        ("1X", frames + [shade], 1, False)], batch=BATCH)

    # three bombers fly over a Luna road at 600 px/s (30 px a frame at 20 fps), shadows below
    w, per = 300, 600
    strip = ar16.regolith((w, per), 1702, lambda y: w / 2 + 30 * np.sin(2 * np.pi * y / per), 27)
    strip = np.array(strip)
    gif = []
    xs = [(w / 2 - 90, 0), (w / 2 + 30, 70), (w / 2 - 30, 150)]   # x, start delay in px
    for f in range(30):
        ground = Image.fromarray(np.roll(strip, f * 6, axis=0)[:540], "RGBA")
        for x, delay in xs:
            y = 540 + 40 + delay - f * 30
            spr = frames[f % len(frames)]
            sprite.paste_center(ground, shade, x + SHADOW_OFFSET[0], y + SHADOW_OFFSET[1])
            sprite.paste_center(ground, spr, x, y)
        gif.append(ground)
    artkit.save_review(sheet, gif, CONCEPT, "airstrike-bomber", fps=20)


if __name__ == "__main__":
    if "--review" not in sys.argv[1:]:
        build()
    review()
