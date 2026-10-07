#!/usr/bin/env python3
"""Concept round 31 - the Ndidi Arcology's collapse in Level 09 (design/campaign/act-2-homefront/
level-09-arcology-fall, Collapse set piece; user decisions D5 = a and D9 = a of M5 part C: the
look is the round's a/b, everything else is production art).

Outputs (design/campaign/act-2-homefront/level-09-arcology-fall/concept/):
  collapse-r31-a.png / .gif   variant A "topples across": the tower (Level 09's production roof and
                              wall texture) tips over its right foot edge and falls across the play
                              field, drawn with the tower projection (TowerProjection's camera, a box
                              of 200 x 180 x h 1.5 rotated about its hinge); its tip's ground x runs
                              linearly with the simulation's kill band (fall angle asin(s), s = 0..1
                              over 3 s: slow start, a crash at the end); at impact a dust burst along
                              the band, the heavy dust peak, the crushed body left lying across
  collapse-r31-b.png / .gif   variant B "pancakes into a dust wave": the floors give way and the tower
                              sinks into itself in 1.2 s (the projection with a shrinking height, the
                              wall texture losing its storeys from the top), a dust cloud boils out of
                              its foot and a dust wave rolls left to right along the band in 3 s (its
                              front is the kill band), leaving a rubble heap where it stood and debris
                              strewn along the band
  collapse-r31-c.png / .gif   variant C "leans, then collapses into a dust blast" (after the user's
                              verdicts on a and b and on c's first render, 2026-10-07): the warning is
                              a 1.5 s lean to the right, one direction only (0 -> 4 degrees, eased in
                              w^1.6, a bending x offset H sin(lean) (z / H)^2, a small shudder, debris
                              and dust trickling off the walls), the tower's cast shadow (the kit's
                              drop_shadow rule, down and right, at half its length: 0.5 z / SLOPE)
                              attached to it; then a 1.5 s drop straight down in one go (the height
                              eased 0.25 p + 0.75 p^2, the wall texture keeping its rows from the
                              foot, a crushed band at the top, the lean kept and relaxing to 0.65 x
                              4 degrees, the shadow shrinking with the height); dust out of the base
                              0.35 s before the impact (3.0 s after the warning starts), a radial
                              dust blast of lit puff sprites at it, settling over 6 s onto a new
                              rubble heap (280 x 250); the sheet (12 frames) marks the kill ring
                              (radius 100 -> 390 px from the foot's centre in 1 s, ease-out) and the
                              band; the GIF (10.2 s) has no overlay
A and B: the same 1.5 s warning (the tower shudders, its shadow, the band's footprint, sweeps across
the ground from its foot to the right edge), then the 3 s fall, then the `heavy` dust peak ramping
out over 6 s (the production `dust-heavy` banks); the lobby district from Level 09's production
backdrop (tools/art/backdrop_l09.py: its tiles, the lobby plaza, towers; the arcology itself drawn
here) at the hold's 30 px/s, the scroll held at the hold speed through the collapse (an assumption
for the sim step: at 150 px/s the band would cross 675 px of screen during warning and fall).
Each sheet: eight key frames at 1x with their times, the band marked on the first; the GIF plays
the sequence at 10 fps (11 s).

What the renderer needs (written into the sheets as well):
  a: TowerProjection for a tilted box: the 8 corners of the arcology's footprint x height rotated
     about the hinge (the foot's right edge, along the screen's y), projected with the camera
     (6 units, k = 6 / (6 - z)), the visible faces (normals toward the camera) drawn as textured
     quads (the roof image on the roof face, the wall texture on the walls, shade by face), the fall
     angle per frame; at impact the tower is swapped for the rubble piece (the crushed body), a dust
     burst sheet along the band (about 6 frames, 480 x 220, additive-free alpha), the heavy peak.
  b: TowerProjection with a height animated from 1.5 to about 0.1 over 1.2 s (the roof at the
     current scale, the wall texture cropped from its top rows), a dust cloud at the foot (about 6
     frames, 240 x 240), the dust wave: an animated sheet (about 8 frames, 180 x 260) moved left to
     right with the band's swept edge, the rubble heap piece at the foot and a debris strip piece
     along the band, the heavy peak.

  c: see the collapse-r31-c entry in the level's concept/prompts.md (lean, drop, shadow, puff sprites
     and particle counts, the heap, the timings).

Run: python3 tools/concept/collapse_r31.py [a] [b] [c]   (about 2 min for a and b, 30 s for c)
"""
import math
import sys
from pathlib import Path

import numpy as np
from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
sys.path.insert(0, str(HERE.parent / "art"))
import backdrop_l09 as l9  # noqa: E402  (Level 09's production backdrop, its composite)
from render import raster  # noqa: E402
from render.config import ROOT  # noqa: E402
from render.enemy_rigs import write_gif  # noqa: E402

OUT = ROOT / "design" / "campaign" / "act-2-homefront" / "level-09-arcology-fall" / "concept"
ASSETS = ROOT / "assets" / "backdrop" / "level-09"
W, SCREEN = 480, 540
CX, CY = 240.0, SCREEN - 297.0          # the projection centre, y up from the bottom
UNIT = 267.0                            # px per camera unit: the lying tower spans about 400 px
CAM = 6 * UNIT
FOOT = l9.ARCOLOGY["foot"]
HEIGHT = l9.ARCOLOGY["height"] * UNIT   # 400 px
HOLD = 30.0                             # px/s: the scroll through the collapse (the hold's speed)
FPS = 10
STAND, WARN, FALL, DUST = 0.8, 1.5, 3.0, 6.0
CLIP = STAND + WARN + FALL + DUST - 0.3
FALL_AT = STAND + WARN
KEY = np.array([-0.55, 0.6, 0.75])
KEY = KEY / np.linalg.norm(KEY)


def load(name):
    return np.array(Image.open(ASSETS / f"{name}.png").convert("RGBA")).astype(np.float64)


ROOF = load("arcology")
WALL = load("wall-arcology-hollow")
DUST_TILE = load("dust-heavy")
# The provisional rubble of A and B, gone since c went to production (tools/art/collapse_l09.py writes
# the heap); A and B need it back (git history) to be re-run, C and the production heap do not.
RUBBLE = load("arcology-rubble") if (ASSETS / "arcology-rubble.png").exists() else None


# --------------------------------------------------------------------------- the scene

class Scene:
    """The lobby district during the collapse: the backdrop without the arcology, the scroll at the
    hold's speed, the foot's centre about 260 px below the top edge as the fall starts."""

    def __init__(self):
        self.backdrop, _ = l9.level_backdrop()
        self.placed = [p for p in l9.k8.expanded(self.backdrop) if p["piece"] != "arcology"]
        self.ax, self.apos = l9.arcology_place()
        self.scroll0 = self.apos - (SCREEN - 260)          # the ground scroll as the fall starts

    def scroll(self, rt):
        return self.scroll0 + HOLD * (rt - FALL_AT)

    def ground(self, rt):
        s = self.scroll(rt)
        img = l9.k8.composite(self.backdrop, s / l9.SPEED, self.placed)
        return np.array(img.convert("RGBA")).astype(np.float64)

    def foot_y(self, rt):
        """The foot's centre, y up from the bottom of the screen."""
        return self.apos - self.scroll(rt)


def project(p):
    """Camera projection of points (x, y up, z) to screen (x, y up)."""
    k = CAM / (CAM - p[..., 2])
    return np.stack([CX + (p[..., 0] - CX) * k, CY + (p[..., 1] - CY) * k], axis=-1)


def box_faces(x0, x1, y0, y1, height):
    """The tower's faces: (origin, axis a, axis b, outward normal, texture kind)."""
    o = np.array
    return [
        (o([x0, y0, height]), o([x1 - x0, 0, 0]), o([0, y1 - y0, 0]), o([0, 0, 1.0]), "roof"),
        (o([x0, y0, 0]), o([x1 - x0, 0, 0]), o([0, 0, height]), o([0, -1.0, 0]), "wall"),
        (o([x1, y1, 0]), o([x0 - x1, 0, 0]), o([0, 0, height]), o([0, 1.0, 0]), "wall"),
        (o([x0, y1, 0]), o([0, y0 - y1, 0]), o([0, 0, height]), o([-1.0, 0, 0]), "wall"),
        (o([x1, y0, 0]), o([0, y1 - y0, 0]), o([0, 0, height]), o([1.0, 0, 0]), "wall"),
    ]


def rotate(p, hinge_x, theta):
    """Points tipped by ``theta`` about the hinge (x = hinge_x, z = 0, along y), toward +x."""
    q = np.array(p, float)
    dx, z = q[..., 0] - hinge_x, q[..., 2]
    q[..., 0] = hinge_x + dx * math.cos(theta) + z * math.sin(theta)
    q[..., 2] = -dx * math.sin(theta) + z * math.cos(theta)
    return q


def draw_tower(frame, x0, x1, y0, y1, height, theta=0.0, squash=1.0, wall_rows=1.0, dark=1.0):
    """The tower as a textured box (point-splatted at half-pixel steps), tipped ``theta`` about its
    right foot edge; ``squash`` flattens it after impact; ``wall_rows`` the share of the wall
    texture's storeys left (the pancake); ``dark`` dims it (the crushed body)."""
    cam = np.array([CX, CY, CAM])
    th, tw = WALL.shape[:2]
    rh, rw = ROOF.shape[:2]
    for origin, a, b, n, kind in box_faces(x0, x1, y0, y1, height):
        la, lb = np.linalg.norm(a), np.linalg.norm(b)
        ua = np.arange(0, 1, 0.5 / max(la, 1)) + 0.25 / max(la, 1)
        ub = np.arange(0, 1, 0.5 / max(lb, 1)) + 0.25 / max(lb, 1)
        A, Bm = np.meshgrid(ua, ub)
        pts = origin + A[..., None] * a + Bm[..., None] * b
        pts = rotate(pts, x1, theta)
        pts[..., 2] *= squash
        nn = rotate(n[None, :] + np.array([x1, 0, 0]), x1, theta)[0] - rotate(np.array([[x1, 0, 0]]), x1, theta)[0]
        centre = pts.reshape(-1, 3).mean(axis=0)
        if np.dot(nn, cam - centre) <= 0:
            continue
        light = (0.5 + 0.6 * max(0.0, float(np.dot(nn, KEY)))) * dark
        if kind == "roof":
            col = ROOF[np.clip(((1 - Bm) * rh).astype(int), 0, rh - 1), np.clip((A * rw).astype(int), 0, rw - 1)]
        else:
            along = A * la
            up = Bm * wall_rows
            col = WALL[np.clip(((1 - up) * th).astype(int), 0, th - 1), (along % tw).astype(int)]
        scr = project(pts)
        xs = np.round(scr[..., 0]).astype(int)
        ys = (SCREEN - 1 - np.round(scr[..., 1])).astype(int)
        ok = (xs >= 0) & (xs < W) & (ys >= 0) & (ys < SCREEN) & (col[..., 3] > 0)
        frame[ys[ok], xs[ok], :3] = np.clip(col[ok][:, :3] * light, 0, 255)


def blend(frame, layer, alpha):
    a = layer[..., 3:4] / 255 * alpha
    frame[..., :3] = frame[..., :3] * (1 - a) + layer[..., :3] * a


def band_shadow(frame, y0, y1, x_from, x_to, alpha):
    """The warning: the band's footprint darkened from the foot to ``x_to`` (rows y0..y1, y up)."""
    r0, r1 = int(max(0, SCREEN - y1)), int(min(SCREEN, SCREEN - y0))
    c0, c1 = int(max(0, x_from)), int(min(W, x_to))
    if r1 > r0 and c1 > c0:
        frame[r0:r1, c0:c1, :3] *= 1 - alpha


def dust_sheet(shape, seed, y0, y1, x_from, x_to, density, front=None):
    """A pale dust billow over the band (rows y0..y1, y up) from ``x_from`` to ``x_to``: fbm-shaped,
    denser toward ``front`` (a wave's leading edge) when given."""
    h, w = shape
    n = raster.fbm(w, h, 24, seed, octaves=4, period=False)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64)
    yu = SCREEN - 1 - yy
    mid, half = (y0 + y1) / 2, (y1 - y0) / 2 + 40
    across = np.clip(1 - np.abs(yu - mid) / half, 0, 1)
    along = ((xx >= x_from) & (xx <= x_to)).astype(float)
    if front is not None:
        along *= np.clip(1 - (front - xx) / 160, 0.15, 1) * np.clip((front + 30 - xx) / 30, 0, 1)
    a = np.clip((n * 1.3 + across * 0.9 - 0.9) * density, 0, 1) * along * np.clip(across * 3, 0, 1)
    a = np.round(a * 5) / 5
    lit = np.clip(0.55 + (n - 0.5) * 1.4 + (xx / w) * 0.25, 0, 1)
    rgb = np.stack([90 + 90 * lit, 86 + 84 * lit, 84 + 78 * lit], axis=-1)
    return np.dstack([rgb, a * 255])


def dust_peak(rt):
    """The heavy peak's weight: up over 0.6 s at impact (a) or as the wave runs (b), out over 6 s."""
    t = rt - (FALL_AT + FALL)
    if t < -FALL:
        return 0.0
    if t < 0:
        return 0.6 * (t + FALL) / FALL
    s = np.clip(t / DUST, 0, 1)
    return float(0.6 + 0.4 * (1 - s) - 0.6 * s * s * (3 - 2 * s)) if s < 1 else 0.0


def frame_at(scene, rt, variant):
    frame = scene.ground(rt)
    fy = scene.foot_y(rt)
    x0, x1 = scene.ax - FOOT[0] / 2, scene.ax + FOOT[0] / 2
    y0, y1 = fy - FOOT[1] / 2, fy + FOOT[1] / 2
    jitter = 0.0
    if STAND <= rt < FALL_AT:                                                       # the warning
        w_ = (rt - STAND) / WARN
        jitter = (1 if int(rt * 20) % 2 else -1) * (1 + 2 * w_)
        band_shadow(frame, y0, y1, x1, x1 + (W + 60 - x1) * w_, 0.35 + 0.15 * math.sin(rt * 18))
    s = np.clip((rt - FALL_AT) / FALL, 0, 1)
    rub = np.zeros_like(frame)
    if variant == "a":
        if s < 1:
            theta = math.asin(s)
            if s > 0:
                band_shadow(frame, y0, y1, x1, x1 + HEIGHT * s, 0.35)
            draw_tower(frame, x0 + jitter, x1 + jitter, y0, y1, HEIGHT, theta)
        else:
            impact = rt - (FALL_AT + FALL)
            l9.k8.blit(rub, RUBBLE, 0, int(round(fy - RUBBLE.shape[0] / 2)))
            blend(frame, rub, 0.9)
            draw_tower(frame, x0, x1, y0 + 8, y1 - 8, HEIGHT, math.pi / 2, squash=0.22, dark=0.55)
            burst = dust_sheet(frame.shape[:2], 3101, y0, y1, x1 - 40, W + 40, 1.6 - min(1.2, impact * 0.25))
            blend(frame, burst, 1.0)
    else:
        if s <= 0:
            draw_tower(frame, x0 + jitter, x1 + jitter, y0, y1, HEIGHT)
        else:
            sink = np.clip(s * FALL / 1.2, 0, 1)
            hgt = HEIGHT * (1 - 0.9 * sink ** 1.6)
            front = x1 + (W + 80 - x1) * s
            debris = np.zeros_like(frame)
            l9.k8.blit(debris, RUBBLE, 0, int(round(fy - RUBBLE.shape[0] / 2)))
            debris[:, int(min(W, max(0, front))):, 3] = 0
            debris[:, :int(max(0, x1)), 3] *= 0.3
            blend(frame, debris, 0.85 * min(1.0, s * 3))
            draw_tower(frame, x0 - 6, x1 + 6, y0 - 6, y1 + 6, HEIGHT * 0.1 * sink, wall_rows=0.12, dark=0.6)
            if sink < 1:
                draw_tower(frame, x0, x1, y0, y1, hgt, wall_rows=hgt / HEIGHT)
            cloud = dust_sheet(frame.shape[:2], 3103, y0 - 30, y1 + 30, x0 - 20, x1 + 30, 1.3 * min(1.0, max(0.0, sink - 0.55) * 2.5))
            blend(frame, cloud, 1.0)
            if s < 1:
                wave = dust_sheet(frame.shape[:2], 3105, y0, y1, x1 - 20, front + 30, 2.0, front=front)
                blend(frame, wave, 1.0)
        if s >= 1:
            settle = dust_sheet(frame.shape[:2], 3107, y0, y1, x0, W + 40, 1.4 - min(1.1, (rt - FALL_AT - FALL) * 0.22))
            blend(frame, settle, 1.0)
    peak = dust_peak(rt)
    if peak > 0:
        rows = (int(scene.scroll(rt) * 1.35) + (SCREEN - 1 - np.arange(SCREEN))) % DUST_TILE.shape[0]
        tile = DUST_TILE[DUST_TILE.shape[0] - 1 - rows]
        blend(frame, tile, peak)
    return Image.fromarray(np.clip(frame[..., :3], 0, 255).astype(np.uint8), "RGB"), (y0, y1, x1)


TEXT = {
    "a": ("A - TOPPLES ACROSS",
          ["THE TOWER TIPS OVER ITS RIGHT FOOT EDGE AND FALLS ACROSS THE FIELD, DRAWN WITH THE TOWER",
           "PROJECTION; ITS TIP'S GROUND X RUNS WITH THE KILL BAND (ANGLE ASIN(S)); AT IMPACT A DUST BURST,",
           "THE CRUSHED BODY LEFT LYING ACROSS.  RENDERER: A TILTED-BOX TOWERPROJECTION (8 CORNERS ROTATED",
           "ABOUT THE HINGE, VISIBLE FACES AS TEXTURED QUADS), THE RUBBLE PIECE AT IMPACT, A DUST-BURST SHEET."]),
    "b": ("B - PANCAKES INTO A DUST WAVE",
          ["THE FLOORS GIVE WAY: THE TOWER SINKS INTO ITSELF IN 1.2 S, A DUST CLOUD BOILS OUT OF ITS FOOT AND",
           "A DUST WAVE ROLLS ALONG THE BAND IN 3 S (ITS FRONT IS THE KILL BAND), LEAVING A HEAP AND DEBRIS.",
           "RENDERER: TOWERPROJECTION WITH AN ANIMATED HEIGHT (WALL ROWS CROPPED FROM THE TOP), A FOOT-CLOUD",
           "SHEET, A DUST-WAVE SHEET MOVED WITH THE BAND'S EDGE, A HEAP PIECE AND A DEBRIS STRIP PIECE."]),
}
KEYS = [(0.5, "STANDING (NODES C DEAD)"), (1.6, "WARNING: SHUDDER, SHADOW"), (2.2, "THE SHADOW ACROSS"),
        (3.1, "FALL 0.8 S"), (4.1, "FALL 1.8 S"), (5.1, "FALL 2.8 S"), (6.0, "IMPACT + 0.7 S"), (9.5, "DUST RAMPING OUT")]


def build(variant):
    scene = Scene()
    shots = []
    keys = KEYS if variant == "a" else KEYS[:3] + [(2.75, "PANCAKING 0.45 S")] + KEYS[4:]
    for rt, label in keys:
        img, band = frame_at(scene, rt, variant)
        shots.append((img, label, rt, band))
    cols = 4
    width = 16 + cols * (W + 12)
    sheet = raster.sheet(width, 110 + 2 * (SCREEN + 30) + 10, f"LEVEL 09 COLLAPSE - {TEXT[variant][0]} (CONCEPT R31 {variant.upper()})",
                         "1.5 S WARNING, 3 S FALL, HEAVY DUST OVER 6 S; THE SCROLL AT THE HOLD'S 30 PX/S; 1X")
    for i, line in enumerate(TEXT[variant][1]):
        raster.draw_text(sheet, 16, 36 + i * 11, line, raster.LABEL if i < 2 else raster.LABEL_DIM)
    for k, (img, label, rt, (y0, y1, x1)) in enumerate(shots):
        x, y = 16 + (k % cols) * (W + 12), 100 + (k // cols) * (SCREEN + 30)
        im = img.convert("RGBA")
        if k == 0:
            from PIL import ImageDraw
            d = ImageDraw.Draw(im)
            d.rectangle([x1, SCREEN - y1, W - 1, SCREEN - y0], outline=(255, 190, 60, 255))
            raster.draw_text(im, int(x1) + 6, int(SCREEN - y1) + 4, "KILL BAND", (255, 190, 60))
        sheet.alpha_composite(im, (x, y + 12))
        raster.draw_text(sheet, x, y, f"T+{rt:.1f} S  {label}", raster.LABEL)
    png = OUT / f"collapse-r31-{variant}.png"
    sheet.convert("RGB").save(png, optimize=True)
    frames = [frame_at(scene, float(rt), variant)[0] for rt in np.arange(0, CLIP, 1 / FPS)]
    gif = png.with_suffix(".gif")
    write_gif(frames, gif, fps=FPS, colors=128)
    print(f"{png.relative_to(ROOT)}, {gif.relative_to(ROOT)} {gif.stat().st_size / 1e6:.1f} MB")


# --------------------------------------------------------------------------- variant C (after the round's verdict)
# The user's verdict on a and b (2026-10-07): the tower sways a little, then collapses straight down;
# its shadow stays attached to it; dust in all directions from just before the impact, a big dust
# explosion at the impact. Times are from the clip's start; the warning (the lean) starts at C_WARN.
# The user's verdict on c's first render (2026-10-07): the shadow about half as large; no swaying back
# and forth, the tower leans a bit to the right and then drops in one go; the dust unchanged.

C_WARN, C_LEANING, C_DROP = 0.8, 1.5, 1.5        # the lean (the gameplay warning), then the drop
C_FALL = C_WARN + C_LEANING                         # the drop starts (2.3 s, as a/b's FALL_AT)
C_IMPACT = C_FALL + C_DROP                       # the impact: 3.0 s after the warning starts
C_DUST = C_IMPACT - 0.35                         # the base dust starts
C_CLIP = C_IMPACT + DUST + 0.4                   # the dust ramps out over 6 s after the impact
C_LEAN = math.radians(4.0)                       # the lean at the warning's end, to the right
C_STUMP = 0.0                                    # the height left at the impact (the heap takes over)
C_KILL = (100.0, 390.0, 1.0)                     # the kill ring: from the foot's edge to the band's far corner in 1 s
PUFF = 128                                       # the puff sprites' size, px
SHADOW_D = np.array([-KEY[0], -KEY[1]]) / math.hypot(KEY[0], KEY[1])   # down and right (y up)
SHADOW_SLOPE = KEY[2] / math.hypot(KEY[0], KEY[1])                     # the kit's SLOPE: px of height per px of shadow
SHADOW_SCALE = 0.5                               # the user (2026-10-07): the shadow about half as long as the kit's rule


def c_lean(rt):
    """The tower's lean (radians, + to the right) and shudder (px at the roof) at clip time ``rt``:
    one direction only, growing over the warning (eased in, w^1.6), then kept and relaxing to 0.65 x
    through the drop."""
    if rt < C_WARN:
        return 0.0, 0.0
    if rt < C_FALL:
        w_ = (rt - C_WARN) / C_LEANING
        return C_LEAN * w_ ** 1.6, 0.3 + 0.5 * w_
    p = min(1.0, (rt - C_FALL) / C_DROP)
    return C_LEAN * (1 - 0.35 * p), 0.8 * (1 - p)


def c_height(rt):
    """The tower's height px: the roof drops with a gravity-like ease (slow start, fast end)."""
    if rt < C_FALL:
        return HEIGHT
    p = min(1.0, (rt - C_FALL) / C_DROP)
    return HEIGHT * (1 - (1 - C_STUMP) * (0.25 * p + 0.75 * p * p))


def c_offset(z, lean):
    """The lean's bending: a point ``z`` px up is moved sideways by H sin(lean) (z / H)^2."""
    return HEIGHT * math.sin(lean) * (z / HEIGHT) ** 2


def c_jitter(rt, amount):
    """The shudder: a few px of high-frequency shake at the roof, seeded per 1/20 s."""
    if amount <= 0:
        return 0.0, 0.0
    r = np.random.default_rng(int(rt * 20) + 7001)
    return float(r.uniform(-1, 1) * amount), float(r.uniform(-1, 1) * amount * 0.5)


def draw_tower_c(frame, x0, x1, y0, y1, h, lean, jx, jy, crush, tint):
    """The tower as a textured box (as draw_tower), bent by the lean (c_offset) and shaken by the
    shudder (jx, jy scaled by z / H); walls first, then the roof, shaded as TowerProjection shades
    them (walls facing right or down at the arcology's 0.5); the wall texture keeps its rows from the
    foot (the storeys above ``h`` gone); ``crush`` px below the roof's edge the walls break up
    (darker, dusty); ``tint`` greys the roof with dust."""
    if h <= 1:
        return
    cam = np.array([CX, CY, CAM])
    th, tw = WALL.shape[:2]
    rh, rw = ROOF.shape[:2]
    faces = box_faces(x0, x1, y0, y1, h)
    noise = raster.fbm(64, 64, 8, 3131, octaves=3, period=True)
    for origin, a, b, n, kind in faces[1:] + faces[:1]:
        la, lb = np.linalg.norm(a), np.linalg.norm(b)
        ua = np.arange(0, 1, 0.5 / max(la, 1)) + 0.25 / max(la, 1)
        ub = np.arange(0, 1, 0.5 / max(lb, 1)) + 0.25 / max(lb, 1)
        A, Bm = np.meshgrid(ua, ub)
        pts = origin + A[..., None] * a + Bm[..., None] * b
        centre = pts.reshape(-1, 3).mean(axis=0)
        if np.dot(n, cam - centre) <= 0:
            continue
        z = pts[..., 2]
        pts[..., 0] += np.array([c_offset(zz, lean) for zz in z.ravel()]).reshape(z.shape) + jx * z / HEIGHT
        pts[..., 1] += jy * z / HEIGHT
        if kind == "roof":
            col = ROOF[np.clip(((1 - Bm) * rh).astype(int), 0, rh - 1), np.clip((A * rw).astype(int), 0, rw - 1)].copy()
            light = 1.0
            if tint > 0:
                nz = noise[(Bm * 63).astype(int), (A * 63).astype(int)]
                g = np.clip(tint * (0.6 + 0.8 * nz), 0, 1)[..., None]
                col[..., :3] = col[..., :3] * (1 - g) + np.array([118, 112, 110]) * g
        else:
            along = A * la
            col = WALL[np.clip(((1 - z / HEIGHT) * th).astype(int), 0, th - 1), (along % tw).astype(int)].copy()
            light = 0.5 if (n[0] > 0 or n[1] < 0) else 1.0
            if crush > 0:
                nz = noise[(z * 0.5).astype(int) % 64, (along * 0.5).astype(int) % 64]
                depth = (h - z) / crush
                broken = depth < 0.35 + 0.9 * nz
                g = np.clip(1.2 - depth, 0, 1) * broken
                col[..., :3] = col[..., :3] * (1 - 0.75 * g[..., None]) + np.array([96, 92, 92]) * (0.55 * g[..., None])
        scr = project(pts)
        xs = np.round(scr[..., 0]).astype(int)
        ys = (SCREEN - 1 - np.round(scr[..., 1])).astype(int)
        ok = (xs >= 0) & (xs < W) & (ys >= 0) & (ys < SCREEN) & (col[..., 3] > 0)
        frame[ys[ok], xs[ok], :3] = np.clip(col[ok][:, :3] * light, 0, 255)


def tower_shadow(frame, x0, x1, y0, y1, h, lean, opacity=0.4):
    """The tower's cast shadow, the kit's rule (drop_shadow) at half its length (SHADOW_SCALE): the
    key light from the top left, each point z px up shadowed 0.5 z / SLOPE px down and right; the footprint swept up the bent tower (40
    slices), softened (a penumbra of a few px), darkening the ground below the tower."""
    from PIL import ImageDraw, ImageFilter
    f = 2
    mask = Image.new("L", (W * f, SCREEN * f), 0)
    d = ImageDraw.Draw(mask)
    for z in np.linspace(0, h, 40):
        dx = c_offset(z, lean) + SHADOW_D[0] * SHADOW_SCALE * z / SHADOW_SLOPE
        dy = SHADOW_D[1] * SHADOW_SCALE * z / SHADOW_SLOPE
        d.rectangle([(x0 + dx) * f, (SCREEN - (y1 + dy)) * f, (x1 + dx) * f, (SCREEN - (y0 + dy)) * f], fill=255)
    mask = mask.filter(ImageFilter.GaussianBlur(3 * f)).resize((W, SCREEN), Image.BILINEAR)
    m = np.array(mask).astype(np.float64) / 255
    frame[..., :3] *= (1 - opacity * m)[..., None]


# --- dust: puff sprites (lit soft balls of dust) and their particles

def puff_bank(n=8, size=PUFF, seed=3141):
    """``n`` puff sprites: a cluster of 10-18 small balls (a height field, each ball a hemisphere)
    with a fine cauliflower bump, under a wide soft haze; lit by the key light from the top left
    (pale grey on the top left, navy grey below, little contrast: dust at night), the edge eroded by
    noise into a soft, irregular fringe (no hard rim). Variants 0-3 billowy, 4-7 hazier. Float RGBA
    (rgb 0..255, a 0..1)."""
    out = []
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float64) + 0.5
    for v in range(n):
        rng = np.random.default_rng(seed + v)
        hazy = v >= n // 2
        hf = np.zeros((size, size))
        for _ in range(rng.integers(10, 19)):
            cx, cy = rng.normal(size / 2, size * 0.12, 2)
            r = size * rng.uniform(0.07, 0.17)
            hf = np.maximum(hf, np.sqrt(np.clip(r * r - (xx - cx) ** 2 - (yy - cy) ** 2, 0, None)))
        bump = raster.fbm(size, size, 16, seed + 50 + v, octaves=4, period=False)
        hf = hf + (hf > 0) * size * 0.04 * bump
        hf = np.array(Image.fromarray((hf / max(hf.max(), 1e-6) * 255).astype(np.uint8)).filter(
            __import__("PIL.ImageFilter").ImageFilter.GaussianBlur(size * (0.035 if hazy else 0.012)))).astype(np.float64) / 255
        d = np.hypot(xx - size / 2, yy - size / 2) / (size / 2)
        haze = np.clip(1 - d, 0, 1) ** 1.5
        gy, gx = np.gradient(hf * size * 0.3)
        nx, ny, nz = -gx, gy, np.ones_like(hf)
        nn = np.sqrt(nx * nx + ny * ny + nz * nz)
        lam = np.clip((nx * KEY[0] + ny * KEY[1] + nz * KEY[2]) / nn, 0, 1)
        shade = np.clip(0.3 + 0.75 * lam * (0.6 + 0.4 * hf), 0, 1)
        lit_c, dark_c = np.array([158, 151, 146]), np.array([62, 63, 80])
        rgb = dark_c + (lit_c - dark_c) * shade[..., None]
        ero = raster.fbm(size, size, 10, seed + 90 + v, octaves=4, period=False)
        body = hf * (0.7 if hazy else 1.0) + haze * (0.55 if hazy else 0.25)
        a = np.clip((body * 1.5 + (ero - 0.5) * 0.8 - 0.12) / 0.7, 0, 1)
        a = a * a * (3 - 2 * a) * (0.8 if hazy else 0.95)
        a = np.where(body > 0.06, a, 0.0)
        out.append(np.dstack([rgb, a]))
    return out


PUFFS = None
_PUFF_CACHE = {}


def puff_sprite(v, px):
    key = (v, px)
    if key not in _PUFF_CACHE:
        src = PUFFS[v]
        img = Image.fromarray(np.dstack([np.clip(src[..., :3], 0, 255), src[..., 3:] * 255]).astype(np.uint8), "RGBA")
        _PUFF_CACHE[key] = np.array(img.resize((px, px), Image.BILINEAR)).astype(np.float64)
    return _PUFF_CACHE[key]


def stamp(frame, v, sx, sy, scale, alpha):
    """Puff ``v`` centred at screen (sx, sy up) at ``scale`` x its 128 px, faded by ``alpha``."""
    px = max(3, int(round(PUFF * scale)))
    if alpha <= 0.01 or px < 3:
        return
    spr = puff_sprite(v, px)
    left, top = int(round(sx - px / 2)), int(round(SCREEN - sy - px / 2))
    r0, r1, c0, c1 = max(0, top), min(SCREEN, top + px), max(0, left), min(W, left + px)
    if r1 <= r0 or c1 <= c0:
        return
    s = spr[r0 - top:r1 - top, c0 - left:c1 - left]
    a = s[..., 3:4] / 255 * alpha
    frame[r0:r1, c0:c1, :3] = frame[r0:r1, c0:c1, :3] * (1 - a) + s[..., :3] * a


def ease_out(t):
    t = min(1.0, max(0.0, t))
    return 1 - (1 - t) ** 2


def fade(t, life, peak, fade_in=0.15):
    if t < 0 or t > life:
        return 0.0
    return peak * min(1.0, t / fade_in) * (1 - t / life) ** 1.25


def rect_reach(ang, hw, hh):
    """From the footprint's centre to its edge along ``ang``."""
    c, s = abs(math.cos(ang)), abs(math.sin(ang))
    return min(hw / max(c, 1e-6), hh / max(s, 1e-6))


class Particles:
    """Every particle of variant C, seeded: positions relative to the footprint's centre on the
    ground (px, y up), so they scroll with the ground."""

    def __init__(self):
        rng = np.random.default_rng(3161)
        hw, hh = FOOT[0] / 2, FOOT[1] / 2
        self.trickle, self.wisps, self.chunks, self.base, self.ring, self.billow, self.thrown = [], [], [], [], [], [], []
        for _ in range(46):                   # debris trickling off the walls during the lean and the drop
            t0 = rng.uniform(C_WARN + 0.15, C_IMPACT - 0.15)
            side = rng.choice(["e", "n", "s", "e"])
            along = rng.uniform(-1, 1)
            x, y = {"e": (hw + 2, along * hh), "n": (along * hw, hh + 2), "s": (along * hw, -hh - 2)}[side]
            out = {"e": (1, 0), "n": (0, 1), "s": (0, -1)}[side]
            self.trickle.append(dict(t0=t0, x=x, y=y, frac=rng.uniform(0.35, 0.98), v=rng.uniform(8, 30), out=out,
                                     puff=int(rng.integers(8)), size=rng.uniform(0.08, 0.14)))
        for _ in range(16):                   # dust wisps off the walls (small, during the lean)
            t0 = rng.uniform(C_WARN + 0.3, C_FALL + 0.6)
            side = rng.choice(["e", "n", "s"])
            along = rng.uniform(-0.9, 0.9)
            x, y = {"e": (hw + 3, along * hh), "n": (along * hw, hh + 3), "s": (along * hw, -hh - 3)}[side]
            out = {"e": (1, 0), "n": (0, 1), "s": (0, -1)}[side]
            self.wisps.append(dict(t0=t0, x=x, y=y, frac=rng.uniform(0.45, 0.95), out=out, puff=int(rng.integers(8)),
                                   size=rng.uniform(0.08, 0.15)))
        for _ in range(60):                   # chunks spat out of the crushing floors during the drop
            t0 = rng.uniform(C_FALL + 0.15, C_IMPACT - 0.05)
            ang = rng.uniform(0, 2 * math.pi)
            self.chunks.append(dict(t0=t0, ang=ang, v=rng.uniform(40, 130), tone=rng.uniform(0.25, 0.55)))
        for _ in range(44):                   # the base dust: out of the foot from just before the impact
            t0 = rng.uniform(C_DUST, C_IMPACT)
            ang = rng.uniform(0, 2 * math.pi)
            self.base.append(dict(t0=t0, ang=ang, r0=rect_reach(ang, hw, hh) * rng.uniform(0.6, 1.0), d=rng.uniform(30, 150),
                                  s0=rng.uniform(0.25, 0.4), s1=rng.uniform(0.8, 1.2), life=rng.uniform(3.0, 4.5),
                                  peak=rng.uniform(0.55, 0.75), puff=int(rng.integers(8))))
        for i in range(200):                  # the blast: a ring of puffs rolling out in every direction (130 at
            n_ = 130 if i < 130 else 70       # its front, 70 filling in behind it)
            ang = ((i % n_) + rng.uniform(-0.45, 0.45)) * 2 * math.pi / n_
            r0 = rect_reach(ang, hw, hh)
            lobes = 1 + 0.16 * math.sin(3 * ang + 0.7) + 0.1 * math.sin(5 * ang + 2.1) + 0.06 * math.sin(9 * ang + 4.0)
            reach = (rng.uniform(0.62, 1.05) if i < 130 else rng.uniform(0.2, 0.6)) * lobes
            self.ring.append(dict(ang=ang, r0=r0 * rng.uniform(0.7, 1.0), d=reach * (C_KILL[1] - C_KILL[0]),
                                  drift=rng.uniform(20, 70), s0=rng.uniform(0.35, 0.6), s1=rng.uniform(0.9, 1.7),
                                  life=rng.uniform(2.6, 4.6), peak=rng.uniform(0.5, 0.7) if i < 130 else rng.uniform(0.35, 0.5),
                                  puff=int(rng.integers(8)), lag=rng.uniform(0, 0.22)))
        for _ in range(26):                   # the blast: the billow rising over the heap
            ang, rr = rng.uniform(0, 2 * math.pi), math.sqrt(rng.uniform(0, 1))
            rr *= 1 + 0.25 * math.sin(3 * ang + 1.3) + rng.uniform(-0.15, 0.15)
            self.billow.append(dict(x=math.cos(ang) * rr * hw * 1.1, y=math.sin(ang) * rr * hh * 1.1,
                                    z1=rng.uniform(60, 150), s0=rng.uniform(0.8, 1.1), s1=rng.uniform(1.7, 2.4),
                                    life=rng.uniform(3.6, 5.8), peak=rng.uniform(0.55, 0.7), puff=int(rng.integers(8)),
                                    lag=rng.uniform(-0.1, 0.05)))
        for _ in range(40):                   # the blast: chunks thrown out at the impact
            ang = rng.uniform(0, 2 * math.pi)
            self.thrown.append(dict(ang=ang, r0=rect_reach(ang, hw, hh), v=rng.uniform(150, 330), up=rng.uniform(60, 160),
                                    tone=rng.uniform(0.25, 0.5)))


def speck(frame, sx, sy, tone, size=2):
    c0, r0 = int(round(sx - size / 2)), int(round(SCREEN - sy - size / 2))
    r1, c1 = min(SCREEN, r0 + size), min(W, c0 + size)
    r0, c0 = max(0, r0), max(0, c0)
    if r1 > r0 and c1 > c0:
        frame[r0:r1, c0:c1, :3] = np.array([150, 145, 150]) * tone


G = 520.0          # px/s^2 at this scale: a 400 px fall in about 1.25 s


def c_heap():
    """The rubble heap left at the foot (280 x 250, wider than the footprint): a low mound of broken
    floor slabs, facade panels with their window grids, the creep's torn chitin, rebar, a few embers;
    lit as the kit's height fields, coated with settled dust, its shadow down and right, the pre-dawn
    grade."""
    w, h = 280, 250
    c = l9.Canvas(w, h, f=2, alpha=0.0)
    rng = np.random.default_rng(3171)
    nz = raster.fbm(w * 2, h * 2, 24, 3173, octaves=4, period=False)

    def q(xx, uu):
        n = nz[np.clip(((h - uu) * 2).astype(int), 0, h * 2 - 1), np.clip((xx * 2).astype(int), 0, w * 2 - 1)]
        return ((xx - w / 2) / 128) ** 2 + ((uu - h / 2) / 112) ** 2 + (n - 0.5) * 0.7

    c.paint((0, 0, w, h), mask_fn=lambda xx, uu: q(xx, uu) < 1, rgb=l9.CONCRETE * 0.62 + l9.SCORCH * 0.3, alpha=1.0,
            height_fn=lambda xx, uu: 18 * np.clip(1 - q(xx, uu), 0, 1) ** 0.7)
    c.grain(3175, 0.3)
    for _ in range(900):
        rx, ru = rng.uniform(6, w - 6), rng.uniform(6, h - 6)
        qq = q(np.array([rx]), np.array([ru]))[0]
        if qq > 0.95:
            continue
        base = 18 * max(0.0, 1 - qq) ** 0.7
        tone = l9.CONCRETE * rng.uniform(0.45, 1.05) if rng.random() < 0.85 else l9.CHITIN[1] * 0.85
        c.disc(rx, ru, rng.uniform(1.2, 4.5), rgb=tone, height=base + rng.uniform(1, 5), maxh=True)
    for _ in range(20):                                                              # facade panels
        px, pu = rng.uniform(20, w - 46), rng.uniform(22, h - 40)
        if q(np.array([px + 10]), np.array([pu + 6]))[0] > 0.8:
            continue
        pw, ph = rng.uniform(14, 28), rng.uniform(9, 15)
        base = 18 * max(0.0, 1 - q(np.array([px]), np.array([pu]))[0]) ** 0.7 + 3
        c.rect(px, pu, px + pw, pu + ph, rgb=l9.NAVY[1] * 1.4, height=base + rng.uniform(1, 4))
        for wx in np.arange(px + 2, px + pw - 2, 4):
            for wu in np.arange(pu + 2, pu + ph - 2, 5):
                c.rect(wx, wu, wx + 2, wu + 2, rgb=l9.NAVY[0], height=base)
    for _ in range(16):                                                              # rebar
        ax_, au = rng.uniform(40, w - 40), rng.uniform(40, h - 40)
        ang, ln = rng.uniform(0, math.pi), rng.uniform(8, 20)
        c.line(ax_, au, ax_ + ln * math.cos(ang), au + ln * math.sin(ang), 0.8, rgb=l9.SCORCH * 0.5, lift=2.0)
    for _ in range(26):
        ex, eu = rng.uniform(40, w - 40), rng.uniform(40, h - 40)
        if q(np.array([ex]), np.array([eu]))[0] < 0.8:
            c.disc(ex, eu, rng.uniform(0.6, 1.3), emis=l9.EMBER * rng.uniform(0.3, 0.6), rgb=l9.SCORCH)
    arr = c.render(ambient=0.32)
    coat = raster.fbm(w, h, 14, 3177, octaves=3, period=False)[..., None]           # the dust settled on it
    g = 0.25 + 0.4 * coat
    arr[..., :3] = arr[..., :3] * (1 - g) + np.array([0.33, 0.32, 0.33]) * g * (0.7 + 0.6 * arr[..., :3].mean(-1, keepdims=True) / 0.3)
    arr = l9.drop_shadow(arr, np.where(arr[..., 3] > 0.5, 6.0, 0.0), 0.45)
    img = l9.finish(l9.predawn(l9.clear_rim(arr), -40.0), 32)
    return np.array(img.convert("RGBA")).astype(np.float64)


HEAP = None


def blit_centre(frame, img, cx, cy, alpha=1.0):
    h, w = img.shape[:2]
    left, top = int(round(cx - w / 2)), int(round(SCREEN - cy - h / 2))
    r0, r1, c0, c1 = max(0, top), min(SCREEN, top + h), max(0, left), min(W, left + w)
    if r1 <= r0 or c1 <= c0:
        return
    s = img[r0 - top:r1 - top, c0 - left:c1 - left]
    a = s[..., 3:4] / 255 * alpha
    frame[r0:r1, c0:c1, :3] = frame[r0:r1, c0:c1, :3] * (1 - a) + s[..., :3] * a


def c_peak(rt):
    """The heavy dust peak (the production `dust-heavy` tile): in from the base dust's start, full
    0.3 s after the impact, ramping out over 6 s (smoothstep)."""
    if rt < C_DUST:
        return 0.0
    if rt < C_IMPACT + 0.3:
        return (rt - C_DUST) / (C_IMPACT + 0.3 - C_DUST)
    s = min(1.0, (rt - C_IMPACT - 0.3) / DUST)
    return 1 - s * s * (3 - 2 * s)


def frame_c(scene, parts, rt):
    frame = scene.ground(rt)
    fy = scene.foot_y(rt)
    cxf = scene.ax
    hw, hh = FOOT[0] / 2, FOOT[1] / 2
    x0, x1, y0, y1 = cxf - hw, cxf + hw, fy - hh, fy + hh
    lean, shake = c_lean(rt)
    h = c_height(rt)
    standing = rt < C_IMPACT
    jx, jy = c_jitter(rt, shake)
    if standing:
        tower_shadow(frame, x0, x1, y0, y1, h, lean)
    if rt >= C_IMPACT - 0.1:
        blit_centre(frame, HEAP, cxf, fy, min(1.0, (rt - C_IMPACT + 0.1) / 0.2))
    for p in parts.trickle:                                       # landing puffs of the trickling debris
        ft = math.sqrt(2 * p["frac"] * HEIGHT / G)
        t = rt - p["t0"] - ft
        if 0 <= t < 1.6:
            gx = p["x"] + p["out"][0] * p["v"] * ft
            gy = p["y"] + p["out"][1] * p["v"] * ft
            stamp(frame, p["puff"], cxf + gx, fy + gy, p["size"] * (1 + 1.4 * ease_out(t / 1.6)), fade(t, 1.6, 0.4, 0.08))
    for p in parts.base:                                          # the base dust (under the walls)
        t = rt - p["t0"]
        if t < 0:
            continue
        r = p["r0"] + p["d"] * (1 - math.exp(-t / 0.7))
        s = p["s0"] + (p["s1"] - p["s0"]) * ease_out(t / 2.0)
        stamp(frame, p["puff"], cxf + r * math.cos(p["ang"]), fy + r * math.sin(p["ang"]), s, fade(t, p["life"], p["peak"]))
    if standing:
        p_ = max(0.0, (rt - C_FALL) / C_DROP)
        draw_tower_c(frame, x0, x1, y0, y1, h, lean, jx, jy, crush=28 * min(1.0, p_ * 3) if p_ > 0 else 0.0,
                     tint=0.5 * p_ ** 1.5)
    for p in parts.wisps:                                         # dust wisps off the walls in the lean
        t = rt - p["t0"]
        if 0 <= t < 1.4:
            z = p["frac"] * HEIGHT - 60 * t * t
            gx = p["x"] + p["out"][0] * 14 * t + c_offset(z, lean)
            gy = p["y"] + p["out"][1] * 14 * t
            sx, sy = project(np.array([cxf + gx, fy + gy, z]))
            stamp(frame, p["puff"], sx, sy, p["size"] * (1 + 1.4 * t), fade(t, 1.4, 0.35, 0.1))
    for p in parts.trickle:                                       # the trickling debris, falling
        ft = math.sqrt(2 * p["frac"] * HEIGHT / G)
        t = rt - p["t0"]
        if 0 <= t < ft:
            z0 = p["frac"] * c_height(p["t0"])
            z = z0 - 0.5 * G * t * t
            if z <= 0:
                continue
            gx = p["x"] + p["out"][0] * p["v"] * t + c_offset(z0, c_lean(p["t0"])[0])
            gy = p["y"] + p["out"][1] * p["v"] * t
            sx, sy = project(np.array([cxf + gx, fy + gy, z]))
            speck(frame, sx, sy, 0.35)
    for p in parts.chunks:                                        # chunks out of the crushing floors
        t = rt - p["t0"]
        z0 = c_height(p["t0"])
        if t < 0 or z0 <= 2:
            continue
        z = z0 - 0.5 * G * t * t
        if z <= 0:
            continue
        r = rect_reach(p["ang"], hw, hh) + 3 + p["v"] * t
        sx, sy = project(np.array([cxf + r * math.cos(p["ang"]) + c_offset(z0, c_lean(p["t0"])[0]),
                                   fy + r * math.sin(p["ang"]), z]))
        speck(frame, sx, sy, p["tone"])
    if rt >= C_IMPACT - 0.1:
        t = rt - C_IMPACT
        for p in parts.thrown:                                    # chunks thrown out at the impact
            z = p["up"] * t - 0.5 * G * t * t
            if t < 0 or (z <= 0 and t > 0.05):
                continue
            r = p["r0"] + p["v"] * t
            speck(frame, cxf + r * math.cos(p["ang"]), fy + r * math.sin(p["ang"]) + max(0.0, z) * 0.15, p["tone"])
        for p in parts.ring:                                      # the blast's ring, rolling out
            tt = t - p["lag"]
            if tt < 0:
                continue
            e = ease_out(tt / C_KILL[2]) if tt < C_KILL[2] else 1 + (1 - math.exp(-(tt - C_KILL[2]) / 1.6)) * p["drift"] / p["d"]
            r = p["r0"] + p["d"] * e
            s = p["s0"] + (p["s1"] - p["s0"]) * ease_out(tt / 2.4)
            z = 10 + 30 * ease_out(tt / 3.0)
            sx, sy = project(np.array([cxf + r * math.cos(p["ang"]), fy + r * math.sin(p["ang"]), z]))
            stamp(frame, p["puff"], sx, sy, s, fade(tt, p["life"], p["peak"], 0.08))
        for p in parts.billow:                                    # the billow over the heap
            tt = t - p["lag"]
            if tt < 0:
                continue
            z = 20 + (p["z1"] - 20) * ease_out(tt / 4.0)
            s = p["s0"] + (p["s1"] - p["s0"]) * ease_out(tt / 3.0)
            sx, sy = project(np.array([cxf + p["x"] * (1 + 0.25 * ease_out(tt / 3)), fy + p["y"] * (1 + 0.25 * ease_out(tt / 3)), z]))
            stamp(frame, p["puff"], sx, sy, s, fade(tt, p["life"], p["peak"], 0.1))
    peak = c_peak(rt)
    if peak > 0:
        rows = (int(scene.scroll(rt) * 1.35) + (SCREEN - 1 - np.arange(SCREEN))) % DUST_TILE.shape[0]
        tile = DUST_TILE[DUST_TILE.shape[0] - 1 - rows]
        blend(frame, tile, peak)
    return Image.fromarray(np.clip(frame[..., :3], 0, 255).astype(np.uint8), "RGB"), (y0, y1, x1, cxf, fy)


KEYS_C = [(0.5, "STANDING (NODES C DEAD)"), (1.25, "LEAN 0.45 S: DEBRIS, DUST"), (1.75, "LEAN 0.95 S"),
          (2.25, "LEAN 1.45 S: 4 DEG TO THE RIGHT"), (2.8, "DROP 0.5 S"), (3.3, "DROP 1.0 S"), (3.6, "DROP 1.3 S: BASE DUST"),
          (3.85, "IMPACT + 0.05 S: THE BLAST"), (4.3, "IMPACT + 0.5 S"), (4.8, "IMPACT + 1.0 S"),
          (6.8, "IMPACT + 3 S: SETTLING"), (9.8, "IMPACT + 6 S: THE HEAP")]
TEXT_C = ("C - LEANS, THEN COLLAPSES INTO A DUST BLAST",
          ["THE WARNING IS THE LEAN (1.5 S): THE TOWER LEANS SLOWLY TO THE RIGHT, 0 TO 4 DEGREES, DEBRIS AND DUST",
           "TRICKLING OFF IT, ITS SHADOW (HALF LENGTH) ON IT. THEN IT DROPS STRAIGHT DOWN IN ONE GO IN 1.5 S (THE LEAN KEPT,",
           "THE SHADOW SHRINKING); DUST OUT OF ITS BASE 0.35 S BEFORE THE IMPACT, A DUST BLAST IN EVERY DIRECTION AT IT (IMPACT =",
           "WARNING + 3.0 S), SETTLING OVER 6 S ONTO A RUBBLE HEAP.  DEBUG (SHEET ONLY): THE KILL RING, THE BAND."])


def build_c():
    global PUFFS, HEAP
    PUFFS = puff_bank()
    HEAP = c_heap()
    scene = Scene()
    parts = Particles()
    from PIL import ImageDraw
    cols, rows = 4, 3
    width = 16 + cols * (W + 12)
    sheet = raster.sheet(width, 112 + rows * (SCREEN + 30) + 10, f"LEVEL 09 COLLAPSE - {TEXT_C[0]} (CONCEPT R31 C)",
                         "1.5 S LEAN, 1.5 S DROP, DUST BLAST, HEAVY DUST OVER 6 S; THE SCROLL AT THE HOLD'S 30 PX/S; 1X")
    for i, line in enumerate(TEXT_C[1]):
        raster.draw_text(sheet, 16, 36 + i * 11, line, raster.LABEL if i < 3 else raster.LABEL_DIM)
    for k, (rt, label) in enumerate(KEYS_C):
        img, (y0, y1, x1, cxf, fy) = frame_c(scene, parts, rt)
        im = img.convert("RGBA")
        d = ImageDraw.Draw(im)
        if k == 0 or rt >= C_IMPACT:
            d.rectangle([x1, SCREEN - y1, W - 1, SCREEN - y0], outline=(255, 190, 60, 255))
            if k == 0:
                raster.draw_text(im, int(x1) + 6, int(SCREEN - y1) + 4, "KILL BAND", (255, 190, 60))
        t = rt - C_IMPACT
        if 0 <= t <= C_KILL[2]:
            r = C_KILL[0] + (C_KILL[1] - C_KILL[0]) * ease_out(t / C_KILL[2])
            d.ellipse([cxf - r, SCREEN - fy - r, cxf + r, SCREEN - fy + r], outline=(255, 90, 60, 255), width=2)
            raster.draw_text(im, int(min(W - 120, x1 + 6)), int(SCREEN - y1) + 4, f"KILL RING R {r:.0f}", (255, 120, 80))
        x, y = 16 + (k % cols) * (W + 12), 102 + (k // cols) * (SCREEN + 30)
        sheet.alpha_composite(im, (x, y + 12))
        raster.draw_text(sheet, x, y, f"T+{rt:.2f} S  {label}", raster.LABEL)
    png = OUT / "collapse-r31-c.png"
    sheet.convert("RGB").save(png, optimize=True)
    frames = [frame_c(scene, parts, float(rt))[0] for rt in np.arange(0, C_CLIP, 1 / FPS)]
    gif = png.with_suffix(".gif")
    write_gif(frames, gif, fps=FPS, colors=128)
    print(f"{png.relative_to(ROOT)}, {gif.relative_to(ROOT)} {gif.stat().st_size / 1e6:.1f} MB, {len(frames)} frames")


if __name__ == "__main__":
    for v in [a for a in sys.argv[1:] if a in ("a", "b", "c")] or ["a", "b", "c"]:
        build_c() if v == "c" else build(v)
