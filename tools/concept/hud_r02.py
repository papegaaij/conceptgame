#!/usr/bin/env python3
"""Concept round 02 - HUD A (metallic bevelled panels) at 960x540 in palette B.

Output: design/ui/hud/concept/hud-r02-a.png  - the full 960x540 screen at 1x.

Layout: left panel 0-239 (mission), play field 240-719 (parallax scene A of round 02),
right panel 720-959 (ship). Elements follow design/ui/hud. State: level 07 "Brood Carrier"
(rear gun and escort slot still empty).
Run: python3 tools/concept/hud_r02.py
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
import parallax_r02  # noqa: E402
from render import raster  # noqa: E402
from render.config import FIELD_X, PANEL_W, ROOT, SCREEN_H, SCREEN_W  # noqa: E402
from render.palette import B  # noqa: E402

OUT = ROOT / "design" / "ui" / "hud" / "concept"
SW, SH, P = SCREEN_W, SCREEN_H, PANEL_W

STATE = {
    # Level 07 "Brood Carrier" (Earth orbit): gear unlocked by L07 only (see player/weapons);
    # the rear gun (L08) and the escort slot (Rook, L08) are still empty.
    "mission": (7, "BROOD CARRIER"), "score": 1245680, "credits": 3280,
    "chain": (34, 2.5, 0.6), "progress": 0.62,
    "armor": (63, 90), "shield": (18, 40), "power": (10, 12, 10),
    "weapons": [("FRONT", "PULSE CANNON", 3, 4), ("REAR", "- EMPTY -", 0, 0),
                ("LEFT WING", "MICRO-MISSILE", 2, 3), ("RIGHT WING", "AUTOCANNON", 2, 3)],
    "overdrive": None,
    "special": ("AIRSTRIKE", 2, 3),
    "radio": ("CMDR OKAFOR", "BROOD CARRIER INBOUND ON YOUR VECTOR. HOLD THE LINE, LANCER!"),
    "escort": None,
}

METAL = B["UTC HULL"]          # violet-shadowed chrome of palette B
PANELS = B["UI PANELS"]
TEXT = B["UI TEXT"]
LCD_BG = PANELS[0]
LCD = TEXT[1]                  # 00ff66
AMBER = TEXT[2]                # ffff00
CYAN = TEXT[0]
ALERT = TEXT[4]                # ff4400
HAZARD = B["UTC ACCENTS"][5]


def wrap(text, width):
    words, lines, cur = text.split(), [], ""
    for w in words:
        if len(cur) + len(w) + (1 if cur else 0) > width:
            lines.append(cur)
            cur = w
        else:
            cur = f"{cur} {w}" if cur else w
    return lines + ([cur] if cur else [])


def txt(img, x, y, s, color, scale=2, shadow=None):
    return raster.draw_text(img, x, y, s, color, scale=scale, shadow=shadow)


def portrait(w=72, h=84, tint=LCD):
    """Radio portrait (officer with headset) drawn at high resolution, reduced, then given
    the video-feed treatment (tint + scanlines)."""
    cw, ch = 176, 208
    img = Image.new("RGBA", (cw, ch), (10, 16, 22, 255))
    d = ImageDraw.Draw(img)
    for y in range(ch):
        c = int(14 + 18 * y / ch)
        d.line([0, y, cw, y], fill=(c, c + 6, c + 12, 255))
    cx = cw // 2
    d.polygon([(cx - 90, ch), (cx - 70, 150), (cx + 70, 150), (cx + 90, ch)], fill=(48, 58, 74))
    d.polygon([(cx - 22, 150), (cx, 185), (cx + 22, 150)], fill=(30, 36, 48))
    d.rectangle([cx - 62, 168, cx - 40, 176], fill=(200, 160, 60))
    d.rectangle([cx - 16, 118, cx + 16, 156], fill=(120, 80, 62))
    d.ellipse([cx - 38, 40, cx + 38, 136], fill=(140, 96, 74))
    d.ellipse([cx + 4, 44, cx + 38, 136], fill=(112, 76, 58))
    d.ellipse([cx - 36, 42, cx + 30, 130], fill=(140, 96, 74))
    d.chord([cx - 40, 30, cx + 40, 96], 180, 360, fill=(26, 20, 18))
    d.rectangle([cx - 24, 82, cx - 10, 86], fill=(30, 22, 20))
    d.rectangle([cx + 8, 82, cx + 22, 86], fill=(30, 22, 20))
    d.rectangle([cx - 26, 74, cx - 8, 77], fill=(40, 30, 26))
    d.rectangle([cx + 6, 74, cx + 24, 77], fill=(40, 30, 26))
    d.line([cx - 12, 116, cx + 12, 116], fill=(90, 50, 44), width=4)
    d.arc([cx - 46, 34, cx + 46, 124], 190, 350, fill=(30, 32, 36), width=8)
    d.ellipse([cx - 52, 78, cx - 30, 108], fill=(40, 42, 48))
    d.line([cx - 40, 104, cx - 8, 124], fill=(40, 42, 48), width=5)
    d.ellipse([cx - 12, 118, cx - 2, 128], fill=(60, 62, 70))
    small = img.resize((w, h), Image.LANCZOS)
    arr = np.array(small).astype(np.float64)
    lum = arr[..., :3].mean(axis=-1, keepdims=True) / 255
    arr[..., :3] = arr[..., :3] * 0.3 + lum * np.array(tint) * 0.95
    arr[1::2, :, :3] *= 0.65
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")


def airstrike_icon(color, dim=False, s=2):
    img = Image.new("RGBA", (11 * s, 11 * s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c = tuple(int(v * (0.3 if dim else 1)) for v in color) + (255,)
    pts = [(5, 0), (7, 4), (10, 6), (10, 7), (6, 6), (6, 9), (8, 10), (2, 10), (4, 9),
           (4, 6), (0, 7), (0, 6), (3, 4)]
    d.polygon([(x * s, y * s) for x, y in pts], fill=c)
    return img


class Metal:
    """HUD A: brushed metal plates, bevels, rivets, hazard stripes, recessed LCD wells."""

    def panel(self, img, x0, x1):
        arr = np.array(img).astype(np.float64)
        n = raster.fbm(x1 - x0, SH, 24, 3 + x0, octaves=3, period=False)
        brushed = raster.value_noise(x1 - x0, SH, 2, 9 + x0, period=False)
        yy = np.linspace(0, 1, SH)[:, None]
        t = 0.55 + 0.25 * (1 - yy) + 0.12 * n + 0.05 * brushed
        col = raster.ramp([(0.0, METAL[1]), (0.5, METAL[2]), (1.0, METAL[3])], np.clip(t, 0, 1))
        arr[:, x0:x1, :3] = col * 0.82
        img.paste(Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA"))
        d = ImageDraw.Draw(img)
        d.line([x0, 0, x0, SH], fill=METAL[4])
        d.line([x0 + 1, 0, x0 + 1, SH], fill=METAL[3])
        d.line([x1 - 1, 0, x1 - 1, SH], fill=METAL[0])
        d.line([x1 - 2, 0, x1 - 2, SH], fill=METAL[1])
        for y in (8, SH - 9):
            for x in (x0 + 8, x1 - 9):
                d.ellipse([x - 3, y - 3, x + 3, y + 3], fill=METAL[1])
                d.point([(x - 1, y - 1), (x - 2, y - 1)], fill=METAL[4])

    def well(self, img, box, bg=LCD_BG):
        x0, y0, x1, y1 = box
        d = ImageDraw.Draw(img)
        d.rectangle(box, fill=bg)
        for i, (hi, lo) in enumerate(((METAL[0], METAL[4]), (METAL[1], METAL[3]))):
            d.line([x0 - 1 - i, y0 - 1 - i, x1 + 1 + i, y0 - 1 - i], fill=hi)
            d.line([x0 - 1 - i, y0 - 1 - i, x0 - 1 - i, y1 + 1 + i], fill=hi)
            d.line([x0 - 1 - i, y1 + 1 + i, x1 + 1 + i, y1 + 1 + i], fill=lo)
            d.line([x1 + 1 + i, y0 - 1 - i, x1 + 1 + i, y1 + 1 + i], fill=lo)

    def plate(self, img, x, y, text):
        d = ImageDraw.Draw(img)
        w = raster.text_width(text) + 10
        d.rectangle([x, y, x + w, y + 12], fill=METAL[1])
        d.line([x, y, x + w, y], fill=METAL[0])
        d.line([x, y + 12, x + w, y + 12], fill=METAL[3])
        txt(img, x + 5, y + 3, text, METAL[4], scale=1, shadow=METAL[0])

    def bar(self, img, x, y, w, value, color, segs=18, h=12):
        self.well(img, (x, y, x + w, y + h), (6, 6, 14))
        d = ImageDraw.Draw(img)
        sw = (w - 3) / segs
        on_n = round(value * segs)
        for i in range(segs):
            c = color if i < on_n else tuple(v // 6 for v in color)
            sx = x + 2 + i * sw
            d.rectangle([sx, y + 2, sx + sw - 2, y + h - 2], fill=c)
            if i < on_n:
                d.line([sx, y + 2, sx + sw - 2, y + 2], fill=tuple(min(255, v + 110) for v in color))

    def pips(self, img, x, y, lvl, mx, total=5, color=AMBER):
        d = ImageDraw.Draw(img)
        for i in range(total):
            if i >= mx:
                c = (24, 26, 40)
            else:
                c = color if i < lvl else (40, 44, 20)
            d.rectangle([x + i * 14, y, x + i * 14 + 10, y + 5], fill=c)

    def draw(self, img, st):
        self.panel(img, 0, P)
        self.panel(img, SW - P, SW)
        d = ImageDraw.Draw(img)
        # ---- left panel: mission
        for x in range(10, P - 10, 12):
            d.polygon([(x, 14), (x + 6, 14), (x, 26), (x - 6, 26)], fill=HAZARD)
        d.rectangle([10, 14, P - 11, 26], outline=METAL[0])
        d.rectangle([52, 12, 188, 28], fill=METAL[0])
        txt(img, 58, 14, "AF-12 CDF", HAZARD)
        num, name = st["mission"]
        self.plate(img, 14, 38, f"MISSION {num:02d}")
        txt(img, 14, 56, name, METAL[5], shadow=METAL[0])
        self.plate(img, 14, 80, "SCORE")
        self.well(img, (14, 96, P - 15, 118))
        txt(img, 22, 100, f"{st['score']:>11,}".replace(",", " "), LCD)
        self.plate(img, 14, 128, "CREDITS")
        self.well(img, (14, 144, P - 15, 166))
        txt(img, 22, 148, f"CR {st['credits']:>7,}".replace(",", " "), AMBER)
        chain, mult, window = st["chain"]
        self.plate(img, 14, 176, "CHAIN")
        self.well(img, (14, 192, P - 15, 222))
        txt(img, 22, 196, f"{chain:>3}", LCD)
        txt(img, P - 76, 196, f"X{mult:.1f}", AMBER)
        self.bar(img, 22, 214, P - 45, window, LCD, segs=16, h=5)
        # radio
        rname, msg = st["radio"]
        self.plate(img, 14, 234, "RADIO")
        self.well(img, (14, 250, 88, 336), (4, 8, 10))
        img.alpha_composite(portrait(), (15, 251))
        txt(img, 98, 254, rname.split()[0], AMBER)
        txt(img, 98, 272, " ".join(rname.split()[1:]), AMBER)
        self.well(img, (14, 348, P - 15, 440))
        for i, line in enumerate(wrap(msg, 17)[:5]):
            txt(img, 20, 354 + i * 17, line, LCD)
        # progress
        self.plate(img, 14, 452, "PROGRESS")
        self.well(img, (14, 468, P - 15, 482), (6, 6, 14))
        px = 16 + int((P - 33) * st["progress"])
        d.rectangle([16, 471, px, 479], fill=CYAN)
        d.polygon([(P - 24, 469), (P - 17, 475), (P - 24, 481)], fill=ALERT)
        txt(img, 14, 490, f"{int(st['progress'] * 100)}%", METAL[4], scale=1)
        txt(img, P - 44, 490, "BOSS", ALERT, scale=1)
        txt(img, 14, SH - 30, "SYS NOMINAL", METAL[1], scale=1)

        # ---- right panel: ship
        rx, rw = SW - P + 14, P - 29
        armour, amax = st["armor"]
        self.plate(img, rx, 14, "ARMOUR")
        txt(img, rx + rw - 36, 14, f"{armour:>3}", METAL[5], shadow=METAL[0])
        self.bar(img, rx, 32, rw, armour / amax, ALERT)
        shield, smax = st["shield"]
        self.plate(img, rx, 54, "SHIELD")
        txt(img, rx + rw - 36, 54, f"{shield:>3}", METAL[5], shadow=METAL[0])
        self.bar(img, rx, 72, rw, shield / smax, CYAN)
        load, out, regen = st["power"]
        self.plate(img, rx, 94, f"POWER {load}/{out} MW")
        self.well(img, (rx, 112, rx + rw, 128), (6, 6, 14))
        for i in range(out):
            c = AMBER if i < load else LCD
            sx = rx + 3 + i * ((rw - 4) / out)
            d.rectangle([sx, 115, sx + (rw - 4) / out - 3, 125], fill=c)
        txt(img, rx, 134, f"SPARE +{out - load} MW  REGEN +{regen}%", LCD, scale=1)
        self.plate(img, rx, 152, "WEAPONS")
        y = 170
        for slot, wname, lvl, mx in st["weapons"]:
            self.well(img, (rx, y, rx + rw, y + 40))
            empty = mx == 0
            dim = (60, 70, 90)
            txt(img, rx + 6, y + 4, slot, AMBER if not empty else dim, scale=1)
            txt(img, rx + 6, y + 14, wname, LCD if not empty else dim)
            self.pips(img, rx + rw - 72, y + 5, lvl, mx)
            y += 48
        self.plate(img, rx, y + 2, "OVERDRIVE")
        self.well(img, (rx, y + 20, rx + rw, y + 34), (6, 6, 14))
        txt(img, rx + rw - 110, y + 5, "NOT CHARGED" if not st["overdrive"] else "", METAL[1],
            scale=1)
        y += 46
        sname, count, mx = st["special"]
        self.plate(img, rx, y, "SPECIAL")
        self.well(img, (rx, y + 18, rx + rw, y + 48))
        txt(img, rx + 6, y + 26, sname, AMBER)
        for i in range(mx):
            img.alpha_composite(airstrike_icon(AMBER, i >= count),
                                (rx + rw - 28 - (mx - 1 - i) * 26, y + 22))
        y += 60
        self.plate(img, rx, y, "ESCORT")
        self.well(img, (rx, y + 18, rx + rw, y + 58))
        if st["escort"] is None:
            txt(img, rx + 6, y + 24, "NOT ASSIGNED", (70, 80, 110))
            txt(img, rx + 6, y + 44, "ESCORT SLOT OPENS L08", (70, 80, 110), scale=1)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    screen = Image.new("RGBA", (SW, SH), (0, 0, 0, 255))
    screen.alpha_composite(parallax_r02.get_scene("a").compose(24), (FIELD_X, 0))
    Metal().draw(screen, STATE)
    sheet = raster.sheet(SW + 32, SH + 70, "HUD A (R02): METALLIC BEVELLED PANELS, PALETTE B",
                         "CONCEPT ROUND 02 - 960X540")
    raster.draw_text(sheet, 16, 38, "960X540 NATIVE SCREEN AT 1X. PLAY FIELD 480X540, SIDE PANELS "
                     "240X540. LEVEL 07 STATE: REAR GUN AND ESCORT SLOT STILL EMPTY.",
                     raster.LABEL_DIM)
    sheet.alpha_composite(screen, (16, 52))
    path = OUT / "hud-r02-a.png"
    sheet.convert("RGB").save(path, optimize=True)
    print("wrote", path.relative_to(ROOT))


if __name__ == "__main__":
    main()
