#!/usr/bin/env python3
"""Production art, two variants of one ship pair: Level 11's convoy, the UTC cargo ship
(Halvorsen, Mbeki, Saint-Laurent) and its CDF escort frigate (Ruyter) (design/allies, Convoy cargo
ship and Escort frigate; M5 part E batch, concept round 33). They had no concept of their own,
only the models of the chosen ocean scene (scene-ocean-r10-a: render/r08_models.py's freighter and
frigate), so this script proposes the pair as two sets at production quality (user decision
E9 = a); a writes the game's names until the user's pick.

  a  "Atlantic line" (the ocean scene's pair): a feeder container ship with a dark navy hull,
     five bays of muted multicoloured containers stacked one to three high, a white bridge block
     aft with a red funnel, a mast light on the forecastle; the scene's grey CDF frigate with its
     bow gun, missile cells, superstructure with a radar dome, and a helipad aft
  b  "Reactor run": a heavy-lift carrier in rust-red with its white bridge forward, two long
     reactor-vessel segments and a reactor dome in hazard-striped cradles (a teal status light on
     each), a yellow deck crane and twin funnels aft; a CDF stealth trimaran frigate in dark slate:
     a slim faceted main hull on two outriggers, a faceted gun and missile cells forward, an
     integrated pyramid mast, a CDF-blue stripe and a helipad across the aft deck

Outputs (variant a, the names the game loads; `--variant b` writes b's instead, otherwise b lives
only in its review files):
  assets/sprites/cargo-ship_0               56x120 bow up, afloat: the hull's waterline at the
                                            surface (z = 0), 40 colours shared by every hull frame
  assets/sprites/cargo-ship-damaged_0       56x120 after the first slam: listing 8 degrees,
                                            scorched, containers knocked off (a: two stacks; b: the
                                            crane), a breach at the waterline; the smoke is the
                                            game's (ship-smoke puffs from the pivot) with the fire
  assets/sprites/cargo-ship-fire_0..3       12x12 additive: the deck fire's flicker on the pivot
  assets/sprites/cargo-ship-collar_0..3     68x132 the animated foam collar along the waterline,
                                            centred on the hull (drawn over its edge)
  assets/sprites/cargo-ship-wake_0..15      88x208 bow waves peeling off down both sides, the hull
                                            wash and the churned prop-wash trail; the hull's centre
                                            at (44, 64); 16 frames at 20 fps stream the foam down
                                            at ~140 px/s, the scroll speed (drawn under the hull)
  assets/sprites/cargo-ship-sink_0..9       72x136 the sinking, centred as the hull: listing to 32
                                            degrees and down by the stern, settling through the
                                            surface (what is under it seen through the water: toward
                                            the water colour, darker, stepped translucency), lights
                                            dead, a collar along the shrinking waterline, a foam ring
                                            spreading, bubbles and flotsam; the last step only foam
  assets/sprites/cargo-ship-pip             8x18 HUD pip: the top-down silhouette, flat white,
                                            tinted at run time (MissionPanel loads <ally>-pip)
  assets/sprites/escort-frigate_0           40x110 bow up, afloat (never damaged)
  assets/sprites/escort-frigate-collar_0..3 52x122
  assets/sprites/escort-frigate-wake_0..15  72x200, the hull's centre at (36, 62)
  assets/sprites/escort-frigate-muzzle_0..2 8x8 additive: the bow gun's flash (flak cue)
  assets/sprites/escort-frigate-flak_0..7   24x24 a flak burst on low-air over the convoy: an
                                            orange flash, then a dark puff spreading and thinning
                                            (presentation only, hits nothing)
  assets/pivots/cargo-ship.json             the smoke and fire points of the damaged frame, the
                                            wake's and collar's offsets, the sink frames' centre
  assets/pivots/escort-frigate.json         the bow gun's muzzle, the wake's and collar's offsets
  design/allies/concept/convoy-ships-r33-a.png/.gif   the review sheets and loops (the pair at
                                            their stations on the ocean, a slam hit, the sinking):
                                            chosen a (round 33); the rejected b's in
                                            concept/rejected/convoy-ships-r33-b.*

The ships hold screen stations at the scroll speed, so the wake is stationary in the hull's frame and
only its foam streams; at the arena halt the game may fade the wake (the convoy keeps steaming).

Run: python3 tools/art/convoy_ships.py [--variant a|b] [--review]   (~1 min; --review rebuilds a's
review from assets/ and renders b afresh)
"""
import json
import sys
from concurrent.futures import ProcessPoolExecutor

import numpy as np
from PIL import Image

import artkit
from artkit import DESIGN, ROOT, TAU, sprite

import vfx_r08 as v8  # noqa: E402  (concept script, imported unchanged)
import water_fx as wfx  # noqa: E402  (tools/art: foam, collars, waterline rendering, the sea stand-in)
import scenes_r08 as s8  # noqa: E402  (concept script, imported unchanged: streams)
from render import r08_models as m8  # noqa: E402  (the ocean scene's ship models and materials)
from render.enemy_rigs import write_gif  # noqa: E402
from render.sdf import (Material, mirror_x, panel_lines, rotate_x, rotate_y, sd_box, sd_capsule,  # noqa: E402
                        sd_cylinder_y, sd_cylinder_z, sd_plate, sd_sphere, subtract, union)

SCRIPT = "convoy_ships.py"
BATCH = "M5 part E batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
REVIEW_ROUND = "r33"
CONCEPT = DESIGN / "allies" / "concept"
PRODUCTION = "a"                                # the user's pick in round 33
CARGO, FRIGATE = "cargo-ship", "escort-frigate"

CARGO_SIZE = (56, 120)                          # design/allies: about 56x120 bow up
FRIGATE_SIZE = (40, 110)                        # about 40x110
Z_TOP = 40.0
STEPS = 160
FIT = 0.93                                      # the models' scale on the canvas: bow and stern clear the edges
CAMERA = 260.0                                  # a mild perspective so the list reads
HULL_COLOURS = 40
DAMAGE_ROLL = 12.0                              # degrees of list after the first slam
SINK_STEPS = 10
SINK_SIZE = (72, 136)
SINK = [(10 + 22 * u ** 0.8, 8 * u, 1.0 + 30 * u ** 1.25) for u in np.linspace(0, 1, SINK_STEPS)]  # roll, pitch, depth
COLLAR_PAD = 6
WAKE = {CARGO: ((88, 208), (44, 64)), FRIGATE: ((72, 200), (36, 62))}   # canvas, the hull's centre in it
WAKE_FRAMES = 16
FIRE = (12, 12)
FLAK = 24
FLAK_FRAMES = 8
PIP = (8, 18)

# extra materials after the scene's (r08_models: HULL 0 .. FLAME 11, containers 12..17)
SCORCH, RUST, HAZARD, CDF_BLUE, TEAL, SLATE, YELLOW = range(18, 25)
HULLISH = (m8.HULL, m8.DECK, m8.WHITE, m8.GREY, m8.FUNNEL, RUST, SLATE, HAZARD, YELLOW) + tuple(range(12, 18))


def materials(pad_c=(0.0, 7.0), lit=True):
    mats = m8._mats(pad_c)

    def char(p, n):
        v = np.sin(p[:, 0] * 1.7 + 0.4) * np.sin(p[:, 1] * 1.3 - 0.8) + 0.5 * np.sin((p[:, 0] - p[:, 1]) * 2.9)
        return 0.55 + 0.45 * np.clip(v, -1, 1)

    def stripes(p, n):
        return np.where(((p[:, 0] + p[:, 1]) / 4) % 1 < 0.5, 0.16, 1.0)
    mats += [Material((0.22, 0.19, 0.17), metal=0.1, shininess=10, spec=0.15, pattern=char),          # SCORCH
             Material((0.42, 0.17, 0.12), metal=0.3, shininess=35, spec=0.4,
                      pattern=lambda p, n: np.where((p[:, 1] * 0.5) % 1 < 0.06, 0.82, 1.0)),          # RUST
             Material((0.95, 0.72, 0.12), metal=0.1, shininess=20, spec=0.3, pattern=stripes),         # HAZARD
             Material((0.16, 0.3, 0.62), metal=0.2, shininess=50, spec=0.5),                           # CDF_BLUE
             Material((0.05, 0.3, 0.3), emission=(0.2, 1.5, 1.3)),                                     # TEAL
             Material((0.36, 0.39, 0.44), metal=0.5, shininess=55, spec=0.55,
                      pattern=panel_lines(0.12, 0.05, 0.86)),                                       # SLATE
             Material((0.88, 0.70, 0.10), metal=0.3, shininess=40, spec=0.5)]                          # YELLOW
    if not lit:                                     # a sinking hull's lights are dead
        dead = Material((0.08, 0.09, 0.11), metal=0.4, shininess=60, spec=0.5)
        for k in (m8.WINDOW, m8.RED_L, m8.GREEN_L, m8.GLOW, TEAL):
            mats[k] = dead
    return mats


# =========================================================================== models
# Units: 1 = 1 native px. x right, y up the screen (the bow), z toward the camera; the water
# surface is z = 0 (the hull plates reach 5 below it).

def cargo_a(damaged=False):
    """Variant a's container ship: the ocean scene's freighter as a feeder ship, 116 x 36."""
    L, B = 116.0, 36.0
    hl = L / 2
    hull = m8._hull_poly(L, B, bow=0.22, stern=0.06)
    deck = m8._hull_poly(L - 6, B - 5, bow=0.22, stern=0.06)
    rng = np.random.default_rng(5)
    stacks = []
    for row, y in enumerate((28.0, 15.6, 3.2, -9.2, -21.6)):
        for col, cx in enumerate((-10.8, 0.0, 10.8)):
            if damaged and (row, col) in ((1, 2), (2, 2)):
                continue
            stacks.append((cx, y, int(rng.integers(1, 4)), 12 + int(rng.integers(6))))
    by = -hl + 20

    def scene(p):
        q = mirror_x(p)
        items = [(sd_plate(p, hull, 0.0, 5.0, 1.5), m8.HULL),
                 (sd_plate(p, deck, 4.5, 1.0, 0.5), m8.DECK),
                 (sd_plate(p, [(0, hl - 1), (8, hl - 13), (-8, hl - 13)], 6.2, 1.4, 0.6), m8.DECK),   # forecastle
                 (sd_box(p, (0, hl - 19, 7.0), (12.5, 0.7, 2.0), 0.4), m8.GREY)]                        # breakwater
        for cx, cy, h, mt in stacks:
            items.append((sd_box(p, (cx, cy, 5 + 2.6 * h), (5.0, 5.6, 2.6 * h), 0.4), mt))
        items += [(sd_box(p, (0, by, 12), (15.0, 7.5, 8), 1.0), m8.WHITE),
                  (sd_box(p, (0, by + 7.2, 16), (14.0, 0.8, 2.0), 0.3), m8.WINDOW),
                  (sd_box(p, (0, by + 2, 21), (17.5, 2.6, 0.8), 0.4), m8.WHITE),
                  (sd_cylinder_z(p, (0, by - 10, 16), 4.4, 6), m8.FUNNEL),
                  (sd_cylinder_z(p, (0, by - 10, 22.2), 4.5, 0.4), m8.DARK),
                  (sd_capsule(p, (0, hl - 10, 7), (0, hl - 10, 16), 0.9), m8.GREY),
                  (sd_sphere(p, (0, hl - 10, 16.5), 1.4), m8.RED_L),
                  (sd_sphere(p, (17.5, by + 2, 21), 1.2), m8.GREEN_L),
                  (sd_sphere(p, (-17.5, by + 2, 21), 1.2), m8.RED_L),
                  (sd_box(q, (16.0, -hl + 6, 6.5), (1.0, 1.4, 1.2), 0.3), m8.GREY)]                   # mooring bitts
        d, m = union(*items)
        if damaged:
            d, m = subtract((d, m), sd_sphere(p, (17.5, 8.0, 1.5), 4.2))                               # the breach
        return d, m
    return scene


def cargo_b(damaged=False):
    """Variant b's heavy-lift reactor carrier, 116 x 38: bridge forward, reactor cargo aft."""
    L, B = 116.0, 38.0
    hl = L / 2
    hull = m8._hull_poly(L, B, bow=0.24, stern=0.08)
    deck = m8._hull_poly(L - 6, B - 5, bow=0.24, stern=0.08)

    def scene(p):
        q = mirror_x(p)
        items = [(sd_plate(p, hull, 0.0, 5.0, 1.5), RUST),
                 (sd_plate(p, deck, 4.5, 1.0, 0.5), m8.DECK),
                 (sd_box(p, (0, 30, 12), (14.5, 7.0, 7.5), 1.0), m8.WHITE),                    # bridge forward
                 (sd_box(p, (0, 36.6, 15.5), (13.5, 0.8, 2.0), 0.3), m8.WINDOW),
                 (sd_box(p, (0, 32, 20), (18.0, 2.4, 0.8), 0.4), m8.WHITE),
                 (sd_capsule(p, (0, 26, 20), (0, 26, 27), 0.8), m8.GREY),                      # mast
                 (sd_sphere(p, (0, 26, 27.5), 1.2), m8.RED_L),
                 (sd_sphere(p, (18.0, 32, 20), 1.2), m8.GREEN_L),
                 (sd_sphere(p, (-18.0, 32, 20), 1.2), m8.RED_L),
                 (sd_box(p, (0, hl - 7, 7.2), (6, 2.0, 1.4), 0.6), m8.GREY)]                   # bow winch
        for cx in (-8.5, 8.5):                                                                 # vessel segments
            items += [(sd_capsule(p, (cx, 16, 12.0), (cx, -14, 12.0), 6.6), m8.GREY),
                      (sd_box(p, (cx, 12, 7.5), (7.6, 1.6, 3.0), 0.4), HAZARD),
                      (sd_box(p, (cx, -10, 7.5), (7.6, 1.6, 3.0), 0.4), HAZARD),
                      (sd_cylinder_y(p, (cx, 1.0, 12.0), 6.9, 1.2), HAZARD),
                      (sd_sphere(p, (cx, 1.0, 18.7), 1.1), TEAL)]
        items += [(sd_cylinder_z(p, (0, -30, 9.5), 11.5, 4.0), m8.GREY),                       # reactor dome
                  (sd_sphere(p, (0, -30, 10.5), 9.5), m8.WHITE),
                  (sd_box(p, (0, -30, 7.0), (14.5, 12.5, 1.6), 0.5), HAZARD),
                  (sd_sphere(p, (0, -30, 20.2), 1.3), TEAL),
                  (sd_cylinder_z(q, (8.0, -hl + 9, 13), 3.4, 7), m8.DARK),                     # twin funnels
                  (sd_cylinder_z(q, (8.0, -hl + 9, 20.2), 3.5, 0.4), m8.FUNNEL),
                  (sd_box(p, (0, -hl + 9, 9), (12.0, 4.5, 4.0), 0.8), m8.WHITE)]
        if not damaged:                                                                         # the deck crane
            items += [(sd_cylinder_z(p, (14.5, 22, 9), 2.2, 4), YELLOW),
                      (sd_capsule(p, (14.5, 22, 13), (6.0, -4, 17), 1.1, 0.8), YELLOW)]
        else:
            items += [(sd_capsule(p, (15.5, 20, 6.5), (19.0, -2, 5.5), 1.1, 0.8), YELLOW)]     # toppled
        d, m = union(*items)
        if damaged:
            d, m = subtract((d, m), sd_sphere(p, (18.5, -2.0, 1.5), 4.2))
        return d, m
    return scene


def frigate_a():
    """Variant a's frigate: the ocean scene's CDF frigate (r08_models.frigate) at 108 x 24."""
    scene, _, _ = m8.frigate(length=108, beam=24)
    return scene


FRIGATE_A_PAD = (0.0, -54 + 17, 7.0)


def frigate_b():
    """Variant b's CDF stealth trimaran, 108 long, 34 across the outriggers."""
    L = 108.0
    hl = L / 2
    hull = m8._hull_poly(L, 14, bow=0.34, stern=0.03)
    outrig = m8._hull_poly(52, 5, bow=0.35, stern=0.05)

    def scene(p):
        q = mirror_x(p)
        qo = q - np.array([14.5, -24.0, 0.0])
        items = [(sd_plate(p, hull, 0.0, 4.6, 1.2), SLATE),
                 (sd_plate(qo, outrig, 0.0, 3.2, 1.0), SLATE),
                 (sd_box(p, (0, -36, 4.6), (16.0, 15.5, 0.9), 0.5), m8.PAD),                  # wide aft deck
                 (sd_box(q, (8.5, -22, 3.5), (6.5, 2.0, 1.2), 0.5), SLATE),                     # cross-struts
                 (np.maximum(sd_box(p, (0, 4, 9.5), (6.2, 15.0, 4.8), 0.6),
                             (np.abs(p[:, 0]) * 0.8 + p[:, 2] - 15.0) / 1.28), SLATE),              # faceted superstructure
                 (sd_box(p, (0, 16.5, 12.0), (4.4, 1.0, 1.6), 0.3), m8.WINDOW),
                 (np.maximum(sd_box(p, (0, 2.0, 18.0), (3.6, 3.6, 4.5), 0.3),
                             (np.abs(p[:, 0]) + np.abs(p[:, 1] - 2.0) + p[:, 2] - 25.0) / 1.73), m8.GREY),   # pyramid mast
                 (sd_box(p, (0, 30, 6.0), (3.6, 5.0, 1.2), 0.4), m8.DARK),                       # missile cells
                 (np.maximum(sd_box(p, (0, 40, 7.0), (3.4, 3.4, 2.6), 0.4),
                             (np.abs(p[:, 0]) + p[:, 2] - 10.4) / 1.41), SLATE),                      # faceted gun
                 (sd_capsule(p, (0, 42, 7.2), (0, 49, 7.2), 0.6), m8.DARK),
                 (sd_box(p, (0, -14, 7.0), (1.6, 6.0, 1.8), 0.4), m8.DARK),                      # exhausts
                 (sd_sphere(p, (0, hl - 2, 5.2), 0.9), m8.RED_L),
                 (sd_sphere(q, (15.5, -40, 6.0), 0.9), m8.GREEN_L)]
        d, m = union(*items)
        stripe = (np.abs(p[:, 0]) < 1.2) & (p[:, 1] > 18) & (p[:, 1] < 50)
        m = np.where(stripe & (m == SLATE), CDF_BLUE, m)
        return d, m
    return scene


FRIGATE_B_PAD = (0.0, -38.0, 7.0)

VARIANTS = {"a": (cargo_a, frigate_a, FRIGATE_A_PAD, "A - ATLANTIC LINE",
                  [(-6.0, 6.0, 3.6), (9.0, -15.0, 3.2), (-11.0, 22.0, 2.6), (4.0, -36.0, 3.0)], (0, 46)),
            "b": (cargo_b, frigate_b, FRIGATE_B_PAD, "B - REACTOR RUN",
                  [(-7.0, 4.0, 3.6), (8.0, -26.0, 3.4), (-4.0, 30.0, 2.6), (12.0, 16.0, 3.0)], (0, 49))}


def scorched(scene, spots):
    """Char decals (x, y, radius in model px) over the hull materials."""
    def out(p):
        d, m = scene(p)
        for j, (sx, sy, r) in enumerate(spots):
            ang = np.arctan2(p[:, 1] - sy, p[:, 0] - sx)
            ragged = 1.2 * r * (0.8 + 0.2 * np.sin(3 * ang + j) * np.cos(2 * ang - 2 * j))
            hit = np.hypot(p[:, 0] - sx, (p[:, 1] - sy) * 0.8) < ragged
            m = np.where(hit & np.isin(m, HULLISH), SCORCH, m)
        return d, m
    return out


def posed(base, roll=0.0, pitch=0.0, sink=0.0):
    """``base`` listing ``roll`` degrees (to starboard), down by the stern ``pitch`` degrees and
    lowered ``sink`` px through the surface, seen through the perspective camera."""
    r, pt = np.radians(roll), np.radians(pitch)

    def scene(p):
        q = p.copy() / FIT
        q[:, 2] += sink
        q = rotate_y(rotate_x(q, pt), r)
        d, m = base(q)
        return d * FIT, m
    return artkit.perspective(scene, CAMERA)


def render(scene, mats, size):
    return wfx.waterline_render(scene, mats, size, float(size[0]), z_top=Z_TOP, steps=STEPS, depth=12.0)


# =========================================================================== effects

def fire_frames(seed=2201, n=4):
    """A deck fire's flicker: a white-yellow core under orange tongues (additive)."""
    rng = np.random.default_rng(seed)
    out = []
    w, h = FIRE
    for f in range(n):
        cv = v8.Canvas(w, h)
        for k in range(5):
            x = w / 2 + rng.normal(0, 1.6)
            y = h / 2 + rng.normal(0, 1.2)
            r = rng.uniform(1.6, 3.2)
            cv.add((255, 90, 20), v8.gauss(cv.dist(x, y), r) * 0.9)
        cv.add((255, 200, 80), v8.gauss(cv.dist(w / 2, h / 2 + 0.5), 1.8 + 0.4 * np.sin(f * 2.1)) * 1.1)
        cv.add((255, 255, 220), v8.solid(cv.dist(w / 2, h / 2 + 0.6), 0.9))
        out.append(artkit.additive(cv.image()))
    return artkit.quantize_set(out, 16)


def muzzle_frames():
    out = []
    for f, g in enumerate((1.0, 0.65, 0.3)):
        cv = v8.Canvas(8, 8)
        cv.add((255, 170, 60), v8.gauss(cv.dist(4, 4), 2.2 + f * 0.5) * g * 1.2)
        cv.add((255, 250, 220), v8.solid(cv.dist(4, 4), 1.2 * (1 - 0.3 * f)) * g)
        out.append(artkit.additive(cv.image()))
    return artkit.quantize_set(out, 12)


def flak_frames(seed=2207):
    """A flak burst: an orange flash in a black core, then a dark grey puff of a few lobes
    spreading and thinning (alpha-blended, stepped)."""
    rng = np.random.default_rng(seed)
    lobes = [(rng.uniform(0, TAU), rng.uniform(0.2, 0.8), rng.uniform(0.45, 0.8)) for _ in range(6)]
    out = []
    for i in range(FLAK_FRAMES):
        t = i / (FLAK_FRAMES - 1)
        cv = v8.Canvas(FLAK, FLAK)
        R = FLAK / 2
        for a, rr, s in lobes:
            x, y = R + np.cos(a) * rr * R * (0.3 + 0.6 * t), R + np.sin(a) * rr * R * (0.3 + 0.6 * t) + 2 * t
            rad = s * R * (0.3 + 0.45 * t ** 0.6)
            cv.over((44, 44, 48), v8.solid(cv.dist(x, y), rad) * (0.95 - 0.75 * t))
            cv.over((86, 86, 90), v8.solid(cv.dist(x - rad * 0.3, y - rad * 0.3), rad * 0.55) * (0.8 - 0.7 * t))
        if i < 2:
            d = cv.dist(R, R)
            cv.over((255, 120, 40), np.clip(v8.gauss(d, R * (0.45 - 0.1 * i)) * 1.3, 0, 1) * (1 - 0.4 * i))
            cv.over((255, 230, 170), v8.solid(d, R * (0.16 - 0.06 * i)))
        out.append(wfx.stepped(cv.image(), 4))
    return artkit.quantize_set(out, 20)


def wake_frames(kind, size_hull):
    """The hull's wake on its canvas, stationary in the hull's frame: the bow waves peeling off
    down both sides, the wash along the hull, the churned prop-wash trail; the foam streams down at
    the scroll speed (16 frames, one stream cycle each)."""
    (w, h), (cx, cy) = WAKE[kind]
    hw, hh = size_hull[0] / 2, size_hull[1] / 2
    big = kind == CARGO
    bow, stern = cy - hh + 2, cy + hh - 4
    seed = 2300 if big else 2400
    out = []
    for f in range(WAKE_FRAMES):
        foam = wfx.Foam(w, h)
        ff = wfx.loop_f(f, WAKE_FRAMES)
        for side in (-1, 1):
            s8.stream(foam, ff, cx + side * 3, bow + 6, side * 0.34, 0.94, 116, 1, 80 if big else 64,
                      0.8, 5.0, 1.0, 2.1, 1.0, seed + side)
            s8.stream(foam, ff, cx + side * (hw - 3), cy, side * 0.1, 1.0, 112, 1, 24, 0.5, 3.0, 0.8, 1.4, 0.55,
                      seed + 5 + side)
        s8.stream(foam, ff, cx, stern, 0.0, 1.0, h - stern + 6, 1, 110 if big else 90, 2.5, 18.0 if big else 14.0,
                  1.2, 3.0, 0.95, seed + 9, wave=2.2)
        out.append(foam.image(4))
    return artkit.quantize_set(out, 12)


def collar(scene, size, seed):
    pts = wfx.waterline_points(scene, size, float(size[0]), step=3)
    w, h = size[0] + 2 * COLLAR_PAD, size[1] + 2 * COLLAR_PAD
    return artkit.quantize_set(wfx.collar_frames(pts, w, h, w / 2, h / 2, seed, r=1.3, push=1.4), 12)


def pip(base):
    """The HUD pip: the hull's top-down silhouette, flat white."""
    from render import sdf
    w, h = PIP
    extent = 124.0 * w / h
    hi = sdf.render(base, materials(), (w * 8, h * 8), extent, z_top=Z_TOP, shadows=False)
    a = sprite.downsample(hi, 8)[..., 3] >= 0.5
    rgba = np.zeros((h, w, 4), np.uint8)
    rgba[a] = (255, 255, 255, 255)
    return Image.fromarray(rgba, "RGBA")


def sink_frame(variant, i):
    """Sink step ``i``: the hull posed and rendered through the water on the larger canvas, with
    the collar along its waterline and the spreading foam ring baked in."""
    make, _, _, _, spots, _ = VARIANTS[variant]
    roll, pitch, depth = SINK[i]
    u = i / (SINK_STEPS - 1)
    scene = posed(scorched(make(True), spots), roll, pitch, depth)
    img = render(scene, materials(lit=False), SINK_SIZE)
    if u > 0.85:                                     # the last steps: only a dark ghost under the foam
        a = np.array(img).astype(np.float64)
        a[..., 3] *= max(0.0, 1 - (u - 0.85) / 0.15 * 0.7)
        a[..., 3] = np.floor(a[..., 3] / 255 * 3 + 0.5) / 3 * 255
        img = Image.fromarray(a.astype(np.uint8), "RGBA")
    pts = wfx.waterline_points(scene, SINK_SIZE, float(SINK_SIZE[0]), step=3)
    foam = wfx.Foam(*SINK_SIZE)
    wfx.sink_foam(foam, wfx.loop_f(i % 4, 4), pts, SINK_SIZE[0] / 2, SINK_SIZE[1] / 2, u, 2500 + i, (22, 60),
                  np.array(img)[..., 3])
    img.alpha_composite(foam.image(4))
    return img


# =========================================================================== build

def _job(job):
    variant, kind, arg = job
    make, fmake, fpad, _, spots, _ = VARIANTS[variant]
    if kind == "cargo":
        return render(posed(make()), materials(), CARGO_SIZE)
    if kind == "damaged":
        return render(posed(scorched(make(True), spots), DAMAGE_ROLL), materials(), CARGO_SIZE)
    if kind == "frigate":
        return render(posed(fmake()), materials(fpad), FRIGATE_SIZE)
    if kind == "sink":
        return sink_frame(variant, arg)
    if kind == "collar-cargo":
        return collar(posed(make()), CARGO_SIZE, 2601)
    if kind == "collar-frigate":
        return collar(posed(fmake()), FRIGATE_SIZE, 2602)
    if kind == "wake-cargo":
        return wake_frames(CARGO, (36, 116))
    if kind == "wake-frigate":
        return wake_frames(FRIGATE, (24 if variant == "a" else 34, 108))
    if kind == "pip":
        return pip(make())
    raise ValueError(kind)


def frame_sets(variant):
    jobs = ([(variant, "cargo", None), (variant, "damaged", None), (variant, "frigate", None)]
            + [(variant, "sink", i) for i in range(SINK_STEPS)]
            + [(variant, k, None) for k in ("collar-cargo", "collar-frigate", "wake-cargo", "wake-frigate", "pip")])
    with ProcessPoolExecutor() as pool:
        res = list(pool.map(_job, jobs))
    hulls = artkit.quantize_set(res[:2] + res[3:3 + SINK_STEPS], HULL_COLOURS)   # one palette: no flicker
    frigate = artkit.quantize_set([res[2]], HULL_COLOURS)
    k = 3 + SINK_STEPS
    return {CARGO: hulls[:1], f"{CARGO}-damaged": hulls[1:2], f"{CARGO}-fire": fire_frames(),
            f"{CARGO}-collar": res[k], f"{CARGO}-wake": res[k + 2], f"{CARGO}-sink": hulls[2:],
            f"{CARGO}-pip": [res[k + 4]],
            FRIGATE: frigate, f"{FRIGATE}-collar": res[k + 1], f"{FRIGATE}-wake": res[k + 3],
            f"{FRIGATE}-muzzle": muzzle_frames(), f"{FRIGATE}-flak": flak_frames()}


def project(x, y, z, size, roll=0.0):
    """A model point on the sprite (px from its top left), rolled and through the camera."""
    v = rotate_y(np.array([[x, y, z]], float) * FIT, -np.radians(roll))[0]
    k = CAMERA / (CAMERA - v[2])
    return [int(round(size[0] / 2 + v[0] * k)), int(round(size[1] / 2 - v[1] * k))]


def pivots(variant):
    _, _, _, _, spots, gun = VARIANTS[variant]
    (cw, _), (ccx, ccy) = WAKE[CARGO]
    (fw, _), (fcx, fcy) = WAKE[FRIGATE]
    smoke = spots[0]
    return {
        CARGO: {"variant": variant,
                "frames": {"cargo-ship": "afloat", "cargo-ship-damaged": "after the first slam"},
                "smoke": project(smoke[0], smoke[1], 8.0, CARGO_SIZE, DAMAGE_ROLL),
                "fire": project(spots[1][0], spots[1][1], 8.0, CARGO_SIZE, DAMAGE_ROLL),
                "collar": [-COLLAR_PAD, -COLLAR_PAD],
                "wake": [CARGO_SIZE[0] // 2 - ccx, CARGO_SIZE[1] // 2 - ccy],
                "sink": [(CARGO_SIZE[0] - SINK_SIZE[0]) // 2, (CARGO_SIZE[1] - SINK_SIZE[1]) // 2],
                "note": "offsets: the top left of each overlay relative to the hull frame's top left"},
        FRIGATE: {"variant": variant,
                  "muzzle": project(gun[0], gun[1] + 1, 7.2, FRIGATE_SIZE),
                  "collar": [-COLLAR_PAD, -COLLAR_PAD],
                  "wake": [FRIGATE_SIZE[0] // 2 - fcx, FRIGATE_SIZE[1] // 2 - fcy],
                  "note": "offsets: the top left of each overlay relative to the hull frame's top left"},
    }


def build(variant):
    sets = frame_sets(variant)
    for name, frames in sets.items():
        artkit.write_frames(name, frames, SOURCE + f", variant {variant}", single=name.endswith("-pip"))
        print(f"{name}: {len(frames)} frames, {frames[0].width}x{frames[0].height}")
    for name, data in pivots(variant).items():
        artkit.write_pivots(name, SOURCE + f", variant {variant}", data)
    return sets


NAMES = [CARGO, f"{CARGO}-damaged", f"{CARGO}-fire", f"{CARGO}-collar", f"{CARGO}-wake", f"{CARGO}-sink",
         f"{CARGO}-pip", FRIGATE, f"{FRIGATE}-collar", f"{FRIGATE}-wake", f"{FRIGATE}-muzzle", f"{FRIGATE}-flak"]


def load_sets():
    return {n: artkit.load_frames(n) for n in NAMES}


# =========================================================================== review

def draw_ship(img, sets, pv, kind, x, y, i, state="ok", sink=0, shadow=True):
    """A hull at (x, y) (its centre) with its wake under it and its collar over it, as the game
    layers them; ``state`` ok, damaged or sinking (step ``sink``)."""
    w, h = img.size
    name = CARGO if kind == "cargo" else FRIGATE
    size = CARGO_SIZE if kind == "cargo" else FRIGATE_SIZE
    left, top = int(round(x - size[0] / 2)), int(round(y - size[1] / 2))
    data = pv[name]
    if state != "sinking":
        wk = sets[f"{name}-wake"][i % WAKE_FRAMES]
        img.alpha_composite(wk, (left + data["wake"][0], top + data["wake"][1]))
        frame = sets[f"{CARGO}-damaged"][0] if state == "damaged" else sets[name][0]
        if shadow:
            sh = Image.fromarray(np.dstack([np.zeros(frame.size[::-1] + (3,), np.uint8),
                                            (np.array(frame)[..., 3] > 127).astype(np.uint8) * 70]), "RGBA")
            img.alpha_composite(sh, (left + 3, top + 4))
        img.alpha_composite(frame, (left, top))
        img.alpha_composite(sets[f"{name}-collar"][(i // 3) % 4], (left + data["collar"][0], top + data["collar"][1]))
        if state == "damaged":
            fx, fy = data["fire"]
            img = artkit.add_light(img, sets[f"{CARGO}-fire"][(i // 2) % 4], (left + fx - 6, top + fy - 6))
    else:
        fr = sets[f"{CARGO}-sink"][min(sink, SINK_STEPS - 1)]
        img.alpha_composite(fr, (left + data["sink"][0], top + data["sink"][1]))
    return img


def review(variant, sets, pv):
    title = VARIANTS[variant][3]
    pair = []
    for state in ("ok", "damaged"):
        cell = wfx.sea_frame(10, 150, 200, 160, 200, speed=0)
        cell = draw_ship(cell, sets, pv, "cargo", 45, 92, 4, state)
        if state == "ok":
            cell = draw_ship(cell, sets, pv, "frigate", 112, 92, 4)
        pair.append(cell)
    pipcell = Image.new("RGBA", (40, 22), artkit.PLATE)
    for k, col in enumerate(((90, 220, 120), (255, 190, 60), (70, 70, 70))):
        tint = np.array(sets[f"{CARGO}-pip"][0]).astype(np.float64)
        tint[..., :3] *= np.array(col) / 255
        pipcell.alpha_composite(Image.fromarray(tint.astype(np.uint8), "RGBA"), (3 + 12 * k, 2))
    rows = [
        ("CARGO SHIP 56X120 AND FRIGATE 40X110, AFLOAT (BOW UP)", sets[CARGO] + sets[FRIGATE], 3, False),
        ("CARGO SHIP AFTER THE FIRST SLAM: LISTING 12 DEG, SCORCHED, A BREACH (+ FIRE 12X12 ADDITIVE)",
         sets[f"{CARGO}-damaged"] + sets[f"{CARGO}-fire"], 3, False),
        ("ON THE OCEAN STAND-IN WITH WAKE, COLLAR AND SHADOW: THE PAIR; DAMAGED WITH ITS FIRE", pair, 2, False),
        ("SINKING, 10 STEPS 72X136: LIST TO 32 DEG, DOWN BY THE STERN, THROUGH THE SURFACE, FOAM RING",
         sets[f"{CARGO}-sink"], 2, False),
        ("FOAM COLLARS (4-FRAME LOOPS)", sets[f"{CARGO}-collar"] + sets[f"{FRIGATE}-collar"], 2, False),
        ("WAKES (EVERY 4TH OF 16 FRAMES)", sets[f"{CARGO}-wake"][::4] + sets[f"{FRIGATE}-wake"][::4], 2, False),
        ("FRIGATE: BOW GUN FLASH 8X8 (ADDITIVE), FLAK BURST 24X24 ON LOW-AIR", sets[f"{FRIGATE}-muzzle"], 6, True),
        ("FLAK BURST", sets[f"{FRIGATE}-flak"], 4, False),
        ("HUD PIP 8X18, TINTED GREEN / AMBER (HIT) / DARK (SUNK) AT RUN TIME", [pipcell], 4, False),
        ("GREYSCALE CHECK", [pair[0].convert("L").convert("RGBA")], 2, False),
        ("1X", sets[CARGO] + sets[f"{CARGO}-damaged"] + sets[FRIGATE] + sets[f"{CARGO}-sink"][::3], 1, False),
    ]
    sheet = artkit.review_sheet(f"CONVOY SHIPS {title} - PRODUCTION SPRITES (ROUND 33 PROPOSAL {variant.upper()})",
                                rows, width=1500, batch=BATCH)
    folder = CONCEPT if variant == PRODUCTION else CONCEPT / "rejected"   # the user's pick, round 33
    png = folder / f"convoy-ships-{REVIEW_ROUND}-{variant}.png"
    gif = folder / f"convoy-ships-{REVIEW_ROUND}-{variant}.gif"
    sheet.convert("RGB").save(png, optimize=True)
    write_gif(review_loop(sets, pv), gif, fps=20, colors=128)
    print(f"review: {png.relative_to(ROOT)}, {gif.relative_to(ROOT)}")


def review_loop(sets, pv):
    """The convoy at its stations (Level 11's, the field's 360x300 below y 230 at 2x) on the ocean
    stand-in scrolling at 140 px/s: the frigate's flak overhead, then Mbeki takes a slam (a large
    water blast), lists, burns and smokes; a second slam and it sinks in its foam ring over 3 s."""
    w, h, n = 360, 300, 140
    x0, y0 = 60, 230
    ships = [("cargo", 130 - x0, 380 - y0), ("cargo", 240 - x0, 350 - y0), ("cargo", 350 - x0, 380 - y0),
             ("frigate", 240 - x0, 480 - y0)]
    smoke = artkit.load_frames("ship-smoke")
    blast = artkit.load_frames("explosion-water-large")
    t_hit, t_sink = 30, 66
    rng = np.random.default_rng(9)
    flak = [(int(k), rng.uniform(40, w - 40), rng.uniform(20, 120)) for k in np.arange(4, n, 13)]
    puffs = []
    out = []
    for i in range(n):
        img = wfx.sea_frame(i, w, h, x0, y0)
        for k, (kind, x, y) in enumerate(ships):
            state = "ok"
            if k == 1 and i >= t_hit:
                state = "damaged" if i < t_sink else "sinking"
            img = draw_ship(img, sets, pv, kind, x, y, i + 5 * k, state, sink=(i - t_sink) // 6)
            if k == 1 and state == "damaged" and i % 3 == 0:
                sx, sy = pv[CARGO]["smoke"]
                puffs.append((i, x - CARGO_SIZE[0] / 2 + sx, y - CARGO_SIZE[1] / 2 + sy))
        for born, sx, sy in puffs:
            age = i - born
            fr = wfx.timed(smoke, age, 3)
            if fr is not None:
                wfx.put(img, fr, sx + age * 0.3, sy + age * 140 / 20 * 0.6)
        for t0 in (t_hit, t_sink):
            fr = wfx.timed(blast, i - t0, 1)
            if fr is not None:
                wfx.put(img, fr, ships[1][1] + 6, ships[1][2] - 8)
        fx_, fy_ = pv[FRIGATE]["muzzle"]
        for t0, x, y in flak:
            fr = wfx.timed(sets[f"{FRIGATE}-flak"], i - t0, 2)
            if fr is not None:
                wfx.put(img, fr, x, y)
            mz = wfx.timed(sets[f"{FRIGATE}-muzzle"], i - t0 + 2, 1)
            if mz is not None:
                fx0 = ships[3][1] - FRIGATE_SIZE[0] / 2 + fx_
                fy0 = ships[3][2] - FRIGATE_SIZE[1] / 2 + fy_
                img = wfx.put_light(img, mz, fx0, fy0)
        out.append(sprite.enlarge(img, 2))
    return out


if __name__ == "__main__":
    artkit.REVIEW_ROUND = REVIEW_ROUND
    args = sys.argv[1:]
    chosen = args[args.index("--variant") + 1] if "--variant" in args else PRODUCTION
    if "--review" not in args:
        sets = build(chosen)
    else:
        chosen = PRODUCTION
        sets = load_sets()
    review(chosen, sets, pivots(chosen))
    for other in sorted(set(VARIANTS) - {chosen}):
        review(other, frame_sets(other), pivots(other))
