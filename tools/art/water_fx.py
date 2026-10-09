#!/usr/bin/env python3
"""Production art: the shared water and torpedo effects, and the Harbour Kraken's lane telegraph
and slam look as an a/b (M5 part E batch, concept round 33; design/art-direction, Animation rules:
Water; design/player/weapons/torpedo-pod; design/enemies/bosses/harbour-kraken, Lanes and slams).
Straight to production from the chosen concepts (user decision E9 = a): the water-surface and
under-water explosions of explosions-r09-a (tools/concept/vfx_r09.py: vfx_r08's water_frames and
round 09's under_burst, imported unchanged), the torpedo of projectiles-r08-a (vfx_r08's
torpedo_model); the foam, collars, ripple trains and wakes follow the ocean scene's water
(scene-ocean-r10-a: tools/concept/scenes_r08.py's loop-safe streams and collars, imported
unchanged). The lane telegraph and slam look had no concept: two variants at production quality.

Drawing (user decision E4 = c, the hybrid): everything on the `sub` layer (the under-water bursts,
the torpedo, its bubbles) is drawn plain here and gets its tint, blur and wave from the game's
generic `sub` pass; foam, collars, ripples and splashes are pre-rendered. Foam and water effects
are drawn alpha-blended (not additive), white-cyan foam over dark troughs, their alpha stepped to a
few translucency levels (90s translucency tables); only the lane marks are additive light.

Outputs (assets/sprites/; the game's names):
  explosion-water-tiny_0..11     24x24  a kill or a landing blast on the water surface: white
  explosion-water-small_0..11    40x40  column, spray, patchy foam, broken ripple arcs (the size
  explosion-water-medium_0..11   64x64  rungs of the fireball ladder; on a level with water: true
  explosion-water-large_0..13    96x96  every kill and landing blast on the play field uses them)
  explosion-under-tiny_0..11     24x24  a kill under water (`sub`): cyan flash, pressure ring, dark
  explosion-under-small_0..11    40x40  silt cloud, bubbles rising (drawn through the sub pass);
  explosion-under-medium_0..11   64x64  the torpedo's impact is the small rung (surface or under)
  explosion-under-large_0..13    96x96
  water-splash_0..7              24x24  a small splash: a crown of drops and a foam ring (a shot
                                        or shell hitting the water, a unit dropping in, a jelly diving)
  water-ripple_0..11             64x64  an uneven ripple train expanding and breaking up (after a
                                        kill sinks, a unit surfacing, a raft settling)
  water-collar-24_0..3           36x36  animated foam collars for round bodies at the waterline
  water-collar-40_0..3           52x52  (the diameter in the name; centred on the body, drawn over
  water-collar-64_0..3           76x76  its edge); a 4-frame flicker loop
  water-foam-strip_0..3          32x10  a foam band, tileable along x: a limb or hull edge lying
                                        awash (the slam arm's awash window), drawn rotated along it
  water-wake_0..7                32x64  a small craft's V-wake and churned trail, its bow at
                                        (16, 6), streaming down (rafts on the current, L13's Skimmers)
  torpedo-pod-shot_0..31         18x18  the torpedo (5x16 body) at 32 headings, k x 11.25 degrees
                                        clockwise from up, rendered with the key light fixed
  torpedo-pod-bubbles_0..5       10x10  a puff of its bubble trail, spreading and fading
  torpedo-pod-splash_0..5        16x16  the drop splash where it enters the water
  harbour-kraken-lane-churn_0..5 (a) 40x48 / (b) 120x48  the telegraph's churn, tileable down the
                                        lane (a: a boiling band on the lane's centre line; b: the
                                        whole lane boiling over the arm's dark shadow)
  harbour-kraken-lane-mark_0..1  (a) 6x16 / (b) 22x14  the red telegraph mark, additive, lit and
                                        dim (a: a dash, drawn down both lane edges; b: a chevron
                                        pointing down the lane, drawn down its centre)
  harbour-kraken-lane-splash_0..9 120x64  the slam's impact, one segment of the lane (the game lays
                                        them along the arm every 48 px, base to tip, a frame apart):
                                        a: spray sheets thrown to both sides; b: a crown with two
                                        rolling foam waves that break at the lane edges
  design/art-direction/concept/water-fx-final-r33-a.png/.gif   the production fx (review)
  design/art-direction/concept/slam-r33-a.png/.gif   the lane telegraph and slam: chosen a
                                        (round 33), its names the game's; the rejected b's review
                                        in concept/rejected/slam-r33-b.* (its frames are written
                                        only with --variant b)

Run: python3 tools/art/water_fx.py [--variant a|b] [--review]   (~1 min; --review rebuilds the
review files from assets/ for the production variant and renders the other variant afresh)
"""
import sys
from concurrent.futures import ProcessPoolExecutor

import numpy as np
from PIL import Image, ImageDraw

import artkit
from artkit import DESIGN, ROOT, TAU, sprite

import scenes_r08 as s8  # noqa: E402  (concept script, imported unchanged: Sea, stream, collar)
import scenes_r09 as s9  # noqa: E402  (concept script, imported unchanged: the ocean's colour stops)
import vfx_r08 as v8  # noqa: E402  (concept script, imported unchanged)
import vfx_r09 as v9  # noqa: E402  (concept script, imported unchanged)
from render.enemy_rigs import write_gif  # noqa: E402

SCRIPT = "water_fx.py"
BATCH = "M5 part E batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
REVIEW_ROUND = "r33"
CONCEPT = DESIGN / "art-direction" / "concept"
PRODUCTION = "a"                     # the lane telegraph and slam look: the user's pick (round 33)
FPS = 20
LOOP = s8.N                          # the concept streams' loop length in frames (f is scaled onto it)

WATER = tuple(s8.OCEAN_WATER)        # (24, 48, 64): the sea's body colour under the surface
FOAM = s8.FOAM                       # (226, 238, 242)
FOAM_SHADE = (150, 178, 192)
TROUGH = (10, 26, 44)
CREST = (190, 220, 235)

LADDER = {"tiny": (24, 12), "small": (40, 12), "medium": (64, 12), "large": (96, 14)}
EXPLOSION_COLOURS = 40
FX_COLOURS = 24
COLLARS = (24, 40, 64)
COLLAR_FRAMES = 4
COLLAR_PAD = 6
TORPEDO_HEADINGS = 32
LANE = 120                           # px, the Kraken's lane width
SPLASH = (LANE, 64)
SPLASH_FRAMES = 10
CHURN_FRAMES = 6
SEGMENT = 48                         # px between splash segments along the arm


# =========================================================================== foam canvas

class Foam:
    """Foam, spray and bubbles on a ``w`` x ``h`` canvas supersampled ``ss`` x: dots composited
    over each other (premultiplied), box-reduced and stepped to a few translucency levels. The
    ``dot`` signature is the concept's (scenes_r08.Foam), so its loop-safe ``stream`` and
    ``collar`` draw onto it unchanged."""

    def __init__(self, w, h, ss=4, wrap_x=None, wrap_y=None):
        self.w, self.h, self.ss = int(w), int(h), ss
        self.wrap_x, self.wrap_y = wrap_x, wrap_y
        self.rgb = np.zeros((self.h * ss, self.w * ss, 3))
        self.a = np.zeros((self.h * ss, self.w * ss))
        self.default = FOAM

    def _blend(self, ys, xs, color, cover):
        c = np.asarray(color, np.float64) / 255.0
        self.rgb[ys, xs] = self.rgb[ys, xs] * (1 - cover[..., None]) + c * cover[..., None]
        self.a[ys, xs] = self.a[ys, xs] * (1 - cover) + cover

    def _shape(self, x, y, rx, ry, cover_fn):
        offsets_x = [0] if self.wrap_x is None else [-self.wrap_x, 0, self.wrap_x]
        offsets_y = [0] if self.wrap_y is None else [-self.wrap_y, 0, self.wrap_y]
        s = self.ss
        for ox in offsets_x:
            for oy in offsets_y:
                cx, cy = x + ox, y + oy
                x0, x1 = max(0, int(np.floor((cx - rx - 1) * s))), min(self.w * s, int(np.ceil((cx + rx + 1) * s)))
                y0, y1 = max(0, int(np.floor((cy - ry - 1) * s))), min(self.h * s, int(np.ceil((cy + ry + 1) * s)))
                if x0 >= x1 or y0 >= y1:
                    continue
                py, px = np.mgrid[y0:y1, x0:x1]
                cover = cover_fn((px + 0.5) / s - cx, (py + 0.5) / s - cy)
                if cover.max() > 0:
                    self._blend(slice(y0, y1), slice(x0, x1), self.color_now, cover)

    def dot(self, x, y, r, alpha, color=None, squash=0.85):
        if alpha <= 0.03:
            return
        r = max(0.5, r)
        self.color_now = color or self.default
        a = min(1.0, alpha)
        self._shape(x, y, r, r * squash, lambda dx, dy: ((dx / r) ** 2 + (dy / (r * squash)) ** 2 <= 1) * a)

    def ellipse(self, x, y, rx, ry, angle, alpha, color=None):
        """A filled ellipse turned ``angle`` radians (spray sheets, streaks)."""
        if alpha <= 0.03:
            return
        self.color_now = color or self.default
        c, s_ = np.cos(angle), np.sin(angle)
        a = min(1.0, alpha)
        rx, ry = max(0.5, rx), max(0.5, ry)
        big = max(rx, ry)
        self._shape(x, y, big, big, lambda dx, dy: ((((c * dx + s_ * dy) / rx) ** 2
                                                       + ((-s_ * dx + c * dy) / ry) ** 2) <= 1) * a)

    def ring(self, x, y, r, width, alpha, color=None):
        """A bubble or ripple ring of radius ``r``."""
        if alpha <= 0.03:
            return
        self.color_now = color or self.default
        a = min(1.0, alpha)
        self._shape(x, y, r + width, r + width, lambda dx, dy: (np.abs(np.hypot(dx, dy) - r) <= width / 2) * a)

    def image(self, levels=4):
        s = self.ss
        a = self.a.reshape(self.h, s, self.w, s).mean(axis=(1, 3))
        prem = self.rgb.reshape(self.h, s, self.w, s, 3).mean(axis=(1, 3))
        rgb = prem / np.maximum(a, 1e-6)[..., None]
        steps = levels - 1
        a = np.floor(a * steps + 0.5) / steps
        arr = np.dstack([np.clip(rgb, 0, 1) * 255, a * 255])
        arr[a <= 0] = 0
        return Image.fromarray(np.round(arr).astype(np.uint8), "RGBA")


def loop_f(f, n):
    """Frame ``f`` of an ``n``-frame loop on the concept streams' clock (integer cycles stay
    seamless over n frames)."""
    return f * LOOP / n


def stepped(img, levels=5):
    return artkit.stepped_alpha(img, levels)


# =========================================================================== bodies at the waterline
# Shared with convoy_ships.py and l11_props.py: a model is rendered with the water surface at z = 0;
# what lies below it is pre-rendered seen through the water (toward the water colour, darker,
# stepped translucency), and its waterline (the model's slice at z = 0) carries the foam collar.

def depth_map(scene, size, extent, z_top, steps):
    """The height of the surface each straight-down ray hits (NaN for a miss): the ray march of
    render.sdf.render, repeated for the depth it does not return."""
    w, h = size
    px = extent / w
    gx, gy = np.meshgrid((np.arange(w) + 0.5 - w / 2) * px, -(np.arange(h) + 0.5 - h / 2) * px)
    n = w * h
    origin = np.stack([gx.ravel(), gy.ravel(), np.full(n, z_top)], axis=-1)
    t = np.zeros(n)
    hit = np.zeros(n, dtype=bool)
    active = np.arange(n)
    eps = px * 0.25
    for _ in range(steps):
        if active.size == 0:
            break
        p = origin[active].copy()
        p[:, 2] -= t[active]
        d, _ = scene(p)
        t[active] += d * 0.8
        done_hit = d < eps
        done_miss = t[active] > 2 * z_top
        hit[active[done_hit]] = True
        active = active[~(done_hit | done_miss)]
    return np.where(hit, z_top - t, np.nan).reshape(h, w)


def waterline_render(scene, mats, size, extent, z_top=40.0, steps=160, depth=10.0, crisp=70, levels=4, factor=None):
    """A model floating in the water (surface at z = 0), as a native sprite: above the surface
    as rendered; below it toward the water colour and darker with depth (``depth`` model units to
    the deepest tint), its alpha stepped to ``levels`` translucency levels."""
    from render import sdf as _sdf
    w, h = size
    factor = factor or artkit.factor_for(w, h)
    hi_size = (w * factor, h * factor)
    hi = _sdf.render(scene, mats, hi_size, extent, z_top=z_top, steps=steps)
    z = depth_map(scene, hi_size, extent, z_top, steps)
    dd = np.where(np.isnan(z), 0.0, np.clip(-z / depth, 0, 1))
    under = (~np.isnan(z)) & (z < 0)
    k = np.where(under, 0.35 + 0.55 * dd, 0.0)[..., None]
    water = np.array(WATER) / 255.0
    hi[..., :3] = (hi[..., :3] * (1 - k) + water * k * hi[..., 3:4]) * (1 - 0.25 * dd[..., None])
    img = np.array(artkit.native(hi, factor, crisp)).astype(np.float64)
    trans = np.where(under, 1 - 0.85 * dd, 1.0).reshape(h, factor, w, factor).mean(axis=(1, 3))
    steps_ = levels - 1
    img[..., 3] *= np.floor(trans * steps_ + 0.5) / steps_
    img[img[..., 3] < 1] = 0
    return Image.fromarray(np.round(img).astype(np.uint8), "RGBA")


def waterline_points(scene, size, extent, step=3, ss=2, pad=4):
    """Points along the model's waterline (its slice at z = 0), px relative to the sprite's
    centre, every ``step`` px, as the concept's collar wants them (the slice is taken on a grid
    ``pad`` px larger all round, so a body filling its frame keeps its edges)."""
    w, h = size
    W, H = w + 2 * pad, h + 2 * pad
    px = extent / (w * ss)
    gx, gy = np.meshgrid((np.arange(W * ss) + 0.5 - W * ss / 2) * px, -(np.arange(H * ss) + 0.5 - H * ss / 2) * px)
    d, _ = scene(np.stack([gx.ravel(), gy.ravel(), np.zeros(gx.size)], axis=-1))
    inside = (d.reshape(gx.shape) < 0).astype(np.uint8) * 255
    mask = Image.fromarray(np.dstack([inside] * 4), "RGBA").resize((W, H), Image.BOX)
    return s8.outline_points(mask, step)


def sink_foam(foam, f, pts, cx, cy, u, seed, span, solid=None):
    """The foam ring of a sinking body: the collar along its shrinking waterline, broken foam
    spreading out round the hull (``span``: its half-size in px), bubbles boiling up where the
    hull is under water and a few flecks of flotsam (``u`` 0..1 of the sinking; ``solid``, the
    frame's alpha, keeps the bubbles off what is still above the surface)."""
    rng = np.random.default_rng(seed)
    if len(pts):
        s8.collar(foam, f, pts, cx, cy, seed, alpha=0.95, r=1.5, push=1.6)
    span = np.asarray(span, np.float64)
    grow = 0.75 + 0.55 * u
    fade = 1.0 if u < 0.7 else max(0.0, 1 - (u - 0.7) / 0.3 * 0.6)
    for k in range(110):
        a = rng.uniform(0, TAU)
        v = 0.5 + 0.5 * np.sin(3 * a + seed) * np.sin(5 * a - 2 * seed + 3 * u)
        if v < 0.3 or rng.random() < 0.25:
            continue
        r = grow * rng.uniform(0.8, 1.15)
        foam.dot(cx + np.cos(a) * span[0] * r, cy + np.sin(a) * span[1] * r, 0.8 + 1.4 * v * rng.uniform(0.6, 1.2),
                 0.9 * v * fade)
    for _ in range(int(16 + 34 * min(1.0, u * 2))):
        a, r = rng.uniform(0, TAU), rng.uniform(0, 1) ** 0.6
        x, y = cx + np.cos(a) * span[0] * r * 0.85, cy + np.sin(a) * span[1] * r * 0.85
        if solid is not None:
            xi, yi = int(x), int(y)
            if 0 <= yi < solid.shape[0] and 0 <= xi < solid.shape[1] and solid[yi, xi] >= 250:
                continue
        foam.ring(x, y, rng.uniform(0.8, 2.0), 0.6, 0.8 * rng.uniform(0.4, 1), (200, 236, 245))
    for _ in range(6):
        a, r = rng.uniform(0, TAU), rng.uniform(0.6, 1.1)
        foam.ellipse(cx + np.cos(a) * span[0] * r * grow, cy + np.sin(a) * span[1] * r * grow, 1.6, 1.0, a,
                     0.9 * min(1.0, u * 3), (70, 60, 52))


# =========================================================================== explosions

def explosion(job):
    kind, rung = job
    size, n = LADDER[rung]
    under = kind == "under"
    raw = v9.splash(size, n, 31 + under + size, under)   # the concept GIF's seeds per size
    return artkit.quantize_set([stepped(f, 5) for f in raw], EXPLOSION_COLOURS)


# =========================================================================== small effects

def drop_splash(size, n, seed):
    """A small splash seen from above: a white plume, a crown of drops thrown out and falling
    back, a broken foam ring with a dark trough inside it, fading."""
    rng = np.random.default_rng(seed)
    R = size / 2
    drops = [(rng.uniform(0, TAU), rng.uniform(0.55, 1.0), rng.uniform(0.5, 1.0)) for _ in range(int(6 + size * 0.5))]
    gaps = rng.uniform(0, TAU, 3)
    out = []
    for i in range(n):
        t = i / max(n - 1, 1)
        cv = v8.Canvas(size, size)
        d = cv.dist(R, R) / R
        ang = np.arctan2(cv.y - R, cv.x - R)
        gate = np.clip(0.55 + 0.45 * np.sin(3 * ang + gaps[0]) * np.cos(2 * ang + gaps[1]) + 0.3, 0, 1)
        rr = 0.25 + 0.6 * t
        cv.over(TROUGH, v8.gauss(d - rr + 0.12, 0.08) * 0.45 * (1 - t))
        cv.over(FOAM, np.clip((0.09 - np.abs(d - rr)) / 0.09, 0, 1) * gate * (1 - t) ** 0.8)
        cv.over(v8.WHITE, v8.solid(d, 0.34 * (1 - t) ** 1.6) * (t < 0.6))
        for a, sp, s in drops:
            reach = sp * (0.2 + 0.75 * (1 - (1 - t) ** 2)) * R
            lift = np.sin(np.pi * min(1.0, t * 1.25)) * R * 0.18 * sp
            bx, by = R + np.cos(a) * reach, R + np.sin(a) * reach - lift
            cv.over((220, 240, 250), v8.solid(cv.dist(bx, by), max(0.3, s * (1 - 0.7 * t)) * size / 32 + 0.35)
                    * np.clip(1.2 - t * 1.1, 0, 1))
        out.append(stepped(cv.image(), 4))
    return out


def ripple_train(size, n, seed):
    """An uneven ripple train: three broken rings expanding with light crests and dark troughs,
    distorting, breaking up and fading (no single-pixel circles)."""
    R = size / 2
    tex = v8._cart_noise(size, seed)
    gate_tex = v8._cart_noise(size, seed + 5, cell=size * v8.SS // 3)
    out = []
    for i in range(n):
        t = (i + 1) / n
        cv = v8.Canvas(size, size)
        d = cv.dist(R, R) / R
        ang = np.arctan2(cv.y - R, cv.x - R)
        dw = d * (1 + 0.07 * np.sin(2 * ang + seed) + 0.05 * np.sin(3 * ang - seed)) + (tex - 0.5) * 0.1
        for k in range(3):
            rr = 0.12 + 0.86 * t - k * 0.17
            if rr <= 0.06:
                continue
            gate = np.clip((v8._shift(gate_tex, 9 * k, 5 * k) - 0.45 + 0.25 * t) * 3.0, 0, 1)
            gate = 1 - gate * min(0.85, 0.25 + 0.7 * t)          # breaks open as it spreads
            amp = min(1.0, (1 - t) ** 0.7 * 1.25) * (1 - 0.25 * k)
            cv.over(TROUGH, v8.gauss(dw - rr + 0.07, 0.042) * gate * amp * 0.6)
            cv.over(CREST, v8.gauss(dw - rr, 0.032) * gate * amp * 0.95)
        out.append(stepped(cv.image(), 4))
    return out


def circle_points(radius, seed, step=2.2):
    k = max(8, int(TAU * radius / step))
    a = np.linspace(0, TAU, k, endpoint=False) + seed
    r = radius * (1 + 0.04 * np.sin(3 * a + seed) + 0.03 * np.sin(5 * a - seed))
    return np.stack([np.cos(a) * r, np.sin(a) * r], 1)


def collar_frames(pts, w, h, cx, cy, seed, n=COLLAR_FRAMES, alpha=0.85, r=1.2, push=1.3, shade=True):
    """A broken, wobbling foam collar along a waterline (points relative to (cx, cy)), as an
    ``n``-frame loop: the concept's collar (scenes_r08.collar) on a ``w`` x ``h`` canvas, with a
    thinner shaded line just inside it where the water meets the body."""
    out = []
    for f in range(n):
        foam = Foam(w, h)
        if shade:
            foam.default = FOAM_SHADE
            s8.collar(foam, loop_f(f, n), pts * 0.985, cx, cy, seed + 11, alpha=0.5, r=r * 0.7, push=0.2)
            foam.default = FOAM
        s8.collar(foam, loop_f(f, n), pts, cx, cy, seed, alpha=alpha, r=r, push=push)
        out.append(foam.image(4))
    return out


def ring_collar(diam, seed):
    side = diam + 2 * COLLAR_PAD
    pts = circle_points(diam / 2, seed)
    return collar_frames(pts, side, side, side / 2, side / 2, seed, r=1.0 + diam / 80)


def foam_strip(n=4, w=32, h=10, seed=7):
    """A band of foam tileable along x: clumps flicker with integer harmonics, gaps open and
    close (loop-safe), a few flecks drift off either side."""
    rng = np.random.default_rng(seed)
    k = 26
    xs = rng.uniform(0, w, k)
    ys = h / 2 + rng.normal(0, 1.4, k)
    ph = rng.uniform(0, TAU, k)
    hk = rng.integers(1, 3, k)
    rad = rng.uniform(0.9, 1.9, k)
    out = []
    for f in range(n):
        foam = Foam(w, h, wrap_x=w)
        for x, y, p, hh, r in zip(xs, ys, ph, hk, rad):
            v = 0.5 + 0.5 * np.sin(TAU * hh * f / n + p)
            if v < 0.2:
                continue
            drift = (y - h / 2) * (0.3 + 0.4 * v)
            foam.dot(x, y + drift * 0.5, r * (0.6 + 0.5 * v), 0.6 + 0.4 * v,
                     FOAM if r > 1.2 else FOAM_SHADE)
        out.append(foam.image(4))
    return out


def small_wake(n=8, w=32, h=64, seed=41):
    """A small craft's wake: the bow wave peeling off along two V arms and a churned trail
    streaming down from the stern (loop-safe concept streams)."""
    out = []
    for f in range(n):
        foam = Foam(w, h)
        ff = loop_f(f, n)
        for side in (-1, 1):
            s8.stream(foam, ff, w / 2 + side * 2, 7, side * 0.36, 0.93, 52, 2, 22, 0.6, 3.0, 0.6, 1.3,
                      0.8, seed + side)
        s8.stream(foam, ff, w / 2, 14, 0.0, 1.0, 50, 2, 34, 1.0, 6.0, 0.7, 1.6, 0.85, seed + 9, wave=1.2)
        out.append(foam.image(4))
    return out


# =========================================================================== torpedo

def torpedo(k):
    return v8._obj(v8.torpedo_model, 18, 2.1, heading=-TAU * k / TORPEDO_HEADINGS)


def bubbles(n=6, size=10, seed=53):
    """A puff of the torpedo's bubble trail: a few small bubbles spreading out and fading."""
    rng = np.random.default_rng(seed)
    bub = [(rng.uniform(0, TAU), rng.uniform(0.3, 1.0), rng.uniform(0.55, 1.1)) for _ in range(6)]
    out = []
    for i in range(n):
        t = i / (n - 1)
        foam = Foam(size, size)
        for a, sp, r in bub:
            rr = (0.8 + 3.2 * sp * t)
            x, y = size / 2 + np.cos(a) * rr, size / 2 + np.sin(a) * rr
            foam.ring(x, y, r * (1 + 0.4 * t), 0.55, 0.95 * (1 - t) ** 0.8, (190, 235, 245))
            foam.dot(x - r * 0.35, y - r * 0.35, 0.35, 0.9 * (1 - t), v8.WHITE)
        out.append(foam.image(4))
    return out


# =========================================================================== lane telegraph and slam

def churn_a(n=CHURN_FRAMES, w=40, h=48, seed=61):
    """Variant a's churn: a band of boiling water on the lane's centre line, tileable down the
    lane: foam boils swelling and popping at their own integer rhythm, darker churned water
    between them."""
    rng = np.random.default_rng(seed)
    k = 22
    xs = w / 2 + rng.normal(0, w * 0.17, k)
    ys = rng.uniform(0, h, k)
    ph = rng.uniform(0, TAU, k)
    hk = rng.integers(1, 3, k)
    size = rng.uniform(2.0, 4.2, k)
    out = []
    for f in range(n):
        foam = Foam(w, h, wrap_y=h)
        for x, y, p, hh, s in zip(xs, ys, ph, hk, size):
            u = (hh * f / n + p / TAU) % 1.0                  # a boil's life: swell, pop, fade
            foam.dot(x, y, s * (1.2 + 0.6 * u), 0.35 * (1 - u), TROUGH)
        for x, y, p, hh, s in zip(xs, ys, ph, hk, size):
            u = (hh * f / n + p / TAU) % 1.0
            if u < 0.55:
                foam.dot(x, y, s * (0.45 + 0.9 * u), 0.95, FOAM)
                foam.dot(x + 0.6, y + 0.8, s * 0.35 * (0.5 + u), 0.8, FOAM_SHADE)
            else:
                v = (u - 0.55) / 0.45
                for j in range(5):                           # the boil breaks into flecks
                    a = p + j * TAU / 5
                    foam.dot(x + np.cos(a) * s * (0.8 + 1.4 * v), y + np.sin(a) * s * (0.7 + 1.2 * v),
                             0.9 * (1 - 0.5 * v), 0.85 * (1 - v), FOAM)
        out.append(foam.image(4))
    return out


def churn_b(n=CHURN_FRAMES, w=LANE, h=48, seed=67):
    """Variant b's churn: the whole lane boils, tileable down it: the arm's dark rust-violet
    shadow wavering under the surface across the middle, bubble rings bursting all over the lane
    and a pale seam of churned foam along both edges."""
    rng = np.random.default_rng(seed)
    k = 34
    xs = rng.uniform(6, w - 6, k)
    ys = rng.uniform(0, h, k)
    ph = rng.uniform(0, TAU, k)
    hk = rng.integers(1, 3, k)
    size = rng.uniform(1.4, 3.4, k)
    out = []
    for f in range(n):
        foam = Foam(w, h, wrap_y=h)
        for j in range(12):                                  # the shadow: a wavering band of soft dots
            y = j * h / 12
            x = w / 2 + 6 * np.sin(TAU * (j / 12 + f / n))
            foam.ellipse(x, y, 26 + 3 * np.sin(TAU * (2 * j / 12 + f / n)), 4.5, 0.0, 0.42, (34, 14, 30))
        for side in (0, 1):                                  # the edge seams
            for j in range(10):
                y = (j * h / 10 + 3 * side) % h
                x = 5 + (w - 10) * side + 1.5 * np.sin(TAU * (j * 3 / 10 + f / n) + side)
                foam.dot(x, y, 1.6, 0.55 + 0.25 * np.sin(TAU * (j / 10 + f / n)), FOAM_SHADE)
        for x, y, p, hh, s in zip(xs, ys, ph, hk, size):
            u = (hh * f / n + p / TAU) % 1.0
            if u < 0.6:
                foam.ring(x, y, s * (0.5 + 0.9 * u), 0.7, 0.9, FOAM)
                foam.dot(x - s * 0.3, y - s * 0.3, 0.5, 0.8, v8.WHITE)
            else:
                v = (u - 0.6) / 0.4
                foam.ring(x, y, s * (1.4 + 2.2 * v), 0.6, 0.6 * (1 - v), CREST)
        out.append(foam.image(4))
    return out


def mark_a():
    """Variant a's telegraph mark: a red dash for the lane edges, lit and dim (additive)."""
    out = []
    for gain in (1.0, 0.45):
        cv = v8.Canvas(6, 16)
        d = cv.seg(3, 2.5, 3, 13.5)
        cv.add((200, 20, 10), v8.gauss(d, 2.2) * 0.8 * gain)
        cv.add((255, 90, 60), v8.solid(d, 1.0) * gain)
        cv.add((255, 220, 200), v8.solid(d, 0.4) * gain * 0.8)
        out.append(artkit.additive(cv.image()))
    return out


def mark_b():
    """Variant b's telegraph mark: a red chevron pointing down the lane, lit and dim (additive)."""
    out = []
    for gain in (1.0, 0.45):
        cv = v8.Canvas(22, 14)
        d = np.minimum(cv.seg(3, 3, 11, 10.5), cv.seg(11, 10.5, 19, 3))
        cv.add((200, 20, 10), v8.gauss(d, 2.4) * 0.8 * gain)
        cv.add((255, 90, 60), v8.solid(d, 1.3) * gain)
        cv.add((255, 220, 200), v8.solid(d, 0.5) * gain * 0.8)
        out.append(artkit.additive(cv.image()))
    return out


def splash_a(n=SPLASH_FRAMES, seed=71):
    """Variant a's slam splash, one 64 px segment of the arm (along y): water thrown out to both
    sides as sheets and drops, rising and falling back, then a foam bed along the arm spreading
    and breaking up; everything stays inside the lane."""
    w, h = SPLASH
    rng = np.random.default_rng(seed)
    sheets = [(rng.choice((-1, 1)), rng.uniform(4, h - 4), rng.uniform(0.45, 1.0), rng.uniform(-0.35, 0.35))
              for _ in range(14)]
    drops = [(rng.choice((-1, 1)), rng.uniform(0, h), rng.uniform(0.3, 1.0), rng.uniform(0.5, 1.4))
             for _ in range(46)]
    bed = [(rng.normal(0, 5), rng.uniform(0, h), rng.uniform(1.2, 2.8), rng.uniform(0, TAU)) for _ in range(40)]
    out = []
    for i in range(n):
        t = i / (n - 1)
        foam = Foam(w, h)
        cx = w / 2
        reach = 1 - (1 - t) ** 2
        for side, y, sp, tilt in sheets:                      # dark wet troughs under the sheets
            x = cx + side * (6 + 40 * sp * reach)
            foam.ellipse(x, y, 7 * sp + 2, 3.5, tilt, 0.4 * (1 - t) ** 1.5, TROUGH)
        for k, (dx, y, r, p) in enumerate(bed):               # the foam bed along the arm
            spread = 1 + 2.2 * t
            v = 0.5 + 0.5 * np.sin(p + 7 * t)
            foam.dot(cx + dx * spread, y, r * (0.8 + 0.4 * v), (0.95 if t < 0.5 else 0.95 * (1 - t) / 0.5) * (0.5 + 0.5 * v))
        for k, (side, y, sp, tilt) in enumerate(sheets):      # the sheets thrown sideways, tearing into drops
            lift = np.sin(np.pi * min(1.0, t * 1.15))
            fade = np.clip(1.3 - t * 1.2, 0, 1)
            torn = min(1.0, t * 1.6)
            for j in range(6):
                u = j / 5
                x = cx + side * (5 + (40 * sp * reach) * (0.35 + 0.65 * u))
                yy = y - 3 * lift * u + tilt * 10 * u + (torn * 2.5 * np.sin(k * 3.1 + j * 1.7) if j else 0)
                r = (2.6 - 1.4 * u) * (0.7 + 0.5 * sp) * (1 - 0.35 * torn * u)
                if torn > 0.4 and (j + k) % 3 == 0:
                    continue
                foam.dot(x, yy, r, 0.95 * fade, FOAM if j < 4 else (225, 244, 252))
        for side, y, sp, r in drops:                          # drops
            x = cx + side * (4 + 52 * sp * reach)
            lift = np.sin(np.pi * min(1.0, t * 1.1)) * 6 * sp
            foam.dot(x, y - lift, r * (1 - 0.5 * t), 0.95 * np.clip(1.25 - t, 0, 1), (225, 244, 252))
        if i < 3:                                             # the impact's white flash along the arm
            foam.ellipse(cx, h / 2, 9 - 2 * i, h / 2, 0.0, 1.0 - 0.3 * i, v8.WHITE)
        out.append(foam.image(4))
    return out


def splash_b(n=SPLASH_FRAMES, seed=73):
    """Variant b's slam splash, one 64 px segment: a heavy white crown along the arm, then two
    rolling foam waves running out to the lane edges with a dark trough behind them, breaking white
    against the edges; spray from their crests."""
    w, h = SPLASH
    rng = np.random.default_rng(seed)
    crest = [(rng.uniform(0, h), rng.uniform(0.8, 2.4), rng.uniform(0, TAU)) for _ in range(30)]
    spray = [(rng.choice((-1, 1)), rng.uniform(0, h), rng.uniform(0.4, 1.0), rng.uniform(0.5, 1.2))
             for _ in range(30)]
    crown = [(rng.normal(0, 4.5), rng.uniform(0, h), rng.uniform(2.0, 4.5)) for _ in range(26)]
    out = []
    for i in range(n):
        t = i / (n - 1)
        foam = Foam(w, h)
        cx = w / 2
        front = 8 + (cx - 8) * min(1.0, (t / 0.7) ** 0.8)      # the rollers reach the edges at t = 0.7
        broke = max(0.0, (t - 0.6) / 0.4)
        for side in (-1, 1):
            x = cx + side * front
            foam.ellipse(x - side * 7, h / 2, 6, h / 2 + 2, 0.0, 0.45 * (1 - broke), TROUGH)
            for y, r, p in crest:
                wob = 1.6 * np.sin(p + y * 0.3 + 9 * t)
                foam.dot(x + wob, y, r * (1 + 0.8 * broke), 0.95 * (1 - 0.7 * broke), FOAM)
                foam.dot(x - side * 3 + wob * 0.5, y + 1, r * 0.6, 0.7 * (1 - broke), FOAM_SHADE)
        for dx, y, r in crown:                                # the crown along the arm
            k = max(0.0, 1 - t / 0.55)
            foam.dot(cx + dx * (1 + 1.5 * t), y, r * (0.4 + 0.8 * k), 0.95 * k + 0.25 * (1 - k) * (1 - t), FOAM)
        for side, y, sp, r in spray:
            x = cx + side * (front + 4 * sp)
            lift = 7 * sp * np.sin(np.pi * min(1.0, t * 1.05))
            foam.dot(min(w - 2, max(2, x)), y - lift, r * (1 - 0.5 * t), 0.9 * np.clip(1.2 - t, 0, 1), (225, 244, 252))
        if i < 3:
            foam.ellipse(cx, h / 2, 12 - 3 * i, h / 2, 0.0, 1.0 - 0.3 * i, v8.WHITE)
        out.append(foam.image(4))
    return out


LANE_LOOKS = {"a": (churn_a, mark_a, splash_a, "A - EDGE DASHES AND SPRAY SHEETS"),
              "b": (churn_b, mark_b, splash_b, "B - LANE BOIL, CHEVRONS AND ROLLERS")}


def lane_sets(variant):
    churn, mark, splash, _ = LANE_LOOKS[variant]
    return {"harbour-kraken-lane-churn": artkit.quantize_set(churn(), FX_COLOURS),
            "harbour-kraken-lane-mark": artkit.quantize_set(mark(), 16),
            "harbour-kraken-lane-splash": artkit.quantize_set(splash(), FX_COLOURS)}


# =========================================================================== build

def _job(job):
    kind, arg = job
    if kind in ("water", "under"):
        return explosion((kind, arg))
    if kind == "collar":
        return ring_collar(arg, 300 + arg)
    if kind == "torpedo":
        return torpedo(arg)
    return {"splash": lambda: drop_splash(24, 8, 81), "ripple": lambda: ripple_train(64, 12, 83),
            "strip": foam_strip, "wake": small_wake, "bubbles": bubbles,
            "tsplash": lambda: drop_splash(16, 6, 87)}[kind]()


def fx_sets():
    jobs = ([("water", r) for r in LADDER] + [("under", r) for r in LADDER] + [("collar", d) for d in COLLARS]
            + [("splash", None), ("ripple", None), ("strip", None), ("wake", None), ("bubbles", None),
               ("tsplash", None)] + [("torpedo", k) for k in range(TORPEDO_HEADINGS)])
    with ProcessPoolExecutor() as pool:
        res = list(pool.map(_job, jobs))
    sets = {}
    k = 0
    for kind in ("water", "under"):
        for rung in LADDER:
            sets[f"explosion-{kind}-{rung}"] = res[k]
            k += 1
    for d in COLLARS:
        sets[f"water-collar-{d}"] = artkit.quantize_set(res[k], 16)
        k += 1
    for name in ("water-splash", "water-ripple", "water-foam-strip", "water-wake", "torpedo-pod-bubbles",
                 "torpedo-pod-splash"):
        sets[name] = artkit.quantize_set(res[k], FX_COLOURS)
        k += 1
    sets["torpedo-pod-shot"] = artkit.quantize_set(res[k:k + TORPEDO_HEADINGS], 24)
    return sets


FX_NAMES = ([f"explosion-{kind}-{rung}" for kind in ("water", "under") for rung in LADDER]
            + [f"water-collar-{d}" for d in COLLARS]
            + ["water-splash", "water-ripple", "water-foam-strip", "water-wake", "torpedo-pod-shot",
               "torpedo-pod-bubbles", "torpedo-pod-splash"])
LANE_NAMES = ["harbour-kraken-lane-churn", "harbour-kraken-lane-mark", "harbour-kraken-lane-splash"]


def build(variant):
    sets = fx_sets()
    for name, frames in sets.items():
        artkit.write_frames(name, frames, SOURCE)
        print(f"{name}: {len(frames)} frames, {frames[0].width}x{frames[0].height}")
    lanes = lane_sets(variant)
    for name, frames in lanes.items():
        artkit.write_frames(name, frames, SOURCE + f", variant {variant}")
        print(f"{name}: {len(frames)} frames, {frames[0].width}x{frames[0].height} (variant {variant})")
    return sets, lanes


# =========================================================================== review helpers

_SEA = {}


def sea_frame(i, w, h, x0=0, y0=0, speed=140.0, fps=FPS):
    """A crop of the chosen ocean's sea (scene-ocean-r10-a: scenes_r08.Sea with round 09's colour
    stops, round 10's calmer swell) scrolling at ``speed`` px/s; a stand-in for Level 11's
    backdrop, which is built in step E3b."""
    if "sea" not in _SEA:
        _SEA["sea"] = s8.Sea(480, [(160, 1.0, 72, 1), (96, 0.7, 52, 2), (60, 0.45, 104, 2), (40, 0.28, 34, 3),
                                   (26, 0.16, 84, 3)], 1101, s9.OCEAN_STOPS, tones=7, light_gain=1.0)
    sea = _SEA["sea"]
    off = speed * i / fps
    hgt = sea.height(i % LOOP, off)
    rgb = sea.colour(sea.light(hgt))
    img = Image.fromarray(np.clip(rgb, 0, 255).astype(np.uint8), "RGB").convert("RGBA")
    return img.crop((x0, y0, x0 + w, y0 + h))


def put(img, sp, x, y):
    img.alpha_composite(sp, (int(round(x - sp.width / 2)), int(round(y - sp.height / 2))))
    return img


def put_light(img, sp, x, y):
    return artkit.add_light(img, sp, (int(round(x - sp.width / 2)), int(round(y - sp.height / 2))))


def sub_look(sp, depth=0.45):
    """A stand-in for the game's generic `sub` pass in the reviews: toward the water colour,
    darker, softer (scenes_r08.deep_tint)."""
    return s8.deep_tint(sp, WATER, depth, blur=0.6)


def timed(frames, age, step=1):
    if age < 0 or age >= len(frames) * step:
        return None
    return frames[age // step]


def label(img, x, y, text, color=None):
    from render import raster
    raster.draw_text(img, x, y, text, color or raster.LABEL)


# =========================================================================== review: production fx

def fx_review(sets):
    rows = []
    for kind, what in (("water", "SURFACE: COLUMN, SPRAY, FOAM, BROKEN RIPPLE ARCS"),
                       ("under", "UNDER WATER (SUB PASS TINTS IT): FLASH, PRESSURE RING, SILT, BUBBLES")):
        for rung in LADDER:
            frames = sets[f"explosion-{kind}-{rung}"]
            zoom = 3 if frames[0].width <= 24 else 2 if frames[0].width <= 40 else 1
            rows.append((f"EXPLOSION {kind.upper()} {rung.upper()} - {what}", frames, zoom, False))
    rows += [
        ("WATER SPLASH (A SHOT OR A UNIT DROPPING IN)", sets["water-splash"], 4, False),
        ("RIPPLE TRAIN (AFTER A KILL SINKS, SURFACING)", sets["water-ripple"], 2, False),
        ("FOAM COLLARS 24 / 40 / 64 PX, 4-FRAME LOOPS", sets["water-collar-24"] + sets["water-collar-40"]
         + sets["water-collar-64"], 2, False),
        ("FOAM STRIP 32X10, TILEABLE ALONG X (AN ARM AWASH)", sets["water-foam-strip"], 5, False),
        ("SMALL CRAFT WAKE 32X64, 8-FRAME LOOP", sets["water-wake"], 3, False),
        ("TORPEDO 32 HEADINGS (5X16 BODY)", sets["torpedo-pod-shot"][:16], 4, False),
        ("TORPEDO 32 HEADINGS (CONT.)", sets["torpedo-pod-shot"][16:], 4, False),
        ("TORPEDO BUBBLE PUFF, DROP SPLASH", sets["torpedo-pod-bubbles"] + sets["torpedo-pod-splash"], 6, False),
    ]
    sheet = artkit.review_sheet("WATER AND TORPEDO EFFECTS - PRODUCTION SPRITES (ROUND 33)", rows, width=1500,
                                batch=BATCH)
    sheet = sheet_on_sea(sheet, sets)
    artkit.save_review(sheet, fx_loop(sets), CONCEPT, "water-fx", fps=FPS)


def sheet_on_sea(sheet, sets):
    """Append a strip of the effects over the sea stand-in at 2x (they are judged on water)."""
    w, h = 300, 150
    cell = sea_frame(30, w, h, 90, 200)
    for k, rung in enumerate(LADDER):
        fr = sets[f"explosion-water-{rung}"]
        put(cell, fr[len(fr) // 3], 24 + [0, 30, 80, 170][k] + fr[0].width / 2 - 6, 70)
    put(cell, sets["water-collar-40"][1], 30, 125)
    put(cell, sets["water-ripple"][4], 80, 125)
    put(cell, sets["water-wake"][2], 240, 110)
    big = sprite.enlarge(cell, 2)
    out = Image.new("RGBA", (sheet.width, sheet.height + big.height + 40), artkit.SHEET_BG)
    out.alpha_composite(sheet)
    label(out, 16, sheet.height + 6, "ON THE OCEAN STAND-IN (SCENE-OCEAN-R10-A'S SEA), 2X: SURFACE RUNGS AT A THIRD OF "
                                     "THEIR RUN, A COLLAR, A RIPPLE TRAIN, A WAKE")
    out.alpha_composite(big, (16, sheet.height + 22))
    return out


def fx_loop(sets):
    """240x320 of the ocean stand-in at 2x: kills on the surface (the four rungs, each leaving a
    ripple train), a buoy-sized body bobbing in its collar, a raft-sized block drifting with its
    wake, then two torpedoes dropping in (splash), running up with their bubble trails and hitting
    a submerged dummy (the under-water small rung)."""
    w, h, n = 240, 320, 120
    kills = [(4, "small", 70, 90), (12, "tiny", 170, 60), (22, "medium", 120, 150), (40, "large", 140, 110),
             (64, "under-small", 60, 200), (72, "under-medium", 180, 230)]
    torps = [(78, 96, 300), (84, 144, 300)]
    target = (120, 120)
    out = []
    for i in range(n):
        img = sea_frame(i, w, h, 120, 160)
        # the submerged dummy (a dark disc through the sub stand-in) until the torpedoes hit
        if i < 100:
            disc = Image.new("RGBA", (22, 22), (0, 0, 0, 0))
            ImageDraw.Draw(disc).ellipse([1, 1, 20, 20], fill=(70, 60, 80, 255))
            put(img, sub_look(disc, 0.5), *target)
        # the bobbing buoy-sized body with its collar
        bob = Image.new("RGBA", (36, 36), (0, 0, 0, 0))
        ImageDraw.Draw(bob).ellipse([2, 2, 33, 33], fill=(150, 70, 50, 255), outline=(90, 40, 30, 255))
        put(img, bob, 40, 280)
        put(img, sets["water-collar-40"][(i // 3) % COLLAR_FRAMES], 40, 280)
        # a raft-sized block with its wake
        put(img, sets["water-wake"][(i // 2) % 8], 205, 290 + 26)
        block = Image.new("RGBA", (16, 20), (88, 92, 70, 255))
        put(img, block, 205, 298)
        for t0, rung, x, y in kills:
            under = rung.startswith("under")
            name = f"explosion-under-{rung[6:]}" if under else f"explosion-water-{rung}"
            fr = timed(sets[name], i - t0, 1)
            if fr is not None:
                put(img, sub_look(fr, 0.25) if under else fr, x, y)
            rp = timed(sets["water-ripple"], i - t0 - len(sets[name]) // 2, 2)
            if rp is not None and not under:
                put(img, rp, x, y)
        for t0, x, y in torps:
            age = i - t0
            if age < 0:
                continue
            sp = timed(sets["torpedo-pod-splash"], age, 2)
            if sp is not None:
                put(img, sp, x, y)
            # accelerate 300 -> 420 px/s over 0.5 s, steer toward the target
            pos = np.array([x, y], float)
            head = 0.0
            trail = []
            hit = False
            for s in range(age):
                speed = 300 + 120 * min(1.0, s / (0.5 * FPS))
                want = np.arctan2(target[0] - pos[0], -(target[1] - pos[1]))
                delta = (want - head + np.pi) % TAU - np.pi
                head += np.clip(delta, -np.radians(60) / FPS * 3, np.radians(60) / FPS * 3)
                pos += np.array([np.sin(head), -np.cos(head)]) * speed / FPS
                trail.append((pos.copy(), s))
                if np.hypot(*(pos - target)) < 12:
                    hit = True
                    break
            for p, s in trail:
                b = timed(sets["torpedo-pod-bubbles"], age - s, 2)
                if b is not None:
                    put(img, sub_look(b, 0.2), *p)
            if hit:
                hit_at = t0 + len(trail)
                fr = timed(sets["explosion-under-small"], i - hit_at, 1)
                if fr is not None:
                    put(img, sub_look(fr, 0.25), *target)
            else:
                k = round(head / TAU * TORPEDO_HEADINGS) % TORPEDO_HEADINGS
                put(img, sub_look(sets["torpedo-pod-shot"][k], 0.3), *pos)
        out.append(sprite.enlarge(img, 2))
    return out


# =========================================================================== review: lane telegraph and slam

def stand_in_arm(length, rise):
    """A stand-in for the slam arm (E1b draws the real one): a rust tapered tube, ``rise`` 0..1
    of it above the water from the base."""
    img = Image.new("RGBA", (40, length), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    up = int(length * rise)
    for y in range(length):
        r = 13 - 9 * y / length
        if y < up:                                   # rising base (the platform) to tip
            col = (128, 62, 40, 255) if (y // 6) % 2 else (110, 52, 34, 255)
        else:
            col = (60, 50, 60, 150)
        d.ellipse([20 - r, y - 2, 20 + r, y + 2], fill=col)
    return img


def lane_review(variant, lanes, fx):
    churn, mark, splash = (lanes[n] for n in LANE_NAMES)
    title = LANE_LOOKS[variant][3]
    ship = artkit.load_frames("cargo-ship")
    ship = ship[0] if ship else None
    rows = [
        ("TELEGRAPH CHURN, TILEABLE DOWN THE LANE (6-FRAME LOOP)", churn, 3 if churn[0].width < 60 else 2, False),
        ("TELEGRAPH MARK, LIT AND DIM (ADDITIVE, BLINKING 5 HZ)", mark, 6, True),
        ("SLAM SPLASH, ONE 64 PX SEGMENT (10 FRAMES, 20 FPS)", splash, 2, False),
    ]
    sheet = artkit.review_sheet(f"LANE TELEGRAPH AND SLAM {title} (ROUND 33 PROPOSAL {variant.upper()})", rows,
                                width=1500, batch=BATCH)
    loop = lane_loop(variant, lanes, fx, ship)
    stills = [loop[k].resize((loop[k].width // 2, loop[k].height // 2), Image.NEAREST) for k in (12, 30, 37, 41, 56)]
    strip = Image.new("RGBA", (sheet.width, stills[0].height + 40), artkit.SHEET_BG)
    label(strip, 16, 6, "IN THE ARENA (OCEAN STAND-IN, 1X; STAND-IN ARM): TELEGRAPH, RISE, IMPACT, SPLASH RUNNING TO THE TIP, AWASH")
    x = 16
    for s in stills:
        strip.alpha_composite(s, (x, 22))
        x += s.width + 8
        if x + s.width > strip.width:
            break
    out = Image.new("RGBA", (sheet.width, sheet.height + strip.height), artkit.SHEET_BG)
    out.alpha_composite(sheet)
    out.alpha_composite(strip, (0, sheet.height))
    folder = CONCEPT if variant == PRODUCTION else CONCEPT / "rejected"   # the user's pick, round 33
    png = folder / f"slam-{REVIEW_ROUND}-{variant}.png"
    gif = folder / f"slam-{REVIEW_ROUND}-{variant}.gif"
    out.convert("RGB").save(png, optimize=True)
    write_gif(loop, gif, fps=FPS, colors=128)
    print(f"review: {png.relative_to(ROOT)}, {gif.relative_to(ROOT)}")


def lane_loop(variant, lanes, fx, ship):
    """Two lanes (240 x 300 of the arena below the platform) at 2x: calm, the 1.0 s telegraph in
    lane 1 where a cargo ship holds station, the stand-in arm rising 0.5 s, the impact laid along
    the arm base to tip, awash 1.5 s on a foam strip, sinking 0.6 s; the ship takes its hit."""
    churn, mark, splash = (lanes[n] for n in LANE_NAMES)
    w, h, n = 240, 300, 100
    lane_x = 60
    t_tel, t_rise, t_hit, t_sink, t_gone = 6, 26, 36, 66, 78
    ship_y = 230
    out = []
    for i in range(n):
        img = sea_frame(i, w, h, 120, 220, speed=0.0)
        tel = t_tel <= i < t_hit + 4
        if tel:
            u = min(1.0, (i - t_tel) / 6)
            ch = churn[(i // 2) % CHURN_FRAMES]
            if u < 1:
                ch = Image.fromarray((np.array(ch) * np.array([1, 1, 1, u])).astype(np.uint8), "RGBA")
            for y in range(-ch.height, h + ch.height, ch.height):
                put(img, ch, lane_x, y + ch.height / 2)
        if ship is not None:
            put(img, ship, lane_x, ship_y)
        if tel and i < t_hit:
            mk = mark[0 if (i // 2) % 2 == 0 else 1]
            if variant == "a":
                for ex in (lane_x - LANE / 2 + 3, lane_x + LANE / 2 - 3):
                    for y in range(8, h, 24):
                        img = put_light(img, mk, ex, y)
            else:
                for y in range(16, h, 40):
                    img = put_light(img, mk, lane_x, y + (i % 8) * 2)
        arm_len = h - 10
        if t_rise <= i < t_gone:
            rise = min(1.0, (i - t_rise) / 10) if i < t_sink else max(0.0, 1 - (i - t_sink) / 12)
            if i >= t_hit:
                for y in range(0, arm_len, 24):
                    put(img, fx["water-foam-strip"][(i // 3) % 4].rotate(90, expand=True), lane_x - 16, y + 12)
                    put(img, fx["water-foam-strip"][(i // 3 + 2) % 4].rotate(90, expand=True), lane_x + 16, y + 12)
            put(img, stand_in_arm(arm_len, rise), lane_x, arm_len / 2)
        if i >= t_hit:
            for k, y in enumerate(range(0, h, SEGMENT)):
                fr = timed(splash, i - t_hit - k, 1)
                if fr is not None:
                    put(img, fr, lane_x, y + SPLASH[1] / 2)
            if ship is not None:
                fr = timed(fx["explosion-water-small"], i - t_hit, 1)
                if fr is not None:
                    put(img, fr, lane_x + 8, ship_y - 10)
        label(img, 4, 4, ["CALM", "TELEGRAPH 1.0 S", "RISE 0.5 S", "IMPACT", "AWASH 1.5 S", "SINK 0.6 S", ""][
            0 if i < t_tel else 1 if i < t_rise else 2 if i < t_hit else 3 if i < t_hit + 8 else 4 if i < t_sink
            else 5 if i < t_gone else 6])
        # the lane borders, faint (the review's guide, not drawn by the game)
        d = ImageDraw.Draw(img)
        d.line([(LANE, 0), (LANE, h)], fill=(255, 255, 255, 40))
        out.append(sprite.enlarge(img, 2))
    return out


def load(names):
    return {n: artkit.load_frames(n) for n in names}


if __name__ == "__main__":
    artkit.REVIEW_ROUND = REVIEW_ROUND
    args = sys.argv[1:]
    chosen = args[args.index("--variant") + 1] if "--variant" in args else PRODUCTION
    if "--review" not in args:
        fx, lanes = build(chosen)
    else:
        chosen = PRODUCTION
        fx, lanes = load(FX_NAMES), load(LANE_NAMES)
    fx_review(fx)
    lane_review(chosen, lanes, fx)
    for other in sorted(set(LANE_LOOKS) - {chosen}):
        lane_review(other, lane_sets(other), fx)
