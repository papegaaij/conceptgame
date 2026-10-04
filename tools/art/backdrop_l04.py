#!/usr/bin/env python3
"""Production art PROPOSAL: the Level 04 backdrop, the convoy road across Mare Tranquillitatis
from Tranquility Base to the mass-driver terminal (design/campaign/act-1-first-contact/
level-04-tranquility-run; M4 part D batch, concept round 17).

Outputs (assets/backdrop/level-04/, one PNG per tile set and set piece of the level's `backdrop`
block, named by its id and checked against its size; frames as <id>_<n>.png):
  mare-a, mare-b                    ground: grey mare regolith, two variants (sections 1, 2)
  rille-rims                        ground: mare-b with the sinuous rille cut through it, the
                                    channel transparent (section 3)
  crater-growth                     ground: the crater field with the first teal Vrell roots (section 4)
  mass-driver-field                 ground: cratered mare with the mass-driver rail (section 5)
  rille-floor                       far: the rille floor in earthshine, under the channel (sections 2-3)
  plumes-light/-medium/-heavy       low-air: regolith plumes (the atmosphere's banks; heavy = the
                                    lander impact of the peak)
  ejecta-streaks                    high-air: ejected rock streaking past (section 4)
  convoy-apron, hab-domes-a/-b,     ground, section 1: the convoy's marshalling apron, pressure
  heritage-dome, landing-pad        domes, the Apollo 11 heritage dome (the Eagle's descent stage
                                    under glass), a landing pad
  rover-a, rover-b, boulder,        ground, sections 2-3: abandoned rovers, boulders, boulders with a
  boulder-growth-a/-b               Vrell growth socket on top (the Spine Turrets' bases)
  rille-end-south, rille-end-north  ground: the rille's two ends, laid over the section seams
  road-bridge, road-bridge-arches   ground: the road bridge across the rille, turned to the road:
                                    the deck under the convoy, and the overhead arches (placed with
                                    `overhead: true`) the convoy passes under, the Spine Turrets on top
  pod-husk-a, pod-husk-b            ground, section 4: split pod-lander husks in their impact craters
  rail-head, sled-run_0..29,        ground, section 5: the mass driver's loading station over the
  terminal-gate, terminal-gate-roof rail's start, the sled overlay (lights and the sled shot), the
                                    terminal's vehicle hangar (380x300, its door on the road's end):
                                    the apron with its static landing lights under the convoy, and
                                    the hangar as an overhead piece the crawlers drive in under
  road-texture                      56x192, not a set piece: the road ribbon's texture (below)
  design/campaign/.../level-04-tranquility-run/concept/backdrop-final-r17-a.png   review sheet with
                                    composite frames of the level at chosen times

The backdrop block (and the `road`) come from the level's data.yaml.

**The ground tiles share one terrain.** Every ground tile set is 480x960 and built on the same
periodic base terrain (the chosen scene's fbm regolith, craters, low-sun cast shadows and the
blue earthshine tint in the shadows, one shared palette); a tile set only adds its features, none
within 30 px of the rows where its section seams fall (tiles repeat from layer position 0, so a
seam's row is fixed: 660, 300, 300 and 900 px above a tile's bottom; a crater faded or clipped
there would end halfway, and the roots fail the run if they reach one). Every tile set wraps top
to bottom: each noise octave's lattice divides 960 (pfbm). game/.../BackdropSeamsTest checks the
tiles' wrap rows and the rows at the set borders. Seams between ground tile sets are
therefore invisible except where a feature crosses one on purpose: the rille's channel (covered by
the rille-end pieces) and the rail (covered by the rail head). Changing a section's start time
moves a seam: rerun and look at the composite.

**The rille.** Section 3's ground tiles are opaque except the channel, centre
x = 268 + 18 sin(2pi u/960 + 0.6) + 8 sin(4pi u/960 + 2.0) (u = px above the tile's bottom),
transparent 30 px either side, walls to 46 px, rims to ~56. The far layer's rille floor shows
through it; it is listed in sections 2 and 3 so it already lies under the channel when the
channel's lower end enters (the far layer scrolls slower than the ground).

**The road texture's contract** (road-texture.png, 56x192): the game draws the road as a ribbon
56 px wide along the level's `road` curve on the ground layer, after the ground tiles and before
the ground set pieces (the apron, the bridge and the gate lie over it). The texture's x spans the
ribbon's width across the road (column 28 on the curve), perpendicular to the curve; its v runs
along the road, repeating every 192 px of arc length, anchored to the ground (v = arc length from
the road's first point, so the ruts never swim); the image's bottom row comes first, as in the
tile sets (v grows up the screen). Its edge columns are dithered 1-bit alpha (regolith berms), so
the ribbon needs no blending; nearest filtering, whole pixels. The texture is not mirrored or
turned: it bends at most 30 degrees with the road.

Kit: the chosen scene's helpers (tools/concept/scenes_r06.py: hillshade, cast shadows, periodic
filtering, its grey ramp and dust tones), Level 01's station kit (tools/art/backdrop_l01.py,
loaded by path; domes, trusses, cargo, radiators in palette B) and the Vrell materials of the
enemy models; posterized to 12-32 colours, translucency stepped, wide gradients ordered-dithered.
Nothing lit is rotated or mirrored as an image: the bridge and the rovers are turned in the model.

Run: python3 tools/art/backdrop_l04.py [--review] [id ...]   (~5 min on 20 cores: the hangar roof alone
takes ~4 min on one core; --review alone a few seconds)
"""
import functools
import importlib.util
import sys
from concurrent.futures import ProcessPoolExecutor
from pathlib import Path

import numpy as np
import yaml
from PIL import Image, ImageDraw

import artkit
from artkit import DESIGN, ROOT, raster, sprite

import parallax_r03 as r03  # noqa: E402  (concept scripts, imported unchanged)
import scenes_r06 as s6  # noqa: E402
from parallax_r02 import posterize  # noqa: E402
from render import enemy_models as em, sdf, station  # noqa: E402
from render.scene_models import PaletteShim, rock_sprites  # noqa: E402
from render.sdf import (rotate_y, rotate_z, sd_box, sd_capsule, sd_cylinder_x, sd_cylinder_y, sd_cylinder_z,  # noqa: E402
                        sd_ellipsoid, sd_sphere, union)
from render.station import ACCENT, DARK, GLASS, HULL, RED, SOLAR, WARM  # noqa: E402


def _load(name, file):
    spec = importlib.util.spec_from_file_location(name, Path(__file__).resolve().parent / file)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


b1 = _load("art_backdrop_l01", "backdrop_l01.py")
l01 = b1.l01

SCRIPT = "backdrop_l04.py"
SOURCE = artkit.source_note(SCRIPT, "M4 part D batch (proposal)")
LEVEL_DIR = DESIGN / "campaign" / "act-1-first-contact" / "level-04-tranquility-run"
OUT = ROOT / "assets" / "backdrop" / "level-04"
REVIEW = LEVEL_DIR / "concept" / "backdrop-final-r17-a.png"
W, H = 480, 960                        # every tile set
SPEED = 120.0                          # px/s, the level's scroll
MID = 270                              # half the play field: a point passes the middle at pos - MID
GREYS = s6.LunaScene.GREYS
EARTHSHINE = np.array((14, 18, 34), float)
SEAMS = {"mare-a": (660,), "mare-b": (660, 300), "rille-rims": (660, 300), "crater-growth": (300, 900),
         "mass-driver-field": (900,)}  # u of the seams next to each section's tiles
RAIL_X = 432
CHITIN, TEAL = (122, 44, 122), (0, 255, 154)
WARM_GLASS = PaletteShim(l01.KIT_PAL, {("EARTH ORBIT", 3): (120, 150, 200)})
GATE_PAL = PaletteShim(l01.KIT_PAL, {("UTC ACCENTS", 4): (226, 164, 40), ("UTC ACCENTS", 5): (255, 176, 84)})
GOLD = PaletteShim(l01.KIT_PAL, {("UTC ACCENTS", 4): (214, 166, 58), ("EARTH ORBIT", 2): (60, 80, 130)})


# --------------------------------------------------------------------------- helpers

def rows_u():
    """u (px above the tile's bottom) of every image row."""
    return (H - 1 - np.arange(H))[:, None].astype(np.float64)


def channel_x(u):
    return 268 + 18 * np.sin(2 * np.pi * u / H + 0.6) + 8 * np.sin(4 * np.pi * u / H + 2.0)


def seam_mask(seams, inner=30):
    """1 where a tile's features may lie, fading to 0 within ``inner`` to ``inner`` + 60 px of
    its seams."""
    u = rows_u()
    m = np.ones_like(u)
    for s in seams:
        d = np.abs(u - s) % H
        d = np.minimum(d, H - d)
        m = np.minimum(m, np.clip((d - inner) / 60, 0, 1))
    return np.broadcast_to(m, (H, W))


def border_clear(img):
    """A ground piece's outermost pixels transparent (the opaque-layer edge rule)."""
    a = np.array(img)
    a[0, :, 3] = a[-1, :, 3] = a[:, 0, 3] = a[:, -1, 3] = 0
    a[a[..., 3] == 0] = 0
    return Image.fromarray(a, "RGBA")


def long_shadow(spr, length, opacity=0.55):
    """The low sun's long shadow down-right: the silhouette smeared along (0.6, 0.8)."""
    a = np.array(spr.getchannel("A")) > 127
    h, w = a.shape
    pad = int(length) + 2
    out = np.zeros((h + pad, w + pad), bool)
    for k in range(int(length) + 1):
        dx, dy = int(round(0.6 * k)), int(round(0.8 * k))
        out[dy:dy + h, dx:dx + w] |= a
    img = Image.new("RGBA", out.shape[::-1], (0, 0, 0, 0))
    img.putalpha(Image.fromarray((out * 255 * opacity).astype(np.uint8)))
    return img


def place(canvas, spr, x, y, shadow=0, opacity=0.55):
    """Paste a sprite centred at (x, y) with its long shadow under it."""
    if shadow:
        sh = long_shadow(spr, shadow, opacity)
        sprite.paste(canvas, sh, x - spr.width / 2, y - spr.height / 2)
    sprite.paste_center(canvas, spr, x, y)


def finish(img, colours=32):
    return border_clear(artkit.quantize_set([artkit.stepped_alpha(img, 4)], colours)[0])


def render_model(scene, pal, size, factor=4, colors=48):
    return station.render_part((scene, size), pal, factor=factor, colors=colors)


def vrell(scene, size, factor=3, glow=1.4):
    w, h = size
    hi = sdf.render(scene, em.vrell_scheme_mats("skitter", "a", glow), (w * factor, h * factor), float(w),
                    z_top=60.0, steps=110)
    return sprite.make_sprite(hi, factor, 24, crisp=60)


def roots(canvas, origin, seed, count, length, width=3.0, limit=None):
    """The scene's Vrell roots: plum chitin strands with teal cores, branching (2D)."""
    ss = 2
    w, h = canvas.size
    lay = Image.new("RGBA", (w * ss, h * ss), (0, 0, 0, 0))
    d = ImageDraw.Draw(lay)
    rng = np.random.default_rng(seed)

    def branch(x, y, ang, length, width, depth):
        pts = [(x, y)]
        for _ in range(int(length / 5)):
            ang += rng.uniform(-0.35, 0.35)
            x, y = x + 5 * np.cos(ang), y + 5 * np.sin(ang)
            pts.append((x, y))
        q = [(px * ss, py * ss) for px, py in pts]
        d.line(q, fill=CHITIN + (255,), width=max(2, int(width * ss)))
        d.line(q, fill=TEAL + (255,), width=max(1, int(width * 0.4 * ss)))
        if depth > 0:
            for _ in range(2):
                k = int(rng.integers(len(pts) // 3, len(pts)))
                branch(*pts[k], ang + rng.uniform(-1.0, 1.0), length * 0.6, width * 0.65, depth - 1)
    for a in np.linspace(0, 2 * np.pi, count, endpoint=False):
        branch(origin[0], origin[1], a + rng.uniform(-0.3, 0.3), rng.uniform(0.6, 1.0) * length, width, 2)
    lay = lay.resize((w, h), Image.BOX)
    a = np.array(lay)
    a[..., 3] = np.where(a[..., 3] > 110, 255, 0)
    if limit is not None:
        a[..., 3] = np.where(limit, a[..., 3], 0)
    lay = Image.fromarray(a, "RGBA")
    canvas.alpha_composite(sprite.shadow_of(lay, opacity=0.45, blur=0.8), (2, 3))
    canvas.alpha_composite(lay)
    return canvas


# --------------------------------------------------------------------------- ground terrain

def pfbm(w, h, cell, seed, octaves=5, gain=0.5):
    """raster.fbm, but periodic top to bottom at every octave: raster.fbm halves the lattice per
    octave, and a lattice that does not divide ``h`` (120 >> 4 = 7 in 960) does not wrap, which left
    a 1 px line where the tiles repeat. Each octave's lattice is the divisor of ``h`` nearest to the
    halved cell (the larger on a tie); octaves whose cell divides ``h`` are raster.fbm's exactly."""
    out = np.zeros((h, w))
    amp, total = 1.0, 0.0
    for o in range(octaves):
        c = max(1, cell >> o)
        c = min((d for d in range(1, h + 1) if h % d == 0), key=lambda d: (abs(d - c), -d))
        out += amp * raster.value_noise(w, h, c, seed + o * 101, True)
        total += amp
        amp *= gain
    out /= total
    lo, hi = out.min(), out.max()
    return (out - lo) / max(hi - lo, 1e-9)


def crater(hgt, cx, cy, r, depth=0.55):
    yy, xx = np.mgrid[0:hgt.shape[0], 0:hgt.shape[1]]
    d = np.hypot(xx - cx, s6.periodic_dist(yy, cy, hgt.shape[0])) / r
    bowl = np.where(d < 1, -(1 - d * d) * depth, 0)
    rim = np.exp(-((d - 1) / 0.16) ** 2) * 0.2
    return (bowl + rim) * min(1.0, r / 26)


@functools.lru_cache
def base_height():
    """The shared terrain: the scene's fbm regolith with small craters, periodic in 960."""
    hgt = pfbm(W, H, 120, 622, octaves=5) * 0.5 + pfbm(W, H, 24, 623, octaves=1) * 0.05
    rng = np.random.default_rng(621)
    for _ in range(46):
        hgt += crater(hgt, rng.uniform(0, W), rng.uniform(0, H), rng.uniform(4, 11))
    return hgt


CLEAR = 30     # px a tile set's features keep from its seams (their cast shadows reach ~28 px)


def seam_distance(row, seams):
    """Distance (px, wrapping) from image row ``row`` to the nearest seam row (u values)."""
    if not seams:
        return np.inf
    return min(min(abs(row - (H - 1 - s)) % H, H - abs(row - (H - 1 - s)) % H) for s in seams)


def features(seed, craters, big, rocks, seams=()):
    """Craters and rocks of a tile set. One that would reach within CLEAR px of one of ``seams``
    (its rim at 1.4 radii, a rock at 3) is left out: a feature faded or clipped at a seam row ends
    halfway where the next section's tiles begin."""
    rng = np.random.default_rng(seed)
    f = np.zeros((H, W))
    for _ in range(craters):
        x, y, r = rng.uniform(0, W), rng.uniform(0, H), rng.uniform(*big)
        if seam_distance(y, seams) > 1.4 * r + CLEAR:
            f += crater(f, x, y, r)
    yy, xx = np.mgrid[0:H, 0:W]
    for _ in range(rocks):
        x, y, r = rng.uniform(0, W), rng.uniform(0, H), rng.uniform(1.5, 3.5)
        if seam_distance(y, seams) > 3 * r + CLEAR:
            f += 0.12 * np.exp(-(xx - x) ** 2 / (2 * r * r) - s6.periodic_dist(yy, y, H) ** 2 / (2 * r * r))
    return f


def assert_clear(overlay, seams, name):
    """Fail when an overlay (roots) reaches within CLEAR px of a seam row: it would end halfway."""
    rows = np.nonzero(np.array(overlay)[..., 3].any(axis=1))[0]
    near = [int(r) for r in rows if seam_distance(r, seams) <= CLEAR]
    if near:
        raise ValueError(f"{name}: the overlay reaches rows {near[0]}..{near[-1]}, within {CLEAR} px of "
                         f"a seam (u {seams}); move it")
    return overlay


@functools.lru_cache
def tile_heights():
    """Height field of every ground tile set (and the rille's channel depth)."""
    base = base_height()
    out = {
        "mare-a": base + features(701, 7, (16, 30), 30, SEAMS["mare-a"]),
        "mare-b": base + features(711, 6, (14, 34), 34, SEAMS["mare-b"]),
        "crater-growth": base + features(721, 16, (18, 52), 22, SEAMS["crater-growth"]),
    }
    xx = np.arange(W)[None, :].astype(np.float64)
    rail = np.clip(1 - (np.abs(xx - RAIL_X) - 24) / 6, 0, 1)
    md = base + features(731, 5, (14, 26), 24, SEAMS["mass-driver-field"])
    out["mass-driver-field"] = md * (1 - rail) + (md * 0.3 + 0.1) * rail
    out["rille-rims"] = out["mare-b"]
    return out


def channel(u_from=None, u_to=None):
    """(open, wall, rim) fields of the rille; the channel may begin above ``u_from`` or end below
    ``u_to`` with a rounded end (the rille-end pieces)."""
    u = rows_u()
    xx = np.arange(W)[None, :].astype(np.float64)
    d = np.abs(xx - channel_x(u))
    wobble = 3 * pfbm(W, H, 8, 741, octaves=2)
    scale = np.ones_like(u)
    if u_from is not None:
        scale = np.minimum(scale, np.clip((u - u_from) / 60, 0, 1))
    if u_to is not None:
        scale = np.minimum(scale, np.clip((u_to - u) / 60, 0, 1))
    scale = np.sqrt(scale)
    half = 30 * scale + wobble * (scale > 0)
    wall = 46 * scale + wobble * (scale > 0)
    opening = (d < half) & (scale > 0.05)
    in_wall = (d < wall) & ~opening & (scale > 0.05)
    rim = 0.1 * np.exp(-((d - wall - 6) / 6) ** 2) * scale
    return opening, in_wall, rim, xx < channel_x(u)


def shade_terrain(hgt):
    """The scene's colouring: grey ramp by height (fixed range: every tile alike), hillshade, long
    cast shadows, the earthshine tint in the shadows."""
    base = base_height()
    lo, hi = base.min() - 0.15, base.max() + 0.1
    shade = s6.hillshade(hgt * 60, 0.55)
    lit = s6.cast_shadows(hgt * 60, steps=28, drop=1.0)
    t = np.clip((hgt - lo) / (hi - lo), 0, 1)
    col = s6.ramp_img(GREYS, 0.25 + t * 0.6) * (shade * (0.35 + 0.65 * lit))[..., None]
    return (col + EARTHSHINE * (1 - lit)[..., None] * 0.9) * 0.8


def rille_colour(col, ch):
    """Walls of the channel: the west wall (facing away from the sun) in shadow and earthshine,
    the east wall lit; the open channel transparent."""
    opening, in_wall, rim, west = ch
    col = col.copy()
    dark = np.array(GREYS[0][1], float) + EARTHSHINE * 1.4
    col[in_wall & west] = dark
    lit_wall = s6.ramp_img(GREYS, np.full(col.shape[:2], 0.62)) * 0.82
    col[in_wall & ~west] = lit_wall[in_wall & ~west]
    alpha = np.where(opening, 0, 255)
    return col, alpha


def dithered(col):
    """A Bayer offset before the shared median cut, so wide shading breaks into fine grain."""
    h, w = col.shape[:2]
    b = np.tile(l01.BAYER4, (h // 4 + 1, w // 4 + 1))[:h, :w]
    return np.clip(col + ((b - 0.5) * 6)[..., None], 0, 255)


def overlaid(terrain, overlay):
    """An overlay (roots, the rail) with its translucent shadow over the quantized terrain, every
    changed pixel snapped to the terrain's and the overlay's colours (so the tile keeps <= 32)."""
    base = np.array(terrain)
    out = terrain.copy()
    out.alpha_composite(overlay)
    a = np.array(out)
    pal = np.unique(np.concatenate([base[..., :3].reshape(-1, 3), np.array(overlay)[..., :3][np.array(overlay)[..., 3] == 255]]), axis=0)
    changed = np.any(a != base, axis=-1)
    px = a[changed][:, :3].astype(np.int32)
    nearest = np.argmin(((px[:, None, :] - pal[None, :, :].astype(np.int32)) ** 2).sum(-1), axis=1)
    a[changed, :3] = pal[nearest]
    return Image.fromarray(a, "RGBA")


def crater_growth_overlay():
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    for (x, y), seed in zip(((90, 200), (380, 470), (150, 820)), (751, 752, 753)):
        for dy in (-H, 0, H):
            roots(img, (x, y + dy), seed, 5, 70, 3.0)
    return assert_clear(img, SEAMS["crater-growth"], "crater-growth roots")


def rail_overlay():
    """The scene's mass-driver rail: ties, two rails, pylons every 60 px with lamp housings."""
    rail = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    rd = ImageDraw.Draw(rail)
    x0 = RAIL_X
    for y in range(0, H, 10):
        rd.rectangle([x0 - 13, y, x0 + 13, y + 3], fill=(84, 84, 92, 255))
    for dx in (-8, 8):
        rd.rectangle([x0 + dx - 2, 0, x0 + dx + 2, H], fill=(150, 152, 164, 255))
        rd.line([x0 + dx - 1, 0, x0 + dx - 1, H], fill=(214, 216, 226, 255))
    for u in range(20, H, 60):
        y = H - 1 - u
        rd.rectangle([x0 - 20, y - 6, x0 + 20, y + 6], fill=(110, 112, 122, 255), outline=(60, 60, 68, 255))
        rd.rectangle([x0 - 19, y - 5, x0 + 19, y - 3], fill=(170, 172, 182, 255))
        for dx in (-17, 17):
            rd.rectangle([x0 + dx - 1, y - 1, x0 + dx + 1, y + 1], fill=(70, 40, 24, 255))
    out = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    out.alpha_composite(s6.pshadow(rail, 0.6, 0.8), (6, 8))
    out.alpha_composite(rail)
    return out


CAPS = {"rille-end-south": (300, "south"), "rille-end-north": (300, "north")}


@functools.lru_cache
def ground_group(caps_placed):
    """Every ground tile set and the two rille ends, quantized to one terrain palette. A rille end
    is cut from a render of the terrain with the channel ending, at exactly the tile rows and
    columns it is placed over (``caps_placed``: (name, t, x, w, h)), so outside the channel it is
    pixel for pixel the tiles under it."""
    hs = tile_heights()
    terr, alphas = {}, {}
    for name, hgt in hs.items():
        if name == "rille-rims":
            ch = channel()
            col, alpha = rille_colour(shade_terrain(hgt + ch[2]), ch)
        else:
            col, alpha = shade_terrain(hgt), np.full((H, W), 255)
        terr[name], alphas[name] = dithered(col), alpha
    # the rille's ends, rendered over the seam they cover (same terrain on both sides of it)
    caps = {}
    for name, (u_seam, end) in CAPS.items():
        if end == "south":
            ch = channel(u_from=u_seam + 20)
            hgt = hs["rille-rims"]
        else:
            ch = channel(u_to=u_seam - 20)
            hgt = np.where(rows_u() >= u_seam, hs["crater-growth"], hs["rille-rims"])
        col, alpha = rille_colour(shade_terrain(hgt + ch[2]), ch)
        caps[name] = (dithered(col), alpha, u_seam)
    names = list(terr) + list(caps)
    imgs = [l01.rgba(terr[n], alphas[n]) for n in terr] + [l01.rgba(c, a) for c, a, _ in caps.values()]
    quant = dict(zip(names, artkit.quantize_set(imgs, 22)))
    for name, overlay in (("crater-growth", crater_growth_overlay()), ("mass-driver-field", rail_overlay())):
        quant[name] = overlaid(quant[name], artkit.quantize_set([overlay], 10)[0])
    out = {n: quant[n] for n in terr}
    for name, t, x, cw, chh in caps_placed:
        bottom = round(t * SPEED + MID - chh / 2) % H          # the layer position of its bottom row
        y0 = H - bottom - chh
        x0 = int(round(x - cw / 2))
        if y0 < 0:
            raise ValueError(f"{name}: crosses the tile's top row; move it")
        crop = quant[name].crop((x0, y0, x0 + cw, y0 + chh))
        a = np.array(crop)
        yy, xx = np.mgrid[0:chh, 0:cw]
        edge = np.minimum.reduce([xx, yy, cw - 1 - xx, chh - 1 - yy]).astype(float)
        keep = artkit.ordered_dither(np.clip((edge - 1) / 10, 0, 1), 2) > 0.5
        a[..., 3] = np.where(keep, a[..., 3], 0)
        a[a[..., 3] == 0] = 0
        out[name] = Image.fromarray(a, "RGBA")
    return out


# --------------------------------------------------------------------------- far, low-air, high-air

def rille_floor(w, h):
    """The rille floor far below the rims: rubble in the wall's shadow, lit only by earthshine."""
    hgt = pfbm(w, h, 60, 761, octaves=4) * 0.4
    rng = np.random.default_rng(762)
    for _ in range(60):
        hgt += crater(hgt, rng.uniform(0, w), rng.uniform(0, h), rng.uniform(2, 7), depth=-0.6)
    shade = s6.hillshade(hgt * 60, 0.5)
    lit = s6.cast_shadows(hgt * 60, steps=14, drop=1.0)
    t = (hgt - hgt.min()) / (hgt.max() - hgt.min())
    col = s6.ramp_img(GREYS, 0.12 + t * 0.4) * (shade * (0.55 + 0.45 * lit))[..., None] * 0.55
    col = col + EARTHSHINE * 1.3
    return artkit.quantize_set([l01.rgba(dithered(col), np.full((h, w), 255))], 16)[0]


DUST = [(70, 68, 70), (104, 101, 102), (140, 136, 134), (176, 172, 168)]


def plumes(share, max_alpha, seed):
    """Regolith plumes (the scene's dust banks) covering about ``share`` of the screen, tiling in
    x and y so they can drift."""
    def tile(w, h):
        n = r03.noise(w, h, [160, 80, 32], seed, gains=[1.0, 0.5, 0.3])
        soft = 0.12
        cover = np.quantile(n, 1 - share) - soft / 2
        arr = l01.wrap_padded(lambda big: r03.bank(big, cover, soft, DUST, max_alpha=max_alpha, light=7.0), n)
        img = r03.step_alpha(posterize(Image.fromarray(arr, "RGBA"), 12), 6)
        return img
    return tile


def ejecta_streaks(w, h):
    """Ejected rock streaking past over the play plane (high-air, drawn additively at 40 %):
    grey fragments with a bright head and a fading tail up the screen."""
    arr = np.zeros((h, w, 4))
    rng = np.random.default_rng(771)
    for _ in range(26):
        x, y, length = rng.uniform(4, w - 4), int(rng.uniform(0, h)), int(rng.uniform(14, 40))
        sway = rng.uniform(-0.15, 0.15)
        for i in range(length):
            xi = int(round(x + sway * i)) % w
            arr[(y - i) % h, xi] = (190, 186, 182, (1 - i / length) * rng.uniform(0.4, 0.8))
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
            arr[(y + dy) % h, (int(x) + dx) % w] = (232, 228, 222, 1.0)
    return artkit.stepped_alpha(l01.rgba(arr[..., :3], arr[..., 3] * 255), 9)


# --------------------------------------------------------------------------- section 1

def paved(w, h, x0, y0, x1, y1, seed):
    """A paved slab: grey concrete with joints, bevelled edge, dust drifting over its border."""
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([x0, y0, x1, y1], fill=(92, 92, 100, 255))
    for x in range(x0 + 24, x1, 24):
        d.line([x, y0, x, y1], fill=(76, 76, 84, 255))
    for y in range(y0 + 24, y1, 24):
        d.line([x0, y, x1, y], fill=(76, 76, 84, 255))
    d.line([x0, y0, x1, y0], fill=(140, 140, 148, 255))
    d.line([x0, y0, x0, y1], fill=(140, 140, 148, 255))
    d.line([x0, y1, x1, y1], fill=(54, 54, 62, 255))
    d.line([x1, y0, x1, y1], fill=(54, 54, 62, 255))
    a = np.array(img).astype(np.float64)
    dust = raster.fbm(w, h, 16, seed, octaves=3, period=False)
    yy, xx = np.mgrid[0:h, 0:w]
    edge = np.minimum.reduce([xx - x0, yy - y0, x1 - xx, y1 - yy]).astype(float)
    cover = np.clip((dust - 0.35) * 2 - edge / 14, 0, 1) * (a[..., 3] > 0)
    rgo = s6.ramp_img(GREYS, 0.55 + dust * 0.2)
    a[..., :3] = a[..., :3] * (1 - cover[..., None]) + rgo * cover[..., None]
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


def lamp_post():
    def scene(p):
        return union((sd_cylinder_z(p, (0, 0, 0), 2.2, 10), DARK), (sd_box(p, (0, 0, 11), (5, 3, 1.5), 0.5), HULL),
                     (sd_box(p, (0, -1.5, 12), (3.5, 1, 0.8), 0.3), WARM))
    return render_model(scene, l01.KIT_PAL, (14, 12), factor=6, colors=16)


def light_pool(img, x, y, r, strength=0.22, colour=(255, 196, 110)):
    """Warm lamplight on the ground: a translucent pool, alpha stepped with ordered dither."""
    w, h = img.size
    yy, xx = np.mgrid[0:h, 0:w]
    v = np.clip(1 - np.hypot(xx - x, (yy - y) * 1.15) / r, 0, 1) ** 1.5 * strength
    a = artkit.ordered_dither(v / strength, 4) * strength
    pool = l01.rgba(np.broadcast_to(np.array(colour, float), (h, w, 3)), a * 255)
    out = img.copy()
    out.alpha_composite(pool)
    return out


def convoy_apron(w, h):
    """The marshalling apron the convoy pulls out of: a long slab with yellow lane marks, floodlight
    masts, cargo stacks; the road leaves through its open top end."""
    img = paved(w, h, 22, 8, w - 23, h - 6, 781)
    d = ImageDraw.Draw(img)
    cx = w // 2
    for y in range(20, h - 10, 22):
        for x in (cx - 30, cx + 30):
            d.rectangle([x - 1, y, x + 1, y + 10], fill=(214, 180, 60, 255))
    for y in range(40, h - 30, 84):
        d.rectangle([cx - 30, y, cx + 30, y + 1], fill=(200, 200, 204, 255))
    for k in range(4):
        y = 24 + k * 6
        d.line([cx - 22, y + 8, cx, y, cx + 22, y + 8], fill=(214, 180, 60, 255), width=2)
    for y in (90, 270, 450):
        img = light_pool(img, 30, y, 60)
        img = light_pool(img, w - 30, y + 90, 60)
    post = lamp_post()
    cargo = station.render_part(station.cargo_part(), l01.KIT_PAL)
    cargo_b = station.render_part(station.cargo_part(SOLAR), l01.KIT_PAL)
    for y in (90, 270, 450):
        place(img, post, 30, y, shadow=14)
        place(img, post, w - 30, y + 90, shadow=14)
    for y, c in ((150, cargo), (176, cargo_b), (330, cargo), (520, cargo_b)):
        place(img, c, 40, y, shadow=8)
    for y, c in ((60, cargo_b), (400, cargo), (426, cargo)):
        place(img, c, w - 40, y, shadow=8)
    return finish(img)


def hab_domes(w, h, layout):
    """A cluster of pressure domes (the scene's Tranquility Base: warm-lit glass domes, truss
    links, cargo and radiators) with warm light pools and long shadows."""
    kit = {"dome": station.render_part(station.dome_part(24), WARM_GLASS),
           "dome_m": station.render_part(station.dome_part(20), WARM_GLASS),
           "dome_s": station.render_part(station.dome_part(16), WARM_GLASS),
           "truss": station.render_part(station.truss_h_part(64, 16), l01.KIT_PAL),
           "truss_s": station.render_part(station.truss_h_part(40, 14), l01.KIT_PAL),
           "cargo": station.render_part(station.cargo_part(), l01.KIT_PAL),
           "cargo_b": station.render_part(station.cargo_part(SOLAR), l01.KIT_PAL),
           "radiator": station.render_part(station.radiator_part(56, 2), l01.KIT_PAL),
           "dish": l01.kit()["dish"]}
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    for name, x, y in layout:
        if name.startswith("dome"):
            img = light_pool(img, x, y, kit[name].width * 0.9)
    for name, x, y in layout:
        place(img, kit[name], x, y, shadow=10 if name.startswith(("dome", "dish")) else 6)
    return finish(img)


HAB_A = [("radiator", 128, 22), ("truss", 88, 80), ("dome", 64, 52), ("dome_s", 112, 112),
         ("cargo", 30, 108), ("cargo_b", 30, 134)]
HAB_B = [("truss_s", 82, 60), ("dome_m", 46, 60), ("dome_m", 120, 52), ("dome_s", 82, 112),
         ("dish", 140, 112), ("radiator", 34, 124), ("cargo", 148, 18)]


def eagle_scene(p):
    """The Apollo 11 descent stage as left in 1969, top view: the gold-foil octagon (the ascent
    stage gone), four splayed legs with round footpads, the ladder on the front leg, the laser
    reflector and the seismometer with its two solar wings set out beside it."""
    body = np.maximum(sd_box(p, (0, 0, 6), (13, 13, 6), 1), sd_box(rotate_z(p, np.pi / 4), (0, 0, 6), (13, 13, 6), 1))
    items = [(body, ACCENT), (sd_box(p, (0, 0, 12.4), (5, 5, 0.8), 0.4), DARK),
             (sd_box(p, (3, -2, 13.2), (3, 2, 0.8), 0.3), HULL)]
    for k in range(4):
        a = np.pi / 4 + k * np.pi / 2
        c, s = np.cos(a), np.sin(a)
        items += [(sd_capsule(p, (11 * c, 11 * s, 9), (25 * c, 25 * s, 1.2), 1.1), HULL),
                  (sd_capsule(p, (12 * c - 5 * s, 12 * s + 5 * c, 3), (22 * c, 22 * s, 2), 0.7), HULL),
                  (sd_capsule(p, (12 * c + 5 * s, 12 * s - 5 * c, 3), (22 * c, 22 * s, 2), 0.7), HULL),
                  (sd_cylinder_z(p, (26 * c, 26 * s, 0), 3.4, 0.8), HULL)]
    a = -np.pi / 4 - np.pi / 2
    c, s = np.cos(a), np.sin(a)
    items += [(sd_box(rotate_z(p - np.array([17 * c, 17 * s, 6]), -a), (0, 0, 0), (6, 2.2, 0.5), 0.2), DARK)]
    items += [(sd_box(p, (30, -20, 1.2), (3.5, 3.5, 1.2), 0.3), DARK),
              (sd_box(p, (-30, 18, 1.6), (2.5, 2.5, 1.6), 0.4), HULL),
              (sd_box(p, (-36, 18, 1.0), (3.5, 2.2, 0.3), 0.1), SOLAR),
              (sd_box(p, (-24, 18, 1.0), (3.5, 2.2, 0.3), 0.1), SOLAR)]
    return union(*items)


def heritage_dome(w, h):
    """The Apollo 11 heritage dome: the Eagle's descent stage on the preserved regolith (the
    first footprints still in it) under a glass dome with a ribbed frame, on a ring base with a
    visitors' airlock."""
    R = 50

    def frame(p):
        r = np.linalg.norm(p, axis=1)
        shell = np.maximum(np.abs(r - R) - 0.9, -p[:, 2])
        ribs = []
        for k in range(4):                       # eight meridians, open over the Eagle (an oculus)
            q = rotate_z(p, k * np.pi / 4 + np.pi / 8)
            ribs.append(np.maximum(np.maximum(shell, np.abs(q[:, 1]) - 0.8), 30 - r * 0 - np.hypot(p[:, 0], p[:, 1])))
        for z in (16, 30):
            ribs.append(np.maximum(shell, np.abs(p[:, 2] - z) - 0.8))
        ribs.append(np.maximum(shell, np.abs(np.hypot(p[:, 0], p[:, 1]) - 30) - 1.0))
        return union((np.minimum.reduce(ribs), HULL),
                     (np.maximum(sd_cylinder_z(p, (0, 0, 0), R + 5, 2.5), -sd_cylinder_z(p, (0, 0, 0), R - 1, 4)), HULL),
                     (sd_box(p, (R + 6, -R * 0.45, 2), (7, 6, 4), 1), HULL),
                     (sd_box(p, (R + 11, -R * 0.45, 3), (1.5, 3, 2), 0.4), ACCENT))
    floor = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    yy, xx = np.mgrid[0:h, 0:w]
    rr = np.hypot(xx - w / 2 + 0.5, yy - h / 2 + 0.5)
    tex = raster.fbm(w, h, 8, 791, octaves=3, period=False)
    col = s6.ramp_img(GREYS, 0.55 + tex * 0.18) * 0.92
    rng = np.random.default_rng(792)
    for _ in range(3):                       # the first footprint trails
        x, y, a = w / 2 + rng.uniform(-6, 6), h / 2 + rng.uniform(-6, 6), rng.uniform(0, 2 * np.pi)
        for i in range(int(rng.uniform(14, 22))):
            a += rng.uniform(-0.25, 0.25)
            x, y = x + 2 * np.cos(a), y + 2 * np.sin(a)
            if np.hypot(x - w / 2, y - h / 2) < R - 4:
                col[int(y), int(x)] *= 0.72
    inside = rr < R
    floor = l01.rgba(col, inside * 255.0)
    eagle = render_model(eagle_scene, GOLD, (84, 84), factor=5, colors=40)
    place(floor, eagle, w / 2, h / 2, shadow=6, opacity=0.5)
    fr = render_model(frame, l01.KIT_PAL, (w, h), factor=3, colors=24)
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    img.alpha_composite(long_shadow(Image.fromarray((np.dstack([np.zeros((h, w, 3)), (rr < R + 5) * 255.0])).astype(np.uint8), "RGBA"), 10, 0.3).crop((0, 0, w, h)))
    img.alpha_composite(floor)
    # the glass: a cool tint over the floor, a stepped highlight at the top left (no dither on lit pixels)
    a = np.array(img).astype(np.float64)
    tint = inside[..., None] * 0.14
    a[..., :3] = a[..., :3] * (1 - tint) + np.array((150, 176, 214)) * tint
    hl = (np.abs(np.hypot(xx - w / 2 + R * 0.1, yy - h / 2 + R * 0.1) - R * 0.8) < 3) & \
        (np.arctan2(yy - h / 2, xx - w / 2) < -1.7) & (np.arctan2(yy - h / 2, xx - w / 2) > -2.9)
    a[..., :3][hl] = a[..., :3][hl] * 0.5 + np.array((226, 236, 250)) * 0.5
    img = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")
    img.alpha_composite(fr)
    return finish(img)


def landing_pad(w, h):
    """The scene's landing pad: a raised grey disc, the yellow ring and white cross, eight static
    amber rim lights."""
    s = 4
    big = Image.new("RGBA", (w * s, h * s), (0, 0, 0, 0))
    d = ImageDraw.Draw(big)
    c, r = w * s / 2, (w / 2 - 12) * s
    d.ellipse([c - r, c - r, c + r, c + r], fill=(76, 76, 84, 255), outline=(150, 150, 158, 255), width=s * 2)
    rr = r * 0.78
    d.ellipse([c - rr, c - rr, c + rr, c + rr], outline=(255, 200, 60, 255), width=s * 2)
    for dx, dy in ((1, 0), (0, 1)):
        d.line([c - dx * r * 0.36, c - dy * r * 0.36, c + dx * r * 0.36, c + dy * r * 0.36], fill=(230, 230, 236, 255), width=s * 3)
    pad = big.resize((w, h), Image.BOX)
    a = np.array(pad)
    a[..., 3] = np.where(a[..., 3] > 127, 255, 0)
    pad = Image.fromarray(a, "RGBA")
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    place(img, pad, w / 2, h / 2, shadow=4, opacity=0.5)
    d = ImageDraw.Draw(img)
    for k in range(8):
        ang = k * np.pi / 4 + np.pi / 8
        x, y = w / 2 + (w / 2 - 14) * np.cos(ang), h / 2 + (w / 2 - 14) * np.sin(ang)
        d.rectangle([x - 1, y - 1, x + 1, y + 1], fill=(255, 150, 40, 255))
        d.point([(x, y)], fill=(255, 236, 190, 255))
    return finish(img)


# --------------------------------------------------------------------------- sections 2-3

def rover_scene(kind, turn):
    def scene(p):
        q = rotate_z(p, turn)
        if kind == "a":                       # pressurised rover: cab, hab body, six wheels
            items = [(sd_box(q, (0, -2, 5), (10, 18, 5), 2.5), HULL),
                     (sd_box(q, (0, 15, 6), (8, 5, 3.5), 2), GLASS),
                     (sd_box(q, (0, -6, 10.2), (7, 8, 0.6), 0.2), SOLAR),
                     (sd_box(q, (0, 4, 10.4), (10.4, 1.2, 0.6), 0.2), ACCENT),
                     (sd_cylinder_z(q, (5, -16, 11), 2.5, 0.8), DARK)]
            items += [(sd_cylinder_x(q, (sx * 12, y, 3.5), 4, 2.2), DARK) for sx in (-1, 1) for y in (-14, -1, 12)
                      if not (sx == 1 and y == 12)]
        else:                                 # open utility rover with its cargo trailer
            items = [(sd_box(q, (0, 8, 3.5), (8, 9, 2), 1), HULL),
                     (sd_box(q, (0, 12, 6.5), (6, 2, 2), 0.6), DARK),
                     (sd_box(q, (0, -14, 4), (7, 8, 3.5), 1.2), ACCENT),
                     (sd_capsule(q, (0, -2, 3), (0, -6, 3), 0.8), DARK)]
            items += [(sd_cylinder_x(q, (sx * 10, y, 3), 3, 1.8), DARK) for sx in (-1, 1) for y in (3, 14, -14)]
        d, m = union(*items)
        return d, m
    return scene


def rover(w, h, kind, turn):
    """An abandoned rover, turned in the model, half-drifted with regolith dust."""
    spr = render_model(rover_scene(kind, np.radians(turn)), l01.KIT_PAL, (w - 20, h - 20), factor=6, colors=32)
    a = np.array(spr).astype(np.float64)
    dust = raster.fbm(spr.width, spr.height, 6, 801 + len(kind), octaves=2, period=False)
    k = np.clip((dust - 0.55) * 3, 0, 0.7)[..., None] * (a[..., 3:4] > 0)
    a[..., :3] = a[..., :3] * (1 - k) + s6.ramp_img(GREYS, np.full(dust.shape, 0.7)) * k
    spr = Image.fromarray(a.astype(np.uint8), "RGBA")
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    place(img, spr, w / 2 - 4, h / 2 - 5, shadow=9, opacity=0.6)
    return finish(img, 24)


def growth_socket(seed):
    """A Vrell growth socket: glossy violet bulbs with teal seams, a ring the turret grows from."""
    rng = np.random.default_rng(seed)
    blobs = [(rng.uniform(-9, 9), rng.uniform(-9, 9), rng.uniform(4, 7)) for _ in range(7)]

    def scene(p):
        items = [(sd_ellipsoid(p, (x, y, 2), (r, r * 0.85, r * 0.55)), em.V_BODY) for x, y, r in blobs]
        items += [(np.maximum(sd_cylinder_z(p, (0, 0, 4), 8, 2.2), -sd_cylinder_z(p, (0, 0, 4), 5, 4)), em.V_DARK)]
        items += [(sd_sphere(p, (x, y, 2 + r * 0.45), r * 0.3), em.V_GLOW) for x, y, r in blobs[::2]]
        return union(*items, k=2.0)
    return vrell(scene, (40, 40))


def boulder(w, h, seed, growth=False):
    """A mare boulder (the scene's rock shading, lit from the top left) with its long shadow; with
    ``growth`` a Vrell socket on top, the base a Spine Turret stands on."""
    r = min(w, h) * (0.36 if growth else 0.3)
    rock = rock_sprites(r, seed, GREYS[1:], rotations=1, crater_count=4)[0]
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    cx, cy = w / 2 - r * 0.25, h / 2 - r * 0.3
    place(img, rock, cx, cy, shadow=r * 1.3, opacity=0.6)
    if growth:
        sock = growth_socket(seed + 1)
        img = roots(img, (cx, cy + 4), seed + 2, 4, r * 1.2, 2.2)
        place(img, sock, cx, cy - 2, shadow=4, opacity=0.4)
    return finish(img, 28)


def road_bridge(w, h, angle, span, part="base"):
    """The convoy road's bridge across the rille, turned to the road's heading in the model, in two
    images at the same place: the base under the road and the convoy (a deck with tyre tracks
    between truss railings with orange posts, the footing slabs at both ends), and the overhead
    arches the convoy passes under (`part="arches"`; a portal of two pillars and a cross beam over
    each footing, a Vrell-proof socket plate on top: the Spine Turrets of 105 s stand on them),
    with a short drop shadow onto the road and the crawlers."""
    turn = -np.radians(angle)
    half = span / 2

    def scene(p):
        q = rotate_z(p, turn)
        if part == "arches":
            items = []
            for sy in (-1, 1):
                y = sy * (half + 6)
                items += [(sd_box(q, (sx * 40, y, 12), (5, 6, 12), 1.2), HULL) for sx in (-1, 1)]
                items += [(sd_box(q, (0, y, 22), (45, 7, 3), 1.2), HULL),
                          (sd_box(q, (0, y - sy * 6.6, 22), (38, 0.6, 1.4), 0.2), ACCENT),
                          (sd_box(q, (0, y, 25.3), (9, 6, 0.6), 0.3), DARK)]
            return union(*items)
        items = [(sd_box(q, (0, 0, 6), (30, half, 2.2), 0.6), HULL),
                 (sd_box(q, (0, 0, 2), (24, half, 3), 0.5), DARK)]
        for sx in (-1, 1):
            items += [(sd_box(q, (sx * 32, 0, 9), (1.6, half - 4, 1.2), 0.4), DARK),
                      (sd_box(q, (sx * 32, 0, 6), (1.2, half - 4, 2.5), 0.3), DARK)]
            items += [(sd_box(q, (sx * 32, y, 8), (1.6, 1.6, 3), 0.3), ACCENT) for y in np.arange(-half + 10, half - 6, 22)]
            items += [(sd_box(q, (sx * 13, 0, 8.3), (3, half, 0.25), 0.1), DARK)]
        for sy in (-1, 1):
            items += [(sd_box(q, (0, sy * (half + 6), 4), (38, 12, 5), 1.5), HULL),
                      (sd_box(q, (0, sy * (half + 6), 9.3), (30, 1.2, 0.5), 0.2), DARK)]
        return union(*items)
    spr = render_model(scene, l01.KIT_PAL, (w - 24, h - 24), factor=3, colors=32)
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    if part == "arches":
        place(img, spr, w / 2 - 6, h / 2 - 8, shadow=9, opacity=0.4)
    else:
        place(img, spr, w / 2 - 6, h / 2 - 8, shadow=14, opacity=0.5)
    return finish(img)


# --------------------------------------------------------------------------- section 4

def husk_scene(seed):
    """A pod lander split open on impact: the two halves of its ribbed chitin shell lying open like
    a seed case, tipped outwards, the wet lining inside with teal veins still glowing."""
    rng = np.random.default_rng(seed)
    tilt = rng.uniform(0.25, 0.45)
    turn = rng.uniform(-0.6, 0.6)

    def half(p, side):
        q = rotate_z(p, turn) - np.array([side * 15.0, 0, 0])
        q = rotate_y(q, side * tilt)
        outer = sd_ellipsoid(q, (0, 0, 0), (14, 29, 13))
        inner = sd_ellipsoid(q, (0, 0, 0), (11.5, 26.5, 10.5))
        cut = q[:, 2] - 2.0                          # the bowl below its rim
        body = np.maximum(np.maximum(outer, -inner), cut)
        rim = np.maximum(body, -(q[:, 2] - 0.5))
        ribs = np.maximum(np.maximum(outer - 0.9, np.abs((q[:, 1] % 8) - 4) - 0.9), cut)
        lining = np.maximum(np.maximum(np.abs(inner) - 1.2, outer), cut)
        vein = np.maximum(lining - 0.4, np.minimum(np.abs(q[:, 0] + 2.5 * np.sin(q[:, 1] * 0.22)) - 0.9,
                                                   np.abs(q[:, 1] - 8 + 0.6 * q[:, 0]) - 0.8))
        return [(body, em.V_BODY), (rim, em.V_BONE), (ribs, em.V_DARK), (lining, em.V_SAC), (vein, em.V_GLOW)]

    def scene(p):
        return union(*(half(p, -1.0) + half(p, 1.0)))
    return scene


def pod_husk(w, h, seed):
    """A split pod-lander husk in its fresh impact crater, roots spreading from it."""
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    yy, xx = np.mgrid[0:h, 0:w]
    d = np.hypot(xx - w / 2, (yy - h / 2) * 1.1) / (min(w, h) * 0.42)
    tex = raster.fbm(w, h, 6, seed, octaves=3, period=False)
    bowl = np.clip(1 - d, 0, 1)
    ring = np.exp(-((d - 1) / 0.13) ** 2)
    fall = np.clip(1 - (d - 1) / 0.35, 0, 1) * (d > 1)
    shadow = (np.hypot(xx - w / 2 + 6, (yy - h / 2 + 7) * 1.1) / (min(w, h) * 0.42) > 1) & (d < 1)
    col = s6.ramp_img(GREYS, 0.35 + 0.25 * tex + 0.3 * ring)
    col = np.where(shadow[..., None], np.array(GREYS[1][1], float) * 0.7 + EARTHSHINE * 1.2, col)
    alpha = np.clip(np.maximum(bowl > 0, 0) + ring + fall * (tex > 0.5), 0, 1)
    alpha = artkit.ordered_dither(np.clip(alpha * (0.4 + tex), 0, 1), 2) * (d < 1.35)
    alpha = np.where(d < 1, 1, alpha)
    img = l01.rgba(col, alpha * 255)
    img = roots(img, (w / 2, h / 2), seed + 3, 6, min(w, h) * 0.55, 2.6)
    husk = vrell(husk_scene(seed), (int(w * 0.62), int(h * 0.7)), factor=3)
    place(img, husk, w / 2 - 2, h / 2 - 2, shadow=7, opacity=0.5)
    return finish(img)


# --------------------------------------------------------------------------- section 5

def sled_sprite():
    def scene(p):
        return union((sd_box(p, (0, 0, 3), (5, 12, 3), 1.5), HULL), (sd_box(p, (0, -2, 6), (3.5, 6, 1.5), 1), ACCENT),
                     (sd_box(p, (0, 11, 4), (4, 1.5, 2), 0.6), DARK), (sd_box(p, (0, -12, 3), (3, 1, 1.5), 0.4), WARM))
    return render_model(scene, l01.KIT_PAL, (14, 30), factor=6, colors=16)


def sled_run(w, h, n):
    """The overlay over the rail: the pylon lamps blinking (slowly, faster before a shot), then a
    sled racing up the rail with its glow trail (the scene's sled shot, 55 px per frame at 10 fps,
    about 550 px/s along the rail). Frames line up with the rail's pylons when the piece's bottom
    edge lies on a layer position divisible by 60."""
    sled = sled_sprite()
    frames = []
    cx = w // 2
    for i in range(n):
        img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        warn = 14 <= i < 22
        on = (i % 2 == 0) if warn else ((i // 5) % 2 == 0 or i >= 22)
        lamp = (255, 140, 30, 255) if on else (110, 50, 20, 255)
        for k in range(8):
            y = h - 1 - (20 + 60 * k)
            for dx in (-17, 17):
                d.rectangle([cx + dx - 1, y - 1, cx + dx + 1, y + 1], fill=lamp)
        if warn and on:
            img = artkit.stepped_alpha(img, 4)
        if i >= 22:
            yb = 40 + (i - 22) * 55
            y = h - 1 - yb
            trail = min(70, yb - 18)
            arr = np.zeros((h, w, 4))
            for j in range(trail):
                arr[y + 14 + j, cx - 4:cx + 5] = (255, 200, 120, 0.7 * (1 - j / trail))
            glow = artkit.stepped_alpha(l01.rgba(arr[..., :3], arr[..., 3] * 255), 4)
            img.alpha_composite(glow)
            sprite.paste_center(img, sled, cx, y)
        frames.append(border_clear(img))
    return artkit.quantize_set(frames, 24)


def rail_head(w, h):
    """The mass driver's loading station over the start of the rail: the main hall, ore hoppers
    and a conveyor, a sled waiting in its cradle; static amber lamps."""
    def scene(p):
        items = [(sd_box(p, (0, -26, 9), (34, 38, 9), 3), HULL),
                 (sd_box(p, (0, -26, 18.4), (28, 30, 0.8), 0.3), HULL),
                 (sd_box(p, (0, -26, 19.0), (29, 0.8, 0.5), 0.1), DARK),
                 (sd_box(p, (0, 22, 6), (16, 16, 6), 2), HULL),
                 (sd_box(p, (0, 22, 12.4), (2, 14, 0.6), 0.2), DARK),
                 (sd_box(p, (0, 40, 4), (14, 3, 3), 0.8), ACCENT),
                 (sd_box(p, (-44, -10, 3), (6, 30, 3), 1), DARK)]
        items += [(sd_cylinder_z(p, (-40, y, 0), 7, 12), DARK) for y in (-52, -34)]
        items += [(sd_cylinder_z(p, (-40, y, 12), 5, 1), HULL) for y in (-52, -34)]
        items += [(sd_box(p, (sx * 16, -26, 19.4), (2, 2, 0.6), 0.2), WARM) for sx in (-1, 1)]
        items += [(sd_box(p, (0, y, 18.8), (24, 1.2, 0.5), 0.2), ACCENT) for y in (-50, -2)]
        return union(*items)
    spr = render_model(scene, l01.KIT_PAL, (w - 16, h - 24), factor=4, colors=32)
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    place(img, spr, w / 2 - 4, h / 2 - 6, shadow=12, opacity=0.55)
    sled = sled_sprite()
    sprite.paste_center(img, sled, w / 2 - 4, h / 2 - 6 - 36 - 8)
    return finish(img)


GATE_ROOF_Y = 160      # the hangar's centre row in the gate's images (its south face at row 310)
GATE_HALF = (190, 150)  # the hangar's half width and depth, px (380 x 300)
GATE_DOOR = (40, 24)    # the door's half width (a 40 px crawler in an 80 px opening) and its depth


def gate_scene(p):
    """The terminal's vehicle hangar, 380 x 300 px, the door in its south face (y = -150): a
    vaulted vehicle hall (160 px wide, ribbed, a ridge vent) between two flat-roofed wings (west:
    radiators, a dish and vents; east: a solar array and the control tower with its glazed band
    and red beacon), cut back at the door by an 80 x 24 px bay with a warm-lit frame and
    hazard-striped jambs."""
    hw, hd = GATE_HALF
    dw, dd = GATE_DOOR
    ax = np.abs(p[:, 0])

    def shell(y0, y1, x0, x1, lift=1.2):
        """A band of the vault's skin between y0..y1 and |x| in x0..x1, lifted off it."""
        band = sd_cylinder_y(p, (0, (y0 + y1) / 2, -118), 162 + lift, (y1 - y0) / 2)
        return np.maximum(np.maximum(band, np.abs(ax - (x0 + x1) / 2) - (x1 - x0) / 2), -p[:, 2])

    vault = np.maximum(np.maximum(sd_cylinder_y(p, (0, 0, -118), 162, hd), ax - 80), -p[:, 2])
    items = [(vault, HULL), (sd_box(p, (0, 0, 12), (80, hd, 12), 2), HULL)]
    items += [(shell(y - 2.2, y + 2.2, 0, 80.5), DARK) for y in np.arange(-hd + 40, hd - 10, 40)]
    items += [(sd_box(p, (0, 0, 44.5), (6, hd - 12, 1.6), 0.5), DARK),
              (sd_box(p, (0, 0, 46.3), (2, hd - 16, 0.5), 0.2), HULL)]
    items += [(sd_box(p, (0, y, 46.6), (2.2, 2.2, 0.6), 0.2), WARM) for y in (-hd + 40, 0, hd - 40)]
    for sx in (-1, 1):
        cx = sx * (80 + (hw - 80) / 2)
        items += [(sd_box(p, (cx, 0, 15), ((hw - 80) / 2, hd, 15), 4), HULL),
                  (sd_box(p, (cx, 0, 30.4), ((hw - 80) / 2 - 8, hd - 8, 0.8), 0.3), HULL),
                  (sd_box(p, (cx, -hd + 3, 26), ((hw - 80) / 2 - 6, 1.6, 2.2), 0.5), ACCENT)]
        items += [(sd_box(p, (cx, y, 31.4), ((hw - 80) / 2 - 8, 0.7, 0.4), 0.1), DARK) for y in (-60, 30, 110)]
        items += [(sd_box(p, (sx * 82, 0, 31.4), (1.2, hd - 4, 0.8), 0.3), DARK)]
    # west wing: radiators, a dish, vents
    items += [(sd_box(p, (x, 70, 32), (14, 40, 1.4), 0.4), DARK) for x in (-162, -124)]
    items += [(sd_box(p, (x, y, 33.6), (12, 1, 0.5), 0.1), HULL) for x in (-162, -124)
              for y in np.arange(38, 104, 8)]
    items += [(sd_cylinder_z(p, (-140, -70, 33), 7, 3), HULL), (sd_sphere(p, (-140, -70, 40), 16), HULL),
              (sd_cylinder_z(p, (-140, -70, 53), 1.4, 6), DARK)]
    items += [(sd_cylinder_z(p, (x, y, 32), 4.5, 2), DARK) for x, y in ((-170, -10), (-150, -10), (-110, -16),
                                                                         (-110, -110), (-168, -120))]
    # east wing: a solar array and the control tower by the door
    items += [(sd_box(p, (135, 75, 32.4), (44, 50, 1), 0.3), SOLAR)]
    items += [(sd_box(p, (135, y, 33.5), (44, 0.6, 0.4), 0.1), DARK) for y in np.arange(35, 120, 16)]
    items += [(sd_box(p, (x, 75, 33.5), (0.6, 50, 0.4), 0.1), DARK) for x in (113, 135, 157)]
    items += [(sd_box(p, (148, -95, 26), (24, 26, 26), 4), HULL),
              (sd_box(p, (148, -95, 46), (25.5, 27.5, 4), 1.5), GLASS),
              (sd_box(p, (148, -95, 52), (20, 22, 2), 1), HULL),
              (sd_cylinder_z(p, (148, -95, 55), 3, 2.5), RED),
              (sd_box(p, (110, -40, 32), (10, 8, 3), 1), DARK)]
    hall = union(*items)
    door = sd_box(p, (0, -hd + dd / 2 - 4, 30), (dw, dd / 2 + 4, 60))
    hall = (np.maximum(hall[0], -door), hall[1])
    frame = [(shell(-hd, -hd + dd, dw, dw + 4), WARM), (shell(-hd + dd - 0.5, -hd + dd + 3.5, 0, dw + 4), WARM),
             (shell(-hd + dd + 3.5, -hd + dd + 9, 0, dw + 10, 1.6), DARK)]
    frame += [(shell(-hd, -hd + 12, dw + 6, dw + 28), ACCENT)]
    frame += [(shell(-hd, -hd + 12, dw + 7 + k * 5, dw + 9 + k * 5, 1.6), DARK) for k in range(5)]
    return union(hall, *frame)


def terminal_gate(w, h, part="base"):
    """The terminal's vehicle hangar in two images at the same place (432 x 480, the hangar's
    door on the road at x = w/2): the base under the convoy (the paved apron before the door, the
    approach between two rows of static landing lights, floodlight masts, the lit hangar floor
    in the door bay), and the hangar itself as an overhead piece (`part="roof"`, gate_scene, with
    its long shadow), which the crawlers drive in under."""
    hw, hd = GATE_HALF
    dw, dd = GATE_DOOR
    cx, south = w // 2, GATE_ROOF_Y + hd
    if part == "roof":
        img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        spr = render_model(gate_scene, GATE_PAL, (w - 24, 2 * hd + 16), factor=3, colors=48)
        place(img, spr, cx, GATE_ROOF_Y, shadow=22, opacity=0.6)
        return finish(img, 40)
    img = paved(w, h, cx - 168, south - 30, cx + 168, h - 6, 811)
    d = ImageDraw.Draw(img)
    d.rectangle([cx - dw, south - dd - 20, cx + dw, south], fill=(30, 28, 30, 255))
    for x in (cx - 30, cx + 30):
        for y in range(south + 6, h - 12, 22):
            d.rectangle([x - 1, y, x + 1, y + 10], fill=(214, 180, 60, 255))
        d.rectangle([x - 1, south - dd - 20, x + 1, south], fill=(150, 124, 48, 255))
    for k in range(4):
        y = south + 16 + k * 7
        d.line([cx - 26, y + 9, cx, y, cx + 26, y + 9], fill=(214, 180, 60, 255), width=2)
    img = light_pool(img, cx, south - 4, 46, 0.3, (255, 210, 150))
    for y in range(south + 26, h - 14, 26):
        k = (y - south - 26) // 26
        for sx in (-1, 1):
            x = cx + sx * 56
            d = ImageDraw.Draw(img)
            d.rectangle([x - 3, y - 3, x + 2, y + 2], fill=(60, 60, 68, 255))
            d.rectangle([x - 2, y - 2, x + 1, y + 1], fill=(255, 244, 214, 255) if k % 2 else (255, 168, 48, 255))
    d = ImageDraw.Draw(img)
    y = south + 14
    for x in range(cx - 76, cx + 77, 8):
        if abs(x - cx) > 46:
            d.rectangle([x - 2, y - 2, x + 1, y + 1], fill=(60, 60, 68, 255))
            d.rectangle([x - 1, y - 1, x, y], fill=(255, 236, 200, 255))
    for y in range(south + 60, h - 30, 90):
        img = light_pool(img, cx, y, 60, 0.12, (255, 236, 200))
    post = lamp_post()
    for sx in (-1, 1):
        for y in (south + 40, h - 50):
            img = light_pool(img, cx + sx * 150, y, 56)
    for sx in (-1, 1):
        for y in (south + 40, h - 50):
            place(img, post, cx + sx * 150, y, shadow=14)
    return finish(img)


# --------------------------------------------------------------------------- the road texture

def road_texture(w, h):
    """56x192: compacted regolith, a crawler's two tread ruts (chevron treads, the groove's west
    side in shadow), orange edge posts every 48 px, regolith berms dithered out at the edges."""
    tex = raster.fbm(w, h, 8, 821, octaves=3)
    col = s6.ramp_img(GREYS, 0.42 + tex * 0.16) * 0.78
    xx = np.arange(w)[None, :].repeat(h, 0).astype(float)
    yy = np.arange(h)[:, None].repeat(w, 1).astype(float)
    for cx in (w / 2 - 14, w / 2 + 14):
        rut = np.abs(xx - cx) < 4.5
        tread = ((yy + np.abs(xx - cx) * 1.2) % 6) < 2.5
        col[rut] *= 0.8
        col[rut & tread] *= 0.85
        col[np.abs(xx - (cx - 4.5)) < 0.6] *= 0.62
        col[np.abs(xx - (cx + 4.5)) < 0.6] = col[np.abs(xx - (cx + 4.5)) < 0.6] * 0.8 + 40
    edge = np.minimum(xx, w - 1 - xx)
    berm = (edge >= 2) & (edge < 7)
    col[berm] = s6.ramp_img(GREYS, 0.58 + tex[berm] * 0.2) * 0.86
    alpha = np.where(edge >= 6, 1.0, artkit.ordered_dither(np.clip((edge - 0.5) / 6 + 0.15 * (tex - 0.5), 0, 1), 2))
    alpha[:, [0, w - 1]] = 0
    for y in range(8, h, 48):
        for x in (4, w - 5):
            col[y:y + 2, x:x + 2] = (255, 122, 42)
            col[y + 2, x + 1:x + 3] *= 0.5
            alpha[y:y + 3, x:x + 3] = 1
    img = l01.rgba(col, alpha * 255)
    return artkit.quantize_set([img], 16)[0]


# --------------------------------------------------------------------------- tables

TILE_SETS = {
    "rille-floor": rille_floor,
    "plumes-light": plumes(0.06, 190, 601), "plumes-medium": plumes(0.15, 205, 603),
    "plumes-heavy": plumes(0.45, 235, 605), "ejecta-streaks": ejecta_streaks,
}
GROUND_TILES = ("mare-a", "mare-b", "rille-rims", "crater-growth", "mass-driver-field")
PIECES = {
    "convoy-apron": convoy_apron,
    "hab-domes-a": lambda w, h: hab_domes(w, h, HAB_A), "hab-domes-b": lambda w, h: hab_domes(w, h, HAB_B),
    "heritage-dome": heritage_dome, "landing-pad": landing_pad,
    "rover-a": lambda w, h: rover(w, h, "a", 28), "rover-b": lambda w, h: rover(w, h, "b", -42),
    "boulder": lambda w, h: boulder(w, h, 841),
    "boulder-growth-a": lambda w, h: boulder(w, h, 851, True), "boulder-growth-b": lambda w, h: boulder(w, h, 861, True),
    "pod-husk-a": lambda w, h: pod_husk(w, h, 871), "pod-husk-b": lambda w, h: pod_husk(w, h, 881),
    "rail-head": rail_head, "terminal-gate": terminal_gate,
    "terminal-gate-roof": lambda w, h: terminal_gate(w, h, "roof"),
}
ANIMATED = {"sled-run": sled_run}
EXTRAS = {"road-texture": ((56, 192), road_texture)}
BRIDGE = {"road-bridge": "base", "road-bridge-arches": "arches"}   # turned to the road at render time


def level_data():
    """The level's data.yaml (its backdrop block, road and sections; the round 17 proposal file was
    merged into it)."""
    return yaml.safe_load((LEVEL_DIR / "data.yaml").read_text(encoding="utf-8"))


def road_at(points, t):
    ts, xs = zip(*points)
    return float(np.interp(t, ts, xs))


def check_road(level):
    """The road's rules (bends at most 30 degrees, centre within x = 72-408) and its clearance
    from the rille's gorge outside the bridge."""
    pts = level["road"]["points"]
    for (t0, x0), (t1, x1) in zip(pts, pts[1:]):
        angle = np.degrees(np.arctan2(abs(x1 - x0), (t1 - t0) * SPEED))
        if angle > 30 or not (72 <= x1 <= 408):
            raise SystemExit(f"road [{t1}, {x1}]: {angle:.1f} degrees from [{t0}, {x0}]")
    bridge = next(p for p in level["backdrop"]["placed"] if p["piece"] == "road-bridge")
    starts = [s["end"] for s in level["sections"]]
    lo, hi = starts[1] * SPEED + 540, starts[2] * SPEED + 540      # the rille-rims tiles on the ground layer
    worst = 99
    for t in np.arange((lo - MID) / SPEED, (hi - MID) / SPEED, 0.05):
        if abs(t - bridge["t"]) < 2.2:
            continue
        x = road_at(pts, t)
        gap = abs(x - channel_x((t * SPEED + MID) % H)) - 46 - 28
        worst = min(worst, gap)
    print(f"road: {len(pts)} points; nearest approach to the rille's walls off the bridge: {worst:.0f} px")


def bridge_geometry(level):
    """The bridge's heading (degrees right of straight up) and span from the road at its place."""
    bridge = next(p for p in level["backdrop"]["placed"] if p["piece"] == "road-bridge")
    pts = level["road"]["points"]
    t = bridge["t"]
    slope = (road_at(pts, t + 0.5) - road_at(pts, t - 0.5)) / SPEED
    angle = np.degrees(np.arctan(slope))
    ts = np.arange(t - 3, t + 3, 0.005)
    xs = np.interp(ts, *zip(*pts))
    over = np.abs(xs - channel_x((ts * SPEED + MID) % H)) < 30 + 28   # the ribbon over the opening
    span = np.hypot((ts[over][-1] - ts[over][0]) * SPEED, xs[over][-1] - xs[over][0])
    return angle, float(span) + 16


def jobs(backdrop, wanted):
    out = [(n, (W, s["height"]), None) for n, s in backdrop["tile_sets"].items() if n not in GROUND_TILES]
    out += [(n, tuple(s["size"]), s.get("frames")) for n, s in backdrop["pieces"].items()
            if n not in CAPS]
    out += [(n, size, None) for n, (size, _) in EXTRAS.items()]
    return [job for job in out if not wanted or job[0] in wanted]


def render(job):
    name, (w, h), count = job[:3]
    if name in BRIDGE:
        images = [road_bridge(w, h, *job[3], BRIDGE[name])]
    elif count:
        images = ANIMATED[name](w, h, count)
    else:
        fn = TILE_SETS.get(name) or PIECES.get(name) or EXTRAS[name][1]
        images = [fn(w, h)]
    for img in images:
        if img.size != (w, h):
            raise ValueError(f"{name}: rendered {img.size}, the data file says {(w, h)}")
    return images


def write(name, images, count=None):
    if count:
        for old in OUT.glob(f"{name}_*.png"):
            if old.stem.rsplit("_", 1)[1].isdigit() and old.stem.rsplit("_", 1)[0] == name:
                old.unlink()
        for i, img in enumerate(images):
            artkit.save_png(img, OUT / f"{name}_{i}.png", SOURCE)
    else:
        artkit.save_png(images[0], OUT / f"{name}.png", SOURCE)
    print(f"{name}: {len(images)} image(s), {artkit.colour_count(images)} colours")


# --------------------------------------------------------------------------- review: composites

def load(name, count=None):
    if count:
        return [Image.open(OUT / f"{name}_{i}.png").convert("RGBA") for i in range(count)]
    return Image.open(OUT / f"{name}.png").convert("RGBA")


def composite(level, t):
    """The play field at time t as Backdrop draws it (far, haze, ground tiles from their seams, the
    road ribbon, ground pieces, the convoy, the overhead ground pieces, low-air banks, high-air at
    40 % additive)."""
    bd = level["backdrop"]
    secs = level["sections"]
    starts = [0.0] + [s["end"] for s in secs[:-1]]
    scroll = t * SPEED
    sec = max(i for i, s in enumerate(starts) if t >= s)
    peak = secs[sec].get("peak")
    look = bd["atmosphere"][peak["atmosphere"] if peak and peak["from"] <= t < peak["to"] else secs[sec]["atmosphere"]]
    img = np.zeros((540, W, 3))

    def layer(name):
        factor = bd["scroll_factors"][name]
        pos0 = scroll * factor
        canvas = Image.new("RGBA", (W, 540), (0, 0, 0, 0))
        for i, s in enumerate(secs):
            for tid in s.get("tiles", []):
                if bd["tile_sets"][tid]["layer"] != name:
                    continue
                tile = load(tid)
                seam = -1e9 if i == 0 else starts[i] * SPEED * factor + 540
                end = starts[i + 1] * SPEED * factor + 540 if i + 1 < len(secs) else 1e12
                th = tile.height
                for y in range(540):
                    pos = pos0 + (539 - y)
                    if seam <= pos < end:
                        canvas.paste(tile.crop((0, th - 1 - int(pos) % th, W, th - int(pos) % th)), (0, y))
        if name == "ground":
            draw_road(canvas, level, scroll)
        def pieces(overhead):
            for p in bd["placed"]:
                spec = bd["pieces"][p["piece"]]
                if spec["layer"] != name or p.get("overhead", False) != overhead:
                    continue
                pw, ph = spec["size"]
                bottom = round(p["t"] * SPEED * factor + MID - ph / 2)
                y = 540 - (bottom - round(pos0)) - ph
                if y >= 540 or y + ph <= 0:
                    continue
                frames = spec.get("frames")
                im = load(p["piece"], frames)[int(t * spec["fps"]) % frames] if frames else load(p["piece"])
                sprite.paste(canvas, im, round(p["x"] - pw / 2), y)

        pieces(False)
        if name == "ground":
            draw_convoy(canvas, level, scroll)
            pieces(True)
        return canvas

    def blend(canvas, add=False, scale=1.0):
        a = np.array(canvas).astype(np.float64)
        al = a[..., 3:4] / 255 * scale
        if add:
            img[:] = np.minimum(255, img + a[..., :3] * al)
        else:
            img[:] = img * (1 - al) + a[..., :3] * al

    blend(layer("far"))
    haze = np.array([int(bd["haze_colour"][i:i + 2], 16) for i in (0, 2, 4)], float)
    img[:] = img * (1 - look["haze"]) + haze * look["haze"]
    blend(layer("ground"))
    low = layer("low-air")
    if look.get("banks"):
        banks = load(look["banks"])
        shift = int(round(bd["tile_sets"][look["banks"]].get("drift", 0) * t)) % W
        banks = Image.fromarray(np.roll(np.array(banks), shift, axis=1), "RGBA")
        pos0 = scroll * bd["scroll_factors"]["low-air"]
        for y in range(540):
            pos = int(pos0 + 539 - y) % banks.height
            low.alpha_composite(banks.crop((0, banks.height - 1 - pos, W, banks.height - pos)), (0, y))
    blend(low)
    blend(layer("high-air"), add=True, scale=0.4)
    return Image.fromarray(img.astype(np.uint8), "RGB")


def road_frame(level):
    """Per layer position (1 px steps): the road's x, its cos(bend) and its arc length."""
    pts = level["road"]["points"]
    p0 = pts[0][0] * SPEED + MID
    p1 = pts[-1][0] * SPEED + MID
    pos = np.arange(int(p0), int(p1) + 1, dtype=float)
    x = np.interp((pos - MID) / SPEED, *zip(*pts))
    slope = np.gradient(x)
    arc = np.cumsum(np.sqrt(1 + slope ** 2))
    return pos, x, 1 / np.sqrt(1 + slope ** 2), arc


def draw_road(canvas, level, scroll):
    tex = np.array(load("road-texture"))
    th, tw = tex.shape[:2]
    pos, xs, cos, arc = road_frame(level)
    a = np.array(canvas)
    for y in range(540):
        p = scroll + 539 - y
        i = int(round(p - pos[0]))
        if not 0 <= i < len(pos):
            continue
        row = th - 1 - int(arc[i]) % th
        for X in range(int(xs[i] - 34), int(xs[i] + 35)):
            col = int(round((X - xs[i]) * cos[i] + tw / 2 - 0.5))
            if 0 <= X < W and 0 <= col < tw and tex[row, col, 3]:
                a[y, X] = tex[row, col]
    canvas.paste(Image.fromarray(a, "RGBA"))


def draw_convoy(canvas, level, scroll):
    crawler = Image.open(ROOT / "assets" / "sprites" / "civilian-crawler_9.png").convert("RGBA")
    pos, xs, _, _ = road_frame(level)
    for yc in (150, 234, 318, 402, 486):
        p = scroll + 540 - yc
        i = int(round(p - pos[0]))
        if 0 <= i < len(pos):
            sprite.paste_center(canvas, crawler, xs[i], yc)


TIMES = [(2, "1 APRON, CONVOY FORMS UP"), (9.5, "1 HERITAGE DOME"), (42.5, "2 TURRET NEST BY THE ROAD"),
         (72.6, "3 RILLE BEGINS (SEAM)"), (90, "3 ALONG THE RIM"), (106.4, "3 THE BRIDGE"),
         (112.6, "4 RILLE ENDS (SEAM)"), (133, "4 HEAVY PEAK"), (147, "4 POD HUSKS"),
         (158.4, "5 RAIL HEAD, SLED SHOT"), (185.5, "5 THE TERMINAL'S HANGAR"), (188.6, "5 INTO THE HANGAR")]


def review(level):
    backdrop = level["backdrop"]
    items = []
    for name, spec in list(backdrop["tile_sets"].items()) + list(backdrop["pieces"].items()) + \
            [(n, {"layer": "road"}) for n in EXTRAS]:
        count = spec.get("frames")
        im = load(name, count)
        im = im[25] if count else im
        scale = min(1.0, 220 / max(im.size))
        im = im.resize((max(1, int(im.width * scale)), max(1, int(im.height * scale))), Image.NEAREST)
        items.append((f"{name} {spec['layer']}", im, spec.get("layer") == "high-air"))
    comps = [(f"T={t:g} {label}", composite(level, t)) for t, label in TIMES]
    width, x, y, row_h = 1460, 16, 44, 0
    rows = []
    for name, im, add in items:
        cell = max(im.width, 6 * len(name))
        if x + cell > width - 16:
            x, y, row_h = 16, y + row_h + 30, 0
        rows.append((name, im, add, x, y))
        x += cell + 14
        row_h = max(row_h, im.height)
    y += row_h + 40
    comp_y = y
    cw = 480 // 2 + 14
    per_row = (width - 16) // cw
    n_rows = (len(comps) + per_row - 1) // per_row
    artkit.REVIEW_ROUND = "r17"
    sheet = raster.sheet(width, comp_y + n_rows * (270 + 30) + 20, "LEVEL 04 BACKDROP - FINAL PIECES (PROPOSAL)",
                         "PRODUCTION ART, M4 PART D BATCH - R17; COMPOSITES AT 1/2 SCALE WITH THE ROAD AND THE CONVOY")
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
    area += sum(w * h for (w, h), _ in EXTRAS.values())
    print(f"atlas area: {area:,} px = {area / 2048 ** 2:.2f} pages of 2048x2048")


def main(argv):
    level = level_data()
    backdrop = level["backdrop"]
    wanted = {a for a in argv if not a.startswith("--")}
    known = set(TILE_SETS) | set(GROUND_TILES) | set(PIECES) | set(ANIMATED) | set(EXTRAS) | set(CAPS) | set(BRIDGE)
    missing = (backdrop["tile_sets"].keys() | backdrop["pieces"].keys()) - known
    if missing or wanted - known:
        raise SystemExit(f"no generator for {sorted(missing | (wanted - known))}")
    check_road(level)
    if "--review" not in argv:
        OUT.mkdir(parents=True, exist_ok=True)
        todo = jobs(backdrop, wanted)
        bridge = bridge_geometry(level)
        todo = [job + (bridge,) if job[0] in BRIDGE else job for job in todo]
        with ProcessPoolExecutor() as pool:
            caps = tuple((p["piece"], p["t"], p["x"], *backdrop["pieces"][p["piece"]]["size"])
                         for p in backdrop["placed"] if p["piece"] in CAPS)
            ground = pool.submit(ground_group, caps) if not wanted or wanted & (set(GROUND_TILES) | set(CAPS)) else None
            for job, images in zip(todo, pool.map(render, todo)):
                write(job[0], images, job[2])
            if ground is not None:
                for name, img in ground.result().items():
                    size = (W, backdrop["tile_sets"][name]["height"]) if name in GROUND_TILES else \
                        tuple(backdrop["pieces"][name]["size"])
                    if img.size != size:
                        raise ValueError(f"{name}: rendered {img.size}, the data file says {size}")
                    write(name, [img])
        print(f"bridge: heading {bridge[0]:.1f} degrees, span {bridge[1]:.0f} px")
    atlas_area(backdrop)
    review(level)


if __name__ == "__main__":
    main(sys.argv[1:])
