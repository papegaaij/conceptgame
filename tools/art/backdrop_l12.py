#!/usr/bin/env python3
"""Production art: the Level 12 backdrop, the Atlantic storm off the Azores
(design/campaign/act-2-homefront/level-12-storm-front; M5 part F batch, concept round 34).

Straight to production (user decision F8 = a) from the chosen storm scene, scene-storm-r10-a
(tools/concept/scenes_r10.py on scenes_r09.py: its sea's colour stops, five wave components, light
gain, whitecaps, wind streaks and dark scud tones, imported or taken unchanged), built on Level 11's
ocean kit (tools/art/backdrop_l11.py: the sea split into the long swell on `deep` and the short chop
on `ground`, the review composites, the checks, ``--proposal``). The eye's cloud wall and light shaft
are tools/art/eye_l12.py's ``wall_ring("a")`` and ``shaft("a")`` (look a, round 34's audition; the
round decides, so re-run with ``EYE_LOOK = "b"`` if the user picks b).

**The sea** is the storm's: dark slate-teal stops (scenes_r09.STORM_STOPS), the light gain 2.0 of
round 10, the swell's two long components on the opaque `deep` tile set `swell` (0.85), the three
short ones with the torn whitecaps and the horizontal wind streaks (the wind blows from the left) on
the translucent `ground` tile set `chop` (1.0, alpha >= 3 everywhere: the shadow stencil). The same
two tile sets run through sections 1-4 and 6, so there are no seams; the sections differ by their
atmosphere (bank cover, haze toward the rain's grey: the wall's sea drops in contrast) and by the
game's rain. **The eye** (section 5) has its own `chop-calm` (the same chop at 30 %, no caps, no
streaks) and no haze, so its sea lies calm and dark; two ground pieces, `sea-calm-in` and
`sea-calm-out`, sit exactly on the two seams and fade the storm's chop over 320 px into and out of
the calm (each computed so that the composite runs on across the seam).

**The cloud banks** (`low-air`, 1.5 as in the concept) are opaque in their cores (F1 = a: the game
draws `low-air` bodies under them, a glow on top; the flash makes them see-through for 0.5 s):
`scud-light` (~12 %, at the edges: the squall line's first scud), `scud-banks` (~22 %, sections 2,
3, 6) and `storm-banks` (~35 %, the wall's heavy peak, the centre lane kept clear). Two tall bank
pieces (`bank-tall-a`, `-b`) carry the drifting canister through its time on screen inside a bank
on the right (F2 = a; built round the canister's entry and drift, read from the level's data when
it has them, otherwise the assumption below). No wisps on `high-air`: the rain is the game's.

**Screen-fixed weather looks** (new; not backdrop data yet, step 6 draws them, see the proposal's
``weather_looks``): `eye-wall` (760 x 760, the eye's cloud wall all round, turned clockwise at
3 deg/s about the play field's centre by the game: a separate image that is rotated, lit radially
so the turn keeps its light), `eye-shaft` (480 x 540 additive, the pale light on the calm sea) and
`breakout-light` (480 x 200 additive, the sky lightening at the top edge toward the end). They live
with the backdrop images (assets/backdrop/level-12/) and are packed in the backdrop atlas.

**Motion budget**: no tile set drifts and nothing is animated in the backdrop; the strong motion is
the sea's parallax and the game's rain; in the eye the rain stops and the wall turns (12 px/s at
its inner edge, 20 px/s at the corners).

Outputs (assets/backdrop/level-12/):
  swell                deep: the storm's long swell and its slow tone field (opaque)
  chop                 ground: the short chop, torn whitecaps, wind streaks (translucent)
  chop-calm            ground: the eye's calm chop (translucent)
  scud-light, scud-banks, storm-banks   low-air banks: light ~12 %, medium ~22 %, heavy ~35 %
  sea-calm-in, sea-calm-out             ground pieces on the eye's two seams (the chop fading)
  bank-tall-a, bank-tall-b              low-air pieces: the bank on the right hiding the canister
  eye-wall, eye-shaft, breakout-light   screen-fixed weather looks (see above)
  design/campaign/.../level-12-storm-front/concept/backdrop-final-r34-a.png/.gif   review: the
                       tiles, pieces and looks, composites per section as the game is to draw them
                       (rain, banks over the low-air units with their glow, the canister, a flash
                       with its 0.5 s reveal, the gust telegraph, a sonde, the eye's turning wall);
                       the GIF the canister's flash and the way into the eye
  design/campaign/.../level-12-storm-front/concept/backdrop-proposal.yaml   the backdrop block,
                       the sections' tiles and the planned weather looks (--proposal)

Reads the level's data.yaml when it exists (its sections, scroll speed, backdrop block, weather's
rain and lightning, sondes and the canister), otherwise the README's values. Run:
python3 tools/art/backdrop_l12.py [--proposal | --review | --check | --strip t,.. out.png] [id ...]
(all: about 1 min)
"""
import math
import sys
from concurrent.futures import ProcessPoolExecutor

import numpy as np
import yaml
from PIL import Image

import artkit
from artkit import DESIGN, ROOT, TAU, raster

import backdrop_l11 as b11  # noqa: E402  (tools/art: the ocean kit's helpers: blending, wrap score, flow)
import eye_l12  # noqa: E402  (tools/art: the eye's wall ring and shaft, round 34's looks)
import parallax_r02 as r02  # noqa: E402  (concept script, unchanged: periodic noise)
import parallax_r03 as r03  # noqa: E402  (concept script, unchanged: banks, stretched noise, stepped alpha)
import scenes_r06 as s6  # noqa: E402  (concept script, unchanged: wrap-safe tiling)
import scenes_r09 as s9  # noqa: E402  (concept script, unchanged: the storm's colour stops, tone fields)

SCRIPT = "backdrop_l12.py"
BATCH = "M5 part F batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
LEVEL_DIR = DESIGN / "campaign" / "act-2-homefront" / "level-12-storm-front"
CONCEPT = LEVEL_DIR / "concept"
PROPOSAL = CONCEPT / "backdrop-proposal.yaml"
OUT = ROOT / "assets" / "backdrop" / "level-12"
REVIEW = CONCEPT / "backdrop-final-r34-a.png"
W, SCREEN = 480, 540
MID = SCREEN / 2
TILE_H = 960
EYE_LOOK = "a"                          # round 34's audition: look a goes in the game until the user chooses

FACTORS = {"deep": 0.85, "far": 0.9, "ground": 1.0, "low-air": 1.5, "high-air": 2.2}   # scenes_r10's storm
HAZE_COLOUR = "5a6670"                  # the rain's grey over the sea
ATMOSPHERE = {
    "clear": {"haze": 0.0},                                  # the eye: no banks, the calm dark sea
    "light": {"banks": "scud-light", "haze": 0.05},          # the squall line, the breakout's end
    "medium": {"banks": "scud-banks", "haze": 0.10},         # rain wall, lightning field, breakout
    "heavy": {"banks": "storm-banks", "haze": 0.18},         # the wall's peak: the sea drops in contrast
}
RAMP = 5                                # README: heavy 130-145 with 5 s ramps each side

# --------------------------------------------------------------------------- the level's timeline

DEFAULT_SECTIONS = [
    {"name": "Squall Line", "end": 25, "atmosphere": "light"},
    {"name": "Rain Wall", "end": 75, "atmosphere": "medium"},
    {"name": "Lightning Field", "end": 125, "atmosphere": "medium"},
    {"name": "Storm Wall", "end": 150, "atmosphere": "medium", "peak": {"from": 130, "to": 145, "atmosphere": "heavy"}},
    {"name": "The Eye", "end": 175, "atmosphere": "clear"},
    {"name": "Breakout", "end": 205, "atmosphere": "medium", "peak": {"from": 198, "to": 205, "atmosphere": "light"}},
]
# README *Weather* (draft; the level's data replaces them when it has a weather block)
DEFAULT_RAIN = [[0, 0.0], [10, 0.0], [14, 0.3], [25, 0.5], [75, 0.55], [125, 0.6], [130, 1.0], [145, 1.0],
                [148, 0.4], [150, 0.0], [175, 0.0], [178, 0.6], [195, 0.4], [205, 0.15]]
DEFAULT_LIGHTNING = [78, 87, 93, 99.5, 103.5, 111, 117, 123, 129, 135, 141, 147, 180, 189]
DEFAULT_GUSTS = [16, 52, 90, 118, 126, 132, 138, 144, 182]
DEFAULT_SONDES = [[60, 96], [110, 384], [165, 300]]
# The drifting canister (README *Secrets and pickups*; the level's data places it): enters with its
# centre on the top edge at t, x and drifts (dx, dy) px/s in screen space, slowly across and down.
# Here dy counts DOWN the screen; the data's `drift` counts y up (its [-24, -70] is 70 px/s down).
DEFAULT_CANISTER = {"t": 99.0, "x": 420.0, "drift": [-24.0, 70.0]}
REVEAL = 0.5


def level_data():
    path = LEVEL_DIR / "data.yaml"
    data = yaml.safe_load(path.read_text(encoding="utf-8")) if path.exists() else {}
    data = data or {}
    weather = data.get("weather") or {}
    canister = DEFAULT_CANISTER
    for target in data.get("ground_targets") or []:
        drift = target.get("drift") if isinstance(target, dict) else None
        if drift and target.get("layer") == "low-air" and target.get("at"):
            at = target["at"][0]
            canister = {"t": float(at[0]), "x": float(at[1]), "drift": [float(drift[0]), -float(drift[1])]}
    sondes = data.get("sondes") or {}
    return {"scroll_speed": data.get("scroll_speed", 140), "sections": data.get("sections") or DEFAULT_SECTIONS,
            "backdrop": data.get("backdrop"), "own": path.exists(),
            "rain": weather.get("rain") or DEFAULT_RAIN,
            "lightning": (weather.get("lightning") or {}).get("at") or DEFAULT_LIGHTNING,
            "gusts": [g["t"] if isinstance(g, dict) else g for g in ((weather.get("gusts") or {}).get("at") or DEFAULT_GUSTS)],
            "sondes": sondes.get("at") or DEFAULT_SONDES, "canister": canister}


DATA = level_data()
SECTIONS = DATA["sections"]
SPEED = float(DATA["scroll_speed"])
STARTS = [0.0] + [float(s["end"]) for s in SECTIONS[:-1]]
END = float(SECTIONS[-1]["end"])
OUTRO_END = END + 15                    # LevelData.OUTRO_SECONDS
EYE = next(i for i, s in enumerate(SECTIONS) if s["atmosphere"] == "clear")
EYE_FROM, EYE_TO = STARTS[EYE], float(SECTIONS[EYE]["end"])
CANISTER = DATA["canister"]


def speed(i):
    return float(SECTIONS[i].get("speed", SPEED))


def scroll_at(t):
    """LevelData.scrollAt (no halt in this level): before the start and after the end the first and
    the last section's speed carry on."""
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


def seam(layer, index):
    """LevelData.seam rounded as Backdrop rounds it: the layer position where section ``index``'s
    tile set begins (it enters at the top edge when the section starts)."""
    return b11.java_round(scroll_at(STARTS[index]) * FACTORS[layer] + SCREEN)


def t_for_bottom(layer, bottom, h):
    """A placement time whose piece (height h) has its bottom edge exactly at layer position
    ``bottom`` (Backdrop: java_round(centre - h / 2)), to 4 decimals."""
    t = round(t_at((bottom + h / 2 - MID) / FACTORS[layer]), 4)
    centre = scroll_at(t) * FACTORS[layer] + MID
    assert b11.java_round(centre - h / 2) == bottom, (t, centre, bottom)
    return t


# --------------------------------------------------------------------------- the layout

TILE_SETS = {   # id -> (layer, height, drift, note)
    "swell": ("deep", TILE_H, None, "the storm's long swell and its slow tone field (opaque), every section; "
                                    "the chop slides over it (deep at 0.85)"),
    "chop": ("ground", TILE_H, None, "the short chop, torn whitecaps and wind streaks from the left "
                                     "(translucent, alpha >= 3 everywhere: the shadow stencil), sections 1-4 and 6"),
    "chop-calm": ("ground", TILE_H, None, "the eye's calm chop: the same chop at 30 %, no caps, no streaks"),
    "scud-light": ("low-air", TILE_H, None, "light: the squall line's first scud at the edges, ~12 % cover, opaque cores"),
    "scud-banks": ("low-air", TILE_H, None, "medium: dark scud banks ~22 %, opaque cores (they hide low-air bodies)"),
    "storm-banks": ("low-air", TILE_H, None, "heavy: the wall's banks ~35 %, the centre lane kept clear"),
}
SECTION_TILES = [["swell", "chop-calm"] if i == EYE else ["swell", "chop"] for i in range(len(SECTIONS))]
COVER = {"scud-light": 0.12, "scud-banks": 0.22, "storm-banks": 0.35}
BLEND_H = 320
BANK_A, BANK_B = (256, 704), (256, 704)
PIECES = {      # id -> (layer, (w, h), extra spec, mid-size, note)
    "sea-calm-in": ("ground", (W, BLEND_H), {}, False,
                    "on the eye's first seam: the storm's chop fading out over the calm one (320 px)"),
    "sea-calm-out": ("ground", (W, BLEND_H), {}, False, "on the eye's last seam: the storm's chop fading back in"),
    "bank-tall-a": ("low-air", BANK_A, {}, False, "a tall scud bank on the right: the drifting canister's cover"),
    "bank-tall-b": ("low-air", BANK_B, {}, False, None),
}
LOOKS = {       # the screen-fixed weather looks (new, step 6): id -> (size, note)
    "eye-wall": ((eye_l12.RING, eye_l12.RING), "the eye's cloud wall all round (eye_l12.wall_ring, look a)"),
    "eye-shaft": ((W, SCREEN), "the pale shaft of light on the calm sea (eye_l12.shaft, look a; additive)"),
    "breakout-light": ((W, 200), "the sky lightening at the top edge toward the breakout's end (additive)"),
}
SPIN = 3.0                              # deg/s, clockwise on the screen (the storm is steered: README)


def canister_at(t):
    """The canister's centre (x, y from the top) at t, by DATA's entry and drift."""
    dt = t - CANISTER["t"]
    return CANISTER["x"] + CANISTER["drift"][0] * dt, CANISTER["drift"][1] * dt


def canister_leaves():
    return CANISTER["t"] + (SCREEN + 24) / CANISTER["drift"][1]


def bank_piece_times():
    """The two tall banks' placement times and x: each covers half of the canister's time on screen,
    its centre where the canister is at the middle of that half (a low-air piece moves down at the
    ground speed x 1.5, faster than the canister drifts, so each passes over it)."""
    t0, t1 = CANISTER["t"], canister_leaves()
    out = []
    v = SPEED * FACTORS["low-air"]
    for a, b in ((t0, (t0 + t1) / 2), ((t0 + t1) / 2, t1)):
        # rel(t) = y_can(t) - y_piece(t), y_piece(t) = MID + v (t - tp); rel(a) = -rel(b)
        ya, yb = canister_at(a)[1], canister_at(b)[1]
        tp = (v * (a + b) + 2 * MID - ya - yb) / (2 * v)
        xs = [canister_at(a)[0], canister_at(b)[0]]
        out.append((round(tp, 2), round(sum(xs) / 2 + 8)))
    return out


def placements():
    out = []
    eye_in = seam("ground", EYE)
    eye_out = seam("ground", EYE + 1)
    (ta, xa), (tb, xb) = bank_piece_times()
    out.append(({"piece": "bank-tall-a", "t": ta, "x": xa},
                f"3. Lightning Field: the canister's bank on the right (the canister enters at t {CANISTER['t']:g}, x "
                f"{CANISTER['x']:g}, drifting {CANISTER['drift'][0]:g}, {-CANISTER['drift'][1]:g} px/s y up; it is under one of "
                f"the two until it leaves at t {canister_leaves():.1f})"))
    out.append(({"piece": "bank-tall-b", "t": tb, "x": xb}, None))
    out.append(({"piece": "sea-calm-in", "t": t_for_bottom("ground", eye_in, BLEND_H), "x": 240},
                f"5. The Eye: the chop fades into the calm over the eye's first seam (ground position {eye_in}) and back "
                f"out of it under the last one ({eye_out}); exact rows, re-run after the data moves a section end"))
    out.append(({"piece": "sea-calm-out", "t": t_for_bottom("ground", eye_out - BLEND_H, BLEND_H), "x": 240}, None))
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


flow = b11.flow


def write_proposal():
    b = block()
    lines = [
        "# Level 12 backdrop PROPOSAL (tools/art/backdrop_l12.py --proposal; M5 part F batch, round 34).",
        "# For the level-data step to merge into data.yaml: `backdrop` is the level's block, and every section",
        "# takes the `tiles` below. `weather_looks` at the end is NOT a data key yet: the screen-fixed looks",
        "# the game (step 6) draws from the backdrop images, written down here so the merge and step 6 agree.",
        "#",
        "# sections[i].tiles (by section, in order):",
    ]
    for s, tiles in zip(SECTIONS, SECTION_TILES):
        lines.append(f"#   {s['name']}: [{', '.join(tiles)}]")
    lines += [
        "#",
        "# Built round this timeline (re-run --proposal and the images after the data moves a section end):",
        "#   " + ", ".join(f"{s['name']} to {s['end']:g} ({s['atmosphere']}"
                          + (f", {s['peak']['atmosphere']} {s['peak']['from']:g}-{s['peak']['to']:g}" if s.get("peak") else "")
                          + ")" for s in SECTIONS) + f"; {SPEED:g} px/s.",
        "# Sections' atmosphere as assumed: the Storm Wall `medium` with a `heavy` peak 130-145 (the 5 s ramp",
        "#   centred on each end, README), the Breakout `medium` with a `light` peak from 198 (light by t=200).",
        f"# The eye's seams: its chop-calm tile set begins at ground position {seam('ground', EYE)} and ends at",
        f"#   {seam('ground', EYE + 1)}; the two sea-calm pieces lie exactly on them (t to 4 decimals on purpose).",
        f"# The drifting canister (the data's ground target, or the default): enters at t {CANISTER['t']:g} at x {CANISTER['x']:g}",
        f"#   (centre on the top edge), drifting {-CANISTER['drift'][0]:g} px/s left and {CANISTER['drift'][1]:g} px/s down (the data's",
        f"#   drift [{CANISTER['drift'][0]:g}, {-CANISTER['drift'][1]:g}], y up), off the bottom at t {canister_leaves():.1f}; the bank-tall pieces are",
        "#   placed round it: re-run --proposal after the data moves it. Its sprite: `sprite: supply-canister`",
        "#   (tools/art/l12_props.py: 0 intact, 1 hit, 2 spent; supply-canister-glow, the beacon over the bank).",
        "# Occlusion (F1 = a): the game draws `low-air` bodies (Spore Bombers, the canister) before the whole",
        "#   low-air layer (tiles, banks and these pieces), their glow after it.",
        "# Motion budget (BackdropCheck): no animated pieces, no drifting tile sets; the sea moves by its",
        "#   parallax, the rain is the game's.",
        "",
        "backdrop:",
        "  # Presentation only: the Atlantic storm (tools/art/backdrop_l12.py, straight from scene-storm-r10-a, F8 = a,",
        "  # on Level 11's ocean kit). The sea is two layers: the long swell on `deep` (0.85, opaque) and the short",
        "  # chop on `ground` (1.0, translucent); the eye has its own calm chop. Opaque scud banks on `low-air` (1.5).",
        "  scroll_factors: " + flow(b["scroll_factors"]),
        f"  ramp: {RAMP}",
        f"  haze_colour: {HAZE_COLOUR}  # the rain's grey over the sea (none in the eye)",
        "  atmosphere:",
    ]
    notes = {"clear": "the eye: no banks, no haze (the calm dark sea)", "light": "the squall line: scud at the edges ~12 %",
             "medium": "rain wall, lightning field, breakout: scud banks ~22 %",
             "heavy": "the wall's peak: banks ~35 %, the centre lane clear, the sea hazed"}
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
    ring = LOOKS["eye-wall"][0][0]
    lines += [
        "",
        "# NOT level data (yet): the screen-fixed weather looks step 6 draws (WeatherLooks), images in",
        "# assets/backdrop/level-12/<id>.png, packed with the backdrop (nearest). `from`/`to` are level times, faded",
        "# in and out over the backdrop's ramp centred on them; positions are the play field's (centre 240, 270).",
        "weather_looks:",
        f"  eye-wall: {{size: [{ring}, {ring}], layer: low-air, from: {EYE_FROM:g}, to: {EYE_TO:g}, centre: [240, 270], "
        f"spin: {SPIN:g}, blend: alpha}}",
        "    # the cloud wall all round, drawn after the low-air layer and before the play plane, turned CLOCKWISE",
        f"    # on the screen at {SPIN:g} deg/s from 0 at `from` (libGDX: rotation = -{SPIN:g} * (t - {EYE_FROM:g}), t the",
        "    # script clock); lit radially, so any turn keeps its light. Nearest sampling crawls a little on the",
        "    # stepped edge: draw it with the backdrop page's filter switched to Linear for this one draw call.",
        f"  eye-shaft: {{size: [{W}, {SCREEN}], layer: ground, from: {EYE_FROM:g}, to: {EYE_TO:g}, blend: additive}}",
        "    # the light on the calm sea, after the ground layer's tiles and pieces, before the ground objects",
        f"  breakout-light: {{size: [{W}, 200], layer: high-air, top: 0, from: 190, to: {OUTRO_END:g}, blend: additive}}",
        "    # the sky lightening at the top edge, fading in over 190-200 and held into the outro; at 40 % like high-air",
    ]
    PROPOSAL.parent.mkdir(parents=True, exist_ok=True)
    PROPOSAL.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"proposal: {PROPOSAL.relative_to(ROOT)}")


def level_backdrop():
    own = DATA.get("backdrop") or {}
    if own and "images" not in own:
        return own, "data.yaml"
    write_proposal()
    return yaml.safe_load(PROPOSAL.read_text(encoding="utf-8"))["backdrop"], PROPOSAL.name


# =========================================================================== the sea

STOPS = s9.STORM_STOPS                 # round 09/10's storm colour stops
TONES = 7                              # scenes_r10: tones=7, light_gain=2.0, warp 2.4
LIGHT_GAIN = 2.0
WARP_GAIN = 2.4
# (n, m, amplitude): n whole cycles across the 480 px width, m along the 960 px tile; scenes_r10's
# storm components (220/58 deg, 128/40, 80/76, 48/24, 30/64) rounded to whole cycles
SWELL = [(1, 4, 1.25), (3, 5, 0.9)]
CHOP = [(1, 12, 0.6), (9, 8, 0.34), (7, 29, 0.18)]

_SEA = {}


def sea_fields():
    if not _SEA:
        rng = np.random.default_rng(1201)
        phases = rng.uniform(0, TAU, len(SWELL) + len(CHOP))
        warp = [r02.periodic_fbm(W, TILE_H, 160, 1208 + i, octaves=2).astype(np.float64) for i in range(2)]
        yy, xx = np.mgrid[0:TILE_H, 0:W].astype(np.float64)

        def height(comps, ph):
            h = np.zeros((TILE_H, W))
            for i, ((n, m, a), p) in enumerate(zip(comps, ph)):
                h += a * np.sin(TAU * n * xx / W + TAU * m * yy / TILE_H + WARP_GAIN * (warp[i % 2] - 0.5) + p)
            return h
        _SEA["swell"] = height(SWELL, phases[:len(SWELL)])
        _SEA["chop"] = height(CHOP, phases[len(SWELL):])
        _SEA["tone"] = r02.periodic_fbm(W, TILE_H, 160, 1254, octaves=3).astype(np.float64)
        _SEA["capn"] = r02.periodic_fbm(W, TILE_H, 16, 1251, octaves=3).astype(np.float64)
        wind = r03.stretched(W, TILE_H, [64, 32, 16], 1252, 8)
        _SEA["windline"] = np.clip(1 - np.abs(wind - 0.5) / 0.05, 0, 1)
        _SEA["windgate"] = s9.tone_field(TILE_H, 1253, 120).astype(np.float64)
    return _SEA


def light(h):
    """scenes_r08.Sea.light (the storm's gain 2.0) with periodic gradients (the tile wraps both ways)."""
    gx = (np.roll(h, -1, 1) - np.roll(h, 1, 1)) / 2
    gy = (np.roll(h, -1, 0) - np.roll(h, 1, 0)) / 2
    nx, ny = -gx * 3.0, -gy * 3.0
    inv = 1.0 / np.sqrt(nx * nx + ny * ny + 1)
    diff = (nx * -0.48 + ny * -0.56 + inv * 0.68) - 0.68
    return diff * LIGHT_GAIN + 0.025 * h


def posterized(L):
    t = np.clip(0.5 + L, 0, 0.999)
    return raster.ramp(STOPS, np.clip(np.floor(t * TONES) / (TONES - 1), 0, 1))


def toned(body):
    """scenes_r10's storm tone field: body * (0.84 + 0.30 tone)."""
    tone = sea_fields()["tone"][..., None]
    return body * (0.84 + 0.30 * tone)


def swell_body():
    return toned(posterized(light(sea_fields()["swell"])))


def swell(w, h):
    img = Image.fromarray(np.clip(swell_body(), 0, 255).astype(np.uint8), "RGB").convert("RGBA")
    return artkit.quantize_set([img], 40)[0]


CREST = np.array(STOPS[-1][1], float) * 1.05
TROUGH = np.array((2, 6, 12), float)
CAP = np.array((168, 180, 186), float)          # scenes_r10's torn crests
STREAK = np.array((112, 128, 136), float)       # its wind streaks
LUM = np.array([0.299, 0.587, 0.114])


def chop_layer(calm=False):
    """ground: the chop as the difference the concept's whole sea makes over the swell alone (Level
    11's construction), plus the storm's torn whitecaps and faint wind streaks along the wind; calm:
    the same chop at 30 % without caps and streaks. alpha >= 3 everywhere (the shadow stencil)."""
    f = sea_fields()
    base = swell_body()
    target = toned(posterized(light(f["swell"] + f["chop"])))
    lb, lt = base @ LUM, target @ LUM
    up = lt >= lb
    lc = np.where(up, CREST @ LUM, TROUGH @ LUM)
    a = np.clip((lt - lb) / np.where(up, np.maximum(lc - lb, 1), np.minimum(lc - lb, -1)), 0, 1)
    rgb = np.where(up[..., None], CREST, TROUGH)
    if calm:
        a = np.round(a * 0.3 * 8) / 8
        alpha = a
    else:
        a = np.round(a * 8) / 8
        hgt = f["swell"] + f["chop"]
        caps = np.clip((hgt - 1.9) * 1.4, 0, 1) * np.clip((f["capn"] - 0.6) * 8, 0, 1)   # sparse torn crests
        caps = np.round(caps * 3) / 3 * 0.45
        streak = f["windline"] * np.clip((f["windgate"] - 0.45) * 4, 0, 1)
        streak = np.round(streak * 3) / 3 * 0.3
        rgb = np.where((streak > a)[..., None], STREAK, rgb)
        alpha = np.maximum(a, streak)
        rgb = np.where((caps > 0)[..., None], CAP, rgb)
        alpha = np.where(caps > 0, np.maximum(caps, alpha), alpha)
    a8 = np.maximum(3, np.round(alpha * 255))
    return np.dstack([np.clip(rgb, 0, 255), a8])


def chop(w, h):
    return Image.fromarray(chop_layer().astype(np.uint8), "RGBA")


def chop_calm(w, h):
    return Image.fromarray(chop_layer(calm=True).astype(np.uint8), "RGBA")


def blend_piece(direction):
    """A seam piece: the storm's chop at exactly the rows the tiles show at its place, composited
    over the calm chop so the result fades from the storm's sea at the seam (f = 1) to the calm (f = 0)
    over 320 px. Both chops share their colours (crest or trough), so the right alpha makes the blend
    exact: beta = a f (1 - k) / (1 - k a), k the calm's share (caps and streaks approximately)."""
    storm = chop_layer().astype(np.float64)
    calm = chop_layer(calm=True).astype(np.float64)
    bottom = seam("ground", EYE) if direction == "in" else seam("ground", EYE + 1) - BLEND_H
    rows = np.array([TILE_H - 1 - ((bottom + k) % TILE_H) for k in range(BLEND_H)])   # image rows, bottom up
    s, c = storm[rows], calm[rows]                                     # index 0 = the piece's bottom row
    k_up = np.arange(BLEND_H) / (BLEND_H - 1)
    f = 1 - k_up if direction == "in" else k_up                      # 1 at the seam
    f = (f * f * (3 - 2 * f))[:, None]
    a_s, a_c = s[..., 3] / 255, c[..., 3] / 255
    beta = np.clip((a_s * f - a_c * f) / np.maximum(1 - a_c, 1e-3), 0, 1)
    beta = np.round(beta * 16) / 16
    out = np.dstack([s[..., :3], beta * 255])[::-1]                 # image rows top down
    out[..., 3] = np.where(out[..., 3] < 1, 0, out[..., 3])
    out[out[..., 3] == 0] = 0
    return Image.fromarray(np.clip(out, 0, 255).astype(np.uint8), "RGBA")


# =========================================================================== the banks

SCUD = [(16, 19, 25), (28, 33, 40), (42, 47, 55), (58, 64, 71), (74, 81, 88)]   # scenes_r10's dark scud tones, a lit top added
BANK_COLOURS = 12


def edge_weight(xx):
    """0 in the centre 200 px, 1 at the edges."""
    return np.clip((np.abs(xx - W / 2 + 0.5) - 100) / 140, 0, 1)


def bank_field(name, seed):
    n = r03.stretched(W, TILE_H, [240, 120, 60, 30], seed, 2)
    xx = np.arange(W, dtype=np.float64)[None, :]
    e = edge_weight(xx)
    if name == "scud-light":
        return n - 0.30 + 0.42 * e
    if name == "storm-banks":
        lane = np.clip(1 - np.abs(xx - W / 2 + 0.5) / 72, 0, 1)
        return n + 0.16 * e - 0.32 * lane
    return n + 0.10 * e - 0.04


def bank_image(field, cover, max_alpha=255):
    b = s6.pmid(r03.bank(s6.ptile(field), cover, 0.10, SCUD, max_alpha=max_alpha, light=5.0), TILE_H)
    return r03.step_alpha(r02.posterize(b, BANK_COLOURS), 6)


def bank_tile(name, seed):
    """An opaque-cored scud bank tile (r03.bank: shaded from the underside to the lit top-left,
    soft stepped edges) whose cover is tuned to the intensity's share by bisection."""
    field = bank_field(name, seed)
    lo, hi = 0.0, 1.2
    for _ in range(18):
        mid = (lo + hi) / 2
        cover = (np.array(bank_image(field, mid))[..., 3] > 8).mean()
        lo, hi = (mid, hi) if cover > COVER[name] else (lo, mid)
    return bank_image(field, (lo + hi) / 2)


def bank_tall(w, h, seed):
    """A tall bank piece: stretched noise under a soft vertical lozenge, opaque in its core,
    transparent at every border."""
    n = raster.fbm(w, h, 32, seed, octaves=4, period=False)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64) + 0.5
    dx, dy = np.abs(xx - w / 2) / (w / 2), np.abs(yy - h / 2) / (h / 2)
    r = (dx ** 3 + dy ** 3) ** (1 / 3)                         # a rounded slab, not a lozenge
    field = n * 0.45 + (1 - r) * 1.1
    b = r03.bank(field, 0.42, 0.12, SCUD, max_alpha=255, light=5.0)
    img = r03.step_alpha(r02.posterize(b, BANK_COLOURS), 6)
    a = np.array(img)
    a[:2], a[-2:], a[:, :2], a[:, -2:] = 0, 0, 0, 0
    return Image.fromarray(a, "RGBA")


# =========================================================================== the looks

def breakout_light(w, h):
    """The sky lightening at the top edge: a pale warm-white glow fading down over 200 px, its lower
    edge broken by cloud (additive, premultiplied, light stepped to 8 levels)."""
    n = raster.fbm(w, h, 40, 1290, octaves=4, period=False)
    yy = np.mgrid[0:h, 0:w][0].astype(np.float64) + 0.5
    fall = np.clip(1 - yy / h, 0, 1)
    a = np.clip(fall ** 1.6 * (0.7 + 0.6 * n) - 0.05, 0, 1) * 0.55
    a = np.floor(a * 8 + 0.5) / 8
    rgb = np.array([226, 222, 206], float) * a[..., None]
    alpha = np.where(rgb.max(axis=-1) >= 1, 255, 0)
    return Image.fromarray(np.dstack([np.round(rgb), alpha]).astype(np.uint8), "RGBA")


def eye_wall(w, h):
    return eye_l12.wall_ring(EYE_LOOK)


def eye_shaft(w, h):
    return eye_l12.shaft(EYE_LOOK)


# =========================================================================== render, write

GENERATORS = {
    "swell": swell, "chop": chop, "chop-calm": chop_calm,
    "scud-light": lambda w, h: bank_tile("scud-light", 1221), "scud-banks": lambda w, h: bank_tile("scud-banks", 1223),
    "storm-banks": lambda w, h: bank_tile("storm-banks", 1227),
    "sea-calm-in": lambda w, h: blend_piece("in"), "sea-calm-out": lambda w, h: blend_piece("out"),
    "bank-tall-a": lambda w, h: bank_tall(w, h, 1231), "bank-tall-b": lambda w, h: bank_tall(w, h, 1237),
    "eye-wall": eye_wall, "eye-shaft": eye_shaft, "breakout-light": breakout_light,
}


def jobs(backdrop, wanted):
    out = [(name, (W, spec["height"])) for name, spec in backdrop["tile_sets"].items()]
    out += [(name, tuple(spec["size"])) for name, spec in backdrop["pieces"].items()]
    out += [(name, size) for name, (size, _) in LOOKS.items()]
    return [job for job in out if not wanted or job[0] in wanted]


def render(job):
    name, (w, h) = job
    img = GENERATORS[name](w, h)
    if img.size != (w, h):
        raise ValueError(f"{name}: rendered {img.size}, the block says {(w, h)}")
    return img


def write(job, img):
    name = job[0]
    artkit.save_png(img, OUT / f"{name}.png", SOURCE)
    print(f"{name}: {img.size[0]}x{img.size[1]}, {artkit.colour_count([img])} colours")


_cache = {}


def cached(name):
    if name not in _cache:
        _cache[name] = np.array(Image.open(OUT / f"{name}.png").convert("RGBA")).astype(np.float64)
    return _cache[name]


# =========================================================================== the game's draw

over, add = b11.over, b11.add
java_round = b11.java_round


def piece_rect(backdrop, placed, scroll):
    spec = backdrop["pieces"][placed["piece"]]
    w, h = spec["size"]
    f = backdrop["scroll_factors"][spec["layer"]]
    centre = scroll_at(placed["t"]) * f + MID
    bottom = java_round(centre - h / 2)
    return java_round(placed["x"] - w / 2), bottom - java_round(scroll * f), w, h


def on_screen(backdrop, placed, scroll):
    x, y, w, h = piece_rect(backdrop, placed, scroll)
    return x + w > 0 and x < W and y + h > 0 and y < SCREEN


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


def smooth(p):
    p = min(1.0, max(0.0, p))
    return p * p * (3 - 2 * p)


def window(t, t0, t1, ramp=RAMP):
    """A look's strength: faded in over the ramp centred on t0, out over the one centred on t1."""
    return smooth((t - t0) / ramp + 0.5) * (1 - smooth((t - t1) / ramp + 0.5))


def tile_rows(name, rows):
    img = cached(name)
    hgt = img.shape[0]
    return img[[hgt - 1 - (int(r) % hgt) for r in rows]]


def draw_layer_tiles(frame, layer, scroll, sections_tiles, backdrop):
    rows = scroll + (SCREEN - 1 - np.arange(SCREEN))
    for s in range(len(SECTIONS)):
        tiles = [n for n in sections_tiles[s] if backdrop["tile_sets"][n]["layer"] == layer]
        if not tiles:
            continue
        lo = -math.inf if s == 0 else seam(layer, s)
        hi = seam(layer, s + 1) if s + 1 < len(SECTIONS) else math.inf
        sel = (rows >= lo) & (rows < hi)
        if sel.any():
            sub = frame[sel]
            over(sub, tile_rows(tiles[0], rows[sel]))
            frame[sel] = sub


def draw_pieces(backdrop, frame, layer, scroll, lit=0.0, see=0.0):
    for placed in backdrop["placed"]:
        spec = backdrop["pieces"][placed["piece"]]
        if spec["layer"] != layer or not on_screen(backdrop, placed, scroll):
            continue
        x, y, w, h = piece_rect(backdrop, placed, scroll)
        img = lit_bank(cached(placed["piece"]), lit) if layer == "low-air" else cached(placed["piece"])
        top = SCREEN - y - h
        y0, y1 = max(0, top), min(SCREEN, top + h)
        x0, x1 = max(0, x), min(W, x + w)
        over(frame[y0:y1, x0:x1], img[y0 - top:y1 - top, x0 - x:x1 - x], 1 - see)


LIT = np.array([190, 205, 235], float)


def lit_bank(img, lit):
    """A bank lit from inside by a flash (the game: FlashShader toward a pale blue, 0.45 at the flash)."""
    if lit <= 0:
        return img
    out = img.copy()
    out[..., :3] = img[..., :3] + (LIT - img[..., :3]) * (0.45 * lit)
    return out


def draw_banks(backdrop, frame, look_a, look_b, weight, scroll, lit=0.0, see=0.0):
    s = java_round(scroll * backdrop["scroll_factors"]["low-air"])
    rows = s + (SCREEN - 1 - np.arange(SCREEN))
    for look, share in ((look_a, 1 - weight), (look_b, weight)) if look_a is not look_b else ((look_b, 1.0),):
        name = look.get("banks")
        if not name or share <= 0:
            continue
        over(frame, lit_bank(tile_rows(name, rows), lit), share * (1 - see))


def flash_state(t):
    """(sky, reveal): the sky flash (<= 0.1 s) and the banks' 0.5 s reveal after the last flash."""
    sky, reveal = 0.0, 0.0
    for at in DATA["lightning"]:
        a = t - at
        if 0 <= a < 0.1:
            sky = max(sky, 1 - a / 0.1)
        if 0 <= a < REVEAL:
            reveal = max(reveal, 1 - a / REVEAL)
    return sky, reveal


def rain_at(t):
    keys = DATA["rain"]
    if t <= keys[0][0]:
        return keys[0][1]
    for (t0, d0), (t1, d1) in zip(keys, keys[1:]):
        if t < t1:
            return d0 + (d1 - d0) * (t - t0) / (t1 - t0)
    return keys[-1][1]


_SPR = {}


def spr(name):
    if name not in _SPR:
        _SPR[name] = [np.array(f).astype(np.float64) for f in artkit.load_frames(name)]
    return _SPR[name]


def put(frame, img, x, y, alpha=1.0, additive=False):
    """A float RGBA image centred at (x, y) (px from the top left), alpha-blended or added."""
    h, w = img.shape[:2]
    left, top = java_round(x - w / 2), java_round(y - h / 2)
    y0, y1 = max(0, top), min(SCREEN, top + h)
    x0, x1 = max(0, left), min(W, left + w)
    if y0 >= y1 or x0 >= x1:
        return
    src = img[y0 - top:y1 - top, x0 - left:x1 - left]
    if additive:
        add(frame[y0:y1, x0:x1], src, alpha)
    else:
        over(frame[y0:y1, x0:x1], src, alpha)


_RING = {}


def ring_turned(t):
    """The wall ring turned clockwise by SPIN deg/s since the eye began, nearest-sampled as the
    backdrop atlas draws it (rounded to 0.25 deg for the cache)."""
    ang = round(SPIN * (t - EYE_FROM) * 4) / 4
    if ang not in _RING:
        ring = Image.open(OUT / "eye-wall.png").convert("RGBA")
        _RING[ang] = np.array(ring.rotate(-ang, resample=Image.NEAREST)).astype(np.float64)
    return _RING[ang]


RAIN_DROPS = None


def rain_drops():
    global RAIN_DROPS
    if RAIN_DROPS is None:
        rng = np.random.default_rng(1260)
        RAIN_DROPS = [(rng.uniform(0, W), rng.uniform(0, 960), int(rng.integers(0, 4)), rng.uniform(0.85, 1.15),
                       rng.uniform(0, 1)) for _ in range(220)]
    return RAIN_DROPS


def draw_rain(frame, t, real):
    """The game's rain (a stand-in for step 6): streaks falling 480 px/s with a 1:4 slant to the
    right, as many as the density allows (220 at 1.0), additive at 40 %."""
    dens = rain_at(t)
    if dens <= 0:
        return
    sprites = spr("weather-rain")
    for x0, y0, v, sp, gate in rain_drops():
        if gate >= dens:
            continue
        y = (y0 + 480 * sp * real) % 960 - 200
        x = (x0 + 120 * sp * real) % W
        if -20 < y < SCREEN + 20:
            put(frame, sprites[v], x, y, 0.4, additive=True)


def lamprey(heading, phase=0):
    return spr("lamprey")[(heading % 16) * 4 + phase % 4]


def units_at(t, real):
    """Stand-ins for the play plane and the low-air bodies at t, to show the layers: the ship, the
    Spore Bombers under the banks (t 64-72, 146-152), the canister, Lampreys and a sonde."""
    out = {"low": [], "air": [], "glow": []}
    if 63 <= t <= 74 or 145 <= t <= 153:
        t0 = 63 if t < 100 else 145
        for k, x in enumerate((150, 330)):
            y = -40 + (t - t0) * 70
            out["low"].append((spr("spore-bomber")[0], x, y))
            out["glow"].append((spr("weather-glow")[int(real * 4) % 4], x, y))
    if CANISTER["t"] <= t <= canister_leaves():
        x, y = canister_at(t)
        state = 0 if t < CANISTER["t"] + 4 else 1
        out["low"].append((spr("supply-canister")[state], x, y))
        out["glow"].append((spr("supply-canister-glow")[int(real * 8) % 4 + 4 * state], x, y))
    for st, sx in DATA["sondes"]:
        y = -16 + (t - st) * 60
        if -20 < y < SCREEN + 20:
            out["air"].append((spr("weather-sonde")[int(real * 6) % 4], sx, y))
    if 32 <= t <= 40 or 86 <= t <= 92 or 136 <= t <= 142:
        for k in range(3):
            out["air"].append((lamprey(12 + k, int(real * 10) + k), 120 + 110 * k, 120 + 40 * math.sin(k + t)))
    out["air"].append((spr("ship")[0], 240 + 40 * math.sin(t * 0.7), 440))
    return out


def composite(backdrop, t, real=None, sections_tiles=None, units=True, flash=None):
    """The play field at level time t as the game is to draw it (F1 = a): deep, haze, ground tiles and
    pieces, the eye's shaft, the low-air bodies, the low-air layer (banks, pieces: lit and seen through
    after a flash), the eye's wall, the bodies' glows, the bolt, the play plane, the rain and the
    breakout's light on high-air, the sky flash."""
    real = t if real is None else real
    sections_tiles = sections_tiles or SECTION_TILES
    scroll = scroll_at(t)
    frame = np.zeros((SCREEN, W, 4))
    frame[..., 3] = 255
    look_a, look_b, weight = looks_at(backdrop, t)
    draw_layer_tiles(frame, "deep", java_round(scroll * backdrop["scroll_factors"]["deep"]), sections_tiles, backdrop)
    veil = look_a["haze"] + (look_b["haze"] - look_a["haze"]) * weight
    hz = np.array([int(backdrop["haze_colour"][i:i + 2], 16) for i in (0, 2, 4)], float)
    frame[..., :3] = frame[..., :3] * (1 - veil) + hz * veil
    draw_layer_tiles(frame, "ground", java_round(scroll), sections_tiles, backdrop)
    draw_pieces(backdrop, frame, "ground", scroll)
    eye = window(t, EYE_FROM, EYE_TO)
    if eye > 0:
        put(frame, cached("eye-shaft"), W / 2, SCREEN / 2, eye, additive=True)
    sky, reveal = flash_state(t) if flash is None else flash
    u = units_at(t, real) if units else {"low": [], "air": [], "glow": []}
    for img, x, y in u["low"]:
        put(frame, img, x, y)
    draw_banks(backdrop, frame, look_a, look_b, weight, scroll, reveal, 0.6 * reveal)
    draw_pieces(backdrop, frame, "low-air", scroll, reveal, 0.6 * reveal)
    if eye > 0:
        put(frame, ring_turned(t), W / 2, SCREEN / 2, eye)
    for img, x, y in u["glow"]:
        put(frame, img, x, y, 1.0, additive=True)
    if sky > 0:
        bolt = spr("weather-bolt")[int(t * 7) % 3]
        put(frame, bolt, 84 if int(t) % 2 else 396, bolt.shape[0] / 2 - 10, 1.0, additive=True)
    if reveal > 0:
        put(frame, spr("weather-flash")[0], 84 if int(t) % 2 else 396, 120, reveal, additive=True)
    for img, x, y in u["air"]:
        put(frame, img, x, y)
    draw_rain(frame, t, real)
    light_end = window(t, 195, OUTRO_END + 10, ramp=10)
    if light_end > 0:
        put(frame, cached("breakout-light"), W / 2, 100, 0.4 * light_end, additive=True)
    if sky > 0:
        frame[..., :3] = np.minimum(255, frame[..., :3] + np.array([150, 165, 190]) * 0.22 * sky)
    return Image.fromarray(np.clip(frame[..., :3], 0, 255).astype(np.uint8), "RGB")


def gust_overlay(img, t):
    """The gust telegraph at the left edge for the review (the game's look: the arrow and streaks)."""
    frame = np.array(img.convert("RGBA")).astype(np.float64)
    arrows = spr("weather-gust-arrow")
    streaks = spr("weather-gust-streak")
    k = int(t * 12)
    put(frame, arrows[k % len(arrows)], 36, 270, 1.0, additive=True)
    for j, y in enumerate((120, 210, 330, 420)):
        put(frame, streaks[j % 2], -80 + (k * 18 + j * 90) % 640, y, 0.7, additive=True)
    return Image.fromarray(np.clip(frame[..., :3], 0, 255).astype(np.uint8), "RGB")


# =========================================================================== checks

def check_tiles(backdrop):
    problems = []
    for name in backdrop["tile_sets"]:
        a = cached(name)
        score = b11.wrap_score(a)
        cover = (a[..., 3] > 8).mean()
        core = (a[..., 3] >= 250).mean()
        print(f"  wrap {name}: rows {score:.2f}, cover {cover:.0%}, opaque {core:.0%}")
        if score > 1.5:
            problems.append(f"{name}: a line where it repeats ({score:.2f})")
        if name.startswith("chop") and a[..., 3].min() < 3:
            problems.append(f"{name}: a pixel with alpha < 3 (the shadow stencil)")
        if name in COVER and abs(cover - COVER[name]) > 0.03:
            problems.append(f"{name}: cover {cover:.0%}, not ~{COVER[name]:.0%}")
        if name in COVER and core < cover * 0.4:
            problems.append(f"{name}: too few opaque cores ({core:.0%} of {cover:.0%}) to hide low-air bodies")
    lane = cached("storm-banks")[:, 200:280, 3]
    print(f"  storm-banks: the centre lane (x 200-280) {(lane > 8).mean():.0%} covered")
    if (lane > 8).mean() > 0.12:
        problems.append("storm-banks: the centre lane is not readable")
    return problems


def check_edges(backdrop):
    bad = []
    for placed in backdrop["placed"]:
        if placed["piece"].startswith("sea-calm"):
            continue
        a = cached(placed["piece"])[..., 3] > 0
        if a[0].any() or a[-1].any() or a[:, 0].any() or a[:, -1].any():
            bad.append(f"{placed['piece']}: an opaque border")
    return bad


def check_seams(backdrop):
    """The sea runs on across the eye's two seams: the change between the rows either side of a seam
    is no larger than the sea's usual row-to-row change (deep and ground composited)."""
    bad = []
    for idx in (EYE, EYE + 1):
        pos = seam("ground", idx)
        t = STARTS[idx] + 0.6
        scroll = scroll_at(t)
        img = np.array(composite(backdrop, t, units=False, flash=(0, 0))).astype(np.float64)
        row = SCREEN - 1 - (pos - java_round(scroll))
        lum = img @ LUM
        jump = np.abs(lum[row] - lum[row - 1]).mean()
        ref = np.median([np.abs(lum[r] - lum[r - 1]).mean() for r in range(row - 40, row + 40, 3) if r != row])
        print(f"  seam {idx} (ground {pos}, screen row {row}): change {jump:.1f}, around it {ref:.1f}")
        if jump > 1.6 * ref:
            bad.append(f"seam {idx}: the sea jumps ({jump:.1f} vs {ref:.1f})")
    return bad


def check_canister(backdrop):
    """The canister is under bank cover (alpha >= 75 %) for most of its time on screen."""
    covered, total = 0, 0
    for step in range(int(CANISTER["t"] * 10), int(canister_leaves() * 10)):
        t = step / 10
        x, y = canister_at(t)
        if not (0 <= x < W and 0 <= y < SCREEN):
            continue
        look_a, look_b, weight = looks_at(backdrop, t)
        scroll = scroll_at(t)
        draw_banks_alpha = np.zeros((SCREEN, W))
        for look, share in ((look_a, 1 - weight), (look_b, weight)):
            name = look.get("banks")
            if name and share > 0:
                rows = java_round(scroll * FACTORS["low-air"]) + (SCREEN - 1 - np.arange(SCREEN))
                al = tile_rows(name, rows)[..., 3] / 255 * share
                draw_banks_alpha = draw_banks_alpha + al * (1 - draw_banks_alpha)
        for placed in backdrop["placed"]:
            spec = backdrop["pieces"][placed["piece"]]
            if spec["layer"] == "low-air" and on_screen(backdrop, placed, scroll):
                px, py, w, h = piece_rect(backdrop, placed, scroll)
                top = SCREEN - py - h
                ix, iy = int(x) - px, int(y) - top
                if 0 <= ix < w and 0 <= iy < h:
                    al = cached(placed["piece"])[iy, ix, 3] / 255
                    draw_banks_alpha[int(y), int(x)] = al + draw_banks_alpha[int(y), int(x)] * (1 - al)
        total += 1
        covered += draw_banks_alpha[int(y), int(x)] >= 0.75
    share = covered / max(total, 1)
    print(f"  canister under a bank (>= 75 %): {share:.0%} of its {total / 10:.1f} s on screen")
    return [] if share >= 0.75 else [f"the canister is under a bank only {share:.0%} of its time"]


def check_screens(backdrop):
    problems = []
    seen = [False] * len(backdrop["placed"])
    for step in range(int(OUTRO_END * 60) + 1):
        scroll = scroll_at(step / 60)
        for i, placed in enumerate(backdrop["placed"]):
            if on_screen(backdrop, placed, scroll):
                seen[i] = True
    problems += [f"{p['piece']} at t {p['t']} is never on screen" for p, s in zip(backdrop["placed"], seen) if not s]
    for name, spec in backdrop["pieces"].items():
        if spec.get("frames") or spec.get("mid_size"):
            problems.append(f"{name}: animated or mid-size (the backdrop has none on purpose)")
    return problems


def check_flashes():
    times = sorted(DATA["lightning"])
    worst = min((b - a for a, b in zip(times, times[1:])), default=99)
    print(f"  lightning: {len(times)} flashes, closest {worst:.1f} s apart")
    return [] if worst >= 1 / 3 else ["more than 3 flashes a second"]


def run_checks(backdrop):
    problems = (check_tiles(backdrop) + check_edges(backdrop) + check_seams(backdrop) + check_canister(backdrop)
                + check_screens(backdrop) + check_flashes())
    for name, (size, _) in LOOKS.items():
        img = cached(name)
        if (img.shape[1], img.shape[0]) != tuple(size):
            problems.append(f"{name}: {img.shape[1]}x{img.shape[0]}, not {size}")
    for p in problems:
        print("PROBLEM:", p)
    if not problems:
        print("checks: ok")
    return problems


def atlas_area(backdrop):
    area = sum(W * s["height"] for s in backdrop["tile_sets"].values())
    area += sum(s["size"][0] * s["size"][1] for s in backdrop["pieces"].values())
    area += sum(w * h for (w, h), _ in LOOKS.values())
    print(f"backdrop area: {area:,} px = {area / 2048 ** 2:.2f} pages of 2048x2048")
    return area


# =========================================================================== review

TIMES = [(5, "1 SQUALL LINE: SCUD AT THE EDGES"), (16.5, "1 THE DEMONSTRATION GUST (TELEGRAPH)"),
         (34, "2 RAIN WALL: THE FIRST LAMPREYS"), (61, "2 THE FIRST SONDE IN THE RAIN"),
         (68, "2 SPORE BOMBERS UNDER THE BANKS"), (87.05, "3 THE T=87 FLASH: BANKS LIT, SEEN THROUGH"),
         (101.5, "3 THE CANISTER UNDER ITS BANK (GLINT)"), (103.55, "3 THE CANISTER IN A FLASH"),
         (127, "4 STORM WALL: MEDIUM"), (138, "4 THE HEAVY PEAK, CENTRE LANE CLEAR"),
         (149.5, "4/5 INTO THE EYE: WALL FADING IN"), (158, "5 THE EYE: CALM SEA, SHAFT"),
         (172, "5 THE EYE, THE WALL TURNED 66 DEG"), (178, "6 BREAKOUT: BACK INTO THE RAIN"),
         (202, "6 THE SKY LIGHTENING AT THE TOP")]


def review(backdrop):
    items = []
    for name, spec in backdrop["tile_sets"].items():
        items.append((f"{name} {spec['layer']} 480x{spec['height']}", load(name).resize((160, 320), Image.BOX), False))
    for name, spec in backdrop["pieces"].items():
        im = load(name)
        scale = 0.5 if max(im.size) > 300 else 1.0
        items.append((f"{name} {spec['layer']} {im.width}x{im.height}", im.resize((int(im.width * scale), int(im.height * scale)), Image.BOX), False))
    for name, ((w, h), _) in LOOKS.items():
        im = load(name)
        items.append((f"{name} {w}x{h} (look)", im.resize((w // 2, h // 2), Image.BOX), name != "eye-wall"))
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
    sheet = raster.sheet(width, big_y + 540 + 60, "LEVEL 12 BACKDROP - FINAL (PROPOSAL)",
                         "PRODUCTION ART, M5 PART F BATCH - R34; STRAIGHT FROM SCENE-STORM-R10-A ON LEVEL 11'S OCEAN KIT; "
                         "COMPOSITES AS THE GAME IS TO DRAW THEM (STAND-INS FOR THE RAIN, FLASH AND UNITS), 1/2 SCALE")
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
        img = composite(backdrop, t)
        if abs(t - 16.5) < 0.01:
            img = gust_overlay(img, t)
        sheet.alpha_composite(img.convert("RGBA").resize((240, 270), Image.BOX), (px, py + 14))
        raster.draw_text(sheet, px, py, f"T={t:g} {label}", raster.LABEL)
    for k, (t, label) in enumerate([(103.55, "T=103.55 AT 1X: THE FLASH OVER THE CANISTER'S BANK"),
                                    (138, "T=138 AT 1X: THE WALL'S HEAVY PEAK"),
                                    (160, "T=160 AT 1X: THE EYE (LOOK A), THE WALL TURNED 30 DEG")]):
        px = 16 + k * (480 + 24)
        sheet.alpha_composite(composite(backdrop, t).convert("RGBA"), (px, big_y + 14))
        raster.draw_text(sheet, px, big_y, label, raster.LABEL)
    REVIEW.parent.mkdir(parents=True, exist_ok=True)
    sheet.convert("RGB").save(REVIEW, optimize=True)
    frames = gif_frames(backdrop)
    gif = REVIEW.with_suffix(".gif")
    artkit.write_gif(frames, gif, fps=10)
    print(f"review: {REVIEW.relative_to(ROOT)}, {gif.relative_to(ROOT)} {gif.stat().st_size / 1e6:.1f} MB")


def gif_frames(backdrop):
    """The canister's two flashes (99-105 s) and the way into the eye (146.5-155.5 s), level time = real
    time, at 10 fps."""
    out = []
    for t in list(np.arange(99.0, 104.6, 0.1)) + list(np.arange(146.5, 155.5, 0.1)):
        out.append(composite(backdrop, float(t)).resize((360, 405), Image.BOX))
    return out


def load(name):
    return Image.open(OUT / f"{name}.png").convert("RGBA")


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
    names = set(backdrop["tile_sets"]) | set(backdrop["pieces"]) | set(LOOKS)
    missing = names - set(GENERATORS)
    if missing or wanted - set(GENERATORS):
        raise SystemExit(f"no generator for {sorted(missing | (wanted - set(GENERATORS)))}")
    if "--check" in argv:
        atlas_area(backdrop)
        sys.exit(1 if run_checks(backdrop) else 0)
    if "--review" not in argv:
        OUT.mkdir(parents=True, exist_ok=True)
        todo = jobs(backdrop, wanted)
        todo.sort(key=lambda j: -(j[1][0] * j[1][1]))
        with ProcessPoolExecutor() as pool:
            for job, img in zip(todo, pool.map(render, todo)):
                write(job, img)
        if not wanted:
            for f in OUT.glob("*.png"):
                if f.stem not in names:
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
