#!/usr/bin/env python3
"""Production art: the Level 03 backdrop, the high orbital lanes over the wreckage of the first
battle (design/campaign/act-1-first-contact/level-03-spore-drift; M4 part C batch).

Outputs (assets/backdrop/level-03/, one PNG per tile set and set piece of the level's `backdrop`
block, named by its id and checked against its size):
  earth, weather-front-a, weather-front-b       deep: Level 01's day side, two cloud fronts over it
                                                (r03's weather, as translucent overlays)
  nav-beacon, defence-ring                      far: the lane beacons, the orbital defence ring ahead
  lane-buoy, kestrel, platform-half-a/-b,       ground: marker buoys, the broken frigate Kestrel
  wreck-a/-b/-c, tug-burnt, platform, crossbeam with its lifeboat rack, halves of CDF platforms,
                                                wreck plates, a burnt-out tug; Level 01's platform
                                                and crossbeam for the defence ring
  banks-light, spore-haze, spore-banks          low-air: Level 01's thin cloud decks, the olive-grey
                                                spore haze and the dense spore banks of the peak
  wisps, ice-streaks, spore-streaks             high-air (additive): Level 01's wisps, Level 02's
                                                frost streaks, pale spore streaks
  lifeboat-light                                12x12 amber light (additive), not a set piece: the
                                                game draws it over the Kestrel's four lenses for the
                                                lifeboat rack's triggers and turns it off when hit
  design/campaign/.../level-03-spore-drift/concept/backdrop-final-r16-a.png   review sheet

The backdrop block comes from the level's data.yaml once it has one, until then from
backdrop-proposal.yaml next to it. Level 01's and Level 02's production pieces
(tools/art/backdrop_l01.py, backdrop_l02.py, loaded by path: the concept generators have the same
module names) render the shared kit unchanged; the new pieces use the same kit, palette B, the
top-left key light and production bar (posterized to 12-32 colours, translucency stepped, wide
gradients ordered-dithered). Nothing lit is rotated or mirrored as an image: the wrecks are turned
in the model before they are lit.

Run: python3 tools/art/backdrop_l03.py [--review] [id ...]   (~1 min on 20 cores)
"""
import importlib.util
import sys
from concurrent.futures import ProcessPoolExecutor
from pathlib import Path

import numpy as np
import yaml
from PIL import Image

import artkit
from artkit import DESIGN, ROOT, raster, sprite

from parallax_r02 import haze  # noqa: E402  (concept script, imported unchanged)
from render.sdf import (rotate_z, sd_box, sd_capsule, sd_cylinder_y, sd_cylinder_z, sd_ellipsoid,  # noqa: E402
                        sd_plate, sd_polygon2, sd_sphere, union)
from render.station import ACCENT, DARK, GLASS, HULL, RED, SOLAR  # noqa: E402


def _load(name, file):
    spec = importlib.util.spec_from_file_location(name, Path(__file__).resolve().parent / file)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


b1 = _load("art_backdrop_l01", "backdrop_l01.py")
b2 = _load("art_backdrop_l02", "backdrop_l02.py")
l01 = b1.l01
SEA = l01.SEA

SCRIPT = "backdrop_l03.py"
SOURCE = artkit.source_note(SCRIPT, "M4 part C batch")
LEVEL_DIR = DESIGN / "campaign" / "act-1-first-contact" / "level-03-spore-drift"
OUT = ROOT / "assets" / "backdrop" / "level-03"
COLOURS = 32
SPORE = np.array([146, 150, 122], float)          # olive grey
SPORE_DENSE = np.array([128, 132, 98], float)
RECEDE = 0.22                                     # the wrecks hazed towards the orbit blue: below the air layer's debris
SCORCH = 0.74                                     # the wrecks' burnt hull


# --------------------------------------------------------------------------- helpers

def scorched(img, seed, amount=0.45, base=SCORCH):
    """A wreck's colours darkened overall and in sooty blotches (fbm), alpha kept."""
    a = np.array(img.convert("RGBA")).astype(np.float64)
    h, w = a.shape[:2]
    soot = np.clip((raster.fbm(w, h, 18, seed, octaves=4, period=False) - 0.45) * 2.2, 0, 1)
    a[..., :3] *= (base * (1 - amount * soot))[..., None]
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


def receded(img):
    """A ground wreck pushed back under the play plane (the debris chunks on air stay brighter)."""
    return haze(img, SEA[2], RECEDE)


def centred(img, w, h):
    """The opaque part of a render, cropped and centred on a (w, h) canvas (checked to fit)."""
    box = img.getchannel("A").getbbox()
    crop = img.crop(box)
    if crop.width > w or crop.height > h:
        raise ValueError(f"the render's opaque part is {crop.size}, the piece is {(w, h)}")
    out = l01.blank(w, h)
    out.alpha_composite(crop, ((w - crop.width) // 2, (h - crop.height) // 2))
    return out


def edge_fade(w, h, margin):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64)
    return np.clip((np.minimum.reduce([xx, yy, w - 1 - xx, h - 1 - yy]) - 2) / margin, 0, 1)


# --------------------------------------------------------------------------- deep

def weather_front(w, h, seed, cyclone, slope):
    """A cloud front over the earth tiles: r03's weather system (a frontal band of fbm cloud, its
    shadow offset down-right, optionally a small cyclone) as a translucent overlay in the earth
    tile's cloud colour, the alpha ordered-dithered and faded out at the borders so no edge shows."""
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64)
    rng = np.random.default_rng(seed)
    phase = rng.uniform(0, 2 * np.pi)
    centre = w * 0.5 + w * 0.12 * np.sin(yy / h * np.pi * 0.9 + phase) + (yy - h / 2) * slope
    width = w * (0.08 + 0.07 * raster.fbm(w, h, 160, seed + 1, octaves=2, period=False))
    band = np.exp(-((xx - centre) / width) ** 2)
    tex = raster.fbm(w, h, 32, seed, octaves=5, period=False)
    c = np.clip((band * 0.9 + tex * 0.6 - 0.8) * 3.0, 0, 1)
    if cyclone:
        cx, cy = w * 0.72, h * 0.3
        r = np.hypot(xx - cx, yy - cy) + 1e-6
        ang = np.arctan2(yy - cy, xx - cx)
        swirl = np.sin(ang * 2 + np.log(r) * 3.6) * 0.5 + 0.5
        fall = np.clip(1 - r / 110, 0, 1)
        eye = np.clip((r - 4) / 8, 0, 1)
        c = np.maximum(c, np.clip((swirl * 0.75 + tex * 0.45 - 0.5) * 3.0, 0, 1) * fall ** 0.6 * eye)
    fade = edge_fade(w, h, 36)
    c = np.clip(c, 0, 0.9) * fade
    cloud = np.array(SEA[5], float)
    cloud = (cloud * 0.6 + cloud.mean() * 0.4) * 0.5 + np.array(SEA[1]) * 0.5     # the earth tile's grade
    shadow = np.roll(np.roll(c, 4, axis=0), 4, axis=1) * (1 - c) * fade
    arr = np.zeros((h, w, 4))
    l01.over(arr, np.zeros(3), 0.3 * shadow)
    l01.over(arr, cloud, c)
    return l01.finish_dithered(arr, colors=12, alpha_levels=8, rgb_amp=6.0)


# --------------------------------------------------------------------------- far

def far_piece(build, w, h, colours=24):
    """Built at full scale, reduced to the far layer's 0.45 and hazed as the north arm is."""
    fw, fh = int(round(w / l01.FAR_SCALE)), int(round(h / l01.FAR_SCALE))
    low, high = l01.blank(fw, fh), l01.blank(fw, fh)
    build(low, high, fw, fh)
    img = l01.with_shadow(low, high).resize((w, h), Image.BOX)
    img.putalpha(img.getchannel("A").point(lambda v: 255 if v >= 128 else 0))
    return artkit.quantize_set([haze(img, SEA[2], 0.5)], colours)[0]


def lamp_head(p):
    return union((sd_box(p, (0, 0, 0), (14, 10, 6), 2), HULL),
                 (sd_box(p, (0, -2, 6), (8, 4, 1.5), 0.5), DARK),
                 (sd_sphere(p, (0, 5, 7), 4.2), RED))


def nav_beacon(w, h):
    """A lane beacon: a truss mast with a lamp head (its red lamp lit, still: the motion budget),
    a module and a dish."""
    k = l01.kit()

    def build(low, high, fw, fh):
        cx = fw // 2
        l01.column(low, k["truss_v"], cx, 34, fh - 4)
        l01.put(low, l01.truss_h(70, 18), cx, fh - 40)
        l01.put(high, k["drum_s"], cx, 96)
        l01.put(high, k["dish"], cx + 18, fh - 70)
        l01.put(high, l01.part(lamp_head, (34, 26)), cx, 20)
    return far_piece(build, w, h)


def defence_ring(w, h):
    """The orbital defence ring ahead: a double truss band across the lane with gun platforms on
    it, a sensor station on a pylon below and an array mast above, at the far scale."""
    k = l01.kit()
    platform = l01.platform(140, 140)

    def build(low, high, fw, fh):
        band = (fh * 0.42, fh * 0.42 + 84)
        for y in band:
            l01.put(low, l01.truss_h(fw + 40, 26), fw / 2, y)
        for x in range(60, fw, 150):
            l01.column(low, k["truss_v"], x, band[0], band[1])
        for x in (fw * 0.16, fw * 0.5, fw * 0.84):
            l01.put(high, platform, x, sum(band) / 2)
        sx = fw * 0.67
        l01.column(low, k["truss_v"], sx, band[1], fh - 70)
        l01.put(low, k["radiator"], sx + 60, band[1] + 120)
        l01.put(high, k["drum"], sx, fh - 64)
        l01.put(high, k["dish"], sx - 34, fh - 40)
        mx = fw * 0.33
        l01.column(low, k["truss_v"], mx, 40, band[0])
        l01.put(low, k["solar_l"], mx - 2, 120)
        l01.put(high, k["dock"], mx, 40)
    return far_piece(build, w, h)


# --------------------------------------------------------------------------- ground

def lane_buoy(w, h):
    """A lane-marker buoy: a puck with solar petals, a short mast and a red lamp."""
    def scene(p):
        return union((sd_cylinder_z(p, (0, 0, 0), 9, 3), HULL),
                     (sd_cylinder_z(p, (0, 0, 3), 6, 1.2), DARK),
                     (sd_box(p, (0, 0, 0), (15, 4, 0.8), 0.3), SOLAR),
                     (sd_box(p, (0, 0, 0.9), (15.4, 0.8, 0.6), 0.2), DARK),
                     (sd_capsule(p, (0, 0, 3), (0, 0, 9), 1.4), DARK),
                     (sd_box(p, (0, -6, 2.5), (4, 1.2, 1), 0.4), ACCENT),
                     (sd_sphere(p, (0, 0, 10), 2.2), RED))
    return artkit.quantize_set([l01.part(scene, (w, h), factor=8)], 20)[0]


# The Kestrel: a CDF frigate broken in two. Frigate coordinates: y along the hull (bow up the
# screen), z up; each half is turned and moved apart in the model (before it is lit).
STERN = (np.radians(4.0), np.array([-10.0, -14.0, 0.0]))    # turn, offset of the stern half
BOW = (np.radians(-7.0), np.array([12.0, 22.0, 0.0]))
RACK = (76.0, -76.0)                                       # the lifeboat rack's centre (frigate)
RACK_LIGHTS = [(56.0, -46.0), (96.0, -46.0), (56.0, -106.0), (96.0, -106.0)]


def break_line(x):
    return 4 + 9 * np.sin(x * 0.21) + 5 * np.sin(x * 0.53 + 1.0)


def kestrel_parts(q, stern):
    """One half's (distance, material) in frigate coordinates."""
    x, y = q[:, 0], q[:, 1]
    body = sd_box(q, (0, 0, 0), (34, 268, 16), 9)
    shell = np.minimum(body, sd_ellipsoid(q, (0, 262, 0), (32, 46, 15)))
    cut = break_line(x)
    zone = 30.0
    # plating ends `zone` px before the break; bare ribs and the keel run on to it
    if stern:
        plated = np.maximum(shell, y - (cut - zone))
        bare = np.maximum((cut - zone) - y, y - cut)
    else:
        plated = np.maximum(shell, (cut + zone) - y)
        bare = np.maximum(y - (cut + zone), cut - y)
    slabs = np.abs((y % 12) - 6) - 2.0
    ribs = np.maximum(np.maximum(np.abs(shell) - 3, slabs), bare)
    floor = np.maximum(sd_box(q, (0, 0, -3), (28, 268, 7), 1), bare)
    keel = np.maximum(sd_box(q, (0, 0, -6), (5, 268, 6), 1.5), bare + 4)
    items = [(plated, HULL), (ribs, DARK), (floor, DARK), (keel, DARK),
             (np.maximum(sd_box(q, (0, 0, 14), (6, 250, 4), 1.5), plated - 1), DARK)]   # dorsal spine
    if stern:
        items += [(sd_box(q, (0, -262, 0), (42, 22, 17), 4), DARK)]
        items += [(sd_cylinder_y(q, (bx, -292, 0), 10, 7), DARK) for bx in (-25, 0, 25)]
        items += [(sd_box(q, (sx * 56, -205, 3), (20, 30, 1.6), 0.6), HULL) for sx in (-1, 1)]
        items += [(sd_box(q, (sx * 56, -205 + dy, 4.8), (18, 1.2, 0.8), 0.3), DARK) for sx in (-1, 1) for dy in (-14, 0, 14)]
        items += [(sd_box(q, (0, -190, 16.5), (28, 3, 1.4), 0.6), ACCENT),
                  (sd_cylinder_z(q, (0, -120, 16), 9, 3), HULL), (sd_sphere(q, (0, -120, 18), 7), HULL),
                  (sd_capsule(q, (0, -118, 21), (0, -96, 21), 1.8), DARK)]
        # the lifeboat rack on the starboard flank: struts, a frame, three escape pods, four lamps
        rx, ry = RACK
        frame = np.maximum(sd_box(q, (rx, ry, 4), (24, 34, 4), 1), -sd_box(q, (rx, ry, 4), (19, 29, 8), 0))
        items += [(sd_box(q, (41, ry + sy * 18, 3), (9, 3, 2), 0.6), DARK) for sy in (-1, 1)]
        items += [(frame, DARK)]
        items += [(sd_capsule(q, (rx - 12, ry + py, 5), (rx + 12, ry + py, 5), 5.5), HULL) for py in (-18, 0, 18)]
        items += [(sd_box(q, (rx + 12, ry + py, 5), (1.4, 5.2, 5.2), 0.5), DARK) for py in (-18, 0, 18)]
        items += [(sd_cylinder_z(q, (lx, ly, 6), 3.4, 2.4), DARK) for lx, ly in RACK_LIGHTS]
    else:
        items += [(sd_box(q, (0, 150, 18), (16, 24, 6), 2), DARK),
                  (sd_box(q, (0, 167, 23.5), (11, 2.6, 1.4), 0.5), GLASS),
                  (sd_box(q, (0, 100, 16.5), (28, 3, 1.4), 0.6), ACCENT),
                  (sd_cylinder_z(q, (0, 214, 15), 9, 3), HULL), (sd_sphere(q, (0, 214, 17), 7), HULL),
                  (sd_capsule(q, (0, 216, 21), (0, 240, 21), 1.8), DARK),
                  (sd_capsule(q, (-10, 120, 22), (-10, 132, 32), 1.2), DARK)]
    d, m = union(*items)
    d = np.maximum(d, y - cut if stern else cut - y)            # the break
    for cx, cy, r in ((18, -170, 10), (-16, -44, 9)) if stern else ((-17, 70, 11), (16, 190, 8)):
        d = np.maximum(d, -sd_sphere(q, (cx, cy, 16), r))       # blast craters
    return d, m


def kestrel_scene(p):
    halves = []
    for (turn, offset), stern in ((STERN, True), (BOW, False)):
        q = rotate_z(p - offset, turn)
        halves.append(kestrel_parts(q, stern))
    return union(*halves)


def kestrel_point(fx, fy, stern=True):
    """A frigate point in the piece: px from its centre (x right, y up)."""
    turn, offset = STERN if stern else BOW
    c, s = np.cos(turn), np.sin(turn)
    return c * fx - s * fy + offset[0], s * fx + c * fy + offset[1]


def kestrel_lights():
    return [kestrel_point(x, y) for x, y in RACK_LIGHTS]


LENS = ((92, 60, 30), (54, 36, 22))               # the lifeboat lamps' dark amber lens and its rim


def kestrel(w, h):
    """The broken frigate Kestrel: two halves turned apart in the model, bare ribs and keel at the
    break, blast craters, scorched; the lifeboat rack's four lamps as dark amber lenses (the game
    draws the lit lights over them)."""
    img = l01.part(kestrel_scene, (w, h), factor=3, colors=64)
    img = artkit.quantize_set([receded(scorched(img, 331))], COLOURS - 2)[0]
    a = np.array(img)
    for dx, dy in kestrel_lights():
        cx, cy = int(round(w / 2 + dx)), int(round(h / 2 - dy))
        a[cy - 2:cy + 2, cx - 2:cx + 2, :3] = LENS[1]
        a[cy - 1:cy + 1, cx - 1:cx + 1, :3] = LENS[0]
        a[cy - 2:cy + 2, cx - 2:cx + 2, 3] = 255
    return Image.fromarray(a, "RGBA")


def lifeboat_light(w, h):
    """The rack's amber light, lit (additive, premultiplied on black): a core and a stepped halo."""
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64)
    d = np.hypot(xx - (w - 1) / 2, yy - (h - 1) / 2)
    halo = np.exp(-(d / 3.0) ** 2 / 2) * 1.2
    core = np.exp(-(d / 1.1) ** 2 / 2)
    rgb = np.array([255, 170, 40], float) * halo[..., None] + np.array([255, 240, 200], float) * core[..., None]
    rgb = np.clip(rgb, 0, 255)
    level = np.clip(np.round(rgb.max(axis=-1) / 255 * 5) / 5, 0, 1)        # stepped brightness
    rgb = rgb / np.maximum(rgb.max(axis=-1, keepdims=True), 1) * (level * 255)[..., None]
    return artkit.additive(l01.rgba(rgb, np.full((h, w), 255.0)))


def platform_half(w, h, side, turn, x0, seed):
    """Half of a broken CDF defence platform (Level 01's damaged model), split along a jagged line
    and turned in the model; scorched."""
    model = l01.platform_model(True)

    def scene(p):
        q = rotate_z(p, turn)
        d, m = model(q)
        jag = x0 + 6 * np.sin(q[:, 1] * 0.3 + seed) + 4 * np.sin(q[:, 1] * 0.71 + 2)
        return np.maximum(d, side * (q[:, 0] - jag)), m
    img = l01.part(scene, (150, 150), factor=4, colors=48)
    return artkit.quantize_set([centred(receded(scorched(img, seed)), w, h)], COLOURS)[0]


def wreck(w, h, seed, ribs, turn):
    """A torn hull plate with ribs under it and a stub of truss, turned in the model; scorched."""
    rng = np.random.default_rng(seed)
    n = 8
    angles = np.sort(rng.uniform(0, 2 * np.pi, n))
    rx, ry = w * 0.36, h * 0.36
    poly = [(np.cos(a) * rx * rng.uniform(0.6, 1.0), np.sin(a) * ry * rng.uniform(0.6, 1.0)) for a in angles]

    def scene(p):
        q = rotate_z(p, turn)
        inside = sd_polygon2(q[:, 0], q[:, 1], poly)
        items = [(sd_plate(q, poly, 2, 2.2, 0.6), HULL)]
        for i in range(ribs):
            y = (i - (ribs - 1) / 2) * 12
            items.append((np.maximum(sd_box(q, (0, y, -0.5), (60, 1.8, 2.2), 0.5), inside - 3), DARK))
        items.append((np.maximum(sd_box(q, (rx * 0.3, 0, -1), (2.4, 60, 2.2), 0.5), inside - 5), DARK))
        d, m = union(*items)
        return np.maximum(d, -sd_sphere(q, (rx * -0.25, ry * 0.2, 3), min(rx, ry) * 0.28)), m
    img = l01.part(scene, (w, h), factor=4, colors=40)
    return artkit.quantize_set([receded(scorched(img, seed, amount=0.5, base=0.66))], 20)[0]


def tug_burnt(w, h):
    """A burnt-out cargo tug: boxy body and cab, engine block, the grapple arms forward, a breach;
    blackened (no fire: the motion budget)."""
    def scene(p):
        q = rotate_z(p, np.radians(-12))
        d, m = union((sd_box(q, (0, 4, 0), (20, 36, 12), 4), HULL),
                     (sd_box(q, (0, 32, 13), (12, 10, 5), 2), DARK),
                     (sd_box(q, (0, 40, 17), (8, 2, 1.2), 0.4), GLASS),
                     (sd_box(q, (0, -40, 0), (25, 10, 10), 3), DARK),
                     (sd_cylinder_y(q, (-12, -52, 0), 6, 4), DARK), (sd_cylinder_y(q, (12, -52, 0), 6, 4), DARK),
                     (sd_box(q, (0, -10, 12.5), (16, 2.5, 1.2), 0.5), ACCENT),
                     (sd_capsule(q, (-13, 38, 2), (-17, 62, 2), 3), DARK), (sd_capsule(q, (13, 38, 2), (19, 60, 2), 3), DARK),
                     (sd_capsule(q, (-17, 62, 2), (-10, 70, 2), 2.2), DARK), (sd_capsule(q, (19, 60, 2), (12, 69, 2), 2.2), DARK),
                     (sd_box(q, (-23, 10, 0), (4, 8, 5), 1), HULL), (sd_box(q, (23, -6, 0), (4, 8, 5), 1), HULL))
        return np.maximum(d, -sd_sphere(q, (8, 6, 12), 9)), m
    img = l01.part(scene, (w, h), factor=4, colors=48)
    return artkit.quantize_set([receded(scorched(img, 451, amount=0.55, base=0.56))], 24)[0]


# --------------------------------------------------------------------------- low-air and high-air

def spore_haze(w, h):
    """The olive-grey spore haze: Level 01's cloud decks at ~24 % cover, tinted and thinned to a
    veil the Earth shows through (opaque olive would read as land): alpha at most ~55 %, stepped
    with ordered dithering."""
    img = b2.tinted(l01.banks(w, h, 0.24), SPORE, 0.55)
    return artkit.quantize_set([veil(img, 0.6, 5)], 12)[0]


def veil(img, share, levels):
    """A bank's alpha scaled by ``share`` and stepped to ``levels`` with ordered dithering."""
    a = np.array(img).astype(np.float64)
    alpha = a[..., 3] / 255 * share
    stepped = artkit.ordered_dither(alpha / share, levels) * share
    a[..., 3] = np.where(alpha > 0, stepped, 0) * 255
    a[a[..., 3] == 0] = 0
    return Image.fromarray(a.astype(np.uint8), "RGBA")


def spore_banks(w, h):
    """The dense spore banks of the heavy peak: the decks at ~50 % cover, olive, their cores
    nearly opaque (~85 %) and the edges dithered, the decks' shading kept."""
    img = b2.tinted(l01.banks(w, h, 0.5), SPORE_DENSE, 0.55)
    return artkit.quantize_set([veil(img, 0.88, 6)], 12)[0]


def spore_streaks(w, h):
    """Spores streaming past over the play plane (additive): fewer, longer streaks than the
    sparks, pale olive, each with a brighter head; tails stepped to 8 translucency levels."""
    arr = np.zeros((h, w, 4))
    rng = np.random.default_rng(71)
    for _ in range(34):
        x, y, length = int(rng.uniform(0, w)), int(rng.uniform(0, h)), int(rng.uniform(12, 30))
        sway = rng.uniform(-0.12, 0.12)
        for i in range(length):
            xi = int(round(x + sway * i)) % w
            arr[(y - i) % h, xi] = (200, 210, 140, (1 - i / length) * rng.uniform(0.45, 0.85))
        arr[y % h, x] = (225, 232, 170, 1.0)
    return artkit.stepped_alpha(l01.rgba(arr[..., :3], arr[..., 3] * 255), 9)


TILE_SETS = {
    "earth": b1.TILE_SETS["earth"], "banks-light": b1.TILE_SETS["banks-light"],
    "spore-haze": spore_haze, "spore-banks": spore_banks,
    "wisps": b1.TILE_SETS["wisps"], "ice-streaks": b2.frost_streaks, "spore-streaks": spore_streaks,
}
PIECES = {
    "weather-front-a": lambda w, h: weather_front(w, h, 361, True, -0.8),
    "weather-front-b": lambda w, h: weather_front(w, h, 377, False, 0.9),
    "nav-beacon": nav_beacon, "defence-ring": defence_ring,
    "lane-buoy": lane_buoy, "kestrel": kestrel,
    "platform-half-a": lambda w, h: platform_half(w, h, 1, np.radians(24), 6, 3),
    "platform-half-b": lambda w, h: platform_half(w, h, -1, np.radians(-38), -10, 5),
    "wreck-a": lambda w, h: wreck(w, h, 811, 3, np.radians(18)),
    "wreck-b": lambda w, h: wreck(w, h, 823, 4, np.radians(-30)),
    "wreck-c": lambda w, h: wreck(w, h, 837, 3, np.radians(65)),
    "tug-burnt": tug_burnt,
    "platform": b1.PIECES["platform"], "crossbeam": b1.PIECES["crossbeam"],
}
# Images the game loads from the backdrop folder that are not set pieces: id -> (size, generator).
EXTRAS = {"lifeboat-light": ((12, 12), lifeboat_light)}


def level_data():
    """The level's data.yaml if it has a backdrop block, otherwise the proposal."""
    data = LEVEL_DIR / "data.yaml"
    if data.exists():
        loaded = yaml.safe_load(data.read_text(encoding="utf-8"))
        if "backdrop" in loaded:
            return loaded
    return yaml.safe_load((LEVEL_DIR / "backdrop-proposal.yaml").read_text(encoding="utf-8"))


def jobs(backdrop, wanted):
    out = b1.jobs(backdrop, set())
    out += [(name, size, None) for name, (size, _) in EXTRAS.items()]
    return [job for job in out if not wanted or job[0] in wanted]


def render(job):
    name, (w, h), count = job
    fn = TILE_SETS.get(name) or PIECES.get(name) or EXTRAS[name][1]
    images = fn(w, h, count) if count else [fn(w, h)]
    for img in images:
        if img.size != (w, h):
            raise ValueError(f"{name}: rendered {img.size}, the data file says {(w, h)}")
    return images


def write(job, images):
    name = job[0]
    artkit.save_png(images[0], OUT / f"{name}.png", SOURCE)
    print(f"{name}: {len(images)} image(s), {artkit.colour_count(images)} colours")


def print_lights(level):
    """The lifeboat lights' offsets from the kestrel's centre and their ground-target placements."""
    backdrop = level["backdrop"]
    placed = next(p for p in backdrop["placed"] if p["piece"] == "kestrel")
    speed = 140.0
    names = ("top left", "top right", "bottom left", "bottom right")
    rack = kestrel_point(*RACK)
    print(f"lifeboat rack centre: dx {rack[0]:.0f}, dy {rack[1]:.0f}; passes mid-screen at t = "
          f"{placed['t'] + rack[1] / speed:.2f}")
    for name, (dx, dy) in zip(names, kestrel_lights()):
        t_mid = placed["t"] + dy / speed  # the scroll brings a point below the centre (dy < 0) in first
        print(f"lifeboat light ({name}): dx {dx:.0f}, dy {dy:.0f} -> at [{t_mid - 276 / speed:.2f}, "
              f"{placed['x'] + dx:.0f}]")


# --------------------------------------------------------------------------- review

def review(backdrop):
    """Every tile set and piece on the dark plate, shrunk to fit; the Kestrel also at 1x with its
    lights lit, and the light at 4x."""
    items = []
    for name, spec in list(backdrop["tile_sets"].items()) + list(backdrop["pieces"].items()):
        img = Image.open(OUT / f"{name}.png").convert("RGBA")
        additive = spec.get("layer") == "high-air"
        scale = min(1.0, 260 / max(img.size))
        img = img.resize((max(1, int(img.width * scale)), max(1, int(img.height * scale))), Image.NEAREST)
        items.append((f"{name} {spec.get('layer')}", img, additive))
    light = Image.open(OUT / "lifeboat-light.png").convert("RGBA")
    k = Image.open(OUT / "kestrel.png").convert("RGBA")
    lit = Image.new("RGBA", k.size, artkit.PLATE)
    lit.alpha_composite(k)
    for dx, dy in kestrel_lights():
        lit = artkit.add_light(lit, light, (int(round(k.width / 2 + dx)) - 6, int(round(k.height / 2 - dy)) - 6))
    items.append(("kestrel 1x, rack lights lit", lit, False))
    rx, ry = kestrel_point(*RACK)
    cx, cy = int(round(k.width / 2 + rx)), int(round(k.height / 2 - ry))
    rack = lit.crop((cx - 60, cy - 60, cx + 60, cy + 60))
    items.append(("rack 2x", sprite.enlarge(rack, 2), False))
    items.append(("lifeboat-light 4x (additive)", sprite.enlarge(light, 4), True))
    width, x, y, row_h = 1400, 16, 44, 0
    rows = []
    for name, img, add in items:
        cell = max(img.width, 6 * len(name))
        if x + cell > width - 16:
            x, y = 16, y + row_h + 30
            row_h = 0
        rows.append((name, img, add, x, y))
        x += cell + 14
        row_h = max(row_h, img.height)
    artkit.REVIEW_ROUND = "r16"
    sheet = raster.sheet(width, y + row_h + 40, "LEVEL 03 BACKDROP - FINAL PIECES", "PRODUCTION ART, M4 PART C BATCH - R16")
    for name, img, add, px, py in rows:
        plate = Image.new("RGBA", img.size, artkit.PLATE)
        if add:
            plate = artkit.add_light(plate, img)
        else:
            plate.alpha_composite(img)
        sheet.alpha_composite(plate, (px, py + 14))
        raster.draw_text(sheet, px, py, name.upper(), raster.LABEL)
    path = LEVEL_DIR / "concept" / "backdrop-final-r16-a.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    sheet.convert("RGB").save(path, optimize=True)
    print(f"review: {path.relative_to(ROOT)}")


def main(argv):
    level = level_data()
    backdrop = level["backdrop"]
    wanted = {a for a in argv if a != "--review"}
    known = TILE_SETS.keys() | PIECES.keys() | EXTRAS.keys()
    missing = (backdrop["tile_sets"].keys() | backdrop["pieces"].keys()) - known
    if missing or wanted - known:
        raise SystemExit(f"no generator for {sorted(missing | (wanted - known))}")
    if "--review" not in argv:
        OUT.mkdir(parents=True, exist_ok=True)
        todo = jobs(backdrop, wanted)
        with ProcessPoolExecutor() as pool:
            for job, images in zip(todo, pool.map(render, todo)):
                write(job, images)
    print_lights(level)
    review(backdrop)


if __name__ == "__main__":
    main(sys.argv[1:])
