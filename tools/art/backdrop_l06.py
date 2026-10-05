#!/usr/bin/env python3
"""Production art PROPOSAL: the Level 06 backdrop, Daedalus Rim on the Moon's far side
(design/campaign/act-1-first-contact/level-06-farside; M4 part F batch, concept round 23).

Built the way backdrop_l04.py and backdrop_l05.py work and on backdrop_l04.py's helpers (imported
unchanged): the same kit, regolith ramp and periodic base terrain, no deep layer, nothing lit
rotated or mirrored (domes and dishes are turned in the model).

**The far side is lit at runtime.** The game multiplies a light map over the ground layer and the
ground units (design/campaign Level 06, Darkness rules): near-black ambient, warm pools at the
domes and rail lamps, the flares and the headlight. These images are therefore the ground *as a
lamp shows it*: soft, low-contrast relief under a high light (no long sun shadows, no earthshine:
Earth never rises here), the domes' warm windows and open airlocks, the Vrell growth's teal glow.
Only section 1 keeps the sun: its big craters' rims catch the last grazing light (strong relief and
long shadows on those features alone, so every tile still meets the next at its seam).

Outputs (assets/backdrop/level-06/, one PNG per tile set and set piece of the level's `backdrop`
block, named by its id and checked against its size):
  terminator                        ground, section 1: the shared terrain with big craters whose
                                    rims catch the last sunlight
  dome-field                        ground, sections 2 and 5: settlement regolith, low relief
  array-field                       ground, section 3: cratered regolith round the dish arrays
  vrell-field                       ground, section 4: teal Vrell roots in the regolith
  (all four carry the ore rail at x 410 on the same rows, so it runs on over every seam)
  plumes-light/-medium/-heavy       low-air: Level 04's regolith plumes, darkened as the chosen
                                    scene's (the low-air layer is not darkened at runtime)
  ejecta-streaks                    high-air: Level 04's ejected rock, dimmed (no sun to catch)
  dome-a, dome-b, dome-c            ground: Daedalus Rim's mining domes, warm windows lit, airlocks
                                    open, a pool of light spilling from each open door
  dome-overgrown, dome-broken       ground, section 4: a dome under Vrell growth, a collapsed dome
  nest-a, nest-b                    ground, section 4: Vrell nests, a teal-glowing core with
                                    luminous tendrils
  dish-big, dish-array              ground, section 3: the radio-silent observatory dishes
  dark-crater                       ground, section 3: the deep crater of the survey cache, cut
                                    from the terrain (outside the crater it is the tile under it,
                                    so it is transparent there)
  landing-pad                       ground, section 5: Level 04's pad, rendered here (empty)
  daedalus-gate                     ground, section 5: the settlement's pressure wall across the
                                    field, the main airlock wide open and lit (the data core's
                                    terminal stands in its mouth), the freight lock over the rail
  rail-lamp                         ground: a lamp mast beside the rail (the runtime light pool
                                    is its `darkness.lights` entry)
  boulder, boulder-growth-a/-b      ground: Level 04's pieces (its generators, the same images);
                                    the growth sockets are turret bases
  design/campaign/.../level-06-farside/concept/backdrop-final-r23-a.png   review sheet: the
                                    pieces, and composites of the level, each raw and under a
                                    preview of the runtime light map

**Data.** The backdrop block comes from the level's data.yaml once it has one, otherwise from
backdrop-proposal.yaml next to it (written by --proposal, with the suggested static
`darkness.lights` and the ore cart's, survey cache's and data core terminal's positions); the
sections (their ends and tile sets) from data.yaml, otherwise the L06 README's Layout. Changing a
section's start time moves a seam: rerun, the seam check fails when a feature reaches one.

**Seams.** As in Levels 04-05: one periodic base terrain (each noise octave's lattice divides
960), every tile set's features keep CLEAR px from the rows where its seams fall, one palette for
all ground tiles; --check (run after every render) scores the wrap rows and compares the rows at
every set border as game/.../BackdropSeamsTest does.

Run: python3 tools/art/backdrop_l06.py [--review | --check | --proposal] [id ...]   (~3 min on 20 cores)
"""
import functools
import sys
from concurrent.futures import ProcessPoolExecutor

import numpy as np
import yaml
from PIL import Image, ImageDraw

import artkit
from artkit import DESIGN, ROOT, raster, sprite

import backdrop_l04 as b4  # noqa: E402  (the Level 04 generator, imported unchanged)
import scenes_r06 as s6  # noqa: E402
from render import enemy_models as em  # noqa: E402
from render.scene_models import PaletteShim  # noqa: E402
from render.sdf import (rotate_z, sd_box, sd_capsule, sd_cylinder_x, sd_cylinder_z, sd_ellipsoid,  # noqa: E402
                        sd_sphere, union)
from render.station import ACCENT, DARK, GLASS, HULL, WARM  # noqa: E402

l01 = b4.l01
SCRIPT = "backdrop_l06.py"
SOURCE = artkit.source_note(SCRIPT, "M4 part F batch (proposal)")
LEVEL_DIR = DESIGN / "campaign" / "act-1-first-contact" / "level-06-farside"
PROPOSAL = LEVEL_DIR / "backdrop-proposal.yaml"
OUT = ROOT / "assets" / "backdrop" / "level-06"
REVIEW = LEVEL_DIR / "concept" / "backdrop-final-r23-a.png"
W, H = b4.W, b4.H
MID, SCREEN = 270, 540
SPEED = 130.0
RAIL_X = 410                 # the ore rail's centre
GREYS = b4.GREYS
CLEAR = b4.CLEAR             # px a feature keeps from a seam row
SUN_STEPS = 60               # the terminator rims' long shadow, px
LIT_GLASS = PaletteShim(l01.KIT_PAL, {("EARTH ORBIT", 3): (196, 150, 92)})   # glass lit from inside
DARK_GLASS = PaletteShim(l01.KIT_PAL, {("EARTH ORBIT", 3): (70, 76, 92)})

# The L06 README's Layout until the data file has sections: (name, end, atmosphere, tiles, peak).
DEFAULT_SECTIONS = [
    {"name": "Terminator", "end": 25, "atmosphere": "clear", "tiles": ["terminator"]},
    {"name": "Silent Domes", "end": 70, "atmosphere": "clear", "tiles": ["dome-field"]},
    {"name": "Observatory Array", "end": 110, "atmosphere": "light", "tiles": ["array-field", "ejecta-streaks"]},
    {"name": "Vrell Fields", "end": 160, "atmosphere": "medium",
     "peak": {"atmosphere": "heavy", "from": 140, "to": 146}, "tiles": ["vrell-field"]},
    {"name": "Daedalus Gate", "end": 190, "atmosphere": "light", "tiles": ["dome-field"]},
]


# --------------------------------------------------------------------------- the level's geometry

def level_data():
    """The sections' times and atmospheres from data.yaml (the README's Layout without one); their
    tile sets are this generator's (by section index) until data.yaml names them; the backdrop block
    from data.yaml once it is this generator's (no `images` borrowed from another level), otherwise
    from backdrop-proposal.yaml."""
    data_file = LEVEL_DIR / "data.yaml"
    data = (yaml.safe_load(data_file.read_text(encoding="utf-8")) if data_file.exists() else None) or {}
    sections = data.get("sections") or DEFAULT_SECTIONS
    if len(sections) != len(DEFAULT_SECTIONS):
        raise SystemExit(f"data.yaml has {len(sections)} sections, the generator {len(DEFAULT_SECTIONS)}")
    sections = [dict(s, tiles=s["tiles"] if any(t in GROUND_TILES for t in s.get("tiles", [])) else d["tiles"])
                for s, d in zip(sections, DEFAULT_SECTIONS)]
    backdrop = data.get("backdrop")
    ours = backdrop is not None and "images" not in backdrop and set(GROUND_TILES) <= set(backdrop["tile_sets"])
    if not ours:
        backdrop = yaml.safe_load(PROPOSAL.read_text(encoding="utf-8"))["backdrop"] if PROPOSAL.exists() else None
    return {"sections": sections, "scroll_speed": data.get("scroll_speed", SPEED), "backdrop": backdrop,
            "from_data": ours, "data": data}


GROUND_TILES = ("terminator", "dome-field", "array-field", "vrell-field")
LEVEL = level_data()
SECTIONS = LEVEL["sections"]
STARTS = [0.0] + [s["end"] for s in SECTIONS[:-1]]


def speed(i):
    return SECTIONS[i].get("speed", LEVEL["scroll_speed"])


def scroll_at(t):
    """LevelData.scrollAt: the ground layer's scroll at t, px."""
    s = 0.0
    for i, sec in enumerate(SECTIONS):
        if t < sec["end"] or i == len(SECTIONS) - 1:
            return s + (t - STARTS[i]) * speed(i)
        s += (sec["end"] - STARTS[i]) * speed(i)


def t_at(scroll):
    s = 0.0
    for i, sec in enumerate(SECTIONS):
        span = (sec["end"] - STARTS[i]) * speed(i)
        if scroll < s + span or i == len(SECTIONS) - 1:
            return STARTS[i] + (scroll - s) / speed(i)
        s += span


SEAM = [None] + [round(scroll_at(STARTS[i]) + SCREEN) for i in range(1, len(SECTIONS))]
TILE_OF = [next(t for t in s["tiles"] if t in GROUND_TILES) for s in SECTIONS]


def seams_of(tile):
    """u of the seams next to a ground tile set's sections."""
    out = set()
    for i, t in enumerate(TILE_OF):
        if t != tile:
            continue
        if i > 0 and TILE_OF[i - 1] != tile:
            out.add(SEAM[i] % H)
        if i + 1 < len(TILE_OF) and TILE_OF[i + 1] != tile:
            out.add(SEAM[i + 1] % H)
    return tuple(sorted(out))


def section_of(pos):
    return max(j for j in range(len(SECTIONS)) if j == 0 or pos >= SEAM[j])


# --------------------------------------------------------------------------- terrain

def clear_features(seed, count, radii, clear, seams):
    """Big craters (height field and centres) that keep ``clear`` px beyond their rims from every seam."""
    rng = np.random.default_rng(seed)
    f = np.zeros((H, W))
    centres = []
    for _ in range(count * 6):
        if len(centres) == count:
            break
        x, y, r = rng.uniform(60, W - 60), rng.uniform(0, H), rng.uniform(*radii)
        if b4.seam_distance(y, seams) <= 1.4 * r + clear:
            continue
        if any(np.hypot(x - cx, s6.periodic_dist(y, cy, H)) < r + cr + 20 for cx, cy, cr in centres):
            continue
        if abs(x - RAIL_X) < r * 1.4 + 26:
            continue
        f += b4.crater(f, x, y, r, depth=0.7)
        centres.append((x, y, r))
    return f, centres


def rail_bed():
    xx = np.arange(W)[None, :].astype(float)
    return np.clip(1 - (np.abs(xx - RAIL_X) - 15) / 5, 0, 1)


@functools.lru_cache
def tile_heights():
    """Height field of every ground tile set (H x W, row 0 = u 959) and the terminator's sun features."""
    base = b4.base_height()
    out = {}
    sun, centres = clear_features(1601, 4, (44, 70), CLEAR + SUN_STEPS, seams_of("terminator"))
    out["terminator"] = base + b4.features(1602, 6, (10, 20), 18, seams_of("terminator")) + sun
    out["dome-field"] = base + b4.features(1611, 4, (10, 20), 18, seams_of("dome-field"))
    out["array-field"] = base + b4.features(1621, 9, (14, 32), 26, seams_of("array-field"))
    out["vrell-field"] = base + b4.features(1631, 10, (14, 36), 24, seams_of("vrell-field"))
    bed = rail_bed()
    for k in out:
        out[k] = out[k] * (1 - bed) + (out[k] * 0.3 + 0.1) * bed
    return out, sun, tuple(centres)


def shade(h, sun=None):
    """The lamp-lit regolith: grey ramp by height, a soft high light from the top left, no cast
    shadows. With ``sun`` (a feature height field, 0 elsewhere) the grazing sunlight on those
    features: strong relief, long shadows, bright rims; where ``sun`` is 0 the factor is 1."""
    base = b4.base_height()
    lo, hi = base.min() - 0.15, base.max() + 0.1
    sh = s6.hillshade(h * 60, 0.32)
    t = np.clip((h - lo) / (hi - lo), 0, 1)
    col = s6.ramp_img(GREYS, 0.3 + t * 0.48) * sh[..., None] * 0.86
    if sun is not None:
        hs = s6.hillshade(sun * 60, 1.3)
        lit = s6.cast_shadows(sun * 60, steps=SUN_STEPS, drop=0.7)
        factor = np.clip(hs, 0.3, 1.6) * (0.3 + 0.7 * lit)
        glare = np.clip(hs - 1.12, 0, 1) * lit
        col = col * factor[..., None] + np.array((255, 236, 206)) * (glare * 0.9)[..., None]
    return col


def dithered(col, rows):
    """A Bayer offset with the phase of the tile rows ``rows`` before the shared median cut."""
    b = l01.BAYER4[np.asarray(rows)[:, None] % 4, np.arange(col.shape[1])[None, :] % 4]
    return np.clip(col + ((b - 0.5) * 6)[..., None], 0, 255)


def rail_overlay():
    """The ore rail: close-set sleepers, two rails with a lit edge, a junction plate every 240 px;
    rows repeat every 8 px and 240 px, both dividing 960, so the overlay is the same in every
    tile set and runs on over every seam."""
    rail = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    rd = ImageDraw.Draw(rail)
    x0 = RAIL_X
    for y in range(0, H, 8):
        rd.rectangle([x0 - 11, y, x0 + 11, y + 2], fill=(74, 68, 66, 255))
    for dx in (-6, 6):
        rd.rectangle([x0 + dx - 1, 0, x0 + dx + 1, H - 1], fill=(128, 128, 136, 255))
        rd.line([x0 + dx - 1, 0, x0 + dx - 1, H - 1], fill=(188, 188, 196, 255))
    for u in range(100, H, 240):
        y = H - 1 - u
        rd.rectangle([x0 - 13, y - 3, x0 + 13, y + 3], fill=(96, 92, 92, 255), outline=(52, 50, 54, 255))
    out = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    out.alpha_composite(s6.pshadow(rail, 0.5, 0.7), (2, 3))
    out.alpha_composite(rail)
    return out


ROOTS = ((70, 150, 70), (250, 330, 46), (330, 880, 60), (110, 640, 40), (190, 60, 50))   # (x, row, length)


def roots_overlay():
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    for (x, y, length), seed in zip(ROOTS, range(1641, 1650)):
        for dy in (-H, 0, H):
            b4.roots(img, (x, y + dy), seed, 5, length, 2.6)
    a = np.array(img)
    a[:, RAIL_X - 16:RAIL_X + 17] = 0            # the rail stays clear (its bed is kept)
    img = Image.fromarray(a, "RGBA")
    return b4.assert_clear(img, seams_of("vrell-field"), "vrell-field roots")


# --------------------------------------------------------------------------- the dark crater (cut from the terrain)

MARGIN = 8           # rows beyond a window (the lamp-lit shading looks 1 px around)


def window(bottom, height):
    """Layer positions (top row first, ``MARGIN`` rows beyond each end), tile rows, tile names and heights."""
    pos = np.arange(bottom + height - 1 + MARGIN, bottom - 1 - MARGIN, -1)
    hs, _, _ = tile_heights()
    rows = H - 1 - np.mod(pos, H)
    names = np.array([TILE_OF[section_of(p)] for p in pos], object)
    h = np.stack([hs[n][r] for n, r in zip(names, rows)])
    return pos, rows, names, h


DARK_CRATER = {"size": (200, 200), "r": 64}


def crater_spec(backdrop):
    p = next(p for p in backdrop["placed"] if p["piece"] == "dark-crater")
    w, h = backdrop["pieces"]["dark-crater"]["size"]
    centre = scroll_at(p["t"]) + MID
    return int(round(centre - h / 2)), w, h, p["x"]


def crater_window(backdrop):
    """The dark crater's colours over its window rows (full width), before quantization, and the mask
    where it differs from the terrain."""
    bottom, w, h, x = crater_spec(backdrop)
    pos, rows, names, hgt = window(bottom, h)
    if len(set(names)) > 1:
        raise SystemExit("dark-crater: its window crosses a seam; move it")
    r = DARK_CRATER["r"]
    cy = pos[0] - (bottom + h / 2)            # its centre's row in the window
    extra = np.zeros_like(hgt)
    yy, xx = np.mgrid[0:len(pos), 0:W]
    d = np.hypot(xx - x, yy - cy) / r
    extra += np.where(d < 1, -(1 - d * d) * 1.2, 0) + np.exp(-((d - 1) / 0.13) ** 2) * 0.32
    rng = np.random.default_rng(1661)
    for _ in range(18):                          # slump blocks on the floor and the walls
        a, rr = rng.uniform(0, 2 * np.pi), rng.uniform(0.1, 0.85) * r
        bx, by, br = x + rr * np.cos(a), cy + rr * np.sin(a), rng.uniform(2, 5)
        extra += 0.14 * np.exp(-((xx - bx) ** 2 + (yy - by) ** 2) / (2 * br * br))
    extra *= d < 1.45
    col = shade(hgt + extra)
    floor = np.clip(1 - d / 0.9, 0, 1)            # the floor never sunlit: darker regolith
    col *= (1 - 0.2 * floor)[..., None]
    col = dithered(col, rows)
    keep = slice(MARGIN, MARGIN + h)
    return col[keep], (d < 1.45)[keep], rows[keep], names[keep][0]


# --------------------------------------------------------------------------- the ground group

@functools.lru_cache
def ground_group():
    """Every ground tile set and the dark crater's window, quantized to one terrain palette; then the
    rail and the roots over the tiles (each from a small palette of its own)."""
    hs, sun, _ = tile_heights()
    allrows = np.arange(H)
    cols = {n: dithered(shade(hs[n], sun if n == "terminator" else None), allrows) for n in GROUND_TILES}
    backdrop = LEVEL["backdrop"]
    crater = "dark-crater" in backdrop["pieces"]
    if crater:
        ccol, cmask, crows, ctile = crater_window(backdrop)
        cols["dark-crater"] = ccol
    names = list(cols)
    quant = dict(zip(names, artkit.quantize_set(
        [l01.rgba(cols[n], np.full(cols[n].shape[:2], 255)) for n in names], 24)))
    rail = artkit.quantize_set([rail_overlay()], 8)[0]
    for n in GROUND_TILES:
        quant[n] = b4.overlaid(quant[n], rail)
    quant["vrell-field"] = b4.overlaid(quant["vrell-field"], artkit.quantize_set([roots_overlay()], 8)[0])
    if crater:
        bottom, w, h, x = crater_spec(backdrop)
        tile = np.array(quant[ctile])[crows]
        a = np.array(quant["dark-crater"])
        a[..., 3] = np.where(cmask & np.any(a[..., :3] != tile[..., :3], axis=-1), 255, 0)
        left = int(round(x - w / 2))
        a = a[:, left:left + w]
        a[a[..., 3] == 0] = 0
        quant["dark-crater"] = b4.border_clear(Image.fromarray(a, "RGBA"))
    return quant


# --------------------------------------------------------------------------- domes

def mining_dome(R, locks, broken=False):
    """A mining dome: a ribbed glass hemisphere (lit from inside: warm) on a ring base with lit
    windows, and an airlock module at every angle of ``locks`` (degrees, 0 = right, anticlockwise)
    with its outer door slid open on a lit bay. ``broken``: the shell caved in, the lights dead."""
    rng = np.random.default_rng(int(R * 10) + len(locks))
    holes = [(rng.uniform(-R * 0.6, R * 0.6), rng.uniform(-R * 0.6, R * 0.6), rng.uniform(R * 0.25, R * 0.45))
             for _ in range(5)]
    panels = [(rng.uniform(0, 2 * np.pi), rng.uniform(0, R * 0.7), rng.uniform(3, 6), rng.uniform(2, 4))
              for _ in range(7)]

    def scene(p):
        r = np.linalg.norm(p, axis=1)
        dome = np.maximum(sd_sphere(p, (0, 0, 0), R), -p[:, 2])
        shell = np.maximum(np.abs(r - R) - 0.9, -p[:, 2])
        ribs = []
        for k in range(4):
            q = rotate_z(p, k * np.pi / 4)
            ribs.append(np.maximum(shell - 0.4, np.abs(q[:, 1]) - 0.7))
        ribs.append(np.maximum(shell - 0.4, np.abs(p[:, 2] - R * 0.55) - 0.7))
        if broken:
            for hx, hy, hr in holes:
                dome = np.maximum(dome, -sd_cylinder_z(p, (hx, hy, R * 0.6), hr, R))
            dome = np.maximum(dome, -sd_sphere(p, (0, 0, 0), R - 1.6))       # a shell, open to the dark inside
            items = [(dome, GLASS), (np.minimum.reduce(ribs), DARK),
                     (sd_cylinder_z(p, (0, 0, 0), R - 2, 1.5), DARK)]
            for a, rr, pw, pl in panels:                                   # fallen panels on the floor
                q = rotate_z(p - np.array([rr * np.cos(a), rr * np.sin(a), 2.5]), a * 1.7)
                items.append((sd_box(q, (0, 0, 0), (pw, pl, 0.6), 0.2), GLASS))
        else:
            items = [(dome, GLASS), (np.minimum.reduce(ribs), HULL), (sd_sphere(p, (0, 0, R - 1.5), 2.6), WARM)]
        items.append((np.maximum(sd_cylinder_z(p, (0, 0, 0), R + 5, 3.2), -sd_cylinder_z(p, (0, 0, 0), R - 1, 5)), HULL))
        for k in range(16):                                                # windows round the base ring
            a = 2 * np.pi * (k + 0.5) / 16
            if any(abs(np.angle(np.exp(1j * (a - np.radians(L))))) < 0.3 for L in locks):
                continue
            items.append((sd_box(rotate_z(p, a), (R + 3.2, 0, 2.6), (1.2, 1.6, 1.0), 0.3),
                          DARK if broken else WARM))
        for L in locks:
            q = rotate_z(p, np.radians(L))      # anticlockwise by L (+y up)
            body = sd_box(q, (R + 10, 0, 4), (9, 7, 4.5), 1.2)
            bay = sd_box(q, (R + 14, 0, 4.8), (6.5, 4.6, 4.2), 0.4)          # the open outer door
            items.append((np.maximum(body, -bay), HULL))
            items.append((sd_box(q, (R + 13, 0, 0.8), (6, 4.4, 0.5), 0.2), DARK if broken else WARM))
            items.append((sd_box(q, (R + 16, 9.5, 4), (3.5, 1.4, 4), 0.4), ACCENT))      # the door leaf, slid aside
        return union(*items)
    size = 2 * R + 34
    return b4.render_model(scene, DARK_GLASS if broken else LIT_GLASS, (size, size), factor=4, colors=40)


def spill(img, x, y, angle, length, strength=0.26):
    """Lamplight spilling from an open door along ``angle`` (degrees, 0 = right, anticlockwise)."""
    a = np.radians(angle)
    return b4.light_pool(img, x + np.cos(a) * length * 0.55, y - np.sin(a) * length * 0.55, length * 0.6, strength)


def dome_cluster(w, h, layout, links=(), extras=()):
    """Domes (R, x, y, locks) with light pools, linking tunnels and kit parts; long shadows short
    (a lamp is above, not a low sun)."""
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    for (x0, y0, x1, y1) in links:
        d = ImageDraw.Draw(img)
        d.line([x0 + 2, y0 + 3, x1 + 2, y1 + 3], fill=(0, 0, 0, 110), width=9)
    for (x0, y0, x1, y1) in links:
        tunnel = link_tunnel(np.hypot(x1 - x0, y1 - y0), np.degrees(np.arctan2(-(y1 - y0), x1 - x0)))
        sprite.paste_center(img, tunnel, (x0 + x1) / 2, (y0 + y1) / 2)
    for (name, x, y) in extras:
        b4.place(img, kit_part(name), x, y, shadow=4, opacity=0.45)
    for (R, x, y, locks) in layout:
        b4.place(img, mining_dome(R, tuple(locks)), x, y, shadow=5, opacity=0.45)
    return b4.finish(img)


@functools.lru_cache
def link_tunnel(length, angle):
    """A pressurised walkway between two domes, turned in the model."""
    def scene(p):
        q = rotate_z(p, np.radians(angle))
        return union((sd_cylinder_x(q, (0, 0, 2), 4.2, length / 2), HULL),
                     *[(sd_box(q, (x, 0, 2), (0.8, 4.8, 4.8), 0.3), DARK) for x in np.arange(-length / 2 + 6, length / 2 - 4, 9)])
    s = int(length + 16)
    return b4.render_model(scene, l01.KIT_PAL, (s, s), factor=4, colors=24)


@functools.lru_cache
def kit_part(name):
    from render import station
    return {"cargo": lambda: station.render_part(station.cargo_part(), l01.KIT_PAL),
            "radiator": lambda: station.render_part(station.radiator_part(48, 2), l01.KIT_PAL),
            "dish": lambda: l01.kit()["dish"],
            "hopper": ore_hopper,
            "lamp": b4.lamp_post}[name]()


def ore_hopper():
    """An ore hopper on legs over a conveyor stub (the mines' output)."""
    def scene(p):
        return union((sd_box(p, (0, 0, 8), (9, 9, 4), 1.2), ACCENT), (sd_box(p, (0, 0, 12.5), (6, 6, 0.6), 0.3), DARK),
                     *[(sd_cylinder_z(p, (sx * 8, sy * 8, 0), 1.2, 8), DARK) for sx in (-1, 1) for sy in (-1, 1)],
                     (sd_box(p, (0, -15, 2), (2.5, 9, 1.5), 0.4), HULL))
    return b4.render_model(scene, l01.KIT_PAL, (30, 44), factor=5, colors=24)


def dome_a(w, h):
    return dome_cluster(w, h, [(32, 64, 74, (300,)), (20, 140, 44, (20,))], links=[(94, 62, 122, 50)],
                        extras=[("hopper", 140, 112), ("cargo", 158, 128)])


def dome_b(w, h):
    return dome_cluster(w, h, [(26, 52, 52, (250,)), (26, 120, 86, (330, 160))], links=[(74, 62, 98, 76)],
                        extras=[("radiator", 44, 116), ("dish", 140, 26)])


def dome_c(w, h):
    return dome_cluster(w, h, [(40, 76, 64, (290, 30))], extras=[("cargo", 22, 116), ("cargo", 40, 120)])


def dome_overgrown(w, h):
    """A dome under Vrell growth: a resin stain round it, roots across the glass, a mass of bulbs
    swallowing its airlock (the lights still on inside)."""
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    yy, xx = np.mgrid[0:h, 0:w]
    tex = raster.fbm(w, h, 14, 1671, octaves=3, period=False)
    d = np.hypot(xx - w / 2, (yy - h / 2) * 1.1) / (min(w, h) * 0.47)
    cover = np.clip((1 - d) * 2.4 + (tex - 0.5), 0, 1)
    stain = s6.ramp_img(GREYS, 0.2 + 0.15 * tex) * 0.55 + np.array(b4.CHITIN, float) * 0.25
    img = l01.rgba(stain, artkit.ordered_dither(cover * 0.8, 3) * 255)
    img = b4.light_pool(img, w / 2, h / 2, 46, 0.14)
    b4.place(img, mining_dome(32, (230,)), w / 2, h / 2 - 4, shadow=5, opacity=0.45)
    b4.roots(img, (w / 2 - 40, h / 2 + 30), 1672, 5, 54, 2.6)
    b4.roots(img, (w / 2 + 30, h / 2 - 34), 1673, 4, 40, 2.2)
    b4.place(img, growth_mass(1674, (52, 46)), w / 2 - 36, h / 2 + 34, shadow=4, opacity=0.4)
    return b4.finish(img)


def dome_broken(w, h):
    """The collapsed dome of the heavy peak: shell caved in, the lights dead, dust and shards round it."""
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    yy, xx = np.mgrid[0:h, 0:w]
    tex = raster.fbm(w, h, 8, 1681, octaves=3, period=False)
    d = np.hypot(xx - w / 2, yy - h / 2) / (min(w, h) * 0.46)
    dust = np.clip((1 - d) * 2 + (tex - 0.5) * 1.2, 0, 1)
    img = l01.rgba(s6.ramp_img(GREYS, 0.5 + 0.2 * tex), artkit.ordered_dither(dust * 0.7, 3) * 255)
    b4.place(img, mining_dome(34, (120,), broken=True), w / 2, h / 2, shadow=5, opacity=0.45)
    rng = np.random.default_rng(1682)
    dr = ImageDraw.Draw(img)
    for _ in range(26):                      # glass shards thrown out of the collapse
        a, rr = rng.uniform(0, 2 * np.pi), rng.uniform(38, min(w, h) * 0.46)
        x, y = w / 2 + rr * np.cos(a), h / 2 + rr * np.sin(a)
        dr.line([x, y, x + rng.uniform(-3, 3), y + rng.uniform(-3, 3)], fill=(120, 128, 146, 255), width=1)
    return b4.finish(img)


# --------------------------------------------------------------------------- Vrell nests

def growth_mass(seed, size):
    """A clump of glossy violet bulbs with teal seams and glowing pores."""
    rng = np.random.default_rng(seed)
    w, h = size
    blobs = [(rng.uniform(-w * 0.28, w * 0.28), rng.uniform(-h * 0.28, h * 0.28), rng.uniform(5, 9)) for _ in range(9)]

    def scene(p):
        items = [(sd_ellipsoid(p, (x, y, 2), (r, r * 0.85, r * 0.6)), em.V_BODY) for x, y, r in blobs]
        items += [(sd_sphere(p, (x, y, 2 + r * 0.5), r * 0.32), em.V_GLOW) for x, y, r in blobs[::2]]
        return union(*items, k=2.5)
    return b4.vrell(scene, size)


def nest_scene(seed):
    """A Vrell nest: a ring of swollen bulbs round a glowing teal well, ribs between them."""
    rng = np.random.default_rng(seed)

    def scene(p):
        items = [(np.maximum(sd_ellipsoid(p, (0, 0, 0), (44, 40, 14)), -sd_ellipsoid(p, (0, 0, 10), (24, 22, 12))),
                  em.V_BODY),
                 (sd_ellipsoid(p, (0, 0, 0), (22, 20, 5)), em.V_SAC),
                 (sd_sphere(p, (0, 0, 1), 7), em.V_GLOW)]
        items += [(sd_sphere(p, (14 * np.cos(a), 13 * np.sin(a), 3), 2.2), em.V_GLOW)
                  for a in np.linspace(0.4, 2 * np.pi + 0.4, 6, endpoint=False)]
        for a in np.linspace(0, 2 * np.pi, 8, endpoint=False) + rng.uniform(0, 0.5):
            r = rng.uniform(9, 14)
            items.append((sd_ellipsoid(p, (48 * np.cos(a), 44 * np.sin(a), 3), (r, r * 0.85, r * 0.7)), em.V_BODY))
        for a in np.linspace(0.2, 2 * np.pi + 0.2, 6, endpoint=False):
            q = rotate_z(p, -a)
            items.append((sd_box(q, (32, 0, 9), (10, 1.8, 3), 1.2), em.V_DARK))
        return union(*items, k=3.5)
    return scene


def nest(w, h, seed):
    """The nest with its luminous tendrils spreading across a resin stain (kept inside the piece)."""
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    yy, xx = np.mgrid[0:h, 0:w]
    tex = raster.fbm(w, h, 16, seed + 1, octaves=3, period=False)
    d = np.hypot((xx - w / 2) / (w * 0.48), (yy - h / 2) / (h * 0.48))
    cover = np.clip((1 - d) * 2.2 + (tex - 0.5), 0, 1)
    stain = s6.ramp_img(GREYS, 0.18 + 0.12 * tex) * 0.5 + np.array(b4.CHITIN, float) * 0.22
    img = l01.rgba(stain, artkit.ordered_dither(cover * 0.85, 3) * 255)
    inside = d < 0.93
    b4.roots(img, (w / 2, h / 2), seed + 2, 7, min(w, h) * 0.36, 3.2, limit=inside)
    glow = b4.light_pool(Image.new("RGBA", (w, h), (0, 0, 0, 0)), w / 2, h / 2, 70, 0.3, colour=b4.TEAL)
    img.alpha_composite(glow)
    body = b4.vrell(nest_scene(seed + 3), (112, 104), factor=3, glow=1.6)
    b4.place(img, body, w / 2, h / 2, shadow=6, opacity=0.45)
    return b4.finish(img)


# --------------------------------------------------------------------------- the observatory

def dish_scene(R, feed=True, turn=0.0):
    """A radio dish pointing at the zenith: the bowl with its panel rings, a feed tripod and the
    receiver cabin over its focus, on a mount ring."""
    def scene(p):
        q = rotate_z(p, -turn)
        bowl = sd_sphere(q, (0, 0, -R * 0.6), R)
        cut = sd_sphere(q, (0, 0, R * 0.78), R * 1.16)
        d = np.maximum(bowl, -cut)
        rings = np.maximum(d - 0.3, np.abs((np.hypot(q[:, 0], q[:, 1]) % (R / 4)) - R / 8) - 0.5)
        items = [(d, HULL), (rings, DARK), (sd_cylinder_z(q, (0, 0, -2), R * 0.35, 2), DARK)]
        if feed:
            top = (0, 0, R * 0.62)
            for k in range(3):
                a = 2 * np.pi * k / 3 + np.pi / 2
                items.append((sd_capsule(q, (R * 0.8 * np.cos(a), R * 0.8 * np.sin(a), R * 0.16), top, 0.9), DARK))
            items.append((sd_box(q, top, (R * 0.1 + 1.5, R * 0.1 + 1.5, 2), 0.6), ACCENT))
        return union(*items)
    return scene


def dish_big(w, h):
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    img = b4.paved(w, h, 18, 18, w - 19, h - 19, 1691)
    a = np.array(img).astype(float)
    yy, xx = np.mgrid[0:h, 0:w]
    a[..., 3] *= np.hypot(xx - w / 2, yy - h / 2) < w / 2 - 8            # a round concrete footing
    img = Image.fromarray(a.astype(np.uint8), "RGBA")
    dish = b4.render_model(dish_scene(46, turn=0.3), l01.KIT_PAL, (102, 102), factor=4, colors=40)
    b4.place(img, dish, w / 2, h / 2, shadow=7, opacity=0.5)
    b4.place(img, kit_part("lamp"), w - 26, h - 22, shadow=3)
    return b4.finish(img)


def dish_array(w, h):
    """Three dishes in a row on their footings, a cable trench linking them to a control hut."""
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    xs = (52, 136, 220)
    d.line([xs[0], 92, w - 30, 92], fill=(44, 44, 50, 255), width=4)
    d.line([xs[0], 91, w - 30, 91], fill=(84, 84, 92, 255), width=1)
    for x in xs:
        d.line([x, 60, x, 92], fill=(44, 44, 50, 255), width=3)
    for k, x in enumerate(xs):
        pad = b4.paved(64, 64, 4, 4, 59, 59, 1700 + k)
        sprite.paste_center(img, pad, x, 54)
        dish = b4.render_model(dish_scene(24, turn=0.4 + k), l01.KIT_PAL, (56, 56), factor=5, colors=32)
        b4.place(img, dish, x, 52, shadow=5, opacity=0.5)
    hut = b4.render_model(lambda p: union((sd_box(p, (0, 0, 5), (12, 9, 5), 1.2), HULL),
                                          (sd_box(p, (-4, -9.3, 6), (4, 0.6, 1.5), 0.2), WARM),
                                          (sd_cylinder_z(p, (7, 4, 10), 1, 6), DARK)),
                          l01.KIT_PAL, (32, 28), factor=5, colors=24)
    b4.place(img, hut, w - 32, 92, shadow=4, opacity=0.5)
    return b4.finish(img)


# --------------------------------------------------------------------------- Daedalus Gate

GATE_LOCK = (0, -32)       # the main airlock's mouth (model units from the piece's centre, +y up)
FREIGHT_X = RAIL_X - 240


def gate_scene(p):
    """The settlement's pressure wall across the field (a tunnel on footings), the main airlock jutting
    out of it with its doors slid wide open on a lit bay, the freight lock over the ore rail, domes behind."""
    items = [(sd_cylinder_x(p, (0, 34, 9), 14, 250), HULL),
             (sd_box(p, (0, 34, 22.5), (250, 3, 0.8), 0.3), DARK)]
    for x in range(-232, 240, 32):
        items.append((sd_box(p, (x, 34, 7), (2.4, 16, 8), 0.8), DARK))
        items.append((sd_box(p, (x + 16, 19.6, 6), (1.2, 0.6, 1.2), 0.2), WARM))      # windows along the wall
    lock = sd_box(p, (0, 4, 13), (46, 34, 13), 3)
    bay = sd_box(p, (0, -8, 22), (30, 30, 19), 1)                                   # the bay, open to the sky
    items.append((np.maximum(lock, -bay), HULL))
    items.append((sd_box(p, (0, -6, 3.4), (29, 28, 0.6), 0.2), DARK))
    for y in range(-30, 20, 9):
        items.append((sd_box(p, (0, y, 4.2), (26, 0.8, 0.4), 0.1), WARM))            # the lit bay floor
    for sx in (-1, 1):
        items.append((sd_box(p, (sx * 40, -30, 13), (8, 2.4, 12), 0.6), ACCENT))     # door leaves, slid aside
        items.append((sd_box(p, (sx * 22, -29, 26.2), (3, 3, 0.8), 0.3), WARM))     # beacons over the mouth
    fl = sd_box(p, (FREIGHT_X, 22, 11), (20, 22, 11), 2)
    fbay = sd_box(p, (FREIGHT_X, 8, 20), (13, 16, 18.5), 0.6)
    items.append((np.maximum(fl, -fbay), HULL))
    items.append((sd_box(p, (FREIGHT_X, 8, 2.8), (12, 14, 0.4), 0.1), DARK))
    items += [(sd_box(p, (FREIGHT_X + dx, 8, 3.4), (0.9, 15, 0.5), 0.1), HULL) for dx in (-6, 6)]     # the rail runs in
    items += [(sd_box(p, (FREIGHT_X + dx, y, 3.4), (1.2, 2.5, 0.4), 0.1), WARM) for dx in (-11, 11) for y in (-2, 8, 18)]
    for x, y, R in ((-150, 82, 32), (120, 84, 28)):
        r = np.linalg.norm(p - np.array([x, y, 0]), axis=1)
        items.append((np.maximum(sd_sphere(p, (x, y, 0), R), -p[:, 2]), GLASS))
        items.append((sd_cylinder_z(p, (x, y, 0), R + 4, 3), HULL))
        q = rotate_z(p - np.array([x, y, 0]), 0)
        items.append((np.maximum(np.maximum(np.abs(r - R) - 1.3, -p[:, 2]), np.abs(q[:, 0]) - 0.7), HULL))
        items.append((sd_sphere(p, (x, y, R - 1.5), 2.4), WARM))
    return union(*items)


GATE_BODY_H = 240          # the gate model's image height (its centre row 120 from the piece's top)


def daedalus_gate(w, h):
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    cx, cy = w / 2, GATE_BODY_H / 2
    mouth = cy + 38                                                                 # the airlock's mouth row
    apron = b4.paved(w, h, int(cx - 70), int(mouth - 6), int(cx + 70), h - 12, 1711)
    img.alpha_composite(apron)
    d = ImageDraw.Draw(img)
    for y in range(int(mouth + 12), h - 16, 18):                                    # lane marks out of the mouth
        d.rectangle([cx - 1, y, cx + 1, y + 8], fill=(214, 180, 60, 255))
    img = spill(img, cx, mouth, 270, 120, 0.3)
    body = b4.render_model(gate_scene, LIT_GLASS, (w, GATE_BODY_H), factor=3, colors=48)
    sprite.paste(img, b4.long_shadow(body, 6, 0.45), 0, 0)
    sprite.paste(img, body, 0, 0)
    for x in (cx - 82, cx + 82):
        b4.place(img, kit_part("lamp"), x, mouth + 14, shadow=3)
    out = np.array(b4.finish(img))
    out[:, 0], out[:, -1] = out[:, 1], out[:, -2]       # full width: the wall runs off both edges of the field
    out[0], out[-1] = 0, 0
    return Image.fromarray(out, "RGBA")


# --------------------------------------------------------------------------- small pieces

def rail_lamp(w, h):
    """A lamp mast beside the rail: the post, the arm and the warm lamp head over the track side."""
    def scene(p):
        return union((sd_cylinder_z(p, (-4, 0, 0), 1.8, 12), DARK), (sd_box(p, (0, 0, 12), (5.5, 1.3, 0.8), 0.3), HULL),
                     (sd_box(p, (4.5, 0, 11), (2.4, 2.4, 1.2), 0.5), HULL), (sd_box(p, (4.5, 0, 10), (1.8, 1.8, 0.6), 0.2), WARM))
    lamp = b4.render_model(scene, l01.KIT_PAL, (18, 12), factor=6, colors=16)
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    b4.place(img, lamp, w / 2, h / 2 - 2, shadow=4, opacity=0.5)
    return b4.finish(img, 16)


def dim(img, k, alpha=1.0):
    a = np.array(img).astype(float)
    a[..., :3] *= k
    a[..., 3] *= alpha
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


def dark_plumes(share, max_alpha, seed):
    """Level 04's plumes, darkened as the chosen far-side scene's (tinted to the night, 60 % alpha)."""
    def tile(w, h):
        img = s6.tinted(b4.plumes(share, max_alpha, seed)(w, h), (20, 22, 34), 0.75, 0.5)
        return artkit.quantize_set([r03_step(dim(img, 1.0, 0.6))], 12)[0]
    return tile


def r03_step(img):
    return b4.r03.step_alpha(img, 6)


def ejecta(w, h):
    return artkit.stepped_alpha(dim(b4.ejecta_streaks(w, h), 0.6), 9)


TILE_SETS = {"plumes-light": dark_plumes(0.06, 190, 601), "plumes-medium": dark_plumes(0.15, 205, 603),
             "plumes-heavy": dark_plumes(0.45, 235, 605), "ejecta-streaks": ejecta}
PIECES = {
    "dome-a": dome_a, "dome-b": dome_b, "dome-c": dome_c, "dome-overgrown": dome_overgrown,
    "dome-broken": dome_broken, "nest-a": lambda w, h: nest(w, h, 1721), "nest-b": lambda w, h: nest(w, h, 1731),
    "dish-big": dish_big, "dish-array": dish_array, "daedalus-gate": daedalus_gate, "rail-lamp": rail_lamp,
    "landing-pad": b4.landing_pad,
    "boulder": b4.PIECES["boulder"], "boulder-growth-a": b4.PIECES["boulder-growth-a"],
    "boulder-growth-b": b4.PIECES["boulder-growth-b"],
}
CUT = ("dark-crater",)


# --------------------------------------------------------------------------- the proposal (backdrop block and lights)

SIZES = {"dome-a": (180, 150), "dome-b": (170, 140), "dome-c": (150, 140), "dome-overgrown": (170, 150),
         "dome-broken": (170, 150), "nest-a": (240, 220), "nest-b": (220, 200), "dish-big": (140, 140),
         "dish-array": (280, 120), "dark-crater": DARK_CRATER["size"], "landing-pad": (240, 240),
         "daedalus-gate": (480, 280), "rail-lamp": (24, 24), "boulder": (64, 56), "boulder-growth-a": (72, 64),
         "boulder-growth-b": (72, 64)}
MID_SIZE = {"dome-a", "dome-b", "dome-c", "dome-overgrown", "dome-broken", "nest-a", "nest-b", "dish-big",
            "dish-array", "dark-crater", "landing-pad", "daedalus-gate"}
NOTES = {"dome-a": "mining domes, windows lit, airlocks open", "dome-b": "two domes, a walkway, a dish",
         "dome-c": "a big dome, two open airlocks", "dome-overgrown": "a dome under Vrell growth",
         "dome-broken": "the collapsed dome (heavy peak)", "nest-a": "Vrell nest, teal well, luminous tendrils",
         "nest-b": "a smaller nest", "dish-big": "the observatory's big dish",
         "dish-array": "three dishes, cable trench, control hut",
         "dark-crater": "the survey cache's crater, cut from the terrain",
         "landing-pad": "the empty landing pad (Level 04's)",
         "daedalus-gate": "the pressure wall, the main airlock open, the freight lock over the rail",
         "rail-lamp": "lamp mast beside the rail (its pool is a darkness light)",
         "boulder": "", "boulder-growth-a": "a Vrell socket on top: a turret's base", "boulder-growth-b": ""}
# (piece, t, x, comment); the pieces under the data's ground targets are placed from them: the dark
# crater under the survey cache, the landing pad under the pad mortars, the gate round the airlock
# terminal, the growth sockets under the dome turrets, a nest under the Vrell-field units.
PLACED = [
    ("boulder", 7, 120, "1. Terminator: the sunlit crater rims"), ("boulder", 19, 300, None),
    ("rail-lamp", 27, RAIL_X - 26, "2. Silent Domes: domes, the ore rail's lamps, the turrets' sockets between domes"),
    ("dome-a", 31, 120, None), ("rail-lamp", 33, RAIL_X - 26, None), ("rail-lamp", 39, RAIL_X - 26, None),
    ("dome-b", 40, 270, None), ("rail-lamp", 45, RAIL_X - 26, None), ("boulder-growth-a", 47.23, 130, None),
    ("boulder-growth-b", 47.83, 350, None), ("rail-lamp", 51, RAIL_X - 26, None), ("dome-c", 54, 110, None),
    ("rail-lamp", 57, RAIL_X - 26, None), ("rail-lamp", 63, RAIL_X - 26, None), ("dome-a", 64, 270, None),
    ("rail-lamp", 69, RAIL_X - 26, None),
    ("dish-array", 76, 170, "3. Observatory Array: the dishes, the dark crater (the survey cache)"),
    ("rail-lamp", 79, RAIL_X - 26, None), ("dish-big", 85, 300, None), ("rail-lamp", 93, RAIL_X - 26, None),
    ("dish-array", 99, 200, None), ("rail-lamp", 107, RAIL_X - 26, None), ("dark-crater", 111, 280, None),
    ("nest-b", 120.3, 240, "4. Vrell Fields: nests and tendrils between the domes"),
    ("rail-lamp", 121, RAIL_X - 26, None), ("dome-overgrown", 128, 110, None), ("nest-a", 134, 320, None),
    ("dome-broken", 147, 300, None), ("rail-lamp", 149, RAIL_X - 26, None), ("nest-a", 153, 120, None),
    ("dome-overgrown", 158.5, 320, None),
    ("landing-pad", 167.2, 240, "5. Daedalus Gate: the landing pad, the main airlock (the data core)"),
    ("rail-lamp", 163, RAIL_X - 26, None), ("rail-lamp", 169, RAIL_X - 26, None), ("dome-c", 174, 110, None),
    ("daedalus-gate", 182.25, 240, None), ("dome-b", 188.5, 120, None),
]


def light_t(pos, radius):
    """The `t` of a static light whose centre lies at layer position ``pos`` (it enters at the top edge
    with its centre ``radius`` above it)."""
    return round(t_at(pos - SCREEN - radius), 2)


def piece_pos(t):
    return scroll_at(t) + MID


def lights():
    """Static pools: on the domes, the lamps, the airlocks and the pad; section 1's sunlit rims."""
    out = []
    _, _, centres = tile_heights()
    for k in range(-1, 4):                        # section 1's tiles from layer position 0 to its seam
        for x, y, r in centres:
            pos = k * H + (H - 1 - y)
            t = light_t(pos, round(r * 1.6))
            if t >= 0 and pos + r < SEAM[1] - 60:      # (a light's t is not negative: the first screen's stay dark)
                out.append((t, round(x), round(r * 1.6), "sunlit rim"))
    for piece, t, x, _ in PLACED:
        pos = piece_pos(t)
        if piece == "rail-lamp":
            out.append((light_t(pos, 64), x + 8, 64, "rail lamp"))
        elif piece.startswith("dome") and piece != "dome-broken":
            out.append((light_t(pos, 110), x, 110, piece))
        elif piece == "landing-pad":
            out.append((light_t(pos, 100), x, 100, piece))
        elif piece == "daedalus-gate":
            out.append((light_t(pos - 10, 140), x, 140, "the main airlock"))
            out.append((light_t(pos + 28, 70), x + FREIGHT_X, 70, "the freight lock"))
        elif piece == "dish-big":
            out.append((light_t(pos - 48, 50), x + 44, 50, "the dish's work lamp"))
    return sorted(out)


def targets():
    """Suggested positions of the level's lit targets, as layer positions of their centres; a ground
    target's `at: [t, x]` puts it there with t = (pos - 540 - its hitbox height / 2) / 130."""
    lamp = next(t for p, t, *_ in PLACED if p == "rail-lamp" and 54 <= t <= 58)
    crater = next((t, x) for p, t, x, _ in PLACED if p == "dark-crater")
    gate = next((t, x) for p, t, x, _ in PLACED if p == "daedalus-gate")
    gh = SIZES["daedalus-gate"][1]
    return {
        "ore-cart": (round(piece_pos(lamp)), RAIL_X, 24, "on the rail beside the t=%g rail lamp" % lamp),
        "survey-cache": (round(piece_pos(crater[0])), crater[1] + 6, 22, "on the dark crater's floor"),
        "airlock-terminal": (round(piece_pos(gate[0]) + gh / 2 - (GATE_BODY_H / 2 + 26)), gate[1], 30,
                             "in the main airlock's lit bay"),
    }


def write_proposal():
    lines = [
        "# Level 06 backdrop PROPOSAL (tools/art/backdrop_l06.py --proposal; M4 part F batch, round 23).",
        "# For the main agent to merge into data.yaml: `backdrop` as the level's backdrop block, the",
        "# sections' `tiles` below, and the commented `darkness.lights` and target positions.",
        "# Far side (tools/art/backdrop_l06.py): Level 04's top-down kit, no deep layer, one shared terrain and",
        "# palette for the ground tiles, the ore rail at x %d in every ground tile set (it runs on over the" % RAIL_X,
        "# seams). The ground images are the regolith as a lamp shows it; the runtime light map darkens it.",
        "",
        "# sections[i].tiles (by section, in order):",
    ]
    for s in SECTIONS:
        lines.append(f"#   {s['name']}: [{', '.join(s['tiles'])}]")
    lines += [
        "",
        "backdrop:",
        "  scroll_factors: {far: 0.85, ground: 1.0, low-air: 1.35, high-air: 2.2}",
        "  ramp: 2",
        "  haze_colour: 0c0e16",
        "  atmosphere:",
        "    clear: {haze: 0.2}",
        "    light: {banks: plumes-light, haze: 0.25}",
        "    medium: {banks: plumes-medium, haze: 0.3}",
        "    heavy: {banks: plumes-heavy, haze: 0.4}      # the collapsing dome's dust (140-146 s)",
        "  tile_sets:",
        "    terminator: {layer: ground, height: 960}     # section 1: big craters, their rims catch the last sun",
        "    dome-field: {layer: ground, height: 960}     # sections 2 and 5: settlement regolith, low relief",
        "    array-field: {layer: ground, height: 960}    # section 3: cratered regolith round the dishes",
        "    vrell-field: {layer: ground, height: 960}    # section 4: teal Vrell roots",
        "    plumes-light: {layer: low-air, height: 960, drift: 6}     # darkened (the low-air layer is not)",
        "    plumes-medium: {layer: low-air, height: 960, drift: 8}",
        "    plumes-heavy: {layer: low-air, height: 960, drift: 10}",
        "    ejecta-streaks: {layer: high-air, height: 960}",
        "  pieces:",
    ]
    for name, (w, h) in SIZES.items():
        mid = ", mid_size: true" if name in MID_SIZE else ""
        note = f"   # {NOTES[name]}" if NOTES.get(name) else ""
        lines.append(f"    {name}: {{layer: ground, size: [{w}, {h}]{mid}}}{note}")
    lines.append("  placed:")
    for piece, t, x, comment in PLACED:
        if comment:
            lines.append(f"    # {comment}")
        lines.append(f"    - {{piece: {piece}, t: {t:g}, x: {x}}}")
    lines += ["", "# darkness.lights (suggested static pools, `t` entering at the top edge, `x`, `radius`):",
              "# the domes, the rail lamps, the airlocks, the pad; section 1's sunlit crater rims are pools too",
              "# (unsure: the light map tints every static pool warm, the sun would be white).",
              "# lights:"]
    for t, x, r, what in lights():
        lines.append(f"#   - {{t: {t:g}, x: {x}, radius: {r}}}   # {what}")
    lines += ["", "# Suggested target positions (layer position of the centre, x); a ground target's",
              "# `at: [t, x]` places it there with t = (pos - 540 - its hitbox height / 2) / 130:"]
    for name, (pos, x, hb, what) in targets().items():
        lines.append(f"#   {name}: at [{t_at(pos - SCREEN - hb / 2):.2f}, {x}]   # centre at layer position {pos} "
                     f"(hitbox height {hb}); {what}")
    PROPOSAL.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"proposal: {PROPOSAL.relative_to(ROOT)}")


# --------------------------------------------------------------------------- checks

def check_mid_size(backdrop):
    """The art direction's density: at most 3 mid-size set pieces on screen (sampled every 0.05 s)."""
    bad = []
    end = SECTIONS[-1]["end"] + 6
    for t in np.arange(0, end, 0.05):
        s = scroll_at(t)
        on = [p["piece"] for p in backdrop["placed"] if backdrop["pieces"][p["piece"]].get("mid_size")
              and abs(piece_pos(p["t"]) - (s + MID)) < MID + backdrop["pieces"][p["piece"]]["size"][1] / 2]
        if len(on) > 3:
            bad.append((round(t, 2), on))
    return bad


def wrap_score(a):
    """BackdropSeamsTest.wrapScore on an RGBA array."""
    lum = a[..., 0] * 0.299 + a[..., 1] * 0.587 + a[..., 2] * 0.114
    op = a[..., 3] > 0
    h = a.shape[0]

    def change(i, j):
        both = op[i] & op[j]
        if both.sum() < a.shape[1] / 4:
            return np.nan
        d = np.sort(np.abs(lum[i][both] - lum[j][both]))
        kept = int(len(d) * 0.9)
        return d[:kept].mean() if kept else 0
    near = max(change(h - 2, h - 1), change(h - 1, 0), change(0, 1))
    ref = [change(k % h, (k + 1) % h) for k in range(h - 40, h + 40) if k < h - 4 or k > h + 2]
    ref = sorted(r for r in ref if not np.isnan(r))
    return near / max(ref[len(ref) // 2], 0.5)


def composite_rows(backdrop, tile, frm, to):
    """BackdropSeamsTest.composite: layer rows [frm, to) of one tile set with the still ground pieces on top."""
    t_img = np.array(load(tile)).astype(float)
    rows = np.stack([t_img[H - 1 - (r % H)] for r in range(frm, to)])
    for p in backdrop["placed"]:
        spec = backdrop["pieces"][p["piece"]]
        if spec["layer"] != "ground":
            continue
        img = np.array(load(p["piece"])).astype(float)
        ph, pw = img.shape[:2]
        bottom = round(piece_pos(p["t"]) - ph / 2)
        left = round(p["x"] - pw / 2)
        for k, r in enumerate(range(frm, to)):
            y = ph - 1 - (r - bottom)
            if 0 <= y < ph:
                for x in range(W):
                    c = x - left
                    if 0 <= c < pw and img[y, c, 3] > 0:
                        al = img[y, c, 3] / 255
                        rows[k, x, :3] = img[y, c, :3] * al + rows[k, x, :3] * (1 - al)
                        rows[k, x, 3] = max(rows[k, x, 3], img[y, c, 3])
    return rows


def check_seams(backdrop):
    problems = []
    for name in GROUND_TILES:
        score = wrap_score(np.array(load(name)).astype(float))
        print(f"  wrap {name}: {score:.2f}")
        if score > 1.5:
            problems.append(f"{name} wraps with a line ({score:.2f})")
    for i in range(1, len(SECTIONS)):
        below, above = TILE_OF[i - 1], TILE_OF[i]
        if below == above:
            continue
        a = composite_rows(backdrop, below, SEAM[i] - 2, SEAM[i] + 2)
        b = composite_rows(backdrop, above, SEAM[i] - 2, SEAM[i] + 2)
        la = a[..., :3] @ [0.299, 0.587, 0.114]
        lb = b[..., :3] @ [0.299, 0.587, 0.114]
        diff = ((a[..., 3] > 0) != (b[..., 3] > 0)) | ((a[..., 3] > 0) & (np.abs(la - lb) > 16))
        share = diff.mean()
        print(f"  seam {below} -> {above} at {SEAM[i]}: {share * 100:.2f} % differ")
        if share > 0.002:
            problems.append(f"seam {below} -> {above} at {SEAM[i]}")
    return problems


def check_edges(backdrop):
    """BackdropAssetsTest: a ground piece is transparent along every edge that crosses the screen."""
    bad = []
    for p in backdrop["placed"]:
        img = np.array(load(p["piece"]))[..., 3] > 0
        ph, pw = img.shape
        left = round(p["x"] - pw / 2)
        cols = slice(max(0, -left), min(pw, W - left))
        if img[0, cols].any() or img[-1, cols].any():
            bad.append(f"{p['piece']} at t {p['t']}: top or bottom row opaque")
        if (0 < left < W and img[:, 0].any()) or (0 < left + pw < W and img[:, -1].any()):
            bad.append(f"{p['piece']} at t {p['t']}: a side column opaque")
    return bad


# --------------------------------------------------------------------------- jobs

def jobs(backdrop, wanted):
    out = [(n, (W, s["height"])) for n, s in backdrop["tile_sets"].items() if n in TILE_SETS]
    out += [(n, tuple(s["size"])) for n, s in backdrop["pieces"].items() if n in PIECES]
    return [j for j in out if not wanted or j[0] in wanted]


def render(job):
    name, (w, h) = job
    img = (TILE_SETS.get(name) or PIECES[name])(w, h)
    if img.size != (w, h):
        raise ValueError(f"{name}: rendered {img.size}, the data file says {(w, h)}")
    return img


def write(name, img):
    artkit.save_png(img, OUT / f"{name}.png", SOURCE)
    print(f"{name}: {img.size[0]}x{img.size[1]}, {artkit.colour_count([img])} colours")


def load(name):
    return Image.open(OUT / f"{name}.png").convert("RGBA")


# --------------------------------------------------------------------------- review

AMBIENT = 0.12
LAMP = np.array((1.0, 0.82, 0.55))
HEADLIGHT = np.array((0.72, 0.84, 1.0))


def light_map(t, scroll, ship=(240, 450), flare=None):
    """A preview of FarsideLooks' light map (ambient, the static pools, the headlight from t 22, a flare)."""
    yy, xx = np.mgrid[0:SCREEN, 0:W].astype(float)
    up = SCREEN - 1 - yy                                          # px above the bottom edge

    def pool(x, y, r):
        d = np.clip(1 - np.hypot(xx - x, up - y) / r, 0, 1)
        return d * d * (3 - 2 * d)
    lm = np.full((SCREEN, W, 3), AMBIENT) * np.array((1, 1, 1.1))
    for lt, x, r, _ in lights():
        y = SCREEN + r - (scroll - scroll_at(lt))
        if -r < y < SCREEN + r:
            lm += pool(x, y, r)[..., None] * LAMP
    if t >= 22:
        sx, sy = ship[0], SCREEN - ship[1] + 18
        along = (up - sy) / 200
        off = np.abs(np.arctan2(xx - sx, np.maximum(up - sy, 1e-3)))
        cone = np.clip((1 - off / np.radians(30)) * 3, 0, 1) * np.clip(1 - along ** 2, 0, 1) * (up > sy)
        lm += cone[..., None] * HEADLIGHT
    if flare:
        lm += pool(*flare, 120)[..., None] * np.array((1.0, 0.78, 0.5))
    return np.clip(lm, 0, 1)


def composite(t, darkness=False, flare=None):
    """The play field at t as Backdrop draws it (ground tiles from their seams, the ground pieces, the
    banks, high-air at 40 % additive); with ``darkness`` the ground under the light map preview."""
    bd = LEVEL["backdrop"]
    scroll = scroll_at(t)
    sec = max(i for i, s in enumerate(STARTS) if t >= s)
    peak = SECTIONS[sec].get("peak")
    look = bd["atmosphere"][peak["atmosphere"] if peak and peak["from"] <= t < peak["to"] else SECTIONS[sec]["atmosphere"]]
    ground = Image.new("RGBA", (W, SCREEN), (0, 0, 0, 255))
    for y in range(SCREEN):
        pos = scroll + (SCREEN - 1 - y)
        tile = load(TILE_OF[section_of(pos)])
        r = int(np.floor(pos)) % H
        ground.paste(tile.crop((0, H - 1 - r, W, H - r)), (0, y))
    for p in bd["placed"]:
        spec = bd["pieces"][p["piece"]]
        pw, ph = spec["size"]
        bottom = round(piece_pos(p["t"]) - ph / 2)
        y = SCREEN - (bottom - round(scroll)) - ph
        if -ph < y < SCREEN:
            sprite.paste(ground, load(p["piece"]), round(p["x"] - pw / 2), y)
    img = np.array(ground)[..., :3].astype(float)
    if darkness:
        img *= light_map(t, scroll, flare=flare)
    if look.get("banks"):
        banks = np.array(load(look["banks"])).astype(float)
        shift = int(round(bd["tile_sets"][look["banks"]].get("drift", 0) * t)) % W
        banks = np.roll(banks, shift, axis=1)
        pos0 = scroll * bd["scroll_factors"]["low-air"]
        for y in range(SCREEN):
            row = banks[(banks.shape[0] - 1 - int(pos0 + SCREEN - 1 - y)) % banks.shape[0]]
            al = row[:, 3:4] / 255
            img[y] = img[y] * (1 - al) + row[:, :3] * al
    if "ejecta-streaks" in SECTIONS[sec]["tiles"]:
        ej = np.array(load("ejecta-streaks")).astype(float)
        pos0 = scroll * bd["scroll_factors"]["high-air"]
        for y in range(SCREEN):
            row = ej[(ej.shape[0] - 1 - int(pos0 + SCREEN - 1 - y)) % ej.shape[0]]
            img[y] = np.minimum(255, img[y] + row[:, :3] * row[:, 3:4] / 255 * 0.4)
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGB")


TIMES = [(8, "1 TERMINATOR", None), (31, "2 OUTER DOMES", None), (57, "2 RAIL LAMP, ORE CART", None),
         (76, "3 DISH ARRAY, FLARE", (200, 300)), (111, "3 DARK CRATER, FLARE", (300, 240)),
         (120.3, "4 NEST UNDER THE FIELD UNITS", None), (134, "4 NESTS AND DOMES", None),
         (145, "4 HEAVY PEAK, BROKEN DOME", (260, 260)), (167.2, "5 LANDING PAD", None),
         (182.25, "5 DAEDALUS GATE", None)]


def review():
    backdrop = LEVEL["backdrop"]
    items = []
    for name, spec in list(backdrop["tile_sets"].items()) + list(backdrop["pieces"].items()):
        im = load(name)
        scale = min(1.0, 200 / max(im.size))
        im = im.resize((max(1, int(im.width * scale)), max(1, int(im.height * scale))), Image.NEAREST)
        items.append((name, im, spec.get("layer") == "high-air"))
    width, x, y, row_h = 1500, 16, 44, 0
    rows = []
    for name, im, add in items:
        cell = max(im.width, 6 * len(name))
        if x + cell > width - 16:
            x, y, row_h = 16, y + row_h + 30, 0
        rows.append((name, im, add, x, y))
        x += cell + 14
        row_h = max(row_h, im.height)
    comp_y = y + row_h + 40
    cw = 2 * 240 + 30
    per_row = (width - 16) // cw
    n_rows = (len(TIMES) + per_row - 1) // per_row
    artkit.REVIEW_ROUND = "r23"
    sheet = raster.sheet(width, comp_y + n_rows * (270 + 30) + 20, "LEVEL 06 BACKDROP - FINAL PIECES (PROPOSAL)",
                         "R23; COMPOSITES 1/2 SCALE: RAW (LEFT), UNDER A LIGHT-MAP PREVIEW (RIGHT)")
    for name, im, add, px, py in rows:
        plate = Image.new("RGBA", im.size, artkit.PLATE)
        plate = artkit.add_light(plate, im) if add else (plate.alpha_composite(im) or plate)
        sheet.alpha_composite(plate, (px, py + 14))
        raster.draw_text(sheet, px, py, name.upper(), raster.LABEL)
    for k, (t, label, flare) in enumerate(TIMES):
        px, py = 16 + (k % per_row) * cw, comp_y + (k // per_row) * 300
        raw = composite(t).convert("RGBA").resize((240, 270), Image.BOX)
        dark = composite(t, True, flare).convert("RGBA").resize((240, 270), Image.BOX)
        sheet.alpha_composite(raw, (px, py + 14))
        sheet.alpha_composite(dark, (px + 246, py + 14))
        raster.draw_text(sheet, px, py, f"T={t:g} {label}", raster.LABEL)
    REVIEW.parent.mkdir(parents=True, exist_ok=True)
    sheet.convert("RGB").save(REVIEW, optimize=True)
    print(f"review: {REVIEW.relative_to(ROOT)}")


def atlas_area(backdrop):
    area = sum(W * s["height"] for s in backdrop["tile_sets"].values())
    area += sum(s["size"][0] * s["size"][1] * s.get("frames", 1) for s in backdrop["pieces"].values())
    print(f"atlas area: {area:,} px = {area / 2048 ** 2:.2f} pages of 2048x2048")


def run_checks(backdrop):
    problems = [f"mid-size density at t {t}: {on}" for t, on in check_mid_size(backdrop)[:3]]
    problems += check_seams(backdrop) + check_edges(backdrop)
    for p in problems:
        print("PROBLEM:", p)
    return problems


def main(argv):
    global LEVEL
    if "--proposal" in argv or LEVEL["backdrop"] is None:
        write_proposal()
        LEVEL = level_data()
        if "--proposal" in argv:
            return
    backdrop = LEVEL["backdrop"]
    print(f"sections: seams at {SEAM[1:]} (u {[s % H for s in SEAM[1:]]}); backdrop from "
          f"{'data.yaml' if LEVEL['from_data'] else PROPOSAL.name}")
    if "--check" in argv:
        sys.exit(1 if run_checks(backdrop) else 0)
    wanted = {a for a in argv if not a.startswith("--")}
    known = set(GROUND_TILES) | set(TILE_SETS) | set(PIECES) | set(CUT)
    missing = (backdrop["tile_sets"].keys() | backdrop["pieces"].keys()) - known
    if missing or wanted - known:
        raise SystemExit(f"no generator for {sorted(missing | (wanted - known))}")
    if "--review" not in argv:
        OUT.mkdir(parents=True, exist_ok=True)
        todo = jobs(backdrop, wanted)
        with ProcessPoolExecutor() as pool:
            ground = pool.submit(ground_group) if not wanted or wanted & (set(GROUND_TILES) | set(CUT)) else None
            for job, img in zip(todo, pool.map(render, todo)):
                write(job[0], img)
            if ground is not None:
                for name, img in ground.result().items():
                    size = (W, backdrop["tile_sets"][name]["height"]) if name in GROUND_TILES else \
                        tuple(backdrop["pieces"][name]["size"])
                    if img.size != size:
                        raise ValueError(f"{name}: rendered {img.size}, the data file says {size}")
                    write(name, img)
        run_checks(backdrop)
    atlas_area(backdrop)
    review()


if __name__ == "__main__":
    main(sys.argv[1:])
