#!/usr/bin/env python3
"""Production art PROPOSAL: the Level 05 backdrop, the Vrell nest crater beside the mass-driver
line on Luna (design/campaign/act-1-first-contact/level-05-crater-nest; concept round 22).

Built the way backdrop_l04.py works and on its helpers (imported unchanged): the same kit, palette
and key light, the same shared periodic terrain, no deep layer, nothing lit mirrored.

Outputs (assets/backdrop/level-05/, one PNG per tile set and set piece of the level's `backdrop`
block, named by its id and checked against its size; frames as <id>_<n>.png):
  mass-driver-field                 ground, section 1: Level 04's cratered mare with the rail at
                                    x 432, re-rendered here with Level 05's seam rows
  crater-slope                      ground, section 2: the crater's inner slope, talus and two slump
                                    ridges
  nest-floor                        ground, sections 3-4: the crater floor with teal Vrell roots
  arena-floor                       ground, section 5: the calm floor under the frigate (low relief,
                                    darkened ash, no roots), so the bell reads over it
  mare-a                            ground, section 6: Level 04's grey mare outside the far rim
  plumes-light/-medium/-heavy,      low-air / high-air: Level 04's regolith plumes and ejected
  ejecta-streaks                    rock (its generators, the same images)
  rim-south                         ground, 480x920 over the section 2 seam: the outer rim wall,
                                    the outer slope facing away from the sun, the crest with
                                    rubble, the lit inner scarp and a terrace; the rail climbs it
                                    in a cutting to the launch lip at the crest
  rim-north                         ground, 480x640 over the section 6 seam: the far rim, its inner
                                    wall facing away from the sun in the crest's long shadow, the
                                    lit outer slope
  battery-a .. battery-d            ground: the four nest batteries' growth patches, a socket under
                                    every unit the battery's ground targets place (all difficulties;
                                    a socket stays empty when a difficulty leaves its unit out)
  burning-nest_0..5                 ground, 288x224, 6 frames at 8 fps: the nest's brood mound at
                                    the arena's end, charred, fire in its torn vents
  boulder, boulder-growth-a/-b,     ground: Level 04's pieces (its generators, the same images);
  rover-a/-b, pod-husk-a/-b         the growth sockets carry the rim turrets
  sled-run_0..29                    ground, 48x480, 30 frames: the rail lamps and the sled shot the
                                    game draws over the rail (Level 04's generator, rendered here
                                    so the level loads only its own backdrop pages)
  design/campaign/.../level-05-crater-nest/concept/backdrop-final-r22-a.png   review sheet with
                                    composites of the level at chosen times (units, the sled lamps
                                    and the frigate's bell drawn in for scale)

**Pieces cut from the terrain.** The rims are rendered as windows of the ground layer: the same
height field as the tiles under them (each row the tile set of its section, by layer position),
the rim's height added, then shaded, dithered (Bayer phase of the tile rows) and quantized with the
tiles in one palette, so outside the rim a rim piece is pixel for pixel the tiles under it. Their
top and bottom rows fade out in an ordered dither; their sides lie on the play field's edges.

**Geometry from the data.** Layer positions follow LevelData.scrollAt (150 px/s, 30 in the
arena); the rims sit over their seams, the battery patches under their units (a unit's centre is
scrollAt(t) + 540 + half its hitbox, as the sim spawns it). The script prints every generated
piece's size and placement and fails when the data file's differ (`--layout` prints them only).

**The rail.** The mass-driver-field's rail and pylons (Level 04's overlay) continue in rim-south
up to the launch lip at layer position 6,290, past the 6,090 the sled lamps reach (the game draws
the sled-run frames over the rail until 2 s after the last sled, t = 37 s), with the pylons
on the same 60 px pitch, so the lamps stay on them.

Run: python3 tools/art/backdrop_l05.py [--review | --layout] [id ...]   (~2 min on 20 cores)
"""
import functools
import sys
from concurrent.futures import ProcessPoolExecutor

import numpy as np
import yaml
from PIL import Image

import artkit
from artkit import DESIGN, ROOT, raster, sprite

import backdrop_l04 as b4  # noqa: E402  (the Level 04 generator, imported unchanged)
import scenes_r06 as s6  # noqa: E402
from render import enemy_models as em  # noqa: E402
from render.scene_models import rock_sprites  # noqa: E402
from render.sdf import sd_box, sd_cylinder_z, sd_ellipsoid, sd_sphere, rotate_z, union  # noqa: E402
from render.station import ACCENT, DARK, HULL, WARM  # noqa: E402

l01 = b4.l01
SCRIPT = "backdrop_l05.py"
SOURCE = artkit.source_note(SCRIPT, "Level 05 backdrop (proposal)")
LEVEL_DIR = DESIGN / "campaign" / "act-1-first-contact" / "level-05-crater-nest"
OUT = ROOT / "assets" / "backdrop" / "level-05"
REVIEW = LEVEL_DIR / "concept" / "backdrop-final-r22-a.png"
W, H = b4.W, b4.H
MID, SCREEN = 270, 540
RAIL_X = b4.RAIL_X
GREYS, EARTHSHINE = b4.GREYS, b4.EARTHSHINE
MARGIN = 48                  # rows rendered beyond a window's ends (shading neighbourhood)
FACE_K = 25.0                # the rims' slope light: brightness change per unit of rise per px
RIM_SHADOW_STEPS = 120       # the rims' long cast shadow, px
RIM_RAMP = 0.12              # how much of a rim's height brightens the grey ramp (the slope light shapes it)


def level_data():
    return yaml.safe_load((LEVEL_DIR / "data.yaml").read_text(encoding="utf-8"))


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
    """The inverse of scroll_at."""
    s = 0.0
    for i, sec in enumerate(SECTIONS):
        span = (sec["end"] - STARTS[i]) * speed(i)
        if scroll < s + span or i == len(SECTIONS) - 1:
            return STARTS[i] + (scroll - s) / speed(i)
        s += span


def seam(i):
    """Where section i's ground tiles begin (layer position)."""
    return scroll_at(STARTS[i]) + SCREEN


SEAM = [None] + [round(seam(i)) for i in range(1, len(SECTIONS))]   # 5790, 11790, 19290, 23040, 24690
TILE_OF = [next(t for t in s["tiles"] if LEVEL["backdrop"]["tile_sets"][t]["layer"] == "ground") for s in SECTIONS]


def u_of(pos):
    return np.mod(pos, H)


def seams_of(tile):
    """u of the seams next to a ground tile set's sections (where its features fade out)."""
    out = set()
    for i, t in enumerate(TILE_OF):
        if t != tile:
            continue
        if i > 0 and TILE_OF[i - 1] != tile:
            out.add(SEAM[i] % H)
        if i + 1 < len(TILE_OF) and TILE_OF[i + 1] != tile:
            out.add(SEAM[i + 1] % H)
    return tuple(sorted(out))


# --------------------------------------------------------------------------- the rims' geometry

CREST_S = SEAM[1] + 510            # 6300: rim-south's crest (the rim turrets stand behind it, 6555)
RAIL_END = CREST_S - 10            # the launch lip
RIM_S = (SEAM[1] - 70, 920)        # (bottom layer position, height)
CREST_N = SEAM[5] + 70             # 24760
RIM_N = (CREST_N - 320, 640)
KNOTS_S = [(-470, 0), (-400, 0.2), (-220, 1.3), (-60, 2.7), (0, 3.0), (25, 2.7), (85, 0.95), (190, 0.8),
           (240, 0.25), (290, 0)]
KNOTS_N = [(-250, 0), (-200, 0.15), (-150, 0.6), (-100, 0.85), (-55, 2.4), (-15, 2.95), (0, 3.0), (50, 2.6),
           (170, 1.0), (230, 0.2), (260, 0)]


def profile(knots):
    """A smooth rim profile R(dv) on a 1 px grid, dv from the crest."""
    dv = np.arange(knots[0][0] - 40, knots[-1][0] + 41)
    r = np.interp(dv, *zip(*knots))
    k = np.exp(-np.linspace(-2.5, 2.5, 25) ** 2)
    r = np.convolve(np.pad(r, 12, mode="edge"), k / k.sum(), mode="valid")
    return dv, r


def rim_height(pos, crest, knots, seed, cutting=False):
    """The rim's added height over window rows ``pos`` (layer positions, top row first)."""
    dv_grid, r = profile(knots)
    xx = np.arange(W, dtype=float)[None, :]
    wob = 16 * np.sin(2 * np.pi * xx / W * 1.3 + seed % 7) + 9 * np.sin(2 * np.pi * xx / W * 3.1 + 1.3)
    dv = pos[:, None] - (crest + wob)
    rr = np.interp(dv, dv_grid, r, left=0, right=0)
    tex = raster.fbm(W, len(pos), 64, seed, octaves=3, period=False)
    rr = rr * (0.92 + 0.16 * tex)
    rr[rr < 2e-3] = 0          # exactly the tiles outside the rim
    if cutting:      # the rail's cutting through the rim, up to the launch lip
        cut = np.clip(1 - (np.abs(xx - RAIL_X) - 26) / 10, 0, 1) * (pos[:, None] <= RAIL_END + 14)
        rr = rr * (1 - 0.9 * cut)
    return rr


# --------------------------------------------------------------------------- terrain

def ridges(rows_u, centres, seed):
    """Slump ridges across the slope: a steep face below, a long back above (u up the screen)."""
    rng = np.random.default_rng(seed)
    xx = np.arange(W, dtype=float)[None, :]
    out = np.zeros((len(rows_u), W))
    for c in centres:
        line = c + 14 * np.sin(2 * np.pi * xx / W * rng.uniform(1, 2.5) + rng.uniform(0, 6))
        d = np.mod(rows_u[:, None] - line + H / 2, H) - H / 2     # periodic: the tile wraps
        out += 0.14 * np.where(d < 0, np.exp(-(d / 18) ** 2), np.exp(-(d / 70) ** 2))
    return out


@functools.lru_cache
def tile_heights():
    """Height field of every ground tile set (H x W, row 0 = u 959)."""
    base = b4.base_height()
    u = (H - 1 - np.arange(H)).astype(float)
    out = {}
    md = base + b4.features(731, 5, (14, 26), 24, seams_of("mass-driver-field"))
    rail = np.clip(1 - (np.abs(np.arange(W)[None, :] - RAIL_X) - 24) / 6, 0, 1)
    out["mass-driver-field"] = md * (1 - rail) + (md * 0.3 + 0.1) * rail
    m = b4.seam_mask(seams_of("crater-slope"))
    out["crater-slope"] = base + b4.features(911, 8, (14, 30), 70, seams_of("crater-slope")) + ridges(u, (470, 760), 912) * m
    out["nest-floor"] = base + b4.features(921, 12, (16, 44), 26, seams_of("nest-floor"))
    m = b4.seam_mask(seams_of("arena-floor"), 30)
    out["arena-floor"] = base * (1 - 0.6 * m) + float(base.mean()) * 0.6 * m
    out["mare-a"] = base + b4.features(701, 7, (16, 30), 30, seams_of("mare-a"))
    return out


def calm(names, rows):
    """The arena floor's darkening (1 = none) per row (its tile set and tile row index), W wide."""
    mask = np.asarray(b4.seam_mask(seams_of("arena-floor"), 30))
    out = np.ones((len(rows), W))
    arena = np.asarray(names) == "arena-floor"
    out[arena] = 1 - 0.2 * mask[np.asarray(rows)[arena]]
    return out


def shade(h, rim=None):
    """backdrop_l04.shade_terrain with the rims' slope light and long shadow added; without a rim
    it is shade_terrain exactly (so a window equals its tiles outside the rim)."""
    base = b4.base_height()
    lo, hi = base.min() - 0.15, base.max() + 0.1
    total = h if rim is None else h + rim
    sh = s6.hillshade(h * 60, 0.55)
    lit = s6.cast_shadows(total * 60, steps=28, drop=1.0)
    face = 1.0
    if rim is not None:
        rise = (np.roll(rim, 1, axis=0) - np.roll(rim, -1, axis=0)) / 2      # per px up the screen
        face = np.clip(1 - rise * FACE_K, 0.4, 1.4)
        lit = np.minimum(lit, s6.cast_shadows(rim * 60, steps=RIM_SHADOW_STEPS, drop=1.0))
    t = np.clip(((total if rim is None else h + RIM_RAMP * rim) - lo) / (hi - lo), 0, 1)
    col = s6.ramp_img(GREYS, 0.25 + t * 0.6) * (sh * face * (0.35 + 0.65 * lit))[..., None]
    dark = 1 - np.minimum(lit, face) if rim is not None else 1 - lit
    return (col + EARTHSHINE * dark[..., None] * 0.9) * 0.8


def dithered(col, rows):
    """backdrop_l04.dithered with the Bayer phase of the tile rows ``rows``."""
    b = l01.BAYER4[np.asarray(rows)[:, None] % 4, np.arange(col.shape[1])[None, :] % 4]
    return np.clip(col + ((b - 0.5) * 6)[..., None], 0, 255)


def window_rows(bottom, height, margin=MARGIN):
    """Layer positions of a window's rows, top row first, with ``margin`` rows beyond each end."""
    return np.arange(bottom + height - 1 + margin, bottom - 1 - margin, -1)


def window_heights(pos):
    hs = tile_heights()
    rows = H - 1 - u_of(pos)
    h = np.empty((len(pos), W))
    names = np.empty(len(pos), object)
    for k, p in enumerate(pos):
        i = max(j for j in range(len(SECTIONS)) if j == 0 or p >= SEAM[j])
        names[k] = TILE_OF[i]
        h[k] = hs[TILE_OF[i]][rows[k]]
    return h, rows, names


def rail_rows():
    """The rail overlay quantized as for the mass-driver-field tile."""
    return np.array(artkit.quantize_set([b4.rail_overlay()], 10)[0])


ROOTS = ((80, 180), (330, 380), (200, 610), (420, 820), (120, 800))   # clear of the seam rows 689 and 959


def roots_overlay():
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    for (x, y), seed in zip(ROOTS, range(931, 936)):
        for dy in (-H, 0, H):
            b4.roots(img, (x, y + dy), seed, 5, 64, 2.8)
    return b4.assert_clear(img, seams_of("nest-floor"), "nest-floor roots")


def rail_end():
    """The launch lip at the rail's end on the crest: a gantry over the rail, a raised lip, amber lamps."""
    def scene(p):
        items = [(sd_box(p, (0, 0, 4), (16, 10, 4), 1.5), HULL),
                 (sd_box(p, (0, -6, 9), (12, 3, 2.5), 1), ACCENT),
                 (sd_box(p, (0, 4, 14), (22, 2.2, 1.6), 0.6), HULL)]
        items += [(sd_box(p, (sx * 21, 4, 7), (2.4, 2.4, 7), 0.6), DARK) for sx in (-1, 1)]
        items += [(sd_box(p, (sx * 21, 4, 15.2), (1.6, 1.6, 0.8), 0.3), WARM) for sx in (-1, 1)]
        return union(*items)
    return b4.render_model(scene, l01.KIT_PAL, (60, 36), factor=4, colors=24)


def rim_rail(pos, rows):
    """rim-south's rail up to the launch lip: the mass-driver-field's rail rows exactly (already
    quantized, so its colours match the tile's where the rim's bottom fades into it)."""
    a = np.zeros((len(pos), W, 4), np.uint8)
    keep = pos <= RAIL_END
    a[keep] = rail_rows()[rows[keep]]
    return Image.fromarray(a, "RGBA")


def rim_overlay(name, pos, rows):
    """What lies on a rim window: crest rubble, and on rim-south the rail's launch lip."""
    img = Image.new("RGBA", (W, len(pos)), (0, 0, 0, 0))
    crest = CREST_S if name == "rim-south" else CREST_N
    rng = np.random.default_rng(951 if name == "rim-south" else 961)
    top = pos[0]
    for k in range(9):
        x = rng.uniform(20, W - 20)
        if name == "rim-south" and abs(x - RAIL_X) < 50:
            continue
        r = rng.uniform(5, 11)
        rock = rock_sprites(r, int(rng.integers(1000, 9000)), GREYS[1:], rotations=1, crater_count=2)[0]
        b4.place(img, rock, x, top - (crest + rng.uniform(-30, 30)), shadow=r * 1.4, opacity=0.6)
    if name == "rim-south":
        b4.place(img, rail_end(), RAIL_X, top - RAIL_END, shadow=10, opacity=0.55)
    return img


RIMS = {"rim-south": (RIM_S, CREST_S, KNOTS_S, 941, True), "rim-north": (RIM_N, CREST_N, KNOTS_N, 942, False)}
GROUND_TILES = ("mass-driver-field", "crater-slope", "nest-floor", "arena-floor", "mare-a")


@functools.lru_cache
def ground_group():
    """Every ground tile set and the two rims, quantized to one terrain palette."""
    hs = tile_heights()
    cols, extra = {}, {}
    allrows = np.arange(H)
    for name in GROUND_TILES:
        cols[name] = dithered(shade(hs[name]) * calm([name] * H, allrows)[..., None], allrows)
    for name, ((bottom, height), crest, knots, seed, cutting) in RIMS.items():
        pos = window_rows(bottom, height)
        h, rows, names = window_heights(pos)
        if cutting:   # the rail's bed continues up the rim to the lip
            rail = np.clip(1 - (np.abs(np.arange(W)[None, :] - RAIL_X) - 24) / 6, 0, 1)
            up = ((names != "mass-driver-field") & (pos <= RAIL_END + 14))[:, None]
            h = np.where(up, h * (1 - rail) + (h * 0.3 + 0.1) * rail, h)
        rim = rim_height(pos, crest, knots, seed, cutting)
        col = shade(h, rim) * calm(names, rows)[..., None]
        cols[name] = dithered(col, rows)[MARGIN:-MARGIN]
        extra[name] = (pos[MARGIN:-MARGIN], rows[MARGIN:-MARGIN])
    names = list(cols)
    quant = dict(zip(names, artkit.quantize_set([l01.rgba(cols[n], np.full(cols[n].shape[:2], 255)) for n in names], 22)))
    quant["mass-driver-field"] = b4.overlaid(quant["mass-driver-field"], Image.fromarray(rail_rows(), "RGBA"))
    quant["nest-floor"] = b4.overlaid(quant["nest-floor"], artkit.quantize_set([roots_overlay()], 10)[0])
    for name in RIMS:
        pos, rows = extra[name]
        if name == "rim-south":
            quant[name] = b4.overlaid(quant[name], rim_rail(pos, rows))
        over = rim_overlay(name, pos, rows)
        spare = 32 - artkit.colour_count([quant[name]])        # the rubble and the lip in what is left of 32
        quant[name] = b4.overlaid(quant[name], artkit.quantize_set([over], min(10, spare))[0])
        a = np.array(quant[name])
        hh = a.shape[0]
        edge = np.minimum(np.arange(hh), hh - 1 - np.arange(hh)).astype(float)[:, None] * np.ones((1, W))
        keep = artkit.ordered_dither(np.clip((edge - 1) / 12, 0, 1), 2) > 0.5
        a[..., 3] = np.where(keep, a[..., 3], 0)
        a[a[..., 3] == 0] = 0
        quant[name] = Image.fromarray(a, "RGBA")
    return quant


# --------------------------------------------------------------------------- the batteries

def hitbox(enemy):
    data = yaml.safe_load((DESIGN / "enemies" / "ground" / enemy / "data.yaml").read_text(encoding="utf-8"))
    return data["hitbox"]


def battery_units():
    """Per battery: every unit position over all difficulties, (enemy, x, layer position of its centre)."""
    out = {}
    for g in LEVEL["ground_targets"]:
        group = g.get("group", "")
        if not group.startswith("Battery"):
            continue
        hb = hitbox(g["enemy"])[1]
        spots = {tuple(a) for v in [g] + [g.get(d, {}) for d in ("easy", "hard")] for a in v.get("at", [])}
        for t, x in sorted(spots):
            out.setdefault(group, []).append((g["enemy"], x, scroll_at(t) + SCREEN + hb / 2))
    return out


def battery_layout():
    """Per battery piece: (id, width, height, left, bottom, units)."""
    out = []
    for group, units in sorted(battery_units().items()):
        xs = [x for _, x, _ in units]
        ps = [p for _, _, p in units]
        m = 52
        left = int(np.floor(min(xs) - m))
        width = int(np.ceil(max(xs) + m)) - left
        width += width % 2
        bottom = int(np.floor(min(ps) - m))
        height = int(np.ceil(max(ps) + m)) - bottom
        height += height % 2
        out.append(("battery-" + group.split()[-1].lower(), width, height, left, bottom, units))
    return out


def mortar_socket(seed):
    """The Polyp Mortar's pit: a fleshy ring of violet bulbs round a wet teal hollow."""
    rng = np.random.default_rng(seed)
    blobs = [(13 * np.cos(a), 13 * np.sin(a), rng.uniform(4, 6)) for a in np.linspace(0, 2 * np.pi, 9, endpoint=False)]

    def scene(p):
        items = [(sd_ellipsoid(p, (x, y, 2), (r, r * 0.9, r * 0.6)), em.V_BODY) for x, y, r in blobs]
        items += [(sd_ellipsoid(p, (0, 0, -1), (11, 11, 2.5)), em.V_SAC)]
        items += [(sd_sphere(p, (x * 0.8, y * 0.8, 3), r * 0.35), em.V_GLOW) for x, y, r in blobs[::3]]
        return union(*items, k=2.0)
    return b4.vrell(scene, (52, 52))


def battery(w, h, spec):
    """A battery's growth patch: a dark resin stain, roots between the sockets, a socket per unit."""
    _, _, _, left, bottom, units = spec
    seed = 970 + ord(spec[0][-1])
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    yy, xx = np.mgrid[0:h, 0:w]
    pts = [(x - left, bottom + h - p) for _, x, p in units]
    tex = raster.fbm(w, h, 14, seed, octaves=3, period=False)
    d = np.min([np.hypot(xx - x, yy - y) for x, y in pts], axis=0)
    cover = np.clip(1 - (d - 26 - 22 * tex) / 18, 0, 1)
    stain = s6.ramp_img(GREYS, 0.2 + 0.15 * tex) * 0.55 + np.array(b4.CHITIN, float) * 0.25
    img = l01.rgba(stain, artkit.ordered_dither(cover * 0.85, 3) * 255)
    for k, (x, y) in enumerate(pts):
        b4.roots(img, (x, y), seed + k, 4, 46, 2.4)
    for k, ((enemy, _, _), (x, y)) in enumerate(zip(units, pts)):
        sock = mortar_socket(seed + 20 + k) if enemy == "polyp-mortar" else b4.growth_socket(seed + 40 + k)
        b4.place(img, sock, x, y, shadow=5, opacity=0.45)
    return b4.finish(img, 28)


# --------------------------------------------------------------------------- the burning nest

def nest_scene(p):
    """The brood mound: a ring of swollen sacs round a torn crown, ribs between, three torn vents."""
    rng = np.random.default_rng(981)
    items = [(sd_ellipsoid(p, (0, 0, 0), (60, 48, 22)), em.V_BODY)]
    for a in np.linspace(0, 2 * np.pi, 9, endpoint=False):
        r = rng.uniform(15, 22)
        items.append((sd_ellipsoid(p, (62 * np.cos(a), 50 * np.sin(a), 4), (r, r * 0.85, r * 0.7)), em.V_SAC))
    for a in np.linspace(0.3, 2 * np.pi + 0.3, 12, endpoint=False):
        q = rotate_z(p, -a)
        items.append((sd_box(q, (36, 0, 14), (30, 2.4, 6), 2), em.V_DARK))
    d, m = union(*items, k=4.0)
    for x, y in VENTS:      # the torn vents the fire burns in
        d = np.maximum(d, -sd_cylinder_z(p, (x, y, 10), 11, 30))
    return d, m


VENTS = [(-22, -10), (24, 6), (-4, 26)]
NEST_SIZE = (288, 224)
NEST_MOUND = (220, 176)
FIRE = [(60, 8, 10), (170, 40, 12), (236, 110, 20), (255, 190, 60), (255, 240, 170)]


@functools.lru_cache
def nest_base(w, h):
    """The charred mound in its scorched, ember-flecked ring of ground (static under the fire)."""
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    yy, xx = np.mgrid[0:h, 0:w]
    tex = raster.fbm(w, h, 10, 983, octaves=3, period=False)
    d = np.hypot((xx - w / 2) / (w * 0.47), (yy - h / 2) / (h * 0.47))
    scorch = np.clip((1 - d) * 3 + (tex - 0.5), 0, 1)
    col = s6.ramp_img(GREYS, 0.12 + 0.18 * tex) * 0.6
    embers = (raster.fbm(w, h, 3, 984, octaves=1, period=False) > 0.78) & (d < 0.8)
    col[embers] = (200, 70, 20)
    img = l01.rgba(col, artkit.ordered_dither(scorch, 3) * 255)
    mound = b4.vrell(nest_scene, NEST_MOUND, factor=2, glow=0.6)
    a = np.array(mound).astype(float)
    lum = a[..., :3].mean(-1, keepdims=True)
    a[..., :3] = a[..., :3] * 0.3 + lum * np.array([0.42, 0.34, 0.4]) * 0.5    # charred chitin
    b4.place(img, Image.fromarray(a.astype(np.uint8), "RGBA"), w / 2, h / 2, shadow=14, opacity=0.6)
    return img


def burning_nest(w, h, n):
    """The mound with fire in its vents: per vent a flickering flame field, looping over n frames."""
    base = nest_base(w, h)
    yy, xx = np.mgrid[0:h, 0:w].astype(float)
    n1 = raster.fbm(w, h, 7, 991, octaves=3, period=False)
    n2 = raster.fbm(w, h, 7, 992, octaves=3, period=False)
    frames = []
    for i in range(n):
        ph = 2 * np.pi * i / n
        noise = 1.2 * ((n1 - 0.5) * np.cos(ph) + (n2 - 0.5) * np.sin(ph))
        heat = np.zeros((h, w))
        for k, (vx, vy) in enumerate(VENTS):
            cx, cy = w / 2 + vx, h / 2 - vy
            r = np.hypot(xx - cx, (yy - cy) * 1.15) / (16 + 2 * np.sin(ph * 2 + k))
            heat = np.maximum(heat, np.clip(1.25 - r + 0.8 * noise, 0, 1))
        img = base.copy()
        level = np.digitize(heat, [0.2, 0.4, 0.6, 0.8])
        fire = np.zeros((h, w, 4), np.uint8)
        for j in range(1, 5):
            fire[level == j] = FIRE[j] + (255,)
        fire[(level == 1)] = FIRE[1] + (150,)
        img.alpha_composite(Image.fromarray(fire, "RGBA"))
        frames.append(b4.border_clear(img))
    frames = artkit.quantize_set([artkit.stepped_alpha(f, 4) for f in frames], 32)
    return [b4.border_clear(f) for f in frames]


# --------------------------------------------------------------------------- jobs

L04_TILES = {k: b4.TILE_SETS[k] for k in ("plumes-light", "plumes-medium", "plumes-heavy", "ejecta-streaks")}
L04_PIECES = {k: b4.PIECES[k] for k in ("boulder", "boulder-growth-a", "boulder-growth-b", "rover-a", "rover-b",
                                         "pod-husk-a", "pod-husk-b")}
ANIMATED = {"burning-nest": burning_nest, "sled-run": b4.sled_run}   # sled-run: Level 04's generator


def layout():
    """The generated pieces' sizes and placements: {id: (w, h, [(t, x), ...])}."""
    out = {}
    for name, ((bottom, height), *_rest) in RIMS.items():
        out[name] = (W, height, [(round(t_at(bottom + height / 2 - MID), 4), W // 2)])
    for name, w, h, left, bottom, _ in battery_layout():
        out[name] = (w, h, [(round(t_at(bottom + h / 2 - MID), 4), left + w // 2)])
    nb = round(SEAM[5] - 250 - NEST_SIZE[1] - 20)          # its top 270 px under the far rim's crest
    out["burning-nest"] = (*NEST_SIZE, [(round(t_at(nb + NEST_SIZE[1] / 2 - MID), 4), 250)])
    return out


def check_layout(backdrop):
    bad = []
    for name, (w, h, places) in layout().items():
        spec = backdrop["pieces"].get(name)
        got = [(p["t"], p["x"]) for p in backdrop["placed"] if p["piece"] == name]
        print(f"  {name}: size [{w}, {h}], placed {', '.join(f't: {t}, x: {x}' for t, x in places)}")
        if spec is None or list(spec["size"]) != [w, h] or sorted(got) != sorted(places):
            bad.append(name)
    return bad


def jobs(backdrop, wanted):
    specs = {s[0]: s for s in battery_layout()}
    out = []
    for n, s in backdrop["tile_sets"].items():
        if n in L04_TILES:
            out.append((n, (W, s["height"]), None, None))
    for n, s in backdrop["pieces"].items():
        if n in L04_PIECES or n in ANIMATED or n in specs:
            out.append((n, tuple(s["size"]), s.get("frames"), specs.get(n)))
    return [j for j in out if not wanted or j[0] in wanted]


def render(job):
    name, (w, h), count, spec = job
    if count:
        images = ANIMATED[name](w, h, count)
    elif spec:
        images = [battery(w, h, spec)]
    else:
        images = [(L04_TILES.get(name) or L04_PIECES[name])(w, h)]
    for img in images:
        if img.size != (w, h):
            raise ValueError(f"{name}: rendered {img.size}, the data file says {(w, h)}")
    return images


def write(name, images, count=None):
    if count:
        for old in OUT.glob(f"{name}_*.png"):
            if old.stem.rsplit("_", 1)[0] == name and old.stem.rsplit("_", 1)[1].isdigit():
                old.unlink()
        for i, img in enumerate(images):
            artkit.save_png(img, OUT / f"{name}_{i}.png", SOURCE)
    else:
        artkit.save_png(images[0], OUT / f"{name}.png", SOURCE)
    print(f"{name}: {len(images)} image(s), {artkit.colour_count(images)} colours")


# --------------------------------------------------------------------------- review

def load(name, count=None, folder=None):
    folder = folder or OUT
    if count:
        return [Image.open(folder / f"{name}_{i}.png").convert("RGBA") for i in range(count)]
    return Image.open(folder / f"{name}.png").convert("RGBA")


def sprite_img(name):
    return Image.open(ROOT / "assets" / "sprites" / f"{name}.png").convert("RGBA")


def composite(t):
    """The play field at t as Backdrop draws it (ground tiles from their seams, ground pieces, the
    sled lamps on the rail until t = 37, the ground units, low-air banks, high-air at 40 % additive),
    with the frigate's bell in the arena for scale."""
    bd = LEVEL["backdrop"]
    scroll = scroll_at(t)
    sec = max(i for i, s in enumerate(STARTS) if t >= s)
    peak = SECTIONS[sec].get("peak")
    look = bd["atmosphere"][peak["atmosphere"] if peak and peak["from"] <= t < peak["to"] else SECTIONS[sec]["atmosphere"]]
    img = np.zeros((SCREEN, W, 3))

    def layer(name):
        factor = bd["scroll_factors"][name]
        pos0 = scroll * factor
        canvas = Image.new("RGBA", (W, SCREEN), (0, 0, 0, 0))
        for i, s in enumerate(SECTIONS):
            for tid in s.get("tiles", []):
                if bd["tile_sets"][tid]["layer"] != name:
                    continue
                tile = load(tid)
                lo = -1e9 if i == 0 else scroll_at(STARTS[i]) * factor + SCREEN
                hi = scroll_at(STARTS[i + 1]) * factor + SCREEN if i + 1 < len(SECTIONS) else 1e12
                for y in range(SCREEN):
                    pos = pos0 + (SCREEN - 1 - y)
                    if lo <= pos < hi:
                        r = int(np.floor(pos)) % H
                        canvas.paste(tile.crop((0, H - 1 - r, W, H - r)), (0, y))
        for p in bd["placed"]:
            spec = bd["pieces"][p["piece"]]
            if spec["layer"] != name:
                continue
            pw, ph = spec["size"]
            bottom = round(scroll_at(p["t"]) * factor + MID - ph / 2)
            y = SCREEN - (bottom - round(pos0)) - ph
            if y >= SCREEN or y + ph <= 0:
                continue
            frames = spec.get("frames")
            im = load(p["piece"], frames)[int(t * spec["fps"]) % frames] if frames else load(p["piece"])
            sprite.paste(canvas, im, round(p["x"] - pw / 2), y)
        if name == "ground":
            draw_sled(canvas, t, scroll)
            draw_units(canvas, scroll)
        return canvas

    def blend(canvas, add=False, scale=1.0):
        a = np.array(canvas).astype(np.float64)
        al = a[..., 3:4] / 255 * scale
        if add:
            img[:] = np.minimum(255, img + a[..., :3] * al)
        else:
            img[:] = img * (1 - al) + a[..., :3] * al

    blend(layer("ground"))
    low = layer("low-air")
    if look.get("banks"):
        banks = load(look["banks"])
        shift = int(round(bd["tile_sets"][look["banks"]].get("drift", 0) * t)) % W
        banks = Image.fromarray(np.roll(np.array(banks), shift, axis=1), "RGBA")
        pos0 = scroll * bd["scroll_factors"]["low-air"]
        for y in range(SCREEN):
            pos = int(pos0 + SCREEN - 1 - y) % banks.height
            low.alpha_composite(banks.crop((0, banks.height - 1 - pos, W, banks.height - pos)), (0, y))
    blend(low)
    blend(layer("high-air"), add=True, scale=0.4)
    out = Image.fromarray(img.astype(np.uint8), "RGB").convert("RGBA")
    if 150 <= t < 205:
        sprite.paste_center(out, sprite_img("gorgon-frigate-bell_0"), W / 2, 110)
    return out.convert("RGB")


def draw_sled(canvas, t, scroll):
    """The sled-run lamp frame over the rail, as LunaLooks draws it (until 2 s after the last sled)."""
    sleds = LEVEL["sleds"]
    if t > sleds["until"] + 2:
        return
    frame = load("sled-run", 30)[25 if abs((t - sleds["first"]) % sleds["period"]) < 0.4 else int(t * 10) % 14]
    offset = int(round(scroll)) % 60
    for yb in range(-offset, SCREEN, 480):
        sprite.paste(canvas, frame, RAIL_X - frame.width // 2, SCREEN - yb - frame.height)


def draw_units(canvas, scroll):
    for g in LEVEL["ground_targets"]:
        look = {"spine-turret": "spine-turret_0", "polyp-mortar": "polyp-mortar_0"}.get(g.get("enemy"))
        if g.get("target") == "stuck sled":
            look, hb = "ore-canister_0", g["size"][1]
        elif look:
            hb = hitbox(g["enemy"])[1]
        else:
            continue
        for te, x in g["at"]:
            p = scroll_at(te) + SCREEN + hb / 2
            y = SCREEN - (p - scroll)
            if -40 < y < SCREEN + 40:
                sprite.paste_center(canvas, sprite_img(look), x, y)


TIMES = [(14, "1 THE RAIL"), (31.6, "1 SLED, STUCK CANISTER"), (36.5, "2 OUTER RIM, RAIL'S LIP"),
         (40.6, "2 RIM CREST, TURRETS"), (56.3, "2 BATTERY A"), (89.3, "3 BATTERY B"),
         (109.2, "3 BATTERY C"), (118, "3 HEAVY PEAK"), (136.2, "4 BATTERY D"), (153, "5 ARENA, FRIGATE"),
         (180, "5 ARENA, CALM FLOOR"), (204.9, "5 ARENA END, NEST BURNS"), (206.5, "6 LIFT-OFF, FAR RIM"),
         (211, "6 BEYOND THE RIM")]


def review():
    backdrop = LEVEL["backdrop"]
    items = []
    for name, spec in list(backdrop["tile_sets"].items()) + list(backdrop["pieces"].items()):
        count = spec.get("frames")
        im = load(name, count)
        im = im[0] if count else im
        scale = min(1.0, 220 / max(im.size))
        im = im.resize((max(1, int(im.width * scale)), max(1, int(im.height * scale))), Image.NEAREST)
        items.append((f"{name} {spec['layer']}", im, spec.get("layer") == "high-air"))
    comps = [(f"T={t:g} {label}", composite(t)) for t, label in TIMES]
    width, x, y, row_h = 1460, 16, 44, 0
    rows = []
    for name, im, add in items:
        cell = max(im.width, 6 * len(name))
        if x + cell > width - 16:
            x, y, row_h = 16, y + row_h + 30, 0
        rows.append((name, im, add, x, y))
        x += cell + 14
        row_h = max(row_h, im.height)
    comp_y = y + row_h + 40
    cw = W // 2 + 14
    per_row = (width - 16) // cw
    n_rows = (len(comps) + per_row - 1) // per_row
    artkit.REVIEW_ROUND = "r22"
    sheet = raster.sheet(width, comp_y + n_rows * (270 + 30) + 20, "LEVEL 05 BACKDROP - FINAL PIECES (PROPOSAL)",
                         "PRODUCTION ART - R22; COMPOSITES AT 1/2 SCALE WITH THE GROUND UNITS, THE SLED LAMPS AND THE BELL")
    for name, im, add, px, py in rows:
        plate = Image.new("RGBA", im.size, artkit.PLATE)
        plate = artkit.add_light(plate, im) if add else (plate.alpha_composite(im) or plate)
        sheet.alpha_composite(plate, (px, py + 14))
        raster.draw_text(sheet, px, py, name.upper(), raster.LABEL)
    for k, (label, im) in enumerate(comps):
        px, py = 16 + (k % per_row) * cw, comp_y + (k // per_row) * 300
        sheet.alpha_composite(im.convert("RGBA").resize((240, 270), Image.BOX), (px, py + 14))
        raster.draw_text(sheet, px, py, label, raster.LABEL)
    REVIEW.parent.mkdir(parents=True, exist_ok=True)
    sheet.convert("RGB").save(REVIEW, optimize=True)
    print(f"review: {REVIEW.relative_to(ROOT)}")


def atlas_area(backdrop):
    area = sum(W * s["height"] for s in backdrop["tile_sets"].values())
    area += sum(s["size"][0] * s["size"][1] * s.get("frames", 1) for s in backdrop["pieces"].values())
    print(f"atlas area: {area:,} px = {area / 2048 ** 2:.2f} pages of 2048x2048")


def main(argv):
    backdrop = LEVEL["backdrop"]
    wanted = {a for a in argv if not a.startswith("--")}
    print("layout (from the data's scroll and ground targets):")
    bad = check_layout(backdrop)
    if "--layout" in argv:
        return
    if bad:
        raise SystemExit(f"the data file's size or placement differs for {bad}: copy the layout above")
    known = set(GROUND_TILES) | set(RIMS) | set(L04_TILES) | set(L04_PIECES) | set(ANIMATED) | set(layout())
    missing = (backdrop["tile_sets"].keys() | backdrop["pieces"].keys()) - known
    if missing or wanted - known:
        raise SystemExit(f"no generator for {sorted(missing | (wanted - known))}")
    if "--review" not in argv:
        OUT.mkdir(parents=True, exist_ok=True)
        todo = jobs(backdrop, wanted)
        with ProcessPoolExecutor() as pool:
            ground = pool.submit(ground_group) if not wanted or wanted & (set(GROUND_TILES) | set(RIMS)) else None
            for job, images in zip(todo, pool.map(render, todo)):
                write(job[0], images, job[2])
            if ground is not None:
                for name, img in ground.result().items():
                    size = (W, backdrop["tile_sets"][name]["height"]) if name in GROUND_TILES else \
                        tuple(backdrop["pieces"][name]["size"])
                    if img.size != size:
                        raise ValueError(f"{name}: rendered {img.size}, the data file says {size}")
                    write(name, [img])
    atlas_area(backdrop)
    review()


if __name__ == "__main__":
    main(sys.argv[1:])
