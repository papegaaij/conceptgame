#!/usr/bin/env python3
"""Production art: Rook's craft, his engine flame and his eject pod (design/player/wingmen, In the
game; M5 part A batch, concept round 28).

Outputs (assets/, the names WingmanLooks loads):
  sprites/rook_0..4.png        42x42 banking frames: hard left -30, left -15, level, right +15,
                               hard right +30 degrees of roll (the Stormhawk's angles, so the pair
                               banks as one), seen through the Stormhawk's mild perspective camera;
                               the chosen Ember scheme on the ship-C airframe of round 09
  sprites/rook-pod_0..3.png    16x16 eject pod, normal blending, a 32-colour palette: a
                               slate capsule with dark end caps, a yellow nose band, a canopy,
                               tumbling slowly (0, 45, 90, 135 degrees clockwise; the loop's last
                               frame leads back to the first as the capsule's end-to-end look
                               repeats after 180) with its red beacon lit in frame 0 only,
                               so the loop blinks once (WingmanLooks shows a frame 8 steps)
  sprites/rook-flame_0..2.png  8x14 additive warm engine flame, a 3-frame flicker loop, drawn under
                               the hull with its top centre (4, 0) on each engine mount
  pivots/rook.json             per banking frame the engine mounts (`points.engine-left`,
                               `points.engine-right`, [x, y] px from the sprite's top left, y down,
                               as pivots/ship.json): the nozzles' rear ends, rolled with the hull
                               and projected through the camera
  design/player/wingmen/concept/rook-final-r28-a.png/.gif   review sheet and loop

The model is the chosen round-09 one (vfx_r09.rook_sheet: vfx_r08.rook_sprite, i.e.
render/models.ship_c_model in ships_r02's Ember colours), imported unchanged and rendered at the
quality bar's 8x through the Stormhawk's camera (tools/art/stormhawk.py) at 42 px instead of the
concept's 40. Each sprite has one palette over all its frames (hull and pod 32 colours each, the
additive flame 16, premultiplied on black), as the Stormhawk's. The shadow is not an asset: the game draws it from the
hull's alpha.

Run: python3 tools/art/rook.py [--review]   (~5 s; --review only rebuilds the sheet)
"""
import json
import sys
from concurrent.futures import ProcessPoolExecutor

import numpy as np
from PIL import Image

import artkit
from artkit import DESIGN, sprite

import ships_r02  # noqa: E402  (concept script, imported unchanged: the Ember scheme)
import vfx_r08 as v8  # noqa: E402  (concept script, imported unchanged)
from render import models  # noqa: E402
from render.palette import B  # noqa: E402
from render.sdf import (Material, rotate_y, rotate_z, sd_capsule, sd_ellipsoid,  # noqa: E402
                        sd_sphere, union)

SCRIPT = "rook.py"
BATCH = "M5 part A batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
REVIEW_ROUND = "r28"
WING_DIR = DESIGN / "player" / "wingmen" / "concept"

SIZE = 42                         # design/player/wingmen: 42x42, smaller than the player's 48
EXTENT = 2.3                      # model units across the sprite, as in the chosen concept
BANKS = [-30, -15, 0, 15, 30]     # the Stormhawk's (tools/art/stormhawk.py)
CAMERA = 4.0                      # the Stormhawk's perspective camera
EMBER = ships_r02.ROOK_SCHEMES["a"][1]
ENGINE = (0.15, -0.735, 0.0)      # ship_c_model's engine (x mirrored): the nozzle's rear end
HULL_COLOURS = 32
POD_COLOURS = 32

POD = 16
POD_TURNS = [0, 45, 90, 135]      # degrees clockwise on the screen
POD_EXTENT = POD * EXTENT / SIZE  # the craft's scale: model units per px kept
POD_LEN = 0.30                    # half the capsule's axis, model units (about 13 px end to end)
POD_R = 0.135
BEACON_ON = (1.0, 0.16, 0.08)     # a red rescue beacon, apart from the yellow band

FLAME = (8, 14)
FLAME_ATTACH = (4, 0)             # the flame's top centre sits on the engine mount
FLAME_FRAMES = 3
MARS = B["MARS"]
ACC = B["UTC ACCENTS"]


def render_bank(bank):
    scene, mats = models.ship_c_model(bank=np.radians(bank), palette=EMBER)
    return artkit.render_hi(artkit.perspective(scene, CAMERA), mats, (SIZE, SIZE), EXTENT)


def pod_model(turn, beacon):
    """The eject capsule lying flat, nose up the screen before the turn: slate body with dark end
    caps, a yellow band at the nose (the craft's nose band), the dark canopy over the seat and the beacon on the spine
    behind it (lit red, or a dark lens)."""
    hull = Material(EMBER["hull"], metal=0.35, shininess=60, spec=0.55)
    dark = Material(EMBER["hull_dark"], metal=0.5, shininess=30, spec=0.35)
    band = Material(EMBER["accent2"], metal=0.3, shininess=50, spec=0.5)
    glass = Material(EMBER["glass"], metal=0.85, shininess=120, spec=1.2)
    lamp = (Material((0.1, 0.03, 0.02), emission=tuple(np.array(BEACON_ON) * 1.15)) if beacon
            else Material((0.16, 0.05, 0.05), metal=0.1, shininess=20, spec=0.15))

    def scene(p):
        q = rotate_z(p, np.radians(-turn))
        d, m = union(
            (sd_capsule(q, (0, -POD_LEN, 0), (0, POD_LEN, 0), POD_R), 0),
            (sd_ellipsoid(q, (0, 0.02, 0.07), (0.08, 0.14, 0.1)), 3),
            k=0.02)
        y = q[:, 1]
        m = np.where((m == 0) & (np.abs(y - 0.24) < 0.04), 2, m)
        m = np.where((m == 0) & (np.abs(y) > POD_LEN + 0.04), 1, m)
        lens = sd_sphere(q, (0, -0.21, POD_R - 0.02), 0.075)
        m = np.where(lens < d, 4, m)
        return np.minimum(d, lens), m

    return scene, [hull, dark, band, glass, lamp]


def render_pod(turn, beacon):
    scene, mats = pod_model(turn, beacon)
    return artkit.render_hi(scene, mats, (POD, POD), POD_EXTENT)


def engine_mounts():
    """The nozzles' rear ends rolled with each banking frame and projected through the camera
    (as stormhawk.mount_points)."""
    scale = SIZE / EXTENT
    points = {"engine-left": [], "engine-right": []}
    for name, sx in (("engine-left", -1), ("engine-right", 1)):
        model = np.array([[sx * ENGINE[0], ENGINE[1], ENGINE[2]]])
        for bank in BANKS:
            x, y, z = rotate_y(model, -np.radians(bank))[0]
            k = scale * CAMERA / (CAMERA - z)
            points[name].append([int(round(SIZE / 2 + x * k)), int(round(SIZE / 2 - y * k))])
    return points


def flame_frames():
    """Warm engine flame (the Ember scheme's engine glow): pale yellow core at the nozzle, orange
    body, red-brown tail, flickering in length over 3 frames."""
    rng = np.random.default_rng(28)
    flicker = rng.uniform(0.82, 1.12, FLAME_FRAMES)
    out = []
    for f in range(FLAME_FRAMES):
        cv = v8.Canvas(*FLAME)
        ax, ay = FLAME[0] / 2, 1.0
        tail = ay + (FLAME[1] - ay - 2.5) * flicker[f] / 1.12
        d = cv.seg(ax, ay, ax, tail)
        along = np.clip((cv.y - ay) / max(tail - ay, 1), 0, 1)
        width = 1.7 * (1 - 0.65 * along) * (0.92 + 0.08 * flicker[f])
        cv.add(MARS[2], v8.gauss(d, width * 1.8) * 0.8 * (1 - along) ** 0.6)
        cv.add(MARS[4], v8.solid(d, width) * (1 - along ** 1.5))
        cv.add(MARS[5], v8.solid(d, width * 0.5) * (1 - along) ** 1.2)
        cv.add(ACC[5], v8.solid(cv.dist(ax, ay + 0.5), 1.1))
        out.append(artkit.additive(cv.image()))
    return artkit.quantize_set(out, 16)


def build():
    jobs = [("bank", b, False) for b in BANKS] + [("pod", t, i == 0) for i, t in enumerate(POD_TURNS)]
    with ProcessPoolExecutor() as pool:
        renders = list(pool.map(_render_job, jobs))
    frames = [artkit.native(hi, factor) for hi, factor in renders]
    artkit.write_frames("rook", artkit.quantize_set(frames[:len(BANKS)], HULL_COLOURS), SOURCE)
    artkit.write_frames("rook-pod", pod_palette(frames[len(BANKS):]), SOURCE)
    artkit.write_frames("rook-flame", flame_frames(), SOURCE)
    artkit.write_pivots("rook", SOURCE, {
        "frames": ["hard left", "left", "level", "right", "hard right"],
        "points": engine_mounts(),
        "rook-flame": {"attach": list(FLAME_ATTACH)}})


def pod_palette(frames):
    """The pod's frames on one palette. The lit beacon is a handful of pixels in one frame, which
    the median cut would merge into the hull's warm shades, so its pixels are weighted up (repeated in a
    helper frame that is quantised with the set and then dropped)."""
    lit = np.array(frames[0].convert("RGBA"))
    red = lit[(lit[..., 3] > 0) & (lit[..., 0] > 180) & (lit[..., 1] < 80)]
    helper = np.resize(red, (2 * POD, 4)).reshape(2, POD, 4)
    return artkit.quantize_set(list(frames) + [Image.fromarray(helper, "RGBA")], POD_COLOURS)[:-1]


def _render_job(job):
    kind, value, beacon = job
    return render_bank(value) if kind == "bank" else render_pod(value, beacon)


# --------------------------------------------------------------------------- review

def draw_rook(img, cx, cy, bank, flame):
    """The hull centred on (cx, cy) with its two flames added under it, as WingmanLooks draws it."""
    hull = artkit.load_frames("rook")[bank]
    flames = artkit.load_frames("rook-flame")
    pivots = json.loads((artkit.PIVOTS / "rook.json").read_text(encoding="utf-8"))
    ax, ay = pivots["rook-flame"]["attach"]
    left, top = cx - SIZE // 2, cy - SIZE // 2
    for i, side in enumerate(("engine-left", "engine-right")):
        x, y = pivots["points"][side][bank]
        img = artkit.add_light(img, flames[(flame + i) % len(flames)], (left + x - ax, top + y - ay))
    img.alpha_composite(hull, (left, top))
    return img


def draw_ship(img, cx, cy, bank, flame):
    """The player's Stormhawk with its cruise flames (tools/art/ship_fx.draw_ship's way)."""
    hull = artkit.load_frames("ship")[bank]
    flames = artkit.load_frames("engine-flame")
    pivots = json.loads((artkit.PIVOTS / "ship.json").read_text(encoding="utf-8"))
    pods = json.loads((artkit.PIVOTS / "pods.json").read_text(encoding="utf-8"))
    ax, ay = pods["engine-flame"]["attach"]
    left, top = cx - hull.width // 2, cy - hull.height // 2
    for side in ("engine-left", "engine-right"):
        x, y = pivots["points"][side][bank]
        img = artkit.add_light(img, flames[flame % 3], (left + x - ax, top + y - ay))
    img.alpha_composite(hull, (left, top))
    return img


def with_flames(bank, flame=0):
    cell = Image.new("RGBA", (SIZE, SIZE + 10), artkit.PLATE)
    return draw_rook(cell, SIZE // 2, SIZE // 2, bank, flame)


def pair_strip():
    """1x: Rook beside the Stormhawk at the five banks, on the plate."""
    img = Image.new("RGBA", (5 * 100, 64), artkit.PLATE)
    for b in range(5):
        img = draw_rook(img, 22 + b * 100, 30, b, b)
        img = draw_ship(img, 70 + b * 100, 26, b, b)
    return img


def scene(i, pod):
    """A 160x120 plate: the Stormhawk and Rook in the Wing formation banking left and right, then
    Rook's pod drifting off to the side edge (frames 48 on)."""
    img = Image.new("RGBA", (160, 120), (24, 30, 58, 255))
    seq = [2, 1, 0, 0, 0, 1, 2, 3, 4, 4, 4, 3]
    bank = seq[(i // 2) % len(seq)]
    sway = [0, -2, -5, -7, -8, -6, -3, 0, 3, 6, 8, 7][(i // 2) % 12] if i < 48 else 0
    img = draw_ship(img, 80 + sway, 50, bank, i)
    if i < 48:
        img = draw_rook(img, 46 + sway, 78, bank, i)
    else:
        t = i - 48
        frame = pod[(t * 3 // 8) % len(pod)]          # 8 steps a frame at 60 steps/s, 20 fps here
        sprite.paste_center(img, frame, 46 - t * 2.0, 78)
    return img


def review():
    artkit.REVIEW_ROUND = REVIEW_ROUND
    hull = artkit.load_frames("rook")
    pod = artkit.load_frames("rook-pod")
    flame = artkit.load_frames("rook-flame")
    rows = [
        ("BANKING FRAMES HARD LEFT .. HARD RIGHT (-30 -15 0 +15 +30 DEG)", hull, 5, False),
        ("WITH BOTH ENGINE FLAMES AT THE PIVOTS, AS THE GAME DRAWS THEM", [with_flames(b, b) for b in range(5)], 4, False),
        ("ENGINE FLAME, 3-FRAME LOOP (ADDITIVE)", flame, 8, True),
        ("EJECT POD: SLOW TUMBLE, BEACON IN FRAME 0 (NORMAL BLENDING)", pod, 8, False),
        ("1X: ROOK BESIDE THE STORMHAWK, SAME 5 BANKS", [pair_strip()], 1, False),
        ("GREYSCALE CHECK", [pair_strip().convert("L").convert("RGBA")], 1, False),
        ("1X FRAMES", hull + pod, 1, False),
    ]
    sheet = artkit.review_sheet("ROOK'S CRAFT (EMBER) - FINAL SPRITES", rows, batch=BATCH)
    gif = [sprite.enlarge(scene(i, pod), 3) for i in range(80)]
    artkit.save_review(sheet, gif, WING_DIR, "rook", fps=20)


if __name__ == "__main__":
    if "--review" not in sys.argv[1:]:
        build()
    review()
