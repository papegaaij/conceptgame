#!/usr/bin/env python3
"""Production art, two variants: Level 13's whiteout (design/campaign/act-2-homefront/level-13-polar-relay,
"The halt" section 4 and "Whiteout (G7 = a)"; M5 part G batch, concept round 35, step G2; an a/b,
decision G9 = a, pending the user's review). No concept: designed to the README and to the chosen
arctic scene's fog and snow (scene-arctic-r09-a).

The whiteout is two layers on the real clock (the scroll is halted, so nothing keyed to the scroll
may drive them): the **banks** on `low-air`, rolling in from both side edges and covering the ground
units there (by the layer model the boats and walkers come out of the white; air units, bullets,
Rook, the player and the edge warnings draw above them), the centre round the relay kept clear; and
the **snow** on `high-air` (at most 40 % of the frame). The peak is t=176-188 with 4 s ramps
(easy: a 6 s peak); `cover` 0..1 is the ramp.

  a  "Rolling banks": soft, billowing cloud banks from both sides, lumpy and lit from the top-left
     (pale crests, blue-grey undersides), their inner edge undulating and rolling slowly inward and
     down; fine snowflakes drifting down with a light wind (the arctic scene's fog, thickened)
  b  "Blizzard curtain": driving spindrift: the edges a near-opaque white sheet with streamers torn off
     it reaching further in, streaking with the wind from the right (down and to the left); the snow
     as short slanted streaks, more of them at the edges

Functions for the backdrop step (tools/art/backdrop_l13.py imports them; nothing is written to assets
here): ``bank_layer(variant, w, h, t, cover)`` and ``snow_layer(variant, w, h, t, density)`` return
the RGBA layers (straight alpha, stepped to a few translucency levels) at real time ``t`` s;
``bank_tiles(variant, ...)`` gives a periodic set of frames the game can loop (step G5/G6 decide
whether the banks are tiles or drawn procedurally).

Outputs (review only): design/campaign/act-2-homefront/level-13-polar-relay/concept/
  whiteout-r35-a.png / .gif, whiteout-r35-b.png / .gif   a still of the halted frame at the peak (the
  relay, Skimmers and a Scuttler at the edges in the white, Stingers diving at the relay, the player,
  Rook, bullets, the edge warnings) with the layer breakdown and the ramp; a 4 s loop of the peak

Run: python3 tools/art/whiteout_l13.py   (~1 min)
"""
import sys
from functools import lru_cache

import numpy as np
from PIL import Image, ImageDraw

import artkit
from artkit import ROOT

import l13_props  # noqa: E402  (tools/art: the halted frame's stand-in plate)
from render import raster  # noqa: E402
from render.enemy_rigs import write_gif  # noqa: E402

REVIEW_ROUND = "r35"
CONCEPT = l13_props.CONCEPT
W, H = 480, 540
VARIANTS = {"a": "A - ROLLING BANKS", "b": "B - BLIZZARD CURTAIN"}
REACH = {"a": 112.0, "b": 96.0}           # px from each side edge the bank reaches at the peak
STREAMER = 176.0                           # b: how far its streamers reach
WIND = np.array([-0.42, 0.91])             # b: the wind's direction on screen (from the upper right)
LEVELS = 5                                 # translucency steps


def stepped_rgba(rgb, a, levels=LEVELS):
    s = levels - 1
    q = np.floor(np.clip(a, 0, 1) * s + 0.5) / s
    out = np.dstack([np.clip(rgb, 0, 255), q * 255])
    out[q <= 0] = 0
    return Image.fromarray(out.astype(np.uint8), "RGBA")


@lru_cache(maxsize=None)
def noise(cell, seed, w=W, h=H, octaves=4):
    """A tileable fBm field (periodic both ways: every octave's cell, cell >> o, divides w and h),
    0..1."""
    n = raster.fbm(w, h, cell, seed, octaves=octaves, period=True)
    return (n - n.min()) / (n.max() - n.min() + 1e-9)


def sample(field, x, y):
    """``field`` at float px positions, wrapped, bilinear."""
    hh, ww = field.shape
    x0, y0 = np.floor(x).astype(int), np.floor(y).astype(int)
    fx, fy = x - x0, y - y0
    x0, y0 = x0 % ww, y0 % hh
    x1, y1 = (x0 + 1) % ww, (y0 + 1) % hh
    top = field[y0, x0] * (1 - fx) + field[y0, x1] * fx
    bot = field[y1, x0] * (1 - fx) + field[y1, x1] * fx
    return top * (1 - fy) + bot * fy


def streaks(seed, x, y):
    """Noise drawn out along the wind (b) at screen px (x, y): a tileable 512 px field sampled on a
    grid stretched 6x along the wind."""
    n = noise(16, seed, 512, 512, 3)
    along = x * WIND[0] + y * WIND[1]
    across = x * WIND[1] - y * WIND[0]
    return sample(n, across * 1.3, along / 6.0)


def bank_layer(variant, w=W, h=H, t=0.0, cover=1.0):
    """The banks at real time ``t`` (s) and ramp ``cover`` (0 none .. 1 the peak), w x h, RGBA."""
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64)
    edge = np.minimum(xx, w - 1 - xx)                     # distance to the nearer side edge
    side = np.where(xx < w / 2, 1.0, -1.0)                # +1 left bank, -1 right bank (inward)
    if cover <= 0:
        return Image.new("RGBA", (w, h))
    if variant == "a":
        roll_x, roll_y = 7.0 * t * side, 10.0 * t
        big = sample(noise(60, 3801, octaves=3), xx - roll_x, yy - roll_y)
        small = sample(noise(30, 3802, octaves=2), xx - roll_x * 1.4, yy - roll_y * 1.3)
        rim = sample(noise(60, 3803, octaves=3), side * 37.0 + 3.0 * t, yy - roll_y)
        dens = 0.75 * big + 0.25 * small
        reach = REACH["a"] * (0.35 + 0.65 * cover) * (0.8 + 0.4 * rim)
        prof = (reach - edge) / (0.5 * reach) + (dens - 0.5) * 1.6       # the billows push the edge out
        a = np.clip(prof, 0, 1) ** 0.8 * (0.62 + 0.38 * np.clip(prof - 0.6, 0, 1)) * (0.55 + 0.45 * cover)
        a = np.clip(a, 0, 0.95)
        gy, gx = np.gradient(big * 40)
        lit = np.clip(0.62 - (gx + gy) * 0.8, 0, 1)                      # billows lit from the top-left
        lo, hi_ = np.array([150, 166, 184]), np.array([242, 246, 250])
        rgb = lo + (hi_ - lo) * np.clip(0.2 + 0.8 * lit, 0, 1)[..., None]
        return stepped_rgba(rgb, a, 6)
    # b: a near-opaque sheet at the edge, torn along the wind into streamers
    drift = 70.0 * t
    sx, sy = xx - WIND[0] * drift, yy - WIND[1] * drift
    st = streaks(3811, sx, sy)
    gust = sample(noise(60, 3812, octaves=3), sx * 0.5, sy * 0.5 + 15 * t)
    reach = REACH["b"] * (0.35 + 0.65 * cover) * (0.8 + 0.4 * gust)
    prof = (reach - edge) / (0.4 * reach) + (st - 0.5) * 2.2
    sheet = np.clip(prof, 0, 1) ** 0.7
    far = STREAMER * (0.4 + 0.6 * cover)
    streamer = np.clip((st - 0.52) / 0.16, 0, 1) * np.clip((far - edge) / (0.75 * far), 0, 1)
    a = np.clip(np.maximum(sheet * 0.93, streamer * 0.7) * (0.5 + 0.5 * cover), 0, 0.96)
    lo, hi_ = np.array([170, 186, 202]), np.array([246, 249, 252])
    rgb = lo + (hi_ - lo) * np.clip(0.45 + 1.1 * (st - 0.5) + 0.3 * sheet, 0, 1)[..., None]
    return stepped_rgba(rgb, a, 6)


@lru_cache(maxsize=None)
def flakes(variant, n, seed):
    rng = np.random.default_rng(seed)
    return np.column_stack([rng.uniform(0, W, n), rng.uniform(0, H, n), rng.uniform(0.6, 1.0, n),
                            rng.uniform(0, 6.28, n), rng.uniform(0, 1, n)])


def snow_layer(variant, w=W, h=H, t=0.0, density=1.0):
    """The snow on high-air at real time ``t`` (s), ``density`` 0..1 (the ramp); at most 40 % of the
    frame. a: flakes drifting down with a light wind; b: slanted streaks with the wind, denser at the
    edges."""
    ss = 2
    img = Image.new("RGBA", (w * ss, h * ss))
    d = ImageDraw.Draw(img)
    if variant == "a":
        pts = flakes("a", 260, 3821)
        for x0, y0, sp, ph, keep in pts:
            if keep > 0.25 + 0.75 * density:
                continue
            y = (y0 + (52 + 40 * sp) * t) % (h + 8) - 4
            x = (x0 - 16 * t * sp + 5 * np.sin(1.3 * t + ph)) % w
            r = (0.6 + 0.9 * sp) * ss
            a = int(140 + 90 * sp)
            d.ellipse([x * ss - r, y * ss - r, x * ss + r, y * ss + r], fill=(236, 244, 250, a))
    else:
        pts = flakes("b", 320, 3831)
        for x0, y0, sp, ph, keep in pts:
            edge = min(x0, W - x0) / (W / 2)
            if keep > (0.2 + 0.8 * density) * (1.1 - 0.6 * edge):
                continue
            v = (120 + 90 * sp)
            x = (x0 + WIND[0] * v * t) % (w + 20) - 10
            y = (y0 + WIND[1] * v * t) % (h + 20) - 10
            ln = (5 + 7 * sp) * ss
            a = int(120 + 100 * sp)
            d.line([x * ss, y * ss, x * ss - WIND[0] * ln, y * ss - WIND[1] * ln], fill=(240, 246, 252, a),
                   width=max(1, int(ss * (0.6 + 0.6 * sp))))
    small = np.array(img.resize((w, h), Image.BOX)).astype(np.float64)
    return stepped_rgba(small[..., :3], small[..., 3] / 255, 4)


def bank_tiles(variant, frames=40, period=4.0, w=W, h=H):
    """The peak banks as ``frames`` frames over ``period`` s, for a looped bank animation. The fields
    are tileable, so the loop is seamless when the drift over ``period`` is a whole field period (the
    backdrop step picks the period and whether the game draws tiles or the layer itself)."""
    return [bank_layer(variant, w, h, period * i / frames, 1.0) for i in range(frames)]


# =========================================================================== review composition

def edge_warning(base, x, y, side):
    """The 3 s edge warning stand-in: an amber chevron pair pointing in from the edge."""
    d = ImageDraw.Draw(base)
    s = 1 if side == "left" else -1
    for k in range(2):
        cx = x + s * (6 + 9 * k)
        d.polygon([(cx - s * 5, y - 8), (cx + s * 4, y), (cx - s * 5, y + 8), (cx - s * 1, y)],
                  fill=(255, 176, 40, 255))
    return base


class Units:
    def __init__(self):
        self.skimmer = None
        try:
            import skimmer as sk  # the review sets are rendered in-process (no assets yet)
            self.sk = sk
        except Exception:                                     # pragma: no cover
            self.sk = None
        self.scuttler = artkit.load_frames("scuttler")
        self.stinger = artkit.load_frames("stinger")
        self.orb = artkit.load_frames("orb")
        self.ship = artkit.load_frames("ship")
        self.rook = artkit.load_frames("rook")

    def skimmer_frames(self):
        if self.skimmer is None:
            self.skimmer = {k: self.sk.above_frame((k, 0)) for k in (0, 8, 7, 9)}
        return self.skimmer


def frame(variant, plate, units, relay_img, t, cover=1.0, f=0, parts=False):
    """The halted frame at real time t: ground (plate, relay, ground units), low-air (banks), air
    (Stingers, bullets, Rook, the player), high-air (snow), the warnings on top."""
    g = plate.ground(f)
    l13_props.put(g, relay_img, 240, 250)
    sk = units.skimmer_frames()
    put = l13_props.put
    # Skimmers in the leads at the edges, in the white; one in the clear lead below the floe
    for x, y0, k in ((64, 150, 0), (418, 420, 8), (60, 360, 0)):
        y = (y0 + (180 if k == 0 else -180) * (t % 2.0)) % 600 - 30
        put(g, sk[k], x + 30 * np.sin(5.2 * t + x), y)
    put(g, sk[7], 240 + 160 * ((t * 0.25) % 1) - 80, 486)
    scut = units.scuttler
    put(g, scut[4 * 6 + int(t * 8) % 6], 30 + 10 * t % 40, 280)            # walking in from the left edge
    put(g, scut[12 * 6 + int(t * 8) % 6], 452 - 10 * t % 40, 200)          # and from the right
    low = bank_layer(variant, W, H, t, cover)
    air = Image.new("RGBA", (W, H))
    st = units.stinger
    for k, (x0, y0) in enumerate(((150, 60), (330, 40))):
        u = (t * 0.45 + 0.3 * k) % 1
        x, y = x0 + (240 - x0) * u, y0 + (250 - y0) * u * 1.1
        put(air, st[(1 + 4 * k) * 4 + int(t * 12) % 4], x, y)
    for k in range(9):
        a = 0.7 * k
        r = (60 * t + 40 * k) % 260
        put(air, units.orb[int(t * 10 + k) % 4], 240 + np.cos(a) * r * 0.9, 250 + np.sin(a) * r * 0.8 - 30)
    put(air, units.rook[2], 196, 392)
    put(air, units.ship[2], 264 + 16 * np.sin(t), 420)
    high = snow_layer(variant, W, H, t, cover)
    out = g.copy()
    out.alpha_composite(low)
    out.alpha_composite(air)
    out.alpha_composite(high)
    edge_warning(out, 2, 150, "left")
    edge_warning(out, W - 3, 420, "right")
    if parts:
        return out, g, low, air, high
    return out


def review(variant, plate, units, relay_img):
    artkit.REVIEW_ROUND = REVIEW_ROUND
    still, g, low, air, high = frame(variant, plate, units, relay_img, 1.2, 1.0, 0, parts=True)
    ramp = [frame(variant, plate, units, relay_img, 1.2, c, 0).resize((160, 180), Image.BOX) for c in (0.0, 0.33, 0.66, 1.0)]
    over_dark = []
    for lay in (low, high):
        cell = artkit.checker(W, H)
        cell.alpha_composite(lay)
        over_dark.append(cell.resize((240, 270), Image.BOX))
    sheet = raster.sheet(1300, 40 + 560 + 30, f"WHITEOUT {VARIANTS[variant]} - LEVEL 13 PEAK (ROUND 35 PROPOSAL)",
                         "PRODUCTION PROPOSAL, M5 PART G BATCH - R35")
    raster.draw_text(sheet, 16, 38, "THE HALTED FRAME AT THE PEAK, 1X", raster.LABEL)
    sheet.alpha_composite(still, (16, 52))
    raster.draw_text(sheet, 520, 38, "LOW-AIR BANKS ALONE  /  HIGH-AIR SNOW ALONE (0.5X)", raster.LABEL)
    for i, c in enumerate(over_dark):
        sheet.alpha_composite(c, (520 + i * 250, 52))
    raster.draw_text(sheet, 520, 336, "THE RAMP: COVER 0 / 0.33 / 0.66 / 1 (4 S RAMPS INTO THE 176-188 PEAK)", raster.LABEL)
    for i, c in enumerate(ramp):
        sheet.alpha_composite(c, (520 + i * 168, 350))
    gif = [frame(variant, plate, units, relay_img, i / 20, 1.0, i % 80) for i in range(80)]
    CONCEPT.mkdir(parents=True, exist_ok=True)
    png, gpath = CONCEPT / f"whiteout-{REVIEW_ROUND}-{variant}.png", CONCEPT / f"whiteout-{REVIEW_ROUND}-{variant}.gif"
    sheet.convert("RGB").save(png, optimize=True)
    write_gif(gif, gpath, fps=20, colors=128)
    print(f"review: {png.relative_to(ROOT)}, {gpath.relative_to(ROOT)}")


def main(args):
    import relay  # tools/art: the relay's variant a (the game's until the pick)
    plate = l13_props.HaltPlate()
    units = Units()
    relay_img = relay.preview_frame("a")
    for v in VARIANTS:
        if args and v not in args:
            continue
        review(v, plate, units, relay_img)


if __name__ == "__main__":
    main(sys.argv[1:])
