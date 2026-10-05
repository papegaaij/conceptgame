#!/usr/bin/env python3
"""Concept round 23 - Level 06's parts without a chosen concept, two variants each (user decision
D8 of M4 part F; design/campaign/act-1-first-contact/level-06-farside and design/enemies/air/mantis).

Outputs:
  design/enemies/air/mantis/concept/
    mantis-beam-r23-a.png / .gif   the Mantis's beam and arc telegraph, variant A "hot wire": a
                                   tileable beam strip (16x12: white-hot 2 px core, 6 px crimson
                                   body, stepped halo), a round tip flare (12x12) and an eye flare
                                   (14x14, 3 frames); the telegraph as crimson dots on the reach arc
                                   (5x5) and dashed edge lines (6x2 dashes), pulsing
    mantis-beam-r23-b.png / .gif   variant B "charged lance": a thinner beam strip with energy knots
                                   running outwards (32x10, 4 frames), an impact spark at the tip
                                   (16x16, 4 frames) and an eye ring (16x16); the telegraph as a
                                   faint filled wedge with a bright rim on the arc and the edges
                                   (one pre-rendered 70 deg fan, 300 px radius; 90 deg on hard)
  design/campaign/act-1-first-contact/level-06-farside/concept/
    ore-cart-r23-a.png / -b.png      the abandoned ore cart on the rail (28x40, ground, 3 HP, medium
                                     salvage): intact, damaged, wrecked; A an open rust ore tub
                                     heaped with ore, B a closed hopper car with a hatched lid
    survey-cache-r23-a.png / -b.png  the CDF survey cache in the dark crater (32x24, hidden crate,
                                     3 hits): closed, hit, opened; A a ribbed field case with three
                                     retro-reflector markers that flash only when light hits them,
                                     B a half-buried instrument drum with three dim amber blinkers
    data-core-r23-a.png / -b.png     the airlock terminal at Daedalus Gate (40x32, 2 hits) and its
                                     released core (16x16 pickup): A a pedestal console with a teal
                                     screen and a hazard-striped core bay, the core a cyan cartridge;
                                     B a wall cabinet with LED rows and a socketed amber core orb

Every sheet shows the frames at 1x and 3x on lit regolith (ground_targets_r17.py's strip), then an
in-context view on the darkened Luna ground of Level 06: the production Luna ground (Level 05's
mass-driver field and mare tiles from assets/backdrop/level-05/, the rail included) multiplied by
a light map as the game's FarsideLooks draws it (cool ambient, warm lamp and dome pools, the cool
headlight cone), the glow drawn after it at full brightness.

The solid props are top-down SDF renders (render/sdf.py, the fixed top-left key light, 1 world
unit = 1 px, 4x supersampled, 32 colours) with the station kit's materials and the loot amber of
ground_targets_r17.py (imported unchanged) and ground_targets.py's light rim and scorch. The beam
parts are 2D light fields supersampled 4x, premultiplied on black (additive). The Mantis in the
beam's context is the chosen round-04 model (enemies_r04.py) turned in the model to face inward.

Run: python3 tools/concept/props_r23.py [mantis-beam] [ore-cart] [survey-cache] [data-core]
(about a minute; the Mantis render alone ~20 s)
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import enemies_r03 as e3  # noqa: E402  (the concept render cache, the player sprite)
import enemies_r04 as e4  # noqa: E402  (the chosen Mantis colours)
import ground_targets as gt  # noqa: E402  (light rim, scorch)
import ground_targets_r17 as r17  # noqa: E402  (kit materials with the loot amber, regolith strip)
from render import enemy_models as em  # noqa: E402
from render import raster, sdf, sprite  # noqa: E402
from render.config import ROOT  # noqa: E402
from render.sdf import (Material, rotate_x, rotate_z, sd_box, sd_capsule, sd_cylinder_x,  # noqa: E402
                        sd_cylinder_y, sd_cylinder_z, sd_ellipsoid, sd_sphere, subtract, union)

L06 = ROOT / "design" / "campaign" / "act-1-first-contact" / "level-06-farside" / "concept"
MANTIS = ROOT / "design" / "enemies" / "air" / "mantis" / "concept"
BACK = ROOT / "assets" / "backdrop" / "level-05"
RAIL_X = 432                                     # backdrop_l04.py: the rail's x in its tile sets
FACTOR, COLOURS = 4, 32
GAP = 24

# kit indices from ground_targets_r17.materials, then this round's own
HULL, DARK, SOLAR, ACCENT, RED, WARM, GLASS = range(7)
AMBER, BAG, SINTER, VOID, CHAR, CORR = range(7, 13)
RUST, ORE, RETRO, SCREEN, LED, CORE, OLIVE = range(13, 20)

AMBIENT = np.array([0.10, 0.10, 0.12])           # the darkness's ambient light (cool)
LAMP = np.array([1.0, 0.82, 0.55])               # FarsideLooks.LAMP
HEADLIGHT = np.array([0.72, 0.84, 1.0])          # FarsideLooks.HEADLIGHT
CRIMSON = np.array([255, 48, 56]) / 255          # the Mantis glow, #ff3038


def materials(state, lit_core=True, leds=True):
    mats = r17.materials(state)
    burnt = state == 2
    return mats + [
        Material((0.36, 0.17, 0.09) if burnt else (0.50, 0.24, 0.12), metal=0.3, shininess=20, spec=0.25),
        Material((0.40, 0.36, 0.32), metal=0.1, shininess=8, spec=0.1),                 # ore
        Material((0.80, 0.80, 0.76), metal=0.9, shininess=140, spec=1.4),               # retro marker
        Material((0.04, 0.10, 0.09), emission=(0.25, 0.95, 0.75)) if lit_core
        else Material((0.05, 0.06, 0.06), metal=0.6, shininess=90, spec=0.9),           # screen
        Material((0.2, 0.1, 0.02), emission=(1.6, 0.75, 0.15)) if leds
        else Material((0.15, 0.1, 0.06), shininess=40, spec=0.4),                       # amber LED
        Material((0.05, 0.2, 0.25), emission=(0.3, 1.3, 1.5)),                          # data core
        Material((0.30, 0.33, 0.27), metal=0.2, shininess=25, spec=0.3),                # CDF olive
    ]


def render(scene, mats, size, rim=True):
    w, h = size
    hi = sdf.render(scene, mats, (w * FACTOR, h * FACTOR), float(max(w, h)) if w >= h else float(w),
                    z_top=max(w, h), steps=150)
    arr = np.array(sprite.make_sprite(hi, FACTOR, COLOURS, crisp=60)).astype(np.float64)
    return gt.light_rim(arr) if rim else arr


def damage(arr, s, seed):
    if s >= 1:
        h, w = arr.shape[:2]
        rng = np.random.default_rng(seed + s)
        spots = [(rng.uniform(0.2, 0.8) * w, rng.uniform(0.2, 0.8) * h, rng.uniform(3, 5.5)) for _ in range(2 * s)]
        arr = gt.scorch(arr, spots, seed + 7 * s)
    return arr


def img(arr):
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")


# --------------------------------------------------------------------------- light map (game-like)

def radial(xx, yy, x, y, r):
    a = np.clip(1 - np.hypot(xx - x, yy - y) / r, 0, 1)
    return a * a * (3 - 2 * a)


def light_map(w, h, pools=(), cone=None):
    """FarsideLooks.darken: the ambient plus additive pools [(x, y, r, colour)] and the headlight
    cone (x, y of the nose, length, angle), image rows running down."""
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64) + 0.5
    light = np.zeros((h, w, 3)) + AMBIENT
    for x, y, r, col in pools:
        light += radial(xx, yy, x, y, r)[..., None] * np.array(col)
    if cone:
        cx, cy, length, angle = cone
        along = (cy - yy) / length
        off = np.abs(np.arctan2(xx - cx, cy - yy))
        side = np.clip((1 - off / (angle / 2)) * 3, 0, 1)
        a = np.where((along > 0) & (along < 1), side * (1 - along * along), 0)
        light += a[..., None] * HEADLIGHT
    return np.clip(light, 0, 1)


def darken(ground, light):
    arr = np.array(ground).astype(np.float64)
    arr[..., :3] *= light
    return arr


def add(base, glow, x, y):
    """Add a premultiplied glow image (rgb on black) at (x, y), clipped."""
    g = np.array(glow).astype(np.float64)[..., :3] if isinstance(glow, Image.Image) else glow[..., :3]
    h, w = g.shape[:2]
    H, W = base.shape[:2]
    x0, y0, x1, y1 = max(0, x), max(0, y), min(W, x + w), min(H, y + h)
    if x1 <= x0 or y1 <= y0:
        return base
    base[y0:y1, x0:x1, :3] = np.clip(base[y0:y1, x0:x1, :3] + g[y0 - y:y1 - y, x0 - x:x1 - x], 0, 255)
    return base


def over(base, sprite_img, x, y, shade=None):
    """Alpha-composite a sprite (optionally multiplied by a light map patch)."""
    s = np.array(sprite_img).astype(np.float64)
    if shade is not None:
        s[..., :3] *= shade[y:y + s.shape[0], x:x + s.shape[1]]
    out = Image.fromarray(np.clip(base, 0, 255).astype(np.uint8), "RGBA")
    out.alpha_composite(img(s), (x, y))
    return np.array(out).astype(np.float64)


def shadowed(base, sprite_img, x, y, light):
    sh = sprite.shadow_of(sprite_img, opacity=0.55, blur=1.2)
    base = over(base, sh, x + 3, y + 4)
    return over(base, sprite_img, x, y, light)


# --------------------------------------------------------------------------- 2D light fields

def field(w, h, fn, ss=4):
    """Supersample fn(x, y) -> (r, g, b) intensity arrays (0..1+) over a w x h sprite, pixel
    centres at +0.5; returns the premultiplied RGBA (alpha 1 where it adds light)."""
    yy, xx = np.mgrid[0:h * ss, 0:w * ss].astype(np.float64)
    rgb = fn((xx + 0.5) / ss, (yy + 0.5) / ss)
    rgb = rgb.reshape(h, ss, w, ss, 3).mean(axis=(1, 3))
    rgb = np.clip(rgb, 0, 1) * 255
    alpha = (rgb.max(axis=2) > 2).astype(np.float64) * 255
    return Image.fromarray(np.dstack([rgb, alpha]).astype(np.uint8), "RGBA")


def colour(t):
    """Intensity t in 0..1: black to crimson at 0.6, then to white-hot."""
    t = np.clip(t, 0, 1)[..., None]
    hot = np.array([1.0, 0.92, 0.9])
    return np.where(t < 0.6, CRIMSON * (t / 0.6), CRIMSON * (1 - (t - 0.6) / 0.4) + hot * ((t - 0.6) / 0.4))


def steps(v, levels=(0.15, 0.3, 0.55)):
    """Step a soft halo to a few translucency levels (artkit.stepped_alpha's idea)."""
    out = np.zeros_like(v)
    for lv in levels:
        out = np.where(v >= lv, lv, out)
    return np.where(v >= 0.8, v, out)


def beam_a(frame):
    def fn(x, y):
        d = np.abs(y - 6)
        shimmer = 1 + 0.08 * np.sin(2 * np.pi * (x / 16 + frame / 3))
        core = np.clip(1.6 - d, 0, 1)
        body = np.clip(3.4 - d, 0, 1)
        halo = steps(np.clip(1 - (d - 3) / 3.2, 0, 1) * 0.6)
        t = np.maximum(np.maximum(core, body * 0.62 * shimmer), halo * 0.5)
        return colour(t) * np.clip(t * 1.6, 0, 1)[..., None]
    return field(16, 12, fn)


def tip_a():
    def fn(x, y):
        d = np.hypot(x - 6, y - 6)
        t = np.maximum(np.clip(2.2 - d, 0, 1), steps(np.clip(1 - d / 6, 0, 1) * 0.75))
        return colour(t) * np.clip(t * 1.6, 0, 1)[..., None]
    return field(12, 12, fn)


def eye_a(frame):
    def fn(x, y):
        d = np.hypot(x - 7, y - 7)
        r = 2.0 + frame * 0.6
        rays = 0.5 + 0.5 * np.cos(4 * np.arctan2(y - 7, x - 7) + frame)
        t = np.maximum(np.clip(r - d, 0, 1), steps(np.clip(1 - d / 7, 0, 1) * (0.5 + 0.3 * rays)))
        return colour(t) * np.clip(t * 1.6, 0, 1)[..., None]
    return field(14, 14, fn)


def dot_a():
    def fn(x, y):
        d = np.hypot(x - 2.5, y - 2.5)
        t = np.maximum(np.clip(1.6 - d, 0, 1) * 0.75, steps(np.clip(1 - d / 2.6, 0, 1) * 0.5))
        return colour(t) * np.clip(t * 1.6, 0, 1)[..., None]
    return field(5, 5, fn)


def dash_a():
    return field(6, 2, lambda x, y: colour(np.full_like(x, 0.55)) * 0.9)


def beam_b(frame):
    def fn(x, y):
        d = np.abs(y - 5)
        phase = (x - frame * 4) % 16
        knot = np.exp(-((phase - 8) / 2.2) ** 2)
        core = np.clip(1.1 - d, 0, 1) * (0.75 + 0.25 * knot)
        body = np.clip(2.4 - d + knot * 1.0, 0, 1)
        halo = steps(np.clip(1 - (d - 2) / 3, 0, 1) * (0.32 + 0.25 * knot))
        t = np.maximum(np.maximum(core, body * 0.6), halo * 0.55)
        return colour(t) * np.clip(t * 1.6, 0, 1)[..., None]
    return field(32, 10, fn)


def spark_b(frame):
    rng = np.random.default_rng(230 + frame)
    sparks = [(rng.uniform(0, 2 * np.pi), rng.uniform(2, 7)) for _ in range(6)]

    def fn(x, y):
        d = np.hypot(x - 8, y - 8)
        t = np.clip(2.6 - frame * 0.3 - d, 0, 1)
        t = np.maximum(t, steps(np.clip(1 - d / 7, 0, 1) * 0.55))
        for a, r in sparks:
            sx, sy = 8 + np.cos(a) * r, 8 + np.sin(a) * r
            t = np.maximum(t, np.clip(1.1 - np.hypot(x - sx, y - sy), 0, 1) * 0.9)
        return colour(t) * np.clip(t * 1.6, 0, 1)[..., None]
    return field(16, 16, fn)


def ring_b():
    def fn(x, y):
        d = np.hypot(x - 8, y - 8)
        t = np.maximum(np.clip(1.4 - np.abs(d - 4.5), 0, 1) * 0.8, np.clip(1.8 - d, 0, 1))
        t = np.maximum(t, steps(np.clip(1 - d / 8, 0, 1) * 0.35))
        return colour(t) * np.clip(t * 1.6, 0, 1)[..., None]
    return field(16, 16, fn)


def wedge_b(arc_deg, radius=300):
    """The filled telegraph fan, pointing right from (0, h/2): fill rising to the rim, a 2 px
    bright arc and 1 px edges."""
    half = np.radians(arc_deg / 2)
    w = radius + 2
    h = int(np.ceil(2 * radius * np.sin(half))) + 4

    def fn(x, y):
        dx, dy = x, y - h / 2
        r = np.hypot(dx, dy)
        ang = np.abs(np.arctan2(dy, dx))
        inside = (ang <= half) & (r <= radius)
        fill = np.where(inside, 0.10 + 0.14 * (r / radius) ** 2, 0)
        rim = np.clip(1.2 - np.abs(r - radius + 1), 0, 1) * (ang <= half)
        edge = np.clip(1.0 - np.abs(r * np.sin(half - ang)), 0, 1) * (r <= radius) * (ang <= half + 0.02)
        t = np.maximum(fill, 0.62 * np.maximum(rim, edge))
        return colour(t) * np.clip(t * 1.6, 0, 1)[..., None]
    return field(w, h, fn, ss=2)


# --------------------------------------------------------------------------- the Mantis in context

def mantis_sprite(anim):
    """The chosen round-04 Mantis at its data size (80 px), turned in the model to face inward
    from the left edge (under the fixed key light, not rotated as an image)."""
    model = e4.vrell("mantis", em.mantis_a, "a")

    def turned(a, glow=1.0):
        scene, mats = model(a, glow)
        return (lambda p: scene(rotate_z(p, np.pi / 2))), mats
    turned.__name__ = "mantis_inward"
    _, sp = e3.render(turned, 80, anim, factor=8, colors=32)
    return sp


def eye_of(sp):
    a = np.array(sp).astype(int)
    red = (a[..., 0] - a[..., 1] > 50) & (a[..., 3] > 0)
    ys, xs = np.nonzero(red)
    keep = xs >= xs.max() - 3
    return int(round(xs[keep].mean())), int(round(ys[keep].mean()))


def rotated_strip(strip, length, angle_img):
    """Lay the beam: the strip tiled to ``length`` px, rotated about its start to the image-space
    angle (radians, x right, y down); returns (premultiplied rgb array, origin offset)."""
    sw, sh = strip.size
    canvas = Image.new("RGB", (2 * length + 8, 2 * length + 8), (0, 0, 0))
    c = length + 4
    rgb = strip.convert("RGB")
    for x in range(0, length, sw):
        canvas.paste(rgb.crop((0, 0, min(sw, length - x), sh)), (c + x, c - sh // 2))
    out = canvas.rotate(np.degrees(-angle_img), resample=Image.BILINEAR, center=(c, c))
    return np.array(out).astype(np.float64), c


def heading_to_img(h):
    """Game heading (radians clockwise from straight down, y up) to an image-space angle."""
    return np.arctan2(np.cos(h), -np.sin(h))


def beam_scene(variant, ground, light, mantis, ship, t):
    """The play field at time t of a telegraph (0..0.6 s) and sweep (0.6..1.8 s)."""
    base = darken(ground, light)
    base = over(base, ship, 300 - ship.width // 2, 330 - ship.height // 2)
    mx, my = 40, 150
    base = over(base, mantis, mx - mantis.width // 2, my - mantis.height // 2)
    ex, ey = eye_of(mantis)
    ex, ey = mx - mantis.width // 2 + ex, my - mantis.height // 2 + ey
    length = 300
    h_from, h_to = np.radians(-80), np.radians(-10)
    if t < 0.6:
        pulse = 0.45 + 0.35 * abs(np.sin(t * 60 * 0.4 / 2))
        if variant == "a":
            dot, dash = np.array(dot_a()).astype(np.float64), np.array(dash_a()).astype(np.float64)
            for k in range(19):
                h = h_from + (h_to - h_from) * k / 18
                a = heading_to_img(h)
                add(base, dot * pulse, int(ex + np.cos(a) * length - 2), int(ey + np.sin(a) * length - 2))
            for h in (h_from, h_to):
                a = heading_to_img(h)
                for r in range(10, length, 10):
                    add(base, dash * pulse, int(ex + np.cos(a) * r - 3), int(ey + np.sin(a) * r - 1))
        else:
            arc = np.degrees(h_to - h_from)
            wedge = wedge_b(arc, length)
            mid = heading_to_img((h_from + h_to) / 2)
            canvas = Image.new("RGB", (2 * length + 8, 2 * length + 8))
            cc = length + 4
            canvas.paste(wedge.convert("RGB"), (cc, cc - wedge.height // 2))
            rot = np.array(canvas.rotate(np.degrees(-mid), resample=Image.BILINEAR, center=(cc, cc))).astype(np.float64)
            add(base, rot * (0.6 + 0.5 * pulse), ex - cc, ey - cc)
        add(base, np.array(eye_a(1) if variant == "a" else ring_b()).astype(np.float64), ex - 7, ey - 7)
        return base
    s = min(1.0, (t - 0.6) / 1.2)
    h = h_from + (h_to - h_from) * s
    a = heading_to_img(h)
    frame = int(t * 20)
    strip = beam_a(frame % 3) if variant == "a" else beam_b(frame % 4)
    arr, c = rotated_strip(strip, length, a)
    add(base, arr, ex - c, ey - c)
    tip = tip_a() if variant == "a" else spark_b(frame % 4)
    tx, ty = ex + np.cos(a) * length, ey + np.sin(a) * length
    add(base, np.array(tip).astype(np.float64), int(tx - tip.width / 2), int(ty - tip.height / 2))
    eye = eye_a(frame % 3) if variant == "a" else ring_b()
    add(base, np.array(eye).astype(np.float64), ex - eye.width // 2, ey - eye.height // 2)
    return base


def beam_sheet(variant):
    ground = Image.open(BACK / "mare-a.png").convert("RGBA").crop((0, 200, 480, 600))
    light = light_map(480, 400, pools=[(380, 90, 70, LAMP), (150, 330, 60, LAMP * 0.8)],
                      cone=(300, 312, 200, np.radians(60)))
    mantis = mantis_sprite(1.0)
    ship = e3.player_sprite()
    if variant == "a":
        parts = [("BEAM 16X12 X3 (TILED)", [beam_a(i) for i in range(3)]), ("TIP 12X12", [tip_a()]),
                 ("EYE 14X14 X3", [eye_a(i) for i in range(3)]), ("ARC DOT 5X5", [dot_a()]),
                 ("EDGE DASH 6X2", [dash_a()])]
        desc = ["HOT WIRE: WHITE-HOT 2 PX CORE IN A 6 PX CRIMSON BODY, STEPPED HALO TO 12 PX, A ROUND TIP FLARE.",
                "TELEGRAPH: 19 PULSING DOTS ON THE 300 PX REACH ARC, DASHED EDGE LINES (THE GAME'S LAYOUT TODAY).",
                "ALL ADDITIVE, PREMULTIPLIED ON BLACK; THE STRIP TILES ALONG THE BEAM AND ROTATES WITH IT."]
    else:
        parts = [("BEAM 32X10 X4 (TILED)", [beam_b(i) for i in range(4)]),
                 ("TIP SPARK 16X16 X4", [spark_b(i) for i in range(4)]), ("EYE RING 16X16", [ring_b()])]
        desc = ["CHARGED LANCE: 2 PX CORE, ENERGY KNOTS RUNNING OUT ALONG THE BEAM (4 PX A FRAME), SPARKS AT THE TIP.",
                "TELEGRAPH: A FAINT FILLED WEDGE WITH A BRIGHT RIM ON THE ARC AND THE EDGES (70 DEG, 90 ON HARD):",
                "ONE PRE-RENDERED FAN EACH (302X348 / 302X428 PX); THE WEDGE SHOWS THE DANGER ZONE AS AN AREA."]
    times = (0.3, 0.9, 1.5)
    scenes = [img(beam_scene(variant, ground, light, mantis, ship, t)) for t in times]
    # the parts strip on black, at 4x
    pw = sum(sum(f.width + 4 for f in fr) + 12 for _, fr in parts)
    ph = max(f.height for _, fr in parts for f in fr)
    strip_img = Image.new("RGBA", (pw, ph), (0, 0, 0, 255))
    x, labels = 0, []
    for name, fr in parts:
        labels.append((x, name))
        for f in fr:
            strip_img.alpha_composite(f, (x, 0))
            x += f.width + 4
        x += 12
    big = sprite.enlarge(strip_img, 4)
    sw = max(16 + 3 * 330 + 16, 32 + big.width)
    scene_h = 400 * 330 // 480
    sheet = raster.sheet(sw, 70 + 12 + big.height + 22 + 12 + scene_h + 30 + (330 if variant == "b" else 0),
                         f"MANTIS BEAM - ROUND 23 VARIANT {variant.upper()}", "LEVEL 06 EFFECT CONCEPT")
    raster.draw_text(sheet, 16, 34, desc[0], raster.LABEL)
    raster.draw_text(sheet, 16, 46, desc[1], raster.LABEL_DIM)
    raster.draw_text(sheet, 16, 58, desc[2], raster.LABEL_DIM)
    y = 74
    raster.draw_text(sheet, 16, y, "PARTS AT 4X (ADDITIVE ON BLACK)", raster.LABEL_DIM)
    sheet.alpha_composite(big, (16, y + 12))
    for lx, name in labels:
        raster.draw_text(sheet, 16 + lx * 4, y + 14 + big.height, name, raster.LABEL)
    y += 12 + big.height + 22
    raster.draw_text(sheet, 16, y, "PLAY FIELD 1X (SCALED 0.69): TELEGRAPH 0.3 S, SWEEP 0.9 S AND 1.5 S, DARK GROUND, MANTIS LIT",
                     raster.LABEL_DIM)
    for i, s in enumerate(scenes):
        sheet.alpha_composite(s.resize((330, scene_h), Image.NEAREST), (16 + i * 330, y + 12))
    if variant == "b":
        y += 12 + scene_h + 12
        raster.draw_text(sheet, 16, y, "1X CROPS: TELEGRAPH WEDGE, SWEEP", raster.LABEL_DIM)
        for i, s in enumerate(scenes[:2]):
            sheet.alpha_composite(s.crop((0, 60, 480, 360)), (16 + i * 496, y + 12))
    path = MANTIS / f"mantis-beam-r23-{variant}.png"
    sheet.convert("RGB").save(path, optimize=True)
    print(path.relative_to(ROOT))
    frames = [img(beam_scene(variant, ground, light, mantis, ship, t)).convert("RGB")
              for t in np.arange(0, 2.1, 0.05)]
    gif = MANTIS / f"mantis-beam-r23-{variant}.gif"
    frames[0].save(gif, save_all=True, append_images=frames[1:], duration=50, loop=0, optimize=True)
    print(gif.relative_to(ROOT))


# --------------------------------------------------------------------------- ore cart (28x40)

def cart_a(s):
    """Open rust ore tub on four wheels, heaped with ore, a hazard band round the rim."""
    rng = np.random.default_rng(61)
    lumps = [(rng.uniform(-7, 7), rng.uniform(-12, 12), rng.uniform(2.0, 3.4)) for _ in range(14)]

    def scene(p):
        tub = sd_box(p, (0, 0, 2.0), (10.5, 15.5, 6.0), 2.0)
        hollow = sd_box(p, (0, 0, 7.0), (9.0, 14.0, 6.0), 1.5)
        items = [(np.maximum(tub, -hollow), RUST),
                 (np.maximum(sd_box(p, (0, 0, 7.4), (10.7, 15.7, 0.8), 0.4), -hollow), AMBER)]
        for sx in (-1, 1):
            for sy in (-1, 1):
                if not (s == 2 and sx > 0 and sy > 0):
                    items.append((sd_cylinder_x(p, (sx * 9.0, sy * 10.5, -3.5), 3.2, 1.4), DARK))
        items += [(sd_box(p, (0, sy * 18.0, -1.5), (2.0, 2.4, 1.0), 0.4), DARK) for sy in (-1, 1)]
        heap = 3 if s == 0 else (2 if s == 1 else 0)
        if heap:
            items += [(sd_sphere(p, (x, y, 4.5 + r * 0.4), r), ORE) for x, y, r in lumps[:heap * 5]]
        d, m = union(*items, k=0.3)
        if s == 1:
            d = np.maximum(d, -sd_sphere(p, (9, 6, 6), 3.0))                          # dent
        if s == 2:                                                                    # tipped open
            d, m = subtract((d, m), sd_box(rotate_z(p - np.array([6.0, 4.0, 4.0]), 0.5), (0, 0, 0), (6, 9, 8), 0.5))
            d, m = union((d, m), (sd_box(p, (0, 0, -1.5), (8.5, 13.5, 0.6), 0.3), VOID),
                         *[(sd_sphere(p, (11 + x * 0.3, y * 0.8 + 4, 0.5), r * 0.8), ORE) for x, y, r in lumps[:6]],
                         (sd_capsule(p, (-6, -4, 1), (-2, -9, 4), 0.8), CHAR))
        return d, m
    return scene


def cart_b(s):
    """Closed hopper car: a sloped steel body, a hatched lid with amber/black stripes, a beacon."""
    def scene(p):
        body = sd_box(p, (0, 0, 2.5), (10.0, 15.0, 6.5), 1.8)
        slope = sd_box(rotate_x(p - np.array([0.0, 0.0, 9.0]), 0.0), (0, 0, 0), (7.0, 13.0, 1.6), 1.2)
        items = [(body, HULL if s < 2 else CHAR), (slope, OLIVE)]
        if s < 2:
            items += [(sd_box(p, (0, sy * 6.0, 10.8), (5.5, 4.5, 0.6), 0.3), AMBER) for sy in (-1, 1)]
            items += [(sd_box(p, (0, 0, 10.9), (6.0, 0.6, 0.4), 0.2), DARK)]
            items += [(sd_cylinder_z(p, (-7.5, -12.5, 9.5), 1.8, 1.0), DARK), (sd_sphere(p, (-7.5, -12.5, 10.6), 1.2), RED)]
        for sy in (-1, 1):
            items.append((sd_box(p, (0, sy * 17.5, -1.0), (8.0, 1.6, 2.0), 0.5), DARK))
            for sx in (-1, 1):
                items.append((sd_cylinder_x(p, (sx * 9.5, sy * 10.0, -3.5), 3.2, 1.2), DARK))
        d, m = union(*items, k=0.4)
        if s == 1:
            d = np.maximum(d, -sd_sphere(p, (-4, 8, 11), 3.2))
            d = np.maximum(d, -sd_sphere(p, (6, -4, 10), 2.4))
        if s == 2:
            d, m = subtract((d, m), sd_box(p, (0, 0, 8.0), (8.0, 13.0, 6.0), 1.0))
            rng = np.random.default_rng(62)
            d, m = union((d, m), (sd_box(p, (0, 0, -1.0), (8, 12.5, 0.6), 0.3), VOID),
                         *[(sd_sphere(p, (rng.uniform(-6, 6), rng.uniform(-10, 10), 1.5), rng.uniform(1.5, 2.6)), ORE)
                           for _ in range(7)],
                         (sd_box(rotate_z(p - np.array([13.0, -6.0, 0.0]), 0.6), (0, 0, 0.5), (1.2, 5.5, 3.5), 0.4), AMBER))
        return d, m
    return scene


def ore_cart(variant):
    make = cart_a if variant == "a" else cart_b
    frames = [img(damage(render(make(s), materials(s), (28, 40), rim=s < 2), s, 230)) for s in range(3)]
    return frames, ["INTACT", "DAMAGED", "WRECKED"]


def cart_context(frames):
    """On Level 05's mass-driver field (the rail), under a rail lamp, in the dark."""
    ground = Image.open(BACK / "mass-driver-field.png").convert("RGBA").crop((RAIL_X - 140, 300, RAIL_X + 44, 480))
    w, h = ground.size
    views = []
    for f in (frames[0], frames[2]):
        light = light_map(w, h, pools=[(140, 70, 64, LAMP)])
        base = darken(ground, light)
        views.append(img(shadowed(base, f, 140 - 14, 70 - 10, light)))
    light = light_map(w, h, cone=(128, 190, 200, np.radians(60)))
    views.append(img(shadowed(darken(ground, light), frames[0], 140 - 14, 70 - 10, light)))
    light = light_map(w, h)
    views.append(img(shadowed(darken(ground, light), frames[0], 140 - 14, 70 - 10, light)))
    return views, ["UNDER THE RAIL LAMP", "WRECKED, LAMP", "IN THE HEADLIGHT", "UNLIT (AMBIENT)"]


# --------------------------------------------------------------------------- survey cache (32x24)

def cache_a(s):
    """CDF field case: ribbed olive box, amber/black end bands, three retro markers on the lid."""
    def scene(p):
        items = [(sd_box(p, (0, 0, 1.5), (14.5, 9.5, 5.0), 1.6), OLIVE)]
        items += [(sd_box(p, (x, 0, 6.6), (0.7, 9.6, 0.5), 0.3), DARK) for x in (-6, 0, 6)]
        if s < 2:
            items += [(sd_box(p, (sx * 12.0, 0, 6.4), (2.0, 9.4, 0.6), 0.3), AMBER) for sx in (-1, 1)]
            items += [(sd_cylinder_z(p, (x, -4.0, 7.0), 1.6, 0.5), RETRO) for x in (-6.5, -0.5, 5.5)]
        d, m = union(*items, k=0.3)
        if s == 1:
            d = np.maximum(d, -sd_sphere(p, (4, 4, 8.5), 3.0))
        if s == 2:                                                   # the lid blown off, the payload
            d, m = subtract((d, m), sd_box(p, (0, 0, 6.0), (13.0, 8.0, 4.0), 0.8))
            d, m = union((d, m), (sd_box(p, (0, 0, 1.0), (12.5, 7.5, 0.5), 0.3), VOID),
                         *[(sd_cylinder_y(p, (x, 0, 3.0), 1.8, 6.0), HULL) for x in (-8, -3.5, 1)],
                         (sd_box(p, (7.5, 0, 3.0), (3.0, 5.5, 2.0), 0.6), AMBER),
                         (sd_box(rotate_z(p - np.array([4.0, 13.5, 0.0]), 0.35), (0, 0, 0.5), (12, 2.0, 1.0), 0.4), OLIVE))
        return d, m
    return scene


def cache_b(s):
    """Half-buried instrument drum: a round lid with an amber ring, three amber blinkers in a
    triangle, a folded antenna, a regolith skirt."""
    def scene(p):
        items = [(sd_ellipsoid(p, (0, 0, -5.0), (15.5, 11.5, 6.0)), BAG),
                 (sd_cylinder_z(p, (0, 0, 0.0), 9.5, 4.0), HULL),
                 (np.maximum(sd_cylinder_z(p, (0, 0, 4.0), 9.8, 0.7), -sd_cylinder_z(p, (0, 0, 4.0), 7.6, 2)), AMBER)]
        if s < 2:
            items.append((sd_cylinder_z(p, (0, 0, 4.2), 7.4, 0.6), OLIVE))
            items += [(sd_sphere(p, (np.sin(a) * 4.5, -np.cos(a) * 4.5, 5.0), 1.1), LED)
                      for a in (0, 2.094, 4.189)]
            items.append((sd_capsule(p, (6.5, 6.0, 4.0), (13.0, 9.0, 1.5), 0.5), DARK))
        d, m = union(*items, k=0.4)
        if s == 1:
            d = np.maximum(d, -sd_sphere(p, (-4, 3, 6.5), 2.8))
        if s == 2:
            d, m = subtract((d, m), sd_cylinder_z(p, (0, 0, 4.0), 7.6, 3.0))
            d, m = union((d, m), (sd_cylinder_z(p, (0, 0, 0.6), 7.4, 0.4), VOID),
                         (sd_box(p, (-2.0, 0, 2.0), (3.0, 4.0, 1.5), 0.5), HULL),
                         (sd_cylinder_z(p, (3.5, 1.5, 2.0), 1.6, 1.4), SOLAR),
                         (sd_cylinder_z(rotate_x(p - np.array([10.0, -6.5, 0.0]), 0.5), (0, 0, 0), 4.5, 0.6), OLIVE))
        return d, m
    return scene


def survey_cache(variant):
    make = cache_a if variant == "a" else cache_b
    frames = [img(damage(render(make(s), materials(s, leds=variant == "b"), (32, 24), rim=s < 2), s, 231))
              for s in range(3)]
    return frames, ["CLOSED", "HIT", "OPENED"]


def marker_glow(variant, frame, lit):
    """What the game adds after the light pass: A a glint on each retro marker while lit;
    B the three blinkers at a dim 55 % even in the dark (blinking)."""
    xs = (-6.5, -0.5, 5.5) if variant == "a" else tuple(np.sin(a) * 4.5 for a in (0, 2.094, 4.189))
    ys = (-4.0,) * 3 if variant == "a" else tuple(-np.cos(a) * 4.5 for a in (0, 2.094, 4.189))
    k = (1.0 if lit else 0.0) if variant == "a" else (0.55 if not lit else 0.8) * (1 if frame % 2 == 0 else 0.4)
    col = np.array([1.0, 0.95, 0.85]) if variant == "a" else np.array([1.0, 0.62, 0.15])

    def fn(x, y):
        t = np.zeros_like(x)
        for mx, my in zip(xs, ys):
            cx, cy = 16 + mx, 12 - my
            d = np.hypot(x - cx, y - cy)
            cross = np.clip(1 - np.minimum(np.abs(x - cx), np.abs(y - cy)), 0, 1) * np.clip(1 - d / 4, 0, 1)
            t = np.maximum(t, np.maximum(np.clip(1.8 - d, 0, 1), cross * (0.8 if variant == "a" else 0)))
            t = np.maximum(t, steps(np.clip(1 - d / 3.5, 0, 1) * 0.5))
        return col * (t * k)[..., None]
    return field(32, 24, fn)


def cache_context(frames, variant):
    ground = Image.open(BACK / "mare-a.png").convert("RGBA").crop((120, 380, 300, 560))
    w, h = ground.size
    x, y = 90 - 16, 70 - 12
    views = []
    light = light_map(w, h)
    views.append(img(add(shadowed(darken(ground, light), frames[0], x, y, light), np.array(marker_glow(variant, 0, False)).astype(np.float64), x, y)))
    light = light_map(w, h, cone=(90, 176, 200, np.radians(60)))
    views.append(img(add(shadowed(darken(ground, light), frames[0], x, y, light), np.array(marker_glow(variant, 0, True)).astype(np.float64), x, y)))
    light = light_map(w, h, pools=[(110, 50, 120, np.array([1.0, 0.78, 0.5]))])
    views.append(img(add(shadowed(darken(ground, light), frames[1], x, y, light), np.array(marker_glow(variant, 1, True)).astype(np.float64), x, y)))
    light = light_map(w, h, cone=(90, 176, 200, np.radians(60)))
    views.append(img(shadowed(darken(ground, light), frames[2], x, y, light)))
    return views, ["DARK CRATER", "IN THE HEADLIGHT", "FLARE, HIT", "OPENED"]


# --------------------------------------------------------------------------- data core (40x32 + 16x16)

def term_a(s):
    """Pedestal console: steel body, a slanted teal screen, a keypad, an amber/black core bay;
    released: the bay open and empty, the screen dark."""
    def scene(p):
        items = [(sd_box(p, (0, 0, 0.0), (17.0, 12.0, 5.0), 1.4), HULL),
                 (sd_box(p, (0, -14.5, -2.0), (14.0, 2.0, 2.5), 0.6), DARK),                   # cable duct
                 (sd_box(rotate_x(p - np.array([-4.0, 3.0, 7.0]), 0.5), (0, 0, 0), (10.0, 6.0, 1.2), 0.6), DARK),
                 (sd_box(rotate_x(p - np.array([-4.0, 3.0, 8.3]), 0.5), (0, 0, 0), (8.5, 4.8, 0.3), 0.2), SCREEN),
                 (sd_box(p, (-4.0, -6.0, 5.6), (8.0, 2.5, 0.6), 0.3), ACCENT)]
        items += [(sd_box(p, (-10 + i * 3.0, -6.0, 6.2), (0.9, 0.9, 0.4), 0.2), DARK) for i in range(6)]
        items.append((sd_box(p, (11.0, 0, 5.4), (4.5, 9.0, 0.6), 0.3), AMBER))
        if s == 0:
            items.append((sd_box(p, (11.0, 0, 6.2), (2.2, 5.5, 0.8), 0.4), CORE))
        d, m = union(*items, k=0.3)
        if s == 1:
            d, m = subtract((d, m), sd_box(p, (11.0, 0, 6.0), (2.4, 5.8, 3.0), 0.3))
            d, m = union((d, m), (sd_box(p, (11.0, 0, 2.5), (2.2, 5.6, 0.4), 0.2), VOID))
        return d, m
    return scene


def term_b(s):
    """Wall cabinet beside the airlock: a long steel cabinet with two LED rows and a round
    socket in an amber ring holding the amber core orb; released: the socket empty, LEDs dead."""
    def scene(p):
        items = [(sd_box(p, (0, 2.0, 0.0), (18.5, 9.0, 6.0), 1.2), HULL),
                 (sd_box(p, (0, 11.5, 2.0), (19.5, 2.0, 7.0), 0.6), DARK),                     # the wall
                 (np.maximum(sd_cylinder_z(p, (8.0, 0.0, 6.0), 6.0, 0.8), -sd_cylinder_z(p, (8.0, 0.0, 6.0), 4.2, 2)), AMBER),
                 (sd_cylinder_z(p, (8.0, 0.0, 4.0), 4.2, 1.6), VOID)]
        for row in (-3.0, 3.0):
            items += [(sd_box(p, (-15 + i * 3.0, row, 6.3), (0.8, 0.8, 0.5), 0.2), LED if s == 0 else DARK)
                      for i in range(6)]
        items.append((sd_box(p, (-6.0, 0.0, 6.0), (9.0, 6.0, 0.4), 0.3), DARK))
        if s == 0:
            items.append((sd_sphere(p, (8.0, 0.0, 6.0), 3.4), LED))
        return union(*items, k=0.25)
    return scene


def core_a():
    def scene(p):
        return union((sd_box(p, (0, 0, 1.0), (3.8, 6.0, 1.6), 0.6), HULL),
                     (sd_box(p, (0, 0.8, 2.6), (2.6, 4.0, 0.4), 0.2), CORE),
                     (sd_box(p, (0, -5.6, 1.2), (2.8, 0.8, 1.2), 0.3), AMBER))
    return scene


def core_b():
    def scene(p):
        ring = np.maximum(sd_cylinder_z(p, (0, 0, 0), 5.8, 0.7), -sd_cylinder_z(p, (0, 0, 0), 4.6, 2))
        return union((sd_sphere(p, (0, 0, 0), 4.6), LED), (ring, DARK),
                     (sd_box(p, (0, 0, 0), (0.6, 6.4, 0.6), 0.3), DARK))
    return scene


def data_core(variant):
    make = term_a if variant == "a" else term_b
    frames = [img(render(make(s), materials(0, lit_core=s == 0, leds=s == 0), (40, 32))) for s in range(2)]
    core = img(render(core_a() if variant == "a" else core_b(), materials(0), (16, 16)))
    return frames, core, ["TERMINAL", "CORE RELEASED"]


def core_glow(core, variant):
    """The released core's pulse, added after the light pass (it must read as a pickup)."""
    col = np.array([0.3, 0.95, 1.0]) if variant == "a" else np.array([1.0, 0.65, 0.2])

    def fn(x, y):
        d = np.hypot(x - 8, y - 8)
        return col * steps(np.clip(1 - d / 8, 0, 1) * 0.55)[..., None]
    return field(16, 16, fn)


def core_context(frames, core, variant):
    ground = Image.open(BACK / "mare-a.png").convert("RGBA").crop((240, 100, 420, 280))
    w, h = ground.size
    deck = np.array(ground).astype(np.float64)
    deck[40:140, 30:150, :3] = deck[40:140, 30:150, :3] * 0.35 + np.array([92, 94, 100]) * 0.65   # the airlock apron
    deck[40:140:16, 30:150, :3] *= 0.7
    ground = img(deck)
    x, y = 90 - 20, 70 - 16
    views = []
    light = light_map(w, h, pools=[(60, 40, 110, LAMP)])
    views.append(img(shadowed(darken(ground, light), frames[0], x, y, light)))
    base = shadowed(darken(ground, light), frames[1], x, y, light)
    base = add(base, np.array(core_glow(core, variant)).astype(np.float64), 120, 90)
    views.append(img(shadowed(base, core, 120, 90, None)))
    light = light_map(w, h)
    views.append(img(shadowed(darken(ground, light), frames[0], x, y, light)))
    return views, ["IN THE DOME LIGHT", "CORE RELEASED", "UNLIT (AMBIENT)"]


# --------------------------------------------------------------------------- sheets

PROPS = {
    "ore-cart": ("ABANDONED ORE CART - 28X40, GROUND LAYER, 3 HP, MEDIUM SALVAGE (50)", {
        "a": "OPEN RUST ORE TUB HEAPED WITH ORE, AMBER/BLACK HAZARD RIM, FOUR WHEELS ON THE RAIL",
        "b": "CLOSED HOPPER CAR, OLIVE ROOF, AMBER/BLACK HATCHED LID, RED BEACON, COUPLERS"}),
    "survey-cache": ("CDF SURVEY CACHE - 32X24, HIDDEN CRATE (135), 3 HITS, HITTABLE ONLY WHILE LIT", {
        "a": "RIBBED OLIVE FIELD CASE, HAZARD END BANDS, THREE RETRO MARKERS THAT GLINT ONLY WHEN LIT",
        "b": "HALF-BURIED INSTRUMENT DRUM, HAZARD RING, THREE DIM AMBER BLINKERS (A FAINT HINT IN THE DARK)"}),
    "data-core": ("DATA CORE - AIRLOCK TERMINAL 40X32 (2 HITS) AND THE RELEASED CORE 16X16", {
        "a": "PEDESTAL CONSOLE, TEAL SCREEN, HAZARD-STRIPED CORE BAY; THE CORE A CYAN CARTRIDGE",
        "b": "WALL CABINET BY THE AIRLOCK, LED ROWS, AMBER-RING SOCKET; THE CORE AN AMBER ORB IN A CAGE"}),
}


def lit_strip(frames):
    w = sum(f.width + GAP for f in frames)
    h = max(f.height for f in frames) + GAP
    ground = r17.regolith(w, h, 2300 + w)
    x = 0
    for f in frames:
        ground.alpha_composite(sprite.shadow_of(f, opacity=0.55, blur=1.2), (x + GAP // 2 + 3, GAP // 2 + 4))
        ground.alpha_composite(f, (x + GAP // 2, GAP // 2))
        x += f.width + GAP
    return ground


def prop_sheet(prop, variant):
    title, desc = PROPS[prop]
    if prop == "ore-cart":
        frames, labels = ore_cart(variant)
        views, vlabels = cart_context(frames)
    elif prop == "survey-cache":
        frames, labels = survey_cache(variant)
        views, vlabels = cache_context(frames, variant)
    else:
        frames, core, labels = data_core(variant)
        views, vlabels = core_context(frames, core, variant)
        frames = frames + [core]
        labels = labels + ["CORE 16X16"]
    small = lit_strip(frames)
    big = sprite.enlarge(small, 3)
    vw = views[0].width
    ctx_w = len(views) * (vw * 2 + 12)
    sw = max(32 + big.width, 32 + ctx_w, 640)
    sheet = raster.sheet(sw, 70 + small.height + 24 + big.height + 30 + views[0].height * 2 + 30,
                         f"{prop.upper()} - ROUND 23 VARIANT {variant.upper()}", "LEVEL 06 PROP CONCEPT")
    raster.draw_text(sheet, 16, 34, title, raster.LABEL)
    raster.draw_text(sheet, 16, 46, desc[variant], raster.LABEL_DIM)
    y = 62
    raster.draw_text(sheet, 16, y, "1X, LIT", raster.LABEL_DIM)
    sheet.alpha_composite(small, (16, y + 10))
    y += 10 + small.height + 10
    raster.draw_text(sheet, 16, y, "3X, LIT", raster.LABEL_DIM)
    sheet.alpha_composite(big, (16, y + 10))
    x = 16
    for f, name in zip(frames, labels):
        raster.draw_text(sheet, x + 6, y + 12 + big.height, name, raster.LABEL)
        x += (f.width + GAP) * 3
    y += 10 + big.height + 18
    raster.draw_text(sheet, 16, y, "IN CONTEXT, 2X: THE DARK LUNA GROUND UNDER THE LIGHT MAP (AMBIENT, LAMP OR DOME POOL, HEADLIGHT, FLARE)",
                     raster.LABEL_DIM)
    for i, (v, name) in enumerate(zip(views, vlabels)):
        sheet.alpha_composite(sprite.enlarge(v, 2), (16 + i * (vw * 2 + 12), y + 10))
        raster.draw_text(sheet, 16 + i * (vw * 2 + 12) + 4, y + 14 + v.height * 2, name, raster.LABEL)
    path = L06 / f"{prop}-r23-{variant}.png"
    sheet.convert("RGB").save(path, optimize=True)
    print(path.relative_to(ROOT))


def main(args):
    L06.mkdir(parents=True, exist_ok=True)
    MANTIS.mkdir(parents=True, exist_ok=True)
    wanted = args or ["mantis-beam", *PROPS]
    for name in wanted:
        for variant in ("a", "b"):
            if name == "mantis-beam":
                beam_sheet(variant)
            else:
                prop_sheet(name, variant)


if __name__ == "__main__":
    main(sys.argv[1:])
