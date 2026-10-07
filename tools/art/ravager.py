#!/usr/bin/env python3
"""Production art: the Ravager, Act 2's Vrell pack hunter (design/enemies/ground/ravager; M5 part C
batch, concept round 31). Straight to production from its chosen concept (user decision D9 = a):
the round-05 model ravager-r05-a (tools/concept/render/beast_models.ravager, imported unchanged).

Outputs (assets/sprites/; the size and the stride from the Ravager's data.yaml when it has them):
  ravager_0..127            56x56, `16 angles`, ground layer: 16 headings x 8 gallop frames,
                            indexed heading * 8 + frame (EnemyLooks.walkFrame); heading k runs
                            k x 22.5 degrees clockwise from straight down (heading 0 runs down the
                            screen, heading 4 to the left); one gallop cycle = the stride (46 px)
  ravager-glow_0..127       56x56, additive: the teal maw and the eyes alone, in the gallop frames'
                            order, drawn above the low-air layer
  ravager-leap_0..63        80x80: the leap pose (fore legs reaching, hind legs thrown back, jaws
                            wide) at 16 headings x 4 lift steps, indexed heading * 4 + step; step s
                            draws the beast LEAP_SCALES[s] = 1.11, 1.21, 1.32, 1.43 x its ground size
                            (lift 0.25, 0.5, 0.75, 1), so the renderer never scales a lit frame
  ravager-leap-glow_0..63   80x80, additive: the leap frames' maw and eyes, same order
  ravager-death_0..9        80x80, additive: the `small` organic burst: a teal-white flash from
                            the maw, a teal bloom, teal ichor flecks and a rust-brown mist (4 steps
                            per frame, with the small burst)
  ravager-tatters_0..9      80x80, solid: the four legs torn off, the tail in three pieces, the
                            jaws and chitin flakes off the back, flung out, tumbling, shrivelling;
                            no husk (nothing is left)
  design/enemies/ground/ravager/concept/ravager-final-r31-a.png/.gif

The pounce's shadow is not baked (art direction, Shadows rule): the game draws it from the leap
frame's silhouette (Shadows.draw), pushed down-right with the lift.

For Level 08/09's night city (the last hour of night, user decision D9) the rust chitin is lifted a
little, the near-black legs and tail bands are mixed toward the rust, and every gallop and leap
frame gets the Creeper's 1 px lavender-white light rim (tools/art/creeper.py light_rim), stronger
where the silhouette faces the top-left key light. Every heading is its own render under the fixed
key light; the material patterns turn with the body (model space, as in round 05).

Run: python3 tools/art/ravager.py [--review]   (~2 min; --review only rebuilds the review files)
"""
import sys
from concurrent.futures import ProcessPoolExecutor
from dataclasses import replace

import numpy as np
import yaml
from PIL import Image

import artkit
from artkit import DESIGN, ROOT, TAU, sprite

import creeper  # noqa: E402  (tools/art: the night city's light rim, the review's path helpers)
import vfx_r08 as v8  # noqa: E402  (concept script, imported unchanged)
import vrell_deaths as vd  # noqa: E402  (tools/art: the death pieces' pose and the motes)
import vrell_fx as fx  # noqa: E402  (tools/art: easing, torn outlines, timing)
import vrell_l03 as l03  # noqa: E402  (tools/art: patterns in a part's own frame)
from render import beast_models as bm  # noqa: E402
from render import enemy_models as em  # noqa: E402
from render.enemy_models import V_BODY, V_BONE, V_DARK, V_EYE, V_GLOW  # noqa: E402
from render.enemy_rigs import model_space_materials  # noqa: E402
from render.sdf import Material, rotate_z, sd_capsule, sd_ellipsoid, sd_plate, union  # noqa: E402

# =========================================================================== parameters

SCRIPT = "ravager.py"
BATCH = "M5 part C batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
REVIEW_ROUND = "r31"
CONCEPT = DESIGN / "enemies" / "ground" / "ravager" / "concept"
_DATA_FILE = DESIGN / "enemies" / "ground" / "ravager" / "data.yaml"
DATA = yaml.safe_load(_DATA_FILE.read_text(encoding="utf-8")) if _DATA_FILE.exists() else {}
_WALK = (DATA.get("movement") or {}).get("walk") or {}
STEP = 60                                       # game steps per second

SIZE = tuple(DATA.get("size", (56, 56)))        # 56x56
EXTENT = 2.3                                    # model units across the sprite (the concept's)
PX = SIZE[0] / EXTENT                           # px per model unit (~24)
FIT = 0.95                                      # the model's scale: the concept's jaw tips and tail tip ran over the edge
SHIFT = 0.1                                     # model units the beast sits back, so jaws and tail fit alike
HEADINGS = 16
GALLOP_FRAMES = 8
STRIDE_PX = _WALK.get("stride", 46)             # px of ground per gallop cycle (the concept's RAV_STRIDE)
SPEED = _WALK.get("speed", 160)                 # px/s galloping (review loop)
COLOURS = 32
GLOW_COLOURS = 16
# Readability on the night city (Level 08's capture: dark navy streets): the rust lifted a little,
# the near-black legs and tail bands mixed toward the rust, the Creeper's light rim
BODY_LIFT = 1.25                                # the rust chitin's albedo scale
DARK_LIFT = 0.35                                # the dark sinew mixed this far toward the rust
GLOW_GAIN = 1.0

LEAP_SIZE = (80, 80)                            # 1.43 x 56
LEAP_STEPS = 4
LEAP_SCALES = tuple(1 + 0.43 * (s + 1) / LEAP_STEPS for s in range(LEAP_STEPS))   # 1.11 .. 1.43
LEAP_T = 0.75                                   # s, the pounce (stat block)

DEATH_SIZE = (80, 80)
DEATH_FRAMES = 10
DEATH_STEPS = 4                                 # EnemyLooks.DEATH_FRAME_TICKS
DEATH_COLOURS = 32
DEATH_EXTENT = EXTENT * DEATH_SIZE[0] / SIZE[0]
TEAL = np.array(em.hx(em.GLOWS["teal"])) * 255


# =========================================================================== model

def lifted_mats(glow=1.0):
    """The round-05 rust scheme lifted for the night city."""
    mats = bm.ravager(0.0)[1]
    mid = np.array(mats[V_BODY].albedo)
    for slot in (V_BODY, 2, 6):
        mats[slot] = replace(mats[slot], albedo=tuple(np.minimum(1, np.array(mats[slot].albedo) * BODY_LIFT)))
    dark = np.array(mats[V_DARK].albedo)
    mats[V_DARK] = replace(mats[V_DARK], albedo=tuple(dark + (mid - dark) * DARK_LIFT))
    if glow != 1.0:
        mats = [replace(m, emission=tuple(np.array(m.emission) * glow)) for m in mats]
    return mats


def ravager_mats(kind):
    mats = lifted_mats()
    if kind == "glow":                                        # only the maw's and the eyes' emission
        black = Material((0.0, 0.0, 0.0), metal=0.0, shininess=1.0, spec=0.0)
        return [replace(m, albedo=(0.0, 0.0, 0.0), metal=0.0, spec=0.0, pattern=None,
                        emission=tuple(np.array(m.emission) * GLOW_GAIN))
                if i in (V_GLOW, V_EYE) else black for i, m in enumerate(mats)]
    return mats


def rotation(k):
    return np.pi - TAU * k / HEADINGS                        # +Y forward -> heading k, as creeper.py


def model(k, j, pose="run", kind="unit", scale=1.0):
    """(scene, mats) at heading k, gallop frame j (or the leap pose), drawn ``scale`` x bigger."""
    rot = rotation(k)
    base = bm.ravager(TAU * j / GALLOP_FRAMES, pose)[0]

    def scene(p):
        q = rotate_z(p, rot)
        q[:, 1] += SHIFT * scale
        d, m = base(q / (scale * FIT))
        return d * scale * FIT, m
    return scene, model_space_materials(ravager_mats(kind), rot)


def render_unit(k, j, kind, pose="run", step=0):
    if pose == "leap":
        scale = LEAP_SCALES[step]
        size, extent = LEAP_SIZE, EXTENT * LEAP_SIZE[0] / SIZE[0]
    else:
        scale, size, extent = 1.0, SIZE, EXTENT
    img = artkit.native(*artkit.render_hi(*model(k, j, pose, kind, scale), size, extent))
    if kind == "glow":
        return artkit.additive(img)
    return creeper.light_rim(img)


# =========================================================================== death

def death_glow(i):
    """The maw bursting: a teal-white flash, a teal bloom, teal ichor flecks, a rust-brown mist."""
    w, h = DEATH_SIZE
    t = fx.t_of(i, DEATH_FRAMES)
    rng = np.random.default_rng(3141)
    c = w / 2
    cv = v8.Canvas(w, h)
    d = cv.dist(c, c)
    fade = np.clip((w / 2 - d) / 4.0, 0, 1)
    tex = v8._cart_noise(w, 3142, cell=w * v8.SS // 5)
    churn = v8._shift(tex, t * w * 0.2, -t * w * 0.15)
    haze = np.clip(v8.gauss(d, 7 + 12 * fx.ease_out(t, 2.2)) * (0.5 + 0.9 * churn) - 0.2 - 0.6 * t, 0, 1)
    env = np.clip((i + 1) / 2.0, 0.4, 1) * (1 - t) ** 0.8
    cv.add((70, 30, 18), haze * 1.2 * env * fade)                     # the mist's rust-brown body
    cv.add(TEAL, np.clip(haze - 0.5, 0, 1) * 0.5 * env * fade)
    cv.add((225, 255, 245), v8.gauss(d, 3 + 5 * t) * 1.4 * np.clip(1 - t * 2.6, 0, 1))
    cv.add(TEAL, v8.gauss(d, 6 + 8 * fx.ease_out(t)) * 0.6 * (1 - t) ** 1.5 * fade)
    for _ in range(26):                                              # ichor flecks with short trails
        a, reach, size, delay = rng.uniform(0, TAU), rng.uniform(10, 24), rng.uniform(0.5, 1.2), rng.uniform(0, 0.15)
        if t < delay:
            continue
        tt = np.clip((t - delay) / (1 - delay), 0, 1)

        def at(u, a=a, reach=reach):
            r = 3 + reach * fx.ease_out(u, 2.6)
            return c + np.cos(a) * r, c + np.sin(a) * r + 1.5 * u * u
        x, y = at(tt)
        xp, yp = at(max(tt - 0.12, 0.0))
        life = np.clip(1.1 - tt * rng.uniform(0.9, 1.3), 0, 1)
        cv.add(TEAL, v8.gauss(cv.seg(xp, yp, x, y), size * 0.8) * 0.8 * life * fade)
    vd.motes(cv, rng, c, t, 8, (10, 22), (0.5, 0.9), TEAL, (230, 255, 248), fade)
    return artkit.additive(cv.image())


def death_pieces():
    """The four legs torn off at the shoulders and hips, the tail in three pieces, the two jaws,
    chitin flakes off the back; every piece a list of (geometry, material slot)."""
    rng = np.random.default_rng(3151)
    chest_y, hip_y = 0.35, -0.35
    pieces = []
    for sx, sy0, front in ((1, chest_y, True), (-1, chest_y, True), (1, hip_y, False), (-1, hip_y, False)):
        fy = 0.3 if front else -0.3
        sh = (sx * 0.24, sy0, 0.12)
        knee = (sx * 0.44, sy0 + fy * 0.5 + (0.05 if front else -0.1), 0.22)
        foot = (sx * 0.4, sy0 + fy, 0.0)
        r_up = 0.085 if front else 0.11

        def upper(q, sh=sh, knee=knee, r_up=r_up):
            return sd_capsule(q, sh, knee, r_up, 0.065)

        def lower(q, knee=knee, foot=foot):
            return union((sd_capsule(q, knee, foot, 0.055, 0.03), 0),
                         *[(sd_capsule(q, foot, (foot[0] + dx, foot[1] + 0.09, foot[2] - 0.02), 0.025, 0.008), 0)
                           for dx in (-0.04, 0.04)], k=0.03)[0]
        c0 = ((sh[0] + foot[0]) / 2, (sh[1] + foot[1]) / 2, 0.1)
        pieces.append(dict(parts=[(upper, V_DARK), (lower, V_BONE)], c0=c0, dir=vd.outward(rng, c0, 0.4),
                           reach=rng.uniform(0.45, 0.75), ease=3.0, axis=rng.uniform(0, TAU),
                           tumble=rng.uniform(3, 6) * rng.choice([-1, 1]), spin=rng.uniform(-3, 3),
                           rise=0.3, shrink_at=0.65))
    tail = [(0.0, hip_y - 0.16 - 0.11 * i, 0.15 - 0.015 * i) for i in range(7)]
    for a, b in ((0, 2), (2, 4), (4, 6)):                     # the tail in three pieces
        def seg(q, a=a, b=b):
            return union(*[(sd_capsule(q, tail[i], tail[i + 1], 0.11 - 0.018 * i, 0.095 - 0.018 * i), 0)
                           for i in range(a, min(b, 5))] or [(sd_capsule(q, tail[a], tail[b], 0.03, 0.02), 0)],
                         k=0.04)[0]
        c0 = (rng.uniform(-0.05, 0.05), (tail[a][1] + tail[b][1]) / 2, 0.12)
        pieces.append(dict(parts=[(seg, V_BODY if a % 4 == 0 else V_DARK)], c0=c0,
                           dir=(float(rng.uniform(-0.6, 0.6)), -1.0), reach=rng.uniform(0.15, 0.35), ease=2.6,
                           axis=rng.uniform(0, TAU), tumble=rng.uniform(2, 4) * rng.choice([-1, 1]),
                           spin=rng.uniform(-3, 3), rise=0.25, shrink_at=0.65))
    hy = chest_y + 0.55
    for side in (1, -1):                                      # the jaws, snapped off at the hinge
        def jaw(q, side=side):
            r = q.copy()
            r[:, 0] *= side
            return sd_capsule(r, (0.08, hy + 0.02, 0.1), (0.1, hy + 0.36, 0.08), 0.07, 0.028)
        c0 = (side * 0.09, hy + 0.2, 0.1)
        pieces.append(dict(parts=[(jaw, V_BONE)], c0=c0, dir=vd.outward(rng, c0, 0.3),
                           reach=rng.uniform(0.2, 0.4), ease=3.0, axis=rng.uniform(0, TAU),
                           tumble=rng.uniform(4, 7) * rng.choice([-1, 1]), spin=rng.uniform(-4, 4),
                           rise=0.3, shrink_at=0.65))
    for k in range(5):                                        # chitin flakes off the back
        cy = chest_y + 0.2 - 0.2 * k
        cx = (1 if k % 2 else -1) * rng.uniform(0.04, 0.12)
        poly = fx.jagged(rng, rng.uniform(0.13, 0.17), 6)

        def flake(q, cx=cx, cy=cy, poly=poly):
            r = q.copy()
            r[:, 0] -= cx
            r[:, 1] -= cy
            r[:, 2] -= 0.3 - 2.0 * (r[:, 0] ** 2 + r[:, 1] ** 2)
            return sd_plate(r, poly, 0.0, 0.03, 0.01)
        c0 = (cx, cy, 0.3)
        pieces.append(dict(parts=[(flake, V_BODY)], c0=c0, dir=vd.outward(rng, c0, 0.5),
                           reach=rng.uniform(0.5, 0.9), ease=2.6, axis=rng.uniform(0, TAU),
                           tumble=rng.uniform(4, 7) * rng.choice([-1, 1]), spin=rng.uniform(-3, 3),
                           rise=0.4, shrink_at=0.6))
    pieces.append(dict(parts=[(lambda q: sd_ellipsoid(q, (0.0, hy - 0.02, 0.17), (0.15, 0.17, 0.11)), V_BODY)],
                       c0=(0.0, hy, 0.17), dir=(0.0, 1.0), reach=0.25, ease=3.0, axis=rng.uniform(0, TAU),
                       tumble=2.5, spin=1.5, rise=0.2, shrink_at=0.6))   # the skull
    for cy, half, d in ((chest_y, (0.24, 0.26, 0.18), 1.0), (hip_y, (0.2, 0.24, 0.14), -1.0)):   # the torso split
        def chunk(q, cy=cy, half=half):
            return sd_ellipsoid(q, (0.0, cy, 0.15), half)
        pieces.append(dict(parts=[(chunk, V_BODY)], c0=(0.0, cy, 0.15), dir=(float(rng.uniform(-0.3, 0.3)), d),
                           reach=0.18, ease=3.0, axis=rng.uniform(0, TAU), tumble=1.2 * rng.choice([-1, 1]),
                           spin=rng.uniform(-1, 1), rise=0.1, shrink_at=0.15, least=0.1))
    return pieces


def death_tatters(i):
    # built facing +Y like the runner: heading 0 (down the screen) is a half turn
    scene, mats = creeper.pieces_scene(death_pieces(), lifted_mats(0.3), fx.t_of(i, DEATH_FRAMES),
                                       lambda p: rotate_z(p, np.pi))
    return artkit.native(*artkit.render_hi(scene, mats, DEATH_SIZE, DEATH_EXTENT))


# =========================================================================== build

def _job(job):
    name, args = job
    return {"unit": render_unit, "death": death_glow, "tatters": death_tatters}[name](*args)


def build():
    run_ = [(k, j) for k in range(HEADINGS) for j in range(GALLOP_FRAMES)]   # heading * 8 + frame
    leap = [(k, s) for k in range(HEADINGS) for s in range(LEAP_STEPS)]       # heading * 4 + step
    with ProcessPoolExecutor() as pool:
        def run(name, arglist):
            return list(pool.map(_job, [(name, a) for a in arglist]))
        sets = [("ravager", run("unit", [(k, j, "unit") for k, j in run_]), COLOURS),
                ("ravager-glow", run("unit", [(k, j, "glow") for k, j in run_]), GLOW_COLOURS),
                ("ravager-leap", run("unit", [(k, 0, "unit", "leap", s) for k, s in leap]), COLOURS),
                ("ravager-leap-glow", run("unit", [(k, 0, "glow", "leap", s) for k, s in leap]), GLOW_COLOURS),
                ("ravager-death", run("death", [(i,) for i in range(DEATH_FRAMES)]), DEATH_COLOURS),
                ("ravager-tatters", run("tatters", [(i,) for i in range(DEATH_FRAMES)]), DEATH_COLOURS)]
    # the gallop and the leap share one palette, so the beast keeps its colours as it takes off
    body = artkit.quantize_set(sets[0][1] + sets[2][1], COLOURS)
    glow = artkit.quantize_set(sets[1][1] + sets[3][1], GLOW_COLOURS)
    n = len(run_)
    out = {"ravager": body[:n], "ravager-leap": body[n:], "ravager-glow": glow[:n], "ravager-leap-glow": glow[n:]}
    for name, frames, colours in sets:
        frames = out.get(name) or artkit.quantize_set(frames, colours)
        artkit.write_frames(name, frames, SOURCE)
        print(f"{name}: {len(frames)} frames, {frames[0].width}x{frames[0].height}")


# =========================================================================== review

def night_plate(w, h):
    """A crop of Level 08's avenues (the night city the Ravagers run in)."""
    src = Image.open(ROOT / "assets" / "backdrop" / "level-08" / "avenues.png").convert("RGBA")
    return src.crop((120, 300, 120 + w, 300 + h))


def leap_state(u):
    """The lift (0..1) and the leap step at fraction u of the pounce, or None while it gallops."""
    lift = float(np.sin(np.pi * np.clip(u, 0, 1)))
    if lift < 0.125:
        return lift, None
    return lift, int(np.clip(round(lift * LEAP_STEPS) - 1, 0, LEAP_STEPS - 1))


def shadow(frame, scale, opacity=0.5):
    """The game's drop shadow: the frame's solid silhouette, flat dark, at 85 % x ``scale``."""
    a = np.array(frame)[..., 3] > 127
    img = Image.fromarray(np.dstack([np.zeros(a.shape + (3,), np.uint8), (a * int(255 * opacity)).astype(np.uint8)]),
                          "RGBA")
    s = 0.85 * scale
    return img.resize((max(1, round(img.width * s)), max(1, round(img.height * s))), Image.NEAREST)


def review():
    frames = artkit.load_frames("ravager")
    glow = artkit.load_frames("ravager-glow")
    leap = artkit.load_frames("ravager-leap")
    leap_glow = artkit.load_frames("ravager-leap-glow")
    tatters = artkit.load_frames("ravager-tatters")
    burst = artkit.load_frames("ravager-death")
    boom = artkit.load_frames("explosion-small")
    heading = [frames[k * GALLOP_FRAMES:(k + 1) * GALLOP_FRAMES] for k in range(HEADINGS)]
    glows = [glow[k * GALLOP_FRAMES:(k + 1) * GALLOP_FRAMES] for k in range(HEADINGS)]
    leaps = [leap[k * LEAP_STEPS:(k + 1) * LEAP_STEPS] for k in range(HEADINGS)]
    leap_glows = [leap_glow[k * LEAP_STEPS:(k + 1) * LEAP_STEPS] for k in range(HEADINGS)]
    # the pounce as the game draws it: ground frame, then the steps with the shadow pushed away
    pounce = []
    for lift, frame, scale in [(0.0, heading[0][0], 1.0)] + [((s + 1) / LEAP_STEPS, leaps[0][s], LEAP_SCALES[s])
                                                             for s in range(LEAP_STEPS)]:
        cell = night_plate(110, 110)
        sh = shadow(frame, 1 / (0.85 * scale))
        if lift > 0:
            cell.alpha_composite(sh, (int(55 + 21 * lift - sh.width / 2), int(55 + 30 * lift - sh.height / 2)))
        sprite.paste_center(cell, frame, 55, 55)
        pounce.append(cell)
    layers = [(tatters, DEATH_STEPS, False, 0), (boom, 2, True, 0), (burst, DEATH_STEPS, True, 0)]
    together = fx.composite_strip(layers, DEATH_FRAMES * DEATH_STEPS, DEATH_SIZE, 1)
    sheet = artkit.review_sheet("RAVAGER - FINAL SPRITES (PROPOSAL)", [
        ("HEADINGS 0-7 (CLOCKWISE FROM DOWN: 4 RUNS LEFT), GALLOP FRAME 0", [h[0] for h in heading[:8]], 3, False),
        ("HEADINGS 8-15 (12 RUNS RIGHT)", [h[0] for h in heading[8:]], 3, False),
        (f"GALLOP, HEADING 0 (DOWN), {STRIDE_PX} PX PER CYCLE", heading[0], 3, False),
        ("GALLOP, HEADING 4 (LEFT)", heading[4], 3, False),
        ("GALLOP, HEADING 10", heading[10], 3, False),
        ("TEAL MAW AND EYE GLOW MASKS, HEADING 4 (ADDITIVE)", glows[4], 3, True),
        ("LEAP STEPS 0-3, HEADING 0: 1.11 / 1.21 / 1.32 / 1.43 X; HEADING 6", leaps[0] + leaps[6], 2, False),
        ("LEAP GLOW MASKS, HEADING 0 (ADDITIVE)", leap_glows[0], 2, True),
        ("POUNCE AS DRAWN: GROUND, THEN LIFT 0.25-1 WITH THE SHADOW (21, 30) X LIFT AWAY", pounce, 2, False),
        (f"TATTERS, {STEP // DEATH_STEPS} FPS, SOLID", tatters, 2, False),
        (f"DEATH GLOW, {STEP // DEATH_STEPS} FPS, ADDITIVE", burst, 2, True),
        ("TOGETHER WITH THE SMALL BURST, EVERY 2ND STEP; PIECES UNDER THE GLOWS", together[::2], 2, False),
        ("1X", [h[0] for h in heading] + leaps[0], 1, False)], width=1700, batch=BATCH)
    # the loop: a pack of four gallops across a night street, the lead pounces at a point and runs on,
    # then it is shot and bursts
    fw, fh, fps = 260, 300, 30
    plate = night_plate(fw, fh)
    paths = [creeper.smooth_path([(-40, 40 + 34 * u), (90, 70 + 30 * u), (170, 150 + 20 * u), (300, 210 + 26 * u)])
             for u in range(4)]
    target = (110, 215)                                       # where the ship was at take-off
    lead_take = 150.0                                         # px of path where the lead pounces
    total = paths[0][1][-1]
    n = int(fps * 4.0)
    gif = []
    lead_dist = 0.0
    landed = None
    die_step = int(2.2 * STEP)
    for f in range(n):
        step = f * STEP // fps
        sec = step / STEP
        cell = plate.copy()
        lights = []
        drawn = []
        for u in range(1, 4):                                 # the pack behind the lead, 0.25 s apart
            s = (sec - 0.25 * u) * SPEED
            if 0 <= s <= total:
                pos, dv = creeper.along(paths[u], s)
                k = creeper.heading_of(*dv)
                j = int(np.floor(s / STRIDE_PX * GALLOP_FRAMES)) % GALLOP_FRAMES
                drawn.append((heading[k][j], glows[k][j], pos, None))
        s0 = sec * SPEED
        t_take = lead_take / SPEED
        if step < die_step:
            if s0 < lead_take:
                pos, dv = creeper.along(paths[0], s0)
                k = creeper.heading_of(*dv)
                lead_dist = s0
                j = int(np.floor(lead_dist / STRIDE_PX * GALLOP_FRAMES)) % GALLOP_FRAMES
                drawn.append((heading[k][j], glows[k][j], pos, None))
            elif sec < t_take + LEAP_T:
                start, _ = creeper.along(paths[0], lead_take)
                u = (sec - t_take) / LEAP_T
                pos = start + (np.array(target) - start) * u
                k = creeper.heading_of(*(np.array(target) - start))
                lift, st = leap_state(u)
                if st is None:
                    j = int(np.floor((lead_dist + np.hypot(*(pos - start))) / STRIDE_PX * GALLOP_FRAMES)) % GALLOP_FRAMES
                    drawn.append((heading[k][j], glows[k][j], pos, None))
                else:
                    drawn.append((leaps[k][st], leap_glows[k][st], pos, (lift, LEAP_SCALES[st])))
            else:
                if landed is None:
                    landed = lead_dist + np.hypot(*(np.array(target) - creeper.along(paths[0], lead_take)[0]))
                run = (sec - t_take - LEAP_T) * SPEED
                pos = np.array(target) + np.array((0.9, 0.44)) * run      # runs on, down and right
                k = creeper.heading_of(0.9, 0.44)
                j = int(np.floor((landed + run) / STRIDE_PX * GALLOP_FRAMES)) % GALLOP_FRAMES
                drawn.append((heading[k][j], glows[k][j], pos, None))
                lead_pos = pos
        else:
            age = step - die_step
            x, y = lead_pos
            for frames_, steps, glowing, delay in layers:
                fr = fx.timed(frames_, steps, age - delay)
                if fr is None:
                    continue
                xy = (int(round(x - fr.width / 2)), int(round(y - fr.height / 2)))
                if glowing:
                    lights.append((fr, xy))
                else:
                    cell.alpha_composite(fr, xy)
        drawn.sort(key=lambda d: d[3] is not None)            # the leaper on top
        for fr, gl, (x, y), leap_ in drawn:
            if leap_ is not None:
                lift, scale = leap_
                sh = shadow(fr, 1 / (0.85 * scale))
                cell.alpha_composite(sh, (int(round(x + 21 * lift - sh.width / 2)), int(round(y + 30 * lift - sh.height / 2))))
            sprite.paste_center(cell, fr, x, y)
            lights.append((gl, (int(round(x - gl.width / 2)), int(round(y - gl.height / 2)))))
        for fr, xy in lights:
            cell = artkit.add_light(cell, fr, xy)
        gif.append(sprite.enlarge(cell, 2))
    artkit.save_review(sheet, gif, CONCEPT, "ravager", fps=fps)


if __name__ == "__main__":
    artkit.REVIEW_ROUND = REVIEW_ROUND
    if "--review" not in sys.argv[1:]:
        build()
    review()
