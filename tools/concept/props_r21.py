#!/usr/bin/env python3
"""Concept round 21 - Level 05's small props that are still placeholders, two variants each
(design/campaign/act-1-first-contact/level-05-crater-nest, "Hazards" and "Secrets and pickups";
design/enemies/ground/polyp-mortar, "Death"; user decision D8 of M4 part E).

Outputs (design/campaign/act-1-first-contact/level-05-crater-nest/concept/):
  rocks-r21-a.png          the low-gravity rocks (16x16, air layer, thrown by a destroyed ground
                           unit), variant A: plain regolith chunks, light grey with a bright rim
                           and a long air-layer shadow; one rock tumbling (4 frames) and two
                           other shapes
  rocks-r21-b.png          variant B: dark scorched basalt chunks torn from the nest floor, each
                           with a crust of glowing lime creep on one face
  ore-canister-r21-a.png   the stuck sled's ore canister (40x28, jammed on Level 04's rail),
                           variant A: a steel ore cylinder with amber/black hazard bands, held by
                           a dark clamp yoke with the beacon; beacon dark, beacon lit, clamp shot
  ore-canister-r21-b.png   variant B: an open ore skip heaped with grey-brown ore, hazard rim,
                           clamp jaws at both ends with the beacon in the middle
  acid-splash-r21-a.png    the acid splash decal (40x40, ground layer; the Polyp Mortar's death,
                           its spec's "acid splash decal"), variant A: a wet lime splatter with
                           droplets and glints; fresh, after 1 s, fading
  acid-splash-r21-b.png    variant B: an etched burn: a dark scorched pit with a teal-green
                           bubbling pool and a pale etched ring; fresh, after 1 s, fading
Each sheet shows the frames on a strip of Luna regolith (ground_targets_r17.py's regolith(), the
chosen scene's ground) at 1x and at 3x; the canister sits on Level 04's rail frame
(assets/backdrop/level-04/sled-run_0.png). The battery outline gets no concept: the game's 1 px
outline in the objective colour stays (main-agent call, round 21).

The solid props are top-down SDF renders (render/sdf.py, the fixed top-left key light, 1 world
unit = 1 px, 4x supersampled) with ground_targets.py's light rim, scorch and hazard stripes; the
rocks' surfaces are displaced spheres. The decals are 2D fields (numpy), drawn flat (no shadow).

Run: python3 tools/concept/props_r21.py   (a few seconds)
"""
import sys
from dataclasses import replace
from pathlib import Path

import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import ground_targets as gt  # noqa: E402  (stripes, light rim, scorch)
import ground_targets_r17 as r17  # noqa: E402  (the regolith strip)
from render import raster, sdf, sprite  # noqa: E402
from render.config import ROOT  # noqa: E402
from render.sdf import (Material, rotate_x, rotate_y, rotate_z, sd_box, sd_cylinder_x,  # noqa: E402
                        sd_cylinder_z, sd_ellipsoid, sd_sphere, union)

OUT = ROOT / "design" / "campaign" / "act-1-first-contact" / "level-05-crater-nest" / "concept"
RAIL = ROOT / "assets" / "backdrop" / "level-04" / "sled-run_0.png"
FACTOR = 4
LIME = (0.62, 0.95, 0.18)


def render(scene, mats, size, colours=32, rim=True):
    w, h = size
    hi = sdf.render(scene, mats, (w * FACTOR, h * FACTOR), float(w), z_top=max(w, h), steps=150)
    arr = np.array(sprite.make_sprite(hi, FACTOR, colours, crisp=60)).astype(np.float64)
    return gt.light_rim(arr) if rim else arr


def to_img(arr):
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")


# --------------------------------------------------------------------------- rocks (16x16)

def bumps(q, seed):
    rng = np.random.default_rng(seed)
    d = 0.0
    for _ in range(5):
        k = rng.normal(size=3)
        k = k / np.linalg.norm(k) * rng.uniform(0.6, 1.4)
        d = d + np.sin(q @ k + rng.uniform(0, 6.3)) * rng.uniform(0.25, 0.55)
    return d


def rock_scene(seed, turn, crust):
    stretch = np.random.default_rng(seed).uniform(0.75, 1.0, 3)

    def scene(p):
        q = rotate_x(rotate_y(rotate_z(p - np.array([0, 0, 4.5]), turn), turn * 0.7), turn * 0.4)
        q = q / stretch
        d = (np.linalg.norm(q, axis=1) - 6.2) * stretch.min() + bumps(q, seed)
        d = np.maximum(d, -q[:, 2] - 3.4)                             # one flat broken face
        items = [(d, 0)]
        if crust:
            c = np.linalg.norm(q - np.array([1.5, -1.0, 4.2]), axis=1) - 2.4 + 0.4 * bumps(q * 2, seed + 9)
            items.append((np.maximum(c, d - 0.5), 1))
        return union(*items)
    return scene


def rocks(variant):
    if variant == "a":
        mats = [Material((0.66, 0.64, 0.61), metal=0.0, shininess=8, spec=0.08), None]
    else:
        mats = [Material((0.20, 0.19, 0.20), metal=0.1, shininess=20, spec=0.25),
                Material((0.25, 0.42, 0.08), emission=tuple(c * 0.9 for c in LIME))]
    mats[1] = mats[1] or mats[0]
    crust = variant == "b"
    frames = [render(rock_scene(5, t, crust), mats, (16, 16), 16) for t in (0.0, 0.8, 1.6, 2.4)]
    frames += [render(rock_scene(s, 0.4, crust), mats, (16, 16), 16) for s in (11, 23)]
    return [to_img(f) for f in frames], ["TUMBLE 1", "2", "3", "4", "ROCK 2", "ROCK 3"]


# --------------------------------------------------------------------------- ore canister (40x28)

def canister_mats(lit, variant):
    mats = gt.materials(lit)                                     # rust, hazard amber, hull, lens
    steel = Material((0.58, 0.60, 0.63), metal=0.6, shininess=50, spec=0.6,
                     pattern=sdf.panel_lines(0.25, 0.05, 0.8))
    dark = Material((0.16, 0.17, 0.19), metal=0.4, shininess=30, spec=0.4)
    ore = Material((0.42, 0.36, 0.30), metal=0.0, shininess=6, spec=0.05)
    glint = Material((0.55, 0.5, 0.42), metal=0.9, shininess=90, spec=1.2)
    return mats + [steel, dark, ore, glint]


RUST, HAZ, HULL, LENS, STEEL, DARK, ORE, GLINT = range(8)


def canister_a(clamp_ok):
    def scene(p):
        items = [(sd_cylinder_x(p, (0, 0, 7.0), 7.5, 17.0), STEEL),
                 (sd_cylinder_x(p, (-12.5, 0, 7.0), 7.8, 2.2), HAZ),
                 (sd_cylinder_x(p, (12.5, 0, 7.0), 7.8, 2.2), HAZ),
                 (sd_cylinder_x(p, (-18.5, 0, 7.0), 5.0, 1.2), DARK),
                 (sd_cylinder_x(p, (18.5, 0, 7.0), 5.0, 1.2), DARK)]
        if clamp_ok:
            items += [(sd_box(p, (0, 0, 9.0), (4.0, 13.0, 6.0), 1.0), DARK),
                      (sd_cylinder_z(p, (0, -3.5, 15.0), 2.6, 1.2), HAZ),
                      (sd_sphere(p, (0, -3.5, 16.2), 1.5), LENS)]
        else:                                                   # the yoke shot open, one jaw gone
            items += [(sd_box(rotate_z(p - np.array([0, 9.0, 9.0]), 0.5), (0, 0, 0), (3.5, 4.5, 5.0), 1.0), DARK),
                      (sd_box(p, (0, -11, 3.0), (3.5, 2.5, 2.0), 0.6), RUST)]
        return union(*items)
    return scene


def canister_b(clamp_ok):
    rng = np.random.default_rng(31)
    lumps = [(rng.uniform(-13, 13), rng.uniform(-6, 6), rng.uniform(2.0, 3.6)) for _ in range(16)]

    def scene(p):
        shell = sd_box(p, (0, 0, 5.0), (15.0, 9.5, 5.0), 1.2)
        hollow = sd_box(p, (0, 0, 9.0), (13.5, 8.0, 5.0), 0.8)
        items = [(np.maximum(shell, -hollow), STEEL),
                 (np.maximum(sd_box(p, (0, 0, 9.4), (15.2, 9.7, 0.7), 0.3), -hollow), HAZ)]
        items += [(sd_sphere(p, (x, y, 8.0 + r * 0.3), r), GLINT if i % 7 == 0 else ORE)
                  for i, (x, y, r) in enumerate(lumps)]
        for sx in (-1, 1):
            if clamp_ok or sx < 0:
                items.append((sd_box(p, (sx * 17.5, 0, 4.0), (2.5, 12.0, 4.0), 0.8), DARK))
            else:
                items.append((sd_box(rotate_z(p - np.array([19.0, 4.0, 2.5]), 0.6), (0, 0, 0), (2.0, 6.0, 2.5), 0.6), RUST))
        if clamp_ok:
            items += [(sd_cylinder_z(p, (0, -11.0, 6.0), 2.6, 1.2), HAZ), (sd_sphere(p, (0, -11.0, 7.2), 1.5), LENS)]
        return union(*items)
    return scene


def canister(variant):
    make = canister_a if variant == "a" else canister_b
    frames = [render(make(True), canister_mats(False, variant), (40, 28)),
              render(make(True), canister_mats(True, variant), (40, 28)),
              gt.scorch(render(make(False), canister_mats(False, variant), (40, 28)),
                        [(24, 12, 5), (30, 18, 4)], 21)]
    return [to_img(f) for f in frames], ["BEACON DARK", "BEACON LIT (HITTABLE)", "CLAMP SHOT"]


# --------------------------------------------------------------------------- acid splash (40x40)

def blobs(n, rng, r0, r1, reach):
    return [(rng.uniform(0, 2 * np.pi), rng.uniform(*reach), rng.uniform(r0, r1)) for _ in range(n)]


def splash_a(stage):
    """Wet lime splatter: a lobed pool, flung droplets and streaks, wet glints; it darkens and
    soaks away."""
    n, c = 40, 20
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float64)
    a = np.arctan2(yy - c, xx - c)
    r = np.hypot(xx - c, yy - c)
    rng = np.random.default_rng(2101)
    lobes = 9 + 2.2 * np.sin(5 * a + 1.0) + 1.4 * np.sin(3 * a - 0.4) + 1.0 * np.sin(8 * a)
    field = np.clip(lobes - r + 1, 0, 1)
    for ang, d, s in blobs(14, rng, 0.8, 2.2, (11, 18)):
        field = np.maximum(field, np.clip(s - np.hypot(xx - c - d * np.cos(ang), yy - c - d * np.sin(ang)) + 0.6, 0, 1))
    for ang, d, s in blobs(6, rng, 0.6, 1.0, (10, 17)):              # streaks thrown outwards
        t = np.clip(((xx - c) * np.cos(ang) + (yy - c) * np.sin(ang) - 8) / d, 0, 1)
        px, py = c + np.cos(ang) * (8 + t * d), c + np.sin(ang) * (8 + t * d)
        field = np.maximum(field, np.clip(s * (1 - 0.6 * t) - np.hypot(xx - px, yy - py) + 0.5, 0, 1) * (t < 1))
    wet = np.clip(1 - r / 11, 0, 1)
    col = np.array([70, 120, 14]) + (np.array([200, 250, 90]) - np.array([70, 120, 14])) * wet[..., None]
    col = col * (1 - 0.45 * stage)
    if stage < 0.5:
        for gx, gy in ((16, 15), (22, 18), (14, 22)):
            col[gy, gx] = (240, 255, 210)
    alpha = field * (0.95 - 0.55 * stage)
    return to_img(np.dstack([col, alpha * 255]))


def splash_b(stage):
    """Etched burn: a dark scorched pit, a teal-green pool with bubbles, a pale etched ring."""
    n, c = 40, 20
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float64)
    a = np.arctan2(yy - c, xx - c)
    r = np.hypot(xx - c, yy - c) / (1 + 0.08 * np.sin(4 * a + 0.7) + 0.05 * np.sin(7 * a))
    rng = np.random.default_rng(2102)
    noise = rng.random((n, n))
    scorch = np.clip((17 - r) / 6, 0, 1) * (0.7 + 0.3 * noise)
    ring = np.exp(-((r - 12.5) / 1.1) ** 2) * (0.6 + 0.4 * noise)
    pool = np.clip(8.5 * (1 - 0.35 * stage) - r, 0, 1)
    col = np.zeros((n, n, 3)) + np.array([22, 20, 18])
    col = col * (1 - ring[..., None]) + np.array([170, 175, 150]) * ring[..., None]
    poolc = np.array([40, 150, 110]) + np.array([60, 80, 40]) * np.clip(1 - r / 8, 0, 1)[..., None]
    col = col * (1 - pool[..., None]) + poolc * (1 - 0.5 * stage) * pool[..., None]
    alpha = np.maximum(scorch * 0.85, np.maximum(ring * 0.9, pool)) * (1 - 0.5 * stage)
    if stage < 0.9:
        for bx, by in rng.uniform(c - 6, c + 6, (7 if stage < 0.5 else 3, 2)):
            if np.hypot(bx - c, by - c) < 7:
                col[int(by), int(bx)] = (190, 255, 220)
    return to_img(np.dstack([col, alpha * 255]))


def splash(variant):
    make = splash_a if variant == "a" else splash_b
    return [make(s) for s in (0.0, 0.5, 0.9)], ["FRESH", "AFTER 1 S", "FADING"]


# --------------------------------------------------------------------------- sheets

PROPS = {
    "rocks": (rocks, (8, 10), False, "LOW-GRAVITY ROCKS - 16X16, AIR LAYER", {
        "a": "PLAIN REGOLITH CHUNKS, BRIGHT RIM, LONG AIR-LAYER SHADOW",
        "b": "SCORCHED BASALT FROM THE NEST FLOOR, GLOWING LIME CREEP CRUST"}),
    "ore-canister": (canister, None, True, "STUCK SLED ORE CANISTER - 40X28 ON THE RAIL", {
        "a": "STEEL ORE CYLINDER, HAZARD BANDS, CLAMP YOKE WITH BEACON",
        "b": "OPEN ORE SKIP HEAPED WITH ORE, HAZARD RIM, CLAMP JAWS AT BOTH ENDS"}),
    "acid-splash": (splash, None, False, "ACID SPLASH DECAL - 40X40, GROUND LAYER", {
        "a": "WET LIME SPLATTER WITH DROPLETS AND GLINTS",
        "b": "ETCHED BURN: SCORCHED PIT, TEAL-GREEN BUBBLING POOL, PALE RING"}),
}


def strip(frames, gap, seed, shadow, rail):
    w, h = frames[0].size
    cell = w + gap
    ground = r17.regolith(cell * len(frames), h + gap, seed)
    if rail:
        track = Image.open(RAIL).convert("RGBA")
        for i in range(len(frames)):
            x = i * cell + (cell - track.width) // 2
            ground.alpha_composite(track.crop((0, 0, track.width, ground.height)), (x, 0))
    if shadow:
        for i, f in enumerate(frames):
            ground.alpha_composite(sprite.shadow_of(f, opacity=0.55, blur=1.2),
                                   (i * cell + gap // 2 + shadow[0], gap // 2 + shadow[1]))
    for i, f in enumerate(frames):
        ground.alpha_composite(f, (i * cell + gap // 2, gap // 2))
    return ground


def make_sheet(prop, variant):
    make, shadow, rail, title, desc = PROPS[prop]
    frames, labels = make(variant)
    gap = 24
    if prop != "acid-splash":
        shadow = shadow or (3, 4)                               # decals lie flat: no shadow
    small = strip(frames, gap, 2100 + len(prop) + ord(variant), shadow, rail)
    big = sprite.enlarge(small, 3)
    sheet = raster.sheet(max(32 + big.width, 640), 74 + small.height + 30 + big.height + 30,
                         f"{prop.upper()} - ROUND 21 VARIANT {variant.upper()}", "LEVEL 05 PROP CONCEPT")
    raster.draw_text(sheet, 16, 40, title, raster.LABEL)
    raster.draw_text(sheet, 16, 52, desc[variant], raster.LABEL_DIM)
    y = 70
    raster.draw_text(sheet, 16, y, "1X ON LUNA REGOLITH", raster.LABEL_DIM)
    sheet.alpha_composite(small, (16, y + 12))
    y += 12 + small.height + 12
    raster.draw_text(sheet, 16, y, "3X", raster.LABEL_DIM)
    sheet.alpha_composite(big, (16, y + 12))
    cell = (frames[0].width + gap) * 3
    for i, name in enumerate(labels):
        raster.draw_text(sheet, 16 + i * cell + 6, y + 12 + big.height + 6, name, raster.LABEL)
    path = OUT / f"{prop}-r21-{variant}.png"
    sheet.convert("RGB").save(path, optimize=True)
    print(path.relative_to(ROOT))


def main():
    for prop in PROPS:
        for variant in ("a", "b"):
            make_sheet(prop, variant)


if __name__ == "__main__":
    main()
