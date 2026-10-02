#!/usr/bin/env python3
"""Production art: the pre-rendered scenes behind the out-of-game screens (design/ui) and the logo.

Outputs (assets/ui/, loaded as textures of their own, not packed):
  title-scene.png   960x540  main menu A's hero scene behind the title, main menu, difficulty
                             select, load game, options and the briefings: Earth's limb below, the
                             sun top-left, the Vrell fleet emerging top-right, the Stormhawk climbing
                             to meet it with its engine trail
  title-logo.png    460 wide logo D (TERRAN VANGUARD, blue steel, deep extrusion) on transparency
  hangar-map.png    960x540  the hangar's tactical display (hangar-r07-b) for Act 1: a cyan grid
                             over the dark Earth seen from orbit, its limb across the lower part,
                             orange range rings round the operation's area and a dashed approach
                             route with its arrowhead
  design/ui/main-menu/concept/main-menu-final-r13-a.png   review sheet: scene, logo, both composed

The look is the chosen concepts' (tools/concept/ui_r06.py: hero_backdrop, logo_layer; the hangar's
r07-b map), brought to the production bar (design/art-direction/production):
  - the scene is built in layers: the space and Earth background (the concept's nebula, terrain,
    clouds, limb and rim, sun, the engine trail and the vignette), space, Earth and the sun
    posterized to 32 colours each with 4x4 ordered dither on their wide gradients; the Stormhawk
    and the Vrell are ray-marched at the sprite quality bar (8x up to 64 px, 4x above) at their
    size in the scene, turned by their heading in the model so the key light stays top-left (the
    concept rotated finished images), 1-bit alpha, unsharp mask, each with its own palette; the
    engine glows are added on top in four steps;
  - the logo is the concept's logo D at 460 px, its glow and edges stepped to five translucency
    levels and its colours cut to 48;
  - the map is a 2D tactical display posterized to 24 colours, its wide gradients dithered, and its
    grid, rings and route to 12 colours of their own.

Run: python3 tools/art/ui_scenes.py [--review]   (~1 min; --review only rebuilds the sheet)
"""
import sys
from concurrent.futures import ProcessPoolExecutor

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

import artkit
from artkit import DESIGN, ROOT, sprite

import enemies_r04 as e4  # noqa: E402  (concept scripts, imported unchanged)
import ui_r06  # noqa: E402
from render import models, raster, terrain  # noqa: E402
from render.palette import B  # noqa: E402
from render.sdf import rotate_z  # noqa: E402

SCRIPT = "ui_scenes.py"
SOURCE = artkit.source_note(SCRIPT, "UI batch")
OUT = ROOT / "assets" / "ui"
CONCEPT = DESIGN / "ui" / "main-menu" / "concept"
ROUND = "r13"
W, H = 960, 540
SCENE_COLOURS = 32
MAP_COLOURS = 24
LINE_COLOURS = 12
LOGO_COLOURS = 48
LOGO_WIDTH = 460
EXTENT = 2.3                        # model units across a sprite of its nominal size

# the hero ship (ui_r06.hero_ship): 150 px nominal, banked, climbing 24 degrees right of up
HERO = dict(size=150, heading=24.0, bank=-0.32, at=(270, 330))
TRAIL_BACK = 44                     # the trail starts this far behind the ship's centre, px
ENGINE_OFFSET = 9                   # the two engines either side of the axis, px
SEA, LAND = B["EARTH ORBIT"], B["EARTH SURFACE"]
EARTH = dict(cx=560.0, cy=1500.0, r=1110.0)
LIMB_GLOW = 40                      # the rim's glow outside the disk, posterized with Earth
SUN = (70, 60, 100)                 # the sun's centre and the reach of its glow's own palette


# --------------------------------------------------------------------------- the title scene

def background():
    """The concept's nebula, Earth, limb, rim and sun (ui_r06.hero_backdrop) and the trail, as a
    float RGB array, before the sprites."""
    img = ui_r06.nebula_bg(77)
    tex = terrain.earth_coast(W, H, seed=11, period=False, stops=terrain.coast_stops_from(SEA, LAND, 0.56), cell=128)
    e = np.array(tex).astype(np.float64)[..., :3]
    clouds = raster.fbm(W, H, 64, 61, octaves=5, period=False)
    c = np.clip((clouds - 0.52) * 2.6, 0, 0.9)[..., None]
    e = e * (1 - c) + np.array(SEA[5]) * c
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float64)
    d = np.hypot(xx - EARTH["cx"], yy - EARTH["cy"])
    r = EARTH["r"]
    depth = np.clip((r - d) / 240, 0, 1)
    light = (0.35 + 0.65 * depth ** 0.6) * (0.55 + 0.45 * np.clip(1.1 - xx / W, 0, 1))
    e *= light[..., None] * 0.9
    arr = np.array(img).astype(np.float64)
    arr[..., :3] = np.where((d < r)[..., None], e, arr[..., :3])
    rim = np.exp(-((d - r) / 9) ** 2) * 1.2 + np.exp(-np.clip(d - r, 0, None) / 26) * (d >= r) * 0.55 + \
        np.exp(-np.clip(r - d, 0, None) / 40) * (d < r) * 0.35
    rim *= 0.5 + 0.5 * np.clip(1.15 - xx / W, 0, 1)
    arr[..., :3] += rim[..., None] * np.array([70, 150, 255])
    img = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")
    x, y, _ = SUN
    img = raster.add_light(img, x, y, 160, (90, 130, 255), 0.55)
    img = raster.add_light(img, x, y, 26, (255, 245, 220), 1.6)
    img = raster.add_light(img, x, y, 8, (255, 255, 255), 2.0)
    img.alpha_composite(trail())
    return np.array(img).astype(np.float64)[..., :3] * vignette()[..., None]


def vignette():
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float64)
    return np.clip(1.25 - 0.55 * np.hypot((xx - W / 2) / W, (yy - H / 2) / H) * 2, 0.55, 1)


def back_axis():
    a = np.radians(HERO["heading"])
    back = np.array([-np.sin(a), np.cos(a)])        # screen direction towards the tail (y down)
    return back, np.array([back[1], -back[0]])


def trail():
    """The concept's twin engine trail behind the ship."""
    sx, sy = HERO["at"]
    back, side = back_axis()
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    for k in range(34):
        p = np.array([sx, sy]) + back * (TRAIL_BACK + k * 4.0)
        a = int(200 * (1 - k / 34) ** 1.5)
        r = 4 - 2.5 * k / 34
        for off in (-ENGINE_OFFSET, ENGINE_OFFSET):
            q = p + side * off
            d.ellipse([q[0] - r, q[1] - r, q[0] + r, q[1] + r], fill=(0, 190, 255, a))
    out = layer.filter(ImageFilter.GaussianBlur(2.2))
    out.alpha_composite(layer.filter(ImageFilter.GaussianBlur(0.6)))
    return out


def hero_ship():
    """The Stormhawk at 4x on a canvas with room for its heading, turned in the model."""
    size = int(HERO["size"] * 1.25)
    scene, mats = models.ship_a_model(HERO["bank"], palette=B.ship_colors())
    turn = -np.radians(HERO["heading"])
    hi, factor = artkit.render_hi(lambda p: scene(rotate_z(p, turn)), mats, (size, size), EXTENT * 1.25)
    return artkit.native(hi, factor)


# The Vrell fleet: the concept's draw (seed 6) of kinds, scales, haze and headings.
FLEET_KINDS = [("skitter-a", 30), ("stinger-a", 36), ("mantis-a", 60)]


def fleet():
    """[(kind, size px, haze, heading deg, centre x, top y)] as ui_r06.hero_backdrop draws them."""
    rng = np.random.default_rng(6)
    out = []
    for i in range(16):
        kind, n = FLEET_KINDS[rng.integers(0, 3)] if i > 3 else FLEET_KINDS[2]
        s = rng.uniform(0.32, 0.6) if i > 3 else rng.uniform(0.6, 0.85)
        haze = 0.55 if i > 3 else 0.25
        heading = rng.uniform(-25, -5)
        x = int(rng.uniform(640, 930))
        y = int(rng.uniform(30, 200) - (x - 640) * 0.15)
        out.append((kind, max(4, int(n * s)), haze, heading, x, max(0, y)))
    return out


def vrell(job):
    """One Vrell at its size in the scene, turned in the model (counter-clockwise by ``heading``
    as the concept's image rotation), hazed towards the fleet's violet."""
    kind, size, haze, heading, _, _ = job
    canvas = int(np.ceil(size * 1.3))
    scene, mats = e4.R04[kind][4](0.0, 1.0)
    turn = np.radians(heading)
    hi, factor = artkit.render_hi(lambda p: scene(rotate_z(p, -turn)), mats, (canvas, canvas), EXTENT * 1.3)
    a = np.array(artkit.native(hi, factor)).astype(np.float64)
    a[..., :3] = a[..., :3] * (1 - haze) + np.array([30, 20, 70]) * haze
    return Image.fromarray(a.astype(np.uint8), "RGBA")


def vignetted(img, x, y):
    """A sprite darkened by the scene's vignette where it stands."""
    a = np.array(img).astype(np.float64)
    v = vignette()[y:y + img.height, x:x + img.width]
    a[:v.shape[0], :v.shape[1], :3] *= v[..., None]
    return Image.fromarray(a.astype(np.uint8), "RGBA")


def engine_glows():
    """The two engine glows, additive, stepped to four levels."""
    sx, sy = HERO["at"]
    back, side = back_axis()
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 255))
    for off in (-ENGINE_OFFSET, ENGINE_OFFSET):
        q = np.array([sx, sy]) + back * (TRAIL_BACK + 2) + side * off
        layer = raster.add_light(layer, q[0], q[1], 10, (0, 192, 255), 1.1)
    a = np.array(layer).astype(np.float64)
    a[..., :3] = np.floor(a[..., :3] / 255 * 4 + 0.5) / 4 * 255
    return a[..., :3]


def title_scene(ship, fleet_images):
    arr = background()
    yy, xx = np.mgrid[0:H, 0:W]
    earth = np.hypot(xx - EARTH["cx"], yy - EARTH["cy"]) < EARTH["r"] + LIMB_GLOW
    sun = np.hypot(xx - SUN[0], yy - SUN[1]) < SUN[2]
    img = dithered(arr, SCENE_COLOURS, ~earth & ~sun)             # space
    for layer in (earth, sun):                                    # Earth with its limb and rim; the sun
        img.alpha_composite(dithered(arr, SCENE_COLOURS, layer))
    for (_, _, _, _, x, y), sp in zip(fleet(), fleet_images):
        left, top = x - sp.width // 2, y
        sp = artkit.quantize_set([vignetted(sp, max(0, left), top)], 24)[0]
        img.alpha_composite(sp, (left, top))
    sx, sy = HERO["at"]
    left, top = sx - ship.width // 2, sy - ship.height // 2
    img.alpha_composite(artkit.quantize_set([vignetted(ship, left, top)], 48)[0], (left, top))
    out = np.array(img).astype(np.float64)
    out[..., :3] = np.minimum(255, out[..., :3] + engine_glows())
    return Image.fromarray(out.astype(np.uint8), "RGBA").convert("RGB")


def dithered(rgb, colours, mask=None):
    """Posterize an opaque float RGB layer (the pixels of ``mask``, all by default) to ``colours``
    with 4x4 ordered dither before the median cut (the backdrops' rule for wide gradients)."""
    h, w = rgb.shape[:2]
    b = np.tile(artkit.BAYER4, (h // 4 + 1, w // 4 + 1))[:h, :w]
    alpha = np.full((h, w), 255) if mask is None else mask * 255
    img = raster.to_rgba_image(rgb + ((b - 0.5) * 10.0)[..., None], alpha)
    return artkit.quantize_set([img], colours)[0]


# --------------------------------------------------------------------------- the logo

def logo():
    img = artkit.stepped_alpha(ui_r06.logo_layer(LOGO_WIDTH), 5)
    return artkit.quantize_set([img], LOGO_COLOURS)[0]


# --------------------------------------------------------------------------- the hangar map

GRID = 32
RINGS = (50, 100, 160, 230)
TARGET = (470, 300)
ROUTE_FROM = (60, 20)
LIMB = dict(cx=480.0, cy=540.0 + 900 - 150, r=900.0)
HOLO = [(0.0, (2, 4, 14)), (0.35, (4, 18, 40)), (0.7, (8, 46, 78)), (1.0, (24, 96, 130))]


def hangar_map():
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float64)
    arr = np.zeros((H, W, 3))
    arr[...] = (5, 8, 26)
    # Earth from orbit as a holographic relief: the terrain's brightness through a teal ramp
    tex = terrain.earth_coast(W, H, seed=19, period=False, stops=terrain.coast_stops_from(SEA, LAND, 0.5), cell=96)
    lum = np.array(tex.convert("L")).astype(np.float64) / 255
    d = np.hypot(xx - LIMB["cx"], yy - LIMB["cy"])
    inside = d < LIMB["r"]
    depth = np.clip((LIMB["r"] - d) / 200, 0, 1)
    planet = raster.ramp(HOLO, np.clip(lum * 1.3 * (0.45 + 0.55 * depth), 0, 1))
    arr = np.where(inside[..., None], planet, arr)
    arr += (np.exp(-((d - LIMB["r"]) / 5) ** 2) * 0.9)[..., None] * np.array([40, 150, 200])
    arr += (np.exp(-np.clip(d - LIMB["r"], 0, None) / 30) * (d >= LIMB["r"]) * 0.35)[..., None] * np.array([20, 70, 120])
    # the display's faint scan rows
    arr *= np.where(yy % 2 == 1, 0.88, 1.0)[..., None]
    img = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGB").convert("RGBA")
    over = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    g = ImageDraw.Draw(over)
    for x in range(0, W, GRID):
        g.line([(x, 0), (x, H)], fill=(40, 120, 170, 110 if x % (4 * GRID) == 0 else 55))
    for y in range(0, H, GRID):
        g.line([(0, y), (W, y)], fill=(40, 120, 170, 110 if y % (4 * GRID) == 0 else 55))
    for x in range(0, W, GRID // 4):                                   # edge ticks
        g.line([(x, 0), (x, 3 if x % GRID else 7)], fill=(80, 190, 230, 160))
    tx, ty = TARGET
    for i, radius in enumerate(RINGS):
        g.ellipse([tx - radius, ty - radius, tx + radius, ty + radius], outline=(230, 110, 24, 190 if i else 240))
    for k in range(0, 360, 10):                                        # the outer ring's bearing ticks
        a = np.radians(k)
        r0, r1 = RINGS[-1], RINGS[-1] + (8 if k % 30 == 0 else 4)
        g.line([(tx + r0 * np.cos(a), ty + r0 * np.sin(a)), (tx + r1 * np.cos(a), ty + r1 * np.sin(a))],
               fill=(220, 120, 40, 170))
    g.line([(tx - 10, ty), (tx + 10, ty)], fill=(255, 150, 60, 230))
    g.line([(tx, ty - 10), (tx, ty + 10)], fill=(255, 150, 60, 230))
    fx, fy = ROUTE_FROM
    steps = 40
    for i in range(0, steps - 2, 2):                                   # dashed approach route
        a0, a1 = i / steps, (i + 1) / steps
        g.line([(fx + (tx - fx) * a0, fy + (ty - fy) * a0), (fx + (tx - fx) * a1, fy + (ty - fy) * a1)],
               fill=(60, 220, 255, 220), width=2)
    direction = np.array([tx - fx, ty - fy], float)
    direction /= np.linalg.norm(direction)
    tip = np.array(TARGET) - direction * (RINGS[0] + 4)
    normal = np.array([-direction[1], direction[0]])
    g.polygon([tuple(tip), tuple(tip - direction * 12 + normal * 6), tuple(tip - direction * 12 - normal * 6)],
              fill=(60, 220, 255, 230))
    img.alpha_composite(over)
    a = np.array(img).astype(np.float64)[..., :3] * (vignette() * 0.85 + 0.15)[..., None]
    lines = np.array(over)[..., 3] > 0
    out = dithered(a, MAP_COLOURS, ~lines)                        # the display and the planet
    out.alpha_composite(dithered(a, LINE_COLOURS, lines))         # grid, rings and route: their own few colours
    return out.convert("RGB")


# --------------------------------------------------------------------------- build and review

def build():
    OUT.mkdir(parents=True, exist_ok=True)
    jobs = fleet()
    with ProcessPoolExecutor() as pool:
        ship_job = pool.submit(hero_ship)
        fleet_images = list(pool.map(vrell, jobs))
        ship = ship_job.result()
    outputs = {"title-scene": title_scene(ship, fleet_images), "title-logo": logo(), "hangar-map": hangar_map()}
    for name, img in outputs.items():
        artkit.save_png(img, OUT / f"{name}.png", SOURCE)
        print(f"{name}: {img.width}x{img.height}, {artkit.colour_count([img.convert('RGBA')])} colours")


def review():
    scene = Image.open(OUT / "title-scene.png").convert("RGBA")
    logo_img = Image.open(OUT / "title-logo.png").convert("RGBA")
    composed = scene.copy()
    composed.alpha_composite(logo_img, ((W - logo_img.width) // 2, 20))
    sheet = raster.sheet(16 + W + 16 + 480 + 16, 52 + H + 30 + 270 + 20, "MAIN MENU / TITLE (FINAL R13): HERO SCENE AND LOGO D",
                         f"PRODUCTION ART, UI BATCH - {ROUND.upper()}")
    raster.draw_text(sheet, 16, 38, f"TITLE SCENE 960X540 ({artkit.colour_count([scene])} COLOURS) WITH THE LOGO "
                                    "AS THE TITLE SCREEN DRAWS IT", raster.LABEL)
    sheet.alpha_composite(composed, (16, 52))
    x = 16 + W + 16
    raster.draw_text(sheet, x, 38, f"LOGO {logo_img.width}X{logo_img.height} ({artkit.colour_count([logo_img])} "
                                   "COLOURS) ON A CHECKER", raster.LABEL)
    check = artkit.checker(logo_img.width, logo_img.height, 8)
    check.alpha_composite(logo_img)
    sheet.alpha_composite(check.resize((480, int(check.height * 480 / check.width)), Image.NEAREST), (x, 52))
    raster.draw_text(sheet, x, 52 + 260, "STORMHAWK AND VRELL 2X", raster.LABEL)
    sheet.alpha_composite(sprite.enlarge(scene.crop((170, 230, 410, 430)), 2).crop((0, 0, 480, 400)), (x, 52 + 274))
    y = 52 + H + 16
    raster.draw_text(sheet, 16, y, "2X: THE VRELL FLEET, THE SUN, THE LIMB", raster.LABEL)
    sheet.alpha_composite(sprite.enlarge(scene.crop((620, 0, 960, 130)), 2), (16, y + 14))
    sheet.alpha_composite(sprite.enlarge(scene.crop((0, 400, 300, 530)), 2).crop((0, 0, 280, 260)), (16 + 680 + 16, y + 14))
    path = CONCEPT / f"main-menu-final-{ROUND}-a.png"
    sheet.convert("RGB").save(path, optimize=True)
    print(f"review: {path.relative_to(ROOT)}")


if __name__ == "__main__":
    if "--review" not in sys.argv[1:]:
        build()
    review()
