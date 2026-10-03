#!/usr/bin/env python3
"""Production art: the death effects of Level 03's Vrell beyond the explosion ladder
(design/enemies/air/spore-bomber, design/enemies/air/whirl-seed, design/enemies/space/leviathan;
M4 part C batch). Each plays together with the unit's explosion-ladder burst, which stays as it is.

Outputs (assets/sprites/; glows additive, premultiplied on black; physical pieces solid):
  spore-bomber-death_0..15     96x96, additive: the lime spore cloud bursting from the gas bag's
                               outline, a billowing haze and drifting motes that twinkle out
  spore-bomber-tatters_0..15   96x96, solid: twelve olive membrane tatters (the gas bag's hide with its
                               veins) and four dark chitin bits flung out, tumbling, shrivelling
  whirl-seed-husk_0..11        40x40, solid: the six blades breaking off and tumbling outwards, the
                               core husk splitting into two halves that fall open
  whirl-seed-death_0..7        32x32, additive: the teal glint, a four-point star flare with sparks
  leviathan-ichor_0..15        160x160, additive: a gush of glowing violet ichor globules and a
                               violet mist that billows out and thins (one per wound, see below)
  design/enemies/air/spore-bomber/concept/spore-bomber-death-final-r16-a.png/.gif
  design/enemies/air/whirl-seed/concept/whirl-seed-death-final-r16-a.png/.gif
  design/enemies/space/leviathan/concept/leviathan-death-final-r16-a.png/.gif

Timing (game steps of 1/60 s per frame; all centred on the unit's position):
  spore-bomber-death, -tatters  4 steps (15 fps, 1.07 s), both started with the medium burst
  whirl-seed-husk, -death       2 steps (30 fps, 0.40 s and 0.27 s), the husk started with the tiny pop,
                                the glint 4 steps later (as the pop's white flash fades)
  leviathan-ichor               6 steps (10 fps, 1.60 s), one at each part with that part's chained
                                medium burst (same delay) and one at the centre with the
                                large burst

The glows are vfx_r08's 2D light fields (its Canvas, gauss, the cartesian fBm), like the explosions
and the spore mine; the solid pieces are ray-marched from the units' own models and materials
(the chosen round-04 Spore Bomber's gas-bag hide, the six-blade production Whirl Seed of
tools/art/vrell_l03.py) posed per frame in the model under the fixed key light, so nothing lit is
rotated or mirrored as an image. One palette per set.

Run: python3 tools/art/vrell_fx.py [bomber] [seed] [ichor] [--review]   (~1 min)
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
import leviathan as lev  # noqa: E402  (tools/art: the unit's layout and review compose)
import vrell_l03 as l03  # noqa: E402  (tools/art: the seed's blade and material frames)
from render import enemy_models as em  # noqa: E402
from render.sdf import rotate_x, rotate_z, sd_capsule, sd_plate, sd_sphere, union  # noqa: E402

SCRIPT = "vrell_fx.py"
BATCH = "M4 part C batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
REVIEW_ROUND = "r16"
STEP = 60                                       # game steps per second
GLINT_DELAY = 4                                 # steps: the glint starts as the pop's white flash fades

LIME = np.array(em.hx(em.GLOWS["lime"])) * 255
TEAL = np.array(em.hx(em.GLOWS["teal"])) * 255
VIOLET = np.array(em.hx(em.GLOWS["violet"])) * 255

# name -> (size, frames, steps per frame, colours)
SETS = {
    "spore-bomber-death": ((96, 96), 16, 4, 32),
    "spore-bomber-tatters": ((96, 96), 16, 4, 32),
    "whirl-seed-husk": ((40, 40), 12, 2, 24),
    "whirl-seed-death": ((32, 32), 8, 2, 16),
    "leviathan-ichor": ((160, 160), 16, 6, 40),
}


def ease_out(t, k=2.0):
    return 1 - (1 - np.clip(t, 0, 1)) ** k


def t_of(i, n):
    return i / max(n - 1, 1)


# =========================================================================== Spore Bomber

BOMBER = l03.size_of("spore-bomber")            # 72x72
BAG = (14.0, 25.0)                              # the gas bag's half axes on screen, px (72 px sprite)


def spore_cloud(i):
    """Frame i of the lime spore cloud: motes burst from the bag's outline and drift out on a
    billowing haze; everything twinkles and thins out."""
    (w, h), n, _, _ = SETS["spore-bomber-death"]
    rng = np.random.default_rng(1601)
    t = t_of(i, n)
    cx, cy = w / 2, h / 2
    cv = v8.Canvas(w, h)
    tex = v8._cart_noise(w, 1602, cell=w * v8.SS // 6)
    puffs = [(rng.uniform(0, TAU), rng.uniform(0.3, 1.0), rng.uniform(9, 15)) for _ in range(9)]
    dens = np.zeros_like(cv.x)
    grow = ease_out(t, 2.4)
    for a, r0, rb in puffs:
        px = cx + np.cos(a) * BAG[0] * r0 * (0.6 + 1.1 * grow)
        py = cy + np.sin(a) * BAG[1] * r0 * (0.6 + 0.8 * grow)
        dd = cv.dist(px, py) / (rb * (0.7 + 0.9 * grow))
        dens = np.maximum(dens, np.exp(-dd * dd * 1.3))
    churn = v8._shift(tex, t * w * 0.35, -t * w * 0.25)
    haze = np.clip(dens * (0.45 + 0.9 * churn) - 0.18 - 0.35 * t, 0, 1)
    env = np.clip(i / 2.0, 0.35, 1) * (1 - t) ** 0.8
    cv.add((60, 110, 14), haze * 1.2 * env)                         # the haze's dark body
    cv.add(LIME, np.clip(haze - 0.4, 0, 1) * 0.75 * env)            # its brighter billows
    fade = np.clip((w / 2 - cv.dist(cx, cy)) / 4.0, 0, 1)           # nothing reaches the frame edge
    for k in range(96):
        a = rng.uniform(0, TAU)
        start = rng.uniform(0.7, 1.0)
        reach = rng.uniform(0.25, 0.85)
        size = rng.uniform(0.4, 0.95)
        phase = rng.uniform(0, TAU)
        delay = rng.uniform(0, 0.12)
        tt = np.clip((t - delay) / (1 - delay), 0, 1)
        if t < delay:
            continue
        r = start + reach * ease_out(tt, 2.2) * 1.6
        x = cx + np.cos(a) * BAG[0] * r + 2.0 * np.sin(phase + 3 * tt)
        y = cy + np.sin(a) * BAG[1] * r + 2.0 * np.cos(phase + 2 * tt) + 4.0 * tt   # sinking a little
        twinkle = 0.65 + 0.35 * np.sin(phase + 7.0 * tt * TAU / 3)
        life = np.clip(1.15 - tt * (0.9 + 0.5 * rng.random()), 0, 1)
        d = cv.dist(x, y)
        col = (235, 255, 190) if size > 0.8 else LIME
        cv.add(col, v8.gauss(d, size) * 1.4 * twinkle * life * fade)
        cv.add(LIME, v8.gauss(d, size * 2.4) * 0.22 * life * fade)
    return artkit.additive(cv.image())


TATTER_EXTENT = 2.15 * 96 / 72                  # the bomber's model scale (vrell_l03 BOMBER_EXTENT)
PX = 96 / TATTER_EXTENT                         # px per model unit


def jagged(rng, size, points=7):
    """An irregular torn membrane outline around the origin."""
    angles = np.sort(rng.uniform(0, TAU, points))
    radii = size * rng.uniform(0.55, 1.0, points)
    return [(float(np.cos(a) * r), float(np.sin(a) * r * rng.uniform(0.6, 1.0))) for a, r in zip(angles, radii)]


def tatter_pieces():
    rng = np.random.default_rng(1611)
    pieces = []
    for k in range(12):                         # membrane tatters from the bag's outline
        a = TAU * k / 12 + rng.uniform(-0.2, 0.2)
        size = rng.uniform(0.17, 0.3)
        pieces.append(dict(kind="hide", a=a, start=rng.uniform(0.45, 0.8), reach=rng.uniform(0.38, 0.68),
                           poly=jagged(rng, size), size=size, axis=rng.uniform(0, TAU),
                           tumble=rng.uniform(2.5, 6.0) * rng.choice([-1, 1]), spin=rng.uniform(-2.5, 2.5),
                           tilt0=rng.uniform(-0.5, 0.5), bend=rng.uniform(0.8, 1.6), vein=k % 2 == 0))
    for k in range(4):                          # chitin bits of the head and the tail fins
        a = [np.pi / 2, -np.pi / 2 + 0.5, -np.pi / 2 - 0.5, np.pi / 2 + 0.9][k]
        size = rng.uniform(0.08, 0.12)
        pieces.append(dict(kind="chitin", a=a, start=0.8, reach=rng.uniform(0.5, 0.7),
                           poly=jagged(rng, size, 4), size=size, axis=rng.uniform(0, TAU),
                           tumble=rng.uniform(5, 9) * rng.choice([-1, 1]), spin=rng.uniform(-4, 4),
                           tilt0=0.0, bend=0.0, vein=False))
    return pieces


def tatters_model(t):
    """The tatters at time t (0..1 of the set): each piece posed in the model, its hide pattern
    evaluated in its own frame so it turns with the piece."""
    _, mats = e4.R04["spore-bomber-a"][4](anim=0.0, glow=1.0)
    mats = list(mats)
    bag = (BAG[0] / PX, BAG[1] / PX)
    items_of = []
    pieces = tatter_pieces()
    shrink = np.clip(1.0 - (t - 0.6) / 0.4 * 0.65, 0.35, 1.0)
    for j, pc in enumerate(pieces):
        out = pc["reach"] * ease_out(t, 2.0)                 # flung out radially from the bag
        cx = np.cos(pc["a"]) * (bag[0] * pc["start"] + out)
        cy = -(np.sin(pc["a"]) * (bag[1] * pc["start"] + out) + 0.12 * t)   # screen y down = model y up
        ang = pc["tilt0"] + pc["tumble"] * t
        spin = pc["spin"] * t
        axis = pc["axis"]
        sc = shrink * (1.0 if pc["kind"] == "hide" else 0.5 + 0.5 * shrink)

        def local(p, cx=cx, cy=cy, axis=axis, ang=ang, spin=spin, sc=sc):
            q = p.copy()
            q[:, 0] -= cx
            q[:, 1] -= cy
            q = rotate_z(q, axis)
            q = rotate_x(q, ang)
            q = rotate_z(q, spin - axis)
            return q / sc

        base = em.V_SAC if pc["kind"] == "hide" else em.V_DARK
        mats.append(replace(mats[base], pattern=l03.in_frame(mats[base].pattern, local)
                            if mats[base].pattern else None))
        items_of.append((pc, local, sc, len(mats) - 1))

    def scene(p):
        items = []
        for pc, local, sc, m in items_of:
            q = local(p)
            if pc["bend"]:                                    # a curled scrap of hide
                q = q.copy()
                q[:, 2] -= pc["bend"] * (q[:, 0] ** 2 + q[:, 1] ** 2)
            d = sd_plate(q, pc["poly"], 0.0, 0.018 if pc["kind"] == "hide" else 0.03, 0.008) * sc / 1.25
            items.append((d, m))
            if pc["vein"]:
                vq = local(p)
                vq = vq.copy()
                vq[:, 2] -= pc["bend"] * (vq[:, 0] ** 2 + vq[:, 1] ** 2)
                s = pc["size"] * 0.7
                items.append((sd_capsule(vq, (-s, -0.2 * s, 0.012), (s, 0.25 * s, 0.012), 0.016) * sc / 1.25,
                              em.V_SEAM))
        return union(*items)
    return scene, mats


def render_tatters(i):
    (w, h), n, _, _ = SETS["spore-bomber-tatters"]
    scene, mats = tatters_model(t_of(i, n))
    return artkit.native(*artkit.render_hi(scene, mats, (w, h), TATTER_EXTENT))


# =========================================================================== Whirl Seed

HUSK_EXTENT = l03.SEED_EXTENT * 40 / 26         # the seed's model scale on the 40 px canvas
SPLIT = 0.6                                     # the core splits along this screen angle (rad)


def husk_model(t):
    """The seed at time t of its pop: blades torn off and tumbling out along their axes, the
    core husk split into two halves falling apart; the eye is gone into the glint."""
    sector = TAU / l03.SEED_PETALS
    base = em.vrell_scheme_mats("whirl-seed", "a")
    mats = list(base)
    spin = 0.35 * ease_out(t, 2.0)                           # the spin running down
    shrink = np.clip(1.0 - (t - 0.6) / 0.4 * 0.7, 0.3, 1.0)
    rng = np.random.default_rng(1621)
    blades = []
    for k in range(l03.SEED_PETALS):
        out = 0.95 * ease_out(t, 1.8) * rng.uniform(0.8, 1.15)
        roll = rng.uniform(2.0, 4.0) * rng.choice([-1, 1]) * t
        yaw = rng.uniform(-0.8, 0.8) * t

        def local(p, k=k, out=out, roll=roll, yaw=yaw):
            q = rotate_z(p, -spin)
            q = rotate_z(q, k * sector)                      # blade k along +x
            q = q.copy()
            q[:, 0] -= out
            q[:, 0] -= 0.5
            q = rotate_z(q, yaw)
            q = rotate_x(q, roll)                            # tumbling about its own long axis
            q[:, 0] += 0.5
            return q / shrink

        body = base[em.V_BODY]
        mats.append(replace(body, pattern=l03.in_frame(body.pattern, local)))
        blades.append((local, len(mats) - 1))
    halves = []
    for sgn in (1, -1):
        apart = 0.42 * ease_out(t, 2.0)
        open_ = 1.1 * ease_out(t, 1.6)

        def local(p, sgn=sgn, apart=apart, open_=open_):
            q = rotate_z(p, -SPLIT)                          # split line along local x
            q = q.copy()
            q[:, 1] -= sgn * apart
            q = rotate_x(q, -sgn * open_)                    # each half falls open outwards
            return q / shrink
        halves.append((sgn, local))
    seam = base[em.V_SEAM]
    mats[em.V_SEAM] = replace(seam, emission=tuple(np.array(seam.emission) * l03.SEED_SEAM))

    def scene(p):
        items = [(sd_plate(local(p), l03.SEED_PETAL, 0.0, 0.06, 0.02,
                           taper=lambda x, y: np.clip(1.2 - 0.8 * x, 0.3, 1)) * shrink, m)
                 for local, m in blades]
        for sgn, local in halves:
            q = local(p)
            shell = np.maximum(sd_sphere(q, (0, 0, 0.05), 0.32), -sd_sphere(q, (0, 0, 0.05), 0.24))
            half = np.maximum(shell, -sgn * q[:, 1])
            items.append((half * shrink, em.V_SEAM))
        if t == 0:
            items.append((sd_sphere(p, (0, 0, 0.25), 0.16), em.V_EYE))
        return union(*items)
    return scene, mats


def render_husk(i):
    (w, h), n, _, _ = SETS["whirl-seed-husk"]
    scene, mats = husk_model(t_of(i, n))
    return artkit.native(*artkit.render_hi(scene, mats, (w, h), HUSK_EXTENT))


def glint(i):
    """Frame i of the teal glint: a four-point star flare flaring and closing, a few sparks flying."""
    (w, h), n, _, _ = SETS["whirl-seed-death"]
    t = t_of(i, n)
    cv = v8.Canvas(w, h)
    cx, cy = w / 2, h / 2
    d = cv.dist(cx, cy)
    flare = np.sin(np.pi * np.clip(0.15 + t * 1.25, 0, 1)) ** 1.5
    fade = np.clip((w / 2 - d) / 3.0, 0, 1)
    for ang, length, k in ((0.0, 13.0, 1.0), (np.pi / 2, 13.0, 1.0), (np.pi / 4, 6.0, 0.55), (-np.pi / 4, 6.0, 0.55)):
        ln = length * (0.4 + 0.6 * flare)
        dx, dy = np.cos(ang) * ln, np.sin(ang) * ln
        seg = cv.seg(cx - dx, cy - dy, cx + dx, cy + dy)
        along = np.clip(1 - d / ln, 0, 1)
        cv.add(TEAL, v8.gauss(seg, 0.55) * along * 1.3 * k * flare * fade)
    cv.add(TEAL, v8.gauss(d, 3.2 + 2.0 * t) * 0.55 * (1 - t) * fade)
    cv.add((205, 255, 232), v8.gauss(d, 0.9 + 0.8 * flare) * 1.5 * flare)
    rng = np.random.default_rng(1641)
    for k in range(6):                                       # sparks of the core's light (no ring:
        a = TAU * k / 6 + rng.uniform(-0.4, 0.4)             # a ring and a star read as a reticle)
        r = 2.0 + rng.uniform(8, 12) * ease_out(t, 1.8)
        x, y = cx + np.cos(a) * r, cy + np.sin(a) * r
        cv.add(TEAL, v8.gauss(cv.dist(x, y), 0.55) * 1.2 * (1 - t) * fade * (t > 0))
    return artkit.additive(cv.image())


# =========================================================================== Leviathan

def ichor(i):
    """Frame i of a wound's ichor: a gush of glowing globules thrown out of the wound, a violet
    mist billowing after them, spreading and thinning into wisps."""
    (w, h), n, _, _ = SETS["leviathan-ichor"]
    rng = np.random.default_rng(1631)
    t = t_of(i, n)
    cx, cy = w / 2, h / 2
    cv = v8.Canvas(w, h)
    tex = v8._cart_noise(w, 1632, cell=w * v8.SS // 6)
    tex2 = v8._cart_noise(w, 1633, cell=w * v8.SS // 12)
    dens = np.zeros_like(cv.x)
    for k in range(12):
        a = rng.uniform(0, TAU)
        r0 = 0.0 if k == 0 else rng.uniform(10, 46)
        rb = rng.uniform(14, 24)
        delay = 0.0 if k == 0 else rng.uniform(0, 0.3)
        tt = np.clip((t - delay) / (1 - delay), 0, 1)
        g = ease_out(tt, 2.2)
        px = cx + np.cos(a) * r0 * (0.25 + 1.0 * g)
        py = cy + np.sin(a) * r0 * (0.25 + 1.0 * g)
        dd = cv.dist(px, py) / (rb * (0.35 + 1.1 * g) + 1e-6)
        dens = np.maximum(dens, np.exp(-dd * dd * 1.3) * (t >= delay))
    churn = v8._shift(tex, t * w * 0.3, t * w * 0.2)
    wisp = v8._shift(tex2, -t * w * 0.4, t * w * 0.15)
    mist = np.clip(dens * (0.4 + 0.8 * churn) * (0.75 + 0.5 * wisp) - 0.1 - 0.4 * t, 0, 1)
    env = np.clip((i + 1) / 3.0, 0, 1) * (1 - t) ** 0.6
    d = cv.dist(cx, cy)
    fade = np.clip((w / 2 - d) / 10.0, 0, 1)
    cv.add((52, 8, 84), mist * 2.2 * env * fade)                       # the plum body of the mist
    cv.add(VIOLET, np.clip(mist - 0.22, 0, 1) * 1.4 * env * fade)       # violet billows
    cv.add((240, 160, 255), np.clip(mist - 0.5, 0, 1) * 1.4 * env * fade * (1 - t))   # lilac where dense
    gush = max(0.0, 1 - i / 3)                                         # the wound bursting open
    cv.add(VIOLET, v8.gauss(d, 6 + 6 * t) * 1.2 * gush)
    cv.add((255, 225, 255), v8.gauss(d, 2.5 + 3 * t) * 1.3 * gush)
    for k in range(30):
        a = rng.uniform(0, TAU)
        speed = rng.uniform(0.3, 1.0)
        size = rng.uniform(0.7, 1.9) * (1.3 - 0.5 * speed)              # the fast ones are small
        reach = 64 * speed
        r = 3 + reach * ease_out(t, 3.0)
        r_prev = 3 + reach * ease_out(max(t - 0.035, 0), 3.0)            # a short smear while fast
        x, y = cx + np.cos(a) * r, cy + np.sin(a) * r
        xp, yp = cx + np.cos(a) * r_prev, cy + np.sin(a) * r_prev
        life = np.clip(1.15 - t * rng.uniform(0.9, 1.5), 0, 1)
        if t == 0:
            continue
        seg = cv.seg(xp, yp, x, y)
        cv.add(VIOLET, v8.gauss(seg, 1.1 * size) * 0.7 * life * fade)
        cv.add((240, 160, 255), v8.gauss(seg, 0.55 * size) * 1.2 * life * fade)
        cv.add((255, 235, 255), v8.gauss(cv.dist(x, y), 0.35 * size) * life * life * fade)
    return artkit.additive(cv.image())


# =========================================================================== build

GLOWS = {"spore-bomber-death": spore_cloud, "whirl-seed-death": glint, "leviathan-ichor": ichor}
SOLIDS = {"spore-bomber-tatters": render_tatters, "whirl-seed-husk": render_husk}
PARTS = {"bomber": ["spore-bomber-death", "spore-bomber-tatters"],
         "seed": ["whirl-seed-husk", "whirl-seed-death"],
         "ichor": ["leviathan-ichor"]}


def _frame(job):
    name, i = job
    return (GLOWS.get(name) or SOLIDS[name])(i)


def build(part):
    with ProcessPoolExecutor() as pool:
        for name in PARTS[part]:
            size, n, _, colours = SETS[name]
            frames = list(pool.map(_frame, [(name, i) for i in range(n)]))
            assert all(f.size == size for f in frames), (name, frames[0].size)
            artkit.write_frames(name, artkit.quantize_set(frames, colours), SOURCE)


# =========================================================================== review

PLATE_SPACE = artkit.PLATE


def timed(frames, steps, age):
    """The frame an effect shows ``age`` game steps after it started, or None when it is over."""
    if age < 0 or age >= len(frames) * steps:
        return None
    return frames[age // steps]


def rows_for(names, zoom, every=1):
    rows = []
    for name in names:
        frames = artkit.load_frames(name)
        _, n, steps, _ = SETS[name]
        glow = name in GLOWS
        sampled = f", EVERY {every}{'RD' if every == 3 else 'ND' if every == 2 else 'TH'} FRAME" if every > 1 else ""
        rows.append((f"{name.upper()}, {STEP // steps} FPS ({steps} STEPS), {'ADDITIVE' if glow else 'SOLID'}{sampled}",
                     frames[::every], zoom, glow))
    return rows


def composite_strip(frames_by_layer, n, size, zoom):
    """The layers as the game stacks them, frame by frame at 60 Hz sampled every 2 steps:
    solid pieces under the glows, glows added."""
    out = []
    for age in range(0, n, 2):
        img = Image.new("RGBA", size, PLATE_SPACE)
        for frames, steps, glow, delay in frames_by_layer:
            f = timed(frames, steps, age - delay)
            if f is None:
                continue
            xy = ((size[0] - f.width) // 2, (size[1] - f.height) // 2)
            if glow:
                img = artkit.add_light(img, f, xy)
            else:
                img.alpha_composite(f, xy)
        out.append(sprite.enlarge(img, zoom))
    return out


def review_bomber():
    unit = artkit.load_frames("spore-bomber")
    burst = artkit.load_frames("explosion-medium")
    cloud = artkit.load_frames("spore-bomber-death")
    tatters = artkit.load_frames("spore-bomber-tatters")
    layers = [(tatters, 4, False, 0), (burst, 2, True, 0), (cloud, 4, True, 0)]
    together = composite_strip(layers, 64, (96, 96), 1)
    sheet = artkit.review_sheet("SPORE BOMBER DEATH - FINAL EFFECTS", rows_for(
        ["spore-bomber-death", "spore-bomber-tatters"], 3, every=3) + [
        ("TOGETHER WITH THE MEDIUM BURST (ITS 14 FRAMES AT 2 STEPS), EVERY 2ND STEP; TATTERS UNDER THE GLOWS",
         together, 2, False),
        ("1X", cloud, 1, True), ("1X", tatters, 1, False)], width=1700, batch=BATCH)
    fw, fh, fps = 180, 170, 30
    gif = []
    for f in range(int(fps * 2.4)):
        step = f * 2
        cell = Image.new("RGBA", (fw, fh), PLATE_SPACE)
        die = 30                                              # steps: the bomber dies 0.5 s in
        x, y = fw / 2, 64 + 45 * min(step, die) / STEP        # the data's 45 px/s down the screen
        if step < die:
            sprite.paste_center(cell, unit[(step // 6) % len(unit)], x, y)
        age = step - die
        for frames, steps, glow, delay in layers:
            fr = timed(frames, steps, age - delay)
            if fr is None:
                continue
            xy = (int(round(x - fr.width / 2)), int(round(y - fr.height / 2)))
            if glow:
                cell = artkit.add_light(cell, fr, xy)
            else:
                cell.alpha_composite(fr, xy)
        gif.append(sprite.enlarge(cell, 2))
    artkit.save_review(sheet, gif, DESIGN / "enemies" / "air" / "spore-bomber" / "concept", "spore-bomber-death",
                       fps=fps)


def review_seed():
    unit = artkit.load_frames("whirl-seed")
    pop = artkit.load_frames("explosion-tiny")
    husk = artkit.load_frames("whirl-seed-husk")
    flash = artkit.load_frames("whirl-seed-death")
    layers = [(husk, 2, False, 0), (pop, 2, True, 0), (flash, 2, True, GLINT_DELAY)]
    together = composite_strip(layers, 24, (40, 40), 1)
    sheet = artkit.review_sheet("WHIRL SEED DEATH - FINAL EFFECTS", rows_for(
        ["whirl-seed-husk", "whirl-seed-death"], 6) + [
        ("TOGETHER WITH THE TINY POP (ITS 12 FRAMES AT 2 STEPS), EVERY 2ND STEP; HUSK UNDER THE GLOWS, GLINT 4 STEPS LATE",
         together, 6, False),
        ("1X", husk, 1, False), ("1X", flash, 1, True)], batch=BATCH)
    fw, fh, fps = 160, 120, 30
    seeds = [(40, 60, 16), (80, 50, 34), (120, 66, 52)]    # x, y, the step it pops at
    spin = 1.5 * TAU / STEP / l03.SEED_STEP                 # spin frames per step
    gif = []
    for f in range(45):
        step = f * 2
        cell = Image.new("RGBA", (fw, fh), PLATE_SPACE)
        for x, y, die in seeds:
            if step < die:
                sprite.paste_center(cell, unit[int(step * spin) % len(unit)], x, y)
            for frames, steps, glow, delay in layers:
                fr = timed(frames, steps, step - die - delay)
                if fr is None:
                    continue
                xy = (int(round(x - fr.width / 2)), int(round(y - fr.height / 2)))
                if glow:
                    cell = artkit.add_light(cell, fr, xy)
                else:
                    cell.alpha_composite(fr, xy)
        gif.append(sprite.enlarge(cell, 4))
    artkit.save_review(sheet, gif, DESIGN / "enemies" / "air" / "whirl-seed" / "concept", "whirl-seed-death", fps=fps)


CHAIN_STEP = 6                                  # steps between the chained bursts (the game's chain)


def review_ichor():
    bodies = artkit.load_frames("leviathan-down")
    intact = lev.load_parts("intact")
    glow = artkit.load_frames("leviathan-blowhole-glow")[0]
    medium = artkit.load_frames("explosion-medium")
    large = artkit.load_frames("explosion-large")
    cloud = artkit.load_frames("leviathan-ichor")
    parts = lev.SPEC["part_list"]
    sheet = artkit.review_sheet("LEVIATHAN DEATH - FINAL ICHOR CLOUD", rows_for(["leviathan-ichor"], 1, every=2) + [
        ("EVERY 3RD FRAME, 2X", cloud[::3], 2, True)], width=2000, batch=BATCH)
    fps = 30
    pad = 70
    W, H = lev.W + 2 * pad, lev.H + 2 * pad
    death = 20                                                # steps of the unit alive first
    end_chain = len(parts) * CHAIN_STEP
    gif = []
    for f in range(int(fps * 3.2)):
        step = f * 2
        cell = Image.new("RGBA", (W, H), PLATE_SPACE)
        age = step - death
        if age < end_chain:                                   # the body stays until the large burst
            k = [0, 1, 2, 1][(step // 6) % 4]
            unit = lev.compose(bodies[k], {n: fr[k] if n == "fluke" else fr[0] for n, fr in intact.items()}, k,
                               glow, 0.55 + 0.45 * np.sin(TAU * step / 72))
            cell.alpha_composite(unit, (pad, pad))
        for p, part in enumerate(parts):
            x, y = lev.canvas_xy(part["offset"])
            x, y = x + pad, y + pad
            for frames, steps in ((cloud, 6), (medium, 3)):
                fr = timed(frames, steps, age - p * CHAIN_STEP)
                if fr is not None:
                    cell = artkit.add_light(cell, fr, (int(x - fr.width / 2), int(y - fr.height / 2)))
        for frames, steps in ((cloud, 6), (large, 4)):            # the centre's ichor with the large burst
            fr = timed(frames, steps, age - end_chain)
            if fr is not None:
                cell = artkit.add_light(cell, fr, (W // 2 - fr.width // 2, H // 2 - fr.height // 2))
        gif.append(cell)
    artkit.save_review(sheet, gif, lev.UNIT / "concept", "leviathan-death", fps=fps)


REVIEWS = {"bomber": review_bomber, "seed": review_seed, "ichor": review_ichor}

if __name__ == "__main__":
    artkit.REVIEW_ROUND = REVIEW_ROUND
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    for part in args or list(PARTS):
        if "--review" not in sys.argv[1:]:
            build(part)
        REVIEWS[part]()
