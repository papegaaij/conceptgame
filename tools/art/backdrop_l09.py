#!/usr/bin/env python3
"""Production art: the Level 09 backdrop, the Nova Lagos arcology district in the last hour of the
night (design/campaign/act-2-homefront/level-09-arcology-fall; M5 part C batch, concept round 31).

Straight to production (user decision D9 = a of M5 part C): Level 08's production kit
(tools/art/backdrop_l08.py, imported as a library: its height-field lighting, Canvas, city tiles,
low roofs, parks, cross highways, burnt blocks, fires, smoke plumes, tower roofs and wall textures,
the banks, the composite as the game draws it and the checks) re-dressed for the arcology district.
The kit's module state (the level's data, sections, plans and output folder) is pointed at Level 09
when this module loads, so its draw and check functions see Level 09's timeline.

**Time of day** (D9 = a): the last hour of the night, a grey glow in the east, so "first light"
stays Level 10's. Every image gets the pre-dawn grade (predawn(): the navy a little less saturated,
the darks lifted toward a cool grey, more toward the east, the right edge of the play field); the
lagoon's water shows the deep layer, the sky's reflection, its east half grey with the coming dawn;
the low-air banks and the high-air mist are lit grey on their east side. Sodium lamps still burn,
fewer windows are lit (the district is evacuated).

**Creep**: violet chitin veins on the towers' upper floors (wall-creep-*, creep roofs) and on the
Ndidi Arcology, as Level 08's arcology; on the ground the Hive Nodes' teal-black creep (their own
192 px creep sprite lies under each node; the plaza, bridgehead and lobby pieces spread it further
across the paving with glowing teal veins).

**Towers** (as Level 08, D1 = a of M5 part B): scenery in true perspective on the city lots, each
as tall as its spot allows without leaning over a ground target, its structure or a walker's path
while on screen (the walkers of the level's data: Creeper convoys and Ravager packs, each unit's own
path at its own speed). Nothing on `far`. The Ndidi Arcology is a ground tower (h 1.5, footprint
200 x 180) on the left of the lobby plaza, hollowed by creep; it leans and drops straight down in
the collapse (round 31's look c, tools/concept/collapse_r31.py, drawn by the game) and leaves the
`arcology-heap` (not placed), whose image is tools/art/collapse_l09.py's.

**Gameplay structures** (drawn flat, without lean) are sized and placed from the level's ground
targets (the data's `ground_targets`, every difficulty; until Level 09's data.yaml exists, the
layout proposed in DEFAULT_TARGETS below, for the level-data step to adopt or move): the Unity
Plaza (the plaza turret pair and nodes A1/A2), a low roof per boulevard mortar, the boulevard's
roof nest, the cocoon's low roof, the quay pads under the far bank's mortars, the bridgehead (its
turret pair and nodes B1/B2) and the Ndidi lobby plaza (nodes C1/C2, hard's turret pair). Every
turret and mortar stands on a pale painted spot, every node on a creep patch.

**Moving scenery** (motion budget: at most two moving piece ids on screen): the Kilo trucks on the
Okonjo Bridge in section 4 (one piece, `kilo-truck`, 16 headings, a CDF cargo truck from the gunship
kit: tools/concept/props_r31.py's model, rendered as l08_traffic renders its craft) driving down the
bridge's southbound lane; the fires on burnt blocks (sections 1 and 3, never with the trucks). The
banks do not drift. Paths and drift run on the real clock (D2 = a, so the trucks do not crawl in a
hold); placements are script time (distance / 150).

Outputs (assets/backdrop/level-09/, one PNG per tile set and piece of the backdrop block, named by
its id; headings and frames as <id>_<n>.png; wall textures by id):
  pre-dawn                   deep: the sky's reflection, grey in the east (seen in the lagoon only)
  approach                   ground, section 1: the ruined elevated highway, burnt-out traffic,
                             blocks either side
  plaza-district             ground, section 2: avenues, office roofs, parks
  boulevard                  ground, section 3: the boulevard with tram tracks, abandoned cars,
                             residential roofs
  lagoon                     ground, section 4: the lagoon (translucent), the west bank's quay and
                             warehouses, the Okonjo Bridge's deck running up the screen
  lobby-district             ground, section 5: the arcology podiums round a wide avenue, creep
  dust, smoke, dust-heavy    low-air banks: street dust (light), river smoke (medium), the
                             collapse's dust (heavy, the event-triggered peak)
  mist, ash                  high-air wisps (additive)
  cross-highway              ground: an elevated cross highway over every section seam
  barricade, highway-breach  ground, section 1: the CDF barricade, a fallen span of the highway
  unity-plaza, mortar-roof, roof-nest, cocoon-roof, quay-pads, bridgehead, lobby-plaza
                             ground: the targets' structures
  tram, car-wrecks-a/b       ground, section 3: the burnt-out tram, abandoned cars
  ferry-wreck                ground, section 4: a capsized ferry in the lagoon
  burnt-block-a/b/c, fire_0..3   ground: collapsed blocks, flames
  smoke-column-a/b           low-air: smoke plumes over the burning blocks (static)
  kilo-truck_0..15           ground: the Kilo convoy's truck at 16 headings (_0 nose up, clockwise)
  tower-<w>x<h>-h<NNN>-<style>, arcology, arcology-b   tower roofs (city a/b, creep, burnt,
                             the Ndidi Arcology, a smaller arcology)
  arcology-heap              ground: the rubble heap the collapse leaves (tools/art/collapse_l09.py's)
  wall-low/-mid/-tall, wall-creep-mid/-tall, wall-burnt, wall-arcology, wall-arcology-hollow
  assets/sprites/cocoon_0..2.png, cocoon-break_0..7.png   the rooftop cocoon (44 x 40): intact,
                             hit, opened; its break-apart (64 x 64), as Level 04's dugout (--props)
  design/campaign/.../level-09-arcology-fall/concept/backdrop-final-r31-a.png/.gif   review sheet
                             (tiles, pieces, the cocoon and the trucks, composites as the game draws
                             them) and the bridge with the convoy (GIF)
  design/campaign/.../level-09-arcology-fall/concept/backdrop-proposal.yaml   the backdrop block and
                             the sections' tiles for the level data (--proposal)

Run: python3 tools/art/backdrop_l09.py [--proposal | --props | --review | --check |
     --strip t,.. out.png [--units]] [id ...]   (all: about 6 min)
"""
import copy
import math
import sys
from concurrent.futures import ProcessPoolExecutor
from dataclasses import replace

import numpy as np
import yaml
from PIL import Image, ImageDraw

import artkit
import backdrop_l08 as k8
import l08_traffic
from artkit import DESIGN, ROOT, SPRITES, raster, sdf, sprite
from backdrop_l08 import (CHITIN, CONCRETE, EMBER, GREEN, GROUND, JOV, LAMP, LOT0, LOT1, MARS, MID, NAVY, ORBIT, PERIOD,
                          ROAD, SCORCH, SCREEN, SODIUM, TILE_H, VGLOW, WARM_WIN, Canvas, W, add_glow, drop_shadow, finish,
                          finish_tile, java_round, low_roof, paste_sprite, quantize_lit, specks, to_img, tower_scale, trees)

import ground_targets as gt  # noqa: E402  (concept script: scorch, light rim)
import props_r30 as p30  # noqa: E402  (concept script, imported unchanged: rotation, muting, halos)
import props_r31 as p31  # noqa: E402  (concept models of round 31: the Kilo truck, the cocoon)
from loot_targets import crumbled  # noqa: E402  (the break-apart's burn-down)
from parallax_r03 import noise  # noqa: E402  (concept script, unchanged)
from render.sdf import rotate_x, rotate_z, sd_box  # noqa: E402

SCRIPT = "backdrop_l09.py"
SOURCE = artkit.source_note(SCRIPT, "M5 part C batch")
LEVEL_DIR = DESIGN / "campaign" / "act-2-homefront" / "level-09-arcology-fall"
CONCEPT = LEVEL_DIR / "concept"
PROPOSAL = CONCEPT / "backdrop-proposal.yaml"
OUT = ROOT / "assets" / "backdrop" / "level-09"
REVIEW = CONCEPT / "backdrop-final-r31-a.png"
FACTORS = dict(k8.FACTORS)
HAZE_COLOUR = "2a2a3a"            # cool grey-violet: the haze before dawn
ATMOSPHERE = {
    "clear": {"wisps": "mist", "haze": 0.0},
    "light": {"banks": "dust", "wisps": "mist", "haze": 0.06},
    "medium": {"banks": "smoke", "wisps": "ash", "haze": 0.14},
    "heavy": {"banks": "dust-heavy", "wisps": "ash", "haze": 0.26},
}
RAMP = 4
CONVOY = 1.5                      # s between a convoy's units (Formations.CONVOY_INTERVAL_SECONDS)
PACK = 0.25                       # s between a pack's units

# --------------------------------------------------------------------------- the level (script time)
# Until Level 09's data.yaml exists, its sections from the README's Layout table and the ground
# targets proposed here (the README's estimates: t is when a unit enters at the top edge, x its
# centre). The level-data step adopts or moves them; a re-run then follows the data.

DEFAULT_SECTIONS = [
    {"name": "Arcology Approach", "end": 20, "atmosphere": "light"},
    {"name": "Unity Plaza", "end": 58, "atmosphere": "clear"},
    {"name": "Boulevard Run", "end": 96, "atmosphere": "light"},
    {"name": "Okonjo Bridge", "end": 138, "atmosphere": "medium"},
    {"name": "Arcology Fall", "end": 186, "atmosphere": "medium"},
]
DEFAULT_TARGETS = [
    {"target": "plaza turrets", "section": 2, "enemy": "spine-turret", "at": [[34.6, 168], [34.6, 312]]},
    {"target": "Node A1", "section": 2, "enemy": "hive-node", "at": [[37.4, 150]]},
    {"target": "Node A2", "section": 2, "enemy": "hive-node", "at": [[38.2, 330]]},
    {"target": "boulevard mortars", "section": 3, "enemy": "polyp-mortar", "at": [[67.6, 120], [68.8, 352]]},
    {"target": "roof nest", "section": 3, "enemy": "spine-turret", "at": [[79.6, 340], [80.0, 390], [80.4, 440]]},
    {"target": "creep cocoon", "section": 3, "layer": "ground", "size": [44, 40], "sprite": "cocoon", "hp": 30,
     "hardened": True, "at": [[86.0, 118]]},
    {"target": "quay mortars", "section": 4, "enemy": "polyp-mortar", "at": [[107.6, 48], [108.8, 84]]},
    {"target": "bridgehead turrets", "section": 4, "enemy": "spine-turret", "at": [[116.2, 52], [116.2, 186]]},
    {"target": "Node B1", "section": 4, "enemy": "hive-node", "at": [[119.4, 64]]},
    {"target": "Node B2", "section": 4, "enemy": "hive-node", "at": [[120.2, 168]]},
    {"target": "cluster C turrets", "section": 5, "enemy": "spine-turret", "hard": {"at": [[161.6, 286], [161.6, 448]]}},
    {"target": "Node C1", "section": 5, "enemy": "hive-node", "at": [[163.6, 318]]},
    {"target": "Node C2", "section": 5, "enemy": "hive-node", "at": [[164.4, 420]]},
]


def level_data():
    path = LEVEL_DIR / "data.yaml"
    data = yaml.safe_load(path.read_text(encoding="utf-8")) if path.exists() else {}
    out = {"scroll_speed": data.get("scroll_speed", 150), "sections": data.get("sections") or DEFAULT_SECTIONS,
           "ground_targets": data.get("ground_targets") or DEFAULT_TARGETS, "waves": data.get("waves") or [],
           "backdrop": data.get("backdrop"), "own": path.exists()}
    return out


DATA = level_data()
SECTIONS = DATA["sections"]
SPEED = float(DATA["scroll_speed"])
STARTS = [0.0] + [float(s["end"]) for s in SECTIONS[:-1]]
END = float(SECTIONS[-1]["end"])
OUTRO_END = END + 15                   # LevelData.OUTRO_SECONDS

# The plans: tile coordinates as Level 08's (x from the left, u up from the tile's bottom row).
BANK_X = 110                           # the lagoon's west bank: its quay edge
DECK = (220, 300)                      # the Okonjo Bridge's deck (four lanes, a median)
TRUCK_LANE = 248.5                     # the southbound inner lane's middle
PLANS = {
    "arcology": {"blocks": [(6, 96), (108, 196), (284, 372), (384, 474)],
                 "roads": [(96, 108, "street"), (196, 284, "boulevard"), (372, 384, "street"), (0, 6, "lane"),
                           (474, 480, "lane")]},
    "lagoon": {"blocks": [(6, 56)], "roads": []},
}
SECTION_GROUND = ["third-mainland", "avenues", "rooftops", "lagoon", "arcology"]
SECTION_TILES = [["pre-dawn", "approach"], ["pre-dawn", "plaza-district"], ["pre-dawn", "boulevard"],
                 ["pre-dawn", "lagoon"], ["pre-dawn", "lobby-district"]]

# --------------------------------------------------------------------------- point the kit at Level 09

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


def enemy_data(enemy):
    return k8.enemy_data(enemy)


def target_units():
    """Every ground target's unit over all difficulties: (target, section, kind, x, layer position of
    its centre, sprite w, h); kind is the enemy's slug or the target's sprite."""
    out = []
    for g in DATA["ground_targets"]:
        if "enemy" in g:
            e = enemy_data(g["enemy"])
            hb, size, kind = e["hitbox"], e.get("size", e["hitbox"]), g["enemy"]
        else:
            hb = size = g["size"]
            kind = g.get("sprite", "cargo-container")
        spots = {tuple(a) for v in [g] + [g.get(d, {}) for d in ("easy", "hard")] for a in v.get("at", [])}
        for t, x in sorted(spots):
            out.append((g["target"], int(g.get("section", 1)), kind, float(x), scroll_at(t) + SCREEN + hb[1] / 2,
                        size[0], size[1]))
    return out


def walkers():
    """Every walker unit over all difficulties: (entry t, ground path [(x, pos)], speed px/s): a
    convoy's units on its one path 1.5 s apart, a pack's each on its own path 0.25 s apart."""
    out = []
    for w in DATA["waves"]:
        paths = w.get("paths")
        if not paths:
            continue
        n = max([w.get("count", 1)] + [w.get(d, {}).get("count", 0) for d in ("easy", "hard")])
        speed_w = float(enemy_data(w["enemy"]).get("speed", 35))
        pack = w.get("formation") == "pack"
        for i in range(n):
            t0 = float(w["t"]) + (PACK if pack else CONVOY) * i
            path = paths[i % len(paths)] if pack else paths[0]
            base = scroll_at(t0) + SCREEN
            out.append((t0, [(float(x), base - float(y)) for x, y in path], speed_w))
    return out


_PLACES = None


def ground_places(t):
    """k8.ground_places for Level 09: (walker places, walked tracks, target boxes) at t."""
    global _PLACES
    if _PLACES is None:
        _PLACES = (walkers(), [(x, pos, w, h) for _, _, _, x, pos, w, h in target_units()])
    paths, boxes = _PLACES
    units, tracks = [], []
    for t0, g, speed_w in paths:
        if t >= t0:
            pts = k8.walked(g, (t - t0) * speed_w)
            units.append(pts[-1])
            tracks.append(pts)
    return units, tracks, boxes


k8.ground_places = ground_places


# --------------------------------------------------------------------------- the pre-dawn grade

DAWN = np.array([150, 158, 182], float)     # the grey glow in the east
CREEP_DARK = np.array([6, 32, 30], float)   # the Hive Node's teal-black creep (render/enemy_models "teal-black")
CREEP_MID = np.array([30, 92, 90], float)
CREEP_VEIN = VGLOW[1] * 0.75                # its glowing veins, teal
HULL_GREY = np.array([70, 76, 70], float)   # CDF olive-grey (the gunship kit's)


def predawn(arr, x0=0.0, lift=0.035, east=0.11, sat=0.86):
    """The last hour of the night on a native float RGBA image whose left column lies at play-field
    x ``x0`` (None: the play field's middle for every column, for pieces placed at many x): the
    colours a little less saturated, the darks lifted toward a cool grey, more toward the east."""
    out = arr.copy()
    rgb = out[..., :3]
    lum = rgb @ np.array([0.3, 0.59, 0.11])
    rgb = lum[..., None] * (1 - sat) + rgb * sat
    w = arr.shape[1]
    xs = np.full(w, 0.5) if x0 is None else np.clip((x0 + np.arange(w) + 0.5) / W, 0, 1)
    g = lift + east * xs ** 1.6
    dark = np.clip(1 - lum * 3.0, 0, 1)[..., None]
    out[..., :3] = np.clip(rgb + (DAWN / 255)[None, None, :] * g[None, :, None] * (0.45 + 0.55 * dark), 0, 1)
    return out


def creep_blots(arr, rng, count, rmin, rmax, wrap=True, keep=None, veins=True):
    """Teal-black creep blots on a native float RGBA image (flat goo over roofs and paving): a ragged
    rim from value noise, a darker core, a few glowing veins; ``keep`` an optional mask where none
    may go."""
    h, w = arr.shape[:2]
    nz = raster.fbm(w, h, 12, int(rng.integers(1 << 30)), octaves=3, period=wrap)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64)
    for _ in range(count):
        cx, cy, r = rng.uniform(0, w), rng.uniform(0, h), rng.uniform(rmin, rmax)
        dy = yy - cy
        if wrap:
            dy = (dy + h / 2) % h - h / 2
        d = np.hypot(xx - cx, dy) / r + (nz - 0.5) * 0.9
        body = d < 1.0
        if keep is not None:
            body &= ~keep
        core = d < 0.55
        tone = np.where(core, 0.0, 1.0)[..., None]
        col = (CREEP_DARK * (1 - tone) + CREEP_MID * tone * 0.75) / 255
        arr[body, :3] = arr[body, :3] * 0.25 + col[body] * 0.75
        if veins:
            ang = np.arctan2(dy, xx - cx)
            v = np.abs(np.sin(ang * 5 + nz * 6)) < 0.1
            sel = body & v & (d > 0.2)
            arr[sel, :3] = np.minimum(1, arr[sel, :3] * 0.4 + CREEP_VEIN / 255)
    return arr


def scorch_blots(arr, rng, count, rmin, rmax, wrap=True):
    h, w = arr.shape[:2]
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64)
    nz = raster.fbm(w, h, 8, int(rng.integers(1 << 30)), octaves=3, period=wrap)
    for _ in range(count):
        cx, cy, r = rng.uniform(0, w), rng.uniform(0, h), rng.uniform(rmin, rmax)
        dy = yy - cy
        if wrap:
            dy = (dy + h / 2) % h - h / 2
        burn = np.clip(1.25 - np.hypot(xx - cx, dy) / r + (nz - 0.5), 0, 1)
        arr[..., :3] = arr[..., :3] * (1 - 0.75 * burn[..., None]) + SCORCH / 255 * 0.75 * burn[..., None]
    return arr


def dim_windows(arr, rng, share=0.45):
    """The evacuated district: about ``share`` of the warm and cool window lights on the roofs go
    dark (only bright, unsaturated-ish window pixels; the sodium lamps' pools stay)."""
    rgb = arr[..., :3]
    lum = rgb @ np.array([0.3, 0.59, 0.11])
    lit = (lum > 0.42) & (arr[..., 3] > 0.9)
    off = lit & (rng.random(lum.shape) < share)
    arr[off, :3] = NAVY[1] / 255 * 1.2
    return arr


# --------------------------------------------------------------------------- tile sets

def pre_dawn(w, h):
    """Deep: the sky before dawn mirrored in the lagoon: dark navy-violet clouds, a few stars in the
    west, the east half greying with the coming dawn (only seen through the lagoon's water)."""
    n = raster.fbm(w, h, 96, 911, octaves=4, period=True)
    fine = raster.fbm(w, h, 24, 913, octaves=3, period=True)
    xs = (np.arange(w)[None, :] + 0.5) / w
    base = ORBIT[0] * 0.6 + NAVY[1] * 0.7
    cloud = np.clip((n - 0.42) * 2.0, 0, 1)
    dawn = np.clip(xs - 0.35, 0, 1) ** 1.4 * 1.5
    rgb = base[None, None, :] * (0.8 + 0.3 * fine)[..., None] + (CHITIN[1] * 0.25)[None, None, :] * cloud[..., None]
    rgb = rgb + DAWN[None, None, :] * (dawn * (0.25 + 0.35 * (1 - cloud)))[..., None] * 0.55
    rng = np.random.default_rng(917)
    for _ in range(60):
        x, y = int(rng.integers(w // 2)), int(rng.integers(h))
        rgb[y, x] = np.maximum(rgb[y, x], np.array([150, 160, 200]) * rng.uniform(0.4, 0.85))
    return finish(np.dstack([np.clip(rgb / 255, 0, 1), np.ones((h, w))]), 16)


def wrecks_on(arr, rng, x0, x1, count, wrap=True):
    """Burnt-out cars on a road (static, flat): dark scorched bodies 6 x 11 px with a pale roof
    edge, a scorch halo, some askew; ember specks in a few."""
    h = arr.shape[0]
    for _ in range(count):
        cx, cy = rng.uniform(x0 + 5, x1 - 5), rng.uniform(0, h)
        ang = rng.uniform(-0.5, 0.5) + (math.pi if rng.random() < 0.5 else 0)
        ca, sa = math.cos(ang), math.sin(ang)
        for yy in range(-9, 10):
            for xx in range(-7, 8):
                px, py = int(cx + xx), int(cy + yy) % h
                if not 0 <= px < arr.shape[1]:
                    continue
                lx, ly = xx * ca + yy * sa, -xx * sa + yy * ca
                if abs(lx) <= 3.2 and abs(ly) <= 5.6:
                    edge = abs(lx) > 2.4 or abs(ly) > 4.8
                    tone = (CONCRETE * 0.55 if edge and lx < 0 else SCORCH * 1.3 if edge else SCORCH * 0.8) / 255
                    arr[py, px, :3] = tone
                elif abs(lx) <= 5.5 and abs(ly) <= 8.0:
                    arr[py, px, :3] *= 0.7
        if rng.random() < 0.35:
            arr[int(cy) % h, int(cx), :3] = EMBER / 255 * 0.8
    return arr


def approach_tile():
    """Section 1: Level 08's elevated highway plan (x 170-310) re-dressed: burnt-out traffic on its
    lanes, cracks, scorched and creep-touched roofs either side."""
    rng = np.random.default_rng(921)
    arr = k8.city_tile("third-mainland", 921, "residential", park_share=0.06)
    arr = wrecks_on(arr, rng, 176, 236, 16)
    arr = wrecks_on(arr, rng, 244, 304, 12)
    arr = scorch_blots(arr, rng, 9, 10, 26)
    arr = creep_blots(arr, rng, 5, 8, 18, keep=_highway_mask(arr))
    arr = dim_windows(arr, rng)
    return finish_tile(predawn(arr), 32)


def _highway_mask(arr):
    m = np.zeros(arr.shape[:2], bool)
    m[:, 166:314] = True
    return m


def plaza_district_tile():
    """Section 2: Level 08's avenues plan (avenues at x 112 and 252) re-dressed: office roofs,
    parks, a few creep blots, fewer lit windows."""
    rng = np.random.default_rng(923)
    arr = k8.city_tile("avenues", 923, "office", park_share=0.16)
    arr = creep_blots(arr, rng, 6, 8, 16)
    arr = scorch_blots(arr, rng, 4, 8, 18)
    arr = dim_windows(arr, rng)
    return finish_tile(predawn(arr), 32)


def boulevard_tile():
    """Section 3: Level 08's rooftops plan (the boulevard x 170-290) re-dressed: two tram tracks in
    its middle, abandoned cars on its lanes, residential roofs with creep blots and scorch."""
    rng = np.random.default_rng(925)
    arr = k8.city_tile("rooftops", 925, "residential", park_share=0.05)
    h = arr.shape[0]
    for x in (219, 223, 237, 241):                                   # the rails
        arr[:, x, :3] = CONCRETE / 255 * 1.6
    for u in range(0, h, 6):                                         # sleepers under them
        arr[u % h, 217:226, :3] *= 0.75
        arr[u % h, 235:244, :3] *= 0.75
    arr = wrecks_on(arr, rng, 186, 214, 7)
    arr = wrecks_on(arr, rng, 246, 274, 7)
    arr = creep_blots(arr, rng, 8, 8, 18, keep=_lane_mask(arr, 168, 292))
    arr = scorch_blots(arr, rng, 6, 8, 20)
    arr = dim_windows(arr, rng)
    return finish_tile(predawn(arr), 32)


def _lane_mask(arr, x0, x1):
    m = np.zeros(arr.shape[:2], bool)
    m[:, x0:x1] = True
    return m


def lagoon_tile():
    """Section 4: the lagoon (translucent: the deep layer's pre-dawn sky shows in it), the west
    bank (x 0-110: warehouses, the quay road, the quay edge with bollards and lamps), the Okonjo
    Bridge's deck running up the screen (x 220-300: four lanes, a median with lamps, barriers), its
    piers and its shadow on the water, a few abandoned cars on the northbound lanes."""
    h = TILE_H
    rng = np.random.default_rng(927)
    c = Canvas(W, h, f=2, wrap=True, rgb=GROUND)
    c.grain(927, 0.3)
    hh, ww = c.hm.shape
    rip = noise(ww, hh, [24, 12, 6], 929)
    c.paint((BANK_X, 0, W, h), rgb_fn=lambda xx, uu: np.zeros(xx.shape + (3,)), alpha=0.62, height=0.0)
    sub = c.alb[:, BANK_X * 2:]
    sub[:] = (ORBIT[1] * 0.45 + NAVY[1] * 0.55)[None, None, :] * (0.8 + 0.4 * rip[:, BANK_X * 2:])[..., None]
    xs = (np.arange(BANK_X * 2, ww) + 0.5) / ww
    streak = noise(ww // 8, hh, [24, 12], 933)
    streak = np.repeat(streak, 8, axis=1)[:, BANK_X * 2:]
    sub += DAWN[None, None, :] * (0.55 * np.clip(xs - 0.35, 0, 1) ** 1.2 * (0.5 + 0.9 * np.clip(streak - 0.35, 0, 1)))[..., None]
    c.rect(BANK_X - 14, 0, BANK_X, h, rgb=CONCRETE * 1.25, height=2.5)            # the quay edge
    for u in range(8, h, 24):
        c.disc(BANK_X - 3, u, 1.2, rgb=CONCRETE * 0.7, height=3.4)
    c.rect(56, 0, BANK_X - 14, h, rgb=ROAD * 1.1, height=2.4)                      # the quay road
    for u in range(0, h, 12):
        c.rect(76, u, 77, u + 6, rgb=NAVY[3] * 0.7, height=2.4)
    for k in range(h // PERIOD):                                                   # warehouses on the bank
        u0, u1 = k * PERIOD + LOT0, k * PERIOD + LOT1
        c.rect(0, k * PERIOD - LOT0, 56, k * PERIOD + LOT0, rgb=ROAD * 1.1, height=2.4)
        low_roof(c, 3, u0 + 3, 53, u1 - 3, rng, height=rng.uniform(4.0, 6.0), style="office")
    lamps = [(BANK_X - 12, u) for u in range(20, h, 48)]
    for bu in (70, 300, 520, 760, 880):                                            # boats moored at the quay
        blen, bw = rng.uniform(26, 44), rng.uniform(9, 13)
        bx = BANK_X + bw / 2 + 2
        c.paint((bx - bw, bu - blen / 2, bx + bw, bu + blen / 2),
                mask_fn=lambda xx, uu, bx=bx, bu=bu, blen=blen, bw=bw:
                ((xx - bx) / (bw / 2)) ** 2 + np.clip(np.abs(uu - bu) / (blen / 2) - 0.4, 0, 1) ** 2 * 2.8 <= 1,
                rgb=(CONCRETE * 0.9 if bu % 3 else NAVY[2] * 0.9), height=2.0, alpha=1.0)
        c.rect(bx - bw * 0.25, bu - blen * 0.1, bx + bw * 0.25, bu + blen * 0.2, rgb=CONCRETE * 1.3, height=3.6)
    x0, x1 = DECK
    for u in range(0, h, 96):                                                      # the piers under the deck
        c.rect(x0 + 8, u - 5, x1 - 8, u + 5, rgb=CONCRETE * 0.8, height=3.0, alpha=1.0)
    c.rect(x0, 0, x1, h, rgb=ROAD * 1.15, height=7.0, alpha=1.0)                    # the deck
    c.rect(x0, 0, x0 + 3, h, rgb=CONCRETE * 1.4, height=8.4)
    c.rect(x1 - 3, 0, x1, h, rgb=CONCRETE * 1.4, height=8.4)
    mid = (x0 + x1) / 2
    c.rect(mid - 3, 0, mid + 3, h, rgb=CONCRETE * 1.1, height=7.4)
    for lx in (x0 + 20, mid - 20, mid + 20, x1 - 20):                              # lane lines
        for u in range(0, h, 12):
            c.rect(lx - 0.5, u, lx + 0.5, u + 6, rgb=NAVY[3] * 0.9, height=7.0)
    for u in range(24, h, 48):
        c.rect(x0 + 3, u, x1 - 3, u + 1, rgb=ROAD * 0.8, height=7.0)                # expansion joints
        lamps.append((mid, u))
    arr = c.render()
    water = arr[..., 3] < 0.8
    for u in range(h):                                                             # the deck's shadow on the water
        r = h - 1 - u
        arr[r, x1:x1 + 13, :3] *= 0.55
        arr[r, x1:x1 + 13, 3] = np.where(water[r, x1:x1 + 13], 0.8, arr[r, x1:x1 + 13, 3])
    arr = wrecks_on(arr, rng, mid + 6, x1 - 6, 6)
    for _ in range(90):                                                            # flotsam on the water
        fx, fu = int(rng.uniform(x1 + 16, W - 2)), int(rng.uniform(0, h))
        r = h - 1 - fu
        if water[r, fx]:
            arr[r, fx:fx + int(rng.integers(1, 4)), :3] = (SCORCH * 1.6 + NAVY[1] * 0.5) / 255
            arr[r, fx:fx + 3, 3] = np.where(water[r, fx:fx + 3], 1.0, arr[r, fx:fx + 3, 3])
    land = ~water
    specks_land = arr.copy()
    specks(specks_land, rng, 56 * h // 120)
    arr[land] = specks_land[land]
    for lx, lu in lamps:
        add_glow(arr, lx, lu, 4.4, SODIUM * 1.25, wrap=True)
        if lx == mid:
            for sx in (x0 - 10, x1 + 14):                                         # their streaks on the water
                for k in range(5):
                    add_glow(arr, sx + rng.uniform(-2, 2), lu - 3 - 3 * k, 2.2, SODIUM * 0.25, wrap=True)
    arr = dim_windows(arr, rng)
    return finish_tile(predawn(arr), 32, water=0.62)


def lobby_district_tile():
    """Section 5: the arcology district: wide podium roofs either side of the avenue (x 196-284,
    the bridge's road carried on), plazas, heavy creep and scorch, few lit windows."""
    rng = np.random.default_rng(931)
    arr = k8.city_tile("arcology", 931, "office", park_share=0.1)
    arr = creep_blots(arr, rng, 14, 10, 24, keep=_lane_mask(arr, 196, 284))
    arr = scorch_blots(arr, rng, 10, 10, 26)
    arr = dim_windows(arr, rng, 0.6)
    return finish_tile(predawn(arr), 32)


# --------------------------------------------------------------------------- banks and wisps

def east_lit(img, strength):
    """A bank or wisp tile lit grey from the east: its colour lifted toward DAWN across x."""
    a = np.array(img).astype(np.float64)
    xs = (np.arange(a.shape[1]) + 0.5) / a.shape[1]
    k = (strength * xs ** 1.5)[None, :, None]
    a[..., :3] = np.clip(a[..., :3] * (1 - k) + DAWN[None, None, :] * k, 0, 255)
    return Image.fromarray(a.astype(np.uint8), "RGBA")


def dust(w, h):
    """Light: street dust and smoke wisps, grey-brown, thin (about 15 % coverage), lit amber from
    the lamps below and grey from the east; parted over the ground units."""
    cols = [(46, 42, 48), (74, 68, 70), (104, 98, 96)]
    part = k8.bank_parting("dust", w, h, 0, 0.5)
    img = k8.periodic_bank(w, h, 941, 0.64, 0.14, cols, 120, 3.0, JOV[3] * 0.22, 943, part=(part, 0.15))
    return artkit.quantize_set([east_lit(img, 0.35)], 12)[0]


def smoke(w, h):
    """Medium: river smoke and the burning towers' smoke, brown-grey, lit orange from below and
    grey from the east (about 20 % coverage), thinned over the ground units while it is drawn."""
    cols = [(34, 28, 34), (62, 54, 58), (94, 84, 82)]
    part = k8.bank_parting("smoke", w, h, 0, 0.5)
    img = k8.periodic_bank(w, h, 951, 0.57, 0.16, cols, 150, 3.0, MARS[3] * 0.2, 953, part=(part, 0.13))
    return artkit.quantize_set([east_lit(img, 0.3)], 12)[0]


def dust_heavy(w, h):
    """Heavy: the collapse's dust (the event-triggered peak): pale concrete dust banks, about 40 %
    of the screen, lit grey from the east and the lamps' amber from below."""
    cols = [(70, 66, 70), (112, 106, 104), (150, 144, 138)]
    img = k8.periodic_bank(w, h, 961, 0.5, 0.2, cols, 165, 3.0, JOV[3] * 0.14, 963)
    return artkit.quantize_set([east_lit(img, 0.25)], 12)[0]


def mist(w, h):
    return east_lit(k8.mist(w, h), 0.4)


def ash(w, h):
    return k8.ash(w, h)


# --------------------------------------------------------------------------- the targets' structures

SPOT, PAINT, SPOT_LAMP = k8.SPOT, k8.PAINT, k8.SPOT_LAMP
MORTAR_ROOF = (96, 92)
COCOON_ROOF = (124, 112)


def structure_kind(section, kind):
    """Which flat structure a ground target's unit stands on, by section and kind."""
    if section == 2:
        return "unity-plaza"
    if section == 3:
        return {"polyp-mortar": "mortar-roof", "cocoon": "cocoon-roof"}.get(kind, "roof-nest")
    if section == 4:
        return "quay-pads" if kind == "polyp-mortar" else "bridgehead"
    if section == 5:
        return "lobby-plaza"
    return "roof-nest"


def structures():
    """The structures: id -> dict(kind, x, centre, w, h, units [(kind, x, pos, w, h)], placements
    [(x, centre)]). A mortar roof is one piece per unit (the same image); the others one piece
    round all their units."""
    groups = {}
    for _, section, kind, x, pos, w, h in target_units():
        groups.setdefault(structure_kind(section, kind), []).append((kind, x, pos, w, h))
    out = {}
    for sid, units in groups.items():
        x0 = min(x - w / 2 for _, x, _, w, _ in units)
        x1 = max(x + w / 2 for _, x, _, w, _ in units)
        p0 = min(p - h / 2 for _, _, p, _, h in units)
        p1 = max(p + h / 2 for _, _, p, _, h in units)
        if sid == "mortar-roof":
            w, h = MORTAR_ROOF
            out[sid] = {"x": units[0][1], "centre": units[0][2], "w": w, "h": h, "units": [units[0]],
                        "placements": [(u[1], u[2]) for u in units]}
            continue
        if sid == "cocoon-roof":
            w, h = COCOON_ROOF
            cx, cy = units[0][1] + 4, units[0][2] - 4
        elif sid == "unity-plaza":
            w, h = W, int(2 * math.ceil((p1 - p0 + 150) / 2))
            cx, cy = W / 2, (p0 + p1) / 2
        elif sid == "bridgehead":
            w, h = DECK[0] + 4, int(2 * math.ceil((p1 - p0 + 170) / 2))
            cx, cy = w / 2, (p0 + p1) / 2 - 10
        elif sid == "lobby-plaza":
            left = min(200, x0 - 40)
            w, h = int(2 * math.ceil((W - left) / 2)), int(2 * math.ceil((p1 - p0 + 150) / 2))
            cx, cy = W - w / 2, (p0 + p1) / 2
        elif sid == "quay-pads":
            w, h = int(2 * math.ceil((x1 - x0 + 40) / 2)), int(2 * math.ceil((p1 - p0 + 40) / 2))
            cx, cy = (x0 + x1) / 2, (p0 + p1) / 2
        else:
            w = int(2 * math.ceil((x1 - x0 + 68) / 2))
            h = int(2 * math.ceil((p1 - p0 + 68) / 2))
            cx, cy = (x0 + x1) / 2, (p0 + p1) / 2
        out[sid] = {"x": round(cx), "centre": cy, "w": w, "h": h, "units": units, "placements": [(round(cx), cy)]}
    return out


STRUCTURES = structures()


def local_units(sid):
    """The structure's units in its piece's px: (kind, x, u up from the bottom, w, h)."""
    s = STRUCTURES[sid]
    return [(k, x - (s["x"] - s["w"] / 2), p - (s["centre"] - s["h"] / 2), w, h) for k, x, p, w, h in s["units"]]


def spot(c, x, u, r, base, lights):
    """A turret's or mortar's pale painted spot with its ring and four lamps (as Level 08's)."""
    c.disc(x, u, r + 2, rgb=SPOT * 0.45, height=base - 0.5)
    c.disc(x, u, r, rgb=SPOT, height=base - 0.5)
    c.ring(x, u, r + 4.5, 1.5, rgb=PAINT)
    for k in range(4):
        a = math.pi / 4 + k * math.pi / 2
        lights.append((x + math.cos(a) * (r + 6), u + math.sin(a) * (r + 6), SPOT_LAMP))


def unit_spots(c, sid, base, lights):
    """Spots under the turrets and mortars of a structure; the nodes' and cocoon's places are
    returned for their creep."""
    nests = []
    for kind, x, u, w, _ in local_units(sid):
        if kind in ("spine-turret", "polyp-mortar"):
            spot(c, x, u, 17 if kind == "spine-turret" else 19, base, lights)
        else:
            nests.append((x, u, w))
    return nests


def creep_spread(arr, places, reach, seed, inner=0.45, links=True):
    """Creep spreading from each place across a flat piece (native float RGBA): a ragged teal-black
    field to ``reach`` px round it (the node's own 192 px creep sprite covers its middle), tendrils
    winding out to about 1.6 x ``reach`` with a glowing teal vein down their middle, bubbles along
    the field's rim; with ``links`` a tendril joins neighbouring places."""
    h, w = arr.shape[:2]
    rng = np.random.default_rng(seed)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64)
    uu = h - 1 - yy
    nz = raster.fbm(w, h, 20, seed, octaves=3, period=False)
    field = np.full((h, w), 9.0)
    for x, u, _ in places:
        field = np.minimum(field, np.hypot(xx - x, uu - u) / reach + (nz - 0.5) * 0.7)
    solid = arr[..., 3] > 0.5
    layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    mid = tuple(int(v) for v in CREEP_MID * 0.85) + (255,)
    vein = tuple(int(v) for v in CREEP_VEIN) + (255,)
    lines = []
    for x, u, _ in places:
        for k in range(7):
            ang = rng.uniform(0, 2 * math.pi)
            px, pu = x, u
            pts = [(px, pu)]
            for _ in range(int(reach * 1.6 / 6)):
                ang += rng.uniform(-0.35, 0.35)
                px, pu = px + math.cos(ang) * 6, pu + math.sin(ang) * 6
                pts.append((px, pu))
            lines.append(pts)
    if links:
        for (x0, u0, _), (x1, u1, _) in zip(places, places[1:]):
            pts = [(x0 + (x1 - x0) * s_ + rng.uniform(-6, 6), u0 + (u1 - u0) * s_ + 10 * math.sin(s_ * 6)) for s_ in np.linspace(0, 1, 16)]
            lines.append(pts)
    for pts in lines:
        n = len(pts)
        for i in range(n - 1):
            width = max(1, int(round(4 * (1 - i / n))))
            (ax, au), (bx, bu) = pts[i], pts[i + 1]
            d.line([(ax, h - 1 - au), (bx, h - 1 - bu)], fill=mid, width=width + 1)
        for i in range(int(n * 0.75)):
            (ax, au), (bx, bu) = pts[i], pts[i + 1]
            d.line([(ax, h - 1 - au), (bx, h - 1 - bu)], fill=vein, width=1)
    body = (field < 1.0) & solid
    core = field < inner
    col = np.where(core[..., None], CREEP_DARK, CREEP_DARK * 0.55 + CREEP_MID * 0.45) / 255
    arr[body, :3] = arr[body, :3] * 0.15 + col[body] * 0.85
    lay = np.array(layer).astype(np.float64) / 255
    put = (lay[..., 3] > 0) & solid
    arr[put, :3] = lay[put, :3]
    rim = np.argwhere((field > 0.78) & (field < 1.0) & solid)
    for r, c_ in rim[rng.choice(len(rim), size=min(len(rim), int(len(rim) / 60) + 1), replace=False)] if len(rim) else []:
        rad = rng.uniform(1.2, 3.2)
        sel = (np.hypot(xx - c_, yy - r) <= rad) & solid
        arr[sel, :3] = CREEP_MID / 255 * 1.1
        if 0 <= r - 1 and 0 <= c_ - 1:
            arr[r - 1, c_ - 1, :3] = np.minimum(1, CREEP_MID / 255 * 1.7)
    return arr


def lit(arr, lights, radius=3.0):
    h = arr.shape[0]
    for lx, lu, col in lights:
        add_glow(arr, lx, lu, radius, col * 0.6)
        if 0 <= int(lu) < h and 0 <= int(lx) < arr.shape[1]:
            arr[h - 1 - int(lu), int(lx), :3] = np.minimum(1, col / 255 * 1.1)
    return arr


def clear_rim(arr):
    arr[0, :, 3] = arr[-1, :, 3] = 0
    arr[:, 0, 3] = arr[:, -1, 3] = 0
    return arr


def unity_plaza(w, h):
    """Unity Plaza (full width): pale slab paving, a radial pattern round the dry fountain and the
    Unity column in its middle (between the turret pair's line and the nodes'), benches and planters
    in rings, park strips with trees at both sides, kerbs and lamp posts along its north and south
    edges, spots under the turret pair, the nodes' creep spreading across the paving toward the
    fountain."""
    rng = np.random.default_rng(971)
    c = Canvas(w, h, f=2, alpha=0.0)
    m = 10
    pave = CONCRETE * 1.35 + NAVY[2] * 0.35
    c.rect(0, m, w, h - m, rgb=ROAD * 1.2, height=0.3, alpha=1.0)                    # the kerb street
    c.rect(0, m + 6, w, h - m - 6, rgb=pave, height=0.6)
    for x in range(0, w, 24):                                                       # slabs, two tones
        for u in range(m + 6, h - m - 6, 24):
            if (x // 24 + u // 24) % 2:
                c.rect(x, u, x + 24, min(u + 24, h - m - 6), rgb=pave * 0.92, height=0.6)
    for x in range(0, w, 12):
        c.rect(x, m + 6, x + 0.6, h - m - 6, rgb=pave * 0.8, height=0.6)
    for u in range(m + 6, h - m - 6, 12):
        c.rect(0, u, w, u + 0.6, rgb=pave * 0.8, height=0.6)
    for x0, x1 in ((0, 58), (w - 58, w)):                                           # park strips
        c.rect(x0, m + 10, x1, h - m - 10, rgb=GREEN[1] * 0.5, height=0.4)
        c.rect((x0 + x1) / 2 - 1.5, m + 10, (x0 + x1) / 2 + 1.5, h - m - 10, rgb=pave * 0.8, height=0.45)
        for _ in range(int((x1 - x0) * (h - 2 * m) / 300)):
            tx = rng.uniform(x0 + 6, x1 - 6)
            if abs(tx - (x0 + x1) / 2) > 5:
                c.spots.append((tx, rng.uniform(m + 14, h - m - 14), "big"))
    units = local_units("unity-plaza")
    nodes = [u for k, _, u, _, _ in units if k == "hive-node"]
    guns = [u for k, _, u, _, _ in units if k != "hive-node"]
    fx = w / 2
    fu = (min(nodes) + max(guns)) / 2 if nodes and guns else h / 2
    rings = (46, 70, 94)
    c.paint((60, m + 6, w - 60, h - m - 6), mask_fn=lambda xx, uu:
            (np.abs((np.hypot(xx - fx, uu - fu) + 7) % 24 - 12) < 0.7) & (np.hypot(xx - fx, uu - fu) < 120)
            | ((np.abs(np.sin(np.arctan2(uu - fu, xx - fx) * 8)) < 0.05) & (np.hypot(xx - fx, uu - fu) < 120)
               & (np.hypot(xx - fx, uu - fu) > 34)), rgb=JOV[2] * 0.5 + pave * 0.5, height=0.6)
    for r in rings[1:]:                                                             # benches and planters
        for k in range(10):
            a = k * 2 * math.pi / 10 + (0.3 if r == rings[2] else 0)
            bx, bu = fx + math.cos(a) * r, fu + math.sin(a) * r
            if k % 2:
                c.disc(bx, bu, 5, rgb=CONCRETE * 1.4, height=1.6)
                c.spots.append((bx, bu, "small"))
            else:
                c.line(bx - math.sin(a) * 5, bu + math.cos(a) * 5, bx + math.sin(a) * 5, bu - math.cos(a) * 5, 2.2,
                       rgb=JOV[1] * 0.9, height=1.2)
    c.disc(fx, fu, 32, rgb=CONCRETE * 1.5, height=1.8)                              # the dry fountain
    c.disc(fx, fu, 28, rgb=SCORCH * 1.6 + NAVY[1] * 0.5, height=0.4)
    for k in range(7):
        a = k * 2 * math.pi / 7 + 0.4
        c.line(fx + math.cos(a) * 10, fu + math.sin(a) * 10, fx + math.cos(a + 0.35) * 26, fu + math.sin(a + 0.35) * 26,
               0.8, rgb=SCORCH, height=0.4)
    c.disc(fx, fu, 9, rgb=CONCRETE * 1.3, height=2.6)                               # the Unity column's plinth
    c.disc(fx, fu, 4.5, rgb=CONCRETE * 1.7, height=14.0)                            # and the column
    lights = []
    nests = unit_spots(c, "unity-plaza", 1.2, lights)
    for x in range(30, w, 60):
        lights += [(x, m + 3, SODIUM * 1.2), (x, h - m - 3, SODIUM * 1.2)]
    for k in range(8):
        a = k * math.pi / 4
        lights.append((fx + math.cos(a) * 112, fu + math.sin(a) * 112, SODIUM * 1.1))
    c.grain(973, 0.16, cells=(4, 2))
    arr = c.render()
    arr = creep_spread(arr, nests or [(fx, fu, 76)], 100, 975)
    tr = trees()
    for tx, tu, kind in c.spots:
        paste_sprite(arr, tr[kind][int(rng.integers(len(tr[kind])))], tx, tu)
    arr = lit(arr, lights)
    return finish(predawn(clear_rim(arr), 0.0), 32)


def low_roof_piece(w, h, sid, seed, base=5.0, creep=0.0):
    """A low roof drawn flat (D1 = a) with spots under its turrets or mortars; its shadow down and
    right; ``creep`` > 0 spreads creep from its non-turret units (the cocoon)."""
    rng = np.random.default_rng(seed)
    c = Canvas(w, h, f=2, alpha=0.0)
    m = 6
    x0, u0, x1, u1 = m, m + 6, w - m - 6, h - m
    tone = NAVY[1] * 0.55 + NAVY[2] * 0.45
    c.rect(x0, u0, x1, u1, rgb=tone, height=base, alpha=1.0)
    c.rect(x0 + 3, u0 + 3, x1 - 3, u1 - 3, rgb=tone * 0.85, height=base - 0.8)
    lights = []
    nests = unit_spots(c, sid, base, lights)
    for _ in range(2):
        bx, bu = rng.uniform(x0 + 4, x0 + 14), rng.uniform(u0 + 4, u1 - 14)
        c.rect(bx, bu, bx + 7, bu + 6, rgb=NAVY[2], height=base + 2.0)
    c.grain(seed, 0.18, cells=(4, 2))
    arr = c.render()
    arr = drop_shadow(arr, np.where(arr[..., 3] > 0.5, base, 0.0), 0.45)
    if creep and nests:
        arr = creep_spread(arr, nests, creep, seed + 1, inner=0.55, links=False)
    arr = lit(arr, lights)
    return finish(predawn(arr, STRUCTURES[sid]["x"] - w / 2), 32)


def mortar_roof(w, h):
    return low_roof_piece(w, h, "mortar-roof", 977)


def roof_nest(w, h):
    return low_roof_piece(w, h, "roof-nest", 979)


def cocoon_roof(w, h):
    return low_roof_piece(w, h, "cocoon-roof", 981, creep=46)


def quay_pads(w, h):
    """Pale concrete pads on the far bank's quay under the mortars, ringed and lamp-lit."""
    c = Canvas(w, h, f=2, alpha=0.0)
    lights = []
    for kind, x, u, _, _ in local_units("quay-pads"):
        c.disc(x, u, 23, rgb=CONCRETE * 1.3, height=3.0, alpha=1.0)
        spot(c, x, u, 19, 3.0, lights)
    arr = c.render()
    arr = drop_shadow(arr, np.where(arr[..., 3] > 0.5, 3.0, 0.0), 0.4)
    return finish(predawn(clear_rim(lit(arr, lights)), STRUCTURES["quay-pads"]["x"] - w / 2), 32)


def bridgehead(w, h):
    """The Okonjo Bridge's bridgehead on the far bank: reclaimed land widening the bank (x 0-110 in
    the tile) out to the deck, its quay wall along the water, the approach ramp coming down off the
    deck to the west, a paved approach plaza with spots under the turret pair and the nodes' creep."""
    rng = np.random.default_rng(983)
    c = Canvas(w, h, f=2, alpha=0.0)
    span = h - 40

    def edge(uu):
        s = np.clip((uu - 20) / span, 0, 1)
        return BANK_X - 6 + (w - 8 - BANK_X) * np.clip(np.sin(s * math.pi) * 1.6, 0, 1)
    land = lambda xx, uu: (xx <= edge(uu)) & (uu > 4) & (uu < h - 4)
    c.paint((0, 0, w, h), mask_fn=land, rgb=ROAD * 1.1, height=2.4, alpha=1.0)
    c.paint((0, 0, w, h), mask_fn=lambda xx, uu: land(xx, uu) & (xx > edge(uu) - 8) & (xx > BANK_X - 14),
            rgb=CONCRETE * 1.25, height=2.9)                                        # the quay wall
    pave = CONCRETE * 1.3 + NAVY[2] * 0.35
    c.paint((0, 0, w, h), mask_fn=lambda xx, uu: land(xx, uu) & (xx < edge(uu) - 10) & (xx > 8) & (uu > 30) & (uu < h - 30),
            rgb=pave, height=2.6)
    c.paint((0, 0, w, h), mask_fn=lambda xx, uu: land(xx, uu) & (xx < edge(uu) - 10) & (xx > 8) & (uu > 30) & (uu < h - 30)
            & (((xx // 24 + uu // 24) % 2) == 1), rgb=pave * 0.9, height=2.6)
    ru0, ru1 = h * 0.72, h * 0.42                                                    # the ramp off the deck
    c.line(w + 4, ru0, BANK_X - 10, ru1, 22, rgb=ROAD * 1.25, height=3.6)
    c.line(w + 4, ru0 + 10.5, BANK_X - 10, ru1 + 10.5, 1.4, rgb=CONCRETE * 1.4, height=4.6)
    c.line(w + 4, ru0 - 10.5, BANK_X - 10, ru1 - 10.5, 1.4, rgb=CONCRETE * 1.4, height=4.6)
    lights = []
    nests = unit_spots(c, "bridgehead", 2.6, lights)
    for u in np.arange(30, h - 30, 40):
        e = float(edge(np.array([u]))[0])
        if e > BANK_X:
            lights.append((e - 4, u, SODIUM * 1.2))
    c.grain(985, 0.18, cells=(4, 2))
    arr = c.render()
    arr = drop_shadow(arr, np.where(arr[..., 3] > 0.5, 2.6, 0.0), 0.4)
    if nests:
        arr = creep_spread(arr, nests, 96, 987)
    specks(arr, rng, w * h // 400, wrap=False)
    return finish(predawn(clear_rim(lit(arr, lights)), 0.0), 32)


ARCOLOGY = {"foot": (200, 180), "height": 1.5}
ARCOLOGY_B = {"foot": (150, 140), "height": 1.35}


def arcology_place():
    """The Ndidi Arcology's footprint centre (x, ground position): left of the lobby plaza, its foot's
    right edge where the plaza begins, a little north of the nodes, so it looms into view during
    the hold and falls across the middle of the screen."""
    s = STRUCTURES.get("lobby-plaza")
    x0 = (s["x"] - s["w"] / 2) if s else 200
    nodes = [p for k, _, p, _, _ in (s["units"] if s else []) if k == "hive-node"]
    pos = (sum(nodes) / len(nodes) if nodes else scroll_at(164) + SCREEN) + 30
    return x0 - ARCOLOGY["foot"][0] / 2, pos


def lobby_plaza(w, h):
    """The Ndidi Arcology's lobby forecourt, from the tower's foot (its left edge) to the play field's
    right: a radial paving round a sunken court, planters, the lobby's glass canopy along the tower,
    spots for hard's turret pair, the nodes' heavy creep."""
    rng = np.random.default_rng(989)
    c = Canvas(w, h, f=2, alpha=0.0)
    m = 10
    pave = CONCRETE * 1.0 + NAVY[2] * 0.3
    c.rect(0, m, w, h - m, rgb=ROAD * 1.2, height=0.3, alpha=1.0)
    c.rect(0, m + 6, w - 4, h - m - 6, rgb=pave, height=0.6)
    cx, cu = w * 0.55, h * 0.5
    c.paint((0, m + 6, w - 4, h - m - 6),
            mask_fn=lambda xx, uu: (np.abs(np.hypot(xx - cx, uu - cu) % 14 - 7) < 0.5)
            | (np.abs(np.sin(np.arctan2(uu - cu, xx - cx) * 12)) < 0.04), rgb=pave * 0.8, height=0.6)
    c.disc(cx, cu, 34, rgb=pave * 0.7, height=0.1)                                   # the sunken court
    c.ring(cx, cu, 34, 3, rgb=CONCRETE * 1.4, height=1.2)
    c.rect(0, m + 6, 16, h - m - 6, rgb=ORBIT[1] * 0.55 + NAVY[2] * 0.3, height=4.0)   # the lobby canopy
    for u in np.arange(m + 10, h - m - 10, 8):
        c.rect(1, u, 15, u + 1, rgb=NAVY[3] * 0.8, height=4.2)
    for _ in range(8):                                                               # planters
        px, pu = rng.uniform(40, w - 30), rng.uniform(m + 20, h - m - 20)
        if np.hypot(px - cx, pu - cu) < 50:
            continue
        c.rect(px - 7, pu - 7, px + 7, pu + 7, rgb=CONCRETE * 1.3, height=1.8)
        c.spots.append((px, pu, "small"))
    lights = []
    nests = unit_spots(c, "lobby-plaza", 1.2, lights)
    for u in np.arange(m + 20, h - m - 20, 36):
        lights.append((20, u, WARM_WIN * 0.9))
    c.grain(991, 0.16, cells=(4, 2))
    arr = c.render()
    arr = creep_spread(arr, nests or [(cx, cu, 76)], 110, 993, inner=0.4)
    tr = trees()
    for tx, tu, kind in c.spots:
        paste_sprite(arr, tr[kind][int(rng.integers(len(tr[kind])))], tx, tu)
    arr = lit(arr, lights)
    return finish(predawn(clear_rim(arr), W - w), 32)


# --------------------------------------------------------------------------- set dressing

def graded(img, x0=None, colours=32):
    """A finished kit image (Level 08's generators) with the pre-dawn grade, re-posterized."""
    arr = np.array(img.convert("RGBA")).astype(np.float64) / 255
    return quantize_lit([to_img(predawn(arr, x0))], colours)[0]


def barricade(w, h):
    """The CDF checkpoint across the elevated highway (its deck x 170-310): two staggered rows of
    pale jersey barriers leaving a chicane, sandbag nests at the barriers' ends, a checkpoint booth,
    two floodlight masts throwing cool pools, a burnt-out APC hulk askew in the chicane."""
    c = Canvas(w, h, f=2, alpha=0.0)
    rng = np.random.default_rng(995)
    jersey = CONCRETE * 1.8
    x0, x1 = w / 2 - 68, w / 2 + 68                                                 # the deck's barriers
    rows = ((h * 0.7, x0 + 2, w / 2 + 22), (h * 0.36, w / 2 - 22, x1 - 2))
    for u, a0, a1 in rows:
        for x in np.arange(a0, a1 - 10, 12):
            c.rect(x, u - 3.5, x + 11, u + 3.5, rgb=jersey * rng.uniform(0.9, 1.05), height=3.0, alpha=1.0)
            c.rect(x + 1, u - 1, x + 10, u + 1, rgb=jersey * 1.12, height=3.3)
    for sx, u in ((w / 2 + 30, h * 0.7), (w / 2 - 30, h * 0.36)):                    # sandbag nests
        for k in range(9):
            a = math.pi * k / 8
            c.disc(sx + math.cos(a) * 9, u + math.sin(a) * 7 * (1 if u < h / 2 else -1), 2.6,
                   rgb=JOV[1] * 0.9 + CONCRETE * 0.4, height=2.4, alpha=1.0)
    c.rect(x0 + 6, h * 0.42, x0 + 20, h * 0.58, rgb=HULL_GREY, height=6.0, alpha=1.0)    # the booth
    c.rect(x0 + 8, h * 0.44, x0 + 18, h * 0.56, rgb=HULL_GREY * 1.2, height=6.4)
    ax, au, ang = w / 2 + 4, h * 0.53, 0.35                                         # the APC hulk, askew
    ca, sa = math.cos(ang), math.sin(ang)
    c.paint((ax - 26, au - 18, ax + 26, au + 18),
            mask_fn=lambda xx, uu: (np.abs((xx - ax) * ca + (uu - au) * sa) < 21)
            & (np.abs(-(xx - ax) * sa + (uu - au) * ca) < 9), rgb=SCORCH * 2.2 + NAVY[1] * 0.4, height=5.0, alpha=1.0)
    c.paint((ax - 16, au - 12, ax + 16, au + 12),
            mask_fn=lambda xx, uu: (np.abs((xx - ax) * ca + (uu - au) * sa) < 8)
            & (np.abs(-(xx - ax) * sa + (uu - au) * ca) < 5), rgb=SCORCH * 1.5, height=6.5)
    lights = []
    for mx in (x0 - 6, x1 + 6):
        c.disc(mx, h * 0.5, 2.2, rgb=CONCRETE * 1.4, height=10.0, alpha=1.0)
        lights.append((mx, h * 0.5))
    arr = c.render()
    arr = drop_shadow(arr, np.where(arr[..., 3] > 0.5, 3.0, 0.0), 0.45)
    for lx, lu in lights:
        add_glow(arr, lx + (18 if lx < w / 2 else -18), lu, 20.0, np.array([120, 140, 190]) * 0.3)
        arr[h - 1 - int(lu), int(lx), :3] = [0.85, 0.9, 1.0]
    add_glow(arr, ax + 6, au - 2, 3.0, EMBER * 0.7)
    return finish(predawn(clear_rim(arr), 240 - w / 2), 32)


def highway_breach(w, h):
    """A fallen span of the elevated highway: the deck broken off in jagged edges, the street below
    dark with a rubble heap and bent rebar, scorch, an ember or two."""
    c = Canvas(w, h, f=2, alpha=0.0)
    rng = np.random.default_rng(997)
    nz = raster.fbm(w * 2, h * 2, 6, 999, octaves=3, period=False)
    lo, hi = h * 0.3, h * 0.72
    hole = lambda xx, uu: (uu > lo + 5 * np.sin(xx * 0.21) + 3 * np.sin(xx * 0.7 + 1)) & \
        (uu < hi + 5 * np.sin(xx * 0.17 + 2) + 3 * np.sin(xx * 0.6)) & (xx > 4) & (xx < w - 4)
    c.paint((0, 0, w, h), mask_fn=hole, rgb=GROUND * 1.2 + SCORCH * 0.3, height=0.0, alpha=1.0)
    for _ in range(70):                                                              # the fallen deck's rubble
        rx, ru = rng.uniform(10, w - 10), rng.uniform(lo + 6, hi - 6)
        c.disc(rx, ru, rng.uniform(2, 6), rgb=(ROAD * 1.2 + CONCRETE * 0.4) * rng.uniform(0.6, 1.0),
               height=rng.uniform(0.5, 4.5), maxh=True)
    for _ in range(9):
        x0 = rng.uniform(10, w - 30)
        u0 = rng.choice([lo + 2, hi - 2])
        c.line(x0, u0, x0 + rng.uniform(-8, 8), u0 + rng.uniform(-14, 14) * (1 if u0 < h / 2 else -1), 0.7,
               rgb=MARS[1] * 0.8, height=5.0)
    for _ in range(14):
        c.disc(rng.uniform(10, w - 10), rng.uniform(lo, hi), rng.uniform(0.6, 1.4), emis=EMBER * rng.uniform(0.3, 0.6),
               rgb=SCORCH)
    arr = c.render(ambient=0.3)
    return finish(predawn(clear_rim(arr), 240 - w / 2), 32)


def tram(w, h):
    """The burnt-out tram on the boulevard's tracks: two articulated cars, the roofs scorched black
    with a few panels of the pale green livery left, a pantograph, blown-out windows with embers."""
    c = Canvas(w, h, f=4, alpha=0.0)
    rng = np.random.default_rng(1001)
    cx = w / 2
    livery = GREEN[2] * 0.45 + CONCRETE * 0.8
    cars = ((8, h / 2 - 2), (h / 2 + 2, h - 8))
    for u0, u1 in cars:
        c.paint((cx - 12, u0, cx + 12, u1), mask_fn=lambda xx, uu, u0=u0, u1=u1:
                (np.abs(xx - cx) <= 11.5 - np.clip(3 - (uu - u0), 0, 3) - np.clip(3 - (u1 - uu), 0, 3)),
                rgb=livery, height=6.0, alpha=1.0)
        c.rect(cx - 11, u0 + 3, cx - 9.5, u1 - 3, rgb=NAVY[0], height=5.4)          # window bands
        c.rect(cx + 9.5, u0 + 3, cx + 11, u1 - 3, rgb=NAVY[0], height=5.4)
        for _ in range(int((u1 - u0) / 5)):
            sx, su = rng.uniform(cx - 10, cx + 10), rng.uniform(u0 + 2, u1 - 2)
            c.disc(sx, su, rng.uniform(3, 6), rgb=SCORCH * rng.uniform(0.9, 1.5), height=6.0)
        for u in np.arange(u0 + 6, u1 - 4, 9):
            c.rect(cx - 7, u, cx + 7, u + 0.8, rgb=SCORCH * 0.8, height=6.1)
    c.rect(cx - 5, h / 2 - 3, cx + 5, h / 2 + 3, rgb=SCORCH * 1.4, height=5.0)        # the articulation
    c.line(cx - 6, h * 0.7, cx + 6, h * 0.76, 1.0, rgb=CONCRETE * 1.2, height=7.6)    # the pantograph
    arr = c.render(ambient=0.32)
    arr = drop_shadow(arr, np.where(arr[..., 3] > 0.5, 6.0, 0.0), 0.45)
    for _ in range(5):
        add_glow(arr, cx + rng.choice([-10.5, 10.5]), rng.uniform(14, h - 14), 2.4, EMBER * 0.6)
    return finish(predawn(clear_rim(arr), 230 - w / 2), 32)


def car_wrecks(w, h, seed):
    """Abandoned aircars grounded on the boulevard: two or three of Level 08's wedge sedans and vans
    (their production headings, lights off), set down askew, one scorched, with their shadows."""
    rng = np.random.default_rng(seed)
    arr = np.zeros((h, w, 4))
    picks = [("aircar-sedan", (w * 0.3, h * 0.72)), ("aircar-van", (w * 0.66, h * 0.42)), ("aircar-sedan", (w * 0.36, h * 0.2))]
    for k, (name, (x, u)) in enumerate(picks[:2 + seed % 2]):
        heading = int(rng.integers(16))
        img = Image.open(ROOT / "assets" / "backdrop" / "level-08" / f"{name}_{heading}.png").convert("RGBA")
        a = np.array(img).astype(np.float64) / 255
        solid = a[..., 3] >= 1.0
        a[~solid] = 0                                                              # no lamp halos: parked, dark
        lum = a[..., :3] @ np.array([0.3, 0.59, 0.11])
        hot = solid & (lum > 0.6)
        a[hot, :3] = NAVY[1] / 255 * 1.4
        if k == 1:
            a[solid, :3] = a[solid, :3] * 0.45 + SCORCH / 255 * 0.55
        sh = np.zeros_like(a)
        sh[3:, 2:] = a[:-3, :-2]
        sh_mask = (sh[..., 3] > 0) & ~solid
        a[sh_mask, :3] = 0
        a[sh_mask, 3] = 0.45
        paste_rgba(arr, a, x, u)
    return finish(predawn(clear_rim(arr), None), 32)


def paste_rgba(arr, a, x, u):
    """Alpha-composite a float RGBA image (stepped alpha) centred at (x, u) on a native float RGBA image."""
    h, w = arr.shape[:2]
    sh, sw = a.shape[:2]
    left, top = int(round(x - sw / 2)), int(round(h - u - sh / 2))
    for j in range(sh):
        r = top + j
        if not 0 <= r < h:
            continue
        for i in range(sw):
            col = left + i
            if 0 <= col < w and a[j, i, 3] > 0:
                al = a[j, i, 3]
                arr[r, col, :3] = a[j, i, :3] * al + arr[r, col, :3] * (1 - al) if arr[r, col, 3] > 0 else a[j, i, :3]
                arr[r, col, 3] = max(arr[r, col, 3], al)


def ferry_wreck(w, h):
    """A capsized ferry in the lagoon: the upturned hull, rust red-brown with a keel and two
    propeller shafts, awash at its edges (stepped translucency), an oil sheen beside it."""
    c = Canvas(w, h, f=2, alpha=0.0)
    cx = w / 2

    def hull(xx, uu):
        bow = h - 10
        half = 26 * np.sqrt(np.clip((bow - uu) / 70, 0, 1)) * np.where(uu > bow - 70, 1, 0) + np.where(uu <= bow - 70, 26, 0)
        return (np.abs(xx - cx) <= half) & (uu >= 10) & (uu <= bow)
    rust = MARS[1] * 0.45 + JOV[1] * 0.3 + NAVY[1] * 0.5
    c.paint((0, 0, w, h), mask_fn=hull, rgb=rust, height=3.0, alpha=1.0)
    c.paint((0, 0, w, h), mask_fn=lambda xx, uu: hull(xx, uu) & (np.abs(xx - cx) < 17), rgb=rust * 1.15, height=4.4)
    c.rect(cx - 1.2, 16, cx + 1.2, h - 22, rgb=rust * 0.7, height=5.4)                 # the keel
    for sx in (-8, 8):
        c.line(cx + sx, 12, cx + sx, 30, 1.2, rgb=CONCRETE * 1.1, height=5.0)
        c.disc(cx + sx, 14, 2.6, rgb=JOV[2] * 0.6, height=5.2)
    c.grain(1003, 0.3, cells=(4, 2))
    arr = c.render(ambient=0.3)
    edge = (arr[..., 3] > 0.5) & (k8.box_blur((arr[..., 3] > 0.5).astype(float), 2) < 0.9)
    arr[edge, 3] = 0.55
    rng = np.random.default_rng(1005)
    for _ in range(6):                                                               # the oil sheen
        ox, ou = rng.uniform(w * 0.6, w - 8), rng.uniform(20, h - 20)
        add_glow(arr, ox, ou, 5.0, CHITIN[2] * 0.12)
    return finish(predawn(clear_rim(arr), 392 - w / 2), 32, alpha_levels=4, solid=0.6)


def heap(w, h):
    """The rubble heap the collapse leaves (round 31's look c): tools/art/collapse_l09.py's image,
    imported here only when it is drawn (that module imports this one through the concept script)."""
    import collapse_l09
    return collapse_l09.heap_image(w, h)


TRUCK_SIDE = 44   # the Kilo truck's frame, px
HEADINGS = 16     # its headings (0 nose up, clockwise)


def truck_frame(k, n=HEADINGS):
    """Heading ``k`` of the Kilo truck as l08_traffic renders its craft: the model turned under the
    fixed key light, 8x, muted as scenery, the head and tail lamps' halos baked in, and (a ground
    vehicle) its short shadow down and right."""
    side, f = TRUCK_SIDE, 8
    deg = k * 360 / n
    hi = sdf.render(p30.rotated(p31.kilo_truck(), deg), p31.truck_materials(), (side * f, side * f), float(side),
                    z_top=float(side), steps=160)
    arr = np.array(artkit.native(hi, f, crisp=60)).astype(np.float64)
    arr = p30.mute(arr, k=0.86, sat=0.8)
    a = np.radians(deg)
    glow = np.zeros((side, side, 3))
    for x, y, kind in p31.TRUCK_LIGHTS:
        sx = x * np.cos(a) + y * np.sin(a)
        sy = -x * np.sin(a) + y * np.cos(a)
        r = 3
        p30.add(glow, p30.p25.halo(r, p30.LIGHT_COL[kind], 0.55), side / 2 + sx - r, side / 2 - sy - r)
    arr = l08_traffic.bake(arr, glow)
    body = arr[..., 3] >= 255
    sh = np.zeros_like(body)
    sh[3:, 2:] = body[:-3, :-2]
    put = sh & (arr[..., 3] == 0)
    arr[put, :3] = 0
    arr[put, 3] = 255 * 0.42
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")


def truck_frames(w, h, n):
    imgs = [truck_frame(k, n) for k in range(n)]
    arrs = [np.array(i).astype(np.float64) / 255 for i in imgs]
    return l08_traffic.quantize([to_img(predawn(a, None)) for a in arrs])


COCOON_COLOURS = 32
COCOON_BREAK, COCOON_CRUMBLE, COCOON_CANVAS = 8, 3, 64


def cocoon_frame(s):
    """State ``s`` (0 intact, 1 hit, 2 opened) as Level 04's dugout: the model at 8x, the light
    rim on the standing frames, scorch on the hit and opened ones."""
    w, h = p31.COCOON_SIZE
    hi, factor = artkit.render_hi(p31.cocoon(s), p31.cocoon_materials(s), (w, h), float(w), z_top=40.0, steps=160)
    arr = np.array(artkit.native(hi, factor, crisp=60)).astype(np.float64)
    if s < 2:
        arr = gt.light_rim(arr)
    if s >= 1:
        rng = np.random.default_rng(31 + s)
        arr = gt.scorch(arr, [(rng.uniform(0.3, 0.8) * w, rng.uniform(0.3, 0.8) * h, rng.uniform(3, 6)) for _ in range(2 * s)], 9 + s)
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")


def cocoon_pieces(seed=9):
    rng = np.random.default_rng(seed)
    w, h = p31.COCOON_SIZE
    cw, ch = w / 3, h / 2
    out = []
    for cy in range(2):
        for cx in range(3):
            centre = np.array([(cx + 0.5) * cw - w / 2, h / 2 - (cy + 0.5) * ch, 4.0])
            direction = centre[:2] / max(np.hypot(*centre[:2]), 1) + rng.uniform(-0.25, 0.25, 2)
            out.append((centre, (cw / 2, ch / 2), direction * rng.uniform(1.8, 2.4), rng.uniform(-0.25, 0.25),
                        rng.uniform(-0.3, 0.3)))
    return out


def cocoon_break_frame(f):
    """Break-apart frame ``f``: the hit cocoon in six pieces flying apart, tumbling and charring, then
    crumbling away (Level 04's dugout method, tools/art/l04_targets.py)."""
    base, base_mats = p31.cocoon(1), p31.cocoon_materials(1)
    char = 1 - 0.07 * f
    transforms = []
    for centre, half, velocity, spin, tumble in cocoon_pieces():
        offset = np.array([velocity[0] * (f + 1), velocity[1] * (f + 1), -0.4 * f])

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
            d = np.maximum(d, sd_box(q, (centre[0], centre[1], 0), (half[0], half[1], 20.0)))
            m = m + i * count
            if best_d is None:
                best_d, best_m = d, m
            else:
                closer = d < best_d
                best_d, best_m = np.where(closer, d, best_d), np.where(closer, m, best_m)
        return best_d, best_m
    e = COCOON_CANVAS
    hi, factor = artkit.render_hi(scene, mats, (e, e), float(e), z_top=40.0, steps=150)
    return crumbled(artkit.native(hi, factor, crisp=60), f - (COCOON_BREAK - COCOON_CRUMBLE) + 1)


def _cocoon_job(job):
    kind, i = job
    return cocoon_frame(i) if kind == "frame" else cocoon_break_frame(i)


def build_props():
    """The cocoon's sprites (assets/sprites: cocoon_0..2, cocoon-break_0..7, one palette)."""
    jobs_ = [("frame", s) for s in range(3)] + [("break", f) for f in range(COCOON_BREAK)]
    with ProcessPoolExecutor() as pool:
        images = list(pool.map(_cocoon_job, jobs_))
    done = artkit.quantize_set(images, COCOON_COLOURS)
    artkit.write_frames("cocoon", done[:3], SOURCE)
    artkit.write_frames("cocoon-break", done[3:], SOURCE)
    print(f"cocoon: 3 frames {done[0].size}, break-apart {COCOON_BREAK} frames {done[3].size}, "
          f"{artkit.colour_count(done)} colours")


# --------------------------------------------------------------------------- towers

FOOTPRINTS = [(40, 44), (56, 56), (68, 64), (84, 72)]
STYLES = {  # style -> (heights, shade)
    "a": ((0.35, 0.6, 0.85, 1.1, 1.35), None),
    "b": ((0.35, 0.6, 0.85, 1.1, 1.35), None),
    "creep": ((0.85, 1.1, 1.35), None),
    "burnt": ((0.6, 0.85), 0.45),
}
WALLS = {  # id -> (width, storeys, storey px, style)
    "wall-low": (32, 3, 6, "city"), "wall-mid": (32, 7, 6, "city"), "wall-tall": (32, 12, 6, "city"),
    "wall-creep-mid": (32, 7, 6, "creep"), "wall-creep-tall": (32, 12, 6, "creep"),
    "wall-burnt": (32, 7, 6, "burnt"), "wall-arcology": (40, 22, 5, "arcology"),
    "wall-arcology-hollow": (40, 22, 5, "hollow"),
}


def wall_for(style, h):
    if style == "burnt":
        return "wall-burnt"
    if style == "creep":
        return "wall-creep-mid" if h < 1.0 else "wall-creep-tall"
    return "wall-low" if h < 0.5 else "wall-mid" if h < 1.0 else "wall-tall"


def tower_id(foot, h, style):
    return f"tower-{foot[0]}x{foot[1]}-h{round(h * 100):03d}-{style}"


def creep_overlay(arr, seed, reach, rows_from_top=None):
    """Violet chitin creep over an image (native float RGBA, rows from the top): over the top-left
    part of a roof (``reach`` of its diagonal), or over the top ``rows_from_top`` rows of a wall
    texture (its upper floors), thinning downward; glowing pods here and there."""
    h, w = arr.shape[:2]
    nz = raster.fbm(w, h, 6, seed, octaves=3, period=False)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64)
    if rows_from_top is None:
        share = np.clip(1 - (xx / w + yy / h) / (2 * reach), 0, 1)
    else:
        share = np.clip(1 - yy / rows_from_top, 0, 1)
    sel = (nz * 0.9 + share * 0.9) > 1.02
    sel &= arr[..., 3] > 0.5
    tone = CHITIN[1] * 0.5 + NAVY[1] * 0.7
    arr[sel, :3] = (tone * (0.8 + 0.4 * nz[sel, None])) / 255
    rng = np.random.default_rng(seed + 1)
    cells = np.argwhere(sel)
    for r, c_ in cells[rng.choice(len(cells), size=min(len(cells), max(2, len(cells) // 90)), replace=False)] if len(cells) else []:
        arr[r, c_, :3] = VGLOW[1] / 255 * 0.85 if rng.random() < 0.6 else VGLOW[4] / 255 * 0.7
    return arr


def tower_roof(foot, h, style):
    if style == "creep":
        img = k8.tower_roof(foot, h, "a" if (foot[0] + int(h * 100)) % 2 else "b")
        arr = np.array(img.convert("RGBA")).astype(np.float64) / 255
        arr = creep_overlay(arr, foot[0] * 7 + int(h * 100), 0.75)
        return quantize_lit([to_img(predawn(arr, None))], 28)[0]
    return graded(k8.tower_roof(foot, h, style), None, 28)


def wall_texture(name):
    w, storeys, px, style = WALLS[name]
    if style in ("creep", "hollow"):
        base = k8.wall_texture(w, storeys, px, "city" if style == "creep" else "arcology")
        arr = np.array(base.convert("RGBA")).astype(np.float64) / 255
        if style == "hollow":                                         # gutted: most windows dark
            rng = np.random.default_rng(1013)
            lum = arr[..., :3] @ np.array([0.3, 0.59, 0.11])
            lit_ = (lum > 0.35) & (rng.random(lum.shape) < 0.75)
            arr[lit_, :3] = NAVY[0] / 255 * 1.2
        arr = creep_overlay(arr, storeys * 31 + w, 0, rows_from_top=int(storeys * px * (0.45 if style == "creep" else 0.7)))
        return artkit.quantize_set([to_img(predawn(arr, None))], 24)[0]
    return artkit.quantize_set([to_img(predawn(np.array(k8.wall_texture(w, storeys, px, style).convert("RGBA"))
                                               .astype(np.float64) / 255, None))], 24)[0]


def arcology_roof(w, hh, hollow=True):
    """The Ndidi Arcology's roof (h 1.5) hollowed by the creep: Level 08's terraced roof, the creep
    now over most of it, the crown ring broken, holes into the dark shaft, pods glowing."""
    img = k8.arcology_roof(w, hh)
    arr = np.array(img.convert("RGBA")).astype(np.float64) / 255
    if hollow:
        arr = creep_overlay(arr, 1015, 1.2)
        rng = np.random.default_rng(1017)
        yy, xx = np.mgrid[0:hh, 0:w].astype(np.float64)
        for _ in range(5):
            hx, hy, r = rng.uniform(w * 0.15, w * 0.85), rng.uniform(hh * 0.15, hh * 0.85), rng.uniform(8, 16)
            d = np.hypot(xx - hx, yy - hy)
            arr[d < r, :3] = NAVY[0] / 255 * 0.6
            rim = (d >= r) & (d < r + 2)
            arr[rim, :3] = CHITIN[2] / 255 * 0.7
    return quantize_lit([to_img(predawn(arr, None))], 32)[0]


def arcology_b_roof(w, hh):
    img = k8.arcology_roof(w, hh)
    arr = np.array(img.convert("RGBA")).astype(np.float64) / 255
    arr = creep_overlay(arr, 1019, 0.35)
    return quantize_lit([to_img(predawn(arr, None))], 32)[0]


# --------------------------------------------------------------------------- the layout

TILE_SETS = {   # id -> (layer, height, drift, note)
    "pre-dawn": ("deep", 480, None, "the sky before dawn mirrored, grey in the east: seen only in the lagoon"),
    "approach": ("ground", TILE_H, None, "1: the ruined elevated highway (x 170-310), burnt-out traffic, blocks either side"),
    "plaza-district": ("ground", TILE_H, None, "2: avenues (x 112, 252), office roofs, parks"),
    "boulevard": ("ground", TILE_H, None, "3: the boulevard (x 170-290) with tram tracks, abandoned cars"),
    "lagoon": ("ground", TILE_H, None, "4: the lagoon (translucent), the west bank's quay, the Okonjo Bridge (x 220-300)"),
    "lobby-district": ("ground", TILE_H, None, "5: arcology podiums round the avenue (x 196-284), creep"),
    "dust": ("low-air", 960, None, "light: street dust, thin, lit grey from the east"),
    "smoke": ("low-air", 960, None, "medium: river smoke and the towers' smoke"),
    "dust-heavy": ("low-air", 960, None, "heavy: the collapse's dust (the event-triggered peak)"),
    "mist": ("high-air", 960, None, "clear and light: thin mist (additive), grey in the east"),
    "ash": ("high-air", 960, None, "medium and heavy: ash (additive)"),
}
PIECES = {      # id -> (layer, (w, h), extra spec, mid-size, note)
    "cross-highway": ("ground", (480, 92), {}, False, "an elevated cross highway; one over every section seam"),
    "barricade": ("ground", (172, 96), {}, False, "the CDF checkpoint across the elevated highway"),
    "highway-breach": ("ground", (148, 120), {}, False, "a fallen span of the elevated highway"),
    "burnt-block-a": ("ground", (72, 82), {}, False, "a collapsed block"),
    "burnt-block-b": ("ground", (76, 82), {}, False, None),
    "burnt-block-c": ("ground", (94, 82), {}, False, None),
    "fire": ("ground", (40, 40), {"frames": 4, "fps": 8}, False, "flames on the burnt blocks (sections 1, 3, 5)"),
    "smoke-column-a": ("low-air", (150, 200), {}, False, "smoke plumes over the burning blocks (static)"),
    "smoke-column-b": ("low-air", (130, 180), {}, False, None),
    "tram": ("ground", (36, 168), {}, False, "the burnt-out tram on the boulevard's tracks"),
    "car-wrecks-a": ("ground", (44, 64), {}, False, "abandoned aircars on the boulevard"),
    "car-wrecks-b": ("ground", (44, 64), {}, False, None),
    "ferry-wreck": ("ground", (72, 168), {}, True, "a capsized ferry in the lagoon"),
    "kilo-truck": ("ground", (TRUCK_SIDE, TRUCK_SIDE), {"headings": HEADINGS}, False,
                   "the Kilo convoy's CDF truck (16 headings, 0 nose up, clockwise); scenery, shots pass through"),
    "arcology-heap": ("ground", (280, 250), {}, True,
                      "the rubble heap the collapse leaves at the arcology's foot (round 31's look c; not placed)"),
}
_NOTES = {"unity-plaza": "Unity Plaza: the plaza pair's spots, the dry fountain, nodes A1/A2's creep",
          "mortar-roof": "a boulevard mortar's low roof (one per mortar)",
          "roof-nest": "the boulevard turret nest's low roof",
          "cocoon-roof": "the cocoon's low roof, creep round its spot",
          "quay-pads": "the far bank's quay pads under the two mortars",
          "bridgehead": "the bridgehead: the far bank widened to the deck, the ramp, the pair's spots, nodes B1/B2's creep",
          "lobby-plaza": "the Ndidi lobby plaza: nodes C1/C2's creep, hard's pair's spots"}
for _id, _s in STRUCTURES.items():
    PIECES[_id] = ("ground", (_s["w"], _s["h"]), {}, _id in ("unity-plaza", "bridgehead", "lobby-plaza", "roof-nest"),
                   _NOTES.get(_id))
k8.PIECES = PIECES
NOTE = {1: "1. Arcology Approach: the ruined elevated highway, the CDF barricade", 2: "2. Unity Plaza",
        3: "3. Boulevard Run: the tram, the mortars' roofs, the nest, the cocoon's roof",
        4: "4. Okonjo Bridge: the lagoon, the ferry wreck, the quay pads, the bridgehead",
        5: "5. Arcology Fall: the Ndidi lobby plaza and the arcology"}


def fixed_placements():
    """(piece, t, x, extra, comment): everything but the towers, burnt blocks, plumes and trucks."""
    out = []
    for s in range(1, len(SECTIONS)):
        out.append(("cross-highway", round(t_for_centre("ground", seam_pos(s)), 3), 240, None,
                    f"over the seam of sections {s} and {s + 1}, where the ground tiles change" if s == 1 else None))
    out.append(("barricade", 9.0, 240, None, NOTE[1]))
    out.append(("highway-breach", 15.6, 240, None, None))
    for sid, s in STRUCTURES.items():
        for k, (x, centre) in enumerate(s["placements"]):
            sec = section_of(centre)
            out.append((sid, round(t_for_centre("ground", centre), 3), round(x), None,
                        NOTE.get(sec + 1) if sid in ("unity-plaza", "ferry-wreck") and k == 0 else None))
    out.append(("tram", 77.4, 230, None, NOTE[3]))
    out.append(("car-wrecks-a", 62.2, 200, None, None))
    out.append(("car-wrecks-b", 70.4, 262, None, None))
    out.append(("car-wrecks-b", 88.6, 200, None, None))
    out.append(("ferry-wreck", 100.6, 392, None, NOTE[4]))
    ax, apos = arcology_place()
    out.append(("arcology", round(t_for_centre("ground", apos), 3), round(ax), None,
                NOTE[5] + " (the Ndidi Arcology: a tower, h 1.5, left of the plaza; it falls in the collapse)"))
    return out


def piece_rects(placed_list):
    """Ground rects (piece, x0, p0, x1, p1) of placed flat pieces (towers by their footprint)."""
    out = []
    for piece, t, x, extra, _ in placed_list:
        w, h = ARCOLOGY["foot"] if piece == "arcology" else PIECES[piece][1]
        c = scroll_at(t) + MID
        out.append((piece, x - w / 2, c - h / 2, x + w / 2, c + h / 2))
    return out


def zones():
    """What the towers must keep clear of: the structures, every walker unit's path while it can be
    on screen, every ground target's unit, the boulevard's tram and the bridge's deck (ground rects,
    with margins)."""
    out = []
    for piece, x0, p0, x1, p1 in piece_rects([p for p in fixed_placements() if p[0] in STRUCTURES or p[0] == "tram"]):
        out.append((x0 - 6, p0 - 6, x1 + 6, p1 + 6))
    for t0, path, _ in walkers():
        walked_ = 0.0
        for (ax, ap), (bx, bp) in zip(path, path[1:]):
            seg = math.hypot(bx - ax, bp - ap)
            n = max(1, int(seg / 30))
            for k in range(n):
                if walked_ + seg * k / n > 600:
                    break
                px, pp = ax + (bx - ax) * k / n, ap + (bp - ap) * k / n
                out.append((px - 40, pp - 40, px + 40, pp + 40))
            walked_ += seg
    for _, _, _, x, pos, w, h in target_units():
        out.append((x - w / 2 - 14, pos - h / 2 - 14, x + w / 2 + 14, pos + h / 2 + 14))
    p0, p1 = seam_pos(3), seam_pos(4)
    out.append((DECK[0] - 4, p0, DECK[1] + 4, p1))
    return out


_TOWERS = None


def tower_placements():
    global _TOWERS
    if _TOWERS is None:
        _TOWERS = place_towers()
    return _TOWERS


def place_towers():
    """Towers on the city lots section by section, each as tall as its spot allows (or none): the
    approach thin and low, rising toward the plaza; the plaza's district dense and tall; the
    boulevard medium, some burnt; the west bank's slim ones; the arcology district creep-covered;
    two smaller arcologies on the edges."""
    rng = np.random.default_rng(3009)
    zl = zones()
    flats = piece_rects([p for p in fixed_placements() if p[0] not in ("arcology",)])
    ax, apos = arcology_place()
    out = []
    sec_starts = [seam_pos(s) for s in range(len(SECTIONS))]
    sec_starts[0] = 0.0
    end_pos = scroll_at(OUTRO_END) + SCREEN + 10
    for s in range(len(SECTIONS)):
        p0 = sec_starts[s] + (SCREEN if s == 0 else 0)
        p1 = sec_starts[s + 1] if s + 1 < len(SECTIONS) else end_pos
        for x0, x1, q0, q1 in k8.lots(SECTION_GROUND[s], p0 + 30, p1 - 30):
            cx, cp = (x0 + x1) / 2, (q0 + q1) / 2
            if any(not (r[3] + 4 < x0 or r[1] - 4 > x1 or r[4] + 4 < q0 or r[2] - 4 > q1) for r in flats):
                continue
            if abs(cp - apos) < ARCOLOGY["foot"][1] / 2 + 90 and x0 < ax + ARCOLOGY["foot"][0] / 2 + 40:
                continue
            progress = (cp - p0) / max(1, p1 - p0)
            r = rng.random()
            if s == 0:
                share, top, style = 0.18 + 0.4 * progress, 0.6 + 0.5 * progress, ("creep" if r < 0.25 else "a" if r < 0.65 else "b")
            elif s == 1:
                share, top, style = 0.7, 1.35, ("creep" if r < 0.3 else "a" if r < 0.65 else "b")
            elif s == 2:
                share, top, style = 0.45, 1.1, ("burnt" if r < 0.25 else "creep" if r < 0.45 else "a" if r < 0.75 else "b")
            elif s == 3:
                share, top, style = 0.5, 1.1, ("a" if r < 0.5 else "b")
            else:
                share, top, style = 0.6, 1.35, ("creep" if r < 0.55 else "burnt" if r < 0.8 else "b")
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
    for t_mid, x in ((23.6, 52), (180.0, 430)):                                       # two smaller arcologies
        cp = scroll_at(t_mid) + MID
        scrolls = np.array([scroll_at(t) for t in np.arange(t_mid - 6, t_mid + 6, 1 / 15)])
        if k8.hull_clear(x, cp, ARCOLOGY_B["foot"], ARCOLOGY_B["height"], zl, scrolls):
            out = [p for p in out if not (abs(p[2] - x) < 120 and abs(scroll_at(p[1]) + MID - cp) < 130)]
            out.append(("arcology-b", t_mid, x, None, None))
    return out


def burnt_placements():
    """Collapsed blocks on free lots of sections 1, 3 and 5, fires on some of them (none in
    section 4: the trucks are its motion)."""
    rng = np.random.default_rng(4009)
    towers = tower_placements()
    rects = piece_rects([p for p in fixed_placements()])
    tower_spots = {(round(x), round(scroll_at(t) + MID)) for _, t, x, _, _ in towers}
    out, fires = [], []
    for s, share in ((0, 0.35), (2, 0.22), (4, 0.3)):
        p0 = seam_pos(s) if s else SCREEN
        p1 = seam_pos(s + 1) if s + 1 < len(SECTIONS) else scroll_at(OUTRO_END) + SCREEN
        for x0, x1, q0, q1 in k8.lots(SECTION_GROUND[s], p0 + 40, p1 - 60):
            cx, cp = (x0 + x1) / 2, (q0 + q1) / 2
            if (round(cx), round(cp)) in tower_spots:
                continue
            if any(not (r[3] + 2 < x0 or r[1] - 2 > x1 or r[4] + 2 < q0 or r[2] - 2 > q1) for r in rects):
                continue
            if any(abs(scroll_at(t) + MID - cp) < 60 and abs(x - cx) < 40 for _, t, x, _, _ in towers):
                continue
            if rng.random() > share:
                continue
            w = x1 - x0
            if w < 70:
                continue
            pid = "burnt-block-a" if w < 75 else "burnt-block-b" if w < 85 else "burnt-block-c"
            t = round(t_for_centre("ground", cp), 3)
            out.append((pid, t, cx, None, None))
            if rng.random() < 0.4:
                fires.append(("fire", round(t + rng.uniform(-0.12, 0.12), 3), round(cx + rng.uniform(-12, 12)), None, None))
    return out, fires


def smoke_placements(fires):
    """Smoke plumes on low-air over some of the fires (static), at the side that veils the ground
    units least (Level 08's plume_overlap)."""
    rng = np.random.default_rng(5009)
    out = []
    last = -99.0
    for k, (_, t, x, _, _) in enumerate(fires):
        if t - last < 5.0:
            continue
        pid = ("smoke-column-a", "smoke-column-b")[len(out) % 2]
        jitter = rng.uniform(-14, 14)
        cands = [x + 40, x - 40] if x < 240 else [x - 40, x + 40]
        scores = {cx: k8.plume_overlap(pid, round(t + 0.6, 2), cx + jitter) for cx in cands}
        best = min(scores, key=scores.get)
        out.append((pid, round(t + 0.6, 2), round(float(best + jitter)), None,
                    "smoke plumes on low-air over the fires (static)" if not out else None))
        last = t
    return out


def truck_stream(t0, t1, every, x=TRUCK_LANE, own=40.0):
    """The Kilo convoy: trucks entering at the top edge from ``t0`` every ``every`` s until ``t1``,
    driving down the bridge's southbound lane at ``own`` px/s (heading 8). The path runs 12 s on
    the real clock, so a truck still on the bridge in a hold drives on off the bottom edge."""
    w, h = PIECES["kilo-truck"][1]
    centre = scroll_at(t0) + SCREEN + h / 2 + 1
    t_place = t_at(centre - MID)
    count = max(1, int((t1 - t0) / every) + 1)
    span = 12.0
    entry = {"path": [[round(t0, 2), 0, 0], [round(t0 + span, 2), 0, round(-own * span, 1)]]}
    if count > 1:
        entry["repeat"] = {"count": count, "every": every}
    return ("kilo-truck", round(t_place, 3), x, entry, "4. the Kilo convoy down the bridge (scenery, shots pass through)")


def placements():
    fixed = fixed_placements()
    burnt, fires = burnt_placements()
    return {"fixed": fixed, "towers": tower_placements(), "burnt": burnt, "fires": fires,
            "smoke": smoke_placements(fires), "trucks": [truck_stream(99.4, 119.0, 1.9)]}


def tower_pieces(towers):
    out = {}
    for pid, *_ in towers:
        if pid == "arcology-b":
            out[pid] = ("ground", ARCOLOGY_B["foot"], {"tower": {"height": ARCOLOGY_B["height"], "wall": "wall-arcology",
                                                                  "shade": 0.5}}, False,
                        "a smaller arcology (h 1.35) on the edge of the district")
            continue
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
    pieces["arcology"] = ("ground", ARCOLOGY["foot"], {"tower": {"height": ARCOLOGY["height"], "wall": "wall-arcology-hollow",
                                                                 "shade": 0.5}}, True,
                          "the Ndidi Arcology (a tower, h 1.5), hollowed by the creep; it falls in the collapse")
    return pieces


def ordered_placements(pl):
    """Draw order on a layer: the flat structures and set dressing first, then the burnt blocks and
    fires, the trucks over the deck, the towers (drawn by height by the game anyway); low-air: the
    plumes."""
    return pl["fixed"] + pl["burnt"] + pl["fires"] + pl["trucks"] + pl["towers"] + pl["smoke"]


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
    lines = [
        "# Level 09 backdrop PROPOSAL (tools/art/backdrop_l09.py --proposal; M5 part C batch, round 31).",
        "# For the level-data step to merge into data.yaml: `backdrop` is the level's block, and the",
        "# sections take the `tiles` below. Placements are SCRIPT time (scroll distance / 150 px/s): a hold",
        "# slows the level clock with the scroll, so they wait with the ground; the trucks' `path` runs on",
        "# the real clock from its first waypoint (D2 = a).",
        "#",
        "# sections[i].tiles (by section, in order):",
    ]
    for s, tiles in zip(SECTIONS, SECTION_TILES):
        lines.append(f"#   {s['name']}: [{', '.join(tiles)}]")
    lines += [
        "#",
        "# The ground targets this block was built round (t: entry at the top edge, x; the README's",
        "# estimates; re-run --proposal and the images after moving a target):",
    ]
    for g in DATA["ground_targets"]:
        spots = g.get("at") or g.get("hard", {}).get("at")
        what = g.get("enemy") or g.get("sprite")
        extra = " (hard only)" if "at" not in g else ""
        lines.append(f"#   {g['target']} ({what}, section {g.get('section')}){extra}: at {spots}")
    ax, apos = arcology_place()
    lines += [
        "#",
        "# The collapse (D5 = a): the arcology placement below is the tower that falls; the `collapse` block's",
        f"#   `rubble: arcology-heap` (a piece, not placed: the collapse leaves it where the tower stood), and",
        f"#   `band` round the arcology's footprint (200 x 180 at x {ax:g}, its centre at ground position {apos:.0f},",
        "#   so about 90 px above and below its centre's screen y when the fall starts). The look is round 31's",
        "#   c (concept/collapse-r31-c), drawn by the game; the heap is tools/art/collapse_l09.py's.",
        "# Kilo trucks: scenery on the ground layer (shots pass through), one piece so the motion budget",
        "#   holds (trucks 1 + nothing else in section 4; fires only in sections 1, 3 and 5; no bank drifts).",
        "# Walkers: every Creeper and Ravager path of the data keeps clear of the towers once --proposal is",
        "#   re-run with the paths in data.yaml (the towers are placed against them, as Level 08's).",
        "",
        "backdrop:",
        "  # Presentation only: the Nova Lagos arcology district in the last hour of the night (tools/art/",
        "  # backdrop_l09.py, Level 08's production kit re-dressed; D9 = a). Towers are scenery in true",
        "  # perspective; none leans over a ground target, its structure or a walker's path while on screen.",
        "  # Motion budget: the fires (1) and the Kilo trucks (1), never together; the banks do not drift.",
        "  scroll_factors: " + flow(b["scroll_factors"]),
        f"  ramp: {RAMP}",
        f"  haze_colour: {HAZE_COLOUR}  # cool grey-violet: the haze before dawn",
        "  atmosphere:",
    ]
    notes = {"clear": "section 2: thin mist only", "light": "sections 1 and 3: street dust",
             "medium": "sections 4-5: river smoke, the towers' smoke, ash",
             "heavy": "the collapse's dust (the event-triggered peak, ramping out over 6 s)"}
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
    "pre-dawn": pre_dawn, "approach": lambda w, h: approach_tile(), "plaza-district": lambda w, h: plaza_district_tile(),
    "boulevard": lambda w, h: boulevard_tile(), "lagoon": lambda w, h: lagoon_tile(),
    "lobby-district": lambda w, h: lobby_district_tile(),
    "dust": dust, "smoke": smoke, "dust-heavy": dust_heavy, "mist": mist, "ash": ash,
    "cross-highway": lambda w, h: graded(k8.cross_highway(w, h), 0.0), "barricade": barricade,
    "highway-breach": highway_breach,
    "burnt-block-a": lambda w, h: graded(k8.burnt_block(w, h, 881), None, 24),
    "burnt-block-b": lambda w, h: graded(k8.burnt_block(w, h, 883), None, 24),
    "burnt-block-c": lambda w, h: graded(k8.burnt_block(w, h, 887), None, 24),
    "fire": lambda w, h, n: k8.fire(w, h, n),
    "smoke-column-a": lambda w, h: graded(k8.smoke_column(w, h, 1021), None, 16),
    "smoke-column-b": lambda w, h: graded(k8.smoke_column(w, h, 1023), None, 16),
    "tram": tram, "car-wrecks-a": lambda w, h: car_wrecks(w, h, 1025), "car-wrecks-b": lambda w, h: car_wrecks(w, h, 1026),
    "ferry-wreck": ferry_wreck, "kilo-truck": truck_frames, "arcology-heap": lambda w, h: heap(w, h),
    "unity-plaza": unity_plaza, "mortar-roof": mortar_roof, "roof-nest": roof_nest, "cocoon-roof": cocoon_roof,
    "quay-pads": quay_pads, "bridgehead": bridgehead, "lobby-plaza": lobby_plaza,
    "arcology": lambda w, h: arcology_roof(w, h), "arcology-b": arcology_b_roof,
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
    """No tower's walls or roof come over a ground target's unit or a walker at any step."""
    problems = []
    towers = [p for p in placed_all if "tower" in backdrop["pieces"][p["piece"]]]
    units = [(u[0], u[3], u[4], u[5], u[6]) for u in target_units()]
    walks = walkers()
    for step in range(0, int(OUTRO_END * 30)):
        t = step / 30
        scroll = scroll_at(t)
        spots = [(name, x, pos - scroll, max(w, 30), max(h, 30)) for name, x, pos, w, h in units]
        for t0, g, speed_w in walks:
            if t >= t0:
                px, pp = k8.walked(g, (t - t0) * speed_w)[-1]
                spots.append((f"walker t={t0:g}", px, pp - scroll, 44, 44))
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


# --------------------------------------------------------------------------- review

TIMES = [(2, "1 THE LAUNCH OVER THE APPROACH"), (9, "1 THE CDF BARRICADE"), (15.6, "1 THE FALLEN SPAN"),
         (23.6, "1-2 CROSS HIGHWAY, ARCOLOGY B"), (30, "2 THE PLAZA DISTRICT"), (38.6, "2 UNITY PLAZA, NODES A"),
         (50, "2 TOWERS ROUND THE PLAZA"), (62.2, "3 THE BOULEVARD, CAR WRECKS"), (69, "3 THE MORTARS' ROOFS"),
         (77.4, "3 THE BURNT-OUT TRAM"), (81, "3 THE ROOF NEST"), (87, "3 THE COCOON'S ROOF"),
         (97.5, "3-4 ONTO THE LAGOON"), (104, "4 THE KILO CONVOY ON THE BRIDGE"), (109.4, "4 THE QUAY PADS"),
         (120.6, "4 THE BRIDGEHEAD, NODES B"), (132, "4 OVER THE LAGOON"), (150, "5 THE ARCOLOGY DISTRICT"),
         (165.2, "5 THE NDIDI LOBBY PLAZA, NODES C"), (182, "5 THE END, ARCOLOGY B")]
GIF = (102.0, 106.0, 0.08)        # s: the convoy down the bridge, at 12.5 fps


def unit_sprite(kind):
    names = {"hive-node": "hive-node_0", "spine-turret": "spine-turret_0", "polyp-mortar": "polyp-mortar_0",
             "cocoon": "cocoon_0"}
    path = SPRITES / f"{names.get(kind, kind)}.png"
    return np.array(Image.open(path).convert("RGBA")).astype(np.float64) if path.exists() else None


def with_units(frame, t):
    """The ground targets' production sprites at their places (the nodes on their creep patch), so the
    review shows how the structures carry them."""
    out = np.array(frame.convert("RGBA")).astype(np.float64)
    scroll = scroll_at(t)
    creep = SPRITES / "hive-node-creep_0.png"
    creep_img = np.array(Image.open(creep).convert("RGBA")).astype(np.float64) if creep.exists() else None
    for _, _, kind, x, pos, w, h in target_units():
        y = SCREEN - (pos - scroll)
        if not -100 < y < SCREEN + 100:
            continue
        if kind == "hive-node" and creep_img is not None:
            k8.blit(out, creep_img, int(round(x - creep_img.shape[1] / 2)), int(round(pos - scroll - creep_img.shape[0] / 2)))
        img = unit_sprite(kind)
        if img is not None:
            k8.blit(out, img, int(round(x - img.shape[1] / 2)), int(round(pos - scroll - img.shape[0] / 2)))
    return Image.fromarray(np.clip(out[..., :3], 0, 255).astype(np.uint8), "RGB")


def review(backdrop):
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
        for k, im in enumerate(imgs[:4:2] if spec.get("headings") else imgs[:1]):
            scale = min(1.0, 200 / max(im.size)) if spec.get("layer") != "ground" or "tower" in spec or max(im.size) > 200 else 1.0
            if spec.get("headings") or name == "fire":
                scale = 2.0
            im = im.resize((max(1, int(im.width * scale)), max(1, int(im.height * scale))), Image.NEAREST)
            items.append((label + (f" [{k * 2}]" if count else ""), im, spec.get("layer") == "high-air"))
    for wall in walls:
        im = k8.load(wall)
        items.append((f"{wall} {im.width}x{im.height}", im.resize((im.width * 2, im.height * 2), Image.NEAREST), False))
    cocoon = artkit.load_frames("cocoon") + artkit.load_frames("cocoon-break")[::2]
    for k, im in enumerate(cocoon):
        label = ["cocoon_0 intact", "cocoon_1 hit", "cocoon_2 opened"][k] if k < 3 else f"cocoon-break_{(k - 3) * 2}"
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
    sheet = raster.sheet(width, comp_y + n_rows * (270 + 30) + 20, "LEVEL 09 BACKDROP - FINAL",
                         "PRODUCTION ART, M5 PART C BATCH - R31; THE LAST HOUR OF THE NIGHT; COMPOSITES AS THE GAME DRAWS "
                         "THEM (TOWERS PROJECTED, THE TARGETS' SPRITES ON THEIR STRUCTURES), 1/2 SCALE")
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
        shot = with_units(k8.composite(backdrop, t, placed_all), t)
        sheet.alpha_composite(shot.convert("RGBA").resize((240, 270), Image.BOX), (px, py + 14))
        raster.draw_text(sheet, px, py, f"T={t:g} {label}"[:40], raster.LABEL)
    REVIEW.parent.mkdir(parents=True, exist_ok=True)
    sheet.convert("RGB").save(REVIEW, optimize=True)
    t0, t1, dt = GIF
    frames = [with_units(k8.composite(backdrop, float(t), placed_all), float(t)) for t in np.arange(t0, t1, dt)]
    gif = REVIEW.with_suffix(".gif")
    artkit.write_gif(frames, gif, fps=round(1 / dt, 1))
    print(f"review: {REVIEW.relative_to(ROOT)}, {gif.relative_to(ROOT)} {gif.stat().st_size / 1e6:.1f} MB")


def strip_frames(backdrop, times, path, units=False):
    placed_all = k8.expanded(backdrop)
    shots = [k8.composite(backdrop, t, placed_all) for t in times]
    if units:
        shots = [with_units(s, t) for s, t in zip(shots, times)]
    out = Image.new("RGB", (len(shots) * (W + 8), SCREEN), (0, 0, 0))
    for i, s in enumerate(shots):
        out.paste(s, (i * (W + 8), 0))
    out.save(path)
    print(f"strip: {path}")


def main(argv):
    if "--proposal" in argv:
        write_proposal()
        return
    if "--props" in argv:
        build_props()
        return
    backdrop, source = level_backdrop()
    if source == "data.yaml":
        k8.SECTION_OF = [s["tiles"] for s in SECTIONS]
    print(f"backdrop from {source}")
    if "--strip" in argv:
        k = argv.index("--strip")
        strip_frames(backdrop, [float(a) for a in argv[k + 1].split(",")], argv[k + 2], "--units" in argv)
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
