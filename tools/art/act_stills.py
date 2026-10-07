#!/usr/bin/env python3
"""Production art: the still behind an act's title card (design/campaign/act-1-first-contact, Act
intro and outro: "over a still of the Gagarin shipyards at dawn, Earth's terminator behind";
design/ui/briefing, the act title card act-title-r08-a; M4 part H batch, concept round 26).

Outputs (assets/ui/, a texture of its own like the title scene, not packed):
  act-1-first-contact-still.png   960x540: the Gagarin yards from above at dawn, as Level 01 shows
                                  them, over Earth's day side with the sunrise terminator running
                                  across behind them (the night side and its city lights bottom
                                  left, the dawn band warm along the line); the south launch rail
                                  with Lancer's Stormhawk on it on the left, the north arm with
                                  Aegis Two launching beyond it, a bridge crane over the top, open
                                  dock frames and a burning platform along the bottom, the cruiser
                                  hull on the right; the middle kept open for the chrome lettering
  design/campaign/act-1-first-contact/concept/act-1-still-final-r26-a.png   review sheet: the still,
                                  and the title card as the game draws it over it (no GIF: a still)
  act-2-homefront-still.png       960x540 (M5 part B batch, concept round 30): Nova Lagos at night
                                  from above, under the landing, as Level 08 shows it: its avenues
                                  and rooftops, towers with their walls projected from the still's
                                  middle, the harbour stepping down to the Gulf of Guinea bottom left
                                  (the quays, piers, a container ship, cranes, container yards), fires
                                  on the yards and two burnt blocks with their smoke columns leaning
                                  north east, the fog banks and mist; a cloud deck over the top and the
                                  right, lit from below; eight landers (small Vrell seed pods, their
                                  heat shields glowing) coming down through it from the top right on
                                  their smoke trails, kept off the lettering
  design/campaign/act-2-homefront/concept/act-2-still-final-r30-a.png   its review sheet, as Act 1's

The look is Level 01's final backdrop (tools/art/backdrop_l01.py, approved in round 12): Earth and
the terminator are its placeholder generator's functions (tools/concept/backdrop_l01.py, frozen,
imported by path) rendered at 960x540 and posterized with 4x4 ordered dither on the wide
gradients; the structures are its production pieces from assets/backdrop/level-01/ and the far
Stormhawks its 16-heading set, placed as the level places them (the far layer hazed and smaller);
Lancer's Stormhawk is tools/art/stormhawk.py's level frame. Nothing lit is mirrored: the mirrored
placements use the level's own -mirrored pieces. The game draws the still darkened behind the
lettering (BriefingScreen.drawTitleCard).

Act 2's still is built from Level 08's production backdrop (tools/art/backdrop_l08.py, imported by
path; its images from assets/backdrop/level-08/): the gulf glow under the haze, the avenues and
rooftops tile sets side by side, the harbour tile in three steps (the open water its first columns
mirrored), the ship, cranes, burnt blocks, fires and smoke columns, the fog and mist tile sets, and
its tower roofs with their walls drawn by its own projection (TowerProjection's), centred on the
still's middle and no roof leaning out more than 84 px. The cloud deck (r03's bank shading), the
trails and the landers are drawn here. The composite under the light is posterized to one
96-colour palette with ordered dither, the additive light (fires, lamps, the landers' shields and
trails) to 32 of its own.

Run: python3 tools/art/act_stills.py [act-1] [act-2] [--review]   (~25 s for both; after
backdrop_l01.py, stormhawk.py and backdrop_l08.py)
"""
import importlib.util
import sys
from pathlib import Path

import numpy as np
from PIL import Image

import artkit
from artkit import DESIGN, ROOT, raster, sprite

from parallax_r02 import haze  # noqa: E402  (concept script, imported unchanged)
from render import sdf  # noqa: E402

_spec = importlib.util.spec_from_file_location(
    "concept_backdrop_l01", Path(__file__).resolve().parents[1] / "concept" / "backdrop_l01.py")
l01 = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(l01)

SCRIPT = "act_stills.py"
SOURCE = artkit.source_note(SCRIPT, "M4 part H batch")
ROUND = "r26"
OUT = ROOT / "assets" / "ui"
PIECES = ROOT / "assets" / "backdrop" / "level-01"
CONCEPT = DESIGN / "campaign" / "act-1-first-contact" / "concept"
W, H = 960, 540
SEA = l01.SEA
DECK_COLOURS = 32
BRIGHTNESS = 0.55                                # BriefingScreen.STILL_BRIGHTNESS, for the review
BAR = 58                                         # the title card's letterbox bars


def piece(name):
    return Image.open(PIECES / f"{name}.png").convert("RGBA")


def earth():
    """Earth far below: the level's day side and its dawn overlay, laid out for the wide still:
    the terminator a gentle diagonal behind the lettering, night bottom left."""
    day = np.array(l01.earth_tile(W, H)).astype(np.float64)
    arr = np.zeros((H, W, 4))
    arr[..., :3] = day[..., :3]
    arr[..., 3] = 1
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float64)
    line = (yy - 300) * 0.9 - (xx - 430) * 0.42          # the terminator, day above and right
    inside = np.ones((H, W), bool)
    night = sdf.smoothstep(-50, 70, line) * inside
    l01.over(arr, np.array(SEA[0], float) + 6, night * 0.84)
    rng = np.random.default_rng(2681)
    towns = raster.fbm(W, H, 32, 2683, octaves=3, period=False)
    lights = (towns > 0.6) & (rng.random((H, W)) < 0.05) & (night > 0.5)
    l01.over(arr, np.array([210, 180, 120], float), lights * 0.9)
    band = np.exp(-(line / 60.0) ** 2)                    # dawn light along the terminator
    l01.over(arr, np.array([200, 150, 100], float), band * 0.28)
    core = np.exp(-((line - 8) / 18.0) ** 2)              # its brightest strip, day side
    l01.over(arr, np.array([226, 178, 124], float), core * 0.14)
    img = l01.finish_dithered(arr, colors=DECK_COLOURS)
    a = np.array(img).astype(np.float64)
    a[..., :3] *= 0.8                                    # the deep layer recedes, as in the level
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


def far_layer():
    """The north arm and Aegis Two launching from it, at the far layer's haze."""
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    arm = piece("north-arm")
    layer.alpha_composite(arm.crop((0, 40, 180, 40 + H)), (560, 0))
    for heading, (x, y) in ((1, (636, 190)), (2, (688, 140))):   # banking away to the right
        sprite.paste_center(layer, piece(f"stormhawk-far_{heading}"), x, y)
    return layer


def ground_layer():
    """The yard's structures at the ground layer, framing the open middle."""
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    rail = piece("launch-rail")
    layer.alpha_composite(rail.crop((0, 120, 200, 120 + H)), (40, 0))
    layer.alpha_composite(piece("cruiser-hull"), (W - 236, 40))
    dock = piece("dock-frame")
    dock_m = piece("dock-frame-mirrored")
    layer.alpha_composite(dock, (250, 360))
    layer.alpha_composite(dock_m, (500, 380))
    layer.alpha_composite(piece("platform-burning_1"), (380, 430))
    layer.alpha_composite(piece("crossbeam"), (0, 476))
    layer.alpha_composite(piece("crossbeam"), (480, 476))
    crane = piece("bridge-crane")
    layer.alpha_composite(crane, (230, 64))
    return layer


def low_layer():
    """Low-air pieces over the ground: a crane jib and a lattice beam, larger than the ground."""
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    layer.alpha_composite(piece("crane-jib-mirrored"), (640, 330))
    return layer


def still():
    img = earth()
    far = far_layer()
    img.alpha_composite(haze(far, SEA[2], 0.35))
    ground = ground_layer()
    shadow = sprite.shadow_of(ground, opacity=0.45, blur=1.2)   # the art direction's drop shadow
    img.alpha_composite(shadow, (6, 8))
    img.alpha_composite(ground)
    ship = artkit.load_frames("ship")[2]
    sprite.paste_center(img, sprite.shadow_of(ship, opacity=0.45, blur=1.0), 140 + 8, 392 + 10)
    sprite.paste_center(img, ship, 140, 392)                    # Lancer on the south rail
    flame = artkit.load_frames("engine-flame")
    if flame:
        f = flame[4]
        for dx in (-5, 5):
            img = artkit.add_light(img, f, (int(140 + dx - f.width / 2), int(392 + 18)))
    low = low_layer()
    img.alpha_composite(sprite.shadow_of(low, opacity=0.4, blur=1.5), (10, 14))
    img.alpha_composite(low)
    return img.convert("RGB").convert("RGBA")


def build():
    OUT.mkdir(parents=True, exist_ok=True)
    img = still()
    artkit.save_png(img, OUT / "act-1-first-contact-still.png", SOURCE)
    print(f"act-1-first-contact-still.png: {img.width}x{img.height}, {artkit.colour_count([img])} colours")


def title_card(img, act="act-1-first-contact"):
    """The title card as BriefingScreen draws it: the still darkened, the letterbox bars, the
    chrome lettering (without the settings line and the missions, which the game sets in its font)."""
    card = Image.new("RGBA", (W, H), (0, 0, 0, 255))
    dark = Image.blend(Image.new("RGBA", (W, H), (0, 0, 0, 255)), img, BRIGHTNESS)
    card.alpha_composite(dark)
    card.paste((0, 0, 0, 255), (0, 0, W, BAR))
    card.paste((0, 0, 0, 255), (0, H - BAR, W, H))
    letters = Image.open(OUT / f"{act}-title.png").convert("RGBA")
    card.alpha_composite(letters, ((W - letters.width) // 2, 138))
    return card


def review():
    img = Image.open(OUT / "act-1-first-contact-still.png").convert("RGBA")
    sheet = raster.sheet(1000, 40 + 2 * (H + 30) + 10, "ACT 1 TITLE-CARD STILL (FINAL R26): THE GAGARIN YARDS AT DAWN",
                         f"PRODUCTION ART, M4 PART H BATCH - {ROUND.upper()}")
    raster.draw_text(sheet, 16, 38, f"THE STILL, 960X540 AT 1X ({artkit.colour_count([img])} COLOURS)", raster.LABEL)
    sheet.alpha_composite(img, (16, 52))
    raster.draw_text(sheet, 16, 52 + H + 14, f"THE TITLE CARD OVER IT: DARKENED TO {int(BRIGHTNESS * 100)} %, LETTERBOX, "
                                             "CHROME LETTERING (THE GAME ADDS THE SETTINGS LINE AND THE MISSIONS)",
                     raster.LABEL)
    sheet.alpha_composite(title_card(img), (16, 52 + H + 28))
    CONCEPT.mkdir(parents=True, exist_ok=True)
    path = CONCEPT / f"act-1-still-final-{ROUND}-a.png"
    sheet.convert("RGB").save(path, optimize=True)
    print(f"review: {path.relative_to(ROOT)}")


# =========================================================================== Act 2: Nova Lagos

_spec8 = importlib.util.spec_from_file_location("backdrop_l08", Path(__file__).resolve().parent / "backdrop_l08.py")
l08 = importlib.util.module_from_spec(_spec8)
_spec8.loader.exec_module(l08)
l08.W = W               # its projection and blits draw on the still's 960 px (only in this process)

A2_SOURCE = artkit.source_note(SCRIPT, "M5 part B batch")
A2_ROUND = "r30"
A2_CONCEPT = DESIGN / "campaign" / "act-2-homefront" / "concept"
A2_NAME = "act-2-homefront"
STILL_COLOURS = 96       # the composite under the light, one dithered palette
CLOUD_COLOURS = 16
LIGHT_COLOURS = 32
HAZE = (np.array([0x2c, 0x24, 0x34], float), 0.06)   # Level 08's haze colour at its `light` look
# the city: Level 08's ground tile sets side by side (no repeat), the tile row at the still's top
# chosen so their cross streets lie at y = 12 + 96 k (300, 396 and 492 are the harbour's steps)
CITY = (("avenues", 0, 83), ("rooftops", 480, 371))
# the harbour, stepping down to the south east: (top y, right x, harbour tile row at y 0): from
# its top the band takes the harbour tile (water, quay, quay road, container yards) with the yards'
# right edge on a city street, and the Gulf (the water extended) left of it
HARBOUR = ((300, 474, 467), (396, 650, 371), (492, 866, 275))
WATER_EDGE, YARDS_END = 196, 480      # the harbour tile's quay edge and its right end
QUAY = np.array([24, 34, 52.0])       # its quay edge
PERIOD = l08.PERIOD                   # the city's cross streets
L08_DIR = l08.OUT
CX, CY = 480.0, 280.0                 # the towers' projection centre (y down): the still's middle
FOG = 0.6                             # the fog banks' share (the deck is over them)
LEAN = 84                             # most px a roof may lean out from its foot
# the landers' heads, kept off the lettering (x 100-860, y 138-345 with the settings line)
TRAIL_DIR = np.array([-0.45, 0.89]) / np.hypot(-0.45, 0.89)   # the landers come in from the top right
LANDERS = [((215, 418), 0.0, 1.0), ((392, 458), 2.0, 0.9), ((642, 394), -2.5, 1.0),
           ((792, 448), 1.5, 0.85), ((906, 262), -1.0, 0.8), ((62, 214), 3.0, 0.7),
           ((704, 104), -1.5, 0.6), ((296, 92), 1.0, 0.55)]
# the fires: on the yards along the quays, then two burnt blocks in the city (x, y, size)
SHORE_FIRES = [(300, 352, 0.9), (414, 330, 0.7), (470, 448, 1.0), (592, 424, 0.6), (700, 518, 0.8),
               (812, 508, 0.6)]
BURNT = [("burnt-block-c", 771, 403), ("burnt-block-a", 880, 307)]     # top left on their lots
CITY_FIRES = [(818, 444, 0.8), (916, 346, 0.6)]
SHIP = (86, 352)                                    # the container ship's top left, at the first quay
CRANES = [(82, 326), (258, 426)]                    # the quay cranes' top left, booms over the water
_yy, _xx = np.mgrid[0:H, 0:W].astype(np.float64)


def fires():
    return SHORE_FIRES + CITY_FIRES


def piece8(name):
    return np.array(l08.load(name)).astype(np.float64)


def over_arr(dst, src, alpha=1.0):
    a = src[..., 3:4] / 255 * alpha
    dst[..., :3] = src[..., :3] * a + dst[..., :3] * (1 - a)


def put(frame, img, x, y, additive=None):
    """A float RGBA image with its top left at (x, y) on the still (y down), clipped."""
    l08.blit(frame, img, int(x), int(H - y - img.shape[0]), additive)


def harbour_columns(right):
    """The harbour tile's column for each x left of ``right``: the tile ends at ``right``; left of the
    tile the open water, its first 36 columns mirrored back and forth (no pier crosses them)."""
    tx = np.arange(right) - (right - YARDS_END)
    k = -tx - 1
    m = k % 72
    return np.where(tx >= 0, tx, np.where(m < 36, m, 71 - m))


def ground():
    """Nova Lagos from above as Level 08's ground draws it: the Gulf's glow (deep, under the haze),
    the avenues and rooftops tile sets, the harbour in three steps, the ship and the cranes. Returns
    the frame (float RGBA) and the water's mask."""
    gulf = piece8("gulf")
    sea = gulf[np.arange(H) % gulf.shape[0]][:, np.arange(W) % gulf.shape[1]].copy()
    sea[..., :3] = sea[..., :3] * (1 - HAZE[1]) + HAZE[0] * HAZE[1]
    frame = sea.copy()
    for name, x0, row in CITY:
        tile = piece8(name)
        frame[:, x0:x0 + 480] = tile[(row + np.arange(H)) % tile.shape[0]]
    harbour = piece8("harbour")
    water = np.zeros((H, W), bool)
    left = 0
    for top, right, row in HARBOUR:
        band = harbour[(row + np.arange(top, H)) % harbour.shape[0]][:, harbour_columns(right)]
        frame[top:, :right] = sea[top:, :right]
        over_arr(frame[top:, :right], band)
        water[top:, :right] = band[..., 3] < 250
        edge = right - YARDS_END + WATER_EDGE
        frame[top:top + 2, left:edge, :3] = QUAY                 # the quay's lip where the band steps out
        left = edge
    put(frame, piece8("container-ship"), *SHIP)
    for x, y in CRANES:
        put(frame, piece8("quay-crane"), x, y)
    for name, x, y in BURNT:
        put(frame, piece8(name), x, y)
    return frame, water


def harbour_lamps():
    """Sodium lamps along the quays' lips (the tile's own lamps are in it)."""
    out, left = [], 0
    for top, right, _ in HARBOUR:
        edge = right - YARDS_END + WATER_EDGE
        out += [(x, top + 3) for x in range(left + 24, edge - 8, 48)]
        left = edge
    return out


def lots():
    """The city's lots (x0, y0, x1, y1) on the still that the harbour and the burnt blocks leave free."""
    out = []
    for name, x0, _ in CITY:
        for b0, b1 in l08.PLANS[name]["blocks"]:
            for s in range(12 - PERIOD, H, PERIOD):              # the cross streets' centres
                y0, y1 = s + 6, s + PERIOD - 6
                if any(y1 > top and x0 + b0 < right for top, right, _ in HARBOUR):
                    continue
                if any(abs(x0 + b0 - x) < 8 and abs(y0 - y) < 8 for _, x, y in BURNT):
                    continue
                out.append((x0 + b0, y0, x0 + b1, y1))
    return out



def towers():
    """Towers on the free lots, Level 08's roof pieces with their walls projected from the still's
    middle (backdrop_l08.py's TowerProjection draw): taller towards the middle, so no roof leans more
    than LEAN px; a few neon towers to the east, under the deck."""
    rng = np.random.default_rng(3088)
    out = []
    for x0, y0, x1, y1 in lots():
        if rng.random() > 0.62:
            continue
        fits = [f for f in l08.FOOTPRINTS if f[0] <= x1 - x0 - 6 and f[1] <= y1 - y0 - 8]
        if not fits:
            continue
        foot = fits[-1] if rng.random() < 0.7 or len(fits) == 1 else fits[-2]
        cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
        dist = max(np.hypot(cx - CX, cy - CY), 1)
        style = ("neon" if rng.random() < 0.5 else "neon-b") if cx > 560 and rng.random() < 0.4 else \
            ("a" if rng.random() < 0.55 else "b")
        heights = [h for h in l08.STYLES[style][0] if (l08.tower_scale(h) - 1) * dist <= LEAN
                   and (L08_DIR / f"{l08.tower_id(foot, h, style)}.png").exists()]
        if not heights:
            continue
        h = heights[min(len(heights) - 1, int(rng.uniform(0.35, 1.0) ** 1.1 * len(heights)))]
        out.append((l08.tower_id(foot, h, style), foot, h, style, cx, cy))
    return out



def draw_towers(frame):
    """As backdrop_l08.draw_towers, on the still: the walls the camera sees, then the roof."""
    cy_up = H - CY
    for name, (fw, fh), h, style, cx, cy in sorted(towers(), key=lambda t: t[2]):
        k = l08.tower_scale(h)
        roof = piece8(name)
        rw, rh = roof.shape[1], roof.shape[0]
        fx, fy = l08.java_round(cx - fw / 2), l08.java_round(H - cy - fh / 2)      # bottom left, y up
        rx = l08.java_round(CX + (fx + fw / 2 - CX) * k - rw / 2)
        ry = l08.java_round(cy_up + (fy + fh / 2 - cy_up) * k - rh / 2)
        wall = l08.wall_for(style, h)
        tex = np.array(l08.load(wall)).astype(np.float64)
        shade = l08.STYLES[style][2] or 0.5
        fx1, fy1, rx1, ry1 = fx + fw, fy + fh, rx + rw, ry + rh
        if ry > fy:
            l08.wall_quad(frame, tex, shade, ((fx, fy), (fx1, fy)), ((rx, ry), (rx1, ry)), h)
        if ry1 < fy1:
            l08.wall_quad(frame, tex, 1.0, ((fx1, fy1), (fx, fy1)), ((rx1, ry1), (rx, ry1)), h)
        if rx < fx:
            l08.wall_quad(frame, tex, 1.0, ((fx, fy1), (fx, fy)), ((rx, ry1), (rx, ry)), h)
        if rx1 > fx1:
            l08.wall_quad(frame, tex, shade, ((fx1, fy), (fx1, fy1)), ((rx1, ry), (rx1, ry1)), h)
        l08.blit(frame, roof, rx, ry)


def air(frame):
    """Level 08's low and high air over the city: its amber-lit fog banks (two windows of the tile
    set), the fires on the yards and the burnt blocks with their smoke columns, its thin mist."""
    fog = piece8("fog")
    for x0, row in ((0, 120), (480, 560)):
        over_arr(frame[:, x0:x0 + 480], fog[(row + np.arange(H)) % fog.shape[0]], FOG)
    for i, (x, y, s) in enumerate(fires()):
        f = piece8(f"fire_{i % 4}")
        put(frame, f, x - f.shape[1] / 2, y - f.shape[0] / 2)
    for i, (x, y, s) in enumerate(fires()):
        col = piece8(f"smoke-column-{'abc'[i % 3]}")
        put(frame, col, x - col.shape[1] * 0.32, y - col.shape[0] * 0.72)
    mist = piece8("mist")
    for x0, row in ((0, 300), (480, 0)):
        part = mist[(row + np.arange(H)) % mist.shape[0]]
        l08.add(frame[:, x0:x0 + 480], part, 0.4)


def city():
    frame, water = ground()
    draw_towers(frame)
    air(frame)
    return Image.fromarray(np.clip(frame, 0, 255).astype(np.uint8), "RGBA"), water


def ui_dither(rgb, mask, colours):
    """An RGB layer posterized with 4x4 ordered dither on its mask (the backdrops' rule for wide
    gradients), as tools/art/ui_scenes.dithered."""
    h, w = rgb.shape[:2]
    b = np.tile(artkit.BAYER4, (h // 4 + 1, w // 4 + 1))[:h, :w]
    img = raster.to_rgba_image(rgb + ((b - 0.5) * 10.0)[..., None], mask * 255)
    return artkit.quantize_set([img], colours)[0]


def lander_paths():
    """Each lander's trail: head, tail (off the frame behind it), its brightness (its nearness)."""
    out = []
    for (hx, hy), turn, bright in LANDERS:
        a = np.radians(turn)
        d = np.array([TRAIL_DIR[0] * np.cos(a) - TRAIL_DIR[1] * np.sin(a),
                      TRAIL_DIR[0] * np.sin(a) + TRAIL_DIR[1] * np.cos(a)])
        head = np.array([hx, hy], float)
        out.append((head, head - d * 900, bright))
    return out


def along(head, tail):
    """Per pixel: t along the trail (0 at the tail, 1 at the head) and the distance across it."""
    seg = head - tail
    length = np.hypot(*seg)
    t = ((_xx - tail[0]) * seg[0] + (_yy - tail[1]) * seg[1]) / length ** 2
    tc = np.clip(t, 0, 1)
    d = np.hypot(_xx - (tail[0] + seg[0] * tc), _yy - (tail[1] + seg[1] * tc))
    return t, d, length


def lander_size(bright):
    """A lander's length and width in px: the nearer (brighter) the larger."""
    return 16 + 16 * bright, 9 + 8 * bright


def lander_shape(head, tail, bright, sx, sy):
    """A lander's body at the points (sx, sy): u along it (-1 tail .. 1 nose, the nose at the trail's
    head), b across it, the half width there and the body mask: a seed pod, blunt and round at the
    nose (its heat shield), tapering to the tail."""
    d = (head - tail) / np.hypot(*(head - tail))
    n = np.array([-d[1], d[0]])
    length, width = lander_size(bright)
    a = (sx - head[0]) * d[0] + (sy - head[1]) * d[1] + length * 0.5
    b = (sx - head[0]) * n[0] + (sy - head[1]) * n[1]
    u = a / (length * 0.5)
    half = width * 0.5 * np.where(u > 0, np.sqrt(np.clip(1 - u ** 2, 0, 1)), np.clip(1 + u * 0.85, 0, 1))
    return u, b, half, (np.abs(b) < half) & (u > -1) & (u < 1), d, n


def cloud_deck():
    """The cloud deck the landing comes down through: billowy banks over the top and the right,
    open over the harbour and the city's middle, lit from below by the city, the fires and the
    trails (r03's bank shading)."""
    import parallax_r03 as p3   # noqa: E402
    n = raster.fbm(W // 2, H, 54, 3201, octaves=5, period=False)
    n = np.array(Image.fromarray((n * 255).astype(np.uint8)).resize((W, H), Image.BICUBIC),
                 dtype=np.float64) / 255
    top = np.clip(1 - (_yy - 40) / 260, 0, 1)
    right = np.clip((_xx - 560) / 360, 0, 1) * np.clip(1 - (_yy - 300) / 260, 0, 1)
    shore = np.clip(1 - np.hypot(_xx - 300, _yy - 430) / 280, 0, 1)
    n = n * 0.8 + 0.42 * np.maximum(top, right) - 0.35 * shore
    glow = np.zeros((H, W))
    for x, y, s in fires():
        glow += np.exp(-((_xx - x) ** 2 + (_yy - y) ** 2) / (90 * s) ** 2) * s
    for head, tail, bright in lander_paths():
        t, d, _ = along(head, tail)
        glow += np.exp(-(d / 30) ** 2) * np.clip(t, 0, 1) ** 2 * bright * (t < 1.05) * 0.7
    pal = [(10, 12, 26), (22, 26, 50), (36, 40, 70), (54, 58, 92)]
    deck = p3.bank(n, 0.58, 0.16, pal, max_alpha=220, light=3.0, glow=(np.array([150, 70, 26]), np.clip(glow, 0, 1.4)))
    return p3.step_alpha(artkit.quantize_set([deck], CLOUD_COLOURS)[0], 5)


def trail_smoke():
    """The trails behind the landers: grey smoke bands widening and thinning behind each craft."""
    a = np.zeros((H, W))
    n = raster.fbm(W, H, 16, 3301, octaves=3, period=False)
    for head, tail, bright in lander_paths():
        t, d, length = along(head, tail)
        behind = (1 - np.clip(t, 0, 1)) * length - lander_size(bright)[0] * 0.6   # px behind the craft
        width = 2.5 + np.clip(behind, 0, None) * 0.035
        a = np.maximum(a, np.exp(-(d / width) ** 2) * (t <= 1) * (behind > 0)
                       * np.exp(-np.clip(behind, 0, None) / 480) * 0.8 * (0.6 + 0.6 * n))
    rgb = np.broadcast_to(np.array([72, 66, 76.0]), (H, W, 3)) * (0.75 + 0.4 * n[..., None])
    a = np.clip(a, 0, 1)
    img = raster.to_rgba_image(rgb, np.floor(a * 6 + 0.5) / 6 * 255)
    return artkit.quantize_set([img], 12)[0]


def landers():
    """The landers themselves: small Vrell seed pods (dark ribbed chitin, three short spines trailing
    from the tail), nose first along their trails, lit from the top left; rendered at 4x and
    box-reduced."""
    f = 4
    out = np.zeros((H, W, 4))
    key = np.array([-0.55, -0.6]) / np.hypot(0.55, 0.6)                 # the light, top left (y down)
    for head, tail, bright in lander_paths():
        length, width = lander_size(bright)
        x0, y0 = max(0, int(head[0] - 40)), max(0, int(head[1] - 40))
        x1, y1 = min(W, int(head[0] + 40)), min(H, int(head[1] + 40))
        sy, sx = np.mgrid[y0 * f:y1 * f, x0 * f:x1 * f].astype(np.float64) / f + 0.5 / f
        u, b, half, body, d, n = lander_shape(head, tail, bright, sx, sy)
        a = u * length * 0.5
        spines = np.zeros_like(body)
        for side in (-1, 0, 1):
            sa = a + length * 0.42                                          # px behind the tail's root
            off = side * (width * 0.12 + np.clip(-sa, 0, None) * 0.45)
            spines |= (sa < 0) & (sa > -length * (0.28 if side else 0.2)) & (np.abs(b - off) < 0.6)
        nb = np.clip(b / np.maximum(half, 1e-3), -1, 1)
        normal = n[None, None, :] * nb[..., None] + d[None, None, :] * np.clip(u, 0, 1)[..., None] * 0.6
        lit = np.clip(0.3 + 0.8 * -(normal @ key), 0, 1)
        rib = (np.floor((a + 100) / 2.4) % 2) * 0.14 * (u < 0.5)            # chitin plates behind the shield
        rgb = l08.CHITIN[0] * (1 - lit[..., None]) + l08.CHITIN[3] * 0.8 * lit[..., None]
        rgb = rgb * (1 - rib[..., None])
        rgb = np.where(spines[..., None] & ~body[..., None], l08.CHITIN[1] * 0.8, rgb)
        tile = np.zeros(sx.shape + (4,))
        tile[..., :3] = rgb / 255
        tile[..., 3] = body | spines
        small = l08.reduce(tile, f)
        sub = out[y0:y1, x0:x1]
        sa_ = small[..., 3:4]
        sub[..., :3] = small[..., :3] * 255 * sa_ + sub[..., :3] * (1 - sa_)
        sub[..., 3] = np.maximum(sub[..., 3], sa_[..., 0] * 255)
    img = raster.to_rgba_image(out[..., :3], (out[..., 3] >= 128) * 255)
    return artkit.quantize_set([img], 16)[0]


def lights(water):
    """The additive light: the fires (a warm glow, flame flecks, broken reflections on the water),
    the quays' lamps, and the landers' entry glow (a hot shield at the nose, a violet halo: Vrell
    craft) with a short ember trail behind them."""
    light = np.zeros((H, W, 3))
    rng = np.random.default_rng(3401)
    ripple = raster.fbm(W // 8, H, 4, 3411, octaves=2, period=False)
    ripple = np.array(Image.fromarray((ripple * 255).astype(np.uint8)).resize((W, H), Image.NEAREST),
                      dtype=np.float64) / 255
    wet = (water * np.clip(ripple * 2 - 0.6, 0, 1))[..., None]
    for x, y, s in fires():
        r2 = (_xx - x) ** 2 + (_yy - y) ** 2
        light += np.exp(-r2 / (30 * s) ** 2)[..., None] * np.array([150, 58, 12]) * s
        light += np.exp(-r2 / (9 * s) ** 2)[..., None] * np.array([130, 70, 20]) * s
        refl = np.exp(-((_xx - x) / (6 + 6 * s)) ** 2 - ((_yy - y - 30) / (34 * s)) ** 2)
        light += refl[..., None] * wet * np.array([150, 70, 20]) * s
        for _ in range(int(30 * s)):                          # the flames: hot flecks
            fx, fy = x + rng.normal(0, 6 * s), y + rng.normal(0, 4 * s)
            ix, iy = int(fx), int(fy)
            if 0 <= ix < W - 1 and 1 <= iy < H:
                hot = rng.random()
                light[iy - 1:iy + 1, ix] += (255, 120 + 120 * hot, 30 + 80 * hot)
    for x, y in harbour_lamps():
        r2 = (_xx - x) ** 2 + (_yy - y) ** 2
        light += np.exp(-r2 / 20.0)[..., None] * l08.SODIUM * 1.25
    for head, tail, bright in lander_paths():
        t, d, length = along(head, tail)
        size, width = lander_size(bright)
        behind = (1 - np.clip(t, 0, 1)) * length - size * 0.9          # px behind the tail
        on = (t <= 1.0) & (t >= 0) & (behind > 0)
        fade = np.exp(-np.clip(behind, 0, None) / 120)
        core = np.exp(-(d / (0.8 + behind * 0.004)) ** 2) * fade * on
        soft = np.exp(-(d / (3 + behind * 0.02)) ** 2) * fade * on
        light += np.array([220, 96, 36.0]) * (core * 0.45 + soft * 0.2)[..., None] * bright
        # the entry glow: the heat shield on the nose white to orange, a warm glow ahead of it and a
        # violet halo round the craft (Vrell)
        u, b, half, body, dd, _ = lander_shape(head, tail, bright, _xx + 0.5, _yy + 0.5)
        shield = body * np.clip((u - 0.55) / 0.45, 0, 1)
        light += shield[..., None] * np.array([230, 150, 90]) * bright
        r2 = (_xx - head[0] - dd[0] * 2) ** 2 + (_yy - head[1] - dd[1] * 2) ** 2
        light += np.exp(-r2 / (width * 0.45) ** 2)[..., None] * np.array([90, 40, 16]) * bright
        light += np.exp(-r2 / 300.0)[..., None] * np.array([80, 30, 120]) * bright
    return light


def add_lit(img, light):
    """``img`` with the additive light on it; the pixels it touches get a palette of their own,
    ordered-dithered so the wide glows do not band."""
    lit = light.max(axis=-1) >= 3
    b = np.tile(artkit.BAYER4, (H // 4 + 1, W // 4 + 1))[:H, :W]
    rgb = np.clip(np.array(img).astype(np.float64)[..., :3] + light + ((b - 0.5) * 10.0)[..., None], 0, 255)
    out = img.copy()
    out.alpha_composite(artkit.quantize_set([raster.to_rgba_image(rgb, lit * 255)], LIGHT_COLOURS)[0])
    return out


def still2():
    img, water = city()
    img.alpha_composite(cloud_deck())
    img.alpha_composite(trail_smoke())
    img.alpha_composite(landers())
    img = ui_dither(np.array(img).astype(np.float64)[..., :3], np.ones((H, W)), STILL_COLOURS)
    img = add_lit(img, lights(water))
    return img.convert("RGB").convert("RGBA")


def build2():
    OUT.mkdir(parents=True, exist_ok=True)
    img = still2()
    artkit.save_png(img, OUT / f"{A2_NAME}-still.png", A2_SOURCE)
    print(f"{A2_NAME}-still.png: {img.width}x{img.height}, {artkit.colour_count([img])} colours")


def review2():
    img = Image.open(OUT / f"{A2_NAME}-still.png").convert("RGBA")
    sheet = raster.sheet(1000, 40 + 2 * (H + 30) + 10,
                         "ACT 2 TITLE-CARD STILL (FINAL R30): NOVA LAGOS, LANDFALL",
                         f"PRODUCTION ART, M5 PART B BATCH - {A2_ROUND.upper()}")
    raster.draw_text(sheet, 16, 38, f"THE STILL, 960X540 AT 1X ({artkit.colour_count([img])} COLOURS)", raster.LABEL)
    sheet.alpha_composite(img, (16, 52))
    raster.draw_text(sheet, 16, 52 + H + 14, f"THE TITLE CARD OVER IT: DARKENED TO {int(BRIGHTNESS * 100)} %, LETTERBOX, "
                                             "CHROME LETTERING (THE GAME ADDS THE SETTINGS LINE AND THE MISSIONS)",
                     raster.LABEL)
    sheet.alpha_composite(title_card(img, A2_NAME), (16, 52 + H + 28))
    A2_CONCEPT.mkdir(parents=True, exist_ok=True)
    path = A2_CONCEPT / f"act-2-still-final-{A2_ROUND}-a.png"
    sheet.convert("RGB").save(path, optimize=True)
    print(f"review: {path.relative_to(ROOT)}")


if __name__ == "__main__":
    acts = [a for a in sys.argv[1:] if a in ("act-1", "act-2")] or ["act-1", "act-2"]
    if "act-1" in acts:
        if "--review" not in sys.argv[1:]:
            build()
        review()
    if "act-2" in acts:
        if "--review" not in sys.argv[1:]:
            build2()
        review2()
