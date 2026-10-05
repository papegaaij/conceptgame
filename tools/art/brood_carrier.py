#!/usr/bin/env python3
"""Production art: the Brood Carrier, the Act 1 boss of Level 07 (design/enemies/bosses/brood-carrier;
M4 part G batch, step A1). The game draws it with HullBossLooks (game/.../render/HullBossLooks.java).

Outputs (assets/sprites/ and assets/pivots/), every sprite under one fixed key light at the top
left of the screen, nothing lit rotated or mirrored afterwards:
  brood-carrier-hull_0..8.png      the hull in its turn (user decision D1 of part G): frame 0
                                   nose-down (288x626, phase 1 on high-air; the game scales it by
                                   1.25 there and casts the shadow from its alpha), frames 1-7 the
                                   90-degree turn in place in even steps of 11.25 degrees, the head
                                   swinging to the screen's right, frame 8 broadside (626x288,
                                   phases 2-3). Each frame is a render of the turned model on a
                                   canvas centred on the hull's centre, just large enough. The
                                   turn frames carry the parts (closed sacs, closed iris, the
                                   mandible turret along the head); at the two ends the game draws
                                   the part sprites below over the hull at the data's offsets
  brood-carrier-sac-down_0..4.png  40x40 a bay sac in its crater, nose-down: 0 closed (the glossy
                                   dark sac sunk in the pit, faint lime veins, as the concept's),
                                   1-2 swelling up out of the pit as its glow rises, 3 open (the
                                   bright weak point), 4 burst (the ruptured sac charred, shards of
                                   its shell at the rim, lime ichor welling in the pit)
  brood-carrier-sac-side_0..4.png  the same, broadside (the sac's long axis along the hull)
  brood-carrier-iris_0..4.png      64x64 the plate iris over the core: 0 closed (six plates), 1-3
                                   opening (the plates slide back under the collar), 4 open over
                                   the lime core; one set for both poses (a radial part)
  brood-carrier-core-glow.png      64x64 the core's lime glow (additive), pulsed by the game
  brood-carrier-turret_0..16.png   40x40 the head's mandible turret (data `mandibles`, fire only):
                                   the maw with its inner mandible pair at 17 headings, steps
                                   -8..8 of 11.25 degrees clockwise from straight down (`headings`
                                   in the pivot file, as the frigate's heads)
  pivots/brood-carrier.json        the heading set, the pose sizes, the parts' offsets per pose;
                                   its "death" entry is brood_carrier_death.py's and is kept

Every part sprite is centred on its part's offset from the data (`part_list` nose-down, `boss.poses`
broadside; px from the hull centre, dx right, dy up): the bay craters, the core collar and the
turret socket are laid out to those offsets in the model, so a part sprite drawn there covers the
hull's own (closed) part exactly. A sac or the iris sprite is the inside of its crater or collar
(a disc mask), rendered with the same geometry and light, so the rim stays the hull's.

Model: the chosen round-04 concept (tools/concept/render/enemy_models.py `brood_carrier`, its
round-04 materials `brood_carrier_scheme_mats`: teal-black chitin, teal glow, lime weak points):
the seven hull segments, the dorsal plates, the flight membranes, the head with its eye clusters,
outer mandibles and limbs, the tail tendrils, 288 px across 2.3 model units (125.2 px per unit) as
the concept sprite. Production detail: the bay lips become raised craters the sacs swell out of, the
core sits under a raised collar, a thin teal seam runs along the spine between the plates, bone
spikes stand between the dorsal plates, the head has a socket for the separate mandible turret.
Rendered at 4x, one palette of 48 colours over every solid sprite (14 of them cut from the lime and
teal glows alone, told apart from the hull's lit greens by hue).

Run: python3 tools/art/brood_carrier.py [--cached | --review]   (~25 min on 20 cores, 6 renders at a
time, up to ~1 GB each; the unquantised renders are kept in ~/.cache/terran-vanguard/brood-carrier,
outside the repo: --cached maps them again without rendering, --review only rebuilds the review
sheet and loop)
"""
import json
import sys
from concurrent.futures import ProcessPoolExecutor
from dataclasses import replace
from pathlib import Path

import numpy as np
import yaml
from PIL import Image, ImageDraw

import artkit
from artkit import DESIGN, TAU, sprite

import vfx_r08 as v8  # noqa: E402  (concept script, imported unchanged)
from render import boss_models as bm  # noqa: E402
from render import enemy_models as em  # noqa: E402
from render import sdf  # noqa: E402
from render.enemy_models import B_BAY, B_BONE, B_CORE, B_EYE, B_HULL, B_LIMB, B_PLATE, B_SAC  # noqa: E402
from render.enemy_rigs import model_space_materials  # noqa: E402
from render.sdf import (Material, mirror_x, rotate_z, sd_capsule, sd_cylinder_z, sd_ellipsoid,  # noqa: E402
                        sd_plate, sd_sphere, union)

SCRIPT = "brood_carrier.py"
BATCH = "M4 part G batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
UNIT = DESIGN / "enemies" / "bosses" / "brood-carrier"
SLUG = "brood-carrier"
REVIEW_ROUND = "r25"
FACTOR = 4
W, H = 288, 626                 # the hull nose-down, px (data `size`)
S = W / 2.3                     # px per model unit: the concept sprite's 2.3 units across 288 px
TURN_FRAMES = 9                 # nose-down, 7 turn frames, broadside
COLOURS = 48
ACCENT_COLOURS = 14             # of the palette cut from the lime and teal glows alone
CACHE = Path.home() / ".cache" / "terran-vanguard" / SLUG     # the unquantised renders (outside the repo)
SAC_SIZE, IRIS_SIZE, TURRET_SIZE, GLOW_SIZE = 40, 64, 40, 64
SAC_STAGES = [0.0, 1 / 3, 2 / 3, 1.0, "burst"]
IRIS_STAGES = [0.0, 0.25, 0.5, 0.75, 1.0]
STEPS, REACH = 32, 8            # the turret heading set: steps -REACH..REACH of 360/STEPS degrees
# The key light at a fixed place relative to the hull's centre (model units, screen frame), so every
# frame of the turn and every part is lit from the same point of the screen: between the concept's
# nose-down light (its 2.3-unit canvas) and its broadside one (5 units), so the 5-unit hull keeps the
# concept's top-left gradient in both poses without a hot spot over its tail.
LIGHT = np.asarray(sdf.KEY_POS) * (3.6 / 2.3)
CHAR, ICHOR = 8, 9              # extra material slots: charred tissue, wet lime ichor


def spec():
    return yaml.safe_load((UNIT / "data.yaml").read_text(encoding="utf-8"))


SPEC = spec()
PART = {p["name"]: p for p in SPEC["part_list"]}
BROADSIDE = SPEC["boss"]["poses"][0]["offsets"]
assert tuple(SPEC["size"]) == (W, H)
BAYS = [f"bay {n} {side}" for n in range(1, 5) for side in ("left", "right")]


def concept_xy(name):
    """A part's data offset (px, dx right, dy up, nose-down) in the concept model's frame (head at +y
    before it is turned to face down; mirrored in x): x = |dx| / S, y = -dy / S."""
    dx, dy = PART[name]["offset"]
    return abs(dx) / S, -dy / S


BAY_XY = [concept_xy(f"bay {n} right") for n in range(1, 5)]
CORE_XY = concept_xy("core")
TURRET_XY = concept_xy("mandibles")


# --------------------------------------------------------------------------- materials

def materials(glow=1.0, sac_open=0.0, core_open=0.0):
    """The concept's round-04 materials (teal-black chitin, teal glow, lime weak points) with the
    production additions: the teal seam along the spine on the hull, the sacs' glow growing with
    their opening, charred tissue and wet lime ichor (the bursts)."""
    mats = em.brood_carrier_scheme_mats(glow, sac_open, core_open)
    mid, dark, bone, gl, weak = em.scheme_colors("brood-carrier")
    seam = em._seam(0.022)

    def spine(p, n):
        # along the hull's back between the plates, not over the head or the tail's tip
        return seam(p, n) * (np.abs(p[:, 1]) < 1.7) * 0.7
    mats[B_HULL] = replace(mats[B_HULL], emission=tuple(gl * glow), emission_pattern=spine)
    veins = em._veins(28, 1.1, 0.6)
    level = 0.55 * sac_open ** 1.5

    def sac_glow(p, n):
        return level + (0.45 + 1.0 * sac_open) * veins(p, n)
    mats[B_SAC] = replace(mats[B_SAC], emission=tuple(weak * glow), emission_pattern=sac_glow)
    mats[B_CORE] = replace(mats[B_CORE], emission=tuple(weak * (0.55 + 0.45 * core_open) * glow))
    wet = em._veins(24, 0.4, 0.8)
    return mats + [Material((0.09, 0.08, 0.07), metal=0.1, shininess=20, spec=0.2),
                   Material(tuple(np.array(weak) * 0.35), metal=0.0, shininess=120, spec=1.3,
                            emission=tuple(weak * 0.9 * glow),
                            emission_pattern=lambda p, n: 0.45 + 0.55 * wet(p, n))]


# --------------------------------------------------------------------------- the hull

SEGMENTS = ((1.55, 0.62), (1.05, 0.82), (0.5, 0.92), (-0.05, 0.95), (-0.6, 0.9), (-1.15, 0.76), (-1.62, 0.56))
PLATES = ((1.1, 0.82), (0.55, 0.92), (-0.62, 0.9), (-1.15, 0.76))
SPIKES = (1.36, 0.83, -0.89, -1.42)


def body(p, q):
    """The hull without its parts (concept frame: head +y, q mirrored), from the concept model."""
    segs = [(sd_ellipsoid(p, (0, y, 0.0), (w, 0.42, 0.34)), B_HULL) for y, w in SEGMENTS]
    d, m = union(*segs, k=0.16)
    plates = [(sd_ellipsoid(p, (0, y, 0.2), (w * 0.62, 0.3, 0.2)), B_PLATE) for y, w in PLATES]
    d, m = union((d, m), *plates, k=0.05)
    for y0 in (1.25, 0.1, -1.05):
        fin = [(0.8, y0 + 0.28), (1.1, y0 + 0.05), (1.08, y0 - 0.38), (0.78, y0 - 0.3)]
        d, m = union((d, m), (sd_plate(q, fin, -0.08, 0.035, 0.01,
                                       taper=lambda x, y: np.clip(2.2 - 1.8 * x, 0.3, 1)), B_LIMB), k=0.08)
    d, m = union((d, m),
                 (sd_ellipsoid(p, (0, 2.0, 0.0), (0.42, 0.34, 0.26)), B_HULL),
                 (sd_capsule(q, (0.22, 2.1, 0.0), (0.46, 2.42, -0.02), 0.1, 0.04), B_BONE),
                 (sd_capsule(q, (0.3, 1.8, 0.05), (0.62, 1.9, 0.0), 0.08, 0.05), B_LIMB), k=0.08)
    tend = [(sd_capsule(q, (x, -1.8, 0.0), (x + 0.1, -2.4, -0.05), 0.08, 0.02), B_LIMB) for x in (0.12, 0.32)]
    d, m = union((d, m), *tend, k=0.06)
    # bone spikes between the dorsal plates, leaning back towards the tail
    spikes = [(sd_capsule(p, (0, y, 0.3), (0, y - 0.1, 0.43), 0.055, 0.012), B_BONE) for y in SPIKES]
    d, m = union((d, m), *spikes, k=0.03)
    return union((d, m),
                 (sd_sphere(q, (0.16, 2.2, 0.18), 0.06), B_EYE),
                 (sd_sphere(q, (0.3, 2.08, 0.12), 0.045), B_EYE),
                 (sd_capsule(q, (0.52, 1.92, 0.02), (0.6, 2.1, 0.0), 0.05, 0.03), B_EYE), k=0.01)


def surface_z(x, y):
    """The hull's top at (x, y) in the concept frame (a downward march on the body alone)."""
    p = np.array([[x, y, 1.0]])
    for _ in range(200):
        d, _ = body(p, mirror_x(p))
        if d[0] < 1e-4:
            break
        p[0, 2] -= d[0] * 0.8
    return float(p[0, 2])


BAY_Z = [surface_z(x, y) for x, y in BAY_XY]
CORE_Z = surface_z(*CORE_XY)
TURRET_Z = surface_z(*TURRET_XY)

# --- a bay: a raised crater on the flank with a lidded sac in it (local frame, the centre on the
# hull's surface; the sac's long axis along the hull)
R_PIT = 0.155                   # the crater's opening: the sac sprite is this disc


def bay_mound(pl):
    return sd_ellipsoid(pl, (0, 0, -0.03), (0.27, 0.26, 0.15))


def bay_pit(pl):
    return sd_cylinder_z(pl, (0, 0, 0.3), R_PIT, 0.36)


def bay_contents(pl, stage):
    """The pit's floor and its sac at ``stage`` (0 closed .. 1 open, or "burst")."""
    d, m = sd_cylinder_z(pl, (0, 0, -0.08), R_PIT + 0.02, 0.02), B_LIMB
    if stage == "burst":
        a = np.arctan2(pl[:, 1], pl[:, 0])
        rag = 0.02 * np.sin(a * 7 + 0.6) + 0.012 * np.sin(a * 13)
        # the ruptured shell: a charred, ragged collar standing round the pit's edge
        shell = np.abs(sd_ellipsoid(pl, (0, 0, -0.07), (0.135, 0.145, 0.12))) - 0.014
        shell = np.maximum(shell, pl[:, 2] - (0.0 + rag))
        d, m = union((d, m), (shell, CHAR), k=0.01)
        # wet lime ichor welling up inside it, in lumps
        d, m = union((d, m), (sd_ellipsoid(pl, (0, 0, -0.06), (0.12, 0.13, 0.05)), ICHOR), k=0.02)
        rng = np.random.default_rng(11)
        blobs = [(sd_sphere(pl, (r * np.cos(t), r * np.sin(t), -0.02), rr), ICHOR)
                 for r, t, rr in zip(rng.uniform(0.0, 0.08, 6), rng.uniform(0, TAU, 6), rng.uniform(0.025, 0.045, 6))]
        d, m = union((d, m), *blobs, k=0.025)
        # shards of the sac's shell, charred, at the rim
        for t in (0.7, 3.6):
            fl = rotate_z(pl, t) - np.array([0.12, 0.0, 0.03])
            d, m = union((d, m), (sd_ellipsoid(fl, (0, 0, 0), (0.04, 0.06, 0.018)), CHAR), k=0.01)
        return d, m
    o = float(stage)
    # the sac fills the pit, sunk and dark when closed, swelling up out of it as it opens
    return union((d, m), (sd_ellipsoid(pl, (0, 0, -0.07 + 0.03 * o), (0.142, 0.15, 0.09 + 0.09 * o)), B_SAC), k=0.015)


# --- the core: a raised collar round a pit with the core sphere under six plates
R_COLLAR = 0.25                 # the collar's opening: the iris sprite is this disc


def core_collar(pl):
    return sd_ellipsoid(pl, (0, 0, -0.02), (0.37, 0.37, 0.12))


def core_pit(pl):
    return sd_cylinder_z(pl, (0, 0, 0.3), R_COLLAR, 0.42)


def core_contents(pl, o, burst=False):
    """The pit's floor, the core sphere and the six iris plates opened by ``o`` (0..1)."""
    d, m = sd_cylinder_z(pl, (0, 0, -0.14), R_COLLAR + 0.02, 0.03), B_LIMB
    if burst:
        d, m = union((d, m), (sd_ellipsoid(pl, (0, 0, -0.1), (0.2, 0.2, 0.08)), ICHOR), k=0.02)
        torn = np.abs(sd_sphere(pl, (0, 0, -0.12), 0.18)) - 0.015
        torn = np.maximum(torn, -(pl[:, 2] + 0.08))
        d, m = union((d, m), (torn, CHAR), k=0.01)
    else:
        d, m = union((d, m), (sd_sphere(pl, (0, 0, -0.12), 0.2), B_CORE), k=0.02)
    plates = []
    for t in np.linspace(0, TAU, 6, endpoint=False) + TAU / 12:
        fl = rotate_z(pl, t)
        r = 0.12 + 0.2 * o
        plates.append((sd_ellipsoid(fl, (r, 0, 0.07 - 0.06 * o), (0.16, 0.12, 0.045)), B_PLATE))
    return union((d, m), *plates, k=0.0)


# --- the head's mandible turret: a maw with a pair of inner mandibles (forward +y)

def turret_model(pl):
    d, m = sd_ellipsoid(pl, (0, -0.01, 0.0), (0.085, 0.08, 0.06)), B_PLATE
    d, m = union((d, m), (sd_cylinder_z(pl, (0, 0.035, 0.05), 0.04, 0.02), B_LIMB), k=0.01)
    d, m = union((d, m), (sd_sphere(pl, (0, 0.04, 0.045), 0.026), B_EYE), k=0.005)
    q = mirror_x(pl)
    d, m = union((d, m), (sd_capsule(q, (0.05, 0.02, 0.03), (0.075, 0.1, 0.03), 0.026, 0.02), B_BONE),
                 (sd_capsule(q, (0.075, 0.1, 0.03), (0.035, 0.155, 0.02), 0.02, 0.009), B_BONE), k=0.012)
    return d, m


def turret_socket(pl):
    return sd_cylinder_z(pl, (0, 0, 0.0), 0.075, 0.06)


def hull_model(stage=0.0, iris=0.0, turret=True, core_burst=False, turret_cw=0.0):
    """The whole carrier in the screen frame nose-down (x right, y up, the head at -y): its sacs at
    ``stage``, the iris opened by ``iris``, the mandible turret turned ``turret_cw`` radians clockwise
    from the head's direction (or only its socket)."""
    def scene(p):
        pc = rotate_z(p, np.pi)         # the concept's face-down turn
        q = mirror_x(pc)
        d, m = body(pc, q)
        # the bay mounds, then their pits cut and filled
        for (bx, by), bz in zip(BAY_XY, BAY_Z):
            pl = q - np.array([bx, by, bz])
            d, m = union((d, m), (bay_mound(pl), B_BAY), k=0.06)
        for (bx, by), bz in zip(BAY_XY, BAY_Z):
            pl = q - np.array([bx, by, bz])
            d = np.maximum(d, -bay_pit(pl))
            d, m = union((d, m), bm.bounded(pl, sd_sphere(pl, (0, 0, 0), 0.3), 0.08,
                                            lambda pp: bay_contents(pp, stage)), k=0.0)
        cl = pc - np.array([CORE_XY[0], CORE_XY[1], CORE_Z])
        d, m = union((d, m), (core_collar(cl), B_PLATE), k=0.05)
        d = np.maximum(d, -core_pit(cl))
        d, m = union((d, m), bm.bounded(cl, sd_sphere(cl, (0, 0, -0.05), 0.5), 0.08,
                                        lambda pp: core_contents(pp, iris, core_burst)), k=0.0)
        tl = pc - np.array([TURRET_XY[0], TURRET_XY[1], TURRET_Z])
        d = np.maximum(d, -turret_socket(tl))
        d, m = union((d, m), (sd_cylinder_z(tl, (0, 0, -0.05), 0.08, 0.012), B_LIMB), k=0.0)
        if turret:
            tt = rotate_z(tl, -turret_cw) if turret_cw else tl
            d, m = union((d, m), bm.bounded(tt, sd_sphere(tt, (0, 0, 0), 0.25), 0.08, turret_model), k=0.0)
        return d, m
    return scene


# --------------------------------------------------------------------------- rendering

def render(scene, mats, angle, size, centre=(0.0, 0.0), factor=None, shadows=True, steps=150):
    """Ray-march ``scene`` turned counter-clockwise by ``angle`` (its patterns with it) onto a
    ``size`` px canvas at S px per unit, the key light at LIGHT relative to the model's origin."""
    w, h = size
    factor = artkit.factor_for(w, h) if factor is None else factor
    extent = w / S
    turned = (lambda p: scene(rotate_z(p, angle))) if angle else scene
    mats = model_space_materials(mats, angle) if angle else mats
    light = (LIGHT - np.array([centre[0], centre[1], 0.0])) / (extent / 2.3)
    return sdf.render(turned, mats, (w * factor, h * factor), extent, center=centre,
                      key_pos=tuple(light), shadows=shadows, steps=steps)


def turn_angle(k):
    """Hull frame k's turn, radians counter-clockwise: 0 nose-down .. pi/2 broadside."""
    return (np.pi / 2) * k / (TURN_FRAMES - 1)


def turn_canvas(k):
    """An even canvas centred on the hull's centre that holds the hull turned for frame k: the
    data's size at both ends, between them the turned nose-down silhouette's box (rotated corners
    of the hull's outline, from a cheap 1x mask) plus a margin."""
    if k == 0:
        return W, H
    if k == TURN_FRAMES - 1:
        return H, W
    mask = outline()
    img = Image.fromarray(mask).rotate(np.degrees(turn_angle(k)), expand=True)
    a = np.array(img) > 0
    ys, xs = np.nonzero(a)
    cx, cy = img.width / 2, img.height / 2
    hw = max(cx - xs.min(), xs.max() + 1 - cx) + 6
    hh = max(cy - ys.min(), ys.max() + 1 - cy) + 6
    return 2 * int(np.ceil(hw)), 2 * int(np.ceil(hh))


_OUTLINE = []


def outline():
    if not _OUTLINE:
        hi = render(hull_model(), materials(), 0.0, (W, H), factor=1, shadows=False, steps=90)
        _OUTLINE.append(((hi[..., 3] > 0.5) * 255).astype(np.uint8))
    return _OUTLINE[0]


def crop_centred(img, size):
    """The frame cropped to an even box centred on the canvas centre (the hull's centre), as small
    as its pixels allow; the two end frames keep the data's sizes."""
    if size in ((W, H), (H, W)):
        return img
    a = np.array(img)[..., 3] > 0
    ys, xs = np.nonzero(a)
    cx, cy = img.width // 2, img.height // 2
    hw = max(cx - xs.min(), xs.max() + 1 - cx)
    hh = max(cy - ys.min(), ys.max() + 1 - cy)
    return img.crop((cx - hw, cy - hh, cx + hw, cy + hh))


def disc_mask(img, radius_px):
    """Keeps the pixels whose centres lie within ``radius_px`` of the canvas centre."""
    a = np.array(img)
    h, w = a.shape[:2]
    yy, xx = np.mgrid[0:h, 0:w]
    inside = (xx + 0.5 - w / 2) ** 2 + (yy + 0.5 - h / 2) ** 2 <= radius_px ** 2
    a[~inside] = 0
    return Image.fromarray(a, "RGBA")


def sac_scene(stage):
    """A bay alone (mound, pit and contents) at the local origin, in the screen frame nose-down."""
    def scene(p):
        pl = rotate_z(p, np.pi) - np.array([0.0, 0.0, 0.0])
        d = np.maximum(bay_mound(pl), -bay_pit(pl))
        dc, mc = bay_contents(pl, stage)
        return union((d, B_BAY), (dc, mc), k=0.0)
    return scene


def iris_scene(o):
    def scene(p):
        pl = rotate_z(p, np.pi)
        d = np.maximum(core_collar(pl), -core_pit(pl))
        dc, mc = core_contents(pl, o)
        return union((d, B_PLATE), (dc, mc), k=0.0)
    return scene


def turret_scene(p):
    return turret_model(rotate_z(p, np.pi))


def job(kind, k):
    if kind == "hull":
        size = turn_canvas(k)
        ends = k in (0, TURN_FRAMES - 1)
        hi = render(hull_model(turret=not ends), materials(), turn_angle(k), size)
        return crop_centred(artkit.native(hi, FACTOR), size)
    if kind in ("sac-down", "sac-side"):
        stage = SAC_STAGES[k]
        o = 0.0 if stage == "burst" else stage
        angle = 0.0 if kind == "sac-down" else np.pi / 2
        hi = render(sac_scene(stage), materials(sac_open=o), angle, (SAC_SIZE, SAC_SIZE), steps=120)
        return disc_mask(artkit.native(hi, artkit.factor_for(SAC_SIZE, SAC_SIZE)), R_PIT * S + 0.5)
    if kind == "iris":
        o = IRIS_STAGES[k]
        hi = render(iris_scene(o), materials(core_open=o), 0.0, (IRIS_SIZE, IRIS_SIZE), steps=120)
        return disc_mask(artkit.native(hi, artkit.factor_for(IRIS_SIZE, IRIS_SIZE)), R_COLLAR * S + 0.5)
    if kind == "turret":
        h = TAU * (k - REACH) / STEPS           # clockwise from straight down
        hi = render(turret_scene, materials(), -h, (TURRET_SIZE, TURRET_SIZE), steps=120)
        return artkit.native(hi, artkit.factor_for(TURRET_SIZE, TURRET_SIZE))
    raise ValueError(kind)


def glowing(pixels):
    """The lime and teal glow pixels (the sacs, the core, the eyes, the seams), told apart from the
    teal-black hull's own lit greens by hue: lime has far more red than blue, the teal glow almost
    no red at full green."""
    r, g, b = pixels[:, 0], pixels[:, 1], pixels[:, 2]
    lime = (g >= 90) & (r >= b + 45) & (g >= r)
    teal = (g >= 160) & (r <= 60) & (g >= b)
    return lime | teal


def quantize_glow(frames, colors=COLOURS, accent=ACCENT_COLOURS):
    """One palette of ``colors`` over every frame, ``accent`` of its entries cut from the glow pixels
    alone (a plain median cut by pixel count merges the small lime sacs into the large teal hull);
    a glow pixel maps to the nearest glow entry, any other to the nearest hull entry."""
    arrays = [np.array(f.convert("RGBA")) for f in frames]
    pixels = np.concatenate([a[a[..., 3] > 0][:, :3] for a in arrays]).astype(np.int32)
    vivid = glowing(pixels)

    def cut(px, n):
        img = Image.fromarray(px.astype(np.uint8).reshape(-1, 1, 3), "RGB")
        pal = img.quantize(colors=n, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE)
        used = sorted(set(np.array(pal).ravel().tolist()))
        return [pal.getpalette()[3 * i:3 * i + 3] for i in used]
    main = cut(pixels[~vivid], colors - accent)
    glow = cut(pixels[vivid], accent) if vivid.any() else main[:1]

    def palette(entries):
        entries = entries + [entries[0]] * (256 - len(entries))
        pal = Image.new("P", (1, 1))
        pal.putpalette([c for e in entries for c in e])
        return pal
    pal_main, pal_glow = palette(main), palette(glow)
    out = []
    for a in arrays:
        rgb = Image.fromarray(np.ascontiguousarray(a[..., :3]), "RGB")
        # each pixel takes its nearest colour among its own class's entries
        to_main = np.array(rgb.quantize(palette=pal_main, dither=Image.Dither.NONE).convert("RGB"))
        to_glow = np.array(rgb.quantize(palette=pal_glow, dither=Image.Dither.NONE).convert("RGB"))
        mask = glowing(a[..., :3].reshape(-1, 3).astype(np.int32)).reshape(a.shape[:2])
        mapped = np.where(mask[..., None], to_glow, to_main)
        result = np.dstack([mapped, a[..., 3]])
        result[a[..., 3] == 0] = 0
        out.append(Image.fromarray(result.astype(np.uint8), "RGBA"))
    return out


def core_glow():
    weak = em.scheme_colors(SLUG)[4] * 255
    cv = v8.Canvas(GLOW_SIZE, GLOW_SIZE)
    d = cv.dist(GLOW_SIZE / 2, GLOW_SIZE / 2)
    cv.add(tuple(weak), v8.gauss(d, 15.0) * 0.85)
    cv.add((235, 255, 200), v8.gauss(d, 6.0) * 0.8)
    return artkit.quantize_set([artkit.additive(cv.image())], 16)[0]


SETS = [("hull", TURN_FRAMES), ("sac-down", len(SAC_STAGES)), ("sac-side", len(SAC_STAGES)),
        ("iris", len(IRIS_STAGES)), ("turret", 2 * REACH + 1)]


def build(cached=False):
    """Renders every frame (or, ``cached``, takes the unquantised renders of the last run from
    CACHE), then maps them all to one palette and writes them."""
    jobs = [(kind, k) for kind, n in SETS for k in range(n)]
    CACHE.mkdir(parents=True, exist_ok=True)
    if cached:
        results = {j: Image.open(CACHE / f"{j[0]}_{j[1]}.png").convert("RGBA") for j in jobs}
    else:
        # the big hull frames first, at most six at a time (2-4 GB each)
        with ProcessPoolExecutor(max_workers=6) as pool:
            results = dict(zip(jobs, pool.map(job, *zip(*jobs))))
        for (kind, k), img in results.items():
            img.save(CACHE / f"{kind}_{k}.png")
    flat = [results[j] for j in jobs]
    mapped = quantize_glow(flat)        # one palette over the whole boss
    at = 0
    for kind, n in SETS:
        artkit.write_frames(f"{SLUG}-{kind}", mapped[at:at + n], SOURCE)
        at += n
    artkit.write_frames(f"{SLUG}-core-glow", [core_glow()], SOURCE, single=True)
    old = artkit.PIVOTS / f"{SLUG}.json"
    keep = {k: v for k, v in json.loads(old.read_text(encoding="utf-8")).items()
            if k == "death"} if old.exists() else {}
    artkit.write_pivots(SLUG, SOURCE, {
        "headings": {"steps": STEPS, "first": -REACH, "count": 2 * REACH + 1},
        "hull": {"frames": TURN_FRAMES, "turn_degrees": 90, "head": "right",
                 "sizes": [list(results[("hull", k)].size) for k in range(TURN_FRAMES)]},
        "parts": {"nose-down": {p["name"]: p["offset"] for p in SPEC["part_list"]},
                  "broadside": BROADSIDE},
        **keep})


# --------------------------------------------------------------------------- review

def load_art():
    art = {kind: artkit.load_frames(f"{SLUG}-{kind}") for kind, _ in SETS}
    art["glow"] = artkit.load_frames(f"{SLUG}-core-glow")[0]
    return art


def offsets(pose):
    """The parts' offsets in a pose: nose-down from the part list, broadside from the data's pose."""
    if pose == 0:
        return {p["name"]: tuple(p["offset"]) for p in SPEC["part_list"]}
    return {name: tuple(v) for name, v in BROADSIDE.items()}


def heading_frame(phi):
    """The turret frame pointing at ``phi`` (radians counter-clockwise, y up), as the game picks it."""
    cw = -np.pi / 2 - phi
    cw -= TAU * np.floor((cw + np.pi) / TAU)
    return int(np.clip(round(cw / (TAU / STEPS)) + REACH, 0, 2 * REACH))


def compose(art, frame, sacs=None, iris=0, glow=0.0, ship=None, canvas=(700, 700), turret_to=None):
    """The carrier as the game draws it, centred on the canvas: the hull frame, and at the two ends
    of the turn every part sprite at its data offset (``sacs``: per bay its stage index)."""
    img = Image.new("RGBA", canvas, artkit.PLATE)
    cx, cy = canvas[0] // 2, canvas[1] // 2
    hull = art["hull"][frame]
    img.alpha_composite(hull, (cx - hull.width // 2, cy - hull.height // 2))
    if 0 < frame < TURN_FRAMES - 1:
        return img
    pose = 0 if frame == 0 else 1
    offs = offsets(pose)
    sac = art["sac-down" if pose == 0 else "sac-side"]
    for i, name in enumerate(BAYS):
        dx, dy = offs[name]
        stage = 0 if sacs is None else sacs[i]
        f = sac[stage]
        img.alpha_composite(f, (cx + dx - f.width // 2, cy - dy - f.height // 2))
    dx, dy = offs["core"]
    f = art["iris"][iris]
    img.alpha_composite(f, (cx + dx - f.width // 2, cy - dy - f.height // 2))
    if glow > 0:
        g = np.array(art["glow"]).astype(np.float64)
        g[..., :3] *= glow
        g = Image.fromarray(g.astype(np.uint8), "RGBA")
        img = artkit.add_light(img, g, (cx + dx - g.width // 2, cy - dy - g.height // 2))
    tx, ty = offs["mandibles"]
    sx, sy = ship if ship is not None else (tx, ty - 300)
    phi = np.arctan2(sy - ty, sx - tx) if turret_to is None else turret_to
    f = art["turret"][heading_frame(phi)]
    img.alpha_composite(f, (cx + tx - f.width // 2, cy - ty - f.height // 2))
    return img


def hit_boxes(img, pose, canvas=(700, 700)):
    """The data's hit boxes over a composed pose: hull white, sacs amber, core violet, turret blue."""
    out = img.copy()
    d = ImageDraw.Draw(out)
    cx, cy = canvas[0] // 2, canvas[1] // 2
    bw, bh = SPEC["hitbox"] if pose == 0 else SPEC["boss"]["poses"][0]["hitbox"]
    d.rectangle([cx - bw // 2, cy - bh // 2, cx + bw // 2 - 1, cy + bh // 2 - 1], outline=(255, 255, 255, 255))
    colours = {"destroyable": (255, 170, 40, 255), "vital": (200, 120, 255, 255), "armoured": (120, 200, 255, 255)}
    for name, (dx, dy) in offsets(pose).items():
        w, h = PART[name]["hitbox"]
        col = colours[PART[name]["kind"]]
        d.rectangle([cx + dx - w // 2, cy - dy - h // 2, cx + dx + w // 2 - 1, cy - dy + h // 2 - 1], outline=col)
        d.line([cx + dx - 3, cy - dy, cx + dx + 3, cy - dy], fill=col)
        d.line([cx + dx, cy - dy - 3, cx + dx, cy - dy + 3], fill=col)
    return out


def review():
    artkit.REVIEW_ROUND = REVIEW_ROUND
    art = load_art()
    open_all = [3] * 8
    mixed = [3, 3, 0, 1, 2, 4, 0, 4]
    down = compose(art, 0)
    side = compose(art, TURN_FRAMES - 1)
    whole = [down, compose(art, 0, sacs=open_all, turret_to=-np.pi / 2), side,
             compose(art, TURN_FRAMES - 1, sacs=mixed, iris=4, glow=1.0, ship=(-150, -330))]
    whole = [w.crop((0, 30, 700, 670)) if i < 2 else w.crop((20, 180, 680, 520)) for i, w in enumerate(whole)]
    boxes = [hit_boxes(down, 0).crop((0, 30, 700, 670)), hit_boxes(side, 1).crop((20, 180, 680, 520))]
    turn = [compose(art, k, canvas=(660, 660)) for k in range(TURN_FRAMES)]
    sheet = artkit.review_sheet("BROOD CARRIER - FINAL SPRITES", [
        ("AS THE GAME DRAWS IT (1X): NOSE-DOWN CLOSED, NOSE-DOWN SACS OPEN; BROADSIDE CLOSED, BROADSIDE PHASE 3 "
         "(SACS OPEN, OPENING, BURST; IRIS OPEN, CORE GLOW; TURRET AT THE SHIP)", whole, 1, False),
        ("THE DATA'S HIT BOXES AND OFFSETS: HULL WHITE, SACS AMBER, CORE VIOLET, MANDIBLE TURRET BLUE", boxes, 1, False),
        (f"THE TURN: HULL FRAMES 0-{TURN_FRAMES - 1}, 11.25 DEG APART, HEAD SWINGING RIGHT (THE TURN FRAMES "
         "CARRY THE PARTS)", turn[:3], 1, False),
        ("", turn[3:6], 1, False),
        ("", turn[6:], 1, False),
        ("BAY SAC NOSE-DOWN: CLOSED, OPENING, OPENING, OPEN, BURST", art["sac-down"], 4, False),
        ("BAY SAC BROADSIDE: CLOSED, OPENING, OPENING, OPEN, BURST", art["sac-side"], 4, False),
        ("PLATE IRIS: CLOSED, OPENING X3, OPEN; CORE GLOW (ADDITIVE)", art["iris"], 3, False),
        ("", [art["glow"]], 3, True),
        (f"MANDIBLE TURRET, {2 * REACH + 1} HEADINGS (-90 TO +90 DEG FROM DOWN, CLOCKWISE)", art["turret"], 3, False),
    ], width=2800, batch=BATCH)
    artkit.save_review(sheet, review_loop(art), UNIT / "concept", SLUG, fps=10)


def review_loop(art):
    """The fight in brief at 10 fps: nose-down with the pairs opening head to tail, the turn, the
    broadside windows with the turret tracking a moving ship, the iris opening and the core glow."""
    frames = []
    canvas = (700, 700)
    for i in range(40):
        pair = (i // 10) % 4
        sacs = [0] * 8
        t = (i % 10)
        stage = [1, 2, 3, 3, 3, 3, 3, 2, 1, 0][t]
        sacs[2 * pair] = sacs[2 * pair + 1] = stage
        frames.append(compose(art, 0, sacs=sacs, ship=(60 * np.sin(i / 6), -330), canvas=canvas))
    for k in range(TURN_FRAMES):
        frames += [compose(art, k, canvas=canvas)] * 2
    wrecked = set()
    for i in range(60):
        sacs = [4 if b in wrecked else 0 for b in range(8)]
        pair = (i // 12) % 4
        t = i % 12
        stage = [0, 1, 2, 3, 3, 3, 3, 3, 2, 1, 0, 0][t]
        for b in (2 * pair, 2 * pair + 1):
            if b not in wrecked:
                sacs[b] = stage
        if t == 7 and pair in (0, 2):
            wrecked.add(2 * pair)
        ship = (-200 + 150 * np.sin(i / 9), -330)
        frames.append(compose(art, TURN_FRAMES - 1, sacs=sacs, ship=ship, canvas=canvas))
    for i in range(30):
        sacs = [4] * 8
        iris = min(4, i // 2)
        glow = min(1.0, i / 8) * (0.75 + 0.25 * np.sin(TAU * i / 8))
        frames.append(compose(art, TURN_FRAMES - 1, sacs=sacs, iris=iris, glow=glow,
                              ship=(-120 + 80 * np.sin(i / 5), -330), canvas=canvas))
    return frames


if __name__ == "__main__":
    if "--review" not in sys.argv[1:]:
        build(cached="--cached" in sys.argv[1:])
    review()
