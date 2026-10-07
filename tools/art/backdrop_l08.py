#!/usr/bin/env python3
"""Production art: the Level 08 backdrop, Nova Lagos at night (design/campaign/act-2-homefront/
level-08-neon-skyline; M5 part B batch, concept round 30).

Straight to production (user decision D6 = a of M5 part B): the chosen megacity scene, parallax
r03 B (tools/concept/parallax_r03.py, imported unchanged: its fog banks and tree canopies), extended
in the same kit to the harbour, the elevated highways, the smoke district and Neon Heights. Palette
B (the UI PANELS navy ramp for the city, sodium lamps from the MARS ramp, windows in JOVIAN and
EARTH ORBIT, trees in EARTH SURFACE at night), the top-left key light, posterized to 16-32 colours
per image (one palette per frame or heading set), translucency stepped, wide gradients
ordered-dithered. Flat structures are lit as height fields (relief(): normals, the key light's
cast shadows and occlusion, rendered at 2-4x and box-reduced); nothing lit is rotated or mirrored
as an image.

**Towers** (user decision D1 = a): the tower district is built from tower pieces
(design/tech/architecture, tower pieces): a roof pre-rendered at its drawn size (the footprint x
k = 6 / (6 - h), rounded as the game rounds) and a wall texture per style and height class (its
rows from the foot, the bottom row, up to the roof's edge). The game projects them round the play
field's (240, 297); this script draws them the same way for its composites. Towers stand only on
the tiles' city lots, and a tower is placed only where its walls and roof, projected at every
moment it is on screen, keep clear of everything the game plays with: the Creepers' paths (each
convoy unit's own, 1.5 s apart), the ground targets and the low structures they stand on. Where a
tall tower would lean over one, a lower one is tried, then none.

**Gameplay structures** (D1 = a: drawn flat, without lean) are sized and placed from the level's
data: the landing pad, the roof nest, the billboard's low roof, the parking deck and the collapsed
overpass round the ground targets' units (`ground_targets`, every difficulty), and one walk roof
per Creeper unit (a low roof with a ramp down to the street, from the wave's path; a convoy's
units walk the same path 1.5 s apart, so its walk roof repeats every 1.5 s). Every turret stands
on a pale painted spot with a ring and four small lamps (turret_spots), the landing pad is pale
concrete, so the dark Spine Turrets read as silhouettes.

**Smoke over the ground units** (readability, round 30's capture): the smoke banks part where the
Creepers walk, their husks lie and the ground targets stand, at every moment the bank is drawn,
mapped into the bank tile's own px (bank_parting: the low-air scroll and the drift); the heavy
wall parts fully over the highway convoy, the medium smoke thins; the smoke plumes take the column
that veils the ground units least (plume_overlap).

Outputs (assets/backdrop/level-08/, one PNG per tile set and piece of the backdrop block, named by
its id and checked against its size; headings and frames as <id>_<n>.png; wall textures by id):
  gulf                       deep (every section): the night sky's glow on the Gulf, seen in the
                             harbour's water (the only opening in the ground)
  harbour                    ground, section 1: water, piers, the quay, container yards
  avenues                    ground, section 2: two tree-lined avenues (the traffic lanes), low
                             office roofs, parks, sodium lamps
  rooftops                   ground, sections 3-4: a central boulevard, side streets, blocks of low
                             roofs with rooftop gardens and water tanks
  third-mainland             ground, section 5: the elevated Third Mainland highway, the arcology
                             district's neon-lit low roofs
  fog, smoke, smoke-heavy    low-air banks: amber-lit fog (light), smoke (medium; drifts), the
                             smoke wall (heavy; drifts)
  mist, ash                  high-air wisps (additive): thin mist, ash
  cross-highway              ground: an elevated cross highway over each section seam (and two
                             more in section 2)
  quay-crane, container-ship, container-yard   ground: the harbour
  landing-pad, roof-nest, billboard-roof, parking-deck, overpass   ground: the targets' structures
  walk-roof-<n>              ground: the Creepers' low roofs and ramps, one per walker wave
  burnt-block-a/b/c, fire_0..3   ground, smoke district: collapsed blocks, flames (animated)
  neon-sign-a/b/c            ground, Neon Heights: rooftop signs (static)
  tower-<w>x<h>-h<NNN>-<style>   ground towers: roofs (city a/b, burnt, neon)
  arcology                   ground tower: the Ndidi Arcology at the level's end, creep on its roof
  wall-low/-mid/-tall, wall-burnt, wall-neon-mid/-tall, wall-arcology   wall textures
  smoke-column-a/b/c         low-air: smoke plumes (static)
  aircar-sedan_0..15, aircar-van_0..15, gunship_0..15   low-air traffic (D2 = a: scenery), round 30
                             variant A "wedge cars" (user choice): the wedge sedan, the box van and
                             the CDF gunship at 16 headings (l08_traffic.py, from the concept models
                             of tools/concept/props_r30.py, imported unchanged)
  design/campaign/.../level-08-neon-skyline/concept/backdrop-final-r30-a.png   review sheet
  design/campaign/.../level-08-neon-skyline/concept/traffic-final-r30-a.png/.gif   the traffic's
                             review: its heading sets, composites of its streams as the game draws
                             them, and the motion (--review, or --traffic-review alone)

**Traffic** (sections 1-3): one piece per craft, so one paint per piece (the sedan slate, the van
beige); every moving piece counts once against the motion budget (two at a time), so the van lane
carries sedans while the CDF gunship pair crosses section 2, and section 3's thinning stream
alternates sedans and vans. Each stream is a placement with a path (its heading picks the frame:
8 south, 0 north) and a repeat; the gunships come in straight, bearing south-south-west, or turning
from south to south-west (headings 8, 9, 10).

**Data.** The backdrop block comes from the level's data.yaml once its block names no other
level's images, otherwise from backdrop-proposal.yaml next to it, which --proposal writes (the
block, the sections' `tiles`). Seams: every tile set repeats without a line (--check scores the
wrap rows, and the drifting banks' columns, as BackdropSeamsTest does); a section's ground tile set
changes under a cross highway.

Run: python3 tools/art/backdrop_l08.py [--review | --traffic-review | --check | --proposal | --strip t,.. out.png [--units]] [id ...]
"""
import math
import sys
from concurrent.futures import ProcessPoolExecutor
from pathlib import Path

import numpy as np
import yaml
from PIL import Image, ImageDraw, ImageFilter

import artkit
import l08_traffic
from artkit import DESIGN, ROOT, raster

from parallax_r03 import bank, foliage_sprite, noise, step_alpha  # noqa: E402  (concept script, unchanged)
from render.palette import B  # noqa: E402

SCRIPT = "backdrop_l08.py"
SOURCE = artkit.source_note(SCRIPT, "M5 part B batch")
LEVEL_DIR = DESIGN / "campaign" / "act-2-homefront" / "level-08-neon-skyline"
PROPOSAL = LEVEL_DIR / "backdrop-proposal.yaml"
OUT = ROOT / "assets" / "backdrop" / "level-08"
REVIEW = LEVEL_DIR / "concept" / "backdrop-final-r30-a.png"
TRAFFIC_REVIEW = LEVEL_DIR / "concept" / "traffic-final-r30-a.png"
W, SCREEN = 480, 540
MID = SCREEN / 2

FACTORS = {"deep": 0.12, "far": 0.45, "ground": 1.0, "low-air": 1.35, "high-air": 2.0}
HAZE_COLOUR = "2c2434"            # smoky violet-brown over the Gulf's glow
ATMOSPHERE = {
    "clear": {"wisps": "mist", "haze": 0.0},
    "light": {"banks": "fog", "wisps": "mist", "haze": 0.06},
    "medium": {"banks": "smoke", "wisps": "ash", "haze": 0.14},
    "heavy": {"banks": "smoke-heavy", "wisps": "ash", "haze": 0.26},
}
RAMP = 4
CAMERA = 6.0
CX, CY = 240.0, SCREEN - 297.0   # the projection centre, y up from the play field's bottom
CONVOY = 1.5                     # s between a walker wave's units (Formations.CONVOY_INTERVAL_SECONDS)

# --------------------------------------------------------------------------- palette (B)


def ramp_of(name):
    return [np.array(c, float) for c in B[name]]


NAVY = ramp_of("UI PANELS")       # 06061a 101438 1c2460 283890 4058c8 80a0ff
GREEN = ramp_of("EARTH SURFACE")
MARS = ramp_of("MARS")
JOV = ramp_of("JOVIAN")
ORBIT = ramp_of("EARTH ORBIT")
CHITIN = ramp_of("VRELL CHITIN")
VGLOW = ramp_of("VRELL GLOW")
ACCENT = ramp_of("UTC ACCENTS")
HULL = ramp_of("UTC HULL")
SODIUM = MARS[4] * 0.55           # the concept's lamp pools
LAMP = np.array([255, 214, 150], float)
WARM_WIN = JOV[4] * 0.8
COOL_WIN = ORBIT[5] * 0.8
NEON = [ACCENT[3] * 0.85, ACCENT[5] * 0.8, np.array([255, 236, 210], float) * 0.8, ACCENT[4] * 0.8]
ROAD = NAVY[1]
GROUND = NAVY[0]
CONCRETE = NAVY[1] * 0.55 + np.array([40, 40, 52], float) * 0.45
SCORCH = np.array([22, 16, 20], float)
EMBER = MARS[4]

# --------------------------------------------------------------------------- the level's timeline

DATA = yaml.safe_load((LEVEL_DIR / "data.yaml").read_text(encoding="utf-8"))
SECTIONS = DATA["sections"]
SPEED = float(DATA["scroll_speed"])
STARTS = [0.0] + [float(s["end"]) for s in SECTIONS[:-1]]
END = float(SECTIONS[-1]["end"])
OUTRO_END = END + 15                   # LevelData.OUTRO_SECONDS


def speed(i):
    return float(SECTIONS[i].get("speed", SPEED))


def scroll_at(t):
    """LevelData.scrollAt: the ground's distance at t."""
    scroll = 0.0
    for i, s in enumerate(SECTIONS):
        if t < s["end"] or i == len(SECTIONS) - 1:
            return scroll + (t - STARTS[i]) * speed(i)
        scroll += (s["end"] - STARTS[i]) * speed(i)
    raise AssertionError


def t_at(scroll):
    if scroll < 0:
        return scroll / speed(0)
    for i, s in enumerate(SECTIONS):
        span = (s["end"] - STARTS[i]) * speed(i)
        if scroll < span or i == len(SECTIONS) - 1:
            return STARTS[i] + scroll / speed(i)
        scroll -= span
    raise AssertionError


def t_for_centre(layer, centre):
    """The placement time that puts a piece's centre at ``centre`` px on its layer."""
    return t_at((centre - MID) / FACTORS[layer])


def seam_pos(s, layer="ground"):
    """Where section ``s``'s tile set begins on ``layer`` (it enters at the top edge at the section's start)."""
    return scroll_at(STARTS[s]) * FACTORS[layer] + SCREEN


def section_of(pos):
    """The section whose ground tile set lies at ground position ``pos``."""
    s = 0
    for i in range(1, len(SECTIONS)):
        if pos >= seam_pos(i):
            s = i
    return s


def java_round(v):
    return math.floor(v + 0.5)


def tower_scale(h):
    return CAMERA / (CAMERA - h)


# --------------------------------------------------------------------------- the city plans
# Tile coordinates: x from the left, u up from the tile's bottom row (layer position mod height).
# Cross streets every PERIOD px (STREET wide); the blocks between them are lots.

PERIOD, STREET = 96, 12
LOT0, LOT1 = STREET // 2, PERIOD - STREET // 2     # a lot's rows in its period: the streets centred on the period's start
TILE_H = 960                       # 10 periods: the tile wraps in the middle of a cross street
PLANS = {
    # blocks (x0, x1) between the streets; the roads (x0, x1, kind)
    "avenues": {"blocks": [(6, 92), (132, 232), (272, 368), (376, 474)],
                "roads": [(92, 132, "avenue"), (232, 272, "avenue"), (368, 376, "lane"), (0, 6, "lane"), (474, 480, "lane")]},
    "rooftops": {"blocks": [(6, 80), (92, 170), (290, 386), (398, 474)],
                 "roads": [(80, 92, "street"), (170, 290, "boulevard"), (386, 398, "street"), (0, 6, "lane"),
                           (474, 480, "lane")]},
    "third-mainland": {"blocks": [(6, 84), (96, 158), (322, 384), (396, 474)],
                       "roads": [(84, 96, "street"), (158, 170, "service"), (170, 310, "highway"), (310, 322, "service"),
                                 (384, 396, "street"), (0, 6, "lane"), (474, 480, "lane")]},
}
SECTION_GROUND = ["harbour", "avenues", "rooftops", "rooftops", "third-mainland"]


def lots(plan, pos0, pos1):
    """The plan's lots whose middle lies in [pos0, pos1) on the ground layer: (x0, x1, p0, p1) in
    layer positions (p up), the tile repeating every TILE_H."""
    out = []
    first = int(math.floor(pos0 / PERIOD))
    for k in range(first, int(math.ceil(pos1 / PERIOD)) + 1):
        p0, p1 = k * PERIOD + LOT0, k * PERIOD + LOT1
        if not pos0 <= (p0 + p1) / 2 < pos1:
            continue
        for x0, x1 in PLANS[plan]["blocks"]:
            out.append((x0, x1, p0, p1))
    return out


# --------------------------------------------------------------------------- gameplay from the data

def enemy_data(enemy):
    for kind in ("ground", "air"):
        f = DESIGN / "enemies" / kind / enemy / "data.yaml"
        if f.exists():
            return yaml.safe_load(f.read_text(encoding="utf-8"))
    raise ValueError(f"no enemy data for {enemy}")


def target_units():
    """Every ground target's unit over all difficulties: (target, x, layer position of its centre,
    sprite w, sprite h); a target enters at the top edge at its t (its hitbox's top at the edge)."""
    out = []
    for g in DATA.get("ground_targets", []):
        if "enemy" in g:
            e = enemy_data(g["enemy"])
            hb, size = e["hitbox"], e.get("size", e["hitbox"])
        else:
            hb = size = g["size"]
        spots = {tuple(a) for v in [g] + [g.get(d, {}) for d in ("easy", "hard")] for a in v.get("at", [])}
        for t, x in sorted(spots):
            out.append((g["target"], float(x), scroll_at(t) + SCREEN + hb[1] / 2, size[0], size[1]))
    return out


def walker_waves():
    """The walker waves with paths: (t, units over all difficulties, path points) by entry time."""
    out = []
    for w in DATA.get("waves", []):
        if not w.get("paths"):
            continue
        n = max([w.get("count", 1)] + [w.get(d, {}).get("count", 0) for d in ("easy", "hard")])
        out.append((float(w["t"]), n, [tuple(map(float, p)) for p in w["paths"][0]]))
    return out


def ground_path(t, path, unit):
    """A walker unit's path on the ground layer: its points are screen positions at its own entry."""
    base = scroll_at(t + CONVOY * unit) + SCREEN
    return [(x, base - y) for x, y in path]


def structure_for(target):
    name = target.lower()
    for key, piece in (("landing", "landing-pad"), ("parking", "parking-deck"), ("billboard", "billboard-roof"),
                       ("overpass", "overpass"), ("roof", "roof-nest")):
        if key in name:
            return piece
    raise ValueError(f"no structure for the ground target '{target}'")


def structures():
    """The targets' structures: id -> (x, centre position, w, h, units [(x, pos, w, h)])."""
    groups = {}
    for target, x, pos, w, h in target_units():
        groups.setdefault(structure_for(target), []).append((x, pos, w, h))
    out = {}
    for piece, units in groups.items():
        x0 = min(x - w / 2 for x, _, w, _ in units)
        x1 = max(x + w / 2 for x, _, w, _ in units)
        p0 = min(p - h / 2 for _, p, _, h in units)
        p1 = max(p + h / 2 for _, p, _, h in units)
        margin = {"billboard-roof": 30, "overpass": 0}.get(piece, 34)
        if piece == "overpass":
            w, h = W, int(2 * math.ceil((p1 - p0 + 150) / 2))
            cx, cy = W / 2, (p0 + p1) / 2
        else:
            w = int(2 * math.ceil((x1 - x0 + 2 * margin) / 2))
            h = int(2 * math.ceil((p1 - p0 + 2 * margin) / 2))
            cx, cy = (x0 + x1) / 2, (p0 + p1) / 2
        out[piece] = (round(cx), cy, w, h, units)
    return out


STRUCTURES = structures()


def walk_roofs():
    """One walk roof per walker wave with a roof-and-ramp path (straight up a low roof, then
    diagonally down a ramp to the street): id -> dict(t, n, x, centre, w, h, the roof's box and the
    ramp's start and end in piece px, burnt). The roof reaches 44 px beyond the path on the far side
    and 22 px on the ramp's side, from 46 px before the path's start to 16 px past its turn; the
    ramp runs from the roof's edge along the path's diagonal to its corner on the street."""
    out = {}
    k = 0
    for t, n, path in walker_waves():
        if len(path) < 3 or path[0][0] != path[1][0] or path[2][0] == path[1][0]:
            continue
        k += 1
        g = ground_path(t, path, 0)
        (rx, r0), (_, r1), (ex, e1) = g[0], g[1], g[2]
        side = 1 if ex > rx else -1
        roof = (rx - 44 if side > 0 else rx - 22, r0 - 46, rx + 22 if side > 0 else rx + 44, r1 + 16)
        x0 = min(roof[0], ex - 26) - 4
        x1 = max(roof[2], ex + 26) + 10
        p0, p1 = roof[1] - 8, e1 + 26
        w, h = int(2 * math.ceil((x1 - x0) / 2)), int(2 * math.ceil((p1 - p0) / 2))
        cx, cy = x0 + w / 2, p0 + h / 2
        out[f"walk-roof-{k}"] = {"t": t, "n": n, "x": cx, "centre": cy, "w": w, "h": h,
                                 "roof": (roof[0] - x0, roof[1] - p0, roof[2] - x0, roof[3] - p0),
                                 "start": (rx - x0, r1 - p0), "end": (ex - x0, e1 - p0),
                                 "burnt": section_of(r0) >= 3, "path": g}
    return out


WALK_ROOFS = walk_roofs()


def creeper_corridors():
    """Every walker unit's path on the ground (the part it walks while on screen and a margin)."""
    out = []
    for t, n, path in walker_waves():
        for i in range(n):
            out.append(ground_path(t, path, i))
    return out


# --------------------------------------------------------------------------- the ground units' places on screen
# The smoke must not veil what the player shoots on the ground (art direction: low-air banks leave
# the level's lanes readable). The places are known at every moment: each Creeper unit walks its
# path from its entry, a convoy's husks lie on the stretch its units have walked, the ground
# targets stand still on the ground.

PART_RADIUS = 34          # px round a Creeper's or a ground target's place kept clear (its 60 px sprite)
PART_FEATHER = 18         # px of soft edge beyond it
PART_STEP = 0.1           # s between the sampled moments
_YUP = (SCREEN - 1 - np.arange(SCREEN))[:, None].astype(np.float64)     # screen px up from the bottom, top row first
_XS = np.arange(W)[None, :].astype(np.float64)


def walked(g, dist):
    """The points of the ground path ``g`` a walker has passed after ``dist`` px, its place last."""
    pts = [g[0]]
    for (ax, ap), (bx, bp) in zip(g, g[1:]):
        seg = math.hypot(bx - ax, bp - ap)
        if dist <= seg:
            pts.append((ax + (bx - ax) * dist / seg, ap + (bp - ap) * dist / seg))
            return pts
        pts.append((bx, bp))
        dist -= seg
    return pts


_PLACES = None


def ground_places(t):
    """The ground units' places at t on the ground layer: (Creeper places [(x, pos)], husk tracks
    [[(x, pos), ..]], target boxes [(x, pos, w, h)]); a Creeper from its entry on."""
    global _PLACES
    if _PLACES is None:
        paths = [(wt + CONVOY * i, ground_path(wt, path, i)) for wt, n, path in walker_waves() for i in range(n)]
        _PLACES = (enemy_data("creeper").get("speed", 35), paths,
                   [(x, pos, w, h) for _, x, pos, w, h in target_units()])
    speed_w, paths, boxes = _PLACES
    units, tracks = [], []
    for t0, g in paths:
        if t >= t0:
            pts = walked(g, (t - t0) * speed_w)
            units.append(pts[-1])
            tracks.append(pts)
    return units, tracks, boxes


def _seg_dist(ax, ay, bx, by, py):
    dx, dy = bx - ax, by - ay
    u = np.clip(((_XS - ax) * dx + (py - ay) * dy) / max(dx * dx + dy * dy, 1e-9), 0, 1)
    return np.hypot(_XS - (ax + u * dx), py - (ay + u * dy))


def ground_clear(t, husks=1.0):
    """A screen mask at t (SCREEN x W, top row first): 1 within PART_RADIUS of a Creeper and of a
    ground target's box, ``husks`` along the stretches the Creepers have walked, falling to 0 over
    PART_FEATHER."""
    scroll = scroll_at(t)
    reach = PART_RADIUS + PART_FEATHER
    units, tracks, boxes = ground_places(t)
    out = np.zeros((SCREEN, W))

    def band(p0, p1):
        """The screen rows (a slice, top first) and their ground positions for ground positions [p0, p1]."""
        r0 = int(max(0, math.floor(SCREEN - 1 - (p1 + reach - scroll))))
        r1 = int(min(SCREEN, math.ceil(SCREEN - (p0 - reach - scroll)) + 1))
        return (slice(r0, r1), scroll + _YUP[r0:r1]) if r1 > r0 else (None, None)

    def soft(d):
        return np.clip((reach - d) / PART_FEATHER, 0, 1)
    for x, p in units:
        rows, pos = band(p, p)
        if rows is not None:
            out[rows] = np.maximum(out[rows], soft(np.hypot(_XS - x, pos - p)))
    if husks > 0:
        for pts in tracks:
            for (ax, ap), (bx, bp) in zip(pts, pts[1:]):
                rows, pos = band(min(ap, bp), max(ap, bp))
                if rows is not None:
                    out[rows] = np.maximum(out[rows], husks * soft(_seg_dist(ax, ap, bx, bp, pos)))
    for x, p, w, h in boxes:
        rows, pos = band(p - h / 2, p + h / 2)
        if rows is not None:
            dx = np.maximum(np.abs(_XS - x) - w / 2, 0)
            dy = np.maximum(np.abs(pos - p) - h / 2, 0)
            out[rows] = np.maximum(out[rows], soft(np.hypot(dx, dy) + 8))
    return out


def bank_share(name, t):
    """How much of the low-air bank tile set ``name`` Backdrop.blend draws at t (0..1)."""
    look_a, look_b, weight = looks_at(level_backdrop()[0], t)
    if look_a is look_b:
        return 1.0 if look_a.get("banks") == name else 0.0
    return (weight if look_b.get("banks") == name else 0.0) + ((1 - weight) if look_a.get("banks") == name else 0.0)


def bank_parting(name, w, h, drift, husks):
    """Where the bank tile set ``name`` (w x h, drifting ``drift`` px/s) must thin, in its image's
    px (top row first, wrapping both ways): at every moment it is drawn, the ground units' places
    under it (ground_clear, weighted by its share of the look), mapped to the tile's rows and
    columns as Backdrop draws them (layer scroll, the sideways drift)."""
    mask = np.zeros((h, w))
    cols = np.arange(W)
    for t in np.arange(0, OUTRO_END, PART_STEP):
        share = bank_share(name, t)
        if share < 0.05:
            continue
        clear = ground_clear(t, husks) * share
        if not clear.any():
            continue
        scroll = java_round(scroll_at(t) * FACTORS["low-air"])
        rows = h - 1 - (scroll + SCREEN - 1 - np.arange(SCREEN)) % h      # image row of every screen row
        shift = java_round(drift * t) % W
        image_cols = (cols - shift) % W                                   # image column of every screen column
        np.maximum.at(mask, (rows[:, None], image_cols[None, :]), clear)
    big = Image.fromarray((np.tile(mask, (3, 3)) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(4))
    return np.asarray(big, dtype=np.float64)[h:2 * h, w:2 * w] / 255


# --------------------------------------------------------------------------- height-field lighting

KEY = np.array([-0.55, 0.6, 0.75])
KEY = KEY / np.linalg.norm(KEY)
KEY_XY = math.hypot(KEY[0], KEY[1])
SLOPE = KEY[2] / KEY_XY            # px of height per px towards the light


def box_blur(a, r, wrap_rows=False):
    """Separable box blur of radius r (applied twice: about a Gaussian)."""
    if r < 1:
        return a
    out = a.astype(np.float64)
    for _ in range(2):
        for axis in (0, 1):
            n = out.shape[axis]
            if axis == 0 and wrap_rows:
                pad = np.concatenate([out[-r:], out, out[:r]], axis=0)
            else:
                pad_width = [(0, 0), (0, 0)]
                pad_width[axis] = (r, r)
                pad = np.pad(out, pad_width, mode="edge")
            c = np.cumsum(pad, axis=axis)
            c = np.concatenate([np.zeros_like(np.take(c, [0], axis=axis)), c], axis=axis)
            hi = np.take(c, np.arange(2 * r + 1, 2 * r + 1 + n), axis=axis)
            lo = np.take(c, np.arange(0, n), axis=axis)
            out = (hi - lo) / (2 * r + 1)
    return out


def shifted(a, dr, dc, wrap_rows, fill):
    """a[r + dr, c + dc] (rows wrapped or filled, columns filled)."""
    if wrap_rows:
        out = np.roll(a, -dr, axis=0)
    else:
        out = np.full_like(a, fill)
        h = a.shape[0]
        if dr >= 0:
            out[:h - dr] = a[dr:]
        else:
            out[-dr:] = a[:h + dr]
    if dc:
        res = np.full_like(out, fill)
        w = a.shape[1]
        if dc > 0:
            res[:, :w - dc] = out[:, dc:]
        else:
            res[:, -dc:] = out[:, :w + dc]
        out = res
    return out


def relief(height, albedo, emission, alpha, f, wrap=False, ambient=0.36, key=0.78, ao=0.11, gradient=0.08):
    """A height field (native px, at f x resolution) lit by the key light from the top left: normals,
    hard cast shadows softened a little, occlusion from the blurred field, a gentle top-left to
    bottom-right fall-off across the piece (the point light of the 90s renders), then emission.
    Returns float RGBA (0..1) at f x."""
    hh = height * f                       # in hi-res px
    if wrap:
        gx = (np.roll(hh, -1, 1) - np.roll(hh, 1, 1)) / 2
        gy = (np.roll(hh, -1, 0) - np.roll(hh, 1, 0)) / 2
    else:
        gy, gx = np.gradient(hh)
    nx, ny, nz = -gx, gy, np.ones_like(hh)
    norm = np.sqrt(nx * nx + ny * ny + nz * nz)
    lam = np.clip((nx * KEY[0] + ny * KEY[1] + nz * KEY[2]) / norm, 0, 1)
    lit = np.ones_like(hh)
    span = hh.max() - hh.min()
    steps = int(math.ceil(span / SLOPE)) + 2 if span > 0 else 0
    sx, sy = KEY[0] / KEY_XY, KEY[1] / KEY_XY
    for s in range(1, steps):
        dr, dc = int(round(-sy * s)), int(round(sx * s))
        other = shifted(hh, dr, dc, wrap, -1e9)
        lit = np.where(other > hh + SLOPE * s + 0.5, 0.0, lit)
    lit = box_blur(lit, max(1, f // 2), wrap)
    occ = np.clip((box_blur(hh, 3 * f, wrap) - hh) / f * ao, 0, 0.45)
    rows, cols = hh.shape
    yy, xx = np.mgrid[0:rows, 0:cols].astype(np.float64)
    fall = 1 + gradient * (0.5 - (xx / cols + yy / rows) / 2) * 2
    shade = (ambient + key * lam * lit) * (1 - occ) * fall
    rgb = albedo / 255 * shade[..., None] + emission / 255
    return np.dstack([np.clip(rgb, 0, 1), alpha])


def reduce(arr, f):
    """Float RGBA at f x -> native, colour weighted by coverage (alpha kept fractional)."""
    if f == 1:
        return arr
    h, w = arr.shape[0] // f, arr.shape[1] // f
    a = arr[:h * f, :w * f].reshape(h, f, w, f, 4)
    alpha = a[..., 3].mean(axis=(1, 3))
    rgb = (a[..., :3] * a[..., 3:4]).mean(axis=(1, 3)) / np.maximum(alpha, 1e-6)[..., None]
    return np.dstack([rgb, alpha])


def to_img(arr):
    return Image.fromarray(np.clip(arr * 255 + 0.5, 0, 255).astype(np.uint8), "RGBA")


def finish(arr, colours=32, alpha_levels=4, solid=0.5):
    """Native float RGBA -> palettised image: alpha above ``solid`` becomes opaque, the rest is
    stepped to a few translucency levels (shadows, glows) or cleared."""
    a = arr[..., 3]
    steps = alpha_levels - 1
    stepped = np.where(a >= solid, 1.0, np.floor(np.clip(a / solid, 0, 1) * steps * 0.999) / steps * 0.6)
    out = arr.copy()
    out[..., 3] = stepped
    img = to_img(out)
    a8 = np.array(img)
    a8[a8[..., 3] == 0] = 0
    return quantize_lit([Image.fromarray(a8, "RGBA")], colours)[0]


def quantize_lit(frames, colours):
    """artkit.quantize_set, but the median cut sees the bright and saturated pixels (lamp pools,
    lit windows, neon, embers) eight times over, so a few hundred of them keep their own colours
    against a tile's hundred thousand navy ones."""
    arrays = [np.array(f.convert("RGBA")) for f in frames]
    pixels = np.concatenate([a[a[..., 3] > 0][:, :3] for a in arrays])
    if len(pixels) == 0:
        return frames
    p = pixels.astype(np.int32)
    lum = p @ np.array([299, 587, 114]) // 1000
    sat = p.max(axis=1) - p.min(axis=1)
    weight = 1 + 7 * ((lum > 70) | (sat > 70))
    sample = np.repeat(pixels, weight, axis=0)
    palette = Image.fromarray(sample.reshape(-1, 1, 3), "RGB").quantize(
        colors=colours, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE)
    out = []
    for a in arrays:
        rgb = Image.fromarray(np.ascontiguousarray(a[..., :3]), "RGB")
        mapped = np.array(rgb.quantize(palette=palette, dither=Image.Dither.NONE).convert("RGB"))
        result = np.dstack([mapped, a[..., 3]])
        result[a[..., 3] == 0] = 0
        out.append(Image.fromarray(result.astype(np.uint8), "RGBA"))
    return out


def finish_tile(arr, colours=32, water=0.62):
    """A ground tile set: opaque, but for the harbour's water (one translucent level)."""
    out = arr.copy()
    out[..., 3] = np.where(arr[..., 3] > 0.8, 1.0, water)
    return quantize_lit([to_img(out)], colours)[0]


def drop_shadow(arr, height_native, opacity=0.45):
    """Under a piece's transparent margins: the key light's shadow of its silhouette, cast down and
    right by its height (stepped to two levels)."""
    a = arr[..., 3] > 0.5
    hmax = np.where(a, height_native, 0)
    shade = np.zeros(a.shape)
    reach = int(math.ceil(hmax.max() / SLOPE)) if hmax.max() > 0 else 0
    sx, sy = -KEY[0] / KEY_XY, KEY[1] / KEY_XY
    for s in range(1, reach + 1):
        dr, dc = int(round(sy * s)), int(round(sx * s))
        src = shifted(hmax, -dr, -dc, False, 0)
        shade = np.maximum(shade, (src > SLOPE * s * 0.9).astype(float))
    out = arr.copy()
    sel = (~a) & (shade > 0)
    out[sel, :3] = 0.0
    out[sel, 3] = np.maximum(out[sel, 3], opacity)
    return out


# --------------------------------------------------------------------------- a painting canvas

class Canvas:
    """Height, albedo, emission and alpha at f x a native (w, h) image. Coordinates are native px,
    x from the left and u up from the bottom row; with ``wrap`` the image repeats vertically (a
    tile set) and everything painted near an edge continues across it."""

    def __init__(self, w, h, f=2, wrap=False, rgb=(0, 0, 0), alpha=1.0, height=0.0):
        self.w, self.h, self.f, self.wrap = w, h, f, wrap
        self.hm = np.full((h * f, w * f), float(height))
        self.alb = np.empty((h * f, w * f, 3))
        self.alb[:] = rgb
        self.em = np.zeros((h * f, w * f, 3))
        self.a = np.full((h * f, w * f), float(alpha))
        self.spots = []

    def region(self, x0, u0, x1, u1):
        """(row slice, column slice, x, u) for each copy of the native box [x0, x1) x [u0, u1)."""
        f, hh = self.f, self.h * self.f
        c0, c1 = max(0, int(math.floor(x0 * f))), min(self.w * f, int(math.ceil(x1 * f)))
        if c1 <= c0:
            return
        r0, r1 = int(math.floor((self.h - u1) * f)), int(math.ceil((self.h - u0) * f))
        for k in ((-1, 0, 1) if self.wrap else (0,)):
            a, b = r0 + k * hh, r1 + k * hh
            a2, b2 = max(0, a), min(hh, b)
            if b2 <= a2:
                continue
            ys = self.h - (np.arange(a2, b2) + 0.5) / f + k * self.h
            xs = (np.arange(c0, c1) + 0.5) / f
            xx, uu = np.meshgrid(xs, ys)
            yield slice(a2, b2), slice(c0, c1), xx, uu

    def paint(self, box, mask_fn=None, rgb=None, height=None, lift=None, emis=None, alpha=None, rgb_fn=None,
              height_fn=None, maxh=False):
        x0, u0, x1, u1 = box
        for rs, cs, xx, uu in self.region(x0, u0, x1, u1):
            m = np.ones(xx.shape, bool) if mask_fn is None else mask_fn(xx, uu)
            if not m.any():
                continue
            if rgb is not None:
                self.alb[rs, cs][m] = rgb
            if rgb_fn is not None:
                self.alb[rs, cs][m] = rgb_fn(xx, uu)[m]
            if height is not None:
                hv = self.hm[rs, cs]
                hv[m] = np.maximum(hv[m], height) if maxh else height
            if height_fn is not None:
                hv = self.hm[rs, cs]
                hv[m] = height_fn(xx, uu)[m]
            if lift is not None:
                self.hm[rs, cs][m] += lift
            if emis is not None:
                self.em[rs, cs][m] = emis
            if alpha is not None:
                self.a[rs, cs][m] = alpha

    def rect(self, x0, u0, x1, u1, **kw):
        self.paint((x0, u0, x1, u1), **kw)

    def disc(self, x, u, r, **kw):
        self.paint((x - r, u - r, x + r, u + r), mask_fn=lambda xx, uu: (xx - x) ** 2 + (uu - u) ** 2 <= r * r, **kw)

    def ring(self, x, u, r, width, **kw):
        self.paint((x - r, u - r, x + r, u + r),
                   mask_fn=lambda xx, uu: np.abs(np.hypot(xx - x, uu - u) - r + width / 2) <= width / 2, **kw)

    def line(self, ax, au, bx, bu, width, **kw):
        def mask(xx, uu):
            dx, du = bx - ax, bu - au
            ln = max(1e-6, dx * dx + du * du)
            s = np.clip(((xx - ax) * dx + (uu - au) * du) / ln, 0, 1)
            return np.hypot(xx - ax - s * dx, uu - au - s * du) <= width / 2
        self.paint((min(ax, bx) - width, min(au, bu) - width, max(ax, bx) + width, max(au, bu) + width),
                   mask_fn=mask, **kw)

    def grain(self, seed, amount=0.24, cells=(8, 4)):
        """Albedo grain: periodic value noise (every lattice divides the image)."""
        hh, ww = self.hm.shape
        n = sum(raster.value_noise(ww, hh, c * self.f, seed + i, period=True) * (0.6 ** i) for i, c in enumerate(cells))
        n = (n - n.min()) / max(1e-9, n.max() - n.min())
        self.alb *= (1 - amount / 2 + amount * n)[..., None]

    def render(self, **kw):
        if self.wrap:
            kw.setdefault("gradient", 0.0)          # a tile set has no fall-off across it: it repeats
        return reduce(relief(self.hm, self.alb, self.em, self.a, self.f, wrap=self.wrap, **kw), self.f)


def add_glow(arr, x, u, radius, rgb, strength=1.0, wrap=False):
    """Additive light pool on a native float RGBA image (y up from the bottom row)."""
    h, w = arr.shape[:2]
    r = int(radius * 3) + 1
    x0, x1 = max(0, int(x) - r), min(w, int(x) + r + 1)
    for k in ((-1, 0, 1) if wrap else (0,)):
        uc = u + k * h
        u0, u1 = max(0, int(uc) - r), min(h, int(uc) + r + 1)
        if u1 <= u0 or x1 <= x0:
            continue
        uu, xx = np.mgrid[u0:u1, x0:x1].astype(np.float64)
        g = np.exp(-((xx + 0.5 - x) ** 2 + (uu + 0.5 - uc) ** 2) / (radius * radius)) * strength
        rows = slice(h - u1, h - u0)
        arr[rows, x0:x1, :3] += (rgb / 255)[None, None, :] * g[::-1][..., None]


def paste_sprite(arr, spr, x, u, wrap=False):
    """Alpha-composite a sprite (PIL RGBA, 1-bit alpha) centred at (x, u) on a native float RGBA image."""
    s = np.array(spr).astype(np.float64) / 255
    h, w = arr.shape[:2]
    sh, sw = s.shape[:2]
    left = int(round(x - sw / 2))
    for k in ((-1, 0, 1) if wrap else (0,)):
        top = int(round(h - (u + k * h) - sh / 2))
        for j in range(sh):
            r = top + j
            if not 0 <= r < h:
                continue
            for i in range(sw):
                c = left + i
                if 0 <= c < w and s[j, i, 3] > 0.5:
                    arr[r, c, :3] = s[j, i, :3]
                    arr[r, c, 3] = max(arr[r, c, 3], 1.0)


# --------------------------------------------------------------------------- kit: lots and streets

_TREES = None


def trees():
    global _TREES
    if _TREES is None:
        _TREES = {"big": [foliage_sprite(s, 400 + i, B["EARTH SURFACE"], night=0.62) for i, s in enumerate((12, 14, 16, 13))],
                  "small": [foliage_sprite(s, 420 + i, B["EARTH SURFACE"], night=0.58) for i, s in enumerate((7, 8, 9, 8))]}
    return _TREES


def low_roof(c, x0, u0, x1, u1, rng, height=3.0, style="office"):
    """A low roof drawn flat (no lean): a slab ``height`` px above the street with a parapet, roof
    furniture and, by style, rooftop gardens (small canopies, pasted later from ``c.spots``), water
    tanks, solar panels, skylights or a neon sign."""
    tone = NAVY[1] * rng.uniform(0.95, 1.3) + NAVY[2] * 0.12
    w, d = x1 - x0, u1 - u0
    c.rect(x0, u0, x1, u1, rgb=tone, height=height)
    c.rect(x0 + 1.5, u0 + 1.5, x1 - 1.5, u1 - 1.5, rgb=tone * 0.84, height=height - 0.7)       # parapet ring
    if w > 22 and d > 22 and rng.random() < 0.45:
        c.rect(x0 + 5, u0 + 5, x1 - 5, u1 - 5, rgb=tone * 1.1, height=height - 0.7)             # an inner panel
    if style in ("garden", "residential") and w > 16 and rng.random() < (0.9 if style == "garden" else 0.3):
        gx0, gu0 = x0 + 4, u0 + 4
        gx1 = x1 - 4 - (rng.uniform(0, w * 0.4) if w > 30 else 0)
        gu1 = u1 - 4
        c.rect(gx0, gu0, gx1, gu1, rgb=GREEN[1] * 0.75, height=height - 0.5)
        for _ in range(int((gx1 - gx0) * (gu1 - gu0) / 40)):
            c.spots.append((rng.uniform(gx0 + 3, gx1 - 3), rng.uniform(gu0 + 3, gu1 - 3), "small"))
        return
    for _ in range(int(rng.integers(1, 4))):                                             # AC units, stair huts
        bw, bd = rng.uniform(4, 8), rng.uniform(4, 7)
        if x1 - x0 - 8 - bw < 1 or u1 - u0 - 8 - bd < 1:
            break
        bx, bu = rng.uniform(x0 + 4, x1 - 4 - bw), rng.uniform(u0 + 4, u1 - 4 - bd)
        c.rect(bx, bu, bx + bw, bu + bd, rgb=tone * 1.25, height=height + rng.uniform(1.2, 2.5))
    if style in ("residential", "garden") and w > 16 and rng.random() < 0.6:            # water tanks
        for _ in range(int(rng.integers(1, 3))):
            tx, tu = rng.uniform(x0 + 6, x1 - 6), rng.uniform(u0 + 6, u1 - 6)
            c.disc(tx, tu, 3.0, rgb=CONCRETE * 1.15, height=height + 3.0)
            c.disc(tx - 0.8, tu + 0.8, 1.1, rgb=CONCRETE * 1.45, height=height + 3.2)
    if style == "office" and w > 26 and d > 26 and rng.random() < 0.4:                  # solar panels
        px0, pu0 = x0 + 6, u0 + 6
        for k in range(int((u1 - u0 - 12) / 5)):
            c.rect(px0, pu0 + k * 5, px0 + (x1 - x0) * 0.5, pu0 + k * 5 + 3.5, rgb=ORBIT[1] * 0.8, height=height + 0.4)
    elif style == "office" and w > 22 and rng.random() < 0.5:                             # skylights
        sx = rng.uniform(x0 + 6, x1 - 16)
        for k in range(3):
            c.rect(sx + k * 4, u0 + d * 0.35, sx + k * 4 + 2, u0 + d * 0.65, rgb=NAVY[3] * 0.6,
                   emis=WARM_WIN * 0.3 * rng.uniform(0.3, 1.0), height=height - 0.4)
    if style == "neon" and x1 - x0 > 34 and d > 20 and rng.random() < 0.7:                         # a rooftop sign
        col = NEON[int(rng.integers(len(NEON)))]
        sx, su = rng.uniform(x0 + 6, x1 - 26), rng.uniform(u0 + 6, u1 - 12)
        c.rect(sx, su, sx + 20, su + 5, rgb=NAVY[1], height=height + 1.0)
        for k in range(5):
            c.rect(sx + 1 + k * 4, su + 1, sx + 3 + k * 4, su + 4, emis=col * 0.55, rgb=col * 0.3, height=height + 1.2)


def park(c, x0, u0, x1, u1, rng):
    c.rect(x0, u0, x1, u1, rgb=GREEN[1] * 0.55, height=0.3)
    mx = (x0 + x1) / 2 + rng.uniform(-6, 6)
    c.rect(mx - 1.5, u0, mx + 1.5, u1, rgb=NAVY[1] * 1.3, height=0.2)
    if x1 - x0 > 40 and rng.random() < 0.6:
        px, pu = rng.uniform(x0 + 10, x1 - 10), rng.uniform(u0 + 10, u1 - 10)
        c.paint((px - 8, pu - 6, px + 8, pu + 6), mask_fn=lambda xx, uu: ((xx - px) / 7) ** 2 + ((uu - pu) / 5) ** 2 <= 1,
                rgb=ORBIT[1] * 0.6, height=0.0)
    for _ in range(int((x1 - x0) * (u1 - u0) / 50)):
        tx, tu = rng.uniform(x0 + 5, x1 - 5), rng.uniform(u0 + 5, u1 - 5)
        if abs(tx - mx) > 5:
            c.spots.append((tx, tu, "big"))


def specks(arr, rng, count, wrap=True):
    """Sparse street-level lights on the dark ground between the roofs (the concept's amber dots)."""
    h, w = arr.shape[:2]
    lum = arr[..., :3].mean(axis=-1)
    for _ in range(count):
        y, x = int(rng.integers(h)), int(rng.integers(w))
        if lum[y, x] < 0.075 and arr[y, x, 3] > 0.9:
            arr[y, x, :3] = (JOV[4] * rng.uniform(0.45, 0.8) if rng.random() < 0.75 else COOL_WIN * 0.55) / 255


def city_tile(plan_id, seed, style, park_share=0.0):
    """A city tile: streets and avenues, kerbs, low roofs on the lots, parks; then lamps, trees
    and car lights at 1x."""
    plan = PLANS[plan_id]
    h = TILE_H
    rng = np.random.default_rng(seed)
    c = Canvas(W, h, f=2, wrap=True, rgb=GROUND)
    c.grain(seed, 0.3)
    road = ROAD
    # cross streets
    for k in range(h // PERIOD):
        c.rect(0, k * PERIOD - LOT0, W, k * PERIOD + LOT0, rgb=road, height=0.0)
    lamps, lights = [], []
    for x0, x1, kind in plan["roads"]:
        if kind in ("lane", "street", "service"):
            c.rect(x0, 0, x1, h, rgb=road)
        elif kind in ("avenue", "boulevard"):
            side = 6 if kind == "avenue" else 14
            c.rect(x0, 0, x1, h, rgb=road * 1.28, height=0.4)                       # sidewalks
            c.rect(x0 + side, 0, x1 - side, h, rgb=road, height=0.0)
            mid = (x0 + x1) / 2
            for u in range(0, h, 10):                                               # dashed lane line
                c.rect(mid - 0.5, u, mid + 0.5, u + 5, rgb=NAVY[3] * 0.8)
            if kind == "boulevard":
                for lx in (mid - 22, mid + 22):
                    for u in range(0, h, 10):
                        c.rect(lx - 0.5, u, lx + 0.5, u + 4, rgb=NAVY[3] * 0.5)
            for sx in (x0 + side / 2, x1 - side / 2):                                # trees along the kerbs
                for u in range(4, h, 16):
                    if (u % PERIOD) < LOT0 + 4 or (u % PERIOD) > LOT1 - 4:
                        continue
                    c.spots.append((sx + rng.uniform(-0.8, 0.8), u, "big"))
            for lx in (x0 + side, x1 - side):                                        # sodium lamps
                for u in range(20, h, 40):
                    lamps.append((lx + (1 if lx < mid else -1), u))
            for lane_x, up in ((mid - (x1 - x0 - 2 * side) / 4, False), (mid + (x1 - x0 - 2 * side) / 4, True)):
                for _ in range(int(h / 70)):
                    lights.append((lane_x + rng.uniform(-2, 2), rng.uniform(0, h), up))
        elif kind == "highway":
            pass
    # the lots: one to three low buildings each, dark ground and plazas between them, some parks
    for k in range(h // PERIOD):
        for x0, x1 in plan["blocks"]:
            u0, u1 = k * PERIOD + LOT0, k * PERIOD + LOT1
            c.rect(x0, u0, x1, u0 + 2, rgb=road * 1.2, height=0.3)                  # kerbs
            c.rect(x0, u1 - 2, x1, u1, rgb=road * 1.2, height=0.3)
            if rng.random() < park_share:
                park(c, x0 + 2, u0 + 3, x1 - 2, u1 - 3, rng)
                continue
            cuts = [x0 + rng.uniform(2, 6)]
            if x1 - x0 > 60 and rng.random() < 0.65:
                cuts.append(rng.uniform(x0 + 26, x1 - 26))
            cuts.append(x1 - rng.uniform(2, 6))
            for a, b in zip(cuts, cuts[1:]):
                if rng.random() < 0.08:                                               # a paved plaza
                    c.rect(a, u0 + 3, b, u1 - 3, rgb=road * 1.1, height=0.2)
                    for gx in np.arange(a + 4, b - 2, 8):
                        c.rect(gx, u0 + 3, gx + 0.6, u1 - 3, rgb=road * 0.85, height=0.2)
                    fx, fu = (a + b) / 2, (u0 + u1) / 2
                    c.disc(fx, fu, 7, rgb=CONCRETE * 1.2, height=0.8)
                    c.disc(fx, fu, 5, rgb=ORBIT[1] * 0.7, height=0.5)
                    continue
                ua = u0 + rng.uniform(3, 9)
                ub = u1 - rng.uniform(3, 9)
                if rng.random() < 0.35 and ub - ua > 40:                              # two buildings, a yard between
                    m = rng.uniform(ua + 16, ub - 16)
                    low_roof(c, a + 1.5, ua, b - 1.5, m - 2.5, rng, height=rng.uniform(2.2, 4.2), style=style)
                    low_roof(c, a + 1.5, m + 2.5, b - 1.5, ub, rng, height=rng.uniform(2.2, 4.2), style=style)
                else:
                    low_roof(c, a + 1.5, ua, b - 1.5, ub, rng, height=rng.uniform(2.2, 4.2), style=style)
    if plan_id == "third-mainland":
        highway_deck(c, 170, 310, h, rng, lamps)
    arr = c.render()
    if style == "neon":                                                               # neon spill on the streets
        for _ in range(70):
            x0, x1, _kind = plan["roads"][int(rng.integers(len(plan["roads"])))]
            if _kind == "highway":
                continue
            col = NEON[int(rng.integers(len(NEON)))]
            add_glow(arr, rng.uniform(x0, x1), rng.uniform(0, h), rng.uniform(5, 9), col * 0.3, wrap=True)
    specks(arr, rng, W * h // 260)
    for lx, lu in lamps:
        add_glow(arr, lx, lu, 4.4, SODIUM * 1.25, wrap=True)
    tr = trees()
    for tx, tu, kind in c.spots:
        paste_sprite(arr, tr[kind][int(rng.integers(len(tr[kind])))], tx, tu, wrap=True)
    for lx, lu, up in lights:                                                         # car lights
        r = h - 1 - int(lu) % h
        col = (np.array([255, 240, 210]) if up else np.array([230, 40, 50])) / 255
        for dx in (-1, 1):
            arr[r, int(lx + dx), :3] = col
    return arr


def highway_deck(c, x0, x1, h, rng, lamps, height=6.0, across=False):
    """The elevated highway's deck along a tile: barriers at both edges, three lanes each way,
    a flat median with lamp posts (Creepers walk along it), expansion joints."""
    mid = (x0 + x1) / 2
    c.rect(x0, 0, x1, h, rgb=ROAD * 1.15, height=height)
    c.rect(x0, 0, x0 + 3, h, rgb=CONCRETE * 1.4, height=height + 1.4)
    c.rect(x1 - 3, 0, x1, h, rgb=CONCRETE * 1.4, height=height + 1.4)
    c.rect(mid - 5, 0, mid + 5, h, rgb=CONCRETE * 1.1, height=height + 0.3)
    lane = (x1 - x0 - 16) / 6
    for k in (1, 2, 4, 5):
        lx = x0 + 3 + k * lane + (5 if k > 3 else 0)
        for u in range(0, h, 12):
            c.rect(lx - 0.5, u, lx + 0.5, u + 6, rgb=NAVY[3] * 0.9, height=height)
    for u in range(24, h, 48):
        c.rect(x0 + 3, u, x1 - 3, u + 1, rgb=ROAD * 0.8, height=height)
        lamps.append((mid, u - 24))


def harbour_tile():
    """Section 1: the harbour's water (translucent: the Gulf's glow on the deep layer shows in it),
    piers, the quay with bollards and lamps, container yards behind it."""
    h = TILE_H
    rng = np.random.default_rng(801)
    c = Canvas(W, h, f=2, wrap=True, rgb=GROUND)
    c.grain(801, 0.3)
    water = 196
    hh, ww = c.hm.shape
    rip = noise(ww, hh, [24, 12, 6], 803)
    c.paint((0, 0, water, h), rgb_fn=lambda xx, uu: np.zeros(xx.shape + (3,)), alpha=0.62, height=0.0)
    sub = c.alb[:, :water * 2]
    sub[:] = (ORBIT[1] * 0.5 + NAVY[1] * 0.5)[None, None, :] * (0.8 + 0.4 * rip[:, :water * 2])[..., None]
    c.rect(water, 0, water + 14, h, rgb=CONCRETE * 1.25, height=2.5)                 # the quay edge
    for u in range(8, h, 24):
        c.disc(water + 3, u, 1.2, rgb=CONCRETE * 0.7, height=3.4)
    c.rect(water + 14, 0, 250, h, rgb=ROAD * 1.1, height=2.4)                         # quay road
    for u in range(0, h, 12):
        c.rect(water + 30, u, water + 31, u + 6, rgb=NAVY[3] * 0.7, height=2.4)
    for k in range(h // PERIOD):
        c.rect(250, k * PERIOD - LOT0, W, k * PERIOD + LOT0, rgb=ROAD * 1.1, height=2.4)
    lamps = [(water + 12, u) for u in range(20, h, 48)]
    for pu, length in ((150, 150), (450, 90), (690, 160)):                            # piers over the water
        c.rect(water - length, pu, water, pu + 22, rgb=CONCRETE * 1.35, height=2.2)
        for x in range(int(water - length) + 4, water, 6):
            c.rect(x, pu, x + 0.6, pu + 22, rgb=CONCRETE * 0.8, height=2.2)
        c.rect(water - length, pu - 2, water - length + 4, pu + 24, rgb=CONCRETE * 0.9, height=2.6)
        lamps.append((water - length + 6, pu + 11))
        lamps.append((water - length / 2, pu + 11))
    for k in range(h // PERIOD):                                                      # container yards
        u0, u1 = k * PERIOD + LOT0 + 2, k * PERIOD + LOT1 - 2
        for x0, x1 in ((252, 356), (364, 476)):
            if k == 4 and x0 == 364:
                low_roof(c, x0, u0, x1, u1, rng, height=5.0, style="office")          # a warehouse
                continue
            container_rows(c, x0 + 2, u0, x1 - 2, u1, rng)
        c.rect(356, u0, 364, u1, rgb=ROAD * 1.1, height=2.4)
    arr = c.render()
    specks(arr, rng, W * h // 120)
    for lx, lu in lamps:
        add_glow(arr, lx, lu, 4.4, SODIUM * 1.25, wrap=True)
    return arr


CONTAINER = [MARS[2] * 0.55, ORBIT[2] * 0.5, GREEN[2] * 0.5, np.array([120, 116, 124], float) * 0.6,
             JOV[2] * 0.55, ACCENT[0] * 0.35]


def container_rows(c, x0, u0, x1, u1, rng, base=2.4):
    """Rows of stacked containers (5 x 24 px from above), one to three high, muted colours."""
    x = x0
    while x + 5 <= x1:
        u = u0
        while u + 24 <= u1:
            if rng.random() < 0.88:
                stack = int(rng.integers(1, 4))
                col = CONTAINER[int(rng.integers(len(CONTAINER)))] * rng.uniform(0.85, 1.15)
                c.rect(x, u, x + 5, u + 24, rgb=col, height=base + 2.6 * stack)
                for k in range(1, 6):
                    c.rect(x, u + k * 4, x + 5, u + k * 4 + 0.6, rgb=col * 0.75, height=base + 2.6 * stack)
            u += 25
        x += 6 if (x - x0) % 30 < 24 else 9


# --------------------------------------------------------------------------- deep, banks, wisps

def gulf(w, h):
    """The deep layer: the night sky's glow on the Gulf of Guinea (clouds lit amber-violet by the
    city from below, a few star glints); opaque; only seen through the harbour's water."""
    n = raster.fbm(w, h, 96, 811, octaves=4, period=True)
    fine = raster.fbm(w, h, 24, 813, octaves=3, period=True)
    base = ORBIT[0] * 0.6 + NAVY[1] * 0.6
    glow = np.clip((n - 0.45) * 2.2, 0, 1)
    rgb = base[None, None, :] * (0.8 + 0.3 * fine)[..., None] + (JOV[2] * 0.5)[None, None, :] * glow[..., None] \
        + (CHITIN[1] * 0.4)[None, None, :] * np.clip(0.6 - n, 0, 1)[..., None]
    rng = np.random.default_rng(817)
    for _ in range(90):
        x, y = int(rng.integers(w)), int(rng.integers(h))
        rgb[y, x] = np.maximum(rgb[y, x], np.array([150, 160, 200]) * rng.uniform(0.4, 0.9))
    arr = np.dstack([rgb / 255, np.ones((h, w))])
    return finish(arr, 16)


def periodic_bank(w, h, seed, cover, soft, colours, max_alpha, light, glow_rgb, glow_seed, part=None):
    """r03's bank() on noise that repeats both ways, blurred on a 3 x 3 tiling and cropped, so the
    bank wraps top to bottom and side to side (the drifting ones). ``part`` (mask, depth) lowers the
    noise by depth x mask, so the bank parts there with its own billowy edges (bank_parting)."""
    n = noise(w, h, [96, 48, 24], seed)
    if part is not None:
        n = n - part[1] * part[0]
    big = np.tile(n, (3, 3))
    lamp = noise(w, h, [96, 48], glow_seed)
    glow = np.tile(np.clip((lamp - 0.45) * 2.0, 0, 1), (3, 3))
    img = bank(big, cover, soft, colours, max_alpha=max_alpha, light=light, glow=(glow_rgb, glow))
    img = img.crop((w, h, 2 * w, 2 * h))
    return step_alpha(artkit.quantize_set([img], 12)[0], 6)


def fog(w, h):
    """Light: r03's fog banks, glowing above the dark city, lit amber from below by the lamps."""
    pal = NAVY
    greyish = [tuple(int(c * 0.6 + g) for c in col) for col, g in ((pal[1], 34), (pal[2], 46), (pal[3], 52))]
    return periodic_bank(w, h, 401, 0.68, 0.14, greyish, 118, 3.0, JOV[3] * 0.32, 411)


SMOKE_PART = (0.5, 0.13)         # medium: (husk track weight, noise depth): thinned over the ground units
HEAVY_PART = (1.0, 0.34)         # heavy: parted over the convoy, its husks and the highway's targets


def smoke(w, h):
    """Medium: smoke banks, brown-grey, lit orange from the fires below; drifting; thinned where the
    Creepers walk and the ground targets stand while it is drawn, a little denser elsewhere (about
    20 % coverage, the art direction's medium)."""
    cols = [(34, 26, 32), (62, 50, 54), (92, 78, 74)]
    part = bank_parting("smoke", w, h, TILE_SETS["smoke"][2], SMOKE_PART[0])
    return periodic_bank(w, h, 421, 0.57, 0.16, cols, 150, 3.0, MARS[3] * 0.22, 431, part=(part, SMOKE_PART[1]))


def smoke_heavy(w, h):
    """Heavy: the smoke wall before the Needler circle, about 40 % of the screen covered (the art
    direction's heavy end), parted over the highway convoy and its husks, a little thinner."""
    cols = [(30, 24, 30), (56, 46, 50), (86, 72, 70)]
    part = bank_parting("smoke-heavy", w, h, TILE_SETS["smoke-heavy"][2], HEAVY_PART[0])
    return periodic_bank(w, h, 441, 0.5, 0.2, cols, 165, 3.0, MARS[3] * 0.2, 451, part=(part, HEAVY_PART[1]))


def mist(w, h):
    """Clear and light: the concept's thin high-air mist (additive, faint)."""
    m = raster.fbm(w, h, 60, 41, octaves=4, period=True)
    a = np.clip((m - 0.56) * 2.5, 0, 1) ** 1.5 * 80 / 255
    a = artkit.ordered_dither(a / 0.32, 5) * 0.32
    arr = np.dstack([np.broadcast_to(NAVY[3] / 255, (h, w, 3)), a])
    return to_img(arr)


def ash(w, h):
    """Medium and heavy: drifting ash (additive): faint warm streaks and specks."""
    m = raster.fbm(w, h, 60, 461, octaves=3, period=True)
    a = np.clip((m - 0.6) * 2.2, 0, 1) ** 1.6 * 0.22
    rgb = np.broadcast_to(np.array([120, 92, 80]) / 255, (h, w, 3)).copy()
    rng = np.random.default_rng(463)
    for _ in range(520):
        x, y = int(rng.integers(w)), int(rng.integers(h))
        length = int(rng.integers(1, 4))
        for k in range(length):
            yy = (y + k) % h
            a[yy, x] = max(a[yy, x], rng.uniform(0.25, 0.5))
            rgb[yy, x] = np.array([200, 150, 110]) / 255 * rng.uniform(0.7, 1.0)
    a = np.round(a * 6) / 6
    return to_img(np.dstack([rgb, a]))


# --------------------------------------------------------------------------- pieces: highways, harbour

def cross_highway(w, h):
    """An elevated cross highway spanning the play field: the deck (barriers, three lanes each way,
    a median with lamp posts), its shadow cast down and right onto the street level."""
    deck0, deck1 = 22, h - 14
    c = Canvas(w, h, f=2, alpha=0.0)
    c.rect(0, deck0, w, deck1, rgb=ROAD * 1.15, height=7.0, alpha=1.0)
    c.rect(0, deck1 - 3, w, deck1, rgb=CONCRETE * 1.4, height=8.4)
    c.rect(0, deck0, w, deck0 + 3, rgb=CONCRETE * 1.4, height=8.4)
    mid = (deck0 + deck1) / 2
    c.rect(0, mid - 2.5, w, mid + 2.5, rgb=CONCRETE * 1.1, height=7.8)
    lane = (deck1 - deck0 - 11) / 6
    for k in (1, 2, 4, 5):
        lu = deck0 + 3 + k * lane + (5 if k > 3 else 0)
        for x in range(0, w, 12):
            c.rect(x, lu - 0.5, x + 6, lu + 0.5, rgb=NAVY[3] * 0.9)
    for x in range(30, w, 48):
        c.rect(x, deck0 + 3, x + 1, deck1 - 3, rgb=ROAD * 0.8)
    arr = c.render()
    arr = drop_shadow(arr, np.where(arr[..., 3] > 0.5, 7.0, 0.0), 0.5)
    for x in range(14, w, 60):
        add_glow(arr, x, mid, 5.5, SODIUM * 1.2)
        arr[h - 1 - int(mid), x, :3] = LAMP / 255
    rng = np.random.default_rng(2207)
    for _ in range(26):
        x = int(rng.integers(3, w - 3))
        up = rng.random() < 0.5
        u = deck0 + 3 + lane * (rng.choice([0.5, 1.5, 2.5]) if up else rng.choice([3.5, 4.5, 5.5])) + (5 if not up else 0)
        col = (np.array([255, 240, 210]) if up else np.array([230, 40, 50])) / 255
        r = h - 1 - int(u)
        arr[r, x - 1, :3] = col
        arr[r, x + 1, :3] = col
    return finish(arr, 32)


def quay_crane(w, h):
    """A gantry crane on the quay from above: the portal frame on its rails (right), the boom out over
    the water (left), the trolley, the machinery house; warning lights at the boom's tip."""
    c = Canvas(w, h, f=4, alpha=0.0)
    mid = h / 2 + 6
    frame_x0 = w - 58
    w = w - 18                                     # room for the shadow on the right
    yellow = JOV[3] * 0.55
    for u in (mid - 12, mid + 10):
        c.rect(frame_x0, u, w - 4, u + 3, rgb=yellow, height=14.0, alpha=1.0)        # portal beams
    for x in (frame_x0, w - 7):
        c.rect(x, mid - 14, x + 3, mid + 15, rgb=yellow * 0.9, height=13.0, alpha=1.0)
    c.rect(4, mid - 4, w - 4, mid + 4, rgb=yellow * 1.05, height=16.0, alpha=1.0)      # the boom
    for x in range(8, w - 8, 7):
        c.rect(x, mid - 4, x + 1, mid + 4, rgb=yellow * 0.7, height=16.2, alpha=1.0)
    c.rect(frame_x0 + 6, mid - 7, frame_x0 + 22, mid + 7, rgb=CONCRETE * 1.3, height=18.0, alpha=1.0)   # machinery
    c.rect(w * 0.35, mid - 6, w * 0.35 + 9, mid + 6, rgb=CONCRETE * 1.1, height=17.0, alpha=1.0)        # trolley
    c.disc(7, mid, 1.6, rgb=MARS[3], emis=MARS[4] * 0.8, height=16.5, alpha=1.0)
    arr = c.render()
    arr = drop_shadow(arr, np.where(arr[..., 3] > 0.5, 14.0, 0.0), 0.45)
    return finish(arr, 24)


def container_ship(w, h):
    """A container ship moored at a pier, bow up: the hull, rows of containers, the bridge
    house at the stern with lit windows, mast lights."""
    c = Canvas(w, h, f=2, alpha=0.0)
    cx = w / 2 - 6
    half = 34

    def hull(xx, uu):
        bow = h - 8
        width = np.where(uu > bow - 60, half * np.sqrt(np.clip((bow - uu) / 60, 0, 1)), half)
        return (np.abs(xx - cx) <= width) & (uu >= 8) & (uu <= bow)
    c.paint((0, 0, w, h), mask_fn=hull, rgb=np.array([46, 30, 36], float), height=5.0, alpha=1.0)
    c.paint((0, 0, w, h), mask_fn=lambda xx, uu: hull(xx, uu) & (np.abs(xx - cx) <= half - 3) & (uu > 11),
            rgb=CONCRETE * 0.9, height=4.4)
    rng = np.random.default_rng(831)
    for u in range(48, h - 70, 26):
        for x in np.arange(cx - half + 5, cx + half - 9, 6):
            stack = int(rng.integers(1, 4))
            col = CONTAINER[int(rng.integers(len(CONTAINER)))]
            c.rect(x, u, x + 5, u + 24, rgb=col, height=4.4 + 2.6 * stack)
    c.rect(cx - half + 4, 14, cx + half - 4, 40, rgb=np.array([170, 168, 176], float) * 0.6, height=14.0)
    for x in np.arange(cx - half + 7, cx + half - 7, 4):
        c.rect(x, 22, x + 2, 24, emis=WARM_WIN * 0.6, height=14.0)
    c.disc(cx, h - 20, 1.4, emis=np.array([200, 255, 200]) * 0.6, height=8.0)
    arr = c.render()
    arr = drop_shadow(arr, np.where(arr[..., 3] > 0.5, 6.0, 0.0), 0.4)
    return finish(arr, 32)


def container_yard(w, h):
    """A container yard over a city block at the harbour's edge: stacks in rows, aisles, a straddle
    carrier, yard lamps."""
    c = Canvas(w, h, f=2, alpha=0.0)
    c.rect(4, 4, w - 4, h - 4, rgb=ROAD * 1.15, height=0.4, alpha=1.0)
    rng = np.random.default_rng(841)
    for u0 in range(8, h - 30, 32):
        container_rows(c, 8, u0, w - 8, u0 + 26, rng, base=0.4)
    c.rect(w * 0.42, h * 0.5 - 14, w * 0.42 + 12, h * 0.5 + 14, rgb=JOV[3] * 0.5, height=11.0)
    arr = c.render()
    for x, u in ((10, 10), (w - 10, 10), (10, h - 10), (w - 10, h - 10), (w / 2, h / 2)):
        add_glow(arr, x, u, 9.0, SODIUM * 1.1)
    arr[0, :, 3] = arr[-1, :, 3] = 0
    arr[:, 0, 3] = arr[:, -1, 3] = 0
    return finish(arr, 32)


# --------------------------------------------------------------------------- pieces: gameplay structures

PAD = np.array([62, 68, 96], float)            # the landing pad's pale concrete (was the dark road tone)
SPOT = np.array([150, 156, 178], float)        # a turret's spot: a pale painted disc, so the dark Spine Turret reads on it
PAINT = np.array([150, 158, 182], float)       # the spot's painted ring: pale grey (not the loot targets' amber)
SPOT_LAMP = np.array([150, 164, 200], float)   # cool white lamps round the spot
SPOT_R = 17                                    # px: the spot's radius (a 40 px Spine Turret's body and spikes)


def turret_spots(c, w, h, kind, base):
    """Under each turret unit of the structure (readability, round 30's capture: dark turrets on a dark
    pad): a pale spot, a painted ring round it and four small lamps; returns the lamps."""
    sx, scy, sw, sh, units = STRUCTURES[kind]
    lights = []
    for ux, upos, _, _ in units:
        x, u = ux - (sx - w / 2), upos - (scy - h / 2)
        c.disc(x, u, SPOT_R + 2, rgb=SPOT * 0.45, height=base - 0.5)                   # a dark seam round it
        c.disc(x, u, SPOT_R, rgb=SPOT, height=base - 0.5)
        c.ring(x, u, SPOT_R + 4.5, 1.5, rgb=PAINT)
        for k in range(4):
            a = math.pi / 4 + k * math.pi / 2
            lights.append((x + math.cos(a) * (SPOT_R + 6), u + math.sin(a) * (SPOT_R + 6), SPOT_LAMP))
    return lights


def podium(w, h, kind):
    """A low structure drawn flat (D1 = a) for a ground target's units: a landing pad on a low roof, a
    plain low roof, the billboard's low roof or the parking deck's top level; its shadow down and right."""
    rng = np.random.default_rng({"landing-pad": 851, "roof-nest": 853, "billboard-roof": 857, "parking-deck": 859}[kind])
    c = Canvas(w, h, f=2, alpha=0.0)
    m = 6
    x0, u0, x1, u1 = m, m + 6, w - m - 6, h - m
    base = 5.0
    tone = NAVY[1] * 0.55 + NAVY[2] * 0.45
    c.rect(x0, u0, x1, u1, rgb=tone, height=base, alpha=1.0)
    c.rect(x0 + 3, u0 + 3, x1 - 3, u1 - 3, rgb=tone * 0.85, height=base - 0.8)
    cx, cu = (x0 + x1) / 2, (u0 + u1) / 2
    lights = []
    if kind == "landing-pad":
        r = min(x1 - x0, u1 - u0) / 2 - 8
        c.disc(cx, cu, r, rgb=PAD, height=base - 0.4)                                    # pale pad concrete
        c.ring(cx, cu, r - 4, 2.0, rgb=JOV[4] * 0.6)
        c.ring(cx, cu, r * 0.45, 1.6, rgb=JOV[4] * 0.45)
        for k in range(16):
            a = 2 * math.pi * k / 16
            lights.append((cx + math.cos(a) * (r + 1), cu + math.sin(a) * (r + 1), ORBIT[4] if k % 2 else JOV[4]))
        c.rect(x1 - 18, u1 - 16, x1 - 6, u1 - 6, rgb=NAVY[2], height=base + 3.0)        # stair hut
    elif kind == "parking-deck":
        for x in np.arange(x0 + 8, x1 - 8, 9):
            for ua, ub in ((u0 + 6, u0 + 24), (u1 - 24, u1 - 6), (cu - 20, cu - 2), (cu + 2, cu + 20)):
                c.rect(x, ua, x + 0.8, ub, rgb=NAVY[3] * 0.7, height=base - 0.8)
                if rng.random() < 0.45:
                    col = CONTAINER[int(rng.integers(len(CONTAINER)))] * 1.1
                    c.rect(x + 2, ua + 3, x + 7, ub - 3, rgb=col, height=base + 1.2)
        for xx, uu in ((x0 + 3, u0 + 3), (x1 - 13, u1 - 13)):
            c.rect(xx, uu, xx + 10, uu + 10, rgb=CONCRETE * 1.2, height=base + 3.5)      # stair towers
        for xx in (x0 + 30, x1 - 30):
            lights.append((xx, cu, LAMP))
    elif kind == "billboard-roof":
        for xx in (cx - 22, cx + 18):
            c.rect(xx, cu - 4, xx + 4, cu + 4, rgb=CONCRETE * 1.3, height=base + 1.5)    # the sign's footings
        for _ in range(2):
            bx, bu = rng.uniform(x0 + 6, x0 + 20), rng.uniform(u0 + 6, u1 - 16)
            c.rect(bx, bu, bx + 7, bu + 6, rgb=NAVY[2], height=base + 2.0)
    else:  # roof-nest: gravel roof, tanks and AC units in the corners, a garden strip
        c.rect(x0 + 6, u1 - 16, x1 - 40, u1 - 6, rgb=GREEN[2] * 0.42, height=base - 0.4)
        for _ in range(int((x1 - x0) * 0.4)):
            c.disc(rng.uniform(x0 + 8, x1 - 42), rng.uniform(u1 - 15, u1 - 7), rng.uniform(1.2, 2.0), rgb=GREEN[3] * 0.5,
                   height=base + 0.8)
        for xx, uu in ((x1 - 14, u1 - 12), (x1 - 26, u1 - 12)):
            c.disc(xx, uu, 4.0, rgb=CONCRETE * 1.2, height=base + 3.5)
        c.rect(x0 + 6, u0 + 6, x0 + 16, u0 + 14, rgb=NAVY[2], height=base + 2.0)
    if kind != "billboard-roof":
        lights += turret_spots(c, w, h, kind, base)
    c.grain(len(kind) * 37, 0.18, cells=(4, 2))
    arr = c.render()
    arr = drop_shadow(arr, np.where(arr[..., 3] > 0.5, base, 0.0), 0.45)
    for lx, lu, col in lights:
        add_glow(arr, lx, lu, 3.0, col * 0.6)
        arr[h - 1 - int(lu), int(lx), :3] = np.minimum(1, col / 255 * 1.1)
    return finish(arr, 32)


def overpass(w, h):
    """The collapsed overpass the mortars stand on: a diagonal deck through the units' line, broken
    off at both ends with rubble and scorch on the street below; its shadow down and right."""
    _, cy, _, _, units = STRUCTURES["overpass"]
    xs = np.array([u[0] for u in units])
    ps = np.array([u[1] for u in units]) - (cy - h / 2)          # piece px, up from its bottom
    slope, icpt = np.polyfit(xs, ps, 1)
    half = 26
    x_lo, x_hi = xs.min() - 50, xs.max() + 50
    c = Canvas(w, h, f=2, alpha=0.0)
    rng = np.random.default_rng(861)
    norm = math.sqrt(1 + slope * slope)

    def across(xx, uu):
        return (uu - (slope * xx + icpt)) / norm

    def along(xx, uu):
        return (xx + slope * (uu - icpt)) / norm

    def deck(xx, uu):
        a = along(xx, uu)
        jag_lo = x_lo / norm + 6 * np.sin(across(xx, uu) * 0.6) + 3 * np.sin(across(xx, uu) * 1.7 + 1)
        jag_hi = x_hi / norm + 6 * np.sin(across(xx, uu) * 0.5 + 2) + 3 * np.sin(across(xx, uu) * 1.9)
        return (np.abs(across(xx, uu)) <= half) & (a >= jag_lo) & (a <= jag_hi)
    for end, d in ((x_lo, -1), (x_hi, 1)):                                          # rubble below the breaks
        for _ in range(60):
            rx = end + d * rng.uniform(0, 46) + rng.normal(0, 6)
            ru = slope * rx + icpt + rng.normal(0, 18)
            r = rng.uniform(2, 5)
            rx, ru = float(np.clip(rx, 8, w - 14)), float(np.clip(ru, 10, h - 8))
            c.disc(rx, ru, r, rgb=CONCRETE * rng.uniform(0.6, 1.0), height=rng.uniform(0.5, 3.0), alpha=1.0, maxh=True)
        for _ in range(12):
            rx = end + d * rng.uniform(4, 40)
            ru = slope * rx + icpt + rng.normal(0, 12)
            rx, ru = float(np.clip(rx, 8, w - 14)), float(np.clip(ru, 10, h - 8))
            c.disc(rx, ru, rng.uniform(1, 2.5), rgb=SCORCH, emis=EMBER * rng.uniform(0.2, 0.5), height=1.0, alpha=1.0)
    c.paint((0, 0, w, h), mask_fn=deck, rgb=ROAD * 0.6 + CONCRETE * 0.7, height=7.0, alpha=1.0)
    c.paint((0, 0, w, h), mask_fn=lambda xx, uu: deck(xx, uu) & (np.abs(across(xx, uu)) >= half - 3),
            rgb=CONCRETE * 1.35, height=8.4)
    c.paint((0, 0, w, h), mask_fn=lambda xx, uu: deck(xx, uu) & (np.abs(across(xx, uu)) <= 1.2), rgb=NAVY[3] * 0.8)
    for _ in range(18):                                                              # cracks and scorch
        ax = rng.uniform(x_lo + 10, x_hi - 10)
        au = slope * ax + icpt + rng.uniform(-half + 4, half - 4)
        c.paint((ax - 9, au - 9, ax + 9, au + 9),
                mask_fn=lambda xx, uu, ax=ax, au=au: deck(xx, uu) & ((xx - ax) ** 2 + (uu - au) ** 2 <= 60),
                rgb_fn=lambda xx, uu: np.broadcast_to(SCORCH * 1.4, xx.shape + (3,)))
    arr = c.render()
    arr = drop_shadow(arr, np.where(arr[..., 3] > 0.5, 7.0, 0.0), 0.5)
    return finish(arr, 32)


def walk_roof(name):
    """A Creeper wave's walk roof (D1 = a): a low roof under the path's straight start, a ramp from
    its edge down to the street along the path's diagonal, rails along the ramp; scorched in the
    smoke district. The path's strip is kept clear of roof furniture."""
    spec = WALK_ROOFS[name]
    w, h = spec["w"], spec["h"]
    rx0, ru0, rx1, ru1 = spec["roof"]
    sx, su = spec["start"]
    ex, e1 = spec["end"]
    burnt = spec["burnt"]
    rng = np.random.default_rng(871 + int(spec["t"]))
    c = Canvas(w, h, f=2, alpha=0.0)
    base = 5.0
    tone = (NAVY[1] * 0.15 + NAVY[2] * 0.85) * (0.7 if burnt else 1.0)
    c.rect(rx0, ru0, rx1, ru1, rgb=tone, height=base, alpha=1.0)
    c.rect(rx0 + 2.5, ru0 + 2.5, rx1 - 2.5, ru1 - 2.5, rgb=tone * 0.86, height=base - 0.8)
    side = 1 if ex > sx else -1
    far = rx0 + 4 if side > 0 else rx1 - 12                                       # furniture off the path
    for k in range(3):
        bu = ru0 + 8 + k * (ru1 - ru0 - 24) / 3 + rng.uniform(0, 6)
        c.rect(far, bu, far + 8, bu + 7, rgb=NAVY[2] * (0.6 if burnt else 0.9), height=base + 2.0)
    length = math.hypot(ex - sx, e1 - su)
    dx, du = (ex - sx) / length, (e1 - su) / length
    exits = [((rx1 if side > 0 else rx0) - sx) / dx if dx else math.inf, (ru1 - su) / du if du > 0 else math.inf]
    t_edge = min(exits)

    def coords(xx, uu):
        return (xx - sx) * dx + (uu - su) * du, -(xx - sx) * du + (uu - su) * dx

    def ramp_mask(xx, uu):
        s_, q = coords(xx, uu)
        return (np.abs(q) <= 13) & (s_ >= t_edge - 3) & (s_ <= length + 6)

    def ramp_height(xx, uu):
        s_, _ = coords(xx, uu)
        return np.clip((base - 0.4) * (1 - (s_ - t_edge) / max(1.0, length - t_edge)), 0.2, base - 0.4)
    c.paint((0, 0, w, h), mask_fn=ramp_mask, rgb=ROAD * 1.3 * (0.8 if burnt else 1.0), height_fn=ramp_height, alpha=1.0)
    c.paint((0, 0, w, h), mask_fn=lambda xx, uu: ramp_mask(xx, uu) & (np.abs(coords(xx, uu)[1]) >= 11.5),
            rgb=CONCRETE * 1.5, lift=1.0)
    c.paint((0, 0, w, h), mask_fn=lambda xx, uu: ramp_mask(xx, uu) & ((coords(xx, uu)[0] % 6) < 0.8), rgb=ROAD * 0.9)
    if burnt:
        for _ in range(16):
            bx, bu = rng.uniform(rx0 + 4, rx1 - 4), rng.uniform(ru0 + 4, ru1 - 4)
            c.paint((bx - 6, bu - 6, bx + 6, bu + 6), mask_fn=lambda xx, uu, bx=bx, bu=bu: (xx - bx) ** 2 + (uu - bu) ** 2 <= 30,
                    rgb_fn=lambda xx, uu: np.broadcast_to(SCORCH * 1.3, xx.shape + (3,)))
    c.grain(int(spec["t"]), 0.16, cells=(4, 2))
    arr = c.render()
    arr = drop_shadow(arr, np.where(arr[..., 3] > 0.5, base, 0.0))
    for k in (0.3, 0.7):
        lx, lu = (rx0 + 4 if side > 0 else rx1 - 4), ru0 + (ru1 - ru0) * k
        add_glow(arr, lx, lu, 4.0, (EMBER * 0.5) if burnt else (SODIUM * 0.9))
    return finish(arr, 32)


def burnt_block(w, h, seed):
    """A collapsed block in the smoke district, flat over a lot: rubble heaps, broken wall stubs,
    black scorch, embers glowing in the debris."""
    rng = np.random.default_rng(seed)
    c = Canvas(w, h, f=2, alpha=0.0)
    c.rect(2, 2, w - 2, h - 2, rgb=SCORCH * 1.6, height=0.6, alpha=1.0)
    for _ in range(int(w * h / 35)):
        rx, ru = rng.uniform(4, w - 4), rng.uniform(4, h - 4)
        r = rng.uniform(1.5, 4.5)
        tone = CONCRETE * rng.uniform(0.35, 0.8) if rng.random() < 0.7 else SCORCH * 1.2
        c.disc(rx, ru, r, rgb=tone, height=rng.uniform(1, 4.5), maxh=True)
    for _ in range(3):                                                              # wall stubs
        if rng.random() < 0.5:
            u = rng.uniform(6, h - 6)
            x0 = rng.uniform(3, w * 0.4)
            c.rect(x0, u, x0 + rng.uniform(14, w * 0.5), u + 2.5, rgb=CONCRETE * 0.7, height=rng.uniform(5, 9))
        else:
            x = rng.uniform(6, w - 6)
            u0 = rng.uniform(3, h * 0.4)
            c.rect(x, u0, x + 2.5, u0 + rng.uniform(14, h * 0.5), rgb=CONCRETE * 0.7, height=rng.uniform(5, 9))
    for _ in range(int(w * h / 260)):
        ex, eu = rng.uniform(5, w - 5), rng.uniform(5, h - 5)
        c.disc(ex, eu, rng.uniform(0.8, 1.6), emis=EMBER * rng.uniform(0.35, 0.8), rgb=SCORCH)
    arr = c.render(ambient=0.3)
    rng2 = np.random.default_rng(seed + 1)
    for _ in range(3):
        add_glow(arr, rng2.uniform(10, w - 10), rng2.uniform(10, h - 10), 7.0, MARS[3] * 0.45)
    arr[0, :, 3] = arr[-1, :, 3] = 0
    arr[:, 0, 3] = arr[:, -1, 3] = 0
    return finish(arr, 24)


def fire(w, h, n):
    """Flames seen from above (4 frames): a ragged burning patch (noise, not a star), a white-gold core
    flickering in it, orange flames leaning with the wind to the right, a dark sooty rim; one palette
    for the frames."""
    frames = []
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64)
    cx, cy = w / 2 - 3, h / 2 + 1
    base = raster.fbm(w, h, 8, 905, octaves=3, period=False)
    for k in range(n):
        flick = raster.fbm(w, h, 4, 910 + k, octaves=2, period=False)
        dx, dy = (xx - cx) / 11.0, (yy - cy) / 9.0
        lean = np.clip(dx, 0, None) * 0.35                     # the flames stretch to the right
        r = np.sqrt((dx - lean) ** 2 + dy * dy)
        heat = np.clip(1.15 - r + (base - 0.5) * 0.9 + (flick - 0.5) * 0.7, 0, 1)
        col = raster.ramp([(0.0, (40, 18, 14)), (0.3, tuple(MARS[1])), (0.5, tuple(MARS[3])), (0.72, tuple(MARS[4])),
                           (0.9, (255, 208, 128)), (1.0, (255, 240, 205))], heat)
        a = np.where(heat > 0.3, 1.0, np.where(heat > 0.12, 0.4, 0.0))
        frames.append(to_img(np.dstack([np.asarray(col) / 255, a])))
    return artkit.quantize_set(frames, 16)


def neon_sign(w, h, seed, word):
    """A rooftop neon sign seen from above (static): a dark frame, the letters as lit tubes in one
    of the district's colours (pink-red, amber, warm white; no teal), a soft stepped halo."""
    col = NEON[seed % len(NEON)]
    c = Canvas(w, h, f=1, alpha=0.0)
    c.rect(3, 3, w - 3, h - 3, rgb=NAVY[1] * 0.8, height=2.0, alpha=1.0)
    arr = c.render()
    tw = raster.text_width(word, 2)
    canvas = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    raster.draw_text(canvas, (w - tw) // 2, (h - 14) // 2, word, (255, 255, 255), scale=2)
    m = np.array(canvas)[..., 3] > 0
    halo = box_blur(m.astype(float), 2)
    arr[..., :3] += (col / 255)[None, None, :] * np.clip(halo * 0.8, 0, 0.35)[..., None] * (arr[..., 3:4] > 0)
    arr[m, :3] = np.minimum(1, col / 255 * 0.85)
    out = np.zeros_like(arr)
    out[..., :3] = arr[..., :3]
    ring = (halo > 0.05) & (arr[..., 3] == 0)
    out[..., 3] = np.where(arr[..., 3] > 0, 1.0, np.where(ring, 0.3, 0.0))
    out[ring, :3] = col / 255 * 0.45
    return finish(out, 16)


# --------------------------------------------------------------------------- low-air

def smoke_column(w, h, seed):
    """A smoke plume from above (static, below the play plane): a dense billowing core over its fire
    (orange glow underneath), spreading and thinning with the wind to the upper right."""
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64)
    sx, sy = w * 0.32, h * 0.72                       # the source (image rows down)
    n = raster.fbm(w, h, 24, seed, octaves=5, period=False)
    t = np.clip(((xx - sx) * 0.55 - (yy - sy) * 0.85) / (h * 0.75), 0, 1)     # along the plume
    cxl, cyl = sx + t * w * 0.42, sy - t * h * 0.62
    rad = 14 + t * 36
    d = np.hypot(xx - cxl, yy - cyl) / rad
    dens = np.clip((1.15 - d) * (1.1 - 0.5 * t) + (n - 0.5) * 0.9, 0, 1)
    dens *= np.clip(np.minimum.reduce([xx, yy, w - 1 - xx, h - 1 - yy]) / 12, 0, 1)
    sm = box_blur(dens, 2)
    gy, gx = np.gradient(sm * 10)
    lit = np.clip(0.45 + sm * 0.5 - (gx * 0.55 - gy * 0.6) * 1.2, 0, 1)
    rgb = raster.ramp([(0, (26, 20, 26)), (0.5, (60, 50, 54)), (1, (104, 92, 90))], lit)
    glow = np.exp(-((xx - sx) ** 2 + (yy - sy) ** 2) / 420.0) * np.clip(dens * 1.5, 0, 1)
    rgb = np.asarray(rgb) + (MARS[3] * 0.32)[None, None, :] * glow[..., None]
    a = artkit.ordered_dither(np.clip(dens * 1.3, 0, 1), 5) * 0.86
    img = to_img(np.dstack([np.clip(rgb / 255, 0, 1), a]))
    return artkit.quantize_set([img], 16)[0]


# --------------------------------------------------------------------------- towers

FOOTPRINTS = [(56, 56), (68, 64), (84, 72)]
STYLES = {  # style -> (heights, walls by height, shade)
    "a": ((0.35, 0.6, 0.85, 1.1, 1.35), None, None),
    "b": ((0.35, 0.6, 0.85, 1.1, 1.35), None, None),
    "burnt": ((0.6, 0.85), "wall-burnt", 0.45),
    "neon": ((0.85, 1.1, 1.35), None, 0.55),
    "neon-b": ((0.85, 1.1, 1.35), None, 0.55),
}
WALLS = {  # id -> (width, storeys, storey px, style)
    "wall-low": (32, 3, 6, "city"), "wall-mid": (32, 7, 6, "city"), "wall-tall": (32, 12, 6, "city"),
    "wall-burnt": (32, 7, 6, "burnt"), "wall-neon-mid": (32, 7, 6, "neon"), "wall-neon-tall": (32, 12, 6, "neon"),
    "wall-arcology": (40, 22, 5, "arcology"),
}
ARCOLOGY = {"foot": (200, 180), "height": 1.5}


def wall_for(style, h):
    if style == "burnt":
        return "wall-burnt"
    if style.startswith("neon"):
        return "wall-neon-mid" if h < 1.0 else "wall-neon-tall"
    return "wall-low" if h < 0.5 else "wall-mid" if h < 1.0 else "wall-tall"


def tower_id(foot, h, style):
    return f"tower-{foot[0]}x{foot[1]}-h{round(h * 100):03d}-{style}"


def roof_size(foot, h):
    k = tower_scale(h)
    return java_round(foot[0] * k), java_round(foot[1] * k)


def tower_roof(foot, h, style):
    """A tower's roof at its drawn size (the footprint x k): a slab with a parapet lit from the top left,
    and by height and style AC units, a stair hut, water tanks, a garden, a helipad, an antenna and a
    red beacon (the tall ones); burnt roofs blackened, holed and ember-lit; neon roofs with signs."""
    w, hh = roof_size(foot, h)
    k = tower_scale(h)
    rng = np.random.default_rng(int(h * 100) * 31 + foot[0] * 7 + foot[1] + sum(map(ord, style)))
    c = Canvas(w, hh, f=4, alpha=1.0)
    tone = NAVY[2] * (1.18 if style != "burnt" else 0.62) * rng.uniform(0.92, 1.08)
    par = 2.2 * k
    c.rect(0, 0, w, hh, rgb=tone * 1.1, height=2.0)
    c.rect(par, par, w - par, hh - par, rgb=tone, height=0.0)
    s = k                                                                       # detail scale

    def box(x, u, bw, bd, ht, col):
        c.rect(x, u, x + bw * s, u + bd * s, rgb=col, height=ht)
    inner = (par + 3, par + 3, w - par - 3, hh - par - 3)
    ix0, iu0, ix1, iu1 = inner
    if style == "burnt":
        for _ in range(4):
            hx, hu = rng.uniform(ix0 + 6, ix1 - 6), rng.uniform(iu0 + 6, iu1 - 6)
            c.disc(hx, hu, rng.uniform(4, 9) * s, rgb=SCORCH * 0.6, height=-2.0)
            c.disc(hx + 1, hu - 1, 1.5 * s, emis=EMBER * 0.5, rgb=SCORCH)
        for _ in range(int(w * hh / 40)):
            c.disc(rng.uniform(0, w), rng.uniform(0, hh), rng.uniform(1, 2.5), rgb=SCORCH * rng.uniform(0.8, 1.6),
                   height=rng.uniform(0, 1.5), maxh=True)
        gap = rng.uniform(0.2, 0.7)
        c.rect(w * gap, hh - par, w * gap + 14, hh, rgb=SCORCH, height=0.0)      # a broken parapet
        return finish(c.render(ambient=0.32), 24)
    box(ix0 + rng.uniform(0, 8), iu1 - 12 * s, 10, 9, 4.0, NAVY[2] * 0.95)      # stair hut
    for _ in range(int(rng.integers(1, 4))):
        bx, bu = rng.uniform(ix0, ix1 - 8 * s), rng.uniform(iu0, iu1 - 7 * s)
        box(bx, bu, rng.uniform(5, 8), rng.uniform(4, 7), rng.uniform(1.5, 2.8), NAVY[2] * 1.05)
    if style == "a" and h < 1.0 and rng.random() < 0.6:                          # a rooftop garden
        gx0, gu0 = ix0 + 2, iu0 + 2
        gx1, gu1 = gx0 + (ix1 - ix0) * rng.uniform(0.4, 0.7), gu0 + (iu1 - iu0) * 0.45
        c.rect(gx0, gu0, gx1, gu1, rgb=GREEN[2] * 0.45, height=0.6)
        for _ in range(int((gx1 - gx0) * (gu1 - gu0) / 22)):
            c.disc(rng.uniform(gx0 + 2, gx1 - 2), rng.uniform(gu0 + 2, gu1 - 2), rng.uniform(1.2, 2.4) * s,
                   rgb=GREEN[3] * 0.52, height=2.4)
    if style in ("a", "b") and h < 1.0 and rng.random() < 0.6:                   # water tanks
        for _ in range(int(rng.integers(1, 3))):
            tx, tu = rng.uniform(ix0 + 5 * s, ix1 - 5 * s), rng.uniform(iu0 + 5 * s, iu1 - 5 * s)
            c.disc(tx, tu, 3.4 * s, rgb=CONCRETE * 1.25, height=5.0)
            c.disc(tx - s, tu + s, 1.4 * s, rgb=CONCRETE * 1.55, height=5.2)
    if style == "b" and h >= 1.0:                                                 # a helipad
        r = min(ix1 - ix0, iu1 - iu0) * 0.34
        px, pu = (ix0 + ix1) / 2 + rng.uniform(-3, 3), (iu0 + iu1) / 2 - 2
        c.disc(px, pu, r, rgb=ROAD * 1.3, height=1.2)
        c.ring(px, pu, r - 1.5 * s, 1.0 * s, rgb=JOV[4] * 0.55, height=1.2)
        c.rect(px - 0.6 * s, pu - r * 0.4, px + 0.6 * s, pu + r * 0.4, rgb=JOV[4] * 0.5, height=1.2)
    if style == "neon":
        col = NEON[int(rng.integers(len(NEON)))]
        sx, su = ix0 + 2, iu0 + 2
        c.rect(sx, su, ix1 - 2, su + 6 * s, rgb=NAVY[1], height=2.4)
        for kk in range(int((ix1 - ix0 - 6) / (5 * s))):
            c.rect(sx + 2 + kk * 5 * s, su + 1.5 * s, sx + 2 + kk * 5 * s + 3 * s, su + 4.5 * s, rgb=col * 0.35,
                   emis=col * 0.5, height=2.6)
    if style == "neon-b":                                                         # a vertical sign, a lit terrace
        col = NEON[int(rng.integers(len(NEON)))]
        sx = ix0 + 2
        c.rect(sx, iu0 + 2, sx + 6 * s, iu1 - 2, rgb=NAVY[1], height=2.4)
        for kk in range(int((iu1 - iu0 - 6) / (5 * s))):
            c.rect(sx + 1.5 * s, iu0 + 4 + kk * 5 * s, sx + 4.5 * s, iu0 + 4 + kk * 5 * s + 3 * s, rgb=col * 0.35,
                   emis=col * 0.5, height=2.6)
        tx0, tu0 = (ix0 + ix1) / 2, iu0 + 4
        for kx in range(4):
            for ku in range(3):
                c.disc(tx0 + kx * 4 * s, tu0 + ku * 4 * s, 0.7 * s, emis=WARM_WIN * 0.7, rgb=NAVY[2], height=0.4)
    beacon = h >= 1.0
    if h >= 0.85:                                                                 # antenna mast
        ax, au = ix1 - 5 * s, iu1 - 5 * s
        c.disc(ax, au, 1.4 * s, rgb=CONCRETE * 1.5, height=10.0)
    arr = c.render()
    if beacon:
        bx, bu = int(ix1 - 5 * s), int(iu1 - 5 * s)
        add_glow(arr, bx, bu, 2.2 * s, np.array([255, 50, 60]) * 0.5)
        arr[hh - 1 - bu, bx, :3] = [1.0, 0.2, 0.24]
    if style == "neon":
        add_glow(arr, (ix0 + ix1) / 2, iu0 + 4 * s, 6 * s, NEON[0] * 0.25)
    if style == "neon-b":
        add_glow(arr, ix0 + 4 * s, (iu0 + iu1) / 2, 6 * s, NEON[1] * 0.22)
    return finish(arr, 28)


def wall_texture(w, storeys, px, style):
    """A wall texture: columns along the wall (tiling every ``w``), rows from the foot (the bottom row)
    to the roof's edge: a lit lobby at the foot, storey slabs, 2 x 2 windows every 4 px, few lit
    (warm or cool), a parapet cap at the top; burnt: blackened, windows blown out or fire-lit;
    neon: more lit windows and vertical neon strips; the arcology: dense lit habitation, creep on its
    upper floors."""
    h = storeys * px
    rng = np.random.default_rng(storeys * 13 + w + sum(map(ord, style)))
    base = NAVY[1] * 1.25
    if style == "burnt":
        base = NAVY[1] * 0.75
    rgb = np.empty((h, w, 3))
    rgb[:] = base
    lit_share = {"city": 0.16, "burnt": 0.08, "neon": 0.3, "arcology": 0.34}[style]
    for s in range(storeys):
        top = h - (s + 1) * px                     # image rows count from the top
        rgb[top, :] = base * 1.18                  # the slab line
        for col in range(0, w, 4):
            lit = rng.random() < lit_share
            if style == "burnt":
                c = MARS[3] * rng.uniform(0.5, 0.9) if lit else SCORCH * 0.7
            elif lit:
                c = (WARM_WIN if rng.random() < 0.7 else COOL_WIN) * rng.uniform(0.75, 1.0)
            else:
                c = NAVY[0] * 1.1
            r0 = top + (px - 2) // 2
            rgb[r0:r0 + 2, col + 1:col + 3] = c
    rgb[h - 2:, :] = base * 0.8
    if style != "burnt":
        rgb[h - 2:h - 1, 1:w - 1:3] = SODIUM * 0.9 + base * 0.3                      # the lobby's glow
    rgb[0:2, :] = NAVY[2] * 1.15                                                       # parapet cap
    if style == "burnt":
        for _ in range(5):                                                             # scorch streaks up
            x = int(rng.integers(w))
            y0 = int(rng.integers(h // 3, h))
            for y in range(y0 - int(rng.integers(6, h // 2)), y0):
                if 0 <= y < h:
                    rgb[y, x] = rgb[y, x] * 0.35 + SCORCH * 0.65
                    rgb[y, (x + 1) % w] = rgb[y, (x + 1) % w] * 0.6 + SCORCH * 0.4
    if style == "neon":
        for x in (int(rng.integers(w)),):
            rgb[2:h - 3, x] = NEON[int(rng.integers(len(NEON)))] * 0.7
    if style == "arcology":
        creep = int(h * 0.3)
        nz = raster.value_noise(w, creep, 4, 977, period=False)
        for y in range(creep):
            share = 1 - y / creep
            sel = nz[y] < share * 0.9
            rgb[y, sel] = CHITIN[2] * 0.7 + CHITIN[3] * 0.3 * nz[y, sel, None]
        for _ in range(8):
            x, y = int(rng.integers(w)), int(rng.integers(2, creep // 2))
            rgb[y, x] = VGLOW[4] * 0.7
    img = Image.fromarray(np.clip(rgb, 0, 255).astype(np.uint8), "RGB").convert("RGBA")
    return artkit.quantize_set([img], 24)[0]


def arcology_roof(w, hh):
    """The Ndidi Arcology's roof (h 1.5): terraced levels, dome gardens, a crown ring and a spire, its
    upper-left half overrun by the creep: violet chitin with glowing pods (the lead into Level 09)."""
    k = tower_scale(ARCOLOGY["height"])
    c = Canvas(w, hh, f=2, alpha=1.0)
    tone = NAVY[2] * 1.2
    c.rect(0, 0, w, hh, rgb=tone * 1.1, height=3.0)
    for step, inset in enumerate((6, 26, 52)):
        c.rect(inset * k, inset * k, w - inset * k, hh - inset * k, rgb=tone * (1.0 + 0.05 * step), height=3.0 + step * 5)
    cx, cu = w / 2, hh / 2
    c.ring(cx, cu, 34 * k, 3 * k, rgb=CONCRETE * 1.5, height=19.0)
    for dx, du in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
        c.disc(cx + dx * 60 * k, cu + du * 50 * k, 9 * k, rgb=GREEN[2] * 0.5, height=12.0)
        c.disc(cx + dx * 60 * k - 2, cu + du * 50 * k + 2, 4 * k, rgb=GREEN[3] * 0.5, height=14.0)
    c.disc(cx, cu, 8 * k, rgb=CONCRETE * 1.6, height=28.0)
    rng = np.random.default_rng(991)
    hh2, ww2 = c.hm.shape
    nz = raster.fbm(ww2, hh2, 24, 993, octaves=4, period=False)
    yy, xx = np.mgrid[0:hh2, 0:ww2].astype(np.float64)
    reach = (xx / ww2 * 0.8 + yy / hh2 * 0.8)
    creep = (nz * 0.9 + (1 - reach) * 0.9) > 0.98
    c.alb[creep] = CHITIN[1] * 0.7 + NAVY[1] * 0.5
    c.hm[creep] += 2.0 + 3.0 * nz[creep]
    for _ in range(26):
        px, pu = rng.uniform(4, w * 0.6), rng.uniform(hh * 0.4, hh - 4)
        if px / w + (hh - pu) / hh < 0.8:
            c.disc(px, pu, rng.uniform(1.5, 3.5), rgb=CHITIN[2], emis=VGLOW[4] * 0.3, lift=2.0)
    for _ in range(24):
        a = rng.uniform(0, 2 * math.pi)
        c.disc(cx + math.cos(a) * 40 * k, cu + math.sin(a) * 40 * k, 1.0, emis=WARM_WIN * 0.6, height=20.0)
    arr = c.render()
    add_glow(arr, cx, cu, 4.0, np.array([255, 60, 70]) * 0.6)
    arr[hh - 1 - int(cu), int(cx), :3] = [1.0, 0.25, 0.3]
    return finish(arr, 32)


# --------------------------------------------------------------------------- the layout

TILE_SETS = {   # id -> (layer, height, drift, note)
    "gulf": ("deep", 480, None, "the night sky's glow on the Gulf: seen only in the harbour's water"),
    "harbour": ("ground", TILE_H, None, "1: water (translucent), piers, the quay, container yards"),
    "avenues": ("ground", TILE_H, None, "2: two tree-lined avenues (x 112, 252) as the traffic lanes, low roofs, parks"),
    "rooftops": ("ground", TILE_H, None, "3-4: the boulevard (x 170-290), side streets, blocks of low roofs, gardens, tanks"),
    "third-mainland": ("ground", TILE_H, None, "5: the elevated Third Mainland highway (x 170-310), neon-lit low roofs"),
    "fog": ("low-air", 960, None, "light: r03's amber-lit fog banks"),
    "smoke": ("low-air", 960, 10, "medium: smoke banks, lit orange from below"),
    "smoke-heavy": ("low-air", 960, 12, "heavy: the smoke wall"),
    "mist": ("high-air", 960, None, "clear and light: thin mist (additive)"),
    "ash": ("high-air", 960, None, "medium and heavy: ash (additive)"),
}
SECTION_TILES = [["gulf", "harbour"], ["gulf", "avenues"], ["gulf", "rooftops"], ["gulf", "rooftops"],
                 ["gulf", "third-mainland"]]

PIECES = {      # id -> (layer, (w, h), extra spec, mid-size, note)
    "cross-highway": ("ground", (480, 92), {}, False, "an elevated cross highway; one over every section seam"),
    "quay-crane": ("ground", (170, 64), {}, False, "a gantry crane on the quay, its boom over the water"),
    "container-ship": ("ground", (100, 290), {}, True, "moored at a pier, bow up"),
    "container-yard": ("ground", (196, 260), {}, False, "a container yard over a block at the harbour's edge"),
    "burnt-block-a": ("ground", (72, 82), {}, False, "a collapsed block (smoke district)"),
    "burnt-block-b": ("ground", (76, 82), {}, False, None),
    "burnt-block-c": ("ground", (94, 82), {}, False, None),
    "fire": ("ground", (40, 40), {"frames": 4, "fps": 8}, False, "flames on the burnt blocks"),
    "neon-sign-a": ("ground", (72, 30), {}, False, "rooftop signs in Neon Heights (static)"),
    "neon-sign-b": ("ground", (60, 30), {}, False, None),
    "neon-sign-c": ("ground", (60, 30), {}, False, None),
    "smoke-column-a": ("low-air", (150, 200), {}, False, "smoke plumes over the fires (static)"),
    "smoke-column-b": ("low-air", (130, 180), {}, False, None),
    "smoke-column-c": ("low-air", (170, 210), {}, False, None),
    "aircar-sedan": ("low-air", (24, 24), {"headings": 16}, False,
                     "civilian wedge sedan (round 30 a), slate; 16 headings, 0 nose up, clockwise"),
    "aircar-van": ("low-air", (28, 28), {"headings": 16}, False, "civilian box van (round 30 a), beige"),
    "gunship": ("low-air", (44, 44), {"headings": 16}, False, "CDF gunship (round 30 a), twin ducted fans"),
}
for _id, (_x, _cy, _w, _h, _units) in STRUCTURES.items():
    PIECES[_id] = ("ground", (_w, _h), {}, _id != "billboard-roof",
                   {"landing-pad": "the t=44 turret nest's landing pad on a low roof",
                    "roof-nest": "the t=88 turret nest's low roof by the boulevard",
                    "billboard-roof": "the billboard's low roof (the billboard is its ground target's sprite)",
                    "parking-deck": "the t=124 turret nest's parking deck",
                    "overpass": "the collapsed overpass under the mortars (diagonal through their line)"}[_id])
for _id, _spec in WALK_ROOFS.items():
    PIECES[_id] = ("ground", (_spec["w"], _spec["h"]), {}, False,
                   f"the t={_spec['t']:g} Creepers' low roof and ramp" + (" (scorched)" if _spec["burnt"] else ""))


def fixed_placements():
    """(piece, t, x, extra, comment): everything but the towers and the traffic."""
    out = []
    note = {1: "1. On the Wing: the harbour (cranes on the quay, a ship at the pier)",
            2: "2. Traffic Lanes", 3: "3. Rooftop Crawl", 4: "4. Smoke District", 5: "5. Neon Heights"}
    seams = [seam_pos(s) for s in range(1, len(SECTIONS))]
    out.append(("quay-crane", round(t_for_centre("ground", 330), 2), 173, None, note[1]))
    out.append(("container-ship", round(t_for_centre("ground", 911), 2), 120, None, None))
    out.append(("quay-crane", round(t_for_centre("ground", 860), 2), 173, None, None))
    out.append(("quay-crane", round(t_for_centre("ground", 1420), 2), 173, None, None))
    for i, pos in enumerate(seams):
        if SECTION_GROUND[i] == SECTION_GROUND[i + 1]:
            continue
        out.append(("cross-highway", round(t_for_centre("ground", pos), 3), 240, None,
                    f"over the seam of sections {i + 1} and {i + 2} (ground position {pos:g}), where the ground tiles change"))
    out.append(("container-yard", round(t_for_centre("ground", seams[0] + 230), 2), 372, None,
                note[2] + ": the harbour's last yards"))
    out.append(("cross-highway", round(t_for_centre("ground", 4416), 3), 240, None, "the elevated highways"))
    out.append(("cross-highway", round(t_for_centre("ground", 7872), 3), 240, None, None))
    for pid, (x, cy, w, h, _) in STRUCTURES.items():
        out.append((pid, round(t_for_centre("ground", cy), 3), x, None, None))
    for pid, spec in WALK_ROOFS.items():
        extra = {"repeat": {"count": spec["n"], "every": CONVOY}} if spec["n"] > 1 else None
        out.append((pid, round(t_for_centre("ground", spec["centre"]), 3), round(spec["x"]), extra, None))
    out.append(("arcology", round(t_for_centre("ground", arcology_centre()), 3), 240, None,
                note[5] + ": the Ndidi Arcology at the level's end (a tower, h 1.5)"))
    return out


def arcology_centre():
    """The arcology's footprint centre: it rises at the top edge as the level ends."""
    return scroll_at(END) + SCREEN + ARCOLOGY["foot"][1] / 2 - 20


def piece_rects(placed_list, specs):
    """Ground rects (x0, p0, x1, p1) of placed flat pieces (repeats written out)."""
    out = []
    for piece, t, x, extra, _ in placed_list:
        w, h = specs[piece][1]
        n, every = 1, 0
        if extra and "repeat" in extra:
            n, every = extra["repeat"]["count"], extra["repeat"]["every"]
        for i in range(n):
            c = scroll_at(t + i * every) + MID
            out.append((piece, x - w / 2, c - h / 2, x + w / 2, c + h / 2))
    return out


def zones():
    """What the towers must keep clear of: the targets' structures, the walk roofs, every walker
    unit's path while it can be on screen, every ground target's unit (ground rects, with margins)."""
    out = []
    for piece, x0, p0, x1, p1 in piece_rects([p for p in fixed_placements() if p[0] in STRUCTURES or p[0] in WALK_ROOFS],
                                             PIECES):
        out.append((x0 - 6, p0 - 6, x1 + 6, p1 + 6))
    for path in creeper_corridors():
        walked = 0.0
        for (ax, ap), (bx, bp) in zip(path, path[1:]):
            seg = math.hypot(bx - ax, bp - ap)
            n = max(1, int(seg / 30))
            for k in range(n):
                if walked + seg * k / n > 330:
                    break
                px, pp = ax + (bx - ax) * k / n, ap + (bp - ap) * k / n
                out.append((px - 40, pp - 40, px + 40, pp + 40))
            walked += seg
    for _, x, pos, w, h in target_units():
        out.append((x - w / 2 - 14, pos - h / 2 - 14, x + w / 2 + 14, pos + h / 2 + 14))
    return out


def hull_clear(x, centre, foot, h, zone_list, scrolls):
    """Whether a tower's projected walls and roof (their bounding box) keep clear of every zone at the
    sampled ground scrolls while both are on screen."""
    fw, fh = foot
    k = tower_scale(h)
    fx0, fx1 = x - fw / 2, x + fw / 2
    hx0 = min(fx0, CX + (fx0 - CX) * k)
    hx1 = max(fx1, CX + (fx1 - CX) * k)
    for zx0, zp0, zx1, zp1 in zone_list:
        if zx1 < hx0 or zx0 > hx1 or zp0 > centre + 420 or zp1 < centre - 420:
            continue
        y0 = centre - fh / 2 - scrolls
        y1 = centre + fh / 2 - scrolls
        hy0 = np.minimum(y0, CY + (y0 - CY) * k)
        hy1 = np.maximum(y1, CY + (y1 - CY) * k)
        zy0, zy1 = zp0 - scrolls, zp1 - scrolls
        on = (zy1 > 0) & (zy0 < SCREEN) & (hy1 > 0) & (hy0 < SCREEN)
        if np.any(on & (hy1 > zy0) & (hy0 < zy1)):
            return False
    return True


_TOWERS = None


def tower_placements():
    """Towers on the city lots, section by section, each as tall as its spot allows (or none)."""
    global _TOWERS
    if _TOWERS is None:
        _TOWERS = place_towers()
    return _TOWERS


def place_towers():
    rng = np.random.default_rng(3008)
    zl = zones()
    flats = [r for r in piece_rects([p for p in fixed_placements() if p[0] != "arcology"], PIECES)]
    out = []
    sec_starts = [seam_pos(s) for s in range(len(SECTIONS))]
    sec_starts[0] = 0.0
    end_pos = scroll_at(OUTRO_END) + SCREEN + 10
    arc_c = arcology_centre()
    afw, afh = ARCOLOGY["foot"]
    for s in range(1, len(SECTIONS)):
        p0 = sec_starts[s]
        p1 = sec_starts[s + 1] if s + 1 < len(SECTIONS) else end_pos
        plan = SECTION_GROUND[s]
        for x0, x1, q0, q1 in lots(plan, p0 + 30, p1 - 30):
            cx, cp = (x0 + x1) / 2, (q0 + q1) / 2
            if any(not (r[3] + 4 < x0 or r[1] - 4 > x1 or r[4] + 4 < q0 or r[2] - 4 > q1) for r in flats):
                continue
            if s == len(SECTIONS) - 1 and abs(cp - arc_c) < afh / 2 + 70 and abs(cx - 240) < afw / 2 + 70:
                continue
            progress = (cp - p0) / max(1, p1 - p0)
            if s == 1:
                share, top = 0.12 + 0.45 * progress, 0.6 + 0.75 * progress
                style = "a" if rng.random() < 0.6 else "b"
            elif s == 2:
                share, top, style = 0.72, 1.35, ("a" if rng.random() < 0.55 else "b")
            elif s == 3:
                share, top, style = 0.42, 0.85, "burnt"
            else:
                share, top, style = 0.55, 1.35, ("neon" if rng.random() < 0.5 else "neon-b")
            if rng.random() > share:
                continue
            fits = [f for f in FOOTPRINTS if f[0] <= x1 - x0 - 6 and f[1] <= q1 - q0 - 8]
            if not fits:
                continue
            foot = fits[-1] if rng.random() < 0.75 or len(fits) == 1 else fits[-2]
            heights = [hh for hh in STYLES[style][0] if hh <= top + 1e-9]
            if not heights:
                continue
            want = heights[min(len(heights) - 1, int(rng.uniform(0.3, 1.0) ** 1.2 * len(heights)))]
            t_mid = t_for_centre("ground", cp)
            scrolls = np.array([scroll_at(t) for t in np.arange(t_mid - 6, t_mid + 6, 1 / 15)])
            for hgt in sorted([hh for hh in heights if hh <= want], reverse=True):
                if hull_clear(cx, cp, foot, hgt, zl, scrolls):
                    out.append((tower_id(foot, hgt, style), round(t_mid, 3), cx, None, None))
                    break
    return out


def burnt_placements():
    """Section 4: collapsed blocks on lots without a tower or a structure, fires on some of them."""
    rng = np.random.default_rng(4004)
    towers = tower_placements()
    fixed = fixed_placements()
    rects = piece_rects([p for p in fixed if p[0] != "arcology"], PIECES)
    tower_spots = {(round(x), round(scroll_at(t) + MID)) for _, t, x, _, _ in towers}
    out, fires = [], []
    p0, p1 = seam_pos(3), seam_pos(4)
    for x0, x1, q0, q1 in lots("rooftops", p0 + 40, p1 - 60):
        cx, cp = (x0 + x1) / 2, (q0 + q1) / 2
        if (round(cx), round(cp)) in tower_spots:
            continue
        if any(not (r[3] + 2 < x0 or r[1] - 2 > x1 or r[4] + 2 < q0 or r[2] - 2 > q1) for r in rects):
            continue
        if rng.random() > 0.55:
            continue
        w = x1 - x0
        pid = "burnt-block-a" if w < 75 else "burnt-block-b" if w < 85 else "burnt-block-c"
        t = round(t_for_centre("ground", cp), 3)
        out.append((pid, t, cx, None, None))
        if rng.random() < 0.4:
            fires.append(("fire", round(t + rng.uniform(-0.12, 0.12), 3), round(cx + rng.uniform(-12, 12)), None, None))
    return out, fires


def lot_placements(section, pids, share, seed, note):
    """Static pieces on free lots of a section (neon signs)."""
    rng = np.random.default_rng(seed)
    towers = {(round(x), round(scroll_at(t) + MID)) for _, t, x, _, _ in tower_placements()}
    rects = piece_rects([p for p in fixed_placements()], {**PIECES, "arcology": ("ground", ARCOLOGY["foot"])})
    out = []
    p0 = seam_pos(section)
    p1 = seam_pos(section + 1) if section + 1 < len(SECTIONS) else scroll_at(OUTRO_END) + SCREEN
    for x0, x1, q0, q1 in lots(SECTION_GROUND[section], p0 + 40, p1 - 20):
        cx, cp = (x0 + x1) / 2, (q0 + q1) / 2
        if (round(cx), round(cp)) in towers or rng.random() > share:
            continue
        if any(not (r[3] < x0 or r[1] > x1 or r[4] < q0 or r[2] > q1) for r in rects):
            continue
        pid = pids[int(rng.integers(len(pids)))]
        if PIECES[pid][1][0] > x1 - x0 - 2:
            continue
        out.append((pid, round(t_for_centre("ground", cp + rng.uniform(-18, 18)), 3), cx, None, note if not out else None))
    return out


def plume_overlap(pid, t_placed, x):
    """How much the smoke plume ``pid`` placed at (t_placed, x) on low-air covers the ground units'
    places while it is on screen: the mean of ground_clear (husk tracks at half weight) inside its
    dense middle (the middle 70 % of its box), over its time on screen."""
    w, h = PIECES[pid][1]
    centre = scroll_at(t_placed) * FACTORS["low-air"] + MID
    total, n = 0.0, 0
    for t in np.arange(t_placed - 4, t_placed + 4, 0.2):
        y = centre - scroll_at(t) * FACTORS["low-air"]                  # its centre, px up from the bottom
        if y + h / 2 < 0 or y - h / 2 > SCREEN:
            continue
        r0, r1 = int(max(0, SCREEN - (y + 0.35 * h))), int(min(SCREEN, SCREEN - (y - 0.35 * h)))
        c0, c1 = int(max(0, x - 0.35 * w)), int(min(W, x + 0.35 * w))
        if r1 > r0 and c1 > c0:
            total += ground_clear(t, 0.5)[r0:r1, c0:c1].sum() / ((0.7 * h) * (0.7 * w))
        n += 1
    return total / max(1, n)


def smoke_placements():
    """Section 4 and the start of 5: smoke plumes on low-air over the fires, each at the one of
    four columns (the random draw's jitter kept) that veils the ground units least (plume_overlap)."""
    rng = np.random.default_rng(5005)
    out = []
    t = STARTS[3] + 3.0
    k = 0
    while t < STARTS[4] + 14:
        pid = ("smoke-column-a", "smoke-column-b", "smoke-column-c")[k % 3]
        base = [90, 150, 330, 390]
        pick = rng.choice(base)
        jitter = rng.uniform(-20, 20)
        tp = round(t, 2)
        scores = {bx: plume_overlap(pid, tp, bx + jitter) for bx in base}
        best = min(scores.values())
        x = pick if scores[pick] <= best + 0.01 else min(scores, key=scores.get)
        out.append((pid, tp, round(float(x + jitter)), None, "4. smoke columns on low-air (static)" if k == 0 else None))
        t += rng.uniform(4.5, 7.5)
        k += 1
    return out


def stream(t0, t1, x, down, every, own, kind="aircar-sedan", legs=None, times=None):
    """A traffic stream: a placement on low-air with a path and a repeat; it enters at the top edge
    at ``t0``, moves ``own`` px/s down (or up) on its own, repeats every ``every`` s until ``t1``
    (or ``times`` copies). ``legs`` [(heading index of 16, s), ...] instead flies those headings at
    ``own`` px/s, the last one until it has left the screen (a turning or slanting craft)."""
    w, h = PIECES[kind][1]
    f = FACTORS["low-air"]
    centre = scroll_at(t0) * f + SCREEN + h / 2 + 1
    t_place = t_at((centre - MID) / f)
    count = times or max(1, int((t1 - t0) / every) + 1)
    if legs is None:
        rate = SPEED * f + (own if down else -own)
        span = (SCREEN + h + 4) / rate + 0.3
        dy = round(-own * span if down else own * span, 1)
        entry = {"path": [[round(t0, 2), 0, 0], [round(t0 + span, 2), 0, dy]]}
    else:
        points, t, px, py = [[round(t0, 2), 0, 0]], t0, 0.0, 0.0
        for k, (hd, secs) in enumerate(legs):
            a = math.radians(hd * 22.5)
            vx, vy = own * math.sin(a), own * math.cos(a)
            if k == len(legs) - 1:            # until the top edge is below the bottom, plus 0.3 s
                secs = (SCREEN + h + 4 + py) / (SPEED * f - vy) + 0.3 - (t - t0)
            t, px, py = t + secs, px + vx * secs, py + vy * secs
            points.append([round(t, 2), round(px, 1), round(py, 1)])
        entry = {"path": points}
    if count > 1:
        entry["repeat"] = {"count": count, "every": every}
    return (kind, round(t_place, 3), x, entry, None)


def screen_window(st):
    """(first, last) s a stream's first craft is on screen, from its path and size (a margin of 0.1 s)."""
    kind, _, _, entry, _ = st
    path = entry["path"]
    return path[0][0] - 0.1, path[-1][0] - 0.2


def split_lane(t0, t1, x, every, own, kinds_at):
    """A lane's stream cut into runs of one piece each: ``kinds_at(entry t, exit t)`` names the piece
    of each craft (consecutive craft of one piece share a placement with a repeat)."""
    count = max(1, int((t1 - t0) / every) + 1)
    probe = stream(t0, t0, x, True, every, own, "aircar-van")
    _, last = screen_window(probe)
    stay = last - t0
    kinds = [kinds_at(t0 + i * every, t0 + i * every + stay) for i in range(count)]
    out, i = [], 0
    while i < count:
        j = i
        while j + 1 < count and kinds[j + 1] == kinds[i]:
            j += 1
        out.append(stream(t0 + i * every, None, x, True, every, own, kinds[i], times=j - i + 1))
        i = j + 1
    return out


def traffic_placements():
    """D2 = a: the traffic is scenery. Section 1: CDF gunships heading home (south and south-west,
    down the screen). Section 2: sedans fleeing south over the west avenue's fast lane, vans in its
    slow lane, a thinner stream of sedans north over the east one, a gunship pair bearing
    south-south-west; section 3: the last sedans and vans thinning out over the boulevard. Motion
    budget (two moving pieces at a time): the van lane carries sedans while the pair is on screen."""
    a1, a2 = PLANS["avenues"]["roads"][0], PLANS["avenues"]["roads"][1]
    x1, x2 = (a1[0] + a1[1]) / 2, (a2[0] + a2[1]) / 2
    pair = [stream(36.0, 36.0, 420, True, 1, 60, "gunship", legs=[(9, 1.0)]),
            stream(37.2, 37.2, 446, True, 1, 60, "gunship", legs=[(9, 1.0)])]
    busy = [screen_window(p) for p in pair]

    def van_or_sedan(enter, leave):
        return "aircar-sedan" if any(enter < b and leave > a for a, b in busy) else "aircar-van"

    out = [
        stream(0.5, 0.5, 330, True, 1, 60, "gunship"),
        stream(4.0, 4.0, 300, True, 1, 60, "gunship", legs=[(9, 1.0)]),
        stream(7.5, 7.5, 380, True, 1, 60, "gunship", legs=[(8, 0.8), (9, 0.8), (10, 1.0)]),
        stream(12.6, 60.0, x1 - 6, True, 0.9, 110),
        *split_lane(13.2, 60.0, x1 + 6, 1.7, 110, van_or_sedan),
        stream(14.0, 58.0, x2 + 6, False, 2.6, 60),
        *pair,
        stream(66.0, 101.0, 216, True, 4.8, 110),
        stream(68.4, 104.0, 216, True, 4.8, 110, "aircar-van"),
    ]
    out[0] = out[0][:4] + ("1-3: traffic on low-air (D2 = a: scenery, shots pass through), round 30 variant A",)
    return out


def placements():
    fixed = fixed_placements()
    burnt, fires = burnt_placements()
    neon = lot_placements(4, ["neon-sign-a", "neon-sign-b", "neon-sign-c"], 0.3, 6006,
                          "5. rooftop neon signs on the free lots (static)")
    towers = tower_placements()
    return {"fixed": fixed, "towers": towers, "burnt": burnt, "fires": fires, "neon": neon,
            "smoke": smoke_placements(), "traffic": traffic_placements()}


def tower_pieces(towers):
    out = {}
    for pid, *_ in towers:
        _, fp, hpart, style = pid.split("-", 3)
        fw, fh = map(int, fp.split("x"))
        h = int(hpart[1:]) / 100
        spec = {"height": h, "wall": wall_for(style, h)}
        shade = STYLES[style][2]
        if shade is not None:
            spec["shade"] = shade
        out[pid] = ("ground", (fw, fh), {"tower": spec}, False, None)
    return out


def all_pieces(pl=None):
    pl = pl or placements()
    pieces = dict(PIECES)
    pieces.update(sorted(tower_pieces(pl["towers"]).items()))
    pieces["arcology"] = ("ground", ARCOLOGY["foot"], {"tower": {"height": ARCOLOGY["height"], "wall": "wall-arcology",
                                                                 "shade": 0.5}}, True,
                          "the Ndidi Arcology (a tower, h 1.5), creep on its roof and upper floors")
    return pieces


def ordered_placements(pl):
    """Draw order on a layer: later over earlier. Flat structures first (cross highways over the
    tiles), then the walk roofs, the burnt blocks, the fires, the neon; towers are drawn by height
    by the game anyway; low-air: the smoke under the traffic."""
    ground = [p for p in pl["fixed"]] + pl["burnt"] + pl["fires"] + pl["neon"] + pl["towers"]
    low = pl["smoke"] + pl["traffic"]
    return ground + low


def block(pl=None):
    """The backdrop block as the data file holds it."""
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


def flow(d):
    """A YAML flow mapping in the data files' style."""
    def value(v):
        if isinstance(v, dict):
            return flow(v)
        if isinstance(v, (list, tuple)):
            return "[" + ", ".join(value(i) for i in v) + "]"
        if isinstance(v, bool):
            return "true" if v else "false"
        if isinstance(v, (float, np.floating)):
            v = float(v)
            if v == int(v):
                return str(int(v))
            return f"{v:g}" if abs(v) < 1000 else f"{v:.3f}".rstrip("0").rstrip(".")
        if isinstance(v, np.integer):
            return str(int(v))
        return str(v)
    return "{" + ", ".join(f"{k}: {value(v)}" for k, v in d.items()) + "}"


def write_proposal():
    pl = placements()
    b = block(pl)
    pieces = all_pieces(pl)
    lines = [
        "# Level 08 backdrop PROPOSAL (tools/art/backdrop_l08.py --proposal; M5 part B batch, round 30).",
        "# For the main agent to merge into data.yaml: `backdrop` replaces the level's block (which takes",
        "# Level 01's images, `images: level-01`), and the sections take the `tiles` below.",
        "#",
        "# sections[i].tiles (by section, in order):",
    ]
    for s, tiles in zip(SECTIONS, SECTION_TILES):
        lines.append(f"#   {s['name']}: [{', '.join(tiles)}]")
    lines += [
        "#",
        "# Section notes that no longer fit a top-down city (suggested rewording for the layers):",
        "#   1 deep: the night sky's glow on the Gulf, seen in the harbour's water; far: unused (the cranes",
        "#     stand on the quay, ground); low-air: CDF gunships heading home; fog banks.",
        "#   2 far: unused; ground: the first perspective towers rise along the avenues; low-air: the",
        "#     aircar streams (no searchlights: the motion budget is full with the sedans and the vans).",
        "#   3 far: unused (the avenue lamps are on the ground tiles).",
        "#   4 deep: unused (the fire glow is on the burnt blocks, the fires and the smoke's underside).",
        "#   5 far: the Ndidi Arcology is a ground tower (h 1.5) at the level's end, not a far piece.",
        "# The billboard itself is its ground target's sprite (assets/sprites, round 30 a/b); this block",
        "# only gives its low roof. The traffic is round 30's variant A (sedan, van, CDF gunship).",
        "# Gameplay structures (landing-pad, roof-nest, billboard-roof, parking-deck, overpass, walk-roof-N)",
        "# are sized and placed from the level's ground_targets and walker paths as they stood at --proposal:",
        "# re-run --proposal (and the images) after moving a target or a Creeper path.",
        "",
        "backdrop:",
        "  # Presentation only: Nova Lagos at night (tools/art/backdrop_l08.py, from the chosen parallax r03 B).",
        "  # Towers (D1 = a) are scenery drawn in true perspective; nothing the game plays with stands on one,",
        "  # and none leans over a Creeper path, a ground target or its structure while on screen. Motion",
        "  # budget: the gunships (1), the sedans and vans (2-3; sedans in the van lane while the CDF pair",
        "  # crosses section 2), the smoke banks' drift and the fires (4-5).",
        "  scroll_factors: " + flow(b["scroll_factors"]),
        f"  ramp: {RAMP}",
        f"  haze_colour: {HAZE_COLOUR}  # smoky violet-brown over the Gulf's glow",
        "  atmosphere:",
    ]
    notes = {"clear": "section 2: thin mist only", "light": "sections 1 and 3: the amber-lit fog banks",
             "medium": "sections 4-5: smoke banks, ash", "heavy": "the smoke wall before the Needler circle"}
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
    PROPOSAL.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"proposal: {PROPOSAL.relative_to(ROOT)} ({len(b['placed'])} placements, "
          f"{sum(1 for p in b['pieces'].values() if 'tower' in p)} tower pieces)")


def level_backdrop():
    """The level's backdrop block once it is Level 08's own, otherwise the proposal's."""
    own = DATA.get("backdrop") or {}
    if own and "images" not in own:
        return own, "data.yaml"
    if not PROPOSAL.exists():
        write_proposal()
    return yaml.safe_load(PROPOSAL.read_text(encoding="utf-8"))["backdrop"], PROPOSAL.name


# --------------------------------------------------------------------------- generators

GENERATORS = {
    "gulf": gulf, "harbour": lambda w, h: finish_tile(harbour_tile(), 32),
    "avenues": lambda w, h: finish_tile(city_tile("avenues", 821, "office", park_share=0.16), 32),
    "rooftops": lambda w, h: finish_tile(city_tile("rooftops", 823, "residential", park_share=0.05), 32),
    "third-mainland": lambda w, h: finish_tile(city_tile("third-mainland", 825, "neon"), 32),
    "fog": fog, "smoke": smoke, "smoke-heavy": smoke_heavy, "mist": mist, "ash": ash,
    "cross-highway": cross_highway, "quay-crane": quay_crane, "container-ship": container_ship,
    "container-yard": container_yard,
    "burnt-block-a": lambda w, h: burnt_block(w, h, 881), "burnt-block-b": lambda w, h: burnt_block(w, h, 883),
    "burnt-block-c": lambda w, h: burnt_block(w, h, 887),
    "fire": fire,
    "neon-sign-a": lambda w, h: neon_sign(w, h, 0, "SUYA"), "neon-sign-b": lambda w, h: neon_sign(w, h, 1, "ZOBO"),
    "neon-sign-c": lambda w, h: neon_sign(w, h, 2, "OKO"),
    "smoke-column-a": lambda w, h: smoke_column(w, h, 941), "smoke-column-b": lambda w, h: smoke_column(w, h, 943),
    "smoke-column-c": lambda w, h: smoke_column(w, h, 947),
    "aircar-sedan": lambda w, h, n: l08_traffic.frames("aircar-sedan", n),
    "aircar-van": lambda w, h, n: l08_traffic.frames("aircar-van", n),
    "gunship": lambda w, h, n: l08_traffic.frames("gunship", n),
    "landing-pad": lambda w, h: podium(w, h, "landing-pad"), "roof-nest": lambda w, h: podium(w, h, "roof-nest"),
    "billboard-roof": lambda w, h: podium(w, h, "billboard-roof"),
    "parking-deck": lambda w, h: podium(w, h, "parking-deck"), "overpass": overpass,
}


def generator(name, spec):
    if name in GENERATORS:
        return GENERATORS[name]
    if name.startswith("walk-roof-"):
        return lambda w, h: walk_roof(name)
    if name == "arcology":
        return arcology_roof
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
            size = roof_size(size, spec["tower"]["height"])
        out.append((name, size, spec.get("frames") or spec.get("headings"), spec))
    walls = sorted({spec["tower"]["wall"] for spec in backdrop["pieces"].values() if "tower" in spec})
    out += [(wall, None, None, {"wall": True}) for wall in walls]
    return [job for job in out if not wanted or job[0] in wanted]


def render(job):
    name, size, count, spec = job
    if spec and spec.get("wall"):
        w, storeys, px, style = WALLS[name]
        return [wall_texture(w, storeys, px, style)]
    fn = generator(name, spec or {})
    w, h = size
    images = fn(w, h, count) if count else [fn(w, h)]
    for img in images:
        if img.size != (w, h):
            raise ValueError(f"{name}: rendered {img.size}, the data file says {(w, h)}")
    return images


def write(job, images):
    name, _, count, _ = job
    save = artkit.save_png
    if count:
        for old in OUT.glob(f"{name}_*.png"):
            old.unlink()
        for i, img in enumerate(images):
            save(img, OUT / f"{name}_{i}.png", SOURCE)
    else:
        save(images[0], OUT / f"{name}.png", SOURCE)


def load(name, i=None):
    return Image.open(OUT / (f"{name}_{i}.png" if i is not None else f"{name}.png")).convert("RGBA")


# --------------------------------------------------------------------------- the game's draw

def along(path, t, k):
    if t <= path[0][0]:
        return path[0][k]
    for a, b in zip(path, path[1:]):
        if t < b[0]:
            return a[k] + (b[k] - a[k]) * (t - a[0]) / (b[0] - a[0])
    return path[-1][k]


def heading_at(path, t):
    i = 1
    while i < len(path) - 1 and t >= path[i][0]:
        i += 1
    a, b = path[i - 1], path[i]
    deg = math.degrees(math.atan2(b[1] - a[1], b[2] - a[2]))
    return deg + 360 if deg < 0 else deg


def expanded(backdrop):
    """Every placement with its repeats written out (BackdropData.placements)."""
    out = []
    for p in backdrop["placed"]:
        rep = p.get("repeat")
        n, every = (rep["count"], rep["every"]) if rep else (1, 0)
        for i in range(n):
            q = dict(p)
            q.pop("repeat", None)
            q["t"] = p["t"] + i * every
            if "path" in p:
                q["path"] = [[pt[0] + i * every, pt[1], pt[2]] for pt in p["path"]]
            out.append(q)
    return out


def piece_centre(backdrop, placed):
    layer = backdrop["pieces"][placed["piece"]]["layer"]
    return scroll_at(placed["t"]) * backdrop["scroll_factors"][layer] + MID


def piece_rect(backdrop, placed, t):
    """(left, bottom) on screen in px from the play field's bottom-left, as Backdrop draws it."""
    spec = backdrop["pieces"][placed["piece"]]
    w, h = spec["size"]
    f = backdrop["scroll_factors"][spec["layer"]]
    bottom = java_round(piece_centre(backdrop, placed) - h / 2)
    path = placed.get("path")
    y = bottom - java_round(scroll_at(t) * f) + (java_round(along(path, t, 2)) if path else 0)
    x = java_round(placed["x"] + (along(path, t, 1) if path else 0) - w / 2)
    return x, y, w, h


def tower_geometry(backdrop, placed, t):
    """(foot x0, y0, w, h; roof x0, y0, w, h; k) on screen, y up, as TowerProjection draws it."""
    spec = backdrop["pieces"][placed["piece"]]
    fw, fh = spec["size"]
    k = tower_scale(spec["tower"]["height"])
    rw, rh = java_round(fw * k), java_round(fh * k)
    centre = piece_centre(backdrop, placed)
    bottom = java_round(centre - fh / 2)
    scroll = scroll_at(t)
    fx = java_round(placed["x"] - fw / 2)
    fy = int(bottom - java_round(scroll))
    rx = java_round(CX + (placed["x"] - CX) * k - rw / 2)
    ry = java_round(CY + (centre - scroll - CY) * k - rh / 2)
    return (fx, fy, fw, fh), (rx, ry, rw, rh), k


def on_screen(backdrop, placed, t):
    spec = backdrop["pieces"][placed["piece"]]
    if "tower" in spec:
        (fx, fy, fw, fh), (rx, ry, rw, rh), _ = tower_geometry(backdrop, placed, t)
        left, right = min(fx, rx), max(fx + fw, rx + rw)
        low, high = min(fy, ry), max(fy + fh, ry + rh)
        return right > 0 and left < W and high > 0 and low < SCREEN
    x, y, w, h = piece_rect(backdrop, placed, t)
    return x + w > 0 and x < W and y + h > 0 and y < SCREEN


def image_index(backdrop, placed, t):
    spec = backdrop["pieces"][placed["piece"]]
    if spec.get("frames"):
        return int(math.floor(t * spec["fps"])) % spec["frames"]
    if spec.get("headings"):
        n = spec["headings"]
        return java_round(heading_at(placed["path"], t) * n / 360) % n
    return None


def stretches():
    out = []
    for i, s in enumerate(SECTIONS):
        start = STARTS[i]
        peak = s.get("peak")
        if peak:
            out += [(start, peak["from"], s["atmosphere"]), (peak["from"], peak["to"], peak["atmosphere"]),
                    (peak["to"], s["end"], s["atmosphere"])]
        else:
            out.append((start, s["end"], s["atmosphere"]))
    return out


def looks_at(backdrop, t):
    """Backdrop.blend: (from look, to look, weight)."""
    st = stretches()
    current = next((i for i, s in enumerate(st) if t < s[1]), len(st) - 1)
    a = b = backdrop["atmosphere"][st[current][2]]
    weight = 1.0
    for i in range(1, len(st)):
        progress = (t - st[i][0]) / backdrop["ramp"] + 0.5
        if 0 < progress < 1:
            a, b = backdrop["atmosphere"][st[i - 1][2]], backdrop["atmosphere"][st[i][2]]
            weight = progress * progress * (3 - 2 * progress)
    return a, b, weight


def seam(backdrop, layer, index):
    return -math.inf if index == 0 else scroll_at(STARTS[index]) * backdrop["scroll_factors"][layer] + SCREEN


_cache = {}


def cached(name, i=None):
    key = (name, i)
    if key not in _cache:
        _cache[key] = np.array(load(name, i)).astype(np.float64)
    return _cache[key]


def tile_rows(name, rows, shift=0):
    img = cached(name)
    hgt = img.shape[0]
    out = img[[hgt - 1 - (int(r) % hgt) for r in rows]]
    return np.roll(out, shift, axis=1) if shift else out


def over(dst, src, alpha=1.0):
    a = src[..., 3:4] / 255 * alpha
    dst[..., :3] = src[..., :3] * a + dst[..., :3] * (1 - a)


def add(dst, src, alpha):
    dst[..., :3] = np.minimum(255, dst[..., :3] + src[..., :3] * src[..., 3:4] / 255 * alpha)


SECTION_OF = SECTION_TILES


def draw_layer_tiles(backdrop, frame, layer, scroll, additive=None):
    rows = scroll + (SCREEN - 1 - np.arange(SCREEN))
    for s in range(len(SECTIONS)):
        tiles = [n for n in SECTION_OF[s] if backdrop["tile_sets"][n]["layer"] == layer]
        if not tiles:
            continue
        lo = seam(backdrop, layer, s)
        hi = seam(backdrop, layer, s + 1) if s + 1 < len(SECTIONS) else math.inf
        sel = (rows >= lo) & (rows < hi)
        if sel.any():
            part = tile_rows(tiles[0], rows[sel])
            sub = frame[sel]
            if additive is None:
                over(sub, part)
            else:
                add(sub, part, additive)
            frame[sel] = sub


def blit(frame, img, x, y, additive=None):
    """``img`` (float RGBA array) with its bottom-left at (x, y) on screen, y up."""
    h, w = img.shape[:2]
    top = SCREEN - y - h
    y0, y1 = max(0, top), min(SCREEN, top + h)
    x0, x1 = max(0, x), min(W, x + w)
    if y1 <= y0 or x1 <= x0:
        return
    sub = frame[y0:y1, x0:x1]
    src = img[y0 - top:y1 - top, x0 - x:x1 - x]
    if additive is None:
        over(sub, src)
    else:
        add(sub, src, additive)


def draw_pieces(backdrop, placed_all, frame, layer, t, additive=None):
    for placed in placed_all:
        spec = backdrop["pieces"][placed["piece"]]
        if spec["layer"] != layer or "tower" in spec or not on_screen(backdrop, placed, t):
            continue
        x, y, w, h = piece_rect(backdrop, placed, t)
        img = cached(placed["piece"], image_index(backdrop, placed, t))
        if placed.get("mirror"):
            img = img[:, ::-1]
        blit(frame, img, x, y, additive)


def wall_quad(frame, tex, shade, foot, roof, height):
    """One wall as TowerProjection draws it: the foot (ax, ay)-(bx, by) and the roof's edge
    (cx, cy)-(dx, dy), left to right as seen from outside; the texture repeating along the foot,
    its rows from the foot up following the true perspective."""
    (ax, ay), (bx, by) = foot
    (cx, cy), (dx, dy) = roof
    k = tower_scale(height)
    xs = [ax, bx, cx, dx]
    ys = [ay, by, cy, dy]
    x0, x1 = int(math.floor(min(xs))), int(math.ceil(max(xs)))
    y0, y1 = int(math.floor(min(ys))), int(math.ceil(max(ys)))
    x0, x1 = max(0, x0), min(W, x1)
    y0, y1 = max(0, y0), min(SCREEN, y1)
    if x1 <= x0 or y1 <= y0:
        return
    yy, xx = np.mgrid[y0:y1, x0:x1].astype(np.float64) + 0.5
    horizontal = abs(ay - by) < 1e-6
    if horizontal:                                 # foot and roof edge horizontal: g from y
        g = (yy - ay) / (cy - ay) if cy != ay else np.zeros_like(yy)
        left = ax + (cx - ax) * g
        right = bx + (dx - bx) * g
        s = (xx - left) / np.where(np.abs(right - left) < 1e-6, 1e-6, right - left)
    else:
        g = (xx - ax) / (cx - ax) if cx != ax else np.zeros_like(xx)
        lo = ay + (cy - ay) * g
        hi = by + (dy - by) * g
        s = (yy - lo) / np.where(np.abs(hi - lo) < 1e-6, 1e-6, hi - lo)
    inside = (g >= 0) & (g <= 1) & (s >= 0) & (s <= 1)
    if not inside.any():
        return
    length = abs(bx - ax) + abs(by - ay)
    th, tw = tex.shape[:2]
    u = np.floor((s * length) % tw).astype(int).clip(0, tw - 1)
    z = (6 - 6 / (1 + np.clip(g, 0, 1) * (k - 1))) / height       # the height fraction at g
    v = (th - 1 - np.floor(np.clip(z, 0, 0.9999) * th)).astype(int)
    col = tex[v, u, :3] * shade
    region_up = frame[SCREEN - y1:SCREEN - y0, x0:x1][::-1]     # rows from the bottom (y0) up
    region_up[inside, :3] = col[inside]


def draw_towers(backdrop, placed_all, frame, t):
    towers = [p for p in placed_all if "tower" in backdrop["pieces"][p["piece"]]]
    towers = sorted(enumerate(towers), key=lambda ip: (backdrop["pieces"][ip[1]["piece"]]["tower"]["height"], ip[0]))
    for _, placed in towers:
        if not on_screen(backdrop, placed, t):
            continue
        spec = backdrop["pieces"][placed["piece"]]["tower"]
        (fx, fy, fw, fh), (rx, ry, rw, rh), k = tower_geometry(backdrop, placed, t)
        tex = cached(spec["wall"])
        shade = spec.get("shade", 0.5)
        fx1, fy1, rx1, ry1 = fx + fw, fy + fh, rx + rw, ry + rh
        h = spec["height"]
        if ry > fy:
            wall_quad(frame, tex, shade, ((fx, fy), (fx1, fy)), ((rx, ry), (rx1, ry)), h)
        if ry1 < fy1:
            wall_quad(frame, tex, 1.0, ((fx1, fy1), (fx, fy1)), ((rx1, ry1), (rx, ry1)), h)
        if rx < fx:
            wall_quad(frame, tex, 1.0, ((fx, fy1), (fx, fy)), ((rx, ry1), (rx, ry)), h)
        if rx1 > fx1:
            wall_quad(frame, tex, shade, ((fx1, fy), (fx1, fy1)), ((rx1, ry), (rx1, ry1)), h)
        blit(frame, cached(placed["piece"]), rx, ry)


def draw_banks(backdrop, frame, look_a, look_b, weight, key, layer, t, additive=None):
    scroll = java_round(scroll_at(t) * backdrop["scroll_factors"][layer])
    rows = scroll + (SCREEN - 1 - np.arange(SCREEN))
    base = additive if additive is not None else 1.0
    for look, share in ((look_a, 1 - weight), (look_b, weight)) if look_a is not look_b else ((look_b, 1.0),):
        name = look.get(key)
        if not name or share <= 0:
            continue
        drift = backdrop["tile_sets"][name].get("drift", 0) or 0
        part = tile_rows(name, rows, java_round(drift * t) % W)
        if additive is None:
            over(frame, part, share)
        else:
            add(frame, part, base * share)


def composite(backdrop, t, placed_all=None, units=False):
    """The play field at t as Backdrop draws it: deep, far, haze, ground (tiles, flat pieces, towers),
    low-air (tiles, banks, pieces), high-air additive at 40 %; ``units`` marks the Creepers' and the
    ground targets' places (for the agent's own checks, not the review)."""
    placed_all = placed_all or expanded(backdrop)
    frame = np.zeros((SCREEN, W, 4))
    frame[..., 3] = 255
    look_a, look_b, weight = looks_at(backdrop, t)
    for layer in ("deep", "far"):
        scroll = java_round(scroll_at(t) * backdrop["scroll_factors"][layer])
        draw_layer_tiles(backdrop, frame, layer, scroll)
        draw_pieces(backdrop, placed_all, frame, layer, t)
    veil = look_a["haze"] + (look_b["haze"] - look_a["haze"]) * weight
    hz = np.array([int(backdrop["haze_colour"][i:i + 2], 16) for i in (0, 2, 4)], float)
    frame[..., :3] = frame[..., :3] * (1 - veil) + hz * veil
    scroll = java_round(scroll_at(t))
    draw_layer_tiles(backdrop, frame, "ground", scroll)
    draw_pieces(backdrop, placed_all, frame, "ground", t)
    draw_towers(backdrop, placed_all, frame, t)
    if units:
        mark_units(frame, t)
    scroll = java_round(scroll_at(t) * backdrop["scroll_factors"]["low-air"])
    draw_layer_tiles(backdrop, frame, "low-air", scroll)
    draw_banks(backdrop, frame, look_a, look_b, weight, "banks", "low-air", t)
    draw_pieces(backdrop, placed_all, frame, "low-air", t)
    scroll = java_round(scroll_at(t) * backdrop["scroll_factors"]["high-air"])
    draw_layer_tiles(backdrop, frame, "high-air", scroll, additive=0.4)
    draw_pieces(backdrop, placed_all, frame, "high-air", t, additive=0.4)
    draw_banks(backdrop, frame, look_a, look_b, weight, "wisps", "high-air", t, additive=0.4)
    return Image.fromarray(np.clip(frame[..., :3], 0, 255).astype(np.uint8), "RGB")


def mark_units(frame, t):
    """Outlines where the Creepers walk (each unit from its entry, at 35 px/s along its path) and
    where the ground targets stand: a check that no tower covers them."""
    scroll = scroll_at(t)
    d = ImageDraw.Draw(img := Image.new("RGBA", (W, SCREEN), (0, 0, 0, 0)))
    for x, pos, w, h in [(u[1], u[2], u[3], u[4]) for u in target_units()]:
        y = SCREEN - (pos - scroll)
        d.rectangle([x - w / 2, y - h / 2, x + w / 2, y + h / 2], outline=(0, 255, 120, 255))
    speed_w = enemy_data("creeper").get("speed", 35)
    for wt, n, path in walker_waves():
        for i in range(n):
            t0 = wt + CONVOY * i
            if t < t0:
                continue
            g = ground_path(wt, path, i)
            dist = (t - t0) * speed_w
            px, pp = g[0]
            for (ax, ap), (bx, bp) in zip(g, g[1:]):
                seg = math.hypot(bx - ax, bp - ap)
                if dist <= seg:
                    px, pp = ax + (bx - ax) * dist / seg, ap + (bp - ap) * dist / seg
                    break
                dist -= seg
                px, pp = bx, bp
            y = SCREEN - (pp - scroll)
            d.ellipse([px - 22, y - 22, px + 22, y + 22], outline=(255, 60, 255, 255), width=2)
    a = np.array(img).astype(np.float64)
    over(frame, a)


# --------------------------------------------------------------------------- checks

def wrap_score(a, columns=False):
    """BackdropSeamsTest's wrap score, on luminance premultiplied by alpha."""
    if columns:
        a = np.transpose(a, (1, 0, 2))
    lum = (a[..., 0] * 0.299 + a[..., 1] * 0.587 + a[..., 2] * 0.114) * a[..., 3] / 255
    h = a.shape[0]

    def change(i, j):
        d = np.sort(np.abs(lum[i] - lum[j]))
        return d[:int(len(d) * 0.9)].mean()
    worst = 0.0
    for k in (h - 2, h - 1, h):
        ref = sorted(change(r % h, (r + 1) % h) for r in range(k - 16, k + 17, 4) if r != k)
        worst = max(worst, change(k % h, (k + 1) % h) / max(ref[len(ref) // 2], 0.5))
    return worst


def check_tiles(backdrop):
    problems = []
    for name, spec in backdrop["tile_sets"].items():
        a = cached(name)
        score = wrap_score(a)
        line = f"  wrap {name}: rows {score:.2f}"
        if score > 1.5:
            problems.append(f"{name}: a line where it repeats ({score:.2f})")
        if spec.get("drift"):
            cscore = wrap_score(a, columns=True)
            line += f", columns {cscore:.2f}"
            if cscore > 1.5:
                problems.append(f"{name}: a line where it wraps sideways ({cscore:.2f})")
        print(line)
    return problems


def check_edges(backdrop, placed_all):
    """No flat piece shows an edge on screen (a tower's roof lies over its own walls)."""
    bad = []
    for placed in placed_all:
        spec = backdrop["pieces"][placed["piece"]]
        if "tower" in spec:
            continue
        for i in range(spec.get("frames") or spec.get("headings") or 1):
            a = cached(placed["piece"], i if (spec.get("frames") or spec.get("headings")) else None)[..., 3] > 0
            x, _, w, _ = piece_rect(backdrop, placed, placed["t"])
            cols = slice(max(0, -x), min(w, W - x))
            if a[0, cols].any() or a[-1, cols].any():
                bad.append(f"{placed['piece']} at t {placed['t']:.2f}: its top or bottom row is opaque")
            if (0 < x < W and a[:, 0].any()) or (0 < x + w < W and a[:, -1].any()):
                bad.append(f"{placed['piece']} at t {placed['t']:.2f}: a side column is opaque on screen")
    return sorted(set(bad))


def check_screens(backdrop, placed_all):
    """BackdropCheck.checkScreens: at most 3 mid-size pieces and 2 strongly animated elements on
    screen at every step, every piece on screen at some time, a tower's roof inside the motion
    budget."""
    problems = []
    seen = [False] * len(placed_all)
    st = stretches()
    for name, spec in backdrop["pieces"].items():
        if "tower" in spec and (tower_scale(spec["tower"]["height"]) - 1) * SPEED > 120:
            problems.append(f"{name}: its roof moves faster than 120 px/s")
    for step in range(0, int(OUTRO_END * 60) + 1, 2):
        t = step / 60
        mid, moving = [], set()
        for i, placed in enumerate(placed_all):
            if on_screen(backdrop, placed, t):
                seen[i] = True
                spec = backdrop["pieces"][placed["piece"]]
                if spec.get("mid_size"):
                    mid.append(placed["piece"])
                if spec.get("frames") or placed.get("path"):
                    moving.add(placed["piece"])
        atms = [next((s for s in st if t < s[1]), st[-1])[2]]
        atms += [x for k in range(1, len(st)) if abs(t - st[k][0]) < RAMP / 2 for x in (st[k - 1][2], st[k][2])]
        if any(backdrop["tile_sets"].get(backdrop["atmosphere"][x].get("banks"), {}).get("drift") for x in atms):
            moving.add("atmosphere banks")
        if len(mid) > 3:
            problems.append(f"t={t:.2f}: {len(mid)} mid-size pieces: {mid}")
        if len(moving) > 2:
            problems.append(f"t={t:.2f}: {len(moving)} animated elements: {sorted(moving)}")
    problems += [f"{p['piece']} at t {p['t']:.2f} is never on screen" for p, s in zip(placed_all, seen) if not s]
    return sorted(set(problems))[:16]


def check_gameplay(backdrop, placed_all):
    """No tower's walls or roof come over a walker unit's place or a ground target's at any step
    (the agent's own check of the placement rule, on the drawn geometry)."""
    problems = []
    towers = [p for p in placed_all if "tower" in backdrop["pieces"][p["piece"]]]
    units = [(u[0], u[1], u[2], u[3], u[4]) for u in target_units()]
    speed_w = enemy_data("creeper").get("speed", 35)
    for step in range(0, int(OUTRO_END * 30)):
        t = step / 30
        scroll = scroll_at(t)
        spots = [(name, x, pos - scroll, max(w, 30), max(h, 30)) for name, x, pos, w, h in units]
        for wt, n, path in walker_waves():
            for i in range(n):
                t0 = wt + CONVOY * i
                if t < t0:
                    continue
                g = ground_path(wt, path, i)
                dist = (t - t0) * speed_w
                px, pp = g[0]
                for (ax, ap), (bx, bp) in zip(g, g[1:]):
                    seg = math.hypot(bx - ax, bp - ap)
                    if dist <= seg:
                        px, pp = ax + (bx - ax) * dist / seg, ap + (bp - ap) * dist / seg
                        break
                    dist -= seg
                    px, pp = bx, bp
                spots.append((f"creeper t={wt:g}#{i}", px, pp - scroll, 44, 44))
        spots = [s for s in spots if -30 < s[2] < SCREEN + 30]
        if not spots:
            continue
        for placed in towers:
            if not on_screen(backdrop, placed, t):
                continue
            (fx, fy, fw, fh), (rx, ry, rw, rh), _ = tower_geometry(backdrop, placed, t)
            x0, x1 = min(fx, rx), max(fx + fw, rx + rw)
            y0, y1 = min(fy, ry), max(fy + fh, ry + rh)
            for name, x, y, w, h in spots:
                if x + w / 2 > x0 and x - w / 2 < x1 and y + h / 2 > y0 and y - h / 2 < y1:
                    problems.append(f"t={t:.1f}: {placed['piece']} at t {placed['t']:.2f} over {name}")
    return sorted(set(problems))[:16]


def run_checks(backdrop):
    placed_all = expanded(backdrop)
    problems = check_tiles(backdrop) + check_edges(backdrop, placed_all) + check_screens(backdrop, placed_all)
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
            rw, rh = roof_size(s["size"], s["tower"]["height"])
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

TIMES = [(2, "1 THE HARBOUR, THE QUAY'S CRANES"), (8, "1 GUNSHIPS HEADING HOME"), (13, "2 CROSS HIGHWAY, LAST YARDS"),
         (30, "2 AIRCAR STREAMS OVER THE AVENUES"), (46.5, "2 THE LANDING-PAD NEST"), (60, "2 THE FIRST TOWERS"),
         (70.5, "3 THE FIRST CREEPER'S ROOF AND RAMP"), (86.5, "3 CONVOY ROOFS IN THE TOWERS"),
         (91, "3 THE ROOF NEST"), (102, "3 THE BILLBOARD'S ROOF"), (118, "3-4 INTO THE SMOKE"),
         (126.5, "4 THE PARKING DECK"), (140, "4 BURNING BLOCKS, FIRES"), (152.5, "4 THE COLLAPSED OVERPASS"),
         (166, "5 THIRD MAINLAND, THE CONVOY'S HIGHWAY"), (171, "5 THE SMOKE WALL"), (185, "5 NEON HEIGHTS"),
         (203, "5 THE NDIDI ARCOLOGY")]


def review(backdrop):
    items = []
    walls = sorted({s["tower"]["wall"] for s in backdrop["pieces"].values() if "tower" in s})
    shown = set()
    for name, spec in list(backdrop["tile_sets"].items()) + list(backdrop["pieces"].items()):
        if "tower" in spec and name != "arcology":
            key = name.rsplit("-", 1)[1] + name.split("-")[2]
            if name.split("-")[1] != "68x64" or key in shown:
                continue
            shown.add(key)
        count = spec.get("frames") or spec.get("headings") if "size" in spec else None
        imgs = [load(name, i) for i in range(count)] if count else [load(name)]
        label = f"{name} {imgs[0].width}x{imgs[0].height} {artkit.colour_count(imgs)} col"
        for k, im in enumerate(imgs[:2] if spec.get("headings") else imgs[:1]):
            scale = min(1.0, 200 / max(im.size)) if spec.get("layer") != "ground" or "tower" in spec or max(im.size) > 200 else 1.0
            if spec.get("headings") or name.startswith("neon") or name == "fire":
                scale = 2.0
            im = im.resize((max(1, int(im.width * scale)), max(1, int(im.height * scale))), Image.NEAREST)
            items.append((label + (f" [{k}]" if count else ""), im, spec.get("layer") == "high-air"))
    for wall in walls:
        im = load(wall)
        items.append((f"{wall} {im.width}x{im.height}", im.resize((im.width * 2, im.height * 2), Image.NEAREST), False))
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
    sheet = raster.sheet(width, comp_y + n_rows * (270 + 30) + 20, "LEVEL 08 BACKDROP - FINAL",
                         "PRODUCTION ART, M5 PART B BATCH - R30; COMPOSITES AS THE GAME DRAWS THEM (TOWERS PROJECTED), 1/2 SCALE")
    for name, im, add_, px, py in rows:
        plate = Image.new("RGBA", im.size, artkit.PLATE)
        if add_:
            plate = artkit.add_light(plate, im)
        else:
            plate.alpha_composite(im)
        sheet.alpha_composite(plate, (px, py + 14))
        raster.draw_text(sheet, px, py, name.upper()[:44], raster.LABEL)
    placed_all = expanded(backdrop)
    for k, (t, label) in enumerate(TIMES):
        px, py = 16 + (k % per_row) * cw, comp_y + (k // per_row) * 300
        sheet.alpha_composite(composite(backdrop, t, placed_all).convert("RGBA").resize((240, 270), Image.BOX), (px, py + 14))
        raster.draw_text(sheet, px, py, f"T={t:g} {label}"[:40], raster.LABEL)
    REVIEW.parent.mkdir(parents=True, exist_ok=True)
    sheet.convert("RGB").save(REVIEW, optimize=True)
    print(f"review: {REVIEW.relative_to(ROOT)}")


TRAFFIC_TIMES = [(1.9, "1 A GUNSHIP HEADING HOME (8)"), (9.2, "1 A GUNSHIP TURNING SOUTH-WEST (8-10)"),
                 (30, "2 SEDANS AND VANS SOUTH, SEDANS NORTH"), (38.2, "2 THE CDF PAIR (9), SEDANS IN THE VAN LANE"),
                 (72.5, "3 THINNING OUT OVER THE BOULEVARD")]
TRAFFIC_GIF = (36.4, 40.4, 0.08)      # s: the CDF pair crossing the avenue streams, at 12.5 fps


def traffic_review(backdrop):
    """traffic-final-r30-a.png/.gif: the three heading sets at 1x and 3x on the night plate, the
    streams in composites as the game draws them, a 2x crop; the GIF the CDF pair over the streams."""
    names = [n for n, s in backdrop["pieces"].items() if s.get("headings") and n in l08_traffic.CRAFT]
    sets = {n: [load(n, i) for i in range(backdrop["pieces"][n]["headings"])] for n in names}
    placed_all = expanded(backdrop)
    shots = [composite(backdrop, t, placed_all) for t, _ in TRAFFIC_TIMES]
    crop = shots[3].crop((240, 120, 480, 390)).resize((480, 540), Image.NEAREST)
    width = 16 + len(shots) * (W + 12) + 4
    width = max(width, 16 + 16 * (44 * 3 + 6))
    y_sets = 74
    rows_h = sum(max(f.height for f in fr) * 4 + 52 for fr in sets.values())
    height = y_sets + rows_h + 24 + SCREEN + 30 + SCREEN + 40
    sheet = raster.sheet(width, height, "LEVEL 08 TRAFFIC - FINAL",
                         "PRODUCTION ART, M5 PART B BATCH - R30 VARIANT A (WEDGE CARS); LOW-AIR SCENERY (D2 = A): NO COLLISION")
    raster.draw_text(sheet, 16, 34, "ROUND 30 VARIANT A'S MODELS (TOOLS/CONCEPT/PROPS_R30.PY) AT 16 HEADINGS (_0 NOSE UP, CLOCKWISE), "
                     "TURNED UNDER THE FIXED TOP-LEFT KEY LIGHT, MUTED, 32 COLOURS PER SET,", raster.LABEL)
    raster.draw_text(sheet, 16, 46, "LAMP HALOS BAKED IN (STEPPED ALPHA), THE STROBE STEADY; ONE PAINT PER PIECE (A STREAM REPEATS ONE "
                     "PIECE): SEDAN SLATE, VAN BEIGE.", raster.LABEL_DIM)
    y = y_sets
    for name, fr in sets.items():
        raster.draw_text(sheet, 16, y, f"{name.upper()} {fr[0].width}X{fr[0].height}, {len(fr)} HEADINGS, "
                         f"{artkit.colour_count(fr)} COLOURS: 1X, THEN 3X", raster.LABEL)
        x = 16
        for f in fr:
            plate = Image.new("RGBA", f.size, artkit.PLATE)
            plate.alpha_composite(f)
            sheet.alpha_composite(plate, (x, y + 12))
            x += f.width + 4
        x, yy = 16, y + 12 + fr[0].height + 8
        for f in fr:
            big = f.resize((f.width * 3, f.height * 3), Image.NEAREST)
            plate = Image.new("RGBA", big.size, artkit.PLATE)
            plate.alpha_composite(big)
            sheet.alpha_composite(plate, (x, yy))
            x += big.width + 6
        y += max(f.height for f in fr) * 4 + 52
    y += 8
    raster.draw_text(sheet, 16, y, "PLAY FIELD 1X AS THE GAME DRAWS IT (THE LEVEL'S BACKDROP BLOCK; HEADING INDEX IN BRACKETS)",
                     raster.LABEL)
    for k, (im, (t, label)) in enumerate(zip(shots, TRAFFIC_TIMES)):
        px = 16 + k * (W + 12)
        sheet.paste(im, (px, y + 26))
        raster.draw_text(sheet, px, y + 14, f"T={t:g} {label}"[:78], raster.LABEL_DIM)
    y += 26 + SCREEN + 16
    raster.draw_text(sheet, 16, y, "2X: THE PAIR'S SECOND GUNSHIP BEARING SOUTH-SOUTH-WEST (HEADING 9) OVER THE EAST BLOCKS (T=38.2)", raster.LABEL_DIM)
    sheet.paste(crop, (16, y + 12))
    sheet = sheet.crop((0, 0, width, y + 12 + crop.height + 16))
    sheet.convert("RGB").save(TRAFFIC_REVIEW, optimize=True)
    t0, t1, dt = TRAFFIC_GIF
    frames = [composite(backdrop, float(t), placed_all) for t in np.arange(t0, t1, dt)]
    gif = TRAFFIC_REVIEW.with_suffix(".gif")
    artkit.write_gif(frames, gif, fps=round(1 / dt, 1))
    print(f"review: {TRAFFIC_REVIEW.relative_to(ROOT)}, {gif.relative_to(ROOT)} {gif.stat().st_size / 1e6:.1f} MB")


def strip_frames(backdrop, times, path, units=False):
    """Full-size composites at ``times`` side by side (for a look at 1x)."""
    placed_all = expanded(backdrop)
    shots = [composite(backdrop, t, placed_all, units) for t in times]
    out = Image.new("RGB", (len(shots) * (W + 8), SCREEN), (0, 0, 0))
    for i, s in enumerate(shots):
        out.paste(s, (i * (W + 8), 0))
    out.save(path)
    print(f"strip: {path}")


def main(argv):
    global SECTION_OF
    if "--proposal" in argv:
        write_proposal()
        return
    backdrop, source = level_backdrop()
    if source == "data.yaml":
        SECTION_OF = [s["tiles"] for s in SECTIONS]
    print(f"backdrop from {source}")
    if "--strip" in argv:
        k = argv.index("--strip")
        strip_frames(backdrop, [float(a) for a in argv[k + 1].split(",")], argv[k + 2], "--units" in argv)
        return
    wanted = {a for a in argv if not a.startswith("--") and not a.endswith(".png") and "," not in a}
    if "--traffic-review" in argv:
        traffic_review(backdrop)
        return
    if "--check" in argv:
        atlas_area(backdrop)
        sys.exit(1 if run_checks(backdrop) else 0)
    if "--review" not in argv:
        OUT.mkdir(parents=True, exist_ok=True)
        todo = jobs(backdrop, wanted)
        todo.sort(key=lambda j: -(j[1][0] * j[1][1] if j[1] else 0))
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
        _cache.clear()
        if not wanted:
            run_checks(backdrop)
    atlas_area(backdrop)
    if not wanted:
        review(backdrop)
        traffic_review(backdrop)


if __name__ == "__main__":
    main(sys.argv[1:])
