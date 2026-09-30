#!/usr/bin/env python3
"""Concept round 01 - parallax scenes demonstrating the layer model of design/art-direction.

Outputs (design/art-direction/concept/):
  parallax-r01-a.png   Earth orbit: frame at 2x plus labelled layer breakdown
  parallax-r01-a.gif   Earth orbit: 4 s seamless scroll loop (native 320x360)
  parallax-r01-b.png   Earth megacity at night: frame at 2x plus layer breakdown
  parallax-r01-b.gif   Earth megacity at night: 4 s seamless scroll loop (native 320x360)

Layers and scroll factors (relative to the ground layer):
  deep 0.25 | ground 1.0 | low-air 1.25 | air = play plane (screen space) | high-air 1.75
The loop is seamless: every layer's content repeats after exactly one loop of travel
(factor * TRAVEL pixels); the deep layer cross-fades because its travel is shorter than the
screen. Other scripts (hud.py) import ``get_scene`` to reuse a frame.
Run: python3 tools/concept/parallax.py
"""
import subprocess
import sys
import tempfile
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, str(Path(__file__).resolve().parent))
from render import models, raster, sdf, sprite, terrain  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "design" / "art-direction" / "concept"

W, H = 320, 360          # play field, native pixels
N = 96                   # frames per loop (24 fps -> 4 s)
FPS = 24
TRAVEL = 384             # ground-layer pixels per loop (96 px/s)
FACTORS = {"deep": 0.25, "ground": 1.0, "low-air": 1.25, "high-air": 1.75}
SHADOW_OFFSET = {"low-air": (5, 7), "air": (14, 20)}


def offset(layer, f):
    return FACTORS[layer] * TRAVEL * f / N


def period(layer):
    return int(round(FACTORS[layer] * TRAVEL))


# --------------------------------------------------------------------------- sprites

def render_sprite(model, size, extent, colors=24, factor=8, **kw):
    scene, mats = model
    hi = sdf.render(scene, mats, (size * factor, size * factor), extent, **kw)
    return sprite.make_sprite(hi, factor, colors)


class Actors:
    """Screen-space actors on the air layer: player, Vrell darts, a brood gunship, bullets."""

    def __init__(self, ship="a"):
        fn = models.PLAYER_SHIPS[ship][1]
        bank = np.radians(24)
        self.player = {b: render_sprite(fn(bank=a), 32, 2.3)
                       for b, a in (("l", -bank), ("c", 0.0), ("r", bank))}
        self.dart = render_sprite(models.vrell_dart_model(), 28, 2.3)
        self.brood = render_sprite(models.vrell_brood_model(), 56, 2.6, colors=32)
        self.enemy_bullet = self._orb((255, 60, 170), (255, 225, 245))
        self.bolt = self._bolt()
        self.flare = self._flare()

    @staticmethod
    def _orb(color, core):
        img = Image.new("RGBA", (9, 9), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        d.ellipse([0, 0, 8, 8], fill=(20, 0, 20, 255))          # dark rim: pops on bright bg
        d.ellipse([1, 1, 7, 7], fill=color + (255,))
        d.ellipse([3, 3, 5, 5], fill=core + (255,))
        return img

    @staticmethod
    def _flare():
        yy, xx = np.mgrid[0:11, 0:11]
        d = np.sqrt((xx - 5) ** 2 + ((yy - 5) * 0.7) ** 2) / 5
        a = np.clip(1 - d, 0, 1) ** 1.6
        rgb = np.zeros((11, 11, 3))
        rgb[:] = (140, 200, 255)
        rgb[d < 0.35] = (255, 255, 255)
        return raster.to_rgba_image(rgb, a * 230)

    @staticmethod
    def _bolt():
        img = Image.new("RGBA", (3, 10), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        d.rectangle([0, 0, 2, 9], fill=(90, 170, 255, 200))
        d.rectangle([1, 1, 1, 8], fill=(255, 255, 255, 255))
        return img

    def player_pos(self, f):
        t = 2 * np.pi * f / N
        return W / 2 + 34 * np.sin(t), 300 + 6 * np.sin(2 * t), np.cos(t)

    def layer(self, f):
        """Return (image, list of (sprite, x, y) that cast shadows)."""
        img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        casters = []
        # brood gunship holding at the top, firing rings of bullets
        bx, by = W / 2 + 40 * np.sin(2 * np.pi * f / N + 1.0), 70
        for k in range(4):
            age = (f - k * 24) % N
            r = 18 + age * 2.4
            for j in range(8):
                a = 2 * np.pi * j / 8 + k * 0.39
                sprite.paste_center(img, self.enemy_bullet,
                                    bx + r * np.cos(a), by + r * np.sin(a) * 0.95)
        casters.append((self.brood, bx, by))
        # a V of five darts diving down the screen once per loop
        base_y = -40 + 460 * f / N
        for i, dx in enumerate((-48, -24, 0, 24, 48)):
            y = base_y - abs(dx) * 0.8
            x = W / 2 + dx + 22 * np.sin(2 * np.pi * (f / N) * 2 + i * 0.4)
            casters.append((self.dart, x, y))
        # player and its twin shots
        px, py, vx = self.player_pos(f)
        for k in range(8):
            age = ((12 * f + 36 * k) % 288) / 12.0
            ox, _, _ = self.player_pos(f - age)
            y = py - 14 - age * 12
            sprite.paste_center(img, self.bolt, ox - 6, y)
            sprite.paste_center(img, self.bolt, ox + 6, y)
        frame = "r" if vx > 0.5 else "l" if vx < -0.5 else "c"
        casters.append((self.player[frame], px, py))
        for sp, x, y in casters:
            sprite.paste_center(img, sp, x, y)
        flicker = 1 + (f % 3 == 0)
        sprite.paste_center(img, self.flare, px, py + 16 + flicker)
        return img, casters


def shadow_layer(casters, dx, dy, opacity=0.5, scale=0.85, blur=1.2):
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    for sp, x, y in casters:
        sh = sprite.shadow_of(sp, opacity=opacity, blur=blur, scale=scale)
        sprite.paste_center(img, sh, x + dx, y + dy)
    return img


def scroll_window(tex, off, per):
    """Vertical window of a periodic texture: content moves down the screen as off grows."""
    arr = np.array(tex)
    h = arr.shape[0]
    start = int(round(-off)) % per
    rows = (np.arange(H) + start) % h
    return Image.fromarray(arr[rows], "RGBA")


# --------------------------------------------------------------------------- scene A: orbit

class OrbitScene:
    key = "a"
    title = "EARTH ORBIT"
    layers = ["deep", "ground", "air", "high-air"]
    labels = {"deep": "DEEP X0.25: EARTH FAR BELOW",
              "ground": "GROUND X1.0: STATION + VRELL PODS",
              "air": "AIR: PLAY PLANE (SCREEN SPACE)",
              "high-air": "HIGH-AIR X1.75: DEBRIS"}

    def __init__(self):
        self.actors = Actors("a")
        deep_travel = int(round(FACTORS["deep"] * TRAVEL))
        self.deep_travel = deep_travel
        self.earth = terrain.earth_from_orbit(W, H + 2 * deep_travel + 2, seed=11,
                                              period=False)
        self.ground = self._station_strip()
        self.debris = [render_sprite(models.debris_model(a, 0.5 + 0.3 * np.sin(a)), 20, 1.4,
                                     colors=16)
                       for a in np.linspace(0, 2 * np.pi, 8, endpoint=False)]
        rng = np.random.default_rng(7)
        per = period("high-air")
        self.debris_pos = [(rng.uniform(10, W - 10), rng.uniform(0, per), rng.integers(0, 8))
                           for _ in range(7)]

    def _station_strip(self):
        per = period("ground")
        strip = Image.new("RGBA", (W, per), (0, 0, 0, 0))
        scale = 60.0  # native px per world unit
        for i, (variant, x) in enumerate(((0, 160), (1, 160))):
            scene, mats = models.station_segment_model(variant)
            w_units, h_units = 3.4, 3.2
            size = (int(w_units * scale) * 4, int(h_units * scale) * 4)
            hi = sdf.render(scene, mats, size, w_units, steps=140)
            seg = sprite.make_sprite(hi, 4, 48, crisp=60)
            y = i * per // 2
            for yy in (y - per, y, y + per):
                sprite.paste_center(strip, seg, x, yy + per // 4)
        # Vrell growth pods clinging to the station (ground-layer enemies)
        pod = render_sprite(models.vrell_turret_model(), 18, 1.6)
        for x, y in ((120, 60), (212, 150), (130, 280), (205, 330)):
            for yy in (y - per, y, y + per):
                sprite.paste_center(strip, pod, x, yy)
        return strip

    def layer_images(self, f):
        # deep: cross-fade between the window and the window one loop further back
        off = offset("deep", f)
        arr = np.array(self.earth).astype(np.float64)

        def window(o):  # o in [-travel, travel]
            s = int(round(self.deep_travel - o))
            return arr[s:s + H]
        wgt = sdf.smoothstep(N - 24, N, f)
        deep = window(off) * (1 - wgt) + window(off - self.deep_travel) * wgt
        deep = Image.fromarray(deep.astype(np.uint8), "RGBA")
        ground = scroll_window(self.ground, offset("ground", f), period("ground"))
        air, casters = self.actors.layer(f)
        high = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        per = period("high-air")
        o = offset("high-air", f)
        for x, y, k in self.debris_pos:
            yy = (y + o) % per - 30
            spr = self.debris[(k + f // 6) % 8]
            spr = spr.resize((spr.width * 3 // 2, spr.height * 3 // 2), Image.BILINEAR)
            spr = spr.filter(ImageFilter.GaussianBlur(0.6))
            a = spr.getchannel("A").point(lambda v: int(v * 0.8))
            spr.putalpha(a)
            sprite.paste_center(high, spr, x, yy)
        # air shadows fall on the station only (space has nothing else to catch them)
        dx, dy = SHADOW_OFFSET["air"]
        sh = shadow_layer(casters, dx, dy, opacity=0.55)
        mask = np.array(ground.getchannel("A")).astype(np.float64) / 255
        sha = np.array(sh)
        sha[..., 3] = (sha[..., 3] * mask).astype(np.uint8)
        shadows = Image.fromarray(sha, "RGBA")
        return {"deep": deep, "ground": ground, "shadows": shadows, "air": air, "high-air": high}

    def compose(self, f):
        L = self.layer_images(f)
        img = L["deep"].copy()
        for k in ("ground", "shadows", "air", "high-air"):
            img.alpha_composite(L[k])
        return img


# --------------------------------------------------------------------------- scene B: city

class CityScene:
    key = "b"
    title = "EARTH MEGACITY AT NIGHT"
    layers = ["ground", "low-air", "air", "high-air"]
    labels = {"ground": "GROUND X1.0: STREETS, TOWERS, TURRETS",
              "low-air": "LOW-AIR X1.25: TRAFFIC",
              "air": "AIR: PLAY PLANE (SCREEN SPACE)",
              "high-air": "HIGH-AIR X1.75: CLOUDS"}
    SS = 2              # supersampling for the vector-drawn city
    CAM = 5.0           # camera height for the building perspective
    BLOCK = 48
    STREET = 10

    def __init__(self):
        self.actors = Actors("a")
        per = period("ground")
        tex, streets = terrain.city_ground(W, per, seed=5, block=self.BLOCK, street=self.STREET)
        self.street_tex = self._street_lights(tex, streets, per)
        self.buildings = self._buildings(per)
        self.turret = render_sprite(models.vrell_turret_model(), 18, 1.6)
        self.clouds = self._clouds()
        rng = np.random.default_rng(12)
        self.traffic = [(rng.uniform(0, 360), rng.uniform(0, period("low-air")),
                         rng.choice([-1, 1]), rng.integers(0, 3)) for _ in range(9)]

    def _street_lights(self, tex, streets, per):
        arr = np.array(tex).astype(np.float64)
        light = np.zeros(arr.shape[:2])
        b, s = self.BLOCK, self.STREET
        yy, xx = np.mgrid[0:per, 0:W]
        for cy in range(0, per, b // 2):
            for cx in range(0, W + b, b):
                for lx, ly in ((cx + s // 2, cy), (cx, cy + s // 2)):
                    d2 = np.minimum((xx - lx) ** 2, (xx - lx - W) ** 2) + \
                        np.minimum((yy - ly) ** 2, np.minimum((yy - ly - per) ** 2,
                                                              (yy - ly + per) ** 2))
                    light += np.exp(-d2 / 14.0)
        arr[..., 0] += light * 120
        arr[..., 1] += light * 72
        arr[..., 2] += light * 22
        return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")

    def _buildings(self, per):
        rng = np.random.default_rng(99)
        b, s = self.BLOCK, self.STREET
        out = []
        for by in range(0, per, b):
            for bx in range(-b, W + b, b):
                # each block holds one to three towers
                n = rng.integers(1, 4)
                cells = [(0, 0, b - s, b - s)] if n == 1 else \
                    [(0, 0, (b - s) // 2 - 1, b - s), ((b - s) // 2 + 1, 0, b - s, b - s)] \
                    if n == 2 else \
                    [(0, 0, b - s, (b - s) // 2 - 1), (0, (b - s) // 2 + 1, (b - s) // 2 - 1,
                     b - s), ((b - s) // 2 + 1, (b - s) // 2 + 1, b - s, b - s)]
                for x0, y0, x1, y1 in cells:
                    inset = rng.integers(1, 4)
                    h = rng.uniform(0.3, 1.0) ** 1.5 * 1.5
                    out.append({
                        "x0": bx + s + x0 + inset, "y0": by + s + y0 + inset,
                        "x1": bx + s + x1 - inset, "y1": by + s + y1 - inset,
                        "h": h, "seed": int(rng.integers(0, 1 << 30)),
                        "tint": rng.uniform(0.85, 1.15),
                        "turret": rng.random() < 0.12 and h > 0.5,
                        "pad": rng.random() < 0.05, "beacon": h > 1.0,
                    })
        return sorted(out, key=lambda o: o["h"])

    def _clouds(self):
        per = period("high-air")
        n = raster.fbm(W, per, 48, 31, octaves=4, period=True)
        a = np.clip((n - 0.55) * 3.0, 0, 1) ** 1.3
        under = raster.fbm(W, per, 48, 77, octaves=3, period=True)
        rgb = np.zeros((per, W, 3))
        rgb[:] = np.array([70, 64, 92])
        rgb += (under[..., None] * np.array([120, 60, 40]))       # city glow from below
        return raster.to_rgba_image(rgb, a * 105)   # high-air never hides the play plane

    def _project(self, x, y, h):
        cx, cy = W / 2, H * 0.55
        k = self.CAM / (self.CAM - h)
        return cx + (x - cx) * k, cy + (y - cy) * k

    def _ground(self, f):
        off = offset("ground", f)
        per = period("ground")
        ss = self.SS
        base = scroll_window(self.street_tex, off, per).resize((W * ss, H * ss), Image.NEAREST)
        d = ImageDraw.Draw(base)
        turrets = []
        for bld in self.buildings:
            for rep in (-per, 0, per):
                y0 = bld["y0"] + off % per + rep - per
                y1 = bld["y1"] + off % per + rep - per
                if y1 < -60 or y0 > H + 60:
                    continue
                self._draw_building(d, bld, bld["x0"], y0, bld["x1"], y1, ss, turrets)
        img = base.resize((W, H), Image.BOX)
        for x, y in turrets:
            sprite.paste_center(img, self.turret, x, y)
        return img

    def _draw_building(self, d, bld, x0, y0, x1, y1, ss, turrets):
        h = bld["h"]
        corners = [(x0, y0), (x1, y0), (x1, y1), (x0, y1)]
        roof = [self._project(x, y, h) for x, y in corners]
        t = bld["tint"]
        wall_col = {  # light from the top-left: walls facing up/left are lit
            "top": (40, 44, 64), "left": (34, 38, 56), "right": (15, 16, 26),
            "bottom": (19, 20, 32)}
        walls = [("top", 0, 1), ("right", 1, 2), ("bottom", 2, 3), ("left", 3, 0)]
        rng = np.random.default_rng(bld["seed"])
        for name, i, j in walls:
            quad = [corners[i], corners[j], roof[j], roof[i]]
            # only draw walls that face the camera (their roof edge moved outward)
            area = 0.0
            for k in range(4):
                xa, ya = quad[k]
                xb, yb = quad[(k + 1) % 4]
                area += xa * yb - xb * ya
            if area <= 0:
                continue
            col = tuple(int(c * t) for c in wall_col[name])
            d.polygon([(x * ss, y * ss) for x, y in quad], fill=col)
            # lit windows along the wall
            floors = max(2, int(h * 9))
            for fl in range(1, floors):
                u = fl / floors
                ax, ay = corners[i][0] + (roof[i][0] - corners[i][0]) * u, \
                    corners[i][1] + (roof[i][1] - corners[i][1]) * u
                bx, by = corners[j][0] + (roof[j][0] - corners[j][0]) * u, \
                    corners[j][1] + (roof[j][1] - corners[j][1]) * u
                nwin = int(max(abs(bx - ax), abs(by - ay)) / 2.5)
                for wv in range(1, nwin):
                    if rng.random() < 0.38:
                        v = wv / nwin
                        wx, wy = ax + (bx - ax) * v, ay + (by - ay) * v
                        warm = rng.random() < 0.7
                        c = (255, 214, 130) if warm else (150, 210, 255)
                        if name in ("right", "bottom"):
                            c = tuple(int(cc * 0.75) for cc in c)
                        d.rectangle([wx * ss, wy * ss, wx * ss + 1, wy * ss + 1], fill=c)
        # roof slab with parapet and details
        roof_col = tuple(int(c * t) for c in (38, 42, 58))
        d.polygon([(x * ss, y * ss) for x, y in roof], fill=(22, 24, 34))
        rx0, ry0 = roof[0]
        rx1, ry1 = roof[2]
        d.rectangle([rx0 * ss + 2, ry0 * ss + 2, rx1 * ss - 2, ry1 * ss - 2], fill=roof_col)
        d.line([rx0 * ss + 2, ry0 * ss + 2, rx1 * ss - 2, ry0 * ss + 2],
               fill=tuple(min(255, int(c * 1.35)) for c in roof_col))
        w, hh = rx1 - rx0, ry1 - ry0
        if bld["pad"] and w > 14 and hh > 14:
            cx, cy = (rx0 + rx1) / 2, (ry0 + ry1) / 2
            d.ellipse([(cx - 5) * ss, (cy - 5) * ss, (cx + 5) * ss, (cy + 5) * ss],
                      outline=(120, 118, 70), width=2)
        else:
            for _ in range(rng.integers(1, 4)):
                ax = rng.uniform(rx0 + 2, max(rx0 + 3, rx1 - 5))
                ay = rng.uniform(ry0 + 2, max(ry0 + 3, ry1 - 5))
                d.rectangle([ax * ss, ay * ss, (ax + 3) * ss, (ay + 3) * ss], fill=(28, 30, 42))
                d.rectangle([ax * ss, ay * ss, (ax + 3) * ss, ay * ss + 1], fill=(64, 68, 88))
        if bld["beacon"]:
            d.rectangle([rx1 * ss - 7, ry0 * ss + 3, rx1 * ss - 4, ry0 * ss + 6],
                        fill=(255, 60, 50))
        if bld["turret"]:
            turrets.append(((rx0 + rx1) / 2, (ry0 + ry1) / 2))

    def _low_air(self, f):
        img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        per = period("low-air")
        o = offset("low-air", f)
        casters = []
        for x0, y0, direction, kind in self.traffic:
            x = (x0 + direction * 360 * f / N) % 360 - 20
            y = (y0 + o) % per - 40
            car = Image.new("RGBA", (9, 5), (0, 0, 0, 0))
            cd = ImageDraw.Draw(car)
            body = [(70, 76, 96), (96, 60, 60), (60, 90, 96)][kind]
            cd.rectangle([0, 0, 8, 4], fill=body + (255,))
            cd.rectangle([1, 1, 7, 1], fill=tuple(min(255, c + 60) for c in body) + (255,))
            head, tail = (8, 1), (0, 1)
            if direction < 0:
                head, tail = tail, head
            cd.point([head, (head[0], 3)], fill=(255, 255, 220, 255))
            cd.point([tail, (tail[0], 3)], fill=(255, 40, 40, 255))
            trail_x = x - direction * 12
            d.line([trail_x, y + 1, x - direction * 4, y + 1], fill=(255, 60, 60, 70))
            d.line([trail_x, y + 3, x - direction * 4, y + 3], fill=(255, 60, 60, 70))
            sprite.paste_center(img, car, x, y + 2)
            casters.append((car, x, y + 2))
        return img, casters

    def _high_air(self, f):
        return scroll_window(self.clouds, offset("high-air", f), period("high-air"))

    def layer_images(self, f):
        ground = self._ground(f)
        low, low_casters = self._low_air(f)
        air, casters = self.actors.layer(f)
        sh = shadow_layer(low_casters, *SHADOW_OFFSET["low-air"], opacity=0.45, blur=0.6)
        sh.alpha_composite(shadow_layer(casters, *SHADOW_OFFSET["air"], opacity=0.5))
        return {"ground": ground, "shadows": sh, "low-air": low, "air": air,
                "high-air": self._high_air(f)}

    def compose(self, f):
        L = self.layer_images(f)
        img = L["ground"].copy()
        for k in ("shadows", "low-air", "air", "high-air"):
            img.alpha_composite(L[k])
        return img


SCENES = {"a": OrbitScene, "b": CityScene}
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


def make_sheet(scene):
    f = 30
    frame_img = scene.compose(f)
    layers = scene.layer_images(f)
    img = raster.sheet(1030, 800, f"PARALLAX SCENE {scene.key.upper()}: {scene.title}",
                       "CONCEPT ROUND 01")
    raster.draw_text(img, 16, 38, "PLAY FIELD 320X360 SHOWN AT 2X", raster.LABEL_DIM)
    img.alpha_composite(sprite.enlarge(frame_img, 2), (16, 50))
    raster.draw_text(img, 680, 38, "LAYER BREAKDOWN (BACK TO FRONT, 1X)", raster.LABEL_DIM)
    for i, key in enumerate(scene.layers):
        x = 680 + (i % 2) * 175
        y = 50 + (i // 2) * 230
        cell = checker(160, 180)
        lay = layers[key]
        if key == "air":
            lay = layers["shadows"].copy()
            lay.alpha_composite(layers["air"])
        cell.alpha_composite(lay.resize((160, 180), Image.BOX))
        img.alpha_composite(cell, (x, y + 12))
        raster.draw_text(img, x, y, f"{i + 1}. {key.upper()}", raster.ACCENT)
        for j, part in enumerate(scene.labels[key].split(": ", 1)[1:]):
            raster.draw_text(img, x, y + 196, part[:27], raster.LABEL_DIM)
            if len(part) > 27:
                raster.draw_text(img, x, y + 206, part[27:], raster.LABEL_DIM)
    notes = [
        "SCROLL FACTORS RELATIVE TO GROUND: DEEP 0.25, GROUND 1.0, LOW-AIR 1.25, HIGH-AIR 1.75.",
        "AIR LAYER IS SCREEN SPACE; FLYERS CAST SHADOWS DOWN-RIGHT (LIGHT FROM TOP-LEFT).",
        "ENEMY BULLETS: DARK RIM + BRIGHT CORE SO THEY READ ON ANY BACKGROUND.",
    ]
    y = 520
    for line in notes:
        for part in wrap(line, 56):
            raster.draw_text(img, 680, y, part, raster.LABEL)
            y += 11
        y += 5
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


def make_gif(scene, path):
    with tempfile.TemporaryDirectory() as tmp:
        for f in range(N):
            scene.compose(f).convert("RGB").save(Path(tmp) / f"f{f:03d}.png")
        cmd = ["ffmpeg", "-v", "error", "-y", "-framerate", str(FPS),
               "-i", str(Path(tmp) / "f%03d.png"),
               "-vf", "split[a][b];[a]palettegen=max_colors=256:stats_mode=full[p];"
                      "[b][p]paletteuse=dither=none:diff_mode=rectangle",
               "-loop", "0", str(path)]
        subprocess.run(cmd, check=True)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    only = sys.argv[1:] or list(SCENES)
    for key in only:
        scene = get_scene(key)
        png = OUT / f"parallax-r01-{key}.png"
        make_sheet(scene).convert("RGB").save(png, optimize=True)
        print("wrote", png.relative_to(ROOT))
        gif = OUT / f"parallax-r01-{key}.gif"
        make_gif(scene, gif)
        print("wrote", gif.relative_to(ROOT))


if __name__ == "__main__":
    main()
