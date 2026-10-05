#!/usr/bin/env python3
"""Production art: the boss bar's plate (design/ui/hud, boss bar; M4 part H batch, concept round
26), in the in-game HUD's metal look (tools/art/hud.py: palette B's UTC HULL steel, bevelled
edges, domed rivets in recessed washers, a recessed near-black LCD well).

Outputs (assets/sprites/hud/, packed into the shared sprites atlas as ``hud/boss-bar-plate``):
  boss-bar-plate.9.png   64x20 libGDX nine-patch (66x22 with its marks): a steel strip with a
                         2 px chamfer, an end cap at each end with a domed rivet, and a recessed
                         trough (1 px chamfer) whose glass is the bar's well. Splits (stretched
                         columns and rows, the top and left marks): left 14, right 14, top 7,
                         bottom 7; padding (the content box, the bottom and right marks): left 11,
                         right 11, top 7, bottom 7, which is the glass of the trough, so the fill
                         drawn in the content box is the bar (6 px tall). Every stretched column is
                         the same (the brushing is averaged out of the middle), so the plate
                         stretches cleanly to any width: 422 px round the 400 px act-boss bar, 262
                         round the 240 px mid-boss bar. Drawn at its own 20 px height.
  design/ui/hud/concept/boss-bar-plate-final-r26-a.png   review sheet (no GIF: nothing moves)

Rendered like hud.py's plate and wells (SDF at 8x, the HUD's low top-left key light, mapped by
shading through the steel ramp, the glass through the UI PANELS navy), undithered since it
stretches. The PNG carries the ``Source`` chunk.

Run: python3 tools/art/boss_bar.py [--review]   (~5 s)
"""
import importlib.util
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

import artkit
from artkit import DESIGN

# tools/art/hud.py (the HUD's steel, wells and light); a concept script of that name is on the path
# first, so it is loaded by its path
_spec = importlib.util.spec_from_file_location("art_hud", Path(__file__).resolve().parent / "hud.py")
hud = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(hud)
from render import raster, sdf  # noqa: E402
from render.sdf import Material  # noqa: E402

SCRIPT = "boss_bar.py"
SOURCE = artkit.source_note(SCRIPT, "M4 part H batch")
ROUND = "r26"
OUT = artkit.SPRITES / "hud"
CONCEPT = DESIGN / "ui" / "hud" / "concept"

SIZE = (64, 20)
SPLIT = (14, 14, 7, 7)                          # left, right, top, bottom (stretched columns / rows)
PAD = (11, 11, 7, 7)                            # the content box: the trough's glass
TROUGH = dict(half_w=22.0, half_h=4.0, chamfer=1.0, depth=2.0)   # the well, centred on the plate
RIVET_X = 5.5                                   # the rivets' centres from the ends, px
FILL = (224, 60, 40)                            # the bar's red (BossBar.FILL), for the review
EMPTY = (58, 20, 20)


def scene(p):
    w, h = SIZE
    plate = artkit.chamfered_box(p, (0, 0, -1.5), (w / 2, h / 2, 1.5), 2.0)
    well, glass = hud.recess(p, TROUGH["half_w"], TROUGH["half_h"], TROUGH["chamfer"], TROUGH["depth"])
    d = np.maximum(plate, well)
    m = np.where(glass == 0, 0, 1).astype(np.int32)
    for x in (-w / 2 + RIVET_X, w / 2 - RIVET_X):
        d = np.maximum(d, -sdf.sd_cylinder_z(p, (x, 0, 0), 3.2, 0.5))      # recessed washer
        rivet = sdf.sd_sphere(p, (x, 0, -1.2), 2.3)                        # domed head
        m = np.where(rivet < d, 2, m)
        d = np.minimum(d, rivet)
    return d, m


def plate():
    w, h = SIZE
    factor = artkit.factor_for(w, h)
    mats = [hud.GLASS_MATS[0]] + hud.steel_mats(SIZE, factor, 47)[1:2] + [hud.STEEL_MATS[2]]
    hi = sdf.render(scene, mats, (w * factor, h * factor), w, z_top=3.0, key_pos=hud.far_light(w), **hud.SHADING)
    hi[..., 3] = 1.0                                                       # opaque to the plate's edge
    img = artkit.native(hi, factor, crisp=0)
    lum = hud.luminance(img)
    yy, xx = np.mgrid[0:h, 0:w]
    l, r, t, b = PAD
    glass_px = (xx >= l) & (xx < w - r) & (yy >= t) & (yy < h - b)
    lit = lum[h // 2, w // 2]
    rel = np.round(np.clip(lum / max(lit, 1e-6), 0, 1) * 3) / 3              # glass in four steps
    rgb = np.where(glass_px[..., None], raster.ramp(hud.GLASS, rel), hud.steel(lum, hud.PLATE_SHADE))
    sl, sr = SPLIT[0], SPLIT[1]
    mid = np.median(rgb[:, sl:w - sr], axis=1, keepdims=True)              # stretch-clean middle
    rgb[:, sl:w - sr] = np.round(mid)
    return raster.to_rgba_image(rgb)


def nine_patch(img):
    """The libGDX nine-patch file: the top and left marks are the stretched columns and rows, the
    bottom and right marks the content box (padding)."""
    w, h = img.size
    left, right, top, bottom = SPLIT
    pl, pr, pt, pb = PAD
    out = Image.new("RGBA", (w + 2, h + 2), (0, 0, 0, 0))
    out.paste(img, (1, 1))
    d = ImageDraw.Draw(out)
    black = (0, 0, 0, 255)
    d.line([(1 + left, 0), (w - right, 0)], fill=black)
    d.line([(0, 1 + top), (0, h - bottom)], fill=black)
    d.line([(1 + pl, h + 1), (w - pr, h + 1)], fill=black)
    d.line([(w + 1, 1 + pt), (w + 1, h - pb)], fill=black)
    return out


def build():
    OUT.mkdir(parents=True, exist_ok=True)
    img = plate()
    artkit.save_png(nine_patch(img), OUT / "boss-bar-plate.9.png", SOURCE)
    print(f"boss-bar-plate.9.png: {img.width}x{img.height}, {artkit.colour_count([img])} colours")


# =========================================================================== review

def drawn(width, share):
    """The plate round a bar ``width`` wide as BossBar draws it: stretched, the dim trough and the
    red fill in the content box."""
    img = hud.unpatch(Image.open(OUT / "boss-bar-plate.9.png").convert("RGBA"))
    l, r, t, b = PAD
    total = width + l + r
    out = hud.stretch(img, SPLIT, (total, img.height))
    fill = hud.load("fill")
    h = img.height - t - b
    lit = round(width * share)
    if lit >= 2:
        out.alpha_composite(hud.stretch(fill, hud.FILL_SPLIT, (lit, h), FILL + (255,)), (l, t))
    return out


def review():
    raw = Image.open(OUT / "boss-bar-plate.9.png").convert("RGBA")
    img = hud.unpatch(raw)
    sheet = raster.sheet(1300, 520, "BOSS BAR PLATE (FINAL R26): HUD STEEL, RIVETED END CAPS, RECESSED TROUGH",
                         f"PRODUCTION ART, M4 PART H BATCH - {ROUND.upper()}")
    raster.draw_text(sheet, 16, 38, f"NINE-PATCH {img.width}X{img.height} AT 8X ({artkit.colour_count([img])} COLOURS): "
                                    "SPLITS (MAGENTA) L14 R14 T7 B7, CONTENT BOX (CYAN) L11 R11 T7 B7 = THE FILL", raster.LABEL)
    cell = artkit.on_background(img, 8)
    d = ImageDraw.Draw(cell)
    l, r, t, b = (v * 8 for v in SPLIT)
    for gx in (l, cell.width - r):
        d.line([(gx, 0), (gx, cell.height)], fill=(255, 60, 200, 255))
    for gy in (t, cell.height - b):
        d.line([(0, gy), (cell.width, gy)], fill=(255, 60, 200, 255))
    pl, pr, pt, pb = (v * 8 for v in PAD)
    d.rectangle([pl, pt, cell.width - pr - 1, cell.height - pb - 1], outline=(60, 230, 255, 255))
    sheet.alpha_composite(cell, (16, 54))
    raster.draw_text(sheet, 560, 54, "THE RAW .9.PNG WITH ITS MARKS, 4X", raster.LABEL_DIM)
    sheet.alpha_composite(artkit.on_background(raw, 4), (560, 68))
    y = 240
    for label, width, share in (("ACT BOSS, 400 PX BAR (PLATE 422), 70 % LEFT", 400, 0.7),
                                ("MID-BOSS, 240 PX BAR (PLATE 262), 35 % LEFT", 240, 0.35)):
        raster.draw_text(sheet, 16, y, f"{label}: AS BOSSBAR DRAWS IT, 1X AND 2X", raster.LABEL)
        bar = drawn(width, share)
        strip = Image.new("RGBA", (480, 40), (12, 14, 30, 255))
        strip.alpha_composite(bar, ((480 - bar.width) // 2, 14))
        sheet.alpha_composite(strip, (16, y + 14))
        sheet.alpha_composite(artkit.sprite.enlarge(strip, 2), (520, y + 14))
        y += 120
    path = CONCEPT / f"boss-bar-plate-final-{ROUND}-a.png"
    sheet.convert("RGB").save(path, optimize=True)
    print(f"review: {path.relative_to(artkit.ROOT)}")


if __name__ == "__main__":
    if "--review" not in sys.argv[1:]:
        build()
    review()
