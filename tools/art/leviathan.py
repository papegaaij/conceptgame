#!/usr/bin/env python3
"""Production art: the Leviathan, Level 03's set piece (design/enemies/space/leviathan; M4 part C
batch).

Outputs (assets/sprites/ and assets/pivots/), every sprite under the fixed key light, nothing lit
rotated or mirrored afterwards:
  leviathan-down_0..2.png    300x480, the second pass (facing down the screen): the body without
                             its shootable parts, the unit centre at the canvas centre, in three
                             frames of the tail sway (the tail end at -6, 0 and +6 degrees, the head
                             still; frame 0 swings the fluke to the screen's right). Played 0-1-2-1.
  leviathan-vent-1..4.png    the four dorsal turret vents (data `vent 1`..`vent 4`), each its own
  leviathan-vent-1..4-wrecked.png  render (the hull slopes differently under each), and the
                             blasted socket of each
  leviathan-fin-left.png, leviathan-fin-right.png and their -wrecked versions: the pectoral fins
                             (screen left and right) and their torn stumps
  leviathan-fluke_0..2.png, leviathan-fluke-wrecked_0..2.png: the fluke per sway frame, whole and
                             with a lobe shot off
  leviathan-blowhole-glow.png  56x56 violet glow over the blowhole (additive)
  pivots/leviathan.json      {"down": {"fluke": [[dx, dy] x 3]}}: the fluke sprite's centre per sway
                             frame, px from the unit centre, dx right and dy up (towards the tail);
                             its "death" entry is leviathan_death.py's and is kept
  leviathan-cross_0..2.png   the first pass (high-air, crossing diagonally down and to the right):
                             the whole intact unit at heading -58.01 degrees (clockwise from straight
                             down; its head turned to the right), the course of Level 03's `cross`
                             pass, drawn 1.25x, in the three sway frames, on a canvas just large
                             enough with the unit centre at its centre
  design/enemies/space/leviathan/concept/leviathan-final-r16-a.png/.gif   review sheet and loop

Every part sprite is centred on its part's offset from the data (`part_list`, px from the unit centre
facing down, dx right, dy up): in the 300x480 frame at x = 150 + dx, y = 240 - dy, so the game draws
it there over the body; a destroyed part is drawn with its -wrecked sprite instead. The parts are cut
from renders of the whole unit (body and part together, so occlusion, shadows and the fillets match)
by an ID pass: every material of a part carries its part number, and a pixel belongs to the part's
sprite when the part covers at least half of what the pixel shows (or the body alone would leave the
pixel empty). One palette of 48 colours for the whole second-pass set (body frames and every part),
so a part over the body never shows a seam, and one for the first-pass frames; 8 of the 48 are cut
from the violet glows alone (a plain median cut merges them into the bone hide).

The model is the chosen round-05 Leviathan (re-rendered in round 08, render/archetype_models.py:
the bone/violet materials, hide mottle, dorsal plates, sacs, vents, tail segments, fluke and fins)
laid out to the data's part offsets: the long head forward, the fins near the middle, the vents and
the blowhole on the back half and a short three-segment tail, 100 px per model unit (125 for the
first pass), rendered at 4x.

Run: python3 tools/art/leviathan.py [--cross] [--review]   (~12 min on 20 cores, 10 renders at a time,
~1.5-2 GB each; --cross renders only the first-pass frames, ~4 min (the second-pass files stay as they are),
--review only rebuilds the review sheet and loop)
"""
import json
import sys
from concurrent.futures import ProcessPoolExecutor

import numpy as np
import yaml
from PIL import Image, ImageDraw

import artkit
from artkit import DESIGN, sprite

import vfx_r08 as v8  # noqa: E402  (concept script, imported unchanged)
from render import archetype_models as am  # noqa: E402
from render import enemy_models as em  # noqa: E402
from render import sdf  # noqa: E402
from render.enemy_models import V_BODY, V_BONE, V_DARK, V_EYE, V_GLOW, V_SAC  # noqa: E402
from render.enemy_rigs import model_rotation, model_space_materials  # noqa: E402
from render.sdf import (Material, mirror_x, rotate_z, sd_capsule, sd_cylinder_z, sd_ellipsoid,  # noqa: E402
                        sd_plate, sd_sphere, union)

SCRIPT = "leviathan.py"
SOURCE = artkit.source_note(SCRIPT, "M4 part C batch")
BATCH = "M4 part C batch"
UNIT = DESIGN / "enemies" / "space" / "leviathan"
FACTOR = 4
S = 100.0                       # px per model unit, second pass
W, H = 300, 480                 # the unit facing down
COLOURS = 48
ACCENT_COLOURS = 8              # of the palette kept for the violet glows
SWAY = [-6.0, 0.0, 6.0]         # degrees of the tail end per sway frame
SHARE = [0.3, 0.6, 0.85, 1.0]   # of the sway at tail segments 1-3 and the fluke
# First pass, degrees clockwise from straight down (negative: the head turned to the screen's right):
# along Level 03's `cross` path, its centre from (-265, -150) at t=55 to (846, 544) at t=80, y below
# the top edge, so atan2(1111, 694) = 58.01 degrees down and to the right (the data's `heading: 58`).
CROSS_PATH = ((-265, -150), (846, 544))
CROSS_HEADING = -float(np.degrees(np.arctan2(CROSS_PATH[1][0] - CROSS_PATH[0][0],
                                             CROSS_PATH[1][1] - CROSS_PATH[0][1])))
CROSS_SCALE = 1.25
# The key light at a fixed place relative to the unit centre (model units), as for the 300 px
# canvas, so every frame and both passes are lit from the same point on the screen.
LIGHT = np.asarray(sdf.KEY_POS) * (3.0 / 2.3)

# --- the body, model units (head +Y, x = the fish's right; facing down it is the screen's left)
MAIN = ((0.0, 0.05, 0.0), (0.62, 1.65, 0.48))
HEAD = ((0.0, 1.7, 0.0), (0.56, 0.62, 0.42))
SAC = ((0.43, -0.35, -0.02), (0.22, 0.85, 0.3))
PLATES = [1.95, 1.45, 0.95, 0.45, -0.1, -1.05]
TAIL_JOINT = -1.35
TAIL_Z = -0.04
TAIL_R = [(0.30, 0.25), (0.25, 0.19), (0.19, 0.13)]
FIN_SCALE = 0.9
FIN_CENTRE = (0.49, -0.215)     # the concept fin's centre in its own frame (outward, forward)
FLUKE_SCALE = 0.75
FLUKE_CENTRE = -0.225           # the fluke's centre below its pivot (model units, scaled)
R_TAG = 0.17                    # an intact vent's sprite: the hide this far around it (fillet, shadow)
R_SCORCH = 0.2                  # a wrecked vent's scorch

VENTS = ["vent 1", "vent 2", "vent 3", "vent 4"]
FINS = ["left fin", "right fin"]
PARTS = VENTS + FINS + ["fluke"]          # part number = index + 1 (0 = the body)
NAMES = {"vent 1": "leviathan-vent-1", "vent 2": "leviathan-vent-2", "vent 3": "leviathan-vent-3",
         "vent 4": "leviathan-vent-4", "left fin": "leviathan-fin-left",
         "right fin": "leviathan-fin-right", "fluke": "leviathan-fluke"}


def spec():
    return yaml.safe_load((UNIT / "data.yaml").read_text(encoding="utf-8"))


SPEC = spec()
PART = {p["name"]: p for p in SPEC["part_list"]}
assert tuple(SPEC["size"]) == (W, H)


def mxy(name):
    """A part's offset in model units (the unit faces down: model = screen turned by 180 degrees)."""
    dx, dy = PART[name]["offset"]
    return -dx / S, -dy / S


def ell_z(c, r, x, y):
    t = 1 - ((x - c[0]) / r[0]) ** 2 - ((y - c[1]) / r[1]) ** 2
    return c[2] + r[2] * np.sqrt(t) if t > 0 else -1.0


def surface_z(x, y):
    return max(ell_z(*MAIN, x, y), ell_z(*HEAD, x, y))


BLOWHOLE = mxy("blowhole")
_FLUKE_Y = mxy("fluke")[1]
_TAIL_LEN = TAIL_JOINT - (_FLUKE_Y - FLUKE_CENTRE)
TAIL_L = [_TAIL_LEN * f / sum(ln for ln, _, _ in am.LEV_TAIL) for f, _, _ in am.LEV_TAIL]


def chain(sway_deg):
    """Joint positions (model units) and headings of the tail segments and the fluke."""
    phis = [np.radians(sway_deg) * s for s in SHARE]
    joints = [np.array([0.0, TAIL_JOINT, TAIL_Z])]
    for ln, phi in zip(TAIL_L, phis):
        joints.append(joints[-1] + np.array([ln * np.sin(phi), -ln * np.cos(phi), 0.0]))
    return joints, phis


def fluke_offset(sway_deg):
    """The fluke sprite's centre in the unit frame (px, dx right, dy up), rounded to whole px."""
    joints, phis = chain(sway_deg)
    x = joints[3][0] - FLUKE_CENTRE * np.sin(phis[3])
    y = joints[3][1] + FLUKE_CENTRE * np.cos(phis[3])
    return [int(round(-x * S)), int(round(-y * S))]


def part_offset(name, k=1):
    return fluke_offset(SWAY[k]) if name == "fluke" else list(PART[name]["offset"])


# --------------------------------------------------------------------------- materials

def materials(scorch=()):
    """The concept's Leviathan materials plus char, ichor and scorched hide (wrecks)."""
    mats = am.lev_mats(1.0)
    mottle = mats[V_BODY].pattern
    centres = np.array(scorch, dtype=np.float64).reshape(-1, 2)

    def scorched(p, n):
        if not len(centres):
            return mottle(p, n)
        d = np.min(np.hypot(p[:, 0:1] - centres[:, 0], p[:, 1:2] - centres[:, 1]), axis=1)
        d = d + 0.025 * np.sin(p[:, 0] * 53) * np.sin(p[:, 1] * 47)
        return mottle(p, n) * (0.22 + 0.78 * sdf.smoothstep(0.07, R_SCORCH, d))
    mid = em.hx(em.CHITIN_BASES["bone"][0])
    mats += [Material((0.10, 0.08, 0.09), metal=0.1, shininess=20, spec=0.2),        # char
             Material((0.20, 0.07, 0.26), metal=0.0, shininess=120, spec=1.2),       # ichor
             Material(mid, metal=0.2, shininess=50, spec=0.4, pattern=scorched)]     # scorched hide
    return mats


N = 10
CHAR, ICHOR, SCORCH = 7, 8, 9


def id_materials():
    """ID pass: the body black, part j emitting its number as three bits of colour."""
    black = Material((0.0, 0.0, 0.0), metal=0.0, shininess=1, spec=0.0)
    out = [black] * N
    for j in range(1, len(PARTS) + 1):
        out += [Material((0.0, 0.0, 0.0), metal=0.0, shininess=1, spec=0.0,
                         emission=(j & 1, (j >> 1) & 1, (j >> 2) & 1))] * N
    return out


# --------------------------------------------------------------------------- the model

def _tag(m, j):
    return m + N * j


def bounded(p, centre, radius, fn):
    """``fn(p)`` evaluated only for the points near a sphere that encloses its shape; elsewhere
    the distance to that sphere (a lower bound, so the march stays safe). Keeps the parts cheap."""
    db = np.linalg.norm(p - np.asarray(centre), axis=1) - radius
    near = db < 0.1
    d, m = db, np.zeros(len(p), dtype=np.int32)
    if near.any():
        dn, mn = fn(p[near])
        d[near] = dn
        m[near] = mn
    return d, m


def fin_items(name, wrecked, p):
    x, y = mxy(name)
    return bounded(p, (x, y, 0.0), 0.78, lambda p: _fin(name, wrecked, p))


def _fin(name, wrecked, p):
    j = PARTS.index(name) + 1
    x, y = mxy(name)
    side = 1.0 if x > 0 else -1.0
    pivot = np.array([x - side * FIN_CENTRE[0] * FIN_SCALE, y - FIN_CENTRE[1] * FIN_SCALE, 0.0])
    u = (p - pivot) / FIN_SCALE
    d, m = am.leviathan_fin(int(side))[0](u)
    q = u.copy()
    q[:, 0] *= side
    stub = sd_plate(q, [(-0.3, 0.13), (0.06, 0.13), (0.06, -0.14), (-0.3, -0.14)], 0.0, 0.06)
    d, m = union((d, m), (stub, V_BODY))
    if wrecked:
        cut = 0.42 + 0.05 * np.sin(q[:, 1] * 23 + 1.3 * side) + 0.025 * np.sin(q[:, 1] * 61 + 2.0)
        d = np.maximum(d, (q[:, 0] - cut) * 0.35)
        m = np.where(q[:, 0] > cut - 0.08, CHAR, m)
        d, m = union((d, m),
                     (sd_ellipsoid(q, (0.34, -0.1, 0.05), (0.05, 0.03, 0.025)), ICHOR),
                     (sd_ellipsoid(q, (0.26, -0.24, 0.045), (0.035, 0.05, 0.02)), ICHOR), k=0.01)
    return d * FIN_SCALE, _tag(m, j)


def fluke_items(joint, phi, wrecked, p):
    c = joint + np.array([-FLUKE_CENTRE * np.sin(phi), FLUKE_CENTRE * np.cos(phi), 0.0])
    return bounded(p, c, 0.7, lambda p: _fluke(joint, phi, wrecked, p))


def _fluke(joint, phi, wrecked, p):
    j = PARTS.index("fluke") + 1
    u = rotate_z(p - joint, phi) / FLUKE_SCALE
    d, m = am.leviathan_fluke()[0](u)
    if wrecked:
        cut = 0.24 + 0.05 * np.sin(u[:, 1] * 23 + 0.5) + 0.025 * np.sin(u[:, 1] * 61 + 1.0)
        d = np.maximum(d, (u[:, 0] - cut) * 0.35)
        m = np.where(u[:, 0] > cut - 0.08, CHAR, m)
        for c, r in (((-0.5, -0.3, 0.0), 0.065), ((-0.64, -0.46, 0.0), 0.04)):
            hole = sd_sphere(u, c, r)
            d = np.maximum(d, -hole)
            m = np.where(hole < 0.035, CHAR, m)
        d, m = union((d, m), (sd_ellipsoid(u, (0.16, -0.1, 0.06), (0.06, 0.035, 0.03)), ICHOR), k=0.01)
    return d * FLUKE_SCALE, _tag(m, j)


def vent_wreck(j, x, y, z, p):
    """A blasted vent socket: a charred crater floor, broken shards round its rim, a smear of ichor."""
    rng = np.random.default_rng(j)
    items = [(sd_ellipsoid(p, (x, y, z - 0.1), (0.1, 0.1, 0.035)), _tag(CHAR, j))]
    for a in rng.uniform(0, 2 * np.pi, 5):
        ln = rng.uniform(0.03, 0.06)
        items.append((sd_capsule(p, (x + 0.095 * np.cos(a), y + 0.095 * np.sin(a), z - 0.07),
                                 (x + (0.11 + ln) * np.cos(a), y + (0.11 + ln) * np.sin(a), z + 0.01 + ln * 0.4),
                                 0.025, 0.01), _tag(CHAR, j)))
    a = rng.uniform(0, 2 * np.pi)
    ix, iy = x + 0.15 * np.cos(a), y + 0.15 * np.sin(a)
    items.append((sd_ellipsoid(p, (ix, iy, surface_z(ix, iy) - 0.01), (0.04, 0.03, 0.02)), _tag(ICHOR, j)))
    return union(*items, k=0.015)


def unit_model(sway_deg, parts_on=True, wrecked=False):
    """The Leviathan in model space (head +Y) with the tail swayed by ``sway_deg`` at its end:
    the body only, or with every part intact or every part wrecked."""
    joints, phis = chain(sway_deg)
    vents = [(PARTS.index(n) + 1, *mxy(n), surface_z(*mxy(n))) for n in VENTS]
    bx, by = BLOWHOLE
    bz = surface_z(bx, by)
    plates = [(0.0, y, surface_z(0.0, y) - 0.035) for y in PLATES]

    def scene(p):
        q = mirror_x(p)
        d, m = union((sd_ellipsoid(p, *MAIN), V_BODY), (sd_ellipsoid(p, *HEAD), V_BODY),
                     (sd_ellipsoid(q, *SAC), V_SAC), k=0.2)
        d, m = union((d, m), *[(sd_ellipsoid(p, c, (0.13, 0.2, 0.07)), V_BONE) for c in plates], k=0.05)
        d, m = union((d, m), (sd_cylinder_z(p, (bx, by, bz - 0.05), 0.15, 0.05), V_DARK), k=0.04)
        d, m = union((d, m),
                     (sd_sphere(p, (bx, by, bz - 0.03), 0.1), V_EYE),
                     (sd_sphere(q, (0.45, 1.86, 0.18), 0.06), V_EYE),
                     (sd_capsule(q, (0.0, 2.29, 0.12), (0.3, 2.18, 0.1), 0.05, 0.05), V_DARK),
                     (sd_capsule(q, (0.3, 2.18, 0.1), (0.52, 1.86, 0.05), 0.05, 0.04), V_DARK), k=0.02)
        if parts_on and wrecked:
            for _, x, y, z in vents:
                d = np.maximum(d, -sd_sphere(p, (x, y, z + 0.04), 0.13))
        items = []
        for i, ((r0, r1), ln) in enumerate(zip(TAIL_R, TAIL_L)):
            lp = rotate_z(p - joints[i], phis[i])
            items += [(sd_capsule(lp, (0, 0, 0), (0, -ln, 0), r0, r1), V_BODY),
                      (sd_capsule(lp, (0, -0.03, r0 * 0.8), (0, -ln + 0.03, r1 * 0.8), 0.05, 0.035), V_BONE)]
        d, m = union((d, m), *items)
        if not parts_on:
            return d, m
        for j, x, y, _ in vents:
            r = np.hypot(p[:, 0] - x, p[:, 1] - y)
            if wrecked:
                m = np.where((m < N) & (r < R_SCORCH), _tag(SCORCH, j), m)
            else:
                m = np.where((m < N) & (r < R_TAG), _tag(m, j), m)
        for j, x, y, z in vents:
            if wrecked:
                d, m = union((d, m), bounded(p, (x, y, z), 0.26, lambda p, v=(j, x, y, z): vent_wreck(*v, p)),
                             k=0.015)
            else:
                d, m = union((d, m), (sd_cylinder_z(p, (x, y, z - 0.095), 0.11, 0.08), _tag(V_DARK, j)), k=0.04)
                d, m = union((d, m), (sd_sphere(p, (x, y, z - 0.035), 0.07), _tag(V_GLOW, j)), k=0.02)
        d, m = union((d, m), fin_items("left fin", wrecked, p), fin_items("right fin", wrecked, p),
                     fluke_items(joints[3], phis[3], wrecked, p))
        return d, m
    scorch = [(x, y) for _, x, y, _ in vents] if parts_on and wrecked else ()
    return scene, materials(scorch)


# --------------------------------------------------------------------------- rendering

ROT_DOWN = model_rotation(np.pi / 2)
ROT_CROSS = model_rotation(np.pi / 2 + np.radians(CROSS_HEADING))
CROSS_CANVAS = (600, 472)       # a little more than the 1.25x unit turned by 58 degrees, cropped later


def render(model, mats, rot, size, scale, shadows=True):
    """Ray-march the model turned by ``rot`` onto a ``size`` px canvas at ``scale`` px per unit,
    the key light at LIGHT on the screen whatever the heading."""
    w, h = size
    extent = w / scale
    scene = lambda p: model(rotate_z(p, rot))  # noqa: E731
    return sdf.render(scene, model_space_materials(mats, rot), (w * FACTOR, h * FACTOR), extent,
                      key_pos=LIGHT / (extent / 2.3), shadows=shadows, steps=140)


def coverage(hi):
    return sprite.downsample(hi, FACTOR)[..., 3]


def part_coverage(hi):
    """Per part the fraction of each native pixel it covers, from an ID-pass render."""
    bits = hi[..., :3] > 0.36
    code = bits[..., 0] + 2 * bits[..., 1] + 4 * bits[..., 2]
    code[hi[..., 3] < 0.5] = 0
    h, w = code.shape
    blocks = code.reshape(h // FACTOR, FACTOR, w // FACTOR, FACTOR)
    return np.stack([(blocks == j).mean(axis=(1, 3)) for j in range(1, len(PARTS) + 1)])


def job(kind, k):
    sway = SWAY[k]
    if kind == "cross":
        model, mats = unit_model(sway)
        return artkit.native(render(model, mats * (len(PARTS) + 1), ROT_CROSS, CROSS_CANVAS, S * CROSS_SCALE), FACTOR), None
    parts_on = kind != "body"
    wrecked = kind.endswith("wrecked")
    model, mats = unit_model(sway, parts_on, wrecked)
    if kind.startswith("id"):
        hi = render(model, id_materials(), ROT_DOWN, (W, H), S, shadows=False)
        return part_coverage(hi), coverage(hi)
    hi = render(model, mats * (len(PARTS) + 1), ROT_DOWN, (W, H), S)
    return artkit.native(hi, FACTOR), coverage(hi)


def quantize_accented(frames, colors, accent=ACCENT_COLOURS):
    """``artkit.quantize_set`` with one palette whose last ``accent`` entries are cut from the
    saturated (glowing violet) pixels alone: median cut by pixel count would merge the small vent
    and sac glows into the large bone hide. The set still shares one palette of ``colors``."""
    arrays = [np.array(f.convert("RGBA")) for f in frames]
    pixels = np.concatenate([a[a[..., 3] > 0][:, :3] for a in arrays]).astype(np.int32)
    hi, lo = pixels.max(axis=1), pixels.min(axis=1)
    vivid = (hi - lo) > 0.3 * np.maximum(hi, 1)

    def cut(px, n):
        img = Image.fromarray(px.astype(np.uint8).reshape(-1, 1, 3), "RGB")
        pal = img.quantize(colors=n, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE)
        used = sorted(set(np.array(pal).ravel().tolist()))
        return [pal.getpalette()[3 * i:3 * i + 3] for i in used]
    entries = cut(pixels[~vivid], colors - accent) + (cut(pixels[vivid], accent) if vivid.any() else [])
    entries += [entries[0]] * (256 - len(entries))
    palette = Image.new("P", (1, 1))
    palette.putpalette([c for e in entries for c in e])
    out = []
    for a in arrays:
        rgb = Image.fromarray(np.ascontiguousarray(a[..., :3]), "RGB")
        mapped = np.array(rgb.quantize(palette=palette, dither=Image.Dither.NONE).convert("RGB"))
        result = np.dstack([mapped, a[..., 3]])
        result[a[..., 3] == 0] = 0
        out.append(Image.fromarray(result.astype(np.uint8), "RGBA"))
    return out


def cut_part(img, cov_all, cov_j, body_alpha, centre):
    """The part's pixels of a whole-unit render, on a canvas centred on the part's offset."""
    a = np.array(img)
    own = (a[..., 3] > 0) & (cov_j > 0) & ((cov_j >= 0.5 * cov_all) | (body_alpha < 0.5))
    a[~own] = 0
    ys, xs = np.nonzero(own)
    cx, cy = centre
    hw = int(max(cx - xs.min(), xs.max() + 1 - cx)) + 1
    hh = int(max(cy - ys.min(), ys.max() + 1 - cy)) + 1
    return Image.fromarray(a, "RGBA").crop((cx - hw, cy - hh, cx + hw, cy + hh))


def canvas_xy(offset):
    return W // 2 + offset[0], H // 2 - offset[1]


def crop_centred(frames):
    """Crop a frame set to the smallest even canvas, the same for all, keeping the centre."""
    w, h = frames[0].size
    cx, cy = w // 2, h // 2
    hw = hh = 0
    for f in frames:
        ys, xs = np.nonzero(np.array(f)[..., 3])
        hw = max(hw, cx - xs.min(), xs.max() + 1 - cx)
        hh = max(hh, cy - ys.min(), ys.max() + 1 - cy)
    hw, hh = int(hw) + 1, int(hh) + 1
    assert hw < cx and hh < cy, "the render canvas is too small for the unit"
    return [f.crop((cx - hw, cy - hh, cx + hw, cy + hh)) for f in frames]


def blowhole_glow():
    """Violet glow over the blowhole (vfx_r08's light fields), additive."""
    weak = em.scheme_colors("leviathan")[4] * 255
    cv = v8.Canvas(56, 56)
    d = cv.dist(28, 28)
    cv.add(tuple(weak), v8.gauss(d, 13.0) * 0.9)
    cv.add((235, 215, 255), v8.gauss(d, 4.5) * 0.8)
    return artkit.quantize_set([artkit.additive(cv.image())], 16)[0]


def build_cross():
    """The first-pass frames alone."""
    with ProcessPoolExecutor(max_workers=3) as pool:
        results = list(pool.map(job, ["cross"] * 3, range(3)))
    write_cross([r[0] for r in results])


def write_cross(frames):
    cross = crop_centred(frames)
    artkit.write_frames("leviathan-cross", quantize_accented(cross, COLOURS), SOURCE)
    print(f"leviathan-cross: {len(cross)} frames of {cross[0].width}x{cross[0].height}")


def build():
    kinds = [("cross", k) for k in range(3)] + [(kind, k) for kind in
                                                ("body", "intact", "wrecked", "id-intact", "id-wrecked")
                                                for k in range(3)]
    with ProcessPoolExecutor(max_workers=10) as pool:
        results = dict(zip(kinds, pool.map(job, *zip(*kinds))))
    bodies = [results[("body", k)][0] for k in range(3)]
    body_alpha = [results[("body", k)][1] for k in range(3)]
    sprites = {}
    for state in ("intact", "wrecked"):
        for name in PARTS:
            j = PARTS.index(name)
            frames = range(3) if name == "fluke" else [1]
            cut = []
            for k in frames:
                img, cov_all = results[(state, k)]
                cov = results[(f"id-{state}", k)][0][j]
                cut.append(cut_part(img, cov_all, cov, body_alpha[k], canvas_xy(part_offset(name, k))))
            sprites[(name, state)] = cut
    # one palette for the body frames and every part of the second pass
    order = [(n, s) for s in ("intact", "wrecked") for n in PARTS]
    flat = bodies + [f for key in order for f in sprites[key]]
    mapped = quantize_accented(flat, COLOURS)
    artkit.write_frames("leviathan-down", mapped[:3], SOURCE)
    i = 3
    for name, state in order:
        n = len(sprites[(name, state)])
        out = mapped[i:i + n]
        i += n
        stem = NAMES[name] + ("-wrecked" if state == "wrecked" else "")
        artkit.write_frames(stem, out, SOURCE, single=(n == 1))
    artkit.write_frames("leviathan-blowhole-glow", [blowhole_glow()], SOURCE, single=True)
    # keeps the "death" entry of tools/art/leviathan_death.py (the break-up's chunks and blasts)
    old = artkit.PIVOTS / "leviathan.json"
    keep = {k: v for k, v in json.loads(old.read_text(encoding="utf-8")).items()
            if k == "death"} if old.exists() else {}
    artkit.write_pivots("leviathan", SOURCE, {"down": {"fluke": [fluke_offset(s) for s in SWAY]}, **keep})
    write_cross([results[("cross", k)][0] for k in range(3)])
    # how well body + parts reproduce the whole render (pixels that differ, before the palette)
    for k in range(3):
        comp = compose(bodies[k], {n: sprites[(n, "intact")][k if n == "fluke" else 0] for n in PARTS}, k)
        ref = np.array(results[("intact", k)][0]).astype(int)
        diff = np.abs(np.array(comp).astype(int) - ref).max(axis=-1) > 24
        print(f"sway frame {k}: body + parts differ from the whole render in {int(diff.sum())} px")


# --------------------------------------------------------------------------- review material

def compose(body, parts, k, glow=None, glow_level=1.0):
    """The second-pass unit as the game draws it: the body, each part on its offset, the glow."""
    img = body.copy()
    for name, spr in parts.items():
        sprite.paste_center(img, spr, *canvas_xy(part_offset(name, k)))
    if glow is not None:
        g = np.array(glow).astype(np.float64)
        g[..., :3] *= glow_level
        g = Image.fromarray(g.astype(np.uint8), "RGBA")
        bx, by = canvas_xy(PART["blowhole"]["offset"])
        img = artkit.add_light(img, g, (bx - glow.width // 2, by - glow.height // 2))
    return img


def on_plate(img):
    out = Image.new("RGBA", img.size, artkit.PLATE)
    out.alpha_composite(img)
    return out


def hit_boxes(img):
    """The data's hit boxes as thin outlines: the body white, vital parts violet, the others amber."""
    out = img.copy()
    d = ImageDraw.Draw(out)
    bw, bh = SPEC["hitbox"]
    d.rectangle([W // 2 - bw // 2, H // 2 - bh // 2, W // 2 + bw // 2 - 1, H // 2 + bh // 2 - 1],
                outline=(255, 255, 255, 255))
    for p in SPEC["part_list"]:
        x, y = canvas_xy(p["offset"])
        w, h = p["hitbox"]
        col = (200, 120, 255, 255) if p["kind"] == "vital" else (255, 170, 40, 255)
        d.rectangle([x - w // 2, y - h // 2, x + w // 2 - 1, y + h // 2 - 1], outline=col)
        d.point([(x, y)], fill=col)
    return out


def level_path():
    """Level 03's `cross` pass course from its data: the first and last path points (x, y below
    the top edge), read independently of CROSS_PATH so the review checks the art against the data."""
    level = DESIGN / "campaign" / "act-1-first-contact" / "level-03-spore-drift" / "data.yaml"
    data = yaml.safe_load(level.read_text(encoding="utf-8"))
    piece = next(s for s in data["set_pieces"] if s["enemy"] == "leviathan")
    path = next(p for p in piece["passes"] if p["name"] == "cross")["path"]
    return path[0][1:], path[-1][1:]


def flight_arrow(img, start, end):
    """The course from ``start`` to ``end`` over a first-pass frame: a thin line through the unit
    centre and an arrow from the centre the way it flies."""
    out = img.copy()
    d = ImageDraw.Draw(out)
    ux, uy = end[0] - start[0], end[1] - start[1]
    n = np.hypot(ux, uy)
    ux, uy = ux / n, uy / n
    cx, cy = img.width / 2, img.height / 2
    reach = np.hypot(cx, cy)
    d.line([cx - ux * reach, cy - uy * reach, cx + ux * reach, cy + uy * reach], fill=(120, 255, 140, 255), width=1)
    tip = (cx + ux * 0.42 * reach, cy + uy * 0.42 * reach)
    d.line([cx, cy, *tip], fill=(120, 255, 140, 255), width=3)
    px, py = -uy, ux
    head = [tip, (tip[0] - 22 * ux + 10 * px, tip[1] - 22 * uy + 10 * py),
            (tip[0] - 22 * ux - 10 * px, tip[1] - 22 * uy - 10 * py)]
    d.polygon(head, fill=(120, 255, 140, 255))
    d.ellipse([cx - 4, cy - 4, cx + 4, cy + 4], outline=(120, 255, 140, 255), width=2)
    return out


def load_parts(state):
    suffix = "-wrecked" if state == "wrecked" else ""
    return {n: artkit.load_frames(NAMES[n] + suffix) for n in PARTS}


def review():
    artkit.REVIEW_ROUND = "r16"
    bodies = artkit.load_frames("leviathan-down")
    intact, wrecked = load_parts("intact"), load_parts("wrecked")
    glow = artkit.load_frames("leviathan-blowhole-glow")[0]
    cross = artkit.load_frames("leviathan-cross")
    pivots = json.loads((artkit.PIVOTS / "leviathan.json").read_text(encoding="utf-8"))
    assert pivots["down"]["fluke"] == [fluke_offset(s) for s in SWAY]

    def pick(parts, k):
        return {n: f[k] if n == "fluke" else f[0] for n, f in parts.items()}

    whole = [on_plate(compose(bodies[k], pick(intact, k), k, glow)) for k in range(3)]
    wreck = [on_plate(compose(bodies[k], pick(wrecked, k), k, glow)) for k in range(3)]
    boxes = [hit_boxes(on_plate(compose(bodies[1], pick(intact, 1), 1))),
             hit_boxes(on_plate(compose(bodies[1], pick(wrecked, 1), 1))),
             hit_boxes(on_plate(bodies[1]))]
    width = 16 * 2 + sum(f.width + 6 for f in cross)
    start, end = level_path()
    course = float(np.degrees(np.arctan2(end[0] - start[0], end[1] - start[1])))
    assert abs(course + CROSS_HEADING) < 0.5, f"the art's heading {CROSS_HEADING} is off the data's course {course}"
    sheet = artkit.review_sheet("LEVIATHAN - FINAL SPRITES", [
        ("PASS 2, FACING DOWN: BODY WITH ITS PARTS AND THE BLOWHOLE GLOW, SWAY FRAMES 0-2 (TAIL END -6, 0, +6 DEG)",
         whole, 1, False),
        ("PASS 2 BODY SPRITES ALONE (LEVIATHAN-DOWN 0-2)", bodies, 1, False),
        ("EVERY PART WRECKED, SWAY FRAMES 0-2", wreck, 1, False),
        ("THE DATA'S HIT BOXES (BODY WHITE, PARTS AMBER, BLOWHOLE VIOLET) OVER FRAME 1: INTACT, WRECKED, BODY",
         boxes, 1, False),
        ("VENTS 1-4, THEN WRECKED 1-4", [intact[n][0] for n in VENTS] + [wrecked[n][0] for n in VENTS], 4, False),
        ("FIN LEFT, RIGHT, THEN WRECKED", [intact[n][0] for n in FINS] + [wrecked[n][0] for n in FINS], 3, False),
        ("FLUKE SWAY FRAMES 0-2, THEN WRECKED 0-2", intact["fluke"] + wrecked["fluke"], 2, False),
        ("BLOWHOLE GLOW (ADDITIVE)", [glow], 4, True),
        (f"PASS 1, HIGH-AIR CROSSING: HEADING {CROSS_HEADING:.2f} DEG, {CROSS_SCALE}X, SWAY FRAMES 0-2",
         cross, 1, False),
        (f"PASS 1 FRAME 1 WITH LEVEL 03'S CROSS PATH (DATA.YAML, {start} TO {end}, "
         f"{course:.2f} DEG FROM STRAIGHT DOWN) AS A GREEN ARROW", [flight_arrow(on_plate(cross[1]), start, end)],
         1, False)], width=width, batch=BATCH)
    gif = []
    stages = [{}, {"vent 2": True, "left fin": True}, {n: True for n in PARTS}]
    for s, broken in enumerate(stages):
        for i in range(24):
            k = [0, 1, 2, 1][(i // 3) % 4]
            parts = {n: (wrecked if broken.get(n) else intact)[n] for n in PARTS}
            level = 0.55 + 0.45 * np.sin(2 * np.pi * (i + 24 * s) / 12)
            gif.append(on_plate(compose(bodies[k], pick(parts, k), k, glow, level)))
    artkit.save_review(sheet, gif, UNIT / "concept", "leviathan", fps=10)


if __name__ == "__main__":
    if "--review" in sys.argv[1:]:
        pass
    elif "--cross" in sys.argv[1:]:
        build_cross()
    else:
        build()
    review()
