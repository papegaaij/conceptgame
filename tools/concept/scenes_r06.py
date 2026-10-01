#!/usr/bin/env python3
"""Concept round 06 - parallax scenes for five more settings, at MEDIUM atmosphere intensity.

Outputs (design/art-direction/concept/):
  scene-luna-r06-a.png / .gif        Luna: regolith with long low-sun shadows, Tranquility Base
                                     domes on the convoy road, the mass-driver rail with a sled
                                     shot, Vrell roots spreading from a nest; regolith plumes
  scene-europa-r06-a.png / .gif      Europa under water: dark sea floor of Thera Deep (domes,
                                     tube corridors, kelp farms, a black smoker), Vrell coral
                                     reefs; the sub layer with silt banks, fish and submerged
                                     Vrell; light shafts through the ice; the ship's headlight
  scene-belt-r06-a.png / .gif        Asteroid belt: a large rock with Coalition modules, a
                                     glowing refinery pit, conveyor tubes and a black-and-gold
                                     Helix block; tumbling rocks at four depths; dust banks
  scene-jovian-r06-a.png / .gif      Jupiter: Aurelia decks, spires and balloons over towering
                                     cloud bands, a storm vortex below, wisps, one lightning flash
  scene-vrell-space-r06-a.png / .gif Vrell space: hive-reef surface with chitin plates, glowing
                                     veins and spawning pits over a violet/teal nebula; spore
                                     sacs, tendrils and spore banks; a Coilwyrm orbiting

Atmosphere intensity follows design/art-direction ("Decoration"): round 03 showed the heavy end;
these scenes show MEDIUM (~20-25 % bank coverage on low-air / sub, wisps on high-air < 40 %).
Each PNG shows the play field at 1x plus the layer breakdown; each GIF is a seamless 4 s loop
(80 frames at 20 fps) with the bullet colours reserved in its palette. Builds on
parallax_r02 / parallax_r03 (helpers), enemies_r04 / enemies_r05 (chosen enemies in role
colours) and render/scene_models.py (Driftjelly, Spiral Nautilus, Aurelia parts, rocks).
Run: python3 tools/concept/scenes_r06.py [luna] [europa] [belt] [jovian] [vrell-space]
"""
import subprocess
import sys
import tempfile
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, str(Path(__file__).resolve().parent))
import enemies_r03 as e3  # noqa: E402
import enemies_r04 as e4  # noqa: E402  (role-colour models, rim light, yellow thorns)
import parallax_r02 as r02  # noqa: E402
import parallax_r03 as r03  # noqa: E402
from parallax_r02 import H, W, crossfade, masked, posterize, scroll_tex  # noqa: E402
from render import archetype_models as am  # noqa: E402
from render import enemy_rigs as rig  # noqa: E402
from render import raster, scene_models as sm, sdf, sprite, station  # noqa: E402
from render.config import ROOT  # noqa: E402
from render.palette import B  # noqa: E402

OUT = ROOT / "design" / "art-direction" / "concept"
N, FPS = r02.N, r02.FPS
TAU = 2 * np.pi
SHADOW = {"ground": (6, 8), "low-air": (9, 13), "air": (21, 30), "sub": (12, 17)}
SHOTS = B["ENEMY SHOTS"]
GOLD = B["ASCEND. GOLD"]


# --------------------------------------------------------------------------- helpers

def periodic_dist(a, b, per):
    d = np.abs(a - b) % per
    return np.minimum(d, per - d)


def hillshade(hgt, strength=1.0):
    """Shade a height field lit from the top-left (gradient wraps vertically)."""
    gy = (np.roll(hgt, -1, axis=0) - np.roll(hgt, 1, axis=0)) / 2
    gx = np.gradient(hgt, axis=1)
    return np.clip(1 - (gx + gy) * strength, 0.3, 1.6)


def cast_shadows(hgt, steps=26, drop=0.035):
    """Long low-sun shadows: a pixel is shadowed if terrain towards the top-left (the light)
    rises above a ray that drops ``drop`` per pixel. Periodic in y, clamped in x."""
    lit = np.ones_like(hgt)
    for k in range(1, steps + 1):
        src = np.roll(hgt, k, axis=0)                 # k px up
        src = np.concatenate([np.repeat(src[:, :1], k, axis=1), src[:, :-k]], axis=1)  # k px left
        lit = np.minimum(lit, np.clip(1 - (src - hgt - k * drop) * 25, 0, 1))
    return lit


def ptile(img_or_arr):
    """Stack a vertically periodic texture three times (for wrap-safe filtering)."""
    if isinstance(img_or_arr, Image.Image):
        out = Image.new(img_or_arr.mode, (img_or_arr.width, img_or_arr.height * 3))
        for k in range(3):
            out.paste(img_or_arr, (0, k * img_or_arr.height))
        return out
    return np.concatenate([img_or_arr] * 3, axis=0)


def pmid(img, per):
    return img.crop((0, per, img.width, 2 * per))


def pblur_mask(mask, radius):
    """Gaussian blur of a 0..1 mask that wraps vertically."""
    per = mask.shape[0]
    im = Image.fromarray((ptile(mask) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(radius))
    return np.array(pmid(im, per), dtype=np.float64) / 255


def pshadow(img, opacity, blur):
    """Drop-shadow image of a vertically periodic texture (wraps)."""
    per = img.height
    return pmid(sprite.shadow_of(ptile(img), opacity=opacity, blur=blur), per)


def smooth_tex(img, size=5):
    """Mode filter (wraps vertically): removes single-pixel speckle from a posterized terrain
    texture while keeping its forms. Speckle on a scrolling layer is what bloats the GIFs."""
    return pmid(ptile(img).filter(ImageFilter.ModeFilter(size)), img.height)


def coverage(img):
    a = np.array(img)[..., 3]
    return float((a > 40).mean())


def bank_tex(per, cells, seed, cover, soft, tones, max_alpha=220, stretch=1, light=5.0,
             colors=12, levels=6, glow=None):
    """Periodic low-air / sub bank (cloud, dust, silt) texture W x per."""
    assert all(per % c == 0 for c in cells), f"noise cells {cells} must divide {per}"
    if stretch > 1:
        n = r03.stretched(W, per, cells, seed, stretch)
    else:
        n = r03.noise(W, per, cells, seed, gains=[1.0, 0.5, 0.3, 0.15][:len(cells)])
    if glow is not None:
        glow = (glow[0], ptile(glow[1]))
    b = pmid(r03.bank(ptile(n), cover, soft, tones, max_alpha=max_alpha, light=light, glow=glow), per)
    return r03.step_alpha(posterize(b, colors), levels)


def wisp_tex(per, cells, seed, color, thresh=0.6, gain=3.0, max_a=92, stretch=1):
    """Thin high-air wisps / veils (<= ~37 % opacity), periodic."""
    assert all(per % c == 0 for c in cells), f"noise cells {cells} must divide {per}"
    n = r03.stretched(W, per, cells, seed, stretch) if stretch > 1 else r03.noise(W, per, cells, seed)
    a = np.clip((n - thresh) * gain, 0, 1) ** 1.3 * max_a
    return r03.step_alpha(raster.to_rgba_image(
        np.broadcast_to(np.array(color, float), a.shape + (3,)), a), 10)


def tinted(img, color, amount, darken=1.0):
    a = np.array(img).astype(np.float64)
    a[..., :3] = (a[..., :3] * (1 - amount) + np.array(color) * amount) * darken
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


def paste_shadowed(img, sh_img, sp, x, y, layer, opacity=0.5):
    """Paste ``sp`` on ``img`` and its drop shadow on ``sh_img`` (layer offsets)."""
    if sh_img is not None and layer in SHADOW:
        dx, dy = SHADOW[layer]
        sprite.paste_center(sh_img, sprite.shadow_of(sp, opacity=opacity, blur=1.5,
                                                     scale=0.85 if layer != "ground" else 1.0),
                            x + dx, y + dy)
    sprite.paste_center(img, sp, x, y)


def ramp_img(stops, t):
    return raster.ramp(stops, np.clip(t, 0, 1))


def lerp_c(a, b, t):
    return tuple(int(x + (y - x) * t) for x, y in zip(a, b))


# --------------------------------------------------------------------------- shared cast

class Cast:
    """Play-plane actors shared by all scenes: the player (ship A, palette B) with its twin
    bolts, plus the round 04 / 05 enemy sprites the scenes pick from (lazily rendered)."""

    def __init__(self):
        self.base = r02.Actors([])           # player banking frames, bolts, flare
        self._u = {}

    def unit(self, slug, anim=0.0, glow=1.0):
        key = (slug, round(anim, 2), round(glow, 2))
        if key not in self._u:
            e = e4.R04[slug]
            self._u[key] = e3.render(e[4], e[3], anim, glow)[1]
        return self._u[key]

    def player(self, img, casters, f):
        a = self.base
        px, py, vx = a.player_pos(f % N)
        for k in range(10):                      # a pair every 4 frames; period 40 divides N
            age = (f + 4 * k) % 40
            ox, _, _ = a.player_pos((f - age) % N)
            y = py - 20 - age * 16
            sprite.paste_center(img, a.bolt, ox - 9, y)
            sprite.paste_center(img, a.bolt, ox + 9, y)
        frame = "r" if vx > 0.5 else "l" if vx < -0.5 else "c"
        sp = a.player[frame]
        casters.append((sp, px, py))
        sprite.paste_center(img, sp, px, py)
        sprite.paste_center(img, a.flare, px, py + 24 + (f % 4 == 0))
        return px, py

    def player_pos(self, f):
        return self.base.player_pos(f % N)


def aimed(img, f, src, cast, period=40, speed=7.0, kind="orb", phase=0, count=2, life=36):
    """Bullets aimed at the player from a source ``src(ff) -> (x, y) or None``, fired every
    ``period`` frames (loop-safe: N is a multiple of ``period``)."""
    for k in range(count):
        age = (f + phase - k * period) % N
        if age > life:
            continue
        s = src(f - age)
        if s is None:
            continue
        sx, sy = s
        tx, ty, _ = cast.player_pos(f - age)
        dx, dy = tx - sx, ty - sy
        n = np.hypot(dx, dy) or 1
        x, y = sx + dx / n * age * speed, sy + dy / n * age * speed
        if kind == "orb":
            e3.orb(img, x, y, 5)
        elif kind == "big":
            e3.orb(img, x, y, 6)
        elif kind == "thorn":
            e4.thorn(img, x, y, np.arctan2(dx, dy))
        elif kind == "acid":
            e3.orb(img, x, y, 5, ring=SHOTS[5])
        elif kind == "gold":
            e3.orb(img, x, y, 4, ring=GOLD[3])


# --------------------------------------------------------------------------- base scene

class Scene:
    slug = key = ""
    title = ""
    layers = []
    notes = {}
    TRAVEL = 600
    FACTORS = {}
    GIF_COLORS = 96
    ORDER = ("deep", "far", "ground", "shadows", "sub", "low-air", "air", "high-air")

    def __init__(self):
        self.s = r02.Scroller(self.TRAVEL, self.FACTORS)
        self.cast = Cast()

    def ys(self, layer, y, f, margin=120):
        return self.s.ys(layer, y, f, margin)

    def compose(self, f):
        f %= N                       # exact phase: frame N is frame 0 (no float drift)
        L = self.layer_images(f)
        keys = [k for k in self.ORDER if k in L]
        img = L[keys[0]].copy()
        for k in keys[1:]:
            img.alpha_composite(L[k])
        return self.post(img, f)

    def post(self, img, f):
        return img

    EXTRA_BOXES = []             # ground-texture regions whose colours must survive the GIF

    def palette_extras(self):
        tex = getattr(self, "ground_tex", None) or getattr(self, "floor", None)
        return [tex.crop(b) for b in self.EXTRA_BOXES] if tex is not None else []


# --------------------------------------------------------------------------- shared sprites

_spr = {}


def shared(name):
    """Lazily built sprites/rigs shared between scenes."""
    if name not in _spr:
        if name == "scuttler":
            _spr[name] = rig.ModelSpaceAngleSprites(lambda phase=0.0: am.scuttler(phase), 64, 16,
                                                    factor=5)
        elif name == "seed":
            _spr[name] = rig.AngleSprites(lambda: am.whirl_seed(), 26, 40, factor=8, sym=5)
        elif name == "saw":
            _spr[name] = rig.AngleSprites(lambda: am.buzzsaw(), 42, 48, factor=6, colors=32, sym=6,
                                          post=e4.rim)
        elif name == "jelly":
            _spr[name] = rig.ModelSpaceAngleSprites(lambda pulse=0.0: sm.driftjelly(pulse), 40, 16,
                                                    factor=6, colors=28)
        elif name == "nautilus":
            _spr[name] = rig.ModelSpaceAngleSprites(lambda uncoil=0.0: sm.spiral_nautilus(uncoil),
                                                    56, 16, factor=6, colors=32)
        elif name == "coil":
            sizes = [58] + [54, 52, 50, 48, 46, 44, 42, 39, 36, 33, 30, 27] + [32]
            _spr[name] = dict(
                sizes=sizes,
                head=rig.ModelSpaceAngleSprites(lambda jaw=0.0: am.coil_head(jaw), sizes[0], 16,
                                                colors=32),
                segs={n: rig.ModelSpaceAngleSprites(lambda: am.coil_segment(), n, 16)
                      for n in set(sizes[1:-1])},
                tail=rig.ModelSpaceAngleSprites(lambda: am.coil_tail(), sizes[-1], 16))
        elif name == "spine":
            _spr[name] = e3.render(e4.R04["spine-turret-a"][4], 36)[1]
        elif name == "polyp":
            _spr[name] = e3.render(e4.R04["polyp-mortar-a"][4], 36)[1]
    return _spr[name]


def walk_sprite(heading, dist, phases=6):
    k = int(round(dist / 30.0 * phases)) % phases
    return shared("scuttler").get(heading, phase=k * TAU / phases)


def skitter_snake(img, casters, cast, f, x0, amp, delay, count=5, gap=0.045, span=760):
    for i in range(count):
        t = ((f / N) + delay - i * gap) % 1.0
        x = x0 + amp * np.sin(TAU * 1.5 * t)
        y = -70 + span * t
        if -40 < y < H + 40:
            sp = cast.unit("skitter-a", anim=[-1.0, 0.0, 1.0, 0.0][(f // 2 + i) % 4])
            casters.append((sp, x, y))
            sprite.paste_center(img, sp, x, y)


def seed_cluster(img, casters, f, cx, cy, ph=0.0, r0=30):
    r = r0 + 12 * np.sin(TAU * 2 * f / N + ph)
    for k in range(5):
        a = TAU * f / N * 2 + ph + k * TAU / 5
        x, y = cx + r * np.cos(a), cy + r * np.sin(a)
        sp = shared("seed").get(TAU * 4 * f / N + k)
        casters.append((sp, x, y))
        sprite.paste_center(img, sp, x, y)


# --------------------------------------------------------------------------- A: Luna

class LunaScene(Scene):
    slug, key = "luna", "luna"
    title = "LUNA - TRANQUILITY BASE, MASS DRIVER, VRELL ROOTS"
    layers = ["ground", "low-air", "air", "high-air"]
    notes = {"ground": "X1.0 REGOLITH, BASE, RAIL, ROOTS", "low-air": "X1.35 REGOLITH PLUMES",
             "air": "PLAY PLANE", "high-air": "X2.2 EJECTED ROCK"}
    TRAVEL = 600                 # 150 px/s: a normal-paced level
    FACTORS = {"ground": 1.0, "low-air": 1.35, "high-air": 2.2}
    GIF_COLORS = 112
    EXTRA_BOXES = [(20, 420, 140, 540), (240, 100, 380, 250), (380, 0, 440, 120)]
    RAIL_X = 412
    GREYS = [(0.0, (16, 17, 22)), (0.3, (58, 58, 64)), (0.6, (112, 111, 116)),
             (0.85, (168, 166, 170)), (1.0, (206, 204, 206))]

    def __init__(self):
        super().__init__()
        per = self.TRAVEL
        self.turrets = [(96, 300), (170, 520)]          # world positions on the roots
        self.ground_tex = self._ground(per)
        dust_tones = [(70, 68, 70), (104, 101, 102), (140, 136, 134), (176, 172, 168)]
        self.plumes = bank_tex(self.s.period("low-air"), [162, 81, 27], 601, 0.64, 0.12,
                               dust_tones, max_alpha=205, light=7.0)
        hper = self.s.period("high-air")
        rng = np.random.default_rng(611)
        rocks = sm.rock_sprites(22, 612, self.GREYS[1:], rotations=8)
        self.ejecta = [(r02.alpha_scale(rk.resize((rk.width * 2, rk.height * 2), Image.BILINEAR)
                                            .filter(ImageFilter.GaussianBlur(2.2)), 0.36),
                        rng.uniform(40, W - 40), rng.uniform(0, hper)) for rk in rocks[:3]]

    # ---- ground texture (static, periodic)
    def road_x(self, y):
        t = TAU * y / self.TRAVEL
        return 228 + 70 * np.sin(t) + 18 * np.sin(2 * t + 1.2)

    def _ground(self, per):
        rng = np.random.default_rng(621)
        hgt = r02.periodic_fbm(W, per, 120, 622, octaves=5) * 0.5
        hgt += r02.periodic_fbm(W, per, 24, 623, octaves=1) * 0.05
        yy, xx = np.mgrid[0:per, 0:W]
        craters = [(rng.uniform(0, W), rng.uniform(0, per), rng.uniform(5, 22)) for _ in range(34)]
        craters += [(70, 470, 64), (330, 80, 44), (430, 330, 30)]     # the nest crater + two big
        for cx, cy, r in craters:
            d = np.hypot(xx - cx, periodic_dist(yy, cy, per)) / r
            bowl = np.where(d < 1, -(1 - d * d) * 0.55, 0)
            rim = np.exp(-((d - 1) / 0.16) ** 2) * 0.2
            hgt += (bowl + rim) * min(1.0, r / 26)
        # graded convoy road: flatten the terrain along it
        rx = self.road_x(yy)
        road = np.clip(1 - (np.abs(xx - rx) - 9) / 5, 0, 1)
        hgt = hgt * (1 - road) + (hgt * 0.3 + 0.1) * road
        shade = hillshade(hgt * 60, 0.55)
        lit = cast_shadows(hgt * 60, steps=28, drop=1.0)
        t = (hgt - hgt.min()) / (hgt.max() - hgt.min())
        col = ramp_img(self.GREYS, 0.25 + t * 0.6) * (shade * (0.35 + 0.65 * lit))[..., None]
        shadow_tint = np.array((14, 18, 34)) * (1 - lit)[..., None] * 0.9     # earthshine
        col = col + shadow_tint
        # road surface: compacted, darker, with two tyre tracks and orange edge posts
        col = col * (1 - 0.28 * road[..., None])
        for off in (-4, 4):
            tr = np.abs(xx - rx - off) < 1.0
            col[tr] *= 0.72
        img = smooth_tex(posterize(raster.to_rgba_image(np.clip(col * 0.8, 0, 255)), 9))
        img = self._roots(img, per)
        img = self._structures(img, per)
        return img

    def _roots(self, img, per):
        """Vrell roots spreading from the nest crater: plum chitin strands with teal cores."""
        ss = 2
        lay = Image.new("RGBA", (W * ss, per * ss), (0, 0, 0, 0))
        glow = Image.new("RGBA", (W * ss, per * ss), (0, 0, 0, 0))
        d, g = ImageDraw.Draw(lay), ImageDraw.Draw(glow)
        rng = np.random.default_rng(631)
        chitin = (122, 44, 122)
        teal = (0, 255, 154)

        def branch(x, y, ang, length, width, depth):
            pts = [(x, y)]
            for _ in range(int(length / 6)):
                ang += rng.uniform(-0.35, 0.35)
                x, y = x + 6 * np.cos(ang), y + 6 * np.sin(ang)
                pts.append((x, y))
            for dy in (-per, 0, per):
                q = [(px * ss, (py + dy) * ss) for px, py in pts]
                d.line(q, fill=chitin + (255,), width=int(width * ss))
                d.line(q, fill=tuple(int(c * 1.5) for c in chitin) + (255,),
                       width=max(1, int(width * 0.3 * ss)))
                g.line(q, fill=teal + (230,), width=max(2, int(width * 0.45 * ss)))
            if depth > 0:
                for _ in range(2):
                    k = int(rng.integers(len(pts) // 3, len(pts)))
                    branch(pts[k][0], pts[k][1], ang + rng.uniform(-1.0, 1.0), length * 0.6,
                           width * 0.65, depth - 1)
        for a in np.linspace(0, TAU, 7, endpoint=False):
            branch(70 + 30 * np.cos(a), 470 + 30 * np.sin(a), a + rng.uniform(-0.2, 0.2),
                   rng.uniform(90, 170), 5.0, 2)
        # nest mass in the crater: teal-black chitin mound with a glowing pit and egg pods
        for dy in (-per, 0, per):
            for r, c in ((34, (12, 48, 46)), (26, (24, 84, 80)), (14, (6, 26, 26))):
                d.ellipse([(70 - r) * ss, (470 + dy - r) * ss, (70 + r) * ss, (470 + dy + r) * ss],
                          fill=c + (255,))
            g.ellipse([(70 - 9) * ss, (470 + dy - 9) * ss, (70 + 9) * ss, (470 + dy + 9) * ss],
                      fill=teal + (255,))
            for a in np.linspace(0.3, TAU + 0.3, 6, endpoint=False):
                ex, ey = 70 + 24 * np.cos(a), 470 + dy + 24 * np.sin(a)
                d.ellipse([(ex - 6) * ss, (ey - 6) * ss, (ex + 6) * ss, (ey + 6) * ss],
                          fill=(40, 110, 104, 255), outline=(10, 30, 30, 255), width=ss)
                g.ellipse([(ex - 2) * ss, (ey - 2) * ss, (ex + 2) * ss, (ey + 2) * ss],
                          fill=teal + (255,))
        lay = lay.resize((W, per), Image.BOX)
        glow = glow.resize((W, per), Image.BOX)
        sh = pshadow(lay, 0.5, 1.0)
        out = img.copy()
        out.alpha_composite(sh, (3, 4))
        out.alpha_composite(lay)
        out.alpha_composite(pmid(raster.glow(ptile(glow), 2.2, 1.0), per))
        return out

    def _structures(self, img, per):
        """Tranquility Base (domes, truss, pad, cargo), the mass-driver rail, spine turrets."""
        p = station
        warm_glass = sm.PaletteShim(B, {("EARTH ORBIT", 3): (120, 150, 200)})
        dome = p.render_part(p.dome_part(24), warm_glass)
        dome_s = p.render_part(p.dome_part(16), warm_glass)
        truss = p.render_part(p.truss_h_part(64, 16), B)
        cargo = p.render_part(p.cargo_part(), B)
        cargo_b = p.render_part(p.cargo_part(p.SOLAR), B)
        radiator = p.render_part(p.radiator_part(56, 2), B)
        items = [(dome, 300, 150), (dome_s, 336, 214), (truss, 318, 182), (cargo, 262, 206),
                 (cargo_b, 262, 232), (radiator, 360, 118)]
        out = img.copy()
        # warm light pools around the habitats
        arr = out
        for x, y, r in ((300, 150, 46), (336, 214, 34), (290, 420, 30)):
            for dy in (-per, 0, per):
                arr = raster.add_light(arr, x, y + dy, r, (255, 190, 90), 0.32)
        out = arr
        d = ImageDraw.Draw(out)
        for dy in (-per, 0, per):             # landing pad
            cx, cy = 290, 420 + dy
            d.ellipse([cx - 26, cy - 26, cx + 26, cy + 26], fill=(70, 70, 76, 255),
                      outline=(150, 150, 156, 255))
            d.ellipse([cx - 20, cy - 20, cx + 20, cy + 20], outline=(255, 200, 60, 255), width=2)
            d.line([cx - 9, cy, cx + 9, cy], fill=(230, 230, 236, 255), width=3)
            d.line([cx, cy - 9, cx, cy + 9], fill=(230, 230, 236, 255), width=3)
        # orange marker posts along the road
        for y in range(0, per, 34):
            for side in (-1, 1):
                x = self.road_x(y) + side * 12
                for dy in (-per, 0, per):
                    d.rectangle([x - 1, y + dy - 1, x + 1, y + dy + 1], fill=(255, 122, 42, 255))
        for sp, x, y in items:
            for dy in (-per, 0, per):
                sh = sprite.shadow_of(sp, opacity=0.6, blur=1.0)
                out.alpha_composite(sh, (int(x - sp.width / 2 + 7), int(y + dy - sp.height / 2 + 9)))
                sprite.paste_center(out, sp, x, y + dy)
        # mass-driver rail: two rails, ties, pylons with hazard lights; shadow to the lower right
        rail = Image.new("RGBA", (W, per), (0, 0, 0, 0))
        rd = ImageDraw.Draw(rail)
        x0 = self.RAIL_X
        for y in range(0, per, 10):
            rd.rectangle([x0 - 13, y, x0 + 13, y + 3], fill=(84, 84, 92, 255))
        for dx in (-8, 8):
            rd.rectangle([x0 + dx - 2, 0, x0 + dx + 2, per], fill=(150, 152, 164, 255))
            rd.line([x0 + dx - 1, 0, x0 + dx - 1, per], fill=(214, 216, 226, 255))
        for y in range(20, per, 60):
            rd.rectangle([x0 - 20, y - 6, x0 + 20, y + 6], fill=(110, 112, 122, 255),
                         outline=(60, 60, 68, 255))
            rd.rectangle([x0 - 19, y - 5, x0 + 19, y - 3], fill=(170, 172, 182, 255))
        out.alpha_composite(pshadow(rail, 0.6, 0.8), (6, 8))
        out.alpha_composite(rail)
        spine = shared("spine")
        for x, y in self.turrets:
            for dy in (-per, 0, per):
                sh = sprite.shadow_of(spine, opacity=0.6, blur=1.0)
                out.alpha_composite(sh, (int(x - spine.width / 2 + 5), int(y + dy - spine.height / 2 + 7)))
                sprite.paste_center(out, spine, x, y + dy)
        return out

    def rail_lights(self, img, f):
        """Hazard lights on the pylons; they blink faster before the sled shot (frames 14-30),
        then the sled races up the track (frames 30-40)."""
        d = ImageDraw.Draw(img)
        x0 = self.RAIL_X
        warn = 14 <= f < 30
        on = (f // (2 if warn else 5)) % 2 == 0          # periods 4 / 10 divide N
        for y in range(20, self.TRAVEL, 60):
            for yy in self.ys("ground", y, f, margin=20):
                c = (255, 140, 30) if on else (110, 50, 20)
                for dx in (-17, 17):
                    d.rectangle([x0 + dx - 1, yy - 1, x0 + dx + 1, yy + 1], fill=c + (255,))
                if warn and on:
                    img = raster.add_light(img, x0, yy, 12, (255, 120, 30), 0.35)
        if 30 <= f < 40:
            sy = H + 40 - (f - 30) * 70
            sled = Image.new("RGBA", (W, H), (0, 0, 0, 0))
            sd = ImageDraw.Draw(sled)
            sd.rectangle([x0 - 10, sy + 20, x0 + 10, sy + 120], fill=(255, 210, 120, 70))
            sd.rectangle([x0 - 7, sy - 14, x0 + 7, sy + 14], fill=(230, 232, 240, 255),
                         outline=(90, 90, 100, 255))
            sd.rectangle([x0 - 5, sy + 8, x0 + 5, sy + 13], fill=(255, 150, 40, 255))
            img.alpha_composite(raster.glow(sled, 3.0, 0.8))
        return img

    def ground_image(self, f):
        img = scroll_tex(self.ground_tex, self.s.off("ground", f))
        img = self.rail_lights(img, f)
        # the Scuttler patrols an ellipse beside the road (one lap per loop, feet don't slide)
        t = TAU * f / N
        x, yw = 150 + 52 * np.cos(t), 330 + 34 * np.sin(t)
        heading = np.arctan2(34 * np.cos(t), -52 * np.sin(t))
        sp = walk_sprite(heading, 3.75 * f)      # 60 phase steps per loop: loops cleanly
        for yy in self.ys("ground", yw, f):
            sprite.paste_center(img, sprite.shadow_of(sp, opacity=0.55, blur=1.0), x + 5, yy + 7)
            sprite.paste_center(img, sp, x, yy)
        return img

    def turret_src(self, i):
        x, yw = self.turrets[i]

        def src(ff):
            y = (yw + self.s.off("ground", ff)) % self.TRAVEL
            return (x, y) if 0 <= y <= H else None
        return src

    def layer_images(self, f):
        ground = self.ground_image(f)
        air = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        casters = []
        skitter_snake(air, casters, self.cast, f, 300, 90, 0.0)
        hx = 150 + 40 * np.sin(TAU * f / N)
        hy = 130 + 14 * np.sin(2 * TAU * f / N)
        nd = self.cast.unit("needler-a", anim=[-1.0, 0.0, 1.0, 0.0][(f // 4) % 4])
        casters.append((nd, hx, hy))
        sprite.paste_center(air, nd, hx, hy)
        cx = -60 + 600 * ((f / N + 0.55) % 1.0)
        seed_cluster(air, casters, f, cx, 300 + 40 * np.sin(TAU * f / N))
        aimed(air, f, lambda ff: (150 + 40 * np.sin(TAU * ff / N), 130 + 14 * np.sin(2 * TAU * ff / N) + 14),
              self.cast, period=40, speed=6.5, kind="thorn", phase=10)
        aimed(air, f, self.turret_src(0), self.cast, period=40, speed=6.0, kind="thorn", phase=0)
        aimed(air, f, self.turret_src(1), self.cast, period=40, speed=6.0, kind="thorn", phase=20)
        self.cast.player(air, casters, f)
        shadows = r02.shadow_layer(casters, *SHADOW["air"], opacity=0.5)
        L = {"ground": ground, "shadows": shadows, "low-air": Image.new("RGBA", (W, H), (0, 0, 0, 0)),
             "air": air}
        plumes = scroll_tex(self.plumes, self.s.off("low-air", f))
        L = r03.decorate(L, plumes, "ground", casters, bank_shadow_opacity=0.32, flyer_opacity=0.35)
        high = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        for spr, x, y in self.ejecta:
            for yy in self.ys("high-air", y, f, margin=80):
                sprite.paste_center(high, spr, x, yy)
        L["high-air"] = high
        return L



# --------------------------------------------------------------------------- B: Europa under water

def branches(draw, glow_draw, rng, x, y, ang, length, width, depth, body, tip, ss, per,
             step=5, jitter=0.4, tips=True):
    """Organic branching growth (roots, coral) drawn on a 2x canvas, periodic in y."""
    pts = [(x, y)]
    for _ in range(max(2, int(length / step))):
        ang += rng.uniform(-jitter, jitter)
        x, y = x + step * np.cos(ang), y + step * np.sin(ang)
        pts.append((x, y))
    for dy in (-per, 0, per):
        q = [(px * ss, (py + dy) * ss) for px, py in pts]
        draw.line(q, fill=body + (255,), width=max(1, int(width * ss)))
        if tips and depth == 0:
            ex, ey = q[-1]
            r = max(2, int(width * 0.9 * ss))
            draw.ellipse([ex - r, ey - r, ex + r, ey + r], fill=body + (255,))
            glow_draw.ellipse([ex - r * 0.7, ey - r * 0.7, ex + r * 0.7, ey + r * 0.7],
                              fill=tip + (255,))
    if depth > 0:
        for _ in range(2 + (depth > 1)):
            k = int(rng.integers(len(pts) // 2, len(pts)))
            branches(draw, glow_draw, rng, pts[k][0], pts[k][1], ang + rng.uniform(-0.9, 0.9),
                     length * 0.62, width * 0.7, depth - 1, body, tip, ss, per, step, jitter,
                     tips)


class EuropaScene(Scene):
    slug, key = "europa", "europa"
    title = "EUROPA UNDER WATER - THERA DEEP"
    layers = ["ground", "sub", "air", "high-air"]
    notes = {"ground": "X1.0 SEA FLOOR, DOMES, KELP, REEF", "sub": "X1.2 SILT, FISH, NAUTILI, JELLIES",
             "air": "PLAY PLANE (IN WATER)", "high-air": "X2.0 LIGHT SHAFTS THROUGH THE ICE"}
    TRAVEL = 560                 # 140 px/s: calm, heavy water
    FACTORS = {"ground": 1.0, "sub": 1.2, "high-air": 2.0}
    GIF_COLORS = 112
    EXTRA_BOXES = [(260, 220, 460, 470), (40, 340, 170, 520), (360, 110, 430, 170)]
    FLOOR = [(0.0, (2, 7, 16)), (0.35, (6, 26, 44)), (0.65, (14, 52, 72)), (0.9, (36, 88, 104)),
             (1.0, (70, 124, 130))]
    WATER = (0, 40, 70)

    def __init__(self):
        super().__init__()
        per = self.TRAVEL
        self.vent = (395, 140)
        self.floor = self._floor(per)
        sper = self.s.period("sub")
        silt = [(4, 22, 34), (8, 40, 56), (14, 60, 76), (24, 84, 98)]
        self.silt = bank_tex(sper, [168, 84, 42, 21], 701, 0.68, 0.14, silt, max_alpha=190,
                             stretch=2, light=4.0)
        rng = np.random.default_rng(702)
        self.motes = [(rng.uniform(0, W), rng.uniform(0, sper), rng.uniform(0.3, 1.0))
                      for _ in range(24)]
        self.fish = self._fish()
        self.school = [(rng.uniform(-40, 40), rng.uniform(-26, 26), rng.uniform(0, TAU))
                       for _ in range(16)]
        hper = self.s.period("high-air")
        self.shafts = self._shafts(hper)
        self.vignette = self._vignette()

    def _floor(self, per):
        rng = np.random.default_rng(711)
        yy, xx = np.mgrid[0:per, 0:W]
        hgt = r02.periodic_fbm(W, per, 112, 712, octaves=3) * 0.6     # broad forms only: fine
        ridge = 1 - np.abs(2 * r02.periodic_fbm(W, per, 56, 713, octaves=2) - 1)   # relief is lost
        hgt += 0.3 * ridge ** 3
        for _ in range(14):                                     # boulders
            cx, cy, r = rng.uniform(0, W), rng.uniform(0, per), rng.uniform(4, 12)
            d = np.hypot(xx - cx, periodic_dist(yy, cy, per)) / r
            hgt += np.sqrt(np.clip(1 - d * d, 0, 1)) * 0.12
        shade = hillshade(hgt * 70, 0.5)
        tt = (hgt - hgt.min()) / (hgt.max() - hgt.min())
        col = ramp_img(self.FLOOR, 0.15 + tt * 0.75) * shade[..., None]
        img = smooth_tex(posterize(raster.to_rgba_image(np.clip(col * 0.82, 0, 255)), 9))
        img = self._reef(img, per)
        img = self._kelp(img, per)
        img = self._city(img, per)
        return img

    def _reef(self, img, per):
        ss = 2
        lay = Image.new("RGBA", (W * ss, per * ss), (0, 0, 0, 0))
        glow = Image.new("RGBA", (W * ss, per * ss), (0, 0, 0, 0))
        d, g = ImageDraw.Draw(lay), ImageDraw.Draw(glow)
        rng = np.random.default_rng(721)
        for cx, cy, n, tip in ((80, 390, 9, (154, 77, 255)), (130, 470, 6, (0, 255, 154)),
                               (430, 470, 7, (154, 77, 255))):
            for a in np.linspace(0, TAU, n, endpoint=False):
                branches(d, g, rng, cx + 8 * np.cos(a), cy + 8 * np.sin(a), a, rng.uniform(40, 70),
                         4.0, 2, (60, 30, 70), tip, ss, per, step=4, jitter=0.5)
            for dy in (-per, 0, per):
                d.ellipse([(cx - 12) * ss, (cy + dy - 12) * ss, (cx + 12) * ss, (cy + dy + 12) * ss],
                          fill=(46, 22, 56, 255))
        lay = lay.resize((W, per), Image.BOX)
        glow = glow.resize((W, per), Image.BOX)
        out = img.copy()
        out.alpha_composite(pshadow(lay, 0.6, 1.2), (3, 4))
        out.alpha_composite(lay)
        out.alpha_composite(pmid(raster.glow(ptile(glow), 2.5, 1.2), per))
        return out

    def _kelp(self, img, per):
        """Two kelp-farm plots: float-line frames with rows of kelp fronds (seen from above)."""
        out = img.copy()
        d = ImageDraw.Draw(out)
        rng = np.random.default_rng(731)
        dark, mid, hi = (6, 44, 28), (22, 104, 56), (70, 160, 90)
        for x0, y0, w, h in ((40, 40, 130, 96), (200, 300, 110, 120)):
            for dy in (-per, 0, per):
                d.rectangle([x0, y0 + dy, x0 + w, y0 + h + dy], outline=(70, 90, 100, 255))
                for ry in range(y0 + 8, y0 + h - 4, 12):    # kelp rows: lit strips, notched
                    d.rectangle([x0 + 4, ry + dy - 3, x0 + w - 4, ry + dy + 3], fill=dark + (255,))
                    d.rectangle([x0 + 4, ry + dy - 3, x0 + w - 4, ry + dy - 1], fill=mid + (255,))
                    for rx in range(x0 + 10, x0 + w - 6, int(rng.integers(9, 14))):
                        d.rectangle([rx, ry + dy - 3, rx + 1, ry + dy + 3], fill=dark + (255,))
                        d.point([(rx + 3, ry + dy - 2)], fill=hi + (255,))
                for k in range(0, w + 1, 26):                   # buoy lights
                    d.rectangle([x0 + k - 1, y0 + dy - 1, x0 + k + 1, y0 + dy + 1], fill=(255, 200, 80, 255))
        return out

    def _city(self, img, per):
        """Thera Deep: pressure domes with warm windows joined by tube corridors; a black
        smoker vent with a hot glowing base."""
        glass = sm.PaletteShim(B, {("EARTH ORBIT", 3): (60, 120, 150), ("UTC HULL", 4): (120, 130, 150)})
        dome = station.render_part(station.dome_part(28), glass)
        dome_m = station.render_part(station.dome_part(20), glass)
        dome_s = station.render_part(station.dome_part(15), glass)
        out = img.copy()
        domes = [(dome, 330, 330), (dome_m, 410, 260), (dome_s, 420, 395), (dome_m, 300, 430)]
        d = ImageDraw.Draw(out)
        for (a, ax, ay), (b, bx, by) in ((domes[0], domes[1]), (domes[0], domes[2]),
                                         (domes[0], domes[3])):
            for dy in (-per, 0, per):
                d.line([ax, ay + dy, bx, by + dy], fill=(40, 48, 60, 255), width=11)
                d.line([ax, ay + dy, bx, by + dy], fill=(110, 122, 140, 255), width=7)
                d.line([ax - 1, ay + dy - 2, bx - 1, by + dy - 2], fill=(170, 182, 196, 255), width=2)
        for x, y, r in ((330, 330, 60), (410, 260, 44), (420, 395, 36), (300, 430, 44)):
            for dy in (-per, 0, per):
                out = raster.add_light(out, x, y + dy, r, (255, 180, 90), 0.4)
        for sp, x, y in domes:
            for dy in (-per, 0, per):
                out.alpha_composite(sprite.shadow_of(sp, opacity=0.6, blur=1.5),
                                    (int(x - sp.width / 2 + 5), int(y + dy - sp.height / 2 + 7)))
                sprite.paste_center(out, sp, x, y + dy)
        vx, vy = self.vent
        d = ImageDraw.Draw(out)
        for dy in (-per, 0, per):
            out = raster.add_light(out, vx, vy + dy, 26, (255, 110, 30), 0.7)
            d = ImageDraw.Draw(out)
            for r, c in ((15, (30, 26, 28)), (11, (52, 44, 44)), (7, (22, 18, 18)), (3, (255, 150, 60))):
                d.ellipse([vx - r, vy + dy - r, vx + r, vy + dy + r], fill=c + (255,))
        return out

    def _fish(self):
        img = Image.new("RGBA", (9, 5), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        d.ellipse([0, 0, 7, 4], fill=(120, 170, 190, 255))
        d.polygon([(7, 2), (9, 0), (9, 4)], fill=(80, 130, 150, 255))
        d.point([(1, 1)], fill=(230, 250, 255, 255))
        return img

    def _shafts(self, per):
        """Diagonal light shafts from cracks in the ice above (top-left to bottom-right)."""
        yy, xx = np.mgrid[0:per, 0:W].astype(np.float64)
        u = xx * 0.8 - yy * 0.6                                 # across the shafts
        a = np.zeros_like(u)
        rng = np.random.default_rng(741)
        for c, wdt, s in [(rng.uniform(-300, 400), rng.uniform(14, 34), rng.uniform(0.5, 1.0))
                          for _ in range(5)]:
            for k in (-1, 0, 1):                                # periodic in y: shift along u
                a += s * np.exp(-((u - c - k * per * -0.6) / wdt) ** 2)
        a *= 0.75 + 0.25 * r02.periodic_fbm(W, per, 40, 742, 3)
        a = np.clip(a, 0, 1) * 64
        col = np.broadcast_to(np.array((150, 230, 240), float), a.shape + (3,))
        img = raster.to_rgba_image(col, a)
        a2 = np.array(pmid(ptile(img).filter(ImageFilter.GaussianBlur(5)), per))
        a2[..., :3] = np.array(col[:1, :1], dtype=np.uint8)
        return r03.step_alpha(Image.fromarray(a2, "RGBA"), 12)

    def _vignette(self):
        yy, xx = np.mgrid[0:H, 0:W].astype(np.float64)
        d = np.hypot((xx - W / 2) / (W * 0.62), (yy - H * 0.55) / (H * 0.66))
        return np.clip(1.15 - d ** 2 * 0.9, 0.38, 1.0)

    def light(self, img, f, strength=1.0):
        """Darkness at the edges plus the ship's headlight cone ahead (up the screen)."""
        a = np.array(img).astype(np.float64)
        px, py, _ = self.cast.player_pos(f)
        yy, xx = np.mgrid[0:H, 0:W].astype(np.float64)
        dy = py - yy
        cone = np.clip(1 - np.abs(xx - px) / (24 + dy * 0.42 + 1e-6), 0, 1) * (dy > -10)
        cone *= np.clip(1 - dy / 420, 0, 1) ** 0.8
        k = np.round(self.vignette * (1 + 1.5 * cone * strength) * 6) / 6     # stepped light
        cq = np.round(cone * strength * 4) / 4                                   # (flat GIF runs)
        a[..., :3] = a[..., :3] * k[..., None] + np.array((40, 50, 40)) * cq[..., None] * 0.35
        return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")

    def ground_image(self, f):
        img = scroll_tex(self.floor, self.s.off("ground", f))
        t = TAU * f / N
        x, yw = 150 + 40 * np.cos(t), 300 + 26 * np.sin(t)          # Scuttler on the sea floor
        heading = np.arctan2(26 * np.cos(t), -40 * np.sin(t))
        sp = walk_sprite(heading, 2.625 * f)     # 42 phase steps per loop
        for yy in self.ys("ground", yw, f):
            sprite.paste_center(img, sprite.shadow_of(sp, opacity=0.5, blur=1.2), x + 5, yy + 7)
            sprite.paste_center(img, sp, x, yy)
        return img

    def nautilus_pos(self, f, k):
        px, py, _ = self.cast.player_pos(f)
        a = TAU * f / N * (1 if k == 0 else -1) + (0 if k == 0 else np.pi)
        r = 150 + 30 * np.sin(2 * TAU * f / N + k)
        return px + r * np.cos(a), py - 120 + r * 0.7 * np.sin(a), a

    def sub_layer(self, f):
        sub = scroll_tex(self.silt, self.s.off("sub", f))
        d = ImageDraw.Draw(sub)
        for x, y, b in self.motes:
            for yy in self.ys("sub", y, f, margin=10):
                d.point([(x + 3 * np.sin(yy * 0.03 + b * 9), yy)], fill=(120, 190, 200, int(70 + 110 * b)))
        # vent smoke: dark plume billowing from the vent, drifting with the current (sub layer)
        vx, vyw = self.vent
        for k in range(6):
            age = ((f + k * 13) % N) / N
            for yy in self.ys("ground", vyw, f, margin=200):
                cx, cy = vx - 50 * age, yy - 30 * age * 0 + 20 * age
                r = 8 + 26 * age
                blob = Image.new("RGBA", (int(2 * r + 4), int(2 * r + 4)), (0, 0, 0, 0))
                ImageDraw.Draw(blob).ellipse([2, 2, 2 * r + 2, 2 * r + 2],
                                             fill=(10, 14, 16, int(150 * (1 - age))))
                blob = r03.step_alpha(blob.filter(ImageFilter.GaussianBlur(3)), 4)
                sprite.paste_center(sub, blob, cx, cy)
        # a fish school crossing right to left once per loop
        sx = W + 60 - (W + 120) * f / N
        sy = 220 + 20 * np.sin(TAU * f / N)
        for ox, oy, ph in self.school:
            sprite.paste_center(sub, self.fish, sx + ox + 3 * np.sin(TAU * 3 * f / N + ph),
                                sy + oy + 2 * np.cos(TAU * 2 * f / N + ph))
        casters = []
        jel = shared("jelly")
        for i, (cx, cy, rx) in enumerate(((120, 120, 30), (380, 200, 24), (240, 360, 36))):
            a = TAU * f / N + i * 2.0
            x, y = cx + rx * np.cos(a), cy + rx * 0.6 * np.sin(a)
            heading = np.arctan2(rx * 0.6 * np.cos(a), -rx * np.sin(a))
            sp = jel.get(heading, pulse=round(np.sin(TAU * 4 * f / N + i), 1))
            casters.append((sp, x, y))
            sprite.paste_center(sub, sp, x, y)
        nau = shared("nautilus")
        for k in range(2):
            x, y, a = self.nautilus_pos(f, k)
            unc = 1.0 if (f + 20 * k) % 40 < 10 else 0.0
            sp = nau.get(TAU * 2 * f / N * (1 if k == 0 else -1) if not unc else a + np.pi,
                         uncoil=unc)
            casters.append((sp, x, y))
            sprite.paste_center(sub, sp, x, y)
        self._sub_casters = casters
        return sub

    def layer_images(self, f):
        ground = self.ground_image(f)
        sub = self.sub_layer(f)
        shadows = r02.shadow_layer(self._sub_casters, *SHADOW["sub"], opacity=0.3, blur=2.5)
        air = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        casters = []
        for k in range(2):
            aimed(air, f, lambda ff, k=k: self.nautilus_pos(ff, k)[:2], self.cast, period=40,
                  speed=4.5, kind="big", phase=6 + 20 * k, life=40)
        for i, (cx, cy, rx) in enumerate(((120, 120, 30), (380, 200, 24))):
            age = (f + 30 * i) % 40
            if age < 14:                                    # Driftjelly ring pulse (mine)
                a = TAU * (f - age) / N + i * 2.0
                x, y = cx + rx * np.cos(a), cy + rx * 0.6 * np.sin(a)
                for j in range(8):
                    an = TAU * j / 8
                    e3.orb(air, x + (12 + age * 4.5) * np.cos(an), y + (12 + age * 4.5) * np.sin(an), 5,
                           ring=SHOTS[5])
        self.cast.player(air, casters, f)
        shadows.alpha_composite(r02.shadow_layer(casters, *SHADOW["air"], opacity=0.3, blur=2.5))
        tint = lambda im: tinted(im, self.WATER, 0.18)
        return {"ground": self.light(ground, f), "shadows": shadows,
                "sub": self.light(tint(sub), f, 0.7), "air": air,
                "high-air": scroll_tex(self.shafts, self.s.off("high-air", f))}



# --------------------------------------------------------------------------- C: asteroid belt

ASC_SHIM = {("UTC HULL", 4): (52, 40, 72), ("UTC HULL", 1): (12, 8, 18),
            ("UTC ACCENTS", 4): (255, 168, 0), ("UTC ACCENTS", 5): (255, 70, 50),
            ("EARTH ORBIT", 2): (90, 56, 10), ("EARTH ORBIT", 3): (110, 24, 36)}
ROCK = [(0.0, (14, 11, 12)), (0.3, (46, 38, 36)), (0.6, (92, 80, 74)), (0.85, (146, 132, 120)),
        (1.0, (196, 182, 166))]


def tri(x):
    """Triangle wave 0..1..0 with period 1."""
    x = x % 1.0
    return 1 - np.abs(2 * x - 1)


class BeltScene(Scene):
    slug, key = "belt", "belt"
    title = "ASTEROID BELT - MINING ROCK, REFINERY, HELIX BLOCK"
    layers = ["deep", "far", "ground", "low-air", "air", "high-air"]
    notes = {"deep": "X0.08 STARS, DUST BAND, SUN", "far": "X0.45 DISTANT ROCKS",
             "ground": "X1.0 ROCK, STATIONS, REFINERY", "low-air": "X1.35 ROCKS, HAULER, DUST",
             "air": "PLAY PLANE, DRIFTING ROCK", "high-air": "X2.3 CLOSE FRAGMENTS"}
    TRAVEL = 640                 # 160 px/s
    FACTORS = {"deep": 0.08, "far": 0.45, "ground": 1.0, "low-air": 1.35, "high-air": 2.3}
    GIF_COLORS = 128
    EXTRA_BOXES = [(80, 160, 160, 240), (60, 470, 250, 580), (300, 110, 440, 260)]

    def __init__(self):
        super().__init__()
        self.deep_shift = self.FACTORS["deep"] * self.TRAVEL
        self.deep = self._deep(int(H + 2 * self.deep_shift) + 4)
        self.far = self._far(self.s.period("far"))
        self.ground_tex = self._ground(self.TRAVEL)
        lp = self.s.period("low-air")
        dust = [(40, 32, 30), (66, 54, 48), (96, 82, 72), (128, 112, 98)]
        self.dust = bank_tex(lp, [216, 108, 54, 27], 801, 0.7, 0.12, dust, max_alpha=170,
                             stretch=2, light=4.0)
        rng = np.random.default_rng(802)
        self.low_rocks = [(sm.rock_sprites(r, 810 + i, ROCK, rotations=16,
                                           haze=((40, 36, 52), 0.25)),
                           rng.uniform(30, W - 30), rng.uniform(0, lp), int(rng.choice([-1, 1])))
                          for i, r in enumerate((14, 10, 18, 12))]
        self.hauler = self._hauler()
        self.big_rock = sm.rock_sprites(30, 830, ROCK, rotations=16)
        hp = self.s.period("high-air")
        frags = sm.rock_sprites(24, 840, ROCK, rotations=4)
        self.frags = [(r02.alpha_scale(fr.resize((fr.width * 3, fr.height * 3), Image.BILINEAR)
                                       .filter(ImageFilter.GaussianBlur(3)), 0.38),
                       rng.uniform(30, W - 30), rng.uniform(0, hp)) for fr in frags[:3]]
        self.streaks = [(rng.uniform(0, W), rng.uniform(0, hp), rng.uniform(0.3, 1.0))
                        for _ in range(26)]

    def _deep(self, h):
        img = raster.starfield(W, h, 851, density=0.003)
        a = np.array(img).astype(np.float64)
        yy, xx = np.mgrid[0:h, 0:W].astype(np.float64)
        band = np.exp(-((xx * 0.5 + yy * 0.25 - 260) / 70) ** 2)
        band *= 0.6 + 0.4 * raster.fbm(W, h, 48, 852, octaves=4, period=False)
        a[..., :3] += band[..., None] * np.array((60, 44, 50)) * 0.6
        img = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")
        img = raster.add_light(img, 420, 70, 26, (255, 240, 210), 1.2)          # the Sun
        img = raster.add_light(img, 420, 70, 70, (255, 200, 140), 0.25)
        tiny = sm.rock_sprites(5, 853, ROCK, rotations=1, haze=((30, 26, 40), 0.55))[0]
        rng = np.random.default_rng(854)
        for _ in range(14):
            sprite.paste_center(img, tiny, rng.uniform(0, W), rng.uniform(0, h))
        return posterize(img, 24)

    def _far(self, per):
        img = Image.new("RGBA", (W, per), (0, 0, 0, 0))
        for i, (x, y, r) in enumerate(((60, 40, 26), (380, 120, 34), (230, 230, 18), (440, 260, 14))):
            rk = sm.rock_sprites(r, 860 + i, ROCK, rotations=1, haze=((44, 36, 60), 0.5))[0]
            for dy in (-per, 0, per):
                sprite.paste_center(img, rk, x, y + dy)
        return img

    def edge_x(self, y):
        t = TAU * y / self.TRAVEL
        return 236 + 58 * np.sin(t + 0.4) + 24 * np.sin(3 * t + 1.1)

    def _ground(self, per):
        rng = np.random.default_rng(871)
        yy, xx = np.mgrid[0:per, 0:W]
        warp = r02.periodic_fbm(W, per, 40, 872, octaves=4)
        inside = xx < self.edge_x(yy) + (warp - 0.5) * 40
        isl_d = np.hypot((xx - 420) / 48.0, periodic_dist(yy, 470, per) / 64.0) + (warp - 0.5) * 0.4
        inside |= isl_d < 1.0
        hgt = r02.periodic_fbm(W, per, 128, 873, octaves=5) * 0.5
        for _ in range(26):
            cx, cy, r = rng.uniform(0, W), rng.uniform(0, per), rng.uniform(5, 24)
            d = np.hypot(xx - cx, periodic_dist(yy, cy, per)) / r
            hgt += np.where(d < 1, -(1 - d * d) * 0.5, 0) * min(1, r / 20)
            hgt += np.exp(-((d - 1) / 0.18) ** 2) * 0.15 * min(1, r / 20)
        # rim falloff: the rock curves away at its edge
        dist_in = pblur_mask(inside.astype(np.float64), 10)
        hgt += dist_in * 0.6
        shade = hillshade(hgt * 60, 0.5) * (0.45 + 0.55 * dist_in)
        lit = cast_shadows(hgt * 60, steps=16, drop=1.2)
        tt = (hgt - hgt.min()) / (hgt.max() - hgt.min())
        col = ramp_img(ROCK, 0.12 + tt * 0.7) * (shade * (0.4 + 0.6 * lit))[..., None]
        # refinery pit: a terraced crater with a molten pool (yellow core, orange edge)
        pit_d = np.hypot(xx - 120, periodic_dist(yy, 200, per)) / 42
        terr = 0.55 + 0.45 * (np.floor(np.clip(pit_d, 0, 1.6) * 5) % 2)
        col = np.where((pit_d < 1.5)[..., None], col * (0.5 + 0.35 * terr)[..., None], col)
        img = raster.to_rgba_image(np.clip(col * 0.72, 0, 255), inside * 255.0)
        img = posterize(img, 24)
        return self._belt_structures(img, per)

    def _belt_structures(self, img, per):
        P = station
        asc = sm.PaletteShim(B, ASC_SHIM)
        drum = P.render_part(P.drum_part(18, 34), B)
        dock = P.render_part(P.dock_part(), B)
        solar = P.render_part(P.solar_part(110, 40, False), B)
        radiator = P.render_part(P.radiator_part(70, 3), B)
        truss = P.render_part(P.truss_h_part(150, 22), B)
        h_drum = P.render_part(P.drum_part(20, 40), asc)
        h_truss = P.render_part(P.truss_h_part(90, 20), asc)
        h_cargo = P.render_part(P.cargo_part(P.ACCENT), asc)
        out = img.copy()
        d = ImageDraw.Draw(out)
        # conveyor tubes from the refinery pit to the station platform across the gap
        for dy in (-per, 0, per):
            for k, (x0, y0, x1, y1) in enumerate(((150, 190, 330, 160), (150, 214, 330, 236))):
                d.line([x0, y0 + dy, x1, y1 + dy], fill=(30, 28, 34, 255), width=9)
                d.line([x0, y0 + dy, x1, y1 + dy], fill=(120, 116, 124, 255), width=5)
                for s in np.linspace(0, 1, 12):
                    xs, ys = x0 + (x1 - x0) * s, y0 + (y1 - y0) * s + dy
                    d.rectangle([xs - 1, ys - 3, xs + 1, ys + 3], fill=(60, 58, 66, 255))
        pool = Image.new("RGBA", (W, per), (0, 0, 0, 0))
        pd = ImageDraw.Draw(pool)
        rng = np.random.default_rng(875)
        for dy in (-per, 0, per):                     # molten ore pool in the refinery pit
            for r, c in ((26, (40, 14, 8)), (22, (130, 44, 12)), (16, (190, 76, 18)),
                         (9, (225, 120, 36))):
                pts = [(120 + (r + rng.uniform(-2, 2)) * np.cos(a),
                        200 + dy + (r + rng.uniform(-2, 2)) * np.sin(a))
                       for a in np.linspace(0, TAU, 18, endpoint=False)]
                pd.polygon(pts, fill=c + (255,))
            for _ in range(7):                        # floating dark crust on the melt
                cx, cy = 120 + rng.uniform(-14, 14), 200 + dy + rng.uniform(-14, 14)
                rr = rng.uniform(2, 4.5)
                pd.ellipse([cx - rr, cy - rr, cx + rr, cy + rr], fill=(60, 22, 10, 255))
        out.alpha_composite(pmid(raster.glow(ptile(pool), 3.0, 0.35), per))
        for dy in (-per, 0, per):
            out = raster.add_light(out, 120, 200 + dy, 46, (255, 110, 30), 0.12)
        items = [(truss, 360, 198, 0.6), (drum, 330, 160, 0.6), (dock, 395, 236, 0.6),
                 (solar, 400, 120, 0.6), (radiator, 95, 370, 0.6),
                 (h_truss, 160, 520, 0.6), (h_drum, 120, 500, 0.6), (h_drum, 200, 540, 0.6),
                 (h_cargo, 90, 545, 0.6), (h_cargo, 108, 545, 0.6)]
        bunker = self.cast.unit("rail-bunker-a")
        items.append((bunker, 230, 470, 0.6))
        self.bunker = (230, 470)
        for sp, x, y, op in items:
            for dy in (-per, 0, per):
                out.alpha_composite(sprite.shadow_of(sp, opacity=op, blur=1.0),
                                    (int(x - sp.width / 2 + 6), int(y + dy - sp.height / 2 + 8)))
                sprite.paste_center(out, sp, x, y + dy)
        for x, y in ((150, 500), (190, 520), (130, 560)):                     # red Helix lights
            for dy in (-per, 0, per):
                out = raster.add_light(out, x, y + dy, 6, (255, 40, 30), 0.8)
        return out

    def _hauler(self):
        P = station
        c1 = P.render_part(P.cargo_part(), B)
        c2 = P.render_part(P.cargo_part(P.SOLAR), B)
        tr = P.render_part(P.truss_v_part(30, 18), B)
        img = Image.new("RGBA", (40, 120), (0, 0, 0, 0))
        for y in range(0, 120, tr.height):
            sprite.paste_center(img, tr, 20, y + tr.height / 2)
        for k, y in enumerate((18, 46, 74, 102)):
            sprite.paste_center(img, c1 if k % 2 else c2, 20, y)
        return r02.haze(img, (40, 36, 52), 0.2)

    def layer_images(self, f):
        deep = crossfade(self.deep, self.deep_shift, f)
        far = scroll_tex(self.far, self.s.off("far", f))
        ground = scroll_tex(self.ground_tex, self.s.off("ground", f))
        low = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        low_casters = []
        for frames, x, y, spin in self.low_rocks:
            for yy in self.ys("low-air", y, f, margin=60):
                sp = frames[(spin * f // 5) % 16]
                low_casters.append((sp, x, yy))
                sprite.paste_center(low, sp, x, yy)
        for yy in self.ys("low-air", 300, f, margin=120):
            low_casters.append((self.hauler, 210, yy))
            sprite.paste_center(low, self.hauler, 210, yy)
        air = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        casters = []
        # a big rock tumbling through the play plane (destructible cover)
        ry = -50 + (H + 100) * ((f / N + 0.3) % 1.0)
        sp = self.big_rock[(f // 5) % 16]
        casters.append((sp, 120, ry))
        sprite.paste_center(air, sp, 120, ry)
        # two Talons swooping in from the top, firing paired gold shots
        for k in range(2):
            t = (f / N + 0.5 * k) % 1.0
            x = 240 + (150 if k else -150) * np.cos(np.pi * t)
            y = -40 + 360 * np.sin(np.pi * t)
            vx = -(150 if k else -150) * np.sin(np.pi * t)
            sp = self.cast.unit("talon-a", anim=float(np.clip(-vx / 300, -0.45, 0.45)))
            casters.append((sp, x, y))
            sprite.paste_center(air, sp, x, y)

            def src(ff, k=k, side=0):
                tt = (ff / N + 0.5 * k) % 1.0
                return (240 + (150 if k else -150) * np.cos(np.pi * tt),
                        -40 + 360 * np.sin(np.pi * tt) + 16)
            aimed(air, f, src, self.cast, period=20, speed=8.0, kind="gold", phase=4 + 10 * k,
                  count=4, life=30)
        # a Buzzsaw Drone ricocheting on a closed zig-zag (triangle waves)
        bx = 40 + (W - 80) * tri(2 * f / N)
        by = 60 + 300 * tri(f / N + 0.25)
        saw = shared("saw")
        sp = saw.get(TAU * 6 * f / N)
        casters.append((sp, bx, by))
        sprite.paste_center(air, sp, bx, by)
        # rail bunker: telegraph line then a rail shot down the player's column (once a loop)
        bxw, byw = self.bunker
        gy = (byw + self.s.off("ground", f)) % self.TRAVEL
        if 0 < gy < H and 44 <= f < 60:
            px, py, _ = self.cast.player_pos(46)
            if f < 54:
                e3.dashed(air, bxw, gy, px, H, (255, 60, 40))
            else:
                e3.beam(air, bxw, gy, px, H, ring=GOLD[3])
        self.cast.player(air, casters, f)
        shadows = r02.shadow_layer(low_casters, *SHADOW["low-air"], opacity=0.45)
        shadows.alpha_composite(r02.shadow_layer(casters, *SHADOW["air"], opacity=0.5))
        shadows = masked(shadows, ground)
        L = {"deep": deep, "far": far, "ground": ground, "shadows": shadows, "low-air": low,
             "air": air}
        dust = scroll_tex(self.dust, self.s.off("low-air", f))
        L = r03.decorate(L, dust, "ground", casters, bank_shadow_opacity=0.25, flyer_opacity=0.3)
        high = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        d = ImageDraw.Draw(high)
        step = self.FACTORS["high-air"] * self.TRAVEL / N
        for x, y, b in self.streaks:
            for yy in self.ys("high-air", y, f, margin=40):
                d.line([x, yy - step * (0.6 + b), x, yy], fill=(170, 150, 130, int(40 + 70 * b)))
        for spr, x, y in self.frags:
            for yy in self.ys("high-air", y, f, margin=100):
                sprite.paste_center(high, spr, x, yy)
        L["high-air"] = high
        return L



# --------------------------------------------------------------------------- D: Jupiter cloud cities

JOV = [(0.0, (46, 22, 16)), (0.3, (120, 56, 30)), (0.55, (186, 112, 58)), (0.8, (226, 184, 124)),
       (1.0, (246, 230, 190))]


def lightning(img, x0, y0, length, seed, color=(220, 236, 255)):
    rng = np.random.default_rng(seed)
    pts = [(x0, y0)]
    x, y = x0, y0
    for _ in range(14):
        x, y = x + rng.uniform(-14, 14), y + length / 14
        pts.append((x, y))
    lay = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(lay)
    d.line(pts, fill=color + (255,), width=3)
    d.line(pts, fill=(255, 255, 255, 255), width=1)
    img.alpha_composite(raster.glow(lay, 5.0, 1.4))


class JovianScene(Scene):
    slug, key = "jovian", "jovian"
    title = "JUPITER - AURELIA OVER THE CLOUD BANDS"
    layers = ["deep", "far", "ground", "low-air", "air", "high-air"]
    notes = {"deep": "X0.1 DEEP CLOUD BANDS, VORTEX", "far": "X0.5 TOWERING CLOUD TOPS",
             "ground": "X1.0 AURELIA DECKS, SPIRES", "low-air": "X1.35 BALLOONS, RING, WISPS",
             "air": "PLAY PLANE", "high-air": "X2.3 STORM WISPS, LIGHTNING"}
    TRAVEL = 680                 # 170 px/s: grand and fast
    FACTORS = {"deep": 0.1, "far": 0.5, "ground": 1.0, "low-air": 1.35, "high-air": 2.3}
    GIF_COLORS = 120
    EXTRA_BOXES = [(230, 120, 410, 300), (40, 280, 80, 320)]
    FLASH = {46: 0.30, 47: 0.18, 48: 0.07}

    def __init__(self):
        super().__init__()
        self.deep_shift = self.FACTORS["deep"] * self.TRAVEL
        self.deep = self._deep(int(H + 2 * self.deep_shift) + 4)
        self.far_shift = self.FACTORS["far"] * self.TRAVEL
        self.far = self._far(int(H + 2 * self.far_shift) + 4)
        self.ground_tex = self._city(self.TRAVEL)
        lp = self.s.period("low-air")
        cream = [(150, 110, 80), (196, 160, 120), (226, 202, 162), (244, 232, 206)]
        self.wisps_low = bank_tex(lp, [153, 51, 17], 901, 0.7, 0.12, cream, max_alpha=190,   # cells divide 918
                                  stretch=2, light=6.0)
        self.balloon = sm.render_px_model(sm.aurelia_balloon(), factor=4)
        hp = self.s.period("high-air")
        self.storm = wisp_tex(hp, [92, 46, 23], 911, (236, 216, 180), thresh=0.6, gain=2.6,   # cells divide 1564
                              max_a=90, stretch=3)

    def _deep(self, h):
        """Deep cloud bands: smooth, streaky bands curling round a storm vortex."""
        yy, xx = np.mgrid[0:h, 0:W].astype(np.float64)
        warp = raster.fbm(W, h, 96, 921, octaves=3, period=False)
        streak = raster.fbm(W // 6, h, 8, 922, octaves=3, period=False)
        streak = np.array(Image.fromarray((streak * 255).astype(np.uint8)).resize((W, h), Image.BICUBIC),
                          dtype=np.float64) / 255
        cx, cy = 320, h * 0.45
        r = np.hypot(xx - cx, (yy - cy) * 1.5) + 1e-6
        ang = np.arctan2((yy - cy) * 1.5, xx - cx)
        fall = np.clip(1 - r / 120, 0, 1) ** 1.5
        bands = 0.5 + 0.5 * np.sin(yy / 30.0 + warp * 2.4)
        swirl = 0.5 + 0.5 * np.sin(ang * 2 + np.log(r) * 3.5)    # integer winding: no seam at +-pi
        v = bands * (1 - fall) + swirl * fall
        v = 0.65 * v + 0.35 * streak
        col = ramp_img(JOV, 0.25 + v * 0.55)
        col = col * 0.5 + np.array((60, 36, 32)) * 0.25                  # deep: darker, hazier
        return posterize(raster.to_rgba_image(np.clip(col, 0, 255)), 24)

    def _far(self, h):
        """Towering cloud tops: clusters of billowy cumulus heads (summed soft blobs), lit
        from the top-left with soft self-shadowing; gaps show the deep bands."""
        rng = np.random.default_rng(931)
        hgt = np.zeros((h, W))
        yy, xx = np.mgrid[0:h, 0:W].astype(np.float64)
        for _ in range(int(h / 95)):
            cx, cy = rng.uniform(-40, W + 40), rng.uniform(0, h)
            for _ in range(16):
                bx, by = cx + rng.normal(0, 46), cy + rng.normal(0, 30)
                rr = rng.uniform(12, 34)
                x0, x1 = int(max(0, bx - 3 * rr)), int(min(W, bx + 3 * rr))
                y0, y1 = int(max(0, by - 3 * rr)), int(min(h, by + 3 * rr))
                if x0 >= x1 or y0 >= y1:
                    continue
                d2 = ((xx[y0:y1, x0:x1] - bx) ** 2 + (yy[y0:y1, x0:x1] - by) ** 2) / rr ** 2
                hgt[y0:y1, x0:x1] = np.maximum(hgt[y0:y1, x0:x1],
                                               rr * np.sqrt(np.clip(1 - d2 / 2.2, 0, 1)))
        hgt = np.array(Image.fromarray(np.clip(hgt * 6, 0, 255).astype(np.uint8))
                       .filter(ImageFilter.GaussianBlur(2)), dtype=np.float64) / 6
        alpha = sdf.smoothstep(4, 9, hgt)
        gy, gx = np.gradient(hgt)
        shade = np.clip(0.5 - (gx + gy) * 0.22 + hgt / 60, 0, 1)
        lit = cast_shadows(np.concatenate([hgt, hgt[:1]], axis=0)[:-1], steps=14, drop=0.9)
        shade *= 0.55 + 0.45 * lit
        tones = [(0.0, (70, 36, 26)), (0.35, (138, 82, 48)), (0.65, (196, 150, 104)),
                 (1.0, (232, 210, 172))]
        col = ramp_img(tones, shade) * 0.84
        return posterize(raster.to_rgba_image(np.clip(col, 0, 255), alpha * 255), 24)

    def _city(self, per):
        out = Image.new("RGBA", (W, per), (0, 0, 0, 0))
        big = sm.render_px_model(sm.aurelia_deck(78), factor=3, colors=48)
        small = sm.render_px_model(sm.aurelia_deck(56), factor=3, colors=48)
        spire = sm.render_px_model(sm.aurelia_spire(18), factor=4)
        asc = sm.PaletteShim(B, ASC_SHIM)
        turret = station.render_part(station.turret_part(), asc)
        d = ImageDraw.Draw(out)
        for dy in (-per, 0, per):                      # gilded walkway between the decks
            for w_, c in ((14, (40, 26, 20)), (10, (224, 200, 160)), (2, (255, 176, 0))):
                d.line([300, 250 + dy, 150, 500 + dy], fill=c + (255,), width=w_)
            d.line([120, 560 + dy, 60, 690 + dy], fill=(224, 200, 160, 255), width=9)
        pad = sm.render_px_model(sm.aurelia_deck(22), factor=4, colors=32)
        pieces = [(big, 320, 210), (small, 140, 520), (pad, 440, 380), (spire, 440, 380),
                  (pad, 56, 300), (spire, 56, 300),
                  (turret, 264, 160), (turret, 376, 160), (turret, 140, 470)]
        self.turrets = [(264, 160), (376, 160), (140, 470)]
        for sp, x, y in pieces:
            for dy in (-per, 0, per):
                sprite.paste_center(out, sp, x, y + dy)
        return out

    def layer_images(self, f):
        deep = crossfade(self.deep, self.deep_shift, f)
        if f in self.FLASH:
            lightning(deep, 120, 60, 260, 940)
        far = crossfade(self.far, self.far_shift, f)
        ground = scroll_tex(self.ground_tex, self.s.off("ground", f))
        # city shadows fall on the cloud tops far below: long offset, soft (depth cue)
        city_sh = sprite.shadow_of(ground, opacity=0.4, blur=4.0)
        far.alpha_composite(masked(city_sh, far), (26, 34))
        low = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        d = ImageDraw.Draw(low)
        low_casters = []
        for x, y in ((70, 120), (420, 520), (250, 820)):
            for yy in self.ys("low-air", y, f, margin=90):
                d.line([x, yy + 44, x - 10, yy + 90], fill=(60, 40, 30, 200), width=1)
                low_casters.append((self.balloon, x, yy))
                sprite.paste_center(low, self.balloon, x, yy)
        for yy in self.ys("ground", 210, f, margin=140):             # anti-grav ring under the deck
            ring = Image.new("RGBA", (W, H), (0, 0, 0, 0))
            ImageDraw.Draw(ring).ellipse([320 - 112, yy - 112, 320 + 112, yy + 112],
                                         outline=(170, 230, 255, 120), width=3)
            low.alpha_composite(raster.glow(ring, 3.0, 0.8))
        air = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        casters = []
        # Gilded Gunship holding station, ring burst twice a loop
        gx, gy = 240 + 80 * np.sin(TAU * f / N), 120
        gs = self.cast.unit("gilded-gunship-a")
        casters.append((gs, gx, gy))
        sprite.paste_center(air, gs, gx, gy)
        for k in range(2):
            age = (f - k * 40) % N
            if age < 30:
                bx = 240 + 80 * np.sin(TAU * (f - age) / N)
                for j in range(12):
                    a = TAU * j / 12 + k * 0.26
                    e3.orb(air, bx + (20 + age * 6) * np.cos(a), gy + (20 + age * 6) * np.sin(a), 5)
        for k in range(2):                                            # Talons crossing
            t = (f / N + 0.5 * k) % 1.0
            x = -40 + (W + 80) * t if k == 0 else W + 40 - (W + 80) * t
            y = 280 + 60 * np.sin(TAU * t)
            sp = self.cast.unit("talon-a", anim=0.45 if k == 0 else -0.45)
            casters.append((sp, x, y))
            sprite.paste_center(air, sp, x, y)
        # a Vrell Spore Bomber (gas-bag organism) drifting down, dropping spores
        sx, sy = 380 + 20 * np.sin(TAU * f / N), -60 + (H + 120) * ((f / N + 0.2) % 1.0)
        sb = self.cast.unit("spore-bomber-a", anim=round(np.sin(TAU * 2 * f / N), 1))
        casters.append((sb, sx, sy))
        sprite.paste_center(air, sb, sx, sy)
        for k in range(4):
            age = (f - k * 20) % N
            if age < 50:
                ox = 380 + 20 * np.sin(TAU * (f - age) / N)
                oy = -60 + (H + 120) * (((f - age) / N + 0.2) % 1.0) + 30 + age * 1.5
                e3.orb(air, ox + 8 * np.sin(age * 0.3), oy, 4, ring=SHOTS[5])
        for i, (tx, tyw) in enumerate(self.turrets[:2]):
            def src(ff, tx=tx, tyw=tyw):
                y = (tyw + self.s.off("ground", ff)) % self.TRAVEL
                return (tx, y) if 0 <= y <= H else None
            aimed(air, f, src, self.cast, period=40, speed=7.0, kind="gold", phase=20 * i)
        self.cast.player(air, casters, f)
        shadows = r02.shadow_layer(low_casters, *SHADOW["low-air"], opacity=0.4)
        shadows.alpha_composite(r02.shadow_layer(casters, *SHADOW["air"], opacity=0.45))
        shadows = masked(shadows, ground)
        L = {"deep": deep, "far": far, "ground": ground, "shadows": shadows, "low-air": low,
             "air": air}
        wisps = scroll_tex(self.wisps_low, self.s.off("low-air", f))
        L = r03.decorate(L, wisps, "ground", casters, bank_shadow_opacity=0.25, flyer_opacity=0.3)
        high = scroll_tex(self.storm, self.s.off("high-air", f))
        if f in self.FLASH:
            fl = Image.new("RGBA", (W, H), (210, 225, 255, int(255 * self.FLASH[f])))
            high.alpha_composite(fl)
        L["high-air"] = high
        return L



# --------------------------------------------------------------------------- E: Vrell space

def voronoi_periodic(w, per, n, seed):
    """Distances to the nearest and second-nearest of ``n`` jittered points (wraps in y)."""
    rng = np.random.default_rng(seed)
    pts = np.stack([rng.uniform(0, w, n), rng.uniform(0, per, n)], axis=1)
    pts = np.concatenate([pts, pts + [0, per], pts - [0, per]])
    yy, xx = np.mgrid[0:per, 0:w].astype(np.float64)
    d1 = np.full((per, w), 1e9)
    d2 = np.full((per, w), 1e9)
    idx = np.zeros((per, w), dtype=int)
    for i, (px, py) in enumerate(pts):
        d = np.hypot(xx - px, yy - py)
        closer = d < d1
        d2 = np.where(closer, d1, np.minimum(d2, d))
        idx = np.where(closer, i % n, idx)
        d1 = np.where(closer, d, d1)
    return d1, d2, idx


class VrellSpaceScene(Scene):
    slug, key = "vrell-space", "vrell"
    title = "VRELL SPACE - HIVE-REEF OVER THE NEBULA"
    layers = ["deep", "far", "ground", "low-air", "air", "high-air"]
    notes = {"deep": "X0.1 NEBULA, STRANGE STARS", "far": "X0.45 DISTANT HIVE-REEFS",
             "ground": "X1.0 HIVE SURFACE, PITS, TURRETS", "low-air": "X1.35 SPORE SACS, TENDRILS",
             "air": "PLAY PLANE", "high-air": "X2.2 SPORE WISPS"}
    TRAVEL = 640                 # 160 px/s
    FACTORS = {"deep": 0.1, "far": 0.45, "ground": 1.0, "low-air": 1.35, "high-air": 2.2}
    GIF_COLORS = 120
    EXTRA_BOXES = [(100, 100, 300, 300), (130, 120, 170, 160)]
    PITS = [(150, 140, 15), (300, 380, 18), (210, 560, 13)]
    TURRETS = [(110, 300), (360, 190), (250, 470)]
    MORTAR = (330, 560)

    def __init__(self):
        super().__init__()
        self.deep_shift = self.FACTORS["deep"] * self.TRAVEL
        self.deep = self._deep(int(H + 2 * self.deep_shift) + 4)
        self.far_shift = self.FACTORS["far"] * self.TRAVEL
        self.far = self._far(int(H + 2 * self.far_shift) + 4)
        self.ground_tex = self._hive(self.TRAVEL)
        lp = self.s.period("low-air")
        spore = [(40, 14, 50), (78, 30, 92), (118, 56, 130), (160, 96, 168)]
        self.spores = bank_tex(lp, [216, 108, 54, 27], 1001, 0.65, 0.13, spore, max_alpha=180,
                               light=5.0)
        rng = np.random.default_rng(1002)
        self.sacs = [(rng.uniform(30, W - 30), rng.uniform(0, lp), rng.uniform(5, 9), rng.uniform(0, TAU))
                     for _ in range(8)]
        self.tendrils = [(rng.uniform(0, W), rng.uniform(0, lp), rng.uniform(0, TAU)) for _ in range(4)]
        hp = self.s.period("high-air")
        self.wisps = wisp_tex(hp, [176, 88, 44, 22], 1011, (170, 110, 190), thresh=0.54, gain=2.4,
                              max_a=88)

    def _deep(self, h):
        img = raster.starfield(W, h, 1021, density=0.002,
                               colors=[(255, 255, 255), (200, 255, 240), (255, 200, 255)])
        a = np.array(img).astype(np.float64)
        n1 = raster.fbm(W, h, 128, 1022, octaves=5, period=False)
        n2 = raster.fbm(W, h, 96, 1023, octaves=5, period=False)
        violet = np.clip((n1 - 0.45) * 2.0, 0, 1)[..., None] * np.array((92, 30, 110))
        teal = np.clip((n2 - 0.55) * 2.4, 0, 1)[..., None] * np.array((10, 80, 86))
        a[..., :3] = a[..., :3] * 0.8 + violet + teal
        return posterize(Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA"), 24)

    def _far(self, h):
        n = raster.fbm(W, h, 80, 1031, octaves=5, period=False)
        n = (n - n.min()) / (n.max() - n.min())
        tones = [(20, 8, 26), (40, 18, 48), (62, 30, 70), (86, 50, 96)]
        img = r03.bank(n, 0.6, 0.06, tones, max_alpha=255, light=6.0)
        a = np.array(img)
        rng = np.random.default_rng(1032)
        for _ in range(int(h * W / 2200)):                 # glow points on the distant reefs
            x, y = int(rng.uniform(0, W)), int(rng.uniform(0, h))
            if a[y, x, 3] > 0:
                c = (0, 200, 140) if rng.random() < 0.5 else (150, 80, 230)
                a[y, x, :3] = c
        return posterize(Image.fromarray(a, "RGBA"), 24)

    def _hive(self, per):
        yy, xx = np.mgrid[0:per, 0:W].astype(np.float64)
        warp = r02.periodic_fbm(W, per, 64, 1041, octaves=4)
        edge_l = 40 + 30 * np.sin(TAU * yy / per * 2 + 0.5) + (warp - 0.5) * 60
        edge_r = 440 + 26 * np.sin(TAU * yy / per + 2.0) + (warp - 0.5) * 60
        inside = (xx > edge_l) & (xx < edge_r)
        hole = np.hypot((xx - 420) / 40, periodic_dist(yy, 330, per) / 60) + (warp - 0.5) * 0.6 < 1
        inside &= ~hole
        d1, d2, idx = voronoi_periodic(W, per, 60, 1042)
        e = (d2 - d1) / 2                                          # px to the plate edge
        g = np.clip(e / 16.0, 0, 1)
        rng = np.random.default_rng(1043)
        tone = rng.uniform(0.75, 1.1, 60)[idx]
        plate_h = np.sqrt(1 - (1 - g) ** 2) * (0.75 + 0.25 * r02.periodic_fbm(W, per, 32, 1044, 3))
        edge = pblur_mask(inside.astype(np.float64), 8)
        hgt = plate_h * 0.9 + edge * 0.8
        shade = hillshade(hgt * 26, 0.9)
        chitin = [(0.0, (12, 6, 10)), (0.4, (36, 18, 28)), (0.75, (68, 38, 50)), (1.0, (108, 74, 84))]
        col = ramp_img(chitin, 0.2 + plate_h * tone * 0.7) * (shade * (0.45 + 0.55 * edge))[..., None]
        vein = np.clip(1 - e / 2.2, 0, 1) * edge
        vein_c = np.where((idx % 3 == 0)[..., None], np.array((0, 230, 150)), np.array((140, 80, 240)))
        col = col * (1 - 0.8 * vein[..., None]) + vein[..., None] * vein_c * 0.85
        img = smooth_tex(posterize(raster.to_rgba_image(np.clip(col, 0, 255), inside * 255.0), 20), 3)
        out = img.copy()
        d = ImageDraw.Draw(out)
        for x, y, r in self.PITS:                                    # spawning pits
            for dy in (-per, 0, per):
                d.ellipse([x - r - 4, y + dy - r - 4, x + r + 4, y + dy + r + 4], fill=(70, 30, 66, 255))
                d.ellipse([x - r, y + dy - r, x + r, y + dy + r], fill=(4, 2, 8, 255))
        spine, polyp = shared("spine"), shared("polyp")
        for (x, y), sp in [(t_, spine) for t_ in self.TURRETS] + [(self.MORTAR, polyp)]:
            for dy in (-per, 0, per):
                out.alpha_composite(sprite.shadow_of(sp, opacity=0.6, blur=1.0),
                                    (int(x - sp.width / 2 + 5), int(y + dy - sp.height / 2 + 7)))
                sprite.paste_center(out, sp, x, y + dy)
        return out

    def coil_path(self, tt):
        w = TAU / (N / FPS)
        return 240 + 150 * np.sin(w * tt), 210 + 95 * np.sin(2 * w * tt)

    def layer_images(self, f):
        deep = crossfade(self.deep, self.deep_shift, f)
        far = crossfade(self.far, self.far_shift, f)
        ground = scroll_tex(self.ground_tex, self.s.off("ground", f))
        glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        gd = ImageDraw.Draw(glow)
        for i, (x, yw, r) in enumerate(self.PITS):                   # pulsing pit rims
            k = 0.5 + 0.5 * np.sin(TAU * 2 * f / N + i * 2.1)
            for yy in self.ys("ground", yw, f, margin=40):
                gd.ellipse([x - r - 2, yy - r - 2, x + r + 2, yy + r + 2],
                           outline=(0, 255, 154, int(120 + 135 * k)), width=3)
                gd.ellipse([x - r * 0.5, yy - r * 0.5, x + r * 0.5, yy + r * 0.5],
                           fill=(0, 120, 80, int(60 * k)))
        ground.alpha_composite(raster.glow(glow, 3.0, 1.0))
        low = scroll_tex(self.spores, self.s.off("low-air", f))
        d = ImageDraw.Draw(low)
        for x, y, r, ph in self.sacs:
            for yy in self.ys("low-air", y, f, margin=20):
                xx = x + 6 * np.sin(TAU * f / N + ph)
                d.ellipse([xx - r, yy - r, xx + r, yy + r], fill=(150, 80, 170, 120),
                          outline=(220, 150, 240, 170))
                d.ellipse([xx - r * 0.35, yy - r * 0.35, xx + r * 0.35, yy + r * 0.35],
                          fill=(0, 230, 150, 200))
        for x, y, ph in self.tendrils:
            for yy in self.ys("low-air", y, f, margin=120):
                pts = [(x + 22 * np.sin(j * 0.45 + ph + TAU * f / N), yy - 100 + j * 10) for j in range(21)]
                d.line(pts, fill=(60, 26, 70, 230), width=5)
                d.line(pts, fill=(120, 60, 130, 230), width=2)
        air = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        casters = []
        c = shared("coil")
        sizes = c["sizes"]
        tt = f / FPS
        spac = [0.5 * (sizes[i] + sizes[i + 1]) / 2 for i in range(len(sizes) - 1)]
        pts, heads = rig.chain_at(self.coil_path, tt, spac)
        for i in range(len(pts) - 1, -1, -1):
            x, y = pts[i]
            if i == 0:
                sp = c["head"].get(heads[0], jaw=round(0.5 + 0.5 * np.sin(TAU * 4 * f / N), 1))
            elif i == len(sizes) - 1:
                sp = c["tail"].get(heads[i])
            else:
                sp = c["segs"][sizes[i]].get(heads[i])
            casters.append((sp, x, y))
            sprite.paste_center(air, sp, x, y)
        cx = W + 60 - (W + 120) * ((f / N + 0.3) % 1.0)
        seed_cluster(air, casters, f, cx, 420 + 30 * np.sin(TAU * f / N), ph=1.0)
        for i, (tx, tyw) in enumerate(self.TURRETS):
            def src(ff, tx=tx, tyw=tyw):
                y = (tyw + self.s.off("ground", ff)) % self.TRAVEL
                return (tx, y) if 0 <= y <= H else None
            aimed(air, f, src, self.cast, period=40, speed=6.0, kind="thorn", phase=13 * i)
        mx, myw = self.MORTAR                                            # mortar lob + marker
        age = f % 40
        my = (myw + self.s.off("ground", f - age)) % self.TRAVEL
        if 0 <= my <= H and age < 24:
            tx, ty, _ = self.cast.player_pos(f - age)
            s_ = age / 24
            bx, by = mx + (tx - mx) * s_, my + (ty - my) * s_ - 90 * np.sin(np.pi * s_)
            e3.dashed(air, tx - 10, ty, tx + 10, ty, (168, 255, 42), dash=4)
            e3.orb(air, bx, by, 6, ring=SHOTS[5])
        self.cast.player(air, casters, f)
        shadows = r02.shadow_layer(casters, *SHADOW["air"], opacity=0.5)
        shadows = masked(shadows, ground)
        L = {"deep": deep, "far": far, "ground": ground, "shadows": shadows,
             "low-air": Image.new("RGBA", (W, H), (0, 0, 0, 0)), "air": air}
        L = r03.decorate(L, low, "ground", casters, bank_shadow_opacity=0.25, flyer_opacity=0.3)
        L["high-air"] = scroll_tex(self.wisps, self.s.off("high-air", f))
        return L



SCENES = {"luna": LunaScene, "europa": EuropaScene, "belt": BeltScene, "jovian": JovianScene,
          "vrell-space": VrellSpaceScene}
NOTES = {
    "luna": "LOW SUN FROM THE TOP-LEFT: LONG CRATER SHADOWS WITH A BLUE EARTHSHINE TINT. "
            "MASS-DRIVER SLED: LIGHTS BLINK FAST AS A WARNING, THEN A SLED RACES UP THE RAIL.",
    "europa": "UNDER WATER: DARK EDGES, THE SHIP'S HEADLIGHT CONE LIGHTS THE FLOOR AHEAD. "
              "NAUTILI ORBIT ON THE SUB LAYER (ANTI-SUB TARGETS); JELLIES PULSE MINE RINGS.",
    "belt": "NO TERRAIN: THE ROCK AND ITS STATIONS ARE THE GROUND LAYER; GAPS SHOW SPACE. "
            "RAIL BUNKER: RED TELEGRAPH LINE, THEN A RAIL SHOT DOWN THE PLAYER'S COLUMN.",
    "jovian": "THE CITY CASTS LONG SOFT SHADOWS ONTO THE CLOUD TOPS FAR BELOW. ONE LIGHTNING "
              "FLASH PER LOOP. THREE-WAY FIGHT: ASCENDANCY GUNSHIP + TALONS, A VRELL SPORE BOMBER.",
    "vrell-space": "HIVE SURFACE: DOMED CHITIN PLATES, GLOWING VEINS, PULSING SPAWNING PITS. "
                   "THE COILWYRM ORBITS ON A CLOSED FIGURE-8; BULLETS ARE OUTLINED AND LARGER.",
}


def make_sheet(scene, f=24):
    frame_img = scene.compose(f)
    layers = scene.layer_images(f)
    tw, th = 192, 216
    img = raster.sheet(1140, 690, f"SCENE {scene.slug.upper()} (R06): {scene.title}",
                       "CONCEPT ROUND 06 - MEDIUM ATMOSPHERE")
    speed = scene.TRAVEL * FPS / N
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
        "FACTORS RELATIVE TO GROUND: " + ", ".join(f"{k.upper()} {v:g}" for k, v in
                                                   scene.FACTORS.items()) + ".",
        "ATMOSPHERE INTENSITY: MEDIUM - BANKS COVER ABOUT 20-25 % (ROUND 03 SHOWED THE HEAVY END); "
        "HIGH-AIR WISPS UNDER 40 % OPACITY.",
        NOTES[scene.slug],
    ]
    y = 580
    for line in notes:
        for part in r02.wrap(line, 100):
            raster.draw_text(img, 516, y, part, raster.LABEL)
            y += 11
        y += 5
    return img


def key_sprites(scene):
    """Small but important sprites whose colours must keep a share of the GIF palette."""
    c = scene.cast
    out = list(c.base.player.values()) + [c.base.bolt, c.base.flare] + list(c._u.values())
    for name in ("spine", "polyp"):
        if name in _spr:
            out.append(_spr[name])
    for name in ("scuttler", "seed", "saw", "jelly", "nautilus"):
        if name in _spr:
            out += list(_spr[name].cache.values())[:3]
    if "coil" in _spr:
        out += list(_spr["coil"]["head"].cache.values())[:2]
        out += [s for a in _spr["coil"]["segs"].values() for s in list(a.cache.values())[:1]][:3]
    out += scene.palette_extras()
    return out


def gif_palette(frames, colors, extras, extra_colors=None):
    """GIF palette = reserved bullet colours + a fixed share for the key sprites (player,
    enemies) + an adaptive share for the backgrounds. Large background areas otherwise squeeze
    small sprites' hues out of the palette (the player's blue hull turned grey)."""
    tile = Image.new("RGB", (W, H), (0, 0, 0))
    x = y = row = 0
    for spr in extras:
        if x + spr.width > W:
            x, y, row = 0, y + row, 0
        if y + spr.height > H:
            break
        tile.paste(spr.convert("RGB"), (x, y), spr)
        x, row = x + spr.width, max(row, spr.height)
    extra_colors = extra_colors or max(20, colors // 3)
    ex = tile.crop((0, 0, W, max(1, min(H, y + row))))
    ex_pal = ex.quantize(colors=extra_colors, method=Image.Quantize.MEDIANCUT,
                         dither=Image.Dither.NONE).getpalette()[:3 * extra_colors]
    n_bg = colors - len(r03.RESERVED) - extra_colors
    sample = Image.new("RGB", (W, H * len(frames)))
    for i, fr in enumerate(frames):
        sample.paste(fr.convert("RGB"), (0, i * H))
    bg_pal = sample.quantize(colors=n_bg, method=Image.Quantize.MEDIANCUT,
                             dither=Image.Dither.NONE).getpalette()[:3 * n_bg]
    cols = [tuple(bg_pal[i:i + 3]) for i in range(0, len(bg_pal), 3)]
    cols += [tuple(ex_pal[i:i + 3]) for i in range(0, len(ex_pal), 3)]
    cols += list(r03.RESERVED)
    cols += [cols[-1]] * (256 - len(cols))
    pal = Image.new("RGB", (16, 16))
    pal.putdata(cols[:256])
    return pal


def make_gif(scene, path, colors, max_mb=8.0):
    """Seamless 4 s loop; the bullet colours are reserved in the palette (parallax_r03)."""
    with tempfile.TemporaryDirectory() as tmp:
        frames = []
        for f in range(N):
            img = scene.compose(f)
            img.convert("RGB").save(Path(tmp) / f"f{f:03d}.png")
            if f % 8 == 0:
                frames.append(img)
        while True:
            gif_palette(frames, colors, key_sprites(scene)).save(Path(tmp) / "palette.png")
            cmd = ["ffmpeg", "-v", "error", "-y", "-framerate", str(FPS),
                   "-i", str(Path(tmp) / "f%03d.png"), "-i", str(Path(tmp) / "palette.png"),
                   "-lavfi", "[0][1]paletteuse=dither=none:diff_mode=rectangle",
                   "-loop", "0", str(path)]
            subprocess.run(cmd, check=True)
            if path.stat().st_size <= max_mb * 1e6 or colors <= 72:
                return colors
            colors -= 12


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for slug in sys.argv[1:] or list(SCENES):
        scene = SCENES[slug]()
        png = OUT / f"scene-{slug}-r06-a.png"
        make_sheet(scene).convert("RGB").save(png, optimize=True)
        print("wrote", png.relative_to(ROOT))
        gif = OUT / f"scene-{slug}-r06-a.gif"
        cols = make_gif(scene, gif, scene.GIF_COLORS)
        print("wrote", gif.relative_to(ROOT), f"{gif.stat().st_size / 1e6:.1f} MB, {cols} colours")


if __name__ == "__main__":
    main()
