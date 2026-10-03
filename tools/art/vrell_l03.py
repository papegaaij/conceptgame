#!/usr/bin/env python3
"""Production art: the new sprites of Level 03, the Spore Bomber with its spore mine, the Whirl
Seed and the debris field's wreck chunks (design/enemies/air, design/world/earth-orbit; M4 part C
batch).

Outputs (assets/sprites/, unit sizes from the parts' data.yaml):
  spore-bomber_0..3.png   72x72, `orientation: fixed` (flies down the screen, head down): the idle
                          loop, the gas-bag membrane breathing and the spore bulbs pulsing (10 fps)
  spore-mine_0..3.png     14x14, additive: the drifting spore, a lime pulse (the game draws it
                          dimmer and smaller while it rises, full once armed)
  whirl-seed_0..7.png     26x26, `orientation: radial`: a six-fold spinner, frame k turned
                          k x 7.5 degrees clockwise, so the 8 frames cover 60 degrees and the loop
                          repeats seamlessly; every frame is its own render under the fixed key light
  debris-large-{a,b,c}.png  96x80, 72x64, 56x48: indestructible chunks, a frigate hull section, half
                          a CDF defence platform, a habitat module with a truss stub
  debris-small-{a,b}.png  32x28, 24x24: breakable chunks, a bent hull plate, a truss junction
  design/enemies/air/spore-bomber/concept/spore-bomber-final-r16-a.png/.gif
  design/enemies/air/whirl-seed/concept/whirl-seed-final-r16-a.png/.gif
  design/world/earth-orbit/concept/debris-final-r16-a.png   (sheet only: the chunks are static)

The Spore Bomber is the chosen round-04 model (tools/concept/enemies_r04.py `spore-bomber-a`,
render/enemy_models.spore_bomber_a, whose `anim` inflates the gas bag), rendered at 72 px with a
slightly tighter extent so it fills the larger sprite. The Whirl Seed is the chosen round-05 seed
(render/archetype_models.whirl_seed: its petal blade, core and eye, plum chitin, teal glow) with
six blades instead of five, as the approved design's six-fold spinner and its 60-degree loop
require; the blades are all the body chitin (the concept's alternating dark blade would make the
pattern repeat only every 120 degrees) and every material pattern is evaluated in the blade's own
frame, so the texture turns with the seed and frame 8 would equal frame 0. The core's teal seam
runs along each blade's axis, six seams at about half the concept's single one. The spore mine is a
2D light field like the pulse effects (vfx_r08's canvas), stored premultiplied on black.

The wreck chunks are not Vrell: they are the Earth-orbit station kit (render/station.py materials
in Level 01's muted kit palette KIT_PAL, tools/concept/backdrop_l01.py, 1 model unit = 1 px), its
frigate section, CDF platform and habitat drum broken along jagged cuts, hollow where torn open,
the lights dead, scorched along the breaks and in a few blast spots. Each is posed (turned and
tilted) in the model and rendered under the fixed key light, its panel patterns turning with it;
the game draws them unrotated. One 32-colour palette for the five chunks.

Run: python3 tools/art/vrell_l03.py [bomber] [seed] [debris] [--review]   (~15 s)
"""
import importlib.util
import sys
from concurrent.futures import ProcessPoolExecutor
from dataclasses import replace
from pathlib import Path

import numpy as np
import yaml
from PIL import Image

import artkit
from artkit import DESIGN, TAU, sprite

import enemies_r04 as e4  # noqa: E402  (concept script, imported unchanged)
import vfx_r08 as v8  # noqa: E402
from render import archetype_models  # noqa: E402,F401  (adds the Whirl Seed's role colours)
from render import enemy_models as em  # noqa: E402
from render import sdf, station  # noqa: E402
from render.sdf import (rotate_x, rotate_y, rotate_z, sd_box, sd_capsule, sd_cylinder_x,  # noqa: E402
                        sd_cylinder_z, sd_plate, sd_sphere, union)
from render.station import ACCENT, DARK, HULL, RED, WARM  # noqa: E402

# The concept Level 01 generator shares a name with tools/art/backdrop_l01.py, so it is loaded by path.
_spec = importlib.util.spec_from_file_location(
    "concept_backdrop_l01", Path(__file__).resolve().parents[1] / "concept" / "backdrop_l01.py")
l01 = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(l01)

SCRIPT = "vrell_l03.py"
BATCH = "M4 part C batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
REVIEW_ROUND = "r16"

# Spore Bomber: the concept model's anim (gas bag +-6 %) and glow per idle frame: deflated,
# inflating, full, deflating (the glow rises with the breath and falls behind it).
BOMBER_EXTENT = 2.15
BOMBER_ANIM = [-1.0, 0.0, 1.0, 0.0]
BOMBER_GLOW = [0.85, 1.05, 1.25, 0.95]
BOMBER_COLOURS = 32
MINE_SIZE = (14, 14)
MINE_PULSE = [0.55, 0.8, 1.0, 0.75]
MINE_SPINES = 6
LIME = np.array(em.hx(em.GLOWS["lime"])) * 255

# Whirl Seed: the concept's petal blade (archetype_models.whirl_seed), six of them.
SEED_EXTENT = 2.3
SEED_PETAL = [(0.12, 0.05), (0.55, 0.38), (1.0, 0.24), (0.92, 0.02), (0.3, -0.12)]
SEED_PETALS = 6
SEED_FRAMES = 8
SEED_STEP = TAU / SEED_PETALS / SEED_FRAMES     # 7.5 degrees clockwise per frame
SEED_COLOURS = 24
SEED_SEAM = 0.55                               # six seams at about half the concept's one, so the
                                               # core glows over about the concept's area

# Wreck chunks: id -> size (Level 03 debris field: large 48-96 px, small 24-32 px).
DEBRIS = {"debris-large-a": (96, 80), "debris-large-b": (72, 64), "debris-large-c": (56, 48),
          "debris-small-a": (32, 28), "debris-small-b": (24, 24)}
DEBRIS_COLOURS = 32
WRECK_TONE = 0.84                              # soot: the kit's surfaces a little duller


def size_of(slug):
    data = yaml.safe_load((DESIGN / "enemies" / "air" / slug / "data.yaml").read_text(encoding="utf-8"))
    return tuple(data["size"])


# --------------------------------------------------------------------------- Spore Bomber

def render_bomber(k):
    scene, mats = e4.R04["spore-bomber-a"][4](anim=BOMBER_ANIM[k], glow=BOMBER_GLOW[k])
    return artkit.native(*artkit.render_hi(scene, mats, size_of("spore-bomber"), BOMBER_EXTENT))


def spore_mine():
    """The drifting spore: a pale lime core in a lime membrane glow with six short spines, its
    light pulsing over four frames (additive, vfx_r08's light fields)."""
    w, h = MINE_SIZE
    out = []
    for s in MINE_PULSE:
        cv = v8.Canvas(w, h)
        cx, cy = w / 2, h / 2
        d = cv.dist(cx, cy)
        fade = np.clip((w / 2 - d) / 2.0, 0, 1)                   # keeps the halo inside the frame
        cv.add(LIME, v8.gauss(d, 2.4 + 1.0 * s) * 0.75 * s * fade)
        cv.add(LIME, v8.gauss(np.abs(d - 3.3), 0.55) * 0.5 * s * fade)   # the membrane
        for j in range(MINE_SPINES):
            a = TAU * j / MINE_SPINES + 0.3
            seg = cv.seg(cx + 3.4 * np.cos(a), cy + 3.4 * np.sin(a), cx + 5.6 * np.cos(a), cy + 5.6 * np.sin(a))
            cv.add(LIME, v8.gauss(seg, 0.42) * 0.55 * (0.6 + 0.4 * s) * fade)
        cv.add((235, 255, 190), v8.gauss(d, 1.2 + 0.35 * s) * (0.75 + 0.35 * s))
        out.append(artkit.additive(cv.image()))
    return artkit.quantize_set(out, 16)


# --------------------------------------------------------------------------- Whirl Seed

def in_frame(fn, to_local):
    """A material pattern evaluated in a local frame (it then turns with the part)."""
    return lambda p, n: fn(to_local(p), n)


def seed_model(spin):
    """The six-blade seed turned ``spin`` radians clockwise on screen."""
    sector = TAU / SEED_PETALS
    base = em.vrell_scheme_mats("whirl-seed", "a")

    def body(p):                       # screen -> seed frame
        return rotate_z(p, -spin)

    def blade(k):                      # screen -> the frame of blade k (blade along +x)
        return lambda p: rotate_z(body(p), k * sector)

    def nearest_blade(p):              # screen -> the frame of the nearest blade (the core's seams)
        q = body(p)
        k = np.round(np.arctan2(q[:, 1], q[:, 0]) / sector)
        c, s = np.cos(k * sector), np.sin(k * sector)
        r = q.copy()
        r[:, 0] = c * q[:, 0] + s * q[:, 1]
        r[:, 1] = -s * q[:, 0] + c * q[:, 1]
        return r

    mats = list(base)
    body_mat = base[em.V_BODY]
    first = len(mats)
    mats += [replace(body_mat, pattern=in_frame(body_mat.pattern, blade(k))) for k in range(SEED_PETALS)]
    seam = base[em.V_SEAM]
    seam_pattern = seam.emission_pattern

    def spokes(q, n):                  # the seam runs along each blade's axis, out of the eye
        r = q.copy()
        r[:, 0], r[:, 1] = q[:, 1], q[:, 0]
        return seam_pattern(r, n)
    mats[em.V_SEAM] = replace(seam, pattern=in_frame(seam.pattern, nearest_blade),
                              emission=tuple(np.array(seam.emission) * SEED_SEAM),
                              emission_pattern=in_frame(spokes, nearest_blade))

    def scene(p):
        items = [(sd_plate(blade(k)(p), SEED_PETAL, 0.0, 0.06, 0.02,
                           taper=lambda x, y: np.clip(1.2 - 0.8 * x, 0.3, 1)), first + k)
                 for k in range(SEED_PETALS)]
        d, m = union(*items, k=0.04)
        return union(
            (d, m),
            (sd_sphere(p, (0, 0, 0.05), 0.32), em.V_SEAM),
            (sd_sphere(p, (0, 0, 0.25), 0.16), em.V_EYE),
            k=0.05,
        )
    return scene, mats


def render_seed(k):
    scene, mats = seed_model(SEED_STEP * k)
    return artkit.native(*artkit.render_hi(scene, mats, size_of("whirl-seed"), SEED_EXTENT))


# --------------------------------------------------------------------------- wreck chunks

def tri(u):
    """Triangle wave in -1..1, period 1."""
    return 4 * np.abs(u - np.floor(u + 0.5)) - 1


def jag(u, amp, period, seed=0.0):
    """A torn edge's offset along a cut: two triangle waves of unrelated periods."""
    return amp * (0.6 * tri(u / period + seed) + 0.4 * tri(u / (period * 0.43) + 2.7 * seed))


def torn(d, q, axis, at, keep, amp=2.5, period=9.0, seed=0.0, slant=None):
    """``d`` cut along the plane ``q[axis] = at`` with a jagged edge; ``keep`` = -1 keeps the
    side below it, +1 the side above. ``slant`` = (axis, k) leans the plane by k px per px along
    that axis (a tear that opens towards the viewer)."""
    other = [i for i in range(3) if i != axis]
    edge = at + jag(q[:, other[0]] + 0.7 * q[:, other[1]], amp, period, seed)
    if slant:
        edge = edge + slant[1] * q[:, slant[0]]
    cut = keep * (edge - q[:, axis]) / (1.6 + abs(slant[1]) if slant else 1.6)   # keep it a bound
    return np.maximum(d, cut)


def shell(d, t):
    """The surface ``d`` as a hollow skin of thickness ``t`` (inside opened up by a cut)."""
    return np.maximum(d, -(d + t))


def pick(items):
    """Union without blending (each part keeps its edges)."""
    return union(*items)


def posed(turn, tilt=0.0, roll=0.0, shift=(0.0, 0.0)):
    """screen -> model: the model turned ``turn`` radians clockwise on screen, tilted about its
    x axis and rolled about its y axis, then moved by ``shift`` px (x right, y up)."""
    def to_model(p):
        q = p - np.array([shift[0], shift[1], 0.0])
        q = rotate_z(q, -turn)
        if tilt:
            q = rotate_x(q, tilt)
        if roll:
            q = rotate_y(q, roll)
        return q
    return to_model


def speckle(q, a=12.9898, b=78.233, c=0.0):
    return np.modf(np.abs(np.sin(q[:, 0] * a + q[:, 1] * b + q[:, 2] * c) * 43758.5453))[0]


def scorch_spots(spots, strength=0.8):
    """A multiplier darkening soft, speckled burn spots (x, y, radius in model px)."""
    def burn(q):
        b = np.zeros(len(q))
        for x, y, r in spots:
            b = np.maximum(b, np.clip(1.25 - np.hypot(q[:, 0] - x, q[:, 1] - y) / r, 0, 1))
        return 1 - strength * b * (0.55 + 0.45 * speckle(q))
    return burn


def scorch_edge(axis, at, width=7.0, strength=0.7, slant=None):
    """A multiplier darkening the band along a cut (the torn edge burnt)."""
    def burn(q):
        edge = at + (slant[1] * q[:, slant[0]] if slant else 0.0)
        b = np.clip(1 - np.abs(q[:, axis] - edge) / width, 0, 1) ** 1.5
        return 1 - strength * b * (0.5 + 0.5 * speckle(q, 39.346, 7.1, 11.135))
    return burn


def wreck_mats(to_model, burns):
    """The station kit's materials (KIT_PAL) with its patterns in the model's frame, the lights
    dead (red marker and windows dark), dulled by soot (WRECK_TONE) and darkened by the burns."""
    mats = station.materials(l01.KIT_PAL)
    dead = replace(mats[DARK], albedo=tuple(np.array(mats[DARK].albedo) * 0.8))
    mats[RED], mats[WARM] = dead, dead

    def finish(m):
        def pattern(p, n, f=m.pattern):
            q = to_model(p)
            v = (f(q, n) if f is not None else np.ones(len(p))) * WRECK_TONE
            for b in burns:
                v = v * b(q)
            return v
        return replace(m, pattern=pattern, emission=(0.0, 0.0, 0.0), emission_pattern=None)
    return [finish(m) for m in mats]


def frigate_chunk(shift):
    """A frigate's hull section (the kit's frigate_section, along x here) broken off at the bow
    end: the hull a hollow skin torn open on a slant, so the ring frames and the deck show in the
    tear; a blast hole behind the bridge, the engine bell at the stern."""
    r, half = 16.0, 30.0
    cut, slant = half - 2, (2, -0.8)                  # the top of the hull torn further back
    to_model = posed(np.radians(-28), tilt=np.radians(14), shift=shift)

    def scene(p):
        q = to_model(p)
        hull = sd_capsule(q, (-half, 0, 0), (half + r, 0, 0), r)
        d = shell(hull, 2.2)
        d = np.maximum(d, -sd_sphere(q, (-22, 6, r), 5.0))                  # blast hole
        d = torn(d, q, 0, cut, -1, amp=3.0, period=8.0, seed=0.2, slant=slant)
        frames = [(torn(np.maximum(np.abs(np.hypot(q[:, 1], q[:, 2]) - (r - 3.0)) - 1.6, np.abs(q[:, 0] - x) - 1.2),
                        q, 0, cut, -1, 3.0, 8.0, 0.2, slant), DARK) for x in (cut - 4, cut - 14, cut - 24)]
        deck = (torn(sd_box(q, (half * 0.3, 0, -4), (half * 0.7 + 6, r - 3.5, 1.0), 0.3), q, 0, cut + 2, -1, 2.0, 6.0, 0.6), DARK)
        bridge = (sd_box(q, (-4, 0, r - 2), (11, 7, 5), 1.5), DARK)
        bridge_top = (sd_box(q, (-6, 0, r + 3.2), (4, 4, 1.2), 0.5), HULL)
        band = (sd_box(q, (-half * 0.62, 0, 0), (2.6, r + 0.8, r + 0.8), 0.8), ACCENT)
        bell = (sd_cylinder_x(q, (-half - r + 3, 0, 0), r * 0.62, 5.0), DARK)
        bell_rim = (sd_cylinder_x(q, (-half - r - 2, 0, 0), r * 0.5, 1.5), HULL)
        return pick([(d, HULL), bridge, bridge_top, band, bell, bell_rim, deck] + frames)
    burns = [scorch_edge(0, cut, 9.0, 0.8, slant), scorch_spots([(-22, 6, 9), (8, -10, 6)], 0.7)]
    return scene, wreck_mats(to_model, burns)


def platform_chunk(shift):
    """Half of a CDF defence platform (the kit's platform at 0.66 scale): the deck ring, the gun
    dome and one array wing, torn across the deck and the dome; the sensor mast snapped."""
    s = 0.66
    model = l01.platform_model(False)
    to_model = posed(np.radians(16), tilt=np.radians(-10), roll=np.radians(12), shift=shift)
    cut = -7.0

    def scene(p):
        q = to_model(p)
        d, m = model(q / s)
        d = d * s
        d = np.maximum(d, -sd_sphere(q, (12, 17, 3), 6.5))                  # blast crater in the deck
        d = torn(d, q, 0, cut, 1, amp=3.0, period=7.0, seed=0.4, slant=(2, 0.5))
        inner = (torn(sd_cylinder_z(q, (0, 0, -0.5), 24.0, 1.8), q, 0, cut + 2.5, 1, 2.0, 5.0, 0.9), DARK)
        stub = (sd_capsule(q, (-2, 4, 8), (1, 11, 11), 1.6), DARK)          # the snapped mast
        return union((d, m), inner, stub)
    burns = [scorch_edge(0, cut, 9.0, 0.8, (2, 0.5)), scorch_spots([(12, 17, 10), (34, -10, 6)], 0.7)]
    return scene, wreck_mats(to_model, burns)


def module_chunk(shift):
    """A habitat module (the kit's drum, smaller) torn off at one end on a slant, hollow inside
    with a bulkhead showing, a broken truss stub still on its domed end."""
    drum, _ = station.drum_part(11, 14, windows=True)
    truss, _ = station.truss_h_part(9, 16)   # its rails run 9 px past each end
    to_model = posed(np.radians(54), tilt=np.radians(-10), shift=shift)
    cut, slant = 8.0, (2, -0.7)

    def scene(p):
        q = to_model(p)
        d, m = drum(q)
        d = shell(d, 2.0)
        d = torn(d, q, 1, cut, -1, amp=2.4, period=6.5, seed=0.1, slant=slant)
        bulk = (np.maximum(np.abs(q[:, 1] - (cut - 9)) - 1.0, np.hypot(q[:, 0], q[:, 2]) - 9.5), DARK)
        tq = rotate_z(q - np.array([0.0, -24.0, 0.0]), np.pi / 2)
        td, tm = truss(tq)
        td = torn(td, tq, 0, 1.0, -1, amp=1.8, period=5.0, seed=0.7)
        return union((d, m), bulk, (td, tm))
    burns = [scorch_edge(1, cut, 7.0, 0.8, slant), scorch_spots([(5, -8, 5)], 0.6)]
    return scene, wreck_mats(to_model, burns)


def plate_chunk(shift):
    """A bent hull plate torn out of a frigate: a curved skin patch with its panel lines and a
    stiffener rib, torn along two edges, the other two its plate seams."""
    rad = 26.0
    to_model = posed(np.radians(-24), tilt=np.radians(10), roll=np.radians(-8), shift=shift)

    def scene(p):
        q = to_model(p)
        bend = np.hypot(q[:, 0], q[:, 2] + rad) - rad                       # bent about y, top at z=0
        d = np.abs(bend + 1.5) - 1.5
        d = torn(d, q, 0, 12.0, -1, 1.6, 5.0, 0.3)
        d = np.maximum(d, -12.0 - q[:, 0])                                  # a seam: a straight edge
        d = torn(d, q, 1, 7.0, -1, 1.4, 4.5, 0.5)
        d = np.maximum(d, -7.5 - q[:, 1])
        rib = (np.maximum(np.maximum(np.abs(q[:, 1] + 1.0) - 1.4, np.abs(bend - 0.6) - 1.0),
                          np.maximum(q[:, 0] - 10.0, -11.5 - q[:, 0])), ACCENT)
        return pick([(d, HULL), rib])
    burns = [scorch_spots([(7, 4, 6)], 0.7)]
    return scene, wreck_mats(to_model, burns)


def truss_chunk(shift):
    """A truss junction: a node block with three rail stubs, each snapped off, and a brace."""
    to_model = posed(np.radians(18), tilt=np.radians(14), roll=np.radians(-8), shift=shift)

    def scene(p):
        q = to_model(p)
        items = [(sd_box(q, (0, 0, 0), (3.6, 3.6, 3.0), 0.8), HULL),
                 (sd_box(q, (0, 0, 3.4), (2.0, 2.0, 0.6), 0.3), ACCENT)]
        for k, (a, length) in enumerate(((0.3, 10.0), (2.4, 8.5), (4.3, 9.5))):
            r = rotate_z(q, a)
            stub = sd_box(r, (length / 2 + 2, 0, 0), (length / 2 + 2, 2.0, 2.0), 0.5)
            items.append((torn(stub, r, 0, length, -1, 1.4, 3.0, 0.3 * k), HULL))
            items.append((torn(sd_box(r, (length / 2, 0, 2.2), (length / 2, 0.7, 0.4), 0.2), r, 0, length - 1.5,
                               -1, 1.2, 3.0, 0.3 * k), DARK))
        items.append((sd_capsule(q, (2.5, 3.0, -0.5), (7.5, 6.5, -0.5), 1.0), DARK))
        return pick(items)
    burns = [scorch_spots([(-3, 2, 5)], 0.6)]
    return scene, wreck_mats(to_model, burns)


CHUNKS = {"debris-large-a": frigate_chunk, "debris-large-b": platform_chunk, "debris-large-c": module_chunk,
          "debris-small-a": plate_chunk, "debris-small-b": truss_chunk}


def centred(name):
    """The chunk moved so its silhouette sits in the middle of its sprite: a quick unlit probe
    render on a larger canvas finds its bounds, which must fit inside the sprite."""
    w, h = DEBRIS[name]
    scene, mats = CHUNKS[name]((0.0, 0.0))
    pw, ph = int(w * 1.6), int(h * 1.6)
    probe = sdf.render(scene, mats, (pw * 2, ph * 2), float(pw), z_top=float(max(w, h)), steps=120, shadows=False)
    ys, xs = np.nonzero(probe[..., 3] > 0)
    x0, x1 = xs.min() / 2, (xs.max() + 1) / 2
    y0, y1 = ys.min() / 2, (ys.max() + 1) / 2
    assert x1 - x0 <= w - 2 and y1 - y0 <= h - 2, f"{name}: {x1 - x0:.0f}x{y1 - y0:.0f} does not fit {w}x{h}"
    shift = (pw / 2 - (x0 + x1) / 2, (y0 + y1) / 2 - ph / 2)     # px -> model (y up)
    return CHUNKS[name](shift)


def render_chunk(name):
    size = DEBRIS[name]
    scene, mats = centred(name)
    hi, factor = artkit.render_hi(scene, mats, size, float(size[0]), z_top=float(max(size)), steps=160)
    return artkit.native(hi, factor, crisp=60)


# --------------------------------------------------------------------------- build

def build(part):
    with ProcessPoolExecutor() as pool:
        if part == "bomber":
            frames = artkit.quantize_set(list(pool.map(render_bomber, range(len(BOMBER_ANIM)))), BOMBER_COLOURS)
            artkit.write_frames("spore-bomber", frames, SOURCE)
            artkit.write_frames("spore-mine", spore_mine(), SOURCE)
        elif part == "seed":
            frames = artkit.quantize_set(list(pool.map(render_seed, range(SEED_FRAMES))), SEED_COLOURS)
            artkit.write_frames("whirl-seed", frames, SOURCE)
        else:
            names = list(DEBRIS)
            chunks = artkit.quantize_set(list(pool.map(render_chunk, names)), DEBRIS_COLOURS)
            for name, img in zip(names, chunks):
                assert img.size == DEBRIS[name], (name, img.size)
                artkit.write_frames(name, [img], SOURCE, single=True)


# --------------------------------------------------------------------------- review

def scaled_glow(frame, scale, dim):
    """A glow frame as the game draws a rising spore: smaller and dimmer."""
    w = max(1, int(round(frame.width * scale)))
    img = frame.resize((w, w), Image.BILINEAR) if scale != 1 else frame
    a = np.array(img).astype(np.float64)
    a[..., :3] *= dim
    return Image.fromarray(a.astype(np.uint8), "RGBA")


def review_bomber():
    frames = artkit.load_frames("spore-bomber")
    mine = artkit.load_frames("spore-mine")
    sheet = artkit.review_sheet("SPORE BOMBER - FINAL SPRITES", [
        ("IDLE LOOP, 10 FPS (FLIES DOWN THE SCREEN, HEAD DOWN): MEMBRANE BREATHES, BULBS PULSE", frames, 4, False),
        ("SPORE MINE, PULSE (ADDITIVE)", mine, 8, True),
        ("SPORE MINE RISING (DRAWN AT 70 %, HALF BRIGHT) AND ARMED", [scaled_glow(mine[0], 0.7, 0.5), mine[2]], 8, True),
        ("1X", frames, 1, False),
        ("1X", mine, 1, True)], batch=BATCH)
    fw, fh = 200, 280
    fps, seconds = 20, 4.8
    rng = np.random.default_rng(31)
    drops = [(0.4 + 1.6 * i, rng.uniform(0, TAU)) for i in range(3)]
    gif = []
    for i in range(int(fps * seconds)):
        t = i / fps
        cell = Image.new("RGBA", (fw, fh), artkit.PLATE)
        bx, by = fw / 2, 24 + 45 * t
        mines = []
        for t0, a in drops:
            for t1 in (t0, t0 - seconds):             # a mine dropped last loop is still drifting
                age = t - t1
                if 0 <= age < seconds:
                    y0 = 24 + 45 * t1 + 20
                    mines.append((bx + 20 * age * np.cos(a), y0 + 20 * age * np.sin(a), age))
        sprite.paste_center(cell, frames[(i // 2) % len(frames)], bx, by)
        for x, y, age in mines:
            f = mine[(i // 2 + int(x)) % len(mine)]
            if age < 1.0:
                f = scaled_glow(f, 0.7, 0.5)
            cell = artkit.add_light(cell, f, (int(round(x - f.width / 2)), int(round(y - f.height / 2))))
        gif.append(sprite.enlarge(cell, 2))
    artkit.save_review(sheet, gif, DESIGN / "enemies" / "air" / "spore-bomber" / "concept", "spore-bomber", fps=fps)


def review_seed():
    frames = artkit.load_frames("whirl-seed")
    sheet = artkit.review_sheet("WHIRL SEED - FINAL SPRITES", [
        ("SPIN, 8 FRAMES OVER 60 DEGREES (7.5 DEG CLOCKWISE EACH; SIX-FOLD, THE LOOP REPEATS)", frames, 5, False),
        ("THE LOOP TWICE (120 DEGREES): FRAME 7 RUNS ON INTO FRAME 0", frames * 2, 3, False),
        ("1X", frames, 1, False)], batch=BATCH)
    fw, fh, fps = 220, 300, 30
    spin_rate = 1.5 * TAU                                  # rad/s, the data's 1.5 turns/s
    n, rel = 6, (fw / 2, 40.0)
    gif = []
    for i in range(int(fps * 3.4)):
        t = i / fps
        cell = Image.new("RGBA", (fw, fh), artkit.PLATE)
        for j in range(n):
            a0 = TAU * j / n
            ts = min(t, 1.5)
            ang = a0 + TAU * ts                            # one turn per second, clockwise on screen
            r = 90 * ts
            cx, cy = rel[0], rel[1] + 60 * ts
            x, y = cx + r * np.cos(ang), cy + r * np.sin(ang)
            if t > 1.5:                                    # then on along the outward angle, downward
                vx, vy = np.cos(ang), abs(np.sin(ang))
                x, y = x + 160 * (t - 1.5) * vx, y + 160 * (t - 1.5) * vy
                for _ in range(3):                         # side ricochets
                    if x < 8:
                        x = 16 - x
                    elif x > fw - 8:
                        x = 2 * (fw - 8) - x
            k = int(np.floor((spin_rate * t + j) / SEED_STEP)) % SEED_FRAMES
            sprite.paste_center(cell, frames[k], x, y)
        big = Image.new("RGBA", (fw, fh), artkit.PLATE)
        k = int(np.floor(spin_rate * t / SEED_STEP)) % SEED_FRAMES
        sprite.paste_center(big, sprite.enlarge(frames[k], 6), fw / 2, fh / 2)
        both = Image.new("RGBA", (fw * 2 + 4, fh), (0, 0, 0, 255))
        both.paste(big, (0, 0))
        both.paste(cell, (fw + 4, 0))
        gif.append(sprite.enlarge(both, 2))
    artkit.save_review(sheet, gif, DESIGN / "enemies" / "air" / "whirl-seed" / "concept", "whirl-seed", fps=fps)


def review_debris():
    chunks = {name: artkit.load_frames(name) for name in DEBRIS}
    rows = [("LARGE A, INDESTRUCTIBLE: A FRIGATE HULL SECTION, BROKEN AT THE BOW", chunks["debris-large-a"], 4, False),
            ("LARGE B: HALF A CDF DEFENCE PLATFORM", chunks["debris-large-b"], 4, False),
            ("LARGE C: A HABITAT MODULE WITH A TRUSS STUB", chunks["debris-large-c"], 4, False),
            ("SMALL A, BREAKABLE: A BENT HULL PLATE", chunks["debris-small-a"], 6, False),
            ("SMALL B: A TRUSS JUNCTION", chunks["debris-small-b"], 6, False),
            ("1X, ALL FIVE (ONE PALETTE)", [f for fs in chunks.values() for f in fs], 1, False)]
    sheet = artkit.review_sheet("DEBRIS FIELD - FINAL SPRITES", rows, batch=BATCH)
    png, _ = artkit.review_paths(DESIGN / "world" / "earth-orbit" / "concept", "debris")
    sheet.convert("RGB").save(png, optimize=True)
    print(f"review: {png.relative_to(artkit.ROOT)}")


PARTS = {"bomber": review_bomber, "seed": review_seed, "debris": review_debris}

if __name__ == "__main__":
    artkit.REVIEW_ROUND = REVIEW_ROUND
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    for part in args or list(PARTS):
        if "--review" not in sys.argv[1:]:
            build(part)
        PARTS[part]()
