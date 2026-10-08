#!/usr/bin/env python3
"""Production art: the Mote Swarm, Act 2's ember-like Vrell flock that loops back from behind
(design/enemies/air/mote-swarm; M5 part D batch, concept round 32). Straight to production from
its chosen concept (user decision D10 = a): the round-05 model mote-swarm-r05-a
(tools/concept/render/archetype_models.mote, imported unchanged).

Outputs (assets/sprites/):
  mote-swarm_0..47         16x16, `16 angles`: 16 headings x 3 glow-flicker frames, indexed
                           heading * 3 + frame (EnemyLooks.frame at 10 fps; each mote at its own
                           phase, so a flock never flickers in step). Heading k flies k x 22.5
                           degrees clockwise from straight down; one of the three bone spikes points
                           along the flight, so a flock reads as aligned, like starlings, while the
                           body stays radial. The crimson slit glows at the concept's three levels
                           (0.8, 1.1, 1.5). A faint 1 px light rim for the dawn sprawl.
  mote-swarm-death_0..7    24x24, additive: the `tiny` ember puff: a crimson-white pop, an orange
                           bloom and embers flung out and dying (EnemyLooks: tiny glows show 2 steps
                           a frame, 4 steps after the explosion-tiny pop)
  mote-swarm-tatters_0..7  24x24, solid: the three dark fins and the husk split in two, flung out,
                           tumbling, shrivelling (2 steps a frame, with the pop)
  design/enemies/air/mote-swarm/concept/mote-swarm-final-r32-a.png/.gif   (the GIF: a 20-mote
                           flock with separation, alignment and cohesion toward a leader route,
                           swirling in front, leaving the bottom edge, the warning, looping back up
                           through the player's lane)

How the game draws it: like any 16-angle unit (EnemyLooks.frame with the mote's facing and its
animation step); no glow mask, no remains. The flock's look is the sim's (D8): nothing more to
draw.

Run: python3 tools/art/mote_swarm.py [--review]   (~20 s; --review only rebuilds the review files)
"""
import sys
from concurrent.futures import ProcessPoolExecutor

import numpy as np

import artkit
from artkit import DESIGN, TAU, sprite

import creeper  # noqa: E402  (tools/art: the light rim, the pieces' scene)
import vfx_r08 as v8  # noqa: E402  (concept script, imported unchanged)
import vrell_deaths as vd  # noqa: E402  (tools/art: the death pieces' pose and the motes)
import vrell_fx as fx  # noqa: E402  (tools/art: easing, timing)
import wraith  # noqa: E402  (tools/art: the dawn stand-in plate and the review helpers)
from render import archetype_models as am  # noqa: E402
from render import enemy_models as em  # noqa: E402
from render.enemy_models import V_BODY, V_BONE, V_DARK  # noqa: E402
from render.enemy_rigs import model_space_materials  # noqa: E402
from render.sdf import rotate_z, sd_capsule, sd_plate, sd_sphere  # noqa: E402

# =========================================================================== parameters

SCRIPT = "mote_swarm.py"
BATCH = "M5 part D batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
REVIEW_ROUND = "r32"
CONCEPT = DESIGN / "enemies" / "air" / "mote-swarm" / "concept"
STEP = 60

SIZE = (16, 16)                                 # the stat block's 16x16
EXTENT = 2.3                                    # the concept's
HEADINGS = 16
GLOWS = (0.8, 1.1, 1.5)                         # the concept's three-frame flicker
FPS = 10
COLOURS = 24
RIM_STRENGTH = 0.5
SPIKE = 1.05                                    # archetype_models.mote: the first spike's angle

DEATH_SIZE = (24, 24)
DEATH_FRAMES = 8
DEATH_STEPS = 2                                 # EnemyLooks.TINY_DEATH_FRAME_TICKS
GLOW_DELAY = 4                                  # EnemyLooks.TINY_GLOW_DELAY_TICKS
DEATH_COLOURS = 24
DEATH_EXTENT = EXTENT * DEATH_SIZE[0] / SIZE[0]
CRIMSON = np.array(em.hx(em.GLOWS["crimson"])) * 255 if "crimson" in em.GLOWS else np.array([255, 48, 56], float)
EMBER = np.array([255, 140, 60], float)
HOT = (255, 236, 214)


# =========================================================================== model

def rotation(k):
    return np.pi - TAU * k / HEADINGS - SPIKE         # +Y forward (a spike first) -> heading k


def model(k, g):
    rot = rotation(k)
    base, mats = am.mote(g)
    return (lambda p: base(rotate_z(p, rot))), model_space_materials(mats, rot)


def render_unit(k, f):
    img = artkit.native(*artkit.render_hi(*model(k, GLOWS[f]), SIZE, EXTENT))
    return creeper.light_rim(img, RIM_STRENGTH)


# =========================================================================== death

def death_glow(i):
    """The ember puff: a crimson-white pop, an orange-crimson bloom, embers flung out and dying."""
    w, h = DEATH_SIZE
    t = fx.t_of(i, DEATH_FRAMES)
    c = w / 2
    cv = v8.Canvas(w, h)
    d = cv.dist(c, c)
    fade = np.clip((w / 2 - d) / 2.5, 0, 1)
    cv.add(HOT, v8.gauss(d, 1.2 + 2 * t) * 1.4 * np.clip(1 - t * 2.4, 0, 1))
    cv.add(CRIMSON, v8.gauss(d, 2.5 + 4 * fx.ease_out(t)) * 0.7 * (1 - t) ** 1.4 * fade)
    vd.motes(cv, np.random.default_rng(3241), c, t, 9, (4, 10), (0.4, 0.7), EMBER, HOT, fade, delay=0.1, gain=1.2)
    return artkit.additive(cv.image())


FIN = [(0.2, 0.1), (0.95, 0.45), (0.85, 0.05), (0.3, -0.15)]   # archetype_models.mote's fin


def death_pieces():
    rng = np.random.default_rng(3251)
    pieces = []
    for a in np.linspace(0, TAU, 3, endpoint=False):           # the three dark fins
        def fin(q, a=a):
            return sd_plate(rotate_z(q, a), FIN, 0.0, 0.07, 0.02)
        c0 = (float(np.cos(-a + 0.6) * 0.5), float(np.sin(-a + 0.6) * 0.5), 0.0)
        pieces.append(dict(parts=[(fin, V_DARK)], c0=c0, dir=vd.outward(rng, c0, 0.4), reach=rng.uniform(0.5, 0.8),
                           ease=2.6, axis=rng.uniform(0, TAU), tumble=rng.uniform(4, 8) * rng.choice([-1, 1]),
                           spin=rng.uniform(-4, 4), rise=0.2, shrink_at=0.5))
    for side in (1, -1):                                         # the husk in two, with its spikes
        def half(q, side=side):
            r = q.copy()
            r[:, 0] *= side
            d = np.maximum(sd_sphere(r, (0, 0, 0), 0.46), -r[:, 0] + 0.04)
            spike = sd_capsule(rotate_z(r, 1.05), (0, 0.3, 0.05), (0, 0.8, 0.1), 0.1, 0.02)
            return np.minimum(d, spike)
        c0 = (side * 0.2, 0.0, 0.0)
        pieces.append(dict(parts=[(half, V_BODY if side > 0 else V_BONE)], c0=c0, dir=(float(side), rng.uniform(-0.4, 0.4)),
                           reach=rng.uniform(0.3, 0.5), ease=2.6, axis=rng.uniform(0, TAU),
                           tumble=rng.uniform(3, 6) * rng.choice([-1, 1]), spin=rng.uniform(-3, 3),
                           rise=0.2, shrink_at=0.5))
    return pieces


def death_tatters(i):
    mats = am.mote(0.3)[1]
    scene, mats = creeper.pieces_scene(death_pieces(), mats, fx.t_of(i, DEATH_FRAMES), lambda p: p)
    return artkit.native(*artkit.render_hi(scene, mats, DEATH_SIZE, DEATH_EXTENT))


# =========================================================================== build

def _job(job):
    name, args = job
    return {"unit": render_unit, "death": death_glow, "tatters": death_tatters}[name](*args)


def build():
    cells = [(k, f) for k in range(HEADINGS) for f in range(len(GLOWS))]   # heading * 3 + frame
    with ProcessPoolExecutor() as pool:
        def run(name, arglist):
            return list(pool.map(_job, [(name, a) for a in arglist]))
        sets = [("mote-swarm", run("unit", cells), COLOURS),
                ("mote-swarm-death", run("death", [(i,) for i in range(DEATH_FRAMES)]), DEATH_COLOURS),
                ("mote-swarm-tatters", run("tatters", [(i,) for i in range(DEATH_FRAMES)]), DEATH_COLOURS)]
    for name, frames, colours in sets:
        frames = artkit.quantize_set(frames, colours)
        artkit.write_frames(name, frames, SOURCE)
        print(f"{name}: {len(frames)} frames, {frames[0].width}x{frames[0].height}")


# =========================================================================== review

def leader_route():
    """The leader's route on a 240x300 field: in from the top right, a swirl in front of the ship,
    out at the bottom left, 1.5 s off the screen, back up from the bottom through the ship's lane and
    out at the top."""
    pts = [(300, 30), (190, 50), (90, 90), (60, 150), (120, 190), (190, 150), (170, 90), (100, 110),
           (40, 200), (-30, 330), (-60, 420), (40, 470), (110, 380), (125, 250), (120, 120), (115, -60)]
    return creeper.smooth_path(pts, rounds=3)


def flock(n=20, fps=20, seconds=8.0):
    """Positions and velocities of a boids flock (separation 18 px, alignment, cohesion toward the
    leader) over the review's frames; deterministic. A few motes are shot down on the way back up."""
    path = leader_route()
    total = path[1][-1]
    rng = np.random.default_rng(32)
    pos = np.array(creeper.along(path, 0)[0]) + rng.normal(0, 14, (n, 2))
    vel = np.tile([-200.0, 40.0], (n, 1))
    offs = rng.normal(0, 1, (n, 2))
    dt = 1 / fps
    out = []
    s = 0.0
    for i in range(int(seconds * fps)):
        t = i * dt
        back = s > total * 0.62                               # the loop-back: the dive at 260 px/s
        s = min(total, s + (260 if back else 200) * dt)
        leader = np.array(creeper.along(path, s)[0])
        target = leader + offs * (14 + 5 * np.sin(t * 2 + np.arange(n))[:, None])
        steer = (target - pos) * 6.0 - vel * 1.6
        diff = pos[:, None, :] - pos[None, :, :]
        dist = np.linalg.norm(diff, axis=-1) + np.eye(n) * 1e6
        sep = (diff / dist[..., None] ** 2 * (dist < 18)[..., None]).sum(axis=1) * 700
        align = (vel.mean(axis=0) - vel) * 0.8
        vel = vel + (steer + sep + align) * dt
        sp = np.linalg.norm(vel, axis=1, keepdims=True)
        vel = np.where(sp > 320, vel / sp * 320, vel)
        pos = pos + vel * dt
        out.append((t, pos.copy(), vel.copy(), back))
    return out


def review():
    frames = artkit.load_frames("mote-swarm")
    burst = artkit.load_frames("mote-swarm-death")
    tatters = artkit.load_frames("mote-swarm-tatters")
    pop = artkit.load_frames("explosion-tiny")
    nf = len(GLOWS)
    heading = [frames[k * nf:(k + 1) * nf] for k in range(HEADINGS)]
    layers = [(tatters, DEATH_STEPS, False, 0), (pop, 2, True, 0), (burst, DEATH_STEPS, True, GLOW_DELAY)]
    together = fx.composite_strip(layers, DEATH_FRAMES * DEATH_STEPS + GLOW_DELAY + 4, DEATH_SIZE, 3)
    strip = wraith.dawn_plate(16 * 22, 40)
    for k in range(HEADINGS):
        strip.alpha_composite(heading[k][k % nf], (4 + 22 * k, 12))
    sheet = artkit.review_sheet("MOTE SWARM - FINAL SPRITES (PROPOSAL)", [
        ("HEADINGS 0-15 (CLOCKWISE FROM DOWN, A SPIKE LEADING), FLICKER FRAME 1", [h[1] for h in heading], 5, False),
        (f"GLOW FLICKER 0.8 / 1.1 / 1.5, HEADING 0 AND HEADING 8, {FPS} FPS", heading[0] + heading[8], 6, False),
        ("1X ON THE DAWN STAND-IN (LEVEL 10'S BACKDROP IS NOT BUILT YET), 3X", [strip], 3, False),
        (f"TATTERS, {STEP // DEATH_STEPS} FPS, SOLID", tatters, 5, False),
        (f"EMBER PUFF, {STEP // DEATH_STEPS} FPS, {GLOW_DELAY} STEPS AFTER THE POP, ADDITIVE", burst, 5, True),
        ("TOGETHER WITH EXPLOSION-TINY, EVERY 2ND STEP", together, 1, False),
        ("1X", [h[0] for h in heading], 1, False)], width=1500, batch=BATCH)
    artkit.save_review(sheet, review_loop(heading), CONCEPT, "mote-swarm", fps=20)


def review_loop(heading):
    fw, fh, fps = 240, 300, 20
    plate = wraith.dawn_plate(fw, 720, 150, 120)
    ship = artkit.load_frames("ship")[2]
    ship_xy = (120, 220)
    pop = artkit.load_frames("explosion-tiny")
    burst = artkit.load_frames("mote-swarm-death")
    tatters = artkit.load_frames("mote-swarm-tatters")
    shot = {3: 4.9, 7: 5.0, 11: 5.15, 15: 5.3}                # motes hit as they dive up past the ship
    dead = {}
    gif = []
    for f, (t, pos, vel, back) in enumerate(flock(fps=fps)):
        scroll = int(t * 190 * 0.5) % (720 - fh)
        cell = plate.crop((0, 720 - fh - scroll, fw, 720 - scroll))
        cell = wraith.draw_body(cell, ship, *ship_xy)
        off = (pos[:, 1] > fh + 8).mean() > 0.5
        if off or (back and pos[:, 1].min() > fh - 20):
            wraith.edge_warning(cell, int(np.clip(pos[:, 0].mean(), 20, fw - 20)), t)
        for k in range(len(pos)):
            if k in shot and t >= shot[k]:
                dead.setdefault(k, (f, pos[k].copy()))
                continue
            x, y = pos[k]
            hd = creeper.heading_of(*vel[k])
            fr = heading[hd][(int(t * FPS) + k) % len(GLOWS)]
            cell = wraith.draw_body(cell, fr, x, y)
        for k, (f0, (x, y)) in dead.items():
            age = (f - f0) * STEP // fps
            for frames_, steps, glow, delay in ((tatters, DEATH_STEPS, False, 0), (pop, 2, True, 0),
                                                (burst, DEATH_STEPS, True, GLOW_DELAY)):
                fr = fx.timed(frames_, steps, age - delay)
                if fr is None:
                    continue
                if glow:
                    cell = wraith.add_centred(cell, fr, x, y)
                else:
                    cell.alpha_composite(fr, (int(round(x - fr.width / 2)), int(round(y - fr.height / 2))))
        gif.append(sprite.enlarge(cell, 2))
    return gif


if __name__ == "__main__":
    artkit.REVIEW_ROUND = REVIEW_ROUND
    if "--review" not in sys.argv[1:]:
        build()
    review()
