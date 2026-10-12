#!/usr/bin/env python3
"""Production art: Level 14's prop, the CDF armoured car half under creep on the Pont du Mont-Blanc
(design/campaign/act-2-homefront/level-14-siege-spire, "Secrets and pickups"; M5 part H batch, concept
round 36, step H2; one version, decision H8 = a, pending the user's review). No concept: designed to
the README, to the chosen Geneva scene's light (scene-geneva-r10-a) and to the secret targets' rule 7
(the 1 px light rim of tools/concept/ground_targets.py).

The level's other props need no new art: the Hammer flight bombers passing below are
`airstrike_bomber.py`'s, the abandoned and moving cars are Level 08's traffic (`l08_traffic.py`), the
supply drone's armour patch enters without a drone sprite (the README), the crater at the Ravager
pack's entry is a root burst without growths (`root_burst.py`), the stump and the shockwave debris are
the Siege Spire's (`siege_spire.py`).

Outputs (written to assets/sprites/ only with --assets, in the content step's time: no data claims
these names yet, and packAtlases fails on sprites no data claims):
  armoured-car_0..7     40x60 the car seen from above, nose up, parked askew (12 degrees) as it was
                        abandoned: a six-wheeled CDF armoured car (slate-grey hull with the CDF-blue
                        band, a small turret with a short cannon, vision blocks, a rear cargo hatch,
                        stowage boxes), violet creep tissue grown over it with glowing veins.
                        0 fully under creep (six blobs) .. 5 one blob left (each of the six hits
                        clears one, shrivelled scorch where it was), 6 cleared, 7 the cargo hatch
                        sprung open and the amber CDF supply crate inside (the game pays the crate:
                        88 in Act 1 terms); rule 7's light rim on every frame
  design/campaign/act-2-homefront/level-14-siege-spire/concept/l14-props-final-r36-a.png/.gif

Run: python3 tools/art/l14_props.py [--assets]   (~1 min; without --assets only the review files)
"""
import sys
from concurrent.futures import ProcessPoolExecutor

import numpy as np
from PIL import Image

import artkit
from artkit import DESIGN, sprite

import ground_targets as gt  # noqa: E402  (concept script, imported unchanged: the light rim, scorch)
from render.sdf import (Material, mirror_x, rotate_x, rotate_z, sd_box, sd_cylinder_x, sd_ellipsoid,  # noqa: E402
                        union)

SCRIPT = "l14_props.py"
BATCH = "M5 part H batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
REVIEW_ROUND = "r36"
LEVEL = DESIGN / "campaign" / "act-2-homefront" / "level-14-siege-spire"
CONCEPT = LEVEL / "concept"
CAR = (40, 60)
SKEW = np.radians(12)
COLOURS = 40

HULL, BAND, DARK, TYRE, GLASS, BOX, CRATE, CREEP, VEIN, HOLD = range(10)


def car_mats():
    return [Material((0.50, 0.52, 0.54), metal=0.45, shininess=40, spec=0.45),
            Material((0.16, 0.30, 0.62), metal=0.4, shininess=40, spec=0.45),
            Material((0.15, 0.16, 0.18), metal=0.4, shininess=30, spec=0.3),
            Material((0.07, 0.07, 0.08), metal=0.0, shininess=8, spec=0.1),
            Material((0.10, 0.16, 0.20), metal=0.8, shininess=90, spec=0.9),
            Material((0.32, 0.33, 0.28), metal=0.3, shininess=30, spec=0.3),
            Material((0.95, 0.64, 0.16), metal=0.2, shininess=40, spec=0.6, emission=(0.25, 0.14, 0.0)),
            Material((0.23, 0.15, 0.27), metal=0.1, shininess=60, spec=0.7),
            Material((0.2, 0.1, 0.3), emission=(0.75, 0.45, 1.3)),
            Material((0.03, 0.03, 0.04), metal=0.0, shininess=4, spec=0.0)]


# the six creep blobs (x, y, rx, ry, rz) in model px (y up, nose up), cleared in this order
BLOBS = [(-6.0, 10.0, 9.0, 11.0, 4.0), (7.0, -2.0, 8.0, 10.0, 4.5), (-5.0, -14.0, 9.0, 9.0, 4.0),
         (6.0, 15.0, 7.0, 8.0, 3.5), (-8.0, -1.0, 6.0, 8.0, 3.5), (4.0, -18.0, 7.0, 6.0, 3.0)]


def car_scene(state):
    """Model units = px, y up, the nose at +y; ``state`` as the frames (0..5 creep, 6 cleared, 7 open)."""
    creep = BLOBS[:max(0, 6 - state)] if state < 6 else []

    def scene(p0):
        p = rotate_z(p0, SKEW)
        q = mirror_x(p)
        hull = sd_box(p, (0.0, -1.0, 4.6), (8.0, 21.0, 2.8), 1.2)
        glacis = sd_box(rotate_x(p - np.array([0.0, 19.5, 5.6]), 0.55), (0.0, 0.0, 0.0), (7.6, 3.4, 1.0), 0.7)
        turret = sd_box(rotate_z(p - np.array([0.0, 5.0, 8.8]), 0.0), (0.0, 0.0, 0.0), (4.4, 5.0, 1.4), 1.6)
        gun = sd_box(p, (0.0, 17.0, 9.2), (0.75, 8.0, 0.75), 0.4)
        blocks = sd_box(q, (3.6, 15.0, 7.6), (1.6, 1.0, 0.6), 0.3)
        wheels = np.minimum.reduce([sd_cylinder_x(q - np.array([9.2, y, 3.4]), (0.0, 0.0, 0.0), 3.4, 1.5)
                                    for y in (-14.0, -3.0, 11.0)])
        boxes = sd_box(q, (6.0, -9.0, 7.6), (1.6, 3.6, 0.8), 0.3)
        items = [(hull, HULL), (glacis, HULL), (turret, HULL), (gun, DARK), (blocks, GLASS),
                 (wheels, TYRE), (boxes, BOX)]
        if state < 7:
            items.append((sd_box(p, (0.0, -17.0, 7.45), (5.0, 3.0, 0.3), 0.3), DARK))      # the cargo hatch
        else:
            lid = rotate_x(p - np.array([0.0, -20.0, 7.4]), 1.25)
            items.append((sd_box(lid, (0.0, 3.0, 0.0), (5.0, 3.0, 0.3), 0.3), DARK))
            items.append((sd_box(p, (0.0, -16.6, 7.35), (4.6, 2.7, 0.3), 0.1), HOLD))
            items.append((sd_box(p, (0.0, -16.2, 8.6), (3.4, 2.2, 1.6), 0.5), CRATE))
        for x, y, rx, ry, rz in creep:
            items.append((sd_ellipsoid(p, (x, y, 7.2), (rx, ry, rz * 0.6)), CREEP))
            items.append((sd_ellipsoid(p, (x * 1.25, y, 3.0), (rx * 0.5, ry * 0.8, 3.0)), CREEP))  # down the sides
        d, m = union(*items, k=0.45)
        band = (np.abs(p[:, 1] + 4.0) < 1.8) & (m == HULL)              # the CDF-blue band across the hull
        m = np.where(band, BAND, m)
        if creep:                                     # glowing veins on the creep
            vein = (np.abs(np.sin(p[:, 0] * 0.9 + np.sin(p[:, 1] * 0.45) * 2.2)) < 0.12) & (m == CREEP)
            m = np.where(vein, VEIN, m)
        return d, m
    return scene


def car_frame(state):
    hi, factor = artkit.render_hi(car_scene(state), car_mats(), CAR, float(CAR[0]), z_top=30.0, steps=170)
    arr = np.array(artkit.native(hi, factor, crisp=60)).astype(np.float64)
    if 0 < state <= 6:                                # where the hits burnt the creep off
        spots = []
        for x, y, rx, ry, rz in BLOBS[6 - state:]:
            c, s = np.cos(SKEW), np.sin(SKEW)
            sx, sy = x * c - y * s, x * s + y * c
            spots.append((CAR[0] / 2 + sx, CAR[1] / 2 - sy, 3.5))
        arr = gt.scorch(arr, spots, 3601 + state)
    return Image.fromarray(gt.light_rim(arr).astype(np.uint8), "RGBA")


def frames():
    with ProcessPoolExecutor(max_workers=8) as pool:
        out = list(pool.map(car_frame, range(8)))
    return artkit.quantize_set(out, COLOURS)


# =========================================================================== review

def bridge_plate():
    """The car's spot: the Rhone's mouth with the Pont du Mont-Blanc, in the Geneva city texture under
    the canopy (as the level's section 1)."""
    import daylight_l14
    rgb = daylight_l14.city()
    y = 388 - 90
    x = int(round(float(daylight_l14._City.shore_x(object.__new__(daylight_l14._City), np.array([388.0]))[0]))) + 9 - 112
    x = max(0, min(rgb.shape[1] - 240, x))
    return Image.fromarray(rgb[y:y + 180, x:x + 240]).convert("RGBA")


def review(fs):
    artkit.REVIEW_ROUND = REVIEW_ROUND
    plate = bridge_plate()
    scenes = []
    for k in (0, 3, 6, 7):
        img = plate.copy()
        sh = sprite.shadow_of(fs[k], opacity=0.45, blur=1.0)
        sprite.paste_center(img, sh, 112 + 3, 92 + 4)
        sprite.paste_center(img, fs[k], 112, 92)
        scenes.append(img)
    rows = [("ARMOURED CAR: 0 UNDER CREEP .. 5 ONE BLOB LEFT, 6 CLEARED, 7 HATCH OPEN WITH THE CRATE", fs, 3, False),
            ("ON THE PONT DU MONT-BLANC (FRAMES 0, 3, 6, 7; THE CITY TEXTURE OF THE CHOSEN GENEVA SCENE)", scenes, 1, False)]
    sheet = artkit.review_sheet("LEVEL 14 PROPS: THE CDF ARMOURED CAR (SECRET)", rows, width=1300, batch=BATCH)
    gif = []
    for k in list(range(8)) + [7, 7, 7]:
        img = plate.copy()
        sprite.paste_center(img, sprite.shadow_of(fs[k], opacity=0.45, blur=1.0), 115, 96)
        sprite.paste_center(img, fs[k], 112, 92)
        gif += [img.resize((480, 360), Image.NEAREST)] * 4
    artkit.save_review(sheet, gif, CONCEPT, "l14-props", fps=10)


def main(args):
    fs = frames()
    if "--assets" in args:
        artkit.write_frames("armoured-car", fs, SOURCE)
    review(fs)


if __name__ == "__main__":
    main(sys.argv[1:])
