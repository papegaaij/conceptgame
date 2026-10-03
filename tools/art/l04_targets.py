#!/usr/bin/env python3
"""Production art: Level 04's two loot targets on the ground layer, the variants chosen in concept
round 17 (design/campaign/act-1-first-contact/level-04-tranquility-run, "Ground targets"; art
direction: readability rule 7, a damaged and a wrecked frame for ground structures).

Outputs (assets/sprites/, sizes from the `ground_targets` of Level 04's data.yaml):
  dugout_0..2.png            48x32 the prospector's dugout (variant a: a corrugated hut half-buried
                             in a berm, hazard hatch and roof band): intact, damaged, wrecked
  dugout-break_0..7.png      72x72 the damaged dugout in six pieces that fly apart, tumble and
                             char, then crumble away over the last three frames (1-bit alpha)
  supply-drop_0..2.png       32x24 the CDF supply drop (variant a: a steel drop crate, hazard end
                             bands, retro nozzles): intact, damaged, wrecked
  supply-drop-break_0..7.png 48x48 its break-apart, as the dugout's
  design/campaign/act-1-first-contact/level-04-tranquility-run/concept/l04-targets-final-r17-b.png/.gif

The game draws frame 0 or 1 while the target stands, plays the break-apart where it is destroyed
and leaves the wrecked frame there (as the cargo container of Level 01, tools/art/loot_targets.py,
whose break-apart method this reuses). The models and materials are the concept round's
(tools/concept/ground_targets_r17.py, imported unchanged), rendered at the quality bar's 8x; the
damaged and wrecked frames get the concept's scorch, the standing frames its light rim. Every
break-apart frame is its own render of the moving pieces with the key light fixed. One palette
per target (its frames and its break-apart).

Run: python3 tools/art/l04_targets.py [--review]   (~30 s)
"""
import sys
from concurrent.futures import ProcessPoolExecutor
from dataclasses import replace

import numpy as np
import yaml
from PIL import Image

import artkit
from artkit import DESIGN, sprite
from render.sdf import rotate_x, rotate_z, sd_box  # noqa: E402

import ground_targets as gt  # noqa: E402  (concept script: light rim, scorch)
import ground_targets_r17 as r17  # noqa: E402  (concept script, imported unchanged)
from loot_targets import crumbled  # noqa: E402  (the break-apart's burn-down)

SCRIPT = "l04_targets.py"
SOURCE = artkit.source_note(SCRIPT, "M4 part D batch")
LEVEL_DIR = DESIGN / "campaign" / "act-1-first-contact" / "level-04-tranquility-run"
DATA = {t["target"]: t for t in yaml.safe_load((LEVEL_DIR / "data.yaml").read_text(encoding="utf-8"))["ground_targets"]}
# name: (the data's target, the chosen model, the break-apart's canvas)
TARGETS = {
    "dugout": ("prospector's dugout", r17.dugout_a, 72),
    "supply-drop": ("supply drop", r17.drop_a, 48),
}
COLOURS = 32
BREAK_FRAMES, CRUMBLE_FRAMES = 8, 3


def size_of(name):
    return tuple(DATA[TARGETS[name][0]]["size"])


def render(scene, mats, size):
    hi, factor = artkit.render_hi(scene, mats, size, float(size[0]), z_top=float(max(size)), steps=150)
    return artkit.native(hi, factor, crisp=60)


def frame(job):
    """State s (0 intact, 1 damaged, 2 wrecked) as the concept's render_frame finishes it."""
    name, s = job
    w, h = size = size_of(name)
    arr = np.array(render(TARGETS[name][1](s), r17.materials(s), size)).astype(np.float64)
    if s < 2:
        arr = gt.light_rim(arr)
    if s >= 1:
        rng = np.random.default_rng(17 + s)
        spots = [(rng.uniform(0.2, 0.8) * w, rng.uniform(0.2, 0.8) * h, rng.uniform(3, 6)) for _ in range(2 * s)]
        arr = gt.scorch(arr, spots, 7 + s)
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")


def pieces(size, seed=5):
    """Six cells of the target (3 x 2) with an outward velocity, a spin and a tumble."""
    rng = np.random.default_rng(seed)
    w, h = size
    cw, ch = w / 3, h / 2
    out = []
    for cy in range(2):
        for cx in range(3):
            centre = np.array([(cx + 0.5) * cw - w / 2, h / 2 - (cy + 0.5) * ch, 3.0])
            direction = centre[:2] / max(np.hypot(*centre[:2]), 1) + rng.uniform(-0.25, 0.25, 2)
            velocity = direction * rng.uniform(2.0, 2.6)
            out.append((centre, (cw / 2, ch / 2), velocity, rng.uniform(-0.25, 0.25), rng.uniform(-0.3, 0.3)))
    return out


def break_frame(job):
    """Frame f: every piece of the damaged model moved, spun and tumbled; its markings move with it."""
    name, f = job
    size = size_of(name)
    base = TARGETS[name][1](1)
    base_mats = r17.materials(1)
    char = 1 - 0.07 * f
    transforms = []
    for centre, half, velocity, spin, tumble in pieces(size):
        offset = np.array([velocity[0] * (f + 1), velocity[1] * (f + 1), -0.4 * f])

        def to_local(p, c=centre, o=offset, s=spin * (f + 1), t=tumble * (f + 1)):
            return rotate_x(rotate_z(p - c - o, s), t) + c
        transforms.append((to_local, centre, half))
    mats = []
    for to_local, _, _ in transforms:
        for m in base_mats:
            pattern = (lambda p, n, f_=m.pattern, tl=to_local: f_(tl(p), n)) if m.pattern else None
            mats.append(replace(m, albedo=tuple(np.array(m.albedo) * char), pattern=pattern))
    count = len(base_mats)

    def scene(p):
        best_d, best_m = None, None
        for i, (to_local, centre, half) in enumerate(transforms):
            q = to_local(p)
            d, m = base(q)
            d = np.maximum(d, sd_box(q, (centre[0], centre[1], 0), (half[0], half[1], 20.0)))
            m = m + i * count
            if best_d is None:
                best_d, best_m = d, m
            else:
                closer = d < best_d
                best_d, best_m = np.where(closer, d, best_d), np.where(closer, m, best_m)
        return best_d, best_m
    edge = TARGETS[name][2]
    return crumbled(render(scene, mats, (edge, edge)), f - (BREAK_FRAMES - CRUMBLE_FRAMES) + 1)


def build():
    with ProcessPoolExecutor() as pool:
        for name in TARGETS:
            frames = list(pool.map(frame, [(name, s) for s in range(3)]))
            breaks = list(pool.map(break_frame, [(name, f) for f in range(BREAK_FRAMES)]))
            for img in frames:
                if img.size != size_of(name):
                    raise ValueError(f"{name}: rendered {img.size}, the data file says {size_of(name)}")
            done = artkit.quantize_set(frames + breaks, COLOURS)      # one palette per target
            artkit.write_frames(name, done[:3], SOURCE)
            artkit.write_frames(f"{name}-break", done[3:], SOURCE)
            print(f"{name}: 3 frames, {BREAK_FRAMES} break frames, {artkit.colour_count(done)} colours")


def review():
    artkit.REVIEW_ROUND = "r17"
    sets = {name: (artkit.load_frames(name), artkit.load_frames(f"{name}-break")) for name in TARGETS}
    rows = []
    for name, (frames, breaks) in sets.items():
        rows += [(f"{name.upper()}: INTACT, DAMAGED, WRECKED", frames, 5, False),
                 (f"{name.upper()}: BREAK-APART", breaks, 2, False)]
    sheet = artkit.review_sheet("LEVEL 04 LOOT TARGETS - FINAL SPRITES", rows, batch="M4 part D batch")
    ground = (92, 90, 92, 255)
    gif = []
    for i in range(44):                     # intact, two hits, break-apart, the wreck left behind
        img = Image.new("RGBA", (200, 90), ground)
        for (frames, breaks), x in zip(sets.values(), (60, 150)):
            if i < 30:
                sprite.paste_center(img, frames[0 if i < 12 else 1], x, 45)
            else:
                sprite.paste_center(img, frames[2], x, 45)
                if i - 30 < len(breaks):
                    sprite.paste_center(img, breaks[i - 30], x, 45)
        gif.append(sprite.enlarge(img, 3))
    artkit.save_review(sheet, gif, LEVEL_DIR / "concept", "l04-targets", fps=10, variant="b")


if __name__ == "__main__":
    if "--review" not in sys.argv[1:]:
        build()
    review()
