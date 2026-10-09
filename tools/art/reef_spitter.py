#!/usr/bin/env python3
"""Production art: the Reef Spitter, Level 11's barnacle gun on a drifting kelp raft
(design/enemies/naval/reef-spitter; M5 part E batch, concept round 33). Straight to production from
its chosen concept (user decision E9 = a): the round-06 models reef_gun and reef_raft
(tools/concept/render/r06_models.py, imported unchanged; the concept sheet is
tools/concept/enemies_r06.py), with the round-07 water rules (waterline cut, foam collar).

Water (E4 = c, see tools/art/driftjelly.py): the raft's under-water part (the lower lobes and the
seaweed fringe) is drawn by the game's generic `sub` pass from a plain raft frame; the part above
the waterline, its foam collar, the bob and the sinking are pre-rendered here.

Outputs (assets/sprites/):
  reef-spitter_0..31          36x36, `orientation: 32 angles`: the gun (heading k = k x 11.25 degrees
                              clockwise from straight down, heading 0 aiming down the screen), each
                              its own render under the fixed key light; the violet barrel root and
                              mouths emissive
  reef-spitter-recoil_0..31   36x36, the recoil frame at the same headings: the three mouths pulled
                              back, their glow flared; shown for 6 steps from a fan
  reef-spitter-raft-sub       96x96, the whole plain raft (84 px across, 36.5 px per model unit),
                              drawn through the `sub` pass under the raft (its under-water part)
  reef-spitter-raft_0..7      96x96, the raft above the waterline with its foam collar, bobbing: 8
                              frames of one 1.6 s bob (10 steps... 12 steps a frame: 7.5 fps), the
                              water plane rising and falling 0.03 units and the mat tilting 3
                              degrees round a turning axis, the collar lapping; drawn on the surface
                              over reef-spitter-raft-sub, the gun over it at the frame's pivot
  reef-spitter-sink_0..9      96x96, the gunless raft sinking (6 steps a frame, 1 s): the water rising
                              over it, one side dipping first, the collar closing in, bubbles and a
                              last foam swirl; drawn over reef-spitter-raft-sub faded 1 -> 0
  reef-spitter-death_0..9     64x64, additive: the `small` burst of the gun, violet (4 steps a frame,
                              with explosion-small)
  reef-spitter-tatters_0..9   64x64, solid: the gun's plates, mouths and base flung out, falling and
                              darkening into the water (4 steps a frame)
  pivots/reef-spitter.json    per heading the three mouths' muzzle points of the gun frames (left,
                              centre, right as seen along the aim; recoil frames alike); per raft
                              and sink frame the gun's point (px from the sprite's top left)
  design/enemies/naval/reef-spitter/concept/reef-spitter-final-r33-a.png/.gif

How the game draws it (game side, step E3c): reef-spitter-raft-sub through the `sub` pass;
reef-spitter-raft_<bob> on the surface; reef-spitter_<heading> (reef-spitter-recoil for 6 steps
after a fan) centred on the raft frame's gun point. On the death: explosion-small +
reef-spitter-death + reef-spitter-tatters at the gun, and the raft plays reef-spitter-sink over its
fading sub frame (the shared splash and ripple train from tools/art/water_fx.py on top).

Run: python3 tools/art/reef_spitter.py [--review]   (~1 min; --review only rebuilds the review files)
"""
import sys
from concurrent.futures import ProcessPoolExecutor

import numpy as np
from PIL import Image

import artkit
from artkit import DESIGN, TAU, sprite

import creeper  # noqa: E402  (tools/art: pieces of several materials)
import driftjelly as dj  # noqa: E402  (tools/art: the water look: foam, swell, the sub-pass stand-in)
import vfx_r08 as v8  # noqa: E402  (concept script, imported unchanged)
import vrell_deaths as vd  # noqa: E402
import vrell_fx as fx  # noqa: E402
from render import enemy_models as em  # noqa: E402
from render import r06_models as m6  # noqa: E402
from render.enemy_models import V_BODY, V_BONE, V_DARK, V_GLOW, V_SEAM  # noqa: E402
from render.enemy_rigs import model_rotation, model_space_materials  # noqa: E402
from render.sdf import rotate_x, rotate_z, sd_capsule, sd_cylinder_z, sd_ellipsoid, sd_sphere  # noqa: E402

SCRIPT = "reef_spitter.py"
BATCH = "M5 part E batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
REVIEW_ROUND = "r33"
UNIT = DESIGN / "enemies" / "naval" / "reef-spitter"
CONCEPT = UNIT / "concept"
SLUG = "reef-spitter"
STEP = 60

GUN = (36, 36)
GUN_EXTENT = 2.3                                # the concept's (15.7 px per model unit)
GUN_PX = GUN[0] / GUN_EXTENT
HEADINGS = 32
RAFT = (96, 96)
RAFT_EXTENT = 2.3 * 96 / 84                     # the concept's 84 px raft at 36.5 px per unit, on 96 px
RAFT_PX = RAFT[0] / RAFT_EXTENT
BOB_FRAMES, BOB_STEPS = 8, 12
SINK_FRAMES, SINK_STEPS = 10, 6
DEATH_SIZE, DEATH_FRAMES = (64, 64), 10
DEATH_EXTENT = GUN_EXTENT * DEATH_SIZE[0] / GUN[0]
GUN_LIFT = 0.0                                  # the gun sits on the raft's centre
COLOURS = 40
MOUTHS = (-0.42, 0.0, 0.42)                     # r06_models.reef_gun's three mouths (rad off the aim)
VIOLET = np.array(em.hx(em.GLOWS["violet"])) * 255
WHITE_VIOLET = (240, 228, 255)


# =========================================================================== gun

def gun_rotation(k):
    return model_rotation(np.pi / 2 + TAU * k / HEADINGS)     # +Y forward -> heading k


def render_gun(k, recoil):
    scene, mats = m6.reef_gun(recoil=1.0 if recoil else 0.0, glow=1.9 if recoil else 1.0)
    rot = gun_rotation(k)
    hi, f = artkit.render_hi(lambda p: scene(rotate_z(p, rot)), model_space_materials(mats, rot), GUN, GUN_EXTENT)
    return artkit.native(hi, f)


def muzzles(k, recoil=False):
    """The three mouths' tips at heading k in gun-sprite px (left to right along the aim)."""
    rot = gun_rotation(k)
    rc = -0.12 if recoil else 0.0
    out = []
    for ang in MOUTHS[::-1]:                                  # +ang is to the aim's left in model space
        m = np.array([[(0.5 + rc + 0.1) * np.cos(np.pi / 2 + ang), (0.5 + rc + 0.1) * np.sin(np.pi / 2 + ang), 0.46]])
        p = rotate_z(m, -rot)[0]
        out.append([round(GUN[0] / 2 + p[0] * GUN_PX, 1), round(GUN[1] / 2 - p[1] * GUN_PX, 1)])
    return out


# =========================================================================== raft

def bob_pose(j, n=BOB_FRAMES):
    """The water plane's offset (model z) and the mat's tilt (radians about a turning axis)."""
    w = TAU * j / n
    return 0.03 * np.sin(w), 0.05, w * 0.5 + 0.4


def raft_model(dz=0.0, tilt=0.0, axis=0.0, level=0.0, keep="above"):
    """The raft tilted ``tilt`` about an in-plane axis at angle ``axis``, raised ``dz``, cut at the
    water plane z = level (keep the part above or below it; None: whole)."""
    scene, mats = m6.reef_raft()

    def posed(p):
        q = p.copy()
        q[:, 2] -= dz
        q = rotate_z(rotate_x(rotate_z(q, axis), tilt), -axis)
        d, m = scene(q)
        if keep == "above":
            d = np.maximum(d, level - p[:, 2])
        elif keep == "below":
            d = np.maximum(d, p[:, 2] - level)
        return d, m
    return posed, mats


def render_raft(dz, tilt, axis, level, keep):
    scene, mats = raft_model(dz, tilt, axis, level, keep)
    return artkit.native(*artkit.render_hi(scene, mats, RAFT, RAFT_EXTENT))


def gun_point(dz, tilt, axis):
    """Where the gun sits on a posed raft, in raft-sprite px (the mat's centre top, tilted)."""
    top = np.array([[0.0, 0.0, 0.06]])
    q = rotate_z(rotate_x(rotate_z(top, axis), -tilt), -axis)
    return [round(RAFT[0] / 2 + q[0, 0] * RAFT_PX, 1), round(RAFT[1] / 2 - q[0, 1] * RAFT_PX - 1, 1)]


def raft_frame(j):
    dz, tilt, axis = bob_pose(j)
    above = render_raft(dz, tilt, axis, 0.0, "above")
    mask = np.array(above)[..., 3] > 0
    f = dj.Field(*RAFT)
    dj.edge_foam(f, mask, TAU * j / BOB_FRAMES, 3401, width=2.6, specks=22, reach=8, inner=1.4)
    out = Image.new("RGBA", RAFT)
    out.alpha_composite(above)
    out.alpha_composite(f.image())
    return out


def sink_frame(j):
    """Sink step j: the water at a rising level, the mat tilting toward one side, the collar
    closing in, bubbles; at the end only a swirl of foam where it went down."""
    t = (j + 1) / SINK_FRAMES
    level = 0.02 + 0.17 * t ** 1.3
    tilt, axis = 0.05 + 0.1 * t, 1.1
    w, h = RAFT
    f = dj.Field(w, h)
    rng = np.random.default_rng(3420)
    if t < 0.95:
        above = render_raft(0.0, tilt, axis, level, "above")
        mask = np.array(above)[..., 3] > 0
        if mask.sum() > 4:
            dj.edge_foam(f, mask, 1.3 * j, 3421 + j, width=2.6 + 1.4 * t, specks=14 + 10 * j, reach=5 + 6 * t,
                         gain=0.9, inner=1.0 + 2 * t)
    else:
        above = None
    for _ in range(26):                                       # bubbles boiling up over the mat
        a, r = rng.uniform(0, TAU), 30 * np.sqrt(rng.uniform(0, 1))
        born = rng.uniform(0.15, 0.95)
        if t < born:
            continue
        age = (t - born) / 0.35
        if age > 1:
            continue
        x, y = w / 2 + np.cos(a) * r, h / 2 + np.sin(a) * r * 0.9
        d = np.hypot(f.x - x, f.y - y)
        rad = 0.7 + 1.3 * age
        f.over(dj.FOAM, (np.abs(d - rad) < 0.45) * (1 - age) * 0.9)
    if t > 0.6:                                               # the last swirl of foam
        s = (t - 0.6) / 0.4
        dj.foam_ring(f, w / 2 + 6, h / 2 + 4, 10 + 18 * s, 2.1 * j, 3430, width=2.4, specks=18, reach=8,
                     gain=0.9 * (1 - 0.5 * s))
    out = Image.new("RGBA", RAFT)
    if above is not None:
        a = np.array(above).astype(np.float64)
        k = 0.45 * t                                          # wet, darkening as it goes under
        a[..., :3] = a[..., :3] * (1 - k) + dj.WATER_TINT * k
        out.alpha_composite(Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA"))
    out.alpha_composite(f.image())
    return out, gun_point(0.0, tilt, axis)


# =========================================================================== death

def death_glow(i):
    """The gun's `small` burst: a white-violet flash from the mouths, a violet bloom, sparks."""
    w, h = DEATH_SIZE
    t = fx.t_of(i, DEATH_FRAMES)
    c = w / 2
    cv = v8.Canvas(w, h)
    d = cv.dist(c, c)
    fade = np.clip((w / 2 - d) / 4.0, 0, 1)
    cv.add(WHITE_VIOLET, v8.gauss(d, 3 + 5 * t) * 1.4 * np.clip(1 - t * 2.8, 0, 1))
    cv.add(tuple(VIOLET), v8.gauss(d, 7 + 11 * fx.ease_out(t)) * 0.6 * (1 - t) ** 1.5 * fade)
    tex = v8._cart_noise(w, 3441, cell=w * v8.SS // 5)
    ring = 5 + 18 * fx.ease_out(t, 2.2)
    cv.add(tuple(VIOLET), v8.gauss(np.abs(d - ring), 1.2 + 1.5 * t) * (0.3 + 0.7 * tex) * 0.5 * (1 - t) ** 2 * fade)
    vd.motes(cv, np.random.default_rng(3442), c, t, 20, (10, 26), (0.5, 1.0), tuple(VIOLET), WHITE_VIOLET, fade)
    return artkit.additive(cv.image())


def death_pieces():
    """The gun in pieces: six armour plates, the three mouths, the plated cone in two."""
    rng = np.random.default_rng(3451)
    pieces = []
    for a in np.linspace(0.6, TAU + 0.6, 6, endpoint=False):
        c0 = (0.38 * np.cos(a), 0.38 * np.sin(a) - 0.05, 0.22)

        def plate(q, c0=c0):
            return sd_ellipsoid(q, c0, (0.15, 0.15, 0.12))
        pieces.append(dict(parts=[(plate, V_DARK)], c0=c0, dir=vd.outward(rng, c0, 0.35),
                           reach=rng.uniform(0.5, 0.9), ease=2.6, axis=rng.uniform(0, TAU),
                           tumble=rng.uniform(3, 6) * rng.choice([-1, 1]), spin=rng.uniform(-3, 3), rise=0.4,
                           shrink_at=0.55))
    for ang in MOUTHS:
        ca, sa = np.cos(np.pi / 2 + ang), np.sin(np.pi / 2 + ang)

        parts = [(lambda q, ca=ca, sa=sa: sd_capsule(q, (0.2 * ca, 0.2 * sa, 0.42), (0.5 * ca, 0.5 * sa, 0.4), 0.14, 0.12),
                  V_DARK),
                 (lambda q, ca=ca, sa=sa: sd_cylinder_z(q, (0.5 * ca, 0.5 * sa, 0.46), 0.14, 0.05), V_BONE),
                 (lambda q, ca=ca, sa=sa: sd_sphere(q, (0.5 * ca, 0.5 * sa, 0.46), 0.08), V_GLOW)]
        c0 = (0.38 * ca, 0.38 * sa, 0.42)
        pieces.append(dict(parts=parts, c0=c0, dir=vd.outward(rng, c0, 0.25), reach=rng.uniform(0.6, 0.95),
                           ease=2.4, axis=rng.uniform(0, TAU), tumble=rng.uniform(4, 7) * rng.choice([-1, 1]),
                           spin=rng.uniform(-3, 3), rise=0.5, shrink_at=0.6))
    for side in (1, -1):
        def half(q, side=side):
            return np.maximum(sd_ellipsoid(q, (0, -0.05, 0.22), (0.4, 0.44, 0.28)), -side * q[:, 0])
        c0 = (side * 0.15, -0.05, 0.2)
        pieces.append(dict(parts=[(half, V_SEAM)], c0=c0, dir=(side * 1.0, rng.uniform(-0.3, 0.3)), reach=0.2,
                           ease=3.0, axis=rng.uniform(0, TAU), tumble=1.5 * side, spin=rng.uniform(-1, 1),
                           rise=0.15, shrink_at=0.35, least=0.15))
    pieces.append(dict(parts=[(lambda q: sd_cylinder_z(q, (0, 0, 0.05), 0.48, 0.1), V_BODY)], c0=(0.0, 0.0, 0.05),
                       dir=(0.0, 1.0), reach=0.02, ease=3.0, axis=0.0, tumble=0.3, spin=0.4, rise=0.0,
                       shrink_at=0.3, least=0.1))
    return pieces


def death_tatters(i):
    t = fx.t_of(i, DEATH_FRAMES)
    mats = m6.reef_gun(glow=0.5)[1]
    scene, mats = creeper.pieces_scene(death_pieces(), mats, t, lambda p: rotate_z(p, np.pi))
    img = artkit.native(*artkit.render_hi(scene, mats, DEATH_SIZE, DEATH_EXTENT))
    sink = float(np.clip((t - 0.5) / 0.5, 0, 1))
    if sink > 0:
        a = np.array(img).astype(np.float64)
        k = 0.65 * sink
        a[..., :3] = a[..., :3] * (1 - k) * (1 - 0.25 * sink) + dj.WATER_TINT * k
        img = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")
    return img


# =========================================================================== build

def _job(job):
    kind, i = job
    if kind == "gun":
        return render_gun(i, False)
    if kind == "recoil":
        return render_gun(i, True)
    if kind == "raft-sub":
        return render_raft(0.0, 0.0, 0.0, 0.0, None)
    if kind == "raft":
        return raft_frame(i)
    if kind == "sink":
        return sink_frame(i)
    if kind == "death":
        return death_glow(i)
    return death_tatters(i)


def build():
    jobs = ([("gun", k) for k in range(HEADINGS)] + [("recoil", k) for k in range(HEADINGS)] + [("raft-sub", 0)]
            + [("raft", j) for j in range(BOB_FRAMES)] + [("sink", j) for j in range(SINK_FRAMES)]
            + [("death", i) for i in range(DEATH_FRAMES)] + [("tatters", i) for i in range(DEATH_FRAMES)])
    with ProcessPoolExecutor() as pool:
        out = list(pool.map(_job, jobs))
    at = 0

    def take(n):
        nonlocal at
        part = out[at:at + n]
        at += n
        return part
    guns = artkit.quantize_set(take(2 * HEADINGS), COLOURS)
    raft_sub = take(1)
    bob = take(BOB_FRAMES)
    sink = take(SINK_FRAMES)
    rafts = artkit.quantize_set(raft_sub + bob + [s for s, _ in sink], COLOURS)
    sets = [(SLUG, guns[:HEADINGS]), (f"{SLUG}-recoil", guns[HEADINGS:]), (f"{SLUG}-raft-sub", rafts[:1]),
            (f"{SLUG}-raft", rafts[1:1 + BOB_FRAMES]), (f"{SLUG}-sink", rafts[1 + BOB_FRAMES:]),
            (f"{SLUG}-death", artkit.quantize_set(take(DEATH_FRAMES), 24)),
            (f"{SLUG}-tatters", artkit.quantize_set(take(DEATH_FRAMES), 24))]
    for name, frames in sets:
        artkit.write_frames(name, frames, SOURCE, single=name.endswith("-sub"))
        print(f"{name}: {len(frames)} frames, {frames[0].width}x{frames[0].height}")
    artkit.write_pivots(SLUG, SOURCE, {
        "headings": HEADINGS,
        "muzzles": [muzzles(k) for k in range(HEADINGS)],
        "muzzles_recoil": [muzzles(k, True) for k in range(HEADINGS)],
        "muzzle_order": "left, centre, right as seen along the aim; px from the gun frame's top left",
        "raft_gun": [gun_point(*bob_pose(j)) for j in range(BOB_FRAMES)],
        "sink_gun": [p for _, p in sink],
        "raft_frame_steps": BOB_STEPS, "sink_frame_steps": SINK_STEPS, "recoil_steps": 6})


# =========================================================================== review

def put(cell, img, x, y):
    cell.alpha_composite(img, (int(round(x - img.width / 2)), int(round(y - img.height / 2))))
    return cell


def add(cell, img, x, y):
    return artkit.add_light(cell, img, (int(round(x - img.width / 2)), int(round(y - img.height / 2))))


def draw_spitter(cell, art, piv, x, y, bob, heading, recoil=False, t=0.0):
    """As the game draws it: the plain raft through the sub-pass stand-in, the raft's surface frame,
    the gun at the frame's gun point."""
    rx, ry = x - RAFT[0] / 2, y - RAFT[1] / 2
    put(cell, dj.sub_pass(art["raft-sub"][0], t), x, y)
    put(cell, art["raft"][bob], x, y)
    gx, gy = piv["raft_gun"][bob]
    put(cell, (art["recoil"] if recoil else art["gun"])[heading], rx + gx, ry + gy)
    return cell


def review():
    import json
    artkit.REVIEW_ROUND = REVIEW_ROUND
    art = {k: artkit.load_frames(f"{SLUG}{s}") for k, s in (("gun", ""), ("recoil", "-recoil"),
                                                              ("raft-sub", "-raft-sub"), ("raft", "-raft"),
                                                              ("sink", "-sink"), ("death", "-death"),
                                                              ("tatters", "-tatters"))}
    piv = json.loads((artkit.PIVOTS / f"{SLUG}.json").read_text(encoding="utf-8"))
    boom = artkit.load_frames("explosion-small")
    sea = lambda w, h: dj.ocean_plate(w, h, seed=7, x0=100, y0=100)  # noqa: E731
    muzzle_marks = []
    for k in range(0, HEADINGS, 4):
        cell = artkit.on_background(art["gun"][k], 3)
        d = Image.new("RGBA", cell.size)
        from PIL import ImageDraw
        dd = ImageDraw.Draw(d)
        for mx, my in piv["muzzles"][k]:
            dd.rectangle([mx * 3 - 1, my * 3 - 1, mx * 3 + 1, my * 3 + 1], fill=(255, 80, 60, 255))
        cell.alpha_composite(d)
        muzzle_marks.append(cell)
    drawn = [draw_spitter(sea(110, 110), art, piv, 55, 55, j, (4 * j) % HEADINGS, t=0.2 * j) for j in range(BOB_FRAMES)]
    sinking = []
    for j in range(SINK_FRAMES):
        cell = sea(110, 110)
        sub = art["raft-sub"][0]
        a = np.array(dj.sub_pass(sub, 0.1 * j)).astype(np.float64)
        a[..., 3] *= 1 - (j + 1) / SINK_FRAMES
        put(cell, Image.fromarray(a.astype(np.uint8), "RGBA"), 55, 55)
        sinking.append(put(cell, art["sink"][j], 55, 55))
    layers = [(art["tatters"], 4, False, 0), (boom, 2, True, 0), (art["death"], 4, True, 0)]
    together = fx.composite_strip(layers, DEATH_FRAMES * 4, DEATH_SIZE, 1)
    sheet = artkit.review_sheet("REEF SPITTER - FINAL SPRITES (PROPOSAL)", [
        ("GUN, 32 HEADINGS: 0-15 (0 AIMS DOWN, 8 LEFT)", art["gun"][:16], 2, False),
        ("GUN, HEADINGS 16-31 (16 AIMS UP, 24 RIGHT)", art["gun"][16:], 2, False),
        ("RECOIL FRAME, EVERY 4TH HEADING", art["recoil"][::4], 3, False),
        ("MUZZLE PIVOTS (RED), EVERY 4TH HEADING", muzzle_marks, 1, False),
        ("RAFT FOR THE SUB PASS (PLAIN)", art["raft-sub"], 2, False),
        ("RAFT ABOVE THE WATERLINE, BOB FRAMES 0-7 (12 STEPS A FRAME), SURFACE LAYER", art["raft"], 2, False),
        ("AS DRAWN ON THE SEA STAND-IN: SUB-PASS RAFT + RAFT + GUN (HEADINGS 0, 4, 8 ...)", drawn, 2, False),
        ("SINKING, 6 STEPS A FRAME, SURFACE LAYER", art["sink"], 2, False),
        ("AS DRAWN: SINKING OVER THE FADING SUB-PASS RAFT", sinking, 2, False),
        ("DEATH: VIOLET BURST (ADDITIVE)", art["death"], 2, True),
        ("DEATH: GUN PIECES (SOLID)", art["tatters"], 2, False),
        ("TOGETHER WITH EXPLOSION-SMALL, EVERY 2ND STEP", together[::2], 1, False)], width=1400, batch=BATCH)
    artkit.save_review(sheet, review_loop(art, piv, boom), CONCEPT, SLUG, fps=20)


def review_loop(art, piv, boom):
    """A nest of three rafts drifting down with the sea (30 px/s) plus a 10 px/s current, the guns
    tracking a passing ship and firing 3-way fans (recoil frames); one is shot: the gun bursts and
    the raft sinks. 240x270 at 2x."""
    fw, fh, fps = 240, 270, 20
    plate = dj.ocean_plate(fw, 540, seed=33)
    orb = artkit.load_frames("orb")
    ship = artkit.load_frames("ship")[2]
    nest = [(60, 20), (170, 60), (95, 130)]
    total, scroll = 6.0, 40.0
    shots = []
    dead, t_dead = 1, 3.6
    gif = []
    for f in range(int(total * fps)):
        t = f / fps
        off = int(t * 30) % (540 - fh)
        cell = plate.crop((0, 540 - fh - off, fw, 540 - off))
        px, py = 120 + 70 * np.sin(0.9 * t), 235.0
        for i, (x0, y0) in enumerate(nest):
            x, y = x0, y0 + scroll * t
            if i == dead and t >= t_dead:
                age = int((t - t_dead) * STEP)
                sj = min(age // SINK_STEPS, SINK_FRAMES - 1)
                if age < SINK_FRAMES * SINK_STEPS:
                    a = np.array(dj.sub_pass(art["raft-sub"][0], t)).astype(np.float64)
                    a[..., 3] *= 1 - (sj + 1) / SINK_FRAMES
                    put(cell, Image.fromarray(a.astype(np.uint8), "RGBA"), x, y)
                    put(cell, art["sink"][sj], x, y)
                gx, gy = piv["raft_gun"][0]
                gx, gy = x - RAFT[0] / 2 + gx, y - RAFT[1] / 2 + gy
                for frames, steps, glow in ((art["tatters"], 4, False), (boom, 2, True), (art["death"], 4, True)):
                    fr = fx.timed(frames, steps, age)
                    if fr is not None:
                        cell = add(cell, fr, gx, gy) if glow else put(cell, fr, gx, gy)
                continue
            bob = int((t + 0.37 * i) * STEP / BOB_STEPS) % BOB_FRAMES
            gx, gy = piv["raft_gun"][bob]
            gx, gy = x - RAFT[0] / 2 + gx, y - RAFT[1] / 2 + gy
            aim = np.arctan2(-(px - gx), py - gy) % TAU            # clockwise from straight down
            k = int(round(aim / TAU * HEADINGS)) % HEADINGS
            fire = 1.0 + 0.8 * i
            recoil = False
            while fire <= t:
                if t - fire < 6 / STEP:
                    recoil = True
                if not any(s[0] == fire and s[1] == i for s in shots):
                    mx, my = piv["muzzles"][k][1]
                    shots.append((fire, i, gx - GUN[0] / 2 + mx, gy - GUN[1] / 2 + my, aim))
                fire += 2.4
            cell = draw_spitter(cell, art, piv, x, y, bob, k, recoil, t + i)
        for t0, i, sx, sy, aim in shots:
            if t < t0:
                continue
            u = (t - t0) * 150
            for spread in (-np.radians(15), 0.0, np.radians(15)):
                a = aim + spread
                bx, by = sx - np.sin(a) * u, sy + np.cos(a) * u + scroll * 0
                if orb is not None and -10 < by < fh + 10:
                    cell = add(cell, orb[int(t * 15) % len(orb)], bx, by)
        put(cell, ship, px, py)
        gif.append(sprite.enlarge(cell, 2))
    return gif


if __name__ == "__main__":
    artkit.REVIEW_ROUND = REVIEW_ROUND
    if "--review" not in sys.argv[1:]:
        build()
    review()
