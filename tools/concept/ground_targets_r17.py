#!/usr/bin/env python3
"""Concept round 17 - Level 04's two small loot targets on the ground layer, two variants each
(design/campaign/act-1-first-contact/level-04-tranquility-run, "Ground targets" and "Secrets and
pickups"; art direction: ground structures get a damaged and a wrecked frame, readability rule 7).

Outputs (design/campaign/act-1-first-contact/level-04-tranquility-run/concept/):
  dugout-r17-a.png        the prospector's dugout (48x32, hardened, reveals the cache), variant A:
                          a corrugated half-buried hut in a regolith berm with sandbag rows, an
                          armoured hatch plate in amber/black hazard stripes over the right end and
                          a hazard band round the roof, a regolith slide over the collapsed far
                          end, a mast with a red lamp
  dugout-r17-b.png        variant B: an octagonal bunker of sintered regolith with an armoured roof
                          hatch in an amber/black hazard ring, a vent stack, a solar panel and a
                          rubble slide over one corner
  supply-drop-r17-a.png   the CDF supply drop (32x24, 3 HP, medium salvage + a special charge),
                          variant A: a steel drop crate with amber/black hazard end bands, ribs and
                          four corner retro nozzles, a red lamp
  supply-drop-r17-b.png   variant B: a squat cylindrical drop pod on four splayed legs, two amber/
                          black hazard rings, a nose cone and a hatch strip
Each sheet shows the intact, damaged and wrecked frames on a strip of Luna regolith (the chosen
scene's ground, scenes_r06.py's height-field helpers and grey ramp) at 1x and at 3x.

Every frame is its own top-down SDF render (render/sdf.py, the fixed top-left key light, 1 world
unit = 1 px, 4x supersampled, 32 colours) with the station kit's materials in Level 01's muted
kit palette (render/station.py, KIT_PAL of backdrop_l01.py), plus the loot target's matte amber
under black hazard stripes (ground_targets.py's stripes, rim and scorch helpers, imported
unchanged): the intact and damaged frames get the 1 px light rim, the damage is modelled (dents,
cracks, a torn part) plus scorch marks; the wreck is blown open onto a dark interior, its amber
burnt dull and without a rim, since it no longer pays anything.

Run: python3 tools/concept/ground_targets_r17.py   (a few seconds)
"""
import sys
from dataclasses import replace
from pathlib import Path

import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import backdrop_l01  # noqa: E402  (KIT_PAL)
import ground_targets as gt  # noqa: E402  (stripes, light rim, scorch)
import scenes_r06 as s6  # noqa: E402  (regolith helpers)
from parallax_r02 import posterize  # noqa: E402
from render import raster, sdf, sprite, station  # noqa: E402
from render.config import ROOT  # noqa: E402
from render.sdf import (Material, mirror_x, rotate_x, rotate_z, sd_box, sd_capsule,  # noqa: E402
                        sd_cylinder_x, sd_cylinder_z, sd_ellipsoid, sd_plate, sd_sphere, subtract, union)

OUT = ROOT / "design" / "campaign" / "act-1-first-contact" / "level-04-tranquility-run" / "concept"
HULL, DARK, SOLAR, ACCENT, RED, WARM, GLASS = range(7)
AMBER, BAG, SINTER, VOID, CHAR, CORR = range(7, 13)
STATES = ("intact", "damaged", "wrecked")
FACTOR, COLOURS = 4, 32
SHADOW = (3, 4)                                 # low structures: a short cast shadow down-right


def corrugated(p, n):
    return np.where((p[:, 0] % 2.0) < 0.9, 0.78, 1.04)


def materials(state):
    mats = station.materials(backdrop_l01.KIT_PAL)
    amber = Material((0.98, 0.66, 0.14), metal=0.0, shininess=8, spec=0.05, pattern=gt.hazard_stripes)
    if state == 2:                                                      # burnt dull, no longer loot
        amber = replace(amber, albedo=(0.36, 0.24, 0.1))
    mats[HULL] = replace(mats[HULL], pattern=None) if state == 2 else mats[HULL]
    return mats + [
        amber,
        Material((0.46, 0.45, 0.44), metal=0.0, shininess=6, spec=0.05),   # regolith bags, berm
        Material((0.52, 0.49, 0.45), metal=0.0, shininess=10, spec=0.1,
                 pattern=sdf.panel_lines(0.16, 0.05, 0.85)),               # sintered regolith
        Material((0.03, 0.03, 0.04), metal=0.0, shininess=4, spec=0.0),    # the dark interior
        Material((0.09, 0.08, 0.08), metal=0.1, shininess=10, spec=0.1),   # charred metal
        replace(mats[HULL], pattern=corrugated),                           # corrugated hull sheet
    ]


# --------------------------------------------------------------------------- dugout (48x32)

def dugout_a(s):
    """Corrugated hut half-buried in a berm, hatch at the right end, far end collapsed."""
    def scene(p):
        hut = (sd_cylinder_x(p, (-2, 1, -3), 9.5, 14), CORR)
        items = [(sd_ellipsoid(p, (-1, 0, -5), (23, 15, 8)), BAG),            # the berm
                 hut,
                 (sd_ellipsoid(p, (-13, 3, 1), (8, 9, 6.5)), SINTER),         # the collapsed end
                 (sd_box(p, (15, 1, 0), (3, 7, 5.5), 1.0), HULL)]             # hatch housing
        if s < 2:                                                             # hatch plate, roof band
            items += [(sd_box(p, (15.5, 1, 5.6), (3.4, 6.8, 0.9), 0.4), AMBER),
                      (sd_box(p, (15.8, 1, 6.6), (1.2, 3.6, 0.5), 0.3), DARK),
                      (sd_cylinder_x(p, (8, 1, -3), 10.0, 1.6), AMBER)]
        for side in (1, -1):                                                  # sandbag rows
            for i in range(5 if s < 2 else 3):
                x = -8 + i * 5.2 + (side + 1) * 0.8
                items.append((sd_capsule(p, (x - 2, side * 12.4, 1.5), (x + 2, side * 12.4, 1.5), 1.6), BAG))
        mast_top = (-17, -10, 9) if s == 0 else ((-13, -12, 6) if s == 1 else (-11, -14, 2))
        items.append((sd_capsule(p, (-17, -10, 0), mast_top, 0.6), DARK))
        if s < 2:
            items.append((sd_sphere(p, mast_top, 1.2), RED))
        d, m = union(*items, k=0.6)
        d, m = union((d, m), (sd_box(p, (2, 1, 6.4), (11, 0.6, 0.6), 0.3), HULL))   # roof ridge
        if s == 1:                                                            # dents in the roof
            d = np.maximum(d, -sd_sphere(p, (4, 5, 9.5), 3.2))
            d = np.maximum(d, -sd_sphere(p, (-4, -3, 9.3), 2.4))
        if s == 2:                                                            # roof blown open
            d, m = subtract((d, m), sd_ellipsoid(p, (2, 1, 6), (13, 8, 9)))
            d, m = union((d, m), (sd_box(p, (2, 1, -3), (13, 7.5, 1), 0.5), VOID),
                         *[(sd_capsule(p, (x, -7, 1), (x + 2, -9.5, 5 + (x % 3)), 0.7), CHAR)
                           for x in (-6, 0, 6)],
                         *[(sd_capsule(p, (x, 9, 1), (x - 1, 11, 4), 0.7), CHAR) for x in (-3, 4, 9)],
                         (sd_box(rotate_z(p - np.array([19.5, -9.0, 0.0]), 0.5), (0, 0, 0.5), (1.0, 5, 4), 0.4), HULL),
                         (sd_box(rotate_z(p - np.array([20.0, -9.5, 0.0]), 0.5), (1.2, 0, 0.5), (0.5, 5.6, 4.6), 0.3), AMBER))
        return d, m
    return scene


def dugout_b(s):
    """Octagonal bunker of sintered regolith, roof hatch with a hazard ring, rubble over a corner."""
    octagon = [(-20, -7), (-13, -14), (13, -14), (20, -7), (20, 7), (13, 14), (-13, 14), (-20, 7)]

    def scene(p):
        items = [(sd_ellipsoid(p, (0, 0, -6), (24, 16, 7)), BAG),
                 (sd_plate(p, octagon, 0.5, 4.5, 1.6), SINTER)]
        if s < 2:
            items += [(sd_box(p, (5, 0, 6.6), (7.5, 7.5, 1.0), 0.4), AMBER),
                      (sd_box(p, (5, 0, 7.6), (4.8, 4.8, 1.0), 0.6), HULL),
                      (sd_box(p, (5, 0, 8.7), (1.2, 3.5, 0.5), 0.3), DARK)]   # the hatch's handle bar
        if s == 0:
            items.append((sd_box(p, (-11, -7, 6), (4.5, 3.2, 0.5), 0.2), SOLAR))
        elif s == 1:                                                          # the panel knocked askew
            q = rotate_z(p - np.array([-11.0, -7.0, 0.0]), 0.35)
            items.append((sd_box(q, (0, 0, 5.5), (4.5, 3.2, 0.5), 0.2), SOLAR))
        items.append((sd_cylinder_z(p, (-9, 8, 6), 2.2, 3 if s < 2 else 1.2), DARK))
        items.append((sd_cylinder_z(p, (-9, 8, 9.2 if s < 2 else 7.4), 1.2, 0.4), CHAR))
        rubble = [(-17, 9, 3, 4.5), (-13, 12, 3.5, 3.6), (-19, 3, 2, 3.4), (-14, 6, 5, 3.0)]
        items += [(sd_sphere(p, (x, y, z), r), SINTER) for x, y, z, r in rubble]
        if s < 2:
            items.append((sd_sphere(p, (17, -12, 4.5), 1.1), RED))
        d, m = union(*items, k=0.8)
        if s >= 1:                                                            # cracks in the slab
            for a, b in (((-3, -14), (2, -4)), ((12, 14), (9, 6)), ((-20, -2), (-12, -1))):
                d = np.maximum(d, -sd_capsule(p, a + (5,), b + (5,), 0.55 if s == 1 else 1.2))
        if s == 1:
            d = np.maximum(d, -sd_sphere(p, (8, 3, 11.0), 2.8))                # dent in the hatch
        if s == 2:                                                            # blown open at the hatch
            d, m = subtract((d, m), sd_ellipsoid(p, (4, 0, 7), (10, 9, 9)))
            d, m = union((d, m), (sd_box(p, (4, 0, -3.5), (8, 7, 1), 0.5), VOID),
                         (sd_plate(rotate_x(p - np.array([4.0, -12.5, 2.0]), 0.6),
                                   [(-6, -2), (5, -3), (7, 2), (-4, 3)], 0, 1.2, 0.4), SINTER),
                         (sd_plate(rotate_z(p - np.array([15.0, 8.0, 0.0]), 0.9),
                                   [(-4, -3), (4, -3), (4, 3), (-4, 3)], 4.5, 0.7, 0.3), CHAR),
                         (sd_box(rotate_z(p - np.array([-12.0, -9.0, 0.0]), 0.7), (0, 0, 3.5), (5, 3.5, 0.4), 0.2), CHAR))
        return d, m
    return scene


# --------------------------------------------------------------------------- supply drop (32x24)

def drop_a(s):
    """A steel drop crate: amber/black hazard end bands, ribs, corner retro nozzles."""
    def scene(p):
        q = mirror_x(p)
        items = [(sd_box(p, (0, 0, 0), (12, 8, 5), 1.0), HULL)]
        if s < 2:
            items.append((sd_box(q, (10, 0, 0.2), (2.6, 8.4, 5.3), 0.8), AMBER))
            items += [(sd_box(p, (x, 0, 5.3), (0.6, 7.2, 0.5), 0.3), DARK) for x in (-4, 0, 4)]
            items.append((sd_sphere(p, (0, 0, 6.2), 1.0), RED))
        corners = [(sx * 12, sy * 8.5) for sx in (1, -1) for sy in (1, -1)]
        if s == 1:
            corners = corners[1:]                                             # one nozzle torn off
        for x, y in corners:
            items.append((sd_cylinder_z(p, (x, y, -1), 2.0, 2.2), DARK))
            items.append((sd_cylinder_z(p, (x, y, 1.6), 1.1, 0.6), CHAR))
        d, m = union(*items, k=0.3)
        if s == 1:
            d = np.maximum(d, -sd_sphere(p, (5, 3, 8.4), 3.6))                # dented lid
            d = np.maximum(d, -sd_capsule(p, (-10, -8, 5), (-5, -3, 5), 0.8))
        if s == 2:                                                            # split open, walls splayed
            d, m = subtract((d, m), sd_box(p, (0, 0, 3), (10.5, 6.5, 6), 0.5))
            d, m = subtract((d, m), sd_box(p, (0, 0, 5), (14, 3, 3)))
            d, m = union((d, m), (sd_box(p, (0, 0, -3.5), (10, 6, 0.6), 0.3), VOID),
                         (sd_cylinder_x(p, (-3, -2, -1.5), 1.6, 3), CHAR),
                         (sd_cylinder_x(p, (5, 3, -1.5), 1.6, 2.5), AMBER),
                         (sd_box(rotate_x(p - np.array([0.0, 10.0, 0.0]), -0.9), (0, 0, 0), (8, 0.5, 3.0), 0.3), CHAR),
                         (sd_box(rotate_x(p - np.array([0.0, -10.0, 0.0]), 0.9), (0, 0, 0), (8, 0.5, 3.0), 0.3), HULL))
        return d, m
    return scene


def drop_b(s):
    """A squat cylindrical drop pod on four legs, two hazard rings, a nose cone."""
    def scene(p):
        items = [(sd_cylinder_x(p, (-1, 0, 0), 6.2, 9.5), HULL),
                 (sd_capsule(p, (8.5, 0, 0), (13.5, 0, 0), 5.8, 2.0), DARK)]
        if s < 2:
            items += [(sd_cylinder_x(p, (x, 0, 0), 6.6, 1.6), AMBER) for x in (-7, 5)]
            items.append((sd_box(p, (-1, 0, 6.0), (4.5, 1.6, 0.6), 0.4), DARK))   # the hatch strip
            items.append((sd_sphere(p, (-10, 3, 4), 1.0), RED))
        else:
            items.append((sd_cylinder_x(p, (-7, 0, 0), 6.6, 1.6), AMBER))
        legs = [((-6, 4.5), (-11, 10.5)), ((4, 4.5), (8, 10.5)), ((-6, -4.5), (-11, -10.5)), ((4, -4.5), (8, -10.5))]
        if s == 1:
            legs[1] = ((4, 4.5), (6, 7))                                       # a leg snapped
        for (ax, ay), (bx, by) in legs:
            items.append((sd_capsule(p, (ax, ay, 0), (bx, by, -4), 0.8, 0.6), DARK))
            items.append((sd_sphere(p, (bx, by, -4), 1.3), DARK))
        d, m = union(*items, k=0.3)
        if s == 1:
            d = np.maximum(d, -sd_sphere(p, (1, 4, 8.4), 3.4))                # dent
        if s == 2:                                                            # split, the cone knocked off
            d, m = subtract((d, m), sd_box(p, (-1, 0, 5), (8.5, 4.2, 6), 0.5))
            d, m = subtract((d, m), sd_box(p, (11, 0, 0), (4, 8, 8)))
            d, m = union((d, m), (sd_box(p, (-1, 0, -2.5), (8, 4, 0.6), 0.3), VOID),
                         (sd_capsule(rotate_z(p - np.array([11.0, -4.0, 0.0]), -0.7),
                                     (-2, 0, -1), (3, 0, -1), 4.0, 1.6), CHAR))
        return d, m
    return scene


TARGETS = {
    "dugout": ((48, 32), {"a": dugout_a, "b": dugout_b},
               "PROSPECTOR'S DUGOUT 48X32 - HARDENED, REVEALS THE CACHE"),
    "supply-drop": ((32, 24), {"a": drop_a, "b": drop_b},
                    "CDF SUPPLY DROP 32X24 - MEDIUM SALVAGE + SPECIAL CHARGE"),
}
DESCRIPTIONS = {
    ("dugout", "a"): "CORRUGATED HUT IN A BERM, HAZARD HATCH AND ROOF BAND",
    ("dugout", "b"): "SINTERED OCTAGON BUNKER, ROOF HATCH IN A HAZARD RING",
    ("supply-drop", "a"): "STEEL DROP CRATE, HAZARD END BANDS, RETRO NOZZLES",
    ("supply-drop", "b"): "CYLINDRICAL DROP POD ON LEGS, HAZARD RINGS",
}


def render_frame(make, size, s):
    w, h = size
    hi = sdf.render(make(s), materials(s), (w * FACTOR, h * FACTOR), float(w), z_top=max(w, h), steps=150)
    arr = np.array(sprite.make_sprite(hi, FACTOR, COLOURS, crisp=60)).astype(np.float64)
    if s < 2:
        arr = gt.light_rim(arr)
    if s >= 1:
        rng = np.random.default_rng(17 + s)
        spots = [(rng.uniform(0.2, 0.8) * w, rng.uniform(0.2, 0.8) * h, rng.uniform(3, 6)) for _ in range(2 * s)]
        arr = gt.scorch(arr, spots, 7 + s)
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")


# --------------------------------------------------------------------------- regolith and sheet

def regolith(w, h, seed):
    """A strip of the Luna scene's ground: fbm height with craters, hillshade, long low-sun cast
    shadows, the grey ramp with the blue earthshine in the shadows, posterized to 9 tones."""
    rng = np.random.default_rng(seed)
    hgt = raster.fbm(w, h, 32, seed, octaves=5) * 0.5 + raster.fbm(w, h, 8, seed + 1, octaves=1) * 0.05
    yy, xx = np.mgrid[0:h, 0:w]
    for _ in range(w // 24):
        cx, cy, r = rng.uniform(0, w), rng.uniform(0, h), rng.uniform(4, 12)
        d = np.hypot(xx - cx, yy - cy) / r
        hgt += (np.where(d < 1, -(1 - d * d) * 0.55, 0) + np.exp(-((d - 1) / 0.16) ** 2) * 0.2) * min(1.0, r / 26)
    shade = s6.hillshade(hgt * 60, 0.55)
    lit = s6.cast_shadows(hgt * 60, steps=28, drop=1.0)
    t = (hgt - hgt.min()) / (hgt.max() - hgt.min())
    col = s6.ramp_img(s6.LunaScene.GREYS, 0.25 + t * 0.6) * (shade * (0.35 + 0.65 * lit))[..., None]
    col = col + np.array((14, 18, 34)) * (1 - lit)[..., None] * 0.9
    return posterize(raster.to_rgba_image(np.clip(col * 0.8, 0, 255)), 9)


def strip(frames, gap, seed):
    w, h = frames[0].size
    cell = w + gap
    ground = regolith(cell * len(frames), h + gap, seed)
    shadows = Image.new("RGBA", ground.size, (0, 0, 0, 0))
    for i, f in enumerate(frames):
        x, y = i * cell + gap // 2, gap // 2
        shadows.alpha_composite(sprite.shadow_of(f, opacity=0.55, blur=1.2), (x + SHADOW[0], y + SHADOW[1]))
    ground.alpha_composite(shadows)
    for i, f in enumerate(frames):
        ground.alpha_composite(f, (i * cell + gap // 2, gap // 2))
    return ground


def make_sheet(target, variant):
    size, makes, title = TARGETS[target]
    frames = [render_frame(makes[variant], size, s) for s in range(3)]
    small = strip(frames, 24, 1700 + len(target) + ord(variant))
    big = sprite.enlarge(small, 3)
    sw = max(16 + big.width + 16, 640)
    sheet = raster.sheet(sw, 74 + small.height + 30 + big.height + 30,
                         f"{target.upper()} - ROUND 17 VARIANT {variant.upper()}", "LEVEL 04 GROUND LOOT TARGET")
    raster.draw_text(sheet, 16, 40, title, raster.LABEL)
    raster.draw_text(sheet, 16, 52, DESCRIPTIONS[(target, variant)], raster.LABEL_DIM)
    y = 70
    raster.draw_text(sheet, 16, y, "1X ON LUNA REGOLITH: INTACT, DAMAGED, WRECKED", raster.LABEL_DIM)
    sheet.alpha_composite(small, (16, y + 12))
    y += 12 + small.height + 12
    raster.draw_text(sheet, 16, y, "3X", raster.LABEL_DIM)
    sheet.alpha_composite(big, (16, y + 12))
    cell = (size[0] + 24) * 3
    for i, name in enumerate(STATES):
        raster.draw_text(sheet, 16 + i * cell + 6, y + 12 + big.height + 6, name.upper(), raster.LABEL)
    path = OUT / f"{target}-r17-{variant}.png"
    sheet.convert("RGB").save(path, optimize=True)
    print(path.relative_to(ROOT))


def main():
    for target in TARGETS:
        for variant in ("a", "b"):
            make_sheet(target, variant)


if __name__ == "__main__":
    main()
