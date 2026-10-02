#!/usr/bin/env python3
"""Production art: the AF-12 Stormhawk (design/player/ship).

Outputs (assets/, sizes from design/player/ship/data.yaml):
  sprites/ship_0..4.png                  48x48 banking frames: hard left -30, left -15, level,
                                         right +15, hard right +30 degrees of roll, seen through a
                                         mild perspective camera so the raised wing grows and the
                                         lowered one shrinks; 32 colours
  sprites/pod-<type>-<left|right>_0..4   the fitted wing pods per banking frame, cut from a render
                                         of the ship with the pods (so the pod's shadow on the wing
                                         is in it); drawn over the hull at the offset in pods.json
  sprites/engine-flame_0..8.png          12x18 additive flame, 3-frame loop per length: cruise
                                         (0-2), at speed (3-5), moving back (6-8)
  pivots/ship.json                       per banking frame the mount points (front, rear, wings,
                                         engines) of data.yaml, rolled with the hull
  pivots/pods.json                       per pod sprite and banking frame its top-left offset on the
                                         hull; the flame's attach point
  design/player/ship/concept/player-ship-final-r12-a.png/.gif   review sheet and loop

The model and pods are the chosen round-08 ones (tools/concept/vfx_r08.py, render/models.py);
the pods get lighter metal materials than the concept's navy ones so their shading shows at 48 px.
The shadow is not an asset: the game draws it from the hull's alpha (art direction, step 6).

Run: python3 tools/art/stormhawk.py [--review]   (~1 min; --review only rebuilds the sheet)
"""
import json
import sys
from concurrent.futures import ProcessPoolExecutor

import numpy as np
import yaml
from PIL import Image

import artkit
from artkit import DESIGN, sprite

import vfx_r08 as v8  # noqa: E402  (concept script, imported unchanged)
from render.palette import B  # noqa: E402
from render.sdf import rotate_y  # noqa: E402

SCRIPT = "stormhawk.py"
SOURCE = artkit.source_note(SCRIPT)
DATA = yaml.safe_load((DESIGN / "player" / "ship" / "data.yaml").read_text(encoding="utf-8"))
SIZE = DATA["size"]
EXTENT = 2.3                      # model units across the sprite, as in the chosen concept
BANKS = [-30, -15, 0, 15, 30]     # degrees of roll; art direction: about +-14 and +-28
CAMERA = 4.0                      # perspective camera height (model units) that makes the roll read
PODS = v8.PODS
H, ACC = v8.HULL_C, v8.ACC_C
# rails and barrels, pod body, missile heads, accent band (v8.POD_MATS slots)
POD_MATS = [v8.m_metal(H[1]), v8.mat(H[3], metal=0.5, shininess=40, spec=0.9), v8.m_metal(H[4]),
            v8.mat(ACC[1], metal=0.3, shininess=40, spec=0.5)]
SHIP_COLOURS = 32
POD_COLOURS = 24
FLAME = (12, 18)
FLAME_ATTACH = (6, 2)             # the flame's top centre sits on the engine mount
FLAME_LENGTHS = {"cruise": 1.0, "speed": 1.4, "back": 0.6}
WING_Z = v8.POD_AT[2]             # the pods sit on top of the wing
PS = B["PLAYER SHOTS"]


def render(bank, pod):
    scene, mats = v8.ship_model(np.radians(bank), pod, B.ship_colors())
    if pod:
        mats = mats[:len(mats) - len(v8.POD_MATS)] + POD_MATS
    return artkit.render_hi(artkit.perspective(scene, CAMERA), mats, (SIZE, SIZE), EXTENT)


def render_all():
    jobs = [(b, None) for b in BANKS] + [(b, k) for k in PODS for b in BANKS]
    with ProcessPoolExecutor() as pool:
        return dict(zip(jobs, pool.map(render, *zip(*jobs))))


def pod_overlay(hi_pod, hi_ship, factor, pod_native):
    """The pixels where the render with pods differs from the bare hull, split into the left and
    right pod with their top-left offsets."""
    diff = (np.abs(hi_pod - hi_ship).max(axis=-1) > 0.03).astype(np.float64)
    mask = diff.reshape(SIZE, factor, SIZE, factor).mean(axis=(1, 3)) >= 0.25
    arr = np.array(pod_native)
    mask &= arr[..., 3] > 0
    out = {}
    for side, cols in (("left", slice(0, SIZE // 2)), ("right", slice(SIZE // 2, SIZE))):
        m = np.zeros_like(mask)
        m[:, cols] = mask[:, cols]
        ys, xs = np.nonzero(m)
        x0, y0, x1, y1 = xs.min(), ys.min(), xs.max() + 1, ys.max() + 1
        piece = arr[y0:y1, x0:x1].copy()
        piece[~m[y0:y1, x0:x1]] = 0
        out[side] = (Image.fromarray(piece, "RGBA"), (int(x0), int(y0)))
    return out


def mount_points():
    """data.yaml's mount points (level frame) rolled with each banking frame and projected
    through the perspective camera."""
    scale = SIZE / EXTENT
    names = {"front": [DATA["mounts"]["front"]], "rear": [DATA["mounts"]["rear"]]}
    names["wing-left"], names["wing-right"] = ([w] for w in DATA["mounts"]["wings"])
    names["engine-left"], names["engine-right"] = ([e] for e in DATA["mounts"]["engines"])
    points = {}
    for name, ((px, py),) in names.items():
        z = WING_Z if name.startswith("wing") else 0.0
        model = np.array([[(px - SIZE / 2) / scale, (SIZE / 2 - py) / scale, z]])
        points[name] = []
        for bank in BANKS:
            x, y, z = rotate_y(model, -np.radians(bank))[0]
            k = scale * CAMERA / (CAMERA - z)
            points[name].append([int(round(SIZE / 2 + x * k)), int(round(SIZE / 2 - y * k))])
    return points


def flame_frames():
    """Blue-white engine flame: hot core at the nozzle, flickering tail (3 frames per length)."""
    out = []
    rng = np.random.default_rng(12)
    flicker = rng.uniform(0.85, 1.12, (len(FLAME_LENGTHS), 3))
    for li, length in enumerate(FLAME_LENGTHS.values()):
        for f in range(3):
            cv = v8.Canvas(*FLAME)
            ax, ay = FLAME_ATTACH
            tail = ay + (FLAME[1] - ay - 2) * length / 1.4 * flicker[li, f]
            d = cv.seg(ax, ay, ax, tail)
            along = np.clip((cv.y - ay) / max(tail - ay, 1), 0, 1)
            width = 2.6 * (1 - 0.7 * along) * (0.92 + 0.08 * flicker[li, f])
            cv.add(PS[1], v8.gauss(d, width * 1.8) * 0.9 * (1 - along) ** 0.6)
            cv.add(PS[2], v8.solid(d, width) * (1 - along ** 1.5))
            cv.add(PS[3], v8.solid(d, width * 0.5) * (1 - along) ** 1.2)
            cv.add(PS[4], v8.solid(cv.dist(ax, ay + 1), 1.4))
            out.append(artkit.additive(cv.image()))
    return artkit.quantize_set(out, 24)


def build():
    renders = render_all()
    factor = renders[(0, None)][1]
    hulls = [artkit.native(renders[(b, None)][0], factor) for b in BANKS]
    hulls = artkit.quantize_set(hulls, SHIP_COLOURS)
    artkit.write_frames("ship", hulls, SOURCE)
    offsets = {}
    for pod in PODS:
        pieces = {"left": [], "right": []}
        for bank in BANKS:
            hi_pod, hi_ship = renders[(bank, pod)][0], renders[(bank, None)][0]
            cut = pod_overlay(hi_pod, hi_ship, factor, artkit.native(hi_pod, factor))
            for side, piece in cut.items():
                pieces[side].append(piece)
        frames = artkit.quantize_set([p[0] for side in ("left", "right") for p in pieces[side]], POD_COLOURS)
        for i, side in enumerate(("left", "right")):
            name = f"pod-{pod}-{side}"
            artkit.write_frames(name, frames[i * len(BANKS):(i + 1) * len(BANKS)], SOURCE)
            offsets[name] = [list(p[1]) for p in pieces[side]]
    artkit.write_frames("engine-flame", flame_frames(), SOURCE)
    frames = ["hard left", "left", "level", "right", "hard right"]
    artkit.write_pivots("ship", SOURCE, {"frames": frames, "points": mount_points()})
    artkit.write_pivots("pods", SOURCE, {
        "frames": frames, "offsets": offsets,
        "engine-flame": {"attach": list(FLAME_ATTACH),
                         "frames": {k: [3 * i, 3 * i + 1, 3 * i + 2] for i, k in enumerate(FLAME_LENGTHS)}}})


# --------------------------------------------------------------------------- review

def composite(bank, pod=None, flame=None):
    """The hull of one banking frame with pods and flames drawn the way the game will."""
    hull = artkit.load_frames("ship")[bank]
    img = Image.new("RGBA", (SIZE, SIZE + 12), (0, 0, 0, 0))
    pivots = json.loads((artkit.PIVOTS / "ship.json").read_text(encoding="utf-8"))
    pods = json.loads((artkit.PIVOTS / "pods.json").read_text(encoding="utf-8"))
    if flame is not None:
        f = artkit.load_frames("engine-flame")[flame]
        ax, ay = pods["engine-flame"]["attach"]
        glow = Image.new("RGBA", img.size, (0, 0, 0, 0))
        for side in ("engine-left", "engine-right"):
            x, y = pivots["points"][side][bank]
            glow.alpha_composite(f, (x - ax, y - ay))
        img = glow
    img.alpha_composite(hull)
    if pod:
        for side in ("left", "right"):
            name = f"pod-{pod}-{side}"
            img.alpha_composite(artkit.load_frames(name)[bank], tuple(pods["offsets"][name][bank]))
    return img


def review():
    ship = artkit.load_frames("ship")
    rows = [("BANKING FRAMES HARD LEFT .. HARD RIGHT", ship, 4, False)]
    for pod in PODS:
        rows.append((f"{v8.POD_NAMES[pod]} ON EVERY BANK FRAME (HULL + POD SPRITES AT THEIR PIVOTS)",
                     [composite(b, pod) for b in range(len(BANKS))], 3, False))
    rows.append(("ENGINE FLAME: CRUISE / AT SPEED / MOVING BACK, 3-FRAME LOOPS (ADDITIVE)",
                 artkit.load_frames("engine-flame"), 4, True))
    sheet = artkit.review_sheet("AF-12 STORMHAWK - FINAL SPRITES", rows)
    seq = [2, 1, 0, 0, 0, 1, 2, 3, 4, 4, 4, 3]
    gif = []
    for i in range(48):
        bank = seq[(i // 2) % len(seq)]
        pod = PODS[i // 12 % len(PODS)] if i >= 12 else None
        frame = composite(bank, pod, flame=3 * (i // 16 % 3) + i % 3)
        cell = Image.new("RGBA", frame.size, artkit.PLATE)
        cell.alpha_composite(frame)
        gif.append(sprite.enlarge(cell, 4))
    artkit.save_review(sheet, gif, DESIGN / "player" / "ship" / "concept", "player-ship", fps=12)


if __name__ == "__main__":
    if "--review" not in sys.argv[1:]:
        build()
    review()
