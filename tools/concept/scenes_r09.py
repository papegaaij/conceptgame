#!/usr/bin/env python3
"""Concept round 09 - the remaining Earth scenes for Acts 1-2 (and Luna's far side), finished.

Round 08 rendered only the ocean (deferred: "needs to be finished first"); this round finishes
it and renders the other four. The scenes, models and water code live in scenes_r08.py; this
script subclasses them with the corrections listed per scene below.

Outputs (design/art-direction/concept/):
  scene-ocean-r09-a.png / .gif         L11 Atlantic Convoy: slate-blue ocean with depth (a slow
                                       swell-colour field, sparse whitecaps), broken Kelvin
                                       wakes and churned prop-wash that flow with the water
                                       instead of dotted lines and hatched stripes, lighter
                                       mist and wisps (medium), a visible shape under the waves
  scene-storm-r09-a.png / .gif         L12 Storm Front (heavy peak): dark heaving sea, small
                                       broken whitecaps and long wind-streak foam lines, waves
                                       breaking round the fusion platform's legs, a patrol boat
                                       ploughing through the swell, dark scud racing sideways,
                                       slanting rain and a lightning flash; Lampreys, Whirl
                                       Seeds and a Needler whose needles test readability
  scene-arctic-r09-a.png / .gif        L13 Polar Relay: ice floes on black water with leads
                                       between them, submerged ice showing teal, broken foam at
                                       the waterlines, Skimmers weaving with V-wakes in the
                                       moving water, the relay on its ice shelf, thin fog
  scene-geneva-r09-a.png / .gif        L14 Geneva Concord under the Vrell canopy: varied old-city
                                       blocks along the lake, the Concord rotunda, the Spire's
                                       roots and creep, mottled canopy shadow, thinner spore haze
  scene-luna-farside-r09-a.png / .gif  L06 Farside: the dark side of the Moon lit only by dome
                                       lights, Vrell glow, falling flares and the ship's
                                       headlight; Mantis at the sides, a Coilwyrm

Rules: design/art-direction/README.md (layer model, decoration and atmosphere intensity -
medium, the storm a heavy peak - and the Water rules). Each PNG shows the play field at 1x plus
the layer breakdown; each GIF is a seamless 4 s loop (80 frames at 20 fps) with the bullet
colours reserved in its palette.
Run: python3 tools/concept/scenes_r09.py [ocean] [storm] [arctic] [geneva] [luna-farside] [--sheet]
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, str(Path(__file__).resolve().parent))
import enemies_r06 as e6  # noqa: E402
import parallax_r02 as r02  # noqa: E402
import parallax_r03 as r03  # noqa: E402
import scenes_r06 as s6  # noqa: E402
import scenes_r08 as s8  # noqa: E402
from parallax_r02 import H, W, scroll_tex  # noqa: E402
from render import raster, sprite  # noqa: E402
from render.config import ROOT  # noqa: E402

OUT = s8.OUT
N, FPS, TAU = s8.N, s8.FPS, s8.TAU
XX, YY = s8.XX, s8.YY
th = s8.th


# =========================================================================== shared water

def broken_kelvin(h, x, ybow, rows, brk, amp=0.55, decay=230.0):
    """A Kelvin wake that is not a drawn pattern: the two divergent arms and transverse waves
    of scenes_r08.kelvin, broken up by a noise field that flows with the water (``brk`` is
    world-space noise, sampled through ``rows``), with a ragged envelope and a shorter tail."""
    y0 = int(max(0, ybow))
    if y0 >= H:
        return
    dy = YY[y0:] - ybow
    dx = np.abs(XX[y0:] - x)
    n = brk[rows[y0:]]
    arm = 0.36 * dy * (1 + 0.12 * (n - 0.5))
    wdt = 3.0 + 0.09 * dy
    env = np.exp(-dy / (decay * (0.7 + 0.6 * n))) * np.clip(dy / 12, 0, 1)
    div = np.exp(-((dx - arm) / wdt) ** 2) * np.sin(0.45 * (0.78 * dy + 0.62 * dx) + 3 * n)
    inside = 1 / (1 + np.exp((dx - arm) / 4.0))
    trans = 0.45 * inside * np.sin(0.21 * dy + 2.5 * n) * np.clip((dy - 30) / 40, 0, 1)
    gate = np.clip((n - 0.25) * 2.2, 0, 1)
    h[y0:] += amp * env * gate * (1.4 * div + trans)


def wash_mask(x, ystern, rows, tex, width0=5.0, spread=0.10, length=300.0, soft=0.12):
    """Churned prop-wash behind a stern: a band that widens and fades downstream, filled with
    world-space foam noise so the churn flows away with the sea (no dotted lines). Returns an
    alpha 0..1 over the screen."""
    out = np.zeros((H, W), np.float32)
    y0 = int(max(0, ystern))
    if y0 >= H:
        return out
    dy = YY[y0:] - ystern
    dx = np.abs(XX[y0:] - x)
    wid = width0 + spread * dy
    band = np.exp(-(dx / wid) ** 2) * np.exp(-dy / length) * np.clip(dy / 4 + 0.3, 0, 1)
    n = tex[rows[y0:]]
    out[y0:] = np.clip((n - (1 - 0.75 * band)) / soft, 0, 1) * np.clip(band * 1.5, 0, 1)
    return out


def apply_foam(body, alpha, color=s8.FOAM, levels=4):
    """Blend foam (alpha 0..1, stepped to a few levels like a 90s translucency table)."""
    a = np.round(np.clip(alpha, 0, 1) * levels) / levels
    return body + (np.array(color, np.float32) - body) * a[..., None]


def tone_field(per, seed, cell=160):
    return r02.periodic_fbm(W, per, cell, seed, octaves=3).astype(np.float32)


def vstreak(per, cell, seed, k=3, octaves=3):
    """Periodic noise elongated ``k`` times along the scroll (streaks drawn out by the flow):
    rendered k times wider, then squeezed horizontally, so y stays exactly periodic."""
    n = r02.periodic_fbm(W * k, per, cell, seed, octaves=octaves)
    img = Image.fromarray((np.clip(n, 0, 1) * 255).astype(np.uint8)).resize((W, per), Image.BOX)
    return np.asarray(img, np.float32) / 255


# =========================================================================== A: ocean (L11)

OCEAN_STOPS = [(0.0, (10, 24, 36)), (0.3, (20, 40, 56)), (0.55, (34, 60, 78)),
               (0.8, (60, 88, 104)), (1.0, (112, 136, 148))]   # top tone calmer than r08


class OceanScene(s8.OceanScene):
    """Round 08 corrections: the Kelvin wakes were regular hatching across half the screen and
    the stern wakes dotted lines; the sea was flat in depth; mist and wisps were too strong."""

    notes = dict(s8.OceanScene.notes, **{"ground": "X1.0 SEA, CONVOY, WAKES, JELLIES",
                                          "low-air": "X1.3 SEA MIST (22 %), SMOKE",
                                          "high-air": "X2.0 THIN WISPS (<22 % ALPHA)"})

    def __init__(self):
        super().__init__()
        per = self.TRAVEL
        self.sea.stops = OCEAN_STOPS
        self.tone = tone_field(per, 1150, 160)        # slow swell-colour field: depth in the sea
        self.brk = tone_field(per, 1151, 48)          # breaks up the Kelvin arms
        self.churn = vstreak(per, 24, 1152, k=3)
        self.capn = r02.periodic_fbm(W, per, 24, 1153, octaves=3).astype(np.float32)
        lp, hp = self.s.period("low-air"), self.s.period("high-air")
        greys = [(92, 104, 114), (120, 132, 140), (146, 156, 162), (166, 174, 180)]
        self.mist = s8.bank_tex(lp, [208, 104, 52], 1141, 0.6, 0.12, greys, max_alpha=120,
                                stretch=2)
        self.wisps = s6.wisp_tex(hp, [192, 96, 48], 1142, (206, 214, 220), thresh=0.54, gain=2.6,
                                 max_a=56, stretch=3)

    def jelly_sub_depth(self):
        return 0.32

    def layer_images(self, f):
        off = self.s.off("ground", f)
        rows = self.sea.rows(off)
        h = self.sea.height(f, off)
        for sh in self.ships:
            x, y = sh.pos(f)
            broken_kelvin(h, x, y - sh.half + 10, rows, self.brk, amp=0.6 * sh.wake)
        sub = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        top = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        foam = s8.Foam()
        kx = 250 + 30 * np.sin(th(f))
        for ky in self.ys("ground", 380, f, margin=220):
            deep_shape(sub, kx, ky + 30 * np.sin(th(f) + 1), f)
        for j in self.JELLIES:
            x, y0, surf, ph = self.jelly_world(j, f)
            pulse = 0.5 + 0.5 * np.sin(th(f, 2) + ph)
            for y in self.ys("ground", y0, f, margin=40):
                if surf:
                    s8.jelly_surface(top, foam, sub, f, x, y, pulse, int(ph * 10), s8.OCEAN_WATER)
                    for k in range(2):
                        age = ((2 * f / N + ph / TAU + k * 0.5) % 1.0) * 2.0
                        s8.ripple_train(h, x, y, age, 1.6, amp=0.4, lam=8, seed=ph + k)
                else:
                    sp = e6.jly.get(0.0, pulse=round(pulse, 1))
                    sprite.paste_center(sub, s8.deep_tint(sp, s8.OCEAN_WATER, self.jelly_sub_depth(),
                                                          blur=0.8), x, y)
        for i, (rx, ry) in enumerate(self.RAFTS):
            for y in self.ys("ground", ry, f, margin=60):
                bob = 1.2 * np.sin(th(f, 2) + i)
                raft = e6.raft.get(0.0)
                sprite.paste_center(sub, s8.deep_tint(raft, s8.OCEAN_WATER, 0.3, blur=0.6, alpha=0.8),
                                    rx + 2, y + 3)
                sprite.paste_center(top, raft, rx, y + bob)
                px, py, _ = self.cast.player_pos(f)
                sprite.paste_center(top, e6.gun.get(np.arctan2(py - y, px - rx)), rx, y + bob - 2)
                ang = np.linspace(0, TAU, 40, endpoint=False)
                pts = np.stack([np.cos(ang) * 40, np.sin(ang) * 36], 1)
                s8.collar(foam, f, pts, rx, y + bob, 40 + i, alpha=0.6, r=1.2)
                s8.ripple_train(h, rx, y, ((f / N * 2 + i * 0.5) % 1.0) * 1.6, 1.6, amp=0.35,
                                lam=9, seed=3 + i)
        wash = np.zeros((H, W), np.float32)
        for sh in self.ships:
            x, y = sh.pos(f)
            wash = np.maximum(wash, wash_mask(x, y + sh.half - 8, rows, self.churn,
                                              width0=sh.sp.width * 0.2, length=330 * sh.wake))
            self.ship_foam(foam, sh, f)
        light = self.sea.light(h)
        body = self.sea.colour(light)
        tone = self.tone[rows]                          # deeper and paler swaths of sea
        body = body * (0.84 + 0.28 * tone[..., None]) + np.array((0, 6, 4)) * (tone[..., None] - 0.5)
        caps = np.clip((h - 1.25) * 1.6, 0, 1) * (self.capn[rows] > 0.62)
        body = apply_foam(body, caps * 0.7, (190, 204, 210), levels=3)
        body = s8.through_surface(body, s8.wobble_layer(sub, f), self.sea.mean(), 0.9)
        body = apply_foam(body, wash * 0.9, (196, 214, 214), levels=4)
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
        s8.fan_shots(air, f, raft_src(0), self.cast, period=40, speed=5.5, phase=8)
        s8.fan_shots(air, f, raft_src(1), self.cast, period=40, speed=5.5, phase=28)
        self.cast.player(air, casters, f)
        shadows = r02.shadow_layer(casters, *s6.SHADOW["air"], opacity=0.42)
        low = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        s8.smoke(low, f, *self.burning().pos(f), self.TRAVEL)
        L = {"ground": ground, "shadows": shadows, "low-air": low, "air": air}
        L = r03.decorate(L, scroll_tex(self.mist, self.s.off("low-air", f)), "ground", casters,
                         bank_shadow_opacity=0.22, flyer_opacity=0.3)
        L["high-air"] = scroll_tex(self.wisps, self.s.off("high-air", f))
        L["sub"] = sub
        return L

    def ship_foam(self, foam, sh, f):
        """Bow waves peeling off along the Kelvin arms, a thin hull-side wash and the broken
        foam collar; the stern churn is the wash mask in the water itself."""
        x, y = sh.pos(f)
        hw = sh.sp.width / 2 - 6
        bow = y - sh.half + 8
        for side in (-1, 1):
            s8.stream(foam, f, x + side * 3, bow + 12, side * 0.36, 0.93, self.TRAVEL / 4, 3, 60,
                      0.8, 5.0, 0.7, 1.5, 0.7 * sh.wake, sh.seed + side, wave=0.6)
            s8.stream(foam, f, x + side * hw, y, side * 0.1, 1.0, self.TRAVEL / 5, 4, 26,
                      0.4, 2.0, 0.6, 1.1, 0.45 * sh.wake, sh.seed + 5 + side, wave=0.4)
        s8.collar(foam, f, sh.pts, x, y, sh.seed + 3, alpha=0.7, r=1.1)


def deep_shape(img, x, y, f):
    """The vast creature far under the waves (the Harbour Kraken, foreshadowed): a mantle with
    six trailing arms that sway, very soft and dark-teal, so it reads through the surface
    tones as something big without stealing the play plane."""
    lay = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(lay)
    c = (2, 10, 18, 225)
    d.ellipse([x - 44, y - 82, x + 44, y + 44], fill=c)
    d.ellipse([x - 30, y - 100, x + 30, y - 40], fill=c)
    for k in range(6):
        a0 = np.pi / 2 + (k - 2.5) * 0.3
        pts = []
        for u in np.linspace(0, 1, 16):
            ang = a0 + 0.55 * np.sin(th(f) + k * 1.1 + u * 3) * u
            r = 30 + 170 * u
            pts.append((x + np.cos(ang) * r * 0.75, y + 24 + np.sin(ang) * r))
        d.line(pts, fill=c, width=13 - 2 * (k % 3))
    img.alpha_composite(lay.filter(ImageFilter.GaussianBlur(8)))


# =========================================================================== B: storm (L12)

STORM_STOPS = [(0.0, (5, 11, 18)), (0.3, (11, 23, 32)), (0.55, (20, 38, 50)),
               (0.8, (38, 58, 70)), (1.0, (84, 104, 114))]


class StormScene(s8.StormScene):
    """Round 08 draft corrections: whitecaps and wind streaks were huge flat white and grey
    patches (camouflage, not sea), the leg ripples were drawn rings, the boat's wake hatched,
    the scud mid-grey and too uniform."""

    notes = dict(s8.StormScene.notes, **{"low-air": "X1.5 DARK SCUD (34 %), SIDEWAYS",
                                          "high-air": "X2.2 RAIN, FLASH"})

    def __init__(self):
        super().__init__()
        per = self.TRAVEL
        self.sea.stops, self.sea.light_gain, self.sea.tones = STORM_STOPS, 2.8, 8
        self.capn = r02.periodic_fbm(W, per, 16, 1251, octaves=3).astype(np.float32)
        wind = r03.stretched(W, per, [64, 32, 16], 1252, 8).astype(np.float32)
        self.windline = np.clip(1 - np.abs(wind - 0.5) / 0.05, 0, 1)   # thin streak lines
        self.windgate = tone_field(per, 1253, 128)
        self.tone = tone_field(per, 1254, 160)
        self.brk = tone_field(per, 1255, 32)
        self.churn = vstreak(per, 32, 1256, k=3)
        lp = self.s.period("low-air")
        n = r03.noise(W, lp, [160, 80, 32, 16], 1221)
        tones = [(18, 22, 28), (32, 37, 44), (50, 56, 62), (70, 76, 82)]
        self.scud = r03.step_alpha(r02.posterize(r03.bank(n, 0.48, 0.12, tones, max_alpha=215,
                                                          light=6.0), 12), 6)
        rng = np.random.default_rng(1230)
        self.rain = [(rng.uniform(0, W), rng.uniform(0, H + 40), int(rng.integers(5, 9)))
                     for _ in range(300)]

    def layer_images(self, f):
        off = self.s.off("ground", f)
        rows = self.sea.rows(off)
        h = self.sea.height(f, off)
        bx, by = self.boat.pos(f)
        broken_kelvin(h, bx, by - self.boat.half + 10, rows, self.brk, amp=0.9, decay=200)
        foam = s8.Foam()
        top = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        legs = []
        hs = 56
        for py in self.ys("ground", self.PLATFORM[1], f, margin=120):
            px = self.PLATFORM[0]
            for lx, ly in ((-hs, -hs), (hs, -hs), (-hs, hs), (hs, hs)):
                legs.append((px + lx, py + ly))
            plat = s8.model("platform")
            sprite.paste_center(top, sprite.shadow_of(plat, opacity=0.45, blur=2.5), px + 9, py + 12)
            sprite.paste_center(top, plat, px, py)
            if (f // 5) % 2 == 0:
                for lx, ly in ((-hs, -hs), (hs, -hs), (-hs, hs), (hs, hs)):
                    top = raster.add_light(top, px + lx, py + ly, 7, (255, 60, 40), 0.9)
        # waves breaking round the legs: the swell streams past them downwind, so the churn
        # texture moves through the legs (2 periods per loop, an integer: seamless)
        flow = (rows - int(round(2 * self.TRAVEL * f / N))) % self.TRAVEL
        wash = np.zeros((H, W), np.float32)
        for k, (lx, ly) in enumerate(legs):
            wash = np.maximum(wash, wash_mask(lx, ly - 10, flow, self.churn, width0=8, spread=0.16,
                                              length=90, soft=0.1))
            age = ((f / N * 4 + k * 0.27) % 1.0) * 1.2
            s8.ripple_train(h, lx, ly, age, 1.2, amp=0.7, lam=11, speed=30, seed=k)
            ang = np.linspace(0, TAU, 26, endpoint=False)
            pts = np.stack([np.cos(ang) * 10, np.sin(ang) * 9], 1)
            s8.collar(foam, f, pts, lx, ly, 70 + k, alpha=0.95, r=1.6, push=3.0)
            s8.stream(foam, f, lx, ly - 8, 0.3, -0.95, 22, 8, 14, 1, 6, 1.0, 2.2, 0.9, 90 + k,
                      wave=1.5)                              # spray thrown up the legs
        wash = np.maximum(wash, wash_mask(bx, by + self.boat.half - 6, rows, self.churn,
                                          width0=self.boat.sp.width * 0.25, length=300))
        hw = self.boat.sp.width / 2 - 4
        bow = by - self.boat.half + 8
        for side in (-1, 1):
            s8.stream(foam, f, bx + side * 3, bow + 10, side * 0.4, 0.92, self.TRAVEL / 5, 4, 56,
                      1.0, 6.0, 0.8, 1.8, 0.85, 1210 + side, wave=1.0)
            s8.stream(foam, f, bx + side * hw, by, side * 0.1, 1.0, self.TRAVEL / 6, 4, 20,
                      0.4, 2.0, 0.6, 1.2, 0.5, 1215 + side)
            s8.stream(foam, f, bx + side * 4, bow, side * 1.0, 0.35, 40, 8, 22, 2, 8, 1.0, 2.2, 0.9,
                      1240 + side, wave=3)                   # spray off the bow
        s8.collar(foam, f, self.boat.pts, bx, by, 1213, alpha=0.8, r=1.2)
        light = self.sea.light(h)
        body = self.sea.colour(light)
        tone = self.tone[rows]
        body = body * (0.82 + 0.34 * tone[..., None])
        # whitecaps: only the steepest crests break, in small torn patches with spindrift
        caps = np.clip((h - 1.55) * 1.4, 0, 1) * np.clip((self.capn[rows] - 0.54) * 8, 0, 1)
        streak = self.windline[rows] * np.clip((self.windgate[rows] - 0.45) * 4, 0, 1)
        streak *= np.clip(0.5 + 0.4 * h, 0, 1)
        body = apply_foam(body, streak * 0.55, (150, 166, 172), levels=3)
        body = apply_foam(body, caps * 0.85, (196, 208, 212), levels=3)
        body = apply_foam(body, wash * 0.9, (190, 204, 208), levels=4)
        ground = raster.to_rgba_image(np.clip(body, 0, 255))
        ground.alpha_composite(foam.done())
        ground.alpha_composite(top)
        self.boat.draw(ground, f)
        if f in self.FLASH:                                  # the strike lights the sea
            fl = np.asarray(ground).astype(np.float32)
            fl[..., :3] += (np.array((150, 165, 190)) * self.FLASH[f] * 1.2)
            ground = Image.fromarray(np.clip(fl, 0, 255).astype(np.uint8), "RGBA")
        air = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        casters = []
        self.lampreys(air, casters, f)
        cx = -60 + 600 * ((f / N + 0.15) % 1.0)
        s6.seed_cluster(air, casters, f, cx, 150 + 50 * np.sin(th(f)))
        cx2 = -60 + 600 * ((f / N + 0.65) % 1.0)
        s6.seed_cluster(air, casters, f, cx2, 330 + 40 * np.sin(th(f) + 2), ph=1.3)
        # a Needler riding the gale (recurring fighter) - its needles test bullet readability
        # against the dark sea, the scud and the flash
        nd = self.cast.unit("needler-a", anim=[-1.0, 0.0, 1.0, 0.0][(f // 4) % 4])
        npos = lambda ff: (380 + 50 * np.sin(th(ff) + 0.5), 110 + 18 * np.sin(th(ff, 2)))
        casters.append((nd,) + npos(f))
        sprite.paste_center(air, nd, *npos(f))
        s6.aimed(air, f, lambda ff: (npos(ff)[0], npos(ff)[1] + 14), self.cast, period=40,
                 speed=6.5, kind="thorn", phase=6)
        self.cast.player(air, casters, f)
        shadows = r02.shadow_layer(casters, *s6.SHADOW["air"], opacity=0.36)
        low = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        if f in self.FLASH and f < 55:
            s6.lightning(low, 300, -20, 330, 1250)
        scud = scroll_tex(self.scud, self.s.off("low-air", f))
        scud = Image.fromarray(np.roll(np.asarray(scud), int(round(W * f / N)), axis=1), "RGBA")
        if f in self.FLASH:                                  # scud lit from above by the flash
            a = np.asarray(scud).astype(np.float32)
            a[..., :3] += 120 * self.FLASH[f]
            scud = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")
        L = {"ground": ground, "shadows": shadows, "low-air": low, "air": air}
        L = r03.decorate(L, scud, "ground", casters, bank_shadow_opacity=0.3, flyer_opacity=0.3)
        L["high-air"] = self.rain_layer(f)
        return L


# =========================================================================== C: arctic (L13)

ICE9 = [(0.0, (70, 98, 122)), (0.35, (128, 156, 178)), (0.7, (176, 196, 212)),
        (1.0, (206, 220, 232))]           # bright, but clearly below the player's hull
ARCTIC_STOPS9 = [(0.0, (3, 10, 18)), (0.35, (7, 20, 32)), (0.65, (13, 32, 48)),
                 (0.9, (30, 56, 74)), (1.0, (64, 94, 112))]


def gauss_add(arr, x, y, r, amp):
    """Add a small Gaussian blob to a screen-space array (local window only)."""
    R = int(r * 2.5) + 1
    x0, x1 = max(0, int(x) - R), min(W, int(x) + R + 1)
    y0, y1 = max(0, int(y) - R), min(H, int(y) + R + 1)
    if x0 >= x1 or y0 >= y1:
        return
    d2 = (XX[y0:y1, x0:x1] - x) ** 2 + (YY[y0:y1, x0:x1] - y) ** 2
    arr[y0:y1, x0:x1] += amp * np.exp(-d2 / (r * r))


class ArcticScene(s8.ArcticScene):
    """Round 08 draft corrections: ice covered most of the screen and was brighter than the
    player's hull, the water was regular stripes, skimmers drove over the ice in screen space
    with dotted wakes and drawn ripple rings. Now two open leads run through the pack, the
    Skimmers travel along them in world space (their wakes stay in the water and flow away
    with it), the ice is toned down and the water calmer and darker."""

    notes = dict(s8.ArcticScene.notes, **{"ground": "X1.0 FLOES, LEADS, RELAY, SKIMMERS",
                                           "low-air": "X1.3 FOG BANKS (22 %)"})
    SHELF = (372, 214, 100)
    # (lead, world cycles per loop, world y at f=0, weave phase): k=1 keeps pace with the
    # player (weaving alongside), k=2 overtakes up the lead
    SKIMMERS = [(0, 1, 150, 0.0), (1, 2, 420, 1.3), (1, 1, 330, 2.4)]

    def lead_x(self, i, y):
        t = TAU * y / self.TRAVEL
        if i == 0:
            return 96 + 30 * np.sin(t) + 10 * np.sin(2 * t + 0.7)
        return 214 + 22 * np.sin(t + 1.0) + 8 * np.sin(3 * t)

    def __init__(self):
        super().__init__()
        self.sea.stops, self.sea.light_gain, self.sea.tones = ARCTIC_STOPS9, 2.4, 7
        self.sea_amp = 0.55                    # calm leads: the swell is damped by the pack
        per = self.TRAVEL
        self.churn = vstreak(per, 24, 1360, k=3)
        lp = self.s.period("low-air")
        pale = [(120, 134, 146), (146, 160, 170), (168, 180, 188), (186, 196, 202)]
        self.fog = s8.bank_tex(lp, [208, 104, 52], 1341, 0.6, 0.14, pale, max_alpha=150, stretch=2)

    def _ice(self, per):
        n = r02.periodic_fbm(W, per, 96, 1311, octaves=4)
        cx, cy, R = self.SHELF
        d = np.hypot(XX[:per] - cx, s6.periodic_dist(YY[:per], cy, per)) / R
        n = n + np.clip(1.2 - d, 0, 1) * 0.7
        yy = np.arange(per, dtype=np.float64)[:, None]
        for i, half in ((0, 34), (1, 30)):              # the open leads the Skimmers use
            lead = np.abs(XX[:per] - self.lead_x(i, yy)) < half + 8 * (n - 0.5)
            n = np.where(lead, 0.0, n)
        ice = n > 0.58
        ice = s6.pblur_mask(ice.astype(np.float64), 1.2) > 0.5
        self.mask = ice
        hgt = s6.pblur_mask(ice.astype(np.float64), 3.0)
        rid = r02.periodic_fbm(W, per, 24, 1312, octaves=2)
        hgt = hgt * (0.8 + 0.25 * rid) + np.where(np.abs(rid - 0.5) < 0.03, 0.15, 0) * ice
        shade = s6.hillshade(hgt * 30, 0.7)
        col = raster.ramp(ICE9, np.clip(0.25 + 0.55 * hgt * (0.7 + 0.3 * rid), 0, 1))
        col = col * shade[..., None]
        rim = ice & ~(np.roll(ice, 2, 0) & np.roll(ice, 2, 1) & np.roll(ice, -1, 0) & np.roll(ice, -1, 1))
        col[rim] = col[rim] * 0.8
        col[~ice] = ICE9[0][1]                 # no stray colours under the transparent water
        self.ice_tex = s6.smooth_tex(r02.posterize(raster.to_rgba_image(np.clip(col, 0, 255),
                                                                         ice * 255), 10))
        grow = s6.pblur_mask(ice.astype(np.float64), 6.0)
        margin = np.clip((grow - 0.05) * 3.0, 0, 1) * ~ice
        shade_w = np.roll(np.roll(s6.pblur_mask(ice.astype(np.float64), 2.0), 12, 0), 8, 1)
        under = np.zeros((per, W, 3))
        under[:] = np.array((52, 124, 142))
        self.under = r03.step_alpha(raster.to_rgba_image(under, margin * 160), 5)
        sh = np.zeros((per, W, 3))
        self.cast_sh = r03.step_alpha(raster.to_rgba_image(sh, shade_w * 120 * ~ice), 4)
        edge = ice & ~(np.roll(ice, 1, 0) & np.roll(ice, -1, 0) & np.roll(ice, 1, 1) & np.roll(ice, -1, 1))
        ys, xs = np.nonzero(edge)
        rng = np.random.default_rng(1313)
        keep = rng.random(len(xs)) < 0.45
        self.edge = np.stack([xs[keep], ys[keep]], 1).astype(np.float64)
        self.edge_ph = rng.uniform(0, TAU, len(self.edge))
        self.edge_k = rng.integers(1, 4, len(self.edge))
        self.station = self._station(per)

    def skimmer_world(self, i, ff):
        lead, k, y0, ph = self.SKIMMERS[i]
        u = ff / N
        yw = y0 - k * self.TRAVEL * u
        return self.lead_x(lead, yw % self.TRAVEL) + 13 * np.sin(TAU * 3 * u + ph), yw

    def skimmer_screen(self, i, ff):
        """Screen positions (all repeats) of skimmer i at frame ff."""
        x, yw = self.skimmer_world(i, ff)
        return [(x, y) for y in self.ys("ground", yw % self.TRAVEL, ff, margin=60)], yw

    def layer_images(self, f):
        off = self.s.off("ground", f)
        rows = self.sea.rows(off)
        h = self.sea.height(f, off, self.sea_amp)
        top = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        wash = np.zeros((H, W), np.float32)
        sks = []
        for i in range(len(self.SKIMMERS)):
            x, yw = self.skimmer_world(i, f)
            x2, yw2 = self.skimmer_world(i, f + 0.25)
            hd = float(np.arctan2(yw2 - yw, x2 - x))              # heading through the water
            for sx, sy in self.skimmer_screen(i, f)[0]:
                sks.append((sx, sy, hd, i))
                dist = 0.0
                px, pyw = x, yw
                for j in range(1, 34):                             # the path behind it, in
                    wx, wyw = self.skimmer_world(i, f - j * 0.5)   # the water (world space)
                    seg = np.hypot(wx - px, wyw - pyw) or 1e-3
                    tx, ty = (px - wx) / seg, (pyw - wyw) / seg
                    dist += seg
                    px, pyw = wx, wyw
                    wy = sy + (wyw - yw)
                    fade = (1 - j / 34) ** 1.2
                    arm = 0.36 * dist
                    for side in (-1, 1):                           # V-wake crests in the waves
                        gauss_add(h, wx - ty * arm * side, wy + tx * arm * side,
                                  1.8 + 0.035 * dist, 2.4 * fade)
                    gauss_add(wash, wx, wy, 1.6 + 0.04 * dist, 1.1 * fade ** 1.5)
        wash = np.clip(wash, 0, 1) * np.clip((self.churn[rows] - 0.22) * 2.5, 0, 1)
        ice = scroll_tex(self.ice_tex, off)
        under = scroll_tex(self.under, off)
        csh = scroll_tex(self.cast_sh, off)
        light = self.sea.light(h)
        body = self.sea.colour(light)
        body = s8.through_surface(body, under, self.sea.mean(), 1.0)
        body = apply_foam(body, np.clip((wash - 0.25) * 1.6, 0, 1), (200, 218, 226), levels=4)
        water = raster.to_rgba_image(np.clip(body, 0, 255))
        water.alpha_composite(csh)
        foam = s8.Foam()
        for x, y, hd, i in sks:
            sp = e6.skm.get(hd, phase=np.pi * ((f // 2 + i) % 2))
            c, s_ = np.cos(hd), np.sin(hd)
            for side in (-1, 1):                                   # bow spray
                s8.stream(foam, f, x + c * 8, y + s_ * 8, -c + side * 0.6 * -s_,
                          -s_ + side * 0.6 * c, 16, 8, 10, 0.5, 3, 0.7, 1.3, 0.8, 1370 + i * 2 + side)
            sprite.paste_center(top, sprite.shadow_of(sp, opacity=0.5, blur=1.0), x + 4, y + 6)
            sprite.paste_center(top, sp, x, y)
        edge_foam = s8.Foam()
        inv = np.full(self.TRAVEL, -1)
        inv[rows] = np.arange(H)
        ey = inv[self.edge[:, 1].astype(int)]
        vis = ey >= 0
        for (ex, _), yy, ph, k in zip(self.edge[vis], ey[vis], self.edge_ph[vis], self.edge_k[vis]):
            val = 0.5 + 0.5 * np.sin(th(f, k) + ph) * np.sin(th(f, 2) + ph * 1.3)
            if val > 0.35:
                edge_foam.dot(ex + 0.8 * np.sin(ph), yy + 0.8 * np.cos(ph), 0.8 + 0.7 * val, 0.75 * val)
        ground = water
        ground.alpha_composite(foam.done())
        ground.alpha_composite(top)
        ground.alpha_composite(ice)
        ground.alpha_composite(edge_foam.done())
        ground.alpha_composite(scroll_tex(self.station, off))
        t = th(f)
        sx, syw = self.SHELF[0] - 30 + 40 * np.cos(t), self.SHELF[1] - 56 + 22 * np.sin(t)
        hd = np.arctan2(22 * np.cos(t), -40 * np.sin(t))
        sp = s6.walk_sprite(hd, 3.4 * f)
        for yy in self.ys("ground", syw, f):
            sprite.paste_center(ground, sprite.shadow_of(sp, opacity=0.5, blur=1.0), sx + 8, yy + 11)
            sprite.paste_center(ground, sp, sx, yy)
        air = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        casters = []
        for k in range(len(self.SKIMMERS)):
            def src(ff, i=k):
                pts = self.skimmer_screen(i, ff)[0]
                pts = [p for p in pts if 0 <= p[1] <= H]
                return pts[0] if pts else None
            s6.aimed(air, f, src, self.cast, period=40, speed=6.0, kind="orb", phase=13 * k,
                     count=2, life=34)
        self.cast.player(air, casters, f)
        shadows = r02.shadow_layer(casters, 12, 17, opacity=0.4)
        L = {"ground": ground, "shadows": shadows, "low-air": Image.new("RGBA", (W, H), (0, 0, 0, 0)),
             "air": air}
        L = r03.decorate(L, scroll_tex(self.fog, self.s.off("low-air", f)), "ground", casters,
                         bank_shadow_opacity=0.22, flyer_opacity=0.28)
        L["high-air"] = self.snow_layer(f)
        return L


# =========================================================================== D: Geneva (L14)

class GenevaScene(s8.GenevaScene):
    """Round 08 draft corrections: the city was one courtyard block repeated on a grid. Now the
    old town has irregular block rows and widths, hip roofs, split buildings, courtyards and
    small squares; the lake gets a slow wave texture and the spore haze is thinner."""

    notes = dict(s8.GenevaScene.notes, **{"low-air": "X1.35 SPORE HAZE (22 %)"})

    def __init__(self):
        super().__init__()
        lp = self.s.period("low-air")
        haze = [(56, 40, 72), (78, 58, 96), (98, 80, 116), (116, 104, 134)]
        self.haze = s8.bank_tex(lp, [270, 135, 54, 27], 1441, 0.68, 0.12, haze, max_alpha=150)

    def _city(self, per):
        rng = np.random.default_rng(1405)
        yy, xx = np.mgrid[0:per, 0:W].astype(np.float64)
        shore = self.shore_x(yy)
        lake = xx < shore
        col = np.zeros((per, W, 3))
        ln = r02.periodic_fbm(W, per, 60, 1402, octaves=3)
        col[:] = np.array((26, 30, 48)) + ln[..., None] * np.array((10, 12, 18))
        hgt = np.zeros((per, W))
        rooftone = np.zeros((per, W))
        park = np.zeros((per, W), bool)
        rows_h = []                                   # block rows of varied depth that tile per
        while sum(rows_h) < per - 80:
            rows_h.append(int(rng.choice([42, 50, 58, 66])) + int(rng.choice([8, 10, 12])))
        rows_h.append(per - sum(rows_h))
        by = 0
        for rh in rows_h:
            street = 9 if rh < 60 else 11
            bh = rh - street
            bx = -int(rng.integers(10, 50))
            while bx < W:
                bw = int(rng.choice([34, 44, 52, 62, 76]))
                x0, x1, y0, y1 = bx, bx + bw, by, by + bh
                bx += bw + int(rng.choice([7, 9, 12]))
                cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
                if cx < self.shore_x(cy) + 24:
                    continue
                if np.hypot(cx - self.PLAZA[0], s6.periodic_dist(cy, self.PLAZA[1], per)) < 74:
                    continue
                xs0, xs1 = max(0, int(x0)), min(W, int(x1))
                if xs0 >= xs1:
                    continue
                kind = rng.random()
                if kind < 0.1:                          # park or square
                    park[y0:y1, xs0:xs1] = True
                    continue
                # split into one to three buildings along x (party walls)
                cuts = [x0, x1]
                if bw >= 52 and rng.random() < 0.6:
                    cuts = [x0, int(x0 + bw * rng.uniform(0.38, 0.62)), x1]
                for a0, a1 in zip(cuts[:-1], cuts[1:]):
                    s0, s1 = max(0, a0), min(W, a1)
                    if s0 >= s1:
                        continue
                    sx_ = xx[y0:y1, s0:s1]
                    sy_ = yy[y0:y1, s0:s1]
                    dx = np.minimum(sx_ - a0, a1 - 1 - sx_)
                    dy = np.minimum(sy_ - y0, y1 - 1 - sy_)
                    d = np.minimum(dx, dy)
                    tone = rng.uniform(0.17, 0.99)
                    if kind < 0.45 and (a1 - a0) > 36 and bh > 40:   # perimeter block
                        ring = 8.0
                        inner = d > 2 * ring
                        roof = np.where(inner, 0, np.clip(ring - np.abs(d - ring), 0, ring))
                        rooftone[y0:y1, s0:s1] = np.where(inner, -1, tone)
                    else:                                             # hip roof
                        roof = np.clip(d, 0, 40.0) * 0.7    # full hip roof with a ridge
                        rooftone[y0:y1, s0:s1] = tone
                    hgt[y0:y1, s0:s1] = roof * rng.uniform(0.8, 1.2)
            by += rh
        shade = s6.hillshade(hgt * 0.6, 0.8)
        roofs = rooftone > 0
        palette = [(72, 74, 86), (60, 86, 82), (110, 70, 60), (86, 80, 80)]   # slate, verdigris, tile, stone
        for i, c in enumerate(palette):
            sel = roofs & (rooftone >= 0.16 + i * 0.21) & (rooftone < 0.16 + (i + 1) * 0.21 + (i == 3))
            col[sel] = np.array(c) * shade[sel][..., None]
        court = rooftone == -1
        col[court] = (32, 34, 40)
        streets = ~lake & ~roofs & ~court & ~park
        col[streets] = (38, 38, 46)
        col[park & ~lake] = np.array((26, 40, 32)) + ln[park & ~lake][..., None] * np.array((6, 14, 8))
        wav = r03.stretched(W, per, [30, 15], 1406, 4)            # slow lake wave texture
        lk = np.array((22, 30, 52)) + ln[..., None] * np.array((12, 16, 26))
        lk = lk + (np.abs(wav - 0.5) < 0.06)[..., None] * np.array((22, 26, 36))
        col[lake] = lk[lake]
        prom = (~lake) & (xx < shore + 6)
        col[prom] = (92, 88, 96)
        lamps = (streets | prom) & (rng.random((per, W)) < 0.0035)   # sparse sodium lamps
        pool = s6.pblur_mask(lamps.astype(np.float64), 2.5)
        pool = pool / (pool.max() or 1)
        col = col + np.clip(pool * 2.2, 0, 1)[..., None] * np.array((70, 44, 14))
        col[lamps] = (230, 190, 120)
        d1, d2, idx = s6.voronoi_periodic(W, per, 46, 1403)
        vein = np.clip(1 - (d2 - d1) / 9.0, 0, 1)
        holes = (idx % 4 == 0)
        lightm = np.clip(1 - vein, 0, 1) * np.where(holes, 1.0, 0.45)
        lightm = s6.pblur_mask(lightm, 6.0)
        col = col * (0.5 + 0.75 * lightm[..., None]) + np.array((18, 8, 30)) * (1 - lightm[..., None])
        img = s6.smooth_tex(r02.posterize(raster.to_rgba_image(np.clip(col, 0, 255)), 20), 3)
        img = self._overgrowth(img, per)
        img = self._landmarks(img, per)
        return img


# =========================================================================== E: Luna far side (L06)

def coil_model_space():
    """Use the chosen round-08 model-space re-render of the Coilwyrm (as rerender_r08 does)."""
    from render import enemy_rigs as rig
    import enemies_r05 as e5
    for a in (e5.coil_head, e5.coil_tail, *e5.coil_segs.values()):
        if a.__class__ is not rig.ModelSpaceAngleSprites:
            a.__class__ = rig.ModelSpaceAngleSprites
            a.cache = {}
    return e5, rig


def feather(spr, px):
    """Fade a soft sprite's alpha out towards its box edges (removes a clipped blur edge)."""
    a = np.asarray(spr).astype(np.float32)
    hh, ww = a.shape[:2]
    yy, xx = np.mgrid[0:hh, 0:ww]
    d = np.minimum(np.minimum(xx, ww - 1 - xx), np.minimum(yy, hh - 1 - yy)).astype(np.float32)
    a[..., 3] *= np.clip(d / px, 0, 1)
    return Image.fromarray(a.astype(np.uint8), "RGBA")


class LunaFarsideScene(s8.LunaFarsideScene):
    """Round 08 draft corrections: the cast was the near-side scene's (Skitters, a Needler,
    Whirl Seeds) instead of L06's, the Coilwyrm was missing, the nest glow outshone the play
    plane and the headlight was barely visible. Now: Mantis holding at both sides, a Coilwyrm
    swirling between the crater rims (one figure-eight lap per loop), the dome turrets, a
    stronger headlight cone, dimmer nest tendrils."""

    title = "LUNA FAR SIDE - FLARES AND VRELL GLOW"
    notes = dict(s8.LunaFarsideScene.notes, **{"air": "MANTIS, COILWYRM, HEADLIGHT"})

    def __init__(self):
        super().__init__()
        a = np.asarray(self.ground_tex).astype(np.float32)
        glowing = (a[..., 1] > 150) & (a[..., 0] < 90)
        a[glowing, :3] *= 0.72                          # nest tendrils: glow, but below the cast
        self.ground_tex = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")
        self.e5, self.rig = coil_model_space()
        self.ejecta = [(feather(spr, 10), x, y) for spr, x, y in self.ejecta]   # r06 clipped them

    def coil_path(self, ff):
        t = TAU * ff / N
        return 250 + 120 * np.sin(t), 240 + 100 * np.sin(2 * t + 0.4)

    def draw_coil(self, img, casters, f):
        e5 = self.e5
        sizes = e5.COIL_SIZES
        spac = [0.5 * (sizes[i] + sizes[i + 1]) / 2 for i in range(len(sizes) - 1)]
        pts, heads = self.rig.chain_at(self.coil_path, float(f), spac, dt=0.05, horizon=60.0)
        for i in range(len(pts))[::-1]:
            x, y = pts[i]
            if i == 0:
                sp = e5.coil_head.get(heads[0], jaw=round(0.5 + 0.5 * np.sin(th(f, 6)), 1))
            elif i == len(sizes) - 1:
                sp = e5.coil_tail.get(heads[i])
            else:
                sp = e5.coil_segs[sizes[i]].get(heads[i])
            casters.append((sp, x, y))
            sprite.paste_center(img, sp, x, y)
        return pts

    def layer_images(self, f):
        ground = self.ground_image(f)
        px, py, _ = self.cast.player_pos(f)
        cone = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        d = ImageDraw.Draw(cone)
        d.polygon([(px - 8, py - 18), (px + 8, py - 18), (px + 80, py - 250), (px - 80, py - 250)],
                  fill=(190, 210, 255, 34))
        d.polygon([(px - 5, py - 18), (px + 5, py - 18), (px + 40, py - 200), (px - 40, py - 200)],
                  fill=(210, 225, 255, 30))
        ground.alpha_composite(cone.filter(ImageFilter.GaussianBlur(9)))
        air = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        casters = []
        mantis = self.cast.unit("mantis-a")
        for k, (mx0, ph) in enumerate(((40, 1.0), (440, 3.2))):
            mx, my = mx0 + 6 * np.sin(th(f) + ph), 230 + 70 * np.sin(th(f) + ph)
            casters.append((mantis, mx, my))
            sprite.paste_center(air, mantis, mx, my)
            s6.aimed(air, f, lambda ff, x0=mx0, p=ph: (x0 + 6 * np.sin(th(ff) + p),
                                                         230 + 70 * np.sin(th(ff) + p)),
                     self.cast, period=40, speed=6.0, kind="thorn", phase=10 + 20 * k)
        self.draw_coil(air, casters, f)
        s6.aimed(air, f, self.turret_src(0), self.cast, period=40, speed=6.0, kind="orb", phase=0)
        s6.aimed(air, f, self.turret_src(1), self.cast, period=40, speed=6.0, kind="orb", phase=20)
        self.cast.player(air, casters, f)
        shadows = r02.shadow_layer(casters, *s6.SHADOW["air"], opacity=0.5)
        low = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        for k in range(2):                              # falling flares (low-air)
            u = (f / N + k * 0.5) % 1.0
            x, y = 120 + 260 * k + 30 * np.sin(TAU * u), -40 + 620 * u
            fl = Image.new("RGBA", (W, H), (0, 0, 0, 0))
            dd = ImageDraw.Draw(fl)
            dd.line([x, y - 40, x, y], fill=(255, 220, 160, 60), width=2)
            dd.ellipse([x - 3, y - 3, x + 3, y + 3], fill=(255, 240, 200, 255))
            low.alpha_composite(raster.glow(fl, 4.0, 1.2))
        L = {"ground": ground, "shadows": shadows, "low-air": low, "air": air}
        plumes = scroll_tex(self.plumes, self.s.off("low-air", f))
        L = r03.decorate(L, plumes, "ground", casters, bank_shadow_opacity=0.32, flyer_opacity=0.35)
        high = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        for spr, x, y in self.ejecta:
            for yy in self.ys("high-air", y, f, margin=80):
                sprite.paste_center(high, spr, x, yy)
        L["high-air"] = high
        return L

    def key_list(self):
        return [self.cast.unit("mantis-a"), self.e5.coil_head.get(np.pi / 2, jaw=0.5),
                self.e5.coil_segs[44].get(np.pi / 2)]


# =========================================================================== sheet + main

SCENES = {"ocean": OceanScene, "storm": StormScene, "arctic": ArcticScene,
          "geneva": GenevaScene, "luna-farside": LunaFarsideScene}

NOTES = dict(s8.NOTES)
NOTES["ocean"] = ("OPEN OCEAN (L11): THE CONVOY STEAMS AT THE SCROLL SPEED AND HOLDS STATION WHILE THE "
                  "SEA STREAMS PAST. ROUND 09: KELVIN WAKES ARE BROKEN UP BY NOISE THAT FLOWS WITH THE "
                  "WATER, STERN CHURN IS FOAM IN THE WATER (NO DOTTED LINES), A SLOW COLOUR FIELD AND "
                  "SPARSE WHITECAPS GIVE THE SEA DEPTH, MIST ABOUT 22 % AND THIN WISPS (MEDIUM).")
NOTES["storm"] = ("STORM (L12): A HEAVY PEAK - DARK HEAVING SEA, ONLY THE STEEPEST CRESTS BREAK INTO TORN "
                  "WHITECAPS, THIN WIND-STREAK LINES, THE SWELL STREAMING PAST THE PLATFORM LEGS AS "
                  "CHURNED FOAM, DARK SCUD (ABOUT 34 %) RACING SIDEWAYS ONE SCREEN WIDTH PER LOOP, RAIN AND A "
                  "LIGHTNING STRIKE THAT LIGHTS SEA AND SCUD. LAMPREYS STREAM IN; WHIRL SEEDS RIDE THE WIND; "
                  "A NEEDLER'S YELLOW NEEDLES CHECK BULLET READABILITY.")
NOTES["arctic"] = ("ARCTIC (L13): FLOES ON BLACK WATER WITH TWO OPEN LEADS; SUBMERGED ICE SHOWS TEAL, BROKEN "
                   "FOAM AT EVERY WATERLINE, LONG COOL SHADOWS. SKIMMERS TRAVEL THE LEADS IN WORLD SPACE: THEIR "
                   "V-WAKES ARE CRESTS IN THE WAVE FIELD AND STAY IN THE WATER. ICE TONED BELOW THE PLAYER'S "
                   "HULL; A SCUTTLER STRIDES OVER THE RELAY'S ICE SHELF; FOG ABOUT 22 %.")
NOTES["geneva"] = ("GENEVA (L14): THE OLD CITY BY THE LAKE UNDER THE VRELL CANOPY - IRREGULAR BLOCKS WITH HIP "
                   "ROOFS AND COURTYARDS, SPARSE SODIUM LAMPS, MOTTLED CANOPY SHADOW WITH LIGHT POOLS, THE "
                   "SPIRE'S ROOTS AND CREEP, THE CONCORD ROTUNDA, THE JET D'EAU; CANOPY ON HIGH-AIR (<40 %).")
NOTES["luna-farside"] = ("FAR SIDE (L06): NO SUNLIGHT - DOME LIGHTS, VRELL GLOW, FALLING FLARES AND THE SHIP'S "
                         "HEADLIGHT LIGHT THE REGOLITH. MANTIS HOLD AT BOTH SIDES; A COILWYRM SWIRLS BETWEEN "
                         "THE CRATER RIMS (ONE FIGURE-EIGHT LAP PER LOOP); DOME TURRETS FIRE.")


def make_sheet(scene, f=24):
    frame_img = scene.compose(f)
    layers = scene.layer_images(f)
    tw, th_ = 192, 216
    img = raster.sheet(1140, 690, f"SCENE {scene.slug.upper()} (R09): {scene.title}",
                       f"CONCEPT ROUND 09 - {scene.intensity}")
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
        png = OUT / f"scene-{slug}-r09-a.png"
        make_sheet(scene).convert("RGB").save(png, optimize=True)
        print("wrote", png.relative_to(ROOT))
        if "--sheet" in sys.argv:
            continue
        gif = OUT / f"scene-{slug}-r09-a.gif"
        cols = s6.make_gif(scene, gif, scene.GIF_COLORS)
        print("wrote", gif.relative_to(ROOT), f"{gif.stat().st_size / 1e6:.1f} MB, {cols} colours")


if __name__ == "__main__":
    main()
