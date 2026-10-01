#!/usr/bin/env python3
"""Concept round 06 - three bosses without concept art yet.

Each boss gets a large PNG sheet (full sprite with labelled parts and weak points, per-phase
states, parts and angle sheets, a frame from the play field) and a GIF of it in action in the
480x540 play field with the player for scale. Rotating parts use pre-rendered headings
(``enemy_rigs.ModelSpaceAngleSprites``: 16, or 32 for large/slow parts) and the nearest frame.

Outputs (design/enemies/bosses/concept/):
  gorgon-frigate-r06-a.{png,gif}   Act 1 mid-boss: medusa-bell frigate, three serpent-neck
                                   turrets that aim and die separately, then the core opens
  harbour-kraken-r06-a.{png,gif}   Act 2 mid-boss: cephalopod wrapped round a harbour platform;
                                   arms rise from the water to slam lanes, the head surfaces
  siege-spire-r06-a.{png,gif}      Act 2 boss: rooted citadel with root turrets and mortars,
                                   launches Wraiths, tears itself free, airborne laser sweep

Models: render/boss_models.py. Colours: round 04 role colours; weak points glow lime on all
three bosses (the Brood Carrier convention).
Run: python3 tools/concept/bosses_r06.py [gorgon] [kraken] [spire]   (~10 min for all)
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, str(Path(__file__).resolve().parent))
import enemies_r03 as e3  # noqa: E402
import enemies_r05 as r5  # noqa: E402
from render import boss_models as bm  # noqa: E402
from render import enemy_rigs as rig  # noqa: E402
from render import raster, sdf, sprite, terrain  # noqa: E402
from render.config import ROOT  # noqa: E402
from render.palette import B  # noqa: E402

FW, FH, FPS, TAU = r5.FW, r5.FH, r5.FPS, 2 * np.pi
SUB = "CONCEPT ROUND 06 - 960X540"
SHOTS = e3.SHOTS
OUT = ROOT / "design" / "enemies" / "bosses" / "concept"
CALL = (255, 170, 60)
ACID = SHOTS[5]
MS = rig.ModelSpaceAngleSprites


# =========================================================================== shared helpers

def times(seconds):
    return [i / FPS for i in range(int(seconds * FPS))]


def ease(s):
    s = float(np.clip(s, 0.0, 1.0))
    return s * s * (3 - 2 * s)


def angdiff(a, b):
    return (a - b + np.pi) % TAU - np.pi


def heading_to(x0, y0, x1, y1):
    return float(np.arctan2(y1 - y0, x1 - x0))


def static_sprite(make, size, extent, factor=2, colors=48, center=(0.0, 0.0)):
    """Render a model that is not turned (model +Y = screen up)."""
    scene, mats = make()
    w, h = (size, size) if isinstance(size, int) else size
    hi = sdf.render(scene, mats, (w * factor, h * factor), extent, center=center)
    return sprite.make_sprite(hi, factor, colors, crisp=70)


_expl = {}


def explosion_frames(size, seed=1, n=14):
    """Pre-rendered-looking fireball sequence: white-hot core, yellow and orange flame, red
    rim, turning to grey smoke (art direction: 12-16 frames)."""
    key = (size, seed)
    if key in _expl:
        return _expl[key]
    noise = raster.fbm(size, size, max(4, size // 6), seed, octaves=3, period=False)
    noise = (noise - noise.min()) / (np.ptp(noise) + 1e-9)
    yy, xx = np.mgrid[0:size, 0:size]
    c = (size - 1) / 2
    d = np.sqrt((xx - c) ** 2 + (yy - c) ** 2) / (size / 2)
    light = np.clip(1.0 - ((xx - c) + (yy - c)) / size * 0.9, 0.5, 1.4)    # top-left lit
    stops = [(0.0, (90, 24, 12)), (0.25, (200, 60, 20)), (0.5, (255, 140, 40)),
             (0.72, (255, 215, 90)), (0.9, (255, 248, 220)), (1.0, (255, 255, 255))]
    frames = []
    for i in range(n):
        s = i / (n - 1)
        R = 0.3 + 0.7 * (1 - (1 - s) ** 2)
        edge = R * (0.82 + 0.36 * (noise - 0.5))
        inside = d < edge
        heat = np.clip(1 - d / np.maximum(edge, 1e-6), 0, 1) * (1.05 - 0.95 * s) + 0.25 * noise * (1 - s)
        rgb = raster.ramp(stops, np.clip(heat, 0, 1)) * light[..., None]
        smoke = np.clip((s - 0.45) / 0.55, 0, 1)
        grey = np.array([62, 56, 58]) * (0.7 + 0.6 * noise)[..., None]
        rgb = rgb * (1 - smoke) + grey * smoke
        alpha = np.where(inside, 255 * (1 - 0.85 * np.clip((s - 0.6) / 0.4, 0, 1)), 0)
        img = Image.fromarray(np.dstack([np.clip(rgb, 0, 255), alpha]).astype(np.uint8), "RGBA")
        frames.append(img)
    _expl[key] = frames
    return frames


class Booms:
    """Explosions spawned at given times (with debris sparks and an optional shockwave)."""

    def __init__(self):
        self.list = []

    def add(self, t0, x, y, size=64, seed=1, ring=False):
        self.list.append((t0, x, y, size, seed, ring))

    def draw(self, img, t):
        d = ImageDraw.Draw(img)
        for t0, x, y, size, seed, ring in self.list:
            k = int((t - t0) * FPS)
            fr = explosion_frames(size, seed)
            if 0 <= k < len(fr):
                if ring and k < 9:
                    r = size * (0.4 + 0.12 * k)
                    d.ellipse([x - r, y - r * 0.85, x + r, y + r * 0.85],
                              outline=(255, 230, 180, max(0, 220 - 25 * k)), width=2)
                sprite.paste_center(img, fr[k], x, y)
                rng = np.random.default_rng(seed + 9)
                for j in range(8):
                    a = rng.uniform(0, TAU)
                    sp = rng.uniform(1.5, 3.2) * size * 0.06 * k
                    px, py = x + np.cos(a) * sp, y + np.sin(a) * sp + 0.02 * k * k
                    d.rectangle([px - 1, py - 1, px + 1, py + 1], fill=(255, 220, 160, 255))


def shadowed(img, layer, off, opacity=0.55, blur=1.4):
    """Composite a transparent layer with its down-right silhouette shadow."""
    sh = sprite.shadow_of(layer, opacity=opacity, blur=blur)
    img.alpha_composite(sh, (int(off[0]), int(off[1])))
    img.alpha_composite(layer)


def wrap(text, width):
    return e3.wrap(text, width)


def boss_sheet(title, full, callouts, panels, game_frame, notes, full_label):
    """Brood Carrier style sheet: the full sprite with callouts at the top left, the play
    field frame at the right, phase/part panels flowing underneath, then the notes."""
    W = 1300
    gx = W - FW - 16
    fx, fy = 16, 62
    tx = fx + full.width + 46
    # flow the panels below the full sprite / play field
    y0 = max(fy + full.height, 50 + FH) + 40
    x, y, row_h, placed = 16, y0, 0, []
    for lab, im in panels:
        if im.width > W - 32:
            k = (W - 32) / im.width
            im = im.resize((W - 32, int(im.height * k)), Image.LANCZOS)
            lab += f" (SHOWN AT {k:.2f})"
        if x + im.width > W - 16 and x > 16:
            x, y, row_h = 16, y + row_h + 30, 0
        placed.append((x, y, lab, im))
        x += im.width + 20
        row_h = max(row_h, im.height)
    notes_y = y + row_h + 30
    H = notes_y + 14 * len(notes) + 24
    img = raster.sheet(W, H, title, SUB)
    raster.draw_text(img, fx, fy - 14, full_label, raster.LABEL_DIM)
    img.alpha_composite(e3.checker(full.width, full.height), (fx, fy))
    img.alpha_composite(full, (fx, fy))
    d = ImageDraw.Draw(img)
    last = fy - 40
    for (px, py), text in sorted(callouts, key=lambda c: c[0][1]):
        lines = wrap(text, max(12, (gx - tx - 16) // 6))
        ly = max(fy + py - 4, last + 12 + 10 * len(lines))
        if ly != max(fy + py - 4, last + 12 + 10 * len(lines)):
            pass
        last = ly + 10 * (len(lines) - 1)
        d.line([fx + px, fy + py, tx - 26, ly + 3], fill=CALL + (200,), width=1)
        d.line([tx - 26, ly + 3, tx - 4, ly + 3], fill=CALL + (200,), width=1)
        d.ellipse([fx + px - 3, fy + py - 3, fx + px + 3, fy + py + 3], outline=CALL + (255,))
        for j, part in enumerate(lines):
            raster.draw_text(img, tx, ly + j * 10, part, raster.LABEL)
    raster.draw_text(img, gx, 38, "PLAY FIELD 480X540 (1X), FRAME FROM THE GIF", raster.LABEL_DIM)
    img.alpha_composite(game_frame.convert("RGBA"), (gx, 50))
    for px, py, lab, im in placed:
        raster.draw_text(img, px, py - 12, lab, raster.LABEL_DIM)
        img.alpha_composite(im, (px, py))
    for i, line in enumerate(notes):
        raster.draw_text(img, 16, notes_y + i * 14, line, raster.ACCENT if i == 0 else raster.LABEL)
    return img


def panel(w, h, bg=(22, 24, 34, 255)):
    return Image.new("RGBA", (w, h), bg)


def save(slug, sheet, frames):
    OUT.mkdir(parents=True, exist_ok=True)
    png = OUT / f"{slug}-r06-a.png"
    sheet.convert("RGB").save(png, optimize=True)
    size = rig.write_gif(frames, OUT / f"{slug}-r06-a.gif", fps=FPS)
    print(f"wrote {png.relative_to(ROOT)} and .gif ({size / 1e6:.1f} MB, {len(frames)} frames)")


def player_draw(img, x, y, t, shadow="ground", shots=True):
    e3.place(img, e3.player_sprite(), x, y, shadow)
    if shots:
        for k in range(4):
            yy = y - 40 - ((t * 420 + k * 105) % 420)
            if yy > -10:
                e3.bolt(img, x - 10, yy)
                e3.bolt(img, x + 10, yy)


class Shots:
    """Straight bullets: (t0, x, y, vx, vy, kind)."""

    def __init__(self):
        self.list = []

    def fire(self, t0, x, y, ang, speed=170, kind="orb"):
        self.list.append((t0, x, y, np.cos(ang) * speed, np.sin(ang) * speed, kind))

    def aimed(self, t0, x, y, tx, ty, speed=170, kind="orb"):
        self.fire(t0, x, y, heading_to(x, y, tx, ty), speed, kind)

    def draw(self, img, t):
        for t0, x, y, vx, vy, kind in self.list:
            if t < t0:
                continue
            bx, by = x + vx * (t - t0), y + vy * (t - t0)
            if -20 < bx < FW + 20 and -20 < by < FH + 20:
                if kind == "orb":
                    e3.orb(img, bx, by, 5)
                elif kind == "small":
                    e3.orb(img, bx, by, 4)
                elif kind == "acid":
                    e3.orb(img, bx, by, 5, ring=ACID)


# =========================================================================== Gorgon Frigate

G_N = 230
G_EXT = 2.8
G_S = G_N / G_EXT                  # px per model unit
g_body = MS(lambda core=0.0, veil=0.0, lost="": bm.gorgon_body(core_open=core, veil=veil, lost=tuple(int(c) for c in lost)),
            G_N, 32, extent=G_EXT, factor=2, colors=48)
NECK_SIZES = [42, 40, 38, 36, 34]
g_neck = {n: MS(lambda: bm.gorgon_neck(), n, 16, extent=1.5, factor=4, colors=24)
          for n in NECK_SIZES}
g_head = MS(lambda mouth=0.0: bm.gorgon_head(mouth), 56, 32, extent=1.75, factor=4, colors=32)
NECK_BASE = [(0.0, 1.0), (1.0, -0.35), (-1.0, -0.35)]   # local rest direction per socket
NECK_PHASE = [0.0, 2.1, 4.2]
G_HEAD_OFF = 20


def gorgon_rig(cx, cy, h, t, aims, lost=(), mouths=(0, 0, 0), core=0.0, veil=0.0):
    """Returns (layer_parts, head_positions): draw lists for the necks/body/heads."""
    parts_under, parts_over, heads = [], [], {}
    for i, (sx, sy) in enumerate(bm.GORGON_SOCKETS):
        if i in lost:
            continue
        ox, oy = rig.local_to_screen(sx, sy, h, G_S)
        jx, jy = cx + ox, cy + oy
        bx, by = rig.local_to_screen(*NECK_BASE[i], h, 1.0)
        base_h = float(np.arctan2(by, bx))
        aim = aims[i]
        dh = float(np.clip(angdiff(aim, base_h), -1.7, 1.7))
        n = len(NECK_SIZES)
        for k, size in enumerate(NECK_SIZES):
            f = ((k + 1) / (n + 1)) ** 0.85
            hk = base_h + dh * f + 0.32 * np.sin(2.4 * t - 0.9 * k + NECK_PHASE[i])
            step = size * 0.5
            mx, my = jx + np.cos(hk) * step / 2, jy + np.sin(hk) * step / 2
            parts_under.append((g_neck[size].get(hk), mx, my))
            jx, jy = jx + np.cos(hk) * step, jy + np.sin(hk) * step
        hh = aim + 0.08 * np.sin(3.1 * t + i)
        hx, hy = jx + np.cos(hh) * G_HEAD_OFF, jy + np.sin(hh) * G_HEAD_OFF
        parts_over.append((g_head.get(hh, mouth=round(mouths[i], 1)), hx, hy))
        heads[i] = (hx, hy, hh)
    body = g_body.get(h, core=round(core, 1), veil=round(veil, 2),
                      lost="".join(str(i) for i in sorted(lost)))
    return parts_under, (body, cx, cy), parts_over, heads


def gorgon_compose(layer, cx, cy, h, t, aims, **kw):
    under, body, over, heads = gorgon_rig(cx, cy, h, t, aims, **kw)
    for sp, x, y in under:
        sprite.paste_center(layer, sp, x, y)
    sprite.paste_center(layer, body[0], body[1], body[2])
    for sp, x, y in over:
        sprite.paste_center(layer, sp, x, y)
    return heads


def g_player(t):
    return 240 + 95 * np.sin(t * 0.8) + 18 * np.sin(t * 2.3), 490


def gorgon():
    T = 9.0
    bg = e3.regolith(FW, FH, 55, craters=12)
    kills = {0: 4.0, 1: 5.6, 2: 6.9}            # turret i destroyed at t
    core_t = 7.2
    shots, booms = Shots(), Booms()
    veil_k = lambda t: (int(t * 3.0) % 3) * (TAU / 3)      # 3 veil frames

    def center(t):
        y = -170 + (175 + 170) * ease(t / 1.8)
        return 240 + 34 * np.sin(t * 0.7), y + 8 * np.sin(t * 1.3)

    def heading(t):
        return np.pi / 2              # bell keeps facing down; the necks do the aiming

    # schedule bursts: every live turret fires 3-shot bursts, staggered
    bursts = []
    for i in range(3):
        for tb in np.arange(1.9 + 0.6 * i, T, 1.8):
            if tb < kills[i] - 0.1:
                bursts.append((tb, i))
    # the last turret alive fires faster (rage)
    bursts += [(tb, 2) for tb in np.arange(5.9, kills[2] - 0.1, 0.45)]

    def mouths(t):
        m = [0.0, 0.0, 0.0]
        for tb, i in bursts:
            if tb - 0.3 <= t < tb + 0.35:
                m[i] = 1.0
        return m

    def lost_at(t):
        return tuple(i for i, tk in kills.items() if t >= tk)

    # pre-compute head positions for firing and explosions
    for tb, i in sorted(bursts):
        for k in range(3):
            tt = tb + 0.12 * k
            cx, cy = center(tt)
            px, py = g_player(tt)
            aims = [heading_to(cx, cy, px, py)] * 3
            _, _, _, heads = gorgon_rig(cx, cy, heading(tt), tt, aims, lost=lost_at(tt))
            if i in heads:
                hx, hy, hh = heads[i]
                shots.aimed(tt, hx + np.cos(hh) * 20, hy + np.sin(hh) * 20, px, py, 185)
    for i, tk in kills.items():
        cx, cy = center(tk)
        px, py = g_player(tk)
        aims = [heading_to(cx, cy, px, py)] * 3
        _, _, _, heads = gorgon_rig(cx, cy, heading(tk), tk, aims, lost=lost_at(tk - 0.01))
        hx, hy, _ = heads[i]
        booms.add(tk, hx, hy, 70, seed=10 + i)
        ox, oy = rig.local_to_screen(*bm.GORGON_SOCKETS[i], heading(tk), G_S)
        booms.add(tk + 0.15, cx + ox, cy + oy, 44, seed=20 + i)
    for k, tr in enumerate((core_t + 0.4, core_t + 1.1)):
        cx, cy = center(tr)
        for a in np.linspace(0, TAU, 14, endpoint=False) + 0.2 * k:
            shots.fire(tr, cx, cy, a, 150)

    frames = []
    for t in times(T):
        f = bg.copy()
        layer = Image.new("RGBA", (FW, FH), (0, 0, 0, 0))
        cx, cy = center(t)
        px, py = g_player(t)
        aim = heading_to(cx, cy, px, py)
        core = round(2 * ease((t - core_t) / 0.5)) / 2 if t >= core_t else 0.0
        gorgon_compose(layer, cx, cy, heading(t), t, [aim] * 3, lost=lost_at(t),
                       mouths=mouths(t), core=core, veil=veil_k(t))
        # ichor spraying from fresh stumps
        d = ImageDraw.Draw(layer)
        for i, tk in kills.items():
            if tk <= t < tk + 1.4:
                ox, oy = rig.local_to_screen(*bm.GORGON_SOCKETS[i], heading(t), G_S)
                rng = np.random.default_rng(int(t * 40) + i)
                for _ in range(6):
                    a = rng.uniform(0, TAU)
                    r = rng.uniform(4, 18)
                    d.ellipse([cx + ox + np.cos(a) * r - 1.5, cy + oy + np.sin(a) * r - 1.5,
                               cx + ox + np.cos(a) * r + 1.5, cy + oy + np.sin(a) * r + 1.5],
                              fill=(160, 90, 255, 230))
        shadowed(f, layer, (21, 30))
        booms.draw(f, t)
        shots.draw(f, t)
        player_draw(f, px, py, t, "orbit")
        frames.append(f)

    # ---- sheet
    full = Image.new("RGBA", (380, 400), (0, 0, 0, 0))
    heads = gorgon_compose(full, 190, 180, np.pi / 2, 0.6, [np.pi / 2 + 0.2, np.pi / 2 + 0.9,
                                                            np.pi / 2 - 0.9])
    hx = lambda i: (int(heads[i][0]), int(heads[i][1]))
    sk = lambda i: (int(190 + rig.local_to_screen(*bm.GORGON_SOCKETS[i], np.pi / 2, G_S)[0]),
                    int(180 + rig.local_to_screen(*bm.GORGON_SOCKETS[i], np.pi / 2, G_S)[1]))
    callouts = [
        (hx(0), "SERPENT TURRET HEAD: AIMS ON ITS OWN (32 HEADINGS), 3-SHOT AIMED BURSTS"),
        ((hx(0)[0] + 6, hx(0)[1] - 6), "LIME EYES = TURRET WEAK POINT"),
        (hx(1), "EACH TURRET DIES SEPARATELY (PHASES 1-3)"),
        ((190, 180), "CROWN PETALS OVER THE CORE: OPEN WHEN ALL THREE TURRETS ARE GONE"),
        ((190, 120), "MEDUSA BELL: RIBBED BONE HULL (ARMOURED)"),
        (sk(2), "NECK SOCKET: SEGMENT CHAIN OF 5, WAVES AND BENDS TOWARD THE AIM"),
        ((190 + 86, 180 - 30), "GLOWING SCALLOPED RIM"),
        ((190, 262), "PROW: SOCKET OF THE FRONT NECK"),
        ((175, 72), "TRAILING VEIL (ANIMATED)"),
    ]
    states = panel(3 * 250, 290)
    for k, (lab, lost, core) in enumerate((("1  THREE TURRETS", (), 0.0),
                                           ("2  TURRETS LOST (STUMPS LEAK)", (0, 1), 0.0),
                                           ("3  ALL LOST: CORE OPENS (RING BURSTS)", (0, 1, 2), 1.0))):
        tmp = Image.new("RGBA", (380, 400), (0, 0, 0, 0))
        gorgon_compose(tmp, 190, 180, np.pi / 2, 0.6, [np.pi / 2 + 0.2, np.pi / 2 + 0.9,
                                                       np.pi / 2 - 0.9], lost=lost, core=core)
        tmp = tmp.resize((228, 240), Image.NEAREST)
        states.alpha_composite(tmp, (k * 250 + 10, 24))
        raster.draw_text(states, k * 250 + 10, 6, lab, raster.LABEL)
    head_grid = r5.grid([g_head.frame(k, mouth=0.0) for k in range(0, 32, 2)], 8, 1)
    neck_parts = r5.grid([g_neck[n].get(np.pi / 2) for n in NECK_SIZES]
                         + [g_head.get(np.pi / 2, mouth=0.0), g_head.get(np.pi / 2, mouth=1.0)],
                         7, 1, labels=[str(n) for n in NECK_SIZES] + ["SHUT", "FIRE"])
    panels = [("PHASE STATES (0.7X)", states),
              ("NECK SEGMENTS + TURRET HEAD (1X)", neck_parts),
              ("TURRET HEAD: 16 OF 32 HEADINGS (1X)", head_grid)]
    notes = ["GORGON FRIGATE - ACT 1 MID-BOSS, L05 CRATER NEST (BONE CHITIN, VIOLET GLOW = AIMED SHOTS; LIME WEAK POINTS)",
             "THE FIRST MULTI-PART ENEMY. A MEDUSA-BELL WARSHIP ABOUT 230 PX ACROSS WITH THREE SERPENT NECKS (THE 'GORGON').",
             "EACH NECK IS A 5-SEGMENT CHAIN (16 HEADINGS PER SEGMENT) THAT BENDS FROM ITS SOCKET TOWARD THE PLAYER AND",
             "WAVES; THE HEAD (32 HEADINGS) OPENS ITS JAWS 0.3 S BEFORE A 3-SHOT AIMED BURST. TURRETS DIE ONE BY ONE AND LEAVE",
             "LEAKING STUMPS; THE LAST ONE FIRES FASTER. WITH ALL THREE GONE THE CROWN PETALS PART AND THE CORE FIRES RING",
             "BURSTS UNTIL IT DIES. AIR LAYER (SHADOW 21,30) OVER THE LUNAR CRATER NEST; ESCORTS NOT SHOWN."]
    sheet = boss_sheet("GORGON FRIGATE - ACT 1 MID-BOSS (VRELL)", full, callouts, panels,
                       frames[int(4.6 * FPS)], notes, "FULL SPRITE 1X (BELL 230 PX CANVAS + NECKS)")
    return sheet, frames


# =========================================================================== Harbour Kraken

K_MN, K_MEXT = 190, 2.6
K_MS = K_MN / K_MEXT
k_mantle = {}


def kraken_mantle_sprite(eyes, beak):
    key = (eyes, beak)
    if key not in k_mantle:
        k_mantle[key] = MS(lambda: bm.kraken_mantle(eyes, beak), K_MN, 32, extent=K_MEXT,
                           factor=2, colors=48).get(np.pi / 2)
    return k_mantle[key]


ARM_SIZES = [36, 35, 34, 32, 30, 28, 26, 24, 22, 20, 18, 16, 14]
class _ArmCache(dict):
    """Arm segment angle sprites, created on demand per pixel size."""

    def __missing__(self, n):
        self[n] = MS(lambda: bm.kraken_arm(), n, 32, extent=1.6, factor=3, colors=24)
        return self[n]


k_arm = _ArmCache()
k_tip = {n: MS(lambda: bm.kraken_tip(), n, 32, extent=1.3, factor=3, colors=24) for n in (22, 34)}
PLAT = (240, 128)
WATER = (6, 30, 78)
_sub = {}


def submerged(sp, depth=1.0):
    """Underwater version of a sprite: tinted toward the sea, faded and softened."""
    key = (id(sp), round(depth, 1))
    if key not in _sub:
        a = np.array(sp).astype(np.float64)
        k = 0.35 + 0.45 * depth
        a[..., :3] = a[..., :3] * (1 - k) + np.array(WATER) * k
        a[..., 3] *= 1.0 - 0.45 * depth
        img = Image.fromarray(a.astype(np.uint8), "RGBA").filter(ImageFilter.GaussianBlur(0.8 * depth))
        _sub[key] = (img, sp)        # keep sp alive so id() stays unique
    return _sub[key][0]


def water_frames(n, seed=3):
    """Animated ocean (scrolling periodic noise, ramp from the Earth orbit blues)."""
    a = raster.fbm(FW, FH, 120, seed, octaves=3, period=True)
    b = raster.fbm(FW, FH, 40, seed + 7, octaves=2, period=True)
    stops = [(0.0, (2, 12, 40)), (0.45, (4, 26, 72)), (0.8, (10, 48, 108)), (1.0, (40, 100, 170))]
    out = []
    for i in range(n):
        t = i / FPS
        aa = np.roll(a, (int(t * 6), int(t * 3)), axis=(0, 1))
        bb = np.roll(b, (int(-t * 14), int(t * 9)), axis=(0, 1))
        v = np.clip(0.55 * aa + 0.45 * bb, 0, 1)
        v = (v - 0.2) / 0.7
        rgb = raster.ramp(stops, np.clip(v, 0, 1))
        crest = np.clip((bb - 0.8) * 5, 0, 1)[..., None]
        rgb = rgb * (1 - 0.3 * crest) + np.array([120, 170, 220]) * 0.3 * crest
        rgb = np.round(rgb / 6) * 6                 # fewer distinct tones (smaller GIF)
        out.append(terrain.recede(raster.to_rgba_image(rgb), amount=0.15, darken=0.8))
    return out


def arm_curve(base, h0, length, curl, t, wave=0.15, phase=0.0, sizes=ARM_SIZES):
    """Segment centres and headings along a curling arm from ``base``."""
    pts, hs = [], []
    x, y, h = base[0], base[1], h0
    total = sum(s * 0.55 for s in sizes)
    run = 0.0
    for s in sizes:
        step = s * 0.55
        u = run / total
        h = h0 + curl * u ** 1.4 + wave * np.sin(1.6 * t - 4 * u + phase)
        mx, my = x + np.cos(h) * step / 2, y + np.sin(h) * step / 2
        pts.append((mx, my))
        hs.append(h)
        x, y = x + np.cos(h) * step, y + np.sin(h) * step
        run += s * 0.55
    return pts, hs, (x, y, h)


def draw_arm(img, base, h0, length, curl, t, surfaced=1.0, wave=0.15, phase=0.0, scale=1.0):
    """surfaced: fraction of segments (from the base) that are above water."""
    k = length / (sum(ARM_SIZES) * 0.55)           # arm thickness follows its length
    sizes = [int(np.clip(round(s * k / 2) * 2, 10, 72)) for s in ARM_SIZES]
    pts, hs, (ex, ey, eh) = arm_curve(base, h0, length, curl, t, wave, phase, sizes)
    n = len(sizes)
    for j in range(n - 1, -1, -1):          # tip first, base on top
        sp = k_arm[sizes[j]].get(hs[j])
        above = (j + 1) / n <= surfaced + 1e-6
        sprite.paste_center(img, sp if above else submerged(sp), *pts[j])
    tip = k_tip[34 if k > 1.3 else 22].get(eh)
    sprite.paste_center(img, tip if surfaced >= 0.999 else submerged(tip), ex, ey)
    return pts


def k_player(t):
    return 240 + 62 * np.sin(t * 0.85) + 10 * np.sin(t * 2.0), 492


WRAPPED = [  # base, heading, length, curl, phase (arms coiled over the platform edges)
    ((128, 52), 1.15, 185, -2.4, 0.0),
    ((352, 52), 2.0, 185, 2.4, 1.3),
    ((110, 175), -0.5, 150, 2.0, 2.2),
    ((372, 182), 3.7, 155, -2.1, 3.1),
]
SLAMS = [  # lane x, telegraph start, rise, hold end, sunk
    (128, 0.8, 1.6, 2.4, 2.9),
    (352, 5.9, 6.7, 7.5, 8.0),
    (128, 7.4, 8.2, 8.9, 9.4),
]
HEAD_UP = (2.9, 3.4, 6.0, 6.5)       # rise start, up, sink start, down


def kraken():
    T = 9.6
    waters = water_frames(int(T * FPS))
    plat = static_sprite(bm.platform, (230, 172), 2.3, factor=2, colors=40)
    shots, booms = Shots(), Booms()
    mx, my = 240, 236                         # mantle centre (head south of the platform)
    beak_xy = (mx, my + 1.04 * K_MS)
    for tf in (3.8, 4.7, 5.5):
        px, py = k_player(tf)
        base = heading_to(*beak_xy, px, py)
        for a in np.linspace(-0.6, 0.6, 7):
            shots.fire(tf, beak_xy[0], beak_xy[1], base + a, 165)
    frames = []
    for i, t in enumerate(times(T)):
        f = waters[i].copy()
        d = ImageDraw.Draw(f)
        # head state
        r0, up, s0, down = HEAD_UP
        if t < r0 or t >= down:
            depth = 1.0
        elif t < up:
            depth = 1 - (t - r0) / (up - r0)
        elif t < s0:
            depth = 0.0
        else:
            depth = (t - s0) / (down - s0)
        beak = 1.0 if any(tf - 0.35 <= t < tf + 0.1 for tf in (3.8, 4.7, 5.5)) else 0.0
        bob = 3 * np.sin(t * 1.7)
        if depth > 0.5:
            sp = submerged(kraken_mantle_sprite(0.0, 0.0), 1.0)
            sprite.paste_center(f, sp, mx, my + 10 + bob)
        else:
            sp = kraken_mantle_sprite(1.0, beak)
            if depth > 0:
                sp = submerged(sp, depth * 2)
            # foam ring where the head breaks the surface
            for k in range(3):
                r = 70 + 6 * k + 3 * np.sin(t * 3 + k)
                d.ellipse([mx - r, my + 30 - r * 0.6, mx + r, my + 30 + r * 0.6],
                          outline=(190, 225, 255, 90 - 25 * k), width=2)
            sprite.paste_center(f, sp, mx, my + bob)
        # active arms: telegraph, rise, slam, sink
        for lane, tt, tr, th, ts in SLAMS:
            base = (lane, 200)
            if tt <= t < tr:            # telegraph: the lane lights up, shadow moving below
                if int(t * 10) % 2 == 0:
                    e3.dashed(f, lane, 210, lane, FH, (255, 60, 40), dash=7)
                for k in range(4):
                    yy = 250 + k * 70 + ((t * 60) % 70)
                    d.ellipse([lane - 14, yy - 4, lane + 14, yy + 4], outline=(150, 200, 240, 120))
                draw_arm(f, base, np.pi / 2 + 0.2, 300, -0.5, t, surfaced=0.0, wave=0.25)
            elif tr <= t < ts:
                if t < th:
                    s = min(1.0, (t - tr) / 0.3)
                else:
                    s = max(0.0, 1 - (t - th) / (ts - th))
                pts = draw_arm(f, base, np.pi / 2, 330, -0.25, t, surfaced=s, wave=0.06)
                if tr + 0.3 <= t < tr + 0.75:     # splash along the slammed arm
                    k = (t - tr - 0.3) / 0.45
                    for x, y in pts[::2]:
                        r = 8 + 26 * k
                        d.ellipse([x - r, y - r * 0.5, x + r, y + r * 0.5],
                                  outline=(220, 240, 255, int(200 * (1 - k))), width=2)
        # platform with its wrapped arms
        sprite.paste_center(f, plat, *PLAT)
        for base, h0, ln, curl, ph in WRAPPED:
            draw_arm(f, base, h0, ln, curl, t, surfaced=1.0, wave=0.08, phase=ph, scale=0.8)
        booms.draw(f, t)
        shots.draw(f, t)
        player_draw(f, *k_player(t), t, "orbit")
        frames.append(f)

    # ---- sheet: the full set-up on a dark backdrop
    full = Image.new("RGBA", (440, 520), (8, 20, 52, 255))
    ox, oy = 440 / 2 - 240, -10
    sp = kraken_mantle_sprite(1.0, 1.0)
    sprite.paste_center(full, sp, mx + ox, my + oy)
    sprite.paste_center(full, plat, PLAT[0] + ox, PLAT[1] + oy)
    for base, h0, ln, curl, ph in WRAPPED:
        draw_arm(full, (base[0] + ox, base[1] + oy), h0, ln, curl, 0.0, 1.0, 0.08, ph, 0.8)
    pts = draw_arm(full, (128 + ox, 200 + oy), np.pi / 2, 300, -0.25, 0.0, 1.0, 0.06)
    callouts = [
        ((int(mx + ox + 0.47 * K_MS), int(my + oy + 0.52 * K_MS)), "LIME EYES = WEAK POINTS (ONLY WHILE SURFACED)"),
        ((int(mx + ox), int(my + oy + 1.04 * K_MS)), "BEAK: CRIMSON GLOW BEFORE EACH 7-ORB FAN"),
        ((int(mx + ox), int(my + oy - 0.2 * K_MS)), "MANTLE UNDER THE PLATFORM (MOTTLED RUST SKIN, PAPILLAE)"),
        ((int(PLAT[0] + ox - 60), int(PLAT[1] + oy - 20)), "UTC HARBOUR PLATFORM (HELIPAD, CONTAINERS, CRANE, TORN CORNER)"),
        ((int(128 + ox + 22), int(52 + oy + 44)), "WRAPPED ARMS COIL OVER THE EDGES (SEGMENT CHAINS)"),
        ((int(pts[6][0]), int(pts[6][1])), "SLAM ARM: RISES FROM THE WATER ALONG A LANE, 13 SEGMENTS + TIP"),
        ((int(pts[11][0]) + 6, int(pts[11][1])), "PALE SUCKERS, CRIMSON SEAM (LINE OF DANGER)"),
    ]
    st = panel(3 * 240, 270, (8, 20, 52, 255))
    labels = ("1  SUBMERGED: SHADOW UNDER WAVES", "2  LANE TELEGRAPH -> ARM SLAMS", "3  HEAD UP: EYES OPEN, FANS")
    for k in range(3):
        tmp = Image.new("RGBA", (300, 340), (8, 20, 52, 255))
        if k == 0:
            sprite.paste_center(tmp, submerged(kraken_mantle_sprite(0.0, 0.0), 1.0), 150, 160)
        elif k == 1:
            sprite.paste_center(tmp, submerged(kraken_mantle_sprite(0.0, 0.0), 1.0), 150, 160)
            e3.dashed(tmp, 90, 60, 90, 340, (255, 60, 40), dash=7)
            draw_arm(tmp, (90, 40), np.pi / 2, 300, -0.25, 0.0, 0.55, 0.06)
        else:
            sprite.paste_center(tmp, kraken_mantle_sprite(1.0, 1.0), 150, 140)
            for a in np.linspace(-0.6, 0.6, 7):
                e3.orb(tmp, 150 + np.cos(np.pi / 2 + a) * 60, 140 + 1.04 * K_MS + np.sin(np.pi / 2 + a) * 60, 5)
        tmp = tmp.resize((216, 245), Image.NEAREST)
        st.alpha_composite(tmp, (k * 240 + 8, 22))
        raster.draw_text(st, k * 240 + 8, 6, labels[k], raster.LABEL)
    rise = panel(5 * 70, 200, (8, 20, 52, 255))
    for k, s in enumerate((0.0, 0.25, 0.5, 0.75, 1.0)):
        tmp = Image.new("RGBA", (120, 360), (8, 20, 52, 255))
        draw_arm(tmp, (60, 10), np.pi / 2, 300, -0.25, 0.0, s, 0.06)
        tmp = tmp.resize((60, 180), Image.NEAREST)
        rise.alpha_composite(tmp, (k * 70 + 5, 12))
        raster.draw_text(rise, k * 70 + 8, 2, f"{int(s * 100)}%", raster.LABEL_DIM)
    arm_grid = r5.grid([k_arm[36].frame(k) for k in range(0, 32, 2)], 8, 1)
    eyes = r5.grid([kraken_mantle_sprite(0.0, 0.0), kraken_mantle_sprite(1.0, 1.0)], 2, 1,
                   labels=["EYES SHUT", "EYES OPEN, BEAK LIT"])
    panels = [("PHASES (0.72X)", st), ("ARM RISING (SEGMENTS SURFACE BASE -> TIP), 0.5X", rise),
              ("MANTLE STATES (1X)", eyes), ("ARM SEGMENT: 16 OF 32 HEADINGS (1X)", arm_grid)]
    notes = ["HARBOUR KRAKEN - ACT 2 MID-BOSS, L11 ATLANTIC CONVOY (RUST SKIN, CRIMSON GLOW = SLAMS; LIME WEAK POINTS)",
             "A REAL CEPHALOPOD WRAPPED ROUND AN OFFSHORE UTC PLATFORM: MANTLE AND HEAD 190 PX, EIGHT SUCKER-LINED ARMS BUILT",
             "AS SEGMENT CHAINS (13 SEGMENTS + CURLED TIP, 32 HEADINGS). THE FIRST LOOK AT THE SUB LAYER: SUBMERGED PARTS ARE",
             "TINTED TOWARD THE SEA, FADED AND SOFTENED. A LANE FLASHES RED 0.8 S AHEAD, THEN AN ARM RISES ALONG IT (SEGMENTS",
             "SURFACE FROM BASE TO TIP) AND SLAMS WITH A SPLASH; THE HEAD SURFACES BETWEEN SLAMS, OPENS ITS EYES (WEAK POINTS)",
             "AND FIRES 7-ORB FANS FROM THE BEAK. PHASE 3 IDEA: TWO LANES AT ONCE WHILE THE HEAD STAYS UP."]
    sheet = boss_sheet("HARBOUR KRAKEN - ACT 2 MID-BOSS (VRELL)", full, callouts, panels,
                       frames[int(4.0 * FPS)], notes, "FULL SET-UP 1X (PLATFORM, MANTLE, WRAPPED AND SLAM ARMS)")
    return sheet, frames


# =========================================================================== Siege Spire

SP_N, SP_EXT = 440, 4.2
SP_S = SP_N / SP_EXT
SP_C = (240, 196)
LEAN = (0.0, 0.26)                 # roof pushed away from the screen centre (up)
FREE_N, FREE_EXT = 290, 2.75       # same px/unit as the rooted sprite
_spire = {}


def spire_sprite(key):
    if key not in _spire:
        if key == "alive":
            mk = lambda: bm.spire(maw=0.0, lean=LEAN)
        elif key == "maw":
            mk = lambda: bm.spire(maw=1.0, lean=LEAN)
        elif key == "dead":
            mk = lambda: bm.spire(maw=0.0, lean=LEAN, dead_roots=True)
        elif key == "stump":
            mk = lambda: bm.spire(stump=True)
        _spire[key] = static_sprite(mk, SP_N, SP_EXT, factor=2, colors=48)
    return _spire[key]


free_sprites = {s: MS(lambda laser=0.0, torn=0.0: bm.spire(rooted=False, laser=laser, torn=torn),
                      int(FREE_N * s), 48, extent=FREE_EXT, factor=2, colors=48, sym=6)
                for s in (1.0, 1.1, 1.2, 1.3)}
pod_turret = MS(lambda: bm.root_pod("turret"), 46, 32, extent=1.7, factor=4, colors=32)
pod_mortar = {o: static_sprite(lambda o=o: bm.root_pod("mortar", open_=o), 46, 1.7, factor=4,
                               colors=32) for o in (0.0, 1.0)}
w_sprite = MS(lambda: bm.wraith(), 46, 16, extent=2.2, factor=4, colors=28)


def faded(sp, k):
    """Push a sprite back toward the creep-covered ground (withered, left-behind parts)."""
    a = np.array(sp).astype(np.float64)
    a[..., :3] = a[..., :3] * (1 - k) + np.array([40, 32, 48]) * k
    return Image.fromarray(a.astype(np.uint8), "RGBA")


def cropped(sp, pad=4):
    box = sp.getchannel("A").getbbox()
    if not box:
        return sp
    return sp.crop((max(0, box[0] - pad), max(0, box[1] - pad),
                    min(sp.width, box[2] + pad), min(sp.height, box[3] + pad)))


def pod_xy(i):
    x, y = bm.SPIRE_PODS[i]
    return SP_C[0] + x * SP_S, SP_C[1] - y * SP_S


def crown_xy():
    return SP_C[0] + LEAN[0] * bm.SPIRE_HEIGHT * SP_S, SP_C[1] - LEAN[1] * bm.SPIRE_HEIGHT * SP_S


def creep_ground(seed=8):
    """The capital under the Vrell canopy: night city blocks, ruins, violet creep veins."""
    img, streets = terrain.city_ground(FW, FH, seed=seed, period=False, block=60, street=10)
    arr = np.array(img).astype(np.float64)
    rng = np.random.default_rng(seed)
    d = ImageDraw.Draw(img)
    for _ in range(40):      # building footprints / ruins
        bx, by = rng.integers(0, FW // 60) * 60 + 12, rng.integers(0, FH // 60) * 60 + 12
        w, h = rng.integers(16, 44), rng.integers(16, 44)
        c = int(rng.uniform(34, 58))
        d.rectangle([bx, by, bx + w, by + h], fill=(c, c, c + 12, 255), outline=(20, 20, 28, 255))
    arr = np.array(img).astype(np.float64)
    yy, xx = np.mgrid[0:FH, 0:FW]
    dist = np.sqrt((xx - SP_C[0]) ** 2 + ((yy - SP_C[1]) * 1.15) ** 2)
    n = raster.fbm(FW, FH, 40, seed + 2, octaves=4, period=False)
    creep = np.clip((260 - dist) / 120 + (n - 0.5) * 1.6, 0, 1)
    tissue = np.array([58, 38, 66]) * (0.7 + 0.6 * n)[..., None]
    arr[..., :3] = arr[..., :3] * (1 - creep[..., None] * 0.85) + tissue * creep[..., None] * 0.85
    veins = (np.abs(np.sin(n * 40)) < 0.06) & (creep > 0.3)
    arr[veins, :3] = arr[veins, :3] * 0.4 + np.array([150, 90, 255]) * 0.6
    out = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")
    return terrain.recede(out, amount=0.2, darken=0.75)


def s_player(t):
    if t < 6.2:
        return 240 + 85 * np.sin(t * 0.9), 490
    return 240 + 85 * np.sin(6.2 * 0.9) + (390 - 240 - 85 * np.sin(6.2 * 0.9)) * ease((t - 6.2) / 0.8), 490


def spire_boss():
    T = 10.4
    bg = creep_ground()
    shots, booms, mortars = Shots(), Booms(), []
    die = [3.7 + 0.16 * i for i in range(6)]               # root pods blow up
    lift = (4.9, 6.4)                                       # tears free and rises
    sweep = (7.0, 7.6, 9.6)                                 # telegraph, beam start, beam end
    maw_open = [(1.0, 1.6), (2.6, 3.2)]
    wraiths = []                                            # (t0, path)
    cx0, cy0 = crown_xy()
    for t0, side in ((1.25, -1), (1.4, 1), (2.85, -1), (3.0, 1)):
        path = rig.catmull_rom([(cx0, cy0), (cx0 + side * 70, cy0 - 30),
                                (cx0 + side * 190, cy0 - 40), (cx0 + side * 300, cy0 + 40)],
                               [t0, t0 + 0.5, t0 + 1.0, t0 + 1.6])
        wraiths.append((t0, path))
    for i in range(6):
        x, y = pod_xy(i)
        if i in bm.SPIRE_MORTARS:
            for tm in np.arange(0.9 + 0.5 * i, die[i] - 0.3, 1.5):
                px, py = s_player(tm + 1.1)
                mortars.append((i, tm, x, y, px, py - 8))
        else:
            for ta in np.arange(0.5 + 0.3 * i, die[i] - 0.2, 1.0):
                px, py = s_player(ta)
                shots.aimed(ta, x, y, px, py, 175)
        booms.add(die[i], x, y, 74, seed=30 + i)
    booms.add(lift[0], SP_C[0], SP_C[1], 120, seed=50, ring=True)

    def free_state(t):
        s = ease((t - lift[0]) / (lift[1] - lift[0]))
        y = SP_C[1] - 70 * s + 24 * max(0.0, t - lift[1]) / 4
        return SP_C[0] + 6 * np.sin(t * 1.1), y, s

    def mortar_draw(img, t):
        for _i, tm, sx, sy, tx, ty in mortars:
            if tm <= t < tm + 1.1:
                s = (t - tm) / 1.1
                dd = ImageDraw.Draw(img)
                dd.ellipse([tx - 16, ty - 10, tx + 16, ty + 10], outline=ACID + (255,), width=1)
                bx, by = sx + (tx - sx) * s, sy + (ty - sy) * s - np.sin(np.pi * s) * 110
                e3.orb(img, bx, by, int(4 + 4 * np.sin(np.pi * s)), ring=ACID)
            elif tm + 1.1 <= t < tm + 1.4:
                s = (t - tm - 1.1) / 0.3
                for a in np.linspace(0, TAU, 8, endpoint=False):
                    e3.orb(img, tx + np.cos(a) * 30 * s, ty + np.sin(a) * 22 * s, 3, ring=ACID)

    frames = []
    for t in times(T):
        f = bg.copy()
        px, py = s_player(t)
        # ground: rooted spire (alive / maw / dead) or the torn stump
        if t < lift[0]:
            dead = t >= die[-1] + 0.1
            maw = any(a <= t < b for a, b in maw_open)
            sp = spire_sprite("dead" if dead else ("maw" if maw else "alive"))
            e3.place(f, sp, SP_C[0], SP_C[1], "ground")
            for i in range(6):
                if t >= die[i]:
                    continue
                x, y = pod_xy(i)
                if i in bm.SPIRE_MORTARS:
                    op = 1.0 if any(j == i and tm - 0.3 <= t < tm + 0.1
                                    for j, tm, *_ in mortars) else 0.0
                    e3.place(f, pod_mortar[op], x, y, "ground")
                else:
                    e3.place(f, pod_turret.get(heading_to(x, y, px, py)), x, y, "ground")
        else:
            e3.place(f, faded(spire_sprite("stump"), 0.35), SP_C[0], SP_C[1], "ground")
        # wraiths launched from the maw (rise toward the camera, then fly off)
        for t0, path in wraiths:
            if t0 <= t < t0 + 1.6:
                x, y = path(t)
                h = rig.path_heading(path, t)
                w = w_sprite.get(h)
                g = ease((t - t0) / 0.4)
                if g < 1:
                    w = w.resize((max(4, int(w.width * (0.4 + 0.6 * g))),
                                  max(4, int(w.height * (0.4 + 0.6 * g)))), Image.NEAREST)
                if int(t * 20) % 3 == 0:
                    w = r5.ghost(w, 0.7)            # cloak shimmer
                e3.place(f, w, x, y, "air")
        # airborne spire
        if t >= lift[0]:
            x, y, s = free_state(t)
            size = min((1.0, 1.1, 1.2, 1.3), key=lambda z: abs(z - (1.0 + 0.3 * s)))
            laser = 1.0 if sweep[0] <= t < sweep[2] else 0.3
            rot = 0.0 if t < lift[1] else (t - lift[1]) * 0.2
            torn = (int(t * 5) % 2) * np.pi if t < lift[1] else 0.0
            sp = free_sprites[size].get(np.pi / 2 + rot, laser=laser, torn=torn)
            sh = sprite.shadow_of(sp, opacity=0.55 - 0.15 * s, blur=1.2 + 1.5 * s, scale=0.9)
            sprite.paste_center(f, sh, x + 6 + 34 * s, y + 8 + 48 * s)
            if t < lift[1]:                     # debris falling off the torn base
                rng = np.random.default_rng(int(t * 30))
                dd = ImageDraw.Draw(f)
                for _ in range(10):
                    a = rng.uniform(0, TAU)
                    r = rng.uniform(60, 140) * s
                    dd.rectangle([x + np.cos(a) * r, y + np.sin(a) * r + 20 * s,
                                  x + np.cos(a) * r + 3, y + np.sin(a) * r + 20 * s + 3],
                                 fill=(80, 70, 90, 255))
            sprite.paste_center(f, sp, x, y)
            # laser sweep: telegraph, then a beam turning across the lower screen
            if sweep[0] <= t < sweep[1]:
                a = 2.55
                if int(t * 12) % 2 == 0:
                    e3.dashed(f, x, y, x + np.cos(a) * 600, y + np.sin(a) * 600, (255, 60, 40), dash=7)
            elif sweep[1] <= t < sweep[2]:
                s2 = (t - sweep[1]) / (sweep[2] - sweep[1])
                a = 2.55 - 1.15 * ease(s2 / 0.8)
                ex, ey = x + np.cos(a) * 640, y + np.sin(a) * 640
                e3.beam(f, x, y, ex, ey)
                raster.add_light(f, x, y, 30, (255, 80, 200), 0.8)
        booms.draw(f, t)
        shots.draw(f, t)
        mortar_draw(f, t)
        player_draw(f, px, py, t, "ground")
        frames.append(f)

    # ---- sheet
    full = Image.new("RGBA", (SP_N, SP_N), (0, 0, 0, 0))
    sprite.paste_center(full, spire_sprite("maw"), SP_N / 2, SP_N / 2)
    for i in range(6):
        x, y = bm.SPIRE_PODS[i]
        xx, yy = SP_N / 2 + x * SP_S, SP_N / 2 - y * SP_S
        if i in bm.SPIRE_MORTARS:
            sprite.paste_center(full, pod_mortar[1.0], xx, yy)
        else:
            sprite.paste_center(full, pod_turret.get(heading_to(xx, yy, SP_N / 2, SP_N + 200)), xx, yy)
    ccx, ccy = SP_N / 2 + LEAN[0] * bm.SPIRE_HEIGHT * SP_S, SP_N / 2 - LEAN[1] * bm.SPIRE_HEIGHT * SP_S
    pod = lambda i: (int(SP_N / 2 + bm.SPIRE_PODS[i][0] * SP_S), int(SP_N / 2 - bm.SPIRE_PODS[i][1] * SP_S))
    callouts = [
        ((int(ccx), int(ccy)), "LAUNCH MAW = WEAK POINT (LIME), OPENS TO LAUNCH WRAITHS"),
        ((int(ccx + 0.85 * SP_S * np.cos(0.3)), int(ccy - 0.85 * SP_S * np.sin(0.3))),
         "PETAL CROWN, CRIMSON EMITTER TIPS (LASER PHASE)"),
        ((int(SP_N / 2 + 40), int(SP_N / 2 + 10)), "RIBBED TRUNK, ROOF PUSHED AWAY FROM THE SCREEN CENTRE (TALL STRUCTURE RULE)"),
        (pod(0), "ROOT TURRET POD: AIMS (32 HEADINGS), AIMED ORBS"),
        (pod(1), "ROOT MORTAR POD: LOBS ACID, IMPACT MARKED"),
        ((int(SP_N / 2 + 0.9 * SP_S * np.cos(bm.SPIRE_ROOTS[5])),
          int(SP_N / 2 - 0.9 * SP_S * np.sin(bm.SPIRE_ROOTS[5]))), "SIX ROOTS ACROSS THE CITY BLOCKS (GROUND LAYER)"),
        ((int(SP_N / 2 - 70), int(SP_N / 2 + 60)), "VRELL CREEP MOUND"),
    ]
    st = panel(3 * 270, 300)
    labs = ("1  ROOTED: TURRETS, MORTARS, WRAITHS", "2  ROOTS DEAD: TEARS ITSELF FREE", "3  AIRBORNE: LASER SWEEP")
    for k in range(3):
        tmp = Image.new("RGBA", (440, 480), (22, 24, 34, 255))
        if k == 0:
            sprite.paste_center(tmp, spire_sprite("maw"), 220, 240)
            for i in range(6):
                x, y = bm.SPIRE_PODS[i]
                sp = pod_mortar[0.0] if i in bm.SPIRE_MORTARS else pod_turret.get(np.pi / 2)
                sprite.paste_center(tmp, sp, 220 + x * SP_S, 240 - y * SP_S)
            sprite.paste_center(tmp, w_sprite.get(-0.6), 300, 90)
        elif k == 1:
            sprite.paste_center(tmp, faded(spire_sprite("stump"), 0.35), 220, 260)
            sp = free_sprites[1.1].get(np.pi / 2, laser=0.3, torn=1.0)
            sprite.paste_center(tmp, sprite.shadow_of(sp, 0.45, 2.0, 0.9), 220 + 26, 220 + 36)
            sprite.paste_center(tmp, sp, 220, 200)
        else:
            sp = free_sprites[1.3].get(np.pi / 2 + 0.3, laser=1.0, torn=2.0)
            sprite.paste_center(tmp, sp, 220, 170)
            e3.beam(tmp, 220, 170, 220 + np.cos(2.1) * 400, 170 + np.sin(2.1) * 400)
        tmp = tmp.resize((250, 273), Image.NEAREST)
        st.alpha_composite(tmp, (k * 270 + 8, 22))
        raster.draw_text(st, k * 270 + 8, 6, labs[k], raster.LABEL)
    pods = r5.grid([pod_turret.frame(k) for k in range(0, 32, 4)]
                   + [pod_mortar[0.0], pod_mortar[1.0], w_sprite.get(np.pi / 2)], 11, 2,
                   labels=[""] * 8 + ["SHUT", "LOB", "WRAITH"])
    scales = r5.grid([cropped(free_sprites[s].get(np.pi / 2, laser=0.3, torn=0.0))
                      for s in (1.0, 1.3)], 2, 1, labels=["LIFT-OFF 1.0", "AIRBORNE 1.3"])
    panels = [("PHASES (0.57X)", st), ("ROOT TURRET 8 OF 32 HEADINGS, MORTAR SHUT/LOB, WRAITH (2X)", pods),
              ("FREE SPIRE: DRAWN BIGGER AS IT RISES (1X)", scales)]
    notes = ["SIEGE SPIRE - ACT 2 BOSS, L14 (SLATE TISSUE, VIOLET GLOW = AIMED SHOTS, CRIMSON EMITTERS; LIME WEAK POINTS)",
             "A VRELL CITADEL ROOTED IN THE UTC CAPITAL UNDER A CREEP CANOPY. SIX ROOTS (ABOUT 380 PX ACROSS) END IN FOUR AIMING ROOT",
             "TURRETS AND TWO ACID MORTARS ON THE GROUND LAYER. THE TALL TRUNK IS DRAWN WITH ITS ROOF PUSHED AWAY FROM THE SCREEN",
             "CENTRE; THE PETAL CROWN OPENS ITS MAW (WEAK POINT) TO LAUNCH CLOAKED WRAITHS THAT CIRCLE ROUND FOR REAR AMBUSHES.",
             "WHEN ALL ROOT PODS DIE THE ROOTS WITHER, THE SPIRE TEARS ITSELF OUT (SHOCKWAVE, DEBRIS), RISES - DRAWN 1.0 -> 1.3X",
             "WITH ITS SHADOW SLIDING AWAY - AND SWEEPS A TELEGRAPHED LASER FROM ITS CRIMSON PETAL TIPS ACROSS THE LOWER SCREEN."]
    sheet = boss_sheet("SIEGE SPIRE - ACT 2 BOSS (VRELL)", full, callouts, panels,
                       frames[int(2.9 * FPS)], notes, f"FULL SPRITE 1X ({SP_N}X{SP_N}, ROOT PODS ATTACHED)")
    return sheet, frames


def main(args):
    if not args or "gorgon" in args:
        save("gorgon-frigate", *gorgon())
    if not args or "kraken" in args:
        save("harbour-kraken", *kraken())
    if not args or "spire" in args:
        save("siege-spire", *spire_boss())


if __name__ == "__main__":
    main(sys.argv[1:])
