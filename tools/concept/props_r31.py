#!/usr/bin/env python3
"""Concept models for Level 09's new props (M5 part C, concept round 31; design/campaign/
act-2-homefront/level-09-arcology-fall, Layout and Secrets and pickups). User decision D9 = a of M5
part C: the props go straight to production, so this module only holds the models (frozen once the
production art is approved); tools/art/backdrop_l09.py renders them into assets/.

  kilo_truck()   the Kilo convoy's CDF cargo truck facing up the screen (16 x 34 px, 1 world unit =
                 1 px): a cab with a slit windscreen and a pale CDF roof band, a tarp-covered bed
                 (sixty civilians ride under the tarps), three axles of fat tyres showing at the
                 sides, head and tail lights, an amber convoy beacon on the cab. Drawn in the
                 gunship kit's CDF olive-grey (props_r30's materials, imported unchanged) with a
                 khaki tarp of its own.
  cocoon(state)  the rooftop creep cocoon (44 x 40 px), a hardened destructible that hides a CDF
                 supply crate: state 0 intact (a lumpy teal-black chitin sac, as the Hive Node's
                 scheme, with glowing teal veins, rooted to the roof by tendrils, a tear at its lit
                 upper left showing the crate's amber hazard corner, the loot cue), 1 hit (scorched,
                 the tear split wide, the veins dimmed), 2 opened (the sac burst into peeled-back
                 petals round a dark hollow; the crate is the game's own pickup, revealed there).

Light and materials follow the frozen render package (render/sdf.py: the fixed top-left key light).

Run: python3 tools/concept/props_r31.py [out.png]   (a quick model preview at 4x; production art
is tools/art/backdrop_l09.py --props)
"""
import sys
from pathlib import Path

import numpy as np

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import ground_targets as gt  # noqa: E402  (hazard stripes)
import props_r30 as p30  # noqa: E402  (concept script, imported unchanged: the gunship kit's materials)
from render.sdf import (Material, panel_lines, rotate_z, sd_box, sd_capsule, sd_cylinder_x,  # noqa: E402
                        sd_ellipsoid, sd_sphere, subtract, union)

# --------------------------------------------------------------------------- the Kilo truck

TRUCK_SIZE = (16, 34)
_BASE = len(p30.tr_materials())
TARP, TARP_RIB, TYRE, BEACON = range(_BASE, _BASE + 4)
CDF, GLASS, DARK, HEAD, TAIL, BAND, STEEL = p30.CDF, p30.GLASS, p30.DARK, p30.HEAD, p30.TAIL, p30.BAND, p30.STEEL_T


def truck_materials():
    """props_r30's traffic materials (the CDF olive-grey, glass, lamps; the strobe off) plus the
    tarp, its ribs, the tyres and the amber beacon."""
    return p30.tr_materials(strobe=False) + [
        Material((0.42, 0.40, 0.30), metal=0.0, shininess=8, spec=0.08),
        Material((0.30, 0.29, 0.22), metal=0.0, shininess=8, spec=0.06),
        Material((0.07, 0.07, 0.08), metal=0.0, shininess=12, spec=0.15),
        Material((0.3, 0.18, 0.04), emission=(1.5, 0.85, 0.2)),
    ]


def kilo_truck():
    """The CDF cargo truck facing up: cab forward (y up), tarp-covered bed behind it."""
    def scene(p):
        items = [(sd_box(p, (0, 12.0, 3.4), (6.6, 3.8, 3.0), 1.2), CDF),                 # cab
                 (sd_box(p, (0, 15.4, 4.4), (5.4, 0.8, 1.2), 0.4), GLASS),               # windscreen
                 (sd_box(p, (0, 11.2, 6.5), (4.6, 1.2, 0.3), 0.2), BAND),                # pale roof band
                 (sd_box(p, (0, 16.0, 1.4), (6.4, 1.0, 1.2), 0.5), STEEL),               # bumper
                 (sd_box(p, (0, -4.0, 2.4), (7.2, 11.6, 1.6), 0.6), CDF)]                # bed and chassis
        rib = np.abs(((p[:, 1] + 16.0) / 4.6) % 1 - 0.5) < 0.09                           # tarp hoops
        tarp = sd_box(p, (0, -4.2, 5.2), (6.9, 11.0, 2.6), 2.6)
        items.append((tarp, TARP))
        items.append((np.where(rib, tarp - 0.25, 1e3), TARP_RIB))
        for y in (11.0, -6.0, -12.5):                                                    # three axles
            for sx in (-1, 1):
                items.append((sd_cylinder_x(p, (sx * 7.6, y, 1.8), 1.9, 1.0), TYRE))
        for sx in (-1, 1):
            items += [(sd_sphere(p, (sx * 4.6, 16.6, 2.4), 0.8), HEAD),
                      (sd_sphere(p, (sx * 5.8, -16.0, 2.6), 0.7), TAIL)]
        items.append((sd_sphere(p, (2.6, 11.2, 7.0), 0.8), BEACON))
        return union(*items, k=0.25)
    return scene


TRUCK_LIGHTS = [(-4.6, 16.6, "h"), (4.6, 16.6, "h"), (-5.8, -16.0, "t"), (5.8, -16.0, "t")]

# --------------------------------------------------------------------------- the creep cocoon

COCOON_SIZE = (44, 40)
SAC, VEIN, ROOT, AMBER, CRATE, VOID, ICHOR, CHAR = range(8)


def _veins(p, n):
    """Glowing veins over the sac: seven meridians from its crown, wavering."""
    r = np.hypot(p[:, 0], p[:, 1])
    a = np.sin(np.arctan2(p[:, 1], p[:, 0]) * 7 + 0.9 * np.sin(r * 0.45))
    return np.where((np.abs(a) < 0.11) & (r > 3.0), 1.0, 0.0)


def cocoon_materials(state):
    """The Hive Node's teal-black scheme (render/enemy_models.py, "teal-black") with teal veins;
    the crate's matte amber with the loot hazard stripes."""
    glow = (0.0, 0.9, 0.55) if state == 0 else (0.0, 0.55, 0.34) if state == 1 else (0.0, 0.2, 0.12)
    return [Material((0.12, 0.33, 0.32), metal=0.3, shininess=60, spec=0.6, pattern=panel_lines(0.35, 0.05, 0.82)),
            Material((0.02, 0.18, 0.12), emission=glow),
            Material((0.05, 0.16, 0.15), metal=0.2, shininess=30, spec=0.3),
            Material((0.98, 0.66, 0.14), metal=0.0, shininess=8, spec=0.05, pattern=gt.hazard_stripes),
            Material((0.36, 0.38, 0.30), metal=0.3, shininess=30, spec=0.3),
            Material((0.02, 0.03, 0.03), metal=0.0, shininess=4, spec=0.0),
            Material((0.05, 0.4, 0.25), emission=(0.0, 0.5, 0.3), shininess=80, spec=0.9),
            Material((0.08, 0.07, 0.07), metal=0.1, shininess=10, spec=0.1)]


def cocoon(state):
    """The sac over the crate on a low roof, tendrils rooting it; ``state`` 0 intact, 1 hit, 2 opened."""
    def scene(p):
        roots = []
        for k, (ang, ln) in enumerate(((0.3, 19), (1.2, 17), (2.2, 18), (3.0, 20), (3.9, 16), (4.8, 19), (5.6, 17))):
            end = (np.cos(ang) * ln, np.sin(ang) * ln * 0.88, 0.4)
            mid = (np.cos(ang + 0.25) * ln * 0.6, np.sin(ang + 0.25) * ln * 0.55, 1.4)
            roots += [(sd_capsule(p, (0, 0, 3.0), mid, 1.6, 1.0), ROOT), (sd_capsule(p, mid, end, 1.0, 0.45), ROOT)]
        if state < 2:
            body = sd_ellipsoid(p, (0.5, 0.0, 4.0), (14.5, 12.5, 8.6))
            for c, r in (((-7, 5, 6.5), (7, 6.5, 5.2)), ((7, -5, 5.5), (7.5, 6, 5.4)), ((5, 7, 6), (6, 5, 4.6)),
                         ((-6, -6, 5), (6, 5.5, 4.4))):
                body = np.minimum(body, sd_ellipsoid(p, c, r))
            sac = (body, SAC)
            vein = (np.where(_veins(p, None) > 0, body - 0.3, 1e3), VEIN)
            tear_c, tear_r = ((-7.5, 6.0, 9.0), (6.8, 5.0, 5.2)) if state == 0 else ((-5.0, 4.5, 9.5), (9.0, 6.5, 6.0))
            d, m = union(*roots, sac, vein, k=1.2)
            d, m = subtract((d, m), sd_ellipsoid(p, tear_c, tear_r))
            crate = [(sd_box(p, (-2.0, 1.5, 3.0), (10.0, 7.0, 3.6), 0.8), CRATE),
                     (sd_box(p, (-8.5, 3.0, 3.6), (3.6, 7.6, 4.4), 0.5), AMBER)]
            d, m = union((d, m), *crate)
            if state == 1:
                drips = [(sd_sphere(p, (x, y, 7.5), r), ICHOR) for x, y, r in ((4, -2, 1.6), (9, 3, 1.2), (-1, -7, 1.3))]
                d, m = union((d, m), *drips, k=0.6)
            return d, m
        petals = []
        for k in range(6):                                                        # the burst sac, peeled back
            ang = k * np.pi / 3 + 0.3
            c = np.array([np.cos(ang) * 12.5, np.sin(ang) * 10.5, 1.6])
            q = rotate_z(p - c, -ang)
            petal = sd_ellipsoid(q, (0, 0, 0), (7.0, 4.6, 1.3))
            petals.append((petal, SAC))
            petals.append((np.where(_veins(p, None) > 0, petal - 0.2, 1e3), VEIN))
        hollow = (sd_ellipsoid(p, (0.0, 0.0, 0.6), (9.0, 7.5, 1.4)), VOID)
        rim = (np.maximum(sd_ellipsoid(p, (0.0, 0.0, 0.8), (11.0, 9.5, 2.6)), -sd_ellipsoid(p, (0.0, 0.0, 0.8), (8.6, 7.2, 4.0))), SAC)
        d, m = union(*roots, *petals, rim, k=1.0)
        d, m = union((d, m), hollow)
        char = [(sd_sphere(p, (x, y, 1.0), r), CHAR) for x, y, r in ((5, 4, 2.0), (-6, -3, 2.4), (2, -6, 1.6))]
        ichor = [(sd_sphere(p, (x, y, 1.2), r), ICHOR) for x, y, r in ((-3, 5, 1.4), (7, -4, 1.1))]
        return union((d, m), *char, *ichor)
    return scene


COCOON_GLOW_SPOTS = [(-7.0, 6.0)]   # model-space centre of the tear (the loot corner's glint)


if __name__ == "__main__":
    from PIL import Image
    from render import sdf, sprite
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else HERE / "props_r31_preview.png"
    tiles = []
    for s in range(3):
        hi = sdf.render(cocoon(s), cocoon_materials(s), (44 * 8, 40 * 8), 44.0, z_top=40.0, steps=160)
        tiles.append(sprite.make_sprite(hi, 8, 32, crisp=60))
    hi = sdf.render(kilo_truck(), truck_materials(), (16 * 8, 34 * 8), 16.0, z_top=34.0, steps=160)
    tiles.append(sprite.make_sprite(hi, 8, 32, crisp=60))
    sheet = Image.new("RGBA", (sum(t.width * 4 + 8 for t in tiles), 160), (24, 26, 52, 255))
    x = 0
    for t in tiles:
        sheet.alpha_composite(t.resize((t.width * 4, t.height * 4), Image.NEAREST), (x, 0))
        x += t.width * 4 + 8
    sheet.save(out)
    print(out)
