#!/usr/bin/env python3
"""Production art: the Level 02 backdrop, the Gagarin yards burning the morning after
(design/campaign/act-1-first-contact/level-02-shipyard-burning; M4 part B batch).

Outputs (assets/backdrop/level-02/, one PNG per tile set and set piece of the level's data.yaml
`backdrop` block, named by its id and checked against its size; frames as <id>_<n>.png):
  earth, space, moon, earth-limb                deep: the day side, open space past the yard
  north-arm-burning                             far: the north arm, burning, with Aegis Two's tracers
  perimeter, dock-frames, gantry-rails,         ground: Level 01's station kit, the docks with their
  crossbeam, platform, platform-burning_0..3,   half-built frigates and teal Vrell growth, the
  dock-growth, resolute (+ -mirrored pieces)    cruiser Resolute with its coolant lines ruptured
  smoke-light, smoke-medium, coolant,           low-air: grey smoke decks and the white coolant
  lattice-beam, crane-jib                       banks of the heavy peak; the kit's beams and jibs
  wisps, spark-streaks, frost-streaks           high-air (additive)
  design/campaign/.../level-02-shipyard-burning/concept/backdrop-final-r15-a.png   review sheet

Level 01's production pieces (tools/art/backdrop_l01.py, loaded by path: the concept generator
has the same module name) render the shared kit unchanged; the new pieces use the same kit,
palette, key light and production bar (posterized to 12-32 colours, stepped translucency).

Run: python3 tools/art/backdrop_l02.py [--review] [id ...]   (~1 min on 20 cores)
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

from render import enemy_models as em  # noqa: E402
from render.sdf import sd_box, sd_capsule, sd_ellipsoid, sd_sphere, union  # noqa: E402
from render.station import ACCENT, DARK, HULL  # noqa: E402

_spec = importlib.util.spec_from_file_location("art_backdrop_l01", Path(__file__).resolve().parent / "backdrop_l01.py")
b1 = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(b1)
l01 = b1.l01

SCRIPT = "backdrop_l02.py"
SOURCE = artkit.source_note(SCRIPT, "M4 part B batch")
LEVEL_DIR = DESIGN / "campaign" / "act-1-first-contact" / "level-02-shipyard-burning"
OUT = ROOT / "assets" / "backdrop" / "level-02"
COLOURS = 32
SMOKE = np.array([150, 150, 158], float)


def tinted(img, colour, share):
    """An image's colours pulled towards ``colour`` by ``share``, alpha kept."""
    a = np.array(img.convert("RGBA")).astype(np.float64)
    a[..., :3] = a[..., :3] * (1 - share) + np.asarray(colour, float) * share
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


def smoke_banks(cover):
    def tile(w, h):
        return artkit.quantize_set([tinted(l01.banks(w, h, cover), SMOKE, 0.55)], 12)[0]
    return tile


def coolant(w, h):
    """The white coolant cloud of the heavy peak: dense, bright banks."""
    return artkit.quantize_set([tinted(l01.banks(w, h, 0.5), (232, 242, 250), 0.6)], 12)[0]


def frost_streaks(w, h):
    """Frost crystals streaming past, the spark streaks in icy white (additive)."""
    return artkit.stepped_alpha(tinted(l01.spark_streaks(w, h), (200, 235, 255), 0.85), 9)


def space(w, h):
    """Open space past the yard: deep blue-black with a fixed starfield, tiling in y."""
    rng = np.random.default_rng(1802)
    arr = np.zeros((h, w, 3))
    arr[:] = (4, 6, 18)
    for _ in range(260):
        x, y = rng.integers(0, w), rng.integers(0, h)
        b = rng.uniform(90, 230)
        arr[y, x] = (b * 0.9, b * 0.95, b)
    return artkit.quantize_set([Image.fromarray(arr.astype(np.uint8), "RGB").convert("RGBA")], 16)[0]


def north_arm_burning(w, h):
    """Level 01's north arm with fires along it and Aegis Two's tracer lines firing up past it."""
    base = np.array(b1.PIECES["north-arm"](w, h).convert("RGBA")).astype(np.float64) / np.array([1, 1, 1, 255])
    rng = np.random.default_rng(902)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64)
    for _ in range(7):
        x, y = rng.uniform(w * 0.3, w * 0.7), rng.uniform(40, h - 40)
        r = rng.uniform(4, 8)
        heat = np.exp(-((xx - x) ** 2 + (yy - y) ** 2) / (2 * r * r))
        step = np.clip(np.floor(heat * 6.2) - 1, -1, 5).astype(int)
        l01.over(base, l01.FIRE[np.clip(step, 0, 5)], (step >= 0) * (base[..., 3] > 0) * 1.0)
    for _ in range(4):
        x = rng.uniform(10, w - 10)
        y0 = rng.uniform(h * 0.4, h - 30)
        for i in range(int(rng.uniform(40, 90))):
            y = int(y0 - i * 3)
            if 0 <= y < h:
                base[y, int(x)] = (230, 240, 255, 0.9 * (1 - i / 90))
    return artkit.quantize_set([l01.rgba(base[..., :3], base[..., 3] * 255)], COLOURS)[0]


def growth_scene(seed, flip=False):
    """Teal Vrell growth: a cluster of glossy bulbs with glowing seams, on the dock's hull."""
    rng = np.random.default_rng(seed)
    blobs = [(rng.uniform(-60, 60) * (-1 if flip else 1), rng.uniform(-90, 90), rng.uniform(9, 20)) for _ in range(9)]

    def scene(p):
        items = [(sd_ellipsoid(p, (x, y, 4), (r, r * 0.8, r * 0.55)), em.V_BODY) for x, y, r in blobs]
        items += [(sd_sphere(p, (x, y, 4 + r * 0.45), r * 0.3), em.V_GLOW) for x, y, r in blobs[::2]]
        return union(*items, k=6.0)
    return scene


def dock_growth(w, h, flip=False):
    """An open dock frame with a half-built frigate in it, its hull overgrown with teal growth."""
    img = l01.blank(w, h)
    frame = b1.PIECES["dock-frame-mirrored" if flip else "dock-frame"](200, 260)
    img.alpha_composite(frame, ((w - 200) // 2, (h - 260) // 2))
    frigate = l01.cruiser_hull(120, 220)
    img.alpha_composite(frigate, ((w - 120) // 2, (h - 220) // 2))
    scene = growth_scene(77 if not flip else 78, flip)
    mats = em.vrell_scheme_mats("skitter", "a", 1.4)  # violet chitin, teal glow
    from render import sdf
    hi = sdf.render(scene, mats, (w * 2, h * 2), float(w), z_top=60.0, steps=110)
    growth = sprite.make_sprite(hi, 2, 24, crisp=60)
    img.alpha_composite(growth)
    return artkit.quantize_set([img], COLOURS)[0]


def resolute(w, h):
    """The cruiser Resolute in Dock Four: Level 01's hull at full size, coolant venting white from
    three ruptured lines (stepped translucency)."""
    img = l01.blank(w, h)
    hull = l01.cruiser_hull(220, 470)
    img.alpha_composite(hull, ((w - 220) // 2, (h - 470) // 2))
    arr = np.array(img).astype(np.float64) / np.array([1, 1, 1, 255])
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64)
    for bx, by, dx in ((w / 2 - 50, h * 0.35, -1), (w / 2 + 52, h * 0.55, 1), (w / 2 - 46, h * 0.7, -1)):
        u = (xx - bx) * dx
        v = yy - by
        jet = np.clip(1 - u / 70, 0, 1) * np.exp(-(v / (4 + np.clip(u, 0, None) * 0.25)) ** 2) * (u > 0)
        l01.over(arr, np.array([228, 240, 250], float), np.clip(jet * 0.8, 0, 0.75))
    arr = b1.dithered_alpha(arr, 5)
    return artkit.quantize_set([l01.rgba(arr[..., :3], arr[..., 3] * 255)], COLOURS)[0]


TILE_SETS = {
    "earth": b1.TILE_SETS["earth"], "space": space,
    "perimeter": b1.TILE_SETS["perimeter"], "dock-frames": b1.TILE_SETS["dock-frames"],
    "gantry-rails": b1.TILE_SETS["gantry-rails"],
    "smoke-light": smoke_banks(0.12), "smoke-medium": smoke_banks(0.22), "coolant": coolant,
    "wisps": b1.TILE_SETS["wisps"], "spark-streaks": b1.TILE_SETS["spark-streaks"], "frost-streaks": frost_streaks,
}
PIECES = {
    "north-arm-burning": north_arm_burning,
    "moon": b1.PIECES["moon"], "earth-limb": b1.PIECES["earth-limb"],
    "crossbeam": b1.PIECES["crossbeam"], "platform": b1.PIECES["platform"],
    "platform-burning": b1.PIECES["platform-burning"],
    "platform-burning-mirrored": b1.PIECES["platform-burning-mirrored"],
    "dock-growth": dock_growth, "dock-growth-mirrored": functools.partial(dock_growth, flip=True),
    "resolute": resolute,
    "lattice-beam": b1.PIECES["lattice-beam"], "crane-jib": b1.PIECES["crane-jib"],
    "crane-jib-mirrored": b1.PIECES["crane-jib-mirrored"],
}


def backdrop_data():
    return yaml.safe_load((LEVEL_DIR / "data.yaml").read_text(encoding="utf-8"))["backdrop"]


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


def review(backdrop):
    """Every tile set and piece on the dark plate, shrunk to fit."""
    items = []
    for name, spec in list(backdrop["tile_sets"].items()) + list(backdrop["pieces"].items()):
        count = spec.get("frames")
        path = OUT / (f"{name}_0.png" if count else f"{name}.png")
        img = Image.open(path).convert("RGBA")
        scale = min(1.0, 260 / max(img.size))
        img = img.resize((max(1, int(img.width * scale)), max(1, int(img.height * scale))), Image.NEAREST)
        items.append((name, img))
    width, x, y, row_h = 1300, 16, 44, 0
    rows = []
    for name, img in items:
        if x + img.width > width - 16:
            x, y = 16, y + row_h + 30
            row_h = 0
        rows.append((name, img, x, y))
        x += img.width + 14
        row_h = max(row_h, img.height)
    artkit.REVIEW_ROUND = "r15"
    sheet = raster.sheet(width, y + row_h + 40, "LEVEL 02 BACKDROP - FINAL PIECES", "PRODUCTION ART, M4 PART B BATCH - R15")
    for name, img, px, py in rows:
        plate = Image.new("RGBA", img.size, artkit.PLATE)
        plate.alpha_composite(img)
        sheet.alpha_composite(plate, (px, py + 14))
        raster.draw_text(sheet, px, py, name.upper(), raster.LABEL)
    path = LEVEL_DIR / "concept" / "backdrop-final-r15-a.png"
    sheet.convert("RGB").save(path, optimize=True)
    print(f"review: {path.relative_to(ROOT)}")


def main(argv):
    backdrop = backdrop_data()
    wanted = {a for a in argv if a != "--review"}
    missing = (backdrop["tile_sets"].keys() | backdrop["pieces"].keys()) - (TILE_SETS.keys() | PIECES.keys())
    if missing:
        raise SystemExit(f"no generator for {sorted(missing)}")
    if "--review" not in argv:
        OUT.mkdir(parents=True, exist_ok=True)
        todo = [job for job in b1.jobs(backdrop, wanted)]
        with ProcessPoolExecutor() as pool:
            for job, images in zip(todo, pool.map(render, todo)):
                write(job, images)
    review(backdrop)


if __name__ == "__main__":
    main(sys.argv[1:])
