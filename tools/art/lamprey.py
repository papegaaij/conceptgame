#!/usr/bin/env python3
"""Production art: the Lamprey, Level 12's eel chaser that latches onto the Stormhawk and drains its
shield (design/enemies/air/lamprey; M5 part F batch, concept round 34). Straight to production from
its chosen concept (user decision F8 = a): the round-06 model lamprey-r06-a
(tools/concept/render/r06_models.lamprey, imported unchanged), with the latch's look shown a/b.

Outputs (assets/sprites/; every pose its own render under the fixed key light, nothing lit rotated
or mirrored afterwards; one 32-colour palette over the body sets):
  lamprey_0..63           36x36, `16 angles`, the chase: 16 headings x 4 swim frames, indexed
                          heading * 4 + frame (EnemyLooks.frame at 10 fps); heading k flies
                          k x 22.5 degrees clockwise from straight down (4 flies left, 8 up, 12
                          right). The teal teeth ring is emissive in the frame itself. A 1 px
                          lavender-white light rim (creeper.light_rim, 0.7) keeps the rust body
                          apart from the dark storm sea.
  lamprey-latched_0..11   36x36, the latched pose at its three hull spots, indexed spot * 4 + frame
                          (spots: 0 left, 1 right, 2 tail; 10 fps): facing in (left spot heading 12,
                          right spot heading 4, tail spot heading 8), the mouth disc flattened onto
                          the hull. Latch look a (the game's names; b only in the review, written
                          under these names by `--latch b`):
                            a "gulp": the body nearly still, swelling and narrowing in a 4-frame
                              swallow (0.4 s a gulp); the game pulses the ship's own shield ring
                              (ship-shield, additive) with each gulp: 0.55 / 0.3 / 0.12 / 0 of its
                              strength on frames 0-3, i.e. a gulp of shield light drawn into the eel
                            b "siphon": the tail lashing (the swim cycle at 0.6 latch), and
                              lamprey-drain_0..3 (32x32, additive, 10 fps) at the contact point:
                              cyan motes of the shield streaming into a teal glow in the mouth
  lamprey-stunned_0..7    36x36, thrown off: the body curled into a C, the teeth dimmed (dazed),
                          tumbling a full turn in 8 frames (the frame index = the turn, 12 fps over
                          the 1 s stun); then it flees with the chase frames
  lamprey-death_0..7      48x48, additive: the `small` wet pop's teal flash, glowing droplets and a
                          spray haze (4 steps a frame, with explosion-small)
  lamprey-tatters_0..7    48x48, solid: the body torn in three, the mouth disc with its teeth ring,
                          the fin spines; flung out, tumbling, shrivelling (4 steps a frame)
  pivots/lamprey.json     the frame counts and the three hull spots: where the latched sprite's
                          centre sits relative to the ship's centre (px, y down; the Stormhawk's
                          level frame ship_2), the mouth 2 px over the hull: the latch's `spots`
                          offsets for the data (step 4)
  design/enemies/air/lamprey/concept/lamprey-final-r34-a.png/.gif
  design/enemies/air/lamprey/concept/latch-r34-a.png/.gif, latch-r34-b.png/.gif

How the game draws it (EnemyLooks / ShipLooks, step 6): chasing, flying on and fleeing as any
16-angle unit with its shadow; latched: lamprey-latched_<spot * 4 + frame> at the ship's centre plus
the spot's offset, drawn over the ship (no shadow of its own), with the gulp's shield pulse (a);
stunned: lamprey-stunned_<frame> with its shadow; death: explosion-small + lamprey-death (additive)
+ lamprey-tatters (solid, under the glow), 4 steps a frame. The sprites stay unclaimed (SpriteUse)
until the Lamprey's data.yaml exists (step 4).

Run: python3 tools/art/lamprey.py [--latch a|b] [--review]   (~1 min; --review only rebuilds the
review files from assets/sprites)
"""
import json
import sys
from concurrent.futures import ProcessPoolExecutor

import numpy as np
from PIL import Image, ImageDraw

import artkit
from artkit import DESIGN, ROOT, TAU, sprite, write_gif

import creeper  # noqa: E402  (tools/art: the light rim, the pieces' scene)
import vfx_r08 as v8  # noqa: E402  (concept script, imported unchanged)
import vrell_deaths as vd  # noqa: E402  (tools/art: the motes)
import vrell_fx as fx  # noqa: E402  (tools/art: easing, timing)
from render import enemy_models as em  # noqa: E402
from render import r06_models as m6  # noqa: E402
from render import raster  # noqa: E402
from render.enemy_models import V_BONE, V_DARK, V_GLOW, V_SEAM  # noqa: E402
from render.enemy_rigs import model_space_materials  # noqa: E402
from render.sdf import rotate_z, sd_capsule, sd_ellipsoid, sd_sphere, subtract, union  # noqa: E402

# =========================================================================== parameters

SCRIPT = "lamprey.py"
BATCH = "M5 part F batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
REVIEW_ROUND = "r34"
CONCEPT = DESIGN / "enemies" / "air" / "lamprey" / "concept"
STEP = 60

SIZE = (36, 36)                                 # the stat block's 36x36
EXTENT = 2.3                                    # the concept's model units across the sprite
HEADINGS = 16
SWIM = 4
FPS = 10                                        # EnemyLooks.ORGANIC_FPS
COLOURS = 32
RIM_STRENGTH = 0.7

SPOTS = ("left", "right", "tail")
SPOT_HEADING = {"left": 12, "right": 4, "tail": 8}   # facing in
SHIP_FRAME = 2                                  # the Stormhawk's level frame
SPOT_CONTACT = {"left": (18, 37), "right": (29, 37), "tail": (24, 43)}   # hull px on ship_2 (48x48)
OVERLAP = 2                                     # the mouth this many px over the hull
GULP = (1.0, 1.07, 1.12, 1.05)                  # a: the body's width over the swallow
GULP_PULSE = (0.55, 0.3, 0.12, 0.0)             # a: the ship's shield ring with each gulp frame
LASH = 0.6                                      # b: the latch value of the lashing pose

STUN, STUN_BEND, STUN_GLOW = 8, 0.75, 0.35
DEATH_SIZE = (48, 48)
DEATH = 8
DEATH_STEPS = 4
DEATH_EXTENT = EXTENT * DEATH_SIZE[0] / SIZE[0]
DRAIN_SIZE = (32, 32)
TEAL = np.array(em.hx(em.GLOWS["teal"])) * 255
SHIELD_CYAN = np.array([120, 225, 255], float)
WHITE_TEAL = (225, 255, 240)


# =========================================================================== model

def rotation(k):
    return np.pi - TAU * k / HEADINGS           # +Y forward -> heading k, as wraith.py


def model(k, phase=0.0, latched=0.0, glow=1.0, width=1.0, bend=0.0):
    """(scene, mats) at heading k: the r06 model, optionally widened (the gulp) or bent into a C
    (the stun) in its own frame before it is turned."""
    rot = rotation(k)
    base, mats = m6.lamprey(phase, latched, glow)

    def own(p):
        q = p
        if bend:                                # each slice across the body turned by bend x y: a C
            q = p.copy()
            ang = bend * p[:, 1]
            c, s = np.cos(ang), np.sin(ang)
            q[:, 0], q[:, 1] = c * p[:, 0] - s * p[:, 1], s * p[:, 0] + c * p[:, 1]
        if width != 1.0:
            q = q.copy()
            q[:, 0] = q[:, 0] / width
        d = base(q)
        return (d[0] * min(1.0, width) * (0.8 if bend else 1.0), d[1])
    return (lambda p: own(rotate_z(p, rot))), model_space_materials(mats, rot)


def render(k, **kw):
    img = artkit.native(*artkit.render_hi(*model(k, **kw), SIZE, EXTENT))
    return creeper.light_rim(img, RIM_STRENGTH)


def render_swim(k, j):
    return render(k, phase=TAU * j / SWIM)


def render_latched(variant, spot, j):
    k = SPOT_HEADING[spot]
    if variant == "a":
        return render(k, phase=0.0, latched=1.0, width=GULP[j])
    return render(k, phase=TAU * j / SWIM, latched=LASH)


def render_stunned(i):
    return render(2 * i, phase=0.8, glow=STUN_GLOW, bend=STUN_BEND)


# =========================================================================== effects

def drain_glow(i):
    """b's siphon at the contact point: cyan shield motes streaming in from the hull side (the
    sprite's left half; the game turns nothing, so it is drawn for the left spot and mirrored in
    light only, which is unlit) into a teal glow in the mouth; 4 frames, additive."""
    w, h = DRAIN_SIZE
    cv = v8.Canvas(w, h)
    cx, cy = w / 2, h / 2
    d = cv.dist(cx, cy)
    beat = 0.75 + 0.25 * np.cos(TAU * i / 4)
    fade = np.clip((w / 2 - d) / 3.0, 0, 1)
    cv.add(WHITE_TEAL, v8.gauss(d, 1.6) * 0.9 * beat)
    cv.add(tuple(TEAL), v8.gauss(d, 4.0) * 0.55 * beat * fade)
    rng = np.random.default_rng(3401)
    for m in range(14):
        a0 = rng.uniform(0, TAU)
        r0 = rng.uniform(9, 14)
        u = ((i / 4) + m / 14) % 1.0               # each mote's way in, a quarter per frame
        r = r0 * (1 - u) + 1.5
        a = a0 + 1.4 * u                          # spiralling in
        x, y = cx + np.cos(a) * r, cy + np.sin(a) * r
        s = 0.55 + 0.35 * u
        cv.add(tuple(SHIELD_CYAN), v8.gauss(cv.dist(x, y), s) * 1.2 * np.sin(np.pi * u) ** 0.6 * fade)
    return artkit.additive(cv.image())


def death_glow(i):
    """The `small` wet pop: a white-teal flash from the mouth, a teal bloom, glowing droplets flung
    out and falling short, a thin spray haze."""
    w, h = DEATH_SIZE
    t = fx.t_of(i, DEATH)
    c = w / 2
    cv = v8.Canvas(w, h)
    d = cv.dist(c, c)
    fade = np.clip((w / 2 - d) / 4.0, 0, 1)
    cv.add(WHITE_TEAL, v8.gauss(d, 2.2 + 4 * t) * 1.4 * np.clip(1 - t * 2.8, 0, 1))
    cv.add(tuple(TEAL), v8.gauss(d, 5 + 9 * fx.ease_out(t)) * 0.55 * (1 - t) ** 1.5 * fade)
    tex = v8._cart_noise(w, 3411, cell=w * v8.SS // 5)
    haze = np.clip(v8.gauss(d, 6 + 10 * fx.ease_out(t, 2.2)) * (0.5 + 0.8 * tex) - 0.25 - 0.6 * t, 0, 1)
    cv.add((40, 90, 84), haze * 1.1 * (1 - t) * fade)
    vd.motes(cv, np.random.default_rng(3412), c, t, 20, (8, 19), (0.6, 1.1), tuple(TEAL), WHITE_TEAL, fade,
             delay=0.05, gain=1.2)
    return artkit.additive(cv.image())


def death_pieces():
    """The body torn in three, the mouth disc with its teeth, the fin spines (r06_models.lamprey's
    parts at phase 0)."""
    rng = np.random.default_rng(3421)
    pieces = []
    ys = [0.62 - 0.27 * i for i in range(7)]
    for a, b in ((0, 2), (2, 4), (4, 6)):          # the body in three
        def body(q, a=a, b=b):
            return union(*[(sd_capsule(q, (0, ys[i], 0.1), (0, ys[i + 1], 0.1), 0.25 - 0.028 * i,
                                       0.222 - 0.028 * i), 0) for i in range(a, b)], k=0.05)[0]
        c0 = (0.0, (ys[a] + ys[b]) / 2, 0.1)
        side = rng.choice([-1, 1])
        pieces.append(dict(parts=[(body, V_SEAM)], c0=c0,
                           dir=(float(side * rng.uniform(0.6, 1.0)), float(np.sign(c0[1]) * 0.6 + rng.uniform(-0.3, 0.3))),
                           reach=rng.uniform(0.35, 0.55), ease=2.4, axis=rng.uniform(0, TAU),
                           tumble=rng.uniform(3, 5) * rng.choice([-1, 1]), spin=rng.uniform(-3, 3),
                           rise=0.25, shrink_at=0.55))
    disc_y = ys[0] + 0.2

    def disc(q):
        dd = sd_ellipsoid(q, (0, disc_y, 0.14), (0.27, 0.23, 0.11))
        return subtract((dd, 0), sd_ellipsoid(q, (0, disc_y + 0.02, 0.3), (0.16, 0.13, 0.14)))[0]

    def teeth(q):
        return union(*[(sd_sphere(q, (0.27 * 0.72 * np.cos(a), disc_y + 0.02 + 0.27 * 0.6 * np.sin(a), 0.22),
                                  0.035), 0) for a in np.linspace(0, TAU, 12, endpoint=False)])[0]
    pieces.append(dict(parts=[(disc, V_DARK), (teeth, V_GLOW)], c0=(0.0, disc_y, 0.14), dir=(0.1, 1.0),
                       reach=0.45, ease=2.6, axis=0.3, tumble=2.5, spin=2.0, rise=0.3, shrink_at=0.6))
    for i in range(3, 6):                         # the fin spines
        def fin(q, i=i):
            return sd_capsule(q, (0, ys[i], 0.24), (0, ys[i + 1], 0.22), 0.03, 0.02)
        c0 = (0.0, (ys[i] + ys[i + 1]) / 2, 0.24)
        pieces.append(dict(parts=[(fin, V_BONE)], c0=c0,
                           dir=(float(rng.uniform(-1, 1)), float(rng.uniform(-1, 0.2))),
                           reach=rng.uniform(0.5, 0.8), ease=2.2, axis=rng.uniform(0, TAU),
                           tumble=rng.uniform(5, 8) * rng.choice([-1, 1]), spin=rng.uniform(-4, 4),
                           rise=0.3, shrink_at=0.5))
    return pieces


def death_tatters(i):
    mats = m6.lamprey(0.0, 0.0, 0.5)[1]
    scene, mats = creeper.pieces_scene(death_pieces(), mats, fx.t_of(i, DEATH), lambda p: rotate_z(p, np.pi))
    return artkit.native(*artkit.render_hi(scene, mats, DEATH_SIZE, DEATH_EXTENT))


# =========================================================================== build

def _job(job):
    name, args = job
    return {"swim": render_swim, "latched": render_latched, "stunned": render_stunned, "drain": drain_glow,
            "death": death_glow, "tatters": death_tatters}[name](*args)


def latched_cells(variant):
    return [(variant, s, j) for s in SPOTS for j in range(SWIM)]


def spot_offsets(latched):
    """Per spot: the latched sprite's centre relative to the ship's centre, the mouth OVERLAP px over
    the hull at the spot's contact pixel (the mouth's extreme opaque pixel along its facing)."""
    out = {}
    for s, spot in enumerate(SPOTS):
        a = np.array(latched[s * SWIM])[..., 3] > 0
        ys, xs = np.nonzero(a)
        cx, cy = SIZE[0] / 2, SIZE[1] / 2
        hx, hy = SPOT_CONTACT[spot]
        if spot == "left":                         # facing right: the mouth is the rightmost pixel
            mouth = xs.max() + 1 - cx
            out[spot] = [round(hx - 24 + OVERLAP - mouth), round(hy - 24 - (ys[xs == xs.max()].mean() + 0.5 - cy))]
        elif spot == "right":
            mouth = cx - xs.min()
            out[spot] = [round(hx - 24 - OVERLAP + mouth), round(hy - 24 - (ys[xs == xs.min()].mean() + 0.5 - cy))]
        else:                                      # facing up: the topmost pixel
            mouth = cy - ys.min()
            out[spot] = [round(hx - 24 - (xs[ys == ys.min()].mean() + 0.5 - cx)), round(hy - 24 - OVERLAP + mouth)]
    return out


def build(variant="a"):
    swim = [(k, j) for k in range(HEADINGS) for j in range(SWIM)]
    with ProcessPoolExecutor() as pool:
        def run(name, arglist):
            return list(pool.map(_job, [(name, a) for a in arglist]))
        body = run("swim", swim) + run("latched", latched_cells(variant)) + run("stunned", [(i,) for i in range(STUN)])
        glow = run("death", [(i,) for i in range(DEATH)])
        tatters = run("tatters", [(i,) for i in range(DEATH)])
        drain = run("drain", [(i,) for i in range(SWIM)]) if variant == "b" else None
    body = artkit.quantize_set(body, COLOURS)
    n_swim, n_lat = len(swim), len(SPOTS) * SWIM
    sets = [("lamprey", body[:n_swim]), ("lamprey-latched", body[n_swim:n_swim + n_lat]),
            ("lamprey-stunned", body[n_swim + n_lat:]), ("lamprey-death", artkit.quantize_set(glow, 24)),
            ("lamprey-tatters", artkit.quantize_set(tatters, 24))]
    if drain:
        sets.append(("lamprey-drain", artkit.quantize_set(drain, 16)))
    else:
        artkit.write_frames("lamprey-drain", [], SOURCE)   # a's look has none: clear a b build's
    for name, frames in sets:
        artkit.write_frames(name, frames, SOURCE)
        print(f"{name}: {len(frames)} frames, {frames[0].width}x{frames[0].height}")
    artkit.write_pivots("lamprey", SOURCE, {
        "headings": HEADINGS, "swim_frames": SWIM, "latched_index": "spot * 4 + frame (spots left, right, tail)",
        "latch_look": variant, "gulp_pulse": list(GULP_PULSE) if variant == "a" else None,
        "stunned_frames": STUN, "ship_frame": f"ship_{SHIP_FRAME}",
        "spots": spot_offsets(sets[1][1])})


# =========================================================================== review

def storm_plate(w, h, y0=0):
    """A stand-in for Level 12's storm sea (its backdrop is step 5's): Level 11's swell tile, darkened
    toward the storm's slate and cooled."""
    src = Image.open(ROOT / "assets" / "backdrop" / "level-11" / "swell.png").convert("RGB")
    tile = np.array(src).astype(np.float64)
    reps = int(np.ceil((y0 + h) / tile.shape[0])) + 1
    a = np.concatenate([tile] * reps, axis=0)[y0:y0 + h, :w]
    lum = a.mean(axis=-1, keepdims=True)
    a = (a * 0.55 + lum * 0.25) * 0.62 + np.array([14, 18, 24])
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGB").convert("RGBA")


def rain(cell, t, seed=5, count=26):
    d = ImageDraw.Draw(cell)
    rng = np.random.default_rng(seed)
    w, h = cell.size
    for _ in range(count):
        x0, y0, v = rng.uniform(0, w), rng.uniform(0, h), rng.uniform(380, 460)
        y = (y0 + v * t) % (h + 20) - 10
        x = (x0 + v * t * 0.25) % w
        d.line([(x, y), (x - 2, y - 9)], fill=(150, 166, 178, 70))


def paste(cell, fr, x, y):
    cell.alpha_composite(fr, (int(round(x - fr.width / 2)), int(round(y - fr.height / 2))))


def add_centred(cell, glow, x, y, gain=1.0):
    if gain <= 0:
        return cell
    if gain < 1:
        a = np.array(glow).astype(np.float64)
        a[..., :3] *= gain
        glow = Image.fromarray(a.astype(np.uint8), "RGBA")
    return artkit.add_light(cell, glow, (int(round(x - glow.width / 2)), int(round(y - glow.height / 2))))


def shadow(frame, opacity=0.4):
    a = np.array(frame)[..., 3] > 127
    img = Image.fromarray(np.dstack([np.zeros(a.shape + (3,), np.uint8), (a * int(255 * opacity)).astype(np.uint8)]),
                          "RGBA")
    return img.resize((max(1, round(img.width * 0.85)), max(1, round(img.height * 0.85))), Image.NEAREST)


def heading_of(dx, dy):
    return int(round(np.arctan2(-dx, dy) / TAU * HEADINGS)) % HEADINGS


def shield_bar(cell, x, y, value, step, draining=True):
    """The HUD's shield row as a stand-in (ShipPanel: cyan bar on its dark track, the label in the
    warning colour while drained), flickering 8 steps on, 8 off as after a break (HUD spec)."""
    d = ImageDraw.Draw(cell)
    on = not draining or (step // 8) % 2 == 1
    raster.draw_text(cell, x, y, "SHIELD DRAIN" if draining else "SHIELD",
                     (255, 120, 60) if draining else (150, 170, 190))
    d.rectangle([x, y + 12, x + 100, y + 19], fill=(0, 42, 42, 255))
    if on:
        d.rectangle([x, y + 12, x + int(100 * value), y + 19], fill=(0, 255, 255, 255))
    raster.draw_text(cell, x + 106, y + 10, f"{int(round(100 * value)):3d}", (150, 230, 240))


def latched_draw(cell, art, variant, spot, x, y, step):
    """A ship at (x, y) with a Lamprey latched at `spot`: the latch look of `variant`."""
    j = (step * FPS // STEP) % SWIM
    lat = art[f"latched-{variant}"][SPOTS.index(spot) * SWIM + j]
    ox, oy = art["spots"][spot]
    paste(cell, art["ship"], x, y)
    if variant == "a":
        cell = add_centred(cell, art["shield"][(step // 4) % len(art["shield"])], x, y, GULP_PULSE[j])
    paste(cell, lat, x + ox, y + oy)
    if variant == "b":
        hx, hy = SPOT_CONTACT[spot]
        cell = add_centred(cell, art["drain"][j], x + hx - 24, y + hy - 24)
    return cell


def load_art():
    art = {"swim": artkit.load_frames("lamprey"), "stunned": artkit.load_frames("lamprey-stunned"),
           "death": artkit.load_frames("lamprey-death"), "tatters": artkit.load_frames("lamprey-tatters"),
           "boom": artkit.load_frames("explosion-small"), "ship": artkit.load_frames("ship")[SHIP_FRAME],
           "shield": artkit.load_frames("ship-shield"),
           "spots": json.loads((artkit.PIVOTS / "lamprey.json").read_text())["spots"]}
    art["latched-a"] = artkit.load_frames("lamprey-latched")
    # b's look for the review only (the game's names hold a): rendered here, not written
    with ProcessPoolExecutor() as pool:
        out = list(pool.map(_job, [("latched", c) for c in latched_cells("b")] + [("drain", (i,)) for i in range(SWIM)]))
    n = len(SPOTS) * SWIM
    art["latched-b"] = artkit.quantize_set(out[:n], COLOURS)
    art["drain"] = artkit.quantize_set(out[n:], 16)
    return art


def latch_review(art, variant):
    name = {"a": "GULP: THE BODY SWALLOWS, THE SHIP'S SHIELD RING PULSES WITH EACH GULP",
            "b": "SIPHON: THE TAIL LASHES, CYAN SHIELD MOTES STREAM INTO THE MOUTH"}[variant]
    spots = []
    for spot in SPOTS:
        for step in (0, 6, 12, 18):
            cell = storm_plate(96, 96, 200)
            cell = latched_draw(cell, art, variant, spot, 48, 40, step)
            spots.append(cell)
    sheet = artkit.review_sheet(f"LAMPREY LATCH {variant.upper()} - {name}", [
        ("LATCHED POSE, LEFT / RIGHT / TAIL SPOT, 4 FRAMES EACH (10 FPS)", art[f"latched-{variant}"], 2, False),
        ("ON THE STORMHAWK AT THE THREE SPOTS, EVERY 0.1 S (STORM SEA STAND-IN)", spots, 1, False),
        ("THE SAME AT 2X: LEFT, RIGHT, TAIL", [spots[0], spots[5], spots[10]], 2, False)]
        + ([("SIPHON GLOW AT THE CONTACT POINT (ADDITIVE, 4 FRAMES)", art["drain"], 3, True)] if variant == "b" else []),
        width=1300, batch=BATCH)
    gif = []
    for f in range(48):                          # 2.4 s at 20 fps: 3 steps a frame
        step = f * 3
        cell = storm_plate(300, 150, 300 + step // 2)
        rain(cell, step / STEP)
        for i, spot in enumerate(SPOTS):
            cell = latched_draw(cell, art, variant, spot, 50 + 100 * i, 62, step)
        shield_bar(cell, 70, 118, max(0.0, 0.8 - 0.08 * step / STEP), step)
        gif.append(sprite.enlarge(cell, 2))
    png = CONCEPT / f"latch-{REVIEW_ROUND}-{variant}.png"
    sheet.convert("RGB").save(png, optimize=True)
    write_gif(gif, CONCEPT / f"latch-{REVIEW_ROUND}-{variant}.gif", fps=20, colors=128)
    print(f"review: {png.relative_to(ROOT)} (+ .gif)")


def review_loop(art):
    """A 240x300 strip of the storm stand-in: a Lamprey chases the ship (the 180 deg/s turn), latches
    on its left side, the ship shakes left-right, the Lamprey is thrown off (120 px/s over the 1 s
    stun, tumbling), flees left; a second one is shot on its way in (the wet pop)."""
    fw, fh, fps = 240, 300, 20
    gif = []
    ship = np.array([120.0, 220.0])
    pos, vel = np.array([30.0, -20.0]), np.array([60.0, 200.0])
    state, t_state, thrown = "chase", 0.0, None
    second = np.array([200.0, -30.0])
    die_at = 2.2
    value = 0.9
    for f in range(int(7.0 * fps)):
        t = f / fps
        step = int(t * STEP)
        sx = ship[0]
        if state == "latched" and t - t_state > 0.6:
            sx = 120 + 60 * np.sin((t - t_state - 0.6) * 14)    # the shake: reversals 0.22 s apart
        cell = storm_plate(fw, fh, 400 + int(t * 70))
        rain(cell, t, count=40)
        sh = (sx, ship[1])
        j = int(t * FPS) % SWIM
        # the second Lamprey: straight in, shot at die_at
        if t < die_at:
            second = second + np.array([-20.0, 150.0]) / fps
            fr = art["swim"][heading_of(-20, 150) * SWIM + j]
            sd = shadow(fr)
            cell.alpha_composite(sd, (int(second[0] + 14 - sd.width / 2), int(second[1] + 22 - sd.height / 2)))
            paste(cell, fr, *second)
        else:
            age = int((t - die_at) * STEP)
            for frames_, steps_, glowing in ((art["tatters"], DEATH_STEPS, False), (art["boom"], 2, True),
                                             (art["death"], DEATH_STEPS, True)):
                fr = fx.timed(frames_, steps_, age)
                if fr is None:
                    continue
                cell = add_centred(cell, fr, *second) if glowing else (paste(cell, fr, *second) or cell)
        if state == "chase":
            want = np.array(sh) - pos
            ang, cur = np.arctan2(want[1], want[0]), np.arctan2(vel[1], vel[0])
            turn = (ang - cur + np.pi) % TAU - np.pi
            cur += np.clip(turn, -np.pi / fps, np.pi / fps)       # 180 deg/s
            vel = np.array([np.cos(cur), np.sin(cur)]) * 220
            pos = pos + vel / fps
            fr = art["swim"][heading_of(*vel) * SWIM + j]
            sd = shadow(fr)
            cell.alpha_composite(sd, (int(pos[0] + 14 - sd.width / 2), int(pos[1] + 22 - sd.height / 2)))
            paste(cell, fr, *pos)
            paste(cell, art["ship"], *sh)
            if np.hypot(*(pos - np.array(sh))) < 22:
                state, t_state = "latched", t
        elif state == "latched":
            cell = latched_draw(cell, art, "a", "left", sh[0], sh[1], step)
            if t - t_state > 0.6 + 4 * 0.22 + 0.05:
                state, t_state = "stunned", t
                ox, oy = art["spots"]["left"]
                thrown = np.array([sh[0] + ox, sh[1] + oy])
        else:
            paste(cell, art["ship"], *sh)
            u = t - t_state
            if u < 1.0:                                       # 120 px/s slowing to a stop over 1 s
                p = thrown + np.array([-1.0, 0.0]) * 120 * (u - u * u / 2)
                fr = art["stunned"][int(u * 12) % STUN]
            else:
                p = thrown + np.array([-60.0 - 220 * (u - 1.0), 0.0])
                fr = art["swim"][4 * SWIM + j]
            sd = shadow(fr)
            cell.alpha_composite(sd, (int(p[0] + 14 - sd.width / 2), int(p[1] + 22 - sd.height / 2)))
            paste(cell, fr, *p)
        if state == "latched":
            value = max(0.0, 0.9 - 0.08 * (t - t_state))     # 8 a second of 100, the regen paused
        shield_bar(cell, 8, 6, value, step, state == "latched")
        gif.append(sprite.enlarge(cell, 2))
    return gif


def review():
    artkit.REVIEW_ROUND = REVIEW_ROUND
    art = load_art()
    heading = [art["swim"][k * SWIM:(k + 1) * SWIM] for k in range(HEADINGS)]
    together = fx.composite_strip([(art["tatters"], DEATH_STEPS, False, 0), (art["boom"], 2, True, 0),
                                   (art["death"], DEATH_STEPS, True, 0)], DEATH * DEATH_STEPS, DEATH_SIZE, 1)

    def on_plate(fr, size=56):
        cell = storm_plate(size, size, 120)
        paste(cell, fr, size / 2, size / 2)
        return cell
    sheet = artkit.review_sheet("LAMPREY - FINAL SPRITES", [
        ("HEADINGS 0-15 (CLOCKWISE FROM DOWN: 4 FLIES LEFT, 8 UP, 12 RIGHT), SWIM FRAME 0", [h[0] for h in heading], 2, False),
        (f"SWIM, HEADING 0 AND HEADING 6, {FPS} FPS", heading[0] + heading[6], 3, False),
        ("LATCHED (LOOK A, THE GAME'S NAMES): LEFT / RIGHT / TAIL SPOT X 4 GULP FRAMES", art["latched-a"], 2, False),
        ("STUNNED: CURLED, TEETH DIMMED, A TURN IN 8 FRAMES (12 FPS)", art["stunned"], 2, False),
        (f"TATTERS, {STEP // DEATH_STEPS} FPS, SOLID", art["tatters"], 2, False),
        (f"WET POP GLOW, {STEP // DEATH_STEPS} FPS, ADDITIVE", art["death"], 2, True),
        ("TOGETHER WITH EXPLOSION-SMALL, EVERY 4 STEPS", together[::2], 2, False),
        ("1X ON THE STORM STAND-IN", [on_plate(h[0]) for h in heading[::2]] + [on_plate(art["stunned"][0])], 1, False)],
        width=1300, batch=BATCH)
    artkit.save_review(sheet, review_loop(art), CONCEPT, "lamprey", fps=20)
    for v in ("a", "b"):
        latch_review(art, v)


if __name__ == "__main__":
    artkit.REVIEW_ROUND = REVIEW_ROUND
    args = sys.argv[1:]
    look = args[args.index("--latch") + 1] if "--latch" in args else "a"
    if "--review" not in args:
        build(look)
    review()
