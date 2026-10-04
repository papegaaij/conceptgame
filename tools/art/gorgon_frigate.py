#!/usr/bin/env python3
"""Production art: the Gorgon Frigate, Level 05's mid-boss (design/enemies/bosses/gorgon-frigate;
M4 part E batch).

Outputs (assets/sprites/ and assets/pivots/), every sprite under the fixed key light, nothing lit
rotated or mirrored afterwards:
  gorgon-frigate-bell_0..3.png    240x200, the medusa bell facing down the screen (the necks'
                                  sockets on its front edge at the data's chain anchors, the
                                  tendril veil trailing up), in four stages of the crown: 0 closed
                                  over the core (phases 1-2), 1-2 opening, 3 open with the lime
                                  core exposed (phase 3); the bell centre at `bell.origin` of the
                                  pivot file
  gorgon-frigate-core-glow.png    72x72 lime glow over the exposed core (additive)
  gorgon-frigate-neck-1..5_0..14  the armoured neck segments, 1 next to the bell to 5 next to the
                                  head (tapering, 50 to 40 px canvases), each at 15 headings
  gorgon-frigate-head_0..14       60x60 cobra head, the turret, centred on the chain's tip
  gorgon-frigate-stump_0..14      48x48 a destroyed head's torn, leaking neck end at the tip
  gorgon-frigate-petals_0..5      160x160 the crown's five petals tearing off at the death and
                                  tumbling away (solid), centred on the core
  gorgon-frigate-ichor_0..9       96x96 lime-violet ichor burst (additive): the death cloud the game
                                  plays with each part's burst (the `<slug>-ichor` convention)
  pivots/gorgon-frigate.json      the bell's origin, the heading set, the core offset

Headings: the neck pieces and the head are a small set of the data's 32 angles, the 15 within
+-78.75 degrees of straight down (step k = -7..7 of 11.25 degrees clockwise from down, frame
k + 7): a neck's anchor turns at most 55 degrees from its rest line (enraged), which itself lies
at most 9.5 degrees off straight down, and every later piece follows it, so no piece ever points
further than 65 degrees; the game rounds a piece's heading to the nearest step and clamps it.

Models: the chosen round-06 concept (tools/concept/render/boss_models.py: the bone/violet/lime
materials, the ribbed dome with its scalloped rim, the crown petals over the core, the tendril
veil; gorgon_neck and gorgon_head used unchanged). The bell is re-laid to the data: wider than
long (hit box 200x120), the three sockets on its front edge at the chains' anchors, the core
at its offset; 100 px per model unit, rendered at 4x, the neck pieces and heads at 8x.

Run: python3 tools/art/gorgon_frigate.py [--review]   (~3 min on 20 cores)
"""
import json
import sys
from concurrent.futures import ProcessPoolExecutor

import numpy as np
import yaml
from PIL import Image, ImageDraw

import artkit
from artkit import DESIGN, TAU, sprite

import vfx_r08 as v8  # noqa: E402  (concept script, imported unchanged)
from render import boss_models as bm  # noqa: E402
from render import enemy_models as em  # noqa: E402
from render import sdf  # noqa: E402
from render.enemy_models import V_BODY, V_BONE, V_DARK, V_EYE, V_SAC, V_SEAM  # noqa: E402
from render.enemy_rigs import model_rotation, model_space_materials  # noqa: E402
from render.sdf import (Material, rotate_x, rotate_y, rotate_z, sd_capsule, sd_cylinder_z,  # noqa: E402
                        sd_ellipsoid, sd_sphere, union)

SCRIPT = "gorgon_frigate.py"
BATCH = "M4 part E batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
UNIT = DESIGN / "enemies" / "bosses" / "gorgon-frigate"
SLUG = "gorgon-frigate"
S = 100.0                         # px per model unit, the bell
BELL_W, BELL_H = 240, 200
ORIGIN = (120, 118)               # the bell centre in its sprite, px from the top left
CROWN = [0.0, 0.4, 0.75, 1.0]     # the petals' opening per bell frame
STEPS, REACH = 32, 7              # the heading set: steps -REACH..REACH of 360/STEPS degrees
NECK_PPU = [30.0, 28.5, 27.0, 25.5, 24.0]   # px per unit, neck piece 1 (bell) to 5 (head)
NECK_CANVAS = [50, 48, 46, 44, 40]
HEAD_PPU, HEAD_CANVAS, HEAD_PIVOT = 36.0, 60, 0.05
STUMP_CANVAS = 48
PETAL_FRAMES, PETAL_CANVAS = 6, 160
ICHOR_FRAMES, ICHOR_SIZE = 10, 96
CHAR, ICHOR = 9, 10


def spec():
    return yaml.safe_load((UNIT / "data.yaml").read_text(encoding="utf-8"))


SPEC = spec()
PART = {p["name"]: p for p in SPEC["part_list"]}
CHAINS = SPEC["chains"]
CORE = tuple(v / S for v in PART["core"]["offset"])
SOCKETS = [tuple(v / S for v in c["from"]) for c in CHAINS]


def materials(pattern=None):
    """The concept's Gorgon materials plus char and wet violet ichor (the wrecks)."""
    m = bm.mats(SLUG, 1.0, body_pattern=pattern or (lambda p, n: np.ones(len(p))))
    return m + [Material((0.10, 0.08, 0.09), metal=0.1, shininess=20, spec=0.2),
                Material((0.22, 0.07, 0.28), metal=0.0, shininess=120, spec=1.2,
                         emission=(0.10, 0.02, 0.16))]


# --------------------------------------------------------------------------- the bell

DOME = ((0.0, 0.02, 0.10), (0.92, 0.58, 0.42))


def dome_z(x, y):
    (cx, cy, cz), (rx, ry, rz) = DOME
    t = 1 - ((x - cx) / rx) ** 2 - ((y - cy) / ry) ** 2
    return cz + rz * np.sqrt(max(0.0, t))


def bell_model(core_open, petals=True):
    """The bell laid out to the data (screen frame: x right, y up towards the tail); without the
    crown's petals for the death's wedges (gorgon_frigate_death.py)."""
    rib_segs = []
    for a in np.linspace(0, TAU, 14, endpoint=False):
        c, s = np.cos(a), np.sin(a)
        pts = [(r * 0.92 * c, r * 0.58 * s + 0.02) for r in (0.42, 0.62, 0.8, 0.96)]
        for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
            rib_segs.append(((x0, y0, dome_z(x0, y0) - 0.005), (x1, y1, dome_z(x1, y1) - 0.005)))
    scallops = [(1.02 * np.cos(a), 0.66 * np.sin(a) + 0.02) for a in np.linspace(0, TAU, 22, endpoint=False)]
    cx, cy = CORE
    cz = dome_z(cx, cy)

    def ribs(pp):
        return union(*[(sd_capsule(pp, a, b, 0.03, 0.026), V_DARK) for a, b in rib_segs])

    def rim(pp):
        return union(*[(sd_ellipsoid(pp, (x, y, 0.02), (0.13, 0.10, 0.06)), V_SEAM) for x, y in scallops])

    def tendrils(pp, veil=0.6):
        items = []
        for i, x in enumerate((-0.44, -0.22, 0.0, 0.22, 0.44)):
            pts = [(x, 0.56, 0.02)]
            for k in range(1, 4):
                sway = 0.07 * k * np.sin(veil + i * 1.1 + k * 0.9)
                pts.append((x * (1 + 0.08 * k) + sway, 0.56 + 0.17 * k, 0.0))
            items += [(sd_capsule(pp, pts[k], pts[k + 1], 0.08 - 0.02 * k, 0.065 - 0.02 * k), V_SAC)
                      for k in range(3)]
        return union(*items)

    def scene(p):
        dome = sd_ellipsoid(p, *DOME)
        d, m = union((dome, V_BODY), (sd_ellipsoid(p, (0, 0.02, 0.0), (1.0, 0.65, 0.12)), V_DARK), k=0.05)
        d, m = union((d, m), bm.bounded(p, np.abs(dome) - 0.07, 0.12, ribs), k=0.03)
        ring = sd_ellipsoid(p, (0, 0.02, 0.02), (1.2, 0.82, 0.2))
        inner = -sd_ellipsoid(p, (0, 0.02, 0.02), (0.78, 0.46, 0.4))
        d, m = union((d, m), bm.bounded(p, np.maximum(ring, inner), 0.12, rim), k=0.03)
        d, m = union((d, m), bm.bounded(p, sdf.sd_box(p, (0, 0.88, 0.0), (0.7, 0.36, 0.15)), 0.1, tendrils),
                     k=0.05)
        # the necks' sockets on the front edge: a dark collar, a violet seam ring, a bone lip
        for sx, sy in SOCKETS:
            d, m = union((d, m), (sd_cylinder_z(p, (sx, sy, 0.1), 0.13, 0.08), V_DARK), k=0.04)
            d, m = union((d, m), (sd_sphere(p, (sx, sy, 0.17), 0.075), V_SEAM),
                         (sd_capsule(p, (sx - 0.1, sy + 0.08, 0.17), (sx + 0.1, sy + 0.08, 0.17), 0.03, 0.03),
                          V_BONE), k=0.02)
        # the brow ridges between the sockets
        for x in (-0.3, 0.3):
            d, m = union((d, m), (sd_capsule(p, (x - 0.12, -0.5, 0.2), (x + 0.12, -0.5, 0.2), 0.045, 0.045),
                                  V_BONE), k=0.04)
        # the lime core sunk under the closed crown, rising as the five petals part (core_open)
        d, m = union((d, m), (sd_cylinder_z(p, (cx, cy, cz - 0.02), 0.27, 0.06), V_DARK), k=0.03)
        d, m = union((d, m), (sd_sphere(p, (cx, cy, cz - 0.1 + 0.12 * core_open), 0.19), V_EYE), k=0.02)
        if not petals:
            return d, m
        crown = [(sd_ellipsoid(rotate_z(p - np.array([cx, cy, 0]), a),
                               (0, 0.15 + 0.29 * core_open, cz + 0.1 - 0.12 * core_open),
                               (0.15, 0.27, 0.06)), V_BONE)
                 for a in np.linspace(0, TAU, 5, endpoint=False) + TAU / 10]
        return union((d, m), *crown, k=0.0)
    return scene, materials(bm.radial_ribs(28, 0.05, 30))


def render_bell(k):
    scene, mats = bell_model(CROWN[k])
    extent = BELL_W / S
    centre = ((BELL_W / 2 - ORIGIN[0]) / S, (ORIGIN[1] - BELL_H / 2) / S)
    hi = sdf.render(scene, mats, (BELL_W * 4, BELL_H * 4), extent, center=centre, steps=140)
    return artkit.native(hi, 4)


# --------------------------------------------------------------------------- necks, heads, stumps

def heading(i):
    """Frame i's heading, radians clockwise from straight down."""
    return TAU * (i - REACH) / STEPS


def turned(model, mats, h):
    """``model`` (forward +Y) turned to point ``h`` clockwise from down, its patterns with it."""
    rot = model_rotation(np.pi / 2 + h)
    return (lambda p: model(rotate_z(p, rot))), model_space_materials(mats, rot)


def stump_model():
    """A destroyed head: the neck's last ring torn open, charred, violet ichor welling out and
    dripping forward; the origin at the chain's tip, the stump reaching back towards the neck."""
    def scene(p):
        rng = np.random.default_rng(7)
        cut = 0.05 + 0.05 * np.sin(np.arctan2(p[:, 2], p[:, 0]) * 5 + 0.7)
        tube = sd_capsule(p, (0, -0.25, 0), (0, 0.1, 0), 0.3, 0.28)
        d = np.maximum(tube, (p[:, 1] - cut) * 0.6)
        m = np.where(p[:, 1] > cut - 0.08, CHAR, V_BODY)
        d, m = union((d, m), (sd_ellipsoid(p, (0, -0.2, 0), (0.36, 0.1, 0.3)), V_DARK), k=0.04)
        d, m = union((d, m), (sd_capsule(p, (0, -0.4, 0.26), (0, -0.05, 0.26), 0.05, 0.05), V_SEAM), k=0.03)
        blobs = [(sd_sphere(p, (0.0, 0.08, 0.06), 0.17), ICHOR)]
        for _ in range(4):
            a = rng.uniform(-1.2, 1.2)
            blobs.append((sd_sphere(p, (0.16 * np.sin(a), 0.16 + 0.1 * rng.uniform(), 0.1 * np.cos(a)),
                                    rng.uniform(0.05, 0.08)), ICHOR))
        blobs.append((sd_capsule(p, (0.05, 0.14, 0.02), (0.08, 0.42, -0.08), 0.06, 0.03), ICHOR))
        for a in (-0.9, 0.0, 0.9):
            blobs.append((sd_capsule(p, (0.26 * np.sin(a), 0.0, 0.26 * np.cos(a)),
                                     (0.33 * np.sin(a), 0.13, 0.32 * np.cos(a)), 0.035, 0.012), CHAR))
        return union((d, m), *blobs, k=0.03)
    return scene, materials()


def render_piece(kind, i):
    h = heading(i)
    if kind == "head":
        head, _ = bm.gorgon_head(0.0)
        model = lambda p: head(p + np.array([0.0, HEAD_PIVOT, 0.0]))  # noqa: E731
        size, ppu, mats = HEAD_CANVAS, HEAD_PPU, materials()
    elif kind == "stump":
        model, mats = stump_model()
        size, ppu = STUMP_CANVAS, HEAD_PPU
    else:
        n = int(kind[-1]) - 1
        model, _ = bm.gorgon_neck()
        size, ppu, mats = NECK_CANVAS[n], NECK_PPU[n], materials()
    scene, mats = turned(model, mats, h)
    hi = sdf.render(scene, mats, (size * 8, size * 8), size / ppu, steps=120)
    return artkit.native(hi, 8)


# --------------------------------------------------------------------------- the death

def petals_model(t):
    """The five crown petals torn off at frame time t (0..1): flung outwards from their open
    places, tumbling, falling away (smaller), charred at the torn base, ichor trailing."""
    sc = 1 - 0.42 * t
    spin = [1.9, -2.4, 2.8, -1.6, 2.2]

    def scene(p):
        items = []
        for j, a in enumerate(np.linspace(0, TAU, 5, endpoint=False) + TAU / 10):
            dist = (0.44 + 0.62 * t * (0.85 + 0.1 * (j % 3))) * sc
            q = rotate_z(p, a) - np.array([0.0, dist, 0.05 * sc])
            q = rotate_x(rotate_y(q, spin[j] * t), 1.1 * t * (1 if j % 2 else -1))
            d = sd_ellipsoid(q, (0, 0, 0), (0.15 * sc, 0.27 * sc, 0.06 * sc))
            m = np.where(q[:, 1] < -0.17 * sc, CHAR, V_BONE)
            items.append((d, m))
            for k in range(3):
                r = (0.045 - 0.01 * k) * sc
                items.append((sd_sphere(rotate_z(p, a), (0.02 * (k - 1) * sc,
                                                         dist - (0.3 + 0.13 * k * (0.3 + t)) * sc, 0.0), r), ICHOR))
        return union(*items)
    return scene, materials()


def render_petals(f):
    scene, mats = petals_model(f / (PETAL_FRAMES - 1))
    hi = sdf.render(scene, mats, (PETAL_CANVAS * 4, PETAL_CANVAS * 4), PETAL_CANVAS / S, steps=120)
    return artkit.native(hi, 4)


def ichor_frames():
    """The death cloud: a lime flash, then violet ichor droplets flung out and fading (additive)."""
    rng = np.random.default_rng(5)
    drops = [(rng.uniform(0, TAU), rng.uniform(0.5, 1.0), rng.uniform(3, 6)) for _ in range(16)]
    lime = em.scheme_colors(SLUG)[4] * 255
    violet = np.array(em.hx(em.GLOWS["violet"])) * 255
    frames = []
    for f in range(ICHOR_FRAMES):
        t = f / (ICHOR_FRAMES - 1)
        cv = v8.Canvas(ICHOR_SIZE, ICHOR_SIZE)
        c = ICHOR_SIZE / 2
        fade = (1 - t) ** 1.4
        cv.add(tuple(lime), v8.gauss(cv.dist(c, c), 8 + 22 * t) * 1.1 * (1 - t) ** 2.5)
        cv.add((240, 255, 220), v8.gauss(cv.dist(c, c), 5 + 6 * t) * max(0.0, 1 - 2.5 * t))
        for a, speed, r in drops:
            reach = 40 * speed * (1 - (1 - t) ** 2)
            x, y = c + np.cos(a) * reach, c + np.sin(a) * reach
            cv.add(tuple(violet), v8.gauss(cv.dist(x, y), r * (1 + 0.6 * t)) * 0.95 * fade)
            cv.add(tuple(lime), v8.gauss(cv.dist(x, y), r * 0.45) * 0.7 * fade)
        frames.append(artkit.additive(cv.image()))
    return artkit.quantize_set(frames, 24)


def core_glow():
    lime = em.scheme_colors(SLUG)[4] * 255
    cv = v8.Canvas(72, 72)
    d = cv.dist(36, 36)
    cv.add(tuple(lime), v8.gauss(d, 17.0) * 0.85)
    cv.add((235, 255, 200), v8.gauss(d, 7.0) * 0.8)
    return artkit.quantize_set([artkit.additive(cv.image())], 16)[0]


# --------------------------------------------------------------------------- build

PIECES = [f"neck-{n}" for n in range(1, 6)] + ["head", "stump"]


def job(kind, i):
    if kind == "bell":
        return render_bell(i)
    if kind == "petals":
        return render_petals(i)
    return render_piece(kind, i)


def build():
    jobs = ([("bell", k) for k in range(len(CROWN))] + [("petals", f) for f in range(PETAL_FRAMES)]
            + [(kind, i) for kind in PIECES for i in range(2 * REACH + 1)])
    with ProcessPoolExecutor() as pool:
        results = dict(zip(jobs, pool.map(job, *zip(*jobs))))
    order = ["bell"] + PIECES + ["petals"]
    counts = {"bell": len(CROWN), "petals": PETAL_FRAMES}
    flat = [results[(kind, i)] for kind in order for i in range(counts.get(kind, 2 * REACH + 1))]
    mapped = artkit.quantize_set(flat, 48)          # one palette over the whole boss
    at = 0
    for kind in order:
        n = counts.get(kind, 2 * REACH + 1)
        artkit.write_frames(f"{SLUG}-{kind}", mapped[at:at + n], SOURCE)
        at += n
    artkit.write_frames(f"{SLUG}-core-glow", [core_glow()], SOURCE, single=True)
    artkit.write_frames(f"{SLUG}-ichor", ichor_frames(), SOURCE)
    # keeps the "death" entry of tools/art/gorgon_frigate_death.py (the break-up's chunks and blasts)
    old = artkit.PIVOTS / f"{SLUG}.json"
    keep = {k: v for k, v in json.loads(old.read_text(encoding="utf-8")).items()
            if k == "death"} if old.exists() else {}
    artkit.write_pivots(SLUG, SOURCE, {
        "bell": {"origin": list(ORIGIN), "crown_frames": len(CROWN)},
        "headings": {"steps": STEPS, "first": -REACH, "count": 2 * REACH + 1},
        "core": {"offset": PART["core"]["offset"]}, **keep})


# --------------------------------------------------------------------------- review

def frame_index(phi):
    """The heading frame of a piece pointing at ``phi`` (radians counter-clockwise, y up)."""
    h = -np.pi / 2 - phi
    k = int(round(((h + np.pi) % TAU - np.pi) / (TAU / STEPS)))
    return int(np.clip(k, -REACH, REACH)) + REACH


def chain_layout(c, angles):
    """Segments and tip of chain c with its pieces' turns (the sim's placeChains), px, y up."""
    ch = CHAINS[c]
    end = PART[ch["to"]]["offset"]
    pieces = ch["segments"] + 1
    sx, sy = (end[0] - ch["from"][0]) / pieces, (end[1] - ch["from"][1]) / pieces
    px, py = ch["from"]
    out = []
    for k in range(pieces):
        a = angles[k]
        px, py = px + sx * np.cos(a) - sy * np.sin(a), py + sx * np.sin(a) + sy * np.cos(a)
        out.append((px, py, np.arctan2(sy, sx) + a))
    return out


def compose(art, angles, crown=0, wrecked=(), glow=0.0, size=(340, 380), centre=(170, 128)):
    """The frigate as the game draws it: necks, bell, core glow, heads (or stumps)."""
    img = Image.new("RGBA", size, artkit.PLATE)
    cx, cy = centre
    for c in range(len(CHAINS)):
        lay = chain_layout(c, angles[c])
        for k, (x, y, phi) in enumerate(lay[:-1]):
            sprite.paste_center(img, art[f"neck-{k + 1}"][frame_index(phi)], cx + x, cy - y)
    bell = art["bell"][crown]
    img.alpha_composite(bell, (cx - ORIGIN[0], cy - ORIGIN[1]))
    if glow > 0:
        g = np.array(art["glow"]).astype(np.float64)
        g[..., :3] *= glow
        g = Image.fromarray(g.astype(np.uint8), "RGBA")
        ox, oy = PART["core"]["offset"]
        img = artkit.add_light(img, g, (int(cx + ox - g.width // 2), int(cy - oy - g.height // 2)))
    heads = [p["name"] for p in SPEC["part_list"] if p["kind"] == "destroyable"]
    for c, ch in enumerate(CHAINS):
        x, y, phi = chain_layout(c, angles[c])[-1]
        kind = "stump" if ch["to"] in wrecked else "head"
        sprite.paste_center(img, art[kind][frame_index(phi)], cx + x, cy - y)
    assert len(heads) == len(CHAINS)
    return img


def hit_boxes(img, angles, centre=(170, 128)):
    out = img.copy()
    d = ImageDraw.Draw(out)
    cx, cy = centre
    bw, bh = SPEC["hitbox"]
    d.rectangle([cx - bw // 2, cy - bh // 2, cx + bw // 2 - 1, cy + bh // 2 - 1], outline=(255, 255, 255, 255))
    for c, ch in enumerate(CHAINS):
        sw, sh = ch["hitbox"]
        lay = chain_layout(c, angles[c])
        for x, y, _ in lay[:-1]:
            d.rectangle([cx + x - sw / 2, cy - y - sh / 2, cx + x + sw / 2 - 1, cy - y + sh / 2 - 1],
                        outline=(120, 200, 255, 255))
        x, y, _ = lay[-1]
        w, h = PART[ch["to"]]["hitbox"]
        d.rectangle([cx + x - w / 2, cy - y - h / 2, cx + x + w / 2 - 1, cy - y + h / 2 - 1], outline=(255, 170, 40, 255))
    ox, oy = PART["core"]["offset"]
    w, h = PART["core"]["hitbox"]
    d.rectangle([cx + ox - w / 2, cy - oy - h / 2, cx + ox + w / 2 - 1, cy - oy + h / 2 - 1], outline=(200, 120, 255, 255))
    return out


def load_art():
    art = {k: artkit.load_frames(f"{SLUG}-{k}") for k in ["bell", "petals", "ichor"] + PIECES}
    art["glow"] = artkit.load_frames(f"{SLUG}-core-glow")[0]
    return art


def swing(t, c, bend=30.0):
    """Lagged neck turns at time t: the anchor swings, each later piece follows 0.12 s late."""
    base = np.radians(bend) * np.sin(TAU * t / 3.0 + 1.7 * c)
    return [np.radians(bend) * np.sin(TAU * (t - 0.12 * k) / 3.0 + 1.7 * c) if k else base for k in range(6)]


def review():
    artkit.REVIEW_ROUND = "r21"
    art = load_art()
    rest = [[0.0] * 6 for _ in CHAINS]
    bent = [swing(0.6, c) for c in range(len(CHAINS))]
    whole = [compose(art, rest), compose(art, bent),
             compose(art, rest, crown=3, wrecked=("left head", "centre head", "right head"), glow=1.0)]
    boxes = [hit_boxes(compose(art, rest), rest), hit_boxes(compose(art, bent), bent)]
    sheet = artkit.review_sheet("GORGON FRIGATE - FINAL SPRITES", [
        ("AS THE GAME DRAWS IT: AT REST, NECKS SWINGING, PHASE 3 (HEADS DESTROYED, CROWN OPEN, CORE GLOW)", whole, 1, False),
        ("THE DATA'S HIT BOXES: BELL WHITE, NECK SEGMENTS BLUE, HEADS AMBER, CORE VIOLET", boxes, 1, False),
        ("BELL: CROWN CLOSED, OPENING, OPENING, OPEN (240X200)", art["bell"], 1, False),
        ("HEAD, 15 HEADINGS (-78.75 TO +78.75 DEG FROM DOWN, CLOCKWISE)", art["head"], 2, False),
        ("STUMP (DESTROYED HEAD), 15 HEADINGS", art["stump"], 2, False),
        ("NECK PIECE 1 (AT THE BELL), 15 HEADINGS", art["neck-1"], 2, False),
        ("NECK PIECES 1-5 POINTING DOWN, THEN AT +-45 DEG", [art[f"neck-{n}"][REACH] for n in range(1, 6)]
         + [art["neck-3"][REACH - 4], art["neck-3"][REACH + 4]], 3, False),
        ("CROWN PETALS TEARING OFF AT THE DEATH (SOLID, 10 FPS)", art["petals"], 1, False),
        ("DEATH ICHOR (ADDITIVE, 10 FPS)", art["ichor"], 1, True),
        ("CORE GLOW (ADDITIVE)", [art["glow"]], 2, True)], width=1400, batch=BATCH)
    gif = []
    lost = []
    for i in range(80):
        t = i / 10
        if i in (24, 40, 52):
            lost.append(["right head", "left head", "centre head"][len(lost)])
        angles = [swing(t, c, 55 if len(lost) == 2 else 30) if CHAINS[c]["to"] not in lost else [0.0] * 6
                  for c in range(len(CHAINS))]
        crown = 0 if len(lost) < 3 else min(3, 1 + (i - 52) // 3)
        glow = 0 if len(lost) < 3 else min(1.0, (i - 52) / 10) * (0.75 + 0.25 * np.sin(TAU * t / 0.8))
        gif.append(compose(art, angles, crown, tuple(lost), glow))
    end = gif[-1]
    for f in range(12):
        img = Image.new("RGBA", end.size, artkit.PLATE)
        if f < 2:
            img = end.copy()
        ox, oy = PART["core"]["offset"]
        if f < len(art["petals"]):
            sprite.paste_center(img, art["petals"][f], 170 + ox, 128 - oy)
        if f < len(art["ichor"]):
            g = art["ichor"][f]
            img = artkit.add_light(img, g, (170 + ox - g.width // 2, 128 - oy - g.height // 2))
        gif.append(img)
    artkit.save_review(sheet, gif, UNIT / "concept", SLUG, fps=10)


if __name__ == "__main__":
    if "--review" not in sys.argv[1:]:
        build()
    review()
