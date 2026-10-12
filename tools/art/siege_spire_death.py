#!/usr/bin/env python3
"""Production art: the Siege Spire's death (design/enemies/bosses/siege-spire, *Death*: "the spire cracks
open, chained explosions over 3 s, the canopy above withers and light breaks through"; M5 part H batch,
concept round 36, step H5). Straight to production (H8 = a: no a/b for the death), from the production
free spire of tools/art/siege_spire.py (assets/sprites/siege-spire-free_0, the airborne pose the Spire
dies in, phase 3), as the Brood Carrier's and the Gorgon Frigate's deaths are made from their own sprites.

Over the boss's 3 s death chain (`death_seconds`) the game plays, centred where the free spire was
(its pivot, the core at the sprite's middle):
  siege-spire-death_0..3   the spire cracking open: lime seams of the core's light run out from the core
                           along the six petals and branch, brighter frame by frame (0.0-1.5 s, the
                           game's chained explosions over it)
  siege-spire-death_4..7   it breaks: six petal chunks (60 degree sectors round the core, cut along
                           the seams) fly apart and burn down to embers (1.5-3.0 s), the core's light
                           gone; nothing is left (the stump stays on the ground, the game draws it)
All eight 244 x 244 (the free frame's size), one 48-colour palette, 1-bit alpha; drawn as the free
spire's frame is (not additive). The shockwave's debris (siege-spire-debris) and the dust ring are the
lift's; the canopy's withering is the Level 14 backdrop's (tools/art/backdrop_l14.py: canopy-tear) and
the game's (step H6).

Outputs: assets/sprites/siege-spire-death_0..7 (with --assets); the review
design/enemies/bosses/siege-spire/concept/siege-spire-death-final-r36-a.png/.gif (the frames, and the
death over the arena's stump on the Level 14 backdrop's halt).

Run: python3 tools/art/siege_spire_death.py [--assets]   (~5 s; after siege_spire.py --assets)
"""
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

import artkit
from artkit import DESIGN, SPRITES, TAU
from loot_targets import crumbled  # noqa: E402  (tools/art: the break-apart's burn-down)

SCRIPT = "siege_spire_death.py"
SOURCE = artkit.source_note(SCRIPT, "M5 part H batch")
CONCEPT = DESIGN / "enemies" / "bosses" / "siege-spire" / "concept"
SIZE = 244
FRAMES = 8
LIME = np.array([200, 255, 90], float)
EMBER = np.array([255, 150, 60], float)
SEED = 1477


def seams(rng):
    """The cracks: from the core out along the six petals, each with two branches: [(points, width)]."""
    c = SIZE / 2
    out = []
    for k in range(6):
        a = k * TAU / 6 + TAU / 12 + rng.uniform(-0.12, 0.12)
        pts, x, y, ang = [(c, c)], c, c, a
        for _ in range(16):
            ang += rng.normal(0, 0.18)
            x, y = x + np.cos(ang) * 6, y + np.sin(ang) * 6
            pts.append((x, y))
        out.append((pts, 3))
        for j in (5, 10):
            bx, by = pts[j]
            ba = a + rng.choice([-1, 1]) * rng.uniform(0.5, 1.0)
            sub = [(bx, by)]
            for _ in range(6):
                ba += rng.normal(0, 0.25)
                bx, by = bx + np.cos(ba) * 5, by + np.sin(ba) * 5
                sub.append((bx, by))
            out.append((sub, 2))
    return out


def crack_layer(share, rng_seed=SEED):
    """The seams shown up to ``share`` of their length, as a mask (1 on a seam's core, 0.5 its rim)."""
    rng = np.random.default_rng(rng_seed)
    img = Image.new("L", (SIZE * 2, SIZE * 2), 0)
    d = ImageDraw.Draw(img)
    for pts, w in seams(rng):
        n = max(2, int(len(pts) * share))
        xy = [(x * 2, y * 2) for x, y in pts[:n]]
        d.line(xy, fill=128, width=w * 2 + 4)
        d.line(xy, fill=255, width=w * 2)
    return np.array(img.resize((SIZE, SIZE), Image.BOX)).astype(np.float64) / 255


def frames():
    free = np.array(Image.open(SPRITES / "siege-spire-free_0.png").convert("RGBA")).astype(np.float64)
    yy, xx = np.mgrid[0:SIZE, 0:SIZE].astype(np.float64)
    c = SIZE / 2
    ang = (np.arctan2(yy - c, xx - c) - TAU / 12) % TAU
    sector = (ang // (TAU / 6)).astype(int)                 # the six petal chunks, cut along the seams
    burn = np.random.default_rng(SEED + 1).random((SIZE, SIZE))
    out = []
    for k in range(FRAMES):
        img = free.copy()
        if k < 4:                                           # cracking open
            share = (k + 1) / 4
            m = crack_layer(share)
            glow = 0.6 + 0.4 * share
            img[..., :3] = img[..., :3] * (1 - m[..., None] * glow) + LIME * (m[..., None] * glow)
            img[..., 3] = np.where(img[..., 3] > 0, 255, 0)
            out.append(img)
            continue
        u = (k - 3) / 4                                     # breaking apart: 0.25 .. 1.0
        canvas = np.zeros_like(free)
        m = crack_layer(1.0)
        src = free.copy()
        lime = (src[..., 1] > 200) & (src[..., 0] > 140) & (src[..., 2] < 160)      # the core's light: gone
        src[lime, :3] = np.array([58, 44, 40]) + 40 * burn[lime, None] * np.array([1.0, 0.5, 0.2])
        src[..., :3] *= 1 - 0.35 * u                        # the tissue charring
        src[..., :3] = src[..., :3] * (1 - m[..., None] * 0.6) + EMBER * (m[..., None] * 0.6) * (1 - u)   # cooling seams
        for s in range(6):
            a = s * TAU / 6 + TAU / 6
            dist = 44 * u ** 0.8
            dx, dy = int(round(np.cos(a) * dist)), int(round(np.sin(a) * dist))
            piece = np.where((sector == s)[..., None], src, 0).astype(np.uint8)
            eaten = crumbled(Image.fromarray(piece, "RGBA"), int(round(9 * (u - 0.25) ** 1.3 * 1.45)))
            pa = np.array(eaten).astype(np.float64)
            alive = pa[..., 3] > 0
            inner = np.array(Image.fromarray((alive * 255).astype(np.uint8)).filter(ImageFilter.MinFilter(3))) > 0
            edge = alive & ~inner                           # the burning rim
            pa[edge, :3] = pa[edge, :3] * 0.3 + EMBER * 0.7
            ys, xs = np.nonzero(alive)
            ty, tx = ys + dy, xs + dx
            ok = (ty >= 0) & (ty < SIZE) & (tx >= 0) & (tx < SIZE)
            canvas[ty[ok], tx[ok]] = pa[ys[ok], xs[ok]]
        out.append(canvas)
    imgs = [Image.fromarray(np.clip(f, 0, 255).astype(np.uint8), "RGBA") for f in out]
    return artkit.quantize_set(imgs, 48)


def review(fs):
    artkit.REVIEW_ROUND = "r36"
    rows = [("CRACKING OPEN 0-3 (0-1.5 S)", fs[:4], 1, False), ("BREAKING APART, BURNING DOWN 4-7 (1.5-3 S)", fs[4:], 1, False)]
    sheet = artkit.review_sheet("SIEGE SPIRE - DEATH (PRODUCTION)", rows, width=1100, batch="M5 part H batch")
    gif = []
    bg = Image.new("RGBA", (300, 300), (14, 16, 26, 255))
    for k, f in enumerate(fs):
        for _ in range(3):
            g = bg.copy()
            g.alpha_composite(f, (28, 28))
            gif.append(g)
    artkit.save_review(sheet, gif, CONCEPT, "siege-spire-death", fps=8)


def main(args):
    fs = frames()
    if "--assets" in args:
        artkit.write_frames("siege-spire-death", fs, SOURCE)
        print(f"assets: sprites/siege-spire-death_0..{len(fs) - 1}")
    review(fs)


if __name__ == "__main__":
    main(sys.argv[1:])
