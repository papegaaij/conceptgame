#!/usr/bin/env python3
"""Production art: the Brood Carrier's break-up at its death (design/enemies/bosses/brood-carrier;
M4 part G batch, step A1). Planned from the start after rounds 16 and 21 ("now it just disappears":
the Leviathan and the Gorgon Frigate needed break-up pieces and many explosions); the mechanism is
theirs (tools/art/gorgon_frigate_death.py, the game's SetPieceDeath), the chain from tail to head
is the act boss's (the game's DeathChain over `death_seconds`).

Outputs (assets/sprites/ and assets/pivots/), every sprite under brood_carrier.py's fixed key light:
  brood-carrier-chunk-1..8_0..2  the carrier where it always dies, broadside (phase 3: the head to
                                 the right), wrecked (every sac burst, the iris open over the burst
                                 core, the mandible turret as the game leaves it), cut along jagged
                                 seams into eight pieces: 1 the tail with its tendrils, 2 the aft
                                 section (bays 4 and 3), 3 and 4 the middle split along the spine
                                 (the core torn open; upper and lower), 5 the fore section (bays 2
                                 and 1), 6 the head with the mandibles, 7 a torn flank with the fore
                                 upper membrane, 8 a torn flank with the aft lower membrane. The
                                 chitin along a cut is charred and the torn flesh glows lime,
                                 cooling over the frames (the carcass's own lights dark). Frame
                                 0 is in place: drawn at its offset with no motion the eight
                                 reproduce the wreck; frames 1 and 2 are the piece tilting (its
                                 outer edge going down) and turning a little about its own centre,
                                 each a render of the turned model on the hull's canvas (so the
                                 light stays where it is for the hull)
  brood-carrier-ichor_0..9.png   96x96 lime-teal ichor burst (additive): the death cloud the game
                                 plays at every part's burst and at the chain's end (the
                                 `<slug>-ichor` convention), reused by the break-up's blasts
  pivots/brood-carrier.json "death"  the break-up the game plays (px, dx right, dy up from the hull
                                 centre broadside; times in game steps of 1/60 s from the death):
                                 the chunks with their centres per frame and drift, the swap, the
                                 end, sinking, darkening and fade, and the blast table
                                 (explosion-large/-medium, brood-carrier-ichor)
  design/enemies/bosses/brood-carrier/concept/brood-carrier-death-final-r25-a.png/.gif  review
                                 sheet and the whole death over the body

The timeline: the game's chain bursts a medium explosion with an ichor cloud at every part and
twelve more along the hull, from the tail at step 0 to the head at step 180 (3 s), then large blasts
at the head and the centre, the screen flash and the credit shower. Over it this table adds a wave
of large and medium blasts that runs along the hull from the tail with the chain (each blast where
the chain's front is at its step), and a cluster over the five seams at the chain's end, where the
body is replaced by the chunks (the swap, step 180, under the flash). The chunks then drift apart
slowly in the vacuum through L07's whole aftermath (35 s after the kill; the chunks last 40 s), the
carcass drifting down and to the right as a whole (the tail comes into view from the left edge) with
each piece spreading away from the others, sinking (drawn smaller), darkening, only fading out from
90 % of the way (after the level has ended), under trailing blasts on them (fire, no ichor). The carcass is
dead: no eye, tendril, spine, sac or core light; only the torn flesh along the cuts glows lime at
the break-up and cools over the tumble frames (6 s each) to dead tissue.

Run: python3 tools/art/brood_carrier_death.py [--data | --review]   (~15 min on 20 cores; after
brood_carrier.py and explosions.py; --data keeps the chunk sprites and rewrites only the death's
data and the review, --review only the review)
"""
import json
import sys
from concurrent.futures import ProcessPoolExecutor
from dataclasses import replace

import numpy as np
from PIL import Image, ImageDraw

import artkit
from artkit import TAU, sprite

import brood_carrier as bc  # noqa: E402  (tools/art: the boss's model, materials, light, layout)
import vfx_r08 as v8  # noqa: E402  (concept script, imported unchanged)
from render import boss_models as bm  # noqa: E402
from render import enemy_models as em  # noqa: E402
from render import raster  # noqa: E402
from render.enemy_rigs import model_space_materials  # noqa: E402
from render.sdf import rotate_z  # noqa: E402

SCRIPT = "brood_carrier_death.py"
BATCH = "M4 part G batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
REVIEW_ROUND = "r25"
SLUG = bc.SLUG
S = bc.S
PAD = (40, 70)                  # px around the 626x288 broadside: a tilting piece may reach past it
CANVAS = (bc.H + 2 * PAD[0], bc.W + 2 * PAD[1])
FRAMES = 3                      # per chunk: in place, then two tumble steps
OVERLAP = 0.006                 # model units each chunk reaches past its seams
GLOW_BAND = 0.025               # the torn flesh along a cut (model units, 125 px each)
CHAR_BAND = 0.075               # the charred chitin beyond it
WRECK_TURRET = 6                # the turret's step the game leaves on the wreck (towards (0, -100)): 67.5 deg
COLOURS, ACCENT = 48, 12

# The seams (px in the broadside, x right from the hull centre): four across the hull (tail | aft |
# middle | fore | head) and one along the spine through the middle section.
CROSS_SEAMS = (-178.0, -62.0, 66.0, 192.0)
# The flank pieces: discs (centre px, radius px) round a membrane, cut out of their sections.
FLANKS = (((160.0, 132.0), 42.0), ((-155.0, -132.0), 42.0))
# name, its tilt at the last frame (radians), turn (radians about the view axis), drift over the
# break-up (px, dx right, dy up)
CHUNKS = [
    ("brood-carrier-chunk-1", 0.30, 0.16, (20, -100)),    # the tail (drifts into view from the left edge)
    ("brood-carrier-chunk-2", 0.22, -0.10, (45, -82)),    # aft, bays 4 and 3
    ("brood-carrier-chunk-3", 0.26, 0.08, (70, -52)),     # middle, upper half
    ("brood-carrier-chunk-4", 0.26, -0.10, (70, -168)),   # middle, lower half
    ("brood-carrier-chunk-5", 0.22, 0.12, (95, -138)),    # fore, bays 2 and 1
    ("brood-carrier-chunk-6", 0.32, -0.20, (110, -120)),  # the head
    ("brood-carrier-chunk-7", 0.45, 0.50, (100, -15)),    # flank, fore upper membrane
    ("brood-carrier-chunk-8", 0.45, -0.45, (40, -205)),   # flank, aft lower membrane
]
# (the drifts: the whole carcass (70, -110), down and to the right from the broadside station so
# the tail at the left edge comes into view, plus each piece's own spread away from the others)

SWAP = 180                      # steps after the death: the chain's end, the body replaced by the chunks
END = SWAP + 2400               # the chunks are gone (40 s of drift: L07's aftermath ends 35 s after the kill)
SINK = 0.3                      # the wreck drawn this much smaller by the end (drifting away from L1)
DARKEN = 0.6                    # its colour this much darker by the end
FADE_FROM = 0.9                 # of the break-up: from here it fades out (after the level has ended)
FRAME_TICKS = 360               # steps per tumble frame: in place, tilting (6 s), tilted (12 s on)
# The torn flesh along the cuts cools per tumble frame: lime at the break-up, dim, then dead; the
# carcass's own lights (eyes, tendrils, spine seam, sacs, core) are dark from the start
COOL = (1.0, 0.35, 0.0)
LEVEL_END = 2100                # steps after the death: L07's aftermath (35 s) is over
ICHOR = f"{SLUG}-ichor"
ICHOR_FRAMES, ICHOR_SIZE = 10, 96
FRAME_STEPS = {"explosion-large": 4, "explosion-medium": 3, ICHOR: 6}
CHAIN = 180                     # the game's chain: death_seconds 3 at 60 steps/s
HALF = bc.H / 2                 # half the hull's length, px


def ease(t):
    return 1 - (1 - np.clip(t, 0.0, 1.0)) ** 2


# --------------------------------------------------------------------------- the blasts

def front(at):
    """Where the chain's front is along the hull at step ``at`` (px, the tail at -HALF)."""
    return -HALF + bc.H * np.clip(at / CHAIN, 0, 1)


def wave():
    """The blasts that run along the hull with the chain: a large one every 9 steps at the front,
    alternating either side of the spine, mediums between them, ichor at the burst sacs' rows."""
    out = []
    rng = np.random.default_rng(25)
    for i, at in enumerate(range(10, CHAIN - 4, 9)):
        x = front(at) + rng.uniform(-14, 14)
        y = (1 if i % 2 else -1) * rng.uniform(25, 85)
        out.append((at, "explosion-large", x, y))
        out.append((at + 4, "explosion-medium", front(at + 4) + rng.uniform(-10, 10), -y * rng.uniform(0.2, 0.9)))
    for at in (40, 95, 150):
        out.append((at, ICHOR, front(at), rng.uniform(-30, 30)))
    return out


def finale():
    """The cluster at the chain's end over every seam and the core, peaking at the swap."""
    out = []
    for i, x in enumerate(CROSS_SEAMS):
        out.append((SWAP - 8 + 3 * i, "explosion-large", x, 40))
        out.append((SWAP - 6 + 3 * i, "explosion-large", x - 6, -48))
        out.append((SWAP - 2 + 2 * i, "explosion-medium", x + 10, 0))
    for (cx, cy), _ in FLANKS:
        out.append((SWAP - 4, "explosion-large", cx - 10, cy - 20))
    out += [(SWAP - 10, "explosion-large", -110, 0), (SWAP - 2, "explosion-large", 120, 0),
            (SWAP, "explosion-large", 0, 30), (SWAP + 2, "explosion-large", 0, -36),
            (SWAP + 4, "explosion-medium", -240, 30), (SWAP + 6, "explosion-medium", 250, -20),
            (SWAP, ICHOR, -60, 20), (SWAP + 4, ICHOR, 60, -20), (SWAP + 8, ICHOR, 0, 0)]
    return out


# ... and the trailing ones on a chunk (index into CHUNKS) wherever it has drifted by then: fire only,
# no ichor (round 25's capture: an ichor burst on a chunk read as the dead carcass still glowing)
TRAILING = [
    (196, "explosion-medium", 2, (-10, 10)), (204, "explosion-large", 3, (0, 6)),
    (214, "explosion-medium", 5, (12, -6)), (224, "explosion-medium", 2, (0, 0)), (232, "explosion-large", 0, (20, 0)),
    (244, "explosion-medium", 6, (0, 0)), (256, "explosion-medium", 1, (-10, -14)),
    (270, "explosion-medium", 3, (0, 0)), (284, "explosion-large", 4, (-14, 8)), (300, "explosion-medium", 7, (0, 0)),
    (318, "explosion-medium", 2, (8, -12)), (340, "explosion-medium", 5, (-6, 10)),
    (366, "explosion-medium", 1, (0, 0)), (392, "explosion-medium", 0, (0, 12)), (430, "explosion-medium", 3, (10, 0)),
]


# --------------------------------------------------------------------------- the pieces

def jag(t, k, a=1.0):
    """A seam's jagged offset (model units) along its run ``t`` (model units)."""
    return a * (0.035 * np.sin(t * 11.0 + 1.3 * k) + 0.02 * np.sin(t * 27.0 + 2.2 * k)
                + 0.012 * np.sin(t * 61.0 + k))


def outside(index, xy):
    """How far outside chunk ``index`` points xy (model units, broadside screen frame) lie (<= 0
    inside). The sections are bands between the cross seams, the middle split by the spine seam,
    with the two flank discs cut out of them."""
    x, y = xy[:, 0], xy[:, 1]
    seams = [s / S + jag(y, k) for k, s in enumerate(CROSS_SEAMS)]
    spine = jag(x, 7, 0.8)
    discs = [np.hypot(x - cx / S, y - cy / S) - (r / S + jag(np.arctan2(y - cy / S, x - cx / S) * 0.4, 9 + i, 0.6))
             for i, ((cx, cy), r) in enumerate(FLANKS)]
    if index >= 6:
        return discs[index - 6]
    band = {0: (None, 0), 1: (0, 1), 2: (1, 2), 3: (1, 2), 4: (2, 3), 5: (3, None)}[index]
    lo, hi = band
    g = np.full(len(x), -1.0)
    if lo is not None:
        g = np.maximum(g, seams[lo] - x)
    if hi is not None:
        g = np.maximum(g, x - seams[hi])
    if index == 2:
        g = np.maximum(g, spine - y)
    if index == 3:
        g = np.maximum(g, y - spine)
    return np.maximum.reduce([g, -discs[0], -discs[1]])


def centre_of(index):
    """The chunk's tumble pivot (model units, broadside): the middle of its in-place pixels."""
    return CENTRES[index]


def chunk_centres():
    xs = np.linspace(-HALF - 20, HALF + 20, 400) / S
    ys = np.linspace(-170, 170, 180) / S
    gx, gy = np.meshgrid(xs, ys)
    pts = np.stack([gx.ravel(), gy.ravel()], axis=-1)
    hull = (np.abs(pts[:, 1]) < 1.15 - 0.6 * np.clip(np.abs(pts[:, 0]) - 1.3, 0, None) ** 1.5)
    out = []
    for i in range(len(CHUNKS)):
        inside = (outside(i, pts) <= 0) & hull
        cx, cy = pts[inside].mean(axis=0)
        out.append(np.array([cx, cy, 0.1]))
    return out


CENTRES = chunk_centres()


def transform(index, f):
    """Screen -> the chunk's own frame at tumble frame f: tilted about an axis in the view plane
    through its centre, turned about the view axis. The sections across the hull roll about the
    screen's y axis, the two middle halves about its x axis, the flank pieces about the tangent."""
    a = f / (FRAMES - 1)
    _, tilt, spin, _ = CHUNKS[index]
    c = centre_of(index)
    if index in (2, 3):
        axis = 0.0
    elif index >= 6:
        axis = np.arctan2(c[1], c[0]) + np.pi / 2
    else:
        axis = np.pi / 2
    side = 1 if (c[0] if index not in (2, 3) else c[1]) >= 0 else -1
    t = tilt * a * side
    s = spin * a

    def rot(q):
        q = rotate_z(q, axis - np.pi / 2)      # the tilt axis onto y ...
        q = np.stack([q[:, 0] * np.cos(t) - q[:, 2] * np.sin(t), q[:, 1],
                      q[:, 0] * np.sin(t) + q[:, 2] * np.cos(t)], axis=-1)
        q = rotate_z(q, np.pi / 2 - axis)      # ... tilted about it, and back
        return rotate_z(q, s)

    return (lambda p: rot(p - c) + c), rot


def wreck_scene():
    """The wreck broadside in the screen frame: every sac burst, the iris open over the burst core,
    the turret at the frame the game leaves on it."""
    # the turret's heading on the wreck, clockwise from the head (the head points right broadside)
    turret_cw = np.radians(90 + WRECK_TURRET * 360 / bc.STEPS)
    nose = bc.hull_model(stage="burst", iris=1.0, turret=True, core_burst=True, turret_cw=turret_cw)
    return lambda p: nose(rotate_z(p, np.pi / 2))


def chunk_model(index, f):
    wreck = wreck_scene()
    to_local, rot = transform(index, f)
    # dead: no light of its own (glow 0); the cut's ichor cools over the tumble frames
    base = bc.materials(glow=0.0, sac_open=0.0, core_open=1.0)
    weak = np.array(em.scheme_colors(SLUG)[4])
    k = COOL[f]
    base[bc.ICHOR] = replace(base[bc.ICHOR], albedo=tuple(weak * 0.35 * (0.45 + 0.55 * k)),
                             emission=tuple(weak * 0.9 * k))
    mats = model_space_materials(base, np.pi / 2)

    def wrap(fn):
        if fn is None:
            return None
        return lambda p, n, fn=fn: fn(to_local(p), rot(n))
    mats = [replace(m, pattern=wrap(m.pattern), emission_pattern=wrap(m.emission_pattern)) for m in mats]
    veins = em._veins(40, 2.0, 0.5)

    def build(q):
        d0, m = wreck(q)
        g = outside(index, q[:, :2])
        d = np.maximum(d0, 0.35 * g - OVERLAP)
        ragged = g + 0.012 * np.sin(q[:, 0] * 41 + q[:, 2] * 7) * np.sin(q[:, 1] * 37)
        m = np.where(ragged > -CHAR_BAND, bc.CHAR, m)
        # the torn flesh glows along the hull's broken edge and in veins down the cut face (d0: how
        # deep under the uncut surface a point lies); the rest of the face is charred
        rim = d0 > -0.045
        vein = veins(q, q) > 0.55
        m = np.where((ragged > -GLOW_BAND) & (rim | vein), bc.ICHOR, m)
        return d, m

    def model(p):
        q = to_local(p)
        g = outside(index, q[:, :2])
        return bm.bounded(q, 0.35 * g - OVERLAP, 0.12, build)
    return model, mats


def job(args):
    index, f = args
    model, mats = chunk_model(index, f)
    w, h = CANVAS
    extent = w / S
    hi = bc.sdf.render(model, mats, (w * 4, h * 4), extent, key_pos=tuple(bc.LIGHT / (extent / 2.3)), steps=160)
    return artkit.native(hi, 4)


def crop_even(img):
    """The sprite cropped to an even-sized box round its pixels, and its centre's offset from the
    hull centre at the canvas centre (px, dx right, dy up)."""
    a = np.array(img)[..., 3]
    ys, xs = np.nonzero(a)
    l, t, r, b = xs.min(), ys.min(), xs.max() + 1, ys.max() + 1
    r += (r - l) % 2
    b += (b - t) % 2
    cx, cy = img.width // 2, img.height // 2
    return img.crop((l, t, r, b)), [int((l + r) // 2 - cx), int(cy - (t + b) // 2)]


def ichor_frames():
    """The death cloud: a lime flash, then lime and teal ichor droplets flung out and fading (additive)."""
    rng = np.random.default_rng(7)
    drops = [(rng.uniform(0, TAU), rng.uniform(0.45, 1.0), rng.uniform(3, 6.5)) for _ in range(18)]
    weak = em.scheme_colors(SLUG)[4] * 255
    teal = np.array(em.hx(em.GLOWS["teal"])) * 255
    frames = []
    for f in range(ICHOR_FRAMES):
        t = f / (ICHOR_FRAMES - 1)
        cv = v8.Canvas(ICHOR_SIZE, ICHOR_SIZE)
        c = ICHOR_SIZE / 2
        fade = (1 - t) ** 1.4
        cv.add(tuple(weak), v8.gauss(cv.dist(c, c), 8 + 22 * t) * 1.1 * (1 - t) ** 2.5)
        cv.add((240, 255, 220), v8.gauss(cv.dist(c, c), 5 + 6 * t) * max(0.0, 1 - 2.5 * t))
        for k, (a, speed, r) in enumerate(drops):
            reach = 40 * speed * (1 - (1 - t) ** 2)
            x, y = c + np.cos(a) * reach, c + np.sin(a) * reach
            colour = teal if k % 3 == 0 else weak
            cv.add(tuple(colour), v8.gauss(cv.dist(x, y), r * (1 + 0.6 * t)) * 0.85 * fade)
            cv.add((230, 255, 200), v8.gauss(cv.dist(x, y), r * 0.4) * 0.5 * fade)
        frames.append(artkit.additive(cv.image()))
    return artkit.quantize_set(frames, 24)


def build():
    jobs = [(i, f) for i in range(len(CHUNKS)) for f in range(FRAMES)]
    with ProcessPoolExecutor(max_workers=8) as pool:
        frames = list(pool.map(job, jobs))
    mapped = bc.quantize_glow(frames, COLOURS, ACCENT)
    offsets = {}
    for i, chunk in enumerate(CHUNKS):
        cut = [crop_even(mapped[i * FRAMES + f]) for f in range(FRAMES)]
        artkit.write_frames(chunk[0], [x[0] for x in cut], SOURCE)
        offsets[chunk[0]] = [x[1] for x in cut]
    artkit.write_frames(ICHOR, ichor_frames(), SOURCE)
    write_death(offsets)


# --------------------------------------------------------------------------- the death's data

def chunk_at(drift, offset, age):
    t = (age - SWAP) / (END - SWAP)
    s = 1 - SINK * np.clip(t, 0, 1)
    e = ease(t)
    return (s * (offset[0] + drift[0] * e), s * (offset[1] + drift[1] * e)), s


def write_death(offsets):
    blasts = [{"at": int(at), "sprite": name, "dx": int(round(dx)), "dy": int(round(dy))}
              for at, name, dx, dy in wave() + finale()]
    for at, name, k, (nx, ny) in TRAILING:
        (cx, cy), _ = chunk_at(CHUNKS[k][3], offsets[CHUNKS[k][0]][0], at)
        blasts.append({"at": at, "sprite": name, "dx": int(round(cx + nx)), "dy": int(round(cy + ny))})
    blasts.sort(key=lambda b: b["at"])
    path = artkit.PIVOTS / f"{SLUG}.json"
    data = json.loads(path.read_text(encoding="utf-8"))
    data["death"] = {
        "source": SOURCE,
        "swap": SWAP, "end": END, "frame_steps": FRAME_TICKS,
        "sink": SINK, "darken": DARKEN, "fade_from": FADE_FROM,
        "chunks": [{"sprite": c[0], "offsets": offsets[c[0]], "drift": list(c[3])} for c in CHUNKS],
        "blast_frame_steps": FRAME_STEPS,
        "blasts": blasts,
    }
    path.write_text(json.dumps(data, indent=1) + "\n", encoding="utf-8")


# --------------------------------------------------------------------------- review material

def death_data():
    return json.loads((artkit.PIVOTS / f"{SLUG}.json").read_text(encoding="utf-8"))["death"]


def draw_chunks(cell, death, chunks, age, centre, motion=True):
    """The chunks as the game draws them at ``age``: drifted, sunk (scaled), darkened, faded."""
    t = np.clip((age - death["swap"]) / (death["end"] - death["swap"]), 0, 1) if motion else 0.0
    s = 1 - death["sink"] * t
    e = ease(t)
    shade = 1 - death["darken"] * t
    alpha = 1.0 if t < death["fade_from"] else 1 - (t - death["fade_from"]) / (1 - death["fade_from"])
    f = min(FRAMES - 1, int((age - death["swap"]) // death["frame_steps"])) if motion else 0
    for spec, frames in zip(death["chunks"], chunks):
        fr = frames[f]
        ox, oy = spec["offsets"][f]
        x = centre[0] + s * (ox + spec["drift"][0] * e)
        y = centre[1] - s * (oy + spec["drift"][1] * e)
        w, h = max(1, round(fr.width * s)), max(1, round(fr.height * s))
        img = fr.resize((w, h), Image.NEAREST) if (w, h) != fr.size else fr
        a = np.array(img).astype(np.float64)
        a[..., :3] *= shade
        a[..., 3] *= alpha
        cell.alpha_composite(Image.fromarray(a.astype(np.uint8), "RGBA"),
                             (int(round(x - w / 2)), int(round(y - h / 2))))
    return cell


def field_outline(cell, centre):
    """The play field round the death at the broadside station (x 170, y 150 below the top)."""
    d = ImageDraw.Draw(cell)
    left, top = centre[0] - 170, centre[1] - 150
    d.rectangle([left, top, left + 479, top + 539], outline=(90, 110, 150, 255))
    return cell


def timed(frames, steps, age):
    if age < 0 or age >= len(frames) * steps:
        return None
    return frames[age // steps]


def chain_bursts():
    """The game's DeathChain for the broadside (parts and 12 hull bursts by their place along the
    hull), as (step, dx, dy, part?)."""
    out = []
    for name, (dx, dy) in bc.offsets(1).items():
        out.append((int(round(np.clip((dx + HALF) / bc.H, 0, 1) * CHAIN)), dx, dy, True))
    for i in range(12):
        along = -HALF + bc.H * (i + 0.5) / 12
        across = (1 if i % 2 == 0 else -1) * bc.W * 0.22
        out.append((int(round((along + HALF) / bc.H * CHAIN)), along, -across, False))
    return out


def wreck_body(art, centre, size):
    """The body as the game draws it until the swap: the broadside frame, every sac burst, the iris
    open without the glow, the turret at the wreck's frame."""
    cell = Image.new("RGBA", size, artkit.PLATE)
    body = bc.compose(art, bc.TURN_FRAMES - 1, sacs=[4] * 8, iris=4, canvas=size,
                      turret_to=np.arctan2(-100, -285))
    cell.alpha_composite(body, (0, 0))
    return cell


def blast_layout(base, death, centre):
    img = base.copy()
    d = ImageDraw.Draw(img)
    colours = {"explosion-large": (255, 120, 40, 255), "explosion-medium": (255, 210, 60, 255),
               ICHOR: (200, 255, 120, 255)}
    radii = {"explosion-large": 30, "explosion-medium": 20, ICHOR: 40}
    cx, cy = centre
    for b in death["blasts"]:
        if b["at"] > death["swap"] + 10:
            continue
        x, y = cx + b["dx"], cy - b["dy"]
        r = radii[b["sprite"]]
        d.ellipse([x - r, y - r, x + r, y + r], outline=colours[b["sprite"]])
        raster.draw_text(img, x - 6, y - 3, str(b["at"]), colours[b["sprite"]])
    for at, dx, dy, part in chain_bursts():
        x, y = cx + dx, cy - dy
        col = (255, 255, 255, 255) if part else (150, 200, 255, 255)
        d.rectangle([x - 2, y - 2, x + 2, y + 2], outline=col)
        raster.draw_text(img, x + 4, y + 2, str(at), col)
    return img


def review():
    artkit.REVIEW_ROUND = REVIEW_ROUND
    death = death_data()
    chunks = [artkit.load_frames(c["sprite"]) for c in death["chunks"]]
    art = bc.load_art()
    sprites = {n: artkit.load_frames(n) for n in FRAME_STEPS}
    size = (900, 690)
    centre = (450, 295)

    def plate():
        return Image.new("RGBA", size, artkit.PLATE)

    composed = draw_chunks(plate(), death, chunks, 0, centre, motion=False)
    body = wreck_body(art, centre, size)
    ages = (death["swap"] + 60, 700, 1400, LEVEL_END)
    stages = [field_outline(draw_chunks(plate(), death, chunks, age, centre), centre) for age in ages]
    tumble = [f for frames in chunks for f in frames]
    sheet = artkit.review_sheet("BROOD CARRIER DEATH - FINAL BREAK-UP", [
        ("THE EIGHT CHUNKS AT THEIR OFFSETS, FRAME 0, NO MOTION; BESIDE THE BODY AS THE GAME DRAWS IT UNTIL THE SWAP",
         [composed, body], 1, False),
        (f"THE BREAK-UP AT STEPS {', '.join(map(str, ages))} (SWAP {death['swap']}, THE LEVEL ENDS AT {LEVEL_END}, "
         f"END {death['end']}): DRIFT, SINK (TO {1 - death['sink']:.2f}X), DARKEN, FADE FROM {death['fade_from']:.0%}; "
         f"THE PLAY FIELD (480X540, THE DEATH AT THE BROADSIDE STATION) OUTLINED", stages[:2], 1, False),
        ("", stages[2:], 1, False),
        (f"EACH CHUNK'S {FRAMES} TUMBLE FRAMES (TAIL, AFT, MIDDLE UPPER, MIDDLE LOWER, FORE, HEAD, FLANKS), "
         f"{death['frame_steps']} STEPS EACH; DEAD (NO EYES, TENDRILS OR CORE LIT), THE CUTS COOLING "
         f"{' / '.join(f'{k:.0%}' for k in COOL)}", tumble[:12], 1, False),
        ("", tumble[12:], 1, False),
        ("THE BLASTS UP TO THE SWAP (STEP AFTER THE DEATH): LARGE ORANGE, MEDIUM AMBER, ICHOR LIME; THE GAME'S "
         "CHAIN: PARTS WHITE, HULL BURSTS BLUE", [blast_layout(body, death, centre)], 1, False),
        ("DEATH ICHOR (ADDITIVE, 10 FPS)", sprites[ICHOR], 1, True),
    ], width=2000, batch=BATCH)
    artkit.save_review(sheet, death_loop(art, death, chunks, sprites, size, centre), bc.UNIT / "concept",
                       "brood-carrier-death", fps=30)


def death_loop(art, death, chunks, sprites, size, centre):
    """The whole death at 30 fps (every second step, from 5 s after the swap a 10x time-lapse to the
    level's end): the live carrier, the chain with this table's blasts over the body, the flash at
    the chain's end, the break-up drifting apart through the aftermath."""
    alive = 20
    chain = chain_bursts()
    gif = []
    # every second step up to 5 s after the swap, then a time-lapse (every 20th step) to the level's end
    slow = alive + death["swap"] + 300
    steps = list(range(0, slow, 2)) + list(range(slow, alive + LEVEL_END + 1, 20))
    for step in steps:
        age = step - alive
        if age < 0:
            cell = plate_copy(bc.compose(art, bc.TURN_FRAMES - 1, sacs=[0, 3, 0, 3, 4, 0, 4, 0], iris=4, glow=1.0,
                                         canvas=size, ship=(-150, -330)))
        elif age < death["swap"]:
            cell = wreck_body(art, centre, size)
        else:
            cell = draw_chunks(Image.new("RGBA", size, artkit.PLATE), death, chunks, age, centre)
        layers = []
        for at, dx, dy, part in chain:
            layers.append(("explosion-medium", age - at, (dx, dy)))
            if part:
                layers.append((ICHOR, age - at, (dx, dy)))
        head = (HALF, 0)
        layers += [("explosion-large", age - CHAIN, head), ("explosion-large", age - CHAIN, (0, 0)),
                   (ICHOR, age - CHAIN, (0, 0))]
        for b in death["blasts"]:
            layers.append((b["sprite"], age - b["at"], (b["dx"], b["dy"])))
        for name, a, (dx, dy) in layers:
            fr = timed(sprites[name], death["blast_frame_steps"][name], a)
            if fr is not None:
                x, y = centre[0] + dx, centre[1] - dy
                cell = artkit.add_light(cell, fr, (int(x - fr.width / 2), int(y - fr.height / 2)))
        if CHAIN <= age < CHAIN + 12:
            k = 1 - (age - CHAIN) / 12
            a = np.array(cell).astype(np.float64)
            a[..., :3] = a[..., :3] + (255 - a[..., :3]) * 0.8 * k
            cell = Image.fromarray(a.astype(np.uint8), "RGBA")
        gif.append(cell)
    return gif


def plate_copy(img):
    out = Image.new("RGBA", img.size, artkit.PLATE)
    out.alpha_composite(img)
    return out


if __name__ == "__main__":
    if "--data" in sys.argv[1:]:
        write_death({c["sprite"]: c["offsets"] for c in death_data()["chunks"]})
    elif "--review" not in sys.argv[1:]:
        build()
    review()
