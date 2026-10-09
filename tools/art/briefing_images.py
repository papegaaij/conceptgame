#!/usr/bin/env python3
"""Production art: the briefings' tactical maps and mission images (design/ui/briefing), one per
briefing page of the Act 1 intro, Levels 01-07, the Act 1 outro, the Act 2 intro and Levels 08-11, in the chosen briefing-r08-a look: a dark
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
  level-03-spore-lanes    L03 p1: the high lanes over last week's battle site and its debris field,
                          the spore carriers seeding Earth, Lancer's lane
  level-03-spore-echo     L03 p2: the Spore Bomber below, its spores rising into a wide spread, the
                          long-range echo (the Leviathan as a noisy silhouette)
  level-04-convoy-road    L04 p1: Tranquility Base and the brood pods, the five crawlers on the road
                          across the rille to the mass-driver terminal, walkers in the craters,
                          Hammer flight (the Airstrike)
  level-04-walker-scan    L04 p2: the Scuttler facing where it walks, claws forward, the glowing back
  level-05-crater-nest    L05 p1: the nest crater beside the mass-driver line, the four batteries A-D
                          on its floor, Lancer's run down the rail and over the rim, the rail's sleds
  level-05-mortar-scan    L05 p2: the Polyp Mortar's lob arcing to its lime marker a second ahead, the
                          ship moving out of it; an unknown contact holding in orbit over the crater
  level-06-daedalus-rim   L06 p1: the far side across the terminator, the silent settlements, Daedalus
                          Rim's lit domes, Lancer's run into the dark by headlight along the ore rail
  level-06-edge-scan      L06 p2: the Mantis at the screen edge sweeping its beam from its eye (the
                          production telegraph wedge and charged-lance beam), side-firing guns
                          reaching it; the Coilwyrm at the data's spacing coming round behind the
                          ship (its path history dashed beyond the tail), its open rear
  level-07-l1-carrier     L07 p1: the Brood Carrier (broadside) holding at Earth-Moon L1, the pods
                          that hit Luna traced back to it, its escort screen, the overrun L1 picket,
                          Aegis's way through; its weak points named
  level-07-overhead-scan  L07 p2: the carrier's three stages (part G's D1): nose-down on high air with
                          its shadow, a bay pair open, only missiles reaching up; the turn in place
                          (a turn frame as a hologram); broadside at the ship's level, a pair open,
                          a pair burst, the plate iris open over the lime core
  act-1-outro-carcass     Act 1 outro p1: the carcass at L1 (the break-up chunks, dead, burnt dark, drifting,
                          ichor clouds), Earth beyond, what the act saved
  act-1-outro-daedalus-rim  outro p2: Daedalus Rim still empty (Level 06's gate and domes, lights on),
                          the search's tally, the file kept open
  act-1-outro-second-fleet  outro p3: the long-range plot: the second fleet beyond the Moon, ten times
                          the destroyed carrier group, its track ending in Earth's atmosphere
  act-1-outro-rook        outro p4: Aegis Wing reassigned to Earth defence, Rook's craft (Ember) in the
                          wing slot beside Lancer's Stormhawk, his radio portrait
  act-2-landfall          Act 2 p1: the landers coming down at dawn below the cloud deck over the Gulf
                          of Guinea, 34 burning trails over the sea, the CDF tracking overlay counting
                          them (12 days tracked, none stopped), the three landing zones
  act-2-front-lines       Act 2 p2: the CDF global display (a flat projection of hand-drawn coasts),
                          the three landing zones glowing, the fronts numbered: the cities (Nova Lagos,
                          Geneva Concord), the Atlantic sea lanes, the Arctic relay chain
  act-2-over-home         Act 2 p3: a Nova Lagos street from rooftop height in one-point perspective,
                          people on the low roofs looking up and waving, Lancer passing low
  act-2-scramble          Act 2 p4: Aegis Wing on a coastal CDF airbase at dusk, Lancer, Rook's Ember
                          and three Stormhawks on their pads, canopies closing; Nova Lagos burning 40 km
                          off
  level-08-nova-lagos     L08 p1: the night route: the harbour, the elevated highways, the tower
                          district, the Third Mainland highway over the lagoon to the Ikoyi shelters,
                          the walkers heading for them; Lancer and Rook at the start
  level-08-walker-scan    L08 p2: the Creeper on a low roof (its gland and eye glow added), its aimed
                          five-way fan of orbs, a convoy's fans 0.5 s apart front to back, the
                          anti-ground x2 marker
  level-09-arcology-district  L09 p1: the arcology district before dawn, the route, clusters A-C
  level-09-node-scan      L09 p2: the Hive Node (hardened, its iris cycle), the Ravager's pounce arc
  level-10-evacuation-route  L10 p1: the corridor at first light, west to east: Eko spaceport's five
                          pads with the shuttles, the jammed suburbs, the maglev viaduct between the
                          creep districts, the coast road and the capsized ferry, the lagoon and the
                          climb-out; the route, Lancer and Rook, contacts closing from behind
  level-10-wraith-scan    L10 p2: the Wraith cloaked (its additive shimmer) and decloaked (over its
                          flash); its pass overhead, loop and rear entry with the 3 s warning, its
                          bursts up through the shuttle band; a Mote Swarm's flock and loop-back
  level-11-convoy-route   L11 p1: the North Atlantic from above: Convoy Atlas-Seven (three cargo hulls and
                          the frigate, production ships heading east), the seeded sea lanes with their
                          Driftjellies, the reef line with its gun rafts, Platform Tiamat on the route
                          with the Kraken's gripping arms and its hologram under it, open water beyond
  level-11-sub-scan       L11 p2: a cut-away at the waterline: a Driftjelly surfaced (guns reach it) and
                          submerged (they do not), the shots stopping at the waterline, a torpedo's run
                          under the surface, a Reef Spitter raft and its roots, the unknown large contact
  design/ui/briefing/concept/briefing-images-final-r13-a.png   review sheet, Act 1 intro + L01-02
  design/ui/briefing/concept/briefing-images-final-r20-a.png   review sheet, L03-04 (M4 batch)
  design/ui/briefing/concept/briefing-images-final-r21-a.png   review sheet, L05 (M4 part E)
  design/ui/briefing/concept/briefing-images-final-r23-a.png   review sheet, L06 (M4 part F)
  design/ui/briefing/concept/briefing-images-final-r25-a.png   review sheet, L07 + Act 1 outro (part G)
  design/ui/briefing/concept/briefing-images-final-r30-a.png   review sheet, Act 2 intro + L08 (M5 part B)
  design/ui/briefing/concept/briefing-images-final-r31-a.png   review sheet, L09 (M5 part C)
  design/ui/briefing/concept/briefing-images-final-r32-a.png   review sheet, L10 (M5 part D)
  design/ui/briefing/concept/briefing-images-final-r33-a.png   review sheet, L11 (M5 part E)
 Every image is composed
in layers like the hangar map (tools/art/ui_scenes.py): the display and planets posterized to 24
colours with ordered dither, the lines, markers and labels to 16 of their own, then the sprites
from assets/ (the Stormhawk, Skitter, Needler, the Brood Carrier's composed poses, chunks and glows,
backdrop pieces, the portraits, the Creeper, the orb, the bomb, Rook's production Ember craft) with
their own palettes; the Act 1 outro's Rook craft from vfx_r08's chosen round-09 render, as approved;
additive light (the Mantis's beam parts from assets/, its wedge from mantis_beam.py's generator
code; the carrier's core glow, also on its open sacs) added as the game blends it, its pixels to 32 colours of their own.
The labels use the concept pixel font (render/raster.py), as the chosen mockup does.

Run: python3 tools/art/briefing_images.py [name ...] [r13|r20|r21|r23|r25|r30|r31|r32|r33] [--review]   (~10 s; after
stormhawk.py, vrell_air.py, intel.py, vrell_l03.py, leviathan.py, vrell_l04.py, civilian_crawler.py
airstrike_bomber.py, l05_hazards.py, mantis.py, mantis_beam.py, coilwyrm.py, l06_darkness.py,
backdrop_l06.py, brood_carrier.py, brood_carrier_death.py, backdrop_l07.py, portraits.py, rook.py,
creeper.py, enemy_bullets.py, weapon_fx.py, hive_node.py, ravager.py, wraith.py, mote_swarm.py and
shuttle.py, convoy_ships.py (switch the ship pair: its --variant b, then this script again),
driftjelly.py, reef_spitter.py, harbour_kraken.py and backdrop_l11.py, whose sprites and pieces it shows); the review
sheet written is the open round's (r33) unless
a round is named.
"""
import json
import sys

import numpy as np
import yaml
from PIL import Image, ImageDraw

import artkit
import brood_carrier as bc
import mantis_beam
import ui_scenes
import vfx_r08 as v8  # noqa: E402  (concept script, imported unchanged: Rook's craft, as stormhawk.py's ship)
from artkit import DESIGN, ROOT, SPRITES, sprite

from render import raster, terrain  # noqa: E402

SCRIPT = "briefing_images.py"
SOURCE = artkit.source_note(SCRIPT, "UI batch")
SOURCE_M4 = artkit.source_note(SCRIPT, "M4 briefing images")
SOURCE_M5 = artkit.source_note(SCRIPT, "M5 part B batch")
SOURCE_M5C = artkit.source_note(SCRIPT, "M5 part C batch")
SOURCE_M5D = artkit.source_note(SCRIPT, "M5 part D batch")
SOURCE_M5E = artkit.source_note(SCRIPT, "M5 part E batch")
ROUND = "r33"  # the open round; the sheet of an earlier batch: name its round (r13, r20, r21, r23, r25, r30, r31, r32)
OUT = ROOT / "assets" / "ui" / "briefing"
CONCEPT = DESIGN / "ui" / "briefing" / "concept"
W, H = 672, 240
DISPLAY_COLOURS = 24
LINE_COLOURS = 16
LIGHT_COLOURS = 32   # each additive light layer's own palette (the Mantis's telegraph wedge: 32 colours)
CYAN = (60, 220, 255)
CYAN_DIM = (40, 130, 180)
WHITE = (220, 240, 255)
AMBER = (255, 190, 60)
RED = (255, 70, 50)
VIOLET = (190, 120, 255)
GREEN = (120, 255, 160)
LIME = (180, 255, 70)   # the Brood Carrier's weak points
GREY = (110, 130, 150)
GRID = 32


class Board:
    """An image in its layers: ``base`` the display as float RGB with the ``grid`` on it, ``over``
    the lines, markers and labels, ``sprites`` pasted last with their own palettes; ``light`` the
    additive sprites (premultiplied on black, as the game blends them), ``under`` on the display
    below the lines or ``over`` the sprites, each with a palette of its own."""

    def __init__(self):
        yy, xx = np.mgrid[0:H, 0:W].astype(np.float64)
        self.yy, self.xx = yy, xx
        t = yy / H
        self.base = np.dstack([5 + 4 * t, 9 + 10 * t, 26 + 16 * t])
        self.over = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        self.draw = ImageDraw.Draw(self.over)
        self.sprites = []
        self.light = {"under": np.zeros((H, W, 3)), "over": np.zeros((H, W, 3))}
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

    def add_light(self, rgb, x, y, layer="under"):
        """An additive sprite (an RGB array, premultiplied on black) with its top left at (x, y)."""
        mantis_beam.add(self.light[layer], np.asarray(rgb, np.float64), int(round(x)), int(round(y)))

    # ------------------------------------------------------------------ out

    def image(self):
        display = raster.to_rgba_image(np.clip(self.base, 0, 255))
        display.alpha_composite(self.grid)
        base = np.array(display).astype(np.float64)[..., :3] * np.where(self.yy % 2 == 1, 0.88, 1.0)[..., None]
        out = ui_scenes.dithered(base, DISPLAY_COLOURS)
        self._lit(out, "under")
        out.alpha_composite(self.over)
        lines = np.array(self.over)[..., 3] > 0
        rgb = np.array(out).astype(np.float64)[..., :3]
        out.alpha_composite(artkit.quantize_set([raster.to_rgba_image(rgb, lines * 255)], LINE_COLOURS)[0])
        for img, x, y in self.sprites:
            out.alpha_composite(img, (x, y))
        self._lit(out, "over")
        return out.convert("RGB")

    def _lit(self, out, layer):
        """Adds a light layer to ``out``; the pixels it touches get a palette of their own."""
        light = self.light[layer]
        lit = light.max(axis=-1) >= 1
        if lit.any():
            rgb = np.clip(np.array(out).astype(np.float64)[..., :3] + light, 0, 255)
            out.alpha_composite(artkit.quantize_set([raster.to_rgba_image(rgb, lit * 255)], LIGHT_COLOURS)[0])


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


def spore_lanes():
    """L03 p1: the high lanes over last week's battle site, the carriers seeding Earth."""
    b = Board()
    b.planet(336, 560, 360, "earth", seed=31)
    b.title("HIGH ORBITAL LANES - OVER THE GAGARIN BATTLE SITE")
    for y, name in ((70, "LANE 2"), (120, "LANE 1")):
        b.dashed([(20, y), (650, y)], CYAN_DIM, 1, 10, 6)
        b.label(24, y - 12, name, CYAN_DIM)
    rng = np.random.default_rng(3)
    for x, y in zip(rng.uniform(250, 470, 26), rng.uniform(84, 150, 26)):
        s = rng.uniform(1.5, 4)
        a = rng.uniform(0, np.pi)
        b.draw.polygon([(x + s * np.cos(a + k * 2.1), y + s * np.sin(a + k * 2.1)) for k in range(3)],
                       outline=GREY + (220,))
    b.bracket(240, 80, 480, 156, GREY)
    b.label(244, 160, "DEBRIS FIELD - LAST WEEK'S BATTLE", GREY)
    b.marker(372, 104, "cross", GREY, 5)
    b.label(384, 92, "KESTREL - WRECK", GREY)
    for x, y in ((150, 96), (300, 94), (440, 112), (560, 98)):
        b.glow(x, y, 16, (60, 30, 90), 0.7)
        b.marker(x, y, "chevron", VIOLET, 5)
        for k in range(4):
            b.marker(x - 6 + 4 * k, y + 16 + 13 * k, "dot", VIOLET, 1)
        b.dashed([(x + 4, y + 10), (x + 22, y + 74)], VIOLET, 1, 3, 4)
    b.label(150, 36, "SPORE CARRIERS", VIOLET)
    b.label(150, 48, "DRIFTING, DROPPING SPORES", VIOLET)
    b.arrow(30, 132, 220, 132, CYAN, dashed=False)
    b.label(30, 140, "LANCER - CLEAR THE LANE", CYAN)
    b.label(W - 10, 9, "10 CARRIERS - NONE GETS THROUGH", AMBER, right=True)
    b.label(W - 10, 176, "SEEDING THE PLANET", RED, right=True)
    b.label(10, H - 16, "EARTH - ATMOSPHERE BELOW", CYAN_DIM)
    return b


def spore_echo():
    """L03 p2: Varga's scan of the Spore Bomber and its rising spores, the long-range echo."""
    b = Board()
    b.title("SENSOR SCAN - SPORE BOMBER, LONG-RANGE ECHO")
    x = 110
    b.glow(x, 112, 60, (10, 40, 50), 0.6)
    b.bracket(x - 76, 36, x + 76, 186)
    b.sprite(asset("spore-bomber_0", 2), x, 112)
    b.label(x - 74, 194, "SPORE BOMBER", WHITE)
    b.label(x - 74, 206, "LOW-AIR - BELOW YOUR LEVEL", AMBER)
    b.label(x - 74, 218, "DROPS SPORE MINES", RED)
    sx, sy = 300, 200
    ship = asset("ship_2")
    b.sprite(ship, sx, sy)
    for a in (-34, -17, 0, 17, 34):
        t = np.radians(a)
        b.dashed([(sx + 14 * np.sin(t), sy - 26), (sx + 150 * np.sin(t), sy - 26 - 130 * np.cos(t))], CYAN, 1, 4, 4)
    mine = Image.open(SPRITES / "spore-mine_2.png").convert("RGBA")
    rng = np.random.default_rng(11)
    for mx, my in zip(rng.uniform(232, 372, 7), rng.uniform(64, 140, 7)):
        b.sprite(sprite.enlarge(mine, 2), mx, my)
        b.line([(mx, my + 16), (mx, my + 30)], VIOLET, 1, 160)
    b.label(232, 30, "SPORES RISE TO YOU", VIOLET)
    b.label(232, 40, "IN ABOUT 1 S", VIOLET)
    b.label(sx + 24, sy - 4, "GO WIDE", CYAN)
    rx, ry, r = 556, 132, 92
    for k in (1, 2, 3):
        b.ring(rx, ry, r * k / 3, colour=CYAN_DIM, alpha=110)
    b.line([(rx - r, ry), (rx + r, ry)], CYAN_DIM, 1, 80)
    b.line([(rx, ry - r), (rx, ry + r)], CYAN_DIM, 1, 80)
    b.draw.pieslice([rx - r, ry - r, rx + r, ry + r], 250, 290, fill=CYAN + (40,))
    b.marker(rx, ry, "square", CYAN, 3)
    big = Image.open(SPRITES / "leviathan-cross_0.png").convert("RGBA")
    big = big.resize((big.width // 4, big.height // 4), Image.LANCZOS)
    a = np.array(big)
    keep = (a[..., 3] > 128) & (np.random.default_rng(62).random(a.shape[:2]) > 0.3)
    keep &= (np.arange(a.shape[0]) % 3 != 0)[:, None]
    a[..., 3] = np.where(keep, 255, 0)
    b.glow(rx + 6, ry - 46, 44, (60, 30, 110), 0.9)
    b.sprite(holo(Image.fromarray(a), VIOLET, 6), rx + 6, ry - 46)
    b.label(rx - r - 10, 28, "LONG-RANGE ECHO", RED, right=True)
    b.label(rx - r - 10, 40, "VERY BIG", WHITE, right=True)
    b.label(rx - r - 10, 52, "VERY SLOW", WHITE, right=True)
    b.label(rx - r - 10, 64, "UNKNOWN", RED, right=True)
    b.label(W - 10, H - 16, "DON'T OUT-SHOOT IT: OUT-FLY IT", AMBER, right=True)
    return b


def convoy_road():
    """L04 p1: Tranquility Base, the brood pods, the convoy's road across the rille to the
    mass-driver terminal, the walkers in the craters, Hammer flight."""
    b = Board()
    b.glow(80, 130, 90, (40, 40, 46), 0.9)
    b.glow(600, 130, 80, (40, 40, 46), 0.7)
    b.title("MARE TRANQUILLITATIS - CONVOY ROAD")
    road = [(70, 150), (170, 160), (270, 136), (330, 140), (390, 128), (490, 150), (600, 138)]
    b.line(road, CYAN_DIM, 7, 120)
    b.line(road, AMBER, 1, 200)
    rille = [(300 + 18 * np.sin(y / 22), y) for y in range(24, H - 20, 4)]
    b.line(rille, CYAN_DIM, 1, 200)
    b.line([(x + 30, y) for x, y in rille], CYAN_DIM, 1, 200)
    b.draw.rectangle([308, 132, 352, 146], outline=CYAN + (230,))
    b.label(358, 206, "RILLE", CYAN_DIM)
    b.label(356, 106, "ROAD BRIDGE", CYAN)
    for cx, cy in ((60, 116), (84, 108), (98, 128), (66, 136)):
        b.ring(cx, cy, 7, colour=CYAN, alpha=200)
    b.marker(80, 124, "diamond", GREEN, 3)
    b.label(14, 36, "TRANQUILITY BASE", CYAN)
    b.label(14, 46, "HERITAGE SITE INTACT", GREEN)
    for px, py in ((44, 172), (126, 98), (150, 194), (30, 80), (200, 82), (230, 196)):
        b.glow(px, py, 12, (50, 20, 80), 0.8)
        b.ring(px, py, 5, colour=VIOLET, alpha=230)
        b.marker(px, py, "dot", VIOLET, 1)
    b.label(14, 214, "BROOD PODS - LANDED OVERNIGHT", VIOLET)
    pip = holo(Image.open(SPRITES / "civilian-crawler-pip.png").convert("RGBA").transpose(Image.ROTATE_270),
               GREEN, 6)
    for k in range(5):
        b.sprite(pip, 120 + 22 * k, 154 + (k > 1) * 3 - (k > 3) * 6)
    b.label(110, 168, "5 CRAWLERS - 400 CIVILIANS", GREEN)
    for cx, cy, walker in ((200, 120, True), (250, 178, False), (420, 172, True), (450, 100, True),
                           (530, 182, True), (560, 104, False)):
        b.ring(cx, cy, 14, 10, GREY, 200)
        if walker:
            b.marker(cx, cy + 2, "chevron", RED, 4)
    b.label(410, 204, "WALKERS IN THE CRATERS", RED)
    tx, ty = 620, 138
    b.draw.rectangle([tx - 16, ty - 12, tx + 16, ty + 12], outline=AMBER + (240,))
    b.marker(tx, ty, "diamond", AMBER, 4)
    b.line([(tx + 16, ty - 4), (W - 4, ty - 70)], CYAN_DIM, 1, 200)
    b.line([(tx + 16, ty + 4), (W - 4, ty - 62)], CYAN_DIM, 1, 200)
    b.label(W - 10, 164, "MASS-DRIVER TERMINAL", AMBER, right=True)
    b.label(W - 10, 9, "LOSE ALL FIVE: MISSION OVER", RED, right=True)
    hx, hy = 470, 52
    b.bracket(hx - 28, hy - 30, hx + 28, hy + 30, CYAN)
    b.sprite(asset("airstrike-bomber_0"), hx, hy)
    b.label(hx + 36, hy - 14, "HAMMER FLIGHT", CYAN)
    b.label(hx + 36, hy - 2, "AIRSTRIKE - RELEASED", GREEN)
    b.label(hx + 36, hy + 10, "TO AEGIS WING", CYAN_DIM)
    return b


def walker_scan():
    """L04 p2: Varga's scan of the Scuttler: it faces where it walks, the claws stop rounds, the
    back glows."""
    b = Board()
    b.title("SENSOR SCAN - VRELL WALKER")
    x, y = 200, 122
    frame = 12 * 6
    b.glow(x, y, 70, (10, 40, 50), 0.6)
    walker = asset(f"scuttler_{frame}", 2)
    glow = asset(f"scuttler-glow_{frame}", 2)
    w = np.array(walker).astype(np.float64)
    g = np.array(glow).astype(np.float64)
    w[..., :3] = np.clip(w[..., :3] + g[..., :3], 0, 255)
    walker = artkit.quantize_set([Image.fromarray(w.astype(np.uint8))], 48)[0]
    for a0, a1, colour, alpha in ((-45, 45, RED, 170), (135, 225, GREEN, 150)):
        b.draw.pieslice([x - 90, y - 90, x + 90, y + 90], a0, a1, outline=colour + (alpha,))
    b.sprite(walker, x, y)
    b.arrow(x + 96, y, x + 150, y, CYAN, dashed=False)
    b.label(x + 100, y + 10, "WALKS", CYAN)
    b.label(x + 100, y + 20, "THIS WAY", CYAN)
    b.label(x + 40, 22, "CLAWS FORWARD", RED)
    b.label(x + 40, 32, "STOP YOUR ROUNDS", RED)
    b.label(x - 150, 200, "BACK GLOWS", GREEN)
    b.label(x - 150, 210, "AIM HERE", GREEN)
    b.label(10, 22, "SCUTTLER", WHITE)
    b.label(10, 32, "TURNS TO FACE", CYAN_DIM)
    b.label(10, 42, "WHERE IT GOES", CYAN_DIM)
    px = 560
    b.label(px - 140, 40, "BE PATIENT", AMBER)
    b.dashed([(px - 130, 150), (px - 60, 150), (px, 110), (px, 74)], RED, 1)
    b.sprite(asset("scuttler_48"), px, 52)
    b.sprite(asset("ship_2"), px, 196)
    b.dashed([(px, 170), (px, 86)], CYAN, 1, 3, 3)
    b.label(px - 140, 56, "LET IT TURN AWAY", CYAN)
    b.label(px + 30, 160, "GET BEHIND IT", CYAN)
    b.label(px + 30, 170, "THEN FIRE", GREEN)
    b.label(W - 10, 9, "GROUND LAYER", CYAN_DIM, right=True)
    return b


def crater_nest():
    """L05 p1: the nest crater beside the mass-driver line, batteries A-D on its floor, Lancer's
    run up the rail and over the rim, the sleds on automatic."""
    b = Board()
    b.planet(336, 760, 560, "moon", seed=51)
    b.title("MASS-DRIVER LINE - VRELL NEST CRATER")
    cx, cy, rx, ry = 400, 128, 170, 84
    b.glow(cx, cy, 120, (30, 14, 50), 0.9)
    b.ring(cx, cy, rx, ry, CYAN, 220, 2)
    b.ring(cx, cy, rx - 10, ry - 7, CYAN_DIM, 140)
    b.label(cx + rx - 4, cy - 50, "CRATER RIM", CYAN_DIM)
    rng = np.random.default_rng(55)
    for _ in range(9):
        a, r = rng.uniform(0, 2 * np.pi), rng.uniform(0.15, 0.7)
        x, y = cx + np.cos(a) * rx * r, cy + np.sin(a) * ry * r
        b.glow(x, y, 10, (50, 20, 80), 0.7)
        b.marker(x, y, "dot", VIOLET, 1)
    b.label(cx - 12, cy + 8, "NEST", VIOLET)
    for name, (x, y) in zip("ABCD", ((280, 104), (350, 170), (450, 92), (520, 160))):
        b.glow(x, y, 14, (90, 60, 10), 0.8)
        b.marker(x, y, "diamond", AMBER, 5)
        for k in (-1, 1):
            b.marker(x + 12 * k, y + 8, "chevron", RED, 3)
        b.label(x + 10, y - 16, f"BATTERY {name}", AMBER)
    ry0 = 30
    b.line([(20, ry0), (650, ry0)], CYAN, 2)
    b.line([(20, ry0 + 5), (650, ry0 + 5)], CYAN_DIM, 1, 140)
    for x in range(40, 650, 30):
        b.marker(x, ry0 + 2, "dot", AMBER, 1)
    b.label(W - 10, 9, "MASS DRIVER: AUTOMATIC, SLEDS EVERY 5 S", AMBER, right=True)
    b.label(W - 10, H - 40, "STAY OFF THE RAIL", RED, right=True)
    b.arrow(30, 52, 200, 52, CYAN, dashed=False)
    b.dashed([(200, 52), (236, 92), (300, 132), (420, 132), (560, 128), (630, 128)], CYAN, 1)
    b.label(30, 60, "LANCER - DOWN THE RAIL, OVER THE RIM", CYAN)
    b.label(10, H - 28, "DESTROY ALL FOUR BATTERIES", AMBER)
    b.label(10, H - 16, "A BATTERY LEFT ALIVE: MISSION FAILED", RED)
    b.label(W - 10, H - 16, "NO SECOND RUN TODAY", WHITE, right=True)
    return b


def mortar_scan():
    """L05 p2: Varga's scan of the Polyp Mortar: the lob arcing to its lime marker a second ahead,
    the ship moving out of it; something in orbit watching the crater."""
    b = Board()
    b.title("SENSOR SCAN - VRELL ACID-THROWER")
    x, y = 90, 150
    b.glow(x, y, 54, (10, 40, 50), 0.6)
    b.bracket(x - 44, y - 44, x + 44, y + 44)
    b.sprite(asset("polyp-mortar_0", 2), x, y)
    b.label(x - 44, y + 52, "POLYP MORTAR", WHITE)
    b.label(x - 44, y + 64, "GROUND LAYER", CYAN_DIM)
    tx, ty = 330, 132
    arc = [(x + (tx - x) * t, y + (ty - y) * t - np.sin(np.pi * t) * 96) for t in np.linspace(0.08, 1, 24)]
    b.dashed(arc, GREEN, 1, 4, 4)
    for t in (0.35, 0.7):
        b.sprite(asset("mortar-blob_0"), *arc[int(t * 23)])
    b.glow(tx, ty, 26, (40, 70, 10), 0.8)
    b.sprite(asset("mortar-marker"), tx, ty)
    b.label(tx - 60, ty + 30, "MARKED 1 S AHEAD", GREEN)
    b.label(tx - 60, ty + 42, "THEN A RING OF ACID", RED)
    ship = asset("ship_2")
    b.sprite(ship, tx + 10, ty + 2)
    b.arrow(tx + 26, ty + 2, tx + 92, ty + 2, CYAN, dashed=False)
    b.label(tx + 30, ty - 12, "KEEP MOVING", CYAN)
    rx, ry, r = 572, 112, 72
    for k in (1, 2, 3):
        b.ring(rx, ry, r * k / 3, colour=CYAN_DIM, alpha=110)
    b.ring(rx, ry, 10, colour=AMBER, alpha=200)
    b.label(rx - 30, ry + 16, "CRATER", AMBER)
    b.arc(rx, ry, r * 0.8, r * 0.8, 200, 340, VIOLET, 220)
    b.glow(rx + 30, ry - 52, 16, (60, 30, 110), 0.9)
    b.marker(rx + 30, ry - 52, "square", RED, 4)
    b.label(rx - r, ry + r + 8, "ORBIT: UNKNOWN CONTACT", RED)
    b.label(rx - r, ry + r + 20, "HOLDING OVER THE CRATER", WHITE)
    b.label(W - 10, 9, "DON'T BE THERE", AMBER, right=True)
    return b


def daedalus_rim():
    """L06 p1: the far side across the terminator, three settlements silent, Daedalus Rim the
    largest with its lights still on, Lancer's run into the dark by headlight."""
    b = Board()
    tx = 190 + 10 * np.sin(b.yy / 30)
    dark = np.clip((b.xx - tx) / 40 + 0.5, 0, 1)
    b.base *= (1 - 0.55 * dark)[..., None]
    b.glow(-60, 120, 170, (70, 64, 50), 1.0)
    b.title("LUNA FAR SIDE - DAEDALUS RIM")
    rng = np.random.default_rng(66)
    for x, y, r in ((60, 70, 22), (120, 170, 30), (170, 92, 16), (40, 200, 12), (150, 40, 10)):
        b.arc(x, y, r, r * 0.8, 140, 320, AMBER, 170)
        b.arc(x, y, r, r * 0.8, -40, 140, GREY, 90)
    for x, y in zip(rng.uniform(230, 650, 14), rng.uniform(30, 220, 14)):
        r = rng.uniform(6, 16)
        b.ring(x, y, r, r * 0.8, GREY, 70)
    b.dashed([(190 + 10 * np.sin(y / 30), y) for y in range(20, H - 16, 6)], AMBER, 1, 4, 4)
    b.label(14, 24, "SUNLIT", AMBER)
    b.label(206, 24, "TERMINATOR", AMBER)
    b.label(206, H - 16, "FULL DARK", CYAN_DIM)
    b.line([(230, 214), (420, 160), (480, 138), (560, 92), (660, 60)], CYAN_DIM, 1, 160)
    b.label(300, 206, "ORE RAIL", CYAN_DIM)
    for x, y, name in ((330, 60, "SETTLEMENT 2"), (600, 70, "SETTLEMENT 3")):
        b.marker(x, y, "square", GREY)
        b.marker(x, y, "cross", RED, 6)
        b.label(x - 30, y + 12, name, GREY)
        b.label(x - 30, y + 22, "SILENT", RED)
    dx, dy = 480, 118
    dome = Image.open(ROOT / "assets" / "backdrop" / "level-06" / "dome-a.png").convert("RGBA")
    dome = np.array(dome.resize((dome.width // 2, dome.height // 2), Image.LANCZOS))
    dome[..., 3] = np.where(dome[..., 3] > 128, 255, 0)
    dome = artkit.quantize_set([Image.fromarray(dome)], 32)[0]
    for ox, oy in ((-22, -6), (14, 10), (24, -14)):
        b.glow(dx + ox, dy + oy, 14, (90, 70, 20), 0.9)
    b.sprite(dome, dx, dy)
    b.bracket(dx - 52, dy - 44, dx + 52, dy + 44, AMBER)
    b.label(dx - 52, dy + 50, "DAEDALUS RIM", AMBER)
    b.label(dx - 52, dy + 60, "2000 PEOPLE - MINERS, FAMILIES", WHITE)
    b.label(dx - 52, dy + 70, "LIGHTS ON - SILENT 30 H", RED)
    sx, sy = 120, 128
    b.sprite(asset("ship_2").transpose(Image.ROTATE_270), sx, sy)
    cone = Image.open(SPRITES / "headlight-cone.png").convert("RGBA").transpose(Image.ROTATE_270)
    cone = np.array(cone.resize((cone.width // 2, cone.height // 2), Image.LANCZOS))[..., 3] / 255
    x0, y0 = sx + 20, sy - cone.shape[0] // 2
    b.base[y0:y0 + cone.shape[0], x0:x0 + cone.shape[1]] += cone[..., None] * np.array([30, 90, 110])
    b.arrow(sx + 26, sy, dx - 60, dy + 4, CYAN)
    b.label(sx - 60, sy + 28, "LANCER - BY HEADLIGHT", CYAN)
    b.label(W - 10, 9, "3 SETTLEMENTS SILENT", RED, right=True)
    b.label(W - 10, 21, "NO DISTRESS CALL - NO WRECKAGE", WHITE, right=True)
    b.label(10, H - 16, "FIND OUT WHAT HAPPENED", AMBER)
    return b


def wyrm_path(points, spacing, sizes):
    """The chain's members along ``points`` (the head first), each ``spacing`` x the mean length of
    two neighbours behind the one before it, as (x, y, direction of travel)."""
    pts = np.array(points, float)
    seg = np.hypot(*(pts[1:] - pts[:-1]).T)
    run = np.concatenate([[0], np.cumsum(seg)])
    out, s = [], 0.0
    for i, size in enumerate(sizes):
        if i:
            s += spacing * (sizes[i - 1] + size) / 2
        k = min(np.searchsorted(run, s, side="right") - 1, len(seg) - 1)
        t = (s - run[k]) / seg[k]
        p = pts[k] + (pts[k + 1] - pts[k]) * t
        travel = pts[k] - pts[k + 1]  # the head leads, so the chain travels toward the path's start
        out.append((p[0], p[1], travel))
    return out


def path_beyond(points, s):
    """The part of the path ``points`` beyond the arc length ``s`` from its start: the head's path
    history behind the tail, the way the chain came round (its own stretch the overlapping members
    hide)."""
    pts = np.array(points, float)
    run = np.concatenate([[0], np.cumsum(np.hypot(*(pts[1:] - pts[:-1]).T))])
    k = min(np.searchsorted(run, s, side="right") - 1, len(pts) - 2)
    p = pts[k] + (pts[k + 1] - pts[k]) * (s - run[k]) / (run[k + 1] - run[k])
    return [tuple(p)] + [tuple(q) for q in pts[k + 1:]]


def heading(travel, count=48):
    """The production heading index of a direction of travel (clockwise from straight down)."""
    deg = np.degrees(np.arctan2(-travel[0], travel[1])) % 360
    return int(round(deg / (360 / count))) % count


def mantis_sweep(b, eye, target, reach=240, at=0.3):
    """The Mantis's telegraph and beam in the production look (round 23 variant b, the charged
    lance: tools/art/mantis_beam.py) from its eye on the left edge: the telegraph wedge at the
    data's arc, centred on the bearing to ``target`` (clamped inward..down, as the game does), and
    the beam ``at`` its way through the sweep with its tip spark, the eye ring over the sprite.
    ``reach`` is the image's beam length (the game's 300 px does not fit the left half)."""
    ex, ey = eye
    bearing = np.arctan2(-(target[0] - ex), target[1] - ey)  # game heading: clockwise from straight down
    centre = np.clip(bearing, -np.pi / 2, 0)
    half = np.radians(mantis_beam.SWEEP["arc"]) / 2
    wedge = artkit.quantize_set([artkit.additive(mantis_beam.r23.wedge_b(int(mantis_beam.SWEEP["arc"]), reach))],
                                LIGHT_COLOURS)[0]
    rgb, c = mantis_beam.rotated(wedge, mantis_beam.r23.heading_to_img(centre), (0, wedge.height // 2))
    b.add_light(rgb, ex - c, ey - c)
    a = mantis_beam.r23.heading_to_img(centre - half + 2 * half * at)
    strip = mantis_beam.lay_beam(artkit.load_frames("mantis-beam")[0], reach)
    rgb, c = mantis_beam.rotated(strip, a, (0, strip.height // 2))
    b.add_light(rgb, ex - c, ey - c, "over")
    for img, x, y in ((artkit.load_frames("mantis-beam-tip")[0], ex + np.cos(a) * reach, ey + np.sin(a) * reach),
                      (artkit.load_frames("mantis-beam-eye")[0], ex, ey)):
        b.add_light(np.array(img.convert("RGB")), x - img.width / 2, y - img.height / 2, "over")


def edge_scan():
    """L06 p2: Varga's two new contacts: the Mantis at the edge sweeping its beam across from its
    eye, side guns reaching it; the Coilwyrm coming round behind the ship, whose rear is open."""
    b = Board()
    b.title("SENSOR SCAN - TWO NEW CONTACTS")
    ex = 22
    b.dashed([(ex, 24), (ex, H - 20)], CYAN_DIM, 1, 4, 4)
    b.label(ex + 4, H - 16, "SCREEN EDGE", CYAN_DIM)
    mx, my = 62, 104
    sx, sy = 236, 188
    mantis = asset("mantis_8")  # the left edge's sweep pose, where the data's sweep.origin is measured
    b.glow(mx, my, 50, (10, 40, 50), 0.6)
    mantis_sweep(b, (mx + mantis_beam.ORIGIN[0], my + mantis_beam.ORIGIN[1]), (sx, sy))
    b.sprite(mantis, mx, my)
    b.bracket(mx - 42, my - 42, mx + 42, my + 42)
    b.label(110, 24, "MANTIS", WHITE)
    b.label(110, 34, "SNIPER AT THE EDGES", RED)
    b.label(200, 108, "ITS BEAM SWEEPS", RED)
    b.label(200, 118, "ACROSS YOU", RED)
    b.sprite(asset("ship_2"), sx, sy)
    b.arrow(sx - 24, sy - 2, mx + 40, sy - 2, GREEN, dashed=False)
    b.label(40, sy + 14, "GUNS THAT FIRE SIDEWAYS", GREEN)
    b.line([(326, 30), (326, H - 20)], CYAN_DIM, 1, 90)
    spec = yaml.safe_load((DESIGN / "enemies" / "air" / "coilwyrm" / "data.yaml").read_text())
    spacing = spec["segment_chain"]["spacing"]
    pivots = json.loads((ROOT / "assets" / "pivots" / "coilwyrm.json").read_text())
    sizes = [58] + pivots["segment_sizes"] + [40]
    path = [(420, 125)]
    for (cx, cy), a0 in (((460, 175), np.pi), ((600, 165), np.pi / 2), ((600, 70), 0)):
        path += [(cx + 40 * np.cos(a), cy + 40 * np.sin(a)) for a in np.linspace(a0, a0 - np.pi / 2, 10)]
    path += [(500, 30)]
    members = wyrm_path(path, spacing, sizes)
    b.dashed(path_beyond(path, spacing * sum((u + v) / 2 for u, v in zip(sizes, sizes[1:])))[::-1],
             VIOLET, 1, 3, 5)
    for i in range(len(members) - 1, -1, -1):
        x, y, travel = members[i]
        k = heading(travel)
        if i == 0:
            name = f"coilwyrm_{k * 4 + 2}"
        elif i == len(members) - 1:
            name = f"coilwyrm-tail_{k}"
        else:
            name = f"coilwyrm-segment_{k * 12 + i - 1}"
        b.sprite(asset(name), x, y)
    px, py = 420, 66
    b.draw.pieslice([px - 46, py - 46, px + 46, py + 46], 50, 130, fill=RED + (40,), outline=RED + (190,))
    b.sprite(asset("ship_2"), px, py)
    b.label(474, 100, "YOUR REAR", RED)
    b.label(474, 110, "IS OPEN", RED)
    b.arrow(px - 26, py + 4, 346, py + 4, CYAN, dashed=False)
    b.label(342, py + 14, "MOVE", CYAN)
    b.label(342, 24, "COILWYRM", WHITE)
    b.label(342, 34, "COMES ROUND BEHIND YOU", VIOLET)
    b.label(474, 140, "DON'T FIGHT", AMBER)
    b.label(474, 150, "IT THERE", AMBER)
    return b


# --------------------------------------------------------------------------- Level 07 and the Act 1 outro

_CARRIER = {}


def carrier_art():
    """The Brood Carrier's production sprites (brood_carrier.py's set), loaded once."""
    if not _CARRIER:
        _CARRIER.update(bc.load_art())
    return _CARRIER


def scaled(img, scale, colours=48):
    """A sprite reduced for the display: smooth, its edge cut at half alpha, on a palette of its own."""
    small = np.array(img.resize((max(1, round(img.width * scale)), max(1, round(img.height * scale))),
                                Image.LANCZOS))
    small[..., 3] = np.where(small[..., 3] > 128, 255, 0)
    return artkit.quantize_set([Image.fromarray(small)], colours)[0]


def carrier(frame, scale, sacs=None, iris=0, turret_to=-np.pi / 2):
    """The carrier as the game composes it (brood_carrier.compose, on a clear canvas): hull ``frame``
    (0 nose-down, 8 broadside) and at the two poses the sac sprites (``sacs``: a stage per bay in
    BAYS order, 0 closed, 3 open, 4 burst), the iris stage and the mandible turret aimed at
    ``turret_to`` (radians counter-clockwise, y up); reduced by ``scale``. Returns the sprite and the
    pose's part offsets at that scale in image coordinates (dx right, dy down)."""
    art = carrier_art()
    hull = art["hull"][frame]
    img = Image.new("RGBA", hull.size, (0, 0, 0, 0))
    img.alpha_composite(hull)
    cx, cy = hull.width // 2, hull.height // 2
    offs = bc.offsets(0 if frame == 0 else 1)
    if frame in (0, len(art["hull"]) - 1):
        sac = art["sac-down" if frame == 0 else "sac-side"]
        parts = [(sac[0 if sacs is None else sacs[i]], offs[name]) for i, name in enumerate(bc.BAYS)]
        parts += [(art["iris"][iris], offs["core"]), (art["turret"][bc.heading_frame(turret_to)], offs["mandibles"])]
        for f, (dx, dy) in parts:
            img.alpha_composite(f, (cx + dx - f.width // 2, cy - dy - f.height // 2))
    return scaled(img, scale), {k: (dx * scale, -dy * scale) for k, (dx, dy) in offs.items()}


def shadow(b, img, x, y, dx, dy, strength=0.55):
    """A high-air unit's shadow on the display: ``img``'s alpha (centred at x, y) offset by dx, dy."""
    a = np.array(img)[..., 3] / 255
    x0, y0 = int(x - img.width / 2 + dx), int(y - img.height / 2 + dy)
    ys, xs = slice(max(0, y0), min(H, y0 + a.shape[0])), slice(max(0, x0), min(W, x0 + a.shape[1]))
    b.base[ys, xs] *= (1 - strength * a[ys.start - y0:ys.stop - y0, xs.start - x0:xs.stop - x0])[..., None]


def core_glow(b, x, y, scale, strength=1.0):
    """The core's additive lime glow (brood-carrier-core-glow) over the sprites at (x, y); smaller,
    the glow the display puts on an open sac."""
    g = carrier_art()["glow"]
    g = np.array(g.resize((round(g.width * scale * 1.6), round(g.height * scale * 1.6)), Image.LANCZOS)).astype(float)
    rgb = g[..., :3] * g[..., 3:4] / 255 * strength
    b.add_light(rgb, x - rgb.shape[1] / 2, y - rgb.shape[0] / 2, "over")


def backdrop(level, name, scale, colours=32):
    """A backdrop piece of a level (assets/backdrop/<level>/), reduced for the display."""
    return scaled(Image.open(ROOT / "assets" / "backdrop" / level / f"{name}.png").convert("RGBA"), scale, colours)


def cloud(b, level, name, scale, x, y):
    """A translucent backdrop piece (an ichor cloud) blended into the display layer, centred at x, y,
    so it takes the display's palette."""
    img = Image.open(ROOT / "assets" / "backdrop" / level / f"{name}.png").convert("RGBA")
    a = np.array(img.resize((round(img.width * scale), round(img.height * scale)), Image.LANCZOS)).astype(float)
    x0, y0 = int(x - a.shape[1] / 2), int(y - a.shape[0] / 2)
    ys, xs = slice(max(0, y0), min(H, y0 + a.shape[0])), slice(max(0, x0), min(W, x0 + a.shape[1]))
    a = a[ys.start - y0:ys.stop - y0, xs.start - x0:xs.stop - x0]
    k = a[..., 3:4] / 255
    b.base[ys, xs] = b.base[ys, xs] * (1 - k) + a[..., :3] * k


def l1_carrier():
    """L07 p1: the carrier holding at the Earth-Moon L1 point where this started, the pods that hit
    Luna traced back to it, its escort screen, the overrun picket, Aegis's way through to it."""
    b = Board()
    b.planet(34, 150, 92, "earth", seed=19)
    b.planet(626, 64, 22, "moon", seed=5)
    b.title("EARTH-MOON L1 - WHERE THIS STARTED")
    cx, cy = 480, 132
    b.glow(cx, cy, 100, (16, 46, 34), 0.9)
    ship, offs = carrier(8, 0.3, turret_to=np.pi)
    for k, (dy0, bend) in enumerate(((-14, -26), (0, -12), (14, 4))):
        tail = (cx + 64, cy + dy0 * 0.5)
        pts = [(tail[0] + (604 - tail[0]) * t, tail[1] + (76 + dy0 * 0.4 - tail[1]) * t + bend * np.sin(np.pi * t))
               for t in np.linspace(0, 1, 12)]
        b.dashed(pts, VIOLET, 1, 3, 4)
    b.label(596, 38, "EVERY POD ON LUNA", VIOLET, right=True)
    b.label(596, 48, "TRACED BACK HERE", VIOLET, right=True)
    b.label(W - 10, 92, "LUNA", CYAN_DIM, right=True)
    b.sprite(ship, cx, cy)
    b.bracket(cx - 102, cy - 50, cx + 102, cy + 50, RED)
    b.label(cx - 84, cy - 68, "BROOD CARRIER", WHITE)
    b.label(cx - 84, cy - 58, "THE SIZE OF A STATION", RED)
    b.label(cx - 84, cy + 56, "WEAK POINTS:", LIME)
    b.label(cx - 84, cy + 66, "LAUNCH SACS, CORE UNDER ARMOUR", LIME)
    for k, a in enumerate(np.linspace(-50, 50, 9)):
        t = np.radians(a)
        x, y = cx - 150 * np.cos(t), cy + 96 * np.sin(t)
        b.glow(x, y, 9, (60, 24, 40), 0.6)
        b.marker(x, y, "chevron" if k % 3 else "dot", RED if k % 3 else VIOLET, 4 if k % 3 else 3)
    b.arc(cx, cy, 150, 96, 130, 230, RED, 120)
    b.label(300, 30, "ESCORT SCREEN", RED)
    for y in (48, 92, 138, 184):
        x = 206 + 8 * np.sin(y / 20)
        b.marker(x, y, "square", GREY)
        b.marker(x, y, "cross", RED, 6)
    b.label(150, 206, "L1 PICKET - OVERRUN", GREY)
    b.arrow(118, 150, 296, 136, CYAN)
    b.label(110, 160, "AEGIS - PUNCH THROUGH", CYAN)
    b.label(W - 10, 9, "WHEN IT DIES, THE PODS STOP", AMBER, right=True)
    b.label(10, H - 16, "DESTROY THE CARRIER", AMBER)
    return b


def overhead_scan():
    """L07 p2: Varga's scan of the carrier's three stages (user decision D1 of part G): it comes over
    nose-down on high air (its shadow on the ship's level) and stops, the bays opening in pairs, only
    missiles reaching up; it descends and turns 90 degrees in place; broadside at the ship's level
    the sacs and the plate iris over the lime core."""
    b = Board()
    b.title("SENSOR SCAN - BROOD CARRIER")
    x1, y1, s1 = 84, 120, 0.3
    high, offs = carrier(0, s1, sacs=[3, 3] + [0] * 6)
    shadow(b, high, x1, y1, 16, 22)
    b.glow(x1, y1 + 30, 50, (30, 60, 10), 0.5)
    b.sprite(high, x1, y1)
    for name in ("bay 1 left", "bay 1 right"):
        core_glow(b, x1 + offs[name][0], y1 + offs[name][1], s1 * 0.6, 0.8)
    b.label(150, 24, "1 HIGH AIR", WHITE)
    b.label(150, 34, "ABOVE YOU", WHITE)
    b.label(150, 46, "NOSE FIRST, STOPS", CYAN_DIM)
    b.label(150, 58, "BAYS OPEN IN PAIRS", LIME)
    b.arrow(20, 34, 20, 100, CYAN_DIM)
    sx, sy = 172, 204
    b.sprite(asset("ship_2"), sx, sy)
    tx, ty = x1 + offs["bay 1 right"][0] + 4, y1 + offs["bay 1 right"][1]
    for bend in (60, -10):  # two homing missiles curving up to the open sac
        pts = [(sx + (tx - sx) * t + (1 - t) * t * bend, sy - 20 + (ty - sy + 20) * t) for t in np.linspace(0, 1, 14)]
        b.dashed(pts, GREEN, 1, 4, 3)
    b.label(150, 84, "ONLY MISSILES", GREEN)
    b.label(150, 94, "REACH THAT HIGH", GREEN)
    b.dashed([(sx + 12, sy - 24), (sx + 12, sy - 60)], GREY, 1, 3, 3)
    b.marker(sx + 12, sy - 64, "cross", GREY, 3)
    b.label(sx + 20, sy - 70, "GUNS: NO", GREY)
    b.line([(266, 30), (266, H - 20)], CYAN_DIM, 1, 90)
    b.line([(394, 30), (394, H - 20)], CYAN_DIM, 1, 90)
    tx2, ty2 = 330, 128
    b.sprite(holo(carrier(4, 0.15)[0], CYAN_DIM, 8), tx2, ty2)
    b.arc(tx2, ty2, 54, 54, 200, 290, CYAN, 220)
    b.arrow(tx2 + 54 * np.cos(np.radians(285)), ty2 + 54 * np.sin(np.radians(285)),
            tx2 + 54 * np.cos(np.radians(300)), ty2 + 54 * np.sin(np.radians(300)), CYAN, 5, dashed=False)
    b.label(274, 24, "2 COMES DOWN", WHITE)
    b.label(274, 34, "TURNS 90 DEG", CYAN_DIM)
    b.label(274, 44, "IN PLACE", CYAN_DIM)
    b.label(274, 198, "NO DAMAGE", RED)
    b.label(274, 208, "WHILE IT TURNS", RED)
    x3, y3, s3 = 530, 110, 0.4
    sx3, sy3 = 512, 210
    tx, ty = x3 + 285 * s3, y3  # the mandibles, broadside
    side, offs3 = carrier(8, s3, sacs=[4, 4, 3, 3, 0, 0, 0, 0], iris=4,
                          turret_to=np.arctan2(-(sy3 - ty), sx3 - tx))
    b.glow(x3, y3, 80, (16, 46, 34), 0.8)
    b.sprite(side, x3, y3)
    cxg, cyg = x3 + offs3["core"][0], y3 + offs3["core"][1]
    core_glow(b, cxg, cyg, s3)
    b.label(402, 24, "3 BROADSIDE, YOUR LEVEL", WHITE)
    b.label(402, 34, "EVERY GUN REACHES IT", CYAN)
    for name in ("bay 2 left", "bay 2 right"):
        core_glow(b, x3 + offs3[name][0], y3 + offs3[name][1], s3 * 0.6, 0.8)
    b.line([(x3 + offs3["bay 2 right"][0] + 8, y3 + offs3["bay 2 right"][1] + 8), (600, 184)], LIME, 1, 200)
    b.label(W - 10, 188, "HIT THE LIME GLOW", LIME, right=True)
    b.label(W - 10, 198, "SACS, THEN THE CORE", LIME, right=True)
    b.line([(cxg - 8, cyg + 10), (440, 184)], VIOLET, 1, 200)
    b.label(402, 188, "PLATE IRIS", VIOLET)
    b.label(402, 198, "OVER THE CORE", VIOLET)
    b.sprite(asset("ship_2"), sx3, sy3)
    for dx in (-6, 6):
        b.dashed([(sx3 + dx, sy3 - 24), (sx3 + dx, cyg + 50)], CYAN, 1, 4, 4)
    b.label(W - 10, 9, "NO MISSILES? SURVIVE UNTIL IT COMES DOWN", AMBER, right=True)
    return b


def outro_carcass():
    """Act 1 outro p1: the carrier's carcass drifting apart at L1 (its production break-up chunks,
    dead and burnt dark, and ichor clouds), Earth beyond; what the week saved."""
    b = Board()
    b.planet(500, 330, 230, "earth", seed=23)
    b.planet(54, 58, 16, "moon", seed=5)
    b.title("EARTH-MOON L1 - THE CARRIER, AFTER")
    cx, cy, s = 250, 112, 0.34
    death = json.loads((ROOT / "assets" / "pivots" / "brood-carrier.json").read_text())["death"]
    darken = death["darken"]
    b.glow(cx, cy, 120, (14, 30, 26), 0.8)
    for k, name in enumerate(("ichor-c", "ichor-a", "ichor-b")):
        cloud(b, "level-07", name, 0.6, cx - 120 + 120 * k, cy + (30, -40, 44)[k])
    rng = np.random.default_rng(71)
    chunks = death["chunks"]
    mx, my = (np.mean([c["drift"][i] for c in chunks]) for i in (0, 1))
    for chunk in chunks:
        # the last tumble frame (the cut flesh cooled to dead tissue, nothing glows) at its centre
        # moved by its drift less the carcass's common drift, the pieces coming apart round the
        # bracket's middle (wider than the data across, as far as the data up and down, 4 px high:
        # clear of the title and the labels); offsets and drift are dy up, the display's y down
        (ox, oy), (dx, dy) = chunk["offsets"][2], chunk["drift"]
        rng.integers(0, 3)  # the frame draw of the approved image, kept so the debris dots stay put
        a = np.array(artkit.load_frames(chunk["sprite"])[2]).astype(float)
        a[..., :3] *= darken
        piece = scaled(Image.fromarray(a.astype(np.uint8)), s, 24)
        b.sprite(piece, cx + (ox + 1.6 * (dx - mx)) * s, cy - 4 - (oy + dy - my) * s)
    for x, y in rng.uniform((cx - 150, cy - 70), (cx + 150, cy + 70), (14, 2)):
        b.marker(x, y, "dot", GREY, 1)
    b.bracket(cx - 160, cy - 76, cx + 160, cy + 82, GREY)
    b.label(cx - 160, cy + 88, "BROOD CARRIER - DEAD, DRIFTING", GREY)
    b.label(cx - 160, cy + 98, "NO LAUNCHES SINCE THE KILL", GREEN)
    b.label(W - 10, 9, "A WEEK AGO, NONE OF THIS WAS CERTAIN", AMBER, right=True)
    for i, (text, colour) in enumerate((("GAGARIN YARDS - SCARRED, STANDING", GREEN),
                                        ("TRANQUILITY CONVOY - HOME", GREEN),
                                        ("LUNA - NOTHING FALLING", GREEN))):
        b.label(W - 10, 30 + 10 * i, text, colour, right=True)
    b.label(40, 80, "LUNA", CYAN_DIM)
    b.label(W - 10, H - 16, "EARTH", CYAN_DIM, right=True)
    b.label(10, H - 16, "WELL FLOWN, AEGIS", AMBER)
    return b


def outro_daedalus():
    """Act 1 outro p2: Daedalus Rim on the far side, still empty: the main airlock open and lit, the
    domes' lights on (Level 06's backdrop pieces), the file on the missing kept open."""
    b = Board()
    b.base *= 0.6
    b.title("LUNA FAR SIDE - DAEDALUS RIM, SEARCHED")
    gx, gy, s = 214, 118, 0.6
    for x, y, r in ((gx, gy - 28, 60), (gx - 92, gy - 62, 40), (gx + 92, gy - 62, 40), (gx, gy + 54, 46)):
        b.glow(x, y, r, (80, 62, 24), 0.8)
    rng = np.random.default_rng(67)
    for x, y in zip(rng.uniform(20, 420, 12), rng.uniform(40, 220, 12)):
        r = rng.uniform(5, 13)
        b.ring(x, y, r, r * 0.8, GREY, 60)
    b.sprite(backdrop("level-06", "daedalus-gate", s), gx, gy)
    for name, x, y in (("dome-a", 52, 178), ("dome-b", 382, 170), ("dome-c", 380, 62)):
        b.glow(x, y, 30, (80, 62, 24), 0.8)
        b.sprite(backdrop("level-06", name, 0.5), x, y)
    b.line([(gx + 14, gy - 14), (gx + 60, gy - 96)], AMBER, 1, 200)
    b.label(gx + 64, gy - 102, "MAIN AIRLOCK - OPEN", AMBER)
    b.line([(gx - 30, gy + 50), (gx - 92, gy + 92)], AMBER, 1, 200)
    b.label(gx - 196, gy + 94, "LIGHTS ON - NOBODY HOME", AMBER)
    px = 470
    b.line([(px - 16, 30), (px - 16, H - 20)], CYAN_DIM, 1, 90)
    b.label(px, 30, "SEARCH - FAR SIDE", WHITE)
    for i, (text, colour) in enumerate((("DAEDALUS RIM", CYAN), ("2000 PEOPLE - NOT FOUND", RED),
                                        ("SETTLEMENTS 2 AND 3", CYAN), ("2000 PEOPLE - NOT FOUND", RED))):
        b.label(px, 50 + 12 * i + 6 * (i > 1), text, colour)
    b.label(px, 118, "NO BODIES - NO WRECKAGE", WHITE)
    b.line([(px, 140), (W - 14, 140)], RED, 1, 160)
    b.label(px, 148, "MISSING: 4000", RED)
    b.draw.rectangle([px - 4, 178, px + 120, 198], outline=AMBER + (230,))
    b.label(px + 4, 184, "FILE: OPEN", AMBER)
    return b


def outro_second_fleet():
    """Act 1 outro p3: Varga's long-range plot: the second fleet beyond the Moon, ten times the
    destroyed carrier group, its track ending in Earth's atmosphere."""
    b = Board()
    b.planet(60, 146, 74, "earth", seed=19)
    b.ring(60, 146, 84, colour=CYAN, alpha=120)
    b.planet(400, 58, 16, "moon", seed=5)
    b.title("LONG RANGE - SECOND FLEET")
    lx, ly = 350, 84
    b.ring(lx, ly, 20, colour=GREY, alpha=180)
    b.marker(lx, ly, "cross", GREY, 5)
    b.label(lx - 60, ly - 36, "CARRIER GROUP", GREY)
    b.label(lx - 60, ly - 26, "DESTROYED", GREEN)
    fx, fy, rx, ry = 584, 134, 60, 66
    b.glow(fx, fy, 70, (60, 24, 90), 0.9)
    rng = np.random.default_rng(24)
    a, r = rng.uniform(0, 2 * np.pi, 70), np.sqrt(rng.uniform(0.02, 1, 70))
    for x, y in zip(fx + rx * r * np.cos(a), fy + ry * r * np.sin(a)):
        b.marker(x, y, "dot", VIOLET, 1)
    b.ring(fx, fy, rx + 6, ry + 6, RED, 200, 1)
    b.label(fx - rx - 4, fy + ry + 12, "SECOND FLEET", RED)
    b.label(fx - rx - 4, fy + ry + 22, "10X THE CARRIER GROUP", WHITE)
    b.label(W - 10, 9, "COURSE: EARTH'S ATMOSPHERE", RED, right=True)
    ex, ey = 60 + 84 * np.cos(np.radians(-10)), 146 + 84 * np.sin(np.radians(-10))
    p0, p1, p2 = np.array([fx - rx - 8, fy + 10.0]), np.array([330, 230.0]), np.array([ex + 4, ey + 6])
    track = [tuple((1 - t) ** 2 * p0 + 2 * (1 - t) * t * p1 + t ** 2 * p2) for t in np.linspace(0, 1, 11)]
    b.dashed(track[:-1], RED, 2)
    b.arrow(*track[-2], *track[-1], RED, dashed=False)
    b.marker(ex, ey, "diamond", AMBER, 5)
    b.label(ex + 16, ey - 30, "IT ENDS IN THE AIR", AMBER)
    b.label(ex + 16, ey - 20, "NOT IN ORBIT", AMBER)
    b.label(10, H - 16, "EARTH", CYAN_DIM)
    b.label(424, 54, "LUNA", CYAN_DIM)
    b.label(100, 222, "ATMOSPHERE", CYAN_DIM)
    b.label(260, H - 16, "THEY'RE COMING TO LAND", RED)
    return b


def outro_rook():
    """Act 1 outro p4: Aegis Wing reassigned to Earth defence; Rook's craft (Ember, the chosen round-09
    sprite from vfx_r08) in the wing slot beside and behind Lancer's Stormhawk, his radio portrait."""
    b = Board()
    b.planet(336, 520, 330, "earth", seed=23)
    b.title("AEGIS WING - REASSIGNED: EARTH DEFENCE")
    lx, ly = 250, 102
    rx, ry = 352, 136
    b.glow(lx, ly, 60, (10, 40, 60), 0.6)
    b.ring(rx, ry, 34, colour=CYAN_DIM, alpha=160)
    b.dashed([(lx + 30, ly + 14), (rx - 28, ry - 10)], CYAN_DIM, 1, 3, 3)
    b.sprite(asset("ship_2", 2), lx, ly)
    b.sprite(sprite.enlarge(v8.rook_sprite(0)[1], 2), rx, ry)
    b.label(lx - 18, ly + 52, "LANCER", CYAN)
    b.label(rx + 44, ry - 4, "ROOK - YOUR WING", AMBER)
    b.label(rx + 44, ry + 6, "FROM TOMORROW", AMBER)
    px, py = 584, 92
    b.sprite(Image.open(SPRITES / "portraits" / "radio-rook-neutral.png").convert("RGBA"), px, py)
    b.bracket(px - 42, py - 42, px + 42, py + 42)
    b.label(px - 42, py + 50, "LT. K. TANAKA", WHITE)
    b.label(px - 42, py + 60, "'ROOK'", CYAN)
    b.label(10, 30, "AEGIS WING", WHITE)
    b.label(10, 40, "OUT OF CISLUNAR SPACE", CYAN_DIM)
    b.label(10, 50, "EFFECTIVE NOW", AMBER)
    b.arrow(60, 66, 150, 186, CYAN)
    b.label(10, 210, "WE MEET THEM ON THE GROUND", RED)
    b.label(W - 10, 9, "KEEP HIM OUT OF TROUBLE", AMBER, right=True)
    return b


# --------------------------------------------------------------------------- Act 2 (M5 part B)

def fill(b, points, colour, alpha=1.0):
    """A polygon filled into the display layer (``b.base``), blended by ``alpha``."""
    mask = Image.new("L", (W, H), 0)
    ImageDraw.Draw(mask).polygon([tuple(p) for p in points], fill=255)
    m = np.array(mask).astype(np.float64)[..., None] / 255 * alpha
    b.base = b.base * (1 - m) + np.array(colour, float) * m


def creeper_lit(frame, zoom):
    """A Creeper frame with its gland and eye glow added as the game blends it, palettised."""
    body = np.array(asset(f"creeper_{frame}", zoom)).astype(np.float64)
    glow = np.array(asset(f"creeper-glow_{frame}", zoom)).astype(np.float64)
    body[..., :3] = np.clip(body[..., :3] + glow[..., :3], 0, 255)
    return artkit.quantize_set([Image.fromarray(body.astype(np.uint8))], 48)[0]


def rook_craft(zoom=1):
    """Rook's craft, the production Ember sprite (tools/art/rook.py), level."""
    return asset("rook_2", zoom)


def landfall():
    """Act 2 p1: the landers coming down at dawn, seen from below the cloud deck over the Gulf of
    Guinea: dozens of burning trails, the CDF tracking overlay counting them."""
    b = Board()
    hy = 196
    sea = b.yy > hy
    b.base = np.where(sea[..., None], np.dstack([4 + 0 * b.yy, 12 + 0 * b.yy, 24 + 10 * (b.yy - hy) / 44]), b.base)
    b.glow(470, hy, 150, (80, 40, 16), 0.9)                       # dawn under the deck, to the east
    deck = raster.fbm(W, H, 24, 811, octaves=4, period=False)
    band = np.exp(-((b.yy - 44) / 15) ** 2) * (0.45 + 0.8 * deck)
    b.base += band[..., None] * np.array([26, 46, 70])
    for y in range(hy + 6, H - 6, 7):                             # the swell, foreshortened
        b.dashed([(10, y), (466, y)], CYAN_DIM, 1, 14 + (y - hy), 6 + (y - hy) // 2)
    b.title("GULF OF GUINEA - 04:00, BELOW THE CLOUD DECK")
    b.line([(0, hy), (470, hy)], CYAN_DIM, 1, 200)
    b.dashed([(0, 64), (470, 64)], CYAN_DIM, 1, 3, 5)
    b.label(12, 68, "CLOUD DECK - 9 KM", CYAN_DIM)
    b.label(12, hy - 12, "SEA LEVEL", CYAN_DIM)
    rng = np.random.default_rng(2185)
    d = np.array([-0.42, 0.91])
    for k in range(34):
        hx = rng.uniform(70, 450)
        hy_ = rng.uniform(78, hy - 8) if k % 3 else rng.uniform(70, 120)
        head = np.array([hx, hy_])
        tail = head - d * (hy_ - 50) / d[1]
        mid = tail + (head - tail) * 0.55
        b.line([tuple(tail), tuple(mid)], RED, 1, 150)
        b.line([tuple(mid), tuple(head)], AMBER, 1, 230)
        b.glow(hx, hy_, 6, (140, 70, 20), 0.9)
        b.marker(hx, hy_, "dot", WHITE, 1)
        if k % 5 == 0:
            b.bracket(hx - 6, hy_ - 6, hx + 6, hy_ + 6, RED, 3)
            b.label(hx + 9, hy_ - 3, f"T{217 + 13 * k:03d}", RED)
    px = 494
    b.line([(px - 14, 26), (px - 14, H - 12)], CYAN_DIM, 1, 90)
    b.label(px, 30, "CDF TRACKING", WHITE)
    b.label(px, 44, "TRACKS IN VIEW: 34", AMBER)
    b.label(px, 56, "TRACKED: 12 DAYS", CYAN)
    b.label(px, 68, "STOPPED: 0", RED)
    b.line([(px, 86), (W - 12, 86)], CYAN_DIM, 1, 120)
    b.label(px, 94, "LANDFALL - 3 ZONES", WHITE)
    for i, zone in enumerate(("GULF OF GUINEA", "JAVA SEA", "RIVER PLATE")):
        b.marker(px + 4, 112 + 14 * i, "chevron", RED, 3)
        b.label(px + 14, 108 + 14 * i, zone, RED if i == 0 else VIOLET)
    b.label(px, 162, "THROUGH THE CLOUD DECK", CYAN_DIM)
    b.label(px, 174, "AT 04:00 LOCAL", CYAN_DIM)
    b.label(W - 10, 9, "THE SECOND FLEET - LANDFALL", RED, right=True)
    b.label(W - 10, H - 16, "WE COULD NOT STOP ONE", AMBER, right=True)
    return b


# Rough coastlines (lon, lat) for the CDF globe display, enough to read the continents.
CONTINENTS = [
    [(-168, 66), (-162, 70), (-140, 70), (-125, 70), (-110, 73), (-95, 72), (-85, 70), (-80, 73), (-65, 62),
     (-60, 55), (-56, 52), (-66, 45), (-70, 42), (-76, 38), (-76, 35), (-81, 31), (-80, 26), (-82, 25),
     (-83, 29), (-90, 30), (-97, 27), (-97, 22), (-92, 18), (-87, 21), (-88, 16), (-83, 10), (-78, 8),
     (-80, 7), (-85, 10), (-92, 14), (-105, 20), (-110, 24), (-112, 29), (-115, 30), (-117, 33), (-121, 35),
     (-124, 40), (-124, 46), (-128, 51), (-135, 57), (-142, 60), (-152, 59), (-158, 56), (-165, 54),
     (-162, 59), (-166, 62)],
    [(-73, 78), (-60, 82), (-30, 83), (-20, 80), (-20, 72), (-25, 68), (-40, 64), (-44, 60), (-50, 62),
     (-55, 68), (-60, 75)],
    [(-80, 9), (-75, 11), (-70, 12), (-62, 11), (-55, 6), (-50, 2), (-45, -2), (-38, -5), (-35, -8),
     (-39, -15), (-41, -22), (-48, -26), (-53, -33), (-57, -35), (-58, -38), (-62, -39), (-65, -42),
     (-66, -47), (-69, -51), (-68, -55), (-72, -54), (-75, -50), (-74, -42), (-72, -30), (-71, -20),
     (-76, -14), (-81, -6), (-80, -1), (-78, 2), (-79, 7)],
    [(-17, 21), (-16, 28), (-10, 32), (-6, 36), (10, 37), (11, 33), (20, 31), (25, 32), (32, 31), (35, 28),
     (39, 20), (43, 12), (51, 12), (46, 4), (41, -2), (40, -10), (40, -16), (35, -24), (32, -29), (27, -34),
     (20, -35), (18, -32), (15, -27), (12, -18), (13, -12), (9, -1), (9, 4), (5, 5), (-2, 5), (-8, 4),
     (-13, 8), (-17, 14)],
    [(-10, 36), (-9, 43), (-2, 44), (-5, 48), (-1, 49), (5, 53), (8, 57), (5, 62), (10, 64), (16, 69),
     (25, 71), (40, 68), (45, 68), (60, 69), (70, 73), (80, 73), (100, 77), (115, 74), (130, 72), (140, 72),
     (160, 70), (170, 70), (180, 68), (180, 65), (177, 62), (163, 60), (160, 55), (156, 51), (155, 57),
     (150, 59), (140, 54), (140, 48), (135, 43), (130, 42), (127, 38), (126, 35), (121, 40), (117, 39),
     (122, 37), (119, 32), (122, 30), (118, 24), (110, 21), (108, 16), (109, 12), (105, 9), (102, 13),
     (100, 13), (98, 8), (103, 1), (100, 3), (98, 10), (97, 17), (92, 22), (88, 22), (80, 15), (77, 8),
     (73, 17), (70, 22), (66, 25), (57, 25), (56, 27), (50, 30), (48, 29), (51, 25), (56, 24), (59, 22),
     (52, 16), (43, 13), (39, 20), (35, 28), (36, 36), (30, 36), (27, 38), (26, 41), (23, 40), (22, 37),
     (20, 40), (16, 38), (13, 41), (13, 45), (19, 42), (12, 46), (9, 44), (3, 43), (-4, 37)],
    [(-5, 50), (1, 51), (1, 53), (-2, 56), (-3, 59), (-6, 58), (-5, 55), (-3, 54)],
    [(95, 5), (98, 4), (104, -2), (106, -6), (102, -4), (96, 2)],
    [(109, 1), (111, 2), (117, 7), (119, 5), (118, 1), (116, -4), (110, -3)],
    [(105, -6), (110, -7), (114, -7), (114, -8), (106, -7)],
    [(131, -1), (141, -3), (150, -10), (142, -9), (138, -8), (132, -4)],
    [(114, -22), (114, -34), (118, -35), (124, -34), (132, -32), (138, -35), (140, -38), (147, -38),
     (150, -37), (153, -30), (153, -25), (146, -19), (142, -11), (141, -17), (136, -12), (130, -12),
     (126, -14), (122, -18)],
    [(130, 31), (135, 34), (140, 36), (142, 40), (141, 45), (140, 41), (136, 36), (131, 34)],
    [(44, -25), (47, -25), (50, -15), (49, -12), (44, -16)],
]
MAP_X, MAP_Y, MAP_K = 30, 26, 1.7      # lon -180..180, lat 80..-45 at 1.7 px a degree


def geo(lon, lat):
    return MAP_X + (lon + 180) * MAP_K, MAP_Y + (80 - lat) * MAP_K


def front_lines():
    """Act 2 p2: the CDF globe display (a flat projection) with the three landing zones glowing and
    the act's fronts: the cities, the Atlantic sea lanes, the Arctic relay chain."""
    b = Board()
    mask = Image.new("L", (W, H), 0)
    md = ImageDraw.Draw(mask)
    for poly in CONTINENTS:
        md.polygon([geo(*p) for p in poly], fill=255)
    land = np.array(mask) > 0
    tex = 0.55 + 0.5 * raster.fbm(W, H, 16, 905, octaves=4, period=False)
    b.base = np.where(land[..., None], raster.ramp(ui_scenes.HOLO, np.clip(tex, 0, 1)), b.base)
    zones = [(3, 3, "GULF OF GUINEA"), (110, -5, "JAVA SEA"), (-56, -35, "RIVER PLATE")]
    for lon, lat, _ in zones:
        b.glow(*geo(lon, lat), 16, (110, 30, 120), 1.1)
    b.title("CDF GLOBAL DISPLAY - THE FRONTS")
    for lon in range(-150, 180, 30):
        b.line([geo(lon, 80), geo(lon, -45)], CYAN_DIM, 1, 50)
    for lat in (60, 30, 0, -30):
        b.line([geo(-180, lat), geo(180, lat)], CYAN_DIM, 1, 50 if lat else 90)
    for poly in CONTINENTS:
        b.line([geo(*p) for p in poly + poly[:1]], CYAN_DIM, 1, 200)
    for i, (lon, lat, name) in enumerate(zones):
        x, y = geo(lon, lat)
        b.ring(x, y, 6, colour=RED, alpha=230, width=2)
        b.ring(x, y, 11, colour=VIOLET, alpha=180)
        b.marker(x, y, "dot", WHITE, 1)
    b.label(geo(3, 3)[0] - 34, geo(3, 3)[1] + 15, "GULF OF GUINEA", RED)
    b.label(geo(110, -5)[0] - 20, geo(110, -5)[1] + 15, "JAVA SEA", RED)
    b.label(geo(-56, -35)[0] + 14, geo(-56, -35)[1] + 2, "RIVER PLATE", RED)
    lagos = geo(3.4, 6.5)
    b.marker(*lagos, "diamond", AMBER, 3)
    b.line([lagos, (lagos[0] - 30, lagos[1] - 6)], AMBER, 1, 200)
    b.label(lagos[0] - 32, lagos[1] - 10, "NOVA LAGOS", WHITE, right=True)
    b.label(lagos[0] - 32, lagos[1], "1 THE CITIES", AMBER, right=True)
    for lon, lat in ((6.1, 46.2), (106.8, -6.2), (-58.4, -34.6)):
        b.marker(*geo(lon, lat), "diamond", AMBER, 2)
    b.label(geo(6.1, 46.2)[0] + 8, geo(6.1, 46.2)[1] - 2, "GENEVA CONCORD", AMBER)
    for path in (((0, 2), (-20, 20), (-35, 38), (-70, 39)), ((0, 2), (-14, 22), (-12, 40), (-6, 47))):
        b.dashed([geo(*p) for p in path], CYAN, 1, 4, 3)
    b.label(geo(-62, 25)[0], geo(-62, 25)[1], "2 THE SEA LANES", CYAN)
    relays = [(-70, 74), (-40, 76), (-10, 75), (20, 76), (50, 75), (80, 76), (110, 75)]
    b.line([geo(*p) for p in relays], GREEN, 1, 200)
    for p in relays:
        b.marker(*geo(*p), "square", GREEN, 2)
    b.label(geo(120, 75)[0], geo(120, 75)[1] - 4, "3 THE ARCTIC RELAYS", GREEN)
    b.label(W - 10, 9, "WHERE THE LINE IS BREAKING", AMBER, right=True)
    b.label(10, H - 16, "LANDING ZONES", RED)
    return b


def person(b, px, py, tall, waving):
    """A small figure standing at (px, py), head up (looking at the sky), maybe an arm raised."""
    w = max(1.0, tall * 0.28)
    r = max(1.0, tall * 0.13)
    hip, neck = py - tall * 0.38, py - tall * 0.78
    colour = WHITE + (240,)
    b.draw.line([(px - w * 0.3, py), (px - w * 0.2, hip)], fill=colour, width=1)
    b.draw.line([(px + w * 0.3, py), (px + w * 0.2, hip)], fill=colour, width=1)
    b.draw.rectangle([px - w / 2, neck, px + w / 2, hip], fill=colour)
    b.draw.ellipse([px - r, neck - 2 * r - 1, px + r, neck - 1], fill=colour)
    if waving:
        b.draw.line([(px + w / 2, neck + 1), (px + w / 2 + tall * 0.25, neck - tall * 0.4)], fill=colour, width=1)


def over_home():
    """Act 2 p3: a city street at night from rooftop height, people on the low roofs and the
    balconies looking up as a Stormhawk passes low over the street."""
    b = Board()
    vx, vy, f, cam = 336.0, 118.0, 210.0, 2.2

    def at(x, y, z):
        return vx + f * x / z, vy - f * (y - cam) / z

    b.glow(vx, vy, 100, (90, 38, 12), 0.9)                       # the city burning far off
    fill(b, [at(-1.5, 0, 1.0), at(1.5, 0, 1.0), at(1.5, 0, 60), at(-1.5, 0, 60)], (8, 10, 22))
    rng = np.random.default_rng(3108)
    blocks = []
    for side in (-1, 1):
        z = 2.0 if side < 0 else 1.6
        low = True                                              # the near ones low, with people
        while z < 36:
            depth = rng.uniform(1.4, 2.4) * (1 + z / 10)
            hgt = rng.uniform(1.1, 1.7) if low else rng.uniform(3.2, 6.5)
            blocks.append((side, z, z + depth, hgt))
            low = not low if rng.random() < 0.8 else low
            z += depth + 0.1
    people = []
    for side, z0, z1, hgt in sorted(blocks, key=lambda blk: -blk[1]):   # far ones first
        x = side * 1.6
        face = [at(x, 0, z0), at(x, 0, z1), at(x, hgt, z1), at(x, hgt, z0)]
        fill(b, face, (26, 36, 74) if side < 0 else (18, 26, 58))
        top = [at(x, hgt, z0), at(x, hgt, z1), at(x + side * 4, hgt, z1), at(x + side * 4, hgt, z0)]
        if hgt < cam:                                             # a low roof, seen from above
            fill(b, top, (34, 46, 86))
            if z0 < 12:
                for _ in range(int(rng.integers(3, 7))):
                    people.append((x + side * rng.uniform(0.15, 2.2), hgt, rng.uniform(z0 + 0.2, z1 - 0.2), True))
        rows = int(hgt / 0.5)
        for fl in range(rows):
            yf = 0.25 + fl * 0.5
            for c in range(int((z1 - z0) / 0.45)):
                zc = z0 + 0.2 + c * 0.45
                if rng.random() > 0.32:
                    continue
                wx, wy = at(x, yf, zc)
                size = max(1, int(round(f * 0.07 / zc)))
                col = AMBER if rng.random() < 0.75 else CYAN
                b.draw.rectangle([wx, wy - size, wx + size - 1, wy], fill=col + (210,))
            if hgt > cam and 0 < fl < rows - 1 and fl % 2 == 0 and z0 < 7 and rng.random() < 0.5:
                b.line([at(x, yf - 0.2, z0 + 0.2), at(x, yf - 0.2, z0 + 1.0)], GREY, 1, 230)   # a balcony
                people.append((x, yf - 0.2, z0 + 0.6, False))
        b.line([at(x, hgt, z0), at(x, hgt, z1)], CYAN, 1, 220)
        b.line([at(x, 0, z0), at(x, hgt, z0)], CYAN_DIM, 1, 160)
    for x, y, z, waving in sorted(people, key=lambda p: -p[2]):
        person(b, *at(x, y, z), max(5.0, f * 0.36 / z), waving and rng.random() < 0.45)
    for z in np.arange(1.6, 30, 1.8):                              # street lamps and the lane marks
        for side in (-1, 1):
            b.glow(*at(side * 1.3, 0.8, z), max(1.5, 22 / z), (130, 86, 30), 0.8)
        b.line([at(0, 0, z), at(0, 0, z + 0.7)], CYAN_DIM, 1, 140)
    b.title("NOVA LAGOS - A STREET, ROOFTOP HEIGHT")
    sx, sy = 400, 52
    b.glow(sx, sy + 26, 12, (40, 90, 140), 0.8)
    b.sprite(asset("ship_2", 2), sx, sy)
    b.label(sx + 52, sy - 14, "LANCER - LOW PASS", CYAN)
    b.label(sx + 52, sy - 4, "60 M OVER THE ROOFS", CYAN_DIM)
    b.label(10, 30, "PEOPLE ON THE ROOFS", WHITE)
    b.label(10, 40, "LOOKING UP", WHITE)
    b.label(10, 50, "WATCHING THE SKY", AMBER)
    b.label(W - 10, 9, "OVER HOME NOW", CYAN_DIM, right=True)
    b.label(W - 10, H - 16, "FLY ACCORDINGLY", AMBER, right=True)
    return b


def scramble():
    """Act 2 p4: Aegis Wing's Stormhawks and Rook's Ember on a coastal CDF airbase at dusk,
    canopies closing, Nova Lagos burning on the horizon."""
    b = Board()
    b.glow(W + 20, 110, 170, (100, 44, 16), 1.0)                  # dusk and the fires to the east
    coast = 70 + 14 * np.sin(b.yy / 23) + 6 * np.sin(b.yy / 9)
    b.base = np.where((b.xx < coast)[..., None], b.base * 0.55 + np.array([2, 8, 22]), b.base)
    b.title("CDF COASTAL AIRBASE - DUSK")
    b.line([(coast[y, 0], y) for y in range(20, H, 4)], CYAN_DIM, 1, 200)
    b.label(12, H - 16, "GULF OF GUINEA", CYAN_DIM)
    ry = 206
    b.draw.rectangle([120, ry - 9, 560, ry + 9], outline=CYAN_DIM + (220,))
    b.dashed([(130, ry), (550, ry)], CYAN_DIM, 1, 8, 6)
    b.label(124, ry - 22, "RUNWAY", CYAN_DIM)
    pads = [("LANCER", "ship_2", CYAN), ("ROOK", "rook", AMBER), ("AEGIS TWO", "ship_2", GREY),
            ("AEGIS 3", "ship_2", GREY), ("AEGIS 4", "ship_2", GREY)]
    for i, (name, kind, colour) in enumerate(pads):
        x, y = 170 + 76 * i, 112
        b.draw.rectangle([x - 28, y - 30, x + 28, y + 30], outline=CYAN_DIM + (180,))
        b.sprite(rook_craft() if kind == "rook" else asset("ship_2"), x, y)
        b.label(x - 26, y + 36, name, colour)
        b.label(x - 26, y + 46, "CANOPY", GREEN if i < 2 else CYAN_DIM)
        b.dashed([(x, y + 30), (x, ry - 10)], CYAN_DIM, 1, 3, 3)
        if i < 2:
            b.bracket(x - 30, y - 32, x + 30, y + 32, colour, 5)
    b.label(130, 50, "AEGIS WING - SCRAMBLE", WHITE)
    b.label(130, 60, "CANOPIES CLOSING", GREEN)
    cx, cy = 628, 100
    for k, (dx, dy) in enumerate(((-18, -10), (-6, 6), (8, -16), (14, 12), (-14, 18), (2, -2))):
        b.draw.rectangle([cx + dx - 4, cy + dy - 4, cx + dx + 4, cy + dy + 4], outline=GREY + (200,))
        if k % 2 == 0:
            b.glow(cx + dx, cy + dy, 9, (150, 60, 14), 0.9)
            b.marker(cx + dx, cy + dy, "dot", RED, 1)
    b.label(W - 10, 140, "NOVA LAGOS", RED, right=True)
    b.label(W - 10, 150, "BURNING - 40 KM", AMBER, right=True)
    b.arrow(566, ry, 610, 126, CYAN)
    b.label(W - 10, 9, "WE CAN STOP THEM STAYING", AMBER, right=True)
    return b


def nova_lagos():
    """L08 p1: the night route over Nova Lagos: in over the harbour, along the elevated highways,
    through the tower district to the Third Mainland highway; the Ikoyi shelters at its far end,
    the walkers heading for them; Lancer and Rook."""
    b = Board()
    lagoon = (b.yy > 150 + 30 * np.sin(b.xx / 90)) & (b.xx > 430)
    sea = b.yy > 196 - 0.12 * b.xx
    water = lagoon | sea
    b.base = np.where(water[..., None], b.base * 0.5 + np.array([2, 8, 24]), b.base)
    rng = np.random.default_rng(808)
    for _ in range(170):                                          # the city's lights
        x, y = rng.uniform(20, 650), rng.uniform(22, 220)
        if not water[int(y), int(x)]:
            b.glow(x, y, rng.uniform(2, 5), (90, 70, 30), 0.5)
    for x, y in ((450, 120), (500, 96), (560, 120)):
        b.glow(x, y, 26, (60, 50, 50), 0.7)                       # smoke over the district
    b.title("NOVA LAGOS - TONIGHT'S ROUTE")
    b.label(14, H - 16, "GULF OF GUINEA", CYAN_DIM)
    b.label(520, H - 16, "LAGOS LAGOON", CYAN_DIM)
    for x in (40, 70, 100):                                       # the harbour piers and cranes
        b.draw.rectangle([x, 178 - 0.12 * x, x + 10, 214 - 0.12 * x], outline=CYAN_DIM + (220,))
        b.line([(x + 5, 182 - 0.12 * x), (x + 5, 168 - 0.12 * x), (x + 12, 168 - 0.12 * x)], CYAN, 1, 220)
    b.label(18, 152, "HARBOUR", CYAN)
    hw = [(110, 150), (170, 128), (240, 132), (290, 112)]
    for off in (-3, 3):
        b.line([(x, y + off) for x, y in hw], CYAN_DIM, 1, 220)
    b.label(170, 140, "ELEVATED HIGHWAYS", CYAN_DIM)
    for x, y, s in ((300, 70, 14), (322, 96, 10), (346, 66, 18), (370, 100, 12), (330, 124, 9), (392, 74, 11),
                    (304, 100, 8), (360, 130, 8)):
        b.draw.rectangle([x - s, y - s, x + s, y + s], outline=CYAN + (220,))
        b.draw.rectangle([x - s + 3, y - s + 3, x + s - 3, y + s - 3], outline=CYAN_DIM + (160,))
    b.label(300, 30, "TOWER DISTRICT", CYAN)
    b.label(452, 72, "SMOKE", GREY)
    tm = [(420, 130), (470, 150), (540, 170), (600, 160), (630, 130)]
    for off in (-3, 3):
        b.line([(x, y + off) for x, y in tm], CYAN, 1, 230)
    b.label(450, 184, "THIRD MAINLAND HIGHWAY", CYAN)
    sx, sy = 636, 108
    b.draw.rectangle([sx - 18, sy - 12, sx + 18, sy + 12], outline=AMBER + (240,))
    b.marker(sx, sy, "diamond", AMBER, 4)
    b.label(W - 10, 76, "IKOYI SHELTERS", AMBER, right=True)
    b.label(W - 10, 86, "CIVILIANS", GREEN, right=True)
    route = [(60, 200), (110, 150), (170, 128), (240, 132), (290, 112), (350, 112), (420, 130), (470, 150),
             (540, 170), (600, 160), (618, 128)]
    b.dashed(route[:-1], AMBER, 2, 7, 4)
    b.arrow(*route[-2], *route[-1], AMBER, dashed=False)
    for x, y in ((250, 108), (380, 150), (420, 104), (500, 132), (560, 146)):
        b.marker(x, y, "chevron", RED, 4)
        b.arrow(x + 7, y - 2, x + 27, y - 2, RED, size=3)
    b.label(250, 210, "WALKERS - HEADING FOR THE SHELTERS", RED)
    b.sprite(asset("ship_2"), 40, 92)
    b.sprite(rook_craft(), 76, 104)
    b.label(14, 58, "LANCER", CYAN)
    b.label(66, 124, "ROOK", AMBER)
    b.label(W - 10, 9, "FIRST SORTIE WITH ROOK", AMBER, right=True)
    b.label(W - 10, 22, "NIGHT - FLY LOW", CYAN_DIM, right=True)
    return b


def creeper_walker_scan():
    """L08 p2: Varga's scan of the Creeper: on a low roof, its five-way fan aimed at the ship; a
    convoy's fans 0.5 s apart; anything that hits the ground hurts it twice as hard."""
    b = Board()
    b.title("SENSOR SCAN - CREEPER")
    cx, cy = 196, 104
    b.glow(cx, cy, 70, (10, 40, 50), 0.6)
    b.draw.rectangle([cx - 80, cy - 44, cx + 80, cy + 44], outline=GREY + (200,))
    b.label(cx + 20, cy + 48, "LOW ROOF", GREY)
    creeper = creeper_lit(4 * 6, 2)                               # heading 4: walks to the left
    b.sprite(creeper, cx, cy)
    gland = np.array([cx - 46, cy + 1.0])
    ship = np.array([84.0, 190])
    b.sprite(asset("ship_2"), *ship)
    aim = np.arctan2(*(ship - gland)[::-1])
    orb = asset("orb_0")
    for k in range(5):
        a = aim + np.radians(-25 + 12.5 * k)
        u = np.array([np.cos(a), np.sin(a)])
        b.dashed([tuple(gland + u * 8), tuple(gland + u * 92)], VIOLET, 1, 3, 3)
        b.sprite(orb, *(gland + u * 66))
    b.label(cx - 150, 26, "CREEPER", WHITE)
    b.label(cx - 150, 36, "CRAWLS THE STREETS", CYAN_DIM)
    b.label(cx - 150, 46, "AND THE LOW ROOFS", CYAN_DIM)
    b.label(cx - 20, 168, "5-WAY FAN, AIMED AT YOU", VIOLET)
    b.label(cx - 20, 178, "EVERY 3 SECONDS", VIOLET)
    b.line([(cx - 40, cy - 12), (cx - 10, 50)], AMBER, 1, 200)
    b.label(cx - 14, 40, "FAN GLAND", AMBER)
    bx, by = 330, 40
    b.draw.rectangle([bx - 4, by - 6, bx + 114, by + 26], outline=GREEN + (240,))
    b.sprite(asset("bomb-rack-shot"), bx + 8, by + 10)
    b.label(bx + 18, by - 1, "ANTI-GROUND", GREEN)
    b.label(bx + 18, by + 9, "DAMAGE X2", GREEN)
    b.arrow(bx - 6, by + 18, cx + 64, cy - 18, GREEN)
    sy = 150
    b.line([(400, sy - 16), (W - 8, sy - 16)], CYAN_DIM, 1, 120)
    b.line([(400, sy + 16), (W - 8, sy + 16)], CYAN_DIM, 1, 120)
    b.label(404, sy + 20, "STREET", CYAN_DIM)
    small = asset(f"creeper_{4 * 6}")
    ship2 = np.array([448.0, 214])
    b.sprite(asset("ship_2"), *ship2)
    for i, x in enumerate((486, 554, 622)):
        b.sprite(small, x, sy)
        g = np.array([x - 23, sy + 1.0])
        aim = np.arctan2(*(ship2 - g)[::-1])
        r = (1.0 - 0.5 * i) * 40 + 10                         # the first volley furthest out
        for k in range(5):
            a = aim + np.radians(-25 + 12.5 * k)
            u = np.array([np.cos(a), np.sin(a)])
            b.line([tuple(g + u * 5), tuple(g + u * (r - 3))], VIOLET, 1, 110)
            b.marker(*(g + u * r), "dot", VIOLET, 2)
        b.label(x - 20, sy - 30, f"T+{0.5 * i:.1f} S", AMBER)
    b.label(404, 92, "CONVOY: FANS 0.5 S APART", WHITE)
    b.label(404, 102, "FRONT TO BACK", CYAN_DIM)
    b.label(W - 10, 9, "GROUND LAYER", CYAN_DIM, right=True)
    b.label(10, H - 16, "EVERY ONE YOU STOP NEVER REACHES THE SHELTERS", AMBER)
    return b


def arcology_district():
    """L09 p1: the arcology district before dawn, the grey glow in the east: the route in along the
    ruined elevated highway, Unity Plaza (cluster A), the boulevard, the lagoon and the Okonjo Bridge
    with the Kilo convoy (cluster B at its bridgehead), the Ndidi Arcology's lobby plaza (cluster C);
    Lancer and Rook at the start."""
    b = Board()
    b.glow(W + 40, 120, 260, (40, 44, 58), 0.9)                   # the grey glow in the east
    lagoon = (b.xx > 360 + 22 * np.sin(b.yy / 40)) & (b.xx < 520 + 18 * np.sin(b.yy / 55 + 1))
    b.base = np.where(lagoon[..., None], b.base * 0.5 + np.array([4, 10, 28]) + (b.xx / W)[..., None] * 14, b.base)
    rng = np.random.default_rng(909)
    for _ in range(150):                                          # the district's last lights
        x, y = rng.uniform(20, 650), rng.uniform(22, 220)
        if not lagoon[int(y), int(x)]:
            b.glow(x, y, rng.uniform(2, 4), (80, 64, 30), 0.4)
    for x, y in ((210, 60), (580, 70), (600, 150)):
        b.glow(x, y, 24, (55, 50, 52), 0.6)                       # smoke
    b.title("NOVA LAGOS - ARCOLOGY DISTRICT - BEFORE DAWN")
    hw = [(20, 210), (80, 180), (130, 150)]                       # the elevated highway in
    for off in (-3, 3):
        b.line([(x, y + off) for x, y in hw], CYAN_DIM, 1, 220)
    b.label(82, 196, "ELEVATED HIGHWAY", CYAN_DIM)                # below the highway, clear of ROOK
    px, py = 180, 120                                             # Unity Plaza
    b.draw.rectangle([px - 34, py - 26, px + 34, py + 26], outline=CYAN + (220,))
    b.draw.ellipse([px - 7, py - 7, px + 7, py + 7], outline=CYAN_DIM + (200,))
    b.label(px - 34, py + 32, "UNITY PLAZA", CYAN)
    bl = [(214, 112), (270, 96), (330, 96), (366, 100)]           # the boulevard
    for off in (-4, 4):
        b.line([(x, y + off) for x, y in bl], CYAN_DIM, 1, 220)
    b.label(244, 74, "BOULEVARD", CYAN_DIM)
    bx = 440                                                      # the Okonjo Bridge over the lagoon
    for off in (-4, 4):
        b.line([(bx - 40 + off, 196), (bx + 40 + off, 40)], CYAN, 1, 230)
    b.label(436, 206, "OKONJO BRIDGE", CYAN)
    b.label(380, 226, "LAGOS LAGOON", CYAN_DIM)
    for k in range(4):                                            # the Kilo convoy on it
        t = 0.25 + 0.12 * k
        cx, cy = bx - 40 + 80 * t, 196 - 156 * t
        b.draw.rectangle([cx - 2, cy - 3, cx + 2, cy + 3], fill=GREEN + (255,))
    b.label(468, 120, "KILO CONVOY", GREEN)
    b.label(468, 130, "60 CIVILIANS", GREEN)
    ax, ay = 600, 96                                              # the Ndidi Arcology
    for r in (22, 15, 8):
        b.draw.rectangle([ax - r, ay - r, ax + r, ay + r], outline=(VIOLET if r == 22 else CYAN_DIM) + (230,))
    b.glow(ax, ay, 22, (60, 20, 70), 0.7)
    b.label(W - 10, 30, "NDIDI ARCOLOGY", VIOLET, right=True)
    b.label(W - 10, 40, "HOLLOW WITH CREEP", GREY, right=True)
    clusters = {"A": [(166, 112), (194, 128)], "B": [(470, 60), (492, 72)], "C": [(574, 122), (590, 134)]}
    for name, nodes in clusters.items():
        for x, y in nodes:
            b.glow(x, y, 8, (40, 10, 60), 0.9)
            b.marker(x, y, "diamond", AMBER, 4)
        cx = sum(x for x, _ in nodes) / 2
        cy = min(y for _, y in nodes) - 14
        b.label(int(cx) - 3, int(cy) - 4, name, AMBER)
    b.label(500, 84, "BRIDGEHEAD", CYAN_DIM)
    route = [(30, 214), (80, 184), (130, 154), (180, 120), (270, 100), (366, 100), (420, 120), (480, 66),
             (540, 90), (596, 128)]
    b.dashed(route[:-1], AMBER, 2, 7, 4)
    b.arrow(*route[-2], *route[-1], AMBER, dashed=False)
    b.sprite(asset("ship_2"), 40, 120)
    b.sprite(rook_craft(), 72, 132)
    b.label(14, 88, "LANCER", CYAN)
    b.label(90, 150, "ROOK", AMBER)
    b.label(W - 10, 9, "DESTROY 6 HIVE NODES", AMBER, right=True)
    b.label(W - 10, 216, "LAST HOUR OF NIGHT", GREY, right=True)
    return b


def node_lit(frame, zoom):
    """A Hive Node frame with its glow added as the game blends it, palettised."""
    body = np.array(asset(f"hive-node_{frame}", zoom)).astype(np.float64)
    glow = np.array(asset(f"hive-node-glow_{frame}", zoom)).astype(np.float64)
    body[..., :3] = np.clip(body[..., :3] + glow[..., :3] * glow[..., 3:4] / 255, 0, 255)
    return artkit.quantize_set([Image.fromarray(body.astype(np.uint8))], 48)[0]


def node_scan():
    """L09 p2: Varga's scan: a Hive Node with its iris open (hardened: normal rounds spark off), its
    iris cycle breeding Skitters, the anti-ground sources that crack it; a Ravager's pounce arc at
    the ship with its air window."""
    b = Board()
    b.title("SENSOR SCAN - HIVE NODE / RAVAGER")
    nx, ny = 104, 120
    b.glow(nx, ny, 70, (6, 40, 34), 0.8)
    creep = asset("hive-node-creep_0")
    b.sprite(creep, nx, ny)
    b.sprite(node_lit(20, 2), nx, ny)
    b.label(14, 26, "HIVE NODE", WHITE)
    b.label(14, 36, "HARDENED SPAWNER", CYAN_DIM)
    b.draw.rectangle([10, 210, 78, 224], outline=GREY + (230,))
    b.label(14, 214, "HARDENED", GREY)
    b.line([(44, 210), (66, 180)], GREY, 1, 200)
    for k, (fx, it) in enumerate(((0, "IRIS SHUT"), (20, "OPEN"))):              # the iris cycle
        x = 252 + k * 74
        b.sprite(node_lit(fx, 1), x, 176)
        b.label(x - 26, 218, it, CYAN_DIM)
    b.arrow(282, 176, 300, 176, CYAN_DIM, size=3)
    for dx, dy in ((372, 160), (390, 186)):
        b.sprite(asset("skitter_0"), dx, dy)
        b.arrow(352, 176, dx - 8, dy, VIOLET, size=2)
    b.label(352, 208, "EVERY 4 S:", VIOLET)
    b.label(352, 218, "2 SKITTERS", VIOLET)
    gx, gy = 200, 30                                              # what cracks it
    b.draw.rectangle([gx - 4, gy - 6, gx + 196, gy + 70], outline=GREEN + (240,))
    b.label(gx + 2, gy - 1, "ONLY ANTI-GROUND CRACKS IT", GREEN)
    b.sprite(asset("bomb-rack-shot"), gx + 12, gy + 22)
    b.label(gx + 26, gy + 18, "BOMBS", GREEN)
    b.sprite(asset("hammer-mortar-shot"), gx + 12, gy + 38)
    b.label(gx + 26, gy + 34, "MORTARS, ROOK'S TOO", GREEN)
    b.sprite(asset("airstrike-bomber_0"), gx + 170, gy + 36)
    b.label(gx + 26, gy + 50, "THE AIRSTRIKE", GREEN)
    b.label(gx - 4, gy + 80, "NORMAL ROUNDS SPARK OFF", GREY)
    rx, ry = 446, 76                                              # the Ravager's pounce
    ship = np.array([630.0, 200])
    b.label(422, 26, "RAVAGER", WHITE)
    b.label(422, 36, "PACK HUNTER", CYAN_DIM)
    b.sprite(asset(f"ravager_{14 * 8}", 1), rx, ry)
    pts = []
    for k in range(25):
        s_ = k / 24
        x = rx + 20 + (ship[0] - 20 - rx - 20) * s_
        y = ry + 16 + (ship[1] - 18 - ry - 16) * s_ - math_sin(s_) * 34
        pts.append((x, y))
    air0, air1 = int(24 * 0.3), int(24 * 0.7)
    b.dashed(pts[:air0 + 1], CYAN_DIM, 1, 4, 3)
    b.line(pts[air0:air1 + 1], RED, 3)
    b.dashed(pts[air1:], CYAN_DIM, 1, 4, 3)
    b.sprite(asset(f"ravager-leap_{14 * 4 + 1}"), *pts[3])
    b.sprite(asset("ship_2"), *ship)
    b.label(452, 196, "AIR 0.3 S:", RED)
    b.label(452, 206, "IT HITS YOU", RED)
    b.label(W - 10, 26, "POUNCES WITHIN", AMBER, right=True)
    b.label(W - 10, 36, "200 PX", AMBER, right=True)
    b.label(422, 224, "SHOOT IT BEFORE IT JUMPS", AMBER)
    b.label(W - 10, 9, "GROUND LAYER", CYAN_DIM, right=True)
    return b


def math_sin(s_):
    return float(np.sin(np.pi * s_))


def scaled_sprite(name, scale, colours=48):
    """A production sprite at ``scale`` (box-filtered, alpha stepped), palettised."""
    img = asset(name)
    w, h = max(1, round(img.width * scale)), max(1, round(img.height * scale))
    a = np.array(img.resize((w, h), Image.BOX)).astype(np.float64)
    a[..., 3] = np.where(a[..., 3] >= 128, 255, 0)
    return artkit.quantize_set([Image.fromarray(a.astype(np.uint8), "RGBA")], colours)[0]


def additive(name, scale=1.0, strength=1.0):
    """An additive sprite (the Wraith's shimmer, its decloak flash) as an RGB array premultiplied
    on black, as the game blends it, at ``scale``."""
    img = asset(name)
    if scale != 1.0:
        img = img.resize((max(1, round(img.width * scale)), max(1, round(img.height * scale))), Image.BOX)
    a = np.array(img).astype(np.float64)
    return a[..., :3] * a[..., 3:4] / 255 * strength


def light_at(b, rgb, cx, cy, layer="over"):
    b.add_light(rgb, cx - rgb.shape[1] / 2, cy - rgb.shape[0] / 2, layer)


DAWN = (70, 52, 40)   # the first light in the east (Level 10)


def evacuation_route():
    """L10 p1: the evacuation corridor at first light, west to east: Eko spaceport with its five
    pads and the shuttles on them, the suburbs with their jammed highways and refugee lights, the
    maglev viaduct between the two creep districts, the coast road with the capsized ferry on its
    sandbar, the lagoon and the climb-out over the sea; the five shuttles' route, Lancer and Rook
    with them, the contacts closing from behind."""
    b = Board()
    b.glow(W + 60, 110, 300, DAWN, 1.0)                           # first light in the east
    b.glow(W + 20, 40, 120, (60, 44, 46), 0.6)
    coast = 520 + 16 * np.sin(b.yy / 46) + 0.18 * (b.yy - 120)
    water = b.xx > coast
    b.base = np.where(water[..., None], b.base * 0.55 + np.array([4, 12, 30]) + ((b.xx - 520) / 150).clip(0, 1)[..., None]
                      * np.array([22, 18, 16]), b.base)
    rng = np.random.default_rng(1010)
    for _ in range(120):                                          # the sprawl's last lights
        x, y = rng.uniform(150, 510), rng.uniform(26, 222)
        if not water[int(y), int(x)]:
            b.glow(x, y, rng.uniform(2, 4), (80, 64, 32), 0.35)
    for cx, cy in ((352, 58), (360, 182)):                        # the creep districts either side of the viaduct
        for _ in range(16):
            b.glow(cx + rng.uniform(-44, 44), cy + rng.uniform(-22, 22), rng.uniform(6, 12), (40, 12, 56), 0.5)
    for x, y in ((300, 40), (410, 200), (230, 200)):
        b.glow(x, y, 22, (56, 50, 52), 0.55)                      # smoke
    b.title("NOVA LAGOS - EVACUATION CORRIDOR - FIRST LIGHT")
    px0, py0 = 18, 64                                             # Eko spaceport and its five pads
    b.draw.rectangle([px0, py0, px0 + 104, py0 + 120], outline=CYAN_DIM + (220,))
    b.line([(px0 + 4, py0 + 116), (px0 + 100, py0 + 116)], CYAN_DIM, 1, 160)
    pads = [(70, 86), (44, 116), (96, 116), (44, 154), (96, 154)]  # One leads, Two to Five in a double column
    for x, y in pads:
        b.ring(x, y, 13, colour=CYAN, alpha=200)
        b.ring(x, y, 9, colour=CYAN_DIM, alpha=140)
    b.label(px0, py0 + 126, "EKO SPACEPORT", CYAN)
    b.label(px0, py0 + 136, "5 PADS", CYAN_DIM)
    for y in (100, 136):                                          # the suburbs' jammed highways
        pts = [(124, y), (180, y - 6), (236, y + 4), (288, y - 2)]
        for off in (-3, 3):
            b.line([(x, yy + off) for x, yy in pts], CYAN_DIM, 1, 200)
        for k in range(26):
            s_ = k / 26
            x = 128 + 156 * s_
            yy = np.interp(x, [p[0] for p in pts], [p[1] for p in pts])
            b.draw.point((x, yy + rng.choice([-1, 1])), fill=(AMBER if k % 3 else GREEN) + (255,))
    b.label(150, 74, "SUBURBS", CYAN)
    b.label(150, 84, "ROADS JAMMED", CYAN_DIM)
    vy = 118                                                      # the maglev viaduct
    for off in (-4, 4):
        b.line([(296, vy + off), (452, vy + off)], CYAN, 1, 230)
    for x in range(300, 452, 14):
        b.line([(x, vy - 4), (x, vy + 4)], CYAN_DIM, 1, 140)
    b.label(318, 100, "MAGLEV VIADUCT", CYAN)
    b.label(318, 28, "CREEP DISTRICT", VIOLET)
    b.label(318, 206, "CREEP DISTRICT", VIOLET)
    cr = [(452, vy), (488, 130), (510, 160), (528, 196)]           # the coast road
    for off in (-3, 3):
        b.line([(x + off, y) for x, y in cr], CYAN_DIM, 1, 220)
    b.label(430, 210, "COAST ROAD", CYAN_DIM)
    fx, fy = 566, 166                                             # the capsized ferry on its sandbar
    b.glow(fx, fy + 4, 16, (40, 46, 40), 0.7)
    ang = np.radians(-28)
    hull = [(-22, 0), (-17, -6), (14, -6), (24, 0), (14, 6), (-17, 6)]
    rot = [(fx + x * np.cos(ang) - y * np.sin(ang), fy + x * np.sin(ang) + y * np.cos(ang)) for x, y in hull]
    b.draw.polygon(rot, outline=CYAN + (230,))
    b.line([rot[0], ((rot[2][0] + rot[4][0]) / 2, (rot[2][1] + rot[4][1]) / 2)], CYAN_DIM, 1, 200)   # the keel
    b.label(fx - 24, fy + 18, "CAPSIZED FERRY", CYAN_DIM)
    shore = [(float(520 + 16 * np.sin(y / 46) + 0.18 * (y - 120)), float(y)) for y in range(22, 232, 6)]
    b.dashed(shore, CYAN_DIM, 1, 3, 3)
    b.label(578, 64, "LAGOON", CYAN_DIM)
    b.label(W - 10, 216, "FIRST LIGHT", GREY, right=True)
    route = [(70, 70), (120, 104), (180, 112), (236, 120), (296, 118), (452, 118), (500, 126), (560, 120), (620, 96),
             (640, 50)]
    b.dashed(route[:-1], AMBER, 2, 7, 4)
    b.arrow(*route[-2], *route[-1], AMBER, dashed=False)
    b.label(W - 10, 26, "CLIMB-OUT", AMBER, right=True)
    b.label(W - 10, 36, "OVER THE SEA", AMBER, right=True)
    for x, y in ((150, 160), (330, 156), (420, 150)):             # contacts closing from behind
        b.marker(x, y, "chevron", RED, 4)
        b.arrow(x + 7, y - 4, x + 27, y - 14, RED, size=3)
    b.label(170, 214, "CONTACTS FROM BEHIND", RED)
    shuttle = scaled_sprite("evacuation-shuttle_2", 0.5)
    for k, (x, y) in enumerate(pads):
        b.sprite(shuttle, x, y)
        b.label(x + 12, y - 15, str(k + 1), GREEN)
    b.sprite(asset("ship_2"), 196, 52)
    b.sprite(rook_craft(), 236, 66)
    b.label(176, 22, "LANCER", CYAN)
    b.label(252, 54, "ROOK", AMBER)
    b.label(W - 10, 9, "ESCORT 5 SHUTTLES", AMBER, right=True)
    b.label(px0, 34, "LIFELINE 1-5", GREEN)
    b.label(px0, 44, "1,100 PEOPLE", GREEN)
    return b


def wraith_scan():
    """L10 p2: Varga's scan: the Wraith cloaked (its shimmer on high air: only homing finds it) and
    decloaked; its pass overhead, out the bottom edge, the loop and the rear entry with the 3 s
    warning, the decloak flash and its bursts up the screen through the shuttle band; a Mote
    Swarm sweeping down, out the bottom and back up from behind."""
    b = Board()
    b.title("SENSOR SCAN - WRAITH / MOTE SWARM")
    cx, cy = 58, 110                                              # cloaked
    b.glow(cx, cy, 46, (24, 14, 44), 0.6)
    light_at(b, additive("wraith-cloak_0"), cx, cy)
    b.label(14, 26, "WRAITH", WHITE)
    b.label(14, 36, "CLOAKED: HIGH AIR", VIOLET)
    b.label(14, 164, "ONLY HOMING", GREEN)
    b.label(14, 174, "FINDS IT", GREEN)
    b.sprite(asset("hornet-launcher-shot_0"), 108, 168)
    dx, dy = 158, 110                                             # decloaked
    light_at(b, additive("wraith-decloak_1", 1.0, 0.55), dx, dy, "under")
    b.sprite(asset("wraith_32"), dx, dy)
    b.arrow(96, 110, 116, 110, CYAN_DIM, size=3)
    b.label(124, 152, "DECLOAKED", WHITE)
    b.label(124, 162, "0.4 S FLASH", VIOLET)
    b.label(124, 202, "THEN EVERY GUN", GREEN)
    b.label(124, 212, "HITS IT", GREEN)
    x0, y0, x1, y1 = 232, 28, 404, 192                            # the play field, top down
    b.draw.rectangle([x0, y0, x1, y1], outline=CYAN_DIM + (200,))
    band0, band1 = 52, 96                                         # the shuttle band
    for x in range(x0 + 2, x1 - 1, 6):
        b.line([(x, band0), (x + 4, band0)], GREEN, 1, 110)
        b.line([(x, band1), (x + 4, band1)], GREEN, 1, 110)
    shuttle = scaled_sprite("evacuation-shuttle_2", 0.5)
    for sx, sy in ((292, 64), (344, 64), (318, 84)):
        b.sprite(shuttle, sx, sy)
    b.label(x1 + 6, band0 + 4, "SHUTTLES", GREEN)
    ship = np.array([300.0, 160])
    pts = [(262, y0 - 14), (258, 60), (270, 120), (284, y1), (292, y1 + 26), (330, y1 + 32), (352, y1 + 18),
           (356, y1 - 12)]
    b.dashed(pts[:4], VIOLET, 1, 4, 3)                            # the pass overhead, cloaked
    b.dashed(pts[3:], GREY, 1, 2, 3)                              # the loop, off screen
    light_at(b, additive("wraith-cloak_0", 0.5), 262, 44)
    b.label(176, 38, "PASSES", VIOLET)
    b.label(176, 48, "OVERHEAD", VIOLET)
    b.label(410, y1 + 10, "LOOPS BEHIND", GREY)
    for k in range(3):                                            # the 3 s warning at the bottom edge
        b.marker(340 + 12 * k, y1 - 3, "chevron", AMBER, 3)
    b.label(x1 + 6, y1 - 6, "3 S WARNING", AMBER)
    wx, wy = 356, 168
    light_at(b, additive("wraith-decloak_2", 0.5, 0.8), wx, wy, "under")
    b.sprite(scaled_sprite("wraith_32", 0.5), wx, wy)
    orb = scaled_sprite("orb-medium_0", 0.6)
    for k, ang in enumerate((-100, -112, -124)):                  # its bursts up the screen
        u = np.array([np.cos(np.radians(ang)), np.sin(np.radians(ang))])
        p0 = np.array([wx, wy - 10.0])
        b.dashed([tuple(p0), tuple(p0 + u * 128)], RED, 1, 3, 3)
        for r in (36, 76, 108):
            b.sprite(orb, *(p0 + u * r))
    b.label(x1 + 6, 112, "DECLOAKS AT", VIOLET)
    b.label(x1 + 6, 122, "YOUR SIX", VIOLET)
    b.label(x1 + 6, 136, "BURSTS UP", RED)
    b.label(x1 + 6, 146, "THROUGH THE", RED)
    b.label(x1 + 6, 156, "SHUTTLES", RED)
    b.sprite(asset("ship_2"), *ship)
    mx0 = 500                                                     # the Mote Swarm
    b.label(mx0, 26, "MOTE SWARM", WHITE)
    b.label(mx0, 36, "A FLOCK, NOT", CYAN_DIM)
    b.label(mx0, 46, "A FORMATION", CYAN_DIM)
    path = [(530, 66), (560, 112), (604, 160), (640, 206)]
    back = [(640, 206), (656, 228), (668, 200), (650, 150), (618, 104)]
    b.dashed(path, VIOLET, 1, 4, 3)
    b.dashed(back[:3], GREY, 1, 2, 3)
    b.arrow(*back[2], *back[3], RED, size=3)
    b.arrow(*back[3], *back[4], RED, size=4, dashed=False)
    b.label(mx0, 196, "TURNS BACK", RED)
    b.label(mx0, 206, "FROM BELOW", RED)
    mrng = np.random.default_rng(1012)
    for k in range(16):                                           # the flock, aligned on the route
        s_ = mrng.uniform(0.15, 0.6)
        bx = np.interp(s_, [0, 1 / 3, 2 / 3, 1], [p[0] for p in path]) + mrng.normal(0, 9)
        by = np.interp(s_, [0, 1 / 3, 2 / 3, 1], [p[1] for p in path]) + mrng.normal(0, 7)
        heading = 14 + int(mrng.integers(-1, 2))                  # flying down and to the right
        b.sprite(asset(f"mote-swarm_{(heading % 16) * 3 + k % 3}"), bx, by)
    b.sprite(asset("mote-swarm_25", 2), 646, 72)
    b.label(W - 10, 9, "FROM BEHIND", AMBER, right=True)
    b.label(10, H - 16, "FIT A REAR GUN, LANCER", AMBER)
    return b

# the convoy's ship pair: variant a's names, which convoy_ships.py writes (``--variant b`` writes b's under the
# same names, so switching the pair means re-running convoy_ships.py --variant b and this script again)
CARGO, FRIGATE = "cargo-ship_0", "escort-frigate_0"
SEA = (10, 44, 62)    # the sea on the display (Level 11)
SEA_DEEP = (2, 12, 24)


def sea_display(b, below=None):
    """The ocean on the display: the level's swell piece as a faint relief in teal over the dark base;
    with ``below`` (a y) the water under that line is darker and deeper (the cut-away of Level 11's scan)."""
    swell = np.array(Image.open(ROOT / "assets" / "backdrop" / "level-11" / "swell.png").convert("L")).astype(float) / 255
    tile = np.hstack([swell[:H, :], swell[:H, ::-1], swell[:H, :]])[:, :W]
    relief = (tile - tile.mean()) * 2.2
    b.base += relief[..., None] * np.array(SEA) * 1.1 + np.array(SEA) * 0.55
    if below is not None:
        k = np.clip((b.yy - below) / 50, 0, 1)[..., None]
        b.base = b.base * (1 - 0.55 * k) + np.array(SEA_DEEP) * 0.5 * k


def turned(img, degrees):
    """A sprite turned clockwise by ``degrees`` (a multiple of 90), its palette kept."""
    return img.rotate(-degrees, expand=True)


def convoy_route():
    """L11 p1: Okafor's map of the Atlantic from above: Convoy Atlas-Seven (the three cargo hulls and the
    frigate, production ships) heading east and then to open water, the seeded sea lanes with their
    Driftjellies, the reef line with its Reef Spitter rafts and Platform Tiamat on the route with the
    Kraken's gripping arms on it and an unknown large contact under it (the Kraken's hologram)."""
    b = Board()
    sea_display(b)
    b.glow(W + 40, 10, 260, (30, 40, 36), 0.5)
    b.title("NORTH ATLANTIC - CONVOY ATLAS-SEVEN - ARCTIC RELAY RUN")
    rng = np.random.default_rng(1101)
    tx, ty = 478, 84                                                  # Platform Tiamat's centre
    for x in (196, 252):                                              # the seeded lanes, two bands crossing the route
        for off in (-22, 22):
            pts = [(x + off, 46), (x + off + 10, 112), (x + off - 6, 216)]
            b.dashed(pts, VIOLET, 1, 5, 4)
    b.label(176, 22, "SEEDED SEA LANES", VIOLET)
    b.label(176, 32, "DRIFTING JELLIES", CYAN_DIM)
    jelly = scaled_sprite("driftjelly_0", 0.7)
    for k in range(11):
        lane = 196 if k % 2 else 252
        b.sprite(jelly, lane + rng.uniform(-14, 14), 54 + k * 15 + rng.uniform(-3, 3))
    chain = [(318, 58), (336, 88), (354, 124), (374, 160), (392, 188)]  # the reef line
    b.dashed(chain, VIOLET, 1, 3, 3)
    reef = [backdrop("level-11", f"reef-growth-{c}_0", 0.55) for c in "abc"]
    for k, (x, y) in enumerate(chain):
        b.sprite(reef[k % 3], x + (14 if k % 2 else -14), y)
    raft = scaled_sprite("reef-spitter-raft_0", 0.45)
    for x, y in ((344, 76), (372, 142), (402, 176)):
        b.sprite(raft, x, y)
    b.label(316, 206, "REEF LINE", VIOLET)
    b.label(316, 216, "GUN RAFTS", CYAN_DIM)
    route = [(150, 170), (230, 158), (310, 128), (384, 100), (412, 92)]
    b.dashed(route, AMBER, 2, 7, 4)
    b.dashed([(544, 80), (590, 70)], AMBER, 2, 7, 4)
    b.arrow(590, 70, 640, 50, AMBER, dashed=False)
    b.label(W - 10, 96, "OPEN WATER", AMBER, right=True)
    b.label(W - 10, 106, "ARCTIC RELAYS BEYOND", CYAN_DIM, right=True)
    b.sprite(holo(scaled_sprite("harbour-kraken-sub", 0.5), VIOLET), tx, ty + 76)   # under the platform
    b.sprite(backdrop("level-11", "platform-tiamat_0", 0.5, 64), tx, ty)
    grip = scaled_sprite("harbour-kraken-grip_0", 0.5)
    b.sprite(grip, tx, ty + 11)
    b.marker(tx - 78, ty - 20, "diamond", AMBER, 4)
    b.label(tx - 66, 22, "PLATFORM TIAMAT", AMBER)
    b.label(tx - 66, 32, "OVERRUN FUSION PLATFORM", CYAN_DIM)
    b.label(tx + 52, 166, "UNKNOWN LARGE", RED)
    b.label(tx + 52, 176, "CONTACT UNDER IT", RED)
    b.draw.line([(tx + 40, 160), (tx + 50, 168)], fill=RED + (230,))
    cargo = turned(scaled_sprite(CARGO, 0.6), 90)
    frigate = turned(scaled_sprite(FRIGATE, 0.6), 90)
    for x, y in ((112, 134), (140, 170), (112, 206)):
        b.sprite(cargo, x, y)
    b.sprite(frigate, 62, 170)
    b.dashed([(112, 134), (112, 206)], CYAN_DIM, 1, 2, 3)
    b.bracket(24, 118, 168, 226, CYAN)
    b.label(30, 80, "ATLAS-SEVEN", CYAN)
    b.label(30, 90, "3 CARGO + 1 FRIGATE", CYAN_DIM)
    b.label(30, 100, "REACTOR PARTS", GREEN)
    b.label(W - 10, 9, "STEAM THEM TO OPEN WATER", AMBER, right=True)
    b.sprite(asset("ship_2"), 232, 200)
    b.label(262, 196, "LANCER", CYAN)
    return b


def sub_scan():
    """L11 p2: Varga's cut-away at the waterline: a Driftjelly surfaced (the guns reach it) and submerged
    (they don't), the shots stopping at the waterline, a torpedo's run under the surface to a submerged
    jelly, a Reef Spitter raft (its gun above, its roots below) and the unknown large contact deep under it all."""
    b = Board()
    wl = 112                                                          # the waterline
    sea_display(b, wl)
    b.title("SENSOR SCAN - DRIFTJELLY / REEF SPITTER - WATERLINE")
    b.line([(0, wl), (W, wl)], CYAN, 1, 230)
    for x in range(6, W, 22):
        b.line([(x, wl + 3), (x + 9, wl + 3)], CYAN_DIM, 1, 140)
    b.label(W - 10, wl - 12, "SURFACE", CYAN, right=True)
    b.label(W - 10, wl + 8, "BELOW", CYAN_DIM, right=True)
    jx = 84                                                           # the jelly, surfaced and submerged
    b.sprite(asset("driftjelly_0", 2), jx, wl - 36)
    b.sprite(asset("driftjelly-sub_0", 2), jx, wl + 48)
    b.label(14, 26, "DRIFTJELLY", WHITE)
    b.label(14, 36, "SURFACED: ALL GUNS", GREEN)
    b.label(14, wl + 94, "SUBMERGED: GUNS", RED)
    b.label(14, wl + 104, "CAN'T REACH IT", RED)
    b.arrow(jx + 52, wl - 20, jx + 52, wl + 26, VIOLET, size=4)
    b.arrow(jx + 58, wl + 26, jx + 58, wl - 20, VIOLET, size=4)
    b.label(jx + 66, wl - 6, "SWAPS EVERY", VIOLET)
    b.label(jx + 66, wl + 4, "6-10 S", VIOLET)
    sx = 262                                                          # the guns stop at the waterline
    b.sprite(asset("ship_2"), sx, 58)
    for dx in (-10, 10):
        b.dashed([(sx + dx, 78), (sx + dx, wl - 4)], AMBER, 1, 4, 3)
        b.marker(sx + dx, wl - 2, "cross", AMBER, 2)
    b.label(sx + 36, 52, "GUNS STOP AT", AMBER)
    b.label(sx + 36, 62, "THE WATERLINE", AMBER)
    ty = wl + 56                                                      # the torpedo's run
    b.sprite(asset("driftjelly-sub_0", 2), 392, ty)
    b.sprite(asset("torpedo-pod-shot_8", 2), 252, ty)
    for k, x in enumerate((236, 224, 212, 202, 194)):
        b.sprite(asset(f"torpedo-pod-bubbles_{k % 6}", 1), x, ty + (k % 2) * 3 - 1)
    b.dashed([(272, ty), (356, ty)], GREEN, 2, 6, 4)
    b.arrow(340, ty, 356, ty, GREEN, size=5, dashed=False)
    b.label(sx - 30, wl + 22, "TORPEDO POD", GREEN)
    b.label(sx - 30, wl + 32, "RUNS UNDER", GREEN)
    b.label(sx - 30, wl + 42, "THE SURFACE", GREEN)
    b.label(sx - 30, wl + 88, "HITS WHAT DIVES", GREEN)
    rx = 520                                                          # the Reef Spitter raft
    b.sprite(holo(scaled_sprite("reef-spitter-raft-sub", 0.9), VIOLET), rx, wl + 38)
    b.sprite(asset("reef-spitter-raft_0"), rx, wl - 40)
    b.sprite(asset("reef-spitter_8"), rx, wl - 40)
    b.label(rx - 40, 26, "REEF SPITTER", WHITE)
    b.label(rx - 40, 36, "GUN RAFT: ALL GUNS", GREEN)
    b.label(rx - 38, wl + 86, "ROOTS IN THE REEF", VIOLET)
    b.sprite(holo(scaled_sprite("harbour-kraken-sub", 0.36), RED, 10), 634, wl + 80)
    b.label(W - 78, H - 26, "UNKNOWN, LARGE,", RED, right=True)
    b.label(W - 78, H - 16, "SUBMERGED", RED, right=True)
    b.label(W - 10, 9, "FIT TORPEDO PODS", AMBER, right=True)
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
    "level-03-spore-lanes": spore_lanes,
    "level-03-spore-echo": spore_echo,
    "level-04-convoy-road": convoy_road,
    "level-04-walker-scan": walker_scan,
    "level-05-crater-nest": crater_nest,
    "level-05-mortar-scan": mortar_scan,
    "level-06-daedalus-rim": daedalus_rim,
    "level-06-edge-scan": edge_scan,
    "level-07-l1-carrier": l1_carrier,
    "level-07-overhead-scan": overhead_scan,
    "act-1-outro-carcass": outro_carcass,
    "act-1-outro-daedalus-rim": outro_daedalus,
    "act-1-outro-second-fleet": outro_second_fleet,
    "act-1-outro-rook": outro_rook,
    "act-2-landfall": landfall,
    "act-2-front-lines": front_lines,
    "act-2-over-home": over_home,
    "act-2-scramble": scramble,
    "level-08-nova-lagos": nova_lagos,
    "level-08-walker-scan": creeper_walker_scan,
    "level-09-arcology-district": arcology_district,
    "level-09-node-scan": node_scan,
    "level-10-evacuation-route": evacuation_route,
    "level-10-wraith-scan": wraith_scan,
    "level-11-convoy-route": convoy_route,
    "level-11-sub-scan": sub_scan,
}

# Review sheets per batch: round, the images on it, the batch name.
BATCHES = {
    "r13": (list(IMAGES)[:9], "UI BATCH"),
    "r20": (list(IMAGES)[9:13], "M4 BRIEFING IMAGES"),
    "r21": (list(IMAGES)[13:15], "M4 PART E"),
    "r23": (list(IMAGES)[15:17], "M4 PART F"),
    "r25": (list(IMAGES)[17:23], "M4 PART G"),
    "r30": (list(IMAGES)[23:29], "M5 PART B"),
    "r31": (list(IMAGES)[29:31], "M5 PART C"),
    "r32": (list(IMAGES)[31:33], "M5 PART D"),
    "r33": (list(IMAGES)[33:], "M5 PART E"),
}


def build(names):
    OUT.mkdir(parents=True, exist_ok=True)
    for name in names:
        source = (SOURCE if name in BATCHES["r13"][0] else SOURCE_M5 if name in BATCHES["r30"][0]
                  else SOURCE_M5C if name in BATCHES["r31"][0] else SOURCE_M5D if name in BATCHES["r32"][0]
                  else SOURCE_M5E if name in BATCHES["r33"][0]
                  else SOURCE_M4)
        artkit.save_png(IMAGES[name]().image(), OUT / f"{name}.png", source)
    print(f"{len(names)} briefing images in {OUT.relative_to(ROOT)}")


def review(rnd):
    names, batch = BATCHES[rnd]
    cols = 2
    rows = -(-len(names) // cols)
    sheet = raster.sheet(16 + cols * (W + 16), 56 + rows * (H + 30),
                         f"BRIEFING IMAGES (FINAL {rnd.upper()}): ONE PER PAGE",
                         f"PRODUCTION ART, {batch} - {rnd.upper()}")
    raster.draw_text(sheet, 16, 38, f"{len(names)} IMAGES {W}X{H} AT 1X, AS THE BRIEFING SCREEN DRAWS THEM ABOVE THE TEXT",
                     raster.LABEL)
    for i, name in enumerate(names):
        img = Image.open(OUT / f"{name}.png").convert("RGBA")
        x, y = 16 + (i % cols) * (W + 16), 54 + (i // cols) * (H + 30)
        sheet.alpha_composite(img, (x, y))
        raster.draw_text(sheet, x, y + H + 6, f"{name.upper()} ({artkit.colour_count([img])} COLOURS)", raster.LABEL_DIM)
    path = CONCEPT / f"briefing-images-final-{rnd}-a.png"
    sheet.convert("RGB").save(path, optimize=True)
    print(f"review: {path.relative_to(ROOT)}")


if __name__ == "__main__":
    args = [a for a in sys.argv[1:] if a != "--review"]
    rounds = [a for a in args if a in BATCHES]
    names = [a for a in args if a not in BATCHES]
    if "--review" not in sys.argv[1:]:
        build(names or list(IMAGES))
    for rnd in rounds or [ROUND]:
        review(rnd)
