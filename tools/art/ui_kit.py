#!/usr/bin/env python3
"""Production art: the glass UI kit of the out-of-game screens (design/ui, the chosen ui-kit-r08-a;
main menu A, hangar r07-b): translucent navy glass with a thin bevelled metal trim and corner
tabs, amber for the selection, cyan holographic callouts, palette B, light from the top-left.

Outputs (assets/sprites/ui/, packed onto the shared sprite pages as ``ui/<name>``; a ``.9.png`` is
a libGDX nine-patch, see tools/art/hud.py):
  glass.9        32x32 (12 px corners)  the glass body, opaque navy with a stepped sheen along the
                                        top and a lit left edge; the game draws it at the panel's
                                        opacity (tinted alpha), the frame on top of it
  frame.9        32x32 (12 px corners)  the panel trim: a 2 px rounded metal rail (outer edge lit
                                        top and left) with the four 10x3 corner tabs; transparent
                                        inside; also the briefing portrait frames
  frame-on.9     32x32                  the same in amber: the selected difficulty card
  dialog.9       32x32 (14 px corners)  the confirm dialog's trim: the rail with cyan holographic
                                        corner brackets instead of tabs
  inset.9        24x24 (4 px corners)   a recessed dark glass area (test fire, power bar, slider
                                        track, the hangar's credits): 2 px bezel, top and left in
                                        shade, the lip's shadow on the glass
  rule.9         24x4                   a header's trim line: a raised 2 px rail ending in a cap
  selection.9    128x20                 the amber selection bar: lit top and left edges, flat amber
                                        that fades out over its last 96 px (dithered alpha steps)
  cursor-small, cursor, cursor-large    the triangular amber cursor for the label, body and
                    4x9, 5x11, 7x15     heading fonts, faceted
  chip.9, chip-on.9, chip-off.9         chips and buttons, normal / on (amber) / disabled: a 1 px
                    12x12 (3 px)        bevel (top-left lit) over a fill with a two-row sheen; the
                                        hangar's module tiles too
  tab.9, tab-on.9   12x12 (3 px)        the options tabs: a chip with clipped top corners; the
                                        active one amber with a lit underline
  knob              7x17                the slider handle, bevelled steel
  bar.9             8x8 (1/2 px)        a lit bar cell, grey, tinted at runtime: slider and power
                                        fills, pips, density cells, bullets, the typing cursor
  row.9             12x12 (2 px)        a list row's faint glass band with a lit top edge
  hint.9            12x12 (3 px)        the key-hint plate under a screen's control hints
  tag.9             12x12 (3 px)        a small tag (NEW), grey, tinted at runtime
  scroll-up, scroll-down   9x5          cyan scroll markers of a list with more rows
  callout.9, callout-on.9  16x16 (6 px) the schematic's holographic slot callouts, cyan / amber:
                                        blueprint glass, a thin border, bright corner ticks
  diamond           7x7                 the trait-match marker, a faceted amber gem
  chevron           53x17               a difficulty card's rank chevron, grey, tinted at runtime
  design/ui/concept/ui-kit-final-r13-a.png   review sheet

Solid parts (rails, tabs, brackets, bezels, knob, cursors, gem, chevron, scroll markers) are SDF
ray-marched at 8x with hud.py's lower top-left key light and mapped by their shading through a
colour ramp (palette B's UTC HULL steel, the kit's amber, the holo cyan); 1 px features (chip
bevels, rows, borders) are drawn as pixel bevels, since a ray-march gives a 1 px line one colour.
Nothing that stretches is dithered across its stretch direction; translucency is stepped. Every
PNG carries the ``Source`` chunk.

Run: python3 tools/art/ui_kit.py [--review]   (~10 s; --review only rebuilds the sheet)
"""
import importlib.util
import sys
from concurrent.futures import ProcessPoolExecutor
from pathlib import Path

import numpy as np
from PIL import Image

import artkit
from artkit import DESIGN, ROOT, sprite

from render import raster, sdf  # noqa: E402
from render.sdf import vec  # noqa: E402

# tools/concept, which artkit puts first on the path, has a hud.py of its own: load the HUD's by path.
_spec = importlib.util.spec_from_file_location("art_hud", Path(__file__).with_name("hud.py"))
hud = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(hud)

SCRIPT = "ui_kit.py"
SOURCE = artkit.source_note(SCRIPT, "UI batch")
OUT = artkit.SPRITES / "ui"
CONCEPT = DESIGN / "ui" / "concept"
ROUND = hud.ROUND
FACTOR = 8

METAL = [raster.hexrgb(c) for c in "121632 2a3068 4e5aa0 8a96d0 c8d0f4 ffffff".split()]  # palette B UTC HULL
GLASS = (6, 8, 26)                          # the concept's glass navy
SHEEN = (34, 42, 92)                        # the glass catching the light at its top edge
AMBER_RAMP = [(0.0, (40, 30, 0)), (0.2, (110, 92, 0)), (0.45, (190, 170, 0)), (0.7, (255, 255, 0)),
              (0.9, (255, 255, 150)), (1.0, (255, 255, 230))]
HOLO_RAMP = [(0.0, (0, 24, 48)), (0.25, (0, 90, 140)), (0.5, (0, 170, 230)), (0.75, (0, 230, 255)),
             (0.9, (127, 240, 255)), (1.0, (230, 255, 255))]
GREY_RAMP = [(0.0, (70, 70, 70)), (0.5, (170, 170, 170)), (0.8, (225, 225, 225)), (1.0, (255, 255, 255))]
STEEL_RAMP = [(i / 5, c) for i, c in enumerate(METAL)]
LEVELS = 12                                 # shading steps on a ramp
BRIGHT = (hud.SHADE[0], [min(1.0, v + 0.25) for v in hud.SHADE[1]])   # small solids: no facet in black

PANEL = (32, 32)
PANEL_SPLIT = (12, 12, 12, 12)
DIALOG_SPLIT = (14, 14, 14, 14)
INSET = (24, 24)
INSET_SPLIT = (4, 4, 4, 4)
SMALL = (12, 12)
SMALL_SPLIT = (3, 3, 3, 3)
ROW_SPLIT = (1, 1, 2, 2)
RULE = (24, 4)
RULE_SPLIT = (1, 5, 0, 0)
SELECTION = (128, 20)
FADE = 96                                   # the selection bar's fade, px
SELECTION_SPLIT = (3, FADE, 2, 1)
BAR = (8, 8)
BAR_SPLIT = (1, 1, 2, 2)
CALLOUT = (16, 16)
CALLOUT_SPLIT = (6, 6, 6, 6)
RAIL = 2.0                                  # the trim's width, px
TAB = (10, 3)                               # a corner tab
BRACKET = 12                                # a dialog bracket's arm, px
CURSORS = {"cursor-small": 4, "cursor": 5, "cursor-large": 7}   # the triangle's depth per font


# --------------------------------------------------------------------------- shading

def render(scene, size, z_top=4.0):
    """Ray-march a piece of ``size`` (px, origin in the centre, y up) at 8x with the HUD's light;
    returns the native image with 1-bit alpha."""
    w, h = size
    hi = sdf.render(scene, hud.STEEL_MATS, (w * FACTOR, h * FACTOR), w, z_top=z_top,
                    key_pos=hud.far_light(w), **hud.SHADING)
    return artkit.native(hi, FACTOR, crisp=0)


def shaded(img, stops, shade=hud.SHADE):
    """A render's shading mapped through a colour ramp in LEVELS steps, keeping its alpha."""
    t = np.interp(hud.luminance(img), *shade)
    t = np.round(t * (LEVELS - 1)) / (LEVELS - 1)
    return np.dstack([raster.ramp(stops, t), np.array(img)[..., 3]])


def rect_ring(p, half_w, half_h):
    """Unsigned distance in the plane to the outline of a centred rectangle."""
    q = np.abs(p[:, :2]) - vec(half_w, half_h)
    return np.abs(sdf.length(np.maximum(q, 0.0)) + np.minimum(np.max(q, axis=-1), 0.0))


def rail(p, w, h, width=RAIL):
    """A rounded metal rail of ``width`` along a w x h piece's edge (a tube of radius width/2)."""
    r = width / 2
    ring = rect_ring(p, w / 2 - r, h / 2 - r)
    return np.sqrt(ring ** 2 + p[:, 2] ** 2) - r


CORNERS = [(sx, sy) for sx in (-1, 1) for sy in (-1, 1)]


def tabs(p, w, h):
    """The four 10x3 corner tabs, raised above the rail with chamfered edges."""
    tw, th = TAB
    d = np.full(len(p), np.inf)
    for sx, sy in CORNERS:
        centre = (sx * (w / 2 - tw / 2), sy * (h / 2 - th / 2), 0.4)
        d = np.minimum(d, artkit.chamfered_box(p, centre, (tw / 2, th / 2, 1.2), 0.9))
    return d


def brackets(p, w, h):
    """Holographic L brackets at the four corners, a tube of the rail's width."""
    r = RAIL / 2
    d = np.full(len(p), np.inf)
    for sx, sy in CORNERS:
        cx, cy = sx * (w / 2 - r), sy * (h / 2 - r)
        for a, b in (((cx, cy), (cx - sx * BRACKET, cy)), ((cx, cy), (cx, cy - sy * BRACKET))):
            d = np.minimum(d, sdf.sd_capsule(p, (*a, 0.6), (*b, 0.6), r))
    return d


def ones(p):
    return np.ones(len(p), dtype=np.int32)


# --------------------------------------------------------------------------- panel pieces

def glass_body():
    """Opaque glass navy (drawn at the panel's opacity) with the sheen stepped down over the top
    eight rows and a lit first column."""
    w, h = PANEL
    rows = np.zeros(h)
    rows[2:10] = np.floor(np.linspace(1, 0, 8) * 4) / 4 * 0.55
    k = np.tile(rows[:, None], (1, w))
    k[2:-2, 2] = np.maximum(k[2:-2, 2], 0.3)
    rgb = np.array(GLASS) + k[..., None] * (np.array(SHEEN) - np.array(GLASS))
    return raster.to_rgba_image(rgb)


def frame(stops):
    w, h = PANEL

    def scene(p):
        return np.minimum(rail(p, w, h), tabs(p, w, h)), ones(p)
    return raster.to_rgba_image(*split(shaded(render(scene, PANEL), stops)))


def dialog_frame():
    w, h = PANEL

    def steel(p):
        return rail(p, w, h), ones(p)

    def holo(p):
        return brackets(p, w, h), ones(p)
    base = shaded(render(steel, PANEL), STEEL_RAMP)
    glow = shaded(render(holo, PANEL, z_top=5.0), HOLO_RAMP)
    out = np.where(glow[..., 3:4] > 0, glow, base)
    return raster.to_rgba_image(*split(out))


def inset():
    """hud.py's recessed well at 2 px, its glass the concept's translucent near-black."""
    w, h = INSET

    def scene(p):
        return hud.recess(p, w / 2, h / 2, 2.0, 3.0)
    hi = sdf.render(scene, hud.GLASS_MATS, (w * FACTOR, h * FACTOR), w, z_top=4.0,
                    key_pos=hud.far_light(w), **hud.SHADING)
    hi[..., 3] = 1.0
    img = artkit.native(hi, FACTOR, crisp=0)
    rgba = shaded(img, STEEL_RAMP)
    yy, xx = np.mgrid[0:h, 0:w]
    glass = (xx >= 2) & (xx < w - 2) & (yy >= 2) & (yy < h - 2)
    lip = glass & ((yy == 2) | (xx == 2))                         # the bezel's shadow on the glass
    rgba[glass] = (4, 5, 16, 215)
    rgba[lip] = (2, 2, 8, 235)
    return raster.to_rgba_image(*split(rgba))


def rule():
    """Two rows of rail (lit, shade) ending in a 4x4 cap."""
    w, h = RULE
    rgba = np.zeros((h, w, 4))
    rgba[1, :w - 4] = (*METAL[3], 255)
    rgba[2, :w - 4] = (*METAL[1], 255)
    rgba[:, w - 4:] = bevel(4, 4, METAL[4], METAL[3], METAL[1])
    return raster.to_rgba_image(*split(rgba))


def selection():
    """Flat amber (the concept's lit bar at its left end) fading out over the last FADE px: the
    colour falls to 15 %, the alpha as (1 - u)^0.6 in ordered-dithered steps; the top edge is lit
    across the flat part and the first half of the fade, the left edge throughout."""
    w, h = SELECTION
    amber = np.array((255, 255, 0), float)
    u = np.clip((np.arange(w) - (w - FADE)) / FADE, 0, 1)
    colour = amber * (1 - np.round(u * 7) / 7 * 0.85)[:, None] * 0.55
    # dithered along x only, so the rows the nine-patch repeats when it stretches stay identical
    threshold = (np.array([0, 2, 1, 3])[np.arange(w) % 4] + 0.5) / 4
    alpha = np.floor((1 - u) ** 0.6 * 5 + threshold).clip(0, 5) / 5
    rgba = np.dstack([np.tile(colour, (h, 1, 1)), np.tile(alpha * 255, (h, 1))])
    top = 1 - np.clip((np.arange(w) - (w - FADE)) / (FADE / 2), 0, 1)
    rgba[0, :, :3] = amber
    rgba[0, :, 3] = np.round(top * 4) / 4 * 255
    rgba[:, 0] = (*amber, 255)
    rgba[h - 1, :, :3] *= 0.8
    return raster.to_rgba_image(*split(rgba))


def cursor(depth):
    """A right-pointing triangle ``depth`` px deep and 2 * depth + 1 high, faceted like a gem."""
    w, h = depth, 2 * depth + 1

    return gem([(-w / 2, h / 2), (w / 2, 0.0), (-w / 2, -h / 2)], (w, h), AMBER_RAMP)


def gem(poly, size, stops):
    """A faceted gem: a pyramid over the polygon (its faces at 42 degrees), cut flat 1 px down."""
    def scene(p):
        pyramid = (sdf.sd_polygon2(p[:, 0], p[:, 1], poly) + p[:, 2] * 0.9) / np.sqrt(1.81)
        return np.maximum(pyramid, -p[:, 2] - 1.0), ones(p)
    return raster.to_rgba_image(*split(shaded(render(scene, size), stops, BRIGHT)))


def diamond():
    w = h = 7
    return gem([(0.0, h / 2), (w / 2, 0.0), (0.0, -h / 2), (-w / 2, 0.0)], (w, h), AMBER_RAMP)


def chevron():
    """The rank chevron (the game's former Pixmap shape) as a bevelled plate, grey for tinting."""
    w, h = 53, 17
    pts = [(0, 10), (26, 0), (52, 10), (52, 16), (26, 6), (0, 16)]
    poly = [(x + 0.5 - w / 2, h / 2 - (y + 0.5)) for x, y in pts]

    def scene(p):
        return sdf.sd_plate(p, poly, 0.0, 1.4, round_=1.2), ones(p)
    return raster.to_rgba_image(*split(shaded(render(scene, (w, h)), GREY_RAMP)))


def knob():
    w, h = 7, 17

    def scene(p):
        return artkit.chamfered_box(p, (0, 0, -0.5), (w / 2, h / 2, 1.5), 1.5), ones(p)
    return raster.to_rgba_image(*split(shaded(render(scene, (w, h)), STEEL_RAMP, BRIGHT)))


def scroll_marker(up):
    w, h = 9, 5
    sign = 1 if up else -1
    return gem([(-w / 2, -sign * h / 2), (0.0, sign * h / 2), (w / 2, -sign * h / 2)], (w, h), HOLO_RAMP)


# --------------------------------------------------------------------------- pixel bevels

def bevel(w, h, light, mid, dark, clip_top=False):
    """A w x h block: top row and left column lit, bottom row and right column in shade."""
    rgba = np.zeros((h, w, 4))
    rgba[...] = (*mid, 255)
    rgba[0, :] = (*light, 255)
    rgba[:, 0] = (*light, 255)
    rgba[h - 1, :] = (*dark, 255)
    rgba[:, w - 1] = (*dark, 255)
    rgba[h - 1, 0] = (*mid, 255)
    rgba[0, w - 1] = (*mid, 255)
    if clip_top:
        rgba[0, 0] = rgba[0, w - 1] = 0
    return rgba


def button(light, fill, dark, sheen, clip_top=False, underline=None):
    """A chip: the bevel round a fill whose two top rows catch the light."""
    w, h = SMALL
    rgba = bevel(w, h, light, fill, dark, clip_top)
    rgba[1:3, 1:w - 1, :3] = (np.array(fill) + np.array([[0.6], [0.3]]) * (np.array(sheen) - np.array(fill)))[:, None, :]
    if underline is not None:
        rgba[h - 3:h - 1, 1:w - 1, :3] = underline
    return raster.to_rgba_image(*split(rgba))


def bar():
    """hud.py's phosphor cell in the glass kit: a lit top row, darker ends and bottom, grey."""
    rows = np.array([1.0, 0.92, 0.84, 0.84, 0.84, 0.8, 0.7, 0.58])
    v = np.tile(rows[:, None], (1, BAR[0]))
    v[:, 0] *= 0.85
    v[:, -1] *= 0.85
    grey = np.round(v * 255)
    return raster.to_rgba_image(np.dstack([grey, grey, grey]))


def row():
    w, h = SMALL
    rgba = np.zeros((h, w, 4))
    rgba[...] = (24, 30, 72, 110)
    rgba[0] = (60, 72, 140, 150)
    rgba[h - 1] = (6, 8, 26, 140)
    return raster.to_rgba_image(*split(rgba))


def hint_plate():
    w, h = SMALL
    rgba = bevel(w, h, METAL[2], GLASS, METAL[1])
    rgba[1:-1, 1:-1, 3] = 150
    for y, x in ((0, 0), (0, w - 1), (h - 1, 0), (h - 1, w - 1)):
        rgba[y, x] = 0
    return raster.to_rgba_image(*split(rgba))


def tag():
    return button((235, 235, 235), (90, 90, 90), (150, 150, 150), (130, 130, 130))


def callout(edge, tick, fill):
    """Blueprint glass, a 1 px border, a faint glow line inside it and bright 2 px corner ticks."""
    w, h = CALLOUT
    rgba = np.zeros((h, w, 4))
    rgba[...] = (*fill, 235)
    rgba[1, 1:-1] = rgba[-2, 1:-1] = (*edge, 70)
    rgba[1:-1, 1] = rgba[1:-1, -2] = (*edge, 70)
    rgba[0, :] = rgba[-1, :] = (*edge, 255)
    rgba[:, 0] = rgba[:, -1] = (*edge, 255)
    for ys in (slice(0, 2), slice(h - 2, h)):
        for xs in (slice(0, 5), slice(w - 5, w)):
            rgba[ys, xs] = (*tick, 255)
    for xs in (slice(0, 2), slice(w - 2, w)):
        for ys in (slice(0, 5), slice(h - 5, h)):
            rgba[ys, xs] = (*tick, 255)
    return raster.to_rgba_image(*split(rgba))


def split(rgba):
    return rgba[..., :3], rgba[..., 3]


# --------------------------------------------------------------------------- build

AMBER_LIGHT, AMBER_DARK = (255, 255, 140), (176, 160, 0)
SELECTED_FILL, CHIP_FILL = (60, 50, 0), (12, 14, 36)
HOLO_EDGE, HOLO_TICK, BLUEPRINT = (64, 190, 242), (127, 240, 255), (8, 15, 51)
CALLOUT_ON_EDGE, CALLOUT_ON_FILL = (255, 255, 0), (64, 56, 0)

# name -> (generator, its arguments, nine-patch split or None)
PIECES = {
    "glass": (glass_body, (), PANEL_SPLIT),
    "frame": (frame, (STEEL_RAMP,), PANEL_SPLIT),
    "frame-on": (frame, (AMBER_RAMP,), PANEL_SPLIT),
    "dialog": (dialog_frame, (), DIALOG_SPLIT),
    "inset": (inset, (), INSET_SPLIT),
    "rule": (rule, (), RULE_SPLIT),
    "selection": (selection, (), SELECTION_SPLIT),
    **{name: (cursor, (depth,), None) for name, depth in CURSORS.items()},
    "chip": (button, (METAL[3], CHIP_FILL, METAL[1], (30, 36, 80)), SMALL_SPLIT),
    "chip-on": (button, (AMBER_LIGHT, SELECTED_FILL, AMBER_DARK, (110, 94, 0)), SMALL_SPLIT),
    "chip-off": (button, ((90, 100, 136), (10, 12, 28), (40, 44, 70), (20, 24, 50)), SMALL_SPLIT),
    "tab": (button, (METAL[3], CHIP_FILL, METAL[1], (30, 36, 80), True), SMALL_SPLIT),
    "tab-on": (button, (AMBER_LIGHT, SELECTED_FILL, AMBER_DARK, (110, 94, 0), True, (255, 255, 0)), SMALL_SPLIT),
    "knob": (knob, (), None),
    "bar": (bar, (), BAR_SPLIT),
    "row": (row, (), ROW_SPLIT),
    "hint": (hint_plate, (), SMALL_SPLIT),
    "tag": (tag, (), SMALL_SPLIT),
    "scroll-up": (scroll_marker, (True,), None),
    "scroll-down": (scroll_marker, (False,), None),
    "callout": (callout, (HOLO_EDGE, HOLO_TICK, BLUEPRINT), CALLOUT_SPLIT),
    "callout-on": (callout, (CALLOUT_ON_EDGE, (255, 255, 200), CALLOUT_ON_FILL), CALLOUT_SPLIT),
    "diamond": (diamond, (), None),
    "chevron": (chevron, (), None),
}


def make(name):
    fn, args, _ = PIECES[name]
    return fn(*args)


def build():
    OUT.mkdir(parents=True, exist_ok=True)
    for old in OUT.glob("*.png"):
        old.unlink()
    with ProcessPoolExecutor() as pool:
        images = dict(zip(PIECES, pool.map(make, PIECES)))
    for name, img in images.items():
        split_ = PIECES[name][2]
        if split_ is None:
            artkit.save_png(img, OUT / f"{name}.png", SOURCE)
        else:
            artkit.save_png(hud.nine_patch(img, split_), OUT / f"{name}.9.png", SOURCE)
        print(f"{name}: {img.width}x{img.height}, {artkit.colour_count([img])} colours")


# --------------------------------------------------------------------------- review

def load(name):
    path = OUT / f"{name}.png"
    if path.exists():
        return Image.open(path).convert("RGBA")
    return hud.unpatch(Image.open(OUT / f"{name}.9.png").convert("RGBA"))


def patch(canvas, name, x, y, w=None, h=None, tint=None):
    """A piece drawn at (x, y, w, h) on the canvas as the game draws it."""
    split_ = PIECES[name][2]
    img = load(name)
    piece = hud.stretch(img, split_ or (0, 0, 0, 0), (w, h) if split_ else img.size, tint)
    canvas.alpha_composite(piece, (x, y))


def label(img, x, y, text, colour, glyphs):
    """Text in the kit's bitmap font with its one-pixel shadow."""
    import ui_r08  # noqa: E402  (concept script, imported unchanged)
    ui_r08.draw_bitmap_text(img, x + 1, y + 1, text, glyphs, (0, 0, 30))
    ui_r08.draw_bitmap_text(img, x, y, text, glyphs, colour)


def demo(scene):
    """The widgets composed as the screens draw them, over the darkened title scene."""
    import ui_r08  # noqa: E402
    small, body = ui_r08.bitmap_glyphs(8, 12, 9), ui_r08.bitmap_glyphs(10, 20, 15)
    img = scene.copy()
    white, amber, cyan, dim = (235, 240, 255), (255, 255, 0), (0, 255, 255), (70, 80, 110)

    def panel(x, y, w, h, alpha=178, name="frame"):
        patch(img, "glass", x, y, w, h, (255, 255, 255, alpha))
        patch(img, name, x, y, w, h)

    def header(text, x, right, y):
        label(img, x, y, text, amber, small)
        lx = x + 8 * len(text) + 6
        patch(img, "rule", lx, y + 2, right - lx, 4)

    panel(16, 16, 300, 186)
    header("PANEL HEADER", 28, 304, 26)
    label(img, 28, 46, "GLASS, METAL TRIM, CORNER TABS", (200, 212, 240), small)
    label(img, 28, 64, "BODY TEXT ON GLASS", white, body)
    patch(img, "inset", 28, 96, 276, 40)
    label(img, 36, 110, "INSET (TEST FIRE, MAPS)", (150, 170, 205), small)
    for i, text in enumerate(("SCATTER VULCAN", "HAMMER MORTAR", "HARPOON TORPEDO")):
        y = 146 + i * 18
        patch(img, "row", 28, y - 3, 276, 16)
        if i == 1:
            patch(img, "selection", 28, y - 3, 276, 16)
            patch(img, "cursor-small", 30, y + 1)
        label(img, 40, y, text, amber if i == 1 else dim if i == 2 else white, small)
        if i < 2:
            patch(img, "diamond", 280, y + 1, 7, 7)
    patch(img, "scroll-down", 290, 190, 9, 5)

    panel(332, 16, 292, 186)
    header("MENU ITEMS", 344, 612, 26)
    for i, text in enumerate(("NORMAL", "SELECTED", "DISABLED")):
        y = 50 + i * 32
        if i == 1:
            patch(img, "selection", 334, y - 6, 288, 30)
            patch(img, "cursor", 346, y + 2)
        label(img, 362, y, text, amber if i == 1 else (80, 88, 130) if i == 2 else white, body)
    for i, (name, text) in enumerate((("chip", "CHIP"), ("chip-on", "CHIP ON"), ("chip-off", "LOCKED"))):
        x = 344 + i * 92
        patch(img, name, x, 150, 84, 20)
        colour = amber if name == "chip-on" else (80, 88, 130) if name == "chip-off" else (200, 212, 240)
        label(img, x + 42 - 4 * len(text), 154, text, colour, small)
    patch(img, "tag", 344, 178, 28, 14, (255, 0, 170, 255))
    label(img, 347, 179, "NEW", (255, 0, 170), small)

    panel(16, 216, 400, 150)
    header("CONTROLS", 28, 404, 226)
    patch(img, "inset", 120, 246, 200, 10)
    patch(img, "bar", 122, 248, 128, 6, (255, 255, 0, 255))
    patch(img, "knob", 247, 243)
    label(img, 332, 245, "65%", amber, small)
    label(img, 28, 245, "SLIDER", (150, 170, 205), small)
    label(img, 28, 270, "TOGGLE", (150, 170, 205), small)
    for i, (name, text) in enumerate((("chip", "OFF"), ("chip-on", "ON"))):
        patch(img, name, 120 + i * 46, 266, 40, 18)
        label(img, 128 + i * 46, 269, text, amber if i else (200, 212, 240), small)
    label(img, 28, 304, "TABS", (150, 170, 205), small)
    x = 120
    for text in ("VIDEO", "AUDIO", "CONTROLS"):
        w = 10 * len(text) + 20
        patch(img, "tab-on" if text == "AUDIO" else "tab", x, 296, w, 24)
        label(img, x + 10, 298, text, amber if text == "AUDIO" else (200, 212, 240), body)
        x += w + 8
    for i in range(5):
        patch(img, "bar", 120 + i * 9, 336, 7, 7, (255, 255, 0, 255) if i < 3 else (26, 30, 56, 255))
    patch(img, "chevron", 200, 332, 53, 17, (255, 255, 0, 255))

    dx, dy, dw, dh = 432, 216, 360, 132
    patch(img, "glass", dx, dy, dw, dh, (255, 255, 255, 240))
    patch(img, "dialog", dx, dy, dw, dh)
    header("CONFIRM", dx + 12, dx + dw - 12, dy + 12)
    label(img, dx + 12, dy + 32, "QUIT TO MAIN MENU?", white, body)
    label(img, dx + 12, dy + 56, "PROGRESS SINCE THE LAST SAVE IS LOST.", (150, 170, 205), small)
    bw = (dw - 36) // 2
    for i, (name, text) in enumerate((("chip", "YES, QUIT"), ("chip-on", "NO, BACK"))):
        bx = dx + 12 + i * (bw + 12)
        patch(img, name, bx, dy + dh - 42, bw, 26)
        label(img, bx + bw // 2 - 5 * len(text), dy + dh - 39, text, amber if i else white, body)

    patch(img, "callout", 640, 16, 140, 40)
    label(img, 646, 22, "FRONT", (64, 190, 242), small)
    label(img, 646, 38, "PULSE CANNON", white, small)
    patch(img, "callout-on", 640, 64, 140, 40)
    label(img, 646, 70, "L WING", amber, small)
    label(img, 646, 86, "EMPTY", dim, small)
    patch(img, "frame-on", 640, 116, 140, 80)
    patch(img, "inset", 800, 16, 144, 28)
    label(img, 808, 20, "CR 12 450", amber, body)
    hint = "ARROWS SELECT  ENTER CONFIRM  ESC BACK"
    patch(img, "hint", 480 - 4 * len(hint) - 10, 380, 8 * len(hint) + 20, 18)
    label(img, 480 - 4 * len(hint), 383, hint, dim, small)
    return img


def review():
    scene = Image.open(ROOT / "assets" / "ui" / "title-scene.png").convert("RGBA").crop((0, 0, 960, 410))
    arr = np.array(scene).astype(np.float64)
    arr[..., :3] *= 0.5
    scene = Image.fromarray(arr.astype(np.uint8), "RGBA")
    sheet = raster.sheet(1420, 1010, "UI KIT (FINAL R13): GLASS PANELS, TRIM, CHIPS, SELECTION, CALLOUTS",
                         f"PRODUCTION ART, UI BATCH - {ROUND.upper()}")
    raster.draw_text(sheet, 16, 38, "AS THE SCREENS DRAW THEM, 1X, OVER THE DARKENED TITLE SCENE", raster.LABEL)
    composed = demo(scene)
    sheet.alpha_composite(composed, (16, 52))
    raster.draw_text(sheet, 16, 474, "DETAIL 2X", raster.LABEL)
    sheet.alpha_composite(sprite.enlarge(composed.crop((0, 0, 480, 250)), 2), (16, 488))
    x, y = 1000, 38
    raster.draw_text(sheet, x, y, "PIECES 3X (2X ABOVE 40 PX, 1X ABOVE 64)", raster.LABEL)
    y += 14
    for name in PIECES:
        img = load(name)
        zoom = 3 if max(img.size) <= 40 else 2 if max(img.size) <= 64 else 1
        cell = artkit.on_background(img, zoom)
        if y + cell.height + 12 > 1000:
            x, y = x + 140, 52
        raster.draw_text(sheet, x, y, f"{name.upper()} {img.width}X{img.height}", raster.LABEL_DIM)
        sheet.alpha_composite(cell, (x, y + 10))
        y += cell.height + 16
    path = CONCEPT / f"ui-kit-final-{ROUND}-a.png"
    sheet.convert("RGB").save(path, optimize=True)
    print(f"review: {path.relative_to(ROOT)}")


if __name__ == "__main__":
    if "--review" not in sys.argv[1:]:
        build()
    review()
