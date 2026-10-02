#!/usr/bin/env python3
"""Production art: the hangar's equipment icons (design/ui/hangar), one per shop item of the
catalogue (design/player: weapons, generators, shields, plating, engines, utility modules,
specials) and Rook's escort craft, in two sizes.

Outputs (assets/sprites/icons/, packed onto the shared sprite pages as ``icons/<name>``):
  <name>.png         16x16, the shop list's rows
  <name>-large.png   24x24, the selected item and the schematic's callouts
  design/ui/hangar/concept/hangar-final-r13-a.png   review sheet: the tactical map and every icon

<name> is the weapon's slug, or ``<kind>-<the item's name as a slug>`` for the other kinds
(``generator-mk-ii-arc``, ``utility-pickup-magnet``), ``escort-rook`` for the escort; the game
derives the same names (vanguard.game.hangar.ItemIcons).

Every icon is one emblem style: a bevelled dark steel badge, light from the top-left, with the item
on it, ray-marched at 8x through the sprite path (1-bit alpha, unsharp mask, 32 colours). The items
with a model are the model: the five wing pods (tools/concept/vfx_r08.py with the Stormhawk's
production pod materials), the Hornet's missile, the mortar shell, the mine, Rook's craft. Guns
without a model show their barrel block and their shots in the firing direction (front guns up,
rear guns down or sideways); the parts and specials have one symbol per kind in the kind's colour
(generator yellow core, shield cyan, plating steel, engine orange, utility green, specials orange).
Graded parts (Mk I-VI, Composite I-V) carry their grade as amber studs along the badge's foot.

Run: python3 tools/art/icons.py [--review]   (~20 s; --review only rebuilds the sheet)
"""
import re
import sys
from concurrent.futures import ProcessPoolExecutor

import numpy as np
import yaml
from PIL import Image

import artkit
import stormhawk
from artkit import DESIGN, ROOT, sprite

import ships_r02  # noqa: E402  (concept scripts, imported unchanged)
import vfx_r08 as v8  # noqa: E402
from render import models, raster, sdf  # noqa: E402
from render.palette import B  # noqa: E402
from render.sdf import rotate_z, sd_box, sd_capsule, sd_sphere, union, vec  # noqa: E402

SCRIPT = "icons.py"
SOURCE = artkit.source_note(SCRIPT, "UI batch")
OUT = artkit.SPRITES / "icons"
CONCEPT = DESIGN / "ui" / "hangar" / "concept"
ROUND = "r13"
SIZES = {"": 16, "-large": 24}
COLOURS = 32
PLAYER = DESIGN / "player"

# Kinds of the catalogue (vanguard.content.campaign.Catalogue) with their data file and list.
PARTS = [("generator", "generator", "models"), ("shield", "shields", "models"), ("plating", "armor", "plating"),
         ("engine", "systems", "engines"), ("utility", "systems", "utility"), ("special", "specials", "specials")]
ROMAN = {"I": 1, "II": 2, "III": 3, "IV": 4, "V": 5, "VI": 6}

HULL, ACC, SHOTS = B["UTC HULL"], B["UTC ACCENTS"], B["PLAYER SHOTS"]
BADGE = v8.mat((34, 40, 82), metal=0.35, shininess=50, spec=0.6)
STEEL = v8.m_metal(HULL[3])
DARK = v8.m_metal(HULL[1])
GLOWS = {"shot": v8.m_glow(SHOTS[2], 1.5), "core": v8.m_glow((255, 236, 140), 1.5),
         "shield": v8.m_glow((0, 200, 255), 1.1), "flame": v8.m_glow(ACC[4], 1.5),
         "util": v8.m_glow((64, 255, 128), 1.2), "special": v8.m_glow(ACC[4], 1.4),
         "stud": v8.m_glow((255, 224, 74), 1.3), "vrell": v8.m_glow((0, 255, 154), 1.3)}
MATS = [BADGE, STEEL, DARK] + list(GLOWS.values())
M_BADGE, M_STEEL, M_DARK = 0, 1, 2
M = {name: 3 + i for i, name in enumerate(GLOWS)}


def slug(text):
    return re.sub(r"[^a-z0-9]+", "-", text.lower()).strip("-")


def catalogue():
    """(icon name, label, symbol key, grade) of every shop item, in the catalogue's order."""
    items = []
    for data in sorted((PLAYER / "weapons").glob("*/data.yaml")):
        weapon = yaml.safe_load(data.read_text(encoding="utf-8"))
        items.append((data.parent.name, weapon["name"], data.parent.name, 0))
    for kind, folder, key in PARTS:
        for part in yaml.safe_load((PLAYER / folder / "data.yaml").read_text(encoding="utf-8"))[key]:
            name = part["name"]
            grade = re.search(r"\b(?:Mk|Composite) (VI|IV|V|I{1,3})\b", name)
            symbol = f"{kind}-{slug(name)}" if kind in ("utility", "special") else kind
            items.append((f"{kind}-{slug(name)}", name, symbol, ROMAN[grade.group(1)] if grade else 0))
    items.append(("escort-rook", "Rook (escort)", "escort-rook", 0))
    return items


# --------------------------------------------------------------------------- shapes (badge units)
# The badge spans -1..1; a scene returns (distance, material) in badge units, z up from the face.

def badge(p):
    return artkit.chamfered_box(p, (0, 0, -0.14), (0.97, 0.97, 0.14), 0.16)


def bolt(p, a, b, r=0.1):
    return sd_capsule(p, (*a, 0.12), (*b, 0.12), r)


def fan(p, base, angles, start, end, r=0.1):
    d = np.full(len(p), np.inf)
    for a in angles:
        direction = np.array([np.sin(np.radians(a)), np.cos(np.radians(a))])
        d = np.minimum(d, bolt(p, base + direction * start, base + direction * end, r))
    return d


def block(p, centre, half):
    return sd_box(p, (*centre, 0.08), (*half, 0.1), 0.04)


def ring(p, centre, radius, tube, z=0.1):
    q = p - vec(*centre, z)
    return np.sqrt((sdf.length(q[:, :2]) - radius) ** 2 + q[:, 2] ** 2) - tube


def arc(p, centre, radius, tube, half_angle):
    """A ring segment of ``half_angle`` either side of straight up."""
    q = p - vec(*centre, 0.1)
    angle = np.abs(np.arctan2(q[:, 0], q[:, 1]))
    on = sdf.length(np.stack([sdf.length(q[:, :2]) - radius, q[:, 2]], axis=-1)) - tube
    ends = [sdf.length(q - vec(s * radius * np.sin(half_angle), radius * np.cos(half_angle), 0)) - tube
            for s in (-1, 1)]
    return np.where(angle <= half_angle, on, np.minimum(*ends))


def plate(p, poly, z=0.08, half=0.1, round_=0.04):
    return sdf.sd_plate(p, poly, z, half, round_)


def guns(kind):
    """The guns without a model: barrel block and shots in the firing direction."""
    def scene(p):
        if kind == "pulse-cannon":
            shots = np.minimum(bolt(p, (-0.28, -0.2), (-0.28, 0.65)), bolt(p, (0.28, -0.2), (0.28, 0.65)))
            gun = block(p, (0, -0.62), (0.5, 0.18))
        elif kind == "scatter-vulcan":
            shots = fan(p, np.array([0.0, -0.55]), (-32, 0, 32), 0.35, 1.25, 0.09)
            gun = block(p, (0, -0.62), (0.3, 0.2))
        elif kind == "lance-laser":
            shots = bolt(p, (0, -0.4), (0, 0.82), 0.12)
            gun = np.minimum(block(p, (0, -0.62), (0.42, 0.16)), block(p, (0, -0.4), (0.16, 0.14)))
        elif kind == "fan-blaster":
            shots = fan(p, np.array([0.0, 0.55]), (148, 180, 212), 0.35, 1.25, 0.09)
            gun = block(p, (0, 0.62), (0.3, 0.2))
        elif kind == "side-splitter":
            shots = np.minimum(bolt(p, (-0.32, 0), (-0.85, 0)), bolt(p, (0.32, 0), (0.85, 0)))
            gun = block(p, (0, 0), (0.2, 0.36))
        else:  # tail-gun
            shots = np.minimum(bolt(p, (0, 0.2), (0, -0.25)), bolt(p, (0, -0.45), (0, -0.85)))
            gun = np.minimum(block(p, (0, 0.62), (0.4, 0.18)), block(p, (0, 0.38), (0.12, 0.12)))
        return union((shots, M["shot"]), (gun, M_STEEL))
    return scene


def generator(p):
    return union((ring(p, (0, 0), 0.52, 0.15), M_STEEL), (sd_sphere(p, (0, 0, 0.08), 0.3), M["core"]),
                 *[(block(p, (0.82 * s, 0), (0.12, 0.1)), M_DARK) for s in (-1, 1)])


def shield(p):
    outer = [(0, 0.85), (0.7, 0.55), (0.6, -0.25), (0, -0.85), (-0.6, -0.25), (-0.7, 0.55)]
    inner = [(x * 0.7, y * 0.7) for x, y in outer]
    return union((plate(p, outer, 0.04, 0.08), M_STEEL), (plate(p, inner, 0.12, 0.08), M["shield"]))


def plating(p):
    d = plate(p, [(-0.7, 0.7), (0.7, 0.7), (0.7, -0.7), (-0.7, -0.7)], 0.06, 0.1, 0.08)
    ribs = np.min([block(p, (0, y), (0.48, 0.05)) for y in (-0.25, 0.0, 0.25)], axis=0)
    rivets = np.min([sd_sphere(p, (0.52 * sx, 0.52 * sy, 0.16), 0.09) for sx in (-1, 1) for sy in (-1, 1)], axis=0)
    return union((d, M_STEEL), (np.maximum(ribs, -(p[:, 2] - 0.22)), M_DARK), (rivets, M_STEEL))


def engine(p):
    flame = np.minimum(sd_sphere(p, (0, 0.1, 0.06), 0.24), bolt(p, (0, 0.0), (0, -0.85), 0.14))
    return union((ring(p, (0, 0.1), 0.45, 0.14), M_STEEL), (flame, M["flame"]))


def utility(name):
    def scene(p):
        if name == "sensor-suite":
            return union((sd_sphere(p, (0, -0.55, 0.1), 0.16), M["util"]),
                         (arc(p, (0, -0.55), 0.55, 0.09, np.radians(48)), M["util"]),
                         (arc(p, (0, -0.55), 1.0, 0.09, np.radians(40)), M["util"]),
                         (block(p, (0, -0.8), (0.3, 0.08)), M_STEEL))
        if name == "pickup-magnet":
            legs = np.minimum(bolt(p, (-0.42, 0.0), (-0.42, 0.55), 0.17), bolt(p, (0.42, 0.0), (0.42, 0.55), 0.17))
            bow = np.where(p[:, 1] < 0.0, ring(p, (0, 0), 0.42, 0.17), np.inf)
            tips = np.minimum(bolt(p, (-0.42, 0.55), (-0.42, 0.78), 0.17), bolt(p, (0.42, 0.55), (0.42, 0.78), 0.17))
            return union((np.minimum(legs, bow), M_STEEL), (tips, M["util"]))
        if name == "salvage-scanner":
            return union((ring(p, (-0.15, 0.18), 0.45, 0.12), M_STEEL), (sd_sphere(p, (-0.15, 0.18, -0.05), 0.36), M["util"]),
                         (bolt(p, (0.2, -0.18), (0.68, -0.68), 0.13), M_STEEL))
        if name == "targeting-computer":
            ticks = np.min([bolt(p, (0.45 * dx, 0.45 * dy), (0.9 * dx, 0.9 * dy), 0.08)
                            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))], axis=0)
            return union((ring(p, (0, 0), 0.6, 0.08), M["util"]), (ticks, M["util"]),
                         (sd_sphere(p, (0, 0, 0.1), 0.13), M["util"]))
        if name == "evasive-thrusters":
            d = np.full(len(p), np.inf)
            for s in (-1, 1):
                for x in (0.25, 0.65):
                    d = np.minimum(d, np.minimum(bolt(p, (s * x, 0), (s * (x - 0.3), 0.4), 0.1),
                                                 bolt(p, (s * x, 0), (s * (x - 0.3), -0.4), 0.1)))
            return union((d, M["util"]), (block(p, (0, 0), (0.1, 0.5)), M_STEEL))
        if name == "auto-repair-nanites":
            cross = np.minimum(block(p, (0, 0), (0.18, 0.55)), block(p, (0, 0), (0.55, 0.18)))
            return union((ring(p, (0, 0), 0.78, 0.1), M_STEEL), (cross, M["util"]))
        if name == "pressure-hull":
            hexagon = [(0.8 * np.cos(a), 0.8 * np.sin(a)) for a in np.radians(np.arange(30, 390, 60))]
            return union((plate(p, hexagon, 0.06, 0.1, 0.06), M_STEEL), (sd_sphere(p, (0, 0, 0.12), 0.26), M["util"]))
        # ascendancy-iff-spoofer: a Vrell eye
        lens = np.maximum(sd_sphere(p, (0, -0.55, -0.6), 1.05), sd_sphere(p, (0, 0.55, -0.6), 1.05))
        return union((lens, M_DARK), (sd_sphere(p, (0, 0, 0.2), 0.24), M["vrell"]))
    return scene


def special(name):
    def scene(p):
        if name == "airstrike":
            pts = [(5, 0), (7, 4), (10, 6), (10, 7), (6, 6), (6, 9), (8, 10), (2, 10), (4, 9), (4, 6), (0, 7), (0, 6), (3, 4)]
            jet = plate(p, [((x - 5) / 6, (5 - y) / 6) for x, y in pts], 0.08, 0.1, 0.03)
            return union((jet, M_STEEL), (sd_sphere(p, (0, -0.7, 0.1), 0.12), M["special"]))
        if name == "smart-bomb":
            rays = np.min([bolt(p, (0.55 * np.cos(a), 0.55 * np.sin(a)), (0.9 * np.cos(a), 0.9 * np.sin(a)), 0.08)
                           for a in np.radians(np.arange(45, 405, 90))], axis=0)
            body = sd_sphere(p, (0, 0, 0.0), 0.48)
            band = np.maximum(body, np.abs(p[:, 1]) - 0.09)
            return union((body, M_DARK), (band - 0.02, M["special"]), (rays, M["special"]))  # the band stands proud
        # decoy-flares
        flares = np.min([np.minimum(sd_sphere(p, (x, y, 0.12), 0.17), bolt(p, (x, y), (x - 0.18, y - 0.55), 0.07))
                         for x, y in ((-0.45, 0.5), (0.1, 0.65), (0.6, 0.35))], axis=0)
        return union((flares, M["special"]), (block(p, (0, -0.7), (0.45, 0.12)), M_STEEL))
    return scene


def studs(p, grade):
    """The grade's amber studs along the badge's foot."""
    pitch = 0.27
    xs = (np.arange(grade) - (grade - 1) / 2) * pitch
    return np.min([sd_sphere(p, (x, -0.8, 0.0), 0.1) for x in xs], axis=0)


# --------------------------------------------------------------------------- models on the badge

def model_symbol(scene, mats, span, angle=-45.0, centre=(0.0, 0.0, 0.0)):
    """A model (in its own units, ``span`` of them filling the badge's symbol area of 1.5) turned
    by ``angle`` degrees about z before it is lit. Returns (scene in badge units, materials)."""
    k = 1.5 / span
    offset = len(MATS)
    rad = np.radians(angle)

    def s(p):
        q = rotate_z(p, rad) / k + vec(*centre)
        q[:, 2] -= 0.12 / k
        d, m = scene(q)
        return d * k, m + offset
    return s, mats


def pod(kind, span=0.55, angle=-45.0):
    """A wing pod of the Stormhawk's (the concept's model at its mount, with the production pod
    materials), centred on the badge."""
    return model_symbol(v8._pod_scene(kind), stormhawk.POD_MATS, span, angle, tuple(v8.POD_AT))


MODELS = {
    "autocannon-pod": lambda: pod("autocannon"), "micro-missile-pod": lambda: pod("micro-missile"),
    "bomb-rack": lambda: pod("bomb-rack"), "swivel-gun": lambda: pod("swivel", 0.42, 0.0),
    "torpedo-pod": lambda: pod("torpedo"),
    "hornet-launcher": lambda: model_symbol(*v8.missile_model(), 1.45),
    "hammer-mortar": lambda: model_symbol(*v8.shell_model(), 1.6, 0.0),
    "proximity-mines": lambda: model_symbol(*v8.mine_model(), 1.7, 0.0),
    "escort-rook": lambda: model_symbol(*models.ship_c_model(palette=ships_r02.ROOK_SCHEMES["a"][1]), 1.6, 0.0),
}
GUNS = ("pulse-cannon", "scatter-vulcan", "lance-laser", "fan-blaster", "side-splitter", "tail-gun")
SYMBOLS = {"generator": generator, "shield": shield, "plating": plating, "engine": engine}


def emblem(symbol, grade):
    """(scene in badge units, materials) of one icon."""
    mats = list(MATS)
    if symbol in MODELS:
        inner, extra = MODELS[symbol]()
        mats += extra
    elif symbol in GUNS:
        inner = guns(symbol)
    elif symbol in SYMBOLS:
        inner = SYMBOLS[symbol]
    elif symbol.startswith("utility-"):
        inner = utility(symbol.removeprefix("utility-"))
    else:
        inner = special(symbol.removeprefix("special-"))

    def scene(p):
        q = p
        if grade:
            q = p.copy()
            q[:, :2] = (q[:, :2] - vec(0, 0.14)[:2]) / 0.84
        d, m = inner(q)
        if grade:
            d = d * 0.84
        items = [(badge(p), M_BADGE), (d, m)]
        if grade:
            items.append((studs(p, grade), M["stud"]))
        return union(*items)
    return scene, mats


def render(job):
    name, symbol, grade, size = job
    scene, mats = emblem(symbol, grade)
    half = size / 2

    def px(p):
        d, m = scene(p / half)
        return d * half, m
    hi = sdf.render(px, mats, (size * 8, size * 8), float(size), z_top=size * 0.5)
    img = artkit.native(hi, 8)
    return artkit.quantize_set([img], COLOURS)[0]


def build():
    OUT.mkdir(parents=True, exist_ok=True)
    for old in OUT.glob("*.png"):
        old.unlink()
    jobs = [(name + suffix, symbol, grade, size) for name, _, symbol, grade in catalogue()
            for suffix, size in SIZES.items()]
    with ProcessPoolExecutor() as pool:
        for job, img in zip(jobs, pool.map(render, jobs)):
            artkit.save_png(img, OUT / f"{job[0]}.png", SOURCE)
    print(f"{len(jobs)} icons ({len(jobs) // len(SIZES)} items x {len(SIZES)} sizes)")


# --------------------------------------------------------------------------- review

def review():
    items = catalogue()
    hangar_map = Image.open(ROOT / "assets" / "ui" / "hangar-map.png").convert("RGBA")
    cols, cell = 6, 200
    rows = -(-len(items) // cols)
    sheet = raster.sheet(16 + max(960, cols * cell) + 16, 60 + 540 + 30 + rows * 70 + 20,
                         "HANGAR (FINAL R13): TACTICAL MAP AND EQUIPMENT ICONS", f"PRODUCTION ART, UI BATCH - {ROUND.upper()}")
    raster.draw_text(sheet, 16, 38, f"TACTICAL MAP 960X540 AT 1X ({artkit.colour_count([hangar_map])} COLOURS), "
                                    "BEHIND THE HANGAR'S GLASS PANELS", raster.LABEL)
    sheet.alpha_composite(hangar_map, (16, 52))
    y0 = 52 + 540 + 18
    raster.draw_text(sheet, 16, y0, f"{len(items)} ICONS: 16X16 (SHOP ROWS) AT 1X AND 3X, 24X24 (SELECTED, CALLOUTS) AT 1X AND 2X",
                     raster.LABEL)
    for i, (name, label, _, _) in enumerate(items):
        x, y = 16 + (i % cols) * cell, y0 + 16 + (i // cols) * 70
        small = Image.open(OUT / f"{name}.png").convert("RGBA")
        large = Image.open(OUT / f"{name}-large.png").convert("RGBA")
        plate_ = Image.new("RGBA", (cell - 8, 62), (6, 8, 26, 255))
        plate_.alpha_composite(small, (2, 2))
        plate_.alpha_composite(sprite.enlarge(small, 3), (22, 2))
        plate_.alpha_composite(large, (74, 2))
        plate_.alpha_composite(sprite.enlarge(large, 2), (102, 2))
        sheet.alpha_composite(plate_, (x, y))
        raster.draw_text(sheet, x + 2, y + 52, label.upper().replace('"', "")[:30], raster.LABEL_DIM)
    path = CONCEPT / f"hangar-final-{ROUND}-a.png"
    sheet.convert("RGB").save(path, optimize=True)
    print(f"review: {path.relative_to(ROOT)}")


if __name__ == "__main__":
    if "--review" not in sys.argv[1:]:
        build()
    review()
