#!/usr/bin/env python3
"""Production art, two variants: the evacuation shuttle, the civilian orbital shuttle with a CDF
evac stripe that Level 10 escorts (design/allies, Evacuation shuttle; M5 part D batch, concept
round 32). It had no concept, so this script proposes two looks, both at production quality (user
decision D10 = a); the user picked **a** (round 32, 2026-10-08), so a is in the game and b's review
files live in concept/rejected/.

  a  "Lifting body": a broad white blended-wing orbiter, a dark heat-shield nose cap, the cockpit
     glass forward, a row of lit cabin windows down each side of the spine, two canted tail fins,
     three engine bells; a rescue-orange evac chevron across both wings and the crawler's CDF-blue
     spine stripe; red and green wingtip navigation lights
  b  "Heavy lifter": a long pressurised cabin fuselage on straight stub wings with two ducted lift
     fans and a nacelle at each tip, a T-tail and two engine bells; a rescue-orange evac band round
     the fuselage and on the nacelle noses, the CDF-blue spine stripe, wingtip navigation lights

Outputs (the chosen variant a only, under the names the game loads; `--variant b` would write b's
instead; b lives only in its review files, so the atlas never carries an unclaimed second set):
  assets/sprites/evacuation-shuttle_0..4          64x40, flying UP the screen, the banking frames for
                                                  the lane drift: hard left -30, left -15, level,
                                                  right +15, hard right +30 degrees of roll (Rook's), seen
                                                  through a mild perspective camera (the Stormhawk's
                                                  way, so the raised wing reads); 40 colours shared
                                                  by every hull set below
  assets/sprites/evacuation-shuttle-damaged_0..4  64x40, the same banks below 50 % armour: scorched
                                                  hull, the left engine dead, a torn panel; the smoke
                                                  is the game's (ship-smoke puffs from the pivot)
  assets/sprites/evacuation-shuttle-lift_0..3     64x40, the liftoff ground -> low-air -> air, level:
                                                  the craft drawn 0.70 (on the pad, ground scale:
                                                  1 / 1.43 as the Ravager's leap), 0.775, 0.85
                                                  (about low-air), 0.925 x; then the level bank frame
                                                  (1.0) on air. No runtime scale on a lit frame
  assets/sprites/evacuation-shuttle-wreck_0..7    64x40, the powerless glide into `far`: lights and
                                                  engines dead, scorched, rolling over to 35 degrees
                                                  and yawing 15, drawn 1.0 -> 0.35 x (far's scale)
                                                  and darkened toward a cool haze (to 50 %), a step
                                                  per 1/8 of the glide; no debris
  assets/sprites/evacuation-shuttle-flame_0..2    6x12, additive blue-white engine flame, a 3-frame
                                                  flicker; its top centre (3, 0) on each engine
  assets/sprites/evacuation-shuttle-flare_0..2    10x26, additive: the long boost flame of the liftoff
                                                  and the climb-out; its top centre (5, 0) on each
                                                  engine (drawn at the lift step's scale on the pad:
                                                  an unlit additive effect may be scaled)
  assets/sprites/evacuation-shuttle-pip           16x10 HUD pip: the top-down silhouette, flat white,
                                                  tinted at run time (MissionPanel loads <ally>-pip)
  assets/pivots/evacuation-shuttle.json           the engine mounts per bank frame (`points.engine-1..n`,
                                                  [x, y] px from the frame's top left), per lift step
                                                  (`lift`) and per wreck step (`wreck`), the smoke
                                                  source (the dead engine) per bank frame and wreck
                                                  step (`smoke`, `wreck-smoke`); the flame and flare
                                                  attach points
  design/allies/concept/evacuation-shuttle-r32-a.png/.gif     the review sheet and loop of the
                                                  chosen a (round 32); the rejected b's in
                                                  concept/rejected/evacuation-shuttle-r32-b.*

The game side (ShuttleLooks, D6): the bank frame from the lane drift's sideways speed v, frame
2 + round(2 v / full) clamped to 0..4 (design/allies data.yaml's planned `banks: {frames: 5, full: 60}`); the flame frames on the mounts every 3 steps;
below 50 % the damaged set and a ship-smoke puff every 6 steps from `smoke`, drifting down the
screen; the liftoff picks the lift step by its scale (0.70 -> 1.0 over 6 s) with the flare on the
mounts scaled alike; the climb-out the level frame with the flare; a lost shuttle plays the wreck
steps over its glide (the data's `glide: 3` s: 22.5 steps each), sinking down the screen up to far's speed
(0.5 x the scroll), drawn under the air layer, with ship-smoke puffs from `wreck-smoke`; its shadow
at the air offset shrinks with the steps and fades out by step 5.

Run: python3 tools/art/shuttle.py [--variant a|b] [--review]   (~40 s; --review rebuilds both
variants' review files: a from assets/, b rendered afresh)
"""
import json
import sys
from concurrent.futures import ProcessPoolExecutor

import numpy as np
from PIL import Image

import artkit
from artkit import DESIGN, ROOT, sprite

import vfx_r08 as v8  # noqa: E402  (concept script, imported unchanged)
import vrell_fx as fx  # noqa: E402  (tools/art: timing)
import wraith  # noqa: E402  (tools/art: the dawn stand-in plate and the review helpers)
from render import sdf  # noqa: E402
from render.enemy_rigs import write_gif  # noqa: E402
from render.sdf import (Material, mirror_x, panel_lines, rotate_y, rotate_z, sd_box, sd_capsule,  # noqa: E402
                        sd_cylinder_y, sd_cylinder_z, sd_ellipsoid, sd_plate, sd_sphere, union)

# =========================================================================== parameters

SCRIPT = "shuttle.py"
BATCH = "M5 part D batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
REVIEW_ROUND = "r32"
CONCEPT = DESIGN / "allies" / "concept"
NAME = "evacuation-shuttle"
PRODUCTION = "a"                                # the user's pick, round 32 (2026-10-08)
STEP = 60

SIZE = (64, 40)                                 # design/allies: sprite 64x40
EXTENT = 64.0                                   # 1 model unit = 1 px
Z_TOP = 40.0
CAMERA = 108.0                                  # the Stormhawk's perspective strength at this span
BANKS = [-30, -15, 0, 15, 30]                   # degrees of roll, hard left .. hard right (Rook's)
FIT = 0.92                                      # the models' scale on the canvas: nose and tail clear the edges
LIFT_SCALES = [0.70, 0.775, 0.85, 0.925]        # ground (1 / 1.43) .. just below air
WRECK_STEPS = 8
WRECK_SCALE = (1.0, 0.35)                       # air -> far
WRECK_ROLL = 35
WRECK_YAW = 15
WRECK_DARK = np.array([0.5, 0.52, 0.6])         # the far haze's darkening at the last step
HULL_COLOURS = 40
FLAME = (6, 12)
FLARE = (10, 26)
FLAME_FRAMES = 3
PIP = (16, 10)

WHITE_HULL = (0.84, 0.85, 0.84)
GREY = (0.46, 0.48, 0.52)
NOSE_CAP = (0.16, 0.16, 0.18)
EVAC = (0.95, 0.38, 0.12)                       # rescue orange (air ally: the loot amber rule is the ground's)
CDF_BLUE = (0.16, 0.3, 0.62)
HULL, DARK, GLASS, STRIPE, BLUE, WINDOW, NAV_RED, NAV_GREEN, ENGINE, SCORCH, CAP = range(11)
ENGINE_GLOW = (0.55, 0.75, 1.3)
FLAME_CORE = (255, 255, 255)
FLAME_BODY = (150, 205, 255)
FLAME_TAIL = (60, 110, 255)


def materials(lit=True, damaged=False):
    def char(p, n):                                           # soot streaks for the scorch decals
        v = np.sin(p[:, 0] * 1.7 + 0.4) * np.sin(p[:, 1] * 1.3 - 0.8) + 0.5 * np.sin((p[:, 0] - p[:, 1]) * 2.9)
        return 0.55 + 0.45 * np.clip(v, -1, 1)
    window = (Material((0.1, 0.08, 0.05), emission=(1.25, 0.95, 0.55)) if lit
              else Material((0.07, 0.07, 0.08), shininess=60, spec=0.6))
    red = Material((0.1, 0.02, 0.02), emission=(1.6, 0.15, 0.1)) if lit else Material((0.25, 0.08, 0.07), spec=0.4)
    green = Material((0.02, 0.1, 0.04), emission=(0.2, 1.5, 0.45)) if lit else Material((0.08, 0.22, 0.12), spec=0.4)
    engine = (Material((0.06, 0.07, 0.1), emission=ENGINE_GLOW) if lit
              else Material((0.08, 0.08, 0.09), metal=0.6, shininess=30, spec=0.4))
    return [
        Material(WHITE_HULL, metal=0.15, shininess=45, spec=0.45, pattern=panel_lines(0.16, 0.05, 0.82)),
        Material(GREY, metal=0.55, shininess=40, spec=0.5),
        Material((0.05, 0.08, 0.12), metal=0.9, shininess=120, spec=1.2),
        Material(EVAC, metal=0.1, shininess=40, spec=0.4),
        Material(CDF_BLUE, metal=0.2, shininess=50, spec=0.5),
        window, red, green, engine,
        Material((0.3, 0.27, 0.24), metal=0.1, shininess=10, spec=0.15, pattern=char),
        Material(NOSE_CAP, metal=0.2, shininess=30, spec=0.35),
    ]


def decal(m, where, slot):
    return np.where(where, slot, m)


# =========================================================================== models
# Units: 1 = 1 native px. x right, y up the screen (the nose), z towards the camera.

A_BODY = [(0.0, 18.5), (5.0, 16.5), (10.0, 10.0), (20.0, 1.0), (29.5, -9.0), (29.5, -13.0),
          (19.0, -13.5), (11.0, -16.0), (0.0, -16.5)]
A_ENGINES = [(0.0, -18.5), (-5.2, -18.0), (5.2, -18.0)]   # the bells' rear ends (x, y)
B_ENGINES = [(-4.0, -20.5), (4.0, -20.5)]
SPOTS_A = [(-9.0, 2.0, 3.0), (14.0, -6.0, 2.6), (-20.0, -7.0, 2.4), (3.0, 8.0, 2.0)]
SPOTS_B = [(-4.0, 5.0, 2.8), (22.0, -1.0, 2.6), (-24.0, -2.0, 2.4), (2.0, -11.0, 2.0)]


def lifting_body(dead_engine=False):
    """Variant a: the broad blended lifting body."""
    def scene(p):
        q = mirror_x(p)
        x, y = q[:, 0], q[:, 1]
        body = sd_plate(q, A_BODY, 1.0, 2.6, 1.2, taper=lambda xx, yy: np.clip(1.15 - np.abs(xx) / 30, 0.3, 1))
        d, m = union((body, HULL), (sd_ellipsoid(p, (0, 0.5, 2.6), (6.8, 17.5, 4.4)), HULL), k=3.0)
        fin = sd_box(rotate_y(q - np.array([9.5, -12.5, 5.0]), 0.45), (0, 0, 0), (0.7, 3.4, 3.6), 0.4)
        bells = [(sd_cylinder_y(p, (ex, ey + 1.2, 1.4), 2.3 if ex == 0 else 1.9, 1.3), DARK) for ex, ey in A_ENGINES]
        nozzles = [(sd_cylinder_y(p, (ex, ey + 0.3, 1.4), 1.5 if ex == 0 else 1.2, 0.5), ENGINE) for ex, ey in A_ENGINES]
        d, m = union((d, m), (fin, HULL), *bells, k=0.6)
        d, m = union((d, m), *nozzles,
                     (sd_ellipsoid(p, (0, 11.5, 4.9), (3.6, 2.4, 1.2)), GLASS), k=0.3)
        navs = [(sd_sphere(p, (-28.5, -10.5, 2.0), 1.1), NAV_RED), (sd_sphere(p, (28.5, -10.5, 2.0), 1.1), NAV_GREEN)]
        d, m = union((d, m), *navs)
        # decals: the nose cap, the evac chevron over both wings, the CDF spine stripe, cabin windows
        m = decal(m, (y > 15.0) & (m == HULL), CAP)
        chevron = np.abs(y - (-1.5 - 0.42 * (x - 9))) < 2.0
        m = decal(m, chevron & (x > 9) & (x < 27) & (m == HULL), STRIPE)
        m = decal(m, (x < 0.9) & (y > -12) & (y < 7.5) & (p[:, 2] > 5.5) & (m == HULL), BLUE)
        win = (np.abs(x - 4.6) < 0.7) & (((y + 9.0) % 3.0) < 1.4) & (y > -9) & (y < 7) & (p[:, 2] > 3.5)
        m = decal(m, win & (m == HULL), WINDOW)
        if dead_engine:
            m = np.where((m == ENGINE) & (p[:, 0] < -2), DARK, m)
        return d, m
    return scene


def heavy_lifter(dead_engine=False):
    """Variant b: the cabin fuselage on stub wings with lift fans and tip nacelles."""
    def scene(p):
        q = mirror_x(p)
        x, y = q[:, 0], q[:, 1]
        fus = sd_box(p, (0, -1.0, 3.2), (6.2, 18.0, 3.8), 3.4)
        nose = sd_ellipsoid(p, (0, 16.0, 3.0), (6.0, 4.4, 3.6))
        wing = sd_box(q, (15.0, -2.0, 1.6), (15.0, 5.6, 1.0), 0.8)
        nacelle = sd_capsule(q, (28.5, 4.5, 2.0), (28.5, -8.5, 2.0), 2.8, 2.4)
        tail = sd_box(p, (0, -17.0, 7.2), (10.0, 2.0, 0.6), 0.5)
        fin = sd_box(p, (0, -16.0, 5.0), (0.8, 3.0, 2.6), 0.4)
        bells = [(sd_cylinder_y(p, (ex, ey + 1.0, 2.2), 2.1, 1.2), DARK) for ex, ey in B_ENGINES]
        nozzles = [(sd_cylinder_y(p, (ex, ey + 0.2, 2.2), 1.4, 0.5), ENGINE) for ex, ey in B_ENGINES]
        d, m = union((fus, HULL), (nose, HULL), k=2.0)
        d, m = union((d, m), (wing, HULL), (nacelle, HULL), (tail, HULL), (fin, HULL), *bells, k=0.8)
        fan_ring = np.maximum(sd_cylinder_z(q, (17.0, -2.0, 2.4), 4.8, 0.7), -sd_cylinder_z(q, (17.0, -2.0, 2.4), 3.6, 2.0))
        fan = sd_cylinder_z(q, (17.0, -2.0, 1.9), 3.7, 0.4)
        d, m = union((d, m), (fan_ring, DARK), (fan, CAP), *nozzles,
                     (sd_box(p, (0, 15.5, 5.6), (4.2, 1.7, 1.0), 0.8), GLASS), k=0.2)
        navs = [(sd_sphere(p, (-28.5, -9.6, 4.3), 1.2), NAV_RED), (sd_sphere(p, (28.5, -9.6, 4.3), 1.2), NAV_GREEN)]
        d, m = union((d, m), *navs)
        # fan blades: dark spokes over the fan disc
        ang = np.arctan2(y + 2.0, x - 17.0)
        rr = np.hypot(x - 17.0, y + 2.0)
        m = np.where((m == CAP) & (rr < 3.6) & (np.cos(ang * 7) > 0.3) & (p[:, 2] > 1.5), DARK, m)
        band = (y > -3.5) & (y < 0.5) & (x < 7.0)
        m = decal(m, band & (m == HULL), STRIPE)
        m = decal(m, (x > 26) & (y > 2.5) & (m == HULL), STRIPE)
        m = decal(m, (x < 1.0) & (y > -14) & (y < 12) & (p[:, 2] > 6.6) & ~band & (m == HULL), BLUE)
        win = (np.abs(x - 5.2) < 0.8) & (((y + 13.0) % 3.0) < 1.4) & ((y > 1.5) | (y < -4.5)) & (y > -13) & (y < 12) \
            & (p[:, 2] > 3.0)
        m = decal(m, win & (m == HULL), WINDOW)
        m = decal(m, (y > 18.5) & (m == HULL), BLUE)
        if dead_engine:
            m = np.where((m == ENGINE) & (p[:, 0] < 0), DARK, m)
        return d, m
    return scene


VARIANTS = {"a": (lifting_body, A_ENGINES, SPOTS_A, "A - LIFTING BODY"),
            "b": (heavy_lifter, B_ENGINES, SPOTS_B, "B - HEAVY LIFTER")}


def scorched(scene, spots):
    """The scene with char decals (spots in model px: x, y, radius)."""
    def out(p):
        d, m = scene(p)
        for j, (sx, sy, r) in enumerate(spots):
            ang = np.arctan2(p[:, 1] - sy, p[:, 0] - sx)
            ragged = 1.3 * r * (0.65 + 0.35 * np.sin(3 * ang + j) * np.cos(5 * ang - 2 * j))   # a torn, uneven burn
            hit = np.hypot((p[:, 0] - sx) * 0.8, p[:, 1] - sy) < ragged
            m = np.where(hit & np.isin(m, (HULL, STRIPE, BLUE, CAP, WINDOW)), SCORCH, m)
        return d, m
    return out


def posed(variant, bank=0.0, scale=1.0, state="ok", yaw=0.0):
    """(scene, mats) of a variant rolled ``bank`` degrees, yawed ``yaw`` degrees (clockwise),
    drawn ``scale`` x; state ok, damaged (scorched, one engine dead) or wreck (all dead)."""
    make, _, spots, _ = VARIANTS[variant]
    base = make(dead_engine=state != "ok")
    if state != "ok":
        base = scorched(base, spots if state == "wreck" else spots[:3])
    b, w = np.radians(bank), np.radians(yaw)

    def scene(p):
        q = rotate_y(rotate_z(p, -w), b) / (scale * FIT)
        d, m = base(q)
        return d * scale * FIT, m
    return artkit.perspective(scene, CAMERA), materials(lit=state != "wreck", damaged=state != "ok")


def render(variant, bank=0.0, scale=1.0, state="ok", yaw=0.0):
    scene, mats = posed(variant, bank, scale, state, yaw)
    hi = sdf.render(scene, mats, (SIZE[0] * 8, SIZE[1] * 8), EXTENT, z_top=Z_TOP, steps=160)
    return artkit.native(hi, 8, crisp=70)


def project(points, bank=0.0, scale=1.0, yaw=0.0, z=1.4):
    """Model points (x, y) on the sprite: rolled, yawed, scaled and through the camera, px from the
    top left."""
    out = []
    for x, y in points:
        v = np.array([[x * scale * FIT, y * scale * FIT, z * scale * FIT]])
        v = rotate_z(rotate_y(v, -np.radians(bank)), np.radians(yaw))[0]
        k = CAMERA / (CAMERA - v[2])
        out.append([int(round(SIZE[0] / 2 + v[0] * k)), int(round(SIZE[1] / 2 - v[1] * k))])
    return out


def wreck_pose(i):
    u = i / (WRECK_STEPS - 1)
    scale = WRECK_SCALE[0] * (WRECK_SCALE[1] / WRECK_SCALE[0]) ** u
    return scale, WRECK_ROLL * u ** 0.7, WRECK_YAW * u


def darken(img, u):
    a = np.array(img).astype(np.float64)
    a[..., :3] *= 1 - (1 - WRECK_DARK) * u
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


# =========================================================================== effects

def flame_frames(size, length, seed):
    """Blue-white plasma flame: a white core at the nozzle, a pale blue body, a deep blue tail,
    flickering in length over 3 frames; additive."""
    rng = np.random.default_rng(seed)
    flicker = rng.uniform(0.8, 1.1, FLAME_FRAMES)
    out = []
    w, h = size
    for f in range(FLAME_FRAMES):
        cv = v8.Canvas(w, h)
        ax, ay = w / 2, 1.0
        tail = ay + (h - ay - 2.0) * flicker[f] / 1.1 * length
        d = cv.seg(ax, ay, ax, tail)
        along = np.clip((cv.y - ay) / max(tail - ay, 1), 0, 1)
        width = w * 0.24 * (1 - 0.6 * along)
        cv.add(FLAME_TAIL, v8.gauss(d, width * 1.8) * 0.8 * (1 - along) ** 0.6)
        cv.add(FLAME_BODY, v8.solid(d, width) * (1 - along ** 1.5))
        cv.add(FLAME_CORE, v8.solid(d, width * 0.5) * (1 - along) ** 1.4)
        out.append(artkit.additive(cv.image()))
    return artkit.quantize_set(out, 16)


def pip(variant):
    make = VARIANTS[variant][0]
    w, h = PIP
    hi = sdf.render(make(), materials(), (w * 8, h * 8), EXTENT, z_top=Z_TOP, shadows=False)
    a = sprite.downsample(hi, 8)[..., 3] >= 0.5
    rgba = np.zeros((h, w, 4), np.uint8)
    rgba[a] = (255, 255, 255, 255)
    return Image.fromarray(rgba, "RGBA")


# =========================================================================== build

def _job(job):
    variant, kind, arg = job
    if kind == "bank":
        return render(variant, arg)
    if kind == "damaged":
        return render(variant, arg, state="damaged")
    if kind == "lift":
        return render(variant, 0.0, arg)
    scale, roll, yaw = wreck_pose(arg)
    return darken(render(variant, roll, scale, "wreck", yaw), arg / (WRECK_STEPS - 1))


def frame_sets(variant):
    jobs = ([(variant, "bank", b) for b in BANKS] + [(variant, "damaged", b) for b in BANKS]
            + [(variant, "lift", s) for s in LIFT_SCALES] + [(variant, "wreck", i) for i in range(WRECK_STEPS)])
    with ProcessPoolExecutor() as pool:
        frames = list(pool.map(_job, jobs))
    frames = artkit.quantize_set(frames, HULL_COLOURS)        # one palette: no flicker between states
    n = len(BANKS)
    return {NAME: frames[:n], f"{NAME}-damaged": frames[n:2 * n],
            f"{NAME}-lift": frames[2 * n:2 * n + 4], f"{NAME}-wreck": frames[2 * n + 4:],
            f"{NAME}-flame": flame_frames(FLAME, 1.0, 3261), f"{NAME}-flare": flame_frames(FLARE, 1.0, 3262),
            f"{NAME}-pip": [pip(variant)]}


def pivots(variant):
    engines = VARIANTS[variant][1]
    dead = [min(engines, key=lambda e: (e[0], -e[1]))]        # the left engine dies with the damage
    names = [f"engine-{i + 1}" for i in range(len(engines))]
    by_bank = [project(engines, b) for b in BANKS]
    lift = [project(engines, 0.0, s) for s in LIFT_SCALES]
    wreck = [project(engines, wreck_pose(i)[1], wreck_pose(i)[0], wreck_pose(i)[2]) for i in range(WRECK_STEPS)]
    return {
        "variant": variant,
        "frames": ["hard left", "left", "level", "right", "hard right"],
        "banks": BANKS,
        "lift-scales": LIFT_SCALES,
        "points": {name: [pts[i] for pts in by_bank] for i, name in enumerate(names)},
        "lift": {name: [pts[i] for pts in lift] for i, name in enumerate(names)},
        "wreck": {name: [pts[i] for pts in wreck] for i, name in enumerate(names)},
        "smoke": [project(dead, b)[0] for b in BANKS],
        "wreck-smoke": [project(dead, wreck_pose(i)[1], wreck_pose(i)[0], wreck_pose(i)[2])[0]
                        for i in range(WRECK_STEPS)],
        f"{NAME}-flame": {"attach": [FLAME[0] // 2, 0]},
        f"{NAME}-flare": {"attach": [FLARE[0] // 2, 0]},
    }


def build(variant):
    sets = frame_sets(variant)
    for name, frames in sets.items():
        artkit.write_frames(name, frames, SOURCE + f", variant {variant}", single=name.endswith("-pip"))
        print(f"{name}: {len(frames)} frames, {frames[0].width}x{frames[0].height}")
    artkit.write_pivots(NAME, SOURCE + f", variant {variant}", pivots(variant))
    return sets


def load_sets():
    names = [NAME, f"{NAME}-damaged", f"{NAME}-lift", f"{NAME}-wreck", f"{NAME}-flame", f"{NAME}-flare", f"{NAME}-pip"]
    return {n: artkit.load_frames(n) for n in names}


# =========================================================================== review

def draw_shuttle(cell, sets, pv, kind, index, x, y, flame=None, flame_kind="flame", flame_scale=1.0,
                 shadow=(21, 30), shadow_alpha=0.5):
    """A shuttle frame centred on (x, y) with its shadow and its engine flames under it."""
    frame = sets[kind][index]
    if shadow is not None and shadow_alpha > 0:
        sh = wraith.shadow(frame, shadow_alpha)
        cell.alpha_composite(sh, (int(round(x + shadow[0] - sh.width / 2)), int(round(y + shadow[1] - sh.height / 2))))
    left, top = int(round(x - SIZE[0] / 2)), int(round(y - SIZE[1] / 2))
    if flame is not None:
        table = {NAME: pv["points"], f"{NAME}-damaged": pv["points"], f"{NAME}-lift": pv["lift"]}[kind]
        fl = sets[f"{NAME}-{flame_kind}"][flame % FLAME_FRAMES]
        if flame_scale != 1.0:
            fl = fl.resize((max(1, round(fl.width * flame_scale)), max(1, round(fl.height * flame_scale))), Image.NEAREST)
        for i, (name, pts) in enumerate(sorted(table.items())):
            if kind == f"{NAME}-damaged" and pts[index] == pv["smoke"][index]:
                continue                                      # the dead engine has no flame
            px, py = pts[index]
            cell = artkit.add_light(cell, fl, (left + px - fl.width // 2, top + py))
    cell.alpha_composite(frame, (left, top))
    return cell


def review(variant, sets, pv):
    title = VARIANTS[variant][3]
    lvl = BANKS.index(0)
    with_flames = []
    for b in range(len(BANKS)):
        cell = Image.new("RGBA", (SIZE[0] + 8, SIZE[1] + 22), artkit.PLATE)
        with_flames.append(draw_shuttle(cell, sets, pv, NAME, b, cell.width / 2, SIZE[1] / 2 + 3, flame=b, shadow=None))
    on_plate = []
    for kind, idx in ((NAME, lvl), (f"{NAME}-damaged", lvl), (NAME, 0), (NAME, 4)):
        cell = wraith.dawn_plate(110, 96)
        on_plate.append(draw_shuttle(cell, sets, pv, kind, idx, 45, 34, flame=idx))
    pipcell = Image.new("RGBA", (60, 18), artkit.PLATE)
    for i, col in enumerate(((90, 220, 120), (255, 190, 60), (255, 255, 255))):
        tint = np.array(sets[f"{NAME}-pip"][0]).astype(np.float64)
        tint[..., :3] *= np.array(col) / 255
        pipcell.alpha_composite(Image.fromarray(tint.astype(np.uint8), "RGBA"), (2 + 20 * i, 4))
    rows = [
        ("BANKING FRAMES HARD LEFT .. HARD RIGHT (-30 -15 0 +15 +30 DEG), FLYING UP", sets[NAME], 4, False),
        ("WITH THE ENGINE FLAMES AT THE PIVOTS, AS THE GAME DRAWS THEM", with_flames, 3, False),
        ("DAMAGED (BELOW 50 %): SCORCHED, LEFT ENGINE DEAD; THE SMOKE IS THE GAME'S", sets[f"{NAME}-damaged"], 4, False),
        ("LIFTOFF STEPS 0.70 / 0.775 / 0.85 / 0.925 X (GROUND -> LOW-AIR), THEN THE LEVEL FRAME", sets[f"{NAME}-lift"]
         + [sets[NAME][lvl]], 4, False),
        ("THE GLIDE INTO FAR: 8 STEPS, 1.0 -> 0.35 X, ROLLING, DARKENING, ALL LIGHTS DEAD", sets[f"{NAME}-wreck"], 3, False),
        ("ENGINE FLAME 6X12 AND BOOST FLARE 10X26 (ADDITIVE, 3-FRAME LOOPS)", sets[f"{NAME}-flame"]
         + sets[f"{NAME}-flare"], 6, True),
        ("ON THE DAWN STAND-IN WITH SHADOW: LEVEL, DAMAGED, HARD LEFT, HARD RIGHT", on_plate, 2, False),
        ("HUD PIP 16X10, TINTED GREEN / AMBER / WHITE (HIT) AT RUN TIME", [pipcell], 4, False),
        ("GREYSCALE CHECK", [on_plate[0].convert("L").convert("RGBA")], 2, False),
        ("1X", sets[NAME] + sets[f"{NAME}-damaged"] + sets[f"{NAME}-lift"], 1, False)]
    sheet = artkit.review_sheet(f"EVACUATION SHUTTLE {title} - PRODUCTION SPRITES (ROUND 32 PROPOSAL {variant.upper()})",
                                rows, width=1500, batch=BATCH)
    folder = CONCEPT if variant == PRODUCTION else CONCEPT / "rejected"   # the user's pick, round 32
    folder.mkdir(parents=True, exist_ok=True)
    png = folder / f"{NAME}-r32-{variant}.png"
    gif = folder / f"{NAME}-r32-{variant}.gif"
    sheet.convert("RGB").save(png, optimize=True)
    write_gif(review_loop(sets, pv), gif, fps=20, colors=128)
    print(f"review: {png.relative_to(ROOT)}, {gif.relative_to(ROOT)}")


def review_loop(sets, pv):
    """Three shuttles on a 240x320 strip of the dawn stand-in: liftoff from the pad (ground ->
    low-air -> air, 3 s here instead of 6, the flare on), the band drifting along its lanes with
    the bank frames, the middle one hit and smoking, then lost: the glide into far with smoke; the
    other two climb out with the flare."""
    fw, fh, fps = 240, 320, 20
    plate = wraith.dawn_plate(fw, 720, 150, 60)
    smoke = artkit.load_frames("ship-smoke")
    lvl = BANKS.index(0)
    lanes = [(60, 150, 0.0), (120, 190, 1.3), (180, 150, 2.6)]
    t_lift, t_hit, t_lost, t_climb, total = 3.0, 5.5, 7.5, 10.5, 12.5
    puffs = []                                               # (born, x, y)
    gif = []
    for f in range(int(total * fps)):
        t = f / fps
        scroll = int(t * 95) % (720 - fh)
        cell = plate.crop((0, 720 - fh - scroll, fw, 720 - scroll))
        for n, (lx, ly, ph) in enumerate(lanes):
            sway = 14 * np.sin(0.9 * t + ph)
            vx = 14 * 0.9 * np.cos(0.9 * t + ph)
            x, y = lx + sway, ly
            bank = lvl + (1 if vx > 4 else -1 if vx < -4 else 0) + (1 if vx > 9 else -1 if vx < -9 else 0)
            if t < t_lift:                                   # the liftoff
                u = t / t_lift
                scale = 0.70 + 0.30 * u
                step = min(3, int(u * 4))
                y = ly + 40 * (1 - u)
                cell = draw_shuttle(cell, sets, pv, f"{NAME}-lift", step, lx, y, flame=f, flame_kind="flare",
                                    flame_scale=LIFT_SCALES[step], shadow=(21 * u, 30 * u), shadow_alpha=0.5)
                continue
            if n == 1 and t >= t_lost:                       # the glide into far
                u = min(1.0, (t - t_lost) / 3.0)
                step = min(WRECK_STEPS - 1, int(u * WRECK_STEPS))
                wx, wy = x - 30 * u, y + 95 * u * u + 40 * u
                if f % 2 == 0:
                    sx, sy = pv["wreck-smoke"][step]
                    puffs.append((t, wx - SIZE[0] / 2 + sx, wy - SIZE[1] / 2 + sy))
                cell = draw_shuttle(cell, sets, pv, f"{NAME}-wreck", step, wx, wy,
                                    shadow=(21 * (1 - u), 30 * (1 - u)), shadow_alpha=0.5 * max(0.0, 1 - u * 1.6))
                continue
            if t >= t_climb and n != 1:                      # the climb-out
                u = t - t_climb
                y = ly - 60 * u * u
                cell = draw_shuttle(cell, sets, pv, NAME, lvl, lx + sway, y, flame=f, flame_kind="flare")
                continue
            damaged = n == 1 and t >= t_hit
            kind = f"{NAME}-damaged" if damaged else NAME
            if damaged and f % 2 == 0:
                sx, sy = pv["smoke"][bank]
                puffs.append((t, x - SIZE[0] / 2 + sx, y - SIZE[1] / 2 + sy))
            cell = draw_shuttle(cell, sets, pv, kind, bank, x, y, flame=f + n)
            if n == 1 and t_hit <= t < t_hit + 0.1:          # the hit flash
                cell = wraith.add_centred(cell, artkit.load_frames("explosion-tiny")[1], x - 6, y + 4)
        for born, sx, sy in puffs:                           # smoke drifts down the screen
            age = int((t - born) * STEP)
            fr = fx.timed(smoke, 6, age)
            if fr is not None:
                cell.alpha_composite(fr, (int(sx - fr.width / 2), int(sy + (t - born) * 110 - fr.height / 2)))
        gif.append(sprite.enlarge(cell, 2))
    return gif


if __name__ == "__main__":
    artkit.REVIEW_ROUND = REVIEW_ROUND
    args = sys.argv[1:]
    chosen = args[args.index("--variant") + 1] if "--variant" in args else PRODUCTION
    if "--review" not in args:
        sets = build(chosen)
        review(chosen, sets, pivots(chosen))
    else:
        review(PRODUCTION, load_sets(), pivots(PRODUCTION))
    for other in sorted(set(VARIANTS) - {chosen if "--review" not in args else PRODUCTION}):
        review(other, frame_sets(other), pivots(other))
