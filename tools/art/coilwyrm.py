#!/usr/bin/env python3
"""Production art: the Coilwyrm, Level 06's segment chain (design/enemies/air/coilwyrm; M4 part F
batch).

Outputs (assets/sprites/; sizes from the data.yaml; every heading its own render under the fixed
key light, nothing lit rotated or mirrored afterwards):
  coilwyrm_0..191              58x58 head, 48 headings x 4 jaw frames (indexed heading * 4 + frame;
                               the jaw snapping shut, half, open, half at the game's 10 fps)
  coilwyrm-regrown_0..47       58x58 the head a cut chain grows: smaller, raw and wet (paler rust,
                               glossier, the teal brighter), jaw half open, 48 headings
  coilwyrm-segment_0..575      the 12 body segments' sizes of the data's taper (54, 52, 49, 47, 44,
                               42, 39, 37, 34, 32, 29, 27 px), 48 headings each, indexed
                               heading * 12 + size (size 0 = 54 px next to the head); a hard
                               chain's 14 segments take the nearest size
  coilwyrm-tail_0..47          40x40 finned tail, 48 headings
  coilwyrm-death_0..11         80x80, additive: the head's teal flash and glowing droplets (with the
                               medium burst, 4 steps a frame); also written as coilwyrm-regrown-death
  coilwyrm-tatters_0..11       80x80, solid: the skull split in two, the four bone horns, the
                               mandibles and the frills flung out and tumbling; also -regrown-tatters
  coilwyrm-segment-death_0..7  40x40, additive: the segment pop's teal glint (tiny: 2 steps a frame,
                               4 steps after the pop's flash); also written as coilwyrm-tail-death
  coilwyrm-segment-tatters_0..7 56x56, solid: the chitin ring broken in four, the fins and the dorsal
                               spine scattering
  coilwyrm-tail-tatters_0..7   48x48, solid: the tail split, its fins tumbling
  pivots/coilwyrm.json         the heading count, the head's jaw frames, the segment sizes
  design/enemies/air/coilwyrm/concept/coilwyrm-final-r23-a.png/.gif

Headings: 48, 7.5 degrees apart (heading k = k x 7.5 degrees clockwise from straight down). The
concept had 16 (22.5 degrees); along a chain each segment takes the path's tangent, so neighbours
on a curve differ by a few degrees and 16 steps showed as kinks; 48 keep the snake smooth at the
segments' size (a 7.5-degree step moves a 54 px segment's rim by at most 3.5 px). About 1.6 M px.

Models: the chosen round-08 Coilwyrm (tools/concept/render/archetype_models.coil_head,
coil_segment, coil_tail, the model-space material patterns of rerender_r08), unchanged; the death
pieces are posed from those models' parts. One 40-colour palette over the whole chain (head,
regrown head, segments, tail), so the parts match; 24 colours per death set.

Run: python3 tools/art/coilwyrm.py [--review]   (~3 min on 20 cores)
"""
import sys
from concurrent.futures import ProcessPoolExecutor
from dataclasses import replace

import numpy as np
import yaml
from PIL import Image

import artkit
from artkit import DESIGN, TAU, sprite

import vfx_r08 as v8  # noqa: E402  (concept script, imported unchanged)
from render import archetype_models as am  # noqa: E402
from render import enemy_models as em  # noqa: E402
from render.enemy_models import V_BODY, V_BONE, V_DARK, V_EYE, V_GLOW, V_SEAM  # noqa: E402
from render.enemy_rigs import catmull_rom, model_rotation, model_space_materials  # noqa: E402
from render.sdf import mirror_x, rotate_x, rotate_y, rotate_z, sd_capsule, sd_ellipsoid, sd_plate, sd_sphere, union  # noqa: E402

SCRIPT = "coilwyrm.py"
BATCH = "M4 part F batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
REVIEW_ROUND = "r23"
UNIT = DESIGN / "enemies" / "air" / "coilwyrm"
SLUG = "coilwyrm"
SPEC = yaml.safe_load((UNIT / "data.yaml").read_text(encoding="utf-8"))
CHAIN = SPEC["segment_chain"]
HEAD = SPEC["size"][0]                                       # 58
TAIL = 40                                                    # the stat block's tail: 40 px
N_SEG = CHAIN["segments"]
SEG_SIZES = [int(round(CHAIN["size"][0] + (CHAIN["size"][1] - CHAIN["size"][0]) * i / (N_SEG - 1)))
             for i in range(N_SEG)]
HEADINGS = 48
JAW = [0.0, 0.45, 1.0, 0.45]
EXTENT = 2.3                                                 # model units across a part's canvas (the concept's)
COLOURS = 40
DEATH, DEATH_SIZE = 12, 80
POP, POP_SIZE, SEG_TATTER_SIZE, TAIL_TATTER_SIZE = 8, 40, 56, 48
TEAL = np.array(em.hx(em.GLOWS["teal"])) * 255


def heading(k):
    return TAU * k / HEADINGS


def turned(scene, mats, h):
    """A +Y-forward model turned to point ``h`` clockwise from straight down, its patterns with it."""
    rot = model_rotation(np.pi / 2 + h)
    return (lambda p: scene(rotate_z(p, rot))), model_space_materials(mats, rot)


def regrown_model():
    """The regrown head: the head model a size smaller, the rust paler and wet (glossier), the teal
    brighter, the jaw half open."""
    scene, mats = am.coil_head(0.5, 1.35)
    bone = np.array(em.hx(em.CHITIN_BASES["rust"][2]))
    mats = [replace(m, albedo=tuple(np.array(m.albedo) * 0.75 + bone * 0.25), shininess=150, spec=1.4)
            if i in (V_BODY, V_SEAM) else m for i, m in enumerate(mats)]
    return (lambda p: scene(p / 0.88)), mats


def render_part(job):
    kind, k, j = job
    if kind == "head":
        model, size = am.coil_head(JAW[j]), HEAD
    elif kind == "regrown":
        model, size = regrown_model(), HEAD
    elif kind == "tail":
        model, size = am.coil_tail(), TAIL
    else:
        model, size = am.coil_segment(), SEG_SIZES[j]
    scene, mats = turned(*model, heading(k))
    return artkit.native(*artkit.render_hi(scene, mats, (size, size), EXTENT))


# --------------------------------------------------------------------------- deaths

def ease_out(t, k=2.0):
    return 1 - (1 - np.clip(t, 0, 1)) ** k


def teal_burst(i, n, size, seed, drops, reach, flash):
    """A teal light field: a white-teal flash, glowing droplets flung out, a haze; additive."""
    t = i / (n - 1)
    c = size / 2
    rng = np.random.default_rng(seed)
    cv = v8.Canvas(size, size)
    d = cv.dist(c, c)
    cv.add((225, 255, 240), v8.gauss(d, flash * (0.4 + 0.8 * t)) * np.clip(1 - t * 3.0, 0, 1))
    cv.add(tuple(TEAL), v8.gauss(d, flash * (0.9 + 1.6 * ease_out(t))) * 0.6 * (1 - t) ** 1.6)
    fade = np.clip((size / 2 - d) / 4.0, 0, 1)
    for _ in range(drops):
        a = rng.uniform(0, TAU)
        r = 3 + rng.uniform(0.4, 1.0) * reach * ease_out(t, 2.4)
        s = rng.uniform(0.5, 1.2)
        life = np.clip(1.15 - t * rng.uniform(0.9, 1.3), 0, 1)
        dd = cv.dist(c + np.cos(a) * r, c + np.sin(a) * r)
        cv.add((210, 255, 235) if s > 1.0 else tuple(TEAL), v8.gauss(dd, s) * 1.4 * life * fade)
        cv.add(tuple(TEAL), v8.gauss(dd, s * 2.5) * 0.2 * life * fade)
    return artkit.additive(cv.image())


def head_glow(i):
    return teal_burst(i, DEATH, DEATH_SIZE, 2308, 34, 32, 10)


def pop_glow(i):
    """The segment pop's glint: a four-point teal star and a few sparks."""
    t = i / (POP - 1)
    c = POP_SIZE / 2
    cv = v8.Canvas(POP_SIZE, POP_SIZE)
    d = cv.dist(c, c)
    k = (1 - t) ** 1.5
    cv.add((220, 255, 240), v8.gauss(d, 2.5 + 2 * t) * k)
    for a in (0, np.pi / 2):
        u = np.abs((cv.x - c) * np.cos(a) + (cv.y - c) * np.sin(a))
        v = np.abs(-(cv.x - c) * np.sin(a) + (cv.y - c) * np.cos(a))
        cv.add(tuple(TEAL), v8.gauss(v, 0.8) * v8.gauss(u, 6 + 10 * t) * 0.9 * k)
    rng = np.random.default_rng(2309)
    for _ in range(10):
        a = rng.uniform(0, TAU)
        r = 3 + rng.uniform(6, 16) * ease_out(t)
        cv.add(tuple(TEAL), v8.gauss(cv.dist(c + np.cos(a) * r, c + np.sin(a) * r), 0.8) * 1.2 * k)
    return artkit.additive(cv.image())


def pieces_scene(kind, t):
    """The pieces of a part at time t (0..1), flung out along their own directions and tumbling,
    shrinking at the end; posed in the model under the fixed key light."""
    _, mats = am.coil_segment()
    rng = np.random.default_rng({"head": 2310, "segment": 2311, "tail": 2312}[kind])
    shrink = np.clip(1.0 - (t - 0.55) / 0.45 * 0.7, 0.3, 1.0)
    items_of = []
    if kind == "head":
        frill = [(0.0, 0.18), (0.55, -0.1), (0.5, -0.4), (0.02, -0.3)]
        for s in (1, -1):
            items_of.append(("skull", s, (0.22 * s, 0.1), 0.5 * s * np.pi / 2 + (0 if s > 0 else np.pi)))
            items_of.append(("frill", s, (0.6 * s, -0.15), (0.2 if s > 0 else np.pi - 0.2)))
            items_of.append(("mandible", s, (0.25 * s, 0.8), (1.2 if s > 0 else np.pi - 1.2)))
        for s, y in ((1, -0.4), (-1, -0.4), (1, 0.0), (-1, 0.0)):
            items_of.append(("horn", s, (0.4 * s, y), (-0.6 if s > 0 else np.pi + 0.6) + rng.uniform(-0.3, 0.3)))
    elif kind == "segment":
        frill = [(0.0, 0.17), (0.52, -0.16), (0.47, -0.4), (0.0, -0.37)]
        for a in np.linspace(0.3, TAU + 0.3, 4, endpoint=False):
            items_of.append(("shard", 1, (0.25 * np.cos(a), 0.25 * np.sin(a)), a))
        for s in (1, -1):
            items_of.append(("frill", s, (0.7 * s, -0.1), (0.1 if s > 0 else np.pi - 0.1)))
        items_of.append(("spine", 1, (0.0, 0.0), np.pi / 2 + 0.4))
    else:
        frill = [(0.0, -0.3), (0.55, -0.75), (0.5, -1.02), (0.0, -0.8)]
        for s in (1, -1):
            items_of.append(("tailhalf", s, (0.0, 0.2 - 0.6 * (s < 0)), np.pi / 2 if s > 0 else -np.pi / 2))
            items_of.append(("frill", s, (0.3 * s, -0.7), (0.3 if s > 0 else np.pi - 0.3)))
    spins = [rng.uniform(3, 7) * rng.choice([-1, 1]) for _ in items_of]
    reach = [rng.uniform(0.35, 0.75) for _ in items_of]

    def scene(p):
        items = []
        for (part, s, (x0, y0), a), spin, r in zip(items_of, spins, reach):
            out = r * ease_out(t, 2.0)
            q = p - np.array([x0 + np.cos(a) * out, y0 + np.sin(a) * out, 0.0])
            q = rotate_x(rotate_z(q, spin * t), spin * t * 0.6) / shrink
            if part == "skull":
                q[:, 0] *= s
                d = np.maximum(sd_ellipsoid(q, (0.2, 0.0, 0), (0.42, 0.6, 0.34)), -q[:, 0] + 0.0)
                items.append((d * shrink, V_SEAM))
                items.append((sd_sphere(q, (0.2, 0.32, 0.22), 0.09) * shrink, V_GLOW))
            elif part == "frill":
                q[:, 0] *= s
                items.append((sd_plate(q, frill, 0.0, 0.04, 0.015) * shrink, V_DARK))
            elif part == "mandible":
                items.append((sd_capsule(q, (0, -0.2, 0), (0.06 * s, 0.2, 0), 0.08, 0.02) * shrink, V_BONE))
            elif part == "horn":
                items.append((sd_capsule(q, (0, -0.25, 0), (0, 0.25, 0), 0.07, 0.012) * shrink, V_BONE))
            elif part == "shard":
                items.append((sd_ellipsoid(rotate_z(q, a), (0, 0, 0), (0.2, 0.32, 0.22)) * shrink, V_SEAM))
            elif part == "spine":
                items.append((sd_capsule(q, (0, -0.25, 0), (0, 0.25, 0), 0.06, 0.02) * shrink, V_BONE))
            else:
                items.append((sd_capsule(q, (0, -0.3, 0), (0, 0.3, 0), 0.3, 0.2) * shrink, V_SEAM))
        return union(*items)
    return scene, mats


def render_pieces(job):
    kind, i, n, size, extent = job
    scene, mats = pieces_scene(kind, i / (n - 1))
    return artkit.native(*artkit.render_hi(scene, mats, (size, size), extent))


# --------------------------------------------------------------------------- build

def _job(job):
    kind = job[0]
    if kind == "glow":
        return head_glow(job[1])
    if kind == "pop":
        return pop_glow(job[1])
    if kind == "pieces":
        return render_pieces(job[1])
    return render_part(job)


def body_jobs():
    return ([("head", k, j) for k in range(HEADINGS) for j in range(len(JAW))]
            + [("regrown", k, 0) for k in range(HEADINGS)]
            + [("segment", k, s) for k in range(HEADINGS) for s in range(N_SEG)]
            + [("tail", k, 0) for k in range(HEADINGS)])


def build():
    head_extent = EXTENT * DEATH_SIZE / HEAD
    jobs = (body_jobs() + [("glow", i) for i in range(DEATH)] + [("pop", i) for i in range(POP)]
            + [("pieces", ("head", i, DEATH, DEATH_SIZE, head_extent)) for i in range(DEATH)]
            + [("pieces", ("segment", i, POP, SEG_TATTER_SIZE, EXTENT * SEG_TATTER_SIZE / 44)) for i in range(POP)]
            + [("pieces", ("tail", i, POP, TAIL_TATTER_SIZE, EXTENT * TAIL_TATTER_SIZE / TAIL)) for i in range(POP)])
    with ProcessPoolExecutor() as pool:
        out = list(pool.map(_job, jobs, chunksize=4))
    nh, nr, ns, nt = HEADINGS * len(JAW), HEADINGS, HEADINGS * N_SEG, HEADINGS
    body = artkit.quantize_set(out[:nh + nr + ns + nt], COLOURS)
    at = 0
    for name, n in ((SLUG, nh), (f"{SLUG}-regrown", nr), (f"{SLUG}-segment", ns), (f"{SLUG}-tail", nt)):
        artkit.write_frames(name, body[at:at + n], SOURCE)
        at += n
    rest = out[at:]
    glow, rest = artkit.quantize_set(rest[:DEATH], 24), rest[DEATH:]
    pop, rest = artkit.quantize_set(rest[:POP], 16), rest[POP:]
    head_p, rest = artkit.quantize_set(rest[:DEATH], 24), rest[DEATH:]
    seg_p, tail_p = artkit.quantize_set(rest[:POP], 24), artkit.quantize_set(rest[POP:], 24)
    for name, frames in ((f"{SLUG}-death", glow), (f"{SLUG}-regrown-death", glow),
                         (f"{SLUG}-tatters", head_p), (f"{SLUG}-regrown-tatters", head_p),
                         (f"{SLUG}-segment-death", pop), (f"{SLUG}-tail-death", pop),
                         (f"{SLUG}-segment-tatters", seg_p), (f"{SLUG}-tail-tatters", tail_p)):
        artkit.write_frames(name, frames, SOURCE)
    artkit.write_pivots(SLUG, SOURCE, {
        "headings": HEADINGS, "head_frames": len(JAW), "segment_sizes": SEG_SIZES,
        "segment_index": "heading * sizes + size, size 0 the largest (next to the head)"})


# --------------------------------------------------------------------------- review

def frame_of(h, n=HEADINGS):
    """The heading index of a screen heading ``h`` (radians clockwise from straight down)."""
    return int(round(h / TAU * n)) % n


def chain_at(path, t, lengths, spacing, dt=0.002):
    """The members' positions and headings on the head's path history (the sim's chain rule:
    spacing x the mean length of two neighbours)."""
    gaps = [spacing * (lengths[i] + lengths[i + 1]) / 2 for i in range(len(lengths) - 1)]
    targets = np.concatenate([[0.0], np.cumsum(gaps)])
    pts = [np.array(path(t))]
    prev, acc, tt, j = pts[0], 0.0, t, 1
    while j < len(targets) and tt > t - 30:
        tt -= dt
        cur = np.array(path(tt))
        acc += np.linalg.norm(cur - prev)
        prev = cur
        while j < len(targets) and acc >= targets[j]:
            pts.append(cur)
            j += 1
    heads = []
    for i, p in enumerate(pts):
        ahead = pts[i - 1] if i else np.array(path(t + 0.01))
        d = ahead - p
        heads.append(float(np.arctan2(-d[0], d[1])))           # clockwise from down, screen y down
    return pts, heads


def draw_chain(img, art, path, t, n=HEADINGS, tick=0, spacing=CHAIN["spacing"]):
    lengths = [HEAD] + SEG_SIZES + [TAIL]
    pts, heads = chain_at(path, t, lengths, spacing)
    for i in range(len(pts) - 1, -1, -1):
        h = frame_of(heads[i], n) * (HEADINGS // n)
        if i == 0:
            fr = art["head"][h * len(JAW) + tick % len(JAW)]
        elif i == len(lengths) - 1:
            fr = art["tail"][h]
        else:
            fr = art["segment"][h * N_SEG + (i - 1)]
        sprite.paste_center(img, fr, *pts[i])


def swirl(t):
    """A looping swirl down the field (prolate cycloid), 180 px/s along its path on average."""
    return (240 + 150 * np.sin(1.2 * t) - 40 * np.sin(2.4 * t), -100 + 60 * t + 110 * np.cos(1.2 * t))


def review():
    artkit.REVIEW_ROUND = REVIEW_ROUND
    art = {"head": artkit.load_frames(SLUG), "regrown": artkit.load_frames(f"{SLUG}-regrown"),
           "segment": artkit.load_frames(f"{SLUG}-segment"), "tail": artkit.load_frames(f"{SLUG}-tail")}
    death = {k: artkit.load_frames(f"{SLUG}-{k}") for k in
             ("death", "tatters", "segment-death", "segment-tatters", "tail-tatters")}
    s_path = lambda tt: (40 + 52 * tt, 120 + 62 * np.sin(1.6 * tt))  # noqa: E731
    panels = []
    for n, spacing in ((HEADINGS, CHAIN["spacing"]), (16, CHAIN["spacing"]), (HEADINGS, 0.5)):
        img = Image.new("RGBA", (440, 240), artkit.PLATE)
        draw_chain(img, art, s_path, 7.2, n=n, spacing=spacing)
        panels.append(img)
    sheet = artkit.review_sheet("COILWYRM - FINAL SPRITES", [
        ("CHAIN ON A CURVE: 48 HEADINGS AT THE DATA'S SPACING (0.9) / 16 HEADINGS (THE CONCEPT'S) / 48 AT THE CONCEPT'S 0.5",
         panels, 1, False),
        ("HEAD, EVERY 4TH OF 48 HEADINGS (JAW HALF OPEN)", [art["head"][k * 4 * len(JAW) + 1] for k in range(12)], 2, False),
        ("HEAD JAW FRAMES AT HEADING 0, REGROWN HEAD EVERY 6TH HEADING", art["head"][:len(JAW)]
         + art["regrown"][::6], 2, False),
        ("SEGMENTS 1-12 AT HEADING 0 (54 TO 27 PX), TAIL", art["segment"][:N_SEG] + [art["tail"][0]], 2, False),
        ("SEGMENT 1, EVERY 4TH HEADING", [art["segment"][k * 4 * N_SEG] for k in range(12)], 2, False),
        ("HEAD DEATH: TEAL FLASH (ADDITIVE)", death["death"], 1, True),
        ("HEAD DEATH: SKULL, HORNS, MANDIBLES, FRILLS (SOLID)", death["tatters"], 1, False),
        ("SEGMENT POP: GLINT (ADDITIVE, ALSO THE TAIL'S)", death["segment-death"], 2, True),
        ("SEGMENT AND TAIL PIECES (SOLID)", death["segment-tatters"] + death["tail-tatters"], 2, False)],
        width=1400, batch=BATCH)
    gif = []
    for i in range(100):
        t = i / 10
        img = Image.new("RGBA", (480, 540), (14, 16, 24, 255))
        draw_chain(img, art, swirl, t + 2.0, tick=i)
        gif.append(img)
    artkit.save_review(sheet, gif, UNIT / "concept", SLUG, fps=10)


if __name__ == "__main__":
    if "--review" not in sys.argv[1:]:
        build()
    review()
