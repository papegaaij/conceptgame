#!/usr/bin/env python3
"""Concept round 08 - the remaining Earth scenes for Acts 1-2 (and Luna's far side).

Outputs (design/art-direction/concept/):
  scene-ocean-r08-a.png / .gif         L11 Atlantic Convoy: overcast slate-blue ocean with a UTC
                                       convoy (two freighters, one burning, and a CDF frigate)
                                       holding station while the sea streams past; Kelvin wakes
                                       and foam, Driftjellies at and under the surface, Reef
                                       Spitter rafts, a vast shape gliding under the waves
  scene-storm-r08-a.png / .gif         L12 Storm Front: dark heaving sea with whitecaps and
                                       wind-streaked foam, a fusion platform with waves breaking
                                       round its legs, a CDF patrol boat ploughing through the
                                       swell, scud racing sideways, slanting rain, lightning;
                                       a Lamprey stream and wind-borne Whirl Seeds
  scene-arctic-r08-a.png / .gif        L13 Polar Relay: ice floes on black water with submerged
                                       ice shelves showing teal under the surface and broken
                                       foam at every waterline, the relay station on an ice
                                       shelf, Skimmers weaving between the floes with wakes, a
                                       Scuttler striding over the ice, fog banks and snow
  scene-geneva-r08-a.png / .gif        L14 Geneva Concord under the Vrell canopy: the old city
                                       by the lake (Jet d'Eau), the Concord rotunda, roofs and
                                       parks overgrown by creep and the Siege Spire's roots,
                                       mottled canopy shadow with light pools, a veined
                                       membrane canopy on high-air, spore haze
  scene-luna-farside-r08-a.png / .gif  L06 Farside: the dark side of the Moon lit only by
                                       mining-dome lights, Vrell glow, falling flares and the
                                       ship's headlight; Mantis at the sides, a Coilwyrm

Rules: design/art-direction/README.md - layer model, decoration and atmosphere intensity
(medium; the storm is a heavy peak), and the Water rules (irregular foam collars, ripple trains
and wakes, submerged parts visible through the water, no drawn circles). The sea is an animated
height field (the kraken_r07 approach) that is periodic in the scroll direction and advances an
integer number of cycles per loop, so every loop is seamless; ripples, wakes and swell are added
to it and lit like waves. Each PNG shows the play field at 1x plus the layer breakdown; each GIF
is a seamless 4 s loop (80 frames at 20 fps) with the bullet colours reserved in its palette.
Builds on scenes_r06 (scene base, cast, helpers, GIF palette), scenes_r07 (fish, bubbles),
enemies_r06 / enemies_r07 (chosen naval units, Lamprey, Creeper, Hive Node, Driftjelly
waterline) and render/r08_models.py (freighter, frigate, fusion platform, rotunda).
Run: python3 tools/concept/scenes_r08.py [ocean] [storm] [arctic] [geneva] [luna-farside] [--sheet]
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, str(Path(__file__).resolve().parent))
import enemies_r03 as e3  # noqa: E402
import enemies_r04 as e4  # noqa: E402
import enemies_r06 as e6  # noqa: E402  (chosen naval units, Lamprey, Creeper, Hive Node)
import enemies_r07 as e7  # noqa: E402  (Driftjelly cut at the waterline)
import parallax_r02 as r02  # noqa: E402
import parallax_r03 as r03  # noqa: E402
import scenes_r06 as s6  # noqa: E402
from parallax_r02 import H, W, posterize, scroll_tex  # noqa: E402
from render import r08_models as m8  # noqa: E402
from render import raster, scene_models as sm, sdf, sprite, station  # noqa: E402
from render.config import ROOT  # noqa: E402
from render.palette import B  # noqa: E402

OUT = s6.OUT
N, FPS, TAU = s6.N, s6.FPS, s6.TAU
SHOTS = s6.SHOTS
FOAM = (226, 238, 242)
YY, XX = np.mgrid[0:H, 0:W].astype(np.float32)


def th(f, k=1.0):
    """Loop phase: ``k`` must be an integer for anything that has to loop seamlessly."""
    return TAU * k * f / N


def lerp(a, b, t):
    return a + (b - a) * t


# =========================================================================== water

class Sea:
    """Animated ocean surface as a height field in *world* space: periodic in y with the ground
    period (every component an integer number of world cycles) and advancing an integer number
    of cycles per loop, viewed through the scroll. Lit from the top-left like waves and
    posterized to a few tones (small GIFs, 90s look)."""

    def __init__(self, per, waves, seed, stops, tones=8, warp_gain=2.2, light_gain=5.0,
                 warp_cell=None):
        self.per, self.stops, self.tones = per, stops, tones
        self.light_gain, self.warp_gain = light_gain, warp_gain
        rng = np.random.default_rng(seed)
        self.comps = []
        for lam, amp, ang, cyc in waves:          # wavelength px, amplitude, travel dir, cycles/loop
            a = np.radians(ang)
            m = int(round(per * np.sin(a) / lam))
            self.comps.append((np.float32(TAU * np.cos(a) / lam), np.float32(TAU * m / per),
                               np.float32(amp), TAU * cyc, rng.uniform(0, TAU)))
        cell = warp_cell or per // 5
        self.warp = [r02.periodic_fbm(W, per, cell, seed + 7 + i, octaves=2).astype(np.float32)
                     for i in range(2)]

    def rows(self, off):
        return (np.arange(H) - int(round(off))) % self.per

    def height(self, f, off, amp=1.0):
        rows = self.rows(off)
        wy = rows.astype(np.float32)[:, None]
        h = np.zeros((H, W), np.float32)
        for i, (kx, ky, a, w, ph) in enumerate(self.comps):
            h += a * np.sin(kx * XX + ky * wy + self.warp_gain * (self.warp[i % 2][rows] - 0.5)
                            - w * f / N + ph)
        return h * amp

    def light(self, h):
        gy, gx = np.gradient(h)
        nx, ny = -gx * 3.0, -gy * 3.0
        inv = 1.0 / np.sqrt(nx * nx + ny * ny + 1)
        diff = (nx * -0.48 + ny * -0.56 + inv * 0.68) - 0.68
        return diff * self.light_gain + 0.025 * h

    def colour(self, light):
        t = np.clip(0.5 + light, 0, 0.999)
        q = np.floor(t * self.tones) / (self.tones - 1)
        return raster.ramp(self.stops, np.clip(q, 0, 1))

    def mean(self):
        return raster.ramp(self.stops, np.array(0.5))


def through_surface(body, sub, mean, k=1.0):
    """Blend a layer of submerged things (RGBA) under the surface lighting: the body colour of
    the water changes, the wave tones stay on top (the surface is still seen over it)."""
    a = np.asarray(sub, np.float32)
    al = a[..., 3:4] / 255 * k
    return body + (a[..., :3] - mean) * al


def wobble_layer(img, f, amp=1.2, freq=0.09, k=3):
    """Row-wise refraction sway of a submerged layer (loop-safe: integer cycles)."""
    a = np.asarray(img)
    shift = np.round(amp * np.sin(freq * np.arange(H) + th(f, k))).astype(int)
    out = np.zeros_like(a)
    for s in np.unique(shift):
        rows = shift == s
        out[rows] = np.roll(a[rows], s, axis=1)
    return Image.fromarray(out, "RGBA")


def deep_tint(sp, water, depth, blur=1.0, alpha=1.0):
    """A sprite seen ``depth`` (0..1) under the surface: towards the water colour, darker,
    softer and fainter."""
    a = np.asarray(sp).astype(np.float32)
    a[..., :3] = a[..., :3] * (1 - depth) * (1 - 0.35 * depth) + np.array(water) * depth
    a[..., 3] *= alpha * (1 - 0.45 * depth)
    img = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")
    return img.filter(ImageFilter.GaussianBlur(blur)) if blur > 0 else img


class Foam:
    """Foam, spray and smoke particles drawn 2x supersampled on one layer per frame."""

    SS = 2

    def __init__(self, color=FOAM):
        self.img = Image.new("RGBA", (W * self.SS, H * self.SS), (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.img)
        self.color = color

    def dot(self, x, y, r, alpha, color=None):
        if alpha <= 0.03 or not (-20 < x < W + 20 and -20 < y < H + 20):
            return
        s = self.SS
        c = (color or self.color) + (int(255 * min(1.0, alpha)),)
        r = max(0.5, r) * s
        self.d.ellipse([x * s - r, y * s - r * 0.85, x * s + r, y * s + r * 0.85], fill=c)

    def done(self, levels=5):
        img = self.img.resize((W, H), Image.BOX)
        return r03.step_alpha(img, levels)


def stream(foam, f, ox, oy, dx, dy, length, cycles, count, spread0, spread1, r0, r1, alpha,
           seed, color=None, wave=1.0):
    """A loop-safe particle stream from an emitter (bow wave, stern wake, smoke): particle k is
    at age u = (cycles * f / N + phase_k) mod 1, so its speed is length * cycles / N px per
    frame and the stream repeats exactly every loop."""
    rng = np.random.default_rng(seed)
    lat = rng.uniform(-1, 1, count)
    ph = rng.uniform(0, 1, count)
    rad = rng.uniform(0.6, 1.25, count)
    jit = rng.uniform(0, TAU, count)
    n = np.hypot(dx, dy)
    dx, dy = dx / n, dy / n
    for k in range(count):
        u = (cycles * f / N + ph[k]) % 1.0
        dist = u * length
        sp = spread0 + (spread1 - spread0) * u
        side = lat[k] * sp + wave * np.sin(TAU * 2 * u + jit[k])
        x = ox + dx * dist - dy * side
        y = oy + dy * dist + dx * side
        a = alpha * (1 - u) ** 1.3 * min(1.0, u * 14 + 0.15)
        foam.dot(x, y, (r0 + (r1 - r0) * u) * rad[k], a, color)


def outline_points(sp, step=3):
    """Boundary pixels of a sprite's silhouette (relative to its centre), every ``step``."""
    a = np.asarray(sp)[..., 3] > 100
    edge = a & ~(np.roll(a, 1, 0) & np.roll(a, -1, 0) & np.roll(a, 1, 1) & np.roll(a, -1, 1))
    ys, xs = np.nonzero(edge)
    order = np.argsort(np.arctan2(ys - sp.height / 2, xs - sp.width / 2))
    pts = np.stack([xs[order] - sp.width / 2, ys[order] - sp.height / 2], 1)
    return pts[::step]


def collar(foam, f, pts, cx, cy, seed, alpha=0.9, r=1.3, push=1.5):
    """Broken, wobbling foam collar along a waterline (loop-safe): each point flickers with
    its own integer-harmonic phase, gaps open and close, flecks drift outwards."""
    rng = np.random.default_rng(seed)
    ph = rng.uniform(0, TAU, len(pts))
    hk = rng.integers(1, 4, len(pts))
    for (px, py), p, k in zip(pts, ph, hk):
        v = 0.5 + 0.5 * np.sin(th(f, k) + p) * np.sin(th(f, 1) * 2 + p * 1.7)
        if v < 0.3:
            continue
        nrm = np.hypot(px, py) or 1
        out = push * (0.5 + v)
        foam.dot(cx + px + px / nrm * out, cy + py + py / nrm * out, r * (0.6 + 0.6 * v), alpha * v)


def kelvin(h, x, ybow, amp=1.0, decay=380.0):
    """A ship's Kelvin wake added to the height field: two divergent arms at ~19.5 degrees with
    feathered crests and transverse waves between them. Stationary in the ship's frame."""
    y0 = int(max(0, ybow))
    if y0 >= H:
        return
    dy = YY[y0:] - ybow
    dx = np.abs(XX[y0:] - x)
    arm = 0.36 * dy
    wdt = 3.0 + 0.07 * dy
    env = np.exp(-dy / decay) * np.clip(dy / 12, 0, 1)
    div = np.exp(-((dx - arm) / wdt) ** 2) * np.sin(0.45 * (0.78 * dy + 0.62 * dx))
    inside = 1 / (1 + np.exp((dx - arm) / 3.0))
    trans = 0.55 * inside * np.sin(0.21 * dy) * np.clip((dy - 30) / 40, 0, 1)
    h[y0:] += amp * env * (1.4 * div + trans)


def ripple_train(h, x, y, age, life, amp=0.8, lam=9.0, speed=26.0, seed=0.0):
    """An expanding, uneven ripple train added to the height field (kraken_r07 style)."""
    if age <= 0 or age >= life:
        return
    front = speed * age
    R = front + 2.5 * lam
    x0, x1 = int(max(0, x - R)), int(min(W, x + R + 1))
    y0, y1 = int(max(0, y - R)), int(min(H, y + R + 1))
    if x0 >= x1 or y0 >= y1:
        return
    ddx, ddy = XX[y0:y1, x0:x1] - x, YY[y0:y1, x0:x1] - y
    r = np.sqrt(ddx * ddx + ddy * ddy)
    a = np.arctan2(ddy, ddx)
    rr = r * (1 + 0.08 * np.sin(2 * a + seed) + 0.05 * np.sin(3 * a + 2 * seed))
    env = np.exp(-((rr - 0.72 * front) / (0.4 * front + lam)) ** 2)
    brk = np.clip(0.3 + 0.7 * (0.5 + 0.5 * np.sin(2 * a + 1.7 * seed) * np.sin(3 * a - seed)), 0, 1)
    h[y0:y1, x0:x1] += amp * (1 - age / life) ** 1.5 * env * brk * np.sin(TAU * (rr - front) / lam)


def fan_shots(img, f, src, cast, period=40, speed=6.0, n=3, spread=0.3, phase=0, life=40,
              kind="orb"):
    """Aimed n-way fans from ``src(ff) -> (x, y) or None`` every ``period`` frames."""
    for k in range(N // period):
        age = (f + phase - k * period) % N
        if age > life:
            continue
        s = src(f - age)
        if s is None:
            continue
        sx, sy = s
        tx, ty, _ = cast.player_pos(f - age)
        base = np.arctan2(ty - sy, tx - sx)
        for j in range(n):
            a = base + (j - (n - 1) / 2) * spread
            x, y = sx + np.cos(a) * age * speed, sy + np.sin(a) * age * speed
            if kind == "orb":
                e3.orb(img, x, y, 5)
            else:
                e3.orb(img, x, y, 5, ring=SHOTS[5])


# =========================================================================== shared models

_m = {}


def model(name):
    if name not in _m:
        if name == "freighter":
            _m[name] = sm.render_px_model(m8.freighter(), factor=3, colors=48)
        elif name == "freighter-b":
            _m[name] = sm.render_px_model(m8.freighter(seed=11), factor=3, colors=48)
        elif name == "frigate":
            _m[name] = sm.render_px_model(m8.frigate(), factor=3, colors=40)
        elif name == "patrol":
            _m[name] = sm.render_px_model(m8.frigate(length=96, beam=18), factor=3, colors=36)
        elif name == "platform":
            _m[name] = sm.render_px_model(m8.fusion_platform(), factor=3, colors=48)
        elif name == "rotunda":
            _m[name] = sm.render_px_model(m8.rotunda(), factor=3, colors=40)
    return _m[name]


class Vessel:
    """A ship holding station on screen while the sea streams past (the convoy steams at the
    scroll speed): a slow loop-safe drift, a Kelvin wake, bow waves, a stern wake and a foam
    collar along the hull."""

    def __init__(self, sp, x, y, drift=(3.0, 5.0), phase=0.0, seed=0, wake=1.0):
        self.sp, self.x0, self.y0, self.drift, self.phase = sp, x, y, drift, phase
        self.seed, self.wake = seed, wake
        self.pts = outline_points(sp, 3)
        self.half = sp.height / 2

    def pos(self, f):
        return (self.x0 + self.drift[0] * np.sin(th(f) + self.phase),
                self.y0 + self.drift[1] * np.sin(th(f) + self.phase * 1.7))

    def add_wake(self, h, f, amp=1.0):
        x, y = self.pos(f)
        kelvin(h, x, y - self.half + 10, amp * self.wake)

    def draw_foam(self, foam, f, travel):
        x, y = self.pos(f)
        hw = self.sp.width / 2 - 6
        bow, stern = y - self.half + 8, y + self.half - 4
        for side in (-1, 1):                              # bow waves peeling off along the arms
            stream(foam, f, x + side * 4, bow + 14, side * 0.36, 0.93, travel / 3, 3, 46,
                   1.0, 7.0, 1.0, 2.0, 0.85 * self.wake, self.seed + side)
            stream(foam, f, x + side * hw, y, side * 0.12, 1.0, travel / 4, 4, 18,
                   0.5, 3.0, 0.8, 1.4, 0.5 * self.wake, self.seed + 5 + side)
        stream(foam, f, x, stern, 0.0, 1.0, travel / 2, 2, 90, 3.0, 24.0, 1.4, 3.6,
               0.95 * self.wake, self.seed + 9, wave=2.5)    # churned stern wake
        collar(foam, f, self.pts, x, y, self.seed + 3, alpha=0.75)

    def draw(self, img, f):
        x, y = self.pos(f)
        sprite.paste_center(img, sprite.shadow_of(self.sp, opacity=0.4, blur=2.0), x + 6, y + 8)
        sprite.paste_center(img, self.sp, x, y)


# =========================================================================== base

class Scene8(s6.Scene):
    intensity = "MEDIUM ATMOSPHERE"
    ORDER = ("deep", "far", "ground", "shadows", "low-air", "air", "high-air")

    def key_list(self):
        return []

    def palette_extras(self):
        return [s for s in self.key_list() if s is not None] + super().palette_extras()


def jelly_surface(img_ground, foam, sub, f, x, y, pulse, seed, water):
    """A Driftjelly floating at the surface: the dome above the water, the lower bell and
    tentacles seen through the surface, a foam collar, and ripple trains on each contraction
    are added to the height field by the caller."""
    p = round(pulse, 1)
    above = e7.jly_above.get(0.0, pulse=p)
    below = e7.jly_below.get(0.0, pulse=p)
    sprite.paste_center(sub, deep_tint(below, water, 0.25, blur=0.5), x, y + 2)
    sprite.paste_center(img_ground, above, x, y)
    rw = e7.waterline_radius(above)
    ang = np.linspace(0, TAU, 28, endpoint=False)
    pts = np.stack([np.cos(ang) * rw, np.sin(ang) * rw * 0.9], 1)
    collar(foam, f, pts, x, y, seed, alpha=0.85, r=1.1, push=1.0)


# =========================================================================== A: open ocean (L11)

OCEAN_STOPS = [(0.0, (12, 26, 38)), (0.3, (22, 42, 58)), (0.55, (36, 62, 80)),
               (0.8, (66, 94, 110)), (1.0, (132, 154, 164))]
OCEAN_WATER = (24, 48, 64)


class OceanScene(Scene8):
    slug = key = "ocean"
    title = "OPEN OCEAN - THE ATLANTIC CONVOY"
    layers = ["ground", "sub", "low-air", "air", "high-air"]
    notes = {"ground": "X1.0 SEA, CONVOY, WAKES, JELLIES", "sub": "SUB: SEEN THROUGH THE SURFACE",
             "low-air": "X1.3 SEA MIST, SMOKE", "air": "PLAY PLANE", "high-air": "X2.0 CLOUD WISPS"}
    TRAVEL = 480                 # 120 px/s: an escort, the convoy holds station
    FACTORS = {"ground": 1.0, "low-air": 1.3, "high-air": 2.0}
    GIF_COLORS = 104
    JELLIES = [(232, 40, True, 0.0), (430, 170, False, 1.3), (44, 300, True, 2.1),
               (222, 420, False, 0.7), (440, 400, True, 2.9)]
    RAFTS = [(220, 250), (446, 20)]

    def __init__(self):
        super().__init__()
        self.sea = Sea(480, [(160, 1.0, 72, 2), (96, 0.75, 52, 3), (60, 0.5, 104, 3),
                             (40, 0.34, 34, 5), (26, 0.22, 84, 6)], 1101, OCEAN_STOPS, tones=8,
                       light_gain=1.6)
        self.ships = [Vessel(model("freighter"), 112, 300, (3, 6), 0.0, 1110),
                      Vessel(model("freighter-b"), 318, 40, (2, 4), 2.0, 1120),
                      Vessel(model("frigate"), 372, 330, (5, 8), 4.0, 1130)]
        lp, hp = self.s.period("low-air"), self.s.period("high-air")
        greys = [(110, 120, 128), (150, 160, 166), (186, 194, 198), (214, 220, 222)]
        self.mist = bank_tex(lp, [208, 104, 52], 1141, 0.74, 0.1, greys, max_alpha=150)
        self.wisps = s6.wisp_tex(hp, [192, 96, 48], 1142, (222, 228, 232), thresh=0.6, gain=2.4,
                                 max_a=80, stretch=2)

    def burning(self):
        return self.ships[1]

    def jelly_world(self, j, f):
        x, y0, surf, ph = j
        x = x + 8 * np.sin(th(f) + ph)
        return x, y0, surf, ph

    def layer_images(self, f):
        off = self.s.off("ground", f)
        h = self.sea.height(f, off)
        for sh in self.ships:
            sh.add_wake(h, f)
        sub = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        top = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        foam = Foam()
        # the vast shape under the waves (a first hint of the Harbour Kraken)
        kx = 250 + 30 * np.sin(th(f))
        for ky in self.ys("ground", 380, f, margin=200):
            shadow_shape(sub, kx, ky + 30 * np.sin(th(f) + 1), f)
        for j in self.JELLIES:
            x, y0, surf, ph = self.jelly_world(j, f)
            pulse = 0.5 + 0.5 * np.sin(th(f, 2) + ph)
            for y in self.ys("ground", y0, f, margin=40):
                if surf:
                    jelly_surface(top, foam, sub, f, x, y, pulse, int(ph * 10), OCEAN_WATER)
                    for k in range(2):                  # a ripple train per contraction
                        age = ((2 * f / N + ph / TAU + k * 0.5) % 1.0) * 2.0
                        ripple_train(h, x, y, age, 1.6, amp=0.9, seed=ph + k)
                else:
                    sp = e6.jly.get(0.0, pulse=round(pulse, 1))
                    sprite.paste_center(sub, deep_tint(sp, OCEAN_WATER, 0.5, blur=1.0), x, y)
        for i, (rx, ry) in enumerate(self.RAFTS):
            for y in self.ys("ground", ry, f, margin=60):
                bob = 1.2 * np.sin(th(f, 2) + i)
                raft = e6.raft.get(0.0)
                sprite.paste_center(sub, deep_tint(raft, OCEAN_WATER, 0.3, blur=0.6, alpha=0.8),
                                    rx + 2, y + 3)
                sprite.paste_center(top, raft, rx, y + bob)
                px, py, _ = self.cast.player_pos(f)
                sprite.paste_center(top, e6.gun.get(np.arctan2(py - y, px - rx)), rx, y + bob - 2)
                ang = np.linspace(0, TAU, 40, endpoint=False)
                pts = np.stack([np.cos(ang) * 40, np.sin(ang) * 36], 1)
                collar(foam, f, pts, rx, y + bob, 40 + i, alpha=0.6, r=1.2)
        for sh in self.ships:
            sh.draw_foam(foam, f, self.TRAVEL)
        light = self.sea.light(h)
        body = self.sea.colour(light)
        body = through_surface(body, wobble_layer(sub, f), self.sea.mean(), 0.85)
        ground = raster.to_rgba_image(np.clip(body, 0, 255))
        ground.alpha_composite(foam.done())
        ground.alpha_composite(top)
        for sh in self.ships:
            sh.draw(ground, f)
        ground = self.fire(ground, f)
        air = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        casters = []
        s6.skitter_snake(air, casters, self.cast, f, 250, 110, 0.35)

        def raft_src(i):
            def src(ff):
                rx, ry = self.RAFTS[i]
                y = (ry + self.s.off("ground", ff)) % self.TRAVEL
                return (rx, y) if 0 <= y <= H else None
            return src
        fan_shots(air, f, raft_src(0), self.cast, period=40, speed=5.5, phase=8)
        fan_shots(air, f, raft_src(1), self.cast, period=40, speed=5.5, phase=28)
        self.cast.player(air, casters, f)
        shadows = r02.shadow_layer(casters, *s6.SHADOW["air"], opacity=0.42)
        low = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        smoke(low, f, *self.burning().pos(f), self.TRAVEL)
        L = {"ground": ground, "shadows": shadows, "low-air": low, "air": air}
        L = r03.decorate(L, scroll_tex(self.mist, self.s.off("low-air", f)), "ground", casters,
                         bank_shadow_opacity=0.25, flyer_opacity=0.3)
        L["high-air"] = scroll_tex(self.wisps, self.s.off("high-air", f))
        L["sub"] = sub
        return L

    def fire(self, img, f):
        x, y = self.burning().pos(f)
        flick = 0.75 + 0.25 * np.sin(th(f, 7)) * np.sin(th(f, 3) + 1)
        for dx, dy, r in ((-6, 10, 16), (5, -14, 11)):
            img = raster.add_light(img, x + dx, y + dy, r, (255, 120, 30), 0.55 * flick)
        d = ImageDraw.Draw(img)
        for k in range(7):
            a = th(f, 5) + k * 1.9
            fx, fy = x - 6 + 6 * np.sin(k * 2.1), y + 10 + 9 * np.cos(k * 1.3)
            rr = 2.0 + 1.3 * (0.5 + 0.5 * np.sin(a))
            d.ellipse([fx - rr, fy - rr, fx + rr, fy + rr], fill=(255, 200, 90, 255))
        return img

    def key_list(self):
        return [s.sp for s in self.ships] + [e6.raft.get(0.0), e7.jly_above.get(0.0, pulse=0.5)]


def bank_tex(per, cells, seed, cover, soft, tones, max_alpha=210, light=5.0, stretch=1):
    return s6.bank_tex(per, cells, seed, cover, soft, tones, max_alpha=max_alpha, light=light,
                       stretch=stretch)


def shadow_shape(img, x, y, f):
    """A vast dark creature gliding deep under the waves: mantle and trailing arms, very soft."""
    lay = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(lay)
    c = (6, 16, 26, 150)
    d.ellipse([x - 38, y - 70, x + 38, y + 40], fill=c)
    for k in range(6):
        a0 = np.pi / 2 + (k - 2.5) * 0.28
        pts = []
        for u in np.linspace(0, 1, 14):
            ang = a0 + 0.5 * np.sin(th(f) + k + u * 3) * u
            r = 30 + 150 * u
            pts.append((x + np.cos(ang) * r * 0.7, y + 20 + np.sin(ang) * r))
        d.line(pts, fill=c, width=10 - k % 3)
    img.alpha_composite(lay.filter(ImageFilter.GaussianBlur(7)))


def smoke(img, f, x, y, travel, seed=5):
    """A smoke column from a burning ship, leaning with the wind (low-air)."""
    fm = Foam(color=(70, 70, 72))
    stream(fm, f, x - 4, y + 6, 0.55, 0.84, 300, 2, 70, 3.0, 26.0, 3.0, 13.0, 0.8, seed,
           wave=4.0)
    stream(fm, f, x - 4, y + 6, 0.55, 0.84, 150, 4, 24, 2.0, 8.0, 2.0, 7.0, 0.85, seed + 1,
           color=(40, 38, 38))
    img.alpha_composite(fm.done(levels=6))


# =========================================================================== B: ocean storm (L12)

STORM_STOPS = [(0.0, (6, 13, 20)), (0.3, (13, 27, 37)), (0.55, (24, 44, 56)),
               (0.8, (48, 70, 80)), (1.0, (112, 130, 136))]
STORM_WATER = (14, 30, 40)


class StormScene(Scene8):
    slug = key = "storm"
    title = "OCEAN STORM - RAIN, WIND AND LIGHTNING"
    intensity = "HEAVY ATMOSPHERE PEAK"
    layers = ["ground", "low-air", "air", "high-air"]
    notes = {"ground": "X1.0 HEAVING SEA, PLATFORM, BOAT", "low-air": "X1.5 SCUD RACING SIDEWAYS",
             "air": "PLAY PLANE", "high-air": "X2.2 SLANTING RAIN"}
    TRAVEL = 640                 # 160 px/s
    FACTORS = {"ground": 1.0, "low-air": 1.5, "high-air": 2.2}
    GIF_COLORS = 104
    FLASH = {52: 0.34, 53: 0.2, 54: 0.08, 56: 0.16, 57: 0.05}
    PLATFORM = (120, 230)

    def __init__(self):
        super().__init__()
        self.sea = Sea(640, [(220, 1.4, 58, 2), (128, 1.0, 40, 3), (80, 0.7, 76, 4),
                             (48, 0.45, 24, 6), (30, 0.28, 64, 8)], 1201, STORM_STOPS, tones=8,
                       light_gain=4.0, warp_gain=2.6, warp_cell=128)
        self.caps = r02.periodic_fbm(W, 640, 64, 1202, octaves=3).astype(np.float32)
        self.streaks = r03.stretched(W, 640, [64, 32], 1203, 6).astype(np.float32)
        self.boat = Vessel(model("patrol"), 360, 380, (6, 10), 1.0, 1210, wake=1.3)
        lp = self.s.period("low-air")                 # 960: cells divide W and lp (x-periodic)
        n = r03.noise(W, lp, [160, 80, 32, 16], 1221)
        tones = [(30, 34, 40), (52, 58, 64), (78, 84, 90), (104, 110, 114)]
        self.scud = r03.step_alpha(posterize(r03.bank(n, 0.56, 0.14, tones, max_alpha=225,
                                                      light=6.0), 12), 6)
        rng = np.random.default_rng(1230)
        self.rain = [(rng.uniform(0, W), rng.uniform(0, H + 40), int(rng.integers(5, 8)))
                     for _ in range(230)]

    def layer_images(self, f):
        off = self.s.off("ground", f)
        h = self.sea.height(f, off)
        self.boat.add_wake(h, f, 1.2)
        foam = Foam()
        top = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        legs = []
        for py in self.ys("ground", self.PLATFORM[1], f, margin=120):
            px = self.PLATFORM[0]
            hs = 56
            for lx, ly in ((-hs, -hs), (hs, -hs), (-hs, hs), (hs, hs)):
                legs.append((px + lx, py + ly))
            plat = model("platform")
            sprite.paste_center(top, sprite.shadow_of(plat, opacity=0.45, blur=2.5), px + 9, py + 12)
            sprite.paste_center(top, plat, px, py)
            blink = (f // 5) % 2 == 0
            if blink:
                for lx, ly in ((-hs, -hs), (hs, -hs), (-hs, hs), (hs, hs)):
                    top = raster.add_light(top, px + lx, py + ly, 7, (255, 60, 40), 0.9)
        for k, (lx, ly) in enumerate(legs):                # waves breaking round the legs
            for j in range(3):
                age = ((f / N * 4 + j / 3 + k * 0.27) % 1.0) * 1.2
                ripple_train(h, lx, ly, age, 1.2, amp=1.4, lam=10, speed=30, seed=k + j)
            ang = np.linspace(0, TAU, 22, endpoint=False)
            pts = np.stack([np.cos(ang) * 9, np.sin(ang) * 9], 1)
            collar(foam, f, pts, lx, ly, 70 + k, alpha=0.95, r=1.8, push=3.0)
            stream(foam, f, lx, ly + 6, 0.25, 1.0, self.TRAVEL / 4, 4, 20, 2, 12, 1.4, 3.0,
                   0.8, 90 + k)
        self.boat.draw_foam(foam, f, self.TRAVEL)
        bx, by = self.boat.pos(f)
        for side in (-1, 1):                             # spray thrown off the bow
            stream(foam, f, bx + side * 4, by - 40, side * 1.0, 0.35, 40, 8, 22, 2, 8, 1.0, 2.2, 0.9,
                   1240 + side, wave=3)
        light = self.sea.light(h)
        body = self.sea.colour(light)
        rows = self.sea.rows(off)
        caps = (h > 1.6) & (self.caps[rows] > 0.52)
        streak = (self.streaks[rows] > 0.66) & (h > 0.4)
        body[streak] = body[streak] * 0.55 + np.array((150, 168, 176)) * 0.45
        body[caps] = np.array((196, 210, 214))
        ground = raster.to_rgba_image(np.clip(body, 0, 255))
        ground.alpha_composite(foam.done())
        ground.alpha_composite(top)
        self.boat.draw(ground, f)
        air = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        casters = []
        self.lampreys(air, casters, f)
        cx = -60 + 600 * ((f / N + 0.15) % 1.0)
        s6.seed_cluster(air, casters, f, cx, 150 + 50 * np.sin(th(f)))
        cx2 = -60 + 600 * ((f / N + 0.65) % 1.0)
        s6.seed_cluster(air, casters, f, cx2, 330 + 40 * np.sin(th(f) + 2), ph=1.3)
        self.cast.player(air, casters, f)
        shadows = r02.shadow_layer(casters, *s6.SHADOW["air"], opacity=0.36)
        low = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        if f in self.FLASH and f < 55:
            s6.lightning(low, 300, -20, 330, 1250)
        scud = scroll_tex(self.scud, self.s.off("low-air", f))
        scud = Image.fromarray(np.roll(np.asarray(scud), int(round(W * f / N)), axis=1), "RGBA")
        L = {"ground": ground, "shadows": shadows, "low-air": low, "air": air}
        L = r03.decorate(L, scud, "ground", casters, bank_shadow_opacity=0.3, flyer_opacity=0.3)
        L["high-air"] = self.rain_layer(f)
        return L

    def lampreys(self, img, casters, f):
        """A Lamprey stream: they sweep in from the upper left, curve towards the player's
        position and loop away (a loop-safe path), each swimming with its own phase."""
        for i in range(6):
            u = (f / N + 0.4 - i * 0.035) % 1.0

            def path(uu):
                a = TAU * uu
                return (240 + 230 * np.sin(a + 3.6) + 40 * np.sin(2 * a),
                        250 + 210 * np.sin(2 * a + 0.6) * 0.9 + 20 * np.cos(a))
            x, y = path(u)
            x2, y2 = path(u + 0.004)
            h_ = float(np.arctan2(y2 - y, x2 - x))
            sp = e6.lmp.get(h_, phase=((f // 2 + i) % 4) * TAU / 4)
            casters.append((sp, x, y))
            sprite.paste_center(img, sp, x, y)

    def rain_layer(self, f):
        img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        hr = H + 40
        vx, vy = W / N, 6 * hr / N                        # one screen width sideways per loop
        n = np.hypot(vx, vy)
        ux, uy = vx / n, vy / n
        for x0, y0, cyc in self.rain:
            x = (x0 + W * f / N) % W
            y = (y0 + cyc * hr * f / N) % hr - 20
            ln = 10 + cyc * 1.5
            d.line([x, y, x - ux * ln, y - uy * ln], fill=(170, 186, 196, 100), width=1)
        if f in self.FLASH:
            img.alpha_composite(Image.new("RGBA", (W, H), (205, 220, 255, int(255 * self.FLASH[f]))))
        return img

    def key_list(self):
        return [model("platform"), self.boat.sp, e6.lmp.get(0.0, phase=0.0)]


# =========================================================================== C: arctic (L13)

ARCTIC_STOPS = [(0.0, (3, 11, 20)), (0.35, (8, 22, 36)), (0.65, (16, 38, 56)),
                (0.9, (40, 72, 92)), (1.0, (96, 128, 146))]
ARCTIC_WATER = (8, 24, 40)
ICE = [(0.0, (86, 116, 140)), (0.35, (150, 178, 198)), (0.7, (206, 222, 234)),
       (1.0, (240, 246, 250))]


class ArcticScene(Scene8):
    slug = key = "arctic"
    title = "ARCTIC - THE POLAR RELAY IN THE ICE FLOES"
    layers = ["ground", "low-air", "air", "high-air"]
    notes = {"ground": "X1.0 FLOES, WATER, RELAY", "low-air": "X1.3 FOG BANKS",
             "air": "PLAY PLANE", "high-air": "X2.0 SNOW FLURRIES"}
    TRAVEL = 480                 # 120 px/s: the scroll slows towards the relay
    FACTORS = {"ground": 1.0, "low-air": 1.3, "high-air": 2.0}
    GIF_COLORS = 104
    SHELF = (338, 200, 118)

    def __init__(self):
        super().__init__()
        per = self.TRAVEL
        self.sea = Sea(per, [(120, 0.7, 70, 2), (70, 0.5, 40, 3), (40, 0.3, 100, 4),
                             (24, 0.2, 60, 6)], 1301, ARCTIC_STOPS, tones=7, light_gain=5.5,
                       warp_cell=96)
        self._ice(per)
        lp = self.s.period("low-air")
        pale = [(150, 170, 186), (186, 202, 214), (214, 226, 234), (234, 242, 246)]
        self.fog = bank_tex(lp, [208, 104, 52], 1341, 0.66, 0.12, pale, max_alpha=190)
        rng = np.random.default_rng(1350)
        self.snow = [(rng.uniform(0, W), rng.uniform(0, H + 20), int(rng.integers(2, 4)),
                      rng.uniform(0.6, 1.6)) for _ in range(150)]

    def _ice(self, per):
        n = r02.periodic_fbm(W, per, 96, 1311, octaves=4)
        cx, cy, R = self.SHELF
        d = np.hypot(XX[:per] - cx, s6.periodic_dist(YY[:per], cy, per)) / R
        n = n + np.clip(1.2 - d, 0, 1) * 0.6
        ice = n > 0.6
        ice = s6.pblur_mask(ice.astype(np.float64), 1.2) > 0.5
        self.mask = ice
        # height: plateau with soft edges, pressure ridges, snow texture
        hgt = s6.pblur_mask(ice.astype(np.float64), 3.0)
        rid = r02.periodic_fbm(W, per, 24, 1312, octaves=2)
        hgt = hgt * (0.8 + 0.25 * rid) + np.where(np.abs(rid - 0.5) < 0.03, 0.15, 0) * ice
        shade = s6.hillshade(hgt * 30, 0.7)
        col = raster.ramp(ICE, np.clip(0.25 + 0.55 * hgt * (0.7 + 0.3 * rid), 0, 1))
        col = col * shade[..., None]
        rim = ice & ~(np.roll(ice, 2, 0) & np.roll(ice, 2, 1) & np.roll(ice, -1, 0) & np.roll(ice, -1, 1))
        col[rim] = col[rim] * 0.82
        alpha = ice * 255
        self.ice_tex = s6.smooth_tex(posterize(raster.to_rgba_image(np.clip(col, 0, 255), alpha), 10))
        # submerged ice shelf: a pale teal margin under the water round every floe, and the
        # floes' long cool shadows on the water (sun low at the top-left)
        grow = s6.pblur_mask(ice.astype(np.float64), 5.0)
        margin = np.clip((grow - 0.06) * 3.0, 0, 1) * ~ice
        shade_w = np.roll(np.roll(s6.pblur_mask(ice.astype(np.float64), 2.0), 10, 0), 7, 1)
        under = np.zeros((per, W, 4))
        under[..., :3] = np.array((70, 150, 168))
        under[..., 3] = margin * 150
        self.under = r03.step_alpha(raster.to_rgba_image(under[..., :3], under[..., 3]), 5)
        sh = np.zeros((per, W, 4))
        sh[..., 3] = shade_w * 120 * ~ice
        self.cast_sh = r03.step_alpha(raster.to_rgba_image(sh[..., :3], sh[..., 3]), 4)
        edge = ice & ~(np.roll(ice, 1, 0) & np.roll(ice, -1, 0) & np.roll(ice, 1, 1) & np.roll(ice, -1, 1))
        ys, xs = np.nonzero(edge)
        rng = np.random.default_rng(1313)
        keep = rng.random(len(xs)) < 0.45
        self.edge = np.stack([xs[keep], ys[keep]], 1).astype(np.float64)
        self.edge_ph = rng.uniform(0, TAU, len(self.edge))
        self.edge_k = rng.integers(1, 4, len(self.edge))
        self.station = self._station(per)

    def _station(self, per):
        """The relay station on the ice shelf: domes, the big dish, a lattice mast, radiators,
        a landing pad, CDF turrets and warm lights (kit-bashed UTC parts)."""
        out = Image.new("RGBA", (W, per), (0, 0, 0, 0))
        p = station
        cx, cy, _ = self.SHELF
        pieces = [(p.render_part(p.dome_part(20), B), cx - 40, cy + 30),
                  (p.render_part(p.dome_part(14), B), cx + 26, cy + 58),
                  (p.render_part(p.dish_part(30), B), cx + 30, cy - 40),
                  (p.render_part(p.radiator_part(60, 2), B), cx - 50, cy - 46),
                  (p.render_part(p.cargo_part(), B), cx - 6, cy + 70),
                  (p.render_part(p.cargo_part(p.SOLAR), B), cx + 8, cy + 70),
                  (p.render_part(p.turret_part(), B), cx - 86, cy - 4),
                  (p.render_part(p.turret_part(), B), cx + 74, cy + 16)]
        self.turrets = [(cx - 86, cy - 4), (cx + 74, cy + 16)]
        d = ImageDraw.Draw(out)
        for dy in (-per, 0, per):                       # landing pad and a cleared yard
            d.ellipse([cx - 30, cy - 6 + dy, cx + 6, cy + 30 + dy], fill=(120, 132, 146, 255),
                      outline=(250, 190, 60, 255), width=2)
            d.line([cx - 18, cy + 12 + dy, cx - 6, cy + 12 + dy], fill=(240, 240, 240, 255), width=2)
        for sp, x, y in pieces:
            for dy in (-per, 0, per):
                sh = sprite.shadow_of(sp, opacity=0.5, blur=1.5)
                out.alpha_composite(sh, (int(x - sp.width / 2 + 9), int(y + dy - sp.height / 2 + 12)))
                sprite.paste_center(out, sp, x, y + dy)
        mast = Image.new("RGBA", (W, per), (0, 0, 0, 0))   # lattice mast, long shadow
        md = ImageDraw.Draw(mast)
        mx, my = cx + 70, cy - 70
        for dy in (-per, 0, per):
            md.rectangle([mx - 4, my - 4 + dy, mx + 4, my + 4 + dy], fill=(176, 180, 190, 255),
                         outline=(80, 84, 96, 255))
            for k in range(3):
                md.line([mx - 3 + 3 * k, my - 3 + dy, mx - 3 + 3 * k, my + 3 + dy], fill=(90, 94, 106, 255))
        out.alpha_composite(s6.pshadow(mast, 0.6, 1.0), (18, 26))
        out.alpha_composite(mast)
        for x, y, r in ((cx - 40, cy + 30, 30), (cx + 26, cy + 58, 22), (cx - 12, cy + 12, 26)):
            for dy in (-per, 0, per):
                out = raster.add_light(out, x, y + dy, r, (255, 196, 110), 0.25)
        return out

    def skimmer_path(self, i):
        def path(u):
            a = TAU * u
            if i == 0:
                return (-60 + 600 * u, 400 + 46 * np.sin(2 * a))
            if i == 1:
                return (W + 60 - 600 * u, 110 + 40 * np.sin(2 * a + 1.0))
            return (90 + 70 * np.sin(2 * a), -60 + 660 * u)
        return path

    def layer_images(self, f):
        off = self.s.off("ground", f)
        h = self.sea.height(f, off)
        foam = Foam()
        top = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        v = self.TRAVEL / N
        sks = []
        for i, delay in enumerate((0.0, 0.33, 0.62)):
            path = self.skimmer_path(i)
            u = (f / N + delay) % 1.0
            x, y = path(u)
            for j in range(1, 30):                     # foam wake left in the moving water
                uj = (u - j / N) % 1.0
                wx, wy = path(uj)
                wy += v * j
                if abs(wx - x) > 200:
                    break
                a = 0.9 * (1 - j / 30)
                for side in (-1, 1):
                    sx = wx + side * (2 + 0.6 * j)
                    foam.dot(sx, wy, 1.2 + 0.05 * j, a * 0.7)
                foam.dot(wx, wy, 1.6, a)
                if j % 6 == 0:
                    ripple_train(h, wx, wy, j / FPS, 1.5, amp=0.6, lam=8, speed=22, seed=i + j)
            x2, y2 = path((u + 0.003) % 1.0)
            sks.append((x, y, float(np.arctan2(y2 - y, x2 - x)), i))
        ice = scroll_tex(self.ice_tex, off)
        under = scroll_tex(self.under, off)
        csh = scroll_tex(self.cast_sh, off)
        light = self.sea.light(h)
        body = self.sea.colour(light)
        body = through_surface(body, under, self.sea.mean(), 1.0)
        water = raster.to_rgba_image(np.clip(body, 0, 255))
        water.alpha_composite(csh)
        for x, y, hd, i in sks:
            sp = e6.skm.get(hd, phase=np.pi * ((f // 2 + i) % 2))
            sprite.paste_center(top, sprite.shadow_of(sp, opacity=0.5, blur=1.0), x + 4, y + 6)
            sprite.paste_center(top, sp, x, y)
        # broken foam at every waterline (loop-safe flicker per point)
        rows = (np.arange(H) - int(round(off))) % self.TRAVEL
        inv = np.full(self.TRAVEL, -1)
        inv[rows] = np.arange(H)
        ey = inv[self.edge[:, 1].astype(int)]
        vis = ey >= 0
        edge_foam = Foam()
        for (ex, _), yy, ph, k in zip(self.edge[vis], ey[vis], self.edge_ph[vis], self.edge_k[vis]):
            val = 0.5 + 0.5 * np.sin(th(f, k) + ph) * np.sin(th(f, 2) + ph * 1.3)
            if val > 0.35:
                edge_foam.dot(ex + 0.8 * np.sin(ph), yy + 0.8 * np.cos(ph), 0.8 + 0.7 * val, 0.85 * val)
        ground = water
        ground.alpha_composite(foam.done())
        ground.alpha_composite(top)
        ground.alpha_composite(ice)
        ground.alpha_composite(edge_foam.done())
        ground.alpha_composite(scroll_tex(self.station, off))
        # a Scuttler striding over the ice shelf (one lap per loop, feet planted)
        t = th(f)
        sx, syw = self.SHELF[0] - 30 + 46 * np.cos(t), self.SHELF[1] - 60 + 26 * np.sin(t)
        hd = np.arctan2(26 * np.cos(t), -46 * np.sin(t))
        sp = s6.walk_sprite(hd, 3.75 * f)
        for yy in self.ys("ground", syw, f):
            sprite.paste_center(ground, sprite.shadow_of(sp, opacity=0.5, blur=1.0), sx + 8, yy + 11)
            sprite.paste_center(ground, sp, sx, yy)
        air = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        casters = []
        for k, (x, y, hd, i) in enumerate(sks):
            path = self.skimmer_path(i)
            delay = (0.0, 0.33, 0.62)[i]
            s6.aimed(air, f, lambda ff, p=path, dl=delay: p((ff / N + dl) % 1.0), self.cast,
                     period=40, speed=6.0, kind="orb", phase=13 * k, count=2, life=34)
        self.cast.player(air, casters, f)
        shadows = r02.shadow_layer(casters, 12, 17, opacity=0.4)
        L = {"ground": ground, "shadows": shadows, "low-air": Image.new("RGBA", (W, H), (0, 0, 0, 0)),
             "air": air}
        L = r03.decorate(L, scroll_tex(self.fog, self.s.off("low-air", f)), "ground", casters,
                         bank_shadow_opacity=0.22, flyer_opacity=0.28)
        L["high-air"] = self.snow_layer(f)
        return L

    def snow_layer(self, f):
        img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        hr = H + 20
        for x0, y0, cyc, r in self.snow:
            x = (x0 + W * f / N + 6 * np.sin(th(f, 2) + y0)) % W
            y = (y0 + cyc * hr * f / N) % hr - 10
            a = int(150 + 60 * (r - 0.6))
            d.ellipse([x - r, y - r, x + r, y + r], fill=(236, 244, 250, a))
        return img

    def key_list(self):
        return [e6.skm.get(0.0, phase=0.0), self.ice_tex.crop((280, 100, 420, 260))]


# =========================================================================== D: Geneva (L14)

class GenevaScene(Scene8):
    slug = key = "geneva"
    title = "GENEVA CONCORD UNDER THE VRELL CANOPY"
    layers = ["ground", "low-air", "air", "high-air"]
    notes = {"ground": "X1.0 OLD CITY, LAKE, ROOTS, CREEP", "low-air": "X1.35 SPORE HAZE",
             "air": "PLAY PLANE", "high-air": "X2.0 VEINED CANOPY (<40%)"}
    TRAVEL = 600                 # 150 px/s
    FACTORS = {"ground": 1.0, "low-air": 1.35, "high-air": 2.0}
    GIF_COLORS = 112
    FOUNTAIN = (64, 330)
    PLAZA = (300, 150)
    HIVE = (380, 470)

    def shore_x(self, y):
        t = TAU * y / self.TRAVEL
        return 128 + 34 * np.sin(t) + 12 * np.sin(2 * t + 1.1)

    def __init__(self):
        super().__init__()
        per = self.TRAVEL
        self.ground_tex = self._city(per)
        lp, hp = self.s.period("low-air"), self.s.period("high-air")
        haze = [(60, 40, 78), (90, 62, 110), (112, 92, 132), (128, 120, 150)]
        self.haze = bank_tex(lp, [270, 135, 54, 27], 1441, 0.64, 0.12, haze, max_alpha=180)
        self.canopy = self._canopy(hp)
        rng = np.random.default_rng(1450)
        self.spores = [(rng.uniform(0, W), rng.uniform(0, H), rng.uniform(0, TAU)) for _ in range(40)]

    # ---- ground: the old city by the lake, overgrown
    def _city(self, per):
        rng = np.random.default_rng(1401)
        yy, xx = np.mgrid[0:per, 0:W].astype(np.float64)
        shore = self.shore_x(yy)
        lake = xx < shore
        col = np.zeros((per, W, 3))
        ln = r02.periodic_fbm(W, per, 60, 1402, octaves=3)
        col[:] = np.array((26, 30, 48)) + ln[..., None] * np.array((10, 12, 18))
        # street grid and perimeter blocks with pitched roofs (height field -> hillshade)
        hgt = np.zeros((per, W))
        rooftone = np.zeros((per, W))
        block_w, block_h, street = 58, 50, 10
        park = np.zeros((per, W), bool)
        for by in range(0, per, block_h + street):
            ox = (by // (block_h + street)) % 2 * 22
            for bx in range(-40 + ox, W, block_w + street):
                x0, x1, y0, y1 = bx, bx + block_w, by, by + block_h
                cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
                if cx < self.shore_x(cy) + 26:
                    continue
                if np.hypot(cx - self.PLAZA[0], s6.periodic_dist(cy, self.PLAZA[1], per)) < 70:
                    continue
                kind = rng.random()
                xs0, xs1 = max(0, int(x0)), min(W, int(x1))
                if xs0 >= xs1:
                    continue
                sub_y = slice(int(y0), int(y1))
                sx_ = xx[sub_y, xs0:xs1]
                sy_ = yy[sub_y, xs0:xs1]
                if kind < 0.16:                         # park
                    park[sub_y, xs0:xs1] = True
                    continue
                ring = 9.0 if kind < 0.75 else 7.0
                dx = np.minimum(sx_ - x0, x1 - sx_)
                dy = np.minimum(sy_ - y0, y1 - sy_)
                d = np.minimum(dx, dy)
                inner = d > 2 * ring
                roof = np.where(inner, 0, np.clip(ring - np.abs(d - ring), 0, ring))
                hgt[sub_y, xs0:xs1] = roof
                rooftone[sub_y, xs0:xs1] = np.where(inner, -1, kind)
        shade = s6.hillshade(hgt * 0.9, 0.55)
        roofs = rooftone > 0
        palette = [(62, 64, 74), (54, 76, 74), (96, 62, 54), (74, 70, 70)]
        for i, c in enumerate(palette):
            sel = roofs & (rooftone >= 0.16 + i * 0.21) & (rooftone < 0.16 + (i + 1) * 0.21 + (i == 3))
            col[sel] = np.array(c) * shade[sel][..., None]
        court = rooftone == -1
        col[court] = (32, 34, 40)
        streets = ~lake & ~roofs & ~court & ~park
        col[streets] = (38, 38, 46)
        col[park & ~lake] = np.array((26, 40, 32)) + ln[park & ~lake][..., None] * np.array((6, 14, 8))
        col[lake] = np.array((22, 30, 52)) + ln[lake][..., None] * np.array((12, 16, 26))
        prom = (~lake) & (xx < shore + 6)
        col[prom] = (92, 88, 96)
        # mottled canopy shadow baked into the ground (world-locked) with light pools
        d1, d2, idx = s6.voronoi_periodic(W, per, 46, 1403)
        vein = np.clip(1 - (d2 - d1) / 9.0, 0, 1)
        holes = (idx % 4 == 0)
        lightm = np.clip(1 - vein, 0, 1) * np.where(holes, 1.0, 0.45)
        lightm = s6.pblur_mask(lightm, 6.0)
        col = col * (0.5 + 0.75 * lightm[..., None]) + np.array((18, 8, 30)) * (1 - lightm[..., None])
        img = s6.smooth_tex(posterize(raster.to_rgba_image(np.clip(col, 0, 255)), 18), 3)
        img = self._overgrowth(img, per)
        img = self._landmarks(img, per)
        return img

    def _overgrowth(self, img, per):
        """The Siege Spire's roots running down from the city centre (ahead), branching over
        the roofs, and creep spreading from them; teal seams glow."""
        ss = 2
        lay = Image.new("RGBA", (W * ss, per * ss), (0, 0, 0, 0))
        glow = Image.new("RGBA", (W * ss, per * ss), (0, 0, 0, 0))
        d, g = ImageDraw.Draw(lay), ImageDraw.Draw(glow)
        rng = np.random.default_rng(1411)
        body, tip = (46, 40, 58), (0, 255, 154)
        for x0, ang, ln_, w in ((300, 1.75, 300, 12.0), (420, 1.95, 260, 10.0), (230, 1.45, 220, 8.0)):
            s6.branches(d, g, rng, x0, -20, ang, ln_, w, 2, body, tip, ss, per, step=6, jitter=0.25)
        out = img.copy()
        for cx, cy in ((self.HIVE[0], self.HIVE[1]), (240, 300), (440, 120)):
            for dy in (-per, 0, per):
                e6.creep(out, cx, cy + dy, 70, int(cx))
        lay = lay.resize((W, per), Image.BOX)
        glow = glow.resize((W, per), Image.BOX)
        out.alpha_composite(s6.pshadow(lay, 0.55, 1.2), (5, 7))
        out.alpha_composite(lay)
        # glowing seam along the roots
        seam = lay.copy()
        a = np.asarray(seam).copy()
        a[..., :3] = (0, 160, 110)
        a[..., 3] = (a[..., 3] > 200) * 90
        out.alpha_composite(Image.fromarray(a, "RGBA").filter(ImageFilter.MinFilter(3)))
        out.alpha_composite(s6.pmid(raster.glow(s6.ptile(glow), 2.0, 0.9), per))
        return out

    def _landmarks(self, img, per):
        out = img.copy()
        rot = model("rotunda")
        px, py = self.PLAZA
        d = ImageDraw.Draw(out)
        for dy in (-per, 0, per):
            d.ellipse([px - 66, py - 66 + dy, px + 66, py + 66 + dy], fill=(84, 82, 92, 255))
            d.ellipse([px - 64, py - 64 + dy, px + 64, py + 64 + dy], outline=(110, 106, 116, 255), width=2)
            for k in range(16):                          # ring of flagpoles, UTC blue flags
                a = TAU * k / 16
                fx, fy = px + 58 * np.cos(a), py + dy + 58 * np.sin(a)
                d.line([fx, fy, fx + 4, fy + 1], fill=(60, 110, 200, 255), width=2)
                d.point([(fx, fy)], fill=(200, 200, 210, 255))
            sh = sprite.shadow_of(rot, opacity=0.55, blur=1.5)
            out.alpha_composite(sh, (int(px - rot.width / 2 + 8), int(py + dy - rot.height / 2 + 10)))
            sprite.paste_center(out, rot, px, py + dy)
        greens = [(10, 20, 14), (20, 34, 24), (30, 50, 34), (44, 66, 44)]
        trees = [r03.foliage_sprite(14, 1420 + k, greens, night=0.8) for k in range(3)]
        for y in range(8, per, 22):                     # tree-lined lakeside promenade
            x = self.shore_x(y) + 14
            t = trees[y % 3]
            for dy in (-per, 0, per):
                sprite.paste_center(out, sprite.shadow_of(t, opacity=0.5, blur=1.0), x + 3, y + dy + 4)
                sprite.paste_center(out, t, x, y + dy)
        fx, fy = self.FOUNTAIN                          # the Jet d'Eau's pier
        for dy in (-per, 0, per):
            d.line([fx, fy + dy, self.shore_x(fy) + 2, fy + 20 + dy], fill=(110, 106, 112, 255), width=3)
            d.ellipse([fx - 3, fy - 3 + dy, fx + 3, fy + 3 + dy], fill=(150, 150, 160, 255))
        return out

    def _canopy(self, per):
        """Vrell canopy membrane: veined cells of dark plum tissue with teal veins, thin
        membrane panes and torn holes; opacity <= ~40 % (high-air rule)."""
        d1, d2, idx = s6.voronoi_periodic(W, per, 70, 1431)
        edge = d2 - d1
        vein = np.clip(1 - edge / 5.0, 0, 1)
        core = np.clip(1 - edge / 1.6, 0, 1)
        holes = (idx % 3 == 0)
        pane = np.where(holes, 0.0, 0.16)
        a = np.maximum(vein * 0.4, pane)
        col = np.zeros((per, W, 3))
        col[:] = (58, 30, 70)
        col = col * (1 - core[..., None]) + np.array((40, 170, 130)) * core[..., None]
        img = raster.to_rgba_image(col, a * 255)
        return r03.step_alpha(posterize(img, 10), 6)

    def layer_images(self, f):
        off = self.s.off("ground", f)
        ground = scroll_tex(self.ground_tex, off)
        fx, fyw = self.FOUNTAIN                         # the Jet d'Eau: plume blown downwind
        for fy in self.ys("ground", fyw, f, margin=60):
            fm = Foam(color=(226, 232, 240))
            stream(fm, f, fx, fy, 0.35, 0.94, 60, 8, 40, 1.0, 9.0, 1.0, 3.0, 0.9, 1460, wave=2)
            stream(fm, f, fx, fy, 0.35, 0.94, 30, 8, 20, 0.5, 3.0, 1.4, 2.4, 1.0, 1461)
            ground.alpha_composite(fm.done())
        # the Hive Node on its square, iris breathing
        iris = round(0.5 + 0.5 * np.sin(th(f, 2)), 1)
        hv = e6.hive.get(0.0, iris=round(iris * 4) / 4, pulse=round(1 + np.sin(th(f, 4))) / 2)
        for hy in self.ys("ground", self.HIVE[1], f, margin=60):
            sprite.paste_center(ground, sprite.shadow_of(hv, opacity=0.5, blur=1.2), self.HIVE[0] + 6, hy + 8)
            sprite.paste_center(ground, hv, self.HIVE[0], hy)
        # a Creeper convoy crawling along a street (planted feet: phase from distance)
        for k in range(3):                               # spaced a third of the period apart, so
            yw = 40 + k * 200 + 200 * f / N               # the convoy repeats every loop
            x = 236 + 6 * np.sin(yw * 0.02)
            for yy in self.ys("ground", yw, f, margin=50):
                sp = e6.crp_sprite(np.pi / 2, yw)
                sprite.paste_center(ground, sprite.shadow_of(sp, opacity=0.5, blur=1.0), x + 4, yy + 6)
                sprite.paste_center(ground, sp, x, yy)
        air = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        casters = []
        s6.skitter_snake(air, casters, self.cast, f, 330, 100, 0.1)
        nx, ny = 140 + 50 * np.sin(th(f)), 120 + 16 * np.sin(th(f, 2))
        nd = self.cast.unit("needler-a", anim=[-1.0, 0.0, 1.0, 0.0][(f // 4) % 4])
        casters.append((nd, nx, ny))
        sprite.paste_center(air, nd, nx, ny)
        s6.aimed(air, f, lambda ff: (140 + 50 * np.sin(th(ff)), 120 + 16 * np.sin(th(ff, 2)) + 14),
                 self.cast, period=40, speed=6.5, kind="thorn", phase=6)

        def hive_src(ff):
            y = (self.HIVE[1] + self.s.off("ground", ff)) % self.TRAVEL
            return (self.HIVE[0], y) if 0 <= y <= H else None
        fan_shots(air, f, hive_src, self.cast, period=40, speed=5.0, n=5, spread=0.24, phase=24,
                  kind="acid")
        self.cast.player(air, casters, f)
        shadows = r02.shadow_layer(casters, *s6.SHADOW["air"], opacity=0.45)
        low = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        d = ImageDraw.Draw(low)
        for x0, y0, ph in self.spores:                  # drifting spores
            x = (x0 + 0.5 * W * 0 + 10 * np.sin(th(f) + ph)) % W
            y = (y0 + 1.35 * self.TRAVEL * f / N) % (H + 20) - 10
            d.ellipse([x - 1.5, y - 1.5, x + 1.5, y + 1.5], fill=(120, 230, 180, 170))
        L = {"ground": ground, "shadows": shadows, "low-air": low, "air": air}
        L = r03.decorate(L, scroll_tex(self.haze, self.s.off("low-air", f)), "ground", casters,
                         bank_shadow_opacity=0.25, flyer_opacity=0.3)
        L["high-air"] = scroll_tex(self.canopy, self.s.off("high-air", f))
        return L

    def key_list(self):
        return [model("rotunda"), e6.hive.get(0.0, iris=0.5, pulse=0.5),
                e6.crp_sprite(np.pi / 2, 0.0), self.ground_tex.crop((260, 90, 340, 210))]


# =========================================================================== E: Luna far side (L06)

class LunaFarsideScene(s6.LunaScene, Scene8):
    slug = key = "luna-farside"
    title = "LUNA FAR SIDE - DARK, LIT BY FLARES AND VRELL GLOW"
    layers = ["ground", "low-air", "air", "high-air"]
    notes = {"ground": "X1.0 DARK REGOLITH, DOMES, NESTS", "low-air": "X1.35 FLARES, PLUMES",
             "air": "PLAY PLANE + HEADLIGHT", "high-air": "X2.2 EJECTED ROCK"}
    ORDER = s6.Scene.ORDER
    GIF_COLORS = 104

    def __init__(self):
        s6.LunaScene.__init__(self)
        a = np.asarray(self.ground_tex).astype(np.float64)
        lum = a[..., :3].mean(-1, keepdims=True)
        dark = a[..., :3] * 0.22 + np.array((4, 6, 14)) * 0.6
        glowing = (a[..., 1] > 170) & (a[..., 0] < 90)          # Vrell teal stays bright
        warm = (a[..., 0] > 200) & (a[..., 2] < 120)            # warm lights, posts stay
        keep = (glowing | warm)[..., None]
        a[..., :3] = np.where(keep, a[..., :3], dark + lum * 0.05)
        img = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")
        per = self.TRAVEL
        for x, y, r, c, k in ((300, 150, 60, (255, 180, 90), 0.5), (336, 214, 40, (255, 180, 90), 0.4),
                              (290, 420, 36, (255, 210, 120), 0.35), (70, 470, 70, (0, 255, 154), 0.35)):
            for dy in (-per, 0, per):
                img = raster.add_light(img, x, y + dy, r, c, k)
        self.ground_tex = img
        self.plumes = r02.alpha_scale(s6.tinted(self.plumes, (20, 22, 34), 0.75, 0.5), 0.6)

    def ground_image(self, f):
        img = s6.LunaScene.ground_image(self, f)
        # falling flares light pools that drift with their flare
        for k in range(2):
            u = (f / N + k * 0.5) % 1.0
            x, y = 120 + 260 * k + 30 * np.sin(TAU * u), -40 + 620 * u
            img = raster.add_light(img, x + 20, y + 30, 60, (255, 170, 100), 0.42 * (1 - 0.4 * u))
        return img

    def layer_images(self, f):
        L = s6.LunaScene.layer_images(self, f)
        low = L["low-air"]
        for k in range(2):
            u = (f / N + k * 0.5) % 1.0
            x, y = 120 + 260 * k + 30 * np.sin(TAU * u), -40 + 620 * u
            fl = Image.new("RGBA", (W, H), (0, 0, 0, 0))
            d = ImageDraw.Draw(fl)
            d.line([x, y - 40, x, y], fill=(255, 220, 160, 60), width=2)
            d.ellipse([x - 3, y - 3, x + 3, y + 3], fill=(255, 240, 200, 255))
            low.alpha_composite(raster.glow(fl, 4.0, 1.2))
        air = L["air"]
        # the Mantis at the left side and the headlight cone ahead of the ship
        mx, my = 40 + 6 * np.sin(th(f)), 250 + 60 * np.sin(th(f) + 1)
        ms = self.cast.unit("mantis-a")
        air.alpha_composite(Image.new("RGBA", (W, H), (0, 0, 0, 0)))
        sprite.paste_center(air, ms, mx, my)
        px, py, _ = self.cast.player_pos(f)
        cone = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        d = ImageDraw.Draw(cone)
        d.polygon([(px - 8, py - 20), (px + 8, py - 20), (px + 70, py - 230), (px - 70, py - 230)],
                  fill=(200, 220, 255, 26))
        L["ground"] = L["ground"].copy()
        L["ground"].alpha_composite(cone.filter(ImageFilter.GaussianBlur(10)))
        return L

    def key_list(self):
        return [self.cast.unit("mantis-a")]


SCENES = {"ocean": OceanScene, "storm": StormScene, "arctic": ArcticScene, "geneva": GenevaScene,
          "luna-farside": LunaFarsideScene}

NOTES = {
    "ocean": "OPEN OCEAN (L11): THE CONVOY STEAMS AT THE SCROLL SPEED, SO IT HOLDS STATION WHILE THE "
             "SEA STREAMS PAST: KELVIN WAKES ARE ADDED TO THE WAVE HEIGHT FIELD, BOW WAVES AND STERN "
             "WAKES ARE FOAM STREAMS; JELLIES SHOW THEIR TENTACLES THROUGH THE SURFACE; A VAST SHAPE "
             "GLIDES UNDER THE WAVES (THE KRAKEN, FORESHADOWED).",
    "storm": "STORM (L12): A HEAVY PEAK - BIG SWELL, WHITECAPS AND WIND STREAKS, WAVES BREAKING ROUND THE "
             "PLATFORM LEGS, SCUD RACING SIDEWAYS (ONE SCREEN WIDTH PER LOOP), SLANTING RAIN, A LIGHTNING "
             "STRIKE. LAMPREYS STREAM IN; WHIRL SEEDS RIDE THE WIND.",
    "arctic": "ARCTIC (L13): FLOES ON BLACK WATER; SUBMERGED ICE SHOWS TEAL UNDER THE SURFACE, BROKEN FOAM "
              "AT EVERY WATERLINE, LONG COOL SHADOWS FROM A LOW SUN. SKIMMERS WEAVE THROUGH THE LEADS WITH "
              "WAKES LEFT IN THE MOVING WATER; A SCUTTLER STRIDES OVER THE RELAY'S ICE SHELF.",
    "geneva": "GENEVA (L14): THE OLD CITY BY THE LAKE UNDER THE VRELL CANOPY - MOTTLED CANOPY SHADOW WITH "
              "LIGHT POOLS, THE SPIRE'S ROOTS AND CREEP OVER THE ROOFS, THE CONCORD ROTUNDA, THE JET D'EAU; "
              "THE CANOPY ITSELF HANGS ON HIGH-AIR UNDER 40 % OPACITY.",
    "luna-farside": "FAR SIDE (L06): NO SUNLIGHT - ONLY MINING-DOME LIGHTS, VRELL GLOW, FALLING FLARES AND "
                    "THE SHIP'S HEADLIGHT LIGHT THE REGOLITH. MANTIS HOLD AT THE SIDES.",
}


def make_sheet(scene, f=24):
    frame_img = scene.compose(f)
    layers = scene.layer_images(f)
    tw, th_ = 192, 216
    img = raster.sheet(1140, 690, f"SCENE {scene.slug.upper()} (R08): {scene.title}",
                       f"CONCEPT ROUND 08 - {scene.intensity}")
    speed = scene.TRAVEL * FPS / N
    raster.draw_text(img, 16, 38, f"PLAY FIELD 480X540 AT 1X - GROUND SCROLLS {speed:.0f} PX/S",
                     raster.LABEL_DIM)
    img.alpha_composite(frame_img, (16, 50))
    raster.draw_text(img, 516, 38, "LAYER BREAKDOWN, BACK TO FRONT (0.4X)", raster.LABEL_DIM)
    for i, key in enumerate(scene.layers):
        x = 516 + (i % 3) * (tw + 12)
        y = 50 + (i // 3) * (th_ + 44)
        cell = r02.checker(tw, th_)
        lay = layers[key]
        if key == "air":
            lay = layers["shadows"].copy()
            lay.alpha_composite(layers["air"])
        cell.alpha_composite(lay.resize((tw, th_), Image.BOX))
        img.alpha_composite(cell, (x, y + 12))
        raster.draw_text(img, x, y, f"{i + 1}. {key.upper()}", raster.ACCENT)
        raster.draw_text(img, x, y + th_ + 16, scene.notes[key], raster.LABEL_DIM)
    notes = [
        "FACTORS RELATIVE TO GROUND: " + ", ".join(f"{k.upper()} {v:g}" for k, v in
                                                   scene.FACTORS.items()) + ".",
        NOTES[scene.slug],
        "WATER RULES: IRREGULAR FOAM COLLARS, RIPPLE TRAINS AND WAKES IN A LIT HEIGHT FIELD, SUBMERGED "
        "PARTS SEEN THROUGH THE SURFACE - NO DRAWN CIRCLES." if scene.slug in ("ocean", "storm", "arctic")
        else "ATMOSPHERE INTENSITY: MEDIUM; HIGH-AIR UNDER 40 % OPACITY.",
    ]
    y = 580
    for line in notes:
        for part in r02.wrap(line, 100):
            raster.draw_text(img, 516, y, part, raster.LABEL)
            y += 11
        y += 5
    return img


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    names = [a for a in sys.argv[1:] if a in SCENES] or list(SCENES)
    for slug in names:
        scene = SCENES[slug]()
        png = OUT / f"scene-{slug}-r08-a.png"
        make_sheet(scene).convert("RGB").save(png, optimize=True)
        print("wrote", png.relative_to(ROOT))
        if "--sheet" in sys.argv:
            continue
        gif = OUT / f"scene-{slug}-r08-a.gif"
        cols = s6.make_gif(scene, gif, scene.GIF_COLORS)
        print("wrote", gif.relative_to(ROOT), f"{gif.stat().st_size / 1e6:.1f} MB, {cols} colours")


if __name__ == "__main__":
    main()
