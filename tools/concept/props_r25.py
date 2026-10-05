#!/usr/bin/env python3
"""Concept round 25 - Level 07's lifeboat tow secret, two variants (M4 part G, art step A3;
design/campaign/act-1-first-contact/level-07-brood-carrier, Secrets: "Lifeboat tow").

Lifeboat Seven is a friendly CDF lifeboat drifting down the play field on the air layer (shots,
bullets and the ship pass it) with a cargo pod hanging 70 px above (behind) it on a cable; 3 hits
on the cable cut the pod loose and it falls free as the hidden crate. Sizes from the level's data
(`tows`): boat 72x36, pod 32x32, the cable's hit box 16x40 midway between them. The game draws
them as game/.../render/TowLooks.java does: the cable centred midway, the pod, then the boat over
the cable's end; sprite names `lifeboat`, `lifeboat-pod`, `lifeboat-cable`.

Outputs (design/campaign/act-1-first-contact/level-07-brood-carrier/concept/):
  lifeboat-r25-a.png / .gif   variant A "white rescue boat": a white lifting-body lifeboat (nose
                              down, the way it drifts) with international-orange outer wings and
                              nose band, a CDF blue band, four lit cabin windows (crew of four), a
                              green strobe on a mast, red/green nav lights, a dark engine block with
                              dead nozzles and a tow eye; the pod a plain olive CDF canister with a
                              white stencil and a small hazard tag; the cable a braided steel tether
                              with three blinking amber marker lamps (the target cue sits on the
                              cable alone), one lamp going out with each hit
  lifeboat-r25-b.png / .gif   variant B "orange lifeboat capsule": a rescue-orange pressure capsule
                              lying across (white end caps and bands, a docking collar, four lit
                              portholes, two blue strobes, a tow bridle); the pod a square crate-pod
                              with an amber/black hazard lid and a light rim (it reads as loot);
                              the cable thin with an amber light strip and a hazard-striped
                              breakaway coupler in the middle, where the hits land

Each sheet: the parts at 1x and 3x on the space plate (boat with the strobe lit and dark; the pod;
the cable intact, after 1 and 2 hits, cut), the tow assembled as TowLooks draws it (intact, cut,
2x), then three play-field frames at 1x over Level 07's own production backdrop
(assets/backdrop/level-07/, composited by tools/art/backdrop_l07.py as the game's Backdrop draws
it) next to the production player ship: a vulcan burst passing over the boat, a hit on the cable
(the level's glance spark, `ballistic-impact`), the cut with the crate pickup falling free; plus a
2x crop. The GIF plays t=22.6-32.6 s of the level at real speed (12.5 fps): the strobes and marker
lamps blink, the cable glints every 2 s (the `glint` sprite), the shots pass the boat and cut the
cable.

Models: top-down SDF renders (render/sdf.py, the fixed top-left key light, 1 world unit = 1 px;
8x supersampled up to 64 px, 4x above, 32 colours), palette B's UTC hull and accents, the loot
amber with hazard stripes of ground_targets.py. Glows (strobes, lamps, the light strip) are 2D
light fields added after the sprites (premultiplied, additive), as the game would.

Run: python3 tools/concept/props_r25.py [a] [b]   (no args = both; ~30 s)
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
sys.path.insert(0, str(HERE.parent / "art"))      # backdrop_l07 (the level's production backdrop)
import ground_targets as gt  # noqa: E402  (hazard stripes, light rim, scorch)
from render import raster, sprite  # noqa: E402
from render import sdf  # noqa: E402
from render.config import ROOT  # noqa: E402
from render.enemy_rigs import write_gif  # noqa: E402
from render.palette import B  # noqa: E402
from render.sdf import (Material, panel_lines, sd_box, sd_capsule, sd_cylinder_x,  # noqa: E402
                        sd_cylinder_y, sd_cylinder_z, sd_ellipsoid, sd_sphere, subtract, union)

OUT = ROOT / "design" / "campaign" / "act-1-first-contact" / "level-07-brood-carrier" / "concept"
SPRITES = ROOT / "assets" / "sprites"
COLOURS = 32
GAP = 24
PLATE = (10, 12, 26, 255)                      # artkit.PLATE: the space plate of review sheets
FIELD_W, FIELD_H = 480, 540

# the tow (data.yaml `tows`)
T0, X0, VX, VY = 22.0, 130.0, 12.0, -45.0
BOAT, POD, CABLE = (72, 36), (32, 32), (16, 40)
TETHER = 70
CABLE_SPRITE = (16, 44)                        # the hit box plus 2 px tucked under boat and pod
HITS = 3
SHOT_SPEED = 700.0                             # scatter vulcan (design/player/weapons)
SHIP_Y = 470                                   # the ship's centre, image rows
CRATE_DRIFT = (12.0, -60.0)

HULL, DARK, ORANGE, BLUE, WARM, GLASS, BEACON, NAV_RED, NAV_GREEN = range(9)
AMBER, AMBER_FINE, OLIVE, CABLE_M, LAMP, STRIP, STEEL, VOID, CHAR, LAMP_OFF, STRIP_OFF = range(9, 20)


def fine_stripes(p, n):
    """Hazard stripes for the thin cable parts: black bands 1.5 px every 3 px along the cable."""
    return np.where((p[:, 1] / 3) % 1 < 0.5, 0.16, 1.0)


def braid(p, n):
    return np.where(((p[:, 1] * 0.8 + p[:, 0] * 1.4) % 1) < 0.38, 0.55, 1.0)


def materials(variant, beacon=True):
    beacon_col = (0.45, 1.9, 0.8) if variant == "a" else (0.5, 1.0, 2.3)
    return [
        Material((0.88, 0.9, 0.95), metal=0.25, shininess=50, spec=0.5, pattern=panel_lines(0.12, 0.05, 0.84)),
        Material(B.f("UTC HULL", 1), metal=0.6, shininess=40, spec=0.5),
        Material((0.95, 0.36, 0.1), metal=0.05, shininess=30, spec=0.35),       # rescue orange (redder than loot amber)
        Material(B.f("UTC ACCENTS", 0), metal=0.3, shininess=50, spec=0.5),     # CDF blue
        Material((0.2, 0.15, 0.08), emission=(1.5, 1.0, 0.45)),                 # cabin windows
        Material(B.f("EARTH ORBIT", 3), metal=0.9, shininess=120, spec=1.2),
        Material((0.1, 0.2, 0.2), emission=beacon_col) if beacon
        else Material((0.14, 0.2, 0.22), metal=0.4, shininess=90, spec=0.9),     # strobe
        Material((0.2, 0.04, 0.04), emission=(1.9, 0.2, 0.2)),
        Material((0.04, 0.2, 0.08), emission=(0.3, 1.7, 0.5)),
        Material((0.98, 0.66, 0.14), metal=0.0, shininess=8, spec=0.05, pattern=gt.hazard_stripes),
        Material((0.98, 0.66, 0.14), metal=0.0, shininess=8, spec=0.05, pattern=fine_stripes),
        Material((0.42, 0.45, 0.35), metal=0.2, shininess=25, spec=0.3),        # CDF olive (lifted for space)
        Material((0.34, 0.34, 0.38), metal=0.7, shininess=60, spec=0.6, pattern=braid),
        Material((0.25, 0.15, 0.02), emission=(2.0, 0.95, 0.15)),               # amber marker lamp
        Material((0.25, 0.15, 0.02), emission=(1.7, 0.8, 0.1)),                 # amber light strip
        Material((0.85, 0.85, 0.88), metal=0.9, shininess=120, spec=1.3),       # bright frayed strands
        Material((0.03, 0.03, 0.04), shininess=4, spec=0.0),
        Material((0.09, 0.08, 0.08), metal=0.1, shininess=10, spec=0.1),
        Material((0.3, 0.2, 0.08), metal=0.2, shininess=40, spec=0.4),         # a dead marker lamp
        Material((0.18, 0.12, 0.05), metal=0.3, shininess=30, spec=0.3),       # the dead strip
    ]


def render(scene, mats, size):
    w, h = size
    factor = 8 if max(w, h) <= 64 else 4
    hi = sdf.render(scene, mats, (w * factor, h * factor), float(w), z_top=float(max(w, h)), steps=160)
    return np.array(sprite.make_sprite(hi, factor, COLOURS, crisp=60)).astype(np.float64)


def img(arr):
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")


def ring(p, c, r_out, r_in, h):
    return np.maximum(sd_cylinder_z(p, c, r_out, h), -sd_cylinder_z(p, c, r_in, h + 2))


# --------------------------------------------------------------------------- variant A

def boat_a():
    """White lifting-body lifeboat, nose down (-y): a cabin body on a wide wing, orange outer wings
    and a nose chevron, a CDF blue band, four lit windows, a green strobe on a mast, red/green nav
    lights, a dark engine block aft (+y) with two dead nozzles and the tow eye."""
    def scene(p):
        x, y = p[:, 0], p[:, 1]
        body = sd_ellipsoid(p, (0, -2.0, 0.5), (20, 15.5, 8))
        wing = sd_ellipsoid(p, (0, 2.5, -0.5), (35.5, 12.5, 3.8))
        d, _ = union((body, HULL), (wing, HULL), k=3.0)
        v = y + 0.42 * np.abs(x)
        paint = np.where(np.abs(x) > 25.0, ORANGE, HULL)
        paint = np.where((v > -14.0) & (v < -10.8), ORANGE, paint)
        paint = np.where(np.abs(y - 1.5) < 1.2, BLUE, paint)
        items = [(d, paint),
                 (sd_ellipsoid(p, (0, -6.0, 6.0), (11.0, 2.8, 2.2)), GLASS)]
        items += [(sd_box(p, (wx, -6.2, 7.4), (1.7, 1.3, 0.8), 0.4), WARM) for wx in (-7.5, -2.5, 2.5, 7.5)]
        items += [(sd_cylinder_z(p, (0, 6.0, 8.0), 1.1, 1.2), DARK), (sd_sphere(p, (0, 6.0, 9.4), 1.9), BEACON)]
        items += [(sd_sphere(p, (-34.0, 3.5, 0.6), 1.4), NAV_GREEN), (sd_sphere(p, (34.0, 3.5, 0.6), 1.4), NAV_RED)]
        items += [(sd_box(p, (sx * 21.0, -1.5, 3.0), (1.4, 1.4, 0.9), 0.3), DARK) for sx in (-1, 1)]   # RCS quads
        items.append((sd_box(p, (0, 13.0, 1.5), (13.0, 3.6, 3.2), 1.6), DARK))                         # engine block
        for sx in (-1, 1):
            items.append((subtract((sd_capsule(p, (sx * 7.5, 14.0, 1.0), (sx * 7.5, 17.0, 1.0), 2.6, 3.0), DARK),
                                   sd_sphere(p, (sx * 7.5, 18.0, 1.0), 2.2))[0], VOID))
        items.append((ring(p, (0, 15.6, 4.8), 1.9, 1.0, 0.7), STEEL))                                  # tow eye
        return union(*items, k=0.3)
    return scene


def pod_a():
    """Plain olive CDF cargo canister lying along the tow: dark bands, a white stencil plate, a
    small hazard tag, end lugs; the tow eye at its foot (-y), toward the boat."""
    def scene(p):
        items = [(sd_capsule(p, (0, -3.5, 0), (0, 5.0, 0), 11.0), OLIVE)]
        items += [(sd_cylinder_y(p, (0, by, 0), 11.5, 0.8), DARK) for by in (-2.0, 3.5)]
        items += [(sd_box(p, (0, 0.8, 10.6), (4.5, 1.6, 0.6), 0.3), HULL),
                  (sd_box(p, (5.5, 8.5, 7.0), (2.6, 1.4, 0.6), 0.3), AMBER)]
        items += [(sd_box(p, (sx * 10.5, 9.0, 0), (1.5, 2.0, 2.0), 0.4), DARK) for sx in (-1, 1)]
        items += [(sd_box(p, (0, -13.2, 1.0), (2.4, 1.8, 1.6), 0.5), DARK),
                  (ring(p, (0, -14.6, 1.6), 1.6, 0.8, 0.6), STEEL)]
        return union(*items, k=0.3)
    return scene


LIT_LAMPS = {0: (-11, 0, 11), 1: (-11, 11), 2: (-11,), 3: ()}   # a lamp goes out with each hit


def cable_a(hits):
    """Braided steel tether with three amber marker lamps (y -11, 0, 11) and short hazard sleeves.
    Each hit puts a lamp out (the count-down) and frays strands at y 4 (necked after 2); after the
    third the cut stub hangs from the boat, its lamp dead."""
    def scene(p):
        if hits >= HITS:
            items = [(sd_capsule(p, (0, -24, 0), (0, -7.0, 0), 1.7), CABLE_M),
                     (sd_box(p, (0, -11, 0.5), (2.6, 2.0, 1.6), 0.6), DARK),
                     (sd_sphere(p, (0, -11, 2.0), 1.5), LAMP_OFF),
                     (sd_capsule(p, (0, -18.5, 0), (0, -15.5, 0), 2.1), AMBER_FINE)]
            for ang, ln in ((-0.6, 4.2), (-0.15, 5.0), (0.3, 4.6), (0.75, 3.6)):
                items.append((sd_capsule(p, (0, -7.5, 0.4), (np.sin(ang) * ln, -7.5 + np.cos(ang) * ln, 0.8), 0.45), STEEL))
            return union(*items, k=0.2)
        neck = 1.7 if hits < 2 else 1.0
        d_cable = np.minimum(sd_capsule(p, (0, -24, 0), (0, 2.5, 0), 1.7), sd_capsule(p, (0, 5.5, 0), (0, 24, 0), 1.7))
        d_cable = np.minimum(d_cable, sd_capsule(p, (0, 2.0, 0), (0, 6.0, 0), neck))
        items = [(d_cable, CABLE_M)]
        for ly in (-11, 0, 11):
            lit = LAMP if ly in LIT_LAMPS[hits] else LAMP_OFF
            items += [(sd_box(p, (0, ly, 0.5), (2.6, 2.0, 1.6), 0.6), DARK), (sd_sphere(p, (0, ly, 2.0), 1.5), lit)]
        items += [(sd_capsule(p, (0, sy - 1.5, 0), (0, sy + 1.5, 0), 2.1), AMBER_FINE) for sy in (-17.0, 17.0)]
        if hits >= 1:
            for ang, ln in ((-1.1, 3.2), (1.0, 2.8))[:1 + hits]:
                items.append((sd_capsule(p, (0, 4.0, 0.6), (np.sin(ang) * ln, 4.0 + np.cos(ang) * ln * 0.6, 1.0), 0.45), STEEL))
        if hits >= 2:
            items += [(sd_capsule(p, (0, 4.5, 0.6), (-3.4, 2.2, 1.0), 0.45), STEEL),
                      (sd_capsule(p, (0, 3.5, 0.6), (3.6, 5.6, 1.0), 0.45), STEEL)]
        return union(*items, k=0.2)
    return scene


# --------------------------------------------------------------------------- variant B

def boat_b():
    """Rescue-orange pressure capsule lying across: white end caps and bands, a CDF blue band, a
    docking collar (left), a thruster bell (right), four lit portholes, two blue strobes, handrails,
    a radiator fin below and a tow bridle from two hard points aft (+y)."""
    def scene(p):
        x = p[:, 0]
        d = sd_capsule(p, (-21.0, 0, 0), (21.0, 0, 0), 12.5)
        paint = np.where(np.abs(x) > 19.5, HULL, ORANGE)
        paint = np.where(np.abs(np.abs(x) - 9.5) < 1.1, HULL, paint)
        paint = np.where(np.abs(x) < 1.2, BLUE, paint)
        items = [(d, paint),
                 (sd_cylinder_x(p, (-34.0, 0, 0), 7.5, 1.6), DARK),
                 (np.maximum(sd_cylinder_x(p, (-35.2, 0, 0), 5.6, 0.8), -sd_cylinder_x(p, (-35.2, 0, 0), 4.0, 2)), STEEL),
                 (subtract((sd_capsule(p, (32.0, 0, 0), (35.6, 0, 0), 4.0, 5.5), DARK),
                           sd_sphere(p, (37.0, 0, 0), 4.3))[0], DARK),
                 (sd_box(p, (0, -15.0, -3.0), (15.0, 2.6, 0.5), 0.3), GLASS)]
        for wx in (-14.0, -5.0, 5.0, 14.0):
            items += [(ring(p, (wx, -3.5, 11.6), 2.5, 1.6, 0.6), DARK), (sd_cylinder_z(p, (wx, -3.5, 11.6), 1.7, 0.7), WARM)]
        for sx in (-1, 1):
            items += [(sd_cylinder_z(p, (sx * 15.0, 5.0, 11.0), 2.2, 0.6), DARK), (sd_sphere(p, (sx * 15.0, 5.0, 11.6), 1.6), BEACON)]
            items += [(sd_box(p, (sx * 10.0, 11.5, 4.5), (1.8, 1.6, 1.4), 0.4), DARK),
                      (sd_capsule(p, (sx * 10.0, 12.5, 5.2), (sx * 0.8, 16.6, 4.0), 0.6), CABLE_M),
                      (sd_box(p, (sx * 25.0, 6.0, 6.5), (1.2, 1.2, 0.8), 0.3), DARK)]
        items += [(sd_capsule(p, (-14.0, 8.0, 10.6), (14.0, 8.0, 10.6), 0.5), STEEL),
                  (ring(p, (0, 16.4, 4.0), 1.8, 0.9, 0.7), STEEL)]
        return union(*items, k=0.3)
    return scene


def pod_b():
    """Square crate-pod: olive body, amber/black hazard lid, dark corner posts, a bridle from the
    two lower corners to the tow ring at its foot (-y)."""
    def scene(p):
        items = [(sd_box(p, (0, 1.5, 0), (12.5, 12.5, 6.0), 1.5), OLIVE),
                 (sd_box(p, (0, 1.5, 6.3), (9.0, 9.0, 0.6), 0.4), AMBER),
                 (sd_box(p, (0, 1.5, 7.0), (2.5, 9.2, 0.3), 0.2), DARK)]
        for sx in (-1, 1):
            for sy in (-1, 1):
                items.append((sd_box(p, (sx * 11.5, 1.5 + sy * 11.5, 0.8), (2.4, 2.4, 6.6), 0.5), DARK))
            items.append((sd_capsule(p, (sx * 9.5, -9.5, 4.0), (sx * 0.8, -14.2, 2.0), 0.6), CABLE_M))
        items.append((ring(p, (0, -14.3, 2.0), 1.7, 0.9, 0.6), STEEL))
        return union(*items, k=0.3)
    return scene


def cable_b(hits):
    """Thin cable with an amber light strip, a hazard-striped breakaway coupler in the middle (the
    hit box). Hit 1: the coupler dented, the strip above it dark; hit 2: the coupler split open at
    one side; hit 3: the lower half with the opened jaw hanging from the boat, the strip dark."""
    def scene(p):
        if hits >= HITS:
            items = [(sd_capsule(p, (0, -24, 0), (0, -5.0, 0), 1.2), CABLE_M),
                     (sd_box(p, (0, -21, 1.0), (0.5, 14.0, 0.45), 0.2), STRIP_OFF),
                     (sd_box(p, (0, -3.2, 0.5), (4.5, 3.2, 2.4), 1.0), AMBER),
                     (sd_box(p, (-2.6, 0.6, 0.8), (1.0, 1.6, 1.5), 0.4), DARK),
                     (sd_box(p, (2.8, 0.2, 0.8), (1.0, 1.4, 1.5), 0.4), DARK)]
            return union(*items, k=0.2)
        items = [(sd_capsule(p, (0, -24, 0), (0, 24, 0), 1.2), CABLE_M),
                 (sd_box(p, (0, -15.5, 1.0), (0.5, 9.0, 0.45), 0.2), STRIP),
                 (sd_box(p, (0, 15.5, 1.0), (0.5, 9.0, 0.45), 0.2), STRIP if hits == 0 else STRIP_OFF)]
        coupler = sd_box(p, (0, 0, 0.5), (4.5, 6.5, 2.4), 1.0)
        if hits >= 1:
            coupler = np.maximum(coupler, -sd_sphere(p, (-2.0, 3.5, 3.6), 1.8))
        if hits >= 2:
            coupler = np.maximum(coupler, -sd_box(p, (3.5, 0.5, 2.0), (2.2, 0.8, 4.0), 0.2))
        items += [(coupler, AMBER), (sd_box(p, (0, 0, 2.9), (4.7, 0.6, 0.3), 0.2), DARK)]
        items += [(sd_sphere(p, (sx * 2.6, sy * 4.0, 2.8), 0.8), DARK) for sx in (-1, 1) for sy in (-1, 1)]
        return union(*items, k=0.2)
    return scene


# --------------------------------------------------------------------------- the parts

VARIANTS = {
    "a": {"boat": boat_a, "pod": pod_a, "cable": cable_a, "pod_rim": False,
          "beacons": [(0, 6.0)], "beacon": (0.35, 1.0, 0.55),
          "title": "LIFEBOAT SEVEN - ROUND 25 VARIANT A",
          "desc": ["WHITE RESCUE BOAT: A WHITE LIFTING BODY, NOSE DOWN, ORANGE OUTER WINGS AND NOSE BAND, CDF BLUE BAND, FOUR LIT WINDOWS,",
                   "A GREEN STROBE, RED/GREEN NAV LIGHTS, DEAD ENGINES. POD: A PLAIN OLIVE CDF CANISTER (NOT A TARGET). CABLE: A BRAIDED TETHER",
                   "WITH THREE BLINKING AMBER MARKER LAMPS - THE ONLY TARGET CUE; EACH HIT PUTS A LAMP OUT AND FRAYS IT, THE CUT STUB HANGS FROM THE BOAT."]},
    "b": {"boat": boat_b, "pod": pod_b, "cable": cable_b, "pod_rim": True,
          "beacons": [(-15.0, 5.0), (15.0, 5.0)], "beacon": (0.4, 0.75, 1.0),
          "title": "LIFEBOAT SEVEN - ROUND 25 VARIANT B",
          "desc": ["ORANGE LIFEBOAT CAPSULE: A RESCUE-ORANGE PRESSURE CAPSULE LYING ACROSS, WHITE END CAPS AND BANDS, DOCKING COLLAR, FOUR",
                   "LIT PORTHOLES, TWO BLUE STROBES, A TOW BRIDLE. POD: A CRATE-POD WITH AN AMBER/BLACK HAZARD LID AND A LIGHT RIM (READS AS",
                   "LOOT). CABLE: AN AMBER LIGHT STRIP AND A HAZARD-STRIPED BREAKAWAY COUPLER IN THE MIDDLE, WHERE THE HITS LAND; IT SPLITS."]},
}


def parts(variant):
    v = VARIANTS[variant]
    boat_on = img(render(v["boat"](), materials(variant), BOAT))
    boat_off = img(render(v["boat"](), materials(variant, beacon=False), BOAT))
    pod = render(v["pod"](), materials(variant), POD)
    pod = img(gt.light_rim(pod) if v["pod_rim"] else pod)
    cables = []
    for hits in range(HITS + 1):
        arr = render(v["cable"](hits), materials(variant), CABLE_SPRITE)
        if 1 <= hits < HITS:
            spots = [(8, 22 - (4 if variant == "a" else 0) + k * 2.5, 2.4 + hits * 0.5) for k in range(hits)]
            arr = gt.scorch(arr, spots, 250 + hits)
        cables.append(img(arr))
    return {"boat": [boat_on, boat_off], "pod": pod, "cable": cables}


# --------------------------------------------------------------------------- glows (additive)

def field(w, h, fn, ss=4):
    yy, xx = np.mgrid[0:h * ss, 0:w * ss].astype(np.float64)
    rgb = fn((xx + 0.5) / ss, (yy + 0.5) / ss)
    rgb = rgb.reshape(h, ss, w, ss, 3).mean(axis=(1, 3))
    return np.clip(rgb, 0, 1) * 255


def steps(v, levels=(0.15, 0.3, 0.55)):
    out = np.zeros_like(v)
    for lv in levels:
        out = np.where(v >= lv, lv, out)
    return np.where(v >= 0.8, v, out)


def halo(r, col, strength=1.0):
    n = 2 * r

    def fn(x, y):
        d = np.hypot(x - r, y - r)
        t = np.maximum(np.clip(1.6 - d, 0, 1), steps(np.clip(1 - d / r, 0, 1) * 0.6)) * strength
        return np.array(col) * t[..., None]
    return field(n, n, fn)


def strip_glow(strength, upper=True):
    """Variant B's light strip: a soft amber line along the cable outside the coupler (the part
    above it only while it is unhit)."""
    w, h = CABLE_SPRITE

    def fn(x, y):
        d = np.abs(x - w / 2)
        along = np.where((y - h / 2 > 7.5) | ((h / 2 - y > 7.5) & upper), 1.0, 0.0)
        t = steps(np.clip(1 - d / 4.5, 0, 1) * 0.5) * along * strength
        return np.array([1.0, 0.62, 0.15]) * t[..., None]
    return field(w, h, fn)


def add(base, glow, x, y):
    h, w = glow.shape[:2]
    H, W = base.shape[:2]
    x, y = int(round(x)), int(round(y))
    x0, y0, x1, y1 = max(0, x), max(0, y), min(W, x + w), min(H, y + h)
    if x1 > x0 and y1 > y0:
        base[y0:y1, x0:x1, :3] = np.clip(base[y0:y1, x0:x1, :3] + glow[y0 - y:y1 - y, x0 - x:x1 - x, :3], 0, 255)


def over(base, spr, x, y):
    out = img(base)
    sprite.paste(out, spr, int(round(x)), int(round(y)))
    return np.array(out).astype(np.float64)


# --------------------------------------------------------------------------- the play field

def tow_at(t):
    """The boat's centre at level time t, image coordinates (rows down), as Tow.update places it."""
    s = max(0.0, t - T0)
    x = X0 + VX * s
    y_up = FIELD_H + BOAT[1] / 2 + VY * s
    return x, FIELD_H - y_up


def ship_x(t):
    """The illustration's ship: under the boat's right wing (the burst passes the boat), then
    sliding under the cable for three single shots."""
    bx, _ = tow_at(t)
    if t < 25.4:
        return bx + 24
    if t < 26.2:
        return bx + 24 * (1 - (t - 25.4) / 0.8)
    return bx


SHOT_TIMES = [24.0 + 0.08 * k for k in range(13)] + [26.6, 27.4, 28.2]


def simulate():
    """Fly every shot up at SHOT_SPEED; return [(t0, x, t_end, hit)] and the hit times."""
    shots, hits = [], []
    for t0 in SHOT_TIMES:
        x = ship_x(t0)
        y0 = SHIP_Y - 24
        t_end, hit = t0 + (y0 + 20) / SHOT_SPEED, False
        if len(hits) < HITS:
            for k in range(1, 400):
                t = t0 + k / 240
                y = y0 - SHOT_SPEED * (t - t0)
                if y < -20:
                    break
                bx, by = tow_at(t)
                cy = by - TETHER / 2
                if abs(x - bx) < CABLE[0] / 2 + 3 and abs(y - cy) < CABLE[1] / 2 + 8:
                    t_end, hit = t, True
                    hits.append((t, x, y))
                    break
        shots.append((t0, x, t_end, hit))
    return shots, hits


class Scene:
    def __init__(self, variant):
        import backdrop_l07 as bl                   # the level's production backdrop (frozen kit)
        self.bl = bl
        self.backdrop, source = bl.level_backdrop()
        if source == "data.yaml":
            bl.SECTION_OF = [s["tiles"] for s in bl.SECTIONS]
        self.variant = variant
        self.v = VARIANTS[variant]
        self.parts = parts(variant)
        load = lambda n: Image.open(SPRITES / f"{n}.png").convert("RGBA")   # noqa: E731
        self.ship = load("ship_2")
        self.flame = [load(f"engine-flame_{i}") for i in range(9)]
        self.shot = load("scatter-vulcan-shot_0")
        self.spark = [load(f"ballistic-impact_{i}") for i in range(4)]
        self.crate = [load(f"pickup-crate_{i}") for i in range(8)]
        self.glint = [load(f"glint_{i}") for i in range(3)]
        self.shots, self.hits = simulate()
        self.cut = self.hits[-1][0] if len(self.hits) >= HITS else None
        self.beacon = halo(6, self.v["beacon"])
        self.lamp = halo(5, (1.0, 0.62, 0.15))

    def over_boat(self, t_from, t_to):
        """A time in [t_from, t_to] when a missing shot crosses the boat's middle."""
        best = (1e9, t_from)
        for t in np.arange(t_from, t_to, 0.01):
            bx, by = tow_at(t)
            for t0, x, t_end, hit in self.shots:
                if not hit and t0 <= t < t_end:
                    best = min(best, (abs(SHIP_Y - 24 - SHOT_SPEED * (t - t0) - by), float(t)))
        return best[1]

    def hits_by(self, t):
        return sum(1 for h in self.hits if h[0] <= t)

    def frame(self, t):
        base = np.array(self.bl.composite(self.backdrop, t).convert("RGBA")).astype(np.float64)
        bx, by = tow_at(t)
        hits = self.hits_by(t)
        holding = hits < HITS
        cw, ch = CABLE_SPRITE
        cx, cy = bx, by - TETHER / 2
        base = over(base, self.parts["cable"][min(hits, HITS)], cx - cw / 2, cy - ch / 2)
        if holding:
            base = over(base, self.parts["pod"], bx - POD[0] / 2, by - TETHER - POD[1] / 2)
        strobe = (t % 1.0) < 0.16
        base = over(base, self.parts["boat"][0 if strobe else 1], bx - BOAT[0] / 2, by - BOAT[1] / 2)
        if strobe:
            for ox, oy in self.v["beacons"]:
                add(base, self.beacon, bx + ox - 6, by - oy - 6)
        if holding:                                               # the target cue blinks
            if self.variant == "a":
                if (t * 2.5) % 1 < 0.6:
                    for oy in LIT_LAMPS[hits]:
                        add(base, self.lamp, cx - 5, cy - oy - 5)
            else:
                add(base, strip_glow(0.55 + 0.45 * np.sin(t * 2 * np.pi * 1.2), hits == 0), cx - cw / 2, cy - ch / 2)
            g = (t - T0) % 2.0
            if g < 0.24:
                gl = self.glint[min(2, int(g / 0.08))]
                base = additive(base, gl, cx - 4 + 3, cy - 4 - 9)
        if self.cut is not None and t >= self.cut:                # the crate falls free
            s = t - self.cut
            px, py = tow_at(self.cut)
            px, py = px + CRATE_DRIFT[0] * s, py - TETHER - CRATE_DRIFT[1] * s
            base = over(base, self.crate[int(t * 10) % 8], px - 17, py - 17)
        for t0, x, t_end, _ in self.shots:                       # player shots, over the tow
            if t0 <= t < t_end:
                y = SHIP_Y - 24 - SHOT_SPEED * (t - t0)
                base = additive(base, self.shot, x - 4, y - 10)
        for th, x, y in self.hits:                                # the glance spark on each hit
            if th <= t < th + 0.2:
                sp = self.spark[min(3, int((t - th) / 0.05))]
                base = additive(base, sp, x - 11, y - 11)
        sx = ship_x(t)
        base = additive(base, self.flame[int(t * 20) % 9], sx - 6, SHIP_Y + 18)
        base = over(base, self.ship, sx - 24, SHIP_Y - 24)
        return img(base).convert("RGB")


def additive(base, spr, x, y):
    """Additive sprites (shots, sparks, glints, flames) are premultiplied on black in assets/."""
    a = np.array(spr).astype(np.float64)
    glow = a[..., :3] * (a[..., 3:4] / 255)
    add(base, glow, x, y)
    return base


# --------------------------------------------------------------------------- sheets

def plate_strip(frames, scale=1):
    w = sum(f.width + GAP for f in frames)
    h = max(f.height for f in frames) + GAP
    out = Image.new("RGBA", (w, h), PLATE)
    x = 0
    for f in frames:
        out.alpha_composite(f, (x + GAP // 2, (h - f.height) // 2))
        x += f.width + GAP
    return sprite.enlarge(out, scale) if scale > 1 else out


def assembled(p, hits):
    """The tow as TowLooks lays it out: cable midway, pod, boat over the cable's end."""
    w, h = BOAT[0] + 8, BOAT[1] // 2 + TETHER + POD[1] // 2 + 8
    out = Image.new("RGBA", (w, h), PLATE)
    bx, by = w // 2, h - 4 - BOAT[1] // 2
    cab = p["cable"][hits]
    out.alpha_composite(cab, (bx - cab.width // 2, by - TETHER // 2 - cab.height // 2))
    if hits < HITS:
        out.alpha_composite(p["pod"], (bx - POD[0] // 2, by - TETHER - POD[1] // 2))
    out.alpha_composite(p["boat"][0], (bx - BOAT[0] // 2, by - BOAT[1] // 2))
    return out


def sheet(variant):
    sc = Scene(variant)
    v, p = sc.v, sc.parts
    frames = p["boat"] + [p["pod"]] + p["cable"]
    labels = ["BOAT 72X36", "STROBE DARK", "POD 32X32", "CABLE 16X44", "1 HIT", "2 HITS", "CUT"]
    small = plate_strip(frames)
    big = plate_strip(frames, 3)
    asm = [sprite.enlarge(assembled(p, 0), 2), sprite.enlarge(assembled(p, HITS), 2)]
    t_hit = sc.hits[0][0] + 0.03
    t_burst = sc.over_boat(24.5, 25.2)
    times = [t_burst, t_hit, sc.cut + 0.9]
    shots = [sc.frame(t) for t in times]
    t_pass = sc.hits[1][0] - 0.03                     # the second hit's shot over the boat
    bx, by = tow_at(t_pass)
    crop = sc.frame(t_pass).crop((int(bx) - 60, int(by) - 90, int(bx) + 60, int(by) + 180))
    crop = crop.resize((240, 540), Image.NEAREST)
    width = 16 + 3 * (FIELD_W + 12) + crop.width + 16
    height = 74 + small.height + 24 + big.height + 30 + asm[0].height + 30 + FIELD_H + 40
    out = raster.sheet(width, height, v["title"], "LEVEL 07 SECRET PROP CONCEPT - FRIENDLY, SHOTS PASS THE BOAT; 3 HITS ON THE CABLE")
    for k, line in enumerate(v["desc"]):
        raster.draw_text(out, 16, 34 + 12 * k, line, raster.LABEL if k == 0 else raster.LABEL_DIM)
    y = 74
    raster.draw_text(out, 16, y, "1X", raster.LABEL_DIM)
    out.alpha_composite(small, (40, y))
    y += small.height + 10
    raster.draw_text(out, 16, y, "3X ON THE SPACE PLATE", raster.LABEL_DIM)
    out.alpha_composite(big, (16, y + 12))
    x = 16
    for f, name in zip(frames, labels):
        raster.draw_text(out, x + 4, y + 16 + big.height, name, raster.LABEL)
        x += (f.width + GAP) * 3
    y += 12 + big.height + 22
    raster.draw_text(out, 16, y, "ASSEMBLED AS TOWLOOKS DRAWS IT (CABLE, POD, BOAT), 2X: INTACT, CUT (THE POD FALLS FREE AS THE CRATE)",
                     raster.LABEL_DIM)
    for k, a in enumerate(asm):
        out.alpha_composite(a, (16 + k * (a.width + 16), y + 12))
    note_x = 16 + 2 * (asm[0].width + 16) + 8
    notes = ["FRAMES: LIFEBOAT 2 (STROBE LIT, DARK), LIFEBOAT-POD 1, LIFEBOAT-CABLE 4 (INTACT, 1 HIT, 2 HITS, CUT).",
             "TOWLOOKS TODAY DRAWS CABLE FRAMES 0 AND 1 ONLY: THE HIT STATES NEED IT TO PICK THE FRAME BY HITS TAKEN.",
             "STROBES" + (", MARKER LAMPS" if variant == "a" else ", THE LIGHT STRIP") + " AND THE GLINT ARE ADDITIVE GLOWS ADDED AFTER THE SPRITES.",
             "NO SHADOW: AIR LAYER IN ORBIT, LIKE THE SHIP. DRAWN UNDER THE FLYERS AND THE SHOTS."]
    for k, line in enumerate(notes):
        raster.draw_text(out, note_x, y + 20 + 14 * k, line, raster.LABEL_DIM)
    y += 12 + asm[0].height + 18
    raster.draw_text(out, 16, y, "PLAY FIELD 1X OVER LEVEL 07'S BACKDROP WITH THE PLAYER SHIP: "
                     f"T={t_burst:.1f} A BURST PASSES THE BOAT'S WING, "
                     f"T={t_hit:.1f} FIRST HIT (SPARK), T={sc.cut + 0.9:.1f} CUT, THE CRATE FALLS FREE; 2X: A SHOT CROSSING THE BOAT",
                     raster.LABEL_DIM)
    for k, s in enumerate(shots):
        out.paste(s, (16 + k * (FIELD_W + 12), y + 12))
    out.paste(crop, (16 + 3 * (FIELD_W + 12), y + 12))
    path = OUT / f"lifeboat-r25-{variant}.png"
    out.convert("RGB").save(path, optimize=True)
    print(path.relative_to(ROOT))
    gif_frames = []
    for t in np.arange(22.6, 32.6, 0.08):
        gif_frames.append(sc.frame(float(t)).crop((30, 0, 390, FIELD_H)))
    gif = OUT / f"lifeboat-r25-{variant}.gif"
    write_gif(gif_frames, gif, fps=12.5)
    print(gif.relative_to(ROOT), f"{gif.stat().st_size / 1e6:.1f} MB")


def main(args):
    OUT.mkdir(parents=True, exist_ok=True)
    for variant in args or ["a", "b"]:
        sheet(variant)


if __name__ == "__main__":
    main(sys.argv[1:])
