"""SDF models for concept round 06: the Act 2 introductions and the round 05 roster additions
that had no art yet. Built facing +Y (forward) like ``render.archetype_models``; render them
with ``enemy_rigs.ModelSpaceAngleSprites`` so seams, plates and stripes turn with the body.
Parts of rigs (chain segments, ring segments, turret heads) have their pivot at the origin.

Vrell (body plans beyond insects, per the enemy variety rules):
  Creeper         long low six-legged salamander/gecko with a fan gland on its head
  Hive Node       grown spawner mound: fleshy lobes, armour plates, an iris that opens
  Wraith          manta-ray-like ghost with veil tendrils (cloaks)
  Lamprey         small eel with a round sucker mouth ring
  Driftjelly      floating jellyfish mine: pulsing bell, frilled rim, trailing tentacles
  Reef Spitter    barnacle gun (aims) on a floating biomass raft (static)
  Skimmer         flying-fish-like skiff with outrigger fins
  Threadcrawler   centipede chain: head, legged segments, tail with cerci
  Dust Devil      radial spinning vortex organism with a core that opens at the top of a spin
  Spiral Nautilus coiled-shell cephalopod: shell (spins) + tentacle crown (uncoiled)
Ascendancy:
  Halo Platform   rotating ring of six turret segments around a shielded core

Colours follow the adopted role colours (design/enemies/README.md "Role colours"); the new units
are added to ``enemy_models.ROLE_SCHEMES`` / ``ASC_ACCENTS`` here.
"""
import numpy as np

from . import enemy_models as em
from .archetype_models import asc, vrell
from .enemy_models import (A_FACET, A_GLASS, A_GOLD, A_GUN, A_HULL, A_RED, V_BODY, V_BONE,
                           V_DARK, V_EYE, V_GLOW, V_SAC, V_SEAM)
from .sdf import (Material, mirror_x, rotate_x, rotate_z, sd_box, sd_capsule, sd_cylinder_z,
                  sd_ellipsoid, sd_plate, sd_sphere, subtract, union)

em.ROLE_SCHEMES.update({
    "creeper":         ("slate", "violet", "violet"),      # walking ground gunner (5-way fan)
    "hive-node":       ("teal-black", "teal", "teal"),     # spawner
    "wraith":          ("rust", "violet", "violet"),       # ambusher diving in from behind, bursts
    "lamprey":         ("rust", "teal", "teal"),           # fast chaser, contact / latch
    "driftjelly":      ("olive", "lime", "lime"),          # floating mine, area denial
    "reef-spitter":    ("slate", "violet", "violet"),      # rooted turret, 3-way fan
    "skimmer":         ("rust", "violet", "violet"),       # fast skiff, aimed shots
    "threadcrawler":   ("olive", "lime", "lime"),          # chain spitting spores (area)
    "dust-devil":      ("teal-black", "lime", "lime"),     # vortex spitting grit (area)
    "spiral-nautilus": ("bone", "violet", "violet"),       # shelled gunner, spiral of shots
})
em.ASC_ACCENTS.update({
    "halo-platform": "e8e4f0",   # white
})


def _hinge_z(q, pivot, angle):
    pv = np.asarray(pivot, dtype=np.float64)
    return rotate_z(q - pv, angle) + pv


def _stripes(freq, depth=0.35, axis=1):
    def f(p, n):
        return (1 - depth) + depth * (np.cos(p[:, axis] * freq) > 0.3)
    return f


# --------------------------------------------------------------------------- Creeper

def creeper(phase=0.0, glow=1.0):
    """Long, low six-legged salamander (forward +Y): a wide flat gecko head on a narrow neck
    with a frilled fan gland, a sinuous body and a long tail swaying with the gait, three
    splayed leg pairs (front legs reach forward, hind legs back) with pale toe pads."""
    n = 9
    spine = []
    for i in range(n):
        y = 0.5 - 0.2 * i
        sway = 0.1 * np.sin(phase - 0.8 * i) * (0.3 + i / (n - 1))
        spine.append((sway, y))
    radii = (0.13, 0.24, 0.28, 0.27, 0.22, 0.15, 0.1, 0.065, 0.035)

    def scene(p):
        hx0 = spine[0][0]
        parts = [(sd_ellipsoid(p, (hx0, 0.8, 0.09), (0.31, 0.27, 0.12)), V_BODY),   # flat head
                 (sd_ellipsoid(p, (hx0, 1.0, 0.07), (0.2, 0.14, 0.08)), V_BODY)]    # snout
        for i in range(n - 1):
            (x0, y0), (x1, y1) = spine[i], spine[i + 1]
            parts.append((sd_capsule(p, (x0, y0, 0.1), (x1, y1, 0.08), radii[i], radii[i + 1]),
                          V_BODY))
        d, m = union(*parts, k=0.07)
        # fan gland: a frilled crest behind the eyes with five glowing pores
        frill = [(0.0, 0.84), (0.28, 0.9), (0.42, 0.72), (0.26, 0.6), (0.0, 0.62)]
        pores = [(sd_sphere(p, (hx0 + 0.11 * k, 0.74 - 0.025 * abs(k), 0.2), 0.045), V_GLOW)
                 for k in (-2, -1, 0, 1, 2)]
        d, m = union((d, m),
                     (sd_plate(_shift_x(p, hx0), frill, 0.15, 0.022, 0.01), V_DARK),
                     (sd_sphere(_shift_x(p, hx0), (0.2, 0.92, 0.15), 0.06), V_EYE),
                     *pores, k=0.02)
        # pale dorsal studs in a double row
        studs = [(sd_sphere(_shift_x(p, spine[i][0]), (0.09, spine[i][1] - 0.08, 0.3 - 0.03 * i),
                            0.045 - 0.004 * i), V_BONE) for i in range(1, 6)]
        d, m = union((d, m), *studs, k=0.02)
        legs = []
        for j, (idx, reach) in enumerate(((1, 0.16), (3, 0.0), (5, -0.16))):
            bx, by = spine[idx]
            for side in (1, -1):
                ph = phase + (0 if (j + (side > 0)) % 2 else np.pi)
                swing = 0.18 * np.sin(ph)
                lift = 0.08 * max(0.0, np.cos(ph))
                hip = (bx + side * 0.2, by, 0.08)
                knee = (bx + side * 0.5, by + reach * 0.5 + swing * 0.4, 0.2 + lift)
                foot = (bx + side * 0.66, by + reach + swing, 0.0 + lift)
                legs += [(sd_capsule(p, hip, knee, 0.085, 0.06), V_DARK),
                         (sd_capsule(p, knee, foot, 0.055, 0.04), V_DARK),
                         (sd_ellipsoid(p, foot, (0.09, 0.09, 0.035)), V_BONE)]
                for dx in (-0.06, 0.0, 0.06):     # splayed toes
                    tip = (foot[0] + side * 0.07 + dx * 0.5, foot[1] + dx + reach * 0.3,
                           foot[2])
                    legs.append((sd_capsule(p, foot, tip, 0.025, 0.015), V_BONE))
        return union((d, m), *legs, k=0.03)
    mats = vrell("creeper", glow)
    return scene, mats


def _shift_x(p, dx):
    """Mirror about the vertical line x = dx (for parts that sway with the head)."""
    r = p.copy()
    r[:, 0] = np.abs(r[:, 0] - dx)
    return r


# --------------------------------------------------------------------------- Hive Node

def hive_node(iris=0.0, pulse=0.0, glow=1.0):
    """Radial spawner mound: a ring of fleshy lobes around a central iris (``iris`` 0 closed,
    1 open), bone armour plates (hardened), polyps and root tendrils spreading outwards.
    ``pulse`` (0..1) swells the lobes."""
    s = 1.0 + 0.05 * pulse

    def scene(p):
        r = np.sqrt(p[:, 0] ** 2 + p[:, 1] ** 2)
        a = np.arctan2(p[:, 1], p[:, 0])
        # 7 lobes: fold the angle into one sector
        k = 7
        sec = 2 * np.pi / k
        af = (a + sec / 2) % sec - sec / 2
        lp = np.stack([r * np.cos(af), r * np.sin(af), p[:, 2]], axis=-1)
        d, m = union(
            (sd_ellipsoid(p, (0, 0, 0.0), (0.66 * s, 0.66 * s, 0.34)), V_SAC),
            (sd_ellipsoid(lp, (0.5, 0.0, 0.1), (0.3 * s, 0.24 * s, 0.26 * s)), V_BODY),
            k=0.12,
        )
        # armour plates between the lobes (folded at half a sector)
        ap = np.stack([r * np.cos(af + sec / 2), r * np.sin(af + sec / 2), p[:, 2]], axis=-1)
        ap2 = np.stack([r * np.cos(((a) % sec) - sec / 2), r * np.sin(((a) % sec) - sec / 2),
                        p[:, 2]], axis=-1)
        d, m = union((d, m),
                     (sd_ellipsoid(ap2, (0.62, 0.0, 0.16), (0.16, 0.12, 0.2)), V_BONE),
                     (sd_capsule(ap2, (0.7, 0.0, 0.28), (0.98, 0.0, 0.42), 0.06, 0.012), V_BONE),
                     k=0.04)
        # root tendrils on the ground, curling outwards between the lobes
        tl = np.stack([r * np.cos(af), r * np.sin(af), p[:, 2]], axis=-1)
        bend = 0.18 * np.clip(tl[:, 0] - 0.7, 0, None)
        tl2 = tl.copy()
        tl2[:, 1] = tl[:, 1] - bend
        d, m = union((d, m), (sd_capsule(tl2, (0.7, 0.0, -0.05), (1.12, 0.0, -0.09), 0.08,
                                         0.02), V_DARK), k=0.06)
        # central iris: a dark crater whose petals retract when open, glowing throat
        open_r = 0.08 + 0.22 * iris
        crater = sd_cylinder_z(p, (0, 0, 0.3), open_r, 0.2)
        d, m = subtract((d, m), crater)
        petals = []
        for j in range(5):
            ang = j * 2 * np.pi / 5
            c, sn = np.cos(ang), np.sin(ang)
            r0 = 0.12 + 0.22 * iris
            petals.append((sd_capsule(p, (c * (r0 + 0.12), sn * (r0 + 0.12), 0.32),
                                      (c * r0, sn * r0, 0.36), 0.07, 0.03), V_DARK))
        d, m = union((d, m), *petals, k=0.02)
        d, m = union((d, m), (sd_sphere(p, (0, 0, 0.12), 0.14 + 0.12 * iris), V_GLOW), k=0.0)
        # small polyps (spawn buds) on the lobes
        buds = [(sd_sphere(lp, (0.58, 0.1, 0.34 * s), 0.06), V_EYE)]
        return union((d, m), *buds, k=0.02)
    return scene, vrell("hive-node", glow)


# --------------------------------------------------------------------------- Wraith

def wraith(phase=0.0, glow=1.0):
    """Manta-ray-like ghost (forward +Y): wide membrane wings rippling with ``phase``, a ridged
    spine, two violet eye glands, a whip tail and four veil tendrils trailing behind."""
    def scene(p):
        q = mirror_x(p)
        x = q[:, 0]
        wave = 0.07 * np.sin(phase + x * 3.2) * np.clip(x - 0.15, 0, None) * 2.2
        qw = q.copy()
        qw[:, 2] = q[:, 2] - wave
        wing = [(0.0, 0.62), (0.32, 0.5), (0.86, 0.12), (1.08, -0.18), (0.82, -0.26),
                (0.4, -0.3), (0.12, -0.5), (0.0, -0.52)]
        d, m = union(
            (sd_plate(qw, wing, 0.05, 0.07, 0.03,
                      taper=lambda xx, yy: np.clip(1.25 - xx, 0.25, 1.0)), V_SEAM),
            (sd_ellipsoid(p, (0, 0.08, 0.1), (0.22, 0.6, 0.14)), V_BODY),
            k=0.08,
        )
        d, m = union(
            (d, m),
            (sd_sphere(q, (0.14, 0.5, 0.18), 0.06), V_EYE),
            (sd_capsule(p, (0, 0.4, 0.22), (0, -0.4, 0.22), 0.04, 0.02), V_BONE),
            (sd_ellipsoid(q, (0.1, 0.66, 0.06), (0.06, 0.12, 0.05)), V_DARK),   # cephalic lobes
            k=0.03,
        )
        tail = []
        for i in range(5):
            sw0 = 0.07 * np.sin(phase - 0.8 * i) * i / 4
            sw1 = 0.07 * np.sin(phase - 0.8 * (i + 1)) * (i + 1) / 4
            tail.append((sd_capsule(p, (sw0, -0.48 - 0.13 * i, 0.08), (sw1, -0.61 - 0.13 * i,
                                                                      0.06), 0.035, 0.025),
                         V_DARK))
        veils = []
        for side in (1, -1):
            for xo in (0.34, 0.62):
                pts = [(side * xo + side * 0.05 * np.sin(phase - 0.9 * j), -0.25 - 0.14 * j,
                        0.04) for j in range(5)]
                veils += [(sd_capsule(p, pts[j], pts[j + 1], 0.04 - 0.006 * j,
                                      0.034 - 0.006 * j), V_SAC) for j in range(4)]
        return union((d, m), *tail, *veils, k=0.03)
    mats = vrell("wraith", glow)
    mid, dark, bone = (em.hx(c) for c in em.CHITIN_BASES["rust"])
    gl = np.array(em.hx(em.GLOWS["violet"]))
    # membrane: dark translucent-looking rust with violet edge veins
    vein = np.array((0.3, 0.32, 1.0))            # blue-violet: never the magenta bullet hue
    veins = _wing_veins()
    mats[V_SEAM] = Material(em._mix(mid, dark, 0.45), metal=0.2, shininess=60, spec=0.6,
                            # the membrane darkens under a vein so the glow reads violet, not pink
                            pattern=lambda pp, n: (0.85 + 0.15 * np.cos(pp[:, 0] * 14) ** 2)
                            * (1.0 - 0.8 * veins(pp, n)),
                            emission=tuple(vein * 0.9 * glow), emission_pattern=veins)
    return scene, mats


def _wing_veins(width=0.05):
    def f(p, n):
        x, y = np.abs(p[:, 0]), p[:, 1]
        ang = np.arctan2(y, x)
        v = np.abs(((ang * 5.0) % 1.0) - 0.5) < width * 3
        return np.where(v & (x > 0.3), 1.0, 0.0)
    return f


# --------------------------------------------------------------------------- Lamprey

def lamprey(phase=0.0, latched=0.0, glow=1.0):
    """Small eel (forward +Y): a sinuous body of shrinking capsules (``phase`` = swimming
    S-curve), a round sucker disc with a ring of teal hooked teeth at the front, gill pores
    along the sides. ``latched`` flattens the disc forward (attached)."""
    n = 7
    pts = []
    amp = 0.13 * (1 - 0.7 * latched)
    for i in range(n):
        y = 0.62 - 0.27 * i
        pts.append((amp * np.sin(phase - 1.1 * i) * (0.3 + i / n), y))

    def scene(p):
        parts = []
        for i in range(n - 1):
            r0 = 0.25 - 0.028 * i
            parts.append((sd_capsule(p, (pts[i][0], pts[i][1], 0.1), (pts[i + 1][0],
                                                                      pts[i + 1][1], 0.1),
                                     r0, r0 - 0.028), V_SEAM))
        d, m = union(*parts, k=0.06)
        hx0, hy0 = pts[0]
        disc_y = hy0 + 0.2 + 0.06 * latched
        rd = 0.27 + 0.05 * latched
        # oral disc tilted up so it reads from above: dark funnel ringed by glowing hooks
        d, m = union(
            (d, m),
            (sd_ellipsoid(p, (hx0, disc_y, 0.14), (rd, rd * 0.85, 0.11)), V_DARK),
            k=0.05,
        )
        d, m = subtract((d, m), sd_ellipsoid(p, (hx0, disc_y + 0.02, 0.3), (rd * 0.6, rd * 0.5, 0.14)))
        teeth = []
        for j in range(12):
            a = j * 2 * np.pi / 12
            teeth.append((sd_sphere(p, (hx0 + rd * 0.72 * np.cos(a),
                                        disc_y + 0.02 + rd * 0.6 * np.sin(a), 0.22), 0.035),
                          V_GLOW))
        gills = [(sd_sphere(p, (pts[1][0] + s * 0.17, pts[1][1] - 0.06 * k, 0.16), 0.025),
                  V_EYE) for s in (1, -1) for k in range(3)]
        fin = [(sd_capsule(p, (pts[i][0], pts[i][1], 0.24), (pts[i + 1][0], pts[i + 1][1], 0.22),
                           0.03, 0.02), V_BONE) for i in range(3, n - 1)]
        return union((d, m), *teeth, *gills, *fin, k=0.02)
    return scene, vrell("lamprey", glow)


# --------------------------------------------------------------------------- Driftjelly

def driftjelly(pulse=0.0, glow=1.0):
    """Floating jellyfish mine (radial): a translucent-looking bell with lime veins and a
    frilled rim, contracting with ``pulse`` (0..1), and eight trailing tentacles spreading
    under it (seen from above as a star of tendrils)."""
    c = 1.0 - 0.18 * pulse

    def scene(p):
        r = np.sqrt(p[:, 0] ** 2 + p[:, 1] ** 2)
        a = np.arctan2(p[:, 1], p[:, 0])
        sec = 2 * np.pi / 8
        af = (a + sec / 2) % sec - sec / 2
        fp = np.stack([r * np.cos(af), r * np.sin(af), p[:, 2]], axis=-1)
        d, m = union(
            (sd_ellipsoid(p, (0, 0, 0.12), (0.62 * c, 0.62 * c, 0.42 + 0.1 * pulse)), V_SAC),
            (sd_ellipsoid(fp, (0.58 * c, 0.0, 0.02), (0.14, 0.12, 0.06)), V_DARK),   # rim frills
            k=0.06,
        )
        # top knob and a glowing organ cluster under the bell top
        d, m = union((d, m), (sd_sphere(p, (0, 0, 0.5 + 0.08 * pulse), 0.13), V_GLOW), k=0.05)
        ten = []
        sw = 0.12 * np.sin(np.pi * pulse)
        for j in range(8):
            ang = (j + 0.5) * sec
            ca, sa = np.cos(ang), np.sin(ang)
            r0, r1, r2 = 0.5 * c, 0.85, 1.08 + sw
            bend = 0.12
            p0 = (ca * r0, sa * r0, -0.05)
            p1 = (ca * r1 - sa * bend, sa * r1 + ca * bend, -0.1)
            p2 = (ca * r2 + sa * bend * 0.5, sa * r2 - ca * bend * 0.5, -0.14)
            ten += [(sd_capsule(p, p0, p1, 0.05, 0.035), V_BONE),
                    (sd_capsule(p, p1, p2, 0.035, 0.015), V_BONE)]
        return union((d, m), *ten, k=0.04)
    mats = vrell("driftjelly", glow)
    mid, dark, bone = (em.hx(c) for c in em.CHITIN_BASES["olive"])
    gl = np.array(em.hx(em.GLOWS["lime"]))

    def bell_veins(p, n):
        a = np.arctan2(p[:, 1], p[:, 0])
        return np.where(np.abs(((a / (2 * np.pi) * 16) % 1.0) - 0.5) < 0.09, 1.0, 0.15)
    mats[V_SAC] = Material(em._mix(mid, (0.85, 0.95, 0.75), 0.25), metal=0.15, shininess=140,
                           spec=1.3, emission=tuple(gl * 0.9 * glow), emission_pattern=bell_veins)
    return scene, mats


# --------------------------------------------------------------------------- Reef Spitter

def reef_raft(glow=1.0):
    """Floating biomass raft (static): an irregular mat of kelp-like lobes and air bladders,
    seaweed fringe, lime-veined. The Reef Spitter gun sits on its centre."""
    rng = np.random.default_rng(6)
    lobes = [(0.0, 0.0, 0.62, 0.55)] + [
        (0.62 * np.cos(a) + rng.uniform(-0.08, 0.08), 0.52 * np.sin(a) + rng.uniform(-0.08, 0.08),
         rng.uniform(0.22, 0.32), rng.uniform(0.2, 0.28))
        for a in np.linspace(0, 2 * np.pi, 9, endpoint=False)]
    bladders = [(0.5 * np.cos(a), 0.44 * np.sin(a)) for a in np.linspace(0.3, 6.6, 7)]

    def scene(p):
        items = [(sd_ellipsoid(p, (x, y, -0.02), (rx, ry, 0.08)), V_DARK) for x, y, rx, ry in lobes]
        d, m = union(*items, k=0.12)
        d, m = union((d, m), *[(sd_sphere(p, (x, y, 0.04), 0.09), V_SAC) for x, y in bladders],
                     k=0.04)
        fr = []
        for a in np.linspace(0, 2 * np.pi, 14, endpoint=False):
            ca, sa = np.cos(a), np.sin(a)
            fr.append((sd_capsule(p, (0.8 * ca, 0.7 * sa, -0.06), (1.05 * ca + 0.1 * sa,
                                                                   0.95 * sa - 0.1 * ca, -0.08),
                                  0.05, 0.015), V_BONE))
        return union((d, m), *fr, k=0.03)
    mats = vrell("reef-spitter", glow)
    gl = np.array(em.hx(em.GLOWS["lime"]))
    olive = (em.hx(c) for c in em.CHITIN_BASES["olive"])
    om, od, ob = olive
    mats[V_DARK] = Material(em._mix(om, od, 0.55), metal=0.05, shininess=30, spec=0.3,
                            pattern=lambda pp, n: 0.8 + 0.2 * np.cos(pp[:, 0] * 23 + pp[:, 1] * 17))
    mats[V_SAC] = Material(em._mix(om, (0.9, 0.9, 0.6), 0.3), metal=0.1, shininess=90, spec=1.0,
                           emission=tuple(gl * 0.35 * glow))
    mats[V_BONE] = Material(em._mix(om, od, 0.3), metal=0.0, shininess=20, spec=0.2)
    return scene, mats


def reef_gun(recoil=0.0, glow=1.0):
    """Barnacle gun (forward +Y = aim): a plated cone with three mouths fanned forward
    (the 3-way fan), violet glow in the mouths. ``recoil`` pulls the mouths back."""
    def scene(p):
        q = mirror_x(p)
        d, m = union(
            (sd_cylinder_z(p, (0, 0, 0.05), 0.5, 0.12), V_BODY),
            (sd_ellipsoid(p, (0, -0.05, 0.25), (0.42, 0.46, 0.3)), V_SEAM),
            k=0.1,
        )
        plates = [(sd_ellipsoid(p, (0.38 * np.cos(a), 0.38 * np.sin(a) - 0.05, 0.22),
                                (0.15, 0.15, 0.14)), V_DARK)
                  for a in np.linspace(0.6, 2 * np.pi + 0.6, 6, endpoint=False)]
        d, m = union((d, m), *plates, k=0.04)
        rc = -0.12 * recoil
        mouths = []
        for ang in (-0.42, 0.0, 0.42):
            ca, sa = np.cos(np.pi / 2 + ang), np.sin(np.pi / 2 + ang)
            a0 = (0.12 * ca, 0.12 * sa, 0.42)
            a1 = ((0.5 + rc) * ca, (0.5 + rc) * sa, 0.4)
            mouths += [(sd_capsule(p, a0, a1, 0.16, 0.14), V_DARK),
                       (sd_cylinder_z(p, ((0.5 + rc) * ca, (0.5 + rc) * sa, 0.46), 0.15, 0.05),
                        V_BONE),
                       (sd_sphere(p, ((0.5 + rc) * ca, (0.5 + rc) * sa, 0.46), 0.09), V_GLOW)]
        return union((d, m), *mouths, k=0.04)
    return scene, vrell("reef-spitter", glow)


# --------------------------------------------------------------------------- Skimmer

def skimmer(phase=0.0, glow=1.0):
    """Flying-fish-like skiff (forward +Y): a deep fish body with a blunt head and side eyes,
    broad fan-shaped pectoral fins swept back with dark ribs (they skim the water), a small
    pelvic fin pair, a big forked tail beating with ``phase``, a violet gland on the snout."""
    def scene(p):
        q = mirror_x(p)
        tail_sw = 0.14 * np.sin(phase)
        d, m = union(
            (sd_ellipsoid(p, (0, 0.18, 0.1), (0.27, 0.62, 0.2)), V_BODY),
            (sd_ellipsoid(p, (0, 0.62, 0.1), (0.22, 0.24, 0.17)), V_BODY),       # blunt head
            (sd_capsule(p, (0, -0.35, 0.08), (tail_sw * 0.4, -0.7, 0.07), 0.15, 0.07), V_DARK),
            k=0.1,
        )
        # fan fins: rounded, swept back, ribbed (rib pattern from the material)
        fin = [(0.18, 0.42), (0.52, 0.3), (0.82, 0.02), (0.84, -0.22), (0.62, -0.32),
               (0.36, -0.12), (0.16, 0.1)]
        pelvic = [(0.12, -0.24), (0.34, -0.36), (0.3, -0.48), (0.1, -0.4)]
        tail = [(0.0, -0.66), (0.42, -0.98), (0.36, -1.1), (0.08, -0.92), (0.0, -0.86)]
        qt = q.copy()
        qt[:, 0] = np.abs(p[:, 0] - tail_sw)
        d, m = union(
            (d, m),
            (sd_plate(q, fin, 0.06, 0.03, 0.012,
                      taper=lambda xx, yy: np.clip(1.25 - xx, 0.35, 1.0)), V_SAC),
            (sd_plate(q, pelvic, 0.04, 0.025, 0.01), V_SAC),
            (sd_plate(qt, tail, 0.06, 0.03, 0.012), V_SAC),
            k=0.03,
        )
        return union(
            (d, m),
            (sd_sphere(p, (0, 0.8, 0.16), 0.065), V_GLOW),
            (sd_sphere(q, (0.17, 0.64, 0.16), 0.05), V_EYE),
            (sd_capsule(p, (0, 0.42, 0.28), (0, -0.3, 0.26), 0.035, 0.02), V_BONE),
            k=0.02,
        )
    mats = vrell("skimmer", glow)
    mid, dark, bone = (em.hx(c) for c in em.CHITIN_BASES["rust"])

    def ribs(pp, n):
        a = np.arctan2(pp[:, 1] - 0.2, np.abs(pp[:, 0]) - 0.1)
        return 0.7 + 0.3 * (np.abs(((a * 7.0) % 1.0) - 0.5) > 0.12)
    mats[V_SAC] = Material(em._mix(mid, dark, 0.55), metal=0.15, shininess=70, spec=0.7,
                           pattern=ribs)
    return scene, mats


# --------------------------------------------------------------------------- Threadcrawler

def thread_head(jaw=0.0, glow=1.0):
    """Centipede head (forward +Y): flat armoured head plate, curved forcipules (open with
    ``jaw``), long antennae, lime eye clusters."""
    def scene(p):
        q = mirror_x(p)
        fq = _hinge_z(q, (0.22, 0.46, 0.0), -0.35 * jaw)
        d, m = union(
            (sd_ellipsoid(p, (0, -0.1, 0.08), (0.78, 0.5, 0.14)), V_BODY),
            (sd_ellipsoid(p, (0, 0.3, 0.09), (0.6, 0.32, 0.12)), V_SEAM),
            k=0.08,
        )
        return union(
            (d, m),
            (sd_capsule(fq, (0.24, 0.46, 0.06), (0.44, 0.82, 0.05), 0.12, 0.07), V_BONE),
            (sd_capsule(fq, (0.44, 0.82, 0.05), (0.14, 1.04, 0.04), 0.07, 0.02), V_BONE),
            (sd_capsule(q, (0.14, 0.62, 0.2), (0.62, 1.12, 0.22), 0.025, 0.012), V_DARK),
            (sd_sphere(q, (0.24, 0.5, 0.24), 0.055), V_EYE),
            (sd_sphere(q, (0.32, 0.4, 0.22), 0.04), V_EYE),
            k=0.02,
        )
    return scene, vrell("threadcrawler", glow)


def thread_segment(phase=0.0, glow=1.0):
    """Body segment (forward +Y): wide flat tergite plate with a lime spore pore, a pair of
    long jointed legs swinging with ``phase``."""
    def scene(p):
        q = mirror_x(p)
        d, m = union(
            (sd_ellipsoid(p, (0, 0, 0.1), (0.62, 0.6, 0.2)), V_SEAM),
            (sd_ellipsoid(p, (0, -0.04, 0.2), (0.52, 0.46, 0.14)), V_BODY),
            k=0.05,
        )
        d, m = union((d, m), (sd_sphere(p, (0, 0.0, 0.33), 0.09), V_GLOW), k=0.02)
        legs = []
        for side in (1, -1):
            ph = phase + (0 if side > 0 else np.pi)
            sw = 0.28 * np.sin(ph)
            lift = 0.08 * max(0.0, np.cos(ph))
            hip = (side * 0.5, 0.0, 0.06)
            knee = (side * 0.88, 0.1 + sw * 0.5, 0.22 + lift)
            foot = (side * 1.1, 0.16 + sw, -0.02 + lift)
            legs += [(sd_capsule(p, hip, knee, 0.1, 0.075), V_DARK),
                     (sd_capsule(p, knee, foot, 0.075, 0.035), V_BONE)]
        return union((d, m), *legs, k=0.03)
    return scene, vrell("threadcrawler", glow)


def thread_tail(glow=1.0):
    def scene(p):
        q = mirror_x(p)
        return union(
            (sd_ellipsoid(p, (0, 0.1, 0.1), (0.45, 0.4, 0.18)), V_SEAM),
            (sd_capsule(q, (0.15, -0.15, 0.08), (0.42, -0.95, 0.06), 0.07, 0.02), V_BONE),
            k=0.05,
        )
    return scene, vrell("threadcrawler", glow)


# --------------------------------------------------------------------------- Dust Devil

def dust_devil(spin=0.0, core=0.0, glow=1.0):
    """Radial vortex organism, 5-fold: curved membrane vanes spiralling around a central
    core. ``spin`` rotates the vanes about Z (radians); ``core`` (0..1) parts the vanes at the
    top of the spin cycle to expose the glowing core (the weak point)."""
    k = 5
    sec = 2 * np.pi / k

    def scene(p):
        pr = rotate_z(p, -spin)
        r = np.sqrt(pr[:, 0] ** 2 + pr[:, 1] ** 2)
        a = np.arctan2(pr[:, 1], pr[:, 0])
        # logarithmic-spiral vanes: unwrap angle by radius, fold into one sector
        tw = a + 2.2 * r
        af = (tw + sec / 2) % sec - sec / 2
        vp = np.stack([r, af * np.maximum(r, 0.2), pr[:, 2]], axis=-1)
        height = 0.34 * (1.0 - 0.7 * core) * np.clip(1.15 - r, 0, 1) + 0.04
        vane = np.maximum(np.maximum(np.abs(vp[:, 1]) - 0.05 - 0.04 * r, r - 1.1),
                          np.maximum(np.abs(vp[:, 2] - height * 0.5) - height * 0.5, 0.18 - r))
        d, m = union(
            (vane, V_SEAM),
            (sd_cylinder_z(pr, (0, 0, -0.02), 0.95, 0.03), V_DARK),       # base membrane disc
            k=0.03,
        )
        # core: a lime orb in a fleshy cup, rising when exposed
        d, m = union((d, m),
                     (sd_ellipsoid(pr, (0, 0, 0.12), (0.3, 0.3, 0.16)), V_SAC),
                     (sd_sphere(pr, (0, 0, 0.14 + 0.18 * core), 0.12 + 0.07 * core), V_GLOW),
                     k=0.05)
        # vane tips: bone hooks
        tips = []
        for j in range(k):
            ang = j * sec - 2.2 * 1.0
            tips.append((sd_sphere(pr, (1.02 * np.cos(ang), 1.02 * np.sin(ang), 0.06), 0.06),
                         V_BONE))
        return union((d, m), *tips, k=0.03)
    return scene, vrell("dust-devil", glow)


# --------------------------------------------------------------------------- Spiral Nautilus

def nautilus_shell(glow=1.0):
    """Coiled shell seen from above: a flattened logarithmic-spiral disc with growth ribs and
    tiger stripes (model space, so they turn when it spins). Aperture faces +Y."""
    def scene(p):
        r = np.sqrt(p[:, 0] ** 2 + p[:, 1] ** 2)
        a = np.arctan2(p[:, 1], p[:, 0])
        # spiral radius grows with angle; the opening (largest whorl) at +Y side
        d, m = union(
            (sd_ellipsoid(p, (0.08, -0.04, 0.0), (0.8, 0.84, 0.24)), V_BODY),
            (sd_ellipsoid(p, (0.0, 0.66, 0.0), (0.7, 0.32, 0.3)), V_SEAM),     # flared aperture
            k=0.14,
        )
        d, m = subtract((d, m), sd_ellipsoid(p, (0.0, 0.82, 0.05), (0.5, 0.2, 0.22)))
        # umbilicus: the coil's centre dimple
        d, m = subtract((d, m), sd_sphere(p, (0.12, -0.08, 0.52), 0.2))
        d, m = union((d, m), (sd_sphere(p, (0.12, -0.08, 0.28), 0.1), V_DARK), k=0.03)
        return d, m
    mats = vrell("spiral-nautilus", glow)
    mid, dark, bone = (em.hx(c) for c in em.CHITIN_BASES["bone"])
    gl = np.array(em.hx(em.GLOWS["violet"]))

    def stripes(p, n):
        x, y = p[:, 0] - 0.12, p[:, 1] + 0.08
        r = np.sqrt(x * x + y * y)
        a = np.arctan2(y, x)
        spiral = (np.log(np.maximum(r, 0.05)) * 3.2 - a / (2 * np.pi) * 3.2 * 1.0)
        ribs = 0.82 + 0.18 * np.cos(a * 22) ** 2
        tiger = np.where(((a * 6 + r * 9) % (2 * np.pi)) < 2.0, 0.45, 1.0)
        whorl = np.where(np.abs((spiral % 1.0) - 0.5) < 0.06, 0.55, 1.0)
        return ribs * tiger * whorl
    mats[V_BODY] = Material(mid, metal=0.25, shininess=120, spec=1.2, pattern=stripes)
    mats[V_SEAM] = Material(em._mix(mid, bone, 0.4), metal=0.25, shininess=120, spec=1.1,
                            emission=tuple(gl * 0.6 * glow),
                            emission_pattern=lambda p, n: np.where(p[:, 1] > 0.86, 1.0, 0.0))
    return scene, mats


def nautilus_crown(spread=1.0, phase=0.0, glow=1.0):
    """Tentacle crown (forward +Y), shown when the nautilus uncoils: a hooded mantle, two
    violet eyes and a fan of tentacles whose ``spread`` opens towards the fire direction."""
    def scene(p):
        q = mirror_x(p)
        d, m = union(
            (sd_ellipsoid(p, (0, -0.2, 0.1), (0.46, 0.36, 0.22)), V_SAC),
            (sd_sphere(q, (0.28, -0.08, 0.22), 0.08), V_EYE),
            k=0.06,
        )
        ten = []
        for j in range(8):
            u = (j - 3.5) / 3.5
            ang = np.pi / 2 + u * 0.55 * spread
            wig = 0.1 * np.sin(phase + j * 1.3)
            ca, sa = np.cos(ang + wig), np.sin(ang + wig)
            l0, l1 = 0.1, 0.55 + 0.45 * spread * (1 - 0.3 * abs(u))
            p0 = (ca * l0 * 0.8, -0.15 + sa * l0, 0.08)
            p1 = (ca * l1, -0.15 + sa * l1, 0.06)
            ten.append((sd_capsule(p, p0, p1, 0.06, 0.02), V_DARK))
            ten.append((sd_sphere(p, p1, 0.035), V_GLOW))
        return union((d, m), *ten, k=0.04)
    return scene, vrell("spiral-nautilus", glow)


# --------------------------------------------------------------------------- Halo Platform

def halo_core(shield=1.0, glow=1.0):
    """Shielded core of the Halo Platform (radial): an octagonal armoured drum, gold trim,
    a red reactor eye under a glass dome. The shield itself is drawn as an overlay."""
    def scene(p):
        r = np.sqrt(p[:, 0] ** 2 + p[:, 1] ** 2)
        a = np.arctan2(p[:, 1], p[:, 0])
        sec = 2 * np.pi / 8
        af = (a + sec / 2) % sec - sec / 2
        op = np.stack([r * np.cos(af), r * np.sin(af), p[:, 2]], axis=-1)
        d, m = union(
            (sd_box(op, (0, 0, 0.12), (0.62, 0.6, 0.16), 0.03), A_HULL),
            (sd_box(op, (0.58, 0, 0.26), (0.06, 0.2, 0.05), 0.01), A_GOLD),
            k=0.0,
        )
        d, m = union((d, m),
                     (sd_cylinder_z(p, (0, 0, 0.3), 0.4, 0.06), A_FACET),
                     (sd_sphere(p, (0, 0, 0.32), 0.3), A_GLASS),
                     (sd_sphere(p, (0, 0, 0.42), 0.14), A_RED),
                     k=0.02)
        bolts = [(sd_sphere(op, (0.44, 0.38 * s, 0.3), 0.04), A_GOLD) for s in (1, -1)]
        return union((d, m), *bolts, k=0.0)
    return scene, asc("halo-platform", glow)


def halo_segment(glow=1.0):
    """One ring segment (forward +Y = tangent direction): a curved armoured rail block with gold
    edge trim and a turret mount ring on top. Pivot at the origin (the segment's centre)."""
    def scene(p):
        q = mirror_x(p)
        # a slight arc: bend the segment around +X (towards the ring centre on -X)
        # arc concave towards the ring centre, which lies on the model's +X side
        bent = p.copy()
        bent[:, 0] = p[:, 0] - 0.25 * (p[:, 1] ** 2)
        bq = bent.copy()
        bq[:, 1] = np.abs(bq[:, 1])
        d, m = union(
            (sd_box(bent, (0, 0, 0.1), (0.24, 1.0, 0.12), 0.04), A_HULL),
            (sd_box(bent, (0.24, 0, 0.18), (0.04, 0.96, 0.035), 0.01), A_GOLD),
            (sd_box(bent, (-0.24, 0, 0.18), (0.04, 0.96, 0.035), 0.01), A_GOLD),
            k=0.0,
        )
        d, m = union((d, m),
                     (sd_cylinder_z(p, (0, 0, 0.24), 0.22, 0.04), A_FACET),
                     (sd_box(bq, (0.0, 0.97, 0.15), (0.28, 0.05, 0.06), 0.01), A_GUN),
                     k=0.0)
        return d, m
    return scene, asc("halo-platform", glow)


def halo_turret(flash=0.0, glow=1.0):
    """Turret head (forward +Y = aim): armoured dome with twin barrels and a red sensor."""
    def scene(p):
        q = mirror_x(p)
        d, m = union(
            (sd_ellipsoid(p, (0, -0.05, 0.3), (0.36, 0.4, 0.2)), A_FACET),
            (sd_box(p, (0, -0.32, 0.28), (0.22, 0.12, 0.12), 0.03), A_HULL),
            k=0.04,
        )
        return union(
            (d, m),
            (sd_capsule(q, (0.12, 0.1, 0.36), (0.12, 0.92, 0.36), 0.06, 0.05), A_GUN),
            (sd_capsule(q, (0.12, 0.9, 0.36), (0.12, 0.98, 0.36), 0.07, 0.07), A_GOLD),
            (sd_sphere(p, (0, 0.12, 0.46), 0.07), A_RED),
            k=0.01,
        )
    return scene, asc("halo-platform", glow * (1 + flash))
