#!/usr/bin/env python3
"""Production art: the Level 10 backdrop, the Nova Lagos evacuation corridor at first light
(design/campaign/act-2-homefront/level-10-evacuation-corridor; M5 part D batch, concept round 32).

Straight to production (user decision D10 = a of M5 part D): Level 08's production kit
(tools/art/backdrop_l08.py, imported as a library: its height-field lighting, Canvas, city tiles,
low roofs, parks, cross highways, burnt blocks, fires, smoke plumes, tower roofs and wall textures,
the banks, the composite as the game draws it and the checks) with Level 09's creep helpers
(tools/art/backdrop_l09.py: the teal-black ground creep, the violet chitin on roofs and walls, the
scorch, the burnt-out cars). The kit's module state (the level's data, sections, plans and output
folder) is pointed at Level 10 when this module loads (after backdrop_l09 has pointed it at Level 09
on its import), so the kit's draw and check functions see Level 10's timeline: 190 px/s, no holds,
so script time is real time, about 38,000 px.

**Time of day** (D11 = a): first light, as Level 09's end line promises. Every image gets the dawn
grade (dawn(): the colours a little less saturated, the darks lifted toward a cool blue-grey in the
west and a warm peach toward the east, the right edge of the play field), a little more with every
section, from the sprawl at dawn in section 1 to the morning sky over the lagoon in section 5. The
lagoon's water shows the deep layer, the morning sky's reflection, gold in the east. Lamps still burn
in sections 1-3, few in section 4, none in section 5. The key light stays the kit's (top left).

**Sections** (the README's Layout): 1 Eko spaceport (the aprons, taxiways with blue edge lights,
hangars, the five launch pads where the shuttles stand at t=1 and lift off by t=7); 2 the suburbs
(a motorway up the middle jammed with stalled cars, refugee columns as lights on its shoulders,
houses, parks and football pitches; CDF gunships on low-air heading the other way); 3 the maglev
viaduct between two infested districts (teal-black creep on the ground, violet chitin on the roofs
and the towers' upper floors, burnt blocks with fires, smoke plumes, a stalled maglev train); 4 the
coast road (a fishing village, the coast road, the beach, piers and sandbars, the capsized ferry on
its sandbar with the hatch the level's ferry secret sits on); 5 the open lagoon (shallows, fish-trap
stakes, buoys blinking). A cross highway hides every section seam (the last two as causeways over
the water, as Lagos's Third Mainland Bridge).

**Towers** (as Level 08, D1 = a of M5 part B): scenery in true perspective on the city lots, in the
creep districts of section 3 and the last blocks of section 2, each as tall as its spot allows
without leaning over the ferry or its hatch while on screen. Their walls are drawn the way the game
draws them since part C (TowerProjection.visibleSides): this module's composites use
draw_towers_fixed(); Level 08's and 09's review images keep the kit's old draw (their scripts are
untouched, so their outputs stay byte-identical).

**Ferry hatch** (the ferry secret, a destructible ground target with a hidden crate, as Level 09's
cocoon): assets/sprites/ferry-hatch_0..2 (36 x 32: intact with its amber emergency lamp, hit,
blown open with the crate in the dark hold) and ferry-hatch-break_0..7 (56 x 56), from a model in
this module (hatch()), rendered as Level 04's dugout. The level data names it as the target's
`sprite: ferry-hatch` (until then the pipeline would refuse unclaimed sprites: --props writes them
to a folder of your choice, see Run).

**Moving scenery** (motion budget: at most two moving piece ids on screen): the CDF gunships of
section 2 (one piece, `cdf-gunship`, Level 08's gunship model re-rendered at dawn, 16 headings),
the fires on burnt blocks in section 3, the drifting banks (light: haze, medium: smoke and sea mist,
blowing in from the sea on the east wind, so they drift west), the buoys' lamps in section 5.
Never more than two at once.

Outputs (assets/backdrop/level-10/, one PNG per tile set and piece of the backdrop block, named by
its id; headings and frames as <id>_<n>.png; wall textures by id):
  morning-sky                deep: the dawn sky's reflection, gold in the east (seen in the water)
  spaceport                  ground, section 1: Eko spaceport's apron, taxiways, hangars
  suburbs                    ground, section 2: the jammed motorway (x 176-304), houses, pitches
  viaduct                    ground, section 3: the maglev viaduct (x 180-300), creep districts
  coast                      ground, section 4: the village, the coast road, beach, water, sandbars
  open-lagoon                ground, section 5: the open lagoon's shallows (translucent), stakes
  haze, smoke-mist           low-air banks: dawn haze (light), smoke and sea mist (medium); drift
  mist, wisps                high-air (additive): thin dawn mist, smoke wisps
  cross-highway              ground: an elevated cross highway (a causeway) over every section seam
  launch-pads                ground, section 1: the five pads round the shuttles' first positions
  service-vehicles           ground, section 1: a fuel bowser, a tug and a stair truck
  cdf-gunship_0..15          low-air, section 2: the CDF gunship at 16 headings (0 nose up, clockwise)
  burnt-block-a/b, fire_0..3, smoke-column-a/b   section 3: collapsed blocks, flames, plumes
  maglev-train               ground, section 3: a stalled maglev train on the viaduct
  ferry                      ground, section 4: the capsized ferry on its sandbar (the hatch's spot)
  fishing-boats              ground, section 4: pirogues drawn up on the beach
  buoy_0..3                  ground, section 5: a channel buoy, its lamp blinking
  tower-<w>x<h>-h<NNN>-<style>   tower roofs (city a/b, creep, burnt)
  wall-low/-mid/-tall, wall-creep-mid/-tall, wall-burnt   wall textures
  assets/sprites/ferry-hatch_0..2, ferry-hatch-break_0..7   (--props; see above)
  design/campaign/.../level-10-evacuation-corridor/concept/backdrop-final-r32-a.png/.gif   review
                             sheet (tiles, pieces, the hatch, composites as the game draws them with
                             the shuttles and the hatch) and the liftoff (GIF)
  design/campaign/.../level-10-evacuation-corridor/concept/backdrop-proposal.yaml   the backdrop
                             block and the sections' tiles for the level data (--proposal)

Run: python3 tools/art/backdrop_l10.py [--proposal | --props [dir] | --review | --check |
     --strip t,.. out.png] [id ...]   (all: about 4 min; --props writes the hatch's sprites to
     assets/sprites, or to dir)
"""
import math
import sys
from concurrent.futures import ProcessPoolExecutor
from dataclasses import replace
from pathlib import Path

import numpy as np
import yaml
from PIL import Image

import artkit
import backdrop_l09 as k9          # noqa: E402  (its helpers; it points the kit at Level 09 on import)
import backdrop_l08 as k8
import l08_traffic
from artkit import DESIGN, ROOT, SPRITES, raster
from backdrop_l08 import (CHITIN, CONCRETE, CX, CY, EMBER, GREEN, GROUND, JOV, LOT0, LOT1, MARS, MID, NAVY, ORBIT,
                          PERIOD, ROAD, SCORCH, SCREEN, SODIUM, TILE_H, VGLOW, Canvas, W, add_glow, drop_shadow, finish,
                          finish_tile, java_round, low_roof, park, paste_sprite, quantize_lit, specks, to_img, tower_scale,
                          trees)

import ground_targets as gt  # noqa: E402  (concept script: scorch, light rim, hazard stripes)
from loot_targets import crumbled  # noqa: E402  (the break-apart's burn-down)
from parallax_r03 import noise  # noqa: E402  (concept script, unchanged)
from render.sdf import (Material, panel_lines, rotate_x, rotate_z, sd_box, sd_ellipsoid, sd_sphere,  # noqa: E402
                        subtract, union)

SCRIPT = "backdrop_l10.py"
SOURCE = artkit.source_note(SCRIPT, "M5 part D batch")
LEVEL_DIR = DESIGN / "campaign" / "act-2-homefront" / "level-10-evacuation-corridor"
CONCEPT = LEVEL_DIR / "concept"
PROPOSAL = CONCEPT / "backdrop-proposal.yaml"
OUT = ROOT / "assets" / "backdrop" / "level-10"
REVIEW = CONCEPT / "backdrop-final-r32-a.png"
FACTORS = dict(k8.FACTORS)
HAZE_COLOUR = "54484e"            # rose-grey: the dawn haze over the water
ATMOSPHERE = {
    "clear": {"wisps": "mist", "haze": 0.0},
    "light": {"banks": "haze", "wisps": "mist", "haze": 0.05},
    "medium": {"banks": "smoke-mist", "wisps": "wisps", "haze": 0.12},
}
DRIFT = {"haze": -8, "smoke-mist": -14}   # px/s: the east wind off the sea blows them west
RAMP = 4

# --------------------------------------------------------------------------- the level (script time = real time)
# Until Level 10's data.yaml exists: its sections from the README's Layout table, the ferry hatch as
# the README's estimate, the shuttles' pads and stations as the README's "The shuttles". The
# level-data step adopts or moves them; a re-run then follows the data.

DEFAULT_SECTIONS = [
    {"name": "Liftoff", "end": 20, "atmosphere": "light"},
    {"name": "Refugee Roads", "end": 70, "atmosphere": "clear"},
    {"name": "The Corridor", "end": 130, "atmosphere": "medium"},
    {"name": "Coast Road", "end": 175, "atmosphere": "light", "peak": {"from": 160, "to": 175, "atmosphere": "medium"}},
    {"name": "Orbital Corridor", "end": 200, "atmosphere": "clear"},
]
HATCH_SIZE = (36, 32)
DEFAULT_TARGETS = [
    {"target": "ferry hatch", "section": 4, "layer": "ground", "size": list(HATCH_SIZE), "sprite": "ferry-hatch", "hp": 8,
     "at": [[150.0, 322]]},
]
DEFAULT_PADS = [[240, 400], [176, 440], [304, 440], [176, 490], [304, 490]]   # screen [x, y below the top] at t=1
DEFAULT_STATIONS = [((240, 165), 9, 0.0), ((168, 215), 11, 0.25), ((312, 215), 10, 0.5), ((168, 270), 12, 0.75),
                    ((312, 270), 8, 0.1)]
SWAY = (16, 4)
LIFT = (1.0, 6.0)                  # t, seconds
CLIMB = (196.0, 2.0)
LOSS = (3, 118.0)                  # Lifeline Three, t


def level_data():
    path = LEVEL_DIR / "data.yaml"
    data = yaml.safe_load(path.read_text(encoding="utf-8")) if path.exists() else {}
    escort = data.get("escort") or {}
    lift = escort.get("liftoff") or {}
    stations = escort.get("stations")
    out = {"scroll_speed": data.get("scroll_speed", 190), "sections": data.get("sections") or DEFAULT_SECTIONS,
           "ground_targets": data.get("ground_targets") or DEFAULT_TARGETS, "waves": data.get("waves") or [],
           "backdrop": data.get("backdrop"), "own": path.exists(),
           "pads": lift.get("pads") or DEFAULT_PADS,
           "lift": (float(lift.get("t", LIFT[0])), float(lift.get("seconds", LIFT[1]))),
           "stations": [((s["at"][0], s["at"][1]), s["period"], s.get("phase", 0.0)) for s in stations]
           if stations else DEFAULT_STATIONS}
    return out


DATA = level_data()
SECTIONS = DATA["sections"]
SPEED = float(DATA["scroll_speed"])
STARTS = [0.0] + [float(s["end"]) for s in SECTIONS[:-1]]
END = float(SECTIONS[-1]["end"])
OUTRO_END = END + 15                   # LevelData.OUTRO_SECONDS

PLANS = {
    "suburbs": {"blocks": [(6, 84), (96, 170), (310, 384), (396, 474)],
                "roads": [(84, 96, "street"), (170, 310, "motorway"), (384, 396, "street"), (0, 6, "lane"),
                          (474, 480, "lane")]},
    "viaduct": {"blocks": [(6, 84), (96, 176), (304, 384), (396, 474)],
                "roads": [(84, 96, "street"), (176, 304, "viaduct"), (384, 396, "street"), (0, 6, "lane"),
                          (474, 480, "lane")]},
}
SECTION_GROUND = ["spaceport", "suburbs", "viaduct", "coast", "open-lagoon"]
SECTION_TILES = [["morning-sky", g] for g in SECTION_GROUND]
STAGE = {g: i / 4 for i, g in enumerate(SECTION_GROUND)}     # the dawn's progress per section, 0..1
MOTORWAY = (176, 304)
DECK = (214, 266)                      # the maglev viaduct's deck
SHORE = 168                            # the coast's waterline (x)

# --------------------------------------------------------------------------- point the kit at Level 10

k8.DATA = DATA
k8.SECTIONS, k8.SPEED, k8.STARTS, k8.END, k8.OUTRO_END = SECTIONS, SPEED, STARTS, END, OUTRO_END
k8.OUT, k8.PROPOSAL, k8.REVIEW = OUT, PROPOSAL, REVIEW
k8.SECTION_OF = SECTION_TILES
k8.PLANS.update(PLANS)
k8.SECTION_GROUND = SECTION_GROUND
k8.STRUCTURES, k8.WALK_ROOFS = {}, {}
k8._PLACES, k8._TOWERS = None, None
k8._cache.clear()
scroll_at, t_at, t_for_centre, seam_pos, section_of = k8.scroll_at, k8.t_at, k8.t_for_centre, k8.seam_pos, k8.section_of


def target_units():
    """Every ground target's unit over all difficulties: (target, section, sprite, x, layer position
    of its centre, w, h); it enters at the top edge at its t."""
    out = []
    for g in DATA["ground_targets"]:
        if "enemy" in g:
            continue
        w, h = g["size"]
        spots = {tuple(a) for v in [g] + [g.get(d, {}) for d in ("easy", "hard")] for a in v.get("at", [])}
        for t, x in sorted(spots):
            out.append((g["target"], int(g.get("section", 1)), g.get("sprite", "cargo-container"), float(x),
                        scroll_at(t) + SCREEN + h / 2, w, h))
    return out


def hatch_unit():
    return next(u for u in target_units() if u[2] == "ferry-hatch")


def ground_places(t):
    """k8.ground_places for Level 10: no walkers; the ferry hatch's box."""
    return [], [], [(x, pos, w, h) for _, _, _, x, pos, w, h in target_units()]


k8.ground_places = ground_places

# --------------------------------------------------------------------------- the dawn grade

DAWN_WARM = np.array([236, 166, 124], float)   # first light in the east: peach
DAWN_COOL = np.array([108, 124, 164], float)   # the west still blue-grey
SKY_GOLD = np.array([242, 196, 140], float)


def dawn(arr, x0=0.0, stage=0.0, lift=0.07, east=0.2, sat=0.88):
    """First light on a native float RGBA image whose left column lies at play-field x ``x0`` (None:
    the play field's middle for every column, for pieces placed at many x): the colours a little less
    saturated, the darks lifted toward a cool blue-grey in the west and a warm peach toward the east;
    ``stage`` (0 at the spaceport .. 1 over the open lagoon) lifts it a little more."""
    out = arr.copy()
    rgb = out[..., :3]
    lum = rgb @ np.array([0.3, 0.59, 0.11])
    rgb = lum[..., None] * (1 - sat) + rgb * sat
    w = arr.shape[1]
    xs = np.full(w, 0.5) if x0 is None else np.clip((x0 + np.arange(w) + 0.5) / W, 0, 1)
    g = (lift + 0.04 * stage) + (east + 0.08 * stage) * xs ** 1.4
    tint = DAWN_COOL[None, :] * (1 - xs[:, None] ** 1.2) + DAWN_WARM[None, :] * xs[:, None] ** 1.2
    dark = np.clip(1 - lum * 3.0, 0, 1)[..., None]
    out[..., :3] = np.clip(rgb * (1.04 + 0.12 * stage) + (tint / 255)[None, :, :] * g[None, :, None] * (0.45 + 0.55 * dark), 0, 1)
    return out


def water_dawn(arr, stage):
    """The dawn grade for a tile with water: the land as every tile, the water lifted less (it
    carries the sky's reflection already, and the deep layer's gold shows through it)."""
    land = dawn(arr, 0.0, stage)
    wet = dawn(arr, 0.0, stage, lift=0.03, east=0.05)
    water = (arr[..., 3] < 0.8)[..., None]
    return np.where(water, wet, land)


def graded(img, x0=None, colours=32, stage=0.5):
    """A finished kit image (Level 08's generators) with the dawn grade, re-posterized."""
    arr = np.array(img.convert("RGBA")).astype(np.float64) / 255
    return quantize_lit([to_img(dawn(arr, x0, stage))], colours)[0]


def east_lit(img, strength, colour=DAWN_WARM):
    """A bank or wisp tile lit from the east: its colour lifted toward the dawn across x."""
    a = np.array(img).astype(np.float64)
    xs = (np.arange(a.shape[1]) + 0.5) / a.shape[1]
    k = (strength * xs ** 1.5)[None, :, None]
    a[..., :3] = np.clip(a[..., :3] * (1 - k) + colour[None, None, :] * k, 0, 255)
    return Image.fromarray(a.astype(np.uint8), "RGBA")


def warm(img, strength, colour=DAWN_WARM):
    """A drifting bank lit by the dawn evenly (it wraps sideways, so no gradient across it)."""
    a = np.array(img).astype(np.float64)
    a[..., :3] = np.clip(a[..., :3] * (1 - strength) + colour[None, None, :] * strength, 0, 255)
    return Image.fromarray(a.astype(np.uint8), "RGBA")


def lamps_off(arr, rng, share):
    """Morning: about ``share`` of the sodium pools and lit windows go dark (bright warm pixels only)."""
    rgb = arr[..., :3]
    lum = rgb @ np.array([0.3, 0.59, 0.11])
    warm = (rgb[..., 0] > rgb[..., 2] * 1.6) & (lum > 0.45) & (arr[..., 3] > 0.9)
    off = warm & (rng.random(lum.shape) < share)
    arr[off, :3] = arr[off, :3] * 0.35 + NAVY[1] / 255 * 0.6
    return arr


WATER = np.array([24, 46, 62], float)     # the lagoon at dawn: dark teal-grey


def water_rgb(c, x0, x1, seed, depth=1.0):
    """The lagoon's water over native columns [x0, x1) of a wrapping canvas: ripples, the dawn's
    reflection streaks brighter toward the east."""
    hh, ww = c.hm.shape
    rip = noise(ww, hh, [24, 12, 6], seed)
    streak = np.repeat(noise(ww // 8, hh, [24, 12], seed + 2), 8, axis=1)
    a, b = int(x0 * c.f), int(x1 * c.f)
    xs = (np.arange(a, b) + 0.5) / ww
    sub = WATER[None, None, :] * depth * (0.82 + 0.36 * rip[:, a:b])[..., None]
    sub = sub + DAWN_WARM[None, None, :] * (0.16 * xs ** 1.6 * (0.4 + 1.1 * np.clip(streak[:, a:b] - 0.4, 0, 1)))[..., None]
    c.alb[:, a:b] = sub


# --------------------------------------------------------------------------- tile sets

def morning_sky(w, h):
    """Deep: the dawn sky mirrored in the water: pale blue-grey in the west, gold and peach toward
    the east, scattered clouds lit pink from below (seen only through the water)."""
    n = raster.fbm(w, h, 96, 1101, octaves=4, period=True)
    fine = raster.fbm(w, h, 24, 1103, octaves=3, period=True)
    xs = (np.arange(w)[None, :] + 0.5) / w
    base = DAWN_COOL * 0.48 * (1 - xs ** 2)[..., None] + SKY_GOLD * 0.5 * (xs ** 2)[..., None]
    cloud = np.clip((n - 0.5) * 2.4, 0, 1)
    pink = np.array([210, 140, 140], float)
    rgb = base * (0.86 + 0.24 * fine)[..., None] + pink * (cloud * (0.15 + 0.35 * xs ** 1.5))[..., None] * 0.5
    rgb = rgb * (1 - 0.18 * cloud[..., None] * (1 - xs[..., None]))
    return finish(np.dstack([np.clip(rgb / 255, 0, 1), np.ones((h, w))]), 16)


APRON = CONCRETE * 1.32 + NAVY[2] * 0.1
TAXI = JOV[3] * 0.8                     # taxiway paint
EDGE_BLUE = np.array([90, 140, 255], float)


def spaceport_tile():
    """Section 1: Eko spaceport: the concrete apron (x 84-396) in slabs with tyre and scorch marks,
    two taxiways up it (amber centre lines, blue edge lights), cross taxiways, short blast fences;
    hangars on the west side, the terminal and maintenance blocks on the east, service roads, flood
    masts; a few service vehicles."""
    h = TILE_H
    rng = np.random.default_rng(1111)
    c = Canvas(W, h, f=2, wrap=True, rgb=GROUND)
    c.grain(1111, 0.25)
    ax0, ax1 = 84, 396
    c.rect(ax0, 0, ax1, h, rgb=APRON, height=0.2)
    for x in range(ax0, ax1, 24):                                                 # slab joints
        c.rect(x, 0, x + 0.6, h, rgb=APRON * 0.82, height=0.2)
    for u in range(12, h, 24):
        c.rect(ax0, u, ax1, u + 0.6, rgb=APRON * 0.82, height=0.2)
    for _ in range(26):                                                           # tyre marks, scorch
        x, u = rng.uniform(ax0 + 10, ax1 - 10), rng.uniform(0, h)
        c.paint((x - 14, u - 20, x + 14, u + 20), mask_fn=lambda xx, uu, x=x, u=u:
                ((xx - x) / 12) ** 2 + ((uu - u) / 18) ** 2 <= 1, rgb=APRON * rng.uniform(0.88, 0.94), height=0.2)
    lights = []
    for tx in (150, 330):                                                         # taxiways up the apron
        for u in range(4, h, 16):
            c.rect(tx - 0.8, u, tx + 0.8, u + 9, rgb=TAXI, height=0.25)
        for u in range(6, h, 32):
            lights += [(tx - 22, u, EDGE_BLUE), (tx + 22, u, EDGE_BLUE)]
    for k in range(4):                                                            # cross taxiways
        u = 120 + k * 240
        for x in range(ax0, ax1, 16):
            c.rect(x, u - 0.8, x + 9, u + 0.8, rgb=TAXI * 0.9, height=0.25)
        for x in (ax0 + 40, ax1 - 40):                                            # hold-short bars
            c.rect(x - 0.8, u - 10, x + 0.8, u + 10, rgb=TAXI, height=0.25)
    for k in range(3):                                                            # blast fences
        x, u = rng.choice([ax0 + 10, ax1 - 40]), 60 + k * 320 + rng.uniform(-20, 20)
        for j in range(6):
            c.rect(x + j * 5, u, x + j * 5 + 4, u + 3, rgb=CONCRETE * 1.6, height=3.5)
    c.rect(70, 0, ax0, h, rgb=ROAD, height=0.0)                                   # service roads
    c.rect(ax1, 0, ax1 + 14, h, rgb=ROAD, height=0.0)
    for u in range(3, h, 12):
        c.rect(77, u, 77.6, u + 6, rgb=NAVY[3] * 0.7)
        c.rect(ax1 + 7, u, ax1 + 7.6, u + 6, rgb=NAVY[3] * 0.7)
    for k in range(h // 160):                                                     # hangars, doors facing the apron
        u0 = k * 160 + 6
        c.rect(4, u0, 66, u0 + 148, rgb=NAVY[2] * 1.1, height=6.5)
        for j in range(10):
            c.rect(6 + j * 6, u0 + 2, 6 + j * 6 + 0.8, u0 + 146, rgb=NAVY[2] * 0.85, height=6.5)      # roof ribs
        c.rect(62, u0 + 20, 66, u0 + 128, rgb=CONCRETE * 1.3, height=5.0)                          # the door
        lights.append((68, u0 + 74, SODIUM * 1.2))
    for k in range(h // PERIOD):                                                  # terminal and maintenance blocks
        u0, u1 = k * PERIOD + LOT0, k * PERIOD + LOT1
        c.rect(ax1 + 14, k * PERIOD - LOT0, W, k * PERIOD + LOT0, rgb=ROAD, height=0.0)
        low_roof(c, ax1 + 18, u0 + 4, W - 4, u1 - 4, rng, height=rng.uniform(4.0, 7.0), style="office")
    for u in range(40, h, 120):                                                   # flood masts
        for x in (ax0 + 3, ax1 - 3):
            c.disc(x, u, 2.0, rgb=CONCRETE * 1.5, height=9.0)
            lights.append((x, u, np.array([200, 210, 235], float)))
    arr = c.render()
    specks(arr, rng, W * h // 500)
    for lx, lu, col in lights:
        if col is EDGE_BLUE:
            add_glow(arr, lx, lu, 1.6, col * 0.55, wrap=True)
            arr[h - 1 - int(lu) % h, int(lx), :3] = col / 255
        else:
            add_glow(arr, lx, lu, 9.0 if col[2] > 220 else 4.4, col * (0.35 if col[2] > 220 else 1.2), wrap=True)
    arr = vehicles_on(arr, rng, ax0 + 12, ax1 - 12, 10)
    return finish_tile(dawn(arr, 0.0, STAGE["spaceport"]), 32)


VEHICLE = [np.array([200, 196, 180], float), JOV[3] * 0.85, np.array([150, 160, 170], float)]


def vehicles_on(arr, rng, x0, x1, count, wrap=True):
    """Small service vehicles (static, flat): pale or amber boxes 5 x 9 px with a darker cab end."""
    h = arr.shape[0]
    for _ in range(count):
        cx, cy = int(rng.uniform(x0, x1)), int(rng.uniform(0, h))
        col = VEHICLE[int(rng.integers(len(VEHICLE)))] / 255
        horiz = rng.random() < 0.5
        bw, bh = (9, 5) if horiz else (5, 9)
        for yy in range(bh):
            for xx in range(bw):
                r = (cy + yy) % h
                arr[r, cx + xx, :3] = col * (0.7 if (xx < 3 if horiz else yy < 3) else 1.0)
        for yy in range(bh):                                                      # shadow down and right
            arr[(cy + yy + 2) % h, cx + bw, :3] *= 0.6
        for xx in range(bw):
            arr[(cy + bh) % h, cx + xx + 1, :3] *= 0.6
    return arr


def suburbs_tile():
    """Section 2: the suburbs: house roofs on small lots, parks and football pitches, the motorway up
    the middle (x 176-304: four lanes each way, a median barrier) jammed with stalled cars, tail
    lights red; refugee columns walking its shoulders north, lights only (torches, phones)."""
    rng = np.random.default_rng(1121)
    arr = k8.city_tile("suburbs", 1121, "residential", park_share=0.1)
    h = TILE_H
    over = Canvas(W, h, f=2, wrap=True, alpha=0.0)
    x0, x1 = MOTORWAY
    over.rect(x0 - 6, 0, x0, h, rgb=CONCRETE * 1.3, height=0.6, alpha=1.0)        # verges
    over.rect(x1, 0, x1 + 6, h, rgb=CONCRETE * 1.3, height=0.6, alpha=1.0)
    over.rect(x0, 0, x1, h, rgb=ROAD * 1.15, height=0.2, alpha=1.0)
    mid = (x0 + x1) / 2
    over.rect(mid - 2, 0, mid + 2, h, rgb=CONCRETE * 1.5, height=1.4)             # the median barrier
    lane = (x1 - x0 - 4) / 8
    for k in range(1, 8):
        if k == 4:
            continue
        lx = x0 + k * lane + (2 if k > 4 else 0)
        for u in range(0, h, 12):
            over.rect(lx - 0.4, u, lx + 0.4, u + 6, rgb=NAVY[3] * 0.85, height=0.2)
    cars = []
    for k in range(8):                                                            # the jam: bumper to bumper
        cx = x0 + (k + 0.5) * lane + (2 if k >= 4 else 0)
        north = k >= 4 or rng.random() < 0.3                                      # even the southbound lanes
        u = rng.uniform(0, 12)
        while u < h - 12:
            if rng.random() < 0.86:
                long_ = rng.uniform(9, 11) if rng.random() < 0.85 else rng.uniform(14, 20)
                col = k9.HULL_GREY * 1.3 if rng.random() < 0.08 else \
                    [CONCRETE * 1.7, NAVY[2] * 1.3, MARS[2] * 0.8, ORBIT[2] * 0.7, JOV[2] * 0.8,
                     np.array([150, 150, 156], float)][int(rng.integers(6))] * rng.uniform(0.8, 1.1)
                cxx = cx + rng.uniform(-0.8, 0.8)
                over.rect(cxx - 2.6, u, cxx + 2.6, u + long_, rgb=col, height=1.6)
                over.rect(cxx - 2.0, u + long_ * 0.3, cxx + 2.0, u + long_ * 0.65, rgb=col * 0.6, height=1.9)
                cars.append((cxx, u, u + long_, north))
                u += long_ + rng.uniform(2, 4)
            else:
                u += rng.uniform(10, 26)
    lay = over.render()
    put = lay[..., 3] > 0.5
    arr[put] = lay[put]
    for cxx, ua, ub, north in cars:                                               # tail and head lights
        tail, head = (ua, ub) if north else (ub, ua)
        for dx in (-1.6, 1.6):
            r = h - 1 - int(tail) % h
            arr[r, int(cxx + dx), :3] = np.array([235, 50, 50]) / 255
            if rng.random() < 0.35:
                arr[h - 1 - int(head) % h, int(cxx + dx), :3] = np.array([255, 240, 210]) / 255 * 0.8
    for sx in (x0 - 3, x1 + 3, x0 - 12, x1 + 12):                                 # refugee columns, lights only
        u = rng.uniform(0, 30)
        while u < h:
            if rng.random() < 0.7:
                for j in range(int(rng.integers(3, 9))):
                    uu = (u + j * rng.uniform(2.5, 4)) % h
                    xx = sx + rng.uniform(-1.5, 1.5)
                    col = (JOV[4] * rng.uniform(0.6, 0.95) if rng.random() < 0.7 else np.array([220, 230, 255]) * 0.8)
                    arr[h - 1 - int(uu), int(xx), :3] = col / 255
                    if rng.random() < 0.25:
                        add_glow(arr, xx, uu, 1.6, col * 0.35, wrap=True)
            u += rng.uniform(18, 46)
    arr = pitches(arr, rng)
    return finish_tile(dawn(arr, 0.0, STAGE["suburbs"]), 32)


def pitches(arr, rng):
    """Football pitches on two lots per tile (grass in stripes, white lines, goals), over the roofs."""
    h = TILE_H
    c = Canvas(W, h, f=2, wrap=True, alpha=0.0)
    for k, (bx0, bx1) in ((2, (6, 84)), (7, (396, 474)), (4, (310, 384))):
        u0, u1 = k * PERIOD + LOT0 + 3, k * PERIOD + LOT1 - 3
        x0, x1 = bx0 + 3, bx1 - 3
        c.rect(x0, u0, x1, u1, rgb=GREEN[1] * 0.62, height=0.3, alpha=1.0)
        for j, u in enumerate(np.arange(u0, u1, 8)):
            if j % 2:
                c.rect(x0, u, x1, min(u + 8, u1), rgb=GREEN[1] * 0.7, height=0.3)
        px0, pu0, px1, pu1 = x0 + 6, u0 + 6, x1 - 6, u1 - 6
        line = dict(rgb=np.array([190, 196, 186], float) * 0.8, height=0.35)
        for a, b, cc, d in ((px0, pu0, px1, pu0 + 0.8), (px0, pu1 - 0.8, px1, pu1), (px0, pu0, px0 + 0.8, pu1),
                            (px1 - 0.8, pu0, px1, pu1), (px0, (pu0 + pu1) / 2 - 0.4, px1, (pu0 + pu1) / 2 + 0.4)):
            c.rect(a, b, cc, d, **line)
        c.ring((px0 + px1) / 2, (pu0 + pu1) / 2, 8, 0.8, **line)
        for gu in (pu0, pu1):                                                     # goals
            c.rect((px0 + px1) / 2 - 5, gu - 1, (px0 + px1) / 2 + 5, gu + 1, rgb=CONCRETE * 1.8, height=1.0)
    lay = c.render()
    put = lay[..., 3] > 0.5
    arr[put] = lay[put]
    return arr


def viaduct_tile():
    """Section 3: the maglev viaduct up the middle (its deck x 214-266, 9 px above the street: two
    guideways, a walkway, pylons' shadows) between two infested districts: residential and office
    roofs with teal-black creep blots spreading over roofs and streets, violet chitin on some roofs,
    scorch; the deck's shadow down its east side."""
    rng = np.random.default_rng(1131)
    arr = k8.city_tile("viaduct", 1132, "residential", park_share=0.04)
    h = TILE_H
    keep = np.zeros(arr.shape[:2], bool)
    keep[:, 170:310] = True
    arr = k9.creep_blots(arr, rng, 16, 10, 26, keep=keep)
    arr = k9.scorch_blots(arr, rng, 8, 8, 22)
    roofs = arr[..., 3] > 0.9
    nz = raster.fbm(W, h, 10, 1133, octaves=3, period=True)
    chit = roofs & (nz > 0.66) & ~keep                                            # violet chitin crusts
    arr[chit, :3] = (CHITIN[1] * 0.5 + NAVY[1] * 0.7) / 255 * (0.8 + 0.4 * nz[chit, None])
    pods = np.argwhere(chit)
    for r, c_ in pods[rng.choice(len(pods), size=min(len(pods), 90), replace=False)]:
        arr[r, c_, :3] = VGLOW[1] / 255 * 0.85
    arr = k9.wrecks_on(arr, rng, 180, 210, 10)
    arr = k9.wrecks_on(arr, rng, 270, 300, 10)
    s0, s1 = DECK
    shadow = 13
    arr[:, s1:s1 + shadow, :3] *= 0.55                                           # the deck's shadow
    deck = Canvas(W, h, f=2, wrap=True, alpha=0.0)
    deck.rect(s0, 0, s1, h, rgb=CONCRETE * 1.15, height=9.0, alpha=1.0)
    deck.rect(s0, 0, s0 + 2, h, rgb=CONCRETE * 1.5, height=10.0)
    deck.rect(s1 - 2, 0, s1, h, rgb=CONCRETE * 1.5, height=10.0)
    for gx in (s0 + 12, s1 - 12):                                                 # the guideways
        deck.rect(gx - 6, 0, gx + 6, h, rgb=NAVY[1] * 1.2, height=9.6)
        deck.rect(gx - 4.5, 0, gx - 3, h, rgb=ORBIT[2] * 0.7, height=10.2)
        deck.rect(gx + 3, 0, gx + 4.5, h, rgb=ORBIT[2] * 0.7, height=10.2)
    mid = (s0 + s1) / 2
    deck.rect(mid - 2, 0, mid + 2, h, rgb=CONCRETE * 1.3, height=9.2)              # the walkway
    for u in range(0, h, 48):                                                     # joints over the pylons
        deck.rect(s0 + 2, u, s1 - 2, u + 1, rgb=CONCRETE * 0.8, height=9.0)
    lay = deck.render()
    put = lay[..., 3] > 0.5
    arr[put] = lay[put]
    for u in range(0, h, 48):                                                     # the pylons' shadows below
        r = h - 1 - u
        arr[max(0, r - 8):r, s1 + shadow:s1 + shadow + 6, :3] *= 0.6
        add_glow(arr, mid, u + 24, 3.0, np.array([170, 190, 255]) * 0.5, wrap=True)   # deck lamps, cool
    arr = k9.dim_windows(arr, rng, 0.5)
    return finish_tile(dawn(arr, 0.0, STAGE["viaduct"]), 32)


def coast_tile():
    """Section 4: the lagoon shore: a fishing village of small roofs and palms (x 0-110), the coast
    road (x 116-146, two lanes, stalled cars and refugee lights), a sea wall and the beach (to x 168),
    the water (translucent: the morning sky shows in it) with sandbars, wooden piers out from the
    beach, fish-trap stakes."""
    h = TILE_H
    rng = np.random.default_rng(1141)
    c = Canvas(W, h, f=2, wrap=True, rgb=GROUND)
    c.grain(1141, 0.25)
    c.paint((SHORE, 0, W, h), rgb_fn=lambda xx, uu: np.zeros(xx.shape + (3,)), alpha=0.62, height=0.0)
    water_rgb(c, SHORE, W, 1143)
    sand = np.array([150, 132, 104], float) * 0.62
    c.rect(146, 0, SHORE + 3, h, rgb=sand, height=0.4)                            # the beach
    nz = raster.fbm(W * 2, h * 2, 20, 1145, octaves=3, period=True)
    hh, ww = c.hm.shape
    for k in range(4):                                                            # sandbars: dry cores, shallows round them
        cx, cu, rx, ru = rng.uniform(230, 440), k * 240 + rng.uniform(40, 200), rng.uniform(26, 50), rng.uniform(50, 90)

        def shoal(xx, uu, cx=cx, cu=cu, rx=rx, ru=ru, s=1.0):
            du = (uu - cu + h / 2) % h - h / 2
            ii = np.clip((uu * c.f).astype(int) % hh, 0, hh - 1)
            jj = np.clip((xx * c.f).astype(int), 0, ww - 1)
            return ((xx - cx) / (rx * s)) ** 2 + (du / (ru * s)) ** 2 + (nz[hh - 1 - ii, jj] - 0.5) * 0.9 <= 1
        c.paint((cx - rx * 1.5, cu - ru * 1.5, cx + rx * 1.5, cu + ru * 1.5), mask_fn=lambda xx, uu, f=shoal:
                f(xx, uu, s=1.4), rgb=WATER * 1.25 + sand * 0.35, alpha=0.62, height=0.0)
        c.paint((cx - rx, cu - ru, cx + rx, cu + ru), mask_fn=shoal, rgb=sand * 1.08, alpha=1.0, height=0.6)
    c.rect(140, 0, 146, h, rgb=CONCRETE * 1.3, height=2.0)                        # the sea wall
    c.rect(116, 0, 140, h, rgb=ROAD * 1.1, height=0.0)                            # the coast road
    for u in range(0, h, 12):
        c.rect(127.6, u, 128.4, u + 6, rgb=NAVY[3] * 0.8)
    lights = []
    for u in range(16, h, 64):
        lights.append((141, u))
    for pu, length in ((140, 120), (520, 80), (780, 150)):                         # wooden piers
        c.rect(SHORE - 6, pu, SHORE + length, pu + 7, rgb=JOV[1] * 0.7 + CONCRETE * 0.3, height=1.6, alpha=1.0)
        for x in range(SHORE, SHORE + length, 5):
            c.rect(x, pu, x + 0.6, pu + 7, rgb=JOV[1] * 0.45, height=1.6)
        c.rect(SHORE + length - 10, pu - 6, SHORE + length, pu + 13, rgb=JOV[1] * 0.7 + CONCRETE * 0.3, height=1.8,
               alpha=1.0)
    for k in range(3):                                                            # fish-trap stakes
        x0, u0 = rng.uniform(250, 420), k * 320 + rng.uniform(0, 200)
        ang = rng.uniform(-0.6, 0.6)
        for j in range(18):
            c.disc(x0 + math.sin(ang) * j * 6, u0 + math.cos(ang) * j * 6, 0.9, rgb=JOV[1] * 0.5, height=1.5, alpha=1.0)
    for k in range(h // 48):                                                      # the village
        u0 = k * 48 + 4
        c.rect(0, k * 48 - 3, 116, k * 48 + 3, rgb=ROAD * 0.9)
        x = 4
        while x < 108:
            bw = rng.uniform(14, 26)
            if x + bw > 112:
                break
            if rng.random() < 0.8:
                low_roof(c, x, u0 + rng.uniform(0, 3), x + bw, u0 + 40 - rng.uniform(0, 3), rng,
                         height=rng.uniform(1.8, 3.0), style="residential")
            else:
                c.spots.append((x + bw / 2, u0 + 20, "big"))
            x += bw + rng.uniform(2, 5)
    arr = c.render()
    water = arr[..., 3] < 0.8
    land = ~water
    specks_land = arr.copy()
    specks(specks_land, rng, 116 * h // 260)
    arr[land] = specks_land[land]
    arr = k9.wrecks_on(arr, rng, 118, 138, 8)
    for lx, lu in lights:
        add_glow(arr, lx, lu, 4.0, SODIUM * 1.1, wrap=True)
    u = 0.0                                                                       # refugees on the coast road
    while u < h:
        if rng.random() < 0.6:
            for j in range(int(rng.integers(3, 7))):
                uu = (u + j * 3) % h
                arr[h - 1 - int(uu), int(118 + rng.uniform(0, 3)), :3] = JOV[4] / 255 * rng.uniform(0.6, 0.9)
        u += rng.uniform(24, 60)
    tr = trees()
    for tx, tu, kind in c.spots:
        paste_sprite(arr, tr[kind][int(rng.integers(len(tr[kind])))], tx, tu, wrap=True)
    arr = lamps_off(arr, rng, 0.4)
    return finish_tile(water_dawn(arr, STAGE["coast"]), 32, water=0.62)


def open_lagoon_tile():
    """Section 5: the open lagoon toward the sea: water everywhere (translucent), lighter shallows
    over sand, a few dry shoals, lines of fish-trap stakes, a channel's darker water."""
    h = TILE_H
    rng = np.random.default_rng(1151)
    c = Canvas(W, h, f=2, wrap=True, rgb=GROUND, alpha=0.62)
    water_rgb(c, 0, W, 1153, depth=0.95)
    hh, ww = c.hm.shape
    xs = (np.arange(ww) + 0.5) / c.f
    channel = np.exp(-((xs - 300) / 60) ** 2)
    c.alb *= (1 - 0.25 * channel)[None, :, None]
    sand = np.array([150, 132, 104], float) * 0.62
    nz = raster.fbm(W * 2, h * 2, 24, 1155, octaves=3, period=True)
    for k in range(5):
        cx, cu, rx, ru = rng.uniform(30, 200) if k % 2 else rng.uniform(380, 460), k * 192 + rng.uniform(20, 170), \
            rng.uniform(22, 44), rng.uniform(40, 80)

        def shoal(xx, uu, cx=cx, cu=cu, rx=rx, ru=ru, s=1.0):
            du = (uu - cu + h / 2) % h - h / 2
            ii = np.clip((uu * c.f).astype(int) % hh, 0, hh - 1)
            jj = np.clip((xx * c.f).astype(int), 0, ww - 1)
            return ((xx - cx) / (rx * s)) ** 2 + (du / (ru * s)) ** 2 + (nz[hh - 1 - ii, jj] - 0.5) * 0.9 <= 1
        c.paint((cx - rx * 1.6, cu - ru * 1.6, cx + rx * 1.6, cu + ru * 1.6), mask_fn=lambda xx, uu, f=shoal:
                f(xx, uu, s=1.5), rgb=WATER * 1.3 + sand * 0.35, height=0.0)
        if k % 2:
            c.paint((cx - rx, cu - ru, cx + rx, cu + ru), mask_fn=lambda xx, uu, f=shoal: f(xx, uu, s=0.6),
                    rgb=sand * 1.08, alpha=1.0, height=0.5)
    for k in range(4):
        x0, u0 = rng.uniform(40, 440), k * 240 + rng.uniform(0, 160)
        ang = rng.uniform(-0.9, 0.9)
        for j in range(16):
            c.disc(x0 + math.sin(ang) * j * 6, u0 + math.cos(ang) * j * 6, 0.9, rgb=JOV[1] * 0.5, height=1.5, alpha=1.0)
    arr = c.render()
    return finish_tile(water_dawn(arr, STAGE["open-lagoon"]), 32, water=0.62)


# --------------------------------------------------------------------------- banks and wisps

def haze(w, h):
    """Light: thin dawn haze and the city's smoke behind, rose-grey, lit peach by the dawn (about
    14 % coverage); drifts west on the sea wind."""
    cols = [(54, 50, 60), (78, 72, 82), (104, 96, 102)]
    img = k8.periodic_bank(w, h, 1161, 0.74, 0.14, cols, 84, 3.0, JOV[3] * 0.1, 1163)
    return artkit.quantize_set([warm(img, 0.18)], 12)[0]


def smoke_mist(w, h):
    """Medium: smoke over the creep districts and the sea mist rolling in, grey-white, lit peach from
    the east (about 20 % coverage); drifts west on the sea wind."""
    cols = [(60, 58, 66), (92, 88, 96), (126, 120, 124)]
    img = k8.periodic_bank(w, h, 1171, 0.61, 0.16, cols, 116, 3.0, JOV[3] * 0.08, 1173)
    return artkit.quantize_set([warm(img, 0.15)], 12)[0]


def mist(w, h):
    """Clear and light: thin high-air mist (additive), pale and warm toward the east."""
    m = raster.fbm(w, h, 60, 1181, octaves=4, period=True)
    a = np.clip((m - 0.56) * 2.5, 0, 1) ** 1.5 * 80 / 255
    a = artkit.ordered_dither(a / 0.32, 5) * 0.32
    xs = (np.arange(w) + 0.5) / w
    rgb = (DAWN_COOL[None, :] * (1 - xs[:, None]) + DAWN_WARM[None, :] * xs[:, None]) / 255 * 0.8
    return to_img(np.dstack([np.broadcast_to(rgb[None], (h, w, 3)), a]))


def wisps(w, h):
    """Medium: smoke wisps (additive): faint grey streaks, warm in the east."""
    m = raster.fbm(w, h, 60, 1191, octaves=3, period=True)
    a = np.clip((m - 0.6) * 2.2, 0, 1) ** 1.6 * 0.26
    a = np.round(a * 6) / 6
    xs = (np.arange(w) + 0.5) / w
    rgb = (np.array([120, 116, 124])[None, :] * (1 - xs[:, None]) + np.array([170, 130, 110])[None, :] * xs[:, None]) / 255
    return to_img(np.dstack([np.broadcast_to(rgb[None], (h, w, 3)), a]))


# --------------------------------------------------------------------------- pieces

def pads_layout():
    """The launch pads round the shuttles' t=1 screen points: (piece x centre, ground centre, w, h,
    the pads' local (x, u))."""
    lift_t = DATA["lift"][0]
    pts = [(float(x), scroll_at(lift_t) + SCREEN - float(y)) for x, y in DATA["pads"]]
    x0, x1 = min(p[0] for p in pts) - 70, max(p[0] for p in pts) + 70
    p0, p1 = min(p[1] for p in pts) - 54, max(p[1] for p in pts) + 54
    w, h = int(2 * math.ceil((x1 - x0) / 2)), int(2 * math.ceil((p1 - p0) / 2))
    cx, cp = (x0 + x1) / 2, (p0 + p1) / 2
    local = [(x - (cx - w / 2), p - (cp - h / 2)) for x, p in pts]
    return cx, cp, w, h, local


PAD = (60, 44)


def launch_pads(w, h):
    """The five launch pads in the stations' double column: each a raised pale concrete pad with a
    hazard-striped edge, a scorched blast centre, its painted ring and corner lamps, a low blast
    fence along its south side; taxi paths joining them; a fuel line, a bowser by the lead pad."""
    _, _, _, _, local = pads_layout()
    rng = np.random.default_rng(1201)
    c = Canvas(w, h, f=2, alpha=0.0)
    lights = []
    for (ax, au), (bx, bu) in ((local[0], local[1]), (local[0], local[2]), (local[1], local[3]), (local[2], local[4])):
        c.line(ax, au, bx, bu, 16, rgb=APRON * 0.92, height=0.3, alpha=1.0)        # taxi paths
        c.line(ax, au, bx, bu, 0.8, rgb=TAXI, height=0.32)
    pw, ph = PAD
    for k, (x, u) in enumerate(local):
        c.rect(x - pw / 2 - 2, u - ph / 2 - 2, x + pw / 2 + 2, u + ph / 2 + 2, rgb=JOV[3] * 0.75, height=1.2, alpha=1.0)
        c.paint((x - pw / 2 - 2, u - ph / 2 - 2, x + pw / 2 + 2, u + ph / 2 + 2),
                mask_fn=lambda xx, uu: ((xx + uu) / 4) % 1 < 0.5, rgb=NAVY[0] * 1.2, height=1.2)   # hazard edge
        c.rect(x - pw / 2 + 1, u - ph / 2 + 1, x + pw / 2 - 1, u + ph / 2 - 1, rgb=APRON * 1.25, height=1.4)
        c.disc(x, u, 15, rgb=SCORCH * 1.7 + APRON * 0.35, height=1.4)              # the blast centre
        c.ring(x, u, 18, 1.2, rgb=np.array([210, 214, 220], float) * 0.75, height=1.45)
        for j in range(k + 1):                                                     # its number, in bars
            c.rect(x - pw / 2 + 4 + j * 3, u + ph / 2 - 6, x - pw / 2 + 5.4 + j * 3, u + ph / 2 - 2,
                   rgb=np.array([210, 214, 220], float) * 0.75, height=1.45)
        c.rect(x - pw / 2, u - ph / 2 - 8, x + pw / 2, u - ph / 2 - 5, rgb=CONCRETE * 1.7, height=4.0, alpha=1.0)
        for dx in (-pw / 2 + 2, pw / 2 - 2):
            for du in (-ph / 2 + 2, ph / 2 - 2):
                lights.append((x + dx, u + du))
    lx, lu = local[0]
    c.line(lx + PAD[0] / 2 + 4, lu, w - 8, lu - 30, 1.4, rgb=CONCRETE * 1.4, height=0.8, alpha=1.0)   # fuel line
    c.rect(lx + PAD[0] / 2 + 12, lu + 6, lx + PAD[0] / 2 + 21, lu + 24, rgb=VEHICLE[0], height=3.0, alpha=1.0)
    c.rect(lx + PAD[0] / 2 + 12, lu + 18, lx + PAD[0] / 2 + 21, lu + 24, rgb=VEHICLE[0] * 0.7, height=3.4)
    c.grain(1203, 0.14, cells=(4, 2))
    arr = c.render()
    arr = drop_shadow(arr, np.where(arr[..., 3] > 0.5, 1.4, 0.0), 0.4)
    for x, u in lights:
        add_glow(arr, x, u, 2.0, EDGE_BLUE * 0.5)
        arr[h - 1 - int(u), int(x), :3] = [0.6, 0.75, 1.0]
    cx, _, _, _, _ = pads_layout()
    return finish(dawn(k9.clear_rim(arr), cx - w / 2, STAGE["spaceport"]), 32)


def service_vehicles(w, h):
    """A fuel bowser, a tug and a stair truck parked on the apron (static), with their shadows."""
    c = Canvas(w, h, f=4, alpha=0.0)
    for x0, u0, x1, u1, col, ht in ((6, 8, 16, 34, VEHICLE[0], 4.0), (22, 10, 30, 20, VEHICLE[1], 3.0),
                                    (34, 8, 42, 30, VEHICLE[2], 3.5)):
        c.rect(x0, u0, x1, u1, rgb=col, height=ht, alpha=1.0)
        c.rect(x0 + 1, u1 - 6, x1 - 1, u1 - 1, rgb=col * 0.7, height=ht + 0.6)     # the cab
    c.rect(36, 12, 40, 26, rgb=VEHICLE[2] * 1.2, height=5.5)                       # the stairs
    for u in range(13, 26, 2):
        c.rect(36, u, 40, u + 0.6, rgb=VEHICLE[2] * 0.8, height=5.5)
    arr = c.render(ambient=0.34)
    arr = drop_shadow(arr, np.where(arr[..., 3] > 0.5, 4.0, 0.0), 0.42)
    add_glow(arr, 11, 33, 1.6, JOV[4] * 0.6)                                       # a beacon
    return finish(dawn(k9.clear_rim(arr), None, STAGE["spaceport"]), 24)


def gunship_frames(w, h, n):
    """The CDF gunship (Level 08's production model, l08_traffic) at ``n`` headings, at dawn."""
    imgs = l08_traffic.frames("gunship", n)
    arrs = [np.array(i.convert("RGBA")).astype(np.float64) / 255 for i in imgs]
    return l08_traffic.quantize([to_img(dawn(a, None, STAGE["suburbs"])) for a in arrs])


def maglev_train(w, h):
    """A stalled maglev train on the viaduct's west guideway: three articulated cars, pale with a
    green stripe along the roof, the nose up the line; its windows dark."""
    c = Canvas(w, h, f=4, alpha=0.0)
    cx = w / 2
    body = np.array([196, 200, 196], float) * 0.72
    cars = [(6, h * 0.34), (h * 0.34 + 3, h * 0.67), (h * 0.67 + 3, h - 8)]
    for k, (u0, u1) in enumerate(cars):
        nose = 10 if k == 2 else 0
        c.paint((cx - 6, u0, cx + 6, u1), mask_fn=lambda xx, uu, u1=u1, nose=nose:
                np.abs(xx - cx) <= 5.5 - np.clip((uu - (u1 - nose)) / max(nose, 1) * 4.5, 0, 4.5) * (nose > 0),
                rgb=body, height=3.0, alpha=1.0)
        c.rect(cx - 1.2, u0 + 2, cx + 1.2, u1 - 2 - nose, rgb=GREEN[2] * 0.55, height=3.3)
        c.rect(cx - 5.5, u0 + 3, cx - 4.4, u1 - 3 - nose, rgb=NAVY[0] * 1.3, height=2.8)
        c.rect(cx + 4.4, u0 + 3, cx + 5.5, u1 - 3 - nose, rgb=NAVY[0] * 1.3, height=2.8)
    arr = c.render(ambient=0.34)
    arr = drop_shadow(arr, np.where(arr[..., 3] > 0.5, 3.0, 0.0), 0.45)
    return finish(dawn(k9.clear_rim(arr), DECK[0] + 12 - w / 2, STAGE["viaduct"]), 24)


FERRY = (132, 264)
FERRY_ANGLE = 0.1                      # rad: the hull lies a little askew on the bar
FERRY_HULL = (30, 112)                 # half beam, half length
HATCH_LOCAL = (13.0, -26.0)            # the hatch on the hull: (across, along) from the hull's middle
HULL_RED = np.array([150, 62, 44], float) * 0.62


def ferry_frame():
    """(hull centre x, u) in the ferry piece and the axes: along (up the bow), across (to starboard)."""
    w, h = FERRY
    ca, sa = math.cos(FERRY_ANGLE), math.sin(FERRY_ANGLE)
    return (w / 2, h / 2 + 8), (sa, ca), (ca, -sa)


def ferry_hatch_local():
    (hx, hu), along, across = ferry_frame()
    s, l = HATCH_LOCAL
    return hx + across[0] * s + along[0] * l, hu + across[1] * s + along[1] * l


def ferry(w, h):
    """The capsized ferry on a sandbar: the upturned hull, faded red antifouling with rust streaks,
    its keel, the bilge rounding into the water line, two propellers and rudders at the stern; a
    patched plate on the hull's flat (where the hatch, a ground target of its own, sits); the sandbar
    under it with the shallows round, flotsam, an oil sheen, a lifebuoy."""
    rng = np.random.default_rng(1211)
    c = Canvas(w, h, f=2, alpha=0.0)
    (hx, hu), along, across = ferry_frame()
    bw, bl = FERRY_HULL
    sand = np.array([150, 132, 104], float) * 0.62
    nz = raster.fbm(w * 2, h * 2, 14, 1213, octaves=3, period=False)

    def bar(xx, uu, s):
        ii = np.clip(((h - uu) * 2).astype(int), 0, h * 2 - 1)
        jj = np.clip((xx * 2).astype(int), 0, w * 2 - 1)
        return ((xx - w * 0.52) / (w * 0.46 * s)) ** 2 + ((uu - h * 0.4) / (h * 0.36 * s)) ** 2 + (nz[ii, jj] - 0.5) * 0.8 <= 1
    c.paint((0, 0, w, h), mask_fn=lambda xx, uu: bar(xx, uu, 1.12), rgb=WATER * 1.3 + sand * 0.35, alpha=0.62, height=0.0)
    c.paint((0, 0, w, h), mask_fn=lambda xx, uu: bar(xx, uu, 0.9), rgb=sand * 1.05, alpha=1.0, height=0.5)

    def hull_coords(xx, uu):
        dx, du = xx - hx, uu - hu
        return dx * across[0] + du * across[1], dx * along[0] + du * along[1]

    def hull(xx, uu):
        s, l = hull_coords(xx, uu)
        half = bw * np.sqrt(np.clip((bl - l) / 46, 0, 1))                         # the bow tapers
        half = np.where(l < -bl + 10, bw * np.clip((l + bl) / 10, 0.6, 1), half)  # a squarer stern
        return (np.abs(s) <= half) & (l >= -bl) & (l <= bl)

    def hull_height(xx, uu):
        s, l = hull_coords(xx, uu)
        half = np.maximum(bw * np.sqrt(np.clip((bl - l) / 46, 0, 1)), 1)
        return 2.0 + 7.0 * np.sqrt(np.clip(1 - (s / half) ** 2, 0, 1))
    c.paint((0, 0, w, h), mask_fn=hull, rgb=HULL_RED, alpha=1.0, height_fn=hull_height)
    c.paint((0, 0, w, h), mask_fn=lambda xx, uu: hull(xx, uu) & (np.abs(hull_coords(xx, uu)[0]) < 1.4),
            rgb=HULL_RED * 0.6, lift=1.2)                                          # the keel
    for side in (-1, 1):                                                           # bilge keels
        c.paint((0, 0, w, h), mask_fn=lambda xx, uu, side=side: hull(xx, uu)
                & (np.abs(hull_coords(xx, uu)[0] - side * bw * 0.62) < 0.8)
                & (np.abs(hull_coords(xx, uu)[1]) < bl * 0.5), rgb=HULL_RED * 0.7, lift=0.6)
    for k in range(18):                                                            # rust streaks
        s0, l0 = rng.uniform(-bw * 0.8, bw * 0.8), rng.uniform(-bl * 0.8, bl * 0.7)
        c.paint((0, 0, w, h), mask_fn=lambda xx, uu, s0=s0, l0=l0: hull(xx, uu)
                & (np.abs(hull_coords(xx, uu)[0] - s0) < rng.uniform(0.6, 1.4))
                & (np.abs(hull_coords(xx, uu)[1] - l0) < rng.uniform(4, 14)),
                rgb=MARS[1] * 0.55 + JOV[1] * 0.3)
    px, pu = ferry_hatch_local()                                                   # the patched plate
    c.rect(px - 15, pu - 13, px + 15, pu + 13, rgb=HULL_RED * 0.85, lift=0.2)
    for sx in (-1, 1):                                                             # propellers and rudders
        sp = (hx + across[0] * sx * 9 + along[0] * (-bl + 4), hu + across[1] * sx * 9 + along[1] * (-bl + 4))
        c.disc(sp[0], sp[1], 4.2, rgb=JOV[2] * 0.75, height=8.5)
        c.disc(sp[0], sp[1], 1.4, rgb=JOV[2] * 0.5, height=9.2)
        rp = (sp[0] - along[0] * 7, sp[1] - along[1] * 7)
        c.line(rp[0], rp[1], rp[0] - along[0] * 6, rp[1] - along[1] * 6, 1.6, rgb=HULL_RED * 0.8, height=8.0)
    c.grain(1215, 0.26, cells=(4, 2))
    arr = c.render(ambient=0.32)
    hullmask = np.zeros(arr.shape[:2], bool)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64) + 0.5
    hullmask = hull(xx, h - yy)
    edge = hullmask & (k8.box_blur(hullmask.astype(float), 2) < 0.85)
    arr[edge, :3] *= 0.7                                                           # wet along the water line
    for _ in range(40):                                                            # flotsam
        fx, fu = rng.uniform(4, w - 4), rng.uniform(4, h - 4)
        r = h - 1 - int(fu)
        if not hullmask[r, int(fx)] and arr[r, int(fx), 3] > 0:
            arr[r, int(fx):int(fx) + int(rng.integers(1, 4)), :3] = (SCORCH * 1.6 + JOV[1] * 0.4) / 255
    lx, lu = w * 0.82, h * 0.22                                                    # a lifebuoy
    for a in np.linspace(0, 2 * math.pi, 16, endpoint=False):
        r_ = h - 1 - int(lu + math.sin(a) * 2.6)
        col = np.array([240, 120, 40]) if int(a / (math.pi / 2)) % 2 else np.array([230, 230, 220])
        arr[r_, int(lx + math.cos(a) * 2.6), :3] = col / 255 * 0.85
        arr[r_, int(lx + math.cos(a) * 2.6), 3] = 1.0
    for _ in range(5):                                                             # the oil sheen
        add_glow(arr, rng.uniform(w * 0.1, w * 0.3), rng.uniform(h * 0.5, h * 0.9), 6.0, CHITIN[2] * 0.08)
    arr = drop_shadow(arr, np.where(hullmask, 6.0, 0.0), 0.35)
    return finish(dawn(k9.clear_rim(arr), ferry_x() - w / 2, STAGE["coast"]), 32, alpha_levels=4, solid=0.6)


def fishing_boats(w, h):
    """Pirogues drawn up on the beach: long narrow wooden hulls, painted bands, one with a tarp."""
    rng = np.random.default_rng(1221)
    c = Canvas(w, h, f=4, alpha=0.0)
    paints = [GREEN[2] * 0.6, MARS[2] * 0.7, ORBIT[2] * 0.7, JOV[3] * 0.7]
    for k in range(4):
        x, u, ang = 8 + k * 11 + rng.uniform(-2, 2), h / 2 + rng.uniform(-6, 6), rng.uniform(-0.25, 0.25)
        ln, bw = rng.uniform(30, 40), 3.6
        ca, sa = math.cos(ang), math.sin(ang)
        mask = (lambda xx, uu, x=x, u=u, ln=ln, ca=ca, sa=sa: np.abs((xx - x) * ca - (uu - u) * sa)
                <= bw * np.sqrt(np.clip(1 - (((xx - x) * sa + (uu - u) * ca) / (ln / 2)) ** 2, 0, 1)))
        c.paint((x - 8, u - ln / 2 - 2, x + 8, u + ln / 2 + 2), mask_fn=mask, rgb=JOV[1] * 0.8, height=2.0, alpha=1.0)
        col = paints[k % len(paints)]
        c.paint((x - 8, u - ln / 2 - 2, x + 8, u + ln / 2 + 2),
                mask_fn=lambda xx, uu, m=mask, x=x, u=u, ca=ca, sa=sa: m(xx, uu)
                & (np.abs(np.abs((xx - x) * ca - (uu - u) * sa) - 2.8) < 0.7), rgb=col, height=2.2)
        c.paint((x - 8, u - ln / 2 - 2, x + 8, u + ln / 2 + 2),
                mask_fn=lambda xx, uu, m=mask, x=x, u=u, ca=ca, sa=sa: m(xx, uu)
                & (np.abs((xx - x) * ca - (uu - u) * sa) < 1.8), rgb=JOV[1] * 0.45, height=1.0)
    arr = c.render(ambient=0.34)
    arr = drop_shadow(arr, np.where(arr[..., 3] > 0.5, 2.0, 0.0), 0.4)
    return finish(dawn(k9.clear_rim(arr), None, STAGE["coast"]), 24)


def buoy_frames(w, h, n):
    """A channel buoy (red, with its cage and lamp) in its ring of ripples; the lamp blinks: lit on
    the first frame, fading over the next, dark on the last two."""
    c = Canvas(w, h, f=4, alpha=0.0)
    cx, cu = w / 2, h / 2
    c.disc(cx, cu, 4.2, rgb=MARS[2] * 0.75, height=2.5, alpha=1.0)
    c.ring(cx, cu, 4.2, 1.0, rgb=MARS[2] * 0.5, height=2.6)
    c.disc(cx, cu, 1.6, rgb=CONCRETE * 1.5, height=4.0)
    base = c.render(ambient=0.36)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64) + 0.5
    d = np.hypot(xx - cx, yy - (h - cu))
    ring = (np.abs(d - 6.2) < 0.6) & (base[..., 3] < 0.5)
    base[ring, :3] = WATER / 255 * 1.6
    base[ring, 3] = 0.6
    out = []
    for k, glow in enumerate((1.0, 0.45, 0.0, 0.0)[:n]):
        arr = base.copy()
        if glow:
            add_glow(arr, cx, cu, 2.6, np.array([255, 70, 60]) * 0.55 * glow)
            near = (d < 4.0) & (arr[..., 3] < 0.5)
            arr[near, :3] = np.array([255, 90, 70]) / 255 * 0.7 * glow
            arr[near, 3] = 0.6
            arr[h - 1 - int(cu), int(cx), :3] = np.array([1.0, 0.5, 0.45]) * (0.6 + 0.4 * glow)
        out.append(to_img(dawn(arr, None, STAGE["open-lagoon"])))
    return quantize_lit(out, 16)


# --------------------------------------------------------------------------- the ferry hatch (a ground target)

HATCH_COLOURS = 32
HATCH_BREAK, HATCH_CRUMBLE, HATCH_CANVAS = 8, 3, 56
PLATE_, RIM_, DOOR_, LAMP_, VOID_, CRATE_, AMBER_, CHAR_ = range(8)


def hatch_materials(state):
    """The hull's faded red antifouling (the ferry piece's), the hatch's steel rim and its door in a
    worn white, the amber emergency lamp, the dark hold, the CDF crate in the loot targets' amber."""
    lamp = (1.6, 1.0, 0.2) if state == 0 else (0.7, 0.36, 0.06) if state == 1 else (0.0, 0.0, 0.0)
    return [Material((0.42, 0.18, 0.12), metal=0.25, shininess=24, spec=0.25, pattern=panel_lines(7.0, 0.05, 0.84)),
            Material((0.46, 0.45, 0.42), metal=0.6, shininess=60, spec=0.6),
            Material((0.62, 0.62, 0.58), metal=0.4, shininess=40, spec=0.45),
            Material((0.35, 0.22, 0.05), emission=lamp),
            Material((0.02, 0.02, 0.03), metal=0.0, shininess=4, spec=0.0),
            Material((0.36, 0.38, 0.30), metal=0.3, shininess=30, spec=0.3),
            Material((0.98, 0.66, 0.14), metal=0.0, shininess=8, spec=0.05, pattern=gt.hazard_stripes),
            Material((0.08, 0.07, 0.07), metal=0.1, shininess=10, spec=0.1)]


def hatch(state):
    """The hull plate with its oval watertight hatch: ``state`` 0 shut (the rim, the door with its
    hand wheel and six dogs, the emergency lamp), 1 hit (the door dented and torn at one edge), 2
    blown open (the door gone, the bent rim round the dark hold and the crate down in it)."""
    def scene(p):
        plate = (sd_box(p, (0.0, 0.0, 0.8), (16.6, 14.6, 1.4), 1.6), PLATE_)
        rim_out = sd_ellipsoid(p, (0.0, 0.0, 2.4), (11.0, 9.0, 1.9))
        rim_in = sd_ellipsoid(p, (0.0, 0.0, 2.4), (8.4, 6.6, 4.0))
        rim = (np.maximum(rim_out, -rim_in), RIM_)
        lamp = (sd_sphere(p, (12.6, 10.6, 2.8), 2.1), LAMP_)
        lamp_base = (sd_box(p, (12.6, 10.6, 1.6), (2.4, 2.4, 0.8), 0.4), RIM_)
        if state < 2:
            door = sd_ellipsoid(p, (0.0, 0.0, 2.7), (8.6, 6.8, 1.5))
            wheel = np.maximum(sd_ellipsoid(p, (0.0, 0.0, 4.0), (3.6, 3.6, 0.9)), -sd_ellipsoid(p, (0.0, 0.0, 4.0), (2.4, 2.4, 3.0)))
            dogs = [(sd_box(p, (np.cos(a) * 9.6, np.sin(a) * 7.8, 3.2), (1.0, 1.0, 0.8), 0.3), RIM_)
                    for a in np.linspace(0, 2 * np.pi, 6, endpoint=False) + 0.3]
            parts = [plate, rim, (door, DOOR_), (wheel, RIM_), lamp, lamp_base, *dogs]
            d, m = union(*parts)
            if state == 1:
                d, m = subtract((d, m), sd_sphere(p, (4.0, -3.0, 5.2), 3.6))
                d, m = subtract((d, m), sd_box(p, (-7.0, 4.5, 3.0), (2.4, 1.6, 2.0)))
            return d, m
        hold = (sd_ellipsoid(p, (0.0, 0.0, 1.6), (8.4, 6.6, 1.2)), VOID_)
        crate = [(sd_box(p, (-1.5, -0.5, 1.7), (5.0, 3.4, 1.0), 0.4), CRATE_),
                 (sd_box(p, (-5.0, -0.5, 1.9), (1.6, 3.4, 1.0), 0.3), AMBER_)]
        bent = (sd_box(rotate_z(p - np.array([10.5, -9.0, 2.6]), 0.6), (0.0, 0.0, 0.0), (5.0, 3.6, 0.7), 0.6), DOOR_)
        d, m = union(plate, rim, lamp_base, bent)
        d, m = subtract((d, m), sd_ellipsoid(p, (0.0, 0.0, 2.4), (8.4, 6.6, 4.0)))
        d, m = union((d, m), hold, *crate)
        char = [(sd_sphere(p, (x, y, 2.4), r), CHAR_) for x, y, r in ((9.0, 3.0, 2.2), (-8.0, -6.0, 2.0), (-10.0, 5.0, 1.6))]
        return union((d, m), *char)
    return scene


def hatch_frame(s):
    """State ``s`` (0 shut, 1 hit, 2 opened) as Level 04's dugout: the model at 8x, the light rim
    on the standing frames, scorch on the hit and opened ones."""
    w, h = HATCH_SIZE
    hi, factor = artkit.render_hi(hatch(s), hatch_materials(s), (w, h), float(w), z_top=12.0, steps=160)
    arr = np.array(artkit.native(hi, factor, crisp=60)).astype(np.float64)
    if s < 2:
        arr = gt.light_rim(arr)
    if s >= 1:
        rng = np.random.default_rng(41 + s)
        arr = gt.scorch(arr, [(rng.uniform(0.3, 0.7) * w, rng.uniform(0.3, 0.7) * h, rng.uniform(3, 6)) for _ in range(2 * s)],
                        11 + s)
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")


def hatch_pieces(seed=10):
    rng = np.random.default_rng(seed)
    w, h = HATCH_SIZE
    cw, ch = w / 3, h / 2
    out = []
    for cy in range(2):
        for cx in range(3):
            centre = np.array([(cx + 0.5) * cw - w / 2, h / 2 - (cy + 0.5) * ch, 2.0])
            direction = centre[:2] / max(np.hypot(*centre[:2]), 1) + rng.uniform(-0.25, 0.25, 2)
            out.append((centre, (cw / 2, ch / 2), direction * rng.uniform(1.6, 2.2), rng.uniform(-0.25, 0.25),
                        rng.uniform(-0.3, 0.3)))
    return out


def hatch_break_frame(f):
    """Break-apart frame ``f``: the hit plate and hatch in six pieces flying apart, tumbling and
    charring, then crumbling away (the cocoon's method, Level 04's dugout's)."""
    base, base_mats = hatch(1), hatch_materials(1)
    char = 1 - 0.07 * f
    transforms = []
    for centre, half, velocity, spin, tumble in hatch_pieces():
        offset = np.array([velocity[0] * (f + 1), velocity[1] * (f + 1), -0.3 * f])

        def to_local(p, c=centre, o=offset, s=spin * (f + 1), t=tumble * (f + 1)):
            return rotate_x(rotate_z(p - c - o, s), t) + c
        transforms.append((to_local, centre, half))
    mats = []
    for to_local, _, _ in transforms:
        for m in base_mats:
            pattern = (lambda p, n, f_=m.pattern, tl=to_local: f_(tl(p), n)) if m.pattern else None
            mats.append(replace(m, albedo=tuple(np.array(m.albedo) * char), pattern=pattern,
                                emission=tuple(np.array(m.emission) * char)))
    count = len(base_mats)

    def scene(p):
        best_d, best_m = None, None
        for i, (to_local, centre, half) in enumerate(transforms):
            q = to_local(p)
            d, m = base(q)
            d = np.maximum(d, sd_box(q, (centre[0], centre[1], 0), (half[0], half[1], 10.0)))
            m = m + i * count
            if best_d is None:
                best_d, best_m = d, m
            else:
                closer = d < best_d
                best_d, best_m = np.where(closer, d, best_d), np.where(closer, m, best_m)
        return best_d, best_m
    e = HATCH_CANVAS
    hi, factor = artkit.render_hi(scene, mats, (e, e), float(e), z_top=12.0, steps=150)
    return crumbled(artkit.native(hi, factor, crisp=60), f - (HATCH_BREAK - HATCH_CRUMBLE) + 1)


def _hatch_job(job):
    kind, i = job
    return hatch_frame(i) if kind == "frame" else hatch_break_frame(i)


def hatch_images():
    jobs_ = [("frame", s) for s in range(3)] + [("break", f) for f in range(HATCH_BREAK)]
    with ProcessPoolExecutor() as pool:
        images = list(pool.map(_hatch_job, jobs_))
    done = artkit.quantize_set(images, HATCH_COLOURS)
    return done[:3], done[3:]


def build_props(folder=None):
    """The hatch's sprites (ferry-hatch_0..2, ferry-hatch-break_0..7, one palette) into
    assets/sprites, or into ``folder`` (while no level data claims them)."""
    frames, breaks = hatch_images()
    if folder is None:
        artkit.write_frames("ferry-hatch", frames, SOURCE)
        artkit.write_frames("ferry-hatch-break", breaks, SOURCE)
        where = SPRITES
    else:
        where = Path(folder)
        where.mkdir(parents=True, exist_ok=True)
        for name, imgs in (("ferry-hatch", frames), ("ferry-hatch-break", breaks)):
            for i, img in enumerate(imgs):
                artkit.save_png(img, where / f"{name}_{i}.png", SOURCE)
    print(f"ferry-hatch: 3 frames {frames[0].size}, break-apart {HATCH_BREAK} frames {breaks[0].size}, "
          f"{artkit.colour_count(frames + breaks)} colours, in {where}")


def hatch_sprites():
    """The hatch's frames for the review: assets/sprites if written there, else the scratch folder
    of the last --props run (HATCH_DIR), else rendered now."""
    for folder in (SPRITES, HATCH_DIR):
        if folder and (Path(folder) / "ferry-hatch_0.png").exists():
            load = lambda n, k: Image.open(Path(folder) / f"{n}_{k}.png").convert("RGBA")
            return [load("ferry-hatch", k) for k in range(3)], [load("ferry-hatch-break", k) for k in range(HATCH_BREAK)]
    return hatch_images()


HATCH_DIR = None

# --------------------------------------------------------------------------- towers (dawn)

FOOTPRINTS = k9.FOOTPRINTS
STYLES = k9.STYLES
WALLS = {k: v for k, v in k9.WALLS.items() if not k.startswith("wall-arcology")}
wall_for, tower_id = k9.wall_for, k9.tower_id


def tower_roof(foot, h, style):
    if style == "creep":
        img = k8.tower_roof(foot, h, "a" if (foot[0] + int(h * 100)) % 2 else "b")
        arr = np.array(img.convert("RGBA")).astype(np.float64) / 255
        arr = k9.creep_overlay(arr, foot[0] * 7 + int(h * 100), 0.75)
        return quantize_lit([to_img(dawn(arr, None, STAGE["viaduct"]))], 28)[0]
    return graded(k8.tower_roof(foot, h, style), None, 28, STAGE["viaduct"])


def wall_texture(name):
    w, storeys, px, style = WALLS[name]
    base = k8.wall_texture(w, storeys, px, "city" if style == "creep" else style)
    arr = np.array(base.convert("RGBA")).astype(np.float64) / 255
    if style == "creep":
        arr = k9.creep_overlay(arr, storeys * 31 + w, 0, rows_from_top=int(storeys * px * 0.45))
    return artkit.quantize_set([to_img(dawn(arr, None, STAGE["viaduct"]))], 24)[0]


def draw_towers_fixed(backdrop, placed_all, frame, t):
    """The kit's draw_towers with the walls the game draws (TowerProjection.visibleSides, fixed in
    part C): a west wall shows when the roof's left edge lies right of the foot's (rx > fx), an east
    wall when its right edge lies left of the foot's (rx1 < fx1)."""
    towers = [p for p in placed_all if "tower" in backdrop["pieces"][p["piece"]]]
    towers = sorted(enumerate(towers), key=lambda ip: (backdrop["pieces"][ip[1]["piece"]]["tower"]["height"], ip[0]))
    for _, placed in towers:
        if not k8.on_screen(backdrop, placed, t):
            continue
        spec = backdrop["pieces"][placed["piece"]]["tower"]
        (fx, fy, fw, fh), (rx, ry, rw, rh), k = k8.tower_geometry(backdrop, placed, t)
        tex = k8.cached(spec["wall"])
        shade = spec.get("shade", 0.5)
        fx1, fy1, rx1, ry1 = fx + fw, fy + fh, rx + rw, ry + rh
        h = spec["height"]
        if ry > fy:
            k8.wall_quad(frame, tex, shade, ((fx, fy), (fx1, fy)), ((rx, ry), (rx1, ry)), h)
        if ry1 < fy1:
            k8.wall_quad(frame, tex, 1.0, ((fx1, fy1), (fx, fy1)), ((rx1, ry1), (rx, ry1)), h)
        if rx > fx:
            k8.wall_quad(frame, tex, 1.0, ((fx, fy1), (fx, fy)), ((rx, ry1), (rx, ry)), h)
        if rx1 < fx1:
            k8.wall_quad(frame, tex, shade, ((fx1, fy), (fx1, fy1)), ((rx1, ry), (rx1, ry1)), h)
        k8.blit(frame, k8.cached(placed["piece"]), rx, ry)


k8.draw_towers = draw_towers_fixed      # Level 10's composites only (this process); the kit's file is unchanged

# --------------------------------------------------------------------------- the layout

TILE_SETS = {   # id -> (layer, height, drift, note)
    "morning-sky": ("deep", 480, None, "the dawn sky mirrored, gold in the east: seen only in the water"),
    "spaceport": ("ground", TILE_H, None, "1: Eko spaceport's apron (x 84-396), taxiways, hangars, the terminal"),
    "suburbs": ("ground", TILE_H, None, "2: the jammed motorway (x 176-304), refugee lights, houses, pitches"),
    "viaduct": ("ground", TILE_H, None, "3: the maglev viaduct (deck x 214-266) between the creep districts"),
    "coast": ("ground", TILE_H, None, "4: the village, the coast road (x 116-146), beach, water from x 168, sandbars"),
    "open-lagoon": ("ground", TILE_H, None, "5: the open lagoon (translucent), shoals, fish-trap stakes"),
    "haze": ("low-air", 960, DRIFT["haze"], "light: dawn haze and the city's smoke, drifting west"),
    "smoke-mist": ("low-air", 960, DRIFT["smoke-mist"], "medium: smoke and sea mist, drifting west"),
    "mist": ("high-air", 960, None, "clear and light: thin dawn mist (additive)"),
    "wisps": ("high-air", 960, None, "medium: smoke wisps (additive)"),
}
GUNSHIP_SIDE = 44


def ferry_place():
    """The ferry piece's (x, ground centre), from the hatch unit it carries."""
    _, _, _, x, pos, _, _ = hatch_unit()
    lx, lu = ferry_hatch_local()
    w, h = FERRY
    return x - (lx - w / 2), pos - (lu - h / 2)


def ferry_x():
    return ferry_place()[0]


PIECES = {      # id -> (layer, (w, h), extra spec, mid-size, note)
    "cross-highway": ("ground", (480, 92), {}, False, "an elevated cross highway (a causeway over the water); one over every section seam"),
    "service-vehicles": ("ground", (48, 40), {}, False, "a fuel bowser, a tug and a stair truck on the apron"),
    "cdf-gunship": ("low-air", (GUNSHIP_SIDE, GUNSHIP_SIDE), {"headings": 16}, False,
                    "the CDF gunship (Level 08's model at dawn), 16 headings, 0 nose up, clockwise; scenery"),
    "burnt-block-a": ("ground", (72, 82), {}, False, "a collapsed block in the creep districts"),
    "burnt-block-b": ("ground", (76, 82), {}, False, None),
    "fire": ("ground", (40, 40), {"frames": 4, "fps": 8}, False, "flames on the burnt blocks (section 3)"),
    "smoke-column-a": ("low-air", (150, 200), {}, False, "smoke plumes over the burning blocks (static)"),
    "smoke-column-b": ("low-air", (130, 180), {}, False, None),
    "maglev-train": ("ground", (16, 180), {}, False, "a stalled maglev train on the viaduct's west guideway"),
    "ferry": ("ground", FERRY, {}, True, "the capsized ferry on its sandbar; the hatch (a ground target) on its hull"),
    "fishing-boats": ("ground", (56, 48), {}, False, "pirogues drawn up on the beach"),
    "buoy": ("ground", (16, 16), {"frames": 4, "fps": 2}, False, "a channel buoy, its lamp blinking"),
}


def _pads_piece():
    _, _, w, h, _ = pads_layout()
    return ("ground", (w, h), {}, True, "the five launch pads round the shuttles' t=1 points (the liftoff's `pads`)")


PIECES["launch-pads"] = _pads_piece()
k8.PIECES = PIECES
NOTE = {1: "1. Liftoff: Eko spaceport, the five pads", 2: "2. Refugee Roads: the jammed motorway, CDF gunships south",
        3: "3. The Corridor: the maglev viaduct between the creep districts", 4: "4. Coast Road: the capsized ferry",
        5: "5. Orbital Corridor: the open lagoon, buoys"}


def fixed_placements():
    """(piece, t, x, extra, comment): everything but the towers, burnt blocks, fires, plumes and gunships."""
    out = []
    for s in range(1, len(SECTIONS)):
        out.append(("cross-highway", round(t_for_centre("ground", seam_pos(s)), 3), 240, None,
                    "over every section seam, where the ground tiles change (the last two: causeways over the water)"
                    if s == 1 else None))
    cx, cp, _, _, _ = pads_layout()
    out.append(("launch-pads", round(t_for_centre("ground", cp), 3), round(cx), None, NOTE[1]))
    for t, x in ((6.5, 128), (12.2, 352), (16.4, 110)):
        out.append(("service-vehicles", t, x, None, None))
    out.append(("maglev-train", 96.0, DECK[0] + 12, None, NOTE[3] + "; the stalled train"))
    fx, fp = ferry_place()
    out.append(("ferry", round(t_for_centre("ground", fp), 3), round(fx), None, NOTE[4] + " (the hatch's spot)"))
    for t, x in ((136.0, 156), (158.5, 154), (168.0, 157)):
        out.append(("fishing-boats", t, x, None, None))
    for t, x in ((178.0, 300), (181.6, 352), (186.0, 248), (190.0, 300), (194.4, 356)):
        out.append(("buoy", t, x, None, NOTE[5] if t == 178.0 else None))
    return out


def piece_rects(placed_list):
    out = []
    for piece, t, x, extra, _ in placed_list:
        w, h = PIECES[piece][1]
        c = scroll_at(t) + MID
        out.append((piece, x - w / 2, c - h / 2, x + w / 2, c + h / 2))
    return out


def zones():
    """What the towers keep clear of: the ferry and its hatch (with margins)."""
    out = []
    for _, x0, p0, x1, p1 in piece_rects([p for p in fixed_placements() if p[0] == "ferry"]):
        out.append((x0 - 6, p0 - 6, x1 + 6, p1 + 6))
    for _, _, _, x, pos, w, h in target_units():
        out.append((x - w / 2 - 14, pos - h / 2 - 14, x + w / 2 + 14, pos + h / 2 + 14))
    return out


_TOWERS = None


def tower_placements():
    global _TOWERS
    if _TOWERS is None:
        _TOWERS = place_towers()
    return _TOWERS


def place_towers():
    """Towers on the city lots: the last stretch of the suburbs low and sparse, rising toward the
    corridor; the creep districts of section 3 dense and tall, most creep-covered, some burnt."""
    rng = np.random.default_rng(3010)
    zl = zones()
    flats = piece_rects([p for p in fixed_placements() if p[0] in ("cross-highway",)])
    out = []
    plans = ((1, seam_pos(2) - 2600, seam_pos(2)), (2, seam_pos(2), seam_pos(3)))
    for s, p0, p1 in plans:
        for x0, x1, q0, q1 in k8.lots(SECTION_GROUND[s], p0 + 30, p1 - 30):
            cx, cp = (x0 + x1) / 2, (q0 + q1) / 2
            if any(not (r[3] + 4 < x0 or r[1] - 4 > x1 or r[4] + 4 < q0 or r[2] - 4 > q1) for r in flats):
                continue
            progress = (cp - p0) / max(1, p1 - p0)
            r = rng.random()
            if s == 1:
                share, top, style = 0.15 + 0.35 * progress, 0.6 + 0.25 * progress, ("a" if r < 0.6 else "b")
            else:
                share, top, style = 0.6, 1.35, ("creep" if r < 0.5 else "burnt" if r < 0.68 else "a" if r < 0.85 else "b")
            if style == "creep" and top < 0.85:
                style = "a"
            if rng.random() > share:
                continue
            fits = [f for f in FOOTPRINTS if f[0] <= x1 - x0 - 6 and f[1] <= q1 - q0 - 8]
            if not fits:
                continue
            foot = fits[-1] if rng.random() < 0.7 or len(fits) == 1 else fits[-2]
            heights = [hh for hh in STYLES[style][0] if hh <= top + 1e-9]
            if not heights:
                continue
            want = heights[min(len(heights) - 1, int(rng.uniform(0.3, 1.0) ** 1.2 * len(heights)))]
            t_mid = t_for_centre("ground", cp)
            scrolls = np.array([scroll_at(t) for t in np.arange(t_mid - 6, t_mid + 6, 1 / 15)])
            for hgt in sorted([hh for hh in heights if hh <= want], reverse=True):
                if k8.hull_clear(cx, cp, foot, hgt, zl, scrolls):
                    out.append((tower_id(foot, hgt, style), round(t_mid, 3), cx, None, None))
                    break
    return out


def burnt_placements():
    """Collapsed blocks on free lots of section 3, fires on some of them (from t≈74, after the
    gunships of section 2 have gone)."""
    rng = np.random.default_rng(4010)
    towers = tower_placements()
    tower_spots = {(round(x), round(scroll_at(t) + MID)) for _, t, x, _, _ in towers}
    rects = piece_rects(fixed_placements())
    out, fires = [], []
    p0, p1 = seam_pos(2), seam_pos(3)
    for x0, x1, q0, q1 in k8.lots("viaduct", p0 + 40, p1 - 60):
        cx, cp = (x0 + x1) / 2, (q0 + q1) / 2
        if (round(cx), round(cp)) in tower_spots:
            continue
        if any(not (r[3] + 2 < x0 or r[1] - 2 > x1 or r[4] + 2 < q0 or r[2] - 2 > q1) for r in rects):
            continue
        if any(abs(scroll_at(t) + MID - cp) < 60 and abs(x - cx) < 40 for _, t, x, _, _ in towers):
            continue
        if rng.random() > 0.3:
            continue
        w = x1 - x0
        if w < 70:
            continue
        pid = "burnt-block-a" if rng.random() < 0.5 else "burnt-block-b"
        t = round(t_for_centre("ground", cp), 3)
        out.append((pid, t, cx, None, None))
        if rng.random() < 0.45 and t > 74:
            fires.append(("fire", round(t + rng.uniform(-0.12, 0.12), 3), round(cx + rng.uniform(-12, 12)), None, None))
    return out, fires


def smoke_placements(fires):
    """Smoke plumes on low-air over some of the fires (static), pushed to the outer side, away from
    the play field's middle."""
    rng = np.random.default_rng(5010)
    out = []
    last = -99.0
    for _, t, x, _, _ in fires:
        if t - last < 4.0:
            continue
        pid = ("smoke-column-a", "smoke-column-b")[len(out) % 2]
        cx = x - 30 if x < 240 else x + 30
        out.append((pid, round(t + 0.6, 2), round(float(cx + rng.uniform(-10, 10))), None,
                    "smoke plumes on low-air over the fires (static)" if not out else None))
        last = t
    return out


def gunship_passes():
    """CDF gunships heading south on low-air over the suburbs, the other way to the corridor: pairs
    entering at the top edge, flying down at 90 px/s on their own (the path, on the real clock) over
    the low-air scroll; the last one gone before t≈66 (the medium smoke's drift and the fires
    follow)."""
    out = []
    side = GUNSHIP_SIDE
    for k, (t0, x, dx) in enumerate(((24.0, 120, 18), (24.6, 160, 18), (36.0, 380, -14), (47.5, 300, 10),
                                      (48.1, 340, 10), (58.0, 96, 22))):
        centre = scroll_at(t0) * FACTORS["low-air"] + SCREEN + side / 2 + 1
        t_place = t_at((centre - MID) / FACTORS["low-air"])
        span = 3.0
        entry = {"path": [[round(t0, 2), 0, 0], [round(t0 + span, 2), dx * span, round(-90 * span, 1)]]}
        out.append(("cdf-gunship", round(t_place, 3), x, entry,
                    "2. CDF gunships heading south, the other way (scenery, shots pass through)" if k == 0 else None))
    return out


def placements():
    fixed = fixed_placements()
    burnt, fires = burnt_placements()
    return {"fixed": fixed, "towers": tower_placements(), "burnt": burnt, "fires": fires,
            "smoke": smoke_placements(fires), "gunships": gunship_passes()}


def tower_pieces(towers):
    out = {}
    for pid, *_ in towers:
        _, fp, hpart, style = pid.split("-", 3)
        fw, fh = map(int, fp.split("x"))
        h = int(hpart[1:]) / 100
        spec = {"height": h, "wall": wall_for(style, h)}
        if STYLES[style][1] is not None:
            spec["shade"] = STYLES[style][1]
        out[pid] = ("ground", (fw, fh), {"tower": spec}, False, None)
    return out


def all_pieces(pl=None):
    pl = pl or placements()
    pieces = dict(PIECES)
    pieces.update(sorted(tower_pieces(pl["towers"]).items()))
    return pieces


def ordered_placements(pl):
    """Draw order on a layer: the flat set dressing first, then the burnt blocks and fires, the
    towers (drawn by height by the game anyway); low-air: the plumes, then the gunships."""
    return pl["fixed"] + pl["burnt"] + pl["fires"] + pl["towers"] + pl["smoke"] + pl["gunships"]


def block(pl=None):
    pl = pl or placements()
    tile_sets = {}
    for name, (layer, height, drift, _) in TILE_SETS.items():
        spec = {"layer": layer, "height": height}
        if drift:
            spec["drift"] = drift
        tile_sets[name] = spec
    pieces = {}
    for name, (layer, (w, h), extra, mid, _) in all_pieces(pl).items():
        spec = {"layer": layer, "size": [w, h], **extra}
        if mid:
            spec["mid_size"] = True
        pieces[name] = spec
    placed = []
    for piece, t, x, extra, _ in ordered_placements(pl):
        entry = {"piece": piece, "t": t, "x": x}
        if extra:
            entry.update(extra)
        placed.append(entry)
    return {"scroll_factors": dict(FACTORS), "ramp": RAMP, "haze_colour": HAZE_COLOUR,
            "atmosphere": ATMOSPHERE, "tile_sets": tile_sets, "pieces": pieces, "placed": placed}


def write_proposal():
    pl = placements()
    b = block(pl)
    pieces = all_pieces(pl)
    flow = k8.flow
    cx, cp, pw, ph, _ = pads_layout()
    lines = [
        "# Level 10 backdrop PROPOSAL (tools/art/backdrop_l10.py --proposal; M5 part D batch, round 32).",
        "# For the level-data step to merge into data.yaml: `backdrop` is the level's block, and the",
        "# sections take the `tiles` below. Level 10 has no holds, so script time is real time",
        "# (scroll distance / 190 px/s); the gunships' `path` runs on the real clock from its first waypoint.",
        "#",
        "# sections[i].tiles (by section, in order):",
    ]
    for s, tiles in zip(SECTIONS, SECTION_TILES):
        lines.append(f"#   {s['name']}: [{', '.join(tiles)}]")
    lines += [
        "# Section 4's atmosphere: light, with a medium `peak` from t=160 (the sea mist) to the section's end.",
        "#",
        "# What this block was built round (re-run --proposal and the images after moving one):",
        f"#   the shuttles' liftoff pads (escort.liftoff.pads, screen [x, y below the top] at t={DATA['lift'][0]:g}):",
        f"#     {DATA['pads']}; the launch-pads piece ({pw} x {ph}) is centred at x {cx:g}, ground {cp:.0f}.",
    ]
    for g in DATA["ground_targets"]:
        lines.append(f"#   {g['target']} (sprite {g.get('sprite')}, size {g.get('size')}, section {g.get('section')}): "
                     f"at {g.get('at')}")
    fx, fp = ferry_place()
    lines += [
        f"#   the ferry piece is placed so the hatch sits on its hull's patched plate (ferry x {fx:.0f}).",
        "# The hatch's sprites: assets/sprites/ferry-hatch_0..2 (36 x 32: shut, hit, open) and",
        "#   ferry-hatch-break_0..7 (56 x 56), `python3 tools/art/backdrop_l10.py --props` once the data's",
        "#   ground target names `sprite: ferry-hatch` (the pipeline refuses unclaimed sprites before that).",
        "# Motion budget (BackdropCheck, at most 2 moving ids on screen): the gunships (section 2, gone by",
        "#   t≈64), the drifting banks (light and medium), the fires (section 3, from t≈74), the buoys",
        "#   (section 5); never more than two at once.",
        "",
        "backdrop:",
        "  # Presentation only: the Nova Lagos evacuation corridor at first light (tools/art/backdrop_l10.py,",
        "  # Level 08's production kit with Level 09's creep; D10 = a, D11 = a). Towers are scenery in true",
        "  # perspective (section 3's creep districts and the suburbs' last blocks), clear of the ferry hatch.",
        "  scroll_factors: " + flow(b["scroll_factors"]),
        f"  ramp: {RAMP}",
        f"  haze_colour: {HAZE_COLOUR}  # rose-grey: the dawn haze over the water",
        "  atmosphere:",
    ]
    notes = {"clear": "sections 2 and 5: thin dawn mist only", "light": "sections 1 and 4: dawn haze, drifting west",
             "medium": "section 3 and section 4's peak: smoke and sea mist, drifting west"}
    for key, look in ATMOSPHERE.items():
        lines.append(f"    {key}: {flow(look)}   # {notes[key]}")
    lines.append("  tile_sets:")
    for name, spec in b["tile_sets"].items():
        lines.append(f"    {name}: {flow(spec)}   # {TILE_SETS[name][3]}")
    lines.append("  pieces:")
    for name, spec in b["pieces"].items():
        note = pieces[name][4]
        lines.append(f"    {name}: {flow(spec)}" + (f"   # {note}" if note else ""))
    lines.append("  placed:")
    for entry, (_, _, _, _, comment) in zip(b["placed"], ordered_placements(pl)):
        if comment:
            lines.append(f"    # {comment}")
        lines.append(f"    - {flow(entry)}")
    PROPOSAL.parent.mkdir(parents=True, exist_ok=True)
    PROPOSAL.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"proposal: {PROPOSAL.relative_to(ROOT)} ({len(b['placed'])} placements, "
          f"{sum(1 for p in b['pieces'].values() if 'tower' in p)} tower pieces)")


def level_backdrop():
    """The level's backdrop block once its data.yaml has its own, otherwise the proposal's."""
    own = DATA.get("backdrop") or {}
    if own and "images" not in own:
        return own, "data.yaml"
    if not PROPOSAL.exists():
        write_proposal()
    return yaml.safe_load(PROPOSAL.read_text(encoding="utf-8"))["backdrop"], PROPOSAL.name


k8.level_backdrop = level_backdrop

# --------------------------------------------------------------------------- generators

GENERATORS = {
    "morning-sky": morning_sky, "spaceport": lambda w, h: spaceport_tile(), "suburbs": lambda w, h: suburbs_tile(),
    "viaduct": lambda w, h: viaduct_tile(), "coast": lambda w, h: coast_tile(),
    "open-lagoon": lambda w, h: open_lagoon_tile(),
    "haze": haze, "smoke-mist": smoke_mist, "mist": mist, "wisps": wisps,
    "cross-highway": lambda w, h: graded(k8.cross_highway(w, h), 0.0, 32, 0.4),
    "launch-pads": launch_pads, "service-vehicles": service_vehicles, "cdf-gunship": gunship_frames,
    "burnt-block-a": lambda w, h: graded(k8.burnt_block(w, h, 881), None, 24, STAGE["viaduct"]),
    "burnt-block-b": lambda w, h: graded(k8.burnt_block(w, h, 883), None, 24, STAGE["viaduct"]),
    "fire": lambda w, h, n: k8.fire(w, h, n),
    "smoke-column-a": lambda w, h: graded(k8.smoke_column(w, h, 1231), None, 16, STAGE["viaduct"]),
    "smoke-column-b": lambda w, h: graded(k8.smoke_column(w, h, 1233), None, 16, STAGE["viaduct"]),
    "maglev-train": maglev_train, "ferry": ferry, "fishing-boats": fishing_boats, "buoy": buoy_frames,
}


def generator(name, spec):
    if name in GENERATORS:
        return GENERATORS[name]
    if "tower" in spec:
        _, fp, hpart, style = name.split("-", 3)
        foot = tuple(map(int, fp.split("x")))
        return lambda w, h: tower_roof(foot, int(hpart[1:]) / 100, style)
    raise KeyError(name)


def jobs(backdrop, wanted):
    out = [(name, (W, spec["height"]), None, None) for name, spec in backdrop["tile_sets"].items()]
    for name, spec in backdrop["pieces"].items():
        size = tuple(spec["size"])
        if "tower" in spec:
            size = k8.roof_size(size, spec["tower"]["height"])
        out.append((name, size, spec.get("frames") or spec.get("headings"), spec))
    walls = sorted({spec["tower"]["wall"] for spec in backdrop["pieces"].values() if "tower" in spec})
    out += [(wall, None, None, {"wall": True}) for wall in walls]
    return [job for job in out if not wanted or job[0] in wanted]


def render(job):
    name, size, count, spec = job
    if spec and spec.get("wall"):
        return [wall_texture(name)]
    fn = generator(name, spec or {})
    w, h = size
    images = fn(w, h, count) if count else [fn(w, h)]
    for img in images:
        if img.size != (w, h):
            raise ValueError(f"{name}: rendered {img.size}, the data file says {(w, h)}")
    return images


def write(job, images):
    name, _, count, _ = job
    if count:
        for old in OUT.glob(f"{name}_*.png"):
            old.unlink()
        for i, img in enumerate(images):
            artkit.save_png(img, OUT / f"{name}_{i}.png", SOURCE)
    else:
        artkit.save_png(images[0], OUT / f"{name}.png", SOURCE)


# --------------------------------------------------------------------------- checks (the agent's own)

def check_gameplay(backdrop, placed_all):
    """No tower's walls or roof come over the ferry hatch at any step."""
    problems = []
    towers = [p for p in placed_all if "tower" in backdrop["pieces"][p["piece"]]]
    units = [(u[0], u[3], u[4], u[5], u[6]) for u in target_units()]
    for step in range(0, int(OUTRO_END * 30)):
        t = step / 30
        scroll = scroll_at(t)
        spots = [(name, x, pos - scroll, max(w, 30), max(h, 30)) for name, x, pos, w, h in units]
        spots = [s for s in spots if -30 < s[2] < SCREEN + 30]
        if not spots:
            continue
        for placed in towers:
            if not k8.on_screen(backdrop, placed, t):
                continue
            (fx, fy, fw, fh), (rx, ry, rw, rh), _ = k8.tower_geometry(backdrop, placed, t)
            x0, x1 = min(fx, rx), max(fx + fw, rx + rw)
            y0, y1 = min(fy, ry), max(fy + fh, ry + rh)
            for name, x, y, w, h in spots:
                if x + w / 2 > x0 and x - w / 2 < x1 and y + h / 2 > y0 and y - h / 2 < y1:
                    problems.append(f"t={t:.1f}: {placed['piece']} at t {placed['t']:.2f} over {name}")
    return sorted(set(problems))[:16]


def run_checks(backdrop):
    placed_all = k8.expanded(backdrop)
    problems = k8.check_tiles(backdrop) + k8.check_edges(backdrop, placed_all) + k8.check_screens(backdrop, placed_all)
    problems += check_gameplay(backdrop, placed_all)
    for p in problems:
        print("PROBLEM:", p)
    if not problems:
        print("checks: ok")
    return problems


def atlas_area(backdrop):
    area = sum(W * s["height"] for s in backdrop["tile_sets"].values())
    walls = set()
    for name, s in backdrop["pieces"].items():
        if "tower" in s:
            rw, rh = k8.roof_size(s["size"], s["tower"]["height"])
            area += rw * rh
            walls.add(s["tower"]["wall"])
        else:
            area += s["size"][0] * s["size"][1] * (s.get("frames") or s.get("headings") or 1)
    for wall in walls:
        w, storeys, px, _ = WALLS[wall]
        area += w * storeys * px
    print(f"atlas area: {area:,} px = {area / 2048 ** 2:.2f} pages of 2048x2048")
    return area


# --------------------------------------------------------------------------- review: the shuttles and the hatch

def smoothstep(s):
    s = min(1.0, max(0.0, s))
    return s * s * (3 - 2 * s)


def shuttle_places(t):
    """Where the review draws the five shuttles at t: (unit 1-5, x, y below the top, sprite name):
    standing on their pads until the liftoff, easing to their stations over it (the -lift frames
    while low), swaying round them, climbing off the top edge at the end; Lifeline Three gone from
    the scripted loss on (its glide is the game's)."""
    lift_t, lift_s = DATA["lift"]
    out = []
    for i, ((sx, sy), period, phase) in enumerate(DATA["stations"]):
        if i + 1 == LOSS[0] and t >= LOSS[1]:
            continue
        th = 2 * math.pi * (t / period + phase)
        stx, sty = sx + SWAY[0] * math.sin(th), sy + SWAY[1] * math.sin(2 * th)
        px, py = DATA["pads"][i]
        if t < lift_t:
            out.append((i + 1, px, py - SPEED * (lift_t - t), "evacuation-shuttle-lift_0"))
        elif t < lift_t + lift_s:
            s = (t - lift_t) / lift_s
            e = smoothstep(s)
            frame = min(3, int(s * 5))
            name = f"evacuation-shuttle-lift_{frame}" if s < 0.8 else "evacuation-shuttle_2"
            out.append((i + 1, px + (stx - px) * e, py + (sty - py) * e, name))
        elif t >= CLIMB[0]:
            s = (t - CLIMB[0]) / CLIMB[1]
            out.append((i + 1, stx, sty - (sty + 40) * smoothstep(s), "evacuation-shuttle_2"))
        else:
            out.append((i + 1, stx, sty, "evacuation-shuttle_2"))
    return out


_SPR = {}


def sprite_arr(name):
    if name not in _SPR:
        path = SPRITES / f"{name}.png"
        _SPR[name] = np.array(Image.open(path).convert("RGBA")).astype(np.float64) if path.exists() else None
    return _SPR[name]


def with_units(frame, t, hatch_frames):
    """The ferry hatch (its shut frame) at its place and the five shuttles (their production
    sprites) over the composite, so the review shows how they read on the backdrop."""
    out = np.array(frame.convert("RGBA")).astype(np.float64)
    scroll = scroll_at(t)
    img = np.array(hatch_frames[0]).astype(np.float64)
    for _, _, _, x, pos, _, _ in target_units():
        if -60 < pos - scroll < SCREEN + 60:
            k8.blit(out, img, int(round(x - img.shape[1] / 2)), int(round(pos - scroll - img.shape[0] / 2)))
    for _, x, y, name in shuttle_places(t):
        spr = sprite_arr(name)
        if spr is None:
            continue
        k8.blit(out, spr, int(round(x - spr.shape[1] / 2)), int(round(SCREEN - y - spr.shape[0] / 2)))
    return Image.fromarray(np.clip(out[..., :3], 0, 255).astype(np.uint8), "RGB")


TIMES = [(0.5, "1 THE SHUTTLES ON THEIR PADS"), (4, "1 THE LIFTOFF"), (12, "1 THE SPACEPORT'S APRON"),
         (21.5, "1-2 CROSS HIGHWAY, SUBURBS"), (25.4, "2 CDF GUNSHIPS HEADING SOUTH"), (34, "2 THE JAMMED MOTORWAY"),
         (52, "2 PITCHES, THE FIRST TOWERS"), (71.5, "2-3 ONTO THE VIADUCT"), (84, "3 THE CREEP DISTRICTS"),
         (97, "3 THE STALLED MAGLEV"), (117, "3 UNDER THE CLOUD DECK (T=118)"), (131.5, "3-4 ONTO THE COAST ROAD"),
         (138, "4 PIERS, SANDBARS, PIROGUES"), (150.6, "4 THE CAPSIZED FERRY, ITS HATCH"), (165, "4 THE SEA MIST"),
         (176.5, "4-5 THE CAUSEWAY"), (184, "5 THE OPEN LAGOON, BUOYS"), (197, "5 THE CLIMB-OUT")]
GIF = (0.0, 8.0, 0.1)            # s: the liftoff, at 10 fps


def review(backdrop):
    hatch_frames, hatch_breaks = hatch_sprites()
    items = []
    walls = sorted({s["tower"]["wall"] for s in backdrop["pieces"].values() if "tower" in s})
    shown = set()
    for name, spec in list(backdrop["tile_sets"].items()) + list(backdrop["pieces"].items()):
        if "tower" in spec and name.startswith("tower-"):
            key = name.rsplit("-", 1)[1] + name.split("-")[2]
            if name.split("-")[1] != "68x64" or key in shown:
                continue
            shown.add(key)
        count = spec.get("frames") or spec.get("headings") if "size" in spec else None
        imgs = [k8.load(name, i) for i in range(count)] if count else [k8.load(name)]
        label = f"{name} {imgs[0].width}x{imgs[0].height} {artkit.colour_count(imgs)} col"
        picks = imgs[::4] if spec.get("headings") else imgs if name == "buoy" else imgs[:1]
        for k, im in enumerate(picks):
            scale = min(1.0, 200 / max(im.size)) if spec.get("layer") != "ground" or "tower" in spec or max(im.size) > 200 else 1.0
            if spec.get("headings") or name in ("fire", "buoy", "service-vehicles"):
                scale = 2.0
            im = im.resize((max(1, int(im.width * scale)), max(1, int(im.height * scale))), Image.NEAREST)
            idx = k * 4 if spec.get("headings") else k
            items.append((label + (f" [{idx}]" if count else ""), im, spec.get("layer") == "high-air"))
    for wall in walls:
        im = k8.load(wall)
        items.append((f"{wall} {im.width}x{im.height}", im.resize((im.width * 2, im.height * 2), Image.NEAREST), False))
    names = ["ferry-hatch_0 shut", "ferry-hatch_1 hit", "ferry-hatch_2 open"] + \
        [f"ferry-hatch-break_{k}" for k in range(0, HATCH_BREAK, 2)]
    for label, im in zip(names, hatch_frames + hatch_breaks[::2]):
        items.append((label + " 3x", im.resize((im.width * 3, im.height * 3), Image.NEAREST), False))
    width, x, y, row_h = 1640, 16, 44, 0
    rows = []
    for name, im, add_ in items:
        cell = max(im.width, 6 * len(name))
        if x + cell > width - 16:
            x, y, row_h = 16, y + row_h + 30, 0
        rows.append((name, im, add_, x, y))
        x += cell + 14
        row_h = max(row_h, im.height)
    comp_y = y + row_h + 40
    cw = 240 + 12
    per_row = (width - 16) // cw
    n_rows = (len(TIMES) + per_row - 1) // per_row
    sheet = raster.sheet(width, comp_y + n_rows * (270 + 30) + 20, "LEVEL 10 BACKDROP - FINAL",
                         "PRODUCTION ART, M5 PART D BATCH - R32; FIRST LIGHT; COMPOSITES AS THE GAME DRAWS THEM (TOWERS "
                         "PROJECTED, THE SHUTTLES AND THE FERRY HATCH ON TOP), 1/2 SCALE")
    for name, im, add_, px, py in rows:
        plate = Image.new("RGBA", im.size, artkit.PLATE)
        if add_:
            plate = artkit.add_light(plate, im)
        else:
            plate.alpha_composite(im)
        sheet.alpha_composite(plate, (px, py + 14))
        raster.draw_text(sheet, px, py, name.upper()[:44], raster.LABEL)
    placed_all = k8.expanded(backdrop)
    for k, (t, label) in enumerate(TIMES):
        px, py = 16 + (k % per_row) * cw, comp_y + (k // per_row) * 300
        shot = with_units(k8.composite(backdrop, t, placed_all), t, hatch_frames)
        sheet.alpha_composite(shot.convert("RGBA").resize((240, 270), Image.BOX), (px, py + 14))
        raster.draw_text(sheet, px, py, f"T={t:g} {label}"[:40], raster.LABEL)
    REVIEW.parent.mkdir(parents=True, exist_ok=True)
    sheet.convert("RGB").save(REVIEW, optimize=True)
    t0, t1, dt = GIF
    frames = [with_units(k8.composite(backdrop, float(t), placed_all), float(t), hatch_frames) for t in np.arange(t0, t1, dt)]
    gif = REVIEW.with_suffix(".gif")
    artkit.write_gif(frames, gif, fps=round(1 / dt, 1))
    print(f"review: {REVIEW.relative_to(ROOT)}, {gif.relative_to(ROOT)} {gif.stat().st_size / 1e6:.1f} MB")


def strip_frames(backdrop, times, path):
    placed_all = k8.expanded(backdrop)
    hatch_frames, _ = hatch_sprites()
    shots = [with_units(k8.composite(backdrop, t, placed_all), t, hatch_frames) for t in times]
    out = Image.new("RGB", (len(shots) * (W + 8), SCREEN), (0, 0, 0))
    for i, s in enumerate(shots):
        out.paste(s, (i * (W + 8), 0))
    out.save(path)
    print(f"strip: {path}")


def main(argv):
    global HATCH_DIR
    if "--hatch-dir" in argv:
        k = argv.index("--hatch-dir")
        HATCH_DIR = argv[k + 1]
        argv = argv[:k] + argv[k + 2:]
    if "--proposal" in argv:
        write_proposal()
        return
    if "--props" in argv:
        k = argv.index("--props")
        build_props(argv[k + 1] if len(argv) > k + 1 and not argv[k + 1].startswith("--") else None)
        return
    backdrop, source = level_backdrop()
    if source == "data.yaml":
        k8.SECTION_OF = [s["tiles"] for s in SECTIONS]
    print(f"backdrop from {source}")
    if "--strip" in argv:
        k = argv.index("--strip")
        strip_frames(backdrop, [float(a) for a in argv[k + 1].split(",")], argv[k + 2])
        return
    wanted = {a for a in argv if not a.startswith("--") and not a.endswith(".png") and "," not in a}
    if "--check" in argv:
        atlas_area(backdrop)
        sys.exit(1 if run_checks(backdrop) else 0)
    if "--review" not in argv:
        OUT.mkdir(parents=True, exist_ok=True)
        todo = jobs(backdrop, wanted)
        todo.sort(key=lambda j: -(j[1][0] * j[1][1] * (j[2] or 1) if j[1] else 0))
        with ProcessPoolExecutor() as pool:
            for job, images in zip(todo, pool.map(render, todo)):
                write(job, images)
        print(f"{len(todo)} images written")
        if not wanted:                                    # images no id of the block names any more
            names = {j[0] for j in jobs(backdrop, set())}
            for f in OUT.glob("*.png"):
                stem = f.stem.rsplit("_", 1)[0] if f.stem.rsplit("_", 1)[-1].isdigit() else f.stem
                if stem not in names:
                    f.unlink()
                    print(f"removed {f.name}")
        k8._cache.clear()
        if not wanted:
            run_checks(backdrop)
    atlas_area(backdrop)
    if not wanted:
        review(backdrop)


if __name__ == "__main__":
    main(sys.argv[1:])
