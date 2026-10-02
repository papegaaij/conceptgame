#!/usr/bin/env python3
"""Production art: the hangar intel's sensor-L2 pictures (design/ui/hangar, design/player/systems
"Sensor levels and hangar intel": enemy types with portraits, boss name and silhouette).

Outputs (assets/sprites/intel/, packed onto the shared sprite pages as ``intel/<name>``):
  <enemy>.png        30x30 sensor portrait of an enemy type of Levels 01-02: skitter, needler,
                     stinger, spine-turret
  boss-<boss>.png    40x40 sensor silhouette of a boss of Act 1: gorgon-frigate (L05 mid-boss),
                     brood-carrier (L07)
  design/ui/hangar/concept/intel-final-r13-a.png   review sheet

The names are the enemy's or boss's name as a slug (vanguard.game.render.Portraits.slug). A
portrait is the unit's chosen round-04 model (tools/concept/enemies_r04.py, imported unchanged) in
its own colours, nose down as it comes at the player, ray-marched at 8x through the sprite path
(1-bit alpha, unsharp mask) onto the intel's sensor plate: a dark teal screen with a dot grid, a
cyan scan line and corner brackets, lightly tinted cyan as the scan sees it, 32 colours. A boss
silhouette shows what L2 knows of it: the outline only, the shape filled flat in dark teal with a
bright cyan rim and the plate's grid running through it, from the chosen models (the Brood
Carrier of round 04, the Gorgon Frigate's bell, necks and heads of round 06 at rest).

Run: python3 tools/art/intel.py [--review]   (~40 s; --review only rebuilds the sheet)
"""
import sys
from concurrent.futures import ProcessPoolExecutor

import numpy as np
from PIL import Image

import artkit
from artkit import DESIGN, ROOT, SPRITES, sprite

import bosses_r06  # noqa: E402  (concept scripts, imported unchanged)
import enemies_r03 as e3  # noqa: E402
import enemies_r04 as e4  # noqa: E402
from render import enemy_models as em  # noqa: E402
from render import raster, sdf  # noqa: E402

SCRIPT = "intel.py"
SOURCE = artkit.source_note(SCRIPT, "UI batch")
ROUND = "r13"
OUT = SPRITES / "intel"
CONCEPT = DESIGN / "ui" / "hangar" / "concept"
PORTRAIT = 30
SILHOUETTE = 40
COLOURS = 32
ENEMIES = {"skitter": "skitter-a", "needler": "needler-a", "stinger": "stinger-a", "spine-turret": "spine-turret-a"}
BOSSES = ("gorgon-frigate", "brood-carrier")
PLATE = np.array([4, 16, 28], float)
GRID = np.array([16, 60, 80], float)
SCAN = np.array([60, 220, 255], float)
SHAPE = np.array([10, 52, 66], float)


def plate(n):
    """The sensor plate: dark teal with a dot grid, a scan line at two thirds and corner brackets."""
    yy, xx = np.mgrid[0:n, 0:n]
    img = np.empty((n, n, 3))
    img[...] = PLATE
    img[(xx % 5 == 2) & (yy % 5 == 2)] = GRID
    img[2 * n // 3] = PLATE * 0.5 + SCAN * 0.35
    arm = max(3, n // 8)
    for cy, cx in ((0, 0), (0, n - 1), (n - 1, 0), (n - 1, n - 1)):
        ys = slice(cy, cy + arm) if cy == 0 else slice(cy - arm + 1, cy + 1)
        xs = slice(cx, cx + arm) if cx == 0 else slice(cx - arm + 1, cx + 1)
        img[cy, xs] = SCAN
        img[ys, cx] = SCAN
    return img


def portrait(slug):
    scene, mats = e4.R04[ENEMIES[slug]][4](0.0, 1.0)
    hi = sdf.render(scene, mats, (PORTRAIT * 8, PORTRAIT * 8), e3.EXTENT * 1.06)
    unit = np.array(artkit.native(hi, 8)).astype(np.float64)
    a = unit[..., 3:4] / 255
    rgb = unit[..., :3] * 0.82 + SCAN * 0.12 * (unit[..., :3].mean(-1, keepdims=True) / 255 + 0.3)
    card = plate(PORTRAIT) * (1 - a) + rgb * a
    img = Image.fromarray(np.clip(card, 0, 255).astype(np.uint8), "RGB").convert("RGBA")
    return artkit.quantize_set([img], COLOURS)[0]


def gorgon_mask():
    layer = Image.new("RGBA", (480, 480), (0, 0, 0, 0))
    bosses_r06.gorgon_compose(layer, 240, 190, np.pi / 2, 0.0, [np.pi / 2] * 3)
    return np.array(layer)[..., 3] > 127


def carrier_mask():
    scene, mats = em.brood_carrier(0.0, 1.0, 0.0, 0.0)
    hi = sdf.render(scene, mats, (e3.BW, e3.BH), e3.EXTENT)
    return hi[..., 3] > 0.5


def silhouette(slug):
    mask = {"gorgon-frigate": gorgon_mask, "brood-carrier": carrier_mask}[slug]()
    ys, xs = np.nonzero(mask)
    mask = mask[ys.min():ys.max() + 1, xs.min():xs.max() + 1]
    inner = SILHOUETTE - 6
    scale = inner / max(mask.shape)
    size = (max(1, round(mask.shape[1] * scale)), max(1, round(mask.shape[0] * scale)))
    small = np.array(Image.fromarray((mask * 255).astype(np.uint8)).resize(size, Image.BOX)) > 110
    shape = np.zeros((SILHOUETTE, SILHOUETTE), bool)
    y0, x0 = (SILHOUETTE - size[1]) // 2, (SILHOUETTE - size[0]) // 2
    shape[y0:y0 + size[1], x0:x0 + size[0]] = small
    card = plate(SILHOUETTE)
    yy, xx = np.mgrid[0:SILHOUETTE, 0:SILHOUETTE]
    card[shape] = SHAPE
    card[shape & ((xx % 5 == 2) | (yy % 5 == 2))] = SHAPE * 0.6 + SCAN * 0.25
    pad = np.pad(shape, 1)
    rim = shape & ~(pad[:-2, 1:-1] & pad[2:, 1:-1] & pad[1:-1, :-2] & pad[1:-1, 2:])
    card[rim] = SCAN
    return Image.fromarray(card.astype(np.uint8), "RGB").convert("RGBA")


def render(job):
    kind, slug = job
    return portrait(slug) if kind == "enemy" else silhouette(slug)


def build():
    OUT.mkdir(parents=True, exist_ok=True)
    for old in OUT.glob("*.png"):
        old.unlink()
    jobs = [("enemy", slug) for slug in ENEMIES] + [("boss", slug) for slug in BOSSES]
    with ProcessPoolExecutor() as pool:
        for (kind, slug), img in zip(jobs, pool.map(render, jobs)):
            artkit.save_png(img, OUT / (f"boss-{slug}.png" if kind == "boss" else f"{slug}.png"), SOURCE)
    print(f"{len(jobs)} intel pictures in {OUT.relative_to(ROOT)}")


def review():
    sheet = raster.sheet(16 + 6 * 180 + 16, 60 + 150 + 20, "HANGAR INTEL (FINAL R13): SENSOR L2 PICTURES",
                         f"PRODUCTION ART, UI BATCH - {ROUND.upper()}")
    raster.draw_text(sheet, 16, 38, f"ENEMY PORTRAITS {PORTRAIT}X{PORTRAIT} AND BOSS SILHOUETTES "
                                    f"{SILHOUETTE}X{SILHOUETTE}, AT 1X AND 3X", raster.LABEL)
    names = list(ENEMIES) + [f"boss-{slug}" for slug in BOSSES]
    for i, name in enumerate(names):
        img = Image.open(OUT / f"{name}.png").convert("RGBA")
        x, y = 16 + i * 180, 56
        sheet.alpha_composite(img, (x, y))
        sheet.alpha_composite(sprite.enlarge(img, 3), (x + img.width + 6, y))
        raster.draw_text(sheet, x, y + 3 * img.height + 4,
                         f"{name.upper()} ({artkit.colour_count([img])} COL)", raster.LABEL_DIM)
    path = CONCEPT / f"intel-final-{ROUND}-a.png"
    sheet.convert("RGB").save(path, optimize=True)
    print(f"review: {path.relative_to(ROOT)}")


if __name__ == "__main__":
    if "--review" not in sys.argv[1:]:
        build()
    review()
