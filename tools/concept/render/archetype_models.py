"""SDF models for concept round 05: new enemy archetypes (segmented, huge, walking, spinning,
swarming). Unlike round 03/04 models these are built **facing +Y (forward)** without a
built-in turn; ``render.enemy_rigs.AngleSprites`` turns them to any heading. Parts of rigs
(tail segments, fins, turrets, legs) have their pivot at the origin.

World units as elsewhere: a part fits in about +-1.15 units when rendered with extent 2.3,
except the Leviathan parts, which use their own scale (see ``LEVIATHAN_S``).

Colours follow the round 04 role colours (design/enemies/README.md "Role colours (draft)");
this module adds the new units to ``enemy_models.ROLE_SCHEMES`` / ``ASC_ACCENTS``.
"""
import numpy as np

from . import enemy_models as em
from .enemy_models import (A_ENGINE, A_FACET, A_GLASS, A_GOLD, A_GUN, A_HULL, A_RED, V_BODY,
                           V_BONE, V_DARK, V_EYE, V_GLOW, V_SAC, V_SEAM)
from .sdf import (Material, mirror_x, rotate_z, sd_box, sd_capsule, sd_cylinder_z,
                  sd_ellipsoid, sd_plate, sd_sphere, union)

# new units in the role-colour scheme (additive; round 04 units are unchanged)
em.ROLE_SCHEMES.update({
    "coilwyrm":   ("rust", "teal", "teal"),        # fast attacker, contact; head = weak point
    "leviathan":  ("bone", "violet", "violet"),    # huge gunner: dorsal turrets fire aimed shots
    "scuttler":   ("slate", "lime", "lime"),       # rooted/ground walker, spits acid
    "whirl-seed": ("plum", "teal", "teal"),        # fodder, contact
    "mote":       ("rust", "crimson", "crimson"),  # fast swarm, dives in from the rear
})
em.ASC_ACCENTS.update({
    "warden-tank": "e8e4f0",     # white
    "strider": "c8202a",         # red
    "buzzsaw-drone": "6a6e78",   # gunmetal
    "rail-serpent": "c8202a",    # red
})

A_TREAD = 7


def vrell(unit, glow=1.0, style="a"):
    return em.vrell_scheme_mats(unit, style, glow)


def asc(unit, glow=1.0, tread=0.0):
    mats = em.asc_scheme_mats(unit, glow)
    phase = tread

    def tread_pat(p, n):
        band = ((p[:, 1] * 10.0 + phase) % 1.0) < 0.45
        return np.where(band, 0.55, 1.0)
    mats.append(Material(em.ASC[2], metal=0.6, shininess=60, spec=0.6, pattern=tread_pat))
    return mats


def _roof(k):
    return lambda x, y: np.clip(1.0 - k * np.abs(x), 0.25, 1.0)


# --------------------------------------------------------------------------- Coilwyrm (Vrell)

def coil_head(jaw=0.0, glow=1.0):
    """Serpent head: armoured skull, mandibles (open with ``jaw``), crest frills, teal eyes."""
    frill = [(0.32, 0.25), (0.92, -0.05), (0.86, -0.38), (0.36, -0.28)]

    def scene(p):
        q = mirror_x(p)
        mand = em._hinge(q, (0.2, 0.62, 0.0), 0.35 * jaw, axis="z")
        d, m = union(
            (sd_ellipsoid(p, (0, 0.0, 0), (0.48, 0.72, 0.38)), V_SEAM),
            (sd_ellipsoid(p, (0, 0.55, -0.02), (0.3, 0.42, 0.24)), V_BODY),
            (sd_plate(q, frill, -0.02, 0.05, 0.02,
                      taper=lambda x, y: np.clip(1.4 - x, 0.3, 1)), V_DARK),
            k=0.08,
        )
        return union(
            (d, m),
            (sd_capsule(mand, (0.2, 0.62, 0.0), (0.32, 1.02, -0.02), 0.09, 0.02), V_BONE),
            (sd_sphere(q, (0.2, 0.42, 0.24), 0.1), V_EYE),
            (sd_capsule(p, (0, -0.1, 0.34), (0, -0.62, 0.3), 0.06, 0.03), V_BONE),
            (sd_capsule(q, (0.28, -0.2, 0.25), (0.62, -0.78, 0.2), 0.08, 0.015), V_BONE),
            (sd_capsule(q, (0.36, 0.1, 0.2), (0.7, -0.3, 0.12), 0.06, 0.012), V_BONE),
            k=0.03,
        )
    return scene, vrell("coilwyrm", glow)


def coil_segment(glow=1.0):
    """Body segment: rounded chitin ring with dorsal spine, side fins and glow nodes."""
    fin = [(0.48, 0.15), (1.0, -0.18), (0.95, -0.42), (0.48, -0.22)]

    def scene(p):
        q = mirror_x(p)
        d, m = union(
            (sd_ellipsoid(p, (0, 0, 0), (0.62, 0.56, 0.4)), V_SEAM),
            (sd_plate(q, fin, -0.02, 0.04, 0.015,
                      taper=lambda x, y: np.clip(1.5 - x, 0.3, 1)), V_DARK),
            k=0.07,
        )
        return union(
            (d, m),
            (sd_capsule(p, (0, 0.22, 0.36), (0, -0.3, 0.44), 0.07, 0.02), V_BONE),
            (sd_sphere(q, (0.34, 0.02, 0.26), 0.075), V_GLOW),
            k=0.03,
        )
    return scene, vrell("coilwyrm", glow)


def coil_tail(glow=1.0):
    fin = [(0.0, -0.3), (0.55, -0.75), (0.5, -1.02), (0.0, -0.8)]

    def scene(p):
        q = mirror_x(p)
        return union(
            (sd_capsule(p, (0, 0.45, 0), (0, -0.85, 0), 0.4, 0.04), V_SEAM),
            (sd_plate(q, fin, -0.02, 0.035, 0.01), V_DARK),
            (sd_sphere(p, (0, 0.2, 0.3), 0.08), V_GLOW),
            k=0.06,
        )
    return scene, vrell("coilwyrm", glow)


# --------------------------------------------------------------------------- Leviathan (Vrell)

LEVIATHAN_S = 100.0          # native px per model unit
LEV_TAIL = [(0.52, 0.34, 0.26), (0.42, 0.26, 0.19), (0.36, 0.19, 0.12)]   # (length, r0, r1)
LEV_FIN_PIVOT = (0.5, 0.35)  # local pivot of the right pectoral fin on the body
LEV_TAIL_PIVOT = -1.3        # local y of the tail joint on the body
LEV_TURRETS = [(0.3, 0.45), (-0.3, 0.45), (0.28, -0.35), (-0.28, -0.35)]


def lev_mats(glow=1.0):
    """Leviathan materials: the bone role colours with a smooth mottled hide instead of the
    ridged chitin pattern (it is a whale, not an insect)."""
    mats = vrell("leviathan", glow)
    mid = em.hx(em.CHITIN_BASES["bone"][0])

    def mottle(p, n):
        return 0.86 + 0.14 * np.sin(p[:, 0] * 9 + np.sin(p[:, 1] * 7)) * np.cos(p[:, 1] * 5.5)
    mats[V_BODY] = Material(mid, metal=0.2, shininess=70, spec=0.7, pattern=mottle)
    return mats


def leviathan_body(glow=1.0):
    """Whale-like body (head +Y): bone/ivory hide, dorsal plates, four violet dorsal turret
    vents, a blowhole (weak point) and eyes."""
    def scene(p):
        q = mirror_x(p)
        d, m = union(
            (sd_ellipsoid(p, (0, 0.05, 0), (0.62, 1.35, 0.48)), V_BODY),
            (sd_ellipsoid(p, (0, 1.05, 0.0), (0.56, 0.58, 0.42)), V_BODY),
            (sd_ellipsoid(q, (0.42, -0.15, -0.02), (0.22, 0.7, 0.3)), V_SAC),
            k=0.2,
        )
        plates = [(sd_ellipsoid(p, (0, y, 0.4), (0.13, 0.2, 0.07)), V_BONE)
                  for y in (0.62, 0.2, -0.22, -0.64, -1.02)]
        d, m = union((d, m), *plates, k=0.05)
        vents = [(sd_cylinder_z(p, (x, y, 0.3), 0.11, 0.08), V_DARK) for x, y in LEV_TURRETS]
        d, m = union((d, m), *vents, k=0.04)
        glows = [(sd_sphere(p, (x, y, 0.36), 0.07), V_GLOW) for x, y in LEV_TURRETS]
        return union(
            (d, m), *glows,
            (sd_sphere(p, (0, 0.95, 0.42), 0.09), V_EYE),
            (sd_sphere(q, (0.44, 1.2, 0.18), 0.06), V_EYE),
            (sd_capsule(q, (0.0, 1.6, 0.12), (0.3, 1.5, 0.1), 0.05, 0.05), V_DARK),
            (sd_capsule(q, (0.3, 1.5, 0.1), (0.52, 1.2, 0.05), 0.05, 0.04), V_DARK),
            k=0.02,
        )
    return scene, lev_mats(glow)


def leviathan_tail(i, glow=1.0):
    """Tail segment ``i`` (0 = nearest the body): pivot at its front joint, extends to -Y."""
    ln, r0, r1 = LEV_TAIL[i]

    def scene(p):
        return union(
            (sd_capsule(p, (0, 0.0, 0), (0, -ln, 0), r0, r1), V_BODY),
            (sd_capsule(p, (0, -0.05, r0 * 0.8), (0, -ln + 0.05, r1 * 0.8), 0.06, 0.04), V_BONE),
            k=0.04,
        )
    return scene, lev_mats(glow)


def leviathan_fluke(glow=1.0):
    fl = [(0.0, 0.1), (0.3, -0.05), (0.8, -0.42), (0.74, -0.6), (0.3, -0.36), (0.0, -0.28)]

    def scene(p):
        q = mirror_x(p)
        return union(
            (sd_plate(q, fl, 0.0, 0.08, 0.03,
                      taper=lambda x, y: np.clip(1.2 - x, 0.3, 1)), V_BODY),
            (sd_capsule(p, (0, 0.1, 0.02), (0, -0.25, 0.02), 0.14, 0.05), V_BODY),
            (sd_capsule(q, (0.3, -0.12, 0.05), (0.72, -0.5, 0.03), 0.02, 0.01), V_DARK),
            k=0.05,
        )
    return scene, lev_mats(glow)


def leviathan_fin(side=1, glow=1.0):
    """Pectoral fin, pivot at the shoulder; ``side`` +1 = the fish's right (+X)."""
    fin = [(0.0, 0.15), (0.45, 0.08), (0.98, -0.45), (0.88, -0.58), (0.38, -0.28), (0.0, -0.15)]

    def scene(p):
        q = p.copy()
        q[:, 0] = q[:, 0] * side
        return union(
            (sd_plate(q, fin, 0.0, 0.07, 0.03,
                      taper=lambda x, y: np.clip(1.2 - x, 0.3, 1)), V_BODY),
            (sd_capsule(q, (0.1, 0.02, 0.05), (0.85, -0.48, 0.03), 0.03, 0.015), V_DARK),
            k=0.04,
        )
    return scene, lev_mats(glow)


# --------------------------------------------------------------------------- Scuttler (Vrell)

def scuttler(phase=0.0, glow=1.0):
    """Six-legged crab-like ground walker with a tripod gait. ``phase`` in radians. A low,
    flat carapace with long jointed legs that reach well beyond it, so it reads as a walker."""
    base = [(0.22, 0.65), (0.0, 0.0), (-0.22, -0.65)]          # (hip y, outward angle offset)

    def leg_items(p):
        items = []
        for i, (hy, a0) in enumerate(base):
            for side in (1, -1):
                group = (i + (0 if side > 0 else 1)) % 2       # tripod: L0 R1 L2 / R0 L1 R2
                ph = phase + (0.0 if group == 0 else np.pi)
                s = 0.5 * np.sin(ph)
                lift = 0.16 * max(0.0, np.cos(ph))
                ang = a0 + s
                ox, oy = np.cos(ang), np.sin(ang)
                hip = (side * 0.34, hy, 0.08)
                knee = (side * (0.34 + 0.42 * ox), hy + 0.42 * oy, 0.34 + lift)
                foot = (side * (0.34 + 0.86 * ox), hy + 0.86 * oy, -0.08 + lift)
                items += [(sd_capsule(p, hip, knee, 0.085, 0.07), V_BODY),
                          (sd_sphere(p, knee, 0.075), V_DARK),
                          (sd_capsule(p, knee, foot, 0.065, 0.02), V_BONE)]
        return items

    def scene(p):
        q = mirror_x(p)
        d, m = union(
            (sd_ellipsoid(p, (0, -0.05, 0.08), (0.44, 0.4, 0.17)), V_SEAM),
            (sd_ellipsoid(p, (0, 0.38, 0.05), (0.24, 0.17, 0.12)), V_BODY),
            (sd_ellipsoid(p, (0, -0.4, 0.04), (0.26, 0.16, 0.1)), V_DARK),
            k=0.05,
        )
        d, m = union((d, m), *leg_items(p), k=0.025)
        return union(
            (d, m),
            (sd_capsule(q, (0.1, 0.48, 0.05), (0.18, 0.7, 0.0), 0.05, 0.015), V_BONE),
            (sd_sphere(q, (0.12, 0.4, 0.15), 0.06), V_GLOW),
            (sd_sphere(p, (0, 0.3, 0.18), 0.055), V_EYE),
            k=0.02,
        )
    return scene, vrell("scuttler", glow)


# --------------------------------------------------------------------------- Whirl Seed / Mote

def whirl_seed(glow=1.0):
    """Radial seed pod: five curved petal blades around a glowing core (5-fold symmetric)."""
    petal = [(0.12, 0.05), (0.55, 0.38), (1.0, 0.24), (0.92, 0.02), (0.3, -0.12)]

    def scene(p):
        items = [(sd_plate(rotate_z(p, a), petal, 0.0, 0.06, 0.02,
                           taper=lambda x, y: np.clip(1.2 - 0.8 * x, 0.3, 1)),
                  V_BODY if k % 2 == 0 else V_DARK)
                 for k, a in enumerate(np.linspace(0, 2 * np.pi, 5, endpoint=False))]
        d, m = union(*items, k=0.04)
        return union(
            (d, m),
            (sd_sphere(p, (0, 0, 0.05), 0.32), V_SEAM),
            (sd_sphere(p, (0, 0, 0.25), 0.16), V_EYE),
            k=0.05,
        )
    return scene, vrell("whirl-seed", glow)


def mote(glow=1.0):
    """Tiny ember-like mote: spiky rust husk with three swept fins and a small crimson slit
    (radial; the body dominates so it never reads as a round bullet)."""
    fin = [(0.2, 0.1), (0.95, 0.45), (0.85, 0.05), (0.3, -0.15)]

    def scene(p):
        fins = [(sd_plate(rotate_z(p, a), fin, 0.0, 0.07, 0.02), V_DARK)
                for a in np.linspace(0, 2 * np.pi, 3, endpoint=False)]
        spikes = [(sd_capsule(rotate_z(p, a), (0, 0.3, 0.05), (0, 0.8, 0.1), 0.1, 0.02), V_BONE)
                  for a in np.linspace(1.05, 2 * np.pi + 1.05, 3, endpoint=False)]
        d, m = union((sd_sphere(p, (0, 0, 0), 0.46), V_BODY), *fins, *spikes, k=0.07)
        return union((d, m), (sd_ellipsoid(p, (0, 0, 0.36), (0.1, 0.24, 0.1)), V_GLOW), k=0.03)
    return scene, vrell("mote", glow)


# --------------------------------------------------------------------------- Ascendancy

def warden_hull(tread=0.0, glow=1.0):
    """Tracked tank hull (forward +Y): angular armoured body, two tread boxes (animated with
    ``tread`` 0..1), engine deck, white hazard stripes, red lights."""
    def scene(p):
        q = mirror_x(p)
        d, m = union(
            (sd_plate(q, [(0.0, 0.78), (0.42, 0.62), (0.46, -0.7), (0.0, -0.76)], 0.12, 0.16, 0.0,
                      taper=_roof(1.2)), A_HULL),
            (sd_box(q, (0.62, 0.0, 0.08), (0.16, 0.86, 0.12), 0.03), A_TREAD),
        )
        return union(
            (d, m),
            (sd_box(p, (0, -0.52, 0.26), (0.3, 0.16, 0.04), 0.0), A_FACET),
            (sd_box(q, (0.3, 0.55, 0.26), (0.06, 0.12, 0.02), 0.0), A_GUN),
            (sd_box(q, (0.44, -0.2, 0.2), (0.02, 0.5, 0.03), 0.0), A_GOLD),
            (sd_sphere(q, (0.4, 0.74, 0.18), 0.05), A_RED),
            (sd_cylinder_z(q, (0.25, -0.82, 0.12), 0.07, 0.05), A_ENGINE),
        )
    return scene, asc("warden-tank", glow, tread)


def warden_turret(glow=1.0):
    """Tank turret (pivot at the ring centre, barrel +Y)."""
    hexa = [(np.cos(a) * 0.46, np.sin(a) * 0.46 - 0.05)
            for a in np.linspace(np.pi / 6, 2 * np.pi + np.pi / 6, 6, endpoint=False)]

    def scene(p):
        return union(
            (sd_plate(p, hexa, 0.4, 0.14, 0.0,
                      taper=lambda x, y: np.clip(1.1 - 1.2 * np.sqrt(x * x + y * y), 0.4, 1)),
             A_HULL),
            (sd_box(p, (0, 0.66, 0.44), (0.08, 0.42, 0.06), 0.02), A_FACET),
            (sd_box(p, (0, 1.06, 0.44), (0.12, 0.06, 0.08), 0.0), A_GOLD),
            (sd_cylinder_z(p, (0, -0.05, 0.3), 0.5, 0.03), A_GOLD),
            (sd_sphere(p, (0.14, 0.12, 0.54), 0.05), A_RED),
            (sd_box(p, (-0.14, -0.16, 0.54), (0.1, 0.08, 0.02), 0.0), A_GUN),
        )
    return scene, asc("warden-tank", glow)


def strider_legs(phase=0.0, glow=1.0):
    """Bipedal walker legs and pelvis (forward +Y), walk cycle ``phase`` (radians). Knees bow
    outward so the legs read from above; big clawed foot pads."""
    def scene(p):
        items = [(sd_box(p, (0, 0, 0.55), (0.28, 0.2, 0.14), 0.05), A_HULL)]
        for side in (1, -1):
            ph = phase + (0.0 if side > 0 else np.pi)
            fy = 0.55 * np.sin(ph)
            lift = 0.2 * max(0.0, np.cos(ph))
            hip = (side * 0.3, 0.0, 0.52)
            knee = (side * 0.68, 0.1 + fy * 0.45, 0.4 + lift)
            foot = (side * 0.5, fy, 0.08 + lift)
            items += [(sd_capsule(p, hip, knee, 0.16, 0.13), A_FACET),
                      (sd_capsule(p, knee, foot, 0.13, 0.1), A_HULL),
                      (sd_sphere(p, knee, 0.15), A_GOLD),
                      (sd_ellipsoid(p, (foot[0], foot[1] + 0.02, foot[2] - 0.04),
                                    (0.2, 0.26, 0.08)), A_HULL)]
            for dx in (-0.13, 0.0, 0.13):
                items.append((sd_capsule(p, (foot[0] + dx, foot[1] + 0.12, foot[2] - 0.04),
                                         (foot[0] + dx * 1.5, foot[1] + 0.4, foot[2] - 0.06),
                                         0.06, 0.02), A_GOLD if dx == 0 else A_FACET))
        return union(*items, k=0.02)
    return scene, asc("strider", glow)


def strider_torso(glow=1.0):
    """Walker torso and arm cannons (pivot at the waist, aims along +Y)."""
    body = [(0.0, 0.52), (0.32, 0.34), (0.4, -0.2), (0.22, -0.46), (0.0, -0.4)]

    def scene(p):
        q = mirror_x(p)
        return union(
            (sd_plate(q, body, 0.75, 0.2, 0.0, taper=_roof(1.6)), A_HULL),
            (sd_box(q, (0.52, 0.02, 0.8), (0.14, 0.2, 0.1), 0.03), A_GOLD),
            (sd_box(q, (0.56, 0.3, 0.86), (0.12, 0.3, 0.1), 0.03), A_GUN),
            (sd_box(q, (0.56, 0.64, 0.86), (0.09, 0.06, 0.08), 0.0), A_FACET),
            (sd_box(q, (0.56, 0.1, 0.98), (0.04, 0.16, 0.02), 0.0), A_GOLD),
            (sd_plate(p, [(0, 0.46), (0.08, 0.26), (0, 0.18), (-0.08, 0.26)], 0.98, 0.03, 0.0),
             A_GLASS),
            (sd_sphere(p, (0, 0.36, 0.98), 0.05), A_RED),
            (sd_cylinder_z(q, (0.14, -0.36, 0.86), 0.07, 0.05), A_ENGINE),
        )
    return scene, asc("strider", glow)


def buzzsaw(glow=1.0):
    """Radially symmetric spinning blade drone: six hooked gunmetal blades around a gold hub
    with a red eye (6-fold symmetric)."""
    blade = [(0.24, 0.12), (0.72, 0.42), (1.05, 0.3), (0.98, 0.12), (0.34, -0.1)]

    def scene(p):
        items = [(sd_plate(rotate_z(p, a), blade, 0.0, 0.05, 0.0,
                           taper=lambda x, y: np.clip(1.2 - 0.9 * x, 0.25, 1)), A_GUN)
                 for a in np.linspace(0, 2 * np.pi, 6, endpoint=False)]
        d, m = union(*items)
        # gold cutting edges on the leading edge of each blade
        return union(
            (d, m),
            (sd_cylinder_z(p, (0, 0, 0.05), 0.4, 0.1), A_HULL),
            (sd_cylinder_z(p, (0, 0, 0.12), 0.28, 0.08), A_GOLD),
            (sd_sphere(p, (0, 0, 0.18), 0.12), A_RED),
            k=0.01,
        )
    return scene, asc("buzzsaw-drone", glow)


def rail_car(head=False, glow=1.0):
    """Rail Serpent car: armoured drone module (forward +Y); the head car has a sensor prow."""
    def scene(p):
        q = mirror_x(p)
        items = [(sd_plate(q, [(0.0, 0.62), (0.44, 0.44), (0.5, -0.5), (0.0, -0.62)], 0.1, 0.16,
                           0.0, taper=_roof(1.3)), A_HULL),
                 (sd_box(q, (0.56, 0.0, 0.06), (0.1, 0.4, 0.06), 0.02), A_FACET),
                 (sd_box(p, (0, 0.0, 0.26), (0.08, 0.4, 0.03), 0.0), A_GOLD),
                 (sd_capsule(p, (0, -0.55, 0.08), (0, -0.85, 0.08), 0.06, 0.06), A_GUN)]
        if head:
            items += [(sd_plate(q, [(0.0, 1.05), (0.3, 0.6), (0.0, 0.5)], 0.14, 0.08, 0.0), A_GUN),
                      (sd_sphere(p, (0, 0.72, 0.24), 0.08), A_RED)]
        else:
            items += [(sd_sphere(q, (0.3, 0.2, 0.26), 0.05), A_RED)]
        return union(*items)
    return scene, asc("rail-serpent", glow)
