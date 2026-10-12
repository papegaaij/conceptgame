#!/usr/bin/env python3
"""Production art, two variants: Nansen Relay, Level 13's grid relay on the pack ice that the player
defends (design/allies, "Nansen Relay"; design/campaign/act-2-homefront/level-13-polar-relay; M5 part G
batch, concept round 35, step G2). It had no concept of its own, only the station of the chosen
arctic scene (scene-arctic-r09-a: tools/concept/scenes_r08.py's `_station`, kit-bashed from
render/station.py), so this script proposes it as two sets at production quality (decision G9 = a,
pending the user's review); both are produced, a in the game until the pick (as Level 11's ships).

  a  "Ice station" (the arctic scene's station, consolidated into one 96 px compound): a steel
     foundation deck, two glass habitat domes with warm lights, the big up-tilted dish with its red
     feed light, a lattice mast with a red beacon, the control block with a lit window strip, a
     radiator bank, fuel tanks and the helipad (yellow ring, white H) at the south-west corner
  b  "Grid node" (a hardened CDF relay): a hexagonal deck, a slate octagonal blockhouse with a CDF-blue
     band, three phased-array faces tilted out to the north, south-west and south-east, a central mast
     with a red beacon and a crossbar, the helipad to the south, a prefab crew module (warm windows)
     to the north-west and fuel tanks to the north-east

Outputs (assets/sprites/; written only with --assets [--variant a|b], in the content step's time:
no data claims the relay yet, and packAtlases fails on sprites no data claims; the review files are
always written):
  nansen-relay_0..3          96x96 (hit box 80x80 inside): 0 intact; 1 damaged (shown below 50 %:
                             scorched, a breach in a module, the dish knocked askew, a radiator bent);
                             2 critical (below 25 %: breaches, a dome / an array face smashed, the mast
                             leaning, heavy scorch); 3 destroyed (dark: every light out, charred, the
                             mast down across the deck, the domes / faces caved in); the warm windows
                             lit in 0-2 (the crew at work) and the red beacons lit in 0-2
  nansen-relay-lights_0..9   96x96 additive: grid online at t=220 (README: the lights come on in
                             sequence): 0-7 the sequence (4 fps, 2 s): the helipad's edge lights, then
                             the windows brighten, the dish / array status lights in cyan one by one,
                             the mast's strobe; 8-9 the steady loop (all on, the strobe blinking, 2 fps)
  nansen-relay-fire_0..3     16x16 additive: a small fire's flicker (8 fps) at the state's fire points
  nansen-relay-smoke_0..7    32x32 the destroyed relay's heavy smoke column puff (dark grey, growing and
                             thinning, stepped alpha; 15 steps a frame, 2 s), emitted every 0.15 s from
                             the pivots' column point and moved by the game (≈ 42 px/s up the screen,
                             16 px/s with the wind; the review's values); the damaged smoke below 50 %
                             is the shared ship-smoke (16x16) from the state's smoke points, as Level
                             11's damaged cargo ship
  nansen-relay-pip           12x12 HUD icon: the dish and mast, flat white, tinted at run time
                             (MissionPanel loads <ally>-pip)
  pivots/nansen-relay.json   per state the smoke and fire points, the column point, the helipad's
                             centre (the supply drone's lift-off), px from the frame's top left
  design/allies/concept/relay-r35-a.png/.gif, relay-r35-b.png/.gif   the review sheets and loops (the
                             relay on the halted frame's stand-in plate: hit flashes, smoke below 50 %,
                             fire below 25 %, grid online's sequence, destroyed with its column)

The hit flash is the game's (a white silhouette of the current frame for 2 steps, as the convoy's).

Run: python3 tools/art/relay.py [--assets] [--variant a|b]   (~1 min)
"""
import json
import sys
from concurrent.futures import ProcessPoolExecutor

import numpy as np
from PIL import Image, ImageDraw

import artkit
from artkit import DESIGN, PIVOTS, TAU, sprite

import ground_targets as gt  # noqa: E402  (concept script, imported unchanged: scorch)
import l13_props  # noqa: E402  (tools/art: the halted frame's stand-in plate, additive light discs)
from render import station  # noqa: E402  (the arctic scene's station kit: its materials)
from render.palette import B  # noqa: E402
from render.sdf import (Material, mirror_x, panel_lines, rotate_x, rotate_y, rotate_z, sd_box,  # noqa: E402
                        sd_capsule, sd_cylinder_x, sd_cylinder_z, sd_polygon2, sd_sphere, union)

SCRIPT = "relay.py"
BATCH = "M5 part G batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
REVIEW_ROUND = "r35"
CONCEPT = DESIGN / "allies" / "concept"
SLUG = "nansen-relay"
PRODUCTION = "a"                     # a in the game until the user's pick (round 35)
SIZE = (96, 96)
COLOURS = 48
STATES = 4
LIGHT_FRAMES = 10
FIRE, SMOKE = (16, 16), (32, 32)

# station.materials(B): 0 HULL, 1 DARK, 2 SOLAR (grid), 3 ACCENT (orange), 4 RED (emissive), 5 WARM
# (emissive), 6 GLASS; then ours:
HULL, DARK, SOLAR, ACCENT, RED, WARM, GLASS = range(7)
DECK, PAD, MARK, PAD_H, BLUE, SLATE, HOLE, TANK = range(7, 15)
CYAN = np.array([110, 235, 255], float)
WARM_RGB = np.array([255, 200, 120], float)
WHITE = np.array([240, 246, 255], float)
RED_RGB = np.array([255, 70, 60], float)
FIRE_RGB = np.array([255, 150, 50], float)


def mats(state):
    m = station.materials(B)
    if state >= 3:                                    # power down: nothing glows
        m[RED] = Material((0.22, 0.08, 0.08), metal=0.2, shininess=40, spec=0.4)
        m[WARM] = Material((0.20, 0.18, 0.14), metal=0.2, shininess=40, spec=0.3)
    return m + [Material((0.42, 0.44, 0.47), metal=0.4, shininess=30, spec=0.35, pattern=panel_lines(0.11, 0.05, 0.8)),
                Material((0.30, 0.32, 0.34), metal=0.3, shininess=20, spec=0.3),
                Material((0.95, 0.76, 0.16), metal=0.1, shininess=20, spec=0.3),
                Material((0.92, 0.93, 0.94), metal=0.1, shininess=20, spec=0.3),
                Material((0.14, 0.34, 0.70), metal=0.3, shininess=50, spec=0.5),
                Material((0.30, 0.34, 0.38), metal=0.5, shininess=40, spec=0.45,
                         pattern=panel_lines(0.09, 0.05, 0.8)),
                Material((0.05, 0.05, 0.06), metal=0.0, shininess=4, spec=0.0),
                Material((0.80, 0.82, 0.80), metal=0.5, shininess=60, spec=0.6)]


def ring(p, c, r, w, z0, z1):
    rr = np.hypot(p[:, 0] - c[0], p[:, 1] - c[1])
    return np.maximum(np.abs(rr - r) - w, np.maximum(z0 - p[:, 2], p[:, 2] - z1))


def dome(p, c, r, state_cave=0.0, glass=GLASS):
    """A glass habitat dome on its ring base with an airlock (station.dome_part, rebuilt in place);
    ``state_cave`` 0..1 flattens and darkens it (smashed)."""
    base = sd_cylinder_z(p, (c[0], c[1], 1.5), r + 2.5, 1.5)
    q = p - np.array([c[0], c[1], 1.5])
    q[:, 2] /= (1 - 0.6 * state_cave)
    shell = np.maximum(sd_sphere(q, (0, 0, 0), r) * (1 - 0.6 * state_cave), -q[:, 2])
    lock = sd_box(p, (c[0] + r + 2.0, c[1], 3.5), (3.0, 2.6, 2.4), 0.6)
    lamp = sd_sphere(p, (c[0], c[1], 1.5 + r * (1 - 0.6 * state_cave) - 1.2), 1.6)
    out = [(base, HULL), (shell, HOLE if state_cave > 0.5 else glass), (lock, HULL)]
    if state_cave < 0.5:
        out.append((lamp, WARM))
    return out


def dish(p, c, r, tilt, aim=0.6):
    """The big dish on its pedestal, tilted ``tilt`` rad toward the angle ``aim`` (screen-up = pi/2)."""
    ped = sd_cylinder_z(p, (c[0], c[1], 3.0), 3.5, 3.0)
    q = p - np.array([c[0], c[1], 9.0])
    q = rotate_z(q, aim)
    q = rotate_x(q, tilt)
    bowl = np.maximum(sd_sphere(q, (0, 0, r * 0.9), r * 1.15), -sd_sphere(q, (0, 0, r * 1.15), r * 1.18))
    bowl = np.maximum(bowl, q[:, 2] - r * 0.5)
    feed = sd_capsule(q, (0, 0, 0.2), (0, 0, r * 0.62), 0.7)
    tip = sd_sphere(q, (0, 0, r * 0.62), 1.4)
    return [(ped, DARK), (bowl, HULL), (feed, DARK), (tip, RED)]


def mast(p, c, height, lean=0.0, lean_dir=0.0, bar=False):
    """A lattice mast (four legs and rungs) with a red beacon; ``lean`` rad toward ``lean_dir``."""
    q = p - np.array([c[0], c[1], 0.0])
    q = rotate_z(q, lean_dir)
    q = rotate_y(q, -lean)
    q = rotate_z(q, -lean_dir)
    a = mirror_x(q)
    a[:, 1] = np.abs(q[:, 1])
    legs = sd_capsule(a, (2.2, 2.2, 0.0), (1.0, 1.0, height), 0.45)
    zr = np.clip(np.round(q[:, 2] / 4.0) * 4.0, 2.0, height - 2.0)
    rung = np.maximum(np.abs(q[:, 2] - zr) - 0.35, np.maximum(np.abs(q[:, 0]), np.abs(q[:, 1])) - 2.0)
    rung = np.maximum(rung, -(np.maximum(np.abs(q[:, 0]), np.abs(q[:, 1])) - 1.2))
    beacon = sd_sphere(q, (0.0, 0.0, height + 1.0), 1.6)
    out = [(np.minimum(legs, rung), DARK), (beacon, RED)]
    if bar:
        out.append((sd_box(q, (0.0, 0.0, height - 4.0), (6.0, 0.5, 0.5), 0.2), DARK))
    return out


def breaches(p, holes):
    return np.minimum.reduce([sd_sphere(p, h[:3], h[3]) for h in holes]) if holes else np.full(len(p), 1e3)


# =========================================================================== variant a: ice station

A_PAD = (-21.0, -21.0)


def scene_a(state):
    cave = {0: (0, 0), 1: (0, 0), 2: (0.0, 1.0), 3: (1.0, 1.0)}[state]
    holes = {0: [], 1: [(6.0, -2.0, 8.0, 3.0)], 2: [(6.0, -2.0, 8.0, 3.5), (12.0, -30.0, 2.0, 4.0)],
             3: [(6.0, -2.0, 8.0, 5.0), (12.0, -30.0, 2.0, 5.0), (-4.0, -4.0, 8.0, 4.0)]}[state]
    tilt = (0.45, 0.75, 0.95, 1.5)[state]
    lean = (0.0, 0.0, 0.3, 1.45)[state]

    def scene(p):
        deck = sd_box(p, (0.0, 0.0, 0.0), (42.0, 42.0, 1.5), 7.0)
        items = [(deck, DECK)]
        items += _pad_part(p, A_PAD, 13.5)
        items += dome(p, (-21.0, 17.0), 12.5, cave[0])
        items += dome(p, (-1.0, 32.0), 7.5, cave[1])
        items += dish(p, (21.0, 19.0), 14.0, tilt, aim=-0.9 if state < 3 else -0.4)
        block = sd_box(p, (4.0, -4.0, 4.0), (10.0, 7.0, 4.0), 1.0)
        stripe = sd_box(p, (4.0, -11.2, 5.0), (8.0, 0.4, 1.2), 0.2)
        roof = sd_box(p, (4.0, -4.0, 8.2), (8.5, 1.4, 0.4), 0.2)
        items += [(block, HULL), (stripe, WARM), (roof, BLUE)]
        rad = np.minimum.reduce([sd_box(p, (13.0, -30.0 + 5.5 * i, 2.0 + (0.8 * i if state >= 1 and i == 1 else 0)),
                                        (13.0, 1.8, 0.5), 0.3) for i in range(3)])
        rad_frame = sd_box(p, (-1.0, -24.5, 2.5), (1.4, 8.0, 1.2), 0.4)
        tanks = np.minimum(sd_cylinder_x(p, (31.0, -33.0, 3.0), 3.0, 6.0), sd_cylinder_x(p, (31.0, -25.5, 3.0), 3.0, 6.0))
        items += [(rad, HULL), (rad_frame, DARK), (tanks, TANK)]
        items += mast(p, (34.0, -6.0), 26.0, lean, lean_dir=2.6)
        cables = np.minimum.reduce([sd_capsule(p, (-8.0, -4.0, 1.7), (-14.0, 8.0, 1.7), 0.6),
                                    sd_capsule(p, (14.0, 0.0, 1.7), (28.0, -6.0, 1.7), 0.6)])
        items.append((cables, DARK))
        d, m = union(*items, k=0.4)
        if holes:
            cut = np.maximum(breaches(p, holes), 1.6 - p[:, 2])     # never through the deck
            inside = (cut < 0.8) & (p[:, 2] > 1.4)
            d = np.maximum(d, -cut)
            m = np.where(inside, HOLE, m)
        return d, m
    return scene


def lights_a():
    """Grid online's lights in sprite px, in sequence groups: pad edge, windows, dish, mast."""
    w, h = SIZE
    to = lambda x, y: (w / 2 + x, h / 2 - y)        # noqa: E731
    pad = [to(A_PAD[0] + 12.0 * np.cos(a), A_PAD[1] + 12.0 * np.sin(a)) for a in np.linspace(0, TAU, 8, endpoint=False)]
    windows = [to(x, -11.4) for x in (-2.0, 2.0, 6.0, 10.0)] + [to(-21.0, 17.0), to(-1.0, 32.0)]
    status = [to(21.0 + 10 * np.cos(a), 19.0 + 10 * np.sin(a)) for a in (0.6, 1.8, 3.0, 4.2)] + [to(18.0, -30.0)]
    mast_pt = [to(34.0, -6.0)]
    return pad, windows, status, mast_pt


# =========================================================================== variant b: grid node

def hexagon(p, r):
    q = np.abs(p[:, :2])
    return np.maximum(q[:, 0] * 0.866 + q[:, 1] * 0.5, q[:, 1]) - r


B_PAD = (0.0, -29.0)
FACES = (np.pi / 2, np.pi / 2 + TAU / 3, np.pi / 2 - TAU / 3)        # north, south-west, south-east


def scene_b(state):
    smashed = {0: (), 1: (), 2: (1,), 3: (0, 1, 2)}[state]
    holes = {0: [], 1: [(8.0, 8.0, 10.0, 3.2)], 2: [(8.0, 8.0, 10.0, 4.0), (-24.0, 20.0, 5.0, 3.5)],
             3: [(8.0, 8.0, 10.0, 5.5), (-24.0, 20.0, 5.0, 5.0), (-4.0, -6.0, 10.0, 5.0)]}[state]
    lean = (0.0, 0.0, 0.32, 1.45)[state]

    def scene(p):
        deck = np.maximum(hexagon(p, 42.0), np.abs(p[:, 2]) - 1.5) - 0.5
        items = [(deck, DECK)]
        items += _pad_part(p, B_PAD, 11.5)
        ang = np.arctan2(p[:, 1], p[:, 0])
        rr = np.hypot(p[:, 0], p[:, 1])
        octa = rr * np.cos(((ang + np.pi / 8) % (TAU / 8)) - np.pi / 8) - 15.0
        block = np.maximum(octa, np.abs(p[:, 2] - 5.5) - 5.5) - 0.3
        band = np.maximum(np.abs(octa - 0.3) - 0.6, np.abs(p[:, 2] - 8.0) - 0.9)
        roof = np.maximum(octa + 3.5, np.abs(p[:, 2] - 11.2) - 0.3)
        items += [(block, SLATE), (band, BLUE), (roof, DARK)]
        for i, a in enumerate(FACES):
            q = rotate_z(p, -(a - np.pi / 2))               # the face's outward direction -> +y
            q = q - np.array([0.0, 17.5, 5.0])
            q = rotate_x(q, -0.55 if i not in smashed else -1.2)
            face = sd_box(q, (0.0, 0.0, 0.0), (10.5, 1.2, 6.0), 0.4)
            frame = np.maximum(sd_box(q, (0.0, 0.0, 0.0), (11.3, 1.6, 6.8), 0.4), -sd_box(q, (0, 0, 0), (10.4, 3.0, 5.9)))
            items += [(face, SOLAR if i not in smashed else HOLE), (frame, DARK)]
        items += mast(p, (0.0, 0.0), 30.0, lean, lean_dir=-0.4, bar=True)
        module = sd_box(rotate_z(p - np.array([-26.0, 19.0, 0.0]), -0.52), (0.0, 0.0, 3.6), (8.5, 5.0, 3.6), 0.9)
        wins = sd_box(rotate_z(p - np.array([-26.0, 19.0, 0.0]), -0.52), (0.0, -5.2, 4.4), (6.5, 0.4, 0.9), 0.1)
        tanks = np.minimum(sd_cylinder_z(p, (25.0, 18.0, 3.5), 4.2, 3.5), sd_cylinder_z(p, (31.0, 9.0, 3.0), 3.4, 3.0))
        gen = sd_box(p, (25.0, -14.0, 2.6), (5.0, 4.0, 2.6), 0.8)
        cables = np.minimum.reduce([sd_capsule(p, (14.0, -8.0, 1.7), (22.0, -12.0, 1.7), 0.6),
                                    sd_capsule(p, (-14.0, 6.0, 1.7), (-20.0, 14.0, 1.7), 0.6)])
        edge = hexagon(p, 37.5)
        seg = np.cos(np.arctan2(p[:, 1], p[:, 0]) * 6 + 0.52) > -0.55        # gaps between the wall runs
        walls = np.where(seg, np.maximum(np.abs(edge) - 1.3, np.abs(p[:, 2] - 3.0) - 1.6) - 0.3, 1e3)
        walls = np.maximum(walls, -(np.hypot(p[:, 0] - B_PAD[0], p[:, 1] - B_PAD[1]) - 13.0))   # open to the pad
        hazard = np.maximum(np.abs(hexagon(p, 40.5)) - 0.6, np.abs(p[:, 2] - 1.6) - 0.3)
        items += [(module, HULL), (wins, WARM), (tanks, TANK), (gen, ACCENT), (cables, DARK), (walls, PAD),
                  (hazard, MARK)]
        d, m = union(*items, k=0.4)
        if holes:
            cut = np.maximum(breaches(p, holes), 1.6 - p[:, 2])     # never through the deck
            inside = (cut < 0.8) & (p[:, 2] > 1.4)
            d = np.maximum(d, -cut)
            m = np.where(inside, HOLE, m)
        return d, m
    return scene


def lights_b():
    w, h = SIZE
    to = lambda x, y: (w / 2 + x, h / 2 - y)        # noqa: E731
    pad = [to(B_PAD[0] + 10.0 * np.cos(a), B_PAD[1] + 10.0 * np.sin(a)) for a in np.linspace(0, TAU, 8, endpoint=False)]
    windows = [to(-26.0 + 5.5 * np.cos(-0.52) * s, 19.0 + 5.5 * np.sin(-0.52) * s - 5.0) for s in (-1, 0, 1)]
    status = []
    for a in FACES:
        for s in (-1, 1):
            c = np.array([np.cos(a), np.sin(a)]) * 20.0 + np.array([-np.sin(a), np.cos(a)]) * 7.0 * s
            status.append(to(*c))
    mast_pt = [to(0.0, 0.0)]
    return pad, windows, status, mast_pt


VARIANTS = {"a": (scene_a, lights_a, A_PAD, "A - ICE STATION (THE ARCTIC SCENE'S STATION, CONSOLIDATED)"),
            "b": (scene_b, lights_b, B_PAD, "B - GRID NODE (HARDENED BLOCKHOUSE, THREE PHASED-ARRAY FACES)")}

# per variant and state: smoke points (ship-smoke) and fire points (sprite px), the column point
POINTS = {
    "a": {"smoke": [[], [(54, 50)], [(54, 50), (60, 80), (30, 34)], [(54, 50), (60, 80), (30, 34), (44, 52)]],
          "fire": [[], [], [(55, 52), (61, 79)], [(55, 52), (44, 53)]], "column": (50, 48)},
    "b": {"smoke": [[], [(56, 40)], [(56, 40), (24, 27), (47, 54)], [(56, 40), (24, 27), (47, 54), (52, 58)]],
          "fire": [[], [], [(56, 41), (24, 28)], [(56, 41), (46, 55)]], "column": (48, 50)},
}


def helipad_px(variant):
    c = VARIANTS[variant][2]
    return SIZE[0] / 2 + c[0], SIZE[1] / 2 - c[1]


def helipad(variant):
    """The helipad's centre relative to the frame's centre (px, y down): the supply drone's lift-off."""
    x, y = helipad_px(variant)
    return x - SIZE[0] / 2, y - SIZE[1] / 2


def _pad_part(p, c, r):
    """A round pad (dark grey) with the yellow ring and a white H."""
    base = sd_cylinder_z(p, (c[0], c[1], 1.9), r, 0.5)
    mark = ring(p, c, r - 2.2, 0.8, 2.1, 2.6)
    q = p - np.array([c[0], c[1], 0.0])
    h = np.minimum.reduce([sd_box(q, (-3.2, 0.0, 2.35), (0.9, 4.5, 0.25)), sd_box(q, (3.2, 0.0, 2.35), (0.9, 4.5, 0.25)),
                           sd_box(q, (0.0, 0.0, 2.35), (3.0, 0.8, 0.25))])
    return [(base, PAD), (mark, MARK), (h, PAD_H)]


# =========================================================================== frames

def state_frame(job):
    variant, state = job
    scene = VARIANTS[variant][0](state)
    hi, factor = artkit.render_hi(scene, mats(state), SIZE, float(SIZE[0]), z_top=36.0, steps=160)
    arr = np.array(artkit.native(hi, factor, crisp=60)).astype(np.float64)
    rng = np.random.default_rng(3700 + state + (10 if variant == "b" else 0))
    pts = POINTS[variant]
    if state >= 1:
        spots = [(x, y, 5.0 + 2.5 * state) for x, y in pts["smoke"][state]]
        spots += [(rng.uniform(20, 76), rng.uniform(20, 76), rng.uniform(3, 6)) for _ in range(2 * state)]
        arr = gt.scorch(arr, spots, 3710 + state)
    if state == 3:                                        # charred, cold
        solid = arr[..., 3] > 0
        lum = arr[..., :3].mean(axis=-1, keepdims=True)
        arr[..., :3] = np.where(solid[..., None], (arr[..., :3] * 0.35 + lum * 0.65) * 0.55, arr[..., :3])
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")


def lights_frame(job):
    """Grid online, frame i: 0-7 the sequence, 8-9 the steady loop."""
    variant, i = job
    pad, windows, status, mast_pt = VARIANTS[variant][1]()
    pts = []
    if i >= 0:
        on = len(pad) if i >= 1 else 4
        pts += [(x, y, 4.0, 1.0, WHITE * 0.95) for x, y in pad[:on]]
    if i >= 2:
        pts += [(x, y, 6.0, 0.85 if i >= 3 else 0.5, WARM_RGB) for x, y in windows]
    if i >= 3:
        n = min(len(status), (i - 2) * 2)
        pts += [(x, y, 5.0, 1.0, CYAN) for x, y in status[:n]]
    if i >= 6:
        strobe = i in (6, 7, 8)
        pts += [(x, y, 10.0 if strobe else 4.0, 1.0 if strobe else 0.8, WHITE if strobe else RED_RGB) for x, y in mast_pt]
    if i == 7:                                            # the sync flash: a ring of cyan over the compound
        w, h = SIZE
        yy, xx = np.mgrid[0:h, 0:w].astype(np.float64) + 0.5
        d = np.hypot(xx - w / 2, yy - h / 2)
        a = l13_props.steps(np.clip(1 - np.abs(d - 40) / 5.0, 0, 1) * 0.75, 4)
        ringimg = l13_props.premultiplied(CYAN, a)
        img = l13_props.glow_disc(SIZE, pts)
        return Image.fromarray(np.maximum(np.array(img), np.array(ringimg)), "RGBA")
    return l13_props.glow_disc(SIZE, pts)


def fire_frame(k):
    w, h = FIRE
    rng = np.random.default_rng(3720 + k)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64) + 0.5
    a = np.zeros((h, w))
    for _ in range(5):
        cx, cy = w / 2 + rng.uniform(-2.5, 2.5), h / 2 + rng.uniform(-1.5, 2.5)
        r = rng.uniform(2.0, 4.5)
        a = np.maximum(a, np.clip(1 - np.hypot(xx - cx, (yy - cy) * 0.8) / r, 0, 1))
    core = np.clip(a - 0.5, 0, 1) * 2
    rgb = FIRE_RGB * l13_props.steps(a, 4)[..., None] + np.array([255, 240, 180]) * l13_props.steps(core, 3)[..., None] * 0.5
    out = np.clip(rgb, 0, 255)
    alpha = np.where(out.max(axis=-1) >= 1, 255, 0)
    return Image.fromarray(np.dstack([np.round(out), alpha]).astype(np.uint8), "RGBA")


def smoke_frame(k, n=8):
    """A heavy puff of the destroyed relay's column: dark grey, rising (the game moves it), growing and
    thinning; lumpy, stepped alpha."""
    w, h = SMOKE
    t = (k + 0.5) / n
    rng = np.random.default_rng(3730)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64) + 0.5
    a = np.zeros((h, w))
    for _ in range(7):
        ox, oy, rr = rng.uniform(-4, 4), rng.uniform(-4, 4), rng.uniform(0.6, 1.0)
        r = (5 + 9 * t) * rr
        a = np.maximum(a, np.clip(1 - np.hypot(xx - w / 2 - ox * (0.5 + t), yy - h / 2 - oy * (0.5 + t)) / r, 0, 1) ** 0.7)
    a *= (1.0 - 0.5 * t)
    shade = np.clip(0.5 + 0.5 * ((w / 2 - xx) + (h / 2 - yy)) / 16, 0, 1)
    rgb = np.array([58, 58, 62])[None, None, :] + shade[..., None] * np.array([60, 60, 64]) * (0.5 + 0.5 * t)
    return _stepped(rgb, a)


def _stepped(rgb, a, levels=4):
    s = levels - 1
    q = np.floor(np.clip(a, 0, 1) * s + 0.5) / s
    out = np.dstack([np.clip(rgb, 0, 255), q * 255])
    out[q <= 0] = 0
    return Image.fromarray(out.astype(np.uint8), "RGBA")


def pip():
    """The HUD icon: a dish on its mast, flat white."""
    img = Image.new("RGBA", (48, 48), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.pieslice([4, 2, 40, 38], 200, 380, fill=(255, 255, 255, 255))
    d.line([(22, 20), (22, 44)], fill=(255, 255, 255, 255), width=5)
    d.line([(10, 45), (34, 45)], fill=(255, 255, 255, 255), width=5)
    d.line([(22, 20), (34, 8)], fill=(255, 255, 255, 255), width=3)
    small = img.resize((12, 12), Image.BOX)
    a = np.array(small)
    a[..., 3] = np.where(a[..., 3] > 100, 255, 0)
    a[..., :3] = 255
    a[a[..., 3] == 0] = 0
    return Image.fromarray(a, "RGBA")


def sprite_sets(variant):
    with ProcessPoolExecutor() as pool:
        states = list(pool.map(state_frame, [(variant, s) for s in range(STATES)]))
        lights = list(pool.map(lights_frame, [(variant, i) for i in range(LIGHT_FRAMES)]))
    return {SLUG: artkit.quantize_set(states, COLOURS), f"{SLUG}-lights": artkit.quantize_set(lights, 16),
            f"{SLUG}-fire": artkit.quantize_set([fire_frame(k) for k in range(4)], 12),
            f"{SLUG}-smoke": artkit.quantize_set([smoke_frame(k) for k in range(8)], 12),
            f"{SLUG}-pip": [pip()]}


def write_assets(sets, variant):
    for name, frames in sets.items():
        artkit.write_frames(name, frames, SOURCE, single=name.endswith("-pip"))
        print(f"{name}: {len(frames)} frames, {frames[0].width}x{frames[0].height}")
    PIVOTS.mkdir(parents=True, exist_ok=True)
    pts = POINTS[variant]
    artkit.write_pivots(SLUG, SOURCE, {
        "variant": variant, "smoke": pts["smoke"], "fire": pts["fire"], "column": list(pts["column"]),
        "helipad": list(helipad_px(variant)),
        "note": "px from the frame's top left; per state 0 intact, 1 damaged, 2 critical, 3 destroyed"})


def preview_frame(variant="a"):
    """The intact frame, for other review sheets (l13_props, whiteout_l13)."""
    return state_frame((variant, 0))


# =========================================================================== review

put = l13_props.put
add = l13_props.add


def flash(img):
    """The game's hit flash stand-in: the frame's silhouette in white at 70 %."""
    a = np.array(img)
    out = np.zeros_like(a)
    out[a[..., 3] > 0] = (255, 255, 255, 180)
    return Image.fromarray(out, "RGBA")


def review(variant, sets):
    artkit.REVIEW_ROUND = REVIEW_ROUND
    plate = l13_props.HaltPlate()
    smoke = artkit.load_frames("ship-smoke")
    g0 = plate.ground(0)
    states_on = []
    for s in range(STATES):
        cell = g0.crop((180, 190, 300, 310)).copy()
        put(cell, sets[SLUG][s], 60, 60)
        for x, y in POINTS[variant]["fire"][s]:
            cell = add(cell, sets[f"{SLUG}-fire"][s % 4], 60 - 48 + x, 60 - 48 + y)
        states_on.append(cell)
    lit = []
    for i in range(LIGHT_FRAMES):
        cell = g0.crop((192, 202, 288, 298)).copy()
        put(cell, sets[SLUG][0], 48, 48)
        lit.append(add(cell, sets[f"{SLUG}-lights"][i], 48, 48))
    title = f"NANSEN RELAY {VARIANTS[variant][3].split(' (')[0]}"
    sheet = artkit.review_sheet(title, [
        ("STATES 96X96: INTACT, DAMAGED (BELOW 50 %), CRITICAL (BELOW 25 %), DESTROYED (POWER DOWN)", sets[SLUG], 2,
         False),
        ("ON THE HALTED FRAME'S STAND-IN PLATE (CRITICAL AND DESTROYED WITH THEIR FIRES)", states_on, 2, False),
        ("GRID ONLINE (ADDITIVE): 0-7 THE SEQUENCE AT 4 FPS (PAD, WINDOWS, STATUS LIGHTS, MAST STROBE, SYNC RING), "
         "8-9 THE LOOP", sets[f"{SLUG}-lights"], 1, True),
        ("AS DRAWN: THE LIGHTS OVER THE INTACT RELAY", lit, 2, False),
        ("FIRE 16X16 (ADDITIVE, 8 FPS), DESTROYED SMOKE PUFF 32X32, HUD PIP 12X12",
         sets[f"{SLUG}-fire"] + sets[f"{SLUG}-smoke"][::2] + sets[f"{SLUG}-pip"], 3, False),
    ], width=1300, batch=BATCH)
    sheet_path, gif_path = CONCEPT / f"relay-{REVIEW_ROUND}-{variant}.png", CONCEPT / f"relay-{REVIEW_ROUND}-{variant}.gif"
    CONCEPT.mkdir(parents=True, exist_ok=True)
    sheet.convert("RGB").save(sheet_path, optimize=True)
    from render.enemy_rigs import write_gif
    write_gif(review_loop(variant, sets, plate, smoke), gif_path, fps=20, colors=128)
    print(f"review: {sheet_path.relative_to(artkit.ROOT)}, {gif_path.relative_to(artkit.ROOT)}")


def review_loop(variant, sets, plate, smoke):
    """240x270 of the halted frame round the relay at 2x: hits with flashes (intact), damaged with smoke,
    critical with fires and smoke, destroyed with its column, then (a second take) grid online's
    sequence on the intact relay."""
    fps = 20
    x0, y0 = 120, 115
    cx, cy = 240 - x0, 250 - y0
    pts = POINTS[variant]
    script = [(0.0, 0), (2.0, 1), (4.0, 2), (6.0, 3), (8.5, "lights")]
    rng = np.random.default_rng(3740)
    hits = sorted(rng.uniform(0.2, 6.0, 26))
    puffs = []
    gif = []
    for f in range(int(11.5 * fps)):
        t = f / fps
        mode = [m for t0, m in script if t >= t0][-1]
        cell = plate.ground(f % 80).crop((x0, y0, x0 + 240, y0 + 270))
        state = 0 if mode == "lights" else mode
        frame = sets[SLUG][state]
        put(cell, frame, cx, cy)
        if mode != "lights":
            if any(0 <= t - h < 0.1 for h in hits) and state < 3:
                cell = add(cell, flash(frame), cx, cy)
            src = pts["smoke"][state]
            if state < 3:
                for i, (sx, sy) in enumerate(src):
                    if f % 6 == i % 6:
                        puffs.append([cx - 48 + sx, cy - 48 + sy, t, "ship"])
            elif f % 3 == 0:
                puffs.append([cx - 48 + pts["column"][0] + rng.uniform(-3, 3), cy - 48 + pts["column"][1], t, "column"])
            for x, y in pts["fire"][state]:
                cell = add(cell, sets[f"{SLUG}-fire"][int(t * 8) % 4], cx - 48 + x, cy - 48 + y)
        else:
            k = min(7, int((t - 8.5) * 4)) if t < 10.5 else 8 + int(t * 2) % 2
            cell = add(cell, sets[f"{SLUG}-lights"][k], cx, cy)
        alive = []
        for x, y, t0, kind in puffs:
            age = t - t0
            if mode == "lights":
                continue
            if kind == "ship":
                i = int(age * 4)
                if i < len(smoke):
                    put(cell, smoke[i], x + 10 * age, y - 30 * age)
                    alive.append([x, y, t0, kind])
            else:
                i = int(age * 4)
                if i < 8:
                    put(cell, sets[f"{SLUG}-smoke"][i], x + 16 * age, y - 42 * age)
                    alive.append([x, y, t0, kind])
        puffs = alive
        gif.append(sprite.enlarge(cell, 2))
    return gif


def main(args):
    variant = args[args.index("--variant") + 1] if "--variant" in args else PRODUCTION
    for v in VARIANTS:
        sets = sprite_sets(v)
        if "--assets" in args and v == variant:
            write_assets(sets, v)
        review(v, sets)


if __name__ == "__main__":
    main(sys.argv[1:])
