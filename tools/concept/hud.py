#!/usr/bin/env python3
"""Concept round 01 - HUD layout, two variants of the 16:9 screen with side panels.

Outputs (design/ui/hud/concept/):
  hud-r01-a.png   variant A: classic metallic bevelled 90s panels with LCD readouts
  hud-r01-b.png   variant B: sleek dark-glass cockpit panels with neon outlines

Screen 640x360 native (shown at 2x): left panel 0-159, play field 160-479, right panel
480-639. The play field reuses parallax scene A (tools/concept/parallax.py).
Run: python3 tools/concept/hud.py
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, str(Path(__file__).resolve().parent))
import parallax  # noqa: E402
from render import raster, sprite  # noqa: E402
from render.config import out_path  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "design" / "ui" / "hud" / "concept"
SW, SH = 640, 360
PANEL = 160

STATE = {
    # Level 07 "Brood Carrier" (Earth orbit): only gear unlocked by L07 (see player/weapons);
    # the rear gun (L08) and the escort slot (Rook, L08) are still empty.
    "score": 1245680, "credits": 3280, "act": 1, "level": 7, "progress": 0.62,
    "armor": 0.7, "shield": 0.45, "gen_load": 10, "gen_max": 12,
    "weapons": [("FRONT", "PULSE CANNON", 3, 4), ("REAR", "- EMPTY -", 0, 0),
                ("L WING", "MICRO-MISSILE POD", 2, 3), ("R WING", "AUTOCANNON POD", 2, 3)],
    "special": ("AIRSTRIKE", 2, 3),
    "radio": ("CMDR OKAFOR", "BROOD CARRIER INBOUND ON YOUR VECTOR. HOLD THE LINE, LANCER!"),
    "wingman": ("NOT ASSIGNED", "ESCORT SLOT OPENS L08", 0),
}


# --------------------------------------------------------------------------- shared bits

def wrap(text, width):
    words, lines, cur = text.split(), [], ""
    for w in words:
        if len(cur) + len(w) + (1 if cur else 0) > width:
            lines.append(cur)
            cur = w
        else:
            cur = f"{cur} {w}" if cur else w
    return lines + ([cur] if cur else [])


def portrait(w=44, h=52, tint=(120, 255, 170)):
    """Procedural radio portrait: officer with headset, drawn at 4x and reduced."""
    s = 4
    img = Image.new("RGBA", (w * s, h * s), (10, 16, 22, 255))
    d = ImageDraw.Draw(img)
    for y in range(h * s):
        c = int(14 + 18 * y / (h * s))
        d.line([0, y, w * s, y], fill=(c, c + 6, c + 12, 255))
    cx = w * s // 2
    # shoulders / uniform
    d.polygon([(cx - 90, h * s), (cx - 70, 150), (cx + 70, 150), (cx + 90, h * s)],
              fill=(48, 58, 74, 255))
    d.polygon([(cx - 22, 150), (cx, 185), (cx + 22, 150)], fill=(30, 36, 48, 255))
    d.rectangle([cx - 62, 168, cx - 40, 176], fill=(200, 160, 60, 255))   # rank tab
    # neck and head
    d.rectangle([cx - 16, 118, cx + 16, 156], fill=(170, 120, 96, 255))
    d.ellipse([cx - 38, 40, cx + 38, 136], fill=(196, 146, 118, 255))
    d.ellipse([cx + 4, 44, cx + 38, 136], fill=(160, 112, 90, 255))       # shade right side
    d.ellipse([cx - 36, 42, cx + 30, 130], fill=(196, 146, 118, 255))
    d.chord([cx - 40, 30, cx + 40, 104], 180, 360, fill=(36, 28, 26, 255))  # hair
    d.rectangle([cx - 24, 82, cx - 10, 86], fill=(40, 30, 28, 255))        # eyes
    d.rectangle([cx + 8, 82, cx + 22, 86], fill=(40, 30, 28, 255))
    d.rectangle([cx - 26, 74, cx - 8, 77], fill=(60, 44, 36, 255))         # brows
    d.rectangle([cx + 6, 74, cx + 24, 77], fill=(60, 44, 36, 255))
    d.line([cx - 12, 116, cx + 12, 116], fill=(120, 70, 60, 255), width=4)  # mouth
    # headset
    d.arc([cx - 46, 34, cx + 46, 124], 190, 350, fill=(30, 32, 36, 255), width=8)
    d.ellipse([cx - 52, 78, cx - 30, 108], fill=(40, 42, 48, 255))
    d.line([cx - 40, 104, cx - 8, 124], fill=(40, 42, 48, 255), width=5)
    d.ellipse([cx - 12, 118, cx - 2, 128], fill=(60, 62, 70, 255))
    small = img.resize((w, h), Image.LANCZOS)
    # video-feed treatment: tint + scanlines
    arr = np.array(small).astype(np.float64)
    lum = arr[..., :3].mean(axis=-1, keepdims=True) / 255
    arr[..., :3] = arr[..., :3] * 0.35 + lum * np.array(tint) * 0.9
    arr[1::2, :, :3] *= 0.7
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")


def airstrike_icon(color, dim=False):
    img = Image.new("RGBA", (11, 11), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c = tuple(int(v * (0.35 if dim else 1)) for v in color) + (255,)
    d.polygon([(5, 0), (7, 4), (10, 6), (10, 7), (6, 6), (6, 9), (8, 10), (2, 10), (4, 9),
               (4, 6), (0, 7), (0, 6), (3, 4)], fill=c)
    return img


def play_field():
    scene = parallax.get_scene("a")
    return scene.compose(30)


# --------------------------------------------------------------------------- variant A

class Metal:
    """Classic late-90s metallic bevelled panels with recessed LCD readouts."""
    LCD_BG = (12, 30, 18)
    LCD = (124, 255, 154)
    AMBER = (255, 210, 74)
    RED = (255, 70, 50)

    def panel(self, img, x0, x1):
        arr = np.array(img).astype(np.float64)
        n = raster.fbm(x1 - x0, SH, 16, 3 + x0, octaves=3, period=False)
        yy = np.linspace(0, 1, SH)[:, None]
        base = 78 + 30 * (1 - yy) + 14 * n
        brushed = raster.value_noise(x1 - x0, SH, 2, 9, period=False) * 8
        for ch, k in enumerate((1.0, 1.03, 1.1)):
            arr[:, x0:x1, ch] = (base + brushed) * k
        img.paste(Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA"))
        d = ImageDraw.Draw(img)
        d.line([x0, 0, x0, SH], fill=(170, 176, 190))
        d.line([x1 - 1, 0, x1 - 1, SH], fill=(30, 32, 38))
        for y in (6, SH - 7):
            for x in (x0 + 6, x1 - 7):
                d.ellipse([x - 2, y - 2, x + 2, y + 2], fill=(60, 62, 70))
                d.point([(x - 1, y - 1)], fill=(200, 205, 215))

    def well(self, img, box, bg=None):
        """Recessed window: dark outer bevel top-left, light bottom-right."""
        x0, y0, x1, y1 = box
        d = ImageDraw.Draw(img)
        d.rectangle(box, fill=bg or self.LCD_BG)
        d.line([x0 - 1, y0 - 1, x1 + 1, y0 - 1], fill=(36, 38, 44))
        d.line([x0 - 1, y0 - 1, x0 - 1, y1 + 1], fill=(36, 38, 44))
        d.line([x0 - 1, y1 + 1, x1 + 1, y1 + 1], fill=(190, 196, 210))
        d.line([x1 + 1, y0 - 1, x1 + 1, y1 + 1], fill=(190, 196, 210))

    def plate(self, img, x, y, text):
        d = ImageDraw.Draw(img)
        w = raster.text_width(text) + 8
        d.rectangle([x, y, x + w, y + 10], fill=(52, 54, 62))
        d.line([x, y + 10, x + w, y + 10], fill=(150, 156, 170))
        raster.draw_text(img, x + 4, y + 2, text, (210, 214, 224), shadow=(20, 20, 24))

    def bar(self, img, x, y, w, value, color, segs=14):
        self.well(img, (x, y, x + w, y + 8), (10, 12, 14))
        d = ImageDraw.Draw(img)
        sw = (w - 2) / segs
        for i in range(segs):
            on = i < round(value * segs)
            c = color if on else tuple(v // 5 for v in color)
            sx = x + 2 + i * sw
            d.rectangle([sx, y + 2, sx + sw - 2, y + 6], fill=c)
            if on:
                d.line([sx, y + 2, sx + sw - 2, y + 2], fill=tuple(min(255, v + 90) for v in color))

    def draw(self, img, st):
        self.panel(img, 0, PANEL)
        self.panel(img, SW - PANEL, SW)
        d = ImageDraw.Draw(img)
        # hazard stripe header
        for x in range(8, PANEL - 8, 8):
            d.polygon([(x, 12), (x + 4, 12), (x, 20), (x - 4, 20)], fill=(230, 180, 40))
        d.rectangle([8, 12, PANEL - 9, 20], outline=(40, 40, 40))
        d.rectangle([40, 11, 118, 21], fill=(40, 40, 44))
        raster.draw_text(img, 50, 13, "AF-12  CDF", (230, 180, 40))
        # score / credits
        self.plate(img, 10, 30, "SCORE")
        self.well(img, (10, 43, PANEL - 11, 57))
        raster.draw_text(img, 16, 47, f"{st['score']:09d}", self.LCD, scale=1)
        self.plate(img, 10, 64, "CREDITS")
        self.well(img, (10, 77, PANEL - 11, 91))
        raster.draw_text(img, 16, 81, f"CR {st['credits']:>7,}".replace(",", "."), self.AMBER)
        # level progress
        self.plate(img, 10, 100, f"ACT {st['act']}  LEVEL {st['level']:02d}")
        self.well(img, (10, 114, PANEL - 11, 124), (10, 12, 14))
        px = 12 + int((PANEL - 25) * st["progress"])
        d.rectangle([12, 117, px, 121], fill=(90, 160, 255))
        d.polygon([(PANEL - 16, 116), (PANEL - 12, 119), (PANEL - 16, 122)], fill=self.RED)
        raster.draw_text(img, 12, 128, f"{int(st['progress'] * 100)}%", (40, 40, 48))
        raster.draw_text(img, PANEL - 35, 128, "BOSS", (40, 40, 48))
        # radio
        name, msg = st["radio"]
        self.plate(img, 10, 146, "RADIO")
        self.well(img, (10, 160, 58, 216), (4, 10, 6))
        img.alpha_composite(portrait(tint=(120, 255, 160)), (12, 162))
        raster.draw_text(img, 64, 162, name.split()[0], self.AMBER)
        raster.draw_text(img, 64, 172, " ".join(name.split()[1:]), self.AMBER)
        self.well(img, (10, 224, PANEL - 11, 290))
        for i, line in enumerate(wrap(msg, 22)):
            raster.draw_text(img, 14, 228 + i * 10, line, self.LCD)
        self.plate(img, 10, 298, "WINGMAN")
        self.well(img, (10, 312, PANEL - 11, 340))
        raster.draw_text(img, 14, 316, st["wingman"][0], self.LCD)
        raster.draw_text(img, 14, 328, st["wingman"][1], (80, 150, 100))
        for i in range(6 if st["wingman"][2] else 0):
            c = self.AMBER if i < st["wingman"][2] else (40, 50, 30)
            d.rectangle([PANEL - 64 + i * 8, 317, PANEL - 59 + i * 8, 321], fill=c)

        rx = SW - PANEL + 10
        rw = PANEL - 21
        self.plate(img, rx, 12, "ARMOR")
        self.bar(img, rx, 25, rw, st["armor"], (230, 70, 50))
        self.plate(img, rx, 40, "SHIELD")
        self.bar(img, rx, 53, rw, st["shield"], (80, 200, 255))
        self.plate(img, rx, 68, f"GENERATOR {st['gen_load']}/{st['gen_max']} MW")
        self.bar(img, rx, 81, rw, st["gen_load"] / st["gen_max"], (255, 200, 60), segs=12)
        self.plate(img, rx, 100, "WEAPONS")
        y = 114
        for slot, name, lvl, mx in st["weapons"]:
            self.well(img, (rx, y, rx + rw, y + 30))
            raster.draw_text(img, rx + 4, y + 3, slot, (80, 150, 100))
            raster.draw_text(img, rx + 4, y + 13, name, self.LCD)
            for i in range(mx):
                c = self.AMBER if i < lvl else (40, 50, 30)
                d.rectangle([rx + 4 + i * 9, y + 24, rx + 10 + i * 9, y + 27], fill=c)
            y += 36
        sp_name, count, mx = st["special"]
        self.plate(img, rx, y + 2, "SPECIAL")
        self.well(img, (rx, y + 16, rx + rw, y + 34))
        raster.draw_text(img, rx + 4, y + 22, sp_name, self.AMBER)
        for i in range(mx):
            img.alpha_composite(airstrike_icon(self.AMBER, i >= count), (rx + rw - 14 - i * 13,
                                                                          y + 20))


# --------------------------------------------------------------------------- variant B

class Glass:
    """Sleek dark-glass cockpit panels with chamfered corners and neon outlines."""
    CYAN = (60, 230, 255)
    WHITE = (230, 250, 255)
    MAGENTA = (255, 60, 170)
    AMBER = (255, 190, 60)

    def chamfer_box(self, d, box, cut, fill=None, outline=None, width=1):
        x0, y0, x1, y1 = box
        pts = [(x0 + cut, y0), (x1, y0), (x1, y1 - cut), (x1 - cut, y1), (x0, y1), (x0, y0 + cut)]
        d.polygon(pts, fill=fill, outline=outline, width=width)

    def draw(self, img, st):
        glow_layer = Image.new("RGBA", (SW, SH), (0, 0, 0, 0))
        gd = ImageDraw.Draw(glow_layer)
        # dark glass side panels with a subtle vertical gradient
        arr = np.array(img).astype(np.float64)
        for x0, x1 in ((0, PANEL), (SW - PANEL, SW)):
            yy = np.linspace(0, 1, SH)[:, None]
            arr[:, x0:x1, 0] = 6 + 8 * yy
            arr[:, x0:x1, 1] = 12 + 12 * yy
            arr[:, x0:x1, 2] = 24 + 18 * yy
            arr[::3, x0:x1, :3] *= 0.9
        img.paste(Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA"))
        d = ImageDraw.Draw(img)
        for x in (PANEL - 1, SW - PANEL):
            gd.line([x, 0, x, SH], fill=self.CYAN + (255,))

        def card(box, color=self.CYAN):
            self.chamfer_box(d, box, 6, fill=(10, 24, 40))
            self.chamfer_box(gd, box, 6, outline=color + (255,))

        deferred = []   # edge labels are drawn after the neon bloom so they stay crisp

        def label_on_edge(x, y, text, color=self.CYAN):
            deferred.append((x, y, text, color))

        def gauge(x, y, w, value, color, label):
            label_on_edge(x, y - 10, label, color)
            d.rectangle([x, y, x + w, y + 6], fill=(10, 20, 34))
            fill_w = int(w * value)
            for i in range(fill_w):
                t = i / max(1, w)
                c = tuple(int(v * (0.45 + 0.55 * t)) for v in color)
                d.line([x + i, y + 1, x + i, y + 5], fill=c)
            gd.rectangle([x, y, x + fill_w, y + 6], outline=color + (200,))
            for i in range(0, w, 10):
                d.line([x + i, y + 7, x + i, y + 8], fill=(60, 90, 110))

        # left panel
        card((8, 12, PANEL - 9, 52))
        label_on_edge(16, 9, "SCORE")
        raster.draw_text(img, 16, 22, f"{st['score']:09d}", self.WHITE, scale=2)
        card((8, 64, PANEL - 9, 86))
        label_on_edge(16, 61, "CREDITS", self.AMBER)
        raster.draw_text(img, 16, 72, f"CR {st['credits']:,}".replace(",", "."), self.AMBER)
        card((8, 98, PANEL - 9, 130))
        label_on_edge(16, 95, f"ACT {st['act']} / LEVEL {st['level']:02d}")
        x0, x1 = 16, PANEL - 18
        d.line([x0, 116, x1, 116], fill=(40, 80, 100))
        px = x0 + int((x1 - x0) * st["progress"])
        gd.line([x0, 116, px, 116], fill=self.CYAN + (255,), width=2)
        d.polygon([(px, 111), (px + 4, 116), (px, 121), (px - 4, 116)], fill=self.WHITE)
        d.polygon([(x1 - 3, 112), (x1 + 3, 112), (x1, 120)], fill=self.MAGENTA)
        raster.draw_text(img, x1 - 16, 120, "BOSS", self.MAGENTA)
        # radio
        name, msg = st["radio"]
        card((8, 144, PANEL - 9, 300))
        label_on_edge(16, 141, "INCOMING TRANSMISSION", self.MAGENTA)
        img.alpha_composite(portrait(tint=(90, 220, 255)), (16, 154))
        gd.rectangle([15, 153, 60, 206], outline=self.CYAN + (180,))
        raster.draw_text(img, 66, 156, name.split()[0], self.WHITE)
        raster.draw_text(img, 66, 166, " ".join(name.split()[1:]), self.WHITE)
        for i in range(12):   # voice waveform
            hgt = int(3 + 6 * abs(np.sin(i * 1.7)) * (1 - i / 14))
            d.line([66 + i * 6, 190 - hgt, 66 + i * 6, 190 + hgt], fill=self.CYAN)
        for i, line in enumerate(wrap(msg, 22)):
            raster.draw_text(img, 16, 218 + i * 11, line, self.WHITE)
        # right panel
        rx, rw = SW - PANEL + 12, PANEL - 24
        card((SW - PANEL + 8, 12, SW - 9, 104))
        gauge(rx, 30, rw, st["armor"], (255, 80, 60), "ARMOR")
        gauge(rx, 56, rw, st["shield"], self.CYAN, "SHIELD")
        gauge(rx, 82, rw, st["gen_load"] / st["gen_max"], self.AMBER,
              f"GENERATOR {st['gen_load']}/{st['gen_max']} MW")
        card((SW - PANEL + 8, 116, SW - 9, 280))
        label_on_edge(rx, 113, "WEAPON SYSTEMS")
        y = 126
        for slot, name, lvl, mx in st["weapons"]:
            raster.draw_text(img, rx, y, slot, (90, 150, 180))
            raster.draw_text(img, rx, y + 10, name, self.WHITE)
            for i in range(mx):
                c = self.CYAN if i < lvl else (30, 60, 80)
                self.chamfer_box(d, (rx + rw - 8 - (mx - 1 - i) * 10, y + 10,
                                     rx + rw - (mx - 1 - i) * 10, y + 16), 2, fill=c)
            y += 38
        sp_name, count, mx = st["special"]
        card((SW - PANEL + 8, 292, SW - 9, 330), self.AMBER)
        label_on_edge(rx, 289, "SPECIAL [SPACE]", self.AMBER)
        raster.draw_text(img, rx, 304, sp_name, self.WHITE)
        for i in range(mx):
            img.alpha_composite(airstrike_icon(self.AMBER, i >= count), (rx + rw - 12 - i * 14,
                                                                          302))
        # wingman
        card((8, 312, PANEL - 9, 346))
        label_on_edge(16, 309, "WINGMAN", self.CYAN)
        raster.draw_text(img, 16, 320, st["wingman"][0], self.WHITE)
        raster.draw_text(img, 16, 331, st["wingman"][1], (90, 150, 180))
        for i in range(6 if st["wingman"][2] else 0):
            c = self.CYAN if i < st["wingman"][2] else (30, 60, 80)
            self.chamfer_box(d, (PANEL - 64 + i * 8, 321, PANEL - 58 + i * 8, 327), 2, fill=c)
        # neon bloom
        bloom = glow_layer.filter(ImageFilter.GaussianBlur(2.5))
        img.alpha_composite(bloom)
        img.alpha_composite(bloom)
        img.alpha_composite(glow_layer)
        d = ImageDraw.Draw(img)
        for x, y, text, color in deferred:
            w = raster.text_width(text)
            d.rectangle([x - 3, y - 1, x + w + 2, y + 7], fill=(6, 14, 26))
            raster.draw_text(img, x, y, text, color)


def make(variant_cls, title):
    screen = Image.new("RGBA", (SW, SH), (0, 0, 0, 255))
    screen.alpha_composite(play_field(), (PANEL, 0))
    painter = variant_cls()
    painter.draw(screen, STATE)
    big = sprite.enlarge(screen, 2)
    sheet = raster.sheet(1296, 780, title, "CONCEPT ROUND 01")
    raster.draw_text(sheet, 8, 36, "640X360 NATIVE SCREEN SHOWN AT 2X. PLAY FIELD 320X360, "
                     "SIDE PANELS 160X360.", raster.LABEL_DIM)
    sheet.alpha_composite(big, (8, 50))
    return sheet


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for key, cls, title in (("a", Metal, "HUD A: METALLIC BEVELLED PANELS"),
                            ("b", Glass, "HUD B: DARK GLASS NEON COCKPIT")):
        path = out_path(OUT, f"hud-r01-{key}.png")
        make(cls, title).convert("RGB").save(path, optimize=True)
        print("wrote", path.relative_to(ROOT))


if __name__ == "__main__":
    main()
