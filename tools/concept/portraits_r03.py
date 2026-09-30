#!/usr/bin/env python3
"""Concept round 03 - briefing portraits in two styles (palette B, 960x540 baseline).

Outputs:
  design/story/characters/<slug>/concept/portrait-r03-a.png   style A "pre-rendered 3D bust"
  design/story/characters/<slug>/concept/portrait-r03-b.png   style B "comm-screen pixel portrait"
  design/story/characters/concept/cast-r03-a.png / -b.png      all four side by side per style
for slug in okafor, rook, varga, vorne.

Both styles start from the same stylised SDF bust per character (tools/concept/render/sdf.py,
a key pass plus a coloured rim-light pass from behind). Style A keeps the smooth CGI shading and
adds a late-90s briefing-screen treatment (rim light, 64-colour palette, soft scanlines,
vignette). Style B reduces the bust to a 72 px pixel portrait: posterised to a 5-tone
comm-screen ramp with ordered dithering, a few kept accent colours, an outline and
interference lines. Each sheet shows the 144x144 briefing portrait, the 72x72 HUD radio
portrait in the HUD A metallic frame, and a 3x enlargement.

These are style mockups: faces are procedural, so the point is framing, lighting, colour and
overlay; concept/prompts.md next to each file has the prompts for polished portraits.
Run: python3 tools/concept/portraits_r03.py [okafor] [rook] [varga] [vorne]   (~3 min)
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, str(Path(__file__).resolve().parent))
import hud_r02  # noqa: E402  (HUD A metal panel helpers and colours)
from render import raster, sdf, sprite  # noqa: E402
from render.config import ROOT, out_path  # noqa: E402
from render.palette import B  # noqa: E402
from render.sdf import Material, vec  # noqa: E402

CHAR_DIR = ROOT / "design" / "story" / "characters"
RENDER = 288            # render size; briefing portrait is RENDER / 2 = 144
EXTENT = 1.5            # world units across the render (head and shoulders fill the frame)
CENTER = (0.0, 0.3)

# material slots shared by all busts
SKIN, EYE, IRIS, LIPS, HAIR, CLOTH, ACCENT, METAL, GLOW, EXTRA, BROW = range(11)


# --------------------------------------------------------------------------- extra SDF helpers

def sd_torus_z(p, c, r_major, r_minor):
    """Torus lying in the XY plane (ring facing the camera): glasses rims, lenses."""
    q = p - vec(*c)
    xy = np.sqrt(q[:, 0] ** 2 + q[:, 1] ** 2) - r_major
    return np.sqrt(xy ** 2 + q[:, 2] ** 2) - r_minor


def sd_torus_x(p, c, r_major, r_minor):
    """Torus in the YZ plane (ring around the X axis): headset band over the head."""
    q = p - vec(*c)
    yz = np.sqrt(q[:, 1] ** 2 + q[:, 2] ** 2) - r_major
    return np.sqrt(yz ** 2 + q[:, 0] ** 2) - r_minor


def turn(p, yaw, pitch=0.0, pivot=(0.0, 0.3, 0.0)):
    """Turn the head: yaw about Y (three-quarter view), pitch about X, around the neck."""
    q = p - vec(*pivot)
    q = sdf.rotate_y(q, yaw)
    if pitch:
        q = sdf.rotate_x(q, pitch)
    return q + vec(*pivot)


def noise_pattern(scale, amount, seed=0.0):
    """Cheap procedural grain for hair / fabric (multiplier around 1)."""
    def f(p, n):
        v = (np.sin(p[:, 0] * scale * 1.7 + seed) * np.sin(p[:, 1] * scale * 2.3 + 1.3 * seed)
             * np.sin(p[:, 2] * scale * 1.1 + 0.7 * seed))
        return 1.0 + amount * v
    return f


def strand_pattern(scale, amount):
    """Hair strands running back over the head (slicked or cropped hair)."""
    def f(p, n):
        v = np.sin(p[:, 0] * scale + 3.0 * np.sin(p[:, 1] * 4.0))
        return 1.0 - amount * (0.5 + 0.5 * v)
    return f


# --------------------------------------------------------------------------- the common bust

def face(q, skin_k=0.06, jaw=1.0, mouth="neutral", eyes_open=1.0):
    """Head and face in head space q. Returns a list of (distance, material) items plus the
    skin distance (used by callers to attach hair / props)."""
    head = sdf.sd_ellipsoid(q, (0, 0.47, -0.02), (0.29, 0.37, 0.32))
    jawd = sdf.sd_ellipsoid(q, (0, 0.30, 0.03), (0.215 * jaw, 0.21, 0.25))
    cheek_l = sdf.sd_ellipsoid(q, (-0.15, 0.40, 0.17), (0.085, 0.06, 0.08))
    cheek_r = sdf.sd_ellipsoid(q, (0.15, 0.40, 0.17), (0.085, 0.06, 0.08))
    nose = sdf.sd_capsule(q, (0, 0.49, 0.29), (0, 0.375, 0.365), 0.028, 0.042)
    nostr = sdf.sd_ellipsoid(q, (0, 0.365, 0.325), (0.058, 0.03, 0.04))
    brow = sdf.sd_capsule(q, (-0.20, 0.54, 0.22), (0.20, 0.54, 0.22), 0.035)
    chin = sdf.sd_ellipsoid(q, (0, 0.175, 0.19), (0.07, 0.05, 0.05))
    ear_l = sdf.sd_ellipsoid(q, (-0.29, 0.44, -0.03), (0.035, 0.075, 0.055))
    ear_r = sdf.sd_ellipsoid(q, (0.29, 0.44, -0.03), (0.035, 0.075, 0.055))
    skin = sdf.smin(head, jawd, skin_k)
    for d, k in ((cheek_l, 0.05), (cheek_r, 0.05), (nose, 0.03), (nostr, 0.03),
                 (brow, 0.05), (chin, 0.04), (ear_l, 0.02), (ear_r, 0.02)):
        skin = sdf.smin(skin, d, k)
    # eye sockets and mouth line carved into the skin
    for x in (-0.11, 0.11):
        skin = np.maximum(skin, -sdf.sd_ellipsoid(q, (x, 0.463, 0.31), (0.062, 0.03 * eyes_open + 0.01, 0.05)))
    if mouth == "grin":       # crooked grin: right corner up
        cut = np.minimum(sdf.sd_capsule(q, (-0.075, 0.268, 0.29), (0.0, 0.262, 0.31), 0.011),
                         sdf.sd_capsule(q, (0.0, 0.262, 0.31), (0.09, 0.29, 0.285), 0.013))
    elif mouth == "smile":    # faint, thin smile
        cut = np.minimum(sdf.sd_capsule(q, (-0.07, 0.272, 0.29), (0.0, 0.266, 0.31), 0.007),
                         sdf.sd_capsule(q, (0.0, 0.266, 0.31), (0.075, 0.28, 0.29), 0.007))
    else:
        cut = sdf.sd_capsule(q, (-0.07, 0.27, 0.29), (0.07, 0.27, 0.29), 0.008)
    lips = sdf.sd_ellipsoid(q, (0, 0.27, 0.28), (0.075, 0.03, 0.035))
    skin = np.maximum(skin, -cut)
    items = [(skin, SKIN), (np.maximum(lips, -cut) + 0.004, LIPS)]
    for x in (-0.11, 0.11):
        ball = sdf.sd_sphere(q, (x, 0.462, 0.25), 0.052)
        iris = sdf.sd_sphere(q, (x + 0.004, 0.462, 0.281), 0.029)
        lid = sdf.sd_capsule(q, (x - 0.05, 0.488, 0.283), (x + 0.05, 0.49, 0.28), 0.02)
        lower = sdf.sd_capsule(q, (x - 0.045, 0.438, 0.286), (x + 0.045, 0.437, 0.283), 0.012)
        items += [(ball, EYE), (iris, IRIS), (lid, SKIN), (lower, SKIN)]
        items.append((sdf.sd_capsule(q, (x - 0.06, 0.525, 0.275), (x + 0.06, 0.53, 0.268), 0.013), BROW))
    return items, skin


def body(p, neck_r=0.13, shoulders=0.78):
    """Neck and shoulders in body space (turned less than the head)."""
    neck = sdf.sd_capsule(p, (0, 0.24, -0.05), (0, -0.08, -0.05), neck_r)
    chest = sdf.sd_box(p, (0, -0.62, -0.06), (0.62 * shoulders / 0.78, 0.5, 0.2), 0.17)
    sh = shoulders - 0.26
    torso = chest
    for s in (-1, 1):
        torso = sdf.smin(torso, sdf.sd_ellipsoid(p, (s * sh, -0.24, -0.07), (0.26, 0.15, 0.2)), 0.12)
    trap = sdf.sd_ellipsoid(p, (0, -0.12, -0.08), (0.34, 0.14, 0.16))
    torso = sdf.smin(torso, trap, 0.1)
    return neck, torso


# --------------------------------------------------------------------------- characters

def okafor():
    """Commander Adaeze Okafor: close-cropped grey hair, scar, navy dress uniform, headset."""
    mats = [
        Material((0.42, 0.27, 0.19), shininess=22, spec=0.35),          # skin
        Material((0.85, 0.83, 0.8), shininess=60, spec=0.6),             # eye white
        Material((0.07, 0.04, 0.03), shininess=90, spec=0.9),            # iris
        Material((0.27, 0.14, 0.12), shininess=30, spec=0.35),           # lips
        Material((0.3, 0.3, 0.31), shininess=6, spec=0.05, pattern=noise_pattern(220, 0.55)),
        Material((0.09, 0.12, 0.3), shininess=14, spec=0.2, pattern=noise_pattern(90, 0.08)),
        Material((0.95, 0.72, 0.2), metal=0.8, shininess=80, spec=0.9),  # insignia gold
        Material((0.2, 0.21, 0.24), metal=0.5, shininess=60, spec=0.6),  # headset
        Material((0.1, 0.1, 0.1), emission=(0.2, 1.0, 0.6)),             # mic LED
        Material((0.8, 0.82, 0.86), shininess=20, spec=0.3),             # collar shirt
        Material((0.12, 0.1, 0.1), shininess=10, spec=0.1),              # brows
    ]

    def scene(p):
        q = turn(p, -0.28, 0.03)
        items, skin = face(q, jaw=1.03)
        hair = sdf.sd_ellipsoid(q, (0, 0.52, -0.05), (0.305, 0.36, 0.33))
        hair = np.maximum(hair, -(q[:, 1] - 0.56 + 0.35 * np.maximum(q[:, 2], 0)))  # hairline
        hair = np.maximum(hair, -sdf.sd_ellipsoid(q, (0, 0.40, 0.25), (0.26, 0.2, 0.2)))
        scar = sdf.sd_capsule(q, (-0.135, 0.585, 0.255), (-0.1, 0.515, 0.27), 0.006)
        band = np.maximum(sd_torus_z(q, (0, 0.44, -0.04), 0.335, 0.02), -q[:, 1] + 0.44)
        cup = sdf.sd_cylinder_x(q, (-0.315, 0.44, -0.02), 0.075, 0.03)
        boom = sdf.sd_capsule(q, (-0.32, 0.40, 0.03), (-0.1, 0.29, 0.27), 0.012)
        mic = sdf.sd_sphere(q, (-0.09, 0.285, 0.275), 0.024)
        led = sdf.sd_sphere(q, (-0.33, 0.40, 0.04), 0.012)
        pb = turn(p, -0.1)
        neck, torso = body(pb)
        collar = sdf.sd_capsule(pb, (-0.2, 0.02, 0.1), (0.0, -0.18, 0.18), 0.07)
        collar = np.minimum(collar, sdf.sd_capsule(pb, (0.2, 0.02, 0.1), (0.0, -0.18, 0.18), 0.07))
        shirt = sdf.sd_ellipsoid(pb, (0, -0.08, 0.1), (0.12, 0.14, 0.08))
        pins = np.minimum(sdf.sd_box(pb, (-0.2, -0.06, 0.2), (0.035, 0.02, 0.012), 0.005),
                          sdf.sd_box(pb, (0.2, -0.06, 0.2), (0.035, 0.02, 0.012), 0.005))
        ribbons = sdf.sd_box(pb, (-0.38, -0.36, 0.22), (0.09, 0.04, 0.02), 0.01)
        return sdf.union(*items, (hair, HAIR), (scar - 0.002, LIPS),
                         (sdf.smin(neck, skin, 0.05), SKIN), (torso, CLOTH), (collar, CLOTH),
                         (shirt, EXTRA), (pins, ACCENT), (ribbons, ACCENT),
                         (band, METAL), (cup, METAL), (boom, METAL), (mic, METAL), (led, GLOW))
    return scene, mats


def rook():
    """Lt. Kenji "Rook" Tanaka: messy black hair, pushed-up helmet, crooked grin, flight suit
    with an orange Aegis Wing patch, oxygen mask hanging loose."""
    mats = [
        Material((0.78, 0.6, 0.45), shininess=18, spec=0.25),
        Material((0.88, 0.86, 0.82), shininess=60, spec=0.6),
        Material((0.06, 0.04, 0.03), shininess=90, spec=0.9),
        Material((0.6, 0.35, 0.3), shininess=30, spec=0.3),
        Material((0.05, 0.05, 0.07), shininess=30, spec=0.35, pattern=noise_pattern(120, 0.5, 2)),
        Material((0.22, 0.26, 0.36), shininess=12, spec=0.15, pattern=noise_pattern(80, 0.1)),
        Material((1.0, 0.45, 0.08), shininess=30, spec=0.4),             # Ember orange
        Material((0.55, 0.58, 0.66), metal=0.6, shininess=70, spec=0.8),  # helmet shell
        Material((0.1, 0.1, 0.1), emission=(1.0, 0.55, 0.1)),            # visor glint strip
        Material((0.12, 0.13, 0.16), metal=0.3, shininess=50, spec=0.5),  # mask rubber
        Material((0.05, 0.05, 0.06), shininess=10, spec=0.1),
    ]

    def scene(p):
        q = turn(p, 0.3, -0.02)
        items, skin = face(q, jaw=0.98, mouth="grin")
        hair = sdf.sd_ellipsoid(q, (0, 0.53, -0.05), (0.31, 0.35, 0.33))
        hair = np.maximum(hair, -(q[:, 1] - 0.55 + 0.3 * np.maximum(q[:, 2], 0)))
        tufts = [sdf.sd_capsule(q, (x, 0.66, 0.18), (x + 0.05, 0.55, 0.3), 0.045, 0.02)
                 for x in (-0.16, -0.06, 0.05, 0.14)]
        for t in tufts:
            hair = sdf.smin(hair, t, 0.03)
        helmet = sdf.sd_ellipsoid(q, (0, 0.64, -0.12), (0.36, 0.3, 0.33))
        helmet = np.maximum(helmet, -(q[:, 1] - 0.62))           # only the top shell
        helmet = np.maximum(helmet, -sdf.sd_ellipsoid(q, (0, 0.64, -0.12), (0.32, 0.27, 0.3)))
        visor = sdf.sd_capsule(q, (-0.3, 0.8, 0.02), (0.3, 0.8, 0.02), 0.03)
        mask = sdf.sd_ellipsoid(q, (0.26, 0.22, 0.14), (0.07, 0.09, 0.06))
        hose = sdf.sd_capsule(q, (0.27, 0.15, 0.12), (0.3, 0.0, 0.16), 0.028)
        pb = turn(p, 0.12)
        neck, torso = body(pb, shoulders=0.8)
        collar = sdf.sd_cylinder_y(pb, (0, 0.03, -0.04), 0.19, 0.06)
        zip_ = sdf.sd_box(pb, (0.05, -0.3, 0.26), (0.012, 0.28, 0.012))
        patch = sdf.sd_cylinder_z(pb, (-0.48, -0.36, 0.2), 0.09, 0.03)
        return sdf.union(*items, (hair, HAIR), (helmet, METAL),
                         (visor, GLOW), (mask, EXTRA), (hose, EXTRA),
                         (sdf.smin(neck, skin, 0.05), SKIN), (torso, CLOTH), (collar, CLOTH),
                         (zip_, METAL), (patch, ACCENT))
    return scene, mats


def varga():
    """Dr. Elena Varga: auburn hair pinned up with a stylus, wire glasses, lab coat over an
    intelligence uniform, lit green by a holo display."""
    mats = [
        Material((0.88, 0.7, 0.6), shininess=18, spec=0.25),
        Material((0.9, 0.88, 0.85), shininess=60, spec=0.6),
        Material((0.12, 0.2, 0.1), shininess=90, spec=0.9),
        Material((0.72, 0.38, 0.36), shininess=30, spec=0.35),
        Material((0.45, 0.14, 0.06), shininess=24, spec=0.3, pattern=strand_pattern(90, 0.35)),
        Material((0.2, 0.28, 0.24), shininess=14, spec=0.2),             # intel uniform
        Material((0.8, 0.8, 0.82), metal=0.9, shininess=120, spec=1.0),  # wire glasses
        Material((0.25, 0.25, 0.28), metal=0.5, shininess=50, spec=0.6),  # stylus
        Material((0.1, 0.1, 0.1), emission=(0.2, 1.0, 0.5)),             # holo panel
        Material((0.92, 0.93, 0.95), shininess=12, spec=0.2, pattern=noise_pattern(70, 0.05)),
        Material((0.3, 0.1, 0.05), shininess=10, spec=0.1),
    ]

    def scene(p):
        q = turn(p, -0.22, 0.05)
        items, skin = face(q, jaw=0.92)
        hair = sdf.sd_ellipsoid(q, (0, 0.52, -0.06), (0.31, 0.37, 0.33))
        hair = np.maximum(hair, -(q[:, 1] - 0.6 + 0.35 * np.maximum(q[:, 2], 0)))
        hair = np.maximum(hair, -sdf.sd_ellipsoid(q, (0, 0.40, 0.25), (0.26, 0.2, 0.2)))
        bun = sdf.sd_ellipsoid(q, (0.02, 0.8, -0.18), (0.16, 0.12, 0.13))
        hair = sdf.smin(hair, bun, 0.06)
        stylus = sdf.sd_capsule(q, (-0.22, 0.95, -0.15), (0.26, 0.7, -0.2), 0.013)
        lens = [sd_torus_z(q, (x, 0.462, 0.335), 0.056, 0.006) for x in (-0.11, 0.11)]
        bridge = sdf.sd_capsule(q, (-0.055, 0.47, 0.34), (0.055, 0.47, 0.34), 0.006)
        arms = [sdf.sd_capsule(q, (s * 0.166, 0.466, 0.325), (s * 0.29, 0.47, 0.02), 0.006)
                for s in (-1, 1)]
        glasses = np.minimum.reduce(lens + arms + [bridge])
        pb = turn(p, -0.08)
        neck, torso = body(pb, neck_r=0.12, shoulders=0.72)
        coat = sdf.sd_ellipsoid(pb, (0, -0.6, -0.04), (0.75, 0.47, 0.31))
        inner = sdf.sd_plate(pb, [(-0.16, 0.05), (0.16, 0.05), (0.0, -0.9)], 0.24, 0.12, 0.02)
        lapels = np.minimum(
            sdf.sd_capsule(pb, (-0.18, 0.0, 0.18), (-0.02, -0.75, 0.3), 0.045),
            sdf.sd_capsule(pb, (0.18, 0.0, 0.18), (0.02, -0.75, 0.3), 0.045))
        badge = sdf.sd_box(pb, (0.36, -0.36, 0.23), (0.05, 0.07, 0.015), 0.01)
        return sdf.union(*items, (hair, HAIR), (glasses, ACCENT), (stylus, METAL),
                         (sdf.smin(neck, skin, 0.05), SKIN), (coat, EXTRA),
                         (np.maximum(inner, coat - 0.02), CLOTH), (lapels, EXTRA),
                         (badge, ACCENT))
    return scene, mats


def vorne():
    """Chairman Silas Vorne: slicked-back silver hair, sharp cheekbones, trimmed beard,
    high-collared black suit with gold embroidery and crest, teal shimmer at the temple."""
    mats = [
        Material((0.8, 0.66, 0.58), shininess=24, spec=0.3),
        Material((0.86, 0.84, 0.8), shininess=60, spec=0.6),
        Material((0.2, 0.26, 0.3), shininess=90, spec=0.9),
        Material((0.55, 0.36, 0.34), shininess=30, spec=0.3),
        Material((0.5, 0.53, 0.62), metal=0.5, shininess=70, spec=0.9,
                 pattern=strand_pattern(140, 0.14)),
        Material((0.03, 0.03, 0.035), shininess=40, spec=0.45),          # black suit
        Material((1.0, 0.72, 0.15), metal=0.9, shininess=90, spec=1.0),  # gold
        Material((0.6, 0.6, 0.64), metal=0.3, shininess=40, spec=0.4),   # beard
        Material((0.1, 0.1, 0.1), emission=(0.0, 1.3, 1.0)),             # Vrell teal shimmer
        Material((0.1, 0.08, 0.06), shininess=20, spec=0.2),
        Material((0.45, 0.45, 0.48), shininess=10, spec=0.1),
    ]

    def scene(p):
        q = turn(p, 0.24, -0.06)       # head tilted slightly back: looking down at you
        items, skin = face(q, skin_k=0.04, jaw=0.9, mouth="smile")
        cheek = [sdf.sd_ellipsoid(q, (s * 0.17, 0.415, 0.15), (0.07, 0.035, 0.07)) for s in (-1, 1)]
        items[0] = (sdf.smin(sdf.smin(items[0][0], cheek[0], 0.02), cheek[1], 0.02), SKIN)
        hair = sdf.sd_ellipsoid(q, (0, 0.55, -0.08), (0.30, 0.34, 0.33))
        hair = np.maximum(hair, -(q[:, 1] - 0.62 + 0.25 * np.maximum(q[:, 2], 0)))
        swept = sdf.sd_ellipsoid(q, (0, 0.72, 0.0), (0.27, 0.12, 0.3))
        hair = sdf.smin(hair, swept, 0.05)
        beard = sdf.sd_ellipsoid(q, (0, 0.2, 0.1), (0.17, 0.1, 0.17))
        beard = np.maximum(beard, q[:, 1] - 0.25)
        beard = np.maximum(beard, -sdf.sd_ellipsoid(q, (0, 0.27, 0.3), (0.08, 0.03, 0.05)))
        moust = sdf.sd_capsule(q, (-0.075, 0.305, 0.3), (0.075, 0.305, 0.3), 0.014)
        shimmer = np.minimum(sdf.sd_ellipsoid(q, (0.262, 0.53, 0.1), (0.03, 0.07, 0.045)),
                             sdf.sd_capsule(q, (0.25, 0.47, 0.13), (0.22, 0.4, 0.17), 0.012))
        pb = turn(p, 0.1)
        neck, torso = body(pb, neck_r=0.12, shoulders=0.74)
        collar = sdf.sd_cylinder_y(pb, (0, 0.06, -0.05), 0.2, 0.13)
        collar = np.maximum(collar, -sdf.sd_cylinder_y(pb, (0, 0.06, -0.05), 0.15, 0.2))
        collar = np.maximum(collar, -(pb[:, 2] - 0.0 + (pb[:, 1] - 0.18) * 0.0 - 0.14))
        trim = np.maximum(sdf.sd_cylinder_y(pb, (0, 0.19, -0.05), 0.205, 0.012),
                          -sdf.sd_cylinder_y(pb, (0, 0.19, -0.05), 0.15, 0.2))
        embroid = np.minimum(
            sdf.sd_capsule(pb, (-0.22, -0.12, 0.2), (-0.1, -0.7, 0.3), 0.018),
            sdf.sd_capsule(pb, (0.22, -0.12, 0.2), (0.1, -0.7, 0.3), 0.018))
        crest = sdf.sd_cylinder_z(pb, (-0.36, -0.34, 0.22), 0.07, 0.02)
        ring = sd_torus_z(pb, (-0.36, -0.34, 0.245), 0.1, 0.01)
        return sdf.union(*items, (hair, HAIR), (beard - 0.002, METAL), (moust, METAL),
                         (shimmer, GLOW), (sdf.smin(neck, skin, 0.04), SKIN),
                         (torso, CLOTH), (collar, CLOTH), (trim, ACCENT),
                         (embroid, ACCENT), (crest, ACCENT), (ring, ACCENT))
    return scene, mats


# --------------------------------------------------------------------------- per character setup

CAST = {
    "okafor": dict(build=okafor, name="CMDR A. OKAFOR", role="CDF COMMAND",
                   key=(-1.2, -1.6, 3.2), rim=(2.6, 1.4, -1.2), rim_col=(0.4, 0.7, 1.0),
                   bg=((4, 10, 34), (24, 50, 120)), fill_col=(0.35, 0.55, 1.0), channel="cdf",
                   exposure=1.45, ambient=0.4),
    "rook": dict(build=rook, name="LT. K. TANAKA", role="\"ROOK\" - AEGIS 2",
                 key=(-2.0, 2.4, 3.2), rim=(2.4, 0.6, -1.4), rim_col=(1.0, 0.55, 0.15),
                 bg=((20, 8, 4), (90, 40, 16)), fill_col=(1.0, 0.6, 0.3), channel="cdf"),
    "varga": dict(build=varga, name="DR. E. VARGA", role="CDF INTELLIGENCE",
                  key=(-2.0, 2.2, 3.4), rim=(2.4, -1.2, -1.0), rim_col=(0.3, 1.0, 0.55),
                  bg=((2, 18, 12), (10, 80, 50)), fill_col=(0.4, 1.0, 0.6), channel="cdf"),
    "vorne": dict(build=vorne, name="CHAIRMAN S. VORNE", role="JOVIAN ASCENDANCY",
                  key=(-1.6, 2.8, 2.6), rim=(2.4, 1.6, -1.4), rim_col=(1.0, 0.72, 0.2),
                  bg=((0, 0, 0), (40, 26, 4)), fill_col=(1.0, 0.8, 0.4), channel="enemy"),
}


def render_bust(slug):
    """Key pass + rim pass, returned as float RGBA at RENDER size (alpha = coverage)."""
    c = CAST[slug]
    scene, mats = c["build"]()
    size = (RENDER, RENDER)
    key = sdf.render(scene, mats, size, EXTENT, CENTER, z_top=2.0, steps=120,
                     key_pos=c["key"], ambient=c.get("ambient", 0.28), fill=0.25,
                     exposure=c.get("exposure", 1.0))
    flat = [Material(m.albedo, 0.0, 30.0, 0.6) for m in mats]
    rim = sdf.render(scene, flat, size, EXTENT, CENTER, z_top=2.0, steps=120, shadows=False,
                     key_pos=c["rim"], ambient=0.0, fill=0.0, exposure=1.6)
    out = key.copy()
    lum = rim[..., :3].mean(axis=-1, keepdims=True)
    out[..., :3] = np.clip(key[..., :3] + lum * np.array(c["rim_col"]) * 1.4, 0, 1)
    return out, key


# --------------------------------------------------------------------------- style A

def background_a(slug, n):
    c = CAST[slug]
    lo, hi = (np.array(v, dtype=np.float64) for v in c["bg"])
    yy, xx = np.mgrid[0:n, 0:n] / (n - 1)
    r = np.sqrt((xx - 0.62) ** 2 + (yy - 0.35) ** 2)
    t = np.clip(1.0 - r * 1.3, 0, 1)[..., None]
    rgb = lo + (hi - lo) * t
    grid = ((np.arange(n) % 12 == 0)[None, :] | (np.arange(n) % 12 == 0)[:, None])
    rgb = rgb + grid[..., None] * (hi - lo) * 0.25
    return rgb / 255.0


def style_a(slug, busts):
    bust = busts[0]
    n = RENDER
    bg = background_a(slug, n)
    a = bust[..., 3:4]
    rgb = bust[..., :3] * a + bg * (1 - a)
    img = sprite.to_image(sprite.downsample(np.concatenate([rgb, np.ones((n, n, 1))], -1), 2))
    img = sprite.sharpen(img, 70)
    glow = img.filter(ImageFilter.GaussianBlur(3))
    arr = np.array(img).astype(np.float64)
    g = np.array(glow).astype(np.float64)
    arr[..., :3] = arr[..., :3] + np.clip(g[..., :3] - 150, 0, None) * 0.5   # highlight bloom
    h = arr.shape[0]
    yy, xx = np.mgrid[0:h, 0:h] / (h - 1)
    vig = 1.0 - 0.45 * np.clip(np.sqrt((xx - 0.5) ** 2 + (yy - 0.5) ** 2) * 1.5 - 0.25, 0, 1)
    arr[..., :3] *= vig[..., None]
    arr[1::2, :, :3] *= 0.86                                                  # soft scanlines
    img = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")
    img = sprite.quantize(img, 64)
    small = img.resize((72, 72), Image.LANCZOS)
    small = sprite.quantize(sprite.sharpen(small, 60), 32)
    return img, small


# --------------------------------------------------------------------------- style B

BAYER4 = np.array([[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]) / 16.0
RAMPS = {   # comm-screen tone ramps, dark -> light (palette B UI colours)
    "cdf": ["001810", "00402c", "008060", "00c090", "60ffd0", "e0fff4"],
    "enemy": ["180800", "402000", "805000", "c08000", "ffc040", "fff0c0"],
}


def style_b(slug, busts):
    bust, keyonly = busts
    c = CAST[slug]
    ramp = np.array([raster.hexrgb(h) for h in RAMPS[c["channel"]]], dtype=np.float64)
    small = sprite.downsample(bust, RENDER // 72)                     # 72x72 pixel portrait
    rgb, a = small[..., :3], small[..., 3] > 0.5
    lum = rgb @ np.array([0.35, 0.5, 0.15])
    lum = np.clip((lum - 0.03) / 0.8, 0, 1) ** 0.8
    n = 72
    thr = BAYER4[np.arange(n)[:, None] % 4, np.arange(n)[None, :] % 4]
    levels = len(ramp) - 1
    idx = np.clip(np.floor(lum * (levels - 1) + 0.2 + thr * 0.6), 0, levels - 1).astype(int) + 1
    out = ramp[idx]
    # keep saturated accents (Rook's orange patch, Vorne's gold and teal, Varga's holo glow)
    krgb = sprite.downsample(keyonly, RENDER // 72)[..., :3]
    mx, mn = krgb.max(-1), krgb.min(-1)
    sat = (mx - mn) / np.maximum(mx, 1e-6)
    bluish = (krgb[..., 2] > krgb[..., 0]) & (krgb[..., 2] > krgb[..., 1])   # uniforms, not accents
    accent = a & (sat > 0.6) & (mx > 0.45) & ~bluish
    pa = np.pad(accent, 1)
    accent &= (pa[:-2, 1:-1].astype(int) + pa[2:, 1:-1] + pa[1:-1, :-2] + pa[1:-1, 2:]) >= 2
    q = np.round(krgb * 3) / 3
    out[accent] = np.clip(q[accent] * 255 * 1.1, 0, 255)
    # background: dark screen with a faint dot grid
    bgc = ramp[0].copy()
    yy, xx = np.mgrid[0:n, 0:n]
    grid = (xx % 6 == 0) & (yy % 6 == 0)
    bg = np.where(grid[..., None], ramp[1] * 0.8, bgc)
    img = np.where(a[..., None], out, bg)
    # outline: background pixels touching the figure become the darkest tone
    pad = np.pad(a, 1)
    touch = (pad[:-2, 1:-1] | pad[2:, 1:-1] | pad[1:-1, :-2] | pad[1:-1, 2:]) & ~a
    img[touch] = ramp[0] * 0.4
    # interference: a bright rolling band, two noise lines and two jittered rows
    rng = np.random.default_rng(sum(map(ord, slug)))
    band = 50 + (sum(map(ord, slug)) % 14)          # rolling band over the chest, not the eyes
    img[band:band + 3] = np.clip(img[band:band + 3] * 1.25 + 12, 0, 255)
    for y in rng.choice(np.arange(n - 8, n), 1, replace=False):
        noise = rng.random(n) > 0.6
        img[y, noise] = ramp[-2]
    for y in list(rng.choice(np.arange(2, 12), 1)) + list(rng.choice(np.arange(44, 66), 1)):
        img[y] = np.roll(img[y], int(rng.integers(1, 3)), axis=0)
    hud = Image.fromarray(img.astype(np.uint8), "RGB").convert("RGBA")
    brief = sprite.enlarge(hud, 2)
    arr = np.array(brief).astype(np.float64)
    arr[1::2, :, :3] *= 0.72                                          # CRT scanlines at 2x
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA"), hud


# --------------------------------------------------------------------------- frames and sheets

MET = hud_r02.Metal()



def metal_block(w, h, seed_x=0):
    """A piece of HUD A brushed metal panel, w x h."""
    img = Image.new("RGBA", (w + seed_x, hud_r02.SH), (0, 0, 0, 255))
    MET.panel(img, seed_x, seed_x + w)
    return img.crop((seed_x, 0, seed_x + w, h))


def briefing_frame(slug, portrait):
    """144x144 portrait in a HUD A style bezel with a name plate (the briefing screen block)."""
    c = CAST[slug]
    w, h = 176, 214
    img = metal_block(w, h, 40)
    MET.well(img, (16, 16, 16 + 143, 16 + 143))
    img.alpha_composite(portrait, (16, 16))
    MET.well(img, (16, 170, w - 17, 200))
    col = hud_r02.AMBER if c["channel"] == "cdf" else hud_r02.ALERT
    raster.draw_text(img, 21, 175, c["name"][:22], col)
    raster.draw_text(img, 21, 188, c["role"][:22], hud_r02.CYAN if c["channel"] == "cdf"
                     else B["ASCEND. GOLD"][4])
    return img


def hud_radio(slug, portrait):
    """The HUD A radio block at 1x: RADIO plate, 72x72 portrait well, name, subtitle."""
    c = CAST[slug]
    w, h = 240, 176
    img = metal_block(w, h, 0)
    MET.plate(img, 14, 10, "RADIO")
    MET.well(img, (15, 27, 15 + 71, 27 + 71))
    img.alpha_composite(portrait, (15, 27))
    names = c["name"].split(" ", 1)
    col = hud_r02.AMBER if c["channel"] == "cdf" else hud_r02.ALERT
    hud_r02.txt(img, 96, 32, names[0], col)
    hud_r02.txt(img, 96, 52, names[1][:11], col)
    MET.well(img, (15, 108, w - 16, 164))
    line = {"okafor": "HOLD THE LINE, LANCER. COMMAND OUT.",
            "rook": "CONTACTS ON SIX! I'VE GOT YOUR TAIL.",
            "varga": "SCANS SHOW HELIX ALLOYS IN THAT HULL.",
            "vorne": "HUMANITY NEEDS A NEW SHEPHERD, PILOT."}[slug]
    for i, s in enumerate(hud_r02.wrap(line, 17)[:3]):
        hud_r02.txt(img, 20, 112 + i * 17, s, hud_r02.LCD if c["channel"] == "cdf"
                    else B["ASCEND. GOLD"][4])
    return img


STYLE_TITLE = {"a": "STYLE A: PRE-RENDERED 3D BUST", "b": "STYLE B: COMM-SCREEN PIXEL PORTRAIT"}
STYLE_NOTE = {
    "a": ["SDF BUST: KEY LIGHT + COLOURED RIM LIGHT FROM BEHIND,",
          "64 COLOURS, SOFT SCANLINES, VIGNETTE, HIGHLIGHT BLOOM.",
          "HUD SIZE IS A 32-COLOUR REDUCTION OF THE SAME RENDER."],
    "b": ["72 PX PIXEL PORTRAIT: 5-TONE COMM-SCREEN RAMP, ORDERED",
          "DITHER, KEPT ACCENT COLOURS, OUTLINE, INTERFERENCE LINES.",
          "BRIEFING SIZE IS 2X WITH CRT SCANLINES. CDF CHANNELS ARE",
          "TEAL; ENEMY TRANSMISSIONS (VORNE) ARE AMBER."],
}


def character_sheet(slug, style, brief, hud, round_label="CONCEPT ROUND 03"):
    c = CAST[slug]
    img = raster.sheet(1216, 540, f"{c['name']} - {STYLE_TITLE[style]}", round_label)
    raster.draw_text(img, 16, 38, "BRIEFING 144X144 (1X)", raster.LABEL_DIM)
    img.alpha_composite(briefing_frame(slug, brief), (16, 50))
    raster.draw_text(img, 16, 280, "HUD RADIO 72X72 IN HUD A FRAME (1X)", raster.LABEL_DIM)
    img.alpha_composite(hud_radio(slug, hud), (16, 292))
    raster.draw_text(img, 276, 38, "BRIEFING PORTRAIT 3X", raster.LABEL_DIM)
    img.alpha_composite(sprite.enlarge(brief, 3), (276, 50))
    raster.draw_text(img, 724, 38, "HUD RADIO 2X", raster.LABEL_DIM)
    img.alpha_composite(sprite.enlarge(hud_radio(slug, hud), 2), (724, 50))
    for i, line in enumerate(STYLE_NOTE[style]):
        raster.draw_text(img, 724, 414 + i * 12, line, raster.LABEL)
    raster.draw_text(img, 724, 474, "PROCEDURAL MOCKUP OF THE STYLE - SEE CONCEPT/PROMPTS.MD",
                     raster.LABEL_DIM)
    raster.draw_text(img, 724, 486, "FOR POLISHED PORTRAIT PROMPTS.", raster.LABEL_DIM)
    return img


def cast_sheet(style, results, round_label="CONCEPT ROUND 03"):
    img = raster.sheet(1216, 520, f"BRIEFING CAST - {STYLE_TITLE[style]}", round_label)
    raster.draw_text(img, 16, 38, "BRIEFING PORTRAITS AT 1.5X (NEAREST) WITH NAME PLATES",
                     raster.LABEL_DIM)
    for i, slug in enumerate(CAST):
        brief, hud = results[slug]
        fr = briefing_frame(slug, brief)
        fr = fr.resize((fr.width * 3 // 2, fr.height * 3 // 2), Image.NEAREST)
        img.alpha_composite(fr, (16 + i * 300, 52))
    raster.draw_text(img, 16, 384, "HUD RADIO PORTRAITS 72X72 (1X)", raster.LABEL_DIM)
    for i, slug in enumerate(CAST):
        brief, hud = results[slug]
        MET.well(img, (16 + i * 300, 398, 16 + i * 300 + 71, 398 + 71))
        img.alpha_composite(hud, (16 + i * 300, 398))
        raster.draw_text(img, 100 + i * 300, 400, CAST[slug]["name"][:18], raster.LABEL)
        raster.draw_text(img, 100 + i * 300, 412, CAST[slug]["role"][:18], raster.LABEL_DIM)
    for i, line in enumerate(STYLE_NOTE[style]):
        raster.draw_text(img, 16 + (i // 2) * 600, 484 + (i % 2) * 12, line, raster.LABEL)
    return img


def main():
    wanted = [a for a in sys.argv[1:] if a in CAST] or list(CAST)
    results = {"a": {}, "b": {}}
    for slug in CAST:
        need_file = slug in wanted
        if not need_file and len(wanted) != len(CAST):
            continue
        bust = render_bust(slug)
        for style, fn in (("a", style_a), ("b", style_b)):
            brief, hud = fn(slug, bust)
            results[style][slug] = (brief, hud)
            out = out_path(CHAR_DIR / slug / "concept", f"portrait-r03-{style}.png")
            out.parent.mkdir(parents=True, exist_ok=True)
            character_sheet(slug, style, brief, hud).convert("RGB").save(out, optimize=True)
            print("wrote", out.relative_to(ROOT))
    if len(results["a"]) == len(CAST):
        for style in ("a", "b"):
            out = out_path(CHAR_DIR / "concept", f"cast-r03-{style}.png")
            out.parent.mkdir(parents=True, exist_ok=True)
            cast_sheet(style, results[style]).convert("RGB").save(out, optimize=True)
            print("wrote", out.relative_to(ROOT))


if __name__ == "__main__":
    main()
