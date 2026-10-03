#!/usr/bin/env python3
"""Production art: the Leviathan's break-up at its death (design/enemies/space/leviathan; M4 part C
batch, redo after round 16: "the body vanishing looks strange").

Outputs (assets/sprites/ and assets/pivots/), every sprite under leviathan.py's fixed key light:
  leviathan-chunk-1..5_0..2.png  the second-pass body (every part wrecked, the tail unswayed) cut
                                 into five chunks along jagged seams: 1 the head, 2 and 3 the
                                 middle split along the spine (screen left and right, each with
                                 its fin and vent), 4 the back with the blowhole and vents 3-4,
                                 5 the tail with the fluke stump. The hide along a cut is charred
                                 and the torn flesh glows violet. Frame 0 is in place: drawn at
                                 its offset with no motion the five reproduce the body. Frames 1
                                 and 2 are the chunk tumbling a little about its own centre (pitch
                                 for the head, the back and the tail, roll for the flanks), each a
                                 render of the turned model, so nothing lit is rotated at runtime.
                                 One palette of 48 colours (8 for the glows) over all 15 frames.
  pivots/leviathan.json "death"  the break-up the game plays (px, dx right, dy up from the unit
                                 centre facing down; times in game steps of 1/60 s from the
                                 death): the chunks' sprite centres per frame and their drift,
                                 the swap (the step the body is replaced by the chunks), the end,
                                 the sinking (scale), darkening and fade, and the blast table
                                 (the explosion-large/-medium and leviathan-ichor sprites with
                                 their frame steps). The part chain (a medium burst and an ichor
                                 cloud at each part, 6 steps apart) stays in the game code.
  design/enemies/space/leviathan/concept/leviathan-death-final-r16-b.png/.gif  review sheet and
                                 the whole death over the body

The timeline: the parts' chain from step 0 over the intact body; a cluster of eleven large blasts
over the body (spine, flanks, head, tail) from step 30 that peaks at the swap (step 60), with
mediums and two ichor clouds around it; then the chunks drift apart for 110 steps (1.8 s), sinking
(drawn smaller), darkening and fading out, under trailing blasts and ichor that follow them.

Run: python3 tools/art/leviathan_death.py [--data | --review]   (~5 min on 20 cores; after leviathan.py,
explosions.py and vrell_fx.py, whose sprites the review plays; --data keeps the chunk sprites and
rewrites only the death's data and the review, --review only the review)
"""
import json
import sys
from concurrent.futures import ProcessPoolExecutor
from dataclasses import replace

import numpy as np
from PIL import Image, ImageDraw

import artkit
from artkit import TAU, sprite

import leviathan as lev  # noqa: E402  (tools/art: the unit's model, materials, light and compose)
from render import raster  # noqa: E402
from render.enemy_models import V_GLOW  # noqa: E402
from render.sdf import rotate_x, rotate_y  # noqa: E402

SCRIPT = "leviathan_death.py"
BATCH = "M4 part C batch (redo)"
SOURCE = artkit.source_note(SCRIPT, BATCH)
REVIEW_ROUND = "r16"
REVIEW_VARIANT = "b"          # round 16 reopened: the redo of leviathan-death-final-r16-a
PAD = 24                        # px around the 300x480 canvas: a tumbling chunk may reach past it
CANVAS = (lev.W + 2 * PAD, lev.H + 2 * PAD)
FRAMES = 3                      # per chunk: in place, then two tumble steps
OVERLAP = 0.008                 # model units each chunk reaches past its cuts, so the seams leave no hole
GLOW_BAND = 0.025               # the torn flesh along a cut (model units)
CHAR_BAND = 0.1                 # the charred hide beyond it

# Cuts across the body, model y (head +Y): head | middle, middle | back, back | tail.
CUTS = (1.0, -0.4, -1.38)
# name, (its cut towards the head, its cut towards the tail; None: open), side of the spine split
# (+1 the fish's right, which is the screen's left), the tumble at the last frame (pitch about x,
# roll about y; radians), the
# drift over the break-up (screen px, dx right, dy up)
CHUNKS = [
    ("leviathan-chunk-1", (None, 0), 0, (0.30, 0.0), (5, -55)),      # head, drifts down the screen
    ("leviathan-chunk-2", (0, 1), 1, (0.0, 0.35), (-42, -8)),        # middle, screen left
    ("leviathan-chunk-3", (0, 1), -1, (0.0, -0.35), (42, -3)),       # middle, screen right
    ("leviathan-chunk-4", (1, 2), 0, (-0.15, 0.10), (-8, 25)),       # back
    ("leviathan-chunk-5", (2, None), 0, (-0.30, 0.0), (10, 57)),      # tail, drifts up
]

SWAP = 60                       # steps after the death: the body is replaced by the chunks
END = 170                       # the chunks are gone
SINK = 0.15                     # the wreck drawn this much smaller by the end (sinking away)
DARKEN = 0.6                    # its colour this much darker by the end
FADE_FROM = 0.55                # of the break-up: from here it fades out
FRAME_TICKS = -(-(END - SWAP) // FRAMES)
FRAME_STEPS = {"explosion-large": 4, "explosion-medium": 3, "leviathan-ichor": 6}
CHAIN_STEP = 6                  # the game's part chain (LevelScreen)

# The blasts over the body at its death (step, sprite, dx, dy): the cluster up to the swap ...
CLUSTER = [
    (30, "explosion-large", 0, -40), (36, "explosion-large", -40, 80), (40, "explosion-large", 38, -60),
    (44, "explosion-large", 0, -175), (48, "explosion-large", -46, -10), (51, "explosion-large", 0, 170),
    (54, "explosion-large", 46, 40), (56, "explosion-large", 6, 105), (58, "explosion-large", -22, -120),
    (60, "explosion-large", 24, 155), (61, "explosion-large", 18, -190),
    (54, "leviathan-ichor", 0, -125), (58, "leviathan-ichor", 0, 120),
    (56, "explosion-medium", -52, -150), (58, "explosion-medium", 52, -130), (60, "explosion-medium", 0, 30),
    (62, "explosion-medium", -40, 145), (64, "explosion-medium", 58, 95),
]
# ... and the trailing ones, on a chunk (index) wherever it has drifted by then, plus a nudge
TRAILING = [
    (72, "explosion-medium", 1, (-10, 8)), (80, "explosion-large", 0, (6, -10)),
    (88, "explosion-medium", 2, (12, -6)), (96, "leviathan-ichor", 3, (0, 0)),
    (104, "explosion-medium", 4, (-8, 12)), (118, "explosion-large", 1, (4, -4)),
    (122, "explosion-medium", 0, (-12, 6)), (136, "explosion-medium", 2, (0, 10)),
]


def ease(t):
    return 1 - (1 - np.clip(t, 0.0, 1.0)) ** 2


# --------------------------------------------------------------------------- the chunks

def cut_y(k, x, z):
    """Cut k's jagged surface: model y as a function of x and z."""
    return (CUTS[k] + 0.045 * np.sin(x * 9.3 + 1.7 * k) + 0.03 * np.sin(x * 23.7 + 2.1 * k)
            + 0.018 * np.sin(x * 51.0 + k) + 0.012 * np.sin(z * 19 + k))


def split_x(y, z):
    """The middle's split along the spine: model x as a function of y and z."""
    return (0.03 * np.sin(y * 8.1) + 0.022 * np.sin(y * 21.3 + 1.0) + 0.014 * np.sin(y * 47.0 + 2.0)
            + 0.01 * np.sin(z * 17))


def outside(chunk, p):
    """How far outside chunk's slab a point is (<= 0 inside; a scaled-down distance bound)."""
    _, (above, below), side, _, _ = chunk
    x, y, z = p[:, 0], p[:, 1], p[:, 2]
    g = np.full(len(p), -1.0)
    if above is not None:
        g = np.maximum(g, y - cut_y(above, x, z))
    if below is not None:
        g = np.maximum(g, cut_y(below, x, z) - y)
    if side:
        g = np.maximum(g, -side * (x - split_x(y, z)))
    return 0.4 * g - OVERLAP


def centre_of(chunk):
    """The chunk's centre in model units, its tumble pivot."""
    _, (above, below), side, _, _ = chunk
    top = CUTS[above] if above is not None else 2.3
    bottom = CUTS[below] if below is not None else lev.mxy("fluke")[1] - 0.2
    return np.array([0.3 * side, (top + bottom) / 2, 0.0])


def turn(chunk, f):
    """Model -> the chunk's own frame at tumble frame f (its pieces as before the break)."""
    pitch, roll = chunk[3]
    a = f / (FRAMES - 1)
    c = centre_of(chunk)

    def to_local(p):
        q = rotate_x(rotate_y(p - c, roll * a), pitch * a)
        return q + c
    return to_local


def chunk_model(index, f):
    chunk = CHUNKS[index]
    scene, mats = lev.unit_model(0.0, parts_on=True, wrecked=True)
    to_local = turn(chunk, f)
    pitch, roll = chunk[3]
    a = f / (FRAMES - 1)

    def wrap(fn):
        if fn is None:
            return None
        return lambda p, n, fn=fn: fn(to_local(p), rotate_x(rotate_y(n, roll * a), pitch * a))
    mats = [replace(m, pattern=wrap(m.pattern), emission_pattern=wrap(m.emission_pattern)) for m in mats]

    def model(p):
        q = to_local(p)
        d, m = scene(q)
        g = outside(chunk, q)
        d = np.maximum(d, g)
        ragged = g + 0.012 * np.sin(q[:, 0] * 41 + q[:, 2] * 7) * np.sin(q[:, 1] * 37)
        m = np.where(ragged > -CHAR_BAND * 0.4, lev.CHAR, m)
        m = np.where(ragged > -GLOW_BAND * 0.4, V_GLOW, m)
        return d, m
    return model, mats


def job(args):
    index, f = args
    model, mats = chunk_model(index, f)
    hi = lev.render(model, mats * (len(lev.PARTS) + 1), lev.ROT_DOWN, CANVAS, lev.S)
    return artkit.native(hi, lev.FACTOR)


def crop_even(img):
    """The sprite cropped to an even-sized box round its pixels, and its centre's offset from the
    unit centre (px, dx right, dy up)."""
    a = np.array(img)[..., 3]
    ys, xs = np.nonzero(a)
    l, t, r, b = xs.min(), ys.min(), xs.max() + 1, ys.max() + 1
    r += (r - l) % 2
    b += (b - t) % 2
    cx, cy = CANVAS[0] // 2, CANVAS[1] // 2
    return img.crop((l, t, r, b)), [int((l + r) // 2 - cx), int(cy - (t + b) // 2)]


def build():
    jobs = [(i, f) for i in range(len(CHUNKS)) for f in range(FRAMES)]
    with ProcessPoolExecutor(max_workers=15) as pool:
        frames = list(pool.map(job, jobs))
    mapped = lev.quantize_accented(frames, lev.COLOURS)
    offsets = {}
    for i, chunk in enumerate(CHUNKS):
        cut = [crop_even(mapped[i * FRAMES + f]) for f in range(FRAMES)]
        artkit.write_frames(chunk[0], [c[0] for c in cut], SOURCE)
        offsets[chunk[0]] = [c[1] for c in cut]
    write_death(offsets)


# --------------------------------------------------------------------------- the death's data

def chunk_at(chunk, offset, age):
    """A chunk's centre (dx, dy) and the wreck's scale at ``age`` steps after the death."""
    t = (age - SWAP) / (END - SWAP)
    s = 1 - SINK * np.clip(t, 0, 1)
    e = ease(t)
    return (s * (offset[0] + chunk[4][0] * e), s * (offset[1] + chunk[4][1] * e)), s


def write_death(offsets):
    blasts = [{"at": at, "sprite": name, "dx": dx, "dy": dy} for at, name, dx, dy in CLUSTER]
    for at, name, k, (nx, ny) in TRAILING:
        (cx, cy), _ = chunk_at(CHUNKS[k], offsets[CHUNKS[k][0]][0], at)
        blasts.append({"at": at, "sprite": name, "dx": int(round(cx + nx)), "dy": int(round(cy + ny))})
    blasts.sort(key=lambda b: b["at"])
    path = artkit.PIVOTS / "leviathan.json"
    data = json.loads(path.read_text(encoding="utf-8"))
    data["death"] = {
        "source": SOURCE,
        "swap": SWAP, "end": END, "frame_steps": FRAME_TICKS,
        "sink": SINK, "darken": DARKEN, "fade_from": FADE_FROM,
        "chunks": [{"sprite": c[0], "offsets": offsets[c[0]], "drift": list(c[4])} for c in CHUNKS],
        "blast_frame_steps": FRAME_STEPS,
        "blasts": blasts,
    }
    path.write_text(json.dumps(data, indent=1) + "\n", encoding="utf-8")


# --------------------------------------------------------------------------- review material

def death_data():
    return json.loads((artkit.PIVOTS / "leviathan.json").read_text(encoding="utf-8"))["death"]


def wrecked_body():
    bodies = artkit.load_frames("leviathan-down")
    wrecked = lev.load_parts("wrecked")
    return bodies, wrecked


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


def blast_layout(base, death):
    """The blasts over the wrecked body: large orange, medium amber, ichor violet, each with its step."""
    img = base.copy()
    d = ImageDraw.Draw(img)
    colours = {"explosion-large": (255, 120, 40, 255), "explosion-medium": (255, 210, 60, 255),
               "leviathan-ichor": (200, 120, 255, 255)}
    radii = {"explosion-large": 34, "explosion-medium": 22, "leviathan-ichor": 50}
    cx, cy = img.width // 2, img.height // 2
    for b in death["blasts"]:
        x, y = cx + b["dx"], cy - b["dy"]
        r = radii[b["sprite"]]
        col = colours[b["sprite"]] if b["at"] <= death["swap"] + 4 else (120, 255, 140, 255)
        d.ellipse([x - r, y - r, x + r, y + r], outline=col)
        raster.draw_text(img, x - 6, y - 3, str(b["at"]), col)
    for p, part in enumerate(lev.SPEC["part_list"]):
        x, y = cx + part["offset"][0], cy - part["offset"][1]
        d.rectangle([x - 2, y - 2, x + 2, y + 2], outline=(255, 255, 255, 255))
        raster.draw_text(img, x + 4, y + 2, str(p * CHAIN_STEP), (255, 255, 255, 255))
    return img


def review():
    artkit.REVIEW_ROUND = REVIEW_ROUND
    death = death_data()
    chunks = [artkit.load_frames(c["sprite"]) for c in death["chunks"]]
    bodies, wrecked = wrecked_body()
    sprites = {n: artkit.load_frames(n) for n in FRAME_STEPS}
    pad = 70
    size = (lev.W + 2 * pad, lev.H + 2 * pad)
    centre = (size[0] // 2, size[1] // 2)

    def body_cell(k=1):
        cell = Image.new("RGBA", size, artkit.PLATE)
        unit = lev.compose(bodies[k], {n: fr[k] if n == "fluke" else fr[0] for n, fr in wrecked.items()}, k)
        cell.alpha_composite(unit, (pad, pad))
        return cell

    composed = draw_chunks(Image.new("RGBA", size, artkit.PLATE), death, chunks, 0, centre, motion=False)
    stages = [draw_chunks(Image.new("RGBA", size, artkit.PLATE), death, chunks, age, centre)
              for age in (death["swap"], 90, 120, 150)]
    tumble = [f for frames in chunks for f in frames]
    sheet = artkit.review_sheet("LEVIATHAN DEATH - FINAL BREAK-UP", [
        ("THE FIVE CHUNKS AT THEIR OFFSETS, FRAME 0, NO MOTION; BESIDE THE BODY WITH EVERY PART WRECKED",
         [composed, body_cell()], 1, False),
        (f"THE BREAK-UP AT STEPS {death['swap']} (SWAP), 90, 120, 150 OF {death['end']}: DRIFT, SINK "
         f"(TO {1 - death['sink']:.2f}X), DARKEN, FADE FROM {death['fade_from']:.0%}", stages, 1, False),
        (f"EACH CHUNK'S {FRAMES} TUMBLE FRAMES (HEAD, MIDDLE LEFT, MIDDLE RIGHT, BACK, TAIL), "
         f"{death['frame_steps']} STEPS EACH", tumble, 1, False),
        ("THE BLASTS (STEP AFTER THE DEATH): LARGE ORANGE, MEDIUM AMBER, ICHOR VIOLET; AFTER THE SWAP GREEN, "
         "ON THE DRIFTING CHUNKS; WHITE: THE PART CHAIN", [blast_layout(body_cell(), death)], 1, False),
    ], width=2000, batch=BATCH)
    fps = 30
    alive = 20
    parts = lev.SPEC["part_list"]
    gif = []
    for f in range(int(fps * 3.6)):
        step = f * 2
        age = step - alive
        if age < death["swap"]:
            k = [0, 1, 2, 1][(step // 18) % 4]
            cell = Image.new("RGBA", size, artkit.PLATE)
            unit = lev.compose(bodies[k], {n: fr[k] if n == "fluke" else fr[0] for n, fr in wrecked.items()}, k)
            cell.alpha_composite(unit, (pad, pad))
        else:
            cell = draw_chunks(Image.new("RGBA", size, artkit.PLATE), death, chunks, age, centre)
        layers = []
        for p, part in enumerate(parts):
            for name in ("leviathan-ichor", "explosion-medium"):
                layers.append((name, age - p * CHAIN_STEP, part["offset"]))
        for b in death["blasts"]:
            layers.append((b["sprite"], age - b["at"], (b["dx"], b["dy"])))
        for name, a, (dx, dy) in layers:
            fr = timed(sprites[name], death["blast_frame_steps"][name], a)
            if fr is not None:
                x, y = centre[0] + dx, centre[1] - dy
                cell = artkit.add_light(cell, fr, (int(x - fr.width / 2), int(y - fr.height / 2)))
        gif.append(cell)
    artkit.save_review(sheet, gif, lev.UNIT / "concept", "leviathan-death", fps=fps, variant=REVIEW_VARIANT)


if __name__ == "__main__":
    if "--data" in sys.argv[1:]:          # the chunk sprites as they are, the death's data rewritten
        write_death({c["sprite"]: c["offsets"] for c in death_data()["chunks"]})
    elif "--review" not in sys.argv[1:]:
        build()
    review()
