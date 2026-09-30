#!/usr/bin/env python3
"""Concept round 02 - parallax scenes at 960x540 (play field 480x540), palette B, ship A.

Outputs (design/art-direction/concept/):
  parallax-r02-a.png / .gif   Earth orbit, fuller and faster (6 layers)
  parallax-r02-b.png / .gif   Night megacity, calm (4 layers)
  parallax-r02-c.png / .gif   Mars canyon, the balanced density (5 layers + canyon walls)

Each PNG shows the play field at 1x plus a labelled layer breakdown; each GIF is a seamless
4 s loop (80 frames at 20 fps) at native size. Scroll factors are relative to the ground layer;
every scene sets its own ground speed (see SCENES / design/art-direction):
  deep 0.12 | far 0.45-0.6 | ground 1.0 | low-air 1.35-1.4 | air = play plane | high-air 2.0-2.4
Layers whose travel per loop is shorter than the screen cross-fade over the last second;
all others repeat after exactly one loop, so the GIFs loop without a jump.
Run: python3 tools/concept/parallax_r02.py [a] [b] [c]
"""
import subprocess
import sys
import tempfile
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, str(Path(__file__).resolve().parent))
from render import models, raster, sdf, sprite, station, terrain  # noqa: E402
from render.config import FIELD_H, FIELD_W, ROOT, SHIP_SIZE  # noqa: E402
from render.palette import B  # noqa: E402

OUT = ROOT / "design" / "art-direction" / "concept"
W, H = FIELD_W, FIELD_H
N, FPS = 80, 20
FADE = 20                       # frames of cross-fade for short-travel layers
SHADOW = {"low-air": (9, 13), "air": (21, 30)}   # shadow offsets (light from the top-left)


def rgb(name, i):
    return B[name][i]


# --------------------------------------------------------------------------- layer helpers

class Scroller:
    """Offsets for one scene: world travel per loop and per-layer factors."""

    def __init__(self, travel, factors):
        self.travel = travel
        self.factors = factors

    def off(self, layer, f):
        return self.factors[layer] * self.travel * f / N

    def period(self, layer):
        return int(round(self.factors[layer] * self.travel))

    def ys(self, layer, y0, f, margin=80):
        """Screen positions of an object placed at y0 on a periodic layer (all repeats)."""
        per = self.factors[layer] * self.travel
        y = (y0 + self.off(layer, f)) % per
        out = []
        k = -int(np.ceil((margin + H) / per)) - 1
        while y + k * per < H + margin:
            if y + k * per > -margin:
                out.append(y + k * per)
            k += 1
        return out


def crossfade(tex, shift, f):
    """Window into a non-periodic texture that loops by cross-fading in the last second.
    ``tex`` must be at least H + 2 * shift tall; content moves down the screen."""
    arr = np.asarray(tex, dtype=np.float64)

    def window(o):
        s = int(round(shift - o))
        return arr[s:s + H]
    o = shift * f / N
    w = sdf.smoothstep(N - FADE, N, f)
    out = window(o) * (1 - w) + window(o - shift) * w
    return Image.fromarray(out.astype(np.uint8), "RGBA")


def scroll_tex(tex, off):
    """Window into a texture that is periodic in y (period = its height)."""
    arr = np.asarray(tex)
    start = int(round(-off)) % arr.shape[0]
    rows = (np.arange(H) + start) % arr.shape[0]
    return Image.fromarray(arr[rows], "RGBA")


def haze(img, color, amount):
    """Blend an RGBA layer towards a haze colour (depth cue), keeping alpha."""
    a = np.array(img).astype(np.float64)
    a[..., :3] = a[..., :3] * (1 - amount) + np.array(color) * amount
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


def alpha_scale(img, s):
    img = img.copy()
    img.putalpha(img.getchannel("A").point(lambda v: int(v * s)))
    return img


def masked(img, mask_img):
    """Keep ``img`` only where ``mask_img`` is opaque (shadows only land on structures)."""
    a = np.array(img)
    m = np.array(mask_img.getchannel("A")).astype(np.float64) / 255
    a[..., 3] = (a[..., 3] * m).astype(np.uint8)
    return Image.fromarray(a, "RGBA")


def posterize(img, colors):
    """Limit a background texture to a few colours (no dithering), keeping alpha. 90s tile
    art was palette-limited too, and flat runs keep the GIF loops small."""
    rgb_img = img.convert("RGB").quantize(colors=colors, method=Image.Quantize.MEDIANCUT,
                                          dither=Image.Dither.NONE).convert("RGBA")
    rgb_img.putalpha(img.getchannel("A"))
    return rgb_img


def render_sprite(model, size, extent, colors=28, factor=8):
    scene, mats = model
    hi = sdf.render(scene, mats, (size * factor, size * factor), extent)
    return sprite.make_sprite(hi, factor, colors)


def periodic_fbm(w, per, cell, seed, octaves=4):
    """fBm that tiles vertically with period ``per`` (cell must divide per per octave)."""
    return raster.fbm(w, per, cell, seed, octaves=octaves, period=True)


# --------------------------------------------------------------------------- actors (air layer)

class Actors:
    """Screen-space play plane: player (ship A, palette B), Vrell, bullets. Moderate counts."""

    def __init__(self, darts, brood=False):
        fn = models.PLAYER_SHIPS["a"][1]
        pal = B.ship_colors()
        bank = np.radians(24)
        self.player = {k: render_sprite(fn(bank=a, palette=pal), SHIP_SIZE, 2.3)
                       for k, a in (("l", -bank), ("c", 0.0), ("r", bank))}
        vp = B.vrell_colors()
        self.dart = render_sprite(models.vrell_dart_model(palette=vp), 40, 2.3)
        self.brood = render_sprite(models.vrell_brood_model(palette=vp), 80, 2.6, colors=36) \
            if brood else None
        self.darts = darts
        es = B["ENEMY SHOTS"]
        self.orb = self._orb(es[0], es[2], es[4])
        self.needle = self._needle(es[0], es[5], es[4])
        ps = B["PLAYER SHOTS"]
        self.bolt = self._bolt(ps[2], ps[4])
        self.flare = self._flare(ps[2])

    @staticmethod
    def _orb(rim, body, core, size=11):
        img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        d.ellipse([0, 0, size - 1, size - 1], fill=rim + (255,))
        d.ellipse([1, 1, size - 2, size - 2], fill=body + (255,))
        d.ellipse([4, 4, size - 5, size - 5], fill=core + (255,))
        return img

    @staticmethod
    def _needle(rim, body, core):
        img = Image.new("RGBA", (7, 15), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        d.ellipse([0, 0, 6, 14], fill=rim + (255,))
        d.ellipse([1, 1, 5, 13], fill=body + (255,))
        d.line([3, 3, 3, 11], fill=core + (255,))
        return img

    @staticmethod
    def _bolt(body, core):
        img = Image.new("RGBA", (5, 16), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        d.rectangle([0, 0, 4, 15], fill=body + (190,))
        d.rectangle([1, 1, 3, 14], fill=core + (255,))
        return img

    @staticmethod
    def _flare(color):
        yy, xx = np.mgrid[0:17, 0:17]
        d = np.sqrt((xx - 8) ** 2 + ((yy - 8) * 0.7) ** 2) / 8
        a = np.clip(1 - d, 0, 1) ** 1.6
        c = np.zeros((17, 17, 3))
        c[:] = color
        c[d < 0.35] = (255, 255, 255)
        return raster.to_rgba_image(c, a * 230)

    def player_pos(self, f):
        t = 2 * np.pi * f / N
        return W / 2 + 60 * np.sin(t), 450 + 8 * np.sin(2 * t), np.cos(t)

    def draw(self, f, aimed_from=()):
        """Air layer image, shadow casters, and extra bullets aimed from ground positions."""
        img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        casters = []
        px, py, vx = self.player_pos(f)
        if self.brood is not None:
            bx, by = W / 2 + 70 * np.sin(2 * np.pi * f / N + 1.0), 100
            for k in range(2):                       # two rings of 8 on screen at most
                age = (f - k * 40) % N
                r = 30 + age * 4.2
                for j in range(8):
                    a = 2 * np.pi * j / 8 + k * 0.39
                    sprite.paste_center(img, self.orb, bx + r * np.cos(a), by + r * np.sin(a))
            casters.append((self.brood, bx, by))
        for spec in self.darts:
            x, y = spec(f)
            casters.append((self.dart, x, y))
        for gx, gy, phase in aimed_from:             # ground turrets: slow aimed needles
            for k in range(2):
                age = (f + phase - k * 40) % N
                if age > 36 or not 0 <= gy(f - age) <= H:
                    continue
                tx, ty, _ = self.player_pos(f - age)
                dx, dy = tx - gx(f - age), ty - gy(f - age)
                n = np.hypot(dx, dy) or 1
                sprite.paste_center(img, self.needle, gx(f - age) + dx / n * age * 7,
                                    gy(f - age) + dy / n * age * 7)
        for k in range(8):                           # the player's twin bolts
            age = ((16 * f + 60 * k) % 480) / 16.0
            ox, _, _ = self.player_pos(f - age)
            y = py - 20 - age * 16
            sprite.paste_center(img, self.bolt, ox - 9, y)
            sprite.paste_center(img, self.bolt, ox + 9, y)
        frame = "r" if vx > 0.5 else "l" if vx < -0.5 else "c"
        casters.append((self.player[frame], px, py))
        for sp, x, y in casters:
            sprite.paste_center(img, sp, x, y)
        sprite.paste_center(img, self.flare, px, py + 24 + (f % 3 == 0))
        return img, casters


def shadow_layer(casters, dx, dy, opacity=0.5, blur=1.5):
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    for sp, x, y in casters:
        sprite.paste_center(img, sprite.shadow_of(sp, opacity=opacity, blur=blur, scale=0.85),
                            x + dx, y + dy)
    return img


def dive(x0, amp, phase, delay=0.0, span=720):
    """A dart diving down the screen once per loop with a sine weave."""
    def pos(f):
        t = ((f / N) + delay) % 1.0
        return x0 + amp * np.sin(2 * np.pi * (2 * t) + phase), -60 + span * t
    return pos


# --------------------------------------------------------------------------- scene A: orbit

class OrbitScene:
    key = "a"
    title = "EARTH ORBIT - FULLER, FASTER"
    layers = ["deep", "far", "ground", "low-air", "air", "high-air"]
    notes = {"deep": "X0.12 EARTH + X0.2 HAZE", "far": "X0.45 SISTER STATION",
             "ground": "X1.0 STATION, VRELL PODS", "low-air": "X1.4 DRIFTING WRECKAGE",
             "air": "PLAY PLANE", "high-air": "X2.4 ICE AND DEBRIS STREAKS"}
    TRAVEL = 760                 # ground px per 4 s loop = 190 px/s
    FACTORS = {"deep": 0.12, "haze": 0.2, "far": 0.45, "ground": 1.0, "low-air": 1.4,
               "high-air": 2.4}

    def __init__(self):
        self.s = Scroller(self.TRAVEL, self.FACTORS)
        self.actors = Actors([dive(150, 40, 0.0), dive(330, 40, 1.5, 0.12),
                              dive(240, 60, 3.0, 0.5), dive(400, 30, 0.7, 0.62)], brood=True)
        sea, land = B["EARTH ORBIT"], B["EARTH SURFACE"]
        self.earth_shift = self.FACTORS["deep"] * self.TRAVEL
        earth = terrain.earth_coast(W, int(H + 2 * self.earth_shift) + 4, seed=11, period=False,
                                    stops=terrain.coast_stops_from(sea, land, 0.56), cell=128)
        clouds = raster.fbm(W, earth.height, 64, 61, octaves=5, period=False)
        e = np.array(earth).astype(np.float64)
        c = np.clip((clouds - 0.55) * 2.6, 0, 0.85)[..., None]
        e[..., :3] = e[..., :3] * (1 - c) + np.array(sea[5]) * c
        grey = e[..., :3].mean(axis=-1, keepdims=True)
        e[..., :3] = (e[..., :3] * 0.6 + grey * 0.4) * 0.5 + np.array(sea[1]) * 0.5  # far: hazy
        self.earth = posterize(Image.fromarray(e.astype(np.uint8), "RGBA"), 20)
        self.haze_shift = self.FACTORS["haze"] * self.TRAVEL
        hz = raster.fbm(W, int(H + 2 * self.haze_shift) + 4, 96, 71, octaves=4, period=False)
        streak = np.clip((hz - 0.5) * 2.2, 0, 1) ** 1.5
        self.haze_tex = raster.to_rgba_image(np.broadcast_to(np.array(sea[4], float),
                                                             streak.shape + (3,)), streak * 70)
        self.parts = self._parts()
        self.ground, self.ground_high = self._station_strip(self.TRAVEL, main=True)
        far, far_high = self._station_strip(self.TRAVEL, main=False)
        far.alpha_composite(far_high)
        per = self.s.period("far")
        far = far.resize((int(W * 0.45), per), Image.BOX)
        far = posterize(haze(far, sea[2], 0.55), 24)
        self.far = Image.new("RGBA", (W, per), (0, 0, 0, 0))
        self.far.alpha_composite(far, (int(W * 0.5 - far.width / 2) + 10, 0))
        self.pods = self._pods()
        self.wreck = [render_sprite(models.debris_model(a, 0.5 + 0.3 * np.sin(a)), 44, 1.4,
                                    colors=20) for a in np.linspace(0, 2 * np.pi, 8,
                                                                    endpoint=False)]
        self.wreck = [haze(w, sea[1], 0.35) for w in self.wreck]
        rng = np.random.default_rng(3)
        self.wreck_pos = [(rng.uniform(20, W - 20), rng.uniform(0, self.s.period("low-air")),
                           int(rng.integers(0, 8))) for _ in range(8)]
        self.streaks = [(rng.uniform(0, W), rng.uniform(0, self.s.period("high-air")),
                         rng.uniform(0.3, 1.0)) for _ in range(44)]
        self.big_debris = [(rng.uniform(40, W - 40), rng.uniform(0, self.s.period("high-air")),
                            int(rng.integers(0, 8))) for _ in range(2)]

    def _parts(self):
        p = {}
        p["truss_v"] = station.render_part(station.truss_v_part(), B)
        p["truss_h200"] = station.render_part(station.truss_h_part(200), B)
        p["truss_h110"] = station.render_part(station.truss_h_part(110), B)
        p["drum"] = station.render_part(station.drum_part(), B)
        p["drum_s"] = station.render_part(station.drum_part(16, 30), B)
        p["solar_l"] = station.render_part(station.solar_part(140, 56, True), B)
        p["solar_r"] = station.render_part(station.solar_part(140, 56, False), B)
        p["radiator"] = station.render_part(station.radiator_part(), B)
        p["dock"] = station.render_part(station.dock_part(), B)
        p["dish"] = station.render_part(station.dish_part(), B)
        p["turret"] = station.render_part(station.turret_part(), B)
        p["cargo_a"] = station.render_part(station.cargo_part(), B)
        p["cargo_b"] = station.render_part(station.cargo_part(station.SOLAR), B)
        return p

    def _station_strip(self, per, main=True):
        """Kit-bash the station into a strip that tiles with period ``per`` (low / high)."""
        low = Image.new("RGBA", (W, per), (0, 0, 0, 0))
        high = Image.new("RGBA", (W, per), (0, 0, 0, 0))
        p = self.parts

        def put(layer, spr, x, y):
            for yy in (y - per, y, y + per):
                sprite.paste_center(layer, spr, x, yy)
        tv = p["truss_v"]
        spines = (150, 350) if main else (240,)
        for x in spines:
            for y in range(0, per, tv.height):
                put(low, tv, x, y + tv.height / 2)
        if main:
            for y in (100, 360, 610):
                put(low, p["truss_h200"], 250, y)
            put(low, p["truss_h110"], 85, 250)
            put(low, p["truss_h110"], 415, 500)
            put(low, p["solar_l"], 150, 300)          # sprite centre = mast root
            put(low, p["solar_l"], 150, 690)
            put(low, p["solar_r"], 350, 160)
            put(low, p["solar_r"], 350, 560)
            put(low, p["radiator"], 55, 40)
            put(low, p["radiator"], 420, 380)
            put(high, p["drum"], 150, 180)
            put(high, p["drum"], 350, 440)
            put(high, p["dock"], 150, 470)
            put(high, p["drum_s"], 350, 700)
            put(high, p["dish"], 350, 290)
            put(high, p["turret"], 210, 100)
            put(high, p["turret"], 300, 610)
            for i, y in enumerate((20, 48, 76)):
                put(high, p["cargo_a" if i % 2 else "cargo_b"], 322, y)
        else:
            for y in (140, 520):
                put(low, p["solar_l"], 240, y)
                put(low, p["solar_r"], 240, y + 190)
            put(high, p["drum"], 240, 330)
            put(high, p["dock"], 240, 700)
        return low, high

    def _pods(self):
        pod = render_sprite(models.vrell_turret_model(palette=B.vrell_colors()), 28, 1.6)
        return pod, [(150, 150), (380, 150), (150, 520), (350, 400)]

    def ground_image(self, f):
        off = self.s.off("ground", f)
        low = scroll_tex(self.ground, off)
        high = scroll_tex(self.ground_high, off)
        sh = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        sh.alpha_composite(sprite.shadow_of(high, opacity=0.45, blur=1.2, scale=1.0), (6, 8))
        img = low.copy()
        img.alpha_composite(masked(sh, low))
        img.alpha_composite(high)
        pod, spots = self.pods
        for x, y in spots:
            for yy in self.s.ys("ground", y, f):
                sprite.paste_center(img, pod, x, yy)
        return img

    def turret_sources(self):
        pod, spots = self.pods
        out = []
        for i, (x, y) in enumerate(spots[:2]):
            def gy(ff, y=y):
                return (y + self.s.off("ground", ff)) % self.TRAVEL
            out.append((lambda ff, x=x: x, gy, i * 20))
        return out

    def layer_images(self, f):
        deep = crossfade(self.earth, self.earth_shift, f)
        deep.alpha_composite(crossfade(self.haze_tex, self.haze_shift, f))
        far = scroll_tex(self.far, self.s.off("far", f))
        ground = self.ground_image(f)
        low = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        low_casters = []
        for x, y, k in self.wreck_pos:
            for yy in self.s.ys("low-air", y, f):
                spr = self.wreck[(k + f // 5) % 8]
                sprite.paste_center(low, spr, x, yy)
                low_casters.append((spr, x, yy))
        air, casters = self.actors.draw(f, self.turret_sources())
        shadows = shadow_layer(low_casters, *SHADOW["low-air"], opacity=0.45)
        shadows.alpha_composite(shadow_layer(casters, *SHADOW["air"]))
        shadows = masked(shadows, ground)
        high = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        d = ImageDraw.Draw(high)
        step = self.FACTORS["high-air"] * self.TRAVEL / N
        c = rgb("EARTH ORBIT", 5)
        for x, y, b in self.streaks:
            for yy in self.s.ys("high-air", y, f, margin=40):
                d.line([x, yy - step * (0.6 + b), x, yy], fill=c + (int(70 + 110 * b),),
                       width=1 + (b > 0.75))
        for x, y, k in self.big_debris:
            for yy in self.s.ys("high-air", y, f, margin=120):
                spr = self.wreck[k].resize((110, 110), Image.BILINEAR)
                spr = alpha_scale(spr.filter(ImageFilter.GaussianBlur(2.5)), 0.38)
                sprite.paste_center(high, spr, x, yy)
        return {"deep": deep, "far": far, "ground": ground, "shadows": shadows, "low-air": low,
                "air": air, "high-air": high}

    def compose(self, f):
        L = self.layer_images(f)
        img = L["deep"].copy()
        for k in ("far", "ground", "shadows", "low-air", "air", "high-air"):
            img.alpha_composite(L[k])
        return img


# --------------------------------------------------------------------------- scene B: city

class CityScene:
    key = "b"
    title = "NIGHT MEGACITY - CALM"
    layers = ["ground", "low-air", "air", "high-air"]
    notes = {"ground": "X1.0 STREETS, TOWERS, TURRET", "low-air": "X1.35 AVENUE TRAFFIC",
             "air": "PLAY PLANE", "high-air": "X2.0 THIN MIST"}
    TRAVEL = 560                 # 140 px/s: calmer than orbit
    FACTORS = {"ground": 1.0, "low-air": 1.35, "high-air": 2.0}
    BLOCK = 80
    AVENUES = (150, 330)         # wide north-south avenues = readable lanes
    AVE_W, STREET_W = 26, 8
    CAM = 6.0
    SS = 2

    def __init__(self):
        self.s = Scroller(self.TRAVEL, self.FACTORS)
        self.actors = Actors([dive(240, 70, 0.0), dive(240, 70, 0.0, 0.07),
                              dive(240, 70, 0.0, 0.14)])
        self.pal = B["UI PANELS"]
        self.street = self._street_tex()
        self.buildings = self._buildings()
        self.turret = render_sprite(models.vrell_turret_model(palette=B.vrell_colors()), 28, 1.6)
        mist = periodic_fbm(W, self.s.period("high-air"), 56, 41)
        a = np.clip((mist - 0.56) * 2.5, 0, 1) ** 1.5 * 80
        self.mist = raster.to_rgba_image(np.broadcast_to(np.array(self.pal[3], float),
                                                         a.shape + (3,)), a)
        rng = np.random.default_rng(8)
        self.cars = [(self.AVENUES[i % 2] + (-6 if i % 3 else 6), rng.uniform(0, 756),
                      1 if i % 2 else -1) for i in range(4)]

    def _is_avenue(self, x):
        return any(abs(x - a) < self.AVE_W / 2 for a in self.AVENUES)

    def _street_tex(self):
        per = self.TRAVEL
        base = periodic_fbm(W, per, 56, 5, octaves=3)
        arr = np.zeros((per, W, 3))
        arr[:] = self.pal[0]
        arr *= (0.85 + 0.3 * base)[..., None]
        yy, xx = np.mgrid[0:per, 0:W]
        streets = (yy % self.BLOCK) < self.STREET_W
        for a in self.AVENUES:
            streets |= np.abs(xx - a) < self.AVE_W / 2
        arr[streets] = self.pal[1]
        for a in self.AVENUES:                        # dashed lane markings
            lane = (np.abs(xx - a) < 1) & ((yy // 10) % 2 == 0)
            arr[lane] = np.array(self.pal[3]) * 0.8
        light = np.zeros((per, W))                   # sodium lamps along the avenues only
        for a in self.AVENUES:
            for side in (-1, 1):
                lx = a + side * (self.AVE_W / 2 + 1)
                for ly in range(0, per, 40):
                    d2 = (xx - lx) ** 2 + np.minimum.reduce([(yy - ly) ** 2, (yy - ly - per) ** 2,
                                                             (yy - ly + per) ** 2])
                    light += np.exp(-d2 / 40.0)
        arr += light[..., None] * np.array(rgb("MARS", 4)) * 0.55
        return posterize(raster.to_rgba_image(arr), 24)

    def _buildings(self):
        rng = np.random.default_rng(21)
        b, s = self.BLOCK, self.STREET_W
        edges = [0] + [x for a in self.AVENUES for x in (a - self.AVE_W // 2, a + self.AVE_W // 2)] \
            + [W]
        out = []
        for by in range(0, self.TRAVEL, b):
            for i in range(0, len(edges), 2):
                x0, x1 = edges[i] + 6, edges[i + 1] - 6
                # split the block between avenues into two or three lots
                cuts = sorted(rng.uniform(x0 + 30, x1 - 30, size=int(rng.integers(1, 3))))
                xs = [x0] + list(cuts) + [x1]
                for j in range(len(xs) - 1):
                    if xs[j + 1] - xs[j] < 20:
                        continue
                    park = rng.random() < 0.15        # small parks break up the blocks calmly
                    h = 0.0 if park else rng.uniform(0.25, 1.0) ** 1.4 * 1.35
                    out.append({"x0": xs[j] + 3, "x1": xs[j + 1] - 3, "y0": by + s + 4,
                                "y1": by + b - 4, "h": h, "seed": int(rng.integers(1 << 30)),
                                "turret": False, "beacon": h > 1.15, "park": park})
        tall = sorted(range(len(out)), key=lambda i: -out[i]["h"])
        out[tall[3]]["turret"] = True
        return sorted(out, key=lambda o: o["h"])

    def _project(self, x, y, h):
        cx, cy = W / 2, H * 0.55
        k = self.CAM / (self.CAM - h)
        return cx + (x - cx) * k, cy + (y - cy) * k

    def ground_image(self, f, turrets_out=None):
        off = self.s.off("ground", f)
        per = self.TRAVEL
        ss = self.SS
        base = scroll_tex(self.street, off).resize((W * ss, H * ss), Image.NEAREST)
        d = ImageDraw.Draw(base)
        turrets = []
        for bld in self.buildings:
            for rep in (-per, 0):
                dy = off % per + rep
                y0, y1 = bld["y0"] + dy, bld["y1"] + dy
                if y1 < -80 or y0 > H + 80:
                    continue
                self._draw_building(d, bld, bld["x0"], y0, bld["x1"], y1, ss, turrets)
        img = base.resize((W, H), Image.BOX)
        for x, y in turrets:
            sprite.paste_center(img, self.turret, x, y)
        if turrets_out is not None:
            turrets_out.extend(turrets)
        return img

    def _draw_building(self, d, bld, x0, y0, x1, y1, ss, turrets):
        if bld["park"]:
            g = B["EARTH SURFACE"]
            d.rectangle([x0 * ss, y0 * ss, x1 * ss, y1 * ss], fill=tuple(c // 2 for c in g[1]))
            rng = np.random.default_rng(bld["seed"])
            for _ in range(int((x1 - x0) * (y1 - y0) / 90)):
                tx, ty = rng.uniform(x0 + 2, x1 - 2), rng.uniform(y0 + 2, y1 - 2)
                d.ellipse([(tx - 2) * ss, (ty - 2) * ss, (tx + 2) * ss, (ty + 2) * ss],
                          fill=tuple(int(c * 0.55) for c in g[2]))
            return
        h = bld["h"]
        corners = [(x0, y0), (x1, y0), (x1, y1), (x0, y1)]
        roof = [self._project(x, y, h) for x, y in corners]
        p = self.pal
        lit_wall = tuple(int(c * 1.25) for c in p[1])        # roof > lit walls > shaded walls
        dark_wall = tuple(int(c * 0.6) for c in p[1])
        wall = {"top": lit_wall, "left": lit_wall, "right": dark_wall, "bottom": dark_wall}
        rng = np.random.default_rng(bld["seed"])
        for name, i, j in (("top", 0, 1), ("right", 1, 2), ("bottom", 2, 3), ("left", 3, 0)):
            quad = [corners[i], corners[j], roof[j], roof[i]]
            area = sum(quad[k][0] * quad[(k + 1) % 4][1] - quad[(k + 1) % 4][0] * quad[k][1]
                       for k in range(4))
            if area <= 0:
                continue
            col = wall[name]
            d.polygon([(x * ss, y * ss) for x, y in quad], fill=col)
            floors = max(2, int(h * 8))
            for fl in range(1, floors):
                u = fl / floors
                ax = corners[i][0] + (roof[i][0] - corners[i][0]) * u
                ay = corners[i][1] + (roof[i][1] - corners[i][1]) * u
                bx = corners[j][0] + (roof[j][0] - corners[j][0]) * u
                by = corners[j][1] + (roof[j][1] - corners[j][1]) * u
                nwin = int(max(abs(bx - ax), abs(by - ay)) / 4)
                for wv in range(1, nwin):
                    if rng.random() < 0.16:           # few lit windows: calm
                        v = wv / nwin
                        wx, wy = ax + (bx - ax) * v, ay + (by - ay) * v
                        c = rgb("JOVIAN", 4) if rng.random() < 0.7 else rgb("EARTH ORBIT", 5)
                        c = tuple(int(cc * 0.8) for cc in c)
                        d.rectangle([wx * ss, wy * ss, wx * ss + 1, wy * ss + 1], fill=c)
        rx0, ry0 = roof[0]
        rx1, ry1 = roof[2]
        d.rectangle([rx0 * ss, ry0 * ss, rx1 * ss, ry1 * ss], fill=p[1])
        if rx1 - rx0 > 4 and ry1 - ry0 > 4:
            d.rectangle([rx0 * ss + 3, ry0 * ss + 3, rx1 * ss - 3, ry1 * ss - 3], fill=p[2])
        d.line([rx0 * ss + 3, ry0 * ss + 3, rx1 * ss - 3, ry0 * ss + 3], fill=p[3])
        if rx1 - rx0 > 16 and ry1 - ry0 > 16 and rng.random() < 0.6:
            ax, ay = rng.uniform(rx0 + 4, rx1 - 12), rng.uniform(ry0 + 4, ry1 - 12)
            d.rectangle([ax * ss, ay * ss, (ax + 7) * ss, (ay + 7) * ss], fill=p[1])
            d.line([ax * ss, ay * ss, (ax + 7) * ss, ay * ss], fill=p[3])
        if bld["beacon"]:
            d.rectangle([rx1 * ss - 8, ry0 * ss + 4, rx1 * ss - 4, ry0 * ss + 8],
                        fill=(255, 50, 60))
        if bld["turret"]:
            turrets.append(((rx0 + rx1) / 2, (ry0 + ry1) / 2))

    def layer_images(self, f):
        spots = []
        ground = self.ground_image(f, spots)
        low = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        d = ImageDraw.Draw(low)
        casters = []
        per = self.s.period("low-air")
        for x, y0, direction in self.cars:
            # cars heading north keep pace with the player; southbound ones race past
            y = (y0 + self.s.off("low-air", f) - (per * f / N if direction < 0 else 0)) % per - 60
            car = Image.new("RGBA", (8, 14), (0, 0, 0, 0))
            cd = ImageDraw.Draw(car)
            cd.rectangle([0, 0, 7, 13], fill=self.pal[3] + (255,))
            cd.rectangle([1, 1, 6, 4], fill=self.pal[4] + (255,))
            cd.point([(1, 0), (6, 0)], fill=(255, 255, 220, 255))
            cd.point([(1, 13), (6, 13)], fill=(255, 40, 40, 255))
            d.line([x - 2, y + 7, x - 2, y + 30], fill=(255, 40, 60, 60))
            d.line([x + 2, y + 7, x + 2, y + 30], fill=(255, 40, 60, 60))
            sprite.paste_center(low, car, x, y)
            casters.append((car, x, y))
        src = []
        if spots:
            tx, ty = spots[0]
            base_off = self.s.off("ground", f)
            src = [(lambda ff: tx, lambda ff: ty + self.s.off("ground", ff) - base_off, 0)]
        air, air_casters = self.actors.draw(f, src)
        shadows = shadow_layer(casters, *SHADOW["low-air"], opacity=0.4)
        shadows.alpha_composite(shadow_layer(air_casters, *SHADOW["air"], opacity=0.45))
        high = scroll_tex(self.mist, self.s.off("high-air", f))
        return {"ground": ground, "shadows": shadows, "low-air": low, "air": air,
                "high-air": high}

    def compose(self, f):
        L = self.layer_images(f)
        img = L["ground"].copy()
        for k in ("shadows", "low-air", "air", "high-air"):
            img.alpha_composite(L[k])
        return img


# --------------------------------------------------------------------------- scene C: canyon

class CanyonScene:
    key = "c"
    title = "MARS CANYON - BALANCED"
    layers = ["far", "ground", "low-air", "air", "high-air"]
    notes = {"far": "X0.6 CANYON FLOOR + WALLS", "ground": "X1.0 PLATEAU, COLONY, PODS",
             "low-air": "X1.35 DUST PLUMES", "air": "PLAY PLANE",
             "high-air": "X2.2 DUST STREAKS"}
    TRAVEL = 640                 # 160 px/s
    FACTORS = {"far": 0.6, "ground": 1.0, "low-air": 1.35, "high-air": 2.2}
    SS = 2

    def __init__(self):
        self.s = Scroller(self.TRAVEL, self.FACTORS)
        self.actors = Actors([dive(200, 50, 0.0), dive(280, 50, 0.0, 0.04),
                              dive(160, 50, 0.0, 0.08), dive(320, 50, 0.0, 0.08)])
        m = B["MARS"]
        self.m = m
        per = self.TRAVEL
        self.plateau = posterize(self._plateau(per, m), 20)
        fper = self.s.period("far")
        fn = periodic_fbm(W, fper, 64, 23, octaves=4)
        floor = raster.ramp([(0, m[0]), (0.6, m[1]), (1, m[2])], fn)
        self.floor = posterize(raster.to_rgba_image(floor * 0.85), 12)
        self.parts = {"dome": station.render_part(station.dome_part(), B),
                      "dome_s": station.render_part(station.dome_part(17), B),
                      "truss": station.render_part(station.truss_h_part(70, 18), B),
                      "cargo": station.render_part(station.cargo_part(), B)}
        self.pod = render_sprite(models.vrell_turret_model(palette=B.vrell_colors()), 28, 1.6)
        rng = np.random.default_rng(5)
        self.plumes = []
        for _ in range(6):
            r = rng.uniform(26, 50)
            blob = raster.fbm(int(r * 2), int(r * 2), 16, int(rng.integers(1000)), octaves=3,
                              period=False)
            yy, xx = np.mgrid[0:int(r * 2), 0:int(r * 2)]
            fall = np.clip(1 - np.hypot(xx - r, yy - r) / r, 0, 1)
            a = np.clip(blob * fall * 2.2 - 0.2, 0, 1) * 150
            img = raster.to_rgba_image(np.broadcast_to(np.array(m[5], float) * 0.85,
                                                       a.shape + (3,)), a)
            self.plumes.append((img, rng.uniform(30, W - 30),
                                rng.uniform(0, self.s.period("low-air"))))
        self.streaks = [(rng.uniform(0, W), rng.uniform(0, self.s.period("high-air")),
                         rng.uniform(0.3, 1.0)) for _ in range(16)]

    @staticmethod
    def _plateau(per, m):
        """Plateau texture (tiles with period ``per``): a lit height field of dunes, wind-carved
        ridges and craters, in the darker Mars tones so the sprites stay on top."""
        n = periodic_fbm(W, per, 128, 17, octaves=5)
        ridge = 1 - np.abs(2 * periodic_fbm(W, per, 64, 29, octaves=4) - 1)
        hgt = 0.65 * n + 0.35 * ridge ** 3
        rng = np.random.default_rng(31)
        yy, xx = np.mgrid[0:per, 0:W]
        for _ in range(14):
            cx, cy, r = rng.uniform(0, W), rng.uniform(0, per), rng.uniform(6, 26)
            dy = np.minimum.reduce([np.abs(yy - cy), np.abs(yy - cy - per), np.abs(yy - cy + per)])
            d = np.hypot(xx - cx, dy) / r
            bowl = np.where(d < 1, -(1 - d ** 2), 0.0) * 0.25
            rim = np.exp(-((d - 1) / 0.18) ** 2) * 0.08
            hgt = hgt + (bowl + rim) * (r / 26)
        gy, gx = np.gradient(hgt * 120)
        shade = np.clip(1 - (gx + gy) * 0.45, 0.55, 1.35)
        col = raster.ramp([(0.0, m[0]), (0.35, m[1]), (0.7, m[2]), (1.0, m[3])],
                          np.clip(hgt * 1.1, 0, 1))
        col = col * shade[..., None] * 0.75
        return raster.to_rgba_image(col)

    # canyon centre line and half width, periodic in world y
    def centre(self, yw):
        t = 2 * np.pi * yw / self.TRAVEL
        return W / 2 + 70 * np.sin(t) + 22 * np.sin(3 * t + 0.7)

    def half(self, yw):
        t = 2 * np.pi * yw / self.TRAVEL
        return 62 + 16 * np.sin(2 * t + 1.3)

    def _proj(self, x, y, k):
        cx, cy = W / 2, H * 0.5
        return cx + (x - cx) * k, cy + (y - cy) * k

    # (sprite key, world y, offset from the rim: > 0 right of the right rim, < 0 left of left)
    OUTPOST = [("dome", 120, 50), ("dome_s", 178, 110), ("truss", 150, 95), ("cargo", 205, 40),
               ("dome", 450, -52), ("cargo", 500, -40)]
    PODS = [(60, -30), (330, 30), (560, -30)]

    def ground_image(self, f, pods_out=None):
        """Returns (far, ground): floor + walls alone, and the full ground composite."""
        off = self.s.off("ground", f)
        ss = self.SS
        img = scroll_tex(self.floor, self.s.off("far", f)).resize((W * ss, H * ss), Image.NEAREST)
        d = ImageDraw.Draw(img)
        k = self.FACTORS["far"]
        ys = np.arange(-160, H + 180, 6)
        rims = [[(self.centre(y - off) + side * self.half(y - off), y) for y in ys]
                for side in (-1, 1)]
        # dry riverbed on the floor, projected with the floor's perspective
        river = [self._proj(self.centre(y - off) + 10 * np.sin((y - off) * 0.05), y, k)
                 for y in ys]
        d.line([(x * ss, y * ss) for x, y in river], fill=self.m[0], width=int(7 * ss))
        d.line([(x * ss, y * ss) for x, y in river], fill=tuple(int(c * 0.6) for c in self.m[2]),
               width=int(2 * ss))
        # walls: strata bands between the rim (scale 1) and the floor (scale 0.6)
        to_light = np.array([-0.7071, -0.7071])       # towards the top-left key light
        m = self.m
        strata = [m[3], m[2], tuple(int(c * 0.85) for c in m[3]), m[1], m[2]]
        for side, pts in zip((-1, 1), rims):
            for (xa, ya), (xb, yb) in zip(pts[:-1], pts[1:]):
                n = np.array([yb - ya, -(xb - xa)], dtype=float)
                n /= np.hypot(*n) or 1
                if n[0] * side > 0:                   # make the wall normal face into the canyon
                    n = -n
                lit = 0.45 + 0.55 * max(0.0, float(n @ to_light))
                for bnd, base in enumerate(strata):
                    t0, t1 = bnd / len(strata), (bnd + 1) / len(strata)
                    k0, k1 = 1 - (1 - k) * t0, 1 - (1 - k) * t1
                    q = [self._proj(xa, ya, k0), self._proj(xb, yb, k0),
                         self._proj(xb, yb, k1), self._proj(xa, ya, k1)]
                    col = tuple(int(c * lit * (1 - 0.1 * bnd)) for c in base)
                    d.polygon([(x * ss, y * ss) for x, y in q], fill=col)
        far = img.resize((W, H), Image.BOX)
        # plateau with the canyon cut out
        plat = scroll_tex(self.plateau, off).resize((W * ss, H * ss), Image.NEAREST)
        mask = Image.new("L", (W * ss, H * ss), 255)
        ImageDraw.Draw(mask).polygon([(x * ss, y * ss) for x, y in rims[0] + rims[1][::-1]],
                                     fill=0)
        img.paste(plat, (0, 0), mask)
        for pts, col in ((rims[0], m[4]), (rims[1], m[5])):     # sunlit rim edge
            d.line([(x * ss, y * ss) for x, y in pts], fill=tuple(int(c * 0.8) for c in col),
                   width=ss)
        img = img.resize((W, H), Image.BOX)
        for key, yw, dx in self.OUTPOST:
            x = self._beside(yw, dx)
            for yy in self.s.ys("ground", yw, f):
                sprite.paste_center(img, self.parts[key], x, yy)
        for yw, dx in self.PODS:
            x = self._beside(yw, dx)
            for yy in self.s.ys("ground", yw, f):
                sprite.paste_center(img, self.pod, x, yy)
                if pods_out is not None:
                    pods_out.append((x, yy, yw))
        return far, img

    def _beside(self, yw, dx):
        """x of a point ``dx`` beyond the canyon rim at world y (sign picks the rim)."""
        return float(self.centre(yw) + np.sign(dx) * self.half(yw) + dx)

    def layer_images(self, f):
        far, ground = self.ground_image(f)
        low = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        for img, x, y in self.plumes:
            for yy in self.s.ys("low-air", y, f, margin=120):
                sprite.paste_center(low, img, x + 12 * np.sin(yy * 0.02), yy)
        srcs = []
        for i, (yw, dx) in enumerate(self.PODS[:2]):
            x = self._beside(yw, dx)
            srcs.append((lambda ff, x=x: x,
                         lambda ff, yw=yw: (yw + self.s.off("ground", ff)) % self.TRAVEL,
                         i * 40))
        air, casters = self.actors.draw(f, srcs)
        shadows = shadow_layer(casters, *SHADOW["air"], opacity=0.45)
        high = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        d = ImageDraw.Draw(high)
        step = self.FACTORS["high-air"] * self.TRAVEL / N
        for x, y, b in self.streaks:
            for yy in self.s.ys("high-air", y, f, margin=40):
                d.line([x, yy - step * b, x, yy], fill=self.m[5] + (int(35 + 50 * b),),
                       width=2)
        return {"far": far, "ground": ground, "shadows": shadows, "low-air": low,
                "air": air, "high-air": high}

    def compose(self, f):
        L = self.layer_images(f)
        img = L["ground"].copy()
        for k in ("shadows", "low-air", "air", "high-air"):
            img.alpha_composite(L[k])
        return img


SCENES = {"a": OrbitScene, "b": CityScene, "c": CanyonScene}
_cache = {}


def get_scene(key):
    if key not in _cache:
        _cache[key] = SCENES[key]()
    return _cache[key]


# --------------------------------------------------------------------------- outputs

def checker(w, h, cell=8):
    img = Image.new("RGBA", (w, h))
    d = ImageDraw.Draw(img)
    for y in range(0, h, cell):
        for x in range(0, w, cell):
            c = (34, 38, 50) if (x // cell + y // cell) % 2 == 0 else (44, 48, 62)
            d.rectangle([x, y, x + cell - 1, y + cell - 1], fill=c + (255,))
    return img


def wrap(text, width):
    words, lines, cur = text.split(), [], ""
    for w in words:
        if len(cur) + len(w) + (1 if cur else 0) > width:
            lines.append(cur)
            cur = w
        else:
            cur = f"{cur} {w}" if cur else w
    return lines + ([cur] if cur else [])


def make_sheet(scene, f=24):
    frame_img = scene.compose(f)
    layers = scene.layer_images(f)
    tw, th = 192, 216
    img = raster.sheet(1140, 690, f"PARALLAX {scene.key.upper()} (R02): {scene.title}",
                       "CONCEPT ROUND 02 - 960X540")
    speed = scene.TRAVEL * FPS / N
    raster.draw_text(img, 16, 38, f"PLAY FIELD 480X540 AT 1X - GROUND SCROLLS {speed:.0f} PX/S",
                     raster.LABEL_DIM)
    img.alpha_composite(frame_img, (16, 50))
    raster.draw_text(img, 516, 38, "LAYER BREAKDOWN, BACK TO FRONT (0.4X)", raster.LABEL_DIM)
    for i, key in enumerate(scene.layers):
        x = 516 + (i % 3) * (tw + 12)
        y = 50 + (i // 3) * (th + 44)
        cell = checker(tw, th)
        lay = layers[key]
        if key == "air":
            lay = layers["shadows"].copy()
            lay.alpha_composite(layers["air"])
        cell.alpha_composite(lay.resize((tw, th), Image.BOX))
        img.alpha_composite(cell, (x, y + 12))
        raster.draw_text(img, x, y, f"{i + 1}. {key.upper()}", raster.ACCENT)
        raster.draw_text(img, x, y + th + 16, scene.notes[key], raster.LABEL_DIM)
    notes = [
        f"FACTORS RELATIVE TO GROUND: " + ", ".join(f"{k.upper()} {v:g}" for k, v in
                                                    scene.FACTORS.items()) + ".",
        "4 S SEAMLESS LOOP; LAYERS WHOSE LOOP TRAVEL IS SHORTER THAN THE SCREEN CROSS-FADE.",
        "SHADOWS DOWN-RIGHT: LOW-AIR (9,13), AIR (21,30). ENEMY SHOTS: DARK RIM, WHITE CORE.",
    ]
    y = 580
    for line in notes:
        for part in wrap(line, 100):
            raster.draw_text(img, 516, y, part, raster.LABEL)
            y += 11
        y += 5
    return img


def make_gif(scene, path):
    with tempfile.TemporaryDirectory() as tmp:
        for f in range(N):
            scene.compose(f).convert("RGB").save(Path(tmp) / f"f{f:03d}.png")
        cmd = ["ffmpeg", "-v", "error", "-y", "-framerate", str(FPS),
               "-i", str(Path(tmp) / "f%03d.png"),
               "-vf", "split[a][b];[a]palettegen=max_colors=128:stats_mode=full[p];"
                      "[b][p]paletteuse=dither=none:diff_mode=rectangle",
               "-loop", "0", str(path)]
        subprocess.run(cmd, check=True)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for key in sys.argv[1:] or list(SCENES):
        scene = get_scene(key)
        png = OUT / f"parallax-r02-{key}.png"
        make_sheet(scene).convert("RGB").save(png, optimize=True)
        print("wrote", png.relative_to(ROOT))
        gif = OUT / f"parallax-r02-{key}.gif"
        make_gif(scene, gif)
        print("wrote", gif.relative_to(ROOT), f"{gif.stat().st_size / 1e6:.1f} MB")


if __name__ == "__main__":
    main()
