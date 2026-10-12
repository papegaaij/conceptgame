#!/usr/bin/env python3
"""Production art: the Level 14 backdrop, Geneva Concord under the Vrell canopy
(design/campaign/act-2-homefront/level-14-siege-spire; M5 part H batch, concept round 36, step H5).

Straight to production (H8 = a, pending the user's review) from the chosen Geneva scene,
scene-geneva-r10-a (tools/concept/scenes_r10.py, imported unchanged for its helpers: the street
network as continuous gaps, the perimeter blocks of row houses round courtyards with their two-tone
ridged roofs lit from the top left, party walls, chimneys and skylights, cast shadows, the materials
of the old town and the 19th-century quarters, the lake's colours, the trees, the sodium lamps and
the canopy's mottled shadow). The city is rebuilt here from **plans** (avenue, side streets, cross
streets, squares, parks, diagonals, a roundabout, the lake's shore) so each section gets its own tile
set and the set pieces fit the tiles under them.

**Sections** (the README's Layout; read from the level's data once it exists): 1 the Lakefront at
dusk (Lake Geneva on the left, the quays, the promenade; the Rhone leaving the lake with the Pont du
Mont-Blanc, the armoured car's bridge, the Jet d'Eau's plume; Hammer flight's bombers passing below on
low-air); 2 the Old Town (perimeter blocks, tile roofs, sodium-lit streets, creep veins in the gutters;
the cathedral of Saint-Pierre on its square with the turret nest, its towers drawn in perspective; the
Place du Molard under the two hive nodes); 3 the Root Field (the streets broken by roots as thick as
trams, cracked paving, collapsed houses, creep over the roofs; the Ravager pack's crater; the spore
haze); 4-6 the Concord approach, the arena and the start of the daylight as **one unique strip**
(``concord-1..4``): the avenues converging on the Concord roundabout, the rotunda's plaza where the
Siege Spire stands (the anchor from the boss data), its roots running on along six avenues, then the
city north of it in the daylight. **Why a strip**: the arena is a halt (speed 0); during it the
screen shows the ground from the simulation's halt scroll (the data's scroll at the arena's start plus
the 1 s ease into the halt, 68.83 px at 140 px/s: Sortie.haltScroll) to 540 px above, and every
section from the arena on begins at the seam the arena's start brings to the top edge, so the arena's
own tile set is never drawn and the daylight's shows in the top 69 px; only pieces can give the arena
its frame. Every seam between two tile sets lies under a ``boulevard`` piece (a tree-lined boulevard
with tram rails).

**The daylight** (daylight_l14.py's look **a**, golden hour, in the game until the user picks a or b
in round 36: set ``DAYLIGHT = "b"``): the strip above the arena's frame is graded by its distance past
the frame's top, as it scrolls in after the death (0 at the frame, full by about 9 s of scroll), so
the screen's top always shows the light of the moment and the lower rows the light that came before:
the sunlight sweeps in from ahead. The lamps go out and the canopy's shadow lifts with the grade. The
``daylight`` tile set (fully graded) carries on past the strip. The game draws the tear itself on the
real clock from the Spire's death (step H6), from images written here outside the backdrop block:
``canopy-tear_0..3`` (480 x 540, straight alpha: the withering, tearing membrane at 1.5, 3.5, 5.5 and
7.5 s, tiling with period 540 on the high-air scroll) and ``daylight-beams_0..1`` (480 x 540,
additive: the low sun's beams through the tears at 5 and 7.5 s).

**Layers**: no deep layer (the ground tiles are opaque, the lake painted on them, as Level 04's
Luna); `ground` 1.0; `low-air` 1.35: the spore haze banks (light ~10 %, medium ~22 %, violet lit from
below; static); `high-air` 2.0 (additive at 40 % in the game): ``canopy``, the canopy's teal veins
in every section (the canopy's shadow is baked into the ground tiles, as the concept does: high-air
is additive and cannot darken). **Motion budget**: Hammer flight (one piece on a path) and a few
moving cars (one piece) in sections 1-2; nothing moves in the arena (the halt).

Outputs (assets/backdrop/level-14/):
  lakefront, old-town, root-field, daylight   ground tile sets 480x960
  haze-light, spore-haze                       low-air tile sets 480x960 (banks)
  canopy                                       high-air tile set 480x960 (additive veins)
  concord-1..4                                 ground pieces 480xN: the approach, arena, daylight strip
  boulevard, boulevard-day                     ground pieces 480x48: the seams' boulevard (dusk, sunlit)
  rhone                                        ground piece: the Rhone, the Pont du Mont-Blanc, Ile Rousseau
  jet-deau                                     ground piece: the Jet d'Eau's plume (leaning, static)
  cathedral, molard                            ground pieces: Saint-Pierre's square, the Place du Molard
  cathedral-tower-n/-s                         tower pieces (perspective) with wall-stone
  root-crater                                  ground piece: the Ravager pack's crater (root burst a)
  traffic-car_0..1                             ground, 2 headings: a car with its headlights
  hammer-flight_0..1                           low-air, 2 headings: three CDF bombers in a vee
  canopy-tear_0..3, daylight-beams_0..1        the game's daylight looks (not in the block, see above)
  design/campaign/.../level-14-siege-spire/concept/backdrop-final-r36-a.png/.gif   review
  design/campaign/.../level-14-siege-spire/concept/backdrop-proposal.yaml          the block (--proposal)

Reads the level's data.yaml when it exists (sections, scroll speed, ground targets, root bursts, the
boss's anchor; the backdrop block once it has no ``images`` key), otherwise the README's defaults
below; the Siege Spire's data for its anchor. Run:
python3 tools/art/backdrop_l14.py [--proposal | --review | --check | --strip t,.. out.png] [id ...]
(all: about 4 min)
"""
import math
import sys
from concurrent.futures import ProcessPoolExecutor
from functools import lru_cache

import numpy as np
import yaml
from PIL import Image, ImageDraw, ImageFilter

import artkit
from artkit import DESIGN, ROOT, TAU

import daylight_l14 as dl  # noqa: E402  (tools/art: the daylight grade, the canopy, the beams)
import root_burst  # noqa: E402  (tools/art: the crater)
import backdrop_l08 as k8  # noqa: E402  (tools/art: wrap score, blit, wall quads, YAML flow)
import parallax_r02 as r02  # noqa: E402  (concept scripts, unchanged)
import parallax_r03 as r03  # noqa: E402
import scenes_r06 as s6  # noqa: E402
import scenes_r10 as s10  # noqa: E402
from scenes_r10 import axis_dist, cast_shadow, cheb_dist, hash01  # noqa: E402
from render import raster, sdf, sprite  # noqa: E402
from render.sdf import rotate_z  # noqa: E402

SCRIPT = "backdrop_l14.py"
BATCH = "M5 part H batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
LEVEL_DIR = DESIGN / "campaign" / "act-2-homefront" / "level-14-siege-spire"
CONCEPT = LEVEL_DIR / "concept"
PROPOSAL = CONCEPT / "backdrop-proposal.yaml"
OUT = ROOT / "assets" / "backdrop" / "level-14"
REVIEW = CONCEPT / "backdrop-final-r36-a.png"
W, SCREEN = 480, 540
MID = SCREEN / 2
TILE_H = 960
COLOURS = 32
DAYLIGHT = "a"                          # round 36's audition: look a in the game until the user chooses

FACTORS = {"far": 0.9, "ground": 1.0, "low-air": 1.35, "high-air": 2.0}    # the concept's (far unused)
HAZE_COLOUR = "3a2c48"                  # unused (no deep or far layer), the spore haze's violet
ATMOSPHERE = {   # haze 0: no deep or far layer to veil
    "clear": {"haze": 0.0},                              # the arena and the daylight: canopy only
    "light": {"banks": "haze-light", "haze": 0.0},       # thin spore haze ~10 %
    "medium": {"banks": "spore-haze", "haze": 0.0},      # spore-haze banks lit violet from below ~22 %
}
RAMP = 4
CAMERA = 6.0

# --------------------------------------------------------------------------- the level

DEFAULT_SECTIONS = [   # the README's Layout until the level data exists
    {"name": "Lakefront", "end": 20, "atmosphere": "light"},
    {"name": "Old Town", "end": 60, "atmosphere": "light"},
    {"name": "Root Field", "end": 95, "atmosphere": "medium"},
    {"name": "Concord Approach", "end": 108, "atmosphere": "medium"},
    {"name": "Siege Spire", "end": 250, "atmosphere": "clear", "speed": 0, "arena": True},
    {"name": "Daylight", "end": 262, "atmosphere": "clear"},
]
# The units the backdrop is laid out round, until the level data names them (README; the content step
# H4 adopts or moves them, then a re-run follows the data): [t, x] = enters at the top edge at t.
DEFAULT_CAR = [18.0, 150.0]                       # the armoured car on the Pont du Mont-Blanc
DEFAULT_NEST = [[40.0, 300.0], [40.0, 340.0], [40.5, 320.0]]   # the turret nest on Saint-Pierre's square
DEFAULT_HIVES = [[56.0, 180.0], [56.0, 290.0]]    # the hive nodes in the Place du Molard
DEFAULT_BURSTS = [   # t (the telegraph's start), x, depth (px below the top edge at t)
    {"t": 64, "x": 140, "depth": 220}, {"t": 74, "x": 330, "depth": 180},
    {"t": 84, "x": 190, "depth": 250}, {"t": 92, "x": 300, "depth": 200}]
DEFAULT_ANCHOR_X = 240.0


def _load(path):
    return yaml.safe_load(path.read_text(encoding="utf-8")) if path.exists() else {}


DATA = _load(LEVEL_DIR / "data.yaml")
SPIRE = _load(DESIGN / "enemies" / "bosses" / "siege-spire" / "data.yaml")
SECTIONS = DATA.get("sections") or DEFAULT_SECTIONS
SPEED = float(DATA.get("scroll_speed", 140))
STARTS = [0.0] + [float(s["end"]) for s in SECTIONS[:-1]]
END = float(SECTIONS[-1]["end"])
OUTRO_END = END + 15                    # LevelData.OUTRO_SECONDS
ARENA = next(i for i, s in enumerate(SECTIONS) if s.get("arena"))
HALT_T = STARTS[ARENA]
ARENA_END = float(SECTIONS[ARENA]["end"])


def speed(i):
    return float(SECTIONS[i].get("speed", SPEED))


def scroll_at(t):
    """LevelData.scrollAt (no timed halts in this level): the ground's distance at t."""
    scroll = 0.0
    for i, s in enumerate(SECTIONS):
        if t < s["end"] or i == len(SECTIONS) - 1:
            return scroll + (t - STARTS[i]) * speed(i)
        scroll += (s["end"] - STARTS[i]) * speed(i)
    raise AssertionError


def t_at(scroll):
    """The first t at which the ground has scrolled ``scroll`` (the halted arena skipped)."""
    if scroll < 0:
        return scroll / speed(0)
    for i, s in enumerate(SECTIONS):
        span = (s["end"] - STARTS[i]) * speed(i)
        if speed(i) > 0 and (scroll < span or i == len(SECTIONS) - 1):
            return STARTS[i] + scroll / speed(i)
        scroll -= span
    raise AssertionError


def ease_distance(v):
    """Sortie.easeDistance: the ground scrolled while easing from v px/s to the halt over 1 s (60 steps)."""
    n = 60
    return sum(v * (n - 1 - j) / n / 60 for j in range(n - 1))


EASE = ease_distance(speed(ARENA - 1))
HALT_SCROLL = scroll_at(HALT_T) + EASE  # Sortie.haltScroll


def sim_scroll(t, kill=None):
    """The ground's scroll as the simulation runs it at level time t (the ease into the halt flown over
    the arena's first second, all of it after the arena's end); ``kill`` = the level time of an early
    kill: the clock jumps to the arena's end then (the review's daylight)."""
    if t < HALT_T:
        return scroll_at(t)
    steps = min(59, int((t - HALT_T) * 60))
    return scroll_at(t) + sum(speed(ARENA - 1) * (59 - j) / 60 / 60 for j in range(steps))


def seam_pos(s, layer="ground"):
    return -math.inf if s == 0 else scroll_at(STARTS[s]) * FACTORS[layer] + SCREEN


def t_for_centre(layer, centre):
    return t_at((centre - MID) / FACTORS[layer])


def enemy_data(enemy):
    for kind in ("ground", "air"):
        f = DESIGN / "enemies" / kind / enemy / "data.yaml"
        if f.exists():
            return _load(f)
    return {}


def _targets():
    """(car [t, x], nest [[t, x]..], hives [[t, x]..]) from the data's ground targets, or the defaults."""
    car, nest, hives, molard_nest = None, [], [], []
    for g in DATA.get("ground_targets", []) or []:
        spots = sorted({tuple(a) for v in [g] + [g.get(d) or {} for d in ("easy", "hard")] for a in v.get("at", [])})
        if g.get("side"):
            continue
        if g.get("sprite") == "armoured-car" or "car" in g.get("target", "").lower():
            car = list(spots[0])
        elif g.get("enemy") == "hive-node":
            hives += [list(s) for s in spots]
        elif g.get("enemy") == "spine-turret" and spots and 30 <= spots[0][0] <= 50:
            nest += [list(s) for s in spots]
        elif g.get("enemy") == "spine-turret" and spots and 50 < spots[0][0] <= 60:
            molard_nest += [list(s) for s in spots]    # hard's nest beside the hive nodes: on the Molard too
    return car or DEFAULT_CAR, nest or DEFAULT_NEST, hives or DEFAULT_HIVES, molard_nest


CAR_AT, NEST_AT, HIVES_AT, MOLARD_NEST_AT = _targets()
BURSTS = [{"t": float(b["t"]), "x": float(b["x"]), "depth": float(b.get("y", b.get("depth", 200)))}
          for b in ((DATA.get("root_bursts") or {}).get("bursts") or DEFAULT_BURSTS)]
_BOSS = DATA.get("boss") or {}
ANCHOR_X = float(_BOSS.get("x", DEFAULT_ANCHOR_X))
ANCHOR_Y = float(((SPIRE.get("movement") or {}).get("anchored") or {}).get("y", 196))
ANCHOR_POS = HALT_SCROLL + SCREEN - ANCHOR_Y        # the trunk's foot on the ground layer


def target_pos(t, hitbox_h):
    """A ground target's centre on the ground layer: it enters at the top edge at t (its hitbox's top at the edge)."""
    return scroll_at(t) + SCREEN + hitbox_h / 2


def burst_pos(b):
    return scroll_at(float(b["t"])) + SCREEN - float(b["depth"])


# --------------------------------------------------------------------------- colours (the concept's)

ASPHALT = np.array((50, 50, 60), float)
WALK = np.array((72, 70, 80), float)
COURT = np.array((50, 50, 58), float)
GARDEN = np.array((36, 52, 40), float)
QUAY = np.array((100, 98, 106), float)
MATS = np.array([(150, 88, 66), (128, 92, 74), (94, 98, 114), (114, 122, 136), (80, 116, 104), (62, 72, 86)], float)
GREENS = [(10, 22, 14), (20, 36, 24), (34, 56, 36), (50, 74, 48)]
ROOT_BODY = np.array((58, 50, 66), float)          # the Spire's slate root tissue (boss_models' root colour, dusk)
ROOT_GLOW = np.array((0, 150, 104), float)         # the concept's teal seam on the roots
CREEP = np.array((44, 30, 56), float)
VEIN = np.array((120, 70, 200), float)
LAMP = np.array((54, 38, 16), float)
STREET, SQUARE, PARK, WATER, QUAY_K, PROM, RUBBLE, PLAZA = 1, 2, 3, 4, 5, 6, 7, 8


# --------------------------------------------------------------------------- geometry helpers

def pdy(a, b, per):
    """Signed vertical distance a - b, wrapped into [-per/2, per/2) when ``per`` (else plain)."""
    d = np.asarray(a, float) - b
    return (d + per / 2) % per - per / 2 if per else d


def seg_dist(xx, yy, p0, p1, per=None):
    """Distance from every pixel to the segment p0-p1 (rows wrap with ``per``), and the segment parameter."""
    (x0, y0), (x1, y1) = p0, p1
    vx, vy = x1 - x0, y1 - y0
    dx, dy = xx - x0, pdy(yy, y0, per)
    L2 = vx * vx + vy * vy or 1e-9
    u = np.clip((dx * vx + dy * vy) / L2, 0, 1)
    return np.hypot(dx - u * vx, dy - u * vy), u


def poly_dist(xx, yy, pts, per=None, pad=40):
    """Distance to a polyline and the arc-length share 0..1 at the nearest point (beyond ``pad`` px of
    a segment the distance is left at 1e9 when the rows do not wrap)."""
    best = np.full(xx.shape, 1e9)
    along = np.zeros(xx.shape)
    lens = [math.hypot(b[0] - a[0], b[1] - a[1]) for a, b in zip(pts, pts[1:])]
    total, acc = sum(lens) or 1.0, 0.0
    for (a, b), ln in zip(zip(pts, pts[1:]), lens):
        if max(a[0], b[0]) + pad < 0 or min(a[0], b[0]) - pad > W:
            acc += ln
            continue
        if per:
            rows = slice(None)
        else:
            r0 = int(max(0, math.floor(min(a[1], b[1]) - pad)))
            r1 = int(min(xx.shape[0], math.ceil(max(a[1], b[1]) + pad)))
            if r1 <= r0:
                acc += ln
                continue
            rows = slice(r0, r1)
        d, u = seg_dist(xx[rows], yy[rows], a, b, per)
        closer = d < best[rows]
        best[rows] = np.where(closer, d, best[rows])
        along[rows] = np.where(closer, (acc + u * ln) / total, along[rows])
        acc += ln
    return best, along


def periodic_curve(per, x0, amps, seed, step=4):
    """A root's course down a tile: x(y) = x0 + sum of sines with whole periods, so it wraps."""
    rng = np.random.default_rng(seed)
    phases = rng.uniform(0, TAU, len(amps))
    ys = np.arange(-step, per + 2 * step, step, dtype=float)
    xs = x0 + sum(a * np.sin(TAU * (k + 1) * ys / per + ph) for k, (a, ph) in enumerate(zip(amps, phases)))
    return list(zip(xs, ys))


# --------------------------------------------------------------------------- the city (the concept's method)

def _houses(kind, per, plan, xx, yy, xi, yi):
    """Perimeter blocks of row houses round courtyards (scenes_r10.GenevaScene._city): returns
    (roof mask, ridge height, eave height, roof colour, key, court, garden, cell, hrow)."""
    built = kind == 0
    free = ~built
    dxd, sx = axis_dist(free, 1)
    dyd, sy = axis_dist(free, 0)
    cheb = cheb_dist(free)
    d = 0.5 * (np.minimum(dxd, dyd) + cheb)
    vc = sorted(plan["vcen"])
    hc = sorted(plan["hcen"])
    col = np.digitize(xx, vc)
    row = np.digitize(yy, hc) % max(1, len(hc))
    cell = (col * 64 + row + 4096 * plan.get("salt", 0)).astype(np.int64)
    ring = 13 + np.floor(hash01(cell, 1) * 5)
    rng = np.random.default_rng(1501)
    widths = rng.choice([8, 9, 10, 11, 12, 13, 14, 15, 16, 18], size=600)
    table = np.searchsorted(np.cumsum(widths), np.arange(int(widths.sum())), side="right")
    L = len(table)
    cum = np.cumsum(widths)
    n_y = int(np.searchsorted(cum, per)) + 1            # the rows along y: widths summing to the period,
    wy = widths[:n_y].copy()                             # so the houses run on across the tile's wrap
    wy[-1] -= int(wy.sum() - per)
    if wy[-1] < 6:
        wy[-2] += wy[-1]
        wy = wy[:-1]
    table_y = np.searchsorted(np.cumsum(wy), np.arange(per), side="right")
    offx = (hash01(cell, 2) * L).astype(np.int64)
    offy = (hash01(cell, 3) * per).astype(np.int64)
    corner = (dxd < ring) & (dyd < ring)
    hrow = dyd <= dxd
    key = np.where(corner, 1_000_000 + cell * 8 + sx * 2 + sy,
                   np.where(hrow, 2_000_000 + cell * 4000 + sy * 2000 + table[(xi + offx) % L],
                            3_000_000 + cell * 4000 + sx * 2000 + table_y[(yi + offy) % per]))
    court = built & (d > ring)
    # inner wings across some courtyards (a row of houses with its own ridge)
    vmid = np.array([0.0] + [(a + b) / 2 for a, b in zip(vc[:-1], vc[1:])] + [W])
    cx_cell = np.take(vmid, np.clip(col, 0, len(vmid) - 1))
    hmid = np.array([(hc[-1] - per + hc[0]) / 2] + [(a + b) / 2 for a, b in zip(hc[:-1], hc[1:])]) if hc else np.array([per / 2])
    cy_cell = np.take(hmid, np.digitize(yy, hc) % len(hmid))
    has_wing = hash01(cell, 4) < 0.7
    vert = hash01(cell, 13) < 0.5
    wdist = np.where(vert, np.abs(xx - cx_cell - (hash01(cell, 5) - 0.5) * 20),
                     np.abs(pdy(yy, cy_cell + (hash01(cell, 5) - 0.5) * 16, per)))
    wing = court & has_wing & (wdist < ring / 2)
    wing_rh = np.clip(ring / 2 - wdist, 0, None)
    court &= ~wing
    roofm = built & ~court
    rh = 0.5 * np.clip(ring / 2 - np.abs(np.minimum(d, ring) - ring / 2), 0, None)
    rh = np.where(wing, 0.5 * wing_rh, rh)
    key = np.where(wing, 4_000_000 + cell * 4000 + np.where(vert, table_y[(yi + offy) % per], 2000 + table[(xi + offx) % L]), key)
    eave = 3.0 + 3.6 * hash01(key, 6) + 1.2 * hash01(cell, 7)
    old = plan.get("old", 0.45)
    hm, hb = hash01(key, 8), hash01(cell, 9)
    mat = np.where(hm < old * 0.6, 0, np.where(hm < old, 1, np.where(hm < old + (1 - old) * 0.48, 2,
                                                                     np.where(hm < 0.97, 3, 4))))
    dom = np.where(hb < old * 0.85, np.where(hb < old * 0.55, 0, 1),
                   np.where(hb < old * 0.85 + (1 - old * 0.85) * 0.5, 2, 3))
    mat = np.where(hash01(key, 14) < 0.72, dom, mat)
    gy = (np.roll(rh, -1, 0) - np.roll(rh, 1, 0)) / 2
    gx = np.gradient(rh, axis=1)
    nrm = np.sqrt(gx * gx + gy * gy + 1)
    lam = (gx * 0.55 + gy * 0.65 + 0.75) / nrm / 1.135
    shade = np.round((0.40 + 0.85 * lam) * 10) / 10
    colr = MATS[mat] * (0.95 + 0.10 * np.round(hash01(key, 10)))[..., None] * shade[..., None]
    ridge = roofm & np.where(wing, wing_rh > ring / 2 - 0.8, np.abs(np.minimum(d, ring) - ring / 2) < 0.8)
    colr[ridge] *= 1.1
    colr[roofm & (d < 1.0)] *= 0.84
    party = roofm & ((key != np.roll(key, -1, 1)) & np.roll(roofm, -1, 1) |
                     (key != np.roll(key, -1, 0)) & np.roll(roofm, -1, 0))
    colr[party] *= 0.78
    garden = court & (hash01(cell, 11) < 0.55)
    return roofm, rh, eave, colr, key, court, garden, cell, hrow, d, ring


def _row_values(v, per):
    """A per-row strength (a scalar or an array of ``per`` values) as a (per, 1) column."""
    a = np.asarray(v, float)
    return (np.full(per, float(a)) if a.ndim == 0 else a)[:, None]


@lru_cache(maxsize=None)
def tree_sprites(night):
    return [r03.foliage_sprite(sz, 1520 + k, GREENS, night=night) for k, sz in enumerate((9, 11, 12, 13, 10, 14))]


def city(plan, per):
    """The city texture of a plan, 480 x ``per`` (periodic in y), as a float RGB array plus the kind
    map. Plan keys: avenues / vstreets (x0, x1), hstreets (y0, y1, xa, xb), diagonals ((x0, y0), (x1,
    y1), half width), circles (cx, cy, square r, street r), squares / cobble / parks (x0, x1, y0, y1),
    shore (a function of y: the lake left of it), river (y centre function, half width, x from),
    bridges ((x0, x1)), old (tile-roof share), damage, creep, roots [(points, width)], lamps / canopy /
    grade (a scalar or one value per row: lamp strength, the canopy shadow, the daylight's seconds),
    salt (block hash), extra (a function(img, kind, ctx) drawing a set piece's own things)."""
    yy, xx = np.mgrid[0:per, 0:W].astype(np.float64)
    xi, yi = xx.astype(np.int64), yy.astype(np.int64)
    kind = np.zeros((per, W), np.int8)
    for x0, x1 in plan.get("avenues", []):
        kind[:, x0:x1] = STREET
    hs = plan.get("hstreets", [])
    hs_bounds = sorted({0, per} | {y for y0, y1, _, _ in hs for y in (y0, y1)})
    for i, (x0, x1) in enumerate(plan.get("vstreets", [])):
        for j in range(len(hs_bounds) - 1):
            jj = 0 if j == len(hs_bounds) - 2 else j          # the last segment runs on into the first
            if hash01(i * 31 + jj + 97 * plan.get("salt", 0), 7) < plan.get("gaps", 0.18):
                continue
            kind[hs_bounds[j]:hs_bounds[j + 1], x0:x1] = STREET
    vs = plan.get("avenues", []) + plan.get("vstreets", [])
    vs_bounds = sorted({0, W} | {x for x0, x1 in vs for x in (x0, x1)})
    for i, (y0, y1, xa, xb) in enumerate(hs):
        for j in range(len(vs_bounds) - 1):
            a, b = max(xa, vs_bounds[j]), min(xb, vs_bounds[j + 1])
            whole = y0 <= 0 or y1 >= per                         # the wrap's street has no gaps
            if a >= b or (not whole and hash01(i * 17 + j + 97 * plan.get("salt", 0), 9) < plan.get("gaps", 0.18) * 0.8):
                continue
            rows = np.arange(y0, y1) % per
            kind[rows, a:b] = STREET
    for p0, p1, hw in plan.get("diagonals", []):
        dd, _ = seg_dist(xx, yy, p0, p1, per)
        kind[dd < hw] = STREET
    circle_d = []
    for cx, cy, rs, rr in plan.get("circles", []):
        rpl = np.hypot(xx - cx, pdy(yy, cy, per))
        kind[rpl < rr] = STREET
        kind[rpl < rs] = PLAZA
        circle_d.append((rpl, cx, cy, rs, rr))
    for x0, x1, y0, y1 in plan.get("squares", []):
        m = (xx >= x0) & (xx < x1) & (np.abs(pdy(yy, (y0 + y1) / 2, per)) < (y1 - y0) / 2)
        kind[m] = SQUARE
    cobble = np.zeros((per, W), bool)
    for x0, x1, y0, y1 in plan.get("cobble", []):
        cobble |= (xx >= x0) & (xx < x1) & (np.abs(pdy(yy, (y0 + y1) / 2, per)) < (y1 - y0) / 2)
    kind[cobble] = SQUARE
    for x0, x1, y0, y1 in plan.get("parks", []):
        m = (xx >= x0) & (xx < x1) & (np.abs(pdy(yy, (y0 + y1) / 2, per)) < (y1 - y0) / 2)
        kind[m] = PARK
    lake = np.zeros((per, W), bool)
    shore = None
    if plan.get("shore"):
        shore = plan["shore"](yy)
        lake = xx < shore
        kind[(~lake) & (xx < shore + 18)] = STREET               # quay road
        kind[(~lake) & (xx < shore + 6)] = PROM                  # promenade
        kind[lake] = WATER
    river = np.zeros((per, W), bool)
    bridge = np.zeros((per, W), bool)
    if plan.get("river"):
        rc_f, rhw, rx0 = plan["river"]
        rc = rc_f(xx)
        rdist = np.abs(pdy(yy, rc, per))
        land = (~lake) & (xx >= rx0)
        kind[land & (rdist < rhw + 13)] = STREET
        kind[land & (rdist < rhw + 3)] = QUAY_K
        river = (xx >= (shore - 3 if shore is not None else rx0)) & (rdist < rhw)
        kind[river] = WATER
        for bx0, bx1 in plan.get("bridges", []):
            bridge |= river & (xx >= bx0) & (xx < bx1)
        for cxp, w2, h2 in plan.get("isles", []):
            isle = ((xx - cxp) / w2) ** 2 + (pdy(yy, rc_f(cxp), per) / h2) ** 2 < 1
            kind[isle] = PARK
        kind[bridge] = STREET
    if plan.get("rubble"):
        pass
    roofm, rh, eave, colr, key, court, garden, cell, hrow, dfree, ring = _houses(kind, per, plan, xx, yy, xi, yi)
    damage = plan.get("damage", 0.0)
    ruin = roofm & (hash01(cell * 7 + (key % 97), 21) < damage * 0.22)        # collapsed houses
    noise = r02.periodic_fbm(W, per, 32, 1502 + plan.get("salt", 0), octaves=3)
    img = np.zeros((per, W, 3))
    img[:] = ASPHALT
    dbx, _ = axis_dist((kind == 0) | (kind == PARK), 1)
    dby, _ = axis_dist((kind == 0) | (kind == PARK), 0)
    walk = (kind == STREET) & (np.minimum(dbx, dby) <= 2)
    img[walk] = WALK
    pave = (xi % 6 == 0) | (yi % 6 == 0)
    sq = kind == SQUARE
    img[sq] = np.where(pave[sq][:, None], (78, 76, 86), (88, 86, 95))
    img[cobble] = np.where(((xi + yi) % 4 == 0)[cobble][:, None], (92, 88, 90), (102, 98, 98))
    for rpl, cx, cy, rs, rr in circle_d:
        pl = rpl < rs
        img[pl] = np.where(((rpl[pl] % 9) < 1)[:, None], (80, 78, 88), (92, 90, 99))
    pk = kind == PARK
    img[pk] = np.array((36, 58, 40)) + np.round(noise[pk] * 2)[:, None] / 2 * np.array((10, 18, 10))
    img[kind == QUAY_K] = QUAY
    pm = kind == PROM
    img[pm] = np.where(pave[pm][:, None], (82, 80, 88), (90, 88, 96))
    img[court] = COURT
    img[garden] = GARDEN
    wtr = kind == WATER
    if wtr.any():
        wv = r03.stretched(W, per, [30, 15], 1406, 4)
        lk = np.array((22, 34, 60)) + np.round(noise * 3)[..., None] / 3 * np.array((10, 14, 22))
        lk = lk + (np.abs(wv - 0.5) < 0.05)[..., None] * np.array((14, 18, 26))
        if shore is not None:
            lk = np.where((lake & (xx > shore - 6))[..., None], lk + np.array((8, 12, 16)), lk)
        img[wtr] = lk[wtr]
    img[roofm] = colr[roofm]
    hgt = np.where(roofm, eave + rh, 0.0)
    # chimneys and skylights (deterministic per pixel, so a set piece's render matches the tile's)
    cand = roofm & (dfree > 2) & (dfree < ring - 2)
    ch = cand & (hash01(yi * 977 + xi, 31 + plan.get("salt", 0)) < 1 / 230)
    for y, x in zip(*np.nonzero(ch)):
        yr = np.arange(y, y + 2) % per
        img[yr, x:min(W, x + 2)] = (74, 68, 70)
        img[y, x] = (112, 104, 102)
        hgt[yr, x:min(W, x + 2)] += 1.5
    sk = cand & (hash01(yi * 977 + xi, 37 + plan.get("salt", 0)) < 1 / 260)
    for y, x in zip(*np.nonzero(sk)):
        w_, h_ = (3, 2) if hrow[y, x] else (2, 3)
        c = (176, 146, 96) if hash01(y * 7 + x, 41) < 0.2 else (66, 80, 100)
        img[np.arange(y, y + h_) % per, x:min(W, x + w_)] = c
    # collapsed houses: rubble heaps, scorched, low
    if ruin.any():
        rub = np.array((70, 62, 60)) + (noise[..., None] - 0.5) * 50 + \
            (hash01(yi * 977 + xi, 43)[..., None] - 0.5) * 30
        img[ruin] = rub[ruin] * 0.85
        hgt[ruin] = 1.0 + 2.0 * hash01(yi * 977 + xi, 47)[ruin]
    # the thick roots (the Spire's, as thick as trams), lit as tubes, with their teal seams
    root_h = np.zeros((per, W))
    root_k = np.zeros((per, W))
    for pts, width in plan.get("roots", []):
        wrap = plan.get("wrap_roots", True)
        dd, along = poly_dist(xx, yy, pts, per if wrap else None)
        swell = 0.9 + 0.1 * np.sin(along * TAU * 9 + width) + 0.06 * np.sin(along * TAU * 23)
        wv = width * swell * (1.0 if wrap else (1.0 - 0.55 * along ** 1.3))
        hh = np.sqrt(np.clip(wv * wv / 4 - dd * dd, 0, None)) * 0.9
        root_k = np.where(hh > root_h, np.clip(1 - dd / np.maximum(wv / 2, 1e-3), 0, 1), root_k)
        root_h = np.maximum(root_h, hh)
    if (root_h > 0).any():
        gy = (np.roll(root_h, -1, 0) - np.roll(root_h, 1, 0)) / 2
        gx = np.gradient(root_h, axis=1)
        nrm = np.sqrt(gx * gx + gy * gy + 1)
        lam = np.clip((gx * 0.55 + gy * 0.65 + 0.75) / nrm / 1.135, 0, 1.3)
        rc_ = ROOT_BODY * (0.45 + 0.8 * lam)[..., None]
        rc_ *= (0.9 + 0.2 * noise)[..., None]
        on = root_h > 0.15
        seam = on & (root_k > 0.9) & (hash01((yi // 3) * 977 + xi // 3, 53) < 0.55)
        img[on] = rc_[on]
        img[seam] = img[seam] * 0.5 + ROOT_GLOW * 0.55
        hgt = np.where(on, np.maximum(hgt, root_h + 1.0), hgt)
        # the paving cracked round them
        near = (~on) & (kind != WATER) & (kind != 0)
        c1, c2, idx = s6.voronoi_periodic(W, per, max(40, per // 12), 1471 + plan.get("salt", 0)) \
            if per <= 1200 else _cells_big(per)
        crack = near & ((c2 - c1) < 1.1) & (hash01(idx, 55) < 0.6)
        img[crack] *= 0.55
    if damage > 0:
        c1, c2, idx = s6.voronoi_periodic(W, per, max(40, per // 10), 1473 + plan.get("salt", 0)) \
            if per <= 1200 else _cells_big(per, 1473)
        paved = (kind != 0) & (kind != WATER) & (kind != PARK) & ~ruin
        crack = paved & ((c2 - c1) < 1.0) & (hash01(idx, 57) < damage * 0.7)
        img[crack] *= 0.6
    # creep: veins along the gutters and patches over the roofs
    creep = plan.get("creep", 0.0)
    if creep > 0:
        cn = r02.periodic_fbm(W, per, 16, 1481 + plan.get("salt", 0), octaves=3)
        gutter = (kind == STREET) & (np.minimum(dbx, dby) <= 1) & (cn > 1 - 0.55 * creep)
        img[gutter] = img[gutter] * 0.35 + CREEP * 0.65
        vein = gutter & (hash01(yi * 977 + xi, 61) < 0.08)
        img[vein] = img[vein] * 0.5 + VEIN * 0.4
        roofc = roofm & ~ruin & (cn > 1 - 0.32 * creep)
        img[roofc] = img[roofc] * 0.45 + CREEP * 0.55
        rv = roofc & (hash01(yi * 977 + xi, 63) < 0.05)
        img[rv] = img[rv] * 0.5 + VEIN * 0.45
    shad = cast_shadow(hgt, length=0.9)
    img *= (1 - 0.45 * shad)[..., None]
    if bridge.any():
        img[bridge] = (64, 64, 72)
        rail = bridge & ~(np.roll(bridge, 1, 1) & np.roll(bridge, -1, 1))
        img[rail] = (116, 114, 122)
        bsh = wtr & ~bridge & (np.roll(bridge, 3, 1) | np.roll(bridge, 2, 1))
        img[bsh] *= 0.7
    # road markings: the avenues' dashed centre lines, zebra crossings
    for x0, x1 in plan.get("avenues", []):
        dash = (np.abs(xx - (x0 + x1) / 2 + 0.5) < 0.6) & ((yi // 5) % 2 == 0) & (kind == STREET)
        img[dash & ~walk & (root_h < 0.15)] = (118, 116, 108)
    ctx = {"kind": kind, "xx": xx, "yy": yy, "per": per, "walk": walk, "roofm": roofm, "noise": noise,
           "root_h": root_h}
    out = raster.to_rgba_image(np.clip(img, 0, 255))
    d2 = ImageDraw.Draw(out)
    # parked (abandoned) cars along the side streets, deterministic per place
    pcols = [(96, 62, 60), (70, 80, 98), (122, 122, 126), (60, 66, 62), (138, 132, 118), (52, 56, 76)]
    for x0, x1 in plan.get("vstreets", []) + plan.get("avenues", []):
        for side in (0, 1):
            xc = x0 + 1 if side == 0 else x1 - 4
            y = 0
            while y < per:
                if hash01(y * 13 + xc * 7, 71) < 0.07 and all(kind[(y + k) % per, xc + 1] == STREET and
                                                               root_h[(y + k) % per, xc + 1] < 0.1 for k in range(7)):
                    c = pcols[int(hash01(y * 5 + xc, 73) * len(pcols))]
                    for dy in (-per, 0):
                        d2.rectangle([xc + 1, y + dy + 1, xc + 3, y + dy + 6], fill=(30, 30, 38))
                        d2.rectangle([xc, y + dy, xc + 2, y + dy + 5], fill=c)
                        d2.line([xc, y + dy + 1, xc + 2, y + dy + 1], fill=(30, 34, 46))
                    y += 9
                y += 1
    if plan.get("extra_ground"):
        plan["extra_ground"](out, ctx)
    # trees: the promenade, the avenues, the parks (jittered grid), the gardens
    spots = []
    if shore is not None:
        for y in range(4, per, 13):
            spots.append((float(plan["shore"](np.array(float(y)))) + 3, y))
    for x0, x1 in plan.get("avenues", []):
        for y in range(6, per, 16):
            for x in (x0 + 1.5, x1 - 1.5):
                if kind[y, int(x)] == STREET and root_h[y, int(x)] < 0.1 and not any(
                        rpl[y, int(x)] < rr + 4 for rpl, _, _, _, rr in circle_d):
                    spots.append((x, y))
    for y in range(4, per, 9):
        for x in range(4, W, 9):
            jx, jy = x + (hash01(y * 977 + x, 81) - 0.5) * 6, y + (hash01(y * 977 + x, 83) - 0.5) * 6
            iy, ix = int(jy) % per, int(np.clip(jx, 0, W - 1))
            if kind[iy, ix] == PARK and hash01(y * 977 + x, 85) < 0.75 and root_h[iy, ix] < 0.1:
                spots.append((jx, jy))
            elif garden[iy, ix] and hash01(y * 977 + x, 87) < 0.08:
                spots.append((jx, jy))
    spots += plan.get("tree_spots", [])
    trees = tree_sprites(0.85)
    for x, y in spots:
        t_ = trees[int(hash01(int(x * 7 + y * 13), 12) * len(trees))]
        for dy in (-per, 0, per):
            sprite.paste_center(out, sprite.shadow_of(t_, opacity=0.5, blur=1.0), x + 3, y + dy + 4)
            sprite.paste_center(out, t_, x, y + dy)
    arr = np.asarray(out).astype(np.float64)[..., :3]
    # the canopy's shadow: mottled (the concept's), per row
    can = _row_values(plan.get("canopy", 1.0), per)
    if (can > 0).any():
        c1, c2, idx = s6.voronoi_periodic(W, per, max(46, int(46 * per / 540)), 1403) if per <= 1200 else _cells_big(per, 1403)
        veinm = np.clip(1 - (c2 - c1) / 9.0, 0, 1)
        holes = (idx % 4 == 0)
        lightm = s6.pblur_mask(np.clip(1 - veinm, 0, 1) * np.where(holes, 1.0, 0.5), 7.0)
        shaded = arr * (0.66 + 0.40 * lightm[..., None]) + np.array((14, 6, 26)) * (1 - lightm[..., None])
        arr = arr * (1 - can[..., None]) + shaded * can[..., None]
    # the sodium lamps (after the canopy: they are lights)
    lamps_on = _row_values(plan.get("lamps", 1.0), per)
    heads = []
    for x0, x1 in plan.get("avenues", []):
        for y in range(3, per, 22):
            for x, ph in ((x0 + 0.5, 0), (x1 - 1.5, 11)):
                yy_ = (y + ph) % per
                if kind[yy_, int(x)] == STREET and root_h[yy_, int(x)] < 0.1:
                    heads.append((x, yy_))
    if shore is not None:
        for y in range(10, per, 20):
            heads.append((float(plan["shore"](np.array(float(y)))) + 6.5, y))
    for i, (x0, x1) in enumerate(plan.get("vstreets", [])):
        for y in range(5 + 9 * i, per, 34):
            if kind[y, x1 - 1] == STREET and root_h[y, x1 - 1] < 0.1:
                heads.append((x1 - 1, y))
    for y0, y1, xa, xb in hs:
        if y0 <= 0 or y1 >= per:
            continue                                   # the wrap's street: no lamps (plain asphalt at the seam)
        ly = y0 % per
        for x in range(xa + 9, xb, 48):
            if kind[ly, x] == STREET and root_h[ly, x] < 0.1:
                heads.append((x, ly))
    for rpl, cx, cy, rs, rr in circle_d:
        for k in range(16):
            a = TAU * k / 16
            hx, hy = cx + (rr - 1) * np.cos(a), (cy + (rr - 1) * np.sin(a)) % per
            if 0 <= hx < W:
                heads.append((hx, hy))
    heads += plan.get("lamp_heads", [])
    lampm = np.zeros((per, W))
    for x, y in heads:
        if hash01(int(x) * 31 + int(y), 89) < plan.get("lamp_share", 1.0):
            lampm[int(y) % per, int(np.clip(x, 0, W - 1))] = 1
    pool = s6.pblur_mask(lampm, 3.0)
    pool = pool / (pool.max() or 1)
    arr = arr + np.clip(pool * 1.4, 0, 1)[..., None] * LAMP * lamps_on[..., None]
    lit = (lampm > 0) & (lamps_on[:, 0][:, None] > 0.5)
    arr[lit] = (236, 204, 140)
    if plan.get("extra_top"):
        arr = plan["extra_top"](arr, ctx)
    # the daylight's grade, per row (seconds since the death)
    grade = plan.get("grade")
    if grade is not None:
        g = np.asarray(grade, float) * np.ones(per)
        q = np.round(g * 4) / 4
        for v in np.unique(q):
            rows = q == v
            if v > 0:
                arr[rows] = dl.grade(DAYLIGHT, arr[rows], float(v))
    return np.clip(arr, 0, 255), kind


def _cells_big(per, seed=1403):
    """voronoi_periodic for a tall strip: points in proportion, computed in bands (memory)."""
    n = max(46, int(46 * per / 540))
    rng = np.random.default_rng(seed)
    pts = np.stack([rng.uniform(0, W, n), rng.uniform(0, per, n)], axis=1)
    pts = np.concatenate([pts, pts + [0, per], pts - [0, per]])
    d1 = np.full((per, W), 1e9)
    d2 = np.full((per, W), 1e9)
    idx = np.zeros((per, W), dtype=int)
    for y0 in range(0, per, 256):
        y1 = min(per, y0 + 256)
        yy, xx = np.mgrid[y0:y1, 0:W].astype(np.float64)
        near = pts[(pts[:, 1] > y0 - 200) & (pts[:, 1] < y1 + 200)]
        ids = np.nonzero((pts[:, 1] > y0 - 200) & (pts[:, 1] < y1 + 200))[0]
        a1, a2, ai = d1[y0:y1], d2[y0:y1], idx[y0:y1]
        for i, (px, py) in zip(ids, near):
            d = np.hypot(xx - px, yy - py)
            closer = d < a1
            a2[:] = np.where(closer, a1, np.minimum(a2, d))
            ai[:] = np.where(closer, i % n, ai)
            a1[:] = np.where(closer, d, a1)
    return d1, d2, idx


# --------------------------------------------------------------------------- the plans

def _hs(ys, h=9, xa=0, xb=W, per=TILE_H):
    """Cross streets at ``ys``, plus the one the tile wraps in the middle of (as Level 08's tiles: the
    seam falls on plain asphalt, never on a block's edge)."""
    wide = 26                                           # a wide cross street: the blocks' shadows end before the wrap
    return [(0, wide // 2, 0, W)] + [(y, y + h, xa, xb) for y in ys] + [(per - (wide - wide // 2), per, 0, W)]


def _centres(plan):
    """The block grid's centre lines for the per-block hashes."""
    av = plan.get("avenues", [])
    vs = plan.get("vstreets", [])
    vcen = [(a + b) / 2 for a, b in av + vs]
    hcen = [(a + b) / 2 for a, b, _, _ in plan.get("hstreets", [])]
    return dict(plan, vcen=vcen or [W / 2], hcen=hcen or [0.0])


def lake_shore(y):
    u = TAU * np.asarray(y, float) / TILE_H
    return 128 + 16 * np.sin(2 * u + 0.7) + 7 * np.sin(5 * u + 1.9)


def plan_lakefront():
    return _centres({
        "salt": 1, "shore": lake_shore, "old": 0.32, "creep": 0.08, "canopy": 0.55,
        "avenues": [(290, 312)], "vstreets": [(208, 216), (372, 381), (436, 444)],
        "hstreets": _hs((98, 176, 256, 334, 414, 494, 572, 652, 732, 812, 878)),
        "parks": [(176, 200, 300, 380), (176, 200, 700, 760)],
    })


def plan_old_town(salt=2):
    return _centres({
        "salt": salt, "old": 0.62, "creep": 0.3,
        "avenues": [(290, 312)], "vstreets": [(36, 44), (118, 127), (200, 208), (372, 381), (436, 444)],
        "hstreets": _hs((84, 156, 232, 298, 372, 444, 516, 590, 664, 740, 812, 876)),
        "diagonals": [((44, 640), (200, 452), 5.5)],
        "parks": [(130, 196, 690, 736), (382, 432, 160, 226)],
    })


def plan_root_field():
    roots = [(periodic_curve(TILE_H, 96, [26, 10, 5], 1601), 24),
             (periodic_curve(TILE_H, 262, [34, 14, 6], 1603), 28),
             (periodic_curve(TILE_H, 396, [22, 12, 4], 1605), 20)]
    return _centres({
        "salt": 3, "old": 0.5, "creep": 0.8, "damage": 0.75, "roots": roots, "lamp_share": 0.45,
        "avenues": [(232, 254)], "vstreets": [(56, 64), (150, 158), (330, 338), (410, 418)],
        "hstreets": _hs((90, 166, 252, 330, 412, 494, 572, 652, 734, 812, 874)),
        "parks": [(160, 228, 600, 660)],
    })


def plan_daylight():
    p = plan_old_town(salt=6)
    p.update({"lamps": 0.0, "canopy": 0.0, "grade": 12.0, "creep": 0.25})
    return p


# --------------------------------------------------------------------------- the strip (approach, arena, daylight)

FRAME_TOP = HALT_SCROLL + SCREEN        # the arena frame's top edge on the ground layer
STRIP_PIECES = 4
STRIP_BOT = int(round(seam_pos(ARENA - 1)))                   # the approach's seam (under a boulevard)
# the strip reaches 9.6 s of the daylight's scroll past the frame (step H7b: at the daylight's own speed,
# no longer the level's 140, so its top and the daylight tiles still come into view; 40 px/s in H7b, 25 in H9)
STRIP_H = int(math.ceil((FRAME_TOP + 9.6 * speed(ARENA + 1) - STRIP_BOT) / (64 * STRIP_PIECES))) * 64 * STRIP_PIECES
STRIP_TOP = STRIP_BOT + STRIP_H
PIECE_H = STRIP_H // STRIP_PIECES


def strip_span(k):
    """Strip piece k's rows on the ground layer, [bottom, top): its share plus one row over each
    neighbour; that outermost row is transparent (no piece edge on screen, BackdropAssetsTest), and the
    neighbour's own row there is opaque, so the strip has no gap; its ends lie under boulevards."""
    a = STRIP_BOT + k * PIECE_H - (1 if k > 0 else 0)
    b = STRIP_BOT + (k + 1) * PIECE_H + (1 if k < STRIP_PIECES - 1 else 0)
    return a, b


def roots_of_spire():
    """The six roots' directions (screen angles, y down) from the boss's pivots, or the concept's."""
    path = ROOT / "assets" / "pivots" / "siege-spire.json"
    if path.exists():
        import json
        return [math.radians(r["angle_deg"]) for r in json.loads(path.read_text())["roots"]]
    return [math.radians(8.6 + 60 * k) for k in range(6)]


def strip_t(row):
    """The daylight's seconds since the death that a strip row (0 = top) gets: by its distance past the
    arena frame's top, at the daylight's scroll speed (0 inside the frame and below)."""
    pos = STRIP_TOP - 1 - np.asarray(row, float)
    return np.clip(0.5 + (pos - FRAME_TOP) / speed(ARENA + 1), 0, 12) * (pos >= FRAME_TOP)


def plan_strip():
    per = STRIP_H
    ax, ay = ANCHOR_X, STRIP_TOP - ANCHOR_POS            # the anchor in strip image coords (y down)
    bot = per
    diag = []
    for a in roots_of_spire():                           # six avenues along the roots
        diag.append(((ax, ay), (ax + 420 * math.cos(a), ay + 420 * math.sin(a)), 12))
    diag += [((ax, ay), (ax, bot + 20), 11), ((ax, ay), (ax, -20), 11),            # the approach's spine, north
             ((54, bot + 20), (ax - 20, ay + 60), 7), ((426, bot + 20), (ax + 20, ay + 60), 7),   # converging
             ((ax - 20, ay - 60), (40, -20), 7), ((ax + 20, ay - 60), (440, -20), 7)]
    ys = list(range(30, per, 82))
    hstreets = [(y, y + 9, 0, W) for y in ys if abs(y - ay) > 120]
    rows = np.arange(per)
    gt = strip_t(rows)
    k = np.array([dl.ramp(v, 1.5, 9.0) for v in gt])
    lamps = 1 - np.array([dl.ramp(v, 0.5, 3.0) for v in gt])
    # the roots: the Spire's own (sprites) reach ~200 px; the backdrop's run on along the six avenues,
    # and the root field's come up the approach to the plaza
    roots = []
    for i, a in enumerate(roots_of_spire()):
        r0, r1 = 150, 520 + 80 * (i % 3)
        pts = [(ax + r * math.cos(a) + 6 * math.sin(r / 37 + i), ay + r * math.sin(a) + 6 * math.cos(r / 41 + i))
               for r in range(r0, r1, 8)]
        roots.append((pts, 24 - 2 * (i % 2)))
    for x0, w, end in ((96, 24, 118), (262, 28, 84), (396, 20, 52)):
        ex, ey = ax + 150 * math.cos(math.radians(end)), ay + 150 * math.sin(math.radians(end))
        pts = []
        for y in range(bot + 10, int(ey), -8):
            u = max(0.0, (bot - y) / max(1.0, bot - ey))
            x = x0 + (ex - x0) * u ** 1.4 + 6 * math.sin(y / (47 + w) + x0) * (1 - u)
            pts.append((x, y))
        roots.append((pts, w))
    plan = {
        "salt": 4, "old": 0.5, "creep": 0.55, "damage": 0.35, "wrap_roots": False, "roots": roots,
        "avenues": [], "vstreets": [(40, 48), (128, 136), (344, 352), (432, 440)], "hstreets": hstreets,
        "diagonals": diag, "circles": [(ax, ay, 78, 96)], "gaps": 0.12,
        "canopy": 1 - k, "lamps": lamps, "grade": gt, "lamp_share": 0.8,
        "extra_ground": lambda img, ctx: _plaza(img, ctx, ax, ay),
    }
    return _centres(plan)


def _plaza(img, ctx, ax, ay):
    """The Concord plaza: the broken ring of flagpoles with their UTC flags, the rotunda's cracked
    foundation (under the trunk), the creep mound's fringe."""
    d = ImageDraw.Draw(img)
    for k in range(16):
        a = TAU * (k + 0.25) / 16
        fx, fy = ax + 70 * math.cos(a), ay + 70 * math.sin(a)
        if hash01(k, 91) < 0.35:                       # felled
            d.line([fx, fy, fx + 9 * math.cos(a + 1.2), fy + 9 * math.sin(a + 1.2)], fill=(110, 110, 118, 255))
            continue
        d.line([fx, fy, fx + 4, fy + 1], fill=(60, 104, 190, 255), width=2)
        d.point([(fx, fy)], fill=(200, 200, 210, 255))
    for r in (40, 54):
        d.ellipse([ax - r, ay - r, ax + r, ay + r], outline=(70, 68, 78, 255))
    e6 = s10.e6
    e6.creep(img, int(ax), int(ay), 120, 1491)


# --------------------------------------------------------------------------- quantising (one palette per image set)

def palette_of(arr, n):
    img = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8).reshape(-1, 1, 3) if arr.ndim == 2 else
                          np.clip(arr, 0, 255).astype(np.uint8))
    q = img.quantize(colors=n, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE)
    pal = np.array(q.getpalette()[:3 * n], float).reshape(-1, 3)
    return pal[:len(np.unique(np.asarray(q)))] if len(np.unique(np.asarray(q))) < n else pal


def map_palette(arr, pal):
    flat = arr.reshape(-1, 3)
    out = np.empty_like(flat)
    for i in range(0, len(flat), 65536):
        c = flat[i:i + 65536]
        d = ((c[:, None, :] - pal[None, :, :]) ** 2).sum(-1)
        out[i:i + 65536] = pal[np.argmin(d, 1)]
    return out.reshape(arr.shape)


def rgba(arr, alpha=None):
    a = np.full(arr.shape[:2], 255, np.uint8) if alpha is None else alpha.astype(np.uint8)
    rgb = np.clip(arr, 0, 255).astype(np.uint8)
    rgb[a == 0] = 0
    return Image.fromarray(np.dstack([rgb, a]), "RGBA")


# --------------------------------------------------------------------------- the layout

PLANS = {"lakefront": plan_lakefront, "old-town": plan_old_town, "root-field": plan_root_field,
         "daylight": plan_daylight}
SECTION_GROUND = ["lakefront", "old-town", "root-field", "root-field", "root-field", "daylight"]
SECTION_TILES = [[g, "canopy"] for g in SECTION_GROUND]
TILE_SETS = {   # id -> (layer, height, drift, note)
    "lakefront": ("ground", TILE_H, None, "section 1: Lake Geneva at dusk on the left, the quays and the promenade, "
                                          "the 19th-century blocks; the canopy's shadow lighter (its edge)"),
    "old-town": ("ground", TILE_H, None, "section 2: perimeter blocks of row houses, tile roofs, sodium-lit streets, "
                                         "parks, creep veins in the gutters, the canopy's mottled shadow"),
    "root-field": ("ground", TILE_H, None, "section 3 (and under the strip in 4-5): streets broken by roots as thick "
                                           "as trams, cracked paving, collapsed houses, creep over the roofs"),
    "daylight": ("ground", TILE_H, None, "section 6 after the strip: the old town in the low sun (daylight look "
                                         f"{DAYLIGHT}), lamps out, the creep withered"),
    "haze-light": ("low-air", TILE_H, None, "light: thin spore haze, ~10 %, still on its layer"),
    "spore-haze": ("low-air", TILE_H, None, "medium: spore-haze banks lit violet from below, ~22 %, spores"),
    "canopy": ("high-air", SCREEN, None, "the canopy's teal veins (additive at 40 %), every section; period 540 as "
                                         "the game's canopy-tear frames"),
}


def car_hitbox_h():
    for g in DATA.get("ground_targets", []) or []:
        if g.get("sprite") == "armoured-car":
            return float(g.get("size", [40, 60])[1])
    return 60.0


def nest_hitbox_h():
    return float((enemy_data("spine-turret").get("hitbox") or [40, 40])[1])


def hive_hitbox_h():
    return float((enemy_data("hive-node").get("hitbox") or [60, 60])[1])


def tile_row(pos, per=TILE_H):
    """The tile image row (0 = top) that a layer position shows."""
    return per - 1 - (np.floor(pos).astype(np.int64) % per)


@lru_cache(maxsize=None)
def layout():
    """Where the set pieces go (ground positions and the tile rows under them)."""
    car_pos = target_pos(CAR_AT[0], car_hitbox_h())
    nest = [(x, target_pos(t, nest_hitbox_h())) for t, x in NEST_AT]
    hives = [(x, target_pos(t, hive_hitbox_h())) for t, x in HIVES_AT]
    molard_nest = [(x, target_pos(t, nest_hitbox_h())) for t, x in MOLARD_NEST_AT]
    first = BURSTS[0]
    return {"car": (float(CAR_AT[1]), car_pos), "nest": nest, "hives": hives, "molard_nest": molard_nest,
            "crater": (float(first["x"]), scroll_at(72.0) + SCREEN - 50)}


def overlay_rect(centre_pos, h):
    """(bottom position, height) of an overlay piece centred near ``centre_pos`` (whole pixels)."""
    bottom = int(round(centre_pos - h / 2))
    return bottom, h


OVERLAYS = {    # id -> (base tile, height, note)
    "rhone": ("lakefront", 168, "the Rhone leaving the lake, the Pont du Mont-Blanc under the armoured car, the "
                                "Ile Rousseau, the avenue's bridge"),
    "cathedral": ("old-town", 232, "Saint-Pierre on its cobbled square: nave, transept and apse, the turret nest's "
                                   "square in front; its towers are tower pieces"),
    "molard": ("old-town", 200, "the Place du Molard: the square under the two hive nodes, creep round them"),
}


def overlay_centre(name):
    lay = layout()
    if name == "rhone":
        return lay["car"][1]
    if name == "cathedral":
        return float(np.mean([p for _, p in lay["nest"]])) + 30
    return float(np.mean([p for _, p in lay["hives"]]))


def overlay_plan(name):
    """The base tile's plan with the set piece added, in that tile's image rows at the piece's place."""
    base_name, h, _ = OVERLAYS[name]
    plan = dict(PLANS[base_name]())
    c = overlay_centre(name)
    row = float(tile_row(c))                            # the piece's centre in the tile (y down)
    lay = layout()
    if name == "rhone":
        cx = lay["car"][0]
        plan["river"] = (lambda x, row=row: row + 4 * np.sin(np.asarray(x) / 60.0), 21, 0)
        ave = (289, 313)
        car_bridge = (cx - 14, cx + 14)
        if car_bridge[0] - ave[1] < 40 and ave[0] - car_bridge[1] < 40:      # one wide bridge
            plan["bridges"] = [(min(ave[0], car_bridge[0]), max(ave[1], car_bridge[1]))]
        else:
            plan["bridges"] = [car_bridge, ave]
        plan["isles"] = [(205, 22, 7)]
    elif name == "cathedral":
        xs = [x for x, _ in lay["nest"]]
        x0, x1 = min(xs) - 34, max(xs) + 34
        church_right = (x0 + x1) / 2 < 300
        cx = x1 + 28 if church_right else x0 - 28
        plan["cobble"] = plan.get("cobble", []) + [(x0, x1, row - 48, row + 40)]
        plan["squares"] = plan.get("squares", []) + [(min(x0, cx - 26), max(x1, cx + 26), row - 72, row + 64)]
        plan["extra_ground"] = lambda img, ctx, cx=cx, row=row: _cathedral(img, ctx, cx, row)
    else:
        xs = [x for x, _ in lay["hives"]]
        allx = xs + [x for x, _ in lay["molard_nest"]]
        x0, x1 = min(allx) - 46, max(allx) + 46
        plan["squares"] = plan.get("squares", []) + [(x0, x1, row - 52, row + 52)]
        plan["extra_ground"] = lambda img, ctx, xs=tuple(xs), row=row: _molard(img, ctx, xs, row)
    return plan


CATHEDRAL_TOWERS = (-11, 11)            # the two west towers' x offsets from the nave's axis


def cathedral_axis():
    lay = layout()
    xs = [x for x, _ in lay["nest"]]
    x0, x1 = min(xs) - 34, max(xs) + 34
    return (x1 + 28) if (x0 + x1) / 2 < 300 else (x0 - 28)


def _cathedral(img, ctx, cx, row):
    """Saint-Pierre: nave (along the screen), transept and apse with ridged slate roofs, drawn as the
    concept's height field; its two west towers are tower pieces (drawn in perspective by the game)."""
    per = ctx["per"]
    xx, yy = ctx["xx"], ctx["yy"]
    dy = pdy(yy, row, per)
    nave = (np.abs(xx - cx) < 11) & (dy >= -50) & (dy < 32)
    trans = (np.abs(dy - 10) < 7.5) & (np.abs(xx - cx) < 22)
    apse = np.hypot(xx - cx, dy - 32) < 11
    crh = np.maximum(np.where(nave, 0.62 * (11 - np.abs(xx - cx)), 0), np.where(trans, 0.62 * (7.5 - np.abs(dy - 10)), 0))
    crh = np.maximum(crh, np.where(apse & (dy >= 32), 0.62 * (11 - np.hypot(xx - cx, dy - 32)), 0))
    m = nave | trans | apse
    gy = (np.roll(crh, -1, 0) - np.roll(crh, 1, 0)) / 2
    gx = np.gradient(crh, axis=1)
    lam = (gx * 0.55 + gy * 0.65 + 0.75) / np.sqrt(gx * gx + gy * gy + 1) / 1.135
    col = MATS[3] * 0.85 * np.round((0.40 + 0.85 * lam) * 10)[..., None] / 10
    a = np.asarray(img).astype(np.float64)
    # the church's cast shadow (a tall building: 9 px eaves) before the roof
    sh = cast_shadow(np.where(m, 9 + crh, 0.0), length=0.9)
    a[..., :3] *= (1 - 0.45 * sh * ~m)[..., None]
    a[m, :3] = col[m]
    ridge = m & (((np.abs(xx - cx) < 0.8) & nave) | ((np.abs(dy - 10) < 0.8) & trans))
    a[ridge, :3] *= 1.12
    img.paste(Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA"))


def _molard(img, ctx, xs, row):
    """The Place du Molard: its fountain and the creep spread round the hive nodes' places."""
    d = ImageDraw.Draw(img)
    mx = (xs[0] + xs[-1]) / 2
    d.ellipse([mx - 6, row - 6, mx + 6, row + 6], fill=(84, 84, 96, 255), outline=(120, 120, 130, 255))
    d.ellipse([mx - 3, row - 3, mx + 3, row + 3], fill=(40, 60, 90, 255))
    for i, x in enumerate(xs):
        s10.e6.creep(img, int(x), int(row), 30, 1493 + i)


# --------------------------------------------------------------------------- renders

@lru_cache(maxsize=None)
def tile_float(name):
    arr, _ = city(PLANS[name](), TILE_H)
    return arr


@lru_cache(maxsize=None)
def tile_palette(name):
    return palette_of(tile_float(name), COLOURS)


def ground_tile(name):
    return rgba(map_palette(tile_float(name), tile_palette(name)))


def overlay_piece(name):
    """A set piece over its tile: the tile's render with the piece added, cut where it differs from the
    tile (so its edge falls on pixels identical to the tile under it); the tile's palette plus 16."""
    base_name, h, _ = OVERLAYS[name]
    base = tile_float(base_name)
    feat, _ = city(overlay_plan(name), TILE_H)
    bottom, h = overlay_rect(overlay_centre(name), h)
    rows = tile_row(np.arange(bottom + h - 1, bottom - 1, -1))          # image rows, top first
    b, f = base[rows], feat[rows]
    core = (np.abs(f - b) > 0.75).any(-1)
    core[:2] = core[-2:] = False
    core = np.array(Image.fromarray(core.astype(np.uint8) * 255).filter(ImageFilter.MaxFilter(3))) > 0
    core[:1] = core[-1:] = False
    pal = tile_palette(base_name)
    extra = palette_of(f[core].reshape(-1, 1, 3), 16) if core.any() else np.zeros((0, 3))
    q = map_palette(f, np.concatenate([pal, extra]))
    return rgba(q, core * 255)


@lru_cache(maxsize=None)
def strip_float():
    arr, _ = city(plan_strip(), STRIP_H)
    return arr


def strip_pieces():
    arr = strip_float()
    q = map_palette(arr, palette_of(arr, COLOURS))
    out = []
    for k in range(STRIP_PIECES):
        a, b = strip_span(k)
        rows = q[STRIP_TOP - b:STRIP_TOP - a]                # image rows, top first
        alpha = np.full(rows.shape[:2], 255)
        alpha[0] = alpha[-1] = 0
        out.append(rgba(rows, alpha))
    return out


def boulevard(day=False):
    """A tree-lined boulevard with tram rails across the field: it hides a seam between two tile sets."""
    h = 48
    a = np.zeros((h, W, 3))
    a[:] = ASPHALT
    a[8:12] = WALK
    a[36:40] = WALK
    for y in (18, 22, 26, 30):
        a[y] = (86, 84, 90)
    a[24, ::6] = (118, 116, 108)
    alpha = np.zeros((h, W), np.uint8)
    alpha[6:42] = 255
    img = rgba(a, alpha)
    trees = tree_sprites(0.85)
    for i, x in enumerate(range(6, W, 17)):
        for y in (9, 38):
            t_ = trees[int(hash01(i * 3 + y, 12) * len(trees))]
            sprite.paste_center(img, sprite.shadow_of(t_, opacity=0.5, blur=1.0), x + 3, y + 3)
            sprite.paste_center(img, t_, x, y)
    arr = np.asarray(img).astype(np.float64)
    al = arr[..., 3]
    al[:1] = al[-1:] = 0
    rgb = arr[..., :3]
    lam = np.zeros((h, W))
    for x in range(10, W, 48):
        lam[24, x] = 1
    pool = np.array(Image.fromarray((lam * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(3)), float) / 255
    if day:
        rgb = dl.grade(DAYLIGHT, rgb, 12.0)
    else:
        rgb = rgb * 0.82 + np.array((14, 6, 26)) * 0.18 + np.clip(pool / (pool.max() or 1) * 1.4, 0, 1)[..., None] * LAMP
    pal = palette_of(rgb[al > 0].reshape(-1, 1, 3), 24)
    return rgba(map_palette(rgb, pal), (al > 0) * 255)


def jet_deau():
    """The Jet d'Eau: its jetty from the quay with the little lighthouse, the plume leaning away from the
    screen centre (its x left of the centre: up and to the left; the perspective rule) and blown
    downwind; static (the scroll moves it). The concept's particle streams (scenes_r08.stream), two
    moments laid over each other so the column reads full."""
    w, h = 112, 144
    s8 = s10.s8
    fx, fy = 70.0, 110.0                                  # the fountain in the piece (the jetty runs right)
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.line([fx, fy, w - 2, fy + 14], fill=(104, 100, 108, 255), width=3)
    d.ellipse([fx - 3, fy - 3, fx + 3, fy + 3], fill=(118, 116, 124, 255))
    d.rectangle([w - 8, fy + 9, w - 4, fy + 13], fill=(170, 160, 150, 255))
    # the projection as the concept's tall structures at this piece's x, the screen's middle row
    k = CAMERA / (CAMERA - 0.45)
    sx = 60.0                                             # the piece's x on the field (README: over the lake)
    tx, ty = fx + (sx - W / 2) * (k - 1), fy + (MID - 297) * (k - 1) - 0.45 * 120
    for f in (0, 13):
        fm = s8.Foam(color=(222, 228, 236))
        ln = float(np.hypot(tx - fx, ty - fy)) + 1
        s8.stream(fm, f, fx, fy, tx - fx, ty - fy, ln, 5, 26, 0.5, 2.0, 1.0, 1.9, 0.9, 1462, wave=0.4)
        s8.stream(fm, f, tx, ty, 0.45, 0.89, 46, 3, 30, 1.5, 9.0, 1.4, 3.2, 0.7, 1460, wave=2)
        s8.stream(fm, f, fx, fy, 0.45, 0.89, 24, 3, 14, 1.0, 5.0, 1.0, 2.2, 0.5, 1461)
        img.alpha_composite(fm.done().crop((0, 0, w, h)))
    a = np.array(img)
    a[0] = a[-1] = 0
    a[:, 0] = a[:, -1] = 0
    return Image.fromarray(a, "RGBA")


@lru_cache(maxsize=None)
def burst_frames():
    return root_burst.make("a")


def root_crater():
    """The Ravager pack's crater: the root burst a's last ground decal under its last root frame."""
    dec, _, rot = burst_frames()
    img = Image.new("RGBA", (128, 112), (0, 0, 0, 0))
    sprite.paste_center(img, dec[-1], 64, 56)
    sprite.paste_center(img, rot[-1], 64, 52)
    a = np.array(img)
    a[0] = a[-1] = 0
    a[:, 0] = a[:, -1] = 0
    return Image.fromarray(a, "RGBA")


def traffic_car(n):
    """A car seen from above with its headlights (the concept's car_sprite), heading up (0) and down (1):
    lit only from above, so the down frame is the up one turned over."""
    sp = s10.car_sprite((118, 120, 128))
    sp = sp.resize((sp.width // 4, sp.height // 4), Image.BOX)
    canvas = Image.new("RGBA", (14, 28), (0, 0, 0, 0))
    canvas.alpha_composite(sp, ((14 - sp.width) // 2, (28 - sp.height) // 2))
    return [canvas, canvas.transpose(Image.ROTATE_180)]


def hammer_flight(n):
    """Hammer flight: three CDF bombers in a vee, heading up (0) and down (1): the Airstrike's bomber
    (tools/art/airstrike_bomber.py: frame 0 as it is; for heading down its model turned and rendered
    again, so the key light stays top left, the flames turned with it)."""
    import airstrike_bomber as ab
    import vfx_r09 as v9
    w, h = 136, 120
    out = []
    for k in range(n):
        if k == 0:
            one = artkit.load_frames("airstrike-bomber")[0]
        else:
            scene0, mats = v9.bomber_model()
            ang = k * TAU / n

            def scene(p, s=scene0, a=ang):
                return s(rotate_z(p, a))
            hi = sdf.render(scene, mats, (56 * 8, 64 * 8), 56 / ab.SCALE, center=(0.0, ab.CY / ab.SCALE),
                            z_top=3.0, steps=140)
            one = ab.flames(*ab.FLICKER[0]).rotate(-math.degrees(ang), resample=Image.NEAREST)
            one.alpha_composite(artkit.native(hi, 8))
            one = artkit.stepped_alpha(one)
        img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        sgn = 1 if k == 0 else -1
        for dx, dy in ((0, -26), (-40, 22), (40, 22)):
            img.alpha_composite(one, (int(w / 2 + dx - 28), int(h / 2 + sgn * dy - 32)))
        out.append(img)
    return artkit.quantize_set(out, 40)


TOWER_FOOT = (12, 12)
TOWER_H = 0.42


def tower_roof(foot, hgt):
    """The cathedral tower's roof at its drawn size: a verdigris copper pyramid, lit from the top left."""
    k = CAMERA / (CAMERA - hgt)
    rw, rh = round(foot[0] * k), round(foot[1] * k)
    yy, xx = np.mgrid[0:rh, 0:rw].astype(np.float64) + 0.5
    u, v = (xx - rw / 2) / (rw / 2), (yy - rh / 2) / (rh / 2)
    lit = np.where(np.abs(u) > np.abs(v), np.where(u < 0, 1.0, 0.55), np.where(v < 0, 0.9, 0.6))
    col = np.array((92, 134, 118)) * lit[..., None]
    col[(np.abs(np.abs(u) - np.abs(v)) < 0.12)] *= 0.8
    col[:, 0] = col[0, :] = (120, 114, 112)
    col[:, -1] = col[-1, :] = (70, 66, 72)
    return rgba(col)


def wall_stone():
    """The towers' wall texture: ashlar courses of pale limestone with lancet windows, lit (16 x 20)."""
    w, h = 16, 20
    a = np.zeros((h, w, 3))
    a[:] = (122, 116, 114)
    a[::4] = (104, 98, 98)
    for y in range(0, h, 4):
        off = 0 if (y // 4) % 2 else 4
        a[y:y + 4, off::8] = (104, 98, 98)
    for y0 in (4, 12):
        a[y0:y0 + 5, 7:9] = (40, 38, 50)
        a[y0, 7:9] = (70, 66, 72)
    a[-1] = (90, 86, 90)
    return rgba(a)


def banks(name):
    """The spore haze on low-air (r03.bank, periodic): light ~10 %, medium ~22 %, violet lit from below,
    with the concept's floating spores."""
    n = r03.noise(W, TILE_H, [160, 80, 40, 20], 1621 if name == "spore-haze" else 1623)
    cover, maxa = (0.6, 120) if name == "spore-haze" else (0.7, 90)
    cols = [(46, 34, 66), (78, 60, 104), (112, 92, 138)]
    glow = ((40, 10, 70), np.clip(1 - n, 0, 1) * 0.4)
    img = r03.bank(n, cover, 0.12, cols, max_alpha=maxa, light=4.0, glow=glow)
    img = r03.step_alpha(img, 5)
    d = ImageDraw.Draw(img)
    for i in range(140 if name == "spore-haze" else 60):
        x, y = hash01(i, 101) * W, hash01(i, 103) * (TILE_H - 4) + 2
        d.ellipse([x - 1.2, y - 1.2, x + 1.2, y + 1.2], fill=(120, 230, 180, 150))
    return img


def canopy_tile():
    """The canopy's teal veins as an additive light (the membrane of daylight_l14's cells, period 540)."""
    d1, edge, idx, _ = dl._cells(W, SCREEN)
    vein = np.clip(1 - edge / 4.0, 0, 1) ** 1.5
    core = np.clip(1 - edge / 1.4, 0, 1)
    light = vein * 0.45 + core * 0.55
    q = artkit.ordered_dither(light, 5)
    col = np.array((40, 170, 130))[None, None, :] * q[..., None] * 0.55
    return rgba(col)


TEAR_TIMES = (1.5, 3.5, 5.5, 7.5)       # s after the death: the canopy-tear frames (the atlas budget: 4)
BEAM_TIMES = (5.0, 7.5)                 # s after the death: the beams (or look b's haze)


def tear_frames():
    return [dl.canopy_layer(DAYLIGHT, W, SCREEN, t) for t in TEAR_TIMES]


def beam_frames():
    over, add = zip(*[dl.light_layers(DAYLIGHT, W, SCREEN, t) for t in BEAM_TIMES])
    return list(add) if DAYLIGHT == "a" else list(over)


LOOKS = {"canopy-tear": tear_frames, "daylight-beams" if DAYLIGHT == "a" else "daylight-haze": beam_frames}


# --------------------------------------------------------------------------- the block

PIECES = {      # id -> (layer, (w, h), extra spec, mid-size, note)
    "boulevard": ("ground", (W, 48), {}, False, "a tree-lined boulevard with tram rails: over every seam between two "
                                               "tile sets (dusk)"),
    "boulevard-day": ("ground", (W, 48), {}, False, "the same in the daylight (the strip's top, over the daylight tiles)"),
    "rhone": ("ground", (W, OVERLAYS["rhone"][1]), {}, False, OVERLAYS["rhone"][2]),
    "jet-deau": ("ground", (112, 144), {}, True, "the Jet d'Eau: its jetty and lighthouse, the plume leaning away from "
                                                 "the centre, static"),
    "cathedral": ("ground", (W, OVERLAYS["cathedral"][1]), {}, True, OVERLAYS["cathedral"][2]),
    "cathedral-tower": ("ground", TOWER_FOOT, {"tower": {"height": TOWER_H, "wall": "wall-stone", "shade": 0.55}},
                        False, "a west tower of Saint-Pierre in perspective: limestone walls, a verdigris pyramid"),
    "molard": ("ground", (W, OVERLAYS["molard"][1]), {}, True, OVERLAYS["molard"][2]),
    "root-crater": ("ground", (128, 112), {}, False, "the Ravager pack's crater: root burst a's last frames (no growths)"),
    "traffic-car": ("ground", (14, 28), {"headings": 2}, False, "a car with its headlights, up (0) or down (1) the "
                                                               "avenue: the few still moving"),
    "hammer-flight": ("low-air", (136, 120), {"headings": 2}, False, "Hammer flight: three CDF bombers in a vee, "
                                                                     "heading up (0) or down (1)"),
}
for _k in range(STRIP_PIECES):
    PIECES[f"concord-{_k + 1}"] = ("ground", (W, strip_span(_k)[1] - strip_span(_k)[0]), {}, False,
                                   "the strip: the approach, the arena round the rotunda, the daylight" if _k == 0 else None)
PIECE_GENERATORS = {
    "boulevard": lambda: [boulevard()], "boulevard-day": lambda: [boulevard(True)],
    "rhone": lambda: [overlay_piece("rhone")], "cathedral": lambda: [overlay_piece("cathedral")],
    "molard": lambda: [overlay_piece("molard")], "jet-deau": lambda: [jet_deau()],
    "root-crater": lambda: [root_crater()], "traffic-car": lambda: traffic_car(2),
    "hammer-flight": lambda: hammer_flight(2),
    "cathedral-tower": lambda: [tower_roof(TOWER_FOOT, TOWER_H)], "wall-stone": lambda: [wall_stone()],
    "haze-light": lambda: [banks("haze-light")], "spore-haze": lambda: [banks("spore-haze")],
    "canopy": lambda: [canopy_tile()],
}
for _n in ("lakefront", "old-town", "root-field", "daylight"):
    PIECE_GENERATORS[_n] = (lambda n=_n: [ground_tile(n)])


def r4(t):
    return round(float(t), 4)


def t_bottom(layer, bottom, h):
    """The placement t that puts a piece's bottom row at ``bottom`` on its layer (whole pixels)."""
    return r4(t_for_centre(layer, bottom + h / 2))


def placements():
    """[(piece, t, x, extra, comment)] in draw order (later over earlier on a layer)."""
    out = []
    lay = layout()
    for k in range(STRIP_PIECES):
        a, b = strip_span(k)
        out.append((f"concord-{k + 1}", t_bottom("ground", a, b - a), 240, None,
                    f"4-6. the strip: ground {STRIP_BOT}-{STRIP_TOP} (one render cut in {STRIP_PIECES}): the approach, the "
                    f"arena's frame (the halt shows {HALT_SCROLL:.2f}-{FRAME_TOP:.2f}, the anchor at {ANCHOR_POS:.2f}) and "
                    f"the daylight graded by its distance past the frame" if k == 0 else None))
    for name in ("rhone", "cathedral", "molard"):
        bottom, h = overlay_rect(overlay_centre(name), OVERLAYS[name][1])
        note = {"rhone": f"1. the Rhone with the Pont du Mont-Blanc under the armoured car (x {lay['car'][0]:g}, t {CAR_AT[0]:g})",
                "cathedral": "2. Saint-Pierre's square under the turret nest " + str(NEST_AT),
                "molard": "2. the Place du Molard under the hive nodes " + str(HIVES_AT)}[name]
        out.append((name, t_bottom("ground", bottom, h), 240, None, note + " (cut from the tile under it)"))
    cx = cathedral_axis()
    tpos = overlay_centre("cathedral") + 50 + 6
    for dx in CATHEDRAL_TOWERS:
        out.append(("cathedral-tower", r4(t_for_centre("ground", tpos)), cx + dx, None,
                    "2. Saint-Pierre's west towers (perspective)" if dx < 0 else None))
    out.append(("jet-deau", r4(6.0), 60, None, "1. the Jet d'Eau off the quay"))
    cxr, cpos = lay["crater"]
    out.append(("root-crater", r4(t_for_centre("ground", cpos)), cxr, None,
                "3. the crater the Ravager pack comes out of (t 72, the first burst's x)"))
    for i, s in enumerate(range(1, 3)):
        b = seam_pos(s)
        out.append(("boulevard", t_bottom("ground", int(round(b - 24)), 48), 240, None,
                    "the boulevards over the seams of sections 1-2, 2-3 and the strip's foot" if i == 0 else None))
    out.append(("boulevard", t_bottom("ground", STRIP_BOT - 24, 48), 240, None, None))
    out.append(("boulevard-day", t_bottom("ground", STRIP_TOP - 24, 48), 240, None,
                "6. the strip's top over the daylight tiles"))
    # the few cars still moving: north in lane 306, south in lane 296, at 25 px/s on their own
    for t0, count, every, note in ((3.0, 3, 6.0, "1-2. the few cars still moving on the avenue (25 px/s)"),
                                   (27.0, 4, 7.0, None)):
        for x, t1, sign in ((306, t0, 1), (296, t0 + 3, -1)):
            a, b = max(0.0, t1 - 12), t1 + 12                # no waypoint before the level starts
            out.append(("traffic-car", r4(t1), x, {"path": [[a, 0, sign * 25 * (a - t1)], [b, 0, sign * 25 * (b - t1)]],
                                                    "repeat": {"count": count, "every": every}}, note if x == 306 else None))
    # step H7b: over the lake at x 130, clear of the armoured car (x 330, on the screen t 14.5-20) it hid at x 340
    out.append(("hammer-flight", r4(16.0), 130, {"path": [[10.0, 0, -660], [22.0, 0, 660]]},
                "1. Hammer flight passing below on low-air over the lake (110 px/s on its own, Hammer Lead's line from t 13 "
                "plays as it passes; x 130 clear of the armoured car, step H7b)"))
    return out


def all_pieces():
    return dict(PIECES)


def block(pl=None):
    pl = pl or placements()
    tile_sets = {n: {"layer": l, "height": h, **({"drift": d} if d else {})} for n, (l, h, d, _) in TILE_SETS.items()}
    pieces = {}
    for name, (layer, (w, h), extra, mid, _) in all_pieces().items():
        spec = {"layer": layer, "size": [w, h], **extra}
        if mid:
            spec["mid_size"] = True
        pieces[name] = spec
    placed = []
    for piece, t, x, extra, _ in pl:
        entry = {"piece": piece, "t": t, "x": x}
        if extra:
            entry.update(extra)
        placed.append(entry)
    return {"scroll_factors": dict(FACTORS), "ramp": RAMP, "haze_colour": HAZE_COLOUR,
            "atmosphere": ATMOSPHERE, "tile_sets": tile_sets, "pieces": pieces, "placed": placed}


def write_proposal():
    pl = placements()
    b = block(pl)
    flow = k8.flow
    lines = [
        "# Level 14 backdrop PROPOSAL (tools/art/backdrop_l14.py --proposal; M5 part H batch, round 36, step H5).",
        "# For the main agent to merge into data.yaml: `backdrop` is the level's block, and the sections",
        "# take the `tiles` below.",
        "#",
        "# sections[i].tiles (by section, in order):",
    ]
    for s, tiles in zip(SECTIONS, SECTION_TILES):
        lines.append(f"#   {s['name']}: [{', '.join(tiles)}]")
    lines += [
        "#",
        "# Built round this timeline: " + ", ".join(f"{s['name']} to {s['end']:g}" for s in SECTIONS) + ";",
        f"#   the halt at ground scroll {HALT_SCROLL:.2f} (scrollAt({HALT_T:g}) + the 1 s ease, Sortie.haltScroll); the",
        f"#   anchor (the trunk's foot) at x {ANCHOR_X:g}, {ANCHOR_Y:g} px below the top edge then (ground {ANCHOR_POS:.2f});",
        f"#   the units laid out round: armoured car {CAR_AT}, turret nest {NEST_AT}, hive nodes {HIVES_AT},",
        f"#   root bursts {[(b_['t'], b_['x'], b_['depth']) for b_ in BURSTS]} (t, x, depth). Re-run --proposal (and the",
        "#   images) after the data moves a section end, a target or the anchor: the strip, the overlays and the",
        "#   boulevards are cut at exact rows.",
        "# The game's daylight looks are not in the block: assets/backdrop/level-14/canopy-tear_0..3 (straight",
        "#   alpha, 480x540, at 1.5, 3.5, 5.5, 7.5 s after the Spire's death; tile them on the high-air scroll)",
        f"#   and {'daylight-beams_0..1 (additive' if DAYLIGHT == 'a' else 'daylight-haze_0..1 (over'}, 480x540, at 5, 7.5 s), step H6.",
        "",
        "backdrop:",
        "  # Presentation only: Geneva Concord under the Vrell canopy (tools/art/backdrop_l14.py, straight from",
        "  # scene-geneva-r10-a, H8 = a; M5 part H, step H5). No deep layer: the ground tiles are opaque (the lake",
        "  # painted on them); the canopy's shadow is baked into them, its veins on high-air. Sections 4-6 are one",
        "  # strip of pieces (the arena halts: its own tile set is never drawn); boulevards hide the tile seams.",
        "  # Motion budget: Hammer flight and the few moving cars (sections 1-2); nothing moves in the arena.",
        "  scroll_factors: " + flow(b["scroll_factors"]),
        f"  ramp: {RAMP}",
        f"  haze_colour: {HAZE_COLOUR}  # unused: no deep or far layer",
        "  atmosphere:",
    ]
    notes = {"clear": "the arena and the daylight: the canopy's veins only", "light": "sections 1-2: thin spore haze",
             "medium": "sections 3-4: spore-haze banks lit violet from below"}
    for key, look in ATMOSPHERE.items():
        lines.append(f"    {key}: {flow(look)}   # {notes[key]}")
    lines.append("  tile_sets:")
    for name, spec in b["tile_sets"].items():
        lines.append(f"    {name}: {flow(spec)}   # {TILE_SETS[name][3]}")
    lines.append("  pieces:")
    for name, spec in b["pieces"].items():
        note = PIECES[name][4]
        lines.append(f"    {name}: {flow(spec)}" + (f"   # {note}" if note else ""))
    lines.append("  placed:")
    for entry, (_, _, _, _, comment) in zip(b["placed"], pl):
        if comment:
            lines.append(f"    # {comment}")
        lines.append(f"    - {flow(entry)}")
    PROPOSAL.parent.mkdir(parents=True, exist_ok=True)
    PROPOSAL.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"proposal: {PROPOSAL.relative_to(ROOT)} ({len(b['placed'])} placements)")


def level_backdrop():
    own = DATA.get("backdrop") or {}
    if own and "images" not in own:
        return own, "data.yaml"
    if not PROPOSAL.exists():
        write_proposal()
    return yaml.safe_load(PROPOSAL.read_text(encoding="utf-8"))["backdrop"], PROPOSAL.name


# --------------------------------------------------------------------------- writing

WRITE_ORDER = ["lakefront", "old-town", "root-field", "daylight", "haze-light", "spore-haze", "canopy",
               "boulevard", "boulevard-day", "rhone", "cathedral", "molard", "jet-deau", "root-crater",
               "traffic-car", "hammer-flight", "cathedral-tower", "wall-stone"]


def render_job(name):
    if name == "strip":
        return name, strip_pieces()
    if name in LOOKS:
        return name, LOOKS[name]()
    return name, PIECE_GENERATORS[name]()


def save(name, images):
    OUT.mkdir(parents=True, exist_ok=True)
    if name == "strip":
        for k, img in enumerate(images):
            artkit.save_png(img, OUT / f"concord-{k + 1}.png", SOURCE)
        return
    many = name in LOOKS or (PIECES.get(name, (None, None, {}))[2].get("headings") or
                             PIECES.get(name, (None, None, {}))[2].get("frames"))
    if many:
        for old in OUT.glob(f"{name}_*.png"):
            old.unlink()
        for i, img in enumerate(images):
            artkit.save_png(img, OUT / f"{name}_{i}.png", SOURCE)
    else:
        artkit.save_png(images[0], OUT / f"{name}.png", SOURCE)


def check_sizes(backdrop):
    bad = []
    for name, spec in backdrop["tile_sets"].items():
        im = Image.open(OUT / f"{name}.png")
        if im.size != (W, spec["height"]):
            bad.append(f"{name}: {im.size} is not {(W, spec['height'])}")
    for name, spec in backdrop["pieces"].items():
        n = spec.get("frames") or spec.get("headings")
        size = tuple(spec["size"])
        if "tower" in spec:
            k = CAMERA / (CAMERA - spec["tower"]["height"])
            size = (round(size[0] * k), round(size[1] * k))
        for f in ([OUT / f"{name}_{i}.png" for i in range(n)] if n else [OUT / f"{name}.png"]):
            if not f.exists() or Image.open(f).size != size:
                bad.append(f"{f.name}: missing or not {size}")
    return bad


def write_all(wanted):
    jobs = ["strip"] + WRITE_ORDER + list(LOOKS)
    if wanted:
        jobs = [j for j in jobs if j in wanted or (j == "strip" and any(w.startswith("concord") for w in wanted))]
    # the overlays and the tiles share the tiles' cached renders: render the tiles first in this process
    with ProcessPoolExecutor(max_workers=6) as pool:
        for name, images in pool.map(render_job, jobs):
            save(name, images)
            print(f"  {name}: {len(images)} image(s)")


# --------------------------------------------------------------------------- the game's draw (review, checks)

_cache = {}


def cached(name, i=None):
    key = (name, i)
    if key not in _cache:
        f = OUT / (f"{name}_{i}.png" if i is not None else f"{name}.png")
        _cache[key] = np.array(Image.open(f).convert("RGBA")).astype(np.float64)
    return _cache[key]


def expanded(backdrop):
    return k8.expanded(backdrop)


def piece_bottom(backdrop, placed):
    spec = backdrop["pieces"][placed["piece"]]
    f = backdrop["scroll_factors"][spec["layer"]]
    return k8.java_round(scroll_at(placed["t"]) * f + MID - spec["size"][1] / 2)


def piece_rect(backdrop, placed, t, view):
    """(left, bottom, w, h) on screen, y up, for the ground scroll ``view``."""
    spec = backdrop["pieces"][placed["piece"]]
    w, h = spec["size"]
    f = backdrop["scroll_factors"][spec["layer"]]
    path = placed.get("path")
    y = piece_bottom(backdrop, placed) - k8.java_round(view * f) + (k8.java_round(k8.along(path, t, 2)) if path else 0)
    x = k8.java_round(placed["x"] + (k8.along(path, t, 1) if path else 0) - w / 2)
    return x, y, w, h


def tower_geometry(backdrop, placed, view):
    spec = backdrop["pieces"][placed["piece"]]
    fw, fh = spec["size"]
    k = CAMERA / (CAMERA - spec["tower"]["height"])
    rw, rh = k8.java_round(fw * k), k8.java_round(fh * k)
    centre = scroll_at(placed["t"]) + MID
    fx = k8.java_round(placed["x"] - fw / 2)
    fy = int(k8.java_round(centre - fh / 2) - k8.java_round(view))
    rx = k8.java_round(k8.CX + (placed["x"] - k8.CX) * k - rw / 2)
    ry = k8.java_round(k8.CY + (centre - view - k8.CY) * k - rh / 2)
    return (fx, fy, fw, fh), (rx, ry, rw, rh), k


def on_screen(backdrop, placed, t, view):
    spec = backdrop["pieces"][placed["piece"]]
    if "tower" in spec:
        (fx, fy, fw, fh), (rx, ry, rw, rh), _ = tower_geometry(backdrop, placed, view)
        return max(fx + fw, rx + rw) > 0 and min(fx, rx) < W and max(fy + fh, ry + rh) > 0 and min(fy, ry) < SCREEN
    x, y, w, h = piece_rect(backdrop, placed, t, view)
    return x + w > 0 and x < W and y + h > 0 and y < SCREEN


def section_tiles():
    own = (DATA.get("backdrop") or {}).get("images") is None and DATA.get("backdrop")
    return [(s.get("tiles") if own else None) or SECTION_TILES[i] for i, s in enumerate(SECTIONS)]


def draw_tiles(backdrop, frame, layer, scroll, additive=None):
    rows = scroll + (SCREEN - 1 - np.arange(SCREEN))
    tiles = section_tiles()
    f = backdrop["scroll_factors"][layer]
    for s in range(len(SECTIONS)):
        names = [n for n in tiles[s] if backdrop["tile_sets"][n]["layer"] == layer]
        if not names:
            continue
        lo = -math.inf if s == 0 else scroll_at(STARTS[s]) * f + SCREEN
        hi = scroll_at(STARTS[s + 1]) * f + SCREEN if s + 1 < len(SECTIONS) else math.inf
        sel = (rows >= lo) & (rows < hi)
        if sel.any():
            img = cached(names[0])
            hgt = img.shape[0]
            part = img[[hgt - 1 - (int(r) % hgt) for r in rows[sel]]]
            sub = frame[sel]
            if additive is None:
                k8.over(sub, part)
            else:
                k8.add(sub, part, additive)
            frame[sel] = sub


def image_index(backdrop, placed, t):
    spec = backdrop["pieces"][placed["piece"]]
    if spec.get("frames"):
        return int(math.floor(t * spec["fps"])) % spec["frames"]
    if spec.get("headings"):
        n = spec["headings"]
        return k8.java_round(k8.heading_at(placed["path"], t) * n / 360) % n
    return None


def draw_pieces(backdrop, placed_all, frame, layer, t, view, additive=None):
    for placed in placed_all:
        spec = backdrop["pieces"][placed["piece"]]
        if spec["layer"] != layer or "tower" in spec or not on_screen(backdrop, placed, t, view):
            continue
        x, y, w, h = piece_rect(backdrop, placed, t, view)
        img = cached(placed["piece"], image_index(backdrop, placed, t))
        k8.blit(frame, img, x, y, additive)


def draw_towers(backdrop, placed_all, frame, view):
    towers = [p for p in placed_all if "tower" in backdrop["pieces"][p["piece"]]]
    for placed in sorted(towers, key=lambda p: backdrop["pieces"][p["piece"]]["tower"]["height"]):
        if not on_screen(backdrop, placed, 0, view):
            continue
        spec = backdrop["pieces"][placed["piece"]]["tower"]
        (fx, fy, fw, fh), (rx, ry, rw, rh), k = tower_geometry(backdrop, placed, view)
        tex = cached(spec["wall"])
        shade = spec.get("shade", 0.5)
        fx1, fy1, rx1, ry1 = fx + fw, fy + fh, rx + rw, ry + rh
        h = spec["height"]
        if ry > fy:
            k8.wall_quad(frame, tex, shade, ((fx, fy), (fx1, fy)), ((rx, ry), (rx1, ry)), h)
        if ry1 < fy1:
            k8.wall_quad(frame, tex, 1.0, ((fx1, fy1), (fx, fy1)), ((rx1, ry1), (rx, ry1)), h)
        if rx < fx:
            k8.wall_quad(frame, tex, 1.0, ((fx, fy1), (fx, fy)), ((rx, ry1), (rx, ry)), h)
        if rx1 > fx1:
            k8.wall_quad(frame, tex, shade, ((fx1, fy), (fx1, fy1)), ((rx1, ry), (rx1, ry1)), h)
        k8.blit(frame, cached(placed["piece"]), rx, ry)


def stretches():
    out = []
    for i, s in enumerate(SECTIONS):
        out.append((STARTS[i], s["end"], s["atmosphere"]))
    return out


def looks_at(backdrop, t):
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


def draw_banks(backdrop, frame, look_a, look_b, weight, view):
    f = backdrop["scroll_factors"]["low-air"]
    rows = k8.java_round(view * f) + (SCREEN - 1 - np.arange(SCREEN))
    for look, share in ((look_a, 1 - weight), (look_b, weight)) if look_a is not look_b else ((look_b, 1.0),):
        name = look.get("banks")
        if name and share > 0:
            img = cached(name)
            hgt = img.shape[0]
            k8.over(frame, img[[hgt - 1 - (int(r) % hgt) for r in rows]], share)


# the units the review shows as stand-ins (their final sprites where they exist)
def _sprite(name):
    frames = artkit.load_frames(name)
    return np.array(frames[0]).astype(np.float64) if frames else None


@lru_cache(maxsize=None)
def stand_ins():
    import json
    out = {"car": _sprite("armoured-car"), "turret": _sprite("spine-turret"), "hive": _sprite("hive-node"),
           "mortar": _sprite("polyp-mortar"), "decal": None}
    piv = ROOT / "assets" / "pivots" / "siege-spire.json"
    out["spire"] = json.loads(piv.read_text()) if piv.exists() else None
    for k in ("siege-spire", "siege-spire-root", "siege-spire-pod", "siege-spire-mortar", "siege-spire-stump",
              "siege-spire-free"):
        out[k] = [np.array(f).astype(np.float64) for f in artkit.load_frames(k)]
    return out


def blit_centre(frame, img, x, y_up):
    if img is None:
        return
    h, w = img.shape[:2]
    k8.blit(frame, img, k8.java_round(x - w / 2), k8.java_round(y_up - h / 2))


def draw_units(frame, t, view, phase="rooted"):
    """The Siege Spire at its anchor (trunk, roots and pods from its pivots), the armoured car, the turret
    nest, the hive nodes and the root bursts' growths at their places (review only)."""
    s = stand_ins()
    lay = layout()
    blit_centre(frame, s["car"], lay["car"][0], lay["car"][1] - view)
    for x, p in lay["nest"]:
        blit_centre(frame, s["turret"], x, p - view)
    for x, p in lay["hives"]:
        blit_centre(frame, s["hive"], x, p - view)
    for b in BURSTS:
        if t >= b["t"] + 1.0:
            p = burst_pos(b)
            blit_centre(frame, cached("root-crater")[8:-8, 8:-8], b["x"], p - view)
            blit_centre(frame, s["turret"], b["x"] - 26, p - view - 5)
            blit_centre(frame, s["mortar"], b["x"] + 26, p - view + 5)
    piv = s["spire"]
    if not piv or not s["siege-spire"]:
        return
    ay = ANCHOR_POS - view
    for i, r in enumerate(piv["roots"]):
        dead = phase != "rooted"
        blit_centre(frame, s["siege-spire-root"][i * 2 + dead], ANCHOR_X + r["offset"][0], ay - r["offset"][1])
    if phase == "free":
        blit_centre(frame, s["siege-spire-stump"][0], ANCHOR_X, ay)
        blit_centre(frame, s["siege-spire-free"][0], 240, SCREEN - 120)
    elif phase == "rooted":
        tr = piv["trunk"]["offset"]
        blit_centre(frame, s["siege-spire"][0], ANCHOR_X + tr[0], ay - tr[1])
        for r in piv["roots"]:
            img = s["siege-spire-pod"][0] if r["kind"] == "turret" else s["siege-spire-mortar"][0]
            blit_centre(frame, img, ANCHOR_X + r["pod"][0], ay - r["pod"][1])
    else:
        blit_centre(frame, s["siege-spire-stump"][0], ANCHOR_X, ay)


def composite(backdrop, t, view=None, death=None, units=True, phase="rooted"):
    """The play field at level time t as the game draws it: ground (tiles, flat pieces, towers), the units
    (stand-ins), low-air (banks, pieces), high-air additive at 40 %; after ``death`` (the real second the
    Spire died) the daylight looks the game draws (step H6) over it."""
    placed_all = expanded(backdrop)
    view = sim_scroll(t) if view is None else view
    frame = np.zeros((SCREEN, W, 4))
    frame[..., 3] = 255
    look_a, look_b, weight = looks_at(backdrop, t)
    draw_tiles(backdrop, frame, "ground", k8.java_round(view))
    draw_pieces(backdrop, placed_all, frame, "ground", t, view)
    draw_towers(backdrop, placed_all, frame, view)
    if units:
        draw_units(frame, t, view, phase)
    fl = backdrop["scroll_factors"]["low-air"]
    draw_tiles(backdrop, frame, "low-air", k8.java_round(view * fl))
    draw_banks(backdrop, frame, look_a, look_b, weight, view)
    draw_pieces(backdrop, placed_all, frame, "low-air", t, view)
    fh = backdrop["scroll_factors"]["high-air"]
    hs = k8.java_round(view * fh)
    if death is None:
        draw_tiles(backdrop, frame, "high-air", hs, additive=0.4)
    else:
        tau = t - death
        rows = hs + (SCREEN - 1 - np.arange(SCREEN))
        fade = 1 - dl.ramp(tau, 2.0, 7.0)              # the veins die with the canopy (H6)
        if fade > 0:
            img = cached("canopy")
            k8.add(frame, img[[img.shape[0] - 1 - (int(r) % img.shape[0]) for r in rows]], 0.4 * fade)
        k = next((i for i, s in enumerate(TEAR_TIMES) if tau < s + 1.0), None)
        if k is not None and tau >= 0.5:
            img = cached("canopy-tear", k)
            k8.over(frame, img[[img.shape[0] - 1 - (int(r) % img.shape[0]) for r in rows]])
        name = "daylight-beams" if DAYLIGHT == "a" else "daylight-haze"
        bk = 0 if tau < 6.25 else 1
        if tau >= 2.5:
            img = cached(name, bk)
            strength = dl.ramp(tau, 2.5, 5.0)
            if DAYLIGHT == "a":
                k8.add(frame, img, strength)
            else:
                k8.over(frame, img, strength)
    return Image.fromarray(np.clip(frame[..., :3], 0, 255).astype(np.uint8), "RGB")


def after_kill_view(tau):
    """The ground scroll ``tau`` real seconds after an early kill (the clock jumps to the arena's end,
    the scroll ramps up over 1 s: Level 07's arenaJump)."""
    v = speed(ARENA + 1)
    u = min(tau, 1.0)
    return HALT_SCROLL + v * (tau - (u - u ** 3 + u ** 4 / 2) if tau < 1 else tau - 0.5)


# --------------------------------------------------------------------------- checks

def _lum(a):
    return a[..., 0] * 0.299 + a[..., 1] * 0.587 + a[..., 2] * 0.114


def seams_wrap_score(a):
    """BackdropSeamsTest.wrapScore: the change of the three row pairs round the wrap against the median
    change of the rows within 40 of it (luminance, the 90 % smallest differences, opaque pixels)."""
    h = a.shape[0]
    lum = _lum(a)
    op = a[..., 3] > 0

    def change(i, j):
        m = op[i] & op[j]
        if m.sum() < a.shape[1] / 4:
            return math.nan
        d = np.sort(np.abs(lum[i][m] - lum[j][m]))
        kept = int(len(d) * 0.9)
        return d[:kept].mean() if kept else 0.0
    near = max(change(h - 2, h - 1), change(h - 1, 0), change(0, 1))
    ref = sorted(c for k in range(h - 40, h + 40) if k < h - 4 or k > h + 2
                 for c in [change(k % h, (k + 1) % h)] if not math.isnan(c))
    return near / max(ref[len(ref) // 2], 0.5)


def check_all(backdrop):
    problems = check_sizes(backdrop)
    placed_all = expanded(backdrop)
    # the tile sets repeat without a line
    for name in backdrop["tile_sets"]:
        score = seams_wrap_score(cached(name)) if backdrop["tile_sets"][name]["layer"] == "ground" \
            else k8.wrap_score(cached(name))
        print(f"  wrap {name}: {score:.2f}")
        if score > 1.5:
            problems.append(f"{name}: a line where it repeats ({score:.2f})")
    # flat pieces show no edge (the strip's pieces abut each other and lie under boulevards at its ends)
    for placed in placed_all:
        spec = backdrop["pieces"][placed["piece"]]
        if "tower" in spec:
            continue
        n = spec.get("frames") or spec.get("headings")
        for i in (range(n) if n else [None]):
            a = cached(placed["piece"], i)[..., 3] > 0
            if a[0].any() or a[-1].any():
                problems.append(f"{placed['piece']}: its top or bottom row is opaque")
            x = placed["x"] - spec["size"][0] / 2
            if (0 < x < W and a[:, 0].any()) or (0 < x + spec["size"][0] < W and a[:, -1].any()):
                problems.append(f"{placed['piece']}: a side column is opaque on screen")
    # the strip covers the arena's frame and the tile seams it hides; boulevards cover every other seam
    if not (STRIP_BOT <= HALT_SCROLL and STRIP_TOP >= FRAME_TOP):
        problems.append(f"the strip {STRIP_BOT}-{STRIP_TOP} does not cover the halt's frame {HALT_SCROLL:.2f}-{FRAME_TOP:.2f}")
    boulevards = [piece_bottom(backdrop, p) for p in placed_all if p["piece"].startswith("boulevard")]
    for s in range(1, len(SECTIONS)):
        seam = seam_pos(s)
        under_strip = STRIP_BOT + 8 <= seam <= STRIP_TOP - 8
        tiles = section_tiles()
        same = [n for n in tiles[s] if backdrop["tile_sets"][n]["layer"] == "ground"] == \
            [n for n in tiles[s - 1] if backdrop["tile_sets"][n]["layer"] == "ground"]
        if not under_strip and not same and not any(b + 10 <= seam <= b + 38 for b in boulevards):
            problems.append(f"the ground seam of section {s + 1} at {seam:.1f} lies under no boulevard")
    if not any(b + 10 <= STRIP_TOP <= b + 38 for b in boulevards) or not any(b + 10 <= STRIP_BOT <= b + 38 for b in boulevards):
        problems.append("the strip's ends lie under no boulevard")
    # an overlay lies inside its tile's stretch
    for name in OVERLAYS:
        p = next(p for p in placed_all if p["piece"] == name)
        b = piece_bottom(backdrop, p)
        h = backdrop["pieces"][name]["size"][1]
        s = SECTION_GROUND.index(OVERLAYS[name][0])
        lo, hi = seam_pos(s), seam_pos(s + 1)
        if not (lo <= b and b + h <= hi):
            problems.append(f"{name} ({b}-{b + h}) leaves its tile's stretch {lo:.0f}-{hi:.0f}")
        a = np.array(Image.open(OUT / f"{name}.png"))[..., 3] > 0
        if a[1:4].any() or a[-4:-1].any():
            problems.append(f"{name}: its cut reaches its top or bottom: make it taller")
    # density and motion, every second step of the level to the outro's end (the arena's halt held)
    seen = [False] * len(placed_all)
    for step in range(0, int(OUTRO_END * 60) + 1, 2):
        t = step / 60
        view = sim_scroll(t)
        mid, moving = [], set()
        for i, placed in enumerate(placed_all):
            if on_screen(backdrop, placed, t, view):
                seen[i] = True
                spec = backdrop["pieces"][placed["piece"]]
                if spec.get("mid_size"):
                    mid.append(placed["piece"])
                if spec.get("frames") or placed.get("path"):
                    moving.add(placed["piece"])
        if len(mid) > 3:
            problems.append(f"t={t:.2f}: {len(mid)} mid-size pieces")
        if len(moving) > 2:
            problems.append(f"t={t:.2f}: {len(moving)} animated elements {sorted(moving)}")
        if HALT_T + 1 <= t <= ARENA_END and moving:
            problems.append(f"t={t:.2f}: {sorted(moving)} move in the arena")
    problems += [f"{p['piece']} at t {p['t']} is never on screen" for p, s in zip(placed_all, seen) if not s]
    # no tower leans over a ground target
    lay = layout()
    spots = [(lay["car"][0], lay["car"][1])] + list(lay["nest"]) + list(lay["hives"])
    for placed in placed_all:
        if "tower" not in backdrop["pieces"][placed["piece"]]:
            continue
        for t in np.arange(placed["t"] - 4, placed["t"] + 4, 0.05):
            view = sim_scroll(t)
            (fx, fy, fw, fh), (rx, ry, rw, rh), _ = tower_geometry(backdrop, placed, view)
            for x, p in spots:
                y = p - view
                if (min(fx, rx) - 14 < x < max(fx + fw, rx + rw) + 14) and (min(fy, ry) - 14 < y < max(fy + fh, ry + rh) + 14):
                    problems.append(f"{placed['piece']} at x {placed['x']:g} leans over a target at ({x:g}, t {t:.2f})")
                    break
    return sorted(set(problems))[:30]


def run_checks():
    backdrop, src = level_backdrop()
    print(f"checks on the block from {src}:")
    problems = check_all(backdrop)
    for p in problems:
        print("  PROBLEM:", p)
    print(f"  {len(problems)} problem(s)")
    return problems


def atlas_area(backdrop):
    px = 0
    for name, spec in backdrop["tile_sets"].items():
        px += W * spec["height"]
    for f in OUT.glob("*.png"):
        if f.stem.split("_")[0] in ("canopy-tear", "daylight-beams", "daylight-haze") or f.stem in backdrop["pieces"] or \
                f.stem.rsplit("_", 1)[0] in backdrop["pieces"] or f.stem == "wall-stone":
            if f.stem not in backdrop["tile_sets"]:
                im = Image.open(f)
                px += im.width * im.height
    return px


# --------------------------------------------------------------------------- review

TIMES = [(5, "1 THE LAKEFRONT, THE JET D'EAU"), (16, "1 HAMMER FLIGHT, THE RHONE"), (18, "1 THE CAR ON THE BRIDGE"),
         (30, "2 THE OLD TOWN, TRAFFIC"), (39.5, "2 SAINT-PIERRE, THE NEST"), (55.5, "2 THE MOLARD'S HIVE NODES"),
         (66, "3 THE ROOT FIELD, A BURST"), (72.5, "3 THE RAVAGERS' CRATER"), (90, "3 ROOTS AND SPORE HAZE"),
         (101, "4 THE CONCORD APPROACH"), (110, "5 THE ARENA (THE HALT)"), (200, "5 PHASE 3: THE STUMP")]
DAY_TAUS = [(1.0, "6 THE DEATH + 1 S: WITHERING"), (3.5, "6 + 3.5 S: TEARING, THE LIGHT AHEAD"),
            (6.5, "6 + 6.5 S: BEAMS"), (11.0, "6 + 11 S: DAYLIGHT")]


def review():
    backdrop, src = level_backdrop()
    artkit.REVIEW_ROUND = "r36"
    tiles = [Image.open(OUT / f"{n}.png") for n in ("lakefront", "old-town", "root-field", "daylight")]
    half = [im.resize((im.width // 2, im.height // 2), Image.NEAREST) for im in tiles]
    banks_ = [Image.open(OUT / f"{n}.png").resize((240, 480), Image.NEAREST) for n in ("haze-light", "spore-haze")]
    canopy = Image.open(OUT / "canopy.png")
    pieces = [Image.open(OUT / f"{n}.png") for n in ("rhone", "cathedral", "molard")]
    small = [Image.open(OUT / f"{n}.png") for n in ("jet-deau", "root-crater", "boulevard", "boulevard-day",
                                                     "cathedral-tower", "wall-stone")]
    small += [Image.open(OUT / f"traffic-car_{i}.png") for i in range(2)]
    small += [Image.open(OUT / f"hammer-flight_{i}.png") for i in range(2)]
    strip = [Image.open(OUT / f"concord-{k + 1}.png") for k in range(STRIP_PIECES)]
    strip_img = Image.new("RGBA", (W, STRIP_H))
    for k, im in enumerate(strip):
        strip_img.alpha_composite(im, (0, STRIP_TOP - strip_span(k)[1]))
    strip_img = strip_img.resize((W // 3, STRIP_H // 3), Image.BOX)
    looks = [Image.open(OUT / f"canopy-tear_{i}.png").resize((240, 270), Image.NEAREST) for i in range(len(TEAR_TIMES))]
    comps = []
    for t, label in TIMES:
        phase = "free" if t >= 200 else "rooted"
        img = composite(backdrop, t, phase=phase)
        img = img.convert("RGBA")
        raster.draw_text(img, 4, SCREEN - 12, f"T={t:g} {label}", raster.LABEL)
        comps.append(img)
    death = ARENA_END
    for tau, label in DAY_TAUS:
        img = composite(backdrop, death + tau, view=after_kill_view(tau), death=death, phase="dead")
        img = img.convert("RGBA")
        raster.draw_text(img, 4, SCREEN - 12, f"T={label}", raster.LABEL)
        comps.append(img)
    px = atlas_area(backdrop)
    rows = [
        ("GROUND TILE SETS 480X960 (HALF SIZE): LAKEFRONT, OLD TOWN, ROOT FIELD, DAYLIGHT", half, 1, False),
        ("LOW-AIR BANKS (HALF): HAZE-LIGHT, SPORE-HAZE; HIGH-AIR CANOPY VEINS (ADDITIVE, 1X)", banks_ + [canopy], 1, False),
        (f"THE STRIP CONCORD-1..{STRIP_PIECES} (1/3 SIZE, {STRIP_H} PX): APPROACH, ARENA, DAYLIGHT", [strip_img], 1, False),
        ("OVERLAYS CUT FROM THEIR TILES: RHONE, CATHEDRAL, MOLARD", pieces, 1, False),
        ("PIECES: JET D'EAU, CRATER, BOULEVARDS, TOWER ROOF, WALL, CARS, HAMMER FLIGHT", small, 2, False),
        ("THE GAME'S DAYLIGHT LOOKS: CANOPY-TEAR 0-3 (HALF SIZE)", looks, 1, False),
        ("COMPOSITES AS THE GAME DRAWS THEM (STAND-INS: THE SPIRE, THE TARGETS, THE BURSTS' GROWTHS)", comps[0:3], 1, False),
        ("", comps[3:6], 1, False), ("", comps[6:9], 1, False), ("", comps[9:12], 1, False),
        (f"AFTER THE SPIRE'S DEATH (DAYLIGHT {DAYLIGHT.upper()}; THE TEAR AND THE LIGHT ARE THE GAME'S, H6)", comps[12:15], 1, False),
        ("", comps[15:], 1, False),
    ]
    sheet = artkit.review_sheet(f"LEVEL 14 BACKDROP - GENEVA CONCORD ({px / 1e6:.2f} M PX, ABOUT {px / 2048 ** 2:.1f} PAGES)",
                                rows, width=1640, batch=BATCH)
    REVIEW.parent.mkdir(parents=True, exist_ok=True)
    sheet.convert("RGB").save(REVIEW, optimize=True)
    # the GIF: the approach into the halt, the halt held, then the daylight after a kill
    frames = []
    for t in np.arange(103.0, 109.0, 0.1):
        frames.append(composite(backdrop, float(t)).resize((240, 270), Image.BOX))
    for _ in range(15):
        frames.append(frames[-1])
    for tau in np.arange(0.0, 12.0, 0.1):
        frames.append(composite(backdrop, death + float(tau), view=after_kill_view(float(tau)), death=death,
                                phase="dead").resize((240, 270), Image.BOX))
    from render.enemy_rigs import write_gif
    write_gif(frames, REVIEW.with_suffix(".gif"), fps=10, colors=128)
    print(f"review: {REVIEW.relative_to(ROOT)} and .gif ({px / 1e6:.2f} M px of backdrop images)")


def strip_review(times, path):
    backdrop, _ = level_backdrop()
    imgs = [composite(backdrop, t) for t in times]
    out = Image.new("RGB", (W * len(imgs) + 10 * (len(imgs) - 1), SCREEN))
    for i, im in enumerate(imgs):
        out.paste(im, (i * (W + 10), 0))
    out.save(path)
    print(f"strip: {path}")


def main(argv):
    if "--proposal" in argv:
        write_proposal()
        return
    if "--check" in argv:
        sys.exit(1 if run_checks() else 0)
    if "--review" in argv:
        review()
        return
    if "--strip" in argv:
        i = argv.index("--strip")
        strip_review([float(v) for v in argv[i + 1].split(",")], argv[i + 2])
        return
    wanted = [a for a in argv if not a.startswith("--")]
    if not wanted:
        write_proposal()
    write_all(wanted)
    if not wanted:
        run_checks()
        review()


if __name__ == "__main__":
    main(sys.argv[1:])
