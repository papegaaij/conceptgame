#!/usr/bin/env python3
"""Concept round 08 - combat effects for Acts 1-2 at 960x540, palette B.

Outputs (implemented):
  design/player/ship/concept/player-ship-r08-a.png       Stormhawk: 5 banking frames + wing pods
  design/player/weapons/concept/projectiles-r08-a.png    player projectile families, muzzle
                                                         flashes, impacts, L1/L3/L5 patterns
  design/player/weapons/concept/projectiles-r08-a.gif    the families fired in sequence

Also contains (not yet wired to a sheet): Rook banking sheet, explosion / water / hit-flash /
shield generators. Planned but not done: enemy bullets, explosions, pickups, specials and
edge-warning sheets.

Pre-rendered objects (ship, pods, missiles, bombs, torpedoes, mines) are SDF ray-marched like
every other sprite. Energy effects (shots, flashes, explosions) are drawn as 2D fields at 4x
supersampling and box-filtered down, so they keep soft additive edges. Deterministic.

Run: python3 tools/concept/vfx_r08.py [ship] [projectiles]   (no args = both, ~5 min)
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, str(Path(__file__).resolve().parent))
from render import models, raster, sdf, sprite, terrain  # noqa: E402
from render.config import ROOT, SHIP_SIZE, WINGMAN_SIZE  # noqa: E402
from render.enemy_rigs import write_gif  # noqa: E402
from render.palette import B  # noqa: E402
from render.sdf import (Material, mirror_x, rotate_x, rotate_y, rotate_z, sd_box,  # noqa: E402
                        sd_capsule, sd_cylinder_y, sd_cylinder_z, sd_ellipsoid, sd_fin,
                        sd_plate, sd_sphere, union)

import ships_r02  # noqa: E402

D = ROOT / "design"
OUT = {
    "ship": D / "player" / "ship" / "concept",
    "wing": D / "player" / "wingmen" / "concept",
    "weapons": D / "player" / "weapons" / "concept",
    "enemies": D / "enemies" / "concept",
    "art": D / "art-direction" / "concept",
    "player": D / "player" / "concept",
    "specials": D / "player" / "specials" / "concept",
    "hud": D / "ui" / "hud" / "concept",
}
SUB = "CONCEPT ROUND 08 - 960X540"
FW, FH = 480, 540
FPS = 20
TAU = 2 * np.pi
SS = 4                                   # supersampling for 2D effect fields

PS = B["PLAYER SHOTS"]    # 002060 0060ff 00c0ff a0ffff ffffff 40ff80
ES = B["ENEMY SHOTS"]     # 300030 c000c0 ff40ff ffc0ff ffffff ffff40
GOLD = B["ASCEND. GOLD"]  # 402000 804000 c07000 ffa800 ffd84a ffffa0
VG = B["VRELL GLOW"]      # 003a20 00a060 00ff9a 600080 c000ff f0a0ff
UI = B["UI TEXT"]         # 00ffff 00ff66 ffff00 ff00aa ff4400 ffffff
WHITE = (255, 255, 255)
LIME = (168, 255, 42)
ORANGE = (255, 122, 26)
FRIEND = (64, 255, 128)   # friendly green (pickups only)
LABEL, DIM = raster.LABEL, raster.LABEL_DIM


def rnd(seed):
    return np.random.default_rng(seed)


# =========================================================================== 2D effect fields

class Canvas:
    """Premultiplied float RGBA canvas at SS x supersampling, in native-pixel coordinates."""

    def __init__(self, w, h, ss=SS):
        self.w, self.h, self.ss = int(w), int(h), ss
        ys, xs = np.mgrid[0:self.h * ss, 0:self.w * ss]
        self.x = (xs + 0.5) / ss
        self.y = (ys + 0.5) / ss
        self.rgb = np.zeros(self.x.shape + (3,))
        self.a = np.zeros(self.x.shape)

    @staticmethod
    def _col(c):
        c = np.asarray(c, dtype=np.float64)
        return c / 255.0 if c.max() > 1.0 else c

    def over(self, color, cover):
        cover = np.clip(cover, 0, 1)
        c = self._col(color)
        self.rgb = self.rgb * (1 - cover[..., None]) + c * cover[..., None]
        self.a = self.a * (1 - cover) + cover

    def add(self, color, amount):
        """Additive light; alpha grows with brightness so it also reads on dark backgrounds."""
        amount = np.clip(amount, 0, None)
        c = self._col(color)
        self.rgb = self.rgb + c * amount[..., None]
        self.a = np.clip(self.a + amount * c.max(), 0, 1)

    # ---- distances
    def dist(self, cx, cy):
        return np.hypot(self.x - cx, self.y - cy)

    def seg(self, x0, y0, x1, y1):
        px, py = self.x - x0, self.y - y0
        bx, by = x1 - x0, y1 - y0
        h = np.clip((px * bx + py * by) / max(bx * bx + by * by, 1e-9), 0, 1)
        return np.hypot(px - bx * h, py - by * h)

    def ell(self, cx, cy, rx, ry, ang=0.0):
        c, s = np.cos(ang), np.sin(ang)
        dx, dy = self.x - cx, self.y - cy
        u, v = c * dx + s * dy, -s * dx + c * dy
        return np.sqrt((u / rx) ** 2 + (v / ry) ** 2)          # 1.0 on the outline

    def poly(self, pts):
        d = sdf.sd_polygon2(self.x.ravel(), self.y.ravel(), pts)
        return d.reshape(self.x.shape)

    def image(self, hard=None):
        ss = self.ss
        h, w = self.h, self.w
        a = self.a.reshape(h, ss, w, ss).mean(axis=(1, 3))
        prem = self.rgb.reshape(h, ss, w, ss, 3).mean(axis=(1, 3))
        rgb = prem / np.maximum(a, 1e-6)[..., None]
        if hard is not None:
            a = (a >= hard).astype(np.float64)
        arr = np.dstack([np.clip(rgb, 0, 1), np.clip(a, 0, 1)])
        return Image.fromarray((arr * 255 + 0.5).astype(np.uint8), "RGBA")


def gauss(d, w):
    return np.exp(-(d / np.maximum(w, 1e-6)) ** 2)


def solid(d, r):
    """Hard coverage of distance field d inside radius r (antialiased by supersampling)."""
    return (d <= r).astype(np.float64)


def alpha_mul(img, k):
    out = img.copy()
    out.putalpha(img.getchannel("A").point(lambda v: int(v * k)))
    return out


def additive(dst, src, x, y):
    """Add src's (alpha-weighted) colour onto dst at centre (x, y): a light blend."""
    sx, sy = int(round(x - src.width / 2)), int(round(y - src.height / 2))
    x0, y0 = max(0, sx), max(0, sy)
    x1, y1 = min(dst.width, sx + src.width), min(dst.height, sy + src.height)
    if x1 <= x0 or y1 <= y0:
        return
    d = np.array(dst).astype(np.float64)
    s = np.array(src).astype(np.float64)[y0 - sy:y1 - sy, x0 - sx:x1 - sx]
    k = s[..., 3:4] / 255.0
    d[y0:y1, x0:x1, :3] += s[..., :3] * k
    if d.shape[2] == 4:
        d[y0:y1, x0:x1, 3] = np.maximum(d[y0:y1, x0:x1, 3], s[..., 3])
    dst.paste(Image.fromarray(np.clip(d, 0, 255).astype(np.uint8), dst.mode))


def put(dst, src, x, y):
    sprite.paste_center(dst, src, x, y)


def label(img, x, y, text, color=DIM, scale=1):
    raster.draw_text(img, x, y, text, color, scale=scale)


def checker(w, h, cell=8):
    return ships_r02.checker(w, h, cell)


def on_checker(sp, scale=1, pad=0):
    big = sprite.enlarge(sp, scale) if scale > 1 else sp
    c = checker(big.width + 2 * pad, big.height + 2 * pad, cell=max(4, 2 * scale))
    c.alpha_composite(big, (pad, pad))
    return c


# =========================================================================== backgrounds

def orbit_bg(w=FW, h=FH, seed=11, darken=0.72):
    return terrain.recede(terrain.earth_from_orbit(w, h, seed=seed, period=False),
                          amount=0.2, darken=darken)


def city_bg(w=FW, h=FH, seed=5):
    return terrain.recede(terrain.city_ground(w, h, seed=seed, period=False),
                          amount=0.2, darken=0.8)


def space_bg(w=FW, h=FH, seed=3):
    return raster.starfield(w, h, seed, density=0.003)


SCENES = [("EARTH ORBIT", "parallax-r03-a.gif"), ("MEGACITY", "parallax-r03-b.gif"),
          ("MARS CANYON", "parallax-r03-c.gif"), ("LUNA", "scene-luna-r06-a.gif"),
          ("EUROPA", "scene-europa-r07-a.gif"), ("BELT", "scene-belt-r06-a.gif"),
          ("JUPITER", "scene-jovian-r06-a.gif"), ("VRELL SPACE", "scene-vrell-space-r06-a.gif")]


def scene_frame(gif, frame=0):
    im = Image.open(OUT["art"] / gif)
    im.seek(frame)
    return im.convert("RGBA").copy()


# =========================================================================== SDF objects

F8 = 8


def render_obj(scene, mats, native, extent, colors=24, threshold=0.5):
    hi = sdf.render(scene, mats, (native * F8, native * F8), extent)
    return sprite.make_sprite(hi, F8, colors, threshold=threshold)


def mat(albedo, **kw):
    return Material(tuple(c / 255 for c in albedo) if max(albedo) > 1 else albedo, **kw)


HULL_C = B["UTC HULL"]
ACC_C = B["UTC ACCENTS"]


def m_metal(c):
    return mat(c, metal=0.45, shininess=60, spec=0.6)


def m_glow(c, k=1.6):
    cc = np.array(c) / 255
    return Material((0.08, 0.08, 0.1), emission=tuple(cc * k))


def oriented(fn):
    """Wrap a model so it is evaluated after a heading (about Z) and tumble (about X/Y)."""
    def build(heading=0.0, tilt=0.0, spin=0.0, **kw):
        scene, mats = fn(**kw)

        def s(p):
            q = rotate_z(p, heading)
            if tilt:
                q = rotate_x(q, tilt)
            if spin:
                q = rotate_y(q, spin)
            return scene(q)
        return s, mats
    return build


@oriented
def missile_model(size=1.0, body=HULL_C[4]):
    """Hornet missile: white body, blue band, cruciform fins, glowing motor."""
    mats = [m_metal(body), m_metal(ACC_C[1]), m_metal(HULL_C[1]), m_glow(PS[3], 2.2)]

    def scene(p):
        q = mirror_x(p)
        d, m = union(
            (sd_capsule(p, (0, -0.62, 0), (0, 0.7, 0), 0.24, 0.12), 0),
            (sd_plate(q, [(0.1, -0.3), (0.5, -0.66), (0.5, -0.86), (0.1, -0.78)], 0.0,
                      0.04, 0.015), 2),
            (sd_cylinder_y(p, (0, -0.78, 0), 0.17, 0.05), 3),
            k=0.03)
        m = np.where((np.abs(p[:, 1] - 0.25) < 0.08) & (m == 0), 1, m)
        m = np.where((p[:, 1] > 0.62) & (m == 0), 2, m)
        return d, m
    return scene, mats


@oriented
def bomb_model():
    """Bomb Rack bomb: dark finned body, blue nose band."""
    mats = [m_metal(HULL_C[3]), m_metal(ACC_C[1]), m_metal(HULL_C[1])]

    def scene(p):
        q = mirror_x(p)
        d, m = union(
            (sd_ellipsoid(p, (0, 0.05, 0), (0.42, 0.82, 0.42)), 0),
            (sd_plate(q, [(0.0, -0.45), (0.42, -0.72), (0.42, -0.95), (0.0, -0.85)], 0.0,
                      0.03, 0.01), 2),
            (sd_fin(q, [(-0.45, 0.0), (-0.72, 0.42), (-0.95, 0.42), (-0.85, 0.0)], 0.0, 0.03,
                    0.01), 2),
            k=0.03)
        m = np.where((np.abs(p[:, 1] - 0.5) < 0.08) & (m == 0), 1, m)
        return d, m
    return scene, mats


@oriented
def torpedo_model():
    """Torpedo: long grey body, white band, prop shroud."""
    mats = [m_metal(HULL_C[3]), m_metal(HULL_C[5]), m_metal(HULL_C[1]), m_glow(PS[2], 1.4)]

    def scene(p):
        d, m = union(
            (sd_capsule(p, (0, -0.78, 0), (0, 0.82, 0), 0.27, 0.2), 0),
            (sd_cylinder_y(p, (0, -0.9, 0), 0.32, 0.06), 2),
            (sd_cylinder_y(p, (0, -0.98, 0), 0.12, 0.03), 3),
            k=0.02)
        m = np.where((np.abs(p[:, 1] - 0.3) < 0.07) & (m == 0), 1, m)
        return d, m
    return scene, mats


@oriented
def mine_model(lit=1.0):
    """Proximity mine: squat dark disc with a ring of studs and a blue sensor light."""
    mats = [m_metal(HULL_C[1]), m_metal(HULL_C[3]), m_glow(PS[2], 0.4 + 2.2 * lit)]

    def scene(p):
        ang = np.arctan2(p[:, 1], p[:, 0])
        k = np.round(ang / (TAU / 8)) * (TAU / 8)
        sx, sy = 0.62 * np.cos(k), 0.62 * np.sin(k)
        stud = np.sqrt((p[:, 0] - sx) ** 2 + (p[:, 1] - sy) ** 2 + (p[:, 2] - 0.12) ** 2) - 0.13
        d, m = union(
            (sd_cylinder_z(p, (0, 0, 0), 0.7, 0.14), 0),
            (stud, 1),
            (sd_sphere(p, (0, 0, 0.14), 0.18), 2),
            k=0.04)
        return d, m
    return scene, mats


@oriented
def shell_model():
    """Hammer Mortar shell: blunt dark round with a glowing plasma band."""
    mats = [m_metal(HULL_C[3]), m_glow(PS[2], 1.8)]

    def scene(p):
        d, m = (sd_ellipsoid(p, (0, 0, 0), (0.55, 0.72, 0.55)), 0)
        m = np.where(np.abs(p[:, 1] - 0.1) < 0.14, 1, 0)
        return d, m
    return scene, mats


# ---- player wing pods (local coordinates on the right wing, mirrored)

POD_AT = np.array([0.66, -0.10, 0.085])


def _pod_scene(kind):
    G, BODY, TIP, ACC = 0, 1, 2, 3

    def scene(q):
        p = q - POD_AT
        if kind == "autocannon":
            e = [(sd_capsule(p, (0, -0.3, 0), (0, 0.12, 0), 0.09, 0.075), BODY),
                 (sd_capsule(mirror_x(p), (0.032, 0.1, 0.0), (0.032, 0.42, 0.0), 0.022), G)]
        elif kind == "micro-missile":
            e = [(sd_box(p, (0, -0.08, 0), (0.11, 0.26, 0.06), 0.025), BODY)]
            for ox in (-0.05, 0.05):
                for oy in (0.16, 0.02):
                    e.append((sd_capsule(p, (ox, oy, 0.045), (ox, oy + 0.14, 0.045), 0.028,
                                         0.01), TIP))
        elif kind == "bomb-rack":
            e = [(sd_box(p, (0, -0.06, -0.01), (0.035, 0.24, 0.02), 0.01), G),
                 (sd_ellipsoid(mirror_x(p), (0.07, -0.05, 0.035), (0.06, 0.22, 0.06)), BODY)]
        elif kind == "swivel":
            a = 0.5
            tip = (0.38 * np.sin(a), 0.38 * np.cos(a), 0.05)
            e = [(sd_cylinder_z(p, (0, -0.05, 0), 0.11, 0.035), G),
                 (sd_sphere(p, (0, -0.05, 0.035), 0.095), BODY),
                 (sd_capsule(p, (0, -0.05, 0.04), (tip[0], tip[1] - 0.05, tip[2]), 0.018), G)]
        else:  # torpedo
            e = [(sd_capsule(p, (0, -0.4, 0), (0, 0.3, 0), 0.08, 0.065), BODY),
                 (sd_cylinder_y(p, (0, -0.42, 0), 0.095, 0.03), G)]
        d, m = union(*e, k=0.012)
        if kind in ("autocannon", "torpedo", "bomb-rack"):
            m = np.where((np.abs(p[:, 1] + 0.02) < 0.03) & (m == BODY), ACC, m)
        return d, m
    return scene


POD_MATS = [m_metal((40, 44, 70)), m_metal(HULL_C[1]), m_metal(HULL_C[5]), m_metal(ACC_C[2])]
PODS = ["autocannon", "micro-missile", "bomb-rack", "swivel", "torpedo"]
POD_NAMES = {"autocannon": "AUTOCANNON POD", "micro-missile": "MICRO-MISSILE POD",
             "bomb-rack": "BOMB RACK", "swivel": "SWIVEL GUN", "torpedo": "TORPEDO POD"}


def ship_model(bank=0.0, pod=None, palette=None):
    scene, mats = models.ship_a_model(bank=bank, palette=palette)
    if pod is None:
        return scene, mats
    base = len(mats)
    pod_scene = _pod_scene(pod)

    def s(p):
        d, m = scene(p)
        q = mirror_x(rotate_y(p, bank))
        d2, m2 = pod_scene(q)
        return union((d, m), (d2, m2 + base))
    return s, mats + POD_MATS


BANKS = [-28, -14, 0, 14, 28]
_SHIP_CACHE = {}


def ship_sprite(bank_deg=0, pod=None):
    key = (bank_deg, pod)
    if key not in _SHIP_CACHE:
        scene, mats = ship_model(np.radians(bank_deg), pod, B.ship_colors())
        hi = sdf.render(scene, mats, (SHIP_SIZE * F8, SHIP_SIZE * F8), 2.3)
        _SHIP_CACHE[key] = (hi, sprite.make_sprite(hi, F8, 28))
    return _SHIP_CACHE[key]


def rook_sprite(bank_deg=0):
    colours = ships_r02.ROOK_SCHEMES["a"][1]
    scene, mats = models.ship_c_model(bank=np.radians(bank_deg), palette=colours)
    hi = sdf.render(scene, mats, (WINGMAN_SIZE * F8, WINGMAN_SIZE * F8), 2.3)
    return hi, sprite.make_sprite(hi, F8, 28)


def vrell_sprite(n=40):
    scene, mats = models.vrell_dart_model(palette=B.vrell_colors())
    return render_obj(scene, mats, n, 2.3, colors=28)


def shadow(img, sp, x, y, dx=21, dy=30, opacity=0.55):
    sh = sprite.shadow_of(sp, opacity=opacity, blur=1.2, scale=0.85)
    put(img, sh, x + dx, y + dy)


# =========================================================================== 6. ship + Rook

def coast(w, h, seed=21):
    return ships_r02.coast(w, h, seed)


def ship_sheet():
    frames = [ship_sprite(b)[1] for b in BANKS]
    hi = ship_sprite(0)[0]
    pods = {k: ship_sprite(0, k)[1] for k in PODS}
    pod_bank = [ship_sprite(b, "micro-missile")[1] for b in BANKS]
    n = SHIP_SIZE
    img = raster.sheet(1300, 900, "AF-12 STORMHAWK - 5 BANKING FRAMES AND WING PODS", SUB)
    label(img, 16, 38, "SOURCE RENDER (384 PX)")
    src = checker(384, 384)
    src.alpha_composite(sprite.to_image(hi))
    img.alpha_composite(src, (16, 50))
    x0 = 420
    label(img, x0, 38, "BANKING: HARD LEFT -28, LEFT -14, CENTRE, RIGHT +14, HARD RIGHT +28 (3X)")
    for i, f in enumerate(frames):
        img.alpha_composite(on_checker(f, 3), (x0 + i * (n * 3 + 10), 50))
        label(img, x0 + i * (n * 3 + 10), 50 + n * 3 + 4, f"{BANKS[i]:+d} DEG")
    y1 = 50 + n * 3 + 22
    label(img, x0, y1, "1X STRIP (STEERING LEFT -> RIGHT)")
    strip = checker(5 * (n + 6), n + 8)
    for i, f in enumerate(frames):
        strip.alpha_composite(f, (3 + i * (n + 6), 4))
    img.alpha_composite(strip, (x0, y1 + 12))
    y2 = y1 + n + 40
    label(img, x0, y2, "FITTED WING PODS (PAIR, ON TOP OF THE WINGS AT THE MOUNT POINTS) - 3X / 1X")
    for i, k in enumerate(PODS):
        x = x0 + i * (n * 3 + 26)
        img.alpha_composite(on_checker(pods[k], 3), (x, y2 + 12))
        img.alpha_composite(on_checker(pods[k], 1), (x + n * 3 - n, y2 + 16 + n * 3))
        label(img, x, y2 + 20 + n * 3 + n, POD_NAMES[k])
    y3 = y2 + 40 + n * 4 + 12
    label(img, 16, y3, "BANKING WITH MICRO-MISSILE PODS (3X) - PODS ARE PART OF EVERY BANK FRAME")
    for i, f in enumerate(pod_bank):
        img.alpha_composite(on_checker(f, 3), (16 + i * (n * 3 + 10), y3 + 12))
    # in-game
    g = coast(220, 160, seed=24)
    rook = rook_sprite(0)[1]
    shadow(g, rook, 60, 118)
    put(g, rook, 60, 118)
    shadow(g, pods["autocannon"], 140, 104)
    put(g, pods["autocannon"], 140, 104)
    for yy in (14, 50):
        for dx in (-11, 11):
            d = ImageDraw.Draw(g)
            d.rectangle([140 + dx - 1, yy - 6, 140 + dx + 1, yy + 6], fill=PS[2] + (230,))
            d.line([140 + dx, yy - 4, 140 + dx, yy + 4], fill=PS[4] + (255,))
    for yy in (22, 60):
        for dx in (-26, 26):
            d.rectangle([140 + dx - 1, yy - 4, 140 + dx + 1, yy + 4], fill=PS[3] + (230,))
    gx = 1300 - 16 - 440
    label(img, gx, y3, "IN GAME (2X): AUTOCANNON PODS FIRING, ROOK ON ESCORT")
    img.alpha_composite(sprite.enlarge(g, 2), (gx, y3 + 12))
    notes = ["5 BANKING FRAMES PER THE USER DECISION (WAS 3). FRAMES CHANGE OVER ABOUT 6 GAME FRAMES",
             "WHEN STEERING AND RETURN TO CENTRE ON RELEASE. WING PODS ARE MODELLED ON TOP OF THE",
             "WING AT THE MOUNT POINTS (8,27)/(40,27) SO THEY SHOW FROM ABOVE; EVERY POD TYPE NEEDS",
             "ITS OWN 5-FRAME SET (SHIP + PODS RENDERED TOGETHER, LIT CONSISTENTLY)."]
    for i, line in enumerate(notes):
        label(img, 16, y3 + 26 + n * 3 + i * 12, line, LABEL)
    return img


def rook_sheet():
    data = [rook_sprite(b) for b in BANKS]
    hi = data[2][0]
    frames = [d[1] for d in data]
    n = WINGMAN_SIZE
    img = raster.sheet(1300, 520, "ROOK'S CRAFT (EMBER) - 5 BANKING FRAMES", SUB)
    label(img, 16, 38, "SOURCE RENDER (320 PX)")
    src = checker(320, 320)
    src.alpha_composite(sprite.to_image(hi))
    img.alpha_composite(src, (16, 50))
    x0 = 356
    label(img, x0, 38, "BANKING -28 / -14 / 0 / +14 / +28 DEG (4X)")
    for i, f in enumerate(frames):
        img.alpha_composite(on_checker(f, 4), (x0 + i * (n * 4 + 10), 50))
    y1 = 50 + n * 4 + 16
    label(img, x0, y1, "1X STRIP, NEXT TO THE PLAYER'S 5 FRAMES")
    strip = checker(10 * 56 + 8, 60)
    pl = [ship_sprite(b)[1] for b in BANKS]
    for i in range(5):
        strip.alpha_composite(frames[i], (4 + i * 56, 10))
        strip.alpha_composite(pl[i], (4 + (5 + i) * 56, 6))
    img.alpha_composite(strip, (x0, y1 + 12))
    g = coast(250, 200, seed=34)
    shadow(g, frames[1], 80, 120)
    put(g, frames[1], 80, 120)
    shadow(g, pl[1], 160, 100)
    put(g, pl[1], 160, 100)
    gx = 1300 - 16 - 500
    label(img, gx, y1 + 90, "IN GAME (2X): BOTH BANKING LEFT IN FORMATION")
    img.alpha_composite(sprite.enlarge(g, 2), (gx, y1 + 102))
    label(img, 16, 400, "EMBER SCHEME (CHOSEN IN ROUND 02) ON THE SHIP C AIRFRAME, 40X40. ROOK BANKS WITH",
          LABEL)
    label(img, 16, 412, "THE SAME 5 ANGLES AS THE PLAYER SO THE PAIR MOVES AS ONE IN FORMATION.", LABEL)
    return img


# =========================================================================== explosions & impacts

FIRE_RAMP = [(0.00, (40, 10, 40)), (0.12, (140, 24, 20)), (0.30, (230, 80, 20)),
             (0.55, (255, 170, 40)), (0.78, (255, 236, 150)), (1.0, (255, 255, 255))]
VRELL_RAMP = [(0.00, (30, 0, 40)), (0.15, (96, 0, 128)), (0.35, (0, 160, 96)),
              (0.60, (110, 255, 120)), (0.82, (200, 255, 190)), (1.0, (255, 255, 255))]
ASC_RAMP = [(0.00, (20, 14, 20)), (0.14, (120, 30, 10)), (0.32, (230, 90, 10)),
            (0.55, (255, 168, 0)), (0.80, (255, 240, 170)), (1.0, (255, 255, 255))]
STYLE = {"fire": FIRE_RAMP, "vrell": VRELL_RAMP, "asc": ASC_RAMP}
SMOKE = {"fire": (70, 56, 90), "vrell": (64, 20, 80), "asc": (40, 36, 44)}


def _noise_sampler(seed):
    tex = raster.fbm(256, 64, 32, seed, octaves=4)          # angle x radius, tiles in angle

    def sample(ang, rad):
        u = ((ang / TAU) % 1.0) * 256
        v = np.clip(rad, 0, 0.999) * 63
        return tex[v.astype(int), u.astype(int) % 256]
    return sample


def _cart_noise(size, seed, cell=None):
    """Cartesian fBm at canvas resolution (size*SS) for billowy detail."""
    n = size * SS
    cell = cell or max(8, n // 4)
    return raster.fbm(n, n, cell, seed, octaves=5, period=False)


def _shift(tex, dx, dy):
    return np.roll(np.roll(tex, int(dy), axis=0), int(dx), axis=1)


def explosion_frames(size, n, seed=1, style="fire", debris=True, shock=None):
    """Pre-rendered fireball sequence built from billowing puffs: white flash -> fireball ->
    cooling smoke, with sparks / chunks (fire, asc) or ichor drops / chitin shards (vrell)."""
    rng = rnd(seed)
    R = size / 2
    ramp = STYLE[style]
    tex = _cart_noise(size, seed)
    tex2 = _cart_noise(size, seed + 1, cell=max(6, size * SS // 8))
    shock = size >= 90 if shock is None else shock
    puffs = []
    for k in range(5 + size // 16):
        a = rng.uniform(0, TAU)
        r0 = rng.uniform(0.1, 0.5) * R if k else 0.0
        puffs.append((a, r0, rng.uniform(0.28, 0.46) * R, rng.uniform(0, 0.25)))
    parts = []
    for _ in range(int(5 + size * 0.22)):
        kinds = {"vrell": ["drop", "drop", "shard"], "asc": ["spark", "spark", "plate"],
                 "fire": ["spark", "chunk"]}[style]
        parts.append((rng.uniform(0, TAU), rng.uniform(0.45, 1.0), rng.choice(kinds),
                      rng.uniform(0.6, 1.4)))
    out = []
    for i in range(n):
        t = i / max(n - 1, 1)
        cv = Canvas(size, size)
        dens = np.zeros_like(cv.x)
        sdens = np.zeros_like(cv.x)
        for a, r0, rb, delay in puffs:
            tt = np.clip((t - delay * 0.4) / (1 - delay * 0.4), 0, 1)
            rad = rb * (0.4 + 0.8 * (1 - (1 - tt) ** 2.2))
            cx = R + np.cos(a) * r0 * (0.5 + 0.8 * tt)
            cy = R + np.sin(a) * r0 * (0.5 + 0.8 * tt)
            dd = cv.dist(cx, cy) / rad
            dens = np.maximum(dens, np.exp(-dd * dd * 1.4))
            sdens = np.maximum(sdens, np.exp(-(dd / 1.3) ** 2 * 1.4))
        churn = _shift(tex, t * size * 0.6, -t * size * 0.4)
        dn = dens * (0.55 + 0.9 * churn)
        sn = sdens * (0.55 + 0.9 * _shift(tex2, -t * size, t * size * 0.5))
        if t > 0.15:                                       # cooling smoke behind the fire
            st = (t - 0.15) / 0.85
            sm = np.clip((sn - 0.26) * 6, 0, 1) * (0.95 - 0.75 * st)
            shade = 0.7 + 0.6 * np.clip(_shift(tex2, -6, -6) - tex2 + 0.5, 0, 1)
            cv.over(np.clip(np.array(SMOKE[style]) / 255 * shade[..., None], 0, 1), sm)
        heat = np.clip(dn * 1.3 - 0.05 - t * 1.05, 0, 1)
        cover = np.clip((dn - 0.3) * 8, 0, 1) * np.clip(heat * 3, 0, 1)
        lit = np.clip(0.9 + 0.8 * (_shift(churn, 5, 5) - churn), 0.65, 1.25)  # top-left light
        col = raster.ramp(ramp, heat) / 255.0 * lit[..., None]
        cv.rgb = cv.rgb * (1 - cover[..., None]) + np.clip(col, 0, 1) * cover[..., None]
        cv.a = cv.a * (1 - cover) + cover
        d = cv.dist(R, R) / R
        if i == 0:
            cv.add(WHITE, gauss(d, 0.42) * 1.5)
        elif i == 1:
            cv.add((255, 240, 200), gauss(d, 0.55) * 0.6)
        if shock and 0.05 < t < 0.6:
            rs = 0.3 + 1.1 * t
            band = gauss(d - rs, 0.1) * (0.4 + 0.8 * tex2)
            cv.add((210, 225, 255), band * 0.3 * (1 - t / 0.6))
        if debris:
            for a, sp, kind, sz in parts:
                r0 = (0.15 + 0.95 * sp * (1 - (1 - t) ** 1.6)) * R
                px, py = R + np.cos(a) * r0, R + np.sin(a) * r0
                fade = np.clip(1.2 - t * 1.1, 0, 1)
                if kind == "spark":
                    ln = R * 0.2 * sz * (1 - t)
                    dd = cv.seg(px, py, px - np.cos(a) * ln, py - np.sin(a) * ln)
                    cv.add((255, 236, 170), gauss(dd, 0.5) * 1.3 * fade)
                elif kind == "drop":
                    dd = cv.dist(px, py)
                    cv.over(LIME if sz > 1 else (0, 200, 140), solid(dd, 0.6 + sz * 0.5) * fade)
                else:
                    c = {"chunk": (60, 50, 70), "plate": (90, 70, 30),
                         "shard": (160, 32, 168)}[kind]
                    s = 0.9 + sz * 0.9 * (size / 64) ** 0.5
                    rot = a + t * 6 * sz
                    pts = [(px + np.cos(rot + k * 2.1) * s, py + np.sin(rot + k * 2.1) * s * 0.7)
                           for k in range(3)]
                    dd = cv.poly(pts)
                    cv.over(c, solid(dd, 0.0) * fade)
                    cv.over((230, 210, 150) if kind == "plate" else (255, 160, 255),
                            solid(dd, 0.0) * (dd > -0.6) * fade * 0.6)
        out.append(cv.image())
    return out


def water_frames(size, n, seed=3, under=False):
    """Surface splash (white column, spray, patchy foam, broken ripple arcs) or an under-water
    burst (muffled flash, bubble cloud rising and spreading)."""
    rng = rnd(seed)
    R = size / 2
    tex = _cart_noise(size, seed + 7, cell=max(6, size * SS // 6))
    bubbles = [(rng.uniform(0, TAU), rng.uniform(0.1, 0.9), rng.uniform(0.6, 2.2),
                rng.uniform(0.6, 1.4)) for _ in range(int(size * 0.7))]
    drops = [(rng.uniform(0, TAU), rng.uniform(0.4, 1.0), rng.uniform(0.6, 1.6))
             for _ in range(int(size * 0.55))]
    blobs = [(rng.uniform(0, TAU), rng.uniform(0.8, 1.2), rng.uniform(0.04, 0.09))
             for _ in range(int(10 + size * 0.25))]
    out = []
    for i in range(n):
        t = i / max(n - 1, 1)
        cv = Canvas(size, size)
        d = cv.dist(R, R) / R
        if under:
            flash = gauss(d, 0.25 + 0.3 * t) * np.clip(1.1 - t * 1.6, 0, 1)
            cv.add((120, 230, 220), flash * 1.1)
            murk = np.clip(1 - d / (0.3 + 0.7 * t), 0, 1) * (0.5 + 0.6 * tex) * (1 - t)
            cv.over((60, 140, 150), murk * 0.35)
            for a, r, s, sp in bubbles:
                rr = (0.15 + r * (0.5 + 0.6 * t)) * R
                bx = R + np.cos(a) * rr
                by = R + np.sin(a) * rr * 0.9 - t * t * R * 0.6 * sp
                dd = cv.dist(bx, by)
                fade = np.clip(1.3 - t, 0, 1)
                cv.over((170, 240, 250), (np.abs(dd - s) < 0.45) * 0.85 * fade)
                cv.add(WHITE, gauss(cv.dist(bx - s * 0.4, by - s * 0.4), 0.35) * fade)
        else:
            warp = (tex - 0.5) * 0.35
            for k in range(2):                              # broken ripple arcs
                rr = 0.25 + t * 0.8 - k * 0.22
                if rr <= 0.1:
                    continue
                dw = d + warp
                gate = np.clip((_shift(tex, 7 * k, 11) - 0.42) * 4, 0, 1)
                crest = gauss(dw - rr, 0.04) * gate * (1 - t) ** 0.8
                trough = gauss(dw - rr + 0.07, 0.05) * gate * (1 - t) ** 0.8
                cv.over((8, 24, 50), trough * 0.4)
                cv.add((160, 210, 240), crest * 0.55)
            fr = 0.15 + 0.5 * (1 - (1 - t) ** 2)            # patchy foam
            foam = np.clip(1 - np.abs(d + warp * 0.6 - fr) / 0.16, 0, 1)
            foam *= np.clip((tex - 0.35) * 5, 0, 1)
            cv.over((235, 250, 255), np.clip(foam * 1.4, 0, 1) * np.clip(1.15 - t, 0, 1))
            for a, rf, s in blobs:                          # foam clumps drifting out
                rr = fr * rf * R
                bx, by = R + np.cos(a) * rr, R + np.sin(a) * rr
                bd = cv.ell(bx, by, s * R * 1.6, s * R, a)
                cv.over((225, 245, 255), (bd < 1 + (tex - 0.5)) * np.clip(1.1 - t, 0, 1) * 0.9)
            col = np.clip((0.32 * (1 - t) ** 1.3 - d - warp * 0.4) * 9, 0, 1)
            cv.over(WHITE, col)
            for a, sp, s in drops:
                rr = (0.1 + sp * 0.95 * (1 - (1 - t) ** 1.8)) * R
                bx, by = R + np.cos(a) * rr, R + np.sin(a) * rr
                cv.over((220, 245, 255), solid(cv.dist(bx, by), s * (1 - 0.6 * t))
                        * np.clip(1.2 - t, 0, 1))
        out.append(cv.image())
    return out


def hit_flash_frames(sp):
    """Normal -> white silhouette -> half-white -> normal (2 game frames each)."""
    white = Image.new("RGBA", sp.size, WHITE + (0,))
    white.putalpha(sp.getchannel("A"))
    half = Image.blend(sp, white, 0.5)
    return [sp, white, half, sp]


def shield_frames(sp, n=6, hit=(0.6, -0.8)):
    """Shield-hit shimmer: hex cells on the bubble light up around the hit point and a ripple
    runs over the surface."""
    w, h = sp.width + 28, sp.height + 28
    out = []
    for i in range(n):
        t = i / max(n - 1, 1)
        cv = Canvas(w, h)
        cx, cy = w / 2, h / 2
        rx, ry = sp.width * 0.62, sp.height * 0.66
        e = cv.ell(cx, cy, rx, ry)
        u, v = (cv.x - cx) / rx, (cv.y - cy) / ry
        # hex grid on the bubble
        hx = u * 7.0
        hy = v * 7.0 * 1.1547
        q = np.abs(((hx + 0.5 * (np.floor(hy) % 2)) % 1.0) - 0.5)
        r = np.abs((hy % 1.0) - 0.5)
        lines = np.clip(1 - np.minimum(q, r) * 9, 0, 1)
        hd = np.hypot(u - hit[0], v - hit[1])
        ripple = gauss(hd - t * 1.8, 0.22) * (1 - t)
        local = gauss(hd, 0.5) * (1 - t) ** 1.5
        shell = np.clip(1 - np.abs(e - 1) * 9, 0, 1) * (e < 1.08)
        inside = (e < 1).astype(float)
        amt = (shell * (0.25 + 1.2 * local + ripple)
               + inside * lines * (0.9 * local + 0.7 * ripple)) * (1.0 - 0.35 * t)
        cv.add(PS[2], amt * 0.9)
        cv.add(WHITE, gauss(hd, 0.18) * (1 - t) ** 2 * 1.4 * shell)
        img = cv.image()
        base = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        put(base, sp, w / 2, h / 2)
        base.alpha_composite(img)
        out.append(base)
    return out


def shield_break_frames(sp, n=8, seed=9):
    rng = rnd(seed)
    w, h = sp.width + 44, sp.height + 44
    shards = [(rng.uniform(0, TAU), rng.uniform(0.6, 1.0), rng.uniform(1.5, 3.5),
               rng.uniform(-6, 6)) for _ in range(26)]
    out = []
    for i in range(n):
        t = i / max(n - 1, 1)
        cv = Canvas(w, h)
        cx, cy = w / 2, h / 2
        rx, ry = sp.width * 0.62, sp.height * 0.66
        e = cv.ell(cx, cy, rx, ry)
        if i <= 1:
            shell = np.clip(1 - np.abs(e - 1) * 7, 0, 1)
            cv.add(PS[3], shell * 1.4)
            cv.add(WHITE, (e < 1) * 0.35 * (1 - i * 0.5))
            if i == 1:
                for k in range(7):
                    a = k * TAU / 7 + 0.3
                    d = cv.seg(cx, cy, cx + np.cos(a) * rx, cy + np.sin(a) * ry)
                    cv.add(WHITE, gauss(d, 0.5) * 1.5)
        else:
            tt = (t - 2 / (n - 1)) / (1 - 2 / (n - 1))
            for a, r, s, spin in shards:
                rr = (r + tt * 0.9)
                px, py = cx + np.cos(a) * rx * rr, cy + np.sin(a) * ry * rr
                rot = a + spin * tt
                pts = [(px + np.cos(rot + k * 2.2) * s, py + np.sin(rot + k * 2.2) * s)
                       for k in range(3)]
                dd = cv.poly(pts)
                cv.add(PS[3], solid(dd, 0.2) * (1 - tt) * 1.2)
            cv.add(PS[2], gauss(e - 1 - tt * 0.5, 0.15) * (1 - tt) * 0.5)
        img = cv.image()
        base = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        put(base, sp, w / 2, h / 2)
        base.alpha_composite(img)
        out.append(base)
    return out


LADDER = [("TINY", 24, 6), ("SMALL", 40, 8), ("MEDIUM", 64, 10), ("LARGE", 96, 12),
          ("HUGE", 144, 14)]


# =========================================================================== 1. player projectiles

def energy_bolt(length, radius, body=PS[2], core=PS[4], angle=0.0, glow=1.0, pad=4):
    """Soft energy bolt along ``angle`` (0 = up). Player shots may be semi-transparent."""
    ca, sa = np.sin(angle), -np.cos(angle)
    ext = length / 2
    w = int(np.ceil(abs(ca) * length + 2 * radius + 2 * pad))
    h = int(np.ceil(abs(sa) * length + 2 * radius + 2 * pad))
    cv = Canvas(w, h)
    cx, cy = w / 2, h / 2
    d = cv.seg(cx - ca * ext, cy - sa * ext, cx + ca * ext, cy + sa * ext)
    cv.add(body, gauss(d, radius * 1.7) * 0.55 * glow)
    cv.over(body, solid(d, radius) * 0.85)
    cv.over(core, solid(d, radius * 0.45))
    return cv.image()


def tracer(length, radius=1.0, angle=0.0):
    """Ballistic tracer: white slug with a pale blue fading tail."""
    ca, sa = np.sin(angle), -np.cos(angle)
    w = int(np.ceil(abs(ca) * length + 8))
    h = int(np.ceil(abs(sa) * length + 8))
    cv = Canvas(w, h)
    cx, cy = w / 2, h / 2
    hx, hy = cx + ca * length / 2, cy + sa * length / 2
    tx, ty = cx - ca * length / 2, cy - sa * length / 2
    d = cv.seg(hx, hy, tx, ty)
    along = np.clip(((cv.x - tx) * ca + (cv.y - ty) * sa) / length, 0, 1)
    cv.add(PS[3], gauss(d, radius * 1.4) * along ** 1.5 * 0.9)
    cv.over(WHITE, solid(cv.dist(hx - ca * 1.2, hy - sa * 1.2), radius + 0.4))
    return cv.image()


def laser_lance(length, width):
    cv = Canvas(width * 4 + 6, length + 6)
    cx = cv.w / 2
    d = cv.seg(cx, 3 + width, cx, length + 3 - width)
    cv.add(PS[2], gauss(d, width * 1.4) * 0.8)
    cv.over(PS[3], solid(d, width * 0.75))
    cv.over(WHITE, solid(d, width * 0.32))
    return cv.image()


def beam_column(h, width, t=0.0):
    """Ion beam: continuous column with energy ripples travelling up."""
    cv = Canvas(width * 4 + 8, h)
    cx = cv.w / 2
    dx = np.abs(cv.x - cx)
    wob = 1 + 0.15 * np.sin(cv.y * 0.35 + t * 40)
    cv.add(PS[1], gauss(dx, width * 1.6 * wob) * 0.7)
    cv.over(PS[2], solid(dx, width * 0.6 * wob) * 0.9)
    rip = (np.sin(cv.y * 0.22 + t * 60) > 0.6) * solid(dx, width * 0.75)
    cv.add(PS[3], rip * 0.5)
    cv.over(WHITE, solid(dx, width * 0.25 * wob))
    return cv.image()


def muzzle_frames(kind, n=3):
    out = []
    for i in range(n):
        k = 1 - i / n
        cv = Canvas(20, 20)
        d = cv.dist(10, 12)
        if kind == "energy":
            ang = np.arctan2(cv.y - 12, cv.x - 10)
            star = np.abs(np.cos(ang * 2)) ** 8
            cv.add(PS[2], (gauss(d, 4.5 * k) + star * gauss(d, 7 * k)) * 1.2)
            cv.over(WHITE, solid(d, 1.8 * k))
        elif kind == "ballistic":
            cross = np.minimum(np.abs(cv.x - 10), np.abs(cv.y - 12))
            cv.add((200, 230, 255), gauss(cross, 0.7) * gauss(d, 6 * k) * 1.6)
            cv.add(WHITE, gauss(d, 2.4 * k) * 1.4)
        else:  # launcher: lit puff of exhaust smoke drifting back, with a small flash
            for j, (ox, oy, rr) in enumerate(((0, 0, 3.2), (-2.5, 2, 2.4), (2.6, 2.4, 2.2))):
                e = cv.ell(10 + ox * (1 + i * 0.4), 11 + oy + i * 2, rr + i, rr + i * 0.8)
                cv.over((120, 126, 160), solid(e, 1) * (0.75 - i * 0.22))
                e2 = cv.ell(9.4 + ox * (1 + i * 0.4), 10.4 + oy + i * 2, (rr + i) * 0.6,
                            (rr + i * 0.8) * 0.6)
                cv.over((190, 196, 220), solid(e2, 1) * (0.6 - i * 0.18))
            if i == 0:
                cv.add((200, 230, 255), gauss(d, 2.6) * 1.5)
        out.append(cv.image())
    return out


def impact_frames(kind, n=4, seed=5):
    rng = rnd(seed)
    sparks = [(rng.uniform(-2.6, -0.5), rng.uniform(0.6, 1.0)) for _ in range(9)]
    if kind == "explosive":
        return explosion_frames(24, n, seed, "fire")
    out = []
    for i in range(n):
        t = i / max(n - 1, 1)
        cv = Canvas(22, 22)
        cx, cy = 11, 14
        d = cv.dist(cx, cy)
        col = PS[2] if kind == "energy" else (230, 240, 255)
        cv.add(col, gauss(d, 3.5 * (1 - 0.6 * t)) * (1.4 - t))
        for a, s in sparks:
            r0 = 2 + t * 8 * s
            px, py = cx + np.cos(a) * r0, cy + np.sin(a) * r0
            ln = 3 * (1 - t)
            dd = cv.seg(px, py, px - np.cos(a) * ln, py - np.sin(a) * ln)
            cv.add(PS[3] if kind == "energy" else (255, 255, 255), gauss(dd, 0.5) * (1.2 - t))
        out.append(cv.image())
    return out


def _obj(model, native, extent, **kw):
    scene, mats = model(**kw)
    return render_obj(scene, mats, native, extent, colors=20)


def smoke_puff(r, alpha):
    cv = Canvas(int(r * 2 + 4), int(r * 2 + 4))
    c = cv.w / 2
    cv.over((150, 156, 186), solid(cv.dist(c, c), r) * alpha)
    cv.over((200, 205, 225), solid(cv.dist(c - r * 0.3, c - r * 0.3), r * 0.6) * alpha * 0.7)
    return cv.image()


class Proj:
    """Projectile catalogue: per family a sprite, muzzle kind, impact kind and a pattern
    function ``pattern(level) -> [(dx, dy, angle_rad, sprite_key), ...]``."""

    def __init__(self):
        self.s = {}
        s = self.s
        s["pulse"] = energy_bolt(12, 2.6)
        s["pulse5"] = energy_bolt(14, 3.1)
        s["pulse_d"] = energy_bolt(10, 2.2, angle=np.pi)          # tail gun (rear)
        s["vulcan"] = tracer(11, 1.0)
        s["ballistic"] = tracer(8, 1.4)
        for lv, wd in ((1, 2), (3, 3), (5, 4)):
            s[f"laser{lv}"] = laser_lance(40, wd)
        s["missile"] = _obj(missile_model, 16, 1.9)
        s["micro"] = _obj(missile_model, 10, 1.9)
        s["missile_fx"] = with_plume(s["missile"], 9, 2.2)
        s["micro_fx"] = with_plume(s["micro"], 6, 1.5)
        s["shell"] = _obj(shell_model, 10, 1.7)
        s["bomb"] = _obj(bomb_model, 12, 2.0)
        s["torpedo"] = _obj(torpedo_model, 18, 2.1)
        s["mine_on"] = _obj(mine_model, 12, 1.8, lit=1.0)
        s["mine_off"] = _obj(mine_model, 12, 1.8, lit=0.0)
        s["side"] = energy_bolt(10, 2.2, angle=np.pi / 2)
        s["side_l"] = energy_bolt(10, 2.2, angle=-np.pi / 2)
        self.muzzle = {k: muzzle_frames(k) for k in ("energy", "ballistic", "launcher")}
        self.impact = {k: impact_frames(k) for k in ("energy", "ballistic", "explosive")}

    def rot(self, key, ang):
        return self.s[key].rotate(-np.degrees(ang), resample=Image.NEAREST, expand=True)


# family -> (title, weapons, sprite key for the swatch, muzzle, impact)
FAMILIES = [
    ("pulse", "PULSE", "PULSE CANNON (FRONT)", "pulse", "energy", "energy"),
    ("vulcan", "VULCAN", "SCATTER VULCAN (FRONT, SPREAD)", "vulcan", "ballistic", "ballistic"),
    ("ballistic", "BALLISTIC", "AUTOCANNON POD, SWIVEL GUN (WING)", "ballistic", "ballistic",
     "ballistic"),
    ("laser", "LASER", "LANCE LASER (FRONT, PIERCING)", "laser3", "energy", "energy"),
    ("beam", "BEAM", "ION BEAM (FRONT, L15)", None, "energy", "energy"),
    ("missile", "MISSILE", "HORNET LAUNCHER (FRONT, HOMING)", "missile_fx", "launcher",
     "explosive"),
    ("micro", "MICRO-MISSILE", "MICRO-MISSILE POD (WING, HOMING)", "micro_fx", "launcher",
     "explosive"),
    ("mortar", "MORTAR", "HAMMER MORTAR (FRONT, GROUND ONLY)", "shell", "launcher", "explosive"),
    ("bomb", "BOMB", "BOMB RACK (WING, GROUND ONLY)", "bomb", "launcher", "explosive"),
    ("torpedo", "TORPEDO", "TORPEDO POD (WING, ANTI-SUB)", "torpedo", "launcher", "explosive"),
    ("mine", "MINE", "PROXIMITY MINES (REAR, AREA)", "mine_on", "launcher", "explosive"),
    ("rear", "REAR", "TAIL GUN, FAN BLASTER (REAR)", "pulse_d", "energy", "energy"),
    ("side", "SIDE", "SIDE SPLITTER (LEFT + RIGHT)", "side", "energy", "energy"),
]


def pattern(family, level):
    """Emitter list for one volley: (dx, dy, angle, key). angle 0 = up."""
    r = np.radians
    if family == "pulse":
        return {1: [(0, 0, 0, "pulse")], 3: [(-7, 0, 0, "pulse"), (7, 0, 0, "pulse")],
                5: [(-7, 0, 0, "pulse5"), (7, 0, 0, "pulse5"), (-12, 6, r(-12), "pulse"),
                    (12, 6, r(12), "pulse")]}[level]
    if family == "vulcan":
        n = {1: 3, 3: 5, 5: 7}[level]
        half = {1: 10, 3: 20, 5: 30}[level]
        return [(0, 0, r(-half + 2 * half * k / (n - 1)), "vulcan") for k in range(n)]
    if family == "ballistic":
        base = [(-31, 8), (31, 8)]
        out = [(x, y, 0, "ballistic") for x, y in base]
        if level >= 5:
            out += [(x + (3 if x > 0 else -3), y + 4, 0, "ballistic") for x, y in base]
        return out
    if family == "laser":
        k = f"laser{level}"
        return [(0, 0, 0, k)] if level < 5 else [(-6, 0, 0, k), (6, 0, 0, k)]
    if family == "missile":
        n = {1: 1, 3: 2, 5: 4}[level]
        return [(-14 + 28 * k / max(n - 1, 1) if n > 1 else 0, 0, 0, "missile") for k in range(n)]
    if family == "micro":
        n = {1: 1, 3: 2, 5: 3}[level]
        return [(sx * 31 + sx * 4 * k, 8, 0, "micro") for sx in (-1, 1) for k in range(n)]
    if family == "mortar":
        return [(0, 0, 0, "shell")] if level < 5 else [(-8, 0, 0, "shell"), (8, 0, 0, "shell")]
    if family == "bomb":
        return [(sx * 31, 8, 0, "bomb") for sx in (-1, 1)]
    if family == "torpedo":
        return [(sx * 31, 8, 0, "torpedo") for sx in (-1, 1)]
    if family == "mine":
        return [(0, 20, np.pi, "mine_on")] if level < 5 else [(-8, 20, np.pi, "mine_on"),
                                                                (8, 20, np.pi, "mine_on")]
    if family == "rear":
        if level == 1:
            return [(0, 18, np.pi, "pulse_d")]
        n = 3 if level == 3 else 5
        return [(0, 18, np.pi + r(-24 + 48 * k / (n - 1)), "pulse_d") for k in range(n)]
    if family == "side":
        out = [(-16, 0, -np.pi / 2, "side"), (16, 0, np.pi / 2, "side")]
        if level >= 3:
            out += [(-16, 6, -np.pi / 2, "side"), (16, 6, np.pi / 2, "side")]
        if level >= 5:
            out += [(-14, -4, r(-55), "side"), (14, -4, r(55), "side")]
        return out
    return []


def with_plume(sp, length=8, width=2.0, color=PS[3]):
    """Attach an exhaust plume behind a nose-up physical round."""
    w, h = sp.width + 8, sp.height + length + 4
    cv = Canvas(w, h)
    cx, top = w / 2, sp.height - 2
    d = cv.seg(cx, top, cx, top + length)
    along = np.clip((cv.y - top) / length, 0, 1)
    cv.add(color, gauss(d, width * (1 - 0.5 * along)) * (1 - along) * 1.2)
    cv.add(WHITE, gauss(d, width * 0.4) * (1 - along) ** 2)
    img = cv.image()
    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    out.alpha_composite(img)
    out.alpha_composite(sp, ((w - sp.width) // 2, 2))
    return out


VOLLEYS = {"ballistic": {1: (3, 30), 3: (5, 18), 5: (5, 18)}, "laser": {1: (2, 52), 3: (2, 52),
           5: (2, 52)}, "mine": {1: (3, 30), 3: (3, 30), 5: (3, 30)}}
REARWARD = ("rear", "mine")


def pattern_view(P, family, level, ship):
    """Frozen view of a stream at one upgrade level: several volleys in flight."""
    w, h = 120, 150
    img = Image.new("RGBA", (w, h), (10, 12, 26, 255))
    rear = family in REARWARD
    sx, sy = w / 2, (40 if rear else 124)
    if family == "beam":
        col = beam_column(sy - 18, {1: 3, 3: 5, 5: 7}[level])
        sprite.paste(img, col, sx - col.width / 2, 0)
    count, gap = VOLLEYS.get(family, {}).get(level, (3, 30))
    for v in range(count if family != "beam" else 0):
        g = v * gap
        for dx, dy, ang, key in pattern(family, level):
            ln = 28 + g
            if family in ("mortar", "bomb"):
                ln = 18 + g * 0.8
            if family == "mine":
                ln = 8 + g
            if family == "side":
                ln = 16 + g * 0.7
            px = sx + dx + np.sin(ang) * ln
            py = sy - 26 + dy - np.cos(ang) * ln if not rear else sy + dy - 14 - np.cos(ang) * ln
            if family in ("missile", "micro"):
                bend = np.sin(v * 1.3 + dx * 0.2) * 5 * (v + 1)
                px += bend
                for k in range(1, 5):                 # smoke trail behind
                    tb = np.sin(v * 1.3 + dx * 0.2) * 5 * max(v + 1 - k * 0.3, 0)
                    put(img, smoke_puff(1.2 + k * 0.5, 0.55 - k * 0.1),
                        sx + dx + tb, py + 6 + k * 5)
                sp = P.s[key + "_fx"]
            elif family == "torpedo":
                for k in range(1, 6):
                    ImageDraw.Draw(img).ellipse([px - 1 + (k % 2), py + 8 + k * 4,
                                                 px + (k % 2), py + 9 + k * 4],
                                                outline=(170, 230, 250, 200))
                sp = P.s[key]
            elif family in ("mortar", "bomb"):
                sp = P.s[key]
                k = 1.0 - 0.15 * v if family == "bomb" else 1.0 + 0.3 * np.sin(v / 2 * np.pi)
                sp = sp.resize((max(2, int(sp.width * k)), max(2, int(sp.height * k))),
                               Image.NEAREST)
                if family == "mortar":
                    sp = with_halo(sp)
            elif family == "mine":
                sp = P.s["mine_on" if v % 2 == 0 else "mine_off"]
            elif key in ("pulse_d", "side", "side_l", "laser1", "laser3", "laser5"):
                sp = P.s["side_l" if (key == "side" and dx < 0 and abs(ang) > 1.2) else key]
                if key == "side" and abs(abs(ang) - np.pi / 2) > 0.1:
                    sp = P.rot("pulse", ang)
                elif key == "pulse_d" and abs(ang - np.pi) > 0.05:
                    sp = P.rot("pulse", ang)
            else:
                sp = P.rot(key, ang)
            put(img, sp, px, py)
    if family in ("mortar", "bomb"):                      # ground bursts at the far end
        boom = explosion_frames(24, 6, 11 + level, "fire")[2]
        for dx, dy, ang, key in pattern(family, level):
            ln = 18 + (count - 1) * gap * 0.8 + 16
            put(img, boom, sx + dx, sy - 26 + dy - ln)
    put(img, ship, sx, sy)
    label(img, 4, 4, f"L{level}", LABEL)
    return img


def with_halo(sp):
    w, h = sp.width + 8, sp.height + 8
    cv = Canvas(w, h)
    cv.add(PS[2], gauss(cv.dist(w / 2, h / 2), sp.width * 0.55) * 0.7)
    out = cv.image()
    out.alpha_composite(sp, (4, 4))
    return out


def projectiles_sheet(P, ship):
    row_h = 168
    img = raster.sheet(1300, 70 + row_h * len(FAMILIES) + 60,
                       "PLAYER PROJECTILES - FAMILIES, MUZZLE FLASHES, IMPACTS, L1/L3/L5", SUB)
    heads = [(16, "FAMILY / WEAPONS"), (250, "SPRITE 1X / ZOOM"), (430, "MUZZLE (3 FRAMES, 3X)"),
             (640, "IMPACT (4 FRAMES, 3X)"), (920, "PATTERN L1 / L3 / L5 (1X)")]
    for x, t in heads:
        label(img, x, 36, t)
    for r, (fam, title, weapons, key, muz, imp) in enumerate(FAMILIES):
        y = 50 + r * row_h
        ImageDraw.Draw(img).line([16, y - 4, 1284, y - 4], fill=(40, 46, 64, 255))
        label(img, 16, y + 4, title, raster.ACCENT, scale=2)
        for i, part in enumerate(weapons.split(", ")):
            label(img, 16, y + 26 + i * 12, part, LABEL)
        if key is None:
            sp = beam_column(60, 4)
        else:
            sp = P.s[key]
        z = int(max(1, min(4, 120 // max(sp.height, 1), 70 // max(sp.width, 1))))
        img.alpha_composite(on_checker(sp, 1, 2), (250, y + 8))
        img.alpha_composite(on_checker(sp, z, 2), (250 + sp.width + 14, y + 8))
        for i, f in enumerate(P.muzzle[muz]):
            img.alpha_composite(on_checker(f, 3), (430 + i * 66, y + 8))
        for i, f in enumerate(P.impact[imp]):
            fz = sprite.enlarge(f, 3) if f.width <= 24 else f
            img.alpha_composite(on_checker(fz, 1), (640 + i * 70, y + 8))
        for i, lv in enumerate((1, 3, 5)):
            img.alpha_composite(pattern_view(P, fam, lv, ship), (920 + i * 126, y + 4))
    notes = ["PLAYER SHOTS ARE BLUE / WHITE / CYAN AND SOFT-EDGED (MAY BE SEMI-TRANSPARENT); THEY NEVER "
             "SHARE A HUE WITH ENEMY BULLETS. PHYSICAL ROUNDS (MISSILES, SHELLS, BOMBS, TORPEDOES, MINES)",
             "ARE PRE-RENDERED MODELS. BOMBS AND MORTAR SHELLS ARE GROUND-ONLY: THEY SHRINK AS THEY FALL "
             "(BOMB) OR SWELL ON THE LOB (SHELL) AND BURST ON THE GROUND LAYER."]
    for i, line in enumerate(notes):
        label(img, 16, img.height - 46 + i * 12, line, LABEL)
    return img


# ---- projectiles GIF

RATE = {"pulse": 0.12, "vulcan": 0.11, "ballistic": 0.07, "laser": 0.32, "missile": 0.45,
        "micro": 0.38, "mortar": 0.62, "bomb": 0.5, "torpedo": 0.6, "mine": 0.45, "rear": 0.14,
        "side": 0.18}
SPEED = {"pulse": 620, "vulcan": 650, "ballistic": 720, "laser": 950, "missile": 330,
         "micro": 340, "torpedo": 230, "rear": 520, "side": 520}
GIF_PODS = {"ballistic": "autocannon", "micro": "micro-missile", "bomb": "bomb-rack",
            "torpedo": "torpedo", "beam": None}


def bank_frame(vx):
    i = int(np.clip(round(vx / 35), -2, 2))
    return BANKS[i + 2]


def projectiles_gif(P):
    seg = 1.6
    nfr = int(seg * FPS)
    bg_front = terrain.recede(terrain.earth_coast(FW, FH, seed=8, period=False,
                                                  stops=terrain.coast_stops_from(
                                                      B["EARTH ORBIT"], B["EARTH SURFACE"]),
                                                  cell=128), amount=0.35, darken=0.62)
    dart = vrell_sprite(36)
    frames = []
    imp = P.impact
    ground_boom = explosion_frames(32, 8, 21, "fire")
    for fam, title, weapons, key, muz, impk in FAMILIES:
        level = 5 if fam == "beam" else 3
        rear = fam in REARWARD
        side = fam == "side"
        pod = GIF_PODS.get(fam, "autocannon")
        sy = 250 if rear or side else 440
        if rear:
            targets = [(150, 470), (240, 480), (330, 470)]
        elif side:
            targets = [(40, sy - 30), (40, sy + 10), (440, sy - 30), (440, sy + 10)]
        else:
            targets = [(120, 110), (200, 96), (280, 96), (360, 110)]
        shots = []        # (t0, x0, y0, ang, key)
        tt = 0.05
        while tt < seg - 0.25:
            sx = 240 + 46 * np.sin(tt * 2.2)
            for dx, dy, ang, k in pattern(fam, level):
                shots.append((tt, sx + dx, sy - 22 + dy if not rear else sy - 6 + dy, ang, k))
            tt += RATE.get(fam, 0.2)
        for f in range(nfr):
            t = f / FPS
            img = bg_front.copy()
            for i, (tx, ty) in enumerate(targets):
                bob = 3 * np.sin(t * 4 + i)
                sp = dart if not rear else dart.rotate(180)
                if side:
                    sp = dart.rotate(-90 if tx < 240 else 90, expand=True)
                shadow(img, sp, tx, ty + bob, 14, 20, 0.45)
                put(img, sp, tx, ty + bob)
            sx = 240 + 46 * np.sin(t * 2.2)
            vx = 46 * 2.2 * np.cos(t * 2.2)
            ship = ship_sprite(bank_frame(vx), pod)[1]
            if fam == "beam":
                top = 112
                col = beam_column(int(sy - 24 - top), 7, t)
                sprite.paste(img, col, sx - col.width / 2, top)
                fr = imp["energy"][int(t * 20) % 3]
                put(img, sprite.enlarge(fr, 2), sx, top + 4)
            for (t0, x0, y0, ang, k) in shots:
                age = t - t0
                if age < 0:
                    continue
                if age < 0.1 and fam not in ("mine",):
                    mf = P.muzzle[muz][min(2, int(age / 0.034))]
                    put(img, mf, x0, y0 - 2 if not rear else y0 + 6)
                if fam in ("pulse", "vulcan", "ballistic", "laser", "rear", "side"):
                    v = SPEED[fam]
                    px, py = x0 + np.sin(ang) * v * age, y0 - np.cos(ang) * v * age
                    hit = None
                    for (tx, ty) in targets:
                        if side:
                            if abs(py - ty) < 14 and abs(px - tx) < 14:
                                hit = (tx, py)
                        elif abs(px - tx) < 16 and ((not rear and py <= ty + 12) or
                                                    (rear and py >= ty - 12)):
                            hit = (px, ty + (12 if not rear else -12))
                    if hit:
                        ha = age - (abs(hit[1] - y0) / v if not side else abs(hit[0] - x0) / v)
                        fi = int(ha / 0.05)
                        if 0 <= fi < 4:
                            put(img, imp[impk][fi], *hit)
                        continue
                    if -30 < px < FW + 30 and -40 < py < FH + 40:
                        if k in ("pulse_d", "side", "side_l", "laser3", "laser5", "laser1"):
                            sp = P.s["side_l" if k == "side" and ang < 0 else k]
                            if k == "pulse_d" and abs(ang - np.pi) > 0.05:
                                sp = P.rot("pulse", ang)
                            if k == "side" and abs(abs(ang) - np.pi / 2) > 0.1:
                                sp = P.rot("pulse", ang)
                        else:
                            sp = P.rot(k, ang)
                        put(img, sp, px, py)
                elif fam in ("missile", "micro"):
                    tx, ty = min(targets, key=lambda q: abs(q[0] - x0))
                    dur = 0.75 if fam == "missile" else 0.6
                    u = age / dur
                    if u > 1:
                        fi = int((age - dur) / 0.06)
                        if fi < 4:
                            put(img, imp[impk][fi], tx, ty)
                        continue
                    cx = x0 + (tx - x0) * u ** 1.6 + np.sin(u * np.pi) * (x0 - 240) * 0.4
                    cy = y0 + (ty - y0) * u
                    for kk in range(1, 6):
                        uu = max(u - kk * 0.05, 0)
                        bx = x0 + (tx - x0) * uu ** 1.6 + np.sin(uu * np.pi) * (x0 - 240) * 0.4
                        by = y0 + (ty - y0) * uu
                        put(img, smoke_puff(1.2 + kk * 0.6, 0.5 - kk * 0.08), bx, by + 6)
                    hd = np.arctan2(tx - cx, -(ty - cy))
                    put(img, P.rot(k + "_fx", hd), cx, cy)
                elif fam in ("mortar", "bomb"):
                    dur = 0.8 if fam == "mortar" else 0.6
                    dist = 200 if fam == "mortar" else 60
                    u = age / dur
                    gx, gy = x0, y0 - dist
                    if u > 1:
                        fi = int((age - dur) / 0.05)
                        if fi < 8:
                            put(img, ground_boom[fi], gx, gy)
                        continue
                    k_s = 1 + 0.6 * np.sin(u * np.pi) if fam == "mortar" else 1 - 0.55 * u
                    sp = P.s[k]
                    sp = sp.resize((max(2, int(sp.width * k_s)), max(2, int(sp.height * k_s))),
                                   Image.NEAREST)
                    if fam == "mortar":
                        sp = with_halo(sp)
                    gyy = y0 - dist * u
                    sh = sprite.shadow_of(sp, opacity=0.5, blur=1)
                    put(img, sh, x0 + 6 * (1 - u) + 4, gyy + 8 * (1 - u) + 4)
                    put(img, sp, x0, gyy - (24 * np.sin(u * np.pi) if fam == "mortar" else 0))
                elif fam == "torpedo":
                    v = SPEED[fam]
                    px, py = x0, y0 - v * age
                    tx, ty = min(targets, key=lambda q: abs(q[0] - x0))
                    if py < ty + 10:
                        fi = int((ty + 10 - py) / v / 0.06)
                        if fi < 4:
                            put(img, imp[impk][fi], px, ty + 8)
                        continue
                    for kk in range(1, 8):
                        bb = py + 10 + kk * 5
                        ImageDraw.Draw(img).ellipse([px - 1 + (kk % 3) - 1, bb, px + (kk % 3),
                                                     bb + 1], outline=(180, 235, 250, 220))
                    put(img, P.s[k], px, py)
                elif fam == "mine":
                    px, py = x0, y0 + 30 * min(age, 0.4) + 40 * age
                    if age > 1.1:
                        fi = int((age - 1.1) / 0.06)
                        if fi < 4:
                            put(img, imp["explosive"][fi], px, py)
                        continue
                    put(img, P.s["mine_on" if int(age * 6) % 2 == 0 else "mine_off"], px, py)
            shadow(img, ship, sx, sy)
            put(img, ship, sx, sy)
            label(img, 8, 8, title + f"  L{level}", raster.ACCENT, scale=2)
            label(img, 8, 28, weapons, LABEL)
            frames.append(img)
    return frames


# =========================================================================== main

def main(args):
    todo = args or ["ship", "projectiles"]
    if "ship" in todo:
        OUT["ship"].mkdir(parents=True, exist_ok=True)
        path = OUT["ship"] / "player-ship-r08-a.png"
        ship_sheet().convert("RGB").save(path, optimize=True)
        print("wrote", path.relative_to(ROOT))
    if "projectiles" in todo:
        OUT["weapons"].mkdir(parents=True, exist_ok=True)
        P = Proj()
        path = OUT["weapons"] / "projectiles-r08-a.png"
        projectiles_sheet(P, ship_sprite(0, "micro-missile")[1]).convert("RGB").save(
            path, optimize=True)
        print("wrote", path.relative_to(ROOT))
        gif = OUT["weapons"] / "projectiles-r08-a.gif"
        size = write_gif(projectiles_gif(P), gif, fps=FPS, colors=128)
        print("wrote", gif.relative_to(ROOT), f"{size / 1e6:.1f} MB")


if __name__ == "__main__":
    main(sys.argv[1:])
