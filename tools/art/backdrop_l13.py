#!/usr/bin/env python3
"""Production art: the Level 13 backdrop, the pack ice round Nansen Relay
(design/campaign/act-2-homefront/level-13-polar-relay; M5 part G batch, concept round 35, step G5).

Straight to production (user decision G9 = a, pending the user's review) from the chosen arctic scene,
scene-arctic-r09-a (tools/concept/scenes_r09.py: its ice ramp ICE9, the floes' noise, hillshade, rim,
the teal submerged margins, the long cool shadows, the waterline foam and the dark water's colour stops,
imported or taken unchanged). The whiteout is tools/art/whiteout_l13.py's look **a** (rolling banks;
round 35's audition: re-run with ``WHITEOUT_LOOK = "b"`` if the user picks b).

**The ground is one unique strip** (no repeating ice tile): the approach (t 0-40) and the halt's fixed
frame are a single render, 480 x 6,144 px from ground position -144 to the halt's top edge (6,000),
cut into four `ground` pieces (`pack-ice-1` .. `-4`) placed so that they abut exactly. That is the only
way to give the halt its own frame: during a timed halt every later section's tile set has no length
(they all begin at the same seam, the top edge at the halt), so the halt shows section 1's ground, and
a repeating tile would also repeat the halt's leads through the approach (they would cut the hut's floe
and the relay's floe would cross the t=20 introduction's lead). The strip's water is translucent (alpha
3, the shadow stencil) over the `deep` water, so the leads show the water drifting underneath.

**The leads are cut along the lanes** (G4 = a): every Skimmer lane of the level's data is a Catmull-Rom
curve as the simulation flies it (sim FlightPath: end points repeated), mapped to the ground by the
scroll at its wave's time (the halt's at 5,460 px, so the halt's leads are fixed; the t=20 introduction's
lane at that time's ground places, so it scrolls with the ice). A lead is 2 x 58 px wide (the 40 px
weave, the hull's 15 px half width and a rim), its rough edge biting in to 57 px at most, and brash in
its outer bays (beyond the hulls' reach; --check tests each hull's full width on the weave); where a
Scuttler's path crosses a lead,
the lead is choked with brash rubble (the walker steps over it). Two meandering approach leads join the
halt's west and east leads, the introduction's lead and the start. Forced floes: the relay's (round the
data's `defend.at`, the relay at 240, 250 from the top in the halt) and the research hut's (t=12,
x=380). The halt's top rows are open water (the open sea northward), and so are `open-floes`' rows at
the seam, where the exit's tile set begins (the ground at 6,000 px).

**Layers** (factors as Levels 11 and 12): `deep` 0.85: `arctic-water`, the dark arctic swell (round
09's stops, calm), drifting 3 px/s to the left on the real clock (the open water in the leads moves
gently in the halt); `ground` 1.0: the strip in sections 1-4, `open-floes` (looser floes, open sea) in
the exit; `low-air` 1.5: the atmosphere's banks: `fog-wisps` (light, ~10 %), `fog-banks` (medium:
thicker at the edges, ~20 %), `whiteout` (heavy: whiteout_l13 look a's banks at the peak, from both
side edges, the centre clear; 480 x 540, its noise's period); no high-air tiles (the snow is the game's,
on the real clock). **Loose ice**: one piece `loose-ice` (8 headings: each heading frame a different
floe or brash cluster, frame 2, heading east, the mortar floe) on straight paths at 6 px/s in the halt's
open water outside the lanes, and the Polyp Mortars' floe drifting in from the left at the data's
10 px/s (t=66), the mortars standing on it. **Motion budget** (BackdropCheck: at most two strongly
animated elements): the drifting water and the loose ice; the fog and the whiteout are static in the
data (they cross-fade over the 4 s ramp); a real-clock roll of the whiteout banks, the snow and easy's
6 s peak are the game's (step G6).

Outputs (assets/backdrop/level-13/):
  pack-ice-1 .. pack-ice-4   ground pieces 480x1536: the approach and the halt's frame (one render)
  open-floes                 ground tile set 480x960: the exit's looser floes and open sea
  arctic-water               deep tile set 480x960, drifting: the dark water under the ice
  fog-wisps, fog-banks       low-air tile sets 480x960: light and medium fog
  whiteout                   low-air tile set 480x540: the heavy peak's banks (whiteout_l13 look a)
  loose-ice                  ground piece 96x112, 8 headings: drifting floes and brash, the mortar floe
  design/campaign/.../level-13-polar-relay/concept/backdrop-final-r35-a.png/.gif   review: the
                       images, composites as the game draws them (stand-ins for the units), the GIF the
                       ease into the halt and the whiteout's ramp
  design/campaign/.../level-13-polar-relay/concept/backdrop-proposal.yaml   the block (--proposal)

Reads the level's data.yaml (its sections, scroll speed, waves' lanes and paths, the hut, the mortar
floe's side entry, the defend's place and the backdrop block once it has no `images` key). Run:
python3 tools/art/backdrop_l13.py [--proposal | --review | --check | --strip t,.. out.png] [id ...]
(all: about 2 min)
"""
import math
import sys
from concurrent.futures import ProcessPoolExecutor

import numpy as np
import yaml
from PIL import Image, ImageDraw, ImageFilter

import artkit
from artkit import DESIGN, ROOT, TAU, raster

import backdrop_l11 as b11  # noqa: E402  (tools/art: blending, wrap score, flow)
import whiteout_l13  # noqa: E402  (tools/art: the whiteout's banks, round 35's look a)
import parallax_r02 as r02  # noqa: E402  (concept script, unchanged: periodic noise, posterize)
import parallax_r03 as r03  # noqa: E402  (concept script, unchanged: banks, stretched noise, stepped alpha)
import scenes_r06 as s6  # noqa: E402  (concept script, unchanged: periodic masks, hillshade)
import scenes_r09 as s9  # noqa: E402  (concept script, unchanged: the chosen arctic scene's ice and water)

SCRIPT = "backdrop_l13.py"
BATCH = "M5 part G batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
LEVEL_DIR = DESIGN / "campaign" / "act-2-homefront" / "level-13-polar-relay"
CONCEPT = LEVEL_DIR / "concept"
PROPOSAL = CONCEPT / "backdrop-proposal.yaml"
OUT = ROOT / "assets" / "backdrop" / "level-13"
REVIEW = CONCEPT / "backdrop-final-r35-a.png"
W, SCREEN = 480, 540
MID = SCREEN / 2
TILE_H = 960
WHITEOUT_LOOK = "a"                     # round 35's audition: look a goes in the game until the user chooses

FACTORS = {"deep": 0.85, "far": 0.9, "ground": 1.0, "low-air": 1.5, "high-air": 2.2}
HAZE_COLOUR = "a8b8c4"                  # the arctic's pale grey-blue (over the water only)
ATMOSPHERE = {
    "clear": {"haze": 0.0},                                  # the exit: grid online, no fog
    "light": {"banks": "fog-wisps", "haze": 0.04},           # thin fog wisps over the pack
    "medium": {"banks": "fog-banks", "haze": 0.08},          # the fog thickening at the edges from t=140
    "heavy": {"banks": "whiteout", "haze": 0.14},            # the whiteout's peak: banks from both side edges
}
RAMP = 4                                # README: the whiteout's 4 s ramps
WATER_DRIFT = -3.0                      # px/s: the water under the ice drifts left (real clock)
LOOSE_SPEED = 6.0                       # px/s: the loose floes and brash in the halt (README)

# --------------------------------------------------------------------------- the level's data

DATA_PATH = LEVEL_DIR / "data.yaml"
DATA = yaml.safe_load(DATA_PATH.read_text(encoding="utf-8"))
SECTIONS = DATA["sections"]
SPEED = float(DATA.get("scroll_speed", 140))
END = float(SECTIONS[-1]["end"])
OUTRO_END = END + 15                    # LevelData.OUTRO_SECONDS


def sec_start(i):
    return 0.0 if i == 0 else float(SECTIONS[i - 1]["end"])


def speed(i):
    s = SECTIONS[i]
    return float(s["speed"]) if "speed" in s else SPEED


def timed_halt(i):
    return speed(i) == 0 and not SECTIONS[i].get("arena")


def ramp_seconds(i):
    return float(SECTIONS[i].get("ramp", 2.0))   # LevelScript.Section.TIMED_HALT_RAMP_SECONDS


def _ease_in(u):
    return u * u * u - u * u * u * u / 2


def _ease_out(u):
    return u - u * u * u + u * u * u * u / 2


def _clamped(t, start, ramp):
    return min(max((t - start) / ramp, 0.0), 1.0) if ramp > 0 else (1.0 if t >= start else 0.0)


def _travelled(i, a, b):
    """LevelData.travelled: the px section i scrolls from a to b s, with a timed halt's eases."""
    sp = speed(i)
    st, en = sec_start(i), float(SECTIONS[i]["end"])
    px = (b - a) * sp
    if sp == 0:
        return px
    if i + 1 < len(SECTIONS) and timed_halt(i + 1):
        r = min(ramp_seconds(i + 1), en - st)
        px -= sp * r * (_ease_in(_clamped(b, en - r, r)) - _ease_in(_clamped(a, en - r, r)))
    if i > 0 and timed_halt(i - 1):
        r = min(ramp_seconds(i - 1), en - st)
        px -= sp * r * (_ease_out(_clamped(b, st, r)) - _ease_out(_clamped(a, st, r)))
    return px


def scroll_at(t):
    """LevelData.scrollAt: the ground's scroll at t (the timed halt's smoothstep eases integrated)."""
    scroll = 0.0
    for i, s in enumerate(SECTIONS):
        st = sec_start(i)
        if t < float(s["end"]) or i == len(SECTIONS) - 1:
            return scroll + _travelled(i, st, t)
        scroll += _travelled(i, st, float(s["end"]))
    raise AssertionError


HALT = next(i for i in range(len(SECTIONS)) if timed_halt(i))
HALT_FROM = sec_start(HALT)
HALT_TO = next(sec_start(i) for i in range(HALT, len(SECTIONS)) if not timed_halt(i))
HALT_SCROLL = scroll_at(HALT_FROM)                     # 5,460 with the data's 2 s ease
EXIT = len(SECTIONS) - 1                               # the section after the halt (its tile set: open-floes)
TOP_G = b11.java_round(HALT_SCROLL) + SCREEN           # 6,000: the halt's top edge = every later section's seam
STRIP_H = 6144                                         # the strip's rows: ground TOP_G - 6,144 .. TOP_G
PIECE_H = 1536
PIECES_N = STRIP_H // PIECE_H


def seam(layer, index):
    return b11.java_round(scroll_at(sec_start(index)) * FACTORS[layer] + SCREEN)


def y_img(t, r):
    """The strip's row (0 = its top, the halt's top edge) of a point r px below the top edge at t."""
    return HALT_SCROLL - scroll_at(t) + r


def catmull_rom(points, n=10):
    """sim FlightPath: a uniform Catmull-Rom curve through the points, end points repeated."""
    p = np.array(points, float)
    p = np.vstack([p[0], p, p[-1]])
    out = []
    for i in range(1, len(p) - 2):
        p0, p1, p2, p3 = p[i - 1], p[i], p[i + 1], p[i + 2]
        for t in np.linspace(0, 1, n, endpoint=False):
            t2, t3 = t * t, t * t * t
            out.append(0.5 * (2 * p1 + (p2 - p0) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2
                              + (3 * p1 - p0 - 3 * p2 + p3) * t3))
    out.append(p[-2])
    return np.array(out)


CONVOY_INTERVAL = 1.5                   # sim Formations.CONVOY_INTERVAL_SECONDS
PACK_INTERVAL = 0.25                    # sim Formations.PACK_INTERVAL_SECONDS


def lane_units(w):
    """sim Formations.lanes: every unit of a lane wave as (entry time, lane index, mirrored); its
    count the largest of the difficulties' (each difficulty's units are a prefix of these)."""
    count = max([int(w.get("count", 1))] + [int(w[d]["count"]) for d in ("easy", "hard")
                                            if isinstance(w.get(d), dict) and "count" in w[d]])
    n, form = len(w["lanes"]), w["formation"]
    out = []
    for i in range(count):
        delay = {"cross": 0.0, "pincer": (i // 2) * CONVOY_INTERVAL, "pack": i * PACK_INTERVAL}.get(
            form, i * CONVOY_INTERVAL)
        out.append((float(w["t"]) + delay, i % n, form == "pincer" and n == 1 and i % 2 == 1))
    return out


def lane_runs():
    """Every Skimmer unit's run as (wave t, entry t, lane curve in screen px at its entry): a lane is
    in ground space from its unit's entry (sim Formations.lanes), so a convoy's units, 1.5 s apart,
    each lay theirs on other ground while the ground scrolls (all on the same ground in the halt)."""
    out = []
    for w in DATA["waves"]:
        for entry, k, mirror in lane_units(w) if w.get("lanes") else []:
            c = catmull_rom(w["lanes"][k])
            if mirror:
                c[:, 0] = W - c[:, 0]
            out.append((float(w["t"]), entry, c))
    return out


def wave_lanes():
    """The leads to cut: every distinct lane on the ground as (entry t, curve), duplicates once."""
    seen, out = set(), []
    for _, entry, c in lane_runs():
        key = (round(scroll_at(entry)), c.tobytes())
        if key not in seen:
            seen.add(key)
            out.append((entry, c))
    return out


def walker_paths():
    """The Scuttlers' paths in the halt (a pincer's mirrored for the right), screen px."""
    out = []
    for w in DATA["waves"]:
        if w.get("enemy") != "scuttler" or not w.get("paths"):
            continue
        for path in w["paths"]:
            c = catmull_rom(path)
            out.append((float(w["t"]), c))
            if w["formation"] == "pincer":
                m = c.copy()
                m[:, 0] = W - m[:, 0]
                out.append((float(w["t"]), m))
    return out


def ground_target(sprite=None, enemy=None):
    for gt in DATA.get("ground_targets") or []:
        if (sprite and gt.get("sprite") == sprite) or (enemy and gt.get("enemy") == enemy):
            return gt
    return None


HUT = ground_target(sprite="research-hut")
HUT_T, HUT_X = (float(v) for v in HUT["at"][0])
HUT_SIZE = HUT.get("size", [48, 40])
HUT_Y = y_img(HUT_T, -HUT_SIZE[1] / 2)                  # GroundObject: its centre half its height above the top edge
FLOE = ground_target(enemy="polyp-mortar")
FLOE_T = float(FLOE["at"][0][0])
FLOE_YS = [SCREEN - float(a[1]) for a in FLOE["at"]]    # the mortars' screen y (the data's y up)
FLOE_DRIFT = float(FLOE["side"]["drift"])
FLOE_LEFT = FLOE["side"]["from"] == "left"
MORTAR_HALF = 22                                       # the Polyp Mortar's box width / 2 (its data: 44)
DEFEND = DATA["objectives"]["defend"]
RELAY_X, RELAY_R = float(DEFEND["at"][0]), SCREEN - float(DEFEND["at"][1])   # 240, 250 below the top

# The approach's own leads in strip rows (x, row): the west one leaves the halt's west lead and runs
# into the introduction's lead at the left edge; the east one leaves the halt's east lead off the right
# edge; two more cross the first 24 s, clear of the hut's floe (x 380, row ~3,760).
APPROACH_LEADS = [
    (38, [(75, 560), (70, 800), (112, 1080), (150, 1380), (128, 1680), (64, 1960), (-40, 2160)]),
    (36, [(405, 560), (412, 780), (378, 1040), (428, 1300), (470, 1480), (540, 1620)]),
    (34, [(540, 2880), (392, 3080), (246, 3380), (196, 3700), (226, 4080), (336, 4480), (420, 4880),
          (466, 5260), (540, 5480)]),
    (32, [(-40, 4280), (80, 4560), (128, 4960), (92, 5420), (60, 5900), (40, 6200)]),
]
SKIMMER_SPEED = 180.0                                   # design/enemies/naval/skimmer: px/s along its lane,
WEAVE, WEAVE_PERIOD = 40.0, 1.2                         # its sine weave: px to each side, s per swing
HULL_HALF = 15                                          # the Skimmer's hull across its heading (wing tips, 14.5 px)
LANE_HALF = 58                                          # a lane's lead: 40 px weave + the hull + 2 px for the rim
ICE_SHARE = 0.56                                        # of the pack before the leads are cut (r09: about half ice)


# --------------------------------------------------------------------------- distances

def curve_dist(h, w, curves, y0=0):
    """Per pixel of an h x w field (rows from y0) the smallest d / half over the curves [(half, pts)],
    capped at 3 (computed in each segment's window only)."""
    out = np.full((h, w), 3.0)
    for half, pts in curves:
        reach = 3 * half
        for a, b in zip(pts[:-1], pts[1:]):
            x0 = int(max(0, min(a[0], b[0]) - reach))
            x1 = int(min(w, max(a[0], b[0]) + reach + 1))
            r0 = int(max(0, min(a[1], b[1]) - reach - y0))
            r1 = int(min(h, max(a[1], b[1]) + reach + 1 - y0))
            if x0 >= x1 or r0 >= r1:
                continue
            yy, xx = np.mgrid[r0:r1, x0:x1].astype(np.float64)
            yy += y0 + 0.5
            xx += 0.5
            ab = b - a
            ll = ab @ ab
            if ll == 0:
                d = np.hypot(xx - a[0], yy - a[1])
            else:
                tt = np.clip(((xx - a[0]) * ab[0] + (yy - a[1]) * ab[1]) / ll, 0, 1)
                d = np.hypot(xx - (a[0] + tt * ab[0]), yy - (a[1] + tt * ab[1]))
            np.minimum(out[r0:r1, x0:x1], d / half, out=out[r0:r1, x0:x1])
    return out


def lead_curves():
    """Every lead of the strip as (half width, points in (x, strip row))."""
    out = []
    for t, c in wave_lanes():
        out.append((LANE_HALF, np.c_[c[:, 0], [y_img(t, r) for r in c[:, 1]]]))
    for half, pts in APPROACH_LEADS:
        out.append((half, catmull_rom(pts)))
    return out


def crossing_curves():
    """The Scuttlers' paths in strip rows (the halt's: rows = screen y)."""
    return [(30, np.c_[c[:, 0], [y_img(t, r) for r in c[:, 1]]]) for t, c in walker_paths()]


# =========================================================================== the ice

ICE9 = s9.ICE9
ICE_COLOURS = 10
UNDER = np.array((52, 124, 142), float)                 # r09's teal submerged margins
WATER_RGB = np.array(s9.ARCTIC_STOPS9[0][1], float)
FOAM = (214, 228, 236)


def shifted(a, dy, dx):
    """``a`` moved down dy and right dx px, zero-filled (no wrap)."""
    out = np.zeros_like(a)
    h, w = a.shape
    out[max(0, dy):h + min(0, dy), max(0, dx):w + min(0, dx)] = a[max(0, -dy):h - max(0, dy), max(0, -dx):w - max(0, dx)]
    return out


def blur(mask, radius):
    im = Image.fromarray(np.clip(mask * 255, 0, 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(radius))
    return np.asarray(im, dtype=np.float64) / 255


def blobs(shape, points, seed):
    """A mask of small irregular ice fragments at points [(x, y, r)]."""
    h, w = shape
    img = Image.new("L", (w * 2, h * 2), 0)
    d = ImageDraw.Draw(img)
    rng = np.random.default_rng(seed)
    for x, y, r in points:
        k = int(rng.integers(5, 8))
        ang = np.sort(rng.uniform(0, TAU, k))
        rad = r * rng.uniform(0.6, 1.15, k)
        d.polygon([(2 * (x + rr * math.cos(a)), 2 * (y + rr * math.sin(a) * 0.85)) for a, rr in zip(ang, rad)], fill=255)
    return np.asarray(img.resize((w, h), Image.BOX), dtype=np.float64) / 255 > 0.5


def shade_ice(ice, seed, rng_seed):
    """r09's ice look on a mask: plateau height with pressure ridges, hillshade lit from the top-left,
    the ICE9 ramp, a darker rim, posterized to 10 colours with the mode filter; the teal submerged
    margin, the floes' long cool shadows on the water and the waterline foam. Returns RGBA (float)."""
    h, w = ice.shape
    icef = ice.astype(np.float64)
    hgt = blur(icef, 3.0)
    rid = raster.fbm(w, h, 32, seed + 1, octaves=2, period=True) if h % 32 == 0 and w % 32 == 0 else \
        raster.fbm(w, h, 32, seed + 1, octaves=2, period=False)
    hgt = hgt * (0.8 + 0.25 * rid) + np.where(np.abs(rid - 0.5) < 0.02, 0.08, 0) * ice
    shade = s6.hillshade(hgt * 30, 0.7)
    col = raster.ramp(ICE9, np.clip(0.25 + 0.55 * hgt * (0.7 + 0.3 * rid), 0, 1)) * shade[..., None]
    inner = ice & shifted(ice, 2, 2).astype(bool) & shifted(ice, -1, -1).astype(bool)
    rim = ice & ~inner
    col[rim] *= 0.8
    col[~ice] = ICE9[0][1]
    ice_img = r02.posterize(raster.to_rgba_image(np.clip(col, 0, 255), ice * 255), ICE_COLOURS)
    ice_img = ice_img.filter(ImageFilter.ModeFilter(5))
    ia = np.asarray(ice_img).astype(np.float64)
    ia[..., 3] = np.where(ice, 255, 0)                  # the mode filter keeps the forms; the mask stays exact
    grow = blur(icef, 6.0)
    margin = np.clip((grow - 0.05) * 3.0, 0, 1) * ~ice
    under_a = np.round(margin * 160 / 255 * 5) / 5 * 255
    cast = shifted(blur(icef, 2.0), 12, 8) * ~ice
    cast_a = np.round(cast * 120 / 255 * 4) / 4 * 255
    out = np.zeros((h, w, 4))
    out[..., :3] = WATER_RGB
    out[..., 3] = 3                                      # the water: the shadow stencil, the deep shows through
    over_a(out, np.broadcast_to(UNDER, (h, w, 3)), under_a)
    over_a(out, np.zeros((h, w, 3)), cast_a)
    over_a(out, ia[..., :3], ia[..., 3])
    # the waterline foam: dots on 45 % of the floes' edge pixels (r09), static
    edge = ice & ~(shifted(ice, 1, 0).astype(bool) & shifted(ice, -1, 0).astype(bool)
                   & shifted(ice, 0, 1).astype(bool) & shifted(ice, 0, -1).astype(bool))
    ys, xs = np.nonzero(edge)
    rng = np.random.default_rng(rng_seed)
    keep = rng.random(len(xs)) < 0.45
    foam = Image.new("L", (w * 2, h * 2), 0)
    d = ImageDraw.Draw(foam)
    for x, y, v in zip(xs[keep], ys[keep], rng.uniform(0.35, 1.0, keep.sum())):
        r = (0.8 + 0.7 * v) * 2
        d.ellipse([2 * x - r, 2 * y - r * 0.85, 2 * x + r, 2 * y + r * 0.85], fill=int(255 * 0.75 * v))
    fa = np.asarray(foam.resize((w, h), Image.BOX), dtype=np.float64) / 255
    fa = np.round(fa * 5) / 5 * 255
    over_a(out, np.broadcast_to(np.array(FOAM, float), (h, w, 3)), fa)
    out[..., 3] = np.maximum(out[..., 3], 3)
    return out


def over_a(dst, rgb, alpha):
    """Straight-alpha ``over`` of (rgb, alpha 0-255) onto the RGBA float array ``dst``, in place."""
    sa = alpha[..., None] / 255
    da = dst[..., 3:4] / 255
    oa = sa + da * (1 - sa)
    dst[..., :3] = np.where(oa > 0, (rgb * sa + dst[..., :3] * da * (1 - sa)) / np.maximum(oa, 1e-9), 0)
    dst[..., 3:4] = oa * 255


_STRIP = {}


def strip_field():
    """The strip's ice mask (rows 0 = the halt's top edge), the leads' distance and the rubble."""
    if "ice" in _STRIP:
        return _STRIP
    h = STRIP_H
    yy, xx = np.mgrid[0:h, 0:W].astype(np.float64) + 0.5
    n = raster.fbm(W, h, 96, 1311, octaves=4, period=True)
    rough = raster.fbm(W, h, 48, 1314, octaves=3, period=True)
    rough = (rough - rough.mean()) / (rough.std() + 1e-9)
    n = n - np.quantile(n, 1 - ICE_SHARE)                                             # ICE_SHARE of the pack is ice
    n = n + np.clip(1 - np.abs(yy - SCREEN / 2) / (SCREEN / 2 + 60), 0, 1) * 0.10     # the halt's frame: fuller floes
    n = n + np.clip(1.25 - np.hypot(xx - RELAY_X, yy - RELAY_R) / 110, 0, 1) * 0.9      # the relay's floe
    n = n + np.clip(1.4 - np.hypot(xx - HUT_X, yy - HUT_Y) / 46, 0, 1) * 0.9          # the hut's floe
    n = n - np.clip(1 - (yy - 18) / 16, 0, 1) * 0.6                                   # open water at the top edge
    dist = curve_dist(h, W, lead_curves())
    frame = (yy < SCREEN + 40) & (dist > 1.05)
    n = n + np.where(frame, 0.45, 0.0)                                                # the halt: floes between the leads
    cut = dist < 1 + 0.25 * np.maximum(rough, -0.08)                                 # >= 0.98 half (57 px): a weave's hull stays in its lead
    keep = (np.hypot(xx - HUT_X, yy - HUT_Y) < 34) | (np.hypot(xx - RELAY_X, yy - RELAY_R) < 70)
    n = np.where(cut & ~keep, -1.0, n)
    n = np.where(yy < 18, -1.0, n)
    ice = n > 0
    ice = blur(ice.astype(np.float64), 1.2) > 0.5
    # brash in the leads' margins and rubble where a walker crosses a lead (G4's walkers wade it)
    rng = np.random.default_rng(1321)
    cross = curve_dist(h, W, crossing_curves())
    pts = []
    margin = (dist > 1.0) & (dist < 1.2) & ~ice & (yy > 24)                            # outside the hulls' reach
    ys, xs = np.nonzero(margin)
    pick = rng.random(len(xs)) < np.where(ys < SCREEN + 40, 0.0001, 0.0010)
    pts += [(x, y, rng.uniform(1.4, 3.2)) for x, y in zip(xs[pick], ys[pick])]
    rubble = (cross < 1.0) & ~ice & (dist < 1.1) & (yy > 24)
    ys, xs = np.nonzero(rubble)
    pick = rng.random(len(xs)) < 0.0012
    pts += [(x, y, rng.uniform(2.6, 5.6)) for x, y in zip(xs[pick], ys[pick])]
    ice = ice | blobs((h, W), pts, 1322)
    ice[:18] = False
    _STRIP.update(ice=ice, dist=dist, choke=cross < 1.2)
    return _STRIP


def strip_image():
    if "rgba" not in _STRIP:
        f = strip_field()
        _STRIP["rgba"] = shade_ice(f["ice"], 1311, 1313)
    return _STRIP["rgba"]


def strip_piece(k):
    """pack-ice-k (1 = the start, the lowest): the strip's rows of that piece, quantized together."""
    if "pieces" not in _STRIP:
        rgba = strip_image()
        imgs = [Image.fromarray(np.clip(rgba[(PIECES_N - j) * PIECE_H:(PIECES_N - j + 1) * PIECE_H], 0, 255)
                                .astype(np.uint8), "RGBA") for j in range(1, PIECES_N + 1)]
        _STRIP["pieces"] = artkit.quantize_set(imgs, 48)
    return _STRIP["pieces"][k - 1]


def open_floes(w, h):
    """The exit's ground: looser floes and open sea (r09's ice at a higher threshold), periodic; open
    water round the rows where it begins at the halt's top edge, so the strip runs on into it."""
    n = raster.fbm(W, h, 96, 1331, octaves=4, period=True)
    rows = np.arange(h, dtype=np.float64)[:, None] + 0.5
    seam_row = h - 1 - (TOP_G % h)
    dd = np.abs(rows - seam_row) % h
    dd = np.minimum(dd, h - dd)
    n = n - np.clip(1 - dd / 46, 0, 1) * 0.9
    ice = n > 0.64
    ice = s6.pblur_mask(ice.astype(np.float64), 1.2) > 0.5
    ice[np.broadcast_to(dd < 16, ice.shape)] = False
    rgba = shade_periodic(ice, 1331)
    return artkit.quantize_set([Image.fromarray(np.clip(rgba, 0, 255).astype(np.uint8), "RGBA")], 40)[0]


def shade_periodic(ice, seed):
    """shade_ice on a periodic tile: shaded three times stacked, the middle kept (wrap-safe)."""
    h = ice.shape[0]
    rgba = shade_ice(np.vstack([ice, ice, ice]), seed, seed + 2)
    return rgba[h:2 * h]


# =========================================================================== the water

SWELL = [(2, 6, 0.70), (5, 9, 0.50), (-8, 20, 0.30), (14, 31, 0.20)]   # whole cycles across 480 x 960
SEA_AMP = 0.55                           # r09: calm leads, the swell damped by the pack
LIGHT_GAIN = 2.0
TONES = 7


def arctic_water(w, h):
    """deep: r09's arctic water (its stops, gain 2.4, 7 tones, the damped swell) as whole-cycle
    components with a periodic warp, so it tiles both ways (it drifts sideways), and a slow tone field."""
    rng = np.random.default_rng(1341)
    ph = rng.uniform(0, TAU, len(SWELL))
    warp = [raster.fbm(W, h, 120, 1342 + i, octaves=2, period=True) for i in range(2)]
    yy, xx = np.mgrid[0:h, 0:W].astype(np.float64)
    hgt = np.zeros((h, W))
    for i, ((nx, my, a), p) in enumerate(zip(SWELL, ph)):
        hgt += a * np.sin(TAU * nx * xx / W + TAU * my * yy / h + 5.0 * (warp[i % 2] - 0.5) + p)
    blobs_ = raster.fbm(W, h, 96, 1344, octaves=3, period=True) - 0.5      # r09's water is not stripes: a
    hgt = (0.45 * hgt + 2.4 * blobs_) * SEA_AMP                           # wandering swell, few crests
    gx = (np.roll(hgt, -1, 1) - np.roll(hgt, 1, 1)) / 2
    gy = (np.roll(hgt, -1, 0) - np.roll(hgt, 1, 0)) / 2
    nx, ny = -gx * 3.0, -gy * 3.0
    inv = 1.0 / np.sqrt(nx * nx + ny * ny + 1)
    light = ((nx * -0.48 + ny * -0.56 + inv * 0.68) - 0.68) * LIGHT_GAIN + 0.025 * hgt
    t = np.clip(0.5 + light, 0, 0.999)
    body = raster.ramp(s9.ARCTIC_STOPS9, np.clip(np.floor(t * TONES) / (TONES - 1), 0, 1))
    tone = raster.fbm(W, h, 160, 1343, octaves=3, period=True)[..., None]
    body = body * (0.86 + 0.28 * tone)
    img = Image.fromarray(np.clip(body, 0, 255).astype(np.uint8), "RGB").convert("RGBA")
    return artkit.quantize_set([img], 32)[0]


# =========================================================================== fog and whiteout

# Pale fog over white ice: every tone at or above the ice's lit snow (~(181, 200, 215) on average, its
# snow tops ~240), so a bank lightens what it covers and never greys it; soft, wide edges (G5b: r35's
# r03.bank shaded its thin edges dark and stepped its alpha in two hard levels: grey smudges on the ice).
FOG = [(222, 230, 237), (234, 240, 245), (244, 247, 250), (250, 252, 253)]
COVER = {"fog-wisps": 0.10, "fog-banks": 0.20}
FOG_SOFT = 0.42                          # noise units from the edge to the full density
FOG_ALPHA_LEVELS = 16


def fog_field(name, seed):
    n = r03.stretched(W, TILE_H, [240, 120, 60, 30, 15], seed, 3)
    if name == "fog-banks":
        xx = np.arange(W, dtype=np.float64)[None, :]
        e = np.clip((np.abs(xx - W / 2 + 0.5) - 90) / 150, 0, 1)
        return n + 0.22 * e - 0.10
    return n


def fog_image(field, cover, max_alpha):
    """A pale bank: alpha a smoothstep over FOG_SOFT, blurred (wrapping) for soft edges, the colour
    lighter towards the dense core and lit from the top-left."""
    n = s6.ptile(field)
    u = np.clip((n - cover) / FOG_SOFT, 0, 1)
    dens = u * u * (3 - 2 * u)
    soft = np.array(Image.fromarray((dens * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(6)),
                    np.float64) / 255
    gy, gx = np.gradient(soft * 40)
    shade = np.clip(0.30 + 0.70 * soft - (gx + gy) * 0.6, 0, 1)
    col = raster.ramp([(i / (len(FOG) - 1), c) for i, c in enumerate(FOG)], shade)
    img = s6.pmid(raster.to_rgba_image(col, soft * max_alpha), TILE_H)
    return r03.step_alpha(r02.posterize(img, 16), FOG_ALPHA_LEVELS)


def fog_tile(name, seed, max_alpha):
    field = fog_field(name, seed)
    lo, hi = 0.0, 1.2
    for _ in range(18):
        mid = (lo + hi) / 2
        cover = (np.array(fog_image(field, mid, max_alpha))[..., 3] > 8).mean()
        lo, hi = (mid, hi) if cover > COVER[name] else (lo, mid)
    return fog_image(field, (lo + hi) / 2, max_alpha)


def whiteout(w, h):
    """The heavy peak's banks: whiteout_l13's bank_layer (look a) at the peak, as wide as the play
    field and one noise period (540 px) tall, so it repeats without a line (rendered three periods
    tall and the middle one kept: the layer's light gradient is one-sided at its borders)."""
    per = whiteout_l13.H
    tall = whiteout_l13.bank_layer(WHITEOUT_LOOK, W, 3 * per, 0.0, 1.0).crop((0, per, W, 2 * per))
    return artkit.quantize_set([tall], 32)[0]


# =========================================================================== the loose ice

LOOSE = (96, 112)
LOOSE_HEADINGS = 8
MORTAR_FRAME = 2                         # heading east (90 deg): the mortar floe drifts right


def loose_frames():
    """The loose ice's eight heading frames: frame 2 the mortar floe (a flat floe 64 x 96 the two
    mortars stand on, 40 px apart), the others different drifting floes and brash clusters (a path
    picks one by its direction, so each direction shows its own shape)."""
    w, h = LOOSE
    frames = []
    rng = np.random.default_rng(1351)
    for k in range(LOOSE_HEADINGS):
        pts = []
        if k == MORTAR_FRAME:
            pts.append((w / 2, h / 2, 0))
        else:
            for _ in range(int(rng.integers(1, 3))):
                pts.append((w / 2 + rng.uniform(-14, 14), h / 2 + rng.uniform(-18, 18), 1))
            for _ in range(int(rng.integers(4, 9))):
                pts.append((w / 2 + rng.uniform(-30, 30), h / 2 + rng.uniform(-36, 36), 2))
        big = np.zeros((h, w), bool)
        yy, xx = np.mgrid[0:h, 0:w].astype(np.float64) + 0.5
        n = raster.fbm(w, h, 16, 1352 + k, octaves=3, period=False)
        for x, y, kind in pts:
            if kind == 0:
                d = np.hypot((xx - x) / 32, (yy - y) / 42)
                big |= d + 0.35 * (n - 0.5) < 1
            elif kind == 1:
                rx, ry = rng.uniform(10, 18), rng.uniform(8, 15)
                d = np.hypot((xx - x) / rx, (yy - y) / ry)
                big |= d + 0.5 * (n - 0.5) < 1
            else:
                big |= blobs((h, w), [(x, y, rng.uniform(2.0, 4.5))], 1360 + k)
        big[:4], big[-14:], big[:, :4], big[:, -12:] = False, False, False, False
        rgba = shade_ice(big, 1353 + k, 1354 + k)
        rgba[..., 3] = np.where(rgba[..., 3] <= 3, 0, rgba[..., 3])     # no stencil water round a loose floe
        frames.append(Image.fromarray(np.clip(rgba, 0, 255).astype(np.uint8), "RGBA"))
    return artkit.quantize_set(frames, 32)


# --------------------------------------------------------------------------- the layout

TILE_SETS = {   # id -> (layer, height, drift, note)
    "arctic-water": ("deep", TILE_H, WATER_DRIFT, "the dark arctic water under the ice (r09's stops, the swell damped by "
                                                   "the pack), opaque, every section; drifting left on the real clock: "
                                                   "the leads' water moves in the halt"),
    "open-floes": ("ground", TILE_H, None, "the exit: looser floes and open sea northward (translucent water, alpha >= 3); "
                                           "open water where it begins at the halt's top edge"),
    "fog-wisps": ("low-air", TILE_H, None, "light: thin pale fog wisps, ~10 %"),
    "fog-banks": ("low-air", TILE_H, None, "medium: the fog thickening at the edges, ~20 %, the centre clear"),
    "whiteout": ("low-air", whiteout_l13.H, None, "heavy: the whiteout's banks from both side edges (whiteout_l13 look a), "
                                                  "the centre round the relay clear; they hide the ground units at the edges"),
}
SECTION_TILES = [["arctic-water", "open-floes"] if i >= EXIT else ["arctic-water"] for i in range(len(SECTIONS))]
PIECE_IDS = [f"pack-ice-{k}" for k in range(1, PIECES_N + 1)]
PIECES = {      # id -> (layer, (w, h), extra spec, note)
    **{pid: ("ground", (W, PIECE_H), {}, None) for pid in PIECE_IDS},
    "loose-ice": ("ground", LOOSE, {"headings": LOOSE_HEADINGS},
                  "8 headings, each a different floe or brash cluster; heading 2 (east) the mortars' floe"),
}
PIECE_IDS_NOTE = ("the approach and the halt's frame, one render cut in four (the leads along the lanes, the relay's "
                  "and the hut's floes)")

def loose_paths():
    """The loose floes' straight paths at 6 px/s in the halt (frame, from, to in screen px, start t,
    repeats, every s): the halt's open water is the leads (the lanes fill the ring round the relay's
    floe), so they drift along the leads' outer margins: westward along the south lead, down the west
    lead, up the east lead, north-eastward across the south-east corner (found by --check's search
    for straight lines with the least ice under them)."""
    return [
        (6, (540, 472), (-60, 472), 40.0, 2, 100.0),
        (4, (66, -60), (66, 600), 46.0, 1, 0.0),
        (0, (422, 600), (422, -60), 96.0, 1, 0.0),
        (1, (190, 574), (508, 256), 150.0, 1, 0.0),
    ]


def floe_path():
    """The mortars' floe: in at the data's side and time (the mortars spawn on its centre, MORTAR_HALF
    outside the edge) at its drift, entering 5.8 s earlier off-screen so it never pops in."""
    y = sum(FLOE_YS) / 2
    x_spawn = -MORTAR_HALF if FLOE_LEFT else W + MORTAR_HALF
    sgn = 1 if FLOE_LEFT else -1
    lead = (LOOSE[0] / 2 + 10 + (x_spawn if FLOE_LEFT else W - x_spawn)) / FLOE_DRIFT
    t0 = FLOE_T - lead
    x0 = x_spawn - sgn * FLOE_DRIFT * lead
    x1 = W + LOOSE[0] / 2 + 4 if FLOE_LEFT else -LOOSE[0] / 2 - 4
    t1 = FLOE_T + abs(x1 - x_spawn) / FLOE_DRIFT
    return y, (round(t0, 2), x0), (round(t1, 2), x1)


def r4(v):
    return float(round(v, 4))


def t_for_scroll(target):
    """A time whose scroll is ``target`` (bisection on scroll_at), to 6 decimals."""
    lo, hi = -10.0, OUTRO_END
    for _ in range(80):
        mid = (lo + hi) / 2
        if scroll_at(mid) < target:
            lo = mid
        else:
            hi = mid
    return round(hi, 6)


def strip_placements():
    out = []
    bottom = TOP_G - STRIP_H
    for k, pid in enumerate(PIECE_IDS):
        b = bottom + k * PIECE_H
        t = round(t_for_scroll(b + PIECE_H / 2 - MID), 4)    # as the data writes it
        centre = scroll_at(t) + MID
        assert b11.java_round(centre - PIECE_H / 2) == b and abs(centre - PIECE_H / 2 - b) < 0.05, (pid, t, centre, b)
        out.append({"piece": pid, "t": t, "x": W // 2})
    return out


def path_entry(frame, a, b, t0, speed_):
    """A placement of loose-ice in the halt (its place at HALT_FROM: the screen's centre) with a
    straight path a -> b (screen px, y down) from t0 at speed_."""
    del frame
    dist = math.hypot(b[0] - a[0], b[1] - a[1])
    t1 = t0 + dist / speed_
    return {"piece": "loose-ice", "t": HALT_FROM, "x": W // 2,
            "path": [[r4(t0), r4(a[0] - W / 2), r4(MID - a[1])], [r4(t1), r4(b[0] - W / 2), r4(MID - b[1])]]}


def heading_frame(a, b):
    deg = math.degrees(math.atan2(b[0] - a[0], -(b[1] - a[1]))) % 360
    return int(math.floor(deg * LOOSE_HEADINGS / 360 + 0.5)) % LOOSE_HEADINGS


def placements():
    out = []
    for i, e in enumerate(strip_placements()):
        out.append((e, f"{PIECE_IDS_NOTE}: ground {TOP_G - STRIP_H + i * PIECE_H}-{TOP_G - STRIP_H + (i + 1) * PIECE_H} "
                       "(exact rows: re-run --proposal after the data moves a section end)" if i == 0 else None))
    y, (t0, x0), (t1, x1) = floe_path()
    floe = {"piece": "loose-ice", "t": HALT_FROM, "x": W // 2,
            "path": [[r4(t0), r4(x0 - W / 2), r4(MID - y)], [r4(t1), r4(x1 - W / 2), r4(MID - y)]]}
    assert heading_frame((x0, y), (x1, y)) == MORTAR_FRAME
    out.append((floe, f"the mortars' floe (heading east): in from the {'left' if FLOE_LEFT else 'right'} at {FLOE_DRIFT:g} px/s, "
                      f"its centre at the mortars' spawn ({-MORTAR_HALF}, {y:g} below the top) at t {FLOE_T:g}"))
    first = True
    for frame, a, b, t0, count, every in loose_paths():
        e = path_entry(frame, a, b, t0, LOOSE_SPEED)
        assert heading_frame(a, b) == frame, (frame, heading_frame(a, b))
        if count > 1:
            e["repeat"] = {"count": count, "every": every}
        out.append((e, "the loose ice drifting 6 px/s in the halt's open water, outside the lanes (each direction its "
                       "own heading frame)" if first else None))
        first = False
    return out


# --------------------------------------------------------------------------- the block

def block():
    tile_sets = {}
    for name, (layer, height, drift, _) in TILE_SETS.items():
        spec = {"layer": layer, "height": height}
        if drift:
            spec["drift"] = drift
        tile_sets[name] = spec
    pieces = {name: {"layer": layer, "size": [w, h], **extra} for name, (layer, (w, h), extra, _) in PIECES.items()}
    return {"scroll_factors": dict(FACTORS), "ramp": RAMP, "haze_colour": HAZE_COLOUR,
            "atmosphere": ATMOSPHERE, "tile_sets": tile_sets, "pieces": pieces,
            "placed": [e for e, _ in placements()]}


flow = b11.flow


def block_lines():
    b = block()
    lines = [
        "backdrop:",
        "  # Presentation only: the pack ice round Nansen Relay (tools/art/backdrop_l13.py, straight from",
        "  # scene-arctic-r09-a, G9 = a; M5 part G, step G5). The ground is one unique strip (pack-ice-1..4, the",
        "  # approach and the halt's frame, the leads cut along the waves' lanes) over the drifting `deep` water;",
        "  # the exit's `open-floes`; fog and the whiteout (whiteout_l13 look a) as the atmosphere's banks.",
        "  scroll_factors: " + flow(b["scroll_factors"]),
        f"  ramp: {RAMP}",
        f"  haze_colour: {HAZE_COLOUR}  # the arctic's pale grey-blue over the water",
        "  atmosphere:",
    ]
    notes = {"clear": "the exit: no fog", "light": "thin fog wisps ~10 %",
             "medium": "the fog thickening at the edges ~20 % (t=140 on)",
             "heavy": "the whiteout's peak: banks from both side edges, the centre clear"}
    for key, look in ATMOSPHERE.items():
        lines.append(f"    {key}: {flow(look)}   # {notes[key]}")
    lines.append("  tile_sets:")
    for name, spec in b["tile_sets"].items():
        lines.append(f"    {name}: {flow(spec)}   # {TILE_SETS[name][3]}")
    lines.append("  pieces:")
    for name, spec in b["pieces"].items():
        note = PIECES[name][3]
        lines.append(f"    {name}: {flow(spec)}" + (f"   # {note}" if note else ""))
    lines.append("  placed:")
    for entry, comment in placements():
        if comment:
            lines.append(f"    # {comment}")
        lines.append(f"    - {flow(entry)}")
    return lines


def write_proposal():
    lines = [
        "# Level 13 backdrop PROPOSAL (tools/art/backdrop_l13.py --proposal; M5 part G batch, round 35, step G5).",
        "# `backdrop` is the level's block; every section takes the `tiles` below.",
        "#",
        "# sections[i].tiles (by section, in order):",
    ]
    for s, tiles in zip(SECTIONS, SECTION_TILES):
        lines.append(f"#   {s['name']}: [{', '.join(tiles)}]")
    lines += [
        "#",
        f"# Built round this timeline: the halt from t {HALT_FROM:g} to {HALT_TO:g} at ground scroll {HALT_SCROLL:.2f}; every",
        f"#   section from the halt on begins at ground {TOP_G} (the halt's top edge), so the halt shows the strip.",
        "",
    ] + block_lines()
    PROPOSAL.parent.mkdir(parents=True, exist_ok=True)
    PROPOSAL.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"proposal: {PROPOSAL.relative_to(ROOT)}")


def level_backdrop():
    own = DATA.get("backdrop") or {}
    if own and "images" not in own:
        return own, "data.yaml"
    write_proposal()
    return yaml.safe_load(PROPOSAL.read_text(encoding="utf-8"))["backdrop"], PROPOSAL.name


# =========================================================================== render, write

_LOOSE = {}


def loose_ice(w, h, k):
    if "frames" not in _LOOSE:
        _LOOSE["frames"] = loose_frames()
    return _LOOSE["frames"][k]


def fog_wisps(w, h):
    return fog_tile("fog-wisps", 1361, 110)


def fog_banks(w, h):
    return fog_tile("fog-banks", 1363, 150)


GENERATORS = {"arctic-water": arctic_water, "open-floes": open_floes, "fog-wisps": fog_wisps,
              "fog-banks": fog_banks, "whiteout": whiteout}


def render_tile(name, w, h):
    return GENERATORS[name](w, h)


def write_all(backdrop, wanted):
    """Every image of the block (the strip's pieces rendered once, in this process)."""
    OUT.mkdir(parents=True, exist_ok=True)
    tiles = [(n, (W, s["height"])) for n, s in backdrop["tile_sets"].items() if not wanted or n in wanted]
    with ProcessPoolExecutor() as pool:
        futures = [(name, size, pool.submit(render_tile, name, *size)) for name, size in tiles]
        if not wanted or set(PIECE_IDS) & wanted:
            for k, pid in enumerate(PIECE_IDS, 1):
                save(pid, strip_piece(k))
        if not wanted or "loose-ice" in wanted:
            for k in range(LOOSE_HEADINGS):
                save(f"loose-ice_{k}", loose_ice(*LOOSE, k))
        for name, size, fut in futures:
            img = fut.result()
            if img.size != size:
                raise ValueError(f"{name}: rendered {img.size}, the block says {size}")
            save(name, img)


def save(name, img):
    artkit.save_png(img, OUT / f"{name}.png", SOURCE)
    print(f"{name}: {img.size[0]}x{img.size[1]}, {artkit.colour_count([img])} colours")


_cache = {}


def cached(name):
    if name not in _cache:
        _cache[name] = np.array(Image.open(OUT / f"{name}.png").convert("RGBA")).astype(np.float64)
    return _cache[name]


def piece_images(backdrop, name):
    spec = backdrop["pieces"][name]
    count = spec.get("headings") or spec.get("frames")
    return [cached(f"{name}_{k}") for k in range(count)] if count else [cached(name)]


# =========================================================================== the game's draw

over, add = b11.over, b11.add
java_round = b11.java_round


def path_offset(path, t):
    if not path:
        return 0.0, 0.0
    if t <= path[0][0]:
        return path[0][1], path[0][2]
    for a, b in zip(path, path[1:]):
        if t < b[0]:
            f = (t - a[0]) / (b[0] - a[0])
            return a[1] + (b[1] - a[1]) * f, a[2] + (b[2] - a[2]) * f
    return path[-1][1], path[-1][2]


def expanded(backdrop):
    out = []
    for p in backdrop["placed"]:
        rep = p.get("repeat")
        for n in range(rep["count"] if rep else 1):
            sh = rep["every"] * n if rep else 0.0
            q = dict(p)
            q["t"] = p["t"] + sh
            if p.get("path"):
                q["path"] = [[w[0] + sh, w[1], w[2]] for w in p["path"]]
            q.pop("repeat", None)
            out.append(q)
    return out


def piece_screen(backdrop, placed, scroll, t):
    """(left, top) in screen px (y down) of a placed piece at ground scroll ``scroll`` and time t."""
    spec = backdrop["pieces"][placed["piece"]]
    w, h = spec["size"]
    f = backdrop["scroll_factors"][spec["layer"]]
    bottom = java_round(scroll_at(placed["t"]) * f + MID - h / 2)
    dx, dy = path_offset(placed.get("path"), t)
    y = bottom - java_round(scroll * f) + java_round(dy)
    x = java_round(placed["x"] + java_round(dx) - w / 2)
    return x, SCREEN - y - h, w, h


def image_of(backdrop, placed, t):
    imgs = piece_images(backdrop, placed["piece"])
    spec = backdrop["pieces"][placed["piece"]]
    if spec.get("headings"):
        p = placed["path"]
        i = 1
        while i < len(p) - 1 and t >= p[i][0]:
            i += 1
        a, b = p[i - 1], p[i]
        deg = math.degrees(math.atan2(b[1] - a[1], b[2] - a[2])) % 360
        return imgs[java_round(deg * len(imgs) / 360) % len(imgs)]
    return imgs[0]


def draw_pieces(backdrop, frame, layer, scroll, t):
    for placed in expanded(backdrop):
        spec = backdrop["pieces"][placed["piece"]]
        if spec["layer"] != layer:
            continue
        x, top, w, h = piece_screen(backdrop, placed, scroll, t)
        y0, y1 = max(0, top), min(SCREEN, top + h)
        x0, x1 = max(0, x), min(W, x + w)
        if y0 >= y1 or x0 >= x1:
            continue
        img = image_of(backdrop, placed, t)
        over(frame[y0:y1, x0:x1], img[y0 - top:y1 - top, x0 - x:x1 - x])


def tile_rows(name, rows, shift=0):
    img = cached(name)
    hgt = img.shape[0]
    out = img[[hgt - 1 - (int(r) % hgt) for r in rows]]
    return np.roll(out, shift, axis=1) if shift else out


def draw_layer_tiles(frame, layer, scroll, backdrop, real, sections_tiles):
    rows = scroll + (SCREEN - 1 - np.arange(SCREEN))
    for s in range(len(SECTIONS)):
        tiles = [n for n in sections_tiles[s] if backdrop["tile_sets"][n]["layer"] == layer]
        if not tiles:
            continue
        lo = -math.inf if s == 0 else seam(layer, s)
        hi = seam(layer, s + 1) if s + 1 < len(SECTIONS) else math.inf
        sel = (rows >= lo) & (rows < hi)
        if sel.any():
            drift = backdrop["tile_sets"][tiles[0]].get("drift", 0) or 0
            shift = int(math.floor(drift * real + 0.5)) % W
            sub = frame[sel]
            over(sub, tile_rows(tiles[0], rows[sel], shift))
            frame[sel] = sub


def stretches():
    out = []
    for i, s in enumerate(SECTIONS):
        start = sec_start(i)
        peak = s.get("peak")
        if peak:
            out += [(start, peak["from"], s["atmosphere"]), (peak["from"], peak["to"], peak["atmosphere"]),
                    (peak["to"], s["end"], s["atmosphere"])]
        else:
            out.append((start, s["end"], s["atmosphere"]))
    return [s for s in out if s[1] > s[0]]


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


def draw_banks(backdrop, frame, look_a, look_b, weight, scroll):
    s = java_round(scroll * backdrop["scroll_factors"]["low-air"])
    rows = s + (SCREEN - 1 - np.arange(SCREEN))
    for look, share in ((look_a, 1 - weight), (look_b, weight)) if look_a is not look_b else ((look_b, 1.0),):
        name = look.get("banks")
        if name and share > 0:
            over(frame, tile_rows(name, rows), share)


_SPR = {}


def spr(name):
    if name not in _SPR:
        _SPR[name] = [np.array(f).astype(np.float64) for f in artkit.load_frames(name)]
    return _SPR[name]


def put(frame, img, x, y, alpha=1.0):
    h, w = img.shape[:2]
    left, top = java_round(x - w / 2), java_round(y - h / 2)
    y0, y1 = max(0, top), min(SCREEN, top + h)
    x0, x1 = max(0, left), min(W, left + w)
    if y0 < y1 and x0 < x1:
        over(frame[y0:y1, x0:x1], img[y0 - top:y1 - top, x0 - left:x1 - left], alpha)


def along(curve, s):
    """The point and direction at arc length s along a sampled curve (None past its end)."""
    seg = np.hypot(*np.diff(curve, axis=0).T)
    cum = np.concatenate([[0], np.cumsum(seg)])
    if s < 0 or s > cum[-1]:
        return None
    i = min(np.searchsorted(cum, s, side="right") - 1, len(seg) - 1)
    f = (s - cum[i]) / max(seg[i], 1e-9)
    p = curve[i] + (curve[i + 1] - curve[i]) * f
    d = (curve[i + 1] - curve[i]) / max(seg[i], 1e-9)
    return p, d


def heading16(d):
    """The units' heading index of a screen direction: k x 22.5 deg clockwise from straight down."""
    deg = math.degrees(math.atan2(-d[0], d[1])) % 360
    return int(math.floor(deg / 22.5 + 0.5)) % 16


def units_at(t, scroll):
    """Stand-ins for the ground units and the play plane at t: the relay, the Skimmers on their lanes
    (weave, wake), the Scuttlers on their paths, the mortars on their floe, the hut; the ship."""
    ground, air = [], []
    relay_y = RELAY_R + (scroll - HALT_SCROLL)
    if -60 < relay_y < SCREEN + 60:
        ground.append(("relay", spr("nansen-relay")[0], RELAY_X, relay_y))
    hut_y = scroll - scroll_at(HUT_T) - HUT_SIZE[1] / 2
    if -30 < hut_y < SCREEN + 30:
        ground.append(("hut", spr("research-hut")[0], HUT_X, hut_y))
    for w in DATA["waves"]:
        lanes = w.get("lanes")
        if not lanes or not (w["t"] <= t <= w["t"] + 9):
            continue
        for entry, k, mirror in lane_units(w)[:int(w.get("count", 1))]:   # medium's units
            lane = catmull_rom(lanes[k])
            if mirror:
                lane[:, 0] = W - lane[:, 0]
            s = (t - entry) * SKIMMER_SPEED
            at = along(lane, s)
            if at is None:
                continue
            p, d = at
            off = WEAVE * math.sin(TAU * (t - entry) / WEAVE_PERIOD)
            x = p[0] + off * -d[1]
            y = p[1] + off * d[0] + (scroll_at(t) - scroll_at(entry))
            hd = heading16(d)
            ground.append(("wake", spr("skimmer-wake")[hd * 8 + int(t * 10) % 8], x, y))
            ground.append(("skimmer", spr("skimmer")[hd * 2 + int(t * 8) % 2], x, y))
    for wt, c in walker_paths():
        if wt <= t <= wt + 14:
            at = along(c, (t - wt) * 40)
            if at:
                p, d = at
                ground.append(("scuttler", spr("scuttler")[heading16(d) * 6 + int(t * 8) % 6], p[0], p[1]))
    if FLOE_T <= t <= FLOE_T + 60:
        x = (-MORTAR_HALF + FLOE_DRIFT * (t - FLOE_T)) if FLOE_LEFT else W + MORTAR_HALF - FLOE_DRIFT * (t - FLOE_T)
        for y in FLOE_YS:
            ground.append(("mortar", spr("polyp-mortar")[0], x, y))
    air.append(("ship", spr("ship")[0], 240 + 60 * math.sin(t * 0.5), 430))
    return ground, air


def composite(backdrop, t, units=True, sections_tiles=None):
    """The play field at t as the game draws it: deep (drifting), haze, the ground's tiles and pieces,
    the ground units, the low-air banks, the play plane (stand-ins)."""
    sections_tiles = sections_tiles or SECTION_TILES
    scroll = scroll_at(t)
    frame = np.zeros((SCREEN, W, 4))
    frame[..., 3] = 255
    look_a, look_b, weight = looks_at(backdrop, t)
    draw_layer_tiles(frame, "deep", java_round(scroll * backdrop["scroll_factors"]["deep"]), backdrop, t, sections_tiles)
    veil = look_a["haze"] + (look_b["haze"] - look_a["haze"]) * weight
    hz = np.array([int(backdrop["haze_colour"][i:i + 2], 16) for i in (0, 2, 4)], float)
    frame[..., :3] = frame[..., :3] * (1 - veil) + hz * veil
    draw_layer_tiles(frame, "ground", java_round(scroll), backdrop, t, sections_tiles)
    draw_pieces(backdrop, frame, "ground", scroll, t)
    ground, air = units_at(t, scroll) if units else ([], [])
    for _, img, x, y in ground:
        put(frame, img, x, y)
    draw_banks(backdrop, frame, look_a, look_b, weight, scroll)
    draw_pieces(backdrop, frame, "low-air", scroll, t)
    for _, img, x, y in air:
        put(frame, img, x, y)
    return Image.fromarray(np.clip(frame[..., :3], 0, 255).astype(np.uint8), "RGB")


# =========================================================================== checks

def ice_at_screen(t, x, y):
    """Whether the strip has ice at screen (x, y) at t (False above the strip: open-floes' water)."""
    f = strip_field()
    row = int(math.floor(y_img(t, y)))
    if row < 0 or row >= STRIP_H or not 0 <= x < W:
        return False
    return bool(f["ice"][row, int(x)])


def in_choke(t, x, y):
    """Whether screen (x, y) at t lies on the rubble band where a Scuttler's path crosses a lead."""
    row = int(math.floor(y_img(t, y)))
    return 0 <= row < STRIP_H and 0 <= x < W and bool(strip_field()["choke"][row, int(x)])


def check_layout(backdrop):
    problems = []
    # Each unit's run as the sim flies it: from its entry, 180 px/s along its lane in ground space, its
    # lane's points (centre) and its hull on the weave (40 px, 1.2 s) across its full width (the centre
    # +- HULL_HALF across the lane), each counted while on screen; a hull counts as wet only when both
    # wing tips and its centre are on water.
    per_wave = {}
    for wave_t, entry, c in lane_runs():
        length = float(np.hypot(*np.diff(c, axis=0).T).sum())
        counts = per_wave.setdefault(wave_t, [0, 0, 0, 0, 0])   # centre wet, total, hull wet, total, on rubble
        for s_ in np.arange(0.0, length, 6.0):
            p, d = along(c, s_)
            t_here = entry + s_ / SKIMMER_SPEED
            moved = scroll_at(t_here) - scroll_at(entry)
            off = WEAVE * math.sin(TAU * (t_here - entry) / WEAVE_PERIOD)
            if 0 <= p[0] < W and 0 <= p[1] + moved < SCREEN:
                counts[1] += 1
                counts[0] += not ice_at_screen(entry, *p)
            hull = [(p[0] - o * d[1], p[1] + o * d[0]) for o in (off - HULL_HALF, off, off + HULL_HALF)]
            on = [(x, y) for x, y in hull if 0 <= x < W and 0 <= y + moved < SCREEN]
            if on:
                counts[3] += 1
                hit = [(x, y) for x, y in on if ice_at_screen(entry, x, y)]
                counts[2] += not hit
                counts[4] += bool(hit) and all(in_choke(entry, x, y) for x, y in hit)
    wet = sum(v[0] for v in per_wave.values())
    total = sum(v[1] for v in per_wave.values())
    shares = ", ".join(f"t {k:g} {v[0] / max(v[1], 1):.0%}/{v[2] / max(v[3], 1):.0%}/{v[4]}" for k, v in per_wave.items())
    print(f"  lanes on open water: {wet / max(total, 1):.1%} of {total} centre points (per wave, centre/hull on water/hull samples on rubble: {shares})")
    if wet < 0.96 * total:
        problems.append(f"lanes: only {wet / total:.0%} of their points on water")
    dry = sum(v[3] - v[2] for v in per_wave.values())
    choked = sum(v[4] for v in per_wave.values())
    print(f"  hulls touching ice: {dry} of {sum(v[3] for v in per_wave.values())} samples, {choked} of them only on the "
          "rubble where a Scuttler's path crosses a lead (G4: the lead is choked there)")
    for k, (cw, ct, hw, ht, hr) in per_wave.items():
        if ct == 0 or cw < 0.95 * ct or hw + hr < ht:
            problems.append(f"lanes of the t={k:g} wave: centre {cw}/{ct}, hull {hw}/{ht} points on water")
    if dry > choked:
        problems.append(f"lanes: {dry - choked} hull samples on floe ice")
    yy, xx = np.mgrid[-40:41:4, -40:41:4]
    relay_ice = np.mean([ice_at_screen(HALT_FROM, RELAY_X + dx, RELAY_R + dy) for dx, dy in zip(xx.ravel(), yy.ravel())])
    hy = HUT_Y
    hut_ice = np.mean([bool(strip_field()["ice"][int(hy + dy), int(HUT_X + dx)])
                       for dx in range(-24, 25, 4) for dy in range(-20, 21, 4)])
    print(f"  ice under the relay's 80x80: {relay_ice:.0%}, under the hut: {hut_ice:.0%}")
    if relay_ice < 0.98:
        problems.append("the relay does not stand on ice")
    if hut_ice < 0.95:
        problems.append("the hut does not stand on ice")
    top = strip_field()["ice"][:18].any()
    of = cached("open-floes")
    seam_row = of.shape[0] - 1 - (TOP_G % of.shape[0])
    of_seam = (of[max(0, seam_row - 12):seam_row + 12, :, 3] > 3).mean()
    print(f"  seam at ground {TOP_G}: strip top ice {top}, open-floes ice round its first row {of_seam:.1%}")
    if top or of_seam > 0.01:
        problems.append("ice at the seam where open-floes begins")
    for e in expanded(backdrop):
        if e["piece"] != "loose-ice":
            continue
        p = e["path"]
        bad = 0
        for k in range(0, 21):
            u = k / 20
            x = W / 2 + p[0][1] + (p[1][1] - p[0][1]) * u
            y = MID - (p[0][2] + (p[1][2] - p[0][2]) * u)
            if 0 <= x < W and 0 <= y < SCREEN:
                d = strip_field()["dist"][int(y), int(x)] if y < STRIP_H else 3
                bad += d < 0.75
        if bad and e["path"][0][0] != floe_path()[1][0]:
            print(f"  loose ice from t {p[0][0]:g}: {bad} of 21 samples inside a lane")
    return problems


def check_tiles(backdrop):
    problems = []
    for name, spec in backdrop["tile_sets"].items():
        a = cached(name)
        rows = b11.wrap_score(a)
        cols = b11.wrap_score(a, columns=True) if spec.get("drift") else 0.0
        cover = (a[..., 3] > 8).mean()
        print(f"  wrap {name}: rows {rows:.2f}, columns {cols:.2f}, cover {cover:.0%}")
        if max(rows, cols) > 1.5:
            problems.append(f"{name}: a line where it repeats ({rows:.2f}, {cols:.2f})")
        if spec["layer"] == "ground" and a[..., 3].min() < 3:
            problems.append(f"{name}: alpha < 3 (the shadow stencil)")
        if name in COVER and abs(cover - COVER[name]) > 0.03:
            problems.append(f"{name}: cover {cover:.0%}, not ~{COVER[name]:.0%}")
    w = cached("whiteout")[..., 3]
    centre = (w[:, 180:300] > 8).mean()
    print(f"  whiteout: the centre (x 180-300) {centre:.0%} covered, edges {(w[:, :60] > 8).mean():.0%}")
    if centre > 0.05:
        problems.append("whiteout: the centre round the relay is not clear")
    for pid in PIECE_IDS:
        if cached(pid)[..., 3].min() < 3:
            problems.append(f"{pid}: alpha < 3 (the shadow stencil)")
    return problems


def check_seams_between_pieces():
    """The strip's pieces continue each other: the rows either side of each cut change no more than
    the strip's rows usually do."""
    bad = []
    imgs = [cached(pid) for pid in PIECE_IDS]
    for lo, hi in zip(imgs, imgs[1:]):
        a, b = lo[0], hi[-1]                            # lo's top row meets hi's bottom row
        lum = lambda r: (r[:, :3] @ np.array([0.299, 0.587, 0.114])) * r[:, 3] / 255   # noqa: E731
        jump = np.abs(lum(a) - lum(b)).mean()
        ref = np.median([np.abs(lum(lo[k]) - lum(lo[k + 1])).mean() for k in range(0, 60, 3)])
        if jump > 2.0 * max(ref, 1.0):
            bad.append(f"a line between pieces ({jump:.1f} vs {ref:.1f})")
    return bad


def check_budget(backdrop):
    """BackdropCheck's motion budget and speeds, sampled each 0.1 s."""
    problems = []
    worst = 0
    for step in range(int(OUTRO_END * 10) + 1):
        t = step / 10
        scroll = scroll_at(t)
        animated = set()
        for e in expanded(backdrop):
            x, top, w, h = piece_screen(backdrop, e, scroll, t)
            if x + w > 0 and x < W and top + h > 0 and top < SCREEN and e.get("path"):
                animated.add(e["piece"])
        if any(s.get("drift") for s in backdrop["tile_sets"].values()):
            animated.add("drift")
        worst = max(worst, len(animated))
    print(f"  motion budget: at most {worst} animated elements at once")
    if worst > 2:
        problems.append(f"{worst} animated elements on screen at once (at most 2)")
    return problems


def check_fog():
    """G5b: the fog lies pale over the ice (never greys it) and shows over the water: each bank tile
    over the ice-covered strip rows and over the water, the luminance change where the fog lies."""
    problems = []
    ice = cached("pack-ice-4")[:TILE_H]
    water = cached("arctic-water")[:TILE_H]
    ground = water[..., :3] * (1 - ice[..., 3:] / 255) + ice[..., :3] * ice[..., 3:] / 255
    lum = np.array([0.299, 0.587, 0.114])
    for name in ("fog-wisps", "fog-banks"):
        fog = cached(name)
        on_ice, on_water = [], []
        for off in range(0, TILE_H, TILE_H // 8):
            f = np.roll(fog, -off, axis=0)
            a = f[..., 3:] / 255
            d = ((ground * (1 - a) + f[..., :3] * a) - ground) @ lum
            on_ice.append(d[(ice[..., 3] > 250) & (a[..., 0] > 0.15)])
            on_water.append(d[(ice[..., 3] < 5) & (a[..., 0] > 0.15)])
        di, dw = np.concatenate(on_ice), np.concatenate(on_water)
        print(f"  {name} over ice: {di.mean():+.1f} lum ({(di < -3).mean():.1%} of it darker by > 3), over water {dw.mean():+.1f}")
        if di.mean() < 0 or (di < -3).mean() > 0.05:
            problems.append(f"{name} greys the ice ({di.mean():+.1f} lum)")
    return problems


def run_checks(backdrop):
    problems = (check_layout(backdrop) + check_tiles(backdrop) + check_seams_between_pieces() + check_budget(backdrop)
                + check_fog())
    for p in problems:
        print("PROBLEM:", p)
    if not problems:
        print("checks: ok")
    return problems


def atlas_area(backdrop):
    area = sum(W * s["height"] for s in backdrop["tile_sets"].values())
    for name, s in backdrop["pieces"].items():
        area += s["size"][0] * s["size"][1] * (s.get("headings") or s.get("frames") or 1)
    print(f"backdrop area: {area:,} px = {area / 2048 ** 2:.2f} pages of 2048x2048")
    return area


# =========================================================================== review

TIMES = [(3, "1 THE START: PACK ICE, FOG WISPS"), (12.5, "1 THE RESEARCH HUT ON ITS FLOE"),
         (22, "1 THE SKIMMER INTRODUCTION'S LEAD"), (36, "1 THE RELAY'S FLOE SCROLLING IN"),
         (39, "1/2 THE EASE INTO THE HALT"), (58, "2 THE HALT: THE FIRST CROSS"),
         (72, "2 THE MORTARS' FLOE DRIFTING IN"), (106, "3 SCUTTLERS ON THE FLOES"),
         (150, "3 THE FOG THICKENING (MEDIUM)"), (176, "4 THE WHITEOUT RAMPING IN"),
         (182, "4 THE WHITEOUT'S PEAK (LOOK A)"), (205, "4 THE LAST CROSS"),
         (223, "5 GRID ONLINE: THE SCROLL RESUMES"), (230, "5 OPEN FLOES NORTHWARD")]


def load(name):
    return Image.open(OUT / f"{name}.png").convert("RGBA")


def review(backdrop):
    items = []
    for name in backdrop["tile_sets"]:
        im = load(name)
        items.append((f"{name} {backdrop['tile_sets'][name]['layer']} {im.width}x{im.height}",
                      im.resize((im.width // 3, im.height // 3), Image.BOX)))
    for pid in PIECE_IDS:
        im = load(pid)
        items.append((f"{pid} 480x{PIECE_H}", im.resize((im.width // 4, im.height // 4), Image.BOX)))
    lo = [load(f"loose-ice_{k}") for k in range(LOOSE_HEADINGS)]
    row = Image.new("RGBA", (LOOSE[0] * 4 + 12, LOOSE[1] * 2 + 4))
    for k, im in enumerate(lo):
        row.alpha_composite(im, ((k % 4) * (LOOSE[0] + 4), (k // 4) * (LOOSE[1] + 4)))
    items.append(("loose-ice 8 headings (2: the mortar floe)", row))
    width, x, y, row_h = 1640, 16, 44, 0
    rows = []
    for name, im in items:
        cell = max(im.width, 6 * len(name))
        if x + cell > width - 16:
            x, y, row_h = 16, y + row_h + 30, 0
        rows.append((name, im, x, y))
        x += cell + 14
        row_h = max(row_h, im.height)
    comp_y = y + row_h + 40
    cw = 240 + 12
    per_row = (width - 16) // cw
    n_rows = (len(TIMES) + per_row - 1) // per_row
    big_y = comp_y + n_rows * 300 + 20
    sheet = raster.sheet(width, big_y + 540 + 60, "LEVEL 13 BACKDROP - FINAL (PROPOSAL)",
                         "PRODUCTION ART, M5 PART G BATCH - R35; STRAIGHT FROM SCENE-ARCTIC-R09-A; WHITEOUT LOOK A; "
                         "COMPOSITES AS THE GAME DRAWS THEM (STAND-INS FOR THE UNITS), 1/2 SCALE")
    for name, im, px, py in rows:
        plate = Image.new("RGBA", im.size, artkit.PLATE)
        plate.alpha_composite(im)
        sheet.alpha_composite(plate, (px, py + 14))
        raster.draw_text(sheet, px, py, name.upper()[:48], raster.LABEL)
    for k, (t, label) in enumerate(TIMES):
        px, py = 16 + (k % per_row) * cw, comp_y + (k // per_row) * 300
        sheet.alpha_composite(composite(backdrop, t).convert("RGBA").resize((240, 270), Image.BOX), (px, py + 14))
        raster.draw_text(sheet, px, py, f"T={t:g} {label}", raster.LABEL)
    for k, (t, label) in enumerate([(60, "T=60 AT 1X: THE HALT'S FRAME, THE LEADS ALONG THE LANES"),
                                    (75, "T=75 AT 1X: NO UNITS: THE LEADS AND THE LOOSE ICE"),
                                    (182, "T=182 AT 1X: THE WHITEOUT'S PEAK")]):
        px = 16 + k * (480 + 24)
        sheet.alpha_composite(composite(backdrop, t, units=(k != 1)).convert("RGBA"), (px, big_y + 14))
        raster.draw_text(sheet, px, big_y, label, raster.LABEL)
    REVIEW.parent.mkdir(parents=True, exist_ok=True)
    sheet.convert("RGB").save(REVIEW, optimize=True)
    frames = [composite(backdrop, float(t)).resize((360, 405), Image.BOX)
              for t in list(np.arange(33.0, 44.0, 0.1)) + list(np.arange(172.0, 180.0, 0.2))]
    gif = REVIEW.with_suffix(".gif")
    artkit.write_gif(frames, gif, fps=10)
    print(f"review: {REVIEW.relative_to(ROOT)}, {gif.relative_to(ROOT)} {gif.stat().st_size / 1e6:.1f} MB")


def strip_frames(backdrop, times, path):
    shots = [composite(backdrop, t) for t in times]
    out = Image.new("RGB", (len(shots) * (W + 8), SCREEN), (0, 0, 0))
    for i, s in enumerate(shots):
        out.paste(s, (i * (W + 8), 0))
    out.save(path)
    print(f"strip: {path}")


def main(argv):
    global SECTION_TILES
    if "--proposal" in argv:
        write_proposal()
        return
    backdrop, source = level_backdrop()
    if source == "data.yaml":
        SECTION_TILES = [s["tiles"] for s in SECTIONS]
    print(f"backdrop from {source}")
    if "--strip" in argv:
        k = argv.index("--strip")
        strip_frames(backdrop, [float(a) for a in argv[k + 1].split(",")], argv[k + 2])
        return
    wanted = {a for a in argv if not a.startswith("--")}
    names = set(backdrop["tile_sets"]) | set(backdrop["pieces"])
    known = set(GENERATORS) | set(PIECE_IDS) | {"loose-ice"}
    if names - known or wanted - known:
        raise SystemExit(f"no generator for {sorted((names | wanted) - known)}")
    if "--check" in argv:
        atlas_area(backdrop)
        sys.exit(1 if run_checks(backdrop) else 0)
    if "--review" not in argv:
        write_all(backdrop, wanted)
        if not wanted:
            keep = {f"{n}" for n in backdrop["tile_sets"]} | set(PIECE_IDS) | {f"loose-ice_{k}" for k in range(LOOSE_HEADINGS)}
            for f in OUT.glob("*.png"):
                if f.stem not in keep:
                    f.unlink()
                    print(f"removed {f.name}")
        _cache.clear()
        if not wanted:
            run_checks(backdrop)
    atlas_area(backdrop)
    if not wanted:
        review(backdrop)


if __name__ == "__main__":
    main(sys.argv[1:])
