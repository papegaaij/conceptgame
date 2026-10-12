#!/usr/bin/env python3
"""Production art: the Siege Spire, Act 2's boss rooted in Geneva Concord in Level 14
(design/enemies/bosses/siege-spire; M5 part H batch, concept round 36). Straight to production from
its chosen concept (H8 = a, pending the user's review): the round-06 Spire
(tools/concept/bosses_r06.py: the layout, the lean, the part offsets, the lift) on the round-06
models (tools/concept/render/boss_models.spire, root_pod), imported unchanged.

How it is built: one anchored boss without lanes (H1 = a, H4 = a). The rooted body is split so that
every root can die on its own: the **trunk** (the trunk, its spines and ridges, the claw crown with the
maw, and the creep mound out to 0.62 model units) is one sprite in three maw steps; each of the six
**roots** (with its sector of the mound beyond 0.62 units) is a sprite of its own, alive or dead, cut
from the same model by an SDF mask (a 60 degree wedge round the root, below the spines), so the pieces
fit edge to edge; the **pods** at the root tips (four turrets that aim, two acid mortars that lob) are
the concept's pod models. Phase 3 replaces the trunk by the **stump** (the jagged socket it tore out
of; the dead roots stay) and the **free spire** (the airborne body with its torn root stubs), drawn
bigger as it rises (the concept's 1.0 -> 1.2 here) and turning slowly; the spire is six-fold
symmetric, so its headings cover 60 degrees.

Scale and registration: the concept's, 104.8 px per model unit (the rooted body 440 x 440 px, the
roots about 380 px across, the trunk's roof pushed 63 px up the screen by the lean: the tall-structure
rule for a body at the top of the field). Every offset in assets/pivots/siege-spire.json is in px from
the **anchor** (the trunk's foot, the boss's position; x right, y down the screen).

Outputs (assets/sprites/, only with --assets; the review renders them into the concept directory):
  siege-spire_0..2           180x240 trunk, crown shut / half open / open (the maw exposed, phase 2);
                             its centre at the pivots' `trunk` offset from the anchor
  siege-spire-root_0..11     the six roots, indexed root * 2 + (0 alive, 1 dead; the dead one with its
                             mound sector withered); each its own canvas and offset (`roots`), drawn
                             under the trunk; roots 1 and 4 carry the mortars, 0, 2, 3, 5 the turrets
                             (canvases 100..134 px)
  siege-spire-pod_0..31      46x46 root turret pod, 32 headings (heading k = k x 11.25 degrees
                             clockwise from straight down, the muzzle direction)
  siege-spire-mortar_0..2    46x46 root mortar pod: shut, opening, open (the lob)
  siege-spire-husk_0..1      46x46 a dead pod: turret (pointing down), mortar (glow out)
  siege-spire-stump          154x154 the torn socket left at the anchor in phase 3 (a pit with a dimly
                             glowing floor, torn slate flaps round its rim), over the dead roots
  siege-spire-lift_0..5      204..238 px square, the free spire tearing free: scale 1.0 -> 1.17 of the airborne frames'
                             unit, the torn stubs thrashing; played over the lift's 3 s (the game
                             moves it up and slides its shadow out)
  siege-spire-free_0..15     244x244 the airborne spire at 1.2 (its maw open: the lime core shows), 16 headings over its 60 degree symmetry
                             (frame k turned k x 3.75 degrees clockwise; frame 16 = frame 0)
  siege-spire-emitter_0..3   additive crimson glow of one petal emitter, charge 1/4 .. 4/4 (the sweep's
                             0.6 s telegraph); drawn at the six `free_emitters` points of the heading
  siege-spire-maw-glow_0..3  additive lime bloom of the maw (a launch or the fan: 4 frames, 3 steps each)
  siege-spire-debris_0..7    16x16 slate shards (two shapes x 4 turns) the tear-free shockwave throws out
  pivots/siege-spire.json    the anchor-relative offsets above, the pods (position, kind, rest
                             heading), the maw, the free spire's core and emitter points per heading,
                             the lift's scales
  design/enemies/bosses/siege-spire/concept/siege-spire-final-r36-a.png/.gif   the review

How the game draws it (step H6), back to front: the roots (alive or dead), the trunk or, from the lift
on, the stump; the pods over their root tips (a husk once dead); the maw glow on a launch or a fan;
in phase 3 the free spire (lift frames, then the airborne heading frames) over its shadow (the game's
air shadow from the frame), the emitters during a sweep's telegraph. The death chain, the shockwave's
dust ring (the root burst's dust) and the canopy's withering are not here (siege_spire_death.py and
the Level 14 backdrop, step H5).

Run: python3 tools/art/siege_spire.py [--assets | --review]   (~12 min on 20 cores; the renders are
cached in the system temp dir, conceptgame-cache/siege-spire/; without --assets nothing is written
into assets/: the review is rendered from the fresh frames; --review rebuilds it from assets/)
"""
import sys
import tempfile
from concurrent.futures import ProcessPoolExecutor
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

import artkit
from artkit import DESIGN, TAU, sprite

import bosses_r06 as b6  # noqa: E402  (concept script, imported unchanged: layout, helpers, ground)
import enemies_r03 as e3  # noqa: E402
from render import boss_models as bm  # noqa: E402
from render import sdf  # noqa: E402
from render.enemy_rigs import model_rotation, model_space_materials  # noqa: E402

SCRIPT = "siege_spire.py"
BATCH = "M5 part H batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
REVIEW_ROUND = "r36"
UNIT = DESIGN / "enemies" / "bosses" / "siege-spire"
CONCEPT = UNIT / "concept"
SLUG = "siege-spire"
CACHE = Path(tempfile.gettempdir()) / "conceptgame-cache" / "siege-spire"
WORKERS = 12

S = b6.SP_N / b6.SP_EXT                 # 104.76 px per model unit (the concept's)
LEAN = b6.LEAN
FACTOR = 4                              # render factor (quality bar: 4x above 64 px)
BAND = 64                               # native rows per render job
R_SPLIT = 0.62                          # trunk keeps the mound inside this radius (units)
Z_SPLIT = 0.6                           # roots lie below it, the spines and crown above
WEDGE = np.pi / 6                       # half the 60 degree sector of a root
MAWS = (0.0, 0.5, 1.0)
FREE_SCALE = 1.2
LIFT_SCALES = tuple(1.0 + 0.2 * k / 6 for k in range(6))
FREE_HEADINGS = 16
SYM_STEP = TAU / 6 / FREE_HEADINGS      # 3.75 degrees
POD_N, POD_EXT = 46, 1.7                # the concept's pod canvas
POD_HEADINGS = 32
TIP_R = 0.72                            # a petal emitter's radius (crown r1 + 0.04, the maw open)

# Canvases in native px (w, h) and their centres in model units (x right, y up the screen); measured
# from the models (alpha bounds + 3 px).
TRUNK_SIZE, TRUNK_C = (180, 240), (0.0, 0.482)
ROOT_BOX = {                            # root -> ((w, h), (cx, cy))
    0: ((130, 106), (1.055, -0.119)),
    1: ((116, 122), (0.606, 0.768)),
    2: ((102, 130), (-0.315, 0.959)),
    3: ((134, 106), (-1.074, 0.119)),
    4: ((108, 128), (-0.573, -0.797)),
    5: ((100, 134), (0.301, -0.983)),
}
STUMP_SIZE = (154, 154)
FREE_SIZE = 244                         # the free spire's stubs reach 0.95 units


# =========================================================================== models

def _radius(p):
    return np.sqrt(p[:, 0] ** 2 + p[:, 1] ** 2)


def masked(scene, keep):
    """``scene`` cut to the region where ``keep(p)`` <= 0 (an SDF intersection: a lower bound, safe
    for marching)."""
    def cut(p):
        d, m = scene(p)
        return np.maximum(d, keep(p)), m
    return cut


def keep_trunk(p):
    """The trunk's region: inside the split radius, or above the roots (spines, crown)."""
    return np.minimum(_radius(p) - R_SPLIT, Z_SPLIT - p[:, 2])


def keep_root(i, overlap=0.02):
    """Root ``i``'s region: its 60 degree sector outside the split radius and below the spines."""
    a = bm.SPIRE_ROOTS[i]

    def keep(p):
        r = _radius(p)
        da = np.abs((np.arctan2(p[:, 1], p[:, 0]) - a + np.pi) % TAU - np.pi)
        wedge = r * np.sin(np.clip(da - WEDGE, -np.pi / 2, np.pi / 2))
        return np.maximum.reduce([R_SPLIT - overlap - r, p[:, 2] - Z_SPLIT, wedge])
    return keep


def withered_mound(scene):
    """The dead roots' mound sector: the glowing sac tissue outside the trunk turns to dead tissue."""
    def cut(p):
        d, m = scene(p)
        m = np.where((m == bm.V_SAC) & (_radius(p) > R_SPLIT - 0.05), bm.V_DEAD, m)
        return d, m
    return cut


def model(job):
    """(scene, mats) of a render job (rebuilt in each worker; the jobs are plain tuples)."""
    kind = job[0]
    if kind == "trunk":
        scene, mats = bm.spire(maw=job[1], lean=LEAN)
        return masked(scene, keep_trunk), mats
    if kind == "root":
        i, dead = job[1], job[2]
        scene, mats = bm.spire(maw=0.0, lean=LEAN, dead_roots=dead)
        if dead:
            scene = withered_mound(scene)
        return masked(scene, keep_root(i)), mats
    if kind == "stump":
        scene, mats = stump_model()
        return masked(scene, lambda p: _radius(p) - R_SPLIT - 0.1), mats
    if kind in ("free", "lift"):                            # the maw open: the lime core shows
        scene, mats = bm.spire(rooted=False, maw=1.0, laser=0.3, torn=job[3])
        rot = model_rotation(job[2])
        return (lambda p, s=scene, r=rot: s(sdf.rotate_z(p, r))), model_space_materials(mats, rot)
    if kind in ("pod", "mortar", "husk"):
        if kind == "pod":                                   # aimed: heading = screen angle
            scene, mats = bm.root_pod("turret")
            heading = job[1]
        elif kind == "mortar":                              # not turned (as the concept)
            scene, mats = bm.root_pod("mortar", open_=job[1])
            heading = -np.pi / 2
        else:                                               # husk: glow out; a turret points down
            scene, mats = bm.root_pod(job[1], glow=0.0)
            heading = np.pi / 2 if job[1] == "turret" else -np.pi / 2
        rot = model_rotation(heading)
        return (lambda p, s=scene, r=rot: s(sdf.rotate_z(p, r))), model_space_materials(mats, rot)
    if kind == "debris":
        return debris_model(job[1], job[2])
    raise ValueError(kind)


def stump_model():
    """The concept's stump (the withered base and its standing shards) with the socket the spire tore
    out of: a pit with a dimly glowing floor and a ring of torn slate flaps round its rim."""
    from render.sdf import sd_capsule, sd_cylinder_z, union
    base, mats = bm.spire(stump=True, glow=0.6)

    def scene(p):
        d, m = base(p)
        d = np.maximum(d, -sd_cylinder_z(p, (0.0, 0.0, 0.32), 0.3, 0.2))
        flaps = []
        for k in range(9):
            a = k * TAU / 9 + 0.2
            c, s = np.cos(a), np.sin(a)
            c2, s2 = np.cos(a + 0.12), np.sin(a + 0.12)
            flaps.append((sd_capsule(p, (0.36 * c, 0.36 * s, 0.18),
                                     (0.52 * c2, 0.52 * s2, 0.46 + 0.1 * np.sin(5 * a)), 0.09, 0.02), bm.V_BODY))
        d, m = union((d, m), *flaps, k=0.04)
        return union((d, m), (sd_cylinder_z(p, (0.0, 0.0, 0.12), 0.29, 0.03), bm.V_SAC), k=0.0)
    return scene, mats


def debris_model(shape, turn):
    """A slate shard: a flattened chip (0) or a splinter with a dead-root stub (1)."""
    from render.sdf import rotate_z, sd_capsule, sd_ellipsoid, union
    _, mats = bm.spire(stump=True, glow=0.3)

    def scene(p):
        q = rotate_z(p, turn)
        if shape == 0:
            return union((sd_ellipsoid(q, (0, 0, 0.1), (0.55, 0.35, 0.18)), bm.V_DEAD),
                         (sd_ellipsoid(q, (0.2, 0.15, 0.2), (0.25, 0.2, 0.12)), bm.V_DARK), k=0.08)
        return union((sd_capsule(q, (-0.55, -0.1, 0.1), (0.5, 0.15, 0.12), 0.18, 0.08), bm.V_DEAD),
                     (sd_capsule(q, (0.1, 0.05, 0.15), (0.3, 0.45, 0.1), 0.1, 0.04), bm.V_DARK), k=0.06)
    return scene, mats


def geometry(job):
    """(native size (w, h), centre in model units, units per native px, render factor) of a job."""
    kind = job[0]
    if kind == "trunk":
        return TRUNK_SIZE, TRUNK_C, 1 / S, FACTOR
    if kind == "root":
        size, c = ROOT_BOX[job[1]]
        return size, c, 1 / S, FACTOR
    if kind == "stump":
        return STUMP_SIZE, (0.0, 0.0), 1 / S, FACTOR
    if kind == "free":
        return (FREE_SIZE, FREE_SIZE), (0.0, 0.0), 1 / (S * FREE_SCALE), FACTOR
    if kind == "lift":
        n = int(np.ceil(FREE_SIZE * job[1] / FREE_SCALE / 2)) * 2
        return (n, n), (0.0, 0.0), 1 / (S * job[1]), FACTOR
    if kind in ("pod", "mortar", "husk"):
        return (POD_N, POD_N), (0.0, 0.0), POD_EXT / POD_N, artkit.factor_for(POD_N, POD_N)
    if kind == "debris":
        return (16, 16), (0.0, 0.0), 1.6 / 16, artkit.factor_for(16, 16)
    raise ValueError(kind)


def light_extent(job):
    """The canvas width (units) the concept rendered a part at: the key light sits where it did
    there, so pieces cut from one model (trunk, roots, stump) are lit alike and meet without a seam."""
    kind = job[0]
    if kind in ("trunk", "root", "stump"):
        return b6.SP_EXT
    if kind in ("free", "lift"):
        return b6.FREE_EXT
    if kind in ("pod", "mortar", "husk"):
        return POD_EXT
    return 1.6


def _band(task):
    """One horizontal band of a job's high-res render."""
    job, r0, rows = task
    (w, h), (cx, cy), unit, factor = geometry(job)
    extent = unit * w
    scene, mats = model(job)
    by = cy - ((r0 + rows / 2) - h / 2) * unit
    light = sdf.KEY_POS * (light_extent(job) / 2.3)      # the concept canvas's key light, in units
    key = (light - sdf.vec(cx, by, 0.0)) / (extent / 2.3)
    return sdf.render(scene, mats, (w * factor, rows * factor), extent, center=(cx, by),
                      key_pos=tuple(key))


def _key(job):
    return "-".join(f"{v:.4f}" if isinstance(v, float) else str(v) for v in job)


def render_all(jobs):
    """High-res renders of ``jobs`` (cached), each split into bands over a process pool."""
    CACHE.mkdir(parents=True, exist_ok=True)
    out, tasks, owners = {}, [], []
    for job in jobs:
        path = CACHE / f"{_key(job)}.npy"
        if path.exists():
            out[job] = np.load(path)
            continue
        (w, h), *_ = geometry(job)
        for r0 in range(0, h, BAND):
            tasks.append((job, r0, min(BAND, h - r0)))
            owners.append(job)
    if tasks:
        print(f"rendering {len(tasks)} bands of {len({o for o in owners})} frames")
        with ProcessPoolExecutor(max_workers=WORKERS) as pool:
            bands = list(pool.map(_band, tasks, chunksize=1))
        parts = {}
        for job, band in zip(owners, bands):
            parts.setdefault(job, []).append(band)
        for job, pieces in parts.items():
            hi = np.concatenate(pieces, axis=0)
            np.save(CACHE / f"{_key(job)}.npy", hi)
            out[job] = hi
    return out


def finish(his, jobs, colors):
    """High-res renders -> native sprites on one shared palette."""
    frames = []
    for job in jobs:
        *_, factor = geometry(job)
        frames.append(artkit.native(his[job], factor))
    return artkit.quantize_set(frames, colors)


# =========================================================================== frame sets

def job_sets():
    """Every sprite's render jobs, in frame order, with its palette size."""
    return {
        SLUG: ([("trunk", m) for m in MAWS], 48),
        f"{SLUG}-root": ([("root", i, dead) for i in range(6) for dead in (False, True)], 48),
        f"{SLUG}-pod": ([("pod", np.pi / 2 + TAU * k / POD_HEADINGS) for k in range(POD_HEADINGS)], 32),
        f"{SLUG}-mortar": ([("mortar", o) for o in (0.0, 0.5, 1.0)], 32),
        f"{SLUG}-husk": ([("husk", "turret"), ("husk", "mortar")], 24),
        f"{SLUG}-stump": ([("stump",)], 32),
        f"{SLUG}-lift": ([("lift", sc, np.pi / 2, k * np.pi / 2) for k, sc in enumerate(LIFT_SCALES)], 48),
        f"{SLUG}-free": ([("free", FREE_SCALE, np.pi / 2 + k * SYM_STEP, 0.0) for k in range(FREE_HEADINGS)], 48),
        f"{SLUG}-debris": ([("debris", sh, k * np.pi / 4) for sh in (0, 1) for k in range(4)], 16),
    }


def glow_disc(size, rgb, strength, core=0.35, levels=5):
    """Additive radial glow: a hot core and a stepped halo (premultiplied on black)."""
    yy, xx = np.mgrid[0:size, 0:size]
    r = np.sqrt((xx + 0.5 - size / 2) ** 2 + (yy + 0.5 - size / 2) ** 2) / (size / 2)
    a = np.clip(1 - r, 0, 1) ** 1.6 * strength
    a = artkit.ordered_dither(a, levels)
    hot = np.clip((core - r) / core, 0, 1)
    col = np.array(rgb, dtype=np.float64)[None, None, :] * (1 - hot[..., None]) + 255 * hot[..., None]
    img = np.dstack([col, a * 255]).clip(0, 255).astype(np.uint8)
    return artkit.additive(Image.fromarray(img, "RGBA"))


def rgb255(name):
    return tuple(int(255 * c) for c in bm.em.hx(bm.em.GLOWS[name]))


def emitter_frames():
    return [glow_disc(20, rgb255("crimson"), 0.25 * (k + 1), core=0.12 + 0.06 * k) for k in range(4)]


def maw_glow_frames():
    return [glow_disc(72, rgb255("lime"), s, core=0.1 + 0.2 * s) for s in (0.55, 1.0, 0.7, 0.35)]


def make_frames():
    sets = job_sets()
    his = render_all([j for jobs, _ in sets.values() for j in jobs])
    frames = {name: finish(his, jobs, colors) for name, (jobs, colors) in sets.items()}
    frames[f"{SLUG}-emitter"] = emitter_frames()
    frames[f"{SLUG}-maw-glow"] = maw_glow_frames()
    return frames


# =========================================================================== registration

def px(x, y, scale=1.0):
    """Model units (y up) -> px offset from the anchor (y down)."""
    return [round(float(x) * S * scale, 1), round(-float(y) * S * scale, 1)]


def emitter_points(k):
    """The six petal emitters of free-spire frame k, px from the spire's centre."""
    rot = model_rotation(np.pi / 2 + k * SYM_STEP)
    out = []
    for j in range(6):
        a = 0.3 + j * TAU / 6 + rot
        out.append(px(TIP_R * np.cos(a), TIP_R * np.sin(a), FREE_SCALE))
    return out


def pivots():
    crown = (LEAN[0] * (bm.SPIRE_HEIGHT + 0.1), LEAN[1] * (bm.SPIRE_HEIGHT + 0.1))   # the maw's eye
    roots = []
    for i in range(6):
        (w, h), c = ROOT_BOX[i]
        pod = bm.SPIRE_PODS[i]
        roots.append({"offset": px(*c), "size": [w, h], "pod": px(*pod),
                      "kind": "mortar" if i in bm.SPIRE_MORTARS else "turret",
                      "angle_deg": round(float(np.degrees(-bm.SPIRE_ROOTS[i])) % 360, 1)})
    return {
        "unit_px": round(S, 3),
        "anchor": "the trunk's foot (the boss's position); offsets are px from it, x right, y down",
        "trunk": {"offset": px(*TRUNK_C), "size": list(TRUNK_SIZE), "frames": ["shut", "half", "open"]},
        "maw": px(*crown),
        "roots": roots,
        "pod_headings": POD_HEADINGS,
        "pod_heading_0": "down the screen, clockwise",
        "stump": {"offset": [0.0, 0.0], "size": list(STUMP_SIZE)},
        "lift": {"scales": [round(sc / FREE_SCALE, 4) for sc in LIFT_SCALES], "seconds": 3.0},
        "free": {"size": FREE_SIZE, "unit_px": round(S * FREE_SCALE, 3), "symmetry": 6,
                 "headings": FREE_HEADINGS, "heading_step_deg": round(float(np.degrees(SYM_STEP)), 3),
                 "core": [0.0, 0.0], "emitters": [emitter_points(k) for k in range(FREE_HEADINGS)]},
    }


# =========================================================================== review

FIELD = (480, 540)
ANCHOR = b6.SP_C                        # the concept's anchor in the 480 x 540 field
STATION = (ANCHOR[0], ANCHOR[1] - 76)   # the free spire's height after the lift


def pod_index(x, y, tx, ty):
    a = np.arctan2(ty - y, tx - x) - np.pi / 2
    return int(round(a / (TAU / POD_HEADINGS))) % POD_HEADINGS


class Rig:
    """Draws the boss from the production frames the way the game will (review only)."""

    def __init__(self, f):
        self.f = f

    def at(self, off, ax=ANCHOR[0], ay=ANCHOR[1]):
        return ax + off[0], ay + off[1]

    def rooted(self, img, t_maw, dead, aim, mortar_open, stump=False, glow=None):
        f, pv = self.f, PIVOTS
        for i in range(6):
            sprite.paste_center(img, f[f"{SLUG}-root"][i * 2 + int(i in dead)], *self.at(pv["roots"][i]["offset"]))
        if stump:
            sprite.paste_center(img, f[f"{SLUG}-stump"][0], *ANCHOR)
        else:
            e3.place(img, f[SLUG][t_maw], *self.at(pv["trunk"]["offset"]), "ground")
        for i in range(6):
            x, y = self.at(pv["roots"][i]["pod"])
            mortar = i in bm.SPIRE_MORTARS
            if i in dead:
                sp = f[f"{SLUG}-husk"][int(mortar)]
            elif mortar:
                sp = f[f"{SLUG}-mortar"][mortar_open.get(i, 0)]
            else:
                sp = f[f"{SLUG}-pod"][pod_index(x, y, *aim)]
            e3.place(img, sp, x, y, "ground")
        if glow is not None and not stump:
            g = f[f"{SLUG}-maw-glow"][glow]
            mx, my = self.at(pv["maw"])
            add_at(img, g, mx, my)

    def free(self, img, x, y, k, lift=None, height=1.0, charge=0):
        f = self.f
        sp = f[f"{SLUG}-lift"][lift] if lift is not None else f[f"{SLUG}-free"][k % FREE_HEADINGS]
        sh = sprite.shadow_of(sp, opacity=0.55 - 0.15 * height, blur=1.2 + 1.5 * height, scale=0.9)
        sprite.paste_center(img, sh, x + 6 + 34 * height, y + 8 + 48 * height)
        sprite.paste_center(img, sp, x, y)
        if charge and lift is None:
            for ex, ey in PIVOTS["free"]["emitters"][k % FREE_HEADINGS]:
                add_at(img, f[f"{SLUG}-emitter"][charge - 1], x + ex, y + ey)


def add_at(img, glow, x, y):
    out = artkit.add_light(img, glow, (int(round(x - glow.width / 2)), int(round(y - glow.height / 2))))
    img.paste(out)


PIVOTS = pivots()


def fight_gif(frames):
    """The fight in the 480 x 540 field, about 14 s: the roots firing and dying, the maw opening and
    launching cloaked Wraiths, the lift with the shockwave, the airborne drift with the sweep."""
    rig = Rig(frames)
    bg = b6.creep_ground()
    shots, booms = b6.Shots(), b6.Booms()
    pv = PIVOTS
    pod_xy = [rig.at(r["pod"]) for r in pv["roots"]]
    maw_xy = rig.at(pv["maw"])
    order = (0, 2, 5, 1, 3, 4)
    die = {i: 2.4 + 0.42 * n for n, i in enumerate(order)}
    maw_open, maw_die, lift0, lift1 = 4.8, 7.4, 7.6, 10.6
    sweep = (11.6, 12.2, 13.4)
    launches = (5.2, 6.6)
    fans = (5.8, 6.9)
    rings = (10.9, 13.7)

    def player(t):
        return 240 + 110 * np.sin(t * 0.7), 492

    lobs = []
    for i, t0 in ((1, 0.6), (4, 1.6), (1, 2.6)):
        if t0 < die[i]:
            lobs.append((i, t0, *player(t0)))
    for i in range(6):
        if i in bm.SPIRE_MORTARS:
            continue
        for ta in np.arange(0.3 + 0.4 * (i % 4), die[i], 1.6):
            shots.aimed(ta, *pod_xy[i], *player(ta), 150)
    for i in range(6):
        booms.add(die[i], *pod_xy[i], 60, seed=40 + i)
    for t0 in fans:
        for a in np.radians(np.linspace(50, 130, 9)):
            shots.fire(t0, *maw_xy, a, 140)
    booms.add(maw_die, *maw_xy, 90, seed=60)
    booms.add(lift0, *ANCHOR, 130, seed=61, ring=True)

    def free_state(t):
        if t < lift1:
            s = b6.ease((t - lift0) / (lift1 - lift0))
            return ANCHOR[0], ANCHOR[1] + (STATION[1] - ANCHOR[1]) * s, s
        u = t - lift1
        return ANCHOR[0] + 100 * np.sin(u * 0.5), STATION[1], 1.0

    rng = np.random.default_rng(7)
    shards = [(rng.uniform(0, TAU), rng.uniform(90, 210), int(rng.integers(0, 8))) for _ in range(18)]
    wraith = artkit.load_frames("wraith-cloak")
    out = []
    for t in np.arange(0, 14.6, 0.1):
        img = bg.copy()
        tx, ty = player(t)
        dead = {i for i in range(6) if t >= die[i]}
        if t < lift0:
            m = 0 if t < maw_open else (1 if t < maw_open + 0.2 else 2)
            if t >= maw_die:
                m = 2
            glow = None
            for t0 in launches + fans:
                if t0 <= t < t0 + 0.4 and t < maw_die:
                    glow = min(3, int((t - t0) / 0.1))
            opened = {}
            for i, t0, *_ in lobs:
                if t0 - 0.3 <= t < t0:
                    opened[i] = 1 if t < t0 - 0.15 else 2
                elif t0 <= t < t0 + 0.15:
                    opened[i] = 2
            rig.rooted(img, m, dead, (tx, ty), opened, glow=glow)
        else:
            rig.rooted(img, 0, dead, (tx, ty), {}, stump=True)
            age = t - lift0
            if age < 1.6:                                    # the shockwave's shards
                for a, sp_, k in shards:
                    r = 40 + sp_ * (1 - np.exp(-age * 2.2)) * 0.9
                    sx, sy = ANCHOR[0] + np.cos(a) * r, ANCHOR[1] + np.sin(a) * r * 0.85
                    e3.place(img, frames[f"{SLUG}-debris"][k], sx, sy, "ground")
        # lobs: lime marker on the ship's position at the throw, the arc, the ring of 10
        d = ImageDraw.Draw(img)
        for i, t0, mx, my in lobs:
            sx, sy = pod_xy[i]
            if t0 <= t < t0 + 1.0:
                s = (t - t0) / 1.0
                d.ellipse([mx - 16, my - 10, mx + 16, my + 10], outline=b6.ACID + (255,), width=2)
                bx, by = sx + (mx - sx) * s, sy + (my - sy) * s - np.sin(np.pi * s) * 120
                e3.orb(img, bx, by, int(4 + 4 * np.sin(np.pi * s)), ring=b6.ACID)
            elif t0 + 1.0 <= t < t0 + 1.6:
                s = (t - t0 - 1.0) * 140
                for a in np.linspace(0, TAU, 10, endpoint=False):
                    e3.orb(img, mx + np.cos(a) * s, my + np.sin(a) * s, 4, ring=b6.ACID)
        # the maw's Wraiths, cloaked (the additive shimmer), swooping down the sides
        if wraith:
            for t0 in launches:
                for side in (-1, 1):
                    u = t - t0
                    if 0 <= u < 1.4:
                        x = maw_xy[0] + side * (40 + 170 * u)
                        y = maw_xy[1] + 40 + 260 * u * u
                        h = 2 if side < 0 else 14                    # heading toward its side, down
                        g = wraith[h * 4 + int(t * 10) % 4]
                        add_at(img, g, x, y)
        if t >= lift0:
            x, y, s = free_state(t)
            if t < lift1:
                k = min(5, int((t - lift0) / (lift1 - lift0) * 6))
                rig.free(img, x, y, 0, lift=k, height=s)
            else:
                k = int(round((t - lift1) * 0.2 / SYM_STEP))
                charge = 0
                if sweep[0] <= t < sweep[1]:
                    charge = 1 + min(3, int((t - sweep[0]) / (sweep[1] - sweep[0]) * 4))
                elif sweep[1] <= t < sweep[2]:
                    charge = 4
                rig.free(img, x, y, k, charge=charge)
                if sweep[0] <= t < sweep[1] and int(t * 10) % 2 == 0:
                    e3.dashed(img, x, y, x + np.cos(2.6) * 600, y + np.sin(2.6) * 600, (255, 60, 40), dash=7)
                elif sweep[1] <= t < sweep[2]:
                    s2 = (t - sweep[1]) / (sweep[2] - sweep[1])
                    a = 2.6 - np.radians(120) * b6.ease(s2)
                    e3.beam(img, x, y, x + np.cos(a) * 640, y + np.sin(a) * 640)
                for t0 in rings:
                    if t0 <= t < t0 + 3:
                        r = (t - t0) * 120
                        for a in np.linspace(0, TAU, 14, endpoint=False):
                            e3.orb(img, x + np.cos(a) * r, y + np.sin(a) * r, 4)
        booms.draw(img, t)
        shots.draw(img, t)
        b6.player_draw(img, tx, ty, t, "ground")
        out.append(img.convert("RGB").convert("RGBA"))
    return out


def assembled(frames):
    """Three stills of the boss at 1x on the concept's ground: the rooted body (phase 1), the roots
    dead and the maw open (phase 2), the stump with the free spire charging its emitters (phase 3)."""
    rig = Rig(frames)
    bg = b6.creep_ground()
    stills = []
    a = bg.copy()
    rig.rooted(a, 0, set(), (240, 520), {1: 2})
    b = bg.copy()
    rig.rooted(b, 2, set(range(6)), (240, 520), {}, glow=1)
    c = bg.copy()
    rig.rooted(c, 0, set(range(6)), (240, 520), {}, stump=True)
    rig.free(c, 112, 100, 5, charge=4)
    for img in (a, b, c):
        stills.append(img.crop((0, 0, 480, 440)))
    return stills


def review(frames):
    artkit.REVIEW_ROUND = REVIEW_ROUND
    f = frames
    emit_check = []
    for k in (0, 4, 8, 12):
        base = artkit.checker(FREE_SIZE, FREE_SIZE, 4)
        base.alpha_composite(f[f"{SLUG}-free"][k])
        for ex, ey in PIVOTS["free"]["emitters"][k]:
            add_at(base, f[f"{SLUG}-emitter"][3], FREE_SIZE / 2 + ex, FREE_SIZE / 2 + ey)
        emit_check.append(base)
    rows = [
        ("ASSEMBLED: PHASE 1 ROOTED / PHASE 2 ROOTS DEAD, MAW OPEN / PHASE 3 STUMP, FREE SPIRE CHARGING", assembled(f), 1, False),
        ("TRUNK: SHUT, HALF, OPEN (MAW)", f[SLUG], 1, False),
        ("ROOTS 0-5, ALIVE / DEAD", f[f"{SLUG}-root"], 1, False),
        ("TURRET POD HEADINGS 0-15", f[f"{SLUG}-pod"][:16], 2, False),
        ("TURRET POD HEADINGS 16-31", f[f"{SLUG}-pod"][16:], 2, False),
        ("MORTAR SHUT/OPENING/OPEN, HUSKS", f[f"{SLUG}-mortar"] + f[f"{SLUG}-husk"], 2, False),
        ("STUMP", f[f"{SLUG}-stump"], 1, False),
        ("LIFT 0-5 (1.0 -> 1.17)", f[f"{SLUG}-lift"], 1, False),
        ("FREE SPIRE HEADINGS 0-4 (3.75 DEG STEPS, 6-FOLD)", f[f"{SLUG}-free"][:5], 1, False),
        ("FREE SPIRE HEADINGS 5-9", f[f"{SLUG}-free"][5:10], 1, False),
        ("FREE SPIRE HEADINGS 10-15", f[f"{SLUG}-free"][10:], 1, False),
        ("EMITTER PIVOT CHECK: FRAMES 0, 4, 8, 12 WITH THE CHARGED EMITTERS", emit_check, 1, False),
        ("EMITTER CHARGE 1-4, MAW GLOW", f[f"{SLUG}-emitter"] + f[f"{SLUG}-maw-glow"], 2, True),
        ("SHOCKWAVE DEBRIS", f[f"{SLUG}-debris"], 4, False),
    ]
    sheet = artkit.review_sheet("SIEGE SPIRE - ACT 2 BOSS (PRODUCTION, NOT YET IN ASSETS)", rows, width=1500, batch=BATCH)
    artkit.save_review(sheet, fight_gif(f), CONCEPT, SLUG, fps=10)


# =========================================================================== atlas estimate

def atlas_estimate(frames, padding=2):
    """Area of every frame (canvas + the packer's padding; whitespace is not stripped) against one
    2048 x 2048 page, and a shelf pack (tallest first) as a check."""
    rects = [(im.width + padding, im.height + padding) for fs in frames.values() for im in fs]
    area = sum(w * h for w, h in rects)
    x = y = shelf = 0
    for w, h in sorted(rects, key=lambda r: -r[1]):
        if x + w > 2048:
            x, y, shelf = 0, y + shelf, 0
        x, shelf = x + w, max(shelf, h)
    used = y + shelf
    print(f"atlas: {len(rects)} regions, {area / 2048 ** 2:.0%} of a 2048x2048 page; "
          f"shelf pack height {used} px of 2048")
    for name, fs in frames.items():
        a = sum((im.width + padding) * (im.height + padding) for im in fs)
        print(f"  {name:22s} {len(fs):3d} x {fs[0].width}x{fs[0].height}  {a / 2048 ** 2:6.1%}")


# =========================================================================== main

def write_assets(frames):
    for name, fs in frames.items():
        artkit.write_frames(name, fs, SOURCE, single=(len(fs) == 1))
    artkit.write_pivots(SLUG, SOURCE, PIVOTS)


def load_assets():
    frames = {name: artkit.load_frames(name) for name in list(job_sets()) + [f"{SLUG}-emitter", f"{SLUG}-maw-glow"]}
    missing = [n for n, fs in frames.items() if not fs]
    if missing:
        sys.exit(f"--review needs the assets (run with --assets first); missing: {', '.join(missing)}")
    return frames


def main(args):
    if "--review" in args:
        frames = load_assets()
    else:
        frames = make_frames()
        if "--assets" in args:
            write_assets(frames)
    atlas_estimate(frames)
    review(frames)


if __name__ == "__main__":
    main(sys.argv[1:])
