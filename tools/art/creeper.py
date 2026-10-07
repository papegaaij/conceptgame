#!/usr/bin/env python3
"""Production art: the Creeper, Act 2's six-legged Vrell salamander walker
(design/enemies/ground/creeper; M5 part B batch, concept round 30). Straight to production from its
chosen concept (user decision D6 = a): the round-06 model creeper-r06-a.

Outputs (assets/sprites/; sizes and the stride from the Creeper's data.yaml):
  creeper_0..95             60x60, `16 angles`, ground layer: 16 headings x 6 walk frames, indexed
                            heading * 6 + frame (EnemyLooks.walkFrame); heading k walks k x 22.5
                            degrees clockwise from straight down (heading 0 walks down the screen,
                            heading 4 to the left); one walk cycle = the data's stride (22 px)
  creeper-husk_0..15        60x60: the legless, scorched body at each heading (its remains, left at
                            its last heading), the hip stubs and the dead gland
  creeper-glow_0..95        60x60, additive: the fan gland's five pores, the three pores along the
                            back and the eyes alone, in the walk frames' order, drawn above the
                            low-air layer (L08's fog banks)
  creeper-death_0..15       96x96, additive: the gland bursting violet, a violet-white flash, violet
                            ichor globules with short trails and a dark plum mist that billows low
                            and thins (4 steps per frame, with the `medium` burst)
  creeper-tatters_0..15     96x96, solid: the six legs torn off at the hips with their toe pads, the
                            fan frill split into four shards and chitin chips with bone studs off
                            the back, flung out, tumbling, settling and shrivelling (4 steps per
                            frame); the husk stays where it died
  design/enemies/ground/creeper/concept/creeper-final-r30-a.png/.gif

The model is the chosen round-06 Creeper (tools/concept/render/r06_models.creeper, built facing +Y),
its geometry copied here so the legs' swing can follow the data's stride: the concept's +-0.18
model-unit foot swing carried the body ~19 px per cycle; at the 22 px stride the planted feet stay
put with a swing of +-0.21 (a tripod gait: each foot pushes back for half the cycle). Body and tail
sway in the concept's S-curve with the gait. Like round 06, the material patterns turn with the
body (enemy_rigs.model_space_materials), and every heading is its own render under the fixed
top-left key light; nothing lit is rotated or mirrored as an image. The death glow is a 2D light
field (vfx_r08's canvas) like the Scuttler's (tools/art/vrell_deaths.py); the pieces are ray-marched
per frame from the Creeper's own geometry and materials, each pattern evaluated in the piece's frame.
One palette per set. For the night city (round 30's Level 08 capture) the slate chitin is lifted,
the legs and frill sit between dark chitin and slate, three violet glow pores run along the back
and every walk and husk frame gets a 1 px lavender-white light rim, stronger where the silhouette
faces the top-left key light (light_rim).

Run: python3 tools/art/creeper.py [--review]   (~2 min; --review only rebuilds the review files)
"""
import sys
from concurrent.futures import ProcessPoolExecutor
from dataclasses import replace

import numpy as np
import yaml
from PIL import Image, ImageDraw, ImageFilter

import artkit
from artkit import DESIGN, TAU, sprite

import vfx_r08 as v8  # noqa: E402  (concept script, imported unchanged)
import vrell_deaths as vd  # noqa: E402  (tools/art: the death pieces' pose and the motes)
import vrell_fx as fx  # noqa: E402  (tools/art: easing, torn outlines, timing)
import vrell_l03 as l03  # noqa: E402  (tools/art: patterns in a part's own frame)
from render import enemy_models as em  # noqa: E402
from render import r06_models as m6  # noqa: E402,F401  (adds the Creeper's role colours)
from render.enemy_models import V_BODY, V_BONE, V_DARK, V_EYE, V_GLOW  # noqa: E402

V_STUD = 7                                      # the back pores: bone glowing violet (a slot of this script's)
from render.enemy_rigs import model_space_materials  # noqa: E402
from render.sdf import Material, rotate_z, sd_capsule, sd_ellipsoid, sd_plate, sd_sphere, union  # noqa: E402

# =========================================================================== parameters

SCRIPT = "creeper.py"
BATCH = "M5 part B batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
REVIEW_ROUND = "r30"
CONCEPT = DESIGN / "enemies" / "ground" / "creeper" / "concept"
DATA = yaml.safe_load((DESIGN / "enemies" / "ground" / "creeper" / "data.yaml").read_text(encoding="utf-8"))
STEP = 60                                       # game steps per second

SIZE = tuple(DATA["size"])                      # 60x60
EXTENT = 2.3                                    # model units across the sprite (the concept's)
PX = SIZE[0] / EXTENT                           # px per model unit (~26)
HEADINGS = 16
WALK_FRAMES = 6
STRIDE_PX = DATA["movement"]["walk"]["stride"]  # 22 px of ground per walk cycle
SPEED = DATA["movement"]["walk"]["speed"]       # 35 px/s (review loop)
# tripod gait: a planted foot travels STRIDE / 2 per stance, which is twice the swing amplitude
SWING = STRIDE_PX / PX / 4                      # the foot's fore-aft swing, model units (concept 0.18)
SWAY = 0.1                                      # the spine's S-curve (the concept's)
COLOURS = 32
HUSK_GLOW = 0.1                                 # the dead gland's glow
HUSK_TONE = 0.8                                 # scorched: albedo scale (of the lifted body)
# Readability on Level 08's night city (round 30's capture: dark grey-brown on dark navy): the slate
# lifted, the legs no longer near-black, three violet glow pores along the back, a 1 px light rim
BODY_LIFT = 1.4                                 # the slate chitin's albedo scale
LEG_LIFT = 0.4                                  # the legs and frill mixed this far from dark chitin toward the slate
STUD_GLOW = 1.0                                 # the back pores' violet emission (in the glow masks too)
BACK_PORE = 0.05                                # model units: the back pores' radius (~2.6 px across)
RIM = np.array([222, 210, 248], float)          # the light rim: lavender white (cool, not the loot targets' warm white)
RIM_LIT, RIM_SHADE = 0.8, 0.45                  # rim strength facing the top-left key light, facing away
HUSK_RIM = 0.55                                 # the husk's rim, scaled
HUSK_COLOURS = 24
GLOW_GAIN = 1.0                                 # the pores' and eyes' emission in the glow masks
GLOW_COLOURS = 16

DEATH_SIZE = (96, 96)                           # the Scuttler's effect canvas (medium tier)
DEATH_FRAMES = 16
DEATH_STEPS = 4                                 # EnemyLooks.DEATH_FRAME_TICKS
DEATH_COLOURS = 32
DEATH_EXTENT = EXTENT * DEATH_SIZE[0] / SIZE[0]  # the unit's scale on the effect canvas
GLAND = 0.45                                    # the burst's core radius, model units (~12 px)
VIOLET = np.array(em.hx(em.GLOWS["violet"])) * 255

N_SPINE = 9
RADII = (0.13, 0.24, 0.28, 0.27, 0.22, 0.15, 0.1, 0.065, 0.035)
LEGS = ((1, 0.16), (3, 0.0), (5, -0.16))        # (spine node, fore-aft reach): front, middle, hind
FRILL = [(0.0, 0.84), (0.28, 0.9), (0.42, 0.72), (0.26, 0.6), (0.0, 0.62)]   # one half, mirrored
FRILL_SHARDS = [[(0.12, 0.86), (0.28, 0.9), (0.42, 0.72), (0.3, 0.66)],           # torn along a crack
                [(0.0, 0.84), (0.12, 0.86), (0.3, 0.66), (0.26, 0.6), (0.0, 0.62)]]


# =========================================================================== model

def spine_of(phase):
    """The spine's nodes (x, y) swaying in an S-curve with the gait, head first."""
    return [(SWAY * np.sin(phase - 0.8 * i) * (0.3 + i / (N_SPINE - 1)), 0.5 - 0.2 * i) for i in range(N_SPINE)]


def leg_points(spine, j, side, phase):
    """Hip, knee and foot of leg pair j on ``side`` (+1 right, -1 left) at the gait's phase."""
    idx, reach = LEGS[j]
    bx, by = spine[idx]
    ph = phase + (0 if (j + (side > 0)) % 2 else np.pi)      # diagonal (tripod) gait
    swing = SWING * np.sin(ph)
    lift = 0.08 * max(0.0, np.cos(ph))
    hip = (bx + side * 0.2, by, 0.08)
    knee = (bx + side * 0.5, by + reach * 0.5 + swing * 0.4, 0.2 + lift)
    foot = (bx + side * 0.66, by + reach + swing, 0.0 + lift)
    return hip, knee, foot


def toes(foot, side, reach):
    return [(foot[0] + side * 0.07 + dx * 0.5, foot[1] + dx + reach * 0.3, foot[2]) for dx in (-0.06, 0.0, 0.06)]


def _shift_x(p, dx):
    """Mirror about the vertical line x = dx (the parts that sway with the head), as r06_models."""
    r = p.copy()
    r[:, 0] = np.abs(r[:, 0] - dx)
    return r


def creeper_scene(phase, legs=True):
    """render/r06_models.creeper with the stride's leg swing (forward +Y); ``legs=False`` leaves
    only the hip stubs (the husk)."""
    spine = spine_of(phase)

    def scene(p):
        hx0 = spine[0][0]
        parts = [(sd_ellipsoid(p, (hx0, 0.8, 0.09), (0.31, 0.27, 0.12)), V_BODY),   # flat head
                 (sd_ellipsoid(p, (hx0, 1.0, 0.07), (0.2, 0.14, 0.08)), V_BODY)]    # snout
        for i in range(N_SPINE - 1):
            (x0, y0), (x1, y1) = spine[i], spine[i + 1]
            parts.append((sd_capsule(p, (x0, y0, 0.1), (x1, y1, 0.08), RADII[i], RADII[i + 1]), V_BODY))
        d, m = union(*parts, k=0.07)
        pores = [(sd_sphere(p, (hx0 + 0.11 * k, 0.74 - 0.025 * abs(k), 0.2), 0.045), V_GLOW)
                 for k in (-2, -1, 0, 1, 2)]
        d, m = union((d, m),
                     (sd_plate(_shift_x(p, hx0), FRILL, 0.15, 0.022, 0.01), V_DARK),
                     (sd_sphere(_shift_x(p, hx0), (0.2, 0.92, 0.15), 0.06), V_EYE),
                     *pores, k=0.02)
        studs = [(sd_sphere(_shift_x(p, spine[i][0]), (0.09, spine[i][1] - 0.08, 0.3 - 0.03 * i),
                            0.045 - 0.004 * i), V_BONE) for i in range(1, 6)]
        back = [(sd_sphere(p, (spine[i][0], spine[i][1], 0.08 + RADII[i]), BACK_PORE), V_STUD)
                for i in (2, 3, 4)]                                 # glow pores along the back's crest
        d, m = union((d, m), *studs, *back, k=0.02)
        items = []
        for j in range(3):
            for side in (1, -1):
                hip, knee, foot = leg_points(spine, j, side, phase)
                if not legs:
                    stub = tuple(a + 0.3 * (b - a) for a, b in zip(hip, knee))
                    items.append((sd_capsule(p, hip, stub, 0.085, 0.075), V_DARK))
                    continue
                items += [(sd_capsule(p, hip, knee, 0.085, 0.06), V_DARK),
                          (sd_capsule(p, knee, foot, 0.055, 0.04), V_DARK),
                          (sd_ellipsoid(p, foot, (0.09, 0.09, 0.035)), V_BONE)]
                items += [(sd_capsule(p, foot, tip, 0.025, 0.015), V_BONE) for tip in toes(foot, side, LEGS[j][1])]
        return union((d, m), *items, k=0.03)
    return scene


def lifted_mats(glow=1.0):
    """The round-04 slate scheme lifted for the night city: lighter chitin, legs and frill between
    dark chitin and slate, and the back pores (slot V_STUD) glowing violet."""
    mats = em.vrell_scheme_mats("creeper", "a", glow)
    mid = np.array(mats[V_BODY].albedo)
    for slot in (V_BODY, 2, 6):                              # body, seam body, sac: the slate surfaces
        mats[slot] = replace(mats[slot], albedo=tuple(np.minimum(1, np.array(mats[slot].albedo) * BODY_LIFT)))
    dark = np.array(mats[V_DARK].albedo)
    mats[V_DARK] = replace(mats[V_DARK], albedo=tuple(dark + (mid - dark) * LEG_LIFT))
    violet = np.array(em.hx(em.GLOWS["violet"]))
    mats.append(replace(mats[V_BONE], emission=tuple(violet * STUD_GLOW * glow)))
    return mats


def creeper_mats(kind):
    if kind == "husk":
        return [replace(m, albedo=tuple(np.array(m.albedo) * HUSK_TONE)) for m in lifted_mats(HUSK_GLOW)]
    mats = lifted_mats(1.0)
    if kind == "glow":                                        # only the pores' (gland and back) and the eyes' emission
        black = Material((0.0, 0.0, 0.0), metal=0.0, shininess=1.0, spec=0.0)
        return [replace(m, albedo=(0.0, 0.0, 0.0), metal=0.0, spec=0.0, pattern=None,
                        emission=tuple(np.array(m.emission) * GLOW_GAIN))
                if i in (V_GLOW, V_EYE, V_STUD) else black for i, m in enumerate(mats)]
    return mats


def light_rim(img, strength=1.0):
    """The 1 px light rim (readability rules): the silhouette's outermost pixels mixed toward RIM,
    RIM_LIT where they face the top-left key light, RIM_SHADE where they face away (the outward
    direction from the blurred alpha's gradient), so the rim follows the light at every heading."""
    a = np.array(img.convert("RGBA")).astype(np.float64)
    solid = a[..., 3] > 0
    inner = solid.copy()
    inner[1:, :] &= solid[:-1, :]
    inner[:-1, :] &= solid[1:, :]
    inner[:, 1:] &= solid[:, :-1]
    inner[:, :-1] &= solid[:, 1:]
    edge = solid & ~inner
    soft = np.asarray(Image.fromarray(solid.astype(np.uint8) * 255).filter(ImageFilter.GaussianBlur(1.5)), float)
    gy, gx = np.gradient(soft)
    norm = np.maximum(np.hypot(gx, gy), 1e-6)
    facing = np.clip((gx / norm + gy / norm) / np.sqrt(2), 0, 1)   # the outward direction is -gradient; key light up-left
    k = (RIM_SHADE + (RIM_LIT - RIM_SHADE) * facing) * strength
    a[..., :3] = np.where(edge[..., None], a[..., :3] * (1 - k[..., None]) + RIM * k[..., None], a[..., :3])
    return Image.fromarray(np.clip(a + 0.5, 0, 255).astype(np.uint8), "RGBA")


def rotation(k):
    return np.pi - TAU * k / HEADINGS                        # +Y forward -> heading k, as vrell_l04


def model(k, j, kind="unit"):
    """(scene, mats) at heading k, walk frame j."""
    rot = rotation(k)
    base = creeper_scene(TAU * j / WALK_FRAMES, legs=kind != "husk")
    return (lambda p: base(rotate_z(p, rot))), model_space_materials(creeper_mats(kind), rot)


def render_unit(k, j, kind):
    img = artkit.native(*artkit.render_hi(*model(k, j, kind), SIZE, EXTENT))
    if kind == "glow":
        return artkit.additive(img)
    return light_rim(img, HUSK_RIM if kind == "husk" else 1.0)


# =========================================================================== death

def death_glow(i):
    """The gland bursting violet: a violet-white flash, ichor globules with short trails sagging a
    little, a dark plum mist billowing low and thinning, a few motes."""
    w, h = DEATH_SIZE
    t = fx.t_of(i, DEATH_FRAMES)
    rng = np.random.default_rng(3041)
    c = w / 2
    core = GLAND * PX                                         # ~12 px
    cv = v8.Canvas(w, h)
    d = cv.dist(c, c)
    fade = np.clip((w / 2 - d) / 5.0, 0, 1)
    tex = v8._cart_noise(w, 3042, cell=w * v8.SS // 6)
    churn = v8._shift(tex, t * w * 0.25, -t * w * 0.2)
    grow = fx.ease_out(t, 2.2)
    dens = v8.gauss(d, core * (1.3 + 1.6 * grow))
    haze = np.clip(dens * (0.5 + 0.9 * churn) - 0.2 - 0.5 * t, 0, 1)
    env = np.clip((i + 1) / 2.0, 0.4, 1) * (1 - t) ** 0.8
    cv.add((56, 22, 84), haze * 1.3 * env * fade)                    # the mist's plum body
    cv.add(VIOLET, np.clip(haze - 0.45, 0, 1) * 0.6 * env * fade)
    flash = max(0.0, 1 - i / 4.0)
    cv.add((240, 220, 255), v8.gauss(d, core * (0.6 + 0.5 * t)) * 1.3 * flash)
    cv.add(VIOLET, v8.gauss(d, core * (1.1 + 0.8 * t)) * 0.8 * flash)
    drops = [(rng.uniform(0, TAU), rng.uniform(0.5, 1.0), rng.uniform(0.6, 1.5), rng.uniform(0.5, 1.4),
              rng.uniform(0, 0.15), rng.uniform(0.8, 1.3)) for _ in range(44)]
    for a, start, reach, size, delay, decay in drops:
        if t < delay:
            continue
        tt = np.clip((t - delay) / (1 - delay), 0, 1)

        def at(u, a=a, start=start, reach=reach):
            r = core * start + core * 1.6 * reach * fx.ease_out(u, 2.6)
            return c + np.cos(a) * r, c + np.sin(a) * r + 2.0 * u * u
        x, y = at(tt)
        xp, yp = at(max(tt - 0.1, 0.0))
        life = np.clip(1.1 - tt * decay, 0, 1)
        cv.add(VIOLET, v8.gauss(cv.seg(xp, yp, x, y), size * 0.8) * 0.85 * life * fade)
        if size > 1.0:
            cv.add((230, 210, 255), v8.gauss(cv.dist(x, y), size * 0.6) * 1.0 * life * fade)
    vd.motes(cv, rng, c, t, 10, (14, 34), (0.5, 0.9), VIOLET, (245, 235, 255), fade)
    return artkit.additive(cv.image())


def death_pieces():
    """The six legs torn off at the hips (each with its toe pad), the fan frill in four shards,
    chitin chips with bone studs off the back; every piece a list of (geometry, material slot)."""
    rng = np.random.default_rng(3051)
    spine = spine_of(0.0)
    pieces = []
    for j in range(3):
        for side in (1, -1):
            hip, knee, foot = leg_points(spine, j, side, np.pi / 2)    # mid-swing, feet down
            stub = tuple(a + 0.3 * (b - a) for a, b in zip(hip, knee))  # the husk keeps the stubs
            tips = toes(foot, side, LEGS[j][1])

            def leg(q, stub=stub, knee=knee, foot=foot):
                return union((sd_capsule(q, stub, knee, 0.08, 0.06), 0), (sd_capsule(q, knee, foot, 0.055, 0.04), 0),
                             k=0.03)[0]

            def pad(q, foot=foot, tips=tips):
                return union((sd_ellipsoid(q, foot, (0.09, 0.09, 0.035)), 0),
                             *[(sd_capsule(q, foot, tip, 0.025, 0.015), 0) for tip in tips], k=0.03)[0]
            c0 = ((stub[0] + foot[0]) / 2, (stub[1] + foot[1]) / 2, 0.1)
            pieces.append(dict(parts=[(leg, V_DARK), (pad, V_BONE)], c0=c0, dir=vd.outward(rng, c0, 0.35),
                               reach=rng.uniform(0.45, 0.8), ease=3.0, axis=rng.uniform(0, TAU),
                               tumble=rng.uniform(3, 6) * rng.choice([-1, 1]), spin=rng.uniform(-2.5, 2.5),
                               rise=0.3, shrink_at=0.7))
    for half in FRILL_SHARDS:                                 # the frill split into four shards
        for side in (1, -1):
            poly = [(side * x, y) for x, y in half]
            cx, cy = float(np.mean([x for x, _ in poly])), float(np.mean([y for _, y in poly]))

            def shard(q, poly=poly):
                return sd_plate(q, poly, 0.15, 0.022, 0.01)
            c0 = (cx, cy, 0.15)
            pieces.append(dict(parts=[(shard, V_DARK)], c0=c0, dir=vd.outward(rng, c0, 0.4),
                               reach=rng.uniform(0.5, 0.75), ease=3.0, axis=rng.uniform(0, TAU),
                               tumble=rng.uniform(6, 9) * rng.choice([-1, 1]), spin=rng.uniform(-4, 4),
                               rise=0.25, shrink_at=0.7))
    for k in range(6):                                        # chitin chips along the back, with studs
        i = 1 + k
        bx, by = spine[i]
        sx = (1 if k % 2 == 0 else -1) * rng.uniform(0.06, 0.14)
        cx, cy = bx + sx, by - 0.04
        poly = fx.jagged(rng, rng.uniform(0.13, 0.17), 6)
        z = 0.18 + 0.6 * RADII[i] - 0.02

        def chip(q, cx=cx, cy=cy, poly=poly, z=z):
            r = q.copy()
            r[:, 0] -= cx
            r[:, 1] -= cy
            r[:, 2] -= z - 2.0 * (r[:, 0] ** 2 + r[:, 1] ** 2)   # curved like the back
            return sd_plate(r, poly, 0.0, 0.03, 0.01)

        def stud(q, cx=cx, cy=cy, z=z):
            return sd_sphere(q, (cx, cy, z + 0.03), 0.04)
        parts = [(chip, V_BODY)] + ([(stud, V_BONE)] if k < 4 else [])
        c0 = (cx, cy, z)
        pieces.append(dict(parts=parts, c0=c0, dir=vd.outward(rng, c0, 0.5),
                           reach=rng.uniform(0.6, 1.0), ease=2.6, axis=rng.uniform(0, TAU),
                           tumble=rng.uniform(4, 7) * rng.choice([-1, 1]), spin=rng.uniform(-3, 3),
                           rise=0.4, shrink_at=0.65))
    return pieces


def pieces_scene(pieces, base_mats, t, pre):
    """vrell_deaths.pieces_scene for pieces of several materials: each part of a piece posed with
    the piece, its material's pattern evaluated in the piece's frame."""
    mats = list(base_mats)
    items_of = []
    for pc in pieces:
        local, sc = vd.posed(pc, t)
        for geo, slot in pc["parts"]:
            m = mats[slot]
            mats.append(replace(m, pattern=l03.in_frame(m.pattern, local) if m.pattern else None,
                                emission_pattern=l03.in_frame(m.emission_pattern, local)
                                if m.emission_pattern else None))
            items_of.append((geo, local, sc, len(mats) - 1))

    def scene(p):
        p = pre(p)
        return union(*[(geo(local(p)) * sc, m) for geo, local, sc, m in items_of], k=0.01)
    return scene, mats


def death_tatters(i):
    # built facing +Y like the walker: heading 0 (down the screen) is a half turn
    scene, mats = pieces_scene(death_pieces(), lifted_mats(), fx.t_of(i, DEATH_FRAMES),
                               lambda p: rotate_z(p, np.pi))
    return artkit.native(*artkit.render_hi(scene, mats, DEATH_SIZE, DEATH_EXTENT))


# =========================================================================== build

def _job(job):
    name, args = job
    return {"unit": render_unit, "death": death_glow, "tatters": death_tatters}[name](*args)


def build():
    walk = [(k, j) for k in range(HEADINGS) for j in range(WALK_FRAMES)]   # heading * 6 + frame
    with ProcessPoolExecutor() as pool:
        def run(name, arglist):
            return list(pool.map(_job, [(name, a) for a in arglist]))
        sets = [("creeper", run("unit", [(k, j, "unit") for k, j in walk]), COLOURS),
                ("creeper-husk", run("unit", [(k, 0, "husk") for k in range(HEADINGS)]), HUSK_COLOURS),
                ("creeper-glow", run("unit", [(k, j, "glow") for k, j in walk]), GLOW_COLOURS),
                ("creeper-death", run("death", [(i,) for i in range(DEATH_FRAMES)]), DEATH_COLOURS),
                ("creeper-tatters", run("tatters", [(i,) for i in range(DEATH_FRAMES)]), DEATH_COLOURS)]
    for name, frames, colours in sets:
        artkit.write_frames(name, artkit.quantize_set(frames, colours), SOURCE)
        print(f"{name}: {len(frames)} frames, {frames[0].width}x{frames[0].height}")


# =========================================================================== review

GROUND = (40, 40, 48, 255)                      # night asphalt
ROAD = (54, 54, 62, 255)
LANE = (150, 130, 70, 255)


def heading_of(dx, dy):
    """Heading index of a screen direction (y down), clockwise from straight down."""
    return int(round(np.arctan2(-dx, dy) / TAU * HEADINGS)) % HEADINGS


def smooth_path(points, rounds=4):
    """A polyline with its corners rounded (Chaikin), sampled densely, and its arc length."""
    pts = np.array(points, float)
    for _ in range(rounds):
        q = [pts[0]]
        for a, b in zip(pts[:-1], pts[1:]):
            q += [0.75 * a + 0.25 * b, 0.25 * a + 0.75 * b]
        q.append(pts[-1])
        pts = np.array(q)
    dense = []
    for a, b in zip(pts[:-1], pts[1:]):
        n = max(1, int(np.hypot(*(b - a))))
        dense += [a + (b - a) * u for u in np.arange(n) / n]
    dense.append(pts[-1])
    dense = np.array(dense)
    length = np.concatenate([[0], np.cumsum(np.hypot(*np.diff(dense, axis=0).T))])
    return dense, length


def along(path, s):
    """Position and direction at arc length s."""
    dense, length = path
    i = int(np.clip(np.searchsorted(length, s), 1, len(dense) - 1))
    a, b = dense[i - 1], dense[i]
    u = np.clip((s - length[i - 1]) / max(length[i] - length[i - 1], 1e-6), 0, 1)
    return a + (b - a) * u, b - a


def review():
    frames = artkit.load_frames("creeper")
    husk = artkit.load_frames("creeper-husk")
    glow = artkit.load_frames("creeper-glow")
    tatters = artkit.load_frames("creeper-tatters")
    burst = artkit.load_frames("creeper-death")
    boom = artkit.load_frames("explosion-medium")
    heading = [frames[k * WALK_FRAMES:(k + 1) * WALK_FRAMES] for k in range(HEADINGS)]
    glows = [glow[k * WALK_FRAMES:(k + 1) * WALK_FRAMES] for k in range(HEADINGS)]
    layers = [(tatters, DEATH_STEPS, False, 0), (boom, 2, True, 0), (burst, DEATH_STEPS, True, 0)]
    together = fx.composite_strip(layers, DEATH_FRAMES * DEATH_STEPS, DEATH_SIZE, 1)
    on_husk = []
    for frame in together:
        cell = Image.new("RGBA", DEATH_SIZE, GROUND)
        sprite.paste_center(cell, husk[0], DEATH_SIZE[0] / 2, DEATH_SIZE[1] / 2)
        cell.alpha_composite(frame.convert("RGBA"))
        on_husk.append(cell)
    sheet = artkit.review_sheet("CREEPER - FINAL SPRITES (PROPOSAL)", [
        ("HEADINGS 0-7 (CLOCKWISE FROM DOWN: 4 WALKS LEFT), WALK FRAME 0", [h[0] for h in heading[:8]], 3, False),
        ("HEADINGS 8-15 (12 WALKS RIGHT)", [h[0] for h in heading[8:]], 3, False),
        (f"WALK CYCLE, HEADING 0 (DOWN), {STRIDE_PX} PX PER CYCLE", heading[0], 4, False),
        ("WALK CYCLE, HEADING 4 (LEFT)", heading[4], 4, False),
        ("WALK CYCLE, HEADING 10", heading[10], 4, False),
        ("HUSKS 0-7", husk[:8], 3, False), ("HUSKS 8-15", husk[8:], 3, False),
        ("VIOLET GLAND AND EYE GLOW MASKS, HEADING 4 (ADDITIVE)", glows[4], 4, True),
        (f"TATTERS, {STEP // DEATH_STEPS} FPS, SOLID, EVERY 2ND FRAME", tatters[::2], 2, False),
        (f"DEATH GLOW, {STEP // DEATH_STEPS} FPS, ADDITIVE, EVERY 2ND FRAME", burst[::2], 2, True),
        ("TOGETHER WITH THE MEDIUM BURST OVER THE HUSK, EVERY 4TH STEP; PIECES UNDER THE GLOWS", on_husk[::2], 2, False),
        ("1X", [h[0] for h in heading] + husk[::4], 1, False)], width=1700, batch=BATCH)
    # the loop: a convoy of three on a street that turns, 1.5 s apart; the lead dies and leaves its husk
    fw, fh, fps = 230, 260, 20
    path = smooth_path([(60, -40), (60, 70), (170, 120), (170, 300)])
    total = path[1][-1]
    lag = 1.5 * SPEED                                         # the convoy's spacing, px of path
    die_at = 290.0                                            # px of path where the lead dies (lower straight)
    die_step = int(die_at / SPEED * STEP)
    n = int(fps * 9.8)
    plate = Image.new("RGBA", (fw, fh), GROUND)
    draw = ImageDraw.Draw(plate)
    dense = path[0]
    draw.line([tuple(p) for p in dense], fill=ROAD, width=46)
    for s in np.arange(0, total, 16):                         # the lane dashes
        a, _ = along(path, s)
        b, _ = along(path, s + 7)
        draw.line([tuple(a), tuple(b)], fill=LANE, width=1)
    gif = []
    lead = None
    for f in range(n):
        step = f * STEP // fps
        cell = plate.copy()
        lights = []
        for u in range(3):
            s = step / STEP * SPEED - u * lag
            if u == 0 and step >= die_step:
                if lead is None:
                    pos, dv = along(path, die_at)
                    lead = (pos, heading_of(*dv))
                sprite.paste_center(cell, husk[lead[1]], *lead[0])
                continue
            if s < 0 or s > total:
                continue
            pos, dv = along(path, s)
            k = heading_of(*dv)
            j = int(np.floor(s / STRIDE_PX * WALK_FRAMES)) % WALK_FRAMES   # the walk frame follows the distance
            sprite.paste_center(cell, heading[k][j], *pos)
            lights.append((glows[k][j], pos))
        if lead is not None:
            age = step - die_step
            x, y = lead[0]
            for frames_, steps, glowing, delay in layers:
                fr = fx.timed(frames_, steps, age - delay)
                if fr is None:
                    continue
                xy = (int(round(x - fr.width / 2)), int(round(y - fr.height / 2)))
                if glowing:
                    lights.append((fr, None, xy))
                else:
                    cell.alpha_composite(fr, xy)
        for light in lights:
            if len(light) == 3:
                cell = artkit.add_light(cell, light[0], light[2])
            else:
                fr, (x, y) = light
                cell = artkit.add_light(cell, fr, (int(round(x - fr.width / 2)), int(round(y - fr.height / 2))))
        gif.append(sprite.enlarge(cell, 2))
    artkit.save_review(sheet, gif, CONCEPT, "creeper", fps=fps)


if __name__ == "__main__":
    artkit.REVIEW_ROUND = REVIEW_ROUND
    if "--review" not in sys.argv[1:]:
        build()
    review()
