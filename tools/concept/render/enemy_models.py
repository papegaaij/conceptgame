"""SDF models for concept round 03: the Act 1 Vrell enemies (two design languages for the key
ones), three Ascendancy enemies for faction contrast, and the Brood Carrier boss.

World units as in render.models: a model fits in about +-1.1 units (rendered with extent 2.3).
Enemies are modelled nose-up (+Y) and turned to face the player (down) by ``_face_down``.

Every ``*_model(anim=0.0, glow=1.0)`` returns ``(scene, materials)`` for ``sdf.render``:

anim   animation phase in -1..1 (wing beat, sac pulse, turret aim angle in radians for turrets)
glow   multiplier for the bioluminescent / light emission (glow pulse frames)

Two Vrell design languages are proposed (see design/enemies, concept round 03):

A "Sleek chitin"      smooth, elongated, glossy violet chitin with thin glowing teal seams
B "Armoured brood"    bulky segmented carapace plates, claws and spikes, glow only in the
                      gaps between plates and in eye clusters
"""
import numpy as np

from .sdf import (Material, mirror_x, rotate_x, rotate_y, rotate_z, sd_box, sd_capsule,
                  sd_cylinder_y, sd_cylinder_z, sd_ellipsoid, sd_fin, sd_plate, sd_sphere,
                  smoothstep, subtract, union)


def hx(code):
    code = code.lstrip("#")
    return tuple(int(code[i:i + 2], 16) / 255 for i in (0, 2, 4))


# Palette B ramps (tools/concept/render/palette.py), as floats
CHITIN = [hx(c) for c in "1a0020 40004a 6e0a78 a020a8 d050d0 ff9aff".split()]
VGLOW = [hx(c) for c in "003a20 00a060 00ff9a 600080 c000ff f0a0ff".split()]
ASC = [hx(c) for c in "000000 100818 201430 34244a 4c3a68 6c5890".split()]
GOLD = [hx(c) for c in "402000 804000 c07000 ffa800 ffd84a ffffa0".split()]
TEAL = np.array(VGLOW[2])
PINK = np.array(hx("ff40ff"))
RED = np.array((1.0, 0.16, 0.12))
THRUST = np.array(hx("ff7a2a"))


def _face_down(p):
    return rotate_z(p, np.pi)


def _hinge(q, pivot, angle, axis="y"):
    """Rotate points q about an axis through ``pivot`` (for wing beats / limbs)."""
    pv = np.asarray(pivot, dtype=np.float64)
    r = rotate_y if axis == "y" else (rotate_z if axis == "z" else rotate_x)
    return r(q - pv, angle) + pv


# --------------------------------------------------------------------------- patterns


def _ridges(scale, depth=0.25, axis=1):
    def f(p, n):
        return (1 - depth) + depth * np.cos(p[:, axis] * scale) ** 2
    return f


def _plates(scale, gap=0.08, depth=0.55):
    """Segment bands across the body (armoured-plate language): dark grooves between plates."""
    def f(p, n):
        u = np.abs((p[:, 1] * scale) % 1.0 - 0.5)
        return np.where(u > 0.5 - gap, depth, 1.0) * (0.85 + 0.15 * n[:, 2])
    return f


def _seam(width=0.035, freq=0.0, x0=0.0):
    """Glowing seam line along the spine (|x| small), optionally with cross seams."""
    def f(p, n):
        s = smoothstep(width, width * 0.3, np.abs(np.abs(p[:, 0]) - x0))
        if freq:
            c = smoothstep(0.06, 0.02, np.abs(np.sin(p[:, 1] * freq)))
            s = np.maximum(s, c * 0.8)
        return s
    return f


def _plate_gaps(scale, gap=0.07):
    """Emission only in the grooves between plates (style B glow)."""
    def f(p, n):
        u = np.abs((p[:, 1] * scale) % 1.0 - 0.5)
        return smoothstep(0.5 - gap, 0.5 - gap * 0.2, u)
    return f


def _veins(scale, seed=0.0, width=0.1):
    def f(p, n):
        v = (np.sin(p[:, 0] * scale * 1.3 + seed) + np.sin(p[:, 1] * scale * 0.9 + 2 * seed)
             + np.sin((p[:, 0] - p[:, 1]) * scale * 0.7 + 1.0))
        return smoothstep(width, 0.0, np.abs(v)) * 1.0
    return f


def _hex(scale, width=0.08):
    """Hex-ish panel grid for Ascendancy hulls (multiplier)."""
    def f(p, n):
        x, y = p[:, 0] * scale, p[:, 1] * scale
        a = np.abs(((x) % 1.0) - 0.5)
        b = np.abs(((0.5 * x + 0.866 * y) % 1.0) - 0.5)
        c = np.abs(((-0.5 * x + 0.866 * y) % 1.0) - 0.5)
        line = np.maximum(np.maximum(a, b), c)
        return np.where(line > 0.5 - width, 0.62, 1.0)
    return f


# --------------------------------------------------------------------------- materials

# Vrell material slots
V_BODY, V_DARK, V_SEAM, V_GLOW, V_EYE, V_BONE, V_SAC = range(7)


def vrell_mats(style="a", glow=1.0):
    g = glow
    if style == "a":
        return [
            Material(CHITIN[3], metal=0.3, shininess=110, spec=1.1, pattern=_ridges(18, 0.2)),
            Material(CHITIN[2], metal=0.35, shininess=80, spec=0.8),
            Material(CHITIN[3], metal=0.3, shininess=110, spec=1.1, pattern=_ridges(18, 0.15),
                     emission=tuple(TEAL * 1.6 * g), emission_pattern=_seam(0.055)),
            Material((0.04, 0.04, 0.04), emission=tuple(TEAL * 1.35 * g)),
            Material((0.04, 0.04, 0.04), emission=tuple(PINK * 1.4 * g)),
            Material(CHITIN[4], metal=0.2, shininess=70, spec=0.7),
            Material(CHITIN[2], metal=0.1, shininess=60, spec=0.9,
                     emission=tuple(TEAL * 1.1 * g), emission_pattern=_veins(9, 0.3, 0.35)),
        ]
    return [
        Material(CHITIN[3], metal=0.35, shininess=70, spec=0.9, pattern=_plates(3.0, 0.07)),
        Material(CHITIN[2], metal=0.4, shininess=60, spec=0.7, pattern=_ridges(30, 0.3)),
        Material(CHITIN[3], metal=0.35, shininess=70, spec=0.9, pattern=_plates(3.0, 0.07),
                 emission=tuple(TEAL * 1.6 * g), emission_pattern=_plate_gaps(3.0, 0.07)),
        Material((0.04, 0.04, 0.04), emission=tuple(TEAL * 1.35 * g)),
        Material((0.04, 0.04, 0.04), emission=tuple(PINK * 1.4 * g)),
        Material(CHITIN[5], metal=0.15, shininess=50, spec=0.6),
        Material(CHITIN[3], metal=0.1, shininess=60, spec=0.9,
                 emission=tuple(TEAL * 1.1 * g), emission_pattern=_veins(9, 0.3, 0.35)),
    ]


# Ascendancy material slots
A_HULL, A_FACET, A_GOLD, A_RED, A_ENGINE, A_GLASS, A_GUN = range(7)


def asc_mats(glow=1.0):
    g = glow
    return [
        Material(ASC[4], metal=0.75, shininess=90, spec=1.0, pattern=_hex(9)),
        Material(ASC[3], metal=0.85, shininess=140, spec=1.3),
        Material(GOLD[3], metal=0.9, shininess=120, spec=1.2),
        Material((0.05, 0.02, 0.02), emission=tuple(RED * 1.6 * g)),
        Material((0.08, 0.04, 0.02), emission=tuple(THRUST * 1.6 * g)),
        Material(hx("300818"), metal=0.9, shininess=150, spec=1.4),
        Material(ASC[4], metal=0.8, shininess=100, spec=1.0),
    ]


# --------------------------------------------------------------------------- Vrell: style A

def skitter_a(anim=0.0, glow=1.0):
    """Skitter, style A: needle-thin teardrop with two scythe wings, glowing spine seam."""
    wing = [(0.10, 0.20), (0.55, -0.05), (0.98, -0.62), (0.80, -0.60), (0.12, -0.25)]
    beat = 0.45 * anim

    def scene(p):
        p = _face_down(p)
        q = mirror_x(p)
        w = _hinge(q, (0.1, 0.0, 0.0), beat)
        d, m = union(
            (sd_ellipsoid(p, (0, 0.05, 0), (0.2, 0.78, 0.17)), V_SEAM),
            (sd_capsule(p, (0, 0.55, 0.02), (0, 1.0, 0.0), 0.09, 0.012), V_BODY),
            (sd_plate(w, wing, 0.0, 0.05, 0.02,
                      taper=lambda x, y: np.clip(1.1 - 0.9 * x, 0.25, 1)), V_BODY),
            k=0.1,
        )
        return union(
            (d, m),
            (sd_capsule(q, (0.08, -0.55, 0.0), (0.2, -0.95, -0.02), 0.05, 0.01), V_DARK),
            (sd_sphere(p, (0, 0.62, 0.1), 0.085), V_EYE),
            (sd_ellipsoid(q, (0.4, -0.1, 0.03), (0.1, 0.05, 0.03)), V_GLOW),
            k=0.03,
        )
    return scene, vrell_mats("a", glow)


def needler_a(anim=0.0, glow=1.0):
    """Needler, style A: slim body with a long thorn proboscis (the gun) and four swept fins."""
    fin1 = [(0.12, 0.25), (0.78, 0.02), (0.92, -0.18), (0.14, -0.10)]
    fin2 = [(0.12, -0.25), (0.70, -0.55), (0.72, -0.80), (0.10, -0.55)]
    beat = 0.35 * anim

    def scene(p):
        p = _face_down(p)
        q = mirror_x(p)
        w1 = _hinge(q, (0.12, 0.0, 0.0), beat)
        w2 = _hinge(q, (0.12, 0.0, 0.0), -beat * 0.7)
        d, m = union(
            (sd_ellipsoid(p, (0, 0.0, 0), (0.26, 0.62, 0.2)), V_SEAM),
            (sd_ellipsoid(p, (0, -0.62, 0), (0.16, 0.3, 0.12)), V_BODY),
            (sd_plate(w1, fin1, 0.0, 0.045, 0.015,
                      taper=lambda x, y: np.clip(1.1 - x, 0.3, 1)), V_BODY),
            (sd_plate(w2, fin2, -0.02, 0.04, 0.015,
                      taper=lambda x, y: np.clip(1.1 - x, 0.3, 1)), V_DARK),
            k=0.09,
        )
        return union(
            (d, m),
            (sd_capsule(p, (0, 0.45, 0.02), (0, 1.05, 0.0), 0.07, 0.012), V_BONE),
            (sd_sphere(p, (0, 1.0, 0.0), 0.05), V_EYE),
            (sd_sphere(q, (0.1, 0.35, 0.13), 0.055), V_GLOW),
            (sd_ellipsoid(p, (0, -0.1, 0.15), (0.08, 0.2, 0.06)), V_EYE),
            k=0.03,
        )
    return scene, vrell_mats("a", glow)


def stinger_a(anim=0.0, glow=1.0):
    """Stinger: wasp-like diver, long narrow wings swept back, forward stinger with glow."""
    wing = [(0.10, 0.35), (0.62, 0.10), (1.0, -0.30), (0.86, -0.42), (0.10, 0.02)]
    wing2 = [(0.10, 0.0), (0.55, -0.35), (0.62, -0.55), (0.10, -0.25)]
    beat = 0.5 * anim

    def scene(p):
        p = _face_down(p)
        q = mirror_x(p)
        w = _hinge(q, (0.1, 0.1, 0.0), beat)
        w2 = _hinge(q, (0.1, 0.0, 0.0), beat * 0.8)
        segs = [(sd_ellipsoid(p, (0, y, 0), (r, 0.2, r * 0.8)), V_SEAM if i % 2 else V_BODY)
                for i, (y, r) in enumerate(((0.35, 0.16), (0.02, 0.22), (-0.32, 0.18),
                                            (-0.6, 0.13)))]
        d, m = union(*segs, k=0.1)
        d, m = union(
            (d, m),
            (sd_plate(w, wing, 0.05, 0.03, 0.01,
                      taper=lambda x, y: np.clip(1.2 - x, 0.3, 1)), V_BODY),
            (sd_plate(w2, wing2, 0.02, 0.028, 0.01), V_DARK),
            k=0.06,
        )
        return union(
            (d, m),
            (sd_capsule(p, (0, 0.5, 0.0), (0, 1.02, 0.0), 0.1, 0.01), V_BONE),
            (sd_sphere(p, (0, 0.98, 0.0), 0.045), V_EYE),
            (sd_sphere(q, (0.09, 0.5, 0.1), 0.06), V_GLOW),
            (sd_capsule(p, (0, -0.7, 0.0), (0, -0.95, 0.0), 0.06, 0.02), V_DARK),
            k=0.03,
        )
    return scene, vrell_mats("a", glow)


def spore_bomber_a(anim=0.0, glow=1.0):
    """Spore Bomber: elongated gas-bag (blimp) with glowing veins, a small chitin head,
    tail fins and rows of spore bulbs hanging along its flanks."""
    fin = [(0.22, -0.55), (0.62, -0.78), (0.66, -0.98), (0.2, -0.82)]
    pulse = 1.0 + 0.06 * anim

    def scene(p):
        p = _face_down(p)
        q = mirror_x(p)
        bulbs = [(sd_sphere(q, (0.42 * pulse, y, -0.1), 0.1), V_EYE)
                 for y in (0.42, 0.14, -0.14, -0.42)]
        d, m = union(
            (sd_ellipsoid(p, (0, -0.02, 0.0), (0.46 * pulse, 0.82, 0.38 * pulse)), V_SAC),
            (sd_ellipsoid(p, (0, 0.78, 0.02), (0.22, 0.22, 0.18)), V_BODY),
            (sd_plate(q, fin, 0.0, 0.05, 0.02,
                      taper=lambda x, y: np.clip(1.4 - x, 0.3, 1)), V_BODY),
            k=0.1,
        )
        d, m = union((d, m), *bulbs, k=0.05)
        return union(
            (d, m),
            (sd_ellipsoid(p, (0, 0.0, 0.36 * pulse), (0.08, 0.6, 0.06)), V_SEAM),
            (sd_sphere(q, (0.1, 0.9, 0.12), 0.05), V_GLOW),
            k=0.04,
        )
    return scene, vrell_mats("a", glow)


def brood_pod_a(anim=0.0, glow=1.0):
    """Brood Pod: a pulsing egg sac wrapped in chitin ribs; the burst seam glows brighter
    as it matures. Small cilia around the rim."""
    pulse = 1.0 + 0.07 * anim

    def scene(p):
        p = _face_down(p)
        ribs = [(sd_capsule(rotate_z(p, a), (0.0, -0.1, 0.52), (0.0, 0.78 * pulse, 0.05),
                            0.07, 0.03), V_BODY)
                for a in np.linspace(0, 2 * np.pi, 6, endpoint=False)]
        cilia = [(sd_capsule(rotate_z(p, a), (0.0, 0.7 * pulse, 0.0), (0.0, 0.98, -0.05),
                             0.035, 0.01), V_DARK)
                 for a in np.linspace(0.26, 2 * np.pi + 0.26, 12, endpoint=False)]
        d, m = union(
            (sd_ellipsoid(p, (0, 0, 0.0), (0.72 * pulse, 0.72 * pulse, 0.5 * pulse)), V_SAC),
            *ribs, k=0.06,
        )
        d, m = union((d, m), *cilia, k=0.04)
        return union(
            (d, m),
            (sd_sphere(p, (0, 0, 0.5 * pulse), 0.13), V_EYE),
            k=0.05,
        )
    return scene, vrell_mats("a", glow)


def mantis_a(anim=0.0, glow=1.0):
    """Mantis: long thorax, two raptorial arms folded forward, laser emitter in the head,
    translucent wings. Holds position at the screen side and sweeps."""
    wing = [(0.1, 0.05), (0.62, -0.1), (0.9, -0.6), (0.72, -0.7), (0.08, -0.3)]
    arm_open = 0.25 * anim

    def scene(p):
        p = _face_down(p)
        q = mirror_x(p)
        a = _hinge(q, (0.14, 0.35, 0.0), arm_open, axis="z")
        d, m = union(
            (sd_ellipsoid(p, (0, -0.3, 0), (0.2, 0.55, 0.16)), V_SEAM),
            (sd_capsule(p, (0, 0.1, 0.02), (0, 0.62, 0.02), 0.12, 0.09), V_BODY),
            (sd_ellipsoid(p, (0, 0.72, 0.03), (0.2, 0.14, 0.12)), V_BODY),
            k=0.08,
        )
        d, m = union(
            (d, m),
            (sd_capsule(a, (0.14, 0.35, 0.0), (0.46, 0.62, 0.02), 0.07, 0.05), V_BODY),
            (sd_capsule(a, (0.46, 0.62, 0.02), (0.3, 1.02, 0.0), 0.05, 0.02), V_BONE),
            (sd_plate(q, wing, -0.04, 0.025, 0.01,
                      taper=lambda x, y: np.clip(1.2 - x, 0.3, 1)), V_DARK),
            (sd_capsule(q, (0.1, -0.7, 0.0), (0.26, -1.0, -0.02), 0.04, 0.01), V_DARK),
            k=0.05,
        )
        return union(
            (d, m),
            (sd_sphere(q, (0.12, 0.78, 0.1), 0.06), V_GLOW),
            (sd_sphere(p, (0, 0.88, 0.02), 0.07), V_EYE),
            k=0.02,
        )
    return scene, vrell_mats("a", glow)


def spine_turret_a(anim=0.0, glow=1.0):
    """Spine Turret, style A: smooth bulb with petal collar and a single long thorn barrel.
    ``anim`` is the aim angle in radians (0 = at the player, i.e. down)."""
    def scene(p):
        roots = [(sd_capsule(rotate_z(p, a), (0, 0.3, -0.05), (0.0, 1.0, -0.2), 0.12, 0.03),
                  V_DARK) for a in np.linspace(0.4, 2 * np.pi + 0.4, 5, endpoint=False)]
        petals = [(sd_ellipsoid(rotate_z(p, a), (0, 0.42, 0.05), (0.16, 0.32, 0.14)), V_BODY)
                  for a in np.linspace(0, 2 * np.pi, 7, endpoint=False)]
        d, m = union(*roots, k=0.08)
        d, m = union((d, m), *petals, k=0.1)
        r = rotate_z(_face_down(p), -anim)
        d, m = union(
            (d, m),
            (sd_sphere(p, (0, 0, 0.2), 0.36), V_SEAM),
            k=0.08,
        )
        return union(
            (d, m),
            (sd_capsule(r, (0, 0.1, 0.4), (0, 0.95, 0.42), 0.1, 0.025), V_BONE),
            (sd_sphere(r, (0, 0.95, 0.42), 0.05), V_EYE),
            (sd_sphere(p, (0, 0, 0.52), 0.11), V_EYE),
            k=0.04,
        )
    return scene, vrell_mats("a", glow)


def polyp_mortar_a(anim=0.0, glow=1.0):
    """Polyp Mortar: squat tube with a wide mouth full of glowing acid and a ring of
    tentacles. ``anim`` = mouth contraction before a shot (-1..1)."""
    mouth = 0.34 + 0.05 * anim

    def scene(p):
        tent = [(sd_capsule(rotate_z(p, a), (0, 0.45, 0.3), (0.0, 0.92, 0.0), 0.08, 0.02),
                 V_BODY) for a in np.linspace(0.2, 2 * np.pi + 0.2, 9, endpoint=False)]
        base = union(
            (sd_cylinder_z(p, (0, 0, 0.15), 0.55, 0.2), V_SEAM),
            (sd_ellipsoid(p, (0, 0, 0.0), (0.8, 0.8, 0.25)), V_DARK),
            *tent, k=0.12,
        )
        d, m = subtract(base, sd_cylinder_z(p, (0, 0, 0.45), mouth, 0.3))
        return union(
            (d, m),
            (sd_cylinder_z(p, (0, 0, 0.1), mouth + 0.02, 0.18), V_GLOW),
            (sd_sphere(p, (0.1, -0.08, 0.26), 0.06), V_EYE),
        )
    return scene, vrell_mats("a", glow)


# --------------------------------------------------------------------------- Vrell: style B

def skitter_b(anim=0.0, glow=1.0):
    """Skitter, style B: armoured beetle, split elytra with glowing gap, six legs, mandibles."""
    beat = 0.22 * anim

    def scene(p):
        p = _face_down(p)
        q = mirror_x(p)
        e = _hinge(q, (0.02, 0.2, 0.2), beat)
        legs = []
        for y, dy in ((0.3, 0.25), (0.0, 0.0), (-0.3, -0.25)):
            legs += [(sd_capsule(q, (0.3, y, 0.0), (0.72, y + dy, -0.08), 0.05, 0.03), V_DARK),
                     (sd_capsule(q, (0.72, y + dy, -0.08), (0.86, y + dy * 1.6 - 0.1, -0.12),
                                 0.03, 0.01), V_BONE)]
        d, m = union(
            (sd_ellipsoid(e, (0.23, -0.1, 0.05), (0.28, 0.62, 0.3)), V_SEAM),
            (sd_ellipsoid(p, (0, 0.55, 0.0), (0.24, 0.2, 0.18)), V_BODY),
            k=0.03,
        )
        d, m = union((d, m), *legs, k=0.03)
        return union(
            (d, m),
            (sd_capsule(q, (0.1, 0.68, 0.0), (0.2, 0.98, -0.02), 0.06, 0.015), V_BONE),
            (sd_ellipsoid(p, (0, -0.1, 0.12), (0.05, 0.6, 0.12)), V_GLOW),
            (sd_sphere(q, (0.11, 0.66, 0.1), 0.045), V_EYE),
            k=0.015,
        )
    return scene, vrell_mats("b", glow)


def needler_b(anim=0.0, glow=1.0):
    """Needler, style B: crab-like crustacean. Wide flat carapace (wider than long) with a
    ridged rim, two big claws forward around a thorn launcher, four splayed legs per side."""
    claw = 0.18 * anim

    def scene(p):
        p = _face_down(p)
        q = mirror_x(p)
        c = _hinge(q, (0.4, 0.3, 0.0), claw, axis="z")
        legs = []
        for i, (y, a) in enumerate(((0.1, 0.25), (-0.12, 0.0), (-0.32, -0.3), (-0.5, -0.6))):
            ex, ey = 0.95, y + np.sin(a) * 0.35
            legs += [(sd_capsule(q, (0.5, y, -0.02), (ex - 0.12, ey + 0.05, -0.08), 0.05, 0.035),
                      V_DARK),
                     (sd_capsule(q, (ex - 0.12, ey + 0.05, -0.08), (ex, ey - 0.12, -0.14),
                                 0.035, 0.012), V_BONE)]
        d, m = union(
            (sd_ellipsoid(p, (0, -0.1, 0.02), (0.64, 0.46, 0.22)), V_SEAM),
            (sd_ellipsoid(p, (0, -0.1, 0.12), (0.46, 0.32, 0.16)), V_BODY),
            k=0.06,
        )
        d, m = union((d, m), *legs, k=0.03)
        d, m = union(
            (d, m),
            (sd_capsule(c, (0.36, 0.22, 0.0), (0.58, 0.55, 0.0), 0.1, 0.09), V_DARK),
            (sd_ellipsoid(c, (0.6, 0.72, 0.0), (0.18, 0.22, 0.12)), V_BODY),
            (sd_capsule(c, (0.52, 0.86, 0.0), (0.38, 1.02, 0.0), 0.06, 0.015), V_BONE),
            (sd_capsule(c, (0.68, 0.9, 0.0), (0.62, 1.06, 0.0), 0.05, 0.012), V_BONE),
            k=0.04,
        )
        return union(
            (d, m),
            (sd_capsule(p, (0, 0.2, 0.14), (0, 0.72, 0.06), 0.08, 0.035), V_BONE),
            (sd_sphere(p, (0, 0.72, 0.06), 0.055), V_EYE),
            (sd_capsule(q, (0.16, 0.28, 0.16), (0.22, 0.44, 0.2), 0.03, 0.02), V_DARK),
            (sd_sphere(q, (0.22, 0.45, 0.21), 0.05), V_GLOW),
            k=0.015,
        )
    return scene, vrell_mats("b", glow)


def spine_turret_b(anim=0.0, glow=1.0):
    """Spine Turret, style B: plated barnacle cone with a triple-spike barrel cluster and glow
    vents between the plates."""
    def scene(p):
        plates = [(sd_ellipsoid(rotate_z(p, a), (0, 0.45, 0.08), (0.3, 0.36, 0.24)), V_SEAM)
                  for a in np.linspace(0.3, 2 * np.pi + 0.3, 6, endpoint=False)]
        legs = [(sd_capsule(rotate_z(p, a), (0, 0.55, 0.0), (0.0, 0.98, -0.15), 0.09, 0.03),
                 V_BONE) for a in np.linspace(0.0, 2 * np.pi, 6, endpoint=False)]
        d, m = union(*legs, k=0.03)
        d, m = union((d, m), *plates, k=0.03)
        r = rotate_z(_face_down(p), -anim)
        rq = mirror_x(r)
        d, m = union((d, m), (sd_sphere(p, (0, 0, 0.3), 0.33), V_BODY), k=0.04)
        return union(
            (d, m),
            (sd_capsule(rq, (0.1, 0.1, 0.45), (0.14, 0.82, 0.42), 0.06, 0.02), V_BONE),
            (sd_capsule(r, (0, 0.1, 0.48), (0, 0.95, 0.46), 0.07, 0.02), V_BONE),
            (sd_sphere(r, (0, -0.12, 0.6), 0.09), V_EYE),
            (sd_sphere(rq, (0.14, -0.05, 0.56), 0.05), V_GLOW),
            k=0.03,
        )
    return scene, vrell_mats("b", glow)


# --------------------------------------------------------------------------- Ascendancy

def talon(anim=0.0, glow=1.0):
    """Talon: angular forward-swept stealth interceptor, black facets, gold chines and crest,
    red running lights, twin orange-red engines. ``anim`` = bank (radians)."""
    wing = [(0.12, 0.05), (0.92, 0.38), (0.98, 0.26), (0.70, -0.20), (0.14, -0.55)]
    body = [(0.0, 1.02), (0.16, 0.45), (0.22, -0.2), (0.26, -0.72), (0.0, -0.62)]
    tail = [(0.18, -0.45), (0.46, -0.78), (0.42, -0.90), (0.18, -0.72)]

    def scene(p):
        p = _face_down(rotate_y(p, anim))
        q = mirror_x(p)
        roof = lambda x, y: np.clip(1.0 - 3.4 * np.abs(x), 0.25, 1.0)
        d, m = union(
            (sd_plate(q, body, 0.0, 0.14, 0.0, taper=roof), A_HULL),
            (sd_plate(q, wing, -0.02, 0.05, 0.0,
                      taper=lambda x, y: np.clip(1.2 - x, 0.25, 1)), A_FACET),
            (sd_plate(q, tail, 0.03, 0.04, 0.0), A_FACET),
        )
        d, m = union(
            (d, m),
            (sd_plate(p, [(0, 0.58), (0.07, 0.28), (0.0, 0.12), (-0.07, 0.28)], 0.12,
                      0.03, 0.0), A_GLASS),
            (sd_cylinder_y(q, (0.12, -0.72, 0.0), 0.07, 0.03), A_ENGINE),
            (sd_capsule(q, (0.6, 0.08, -0.02), (0.6, 0.45, -0.02), 0.03, 0.02), A_GUN),
            (sd_sphere(q, (0.95, 0.3, -0.02), 0.035), A_RED),
        )
        # gold chines along the leading edge and the spine
        lead = (q[:, 1] > 0.05 + (q[:, 0] - 0.12) * 0.41 - 0.06) & (q[:, 0] > 0.14)
        m = np.where(lead & (m == A_FACET), A_GOLD, m)
        m = np.where((q[:, 0] < 0.025) & (q[:, 1] < 0.1) & (q[:, 1] > -0.55) & (m == A_HULL),
                     A_GOLD, m)
        return d, m
    return scene, asc_mats(glow)


def gilded_gunship(anim=0.0, glow=1.0):
    """Gilded Gunship: heavy armoured wedge, gold-plated front armour, ring-burst dome on
    top, four exposed engines at the rear (the weak point). ``anim`` = dome charge glow."""
    hull = [(0.0, 1.0), (0.55, 0.62), (0.7, 0.1), (0.62, -0.6), (0.4, -0.82), (0.0, -0.78)]
    armour = [(0.0, 1.04), (0.58, 0.64), (0.68, 0.30), (0.0, 0.50)]
    pod = [(0.62, 0.2), (0.98, -0.05), (0.98, -0.5), (0.62, -0.55)]

    def scene(p):
        p = _face_down(p)
        q = mirror_x(p)
        roof = lambda x, y: np.clip(1.0 - 1.3 * np.abs(x), 0.3, 1.0)
        d, m = union(
            (sd_plate(q, hull, 0.0, 0.2, 0.0, taper=roof), A_HULL),
            (sd_plate(q, armour, 0.06, 0.2, 0.0, taper=roof), A_GOLD),
            (sd_plate(q, pod, -0.02, 0.1, 0.0), A_FACET),
        )
        d, m = union(
            (d, m),
            (sd_cylinder_z(p, (0, -0.08, 0.2), 0.24, 0.06), A_FACET),
            (sd_sphere(p, (0, -0.08, 0.22), 0.17), A_GLASS),
            (sd_cylinder_y(q, (0.18, -0.84, 0.0), 0.09, 0.07), A_ENGINE),
            (sd_cylinder_y(q, (0.8, -0.56, -0.02), 0.08, 0.05), A_ENGINE),
            (sd_box(q, (0.34, -0.3, 0.14), (0.08, 0.2, 0.05), 0.0), A_GUN),
            (sd_sphere(q, (0.98, -0.05, 0.0), 0.04), A_RED),
            (sd_sphere(p, (0, 0.72, 0.2), 0.04), A_RED),
        )
        m = np.where((np.abs(q[:, 1] + 0.25) < 0.035) & (m == A_HULL), A_GOLD, m)
        return d, m
    mats = asc_mats(glow)
    # the dome glows red as the ring burst charges
    mats[A_GLASS] = Material(hx("300818"), metal=0.9, shininess=150, spec=1.4,
                             emission=tuple(RED * max(0.0, 0.2 + 0.6 * anim) * glow))
    return scene, mats


def rail_bunker(anim=0.0, glow=1.0):
    """Rail Bunker: hardened octagonal bunker, gold rail coils on a long twin-rail barrel,
    red sensor. ``anim`` = barrel aim angle (radians)."""
    octa = [(np.cos(a) * 0.72, np.sin(a) * 0.72) for a in np.linspace(np.pi / 8, 2 * np.pi
                                                                      + np.pi / 8, 8,
                                                                      endpoint=False)]
    inner = [(x * 0.6, y * 0.6) for x, y in octa]

    def scene(p):
        roof = lambda x, y: np.clip(1.0 - 0.9 * np.sqrt(x * x + y * y), 0.35, 1.0)
        d, m = union(
            (sd_plate(p, octa, 0.1, 0.18, 0.0, taper=roof), A_HULL),
            (sd_plate(p, inner, 0.28, 0.08, 0.0), A_FACET),
        )
        r = rotate_z(_face_down(p), -anim)
        rq = mirror_x(r)
        return union(
            (d, m),
            (sd_box(r, (0, 0.05, 0.4), (0.2, 0.26, 0.08), 0.02), A_FACET),
            (sd_box(rq, (0.07, 0.62, 0.42), (0.025, 0.42, 0.03), 0.0), A_GUN),
            (sd_box(r, (0, 0.34, 0.43), (0.12, 0.035, 0.05), 0.0), A_GOLD),
            (sd_box(r, (0, 0.55, 0.43), (0.12, 0.035, 0.05), 0.0), A_GOLD),
            (sd_box(r, (0, 0.76, 0.43), (0.12, 0.035, 0.05), 0.0), A_GOLD),
            (sd_sphere(r, (0, -0.12, 0.5), 0.06), A_RED),
            (sd_box(p, (0.0, -0.62, 0.2), (0.22, 0.05, 0.03), 0.0), A_GOLD),
        )
    return scene, asc_mats(glow)


# --------------------------------------------------------------------------- boss

B_HULL, B_PLATE, B_BAY, B_SAC, B_CORE, B_BONE, B_LIMB, B_EYE = range(8)


def brood_carrier_mats(glow=1.0, bays_open=0.0, core_open=0.0):
    g = glow
    return [
        Material(CHITIN[2], metal=0.3, shininess=90, spec=1.0, pattern=_ridges(9, 0.22)),
        Material(CHITIN[3], metal=0.35, shininess=110, spec=1.1, pattern=_plates(2.2, 0.05)),
        Material(CHITIN[1], metal=0.3, shininess=70, spec=0.8, pattern=_ridges(40, 0.3, axis=0),
                 emission=tuple(TEAL * 0.8 * g), emission_pattern=_seam(0.03, x0=0.9)),
        Material(hx("6e0a78"), metal=0.1, shininess=60, spec=0.9,
                 emission=tuple(PINK * (0.35 + 1.1 * bays_open) * g),
                 emission_pattern=_veins(14, 1.1, 0.6)),
        Material((0.05, 0.05, 0.05), emission=tuple(TEAL * (0.5 + 1.0 * core_open) * g)),
        Material(CHITIN[4], metal=0.2, shininess=70, spec=0.7),
        Material(CHITIN[1], metal=0.3, shininess=70, spec=0.7, pattern=_ridges(30, 0.3)),
        Material((0.05, 0.05, 0.05), emission=tuple(PINK * 1.4 * g)),
    ]


BROOD_BAYS = [(-0.62, 0.95), (-0.72, 0.35), (-0.74, -0.25), (-0.66, -0.85),
              (0.62, 0.95), (0.72, 0.35), (0.74, -0.25), (0.66, -0.85)]
BROOD_CORE = (0.0, 0.05)


def brood_carrier(anim=0.0, glow=1.0, bays_open=0.0, core_open=0.0):
    """Brood Carrier (Act 1 boss): a living carrier ~2.5x longer than wide. Model spans
    x in +-1.0, y in +-2.4 (nose/head at +Y before turning down). Segmented dorsal plates,
    four launch bays per flank (sac weak points), a central core under a plate iris,
    a head with mandible fan-turrets and trailing tendrils."""
    def scene(p):
        p = _face_down(p)
        q = mirror_x(p)
        segs = [(sd_ellipsoid(p, (0, y, 0.0), (w, 0.42, 0.34)), B_HULL)
                for y, w in ((1.55, 0.62), (1.05, 0.82), (0.5, 0.92), (-0.05, 0.95),
                             (-0.6, 0.9), (-1.15, 0.76), (-1.62, 0.56))]
        d, m = union(*segs, k=0.16)
        plates = [(sd_ellipsoid(p, (0, y, 0.2), (w * 0.62, 0.3, 0.2)), B_PLATE)
                  for y, w in ((1.1, 0.82), (0.55, 0.92), (-0.62, 0.9), (-1.15, 0.76))]
        d, m = union((d, m), *plates, k=0.05)
        # launch bays on the flanks: a sac in a chitin lip
        for bx, by in BROOD_BAYS[4:]:
            d, m = union((d, m), (sd_ellipsoid(q, (bx, by, 0.12), (0.22, 0.2, 0.16)), B_BAY),
                         k=0.06)
            d, m = union((d, m),
                         (sd_ellipsoid(q, (bx + 0.02, by, 0.2 + 0.05 * bays_open),
                                       (0.12 + 0.03 * bays_open, 0.11 + 0.03 * bays_open,
                                        0.08)), B_SAC), k=0.01)
        # flight membranes along the flanks (make it read as a flying carrier, not a grub)
        for y0 in (1.25, 0.1, -1.05):
            fin = [(0.8, y0 + 0.28), (1.1, y0 + 0.05), (1.08, y0 - 0.38), (0.78, y0 - 0.3)]
            d, m = union((d, m), (sd_plate(q, fin, -0.08, 0.035, 0.01,
                                           taper=lambda x, y: np.clip(2.2 - 1.8 * x, 0.3, 1)),
                                  B_LIMB), k=0.08)
        # head, mandible turrets and eyes
        d, m = union(
            (d, m),
            (sd_ellipsoid(p, (0, 2.0, 0.0), (0.42, 0.34, 0.26)), B_HULL),
            (sd_capsule(q, (0.22, 2.1, 0.0), (0.46, 2.42, -0.02), 0.1, 0.04), B_BONE),
            (sd_capsule(q, (0.3, 1.8, 0.05), (0.62, 1.9, 0.0), 0.08, 0.05), B_LIMB),
            k=0.08,
        )
        # core iris: plates part as core_open grows
        iris = [(sd_ellipsoid(rotate_z(p - np.array([0, BROOD_CORE[1], 0]), a),
                              (0, 0.13 + 0.2 * core_open, 0.4), (0.15, 0.2, 0.07)), B_PLATE)
                for a in np.linspace(0, 2 * np.pi, 6, endpoint=False)]
        d, m = union((d, m), (sd_sphere(p, (0, BROOD_CORE[1], 0.14), 0.24), B_CORE), k=0.02)
        d, m = union((d, m), *iris, k=0.0)
        # tendrils trailing at the rear
        tend = [(sd_capsule(q, (x, -1.8, 0.0), (x + 0.1 + 0.05 * anim, -2.4, -0.05),
                            0.08, 0.02), B_LIMB) for x in (0.12, 0.32)]
        d, m = union((d, m), *tend, k=0.06)
        return union(
            (d, m),
            (sd_sphere(q, (0.16, 2.2, 0.18), 0.06), B_EYE),
            (sd_sphere(q, (0.3, 2.08, 0.12), 0.045), B_EYE),
            (sd_capsule(q, (0.52, 1.92, 0.02), (0.6, 2.1, 0.0), 0.05, 0.03), B_EYE),
            k=0.01,
        )
    return scene, brood_carrier_mats(glow, bays_open, core_open)


# registry: slug -> (title, faction, category, native size, model, anim kind, style)
ENEMIES = {
    "skitter-a": ("SKITTER", "VRELL", "air", 30, skitter_a, "wing", "a"),
    "skitter-b": ("SKITTER", "VRELL", "air", 30, skitter_b, "wing", "b"),
    "needler-a": ("NEEDLER", "VRELL", "air", 36, needler_a, "wing", "a"),
    "needler-b": ("NEEDLER", "VRELL", "air", 36, needler_b, "claw", "b"),
    "stinger-a": ("STINGER", "VRELL", "air", 36, stinger_a, "wing", "a"),
    "spore-bomber-a": ("SPORE BOMBER", "VRELL", "air", 48, spore_bomber_a, "pulse", "a"),
    "brood-pod-a": ("BROOD POD", "VRELL", "air", 48, brood_pod_a, "pulse", "a"),
    "mantis-a": ("MANTIS", "VRELL", "air", 60, mantis_a, "arms", "a"),
    "spine-turret-a": ("SPINE TURRET", "VRELL", "ground", 36, spine_turret_a, "aim", "a"),
    "spine-turret-b": ("SPINE TURRET", "VRELL", "ground", 36, spine_turret_b, "aim", "b"),
    "polyp-mortar-a": ("POLYP MORTAR", "VRELL", "ground", 36, polyp_mortar_a, "pulse", "a"),
    "talon-a": ("TALON", "ASCENDANCY", "air", 36, talon, "bank", None),
    "gilded-gunship-a": ("GILDED GUNSHIP", "ASCENDANCY", "air", 72, gilded_gunship, "charge",
                         None),
    "rail-bunker-a": ("RAIL BUNKER", "ASCENDANCY", "ground", 42, rail_bunker, "aim", None),
}
