"""SDF models for the round 05 follow-up: animal-like Vrell ground beasts (the Vrell had only
insect-like and crab-like ground units). Built facing +Y like render.archetype_models; render
them with ``enemy_rigs.ModelSpaceAngleSprites`` so seams and plates turn with the body.

Ravager    medium pack hunter: lean hound/raptor quadruped of chitin and sinew, long tail,
           glowing maw between hinged jaws. Poses: ``run`` (8-frame gallop) and ``leap``.
Shellback  large armoured tortoise/armadillo beast with banded domed shell plates and a
           spore-mortar vent on its back. Poses: ``walk`` (6-frame cycle), ``tuck`` (legs and
           head pulled in) and ``ball`` (curled up, rolling about its local X axis).
"""
import numpy as np

from . import enemy_models as em
from .archetype_models import vrell
from .enemy_models import V_BODY, V_BONE, V_DARK, V_EYE, V_GLOW, V_SAC, V_SEAM
from .sdf import (Material, mirror_x, rotate_x, rotate_z, sd_capsule, sd_cylinder_z,
                  sd_ellipsoid, sd_sphere, union)

em.ROLE_SCHEMES.update({
    "ravager":   ("rust", "teal", "teal"),     # fast attacker, contact (pounce)
    "shellback": ("slate", "lime", "lime"),    # heavy ground unit, area denial (spore mortar)
})


def _hinge_z(q, pivot, angle):
    pv = np.asarray(pivot, dtype=np.float64)
    return rotate_z(q - pv, angle) + pv


# --------------------------------------------------------------------------- Ravager

def ravager(phase=0.0, pose="run", glow=1.0):
    """Lean four-legged pack hunter (forward +Y). ``phase`` drives the rotary gallop; the
    ``leap`` pose stretches front legs forward, hind legs back and opens the jaws wide."""
    leap = pose == "leap"
    ext = 1.0 if leap else np.cos(phase)
    chest_y = 0.3 + 0.05 * ext
    hip_y = -0.3 - 0.05 * ext
    jaw = 0.55 if leap else 0.18 + 0.1 * np.sin(phase)
    legs = [(1, chest_y, 0.0, True), (-1, chest_y, 0.4, True),
            (1, hip_y, np.pi, False), (-1, hip_y, np.pi + 0.4, False)]

    def scene(p):
        q = mirror_x(p)
        d, m = union(
            (sd_ellipsoid(p, (0, (chest_y + hip_y) / 2 + 0.04, 0.15), (0.23, 0.56, 0.19)), V_BODY),
            (sd_ellipsoid(p, (0, chest_y, 0.17), (0.27, 0.26, 0.2)), V_BODY),
            (sd_ellipsoid(q, (0.13, hip_y, 0.14), (0.14, 0.2, 0.15)), V_BODY),
            (sd_capsule(p, (0, chest_y + 0.16, 0.2), (0, chest_y + 0.42, 0.18), 0.14, 0.1), V_BODY),
            (sd_capsule(p, (0, chest_y + 0.1, 0.33), (0, hip_y - 0.05, 0.3), 0.05, 0.04), V_DARK),
            k=0.18,
        )
        hy = chest_y + 0.55
        # head: skull, hinged side jaws, glowing maw between them, eyes, brow horns
        jl = _hinge_z(q, (0.07, hy + 0.02, 0.0), -jaw)
        d, m = union(
            (d, m),
            (sd_ellipsoid(p, (0, hy, 0.17), (0.16, 0.19, 0.12)), V_BODY),
            (sd_capsule(jl, (0.08, hy + 0.02, 0.1), (0.06, hy + 0.36, 0.08), 0.07, 0.028), V_BONE),
            (sd_capsule(q, (0.1, hy - 0.06, 0.24), (0.2, hy - 0.3, 0.26), 0.04, 0.01), V_BONE),
            k=0.04,
        )
        d, m = union(
            (d, m),
            (sd_ellipsoid(p, (0, hy + 0.2, 0.1), (0.05 + 0.08 * jaw, 0.12, 0.06)), V_GLOW),
            (sd_sphere(q, (0.1, hy + 0.04, 0.24), 0.045), V_EYE),
            k=0.02,
        )
        # dorsal spines
        spines = [(sd_capsule(p, (0, y, 0.3), (0, y - 0.1, 0.36), 0.035, 0.01), V_BONE)
                  for y in (chest_y + 0.1, chest_y - 0.12, hip_y + 0.12, hip_y - 0.06)]
        d, m = union((d, m), *spines, k=0.02)
        # legs
        items = []
        for sx, sy0, ph0, front in legs:
            if leap:
                fy, lift = (0.55 if front else -0.58), 0.06
            else:
                ph = phase + ph0
                fy, lift = 0.42 * np.sin(ph), 0.14 * max(0.0, np.cos(ph))
            r_up = 0.085 if front else 0.11
            sh = (sx * 0.2, sy0, 0.12)
            knee = (sx * 0.44, sy0 + fy * 0.5 + (0.05 if front else -0.1), 0.22 + lift)
            foot = (sx * 0.4, sy0 + fy, -0.02 + lift)
            items += [(sd_capsule(p, sh, knee, r_up, 0.065), V_DARK),
                      (sd_capsule(p, knee, foot, 0.055, 0.03), V_BONE)]
            for dx in (-0.04, 0.04):
                items.append((sd_capsule(p, foot, (foot[0] + dx + sx * 0.02, foot[1] + 0.09,
                                                    foot[2] - 0.02), 0.025, 0.008), V_BONE))
        d, m = union((d, m), *items, k=0.03)
        # tail: a chain of shrinking capsules swinging with the gallop
        pts = []
        for i in range(6):
            sway = 0.0 if leap else 0.2 * np.sin(phase - 0.8 * i) * (i / 5)
            pts.append((sway, hip_y - 0.16 - 0.11 * i, 0.15 - 0.015 * i))
        tail = [(sd_capsule(p, pts[i], pts[i + 1], 0.11 - 0.018 * i, 0.095 - 0.018 * i),
                 V_BODY if i % 2 == 0 else V_DARK) for i in range(5)]
        return union((d, m), *tail, k=0.04)
    return scene, vrell("ravager", glow)


# --------------------------------------------------------------------------- Shellback

def _bands(m, q, mask, freq=2.4, gap=0.07):
    """Banded armadillo plates computed from model coordinates: alternating plate tones with
    thin glowing gaps (lime) between them."""
    u = (q[:, 1] + 3.0) * freq
    f = u % 1.0
    plate = np.where((np.floor(u) % 2) == 0, V_BODY, V_DARK)
    out = np.where(mask & (f < gap), V_GLOW, np.where(mask, plate, m))
    return out


# tortoise-like scutes on the dome: (x, y, radius); the vent sits where the rear central
# scutes would be
_SCUTES = ([(0.0, 0.5, 0.17), (0.0, 0.16, 0.18)]
           + [(sx * x, y, r) for sx in (1, -1)
              for x, y, r in ((0.32, 0.44, 0.15), (0.38, 0.12, 0.16), (0.37, -0.2, 0.16),
                              (0.3, -0.5, 0.15), (0.14, -0.66, 0.12))]
           + [(0.6 * np.cos(a), 0.74 * np.sin(a), 0.09)
              for a in np.linspace(0, 2 * np.pi, 18, endpoint=False)])


def shellback(phase=0.0, pose="walk", roll=0.0, pulse=1.0, glow=1.0):
    """Large armoured beast (forward +Y). ``walk``: 6-frame diagonal-pair gait; ``tuck``:
    legs and head pulled in; ``ball``: curled into a banded ball rolling by ``roll`` radians
    about its local X axis. ``pulse`` swells the spore-mortar vent before a shot."""
    if pose == "ball":
        return _shellback_ball(roll, glow)
    tuck = 1.0 if pose == "tuck" else 0.0

    def scene(p):
        q = mirror_x(p)
        dome = (sd_ellipsoid(p, (0, 0, 0.16), (0.64, 0.8, 0.44)), V_SAC)
        skirt = (sd_ellipsoid(p, (0, 0, 0.05), (0.72, 0.88, 0.13)), V_SEAM)
        d, m = union(dome, skirt, k=0.03)
        scutes = []
        for i, (x, y, r) in enumerate(_SCUTES):
            zz = 0.16 + 0.44 * np.sqrt(max(0.0, 1 - (x / 0.64) ** 2 - (y / 0.8) ** 2))
            scutes.append((sd_ellipsoid(p, (x, y, zz - 0.03), (r, r * 0.9, 0.07)),
                           V_BODY if i % 3 else V_DARK))
        d, m = union((d, m), *scutes, k=0.015)
        # spore-mortar vent on the rear of the back
        d, m = union(
            (d, m),
            (sd_cylinder_z(p, (0, -0.3, 0.56), 0.21, 0.1), V_DARK),
            (sd_sphere(p, (0, -0.3, 0.64), 0.13 * pulse), V_SAC),
            (sd_sphere(p, (0, -0.3, 0.7), 0.07 * pulse), V_GLOW),
            k=0.02,
        )
        # head with beak (retracts when tucked) and short tail
        hy = 0.92 - 0.22 * tuck
        d, m = union(
            (d, m),
            (sd_ellipsoid(p, (0, hy, 0.06), (0.26, 0.24, 0.16)), V_SEAM),
            (sd_capsule(p, (0, hy + 0.12, 0.08), (0, hy + 0.32, 0.02), 0.11, 0.025), V_BONE),
            (sd_sphere(q, (0.14, hy + 0.07, 0.18), 0.05), V_EYE),
            (sd_capsule(p, (0, -0.84, 0.04), (0, -1.02 + 0.14 * tuck, 0.0), 0.08, 0.02), V_SEAM),
            k=0.04,
        )
        # four stubby elephantine legs at the corners, diagonal pairs in phase
        items = []
        for sx, sy, ph0 in ((1, 0.5, 0.0), (-1, -0.5, 0.0), (-1, 0.5, np.pi), (1, -0.5, np.pi)):
            ph = phase + ph0
            fy = 0.0 if tuck else 0.13 * np.sin(ph)
            lift = 0.0 if tuck else 0.07 * max(0.0, np.cos(ph))
            out = 0.72 - 0.24 * tuck
            foot = (sx * out, sy + fy, -0.02 + lift)
            items += [(sd_ellipsoid(p, foot, (0.19, 0.21, 0.16)), V_SEAM),
                      (sd_capsule(p, (foot[0], foot[1] + 0.12, foot[2] - 0.04),
                                  (foot[0], foot[1] + 0.2, foot[2] - 0.06), 0.05, 0.02), V_BONE),
                      (sd_capsule(p, (foot[0] + 0.07, foot[1] + 0.1, foot[2] - 0.04),
                                  (foot[0] + 0.1, foot[1] + 0.17, foot[2] - 0.06), 0.04, 0.015),
                       V_BONE),
                      (sd_capsule(p, (foot[0] - 0.07, foot[1] + 0.1, foot[2] - 0.04),
                                  (foot[0] - 0.1, foot[1] + 0.17, foot[2] - 0.06), 0.04, 0.015),
                       V_BONE)]
        return union((d, m), *items, k=0.03)
    return scene, _shell_mats(glow)


def _shellback_ball(roll, glow):
    """Curled up: a banded ball rolling about its local X axis (bands are slices around that
    axis, so rolling is visible from above)."""
    c = np.array([0.0, 0.0, 0.2])
    knob_pos = [(sx * 0.3, np.cos(a) * 0.62, np.sin(a) * 0.62)
                for a in np.linspace(0, 2 * np.pi, 10, endpoint=False) for sx in (1, -1)]

    def scene(p):
        q = rotate_x(p - c, roll)
        d = sd_sphere(q, (0, 0, 0), 0.7)
        phi = np.arctan2(q[:, 2], q[:, 1])
        u = (phi / (2 * np.pi) + 1.0) * 10
        f = u % 1.0
        m = np.where(f < 0.1, V_GLOW, np.where((np.floor(u) % 2) == 0, V_BODY, V_DARK))
        knobs = [(sd_sphere(q, k, 0.07), V_BONE) for k in knob_pos]
        return union((d, m), *knobs, k=0.02)
    return scene, _shell_mats(glow)


def _shell_mats(glow):
    """Shellback materials: slate plates without screen-space patterns (bands come from the
    geometry), lime glow in the gaps, a lime-veined vent."""
    mats = vrell("shellback", glow)
    mid, dark, bone = (em.hx(c) for c in em.CHITIN_BASES["slate"])
    mats[V_BODY] = Material(mid, metal=0.35, shininess=80, spec=0.9)
    mats[V_DARK] = Material(em._mix(mid, dark, 0.45), metal=0.35, shininess=80, spec=0.8)
    mats[V_SEAM] = Material(em._mix(mid, dark, 0.65), metal=0.2, shininess=40, spec=0.5,
                            pattern=lambda p, n: 0.85 + 0.15 * np.cos(p[:, 0] * 40) ** 2)
    return mats
