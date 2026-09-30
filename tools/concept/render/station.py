"""Kit of pre-rendered UTC structure parts (round 02): station trusses, modules, solar arrays,
radiators, docking node, dish, turret, cargo, plus a Mars colony dome. Parts are rendered once
with the SDF ray-marcher and then kit-bashed in 2D, the way 90s games built tile sets.

Units: 1 world unit = 1 native pixel at 960x540. Every ``*_part`` returns (scene, materials,
(width, height)); use ``render_part`` to get a native-size sprite.
"""
import numpy as np

from . import sdf, sprite
from .sdf import (Material, mirror_x, panel_lines, sd_box, sd_capsule, sd_cylinder_x,
                  sd_cylinder_y, sd_cylinder_z, sd_sphere, union)

HULL, DARK, SOLAR, ACCENT, RED, WARM, GLASS = range(7)


def materials(pal):
    """Station materials in a palette (render.palette.Palette)."""
    def grid(p, n):
        return np.where(((p[:, 0] * 0.34) % 1 < 0.12) | ((p[:, 1] * 0.34) % 1 < 0.12), 2.6, 1.0)
    return [
        Material(pal.f("UTC HULL", 4), metal=0.4, shininess=50, spec=0.5,
                 pattern=panel_lines(0.09, 0.05, 0.8)),
        Material(pal.f("UTC HULL", 1), metal=0.6, shininess=40, spec=0.5),
        Material(pal.f("EARTH ORBIT", 2), metal=0.8, shininess=90, spec=0.9, pattern=grid),
        Material(pal.f("UTC ACCENTS", 4), metal=0.3, shininess=40, spec=0.4),
        Material((0.1, 0.1, 0.1), emission=(1.6, 0.2, 0.25)),
        Material((0.1, 0.1, 0.1), emission=tuple(np.array(pal.f("UTC ACCENTS", 5)) * 1.3)),
        Material(pal.f("EARTH ORBIT", 3), metal=0.9, shininess=120, spec=1.2),
    ]


def truss_v_part(bay=38, width=34):
    """One vertical truss bay; rails run past the bay ends so bays tile seamlessly."""
    hw = width / 2 - 3

    def scene(p):
        q = mirror_x(p)
        return union(
            (sd_box(q, (hw, 0, 0), (2.2, bay, 2.2), 0.6), DARK),
            (sd_box(p, (0, 0, -1), (hw, 1.6, 1.4), 0.4), DARK),
            (sd_capsule(p, (-hw, -bay / 2, -1), (hw, bay / 2, -1), 1.1), DARK),
            (sd_cylinder_y(p, (hw - 5, 0, 2.5), 1.6, bay), ACCENT),
        )
    return scene, (width, bay)


def truss_h_part(length, width=26):
    """Horizontal truss of any length (rails extend past the ends)."""
    hw = width / 2 - 3
    bays = max(1, int(round(length / 30)))
    pitch = length / bays

    def scene(p):
        items = [(sd_box(p, (0, y, 0), (length, 2.0, 2.0), 0.5), DARK) for y in (-hw, hw)]
        for i in range(bays):
            x0 = -length / 2 + i * pitch
            items.append((sd_box(p, (x0, 0, -1), (1.4, hw, 1.2), 0.4), DARK))
            items.append((sd_capsule(p, (x0, -hw, -1), (x0 + pitch, hw, -1), 1.0), DARK))
        items.append((sd_cylinder_x(p, (0, -hw + 5, 2.2), 1.4, length), HULL))
        return union(*items)
    return scene, (length, width)


def drum_part(radius=22, half=46, windows=True):
    """Habitat drum along the spine (Y axis) with domed ends, ribs, a lit window row."""
    def scene(p):
        items = [
            (sd_cylinder_y(p, (0, 0, 0), radius, half), HULL),
            (sd_sphere(p, (0, half, -4), radius * 0.85), HULL),
            (sd_sphere(p, (0, -half, -4), radius * 0.85), HULL),
        ]
        d, m = union(*items, k=4)
        ribs = [(sd_cylinder_y(p, (0, y, 0), radius + 1.8, 2.2), DARK)
                for y in np.linspace(-half + 8, half - 8, 5)]
        d, m = union((d, m), *ribs, (sd_box(p, (0, half * 0.35, radius - 1), (radius * 0.7, 4, 3),
                                             1), ACCENT))
        if windows:
            wins = [(sd_box(p, (x, -half * 0.3, radius - 0.5), (1.6, 3, 2), 0.5), WARM)
                    for x in np.linspace(-radius * 0.6, radius * 0.6, 5)]
            d, m = union((d, m), *wins)
        return d, m
    return scene, (radius * 2 + 8, half * 2 + radius * 2)


def solar_part(length=150, depth=56, left=True):
    """Solar array wing: two panel blankets on a mast; the mast root is at the spine side."""
    sign = -1 if left else 1

    def scene(p):
        q = p.copy()
        q[:, 0] = q[:, 0] * sign                       # build pointing +x, flip for left
        items = [(sd_cylinder_x(q, (length / 2 - 4, 0, 1), 2.4, length / 2), DARK)]
        for y in (-depth / 4 - 2, depth / 4 + 2):
            items.append((sd_box(q, (length / 2 + 6, y, 0), (length / 2 - 10, depth / 4 - 2, 0.8),
                                 0.2), SOLAR))
            items.append((sd_box(q, (length / 2 + 6, y, 0.6),
                                 (length / 2 - 10, depth / 4 - 2, 0.4), 0.0), SOLAR))
        items.append((sd_sphere(q, (length - 2, 0, 1), 2.4), RED))
        return union(*items)
    return scene, (length * 2, depth)


def radiator_part(length=90, fins=3):
    def scene(p):
        items = [(sd_box(p, (0, (i - (fins - 1) / 2) * 14, 0), (length / 2, 5, 0.8), 0.3), HULL)
                 for i in range(fins)]
        items.append((sd_box(p, (-length / 2 + 4, 0, 1), (3, fins * 7, 2), 0.5), DARK))
        return union(*items)
    return scene, (length + 8, fins * 14 + 8)


def dock_part(radius=24):
    def scene(p):
        q = mirror_x(p)
        d, m = union(
            (sd_sphere(p, (0, 0, 0), radius), HULL),
            (sd_cylinder_z(p, (0, 0, 4), radius + 6, 2.5), DARK),
            (sd_cylinder_x(q, (radius + 6, 0, 0), 8, 8), HULL),
            (sd_cylinder_x(q, (radius + 15, 0, 0), 9, 1.5), ACCENT),
            k=3,
        )
        return union((d, m), (sd_sphere(p, (0, 0, radius - 1), 3), RED))
    return scene, (radius * 2 + 44, radius * 2 + 16)


def dish_part(radius=18):
    def scene(p):
        bowl = sd_sphere(p, (0, 0, -radius * 0.6), radius)
        cut = sd_sphere(p, (0, 0, radius * 0.75), radius * 1.15)
        d = np.maximum(bowl, -cut)
        return union((d, HULL), (sd_capsule(p, (0, 0, -2), (0, 0, radius * 0.7), 1.2), DARK),
                     (sd_sphere(p, (0, 0, radius * 0.7), 2.2), RED))
    return scene, (radius * 2 + 8, radius * 2 + 8)


def turret_part():
    """CDF point-defence turret (twin barrels pointing up the screen)."""
    def scene(p):
        q = mirror_x(p)
        return union(
            (sd_cylinder_z(p, (0, 0, 0), 9, 2.5), DARK),
            (sd_sphere(p, (0, 0, 2), 7), HULL),
            (sd_capsule(q, (2.6, 2, 5), (2.6, 13, 5), 1.3), DARK),
            (sd_box(p, (0, -3, 7), (3, 2, 1), 0.5), ACCENT),
        )
    return scene, (26, 32)


def cargo_part(color=ACCENT):
    def scene(p):
        return union((sd_box(p, (0, 0, 0), (7, 12, 6), 1), color),
                     (sd_box(p, (0, 0, 0), (7.4, 1, 6.4), 0.5), DARK),
                     (sd_box(p, (0, 8, 0), (7.4, 1, 6.4), 0.5), DARK),
                     (sd_box(p, (0, -8, 0), (7.4, 1, 6.4), 0.5), DARK))
    return scene, (18, 28)


def dome_part(radius=26):
    """Mars colony habitat dome: glass hemisphere on a ring base with an airlock."""
    def scene(p):
        return union(
            (sd_cylinder_z(p, (0, 0, 0), radius + 4, 3), HULL),
            (np.maximum(sd_sphere(p, (0, 0, 0), radius), -p[:, 2]), GLASS),
            (sd_box(p, (radius + 4, 0, 1), (7, 5, 4), 1), HULL),
            (sd_box(p, (radius + 9, 0, 4), (2, 3, 2), 0.5), ACCENT),
            (sd_sphere(p, (0, 0, radius - 2), 2.5), WARM),
        )
    return scene, (radius * 2 + 30, radius * 2 + 12)


def render_part(part, pal, factor=4, colors=40, z_top=None):
    """Render a part to a native-size palettised sprite."""
    scene, (w, h) = part
    mats = materials(pal)
    w, h = int(np.ceil(w)), int(np.ceil(h))
    z = z_top if z_top is not None else max(w, h)
    hi = sdf.render(scene, mats, (w * factor, h * factor), float(w), z_top=z, steps=150)
    return sprite.make_sprite(hi, factor, colors, crisp=60)
