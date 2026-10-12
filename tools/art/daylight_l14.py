#!/usr/bin/env python3
"""Production art, two variants: Level 14's daylight section (design/campaign/act-2-homefront/
level-14-siege-spire, section 6 "Daylight" and *Act end*; M5 part H batch, concept round 36, step H2;
an a/b, decision H8 = a, pending the user's review). No concept: designed to the README and to the
chosen Geneva scene (scene-geneva-r10-a: its city, its canopy membrane, its canopy shadow).

After the Spire's death the scroll ramps back up to 140 px/s for 12 s, with no enemies and no music.
On a real-seconds timeline from the death (the arena's halt freezes everything keyed to the scroll):
the canopy withers (its teal veins die to grey-brown, 0-2 s), tears open cell by cell (1-7 s; the
torn edges curl back), its shreds fade (to ~10 s), and the light comes up on the old town, the lake
and the stump.

  a  "Golden hour": low warm sunlight from the west (over the lake, the left of the screen) breaking
     through the tears in long diagonal beams; the city graded warm and bright, long shadows kept
  b  "Pale dawn": a cool, even morning light; a haze lying over the city that burns off from the
     lake inward; no beams, the city graded clear and cool, lower contrast

Functions for the backdrop step (tools/art/backdrop_l14.py imports them; nothing is written to assets
here), ``t`` = real seconds since the death, 0..12:
  ``grade(variant, rgb, t)``           the ground (a float/uint8 RGB array) graded from the canopy's dusk
                                       to the variant's daylight
  ``canopy_layer(variant, w, h, t)``   the withering canopy on `high-air` (RGBA, straight alpha, 6 steps,
                                       tiling vertically with period h)
  ``light_layers(variant, w, h, t)``   (over, add): a's beams as an additive layer (premultiplied on
                                       black, GL_ONE), b's haze as an alpha layer drawn over the ground
                                       (4 steps)
The backdrop step decides which of these become baked tiles (the grade on section 6's tiles) and which
the game draws on the real clock (canopy, beams, haze: a few frames, e.g. one per second).

Outputs (review only): design/campaign/act-2-homefront/level-14-siege-spire/concept/
  daylight-r36-a.png, daylight-r36-b.png   a still of the old town and the lake 8 s after the death
  (480x540, 1x) and the timeline at 0, 3, 6, 9 and 12 s (half size)

Run: python3 tools/art/daylight_l14.py [a] [b]   (~1 min)
"""
import sys
from functools import lru_cache

import numpy as np
from PIL import Image

import artkit
from artkit import DESIGN

import scenes_r06 as s6  # noqa: E402  (concept scripts, imported unchanged)
import scenes_r10 as s10  # noqa: E402
from render import raster  # noqa: E402

REVIEW_ROUND = "r36"
CONCEPT = DESIGN / "campaign" / "act-2-homefront" / "level-14-siege-spire" / "concept"
VARIANTS = {"a": "A - GOLDEN HOUR", "b": "B - PALE DAWN"}
W, H = 480, 540
SUN = np.array([-0.82, -0.57])                  # a: the light's direction on screen (from the west, low)
WITHER, TEAR0, TEAR1, FADE = 2.0, 1.0, 7.0, 10.0


def ramp(t, t0, t1):
    s = float(np.clip((t - t0) / (t1 - t0), 0, 1))
    return s * s * (3 - 2 * s)


# =========================================================================== the ground

def grade(variant, rgb, t):
    """The ground from the canopy's dusk (t=0) to the variant's daylight (from ~2 s, full by ~9 s)."""
    x = np.asarray(rgb, dtype=np.float64)[..., :3] / 255
    k = ramp(t, 1.5, 9.0)
    lum = x @ np.array([0.3, 0.55, 0.15])
    creep = np.clip((x[..., 1] - np.maximum(x[..., 0], x[..., 2] * 0.8)) * 6, 0, 1)[..., None]
    x = x * (1 - creep * k) + (lum[..., None] * np.array([1.05, 0.9, 0.72])) * creep * k   # the creep dies
    if variant == "a":
        lifted = np.clip(x * 1.55 + 0.02, 0, 1) ** 0.88
        warm = lifted * np.array([1.12, 0.96, 0.74])
        warm = warm + np.array([0.06, 0.03, 0.0]) * (lum[..., None] > 0.35)     # sunlit roofs glow
        out = x * (1 - k) + warm * k
    else:
        lifted = np.clip(x * 1.55 + 0.06, 0, 1) ** 0.9
        cool = lifted * np.array([0.92, 0.98, 1.08])
        flat = cool * 0.8 + lum[..., None] * 0.2 + 0.05                         # lower contrast
        out = x * (1 - k) + flat * k
    return np.clip(out * 255, 0, 255)


class _City(s10.GenevaScene):
    """Only the round-10 city texture (with its overgrowth, without the rotunda's landmark)."""

    def _landmarks10(self, img, per):
        return img


@lru_cache(maxsize=None)
def city(per=H):
    c = object.__new__(_City)
    try:
        img = c._city(per)
    except AttributeError:                       # the overgrowth needs the scene's state: leave it out
        c._overgrowth10 = lambda img, per: img
        img = c._city(per)
    return np.array(img.convert("RGB"))


# =========================================================================== the canopy

@lru_cache(maxsize=None)
def _cells(w, h):
    d1, d2, idx = s6.voronoi_periodic(w, h, 70, 1431)        # the round-08/10 canopy's cells
    rng = np.random.default_rng(1436)
    tear = rng.uniform(TEAR0, TEAR1, int(idx.max()) + 1)
    tear[rng.random(len(tear)) < 0.12] = 99.0                # a few hang on as shreds until the fade
    return d1, d2 - d1, idx, tear


def canopy_layer(variant, w=W, h=H, t=0.0):
    """The canopy membrane (veined plum cells, teal veins, thin panes, torn holes; <= 40 % opacity),
    withering and tearing open cell by cell from the death."""
    d1, edge, idx, tear = _cells(w, h)
    vein = np.clip(1 - edge / 5.0, 0, 1)
    core = np.clip(1 - edge / 1.6, 0, 1)
    holes = (idx % 3 == 0)
    pane = np.where(holes, 0.0, 0.16)
    wither = ramp(t, 0.0, WITHER)
    age = t - tear[idx]                                         # how long ago this cell tore
    open_ = np.clip(age / 1.2, 0, 1)
    curl = d1 > (1 - open_) * 60                               # the tear spreads from the cell's middle
    pane = np.where(curl & (age > 0), 0.0, pane)
    rim = np.where((age > 0) & ~curl & (d1 > (1 - open_) * 60 - 4), 0.34, 0.0)   # the curled edge
    fade = 1 - ramp(t, 6.0, FADE)
    a = np.maximum(vein * 0.4 * (1 - 0.5 * open_), np.maximum(pane, rim)) * fade
    plum = np.array((58, 30, 70)) * (1 - wither) + np.array((70, 56, 50)) * wither
    teal = np.array((40, 170, 130)) * (1 - wither) + np.array((110, 96, 78)) * wither
    col = np.zeros((h, w, 3))
    col[:] = plum
    col = col * (1 - core[..., None]) + teal * core[..., None]
    col = np.where(rim[..., None] > 0, np.array((96, 76, 60)) * (0.6 + 0.4 * wither), col)
    if variant == "a":                                          # the low sun catches the torn edges
        col = col + np.array((60, 36, 6)) * ramp(t, 2.0, 6.0) * (rim[..., None] > 0)
    s = 5
    q = np.floor(np.clip(a, 0, 1) * s / 0.4 + 0.5) / s * 0.4
    return Image.fromarray(np.dstack([np.clip(col, 0, 255), q * 255]).astype(np.uint8), "RGBA")


# =========================================================================== the light

@lru_cache(maxsize=None)
def _streak_noise(w, h):
    rng = np.random.default_rng(1437)
    n = raster.fbm(w, h, 48, 1438, octaves=3, period=True)
    return n, rng


def light_layers(variant, w=W, h=H, t=0.0):
    """(over, add). a: additive beams through the canopy's tears along the low sun's direction, and a
    warm wash; over is empty. b: a haze over the city burning off from the lake (the left) inward;
    add is a faint cool wash."""
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64)
    over = np.zeros((h, w, 4))
    add = np.zeros((h, w, 3))
    if variant == "a":
        d1, edge, idx, tear = _cells(w, h)
        gap = ((t - tear[idx]) > 0.4).astype(np.float64)
        u = xx * SUN[1] - yy * SUN[0]                       # across the beams
        v = xx * SUN[0] + yy * SUN[1]
        n, _ = _streak_noise(w, h)
        beams = 0.5 + 0.5 * np.sin(u / 23.0 + 2.0 * np.sin(u / 71.0)) * np.cos(u / 9.0 + 1.3)
        beams = np.clip((beams - 0.35) / 0.65, 0, 1) ** 1.5
        beams *= np.clip(0.55 + 0.45 * np.sin(v / 140.0 + n * 3.0), 0, 1)
        lit = s6.pblur_mask(gap, 9.0) if gap.any() else gap
        k = ramp(t, 2.0, 8.0)
        strength = (0.42 * beams * (0.35 + 0.65 * lit) + 0.08 * lit) * k
        add = np.array((255, 196, 120))[None, None, :] * artkit.ordered_dither(strength / 0.5, 5)[..., None] * 0.5
    else:
        n = raster.fbm(w, h, 64, 1439, octaves=4, period=True)
        front = -60 + 620 * ramp(t, 1.0, 11.0)                # px from the left the haze has burnt off
        thick = np.clip((xx - front) / 160.0 + (n - 0.5) * 0.9, 0, 1) * (1 - ramp(t, 8.0, 12.0) * 0.7)
        a = thick * 0.4 * ramp(t, 0.0, 1.5)
        q = artkit.ordered_dither(a / 0.4, 4) * 0.4
        over[..., :3] = np.array((214, 222, 232)) * (0.9 + 0.1 * n[..., None])
        over[..., 3] = q * 255
        add = np.array((40, 48, 60))[None, None, :] * ramp(t, 2.0, 9.0) * np.ones((h, w, 1)) * 0.6
    add_img = artkit.additive(Image.fromarray(np.dstack([np.clip(add, 0, 255), np.full((h, w), 255)]).astype(np.uint8), "RGBA"))
    return Image.fromarray(over.astype(np.uint8), "RGBA"), add_img


# =========================================================================== review

def frame(variant, t, scroll=0):
    rgb = city()
    rgb = np.roll(rgb, scroll, axis=0)
    img = Image.fromarray(grade(variant, rgb, t).astype(np.uint8), "RGB").convert("RGBA")
    over, add = light_layers(variant, W, H, t)
    img.alpha_composite(over)
    img = artkit.add_light(img, add)
    img.alpha_composite(canopy_layer(variant, W, H, t))
    return img


def review(variant):
    artkit.REVIEW_ROUND = REVIEW_ROUND
    still = frame(variant, 8.0, int(140 * 8))
    strip = []
    for t in (0.0, 3.0, 6.0, 9.0, 12.0):
        f = frame(variant, t, int(140 * t)).resize((W // 2, H // 2), Image.NEAREST)
        raster.draw_text(f, 4, H // 2 - 12, f"T = {t:.0f} S", raster.LABEL)
        strip.append(f)
    rows = [(f"8 S AFTER THE DEATH (1X; THE GROUND GRADED, THE LIGHT, THE CANOPY ON HIGH-AIR)", [still], 1, False),
            ("THE 12 S TIMELINE FROM THE DEATH (HALF SIZE): WITHERING, TEARING, FADING", strip, 1, False)]
    sheet = artkit.review_sheet(f"LEVEL 14 DAYLIGHT, VARIANT {VARIANTS[variant]}", rows, width=1500,
                                batch="M5 part H batch")
    CONCEPT.mkdir(parents=True, exist_ok=True)
    png = CONCEPT / f"daylight-{REVIEW_ROUND}-{variant}.png"
    sheet.convert("RGB").save(png, optimize=True)
    print(f"review: {png.relative_to(artkit.ROOT)}")


def main(args):
    for v in [a for a in args if a in VARIANTS] or list(VARIANTS):
        review(v)


if __name__ == "__main__":
    main(sys.argv[1:])
