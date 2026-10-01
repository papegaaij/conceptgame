#!/usr/bin/env python3
"""Concept round 07 - Europa under water, redone so it reads as under water at a glance.

Outputs (design/art-direction/concept/):
  scene-europa-r07-a.png / .gif   dense kelp forest: tall swaying kelp leaning away from the
                                  screen centre (it reaches up towards the camera), sea-grass
                                  meadows, anemones and coral fans, caustic light rippling over
                                  the floor, bubble streams from the vent, the domes and the
                                  ship's engines, fish schools turning as a group, marine snow
  scene-europa-r07-b.png / .gif   open water: fewer kelp stands, sandier floor and meadows,
                                  stronger light shafts and caustics, more bubbles and fish

User feedback on round 06: "it wasn't clear to me that Europa is under water. We need some
more hints of it being under water, like some under water vegetation and maybe some air
bubbles." Rules: design/art-direction/README.md (Animation rules - Water - Under water;
Decoration at medium intensity). Builds on scenes_r06.EuropaScene (domes, Vrell coral, kelp
farm, vent, silt, headlight) and uses the chosen enemy-sheet models from enemies_r06
(Driftjelly, Spiral Nautilus) so they match their sheets.
Run: python3 tools/concept/scenes_r07.py [a] [b]
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, str(Path(__file__).resolve().parent))
import enemies_r03 as e3  # noqa: E402
import enemies_r06 as e6  # noqa: E402  (chosen Driftjelly / Spiral Nautilus sprites)
import parallax_r02 as r02  # noqa: E402
import parallax_r03 as r03  # noqa: E402
import scenes_r06 as s6  # noqa: E402
from parallax_r02 import H, W, posterize, scroll_tex  # noqa: E402
from render import raster, sprite  # noqa: E402
from render.config import ROOT  # noqa: E402

OUT = s6.OUT
N, FPS, TAU = s6.N, s6.FPS, s6.TAU
SHOTS = s6.SHOTS
CX, CY = W / 2, H * 0.55            # perspective centre: tall things lean away from it


def lean(x, y, k):
    """Screen offset of a point ``k`` (0..1) of the way up a tall object standing at (x, y):
    things closer to the camera spread away from the screen centre (art-direction rule for
    tall structures)."""
    return (x - CX) * k, (y - CY) * k


def fish_sprite(length, body, back, belly, scale=4):
    """Tiny fish seen from above, pointing +x, drawn at ``scale`` and reduced."""
    w, h = length * scale, max(4, length // 2) * scale
    img = Image.new("RGBA", (w + 2 * scale, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse([0, h * 0.18, w * 0.82, h * 0.82], fill=body + (255,))
    d.ellipse([w * 0.08, h * 0.36, w * 0.74, h * 0.56], fill=back + (255,))       # dark back
    d.polygon([(w * 0.76, h * 0.5), (w + scale, h * 0.12), (w + scale, h * 0.88)], fill=body + (255,))
    d.ellipse([w * 0.06, h * 0.2, w * 0.3, h * 0.42], fill=belly + (255,))         # lit flank
    return img


class FishAngles:
    """A decor fish at 16 headings (pre-rotated once, nearest frame at runtime)."""

    def __init__(self, length, body, back, belly, count=16):
        base = fish_sprite(length, body, back, belly)
        self.count = count
        self.frames = []
        for k in range(count):
            r = base.rotate(-np.degrees(k * TAU / count), resample=Image.BICUBIC, expand=True)
            r = r.resize((max(1, r.width // 4), max(1, r.height // 4)), Image.BOX)
            a = np.array(r)
            a[..., 3] = np.where(a[..., 3] > 100, 255, 0)
            self.frames.append(Image.fromarray(a, "RGBA"))

    def get(self, heading):
        return self.frames[int(round((heading % TAU) / (TAU / self.count))) % self.count]


def bubble(img, x, y, r, alpha=1.0):
    """An air bubble: pale rim, faint body, bright highlight top-left."""
    r = max(0.8, r)
    d = ImageDraw.Draw(img)
    a = int(255 * alpha)
    if r < 1.6:
        d.point([(x, y)], fill=(200, 245, 250, a))
        return
    d.ellipse([x - r, y - r, x + r, y + r], fill=(90, 170, 185, int(70 * alpha)),
              outline=(185, 240, 245, a))
    hr = max(0.6, r * 0.35)
    hx, hy = x - r * 0.38, y - r * 0.38
    d.ellipse([hx - hr, hy - hr, hx + hr, hy + hr], fill=(245, 255, 255, a))


class EuropaR07(s6.EuropaScene):
    title = "EUROPA UNDER WATER - THERA DEEP"
    layers = ["ground", "kelp", "sub", "low-air", "air", "high-air"]
    ORDER = ("ground", "shadows", "kelp", "sub", "low-air", "air", "high-air")
    FACTORS = {"ground": 1.0, "sub": 1.2, "high-air": 2.0}
    WATER = (8, 74, 92)
    GIF_COLORS = 104
    EXTRA_BOXES = [(260, 220, 460, 470), (40, 340, 170, 520)]

    VARIANTS = {
        "a": dict(name="KELP FOREST", kelp=3, grass=0.46, shafts=1.0, caustic=1.0, school=26,
                  floor=[(0.0, (4, 22, 30)), (0.35, (10, 50, 58)), (0.65, (22, 86, 88)),
                         (0.9, (52, 124, 116)), (1.0, (96, 156, 138))]),
        "b": dict(name="OPEN WATER", kelp=1, grass=0.56, shafts=1.6, caustic=1.35, school=36,
                  floor=[(0.0, (8, 32, 40)), (0.35, (22, 70, 76)), (0.65, (48, 112, 108)),
                         (0.9, (98, 154, 136)), (1.0, (150, 186, 158))]),
    }
    FOREST = {   # kelp stands (centre x, world y, radius, strand count)
        3: [(64, 120, 66, 20), (424, 60, 56, 15), (110, 470, 52, 14), (250, 200, 26, 5),
            (440, 340, 40, 9)],
        1: [(70, 120, 46, 8), (430, 470, 34, 6)],
    }

    def __init__(self, variant="a"):
        self.variant = variant
        self.v = self.VARIANTS[variant]
        self.FLOOR = self.v["floor"]
        super().__init__()                  # builds floor (our _floor), silt, shafts ...
        per = self.TRAVEL
        rng = np.random.default_rng(7000 + ord(variant))
        self.strands = []
        for cx, cy, rad, n in self.FOREST[self.v["kelp"]]:
            for _ in range(n):
                a, rr = rng.uniform(0, TAU), rad * np.sqrt(rng.uniform(0, 1))
                self.strands.append(dict(x=cx + rr * np.cos(a), y=(cy + rr * np.sin(a)) % per,
                                         k=rng.uniform(0.22, 0.42), ph=rng.uniform(0, TAU),
                                         w=rng.uniform(2.6, 3.8), hue=rng.uniform(0, 1),
                                         h=rng.uniform(0.7, 1.0)))
        # bubble emitters: (world x, world y, period, life, rise, r0, r1, wobble)
        self.emitters = [(395, 140, 4, 34, 0.42, 1.2, 5.5, 3.0),       # the hot vent
                         (338, 318, 10, 30, 0.30, 0.8, 3.0, 2.0),      # dome air vents
                         (412, 250, 10, 28, 0.28, 0.8, 2.6, 2.0),
                         (300, 425, 8, 30, 0.30, 0.8, 3.0, 2.0)]
        for _ in range(4 if variant == "a" else 7):                    # sea-floor seeps
            self.emitters.append((rng.uniform(20, W - 20), rng.uniform(0, per),
                                  8, 26, 0.30, 0.8, 2.6, 2.4))
        sper = self.s.period("sub")
        self.motes = [(rng.uniform(0, W), rng.uniform(0, sper), rng.uniform(0.3, 1.0))
                      for _ in range(70)]
        self.flakes = [(rng.uniform(0, W), rng.uniform(0, H), rng.uniform(0, 1)) for _ in range(14)]
        self.fishA = FishAngles(12, (150, 196, 204), (54, 92, 110), (230, 250, 250))
        self.fishB = FishAngles(9, (80, 168, 160), (24, 84, 92), (170, 230, 210))
        self.schoolA = [(rng.normal(0, 22), rng.normal(0, 14), rng.uniform(0, TAU))
                        for _ in range(self.v["school"])]
        self.schoolB = [(rng.normal(0, 12), rng.normal(0, 9), rng.uniform(0, TAU))
                        for _ in range(12)]
        silt = [(6, 34, 44), (12, 56, 66), (22, 80, 88)]                 # simpler silt bank
        self.silt = s6.bank_tex(sper, [168, 84, 42, 21], 701, 0.68, 0.14, silt, max_alpha=170,
                                stretch=2, light=4.0, colors=6, levels=3)
        self._caustic_grid()

    # ------------------------------------------------------------------ static floor

    def _floor(self, per):
        rng = np.random.default_rng(711)
        yy, xx = np.mgrid[0:per, 0:W]
        hgt = r02.periodic_fbm(W, per, 112, 712, octaves=3) * 0.6
        ridge = 1 - np.abs(2 * r02.periodic_fbm(W, per, 56, 713, octaves=2) - 1)
        hgt += 0.3 * ridge ** 3
        for _ in range(14):
            cx, cy, r = rng.uniform(0, W), rng.uniform(0, per), rng.uniform(4, 12)
            d = np.hypot(xx - cx, s6.periodic_dist(yy, cy, per)) / r
            hgt += np.sqrt(np.clip(1 - d * d, 0, 1)) * 0.12
        shade = s6.hillshade(hgt * 70, 0.5)
        tt = (hgt - hgt.min()) / (hgt.max() - hgt.min())
        col = s6.ramp_img(self.FLOOR, 0.15 + tt * 0.75) * shade[..., None]
        img = s6.smooth_tex(posterize(raster.to_rgba_image(np.clip(col * 0.9, 0, 255)), 7), 7)
        img = self._meadow(img, per)
        img = s6.smooth_tex(posterize(img, 14), 5)   # crisp runs: speckle is what bloats the GIF
        img = self._reef(img, per)            # Vrell coral (r06)
        img = self._life(img, per)            # anemones, coral fans, starfish
        img = s6.smooth_tex(img, 3)
        img = self._kelp(img, per)            # one kelp-farm plot (r06)
        img = self._city(img, per)            # Thera Deep domes + vent (r06)
        return img

    def _meadow(self, img, per):
        """Sea-grass meadows: a flat green tint over the meadow areas plus clustered tufts of
        blades leaning with the current (clustered, hard-edged strokes keep the GIF small)."""
        mask = r02.periodic_fbm(W, per, 56, 751, octaves=2)
        thr = 1 - self.v["grass"]
        m = np.clip((mask - thr) * 6, 0, 1)
        m = (m > 0.5).astype(np.float64)                                # one flat tint step
        a = np.array(img).astype(np.float64)
        a[..., :3] = a[..., :3] * (1 - 0.32 * m[..., None]) + np.array((40, 112, 66)) * 0.32 * m[..., None]
        out = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")
        lay = Image.new("RGBA", (W, per), (0, 0, 0, 0))
        d = ImageDraw.Draw(lay)
        rng = np.random.default_rng(752)
        greens = [(44, 116, 64), (70, 146, 80), (112, 180, 100)]
        n = 0
        while n < 520:
            x, y = rng.uniform(0, W), rng.uniform(0, per)
            mm = mask[int(y) % per, int(x) % W]
            if mm < thr + 0.02:
                continue
            n += 1
            c = greens[min(2, int((mm - thr) * 8))]
            for _ in range(5):                                          # one tuft
                ln = rng.uniform(4, 8)
                ang = -1.9 + rng.normal(0, 0.3)
                ox = rng.uniform(-1.5, 1.5)
                for dy in (-per, 0, per):
                    d.line([(x + ox, y + dy), (x + ox + ln * np.cos(ang) * 0.6, y + dy + ln * np.sin(ang))],
                           fill=c + (255,), width=1)
        out.alpha_composite(r03.step_alpha(s6.pshadow(lay, 0.4, 0.8), 3), (1, 2))
        out.alpha_composite(lay)
        return out

    def _life(self, img, per):
        """Anemones (radial tentacles), fan corals and starfish - muted, so they never
        compete with enemies and bullets (decoration rule)."""
        ss = 3
        lay = Image.new("RGBA", (W * ss, per * ss), (0, 0, 0, 0))
        d = ImageDraw.Draw(lay)
        rng = np.random.default_rng(761)
        anemone = [((214, 204, 178), (120, 220, 200)), ((176, 124, 136), (232, 196, 200)),
                   ((150, 140, 196), (210, 200, 240)), ((112, 176, 150), (220, 240, 220))]
        spots = [(rng.uniform(10, W - 10), rng.uniform(0, per)) for _ in range(7)]
        for sx, sy in spots:
            for _ in range(int(rng.integers(2, 5))):
                x, y = sx + rng.normal(0, 14), sy + rng.normal(0, 10)
                r = rng.uniform(4, 8)
                body, tip = anemone[int(rng.integers(0, len(anemone)))]
                for dy in (-per, 0, per):
                    for a in np.linspace(0, TAU, int(rng.integers(9, 14)), endpoint=False):
                        aa = a + rng.normal(0, 0.12)
                        ex, ey = x + r * np.cos(aa), y + dy + r * np.sin(aa)
                        d.line([(x * ss, (y + dy) * ss), (ex * ss, ey * ss)], fill=body + (255,),
                               width=int(1.2 * ss))
                        d.ellipse([(ex - 0.7) * ss, (ey - 0.7) * ss, (ex + 0.7) * ss,
                                   (ey + 0.7) * ss], fill=tip + (255,))
                    d.ellipse([(x - r * 0.3) * ss, (y + dy - r * 0.3) * ss, (x + r * 0.3) * ss,
                               (y + dy + r * 0.3) * ss], fill=tuple(int(c * 0.6) for c in body) + (255,))
        fans = [(168, 112, 96), (160, 150, 104), (120, 104, 150)]
        glow = Image.new("RGBA", lay.size, (0, 0, 0, 0))
        g = ImageDraw.Draw(glow)
        for _ in range(5):
            x, y = rng.uniform(10, W - 10), rng.uniform(0, per)
            base = rng.uniform(0, TAU)
            col = fans[int(rng.integers(0, len(fans)))]
            for a in np.linspace(-0.7, 0.7, 6):
                s6.branches(d, g, rng, x, y, base + a, rng.uniform(12, 20), 1.2, 1, col, col, ss,
                            per, step=3, jitter=0.25, tips=False)
        for _ in range(5):                                             # starfish
            x, y, r, a0 = rng.uniform(0, W), rng.uniform(0, per), rng.uniform(3, 4.5), rng.uniform(0, TAU)
            for dy in (-per, 0, per):
                pts = []
                for k in range(10):
                    rr = r if k % 2 == 0 else r * 0.4
                    a = a0 + k * TAU / 10
                    pts.append(((x + rr * np.cos(a)) * ss, (y + dy + rr * np.sin(a)) * ss))
                d.polygon(pts, fill=(196, 150, 118, 255))
        lay = lay.resize((W, per), Image.BOX)
        out = img.copy()
        out.alpha_composite(s6.pshadow(lay, 0.5, 1.0), (2, 3))
        out.alpha_composite(lay)
        return out

    # ------------------------------------------------------------------ animated parts

    def _caustic_grid(self):
        """Two periodic noise fields whose ridges (1 - |2n - 1|, raised to a high power) form
        the bright net of caustic lines. Each frame samples them at a small circular offset, so
        the net shimmers and the loop closes exactly."""
        per = self.TRAVEL
        self._n1 = r02.periodic_fbm(W, per, 56, 791, octaves=2)
        self._n2 = r02.periodic_fbm(W, per, 56, 792, octaves=2)
        yy, xx = np.mgrid[0:H, 0:W]
        self._xx, self._yy = xx.astype(np.float64), yy.astype(np.float64)
        self._xi = xx
        patch = r02.periodic_fbm(W, per, 140, 783, octaves=2)
        self._patch = np.clip((patch - 0.25) * 2.0, 0.45, 1.0)

    def caustics(self, f):
        """Caustic net in ground coordinates (moves with the floor, shimmers in time),
        quantised to three levels so it stays cheap in the GIF."""
        per = self.TRAVEL
        off = int(round(self.s.off("ground", f)))
        t = TAU * f / N
        wy = (np.arange(H) - off) % per
        s1 = (int(round(6 * np.cos(t))), int(round(6 * np.sin(t))))
        s2 = (int(round(6 * np.cos(t + 2.1))), int(round(-6 * np.sin(t + 1.3))))
        rows1, rows2 = (wy + s1[1]) % per, (wy + s2[1]) % per
        a = self._n1[rows1][:, (np.arange(W) + s1[0]) % W]
        b = self._n2[rows2][:, (np.arange(W) + s2[0] + 9) % W]
        r1, r2 = 1 - np.abs(2 * a - 1), 1 - np.abs(2 * b - 1)
        net = np.clip((r1 ** 20 + r2 ** 20) * 1.2, 0, 1) * self._patch[wy]
        return (net > 0.45).astype(np.float64)

    def ground_image(self, f):
        img = super().ground_image(f)                 # floor + Scuttler
        a = np.array(img).astype(np.float64)
        c = self.caustics(f) * self.v["caustic"]
        a[..., :3] += c[..., None] * np.array((46, 76, 66))
        return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")

    def kelp_layer(self, f):
        """Tall kelp: each strand is a curve from its holdfast on the floor to a tip that leans
        away from the screen centre (it rises towards the camera), bends with the current and
        sways with the swell; leafy blades along the stipe and a floating canopy of fronds at the
        top. Returns (kelp image, its shadow on the floor)."""
        ss = 2
        lay = Image.new("RGBA", (W * ss, H * ss), (0, 0, 0, 0))
        sh = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        d, ds = ImageDraw.Draw(lay), ImageDraw.Draw(sh)
        t = TAU * f / N
        stems = [(34, 50, 22), (54, 74, 28), (80, 102, 36), (112, 132, 46)]
        blade = [(58, 86, 30), (88, 122, 40), (124, 156, 54), (162, 186, 76)]

        def leaf(x, y, ang, ln, wd, col):
            ca, sa = np.cos(ang), np.sin(ang)
            pts = []
            for u in np.linspace(0, 1, 7):
                pts.append((x + ca * ln * u - sa * wd * np.sin(np.pi * u), y + sa * ln * u + ca * wd * np.sin(np.pi * u)))
            for u in np.linspace(1, 0, 7):
                pts.append((x + ca * ln * u + sa * wd * np.sin(np.pi * u) * 0.5, y + sa * ln * u - ca * wd * np.sin(np.pi * u) * 0.5))
            d.polygon([(px * ss, py * ss) for px, py in pts], fill=col + (255,))
            d.line([(x * ss, y * ss), ((x + ca * ln * 0.8) * ss, (y + sa * ln * 0.8) * ss)],
                   fill=tuple(int(c * 0.7) for c in col) + (255,), width=1)

        for s in sorted(self.strands, key=lambda s: s["k"]):
            for by in self.ys("ground", s["y"], f, margin=200):
                bx = s["x"]
                lx, ly = lean(bx, by, s["k"])
                hgt = s["h"]
                swx = 10 * np.sin(2 * t + s["ph"]) + 4 * np.sin(3 * t + s["ph"] * 1.7)
                swy = 6 * np.cos(2 * t + s["ph"] * 0.7)
                cur = (-26 * hgt, -34 * hgt)                       # the current bends it
                tx, ty = bx + lx + cur[0] + swx, by + ly + cur[1] + swy
                mx, my = bx + lx * 0.45 + cur[0] * 0.2 + swx * 0.25, by + ly * 0.45 + cur[1] * 0.2 + swy * 0.25
                pts = []
                for i in range(17):
                    u = i / 16
                    x = (1 - u) ** 2 * bx + 2 * (1 - u) * u * mx + u * u * tx
                    y = (1 - u) ** 2 * by + 2 * (1 - u) * u * my + u * u * ty
                    pts.append((x, y, u))
                for (x0, y0, u0), (x1, y1, u1) in zip(pts, pts[1:]):
                    c = stems[min(3, int(u1 * 3.8))]
                    wdt = s["w"] * (1.0 - 0.4 * u1)
                    d.line([(x0 * ss, y0 * ss), (x1 * ss, y1 * ss)], fill=c + (255,),
                           width=max(1, int(wdt * ss)))
                    if u1 < 0.75:
                        o = 10 * u1
                        ds.line([(x0 + o + 2, y0 + o * 1.3 + 3), (x1 + o + 2, y1 + o * 1.3 + 3)],
                                fill=(0, 12, 16, int(80 * (1 - u1))), width=3)
                for i in range(2, 16):                                  # blades along the stipe
                    x, y, u = pts[i]
                    px, py = pts[i - 1][0], pts[i - 1][1]
                    side = 1.0 if i % 2 else -1.0
                    ang = np.arctan2(y - py, x - px) + side * (0.9 - 0.3 * u)
                    ang += 0.3 * np.sin(2 * t + s["ph"] + i * 0.7)
                    c = blade[min(3, int(u * 3.2 + s["hue"] * 0.6))]
                    leaf(x, y, ang, 5 + 7 * u, 1.6 + 1.6 * u, c)
                    if i % 3 == 0:
                        r = 1.0 + 0.7 * u
                        d.ellipse([(x - r) * ss, (y - r) * ss, (x + r) * ss, (y + r) * ss],
                                  fill=(176, 196, 104, 255))
                tdir = np.arctan2(ty - pts[-3][1], tx - pts[-3][0])     # canopy at the top
                for j in range(7):
                    ang = tdir + (j - 3) * 0.42 + 0.25 * np.sin(2 * t + s["ph"] + j)
                    c = blade[2 + (j % 2) if s["hue"] > 0.4 else 1 + (j % 3)]
                    leaf(tx, ty, ang, 11 + 5 * ((j * 7) % 3), 2.6, c)
        lay = lay.resize((W, H), Image.BOX)
        lay = s6.tinted(lay, self.WATER, 0.16)
        a = np.array(lay)
        a[..., 3] = np.where(a[..., 3] > 110, 255, 0)           # hard edges, few colours
        lay = posterize(Image.fromarray(a, "RGBA"), 12)
        sh = r03.step_alpha(sh.filter(ImageFilter.GaussianBlur(1.4)), 3)
        return lay, sh

    def bubbles_layer(self, f):
        img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        for ex, ey, period, life, rise, r0, r1, wob in self.emitters:
            for k in range(N // period):
                age = (f - k * period) % N
                if age >= life:
                    continue
                u = age / life
                f0 = f - age
                for by in self.ys("ground", ey, f0, margin=60):
                    by = by + (self.s.off("ground", f) - self.s.off("ground", f0))
                    lx, ly = lean(ex, by, rise * u)
                    wx = wob * np.sin(age * 0.55 + k * 1.7) * (0.4 + u)
                    jx = (8 * np.sin(k * 2.3 + ex) + 5 * np.sin(k * 5.1)) * (0.3 + u)
                    jy = (7 * np.cos(k * 3.7 + ey) + 4 * np.sin(k * 1.9)) * (0.3 + u)
                    x, y = ex + lx + wx + jx, by + ly - 4 * u + jy
                    pop = u > 0.92
                    rk = 0.6 + 0.8 * ((k * 7919) % 11) / 10                # mixed sizes
                    bubble(img, x, y, (r0 + (r1 - r0) * u) * rk * (1.3 if pop else 1.0),
                           alpha=0.55 if pop else 1.0)
        # engine bubbles: released behind the ship, they stay in the water (move down with
        # it), wobble and grow as they rise towards the surface
        drift = self.TRAVEL * 1.1 / N
        for k in range(N // 2):
            age = (f - 2 * k) % N
            if age >= 22:
                continue
            u = age / 22
            px, py, _ = self.cast.player_pos(f - age)
            for side in (-1, 1):
                x = px + side * 6 + 2.2 * np.sin(age * 0.7 + k + side) * (0.5 + u)
                y = py + 26 + age * drift
                bubble(img, x, y, 0.9 + 2.1 * u, alpha=1.0 - 0.4 * u)
        return img

    def school_pos(self, f, which):
        tt = TAU * f / N
        if which == "A":                                    # figure-8: the school turns
            return 240 + 150 * np.sin(tt), 250 + 90 * np.sin(2 * tt), \
                (150 * np.cos(tt), 180 * np.cos(2 * tt))
        return 300 - 120 * np.cos(tt), 430 + 50 * np.sin(tt), (120 * np.sin(tt), 50 * np.cos(tt))

    def draw_school(self, img, f, which):
        x0, y0, (vx, vy) = self.school_pos(f, which)
        heading = np.arctan2(vy, vx)
        ca, sa = np.cos(heading), np.sin(heading)
        fish = self.fishA if which == "A" else self.fishB
        members = self.schoolA if which == "A" else self.schoolB
        t = TAU * f / N
        for ox, oy, ph in members:
            lag = 0.06 * (ox / 20)                          # rear fish turn a moment later
            x1, y1, (vx1, vy1) = self.school_pos(f - lag * N / TAU, which)
            h = np.arctan2(vy1, vx1) + 0.18 * np.sin(4 * t + ph)
            x = x0 + ox * ca - oy * sa + 2 * np.sin(3 * t + ph)
            y = y0 + ox * sa + oy * ca + 2 * np.cos(2 * t + ph)
            sprite.paste_center(img, fish.get(h), x, y)

    def sub_layer(self, f):
        sub = scroll_tex(self.silt, self.s.off("sub", f))
        d = ImageDraw.Draw(sub)
        t = TAU * f / N
        for x, y, b in self.motes:                         # marine snow
            for yy in self.ys("sub", y, f, margin=10):
                d.point([(x + 3 * np.sin(yy * 0.03 + b * 9), yy)],
                        fill=(170, 220, 220, int(80 + 120 * b)))
        vx, vyw = self.vent                                 # vent smoke (r06)
        for k in range(6):
            age = ((f + k * 13) % N) / N
            for yy in self.ys("ground", vyw, f, margin=200):
                cx, cy, r = vx - 50 * age, yy + 20 * age, 8 + 26 * age
                blob = Image.new("RGBA", (int(2 * r + 4), int(2 * r + 4)), (0, 0, 0, 0))
                ImageDraw.Draw(blob).ellipse([2, 2, 2 * r + 2, 2 * r + 2],
                                             fill=(16, 26, 30, int(140 * (1 - age))))
                blob = r03.step_alpha(blob.filter(ImageFilter.GaussianBlur(3)), 4)
                sprite.paste_center(sub, blob, cx, cy)
        self.draw_school(sub, f, "A")
        self.draw_school(sub, f, "B")
        casters = []
        for i, (cx, cy, rx) in enumerate(((120, 120, 30), (380, 200, 24), (240, 360, 36))):
            a = t + i * 2.0
            x, y = cx + rx * np.cos(a), cy + rx * 0.6 * np.sin(a)
            pulse = round(0.5 + 0.5 * np.sin(4 * t + i), 1)
            sp = e6.submerged(e6.jly.get(0.0, pulse=pulse))
            casters.append((sp, x, y))
            sprite.paste_center(sub, sp, x, y)
            sub = raster.add_light(sub, x, y, 12, (168, 255, 42), 0.25 + 0.25 * pulse)
        px, py, _ = self.cast.player_pos(f)
        for k in range(2):
            x, y, a = self.nautilus_pos(f, k)
            sgn = 1 if k == 0 else -1
            unc = (f + 20 * k) % 40 < 10
            aim = np.arctan2(py - y, px - x)
            roll = -sgn * TAU * 3 * f / N
            sp = e6.nau_shell.get(aim if unc else roll)
            casters.append((sp, x, y))
            sprite.paste_center(sub, sp, x, y)
            if unc:
                cx_, cy_ = x + np.cos(aim) * e6.NAU_N * 0.42, y + np.sin(aim) * e6.NAU_N * 0.42
                sprite.paste_center(sub, e6.nau_crown.get(aim, phase=(f // 3) % 6), cx_, cy_)
            sub = raster.add_light(sub, x, y, 16, (154, 77, 255), 0.8 if unc else 0.35)
        self._sub_casters = casters
        return sub

    def light(self, img, f, strength=1.0):
        """Under the ice: edges fade into the water colour (depth haze, not black), the ship's
        headlight cone brightens the way ahead."""
        a = np.array(img).astype(np.float64)
        px, py, _ = self.cast.player_pos(f)
        yy, xx = self._yy, self._xx
        dy = py - yy
        cone = np.clip(1 - np.abs(xx - px) / (24 + dy * 0.42 + 1e-6), 0, 1) * (dy > -10)
        cone *= np.clip(1 - dy / 420, 0, 1) ** 0.8
        vig = np.clip(self.vignette * 1.1, 0.5, 1.0)
        k = np.round(vig * 4) / 4 * (1 + 0.5 * (cone > 0.3) * strength)   # two-step headlight
        haze = np.round(np.clip(1 - vig, 0, 1) * 4)[..., None] / 4 * 0.7
        a[..., :3] = a[..., :3] * k[..., None] * (1 - haze) + np.array(self.WATER) * haze * 1.4
        return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")

    def layer_images(self, f):
        ground = self.ground_image(f)
        kelp, kelp_sh = self.kelp_layer(f)
        sub = self.sub_layer(f)
        shadows = r02.shadow_layer(self._sub_casters, *s6.SHADOW["sub"], opacity=0.3, blur=2.5)
        shadows.alpha_composite(kelp_sh)
        air = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        casters = []
        for k in range(2):
            s6.aimed(air, f, lambda ff, k=k: self.nautilus_pos(ff, k)[:2], self.cast, period=40,
                     speed=4.5, kind="big", phase=6 + 20 * k, life=40)
        for i, (cx, cy, rx) in enumerate(((120, 120, 30), (380, 200, 24))):
            age = (f + 30 * i) % 40
            if age < 14:                                    # Driftjelly mine-ring pulse
                a = TAU * (f - age) / N + i * 2.0
                x, y = cx + rx * np.cos(a), cy + rx * 0.6 * np.sin(a)
                for j in range(8):
                    an = TAU * j / 8
                    e3.orb(air, x + (12 + age * 4.5) * np.cos(an), y + (12 + age * 4.5) * np.sin(an),
                           5, ring=SHOTS[5])
        self.cast.player(air, casters, f)
        shadows.alpha_composite(r02.shadow_layer(casters, *s6.SHADOW["air"], opacity=0.3, blur=2.5))
        high = scroll_tex(self.shafts, self.s.off("high-air", f))
        if self.v["shafts"] != 1.0:
            h = np.array(high).astype(np.float64)
            h[..., 3] = np.clip(h[..., 3] * self.v["shafts"], 0, 110)
            high = Image.fromarray(h.astype(np.uint8), "RGBA")
        d = ImageDraw.Draw(high)
        for x, y, b in self.flakes:                         # big soft particles close to the lens
            yy = (y + self.TRAVEL * 2.4 * f / N) % (H + 40) - 20
            r = 1.5 + 1.5 * b
            d.ellipse([x - r, yy - r, x + r, yy + r], fill=(190, 230, 230, int(40 + 50 * b)))
        tint = lambda im, amt: s6.tinted(im, self.WATER, amt)
        return {"ground": self.light(ground, f), "shadows": shadows,
                "kelp": self.light(kelp, f, 0.8), "sub": self.light(tint(sub, 0.16), f, 0.7),
                "low-air": self.bubbles_layer(f), "air": air, "high-air": high}

    def palette_extras(self):
        """Keep the colours of the small cues (bubbles, fish, kelp blades, jellies, nautili)."""
        extra = super().palette_extras()
        extra += [self.fishA.get(0.0), self.fishB.get(0.0)]
        extra += [e6.submerged(e6.jly.get(0.0, pulse=0.5)), e6.nau_shell.get(0.0)]
        b = Image.new("RGBA", (24, 12), (0, 0, 0, 0))
        bubble(b, 6, 6, 4.0)
        bubble(b, 17, 6, 2.5)
        extra.append(b)
        return extra

    notes = {"ground": "X1.0 FLOOR + CAUSTICS", "kelp": "TIPS LEAN TO CAMERA",
             "sub": "X1.2 SILT, FISH, NAUTILI", "low-air": "RISING BUBBLES",
             "air": "PLAY PLANE (IN WATER)", "high-air": "X2.0 SHAFTS, PARTICLES"}


def make_sheet(scene, f=24):
    frame_img = scene.compose(f)
    layers = scene.layer_images(f)
    tw, th = 192, 216
    img = raster.sheet(1140, 690, f"SCENE EUROPA (R07-{scene.variant.upper()}): {scene.v['name']}",
                       "CONCEPT ROUND 07 - UNDER WATER")
    speed = scene.TRAVEL * FPS / N
    raster.draw_text(img, 16, 38, f"PLAY FIELD 480X540 AT 1X - FLOOR SCROLLS {speed:.0f} PX/S",
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
        "UNDER-WATER CUES (ROUND 06 FEEDBACK): SWAYING KELP AND SEA GRASS, ANEMONES AND FAN CORALS, "
        "RISING BUBBLE STREAMS THAT GROW AS THEY RISE, CAUSTIC LIGHT RIPPLING OVER THE FLOOR, "
        "LIGHT SHAFTS, FISH SCHOOLS, MARINE SNOW, DEPTH HAZE IN THE WATER COLOUR.",
        "TALL KELP AND RISING BUBBLES LEAN AWAY FROM THE SCREEN CENTRE (THEY COME TOWARDS THE "
        "CAMERA), THE SAME PERSPECTIVE RULE AS TALL TOWERS. ATMOSPHERE INTENSITY: MEDIUM.",
        "UNDER WATER: THE SHIP'S HEADLIGHT CONE LIGHTS THE WAY AHEAD; NAUTILI ORBIT ON THE SUB "
        "LAYER (ANTI-SUB TARGETS); JELLIES PULSE MINE RINGS.",
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
    variants = [a for a in sys.argv[1:] if a in EuropaR07.VARIANTS] or ["a", "b"]
    for v in variants:
        scene = EuropaR07(v)
        png = OUT / f"scene-europa-r07-{v}.png"
        make_sheet(scene).convert("RGB").save(png, optimize=True)
        print("wrote", png.relative_to(ROOT))
        if "--sheet" in sys.argv:
            continue
        gif = OUT / f"scene-europa-r07-{v}.gif"
        cols = s6.make_gif(scene, gif, scene.GIF_COLORS)
        print("wrote", gif.relative_to(ROOT), f"{gif.stat().st_size / 1e6:.1f} MB, {cols} colours")


if __name__ == "__main__":
    main()
