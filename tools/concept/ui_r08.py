#!/usr/bin/env python3
"""Concept round 08 - the remaining UI screens for Acts 1-2, a refreshed HUD and the UI kit.

Outputs (full 960x540 screens at 1x on a sheet, plus a 2x detail crop unless noted):
  design/ui/briefing/concept/briefing-r08-a.png       briefing, L10 Evacuation Corridor
  design/ui/briefing/concept/act-title-r08-a.png      act title card, ACT II HOMEFRONT
  design/ui/debrief/concept/debrief-r08-a.png         mission complete, L10
  design/ui/debrief/concept/mission-failed-r08-a.png  mission failed (medium, boss checkpoint)
  design/ui/debrief/concept/game-over-r08-a.png       game over on hard + high-score entry
  design/ui/pause/concept/pause-r08-a.png             pause over the dimmed game
  design/ui/options/concept/options-r08-a.png         options: the four tabs side by side (2x2)
  design/ui/credits/concept/credits-r08-a.png         credits incl. the CC-BY attributions
  design/ui/hud/concept/hud-r08-a.png                 HUD A refresh (metal), L11 Kraken fight
  design/ui/concept/ui-kit-r08-a.png                  shared UI kit + bitmap font specimens

Style rule (user decision, round 06): out-of-game screens use the glass style of main menu A
(translucent dark glass, thin metal trim, amber selection bar and chips over a pre-rendered
scene); the bevelled metal of HUD A is only used for the in-game HUD. Text and radio blips only
(no voice acting). Controls: hold to fire with an auto-fire toggle; fire, special, precision.
Reuses ui_r06 / ui_r07 / hud_r02 / portraits code; nothing in those modules is changed.
Run: python3 tools/concept/ui_r08.py [briefing act-title debrief failed gameover pause
                                      options credits hud kit]
"""
import re
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

sys.path.insert(0, str(Path(__file__).resolve().parent))
import hud_r02  # noqa: E402
import logos  # noqa: E402
import parallax_r03  # noqa: E402
import portraits_r03 as r03  # noqa: E402
import portraits_r04 as r04  # noqa: E402
import ui_r06 as u6  # noqa: E402
import ui_r07 as u7  # noqa: E402
from render import raster, sprite  # noqa: E402
from render.config import FIELD_X, PANEL_W, ROOT  # noqa: E402

UI = ROOT / "design" / "ui"
OUT = {"briefing": UI / "briefing" / "concept", "debrief": UI / "debrief" / "concept",
       "pause": UI / "pause" / "concept", "options": UI / "options" / "concept",
       "credits": UI / "credits" / "concept", "hud": UI / "hud" / "concept",
       "kit": UI / "concept"}
SW, SH, P = u6.SW, u6.SH, PANEL_W
ROUND = "CONCEPT ROUND 08 - 960X540"

txt, tw, ctxt, glass, lit_bar, tri, diamond = u6.txt, u6.tw, u6.ctxt, u6.glass, u6.lit_bar, \
    u6.tri, u6.diamond
chip, header, inset, brackets = u7.chip, u7.header, u7.inset, u7.brackets
AMBER, CYAN, WHITE, DIM, GREEN, RED, ALERT, METAL = u6.AMBER, u6.CYAN, u6.WHITE, u6.DIM, \
    u6.GREEN, u6.RED, u6.ALERT, u6.METAL
LABEL, BODY, GOLD, HOLO = u7.LABEL, u7.BODY, u7.GOLD, u7.HOLO
LCD = hud_r02.LCD
SHADOW = (0, 0, 30)
_cache = {}


# --------------------------------------------------------------------------- shared assets

def portrait(slug):
    """Style-B portrait with retained colour (round 04): (144 px briefing, 72 px HUD)."""
    if ("portrait", slug) not in _cache:
        _cache[("portrait", slug)] = r04.style_b2(slug, r03.render_bust(slug))
    return _cache[("portrait", slug)]


def city_frame():
    """Scenery of the chosen night-megacity scene (round 03, scene B) at 480x540: the ground,
    fog and mist layers only, without the play plane (no ship, enemies or bullets)."""
    if "city" not in _cache:
        L = parallax_r03.get_scene("b").layer_images(24)
        order = [k for k in ("deep", "far", "ground", "low-air", "high-air") if k in L]
        img = L[order[0]].copy()
        for k in order[1:]:
            img.alpha_composite(L[k])
        _cache["city"] = img
    return _cache["city"].copy()


def city_backdrop(k=0.38, blur=3, tint=(0, 0, 0)):
    """The megacity frame enlarged to fill the screen, blurred and darkened (L10 context)."""
    key = ("city_bg", k, blur, tint)
    if key not in _cache:
        f = city_frame().resize((960, 1080), Image.LANCZOS).crop((0, 270, 960, 810))
        f = f.filter(ImageFilter.GaussianBlur(blur))
        arr = np.array(f).astype(np.float64)
        arr[..., :3] = arr[..., :3] * k + np.array(tint, float)
        yy, xx = np.mgrid[0:SH, 0:SW].astype(np.float64)
        vig = np.clip(1.25 - 0.6 * np.hypot((xx - SW / 2) / SW, (yy - SH / 2) / SH) * 2, 0.5, 1)
        arr[..., :3] *= vig[..., None]
        _cache[key] = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")
    return _cache[key].copy()


def kraken_frame(i=None):
    """A frame of the chosen Harbour Kraken animation (round 07) as the 480x540 play field."""
    gif = Image.open(ROOT / "design/enemies/bosses/concept/harbour-kraken-r07-a.gif")
    gif.seek(i if i is not None else 94)          # head up, lime eyes open, fan in flight
    return gif.convert("RGBA").copy()


def footer(img, hint):
    ctxt(img, SW / 2, SH - 18, hint, DIM, scale=1)


def bullet(img, x, y, color=AMBER, s=4):
    ImageDraw.Draw(img).rectangle([x, y, x + s, y + s], fill=color)


def menu_rows(img, box, items, sel, y0, step=30, scale=2, notes=None, disabled=()):
    """Vertical glass menu: items in white, the selected one on the amber bar with a cursor."""
    d = ImageDraw.Draw(img)
    y = y0
    for i, item in enumerate(items):
        x = box[0] + 34
        if i == sel:
            lit_bar(img, (box[0] + 2, y - 6, box[2] - 2, y + 7 * scale + 6))
            tri(d, box[0] + 18, y + 7 * scale // 2, 6, AMBER)
            txt(img, x, y, item, AMBER, scale=scale, shadow=(40, 30, 0))
        else:
            col = DIM if item in disabled else WHITE
            txt(img, x, y, item, col, scale=scale, shadow=(10, 10, 30))
        if notes and item in notes:
            txt(img, x, y + 7 * scale + 4, notes[item], CYAN, scale=1)
            y += 12
        y += step
    return y


def slider(img, x, y, w, value, label=None, show=True):
    inset(img, (x, y, x + w, y + 10))
    d = ImageDraw.Draw(img)
    fw = int((w - 4) * value)
    d.rectangle([x + 2, y + 2, x + 2 + fw, y + 7], fill=AMBER)
    d.line([x + 2, y + 2, x + 2 + fw, y + 2], fill=(255, 255, 180))
    kx = x + 2 + fw
    d.rectangle([kx - 3, y - 3, kx + 3, y + 13], fill=(220, 225, 245), outline=METAL[1])
    if show:
        txt(img, x + w + 10, y + 1, label if label is not None else f"{int(value * 100)}%",
            AMBER, scale=1)


def toggle(img, x, y, options, on, w=None, scale=1):
    """Row of chips with one active (the menu's EASY / MEDIUM / HARD chips)."""
    for i, o in enumerate(options):
        cw = w or tw(o, scale) + 16
        chip(img, (x, y, x + cw, y + 9 * scale + 6), o, on=(i == on), scale=scale)
        x += cw + 6
    return x


def stamp(text, color, size=78, angle=-9):
    """Rubber-stamp style grade letter in a double frame, rotated."""
    f = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", size)
    box = int(size * 1.35)
    img = Image.new("RGBA", (box, box), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([3, 3, box - 4, box - 4], outline=color + (255,), width=4)
    d.rectangle([10, 10, box - 11, box - 11], outline=color + (170,), width=2)
    bb = d.textbbox((0, 0), text, font=f)
    d.text(((box - (bb[2] - bb[0])) / 2 - bb[0], (box - (bb[3] - bb[1])) / 2 - bb[1]), text,
           font=f, fill=color + (255,))
    rng = np.random.default_rng(5)
    a = np.array(img).astype(np.float64)
    a[..., 3] *= np.clip(0.75 + rng.random(a.shape[:2]) * 0.4, 0, 1)
    return Image.fromarray(a.astype(np.uint8), "RGBA").rotate(angle, resample=Image.BICUBIC,
                                                              expand=True)


def chrome_title(text, size, stops=logos.BLUE_STEEL, tracking=12, depth=10, width=None):
    """Big chrome title in the logo D treatment (extrusion, glow, bevelled steel)."""
    mask = logos.pad(logos.text_mask(text, "bold", size, tracking=tracking), 30)
    W, H = mask.size[0] + 60, mask.size[1] + 60
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    cx, cy = W / 2, H / 2
    for k in range(depth, 0, -1):
        t = k / depth
        col = (int(20 + 30 * (1 - t)), int(30 + 50 * (1 - t)), int(70 + 90 * (1 - t)))
        logos.place(img, logos.solid(mask, col), cx + k * 0.9, cy + k * 1.2)
    glow = logos.solid(logos.dilate(mask, 4).filter(ImageFilter.GaussianBlur(9)),
                       (80, 170, 255), 0.6)
    logos.place(img, glow, cx, cy)
    logos.place(img, logos.solid(logos.dilate(mask, 2), (220, 240, 255)), cx, cy)
    logos.place(img, logos.surface(mask, stops, 4), cx, cy)
    img = img.crop(img.getbbox())
    if width:
        img = img.resize((width, int(img.height * width / img.width)), Image.LANCZOS)
    return img


def tinted(img, k, tint):
    arr = np.array(img).astype(np.float64)
    lum = arr[..., :3].mean(axis=-1, keepdims=True)
    arr[..., :3] = (arr[..., :3] * 0.4 + lum * 0.6) * k + np.array(tint, float)
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")


# --------------------------------------------------------------------------- 1 briefing

BRIEF_TEXT = ("LANCER, THE VRELL HAVE BROKEN INTO THE NOVA LAGOS OUTSKIRTS. THE EVACUATION "
              "CORRIDOR IS STILL OPEN - FOR NOW. SIX SHUTTLES ARE LIFTING OUT OF THE ARCOLOGY "
              "PADS. KEEP THE BUGS OFF THEM. AND WATCH YOUR SIX: SCANS SHOW CONTACTS SWINGING "
              "ROUND BEHIND THE CITY")
BRIEF_SHOWN = 0.86   # fraction of the page typed out (typewriter in progress)


def mission_map(w, h):
    """Tactical map of the corridor: the city rotated to landscape, cyan-tinted, with the
    shuttle route, friendly markers and the rear-threat arrows."""
    city = city_frame().rotate(90, expand=True)              # 540 x 480
    city = city.resize((w, int(city.height * w / city.width)), Image.LANCZOS)
    top = (city.height - h) // 2
    city = city.crop((0, top, w, top + h))
    arr = np.array(city).astype(np.float64)
    lum = arr[..., :3].mean(axis=-1)
    arr[..., :3] = lum[..., None] * np.array([0.25, 0.7, 1.0]) * 0.75 + 6
    img = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")
    ov = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(ov)
    for x in range(0, w, 32):
        d.line([x, 0, x, h], fill=(0, 160, 255, 40))
    for y in range(0, h, 32):
        d.line([0, y, w, y], fill=(0, 160, 255, 40))
    # corridor and route, left (arcology pads) to right (orbit lift)
    pts = [(30 + k * (w - 90) / 59, h * 0.55 + np.sin(k / 59 * 3.4) * h * 0.16) for k in range(60)]
    for k in range(0, 59):
        d.line([pts[k], pts[k + 1]], fill=(0, 220, 255, 60), width=26)
    for k in range(0, 59, 3):
        d.line([pts[k], pts[k + 1]], fill=(0, 230, 255, 230), width=2)
    tri(d, int(pts[-1][0]) + 14, int(pts[-1][1]), 9, (0, 230, 255, 255))
    for k in (8, 17, 26, 35, 44, 52):                        # six shuttles
        x, y = pts[k]
        d.rectangle([x - 5, y - 3, x + 5, y + 3], fill=(120, 255, 160, 255))
    # threat arrows: rear (from the left edge behind the shuttles) and front
    for y in (h * 0.25, h * 0.8):
        for i in range(3):
            tri(d, 18 + i * 12, int(y), 7, (255, 60, 50, 230 - i * 60), "r")
    for y in (h * 0.35, h * 0.65):
        tri(d, w - 18, int(y), 7, (255, 140, 40, 220), "l")
    img.alpha_composite(ov)
    raster.draw_text(img, 12, 10, "NOVA LAGOS - EVACUATION CORRIDOR", CYAN, scale=1)
    raster.draw_text(img, 12, h - 16, "ARCOLOGY PADS", (120, 255, 160), scale=1)
    s = "ORBITAL LIFT"
    raster.draw_text(img, w - 12 - tw(s, 1), h - 16, s, (0, 230, 255), scale=1)
    raster.draw_text(img, 46, int(h * 0.25) - 4, "CONTACTS ON SIX", (255, 90, 70), scale=1)
    return img


def briefing():
    img = city_backdrop(0.34, 3)
    d = ImageDraw.Draw(img)
    glass(img, (12, 10, 948, 44), alpha=190)
    txt(img, 26, 20, "ACT II - HOMEFRONT", AMBER, scale=2, shadow=(40, 30, 0))
    s = "MISSION 10: EVACUATION CORRIDOR"
    txt(img, 934 - tw(s, 2), 20, s, WHITE, scale=2, shadow=SHADOW)
    # speaker column
    glass(img, (12, 54, 232, 446), alpha=175)
    big, _ = portrait("okafor")
    px, py = 50, 68
    img.alpha_composite(big, (px, py))
    d.rectangle([px - 1, py - 1, px + 144, py + 144], outline=METAL[3])
    ctxt(img, 122, 222, "CMDR OKAFOR", AMBER, scale=2, shadow=(40, 30, 0))
    ctxt(img, 122, 242, "CDF COMMAND", CYAN, scale=1)
    ctxt(img, 122, 256, "PAGE 2 / 3", LABEL, scale=1)
    header(img, 24, 220, 280, "THREAT")
    rows = [("LAYERS", "AIR  HIGH-AIR"), ("DENSITY", ""), ("HAZARD", "CIVILIAN TRAFFIC")]
    y = 296
    for k, v in rows:
        txt(img, 24, y, k, LABEL, scale=1)
        if k == "DENSITY":
            for i in range(5):
                d.rectangle([90 + i * 14, y, 100 + i * 14, y + 7],
                            fill=AMBER if i < 3 else (40, 40, 60))
        else:
            txt(img, 90, y, v, CYAN, scale=1)
        y += 14
    u6.direction_dial(img, 122, 381, {"FRONT": 60, "REAR": 40}, r=19)
    txt(img, 24, 432, "RECOMMEND", LABEL, scale=1)
    for i, t in enumerate(("REAR", "FORWARD")):
        diamond(d, 96 + i * 66, 435, 3, GOLD)
        txt(img, 103 + i * 66, 432, t, (255, 220, 120), scale=1)
    # map + text
    glass(img, (242, 54, 948, 446), alpha=170)
    mx0, my0, mw, mh = 256, 66, 678, 222
    img.alpha_composite(mission_map(mw, mh), (mx0, my0))
    d.rectangle([mx0 - 1, my0 - 1, mx0 + mw, my0 + mh], outline=METAL[3])
    brackets(d, (mx0 - 4, my0 - 4, mx0 + mw + 3, my0 + mh + 3), HOLO)
    shown = BRIEF_TEXT[:int(len(BRIEF_TEXT) * BRIEF_SHOWN)]
    lines = hud_r02.wrap(shown, 55)
    y = 304
    txt(img, 256, y, '"', WHITE, scale=2)
    for i, line in enumerate(lines):
        x_end = txt(img, 268, y + i * 22, line, WHITE, scale=2, shadow=SHADOW)
    d.rectangle([x_end + 2, y + (len(lines) - 1) * 22, x_end + 11, y + (len(lines) - 1) * 22 + 13],
                fill=AMBER)                                   # typewriter cursor
    txt(img, 256, 424, "TEXT TYPES AT 60 CHARACTERS/S WITH A SOFT BLIP - ENTER SHOWS THE PAGE",
        DIM, scale=1)
    # objectives + hangar teaser
    glass(img, (12, 456, 948, 524), alpha=185)
    header(img, 26, 470, 464, "OBJECTIVES")
    bullet(img, 28, 482, AMBER)
    txt(img, 40, 480, "ESCORT THE SHUTTLES THROUGH THE CORRIDOR", WHITE, scale=1)
    bullet(img, 28, 498, CYAN)
    txt(img, 40, 496, "BONUS: NO FURTHER SHUTTLE LOST", CYAN, scale=1)
    header(img, 492, 934, 464, "IN THE HANGAR")
    txt(img, 492, 480, "REAR GUNS RECOMMENDED - TAIL GUN FITTED,", BODY, scale=1)
    txt(img, 492, 496, "FAN BLASTER AND HORNET LAUNCHER NEW IN THE SHOP", BODY, scale=1)
    footer_box = "ENTER CONTINUE    ESC SKIP TO OBJECTIVES"
    txt(img, 934 - tw(footer_box, 1), 512, footer_box, DIM, scale=1)
    return img


# --------------------------------------------------------------------------- 7 act title

def act_title():
    city = city_frame().resize((960, 1080), Image.LANCZOS).crop((0, 200, 960, 740))
    arr = np.array(city).astype(np.float64)
    yy, xx = np.mgrid[0:SH, 0:SW].astype(np.float64)
    grade = np.clip(0.25 + 0.75 * (yy / SH), 0, 1)[..., None]
    arr[..., :3] = arr[..., :3] * 0.5 * grade + np.array([10, 4, 30]) * (1 - grade)
    vig = np.clip(1.3 - 0.7 * np.hypot((xx - SW / 2) / SW, (yy - SH / 2) / SH) * 2, 0.35, 1)
    arr[..., :3] *= vig[..., None]
    img = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")
    img = raster.add_light(img, 480, 250, 300, (60, 100, 255), 0.35)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, SW, 58], fill=(0, 0, 0, 255))           # letterbox bars
    d.rectangle([0, SH - 58, SW, SH], fill=(0, 0, 0, 255))
    act = chrome_title("ACT II", 70, tracking=30, depth=6, width=230)
    img.alpha_composite(act, (SW // 2 - act.width // 2, 138))
    y = 138 + act.height // 2
    for x0, x1 in ((180, SW // 2 - act.width // 2 - 24), (SW // 2 + act.width // 2 + 24, 780)):
        d.line([x0, y, x1, y], fill=METAL[3])
        d.line([x0, y + 1, x1, y + 1], fill=METAL[1])
    title = chrome_title("HOMEFRONT", 150, tracking=16, depth=14, width=700)
    img.alpha_composite(title, (SW // 2 - title.width // 2, 214))
    ctxt(img, SW / 2, 214 + title.height + 24, "EARTH  -  NOVA LAGOS, ATLANTIC, ARCTIC, GENEVA",
         CYAN, scale=2, shadow=SHADOW)
    ctxt(img, SW / 2, 214 + title.height + 52, "THE WAR COMES HOME.", BODY, scale=1)
    ctxt(img, SW / 2, SH - 36, "MISSIONS 08 - 14", LABEL, scale=1)
    return img


# --------------------------------------------------------------------------- 2 debrief

def debrief():
    img = city_backdrop(0.3, 4, tint=(6, 2, 10))
    d = ImageDraw.Draw(img)
    box = (110, 24, 850, 512)
    glass(img, box, alpha=185)
    ctxt(img, SW / 2, 40, "MISSION 10 COMPLETE", AMBER, scale=3, shadow=(40, 30, 0))
    ctxt(img, SW / 2, 70, "EVACUATION CORRIDOR", WHITE, scale=2, shadow=SHADOW)
    x0, xv, xs = 140, 380, 820
    rows = [("ENEMIES DESTROYED", "142 / 158", "90 %", "+ 14 200"),
            ("ARMOUR DAMAGE TAKEN", "18", "", ""),
            ("SECRETS FOUND", "1 / 2", "", "+ 1 000"),
            ("MAX CHAIN", "41", "X3.0", ""),
            ("SHUTTLES SAVED", "5 / 5", "BONUS", "+ 600 CR")]
    y = 104
    header(img, x0, xs, y, "TALLY")
    y += 16
    for k, v, extra, sc in rows:
        txt(img, x0, y, k, LABEL, scale=2)
        txt(img, xv, y, v, WHITE, scale=2)
        if extra:
            txt(img, xv + 120, y, extra, CYAN, scale=2)
        if sc:
            txt(img, xs - tw(sc, 2), y, sc, AMBER if "CR" in sc else GREEN, scale=2)
        y += 24
    txt(img, x0, y + 2, "DATA CORE:", LABEL, scale=1)
    txt(img, x0 + 70, y + 2, "'EVACUATION ORDER 7 - NOVA LAGOS CIVIL DEFENCE'", CYAN, scale=1)
    y += 24
    header(img, x0, xs - 170, y, "CREDITS")
    y += 16
    money = [("BALANCE AT LAUNCH", "6 120"), ("CREDITS EARNED", "1 690"),
             ("SHUTTLE BONUS", "600"), ("GRADE BONUS  A  +20 %", "338")]
    xm = xs - 180
    for k, v in money:
        txt(img, x0, y, k, LABEL, scale=2)
        txt(img, xm - tw(v, 2), y, v, AMBER, scale=2)
        y += 22
    d.line([x0, y + 2, xm, y + 2], fill=METAL[3])
    y += 10
    txt(img, x0, y, "TOTAL CREDITS", WHITE, scale=2)
    txt(img, xm - tw("8 748", 2), y, "8 748", AMBER, scale=2, shadow=(40, 30, 0))
    y += 24
    txt(img, x0, y, "SCORE", WHITE, scale=2)
    txt(img, xm - tw("412 300", 2), y, "412 300", GREEN, scale=2)
    st = stamp("A", (255, 200, 40))
    sx, sy = 768, 362
    img.alpha_composite(st, (sx - st.width // 2, sy - st.height // 2))
    chip(img, (728, 428, 810, 444), "NEW BEST", on=True)
    ctxt(img, 768, 452, "GRADE", LABEL, scale=1)
    txt(img, x0, 482, "LINES APPEAR 0.3 S APART WITH A TICK; THE GRADE STAMP LANDS LAST.", DIM,
        scale=1)
    s = "ENTER CONTINUE"
    txt(img, 836 - tw(s, 2), 478, s, AMBER, scale=2)
    return img


# --------------------------------------------------------------------------- 8 HUD refresh

HUD_STATE = {
    "mission": (11, "ATLANTIC CONVOY"), "score": 652300, "credits": 7140,
    "chain": (22, 2.0, 0.45), "progress": 0.58,
    "armor": (71, 100), "shield": (12, 45), "power": (12, 14, 10),
    "weapons": [("FRONT", "SCATTER VULCAN", 3, 5), ("REAR", "TAIL GUN", 2, 3),
                ("LEFT WING", "MICRO-MISSILE", 2, 3), ("RIGHT WING", "TORPEDO POD", 1, 3)],
    "overdrive": (9, 0.45),
    "special": ("AIRSTRIKE", 2, 3),
    "radio": ("rook", "LT. ROOK", "AEGIS 2", "HEAD'S COMING UP - HIT THE EYES, LANCER!"),
    "escort": ("ROOK", 0.78),
    "boss": ("HARBOUR KRAKEN", 0.62),
}


class HudR08(hud_r02.Metal):
    """HUD A unchanged in style; element set updated: style-B radio portrait with signal,
    overdrive timer, escort armour with Rook's craft, boss bar and edge warnings."""

    def draw(self, img, st):
        st2 = dict(st, radio=("X X", ""), escort=None)        # base draws the static panels
        super().draw(img, st2)
        d = ImageDraw.Draw(img)
        # ---- left panel: radio with the style-B portrait (overdraw the r02 block)
        slug, name, unit, msg = st["radio"]
        self._clear(img, (12, 232, P - 12, 446))
        self.plate(img, 14, 234, "RADIO")
        self.well(img, (14, 250, 88, 324), (4, 8, 10))
        img.alpha_composite(portrait(slug)[1], (15, 251))
        txt = hud_r02.txt
        txt(img, 98, 254, name.split()[0], hud_r02.AMBER)
        txt(img, 98, 272, " ".join(name.split()[1:]), hud_r02.AMBER)
        txt(img, 98, 292, unit, hud_r02.CYAN, scale=1)
        for i, hgt in enumerate((3, 6, 9, 12, 8, 5, 3)):          # signal bars
            d.rectangle([98 + i * 7, 318 - hgt, 102 + i * 7, 318], fill=LCD)
        self.well(img, (14, 336, P - 15, 410))
        for i, line in enumerate(hud_r02.wrap(msg, 17)[:3]):
            txt(img, 20, 342 + i * 22, line, LCD)
        txt(img, 14, 420, "MSG 1/2  QUEUED: VARGA", METAL[4], scale=1)
        # ---- right panel: overdrive running, escort with Rook
        rx, rw = SW - P + 14, P - 29
        y = 170 + 4 * 48
        self._clear(img, (rx - 2, y, rx + rw + 2, SH - 12))
        secs, frac = st["overdrive"]
        self.plate(img, rx, y + 2, "OVERDRIVE")
        txt(img, rx + rw - 32, y + 2, f"{secs:>2}S", hud_r02.AMBER)
        self.bar(img, rx, y + 20, rw, frac, (255, 120, 255), segs=20, h=12)
        y += 46
        sname, count, mx = st["special"]
        self.plate(img, rx, y, "SPECIAL")
        self.well(img, (rx, y + 18, rx + rw, y + 48))
        txt(img, rx + 6, y + 26, sname, hud_r02.AMBER)
        for i in range(mx):
            img.alpha_composite(hud_r02.airstrike_icon(hud_r02.AMBER, i >= count),
                                (rx + rw - 28 - (mx - 1 - i) * 26, y + 22))
        y += 60
        ename, earm = st["escort"]
        self.plate(img, rx, y, "ESCORT")
        self.well(img, (rx, y + 18, rx + rw, y + 70))
        rook = u6.rook_sprite()
        img.alpha_composite(rook, (rx + 4, y + 22))
        txt(img, rx + 52, y + 24, ename, LCD)
        txt(img, rx + 52, y + 42, "FORMATION: COVER", METAL[4], scale=1)
        self.bar(img, rx + 52, y + 54, rw - 58, earm, hud_r02.ALERT, segs=12, h=10)
        # ---- play field: boss bar and edge warnings
        bname, bhp = st["boss"]
        fx0, fx1 = FIELD_X + 12, FIELD_X + 480 - 12
        self.well(img, (fx0, 10, fx1, 26), (10, 4, 8))
        segs = 40
        sw = (fx1 - fx0 - 3) / segs
        for i in range(segs):
            on = i < round(bhp * segs)
            c = (255, 50, 60) if on else (60, 14, 20)
            d.rectangle([fx0 + 2 + i * sw, 13, fx0 + 2 + i * sw + sw - 2, 23], fill=c)
        txt(img, fx0, 32, bname, (255, 120, 120), scale=1, shadow=(20, 0, 0))
        txt(img, fx1 - tw("WEAK POINT: EYES", 1), 32, "WEAK POINT: EYES", (168, 255, 42),
            scale=1, shadow=(0, 20, 0))
        # edge warning: a Skimmer wave from the left edge in 1.5 s
        ex = FIELD_X + 2
        for i, a in enumerate((255, 170, 90)):
            tri(d, ex + 8 + i * 9, 240, 9, (255, 200, 0, a), "l")
            tri(d, ex + 8 + i * 9, 268, 9, (255, 200, 0, a), "l")
        d.rectangle([ex, 226, ex + 3, 282], fill=(255, 200, 0))
        hud_r02.txt(img, ex + 36, 250, "! LEFT", (255, 200, 0), scale=1, shadow=(30, 20, 0))
        # floating credits
        hud_r02.txt(img, FIELD_X + 92, 432, "+40", hud_r02.AMBER, scale=1, shadow=(30, 20, 0))

    def _clear(self, img, box):
        """Repaint a panel area with clean brushed metal (to overdraw r02 content)."""
        x0, y0, x1, y1 = box
        img.alpha_composite(u6.brushed(x1 - x0, y1 - y0, 11 + x0 + y0, 0.55), (x0, y0))


def hud_screen():
    if "hud" not in _cache:
        screen = Image.new("RGBA", (SW, SH), (0, 0, 0, 255))
        screen.alpha_composite(kraken_frame(), (FIELD_X, 0))
        HudR08().draw(screen, HUD_STATE)
        _cache["hud"] = screen
    return _cache["hud"].copy()


# --------------------------------------------------------------------------- 3 pause

def pause():
    img = u6.darken(hud_screen(), 0.38, blur=2)
    box = (318, 120, 642, 420)
    glass(img, box, alpha=200)
    ctxt(img, SW / 2, 136, "PAUSED", AMBER, scale=3, shadow=(40, 30, 0))
    ctxt(img, SW / 2, 166, "MISSION 11 - ATLANTIC CONVOY", CYAN, scale=1)
    ctxt(img, SW / 2, 180, "TIME 4:12   SCORE 652 300   MEDIUM", LABEL, scale=1)
    items = ["RESUME", "RESTART MISSION", "OPTIONS", "ABORT TO HANGAR", "QUIT TO MAIN MENU"]
    menu_rows(img, box, items, 0, 206, step=32)
    txt(img, box[0] + 16, 372, "RESTART AND ABORT RETURN TO THE", DIM, scale=1)
    txt(img, box[0] + 16, 384, "LEVEL-START STATE (CONFIRMATION).", DIM, scale=1)
    txt(img, box[0] + 16, 398, "RESUME COUNTS 3-2-1 BEFORE PLAY.", DIM, scale=1)
    footer(img, "UP/DOWN SELECT    ENTER CONFIRM    ESC RESUME")
    return img


# --------------------------------------------------------------------------- 4 options

TABS = ["VIDEO", "AUDIO", "CONTROLS", "GAMEPLAY"]


def options_base(tab):
    img = u6.darken(u6.hero_backdrop(), 0.42, blur=2)
    box = (70, 30, 890, 506)
    glass(img, box, alpha=190)
    txt(img, 90, 44, "OPTIONS", WHITE, scale=3, shadow=SHADOW)
    x = 300
    for t in TABS:
        cw = tw(t, 2) + 24
        chip(img, (x, 44, x + cw, 66), t, on=(t == tab), scale=2)
        x += cw + 8
    d = ImageDraw.Draw(img)
    d.line([90, 78, 870, 78], fill=METAL[2])
    footer(img, "UP/DOWN SELECT    LEFT/RIGHT CHANGE    Q/E TAB    ESC BACK")
    return img, box


def option_row(img, y, label, sel=False, x0=90, x1=870, h=26):
    if sel:
        lit_bar(img, (x0 - 18, y - 6, x1, y + h - 8))
        tri(ImageDraw.Draw(img), x0 - 8, y + 6, 5, AMBER)
    txt(img, x0, y, label, AMBER if sel else WHITE, scale=2, shadow=SHADOW)


def options_video():
    img, box = options_base("VIDEO")
    y = 96
    option_row(img, y, "DISPLAY MODE")
    toggle(img, 380, y - 2, ["FULLSCREEN", "BORDERLESS", "WINDOWED"], 0)
    y += 34
    option_row(img, y, "RESOLUTION")
    txt(img, 380, y, "2560 X 1440 (NATIVE)", CYAN, scale=2)
    y += 34
    option_row(img, y, "SCALING", sel=True)
    toggle(img, 380, y - 2, ["INTEGER + LETTERBOX", "SMOOTH, FILL SCREEN"], 0)
    txt(img, 380, y + 20, "960X540 X2 = 1920X1080, BLACK BORDERS ROUND IT.", LABEL, scale=1)
    # preview diagram
    d = ImageDraw.Draw(img)
    px, py, pw, ph = 380, y + 40, 256, 144
    inset(img, (px, py, px + pw, py + ph), fill=(0, 0, 0, 255))
    iw, ih = int(pw * 1920 / 2560), int(ph * 1080 / 1440)
    ix, iy = px + (pw - iw) // 2, py + (ph - ih) // 2
    thumb = u6.hero_backdrop().resize((iw, ih), Image.NEAREST)
    img.alpha_composite(thumb, (ix, iy))
    d.rectangle([ix - 1, iy - 1, ix + iw, iy + ih], outline=AMBER)
    txt(img, px + pw + 14, py + 4, "SCREEN 2560X1440", LABEL, scale=1)
    txt(img, px + pw + 14, py + 18, "GAME 1920X1080 (X2)", AMBER, scale=1)
    txt(img, px + pw + 14, py + 32, "CRISP PIXELS", GREEN, scale=1)
    txt(img, px + pw + 14, py + 56, "SMOOTH: X2.67, FILLS", LABEL, scale=1)
    txt(img, px + pw + 14, py + 70, "THE SCREEN, SLIGHTLY SOFT", LABEL, scale=1)
    y = py + ph + 22
    option_row(img, y, "CRT SCANLINES")
    toggle(img, 380, y - 2, ["OFF", "LIGHT", "STRONG"], 0)
    y += 34
    option_row(img, y, "VSYNC")
    toggle(img, 380, y - 2, ["ON", "OFF"], 0)
    y += 34
    option_row(img, y, "BRIGHTNESS")
    slider(img, 380, y + 2, 300, 0.5, "0")
    return img


def options_audio():
    img, box = options_base("AUDIO")
    y = 100
    for i, (k, v) in enumerate((("MASTER", 0.85), ("MUSIC", 0.7), ("EFFECTS", 0.8),
                                ("INTERFACE", 0.6), ("RADIO BLIPS", 0.65))):
        option_row(img, y, k, sel=(i == 1))
        slider(img, 380, y + 2, 360, v)
        y += 40
    option_row(img, y, "MUSIC STYLE")
    toggle(img, 380, y - 2, ["FULL MIX", "LEVEL THEMES ONLY"], 0)
    y += 40
    option_row(img, y, "SOUND TEST")
    chip(img, (380, y - 2, 600, y + 16), "AFTERBURNER (ACT 1 A)", scale=1)
    txt(img, 612, y + 2, "ENTER PLAY", LABEL, scale=1)
    y += 46
    txt(img, 90, y, "RADIO MESSAGES ARE TEXT WITH A BLIP PER LINE; THERE IS NO VOICE ACTING.",
        LABEL, scale=1)
    return img


CONTROLS = [("MOVE", "ARROWS", "W A S D", "L-STICK / D-PAD"),
            ("FIRE (HOLD)", "SPACE", "Z", "A / R-TRIGGER"),
            ("SPECIAL", "X", "L-CTRL", "B"),
            ("PRECISION (HOLD)", "L-SHIFT", "C", "R-BUMPER"),
            ("DASH", "DOUBLE-TAP", "V", "L-BUMPER"),
            ("PAUSE", "ESC", "P", "START")]


def options_controls():
    img, box = options_base("CONTROLS")
    d = ImageDraw.Draw(img)
    toggle(img, 90, 92, ["KEYBOARD", "GAMEPAD"], 0, scale=2)
    cols = (90, 330, 500, 660)
    y = 132
    for c, h in zip(cols, ("ACTION", "PRIMARY", "ALTERNATIVE", "GAMEPAD")):
        txt(img, c, y, h, LABEL, scale=1)
    y += 16
    for i, row in enumerate(CONTROLS):
        u7.row_band(img, (84, y - 4, 870, y + 18), i == 2)
        for j, (c, v) in enumerate(zip(cols, row)):
            if i == 2 and j == 1:                       # remapping in progress
                chip(img, (c - 4, y - 3, c + 150, y + 15), "PRESS A KEY...", on=True)
                continue
            col = AMBER if (i == 2 and j == 0) else (WHITE if j == 0 else CYAN)
            txt(img, c, y, v, col, scale=2 if j == 0 else 1)
        y += 26
    y += 10
    option_row(img, y, "AUTO-FIRE")
    toggle(img, 380, y - 2, ["OFF", "ON"], 0, scale=2)
    txt(img, 520, y + 2, "OFF: HOLD FIRE TO SHOOT", LABEL, scale=1)
    txt(img, 520, y + 14, "ON: ALWAYS FIRING, NO BUTTON NEEDED", LABEL, scale=1)
    y += 40
    option_row(img, y, "STICK DEAD ZONE")
    slider(img, 380, y + 2, 300, 0.2)
    y += 40
    chip(img, (90, y, 290, y + 20), "RESET TO DEFAULTS", scale=1)
    txt(img, 310, y + 6, "CONFLICT: 'Z' IS ALREADY FIRE (ALTERNATIVE) - ENTER SWAPS THE TWO",
        (255, 150, 90), scale=1)
    return img


def options_gameplay():
    img, box = options_base("GAMEPLAY")
    y = 100
    rows = [("TEXT SPEED", "slider", 0.6, "60 CPS"), ("SCREEN SHAKE", "slider", 0.7, "70%"),
            ("FLOATING CREDIT NUMBERS", "toggle", ["ON", "OFF"], 0),
            ("RADIO SUBTITLE BOX", "toggle", ["FULL", "COMPACT"], 0),
            ("HUD BRIGHTNESS", "slider", 0.9, "90%"),
            ("PAUSE ON FOCUS LOSS", "toggle", ["ON", "OFF"], 0),
            ("LANGUAGE", "toggle", ["ENGLISH"], 0)]
    for i, (k, kind, a, b) in enumerate(rows):
        option_row(img, y, k, sel=(i == 2))
        if kind == "slider":
            slider(img, 420, y + 2, 300, a, b)
        else:
            toggle(img, 420, y - 2, a, b)
        y += 38
    txt(img, 90, y + 8, "EDGE WARNINGS FOR SIDE AND REAR WAVES ARE ALWAYS ON (READABILITY RULE).",
        LABEL, scale=1)
    txt(img, 90, y + 22, "DIFFICULTY IS CHOSEN PER CAMPAIGN WHEN STARTING A NEW GAME.", LABEL,
        scale=1)
    return img


# --------------------------------------------------------------------------- 5 credits

def cc_by_entries():
    """Chosen CC-BY assets from CREDITS.md (files that still exist outside rejected/)."""
    out = []
    for line in (ROOT / "CREDITS.md").read_text().splitlines():
        if not line.startswith("| design/") or "CC-BY" not in line:
            continue
        cells = [c.strip() for c in line.strip("|").split("|")]
        path, title, author, lic = cells[0], cells[1], cells[2], cells[4]
        if not (ROOT / path).exists() or "/rejected/" in path:
            continue
        lic = re.sub(r"\[([^\]]+)\]\([^)]*\)", r"\1", lic)
        title = re.sub(r"\.wav$", "", title, flags=re.I)
        out.append((title.upper(), author.upper(), lic.upper()))
    return out


def cc0_authors():
    names = []
    for line in (ROOT / "CREDITS.md").read_text().splitlines():
        if line.startswith("| design/") and "CC0" in line:
            cells = [c.strip() for c in line.strip("|").split("|")]
            if (ROOT / cells[0]).exists() and cells[2].upper() not in names:
                names.append(cells[2].upper())
    return names


def credits():
    img = u6.darken(u6.hero_backdrop(), 0.4, blur=1)
    box = (220, 0, 740, SH)
    glass(img, box, alpha=170, trim=False)
    d = ImageDraw.Draw(img)
    d.line([220, 0, 220, SH], fill=METAL[2])
    d.line([739, 0, 739, SH], fill=METAL[2])
    cx = SW / 2
    y = -30                                         # scrolled: the logo is leaving the top
    logo = u6.logo_layer(300)
    img.alpha_composite(logo, (int(cx - logo.width / 2), y))
    y += logo.height + 18
    blocks = [("A GAME BY", ["(CREATOR)"]),
              ("DESIGN AND DIRECTION", ["(TBD)"]),
              ("PROGRAMMING", ["(TBD)"]),
              ("ART, MUSIC AND SOUND DESIGN", ["(TBD)"])]
    for head, names in blocks:
        ctxt(img, cx, y, head, AMBER, scale=1)
        y += 14
        for n in names:
            ctxt(img, cx, y, n, WHITE, scale=2)
            y += 20
        y += 10
    ctxt(img, cx, y, "SOUND EFFECTS - CC-BY", AMBER, scale=2, shadow=(40, 30, 0))
    y += 24
    for title, author, lic in cc_by_entries():
        ctxt(img, cx, y, f"'{title}' BY {author}", WHITE, scale=1)
        ctxt(img, cx, y + 12, f"{lic} - FREESOUND.ORG", CYAN, scale=1)
        y += 30
    y += 6
    ctxt(img, cx, y, "ADDITIONAL SOUNDS - CC0, WITH THANKS TO", AMBER, scale=1)
    y += 16
    names = cc0_authors()
    line = ""
    for n in names:
        cand = f"{line}, {n}" if line else n
        if tw(cand, 1) > 470:
            ctxt(img, cx, y, line, BODY, scale=1)
            y += 12
            line = n
        else:
            line = cand
    if line:
        ctxt(img, cx, y, line, BODY, scale=1)
    # fade at top and bottom (the list scrolls upwards)
    arr = np.array(img).astype(np.float64)
    yy = np.arange(SH)[:, None]
    fade = np.clip(np.minimum(yy / 60, (SH - yy) / 60), 0.15, 1)
    arr[:, 220:740, :3] *= fade[..., None]
    img = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")
    txt(img, 760, SH - 30, "ENTER SPEEDS UP   ESC BACK", DIM, scale=1)
    txt(img, 20, SH - 30, "THE LIST SCROLLS UPWARDS", DIM, scale=1)
    return img


# --------------------------------------------------------------------------- 6 failed / over

def mission_failed():
    img = tinted(hud_screen(), 0.42, (40, 0, 4)).filter(ImageFilter.GaussianBlur(2))
    box = (210, 110, 750, 440)
    glass(img, box, alpha=200)
    d = ImageDraw.Draw(img)
    ctxt(img, SW / 2, 124, "MISSION FAILED", (255, 70, 60), scale=3, shadow=(40, 0, 0))
    ctxt(img, SW / 2, 152, "MISSION 11 - ATLANTIC CONVOY - MEDIUM", LABEL, scale=1)
    big, _ = portrait("okafor")
    px, py = 228, 176
    img.alpha_composite(big, (px, py))
    d.rectangle([px - 1, py - 1, px + 144, py + 144], outline=METAL[3])
    txt(img, px, py + 152, "CMDR OKAFOR", AMBER, scale=1)
    for i, line in enumerate(("PULL BACK, LANCER.", "REGROUP AND TRY AGAIN.")):
        txt(img, px, py + 166 + i * 12, line, WHITE, scale=1)
    mbox = (388, 172, 740, 360)
    items = ["RETRY", "RETRY FROM BOSS", "BACK TO HANGAR", "QUIT TO MAIN MENU"]
    menu_rows(img, mbox, items, 0, 184, step=34,
              notes={"RETRY FROM BOSS": "CHECKPOINT AT THE KRAKEN WARNING"})
    txt(img, 404, 370, "THIS ATTEMPT IS DISCARDED:", LABEL, scale=1)
    txt(img, 404, 384, "1 120 CR   98 400 PTS", (255, 150, 90), scale=1)
    txt(img, 404, 404, "BACK TO HANGAR LETS YOU", DIM, scale=1)
    txt(img, 404, 416, "CHANGE THE LOADOUT FIRST.", DIM, scale=1)
    footer(img, "UP/DOWN SELECT    ENTER CONFIRM")
    return img


HISCORES = [("ACE", 2410800, "M24"), ("VEX", 2034500, "M21"), ("KIRA", 1690300, "M17"),
            (None, 1482650, "M12"), ("BOLT", 1290100, "M12"), ("SAM", 1105700, "M10"),
            ("NOVA", 980400, "M09"), ("JUNO", 744200, "M07"), ("REX", 502300, "M05"),
            ("ZED", 310900, "M03")]


def game_over():
    base = Image.new("RGBA", (SW, SH), (0, 0, 0, 255))
    base.alpha_composite(tinted(u6.hero_backdrop(), 0.3, (10, 0, 12)).filter(ImageFilter.GaussianBlur(2)))
    img = base
    d = ImageDraw.Draw(img)
    title = chrome_title("GAME OVER", 120, stops=[(0.0, (120, 20, 20)), (0.45, (255, 200, 190)),
                                                  (0.5, (110, 10, 20)), (1.0, (255, 90, 70))],
                         tracking=14, depth=10, width=470)
    img.alpha_composite(title, (SW // 2 - title.width // 2, 18))
    y0 = 30 + title.height
    ctxt(img, SW / 2, y0, "HARD - RETRIES 0 / 3 - MISSION 12 STORM FRONT", (255, 120, 100), scale=2,
         shadow=(30, 0, 0))
    # stats
    glass(img, (40, y0 + 30, 340, 500), alpha=185)
    header(img, 54, 326, y0 + 40, "CAMPAIGN")
    rows = [("SCORE", "1 482 650"), ("FURTHEST", "MISSION 12"), ("PLAYTIME", "5:12:40"),
            ("ENEMIES", "2 310"), ("BEST GRADE", "S (M07)"), ("CREDITS SPENT", "21 380")]
    y = y0 + 58
    for k, v in rows:
        txt(img, 54, y, k, LABEL, scale=1)
        txt(img, 326 - tw(v, 2), y - 3, v, WHITE if k != "SCORE" else GREEN, scale=2)
        y += 26
    header(img, 54, 326, y + 6, "LAST TRANSMISSION")
    for i, line in enumerate(("AEGIS ONE IS DOWN. WE HOLD THE", "LINE WITHOUT YOU, LANCER.",
                              "- CMDR OKAFOR")):
        txt(img, 54, y + 24 + i * 13, line, BODY if i < 2 else AMBER, scale=1)
    for i, line in enumerate(("THE LAST SAVE CAN BE LOADED", "FROM THE MAIN MENU.")):
        txt(img, 54, 462 + i * 12, line, DIM, scale=1)
    # high-score table with entry
    glass(img, (356, y0 + 30, 920, 500), alpha=185)
    header(img, 370, 906, y0 + 40, "HIGH SCORES - HARD")
    y = y0 + 58
    for i, (name, score, lvl) in enumerate(HISCORES):
        rank = f"{i + 1:>2}"
        if name is None:
            u7.row_band(img, (362, y - 4, 914, y + 16), True)
            txt(img, 372, y, rank, AMBER, scale=2)
            x = txt(img, 412, y, "LANC", AMBER, scale=2)
            d.rectangle([x + 1, y + 12, x + 11, y + 14], fill=AMBER)    # entry cursor
            col = AMBER
        else:
            txt(img, 372, y, rank, LABEL, scale=2)
            txt(img, 412, y, name, WHITE, scale=2)
            col = BODY
        s = f"{score:,}".replace(",", " ")
        txt(img, 720 - tw(s, 2), y, s, col, scale=2)
        txt(img, 760, y, lvl, CYAN, scale=2)
        y += 24
    # letter grid
    gy = y + 4
    chars = list("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.-") + ["DEL", "END"]
    gx = 372
    for i, c in enumerate(chars):
        col_i, row_i = i % 20, i // 20
        w = 22 if len(c) == 1 else 42
        x = gx + (col_i * 26 if row_i == 0 else (col_i * 26 if col_i < 18 else 18 * 26 + (col_i - 18) * 48))
        yy = gy + row_i * 24
        chip(img, (x, yy, x + w, yy + 18), c, on=(c == "E" and row_i == 0), scale=1)
    footer(img, "ARROWS PICK A LETTER    ENTER ADD    END CONFIRM")
    return img


# --------------------------------------------------------------------------- 9 UI kit

FONT_SPECS = [("LABEL 8X12", 8, 12, 9), ("BODY 10X20", 10, 20, 15), ("HEADING 20X30", 20, 30, 26)]
CHARSET = [" !\"#$%&'()*+,-./0123456789:;<=>?", "@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_",
           "`abcdefghijklmnopqrstuvwxyz{|}~", "×·◆▲▼◄►•°—…éèëüöäß©"]
FONT_PATH = "/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf"


def bitmap_glyphs(cell_w, cell_h, size):
    """Proposed production bitmap font: DejaVu Sans Mono Bold rasterised 1-bit into a fixed
    cell (no anti-aliasing), the way a late-90s game font sheet would be made."""
    f = ImageFont.truetype(FONT_PATH, size)
    asc, desc = f.getmetrics()
    out = {}
    for row in CHARSET:
        for ch in row:
            g = Image.new("L", (cell_w, cell_h), 0)
            d = ImageDraw.Draw(g)
            d.fontmode = "1"
            bb = d.textbbox((0, 0), ch, font=f)
            gw = bb[2] - bb[0]
            x = (cell_w - gw) // 2 - bb[0]
            y = (cell_h - (asc + desc)) // 2 + max(0, (asc + desc - cell_h) // 2) * 0
            d.text((x, y), ch, font=f, fill=255)
            out[ch] = g
    return out


def draw_bitmap_text(img, x, y, s, glyphs, color):
    for ch in s:
        g = glyphs.get(ch)
        if g is not None:
            layer = Image.new("RGBA", g.size, color + (0,))
            layer.putalpha(g)
            img.alpha_composite(layer, (x, y))
        x += g.size[0] if g is not None else 8
    return x


def kit():
    W, H = 1400, 930
    img = raster.sheet(W, H, "UI KIT (R08): GLASS FOR MENUS, METAL FOR THE HUD, BITMAP FONTS", ROUND)
    d = ImageDraw.Draw(img)
    # ---- glass section over a scene strip
    gx0, gy0, gx1, gy1 = 16, 52, 840, 470
    bg = u6.darken(u6.hero_backdrop(), 0.5).crop((60, 0, 60 + gx1 - gx0, gy1 - gy0))
    img.alpha_composite(bg, (gx0, gy0))
    raster.draw_text(img, gx0, gy0 - 12, "GLASS STYLE - ALL OUT-OF-GAME SCREENS", raster.ACCENT)
    glass(img, (32, 66, 330, 250))
    header(img, 44, 318, 76, "PANEL HEADER")
    txt(img, 44, 94, "GLASS PANEL, ALPHA 178, THIN", BODY, scale=1)
    txt(img, 44, 106, "METAL TRIM, CORNER TABS.", BODY, scale=1)
    txt(img, 44, 124, "LABEL", LABEL, scale=1)
    txt(img, 110, 124, "VALUE", CYAN, scale=1)
    txt(img, 44, 140, "BODY TEXT ON GLASS", WHITE, scale=2)
    txt(img, 44, 162, "AMBER = SELECTED / MONEY", AMBER, scale=1)
    txt(img, 44, 176, "GREEN = GAIN   RED = LOSS", GREEN, scale=1)
    inset(img, (44, 196, 318, 236))
    txt(img, 54, 210, "INSET (TEST FIRE, BARS, MAPS)", LABEL, scale=1)
    # buttons in states
    glass(img, (346, 66, 824, 250))
    header(img, 358, 812, 76, "MENU ITEMS AND BUTTONS")
    menu_rows(img, (346, 66, 600, 250), ["NORMAL", "SELECTED", "DISABLED"], 1, 96, step=30,
              disabled=("DISABLED",))
    for i, (t, on, dis) in enumerate((("CHIP", False, False), ("CHIP ON", True, False),
                                      ("LOCKED", False, True))):
        x = 620
        yy = 96 + i * 30
        chip(img, (x, yy, x + 120, yy + 20), t, on=on, scale=1,
             color=(DIM if dis else None))
    lb = (620, 186, 812, 222)
    glass(img, lb, alpha=200)
    lit_bar(img, (lb[0] + 1, lb[1] + 1, lb[2] - 1, lb[3] - 1))
    d.rectangle([lb[0], lb[1], lb[2] - 1, lb[3] - 1], outline=AMBER)
    tri(d, lb[0] + 22, (lb[1] + lb[3]) // 2, 7, AMBER)
    txt(img, lb[0] + 40, lb[1] + 11, "LAUNCH", AMBER, scale=2, shadow=(40, 30, 0))
    txt(img, lb[0] + 118, lb[1] + 14, "PRIMARY", LABEL, scale=1)
    # controls
    glass(img, (32, 266, 824, 456))
    header(img, 44, 812, 276, "CONTROLS")
    txt(img, 44, 296, "SLIDER", LABEL, scale=1)
    slider(img, 120, 296, 260, 0.65)
    txt(img, 44, 320, "TOGGLE", LABEL, scale=1)
    toggle(img, 120, 316, ["OFF", "ON"], 1)
    txt(img, 44, 346, "TABS", LABEL, scale=1)
    x = 120
    for t in ("VIDEO", "AUDIO", "CONTROLS"):
        cw = tw(t, 2) + 24
        chip(img, (x, 340, x + cw, 362), t, on=(t == "AUDIO"), scale=2)
        x += cw + 8
    txt(img, 44, 378, "LIST ROWS", LABEL, scale=1)
    for i in range(3):
        u7.row_band(img, (120, 374 + i * 24, 420, 394 + i * 24), i == 1)
        txt(img, 130, 378 + i * 24, ["SCATTER VULCAN", "HAMMER MORTAR", "HARPOON TORPEDO"][i],
            AMBER if i == 1 else (DIM if i == 2 else WHITE), scale=1)
        diamond(d, 360, 384 + i * 24, 3, GOLD if i < 2 else DIM)
    # confirmation dialog
    glass(img, (450, 296, 812, 446), alpha=210)
    header(img, 462, 800, 306, "CONFIRM")
    txt(img, 462, 324, "QUIT TO MAIN MENU?", WHITE, scale=2)
    txt(img, 462, 348, "PROGRESS SINCE THE LAST SAVE IS LOST.", LABEL, scale=1)
    chip(img, (462, 380, 600, 404), "YES, QUIT", scale=2)
    chip(img, (612, 380, 760, 404), "NO, BACK", on=True, scale=2)
    brackets(d, (450, 296, 811, 445), HOLO, n=10)
    # ---- metal HUD section
    mx0, my0 = 856, 52
    raster.draw_text(img, mx0, my0 - 12, "METAL STYLE - IN-GAME HUD ONLY", raster.ACCENT)
    m = Image.new("RGBA", (528, 418), (0, 0, 0, 255))
    met = hud_r02.Metal()
    m.alpha_composite(u6.brushed(528, 418, 21, 0.55), (0, 0))
    met.plate(m, 14, 12, "PLATE")
    met.well(m, (14, 32, 250, 54))
    hud_r02.txt(m, 22, 36, "  1 245 680", hud_r02.LCD)
    met.plate(m, 14, 66, "ARMOUR")
    met.bar(m, 14, 84, 236, 0.7, hud_r02.ALERT)
    met.plate(m, 14, 106, "SHIELD")
    met.bar(m, 14, 124, 236, 0.3, hud_r02.CYAN)
    met.plate(m, 14, 146, "OVERDRIVE")
    met.bar(m, 14, 164, 236, 0.45, (255, 120, 255), segs=20)
    met.plate(m, 14, 186, "PIPS")
    met.well(m, (14, 204, 250, 222))
    met.pips(m, 22, 210, 3, 4)
    met.plate(m, 270, 12, "LCD COLOURS")
    for i, (n, c) in enumerate((("LCD", hud_r02.LCD), ("AMBER", hud_r02.AMBER),
                                ("CYAN", hud_r02.CYAN), ("ALERT", hud_r02.ALERT))):
        met.well(m, (270, 32 + i * 30, 300, 52 + i * 30), c)
        hud_r02.txt(m, 310, 36 + i * 30, n, c)
    met.plate(m, 270, 156, "SPECIAL ICONS")
    met.well(m, (270, 174, 510, 204))
    for i in range(3):
        m.alpha_composite(hud_r02.airstrike_icon(hud_r02.AMBER, i == 2), (278 + i * 26, 178))
    met.plate(m, 14, 240, "RADIO")
    met.well(m, (14, 258, 88, 332), (4, 8, 10))
    m.alpha_composite(portrait("rook")[1], (15, 259))
    hud_r02.txt(m, 98, 262, "LT.", hud_r02.AMBER)
    hud_r02.txt(m, 98, 280, "ROOK", hud_r02.AMBER)
    met.well(m, (14, 344, 250, 402))
    hud_r02.txt(m, 20, 350, "SIX O'CLOCK,", hud_r02.LCD)
    hud_r02.txt(m, 20, 372, "LANCER!", hud_r02.LCD)
    met.plate(m, 270, 240, "ESCORT")
    met.well(m, (270, 258, 510, 310))
    m.alpha_composite(u6.rook_sprite(), (274, 262))
    hud_r02.txt(m, 322, 264, "ROOK", hud_r02.LCD)
    met.bar(m, 322, 290, 182, 0.78, hud_r02.ALERT, segs=12, h=10)
    met.plate(m, 270, 326, "HAZARD STRIP")
    u6.hazard_strip(m, (270, 344, 510, 356))
    img.alpha_composite(m, (mx0, my0))
    raster.frame(img, (mx0 - 1, my0 - 1, mx0 + 528, my0 + 418))
    # ---- fonts
    fy = 500
    raster.draw_text(img, 16, fy, "BITMAP FONTS - PROPOSED PRODUCTION FONTS (DEJAVU SANS MONO BOLD "
                     "RASTERISED 1-BIT INTO FIXED CELLS); MOCKUPS STILL USE THE 5X7 PLACEHOLDER",
                     raster.ACCENT)
    y = fy + 18
    for name, cw, chh, size in FONT_SPECS:
        glyphs = bitmap_glyphs(cw, chh, size)
        raster.draw_text(img, 16, y, name, raster.LABEL)
        y += 12
        for row in CHARSET:
            draw_bitmap_text(img, 16, y, row, glyphs, (235, 240, 255))
            y += chh + 2
        sample = "Mission 10: Evacuation Corridor - CR 12 450 x2.5"
        draw_bitmap_text(img, 16, y, sample, glyphs, AMBER)
        y += chh + 12
    # zoomed sample + placeholder font
    zx = 860
    raster.draw_text(img, zx, fy + 18, "4X: BODY 10X20 CELLS", raster.LABEL)
    glyphs = bitmap_glyphs(10, 20, 15)
    z = Image.new("RGBA", (10 * 12, 20), (10, 12, 30, 255))
    draw_bitmap_text(z, 0, 0, "LANCER x3", glyphs, (235, 240, 255))
    z = sprite.enlarge(z, 4)
    img.alpha_composite(z, (zx, fy + 32))
    d = ImageDraw.Draw(img)
    for i in range(13):
        d.line([zx + i * 40, fy + 32, zx + i * 40, fy + 32 + 80], fill=(60, 70, 120, 255))
    py = fy + 130
    raster.draw_text(img, zx, py, "CURRENT 5X7 PLACEHOLDER FONT (MOCKUPS)", raster.LABEL)
    py += 14
    ph = "".join(sorted(raster._GLYPHS.keys()))
    for sc in (1, 2, 3):
        for chunk in (ph[:24], ph[24:]):
            if tw(chunk, sc) > 520:
                chunk = chunk[:520 // (6 * sc)]
            txt(img, zx, py, chunk, (235, 240, 255), scale=sc)
            py += 7 * sc + 6
    notes = ["THE PLACEHOLDER FONT HAS NO LOWER CASE AND NO", "SYMBOLS (DIAMOND, ARROWS, DOT); THE PRODUCTION",
             "FONTS ADD THEM, PLUS ACCENTED LATIN FOR NAMES.", "SIZES FROM UI/README: 8X12 LABELS, 10X20 BODY,",
             "20X30 HEADINGS. ONE COLOUR PER GLYPH, TINTED", "AT RUNTIME; SHADOW = SAME GLYPH OFFSET 1 PX."]
    py += 8
    for line in notes:
        raster.draw_text(img, zx, py, line, raster.LABEL_DIM)
        py += 12
    return img


# --------------------------------------------------------------------------- sheets + main

def sheet(screen, title, notes, crop_box):
    u6.ROUND = ROUND
    return u6.sheet(screen, title, notes, crop_box)


def options_sheet():
    screens = [options_video(), options_audio(), options_controls(), options_gameplay()]
    W, H = 16 + 2 * (SW + 16), 52 + 2 * (SH + 30) + 30
    img = raster.sheet(W, H, "OPTIONS (R08): VIDEO / AUDIO / CONTROLS / GAMEPLAY", ROUND)
    for i, (s, t) in enumerate(zip(screens, TABS)):
        x = 16 + (i % 2) * (SW + 16)
        y = 52 + (i // 2) * (SH + 30)
        raster.draw_text(img, x, y - 12, f"{t} TAB, 960X540 AT 1X", raster.LABEL_DIM)
        img.alpha_composite(s, (x, y))
        raster.frame(img, (x - 1, y - 1, x + SW, y + SH))
    raster.draw_text(img, 16, H - 20, "SAME SCREENS FROM THE MAIN MENU AND THE PAUSE MENU. GLASS "
                     "STYLE OVER THE DIMMED TITLE SCENE. CONTROLS SHOWS A REMAP IN PROGRESS AND THE "
                     "AUTO-FIRE TOGGLE (OFF = HOLD TO FIRE).", raster.LABEL)
    return img


ITEMS = {
    "briefing": ("briefing", "briefing-r08-a.png", briefing,
                 "BRIEFING (R08): MISSION 10 EVACUATION CORRIDOR",
                 ["GLASS PANELS OVER THE DIMMED MEGACITY. OKAFOR'S 144 PX STYLE-B PORTRAIT,",
                  "THE TACTICAL MAP OF THE CORRIDOR (SHUTTLE ROUTE, CONTACTS ON SIX), THE",
                  "TYPEWRITER TEXT IN PROGRESS WITH ITS CURSOR, A THREAT SUMMARY (LAYERS,",
                  "DENSITY, DIRECTION DIAL, RECOMMENDED TRAITS), OBJECTIVES AND THE HANGAR",
                  "TEASER. PAGE 2 OF 3."], (242, 290, 572, 440)),
    "act-title": ("briefing", "act-title-r08-a.png", act_title, "ACT TITLE CARD (R08): ACT II HOMEFRONT",
                  ["SHOWN BEFORE THE ACT-OPENING BRIEFING: LETTERBOXED PRE-RENDERED SCENE",
                   "(NOVA LAGOS FROM ABOVE), CHROME TITLE IN THE LOGO D TREATMENT, THE ACT'S",
                   "SETTINGS AND A ONE-LINE TAGLINE. HOLDS 3-4 S WITH THE ACT THEME STING."],
                  (250, 120, 580, 270)),
    "debrief": ("debrief", "debrief-r08-a.png", debrief, "DEBRIEF (R08): MISSION 10 COMPLETE",
                ["TALLY (KILLS, DAMAGE, SECRETS, CHAIN, SECONDARY OBJECTIVE), THE DATA CORE",
                 "FOUND, THE CREDITS BREAKDOWN (BALANCE + EARNED + BONUS + GRADE BONUS) AND",
                 "THE SCORE. GRADE A STAMP WITH NEW BEST. BUDGET FOR L10 IS ABOUT 1 840 CR."],
                (130, 100, 460, 250)),
    "failed": ("debrief", "mission-failed-r08-a.png", mission_failed, "MISSION FAILED (R08)",
               ["THE GAME FREEZES RED AND DIM BEHIND THE PANEL. OKAFOR'S LINE, RETRY",
                "(SELECTED), RETRY FROM BOSS (EASY/MEDIUM CHECKPOINT), BACK TO HANGAR, QUIT.",
                "SHOWS WHAT THE DISCARDED ATTEMPT EARNED."], (380, 160, 710, 310)),
    "gameover": ("debrief", "game-over-r08-a.png", game_over, "GAME OVER (R08): HARD, NO RETRIES LEFT",
                 ["CHROME 'GAME OVER' IN RED STEEL, THE CAMPAIGN SUMMARY AND THE HARD-MODE",
                  "TOP 10 WITH THE NEW ENTRY AT RANK 4 BEING TYPED (LETTER GRID BELOW). THEN",
                  "BACK TO THE MAIN MENU, WHERE THE LAST SAVE CAN BE LOADED."], (360, 160, 690, 310)),
    "pause": ("pause", "pause-r08-a.png", pause, "PAUSE (R08): OVER THE DIMMED GAME",
              ["THE IN-GAME FRAME (HUD R08) DIMMED AND SOFTENED; A GLASS PANEL WITH RESUME,",
               "RESTART MISSION, OPTIONS, ABORT TO HANGAR AND QUIT TO MAIN MENU."],
              (318, 120, 648, 270)),
    "credits": ("credits", "credits-r08-a.png", credits, "CREDITS (R08): SCROLLING, WITH CC-BY ATTRIBUTIONS",
                ["SCROLLING COLUMN OVER THE DIMMED TITLE SCENE. ROLES ARE PLACEHOLDERS; THE",
                 "SOUND-EFFECT ATTRIBUTIONS ARE READ FROM CREDITS.MD (CHOSEN CC-BY FILES), THE",
                 "CC0 AUTHORS ARE THANKED BELOW."], (300, 150, 630, 300)),
    "hud": ("hud", "hud-r08-a.png", hud_screen, "HUD A (R08): REFRESHED ELEMENT SET, METAL",
            ["L11 ATLANTIC CONVOY, KRAKEN MID-BOSS. NEW SINCE R02: STYLE-B RADIO PORTRAIT",
             "WITH SIGNAL BARS AND A MESSAGE QUEUE, OVERDRIVE TIMER BAR, ESCORT BOX WITH",
             "ROOK'S CRAFT AND ARMOUR, BOSS BAR WITH NAME AND WEAK POINT IN THE PLAY FIELD,",
             "EDGE WARNING AT THE LEFT EDGE, FLOATING CREDITS."], (0, 230, 330, 380)),
}


def main(args):
    keys = args or list(ITEMS) + ["options", "kit"]
    for k in keys:
        if k == "options":
            path = OUT["options"] / "options-r08-a.png"
            path.parent.mkdir(parents=True, exist_ok=True)
            options_sheet().convert("RGB").save(path, optimize=True)
        elif k == "kit":
            path = OUT["kit"] / "ui-kit-r08-a.png"
            path.parent.mkdir(parents=True, exist_ok=True)
            kit().convert("RGB").save(path, optimize=True)
        else:
            outk, name, fn, title, notes, crop = ITEMS[k]
            path = OUT[outk] / name
            path.parent.mkdir(parents=True, exist_ok=True)
            sheet(fn(), title, notes, crop).convert("RGB").save(path, optimize=True)
        print("wrote", path.relative_to(ROOT))


if __name__ == "__main__":
    main(sys.argv[1:])
