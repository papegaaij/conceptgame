#!/usr/bin/env python3
"""Concept round 07 - Harbour Kraken redo: a creature that operates from the depths.

The round-06 mockup was rejected (arms looked detached, the "waves" were drawn circles, the
head popped up instead of surfacing). This redo follows the Water rules in
design/art-direction/README.md:

* one connected animal - every arm runs under water to the head; submerged parts are drawn
  through the water (tinted, darker, blurred and refracted, fainter the deeper they are) and the
  mantle lurks as a large dark shape below the platform;
* real water - an animated height field (directional swell bands, warped crests, moon glints)
  that every ripple, wake, swell and splash is *added to*, so they are lit like waves instead of
  outlined; irregular foam particles at every waterline, spray and mist on impact;
* surfacing top-down - the mantle sprite carries a height map; a pixel is above water when its
  height exceeds the water level, so the crown breaks first, then the eyes and the rest, with a
  swell before and water streaming off after; diving reverses it. Slam arms rise base to tip out
  of a churning, telegraphed lane.

Outputs (design/enemies/bosses/concept/):
  harbour-kraken-r07-a.png   sheet: labelled set-up with the submerged parts, surfacing and
                             diving sequences, arm entering the water, slam sequence, phases
  harbour-kraken-r07-a.gif   10.4 s seamless loop at 15 fps in the 480x540 play field (night
                             ocean; the swell updates at 7.5 fps and the water is posterized so the
                             GIF stays under 8 MB)

Re-uses the round-06 platform and arm segment models (render/boss_models.py) and helpers from
bosses_r06.py. Run: python3 tools/concept/kraken_r07.py   (~8 min)
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, str(Path(__file__).resolve().parent))
import bosses_r06 as b6  # noqa: E402
import enemies_r03 as e3  # noqa: E402
import enemies_r05 as r5  # noqa: E402
from render import boss_models as bm  # noqa: E402
from render import raster, sdf, sprite  # noqa: E402
from render.config import ROOT  # noqa: E402

FW, FH, TAU = b6.FW, b6.FH, 2 * np.pi
FPS = 15                                   # 15 fps keeps the loop GIF under 8 MB
T = 10.4                                   # loop length (s)
NF = int(round(T * FPS))
DT = 1.0 / FPS
POSTER = 10.0                              # water tone step (fewer tones: smaller GIF)
SUB = "CONCEPT ROUND 07 - 960X540"
OUT = b6.OUT
YY, XX = np.mgrid[0:FH, 0:FW].astype(np.float32)
W2 = TAU / T                               # base angular frequency (everything loops in T)

# water colours (night ocean)
DEEP = np.array([7, 27, 60], np.float32)
SUBTINT = np.array([6, 30, 64], np.float32)
LIGHT = np.array([52, 92, 140], np.float32)
GLINT = np.array([205, 228, 255], np.float32)
FOAM = (232, 246, 255)

# layout
HX, HY = 240.0, 262.0                      # head sprite centre
H_EXT, H_W, H_H, H_CY = 1.7, 150, 266, 0.075
HS = H_W / H_EXT                           # px per model unit (~88)
CROWN = (HX, HY + (-0.5 + H_CY) * HS)      # highest point of the mantle (~ y 225)
BEAK = (HX, HY + (1.04 + H_CY) * HS)       # ~ y 360
PLAT = (240.0, 110.0)
P_W, P_E, P_S, P_N = 136, 344, 186, 34     # platform frame edges (screen)
ROOT_H = 0.15                              # model height of the arm roots


def ease(s):
    s = float(np.clip(s, 0.0, 1.0))
    return s * s * (3 - 2 * s)


def sstep(a, b, x):
    return float(np.clip((x - a) / (b - a), 0.0, 1.0)) ** 2 * (3 - 2 * float(np.clip((x - a) / (b - a), 0.0, 1.0)))


# =========================================================================== head model

def _head_scene(eyes, beak):
    scene, mats = bm.kraken_mantle(eyes, beak)
    # arms end (+Y) faces the player (screen down): rotate the model by 180 degrees
    return (lambda p, s=scene: s(sdf.rotate_z(p, np.pi))), mats


def heightmap(scene, size, extent, center, z_top=3.0, steps=120):
    """Height of the first surface hit by a straight-down ray per pixel (NaN = miss), using
    the same pixel grid as sdf.render."""
    w, h = size
    px = extent / w
    xs = (np.arange(w) + 0.5 - w / 2) * px + center[0]
    ys = -(np.arange(h) + 0.5 - h / 2) * px + center[1]
    gx, gy = np.meshgrid(xs, ys)
    n = w * h
    origin = np.stack([gx.ravel(), gy.ravel(), np.full(n, z_top)], axis=-1)
    t = np.zeros(n)
    hit = np.zeros(n, bool)
    active = np.arange(n)
    for _ in range(steps):
        if active.size == 0:
            break
        p = origin[active].copy()
        p[:, 2] -= t[active]
        d, _ = scene(p)
        t[active] += d * 0.8
        done = d < px * 0.25
        miss = t[active] > 2 * z_top
        hit[active[done]] = True
        active = active[~(done | miss)]
    z = np.where(hit, z_top - t, np.nan)
    return z.reshape(h, w)


_head = {}


def head_sprite(eyes, beak):
    key = (eyes, beak)
    if key not in _head:
        scene, mats = _head_scene(eyes, beak)
        hi = sdf.render(scene, mats, (H_W * 2, H_H * 2), H_EXT, center=(0.0, H_CY))
        _head[key] = np.asarray(sprite.make_sprite(hi, 2, 56, crisp=70)).astype(np.float32)
    return _head[key]


_hm = {}


def head_height():
    if "z" not in _hm:
        scene, _ = _head_scene(0.0, 0.0)
        _hm["z"] = heightmap(scene, (H_W, H_H), H_EXT, (0.0, H_CY))
    return _hm["z"]


# =========================================================================== timeline

# head water level (model units): deep -> rise (swell) -> surface top-down -> up -> dive
L_KEYS = [(0.0, 1.3), (2.4, 1.3), (3.4, 0.62), (4.8, 0.04), (6.8, 0.04), (8.0, 0.7),
          (8.8, 1.3), (T, 1.3)]


def head_level(t):
    t = t % T
    for (t0, a), (t1, b) in zip(L_KEYS, L_KEYS[1:]):
        if t0 <= t <= t1:
            return a + (b - a) * ease((t - t0) / (t1 - t0))
    return 1.3


def head_dlevel(t):
    return (head_level(t + 0.025) - head_level(t - 0.025)) / 0.05


EYES_OPEN = (4.9, 6.8)
FANS = (5.2, 5.9, 6.5)
SLAMS = [(0.3, 150.0, 0), (7.3, 330.0, 1)]       # start, lane x, root index (left/right)
TEL, RISE, HOLD, FALL, LIE, SINK = 0.8, 0.5, 0.4, 0.12, 0.48, 0.7
IMPACT = TEL + RISE + HOLD + FALL


def player_pos(t):
    return (240 + 70 * np.sin(W2 * 2 * t) + 12 * np.sin(W2 * 5 * t), 500.0)


# =========================================================================== arms

def _root(a):
    return (HX - 0.62 * np.sin(a) * HS, HY + (0.9 + 0.36 * np.cos(a) + H_CY) * HS)


ROOTS = {k: _root(a) for k, a in zip(range(8), np.linspace(-1.6, 1.6, 8))}
# indices: 0 right-outer, 1 right-south, 2 right-idle, 3 right-slam, 4 left-slam, 5 left-idle,
#          6 left-south, 7 left-outer


def mirror(pts):
    return [(FW - x, y) for x, y in pts]


SIDE_L = [(150, 336), (110, 292), (94, 222), (104, 160), (P_W - 2, 128), (166, 116),
          (190, 96), (182, 72), (162, 76)]
SOUTH_L = [(160, 362), (136, 322), (146, 262), (168, 214), (182, P_S + 2), (186, 160),
           (206, 142), (224, 150), (218, 168)]
IDLE_L = [(176, 404), (128, 432), (94, 474), (70, 520), (52, 556)]
SLAM_REST_L = [(212, 398), (196, 410), (182, 402), (176, 386), (186, 372)]
SLAM_LANE_L = [(206, 402), (168, 432), (152, 482), (150, 532), (150, 590)]


def catmull(points, per=10):
    p = np.array(points, np.float64)
    p = np.vstack([2 * p[0] - p[1], p, 2 * p[-1] - p[-2]])
    out = []
    for i in range(1, len(p) - 2):
        p0, p1, p2, p3 = p[i - 1], p[i], p[i + 1], p[i + 2]
        for s in np.linspace(0, 1, per, endpoint=False):
            out.append(0.5 * ((2 * p1) + (-p0 + p2) * s + (2 * p0 - 5 * p1 + 4 * p2 - p3) * s * s
                              + (-p0 + 3 * p1 - 3 * p2 + p3) * s ** 3))
    out.append(p[-2])
    return np.array(out)


class ArmPose:
    """A sampled arm: segment centres, headings, sizes, heights above the water (e<0 = depth)
    and the tip."""

    def __init__(self, pts, heads, sizes, es, tip, tip_h, tip_e, key):
        self.pts, self.heads, self.sizes, self.es = pts, heads, sizes, es
        self.tip, self.tip_h, self.tip_e, self.key = tip, tip_h, tip_e, key


def sample_arm(ctrl, e_of_u, key, s0=40, s1=14, edge_index=None):
    """Sample segments along a Catmull-Rom path through ``ctrl``. ``e_of_u(u, u_edge)`` gives
    the height above water at arc fraction u (u_edge: arc fraction of control point
    ``edge_index``, where the arm climbs onto the platform)."""
    poly = catmull(ctrl)
    seg = np.diff(poly, axis=0)
    ln = np.hypot(seg[:, 0], seg[:, 1])
    cum = np.concatenate([[0.0], np.cumsum(ln)])
    total = cum[-1]
    u_edge = None
    if edge_index is not None:
        u_edge = cum[min(edge_index * 10, len(cum) - 1)] / total

    def at(s):
        i = int(np.clip(np.searchsorted(cum, s) - 1, 0, len(seg) - 1))
        f = (s - cum[i]) / max(ln[i], 1e-6)
        p = poly[i] + seg[i] * f
        return p, float(np.arctan2(seg[i][1], seg[i][0]))

    pts, heads, sizes, es = [], [], [], []
    s = s0 * 0.35
    while s < total - 6:
        u = s / total
        size = s0 + (s1 - s0) * u
        p, h = at(s)
        pts.append((float(p[0]), float(p[1])))
        heads.append(h)
        sizes.append(size)
        es.append(float(e_of_u(u, u_edge)))
        s += 0.5 * size
    tp, th = at(total - 0.01)
    return ArmPose(pts, heads, sizes, es, (float(tp[0]), float(tp[1])), th,
                   float(e_of_u(1.0, u_edge)), key)


def sway(pts, t, amp, seed, start=0):
    out = []
    for i, (x, y) in enumerate(pts):
        k = max(0, i - start)
        a = amp * min(1.0, k / 2.0)
        out.append((x + a * np.sin(W2 * 3 * t + seed + 0.9 * i),
                    y + a * 0.7 * np.cos(W2 * 2 * t + seed * 1.3 + 0.7 * i)))
    return out


def wrapped_e(root_e, deck=0.25):
    def f(u, ue):
        if u < ue:
            return root_e * (1 - u / ue) ** 1.2 - 0.03 * min(1.0, u / ue * 3)
        return deck
    return f


def slam_state(t, start):
    """(phase, morph 0..1, e-function factory args) for one slam cycle; None when inactive."""
    r = (t % T) - start
    if r < 0 or r >= IMPACT + LIE + SINK:
        return None
    return r


def arm_poses(t):
    """All eight arms for time t (loop-periodic)."""
    L = head_level(t)
    root_e = ROOT_H - L
    poses = []
    # wrapped arms: side and south, left and mirrored right
    for key, ctrl, ri, ei, seed in (("side-l", SIDE_L, 7, 4, 0.0), ("south-l", SOUTH_L, 6, 4, 1.7),
                                    ("side-r", mirror(SIDE_L), 0, 4, 3.1),
                                    ("south-r", mirror(SOUTH_L), 1, 4, 4.4)):
        under = sway(ctrl[:ei], t, 7, seed, start=0)
        deck = sway(ctrl[ei:], t, 2.5, seed + 2, start=1)
        path = [ROOTS[ri]] + under + deck
        poses.append(sample_arm(path, wrapped_e(root_e), key, 40, 14, edge_index=ei + 1))
    # idle arms trailing deep below
    for key, ctrl, ri, seed in (("idle-l", IDLE_L, 5, 0.5), ("idle-r", mirror(IDLE_L), 2, 2.5)):
        path = [ROOTS[ri]] + sway(ctrl, t, 14, seed)
        poses.append(sample_arm(path, lambda u, ue, re=root_e: re - 0.35 - 0.45 * u, key, 36, 12))
    # slam arms
    for start, lane, side in SLAMS:
        ri = 4 if side == 0 else 3
        rest = SLAM_REST_L if side == 0 else mirror(SLAM_REST_L)
        lane_pts = [(x + (lane - 150), y) for x, y in SLAM_LANE_L] if side == 0 else \
            [(FW - x + (lane - 330), y) for x, y in SLAM_LANE_L]
        r = slam_state(t, start)
        if r is None:
            m, body = 0.0, (lambda u: -0.85)
        else:
            m, body = slam_body(r)
        ctrl = [(a[0] + (b[0] - a[0]) * m, a[1] + (b[1] - a[1]) * m) for a, b in zip(rest, lane_pts)]
        ctrl = sway(ctrl, t, 6 * (1 - m) + 1.5, 5.0 + side)

        def ef(u, ue, re=root_e, body=body):
            w = sstep(0.0, 0.22, u)
            return re * (1 - w) + body(u) * w
        poses.append(sample_arm([ROOTS[ri]] + ctrl, ef, f"slam-{side}", 42, 14))
    return poses


def emax(u):
    return 0.5 * sstep(0.05, 0.4, u) * (1 - 0.35 * u)


def slam_body(r):
    """Morph (rest -> lane) and the arm body height profile for a slam cycle at time r."""
    if r < TEL:                                        # churn + arm slides in under water
        m = ease(r / TEL)
        return m, (lambda u, k=ease(r / TEL): -0.85 + 0.53 * k)
    r -= TEL
    if r < RISE:                                       # rises base to tip
        p = r / RISE
        return 1.0, (lambda u, p=p: -0.32 + (emax(u) + 0.32) * ease(p * 1.6 - 0.6 * u))
    r -= RISE
    if r < HOLD:
        return 1.0, (lambda u, r=r: emax(u) + 0.03 * np.sin(9 * r + 5 * u))
    r -= HOLD
    if r < FALL:                                       # slams down (accelerating)
        q = (r / FALL) ** 2
        return 1.0, (lambda u, q=q: emax(u) * (1 - q) - 0.03 * q)
    r -= FALL
    if r < LIE:                                        # awash in the lane
        return 1.0, (lambda u, r=r: -0.03 - 0.01 * np.sin(6 * r + 4 * u))
    r -= LIE
    k = ease(r / SINK)                                 # sinks back, darker and fainter
    return 1.0 - k, (lambda u, k=k: -0.03 - 0.82 * k)


# =========================================================================== water

def _wave_components():
    rng = np.random.default_rng(11)
    comps = []
    for lam, amp, dang in ((170, 0.95, 0.0), (104, 0.7, 0.32), (70, 0.5, -0.38), (47, 0.36, 0.7),
                           (32, 0.22, -0.85)):
        ang = 1.12 + dang                          # travelling down / down-right
        k = TAU / lam
        speed = 13.0 * np.sqrt(lam / 60.0)
        n = max(1, int(round(speed * T / lam)))
        comps.append((k * np.cos(ang), k * np.sin(ang), amp, W2 * n, rng.uniform(0, TAU)))
    warp = [raster.fbm(FW, FH, 90, 30 + i, octaves=2, period=False).astype(np.float32)
            for i in range(3)]
    return comps, warp


COMPS, WARP = _wave_components()


def base_height(t):
    """The swell is updated at 7.5 fps (it moves only ~2-3 px per update); ripples, foam and
    sprites run at the full frame rate. Unchanged pixels compress away in the GIF."""
    t = np.floor(t * 7.5 + 1e-6) / 7.5
    h = np.zeros((FH, FW), np.float32)
    for i, (kx, ky, a, w, ph) in enumerate(COMPS):
        h += a * np.sin(kx * XX + ky * YY + 2.2 * (WARP[i % 3] - 0.5) - w * t + ph)
    return h


class Ripples:
    """Expanding ripple trains added to the height field: several uneven crests behind a front,
    broken up around the circle and wobbling, decaying with age."""

    def __init__(self):
        self.src = []

    def add(self, t, x, y, amp=0.6, lam=11.0, speed=34.0, tau=1.0, life=2.0, seed=None):
        seed = float(np.random.default_rng(int(abs(x * 13 + y * 7 + t * 101)) % 2**31).uniform(0, 50)) \
            if seed is None else seed
        self.src.append((t, x, y, amp, lam, speed, tau, life, seed))

    def prune(self, t):
        self.src = [s for s in self.src if t - s[0] < s[7]]

    def apply(self, h, t):
        for t0, x, y, amp, lam, speed, tau, life, seed in self.src:
            age = t - t0
            if age <= 0 or age >= life:
                continue
            front = speed * age
            R = front + 2.5 * lam
            x0, x1 = int(max(0, x - R)), int(min(FW, x + R + 1))
            y0, y1 = int(max(0, y - R)), int(min(FH, y + R + 1))
            if x0 >= x1 or y0 >= y1:
                continue
            dx = XX[y0:y1, x0:x1] - x
            dy = YY[y0:y1, x0:x1] - y
            r = np.sqrt(dx * dx + dy * dy)
            th = np.arctan2(dy, dx)
            rr = r * (1 + 0.07 * np.sin(2 * th + seed) + 0.04 * np.sin(3 * th + 2 * seed + 0.7 * age))
            env = np.exp(-((rr - 0.72 * front) / (0.38 * front + lam)) ** 2)
            brk = 0.5 + 0.5 * np.sin(2 * th + 1.7 * seed + 0.8 * age) * np.sin(3 * th - seed + 0.5 * age)
            brk = np.clip(0.25 + brk, 0, 1)
            a = amp * np.exp(-age / tau) / np.sqrt(1 + front / 40)
            h[y0:y1, x0:x1] += a * env * brk * np.sin(TAU * (rr - front) / lam)


def swell(h, L):
    """A rising mound of displaced water over the crown before it breaks the surface."""
    a = 1.6 * sstep(1.05, 0.62, L) * (1 - sstep(0.56, 0.3, L))
    if a > 0:
        dx = (XX - CROWN[0]) / 40.0
        dy = (YY - CROWN[1] - 10) / 56.0
        h += a * np.exp(-(dx * dx + dy * dy))


def shade(h):
    """Wave lighting: returns (signed light, glints, slopes) from the height-field slopes."""
    gy, gx = np.gradient(h)
    s = 3.0
    nx, ny = -gx * s, -gy * s
    inv = 1.0 / np.sqrt(nx * nx + ny * ny + 1)
    nx, ny, nz = nx * inv, ny * inv, inv
    lx, ly, lz = -0.48, -0.56, 0.68
    diff = nx * lx + ny * ly + nz * lz
    light = (diff - lz) * 1.15 + 0.035 * h
    # moon glints: reflected view ray close to the moon direction
    rx, ry, rz = 2 * nz * nx, 2 * nz * ny, 2 * nz * nz - 1
    mx, my, mz = -0.30, -0.42, 0.856
    sp = np.clip(rx * mx + ry * my + rz * mz, 0, 1) ** 320
    return light.astype(np.float32), sp.astype(np.float32), gx, gy


# =========================================================================== particles

def _blob(r, seed, soft=1.2):
    n = int(2 * r + 4)
    rng = np.random.default_rng(seed)
    yy, xx = np.mgrid[0:n, 0:n] - (n - 1) / 2
    th = np.arctan2(yy, xx)
    rad = r * (1 + 0.12 * np.sin(2 * th + rng.uniform(0, 6)) + 0.08 * np.sin(4 * th + rng.uniform(0, 6))
               + 0.05 * np.sin(7 * th + rng.uniform(0, 6)))
    d = np.sqrt(xx * xx + yy * yy)
    a = np.clip((rad - d) / soft, 0, 1)
    core = np.clip((rad * 0.55 - d) / soft, 0, 1)
    rgb = np.zeros((n, n, 3))
    rgb[:] = (150, 205, 235)
    rgb = rgb * (1 - core[..., None]) + np.array(FOAM) * core[..., None]
    return rgb, a


BLOBS = {}
for _r in (1.2, 1.8, 2.6, 3.6, 5.0, 7.0, 10.0, 15.0):
    for _v in range(3):
        _rgb, _a = _blob(_r, int(_r * 10) + _v, soft=1.2 if _r < 8 else 3.0)
        BLOBS[(_r, _v)] = [Image.fromarray(np.dstack([_rgb, _a * 255 * lv]).astype(np.uint8), "RGBA")
                           for lv in (0.2, 0.4, 0.65, 0.9)]
RADII = sorted({k[0] for k in BLOBS})


class Particles:
    """Foam (floats, drifts with the current), spray droplets and mist puffs (airborne)."""

    def __init__(self):
        self.p = []                 # [x, y, vx, vy, age, life, radius, variant, alpha, kind, drag]

    def add(self, x, y, vx=0.0, vy=0.0, life=1.0, r=2.6, alpha=0.9, kind="foam", drag=1.6):
        rr = min(RADII, key=lambda q: abs(q - r))
        self.p.append([x, y, vx, vy, 0.0, life, rr, len(self.p) % 3, alpha, kind, drag])

    def step(self, dt):
        alive = []
        for q in self.p:
            q[4] += dt
            if q[4] >= q[5]:
                continue
            q[0] += (q[2] + (3.0 if q[9] == "foam" else 0)) * dt
            q[1] += (q[3] + (5.0 if q[9] == "foam" else 0)) * dt
            k = np.exp(-q[10] * dt)
            q[2] *= k
            q[3] *= k
            alive.append(q)
        self.p = alive

    def draw(self, img, kinds):
        d = ImageDraw.Draw(img)
        for x, y, vx, vy, age, life, r, v, alpha, kind, drag in self.p:
            if kind not in kinds or not (-20 < x < FW + 20 and -20 < y < FH + 20):
                continue
            s = age / life
            fade = min(1.0, age / 0.08) * (1 - s) ** 0.7 * alpha
            if kind == "spray":           # droplet with a short streak
                c = FOAM + (int(255 * fade),)
                d.line([x, y, x - vx * 0.03, y - vy * 0.03], fill=c, width=1)
                d.rectangle([x - 1, y - 1, x, y], fill=c)
                continue
            rad = r
            if kind == "mist":
                rad = min(RADII, key=lambda q: abs(q - r * (1 + 1.4 * s)))
            lv = int(np.clip(fade * 4 - 0.5, 0, 3))
            if fade < 0.08:
                continue
            sprite.paste_center(img, BLOBS[(rad, v)][lv], x, y)


# =========================================================================== rendering helpers

_tint = {}
_shadow = {}


def arm_sprite(size, heading, tip=False):
    n = int(np.clip(round(size / 2) * 2, 10, 60))
    if tip:
        return b6.k_tip[34 if n > 24 else 22].get(heading)
    return b6.k_arm[n].get(heading)


def tinted(sp, depth):
    """Sprite seen through ``depth`` of water: tinted toward the sea, darker, fainter."""
    dq = round(min(depth, 1.4) * 20) / 20
    key = (id(sp), dq)
    if key not in _tint:
        a = np.asarray(sp).astype(np.float32)
        k = 0.46 + 0.38 * min(dq, 1.0) ** 0.7
        a[..., :3] = a[..., :3] * (1 - k) * (1 - 0.3 * min(dq, 1)) + SUBTINT * k
        a[..., 3] *= 1 - 0.45 * min(dq / 1.2, 1.0)
        _tint[key] = (Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA"), sp)
    return _tint[key][0]


def shadow(sp, blur=2.0, opacity=0.5):
    key = (id(sp), blur, opacity)
    if key not in _shadow:
        _shadow[key] = (sprite.shadow_of(sp, opacity=opacity, blur=blur), sp)
    return _shadow[key][0]


BINS = [(0.0, 0.22, 0.9, 1.2), (0.22, 0.5, 1.0, 1.6), (0.5, 0.85, 1.6, 2.2), (0.85, 9.0, 2.4, 2.8)]


def bin_of(depth):
    for i, (a, b, _, _) in enumerate(BINS):
        if a <= depth < b:
            return i
    return len(BINS) - 1


def head_split(L, eyes, beak, t):
    """Split the head sprite at water level L into (above RGBA, under RGBA per bin,
    contour pixel coords in screen space, above mask)."""
    spr = head_sprite(eyes, beak)
    z = head_height()
    alpha = spr[..., 3] > 8
    valid = alpha & ~np.isnan(z)
    zz = np.where(np.isnan(z), -9, z)
    depth = L - zz
    above = valid & (depth <= 0)
    under = alpha & ~above
    up = spr.copy()
    up[..., 3] = np.where(above, spr[..., 3], 0)
    # wet sheen and rivulets on parts that surfaced recently
    if above.any():
        age = t_since_surfaced(zz, t)
        sheen = np.where(above, np.exp(-np.clip(age, 0, None) / 0.9) * (age >= 0), 0)
        if sheen.max() > 0.01:
            hh, ww = zz.shape
            yy, xx = np.mgrid[0:hh, 0:ww]
            cy, cx = (CROWN[1] - (HY - H_H / 2)), H_W / 2
            th = np.arctan2(yy - cy, xx - cx)
            rr = np.hypot(yy - cy, xx - cx)
            wob = 3.0 * np.sin(rr * 0.09 + 1.7 * np.sin(th * 5)) + 2.0 * np.sin(rr * 0.23 + th * 3)
            riv = np.clip(np.sin(th * 23 + wob), 0, 1) ** 8 * (0.5 + 0.5 * np.sin(th * 7 + 1.3))
            flow = 0.6 + 0.4 * np.sin(rr * 0.18 - 8 * t)
            k = np.clip(sheen * (0.14 + 0.24 * riv * flow), 0, 0.4)[..., None]
            up[..., :3] = up[..., :3] * (1 - k) + np.array([175, 215, 242]) * k
    dd = np.clip(depth, 0, 9)
    k = (0.46 + 0.38 * np.clip(dd, 0, 1) ** 0.7)[..., None]
    un = spr.copy()
    un[..., :3] = un[..., :3] * (1 - k) * (1 - 0.3 * np.clip(dd, 0, 1))[..., None] + SUBTINT * k
    un[..., 3] = np.where(under, spr[..., 3] * (1 - 0.45 * np.clip(dd / 1.2, 0, 1)), 0)
    parts = []
    for a, b, _, _ in BINS:
        m = under & (dd >= a) & (dd < b)
        if m.any():
            u = un.copy()
            u[..., 3] = np.where(m, un[..., 3], 0)
            parts.append(Image.fromarray(np.clip(u, 0, 255).astype(np.uint8), "RGBA"))
        else:
            parts.append(None)
    cont = valid & (np.abs(depth) < 0.035)
    cy, cx = np.nonzero(cont)
    ox, oy = HX - H_W / 2, HY - H_H / 2
    return (Image.fromarray(np.clip(up, 0, 255).astype(np.uint8), "RGBA"), parts,
            (cx + ox, cy + oy), above)


_rise_t = np.linspace(2.4, 4.8, 400)
_rise_L = np.array([head_level(x) for x in _rise_t])


def t_since_surfaced(z, t):
    """Seconds since water level dropped below height z during the surfacing (negative or
    huge when not applicable)."""
    tm = t % T
    if not (3.2 <= tm <= 8.0):
        return np.full(z.shape, 99.0)
    tc = np.interp(-z, -_rise_L, _rise_t)        # _rise_L decreasing -> -_rise_L increasing
    return tm - tc


def displace(arr, gx, gy, gain):
    yi = np.clip(np.rint(YY + gy * gain).astype(np.int32), 0, FH - 1)
    xi = np.clip(np.rint(XX + gx * gain).astype(np.int32), 0, FW - 1)
    return arr[yi, xi]


# =========================================================================== simulation

class Sim:
    """Event-driven state (ripple sources and particles) advanced frame by frame; rendering is
    separate so a warm-up cycle makes the loop seamless."""

    def __init__(self):
        self.rip = Ripples()
        self.par = Particles()
        self.last = {}
        self.rng = np.random.default_rng(5)
        self.shots = b6.Shots()
        self.impacts = set()

    def step(self, t):
        rng, rip, par = self.rng, self.rip, self.par
        tm = t % T
        poses = arm_poses(t)
        # waterline crossings: foam collars, ripple trains / wakes
        for ap in poses:
            pts, es = ap.pts + [ap.tip], ap.es + [ap.tip_e]
            for j in range(len(pts) - 1):
                e0, e1 = es[j], es[j + 1]
                if e0 * e1 < 0 or (abs(e0) < 0.06 and ap.key.startswith("slam")):
                    f = e0 / (e0 - e1) if e0 * e1 < 0 else 0.5
                    x = pts[j][0] + (pts[j + 1][0] - pts[j][0]) * f
                    y = pts[j][1] + (pts[j + 1][1] - pts[j][1]) * f
                    h = ap.heads[min(j, len(ap.heads) - 1)]
                    size = ap.sizes[min(j, len(ap.sizes) - 1)]
                    crossing = e0 * e1 < 0
                    key = (ap.key, j // 3, crossing)
                    lx, ly, lt = self.last.get(key, (x, y, t - 1))
                    speed = np.hypot(x - lx, y - ly) / max(t - lt, 1e-3) if t - lt < 0.2 else 0
                    n = (3 + int(speed / 25)) if crossing else (1 if rng.random() < 0.18 else 0)
                    for _ in range(n):
                        side = rng.choice((-1, 1))
                        off = side * size * (0.25 + 0.3 * rng.random())
                        a = h + np.pi / 2
                        vx = np.cos(a) * side * rng.uniform(6, 20)
                        vy = np.sin(a) * side * rng.uniform(6, 20)
                        par.add(x + np.cos(a) * off + rng.normal(0, 2),
                                y + np.sin(a) * off + rng.normal(0, 2), vx, vy,
                                life=rng.uniform(0.6, 1.3), r=rng.choice((1.2, 1.8, 2.6, 3.6)),
                                alpha=rng.uniform(0.55, 0.95))
                    period = 0.12 if speed > 40 else 0.3
                    if crossing and t - self.last.get(key + ("rip",), (0, 0, -9))[2] > period:
                        rip.add(t, x, y, amp=0.45 + min(speed, 120) / 200, lam=9 + size * 0.12,
                                speed=32, tau=0.9, life=1.8)
                        self.last[key + ("rip",)] = (x, y, t)
                    self.last[key] = (x, y, t)
        # slams: churning telegraph, splash on impact
        for start, lane, side in SLAMS:
            r = slam_state(t, start)
            if r is None:
                continue
            ap = [p for p in poses if p.key == f"slam-{side}"][0]
            if r < TEL + 0.2:
                for _ in range(10):
                    yv = rng.uniform(410, 560)
                    xv = lane + rng.normal(0, 7)
                    par.add(xv, yv, rng.normal(0, 6), rng.normal(0, 6), life=rng.uniform(0.3, 0.6),
                            r=rng.choice((1.2, 1.8, 2.6)), alpha=rng.uniform(0.4, 0.8))
                if rng.random() < 0.85:
                    rip.add(t, lane + rng.normal(0, 6), rng.uniform(410, 560), amp=0.75, lam=9,
                            speed=30, tau=0.6, life=1.2)
            key = (start, int(t // T))
            if r >= IMPACT and key not in self.impacts:
                self.impacts.add(key)
                for j, (x, y) in enumerate(ap.pts):
                    if ap.es[j] > -0.1 and j % 2 == 0:
                        rip.add(t, x, y, amp=1.5, lam=13, speed=58, tau=1.1, life=2.6)
                    if ap.es[j] > -0.1:
                        a = ap.heads[j] + np.pi / 2
                        for _ in range(9):
                            side = rng.choice((-1, 1))
                            sp = rng.uniform(70, 170)
                            par.add(x, y, np.cos(a) * side * sp + rng.normal(0, 25),
                                    np.sin(a) * side * sp + rng.normal(0, 25), life=rng.uniform(0.3, 0.6),
                                    kind="spray", drag=3.5)
                        for _ in range(5):
                            side = rng.choice((-1, 1))
                            sp = rng.uniform(20, 60)
                            par.add(x + rng.normal(0, 4), y + rng.normal(0, 4), np.cos(a) * side * sp,
                                    np.sin(a) * side * sp, life=rng.uniform(0.9, 1.6),
                                    r=rng.choice((1.8, 2.6, 3.6)), alpha=rng.uniform(0.5, 0.85))
                        if j % 2 == 0:
                            par.add(x, y, rng.normal(0, 8), rng.normal(0, 8), life=0.9, r=10.0,
                                    alpha=0.45, kind="mist", drag=2.0)
        # head: swell ripples, foam collar on the contour, water streaming off / pulled in
        L = head_level(t)
        dL = head_dlevel(t)
        z = head_height()
        valid = ~np.isnan(z)
        zz = np.where(valid, z, -9)
        cont = valid & (np.abs(L - zz) < 0.035) & (head_sprite(0.0, 0.0)[..., 3] > 8)
        cy, cx = np.nonzero(cont)
        if cx.size:
            moving = abs(dL) > 0.05
            n = 26 if moving else 9
            idx = rng.integers(0, cx.size, n)
            for i in idx:
                x, y = cx[i] + HX - H_W / 2, cy[i] + HY - H_H / 2
                a = np.arctan2(y - (HY + 10), x - HX)
                sp = rng.uniform(8, 22) * (1 if dL <= 0 else -0.8)
                par.add(x, y, np.cos(a) * sp, np.sin(a) * sp, life=rng.uniform(0.7, 1.5),
                        r=rng.choice((2.6, 3.6, 5.0)), alpha=rng.uniform(0.7, 1.0))
            if rng.random() < (0.35 if moving else 0.08):
                i = rng.integers(0, cx.size)
                rip.add(t, cx[i] + HX - H_W / 2, cy[i] + HY - H_H / 2, amp=0.7 if moving else 0.35,
                        lam=12, speed=36, tau=1.0, life=2.0)
        if 0.56 < L < 1.0 and dL < 0 and rng.random() < 0.25:
            rip.add(t, CROWN[0] + rng.normal(0, 18), CROWN[1] + 10 + rng.normal(0, 22), amp=0.5,
                    lam=14, speed=30, tau=1.0, life=2.0)
        # platform edges: lapping foam
        for _ in range(2):
            e = rng.integers(0, 3)
            if e == 0:
                x, y = rng.uniform(P_W, P_E), P_S + 2
            elif e == 1:
                x, y = P_W - 2, rng.uniform(P_N, P_S)
            else:
                x, y = P_E + 2, rng.uniform(P_N, P_S)
            par.add(x, y, rng.normal(0, 4), rng.normal(0, 4), life=rng.uniform(0.6, 1.2),
                    r=rng.choice((1.2, 1.8, 2.6)), alpha=rng.uniform(0.4, 0.8))
        # beak fans
        for tf in FANS:
            if abs(tm - tf) < DT / 2:
                px, py = player_pos(t)
                base = b6.heading_to(*BEAK, px, py)
                for a in np.linspace(-0.6, 0.6, 7):
                    self.shots.fire(t, BEAK[0], BEAK[1] + 6, base + a, 160)
        self.shots.list = [s for s in self.shots.list if t - s[0] < 4.0]
        self.par.step(DT)
        self.rip.prune(t)
        return poses


# =========================================================================== render

_plat = {}


def platform_sprite():
    if "p" not in _plat:
        _plat["p"] = b6.static_sprite(bm.platform, (230, 172), 2.3, factor=2, colors=40)
    return _plat["p"]


def render(sim, t, poses, telegraph=True, player=True):
    tm = t % T
    L = head_level(t)
    h = base_height(t)
    sim.rip.apply(h, t)
    swell(h, L)
    light, glint, gx, gy = shade(h)
    rgb = np.empty((FH, FW, 3), np.float32)
    rgb[:] = DEEP
    rgb *= (0.92 + 0.08 * np.clip(h, -1.5, 1.5)[..., None] / 1.5)
    # ---- submerged layers (deep first), drawn through the water
    layers = [Image.new("RGBA", (FW, FH), (0, 0, 0, 0)) for _ in BINS]
    eyes = 1.0 if EYES_OPEN[0] <= tm < EYES_OPEN[1] else 0.0
    beak = 1.0 if any(tf - 0.35 <= tm < tf + 0.08 for tf in FANS) else 0.0
    bob = 2.0 * np.sin(W2 * 6 * t) if L < 0.3 else 0.0
    up, parts, _, _ = head_split(L, eyes, beak, t)
    hx0, hy0 = int(round(HX - H_W / 2)), int(round(HY - H_H / 2 + bob))
    for i, p in enumerate(parts):
        if p is not None:
            layers[i].alpha_composite(p, (hx0, hy0))
    above = []
    for ap in poses:
        items = [(ap.pts[j], ap.heads[j], ap.sizes[j], ap.es[j], False) for j in range(len(ap.pts))]
        items.append((ap.tip, ap.tip_h, ap.sizes[-1] if ap.sizes else 16, ap.tip_e, True))
        for j in range(len(items) - 1, -1, -1):       # tip first, base on top
            (x, y), hd, size, e, tip = items[j]
            if e <= 0:
                sp = arm_sprite(size, hd, tip)
                sprite.paste_center(layers[bin_of(-e)], tinted(sp, -e), x, y)
            else:
                above.append((ap.key, j, x, y, hd, size, e, tip))
    for i in range(len(BINS) - 1, -1, -1):
        _, _, blur, gain = BINS[i]
        lay = layers[i].filter(ImageFilter.GaussianBlur(blur))
        a = np.asarray(lay).astype(np.float32)
        a = displace(a, gx, gy, gain * 5.0)
        al = a[..., 3:4] / 255.0
        rgb = rgb * (1 - al) + a[..., :3] * al
    # ---- wave light and moon glints over everything in the water
    rgb += np.clip(light, 0, None)[..., None] * LIGHT
    rgb *= (1 + 0.55 * np.clip(light, -0.9, 0))[..., None]
    rgb += glint[..., None] * GLINT * 0.8
    rgb = np.round(np.clip(rgb, 0, 255) / POSTER) * POSTER     # fewer tones: GIF size
    img = raster.to_rgba_image(np.clip(rgb, 0, 255))
    # ---- foam on the surface
    sim.par.draw(img, ("foam",))
    # ---- shadows of raised parts on the water, platform, deck shadows
    # head above water (its rear passes under the platform deck)
    if np.asarray(up)[..., 3].max() > 0:
        sh = sprite.shadow_of(up, opacity=0.35, blur=2.0)
        img.alpha_composite(sh, (hx0 + 5, hy0 + 7))
        img.alpha_composite(up, (hx0, hy0))
    plat = platform_sprite()
    sprite.paste_center(img, shadow(plat, blur=3.0, opacity=0.45), PLAT[0] + 12, PLAT[1] + 16)
    deck_items = [a for a in above if a[0].startswith(("side", "south")) and a[6] > 0.2]
    water_items = [a for a in above if a not in deck_items]
    for key, j, x, y, hd, size, e, tip in water_items:
        sc = 1 + 0.35 * e
        sp = arm_sprite(size * sc, hd, tip)
        sprite.paste_center(img, shadow(sp, blur=2.0, opacity=0.42), x + 8 + 36 * e, y + 10 + 44 * e)
    sprite.paste_center(img, plat, *PLAT)
    for key, j, x, y, hd, size, e, tip in deck_items:
        sp = arm_sprite(size, hd, tip)
        sprite.paste_center(img, shadow(sp, blur=1.2, opacity=0.5), x + 4, y + 5)
    # ---- arm parts above the water (lowest first)
    for key, j, x, y, hd, size, e, tip in sorted(water_items + deck_items, key=lambda a: (a[6] > 0.2, a[0], -a[1])):
        sc = 1 + 0.35 * e if (key, j) not in [(d[0], d[1]) for d in deck_items] else 1.0
        sprite.paste_center(img, arm_sprite(size * sc, hd, tip), x, y)
    # ---- airborne spray and mist
    sim.par.draw(img, ("spray", "mist"))
    # ---- telegraph (gameplay readability), bullets, player
    if telegraph:
        for start, lane, side in SLAMS:
            r = slam_state(t, start)
            if r is not None and r < TEL + RISE + HOLD and (telegraph == "force" or int(t * 10) % 2 == 0):
                e3.dashed(img, lane, 400, lane, FH, (255, 60, 40), dash=7)
    sim.shots.draw(img, t)
    if player:
        b6.player_draw(img, *player_pos(t), t, "orbit")
    return img


# =========================================================================== build

def run():
    sim = Sim()
    # warm-up cycle so foam and ripples carry over the loop seam
    for i in range(NF):
        sim.step(-T + i * DT)
    frames, keep = [], {}
    wanted = {"setup": 2.9, "game": 5.95, "deck": 1.0, "phase1": 1.85}
    for q, Lq in enumerate((0.9, 0.6, 0.5, 0.36, 0.2, 0.04)):
        wanted[f"rise{q}"] = float(np.interp(-Lq, -_rise_L, _rise_t))
    dive_t = np.linspace(6.8, 8.8, 400)
    dive_L = np.array([head_level(x) for x in dive_t])
    for q, Lq in enumerate((0.15, 0.42, 0.66)):
        wanted[f"dive{q}"] = float(np.interp(Lq, dive_L, dive_t))
    for q, rr in enumerate((0.45, TEL + 0.3, TEL + RISE + 0.2, IMPACT + 0.04, IMPACT + 0.3, IMPACT + LIE + 0.35)):
        wanted[f"slam{q}"] = SLAMS[0][0] + rr
    wanted_f = {k: int(round(v * FPS)) for k, v in wanted.items()}
    for i in range(NF):
        t = i * DT
        poses = sim.step(t)
        f = render(sim, t, poses)
        frames.append(f)
        for k, fi in wanted_f.items():
            if fi == i:
                keep[k] = (render(sim, t, poses, telegraph="force" if k.startswith("slam") else False, player=False), f)
        if i % 20 == 0:
            print(f"  frame {i}/{NF}", flush=True)
    return frames, keep


def crop(img, box, scale=1.0):
    c = img.crop(box)
    if scale != 1.0:
        c = c.resize((int(c.width * scale), int(c.height * scale)), Image.LANCZOS)
    return c


def strip(images, labels, pad=8):
    w = sum(i.width for i in images) + pad * (len(images) - 1)
    h = max(i.height for i in images) + 14
    p = Image.new("RGBA", (w, h), raster.SHEET_BG)
    x = 0
    for im, lab in zip(images, labels):
        raster.draw_text(p, x, 0, lab, raster.LABEL)
        p.alpha_composite(im.convert("RGBA"), (x, 14))
        x += im.width + pad
    return p


def build():
    frames, keep = run()
    setup = keep["setup"][0]
    full = crop(setup, (20, 0, 460, 540))
    ox = -20
    callouts = [
        ((int(PLAT[0] + ox - 40), int(PLAT[1] - 40)), "UTC HARBOUR PLATFORM (DECK ABOVE THE WATER, FOAM LAPPING AT ITS EDGES)"),
        ((int(P_W + ox - 2), 128), "ARM CLIMBS OUT OF THE WATER OVER THE EDGE: IRREGULAR FOAM COLLAR AND RIPPLE TRAIN"),
        ((int(104 + ox), 230), "THE SAME ARM CONTINUES UNDER WATER TO THE HEAD (TINTED, BLURRED, REFRACTED)"),
        ((int(CROWN[0] + ox), int(CROWN[1] + 6)), "MANTLE CROWN: HIGHEST POINT, BREAKS THE SURFACE FIRST (SWELL ABOVE IT)"),
        ((int(HX + ox + 40), int(HY + 0.575 * HS)), "LIME EYES = WEAK POINTS, OPEN ONLY WHILE SURFACED"),
        ((int(HX + ox), int(BEAK[1])), "BEAK: CRIMSON GLOW BEFORE EACH 7-ORB FAN"),
        ((int(150 + ox), 470), "SLAM ARM SINKING AFTER A SLAM: DEEPER = DARKER, BLURRIER, FAINTER"),
        ((int(80 + ox), 505), "IDLE ARMS TRAIL DEEP BELOW, SQUIRMING"),
    ]
    # surfacing / diving sequences (head crops)
    hb = (110, 170, 370, 420)
    rise = strip([crop(keep[f"rise{q}"][0], hb, 0.62) for q in range(6)],
                 ["SWELL", "CROWN NEAR", "CROWN BREAKS", "HEAD", "EYES", "UP, EYES OPEN"])
    dive = strip([crop(keep[f"dive{q}"][0], hb, 0.62) for q in range(3)],
                 ["LOW PARTS GO FIRST", "HEAD SINKS", "CROWN LAST"])
    # arm entering the water (2x)
    d1 = crop(keep["deck"][0], (80, 90, 190, 170), 2.0)
    d2 = crop(keep["deck"][0], (127, 146, 237, 226), 2.0)
    detail = strip([d1, d2], ["SIDE ARM OVER THE WEST EDGE", "SOUTH ARM ONTO THE DECK"])
    sb = (60, 330, 300, 540)
    slam = strip([crop(keep[f"slam{q}"][0], sb, 0.6) for q in range(6)],
                 ["CHURN+TELEGRAPH", "RISES BASE->TIP", "RAISED", "SLAM: SPLASH", "AWASH", "SINKS"])
    ph = strip([crop(keep["phase1"][1], (0, 0, 480, 540), 0.5), crop(keep["game"][1], (0, 0, 480, 540), 0.5)],
               ["PHASE 1: SLAMS, HEAD BELOW", "PHASE 2: HEAD UP, FANS"])
    panels = [("SURFACING: THE HIGHEST PARTS BREAK THE SURFACE FIRST (0.62X)", rise),
              ("DIVING REVERSES IT (0.62X)", dive),
              ("ARM ENTERING THE WATER (2X)", detail),
              ("SLAM SEQUENCE (0.6X)", slam),
              ("PHASES (0.5X)", ph)]
    notes = ["HARBOUR KRAKEN R07 - ACT 2 MID-BOSS, L11 ATLANTIC CONVOY (RUST SKIN, CRIMSON = SLAMS, LIME WEAK POINTS)",
             "ONE CONNECTED ANIMAL: EVERY ARM RUNS UNDER WATER TO THE HEAD; SUBMERGED PARTS ARE DRAWN THROUGH THE WATER IN FOUR",
             "DEPTH BANDS (TINT, DARKEN, BLUR, REFRACTION) SO THEY FADE WITH DEPTH. THE WATER IS A HEIGHT FIELD: SWELL BANDS,",
             "RIPPLE TRAINS, WAKES, THE SWELL AND SPLASHES ARE ADDED TO IT AND LIT LIKE WAVES (NO DRAWN CIRCLES); FOAM AND SPRAY",
             "ARE PARTICLES AT EVERY WATERLINE. THE HEAD CARRIES A HEIGHT MAP: WATER LEVEL DROPS -> CROWN, THEN EYES, THEN THE",
             "REST SURFACE, WATER STREAMS OFF; DIVING REVERSES IT. SLAM: LANE CHURNS AND FLASHES, ARM RISES BASE TO TIP, SLAMS,",
             "LIES AWASH, SINKS. PHASE 3 IDEA (UNCHANGED): TWO LANES AT ONCE WHILE THE HEAD STAYS UP."]
    b6.SUB = SUB
    sheet = b6.boss_sheet("HARBOUR KRAKEN - FROM THE DEPTHS (R07)", full, callouts, panels,
                          keep["game"][1], notes, "SET-UP 1X: HEAD RISING, SLAM ARM SINKING, ALL ARMS CONNECTED UNDER WATER")
    OUT.mkdir(parents=True, exist_ok=True)
    png = OUT / "harbour-kraken-r07-a.png"
    sheet.convert("RGB").save(png, optimize=True)
    size = b6.rig.write_gif(frames, OUT / "harbour-kraken-r07-a.gif", fps=FPS)
    print(f"wrote {png.relative_to(ROOT)} and .gif ({size / 1e6:.1f} MB, {len(frames)} frames)")


if __name__ == "__main__":
    build()
