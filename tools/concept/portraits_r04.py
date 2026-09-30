#!/usr/bin/env python3
"""Concept round 04 - briefing portraits, style B refined: comm-screen pixel portrait with
retained colour.

Outputs:
  design/story/characters/<slug>/concept/portrait-r04-a.png   for okafor, rook, varga, vorne
  design/story/characters/concept/cast-r04-a.png              all four side by side

The user chose style B of round 03 "but with a small bit more colours retained to make it look
less flat". Same busts and sheet layout as round 03 (tools/concept/portraits_r03.py); the 72 px
pixel portrait now keeps each character's own colours:
  - the channel tint (teal for the CDF, amber for Vorne's enemy channel) is blended over the
    character's colours as a unifying overlay instead of replacing them with a mono ramp;
  - saturated accents (Rook's Ember orange, Varga's auburn hair and holo green, Vorne's gold and
    teal shimmer) get a lighter tint so they stay recognisable;
  - a larger palette (36 colours instead of 6 tones) with softer ordered dithering gives the
    faces depth and form;
  - interference lines, the dark screen with its dot grid, the outline and the CRT scanlines at
    2x stay, so it still reads as a comm feed.
Run: python3 tools/concept/portraits_r04.py   (~40 s)
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import portraits_r03 as r03  # noqa: E402
from render import raster, sprite  # noqa: E402
from render.config import ROOT  # noqa: E402

ROUND = "CONCEPT ROUND 04"
N = 72
COLORS = 36                 # palette of the 72 px portrait (round 03: 5 tint tones + accents)
DITHER = 14 / 255           # amplitude of the ordered dither before quantising (soft)
TINT = {"cdf": 0.3, "enemy": 0.42}          # how strongly the channel tint overlays the colours
ACCENT_TINT = {"cdf": 0.08, "enemy": 0.16}  # lighter overlay on saturated accents

r03.STYLE_TITLE["b2"] = "STYLE B: COMM-SCREEN PORTRAIT, RETAINED COLOUR"
r03.STYLE_NOTE["b2"] = [
    "72 PX PIXEL PORTRAIT: CHANNEL TINT BLENDED OVER THE",
    "CHARACTER'S OWN COLOURS, 36 COLOURS, SOFT ORDERED DITHER,",
    "ACCENTS KEPT, OUTLINE, INTERFERENCE, CRT SCANLINES AT 2X.",
    "CDF CHANNELS TEAL; ENEMY CHANNEL (VORNE) AMBER.",
]


def ramp_at(ramp, t):
    """Continuous lookup in a dark->light tint ramp."""
    x = np.clip(t, 0, 1) * (len(ramp) - 1)
    i = np.minimum(np.floor(x).astype(int), len(ramp) - 2)
    f = (x - i)[..., None]
    return ramp[i] * (1 - f) + ramp[i + 1] * f


def style_b2(slug, busts):
    bust, keyonly = busts
    c = r03.CAST[slug]
    ch = c["channel"]
    ramp = np.array([raster.hexrgb(h) for h in r03.RAMPS[ch]], dtype=np.float64) / 255
    small = sprite.downsample(bust, r03.RENDER // N)
    rgb, a = np.clip(small[..., :3], 0, 1), small[..., 3] > 0.5
    lum = rgb @ np.array([0.3, 0.55, 0.15])
    # the character's colours, lifted a little so dark skin and black suits keep their form
    base = np.clip(rgb * 1.18 + 0.03, 0, 1)
    grey = base.mean(-1, keepdims=True)
    base = np.clip(grey + (base - grey) * 1.15, 0, 1)      # a touch more saturation
    tint = ramp_at(ramp, np.clip((lum - 0.02) / 0.75, 0, 1) ** 0.85)
    krgb = sprite.downsample(keyonly, r03.RENDER // N)[..., :3]
    mx, mn = krgb.max(-1), krgb.min(-1)
    sat = (mx - mn) / np.maximum(mx, 1e-6)
    bluish = (krgb[..., 2] > krgb[..., 0]) & (krgb[..., 2] > krgb[..., 1])
    accent = a & (sat > 0.5) & (mx > 0.35) & ~bluish
    w = np.where(accent, ACCENT_TINT[ch], TINT[ch])[..., None]
    col = base * (1 - w) + tint * w
    # soft ordered dither, then a shared palette for the figure
    thr = r03.BAYER4[np.arange(N)[:, None] % 4, np.arange(N)[None, :] % 4] - 0.5
    col = np.clip(col + thr[..., None] * DITHER, 0, 1)
    fig = Image.fromarray((col * 255 + 0.5).astype(np.uint8), "RGB").convert("RGBA")
    fig.putalpha(Image.fromarray((a * 255).astype(np.uint8)))
    fig = np.array(sprite.quantize(fig, COLORS)).astype(np.float64)[..., :3]
    # screen: dark tinted background with a dot grid, outline around the figure
    yy, xx = np.mgrid[0:N, 0:N]
    grid = (xx % 6 == 0) & (yy % 6 == 0)
    bg = np.where(grid[..., None], ramp[1] * 255 * 0.8, ramp[0] * 255)
    img = np.where(a[..., None], fig, bg)
    pad = np.pad(a, 1)
    touch = (pad[:-2, 1:-1] | pad[2:, 1:-1] | pad[1:-1, :-2] | pad[1:-1, 2:]) & ~a
    img[touch] = ramp[0] * 255 * 0.4
    # interference: tinted rolling band over the chest, one noise line, two jittered rows
    rng = np.random.default_rng(sum(map(ord, slug)))
    band = 50 + (sum(map(ord, slug)) % 14)
    img[band:band + 3] = np.clip(img[band:band + 3] * 0.75 + ramp[4] * 255 * 0.35, 0, 255)
    for y in rng.choice(np.arange(N - 8, N), 1, replace=False):
        img[y, rng.random(N) > 0.6] = ramp[-2] * 255
    for y in list(rng.choice(np.arange(2, 12), 1)) + list(rng.choice(np.arange(44, 66), 1)):
        img[y] = np.roll(img[y], int(rng.integers(1, 3)), axis=0)
    # a faint overall channel glow ties everything together
    img = img * 0.94 + ramp[3] * 255 * 0.06
    hud = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGB").convert("RGBA")
    brief = sprite.enlarge(hud, 2)
    arr = np.array(brief).astype(np.float64)
    arr[1::2, :, :3] *= 0.74                      # CRT scanlines at 2x
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA"), hud


def main():
    results = {}
    for slug in r03.CAST:
        brief, hud = style_b2(slug, r03.render_bust(slug))
        results[slug] = (brief, hud)
        out = r03.CHAR_DIR / slug / "concept" / "portrait-r04-a.png"
        r03.character_sheet(slug, "b2", brief, hud, ROUND).convert("RGB").save(out, optimize=True)
        print("wrote", out.relative_to(ROOT))
    out = r03.CHAR_DIR / "concept" / "cast-r04-a.png"
    r03.cast_sheet("b2", results, ROUND).convert("RGB").save(out, optimize=True)
    print("wrote", out.relative_to(ROOT))


if __name__ == "__main__":
    main()
