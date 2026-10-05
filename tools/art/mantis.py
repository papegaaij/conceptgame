#!/usr/bin/env python3
"""Production art: the Mantis, Level 06's side sniper (design/enemies/air/mantis; M4 part F batch).

Outputs (assets/sprites/, sizes from the data.yaml; every frame its own render under the fixed key
light, nothing lit rotated or mirrored afterwards):
  mantis_0..27         80x80, the chosen round-04 model (render/enemy_models.mantis_a, re-posed:
                       wings that beat, raptorial arms that open, an eye that charges), hanging
                       nose down at the side of the play field and leaning inward. Indexed
                       side * 14 + frame, side 0 = on the left edge (leaning right, into the field),
                       side 1 = on the right edge (its own render, not a mirror image):
                         0..3    hover: a wing-beat loop (10 fps), arms folded, leaning 20 degrees in
                         4..7    telegraph: the arms open and the crimson eye charges over the 0.6 s
                                 telegraph, the body rearing to 30 degrees in
                         8..9    sweep: arms wide, eye blazing, a two-frame flicker
                         10..13  exit (and nothing else): leaning 25 degrees out, the wings beating
                                 harder, as it leaves through its edge
  mantis-death_0..11   96x96, additive: the crimson flash and sparks of the medium burst
  mantis-tatters_0..11 96x96, solid: bone chitin shards, the two wings and the raptorial arms flung
                       out and tumbling (the stat block's "bone shards, crimson flash")
  design/enemies/air/mantis/concept/mantis-final-r23-a.png/.gif

The game picks the frame by the side the Mantis holds and its state (FarsideLooks.mantisFrame);
the death sets play with its medium burst, 4 steps a frame. One 40-colour palette over the 28 unit
frames, 24 over each death set.

Run: python3 tools/art/mantis.py [--review]   (~1 min)
"""
import sys
from concurrent.futures import ProcessPoolExecutor

import numpy as np
import yaml
from PIL import Image

import artkit
from artkit import DESIGN, TAU, sprite

import vfx_r08 as v8  # noqa: E402  (concept script, imported unchanged)
from render import enemy_models as em  # noqa: E402
from render.enemy_models import V_BODY, V_BONE, V_DARK, V_EYE, V_GLOW, V_SEAM  # noqa: E402
from render.enemy_rigs import model_rotation, model_space_materials  # noqa: E402
from render.sdf import (mirror_x, rotate_x, rotate_y, rotate_z, sd_capsule, sd_ellipsoid,  # noqa: E402
                        sd_plate, sd_sphere, union)

SCRIPT = "mantis.py"
BATCH = "M4 part F batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
REVIEW_ROUND = "r23"
UNIT = DESIGN / "enemies" / "air" / "mantis"
SLUG = "mantis"
SPEC = yaml.safe_load((UNIT / "data.yaml").read_text(encoding="utf-8"))
SIZE = tuple(SPEC["size"])                       # 80x80
EXTENT = 2.45                                    # model units across the canvas (the concept: 2.3 on 60 px)
COLOURS = 40
DEATH = 12
DEATH_SIZE = 96
DEATH_EXTENT = EXTENT * DEATH_SIZE / SIZE[0]
CRIMSON = np.array(em.hx(em.GLOWS["crimson"])) * 255
PER_SIDE = 14
# per frame of a side: (state, lean in degrees toward the field, wing beat (rad), arms open 0..1, eye charge)
WING = [0.05, 0.55, 0.95, 0.55]
FRAMES = ([("hover", 20, WING[i], 0.0, 1.0) for i in range(4)]
          + [("telegraph", 22 + 2.7 * i, 0.3, (i + 1) / 4, 1.0 + 0.6 * (i + 1)) for i in range(4)]
          + [("sweep", 30, 0.3, 1.0, 3.6), ("sweep", 30, 0.45, 1.0, 3.0)]
          + [("exit", -25, [0.15, 0.85, 1.15, 0.85][i], 0.0, 1.0) for i in range(4)])
assert len(FRAMES) == PER_SIDE


def model(wing, arms, eye):
    """The round-04 Mantis (enemy_models.mantis_a) facing +Y: long thorax, raptorial arms (opening
    with ``arms``, wider than the concept's hint so the sweep reads), translucent wings beating
    about their roots (``wing``, rad), the crimson eyes and the emitter charged by ``eye``."""
    wing_poly = [(0.1, 0.05), (0.62, -0.1), (0.9, -0.6), (0.72, -0.7), (0.08, -0.3)]
    arm_open = 0.25 + 0.45 * arms

    def scene(p):
        q = mirror_x(p)
        a = em._hinge(q, (0.14, 0.35, 0.0), arm_open, axis="z")
        a = em._hinge(a, (0.14, 0.35, 0.0), -0.25 * arms, axis="y")      # the forearms lift as they open
        w = rotate_y(q - np.array([0.1, -0.1, -0.04]), -wing) + np.array([0.1, -0.1, -0.04])
        d, m = union(
            (sd_ellipsoid(p, (0, -0.3, 0), (0.2, 0.55, 0.16)), V_SEAM),
            (sd_capsule(p, (0, 0.1, 0.02), (0, 0.62, 0.02), 0.12, 0.09), V_BODY),
            (sd_ellipsoid(p, (0, 0.72, 0.03), (0.2, 0.14, 0.12)), V_BODY),
            k=0.08,
        )
        d, m = union(
            (d, m),
            (sd_capsule(a, (0.14, 0.35, 0.0), (0.46, 0.62, 0.02), 0.07, 0.05), V_BODY),
            (sd_capsule(a, (0.46, 0.62, 0.02), (0.3, 1.02, 0.0), 0.05, 0.02), V_BONE),
            (sd_plate(w, wing_poly, -0.04, 0.025, 0.01, taper=lambda x, y: np.clip(1.2 - x, 0.3, 1)), V_DARK),
            (sd_capsule(q, (0.1, -0.7, 0.0), (0.26, -1.0, -0.02), 0.04, 0.01), V_DARK),
            k=0.05,
        )
        return union(
            (d, m),
            (sd_sphere(q, (0.12, 0.78, 0.1), 0.06 + 0.008 * (eye - 1)), V_GLOW),
            (sd_sphere(p, (0, 0.88, 0.02), 0.07 + 0.01 * (eye - 1)), V_EYE),
            k=0.02,
        )
    mats = em.vrell_scheme_mats(SLUG, "a", 1.0)
    for slot in (V_GLOW, V_EYE):
        mats[slot].emission = tuple(np.array(mats[slot].emission) * eye)
    return scene, mats


def heading_of(side, lean):
    """Radians clockwise from straight down: nose down, leaning ``lean`` degrees toward the field
    (to the right for the left side)."""
    return np.radians(-lean if side == 0 else lean)


def turned(scene, mats, h):
    rot = model_rotation(np.pi / 2 + h)
    return (lambda p: scene(rotate_z(p, rot))), model_space_materials(mats, rot)


def render_frame(job):
    side, k = job
    _, lean, wing, arms, eye = FRAMES[k]
    scene, mats = turned(*model(wing, arms, eye), heading_of(side, lean))
    return artkit.native(*artkit.render_hi(scene, mats, SIZE, EXTENT))


# --------------------------------------------------------------------------- the death

def ease_out(t, k=2.0):
    return 1 - (1 - np.clip(t, 0, 1)) ** k


def death_glow(i):
    """The crimson flash in the thorax, a ring of crimson sparks and white-hot motes, fading."""
    t = i / (DEATH - 1)
    n = DEATH_SIZE
    c = n / 2
    rng = np.random.default_rng(2306)
    cv = v8.Canvas(n, n)
    d = cv.dist(c, c)
    cv.add((255, 235, 225), v8.gauss(d, 4 + 8 * t) * np.clip(1 - t * 3.0, 0, 1))
    cv.add(tuple(CRIMSON), v8.gauss(d, 9 + 16 * ease_out(t)) * 0.7 * (1 - t) ** 1.6)
    fade = np.clip((n / 2 - d) / 5.0, 0, 1)
    for _ in range(36):
        a = rng.uniform(0, TAU)
        reach = rng.uniform(14, 40)
        size = rng.uniform(0.6, 1.3)
        delay = rng.uniform(0, 0.2)
        if t < delay:
            continue
        tt = (t - delay) / (1 - delay)
        r = 5 + reach * ease_out(tt, 2.5)
        x, y = c + np.cos(a) * r, c + np.sin(a) * r
        life = np.clip(1.15 - tt * rng.uniform(0.9, 1.3), 0, 1)
        dd = cv.dist(x, y)
        cv.add((255, 220, 210) if size > 1.0 else tuple(CRIMSON), v8.gauss(dd, size) * 1.4 * life * fade)
        cv.add(tuple(CRIMSON), v8.gauss(dd, size * 2.6) * 0.22 * life * fade)
    return artkit.additive(cv.image())


def tatters_scene(t):
    """The pieces at time t (0..1): the thorax split in three bone shards, the two wings and the
    two raptorial arms torn off, flung out along their own directions, tumbling, shrinking at the end."""
    _, mats = model(0.3, 0.0, 0.25)
    rng = np.random.default_rng(2307)
    pieces = [("wing", s, a) for s, a in ((1, 0.5), (-1, np.pi - 0.5))]
    pieces += [("arm", s, a) for s, a in ((1, -0.9), (-1, np.pi + 0.9))]
    pieces += [("shard", k, a) for k, a in enumerate((np.pi / 2, -np.pi / 2 + 0.3, -np.pi / 2 - 0.4))]
    spins = {j: rng.uniform(3, 7) * rng.choice([-1, 1]) for j in range(len(pieces))}
    reach = {j: rng.uniform(0.45, 0.8) for j in range(len(pieces))}
    shrink = np.clip(1.0 - (t - 0.55) / 0.45 * 0.7, 0.3, 1.0)
    wing_poly = [(0.0, 0.1), (0.52, -0.05), (0.8, -0.55), (0.62, -0.65), (-0.02, -0.25)]

    def scene(p):
        items = []
        for j, (kind, s, a) in enumerate(pieces):
            out = reach[j] * ease_out(t, 2.0)
            q = p - np.array([np.cos(a) * out, np.sin(a) * out, 0.0])
            if kind == "wing":
                q = q - np.array([0.35 * s, -0.25, 0.0])
                q = rotate_x(rotate_z(q, spins[j] * t * 0.5), spins[j] * t * 0.6) / shrink
                q[:, 0] *= s
                items.append((sd_plate(q, wing_poly, 0.0, 0.025, 0.01) * shrink, V_DARK))
            elif kind == "arm":
                q = q - np.array([0.3 * s, 0.6, 0.0])
                q = rotate_y(rotate_z(q, spins[j] * t), spins[j] * t * 0.4) / shrink
                items.append((sd_capsule(q, (0, -0.25, 0), (0.1 * s, 0.05, 0), 0.06, 0.045) * shrink, V_BODY))
                items.append((sd_capsule(q, (0.1 * s, 0.05, 0), (-0.05 * s, 0.4, 0), 0.045, 0.015) * shrink, V_BONE))
            else:
                y0 = (0.5, -0.1, -0.55)[s]
                q = q - np.array([0.0, y0, 0.0])
                q = rotate_x(rotate_z(q, spins[j] * t), spins[j] * t * 0.7) / shrink
                r = (0.14, 0.17, 0.15)[s]
                items.append((sd_ellipsoid(q, (0, 0, 0), (r, r * 1.6, r * 0.8)) * shrink, V_BODY if s == 0 else V_SEAM))
        return union(*items)
    return scene, mats


def death_tatters(i):
    scene, mats = tatters_scene(i / (DEATH - 1))
    return artkit.native(*artkit.render_hi(scene, mats, (DEATH_SIZE, DEATH_SIZE), DEATH_EXTENT))


# --------------------------------------------------------------------------- build

def _job(job):
    kind, arg = job
    return {"unit": render_frame, "glow": death_glow, "tatters": death_tatters}[kind](arg)


def build():
    jobs = ([("unit", (side, k)) for side in (0, 1) for k in range(PER_SIDE)]
            + [("glow", i) for i in range(DEATH)] + [("tatters", i) for i in range(DEATH)])
    with ProcessPoolExecutor() as pool:
        out = list(pool.map(_job, jobs))
    n = 2 * PER_SIDE
    artkit.write_frames(SLUG, artkit.quantize_set(out[:n], COLOURS), SOURCE)
    artkit.write_frames(f"{SLUG}-death", artkit.quantize_set(out[n:n + DEATH], 24), SOURCE)
    artkit.write_frames(f"{SLUG}-tatters", artkit.quantize_set(out[n + DEATH:], 24), SOURCE)


# --------------------------------------------------------------------------- review

def beam_overlay(img, x, y, heading, length=300, width=6):
    """The beam as the game draws it today (FarsideLooks: a crimson stroke with a white core),
    from the unit's centre; review only."""
    cv = v8.Canvas(img.width, img.height, ss=2)
    x1, y1 = x - np.sin(heading) * length, y + np.cos(heading) * length
    d = cv.seg(x, y, x1, y1)
    cv.add(tuple(CRIMSON), (d <= width / 2).astype(float) * 0.9)
    cv.add((255, 230, 230), (d <= max(1, width / 6)).astype(float) * 0.9)
    return artkit.add_light(img, artkit.additive(cv.image()))


def review():
    artkit.REVIEW_ROUND = REVIEW_ROUND
    frames = artkit.load_frames(SLUG)
    glow = artkit.load_frames(f"{SLUG}-death")
    pieces = artkit.load_frames(f"{SLUG}-tatters")
    left, right = frames[:PER_SIDE], frames[PER_SIDE:]
    sheet = artkit.review_sheet("MANTIS - FINAL SPRITES", [
        ("LEFT EDGE: HOVER 0-3, TELEGRAPH 4-7", left[:8], 2, False),
        ("LEFT EDGE: SWEEP 8-9, EXIT 10-13", left[8:], 2, False),
        ("RIGHT EDGE (ITS OWN RENDERS): HOVER, TELEGRAPH", right[:8], 2, False),
        ("RIGHT EDGE: SWEEP, EXIT", right[8:], 2, False),
        ("DEATH: CRIMSON FLASH (ADDITIVE, 15 FPS)", glow, 1, True),
        ("DEATH: BONE SHARDS, WINGS, ARMS (SOLID)", pieces, 1, False),
        ("1X", [left[0], left[6], left[8], left[10], right[0], right[6], right[8], right[10]], 1, False)],
        width=1400, batch=BATCH)
    # GIF: a pincer at 2x of a 240x300 strip of the field: enter, hover, two sweeps, exit; then the death
    gif = []
    W, H = 480, 300
    for i in range(72):
        t = i / 10
        img = Image.new("RGBA", (W, H), (14, 16, 24, 255))
        for side in (0, 1):
            edge = 40 if side == 0 else W - 40
            out = -40 if side == 0 else W + 40
            if t < 1.0:
                x, k = out + (edge - out) * t, i % 4
            elif t < 6.4:
                hold = t - 1.0
                x = edge
                ph = hold % 3.0
                if 0.6 <= ph < 1.2:
                    k = 4 + min(3, int((ph - 0.6) / 0.15))
                elif 1.2 <= ph < 2.4:
                    k = 8 + i % 2
                else:
                    k = i % 4
            else:
                x, k = edge + (out - edge) * (t - 6.4) / 0.8, 10 + i % 4
            set_ = left if side == 0 else right
            sprite.paste_center(img, set_[k], x, 110)
            if 8 <= k <= 9:
                ph = ((t - 1.0) % 3.0 - 1.2) / 1.2
                centre = np.pi / 4 * (-1 if side == 0 else 1)     # the ship's bearing, half-way down
                half = np.radians(35) * (-1 if side == 0 else 1)
                h = centre - half + 2 * half * ph                   # from the inward edge toward down
                img = beam_overlay(img, x, 110, h)
        gif.append(img)
    for f in range(DEATH):
        img = Image.new("RGBA", (W, H), (14, 16, 24, 255))
        sprite.paste_center(img, pieces[f], 120, 110)
        g = glow[f]
        img = artkit.add_light(img, g, (120 - g.width // 2, 110 - g.height // 2))
        gif.extend([img])
    gif = [sprite.enlarge(g, 2) for g in gif]
    artkit.save_review(sheet, gif, UNIT / "concept", SLUG, fps=10)


if __name__ == "__main__":
    if "--review" not in sys.argv[1:]:
        build()
    review()
