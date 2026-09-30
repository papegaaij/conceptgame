#!/usr/bin/env python3
"""Concept round 03 - decoration pass on the three round 02 parallax scenes.

Outputs (design/art-direction/concept/):
  parallax-r03-a.png / .gif   Earth orbit: weather system on the deep layer, cloud decks on
                              low-air drifting between the station and the play plane, wisps
  parallax-r03-b.png / .gif   Night megacity: tree-lined avenues, richer parks, rooftop gardens,
                              low fog banks on low-air lit from below by the street lamps
  parallax-r03-c.png / .gif   Mars canyon: dust-storm banks on low-air, heavier dust streaks,
                              greenhouse tunnels, algae ponds and lichen fields at the colony

Follows the Decoration rules of design/art-direction: dense, opaque banks go on low-air (below
the play plane, so they never hide bullets); thin wisps and streaks on high-air stay under ~40 %
opacity; decoration is lower in contrast than enemies and bullets. Flyers cast their shadow onto
cloud tops as well as onto the ground. Scenes subclass parallax_r02; its outputs are untouched.
Run: python3 tools/concept/parallax_r03.py [a] [b] [c]
"""
import subprocess
import sys
import tempfile
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, str(Path(__file__).resolve().parent))
import parallax_r02 as r02  # noqa: E402
from parallax_r02 import H, W, alpha_scale, masked, posterize, scroll_tex  # noqa: E402
from render import raster, sdf, sprite  # noqa: E402
from render.config import ROOT  # noqa: E402
from render.palette import B  # noqa: E402
from render.sdf import Material, sd_capsule, sd_cylinder_y, sd_sphere, union  # noqa: E402

OUT = ROOT / "design" / "art-direction" / "concept"
CLOUD_SHADOW = (12, 17)      # flyer shadows on low-air cloud tops (closer than the ground)
BANK_SHADOW = (10, 14)       # cloud banks shadow the ground below them


# --------------------------------------------------------------------------- decoration helpers

def noise(w, per, cells, seed, gains=None):
    """Sum of periodic value noise with the given lattice sizes (each must divide ``per``),
    normalised to 0..1. Tiles vertically with period ``per``."""
    gains = gains or [0.5 ** i for i in range(len(cells))]
    out = sum(g * raster.value_noise(w, per, c, seed + 17 * i, period=True)
              for i, (c, g) in enumerate(zip(cells, gains)))
    lo, hi = out.min(), out.max()
    return (out - lo) / (hi - lo)


def bank(n, cover, soft, colors, max_alpha=235, light=5.0, glow=None):
    """Shaded, opaque-cored bank (cloud, fog, dust) from a noise field ``n``.

    cover      noise level where the bank starts; higher = less coverage
    soft       width of the soft edge in noise units
    colors     ramp from shadowed underside to lit top (list of rgb)
    glow       optional (rgb, weight array) added light, e.g. street lamps under fog
    """
    a = sdf.smoothstep(cover, cover + soft, n)
    dens = np.clip((n - cover) / max(1e-6, 1 - cover), 0, 1)
    smooth = np.array(Image.fromarray((dens * 255).astype(np.uint8)).filter(
        ImageFilter.GaussianBlur(4)), dtype=np.float64) / 255      # billowy, not noisy
    gy, gx = np.gradient(smooth * light * 10)
    shade = np.clip(0.35 + smooth * 0.75 - (gx + gy) * 1.2, 0, 1)  # lit towards the top-left
    stops = [(i / (len(colors) - 1), c) for i, c in enumerate(colors)]
    col = raster.ramp(stops, shade)
    if glow is not None:
        gcol, wgt = glow
        col = col + np.array(gcol) * wgt[..., None]
    return raster.to_rgba_image(col, a * max_alpha)


def step_alpha(img, levels=6):
    """Quantise alpha to a few levels (90s translucency tables did the same). Continuous alpha
    over a scrolling background creates a flood of composite colours and bloats the GIFs."""
    a = np.array(img)
    q = np.round(a[..., 3].astype(np.float64) / 255 * levels) / levels * 255
    a[..., 3] = q.astype(np.uint8)
    return Image.fromarray(a, "RGBA")


def stretched(w, per, cells, seed, stretch):
    """Noise elongated horizontally by ``stretch`` (wind-blown dust), still periodic in y."""
    n = noise(max(8, w // stretch), per, cells, seed)
    return np.array(Image.fromarray((n * 255).astype(np.uint8)).resize((w, per), Image.BICUBIC),
                    dtype=np.float64) / 255


def foliage_sprite(size, seed, greens, night=1.0):
    """A pre-rendered tree canopy: a cluster of lit spheres, palettised."""
    rng = np.random.default_rng(seed)
    blobs = [(rng.uniform(-0.35, 0.35), rng.uniform(-0.35, 0.35), rng.uniform(0.28, 0.42))
             for _ in range(5)]
    mats = [Material(tuple(c / 255 * night for c in greens[2]), shininess=12, spec=0.12),
            Material(tuple(c / 255 * night for c in greens[3]), shininess=12, spec=0.15)]

    def scene(p):
        items = [(sd_sphere(p, (x, y, 0.0), r), i % 2) for i, (x, y, r) in enumerate(blobs)]
        return union(*items, k=0.12)
    hi = sdf.render(scene, mats, (size * 8, size * 8), 2.0, ambient=0.3)
    return sprite.make_sprite(hi, 8, 12)


def greenhouse_sprite(pal, length=34, radius=10):
    """Colony greenhouse tunnel: green-tinted glass half-cylinder with rows of lit crops."""
    glass = Material(tuple(c * 0.7 for c in pal.f("EARTH SURFACE", 2)), metal=0.45,
                     shininess=110, spec=0.9,
                     emission=tuple(np.array(pal.f("EARTH SURFACE", 3)) * 0.6),
                     emission_pattern=lambda p, n: 0.25 + 0.75 * (np.abs(np.sin(p[:, 0] * 1.1))
                                                                  > 0.55))
    frame = Material(pal.f("UTC HULL", 4), metal=0.4, shininess=50, spec=0.5)

    def scene(p):
        body = np.maximum(sd_cylinder_y(p, (0, 0, 0), radius, length / 2), -p[:, 2])
        items = [(body, 0)]
        for y in np.linspace(-length / 2 + 2, length / 2 - 2, 6):
            rib = np.maximum(np.abs(np.sqrt(p[:, 0] ** 2 + p[:, 2] ** 2) - radius) - 0.7,
                             np.abs(p[:, 1] - y) - 0.8)
            items.append((np.maximum(rib, -p[:, 2]), 1))
        items.append((sd_capsule(p, (0, length / 2, 1), (0, length / 2 + 5, 1), 3), 1))
        return union(*items)
    w, h = radius * 2 + 6, length + 12
    hi = sdf.render(scene, [glass, frame], (w * 4, h * 4), float(w), z_top=40.0, steps=150)
    return sprite.make_sprite(hi, 4, 32, crisp=60)


def track_casters(scene):
    """Wrap the scene's Actors.draw so the air-layer shadow casters of the last frame are kept
    (needed to cast flyer shadows onto cloud tops)."""
    orig = scene.actors.draw

    def draw(f, aimed_from=()):
        img, casters = orig(f, aimed_from)
        scene._casters = casters
        return img, casters
    scene.actors.draw = draw
    scene._casters = []


def bank_shadow(bank_img, opacity):
    sh = sprite.shadow_of(bank_img, opacity=opacity, blur=3.0, scale=1.0)
    out = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    out.alpha_composite(sh, BANK_SHADOW)
    return out


def decorate(L, clouds, ground_key, casters, bank_shadow_opacity=0.35, flyer_opacity=0.4):
    """Insert a low-air bank layer into the r02 layer dict: bank shadows on the ground, the
    bank itself, flyer shadows on its top, then the r02 low-air items above it."""
    ground = L[ground_key]
    sh = L["shadows"].copy()
    sh.alpha_composite(masked(bank_shadow(clouds, bank_shadow_opacity), ground))
    top = r02.shadow_layer(casters, *CLOUD_SHADOW, opacity=flyer_opacity)
    low = clouds.copy()
    low.alpha_composite(masked(top, clouds))
    low.alpha_composite(L["low-air"])
    L = dict(L)
    L["shadows"], L["low-air"] = sh, low
    return L


# --------------------------------------------------------------------------- scene A: orbit

class OrbitScene(r02.OrbitScene):
    title = "EARTH ORBIT - WEATHER AND CLOUD DECKS"
    notes = dict(r02.OrbitScene.notes, **{"deep": "X0.12 EARTH, CYCLONE + X0.2 HAZE",
                                          "low-air": "X1.4 CLOUD DECKS, WRECKAGE",
                                          "high-air": "X2.4 WISPS, ICE STREAKS"})

    def __init__(self):
        super().__init__()
        track_casters(self)
        sea = B["EARTH ORBIT"]
        self.earth = self._weather(self.earth, sea)
        per = self.s.period("low-air")                        # 1064 = 56 * 19
        n = noise(W, per, [152, 76, 38, 19], 301, gains=[1.0, 0.5, 0.3, 0.15])
        # pale grey-blue clouds with blue shadows (palette B orbit ramp mixed towards grey)
        tones = [tuple(int(c * k + g) for c in sea[i]) for i, k, g in
                 ((1, 0.8, 30), (2, 0.5, 70), (4, 0.5, 100), (5, 0.6, 80))]
        self.decks = step_alpha(posterize(bank(n, 0.58, 0.12, tones, max_alpha=235), 12))
        self.decks = self._dim(self.decks, 0.85)
        hper = self.s.period("high-air")                      # 1824 = 96 * 19
        w = noise(W, hper, [96, 48, 24, 12], 311)
        a = np.clip((w - 0.62) * 3.0, 0, 1) ** 1.3 * 95      # <= ~37 % opacity
        self.wisps = step_alpha(raster.to_rgba_image(
            np.broadcast_to(np.array(sea[5], float) * 0.9, a.shape + (3,)), a), 10)
        self.haze_tex = step_alpha(self.haze_tex, 3)

    @staticmethod
    def _dim(img, k):
        a = np.array(img).astype(np.float64)
        a[..., :3] *= k
        return Image.fromarray(a.astype(np.uint8), "RGBA")

    def _weather(self, earth, sea):
        """Add a cyclone and cloud fronts to the Earth texture (a deep-layer weather system)."""
        e = np.array(earth).astype(np.float64)
        h, w = e.shape[:2]
        yy, xx = np.mgrid[0:h, 0:w]
        cx, cy = w * 0.6, h * 0.45
        r = np.hypot(xx - cx, yy - cy) + 1e-6
        ang = np.arctan2(yy - cy, xx - cx)
        swirl = (np.sin(ang * 2 + np.log(r) * 3.6) * 0.5 + 0.5)
        tex = raster.fbm(w, h, 32, 331, octaves=5, period=False)
        fall = np.clip(1 - r / (w * 0.55), 0, 1)
        eye = sdf.smoothstep(5, 14, r)
        cyclone = np.clip((swirl * 0.75 + tex * 0.45 - 0.5) * 3.0, 0, 1) * fall ** 0.6 * eye
        front = np.clip((raster.fbm(w, h, 96, 341, octaves=5, period=False) - 0.62) * 2.6, 0, 1)
        front *= np.clip((np.sin(yy / h * 5.0 + xx / w * 2.0) + 0.2), 0, 1)
        c = np.clip(np.maximum(cyclone, front), 0, 0.92)[..., None]
        cloud = np.array(sea[4]) * 0.55 + np.array(sea[5]) * 0.2
        e[..., :3] = e[..., :3] * (1 - c) + cloud * c
        sh = np.roll(np.roll(c, 4, axis=0), 4, axis=1)
        e[..., :3] *= (1 - 0.3 * sh * (1 - c))
        return posterize(Image.fromarray(np.clip(e, 0, 255).astype(np.uint8), "RGBA"), 16)

    def layer_images(self, f):
        L = super().layer_images(f)
        decks = scroll_tex(self.decks, self.s.off("low-air", f))
        L = decorate(L, decks, "ground", self._casters, bank_shadow_opacity=0.4)
        high = scroll_tex(self.wisps, self.s.off("high-air", f))
        high.alpha_composite(L["high-air"])
        L["high-air"] = high
        return L


# --------------------------------------------------------------------------- scene B: city

class CityScene(r02.CityScene):
    title = "NIGHT MEGACITY - TREES AND FOG"
    notes = dict(r02.CityScene.notes, **{"ground": "X1.0 TOWERS, TREES, GARDENS",
                                         "low-air": "X1.35 LAMP-LIT FOG, TRAFFIC"})

    def __init__(self):
        g = B["EARTH SURFACE"]
        self.trees = [foliage_sprite(s, 400 + i, g, night=0.62)
                      for i, s in enumerate((12, 14, 16, 13))]
        super().__init__()
        track_casters(self)
        per = self.s.period("low-air")                        # 756 = 108 * 7
        n = stretched(W, per, [108, 54, 27], 401, 2)          # rolling banks, wider than tall
        xx = np.arange(W)[None, :]
        lamp = sum(np.exp(-((xx - a) / 34.0) ** 2) for a in self.AVENUES) * np.ones((per, 1))
        lamp *= (0.75 + 0.25 * noise(W, per, [27, 9], 411))
        pal = self.pal
        greyish = [tuple(int(c * 0.6 + g) for c in col) for col, g in
                   ((pal[1], 34), (pal[2], 46), (pal[3], 52))]   # fog glows above the dark city
        fog = bank(n, 0.62, 0.14, greyish, max_alpha=150, light=3.0,
                   glow=(np.array(r02.rgb("JOVIAN", 3)) * 0.4, lamp))
        self.fog = step_alpha(posterize(fog, 12))

    def _street_tex(self):
        """r02 streets plus tree rows along both kerbs of each avenue."""
        tex = super()._street_tex()
        per = self.TRAVEL
        rng = np.random.default_rng(420)
        for a in self.AVENUES:
            for side in (-1, 1):
                x = a + side * (self.AVE_W / 2 + 5)
                for y in range(4, per, 17):
                    if (y % self.BLOCK) < self.STREET_W + 4:
                        continue                              # keep the crossings open
                    t = self.trees[int(rng.integers(len(self.trees)))]
                    for yy in (y - per, y, y + per):
                        sprite.paste_center(tex, t, x + rng.uniform(-1, 1), yy)
        return tex

    def _buildings(self):
        out = super()._buildings()
        rng = np.random.default_rng(430)
        for b in out:
            b["garden"] = (not b["park"]) and rng.random() < 0.3 and not b["turret"]
            if not b["park"] and not b["turret"] and rng.random() < 0.08:
                b["park"], b["h"], b["beacon"] = True, 0.0, False   # a few more parks
        return sorted(out, key=lambda o: o["h"])

    def _draw_building(self, d, bld, x0, y0, x1, y1, ss, turrets):
        if bld["park"]:
            self._draw_park(d, bld, x0, y0, x1, y1, ss)
            return
        super()._draw_building(d, bld, x0, y0, x1, y1, ss, turrets)
        if bld.get("garden"):
            (rx0, ry0), (rx1, ry1) = (self._project(x0, y0, bld["h"]),
                                      self._project(x1, y1, bld["h"]))
            if rx1 - rx0 < 16 or ry1 - ry0 < 16:
                return
            g = B["EARTH SURFACE"]
            rng = np.random.default_rng(bld["seed"] + 1)
            gx0, gy0, gx1, gy1 = rx0 + 5, ry0 + 5, rx1 - 5, ry1 - 5
            d.rectangle([gx0 * ss, gy0 * ss, gx1 * ss, gy1 * ss],
                        fill=tuple(int(c * 0.45) for c in g[2]))
            for _ in range(int((gx1 - gx0) * (gy1 - gy0) / 40)):
                tx, ty = rng.uniform(gx0 + 1, gx1 - 1), rng.uniform(gy0 + 1, gy1 - 1)
                d.ellipse([(tx - 1.6) * ss, (ty - 1.6) * ss, (tx + 1.6) * ss, (ty + 1.6) * ss],
                          fill=tuple(int(c * 0.55) for c in g[3]))
                d.point([((tx - 0.8) * ss, (ty - 0.8) * ss)],
                        fill=tuple(int(c * 0.7) for c in g[4]))

    def _draw_park(self, d, bld, x0, y0, x1, y1, ss):
        g = B["EARTH SURFACE"]
        rng = np.random.default_rng(bld["seed"])
        d.rectangle([x0 * ss, y0 * ss, x1 * ss, y1 * ss], fill=tuple(int(c * 0.5) for c in g[1]))
        mx = (x0 + x1) / 2 + rng.uniform(-6, 6)                  # a path through the park
        d.line([mx * ss, y0 * ss, mx * ss, y1 * ss], fill=self.pal[1], width=3 * ss // 2)
        if x1 - x0 > 40 and rng.random() < 0.5:                  # a small pond
            px, py = rng.uniform(x0 + 10, x1 - 10), rng.uniform(y0 + 10, y1 - 10)
            d.ellipse([(px - 7) * ss, (py - 5) * ss, (px + 7) * ss, (py + 5) * ss],
                      fill=tuple(int(c * 0.6) for c in B["EARTH ORBIT"][1]))
        for _ in range(int((x1 - x0) * (y1 - y0) / 55)):
            t = self.trees[int(rng.integers(len(self.trees)))]
            tx, ty = rng.uniform(x0 + 5, x1 - 5), rng.uniform(y0 + 5, y1 - 5)
            if abs(tx - mx) < 5:
                continue
            self._pending_trees.append((t, tx, ty))

    def ground_image(self, f, turrets_out=None):
        self._pending_trees = []
        img = super().ground_image(f, turrets_out)
        for t, x, y in self._pending_trees:                     # park trees drawn at 1x
            sprite.paste_center(img, t, x, y)
        return img

    def layer_images(self, f):
        L = super().layer_images(f)
        fog = scroll_tex(self.fog, self.s.off("low-air", f))
        return decorate(L, fog, "ground", self._casters, bank_shadow_opacity=0.25,
                        flyer_opacity=0.3)


# --------------------------------------------------------------------------- scene C: canyon

class CanyonScene(r02.CanyonScene):
    title = "MARS CANYON - DUST STORM, GREENHOUSES"
    notes = dict(r02.CanyonScene.notes, **{"ground": "X1.0 COLONY, GREENHOUSES, LICHEN",
                                           "low-air": "X1.35 DUST-STORM BANKS",
                                           "high-air": "X2.2 DUST STREAKS, VEIL"})
    OUTPOST = r02.CanyonScene.OUTPOST + [("greenhouse", 240, 60), ("greenhouse", 240, 84),
                                         ("greenhouse", 240, 108), ("greenhouse", 530, -60),
                                         ("greenhouse", 530, -84)]

    def __init__(self):
        super().__init__()
        track_casters(self)
        m = self.m
        self.parts["greenhouse"] = greenhouse_sprite(B)
        self.plateau = posterize(self.plateau, 12)    # calmer plateau leaves GIF colours for the
        self._farm()                                  # colony, greenhouses and lichen
        per = self.s.period("low-air")                        # 864 = 96 * 9
        n = stretched(W, per, [96, 48, 24, 12], 501, 3)
        ochre = (130, 92, 70)                   # storm dust: m-ramp mixed towards dusty ochre
        tones = [tuple(int(c * (1 - k) + o * k) for c, o in zip(col, ochre)) for col, k in
                 ((m[1], 0.2), (m[2], 0.3), (m[3], 0.4), (m[4], 0.5))]
        tones = [tuple(int(c * f_) for c in t) for t, f_ in zip(tones, (0.7, 0.8, 0.85, 0.85))]
        dust = bank(n, 0.66, 0.1, tones, max_alpha=215, light=4.0)
        self.dust = step_alpha(posterize(dust, 12))
        hper = self.s.period("high-air")                      # 1408 = 88 * 16
        v = stretched(W, hper, [88, 44, 22, 11], 511, 4)
        a = np.clip((v - 0.58) * 2.6, 0, 1) * 90             # veil, <= ~35 % opacity
        self.veil = step_alpha(raster.to_rgba_image(
            np.broadcast_to(np.array(m[4], float) * 0.9, a.shape + (3,)), a), 10)
        rng = np.random.default_rng(521)
        self.streaks = [(rng.uniform(0, W), rng.uniform(0, hper), rng.uniform(0.4, 1.0))
                        for _ in range(40)]

    def _farm(self):
        """Paint lichen fields and algae ponds into the plateau texture around the outpost.
        Texture row = world y, so the fields scroll with the colony."""
        tex = np.array(self.plateau).astype(np.float64)
        per = tex.shape[0]
        g = B["EARTH SURFACE"]
        lichen = np.array(g[2]) * 0.7 + np.array(self.m[1]) * 0.3     # dark olive-green: hue
        lichen_hi = np.array(g[3]) * 0.6 + np.array(self.m[2]) * 0.4  # contrast, low value
        n = noise(W, per, [40, 20, 10, 5], 531)
        fine = noise(W, per, [8, 4, 2], 541)
        yy, xx = np.mgrid[0:per, 0:W]
        off = self._off_canyon(xx, yy, 6)
        for yw, dx, r in ((190, 90, 95), (500, -85, 80), (330, 70, 45)):
            cx = self._beside(yw, dx)
            dy = np.minimum.reduce([np.abs(yy - yw), np.abs(yy - yw - per),
                                    np.abs(yy - yw + per)])
            d = np.hypot((xx - cx) * 1.2, dy) / r
            val = n * (1.3 - d) + 0.3 * (fine - 0.5)            # ragged, speckled edges
            wgt = np.clip((val - 0.36) * 4, 0, 1) * 0.75 * off
            hi = np.clip((val - 0.55) * 4, 0, 1) * fine * 0.8 * off
            tex[..., :3] = tex[..., :3] * (1 - wgt[..., None]) + lichen * wgt[..., None]
            tex[..., :3] = tex[..., :3] * (1 - hi[..., None]) + lichen_hi * hi[..., None]
        img = Image.fromarray(np.clip(tex, 0, 255).astype(np.uint8), "RGBA")
        d = ImageDraw.Draw(img)
        for yw, dx in ((140, 150), (160, 150), (180, 150), (460, -140), (480, -140)):
            cx = self._beside(yw, dx)
            for k in range(3):
                x0 = cx - 24 + k * 17
                d.rounded_rectangle([x0, yw - 6, x0 + 14, yw + 6], radius=3,
                                    fill=tuple(int(c * 0.5) for c in g[2]),
                                    outline=tuple(int(c * 0.8) for c in B["UTC HULL"][3]))
                d.line([x0 + 3, yw - 3, x0 + 8, yw - 3], fill=tuple(int(c * 0.7) for c in g[3]))
        self.plateau = img
        # crops of the farm area, so the GIF palette keeps the lichen greens (see key_sprites)
        self.palette_extras = []
        for yw, dx in ((190, 90), (500, -85)):
            cx = int(self._beside(yw, dx))
            box = (max(0, cx - 60), max(0, yw - 60), min(W, cx + 60), min(per, yw + 60))
            self.palette_extras.append(img.crop(box))
    def _off_canyon(self, xx, yy, margin):
        c = self.centre(yy)
        return np.abs(xx - c) > self.half(yy) + margin

    def layer_images(self, f):
        L = super().layer_images(f)
        dust = scroll_tex(self.dust, self.s.off("low-air", f))
        L = decorate(L, dust, "ground", self._casters, bank_shadow_opacity=0.35)
        high = scroll_tex(self.veil, self.s.off("high-air", f))
        d = ImageDraw.Draw(high)
        step = self.FACTORS["high-air"] * self.TRAVEL / N
        for x, y, b in self.streaks:
            for yy in self.s.ys("high-air", y, f, margin=60):
                d.line([x, yy - step * (1 + b), x + 3 * b, yy], fill=self.m[5] + (int(40 + 60 * b),),
                       width=2)
        L["high-air"] = high
        return L


N = r02.N
SCENES = {"a": OrbitScene, "b": CityScene, "c": CanyonScene}
_cache = {}


def get_scene(key):
    if key not in _cache:
        _cache[key] = SCENES[key]()
    return _cache[key]


# --------------------------------------------------------------------------- outputs

def compose(scene, f):
    L = scene.layer_images(f)
    order = [k for k in ("deep", "far", "ground", "shadows", "low-air", "air", "high-air")
             if k in L]
    if isinstance(scene, r02.CanyonScene):
        order.remove("far")                    # the canyon's ground image already holds it
    img = L[order[0]].copy()
    for k in order[1:]:
        img.alpha_composite(L[k])
    return img


def make_sheet(scene, f=24):
    frame_img = compose(scene, f)
    layers = scene.layer_images(f)
    tw, th = 192, 216
    img = raster.sheet(1140, 690, f"PARALLAX {scene.key.upper()} (R03): {scene.title}",
                       "CONCEPT ROUND 03 - DECORATION")
    speed = scene.TRAVEL * r02.FPS / N
    raster.draw_text(img, 16, 38, f"PLAY FIELD 480X540 AT 1X - GROUND SCROLLS {speed:.0f} PX/S",
                     raster.LABEL_DIM)
    img.alpha_composite(frame_img, (16, 50))
    raster.draw_text(img, 516, 38, "LAYER BREAKDOWN, BACK TO FRONT (0.4X)", raster.LABEL_DIM)
    for i, key in enumerate(scene.layers):
        x = 516 + (i % 3) * (tw + 12)
        y = 50 + (i // 3) * (th + 44)
        cell = r02.checker(tw, th)
        lay = layers[key]
        if key == "air":
            lay = layers["shadows"].copy()
            lay.alpha_composite(layers["air"])
        cell.alpha_composite(lay.resize((tw, th), Image.BOX))
        img.alpha_composite(cell, (x, y + 12))
        raster.draw_text(img, x, y, f"{i + 1}. {key.upper()}", raster.ACCENT)
        raster.draw_text(img, x, y + th + 16, scene.notes[key], raster.LABEL_DIM)
    notes = [
        "DENSE BANKS ON LOW-AIR (BELOW THE PLAY PLANE, NEVER OVER BULLETS); THIN WISPS AND STREAKS "
        "ON HIGH-AIR UNDER 40 % OPACITY.",
        "BANKS SHADOW THE GROUND (10,14); FLYERS SHADOW THE BANK TOPS (12,17) AS WELL AS THE "
        "GROUND (21,30).",
        "DECORATION STAYS BELOW ENEMIES AND BULLETS IN CONTRAST AND SATURATION.",
    ]
    y = 580
    for line in notes:
        for part in r02.wrap(line, 100):
            raster.draw_text(img, 516, y, part, raster.LABEL)
            y += 11
        y += 5
    return img


# Colours that must survive GIF quantisation exactly: bullets, shots, engine flare, Vrell glow.
RESERVED = (list(B["ENEMY SHOTS"]) + [B["PLAYER SHOTS"][i] for i in (2, 3, 4)]
            + [B["VRELL GLOW"][i] for i in (2, 4)])


def key_sprites(scene):
    """Small but important sprites whose colours must get a share of the GIF palette."""
    a = scene.actors
    out = list(a.player.values()) + [a.dart, a.orb, a.needle, a.bolt]
    if a.brood is not None:
        out.append(a.brood)
    for name in ("parts",):
        out += list(getattr(scene, name, {}).values())
    for name in ("pod", "turret"):
        if hasattr(scene, name):
            out.append(getattr(scene, name))
    if hasattr(scene, "pods"):
        out.append(scene.pods[0])
    out += getattr(scene, "trees", [])
    out += getattr(scene, "palette_extras", [])
    return out


def gif_palette(frames, colors, extras=()):
    """Adaptive palette from sampled frames plus the RESERVED colours, as a 16x16 image that
    ffmpeg's paletteuse accepts. ``extras`` (key sprites) are tiled into the sample so their
    colours are not squeezed out by the large background areas."""
    tile = Image.new("RGB", (W, H), (0, 0, 0))
    x = y = row = 0
    while extras and y < H:
        for spr in extras:
            if x + spr.width > W:
                x, y, row = 0, y + row, 0
            if y + spr.height > H:
                break
            tile.paste(spr.convert("RGB"), (x, y), spr)
            x, row = x + spr.width, max(row, spr.height)
        else:
            continue
        break
    frames = list(frames) + ([tile] * max(1, len(frames) // 3) if extras else [])
    sample = Image.new("RGB", (W, H * len(frames)))
    for i, fr in enumerate(frames):
        sample.paste(fr.convert("RGB"), (0, i * H))
    adaptive = sample.quantize(colors=colors - len(RESERVED), method=Image.Quantize.MEDIANCUT,
                               dither=Image.Dither.NONE).getpalette()[:3 * (colors -
                                                                            len(RESERVED))]
    cols = [tuple(adaptive[i:i + 3]) for i in range(0, len(adaptive), 3)] + list(RESERVED)
    cols += [cols[-1]] * (256 - len(cols))
    pal = Image.new("RGB", (16, 16))
    pal.putdata(cols)
    return pal


GIF_COLORS = {"a": 72, "b": 128, "c": 84}     # per scene, to stay under ~8 MB


def make_gif(scene, path, colors=80):
    """4 s loop with a small palette (the decoration adds a moving layer; ~80 colours keep the
    loops under ~8 MB) in which the bullet colours are reserved so they never shift."""
    with tempfile.TemporaryDirectory() as tmp:
        frames = []
        for f in range(N):
            img = compose(scene, f)
            img.convert("RGB").save(Path(tmp) / f"f{f:03d}.png")
            if f % 8 == 0:
                frames.append(img)
        gif_palette(frames, colors, key_sprites(scene)).save(Path(tmp) / "palette.png")
        cmd = ["ffmpeg", "-v", "error", "-y", "-framerate", str(r02.FPS),
               "-i", str(Path(tmp) / "f%03d.png"), "-i", str(Path(tmp) / "palette.png"),
               "-lavfi", "[0][1]paletteuse=dither=none:diff_mode=rectangle",
               "-loop", "0", str(path)]
        subprocess.run(cmd, check=True)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for key in sys.argv[1:] or list(SCENES):
        scene = get_scene(key)
        png = OUT / f"parallax-r03-{key}.png"
        make_sheet(scene).convert("RGB").save(png, optimize=True)
        print("wrote", png.relative_to(ROOT))
        gif = OUT / f"parallax-r03-{key}.gif"
        make_gif(scene, gif, colors=GIF_COLORS[key])
        print("wrote", gif.relative_to(ROOT), f"{gif.stat().st_size / 1e6:.1f} MB")


if __name__ == "__main__":
    main()
