"""SDF models for concept round 06: three bosses without concept art yet.

Gorgon Frigate   Act 1 mid-boss (L05). A medusa-bell warship of bone chitin with three serpent
                 necks ("Gorgon"): each neck is a segment chain ending in a turret head that
                 aims on its own and fires aimed bursts. Turrets die separately (stumps), then
                 the crown petals open over the core.
Harbour Kraken   Act 2 mid-boss (L11). A real cephalopod: rust mantle with papillae, big lime
                 eyes (weak points), a crimson-lit beak, eight sucker-lined arms built as
                 segment chains. Wrapped around an offshore UTC platform.
Siege Spire      Act 2 boss (L14). A rooted Vrell citadel: six roots across the capital's
                 blocks, each ending in a root turret or mortar pod, a tall ribbed trunk drawn
                 with the roof pushed away from the screen centre (perspective rule for tall
                 ground structures), a petal crown with the launch maw. Tears free and flies.
Wraith           The medium Vrell flyer the Spire launches (bone hide, violet glow).

Models face +Y (forward) like render.archetype_models; render the turning parts with
``enemy_rigs.ModelSpaceAngleSprites``. Colours follow the round 04 role colours; the three
bosses share one boss convention: **weak points glow lime** (like the Brood Carrier).
"""
import numpy as np

from . import enemy_models as em
from .enemy_models import V_BODY, V_BONE, V_DARK, V_EYE, V_GLOW, V_SAC, V_SEAM
from .sdf import (Material, mirror_x, rotate_x, rotate_z, sd_box, sd_capsule, sd_cylinder_z,
                  sd_ellipsoid, sd_plate, sd_sphere, smoothstep, union)

em.ROLE_SCHEMES.update({
    "gorgon-frigate": ("bone", "violet", "lime"),      # ranged gunner (aimed bursts)
    "harbour-kraken": ("rust", "crimson", "lime"),     # slams lanes (lines of danger)
    "siege-spire":    ("slate", "violet", "lime"),     # rooted; aimed root turrets
    "wraith":         ("bone", "violet", "violet"),    # medium gunner, rear ambush
})

V_LASER = 7          # extra slot: crimson emitter (Spire laser phase)
V_DEAD = 8           # extra slot: withered, dead tissue


def radial_ribs(n=12, width=0.13, rings=0.0):
    """Rib lines radiating from the model origin (medusa bell, spire trunk)."""
    def f(p, nn):
        a = np.arctan2(p[:, 1], p[:, 0])
        rib = np.abs(np.sin(a * n / 2)) < width
        out = np.where(rib, 0.68, 1.0)
        if rings:
            r = np.sqrt(p[:, 0] ** 2 + p[:, 1] ** 2)
            out = out * (0.9 + 0.1 * np.cos(r * rings) ** 2)
        return out
    return f


def mottled(scale=9.0):
    """Cephalopod skin: soft blotches, no stripes."""
    def f(p, n):
        x, y = p[:, 0] * scale, p[:, 1] * scale
        v = np.sin(x * 1.3 + np.sin(y * 0.7)) * np.sin(y * 1.1 + np.cos(x * 0.9))
        return 0.86 + 0.14 * v
    return f


def mats(unit, glow=1.0, weak_k=1.0, laser=0.0, body_pattern=None):
    """Role-colour Vrell materials plus a crimson emitter and a dead-tissue slot.
    ``body_pattern`` replaces the default ridge stripes on the body and seam slots."""
    m = em.vrell_scheme_mats(unit, "a", glow)
    mid, dark, bone, gl, weak = em.scheme_colors(unit)
    if body_pattern is not None:
        from dataclasses import replace
        m[V_BODY] = replace(m[V_BODY], pattern=body_pattern)
        m[V_SEAM] = replace(m[V_SEAM], pattern=None)
    m[V_EYE] = Material((0.04, 0.04, 0.04), emission=tuple(weak * 1.5 * glow * weak_k))
    crimson = np.array(em.hx(em.GLOWS["crimson"]))
    m.append(Material((0.06, 0.02, 0.02), emission=tuple(crimson * (0.35 + 1.4 * laser))))
    m.append(Material(em._mix(dark, (0.5, 0.47, 0.44), 0.75), metal=0.05, shininess=20,
                      spec=0.2, pattern=lambda p, n: 0.75 + 0.25 * np.cos(p[:, 0] * 31) ** 2))
    return m


def bounded(p, bound, margin, build):
    """Evaluate a costly group of primitives only near it. ``bound`` is a lower bound of the
    distance to the group (e.g. the distance to a volume containing it); points farther than
    ``margin`` get ``bound`` (safe for marching), the rest get ``build(p_near)``."""
    d = np.array(bound, dtype=np.float64)
    m = np.zeros(len(p), dtype=np.int32)
    near = d < margin
    if np.any(near):
        dn, mn = build(p[near])
        d[near] = dn
        m[near] = mn
    return d, m


def _hz(q, pivot, angle):
    pv = np.asarray(pivot, dtype=np.float64)
    return rotate_z(q - pv, angle) + pv


# =========================================================================== Gorgon Frigate

GORGON_SOCKETS = [(0.0, 0.86), (0.6, -0.4), (-0.6, -0.4)]   # front, right-rear, left-rear
GORGON_CORE = (0.0, 0.02)


def gorgon_body(core_open=0.0, veil=0.0, glow=1.0, lost=()):
    """Medusa-bell hull. ``lost`` lists socket indices whose turrets are destroyed (the socket
    then shows a torn, leaking stump); ``core_open`` parts the crown petals over the core."""
    def dome_z(x, y):
        return 0.12 + 0.42 * np.sqrt(max(0.0, 1 - (x / 0.74) ** 2 - ((y + 0.02) / 0.84) ** 2))
    rib_segs = []
    for a in np.linspace(0, 2 * np.pi, 12, endpoint=False):
        c, s = np.cos(a), np.sin(a)
        pts = [(r * 0.74 * c, r * 0.84 * s - 0.02) for r in (0.3, 0.55, 0.78, 0.95)]
        for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
            rib_segs.append(((x0, y0, dome_z(x0, y0) - 0.005), (x1, y1, dome_z(x1, y1) - 0.005)))

    def ribs(pp):
        return union(*[(sd_capsule(pp, a, b, 0.034, 0.03), V_DARK) for a, b in rib_segs])

    def rim(pp):
        return union(*[(sd_ellipsoid(pp, (0.84 * np.cos(a), 0.94 * np.sin(a), 0.02),
                                     (0.15, 0.13, 0.07)), V_SEAM)
                       for a in np.linspace(0, 2 * np.pi, 18, endpoint=False)])

    def scene(p):
        q = mirror_x(p)
        dome = sd_ellipsoid(p, (0, -0.02, 0.12), (0.74, 0.84, 0.42))
        d, m = union(
            (dome, V_BODY),
            (sd_ellipsoid(p, (0, -0.02, 0.0), (0.82, 0.92, 0.12)), V_DARK),
            k=0.05,
        )
        # radial ribs over the dome (evaluated only near the dome surface)
        d, m = union((d, m), bounded(p, np.abs(dome) - 0.07, 0.12, ribs), k=0.03)
        # scalloped glowing rim (only near the rim ring)
        ring = sd_ellipsoid(p, (0, -0.02, 0.02), (1.06, 1.16, 0.22))
        inner = -sd_ellipsoid(p, (0, -0.02, 0.02), (0.62, 0.72, 0.4))
        d, m = union((d, m), bounded(p, np.maximum(ring, inner), 0.12, rim), k=0.03)
        # prow with a bone face plate and brow ridges
        d, m = union(
            (d, m),
            (sd_ellipsoid(p, (0, 0.74, 0.22), (0.34, 0.22, 0.22)), V_BONE),
            (sd_capsule(q, (0.08, 0.86, 0.36), (0.3, 0.7, 0.3), 0.05, 0.03), V_BONE),
            k=0.05,
        )
        # sockets for the three serpent necks
        for i, (sx, sy) in enumerate(GORGON_SOCKETS):
            d, m = union((d, m), (sd_cylinder_z(p, (sx, sy, 0.24), 0.17, 0.08), V_DARK), k=0.04)
            if i in lost:     # torn stump leaking ichor
                d, m = union((d, m), (sd_sphere(p, (sx, sy, 0.3), 0.11), V_SAC),
                             (sd_capsule(p, (sx, sy, 0.3), (sx * 1.25, sy * 1.12 + 0.05, 0.22),
                                         0.07, 0.03), V_DARK), k=0.03)
            else:
                d, m = union((d, m), (sd_sphere(p, (sx, sy, 0.3), 0.08), V_SEAM), k=0.02)
        # crown petals over the core (part with core_open)
        cx, cy = GORGON_CORE
        petals = [(sd_ellipsoid(rotate_z(p - np.array([cx, cy, 0]), a),
                                (0, 0.13 + 0.22 * core_open, 0.62 - 0.14 * core_open),
                                (0.14, 0.24, 0.07)), V_BONE)
                  for a in np.linspace(0, 2 * np.pi, 5, endpoint=False)]
        d, m = union((d, m), (sd_sphere(p, (cx, cy, 0.44), 0.16), V_EYE), k=0.02)
        d, m = union((d, m), *petals, k=0.0)
        # trailing veil of short tendrils
        def tendrils(pp):
            tend = []
            for i, x in enumerate((-0.4, -0.2, 0.0, 0.2, 0.4)):
                pts = [(x, -0.78, 0.02)]
                for k in range(1, 4):
                    sway = 0.1 * k * np.sin(veil + i * 1.1 + k * 0.9)
                    pts.append((x * (1 + 0.1 * k) + sway, -0.78 - 0.17 * k, 0.0))
                for k in range(3):
                    tend.append((sd_capsule(pp, pts[k], pts[k + 1], 0.085 - 0.02 * k,
                                            0.07 - 0.02 * k), V_SAC))
            return union(*tend)
        box = sd_box(p, (0, -1.08, 0.0), (0.75, 0.38, 0.15))
        return union((d, m), bounded(p, box, 0.1, tendrils), k=0.05)
    return scene, mats("gorgon-frigate", glow, body_pattern=radial_ribs(24, 0.05, 30))


def gorgon_neck(glow=1.0):
    """One neck segment: ribbed tube with a glowing dorsal seam and two small spines."""
    def scene(p):
        q = mirror_x(p)
        d, m = union(
            (sd_capsule(p, (0, -0.38, 0.0), (0, 0.38, 0.0), 0.34, 0.3), V_BODY),
            (sd_ellipsoid(p, (0, 0.0, 0.0), (0.4, 0.12, 0.32)), V_DARK),
            k=0.05,
        )
        return union((d, m),
                     (sd_capsule(p, (0, -0.3, 0.3), (0, 0.3, 0.28), 0.06, 0.05), V_SEAM),
                     (sd_capsule(q, (0.22, 0.1, 0.22), (0.36, -0.12, 0.3), 0.05, 0.01), V_BONE),
                     k=0.03)
    return scene, mats("gorgon-frigate", glow, body_pattern=lambda p, n: np.ones(len(p)))


def gorgon_head(mouth=0.0, glow=1.0):
    """Serpent turret head: long wedge skull with a low cobra hood joined to it (violet seams),
    a hinged lower jaw around the violet gun mouth, lime eyes (the turret's weak point)."""
    def scene(p):
        q = mirror_x(p)
        jaw = _hz(q, (0.08, 0.2, 0.0), -0.4 * mouth)
        d, m = union(
            (sd_ellipsoid(p, (0, 0.12, 0.12), (0.27, 0.6, 0.2)), V_BODY),
            (sd_ellipsoid(p, (0, -0.32, 0.05), (0.42, 0.34, 0.12)), V_SEAM),     # hood
            (sd_capsule(p, (0, 0.2, 0.2), (0, 0.72, 0.12), 0.11, 0.05), V_BODY),  # snout ridge
            k=0.12,
        )
        d, m = union(
            (d, m),
            (sd_capsule(jaw, (0.1, 0.2, 0.02), (0.06, 0.72, -0.02), 0.09, 0.04), V_BONE),
            (sd_ellipsoid(p, (0, 0.62 + 0.08 * mouth, 0.02), (0.05 + 0.09 * mouth, 0.16, 0.07)),
             V_GLOW),
            k=0.04,
        )
        return union(
            (d, m),
            (sd_sphere(q, (0.15, 0.3, 0.24), 0.075), V_EYE),
            (sd_capsule(q, (0.14, 0.42, 0.26), (0.2, 0.1, 0.27), 0.04, 0.03), V_DARK),  # brow
            (sd_capsule(p, (0, -0.1, 0.28), (0, -0.5, 0.16), 0.05, 0.03), V_BONE),       # crest
            k=0.03,
        )
    return scene, mats("gorgon-frigate", glow, body_pattern=lambda p, n: np.ones(len(p)))


# =========================================================================== Harbour Kraken

KRAKEN_EYES = [(0.46, 0.5), (-0.46, 0.5)]


def kraken_mantle(eyes=1.0, beak=0.0, glow=1.0):
    """Cephalopod head and mantle (forward = arms end, +Y). ``eyes`` 0 = lids closed,
    1 = open lime eyes (weak points); ``beak`` lights the crimson beak/siphon before a volley."""
    rng = np.random.default_rng(7)
    warts = [(rng.uniform(-0.45, 0.45), rng.uniform(-1.25, 0.05), rng.uniform(0.04, 0.08))
             for _ in range(26)]

    def scene(p):
        q = mirror_x(p)
        d, m = union(
            (sd_ellipsoid(p, (0, -0.5, 0.14), (0.56, 0.95, 0.42)), V_BODY),
            (sd_ellipsoid(p, (0, 0.45, 0.1), (0.66, 0.48, 0.34)), V_BODY),
            (sd_ellipsoid(p, (0, 0.9, 0.02), (0.58, 0.3, 0.2)), V_DARK),
            k=0.22,
        )
        bumps = []
        for x, y, r in warts:
            z = 0.14 + 0.42 * np.sqrt(max(0.0, 1 - (x / 0.56) ** 2 - ((y + 0.5) / 0.95) ** 2))
            bumps.append((sd_sphere(p, (x, y, z - 0.03), r), V_BODY))
        d, m = union((d, m), *bumps, k=0.03)
        # glowing mantle veins (crimson seams) along the back
        d, m = union((d, m), (sd_capsule(p, (0, -1.25, 0.42), (0, 0.15, 0.46), 0.05, 0.04),
                              V_SEAM), k=0.04)
        # eyes with lids
        for ex, ey in KRAKEN_EYES[:1]:
            d, m = union((d, m), (sd_ellipsoid(q, (ex, ey, 0.22), (0.17, 0.2, 0.14)), V_DARK),
                         k=0.05)
        if eyes >= 0.5:      # open lime eye (weak point)
            d, m = union((d, m), (sd_ellipsoid(q, (0.47, 0.52, 0.3), (0.15, 0.18, 0.11)),
                                  V_EYE), k=0.0)
        else:                # closed: a dark lid ridge over the eye
            d, m = union((d, m), (sd_ellipsoid(q, (0.47, 0.52, 0.3), (0.15, 0.17, 0.07)),
                                  V_DARK), k=0.02)
        # beak and siphon between the arm bases
        d, m = union(
            (d, m),
            (sd_ellipsoid(p, (0, 1.02, 0.12), (0.14, 0.14, 0.12)), V_BONE),
            (sd_sphere(p, (0, 1.04, 0.2), 0.06 + 0.04 * beak), V_GLOW),
            (sd_capsule(p, (0.22, 0.75, 0.3), (0.3, 1.0, 0.28), 0.07, 0.05), V_DARK),
            k=0.03,
        )
        # eight arm roots around the front
        roots = [(sd_capsule(p, (0.3 * np.sin(a), 0.85 + 0.12 * np.cos(a), 0.06),
                             (0.62 * np.sin(a), 0.9 + 0.36 * np.cos(a), 0.02), 0.13, 0.1),
                  V_BODY) for a in np.linspace(-1.6, 1.6, 8)]
        return union((d, m), *roots, k=0.06)
    return scene, mats("harbour-kraken", glow, laser=beak, body_pattern=mottled())


def kraken_arm(glow=1.0):
    """Arm segment: tapered tube, dark ring, crimson dorsal seam, pale suckers along the
    edges (the arm is slightly rolled so they show from above)."""
    def scene(p):
        q = mirror_x(p)
        d, m = union(
            (sd_capsule(p, (0, -0.42, 0.0), (0, 0.42, 0.0), 0.36, 0.32), V_BODY),
            (sd_ellipsoid(p, (0, -0.36, 0.0), (0.39, 0.08, 0.34)), V_DARK),
            k=0.05,
        )
        suckers = [(sd_sphere(q, (0.3, y, 0.1), 0.09), V_BONE) for y in (-0.22, 0.12)]
        return union((d, m), *suckers,
                     (sd_capsule(p, (0, -0.32, 0.31), (0, 0.32, 0.29), 0.06, 0.05), V_SEAM),
                     k=0.03)
    return scene, mats("harbour-kraken", glow, body_pattern=mottled())


def kraken_tip(glow=1.0):
    """Arm tip: a thin curl."""
    def scene(p):
        pts = [(0.0, -0.45), (0.05, 0.0), (0.2, 0.32), (0.42, 0.42), (0.5, 0.22)]
        items = [(sd_capsule(p, (*pts[i], 0.0), (*pts[i + 1], 0.0), 0.26 - 0.05 * i,
                             0.22 - 0.05 * i), V_BODY) for i in range(4)]
        d, m = union(*items, k=0.05)
        return union((d, m), (sd_sphere(p, (0.0, -0.1, 0.18), 0.07), V_BONE),
                     (sd_sphere(p, (0.14, 0.26, 0.14), 0.06), V_BONE), k=0.02)
    return scene, mats("harbour-kraken", glow, body_pattern=mottled())


def platform():
    """Offshore UTC harbour platform (hard-surface, UTC hull colours): deck slab with
    railings, a helipad, containers, a crane and a damaged corner. Not rotated."""
    hull = [tuple(c / 255 for c in col) for col in
            ((18, 22, 50), (42, 48, 104), (78, 90, 160), (138, 150, 208), (200, 208, 244))]

    def pad(p, n):            # helipad: ring and an "H"
        x, y = p[:, 0] + 0.5, p[:, 1] - 0.18
        ring = np.abs(np.sqrt(x * x + y * y) - 0.24) < 0.022
        h = ((np.abs(np.abs(x) - 0.07) < 0.02) & (np.abs(y) < 0.11)) | \
            ((np.abs(x) < 0.07) & (np.abs(y) < 0.02))
        return np.where(ring | h, 2.2, 1.0)

    def deck(p, n):
        lines = (np.abs(((p[:, 0] * 5) % 1.0) - 0.5) > 0.47) | (np.abs(((p[:, 1] * 5) % 1.0) - 0.5) > 0.47)
        return np.where(lines, 0.78, 1.0)

    m = [
        Material(hull[2], metal=0.5, shininess=60, spec=0.6, pattern=deck),      # 0 deck
        Material(hull[1], metal=0.6, shininess=80, spec=0.7),                     # 1 frame
        Material(hull[2], metal=0.4, shininess=40, spec=0.4, pattern=pad),        # 2 helipad
        Material((0.55, 0.16, 0.14), metal=0.3, shininess=50, spec=0.5),          # 3 red box
        Material((0.12, 0.42, 0.62), metal=0.3, shininess=50, spec=0.5),          # 4 blue box
        Material((0.36, 0.40, 0.34), metal=0.3, shininess=50, spec=0.5),          # 5 grey-green
        Material(hull[4], metal=0.7, shininess=90, spec=0.9),                     # 6 crane
        Material((0.03, 0.03, 0.05), metal=0.0, shininess=10, spec=0.1),          # 7 hole
        Material((0.05, 0.02, 0.02), emission=(1.0, 0.25, 0.2)),                  # 8 warning lamp
    ]

    def scene(p):
        d, mm = union(
            (sd_box(p, (0, 0, 0.0), (1.0, 0.72, 0.08), 0.02), 0),
            (sd_box(p, (0, 0, -0.1), (1.04, 0.76, 0.05), 0.02), 1),
        )
        d, mm = union((d, mm), (sd_cylinder_z(p, (-0.5, 0.18, 0.09), 0.3, 0.02), 2))
        boxes = []
        for i, (x, y, kind) in enumerate(((0.25, 0.42, 3), (0.25, 0.22, 4), (0.55, 0.42, 5),
                                          (0.55, 0.22, 4), (0.4, -0.35, 3), (0.72, -0.35, 5),
                                          (-0.2, -0.45, 4))):
            boxes.append((sd_box(p, (x, y, 0.16), (0.13, 0.08, 0.08), 0.01), kind))
        d, mm = union((d, mm), *boxes)
        crane = [(sd_box(p, (-0.62, -0.32, 0.25), (0.12, 0.12, 0.16), 0.02), 6),
                 (sd_box(rotate_z(p - np.array([-0.62, -0.32, 0]), 0.5), (0.45, 0, 0.4),
                         (0.5, 0.05, 0.04), 0.01), 6)]
        d, mm = union((d, mm), *crane)
        # railings along the edges
        rails = [(sd_box(p, (0, 0.71, 0.12), (1.0, 0.012, 0.03)), 1),
                 (sd_box(p, (0, -0.71, 0.12), (1.0, 0.012, 0.03)), 1),
                 (sd_box(p, (0.99, 0, 0.12), (0.012, 0.72, 0.03)), 1),
                 (sd_box(p, (-0.99, 0, 0.12), (0.012, 0.72, 0.03)), 1)]
        d, mm = union((d, mm), *rails)
        lamps = [(sd_sphere(p, (x, y, 0.16), 0.035), 8) for x, y in ((0.95, 0.67), (-0.95, -0.67),
                                                                      (0.95, -0.67), (-0.95, 0.67))]
        d, mm = union((d, mm), *lamps)
        # torn corner (the Kraken's work): carve a ragged hole
        hole = sd_ellipsoid(p, (0.92, 0.62, 0.0), (0.24, 0.2, 0.3))
        return np.maximum(d, -hole), mm
    return scene, m


# =========================================================================== Siege Spire

SPIRE_ROOTS = [(-0.15 + a) for a in np.linspace(0, 2 * np.pi, 6, endpoint=False)]
SPIRE_ROOT_LEN = 1.55
SPIRE_HEIGHT = 2.4
SPIRE_PODS = [(SPIRE_ROOT_LEN * np.cos(a), SPIRE_ROOT_LEN * np.sin(a)) for a in SPIRE_ROOTS]
SPIRE_MORTARS = (1, 4)            # root indices whose pods are mortars; the rest aim


def _root_path(a, i):
    """Wavy root polyline from the trunk out along angle ``a``."""
    pts = []
    for k in range(7):
        s = k / 6
        r = 0.35 + (SPIRE_ROOT_LEN - 0.35) * s
        w = 0.12 * np.sin(s * 5.0 + i) * s
        pts.append((r * np.cos(a) - w * np.sin(a), r * np.sin(a) + w * np.cos(a),
                    0.18 - 0.12 * s))
    return pts


def spire(maw=0.0, glow=1.0, dead_roots=False, rooted=True, lean=(0.0, 0.0), laser=0.0,
          torn=0.0, stump=False):
    """The citadel. Model +Y is screen up (it is not turned). ``lean`` = model units the roof
    is pushed per unit of height (perspective away from the screen centre). ``rooted=False``
    gives the torn-free, airborne spire with broken root stubs (``torn`` waves them);
    ``stump=True`` leaves only the dead roots and the jagged broken base it tore out of.
    Costly groups (roots, ridges, crown) are evaluated inside bounding volumes only."""
    lx, ly = lean
    top = SPIRE_HEIGHT
    H = SPIRE_HEIGHT - 0.3
    root_paths = [_root_path(a, i) for i, a in enumerate(SPIRE_ROOTS)]
    ridge_segs = []
    for j in range(3):
        for k in range(6):
            s0, s1 = k / 6, (k + 1) / 6
            a0, a1 = j * 2.1 + s0 * 3.0, j * 2.1 + s1 * 3.0
            r0, r1 = 0.6 - 0.34 * s0, 0.6 - 0.34 * s1
            ridge_segs.append(((r0 * np.cos(a0), r0 * np.sin(a0), 0.15 + s0 * H),
                               (r1 * np.cos(a1), r1 * np.sin(a1), 0.15 + s1 * H)))

    def root_group(pts, mat):
        def build(pp):
            items = []
            for k in range(len(pts) - 1):
                r0 = 0.26 - 0.022 * k
                items.append((sd_capsule(pp, pts[k], pts[k + 1], r0, r0 - 0.022), mat))
                if mat != V_DEAD and k in (1, 3, 5):
                    items.append((sd_sphere(pp, (pts[k][0], pts[k][1], pts[k][2] + 0.06),
                                            r0 * 0.85), V_DARK))
            if mat != V_DEAD:
                for k in range(1, 5):
                    items.append((sd_capsule(pp, (*pts[k][:2], pts[k][2] + 0.22 - 0.02 * k),
                                             (*pts[k + 1][:2], pts[k + 1][2] + 0.2 - 0.02 * k),
                                             0.05, 0.045), V_SAC))
            return union(*items, k=0.1)
        return build

    def roots(p, mat):
        d = m = None
        for pts in root_paths:
            bound = sd_capsule(p, pts[0], pts[-1], 0.5)
            g = bounded(p, bound, 0.1, root_group(pts, mat))
            d, m = g if d is None else union((d, m), g, k=0.1)
        return d, m

    def trunk(q):
        items = []
        for k in range(8):
            s = k / 7
            r = 0.58 - 0.34 * s
            items.append((sd_ellipsoid(q, (0, 0, 0.15 + s * H), (r, r, 0.26)), V_BODY))
        return union(*items, k=0.12)

    def spines(qq):            # bone buttress spines up the trunk (height cue)
        items = []
        for s, ln in ((0.3, 0.42), (0.6, 0.34)):
            z0 = 0.15 + s * H
            r = 0.58 - 0.34 * s
            for a in np.linspace(0, 2 * np.pi, 6, endpoint=False) + s * 2:
                c, si = np.cos(a), np.sin(a)
                items.append((sd_capsule(qq, (r * c, r * si, z0), ((r + ln) * c, (r + ln) * si,
                                                                     z0 + 0.3), 0.07, 0.015),
                              V_BONE))
        return union(*items)

    def ridges(qq):
        return union(*[(sd_capsule(qq, a, b, 0.05, 0.04), V_SEAM) for a, b in ridge_segs])

    def crown(qq):
        petals, claws, tips = [], [], []
        open_ = 0.1 * maw
        for a in np.linspace(0, 2 * np.pi, 6, endpoint=False) + 0.3:
            c, s = np.cos(a), np.sin(a)
            r1 = 0.58 + open_
            petals.append((sd_capsule(qq, (0.18 * c, 0.18 * s, top), (r1 * c, r1 * s, top + 0.28),
                                      0.17, 0.1), V_BODY))
            claws.append((sd_capsule(qq, (r1 * c, r1 * s, top + 0.28),
                                     ((r1 + 0.14) * c, (r1 + 0.14) * s, top + 0.58), 0.09, 0.02),
                          V_BONE))
            tips.append((sd_sphere(qq, ((r1 + 0.04) * c, (r1 + 0.04) * s, top + 0.36), 0.075),
                         V_LASER))
        d, m = union(*petals, k=0.08)
        d, m = union((d, m), *claws, k=0.04)
        d, m = union((d, m), *tips, k=0.0)
        d, m = union((d, m), (sd_ellipsoid(qq, (0, 0, top + 0.05), (0.3, 0.3, 0.14)), V_DARK),
                     k=0.05)
        return union((d, m), (sd_sphere(qq, (0, 0, top + 0.02 + 0.08 * maw), 0.12 + 0.08 * maw),
                              V_EYE), k=0.02)

    def scene(p):
        if stump:
            items = [(sd_ellipsoid(p, (0, 0, 0.05), (0.62, 0.62, 0.22)), V_DEAD)]
            for a in np.linspace(0, 2 * np.pi, 7, endpoint=False):
                items.append((sd_capsule(p, (0.38 * np.cos(a), 0.38 * np.sin(a), 0.1),
                                         (0.5 * np.cos(a + 0.2), 0.5 * np.sin(a + 0.2),
                                          0.42 + 0.1 * np.sin(3 * a)), 0.1, 0.02), V_DARK))
            d, m = union(*items, k=0.05)
            d, m = union((d, m), (sd_cylinder_z(p, (0, 0, 0.22), 0.36, 0.06), V_SAC), k=0.0)
            return union((d, m), roots(p, V_DEAD), k=0.1)
        z = np.clip(p[:, 2], 0.0, None)
        q = p.copy()
        q[:, 0] -= lx * z
        q[:, 1] -= ly * z
        d, m = trunk(q)
        d, m = union((d, m), bounded(q, np.abs(d) - 0.08, 0.12, ridges), k=0.03)
        cb = sd_cylinder_z(q, (0, 0, top + 0.3), 1.0, 0.45)
        d, m = union((d, m), bounded(q, cb, 0.1, crown), k=0.08)
        sb = sd_cylinder_z(q, (0, 0, 1.25), 0.95, 0.75)
        d, m = union((d, m), bounded(q, sb, 0.1, spines), k=0.04)
        if rooted:
            d, m = union((d, m), roots(p, V_DEAD if dead_roots else V_BODY), k=0.1)
            d, m = union((d, m), (sd_ellipsoid(p, (0, 0, -0.05), (0.95, 0.95, 0.22)), V_SAC),
                         k=0.15)
        else:
            stubs = []
            for i, a in enumerate(SPIRE_ROOTS):
                c, s = np.cos(a), np.sin(a)
                sway = 0.15 * np.sin(torn + i * 1.3)
                stubs.append((sd_capsule(q, (0.4 * c, 0.4 * s, 0.1),
                                         ((0.75 + sway) * c - 0.1 * s, (0.75 + sway) * s + 0.1 * c,
                                          -0.05), 0.16, 0.05), V_DARK))
            d, m = union((d, m), *stubs, k=0.08)
        return d, m
    return scene, mats("siege-spire", glow, laser=laser, body_pattern=radial_ribs(16, 0.1))


def root_pod(kind="turret", aim=0.0, open_=0.0, glow=1.0):
    """Root turret (aimed, violet) or root mortar (lime acid mouth). Faces +Y."""
    def scene(p):
        q = mirror_x(p)
        d, m = union(
            (sd_ellipsoid(p, (0, 0, 0.15), (0.55, 0.55, 0.36)), V_BODY),
            *[(sd_ellipsoid(rotate_z(p, a), (0, 0.55, 0.05), (0.2, 0.3, 0.1)), V_DARK)
              for a in np.linspace(0, 2 * np.pi, 7, endpoint=False)],
            k=0.08,
        )
        if kind == "turret":
            return union(
                (d, m),
                (sd_capsule(p, (0, 0.1, 0.42), (0, 0.95, 0.38), 0.13, 0.08), V_BONE),
                (sd_sphere(p, (0, 0.96, 0.38), 0.07), V_GLOW),
                (sd_sphere(q, (0.22, -0.1, 0.42), 0.08), V_EYE),
                (sd_capsule(p, (0, -0.2, 0.46), (0, 0.2, 0.48), 0.08, 0.07), V_SEAM),
                k=0.04,
            )
        return union(
            (d, m),
            (sd_cylinder_z(p, (0, 0, 0.42), 0.3, 0.08), V_DARK),
            (sd_sphere(p, (0, 0, 0.44), 0.18 + 0.08 * open_), V_SAC),
            (sd_sphere(p, (0, 0, 0.52), 0.1 + 0.06 * open_), V_EYE),
            k=0.03,
        )
    kind_unit = "siege-spire"
    m = mats(kind_unit, glow)
    if kind == "mortar":            # area denial = lime glow on the mortar pods
        lime = np.array(em.hx(em.GLOWS["lime"]))
        m[V_SAC] = Material(m[V_DARK].albedo, metal=0.1, shininess=60, spec=0.9,
                            emission=tuple(lime * 1.1 * glow), emission_pattern=None)
    return scene, m


def wraith(cloak=0.0, glow=1.0):
    """Wraith: a pale ghost-manta with a ragged veil; ``cloak`` is drawn by the caller."""
    wing = [(0.12, 0.42), (0.95, -0.05), (0.88, -0.32), (0.36, -0.2), (0.14, -0.55)]

    def scene(p):
        q = mirror_x(p)
        d, m = union(
            (sd_ellipsoid(p, (0, 0.05, 0.08), (0.2, 0.6, 0.14)), V_BODY),
            (sd_plate(q, wing, 0.02, 0.05, 0.02,
                      taper=lambda x, y: np.clip(1.2 - 0.9 * x, 0.3, 1.0)), V_SEAM),
            k=0.08,
        )
        veil = [(sd_plate(q, [(x - 0.07, -0.35), (x + 0.07, -0.35), (x + 0.1, -0.8),
                              (x + 0.02, -0.98), (x - 0.04, -0.8)], -0.02, 0.025, 0.01), V_SAC)
                for x in (0.08, 0.3)]
        return union((d, m), *veil,
                     (sd_sphere(q, (0.08, 0.5, 0.16), 0.05), V_EYE),
                     (sd_sphere(p, (0, 0.6, 0.06), 0.06), V_GLOW), k=0.04)
    return scene, mats("wraith", glow)
