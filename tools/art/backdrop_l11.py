#!/usr/bin/env python3
"""Production art: the Level 11 backdrop, the grey North Atlantic of convoy Atlas-Seven
(design/campaign/act-2-homefront/level-11-atlantic-convoy; M5 part E batch, concept round 33).

Straight to production from the chosen ocean scene (user decision E9 = a): scene-ocean-r10-a
(tools/concept/scenes_r10.py on scenes_r09.py / scenes_r08.py, imported unchanged: the sea's
colour stops, its swell and chop, the tone field, the whitecaps, the mist banks and wisps). The
Level 11 props of tools/art/l11_props.py (the burning freighter, the reef growths and the reef
root) come from its ``pieces()``; this script writes them into the backdrop folder.

**The sea in two layers.** The concept animates one height field; a backdrop is still images, so
the sea is split where the concept's swell and chop part: the long swell (its two long components,
the slow tone field of deeper and paler swaths) is the opaque `deep` tile set `swell`, scrolling
at 0.85 of the ground; the short chop (its three short components and the sparse whitecaps) is the
translucent `ground` tile set `chop` over it, at 1.0. The swell slides under the chop at 15 % of
the scroll (21 px/s at 140), so the sea moves while it streams past, with no animated tile. Both
tile sets are the same in every section, so the sea has no seams; the sections differ by their
pieces and atmosphere. The chop marks every pixel (alpha >= 3) so flyers' shadows fall on the whole
sea (the shadow stencil). Every wave component makes whole cycles across the 480 x 960 tile.

**Sections** (the README's Layout): 1 Convoy Muster, open sea under a light sea mist; 2 Jelly
Fields, Vrell spawn slicks on the surface (olive film, spawn beads); 3 Reef Line, seven lines of
Vrell reef growths breaking the surface over their under-water shelves, with roots fading into the
deep, under medium mist banks; the lines leave **channels** for the convoy's stations (hulls at
x 130, 240, 350, 56 px wide): growths only at x < 94, 166-204, 276-314 and > 386; 4 Tiamat
Approach, an oil slick, flotsam and a capsized lifeboat, the abandoned burning freighter at the
right edge with its smoke column trailing down the edge, then Platform Tiamat entering at the top;
5 the arena: the scroll halts with **Platform Tiamat's centre at (240, 78)** (px from the play
field's left and top edges; 110 until round 33 raised it 32 px so the Kraken's head surfaces in open
water off the deck's south edge, user 2026-10-09), its deck x 136-344, y 2-154, and the four 120 px
lanes open below it;
6 Open Water: the platform scrolls out under a lighter overcast (the clear look's veil).

**Platform Tiamat** (`platform-tiamat`, 256 x 208, centred on the platform's centre): round 07's
deck model (render/boss_models.platform, unchanged, 100 px per unit: deck 208 x 152, edges x
+-104, y +-76), the registration harbour_kraken.py's grip overlay and pivots assume, rendered at
the 4x quality bar, scorched, with its shadow on the water and a 4-frame foam loop lapping at its
edges. Its placement (``platform_t()``): its centre lies the simulation's halt scroll (Sortie.haltScroll:
scroll_at at the arena's start plus the ease into the halt, 68.83 px at 140 px/s) + 540 - 78 px up the
ground layer, which the open-water section's time reaches by the data's scrollAt (speed 0 in the arena):
t = end of the arena + 260.83 px / 140 px/s = 162.863 with the level's timeline. The review composites
use the simulation's scroll too (``sim_scroll``: the ease flown over the arena's first second, the whole
of it after the arena's end).

**Motion budget** (at most two strongly animated elements on screen): no tile set drifts and the
banks hold still on their layer (the parallax moves them); animated pieces are the reef growths'
collar loops (each reef line uses at most two growth ids and the lines are 7 s apart, so one line
is on screen at a time), the freighter's fire flicker with its smoke's billow (section 4, nothing
else animated near them) and the platform's foam (the arena). The sea itself moves by its parallax.

Outputs (assets/backdrop/level-11/, one PNG per tile set and piece of the backdrop block, named by
its id; frames as <id>_<n>.png):
  swell                deep: the long Atlantic swell and its slow tone field (opaque)
  chop                 ground: the short chop and sparse whitecaps (translucent, every pixel marked)
  sea-mist, mist-banks low-air banks: light ~15 %, medium ~22 % (no drift)
  wisps                high-air (additive): thin wisps
  spawn-slick-a/b      ground, section 2: Vrell spawn slicks (olive film, spawn beads)
  reef-shelf-a/b       ground, section 3: a reef line's mass under the surface (as wide as the field)
  reef-growth-a/b/c_0..3, reef-root-a   ground, section 3: tools/art/l11_props.py's pieces()
                       (the root drawn here through the water: the backdrop has no `sub` layer)
  oil-slick            ground, section 4: the freighter's oil (translucent film, faint sheen)
  flotsam-a/b          ground, section 4: planks, a barrel, a life ring, crates (static collars)
  lifeboat             ground, section 4: a capsized orange lifeboat
  burning-freighter_0..3   ground, section 4: l11_props.py's freighter (fire flicker)
  freighter-smoke_0..7     ground, section 4: its smoke column billowing (8 frames, no travel)
  platform-tiamat_0..3     ground, sections 4-6: Platform Tiamat, the foam loop at its edges
  design/campaign/.../level-11-atlantic-convoy/concept/backdrop-final-r33-a.png/.gif   review:
                       the tiles and pieces, composites per section as the game draws them (the
                       convoy at its stations, at the halt the ships in their lanes and the
                       Kraken's grip overlay and sub-pass head on the platform), the GIF the
                       approach, the halt (held 3 s on the real clock) and the scroll resuming
  design/campaign/.../level-11-atlantic-convoy/concept/backdrop-proposal.yaml   the backdrop
                       block and the sections' tiles for the level data (--proposal)

Reads the level's data.yaml when it exists (its sections, scroll speed and backdrop block),
otherwise the README's values. Run: python3 tools/art/backdrop_l11.py [--proposal | --review |
--check | --strip t,.. out.png] [id ...]   (all: about 2 min)
"""
import math
import sys
from concurrent.futures import ProcessPoolExecutor

import numpy as np
import yaml
from PIL import Image, ImageFilter

import artkit
from artkit import DESIGN, ROOT, TAU, raster

import convoy_ships as ships  # noqa: E402  (tools/art: the freighter's projection)
import l11_props  # noqa: E402  (tools/art: the backdrop pieces it renders)
import water_fx as wfx  # noqa: E402  (tools/art: foam, waterline rendering, collars)
import parallax_r02 as r02  # noqa: E402  (concept script, unchanged: periodic noise)
import scenes_r06 as s6  # noqa: E402  (concept script, unchanged: banks, wisps)
import scenes_r08 as s8  # noqa: E402  (concept script, unchanged: foam colour, collars, deep tint)
import scenes_r09 as s9  # noqa: E402  (concept script, unchanged: the ocean's colour stops, foam blend)
from render import boss_models as bm  # noqa: E402  (round 07's platform model)
from render import sdf  # noqa: E402
from render.sdf import (Material, rotate_x, rotate_z, sd_box, sd_capsule, sd_ellipsoid, union)  # noqa: E402

SCRIPT = "backdrop_l11.py"
BATCH = "M5 part E batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
LEVEL_DIR = DESIGN / "campaign" / "act-2-homefront" / "level-11-atlantic-convoy"
CONCEPT = LEVEL_DIR / "concept"
PROPOSAL = CONCEPT / "backdrop-proposal.yaml"
OUT = ROOT / "assets" / "backdrop" / "level-11"
REVIEW = CONCEPT / "backdrop-final-r33-a.png"
W, SCREEN = 480, 540
MID = SCREEN / 2
TILE_H = 960
COLOURS = 32

FACTORS = {"deep": 0.85, "far": 0.9, "ground": 1.0, "low-air": 1.3, "high-air": 2.0}
HAZE_COLOUR = "70808a"            # the overcast's grey light on the water
ATMOSPHERE = {
    "clear": {"wisps": "wisps", "haze": 0.08},                          # the arena and open water: a lighter overcast
    "light": {"banks": "sea-mist", "wisps": "wisps", "haze": 0.03},     # sea mist ~15 %
    "medium": {"banks": "mist-banks", "wisps": "wisps", "haze": 0.06},  # mist banks ~22 % between the reefs
}
RAMP = 4

STATIONS = [(130, 380, "cargo"), (240, 350, "cargo"), (350, 380, "cargo"), (240, 480, "frigate")]   # README, The convoy
HULL_HALF = 28                          # half a cargo hull's width (56 px)
CHANNEL_MARGIN = 8
LANE = 120
LANE_SHIPS = [(60, 450), (180, 450), (420, 450)]                # at the halt: lanes 1, 2, 4
CLEAR_SHIPS = [(60, 450), (64, 300), (420, 450)]      # after the halt until the platform has passed (E2b's call)
CLEAR_SECONDS = 4.5
PLATFORM_AT = (240.0, 78.0)             # Tiamat's centre at the halt (round 33: 110 -> 78) (px from the left, below the top): round 07's
PLATFORM = (256, 208)
DECK = (104, 76)                        # the deck's half extents (round 07: 208 x 152)

# --------------------------------------------------------------------------- the level's timeline

DEFAULT_SECTIONS = [
    {"name": "Convoy Muster", "end": 20, "atmosphere": "light"},
    {"name": "Jelly Fields", "end": 65, "atmosphere": "light"},
    {"name": "Reef Line", "end": 110, "atmosphere": "medium"},
    {"name": "Tiamat Approach", "end": 160, "atmosphere": "light"},
    {"name": "Harbour Kraken", "end": 161, "atmosphere": "clear", "speed": 0, "arena": True},
    {"name": "Open Water", "end": 176, "atmosphere": "clear", "peak": {"from": 170, "to": 176, "atmosphere": "light"}},
]


def level_data():
    path = LEVEL_DIR / "data.yaml"
    data = yaml.safe_load(path.read_text(encoding="utf-8")) if path.exists() else {}
    return {"scroll_speed": data.get("scroll_speed", 140), "sections": data.get("sections") or DEFAULT_SECTIONS,
            "backdrop": data.get("backdrop"), "own": path.exists()}


DATA = level_data()
SECTIONS = DATA["sections"]
SPEED = float(DATA["scroll_speed"])
STARTS = [0.0] + [float(s["end"]) for s in SECTIONS[:-1]]
END = float(SECTIONS[-1]["end"])
ARENA = next(i for i, s in enumerate(SECTIONS) if s.get("arena"))
HALT_T = STARTS[ARENA]                 # the arena section starts: the scroll halts (speed 0)
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
    """The first t at which the ground has scrolled ``scroll`` (a halted section is skipped)."""
    if scroll < 0:
        return scroll / speed(0)
    for i, s in enumerate(SECTIONS):
        span = (s["end"] - STARTS[i]) * speed(i)
        if speed(i) > 0 and (scroll < span or i == len(SECTIONS) - 1):
            return STARTS[i] + scroll / speed(i)
        scroll -= span
    raise AssertionError


def ease_distance(v):
    """Sortie.easeDistance: how far the ground scrolls while it eases from v px/s to the arena's halt
    over its 1 s ramp (60 steps, step j at v (n - 1 - j) / n; 68.83 px at 140 px/s)."""
    n = 60
    return sum(v * (n - 1 - j) / n / 60 for j in range(n - 1))


EASE = ease_distance(speed(ARENA - 1))
HALT_SCROLL = scroll_at(HALT_T) + EASE  # Sortie.haltScroll: 22,400 + 68.83 = 22,468.83 with the README's timeline


def sim_scroll(t):
    """The ground's scroll as the simulation runs it at level time t: the data's scroll_at plus the
    part of the ease into the halt flown by then (the whole ease from the arena's end on, when the
    level clock runs again after the boss)."""
    if t < HALT_T:
        return scroll_at(t)
    steps = min(59, int((t - HALT_T) * 60))
    v = speed(ARENA - 1)
    return scroll_at(t) + sum(v * (59 - j) / 60 / 60 for j in range(steps))


def t_for_centre(layer, centre):
    """The placement time that puts a piece's centre at ``centre`` px on its layer."""
    return t_at((centre - MID) / FACTORS[layer])


def platform_centre():
    """Tiamat's centre on the ground layer (px up from the screen's bottom at the level start): at
    the halt it lies PLATFORM_AT[1] below the top edge."""
    return HALT_SCROLL + SCREEN - PLATFORM_AT[1]


def platform_t():
    return round(t_for_centre("ground", platform_centre()), 3)


# --------------------------------------------------------------------------- the layout

TILE_SETS = {   # id -> (layer, height, drift, note)
    "swell": ("deep", TILE_H, None, "the long Atlantic swell and its slow tone field (opaque), every section; "
                                    "the chop slides over it (deep at 0.85)"),
    "chop": ("ground", TILE_H, None, "the short chop and sparse whitecaps (translucent, alpha >= 3 everywhere: "
                                     "the shadow stencil), every section"),
    "sea-mist": ("low-air", TILE_H, None, "light: thin sea-mist banks, ~15 % cover, still on their layer"),
    "mist-banks": ("low-air", TILE_H, None, "medium: mist banks between the reefs, ~22 % cover"),
    "wisps": ("high-air", TILE_H, None, "thin high wisps (additive, under 16 % alpha)"),
}
SECTION_TILES = [["swell", "chop"]] * len(SECTIONS)

REEF_IDS = ["reef-growth-a", "reef-growth-b", "reef-growth-c"]
PIECES = {      # id -> (layer, (w, h), extra spec, mid-size, note)
    "spawn-slick-a": ("ground", (168, 112), {}, False, "a Vrell spawn slick: olive film and spawn beads (section 2)"),
    "spawn-slick-b": ("ground", (128, 168), {}, False, None),
    "reef-shelf-a": ("ground", (480, 128), {}, False, "a reef line's mass under the surface, seen through the water"),
    "reef-shelf-b": ("ground", (480, 112), {}, False, None),
    "reef-growth-a": ("ground", (48, 40), {"frames": 4, "fps": 5}, False,
                      "tools/art/l11_props.py: Vrell reef growths breaking the surface, collar loops"),
    "reef-growth-b": ("ground", (64, 48), {"frames": 4, "fps": 5}, False, None),
    "reef-growth-c": ("ground", (40, 56), {"frames": 4, "fps": 5}, False, None),
    "reef-root-a": ("ground", (64, 48), {}, False, "l11_props.py's root fading into the deep, drawn through the water here"),
    "oil-slick": ("ground", (224, 160), {}, False, "the freighter's oil: a dark film with a faint sheen (section 4)"),
    "flotsam-a": ("ground", (56, 44), {}, False, "planks, a barrel and a life ring afloat"),
    "flotsam-b": ("ground", (52, 48), {}, False, "crates, planks and a fender afloat"),
    "lifeboat": ("ground", (32, 64), {}, False, "a capsized orange lifeboat"),
    "burning-freighter": ("ground", (72, 136), {"frames": 4, "fps": 8}, True,
                          "l11_props.py: the abandoned freighter, listing, its deck fires flickering"),
    "freighter-smoke": ("ground", (88, 232), {"frames": 8, "fps": 6}, False,
                        "its smoke column trailing down the right edge, billowing in place (no travel)"),
    "platform-tiamat": ("ground", PLATFORM, {"frames": 4, "fps": 5}, True,
                        "Platform Tiamat: round 07's deck (208 x 152) at its centre, its shadow, foam lapping at its edges"),
}

FREIGHTER_X = 432
FREIGHTER_T = 128.0
REEF_LINES = [   # t of the line's centre, shelf id, [(growth id, x)], roots [(x, dy)]; at most two growth ids a line
    (67.0, "reef-shelf-a", [("reef-growth-b", 30), ("reef-growth-a", 70), ("reef-growth-a", 185), ("reef-growth-a", 410),
                            ("reef-growth-b", 442)], [(300, -30)]),
    (74.0, "reef-shelf-b", [("reef-growth-c", 20), ("reef-growth-a", 60), ("reef-growth-c", 296), ("reef-growth-a", 420),
                            ("reef-growth-c", 458)], [(80, 34)]),
    (81.0, "reef-shelf-a", [("reef-growth-b", 36), ("reef-growth-c", 74), ("reef-growth-c", 185), ("reef-growth-c", 296),
                            ("reef-growth-b", 444)], [(410, 36)]),
    (88.0, "reef-shelf-b", [("reef-growth-a", 24), ("reef-growth-b", 60), ("reef-growth-a", 296), ("reef-growth-b", 420),
                            ("reef-growth-a", 462)], [(186, 26)]),
    (96.0, "reef-shelf-a", [("reef-growth-c", 28), ("reef-growth-b", 64), ("reef-growth-c", 185), ("reef-growth-b", 432),
                            ("reef-growth-c", 468)], [(296, -30)]),
    (102.5, "reef-shelf-b", [("reef-growth-a", 40), ("reef-growth-c", 76), ("reef-growth-a", 185), ("reef-growth-c", 406),
                             ("reef-growth-a", 450)], [(186, -26)]),
    (108.0, "reef-shelf-a", [("reef-growth-b", 30), ("reef-growth-a", 70), ("reef-growth-a", 296), ("reef-growth-b", 446),
                             ("reef-growth-a", 412)], []),
]
REEF_DY = (8, -6, 4, -10, 6, -3)


def at_dy(t, dy):
    """A placement time ``dy`` px further up the ground than the one of ``t`` (positive dy: higher)."""
    return round(t_at(scroll_at(t) + dy), 2)


def placements():
    """(entry, comment) in drawing order per layer."""
    out = []
    slicks = [(26.0, "spawn-slick-a", 70), (31.5, "spawn-slick-b", 410), (38.0, "spawn-slick-a", 300),
              (44.5, "spawn-slick-b", 96), (51.0, "spawn-slick-a", 196), (58.0, "spawn-slick-b", 380)]
    for k, (t, piece, x) in enumerate(slicks):
        out.append(({"piece": piece, "t": t, "x": x},
                    "2. Jelly Fields: Vrell spawn slicks (flat on the surface, the convoy steams over them)" if k == 0 else None))
    for k, (t, shelf, growths, roots) in enumerate(REEF_LINES):
        note = ("3. Reef Line: seven lines, 7 s apart (one on screen at a time, at most two growth ids each); growths "
                "only outside the convoy's channels (x 102-158, 212-268, 322-378)") if k == 0 else None
        out.append(({"piece": shelf, "t": t, "x": 240}, note))
        for x, dy in roots:
            out.append(({"piece": "reef-root-a", "t": at_dy(t, dy), "x": x}, None))
        for j, (g, x) in enumerate(growths):
            out.append(({"piece": g, "t": at_dy(t, REEF_DY[(j + k) % len(REEF_DY)]), "x": x}, None))
    out.append(({"piece": "oil-slick", "t": at_dy(FREIGHTER_T, -150), "x": 404},
                "4. Tiamat Approach: the burning freighter at the right edge, its oil and smoke, flotsam; containers "
                "and the last raft nest are the level's ground targets"))
    flot = [(114.0, "flotsam-a", 50), (119.5, "lifeboat", 296), (124.0, "flotsam-b", 186), (134.5, "flotsam-a", 60),
            (139.0, "flotsam-b", 436), (145.5, "lifeboat", 185), (150.0, "flotsam-a", 430), (153.0, "lifeboat", 52)]
    for t, piece, x in flot:
        out.append(({"piece": piece, "t": t, "x": x}, None))
    out.append(({"piece": "freighter-smoke", "t": at_dy(FREIGHTER_T, -smoke_offset()[1]), "x": FREIGHTER_X + smoke_offset()[0]},
                None))
    out.append(({"piece": "burning-freighter", "t": FREIGHTER_T, "x": FREIGHTER_X}, None))
    out.append(({"piece": "platform-tiamat", "t": platform_t(), "x": PLATFORM_AT[0]},
                f"4-6. Platform Tiamat: at the halt its centre is at ({PLATFORM_AT[0]:g}, {PLATFORM_AT[1]:g}) (ground "
                f"position {platform_centre():,.2f} px, the simulation's halt scroll + {SCREEN - PLATFORM_AT[1]:g}; t = the open-water speed's time to scroll "
                f"{platform_centre() - MID - scroll_at(HALT_T):.2f} px past the data's scroll at the arena)"))
    return out


# --------------------------------------------------------------------------- the block

def block():
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
    pc = platform_centre()
    lines = [
        "# Level 11 backdrop PROPOSAL (tools/art/backdrop_l11.py --proposal; M5 part E batch, round 33).",
        "# For the level-data step to merge into data.yaml: `backdrop` is the level's block, and every section",
        "# takes the `tiles` below (the sea is the same two tile sets throughout: no seams).",
        "#",
        "# sections[i].tiles (by section, in order):",
    ]
    for s, tiles in zip(SECTIONS, SECTION_TILES):
        lines.append(f"#   {s['name']}: [{', '.join(tiles)}]")
    lines += [
        "#",
        "# Built round this timeline (re-run --proposal and the images after the data moves a section end):",
        "#   " + ", ".join(f"{s['name']} to {s['end']:g}" + (" (speed 0, arena)" if s.get("arena") else "")
                          for s in SECTIONS) + f"; {SPEED:g} px/s.",
        f"# Platform Tiamat (piece platform-tiamat, 256 x 208, centred on the platform's centre; deck 208 x 152,",
        f"#   edges x +-104, y +-76, round 07's model): its centre lies {pc:,.0f} px up the ground layer (from the",
        f"#   screen's bottom at the level start), x {PLATFORM_AT[0]:g}. At the halt (ground scroll {HALT_SCROLL:,.2f} = scrollAt(t {HALT_T:g})",
        f"#   + the ease into the halt, {EASE:.2f} px: Sortie.haltScroll)",
        f"#   that is screen ({PLATFORM_AT[0]:g}, {PLATFORM_AT[1]:g}) from the play field's top left: the deck spans x 136-344,",
        f"#   y {PLATFORM_AT[1] - DECK[1]:g}-{PLATFORM_AT[1] + DECK[1]:g}; the lanes run below y {PLATFORM_AT[1] + DECK[1]:g}; the Kraken's head centre (216 px below, off the",
        f"#   deck's south edge) is at (240, {PLATFORM_AT[1] + 216:g}).",
        f"#   It scrolls in at the top over the last {(PLATFORM[1] / 2 + PLATFORM_AT[1]) / SPEED:.1f} s before the halt; at the halt the piece's top edge",
        f"#   is {PLATFORM_AT[1] - PLATFORM[1] / 2:g} px below the top. The Kraken's anchor uses the same ground position",
        f"#   (the halt scroll + {SCREEN - PLATFORM_AT[1]:g}). The placement t lies in the open-water section, by the data's scrollAt (speed 0",
        "#   in the arena): end(arena) + (ground position - 270 - scrollAt(arena start)) / its speed.",
        "# The convoy's channels: no piece that breaks the surface lies under a station's hull (x 130, 240,",
        "#   350 +- 28 px, the README's stations); slicks, oil and the reef shelves lie flat or under water.",
        "# After the halt the platform (x 136-344) scrolls down through Mbeki's and Ruyter's stations (x 240)",
        "#   in about 3.9 s: the convoy's return to its stations (step E2b) should wait until it has passed.",
        "# Motion budget (BackdropCheck, at most 2 animated ids on screen): the reef growths (one line on screen",
        "#   at a time, at most two ids a line), the freighter and its smoke (section 4), the platform's foam;",
        "#   no tile set drifts. The sea moves by its parallax (deep 0.85 under the ground's chop).",
        "",
        "backdrop:",
        "  # Presentation only: the grey North Atlantic of convoy Atlas-Seven (tools/art/backdrop_l11.py, straight",
        "  # from scene-ocean-r10-a, E9 = a). The sea is two layers: the long swell on `deep` (0.85, opaque) and the",
        "  # short chop on `ground` (1.0, translucent), so the swell slides under the chop while the sea streams by.",
        "  scroll_factors: " + flow(b["scroll_factors"]),
        f"  ramp: {RAMP}",
        f"  haze_colour: {HAZE_COLOUR}  # the overcast's grey light on the water (over the swell only)",
        "  atmosphere:",
    ]
    notes = {"clear": "the arena and open water: a lighter overcast on the swell",
             "light": "sea mist ~15 %, thin wisps", "medium": "section 3: mist banks ~22 % between the reefs"}
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
    PROPOSAL.parent.mkdir(parents=True, exist_ok=True)
    PROPOSAL.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"proposal: {PROPOSAL.relative_to(ROOT)}")


def level_backdrop():
    """The level's backdrop block once its data has one, otherwise the proposal's."""
    own = DATA.get("backdrop") or {}
    if own and "images" not in own:
        return own, "data.yaml"
    write_proposal()
    return yaml.safe_load(PROPOSAL.read_text(encoding="utf-8"))["backdrop"], PROPOSAL.name


# =========================================================================== the sea

OCEAN_STOPS = s9.OCEAN_STOPS           # round 09/10's colour stops (calmer top tone)
TONES = 7                              # scenes_r10: tones=7, light_gain=1.0
WATER = np.array(s8.OCEAN_WATER, float)
# (n, m, amplitude): n whole cycles across the 480 px width, m along the 960 px tile; the concept's
# components (scenes_r10: 160/72 deg, 96/52, 60/104, 40/34, 26/84) rounded to whole cycles
SWELL = [(1, 6, 1.0), (3, 8, 0.7)]
CHOP = [(-2, 16, 0.45), (10, 13, 0.28), (2, 37, 0.16)]
WARP_GAIN = 2.2

_SEA = {}


def sea_fields():
    if not _SEA:
        rng = np.random.default_rng(1101)
        phases = rng.uniform(0, TAU, len(SWELL) + len(CHOP))
        warp = [r02.periodic_fbm(W, TILE_H, 160, 1108 + i, octaves=2).astype(np.float64) for i in range(2)]
        yy, xx = np.mgrid[0:TILE_H, 0:W].astype(np.float64)

        def height(comps, ph):
            h = np.zeros((TILE_H, W))
            for i, ((n, m, a), p) in enumerate(zip(comps, ph)):
                h += a * np.sin(TAU * n * xx / W + TAU * m * yy / TILE_H + WARP_GAIN * (warp[i % 2] - 0.5) + p)
            return h
        _SEA["swell"] = height(SWELL, phases[:len(SWELL)])
        _SEA["chop"] = height(CHOP, phases[len(SWELL):])
        _SEA["tone"] = r02.periodic_fbm(W, TILE_H, 160, 1150, octaves=3).astype(np.float64)
        _SEA["capn"] = r02.periodic_fbm(W, TILE_H, 24, 1153, octaves=3).astype(np.float64)
    return _SEA


def light(h):
    """scenes_r08.Sea.light (gain 1.0) with periodic gradients (the tile wraps both ways)."""
    gx = (np.roll(h, -1, 1) - np.roll(h, 1, 1)) / 2
    gy = (np.roll(h, -1, 0) - np.roll(h, 1, 0)) / 2
    nx, ny = -gx * 3.0, -gy * 3.0
    inv = 1.0 / np.sqrt(nx * nx + ny * ny + 1)
    diff = (nx * -0.48 + ny * -0.56 + inv * 0.68) - 0.68
    return diff * 1.0 + 0.025 * h


def ramp(t):
    return raster.ramp(OCEAN_STOPS, np.clip(t, 0, 1))


def posterized(L):
    """scenes_r08.Sea.colour: the light through the colour stops in the concept's tones."""
    t = np.clip(0.5 + L, 0, 0.999)
    return ramp(np.clip(np.floor(t * TONES) / (TONES - 1), 0, 1))


def toned(body):
    """scenes_r09's slow tone field: deeper and paler swaths of sea."""
    tone = sea_fields()["tone"][..., None]
    return body * (0.84 + 0.28 * tone) + np.array((0, 6, 4)) * (tone - 0.5)


def swell_body():
    f = sea_fields()
    return toned(posterized(light(f["swell"])))


def swell(w, h):
    """deep: the swell alone lit and posterized as the concept lights its sea, under the slow tone
    field of deeper and paler swaths (scenes_r09.OceanScene)."""
    img = Image.fromarray(np.clip(swell_body(), 0, 255).astype(np.uint8), "RGB").convert("RGBA")
    return artkit.quantize_set([img], 40)[0]


CREST = np.array(OCEAN_STOPS[-1][1], float) * 1.05
TROUGH = np.array((4, 12, 20), float)
CAP = np.array((190, 204, 210), float)
LUM = np.array([0.299, 0.587, 0.114])


def chop(w, h):
    """ground: the chop as the difference the concept's whole sea makes over the swell alone (in
    register, the composite is the concept's posterized sea exactly; as the swell slides under it the
    same tone steps fall on other swell tones): toward a pale crest or a dark trough with stepped
    translucency, plus the sparse whitecaps; alpha >= 3 so the whole sea marks the shadow stencil."""
    f = sea_fields()
    base = swell_body()
    target = toned(posterized(light(f["swell"] + f["chop"])))
    lb, lt = base @ LUM, target @ LUM
    up = lt >= lb
    lc = np.where(up, CREST @ LUM, TROUGH @ LUM)
    a = np.clip((lt - lb) / np.where(up, np.maximum(lc - lb, 1), np.minimum(lc - lb, -1)), 0, 1)
    a = np.round(a * 8) / 8
    rgb = np.where(up[..., None], CREST, TROUGH)
    hgt = f["swell"] + f["chop"]
    caps = np.clip((f["chop"] - 0.55) * 2.0, 0, 1) * np.clip((hgt - 0.9) * 1.5, 0, 1) * (f["capn"] > 0.64)
    caps = np.round(caps * 3) / 3 * 0.7
    rgb = np.where((caps > 0)[..., None], CAP, rgb)
    alpha = np.where(caps > 0, np.maximum(caps, a), a)
    a8 = np.maximum(3, np.round(alpha * 255))
    return Image.fromarray(np.dstack([np.clip(rgb, 0, 255), a8]).astype(np.uint8), "RGBA")


MIST_GREYS = [(92, 104, 114), (120, 132, 140), (146, 156, 162), (166, 174, 180)]   # scenes_r09's mist


def sea_mist(w, h):
    """light: thin sea-mist banks (~15 %), stretched along the wind (scenes_r09's mist, lighter)."""
    return s8.bank_tex(TILE_H, [240, 120, 60], 1141, 0.72, 0.12, MIST_GREYS, max_alpha=110, stretch=2)


def mist_banks(w, h):
    """medium: the mist banks between the reefs (~22 %, scenes_r09's 120 alpha)."""
    return s8.bank_tex(TILE_H, [240, 120, 60], 1143, 0.67, 0.12, MIST_GREYS, max_alpha=120, stretch=2)


def wisps(w, h):
    """high-air: scenes_r10's thin wisps (under 16 % alpha)."""
    return s6.wisp_tex(TILE_H, [192, 96, 48], 1142, (206, 214, 220), thresh=0.54, gain=2.6, max_a=40, stretch=3)


# =========================================================================== section pieces

OLIVE = np.array([86, 98, 50], float)
OLIVE_DARK = np.array([44, 56, 34], float)
BEAD = np.array([170, 182, 104], float)
VIOLET = np.array([120, 70, 150], float)


def soft_blob(w, h, seed, cover, cell=16, octaves=4, edge=0.18):
    """A ragged organic shape: noise under an elliptical falloff, 0..1 (0 at the borders)."""
    n = raster.fbm(w, h, cell, seed, octaves=octaves, period=False)
    yy, xx = np.mgrid[0:h, 0:w]
    r = np.hypot((xx - w / 2 + 0.5) / (w / 2), (yy - h / 2 + 0.5) / (h / 2))
    fall = np.clip(1 - r, 0, 1)
    v = n * 0.55 + fall * 0.9 - cover
    return np.clip(v / edge, 0, 1) * (r < 0.98)


def spawn_slick(w, h, seed):
    """A Vrell spawn slick: a thin olive film (stepped translucency, thickest in its streaks) drawn
    out in strands along the current, a faint violet sheen at its rim and clusters of pale spawn
    beads; flat on the surface, low contrast, the sea's tones showing through."""
    film = soft_blob(w, h, seed, 0.72, cell=24)
    st = raster.fbm(w, h * 3, 8, seed + 3, octaves=3, period=False)
    streak = np.array(Image.fromarray((st * 255).astype(np.uint8)).resize((w, h), Image.BOX)) / 255
    rim = np.clip(film * (1 - film) * 4, 0, 1)
    thick = np.clip((streak - 0.42) * 3.5, 0, 1)
    rgb = OLIVE_DARK + (OLIVE - OLIVE_DARK) * thick[..., None]
    rgb = rgb + (VIOLET - rgb) * (rim[..., None] * 0.35)
    alpha = np.floor(film * (0.35 + 0.65 * thick) * 5 + 0.5) / 5 * 0.5
    arr = np.dstack([rgb, alpha * 255])
    rng = np.random.default_rng(seed + 9)
    for _ in range(int(w * h / 300)):
        x, y = rng.uniform(4, w - 4), rng.uniform(4, h - 4)
        if film[int(y), int(x)] < 0.6 or thick[int(y), int(x)] < 0.3:
            continue
        for k in range(rng.integers(2, 6)):
            bx, by = int(x + rng.normal(0, 2.2)), int(y + rng.normal(0, 2.2))
            if 0 < bx < w - 1 and 0 < by < h - 2:
                arr[by, bx, :3] = BEAD * rng.uniform(0.8, 1.05)
                arr[by, bx, 3] = 190
                arr[by + 1, bx, :3] = OLIVE_DARK * 0.7
                arr[by + 1, bx, 3] = max(arr[by + 1, bx, 3], 130)
    arr[0], arr[-1], arr[:, 0], arr[:, -1] = 0, 0, 0, 0
    return quantize(arr, 24)


def quantize(arr, colours):
    a = np.clip(arr, 0, 255).astype(np.uint8)
    a[a[..., 3] == 0] = 0
    return artkit.quantize_set([Image.fromarray(a, "RGBA")], colours)[0]


REEF_DARK = np.array([70, 44, 34], float)
REEF_VEIN = np.array([150, 60, 190], float)


def reef_mass(job):
    """One lump of a reef line's under-water mass: l11_props.reef_scene's roots (its `under` lobes,
    lifted toward the surface), rendered plain as the root piece is."""
    seed, size, lift = job
    scene = l11_props.reef_scene(seed, size, under=True)

    def lifted(p):
        q = p.copy()
        q[:, 2] -= lift
        return scene(q)
    hi, factor = artkit.render_hi(lifted, l11_props.reef_mats(), size, float(size[0]), z_top=24.0, steps=170)
    return artkit.native(hi, factor, crisp=60)


def reef_shelf(w, h, seed):
    """A reef line's mass under the surface: a ridge of reef lumps (l11_props' root lobes) along the
    line over a ragged dark band, seen through the water (toward the water colour, darker, blurred,
    stepped translucency), the shallowest lumps clearest; full width (its sides are the screen's)."""
    rng = np.random.default_rng(seed)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64)
    centre = h / 2 + 8 * np.sin(xx / w * TAU * 2 + rng.uniform(0, TAU)) + 5 * np.sin(xx / w * TAU * 5 + rng.uniform(0, TAU))
    n = raster.fbm(w, h, 32, seed, octaves=4, period=True)
    band = 1 - np.abs(yy - centre) / (h * 0.4)
    body = np.clip((band * 0.9 + (n - 0.5) * 0.8 - 0.15) / 0.3, 0, 1)
    rgb = REEF_DARK * (0.7 + 0.5 * n[..., None])
    base = np.dstack([rgb, body * 0.4 * 255])
    canvas = Image.fromarray(np.clip(base, 0, 255).astype(np.uint8), "RGBA")
    jobs_ = []
    x = rng.uniform(-30, 10)
    while x < w + 20:
        size = (int(rng.choice([80, 96, 112])), int(rng.choice([64, 72])))
        jobs_.append((int(rng.integers(2000, 9000)), size, float(rng.uniform(5, 10)), x,
                      float(centre[0, int(np.clip(x, 0, w - 1))] + rng.normal(0, 6)), float(rng.uniform(0.22, 0.42))))
        x += rng.uniform(48, 76)
    lumps = [reef_mass((j[0], j[1], j[2])) for j in jobs_]     # (already in a worker of main()'s pool)
    for (_, size, _, lx, ly, depth), lump in zip(jobs_, lumps):
        tinted = s8.deep_tint(lump, s8.OCEAN_WATER, depth, blur=0.0)
        canvas.alpha_composite(tinted, (int(round(lx - size[0] / 2)), int(round(ly - size[1] / 2))))
    canvas = s8.deep_tint(canvas, s8.OCEAN_WATER, 0.25, blur=1.0)
    arr = np.array(canvas).astype(np.float64)
    arr[..., 3] = np.floor(arr[..., 3] / 255 * 5 + 0.5) / 5 * 255
    arr[:3], arr[-3:] = 0, 0
    return quantize(arr, 28)


def sub_through(img, depth):
    """A `sub` piece drawn here through the water (the backdrop has no `sub` layer): the review's
    stand-in for the game's sub pass, as water_fx.sub_look, alpha stepped."""
    out = np.array(wfx.sub_look(img, depth)).astype(np.float64)
    out[..., 3] = np.floor(out[..., 3] / 255 * 5 + 0.5) / 5 * 255
    out[0], out[-1], out[:, 0], out[:, -1] = 0, 0, 0, 0
    return quantize(out, 24)


def oil_slick(w, h):
    """The freighter's oil: a dark brown-black film (stepped translucency) with smeared tongues
    along the current and a faint, low-saturation sheen at its thin edges."""
    film = soft_blob(w, h, 1201, 0.58, cell=32, edge=0.25)
    sm = raster.fbm(w * 3, h, 16, 1203, octaves=3, period=False)
    smear = np.array(Image.fromarray((sm * 255).astype(np.uint8)).resize((w, h), Image.BOX)) / 255
    thin = np.clip(film * (1 - film) * 4, 0, 1)
    rgb = np.array([16, 18, 20], float) * (0.8 + 0.5 * smear[..., None])
    hue = (smear * 3 + film * 2) % 1.0
    sheen = np.stack([0.5 + 0.5 * np.cos(TAU * (hue + k / 3)) for k in range(3)], -1) * 70 + 40
    rgb = rgb + (sheen - rgb) * (thin[..., None] * 0.45)
    alpha = np.floor(film * 4 + 0.5) / 4 * (0.48 + 0.18 * smear)
    arr = np.dstack([rgb, alpha * 255])
    arr[0], arr[-1], arr[:, 0], arr[:, -1] = 0, 0, 0, 0
    return quantize(arr, 24)


# --------------------------------------------------------------------------- flotsam (models)

WOOD, BARREL, RING_RED, RING_WHITE, CRATE, ORANGE, HULL_LO, FENDER = range(8)


def flotsam_mats():
    def grain(p, n):
        return 0.82 + 0.18 * np.sin(p[:, 0] * 2.6 + 1.3 * np.sin(p[:, 1] * 0.7))

    return [Material((0.42, 0.33, 0.22), metal=0.0, shininess=12, spec=0.12, pattern=grain),
            Material((0.22, 0.32, 0.46), metal=0.4, shininess=40, spec=0.4),
            Material((0.78, 0.22, 0.14), metal=0.1, shininess=30, spec=0.3),
            Material((0.86, 0.86, 0.82), metal=0.1, shininess=30, spec=0.3),
            Material((0.46, 0.40, 0.30), metal=0.0, shininess=14, spec=0.15, pattern=grain),
            Material((0.92, 0.42, 0.10), metal=0.15, shininess=40, spec=0.35),
            Material((0.30, 0.28, 0.26), metal=0.2, shininess=20, spec=0.2),
            Material((0.12, 0.12, 0.13), metal=0.0, shininess=10, spec=0.1)]


def sd_torus_z(p, c, R, r):
    q = p - np.array(c, float)
    return np.hypot(np.hypot(q[:, 0], q[:, 1]) - R, q[:, 2]) - r


def plank(p, c, length, width, ang, sink=0.6):
    q = rotate_z(p - np.array([c[0], c[1], 0.0]), ang)
    return sd_box(q, (0, 0, 0.6 - sink), (length / 2, width / 2, 0.6), 0.15)


def flotsam_a_scene(p):
    items = [(plank(p, (-10, 6), 26, 5, 0.35), WOOD), (plank(p, (4, -8), 22, 4, -0.5), WOOD),
             (plank(p, (12, 9), 16, 4, 1.25), WOOD)]
    barrel = sd_capsule(rotate_z(p, 0.9), (-6.5, 0, 0.6), (6.5, 0, 0.6), 4.2)
    items.append((barrel - 0.0, BARREL))
    ring = sd_torus_z(p, (-14, -10, 0.3), 5.0, 1.6)
    ang = np.arctan2(p[:, 1] + 10, p[:, 0] + 14)
    items.append((ring, np.where((np.floor(ang / (TAU / 8)) % 2) == 0, RING_RED, RING_WHITE)))
    return union_var(items)


def flotsam_b_scene(p):
    items = [(sd_box(rotate_z(p - np.array([-6.0, 5.0, 0.0]), 0.3), (0, 0, 1.2), (6.5, 6.5, 4.0), 0.4), CRATE),
             (sd_box(rotate_z(p - np.array([9.0, -9.0, 0.0]), -0.6), (0, 0, 0.6), (5.0, 5.0, 3.0), 0.4), CRATE),
             (plank(p, (8, 10), 22, 4, -0.9), WOOD), (plank(p, (-10, -12), 18, 4, 0.6), WOOD),
             (sd_capsule(p, (-15.0, 14.0, 0.4), (-7.0, 18.0, 0.4), 2.4), FENDER)]
    return union_var(items)


def union_var(items):
    """union() for items whose material may be an array (per point)."""
    d = np.full(len(items[0][0]), 1e3)
    m = np.zeros(len(items[0][0]), int)
    for dist, mat in items:
        closer = dist < d
        d = np.where(closer, dist, d)
        m = np.where(closer, mat, m)
    return d, m


def lifeboat_scene(p):
    """A capsized enclosed lifeboat, keel up, a little down by the stern; orange hull, dark keel."""
    q = rotate_x(p, 0.08)
    hull = sd_ellipsoid(q, (0, 0, -1.2), (8.5, 24.0, 6.0))
    keel = sd_box(q, (0, 0, 4.4), (0.9, 20.0, 0.8), 0.3)
    rail = sd_box(q, (0, -24.0, 0.0), (3.0, 1.2, 1.2), 0.4)
    return union((hull, ORANGE), (keel, HULL_LO), (rail, HULL_LO))


def floating(scene, size, extent, seed, z_top=14.0, depth=6.0):
    body = wfx.waterline_render(scene, flotsam_mats(), size, extent, z_top=z_top, steps=150, depth=depth, crisp=60)
    pts = wfx.waterline_points(scene, size, extent, step=2)
    foam = wfx.Foam(*size)
    wfx.s8.collar(foam, wfx.loop_f(1, 4), pts, size[0] / 2, size[1] / 2, seed, alpha=0.8, r=1.1, push=1.2)
    img = body.copy()
    img.alpha_composite(foam.image(4))
    arr = np.array(img)
    arr[0], arr[-1], arr[:, 0], arr[:, -1] = 0, 0, 0, 0
    return artkit.quantize_set([Image.fromarray(arr, "RGBA")], 32)[0]


def flotsam_a(w, h):
    return floating(flotsam_a_scene, (w, h), float(w), 1301)


def flotsam_b(w, h):
    return floating(flotsam_b_scene, (w, h), float(w), 1311)


def lifeboat(w, h):
    return floating(lifeboat_scene, (w, h), float(w), 1321)


# --------------------------------------------------------------------------- the freighter's smoke

SMOKE = (88, 232)
SMOKE_FRAMES = 8


def smoke_source():
    """The freighter's middle deck fire on its sprite (l11_props.freighter_frame's spot)."""
    return ships.project(-6.0, 6.0, 9.0, l11_props.FREIGHTER, 14.0)


def smoke_offset():
    """The smoke piece's centre relative to the freighter's (px, +y down the screen): its source
    (row 14 of the piece, 18 px from its left) on the fire."""
    sx, sy = smoke_source()
    fx, fy = sx - l11_props.FREIGHTER[0] / 2, sy - l11_props.FREIGHTER[1] / 2
    return round(fx - (18 - SMOKE[0] / 2)), round(fy - (14 - SMOKE[1] / 2))


_SMOKE_N = {}


def smoke_frame(i):
    """The column from its source at (18, 14) trailing down and a little right (the wind from the
    north), widening and thinning; its density field billows in place over 8 frames (each noise
    octave shifted round a small circle, under 2 px a frame, no travel: the motion budget)."""
    w, h = SMOKE
    if not _SMOKE_N:
        _SMOKE_N["n"] = [raster.fbm(w + 16, h + 16, c, 1400 + c, octaves=1, period=True) for c in (32, 16, 8)]
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64)
    u = np.clip((yy - 14) / (h - 30), 0, 1)
    cx = 18 + (w - 30) * 0.55 * u ** 1.2
    width = 5 + 26 * u ** 0.8
    across = (xx - cx) / width
    shape = np.exp(-across ** 2 * 1.6) * (yy > 6) * np.clip((yy - 6) / 10, 0, 1)
    dens = np.zeros((h, w))
    for k, (n, amp, rad) in enumerate(zip(_SMOKE_N["n"], (0.55, 0.3, 0.15), (2.0, 1.6, 1.2))):
        a = TAU * (i / SMOKE_FRAMES) * (1 if k % 2 == 0 else -1) + k
        ox, oy = 8 + rad * math.cos(a), 8 + rad * math.sin(a)
        dens += amp * sample(n, xx + ox, yy + oy)
    d = np.clip(shape * (0.55 + 0.9 * (dens - 0.5)) * (1 - 0.55 * u), 0, 1)
    lit = np.clip(0.5 + (dens - np.roll(dens, 2, axis=1)) * 3 + (dens - np.roll(dens, 2, axis=0)) * 2, 0, 1)
    rgb = np.array([40, 38, 38], float) + (np.array([96, 96, 98], float) - [40, 38, 38]) * lit[..., None]
    rgb = rgb + (np.array([150, 80, 40], float) - rgb) * (np.clip(1 - (yy - 14) / 18, 0, 1) * (yy > 10))[..., None] * 0.5
    alpha = np.floor(np.clip(d * 1.1, 0, 0.8) * 6 + 0.5) / 6
    arr = np.dstack([rgb, alpha * 255])
    arr[0], arr[-1], arr[:, 0], arr[:, -1] = 0, 0, 0, 0
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")


def sample(n, x, y):
    """Bilinear sample of a periodic field at float positions (wrapping)."""
    h, w = n.shape
    x0, y0 = np.floor(x).astype(int), np.floor(y).astype(int)
    fx, fy = x - x0, y - y0
    a = n[y0 % h, x0 % w]
    b = n[y0 % h, (x0 + 1) % w]
    c = n[(y0 + 1) % h, x0 % w]
    d = n[(y0 + 1) % h, (x0 + 1) % w]
    return (a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy


def freighter_smoke(w, h, n):
    return artkit.quantize_set([smoke_frame(i) for i in range(n)], 24)


# --------------------------------------------------------------------------- Platform Tiamat

PLATFORM_FRAMES = 4
SHADOW_OFF = (10, 14)
SCORCH = [(-0.1, 0.5, 0.22), (0.7, -0.05, 0.18), (-0.75, 0.4, 0.14), (0.25, -0.6, 0.2)]


def platform_deck():
    """Round 07's platform model (render/boss_models.platform, unchanged) at 100 px per unit on the
    piece's canvas, the 4x quality bar; scorched where the Vrell overran it."""
    scene, mats = bm.platform()
    w, h = PLATFORM
    f = 4
    hi = sdf.render(scene, mats, (w * f, h * f), w / 100.0)
    img = artkit.native(hi, f, crisp=70)
    arr = np.array(img).astype(np.float64)
    yy, xx = np.mgrid[0:h, 0:w]
    ux, uy = (xx + 0.5 - w / 2) / 100, -(yy + 0.5 - h / 2) / 100
    n = raster.fbm(w, h, 8, 1501, octaves=3, period=False)
    char = np.zeros((h, w))
    for x, y, r in SCORCH:
        d = np.hypot(ux - x, uy - y) / r
        char = np.maximum(char, np.clip(1.2 - d - 0.5 * (n - 0.5), 0, 1))
    k = np.floor(char * 3 + 0.5) / 3 * 0.55
    arr[..., :3] = arr[..., :3] * (1 - k[..., None]) + np.array([20, 18, 22]) * k[..., None]
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")


def platform_frames():
    deck = platform_deck()
    w, h = PLATFORM
    a = np.array(deck)[..., 3] > 0
    mask = Image.fromarray((a * 255).astype(np.uint8))
    shadow = Image.new("L", (w, h), 0)
    shadow.paste(mask, SHADOW_OFF)
    shadow = np.array(shadow.filter(ImageFilter.GaussianBlur(2.0))) / 255
    sh = np.floor(shadow * 3 + 0.5) / 3 * 0.5
    shade = np.dstack([np.broadcast_to(np.array([6, 16, 24], float), (h, w, 3)), sh * 255])
    pts = s8.outline_points(Image.fromarray(np.dstack([a * 255] * 4).astype(np.uint8), "RGBA"), 2)
    frames = []
    for i in range(PLATFORM_FRAMES):
        img = Image.fromarray(shade.astype(np.uint8), "RGBA")
        foam = wfx.Foam(w, h)
        wfx.s8.collar(foam, wfx.loop_f(i, PLATFORM_FRAMES), pts * 1.0, w / 2, h / 2, 1600, alpha=0.75, r=1.3, push=2.4)
        wfx.s8.collar(foam, wfx.loop_f(i, PLATFORM_FRAMES), pts * 1.0, w / 2, h / 2, 1601, alpha=0.45, r=1.0, push=5.0)
        img.alpha_composite(foam.image(4))
        img.alpha_composite(deck)
        arr = np.array(img)
        arr[0], arr[-1], arr[:, 0], arr[:, -1] = 0, 0, 0, 0
        frames.append(Image.fromarray(arr, "RGBA"))
    return artkit.quantize_set(frames, 56)


def platform_tiamat(w, h, n):
    return platform_frames()


# =========================================================================== render, write

GENERATORS = {
    "swell": swell, "chop": chop, "sea-mist": sea_mist, "mist-banks": mist_banks, "wisps": wisps,
    "spawn-slick-a": lambda w, h: spawn_slick(w, h, 1201), "spawn-slick-b": lambda w, h: spawn_slick(w, h, 1217),
    "reef-shelf-a": lambda w, h: reef_shelf(w, h, 1231), "reef-shelf-b": lambda w, h: reef_shelf(w, h, 1243),
    "oil-slick": oil_slick, "flotsam-a": flotsam_a, "flotsam-b": flotsam_b, "lifeboat": lifeboat,
    "freighter-smoke": freighter_smoke, "platform-tiamat": platform_tiamat,
}
FROM_PROPS = {"burning-freighter", "reef-growth-a", "reef-growth-b", "reef-growth-c", "reef-root-a"}


def jobs(backdrop, wanted):
    out = [(name, (W, spec["height"]), None) for name, spec in backdrop["tile_sets"].items()]
    out += [(name, tuple(spec["size"]), spec.get("frames") or spec.get("headings"))
            for name, spec in backdrop["pieces"].items()]
    return [job for job in out if (not wanted or job[0] in wanted) and job[0] not in FROM_PROPS]


def render(job):
    name, (w, h), count = job
    fn = GENERATORS[name]
    images = fn(w, h, count) if count else [fn(w, h)]
    for img in images:
        if img.size != (w, h):
            raise ValueError(f"{name}: rendered {img.size}, the data file says {(w, h)}")
    return images


def props_images(backdrop, wanted):
    """l11_props.pieces() (it runs its own process pool, so here in the main process); the root
    drawn through the water."""
    names = [n for n in FROM_PROPS if n in backdrop["pieces"] and (not wanted or n in wanted)]
    if not names:
        return []
    sets = l11_props.pieces()
    out = []
    for name in sorted(names):
        spec = backdrop["pieces"][name]
        frames = sets[name]
        if name == "reef-root-a":
            frames = [sub_through(frames[0], 0.5)]
        if frames[0].size != tuple(spec["size"]):
            raise ValueError(f"{name}: l11_props gives {frames[0].size}, the block says {spec['size']}")
        if len(frames) != (spec.get("frames") or 1):
            raise ValueError(f"{name}: l11_props gives {len(frames)} frames, the block says {spec.get('frames') or 1}")
        out.append(((name, tuple(spec["size"]), spec.get("frames")), frames))
    return out


def write(job, images):
    name, _, count = job
    for old in OUT.glob(f"{name}_*.png"):
        old.unlink()
    if count:
        for i, img in enumerate(images):
            artkit.save_png(img, OUT / f"{name}_{i}.png", SOURCE)
    else:
        artkit.save_png(images[0], OUT / f"{name}.png", SOURCE)
    print(f"{name}: {len(images)} image(s) {images[0].size[0]}x{images[0].size[1]}, "
          f"{artkit.colour_count(images)} colours")


def load(name, i=None):
    return Image.open(OUT / (f"{name}_{i}.png" if i is not None else f"{name}.png")).convert("RGBA")


# =========================================================================== the game's draw

def java_round(v):
    return math.floor(v + 0.5)


def piece_centre(backdrop, placed):
    layer = backdrop["pieces"][placed["piece"]]["layer"]
    return scroll_at(placed["t"]) * backdrop["scroll_factors"][layer] + MID


def piece_rect(backdrop, placed, scroll):
    """(left, bottom) on screen in px from the play field's bottom-left at ground scroll ``scroll``."""
    spec = backdrop["pieces"][placed["piece"]]
    w, h = spec["size"]
    f = backdrop["scroll_factors"][spec["layer"]]
    bottom = java_round(piece_centre(backdrop, placed) - h / 2)
    y = bottom - java_round(scroll * f)
    x = java_round(placed["x"] - w / 2)
    return x, y, w, h


def on_screen(backdrop, placed, scroll):
    x, y, w, h = piece_rect(backdrop, placed, scroll)
    return x + w > 0 and x < W and y + h > 0 and y < SCREEN


def image_index(backdrop, placed, real):
    spec = backdrop["pieces"][placed["piece"]]
    if spec.get("frames"):
        return int(math.floor(real * spec["fps"])) % spec["frames"]
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


def draw_pieces(backdrop, frame, layer, scroll, real, additive=None):
    for placed in backdrop["placed"]:
        spec = backdrop["pieces"][placed["piece"]]
        if spec["layer"] != layer or not on_screen(backdrop, placed, scroll):
            continue
        x, y, w, h = piece_rect(backdrop, placed, scroll)
        img = cached(placed["piece"], image_index(backdrop, placed, real))
        top = SCREEN - y - h
        y0, y1 = max(0, top), min(SCREEN, top + h)
        x0, x1 = max(0, x), min(W, x + w)
        sub = frame[y0:y1, x0:x1]
        src = img[y0 - top:y1 - top, x0 - x:x1 - x]
        if additive is None:
            over(sub, src)
        else:
            add(sub, src, additive)


def draw_banks(backdrop, frame, look_a, look_b, weight, key, layer, scroll, real, additive=None):
    s = java_round(scroll * backdrop["scroll_factors"][layer])
    rows = s + (SCREEN - 1 - np.arange(SCREEN))
    base = additive if additive is not None else 1.0
    for look, share in ((look_a, 1 - weight), (look_b, weight)) if look_a is not look_b else ((look_b, 1.0),):
        name = look.get(key)
        if not name or share <= 0:
            continue
        drift = backdrop["tile_sets"][name].get("drift", 0) or 0
        part = tile_rows(name, rows, java_round(drift * real) % W)
        if additive is None:
            over(frame, part, share)
        else:
            add(frame, part, base * share)


def composite(backdrop, t, scroll=None, real=None, units=None):
    """The play field at level time t (ground scroll ``scroll``, real clock ``real``) as Backdrop
    draws it: deep, haze, ground tiles, the `sub` pass (units), ground pieces, the ground objects
    (units), low-air banks and pieces, high-air additive at 40 %."""
    scroll = sim_scroll(t) if scroll is None else scroll
    real = t if real is None else real
    frame = np.zeros((SCREEN, W, 4))
    frame[..., 3] = 255
    look_a, look_b, weight = looks_at(backdrop, t)
    for layer in ("deep", "far"):
        s = java_round(scroll * backdrop["scroll_factors"][layer])
        draw_layer_tiles(backdrop, frame, layer, s)
        draw_pieces(backdrop, frame, layer, scroll, real)
    veil = look_a["haze"] + (look_b["haze"] - look_a["haze"]) * weight
    hz = np.array([int(backdrop["haze_colour"][i:i + 2], 16) for i in (0, 2, 4)], float)
    frame[..., :3] = frame[..., :3] * (1 - veil) + hz * veil
    draw_layer_tiles(backdrop, frame, "ground", java_round(scroll))
    if units:
        units(frame, "sub", real)
    draw_pieces(backdrop, frame, "ground", scroll, real)
    if units:
        units(frame, "ground", real)
    draw_layer_tiles(backdrop, frame, "low-air", java_round(scroll * backdrop["scroll_factors"]["low-air"]))
    draw_banks(backdrop, frame, look_a, look_b, weight, "banks", "low-air", scroll, real)
    draw_pieces(backdrop, frame, "low-air", scroll, real)
    draw_layer_tiles(backdrop, frame, "high-air", java_round(scroll * backdrop["scroll_factors"]["high-air"]), additive=0.4)
    draw_pieces(backdrop, frame, "high-air", scroll, real, additive=0.4)
    draw_banks(backdrop, frame, look_a, look_b, weight, "wisps", "high-air", scroll, real, additive=0.4)
    return Image.fromarray(np.clip(frame[..., :3], 0, 255).astype(np.uint8), "RGB")


# =========================================================================== checks

def wrap_score(a, columns=False):
    """BackdropSeamsTest's wrap score (as backdrop_l07.py)."""
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
        cover = (a[..., 3] > 8).mean()
        line = f"  wrap {name}: rows {score:.2f}, cover {cover:.0%}"
        if score > 1.5:
            problems.append(f"{name}: a line where it repeats ({score:.2f})")
        if name == "chop" and a[..., 3].min() < 1:
            problems.append("chop: a pixel with alpha 0 (the shadow stencil)")
        print(line)
    return problems


def check_edges(backdrop):
    bad = []
    for placed in backdrop["placed"]:
        spec = backdrop["pieces"][placed["piece"]]
        for i in range(spec.get("frames") or 1):
            a = cached(placed["piece"], i if spec.get("frames") else None)[..., 3] > 0
            x, _, w, _ = piece_rect(backdrop, placed, scroll_at(placed["t"]))
            cols = slice(max(0, -x), min(w, W - x))
            if a[0, cols].any() or a[-1, cols].any():
                bad.append(f"{placed['piece']} at t {placed['t']}: its top or bottom row is opaque")
            if (0 < x < W and a[:, 0].any()) or (0 < x + w < W and a[:, -1].any()):
                bad.append(f"{placed['piece']} at t {placed['t']}: a side column is opaque on screen")
    return sorted(set(bad))


SURFACE = {"reef-growth-a", "reef-growth-b", "reef-growth-c", "flotsam-a", "flotsam-b", "lifeboat",
           "burning-freighter", "freighter-smoke"}


def check_channels(backdrop):
    """Nothing that breaks the surface (or the smoke) lies under a convoy station's hull."""
    bad = []
    for placed in backdrop["placed"]:
        if placed["piece"] not in SURFACE:
            continue
        x, _, w, _ = piece_rect(backdrop, placed, 0)
        a = cached(placed["piece"], 0 if backdrop["pieces"][placed["piece"]].get("frames") else None)[..., 3] > 0
        cols = np.nonzero(a.any(axis=0))[0]
        lo, hi = x + cols.min(), x + cols.max()
        for sx, _, kind in STATIONS:
            half = HULL_HALF if kind == "cargo" else 20
            if lo < sx + half + CHANNEL_MARGIN and hi > sx - half - CHANNEL_MARGIN:
                bad.append(f"{placed['piece']} at t {placed['t']} (x {lo}-{hi}) under the station at x {sx}")
    return bad


def check_platform(backdrop):
    """Tiamat's centre at the halt is PLATFORM_AT, and nothing else breaks the surface in the arena."""
    bad = []
    for placed in backdrop["placed"]:
        if placed["piece"] == "platform-tiamat":
            x, y, w, h = piece_rect(backdrop, placed, HALT_SCROLL)
            cx, cy = x + w / 2, SCREEN - (y + h / 2)
            print(f"  platform at the halt: centre ({cx:g}, {cy:g}) below the top; deck x {cx - DECK[0]:g}-{cx + DECK[0]:g}, "
                  f"y {cy - DECK[1]:g}-{cy + DECK[1]:g}")
            if (cx, cy) != PLATFORM_AT:
                bad.append(f"platform at the halt at ({cx}, {cy}), not {PLATFORM_AT}")
        elif on_screen(backdrop, placed, HALT_SCROLL):
            bad.append(f"{placed['piece']} at t {placed['t']} on screen in the arena")
    return bad


def check_screens(backdrop):
    """BackdropCheck.checkScreens: at most 3 mid-size pieces and 2 strongly animated elements on
    screen at every step; every piece is on screen at some time."""
    problems = []
    seen = [False] * len(backdrop["placed"])
    for step in range(int(OUTRO_END * 60) + 1):
        t = step / 60
        scroll = scroll_at(t)
        mid, moving = [], set()
        for i, placed in enumerate(backdrop["placed"]):
            if on_screen(backdrop, placed, scroll):
                seen[i] = True
                spec = backdrop["pieces"][placed["piece"]]
                if spec.get("mid_size"):
                    mid.append(placed["piece"])
                if spec.get("frames") or placed.get("path"):
                    moving.add(placed["piece"])
        for name, spec in backdrop["tile_sets"].items():
            if spec.get("drift"):
                moving.add(name)
        if len(mid) > 3:
            problems.append(f"t={t:.2f}: {len(mid)} mid-size pieces: {mid}")
        if len(moving) > 2:
            problems.append(f"t={t:.2f}: {len(moving)} animated elements: {sorted(moving)}")
    problems += [f"{p['piece']} at t {p['t']} is never on screen" for p, s in zip(backdrop["placed"], seen) if not s]
    return sorted(set(problems))[:12]


def run_checks(backdrop):
    problems = (check_tiles(backdrop) + check_edges(backdrop) + check_channels(backdrop)
                + check_platform(backdrop) + check_screens(backdrop))
    for p in problems:
        print("PROBLEM:", p)
    if not problems:
        print("checks: ok")
    return problems


def atlas_area(backdrop):
    area = sum(W * s["height"] for s in backdrop["tile_sets"].values())
    area += sum(s["size"][0] * s["size"][1] * (s.get("frames") or 1) for s in backdrop["pieces"].values())
    print(f"atlas area: {area:,} px = {area / 2048 ** 2:.2f} pages of 2048x2048")
    return area


# =========================================================================== review: units on top

_SPR = {}


def spr(name):
    if name not in _SPR:
        _SPR[name] = [np.array(f).astype(np.float64) for f in artkit.load_frames(name)]
    return _SPR[name]


def put(frame, img, x, y, alpha=1.0):
    """Paste a float RGBA image centred at (x, y) (px from the top left)."""
    h, w = img.shape[:2]
    left, top = java_round(x - w / 2), java_round(y - h / 2)
    y0, y1 = max(0, top), min(SCREEN, top + h)
    x0, x1 = max(0, left), min(W, left + w)
    if y0 >= y1 or x0 >= x1:
        return
    over(frame[y0:y1, x0:x1], img[y0 - top:y1 - top, x0 - left:x1 - left], alpha)


def put_tl(frame, img, left, top):
    h, w = img.shape[:2]
    put(frame, img, left + w / 2, top + h / 2)


_SUB = {}


def sub(name, i=0, depth=0.45, scale=1):
    key = (name, i, depth, scale)
    if key not in _SUB:
        im = artkit.load_frames(name)[i]
        if scale != 1:
            im = im.resize((im.width * scale, im.height * scale), Image.BILINEAR)
        _SUB[key] = np.array(wfx.sub_look(im, depth)).astype(np.float64)
    return _SUB[key]


def ship_at(frame, kind, x, y, real, wake=True):
    name = "cargo-ship" if kind == "cargo" else "escort-frigate"
    hull = spr(name)[0]
    h, w = hull.shape[:2]
    left, top = x - w / 2, y - h / 2
    piv = PIVOTS[name]
    if wake:
        wk = spr(f"{name}-wake")
        put_tl(frame, wk[int(real * 20) % len(wk)], left + piv["wake"][0], top + piv["wake"][1])
    put_tl(frame, hull, left, top)
    col = spr(f"{name}-collar")
    put_tl(frame, col[int(real * 7.5) % len(col)], left + piv["collar"][0], top + piv["collar"][1])


PIVOTS = {}


def load_pivots():
    import json
    for n in ("cargo-ship", "escort-frigate", "harbour-kraken"):
        PIVOTS[n] = json.loads((artkit.PIVOTS / f"{n}.json").read_text(encoding="utf-8"))


def units_at(t, halted=False, platform_y=None):
    """The convoy at its stations (or in its lanes at the halt) and, with the platform on screen,
    the Kraken's sub-pass head, idle arms and grip stretch under it and its grip overlay on it."""
    def draw(frame, layer, real):
        k = PIVOTS["harbour-kraken"]
        if platform_y is not None and t <= SECTIONS[ARENA]["end"]:
            px, py = PLATFORM_AT[0], platform_y
            if layer == "sub":
                for side in ("left", "right"):
                    idle = sub(f"harbour-kraken-idle-{side}", int(real * 5) % 4, 0.55, 2)
                    put(frame, idle, px + k[f"idle_{side}"][0], py + k[f"idle_{side}"][1])
                put(frame, sub("harbour-kraken-grip-sub", int(real * 5) % 4, 0.4), px + k["grip_sub"][0], py + k["grip_sub"][1])
                put(frame, sub("harbour-kraken-sub", 0, 0.45), px + k["head"][0], py + k["head"][1])
            else:
                grip = spr("harbour-kraken-grip")
                put(frame, grip[int(real * 5) % 4], px + k["grip"][0], py + k["grip"][1])
        if layer != "ground":
            return
        if halted:
            for x, y in LANE_SHIPS:
                ship_at(frame, "cargo", x, y, real, wake=False)
        elif SECTIONS[ARENA]["end"] < t < SECTIONS[ARENA]["end"] + CLEAR_SECONDS:
            for x, y in CLEAR_SHIPS:             # a proposal for step E2b: clear of the platform passing
                ship_at(frame, "cargo", x, y, real)
        else:
            for x, y, kind in STATIONS:
                ship_at(frame, kind, x, y, real)
    return draw


def platform_screen_y(backdrop, scroll):
    for placed in backdrop["placed"]:
        if placed["piece"] == "platform-tiamat":
            x, y, w, h = piece_rect(backdrop, placed, scroll)
            return SCREEN - (y + h / 2)
    return None


def shot(backdrop, t, scroll=None, real=None, halted=False):
    scroll = sim_scroll(t) if scroll is None else scroll
    py = platform_screen_y(backdrop, scroll)
    py = py if py is not None and -110 < py < SCREEN + 110 else None
    return composite(backdrop, t, scroll, real, units_at(t, halted, py))


# =========================================================================== review

TIMES = [(3, "1 MUSTER: THE CONVOY, SEA MIST"), (30, "2 JELLY FIELDS: SPAWN SLICKS"),
         (51, "2 A SLICK AMONG THE STATIONS"), (67, "3 THE FIRST REEF LINE"), (81, "3 REEFS AND THE CHANNELS"),
         (96, "3 REEF LINE (THE POD'S, T=96)"), (108, "3 THE LAST LINE, MIST"), (119.5, "4 FLOTSAM, A LIFEBOAT"),
         (126, "4 THE OIL SLICK"), (129, "4 THE BURNING FREIGHTER"), (145.5, "4 DEBRIS"),
         (159.2, "4 TIAMAT ENTERS AT THE TOP"), (None, "5 THE HALT: LANES, KRAKEN"), (163, "6 TIAMAT PASSES, SHIPS CLEAR"),
         (172, "6 OPEN WATER, LIGHTER")]
GIF = (151.0, 160.0, 3.0, 4.0, 0.1)    # s: the approach from 151, the halt held 3 s (real clock), 4 s resuming


def halt_shot(backdrop, real):
    return shot(backdrop, HALT_T + 0.5, HALT_SCROLL, real, halted=True)


def review(backdrop):
    load_pivots()
    items = []
    for name, spec in list(backdrop["tile_sets"].items()) + list(backdrop["pieces"].items()):
        count = spec.get("frames") if "size" in spec else None
        imgs = [load(name, i) for i in range(count)] if count else [load(name)]
        label = f"{name} {spec['layer']} {imgs[0].width}x{imgs[0].height} {artkit.colour_count(imgs)} col"
        im = imgs[0]
        if "size" not in spec:
            im = im.resize((240, 480), Image.BOX)
        elif max(im.size) <= 72:
            im = im.resize((im.width * 2, im.height * 2), Image.NEAREST)
            label += " 2x"
        items.append((label, im, spec["layer"] == "high-air"))
    smoke = [load("freighter-smoke", i) for i in range(backdrop["pieces"]["freighter-smoke"]["frames"])]
    strip = Image.new("RGBA", (sum(s.width + 4 for s in smoke), smoke[0].height), (0, 0, 0, 0))
    for k, s in enumerate(smoke):
        strip.alpha_composite(s, (k * (s.width + 4), 0))
    items.append(("freighter-smoke frames 0-7", strip, False))
    plats = [load("platform-tiamat", i) for i in range(4)]
    items.append(("platform-tiamat foam frames 0-3 (crop)",
                  Image.fromarray(np.hstack([np.array(p)[120:208, 128:256] for p in plats])), False))
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
    big_y = comp_y + n_rows * 300 + 20
    sheet = raster.sheet(width, big_y + 540 + 60, "LEVEL 11 BACKDROP - FINAL (PROPOSAL)",
                         "PRODUCTION ART, M5 PART E BATCH - R33; STRAIGHT FROM SCENE-OCEAN-R10-A; COMPOSITES AS THE GAME "
                         "DRAWS THEM WITH THE CONVOY (AND AT THE HALT THE KRAKEN'S GRIP AND SUB HEAD), 1/2 SCALE")
    for name, im, add_, px, py in rows:
        plate = Image.new("RGBA", im.size, artkit.PLATE)
        if add_:
            plate = artkit.add_light(plate, im)
        else:
            plate.alpha_composite(im)
        sheet.alpha_composite(plate, (px, py + 14))
        raster.draw_text(sheet, px, py, name.upper()[:48], raster.LABEL)
    for k, (t, label) in enumerate(TIMES):
        px, py = 16 + (k % per_row) * cw, comp_y + (k // per_row) * 300
        img = halt_shot(backdrop, 0.0) if t is None else shot(backdrop, t)
        sheet.alpha_composite(img.convert("RGBA").resize((240, 270), Image.BOX), (px, py + 14))
        raster.draw_text(sheet, px, py, (f"T={t:g} " if t is not None else "") + label, raster.LABEL)
    # the halt at 1x: registration with the lanes marked, and the same with the Kraken off
    halt = halt_shot(backdrop, 0.0).convert("RGBA")
    marked = halt.copy()
    arr = np.array(marked)
    for lx in (120, 240, 360):
        arr[int(PLATFORM_AT[1] + DECK[1]):, lx, :3] = (200, 60, 60)
    marked = Image.fromarray(arr, "RGBA")
    plain = composite(backdrop, HALT_T + 0.5, HALT_SCROLL, 0.0).convert("RGBA")
    sheet.alpha_composite(marked, (16, big_y + 14))
    raster.draw_text(sheet, 16, big_y, f"THE HALT AT 1X: TIAMAT AT ({PLATFORM_AT[0]:g}, {PLATFORM_AT[1]:g}), LANES (RED), GRIP + HEAD", raster.LABEL)
    sheet.alpha_composite(plain, (16 + 480 + 24, big_y + 14))
    raster.draw_text(sheet, 16 + 480 + 24, big_y, "THE BACKDROP ALONE AT THE HALT", raster.LABEL)
    sheet.alpha_composite(shot(backdrop, 81).convert("RGBA"), (16 + 2 * (480 + 24), big_y + 14))
    raster.draw_text(sheet, 16 + 2 * (480 + 24), big_y, "T=81 AT 1X: A REEF LINE, MEDIUM MIST", raster.LABEL)
    REVIEW.parent.mkdir(parents=True, exist_ok=True)
    sheet.convert("RGB").save(REVIEW, optimize=True)
    frames = gif_frames(backdrop)
    gif = REVIEW.with_suffix(".gif")
    artkit.write_gif(frames, gif, fps=round(1 / GIF[4], 1))
    print(f"review: {REVIEW.relative_to(ROOT)}, {gif.relative_to(ROOT)} {gif.stat().st_size / 1e6:.1f} MB")


def gif_frames(backdrop):
    """The approach (level time = real time), the halt held on the real clock, the scroll resuming
    (the open water's speed, without an ease)."""
    t0, t1, hold, after, dt = GIF
    out = []
    real = t0
    for t in np.arange(t0, t1, dt):
        out.append(shot(backdrop, float(t), real=float(t)))
    real = t1
    for k in range(int(round(hold / dt))):
        out.append(halt_shot(backdrop, real + k * dt))
    end5 = SECTIONS[ARENA]["end"]
    for k in range(int(round(after / dt))):
        t = end5 + k * dt
        out.append(shot(backdrop, t, real=real + hold + k * dt))
    return [f.resize((360, 405), Image.BOX) for f in out]


def strip_frames(backdrop, times, path):
    load_pivots()
    shots = [shot(backdrop, t) for t in times]
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
        strip_frames(backdrop, [float(a) for a in argv[k + 1].split(",")], argv[k + 2])
        return
    known = set(GENERATORS) | FROM_PROPS
    wanted = {a for a in argv if not a.startswith("--")}
    missing = (backdrop["tile_sets"].keys() | backdrop["pieces"].keys()) - known
    if missing or wanted - known:
        raise SystemExit(f"no generator for {sorted(missing | (wanted - known))}")
    if "--check" in argv:
        atlas_area(backdrop)
        sys.exit(1 if run_checks(backdrop) else 0)
    if "--review" not in argv:
        OUT.mkdir(parents=True, exist_ok=True)
        for job, images in props_images(backdrop, wanted):
            write(job, images)
        todo = jobs(backdrop, wanted)
        todo.sort(key=lambda j: -(j[1][0] * j[1][1] * (j[2] or 1)))
        with ProcessPoolExecutor() as pool:
            for job, images in zip(todo, pool.map(render, todo)):
                write(job, images)
        if not wanted:
            names = set(backdrop["tile_sets"]) | set(backdrop["pieces"])
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


if __name__ == "__main__":
    main(sys.argv[1:])
