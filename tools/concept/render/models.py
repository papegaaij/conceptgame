"""SDF models for concept round 01: three AF-12 Stormhawk variants, Vrell enemies and an
orbital station segment. World units: the player ship is 2.0 units long (nose at +Y).

Each ``*_model(bank=0.0, palette=None)`` returns ``(scene, materials)`` ready for
``sdf.render``. ``palette`` optionally overrides material colours (used by the palette sheets).
"""
import numpy as np

from .sdf import (Material, mirror_x, panel_lines, rotate_y, rotate_z, sd_box, sd_capsule,
                  sd_cylinder_x, sd_cylinder_y, sd_cylinder_z, sd_ellipsoid, sd_fin, sd_plate,
                  sd_sphere, smoothstep, subtract, union)

# UTC / CDF default colours (see design/art-direction palette options)
UTC = {
    "hull": (0.80, 0.82, 0.86),
    "hull_dark": (0.30, 0.33, 0.38),
    "glass": (0.10, 0.18, 0.32),
    "accent": (0.16, 0.40, 0.90),
    "accent2": (0.95, 0.52, 0.12),
    "engine": (0.55, 0.80, 1.00),
    "gun": (0.22, 0.23, 0.26),
}

HULL, DARK, GLASS, ACCENT, ACCENT2, ENGINE, GUN = range(7)


def _player_materials(pal):
    c = dict(UTC, **(pal or {}))
    eng = np.array(c["engine"])
    return [
        Material(c["hull"], metal=0.35, shininess=60, spec=0.55, pattern=panel_lines(6.0)),
        Material(c["hull_dark"], metal=0.5, shininess=30, spec=0.35),
        Material(c["glass"], metal=0.85, shininess=120, spec=1.2),
        Material(c["accent"], metal=0.3, shininess=50, spec=0.5),
        Material(c["accent2"], metal=0.3, shininess=50, spec=0.5),
        Material((0.1, 0.1, 0.12), emission=tuple(eng * 1.6)),
        Material(c["gun"], metal=0.7, shininess=80, spec=0.8),
    ]


def _decal(m, cond, mat):
    return np.where(cond & (m == HULL), mat, m)


# --------------------------------------------------------------------------- player ships

def ship_a_model(bank=0.0, palette=None):
    """Variant A 'Forward-swept': long fuselage, forward-swept wings, canards, twin engines."""
    wing = [(0.10, -0.20), (0.90, 0.10), (0.97, 0.02), (0.93, -0.10), (0.12, -0.66)]
    canard = [(0.08, 0.50), (0.36, 0.40), (0.38, 0.33), (0.08, 0.30)]
    fin = [(-0.50, 0.05), (-0.84, 0.30), (-0.95, 0.30), (-0.93, 0.05)]

    def scene(p):
        p = rotate_y(p, bank)
        q = mirror_x(p)
        body = union(
            (sd_ellipsoid(p, (0, -0.05, 0), (0.15, 0.95, 0.12)), HULL),
            (sd_capsule(p, (0, 0.3, 0.0), (0, 1.0, -0.01), 0.12, 0.015), HULL),
            (sd_capsule(q, (0.2, -0.25, -0.01), (0.2, -0.9, -0.01), 0.085, 0.1), DARK),
            k=0.09,
        )
        d, m = union(
            body,
            (sd_ellipsoid(p, (0, 0.40, 0.085), (0.065, 0.2, 0.07)), GLASS),
            (sd_plate(q, wing, -0.01, 0.035, 0.01,
                      taper=lambda x, y: 1.0 - 0.6 * np.clip(x, 0, 1)), HULL),
            (sd_plate(q, canard, 0.02, 0.018, 0.005), HULL),
            (sd_fin(q, fin, 0.2, 0.012, 0.004), ACCENT),
            (sd_capsule(q, (0.95, -0.14, -0.01), (0.95, 0.32, -0.01), 0.032, 0.018), GUN),
            (sd_cylinder_y(q, (0.2, -0.93, -0.01), 0.07, 0.03), ENGINE),
            (sd_box(q, (0.17, 0.02, 0.05), (0.04, 0.12, 0.03), 0.01), DARK),
            k=0.015,
        )
        m = _decal(m, (q[:, 0] > 0.72) & (q[:, 1] > -0.2), ACCENT)
        m = _decal(m, (np.abs(q[:, 1] - 0.7) < 0.04) & (q[:, 2] > 0.02), ACCENT2)
        m = _decal(m, (q[:, 0] < 0.035) & (q[:, 1] < 0.2) & (q[:, 1] > -0.75), ACCENT)
        return d, m

    return scene, _player_materials(palette)


def ship_b_model(bank=0.0, palette=None):
    """Variant B 'Twin-boom': central cockpit pod, two engine booms, straight wing, tailplane."""
    wing = [(0.0, 0.02), (0.98, -0.02), (1.0, -0.12), (0.96, -0.24), (0.0, -0.36)]
    tail = [(0.0, -0.72), (0.52, -0.72), (0.52, -0.84), (0.0, -0.86)]
    fin = [(-0.6, 0.04), (-0.82, 0.30), (-0.94, 0.30), (-0.92, 0.04)]

    def scene(p):
        p = rotate_y(p, bank)
        q = mirror_x(p)
        d, m = union(
            (sd_ellipsoid(p, (0, 0.22, 0.02), (0.17, 0.62, 0.14)), HULL),
            (sd_capsule(q, (0.44, 0.62, 0), (0.44, -0.92, 0), 0.07, 0.1), HULL),
            (sd_plate(q, wing, 0.0, 0.04, 0.01,
                      taper=lambda x, y: 1.0 - 0.5 * np.clip(x, 0, 1)), HULL),
            k=0.07,
        )
        d, m = union(
            (d, m),
            (sd_ellipsoid(p, (0, 0.45, 0.10), (0.075, 0.2, 0.075)), GLASS),
            (sd_plate(q, tail, 0.02, 0.02, 0.006), HULL),
            (sd_fin(q, fin, 0.44, 0.014, 0.004), ACCENT),
            (sd_cylinder_y(q, (0.44, -0.95, 0.0), 0.075, 0.035), ENGINE),
            (sd_cylinder_y(p, (0, -0.38, 0.0), 0.06, 0.03), ENGINE),
            (sd_capsule(q, (0.44, 0.6, 0.0), (0.44, 0.82, 0.0), 0.022, 0.015), GUN),
            (sd_capsule(q, (0.75, -0.02, -0.03), (0.75, 0.2, -0.03), 0.028, 0.02), GUN),
            (sd_box(q, (0.44, 0.18, 0.08), (0.035, 0.1, 0.02), 0.01), DARK),
            k=0.012,
        )
        m = _decal(m, (np.abs(q[:, 0] - 0.44) < 0.1) & (q[:, 1] > 0.38) & (q[:, 1] < 0.5),
                   ACCENT2)
        m = _decal(m, (q[:, 0] > 0.8) & (q[:, 1] < 0.0), ACCENT)
        m = _decal(m, (q[:, 1] < -0.7) & (q[:, 0] < 0.4), ACCENT)
        return d, m

    return scene, _player_materials(palette)


def ship_c_model(bank=0.0, palette=None):
    """Variant C 'Blended wing': manta-like lifting body with canards and buried engines."""
    body = [(0.0, 0.96), (0.14, 0.62), (0.34, 0.12), (0.62, -0.30), (0.95, -0.56),
            (0.93, -0.70), (0.60, -0.62), (0.40, -0.80), (0.16, -0.66), (0.0, -0.78)]
    canard = [(0.10, 0.62), (0.40, 0.56), (0.42, 0.49), (0.12, 0.44)]
    fin = [(-0.35, 0.04), (-0.60, 0.26), (-0.70, 0.26), (-0.66, 0.04)]

    def scene(p):
        p = rotate_y(p, bank)
        q = mirror_x(p)
        d, m = union(
            (sd_plate(q, body, 0.0, 0.07, 0.02,
                      taper=lambda x, y: np.clip(1.0 - 0.85 * np.clip(x, 0, 1), 0.2, 1)), HULL),
            (sd_ellipsoid(p, (0, 0.08, 0.02), (0.2, 0.86, 0.13)), HULL),
            k=0.16,
        )
        d, m = union(
            (d, m),
            (sd_ellipsoid(p, (0, 0.42, 0.11), (0.07, 0.2, 0.075)), GLASS),
            (sd_plate(q, canard, 0.02, 0.018, 0.005), HULL),
            (sd_fin(rotate_y(q - np.array([0.3, 0, 0]), -0.35) + np.array([0.3, 0, 0]),
                    fin, 0.3, 0.013, 0.004), ACCENT),
            (sd_cylinder_y(q, (0.15, -0.70, 0.0), 0.075, 0.035), ENGINE),
            (sd_capsule(q, (0.5, -0.42, -0.02), (0.5, -0.02, -0.02), 0.026, 0.018), GUN),
            k=0.012,
        )
        edge = (q[:, 0] > 0.3) & (q[:, 1] > -0.02 - (q[:, 0] - 0.3) * 1.3 - 0.12) & \
               (q[:, 1] < 0.2 - (q[:, 0] - 0.3) * 1.5)
        m = _decal(m, edge & (q[:, 0] > 0.55), ACCENT)
        m = _decal(m, (q[:, 0] > 0.82), ACCENT2)
        m = _decal(m, (np.abs(q[:, 1] - 0.75) < 0.035), ACCENT2)
        return d, m

    return scene, _player_materials(palette)


PLAYER_SHIPS = {
    "a": ("Forward-swept", ship_a_model),
    "b": ("Twin-boom", ship_b_model),
    "c": ("Blended wing", ship_c_model),
}

# --------------------------------------------------------------------------- Vrell

VRELL = {
    "chitin": (0.42, 0.20, 0.46),
    "chitin2": (0.16, 0.44, 0.42),
    "glow": (0.25, 1.0, 0.75),
    "glow2": (1.0, 0.25, 0.6),
}


def _spots(scale, thresh):
    def f(p, n):
        v = (np.sin(p[:, 0] * scale * 1.7 + 1.3) * np.sin(p[:, 1] * scale + 0.4)
             * np.sin((p[:, 0] + p[:, 1]) * scale * 0.6))
        return smoothstep(thresh, thresh + 0.15, v)
    return f


def _ridges(scale):
    def f(p, n):
        return 0.75 + 0.25 * np.cos(p[:, 1] * scale) ** 2
    return f


def _vrell_materials(pal=None):
    c = dict(VRELL, **(pal or {}))
    return [
        Material(c["chitin"], metal=0.25, shininess=90, spec=0.9, pattern=_ridges(22)),
        Material(c["chitin2"], metal=0.2, shininess=70, spec=0.7, pattern=_ridges(30)),
        Material((0.05, 0.05, 0.05), emission=tuple(np.array(c["glow"]) * 1.4)),
        Material((0.05, 0.05, 0.05), emission=tuple(np.array(c["glow2"]) * 1.4)),
        Material((0.3, 0.1, 0.25), metal=0.1, shininess=40, spec=0.6,
                 emission=tuple(np.array(c["glow"]) * 1.2), emission_pattern=_spots(9, 0.25)),
    ]


CH, CH2, GLOW, GLOW2, SPOTTED = range(5)


def vrell_dart_model(bank=0.0, palette=None):
    """Small Vrell 'dart' fighter: scythe wings around a glowing core. Faces down (-Y)."""
    def scene(p):
        p = rotate_z(rotate_y(p, bank), np.pi)  # nose towards the bottom of the screen
        q = mirror_x(p)
        d, m = union(
            (sd_ellipsoid(p, (0, 0.1, 0), (0.22, 0.7, 0.16)), CH),
            (sd_capsule(q, (0.12, 0.25, 0), (0.75, -0.25, -0.02), 0.12, 0.02), CH2),
            (sd_capsule(q, (0.15, -0.05, 0), (0.62, -0.70, -0.02), 0.09, 0.015), CH),
            (sd_capsule(q, (0.06, 0.5, 0.02), (0.18, 0.95, 0.0), 0.06, 0.01), CH),
            k=0.12,
        )
        d, m = union(
            (d, m),
            (sd_sphere(p, (0, 0.12, 0.12), 0.1), GLOW),
            (sd_ellipsoid(q, (0.36, 0.1, 0.05), (0.08, 0.05, 0.03)), GLOW2),
            k=0.02,
        )
        return d, m
    return scene, _vrell_materials(palette)


def vrell_brood_model(bank=0.0, palette=None):
    """Larger Vrell 'brood' gunship: segmented body, four pincer limbs, spotted carapace."""
    def scene(p):
        p = rotate_z(rotate_y(p, bank), np.pi)
        q = mirror_x(p)
        segs = [(sd_ellipsoid(p, (0, y, 0), (0.42 - abs(y) * 0.25, 0.26, 0.2)), SPOTTED)
                for y in (0.55, 0.2, -0.15, -0.5)]
        d, m = union(*segs, k=0.08)
        d, m = union(
            (d, m),
            (sd_capsule(q, (0.3, 0.45, 0), (0.85, 0.9, -0.05), 0.09, 0.03), CH),
            (sd_capsule(q, (0.85, 0.9, -0.05), (0.7, 1.15, -0.05), 0.03, 0.01), CH2),
            (sd_capsule(q, (0.3, -0.3, 0), (0.9, -0.55, -0.05), 0.1, 0.03), CH),
            (sd_capsule(q, (0.9, -0.55, -0.05), (0.95, -0.9, -0.05), 0.03, 0.01), CH2),
            (sd_capsule(p, (0, -0.6, 0.05), (0, -0.95, 0.0), 0.14, 0.04), CH2),
            k=0.07,
        )
        d, m = union(
            (d, m),
            (sd_sphere(q, (0.13, 0.72, 0.12), 0.06), GLOW2),
            (sd_sphere(p, (0, 0.0, 0.2), 0.08), GLOW),
            k=0.02,
        )
        return d, m
    return scene, _vrell_materials(palette)


def vrell_turret_model(bank=0.0, palette=None):
    """Ground-layer Vrell spore turret: a pod of chitin petals around a glowing eye."""
    def scene(p):
        petals = [(sd_ellipsoid(rotate_z(p, a), (0, 0.45, 0.05), (0.2, 0.42, 0.18)), CH)
                  for a in np.linspace(0, 2 * np.pi, 6, endpoint=False)]
        d, m = union(*petals, k=0.1)
        d, m = union(
            (d, m),
            (sd_ellipsoid(p, (0, 0, 0.1), (0.4, 0.4, 0.25)), CH2),
            (sd_capsule(p, (0, 0, 0.3), (0, -0.55, 0.3), 0.09, 0.05), CH),
            k=0.06,
        )
        return union((d, m), (sd_sphere(p, (0, 0.02, 0.36), 0.12), GLOW2), k=0.02)
    return scene, _vrell_materials(palette)


# --------------------------------------------------------------------------- station

STATION_MATS = [
    Material((0.72, 0.73, 0.76), metal=0.4, shininess=50, spec=0.5, pattern=panel_lines(4.0)),
    Material((0.35, 0.36, 0.40), metal=0.6, shininess=40, spec=0.5),
    Material((0.08, 0.12, 0.32), metal=0.8, shininess=90, spec=0.9,
             pattern=lambda p, n: np.where(((p[:, 0] * 8) % 1 < 0.08) |
                                           ((p[:, 1] * 8) % 1 < 0.08), 2.8, 1.0)),
    Material((0.85, 0.45, 0.12), metal=0.3, shininess=40, spec=0.4),
    Material((0.1, 0.1, 0.1), emission=(1.6, 0.25, 0.2)),
    Material((0.1, 0.1, 0.1), emission=(1.3, 1.1, 0.7)),
]


def station_segment_model(variant=0):
    """Orbital station segment, 3.2 units long so segments join into a continuous spine.

    variant 0: habitat drum, two solar wings, radiators, gun emplacement
    variant 1: docking module with radiator fins and a pair of cargo pods
    """
    if variant == 1:
        return _station_dock_model()

    def scene(p):
        q = mirror_x(p)
        truss = [(sd_box(p, (0, y, 0), (0.12, 0.04, 0.04), 0.01), 1)
                 for y in np.linspace(-1.5, 1.5, 16)]
        d, m = union(
            (sd_box(q, (0.1, 0, 0), (0.025, 1.6, 0.025), 0.01), 1),
            *truss,
        )
        d, m = union(
            (d, m),
            (sd_cylinder_x(p, (0, 0.35, 0.02), 0.28, 0.5), 0),
            (sd_cylinder_x(q, (0.52, 0.35, 0.02), 0.2, 0.05), 1),
            (sd_box(p, (0, -0.55, 0.05), (0.3, 0.22, 0.12), 0.05), 0),
            k=0.02,
        )
        d, m = union(
            (d, m),
            (sd_box(q, (1.05, 0.35, -0.05), (0.48, 0.24, 0.01), 0.0), 2),
            (sd_box(q, (0.56, 0.35, -0.05), (0.02, 0.02, 0.02), 0.0), 1),
            (sd_box(q, (0.55, -0.8, -0.04), (0.22, 0.09, 0.008), 0.0), 0),
            (sd_cylinder_z(p, (0, -0.55, 0.2), 0.12, 0.05), 3),
            (sd_capsule(p, (0, -0.55, 0.25), (0, -0.25, 0.27), 0.03, 0.025), 1),
            (sd_sphere(q, (1.52, 0.58, 0), 0.03), 4),
            (sd_sphere(p, (0, 0.35, 0.31), 0.03), 5),
        )
        return d, m
    return scene, STATION_MATS


def _station_dock_model():
    fins = [(-0.9, 0.0), (-0.7, 0.0)]

    def scene(p):
        q = mirror_x(p)
        truss = [(sd_box(p, (0, y, 0), (0.12, 0.04, 0.04), 0.01), 1)
                 for y in np.linspace(-1.5, 1.5, 16)]
        d, m = union(
            (sd_box(q, (0.1, 0, 0), (0.025, 1.6, 0.025), 0.01), 1),
            *truss,
        )
        d, m = union(
            (d, m),
            (sd_cylinder_y(p, (0, 0.1, 0.03), 0.3, 0.55), 0),
            (sd_sphere(p, (0, 0.65, 0.03), 0.3), 0),
            (sd_cylinder_x(p, (0, 0.2, 0.03), 0.12, 0.62), 1),
            (sd_cylinder_z(q, (0.62, 0.2, 0.03), 0.17, 0.12), 0),
            k=0.03,
        )
        d, m = union(
            (d, m),
            (sd_box(q, (0.62, -0.75, -0.04), (0.42, 0.06, 0.008), 0.0), 0),
            (sd_box(q, (0.62, -0.95, -0.04), (0.42, 0.06, 0.008), 0.0), 0),
            (sd_box(q, (0.62, -1.15, -0.04), (0.42, 0.06, 0.008), 0.0), 0),
            (sd_box(q, (0.25, -0.95, -0.04), (0.02, 0.28, 0.02), 0.0), 1),
            (sd_capsule(q, (0.62, 0.9, 0.0), (0.62, 1.3, 0.0), 0.13, 0.13), 3),
            (sd_sphere(q, (0.62, 0.2, 0.16), 0.04), 5),
            (sd_sphere(q, (1.05, -0.95, -0.03), 0.03), 4),
        )
        return d, m
    return scene, STATION_MATS


def debris_model(angle=0.0, tilt=0.6):
    """A tumbling torn hull panel (high-air layer debris)."""
    shape = [(-0.5, -0.3), (0.45, -0.4), (0.55, 0.1), (0.1, 0.45), (-0.4, 0.3)]

    def scene(p):
        p = rotate_z(p, angle)
        c, s = np.cos(tilt), np.sin(tilt)
        q = p.copy()
        q[:, 1] = c * p[:, 1] + s * p[:, 2]
        q[:, 2] = -s * p[:, 1] + c * p[:, 2]
        d, m = union(
            (sd_plate(q, shape, 0.0, 0.04, 0.01), 0),
            (sd_box(q, (0.0, 0.0, 0.05), (0.35, 0.03, 0.02), 0.01), 1),
            (sd_box(q, (-0.1, -0.1, -0.05), (0.03, 0.3, 0.02), 0.01), 1),
        )
        return d, m
    return scene, STATION_MATS
