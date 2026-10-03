#!/usr/bin/env python3
"""Concept round 16 - the civilian crawler, the CDF evacuation hauler Level 04 escorts.

Outputs (design/allies/concept/):
  civilian-crawler-r16-a.png  A: tracked crawler-transporter, 72x40 (wide reading): a flat deck
                              on four twin-track trucks at the corners, a pressurised passenger
                              drum across the deck, a row of cargo pods, a corner cab
  civilian-crawler-r16-b.png  B: six-wheeled rover train, 72x40 (wide reading): three pressurised
                              cylinders abreast (two passenger cars flanking the cab car) on a
                              coupled frame, three balloon wheels down each side
  civilian-crawler-r16-c.png  C: pressurised bus with a cargo sled, 40x72 (long reading): a
                              six-wheeled bus with a glass nose towing a sled of four cargo pods

Each sheet shows the crawler on the chosen Luna scene's regolith (scenes_r06.LunaScene's grey
ramp, hillshade and long top-left shadows, a graded road with tyre tracks); a column of five on a
winding road at 1x, 60 px apart as in Level 04 (C at 84 px, since 72 px long crawlers 60 px apart
would overlap); a 3x close-up; the damaged (smoke, below 50 % HP) and wrecked (stopped, burning,
lights out) looks at 2x and all three states at 1x. All amber is emissive (hazard beacons): matte
amber markings on the ground layer are reserved for loot targets (art direction, readability
rule 7), so the hulls use the station kit's muted accents (Level 01's KIT_PAL) and a CDF blue
stripe. Models: SDF ray-marched with render/sdf.py and the station kit materials
(render/station.py), key light fixed at the top-left, top-down view.
Run: python3 tools/concept/allies_r16.py [a] [b] [c]   (no args = all three; a few seconds each)
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import parallax_r02 as r02  # noqa: E402
import scenes_r06 as r06  # noqa: E402
from backdrop_l01 import KIT_PAL  # noqa: E402
from ground_targets import scorch  # noqa: E402
from render import raster, sdf, sprite, station  # noqa: E402
from render.config import ROOT, out_path  # noqa: E402
from render.sdf import (Material, mirror_x, sd_box, sd_capsule, sd_cylinder_x,  # noqa: E402
                        sd_cylinder_y, sd_cylinder_z, sd_sphere, union)
from render.station import ACCENT, DARK, GLASS, HULL, WARM  # noqa: E402

OUT = ROOT / "design" / "allies" / "concept"
AMBER, TREAD, BLUE = 7, 8, 9
SHADOW = r06.SHADOW["ground"]
SHEET_W, SHEET_H = 960, 664
FIELD = (480, 600)                       # column strip (period 600 for the wrap-safe helpers)


# --------------------------------------------------------------------------- materials

def cleats(axis):
    """Track cleats / tyre tread: dark bars across the rolling direction."""
    return lambda p, n: np.where((p[:, axis] * 0.75) % 1 < 0.42, 0.45, 1.0)


def materials(lit=True):
    mats = station.materials(KIT_PAL)
    if not lit:                          # wreck: windows and beacons dead
        mats[WARM] = Material((0.08, 0.07, 0.06), shininess=60, spec=0.6)
    amber = (Material((0.12, 0.07, 0.02), emission=(2.0, 0.78, 0.08)) if lit
             else Material((0.22, 0.12, 0.05), shininess=70, spec=0.7))
    return mats + [amber,
                   Material((0.15, 0.15, 0.17), metal=0.2, shininess=14, spec=0.2, pattern=cleats(1)),
                   Material(KIT_PAL.f("UTC ACCENTS", 0), metal=0.3, shininess=50, spec=0.5)]


# --------------------------------------------------------------------------- models
# Units: 1 = 1 native px. x right, y up the screen (the direction of travel), z towards the camera.

def crawler_a():
    """Tracked crawler-transporter: deck on four twin-track trucks at the corners."""
    def scene(p):
        q = mirror_x(p)
        qq = q.copy()
        qq[:, 1] = np.abs(qq[:, 1])                                  # mirror in y as well
        items = [
            (sd_box(qq, (27.5, 13.5, -3), (3.0, 6.5, 2.6), 0.9), TREAD),       # outer track
            (sd_box(qq, (21.0, 13.5, -3), (3.0, 6.5, 2.6), 0.9), TREAD),       # inner track
            (sd_box(qq, (24.2, 13.5, -1.2), (6.6, 4.5, 1.6), 0.6), DARK),      # truck frame
            (sd_box(p, (0, 0, 1.2), (30.5, 12.5, 2.2), 1.0), HULL),            # main deck
            (sd_box(q, (30.6, 0, 1.0), (1.0, 8, 1.6), 0.4), DARK),             # side girders
            (sd_box(p, (0, 0, 3.0), (31.0, 1.0, 0.8), 0.3), DARK),             # deck seam
        ]
        # pressurised passenger drum across the deck, CDF stripe ring at each end
        items += [(sd_cylinder_x(p, (-3, 4.5, 7.0), 5.6, 19.5), HULL),
                  (sd_sphere(p, (16.5, 4.5, 7.0), 5.6), HULL),
                  (sd_sphere(p, (-22.5, 4.5, 7.0), 5.6), HULL)]
        items += [(sd_cylinder_x(p, (x, 4.5, 7.0), 6.2, 1.1), BLUE) for x in (-17, 11)]
        items += [(sd_box(p, (x, 2.0, 12.0), (1.2, 1.0, 0.8), 0.3), WARM)
                  for x in np.linspace(-13, 7, 6)]
        # cargo pods along the rear edge
        items += [(sd_box(p, (x, -7.5, 5.5), (5.2, 3.6, 2.6), 0.8), ACCENT) for x in (-19, -7, 5)]
        items += [(sd_box(p, (x, -7.5, 5.6), (0.6, 3.9, 2.9), 0.3), DARK) for x in (-19, -7, 5)]
        # cab at the front right, glass to the front; radiator and stacks behind it
        items += [(sd_box(p, (22.5, 6.5, 6.0), (5.0, 4.4, 3.2), 1.2), HULL),
                  (sd_box(p, (22.5, 9.6, 8.6), (4.0, 1.3, 1.0), 0.5), GLASS),
                  (sd_box(p, (22.5, -5.5, 4.0), (4.6, 5.0, 0.7), 0.3), DARK),
                  (sd_cylinder_z(p, (19.0, -10.0, 6.0), 1.2, 3.0), DARK),
                  (sd_cylinder_z(p, (26.0, -10.0, 6.0), 1.2, 3.0), DARK)]
        # amber hazard beacons: the four deck corners and the cab roof
        items += [(sd_sphere(qq, (29.0, 11.0, 4.6), 2.0), AMBER),
                  (sd_sphere(p, (22.5, 4.0, 9.6), 1.6), AMBER)]
        return union(*items)
    return scene, (72, 40)


def crawler_b():
    """Six-wheeled rover train: three pressurised cylinders abreast on a coupled frame."""
    def scene(p):
        q = mirror_x(p)
        items = [(sd_box(p, (0, y, 0.0), (29.0, 1.6, 1.4), 0.5), DARK) for y in (-9.0, 9.0)]
        # outer passenger cars and the centre cab car (cylinders along y with domed ends)
        items += [(sd_capsule(q, (21.5, -10.5, 4.0), (21.5, 10.5, 4.0), 7.4), HULL),
                  (sd_capsule(p, (0, -11.0, 4.6), (0, 9.0, 4.6), 8.4), HULL)]
        items += [(sd_cylinder_y(q, (21.5, y, 4.0), 8.0, 1.0), BLUE) for y in (-6.0, 6.0)]
        items += [(sd_box(q, (21.5, y, 11.1), (1.0, 1.3, 0.8), 0.3), WARM)
                  for y in np.linspace(-8, 8, 5)]
        items += [(sd_sphere(p, (0, 11.5, 6.2), 6.4), GLASS),                  # cab nose glass
                  (sd_box(p, (0, -4.0, 12.6), (4.4, 6.0, 0.9), 0.4), DARK),    # roof radiator
                  (sd_box(p, (0, -4.0, 13.4), (4.6, 0.5, 0.4), 0.2), HULL),
                  (sd_cylinder_y(p, (0, 0, 4.6), 9.0, 1.0), BLUE)]
        # six balloon wheels, three down each outer side
        items += [(sd_cylinder_x(q, (31.5, y, -1.0), 5.6, 2.6), TREAD) for y in (-12.5, 0.0, 12.5)]
        items += [(sd_cylinder_x(q, (31.9, y, -1.0), 2.2, 2.4), DARK) for y in (-12.5, 0.0, 12.5)]
        # amber beacons: front and back of each passenger car, the cab roof
        items += [(sd_sphere(q, (21.5, y, 11.4), 1.8), AMBER) for y in (-14.5, 14.5)]
        items += [(sd_sphere(p, (0, 4.5, 13.2), 1.7), AMBER)]
        return union(*items)
    return scene, (72, 40)


def crawler_c():
    """Pressurised bus towing a cargo sled (the long reading)."""
    def scene(p):
        q = mirror_x(p)
        items = [
            (sd_box(p, (0, 19.0, 4.5), (13.6, 15.5, 5.2), 4.0), HULL),           # bus body
            (sd_box(p, (0, 19.0, 9.6), (2.4, 14.0, 0.6), 0.3), BLUE),            # roof stripe
            (sd_box(p, (0, 33.2, 6.0), (10.0, 1.6, 2.4), 1.0), GLASS),           # glass nose
            (sd_box(p, (0, 8.0, 9.8), (6.5, 4.0, 0.8), 0.4), DARK),              # roof radiator
            (sd_capsule(p, (0, 3.0, 0.5), (0, -6.0, 0.5), 1.4), DARK),           # tow bar
            (sd_box(p, (0, -21.0, 0.0), (14.0, 14.5, 1.4), 0.6), DARK),          # sled deck
        ]
        items += [(sd_box(q, (7.0, y, 9.4), (1.0, 1.2, 0.8), 0.3), WARM)
                  for y in np.linspace(8, 29, 6)]
        items += [(sd_cylinder_x(q, (15.6, y, -0.5), 4.6, 2.2), TREAD) for y in (8.0, 18.0, 28.0)]
        items += [(sd_capsule(q, (16.0, -34.5, -1.0), (16.0, -7.5, -1.0), 1.6), DARK)]  # skids
        # four cargo pods on the sled, strapped
        for x in (-7.0, 7.0):
            for y in (-14.0, -28.0):
                items += [(sd_box(p, (x, y, 4.2), (5.8, 5.6, 3.4), 0.9), ACCENT),
                          (sd_box(p, (x, y, 4.3), (6.1, 0.6, 3.7), 0.3), DARK)]
        # amber beacons: bus roof corners, sled rear corners
        items += [(sd_sphere(q, (10.5, 30.0, 9.0), 1.8), AMBER),
                  (sd_sphere(q, (10.5, 8.5, 9.0), 1.3), AMBER),
                  (sd_sphere(q, (13.0, -34.0, 2.6), 1.8), AMBER)]
        return union(*items)
    return scene, (40, 72)


def mirrored(*pts):
    return [(sx * x, y) for x, y in pts for sx in ((1, -1) if x else (1,))]


# beacon centres in model units, for the additive glow composited around them
BEACONS = {"a": mirrored((29, 11), (29, -11), (22.5, 4)),
           "b": mirrored((21.5, 14.5), (21.5, -14.5), (0, 4.5)),
           "c": mirrored((10.5, 30), (10.5, 8.5), (13, -34))}

VARIANTS = {
    "a": (crawler_a, "A - TRACKED CRAWLER-TRANSPORTER", "72x40 WIDE READING", 60),
    "b": (crawler_b, "B - SIX-WHEELED ROVER TRAIN", "72x40 WIDE READING", 60),
    "c": (crawler_c, "C - PRESSURISED BUS + CARGO SLED", "40x72 LONG READING", 84),
}


def render(model, lit=True, factor=4):
    scene, (w, h) = model
    key = sdf.KEY_POS * (72 / w)          # the same light position for the wide and long models
    hi = sdf.render(scene, materials(lit), (w * factor, h * factor), float(w), z_top=max(w, h),
                    steps=160, key_pos=key)
    return sprite.make_sprite(hi, factor, 40, crisp=60)


# --------------------------------------------------------------------------- damage states

def puff_layer(size, puffs, seed, dark=1.0):
    """Smoke puffs [(x, y, r, alpha)] shaded from the top-left, speckled, on a transparent layer."""
    w, h = size
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64)
    rgb = np.zeros((h, w, 3))
    a = np.zeros((h, w))
    speck = np.random.default_rng(seed).random((h, w))
    for x, y, r, al in puffs:
        d = np.hypot(xx - x, yy - y) / r
        cov = np.clip((1 - d) * 3, 0, 1) * al * (0.85 + 0.15 * speck)
        lit = np.clip(1 - ((xx - x) + (yy - y)) / (2.2 * r), 0.35, 1.4)
        col = np.array([118, 114, 112]) * dark * lit[..., None]
        rgb = rgb * (1 - cov[..., None]) + col * cov[..., None]
        a = a + cov * (1 - a)
    img = raster.to_rgba_image(np.clip(rgb, 0, 255), np.clip(a * 255, 0, 255))
    return r02.posterize(img, 10)


def damaged_look(intact, seed):
    """Below 50 % HP: two scorch spots and a smoke trail left behind (screen down)."""
    w, h = intact.size
    arr = scorch(np.array(intact).astype(np.float64), [(w * 0.62, h * 0.36, 5), (w * 0.3, h * 0.64, 4)],
                 seed)
    body = Image.fromarray(arr.astype(np.uint8), "RGBA")
    rng = np.random.default_rng(seed)
    pw, ph = w + 40, h + 70
    sx, sy = w * 0.62 + 20, h * 0.36 + 10
    puffs = [(sx + rng.uniform(-2, 2) + k * 0.6, sy + k * 7.5, 3.5 + k * 1.3, 0.85 - k * 0.09)
             for k in range(8)]
    return body, puff_layer((pw, ph), puffs, seed), (20, 10)


def wrecked_look(dead, seed):
    """Destroyed: stopped on the road, charred, lights out, fires and a dark smoke column."""
    w, h = dead.size
    arr = np.array(dead).astype(np.float64)
    arr[..., :3] *= 0.62
    rng = np.random.default_rng(seed)
    spots = [(rng.uniform(0.15, 0.85) * w, rng.uniform(0.15, 0.85) * h, rng.uniform(4, 8))
             for _ in range(6)]
    arr = scorch(arr, spots, seed)
    body = Image.fromarray(arr.astype(np.uint8), "RGBA")
    fires = spots[:3]
    pw, ph = w + 40, h + 60
    puffs = []
    for i, (fx, fy, _) in enumerate(fires[:2]):
        puffs += [(fx + 20 + k * 0.8, fy + 4 + k * 6.5, 3 + k * 1.5, 0.9 - k * 0.1) for k in range(7)]
    smoke = puff_layer((pw, ph), puffs, seed, dark=0.42)
    return body, smoke, (20, 4), fires


def add_fires(img, fires, ox, oy):
    """Small flames: an orange glow with a yellow-white core (additive)."""
    for fx, fy, _ in fires:
        img = raster.add_light(img, ox + fx, oy + fy, 4.0, (255, 110, 30), 0.9)
        img = raster.add_light(img, ox + fx, oy + fy, 1.6, (255, 230, 150), 1.0)
    return img


# --------------------------------------------------------------------------- regolith

def regolith(size, seed, road_x, road_half):
    """Luna regolith in the chosen scene's look (scenes_r06.LunaScene._ground) with a graded road
    wide enough for the crawler; periodic in y."""
    w, per = size
    rng = np.random.default_rng(seed)
    hgt = r02.periodic_fbm(w, per, 120, seed + 1, octaves=5) * 0.5
    hgt += r02.periodic_fbm(w, per, 24, seed + 2, octaves=1) * 0.05
    yy, xx = np.mgrid[0:per, 0:w]
    rx = road_x(yy)
    for _ in range(int(w * per / 7000)):
        cx, cy, r = rng.uniform(0, w), rng.uniform(0, per), rng.uniform(4, 20)
        if abs(cx - road_x(cy)) < road_half + r:
            continue
        d = np.hypot(xx - cx, r06.periodic_dist(yy, cy, per)) / r
        hgt += (np.where(d < 1, -(1 - d * d) * 0.55, 0) + np.exp(-((d - 1) / 0.16) ** 2) * 0.2) \
            * min(1.0, r / 26)
    road = np.clip(1 - (np.abs(xx - rx) - road_half) / 5, 0, 1)
    hgt = hgt * (1 - road) + (hgt * 0.3 + 0.1) * road
    shade = r06.hillshade(hgt * 60, 0.55)
    lit = r06.cast_shadows(hgt * 60, steps=28, drop=1.0)
    t = (hgt - hgt.min()) / (hgt.max() - hgt.min())
    col = r06.ramp_img(r06.LunaScene.GREYS, 0.25 + t * 0.6) * (shade * (0.35 + 0.65 * lit))[..., None]
    col = col + np.array((14, 18, 34)) * (1 - lit)[..., None] * 0.9
    col = col * (1 - 0.28 * road[..., None])
    for off in (-road_half + 6, road_half - 6):
        col[np.abs(xx - rx - off) < 1.5] *= 0.74
    img = r06.smooth_tex(r02.posterize(raster.to_rgba_image(np.clip(col * 0.8, 0, 255)), 9))
    return img.convert("RGBA")


def place(ground, spr, x, y, smoke=None, smoke_off=(0, 0), fires=None, lights=()):
    """Composite a crawler centred at (x, y) with its ground shadow, beacon glow, smoke, fires."""
    sx, sy = x - spr.width / 2, y - spr.height / 2
    sprite.paste(ground, sprite.shadow_of(spr, opacity=0.55, blur=1.0), sx + SHADOW[0], sy + SHADOW[1])
    sprite.paste(ground, spr, sx, sy)
    for bx, by in lights:
        ground = raster.add_light(ground, x + bx, y - by, 2.6, (255, 128, 24), 0.75)
    if fires:
        ground = add_fires(ground, fires, sx, sy)
    if smoke is not None:
        sprite.paste(ground, sprite.shadow_of(smoke, opacity=0.35, blur=1.5),
                     sx - smoke_off[0] + 4, sy - smoke_off[1] + 6)
        sprite.paste(ground, smoke, sx - smoke_off[0], sy - smoke_off[1])
    return ground


# --------------------------------------------------------------------------- sheet

def label(img, x, y, text, dim=False):
    raster.draw_text(img, x, y, text, raster.LABEL_DIM if dim else raster.LABEL)


def zoom_view(ground, cx, cy, rw, rh, scale):
    box = (int(cx - rw / 2), int(cy - rh / 2), int(cx - rw / 2) + rw, int(cy - rh / 2) + rh)
    return sprite.enlarge(ground.crop(box), scale)


def build(key):
    make, title, reading, spacing = VARIANTS[key]
    model = make()
    w, h = model[1]
    intact = render(model)
    dead = render(model, lit=False)
    dmg_body, dmg_smoke, dmg_off = damaged_look(intact, 31)
    lights = BEACONS[key]
    wreck_body, wreck_smoke, wreck_off, fires = wrecked_look(dead, 47)
    road_half = w / 2 + 7

    sheet = raster.sheet(SHEET_W, SHEET_H, "CIVILIAN CRAWLER R16 " + title, reading + " - L04 ESCORT")

    # column of five on a winding road, 1x
    fw, fh = FIELD
    def road_x(y):
        return fw / 2 + 46 * np.sin(2 * np.pi * y / fh) + 12 * np.sin(4 * np.pi * y / fh + 1.1)
    strip = regolith(FIELD, 1601, road_x, road_half)
    y0 = fh / 2 - 2 * spacing + 12
    for i in range(5):
        y = y0 + i * spacing
        x = road_x(y)
        if i == 2:
            strip = place(strip, dmg_body, x, y, dmg_smoke, dmg_off, lights=lights)
        else:
            strip = place(strip, intact, x, y, lights=lights)
    sheet.alpha_composite(strip, (8, 44))
    raster.frame(sheet, (7, 43, 8 + fw, 44 + fh))
    note = " (60 WOULD OVERLAP)" if spacing != 60 else ", 3RD DAMAGED"
    label(sheet, 8, 33, f"COLUMN OF FIVE AT 1X, {spacing} PX APART{note}")

    # straight-road detail field for the close-ups
    dfw, dfh = 480, 600
    detail = regolith((dfw, dfh), 1611, lambda y: dfw / 2 + 0 * y, road_half)
    spots = {"intact": (240, 110), "damaged": (240, 300), "wrecked": (240, 480)}
    detail = place(detail, intact, *spots["intact"], lights=lights)
    detail = place(detail, dmg_body, *spots["damaged"], dmg_smoke, dmg_off, lights=lights)
    detail = place(detail, wreck_body, *spots["wrecked"], wreck_smoke, wreck_off, fires)

    rx = 496
    label(sheet, rx, 33, "INTACT 3X - AMBER HAZARD BEACONS LIT")
    big = zoom_view(detail, *spots["intact"], 152, 80, 3)
    sheet.alpha_composite(big, (rx, 44))
    raster.frame(sheet, (rx - 1, 43, rx + big.width, 44 + big.height))

    yz = 44 + big.height + 22
    for i, name in enumerate(("damaged", "wrecked")):
        x = rx + i * 232
        text = "DAMAGED 2X - SMOKE BELOW 50%" if name == "damaged" else "WRECKED 2X - BURNS ON ROAD"
        label(sheet, x, yz - 11, text)
        cx, cy = spots[name]
        z = zoom_view(detail, cx, cy + 10, 112, 96, 2)
        sheet.alpha_composite(z, (x, yz))
        raster.frame(sheet, (x - 1, yz - 1, x + z.width, yz + z.height))

    y1 = yz + 192 + 22
    label(sheet, rx, y1 - 11, "AT 1X: INTACT / DAMAGED / WRECKED")
    for i, name in enumerate(("intact", "damaged", "wrecked")):
        x = rx + i * 152
        cx, cy = spots[name]
        t = detail.crop((cx - 72, cy - 50, cx + 72, cy + 70))
        sheet.alpha_composite(t, (x, y1))
        raster.frame(sheet, (x - 1, y1 - 1, x + t.width, y1 + t.height))
    label(sheet, rx, SHEET_H - 12, "SDF MODEL, STATION KIT (KIT_PAL), KEY LIGHT TOP-LEFT", dim=True)
    return sheet


def main(keys):
    OUT.mkdir(parents=True, exist_ok=True)
    for key in keys:
        path = out_path(OUT, f"civilian-crawler-r16-{key}.png")
        build(key).convert("RGB").save(path, optimize=True)
        print("wrote", path.relative_to(ROOT))


if __name__ == "__main__":
    main(sys.argv[1:] or list(VARIANTS))
