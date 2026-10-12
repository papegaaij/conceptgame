#!/usr/bin/env python3
"""Production art, two variants: Level 14's root burst (design/campaign/act-2-homefront/
level-14-siege-spire, *Hazards*: "Root bursts", H2 = a; M5 part H batch, concept round 36, step H2; an
a/b, decision H8 = a, pending the user's review). No concept: designed to the README, to the chosen
Siege Spire's root tissue (render/boss_models: the Spire's materials) and to the Geneva scene's old town.

A burst (data-driven `root_bursts`) is a point on the ground, 100-400 px below the top edge, that
scrolls with the ground: a 1.0 s **telegraph** (no collision, nothing to shoot), then the **eruption**
(0.5 s) and the **growth**: a Spine Turret and a Polyp Mortar at the burst's two growth points grow in
over 1.5 s (hard 1.0 s), invulnerable and silent, then fire. The remains (the crater) scroll on.

  a  "Breaking paving": the paving cracks outward from the point (dark jagged cracks with lit edges,
     slabs lifting, a violet glow seeping up through them at the end), dust puffing up out of the
     cracks and rising, then a slate root arches out of the street, throwing up paving chunks and a
     burst of dust; the growths swell out of its two knots
  b  "Creep surge": violet creep tissue surges in from the gutters on both sides (tendrils with
     glowing veins, converging on the point), a spore mist rising off it, then a sac mound swells up
     and two buds split open; the growths swell out of the buds

The growth itself (both): the unit's own frame, scaled 0.35 -> 1 and tinted from the creep's dark
violet to its own colours over the growth (``grow_in``), drawn over the root or mound; no extra
sprite. The Ravager pack's crater at t=72 is a burst without growths (the same frames).

Functions for the backdrop and props step (``--assets`` writes the game's look into assets/; without it
nothing is written there): ``decal_frames(v)`` (the cracks or creep on the ground, 8 frames over
the telegraph, 112x112), ``puff_frames(v)`` (the dust or mist, 12 frames over the telegraph and the
eruption, 128x128, 4 translucency steps), ``root_frames(v)`` (the root or mound erupting, 6 frames
over 0.5 s, 128x96; the last one stays as the crater), ``grow_in(unit, s)`` and ``GROWTH`` (the two
growth points, px from the burst's point).

Outputs (review only): design/campaign/act-2-homefront/level-14-siege-spire/concept/
  root-burst-r36-a.png / .gif, root-burst-r36-b.png / .gif   the frame sets, a time strip of one
  burst on the old town's ground, and a 4.5 s GIF of two bursts scrolling with the ground at 140 px/s
  (the growths' first shots at 2.5 s)

With --assets (step H5, look a until the user picks; ``--assets b`` writes look b instead) the game's
frames go into assets/sprites/: root-burst-decal_0..7, root-burst-puff_0..11, root-burst-root_0..5
(the frame sets above, under the names the game loads), and that look's review is rebuilt.

Run: python3 tools/art/root_burst.py [a] [b] [--assets [a|b]]   (~2 min)
"""
import sys
from concurrent.futures import ProcessPoolExecutor
from functools import lru_cache

import numpy as np
from PIL import Image, ImageDraw

import artkit
from artkit import DESIGN, TAU, sprite

import enemies_r03 as e3  # noqa: E402
from render import boss_models as bm  # noqa: E402
from render import raster, sdf, terrain  # noqa: E402
from render.enemy_rigs import write_gif  # noqa: E402
from render.sdf import (Material, rotate_x, rotate_z, sd_box, sd_capsule, sd_ellipsoid,  # noqa: E402
                        sd_sphere, union)

REVIEW_ROUND = "r36"
CONCEPT = DESIGN / "campaign" / "act-2-homefront" / "level-14-siege-spire" / "concept"
VARIANTS = {"a": "A - BREAKING PAVING", "b": "B - CREEP SURGE"}
W, H = 480, 540
SCROLL = 140.0
TELEGRAPH, ERUPT, GROW = 1.0, 0.5, 1.5
GROWTH = {"spine-turret": (-26, 5), "polyp-mortar": (26, -5)}     # px from the burst's point
DECAL, PUFF, ROOT = 112, 128, (128, 96)
N_DECAL, N_PUFF, N_ROOT = 8, 12, 6
UNIT_PX = 44.0                                  # root model: px per model unit
LIGHT = np.array([-0.5, -0.6, 0.62]) / np.linalg.norm([-0.5, -0.6, 0.62])
VIOLET = np.array([150, 90, 255])
TISSUE = np.array([58, 38, 66])
SEED = 14


def ease(s):
    s = float(np.clip(s, 0, 1))
    return s * s * (3 - 2 * s)


def to_sprite(hi_rgba, factor=4, colors=32):
    """A float RGBA drawn at ``factor``x -> native, 1-bit alpha, one palette."""
    img = sprite.to_image(sprite.downsample(hi_rgba, factor), 0.5)
    return img


# =========================================================================== the decal (telegraph)

def _walk(rng, x, y, a, length, step=3.0, wiggle=0.38):
    pts, s = [(x, y, 0.0)], 0.0
    while s < length:
        a += rng.normal(0, wiggle)
        x, y, s = x + np.cos(a) * step, y + np.sin(a) * step, s + step
        pts.append((x, y, s))
    return pts


@lru_cache(maxsize=None)
def crack_net(seed=SEED):
    """Main cracks from the point and the two growth points, with branches: [(points, width, start)]."""
    rng = np.random.default_rng(seed)
    cracks = []
    origins = [(0.0, 0.0)] + [tuple(map(float, g)) for g in GROWTH.values()]
    for oi, (ox, oy) in enumerate(origins):
        for k in range(5 if oi == 0 else 3):
            a = rng.uniform(0, TAU) if oi else k * TAU / 5 + rng.uniform(-0.4, 0.4)
            pts = _walk(rng, ox, oy, a, rng.uniform(30, 52) if oi == 0 else rng.uniform(14, 26))
            cracks.append((pts, 2.0 if oi == 0 else 1.5, 0.0 if oi == 0 else 0.3))
            for _ in range(2):
                j = int(rng.integers(2, len(pts) - 1))
                bx, by, bs = pts[j]
                sub = _walk(rng, bx, by, a + rng.choice([-1, 1]) * rng.uniform(0.6, 1.2), rng.uniform(8, 18))
                cracks.append(([(x, y, s + bs) for x, y, s in sub], 1.0, 0.2))
    return cracks


@lru_cache(maxsize=None)
def tendrils(seed=SEED):
    """Creep tendrils from the gutters (the sprite's left and right edges) to the point and the growth
    points: [(points, width)]."""
    rng = np.random.default_rng(seed + 1)
    out = []
    targets = [(0.0, 0.0)] + [tuple(map(float, g)) for g in GROWTH.values()]
    for side in (-1, 1):
        for k in range(4):
            x0, y0 = side * DECAL / 2 * 0.98, rng.uniform(-40, 40)
            tx, ty = targets[k % 3]
            pts, x, y, s = [(x0, y0, 0.0)], x0, y0, 0.0
            while np.hypot(tx - x, ty - y) > 4:
                a = np.arctan2(ty - y, tx - x) + rng.normal(0, 0.45)
                x, y, s = x + np.cos(a) * 3, y + np.sin(a) * 3, s + 3
                pts.append((x, y, s))
            out.append((pts, rng.uniform(5, 8)))
    return out


def decal_frames(variant, n=N_DECAL, f=4):
    """The telegraph on the ground: cracks (a) or the creep surge (b), growing over 1.0 s."""
    frames = []
    for k in range(n):
        g = ((k + 1) / n) ** 0.6                                  # quick at first: the telegraph reads
        img = Image.new("RGBA", (DECAL * f, DECAL * f), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        c = DECAL * f / 2
        P = lambda x, y: (c + x * f, c + y * f)                       # noqa: E731
        if variant == "a":
            if k >= 4:                                                 # slabs lifting round the point
                rng = np.random.default_rng(SEED + 7)
                for j in range(7):
                    a = j * TAU / 7 + rng.uniform(-0.3, 0.3)
                    r = 9 + 4 * (k - 4)
                    cx, cy = np.cos(a) * r, np.sin(a) * r
                    quad = [P(cx + np.cos(a + q) * 5, cy + np.sin(a + q) * 4) for q in (0.6, 2.2, 3.8, 5.4)]
                    d.polygon([(x + 3 * f, y + 3 * f) for x, y in quad], fill=(18, 16, 24, 255))
                    lift = 1 + 0.12 * (k - 4)
                    d.polygon(quad, fill=tuple(int(v * lift) for v in (78, 74, 82)) + (255,))
                    d.line(quad[:2] + quad[1:3], fill=(118, 112, 120, 255), width=f)
            glow = max(0.0, (k - 4) / 3)
            for pts, wdt, start in crack_net():
                L = pts[-1][2]
                shown = [(x, y) for x, y, s in pts if s <= (g - start) / (1 - start) * L + 1e-6]
                if len(shown) < 2:
                    continue
                xy = [P(x, y) for x, y in shown]
                d.line([(x - f, y - f) for x, y in xy], fill=(104, 100, 110, 255), width=int(wdt * f))
                core = (np.array([16, 12, 22]) * (1 - glow) + np.array([110, 60, 200]) * glow).astype(int)
                d.line(xy, fill=tuple(core) + (255,), width=int(wdt * f))
        else:
            for pts, wdt in tendrils():
                L = pts[-1][2]
                shown = [(x, y) for x, y, s in pts if s <= g * L + 1e-6]
                if len(shown) < 2:
                    continue
                xy = [P(x, y) for x, y in shown]
                w0 = wdt * f * (0.6 + 0.4 * g)
                ws = [max(f, int(w0 * min(1.0, 0.25 + s / 24))) for _, _, s in pts[:len(xy)]]
                for j in range(len(xy) - 1):                       # tapering in from the gutter
                    a0, a1, w = xy[j], xy[j + 1], ws[j]
                    d.line([(a0[0] + 2 * f, a0[1] + 2 * f), (a1[0] + 2 * f, a1[1] + 2 * f)], fill=(14, 10, 20, 255), width=w)
                for j in range(len(xy) - 1):
                    d.line([xy[j], xy[j + 1]], fill=tuple(TISSUE) + (255,), width=ws[j])
                    d.ellipse([xy[j][0] - ws[j] / 2, xy[j][1] - ws[j] / 2, xy[j][0] + ws[j] / 2, xy[j][1] + ws[j] / 2],
                              fill=tuple(TISSUE) + (255,))
                for j in range(len(xy) - 1):
                    d.line([(xy[j][0] - f, xy[j][1] - f), (xy[j + 1][0] - f, xy[j + 1][1] - f)],
                           fill=(88, 62, 100, 255), width=max(f, ws[j] // 3))
                vein = tuple(int(v * (0.5 + 0.5 * g)) for v in VIOLET) + (255,)
                d.line(xy, fill=vein, width=f)
            if k >= 4:                                                 # the pool at the point
                r = (k - 3) * 5 * f
                for (gx, gy) in [(0, 0)] + list(GROWTH.values()):
                    x, y = P(gx, gy)
                    rr = r * (1.0 if (gx, gy) == (0, 0) else 0.6)
                    d.ellipse([x - rr, y - rr * 0.8, x + rr, y + rr * 0.8], fill=tuple(TISSUE) + (255,))
                    d.ellipse([x - rr * 0.5, y - rr * 0.45, x + rr * 0.3, y + rr * 0.2],
                              fill=(96, 66, 118, 255))
        a = np.array(img).astype(np.float64) / 255
        frames.append(to_sprite(a, f))
    return artkit.quantize_set(frames, 24)


# =========================================================================== puffs (dust / mist)

@lru_cache(maxsize=None)
def puff_plan(variant):
    """(t0, x0, y0, vx, vy, r0, r1, life) per puff: small ones out of the cracks during the telegraph,
    a ring of big ones at the eruption (fewer, slower and wetter for b)."""
    rng = np.random.default_rng(SEED + (3 if variant == "a" else 5))
    plan = []
    n_tel = 22 if variant == "a" else 10
    for j in range(n_tel):
        t0 = (j / n_tel) * (TELEGRAPH - 0.1) + rng.uniform(0, 0.05)
        a, r = rng.uniform(0, TAU), rng.uniform(4, 10 + 30 * j / n_tel)
        plan.append((t0, np.cos(a) * r, np.sin(a) * r, rng.normal(0, 6), rng.uniform(-18, -8),
                     rng.uniform(4, 6), rng.uniform(13, 19), rng.uniform(0.7, 1.0)))
    n_big = 10 if variant == "a" else 6
    for j in range(n_big):
        a = j * TAU / n_big + rng.uniform(-0.2, 0.2)
        sp = rng.uniform(40, 70) if variant == "a" else rng.uniform(18, 30)
        plan.append((TELEGRAPH + rng.uniform(0, 0.08), np.cos(a) * 10, np.sin(a) * 8, np.cos(a) * sp,
                     np.sin(a) * sp * 0.8 - 8, rng.uniform(7, 10), rng.uniform(18, 26), rng.uniform(0.5, 0.6)))
    return plan


def puff_frames(variant, n=N_PUFF, f=2):
    """Dust (a) or spore mist (b) over the telegraph and the eruption (1.5 s), lit from the top left,
    in 4 translucency steps."""
    base = np.array([150, 138, 124]) if variant == "a" else np.array([112, 94, 132])
    yy, xx = np.mgrid[0:PUFF * f, 0:PUFF * f]
    noise = raster.fbm(PUFF * f, PUFF * f, 10 * f, SEED + 11, octaves=3, period=False)
    frames = []
    for k in range(n):
        t = (k + 0.5) * (TELEGRAPH + ERUPT) / n
        rgb = np.zeros((PUFF * f, PUFF * f, 3))
        alpha = np.zeros((PUFF * f, PUFF * f))
        for t0, x0, y0, vx, vy, r0, r1, life in puff_plan(variant):
            age = t - t0
            if not 0 <= age < life:
                continue
            u = age / life
            r = (r0 + (r1 - r0) * (1 - (1 - u) ** 2)) * f
            cx = PUFF * f / 2 + (x0 + vx * age) * f
            cy = PUFF * f / 2 + (y0 + vy * age) * f
            nx, ny = (xx - cx) / r, (yy - cy) / r
            rr = nx ** 2 + ny ** 2
            edge = 1 - rr + (noise - 0.5) * 1.4
            inside = edge > 0
            nz = np.sqrt(np.clip(1 - rr, 0, 1))
            lit = np.clip(nx * LIGHT[0] + ny * LIGHT[1] + nz * LIGHT[2], 0, 1)
            a = np.where(inside, np.clip(edge * 2, 0, 1), 0) * 0.7 * min(1, age / 0.12) * (1 - u ** 2)
            col = base[None, None, :] * (0.5 + 0.65 * lit[..., None])
            rgb = rgb * (1 - a[..., None]) + col * a[..., None]
            alpha = alpha + a * (1 - alpha)
        if variant == "b":                                      # glowing spores in the mist
            rng = np.random.default_rng(SEED + k)
            for _ in range(10):
                x, y = rng.integers(0, PUFF * f, 2)
                if alpha[y, x] > 0.2:
                    rgb[max(0, y - f):y + f, max(0, x - f):x + f] = (210, 160, 255)
        q = artkit.ordered_dither(alpha, 4)
        img = Image.fromarray(np.dstack([np.clip(rgb, 0, 255), q * 255]).astype(np.uint8), "RGBA")
        img = img.resize((PUFF, PUFF), Image.NEAREST)
        frames.append(img)
    return artkit.quantize_set(frames, 24)


# =========================================================================== root / mound (SDF)

def root_mats():
    m = bm.mats("siege-spire", 1.0, body_pattern=bm.radial_ribs(10, 0.08))
    m.append(Material((0.36, 0.34, 0.36), metal=0.0, shininess=12, spec=0.15,
                      pattern=lambda p, n: 0.8 + 0.2 * (np.abs(np.sin(p[:, 0] * 11) * np.sin(p[:, 1] * 9)) > 0.2)))
    return m


STONE = len(bm.mats("siege-spire"))


def _gp(name):
    gx, gy = GROWTH[name]
    return gx / UNIT_PX, -gy / UNIT_PX                         # model units, y up


def root_model(variant, e):
    """The root breaking out (a) or the mound with its buds (b) at emergence ``e`` 0..1."""
    tx, ty = _gp("spine-turret")
    mx, my = _gp("polyp-mortar")
    rng = np.random.default_rng(SEED + 21)
    chunks = [(rng.uniform(0, TAU), rng.uniform(0.55, 1.15), rng.uniform(0.08, 0.14), rng.uniform(-0.7, 0.7))
              for _ in range(9)]

    def scene(p):
        if variant == "a":
            sink = -0.45 * (1 - e)
            pts = []
            for k in range(9):
                s = k / 8
                x = -1.3 + 2.6 * s
                pts.append((x, 0.18 * np.sin(s * 5.0 + 1.0) - 0.05 * x,
                            sink + 0.3 * np.sin(np.pi * s) ** 0.7 - 0.05))
            items = [(sd_capsule(p, pts[k], pts[k + 1], 0.26 - 0.02 * abs(k - 4), 0.26 - 0.02 * abs(k + 1 - 4)), bm.V_BODY)
                     for k in range(8)]
            items += [(sd_sphere(p, (tx, ty, sink + 0.25), 0.3), bm.V_DARK),
                      (sd_sphere(p, (mx, my, sink + 0.25), 0.32), bm.V_DARK)]
            items += [(sd_capsule(p, (pts[k][0], pts[k][1], pts[k][2] + 0.22), (pts[k + 1][0], pts[k + 1][1],
                                                                                  pts[k + 1][2] + 0.22), 0.05, 0.045), bm.V_SAC)
                      for k in range(1, 7)]
            d, m = union(*items, k=0.12)
            for a, r, sz, tilt in chunks:                         # paving chunks thrown up
                c = (np.cos(a) * r * (0.7 + 0.3 * e), np.sin(a) * r * 0.75 * (0.7 + 0.3 * e), 0.02 + 0.12 * e)
                q = rotate_x(rotate_z(p - sdf.vec(*c), a), tilt * e) + sdf.vec(*c)
                d, m = union((d, m), (sd_box(q, c, (sz * 1.4, sz, 0.05), 0.01), STONE))
            return np.maximum(d, -p[:, 2] - 0.01), m
        sw = 0.08 + 0.34 * min(1.0, e / 0.5)                    # b: the mound swells, then the buds open
        items = [(sd_ellipsoid(p, (0, 0, -0.05), (1.15, 0.72, sw)), bm.V_SAC),
                 (sd_ellipsoid(p, (-0.35, 0.25, -0.02), (0.5, 0.35, sw * 0.8)), bm.V_BODY),
                 (sd_ellipsoid(p, (0.45, -0.3, -0.02), (0.45, 0.3, sw * 0.7)), bm.V_BODY)]
        op = max(0.0, (e - 0.45) / 0.55)
        for gx, gy, rad in ((tx, ty, 0.3), (mx, my, 0.33)):
            items.append((sd_sphere(p, (gx, gy, sw - 0.05), rad * 0.7 * (1 - op)), bm.V_SEAM))
            for j in range(5):                                    # bud petals peeling back
                a = j * TAU / 5 + 0.4
                c, s = np.cos(a), np.sin(a)
                spread = 0.05 + 0.3 * op
                items.append((sd_capsule(p, (gx + c * 0.06, gy + s * 0.06, sw - 0.05),
                                         (gx + c * (0.12 + spread), gy + s * (0.12 + spread),
                                          sw + 0.3 * (1 - op) + 0.05), 0.11, 0.03), bm.V_BODY))
        d, m = union(*items, k=0.08)
        return np.maximum(d, -p[:, 2] - 0.01), m
    return scene, root_mats()


def _render_root(job):
    variant, e = job
    w, h = ROOT
    scene, mats = root_model(variant, e)
    hi, factor = artkit.render_hi(scene, mats, (w, h), w / UNIT_PX)
    return artkit.native(hi, factor)


def root_frames(variant, n=N_ROOT):
    jobs = [(variant, (k + 1) / n) for k in range(n)]
    with ProcessPoolExecutor(max_workers=6) as pool:
        frames = list(pool.map(_render_root, jobs))
    return artkit.quantize_set(frames, 40)


# =========================================================================== the growth

def grow_in(unit, s):
    """A growth at share ``s`` of its growth time: scaled 0.35 -> 1 and tinted from the creep's dark
    violet to its own colours."""
    k = ease(s)
    scale = 0.35 + 0.65 * k
    w, h = max(2, int(round(unit.width * scale))), max(2, int(round(unit.height * scale)))
    img = unit.resize((w, h), Image.NEAREST)
    a = np.array(img).astype(np.float64)
    tint = np.array([0.42, 0.3, 0.6]) * (1 - k) + k
    a[..., :3] *= tint
    return Image.fromarray(a.clip(0, 255).astype(np.uint8), "RGBA")


# =========================================================================== review

@lru_cache(maxsize=None)
def plate():
    """The old town under the canopy: the city ground, darkened (low light), tiling vertically."""
    img, _ = terrain.city_ground(W, H, seed=21, period=True, block=56, street=12)
    return terrain.recede(img, amount=0.15, darken=0.72)


class Burst:
    def __init__(self, variant, frames, units, t0, x, y0):
        self.v, self.f, self.u, self.t0, self.x, self.y0 = variant, frames, units, t0, x, y0

    def y(self, t):
        return self.y0 + SCROLL * (t - self.t0)

    def draw(self, img, t, shots):
        age = t - self.t0
        if age < 0:
            return
        x, y = self.x, self.y(t)
        dec, puf, rot = self.f
        sprite.paste_center(img, dec[min(len(dec) - 1, int(age / TELEGRAPH * len(dec)))], x, y)
        if age >= TELEGRAPH:
            k = min(len(rot) - 1, int((age - TELEGRAPH) / ERUPT * len(rot)))
            e3.place(img, rot[k], x, y, "ground")
            for name, (gx, gy) in GROWTH.items():
                s = (age - TELEGRAPH) / GROW
                unit = self.u[name]
                e3.place(img, grow_in(unit, min(1.0, s)), x + gx, y + gy, "ground")
        pk = int(age / (TELEGRAPH + ERUPT) * len(puf))
        if 0 <= pk < len(puf):
            sprite.paste_center(img, puf[pk], x, y - 6)
        if age >= TELEGRAPH + GROW:                             # first shots
            for t_shot in (TELEGRAPH + GROW, TELEGRAPH + GROW + 1.6):
                u = age - t_shot
                if 0 <= u < 1.4:
                    gx, gy = GROWTH["spine-turret"]
                    e3.orb(img, x + gx, y + gy + 14 + 150 * u + SCROLL * 0 * u, 5)


def units():
    return {"spine-turret": artkit.load_frames("spine-turret")[0],
            "polyp-mortar": artkit.load_frames("polyp-mortar")[0]}


def review(variant, frames, us):
    artkit.REVIEW_ROUND = REVIEW_ROUND
    dec, puf, rot = frames
    # a time strip: one burst on the plate, not scrolling, at moments of its life
    strip = []
    for t in (0.15, 0.4, 0.7, 0.95, 1.1, 1.3, 1.8, 2.5):
        cell = plate().crop((160, 200, 320, 360)).copy()
        b = Burst(variant, frames, us, 0.0, 80, 80)
        b.y = lambda t_, b=b: 80
        b.draw(cell, t, [])
        raster.draw_text(cell, 4, 148, f"{t:.2f} S", raster.LABEL)
        strip.append(cell)
    rows = [
        (f"ONE BURST: TELEGRAPH 0-1 S, ERUPTION 1-1.5 S, GROWTH 1-2.5 S, FIRST SHOT 2.5 S", strip, 1, False),
        ("GROUND DECAL (TELEGRAPH, 8 FR OVER 1 S)", dec, 1, False),
        ("PUFFS (12 FR OVER 1.5 S, 4 TRANSLUCENCY STEPS)", puf[:6], 1, False),
        ("PUFFS CONT.", puf[6:], 1, False),
        ("ERUPTION (6 FR OVER 0.5 S; THE LAST STAYS AS THE CRATER)", rot, 2, False),
        ("GROW-IN OF THE SPINE TURRET AND POLYP MORTAR (0, 0.25, 0.5, 0.75, 1)",
         [grow_in(us[n], s) for n in GROWTH for s in (0.0, 0.25, 0.5, 0.75, 1.0)], 2, False),
    ]
    sheet = artkit.review_sheet(f"LEVEL 14 ROOT BURST, VARIANT {VARIANTS[variant]}", rows, width=1500, batch="M5 part H batch")
    png = CONCEPT / f"root-burst-{REVIEW_ROUND}-{variant}.png"
    CONCEPT.mkdir(parents=True, exist_ok=True)
    sheet.convert("RGB").save(png, optimize=True)
    # the GIF: two bursts scrolling with the ground
    bursts = [Burst(variant, frames, us, 0.2, 150, 120), Burst(variant, frames, us, 1.4, 340, 160)]
    gif = []
    for t in np.arange(0, 4.5, 0.05):
        off = int(SCROLL * t) % H
        bg = plate()
        img = Image.new("RGBA", (W, H))
        img.paste(bg.crop((0, H - off, W, H)), (0, 0))
        img.paste(bg.crop((0, 0, W, H - off)), (0, off))
        for b in bursts:
            b.draw(img, t, [])
        e3.place(img, e3.player_sprite(), 240 + 60 * np.sin(t), 480, "ground")
        gif.append(img)
    write_gif(gif, png.with_suffix(".gif"), fps=20, colors=128)
    print(f"review: {png.relative_to(artkit.ROOT)} and .gif")


def make(variant):
    return decal_frames(variant), puff_frames(variant), root_frames(variant)


SCRIPT = "root_burst.py"
SOURCE = artkit.source_note(SCRIPT, "M5 part H batch")
GAME_VARIANT = "a"                              # round 36: look a in the game until the user picks


def write_assets(variant):
    """The game's frames (step H5): sprites/root-burst-decal_0..7 (112 px, over the telegraph),
    root-burst-puff_0..11 (128 px, 4 translucency steps, over the telegraph and the eruption) and
    root-burst-root_0..5 (128 x 96, over the eruption; the last stays as the crater), of ``variant``."""
    dec, puf, rot = make(variant)
    artkit.write_frames("root-burst-decal", dec, SOURCE)
    artkit.write_frames("root-burst-puff", puf, SOURCE)
    artkit.write_frames("root-burst-root", rot, SOURCE)
    print(f"assets: sprites/root-burst-decal_0..{len(dec) - 1}, -puff_0..{len(puf) - 1}, -root_0..{len(rot) - 1} "
          f"(look {variant})")
    return dec, puf, rot


def main(args):
    us = units()
    if "--assets" in args:
        v = next((a for a in args if a in VARIANTS), GAME_VARIANT)
        review(v, write_assets(v), us)
        return
    for v in [a for a in args if a in VARIANTS] or list(VARIANTS):
        review(v, make(v), us)


if __name__ == "__main__":
    main(sys.argv[1:])
