#!/usr/bin/env python3
"""Production art PROPOSAL: the two new Vrell units of Level 04, the Brood Pod and the Scuttler
(design/enemies/air/brood-pod, design/enemies/ground/scuttler; M4 part D batch, concept round 16).
Pending part D's doc gaps: the sizes, frame counts, timings and names below are the ones asked for
while the part's design notes are still open; they all sit in the parameter block at the top.

Outputs (assets/sprites/):
  brood-pod_0..7.png           64x64, `orientation: fixed`, air layer: the pulse loop, the egg sac
                               swelling over five frames and contracting over three, its teal veins
                               and eye brightening with the swell (10 fps; the game plays it faster
                               over the last 3 s before it bursts on its own)
  brood-pod-burst_0..11.png    96x96, additive (premultiplied on black): the wet burst, a teal-white
                               flash, a ring of fluid, teal ichor globules with short trails and a
                               dark teal mist that billows and thins (4 game steps per frame)
  brood-pod-tatters_0..11.png  96x96, solid: ten torn sac membrane pieces (the sac's hide with its
                               glowing teal veins) and six rib shards flung out, tumbling, curling,
                               their veins dimming and the pieces shrivelling (4 steps per frame)
  scuttler_0..95.png           64x64, `16 angles`, ground layer: 16 headings x 6 walk frames,
                               indexed heading * 6 + frame (the game's EnemyLooks.frame); heading k
                               walks k x 22.5 degrees clockwise from straight down (heading 0 walks
                               down the screen, heading 4 to the left); one walk cycle = 24 px
  scuttler-husk_0..15.png      64x64: the legless, scorched shell at each heading (its remains)
  scuttler-glow_0..95.png      64x64, additive: the lime back's emission alone, in the same frame
                               order, for drawing above the dust banks
  design/enemies/air/brood-pod/concept/brood-pod-final-r16-a.png/.gif
  design/enemies/ground/scuttler/concept/scuttler-final-r16-a.png/.gif

The Brood Pod is the chosen round-04 model (tools/concept/enemies_r04.py `brood-pod-a`,
render/enemy_models.brood_pod_a, whose `anim` swells the sac by +-7 % and whose `glow` scales the
veins and eye), rendered at the stat block's 64 px instead of the concept's 48. Its burst glow is a
2D light field like the spore cloud (vfx_r08's canvas); the tatters are ray-marched per frame from
the pod's own materials the way tools/art/vrell_fx.py makes the Spore Bomber's (its torn outline
helper is reused), each pattern evaluated in the piece's frame so it turns with the piece.

The Scuttler is the chosen round-08 re-render of the round-05 walker (tools/concept/rerender_r08.py,
render/archetype_models.scuttler, built facing +Y): its geometry is copied here so the leg swing can
follow the stride (the concept's +-0.5 rad swing would carry the body ~46 px per cycle; at 24 px the
middle feet stay planted with a swing of ~0.25 rad). Like round 08, the material patterns turn with
the body (enemy_rigs.model_space_materials). Every heading is its own render under the fixed
top-left key light; nothing lit is rotated or mirrored as an image.

Run: python3 tools/art/vrell_l04.py [pod] [scuttler] [--review]   (~1 min)
"""
import sys
from concurrent.futures import ProcessPoolExecutor
from dataclasses import replace

import numpy as np
from PIL import Image

import artkit
from artkit import DESIGN, TAU, sprite

import enemies_r04 as e4  # noqa: E402  (concept script, imported unchanged)
import vfx_r08 as v8  # noqa: E402
import vrell_fx as fx  # noqa: E402  (tools/art: the torn outline, easing and timing helpers)
import vrell_l03 as l03  # noqa: E402  (tools/art: patterns in a part's own frame)
from render import archetype_models  # noqa: E402,F401  (adds the Scuttler's role colours)
from render import enemy_models as em  # noqa: E402
from render.enemy_rigs import model_space_materials  # noqa: E402
from render.sdf import Material, mirror_x, rotate_x, rotate_z, sd_capsule, sd_ellipsoid, sd_plate, sd_sphere, union  # noqa: E402,E501

# =========================================================================== parameters
# (all pending part D's doc gaps)

SCRIPT = "vrell_l04.py"
BATCH = "M4 part D batch (proposal)"
SOURCE = artkit.source_note(SCRIPT, BATCH)
REVIEW_ROUND = "r16"
STEP = 60                                       # game steps per second
ORGANIC_FPS = 10                                # the game's Vrell animation rate

# Brood Pod
POD_SIZE = (64, 64)                             # the stat block (the concept was 48)
POD_EXTENT = 2.3
POD_ANIM = [-1.0, -0.55, 0.0, 0.5, 1.0, 0.45, -0.25, -0.8]   # swell over 5 frames, contract over 3
POD_GLOW = [0.7 + 0.35 * (a + 1) for a in POD_ANIM]          # veins brightest at the full swell
POD_COLOURS = 32
POD_TELEGRAPH_SPEEDUP = 2.0                     # review only: the last 3 s play this much faster
BURST_SIZE = (96, 96)
BURST_FRAMES = 12
BURST_STEPS = 4                                 # game steps per frame (EnemyLooks' DEATH_FRAME_TICKS)
BURST_COLOURS = 32
TATTER_FRAMES = 12
TATTER_COLOURS = 32
TATTER_HIDE = 10                                # sac membrane pieces
TATTER_RIBS = 6                                 # rib shards (the model has six ribs)
SAC_R = 0.72                                    # the sac's radius in model units (brood_pod_a)
TEAL = np.array(em.hx(em.GLOWS["teal"])) * 255

# Scuttler
SCUT_SIZE = (64, 64)
SCUT_EXTENT = 2.3
HEADINGS = 16
WALK_FRAMES = 6
STRIDE_PX = 24                                  # body travel per walk cycle (40 px/s at 10 fps)
LEG_BASE = [(0.22, 0.65), (0.0, 0.0), (-0.22, -0.65)]   # (hip y, outward angle), archetype_models
LEG_REACH = 0.86                                # hip-to-foot reach in model units
LEG_LIFT = 0.16
# In a tripod gait each foot pushes back for half the cycle while the body moves on: a middle foot
# that stays planted travels STRIDE / 2 per stance, 2 x reach x sin(swing).
SWING = float(np.arcsin(STRIDE_PX * SCUT_EXTENT / SCUT_SIZE[0] / (4 * LEG_REACH)))
SCUT_COLOURS = 32
HUSK_GLOW = 0.12                                # the back's glow on the dead shell
HUSK_TONE = 0.72                                # scorched: albedo scale
HUSK_COLOURS = 24
GLOW_GAIN = 1.0                                 # the back's emission in the additive masks
GLOW_COLOURS = 16


# =========================================================================== Brood Pod

def pod_model(anim, glow):
    return e4.R04["brood-pod-a"][4](anim=anim, glow=glow)


def render_pod(k):
    scene, mats = pod_model(POD_ANIM[k], POD_GLOW[k])
    return artkit.native(*artkit.render_hi(scene, mats, POD_SIZE, POD_EXTENT))


TATTER_EXTENT = POD_EXTENT * BURST_SIZE[0] / POD_SIZE[0]     # the pod's model scale on 96 px
PX = BURST_SIZE[0] / TATTER_EXTENT                            # px per model unit


def pod_burst(i):
    """Frame i of the wet burst: a teal-white flash, a ring of fluid, ichor globules with short
    trails sagging a little, a dark teal mist that billows and thins."""
    w, h = BURST_SIZE
    t = fx.t_of(i, BURST_FRAMES)
    rng = np.random.default_rng(1641)
    cx, cy = w / 2, h / 2
    sac = SAC_R * PX                                          # ~20 px
    cv = v8.Canvas(w, h)
    d = cv.dist(cx, cy)
    fade = np.clip((w / 2 - d) / 4.0, 0, 1)                   # nothing reaches the frame edge
    tex = v8._cart_noise(w, 1642, cell=w * v8.SS // 6)
    churn = v8._shift(tex, t * w * 0.3, -t * w * 0.2)
    grow = fx.ease_out(t, 2.4)
    dens = v8.gauss(d, sac * (1.0 + 1.1 * grow))
    haze = np.clip(dens * (0.5 + 0.9 * churn) - 0.2 - 0.45 * t, 0, 1)
    env = np.clip((i + 1) / 2.0, 0.4, 1) * (1 - t) ** 0.9
    cv.add((0, 80, 60), haze * 1.15 * env * fade)             # the mist's dark body
    cv.add(TEAL, np.clip(haze - 0.45, 0, 1) * 0.7 * env * fade)
    flash = max(0.0, 1 - i / 3.0)
    cv.add((210, 255, 235), v8.gauss(d, sac * (0.55 + 0.5 * t)) * 1.3 * flash)
    cv.add(TEAL, v8.gauss(d, sac * (1.0 + 0.6 * t)) * 0.8 * flash)
    ring = sac * (1.0 + 0.8 * grow)
    cv.add(TEAL, v8.gauss(np.abs(d - ring), 1.0 + 2.0 * t) * (0.55 + 0.45 * churn) * 0.7 * (1 - t) ** 1.5 * fade)
    drops = [(rng.uniform(0, TAU), rng.uniform(0.7, 1.0), rng.uniform(0.5, 1.15), rng.uniform(0.5, 1.3),
              rng.uniform(0, 0.15), rng.uniform(0.8, 1.3)) for _ in range(56)]
    for a, start, reach, size, delay, decay in drops:
        if t < delay:
            continue
        tt = np.clip((t - delay) / (1 - delay), 0, 1)

        def at(u):
            r = sac * start + sac * 1.1 * reach * fx.ease_out(u, 2.5)
            return cx + np.cos(a) * r, cy + np.sin(a) * r + 3.0 * u * u   # sagging a little
        x, y = at(tt)
        xp, yp = at(max(tt - 0.12, 0.0))
        life = np.clip(1.1 - tt * decay, 0, 1)
        cv.add(TEAL, v8.gauss(cv.seg(xp, yp, x, y), size * 0.8) * 0.9 * life * fade)
        if size > 0.9:
            cv.add((200, 255, 230), v8.gauss(cv.dist(x, y), size * 0.6) * 1.1 * life * fade)
    return artkit.additive(cv.image())


def tatter_pieces():
    rng = np.random.default_rng(1651)
    pieces = []
    for k in range(TATTER_HIDE):                # sac membrane, torn from all round the sac
        a = TAU * k / TATTER_HIDE + rng.uniform(-0.25, 0.25)
        size = rng.uniform(0.16, 0.28)
        pieces.append(dict(kind="hide", a=a, start=rng.uniform(0.55, 0.9), reach=rng.uniform(0.35, 0.6),
                           poly=fx.jagged(rng, size), axis=rng.uniform(0, TAU),
                           tumble=rng.uniform(2.5, 6.0) * rng.choice([-1, 1]), spin=rng.uniform(-2.5, 2.5),
                           tilt0=rng.uniform(-0.5, 0.5), bend=rng.uniform(0.8, 1.6)))
    for k in range(TATTER_RIBS):                # the ribs, snapped into long shards
        a = TAU * k / TATTER_RIBS + rng.uniform(-0.15, 0.15)
        ln, wd = rng.uniform(0.18, 0.26), rng.uniform(0.045, 0.06)
        poly = [(-ln, -wd), (ln * 0.8, -wd * 0.7), (ln, wd * 0.2), (ln * 0.7, wd), (-ln * 0.9, wd * 0.8)]
        pieces.append(dict(kind="rib", a=a, start=0.85, reach=rng.uniform(0.45, 0.7), poly=poly,
                           axis=a + np.pi / 2 + rng.uniform(-0.4, 0.4),
                           tumble=rng.uniform(5, 9) * rng.choice([-1, 1]), spin=rng.uniform(-4, 4),
                           tilt0=0.0, bend=0.0))
    return pieces


def tatters_model(t):
    """The tatters at time t (0..1): each piece posed in the model, its pattern and veins evaluated
    in its own frame so they turn with it; the veins dim as the pieces die."""
    _, mats = pod_model(1.0, 1.25 * (1 - 0.8 * t))
    mats = list(mats)
    shrink = np.clip(1.0 - (t - 0.6) / 0.4 * 0.65, 0.35, 1.0)
    posed = []
    for pc in tatter_pieces():
        out = pc["reach"] * fx.ease_out(t, 2.0)
        r = SAC_R * pc["start"] + out
        cx, cy = np.cos(pc["a"]) * r, -np.sin(pc["a"]) * r   # screen y down = model y up
        ang, spin, axis = pc["tilt0"] + pc["tumble"] * t, pc["spin"] * t, pc["axis"]
        sc = shrink * (1.0 if pc["kind"] == "hide" else 0.5 + 0.5 * shrink)

        def local(p, cx=cx, cy=cy, axis=axis, ang=ang, spin=spin, sc=sc):
            q = p.copy()
            q[:, 0] -= cx
            q[:, 1] -= cy
            return rotate_z(rotate_x(rotate_z(q, axis), ang), spin - axis) / sc

        base = mats[em.V_SAC if pc["kind"] == "hide" else em.V_BODY]
        mats.append(replace(base, pattern=l03.in_frame(base.pattern, local) if base.pattern else None,
                            emission_pattern=l03.in_frame(base.emission_pattern, local)
                            if base.emission_pattern else None))
        posed.append((pc, local, sc, len(mats) - 1))

    def scene(p):
        items = []
        for pc, local, sc, m in posed:
            q = local(p)
            if pc["bend"]:                                    # a curled scrap of membrane
                q = q.copy()
                q[:, 2] -= pc["bend"] * (q[:, 0] ** 2 + q[:, 1] ** 2)
            thick = 0.02 if pc["kind"] == "hide" else 0.035
            items.append((sd_plate(q, pc["poly"], 0.0, thick, 0.008) * sc / 1.25, m))
        return union(*items)
    return scene, mats


def render_tatters(i):
    scene, mats = tatters_model(fx.t_of(i, TATTER_FRAMES))
    return artkit.native(*artkit.render_hi(scene, mats, BURST_SIZE, TATTER_EXTENT))


# =========================================================================== Scuttler

def scuttler_scene(phase, legs=True):
    """render/archetype_models.scuttler with the stride's leg swing; ``legs=False`` leaves only
    broken hip stubs (the husk). Built facing +Y."""
    def leg_items(p):
        items = []
        for i, (hy, a0) in enumerate(LEG_BASE):
            for side in (1, -1):
                group = (i + (0 if side > 0 else 1)) % 2       # tripod: L0 R1 L2 / R0 L1 R2
                ph = phase + (0.0 if group == 0 else np.pi)
                lift = LEG_LIFT * max(0.0, np.cos(ph)) if legs else 0.0
                ang = a0 + (SWING * np.sin(ph) if legs else 0.0)
                ox, oy = np.cos(ang), np.sin(ang)
                hip = (side * 0.34, hy, 0.08)
                knee = (side * (0.34 + 0.42 * ox), hy + 0.42 * oy, 0.34 + lift)
                if not legs:
                    stub = tuple(a + 0.35 * (b - a) for a, b in zip(hip, knee))
                    items.append((sd_capsule(p, hip, stub, 0.085, 0.075), em.V_DARK))
                    continue
                foot = (side * (0.34 + LEG_REACH * ox), hy + LEG_REACH * oy, -0.08 + lift)
                items += [(sd_capsule(p, hip, knee, 0.085, 0.07), em.V_BODY),
                          (sd_sphere(p, knee, 0.075), em.V_DARK),
                          (sd_capsule(p, knee, foot, 0.065, 0.02), em.V_BONE)]
        return items

    def scene(p):
        q = mirror_x(p)
        d, m = union(
            (sd_ellipsoid(p, (0, -0.05, 0.08), (0.44, 0.4, 0.17)), em.V_SEAM),
            (sd_ellipsoid(p, (0, 0.38, 0.05), (0.24, 0.17, 0.12)), em.V_BODY),
            (sd_ellipsoid(p, (0, -0.4, 0.04), (0.26, 0.16, 0.1)), em.V_DARK),
            k=0.05,
        )
        d, m = union((d, m), *leg_items(p), k=0.025)
        return union(
            (d, m),
            (sd_capsule(q, (0.1, 0.48, 0.05), (0.18, 0.7, 0.0), 0.05, 0.015), em.V_BONE),
            (sd_sphere(q, (0.12, 0.4, 0.15), 0.06), em.V_GLOW),
            (sd_sphere(p, (0, 0.3, 0.18), 0.055), em.V_EYE),
            k=0.02,
        )
    return scene


def scuttler_mats(kind):
    if kind == "husk":
        return [replace(m, albedo=tuple(np.array(m.albedo) * HUSK_TONE))
                for m in em.vrell_scheme_mats("scuttler", "a", HUSK_GLOW)]
    mats = em.vrell_scheme_mats("scuttler", "a", 1.0)
    if kind == "glow":                                        # only the back's emission
        black = Material((0.0, 0.0, 0.0), metal=0.0, shininess=1.0, spec=0.0)
        seam = mats[em.V_SEAM]
        return [replace(seam, albedo=(0.0, 0.0, 0.0), metal=0.0, spec=0.0, pattern=None,
                        emission=tuple(np.array(seam.emission) * GLOW_GAIN))
                if i == em.V_SEAM else black for i in range(len(mats))]
    return mats


def render_scuttler(k, j, kind):
    """Heading k (k x 22.5 degrees clockwise from down), walk frame j; kind unit, husk or glow."""
    rot = np.pi - TAU * k / HEADINGS                          # +Y forward -> heading k, as vrell_air
    base = scuttler_scene(TAU * j / WALK_FRAMES, legs=kind != "husk")
    scene = lambda p: base(rotate_z(p, rot))                   # noqa: E731
    mats = model_space_materials(scuttler_mats(kind), rot)
    img = artkit.native(*artkit.render_hi(scene, mats, SCUT_SIZE, SCUT_EXTENT))
    return artkit.additive(img) if kind == "glow" else img


# =========================================================================== build

def _job(job):
    name, args = job
    return {"pod": render_pod, "burst": pod_burst, "tatters": render_tatters, "scut": render_scuttler}[name](*args)


def build(unit):
    with ProcessPoolExecutor() as pool:
        def run(name, arglist):
            return list(pool.map(_job, [(name, a) for a in arglist]))
        if unit == "pod":
            artkit.write_frames("brood-pod", artkit.quantize_set(run("pod", [(k,) for k in range(len(POD_ANIM))]),
                                                                 POD_COLOURS), SOURCE)
            artkit.write_frames("brood-pod-burst", artkit.quantize_set(
                run("burst", [(i,) for i in range(BURST_FRAMES)]), BURST_COLOURS), SOURCE)
            artkit.write_frames("brood-pod-tatters", artkit.quantize_set(
                run("tatters", [(i,) for i in range(TATTER_FRAMES)]), TATTER_COLOURS), SOURCE)
            return
        walk = [(k, j) for k in range(HEADINGS) for j in range(WALK_FRAMES)]   # heading * 6 + frame
        artkit.write_frames("scuttler", artkit.quantize_set(
            run("scut", [(k, j, "unit") for k, j in walk]), SCUT_COLOURS), SOURCE)
        artkit.write_frames("scuttler-husk", artkit.quantize_set(
            run("scut", [(k, 0, "husk") for k in range(HEADINGS)]), HUSK_COLOURS), SOURCE)
        artkit.write_frames("scuttler-glow", artkit.quantize_set(
            run("scut", [(k, j, "glow") for k, j in walk]), GLOW_COLOURS), SOURCE)


# =========================================================================== review

def heading_of(dx, dy):
    """Heading index of a screen direction (y down), clockwise from straight down."""
    return int(round(np.arctan2(-dx, dy) / TAU * HEADINGS)) % HEADINGS


def review_pod():
    pod = artkit.load_frames("brood-pod")
    burst = artkit.load_frames("brood-pod-burst")
    tatters = artkit.load_frames("brood-pod-tatters")
    boom = artkit.load_frames("explosion-medium")
    skitter = artkit.load_frames("skitter")
    layers = [(tatters, BURST_STEPS, False, 0), (boom, 2, True, 0), (burst, BURST_STEPS, True, 0)]
    together = fx.composite_strip(layers, BURST_FRAMES * BURST_STEPS, BURST_SIZE, 1)
    sheet = artkit.review_sheet("BROOD POD - FINAL SPRITES (PROPOSAL)", [
        ("PULSE LOOP, 10 FPS (FASTER OVER THE LAST 3 S)", pod, 3, False),
        ("BURST, ADDITIVE, 15 FPS, EVERY 2ND FRAME", burst[::2], 3, True),
        ("TATTERS, SOLID, 15 FPS, EVERY 2ND FRAME", tatters[::2], 3, False),
        ("TOGETHER WITH THE MEDIUM BURST, EVERY 2ND STEP; TATTERS UNDER THE GLOWS", together, 2, False),
        ("1X", pod, 1, False), ("1X", burst, 1, True), ("1X", tatters, 1, False)], width=1800, batch=BATCH)
    fw, fh, fps = 170, 190, 30
    calm, fast, after = 120, 120, 90                          # steps: pulse, telegraph, burst
    x, y = fw / 2, 70                                         # the camera follows the pod
    gif = []
    for f in range((calm + fast + after) // 2):
        step = f * 2
        cell = Image.new("RGBA", (fw, fh), artkit.PLATE)
        if step < calm:
            sprite.paste_center(cell, pod[(step * ORGANIC_FPS // STEP) % len(pod)], x, y)
        elif step < calm + fast:
            k = int((calm * ORGANIC_FPS + (step - calm) * ORGANIC_FPS * POD_TELEGRAPH_SPEEDUP) // STEP)
            sprite.paste_center(cell, pod[k % len(pod)], x, y)
        age = step - calm - fast
        if age >= 0:
            for frames, steps, glow, delay in layers[:1]:
                fr = fx.timed(frames, steps, age - delay)
                if fr is not None:
                    cell.alpha_composite(fr, (int(x - fr.width / 2), int(y - fr.height / 2)))
            if age >= 4:                                      # six Skitters fan out over 120 degrees
                for s in range(6):
                    a = np.radians(30 + 120 * s / 5)          # screen angle, y down: toward the player
                    r = 10 + 110 * (age - 4) / STEP
                    dx, dy = np.cos(a), np.sin(a)
                    sprite.paste_center(cell, skitter[heading_of(dx, dy) * 6 + (age // 6 + s) % 6],
                                        x + dx * r, y + dy * r)
            for frames, steps, glow, delay in layers[1:]:
                fr = fx.timed(frames, steps, age - delay)
                if fr is not None:
                    cell = artkit.add_light(cell, fr, (int(x - fr.width / 2), int(y - fr.height / 2)))
        gif.append(sprite.enlarge(cell, 2))
    artkit.save_review(sheet, gif, DESIGN / "enemies" / "air" / "brood-pod" / "concept", "brood-pod", fps=fps)


def review_scuttler():
    frames = artkit.load_frames("scuttler")
    husk = artkit.load_frames("scuttler-husk")
    glow = artkit.load_frames("scuttler-glow")
    heading = [frames[k * WALK_FRAMES:(k + 1) * WALK_FRAMES] for k in range(HEADINGS)]
    glows = [glow[k * WALK_FRAMES:(k + 1) * WALK_FRAMES] for k in range(HEADINGS)]
    sheet = artkit.review_sheet("SCUTTLER - FINAL SPRITES (PROPOSAL)", [
        ("HEADINGS 0-7 (CLOCKWISE FROM DOWN: 4 WALKS LEFT), WALK FRAME 0", [h[0] for h in heading[:8]], 3, False),
        ("HEADINGS 8-15 (12 WALKS RIGHT)", [h[0] for h in heading[8:]], 3, False),
        ("WALK CYCLE, HEADING 0 (DOWN), 24 PX PER CYCLE", heading[0], 4, False),
        ("WALK CYCLE, HEADING 4 (LEFT)", heading[4], 4, False),
        ("WALK CYCLE, HEADING 10", heading[10], 4, False),
        ("HUSKS 0-7", husk[:8], 3, False), ("HUSKS 8-15", husk[8:], 3, False),
        ("LIME BACK GLOW MASKS, HEADING 4 (ADDITIVE)", glows[4], 4, True),
        ("1X", [h[0] for h in heading] + husk[::4], 1, False)], width=1700, batch=BATCH)
    size, fps = 170, 20
    radius = STRIDE_PX * 13 / TAU                             # a circle of 13 whole walk cycles
    n = int(round(STRIDE_PX * 13 / 40 * fps))                 # at 40 px/s: 7.8 s, a seamless loop
    gif = []
    for f in range(n):
        s = f / n                                             # fraction of the loop walked
        a = TAU * s
        x, y = size / 2 + radius * np.cos(a), size / 2 + radius * np.sin(a)
        k = heading_of(-np.sin(a), np.cos(a))
        j = int(s * 13 * WALK_FRAMES) % WALK_FRAMES           # the walk frame follows the distance
        cell = Image.new("RGBA", (size, size), (34, 38, 30, 255))
        sprite.paste_center(cell, heading[k][j], x, y)
        dust = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        dust.paste((120, 110, 88, 170), (0, 58, size, 112))   # a dust bank across the middle
        cell.alpha_composite(dust)
        cell = artkit.add_light(cell, glows[k][j], (int(round(x - 32)), int(round(y - 32))))
        gif.append(sprite.enlarge(cell, 3))
    artkit.save_review(sheet, gif, DESIGN / "enemies" / "ground" / "scuttler" / "concept", "scuttler", fps=fps)


REVIEWS = {"pod": review_pod, "scuttler": review_scuttler}

if __name__ == "__main__":
    artkit.REVIEW_ROUND = REVIEW_ROUND
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    for unit in args or list(REVIEWS):
        if "--review" not in sys.argv[1:]:
            build(unit)
        REVIEWS[unit]()
