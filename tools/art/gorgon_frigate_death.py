#!/usr/bin/env python3
"""Production art: the Gorgon Frigate's break-up at its death (design/enemies/bosses/gorgon-frigate;
M4 part E batch, redo after round 21: "the death needs more explosions and pieces, now it just
disappears"). The mechanism is the Leviathan's (tools/art/leviathan_death.py, the game's
SetPieceDeath).

Outputs (assets/sprites/ and assets/pivots/), every sprite under gorgon_frigate.py's fixed key light:
  gorgon-frigate-chunk-1..5_0..2  the bell with its crown open (the petals already torn off: the
                                  game plays gorgon-frigate-petals at the swap) and the core burst
                                  into wet ichor, cut into five wedges round the core along jagged
                                  seams: 1 the right flank with its socket, 2 the back right and
                                  3 the back left with the tendril veil, 4 the left flank with its
                                  socket, 5 the front with the centre socket. The dome along a cut
                                  is charred, the torn flesh glows along the dome's broken edge and in veins down the cut face. Frame 0 is in place: drawn
                                  at its offset with no motion the five reproduce the bell. Frames
                                  1 and 2 are the wedge tilting outwards about its own centre and
                                  turning a little, each a render of the turned model.
  gorgon-frigate-chain-1..3_0..2  each neck with its torn head stump (left, centre, right) falling
                                  away: frame 0 at rest, frames 1 and 2 the chain turning and
                                  curling, each piece the pre-rendered neck or stump sprite at its
                                  heading (nothing lit is rotated)
  pivots/gorgon-frigate.json "death"  the break-up the game plays (px, dx right, dy up from the
                                  bell centre; times in game steps of 1/60 s from the death): the
                                  chunks (chains first, drawn under the bell) with their centres per
                                  frame and drift, the swap, the end, sinking, darkening and fade,
                                  and the blast table (explosion-large/-medium, gorgon-frigate-ichor)
  design/enemies/bosses/gorgon-frigate/concept/gorgon-frigate-death-final-r21-b.png/.gif  review
                                  sheet and the whole death over the body

The timeline: the parts' chain (a medium burst and an ichor cloud at each head and the core, 6
steps apart) and the credit shower stay in the game code; a cluster of large blasts over the bell
and the necks from step 24 peaks at the swap (step 54), where the body is replaced by the chunks
and the petals tear off; then the chunks drift apart for 106 steps (1.8 s), sinking, darkening and
fading, under trailing blasts and ichor that follow them.

Run: python3 tools/art/gorgon_frigate_death.py [--data | --review]   (~2 min on 20 cores; after
gorgon_frigate.py and explosions.py; --data keeps the chunk sprites and rewrites only the death's
data and the review, --review only the review)
"""
import json
import sys
from concurrent.futures import ProcessPoolExecutor
from dataclasses import replace

import numpy as np
from PIL import Image, ImageDraw

import artkit
from artkit import sprite

import gorgon_frigate as gf  # noqa: E402  (tools/art: the boss's models, materials, layout)
from render import raster, sdf  # noqa: E402
from render.enemy_models import V_EYE, V_GLOW  # noqa: E402
from render.sdf import rotate_z, sd_ellipsoid, union  # noqa: E402

SCRIPT = "gorgon_frigate_death.py"
BATCH = "M4 part E batch (redo)"
SOURCE = artkit.source_note(SCRIPT, BATCH)
REVIEW_ROUND = "r21"
REVIEW_VARIANT = "b"            # round 21: the redo of the death in gorgon-frigate-final-r21-a
SLUG = gf.SLUG
PAD = 30                        # px around the 240x200 bell: a tilting wedge may reach past it
CANVAS = (gf.BELL_W + 2 * PAD, gf.BELL_H + 2 * PAD)
CHAIN_CANVAS = (420, 520)       # centred on the bell centre: the chains hang down to ~-220 px
FRAMES = 3                      # per chunk: in place, then two tumble steps
OVERLAP = 0.008                 # model units each wedge reaches past its seams
GLOW_BAND = 0.02                # the torn flesh along a cut (model units, 100 px each)
CHAR_BAND = 0.06                # the charred dome beyond it
CORE = np.array([gf.CORE[0], gf.CORE[1]])

# The wedges round the core: seam angles (degrees counter-clockwise from screen right, y up; the
# sockets lie at 225, 270 and 315), a wedge from one seam to the next. name, (seam from, seam to),
# its tilt at the last frame (radians, outer edge down), its turn (radians, about the view axis),
# its drift over the break-up (screen px, dx right, dy up)
SEAMS = (35.0, 105.0, 170.0, 248.0, 292.0)
WEDGES = [
    ("gorgon-frigate-chunk-1", (4, 0), 0.32, -0.20, (72, -8)),     # right flank, right socket
    ("gorgon-frigate-chunk-2", (0, 1), 0.30, 0.15, (34, 48)),      # back right, tendrils
    ("gorgon-frigate-chunk-3", (1, 2), 0.30, -0.15, (-36, 46)),    # back left, tendrils
    ("gorgon-frigate-chunk-4", (2, 3), 0.32, 0.20, (-72, -10)),    # left flank, left socket
    ("gorgon-frigate-chunk-5", (3, 4), 0.28, 0.10, (2, -34)),      # front, centre socket
]
# The chains (left, centre, right): their rigid turn and curl at the last frame (radians, counter-
# clockwise), and their drift (they fall away down the screen)
CHAINS = [
    ("gorgon-frigate-chain-1", -0.55, -0.35, (-46, -64)),
    ("gorgon-frigate-chain-2", 0.30, 0.25, (4, -82)),
    ("gorgon-frigate-chain-3", 0.55, 0.35, (46, -64)),
]

SWAP = 54                       # steps after the death: the body is replaced by the chunks
END = 160                       # the chunks are gone
SINK = 0.15                     # the wreck drawn this much smaller by the end
DARKEN = 0.6                    # its colour this much darker by the end
FADE_FROM = 0.55                # of the break-up: from here it fades out
FRAME_TICKS = -(-(END - SWAP) // FRAMES)
ICHOR = f"{SLUG}-ichor"
FRAME_STEPS = {"explosion-large": 4, "explosion-medium": 3, ICHOR: 6}
CHAIN_STEP = 6                  # the game's part chain (LevelScreen)
PETAL_STEPS = 6                 # the petals' frame steps (LevelScreen)

# The blasts at the death (step, sprite, dx, dy): the cluster over the bell and the necks up to the
# swap ...
CLUSTER = [
    (24, "explosion-large", -70, 12), (28, "explosion-large", 62, -18), (32, "explosion-large", 4, 44),
    (36, "explosion-large", -50, -100), (40, "explosion-large", 74, 30), (43, "explosion-large", 46, -118),
    (46, "explosion-large", -88, -22), (48, "explosion-large", 0, -150), (50, "explosion-large", 26, 22),
    (52, "explosion-large", -32, 52), (54, "explosion-large", 0, 8), (55, "explosion-large", 92, -8),
    (56, "explosion-large", -96, 4),
    (44, "explosion-medium", -20, -62), (50, "explosion-medium", 52, 62), (53, "explosion-medium", -66, -150),
    (56, "explosion-medium", 64, -150), (58, "explosion-medium", 0, -84), (60, "explosion-medium", 34, 40),
    (52, ICHOR, 0, 10), (56, ICHOR, -56, -118),
]
# ... and the trailing ones on a chunk (index into chains + wedges) wherever it has drifted by then
TRAILING = [
    (66, "explosion-medium", 3, (-6, 6)), (74, "explosion-large", 1, (0, -10)),
    (82, "explosion-medium", 6, (8, 0)), (90, ICHOR, 4, (0, 6)), (98, "explosion-medium", 2, (0, -12)),
    (108, "explosion-large", 7, (-6, 4)), (118, "explosion-medium", 0, (6, -8)),
    (130, "explosion-medium", 5, (-10, 6)),
]


def ease(t):
    return 1 - (1 - np.clip(t, 0.0, 1.0)) ** 2


# --------------------------------------------------------------------------- the bell's wedges

def seam_side(k, q):
    """Signed distance-ish of local points q (x, y about the core) to seam k's jagged half-line:
    positive on its counter-clockwise side."""
    a = np.radians(SEAMS[k])
    along = q[:, 0] * np.cos(a) + q[:, 1] * np.sin(a)
    across = -q[:, 0] * np.sin(a) + q[:, 1] * np.cos(a)
    jag = (0.035 * np.sin(along * 11.0 + 1.3 * k) + 0.02 * np.sin(along * 27.0 + 2.2 * k)
           + 0.012 * np.sin(along * 61.0 + k))
    return across - jag


def outside(wedge, p):
    """How far outside the wedge a point is (<= 0 inside; a scaled-down bound)."""
    a, b = wedge[1]
    q = p[:, :2] - CORE
    g = np.maximum(-seam_side(a, q), seam_side(b, q))
    return g


def mid_angle(wedge):
    a, b = (np.radians(SEAMS[k]) for k in wedge[1])
    if b < a:
        b += 2 * np.pi
    return (a + b) / 2


def centre_of(wedge):
    """The wedge's tumble pivot (model units): along its middle, a third of the way out."""
    m = mid_angle(wedge)
    r = 0.45
    return np.array([CORE[0] + r * 1.3 * np.cos(m), CORE[1] + r * np.sin(m), 0.15])


def turn(wedge, f):
    """Model -> the wedge's own frame at tumble frame f: tilted about the tangent at its middle
    (its outer edge going down), turned about the view axis."""
    a = f / (FRAMES - 1)
    m = mid_angle(wedge)
    tilt, spin = wedge[2] * a, wedge[3] * a
    c = centre_of(wedge)

    def rot(q):
        q = rotate_z(q, -m)                 # the radial direction onto x ...
        q = rotate_z(q, spin)
        q = np.stack([q[:, 0] * np.cos(tilt) - q[:, 2] * np.sin(tilt), q[:, 1],
                      q[:, 0] * np.sin(tilt) + q[:, 2] * np.cos(tilt)], axis=-1)   # ... tilted
        return rotate_z(q, m)

    return (lambda p: rot(p - c) + c), rot


def wedge_model(index, f):
    wedge = WEDGES[index]
    scene, mats = gf.bell_model(1.0, petals=False)
    to_local, rot = turn(wedge, f)

    def wrap(fn):
        if fn is None:
            return None
        return lambda p, n, fn=fn: fn(to_local(p), rot(n))
    mats = [replace(m, pattern=wrap(m.pattern), emission_pattern=wrap(m.emission_pattern)) for m in mats]
    cz = gf.dome_z(*gf.CORE)

    def model(p):
        q = to_local(p)
        d, m = scene(q)
        # the crown's petals are gone (the game's petals sprite tears them off at the swap) and
        # the core has burst: wet ichor in its socket
        m = np.where(m == V_EYE, gf.ICHOR, m)
        d, m = union((d, m), (sd_ellipsoid(q, (CORE[0], CORE[1], cz - 0.04), (0.24, 0.2, 0.08)), gf.ICHOR), k=0.03)
        g = outside(wedge, q)
        d = np.maximum(d, 0.35 * g - OVERLAP)
        ragged = g + 0.012 * np.sin(q[:, 0] * 41 + q[:, 2] * 7) * np.sin(q[:, 1] * 37)
        m = np.where(ragged > -CHAR_BAND, gf.CHAR, m)
        # the torn flesh glows along the dome's broken edge and in veins down the cut face; the
        # rest of the face is charred
        rim = q[:, 2] > dome_surface(q) - 0.05
        vein = np.sin(q[:, 0] * 23 + q[:, 2] * 31) * np.sin(q[:, 1] * 19 - q[:, 2] * 17) > 0.55
        m = np.where((ragged > -GLOW_BAND) & (rim | vein), V_GLOW, m)
        return d, m
    return model, mats


def dome_surface(q):
    """gf.dome_z over an array of points."""
    (cx, cy, cz), (rx, ry, rz) = gf.DOME
    t = 1 - ((q[:, 0] - cx) / rx) ** 2 - ((q[:, 1] - cy) / ry) ** 2
    return cz + rz * np.sqrt(np.clip(t, 0.0, None))


def key_pos():
    """The key light where the bell's own render has it (sdf.render places it relative to the
    canvas centre and scaled by the extent), for the padded canvas centred on the bell centre."""
    bell_extent = gf.BELL_W / gf.S
    bell_centre = np.array([(gf.BELL_W / 2 - gf.ORIGIN[0]) / gf.S, (gf.ORIGIN[1] - gf.BELL_H / 2) / gf.S, 0.0])
    light = sdf.KEY_POS * (bell_extent / 2.3) + bell_centre
    return tuple(light * 2.3 / (CANVAS[0] / gf.S))


def job(args):
    index, f = args
    model, mats = wedge_model(index, f)
    hi = sdf.render(model, mats, (CANVAS[0] * 4, CANVAS[1] * 4), CANVAS[0] / gf.S, steps=160, key_pos=key_pos())
    return artkit.native(hi, 4)


def crop_even(img):
    """The sprite cropped to an even-sized box round its pixels, and its centre's offset from the
    bell centre at the canvas centre (px, dx right, dy up)."""
    a = np.array(img)[..., 3]
    ys, xs = np.nonzero(a)
    l, t, r, b = xs.min(), ys.min(), xs.max() + 1, ys.max() + 1
    r += (r - l) % 2
    b += (b - t) % 2
    cx, cy = img.width // 2, img.height // 2
    return img.crop((l, t, r, b)), [int((l + r) // 2 - cx), int(cy - (t + b) // 2)]


# --------------------------------------------------------------------------- the chains

def chain_frame(art, c, f):
    """Chain c (its necks and the torn stump) at tumble frame f on the chain canvas: the rest
    layout turned rigidly about its middle and curled, each piece at its pre-rendered heading."""
    _, turn_to, curl, _ = CHAINS[c]
    a = f / (FRAMES - 1)
    pieces = gf.CHAINS[c]["segments"] + 1
    angles = [turn_to * a + curl * a * (k + 1) / pieces for k in range(pieces)]
    rest = gf.chain_layout(c, [0.0] * pieces)
    lay = gf.chain_layout(c, angles)
    # keep the chain's middle where it was at rest
    mx = np.mean([x for x, _, _ in rest]) - np.mean([x for x, _, _ in lay])
    my = np.mean([y for _, y, _ in rest]) - np.mean([y for _, y, _ in lay])
    img = Image.new("RGBA", CHAIN_CANVAS, (0, 0, 0, 0))
    cx, cy = CHAIN_CANVAS[0] // 2, CHAIN_CANVAS[1] // 2
    for k, (x, y, phi) in enumerate(lay):
        kind = f"neck-{k + 1}" if k < pieces - 1 else "stump"
        sprite.paste_center(img, art[kind][gf.frame_index(phi)], cx + x + mx, cy - (y + my))
    return img


def build():
    jobs = [(i, f) for i in range(len(WEDGES)) for f in range(FRAMES)]
    with ProcessPoolExecutor(max_workers=15) as pool:
        frames = list(pool.map(job, jobs))
    mapped = artkit.quantize_set(frames, 48)
    offsets = {}
    for i, wedge in enumerate(WEDGES):
        cut = [crop_even(mapped[i * FRAMES + f]) for f in range(FRAMES)]
        artkit.write_frames(wedge[0], [x[0] for x in cut], SOURCE)
        offsets[wedge[0]] = [x[1] for x in cut]
    art = gf.load_art()
    for c, chain in enumerate(CHAINS):
        cut = [crop_even(chain_frame(art, c, f)) for f in range(FRAMES)]
        artkit.write_frames(chain[0], [x[0] for x in cut], SOURCE)
        offsets[chain[0]] = [x[1] for x in cut]
    write_death(offsets)


# --------------------------------------------------------------------------- the death's data

def all_chunks():
    """Chains first (drawn under the bell), then the wedges: (sprite, drift)."""
    return [(c[0], c[3]) for c in CHAINS] + [(w[0], w[4]) for w in WEDGES]


def chunk_at(drift, offset, age):
    t = (age - SWAP) / (END - SWAP)
    s = 1 - SINK * np.clip(t, 0, 1)
    e = ease(t)
    return (s * (offset[0] + drift[0] * e), s * (offset[1] + drift[1] * e)), s


def write_death(offsets):
    chunks = all_chunks()
    blasts = [{"at": at, "sprite": name, "dx": dx, "dy": dy} for at, name, dx, dy in CLUSTER]
    for at, name, k, (nx, ny) in TRAILING:
        (cx, cy), _ = chunk_at(chunks[k][1], offsets[chunks[k][0]][0], at)
        blasts.append({"at": at, "sprite": name, "dx": int(round(cx + nx)), "dy": int(round(cy + ny))})
    blasts.sort(key=lambda b: b["at"])
    path = artkit.PIVOTS / f"{SLUG}.json"
    data = json.loads(path.read_text(encoding="utf-8"))
    data["death"] = {
        "source": SOURCE,
        "swap": SWAP, "end": END, "frame_steps": FRAME_TICKS,
        "sink": SINK, "darken": DARKEN, "fade_from": FADE_FROM,
        "chunks": [{"sprite": name, "offsets": offsets[name], "drift": list(drift)} for name, drift in chunks],
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


def timed(frames, steps, age):
    if age < 0 or age >= len(frames) * steps:
        return None
    return frames[age // steps]


def blast_layout(base, death, centre):
    img = base.copy()
    d = ImageDraw.Draw(img)
    colours = {"explosion-large": (255, 120, 40, 255), "explosion-medium": (255, 210, 60, 255),
               ICHOR: (200, 120, 255, 255)}
    radii = {"explosion-large": 34, "explosion-medium": 22, ICHOR: 44}
    cx, cy = centre
    for b in death["blasts"]:
        x, y = cx + b["dx"], cy - b["dy"]
        r = radii[b["sprite"]]
        col = colours[b["sprite"]] if b["at"] <= death["swap"] + 6 else (120, 255, 140, 255)
        d.ellipse([x - r, y - r, x + r, y + r], outline=col)
        raster.draw_text(img, x - 6, y - 3, str(b["at"]), col)
    for p, part in enumerate(gf.SPEC["part_list"]):
        x, y = cx + part["offset"][0], cy - part["offset"][1]
        d.rectangle([x - 2, y - 2, x + 2, y + 2], outline=(255, 255, 255, 255))
        raster.draw_text(img, x + 4, y + 2, str(p * CHAIN_STEP), (255, 255, 255, 255))
    return img


def review():
    artkit.REVIEW_ROUND = REVIEW_ROUND
    death = death_data()
    chunks = [artkit.load_frames(c["sprite"]) for c in death["chunks"]]
    art = gf.load_art()
    sprites = {n: artkit.load_frames(n) for n in FRAME_STEPS}
    size = (440, 520)
    centre = (220, 200)
    heads = ("left head", "centre head", "right head")
    rest = [[0.0] * 6 for _ in gf.CHAINS]

    def body_cell():
        return gf.compose(art, rest, crown=3, wrecked=heads, size=size, centre=centre)

    def plate():
        return Image.new("RGBA", size, artkit.PLATE)

    composed = draw_chunks(plate(), death, chunks, 0, centre, motion=False)
    stages = [draw_chunks(plate(), death, chunks, age, centre) for age in (death["swap"], 84, 114, 140)]
    tumble = [f for frames in chunks for f in frames]
    sheet = artkit.review_sheet("GORGON FRIGATE DEATH - FINAL BREAK-UP", [
        ("THE THREE CHAINS AND FIVE WEDGES AT THEIR OFFSETS, FRAME 0, NO MOTION; BESIDE THE BODY AT ITS DEATH",
         [composed, body_cell()], 1, False),
        (f"THE BREAK-UP AT STEPS {death['swap']} (SWAP), 84, 114, 140 OF {death['end']}: DRIFT, SINK "
         f"(TO {1 - death['sink']:.2f}X), DARKEN, FADE FROM {death['fade_from']:.0%}", stages, 1, False),
        (f"EACH CHUNK'S {FRAMES} TUMBLE FRAMES (CHAINS LEFT, CENTRE, RIGHT; WEDGES RIGHT, BACK RIGHT, BACK LEFT, "
         f"LEFT, FRONT), {death['frame_steps']} STEPS EACH", tumble, 1, False),
        ("THE BLASTS (STEP AFTER THE DEATH): LARGE ORANGE, MEDIUM AMBER, ICHOR VIOLET; AFTER THE SWAP GREEN, "
         "ON THE DRIFTING CHUNKS; WHITE: THE PART CHAIN", [blast_layout(body_cell(), death, centre)], 1, False),
    ], width=2000, batch=BATCH)
    fps = 30
    alive = 20
    parts = gf.SPEC["part_list"]
    core = gf.PART["core"]["offset"]
    gif = []
    for f in range(int(fps * 3.4)):
        step = f * 2
        age = step - alive
        if age < 0:
            cell = gf.compose(art, rest, crown=3, wrecked=heads, glow=1.0, size=size, centre=centre)
        elif age < death["swap"]:
            cell = body_cell()
        else:
            cell = draw_chunks(plate(), death, chunks, age, centre)
            fr = timed(art["petals"], PETAL_STEPS, age - death["swap"])
            if fr is not None:
                sprite.paste_center(cell, fr, centre[0] + core[0], centre[1] - core[1])
        layers = []
        for p, part in enumerate(parts):
            for name in (ICHOR, "explosion-medium"):
                layers.append((name, age - p * CHAIN_STEP, part["offset"]))
        for b in death["blasts"]:
            layers.append((b["sprite"], age - b["at"], (b["dx"], b["dy"])))
        for name, a, (dx, dy) in layers:
            fr = timed(sprites[name], death["blast_frame_steps"][name], a)
            if fr is not None:
                x, y = centre[0] + dx, centre[1] - dy
                cell = artkit.add_light(cell, fr, (int(x - fr.width / 2), int(y - fr.height / 2)))
        gif.append(cell)
    artkit.save_review(sheet, gif, gf.UNIT / "concept", "gorgon-frigate-death", fps=fps, variant=REVIEW_VARIANT)


if __name__ == "__main__":
    if "--data" in sys.argv[1:]:
        write_death({c["sprite"]: c["offsets"] for c in death_data()["chunks"]})
    elif "--review" not in sys.argv[1:]:
        build()
    review()
