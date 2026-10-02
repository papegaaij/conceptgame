#!/usr/bin/env python3
"""Production art: the briefings' tactical maps and mission images (design/ui/briefing), one per
briefing page of the Act 1 intro and Levels 01-02, in the chosen briefing-r08-a look: a dark
tactical display with its grid, scan rows and edge ticks, holographic planets, cyan routes and
labels, red and violet for the Vrell, amber for objectives.

Outputs (assets/ui/briefing/<name>.png, 672x240, textures of their own like the title scene):
  act-1-tether-gate       Act 1 p1: the Tether Gate beyond Neptune, lit
  act-1-outer-stations    Act 1 p2: the outer system's stations, contact lost one by one
  act-1-l1-strike         Act 1 p3: the strike group at Earth-Moon L1, its vector to the Gagarin yards
  act-1-aegis-wing        Act 1 p4: Earth orbit's squadrons; only Aegis Wing makes the intercept
  act-1-stormhawk         Act 1 p5: the AF-12 Stormhawk as a schematic
  level-01-gagarin-yards  L01 p1: the yards, the south rail (Lancer) and north rail (Aegis Two)
  level-01-vrell-scan     L01 p2: Varga's scan of the Skitter and Needler, the glowing parts
  level-02-burning-yards  L02 p1: the burning south arm, the four crewed drydocks and their growths
  level-02-yield-signal   L02 p2: the transmission's repeating pattern, the turret's blind arc
  design/ui/briefing/concept/briefing-images-final-r13-a.png   review sheet

Level 02's two are ready for its data (M4); the level's README names them. Every image is composed
in layers like the hangar map (tools/art/ui_scenes.py): the display and planets posterized to 24
colours with ordered dither, the lines, markers and labels to 16 of their own, then the sprites
from assets/ (the Stormhawk, Skitter, Needler and the intel portraits) with their own palettes.
The labels use the concept pixel font (render/raster.py), as the chosen mockup does.

Run: python3 tools/art/briefing_images.py [name ...] [--review]   (~10 s; after stormhawk.py,
vrell_air.py and intel.py, whose sprites it shows)
"""
import sys

import numpy as np
from PIL import Image, ImageDraw

import artkit
import ui_scenes
from artkit import DESIGN, ROOT, SPRITES, sprite

from render import raster, terrain  # noqa: E402

SCRIPT = "briefing_images.py"
SOURCE = artkit.source_note(SCRIPT, "UI batch")
ROUND = "r13"
OUT = ROOT / "assets" / "ui" / "briefing"
CONCEPT = DESIGN / "ui" / "briefing" / "concept"
W, H = 672, 240
DISPLAY_COLOURS = 24
LINE_COLOURS = 16
CYAN = (60, 220, 255)
CYAN_DIM = (40, 130, 180)
WHITE = (220, 240, 255)
AMBER = (255, 190, 60)
RED = (255, 70, 50)
VIOLET = (190, 120, 255)
GREEN = (120, 255, 160)
GREY = (110, 130, 150)
GRID = 32


class Board:
    """An image in its layers: ``base`` the display as float RGB with the ``grid`` on it, ``over``
    the lines, markers and labels, ``sprites`` pasted last with their own palettes."""

    def __init__(self):
        yy, xx = np.mgrid[0:H, 0:W].astype(np.float64)
        self.yy, self.xx = yy, xx
        t = yy / H
        self.base = np.dstack([5 + 4 * t, 9 + 10 * t, 26 + 16 * t])
        self.over = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        self.draw = ImageDraw.Draw(self.over)
        self.sprites = []
        self.grid = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        g = ImageDraw.Draw(self.grid)
        for x in range(0, W, GRID):
            g.line([(x, 0), (x, H)], fill=(40, 120, 170, 90 if x % (4 * GRID) == 0 else 40))
        for y in range(0, H, GRID):
            g.line([(0, y), (W, y)], fill=(40, 120, 170, 90 if y % (4 * GRID) == 0 else 40))
        for x in range(0, W, GRID // 4):
            g.line([(x, 0), (x, 2 if x % GRID else 5)], fill=(80, 190, 230, 150))
            g.line([(x, H - 1), (x, H - 3 if x % GRID else H - 6)], fill=(80, 190, 230, 150))

    # ------------------------------------------------------------------ the display layer

    def glow(self, cx, cy, radius, colour, strength=1.0):
        d2 = ((self.xx - cx) ** 2 + (self.yy - cy) ** 2) / radius ** 2
        self.base += (np.exp(-d2) * strength)[..., None] * np.array(colour, float)

    def planet(self, cx, cy, r, kind, seed=7):
        """A holographic planet: Earth's relief, the Moon's or a gas giant's bands through a ramp,
        lit from the top left, with a rim glow."""
        d = np.hypot(self.xx - cx, self.yy - cy)
        inside = d < r
        if kind == "earth":
            tex = terrain.earth_coast(W, H, seed=seed, period=False, cell=max(24, int(r)),
                                      stops=terrain.coast_stops_from(ui_scenes.SEA, ui_scenes.LAND, 0.5))
            lum = np.array(tex.convert("L")).astype(np.float64) / 255 * 1.3
            ramp = ui_scenes.HOLO
        elif kind == "moon":
            lum = 0.55 + 0.45 * raster.fbm(W, H, max(8, int(r // 2)), seed, octaves=4, period=False)
            ramp = [(0.0, (6, 10, 20)), (0.5, (30, 50, 70)), (1.0, (120, 150, 170))]
        else:
            lum = 0.6 + 0.25 * np.sin((self.yy - cy) / r * 9 + 0.6 * np.sin((self.xx - cx) / r * 3))
            ramp = [(0.0, (4, 8, 30)), (0.5, (20, 50, 120)), (1.0, (80, 140, 220))]
        nx, ny = (self.xx - cx) / r, (self.yy - cy) / r
        light = np.clip(0.35 - 0.55 * nx - 0.55 * ny + 0.5 * np.sqrt(np.clip(1 - nx ** 2 - ny ** 2, 0, 1)), 0.08, 1)
        body = raster.ramp(ramp, np.clip(lum * light, 0, 1))
        self.base = np.where(inside[..., None], body, self.base)
        self.base += (np.exp(-((d - r) / 3) ** 2) * 0.8)[..., None] * np.array([40, 150, 200])

    # ------------------------------------------------------------------ the line layer

    def label(self, x, y, text, colour=CYAN, right=False):
        if right:
            x -= raster.text_width(text)
        raster.draw_text(self.over, x, y, text, colour + (255,))

    def title(self, text):
        self.label(10, 9, text)

    def dashed(self, points, colour=CYAN, width=1, dash=6, gap=4):
        pts = np.array(points, float)
        for a, b in zip(pts[:-1], pts[1:]):
            length = np.hypot(*(b - a))
            for s in np.arange(0, length, dash + gap):
                e = min(length, s + dash)
                self.draw.line([tuple(a + (b - a) * s / length), tuple(a + (b - a) * e / length)],
                               fill=colour + (230,), width=width)

    def line(self, points, colour=CYAN, width=1, alpha=230):
        self.draw.line([tuple(p) for p in points], fill=colour + (alpha,), width=width)

    def arrow(self, x0, y0, x1, y1, colour=CYAN, size=7, dashed=True):
        """A route to (x1, y1) with its head."""
        if dashed:
            self.dashed([(x0, y0), (x1, y1)], colour, 2)
        else:
            self.line([(x0, y0), (x1, y1)], colour, 2)
        d = np.array([x1 - x0, y1 - y0], float)
        d /= np.linalg.norm(d)
        n = np.array([-d[1], d[0]])
        tip = np.array([x1, y1], float)
        self.draw.polygon([tuple(tip), tuple(tip - d * size * 1.6 + n * size * 0.8),
                           tuple(tip - d * size * 1.6 - n * size * 0.8)], fill=colour + (240,))

    def ring(self, cx, cy, rx, ry=None, colour=CYAN, alpha=200, width=1):
        ry = rx if ry is None else ry
        self.draw.ellipse([cx - rx, cy - ry, cx + rx, cy + ry], outline=colour + (alpha,), width=width)

    def arc(self, cx, cy, rx, ry, start, end, colour=CYAN_DIM, alpha=160):
        self.draw.arc([cx - rx, cy - ry, cx + rx, cy + ry], start, end, fill=colour + (alpha,))

    def marker(self, x, y, kind, colour, size=4):
        """Map symbols: ``square`` (friendly), ``diamond`` (objective), ``cross`` (lost), ``chevron``
        (hostile, pointing down), ``dot``."""
        d = self.draw
        c = colour + (255,)
        if kind == "square":
            d.rectangle([x - size, y - size, x + size, y + size], outline=c)
            d.rectangle([x - 1, y - 1, x + 1, y + 1], fill=c)
        elif kind == "diamond":
            d.polygon([(x, y - size - 1), (x + size + 1, y), (x, y + size + 1), (x - size - 1, y)], outline=c)
        elif kind == "cross":
            d.line([(x - size, y - size), (x + size, y + size)], fill=c, width=2)
            d.line([(x - size, y + size), (x + size, y - size)], fill=c, width=2)
        elif kind == "chevron":
            d.line([(x - size, y - size), (x, y), (x + size, y - size)], fill=c, width=2)
            d.line([(x - size, y - size + 4), (x, y + 4), (x + size, y - size + 4)], fill=c, width=1)
        else:
            d.ellipse([x - size, y - size, x + size, y + size], fill=c)

    def bracket(self, x0, y0, x1, y1, colour=CYAN, arm=6):
        for cx, cy, sx, sy in ((x0, y0, 1, 1), (x1, y0, -1, 1), (x0, y1, 1, -1), (x1, y1, -1, -1)):
            self.line([(cx, cy + sy * arm), (cx, cy), (cx + sx * arm, cy)], colour)

    def sprite(self, img, x, y):
        """A sprite (already palettised) with its centre at (x, y)."""
        self.sprites.append((img, int(x - img.width / 2), int(y - img.height / 2)))

    # ------------------------------------------------------------------ out

    def image(self):
        display = raster.to_rgba_image(np.clip(self.base, 0, 255))
        display.alpha_composite(self.grid)
        base = np.array(display).astype(np.float64)[..., :3] * np.where(self.yy % 2 == 1, 0.88, 1.0)[..., None]
        out = ui_scenes.dithered(base, DISPLAY_COLOURS)
        out.alpha_composite(self.over)
        lines = np.array(self.over)[..., 3] > 0
        rgb = np.array(out).astype(np.float64)[..., :3]
        out.alpha_composite(artkit.quantize_set([raster.to_rgba_image(rgb, lines * 255)], LINE_COLOURS)[0])
        for img, x, y in self.sprites:
            out.alpha_composite(img, (x, y))
        return out.convert("RGB")


def asset(name, zoom=1):
    return sprite.enlarge(Image.open(SPRITES / f"{name}.png").convert("RGBA"), zoom)


def holo(img, colour=CYAN, colours=12):
    """A sprite as the display draws it: its brightness in one hue, alpha kept."""
    a = np.array(img).astype(np.float64)
    lum = (a[..., :3] @ np.array([0.3, 0.55, 0.15]) / 255)[..., None]
    rgb = np.clip(np.array(colour) * (0.25 + lum * 1.1), 0, 255)
    return artkit.quantize_set([raster.to_rgba_image(rgb, a[..., 3])], colours)[0]


# --------------------------------------------------------------------------- the images

def tether_gate():
    b = Board()
    b.planet(96, 150, 46, "giant")
    b.glow(450, 120, 70, (60, 30, 120), 0.9)
    b.glow(450, 120, 26, (60, 200, 200), 0.8)
    b.title("TETHER GATE - TRANS-NEPTUNIAN, 41 AU")
    b.label(96, 204, "NEPTUNE", CYAN_DIM, right=False)
    for i, (rx, ry) in enumerate(((92, 40), (86, 36), (78, 32))):
        b.ring(450, 120, rx, ry, (VIOLET, CYAN, WHITE)[i], 230 - 40 * i, 2 if i == 0 else 1)
    for k in range(0, 360, 15):
        a = np.radians(k)
        b.line([(450 + 96 * np.cos(a), 120 + 43 * np.sin(a)), (450 + 104 * np.cos(a), 120 + 47 * np.sin(a))],
               VIOLET, 1, 200)
    b.dashed([(150, 140), (340, 124)], CYAN_DIM)
    b.label(170, 112, "1.8 BILLION KM", CYAN_DIM)
    b.label(W - 10, 40, "14 MAR 2185 06:12 UTC", AMBER, right=True)
    b.label(W - 10, 52, "GATE ACTIVE", RED, right=True)
    b.label(W - 10, 64, "DORMANT 44 YEARS", CYAN_DIM, right=True)
    b.label(10, H - 16, "DEEP SPACE ARRAY - LONG-RANGE OPTICAL", CYAN_DIM)
    return b


def outer_stations():
    b = Board()
    b.glow(-30, 120, 90, (120, 90, 30), 1.2)
    b.title("OUTER SYSTEM - STATION NETWORK")
    stations = [(170, "JUPITER", "CALLISTO RELAY", 3), (280, "SATURN", "TITAN HARBOUR", 7),
                (400, "URANUS", "OBERON WATCH", 9), (510, "NEPTUNE", "TRITON STATION", 11)]
    for x, planet, name, day in stations:
        b.arc(-30, 120, x + 30, 150 + x * 0.12, -40, 40)
        y = 120 + 30 * np.sin(x / 70)
        b.marker(x, y, "square", GREY)
        b.marker(x, y, "cross", RED, 6)
        b.label(x - 30, y + 14, name, CYAN_DIM)
        b.label(x - 30, y + 24, f"SILENT DAY {day}", RED)
        b.label(x - 30, 28, planet, CYAN_DIM)
    b.glow(630, 120, 22, (60, 30, 120), 1.0)
    b.ring(630, 120, 12, 18, VIOLET, 230, 2)
    b.label(W - 10, 150, "GATE", VIOLET, right=True)
    b.dashed([(612, 120), (540, 118)], VIOLET)
    b.label(W - 10, 9, "CONTACT LOST: 4 OF 4", RED, right=True)
    b.label(10, H - 16, "NO DISTRESS CALLS RECEIVED", CYAN_DIM)
    return b


def l1_strike():
    b = Board()
    b.planet(110, 130, 78, "earth", seed=19)
    b.planet(600, 110, 30, "moon", seed=5)
    b.title("CISLUNAR SPACE - EARTH-MOON L1")
    lx, ly = 470, 118
    b.ring(lx, ly, 30, colour=RED, alpha=200)
    b.ring(lx, ly, 16, colour=RED, alpha=140)
    for dx, dy in ((-10, -8), (6, -12), (12, 4), (-4, 8), (-14, 4), (2, -2)):
        b.marker(lx + dx, ly + dy, "dot", VIOLET, 2)
    b.label(lx - 40, ly - 50, "VRELL STRIKE GROUP", RED)
    b.label(lx - 40, ly - 40, "L1 - 06:41 UTC", AMBER)
    gx, gy = 196, 96
    b.marker(gx, gy, "diamond", AMBER, 5)
    b.label(gx + 12, gy - 26, "GAGARIN SHIPYARDS", AMBER)
    b.label(gx + 12, gy - 16, "HALF THE NEW FLEET IN DOCK", CYAN_DIM)
    b.arrow(lx - 34, ly - 6, gx + 12, gy + 2, RED)
    b.arc(110, 130, 86, 86, -80, 80, CYAN_DIM)
    b.label(300, 150, "ETA 40 MIN", RED)
    b.label(10, H - 16, "EARTH", CYAN_DIM)
    b.label(W - 10, H - 16, "LUNA", CYAN_DIM, right=True)
    return b


def aegis_wing():
    b = Board()
    b.planet(336, 520, 330, "earth", seed=23)
    b.title("EARTH ORBIT - CDF SQUADRONS")
    gx, gy = 336, 150
    b.marker(gx, gy, "diamond", AMBER, 5)
    b.label(gx - 50, gy + 12, "GAGARIN YARDS", AMBER)
    squadrons = [(110, 160, "PATROL 3", "ETA 2H 10M"), (210, 176, "PATROL 7", "ETA 1H 25M"),
                 (470, 176, "CUTTERS 2", "ETA 3H"), (570, 158, "PATROL 1", "REFIT")]
    for x, y, name, eta in squadrons:
        b.marker(x, y, "square", GREY)
        b.label(x - 24, y + 10, name, GREY)
        b.label(x - 24, y + 20, eta, GREY)
    b.marker(gx + 40, gy - 30, "square", CYAN)
    b.label(gx + 54, gy - 36, "AEGIS WING", CYAN)
    b.label(gx + 54, gy - 26, "READY - ETA 9 MIN", GREEN)
    vx, vy = 336, 34
    for dx in (-24, -8, 8, 24):
        b.marker(vx + dx, vy, "chevron", RED, 5)
    b.label(vx + 40, vy - 4, "STRIKE GROUP", RED)
    b.line([(gx - 120, gy - 52), (gx + 120, gy - 52)], AMBER, 2)
    b.label(gx + 126, gy - 56, "THE LINE", AMBER)
    return b


def stormhawk():
    b = Board()
    b.title("AF-12 STORMHAWK - SCHEMATIC")
    ship = holo(asset("ship_2", 4))
    cx, cy = 336, 124
    b.sprite(ship, cx, cy)
    b.ring(cx, cy, 104, 104, CYAN_DIM, 70)
    callouts = [((cx, cy - 86), (cx - 150, 40), "FRONT MOUNT - PULSE CANNON"),
                ((cx - 70, cy + 12), (cx - 290, 120), "L WING MOUNT"),
                ((cx + 70, cy + 12), (cx + 170, 120), "R WING MOUNT"),
                ((cx, cy + 80), (cx - 150, 206), "REAR MOUNT"),
                ((cx + 8, cy - 20), (cx + 170, 52), "SHIELD + ARMOUR")]
    for (px, py), (lx, ly), text in callouts:
        b.line([(px, py), (lx + (0 if lx > px else raster.text_width(text)), ly + 3)], CYAN, 1, 200)
        b.marker(px, py, "dot", CYAN, 2)
        b.label(lx, ly, text, WHITE)
    b.label(W - 10, 9, "IN SERVICE: 9 DAYS", AMBER, right=True)
    b.label(W - 10, H - 16, "PILOT: LANCER", CYAN, right=True)
    return b


def yards():
    """The Gagarin yards from above: the hub, the north and south arms with their docks."""
    b = Board()
    hub = (110, 120)
    b.ring(*hub, 22, colour=CYAN, alpha=220, width=2)
    b.ring(*hub, 10, colour=CYAN_DIM)
    b.label(hub[0] - 26, hub[1] + 30, "HUB", CYAN_DIM)
    for y0, name in ((78, "NORTH ARM"), (162, "SOUTH ARM")):
        b.line([(hub[0] + 20, hub[1] + (y0 - hub[1]) * 0.4), (150, y0), (640, y0)], CYAN, 2)
        b.line([(150, y0 - 6), (640, y0 - 6)], CYAN_DIM, 1, 120)
        b.line([(150, y0 + 6), (640, y0 + 6)], CYAN_DIM, 1, 120)
        b.label(590, y0 + (12 if y0 > 120 else -18), name, CYAN_DIM)
    return b


def gagarin_yards():
    b = yards()
    b.title("GAGARIN SHIPYARDS - DOCK ARMS")
    for x in range(190, 560, 70):
        for y0 in (78, 162):
            b.draw.rectangle([x, y0 - 14, x + 40, y0 - 8], outline=CYAN_DIM + (200,))
    b.arrow(170, 200, 600, 200, CYAN, dashed=False)
    b.label(180, 210, "SOUTH RAIL - LANCER", CYAN)
    b.arrow(170, 40, 600, 40, CYAN_DIM)
    b.label(180, 24, "NORTH RAIL - AEGIS TWO", CYAN_DIM)
    for x in (380, 470, 560):
        b.marker(x, 120, "chevron", RED, 5)
    b.arrow(660, 120, 610, 120, RED)
    b.label(W - 10, 132, "FROM L1", RED, right=True)
    b.label(W - 10, 9, "KEEP THEM OFF THE YARDS", AMBER, right=True)
    return b


def vrell_scan():
    b = Board()
    b.title("SENSOR SCAN - VRELL SCOUTS")
    for i, (name, frame, zoom, x) in enumerate((("SKITTER", "skitter_0", 3, 160), ("NEEDLER", "needler_0", 3, 470))):
        img = asset(frame, zoom)
        b.glow(x, 118, 60, (10, 40, 50), 0.6)
        b.bracket(x - 62, 50, x + 62, 186)
        b.sprite(img, x, 118)
        b.label(x - 60, 194, name, WHITE)
        b.label(x - 60, 206, "METAL: NONE DETECTED", RED)
        b.label(x - 60, 218, "HEAT: CORE 1400 K", AMBER)
        b.label(x + 70, 74, "AIM FOR", AMBER)
        b.label(x + 70, 84, "THE GLOW", AMBER)
        b.line([(x + 68, 90), (x + 12, 116)], AMBER, 1, 220)
    return b


def burning_yards():
    b = yards()
    b.title("GAGARIN SHIPYARDS - SOUTH ARM BURNING")
    for x in (230, 330, 430, 530):
        b.glow(x + 20, 160, 26, (120, 40, 10), 0.9)
    for i, x in enumerate((210, 310, 410, 510)):
        b.draw.rectangle([x, 172, x + 46, 196], outline=AMBER + (230,))
        b.label(x + 4, 178, f"DOCK {i + 1}", AMBER)
        b.label(x + 4, 188, "CREW", GREEN)
        for k in range(3):
            b.marker(x + 10 + 13 * k, 166, "dot", RED, 2)
    for x in (260, 380, 480, 600):
        b.glow(x, 78, 18, (100, 30, 10), 0.8)
    b.label(W - 10, 9, "4 DOCKS - CREWS ABOARD", AMBER, right=True)
    b.label(10, H - 16, "BURN THE GROWTHS OFF EACH DOCK BEFORE IT LEAVES YOUR SECTOR", CYAN_DIM)
    b.label(560, 24, "AEGIS TWO", CYAN_DIM)
    return b


def yield_signal():
    b = Board()
    b.title("INTERCEPT - VRELL TRANSMISSION, L01 T+160 S")
    rng = np.random.default_rng(160)
    xs = np.arange(10, 400)
    motif = np.sin(np.linspace(0, 6 * np.pi, 40)) * np.hanning(40) * 26
    wave = rng.normal(0, 5, xs.size)
    for start in (20, 110, 200, 290):
        wave[start:start + 40] += motif
        b.draw.rectangle([10 + start, 60, 10 + start + 40, 150], outline=AMBER + (160,))
    b.line(list(zip(xs, 105 + wave)), VIOLET, 1, 240)
    b.label(10, 160, "REPEATING PATTERN X4", AMBER)
    b.label(10, 172, "BEST FIT: 'YIELD'", WHITE)
    b.label(10, 184, "CONFIDENCE 61 %", CYAN_DIM)
    tx, ty = 540, 110
    turret = Image.open(SPRITES / "intel" / "spine-turret.png").convert("RGBA")
    b.sprite(sprite.enlarge(turret, 3), tx, ty)
    for a0, a1, colour, alpha in ((200, 340, RED, 150), (20, 160, GREEN, 120)):
        b.draw.pieslice([tx - 80, ty - 80, tx + 80, ty + 80], a0, a1, outline=colour + (alpha,))
    b.label(tx - 64, 18, "TRACKING ARC", RED)
    b.label(tx - 64, 214, "BLIND BEHIND", GREEN)
    b.label(W - 10, 9, "GROWTH = TURRET", AMBER, right=True)
    return b


IMAGES = {
    "act-1-tether-gate": tether_gate,
    "act-1-outer-stations": outer_stations,
    "act-1-l1-strike": l1_strike,
    "act-1-aegis-wing": aegis_wing,
    "act-1-stormhawk": stormhawk,
    "level-01-gagarin-yards": gagarin_yards,
    "level-01-vrell-scan": vrell_scan,
    "level-02-burning-yards": burning_yards,
    "level-02-yield-signal": yield_signal,
}


def build(names):
    OUT.mkdir(parents=True, exist_ok=True)
    for name in names:
        artkit.save_png(IMAGES[name]().image(), OUT / f"{name}.png", SOURCE)
    print(f"{len(names)} briefing images in {OUT.relative_to(ROOT)}")


def review():
    cols = 2
    rows = -(-len(IMAGES) // cols)
    sheet = raster.sheet(16 + cols * (W + 16), 56 + rows * (H + 30), "BRIEFING IMAGES (FINAL R13): ONE PER PAGE",
                         f"PRODUCTION ART, UI BATCH - {ROUND.upper()}")
    raster.draw_text(sheet, 16, 38, f"{len(IMAGES)} IMAGES {W}X{H} AT 1X, AS THE BRIEFING SCREEN DRAWS THEM ABOVE THE TEXT",
                     raster.LABEL)
    for i, name in enumerate(IMAGES):
        img = Image.open(OUT / f"{name}.png").convert("RGBA")
        x, y = 16 + (i % cols) * (W + 16), 54 + (i // cols) * (H + 30)
        sheet.alpha_composite(img, (x, y))
        raster.draw_text(sheet, x, y + H + 6, f"{name.upper()} ({artkit.colour_count([img])} COLOURS)", raster.LABEL_DIM)
    path = CONCEPT / f"briefing-images-final-{ROUND}-a.png"
    sheet.convert("RGB").save(path, optimize=True)
    print(f"review: {path.relative_to(ROOT)}")


if __name__ == "__main__":
    args = [a for a in sys.argv[1:] if a != "--review"]
    if "--review" not in sys.argv[1:]:
        build(args or list(IMAGES))
    review()
