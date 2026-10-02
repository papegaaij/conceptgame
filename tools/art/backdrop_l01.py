#!/usr/bin/env python3
"""Production art: the Level 01 backdrop (design/campaign/act-1-first-contact/level-01-break-at-dawn).

Outputs (assets/backdrop/level-01/, one PNG per tile set and set piece of the level's data.yaml
`backdrop` block, named by its id and checked against its size; frames and headings as
<id>_<n>.png):
  earth, earth-dawn, earth-limb, moon, vrell-glow    deep: the day side, the dawn overlay with the
                                                     night side and terminator, the limb in the
                                                     outro, the Moon, the strike group's glow
  north-arm, stormhawk-far_0..15                     far: the north arm, Aegis Two's 16 headings
  launch-rail, crossbeam, dock-frames, dock-frame,   ground: the station structures, tile sets
  gantry-rails, bridge-crane, cruiser-hull,          and set pieces; platform-burning_0..3 is
  perimeter, platform, platform-burning_0..3         the 4-frame fire loop
  dock-frame-mirrored, bridge-crane-mirrored,        the mirrored placements as their own renders:
  crane-jib-mirrored, platform-burning-mirrored_0..3 the layout mirrored, the key light not
  banks-light, banks-medium, lattice-beam, crane-jib low-air
  wisps, spark-streaks                               high-air (additive, at most 40 % opacity)
  design/campaign/.../level-01-break-at-dawn/concept/backdrop-final-r12-a.png/.gif
                                                     review sheet and the animated pieces' loops
  design/campaign/.../level-01-break-at-dawn/concept/<id>-final-r13-a.png
                                                     the review sheet of a piece reworked after
                                                     round 12 (REWORKED: north-arm, whose solar
                                                     wings now end inside the piece)

The look is the placeholder's (tools/concept/backdrop_l01.py, frozen and imported unchanged:
the chosen Earth orbit scene of parallax r03 A, its station kit with the muted ground accent of
readability rule 7, palette B, seeds and layouts), brought to the production bar
(design/art-direction/production):
  - every piece is posterized on its opaque pixels to 12-32 colours (the ground structures were
    300-500), one palette per frame set, so an animation never flickers between palettes;
  - wide gradients are ordered-dithered (4x4 Bayer) as the round-11 dawn fix did: the Earth limb's
    haze and the Vrell glow; the platforms' smoke and the spark streaks get stepped translucency;
  - the Moon is rendered at 4x and downsampled; Aegis Two's Stormhawks are the chosen round-08
    model through the sprite render path (8x, 1-bit alpha, unsharp mask, one palette);
  - nothing lit is mirrored at runtime (symmetry rule): every placement the data used to draw
    flipped has a `-mirrored` piece whose layout is mirrored and rendered under the same top-left
    key light.

Run: python3 tools/art/backdrop_l01.py [--review] [id ...]   (~2 min on 20 cores; --review only
rebuilds the review files from assets/; with ids only those pieces and the sheets of the reworked
ones among them)
"""
import functools
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
from render import models  # noqa: E402
from render.palette import B  # noqa: E402
from render.sdf import mirror_x, rotate_z, sd_box, sd_cylinder_x, sd_cylinder_z, sd_sphere, union  # noqa: E402
from render.station import ACCENT, DARK, HULL, RED, SOLAR  # noqa: E402
from render.enemy_rigs import write_gif  # noqa: E402

# The concept generator has this script's name, so it is loaded by path.
_spec = importlib.util.spec_from_file_location(
    "concept_backdrop_l01", Path(__file__).resolve().parents[1] / "concept" / "backdrop_l01.py")
l01 = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(l01)

SCRIPT = "backdrop_l01.py"
SOURCE = artkit.source_note(SCRIPT)
LEVEL_DIR = DESIGN / "campaign" / "act-1-first-contact" / "level-01-break-at-dawn"
OUT = ROOT / "assets" / "backdrop" / "level-01"
SEA = l01.SEA
COLOURS = 32                     # backdrops: 12-32 colours (palette approach)
FLIP = np.array([-1.0, 1.0, 1.0])
FAR_EXTENT = 2.3 * 24 / (48 * l01.FAR_SCALE)   # the player ship's extent at the far scale


def posterized(fn, colours=COLOURS):
    """A placeholder piece function, its output posterized on the opaque pixels."""
    return lambda w, h: artkit.quantize_set([fn(w, h)], colours)[0]


def flipped(scene):
    """A model mirrored left to right before it is lit, so its shading stays the key light's."""
    return lambda p: scene(p * FLIP)


def dithered_alpha(arr, levels):
    """Translucent pixels (smoke, sparks) stepped to a few alpha levels with ordered dithering."""
    a = arr[..., 3]
    soft = (a > 0) & (a < 1)
    a[soft] = artkit.ordered_dither(a, levels - 1)[soft]
    return arr


# --------------------------------------------------------------------------- deep

def earth_limb(w, h, radius=520, below=240, haze_depth=110):
    """The placeholder's Earth limb (its geometry and colours), with the wide haze and airglow
    ramps ordered-dithered instead of cut into bands."""
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64)
    crest = h - below
    depth = radius - np.hypot(xx - w * 0.58, yy - crest - radius)
    inside = np.clip(depth + 0.5, 0, 1)
    arr = np.zeros((h, w, 4))
    graze = np.clip(1 - depth / haze_depth, 0, 1) ** 2
    l01.over(arr, np.array(SEA[2], float) * (1 - 0.4 * graze)[..., None], 0.85 * graze * inside)
    st = l01.stars(w, h, 13)
    l01.over(arr, st[..., :3] * np.clip(-depth / 60, 0, 1)[..., None], 1 - inside)
    l01.over(arr, np.array(SEA[3], float) * 0.7, 0.3 * np.exp(np.minimum(depth, 0) / 16) * (1 - inside))
    light = 0.8 + 0.2 * (1 - xx / w)
    l01.over(arr, np.array(SEA[4], float), 0.9 * light * np.exp(-((depth + 1) / 2.2) ** 2))
    l01.over(arr, np.array(SEA[5], float), 0.6 * light * np.exp(-((depth + 1) / 1.0) ** 2))
    return l01.finish_dithered(arr, colors=COLOURS, alpha_levels=16, rgb_amp=8.0)


def moon(w, h, factor=4):
    """The Moon at 4x: lit from the top-left, craters, hazed; 1-bit edge from the coverage."""
    W, H = w * factor, h * factor
    yy, xx = (np.mgrid[0:H, 0:W] + 0.5) / factor
    r = w / 2 - 1
    dx, dy = (xx - w / 2) / r, (yy - h / 2) / r
    d2 = dx * dx + dy * dy
    nz = np.sqrt(np.clip(1 - d2, 0, 1))
    lit = np.clip(-0.55 * dx - 0.6 * dy + 0.6 * nz, 0, 1)
    craters = raster.fbm(W, H, 6 * factor, 31, octaves=4, period=False)
    rgb = np.array([150, 155, 170], float) * (0.25 + 0.85 * lit * (0.8 + 0.3 * craters))[..., None]
    rgba = np.dstack([rgb / 255, (d2 <= 1).astype(float)])
    img = sprite.to_image(sprite.downsample(rgba, factor), 0.5)
    return artkit.quantize_set([haze(img, SEA[2], 0.25)], 12)[0]


def vrell_glow(w, h):
    """The placeholder's horizon glow, its long fade ordered-dithered."""
    yy = np.mgrid[0:h, 0:w][0].astype(np.float64)
    n = raster.fbm(w, h, 40, 41, octaves=4, period=False)
    mix = np.clip(raster.fbm(w, h, 120, 43, octaves=2, period=False) * 1.4 - 0.2, 0, 1)[..., None]
    rgb = np.array([96, 20, 128], float) * (1 - mix) + np.array([0, 140, 100], float) * mix
    arr = np.dstack([rgb, (1 - yy / h) ** 2.2 * (0.25 + 0.3 * n)])
    rng = np.random.default_rng(45)
    for _ in range(14):                                     # distant ship lights
        x, y = int(rng.uniform(20, w - 20)), int(rng.uniform(6, 48))
        arr[y, x] = (120, 255, 200, 0.85)
    return l01.finish_dithered(arr, colors=20, alpha_levels=12, rgb_amp=6.0)


# --------------------------------------------------------------------------- far

def stormhawk_far(w, h, n):
    """Aegis Two at the far scale: frame i heads i * 360 / n clockwise from up the screen, each
    heading its own render under the fixed key light; one palette for the set."""
    scene, mats = models.ship_a_model(palette=B.ship_colors())

    def frame(i):
        angle = -2 * np.pi * i / n
        hi, factor = artkit.render_hi(lambda p: scene(rotate_z(p, angle)), mats, (w, h), FAR_EXTENT)
        return haze(artkit.native(hi, factor), SEA[2], 0.3)
    return artkit.quantize_set([frame(i) for i in range(n)], 16)


# Pieces re-rendered after round 12, each reviewed on its own sheet: id -> review round.
REWORKED = {"north-arm": "r13"}
SOLAR_WING = 118                 # the north arm's solar wings, mast root to tip (full scale, px)
RIB_PITCH = 16                   # their cells


def solar_wing(length, depth, left):
    """A solar wing that ends inside the north arm: the kit's mast and two panel blankets (the
    concept's ``station.solar_part``), each blanket in a steel frame with an end rail and split
    into cells by dark ribs, the mast ending in a capped hub with the red tip light, so the wing
    reads as a built structure."""
    sign = -1 if left else 1
    half = length / 2 - 10                 # a blanket's half length; it ends 4 px short of the tip

    def scene(p):
        q = p.copy()
        q[:, 0] = q[:, 0] * sign
        mid = length / 2 + 2
        items = [(sd_cylinder_x(q, (length / 2 - 4, 0, 1), 2.4, length / 2 - 2), DARK),
                 (sd_cylinder_x(q, (length - 4, 0, 1), 3.4, 2.2), HULL)]
        for y in (-depth / 4 - 2, depth / 4 + 2):
            blanket = depth / 4 - 2
            items += [(sd_box(q, (mid, y, 0), (half, blanket, 0.8), 0.2), SOLAR),
                      (sd_box(q, (mid, y, 0.6), (half, blanket, 0.4), 0.0), SOLAR)]
            for side in (-1, 1):
                items.append((sd_box(q, (mid, y + side * blanket, 1.0), (half + 1.2, 1.2, 1.0), 0.4), HULL))
            items.append((sd_box(q, (mid + half, y, 1.0), (1.4, blanket + 1.2, 1.2), 0.4), HULL))
            for x in np.arange(mid - half + RIB_PITCH, mid + half - 4, RIB_PITCH):
                items.append((sd_box(q, (x, y, 0.9), (0.6, blanket, 0.5), 0.2), DARK))
        items.append((sd_sphere(q, (length - 1, 0, 2.2), 2.0), RED))
        return union(*items)
    return l01.part(scene, (length * 2, depth))


def north_arm(w, h):
    """The placeholder's north arm (its kit, layout and scale) with solar wings that end inside
    the piece: the placeholder's 140 px wings ran past its 180 px width and were cut off as flat
    slabs (user decision after round 12)."""
    k = l01.kit()
    fw, fh = int(round(w / l01.FAR_SCALE)), int(round(h / l01.FAR_SCALE))
    low, high = l01.blank(fw, fh), l01.blank(fw, fh)
    cx = fw // 2
    for x in (cx - 72, cx + 72):
        l01.column(low, k["truss_v"], x, 0, fh)
    l01.column(low, k["rail"], cx, 90, fh)
    for y in (180, 560, 940, 1300):
        l01.put(low, l01.truss_h(170), cx, y)
    l01.put(low, solar_wing(SOLAR_WING, 56, True), cx - 72, 420)
    l01.put(low, solar_wing(SOLAR_WING, 56, False), cx + 72, 760)
    l01.put(low, k["radiator"], cx + 130, 1120)
    l01.put(high, k["drum"], cx - 72, 1080)
    l01.put(high, k["dish"], cx + 72, 260)
    l01.put(high, k["dock"], cx, 70)
    img = l01.with_shadow(low, high).resize((w, h), Image.BOX)
    img.putalpha(img.getchannel("A").point(lambda v: 255 if v >= 128 else 0))
    return artkit.quantize_set([haze(img, SEA[2], 0.5)], 24)[0]


# --------------------------------------------------------------------------- ground and low-air

def dock_frame(w, h, flip=False):
    """An open dock frame with a frigate section in it; turrets on opposite corners."""
    k = l01.kit()
    low, high = l01.blank(w, h), l01.blank(w, h)
    for x in (16, w - 16):
        l01.column(low, k["truss_v"], x, 0, h)
    for y in (14, h - 14):
        l01.put(low, l01.truss_h(w - 20), w / 2, y)
    l01.put(high, l01.frigate_section(), w / 2, h / 2 + 6)
    left, right = (w - 16, 16) if flip else (16, w - 16)
    l01.put(high, k["turret"], left, 30)
    l01.put(high, k["turret"], right, h - 30)
    return l01.with_shadow(low, high)


def bridge_crane(w, h, flip=False):
    """A bridge crane spanning the rail beds; its trolley (the beacon's place) is 60 px right of
    the centre, left in the mirrored layout."""
    span = w - 84

    def scene(p):
        q = mirror_x(p)
        return union((sd_box(q, (span / 2, 0, 0), (14, 28, 6), 1.5), HULL),
                     (sd_box(q, (span / 2, 0, 6), (8, 22, 1.5), 0.5), ACCENT),
                     (sd_box(p, (60, 0, 9), (16, 18, 5), 1.2), DARK),
                     (sd_cylinder_z(p, (60, 0, 14), 5, 1.5), HULL),
                     (sd_sphere(q, (span / 2, 24, 7), 1.8), RED))
    img = l01.blank(w, h)
    l01.put(img, l01.truss_h(span, 30), w / 2, h / 2)
    img.alpha_composite(l01.part(flipped(scene) if flip else scene, (w, h)))
    return img


def crane_jib(w, h, flip=False):
    """A tower crane's jib: counterweight at the left, hook at the right (swapped when mirrored)."""
    def scene(p):
        return union((sd_box(p, (-w / 2 + 24, 0, 2), (20, 18, 6), 1.5), DARK),
                     (sd_box(p, (-w / 2 + 24, 0, 8), (14, 12, 1), 0.5), ACCENT),
                     (sd_box(p, (w / 2 - 22, 0, 2), (6, 8, 3), 0.8), HULL),
                     (sd_sphere(p, (w / 2 - 10, 0, 4), 2), RED))
    img = l01.blank(w, h)
    l01.put(img, l01.truss_h(w - 40, 26), w / 2 - 10 if flip else w / 2 + 10, h / 2)
    img.alpha_composite(l01.part(flipped(scene) if flip else scene, (w, h)))
    return img


def platform_burning(w, h, n, flip=False):
    """The damaged platform with fires venting from its breaches, n frames of a calm flicker: the
    placeholder's plumes (round-11 fix: ragged tongues of a dull fire ramp, smoke, embers; the
    turbulence cycles through the frames, so the loop is seamless). Production: the smoke's
    translucency is stepped with ordered dithering and the frames share one palette. Mirrored,
    the breaches and the blown-out deck move to the other side; the vented gas still drifts
    down-right and the key light stays top-left."""
    model = l01.platform_model(True)
    base = l01.part(flipped(model) if flip else model, (w, h), factor=4, colors=40)
    a = np.array(base).astype(np.float64)
    a[..., :3] *= 0.72                                       # scorched
    base = a / np.array([1, 1, 1, 255])
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64)
    cx, cy = w / 2, h / 2
    side = -1 if flip else 1
    breaches = ((cx - 22 * side, cy + 18), (cx + 20 * side, cy - 20), (cx + 50 * side, cy + 8))
    turb = [raster.fbm(64, 64, 16, 140 + k, octaves=3) - 0.5 for k in range(2)]
    drift = np.array([0.45, 0.89])
    edge = np.clip(np.minimum.reduce([xx, yy, w - 1 - xx, h - 1 - yy]) / 12, 0, 1)
    rng = np.random.default_rng(57)
    frames = []
    for f in range(n):
        ph = 2 * np.pi * f / n
        arr = base.copy()
        for i, (bx, by) in enumerate(breaches):
            out = np.array([bx - cx, by - cy])
            d = out / np.hypot(*out) * 0.6 + drift * 0.4
            d /= np.hypot(*d)
            nx, ny = -d[1], d[0]
            u = (xx - bx) * d[0] + (yy - by) * d[1]
            v = (xx - bx) * nx + (yy - by) * ny
            length, width = 25 + 4 * i, 6.5 + i
            t = l01.sample_wrapped(turb[0], u * 3 + 9 * i, v * 3) * np.cos(ph + i) + \
                l01.sample_wrapped(turb[1], u * 3 + 9 * i, v * 3) * np.sin(ph + i)
            su = u - 8
            smoke = np.clip(1 - np.abs(su - 14) / 24, 0, 1) * np.exp(-(v / (10 + su.clip(0) * 0.3)) ** 2)
            smoke *= np.clip(0.6 + 1.4 * t, 0, 1) * (su > -4) * edge
            l01.over(arr, np.array([56, 56, 66], float), np.clip(smoke * 0.65, 0, 0.5))
            wid = width * (0.6 + 0.4 * np.clip(u / length, 0, 1) ** 0.5)
            heat = np.clip(1 - u / length, 0, 1) ** 0.8 * np.exp(-(v / wid) ** 2) * (u > -3)
            heat = heat + 0.55 * t * np.clip(heat * 3, 0, 1) + 0.45 * np.exp(-((u + 1) ** 2 + v ** 2) / 10)
            heat *= np.clip(edge * 12 / 5, 0, 1)
            step = np.clip(np.floor(heat * 6.2) - 1, -1, 5).astype(int)
            l01.over(arr, l01.FIRE[np.clip(step, 0, 5)], (step >= 0) * 1.0)
        for _ in range(6):                                    # embers, a few pixels per frame
            bx, by = breaches[rng.integers(len(breaches))]
            x, y = int(bx + side * rng.normal(6, 7)), int(by + rng.normal(8, 7))
            if 3 <= x < w - 3 and 3 <= y < h - 3:
                arr[y, x] = (*l01.FIRE[4], 1.0)
        arr = dithered_alpha(arr, 5)
        frames.append(l01.rgba(arr[..., :3], arr[..., 3] * 255))
    return artkit.quantize_set(frames, COLOURS)


# --------------------------------------------------------------------------- high-air

def spark_streaks(w, h):
    """The placeholder's spark streaks with their fading tails stepped to 8 translucency levels."""
    return artkit.stepped_alpha(l01.spark_streaks(w, h), 9)


TILE_SETS = {
    "earth": l01.earth_tile, "dock-frames": posterized(l01.dock_frames_tile),
    "gantry-rails": posterized(l01.gantry_rails_tile), "perimeter": posterized(l01.perimeter_tile),
    "banks-light": lambda w, h: l01.banks(w, h, 0.12), "banks-medium": lambda w, h: l01.banks(w, h, 0.22),
    "wisps": l01.wisps, "spark-streaks": spark_streaks,
}
PIECES = {
    "earth-dawn": l01.earth_dawn, "moon": moon, "earth-limb": earth_limb, "vrell-glow": vrell_glow,
    "north-arm": north_arm, "stormhawk-far": stormhawk_far,
    "launch-rail": posterized(l01.launch_rail), "crossbeam": posterized(l01.crossbeam),
    "dock-frame": posterized(dock_frame),
    "dock-frame-mirrored": posterized(functools.partial(dock_frame, flip=True)),
    "bridge-crane": posterized(bridge_crane),
    "bridge-crane-mirrored": posterized(functools.partial(bridge_crane, flip=True)),
    "cruiser-hull": posterized(l01.cruiser_hull),
    "platform-burning": platform_burning,
    "platform-burning-mirrored": functools.partial(platform_burning, flip=True),
    "platform": posterized(l01.platform),
    "lattice-beam": posterized(l01.lattice_beam), "crane-jib": posterized(crane_jib),
    "crane-jib-mirrored": posterized(functools.partial(crane_jib, flip=True)),
}


def backdrop_data():
    return yaml.safe_load((LEVEL_DIR / "data.yaml").read_text(encoding="utf-8"))["backdrop"]


def jobs(backdrop, wanted):
    """(name, size, image count or None) of every tile set and piece of the data file."""
    width = 480
    out = [(name, (width, spec["height"]), None) for name, spec in backdrop["tile_sets"].items()]
    out += [(name, tuple(spec["size"]), spec.get("frames") or spec.get("headings"))
            for name, spec in backdrop["pieces"].items()]
    return [job for job in out if not wanted or job[0] in wanted]


def render(job):
    name, (w, h), count = job
    fn = TILE_SETS.get(name) or PIECES[name]
    images = fn(w, h, count) if count else [fn(w, h)]
    for img in images:
        if img.size != (w, h):
            raise ValueError(f"{name}: rendered {img.size}, the data file says {(w, h)}")
    return images


def write(job, images):
    name, _, count = job
    if count:
        for old in OUT.glob(f"{name}_*.png"):
            if old.stem.rsplit("_", 1)[1].isdigit() and old.stem.rsplit("_", 1)[0] == name:
                old.unlink()
        for i, img in enumerate(images):
            artkit.save_png(img, OUT / f"{name}_{i}.png", SOURCE)
    else:
        artkit.save_png(images[0], OUT / f"{name}.png", SOURCE)
    print(f"{name}: {len(images)} image(s), {artkit.colour_count(images)} colours")


# --------------------------------------------------------------------------- review

def load(name, count=None):
    if count:
        return [Image.open(OUT / f"{name}_{i}.png").convert("RGBA") for i in range(count)]
    return Image.open(OUT / f"{name}.png").convert("RGBA")


def plate(w, h):
    return Image.new("RGBA", (w, h), artkit.PLATE)


def tiled(tile, h):
    out = plate(tile.width, h)
    for y in range(0, h, tile.height):
        out.alpha_composite(tile, (0, y))
    return out


def over(img, base=None, additive=False):
    base = base.copy() if base is not None else plate(*img.size)
    if additive:
        return artkit.add_light(base, img)
    base.alpha_composite(img)
    return base


def shrink(img):
    return img.resize((img.width // 2, img.height // 2), Image.BOX)


def flow(items, width, gap=14):
    """Lay labelled images out left to right in rows; returns the sheet."""
    def cell(item):
        return max(item[1].width, 6 * len(item[0]))
    rows, row, x = [], [], 16
    for item in items:
        if row and x + cell(item) > width - 16:
            rows.append(row)
            row, x = [], 16
        row.append(item)
        x += cell(item) + gap
    rows.append(row)
    height = 44 + sum(max(i[1].height for i in r) + 30 for r in rows)
    sheet = raster.sheet(width, height, "LEVEL 01 BACKDROP - FINAL",
                         f"PRODUCTION ART, LEVEL 01 BATCH - {artkit.REVIEW_ROUND.upper()}")
    y = 44
    for r in rows:
        x = 16
        for label, img in r:
            raster.draw_text(sheet, x, y, label, raster.LABEL)
            sheet.alpha_composite(img, (x, y + 12))
            x += cell((label, img)) + gap
        y += max(i[1].height for i in r) + 30
    return sheet


def review(backdrop):
    pieces = backdrop["pieces"]
    earth = load("earth")
    n_fire = pieces["platform-burning"]["frames"]
    n_heads = pieces["stormhawk-far"]["headings"]

    def label(name, imgs, zoom):
        imgs = imgs if isinstance(imgs, list) else [imgs]
        return f"{name.upper()} {imgs[0].width}X{imgs[0].height} {artkit.colour_count(imgs)} COL {zoom}"

    deep = [
        (label("earth-dawn over earth", load("earth-dawn"), "0.5X"),
         shrink(over(load("earth-dawn"), tiled(earth, 1600)))),
        (label("earth-limb over earth", load("earth-limb"), "0.5X"),
         shrink(over(load("earth-limb"), tiled(earth, 720)))),
        (label("limb", load("earth-limb"), "1X"),
         over(load("earth-limb"), tiled(earth, 720)).crop((0, 380, 480, 620))),
        (label("vrell-glow", load("vrell-glow"), "1X"), over(load("vrell-glow"))),
        (label("terminator", load("earth-dawn"), "1X"),
         over(load("earth-dawn"), tiled(earth, 1600)).crop((0, 1080, 480, 1380))),
        (label("moon", load("moon"), "4X"), sprite.enlarge(over(load("moon")), 4)),
        (label("earth", earth, "0.5X"), shrink(earth)),
    ]
    far = [(label("north-arm", load("north-arm"), "1X"), over(load("north-arm"))),
           (label("stormhawk-far", load("stormhawk-far", n_heads), "3X"),
            strip([sprite.enlarge(over(f), 3) for f in load("stormhawk-far", n_heads)], 8))]
    ground = [(label(name, load(name), "1X"), over(load(name)))
              for name in ("launch-rail", "dock-frames", "gantry-rails", "perimeter", "dock-frame",
                           "dock-frame-mirrored", "cruiser-hull", "platform", "bridge-crane",
                           "bridge-crane-mirrored", "crossbeam")]
    fire = [(label(name, load(name, n_fire), "2X"),
             strip([sprite.enlarge(over(f), 2) for f in load(name, n_fire)], 2))
            for name in ("platform-burning", "platform-burning-mirrored")]
    air = [(label(name, load(name), "1X"), over(load(name))) for name in
           ("lattice-beam", "crane-jib", "crane-jib-mirrored")]
    air += [(label(name, load(name), "0.5X"), shrink(over(load(name), additive=add)))
            for name, add in (("banks-light", False), ("banks-medium", False), ("wisps", True),
                              ("spark-streaks", True))]
    sheet = flow(deep + far + ground + fire + air, 1720)
    gif = []
    for k in range(16):                                     # 2 s at the fires' 8 fps
        f = Image.new("RGBA", (140 * 4 + 72 * 2 + 60, 300), raster.SHEET_BG)
        raster.draw_text(f, 12, 8, "PLATFORM-BURNING / -MIRRORED 2X, 8 FPS", raster.LABEL)
        for j, name in enumerate(("platform-burning", "platform-burning-mirrored")):
            f.alpha_composite(sprite.enlarge(over(load(name, n_fire)[k % n_fire]), 2), (12 + j * 292, 24))
        raster.draw_text(f, 600, 8, "STORMHAWK-FAR 3X", raster.LABEL)
        f.alpha_composite(sprite.enlarge(over(load("stormhawk-far", n_heads)[k % n_heads]), 3), (610, 24))
        gif.append(f)
    artkit.save_review(sheet, gif, LEVEL_DIR / "concept", "backdrop", fps=8)


def strip(frames, per_row, gap=4):
    cols = min(per_row, len(frames))
    rows = -(-len(frames) // cols)
    w, h = frames[0].size
    out = Image.new("RGBA", (cols * (w + gap) - gap, rows * (h + gap) - gap), (0, 0, 0, 0))
    for i, f in enumerate(frames):
        out.alpha_composite(f, ((i % cols) * (w + gap), (i // cols) * (h + gap)))
    return out


def piece_review(name, round_):
    """The review sheet of a piece re-rendered after round 12: alone, over the Earth tile at 1x
    and at 2x, as ``<id>-final-<round>-a.png`` in Level 01's concept directory."""
    img, earth = load(name), load("earth")
    on_earth = over(img, tiled(earth, img.height).crop((0, 0, img.width, img.height)))
    items = [(f"{name.upper()} {img.width}X{img.height}, {artkit.colour_count([img])} COLOURS", over(img)),
             ("OVER THE EARTH TILE, 1X", on_earth), ("2X", sprite.enlarge(on_earth, 2))]
    width = 16 + sum(i[1].width + 14 for i in items) + 2
    sheet = raster.sheet(width, 44 + items[-1][1].height + 30, f"{name.upper()} - FINAL ({round_.upper()})",
                         f"PRODUCTION ART, UI BATCH - {round_.upper()}")
    x = 16
    for label, image in items:
        raster.draw_text(sheet, x, 44, label, raster.LABEL)
        sheet.alpha_composite(image, (x, 56))
        x += image.width + 14
    path = LEVEL_DIR / "concept" / f"{name}-final-{round_}-a.png"
    sheet.convert("RGB").save(path, optimize=True)
    print(f"review: {path.relative_to(ROOT)}")


def main(argv):
    """All pieces and both reviews by default; with ids only those pieces and the review sheets of
    the reworked ones among them."""
    backdrop = backdrop_data()
    review_only = "--review" in argv
    wanted = {a for a in argv if a != "--review"}
    known = TILE_SETS.keys() | PIECES.keys()
    missing = (backdrop["tile_sets"].keys() | backdrop["pieces"].keys()) - known
    if wanted - known or missing:
        raise SystemExit(f"no generator for {sorted((wanted - known) | missing)}")
    if not review_only:
        OUT.mkdir(parents=True, exist_ok=True)
        todo = jobs(backdrop, wanted)
        with ProcessPoolExecutor() as pool:
            for job, images in zip(todo, pool.map(render, todo)):
                write(job, images)
    if not wanted:
        review(backdrop)
    for name in sorted((wanted or REWORKED.keys()) & REWORKED.keys()):
        piece_review(name, REWORKED[name])


if __name__ == "__main__":
    main(sys.argv[1:])
