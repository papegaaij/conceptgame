#!/usr/bin/env python3
"""Production art: the Level 07 backdrop, the Earth-Moon L1 point beyond the overrun picket
(design/campaign/act-1-first-contact/level-07-brood-carrier; M4 part G batch, concept round 25).

Straight to production (part F's D8: the setting has a chosen scene, parallax r03 A). Built on the
Earth-orbit kit of backdrop_l01.py-backdrop_l03.py (loaded by path, imported unchanged): palette B,
the top-left key light, the station kit with its muted ground accent, posterized to 12-32 colours
per piece (one palette per frame or heading set), translucency stepped, wide gradients
ordered-dithered. Nothing lit is rotated or mirrored as an image: every turn is made in the model.

Outputs (assets/backdrop/level-07/, one PNG per tile set and set piece of the backdrop block,
named by its id and checked against its size; frames and headings as <id>_<n>.png):
  stars                              deep tile set (every section): open space at L1
  earth, moon                        deep: Earth's whole disc behind (lower left, its limb still at
                                     the bottom in the arena), the Moon ahead (upper right)
  sensor-station                     far, section 1: a sister sensor station breaking up
  picket-ring                        far, section 1: the overrun picket's ring segment, torn
  carrier-far_0, _1                  far, sections 2-3: the Brood Carrier ahead, a hazed silhouette
                                     in its lime glow (headings 2: 0 nose up, 1 nose down); its
                                     path holds it at the top edge while Lancer closes in, then
                                     it pulls away over the top in the heavy spore wake (88-94 s)
  fleet-line_0..3                    far, aftermath: the second fleet's contacts, a glittering line
                                     out from beyond the Moon (its lower end at the Moon's left limb)
  picket-platform, picket-half,      ground, sections 1-2: overrun CDF picket platforms, gun mounts
  gun-mount, picket-truss,           torn open, teal growth, a torn truss, wreck plates
  wreck-a, wreck-b, wreck-c
  spore-haze, spore-banks            low-air: the escort's spore haze and the carrier's spore wake
                                     (Level 03's generators)
  ichor-a, ichor-b, ichor-c          low-air, aftermath: ichor clouds drifting from the carcass
  ice-streaks, spore-streaks         high-air (additive): Level 02's frost streaks, Level 03's spore
                                     streaks
  design/campaign/.../level-07-brood-carrier/concept/backdrop-final-r25-a.png   review sheet: the
                                     pieces and composites of the level as Backdrop draws it

The carcass in the aftermath is the boss's own sprite (the game draws it on the play plane,
darkening and drifting after the chained death), so it has no backdrop piece.

**Data.** The backdrop block comes from the level's data.yaml once its block names no other
level's images (`images: level-03` is the stand-in), otherwise from backdrop-proposal.yaml next to
it, which --proposal writes (the block, the sections' `tiles` and the reasons). The deep layer
scrolls at 0.015: at L1 Earth and the Moon are 60,000-330,000 km away, so both stay on screen from
the first second to the aftermath (at 0.12 they would be gone after half a minute).

**Seams.** The tile sets come from periodic noise (every lattice divides the tile) or wrap their
stars and streaks; --check scores every tile set's wrap rows (premultiplied by alpha, so the
translucent banks count) and the drifting ones' wrap columns as game/.../BackdropSeamsTest does,
checks that no piece shows an edge on screen, the motion budget and density over the level, and
that nothing but the deep layer is on screen in the arena or when an early kill makes the clock
jump to its end.

Run: python3 tools/art/backdrop_l07.py [--review | --check | --proposal] [id ...]   (~2 min on 20 cores)
"""
import importlib.util
import math
import sys
from concurrent.futures import ProcessPoolExecutor
from pathlib import Path

import numpy as np
import yaml
from PIL import Image, ImageFilter

import artkit
from artkit import DESIGN, ROOT, raster, sprite

from parallax_r02 import haze  # noqa: E402  (concept script, imported unchanged)
from render import enemy_models as em  # noqa: E402
from render import sdf, station  # noqa: E402
from render.sdf import (rotate_z, sd_box, sd_capsule, sd_cylinder_z, sd_ellipsoid, sd_sphere,  # noqa: E402
                        union)
from render.station import ACCENT, DARK, HULL, RED  # noqa: E402


def _load(name, file):
    spec = importlib.util.spec_from_file_location(name, Path(__file__).resolve().parent / file)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


b3 = _load("art_backdrop_l03", "backdrop_l03.py")
b1, b2 = b3.b1, b3.b2
l01 = b1.l01
SEA = l01.SEA

SCRIPT = "backdrop_l07.py"
SOURCE = artkit.source_note(SCRIPT, "M4 part G batch")
LEVEL_DIR = DESIGN / "campaign" / "act-1-first-contact" / "level-07-brood-carrier"
PROPOSAL = LEVEL_DIR / "backdrop-proposal.yaml"
OUT = ROOT / "assets" / "backdrop" / "level-07"
REVIEW = LEVEL_DIR / "concept" / "backdrop-final-r25-a.png"
W, SCREEN = 480, 540
MID = SCREEN / 2
COLOURS = 32

FACTORS = {"deep": 0.015, "far": 0.45, "ground": 1.0, "low-air": 1.4, "high-air": 2.4}
HAZE_COLOUR = "464c3a"            # olive grey: the escort's spores
ATMOSPHERE = {
    "clear": {"haze": 0.02},                                                    # the carrier alone
    "light": {"haze": 0.05},
    "medium": {"banks": "spore-haze", "wisps": "spore-streaks", "haze": 0.12},  # the escort's haze
    "heavy": {"banks": "spore-banks", "wisps": "spore-streaks", "haze": 0.24},  # the carrier's wake
}
RAMP = 4
ICHOR = np.array([46, 86, 74], float)       # the carrier's ichor: dark teal, below the bullets
ICHOR_LIT = np.array([92, 132, 100], float)
LIME_SPECK = np.array([150, 196, 92], float)


# --------------------------------------------------------------------------- the level's timeline

DATA = yaml.safe_load((LEVEL_DIR / "data.yaml").read_text(encoding="utf-8"))
SECTIONS = DATA["sections"]
SPEED = float(DATA["scroll_speed"])
STARTS = [0.0] + [float(s["end"]) for s in SECTIONS[:-1]]
END = float(SECTIONS[-1]["end"])
ARENA = next(i for i, s in enumerate(SECTIONS) if s.get("arena"))
ARENA_END = float(SECTIONS[ARENA]["end"])
OUTRO_END = END + 15                   # LevelData.OUTRO_SECONDS


def speed(i):
    return float(SECTIONS[i].get("speed", SPEED))


def scroll_at(t):
    """LevelData.scrollAt: the ground's distance at t; before the start and after the end the first
    and the last section's speed carry on."""
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


# --------------------------------------------------------------------------- the layout

TILE_SETS = {   # id -> (layer, height, drift, note)
    "stars": ("deep", 960, None, "open space at L1, every section"),
    "spore-haze": ("low-air", 1120, 10, "Level 03's: the escort's olive-grey haze, ~24 % cover, translucent"),
    "spore-banks": ("low-air", 1120, 12, "Level 03's: the carrier's dense spore wake, ~50 % cover"),
    "ice-streaks": ("high-air", 960, None, "Level 02's frost streaks: section 1"),
    "spore-streaks": ("high-air", 960, None, "Level 03's"),
}
SECTION_TILES = [["stars", "ice-streaks"], ["stars"], ["stars"], ["stars"], ["stars"]]

EARTH_BAND = 300                 # the earth tile's rows the globe shows from (its coast and clouds)
EARTH = {"size": 360, "r": 168, "row": 232, "x": 120}   # deep: centre row (layer px), x
MOON = {"size": 96, "r": 44, "row": 486, "x": 372}
CARRIER = {"size": (200, 360), "x": 250}

PIECES = {      # id -> (layer, (w, h), extra spec, mid-size, note)
    "earth": ("deep", (EARTH["size"],) * 2, {}, False,
              "Earth's whole disc behind, smaller than in Levels 01-03; its limb stays at the bottom through the arena"),
    "moon": ("deep", (MOON["size"],) * 2, {}, False, "the Moon ahead"),
    "sensor-station": ("far", (200, 300), {}, True, "a sister sensor station breaking up"),
    "picket-ring": ("far", (480, 240), {}, True, "the overrun picket's ring segment, torn, its platforms overgrown"),
    "carrier-far": ("far", CARRIER["size"], {"headings": 2}, True,
                    "the carrier ahead in its glow: 0 nose up (flying ahead of Lancer), 1 nose down"),
    "fleet-line": ("far", (320, 120), {"frames": 4, "fps": 3}, False,
                   "the second fleet's contacts, a glittering line out from beyond the Moon"),
    "picket-platform": ("ground", (150, 150), {}, True, "an overrun CDF platform: gun dome torn open, teal growth"),
    "picket-half": ("ground", (120, 120), {}, True, "half a picket platform, overgrown"),
    "gun-mount": ("ground", (72, 72), {}, False, "a gun mount torn open, its barrels bent"),
    "picket-truss": ("ground", (480, 44), {}, False, "the picket's truss, torn through"),
    "wreck-a": ("ground", (64, 52), {}, False, None),
    "wreck-b": ("ground", (84, 60), {}, False, None),
    "wreck-c": ("ground", (56, 76), {}, False, None),
    "ichor-a": ("low-air", (200, 140), {}, False, "ichor drifting from the carcass"),
    "ichor-b": ("low-air", (150, 110), {}, False, None),
    "ichor-c": ("low-air", (230, 150), {}, False, None),
}

# (piece, t, x, comment); deep pieces are placed by their layer row, the carrier by its path
PLACED = [
    ("earth", round(t_for_centre("deep", EARTH["row"]), 2), EARTH["x"],
     f"deep: Earth's centre {EARTH['row']} px up the deep layer, the Moon's {MOON['row']} (0.015 px of deep per px of ground)"),
    ("moon", round(t_for_centre("deep", MOON["row"]), 2), MOON["x"], None),
    ("sensor-station", 11, 340, "1. Picket Line: the overrun picket, a sister sensor station breaking up on far"),
    ("picket-platform", 2.5, 360, None),
    ("wreck-a", 5, 90, None),
    ("picket-truss", 8.5, 240, None),
    ("gun-mount", 8.5, 150, "on the truss"),
    ("picket-half", 11.5, 100, None),
    ("wreck-b", 14.5, 410, None),
    ("picket-platform", 17.5, 120, None),
    ("gun-mount", 20, 390, None),
    ("wreck-c", 23.5, 340, "the lifeboat tow drifts down the left half from t = 22: the right side only"),
    ("picket-ring", 24.5, 240, None),
    ("picket-half", 27, 390, None),
    ("picket-truss", 29.5, 240, None),
    ("gun-mount", 33, 380, "2. Escort Screen: the last picket platform, then open space"),
    ("picket-platform", 37, 350, None),
    ("wreck-b", 42, 120, None),
    ("wreck-c", 48, 400, None),
    ("carrier-far", None, CARRIER["x"], "2-3: the carrier ahead (its path: the glow peeks over the top edge from about t = 40, "
                                       "more of it shows as Lancer closes in, it pulls away over the top in the heavy peak)"),
    ("fleet-line", 254, 166, "5. Aftermath: the second fleet's contacts enter at the top at about t = 236, mid-screen at 254, "
                             "the line streaming up and left from the Moon's left limb (x 328)"),
    ("ichor-a", 241.5, 300, "ichor clouds from the carcass, all entering at the top after the arena's end (no pop at an early kill)"),
    ("ichor-b", 247, 110, None),
    ("ichor-c", 253, 360, None),
    ("ichor-a", 259, 150, None),
    ("ichor-b", 265, 330, None),
]

# The carrier's path: its centre's screen height (px above the bottom edge) at these times.
CARRIER_T = 70.0                 # the placement the path's offsets refer to
_TOP = SCREEN + CARRIER["size"][1] / 2


def carrier_screen():
    """(t, screen y of the centre): hidden above the top until 36 s, its tail and glow peeking in at
    44, 250 px of it on screen at 87, then away over the top at 40 px/s (the path at 116.5 px/s
    against the far layer's 76.5) by 93.4, and held there to the end."""
    out = [(36.0, _TOP + 4), (44.0, _TOP - 70), (70.0, _TOP - 160), (87.0, _TOP - 250)]
    up = 40.0
    t_exit = 87.0 + (250 + 6) / up
    out.append((round(t_exit, 2), _TOP + 6))
    for t in (100.0, ARENA_END, OUTRO_END + 5):
        out.append((t, _TOP + 6))
    return out


def carrier_path():
    centre = scroll_at(CARRIER_T) * FACTORS["far"] + MID
    path = []
    for t, y in carrier_screen():
        dy = y - (centre - scroll_at(t) * FACTORS["far"])
        path.append([t, 0, round(dy, 1)])
    return path


def placements():
    out = []
    for piece, t, x, comment in PLACED:
        entry = {"piece": piece, "t": t, "x": x}
        if piece == "carrier-far":
            entry["t"] = CARRIER_T
            entry["path"] = carrier_path()
        out.append((entry, comment))
    return out


def block():
    """The backdrop block as the data file holds it."""
    tile_sets = {}
    for name, (layer, height, drift, _) in TILE_SETS.items():
        spec = {"layer": layer, "height": height}
        if drift:
            spec["drift"] = drift
        tile_sets[name] = spec
    pieces = {}
    for name, (layer, (w, h), extra, mid, _) in PIECES.items():
        spec = {"layer": layer, "size": [w, h], **extra}
        if mid:
            spec["mid_size"] = True
        pieces[name] = spec
    return {"scroll_factors": dict(FACTORS), "ramp": RAMP, "haze_colour": HAZE_COLOUR,
            "atmosphere": ATMOSPHERE, "tile_sets": tile_sets, "pieces": pieces,
            "placed": [e for e, _ in placements()]}


def flow(d):
    """A YAML flow mapping in the data files' style."""
    def value(v):
        if isinstance(v, dict):
            return flow(v)
        if isinstance(v, list):
            return "[" + ", ".join(value(i) for i in v) + "]"
        if isinstance(v, bool):
            return "true" if v else "false"
        if isinstance(v, float):
            return f"{v:.1f}" if v == int(v) and abs(v) < 10 else f"{v:g}"
        return str(v)
    return "{" + ", ".join(f"{k}: {value(v)}" for k, v in d.items()) + "}"


def write_proposal():
    b = block()
    lines = [
        "# Level 07 backdrop PROPOSAL (tools/art/backdrop_l07.py --proposal; M4 part G batch, round 25).",
        "# For the main agent to merge into data.yaml: `backdrop` replaces the level's block (which takes",
        "# Level 03's images, `images: level-03`), and the sections take the `tiles` below.",
        "#",
        "# sections[i].tiles (by section, in order):",
    ]
    for s, tiles in zip(SECTIONS, SECTION_TILES):
        lines.append(f"#   {s['name']}: [{', '.join(tiles)}]")
    lines += [
        "#",
        "# The carcass in the aftermath is the boss's own sprite (LevelRenderer draws it on the play plane,",
        "# drifting after the chained death): no backdrop piece; section 5's `ground` note can say so.",
        "# The second fleet's line is on `far` (section 5's notes say `deep`): the deep layer moves 21 px",
        "# in the whole aftermath, so only a far piece can bring it in after the kill.",
        "",
        "backdrop:",
        "  # Presentation only. As Level 01's (see its data file): Earth-Moon L1 beyond the overrun picket.",
        "  # Art: tools/art/backdrop_l07.py. The deep layer scrolls at 0.015 (art direction: 0.05-0.2): at L1",
        "  # Earth and the Moon are far enough that both stay on screen from the start to the aftermath, and",
        "  # an early kill's jump to the arena's end moves them at most ~20 px. Motion budget: the low-air",
        "  # banks' drift and the carrier's path in sections 2-3, the fleet's glitter in the aftermath.",
        "  scroll_factors: " + flow(b["scroll_factors"]),
        f"  ramp: {RAMP}",
        f"  haze_colour: {HAZE_COLOUR}  # olive grey: the escort's spores",
        "  atmosphere:",
    ]
    notes = {"clear": "the carrier alone", "light": "section 1 and the aftermath: no banks (no air at L1)",
             "medium": "the escort's spore haze", "heavy": "the carrier's spore wake"}
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
    for entry, comment in placements():
        if comment:
            lines.append(f"    # {comment}")
        lines.append(f"    - {flow(entry)}")
    PROPOSAL.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"proposal: {PROPOSAL.relative_to(ROOT)}")


def level_backdrop():
    """The level's backdrop block once it is Level 07's own, otherwise the proposal's."""
    own = DATA.get("backdrop") or {}
    if own and "images" not in own:
        return own, "data.yaml"
    if not PROPOSAL.exists():
        write_proposal()
    return yaml.safe_load(PROPOSAL.read_text(encoding="utf-8"))["backdrop"], PROPOSAL.name


# --------------------------------------------------------------------------- helpers

def box_down(arr, f):
    """A float RGBA render (alpha 0..1) reduced by ``f``: colour weighted by coverage."""
    h, w = arr.shape[0] // f, arr.shape[1] // f
    a = arr[:h * f, :w * f].reshape(h, f, w, f, 4)
    alpha = a[..., 3].mean(axis=(1, 3))
    rgb = (a[..., :3] * a[..., 3:4]).mean(axis=(1, 3)) / np.maximum(alpha, 1e-6)[..., None]
    return np.dstack([rgb, alpha])


def scorched(img, seed, amount=0.45, base=0.74):
    return b3.scorched(img, seed, amount, base)


def receded(img):
    return b3.receded(img)


def growth_scene(blobs, flip=1):
    """Teal Vrell growth (Level 02's): glossy violet bulbs with teal seams, at (x, y, r, z) model px."""
    def scene(p):
        items = [(sd_ellipsoid(p, (x * flip, y, z), (r, r * 0.8, r * 0.55)), em.V_BODY) for x, y, r, z in blobs]
        items += [(sd_sphere(p, (x * flip, y, z + r * 0.45), r * 0.3), em.V_GLOW) for x, y, r, z in blobs[::2]]
        return union(*items, k=5.0)
    return scene


def growth(w, h, blobs, colors=24):
    """The growth rendered over a (w, h) piece (2x, crisp), transparent elsewhere."""
    mats = em.vrell_scheme_mats("skitter", "a", 1.0)     # Level 02's at 1.4: dimmer under the play plane
    hi = sdf.render(growth_scene(blobs), mats, (w * 2, h * 2), float(w), z_top=60.0, steps=110)
    return sprite.make_sprite(hi, 2, colors, crisp=60)


def blob_cluster(rng, n, cx, cy, spread, r):
    return [(cx + rng.normal(0, spread), cy + rng.normal(0, spread), rng.uniform(*r), rng.uniform(2, 6))
            for _ in range(n)]


def opaque_box(img):
    return img.getchannel("A").getbbox()


# --------------------------------------------------------------------------- deep

def stars(w, h):
    """Open space: the kit's starfield, sparser and dimmer than over Earth (the deep recedes), with a
    faint dust band; opaque, wraps in both directions (stars and their crosses wrap modulo)."""
    a = l01.stars(w, h, 707, density=0.0016)
    band = raster.fbm(w, h, 160, 709, octaves=4, period=True)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64)
    a[..., :3] += (np.array([18, 20, 34], float) * np.clip(band - 0.45, 0, 1)[..., None] * 1.6)
    a[..., 3] = 255
    img = l01.rgba(a[..., :3], a[..., 3])
    return artkit.quantize_set([img], 16)[0]


def earth(w, h):
    """Earth's whole disc, seen from L1: Level 01's day-side texture on a sphere, lit from the top
    left, the night side to the lower right with sparse city lights, the thin blue rim, hazed for the
    distance; rendered at 4x, the disc's edge 1-bit, the rim glow and terminator ordered-dithered."""
    f = 4
    r = EARTH["r"]
    tex = np.array(l01.earth_tile(480, 1024).convert("RGBA")).astype(np.float64)
    th, tw = tex.shape[:2]
    yy, xx = (np.mgrid[0:h * f, 0:w * f] + 0.5) / f
    dx, dy = (xx - w / 2) / r, (yy - h / 2) / r
    d2 = dx * dx + dy * dy
    inside = d2 <= 1
    nz = np.sqrt(np.clip(1 - d2, 0, 1))
    tilt = np.radians(18)
    ny, nx = -dy, dx
    ny2 = ny * np.cos(tilt) - nz * np.sin(tilt)            # the axis tipped towards the viewer
    nz2 = ny * np.sin(tilt) + nz * np.cos(tilt)
    lat = np.arcsin(np.clip(ny2, -1, 1))
    lon = np.arctan2(nx, nz2)
    # the tile once round the globe, its one seam at the far side (lon = 180): Level 01's tile does
    # not repeat cleanly side to side at every lattice, so no seam may face the viewer
    u = np.clip((lon / (2 * np.pi) + 0.5) * (tw - 1), 0, tw - 1)
    v = np.clip(EARTH_BAND + (0.5 - lat / np.pi) * tw / 2, 0, th - 1)    # the tile's aspect kept
    rgb = tex[v.astype(int), u.astype(int), :3]
    light = np.array([-0.62, 0.55, 0.56])
    light /= np.linalg.norm(light)
    lam = np.clip(nx * light[0] + ny * light[1] + nz * light[2], -1, 1)
    day = np.clip(lam * 1.15 + 0.05, 0, 1)
    shade = 0.12 + 0.95 * day
    col = rgb * shade[..., None]
    night = np.clip(0.1 - lam, 0, 0.3) / 0.3                # city lights on the night side
    rng = np.random.default_rng(717)
    lights = np.zeros_like(day)
    land = rgb[..., 1] > rgb[..., 2] * 0.9
    for _ in range(900):
        x, y = rng.integers(0, w * f), rng.integers(0, h * f)
        lights[max(0, y - 2):y + 2, max(0, x - 2):x + 2] = rng.uniform(0.4, 1.0)
    col += (np.array([214, 196, 140]) * (lights * night * land)[..., None]) * 0.55
    fres = np.clip(1 - nz, 0, 1) ** 3                       # the atmosphere brightening at the limb
    col = col * (1 - 0.5 * fres[..., None]) + np.array(SEA[4], float) * (0.5 * fres * (0.3 + day))[..., None]
    arr = np.zeros((h * f, w * f, 4))
    arr[..., :3] = col
    arr[..., 3] = inside
    dist = (np.sqrt(d2) - 1) * r                              # px outside the limb
    glow = np.exp(-np.clip(dist, 0, None) / 3.2) * (~inside) * (dist < 7) * np.clip(0.25 + 0.75 * (0.5 + 0.5 * (
        -dx * 0.7 - dy * 0.7) / np.maximum(np.sqrt(d2), 1e-6)), 0, 1)
    l01.over(arr, np.array(SEA[4], float) * 0.85, glow * 0.75)
    small = box_down(arr, f)
    disc = box_down(np.dstack([np.zeros(inside.shape + (3,)), inside.astype(float)]), f)[..., 3]
    small[..., 3] = np.where(disc >= 0.5, 1.0, np.where(disc > 0, np.maximum(small[..., 3], 0) * 0.6, small[..., 3]))
    rgb = small[..., :3] * (1 - 0.18) + np.array(SEA[2], float) * 0.18     # hazed for the distance
    out = np.dstack([rgb, small[..., 3]])
    return l01.finish_dithered(out, colors=COLOURS, alpha_levels=8, rgb_amp=6.0)


def moon(w, h):
    """The Moon ahead at 4x: lit from the top left, dark maria (low-frequency noise), a few ringed
    craters with a bright rim and a shadowed floor; hazed slightly for the distance."""
    f = 4
    r = MOON["r"]
    yy, xx = (np.mgrid[0:h * f, 0:w * f] + 0.5) / f
    dx, dy = (xx - w / 2) / r, (yy - h / 2) / r
    d2 = dx * dx + dy * dy
    nz = np.sqrt(np.clip(1 - d2, 0, 1))
    light = np.array([-0.6, 0.62, 0.5])
    light /= np.linalg.norm(light)
    lam = np.clip(dx * light[0] - dy * light[1] + nz * light[2], 0, 1)
    maria = raster.fbm(w * f, h * f, 26 * f, 731, octaves=3, period=False)
    fine = raster.fbm(w * f, h * f, 4 * f, 733, octaves=3, period=False)
    albedo = 0.86 - 0.36 * np.clip((maria - 0.5) * 3.0, 0, 1) + 0.1 * (fine - 0.5)
    rng = np.random.default_rng(737)
    relief = np.zeros_like(lam)
    for _ in range(16):
        cx, cy = rng.uniform(-0.8, 0.8), rng.uniform(-0.8, 0.8)
        if cx * cx + cy * cy > 0.7:
            continue
        cr = rng.uniform(0.05, 0.16)
        d = np.hypot(dx - cx, dy - cy) / cr
        relief += np.clip(1 - np.abs(d - 1) * 4, 0, 1) * 0.35 * np.sign(-(dx - cx) - (dy - cy) + 1e-9)
        relief -= (d < 1) * 0.12
    shade = np.clip(lam * (albedo + relief) * 1.05 + 0.04, 0, 1)
    rgb = np.array([168, 172, 186], float) * shade[..., None]
    arr = np.dstack([rgb / 255, (d2 <= 1).astype(float)])
    img = sprite.to_image(sprite.downsample(arr, f), 0.5)
    return artkit.quantize_set([haze(img, SEA[2], 0.18)], 16)[0]


# --------------------------------------------------------------------------- far

def kit_part(scene, size, colors=40):
    return station.render_part((scene, size), l01.KIT_PAL, colors=colors)


def torn_truss_scene(length, width, gaps, seed):
    """The kit's horizontal truss torn through at ``gaps`` (x, half-width): jagged cuts in the model."""
    scene, _ = station.truss_h_part(length, width)

    def torn(p):
        d, m = scene(p)
        for i, (gx, half) in enumerate(gaps):
            jag = half + 5 * np.sin(p[:, 1] * 0.7 + seed + i) + 3 * np.sin(p[:, 1] * 1.9 + 2 * i)
            d = np.maximum(d, jag - np.abs(p[:, 0] - gx))
        return d, m
    return torn


def sensor_station(w, h):
    """A sister sensor station breaking up: its sensor drum and dish array on the upper truss, the
    lower truss with the second drum torn away from it and sliding aside, kit debris in the gap;
    built at full scale, reduced to the far layer's 0.45 and hazed as the north arm is."""
    k = l01.kit()

    def build(low, high, fw, fh):
        cx = fw // 2 - 30
        top_end, gap = int(fh * 0.5), 70
        l01.column(low, k["truss_v"], cx, 60, top_end)
        l01.put(low, l01.truss_h(170, 22), cx, 200)
        l01.put(low, k["radiator"], cx - 112, 330)
        l01.put(high, k["dock"], cx, 52)
        l01.put(high, k["drum"], cx, 300)
        l01.put(high, k["dish"], cx + 80, 200)
        l01.put(high, k["dish"], cx - 78, 196)
        # the lower part, torn off and drifting right: the truss, a drum and the lower array
        lx = cx + 64
        l01.column(low, k["truss_v"], lx, top_end + gap, fh - 70)
        l01.put(low, l01.truss_h(130, 22), lx, fh - 170)
        l01.put(high, k["drum_s"], lx, fh - 110)
        l01.put(high, k["dish"], lx + 62, fh - 172)
        l01.put(high, k["cargo"], lx - 56, fh - 230)
        rng = np.random.default_rng(741)
        for part in ("cargo", "drum_s", "turret"):          # debris in the gap
            l01.put(high, k[part], cx + rng.uniform(-40, 90), top_end + rng.uniform(10, gap - 10))
        stub = kit_part(torn_truss_scene(60, 22, [(26, 6)], 3), (60, 22))
        l01.put(low, stub, cx + 20, top_end + gap * 0.5)
    img = b3.far_piece(build, w, h, colours=24)
    return check_inside(img, "sensor-station")


def picket_ring(w, h):
    """The overrun picket ahead: Level 03's defence-ring band of two trusses torn through right of the
    middle, a gun platform each side, the third broken and overgrown with teal growth, the sensor
    pylon below it; at the far scale and hazed."""
    k = l01.kit()
    platform = l01.platform(140, 140)
    damaged = l01.part(l01.platform_model(True), (140, 140), factor=4, colors=40)
    rng = np.random.default_rng(751)
    overgrown = damaged.copy()
    overgrown.alpha_composite(growth(140, 140, blob_cluster(rng, 7, 10, -6, 18, (8, 15))))

    def build(low, high, fw, fh):
        band = (fh * 0.42, fh * 0.42 + 84)
        gap_x = fw * 0.62
        for y in band:
            truss = kit_part(torn_truss_scene(fw + 40, 26, [(gap_x - fw / 2, 36)], 5), (fw + 40, 26))
            l01.put(low, truss, fw / 2, y)
        for x in range(60, fw, 150):
            if abs(x - gap_x) > 60:
                l01.column(low, k["truss_v"], x, band[0], band[1])
        l01.put(high, platform, fw * 0.16, sum(band) / 2)
        l01.put(high, damaged, fw * 0.42, sum(band) / 2)
        l01.put(high, overgrown, fw * 0.84, sum(band) / 2)
        sx = fw * 0.3
        l01.column(low, k["truss_v"], sx, band[1], fh - 80)
        l01.put(high, k["drum"], sx, fh - 74)
        l01.put(high, k["dish"], sx + 36, fh - 48)
    img = b3.far_piece(build, w, h, colours=28)
    return check_inside(img, "picket-ring", sides=False)


def carrier_far(w, h, n):
    """The Brood Carrier ahead, at the far scale: the chosen round-04 model (enemy_models, its r04
    colours), turned in the model for each heading (0 nose up, 1 nose down) and lit from the top
    left, then darkened and hazed into a silhouette whose lime sacs still glow, inside a soft
    teal-lime halo (stepped translucency, ordered-dithered). One palette for both."""
    scene, _ = em.brood_carrier(0.0, 1.0, 0.0, 0.0)
    mats = em.brood_carrier_scheme_mats(1.0, 0.0, 0.0)
    hull_len = 300.0
    extent = w / (hull_len / 4.9)                  # model units across the image (the hull 4.9 long)
    frames = []
    halo_rgb = np.array([70, 150, 110], float)
    for i in range(n):
        angle = np.pi * 2 * i / n
        turned = (lambda a: (lambda p: scene(rotate_z(p, a + np.pi))))(angle)   # the model faces down; heading 0 is up
        hi, factor = artkit.render_hi(turned, mats, (w, h), extent)
        body = artkit.native(hi, factor, crisp=60)
        a = np.array(body).astype(np.float64)
        rgb, alpha = a[..., :3], a[..., 3] / 255
        glow = np.clip((rgb[..., 1] - rgb[..., 2] * 0.9 - 40) / 120, 0, 1) * (rgb[..., 0] > 80)   # the lime sacs
        dark = rgb * 0.38 + np.array([6, 18, 20]) * 0.62
        rgb = dark * (1 - glow[..., None]) + rgb * glow[..., None] * 0.85
        mask = alpha > 0
        yy, xx = np.mgrid[0:h, 0:w].astype(np.float64)
        spread = np.array(Image.fromarray((mask * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(11)),
                          dtype=np.float64) / 255
        border = np.minimum.reduce([xx, yy, w - 1 - xx, h - 1 - yy])
        halo = np.clip(spread * 1.6, 0, 1) * np.clip((border - 2) / 14, 0, 1)
        halo = artkit.ordered_dither(halo, 5) * 0.5
        halo[mask] = 0
        out = np.zeros((h, w, 4))
        out[..., :3] = halo_rgb
        out[..., 3] = halo
        out[mask, :3] = rgb[mask]
        out[mask, 3] = 1
        img = l01.rgba(out[..., :3], out[..., 3] * 255)
        frames.append(haze(img, (14, 22, 34), 0.28))
    frames = artkit.quantize_set(frames, 24)
    for img in frames:
        check_inside(img, "carrier-far")
    return frames


def fleet_line(w, h, n):
    """The second fleet at long range (round 25's capture found 42 single dim pixels too faint): a
    long line of ~90 contacts along a slightly bowed diagonal from the upper left down to the Moon's
    left limb, so it reads as streaming out from beyond the Moon (and passes Earth's disc, lower left,
    only in the aftermath's last seconds as the far layer carries it down); each a bright 1-2 px core in a soft
    halo, the far end smaller and denser (farther off), every seventh or so a lead ship with a lens
    cross as it glints. The contacts glitter over the frames (each its
    own phase, brightness stepped); pale blue-white and a few Vrell violet."""
    rng = np.random.default_rng(761)
    count = 90

    def along(s):
        # the line's centre at s (0 upper left .. 1 lower right, at the Moon's limb); image rows down
        x = 6 + s * (w - 12)
        y = h * 0.14 + s * h * 0.72 - np.sin(s * np.pi) * 7
        return x, y
    pts = []
    for k in range(count):
        s = ((k + rng.uniform(-0.35, 0.35)) / (count - 1)) ** 0.85      # denser towards the far end
        s = float(np.clip(s, 0, 1))
        x, y = along(s)
        y = float(np.clip(y + rng.normal(0, 2.2), 4, h - 5))
        near = 1 - 0.55 * s                                                # the near end larger and brighter
        lead = rng.random() < 0.14 * near + 0.02
        violet = rng.random() < 0.18
        col = np.array([214, 178, 255] if violet else [222, 236, 255], float)
        pts.append((x, y, col, lead, near, rng.uniform(0, 2 * np.pi), rng.uniform(0.75, 1.0)))
    yy, xx = np.mgrid[0:h, 0:w].astype(float)
    frames = []
    for f in range(n):
        acc = np.zeros((h, w, 3))
        alpha = np.zeros((h, w))
        for x, y, col, lead, near, phase, base in pts:
            tw = 0.5 + 0.5 * np.cos(phase + 2 * np.pi * f / n)
            b = base * (0.6 + 0.4 * tw)
            b = np.round(b * 4) / 4
            r2 = (xx - x) ** 2 + (yy - y) ** 2
            core = 0.6 + 0.9 * near                                         # core radius, px
            halo = np.exp(-r2 / (2 * (0.8 + 0.9 * near) ** 2)) * 0.4 * b
            spot = np.clip(core - np.sqrt(r2) + 0.5, 0, 1) * b
            a = np.maximum(spot, halo)
            if lead and tw > 0.5:
                arm = (np.abs(yy - round(y)) < 0.5) & (np.abs(xx - x) < 3 + 3 * near)
                arm |= (np.abs(xx - round(x)) < 0.5) & (np.abs(yy - y) < 2 + 2 * near)
                a = np.maximum(a, arm * 0.7 * b)
            sel = a > alpha
            acc[sel] = (col * a[..., None])[sel]
            alpha = np.maximum(alpha, a)
        rgb = np.where(alpha[..., None] > 0, acc / np.maximum(alpha, 1e-6)[..., None], 0)
        a8 = np.round(np.clip(alpha, 0, 1) * 4) / 4 * 255                   # translucency stepped
        frames.append(l01.rgba(np.clip(rgb, 0, 255), a8))
    return artkit.quantize_set(frames, 16)


# --------------------------------------------------------------------------- ground

def torn_dome(model, centre, radius):
    """A platform model with its gun dome torn open: the dome's top carved away, a dark hole in it,
    the twin barrels bent out of the gap."""
    cx, cy = centre

    def scene(p):
        d, m = model(p)
        d = np.maximum(d, -sd_sphere(p, (cx + 5, cy - 4, 18), radius))
        d2, m2 = union((d, m), (sd_cylinder_z(p, (cx + 4, cy - 3, 6), radius * 0.62, 1.5), DARK),
                       (sd_capsule(p, (cx - 3, cy + 6, 12), (cx - 14, cy + 22, 9), 1.8), DARK),
                       (sd_capsule(p, (cx + 3, cy + 8, 12), (cx + 18, cy + 18, 15), 1.8), DARK))
        return d2, m2
    return scene


def picket_platform(w, h):
    """An overrun picket platform: Level 01's damaged defence platform with its gun dome torn open
    and its barrels bent, teal growth spreading over the deck from the breaches; scorched, receded."""
    model = torn_dome(l01.platform_model(True), (0, 0), 13)
    img = l01.part(model, (w, h), factor=4, colors=48)
    rng = np.random.default_rng(771)
    blobs = blob_cluster(rng, 6, -26, -20, 12, (6, 12)) + blob_cluster(rng, 4, 46, -6, 8, (5, 9))
    img.alpha_composite(growth(w, h, blobs))
    out = artkit.quantize_set([receded(scorched(img, 773))], COLOURS)[0]
    return check_inside(out, "picket-platform")


def picket_half(w, h):
    """Half a picket platform (Level 03's platform-half, turned in the model), overgrown."""
    img = b3.platform_half(150, 150, -1, np.radians(52), -8, 7)
    full = l01.blank(150, 150)
    full.alpha_composite(img, ((150 - img.width) // 2, (150 - img.height) // 2))
    rng = np.random.default_rng(781)
    full.alpha_composite(growth(150, 150, blob_cluster(rng, 7, -10, 4, 14, (6, 12))))
    out = artkit.quantize_set([b3.centred(receded(scorched(full, 783, 0.3, 0.86)), w, h)], COLOURS)[0]
    return check_inside(out, "picket-half")


def gun_mount(w, h):
    """A gun mount torn open: the turret's base ring and housing ripped apart, one barrel bent, the
    other snapped, a tendril of growth in the breach; scorched."""
    def scene(p):
        q = rotate_z(p, np.radians(-20))
        d, m = union((sd_cylinder_z(q, (0, 0, 0), 20, 3), DARK),
                     (sd_box(q, (0, 0, 3), (24, 6, 2), 1), HULL),
                     (sd_sphere(q, (0, 0, 4), 14), HULL),
                     (sd_box(q, (0, -6, 15), (6, 3, 1.5), 0.6), ACCENT),
                     (sd_sphere(q, (12, 14, 4), 1.8), RED))
        d = np.maximum(d, -sd_sphere(q, (5, 4, 14), 10))
        d, m = union((d, m), (sd_cylinder_z(q, (5, 4, 4), 7, 1.5), DARK),
                     (sd_capsule(q, (-4, 8, 9), (-14, 26, 7), 2.2), DARK),
                     (sd_capsule(q, (5, 10, 9), (8, 18, 10), 2.2), DARK))
        return d, m
    img = l01.part(scene, (w, h), factor=8, colors=40)
    rng = np.random.default_rng(791)
    img.alpha_composite(growth(w, h, blob_cluster(rng, 3, 4, 2, 4, (3, 5))))
    out = artkit.quantize_set([receded(scorched(img, 793, 0.5, 0.68))], 24)[0]
    return check_inside(out, "gun-mount")


def picket_truss(w, h):
    """The picket's truss across the lane, torn through in two places (jagged in the model), growth
    clinging at one break; spans the play field (its rails end at the screen edges)."""
    torn = kit_part(torn_truss_scene(w, 26, [(-70, 22), (96, 14)], 9), (w, h))
    rng = np.random.default_rng(801)
    torn.alpha_composite(growth(w, h, blob_cluster(rng, 3, -46, 2, 5, (4, 7))))
    out = artkit.quantize_set([receded(scorched(torn, 803, 0.4, 0.8))], 24)[0]
    return check_inside(out, "picket-truss", sides=False)


# --------------------------------------------------------------------------- low-air

def ichor_cloud(w, h, seed):
    """A cloud of the carrier's ichor: a lumpy fbm mass in dark teal shaded towards the top-left
    light, faint lime droplets in it, translucent (at most ~60 %, stepped with ordered dithering) and
    fading out well inside the piece."""
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64)
    n = raster.fbm(w, h, 28, seed, octaves=5, period=False)
    ell = np.hypot((xx - w / 2) / (w * 0.42), (yy - h / 2) / (h * 0.4))
    dens = np.clip((n * 1.2 - ell * 0.9 - 0.12) * 2.6, 0, 1)
    dens *= np.clip(np.minimum.reduce([xx, yy, w - 1 - xx, h - 1 - yy]) / 10, 0, 1)
    gy, gx = np.gradient(dens)
    lit = np.clip(0.5 - (gx + gy) * 6 + dens * 0.3, 0, 1)
    rgb = ICHOR * (1 - lit[..., None]) + ICHOR_LIT * lit[..., None]
    rng = np.random.default_rng(seed + 1)
    for _ in range(int(w * h / 900)):
        x, y = rng.integers(4, w - 4), rng.integers(4, h - 4)
        if dens[y, x] > 0.4:
            rgb[y, x] = LIME_SPECK
    alpha = artkit.ordered_dither(dens, 6) * 0.62
    img = l01.rgba(rgb, alpha * 255)
    out = artkit.quantize_set([img], 12)[0]
    return check_inside(out, f"ichor {seed}")


# --------------------------------------------------------------------------- generators

def check_inside(img, name, sides=True):
    """A piece keeps clear of its image borders (nothing cut at them); ``sides=False`` for a piece as
    wide as the play field, whose left and right edges are the screen's."""
    a = np.array(img)[..., 3] > 0
    if a[0].any() or a[-1].any() or (sides and (a[:, 0].any() or a[:, -1].any())):
        raise ValueError(f"{name}: opaque pixels on the image border (cut)")
    return img


GENERATORS = {
    "stars": stars, "spore-haze": b3.spore_haze, "spore-banks": b3.spore_banks,
    "ice-streaks": b2.frost_streaks, "spore-streaks": b3.spore_streaks,
    "earth": earth, "moon": moon, "sensor-station": sensor_station, "picket-ring": picket_ring,
    "carrier-far": carrier_far, "fleet-line": fleet_line,
    "picket-platform": picket_platform, "picket-half": picket_half, "gun-mount": gun_mount,
    "picket-truss": picket_truss,
    "wreck-a": lambda w, h: b3.wreck(w, h, 851, 3, np.radians(-24)),
    "wreck-b": lambda w, h: b3.wreck(w, h, 863, 4, np.radians(40)),
    "wreck-c": lambda w, h: b3.wreck(w, h, 877, 3, np.radians(-70)),
    "ichor-a": lambda w, h: ichor_cloud(w, h, 911), "ichor-b": lambda w, h: ichor_cloud(w, h, 923),
    "ichor-c": lambda w, h: ichor_cloud(w, h, 937),
}


def jobs(backdrop, wanted):
    out = [(name, (W, spec["height"]), None) for name, spec in backdrop["tile_sets"].items()]
    out += [(name, tuple(spec["size"]), spec.get("frames") or spec.get("headings"))
            for name, spec in backdrop["pieces"].items()]
    return [job for job in out if not wanted or job[0] in wanted]


def render(job):
    name, (w, h), count = job
    fn = GENERATORS[name]
    images = fn(w, h, count) if count else [fn(w, h)]
    for img in images:
        if img.size != (w, h):
            raise ValueError(f"{name}: rendered {img.size}, the data file says {(w, h)}")
    return images


def write(job, images):
    name, _, count = job
    if count:
        for old in OUT.glob(f"{name}_*.png"):
            old.unlink()
        for i, img in enumerate(images):
            artkit.save_png(img, OUT / f"{name}_{i}.png", SOURCE)
    else:
        artkit.save_png(images[0], OUT / f"{name}.png", SOURCE)
    print(f"{name}: {len(images)} image(s) {images[0].size[0]}x{images[0].size[1]}, "
          f"{artkit.colour_count(images)} colours")


def load(name, i=None):
    return Image.open(OUT / (f"{name}_{i}.png" if i is not None else f"{name}.png")).convert("RGBA")


# --------------------------------------------------------------------------- the game's draw

def java_round(v):
    return math.floor(v + 0.5)


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


def on_screen(backdrop, placed, t):
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
    """The tile set's image rows for layer positions ``rows`` (bottom row first counted from 0)."""
    img = cached(name)
    hgt = img.shape[0]
    out = img[[hgt - 1 - (int(r) % hgt) for r in rows]]
    return np.roll(out, shift, axis=1) if shift else out


def over(dst, src, alpha=1.0):
    a = src[..., 3:4] / 255 * alpha
    dst[..., :3] = src[..., :3] * a + dst[..., :3] * (1 - a)


def add(dst, src, alpha):
    dst[..., :3] = np.minimum(255, dst[..., :3] + src[..., :3] * src[..., 3:4] / 255 * alpha)


def draw_layer_tiles(backdrop, frame, layer, scroll, additive=None):
    rows = scroll + (SCREEN - 1 - np.arange(SCREEN))           # the layer position of every screen row
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


def draw_pieces(backdrop, frame, layer, t, additive=None):
    for placed in backdrop["placed"]:
        spec = backdrop["pieces"][placed["piece"]]
        if spec["layer"] != layer or not on_screen(backdrop, placed, t):
            continue
        x, y, w, h = piece_rect(backdrop, placed, t)
        img = cached(placed["piece"], image_index(backdrop, placed, t))
        top = SCREEN - y - h
        y0, y1 = max(0, top), min(SCREEN, top + h)
        x0, x1 = max(0, x), min(W, x + w)
        sub = frame[y0:y1, x0:x1]
        src = img[y0 - top:y1 - top, x0 - x:x1 - x]
        if additive is None:
            over(sub, src)
        else:
            add(sub, src, additive)


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


def composite(backdrop, t):
    """The play field at t as Backdrop draws it (no units): deep, far, haze, ground, low-air
    (tiles, banks, pieces), high-air additive at 40 %."""
    frame = np.zeros((SCREEN, W, 4))
    frame[..., 3] = 255
    look_a, look_b, weight = looks_at(backdrop, t)
    for layer in ("deep", "far"):
        scroll = java_round(scroll_at(t) * backdrop["scroll_factors"][layer])
        draw_layer_tiles(backdrop, frame, layer, scroll)
        draw_pieces(backdrop, frame, layer, t)
    veil = look_a["haze"] + (look_b["haze"] - look_a["haze"]) * weight
    hz = np.array([int(backdrop["haze_colour"][i:i + 2], 16) for i in (0, 2, 4)], float)
    frame[..., :3] = frame[..., :3] * (1 - veil) + hz * veil
    for layer in ("ground", "low-air"):
        scroll = java_round(scroll_at(t) * backdrop["scroll_factors"][layer])
        draw_layer_tiles(backdrop, frame, layer, scroll)
        if layer == "low-air":
            draw_banks(backdrop, frame, look_a, look_b, weight, "banks", layer, t)
        draw_pieces(backdrop, frame, layer, t)
    scroll = java_round(scroll_at(t) * backdrop["scroll_factors"]["high-air"])
    draw_layer_tiles(backdrop, frame, "high-air", scroll, additive=0.4)
    draw_pieces(backdrop, frame, "high-air", t, additive=0.4)
    draw_banks(backdrop, frame, look_a, look_b, weight, "wisps", "high-air", t, additive=0.4)
    return Image.fromarray(np.clip(frame[..., :3], 0, 255).astype(np.uint8), "RGB")


SECTION_OF = SECTION_TILES


# --------------------------------------------------------------------------- checks

def wrap_score(a, columns=False):
    """BackdropSeamsTest's wrap score, on luminance premultiplied by alpha over every pixel (so the
    translucent banks and the sparse streaks count); ``columns`` scores the left-right wrap."""
    if columns:
        a = np.transpose(a, (1, 0, 2))
    lum = (a[..., 0] * 0.299 + a[..., 1] * 0.587 + a[..., 2] * 0.114) * a[..., 3] / 255
    h = a.shape[0]

    def change(i, j):
        d = np.sort(np.abs(lum[i] - lum[j]))
        return d[:int(len(d) * 0.9)].mean()
    # each wrap pair against the pairs of the same 4-row phase within 16 rows of it (the ordered
    # dither's Bayer rows change by different amounts, and a bank's texture varies over 40 rows)
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


def check_edges(backdrop):
    """No piece shows an edge on screen: wherever an image edge crosses the screen it is transparent."""
    bad = []
    for placed in backdrop["placed"]:
        spec = backdrop["pieces"][placed["piece"]]
        for i in range(spec.get("frames") or spec.get("headings") or 1):
            a = cached(placed["piece"], i if (spec.get("frames") or spec.get("headings")) else None)[..., 3] > 0
            x, _, w, _ = piece_rect(backdrop, placed, placed["t"])
            cols = slice(max(0, -x), min(w, W - x))
            if a[0, cols].any() or a[-1, cols].any():
                bad.append(f"{placed['piece']} at t {placed['t']}: its top or bottom row is opaque")
            if (0 < x < W and a[:, 0].any()) or (0 < x + w < W and a[:, -1].any()):
                bad.append(f"{placed['piece']} at t {placed['t']}: a side column is opaque on screen")
    return bad


def check_screens(backdrop):
    """BackdropCheck.checkScreens: at most 3 mid-size pieces and 2 strongly animated elements on
    screen at every step; every piece is on screen at some time; and in the arena and at the arena's
    end (where an early kill makes the clock jump) only the deep layer has anything on screen."""
    problems = []
    seen = [False] * len(backdrop["placed"])
    st = stretches()
    for step in range(int(OUTRO_END * 60) + 1):
        t = step / 60
        mid, moving = [], set()
        for i, placed in enumerate(backdrop["placed"]):
            if on_screen(backdrop, placed, t):
                seen[i] = True
                spec = backdrop["pieces"][placed["piece"]]
                if spec.get("mid_size"):
                    mid.append(placed["piece"])
                if spec.get("frames") or placed.get("path"):
                    moving.add(placed["piece"])
                if STARTS[ARENA] + 6 <= t <= ARENA_END and spec["layer"] != "deep":
                    problems.append(f"t={t:.2f}: {placed['piece']} on {spec['layer']} on screen in the arena")
        atms = [next((s for s in st if t < s[1]), st[-1])[2]]
        atms += [x for k in range(1, len(st)) if abs(t - st[k][0]) < RAMP / 2 for x in (st[k - 1][2], st[k][2])]
        if any(backdrop["tile_sets"].get(backdrop["atmosphere"][x].get("banks"), {}).get("drift") for x in atms):
            moving.add("atmosphere banks")
        if len(mid) > 3:
            problems.append(f"t={t:.2f}: {len(mid)} mid-size pieces: {mid}")
        if len(moving) > 2:
            problems.append(f"t={t:.2f}: {len(moving)} animated elements: {sorted(moving)}")
    problems += [f"{p['piece']} at t {p['t']} is never on screen" for p, s in zip(backdrop["placed"], seen) if not s]
    return sorted(set(problems))[:12]


def run_checks(backdrop):
    problems = check_tiles(backdrop) + check_edges(backdrop) + check_screens(backdrop)
    for p in problems:
        print("PROBLEM:", p)
    if not problems:
        print("checks: ok")
    return problems


def atlas_area(backdrop):
    area = sum(W * s["height"] for s in backdrop["tile_sets"].values())
    area += sum(s["size"][0] * s["size"][1] * (s.get("frames") or s.get("headings") or 1)
                for s in backdrop["pieces"].values())
    print(f"atlas area: {area:,} px = {area / 2048 ** 2:.2f} pages of 2048x2048")
    return area


# --------------------------------------------------------------------------- review

TIMES = [(1, "1 PICKET LINE: EARTH BEHIND, MOON AHEAD"), (8.5, "1 TORN TRUSS, GUN MOUNT"),
         (11, "1 SENSOR STATION BREAKING UP"), (24.5, "1 THE PICKET'S RING (LIFEBOAT LEFT)"),
         (37, "2 LAST PLATFORM, SPORE HAZE"), (50, "2 THE CARRIER'S GLOW AT THE TOP"),
         (80, "3 ITS SILHOUETTE"), (91, "3 HEAVY WAKE: IT PULLS AWAY"), (101, "4 ARENA: CLEAR"),
         (235.5, "5 AFTERMATH STARTS"), (250, "5 ICHOR CLOUDS"), (254, "5 THE SECOND FLEET"), (262, "5 ITS LINE OUT FROM THE MOON")]


def review(backdrop):
    items = []
    for name, spec in list(backdrop["tile_sets"].items()) + list(backdrop["pieces"].items()):
        count = spec.get("frames") or spec.get("headings") if isinstance(spec, dict) and "size" in spec else None
        imgs = [load(name, i) for i in range(count)] if count else [load(name)]
        label = f"{name} {spec['layer']} {imgs[0].width}x{imgs[0].height} {artkit.colour_count(imgs)} col"
        for k, im in enumerate(imgs[:2] if spec.get("headings") else imgs[:1]):
            scale = min(1.0, 220 / max(im.size))
            im = im.resize((max(1, int(im.width * scale)), max(1, int(im.height * scale))), Image.NEAREST)
            items.append((label + (f" [{k}]" if count else ""), im, spec["layer"] == "high-air"))
    frames = [load("fleet-line", i) for i in range(backdrop["pieces"]["fleet-line"]["frames"])]
    items.append(("fleet-line frames 2x", sprite.enlarge(b1.strip(frames, 2), 2), False))
    width, x, y, row_h = 1520, 16, 44, 0
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
    sheet = raster.sheet(width, comp_y + n_rows * (270 + 30) + 20, "LEVEL 07 BACKDROP - FINAL",
                         "PRODUCTION ART, M4 PART G BATCH - R25; COMPOSITES AS THE GAME DRAWS THEM, 1/2 SCALE")
    for name, im, add_, px, py in rows:
        plate = Image.new("RGBA", im.size, artkit.PLATE)
        if add_:
            plate = artkit.add_light(plate, im)
        else:
            plate.alpha_composite(im)
        sheet.alpha_composite(plate, (px, py + 14))
        raster.draw_text(sheet, px, py, name.upper(), raster.LABEL)
    for k, (t, label) in enumerate(TIMES):
        px, py = 16 + (k % per_row) * cw, comp_y + (k // per_row) * 300
        sheet.alpha_composite(composite(backdrop, t).convert("RGBA").resize((240, 270), Image.BOX), (px, py + 14))
        raster.draw_text(sheet, px, py, f"T={t:g} {label}"[:40], raster.LABEL)
    REVIEW.parent.mkdir(parents=True, exist_ok=True)
    sheet.convert("RGB").save(REVIEW, optimize=True)
    print(f"review: {REVIEW.relative_to(ROOT)}")


def strip_frames(backdrop, times, path):
    """Full-size composites at ``times`` side by side (for a look at 1x)."""
    shots = [composite(backdrop, t) for t in times]
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
    if "--strip" in argv:      # --strip t1,t2,... out.png: full-size composites side by side
        k = argv.index("--strip")
        strip_frames(backdrop, [float(a) for a in argv[k + 1].split(",")], argv[k + 2])
        return
    known = set(GENERATORS)
    wanted = {a for a in argv if not a.startswith("--")}
    missing = (backdrop["tile_sets"].keys() | backdrop["pieces"].keys()) - known
    if missing or wanted - known:
        raise SystemExit(f"no generator for {sorted(missing | (wanted - known))}")
    if "--check" in argv:
        sys.exit(1 if run_checks(backdrop) else 0)
    if "--review" not in argv:
        OUT.mkdir(parents=True, exist_ok=True)
        todo = sorted(jobs(backdrop, wanted), key=lambda j: j[0] != "carrier-far")   # the slowest first
        with ProcessPoolExecutor() as pool:
            for job, images in zip(todo, pool.map(render, todo)):
                write(job, images)
        _cache.clear()
        run_checks(backdrop)
    atlas_area(backdrop)
    review(backdrop)


if __name__ == "__main__":
    main(sys.argv[1:])
