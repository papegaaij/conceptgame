#!/usr/bin/env python3
"""Production art: Level 11's props on and under the water (design/campaign/act-2-homefront/
level-11-atlantic-convoy, "Ground targets", "Secrets and pickups" and "Layout"; M5 part E batch,
concept round 33, straight to production, user decision E9 = a): the floating cargo containers,
the sunken CDF supply pod snagged on a reef root (no concept: designed to the README), the burning
freighter and the Vrell reef growths of the reef line. The water is drawn as tools/art/water_fx.py
draws it (art direction, Water; E4 = c): what breaks the surface has its waterline cut, its
submerged part pre-rendered through the water and an animated foam collar; what lies on the `sub`
layer (the pod, the reef roots) is drawn plain and gets its tint, blur and wave from the game's
generic `sub` pass.

Outputs (assets/sprites/, the names a ground target's `sprite` gives in Level 11's data):
  floating-container_0..1        32x24 Level 01's cargo container (tools/art/loot_targets.py's
                                 model and damage) afloat, a third of it under the surface: intact,
                                 damaged (as cargo-container_0..1, with the light rim)
  floating-container-collar_0..3 44x36 its foam collar, a 4-frame loop, centred on it
  floating-container-break_0..7  48x48 a kill on the water: the damaged container tips, settles and
                                 sinks through the surface in a foam ring with bubbles and a splash
                                 (no debris, no wreck: the game's break-apart slot)
  sunken-pod_0..2                48x40 the CDF supply pod (olive drum, amber hazard ends, an amber
                                 beacon dome) snagged under a violet-veined Vrell reef root, on `sub`:
                                 0 snagged, 1 hit (the root cracked, the pod shifted, scraped),
                                 2 freed (the root torn, the pod gone)
  sunken-pod-rise_0..5           32x32 the freed pod rising: 0..3 under water (plain, on `sub`,
                                 growing 0.8 -> 0.95 as it nears the surface), 4..5 breaking the
                                 surface (waterline cut and collar baked, on the ground layer); the
                                 game then puts the crate pickup in its place
Backdrop pieces for Level 11 (step E3b: tools/art/backdrop_l11.py imports `pieces()` and writes
them as assets/backdrop/level-11/<id>_<n>.png; this script writes only the review):
  burning-freighter_0..3         72x136 a container ship like the convoy's (convoy_ships.py,
                                 variant a's hull) abandoned and burning: listing 14 degrees, down
                                 by the bow, half its containers gone, scorched, fires on the deck
                                 baked in as a 4-frame flicker with their glow, a collar; the smoke
                                 column is the backdrop's (at half particle speed, motion budget)
  reef-growth-a_0..3             48x40  Vrell reef growths breaking the surface: rust-brown chitin
  reef-growth-b_0..3             64x48  lobes with barnacle knobs and glowing violet veins, the
  reef-growth-c_0..3             40x56  parts below the surface seen through the water, a collar
                                        (4-frame loop)
  reef-root-a                    64x48  a root fading into the deep, on `sub` (plain: the sub pass
                                        tints it); the pod's root is drawn into its sprite
  design/art-direction/concept/l11-props-final-r33-a.png/.gif

Run: python3 tools/art/l11_props.py [--review]   (~1 min)
"""
import sys
from concurrent.futures import ProcessPoolExecutor

import numpy as np
from PIL import Image

import artkit
from artkit import DESIGN, ROOT, TAU, sprite

import convoy_ships as ships  # noqa: E402  (tools/art: the convoy's hull and materials)
import ground_targets as gt  # noqa: E402  (concept script, imported unchanged: materials, light rim)
import loot_targets as lt  # noqa: E402  (tools/art: Level 01's cargo container model and its damage)
import water_fx as wfx  # noqa: E402  (tools/art: waterline rendering, collars, foam, the sea stand-in)
from render import sdf  # noqa: E402
from render.enemy_rigs import write_gif  # noqa: E402
from render.sdf import (Material, rotate_x, rotate_y, rotate_z, sd_box, sd_capsule, sd_ellipsoid,  # noqa: E402
                        sd_sphere, smin, union)

SCRIPT = "l11_props.py"
BATCH = "M5 part E batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
REVIEW_ROUND = "r33"
CONCEPT = DESIGN / "art-direction" / "concept"
COLOURS = 32

CONTAINER = lt.CONTAINER                      # 32x24
CONTAINER_SINK = 2.0                          # px of the 12 px tall box below the surface... and a third
BREAK = (48, 48)
BREAK_FRAMES = 8
POD = (48, 40)
RISE = (32, 32)
RISE_STEPS = 6
FREIGHTER = (72, 136)
FREIGHTER_FRAMES = 4
REEFS = {"reef-growth-a": ((48, 40), 911), "reef-growth-b": ((64, 48), 913), "reef-growth-c": ((40, 56), 917)}
ROOT_SIZE = (64, 48)


# =========================================================================== floating container

def container_scene(damaged, sink=CONTAINER_SINK, pitch=0.0, roll=0.0):
    base = lt.container_scene(CONTAINER, damaged)
    p_, r_ = np.radians(pitch), np.radians(roll)

    def scene(p):
        q = p.copy()
        q[:, 2] += sink
        return base(rotate_y(rotate_x(q, p_), r_))
    return scene


def container_mats(damaged):
    mats = gt.materials(False)
    return lt.scorched(mats, lt.CONTAINER_SCORCH, CONTAINER) if damaged else mats


def container_frame(damaged):
    return wfx.waterline_render(container_scene(damaged), container_mats(damaged), CONTAINER, float(CONTAINER[0]),
                                z_top=24.0, steps=150, depth=6.0, crisp=60)


def container_collar():
    pts = wfx.waterline_points(container_scene(False), CONTAINER, float(CONTAINER[0]), step=2)
    w, h = CONTAINER[0] + 12, CONTAINER[1] + 12
    return artkit.quantize_set(wfx.collar_frames(pts, w, h, w / 2, h / 2, 1201, r=1.0, push=1.0), 12)


def container_break(i):
    """Frame ``i`` of the sinking: the damaged box tips by the hazard end, settles and goes under."""
    u = i / (BREAK_FRAMES - 1)
    scene = container_scene(True, CONTAINER_SINK + 16 * u ** 1.3, pitch=24 * u, roll=10 * u)
    img = wfx.waterline_render(scene, container_mats(True), BREAK, float(BREAK[0]), z_top=24.0, steps=150,
                               depth=6.0, crisp=60)
    pts = wfx.waterline_points(scene, BREAK, float(BREAK[0]), step=2)
    foam = wfx.Foam(*BREAK)
    wfx.sink_foam(foam, wfx.loop_f(i % 4, 4), pts, BREAK[0] / 2, BREAK[1] / 2, u, 1300 + i, (16, 12),
                  np.array(img)[..., 3])
    img.alpha_composite(foam.image(4))
    return img


# =========================================================================== the sunken pod

POD_BODY, POD_HAZARD, POD_STEEL, POD_BEACON, CHITIN, VEIN, SCRAPE = range(7)


def pod_mats(beacon=True):
    def veins(p, n):
        v = np.sin(p[:, 0] * 0.9 + 2.0 * np.sin(p[:, 1] * 0.5)) * np.sin(p[:, 1] * 0.7 - p[:, 0] * 0.3)
        return np.where(np.abs(v) < 0.05, 1.0, 0.0)
    return [Material((0.33, 0.37, 0.27), metal=0.3, shininess=40, spec=0.45, pattern=sdf.panel_lines(0.3, 0.06, 0.8)),
            Material((0.98, 0.66, 0.14), metal=0.0, shininess=8, spec=0.05, pattern=gt.hazard_stripes),
            Material((0.45, 0.47, 0.5), metal=0.6, shininess=50, spec=0.6),
            Material((0.3, 0.15, 0.02), emission=(1.8, 1.0, 0.2)) if beacon else Material((0.4, 0.3, 0.1), spec=0.5),
            Material((0.36, 0.21, 0.17), metal=0.15, shininess=25, spec=0.3),
            Material((0.25, 0.05, 0.3), emission=(0.75, 0.15, 1.1), emission_pattern=veins),
            Material((0.55, 0.55, 0.52), metal=0.7, shininess=60, spec=0.7)]


def pod_shape(p, scraped=False):
    """The CDF supply pod: an olive drum with amber hazard end caps, a beacon dome, two handles."""
    d, m = union((sd_capsule(p, (-9.5, 0, 0), (9.5, 0, 0), 5.2), POD_BODY),
                 (sd_capsule(p, (-11.5, 0, 0), (-8.0, 0, 0), 5.5), POD_HAZARD),
                 (sd_capsule(p, (8.0, 0, 0), (11.5, 0, 0), 5.5), POD_HAZARD),
                 (sd_box(p, (-3.5, 0, 5.4), (0.6, 3.0, 0.5), 0.3), POD_STEEL),
                 (sd_box(p, (3.5, 0, 5.4), (0.6, 3.0, 0.5), 0.3), POD_STEEL),
                 (sd_sphere(p, (0, 0, 5.0), 1.8), POD_BEACON))
    if scraped:
        m = np.where((np.abs(p[:, 1] + 1.5 + 0.4 * p[:, 0]) < 0.9) & (m == POD_BODY), SCRAPE, m)
    return d, m


def root_shape(p, state):
    """The reef root: a thick chitin tendril from the lower left over the pod to the right, a second
    one looping over it; torn open when the pod is freed."""
    segs = [((-24, -16, -6), (-12, -6, -2), 4.0, 3.2), ((-12, -6, -2), (-2, 2, 6.5), 3.2, 2.6),
            ((-2, 2, 6.5), (10, 4, 4.5), 2.6, 2.2), ((10, 4, 4.5), (24, 12, -4), 2.2, 1.6),
            ((14, -16, -6), (6, -6, 3), 3.0, 2.2), ((6, -6, 3), (-6, 6, 6.0), 2.2, 1.8),
            ((-6, 6, 6.0), (-16, 15, -2), 1.8, 1.2)]
    if state == 1:                                             # cracked: the middle of the first loop sags
        segs[1] = ((-12, -6, -2), (-3, 1, 4.5), 3.2, 2.4)
    if state == 2:                                             # torn: the loops over the pod are gone
        segs = [segs[0], ((-12, -6, -2), (-7, -1, 1.0), 3.2, 2.2), ((14, 8, 1.0), (24, 12, -4), 2.0, 1.6),
                segs[4], ((6, -6, 3), (2, -1, 2.0), 2.2, 1.4)]
    d = np.full(len(p), 1e3)
    for a, b, ra, rb in segs:
        d = smin(d, sd_capsule(p, a, b, ra, rb), 1.2)
    knobs = [(-15, -9, 1.0, 1.4), (17, 8, 0.0, 1.2), (9, -10, 0.5, 1.2)]
    for x, y, z, r in knobs:
        d = smin(d, sd_sphere(p, (x, y, z), r), 0.6)
    vein = np.where(pod_mats()[VEIN].emission_pattern(p, None) > 0, d - 0.25, 1e3)
    return union((d, CHITIN), (vein, VEIN))


def pod_scene(state):
    def scene(p):
        q = rotate_z(p, np.radians(-14))
        parts = [root_shape(p, state)]
        if state < 2:
            pq = q if state == 0 else rotate_z(q - np.array([1.5, -1.0, 0.0]), np.radians(-9))
            parts.append(pod_shape(pq, scraped=state == 1))
        return union(*parts)
    return scene


def pod_frame(state):
    hi, factor = artkit.render_hi(pod_scene(state), pod_mats(), POD, float(POD[0]), z_top=24.0, steps=150)
    return artkit.native(hi, factor, crisp=60)


def rise_frame(i):
    """Rise step ``i``: under water the pod alone, plain and growing as it nears the surface;
    then breaking the surface through the waterline render with its collar."""
    u = i / (RISE_STEPS - 1)
    scale = 0.8 + 0.15 * min(1.0, i / 3)
    tilt = np.radians(-14 + 20 * u)
    if i <= 3:
        def scene(p):
            d, m = pod_shape(rotate_z(p / scale, tilt))
            return d * scale, m
        hi, factor = artkit.render_hi(scene, pod_mats(), RISE, float(RISE[0]), z_top=20.0, steps=150)
        return artkit.native(hi, factor, crisp=60)
    sink = 4.5 - 3.5 * (i - 4)                                 # the dome first, then the drum

    def scene(p):
        q = p.copy()
        q[:, 2] += sink
        return pod_shape(rotate_z(q, tilt))
    img = wfx.waterline_render(scene, pod_mats(), RISE, float(RISE[0]), z_top=20.0, steps=150, depth=5.0, crisp=60)
    pts = wfx.waterline_points(scene, RISE, float(RISE[0]), step=2)
    foam = wfx.Foam(*RISE)
    if len(pts):
        wfx.s8.collar(foam, wfx.loop_f(i, 4), pts, RISE[0] / 2, RISE[1] / 2, 1400 + i, alpha=0.95, r=1.1, push=1.2)
    for a in np.linspace(0, TAU, 18, endpoint=False):           # the swell ring as it breaks through
        r = 9 + 3 * (i - 4) + np.sin(3 * a + i)
        foam.dot(RISE[0] / 2 + np.cos(a) * r, RISE[1] / 2 + np.sin(a) * r * 0.8, 1.0, 0.7)
    img.alpha_composite(foam.image(4))
    return img


# =========================================================================== backdrop pieces

def freighter_frame(f):
    """The burning freighter: the convoy's hull (variant a) abandoned, listing and down by the
    bow, more containers gone, heavily scorched; deck fires baked in with their glow (alpha-blended:
    a backdrop piece is not additive), flickering over 4 frames; its collar."""
    base = ships.cargo_a(damaged=True)
    spots = [(-6.0, 6.0, 5.0), (9.0, -15.0, 4.6), (-11.0, 22.0, 3.6), (4.0, -36.0, 4.4), (8.0, 30.0, 3.0)]
    fires = [(-6.0, 6.0), (9.0, -15.0), (4.0, -34.0)]

    def gone(p):                                               # cut away two more bays of containers
        d, m = base(p)
        cut = np.maximum(sd_box(p, (-6.0, 9.0, 14.0), (11.0, 9.5, 7.0), 0.5), 5.6 - p[:, 2])
        return np.maximum(d, -cut), m
    scene = ships.posed(ships.scorched(gone, spots), roll=14.0, pitch=-4.0, sink=1.5)
    img = wfx.waterline_render(scene, ships.materials(lit=False), FREIGHTER, float(FREIGHTER[0]), z_top=40.0,
                               steps=160, depth=12.0)
    rng = np.random.default_rng(1500 + f)
    a = np.array(img).astype(np.float64)
    yy, xx = np.mgrid[0:FREIGHTER[1], 0:FREIGHTER[0]]
    for fx, fy in fires:
        px, py = ships.project(fx, fy, 9.0, FREIGHTER, 14.0)
        flick = rng.uniform(0.75, 1.15)
        d = np.hypot(xx - px - rng.normal(0, 0.6), (yy - py) * 1.15)
        glow = np.exp(-(d / (4.5 * flick)) ** 2)
        core = np.exp(-(d / (1.8 * flick)) ** 2)
        light = glow[..., None] * np.array([255, 110, 30]) * 1.1 + core[..., None] * np.array([255, 230, 150])
        on = a[..., 3] > 0
        a[..., :3] = np.where(on[..., None], np.minimum(255, a[..., :3] + light), a[..., :3])
        halo = (glow > 0.25) & ~on
        a[halo, :3] = np.array([235, 110, 40])
        a[halo, 3] = np.maximum(a[halo, 3], np.floor(glow[halo] * 3) / 3 * 255)
    img = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")
    pts = wfx.waterline_points(scene, FREIGHTER, float(FREIGHTER[0]), step=3)
    foam = wfx.Foam(*FREIGHTER)
    wfx.s8.collar(foam, wfx.loop_f(f, FREIGHTER_FRAMES), pts, FREIGHTER[0] / 2, FREIGHTER[1] / 2, 1600, alpha=0.8,
                  r=1.3, push=1.4)
    img.alpha_composite(foam.image(4))
    return img


REEF_CHITIN, REEF_VEIN, REEF_KNOB, REEF_TEAL = range(4)


def reef_mats():
    def veins(p, n):
        v = np.sin(p[:, 0] * 0.45 + 1.6 * np.sin(p[:, 1] * 0.3)) * np.sin(p[:, 1] * 0.35 + p[:, 2] * 0.4)
        return np.where(np.abs(v) < 0.045, 1.0, 0.0)

    def mottle(p, n):
        return 0.8 + 0.2 * np.sin(p[:, 0] * 1.3 + 2 * np.sin(p[:, 1] * 0.9)) * np.sin(p[:, 1] * 1.1)
    return [Material((0.38, 0.22, 0.17), metal=0.15, shininess=25, spec=0.3, pattern=mottle),
            Material((0.25, 0.05, 0.3), emission=(0.75, 0.15, 1.1), emission_pattern=veins),
            Material((0.52, 0.48, 0.40), metal=0.1, shininess=20, spec=0.25),
            Material((0.02, 0.2, 0.18), emission=(0.0, 0.6, 0.5))]


def reef_scene(seed, size, under=False):
    """A reef growth: a cluster of lumpy chitin lobes, highest in the middle, with barnacle knobs
    and teal polyp tips; ``under`` keeps only the lower roots (the `sub` piece)."""
    rng = np.random.default_rng(seed)
    w, h = size
    lobes = []
    for _ in range(9 if not under else 6):
        x, y = rng.uniform(-w * 0.32, w * 0.32), rng.uniform(-h * 0.32, h * 0.32)
        c = 1 - min(1.0, np.hypot(x / (w * 0.4), y / (h * 0.4)))
        top = (-2.0 + 9 * c * rng.uniform(0.6, 1.0)) if not under else rng.uniform(-14, -6)
        lobes.append(((x, y, top - 5), (rng.uniform(4, 8) * (0.7 + 0.5 * c), rng.uniform(4, 8) * (0.7 + 0.5 * c), 6.0)))
    knobs = [(rng.uniform(-w * 0.3, w * 0.3), rng.uniform(-h * 0.3, h * 0.3), rng.uniform(0.9, 1.6)) for _ in range(6)]
    vein_fn = reef_mats()[REEF_VEIN].emission_pattern

    def scene(p):
        d = np.full(len(p), 1e3)
        for c, r in lobes:
            d = smin(d, sd_ellipsoid(p, c, r), 2.5)
        d = d + 0.35 * np.sin(p[:, 0] * 1.1) * np.sin(p[:, 1] * 1.3) * np.sin(p[:, 2] * 0.9)
        items = [(d * 0.8, REEF_CHITIN), (np.where(vein_fn(p, None) > 0, d * 0.8 - 0.25, 1e3), REEF_VEIN)]
        for x, y, r in knobs:
            items.append((sd_sphere(p, (x, y, 0.5 + 2.0 * r), r), REEF_KNOB if r < 1.35 else REEF_TEAL))
        return union(*items)
    return scene


def reef_frames(name):
    size, seed = REEFS[name]
    scene = reef_scene(seed, size)
    body = wfx.waterline_render(scene, reef_mats(), size, float(size[0]), z_top=24.0, steps=170, depth=10.0, crisp=60)
    pts = wfx.waterline_points(scene, size, float(size[0]), step=2)
    out = []
    for f in range(4):
        foam = wfx.Foam(*size)
        wfx.s8.collar(foam, wfx.loop_f(f, 4), pts, size[0] / 2, size[1] / 2, seed, alpha=0.9, r=1.2, push=1.3)
        img = body.copy()
        img.alpha_composite(foam.image(4))
        out.append(img)
    return out


def reef_root():
    scene = reef_scene(919, ROOT_SIZE, under=True)

    def lifted(p):
        q = p.copy()
        q[:, 2] -= 10
        return scene(q)
    hi, factor = artkit.render_hi(lifted, reef_mats(), ROOT_SIZE, float(ROOT_SIZE[0]), z_top=24.0, steps=170)
    return artkit.native(hi, factor, crisp=60)


def pieces():
    """The backdrop pieces for tools/art/backdrop_l11.py: {id: frames}, each set on one palette."""
    with ProcessPoolExecutor() as pool:
        freighter = list(pool.map(freighter_frame, range(FREIGHTER_FRAMES)))
        reefs = list(pool.map(reef_frames, REEFS))
        root = reef_root()
    out = {"burning-freighter": artkit.quantize_set(freighter, 48)}
    for name, frames in zip(REEFS, reefs):
        out[name] = artkit.quantize_set(frames, COLOURS)
    out["reef-root-a"] = artkit.quantize_set([root], COLOURS)
    return out


# =========================================================================== build

def _job(job):
    kind, arg = job
    if kind == "container":
        return container_frame(arg)
    if kind == "break":
        return container_break(arg)
    if kind == "pod":
        return pod_frame(arg)
    if kind == "rise":
        return rise_frame(arg)
    return container_collar()


def sprite_sets():
    jobs = ([("container", False), ("container", True)] + [("break", i) for i in range(BREAK_FRAMES)]
            + [("pod", s) for s in range(3)] + [("rise", i) for i in range(RISE_STEPS)] + [("collar", None)])
    with ProcessPoolExecutor() as pool:
        res = list(pool.map(_job, jobs))
    k = 2 + BREAK_FRAMES
    containers = lt.finish(res[:2])
    return {"floating-container": containers,
            "floating-container-collar": res[-1],
            "floating-container-break": artkit.quantize_set(res[2:k], COLOURS),
            "sunken-pod": artkit.quantize_set(res[k:k + 3], COLOURS),
            "sunken-pod-rise": artkit.quantize_set(res[k + 3:k + 3 + RISE_STEPS], COLOURS)}


NAMES = ["floating-container", "floating-container-collar", "floating-container-break", "sunken-pod",
         "sunken-pod-rise"]


def build():
    sets = sprite_sets()
    for name, frames in sets.items():
        artkit.write_frames(name, frames, SOURCE)
        print(f"{name}: {len(frames)} frames, {frames[0].width}x{frames[0].height}")
    return sets


# =========================================================================== review

def review(sets, backdrop):
    rows = [
        ("FLOATING CONTAINER 32X24: INTACT, DAMAGED (A THIRD UNDER THE SURFACE)", sets["floating-container"], 5, False),
        ("ITS COLLAR 44X36 (4-FRAME LOOP)", sets["floating-container-collar"], 4, False),
        ("A KILL ON THE WATER: TIPS, SETTLES, SINKS IN A FOAM RING (48X48, NO WRECK)",
         sets["floating-container-break"], 3, False),
        ("SUNKEN SUPPLY POD ON ITS REEF ROOT (SUB, PLAIN): SNAGGED, HIT, FREED", sets["sunken-pod"], 4, False),
        ("THE SAME THROUGH THE SUB PASS STAND-IN", [wfx.sub_look(f, 0.45) for f in sets["sunken-pod"]], 4, False),
        ("THE POD RISING: 4 STEPS UNDER WATER, 2 BREAKING THE SURFACE (32X32)", sets["sunken-pod-rise"], 4, False),
        ("BACKDROP PIECE: THE BURNING FREIGHTER 72X136 (FIRE FLICKER BAKED, 4 FRAMES)", backdrop["burning-freighter"],
         2, False),
        ("BACKDROP PIECES: REEF GROWTHS A 48X40, B 64X48, C 40X56 (COLLAR LOOPS), REEF ROOT 64X48 (SUB)",
         [backdrop[n][0] for n in REEFS] + backdrop["reef-root-a"], 3, False),
    ]
    sheet = artkit.review_sheet("LEVEL 11 PROPS - PRODUCTION SPRITES (ROUND 33)", rows, width=1500, batch=BATCH)
    artkit.save_review(sheet, review_loop(sets, backdrop), CONCEPT, "l11-props", fps=20)


def review_loop(sets, backdrop):
    """240x320 of the ocean stand-in at 2x, scrolling at 140 px/s: the reef line and its root under
    the surface, the snagged pod (through the sub stand-in) taking four torpedo hits and rising to
    the crate, floating containers drifting in their collars, one shot and sinking, the burning
    freighter passing."""
    w, h, n = 240, 320, 160
    crate = artkit.load_frames("pickup-crate")
    blast = artkit.load_frames("explosion-under-small")
    torp = artkit.load_frames("torpedo-pod-shot")
    out = []
    speed = 140 / 20
    scen = [("reef-root-a", 70, -40, True), ("reef-growth-b", 40, 10, False), ("reef-growth-a", 200, -60, False),
            ("burning-freighter", 170, -420, False), ("reef-growth-c", 120, -250, False)]
    containers = [(60, -200), (110, -300), (190, -180)]
    pod_y0 = -20
    hits = [40, 52, 64, 76]
    for i in range(n):
        img = wfx.sea_frame(i, w, h, 120, 100)
        off = i * speed
        for name, x, y, under in scen:
            fr = backdrop[name][(i // 4) % len(backdrop[name])]
            if under:
                fr = wfx.sub_look(fr, 0.5)
            wfx.put(img, fr, x, y + off)
        py = pod_y0 + off
        freed = i >= hits[-1]
        if not freed:
            state = 1 if i >= hits[1] else 0
            wfx.put(img, wfx.sub_look(sets["sunken-pod"][state], 0.45), 120, py)
        else:
            wfx.put(img, wfx.sub_look(sets["sunken-pod"][2], 0.45), 120, py)
            step = (i - hits[-1]) // 4
            if step < RISE_STEPS:
                fr = sets["sunken-pod-rise"][step]
                wfx.put(img, wfx.sub_look(fr, 0.4 * (1 - step / 4)) if step < 4 else fr, 122, py - 2 - 3 * step)
            else:
                wfx.put(img, crate[(i // 3) % len(crate)], 122, py - 20)
        for t in hits:
            age = i - t
            if -8 <= age < 0:
                wfx.put(img, wfx.sub_look(torp[0], 0.3), 120, py + 8 - age * 18)
            fr = wfx.timed(blast, age, 1)
            if fr is not None:
                wfx.put(img, wfx.sub_look(fr, 0.25), 120, py + 4)
        for k, (x, y) in enumerate(containers):
            cy = y + off + 6 * np.sin(i / 20 + k)
            if k == 1 and i >= 120:
                fr = wfx.timed(sets["floating-container-break"], i - 120, 3)
                if fr is not None:
                    wfx.put(img, fr, x, cy)
                continue
            wfx.put(img, sets["floating-container"][0], x, cy)
            wfx.put(img, sets["floating-container-collar"][(i // 3 + k) % 4], x, cy)
        out.append(sprite.enlarge(img, 2))
    return out


if __name__ == "__main__":
    artkit.REVIEW_ROUND = REVIEW_ROUND
    sets = build() if "--review" not in sys.argv[1:] else {n: artkit.load_frames(n) for n in NAMES}
    review(sets, pieces())
