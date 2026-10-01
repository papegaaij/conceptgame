#!/usr/bin/env python3
"""Concept round 06 - main menu, difficulty select, load game and hangar/shop screens.

Outputs (full 960x540 screens at 1x on a sheet, plus a 2x detail crop):
  design/ui/main-menu/concept/main-menu-r06-a.png    menu over a pre-rendered hero scene
  design/ui/main-menu/concept/main-menu-r06-b.png    metallic console layout (HUD A style)
  design/ui/main-menu/concept/difficulty-r06-a.png   difficulty select, hero-scene style
  design/ui/main-menu/concept/difficulty-r06-b.png   difficulty select, console style
  design/ui/main-menu/concept/load-game-r06-a.png    load game, hero-scene style
  design/ui/main-menu/concept/load-game-r06-b.png    load game, console style
  design/ui/hangar/concept/hangar-r06-a.png          hangar, three columns (hangar README layout)
  design/ui/hangar/concept/hangar-r06-b.png          hangar, central ship schematic

Style: HUD A (brushed metal, bevels, rivets, recessed LCD wells, bitmap font), palette B,
logo D, style-B portraits with retained colour. State: the hangar before L15 *Olympus Descent*
(act 3) with gear unlocked by then (see design/player/weapons).
Run: python3 tools/concept/ui_r06.py [menu|difficulty|load|hangar ...]
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, str(Path(__file__).resolve().parent))
import enemies_r04 as e4  # noqa: E402
import hud_r02  # noqa: E402
import logos  # noqa: E402
import parallax_r02  # noqa: E402
import portraits_r03 as r03  # noqa: E402
import portraits_r04 as r04  # noqa: E402
import ships_r02  # noqa: E402
from render import models, raster, sdf, sprite, terrain  # noqa: E402
from render.config import ROOT, SCREEN_H, SCREEN_W, SHIP_SIZE, WINGMAN_SIZE  # noqa: E402
from render.palette import B  # noqa: E402

MENU_OUT = ROOT / "design" / "ui" / "main-menu" / "concept"
HANGAR_OUT = ROOT / "design" / "ui" / "hangar" / "concept"
SW, SH = SCREEN_W, SCREEN_H
ROUND = "CONCEPT ROUND 06 - 960X540"

MET = hud_r02.Metal()
METAL = hud_r02.METAL
LCD, AMBER, CYAN, ALERT, HAZARD = hud_r02.LCD, hud_r02.AMBER, hud_r02.CYAN, hud_r02.ALERT, \
    hud_r02.HAZARD
WHITE = (235, 240, 255)
DIM = (70, 80, 110)
GREEN = (80, 255, 120)
RED = (255, 80, 60)
GLASS = (6, 8, 26)
_cache = {}


# --------------------------------------------------------------------------- primitives

def txt(img, x, y, s, color, scale=2, shadow=None):
    return raster.draw_text(img, x, y, s, color, scale=scale, shadow=shadow)


def tw(s, scale=2):
    return raster.text_width(s, scale)


def ctxt(img, cx, y, s, color, scale=2, shadow=None):
    return txt(img, int(cx - tw(s, scale) / 2), y, s, color, scale, shadow)


def brushed(w, h, seed, base=0.55):
    n = raster.fbm(w, h, 24, seed, octaves=3, period=False)
    br = raster.value_noise(w, h, 2, seed + 7, period=False)
    yy = np.linspace(0, 1, h)[:, None]
    t = base + 0.25 * (1 - yy) + 0.12 * n + 0.05 * br
    col = raster.ramp([(0.0, METAL[1]), (0.5, METAL[2]), (1.0, METAL[3])], np.clip(t, 0, 1))
    return raster.to_rgba_image(col * 0.82)


def rivet(d, x, y):
    d.ellipse([x - 3, y - 3, x + 3, y + 3], fill=METAL[1])
    d.point([(x - 1, y - 1), (x - 2, y - 1)], fill=METAL[4])


def metal_box(img, box, seed=1, rivets=True, base=0.55):
    """A bevelled brushed-metal plate (HUD A panel material) in an arbitrary box."""
    x0, y0, x1, y1 = box
    img.alpha_composite(brushed(x1 - x0, y1 - y0, seed + x0 + 3 * y0, base), (x0, y0))
    d = ImageDraw.Draw(img)
    d.line([x0, y0, x1 - 1, y0], fill=METAL[4])
    d.line([x0, y0, x0, y1 - 1], fill=METAL[4])
    d.line([x0 + 1, y0 + 1, x1 - 2, y0 + 1], fill=METAL[3])
    d.line([x0 + 1, y0 + 1, x0 + 1, y1 - 2], fill=METAL[3])
    d.line([x0, y1 - 1, x1 - 1, y1 - 1], fill=METAL[0])
    d.line([x1 - 1, y0, x1 - 1, y1 - 1], fill=METAL[0])
    d.line([x0 + 1, y1 - 2, x1 - 2, y1 - 2], fill=METAL[1])
    d.line([x1 - 2, y0 + 1, x1 - 2, y1 - 2], fill=METAL[1])
    if rivets and x1 - x0 > 40 and y1 - y0 > 30:
        for y in (y0 + 7, y1 - 8):
            for x in (x0 + 7, x1 - 8):
                rivet(d, x, y)


def glass(img, box, alpha=178, trim=True):
    """Translucent dark panel over a scene with a thin metal trim (menu variant A)."""
    x0, y0, x1, y1 = box
    ov = Image.new("RGBA", (x1 - x0, y1 - y0), GLASS + (alpha,))
    img.alpha_composite(ov, (x0, y0))
    if trim:
        d = ImageDraw.Draw(img)
        d.rectangle([x0, y0, x1 - 1, y1 - 1], outline=METAL[2])
        d.line([x0 + 1, y0 + 1, x1 - 2, y0 + 1], fill=METAL[4])
        for x, y in ((x0, y0), (x1 - 10, y0), (x0, y1 - 3), (x1 - 10, y1 - 3)):
            d.rectangle([x, y, x + 9, y + 2], fill=METAL[4])


def well(img, box, bg=hud_r02.LCD_BG):
    MET.well(img, box, bg)


def plate(img, x, y, text):
    MET.plate(img, x, y, text)


def hazard_strip(img, box):
    x0, y0, x1, y1 = box
    w, h = x1 - x0 + 1, y1 - y0 + 1
    strip = Image.new("RGBA", (w, h), METAL[0] + (255,))
    sd = ImageDraw.Draw(strip)
    for x in range(-h, w, 12):
        sd.polygon([(x, h), (x + 6, h), (x + 6 + h, 0), (x + h, 0)], fill=HAZARD)
    sd.rectangle([0, 0, w - 1, h - 1], outline=METAL[0])
    img.alpha_composite(strip, (x0, y0))


def tri(d, x, y, s, color, direction="r"):
    pts = {"r": [(x, y - s), (x + s, y), (x, y + s)], "l": [(x, y - s), (x - s, y), (x, y + s)],
           "u": [(x - s, y), (x, y - s), (x + s, y)], "d": [(x - s, y), (x, y + s), (x + s, y)]}
    d.polygon(pts[direction], fill=color)


def diamond(d, cx, cy, r, color):
    d.polygon([(cx, cy - r), (cx + r, cy), (cx, cy + r), (cx - r, cy)], fill=color)


def lit_bar(img, box, color=AMBER, alpha=255):
    """Selection highlight: a horizontal gradient bar with a bright top edge."""
    x0, y0, x1, y1 = box
    w, h = x1 - x0, y1 - y0
    t = np.linspace(1, 0.15, w)[None, :, None]
    col = np.array(color, float)[None, None, :] * t * 0.55
    a = np.full((h, w), alpha, float) * np.linspace(1, 0.0, w)[None, :] ** 0.6
    img.alpha_composite(raster.to_rgba_image(np.broadcast_to(col, (h, w, 3)), a), (x0, y0))
    d = ImageDraw.Draw(img)
    d.line([x0, y0, x0 + w * 2 // 3, y0], fill=color)
    d.line([x0, y0, x0, y1 - 1], fill=color)


def pips(img, x, y, lvl, mx, color=AMBER):
    d = ImageDraw.Draw(img)
    for i in range(mx):
        c = color if i < lvl else (40, 44, 20)
        d.rectangle([x + i * 9, y, x + i * 9 + 6, y + 5], fill=c)


def bar(img, x, y, w, value, color, segs=18, h=12):
    MET.bar(img, x, y, w, value, color, segs, h)


# --------------------------------------------------------------------------- shared assets

def logo_layer(width=560):
    """Logo D (TERRAN VANGUARD, blue steel, deep extrusion) on a transparent canvas."""
    key = ("logo", width)
    if key in _cache:
        return _cache[key]
    W, H = logos.W, logos.H
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    m = logos.pad(logos.text_mask("TERRAN", "bold", 130, tracking=10), 40)
    v = logos.pad(logos.text_mask("VANGUARD", "bold", 92, tracking=16), 40)
    for mask, cy in ((m, 135), (v, 250)):
        depth = 16
        for k in range(depth, 0, -1):
            t = k / depth
            col = (int(20 + 30 * (1 - t)), int(30 + 50 * (1 - t)), int(70 + 90 * (1 - t)))
            logos.place(img, logos.solid(mask, col), W / 2 + k * 0.9, cy + k * 1.2)
        glow = logos.solid(logos.dilate(mask, 4).filter(ImageFilter.GaussianBlur(9)),
                           (80, 170, 255), 0.7)
        logos.place(img, glow, W / 2, cy)
        logos.place(img, logos.solid(logos.dilate(mask, 2), (220, 240, 255)), W / 2, cy)
        logos.place(img, logos.surface(mask, logos.BLUE_STEEL, 4), W / 2, cy)
    img = img.crop(img.getbbox())
    img = img.resize((width, int(img.height * width / img.width)), Image.LANCZOS)
    _cache[key] = img
    return img


def player_sprite():
    if "player" not in _cache:
        _cache["player"] = ships_r02.render(models.ship_a_model, SHIP_SIZE, B.ship_colors())[1]
    return _cache["player"]


def rook_sprite():
    if "rook" not in _cache:
        _cache["rook"] = ships_r02.render(models.ship_c_model, WINGMAN_SIZE,
                                          ships_r02.ROOK_SCHEMES["a"][1])[1]
    return _cache["rook"]


def hero_ship(size=150, angle=-24.0, bank=-0.32):
    """The Stormhawk rendered large for the title scene, rotated at high resolution."""
    key = ("hero", size, angle, bank)
    if key not in _cache:
        f = 3
        scene, mats = models.ship_a_model(bank, palette=B.ship_colors())
        hi = sdf.render(scene, mats, (size * f, size * f), 2.3)
        big = sprite.to_image(hi).rotate(angle, resample=Image.BICUBIC, expand=True)
        _cache[key] = big.resize((big.width // f, big.height // f), Image.LANCZOS)
    return _cache[key]


def schematic_ship(size=200):
    """Top-down pre-rendered ship for the hangar schematic (not pixel-reduced)."""
    key = ("schem", size)
    if key not in _cache:
        f = 2
        scene, mats = models.ship_a_model(0.0, palette=B.ship_colors())
        hi = sdf.render(scene, mats, (size * f, size * f), 2.3)
        img = sprite.to_image(hi)
        _cache[key] = img.resize((size, size), Image.LANCZOS)
    return _cache[key]


def varga_portrait():
    if "varga" not in _cache:
        _cache["varga"] = r04.style_b2("varga", r03.render_bust("varga"))   # (144 px, 72 px)
    return _cache["varga"]


def enemy_sprite(key, n):
    return e4.render(e4.R04[key][4], n)[1]


def nebula_bg(seed, w=SW, h=SH, nebula=((30, 8, 70), (8, 30, 80))):
    bg = raster.starfield(w, h, seed, density=0.0035)
    n = raster.fbm(w, h, 128, seed + 5, octaves=5, period=False)
    n2 = raster.fbm(w, h, 96, seed + 9, octaves=4, period=False)
    a = np.array(bg).astype(np.float64)
    a[..., :3] += (np.clip(n - 0.45, 0, 1) * 2.2)[..., None] * np.array(nebula[0]) + \
        (np.clip(n2 - 0.5, 0, 1) * 2.2)[..., None] * np.array(nebula[1])
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


def hero_backdrop():
    """Title scene: Earth's limb below, the sun top-left, the Vrell fleet emerging from the
    dark top-right, the Stormhawk climbing to meet it."""
    if "hero_bg" in _cache:
        return _cache["hero_bg"].copy()
    img = nebula_bg(77)
    # Earth: a huge disk whose top edge curves across the lower third
    sea, land = B["EARTH ORBIT"], B["EARTH SURFACE"]
    tex = terrain.earth_coast(SW, SH, seed=11, period=False,
                              stops=terrain.coast_stops_from(sea, land, 0.56), cell=128)
    e = np.array(tex).astype(np.float64)[..., :3]
    clouds = raster.fbm(SW, SH, 64, 61, octaves=5, period=False)
    c = np.clip((clouds - 0.52) * 2.6, 0, 0.9)[..., None]
    e = e * (1 - c) + np.array(sea[5]) * c
    cx, cy, R = 560.0, 1500.0, 1110.0
    yy, xx = np.mgrid[0:SH, 0:SW].astype(np.float64)
    d = np.hypot(xx - cx, yy - cy)
    depth = np.clip((R - d) / 240, 0, 1)
    light = (0.35 + 0.65 * depth ** 0.6) * (0.55 + 0.45 * np.clip(1.1 - xx / SW, 0, 1))
    e *= light[..., None] * 0.9
    inside = (d < R)[..., None]
    arr = np.array(img).astype(np.float64)
    arr[..., :3] = np.where(inside, e, arr[..., :3])
    rim = np.exp(-((d - R) / 9) ** 2) * 1.2 + np.exp(-np.clip(d - R, 0, None) / 26) * \
        (d >= R) * 0.55 + np.exp(-np.clip(R - d, 0, None) / 40) * (d < R) * 0.35
    rim *= 0.5 + 0.5 * np.clip(1.15 - xx / SW, 0, 1)
    arr[..., :3] += rim[..., None] * np.array([70, 150, 255])
    img = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")
    # sun top-left
    img = raster.add_light(img, 70, 60, 160, (90, 130, 255), 0.55)
    img = raster.add_light(img, 70, 60, 26, (255, 245, 220), 1.6)
    img = raster.add_light(img, 70, 60, 8, (255, 255, 255), 2.0)
    # the Vrell fleet: far ones small and hazed, near ones full size
    rng = np.random.default_rng(6)
    far = [("skitter-a", 30), ("stinger-a", 36), ("mantis-a", 60)]
    for i in range(16):
        k, n = far[rng.integers(0, 3)] if i > 3 else far[2]
        sp = enemy_sprite(k, n)
        s = rng.uniform(0.32, 0.6) if i > 3 else rng.uniform(0.6, 0.85)
        sp = sp.resize((max(4, int(sp.width * s)), max(4, int(sp.height * s))), Image.LANCZOS)
        a = np.array(sp).astype(np.float64)
        haze = 0.55 if i > 3 else 0.25
        a[..., :3] = a[..., :3] * (1 - haze) + np.array([30, 20, 70]) * haze
        sp = Image.fromarray(a.astype(np.uint8), "RGBA").rotate(rng.uniform(-25, -5),
                                                               resample=Image.BICUBIC,
                                                               expand=True)
        x = int(rng.uniform(640, 930))
        y = int(rng.uniform(30, 200) - (x - 640) * 0.15)
        img.alpha_composite(sp, (x - sp.width // 2, max(0, y)))
        img = raster.add_light(img, x, max(0, y) + sp.height // 2, 5 + 4 * s, (0, 255, 154), 0.35)
    # the Stormhawk, climbing up-right, engine trail behind
    ship = hero_ship()
    sx, sy = 270, 330
    d2 = ImageDraw.Draw(img)
    ang = np.radians(24)
    back = np.array([-np.sin(ang), np.cos(ang)])          # direction towards the tail
    trail = Image.new("RGBA", (SW, SH), (0, 0, 0, 0))
    td = ImageDraw.Draw(trail)
    for k in range(34):
        p = np.array([sx, sy]) + back * (44 + k * 4.0)
        a = int(200 * (1 - k / 34) ** 1.5)
        r = 4 - 2.5 * k / 34
        for off in (-9, 9):
            q = p + np.array([back[1], -back[0]]) * off
            td.ellipse([q[0] - r, q[1] - r, q[0] + r, q[1] + r], fill=(0, 190, 255, a))
    img.alpha_composite(trail.filter(ImageFilter.GaussianBlur(2.2)))
    img.alpha_composite(trail.filter(ImageFilter.GaussianBlur(0.6)))
    img.alpha_composite(ship, (sx - ship.width // 2, sy - ship.height // 2))
    for off in (-9, 9):
        q = np.array([sx, sy]) + back * 46 + np.array([back[1], -back[0]]) * off
        img = raster.add_light(img, q[0], q[1], 10, (0, 192, 255), 1.1)
    # vignette for menu legibility
    arr = np.array(img).astype(np.float64)
    vig = np.clip(1.25 - 0.55 * np.hypot((xx - SW / 2) / SW, (yy - SH / 2) / SH) * 2, 0.55, 1)
    arr[..., :3] *= vig[..., None]
    img = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")
    _cache["hero_bg"] = img
    return img.copy()


def darken(img, k=0.45, blur=0):
    out = img.filter(ImageFilter.GaussianBlur(blur)) if blur else img.copy()
    arr = np.array(out).astype(np.float64)
    arr[..., :3] *= k
    return Image.fromarray(arr.astype(np.uint8), "RGBA")


def console_bg(seed=3):
    """Full-screen HUD A metal with a hazard strip at the bottom (menu variant B)."""
    img = Image.new("RGBA", (SW, SH), (0, 0, 0, 255))
    metal_box(img, (0, 0, SW, SH), seed, rivets=True, base=0.5)
    hazard_strip(img, (12, SH - 22, SW - 12, SH - 12))
    return img


def footer(img, console=False):
    col = METAL[4] if console else DIM
    y = SH - 40 if console else SH - 18
    txt(img, 16, y, "V0.1  CONCEPT", col, scale=1)
    s = "(C) 2185 UTC DEFENCE FORCE"
    txt(img, SW - 16 - tw(s, 1), y, s, col, scale=1)
    hint = "UP/DOWN SELECT    ENTER CONFIRM    ESC BACK"
    ctxt(img, SW / 2, y, hint, col, scale=1)


# --------------------------------------------------------------------------- data

DIFFS = [
    ("EASY", "RECRUIT", "FORGIVING FIRE, FREE REPAIRS, GENEROUS PAY.",
     [("ENEMY HP", "X0.75"), ("FIRE RATE", "X0.7"), ("CREDITS", "X1.25"), ("REPAIRS", "FREE"),
      ("RETRIES", "UNLIMITED"), ("CHECKPOINT", "YES")], 1),
    ("MEDIUM", "PILOT", "THE WAR AS IT WAS MEANT TO BE FOUGHT.",
     [("ENEMY HP", "X1.0"), ("FIRE RATE", "X1.0"), ("CREDITS", "X1.0"), ("REPAIRS", "5 CR/PT"),
      ("RETRIES", "UNLIMITED"), ("CHECKPOINT", "YES")], 2),
    ("HARD", "ACE", "DENSER FIRE, COSTLY REPAIRS, THREE RETRIES PER MISSION.",
     [("ENEMY HP", "X1.3"), ("FIRE RATE", "X1.3"), ("CREDITS", "X0.9"), ("REPAIRS", "10 CR/PT"),
      ("RETRIES", "3, THEN GAME OVER"), ("CHECKPOINT", "NO")], 3),
]
DIFF_FULL = [("ENEMY HP", "X0.75", "X1.0", "X1.3"), ("BULLET SPEED", "X0.8", "X1.0", "X1.15"),
             ("FIRE RATE", "X0.7", "X1.0", "X1.3"), ("FORMATIONS", "-20%", "BASE", "+20%"),
             ("BULLET BUDGET", "60", "120", "200"), ("SHIELD REGEN", "X1.25", "X1.0", "X1.0"),
             ("CREDIT INCOME", "X1.25", "X1.0", "X0.9"), ("SCORE", "X0.75", "X1.0", "X1.5"),
             ("REPAIRS", "FREE", "5 CR/PT", "10 CR/PT"), ("RETRIES", "UNLTD", "UNLTD", "3"),
             ("BOSS CHECKPOINT", "YES", "YES", "NO"), ("INTEL", "SENSOR +1", "NORMAL", "NORMAL")]

ACT_COL = {1: B["EARTH ORBIT"][3], 2: B["EARTH SURFACE"][3], 3: B["MARS"][3]}
SAVES = [
    ("AUTO", 3, "RED DUST", "M15 OLYMPUS DESCENT", "MEDIUM", "12 450", "6:42:10", "2026-10-01 21:14"),
    ("1", 3, "RED DUST", "M15 OLYMPUS DESCENT", "MEDIUM", "12 450", "6:41:55", "2026-10-01 21:13"),
    ("2", 2, "HOMEFRONT", "M12 STORM FRONT", "MEDIUM", "8 120", "4:55:02", "2026-09-28 22:40"),
    ("3", 2, "HOMEFRONT", "M09 ARCOLOGY FALL", "MEDIUM", "5 310", "3:31:47", "2026-09-26 20:05"),
    ("4", 1, "FIRST CONTACT", "M04 TRANQUILITY RUN", "HARD", "2 340", "1:10:20", "2026-09-20 19:31"),
    ("5", 1, "FIRST CONTACT", "M01 BREAK AT DAWN", "EASY", "0", "0:05:12", "2026-09-19 18:02"),
    ("6", None), ("7", None), ("8", None),
]
SEL_SAVE = 1

MENU = ["CONTINUE", "NEW GAME", "LOAD GAME", "OPTIONS", "CREDITS", "QUIT"]


def act_icon(img, x, y, act, size=22):
    d = ImageDraw.Draw(img)
    col = ACT_COL.get(act, METAL[2])
    d.rectangle([x, y, x + size, y + size], fill=tuple(v // 3 for v in col), outline=col)
    ctxt(img, x + size / 2 + 1, y + size / 2 - 6, str(act), col, scale=2)


def mars_thumb(w=216, h=122):
    if ("thumb", w) not in _cache:
        scene = parallax_r02.get_scene("c").compose(30)
        crop = scene.crop((0, 150, 480, 150 + int(480 * h / w)))
        _cache[("thumb", w)] = crop.resize((w, h), Image.LANCZOS)
    return _cache[("thumb", w)]


# --------------------------------------------------------------------------- main menu

def main_menu_a():
    img = hero_backdrop()
    logo = logo_layer(460)
    img.alpha_composite(logo, (SW // 2 - logo.width // 2, 20))
    box = (586, 232, 920, 500)
    glass(img, box)
    d = ImageDraw.Draw(img)
    y = 248
    for i, item in enumerate(MENU):
        x = 616
        if item == "NEW GAME":
            lit_bar(img, (box[0] + 2, y - 7, box[2] - 2, y + 64))
            tri(d, 600, y + 10, 7, AMBER)
            txt(img, x, y, item, AMBER, scale=3, shadow=(40, 30, 0))
            cy = y + 32
            cxx = x - 96
            for j, (name, *_rest) in enumerate(DIFFS):
                cw = tw(name, 2) + 16
                cxx = x if j == 0 else cxx + 10
                on = name == "MEDIUM"
                d.rectangle([cxx, cy, cxx + cw, cy + 18],
                            fill=(60, 50, 0) if on else (12, 14, 36),
                            outline=AMBER if on else METAL[2])
                txt(img, cxx + 8, cy + 3, name, AMBER if on else METAL[3], scale=2)
                cxx += cw
            txt(img, x, cy + 26, DIFFS[1][2], WHITE, scale=1)
            y += 80
            continue
        col = WHITE if item != "QUIT" else (200, 205, 230)
        txt(img, x, y, item, col, scale=3, shadow=(10, 10, 30))
        if item == "CONTINUE":
            txt(img, x, y + 24, "ACT 3 - M15 OLYMPUS DESCENT - MEDIUM", CYAN, scale=1)
            y += 42
        else:
            y += 33
    footer(img)
    return img


def main_menu_b():
    img = console_bg(5)
    d = ImageDraw.Draw(img)
    # logo viewscreen
    vs = (150, 16, 810, 168)
    well(img, vs, (0, 0, 0))
    view = hero_backdrop().crop((160, 0, 160 + (vs[2] - vs[0]) + 1, vs[3] - vs[1] + 1))
    view = darken(view, 0.55)
    img.alpha_composite(view, (vs[0], vs[1]))
    logo = logo_layer(470)
    img.alpha_composite(logo, ((vs[0] + vs[2]) // 2 - logo.width // 2,
                               (vs[1] + vs[3]) // 2 - logo.height // 2))
    arr = np.array(img).astype(np.float64)
    arr[vs[1]:vs[3]:2, vs[0]:vs[2], :3] *= 0.86                      # screen scanlines
    img = Image.fromarray(arr.astype(np.uint8), "RGBA")
    d = ImageDraw.Draw(img)
    # left: ship status viewport
    plate(img, 22, 186, "AF-12 STORMHAWK")
    well(img, (22, 204, 222, 344), (4, 8, 22))
    grid = Image.new("RGBA", (200, 140), (0, 0, 0, 0))
    gd = ImageDraw.Draw(grid)
    for gx in range(0, 200, 20):
        gd.line([gx, 0, gx, 140], fill=(0, 90, 160, 70))
    for gy in range(0, 140, 20):
        gd.line([0, gy, 200, gy], fill=(0, 90, 160, 70))
    img.alpha_composite(grid, (22, 204))
    ship = sprite.enlarge(player_sprite(), 2)
    img.alpha_composite(ship, (122 - ship.width // 2, 274 - ship.height // 2))
    rook = rook_sprite()
    img.alpha_composite(rook, (62 - rook.width // 2, 300 - rook.height // 2))
    txt(img, 30, 210, "AEGIS WING", CYAN, scale=1)
    txt(img, 30, 332, "LANCER + ROOK", LCD, scale=1)
    plate(img, 22, 356, "SYSTEMS")
    well(img, (22, 374, 222, 470))
    for i, (k, v) in enumerate((("GEN", "MK III  14 MW"), ("SHD", "MK III  45"),
                                ("ARM", "COMP II  74/100"), ("SPC", "AIRSTRIKE X2"))):
        txt(img, 30, 382 + i * 22, k, AMBER, scale=2)
        txt(img, 78, 386 + i * 22, v, LCD, scale=1)
    # centre: menu buttons
    bx0, bx1 = 300, 660
    y = 190
    for item in MENU:
        h = 34 if item != "NEW GAME" else 86
        box = (bx0, y, bx1, y + h)
        on = item == "NEW GAME"
        well(img, box, (40, 30, 0) if on else hud_r02.LCD_BG)
        if on:
            d.rectangle([box[0] + 1, box[1] + 1, box[0] + 8, box[3] - 1], fill=AMBER)
            tri(d, box[0] + 22, y + 15, 6, AMBER)
            txt(img, box[0] + 36, y + 7, item, AMBER, scale=3)
            for j, (name, *_rest) in enumerate(DIFFS):
                cxx = box[0] + 36 + j * 104
                cw = 94
                o = name == "MEDIUM"
                well(img, (cxx, y + 40, cxx + cw, y + 60), (70, 52, 0) if o else (8, 10, 30))
                ctxt(img, cxx + cw / 2, y + 44, name, AMBER if o else METAL[3], scale=2)
            txt(img, box[0] + 36, y + 70, DIFFS[1][2], WHITE, scale=1)
        else:
            col = LCD if item != "QUIT" else (0, 170, 80)
            txt(img, box[0] + 36, y + 9, item, col, scale=2)
            if item == "CONTINUE":
                txt(img, box[0] + 170, y + 13, "ACT 3 - M15", (0, 150, 90), scale=1)
        y += h + 10
    # right: last save + network ticker
    plate(img, 738, 186, "LAST SAVE")
    well(img, (738, 204, 938, 344), (4, 8, 22))
    th = mars_thumb(196, 74)
    img.alpha_composite(th, (740, 206))
    for i, (k, v) in enumerate((("ACT 3", "RED DUST"), ("NEXT", "M15 OLYMPUS DESC."),
                                ("CR", "12 450   MEDIUM"))):
        txt(img, 746, 286 + i * 18, k, AMBER, scale=1)
        txt(img, 790, 286 + i * 18, v, LCD, scale=1)
    plate(img, 738, 356, "CDF NETWORK")
    well(img, (738, 374, 938, 470))
    msg = "VRELL BROOD SIGHTED OVER THARSIS. ALL AEGIS PILOTS REPORT TO HANGAR 7."
    for i, line in enumerate(hud_r02.wrap(msg, 31)):
        txt(img, 746, 382 + i * 14, line, LCD, scale=1)
    txt(img, 746, 452, "> _", LCD, scale=1)
    footer(img, console=True)
    return img


# --------------------------------------------------------------------------- difficulty

def difficulty_a():
    img = darken(hero_backdrop(), 0.5, blur=2)
    logo = logo_layer(250)
    img.alpha_composite(logo, (SW // 2 - logo.width // 2, 12))
    ctxt(img, SW / 2, 126, "SELECT DIFFICULTY", WHITE, scale=3, shadow=(0, 0, 30))
    d = ImageDraw.Draw(img)
    cw, ch, gap = 270, 312, 24
    x0 = (SW - 3 * cw - 2 * gap) // 2
    for i, (name, rank, desc, mods, chev) in enumerate(DIFFS):
        x = x0 + i * (cw + gap)
        y = 172 if name != "MEDIUM" else 162
        on = name == "MEDIUM"
        box = (x, y, x + cw, y + ch + (20 if on else 0))
        glass(img, box, alpha=200 if on else 160)
        if on:
            d.rectangle([box[0] - 2, box[1] - 2, box[2] + 1, box[3] + 1], outline=AMBER)
            img = raster.add_light(img, (box[0] + box[2]) / 2, box[1], 90, (120, 90, 0), 0.25)
            d = ImageDraw.Draw(img)
        # rank chevrons
        for k in range(chev):
            cy = box[1] + 30 + k * 12
            cxm = (box[0] + box[2]) // 2
            d.polygon([(cxm - 26, cy), (cxm, cy - 10), (cxm + 26, cy), (cxm + 26, cy + 6),
                       (cxm, cy - 4), (cxm - 26, cy + 6)], fill=AMBER if on else METAL[3])
        ty = box[1] + 30 + chev * 12 + 14
        ctxt(img, (box[0] + box[2]) / 2, ty, name, AMBER if on else WHITE, scale=3)
        ctxt(img, (box[0] + box[2]) / 2, ty + 28, rank, CYAN, scale=2)
        for j, line in enumerate(hud_r02.wrap(desc, 36)):
            ctxt(img, (box[0] + box[2]) / 2, ty + 54 + j * 12, line, WHITE, scale=1)
        my = ty + 90
        for k, (lbl, val) in enumerate(mods):
            yy = my + k * 21
            d.line([box[0] + 16, yy + 16, box[2] - 16, yy + 16], fill=(40, 48, 90))
            txt(img, box[0] + 18, yy + 3, lbl, METAL[4], scale=1)
            sc = 2 if tw(val, 2) <= cw - 120 else 1
            txt(img, box[2] - 18 - tw(val, sc), yy + (0 if sc == 2 else 4), val,
                LCD if on else (150, 170, 200), scale=sc)
    ctxt(img, SW / 2, 512, "LEFT/RIGHT CHOOSE    ENTER START CAMPAIGN    ESC BACK", DIM, scale=1)
    return img


def difficulty_b():
    img = console_bg(9)
    d = ImageDraw.Draw(img)
    plate(img, 22, 18, "NEW GAME")
    txt(img, 22, 40, "SELECT DIFFICULTY", WHITE, scale=3, shadow=METAL[0])
    logo = logo_layer(260)
    img.alpha_composite(logo, (SW - 22 - logo.width, 14))
    cw, gap = 290, 20
    for i, (name, rank, desc, mods, chev) in enumerate(DIFFS):
        x = 22 + i * (cw + gap)
        on = name == "MEDIUM"
        box = (x, 92, x + cw, 172)
        well(img, box, (60, 44, 0) if on else hud_r02.LCD_BG)
        if on:
            d.rectangle([box[0] + 1, box[1] + 1, box[2] - 1, box[1] + 5], fill=AMBER)
        for k in range(chev):
            d.rectangle([x + 14 + k * 14, 112, x + 24 + k * 14, 140], fill=AMBER if on else
                        METAL[2])
        txt(img, x + 66, 104, name, AMBER if on else LCD, scale=3)
        txt(img, x + 66, 132, rank, CYAN if on else (0, 150, 150), scale=2)
        for j, line in enumerate(hud_r02.wrap(desc, 44)[:2]):
            txt(img, x + 14, 150 + j * 10, line, WHITE if on else METAL[4], scale=1)
    # detail table
    plate(img, 22, 190, "MISSION PARAMETERS")
    tb = (22, 208, SW - 22, 470)
    well(img, tb)
    cols = [40, 330, 520, 710]
    for k, h in enumerate(("", "EASY", "MEDIUM", "HARD")):
        txt(img, cols[k], 216, h, AMBER if h == "MEDIUM" else METAL[4], scale=2)
    d.rectangle([cols[2] - 10, 212, cols[2] + 150, 466], outline=(120, 100, 0))
    for r, (lbl, *vals) in enumerate(DIFF_FULL):
        y = 240 + r * 19
        if r % 2 == 0:
            d.rectangle([26, y - 3, SW - 26, y + 13], fill=(10, 14, 42))
        txt(img, cols[0], y, lbl, (150, 170, 200), scale=1)
        for k, v in enumerate(vals):
            txt(img, cols[k + 1], y - 2, v, AMBER if k == 1 else LCD, scale=2)
    # buttons
    for j, (lbl, on) in enumerate((("< BACK", False), ("START CAMPAIGN >", True))):
        bx = 22 if j == 0 else SW - 22 - 260
        box = (bx, 482, bx + (160 if j == 0 else 260), 510)
        well(img, box, (60, 44, 0) if on else hud_r02.LCD_BG)
        ctxt(img, (box[0] + box[2]) / 2, 489, lbl, AMBER if on else LCD, scale=2)
    return img


# --------------------------------------------------------------------------- load game

def save_preview(img, box, console):
    x0, y0, x1, y1 = box
    th = mars_thumb(x1 - x0 - 16, 122)
    if console:
        well(img, (x0 + 8, y0 + 8, x1 - 8, y0 + 130), (0, 0, 0))
    img.alpha_composite(th, (x0 + 8, y0 + 8))
    d = ImageDraw.Draw(img)
    d.rectangle([x0 + 8, y0 + 8, x1 - 9, y0 + 129], outline=METAL[3])
    s = SAVES[SEL_SAVE]
    txt(img, x0 + 14, y0 + 140, f"ACT {s[1]} - {s[2]}", AMBER, scale=2)
    txt(img, x0 + 14, y0 + 160, "NEXT: " + s[3], WHITE, scale=1)
    rows = [("DIFFICULTY", s[4]), ("CREDITS", "CR " + s[5]), ("SCORE", "4 812 300"),
            ("PLAYTIME", s[6]), ("SAVED", s[7])]
    for i, (k, v) in enumerate(rows):
        txt(img, x0 + 14, y0 + 180 + i * 16, k, METAL[4] if not console else (150, 170, 200),
            scale=1)
        txt(img, x0 + 100, y0 + 180 + i * 16, v, LCD, scale=1)
    txt(img, x0 + 14, y0 + 268, "LOADOUT", AMBER, scale=1)
    for i, line in enumerate(("SCATTER VULCAN L3 / TAIL GUN L1", "MICRO-MISSILE L2 X2",
                              "ESCORT: ROOK   SPC: AIRSTRIKE X2")):
        txt(img, x0 + 14, y0 + 284 + i * 14, line, (150, 200, 255), scale=1)


def save_rows(img, x0, x1, y0, console):
    d = ImageDraw.Draw(img)
    rh = 38
    for i, s in enumerate(SAVES):
        y = y0 + i * (rh + 4)
        box = (x0, y, x1, y + rh)
        on = i == SEL_SAVE
        if console:
            well(img, box, (50, 38, 0) if on else hud_r02.LCD_BG)
        else:
            glass(img, box, alpha=200 if on else 150, trim=False)
            if on:
                lit_bar(img, box)
        if on:
            tri(d, x0 - 10, y + rh // 2, 6, AMBER)
        label = "AUTOSAVE" if s[0] == "AUTO" else f"SLOT {s[0]}"
        txt(img, x0 + 10, y + 6, label, AMBER if on else METAL[4], scale=1)
        if s[1] is None:
            txt(img, x0 + 50, y + 18, "- EMPTY -", DIM, scale=2)
            continue
        act_icon(img, x0 + 10, y + 15, s[1], size=20)
        txt(img, x0 + 40, y + 18, s[3], WHITE if on else (190, 200, 230), scale=2)
        meta = f"{s[4]}   CR {s[5]}   {s[6]}"
        txt(img, x1 - 10 - tw(meta, 1), y + 6, meta, LCD if on else (0, 160, 100), scale=1)
        txt(img, x1 - 10 - tw(s[7], 1), y + 24, s[7], DIM if not on else CYAN, scale=1)


def load_game_a():
    img = darken(hero_backdrop(), 0.42, blur=2)
    logo = logo_layer(200)
    img.alpha_composite(logo, (24, 12))
    txt(img, 250, 36, "LOAD GAME", WHITE, scale=3, shadow=(0, 0, 30))
    save_rows(img, 36, 600, 112, console=False)
    box = (628, 112, 930, 506)
    glass(img, box, alpha=190)
    save_preview(img, box, console=False)
    ctxt(img, SW / 2, SH - 18, "ENTER LOAD    DEL DELETE    ESC BACK", DIM, scale=1)
    return img


def load_game_b():
    img = console_bg(13)
    plate(img, 22, 18, "SAVE ARCHIVE")
    txt(img, 22, 40, "LOAD GAME", WHITE, scale=3, shadow=METAL[0])
    logo = logo_layer(200)
    img.alpha_composite(logo, (SW - 22 - logo.width, 12))
    save_rows(img, 40, 600, 112, console=True)
    plate(img, 628, 94, "SLOT 1 DETAIL")
    box = (628, 112, 938, 470)
    well(img, box, (4, 8, 22))
    save_preview(img, box, console=True)
    for j, (lbl, on) in enumerate((("LOAD", True), ("DELETE", False), ("BACK", False))):
        bx = 628 + j * 106
        b = (bx, 482, bx + 98, 510)
        well(img, b, (60, 44, 0) if on else hud_r02.LCD_BG)
        ctxt(img, (b[0] + b[2]) / 2, 489, lbl, AMBER if on else LCD, scale=2)
    return img


# --------------------------------------------------------------------------- hangar

SHOP = [  # name, status, price, diamonds (trait matches), tag
    ("SCATTER VULCAN", "L3 FITTED", "", 0, ""),
    ("PULSE CANNON", "L4 OWNED", "", 1, ""),
    ("LANCE LASER", "", "2 500", 1, ""),
    ("HORNET LAUNCHER", "", "2 000", 0, ""),
    ("HAMMER MORTAR", "", "1 500", 1, ""),
    ("ION BEAM", "", "5 000", 0, "NEW"),
    ("HARPOON TORPEDO", "LOCKED L22", "", 0, ""),
]
SEL_SHOP = 4
INTEL = [("SETTING", "MARS - DESCENT"), ("LAYERS", "GROUND LOW-AIR AIR"),
         ("HAZARDS", "BURROWERS, CLOUDS"), ("SPECIALS", "ALL AVAILABLE"), ("BOSS", "NONE")]
QUOTE = "BURROWERS UNDER THE SLOPES. THEY ONLY SURFACE TO FIRE - BRING SOMETHING THAT HITS " \
        "THE GROUND."


def topbar(img, active="SHOP"):
    metal_box(img, (0, 0, SW, 40), 21, rivets=False)
    d = ImageDraw.Draw(img)
    x = 10
    for t in ("INTEL", "SHOP", "LOADOUT", "REPAIR", "SAVE"):
        w = tw(t, 2) + 20
        on = t == active
        well(img, (x, 9, x + w, 31), (60, 44, 0) if on else hud_r02.LCD_BG)
        ctxt(img, x + w / 2, 13, t, AMBER if on else LCD, scale=2)
        x += w + 10
    txt(img, 470, 14, "HANGAR 7 - BEFORE M15", METAL[4], scale=1, shadow=METAL[0])
    well(img, (640, 9, 790, 31))
    txt(img, 648, 13, "CR 12 450", AMBER, scale=2)
    lb = (806, 6, 950, 34)
    d.rectangle(lb, fill=(90, 20, 0))
    hazard_strip(img, (lb[0], lb[1], lb[0] + 20, lb[3]))
    d.rectangle(lb, outline=ALERT)
    tri(d, lb[0] + 34, 20, 6, AMBER)
    txt(img, lb[0] + 46, 13, "LAUNCH", AMBER, scale=2)


def slot_box(img, cx, cy, label, value, lvl=0, mx=0, on=False, w=128, h=34):
    x0, y0 = int(cx - w / 2), int(cy - h / 2)
    well(img, (x0, y0, x0 + w, y0 + h), (60, 44, 0) if on else hud_r02.LCD_BG)
    txt(img, x0 + 5, y0 + 4, label, AMBER if on else (0, 170, 170), scale=1)
    txt(img, x0 + 5, y0 + 16, value, AMBER if on else LCD, scale=1)
    if mx:
        pips(img, x0 + w - 6 - mx * 9, y0 + 5, lvl, mx)
    return (x0, y0, x0 + w, y0 + h)


def shop_list(img, x0, x1, y0, rows=SHOP, rh=26):
    d = ImageDraw.Draw(img)
    for i, (name, status, price, dia, tag) in enumerate(rows):
        y = y0 + i * (rh + 4)
        on = i == SEL_SHOP
        locked = status.startswith("LOCKED")
        well(img, (x0, y, x1, y + rh), (60, 44, 0) if on else hud_r02.LCD_BG)
        if on:
            tri(d, x0 + 8, y + rh // 2, 4, AMBER)
        col = AMBER if on else (DIM if locked else LCD)
        txt(img, x0 + 16, y + 8, name, col, scale=1)
        right = status or ("CR " + price)
        rc = DIM if locked else (CYAN if status else (AMBER if on else WHITE))
        txt(img, x1 - 8 - tw(right, 1), y + 8, right, rc, scale=1)
        for k in range(dia):
            diamond(d, x0 + 124 + k * 10, y + rh // 2, 4, (255, 200, 60))
        if tag:
            tx = x0 + 136
            d.rectangle([tx, y + 6, tx + 26, y + rh - 7], fill=(160, 0, 110))
            txt(img, tx + 3, y + 9, tag, WHITE, scale=1)


def item_detail(img, x0, x1, y0, test_fire=True):
    d = ImageDraw.Draw(img)
    txt(img, x0, y0, "HAMMER MORTAR  L1", AMBER, scale=2)
    cx = x0
    for t in ("ANTI-GROUND", "AREA"):
        w = tw(t, 1) + 12
        d.rectangle([cx, y0 + 20, cx + w, y0 + 32], fill=(40, 30, 0), outline=(255, 200, 60))
        diamond(d, cx + 5, y0 + 26, 3, (255, 200, 60))
        txt(img, cx + 10, y0 + 23, t, (255, 220, 120), scale=1)
        cx += w + 6
    txt(img, x0, y0 + 42, "DPS 25 (GROUND ONLY)   DRAW 3 MW", WHITE, scale=1)
    txt(img, x0, y0 + 56, "VS FITTED:", METAL[4], scale=1)
    txt(img, x0 + 66, y0 + 56, "DPS -18 AIR", RED, scale=1)
    txt(img, x0 + 144, y0 + 56, "+25 GROUND", GREEN, scale=1)
    txt(img, x0 + 66, y0 + 68, "DRAW -1 MW", GREEN, scale=1)
    by = y0 + 84
    for j, (lbl, on) in enumerate((("BUY CR 1 500", True), ("TEST FIRE", False))):
        bx = x0 + j * 108
        b = (bx, by, bx + 100, by + 22)
        well(img, b, (60, 44, 0) if on else hud_r02.LCD_BG)
        ctxt(img, (b[0] + b[2]) / 2, by + 8, lbl, AMBER if on else LCD, scale=1)
    if test_fire:
        tb = (x1 - 110, y0 + 4, x1, y0 + 106)
        well(img, tb, (8, 6, 4))
        tx0, ty0 = tb[0] + 2, tb[1] + 2
        prev = Image.new("RGBA", (tb[2] - tb[0] - 3, tb[3] - tb[1] - 3), (40, 18, 8, 255))
        pd = ImageDraw.Draw(prev)
        rng = np.random.default_rng(4)
        for _ in range(30):
            x, y = rng.integers(0, prev.width), rng.integers(0, prev.height)
            pd.point((x, y), fill=(90, 40, 16))
        for (gx, gy) in ((24, 26), (70, 40)):
            pd.rectangle([gx - 6, gy - 6, gx + 6, gy + 6], fill=(80, 60, 50), outline=(140, 110, 90))
        pd.ellipse([60, 30, 80, 50], outline=(255, 160, 40))
        pd.ellipse([64, 34, 76, 46], fill=(255, 210, 90))
        for k in range(8):
            t = k / 7
            x = 52 + (70 - 52) * t
            y = 88 - 50 * t - 24 * np.sin(np.pi * t)
            pd.ellipse([x - 1, y - 1, x + 1, y + 1], fill=(255, 220, 140))
        sp = player_sprite().resize((24, 24), Image.LANCZOS)
        prev.alpha_composite(sp, (40, prev.height - 28))
        img.alpha_composite(prev, (tx0, ty0))
        txt(img, tb[0] + 4, tb[3] + 4, "TEST FIRE LOOP", DIM, scale=1)


def direction_dial(img, cx, cy, shares, r=30):
    """Ship icon with arrows from the attack directions, labelled with the share of waves."""
    d = ImageDraw.Draw(img)
    d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=(40, 60, 120))
    d.ellipse([cx - r // 2, cy - r // 2, cx + r // 2, cy + r // 2], outline=(30, 40, 90))
    tri(d, cx, cy - 2, 5, CYAN, "u")
    d.rectangle([cx - 1, cy - 2, cx + 1, cy + 6], fill=CYAN)
    pos = {"FRONT": (0, -1), "LEFT": (-1, 0), "RIGHT": (1, 0), "REAR": (0, 1)}
    for k, (dx, dy) in pos.items():
        v = shares.get(k, 0)
        col = ALERT if v else (40, 50, 80)
        ax, ay = cx + dx * (r + 6), cy + dy * (r + 6)
        tri(d, ax, ay, 6 if v else 4, col, {"FRONT": "d", "REAR": "u", "LEFT": "r",
                                            "RIGHT": "l"}[k])
        lbl = f"{k} {v}%"
        lx = ax - tw(lbl, 1) / 2 + dx * (tw(lbl, 1) / 2 + 10)
        ly = ay - 3 + dy * 12
        txt(img, int(lx), int(ly), lbl, WHITE if v else DIM, scale=1)


def intel_panel(img, x0, x1, y0, big_portrait=False):
    brief, hud = varga_portrait()
    plate(img, x0, y0, "INTEL - MISSION 15")
    if big_portrait:
        pw = 144
        well(img, (x0, y0 + 18, x0 + pw, y0 + 18 + pw), (0, 0, 0))
        img.alpha_composite(brief, (x0, y0 + 18))
        tx = x0 + pw + 10
        txt(img, tx, y0 + 22, "DR. VARGA", AMBER, scale=2)
        txt(img, tx, y0 + 40, "CDF INTEL", CYAN, scale=1)
        lines = hud_r02.wrap(QUOTE, (x1 - tx) // 6)
        for i, line in enumerate(lines[:8]):
            txt(img, tx, y0 + 58 + i * 12, line, LCD, scale=1)
        y = y0 + 18 + pw + 12
    else:
        well(img, (x0, y0 + 18, x0 + 72, y0 + 90), (0, 0, 0))
        img.alpha_composite(hud, (x0, y0 + 18))
        tx = x0 + 82
        txt(img, tx, y0 + 20, "DR. VARGA", AMBER, scale=2)
        lines = hud_r02.wrap(QUOTE, (x1 - tx) // 6)
        for i, line in enumerate(lines[:5]):
            txt(img, tx, y0 + 40 + i * 11, line, LCD, scale=1)
        y = y0 + 102
    txt(img, x0, y, "OLYMPUS DESCENT", WHITE, scale=2)
    d = ImageDraw.Draw(img)
    d.rectangle([x1 - 52, y, x1, y + 12], fill=(0, 60, 40), outline=LCD)
    txt(img, x1 - 48, y + 3, "SENSOR3", LCD, scale=1)
    y += 22
    for k, v in INTEL:
        txt(img, x0, y, k, METAL[4], scale=1, shadow=METAL[0])
        txt(img, x0 + 68, y, v, LCD, scale=1)
        y += 14
    txt(img, x0, y + 2, "DENSITY", METAL[4], scale=1, shadow=METAL[0])
    for i in range(5):
        d.rectangle([x0 + 68 + i * 14, y + 1, x0 + 78 + i * 14, y + 9],
                    fill=AMBER if i < 3 else (40, 40, 20))
    txt(img, x0 + 142, y + 2, "3/5", AMBER, scale=1)
    y += 18
    return y, d


def hangar_a():
    img = Image.new("RGBA", (SW, SH), (0, 0, 0, 255))
    topbar(img)
    cols = [(0, 300), (300, 670), (670, SW)]
    for i, (a, b) in enumerate(cols):
        metal_box(img, (a, 40, b, 500), 30 + i)
    metal_box(img, (0, 500, SW, SH), 40, rivets=False)
    d = ImageDraw.Draw(img)
    # ---- loadout
    plate(img, 12, 52, "LOADOUT")
    diag = (12, 70, 288, 296)
    well(img, diag, (4, 8, 22))
    for gx in range(diag[0], diag[2], 16):
        d.line([gx, diag[1], gx, diag[3]], fill=(10, 24, 50))
    for gy in range(diag[1], diag[3], 16):
        d.line([diag[0], gy, diag[2], gy], fill=(10, 24, 50))
    cx, cy = 150, 166
    ship = sprite.enlarge(player_sprite(), 2)
    img.alpha_composite(ship, (cx - ship.width // 2, cy - ship.height // 2))
    lines = [((cx, cy - 44), (cx, 113)), ((cx - 34, cy + 8), (62, 214)),
             ((cx + 34, cy + 8), (238, 214)), ((cx, cy + 40), (cx, 245))]
    for a, b in lines:
        d.line([a, b], fill=(0, 170, 255))
        d.ellipse([a[0] - 2, a[1] - 2, a[0] + 2, a[1] + 2], fill=(0, 220, 255))
    slot_box(img, cx, 96, "FRONT", "SCATTER VULCAN", 3, 5, on=True, w=136)
    slot_box(img, 62, 214, "L WING", "MICRO-MSL", 2, 3, w=96)
    slot_box(img, 238, 214, "R WING", "MICRO-MSL", 2, 3, w=96)
    slot_box(img, cx, 262, "REAR", "TAIL GUN", 1, 3, w=112)
    rook = rook_sprite()
    img.alpha_composite(rook, (40 - rook.width // 2, 262 - rook.height // 2))
    txt(img, 18, 284, "ESCORT: ROOK", (230, 140, 60), scale=1)
    y = 308
    for k, v in (("GEN", "MK III FUSION 14 MW"), ("SHD", "MK III  45 / 4 PER S"),
                 ("ARM", "COMPOSITE II  74/100"), ("ENG", "MK II  290 PX/S"),
                 ("SPC", "AIRSTRIKE  X2"), ("UTL", "SENSOR L3, MAGNET L1")):
        well(img, (12, y, 288, y + 22))
        txt(img, 18, y + 4, k, AMBER, scale=2)
        txt(img, 64, y + 8, v, LCD, scale=1)
        y += 30
    # ---- shop
    plate(img, 312, 52, "SHOP - FRONT GUNS")
    txt(img, 470, 55, "TRAIT MATCH", (255, 200, 60), scale=1)
    diamond(d, 462, 58, 4, (255, 200, 60))
    shop_list(img, 312, 658, 70)
    plate(img, 312, 286, "SELECTED")
    well(img, (312, 304, 658, 488), (6, 8, 30))
    item_detail(img, 320, 652, 312)
    txt(img, 320, 438, "OWNED ITEMS FIRST, THEN BUYABLE, THEN LOCKED.", DIM, scale=1)
    txt(img, 320, 452, "UNDO WITHIN THIS VISIT REFUNDS 100%.", DIM, scale=1)
    # ---- intel
    y, d = intel_panel(img, 682, 948, 52)
    txt(img, 682, y, "FROM", METAL[4], scale=1, shadow=METAL[0])
    direction_dial(img, 815, y + 52, {"FRONT": 100})
    y += 110
    txt(img, 682, y, "RECOMMEND", METAL[4], scale=1, shadow=METAL[0])
    for i, t in enumerate(("FORWARD", "ANTI-GROUND")):
        diamond(d, 690, y + 20 + i * 16, 4, (255, 200, 60))
        txt(img, 700, y + 16 + i * 16, t, (255, 220, 120), scale=2)
    # ---- power bar
    txt(img, 12, 510, "POWER", AMBER, scale=2)
    pw = (84, 508, 640, 528)
    well(img, pw, (6, 6, 14))
    out, load, proj = 14, 12, 11
    segw = (pw[2] - pw[0] - 4) / out
    for i in range(out):
        sx = pw[0] + 3 + i * segw
        c = AMBER if i < proj else ((140, 110, 30) if i < load else (20, 40, 20))
        d.rectangle([sx, pw[1] + 3, sx + segw - 3, pw[3] - 3], fill=c)
    txt(img, 652, 507, "12 / 14 MW", LCD, scale=2)
    txt(img, 652, 525, "WITH MORTAR: 11 / 14 MW  (+15% SHIELD REGEN)", (150, 220, 160), scale=1)
    return img


def hangar_b():
    img = Image.new("RGBA", (SW, SH), (0, 0, 0, 255))
    metal_box(img, (0, 0, SW, SH), 50, rivets=True, base=0.5)
    d = ImageDraw.Draw(img)
    # slim top bar
    txt(img, 16, 12, "HANGAR 7", WHITE, scale=3, shadow=METAL[0])
    txt(img, 180, 20, "BEFORE MISSION 15 - OLYMPUS DESCENT", METAL[4], scale=1, shadow=METAL[0])
    well(img, (640, 10, 790, 34))
    txt(img, 648, 15, "CR 12 450", AMBER, scale=2)
    lb = (806, 8, 948, 36)
    d.rectangle(lb, fill=(90, 20, 0))
    hazard_strip(img, (lb[0], lb[1], lb[0] + 20, lb[3]))
    d.rectangle(lb, outline=ALERT)
    tri(d, lb[0] + 34, 22, 6, AMBER)
    txt(img, lb[0] + 46, 15, "LAUNCH", AMBER, scale=2)
    # ---- left: shop drawer
    plate(img, 14, 50, "SHOP - FRONT GUNS")
    shop_list(img, 14, 290, 68, rh=24)
    plate(img, 14, 288, "SELECTED")
    well(img, (14, 306, 290, 500), (6, 8, 30))
    item_detail(img, 22, 284, 314, test_fire=False)
    tb = (22, 420, 284, 492)
    well(img, tb, (8, 6, 4))
    txt(img, 28, 426, "TEST FIRE", DIM, scale=1)
    prev = Image.new("RGBA", (tb[2] - tb[0] - 3, tb[3] - tb[1] - 3), (40, 18, 8, 255))
    pd = ImageDraw.Draw(prev)
    for gx in (60, 150, 210):
        pd.rectangle([gx - 6, 30, gx + 6, 42], fill=(80, 60, 50), outline=(140, 110, 90))
    pd.ellipse([140, 26, 160, 46], outline=(255, 160, 40))
    pd.ellipse([144, 30, 156, 42], fill=(255, 210, 90))
    for k in range(9):
        t = k / 8
        x, y = 30 + 120 * t, 54 - 30 * np.sin(np.pi * t)
        pd.ellipse([x - 1, y - 1, x + 1, y + 1], fill=(255, 220, 140))
    sp = player_sprite().resize((24, 24), Image.LANCZOS)
    prev.alpha_composite(sp, (18, 40))
    img.alpha_composite(prev, (tb[0] + 2, tb[1] + 2))
    # ---- centre: ship schematic
    sc = (304, 50, 656, 380)
    well(img, sc, (2, 10, 30))
    for gx in range(sc[0], sc[2], 12):
        d.line([gx, sc[1], gx, sc[3]], fill=(6, 28, 66))
    for gy in range(sc[1], sc[3], 12):
        d.line([sc[0], gy, sc[2], gy], fill=(6, 28, 66))
    for gx in range(sc[0], sc[2], 60):
        d.line([gx, sc[1], gx, sc[3]], fill=(10, 50, 110))
    for gy in range(sc[1], sc[3], 60):
        d.line([sc[0], gy, sc[2], gy], fill=(10, 50, 110))
    txt(img, sc[0] + 8, sc[1] + 6, "AF-12 STORMHAWK - SCHEMATIC", (0, 170, 255), scale=1)
    cx, cy = 480, 214
    ship = schematic_ship(190)
    img.alpha_composite(ship, (cx - ship.width // 2, cy - ship.height // 2))
    callouts = [((cx, cy - 80), (cx, 96), ("FRONT", "SCATTER VULCAN", 3, 5, True, 140)),
                ((cx - 58, cy + 16), (360, cy - 40), ("L WING", "MICRO-MSL", 2, 3, False, 100)),
                ((cx + 58, cy + 16), (600, cy - 40), ("R WING", "MICRO-MSL", 2, 3, False, 100)),
                ((cx, cy + 78), (cx, 338), ("REAR", "TAIL GUN", 1, 3, False, 112))]
    for a, b, (lbl, val, lv, mx, on, w) in callouts:
        d.line([a, b], fill=AMBER if on else (0, 200, 255))
        d.ellipse([a[0] - 3, a[1] - 3, a[0] + 3, a[1] + 3], outline=AMBER if on else
                  (0, 220, 255))
        slot_box(img, b[0], b[1], lbl, val, lv, mx, on=on, w=w)
    rook = rook_sprite()
    img.alpha_composite(rook, (sc[0] + 30 - rook.width // 2, sc[3] - 44))
    txt(img, sc[0] + 54, sc[3] - 36, "ESCORT", (0, 170, 170), scale=1)
    txt(img, sc[0] + 54, sc[3] - 24, "ROOK - EMBER", (230, 140, 60), scale=1)
    # core module strip
    mods = [("GEN", "MK III", "14 MW"), ("SHD", "MK III", "45"), ("ARM", "COMP II", "74/100"),
            ("ENG", "MK II", "290"), ("SPC", None, "X2"), ("UTL", "SENSOR", "L3"),
            ("UTL", "MAGNET", "L1")]
    mw = (sc[2] - sc[0] - 6 * 6) / 7
    for i, (k, v, n) in enumerate(mods):
        x0 = int(sc[0] + i * (mw + 6))
        well(img, (x0, 394, int(x0 + mw), 448))
        txt(img, x0 + 4, 398, k, AMBER, scale=2)
        if v is None:      # the special: airstrike icons instead of a name that won't fit
            for j in range(2):
                img.alpha_composite(hud_r02.airstrike_icon(AMBER, s=1), (x0 + 4 + j * 14, 418))
        else:
            txt(img, x0 + 4, 418, v, LCD, scale=1)
        txt(img, x0 + 4, 432, n, CYAN, scale=1)
    # power bar under the schematic
    txt(img, 304, 462, "POWER", AMBER, scale=1)
    pw = (304, 474, 656, 492)
    well(img, pw, (6, 6, 14))
    out, load, proj = 14, 12, 11
    segw = (pw[2] - pw[0] - 4) / out
    for i in range(out):
        sx = pw[0] + 3 + i * segw
        c = AMBER if i < proj else ((140, 110, 30) if i < load else (20, 40, 20))
        d.rectangle([sx, pw[1] + 3, sx + segw - 3, pw[3] - 3], fill=c)
    txt(img, 360, 462, "12/14 MW  WITH MORTAR 11/14  +15% REGEN", (150, 220, 160), scale=1)
    # ---- right: intel
    y, d = intel_panel(img, 670, 946, 50, big_portrait=True)
    txt(img, 670, y, "FROM", METAL[4], scale=1, shadow=METAL[0])
    direction_dial(img, 810, y + 48, {"FRONT": 100}, r=26)
    y += 100
    txt(img, 670, y, "RECOMMEND", METAL[4], scale=1, shadow=METAL[0])
    for i, t in enumerate(("FORWARD", "ANTI-GROUND")):
        diamond(d, 678, y + 20 + i * 16, 4, (255, 200, 60))
        txt(img, 688, y + 16 + i * 16, t, (255, 220, 120), scale=2)
    hazard_strip(img, (12, SH - 22, SW - 12, SH - 12))
    return img


# --------------------------------------------------------------------------- sheets

def sheet(screen, title, notes, crop_box):
    x0, y0, x1, y1 = crop_box
    crop = sprite.enlarge(screen.crop(crop_box), 2)
    h = 52 + SH + 34 + crop.height + 20
    img = raster.sheet(SW + 32, h, title, ROUND)
    raster.draw_text(img, 16, 38, "960X540 NATIVE SCREEN AT 1X.", raster.LABEL_DIM)
    img.alpha_composite(screen, (16, 52))
    raster.frame(img, (15, 51, 16 + SW, 52 + SH))
    cy = 52 + SH + 20
    raster.draw_text(img, 16, cy - 12, f"2X DETAIL ({x0},{y0})-({x1},{y1})", raster.LABEL_DIM)
    img.alpha_composite(crop, (16, cy))
    nx = 16 + crop.width + 20
    words = " ".join(notes)
    for i, line in enumerate(hud_r02.wrap(words, (img.width - nx - 12) // 6)):
        raster.draw_text(img, nx, cy + i * 14, line, raster.LABEL)
    return img


ITEMS = {
    "menu": [
        ("main-menu-r06-a.png", MENU_OUT, main_menu_a, "MAIN MENU A (R06): MENU OVER HERO SCENE",
         ["LOGO D OVER A PRE-RENDERED TITLE SCENE: EARTH'S LIMB,", "THE SUN, THE VRELL FLEET EMERGING TOP-RIGHT, THE",
          "STORMHAWK CLIMBING TO MEET IT (ANIMATED IN GAME).", "MENU IN A TRANSLUCENT GLASS PANEL WITH METAL TRIM.",
          "NEW GAME IS SELECTED: THE DIFFICULTY CHIPS OPEN", "INLINE WITH THE ONE-LINE DESCRIPTION.",
          "CONTINUE SHOWS WHERE THE LAST SAVE STANDS."], (586, 232, 920, 376)),
        ("main-menu-r06-b.png", MENU_OUT, main_menu_b, "MAIN MENU B (R06): METALLIC CONSOLE",
         ["THE WHOLE SCREEN IS A HUD A CONSOLE: LOGO ON A", "VIEWSCREEN, MENU ITEMS AS RECESSED LCD BUTTONS,",
          "SHIP STATUS LEFT, LAST SAVE AND A CDF NETWORK", "TICKER RIGHT, HAZARD STRIP AT THE BOTTOM.",
          "NEW GAME IS SELECTED WITH THE DIFFICULTY INLINE."], (300, 236, 630, 380)),
    ],
    "difficulty": [
        ("difficulty-r06-a.png", MENU_OUT, difficulty_a, "DIFFICULTY A (R06): CARDS OVER THE HERO SCENE",
         ["THREE RANK CARDS (RECRUIT / PILOT / ACE) WITH", "CHEVRONS, TAGLINE AND THE KEY LEVERS FROM",
          "SYSTEMS/DIFFICULTY. MEDIUM SELECTED, RAISED AND", "OUTLINED IN AMBER."], (345, 162, 615, 300)),
        ("difficulty-r06-b.png", MENU_OUT, difficulty_b, "DIFFICULTY B (R06): CONSOLE WITH PARAMETER TABLE",
         ["THREE LCD BUTTONS, THEN THE FULL LEVER TABLE FROM", "SYSTEMS/DIFFICULTY WITH THE SELECTED COLUMN",
          "FRAMED. START CAMPAIGN BUTTON BOTTOM RIGHT."], (346, 92, 676, 240)),
    ],
    "load": [
        ("load-game-r06-a.png", MENU_OUT, load_game_a, "LOAD GAME A (R06): SLOTS OVER THE HERO SCENE",
         ["AUTOSAVE + 8 MANUAL SLOTS: ACT ICON, NEXT MISSION,", "DIFFICULTY, CREDITS, PLAYTIME, DATE (SEE",
          "SYSTEMS/SAVES). THE SELECTED SLOT SHOWS A PREVIEW:", "SETTING THUMBNAIL, SCORE AND LOADOUT."],
         (36, 112, 366, 250)),
        ("load-game-r06-b.png", MENU_OUT, load_game_b, "LOAD GAME B (R06): CONSOLE SAVE ARCHIVE",
         ["THE SAME CONTENT IN THE CONSOLE STYLE: SLOTS AS", "LCD WELLS, DETAIL PANEL WITH LOAD / DELETE / BACK."],
         (628, 294, 938, 444)),
    ],
    "hangar": [
        ("hangar-r06-a.png", HANGAR_OUT, hangar_a, "HANGAR A (R06): THREE COLUMNS - LOADOUT / SHOP / INTEL",
         ["BEFORE M15 OLYMPUS DESCENT (ACT 3). FRONT SLOT", "SELECTED, SO THE SHOP SHOWS FRONT GUNS: FITTED,",
          "OWNED, BUYABLE WITH TRAIT-MATCH DIAMONDS, NEW TAG,", "LOCKED. HAMMER MORTAR SELECTED WITH COMPARE DELTAS,",
          "TEST-FIRE LOOP AND THE PROJECTED POWER LOAD. INTEL:", "VARGA (SENSOR L3), THREAT PROFILE, DIRECTIONS,",
          "RECOMMENDED TRAITS."], (312, 104, 642, 254)),
        ("hangar-r06-b.png", HANGAR_OUT, hangar_b, "HANGAR B (R06): CENTRAL SHIP SCHEMATIC",
         ["THE SHIP IS THE CENTRE: A BLUEPRINT SCHEMATIC WITH", "SLOT CALLOUTS, CORE MODULES UNDERNEATH AND THE",
          "POWER BAR. SHOP DRAWER LEFT, INTEL RIGHT WITH THE", "LARGE 144 PX VARGA PORTRAIT. SAME STATE AS A."],
         (318, 60, 648, 210)),
    ],
}


def main(args):
    MENU_OUT.mkdir(parents=True, exist_ok=True)
    HANGAR_OUT.mkdir(parents=True, exist_ok=True)
    groups = args or list(ITEMS)
    for g in groups:
        for name, out, fn, title, notes, crop in ITEMS[g]:
            screen = fn()
            path = out / name
            sheet(screen, title, notes, crop).convert("RGB").save(path, optimize=True)
            print("wrote", path.relative_to(ROOT))


if __name__ == "__main__":
    main(sys.argv[1:])
