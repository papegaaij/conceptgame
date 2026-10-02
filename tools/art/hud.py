#!/usr/bin/env python3
"""Production art: the in-game HUD's metal parts (design/ui/hud), in the chosen HUD A look
(hud-r02-a, hud-r08-a and the metal widgets of ui-kit-r08-a): brushed violet-blue steel of
palette B's UTC HULL ramp, bevelled edges, corner rivets, recessed near-black LCD wells.

Outputs (assets/sprites/hud/, packed into the shared sprites atlas as ``hud/<name>``):
  panel-left.png, panel-right.png   240x540 side-panel plates, whole (the screen is a fixed
                                    960x540, so they never stretch): a plate with a 3 px chamfer
                                    on a dark chassis, four domed corner rivets in recessed
                                    washers, brushed streaks and mottling; the two panels differ
                                    only in their noise seed
  plate.png                         120x22 raised label plate, darker steel, chamfered 2 px
  well.9.png                        32x32 nine-patch (6 px corners): an LCD well recessed into
                                    the panel, 2 px chamfer (top and left in shade, bottom and
                                    right lit) and the lip's shadow on the glass; the wells, the
                                    weapon box, the tracker and the bar troughs
  portrait.9.png                    76x76 nine-patch (6 px corners): the radio portrait's well,
                                    with an idle CRT glass (scanlines, a reflection) that the
                                    72x72 portrait covers while a message plays
  fill.9.png                        8x8 nine-patch (1 px sides, 2 px top and bottom), grey: a
                                    phosphor bar cell with a lit top edge, tinted at runtime for
                                    every bar, segment, pip and flash
  glow.9.png                        24x24 nine-patch (10 px corners), white, alpha stepped to four
                                    levels: the phosphor glow behind a readout, tinted green or
                                    amber by the readout's colour at runtime
  design/ui/hud/concept/hud-final-r13-a.png   review sheet

The solid pieces are SDF ray-marched (tools/concept/render/sdf.py) at the quality bar's factor
(8x up to 64 px, 4x above) with the key light from the top-left, but lower than the sprites' (so
a 45-degree bevel reads), then mapped by their shading through palette B's UTC HULL ramp (the
glass through the UI PANELS navy); the wide panel faces are posterized with 4x4 ordered dither,
everything that stretches is undithered so a stretched edge stays clean. The fill and glow are 2D
light fields. Every PNG carries the ``Source`` chunk.

Run: python3 tools/art/hud.py [--review]   (~30 s; --review only rebuilds the sheet)
"""
import sys
from concurrent.futures import ProcessPoolExecutor
from functools import lru_cache

import numpy as np
from PIL import Image, ImageDraw

import artkit
from artkit import DESIGN, sprite

from render import raster, sdf  # noqa: E402
from render.palette import B  # noqa: E402
from render.sdf import Material, vec  # noqa: E402

SCRIPT = "hud.py"
SOURCE = artkit.source_note(SCRIPT, "UI batch")
OUT = artkit.SPRITES / "hud"
CONCEPT = DESIGN / "ui" / "hud" / "concept"
ROUND = "r13"

PANEL = (240, 540)
PLATE = (120, 22)
WELL = (32, 32)
PORTRAIT = (76, 76)
FILL = (8, 8)
GLOW = (24, 24)
# nine-patch splits (left, right, top, bottom)
WELL_SPLIT = (6, 6, 6, 6)
FILL_SPLIT = (1, 1, 2, 2)
GLOW_SPLIT = (10, 10, 10, 10)

GLOW_ALPHA = 28                    # the game draws the glow at this alpha (of 255), HudKit.GLOW_ALPHA
LIGHT = vec(-2.2, 2.6, 1.7)        # top-left, lower than the sprites' key light (elevation ~27 deg)
STRIPS = 12                        # the panels render in horizontal strips, in parallel
CHAMFER = 3.0                      # the panel plate's bevel, px
RIVET_INSET = 10                   # rivet centres from the plate's corners, px
LEVELS = 28                        # posterization steps on the steel ramp

METAL = B["UTC HULL"]              # 121632 2a3068 4e5aa0 8a96d0 c8d0f4 ffffff
STEEL = [(i / 5, c) for i, c in enumerate(METAL)]
# shading (luminance of the render) -> position on the steel ramp; the panel face lands between
# 4e5aa0 and 8a96d0 at the 0.82 darkening of the concept's panel, the bevels at the ends
SHADE = ([0.0, 0.12, 0.30, 0.42, 0.55, 0.62, 0.75, 1.0], [0.0, 0.06, 0.22, 0.34, 0.45, 0.62, 0.78, 0.92])
PLATE_SHADE = ([0.0, 0.12, 0.30, 0.42, 0.55, 0.62, 0.75, 1.0], [0.0, 0.04, 0.14, 0.22, 0.30, 0.48, 0.66, 0.85])
GLASS = [(0.0, (2, 2, 10)), (1.0, (8, 9, 32))]                 # LCD: shade .. lit, UI PANELS navy
CRT = [(0.0, (1, 6, 4)), (1.0, (6, 22, 14)), (1.6, (22, 52, 38))]  # idle portrait glass, green-black; reflection

STEEL_MATS = [Material((0.12, 0.12, 0.14)),                                    # chassis / glass
              Material((0.8, 0.8, 0.8), metal=0.05, shininess=24, spec=0.35),   # steel
              Material((0.85, 0.85, 0.85), metal=0.4, shininess=60, spec=0.9)]  # rivet heads
GLASS_MATS = [Material((0.8, 0.8, 0.8), spec=0.0), STEEL_MATS[1]]             # glass, steel
SHADING = dict(ambient=0.12, fill=0.2)


def far_light(extent):
    """``key_pos`` for a light so far away that it is directional across a small piece."""
    return tuple(LIGHT * 400 / extent * 2.3)


def chamfered_box(p, centre, half, chamfer):
    """A box whose top edges (only) are chamfered at 45 degrees by ``chamfer``."""
    q = np.abs(p - vec(*centre)) - vec(*half)
    box = sdf.length(np.maximum(q, 0.0)) + np.minimum(np.max(q, axis=-1), 0.0)
    top = p[:, 2] - (centre[2] + half[2])
    return np.maximum.reduce([box, (q[:, 0] + top + chamfer) / np.sqrt(2), (q[:, 1] + top + chamfer) / np.sqrt(2)])


def recess(p, half_w, half_h, chamfer, depth):
    """The surface z <= 0 with a well cut into it: walls chamfered 45 degrees for ``chamfer``,
    then straight down to the glass at ``-depth``. Material 1 is the steel, 0 the glass."""
    lip = np.maximum(p[:, 2] + chamfer, 0.0)
    walls = np.maximum(np.abs(p[:, 0]) - (half_w - chamfer) - lip, np.abs(p[:, 1]) - (half_h - chamfer) - lip)
    hole = np.maximum(walls / np.sqrt(2), -(p[:, 2] + depth))
    d = np.maximum(p[:, 2], -hole)
    return d, np.where(p[:, 2] < -chamfer - 0.25, 0, 1)


# --------------------------------------------------------------------------- brushed steel

@lru_cache(maxsize=4)
def brushed(width, height, factor, seed):
    """Albedo multiplier at render resolution: horizontal brush streaks (one value per render row,
    drifting along x every 16 px) and large soft mottling."""
    w, h = width * factor, height * factor
    rng = np.random.default_rng(seed)
    step = 16 * factor
    knots = rng.random((h, w // step + 2))
    x = np.arange(w) / step
    k = x.astype(int)
    f = x - k
    streak = knots[:, k] * (1 - f) + knots[:, k + 1] * f
    streak = (np.roll(streak, 1, 0) + 2 * streak + np.roll(streak, -1, 0)) / 4
    mottle = raster.fbm(w, h, 40 * factor, seed + 5, octaves=3, period=False)
    return 1 + 0.10 * (streak - 0.5) + 0.12 * (mottle - 0.5)


def brush_pattern(width, height, factor, seed):
    def pattern(p, _n):
        tex = brushed(width, height, factor, seed)
        ix = np.clip(((p[:, 0] + width / 2) * factor).astype(int), 0, width * factor - 1)
        iy = np.clip(((height / 2 - p[:, 1]) * factor).astype(int), 0, height * factor - 1)
        return tex[iy, ix]
    return pattern


def steel_mats(size, factor, seed):
    mats = list(STEEL_MATS)
    s = mats[1]
    mats[1] = Material(s.albedo, s.metal, s.shininess, s.spec, pattern=brush_pattern(*size, factor, seed))
    return mats


# --------------------------------------------------------------------------- panels

def rivets(w, h, inset):
    return [(sx * (w / 2 - inset), sy * (h / 2 - inset)) for sx in (-1, 1) for sy in (-1, 1)]


def panel_scene(p):
    w, h = PANEL
    chassis = p[:, 2] + CHAMFER
    plate = chamfered_box(p, (0, 0, -CHAMFER / 2), (w / 2 - 1, h / 2 - 1, CHAMFER / 2), CHAMFER)
    d, m = sdf.union((chassis, 0), (plate, 1))
    for x, y in rivets(w, h, RIVET_INSET):
        d = np.maximum(d, -sdf.sd_cylinder_z(p, (x, y, 0), 5.5, 0.6))         # recessed washer
        d, m = sdf.union((d, m), (sdf.sd_sphere(p, (x, y, -1.6), 4.0), 2))    # domed head
    return d, m


def panel_strip(seed, index):
    """One horizontal strip of a panel at 4x; the light is the panel's, wherever the strip is."""
    w, h = PANEL
    factor = artkit.factor_for(w, h)
    rows = h // STRIPS
    centre = vec(0.0, h / 2 - (index + 0.5) * rows, 0.0)
    light = (LIGHT * 3 * w / 2.3 - centre) / (w / 2.3)
    return sdf.render(panel_scene, steel_mats(PANEL, factor, seed), (w * factor, rows * factor), w,
                      center=(0.0, centre[1]), z_top=4.0, key_pos=tuple(light), **SHADING)


def luminance(img):
    return np.array(img.convert("RGB")).astype(np.float64) @ np.array([0.3, 0.59, 0.11]) / 255


def steel(lum, shade, dither_mask=None):
    """Shading -> the steel ramp, posterized to LEVELS steps (ordered dither where masked)."""
    t = np.interp(lum, *shade)
    flat = np.round(t * (LEVELS - 1)) / (LEVELS - 1)
    if dither_mask is not None:
        flat = np.where(dither_mask, artkit.ordered_dither(t, LEVELS), flat)
    return raster.ramp(STEEL, flat)


def panel(seed, strips):
    w, h = PANEL
    factor = artkit.factor_for(w, h)
    img = artkit.native(np.concatenate(strips, axis=0), factor)
    yy, xx = np.mgrid[0:h, 0:w]
    edge = 2 + int(CHAMFER)
    face = (xx >= edge) & (xx < w - edge) & (yy >= edge) & (yy < h - edge)
    for x, y in rivets(w, h, RIVET_INSET):
        face &= (xx + 0.5 - (x + w / 2)) ** 2 + (yy + 0.5 - (h / 2 - y)) ** 2 > 7.5 ** 2
    return raster.to_rgba_image(steel(luminance(img), SHADE, face))


# --------------------------------------------------------------------------- small pieces

def plate():
    w, h = PLATE
    factor = artkit.factor_for(w, h)
    def scene(p):
        return chamfered_box(p, (0, 0, -1.5), (w / 2, h / 2, 1.5), 2.0), np.ones(len(p), dtype=np.int32)
    hi = sdf.render(scene, steel_mats(PLATE, factor, 31), (w * factor, h * factor), w, z_top=3.0,
                    key_pos=far_light(w), **SHADING)
    hi[..., 3] = 1.0                       # the chamfer reaches the plate's edge: opaque throughout
    img = artkit.native(hi, factor)
    yy, xx = np.mgrid[0:h, 0:w]
    face = (xx >= 3) & (xx < w - 3) & (yy >= 3) & (yy < h - 3)
    return raster.to_rgba_image(steel(luminance(img), PLATE_SHADE, face))


def well(size, glass, scanlines=False):
    """A recessed well of ``size``: steel chamfer mapped like the panel, glass through ``glass``."""
    w, h = size
    factor = artkit.factor_for(w, h)
    def scene(p):
        return recess(p, w / 2, h / 2, 2.0, 3.0)
    hi = sdf.render(scene, GLASS_MATS, (w * factor, h * factor), w, z_top=4.0, key_pos=far_light(w), **SHADING)
    hi[..., 3] = 1.0
    img = artkit.native(hi, factor, crisp=0)
    lum = luminance(img)
    yy, xx = np.mgrid[0:h, 0:w]
    inner = (xx >= 2) & (xx < w - 2) & (yy >= 2) & (yy < h - 2)
    lit = lum[h // 2, w // 2]
    rel = np.clip(lum / lit, 0, 1)
    rel = np.round(rel * 3) / 3                            # glass in four steps: shade .. lit
    if scanlines:
        rel = rel * np.where(yy % 2 == 1, 0.55, 1.0)
        # the idle tube's reflection: a soft diagonal band from the top-left, stepped
        band = np.clip(1 - np.abs((xx + yy) - 0.45 * (w + h)) / (0.18 * (w + h)), 0, 1)
        rel = np.clip(rel + np.floor(band * 3) / 3 * 0.6 * (yy % 2 == 0), 0, 1.6)
    glass_rgb = raster.ramp(glass, rel)
    rgb = np.where(inner[..., None], glass_rgb, steel(lum, SHADE))
    return raster.to_rgba_image(rgb)


def fill():
    """The phosphor bar cell, grey, tinted at runtime: lit top edge, darker bottom and ends."""
    rows = np.array([1.0, 0.9, 0.8, 0.8, 0.8, 0.8, 0.66, 0.54])
    v = np.tile(rows[:, None], (1, FILL[0]))
    v[:, 0] *= 0.82
    v[:, -1] *= 0.82
    grey = np.round(v * 255)
    return Image.fromarray(np.dstack([grey, grey, grey, np.full(v.shape, 255)]).astype(np.uint8), "RGBA")


def glow():
    """The readout glow: white, strongest along the middle, falling off over the 10 px corners;
    alpha stepped to four levels (artkit.stepped_alpha)."""
    w, h = GLOW
    yy, xx = np.mgrid[0:h, 0:w] + 0.5
    reach = GLOW_SPLIT[0]
    dx = np.clip(np.minimum(xx, w - xx) / reach, 0, 1)
    dy = np.clip(np.minimum(yy, h - yy) / reach, 0, 1)
    a = (dx * dy) ** 0.8
    img = Image.fromarray(np.dstack([np.full((h, w, 3), 255), np.round(a * 255)]).astype(np.uint8), "RGBA")
    return artkit.stepped_alpha(img)


def nine_patch(img, split):
    """The libGDX nine-patch file: the image inside a 1 px border whose black marks on the top
    and left edges are the stretched columns and rows (TexturePacker turns them into ``split``)."""
    w, h = img.size
    left, right, top, bottom = split
    out = Image.new("RGBA", (w + 2, h + 2), (0, 0, 0, 0))
    out.paste(img, (1, 1))
    d = ImageDraw.Draw(out)
    d.line([(1 + left, 0), (w - right, 0)], fill=(0, 0, 0, 255))
    d.line([(0, 1 + top), (0, h - bottom)], fill=(0, 0, 0, 255))
    return out


def unpatch(img):
    return img.crop((1, 1, img.width - 1, img.height - 1))


# --------------------------------------------------------------------------- build

def build():
    OUT.mkdir(parents=True, exist_ok=True)
    with ProcessPoolExecutor() as pool:
        jobs = [(seed, i) for seed in (11, 23) for i in range(STRIPS)]
        strips = list(pool.map(panel_strip, *zip(*jobs)))
        small = {name: pool.submit(fn, *args) for name, fn, args in [
            ("plate", plate, ()),
            ("well", well, (WELL, GLASS)),
            ("portrait", well, (PORTRAIT, CRT, True)),
        ]}
        pieces = {"panel-left": panel(11, strips[:STRIPS]), "panel-right": panel(23, strips[STRIPS:])}
        pieces.update({name: job.result() for name, job in small.items()})
    for name in ("panel-left", "panel-right", "plate"):
        artkit.save_png(pieces[name], OUT / f"{name}.png", SOURCE)
    for name, img, split in [("well", pieces["well"], WELL_SPLIT), ("portrait", pieces["portrait"], WELL_SPLIT),
                             ("fill", fill(), FILL_SPLIT), ("glow", glow(), GLOW_SPLIT)]:
        artkit.save_png(nine_patch(img, split), OUT / f"{name}.9.png", SOURCE)
    for name in sorted(p.name for p in OUT.glob("*.png")):
        img = Image.open(OUT / name)
        print(f"{name}: {img.width}x{img.height}, {artkit.colour_count([img.convert('RGBA')])} colours")


# --------------------------------------------------------------------------- review

def load(name):
    path = OUT / f"{name}.png"
    if path.exists():
        return Image.open(path).convert("RGBA")
    return unpatch(Image.open(OUT / f"{name}.9.png").convert("RGBA"))


def stretch(img, split, size, tint=None):
    """A nine-patch drawn at ``size`` the way libGDX does: corners as they are, edges and centre
    stretched (nearest), optionally tinted (multiplied)."""
    left, right, top, bottom = split
    w, h = img.size
    tw, th = size
    xs = [(0, left, 0, left), (left, w - right, left, tw - right), (w - right, w, tw - right, tw)]
    ys = [(0, top, 0, top), (top, h - bottom, top, th - bottom), (h - bottom, h, th - bottom, th)]
    out = Image.new("RGBA", size, (0, 0, 0, 0))
    for sx0, sx1, dx0, dx1 in xs:
        for sy0, sy1, dy0, dy1 in ys:
            if sx1 > sx0 and sy1 > sy0 and dx1 > dx0 and dy1 > dy0:
                out.paste(img.crop((sx0, sy0, sx1, sy1)).resize((dx1 - dx0, dy1 - dy0), Image.NEAREST), (dx0, dy0))
    if tint is not None:
        a = np.array(out).astype(np.float64)
        a[..., :3] *= np.array(tint[:3]) / 255
        if len(tint) == 4:
            a[..., 3] *= tint[3] / 255
        out = Image.fromarray(np.round(a).astype(np.uint8), "RGBA")
    return out


def bar(canvas, x, y, w, h, share, colour, segments=None):
    """A trough (the well) with a phosphor fill, as the HUD draws it (HudKit)."""
    well_img, fill_img = load("well"), load("fill")
    canvas.alpha_composite(stretch(well_img, WELL_SPLIT, (w + 4, h + 4)), (x - 2, y - 2))
    dim = tuple(c // 6 for c in colour) + (255,)
    if segments is None:
        canvas.alpha_composite(stretch(fill_img, FILL_SPLIT, (w, h), dim), (x, y))
        lit = round(w * share)
        if lit >= 2:
            canvas.alpha_composite(stretch(fill_img, FILL_SPLIT, (lit, h), colour + (255,)), (x, y))
        return
    count, pitch = segments
    lit = int(np.ceil(share * count))
    for i in range(count):
        cell = stretch(fill_img, FILL_SPLIT, (pitch - 1, h - 2), colour + (255,) if i < lit else dim)
        canvas.alpha_composite(cell, (x + 1 + i * pitch, y + 1))


def review():
    sheet = raster.sheet(1300, 970, "HUD (FINAL R13): BEVELLED METAL PANELS, WELLS, BARS",
                         f"PRODUCTION ART, UI BATCH - {ROUND.upper()}")
    left, right = load("panel-left"), load("panel-right")
    raster.draw_text(sheet, 16, 38, f"SIDE PANELS 240X540 AT 1X ({artkit.colour_count([left])} AND "
                                    f"{artkit.colour_count([right])} COLOURS), CORNERS AT 4X", raster.LABEL)
    sheet.alpha_composite(left, (16, 54))
    sheet.alpha_composite(right, (266, 54))
    sheet.alpha_composite(sprite.enlarge(left.crop((0, 0, 48, 48)), 4), (516, 54))
    sheet.alpha_composite(sprite.enlarge(right.crop((192, 492, 240, 540)), 4), (516, 260))

    x0 = 730
    raster.draw_text(sheet, x0, 38, "PIECES AT 4X (NINE-PATCH SPLITS MARKED)", raster.LABEL)
    y = 54
    for name, split in [("plate", None), ("well", WELL_SPLIT), ("fill", FILL_SPLIT), ("glow", GLOW_SPLIT)]:
        img = load(name)
        cell = artkit.on_background(img, 4)
        if split:
            d = ImageDraw.Draw(cell)
            l, r, t, b = (v * 4 for v in split)
            for gx in (l, cell.width - r):
                d.line([(gx, 0), (gx, cell.height)], fill=(255, 60, 200, 255))
            for gy in (t, cell.height - b):
                d.line([(0, gy), (cell.width, gy)], fill=(255, 60, 200, 255))
        raster.draw_text(sheet, x0, y, f"{name.upper()} {img.width}X{img.height}, {artkit.colour_count([img])} COLOURS",
                         raster.LABEL_DIM)
        if x0 + cell.width > 1290:
            cell = cell.crop((0, 0, 1290 - x0, cell.height))
        sheet.alpha_composite(cell, (x0, y + 12))
        y += cell.height + 22
    portrait = load("portrait")
    raster.draw_text(sheet, x0 + 150, 54 + 22 + 88 + 10, "PORTRAIT 76X76, 2X", raster.LABEL_DIM)
    sheet.alpha_composite(artkit.on_background(portrait, 2), (x0 + 150, 54 + 22 + 88 + 22))

    # the pieces as the game draws them, on the left panel, without text
    y = 610
    raster.draw_text(sheet, 16, y - 16, "AS THE GAME DRAWS THEM (1X AND 2X, NO TEXT): READOUT WELLS WITH GREEN / "
                                         "AMBER GLOW, GAUGES, CHAIN, PROGRESS, PIPS, PORTRAIT", raster.LABEL)
    demo = left.crop((0, 0, 240, 340))
    plate_img, well_img, glow_img, fill_img = load("plate"), load("well"), load("glow"), load("fill")
    for wy, colour in ((38, (64, 255, 128)), (98, (255, 224, 74))):
        demo.alpha_composite(plate_img, (12, wy - 22))
        demo.alpha_composite(stretch(well_img, WELL_SPLIT, (212, 28)), (14, wy))
        demo.alpha_composite(stretch(glow_img, GLOW_SPLIT, (208, 24), colour + (GLOW_ALPHA,)), (16, wy + 2))
    bar(demo, 16, 150, 208, 12, 0.7, (255, 68, 0), segments=(16, 13))
    bar(demo, 16, 176, 208, 12, 0.3, (0, 255, 255), segments=(16, 13))
    bar(demo, 16, 202, 208, 6, 0.55, (255, 176, 0))
    bar(demo, 16, 222, 208, 10, 0.62, (138, 208, 255))
    demo.alpha_composite(stretch(load("portrait"), WELL_SPLIT, (76, 76)), (14, 244))
    for i in range(5):
        pip = (255, 224, 74, 255) if i < 3 else (58, 58, 32, 255)
        demo.alpha_composite(stretch(fill_img, FILL_SPLIT, (9, 7), pip), (110 + i * 12, 250))
    sheet.alpha_composite(demo, (16, y))
    sheet.alpha_composite(sprite.enlarge(demo.crop((0, 0, 240, 170)), 2), (266, y))
    path = CONCEPT / f"hud-final-{ROUND}-a.png"
    sheet.convert("RGB").save(path, optimize=True)
    print(f"review: {path.relative_to(artkit.ROOT)}")


if __name__ == "__main__":
    if "--review" not in sys.argv[1:]:
        build()
    review()
