#!/usr/bin/env python3
"""Production art: the death effects of the Level 01 and Level 04 Vrell that still played only the
explosion ladder (design/enemies/air/skitter, design/enemies/air/needler,
design/enemies/ground/scuttler; M4 part H batch, concept round 26). Each plays together with the
unit's ladder burst, which stays as it is; the game picks them up by name (EnemyLooks: a slug's
``-death`` is its additive glow, its ``-tatters`` its solid pieces).

Outputs (assets/sprites/; glows additive, premultiplied on black; pieces solid):
  skitter-death_0..7        32x32, additive: the teal glow flash, a soft teal-white puff from the
                            spine seam and a few teal motes, starting 4 steps after the tiny pop
  skitter-tatters_0..9      32x32, solid: chitin flakes, the two scythe wings torn off at the root
                            and the plum body cracked into a fore and an aft flake with the tail
                            spikes, flung out, tumbling and shrivelling
  needler-death_0..9        48x48, additive: the violet flash in the carapace, a violet bloom and a
                            spray of violet and white sparks
  needler-tatters_0..9      48x48, solid: ivory shards, the carapace split into five jagged plates,
                            the two claws and the thorn launcher snapped off, four leg bits
  scuttler-death_0..15      96x96, additive: the lime organic burst, a lime-white flash from the
                            back, lime ichor globules with short trails and an olive mist that
                            billows low and thins
  scuttler-tatters_0..15    96x96, solid: shell fragments chipped off the back and the six legs torn
                            off at the hips, scattering over the ground and settling, then
                            shrivelling; the legless husk (vrell_l04.py) stays where it died
  design/enemies/air/skitter/concept/skitter-death-final-r26-a.png/.gif
  design/enemies/air/needler/concept/needler-death-final-r26-a.png/.gif
  design/enemies/ground/scuttler/concept/scuttler-death-final-r26-a.png/.gif

Timing (EnemyLooks, by size tier; game steps of 1/60 s per frame, centred on the unit):
  tiny (Skitter)            pieces 2 steps per frame with the pop, the glow 2 steps, 4 steps late
  small, medium (Needler,   4 steps per frame (15 fps), both started with the burst
  Scuttler)

The glows are vfx_r08's 2D light fields (its Canvas and gauss), like tools/art/vrell_fx.py's; the
pieces are ray-marched per frame from the units' own models and materials (vrell_air.py's
Skitter, the chosen round-04 Needler style B, vrell_l04.py's Scuttler) posed in the model under
the fixed key light, each material pattern evaluated in the piece's own frame so it turns with the
piece; nothing lit is rotated or mirrored as an image. One palette per set.

Run: python3 tools/art/vrell_deaths.py [skitter] [needler] [scuttler] [--review]   (~1 min)
"""
import sys
from concurrent.futures import ProcessPoolExecutor
from dataclasses import replace

import numpy as np
from PIL import Image

import artkit
from artkit import DESIGN, TAU, sprite

import vfx_r08 as v8  # noqa: E402  (concept script, imported unchanged)
import vrell_air as air  # noqa: E402  (tools/art: the Skitter's wing and sizes)
import vrell_fx as fx  # noqa: E402  (tools/art: easing, torn outlines, the review compositing)
import vrell_l03 as l03  # noqa: E402  (tools/art: patterns in a part's own frame)
import vrell_l04 as l04  # noqa: E402  (tools/art: the Scuttler's geometry numbers)
from render import archetype_models  # noqa: E402,F401  (adds the Scuttler's role colours)
from render import enemy_models as em  # noqa: E402
from render.sdf import rotate_x, rotate_z, sd_capsule, sd_ellipsoid, sd_plate, sd_sphere, union  # noqa: E402

SCRIPT = "vrell_deaths.py"
BATCH = "M4 part H batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
REVIEW_ROUND = "r26"
STEP = 60                                       # game steps per second
TINY_GLOW_DELAY = 4                             # EnemyLooks.TINY_GLOW_DELAY_TICKS

TEAL = np.array(em.hx(em.GLOWS["teal"])) * 255
VIOLET = np.array(em.hx(em.GLOWS["violet"])) * 255
LIME = np.array(em.hx(em.GLOWS["lime"])) * 255

# name -> (size, frames, steps per frame, colours, delay steps)
SETS = {
    "skitter-death": ((32, 32), 8, 2, 16, TINY_GLOW_DELAY),
    "skitter-tatters": ((32, 32), 10, 2, 24, 0),
    "needler-death": ((48, 48), 10, 4, 24, 0),
    "needler-tatters": ((48, 48), 10, 4, 32, 0),
    "scuttler-death": ((96, 96), 16, 4, 32, 0),
    "scuttler-tatters": ((96, 96), 16, 4, 32, 0),
}
EXTENT = 2.3                                    # model units across a unit's own sprite (vrell_air)


def extent(name, unit_px):
    """The model's scale on the effect's canvas: the unit's extent over its sprite, widened."""
    return EXTENT * SETS[name][0][0] / unit_px


# =========================================================================== shared

def shrink_of(t, start=0.6, least=0.35):
    """The pieces keep their size, then shrivel over the last part of the set."""
    return float(np.clip(1.0 - (t - start) / (1 - start) * (1 - least), least, 1.0))


def posed(piece, t):
    """The model -> piece transform at time t: flung out along its direction (decelerating),
    tumbling about an in-plane axis and spinning about the view axis, shrinking at the end."""
    out = piece["reach"] * fx.ease_out(t, piece.get("ease", 2.0))
    c0 = np.array(piece["c0"], float)
    centre = c0 + np.array([piece["dir"][0] * out, piece["dir"][1] * out, piece.get("rise", 0.0) * np.sin(np.pi * t)])
    axis, ang, spin = piece["axis"], piece["tumble"] * t, piece["spin"] * t
    sc = shrink_of(t, piece.get("shrink_at", 0.6), piece.get("least", 0.35))

    def local(p):
        q = p - centre
        q = rotate_z(rotate_x(rotate_z(q, axis), ang), spin - axis) / sc
        return q + c0                                         # back into the model's own frame
    return local, sc


def pieces_scene(pieces, base_mats, t, pre=None):
    """The scene of posed pieces, each ``(geometry(q) -> distance, material slot)``; ``pre`` maps
    screen to the unit's model frame first (its facing)."""
    mats = list(base_mats)
    items_of = []
    for pc in pieces:
        local, sc = posed(pc, t)
        m = mats[pc["mat"]]
        mats.append(replace(m, pattern=l03.in_frame(m.pattern, local) if m.pattern else None,
                            emission_pattern=l03.in_frame(m.emission_pattern, local) if m.emission_pattern else None,
                            emission=tuple(np.array(m.emission) * pc.get("glow", 1.0))))
        items_of.append((pc["geo"], local, sc, len(mats) - 1))

    def scene(p):
        if pre is not None:
            p = pre(p)
        return union(*[(geo(local(p)) * sc, m) for geo, local, sc, m in items_of])
    return scene, mats


def outward(rng, c0, spread=0.35):
    """A unit direction from the body's centre through the piece, jittered."""
    a = np.arctan2(c0[1], c0[0]) + rng.uniform(-spread, spread)
    return (float(np.cos(a)), float(np.sin(a)))


def motes(cv, rng, c, t, count, reach, size, colour, white, fade, delay=0.15, gain=1.3):
    """Sparks thrown out from the centre, decelerating, dying out; bright ones white-hot."""
    for _ in range(count):
        a = rng.uniform(0, TAU)
        r_max = rng.uniform(*reach)
        s = rng.uniform(*size)
        d0 = rng.uniform(0, delay)
        decay = rng.uniform(0.9, 1.4)
        if t < d0:
            continue
        tt = (t - d0) / (1 - d0)
        r = 2 + r_max * fx.ease_out(tt, 2.5)
        x, y = c + np.cos(a) * r, c + np.sin(a) * r
        life = np.clip(1.15 - tt * decay, 0, 1)
        dd = cv.dist(x, y)
        cv.add(white if s > (size[0] + size[1]) / 2 * 1.15 else colour, v8.gauss(dd, s) * gain * life * fade)
        cv.add(colour, v8.gauss(dd, s * 2.6) * 0.22 * life * fade)


# =========================================================================== Skitter

SKITTER_PX = air.size_of("skitter")[0]          # 24


def skitter_glow(i):
    """The teal glow flash: a puff from the spine seam flaring and closing, a few motes."""
    (w, h), n, _, _, _ = SETS["skitter-death"]
    t = fx.t_of(i, n)
    c = w / 2
    cv = v8.Canvas(w, h)
    d = cv.dist(c, c)
    fade = np.clip((w / 2 - d) / 3.0, 0, 1)
    flare = np.sin(np.pi * np.clip(0.2 + t * 1.2, 0, 1)) ** 1.4
    cv.add(TEAL, v8.gauss(d, 3.0 + 5.5 * fx.ease_out(t)) * 0.55 * (1 - t) ** 1.2 * fade)
    cv.add((210, 255, 236), v8.gauss(d, 1.2 + 1.4 * flare) * 1.4 * flare)
    # the seam's light running along the (vanished) body: a short vertical streak
    seam = cv.seg(c, c - 6 * flare, c, c + 6 * flare)
    cv.add(TEAL, v8.gauss(seam, 0.7) * 0.9 * flare * fade)
    motes(cv, np.random.default_rng(2601), c, t, 7, (7, 12), (0.45, 0.75), TEAL, (215, 255, 240), fade)
    return artkit.additive(cv.image())


def skitter_pieces():
    """Two scythe wings, a fore and an aft flake of the body, the two tail spikes."""
    rng = np.random.default_rng(2611)
    wing = air.SKITTER_WING
    pieces = []
    for side in (1, -1):
        def geo(q, side=side):
            r = q.copy()
            r[:, 0] = r[:, 0] * side                         # each wing in the right wing's frame
            return sd_plate(r, wing, 0.0, 0.05, 0.02, taper=lambda x, y: np.clip(1.1 - 0.9 * x, 0.25, 1))
        c0 = (side * 0.5, -0.25, 0.0)
        pieces.append(dict(geo=geo, mat=em.V_BODY, c0=c0, dir=outward(rng, c0, 0.25), reach=rng.uniform(0.7, 0.9),
                           axis=0.4 * side, tumble=rng.uniform(5, 8) * rng.choice([-1, 1]),
                           spin=rng.uniform(1.5, 3.0) * side, rise=0.15))
    # the body ellipsoid cracked across at y = 0.15: fore flake (with the head spike), aft flake
    for k, (sign, cy) in enumerate(((1, 0.5), (-1, -0.3))):
        def geo(q, sign=sign):
            body = sd_ellipsoid(q, (0, 0.05, 0), (0.2, 0.78, 0.17))
            cut = -sign * (q[:, 1] - 0.15 + 0.05 * np.sin(q[:, 0] * 25))   # a jagged crack
            d = np.maximum(body, cut)
            if sign > 0:
                d = np.minimum(d, sd_capsule(q, (0, 0.55, 0.02), (0, 1.0, 0.0), 0.09, 0.012))
            return d
        c0 = (0.0, cy, 0.0)
        pieces.append(dict(geo=geo, mat=em.V_SEAM, c0=c0, dir=(rng.uniform(-0.25, 0.25), sign * 1.0),
                           reach=rng.uniform(0.35, 0.5), axis=np.pi / 2, tumble=rng.uniform(4, 6) * rng.choice([-1, 1]),
                           spin=rng.uniform(-2.0, 2.0), glow=0.35))
    for side in (1, -1):
        def geo(q, side=side):
            r = q.copy()
            r[:, 0] = r[:, 0] * side
            return sd_capsule(r, (0.08, -0.55, 0.0), (0.2, -0.95, -0.02), 0.05, 0.01)
        c0 = (side * 0.14, -0.75, 0.0)
        pieces.append(dict(geo=geo, mat=em.V_DARK, c0=c0, dir=outward(rng, c0, 0.3), reach=rng.uniform(0.5, 0.7),
                           axis=rng.uniform(0, TAU), tumble=rng.uniform(6, 9) * rng.choice([-1, 1]),
                           spin=rng.uniform(-4, 4)))
    return pieces


def skitter_tatters(i):
    (w, h), n, _, _, _ = SETS["skitter-tatters"]
    scene, mats = pieces_scene(skitter_pieces(), em.vrell_scheme_mats("skitter", "a"), fx.t_of(i, n), em._face_down)
    return artkit.native(*artkit.render_hi(scene, mats, (w, h), extent("skitter-tatters", SKITTER_PX)))


# =========================================================================== Needler

NEEDLER_PX = air.size_of("needler")[0]          # 36


def needler_glow(i):
    """The violet flash in the carapace: a white-violet core, a violet bloom, violet sparks."""
    (w, h), n, _, _, _ = SETS["needler-death"]
    t = fx.t_of(i, n)
    c = w / 2
    cv = v8.Canvas(w, h)
    d = cv.dist(c, c)
    fade = np.clip((w / 2 - d) / 4.0, 0, 1)
    cv.add((245, 228, 255), v8.gauss(d, 2.5 + 5 * t) * 1.4 * np.clip(1 - t * 2.6, 0, 1))
    cv.add(VIOLET, v8.gauss(d, 6 + 9 * fx.ease_out(t)) * 0.5 * (1 - t) ** 1.5 * fade)
    ring = 5 + 13 * fx.ease_out(t, 2.2)                       # a faint shell of light breaking up
    tex = v8._cart_noise(w, 2622, cell=w * v8.SS // 6)
    cv.add(VIOLET, v8.gauss(np.abs(d - ring), 1.2 + 1.5 * t) * (0.3 + 0.7 * tex) * 0.5 * (1 - t) ** 2 * fade)
    motes(cv, np.random.default_rng(2621), c, t, 22, (10, 21), (0.5, 1.0), VIOLET, (240, 220, 255), fade)
    return artkit.additive(cv.image())


def needler_pieces():
    """Ivory shards: the carapace split into five jagged plates, the two claws and the thorn
    launcher snapped off, four leg bits."""
    rng = np.random.default_rng(2631)
    pieces = []
    for k in range(5):                                       # carapace plates around the rim
        a = TAU * k / 5 + 0.3 + rng.uniform(-0.2, 0.2)
        cx, cy = 0.3 * np.cos(a), -0.1 + 0.22 * np.sin(a)
        poly = fx.jagged(rng, rng.uniform(0.42, 0.5), 7)

        def geo(q, cx=cx, cy=cy, poly=poly, bend=rng.uniform(0.6, 1.2)):
            r = q.copy()
            r[:, 0] -= cx
            r[:, 1] -= cy
            r[:, 2] -= 0.12 - bend * (r[:, 0] ** 2 + r[:, 1] ** 2)   # a curved piece of shell
            return sd_plate(r, poly, 0.0, 0.045, 0.012)
        c0 = (cx, cy, 0.0)
        pieces.append(dict(geo=geo, mat=em.V_BODY if k % 2 else em.V_SEAM, c0=c0, dir=outward(rng, c0, 0.3),
                           reach=rng.uniform(0.55, 0.85), axis=rng.uniform(0, TAU),
                           tumble=rng.uniform(2, 3.5) * rng.choice([-1, 1]), spin=rng.uniform(-2.5, 2.5),
                           rise=0.2, glow=0.4))
    for side in (1, -1):                                     # the claws, torn off whole
        def geo(q, side=side):
            r = q.copy()
            r[:, 0] = r[:, 0] * side
            return union((sd_capsule(r, (0.36, 0.22, 0.0), (0.58, 0.55, 0.0), 0.1, 0.09), 0),
                         (sd_ellipsoid(r, (0.6, 0.72, 0.0), (0.18, 0.22, 0.12)), 0),
                         (sd_capsule(r, (0.52, 0.86, 0.0), (0.38, 1.02, 0.0), 0.06, 0.015), 0),
                         (sd_capsule(r, (0.68, 0.9, 0.0), (0.62, 1.06, 0.0), 0.05, 0.012), 0), k=0.04)[0]
        c0 = (side * 0.55, 0.62, 0.0)
        pieces.append(dict(geo=geo, mat=em.V_BONE, c0=c0, dir=outward(rng, c0, 0.25), reach=rng.uniform(0.5, 0.7),
                           axis=rng.uniform(0, TAU), tumble=rng.uniform(4, 7) * rng.choice([-1, 1]),
                           spin=rng.uniform(1.5, 3.5) * side))

    def thorn(q):
        return sd_capsule(q, (0, 0.2, 0.14), (0, 0.72, 0.06), 0.08, 0.035)
    pieces.append(dict(geo=thorn, mat=em.V_BONE, c0=(0.0, 0.46, 0.1), dir=(0.15, 1.0), reach=0.75,
                       axis=0.0, tumble=7.0, spin=4.0, rise=0.25))
    for k, (y, a) in enumerate(((0.1, 0.25), (-0.32, -0.3), (-0.12, 0.0), (-0.5, -0.6))):
        side = 1 if k % 2 == 0 else -1
        ex, ey = 0.95, y + np.sin(a) * 0.35

        def geo(q, side=side, y=y, ex=ex, ey=ey):
            r = q.copy()
            r[:, 0] = r[:, 0] * side
            return np.minimum(sd_capsule(r, (0.5, y, -0.02), (ex - 0.12, ey + 0.05, -0.08), 0.05, 0.035),
                              sd_capsule(r, (ex - 0.12, ey + 0.05, -0.08), (ex, ey - 0.12, -0.14), 0.035, 0.012))
        c0 = (side * 0.75, y, 0.0)
        pieces.append(dict(geo=geo, mat=em.V_DARK if k < 2 else em.V_BONE, c0=c0, dir=outward(rng, c0, 0.4),
                           reach=rng.uniform(0.45, 0.7), axis=rng.uniform(0, TAU),
                           tumble=rng.uniform(6, 10) * rng.choice([-1, 1]), spin=rng.uniform(-4, 4)))
    return pieces


def needler_tatters(i):
    (w, h), n, _, _, _ = SETS["needler-tatters"]
    scene, mats = pieces_scene(needler_pieces(), em.vrell_scheme_mats("needler", "b"), fx.t_of(i, n), em._face_down)
    return artkit.native(*artkit.render_hi(scene, mats, (w, h), extent("needler-tatters", NEEDLER_PX)))


# =========================================================================== Scuttler

SCUTTLER_PX = l04.SCUT_SIZE[0]                  # 64
SCUT_BACK = 0.44                                # the back's half width in model units (archetype_models)


def scuttler_glow(i):
    """The lime organic burst: a lime-white flash from the back, ichor globules with short
    trails sagging a little, an olive mist billowing low and thinning, motes."""
    (w, h), n, _, _, _ = SETS["scuttler-death"]
    t = fx.t_of(i, n)
    rng = np.random.default_rng(2641)
    c = w / 2
    px = w / extent("scuttler-death", SCUTTLER_PX)        # px per model unit
    back = SCUT_BACK * px                                   # ~12 px
    cv = v8.Canvas(w, h)
    d = cv.dist(c, c)
    fade = np.clip((w / 2 - d) / 5.0, 0, 1)
    tex = v8._cart_noise(w, 2642, cell=w * v8.SS // 6)
    churn = v8._shift(tex, t * w * 0.25, -t * w * 0.2)
    grow = fx.ease_out(t, 2.2)
    dens = v8.gauss(d, back * (1.3 + 1.6 * grow))
    haze = np.clip(dens * (0.5 + 0.9 * churn) - 0.2 - 0.5 * t, 0, 1)
    env = np.clip((i + 1) / 2.0, 0.4, 1) * (1 - t) ** 0.8
    cv.add((48, 72, 8), haze * 1.3 * env * fade)                     # the mist's olive body
    cv.add(LIME, np.clip(haze - 0.45, 0, 1) * 0.6 * env * fade)
    flash = max(0.0, 1 - i / 4.0)
    cv.add((235, 255, 200), v8.gauss(d, back * (0.6 + 0.5 * t)) * 1.3 * flash)
    cv.add(LIME, v8.gauss(d, back * (1.1 + 0.8 * t)) * 0.8 * flash)
    drops = [(rng.uniform(0, TAU), rng.uniform(0.5, 1.0), rng.uniform(0.6, 1.5), rng.uniform(0.5, 1.4),
              rng.uniform(0, 0.15), rng.uniform(0.8, 1.3)) for _ in range(44)]
    for a, start, reach, size, delay, decay in drops:
        if t < delay:
            continue
        tt = np.clip((t - delay) / (1 - delay), 0, 1)

        def at(u, a=a, start=start, reach=reach):
            r = back * start + back * 1.6 * reach * fx.ease_out(u, 2.6)
            return c + np.cos(a) * r, c + np.sin(a) * r + 2.0 * u * u
        x, y = at(tt)
        xp, yp = at(max(tt - 0.1, 0.0))
        life = np.clip(1.1 - tt * decay, 0, 1)
        cv.add(LIME, v8.gauss(cv.seg(xp, yp, x, y), size * 0.8) * 0.85 * life * fade)
        if size > 1.0:
            cv.add((225, 255, 190), v8.gauss(cv.dist(x, y), size * 0.6) * 1.0 * life * fade)
    return artkit.additive(cv.image())


def scuttler_pieces():
    """Six legs torn off at the hips and the claws, shell fragments chipped off the back."""
    rng = np.random.default_rng(2651)
    pieces = []
    for i, (hy, a0) in enumerate(l04.LEG_BASE):
        for side in (1, -1):
            ox, oy = np.cos(a0), np.sin(a0)
            hip = (0.34, hy, 0.08)
            knee = (0.34 + 0.42 * ox, hy + 0.42 * oy, 0.34)
            foot = (0.34 + l04.LEG_REACH * ox, hy + l04.LEG_REACH * oy, -0.08)
            stub = tuple(a + 0.35 * (b - a) for a, b in zip(hip, knee))   # the husk keeps the stubs

            def geo(q, side=side, stub=stub, knee=knee, foot=foot):
                r = q.copy()
                r[:, 0] = r[:, 0] * side
                return union((sd_capsule(r, stub, knee, 0.08, 0.07), 0), (sd_sphere(r, knee, 0.075), 0),
                             (sd_capsule(r, knee, foot, 0.065, 0.02), 0), k=0.025)[0]
            c0 = (side * (0.34 + 0.5 * ox), hy + 0.5 * oy, 0.1)
            pieces.append(dict(geo=geo, mat=em.V_BONE if (i + (side > 0)) % 2 else em.V_BODY, c0=c0,
                               dir=outward(rng, c0, 0.35), reach=rng.uniform(0.45, 0.8), ease=3.0,
                               axis=rng.uniform(0, TAU), tumble=rng.uniform(3, 6) * rng.choice([-1, 1]),
                               spin=rng.uniform(-2.5, 2.5), rise=0.3, shrink_at=0.7))
    for side in (1, -1):                                    # the mandible claws
        def geo(q, side=side):
            r = q.copy()
            r[:, 0] = r[:, 0] * side
            return sd_capsule(r, (0.1, 0.48, 0.05), (0.18, 0.7, 0.0), 0.05, 0.015)
        c0 = (side * 0.14, 0.6, 0.03)
        pieces.append(dict(geo=geo, mat=em.V_BONE, c0=c0, dir=outward(rng, c0, 0.3), reach=rng.uniform(0.5, 0.7),
                           ease=3.0, axis=rng.uniform(0, TAU), tumble=rng.uniform(6, 9) * rng.choice([-1, 1]),
                           spin=rng.uniform(-4, 4), rise=0.2, shrink_at=0.7))
    for k in range(7):                                      # shell fragments off the back
        a = TAU * k / 7 + rng.uniform(-0.25, 0.25)
        cx, cy = 0.3 * np.cos(a), -0.05 + 0.27 * np.sin(a)
        poly = fx.jagged(rng, rng.uniform(0.25, 0.3), 6)

        def geo(q, cx=cx, cy=cy, poly=poly):
            r = q.copy()
            r[:, 0] -= cx
            r[:, 1] -= cy
            r[:, 2] -= 0.2 - 1.2 * (r[:, 0] ** 2 + r[:, 1] ** 2)   # curved like the back
            return sd_plate(r, poly, 0.0, 0.035, 0.01)
        c0 = (cx, cy, 0.2)
        pieces.append(dict(geo=geo, mat=em.V_SEAM if k % 2 == 0 else em.V_BODY, c0=c0, dir=outward(rng, c0, 0.3),
                           reach=rng.uniform(0.6, 1.0), ease=2.6, axis=rng.uniform(0, TAU),
                           tumble=rng.uniform(4, 7) * rng.choice([-1, 1]), spin=rng.uniform(-3, 3),
                           rise=0.4, glow=0.5, shrink_at=0.65))
    return pieces


def scuttler_tatters(i):
    (w, h), n, _, _, _ = SETS["scuttler-tatters"]
    # built facing +Y like vrell_l04's walker: heading 0 (down the screen) is a half turn
    scene, mats = pieces_scene(scuttler_pieces(), em.vrell_scheme_mats("scuttler", "a"), fx.t_of(i, n),
                               lambda p: rotate_z(p, np.pi))
    return artkit.native(*artkit.render_hi(scene, mats, (w, h), extent("scuttler-tatters", SCUTTLER_PX)))


# =========================================================================== build

MAKERS = {"skitter-death": skitter_glow, "skitter-tatters": skitter_tatters,
          "needler-death": needler_glow, "needler-tatters": needler_tatters,
          "scuttler-death": scuttler_glow, "scuttler-tatters": scuttler_tatters}
PARTS = {unit: [f"{unit}-tatters", f"{unit}-death"] for unit in ("skitter", "needler", "scuttler")}


def _frame(job):
    name, i = job
    return MAKERS[name](i)


def build(unit):
    with ProcessPoolExecutor() as pool:
        for name in PARTS[unit]:
            size, n, _, colours, _ = SETS[name]
            frames = list(pool.map(_frame, [(name, i) for i in range(n)]))
            assert all(f.size == size for f in frames), (name, frames[0].size)
            artkit.write_frames(name, artkit.quantize_set(frames, colours), SOURCE)


# =========================================================================== review

BURST = {"skitter": "explosion-tiny", "needler": "explosion-small", "scuttler": "explosion-medium"}
CONCEPT = {"skitter": DESIGN / "enemies" / "air" / "skitter" / "concept",
           "needler": DESIGN / "enemies" / "air" / "needler" / "concept",
           "scuttler": DESIGN / "enemies" / "ground" / "scuttler" / "concept"}


def layers_of(unit):
    """The layers as the game stacks them: pieces, the ladder burst (2 steps a frame), the glow."""
    tatters, glow = f"{unit}-tatters", f"{unit}-death"
    return [(artkit.load_frames(tatters), SETS[tatters][2], False, SETS[tatters][4]),
            (artkit.load_frames(BURST[unit]), 2, True, 0),
            (artkit.load_frames(glow), SETS[glow][2], True, SETS[glow][4])]


def row(name, zoom):
    frames = artkit.load_frames(name)
    _, n, steps, _, delay = SETS[name]
    glow = name.endswith("-death")
    late = f", {delay} STEPS LATE" if delay else ""
    return (f"{name.upper()}, {STEP // steps} FPS ({steps} STEPS{late}), {'ADDITIVE' if glow else 'SOLID'}",
            frames, zoom, glow)


def unit_frame(unit, step):
    """The unit as it flies (or walks) before it dies, for the loop."""
    frames = artkit.load_frames(unit)
    if unit == "skitter":
        return frames[(step // 6) % air.FRAMES]              # heading 0, the wing beat at 10 fps
    if unit == "scuttler":
        return frames[(step // 6) % l04.WALK_FRAMES]          # heading 0, walking down
    return frames[(step // 6) % len(frames)]


def review(unit):
    size = SETS[f"{unit}-tatters"][0]
    layers = layers_of(unit)
    span = max(len(f) * s + dl for f, s, _, dl in layers)
    zoom = {"skitter": 6, "needler": 4, "scuttler": 2}[unit]
    together = fx.composite_strip(layers, span, size, 1)
    rows = [row(f"{unit}-tatters", zoom), row(f"{unit}-death", zoom),
            (f"TOGETHER WITH THE {BURST[unit].upper()} BURST (2 STEPS A FRAME), EVERY 2ND STEP; PIECES UNDER THE GLOWS",
             together, zoom, False),
            ("1X", artkit.load_frames(f"{unit}-tatters"), 1, False),
            ("1X", artkit.load_frames(f"{unit}-death"), 1, True)]
    if unit == "scuttler":
        husk = artkit.load_frames("scuttler-husk")[0]
        on_husk = []
        for frame in together:
            cell = Image.new("RGBA", size, (34, 38, 30, 255))
            sprite.paste_center(cell, husk, size[0] / 2, size[1] / 2)
            cell.alpha_composite(frame.convert("RGBA"))
            on_husk.append(cell)
        rows.insert(3, ("OVER THE HUSK IT LEAVES (VRELL_L04.PY), ON LUNA GREY; EVERY 2ND STEP", on_husk[::2], zoom, False))
    sheet = artkit.review_sheet(f"{unit.upper()} DEATH - FINAL EFFECTS", rows, width=1700, batch=BATCH)
    # the loop: three units dying one after the other, as in play
    fw, fh, fps = {"skitter": (150, 100, 30), "needler": (180, 110, 30), "scuttler": (240, 150, 30)}[unit]
    ground = unit == "scuttler"
    plate = (34, 38, 30, 255) if ground else artkit.PLATE
    spots = [(fw * 0.22, fh * 0.5, 10), (fw * 0.5, fh * 0.42, 34), (fw * 0.78, fh * 0.56, 58)]
    husk = artkit.load_frames("scuttler-husk")[0] if ground else None
    gif = []
    for f in range(int(fps * 2.6)):
        step = f * 2
        cell = Image.new("RGBA", (fw, fh), plate)
        for x, y, die in spots:
            if step < die:
                sprite.paste_center(cell, unit_frame(unit, step), x, y)
            elif husk is not None:
                sprite.paste_center(cell, husk, x, y)
        for x, y, die in spots:
            for frames, steps, glow, delay in layers:
                fr = fx.timed(frames, steps, step - die - delay)
                if fr is None:
                    continue
                xy = (int(round(x - fr.width / 2)), int(round(y - fr.height / 2)))
                if glow:
                    cell = artkit.add_light(cell, fr, xy)
                else:
                    cell.alpha_composite(fr, xy)
        gif.append(sprite.enlarge(cell, 3 if unit != "scuttler" else 2))
    artkit.save_review(sheet, gif, CONCEPT[unit], f"{unit}-death", fps=fps)


if __name__ == "__main__":
    artkit.REVIEW_ROUND = REVIEW_ROUND
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    for unit in args or list(PARTS):
        if "--review" not in sys.argv[1:]:
            build(unit)
        review(unit)
