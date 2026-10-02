#!/usr/bin/env python3
"""Production art: Level 01's loot targets under readability rule 7 (design/art-direction):
matte amber hazard markings on a rust body against the cool station, a 1 px light rim, a
damage state from the first hit, a clear break-apart; the secret's beacon blinks.

Outputs (assets/sprites/, sizes from the `ground_targets` of Level 01's data.yaml):
  cargo-container_0..1.png        32x24 intact, damaged (scorch, a hole, a crack in the roof)
  cargo-container-break_0..7.png  48x48 the damaged container in six pieces that fly apart,
                                  tumble and char, then crumble away from their edges over the
                                  last three frames (1-bit alpha)
  beacon_0..3.png                 12x12 dark, lit; the same two damaged (scorch, cracked lens)
  glint_0..2.png                  9x9 sparkle, additive, unchanged from the placeholder kit
  design/campaign/act-1-first-contact/level-01-break-at-dawn/concept/loot-targets-final-r12-a.png/.gif

The models and materials are the placeholder kit's (tools/concept/ground_targets.py), rendered
at 8x; the damage is carved and scorched in the model, and every break-apart frame is its own
render of the moving pieces with the key light fixed (nothing is rotated as an image).

Run: python3 tools/art/loot_targets.py [--review]   (~20 s)
"""
import sys
from concurrent.futures import ProcessPoolExecutor
from dataclasses import replace

import numpy as np
import yaml
from PIL import Image, ImageFilter

import artkit
from artkit import DESIGN, sprite
from render.sdf import (mirror_x, rotate_x, rotate_z, sd_box, sd_capsule, sd_cylinder_z,  # noqa: E402
                        sd_sphere, subtract, union)

import ground_targets as gt  # noqa: E402  (concept script, imported unchanged)

SCRIPT = "loot_targets.py"
SOURCE = artkit.source_note(SCRIPT)
LEVEL_DIR = DESIGN / "campaign" / "act-1-first-contact" / "level-01-break-at-dawn"
TARGETS = {t["target"]: t for t in yaml.safe_load((LEVEL_DIR / "data.yaml").read_text(encoding="utf-8"))["ground_targets"]}
CONTAINER = tuple(TARGETS["cargo container"]["size"])
BEACON = tuple(TARGETS["beacon"]["size"])
BREAK_SIZE, BREAK_FRAMES = gt.BREAK_SIZE, gt.BREAK_FRAMES
BODY, HAZARD, STEEL, LENS = gt.BODY, gt.HAZARD, gt.STEEL, gt.LENS
COLOURS = 32
CRUMBLE_FRAMES = 3                # the last frames of the break-apart erode the pieces
# Damage in sprite px (x right, y down): scorch spots (x, y, radius) and the hole
CONTAINER_SCORCH = [(0.64, 0.38, 7), (0.3, 0.66, 5)]
BEACON_SCORCH = [(0.75, 0.7, 3)]


def to_model(x, y, size):
    """Sprite px -> model units (1 unit = 1 px, origin at the centre, y up)."""
    w, h = size
    return x - w / 2, h / 2 - y


def scorched(mats, spots, size):
    """Materials darkened by soft, speckled scorch spots (fractions of the sprite size)."""
    w, h = size
    centres = [(*to_model(fx * w, fy * h, size), r) for fx, fy, r in spots]

    def burn(p, n):
        b = np.zeros(len(p))
        for x, y, r in centres:
            b = np.maximum(b, np.clip(1.3 - np.hypot(p[:, 0] - x, p[:, 1] - y) / r, 0, 1))
        speck = np.modf(np.abs(np.sin(p[:, 0] * 12.9898 + p[:, 1] * 78.233) * 43758.5453))[0]
        return 1 - 0.9 * b * (0.6 + 0.4 * speck)

    return [replace(m, pattern=(lambda p, n, f=m.pattern: f(p, n) * burn(p, n)) if m.pattern else burn) for m in mats]


def container_scene(size, damaged):
    w, h = size
    hw, hh, band = w / 2 - 0.5, h / 2 - 0.5, 4.0
    hole = (*to_model(CONTAINER_SCORCH[0][0] * w, CONTAINER_SCORCH[0][1] * h, size), 6.6)

    def scene(p):
        q = mirror_x(p)
        parts = [(sd_box(p, (0, 0, 0), (hw - 0.4, hh - 0.4, 6), 1.0), BODY),
                 (sd_box(q, (hw - band, 0, 0.3), (band, hh, 6.3), 0.8), HAZARD)]
        parts += [(sd_box(p, (x, 0, 6.2), (0.7, hh - 2, 0.6), 0.3), BODY) for x in range(-8, 9, 4)]
        d, m = union(*parts)
        if damaged:
            d, m = subtract((d, m), sd_sphere(p, hole, 2.0))
            crack = sd_capsule(p, (hole[0] + 1.5, hole[1] + 1.5, 6.9), (hole[0] + 4, hole[1] + 7.5, 6.9), 0.45)
            d, m = subtract((d, m), crack)
        return d, m
    return scene


def finish(frames, rim=True):
    """Native frames -> the light rim of rule 7 (on solid frames) and one shared palette."""
    if rim:
        frames = [Image.fromarray(gt.light_rim(np.array(f).astype(np.float64)).astype(np.uint8), "RGBA") for f in frames]
    return artkit.quantize_set(frames, COLOURS)


def render(scene, mats, size):
    hi, factor = artkit.render_hi(scene, mats, size, float(size[0]), z_top=float(max(size)), steps=150)
    return artkit.native(hi, factor, crisp=60)


def container_frame(damaged):
    mats = gt.materials(False)
    if damaged:
        mats = scorched(mats, CONTAINER_SCORCH, CONTAINER)
    return render(container_scene(CONTAINER, damaged), mats, CONTAINER)


# --------------------------------------------------------------------------- break-apart

def pieces(seed=5):
    """Six cells of the container (3 x 2) with an outward velocity, a spin and a tumble."""
    rng = np.random.default_rng(seed)
    w, h = CONTAINER
    cw, ch = w / 3, h / 2
    out = []
    for cy in range(2):
        for cx in range(3):
            centre = np.array([(cx + 0.5) * cw - w / 2, h / 2 - (cy + 0.5) * ch, 3.0])
            direction = centre[:2] / max(np.hypot(*centre[:2]), 1) + rng.uniform(-0.25, 0.25, 2)
            velocity = direction * rng.uniform(2.0, 2.6)
            out.append((centre, (cw / 2, ch / 2), velocity, rng.uniform(-0.25, 0.25), rng.uniform(-0.3, 0.3)))
    return out


def break_frame(f):
    """Frame f: every piece moved, spun and tumbled; its markings and scorch move with it."""
    base = container_scene(CONTAINER, True)
    base_mats = scorched(gt.materials(False), CONTAINER_SCORCH, CONTAINER)
    char = 1 - 0.07 * f
    transforms = []
    for centre, half, velocity, spin, tumble in pieces():
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
            cell = sd_box(q, (centre[0], centre[1], 0), (half[0], half[1], 9.0))
            d = np.maximum(d, cell)
            m = m + i * count
            if best_d is None:
                best_d, best_m = d, m
            else:
                closer = d < best_d
                best_d, best_m = np.where(closer, d, best_d), np.where(closer, m, best_m)
        return best_d, best_m
    frame = render(scene, mats, (BREAK_SIZE, BREAK_SIZE))
    return crumbled(frame, f - (BREAK_FRAMES - CRUMBLE_FRAMES) + 1)


def crumbled(frame, layers):
    """The pieces with ``layers`` px eaten from their edges, the outermost remaining layer
    ragged (a fixed pixel hash takes about half of it), so they burn down to embers."""
    if layers <= 0:
        return frame
    a = np.array(frame)
    mask = frame.getchannel("A")
    for _ in range(layers):
        mask = mask.filter(ImageFilter.MinFilter(3))
    keep = np.array(mask) > 0
    rim = keep & ~(np.array(mask.filter(ImageFilter.MinFilter(3))) > 0)
    y, x = np.indices(keep.shape)
    keep &= ~(rim & (np.modf(np.abs(np.sin(x * 12.9898 + y * 78.233)) * 43758.5453)[0] < 0.5))
    a[~keep] = 0
    return Image.fromarray(a, "RGBA")


# --------------------------------------------------------------------------- beacon

def beacon_frame(lit, damaged):
    w, _ = BEACON
    r = w / 2 - 0.5

    def scene(p):
        d, m = union((sd_cylinder_z(p, (0, 0, 0), r - 0.3, 2.0), HAZARD),
                     (sd_cylinder_z(p, (0, 0, 2.0), r - 2.6, 0.8), STEEL),
                     (sd_sphere(p, (0, 0, 2.2), r * 0.5), LENS))
        if damaged:
            d, m = subtract((d, m), sd_capsule(p, (-1.2, 2.2, 5.0), (1.0, -2.0, 5.0), 0.35))
        return d, m
    mats = gt.materials(lit)
    if damaged:
        mats = scorched(mats, BEACON_SCORCH, BEACON)
    return render(scene, mats, BEACON)


def build():
    with ProcessPoolExecutor() as pool:
        containers = pool.map(container_frame, (False, True))
        breaks = pool.map(break_frame, range(BREAK_FRAMES))
        beacons = pool.map(beacon_frame, (False, True, False, True), (False, False, True, True))
        containers, breaks, beacons = list(containers), list(breaks), list(beacons)
    artkit.write_frames("cargo-container", finish(containers), SOURCE)
    artkit.write_frames("cargo-container-break", finish(breaks, rim=False), SOURCE)
    artkit.write_frames("beacon", finish(beacons), SOURCE)
    artkit.write_frames("glint", [artkit.additive(g) for g in gt.glints()], SOURCE)


def review():
    containers = artkit.load_frames("cargo-container")
    breaks = artkit.load_frames("cargo-container-break")
    beacons = artkit.load_frames("beacon")
    glints = artkit.load_frames("glint")
    sheet = artkit.review_sheet("LEVEL 01 LOOT TARGETS - FINAL SPRITES", [
        ("CARGO CONTAINER: INTACT, DAMAGED", containers, 5, False),
        ("BREAK-APART", breaks, 3, False),
        ("BEACON: DARK, LIT, DAMAGED DARK, DAMAGED LIT", beacons, 6, False),
        ("GLINT (ADDITIVE)", glints, 6, True)])
    deck = (70, 76, 104, 255)
    gif = []
    for i in range(40):                     # intact with a glint, two hits, break-apart; the beacon blinks
        img = Image.new("RGBA", (120, 64), deck)
        if i < 30:
            box = containers[0 if i < 12 else 1]
            sprite.paste_center(img, box, 40, 32)
            if i < 3:
                img = artkit.add_light(img, glints[i], (40 - 8 - 4, 32 - 6 - 4))
        elif i - 30 < len(breaks):
            sprite.paste_center(img, breaks[i - 30], 40, 32)
        sprite.paste_center(img, beacons[(2 if i >= 20 else 0) + (i // 5) % 2], 96, 32)
        gif.append(sprite.enlarge(img, 4))
    artkit.save_review(sheet, gif, LEVEL_DIR / "concept", "loot-targets", fps=10)


if __name__ == "__main__":
    if "--review" not in sys.argv[1:]:
        build()
    review()
