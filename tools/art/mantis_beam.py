#!/usr/bin/env python3
"""Production art: the Mantis's beam and its telegraph, the chosen round-23 variant B "charged lance"
(design/enemies/air/mantis; M4 part F batch).

Outputs (assets/sprites/, every file additive: premultiplied on black, alpha 1 where it adds light,
blended GL_SRC_ALPHA, GL_ONE):
  mantis-beam_0..3       32x10, the beam strip: a 2 px core, energy knots 16 px apart running
                         outwards 4 px a frame (20 fps: 80 px/s), a stepped halo; tileable along x,
                         x = 0 at the eye; the game tiles it along the beam's length and rotates it
                         about the eye
  mantis-beam-tip_0..3   16x16, the impact spark at the beam's end, 4 frames
  mantis-beam-eye        16x16, the eye ring, drawn on the eye through the telegraph and the sweep
  mantis-telegraph-<arc> the telegraph: one filled wedge per arc the data uses (70 at easy and
                         medium, the hard hook's 90), a faint crimson fill rising toward the rim, a
                         bright 2 px rim on the reach arc and 1 px edges; the apex (the eye) at the
                         left edge's middle, pointing right, its radius the sweep's length (300 px:
                         302x349 for 70, 302x429 for 90); the game rotates it to the sweep's centre
                         and pulses it
  design/enemies/air/mantis/concept/mantis-beam-final-r23-a.png/.gif

The parts are the chosen concept's 2D light fields (tools/concept/props_r23.py: beam_b, spark_b,
ring_b, wedge_b, imported unchanged; supersampled 4x, the wedge 2x), one 24-colour palette over the
beam, tip and eye frames, 32 colours for each wedge. The arcs and the length come from the Mantis's
data.yaml. The review plays the production sprites (tools/art/mantis.py) of both edges with the
telegraph and the sweep laid out as the game does (FarsideLooks.drawSweeps), from the eye at the
data's sweep.origin.

Run: python3 tools/art/mantis_beam.py [--review]   (~10 s)
"""
import sys

import numpy as np
import yaml
from PIL import Image

import artkit
from artkit import DESIGN, sprite

import props_r23 as r23  # noqa: E402  (concept script, imported unchanged)
from render import raster  # noqa: E402

SCRIPT = "mantis_beam.py"
BATCH = "M4 part F batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
REVIEW_ROUND = "r23"
UNIT = DESIGN / "enemies" / "air" / "mantis"
SLUG = "mantis"
SPEC = yaml.safe_load((UNIT / "data.yaml").read_text(encoding="utf-8"))
SWEEP = next(a for a in SPEC["attacks"] if a["pattern"] == "laser-sweep")["sweep"]
LENGTH = int(SWEEP["length"])
ORIGIN = SWEEP.get("origin", [0, 0])            # the eye: px toward the field, down
ARCS = sorted({int(SWEEP["arc"])} | {int(h["sweep_arc"]) for h in (SPEC.get("difficulty") or {}).values()
                                       if "sweep_arc" in h})
STRIP_FRAMES = 4
SPARK_FRAMES = 4


def build():
    beam = [artkit.additive(r23.beam_b(i)) for i in range(STRIP_FRAMES)]
    tip = [artkit.additive(r23.spark_b(i)) for i in range(SPARK_FRAMES)]
    eye = [artkit.additive(r23.ring_b())]
    parts = artkit.quantize_set(beam + tip + eye, 24)
    artkit.write_frames(f"{SLUG}-beam", parts[:STRIP_FRAMES], SOURCE)
    artkit.write_frames(f"{SLUG}-beam-tip", parts[STRIP_FRAMES:STRIP_FRAMES + SPARK_FRAMES], SOURCE)
    artkit.write_frames(f"{SLUG}-beam-eye", parts[-1:], SOURCE, single=True)
    for arc in ARCS:
        wedge = artkit.quantize_set([artkit.additive(r23.wedge_b(arc, LENGTH))], 32)
        artkit.write_frames(f"{SLUG}-telegraph-{arc}", wedge, SOURCE, single=True)


# --------------------------------------------------------------------------- review

def rotated(img, angle_img, origin):
    """``img`` rotated about ``origin`` (its px) to the image-space angle (radians, x right, y
    down) on a black canvas; returns (rgb array, the origin's position in it)."""
    r = int(np.ceil(max(np.hypot(img.width - origin[0], img.height - origin[1]), np.hypot(*origin)))) + 2
    canvas = Image.new("RGB", (2 * r, 2 * r), (0, 0, 0))
    canvas.paste(img.convert("RGB"), (r - origin[0], r - origin[1]))
    return np.array(canvas.rotate(np.degrees(-angle_img), resample=Image.BILINEAR, center=(r, r))), r


def lay_beam(strip, length):
    """The strip tiled to ``length`` px (the last tile cut), as the game draws it."""
    out = Image.new("RGB", (length, strip.height), (0, 0, 0))
    for x in range(0, length, strip.width):
        out.paste(strip.convert("RGB").crop((0, 0, min(strip.width, length - x), strip.height)), (x, 0))
    return out


def add(base, rgb, x, y, gain=1.0):
    h, w = rgb.shape[:2]
    H, W = base.shape[:2]
    x0, y0, x1, y1 = max(0, x), max(0, y), min(W, x + w), min(H, y + h)
    if x1 > x0 and y1 > y0:
        base[y0:y1, x0:x1] = np.clip(base[y0:y1, x0:x1] + rgb[y0 - y:y1 - y, x0 - x:x1 - x] * gain, 0, 255)


def add_centred(base, img, x, y):
    add(base, np.array(img.convert("RGB")).astype(np.float64), int(round(x - img.width / 2)), int(round(y - img.height / 2)))


def review():
    artkit.REVIEW_ROUND = REVIEW_ROUND
    beam = artkit.load_frames(f"{SLUG}-beam")
    tip = artkit.load_frames(f"{SLUG}-beam-tip")
    eye = artkit.load_frames(f"{SLUG}-beam-eye")
    wedges = {arc: artkit.load_frames(f"{SLUG}-telegraph-{arc}")[0] for arc in ARCS}
    unit = artkit.load_frames(SLUG)
    per_side = len(unit) // 2
    rows = [("BEAM STRIP (TILES ALONG THE BEAM, X = 0 AT THE EYE)", beam, 4, True),
            ("TIP SPARK", tip, 4, True), ("EYE RING", eye, 4, True)]
    rows += [(f"TELEGRAPH {arc} DEG (APEX AT THE EYE, ROTATED TO THE SWEEP'S CENTRE)", [w], 1, True)
             for arc, w in wedges.items()]
    sheet = artkit.review_sheet("MANTIS BEAM - FINAL (ROUND 23 VARIANT B)", rows, width=1000, batch=BATCH)

    # GIF: a pincer at the data's 70 deg, both edges, 2x of a 480x420 field strip: telegraph 0.6 s,
    # sweep 1.2 s, the beam, wedge, tip and ring from the eye at the data's origin.
    W, H, my = 480, 420, 110
    ship = (300, 380)
    ship_img = artkit.load_frames("ship")[2]
    gif = []
    for i in range(int(2.4 * 20)):
        t = i / 20
        base = np.zeros((H, W, 3)) + np.array([14, 16, 24])
        frame_img = Image.fromarray(base.astype(np.uint8)).convert("RGBA")
        for side in (0, 1):
            mx = 40 if side == 0 else W - 40
            k = 4 + min(3, int(t / 0.15)) if t < 0.6 else 8 + i % 2
            sprite.paste_center(frame_img, unit[side * per_side + k], mx, my)
        sprite.paste_center(frame_img, ship_img, *ship)
        base = np.array(frame_img.convert("RGB")).astype(np.float64)
        for side in (0, 1):
            sign = 1 if side == 0 else -1
            mx = 40 if side == 0 else W - 40
            ex, ey = mx + sign * ORIGIN[0], my + ORIGIN[1]
            # the sweep's centre on the ship's bearing from the eye, clamped inward..down (game heading:
            # clockwise from straight down, y up)
            bearing = np.arctan2(-(ship[0] - ex), (ship[1] - ey))
            centre = np.clip(bearing, -np.pi / 2, 0) if side == 0 else np.clip(bearing, 0, np.pi / 2)
            half = np.radians(SWEEP["arc"]) / 2
            h_from, h_to = (centre - half, centre + half) if side == 0 else (centre + half, centre - half)
            if t < 0.6:
                pulse = 0.45 + 0.35 * abs(np.sin(i * 3 * 0.4))
                w = wedges[int(SWEEP["arc"])]
                rgb, c = rotated(w, r23.heading_to_img(centre), (0, w.height // 2))
                add(base, rgb.astype(np.float64), int(round(ex - c)), int(round(ey - c)), 0.6 + 0.5 * pulse)
            else:
                s = min(1.0, (t - 0.6) / 1.2)
                h = h_from + (h_to - h_from) * s
                a = r23.heading_to_img(h)
                strip = lay_beam(beam[i % len(beam)], LENGTH)
                rgb, c = rotated(strip, a, (0, strip.height // 2))
                add(base, rgb.astype(np.float64), int(round(ex - c)), int(round(ey - c)))
                add_centred(base, tip[i % len(tip)], ex + np.cos(a) * LENGTH, ey + np.sin(a) * LENGTH)
            add_centred(base, eye[0], ex, ey)
        gif.append(sprite.enlarge(Image.fromarray(base.astype(np.uint8)).convert("RGBA"), 2))
    # the sheet also shows two GIF frames at 1x: the telegraph and the sweep half-way
    shots = [gif[8], gif[30]]
    out = raster.sheet(sheet.width, sheet.height + 24 + H, "MANTIS BEAM - FINAL (ROUND 23 VARIANT B)",
                       f"PRODUCTION ART, {BATCH.upper()} - {REVIEW_ROUND.upper()}")
    out.alpha_composite(sheet, (0, 0))
    raster.draw_text(out, 16, sheet.height + 4, f"IN PLAY (1X): TELEGRAPH, SWEEP; FROM THE EYE AT {ORIGIN[0]} PX IN, "
                                                f"{ORIGIN[1]} PX DOWN (DATA: SWEEP.ORIGIN), BOTH EDGES", raster.LABEL)
    for j, g in enumerate(shots):
        out.alpha_composite(g.resize((W, H), Image.NEAREST), (16 + j * (W + 8), sheet.height + 18))
    artkit.save_review(out, gif, UNIT / "concept", f"{SLUG}-beam", fps=20)


if __name__ == "__main__":
    if "--review" not in sys.argv[1:]:
        build()
    review()
