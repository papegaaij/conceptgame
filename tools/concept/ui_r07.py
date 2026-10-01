#!/usr/bin/env python3
"""Concept round 07 - hangar/shop layout B in the glass menu style.

Outputs (full 960x540 screen at 1x on a sheet, plus a 2x detail crop):
  design/ui/hangar/concept/hangar-r07-a.png   layout B over a pre-rendered hangar bay: the
                                              real Stormhawk parked under a spotlight is the
                                              centre "schematic", with holo callouts
  design/ui/hangar/concept/hangar-r07-b.png   layout B over a darkened tactical map of Mars
                                              (route to Olympus Mons); centre is a holographic
                                              blueprint in a glass panel

Style: menu variant A (main-menu-r06-a): translucent glass panels with thin metal trim over a
pre-rendered scene, white/cyan text, amber selection bar and chips. The bevelled metal of
HUD A is reserved for the in-game HUD (user decision, round 06), so no metal wells, plates or
hazard strips here. Layout and content follow hangar-r06-b. State: before L15 *Olympus
Descent* (act 3), same as round 06.
Reuses the primitives and assets of ui_r06.py.
Run: python3 tools/concept/ui_r07.py [a] [b]
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, str(Path(__file__).resolve().parent))
import hud_r02  # noqa: E402
import ui_r06 as u6  # noqa: E402
from render import raster  # noqa: E402
from render.palette import B  # noqa: E402

OUT = u6.HANGAR_OUT
SW, SH = u6.SW, u6.SH
txt, tw, ctxt, glass, lit_bar, tri, diamond = u6.txt, u6.tw, u6.ctxt, u6.glass, u6.lit_bar, \
    u6.tri, u6.diamond
AMBER, CYAN, WHITE, DIM, GREEN, RED, ALERT, METAL = u6.AMBER, u6.CYAN, u6.WHITE, u6.DIM, \
    u6.GREEN, u6.RED, u6.ALERT, u6.METAL
LABEL = (150, 170, 205)      # field labels on glass
BODY = (200, 212, 240)       # body text on glass
GOLD = (255, 200, 60)
HOLO = (0, 200, 255)
SEL_FILL = (60, 50, 0)       # selected chip, as the MEDIUM chip of main menu A
CHIP_FILL = (12, 14, 36)

SHIP_C = (480, 214)          # ship centre in both layouts
SC = (304, 50, 656, 380)     # centre schematic area


# --------------------------------------------------------------------------- glass widgets

def header(img, x0, x1, y, text):
    """Section header: amber caps with a thin trim line, instead of a metal plate."""
    txt(img, x0, y, text, AMBER, scale=1, shadow=(30, 20, 0))
    d = ImageDraw.Draw(img)
    d.line([x0 + tw(text, 1) + 6, y + 3, x1, y + 3], fill=METAL[2])


def chip(img, box, text, on=False, scale=1, color=None):
    d = ImageDraw.Draw(img)
    d.rectangle(box, fill=SEL_FILL if on else CHIP_FILL, outline=AMBER if on else METAL[2])
    ctxt(img, (box[0] + box[2]) / 2, (box[1] + box[3]) / 2 - 3.5 * scale, text,
         color or (AMBER if on else BODY), scale=scale)


def row_band(img, box, on):
    """Shop row: a faint glass band; the selected row gets the menu's amber bar."""
    ov = Image.new("RGBA", (box[2] - box[0], box[3] - box[1]), (24, 30, 72, 110))
    img.alpha_composite(ov, (box[0], box[1]))
    if on:
        lit_bar(img, box)


def inset(img, box, fill=(4, 5, 16, 215)):
    """Dark recessed glass area (test fire, power bar) with a thin trim."""
    ov = Image.new("RGBA", (box[2] - box[0], box[3] - box[1]), fill)
    img.alpha_composite(ov, (box[0], box[1]))
    ImageDraw.Draw(img).rectangle([box[0], box[1], box[2] - 1, box[3] - 1], outline=METAL[2])


def brackets(d, box, color, n=14):
    x0, y0, x1, y1 = box
    for (x, y, sx, sy) in ((x0, y0, 1, 1), (x1, y0, -1, 1), (x0, y1, 1, -1), (x1, y1, -1, -1)):
        d.line([x, y, x + sx * n, y], fill=color)
        d.line([x, y, x, y + sy * n], fill=color)


# --------------------------------------------------------------------------- backdrops

def _shade(arr, k):
    arr[..., :3] *= k[..., None]
    return arr


def bay_backdrop():
    """Pre-rendered hangar bay seen from above: deck plating, a hazard-striped pad under a
    spotlight with the Stormhawk parked on it, overhead gantry trusses, cables, work lights."""
    yy, xx = np.mgrid[0:SH, 0:SW].astype(np.float64)
    grime = raster.fbm(SW, SH, 64, 701, octaves=5, period=False)
    base = np.array([26, 30, 48], float)
    arr = np.zeros((SH, SW, 4))
    arr[..., :3] = base * (0.75 + 0.5 * grime[..., None])
    arr[..., 3] = 255
    img = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")
    d = ImageDraw.Draw(img)
    # deck plates: 96 px tiles centred on the pad, seams dark with a lit top/left edge
    ox, oy = SHIP_C[0] % 96, SHIP_C[1] % 96
    for x in range(ox - 96, SW + 96, 96):
        d.line([x, 0, x, SH], fill=(12, 14, 24), width=2)
        d.line([x + 2, 0, x + 2, SH], fill=(42, 48, 74))
    for y in range(oy - 96, SH + 96, 96):
        d.line([0, y, SW, y], fill=(12, 14, 24), width=2)
        d.line([0, y + 2, SW, y + 2], fill=(42, 48, 74))
    for x in range(ox - 96, SW + 96, 96):
        for y in range(oy - 96, SH + 96, 96):
            for bx, by in ((8, 8), (88, 8), (8, 88), (88, 88)):
                d.ellipse([x + bx - 1, y + by - 1, x + bx + 1, y + by + 1], fill=(56, 62, 92))
    # landing pad: hazard frame and painted ring with the bay number
    cx, cy = SHIP_C
    pad = (cx - 150, cy - 150, cx + 150, cy + 150)
    stripe = Image.new("RGBA", (SW, SH), (0, 0, 0, 0))
    sd = ImageDraw.Draw(stripe)
    for x in range(pad[0] - 300, pad[2] + 300, 16):
        sd.polygon([(x, pad[3]), (x + 8, pad[3]), (x + 8 + 300, pad[1]), (x + 300, pad[1])],
                   fill=(124, 98, 16, 255))
    mask = Image.new("L", (SW, SH), 0)
    md = ImageDraw.Draw(mask)
    md.rectangle(pad, fill=255)
    md.rectangle((pad[0] + 10, pad[1] + 10, pad[2] - 10, pad[3] - 10), fill=0)
    img.paste(Image.new("RGBA", (SW, SH), (14, 14, 18, 255)), (0, 0), mask)
    img.paste(stripe, (0, 0), Image.fromarray(
        (np.array(mask) * (np.array(stripe)[..., 3] > 0)).astype(np.uint8)))
    d = ImageDraw.Draw(img)
    d.ellipse([cx - 112, cy - 112, cx + 112, cy + 112], outline=(70, 74, 60), width=3)
    d.ellipse([cx - 96, cy - 96, cx + 96, cy + 96], outline=(48, 52, 44), width=1)
    raster.draw_text(img, cx - raster.text_width("7", 9) // 2, cy + 120 - 70, "7", (52, 56, 46),
                     scale=9)
    raster.draw_text(img, 24, SH - 70, "BAY 07", (40, 44, 62), scale=6)
    # floor lights along the pad corners and the taxi line
    for (lx, ly) in ((pad[0] - 14, pad[1] - 14), (pad[2] + 14, pad[1] - 14),
                     (pad[0] - 14, pad[3] + 14), (pad[2] + 14, pad[3] + 14)):
        img = raster.add_light(img, lx, ly, 10, (255, 170, 40), 0.9)
    for y in range(pad[3] + 40, SH, 34):
        img = raster.add_light(img, cx, y, 5, (0, 200, 255), 0.7)
    # fuel cable snaking in from the left to the wing root
    cab = Image.new("RGBA", (SW, SH), (0, 0, 0, 0))
    cd = ImageDraw.Draw(cab)
    pts = []
    for k in range(40):
        t = k / 39
        pts.append((40 + (cx - 70 - 40) * t, 330 - 120 * t + 40 * np.sin(np.pi * t * 1.6)))
    cd.line(pts, fill=(14, 16, 26, 255), width=7)
    cd.line(pts, fill=(60, 66, 96, 255), width=2)
    img.alpha_composite(cab)
    # lighting: spotlight on the pad, dark corners, warm work light top-left
    arr = np.array(img).astype(np.float64)
    dist = np.hypot(xx - cx, (yy - cy) * 1.1)
    k = 0.32 + 0.95 * np.exp(-(dist / 250) ** 2)
    arr = _shade(arr, k)
    img = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")
    img = raster.add_light(img, cx - 40, cy - 60, 200, (60, 90, 160), 0.25)
    img = raster.add_light(img, 60, 30, 120, (255, 160, 60), 0.18)
    img = raster.add_light(img, 900, 500, 110, (0, 160, 255), 0.15)
    # the parked Stormhawk with its contact shadow (light from the top-left)
    ship = u6.schematic_ship(190)
    a = np.array(ship)[..., 3].astype(np.float64)
    sh = Image.fromarray(np.dstack([np.zeros_like(a)] * 3 + [a * 0.7]).astype(np.uint8), "RGBA")
    sh = sh.filter(ImageFilter.GaussianBlur(5))
    img.alpha_composite(sh, (cx - ship.width // 2 + 10, cy - ship.height // 2 + 14))
    img.alpha_composite(ship, (cx - ship.width // 2, cy - ship.height // 2))
    # overhead gantry trusses (closer to the camera: larger, softer, casting shadows)
    for ty in (34, 452):
        tr = Image.new("RGBA", (SW, 40), (0, 0, 0, 0))
        tdd = ImageDraw.Draw(tr)
        tdd.rectangle([0, 4, SW, 9], fill=(46, 50, 72, 255))
        tdd.rectangle([0, 30, SW, 35], fill=(46, 50, 72, 255))
        for x in range(0, SW, 40):
            tdd.line([x, 9, x + 40, 30], fill=(36, 40, 60, 255), width=3)
            tdd.line([x + 40, 9, x, 30], fill=(30, 34, 52, 255), width=2)
        tdd.line([0, 4, SW, 4], fill=(90, 96, 130, 255))
        shadow = Image.fromarray((np.array(tr) * [0, 0, 0, 0.55]).astype(np.uint8), "RGBA")
        img.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(6)), (16, ty + 26))
        img.alpha_composite(tr.filter(ImageFilter.GaussianBlur(1.2)), (0, ty))
    # faint haze in the light cone
    haze = raster.fbm(SW, SH, 128, 707, octaves=3, period=False)
    arr = np.array(img).astype(np.float64)
    arr[..., :3] += (np.clip(haze - 0.45, 0, 1) * 40 * np.exp(-(dist / 320) ** 2))[..., None] * \
        np.array([0.6, 0.8, 1.0])
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")


def tactical_backdrop():
    """Darkened tactical display: Mars below the orbit line, range rings round the landing
    zone at Olympus Mons, the descent route, a faint cyan grid."""
    yy, xx = np.mgrid[0:SH, 0:SW].astype(np.float64)
    img = u6.nebula_bg(311, nebula=((40, 10, 20), (8, 20, 60)))
    arr = np.array(img).astype(np.float64)
    # Mars: a huge disk whose limb curves through the lower half
    mcx, mcy, R = 520.0, 1180.0, 860.0
    dist = np.hypot(xx - mcx, yy - mcy)
    n = raster.fbm(SW, SH, 96, 313, octaves=6, period=False)
    n2 = raster.fbm(SW, SH, 24, 317, octaves=3, period=False)
    ramp = B["MARS"]
    t = np.clip(0.15 + n * 0.75 + (n2 - 0.5) * 0.25, 0, 0.999) * (len(ramp) - 1)
    lo = np.floor(t).astype(int)
    f = (t - lo)[..., None]
    rmp = np.array(ramp, float)
    surf = rmp[lo] * (1 - f) + rmp[np.minimum(lo + 1, len(ramp) - 1)] * f
    depth = np.clip((R - dist) / 260, 0, 1)
    surf *= (0.3 + 0.55 * depth ** 0.5)[..., None] * (0.55 + 0.45 * np.clip(1.2 - xx / SW, 0, 1))[
        ..., None]
    inside = (dist < R)[..., None]
    arr[..., :3] = np.where(inside, surf, arr[..., :3])
    rim = np.exp(-((dist - R) / 8) ** 2) * 0.9 + np.exp(-np.clip(dist - R, 0, None) / 22) * \
        (dist >= R) * 0.4
    arr[..., :3] += rim[..., None] * np.array([255, 120, 60]) * 0.5
    # darken everything: it is a backdrop for the panels
    arr[..., :3] *= 0.8
    img = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")
    ov = Image.new("RGBA", (SW, SH), (0, 0, 0, 0))
    od = ImageDraw.Draw(ov)
    for x in range(0, SW, 48):
        od.line([x, 0, x, SH], fill=(0, 160, 255, 22))
    for y in range(0, SH, 48):
        od.line([0, y, SW, y], fill=(0, 160, 255, 22))
    lz = (488, 398)
    for r in (30, 60, 100, 150):
        od.ellipse([lz[0] - r, lz[1] - r * 0.55, lz[0] + r, lz[1] + r * 0.55],
                   outline=(255, 120, 40, 110 - r // 3))
    # descent route: dashed arc from orbit (top-left) to the landing zone
    pts = []
    for k in range(60):
        tt = k / 59
        pts.append((90 + (lz[0] - 90) * tt, 70 + (lz[1] - 70) * tt ** 1.7))
    for k in range(0, 59, 3):
        od.line([pts[k], pts[k + 1]], fill=(0, 220, 255, 170), width=2)
    od.polygon([(lz[0] - 7, lz[1]), (lz[0], lz[1] - 9), (lz[0] + 7, lz[1]), (lz[0], lz[1] + 9)],
               outline=(255, 140, 40, 230))
    img.alpha_composite(ov)
    raster.draw_text(img, lz[0] + 16, lz[1] - 4, "LZ OLYMPUS MONS", (255, 150, 60), scale=1)
    # vignette
    arr = np.array(img).astype(np.float64)
    vig = np.clip(1.2 - 0.55 * np.hypot((xx - SW / 2) / SW, (yy - SH / 2) / SH) * 2, 0.55, 1)
    arr[..., :3] *= vig[..., None]
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")


# --------------------------------------------------------------------------- panels

def top_bar(img):
    txt(img, 16, 10, "HANGAR 7", WHITE, scale=3, shadow=(0, 0, 30))
    txt(img, 166, 12, "BEFORE L15 - OLYMPUS DESCENT", CYAN, scale=1, shadow=(0, 0, 30))
    txt(img, 166, 24, "ACT 3 - RED DUST - MEDIUM", LABEL, scale=1, shadow=(0, 0, 30))
    for i, t in enumerate(("REPAIR", "SAVE")):
        x = 500 + i * 70
        chip(img, (x, 12, x + 62, 32), t)
    glass(img, (650, 8, 794, 36), alpha=190)
    txt(img, 660, 15, "CR 12 450", AMBER, scale=2)
    lb = (806, 6, 950, 38)
    img2 = raster.add_light(img, (lb[0] + lb[2]) / 2, (lb[1] + lb[3]) / 2, 60, (255, 170, 0),
                            0.28)
    img.paste(img2)
    glass(img, lb, alpha=200)
    lit_bar(img, (lb[0] + 1, lb[1] + 1, lb[2] - 1, lb[3] - 1))
    d = ImageDraw.Draw(img)
    d.rectangle([lb[0], lb[1], lb[2] - 1, lb[3] - 1], outline=AMBER)
    tri(d, lb[0] + 22, (lb[1] + lb[3]) // 2, 7, AMBER)
    txt(img, lb[0] + 38, lb[1] + 9, "LAUNCH", AMBER, scale=2, shadow=(40, 30, 0))


def shop_panel(img):
    x0, x1 = 8, 296
    glass(img, (x0, 46, x1, 508), alpha=170)
    header(img, x0 + 10, x1 - 10, 56, "SHOP - FRONT GUNS")
    d = ImageDraw.Draw(img)
    lx0, lx1, rh = x0 + 10, x1 - 10, 24
    for i, (name, status, price, dia, tag) in enumerate(u6.SHOP):
        y = 70 + i * (rh + 4)
        on = i == u6.SEL_SHOP
        locked = status.startswith("LOCKED")
        row_band(img, (lx0, y, lx1, y + rh), on)
        if on:
            tri(d, lx0 + 7, y + rh // 2, 4, AMBER)
        col = AMBER if on else (DIM if locked else WHITE)
        txt(img, lx0 + 16, y + 8, name, col, scale=1)
        right = status or ("CR " + price)
        rc = DIM if locked else (CYAN if status else (AMBER if on else BODY))
        txt(img, lx1 - 8 - tw(right, 1), y + 8, right, rc, scale=1)
        for k in range(dia):
            diamond(d, lx0 + 126 + k * 10, y + rh // 2, 4, GOLD)
        if tag:
            tx = lx0 + 136
            d.rectangle([tx, y + 6, tx + 26, y + rh - 7], fill=(160, 0, 110))
            txt(img, tx + 3, y + 9, tag, WHITE, scale=1)
    # selected item
    y0 = 284
    header(img, x0 + 10, x1 - 10, y0, "SELECTED")
    y0 += 16
    txt(img, lx0, y0, "HAMMER MORTAR  L1", AMBER, scale=2, shadow=(40, 30, 0))
    cx = lx0
    for t in ("ANTI-GROUND", "AREA"):
        w = tw(t, 1) + 16
        d.rectangle([cx, y0 + 20, cx + w, y0 + 33], fill=(40, 30, 0), outline=GOLD)
        diamond(d, cx + 6, y0 + 26, 3, GOLD)
        txt(img, cx + 12, y0 + 23, t, (255, 220, 120), scale=1)
        cx += w + 6
    txt(img, lx0, y0 + 42, "DPS 25 (GROUND ONLY)   DRAW 3 MW", WHITE, scale=1)
    txt(img, lx0, y0 + 56, "VS FITTED:", LABEL, scale=1)
    txt(img, lx0 + 66, y0 + 56, "DPS -18 AIR", RED, scale=1)
    txt(img, lx0 + 144, y0 + 56, "+25 GROUND", GREEN, scale=1)
    txt(img, lx0 + 66, y0 + 68, "DRAW -1 MW", GREEN, scale=1)
    by = y0 + 84
    chip(img, (lx0, by, lx0 + 128, by + 22), "BUY  CR 1 500", on=True)
    chip(img, (lx0 + 136, by, lx1, by + 22), "TEST FIRE")
    # test-fire preview
    tb = (lx0, 418, lx1, 498)
    inset(img, tb)
    prev = Image.new("RGBA", (tb[2] - tb[0] - 2, tb[3] - tb[1] - 2), (40, 18, 8, 255))
    pd = ImageDraw.Draw(prev)
    rng = np.random.default_rng(4)
    for _ in range(60):
        pd.point((int(rng.integers(0, prev.width)), int(rng.integers(0, prev.height))),
                 fill=(90, 40, 16))
    for gx in (60, 150, 222):
        pd.rectangle([gx - 6, 30, gx + 6, 42], fill=(80, 60, 50), outline=(140, 110, 90))
    pd.ellipse([140, 26, 160, 46], outline=(255, 160, 40))
    pd.ellipse([144, 30, 156, 42], fill=(255, 210, 90))
    for k in range(9):
        t = k / 8
        x, y = 30 + 120 * t, 58 - 32 * np.sin(np.pi * t)
        pd.ellipse([x - 1, y - 1, x + 1, y + 1], fill=(255, 220, 140))
    sp = u6.player_sprite().resize((24, 24), Image.LANCZOS)
    prev.alpha_composite(sp, (18, prev.height - 28))
    img.alpha_composite(prev, (tb[0] + 1, tb[1] + 1))
    txt(img, tb[0] + 6, tb[1] + 6, "TEST FIRE LOOP", (230, 200, 160), scale=1)


CALLOUTS = [  # anchor offset from ship centre, box centre, (label, value, lvl, max, on, width)
    ((0, -80), (480, 92), ("FRONT", "SCATTER VULCAN", 3, 5, True, 140)),
    ((-58, 16), (364, 172), ("L WING", "MICRO-MSL", 2, 3, False, 104)),
    ((58, 16), (596, 172), ("R WING", "MICRO-MSL", 2, 3, False, 104)),
    ((0, 78), (480, 340), ("REAR", "TAIL GUN", 1, 3, False, 112)),
]


def slot_callout(img, cx, cy, label, value, lvl, mx, on, w, h=34):
    x0, y0 = int(cx - w / 2), int(cy - h / 2)
    box = (x0, y0, x0 + w, y0 + h)
    glass(img, box, alpha=205, trim=False)
    d = ImageDraw.Draw(img)
    if on:
        lit_bar(img, box)
    d.rectangle([box[0], box[1], box[2] - 1, box[3] - 1], outline=AMBER if on else (0, 150, 210))
    txt(img, x0 + 6, y0 + 5, label, AMBER if on else CYAN, scale=1)
    txt(img, x0 + 6, y0 + 18, value, AMBER if on else WHITE, scale=1)
    if mx:
        u6.pips(img, x0 + w - 6 - mx * 9, y0 + 6, lvl, mx)


def centre(img, live):
    """Ship schematic with slot callouts. live=True: the backdrop's parked ship shows
    through with holo brackets and a faint grid; live=False: a blueprint in a glass panel."""
    cx, cy = SHIP_C
    d = ImageDraw.Draw(img)
    if live:
        ov = Image.new("RGBA", (SC[2] - SC[0], SC[3] - SC[1]), (0, 0, 0, 0))
        od = ImageDraw.Draw(ov)
        for gx in range(0, ov.width, 24):
            od.line([gx, 0, gx, ov.height], fill=(0, 180, 255, 18))
        for gy in range(0, ov.height, 24):
            od.line([0, gy, ov.width, gy], fill=(0, 180, 255, 18))
        img.alpha_composite(ov, (SC[0], SC[1]))
        d = ImageDraw.Draw(img)
        brackets(d, SC, HOLO, 18)
        txt(img, SC[0] + 8, SC[1] + 6, "AF-12 STORMHAWK - BAY 7 LIVE", HOLO, scale=1,
            shadow=(0, 0, 20))
    else:
        glass(img, SC, alpha=150)
        d = ImageDraw.Draw(img)
        for gx in range(SC[0] + 1, SC[2] - 1, 12):
            d.line([gx, SC[1] + 1, gx, SC[3] - 2], fill=(6, 28, 66))
        for gy in range(SC[1] + 1, SC[3] - 1, 12):
            d.line([SC[0] + 1, gy, SC[2] - 2, gy], fill=(6, 28, 66))
        for gx in range(SC[0] + 1, SC[2] - 1, 60):
            d.line([gx, SC[1] + 1, gx, SC[3] - 2], fill=(10, 50, 110))
        for gy in range(SC[1] + 1, SC[3] - 1, 60):
            d.line([SC[0] + 1, gy, SC[2] - 2, gy], fill=(10, 50, 110))
        ship = u6.schematic_ship(190)
        img.alpha_composite(ship, (cx - ship.width // 2, cy - ship.height // 2))
        d = ImageDraw.Draw(img)
        txt(img, SC[0] + 8, SC[1] + 6, "AF-12 STORMHAWK - SCHEMATIC", HOLO, scale=1)
    for (ax, ay), b, (lbl, val, lv, mx, on, w) in CALLOUTS:
        a = (cx + ax, cy + ay)
        col = AMBER if on else HOLO
        d.line([a, b], fill=col)
        d.ellipse([a[0] - 3, a[1] - 3, a[0] + 3, a[1] + 3], outline=col)
        slot_callout(img, b[0], b[1], lbl, val, lv, mx, on, w)
        d = ImageDraw.Draw(img)
    # escort: Rook's craft
    rook = u6.rook_sprite()
    ex, ey = SC[0] + 8, SC[3] - 52
    glass(img, (ex, ey, ex + 98, ey + 46), alpha=190, trim=False)
    img.alpha_composite(rook, (ex + 3, ey + 3))
    txt(img, ex + 48, ey + 6, "ESCORT", CYAN, scale=1)
    txt(img, ex + 48, ey + 19, "ROOK", WHITE, scale=1)
    txt(img, ex + 48, ey + 32, "EMBER", (240, 150, 70), scale=1)
    # core modules
    mods = [("GEN", "MK III", "14 MW"), ("SHD", "MK III", "45"), ("ARM", "COMP II", "74/100"),
            ("ENG", "MK II", "290"), ("SPC", None, "X2"), ("UTL", "SENSOR", "L3"),
            ("UTL", "MAGNET", "L1")]
    mw = (SC[2] - SC[0] - 6 * 6) / 7
    for i, (k, v, nval) in enumerate(mods):
        x0 = int(SC[0] + i * (mw + 6))
        glass(img, (x0, 392, int(x0 + mw), 448), alpha=185)
        txt(img, x0 + 5, 397, k, AMBER, scale=2)
        if v is None:
            for j in range(2):
                img.alpha_composite(hud_r02.airstrike_icon(AMBER, s=1), (x0 + 5 + j * 14, 417))
        else:
            txt(img, x0 + 5, 418, v, WHITE, scale=1)
        txt(img, x0 + 5, 432, nval, CYAN, scale=1)
    # power bar
    glass(img, (SC[0], 456, SC[2], 504), alpha=185)
    txt(img, SC[0] + 8, 462, "POWER", AMBER, scale=1)
    txt(img, SC[0] + 52, 462, "12/14 MW   WITH MORTAR 11/14   +15% SHIELD REGEN", BODY,
        scale=1)
    pw = (SC[0] + 8, 476, SC[2] - 8, 496)
    inset(img, pw)
    d = ImageDraw.Draw(img)
    out, load, proj = 14, 12, 11
    segw = (pw[2] - pw[0] - 4) / out
    for i in range(out):
        sx = pw[0] + 3 + i * segw
        c = AMBER if i < proj else ((130, 100, 30) if i < load else (24, 30, 54))
        d.rectangle([sx, pw[1] + 3, sx + segw - 3, pw[3] - 4], fill=c)


def intel(img):
    x0, x1 = 664, 952
    glass(img, (x0, 46, x1, 508), alpha=170)
    header(img, x0 + 10, x1 - 10, 56, "INTEL - L15")
    brief, _ = u6.varga_portrait()
    px, py = x0 + 10, 70
    d = ImageDraw.Draw(img)
    img.alpha_composite(brief, (px, py))
    d.rectangle([px - 1, py - 1, px + 144, py + 144], outline=METAL[3])
    tx = px + 154
    txt(img, tx, py + 2, "DR. VARGA", AMBER, scale=2, shadow=(40, 30, 0))
    txt(img, tx, py + 20, "CDF INTEL", CYAN, scale=1)
    for i, line in enumerate(hud_r02.wrap(u6.QUOTE, (x1 - 10 - tx) // 6)[:8]):
        txt(img, tx, py + 38 + i * 12, line, BODY, scale=1)
    y = py + 154
    txt(img, px, y, "OLYMPUS DESCENT", WHITE, scale=2)
    chip(img, (x1 - 74, y - 1, x1 - 10, y + 13), "SENSOR L3", color=CYAN)
    y += 22
    for k, v in u6.INTEL:
        txt(img, px, y, k, LABEL, scale=1)
        txt(img, px + 70, y, v, CYAN, scale=1)
        y += 14
    txt(img, px, y + 2, "DENSITY", LABEL, scale=1)
    d = ImageDraw.Draw(img)
    for i in range(5):
        d.rectangle([px + 70 + i * 14, y + 1, px + 80 + i * 14, y + 9],
                    fill=AMBER if i < 3 else (40, 40, 60))
    txt(img, px + 144, y + 2, "3/5", AMBER, scale=1)
    y += 22
    txt(img, px, y, "FROM", LABEL, scale=1)
    u6.direction_dial(img, (x0 + x1) // 2, y + 46, {"FRONT": 100}, r=26)
    y += 100
    txt(img, px, y, "RECOMMEND", LABEL, scale=1)
    d = ImageDraw.Draw(img)
    for i, t in enumerate(("FORWARD", "ANTI-GROUND")):
        diamond(d, px + 8, y + 20 + i * 18, 4, GOLD)
        txt(img, px + 18, y + 14 + i * 18, t, (255, 220, 120), scale=2)


def footer(img):
    ctxt(img, SW / 2, SH - 20, "ARROWS SELECT    ENTER BUY    T TEST FIRE    L LAUNCH    ESC BACK",
         DIM, scale=1)


def hangar(live):
    img = bay_backdrop() if live else tactical_backdrop()
    top_bar(img)
    shop_panel(img)
    centre(img, live)
    intel(img)
    footer(img)
    return img


ITEMS = {
    "a": ("hangar-r07-a.png", lambda: hangar(True),
          "HANGAR (R07) A: LAYOUT B IN GLASS STYLE - HANGAR BAY",
          ["LAYOUT B IN THE GLASS STYLE OF MAIN MENU A. THE BACKDROP IS A PRE-RENDERED HANGAR",
           "BAY SEEN FROM ABOVE: THE STORMHAWK PARKED ON PAD 7 UNDER A SPOTLIGHT, OVERHEAD",
           "GANTRIES, A FUEL CABLE. THE CENTRE 'SCHEMATIC' IS THE LIVE SHIP WITH HOLO",
           "BRACKETS AND CALLOUTS; SHOP AND INTEL ARE TRANSLUCENT GLASS PANELS. BEFORE L15."],
          (300, 46, 630, 196)),
    "b": ("hangar-r07-b.png", lambda: hangar(False),
          "HANGAR (R07) B: LAYOUT B IN GLASS STYLE - TACTICAL MAP",
          ["SAME LAYOUT AND CONTENT OVER A DARKENED TACTICAL MAP OF MARS: RANGE RINGS ROUND",
           "THE LANDING ZONE AT OLYMPUS MONS AND THE DESCENT ROUTE FROM ORBIT. THE CENTRE",
           "IS A HOLOGRAPHIC BLUEPRINT IN A GLASS PANEL. BEFORE L15."],
          (300, 46, 630, 196)),
}


def main(args):
    OUT.mkdir(parents=True, exist_ok=True)
    u6.ROUND = "CONCEPT ROUND 07 - 960X540"
    for key in args or list(ITEMS):
        name, fn, title, notes, crop = ITEMS[key]
        path = OUT / name
        u6.sheet(fn(), title, notes, crop).convert("RGB").save(path, optimize=True)
        print("wrote", path.relative_to(u6.ROOT))


if __name__ == "__main__":
    main(sys.argv[1:])
