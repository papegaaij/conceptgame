#!/usr/bin/env python3
"""Concept round 30 - Level 08's new props, two variants each (user decision D6 = a of M5 part B;
design/campaign/act-2-homefront/level-08-neon-skyline, Ground targets, Secrets, Layout).

Outputs (design/campaign/act-2-homefront/level-08-neon-skyline/concept/):
  billboard-r30-a.png / .gif  the flickering billboard "LAGOS NEVER SLEEPS" (a ground trigger on
                              a low roof: 3 hits topple it and its hidden CDF crate drops), variant
                              A "neon sky-sign": a dark panel tilted up toward the aircar lanes on a
                              steel A-frame, red neon "LAGOS" over cool-white "NEVER SLEEPS", four
                              floodlights on the top edge, an amber/black hazard catwalk and foot
                              plates at its front (the matte loot cue); hit: scorched, a tube and a
                              floodlight dead; toppled: the panel down on the frame, the struts
                              snapped, the CDF floor stash under the catwalk sprung open
  billboard-r30-b.png / .gif  variant B "LED screen": a flat video screen in a thick bezel with a
                              sun hood, barely tilted, on a hazard-striped plinth with a CDF service
                              cabinet (amber/black lid) at its foot; beige pixel text on a dim rust
                              screen with scanlines; the flicker is a glitch band and a rolling dark
                              bar; hit: a crack star and dead pixel blocks; toppled: the screen face
                              down (its ribbed back), the cabinet's lid thrown open
  traffic-r30-a.png / .gif    the low-air traffic (D2 = a: scenery, shots pass through), variant A
                              "wedge cars": a wedge sedan (12x20) and a box van (14x24) on four
                              corner lift ducts, and a CDF gunship (32x40, twin ducted fans, stub
                              wings, chin gun) in muted olive-grey
  traffic-r30-b.png / .gif    variant B "pods": a teardrop sedan (12x20) with a bubble canopy and
                              one rear thruster, an evacuee minibus (14x26, a lit window band), and a
                              CDF armoured hauler (30x48, four tilt ducts, door-gun blisters)

Round 30 closed (2026-10-07): a chosen for both (the neon sky-sign, the wedge cars and CDF
gunship; produced by tools/art/billboard.py and tools/art/l08_traffic.py); b's sheets and GIFs were
moved to concept/rejected/ (a rerun writes them to concept/ again, so move them back there).

Proposed sizes (Level 08's data has none yet; B4 writes it): billboard sprite 76x52 with a 64x36
hit box (the panel), frames `billboard_0..2` (intact, hit, toppled) and an additive flicker
`billboard-glow_0..7` (0-3 intact: lit, buzz, dim, off; 4-7 the same after a hit), the cycle 16
steps at 8 fps; traffic pieces on `low-air` with `headings: 16` (`_0` up the screen, clockwise).

Each billboard sheet: the frames and glows at 1x and 3x on a roof plate, the flicker cycle, then
three play-field frames at 1x over the chosen megacity (parallax-r03-b, tools/concept/parallax_r03.py
imported unchanged; atmosphere `light`, its own darts and turret left out) with the production
player ship, its shots, the hit spark and the crate pickup: approaching while a burst passes beside
it, the first hit (white flash), toppled with the crate drifting free; plus a 2x crop. The billboard
stands on a flat low roof (drawn without lean, user decision D1 = a). The GIF plays 5.2 s.
Each traffic sheet: the craft at 1x heading up and down, five of the 16 headings at 3x, then three
play-field frames (atmosphere `clear` and `light`): southbound streams over both avenues and
northbound cars, the CDF craft heading north, Skitters coming down; the player's shots pass over the
traffic with no spark and kill a Skitter; plus a 2x crop. The GIF plays 4 s.

Models: top-down SDF renders (render/sdf.py, the fixed top-left key light, 1 world unit = 1 px, 6x
or 8x supersampled, 32 colours), the loot amber with hazard stripes and the 1 px light rim of
ground_targets.py on the billboard only; the lettering and the screen are 2D pixel art on the
panel's projected face, the glows 2D light fields added after the sprites (premultiplied,
additive), as the game would. The traffic is muted (82 % brightness, 75 % saturation, no rim, no
glint, no loot or enemy hues) and casts the low-air shadow (9, 13).

Run: python3 tools/concept/props_r30.py [billboard] [traffic] [a] [b]   (no args = all; about 3 min)
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageFilter

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import ground_targets as gt  # noqa: E402  (hazard stripes, light rim, scorch)
import parallax_r02 as r02  # noqa: E402
import parallax_r03 as r03  # noqa: E402  (the chosen megacity, parallax-r03-b)
import props_r25 as p25  # noqa: E402  (glow fields, additive paste, plate strips)
from render import raster, sdf, sprite  # noqa: E402
from render.config import FIELD_H, FIELD_W, ROOT  # noqa: E402
from render.enemy_rigs import write_gif  # noqa: E402
from render.palette import B  # noqa: E402
from render.sdf import (Material, panel_lines, sd_box, sd_capsule, sd_cylinder_z,  # noqa: E402
                        sd_ellipsoid, sd_sphere, union)

OUT = ROOT / "design" / "campaign" / "act-2-homefront" / "level-08-neon-skyline" / "concept"
SPRITES = ROOT / "assets" / "sprites"
COLOURS = 32
GAP = 24
ROOF_PLATE = (24, 26, 52, 255)                 # a low roof in the megacity's UI PANELS ramp
NIGHT_PLATE = (12, 14, 34, 255)
GROUND_SPEED, LOW_AIR = 140.0, 1.35
SHOT_SPEED = 700.0                             # scatter vulcan (design/player/weapons)
SHIP_Y = 470
PICKUP_DRIFT = 40.0                            # design/player/data.yaml pickup_drift_speed

# --------------------------------------------------------------------------- shared helpers

img = p25.img
add = p25.add
over = p25.over
additive = p25.additive
plate_strip = p25.plate_strip


def render(scene, mats, size, factor=None):
    w, h = size
    factor = factor or (8 if max(w, h) <= 64 else 6)
    hi = sdf.render(scene, mats, (w * factor, h * factor), float(w), z_top=float(max(w, h)), steps=160)
    return np.array(sprite.make_sprite(hi, factor, COLOURS, crisp=60)).astype(np.float64)


def rot_z(p, deg):
    """Model space of a model turned ``deg`` clockwise on the screen (world y up)."""
    a = np.radians(deg)
    q = p.copy()
    q[:, 0] = p[:, 0] * np.cos(a) - p[:, 1] * np.sin(a)
    q[:, 1] = p[:, 0] * np.sin(a) + p[:, 1] * np.cos(a)
    return q


def ring(p, c, r_out, r_in, h):
    return np.maximum(sd_cylinder_z(p, c, r_out, h), -sd_cylinder_z(p, c, r_in, h + 2))


def bake_shadow(arr, dx, dy, opacity=0.42):
    """A ground prop's own short shadow (art direction: tall parts cast (6, 8)) under the sprite."""
    a = arr[..., 3] > 0
    sh = np.zeros_like(a)
    sh[dy:, dx:] = a[:-dy, :-dx]
    out = arr.copy()
    put = sh & ~a
    out[put, :3] = 0
    out[put, 3] = 255 * opacity
    return out


def soft(mask, radius):
    im = Image.fromarray((np.clip(mask, 0, 1) * 255).astype(np.uint8), "L")
    return np.array(im.filter(ImageFilter.GaussianBlur(radius))).astype(np.float64) / 255


def flash_white(spr, k=0.65):
    a = np.array(spr).astype(np.float64)
    a[..., :3] = a[..., :3] * (1 - k) + 255 * k
    return img(a)


def load(name):
    return Image.open(SPRITES / f"{name}.png").convert("RGBA")


# --------------------------------------------------------------------------- pixel lettering

FONT4 = {   # 4x5 caps for the billboard's small line
    "N": ["#..#", "##.#", "#.##", "#..#", "#..#"], "E": ["####", "#...", "###.", "#...", "####"],
    "V": ["#..#", "#..#", "#..#", ".##.", ".##."], "R": ["###.", "#..#", "###.", "#.#.", "#..#"],
    "S": [".###", "#...", ".##.", "...#", "###."], "L": ["#...", "#...", "#...", "#...", "####"],
    "P": ["###.", "#..#", "###.", "#...", "#..."], "A": [".##.", "#..#", "####", "#..#", "#..#"],
    "G": [".###", "#...", "#.##", "#..#", ".###"], "O": [".##.", "#..#", "#..#", "#..#", ".##."],
}


def text_mask(text, w, h, x, y, scale=1, font=FONT4):
    """Pixel mask of ``text`` in the 4x5 font (``scale`` 2 doubles it), a space 2 px."""
    m = np.zeros((h, w), bool)
    cx = x
    for ch in text:
        if ch == " ":
            cx += 2 * scale
            continue
        for r, line in enumerate(font[ch]):
            for c, v in enumerate(line):
                if v == "#":
                    m[y + r * scale:y + (r + 1) * scale, cx + c * scale:cx + (c + 1) * scale] = True
        cx += 5 * scale
    return m


def text_mask5(text, w, h, x, y):
    """Pixel mask of ``text`` in the sheets' 5x7 font (render/raster.py)."""
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    raster.draw_text(im, x, y, text, (255, 255, 255))
    return np.array(im)[..., 3] > 0


# --------------------------------------------------------------------------- billboard

BB = (76, 52)                  # sprite; the hit box is the panel
BB_HIT = (64, 36)
OX, OY = -2.0, 2.0             # the model sits up-left of the sprite centre (room for its shadow)
FACE = (5, 7, 67, 33)          # the panel's face, projected: cols x0..x1, rows y0..y1
HITS = 3
FLICKER = {                    # 16 steps at 8 fps: glow frame per step (0 lit, 1 buzz, 2 dim, 3 off)
    "intact": [0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 2, 3, 2, 0],
    "hit": [4, 4, 5, 4, 7, 4, 4, 5, 7, 7, 4, 6, 4, 7, 5, 4],
}

STEEL_D, STEEL, FACE_M, BEZEL, HAZ, OLIVE, LAMP, VOID, CONCRETE, BACK, LAMP_OFF = range(11)


def bb_materials():
    return [
        Material((0.22, 0.24, 0.34), metal=0.5, shininess=40, spec=0.4),
        Material((0.44, 0.47, 0.62), metal=0.6, shininess=50, spec=0.5),
        Material((0.07, 0.06, 0.09), metal=0.2, shininess=70, spec=0.6),
        Material((0.17, 0.18, 0.26), metal=0.4, shininess=40, spec=0.4),
        Material((0.98, 0.66, 0.14), metal=0.0, shininess=8, spec=0.05, pattern=gt.hazard_stripes),
        Material((0.40, 0.43, 0.33), metal=0.2, shininess=25, spec=0.3),            # CDF olive
        Material((0.3, 0.26, 0.2), emission=(1.5, 1.3, 0.85)),                    # floodlight
        Material((0.03, 0.03, 0.04), shininess=4, spec=0.0),
        Material((0.30, 0.30, 0.37), metal=0.0, shininess=10, spec=0.1),
        Material((0.30, 0.32, 0.44), metal=0.5, shininess=40, spec=0.4, pattern=panel_lines(0.25, 0.08, 0.7)),
        Material((0.28, 0.26, 0.24), metal=0.3, shininess=40, spec=0.4),          # a dead floodlight
    ]


def tilted(q, c, half, angle, slew=0.0):
    """Box ``half`` = (x, along, thick) tilted ``angle`` deg up toward +y about its centre ``c``,
    optionally slewed about z. Returns the distance and the local (x, along, normal) coordinates."""
    d = q - np.array(c)
    if slew:
        d = rot_z(d, slew)
    a = np.radians(angle)
    u = d[:, 1] * np.cos(a) + d[:, 2] * np.sin(a)
    v = -d[:, 1] * np.sin(a) + d[:, 2] * np.cos(a)
    loc = np.stack([d[:, 0], u, v], axis=-1)
    return sd_box(loc, (0, 0, 0), half, 0.5), loc


def face_paint(loc, hx, hu, bezel, face=FACE_M):
    inner = (np.abs(loc[:, 0]) < hx - bezel) & (np.abs(loc[:, 1]) < hu - bezel) & (loc[:, 2] > 0)
    return np.where(inner, face, BEZEL)


A_TILT = 32.0
A_HALF = (31.0, 13.0 / np.cos(np.radians(A_TILT)), 1.2)
A_LO = 4.0                                        # the panel's lower edge height
A_C = (0.0, 4.0, A_LO + A_HALF[1] * np.sin(np.radians(A_TILT)))


def board_a(state):
    """Neon sky-sign: the panel tilted up toward the aircar lanes on a steel A-frame (two legs, a
    brace), floodlights on its top edge, a hazard-striped catwalk with foot plates at its front.
    state 0 intact, 1 hit (a floodlight dead), 2 toppled: the panel down and slewed on its broken
    frame, the catwalk kicked aside and the CDF floor stash under it sprung open."""
    def scene(p):
        q = p - np.array([OX, OY, 0.0])
        items = [(sd_box(q, (0, 19.0, 0.6), (30.0, 2.0, 0.8), 0.4), CONCRETE),
                 (sd_box(q, (0, -10.0, 0.6), (30.0, 2.0, 0.8), 0.4), CONCRETE)]
        if state < 2:
            d, loc = tilted(q, A_C, A_HALF, A_TILT)
            items.append((d, face_paint(loc, A_HALF[0], A_HALF[1], 1.6)))
            top_y = A_C[1] + A_HALF[1] * np.cos(np.radians(A_TILT))
            top_z = A_C[2] + A_HALF[1] * np.sin(np.radians(A_TILT))
            for sx in (-1, 1):
                items += [(sd_capsule(q, (sx * 24, top_y - 1, top_z - 2), (sx * 24, 19.5, 0.5), 1.3), STEEL),
                          (sd_capsule(q, (sx * 24, -9.0, 0.5), (sx * 24, -8.0, A_LO), 1.1), STEEL)]
            items.append((sd_capsule(q, (-24, 18.5, 8.0), (24, 18.5, 8.0), 0.9), STEEL_D))
            for k, lx in enumerate((-21.0, -7.0, 7.0, 21.0)):
                lit = LAMP if (state == 0 or k != 2) else LAMP_OFF
                items += [(sd_capsule(q, (lx, top_y, top_z + 0.5), (lx, top_y - 3.0, top_z + 2.0), 0.6), STEEL_D),
                          (sd_box(q, (lx, top_y - 3.2, top_z + 2.0), (1.6, 1.0, 0.8), 0.3), STEEL_D),
                          (sd_box(q, (lx, top_y - 4.0, top_z + 1.6), (1.1, 0.4, 0.4), 0.2), lit)]
            items.append((sd_box(q, (0, -12.6, 1.4), (29.0, 3.0, 0.6), 0.3), HAZ))           # catwalk
            items += [(sd_box(q, (sx * 24, -9.0, 1.0), (3.2, 3.0, 0.7), 0.3), HAZ) for sx in (-1, 1)]
            items.append((sd_capsule(q, (-28, -15.8, 3.2), (28, -15.8, 3.2), 0.5), STEEL))  # rail
        else:
            d, loc = tilted(q, (2.0, 6.5, 4.5), A_HALF, 10.0, slew=-9.0)
            items.append((d, face_paint(loc, A_HALF[0], A_HALF[1], 1.6)))                   # face up, dark
            for sx, ln in ((-1, 7.0), (1, 4.5)):                                          # snapped legs
                items.append((sd_capsule(q, (sx * 24, 19.5, 0.5), (sx * 24 + sx * 2.0, 17.0, ln), 1.3), STEEL))
            items.append((sd_capsule(q, (-24, 19.0, 3.0), (-6, 21.0, 2.0), 0.9), STEEL_D))
            items.append((sd_box(rot_z(q - np.array([-20.0, -14.0, 0.0]), 14.0), (0, 0, 1.0), (13.0, 2.2, 0.5), 0.3), HAZ))
            items += [(sd_box(q, (sx * 24, -9.0, 0.9), (2.6, 2.6, 0.6), 0.3), HAZ) for sx in (-1, 1)]
            stash = np.maximum(sd_box(q, (6.0, -12.0, 0.8), (9.0, 5.0, 1.0), 0.4),
                               -sd_box(q, (6.0, -12.0, 1.0), (6.5, 3.0, 2.0), 0.2))
            items += [(stash, HAZ), (sd_box(q, (6.0, -12.0, 0.2), (6.5, 3.0, 0.3), 0.1), VOID),
                      (sd_box(rot_z(q - np.array([19.0, -13.0, 0.0]), -20.0), (0, 0, 1.2), (3.5, 4.5, 0.5), 0.3), OLIVE)]
            for sx, sy, ang in ((-10, 1, 30), (14, 24, -50), (-30, 9, 75), (24, 0, 10)):    # glass and tube bits
                items.append((sd_box(rot_z(q - np.array([sx, sy, 0.0]), ang), (0, 0, 0.4), (1.6, 0.6, 0.4), 0.2), BEZEL))
        return union(*items, k=0.3)
    return scene


B_TILT = 18.0
B_HALF = (31.5, 13.0 / np.cos(np.radians(B_TILT)), 1.8)
B_LO = 6.0
B_C = (0.0, 4.0, B_LO + B_HALF[1] * np.sin(np.radians(B_TILT)))


def board_b(state):
    """LED screen: a thick-bezelled screen barely tilted, a sun hood on its top edge, a hazard-
    striped plinth and a CDF service cabinet (olive, amber/black lid) at its foot. state 2: the
    screen face down and slewed (its ribbed back up), the posts snapped, the cabinet lid thrown
    open."""
    def scene(p):
        q = p - np.array([OX, OY, 0.0])
        items = [(sd_box(q, (-17.0, -13.0, 1.0), (11.0, 2.8, 1.2), 0.4), HAZ),
                 (sd_box(q, (21.0, -13.0, 1.0), (7.0, 2.8, 1.2), 0.4), HAZ)]
        if state < 2:
            d, loc = tilted(q, B_C, B_HALF, B_TILT)
            items.append((d, face_paint(loc, B_HALF[0], B_HALF[1], 2.2)))
            top_y = B_C[1] + B_HALF[1] * np.cos(np.radians(B_TILT))
            top_z = B_C[2] + B_HALF[1] * np.sin(np.radians(B_TILT))
            items.append((sd_box(q, (0, top_y - 1.0, top_z + 1.8), (32.0, 1.8, 0.7), 0.3), STEEL))  # sun hood
            items += [(sd_capsule(q, (sx * 26.0, top_y + 0.5, 0.3), (sx * 26.0, top_y - 0.5, top_z - 1), 1.4), STEEL_D)
                      for sx in (-1, 1)]
            items.append((sd_capsule(q, (30.0, top_y, top_z - 2), (33.0, top_y + 3.0, 0.5), 0.7), STEEL_D))  # conduit
            items += [(sd_box(q, (6.0, -14.0, 2.6), (6.0, 3.6, 2.6), 0.5), OLIVE),
                      (sd_box(q, (6.0, -14.0, 5.3), (5.6, 3.2, 0.4), 0.3), HAZ)]
        else:
            d, loc = tilted(q, (-1.5, 5.0, 2.2), B_HALF, 4.0, slew=7.0)
            items.append((d, np.where(loc[:, 2] > 0.8, BACK, BEZEL)))
            items += [(sd_box(rot_z(q - np.array([sx * 25.0, 17.0, 0.0]), 25.0 * sx), (0, 0, 1.0), (1.5, 4.0, 1.0), 0.4), STEEL_D)
                      for sx in (-1, 1)]
            for fx in (-18.0, -6.0, 6.0, 18.0):                                           # heat-sink ribs
                items.append((sd_box(rot_z(q - np.array([-1.5, 5.0, 0.0]), 7.0), (fx, 0, 4.0), (1.0, 11.0, 0.6), 0.3), STEEL))
            cab = np.maximum(sd_box(q, (6.0, -14.0, 2.6), (6.0, 3.6, 2.6), 0.5),
                             -sd_box(q, (6.0, -14.0, 4.0), (4.6, 2.4, 2.0), 0.2))
            items += [(cab, OLIVE), (sd_box(q, (6.0, -14.0, 2.4), (4.6, 2.4, 0.3), 0.1), VOID),
                      (sd_box(rot_z(q - np.array([6.0, -20.5, 0.0]), 6.0), (0, 0, 0.6), (5.6, 2.6, 0.4), 0.3), HAZ)]
            for sx, sy, ang in ((-30, -3, 20), (29, 12, -35), (-12, -9, 70), (17, -7, 5)):
                items.append((sd_box(rot_z(q - np.array([sx, sy, 0.0]), ang), (0, 0, 0.4), (1.5, 0.7, 0.4), 0.2), BEZEL))
        return union(*items, k=0.3)
    return scene


def face_rect(variant):
    x0, y0, x1, y1 = FACE
    return (x0, y0 + (3 if variant == "b" else 0), x1, y1)       # B's sun hood covers the top rows


def lettering(variant):
    """Masks on the sprite: (main, second line) for A's neon, (text, screen) for B's LED face."""
    w, h = BB
    x0, y0, x1, y1 = face_rect(variant)
    if variant == "a":
        big = text_mask("LAGOS", w, h, x0 + 8, y0 + 3, scale=2)
        small = text_mask("NEVER SLEEPS", w, h, x0 + 3, y0 + 16)
        return big, small
    screen = np.zeros((h, w), bool)
    screen[y0 + 2:y1 - 2, x0 + 2:x1 - 2] = True
    txt = text_mask5("LAGOS", w, h, x0 + 17, y0 + 2) | text_mask("NEVER SLEEPS", w, h, x0 + 3, y0 + 11)
    return txt, screen


DEAD_A = (np.s_[7:21, 51:60], np.s_[22:29, 48:58])        # A's dead tubes after a hit: the last S, "PS"
DEAD_B = [(24, 11, 6, 4), (40, 18, 5, 5), (55, 14, 4, 3), (16, 23, 7, 2)]   # B's dead pixel blocks (x, y, w, h)
HIT_AT = {"a": (49, 16), "b": (44, 17)}


def bb_frames(variant):
    """billboard_0..2 (intact, hit, toppled) as float RGBA arrays, with the light rim, the dark
    lettering or screen and a baked shadow."""
    model = board_a if variant == "a" else board_b
    mats = bb_materials()
    frames = []
    for state in range(3):
        arr = render(model(state), mats, BB)
        if state < 2:
            m1, m2 = lettering(variant)
            if variant == "a":
                arr[m1, :3] = (92, 30, 36)                     # unlit red tube glass
                arr[m2, :3] = (78, 80, 96)                     # unlit white tubes
            else:
                arr[m2, :3] = (20, 19, 26)
                arr[m1, :3] = (34, 31, 36)
        if state == 1:
            hx, hy = HIT_AT[variant]
            arr = gt.scorch(arr, [(hx, hy, 4.5), (hx - 9, hy + 6, 3.0)], 31)
            if variant == "a":
                for k in range(6):                             # a crack across the panel
                    arr[hy - 3 + k, hx + 2 + k // 2, :3] = gt.SCORCH
            else:
                for dx, dy in ((1, 0), (2, -1), (3, -1), (-1, 1), (-2, 2), (0, -2), (1, -3), (-1, -1), (-3, 0), (2, 2), (3, 3)):
                    arr[hy + dy, hx + dx, :3] = (150, 150, 165)   # crack star in the glass
                for x, y, ww, hh in DEAD_B:
                    arr[y:y + hh, x:x + ww, :3] = (8, 8, 10)
        if state == 2:
            if variant == "a":                                 # the fallen face, its tubes broken
                m1, m2 = lettering("a")
                x0, y0, x1, y1 = FACE
                tubes = np.zeros(BB[::-1] + (4,), np.uint8)
                tubes[m1] = (82, 28, 34, 255)
                tubes[m2] = (66, 68, 82, 255)
                tubes[np.random.default_rng(33).random(BB[::-1]) < 0.3] = 0      # shattered
                face = Image.fromarray(tubes[y0:y1, x0:x1], "RGBA").rotate(9.0, Image.NEAREST, expand=True)
                spot = Image.new("RGBA", BB, (0, 0, 0, 0))
                cx, cy = 38 + OX + 2.0, 26 - OY - 6.5
                spot.alpha_composite(face, (int(round(cx - face.width / 2)), int(round(cy - face.height / 2))))
                t = np.array(spot).astype(np.float64)
                put = (t[..., 3] > 0) & (arr[..., 3] > 0) & (arr[..., :3].sum(axis=-1) < 120)
                arr[put, :3] = t[put, :3]
            arr = gt.scorch(arr, [(30, 26, 6.0), (52, 22, 4.0)], 32)
        arr = gt.light_rim(arr)
        frames.append(bake_shadow(arr, 5, 7))
    return frames


def bb_glows(variant):
    """billboard-glow_0..7: additive, premultiplied RGB float arrays (0-3 intact: lit, buzz, dim,
    off; 4-7 the same after a hit)."""
    w, h = BB
    m1, m2 = lettering(variant)
    out = []
    for hit in (False, True):
        a, b = m1.copy(), m2.copy()
        if variant == "a" and hit:
            for s in DEAD_A:
                a[s] = False
                b[s] = False
        dead = np.zeros((h, w), bool)
        if variant == "b" and hit:
            for x, y, ww, hh in DEAD_B:
                dead[y:y + hh, x:x + ww] = True
        for kind in range(4):
            g = np.zeros((h, w, 3))
            if variant == "a":
                red, white = np.array([228.0, 62.0, 52.0]), np.array([214.0, 222.0, 242.0])
                k_red = (1.0, 1.0, 0.45, 0.0)[kind]
                k_white = (1.0, 0.0, 0.5, 0.0)[kind]
                base_r, base_w = np.array([92.0, 30.0, 36.0]), np.array([78.0, 80.0, 96.0])
                g[a] += (red - base_r) * k_red
                g[b] += (white - base_w) * k_white
                halo = soft(a * k_red, 1.6)[..., None] * red * 0.42 + soft(b * k_white, 1.4)[..., None] * white * 0.22
                g += np.floor(halo / 24) * 24 * (~(a | b))[..., None]           # stepped halo
            else:
                x0, y0, x1, y1 = face_rect("b")
                rows = np.arange(h)[:, None] * np.ones((1, w))
                tt = np.clip((rows - y0) / max(1, y1 - y0), 0, 1)[..., None]
                screen_col = np.array([86.0, 36.0, 30.0]) * (1 - tt) + np.array([52.0, 22.0, 40.0]) * tt
                screen_col *= np.where(rows % 2 == 0, 1.0, 0.78)[..., None]          # scanlines
                k = (1.0, 1.0, 1.0, 0.0)[kind]
                scr = b & ~a & ~dead
                txt = a & ~dead
                g[scr] += (screen_col[scr] - 20) * k
                g[txt] += (np.array([236.0, 222.0, 196.0]) - 34) * k
                g[y0 + 3:y0 + 5, x1 - 7:x1 - 5] = np.array([210.0, 40.0, 36.0]) * (k > 0)   # the live dot
                if kind == 1:                                   # glitch: a band slides right, dimmed
                    band = np.s_[y0 + 9:y0 + 15]
                    g[band] = np.roll(g[band], 3, axis=1) * 0.7
                if kind == 2:                                   # a rolling dark bar
                    g[y0 + 4:y0 + 9] *= 0.15
                halo = soft((scr | txt) * k, 2.0)[..., None] * np.array([120.0, 60.0, 50.0]) * 0.35
                g += np.floor(halo / 12) * 12 * (~(scr | txt))[..., None]
            out.append(np.clip(g, 0, 255))
    return out


def glow_img(g):
    """An additive glow as a sprite: premultiplied on black (alpha = brightest channel)."""
    a = np.zeros(g.shape[:2] + (4,))
    a[..., :3] = g
    a[..., 3] = np.max(g, axis=-1)
    return img(a)


def lit(frame, glow):
    out = frame.copy()
    out[..., :3] = np.clip(out[..., :3] + glow * (out[..., 3:4] > 0), 0, 255)
    return img(out)


BB_TEXT = {
    "a": {"title": "BILLBOARD - ROUND 30 VARIANT A",
          "desc": ["NEON SKY-SIGN: A DARK PANEL TILTED UP TOWARD THE AIRCAR LANES ON A STEEL A-FRAME, RED NEON \"LAGOS\" OVER COOL-WHITE",
                   "\"NEVER SLEEPS\", FOUR FLOODLIGHTS ON ITS TOP EDGE, AN AMBER/BLACK HAZARD CATWALK AND FOOT PLATES (THE MATTE LOOT CUE).",
                   "THE NEON BUZZES AND DROPS OUT (THE SECRET'S BLINK); A HIT SCORCHES IT AND KILLS A TUBE AND A LAMP; TOPPLED, THE CDF FLOOR STASH OPENS."]},
    "b": {"title": "BILLBOARD - ROUND 30 VARIANT B",
          "desc": ["LED SCREEN: A FLAT VIDEO SCREEN IN A THICK BEZEL WITH A SUN HOOD, BARELY TILTED, ON A HAZARD-STRIPED PLINTH WITH A CDF",
                   "SERVICE CABINET (OLIVE, AMBER/BLACK LID) AT ITS FOOT; BEIGE PIXEL TEXT ON A DIM RUST SCREEN WITH SCANLINES. THE FLICKER IS A",
                   "GLITCH BAND AND A ROLLING DARK BAR; A HIT CRACKS IT AND KILLS PIXEL BLOCKS; TOPPLED, IT LIES FACE DOWN AND THE CABINET IS OPEN."]},
}

# --------------------------------------------------------------------------- the megacity


class _NoActors:
    def draw(self, f, src):
        return Image.new("RGBA", (FIELD_W, FIELD_H), (0, 0, 0, 0)), []


class City:
    """The chosen megacity scene (parallax-r03-b) without its darts, turret and flat cars, with a
    flat low roof (drawn without lean, D1 = a) for the billboard; its fog scaled per atmosphere."""
    ROOF_BY = 240                                  # the block row of the low roof (ground px)
    ROOF = (172, 308)
    PER = 560

    def __init__(self):
        sc = r03.get_scene("b")
        sc.cars = []
        sc.actors = _NoActors()
        for b in sc.buildings:
            b["turret"] = False
        by = self.ROOF_BY
        sc.buildings = [b for b in sc.buildings
                        if not (b["y0"] == by + sc.STREET_W + 4 and self.ROOF[0] - 4 <= b["x0"] < self.ROOF[1])]
        sc.buildings.insert(0, {"x0": self.ROOF[0], "x1": self.ROOF[1], "y0": by + 12, "y1": by + 76, "h": 0.0,
                                "seed": 3, "turret": False, "beacon": False, "park": False, "garden": False})
        self.sc = sc
        self.fog_full = np.array(sc.fog).astype(np.float64)
        self.roof_centre = ((self.ROOF[0] + self.ROOF[1]) / 2, by + 44)

    def layers(self, t, fog):
        f = t * r02.FPS
        a = self.fog_full.copy()
        a[..., 3] *= fog
        self.sc.fog = Image.fromarray(a.astype(np.uint8), "RGBA")
        return self.sc.layer_images(f)

    def roof_y(self, t):
        """The low roof's centre on the screen (rows down) at scene time t, one repeat."""
        return self.roof_centre[1] + GROUND_SPEED * t - self.PER


_city = None


def city():
    global _city
    if _city is None:
        _city = City()
    return _city


# --------------------------------------------------------------------------- billboard scene

class BillboardScene:
    """Scene time t: t=0 when the billboard's centre enters at y=-30; city time = t + T."""

    def __init__(self, variant):
        self.variant = variant
        self.city = city()
        self.T = (-30 - (self.city.roof_centre[1] - City.PER)) / GROUND_SPEED
        self.frames = bb_frames(variant)
        self.glows = bb_glows(variant)
        self.ship = load("ship_2")
        self.flame = [load(f"engine-flame_{i}") for i in range(9)]
        self.shot = load("scatter-vulcan-shot_0")
        self.spark = [load(f"ballistic-impact_{i}") for i in range(4)]
        self.crate = [load(f"pickup-crate_{i}") for i in range(8)]
        self.glint = [load(f"glint-secret_{i}") for i in range(4)]
        self.boom = [load(f"explosion-small_{i}") for i in range(12)]
        self.shots, self.hits = self.simulate()
        self.spent = self.hits[-1][0]

    def bb_pos(self, t):
        return self.city.roof_centre[0], -30 + GROUND_SPEED * t

    def ship_x(self, t):
        bx = self.bb_pos(t)[0]
        if t < 0.95:
            return bx + 62
        if t < 1.25:
            return bx + 62 * (1 - (t - 0.95) / 0.3)
        return bx

    SHOT_TIMES = [0.3 + 0.08 * k for k in range(8)] + [1.15, 1.75, 2.35]

    def simulate(self):
        shots, hits = [], []
        for t0 in self.SHOT_TIMES:
            x, y0 = self.ship_x(t0), SHIP_Y - 24
            t_end, hit = t0 + (y0 + 20) / SHOT_SPEED, False
            if len(hits) < HITS:
                for k in range(1, 400):
                    t = t0 + k / 240
                    y = y0 - SHOT_SPEED * (t - t0)
                    if y < -20:
                        break
                    bx, by = self.bb_pos(t)
                    if abs(x - bx) < BB_HIT[0] / 2 and abs(y - (by - 2)) < BB_HIT[1] / 2:
                        t_end, hit = t, True
                        hits.append((t, x, y))
                        break
            shots.append((t0, x, t_end, hit))
        return shots, hits

    def hits_by(self, t):
        return sum(1 for h in self.hits if h[0] <= t)

    def frame(self, t, fog=0.45):
        L = self.city.layers(t + self.T, fog)
        base = L["ground"].copy()
        base.alpha_composite(L["shadows"])
        base = np.array(base).astype(np.float64)
        bx, by = self.bb_pos(t)
        hits = self.hits_by(t)
        state = 0 if hits == 0 else (1 if hits < HITS else 2)
        spr = img(self.frames[state])
        if any(th <= t < th + 0.06 for th, _, _ in self.hits):
            spr = flash_white(spr)
        x0, y0 = bx - BB[0] / 2, by - BB[1] / 2
        base = over(base, spr, x0, y0)
        if state < 2:
            step = int(t * 8) % 16
            add(base, self.glows[FLICKER["intact" if state == 0 else "hit"][step]], x0, y0)
            g = t % 1.5
            if g < 0.3:
                base = additive(base, self.glint[min(3, int(g / 0.075))], bx - 11.5, by - 11.5)
        out = img(base)
        out.alpha_composite(L["low-air"])
        base = np.array(out).astype(np.float64)
        if t >= self.spent:                                       # the crate drifts free
            s = t - self.spent
            px, py = self.bb_pos(self.spent)
            py += -12 + PICKUP_DRIFT * s
            if s < 0.48:
                base = additive(base, self.boom[min(11, int(s * 25))], px - 20, py + 2 - 20)
            base = over(base, self.crate[int(t * 10) % 8], px + 4 - 17, py - 17)
        for t0, x, t_end, _ in self.shots:
            if t0 <= t < t_end:
                y = SHIP_Y - 24 - SHOT_SPEED * (t - t0)
                base = additive(base, self.shot, x - 4, y - 10)
        for th, x, y in self.hits:
            if th <= t < th + 0.2:
                base = additive(base, self.spark[min(3, int((t - th) / 0.05))], x - 11, y - 11)
        sx = self.ship_x(t)
        base = additive(base, self.flame[int(t * 20) % 9], sx - 6, SHIP_Y + 18)
        base = over(base, self.ship, sx - 24, SHIP_Y - 24)
        out = img(base)
        out.alpha_composite(L["high-air"])
        return out.convert("RGB")


def billboard_sheet(variant):
    sc = BillboardScene(variant)
    fr, gl = sc.frames, sc.glows
    shown = [img(fr[0]), lit(fr[0], gl[0]), lit(fr[0], gl[1]), lit(fr[0], gl[2]),
             lit(fr[1], gl[4]), lit(fr[1], gl[5]), img(fr[2])]
    small = recolour_plate(plate_strip(shown + [glow_img(g) for g in gl[:4]], 1))
    big_set = [shown[0], shown[1], shown[4], shown[6]]
    labels = ["BILLBOARD_0 76X52 (UNLIT)", "_0 + GLOW_0 (LIT)", "_1 HIT + GLOW_4", "_2 TOPPLED"]
    big = recolour_plate(plate_strip(big_set, 3))
    glows_big = recolour_plate(plate_strip([glow_img(g) for g in gl], 2), NIGHT_PLATE)
    cycles = [recolour_plate(plate_strip([lit(fr[0 if state == "intact" else 1], gl[k]) for k in FLICKER[state]], 1))
              for state in ("intact", "hit")]
    t_pass = 0.62
    t_hit = sc.hits[0][0] + 0.02
    t_down = sc.spent + 0.35
    shots = [sc.frame(t) for t in (t_pass, t_hit, t_down)]
    t_crop = sc.hits[1][0] + 0.08
    bx, by = sc.bb_pos(t_crop)
    crop = sc.frame(t_crop).crop((int(bx) - 60, int(by) - 90, int(bx) + 60, int(by) + 180)).resize((240, 540), Image.NEAREST)
    width = 16 + 3 * (FIELD_W + 12) + crop.width + 16
    height = (74 + small.height + 22 + big.height + 34 + glows_big.height + 30 + 2 * (cycles[0].height + 20)
              + 14 + FIELD_H + 60)
    out = raster.sheet(width, height, BB_TEXT[variant]["title"],
                       "LEVEL 08 SECRET PROP CONCEPT - GROUND TRIGGER ON A LOW ROOF, 3 HITS; THE CDF CRATE DROPS")
    for k, line in enumerate(BB_TEXT[variant]["desc"]):
        raster.draw_text(out, 16, 34 + 12 * k, line, raster.LABEL if k == 0 else raster.LABEL_DIM)
    y = 74
    raster.draw_text(out, 16, y, "1X", raster.LABEL_DIM)
    out.alpha_composite(small, (40, y))
    raster.draw_text(out, 40 + small.width + 12, y + 8, "INTACT UNLIT, LIT, BUZZ, DIM; HIT LIT, HIT FLICKERING; TOPPLED;", raster.LABEL_DIM)
    raster.draw_text(out, 40 + small.width + 12, y + 20, "THEN THE FOUR INTACT GLOWS ALONE (ADDITIVE, ON BLACK)", raster.LABEL_DIM)
    y += small.height + 10
    raster.draw_text(out, 16, y, "3X ON A ROOF PLATE (THE SHADOW (5,7) IS BAKED IN; THE 1 PX LIGHT RIM IS THE LOOT RIM)", raster.LABEL_DIM)
    out.alpha_composite(big, (16, y + 12))
    x = 16
    for f, name in zip(big_set, labels):
        raster.draw_text(out, x + 4, y + 16 + big.height, name, raster.LABEL)
        x += (f.width + GAP) * 3
    notes = ["PROPOSED DATA (B4 DECIDES): A GROUND TRIGGER WITH",
             "SPRITE BILLBOARD (_0 INTACT, _1 HIT, _2 TOPPLED),",
             "SIZE [64, 36] (THE PANEL; THE SPRITE IS 76X52),",
             "HITS 3, REVEALS BILLBOARD CACHE, ON A LOW ROOF.",
             "",
             "THE FLICKER NEEDS A NEW LOOK: BILLBOARD-GLOW_0..7",
             "CYCLED BY THE STEP TABLE BELOW (TODAY A TRIGGER'S",
             "-GLOW IS ONE STILL FRAME), THE HIT SET FROM HIT 1.",
             "",
             "THE CRATE PICKUP SHOULD DROP AT THE STASH, 12 PX",
             "BELOW THE CENTRE. THE SECRET GLINT, THE HIT FLASH",
             "AND THE SPARK ARE THE GAME'S OWN SPRITES."]
    nx = 16 + big.width + 24
    for k, line in enumerate(notes):
        raster.draw_text(out, nx, y + 16 + 12 * k, line, raster.LABEL_DIM)
    y += 12 + big.height + 22
    raster.draw_text(out, 16, y, "BILLBOARD-GLOW_0..7 AT 2X: 0-3 INTACT (LIT, BUZZ, DIM, OFF), 4-7 THE SAME AFTER A HIT", raster.LABEL_DIM)
    out.alpha_composite(glows_big, (16, y + 12))
    y += 12 + glows_big.height + 18
    for state, strip in zip(("INTACT", "AFTER A HIT"), cycles):
        steps_txt = " ".join(str(k) for k in FLICKER["intact" if state == "INTACT" else "hit"])
        raster.draw_text(out, 16, y, f"FLICKER CYCLE {state}, 16 STEPS AT 8 FPS (2 S), GLOW FRAMES {steps_txt}", raster.LABEL_DIM)
        out.alpha_composite(strip, (16, y + 12))
        y += 12 + strip.height + 8
    y += 6
    raster.draw_text(out, 16, y, "PLAY FIELD 1X OVER THE CHOSEN MEGACITY (PARALLAX-R03-B, ATMOSPHERE LIGHT) ON A FLAT LOW ROOF: "
                     f"T={t_pass:.1f} A BURST PASSES BESIDE IT, T={t_hit:.1f} FIRST HIT (FLASH, SPARK), "
                     f"T={t_down:.1f} TOPPLED, THE CRATE DRIFTS FREE; 2X: AFTER THE SECOND HIT", raster.LABEL_DIM)
    for k, im in enumerate(shots):
        out.paste(im, (16 + k * (FIELD_W + 12), y + 12))
    out.paste(crop, (16 + 3 * (FIELD_W + 12), y + 12))
    path = OUT / f"billboard-r30-{variant}.png"
    out.convert("RGB").save(path, optimize=True)
    print(path.relative_to(ROOT))
    gif_frames = [sc.frame(float(t)).crop((60, 0, 420, FIELD_H)) for t in np.arange(-0.2, 5.0, 0.08)]
    gif = OUT / f"billboard-r30-{variant}.gif"
    write_gif(gif_frames, gif, fps=12.5)
    print(gif.relative_to(ROOT), f"{gif.stat().st_size / 1e6:.1f} MB")


def recolour_plate(im, plate=ROOF_PLATE):
    """plate_strip draws on the space plate; swap it for the roof (or night) plate."""
    a = np.array(im)
    sel = np.all(a == np.array(p25.PLATE), axis=-1)
    a[sel] = plate
    return Image.fromarray(a, "RGBA")


# --------------------------------------------------------------------------- traffic

PAINTS = [(0.46, 0.50, 0.64), (0.66, 0.60, 0.52), (0.54, 0.33, 0.35), (0.72, 0.73, 0.78), (0.38, 0.45, 0.43)]
GLASS, DARK, HEAD, TAIL, CDF, BAND, NAV_R, NAV_G, STROBE, WARM, ROTOR, STEEL_T = range(5, 17)


def tr_materials(strobe=True):
    mats = [Material(c, metal=0.35, shininess=50, spec=0.5) for c in PAINTS]
    mats += [Material((0.06, 0.08, 0.14), metal=0.9, shininess=110, spec=1.0),
             Material((0.10, 0.10, 0.14), metal=0.3, shininess=20, spec=0.2),
             Material((0.3, 0.3, 0.25), emission=(1.5, 1.4, 1.1)),
             Material((0.2, 0.03, 0.03), emission=(1.4, 0.15, 0.12)),
             Material((0.34, 0.37, 0.31), metal=0.3, shininess=30, spec=0.3, pattern=panel_lines(0.2, 0.07, 0.75)),
             Material((0.55, 0.57, 0.60), metal=0.2, shininess=30, spec=0.3),
             Material((0.2, 0.04, 0.04), emission=(1.6, 0.2, 0.2)),
             Material((0.04, 0.2, 0.08), emission=(0.2, 1.3, 0.4)),
             Material((0.3, 0.3, 0.3), emission=(1.6, 1.6, 1.6)) if strobe
             else Material((0.3, 0.3, 0.34), metal=0.4, shininess=60, spec=0.6),
             Material((0.2, 0.15, 0.1), emission=(1.1, 0.9, 0.6)),
             Material((0.2, 0.2, 0.25), metal=0.3, shininess=20, spec=0.2),
             Material((0.45, 0.47, 0.55), metal=0.7, shininess=60, spec=0.6)]
    return mats


def sedan_a(paint):
    """Wedge sedan facing up: a low wedge body, a dark canopy, four corner lift ducts."""
    def scene(p):
        y = p[:, 1]
        h = 1.6 + 0.10 * (y + 9)                       # rises toward the back: a wedge
        body = sd_box(p, (0, 0, 1.2), (3.9, 8.6, 1.0), 0.8)
        body = np.maximum(body, p[:, 2] - h)
        items = [(body - 0.5, paint), (sd_ellipsoid(p, (0, -0.5, 2.6), (2.8, 3.8, 1.2)), GLASS)]
        for sx in (-1, 1):
            for sy in (-1, 1):
                items.append((ring(p, (sx * 4.2, sy * 5.8, 1.2), 1.7, 0.8, 0.6), DARK))
            items += [(sd_sphere(p, (sx * 2.6, 8.9, 1.6), 0.7), HEAD), (sd_sphere(p, (sx * 2.8, -8.9, 2.4), 0.7), TAIL)]
        return union(*items, k=0.3)
    return scene


def van_a(paint):
    """Box van facing up: a tall box body, a short nose, roof rails, four lift ducts."""
    def scene(p):
        items = [(sd_box(p, (0, -1.5, 2.2), (4.8, 9.0, 2.2), 0.9), paint),
                 (sd_box(p, (0, 8.6, 1.4), (4.4, 2.6, 1.4), 0.9), paint),
                 (sd_box(p, (0, 7.4, 2.9), (3.8, 1.0, 0.6), 0.4), GLASS)]
        items += [(sd_capsule(p, (sx * 3.6, -8.5, 4.6), (sx * 3.6, 5.0, 4.6), 0.4), STEEL_T) for sx in (-1, 1)]
        for sx in (-1, 1):
            for sy in (-1, 1):
                items.append((ring(p, (sx * 5.4, sy * 7.2, 1.4), 1.8, 0.9, 0.6), DARK))
            items += [(sd_sphere(p, (sx * 3.0, 11.0, 1.6), 0.7), HEAD), (sd_sphere(p, (sx * 3.6, -10.6, 3.0), 0.7), TAIL)]
        return union(*items, k=0.3)
    return scene


def gunship_a():
    """CDF gunship facing up: a fuselage with a glazed nose and a chin gun, two ducted fans on stub
    wings, a tail boom with a V tail, a pale band; nav lights on the fan rims, a strobe aft."""
    def scene(p):
        items = [(sd_capsule(p, (0, -9.0, 3.0), (0, 11.0, 3.0), 4.6, 3.8), CDF),
                 (sd_ellipsoid(p, (0, 13.0, 3.6), (3.0, 4.0, 2.6)), GLASS),
                 (sd_capsule(p, (0, -9.0, 3.0), (0, -18.0, 3.4), 1.8, 1.2), CDF),
                 (sd_box(p, (0, 2.0, 3.0), (11.0, 2.4, 0.8), 0.4), CDF),
                 (sd_box(p, (0, -3.0, 6.9), (2.6, 1.0, 0.6), 0.3), BAND),
                 (sd_capsule(p, (0, 16.0, 0.8), (0, 19.0, 0.8), 0.7), STEEL_T)]
        for sx in (-1, 1):
            items += [(ring(p, (sx * 10.5, 2.0, 3.0), 5.4, 4.2, 1.4), CDF),
                      (sd_cylinder_z(p, (sx * 10.5, 2.0, 2.6), 4.3, 0.2), ROTOR),
                      (sd_cylinder_z(p, (sx * 10.5, 2.0, 3.0), 1.1, 0.8), DARK),
                      (sd_capsule(p, (sx * 1.0, -16.5, 3.4), (sx * 4.5, -19.0, 5.2), 0.5), CDF),
                      (sd_capsule(p, (sx * 6.0, 5.0, 1.4), (sx * 6.0, 9.0, 1.4), 0.9), DARK)]
            items.append((sd_sphere(p, (sx * 15.6, 2.0, 3.4), 0.8), NAV_G if sx > 0 else NAV_R))
        items.append((sd_sphere(p, (0, -18.6, 4.4), 0.8), STROBE))
        return union(*items, k=0.3)
    return scene


def sedan_b(paint):
    """Teardrop sedan facing up: an ellipsoid lifting body, a bubble canopy, one rear thruster,
    two small fins."""
    def scene(p):
        items = [(sd_ellipsoid(p, (0, 0.5, 1.8), (4.6, 8.8, 1.9)), paint),
                 (sd_ellipsoid(p, (0, 2.0, 3.0), (2.6, 3.6, 1.6)), GLASS),
                 (ring(p, (0, -8.4, 1.8), 1.8, 0.9, 0.8), DARK)]
        items += [(sd_capsule(p, (sx * 3.2, -5.0, 2.4), (sx * 5.0, -8.0, 2.6), 0.5), paint) for sx in (-1, 1)]
        items += [(sd_sphere(p, (sx * 2.0, 9.0, 1.8), 0.7), HEAD) for sx in (-1, 1)]
        items += [(sd_sphere(p, (sx * 4.6, -7.8, 2.8), 0.6), TAIL) for sx in (-1, 1)]
        return union(*items, k=0.4)
    return scene


def minibus_b(paint):
    """Evacuee minibus facing up: a long rounded capsule with a lit window band (people inside), a
    rear thruster pair."""
    def scene(p):
        items = [(sd_capsule(p, (0, -8.5, 2.2), (0, 9.0, 2.2), 4.8), paint)]
        items += [(sd_box(p, (sx * 4.3, -0.5, 3.4), (0.8, 7.5, 0.8), 0.3), WARM) for sx in (-1, 1)]   # windows
        items += [(sd_box(p, (0, -1.0, 6.6), (2.0, 6.5, 0.4), 0.3), STEEL_T),
                  (sd_ellipsoid(p, (0, 10.0, 3.2), (3.2, 2.0, 1.5)), GLASS)]
        items += [(ring(p, (sx * 2.4, -12.2, 2.0), 1.4, 0.6, 0.6), DARK) for sx in (-1, 1)]
        items += [(sd_sphere(p, (sx * 2.6, 12.6, 1.8), 0.7), HEAD) for sx in (-1, 1)]
        items += [(sd_sphere(p, (sx * 4.4, -11.4, 3.0), 0.6), TAIL) for sx in (-1, 1)]
        return union(*items, k=0.3)
    return scene


def hauler_b():
    """CDF armoured hauler facing up: a long armoured box with a slit cab, four tilt ducts on
    sponsons, door-gun blisters, a cargo spine and a pale chevron; nav lights and a strobe."""
    def scene(p):
        items = [(sd_box(p, (0, -1.0, 3.6), (8.2, 18.5, 3.4), 1.8), CDF),
                 (sd_box(p, (0, 18.0, 2.8), (6.4, 4.0, 2.6), 1.8), CDF),
                 (sd_box(p, (0, 19.6, 4.4), (4.6, 0.7, 0.6), 0.3), GLASS),
                 (sd_box(p, (0, -3.0, 7.4), (2.4, 12.0, 0.6), 0.4), STEEL_T)]
        v = p[:, 1] - 0.55 * np.abs(p[:, 0])
        chev = np.maximum(sd_box(p, (0, 9.0, 7.1), (5.5, 3.0, 0.3), 0.2), np.abs(v - 7.5) - 1.0)
        items.append((chev, BAND))
        for sx in (-1, 1):
            for sy in (-1, 1):
                c = (sx * 11.8, sy * 13.0 - 1.0, 3.4)
                items += [(ring(p, c, 3.3, 2.4, 1.2), DARK), (sd_cylinder_z(p, (c[0], c[1], 3.0), 2.5, 0.2), ROTOR),
                          (sd_capsule(p, (sx * 8.0, c[1], 3.4), (sx * 9.0, c[1], 3.4), 1.0), CDF)]
            items.append((sd_sphere(p, (sx * 8.6, 3.0, 4.2), 1.6), DARK))
            items.append((sd_sphere(p, (sx * 15.0, 13.0 - 1.0, 3.6), 0.8), NAV_G if sx > 0 else NAV_R))
        items.append((sd_sphere(p, (0, -19.6, 6.8), 0.8), STROBE))
        return union(*items, k=0.3)
    return scene


def rotated(scene, deg):
    return lambda p: scene(rot_z(p, deg))


CRAFT = {
    "a": [("sedan", (12, 20), sedan_a), ("van", (14, 24), van_a), ("gunship", (32, 40), gunship_a)],
    "b": [("sedan", (12, 20), sedan_b), ("minibus", (14, 26), minibus_b), ("hauler", (30, 48), hauler_b)],
}
LIGHTS = {   # model-space (x, y, kind) of the lamps that get a small additive halo
    ("a", "sedan"): [(-2.6, 8.9, "h"), (2.6, 8.9, "h"), (-2.8, -8.9, "t"), (2.8, -8.9, "t")],
    ("a", "van"): [(-3.0, 11.0, "h"), (3.0, 11.0, "h"), (-3.6, -10.6, "t"), (3.6, -10.6, "t")],
    ("a", "gunship"): [(0, -18.6, "s"), (-15.6, 2.0, "r"), (15.6, 2.0, "g")],
    ("b", "sedan"): [(-2.0, 9.0, "h"), (2.0, 9.0, "h"), (0, -8.6, "w")],
    ("b", "minibus"): [(-2.6, 12.6, "h"), (2.6, 12.6, "h"), (-4.4, -11.4, "t"), (4.4, -11.4, "t")],
    ("b", "hauler"): [(0, -19.6, "s"), (-15.0, 12.0, "r"), (15.0, 12.0, "g")],
}
LIGHT_COL = {"h": (1.0, 0.95, 0.8), "t": (1.0, 0.18, 0.15), "s": (1.0, 1.0, 1.0), "r": (1.0, 0.2, 0.2),
             "g": (0.25, 1.0, 0.45), "w": (1.0, 0.75, 0.45)}


def mute(arr, k=0.82, sat=0.75):
    """Traffic recedes (art direction rule 3): darker and less saturated than the play plane."""
    rgb = arr[..., :3]
    lum = rgb @ np.array([0.3, 0.59, 0.11])
    arr[..., :3] = (lum[..., None] * (1 - sat) + rgb * sat) * k
    return arr


class Headings:
    """One craft's angle set (16 headings, `_0` up the screen, clockwise), rendered on first use."""

    def __init__(self, model, size, headings=16):
        self.model, self.size, self.n, self.cache = model, size, headings, {}

    def __getitem__(self, k):
        if k not in self.cache:
            w, h = self.size
            side = int(np.ceil(np.hypot(w, h)))      # the diagonal headings need a square frame
            canvas = (w, h) if k in (0, self.n // 2) else ((h, w) if k in (self.n // 4, 3 * self.n // 4) else (side, side))
            arr = mute(render(rotated(self.model(), k * 360 / self.n), tr_materials(), canvas, factor=8))
            self.cache[k] = img(arr)
        return self.cache[k]


def craft_sprites(variant):
    """{name or name-paint: Headings} plus {name: size}; the cars in every paint."""
    out, sizes = {}, {}
    for name, size, fn in CRAFT[variant]:
        sizes[name] = size
        if fn in (gunship_a, hauler_b):
            out[name] = Headings(fn, size)
        else:
            for pi in range(len(PAINTS)):
                out[f"{name}-{pi}"] = Headings(lambda pi=pi, fn=fn: fn(pi), size)
    return out, sizes


def light_halos(variant, name, deg, strength=0.55):
    """(dx, dy, glow) halos of the craft's lamps at heading deg, screen offsets from its centre."""
    out = []
    a = np.radians(deg)
    for x, y, kind in LIGHTS[(variant, name)]:
        sx = x * np.cos(a) + y * np.sin(a)
        sy = -x * np.sin(a) + y * np.cos(a)
        r = 3 if kind in "ht" else 4
        out.append((sx, -sy, r, kind, p25.halo(r, LIGHT_COL[kind], strength)))
    return out


class TrafficScene:
    AVENUES = (150, 330)
    SOUTH, NORTH = 300.0, 80.0           # screen px/s: low-air 189 + 111 own; 189 - 109 own

    def __init__(self, variant, seed=30):
        self.variant = variant
        self.city = city()
        self.sprites, self.sizes = craft_sprites(variant)
        names = [n for n, _, _ in CRAFT[variant]]
        self.car, self.big, self.heavy = names
        rng = np.random.default_rng(seed)
        self.cars = []                   # (lane x, phase px, speed, kind, paint, heading index)
        for a in self.AVENUES:
            for k in range(7):          # southbound, dense: civilians getting out
                kind = self.big if rng.random() < 0.3 else self.car
                self.cars.append((a - 6 + rng.uniform(-1, 1), k * 112 + rng.uniform(-20, 20), self.SOUTH, kind,
                                  int(rng.integers(len(PAINTS))), 8))
            for k in range(2):          # northbound, sparse
                self.cars.append((a + 7, k * 380 + rng.uniform(0, 80) + (a - 150), self.NORTH, self.car,
                                  int(rng.integers(len(PAINTS))), 0))
        self.ship = load("ship_2")
        self.flame = [load(f"engine-flame_{i}") for i in range(9)]
        self.shot = load("scatter-vulcan-shot_0")
        self.spark = [load(f"ballistic-impact_{i}") for i in range(4)]
        self.boom = [load(f"explosion-small_{i}") for i in range(12)]
        self.skitter = [load(f"skitter_{i}") for i in range(6)]
        self.ship_x = 176
        self.halos = {(n.split("-")[0], hd): light_halos(variant, n.split("-")[0], hd * 22.5)
                      for n in self.sprites for hd in (0, 8)}
        self.shots, self.kills = self.simulate()

    def craft_at(self, t, heavy=True):
        """[(x, y, sprite name, heading index)] on the screen at time t."""
        out = []
        span = 780
        for x, ph, v, kind, paint, hd in self.cars:
            y = (ph + v * t) % span - 60
            out.append((x, y, f"{kind}-{paint}", hd))
        if heavy:                         # CDF craft heading north between the avenues, a pair
            for k, (x, ph) in enumerate(((246, 40), (282, 120))):
                y = (ph + self.NORTH * t) % span - 60
                out.append((x, y, self.heavy, 0))
        return out

    def skitters_at(self, t):
        out = []
        for k in range(6):
            s = t - 0.35 * k
            if s < 0:
                continue
            y = -20 + 150 * s
            x = 200 + 38 * np.sin(2.2 * s + 0.4)
            out.append((k, x, y))
        return out

    SHOT_PERIOD = 0.08

    def simulate(self):
        shots, kills = [], {}
        for i in range(int(4.2 / self.SHOT_PERIOD)):
            t0 = i * self.SHOT_PERIOD
            for dx in (-5, 5):
                x, y0 = self.ship_x + dx, SHIP_Y - 24
                t_end, hit = t0 + (y0 + 20) / SHOT_SPEED, None
                for k in range(1, 300):
                    t = t0 + k / 240
                    y = y0 - SHOT_SPEED * (t - t0)
                    if y < -20:
                        break
                    for sk, sx, sy in self.skitters_at(t):
                        if (sk not in kills or kills[sk][0] > t) and abs(x - sx) < 10 and abs(y - sy) < 10:
                            hit = (sk, t, x, y)
                            break
                    if hit:
                        break
                if hit:
                    sk, t_end, hx, hy = hit
                    if sk not in kills:
                        kills[sk] = (t_end, hx, hy)
                shots.append((t0, x, t_end, hit is not None))
        return shots, kills

    def frame(self, t, fog=0.0, heavy=True, city_t=0.0):
        L = self.city.layers(t + city_t, fog)
        ground = L["ground"].copy()
        ground.alpha_composite(L["shadows"])
        craft = self.craft_at(t, heavy)
        shadows = Image.new("RGBA", (FIELD_W, FIELD_H), (0, 0, 0, 0))
        for x, y, name, hd in craft:
            spr = self.sprites[name][hd]
            sprite.paste_center(shadows, sprite.shadow_of(spr, opacity=0.4, blur=1.0, scale=0.85), x + 9, y + 13)
        ground.alpha_composite(shadows)
        ground.alpha_composite(L["low-air"])
        base = np.array(ground).astype(np.float64)
        for x, y, name, hd in craft:
            spr = self.sprites[name][hd]
            base = over(base, spr, x - spr.width / 2, y - spr.height / 2)
            key = name.split("-")[0]
            for dx, dy, r, kind, halo in self.halos[(key, hd)]:
                if kind == "s" and t % 1.0 > 0.15:
                    continue                                    # the strobe blinks
                add(base, halo, x + dx - r, y + dy - r)
        air = Image.new("RGBA", (FIELD_W, FIELD_H), (0, 0, 0, 0))
        for k, x, y in self.skitters_at(t):
            if k in self.kills and t >= self.kills[k][0]:
                continue
            sk = self.skitter[int(t * 12 + k) % 6]
            sprite.paste_center(air, sprite.shadow_of(sk, opacity=0.5, blur=1.0, scale=0.85), x + 21, y + 30)
        base = over(base, air, 0, 0)
        for k, x, y in self.skitters_at(t):
            if k in self.kills and t >= self.kills[k][0]:
                continue
            base = over(base, self.skitter[int(t * 12 + k) % 6], x - 12, y - 12)
        for k, (tk, hx, hy) in self.kills.items():
            if tk <= t < tk + 0.48:
                base = additive(base, self.boom[min(11, int((t - tk) * 25))], hx - 20, hy - 20)
        for t0, x, t_end, _ in self.shots:
            if t0 <= t < t_end:
                y = SHIP_Y - 24 - SHOT_SPEED * (t - t0)
                base = additive(base, self.shot, x - 4, y - 10)
        for k, (tk, hx, hy) in self.kills.items():
            if tk <= t < tk + 0.2:
                base = additive(base, self.spark[min(3, int((t - tk) / 0.05))], hx - 11, hy - 11)
        base = additive(base, self.flame[int(t * 20) % 9], self.ship_x - 6, SHIP_Y + 18)
        base = over(base, self.ship, self.ship_x - 24, SHIP_Y - 24)
        out = img(base)
        out.alpha_composite(L["high-air"])
        return out.convert("RGB")


TR_TEXT = {
    "a": {"title": "TRAFFIC - ROUND 30 VARIANT A",
          "desc": ["WEDGE CARS: A WEDGE SEDAN (12X20) AND A BOX VAN (14X24) ON FOUR CORNER LIFT DUCTS, IN DUSTY SLATE, BEIGE, MAROON, GREY AND",
                   "GREEN-GREY; HEADLIGHTS AND RED TAIL LIGHTS ONLY. CDF GUNSHIP (32X40): TWIN DUCTED FANS ON STUB WINGS, GLAZED NOSE, CHIN GUN,",
                   "V TAIL, MUTED OLIVE-GREY WITH A PALE BAND, RED/GREEN NAV LIGHTS AND A WHITE STROBE. SCENERY: MUTED, NO RIM, NO GLINT, NO HIT SPARK."]},
    "b": {"title": "TRAFFIC - ROUND 30 VARIANT B",
          "desc": ["PODS: A TEARDROP SEDAN (12X20) WITH A BUBBLE CANOPY AND ONE REAR THRUSTER, AN EVACUEE MINIBUS (14X26) WITH A LIT WINDOW",
                   "BAND (PEOPLE INSIDE). CDF ARMOURED HAULER (30X48): A LONG ARMOURED BOX ON FOUR TILT DUCTS, A SLIT CAB, DOOR-GUN BLISTERS, A",
                   "CARGO SPINE AND A PALE CHEVRON, MUTED OLIVE-GREY, NAV LIGHTS AND A STROBE. SCENERY: MUTED, NO RIM, NO GLINT, NO HIT SPARK."]},
}


def traffic_sheet(variant):
    sc = TrafficScene(variant)
    s = sc.sprites
    names = [n for n, _, _ in CRAFT[variant]]
    keys = [f"{names[0]}-0", f"{names[0]}-1", f"{names[0]}-2", f"{names[1]}-3", f"{names[1]}-4", names[2]]
    small = recolour_plate(plate_strip([s[k][0] for k in keys] + [s[k][8] for k in keys], 1), NIGHT_PLATE)
    idx = (0, 2, 4, 6, 8)
    big_frames = [s[k][i] for k in (f"{names[0]}-0", f"{names[1]}-1", names[2]) for i in idx]
    big = recolour_plate(plate_strip(big_frames, 3), NIGHT_PLATE)
    times = (1.1, 2.3, 3.2)
    fogs = (0.0, 0.45, 0.45)
    shots = [sc.frame(t, fog=fg, city_t=6.0 * k) for k, (t, fg) in enumerate(zip(times, fogs))]
    t_crop = 1.62
    crop_src = sc.frame(t_crop, fog=0.0, city_t=0.0)
    crop = crop_src.crop((90, 220, 210, 490)).resize((240, 540), Image.NEAREST)
    width = 16 + 3 * (FIELD_W + 12) + crop.width + 16
    height = 74 + small.height + 24 + big.height + 34 + 34 + FIELD_H + 60
    out = raster.sheet(width, height, TR_TEXT[variant]["title"],
                       "LEVEL 08 LOW-AIR TRAFFIC CONCEPT - SCENERY (D2 = A): NO COLLISION, SHOTS PASS THROUGH")
    for k, line in enumerate(TR_TEXT[variant]["desc"]):
        raster.draw_text(out, 16, 34 + 12 * k, line, raster.LABEL if k == 0 else raster.LABEL_DIM)
    y = 74
    raster.draw_text(out, 16, y, "1X", raster.LABEL_DIM)
    out.alpha_composite(small, (40, y))
    sz = sc.sizes
    raster.draw_text(out, 40 + small.width + 12, y + 8,
                     f"HEADING UP (_0) THEN DOWN (_8): {names[0].upper()} {sz[names[0]][0]}X{sz[names[0]][1]} IN THREE PAINTS, "
                     f"{names[1].upper()} {sz[names[1]][0]}X{sz[names[1]][1]} IN TWO, CDF {names[2].upper()} {sz[names[2]][0]}X{sz[names[2]][1]}",
                     raster.LABEL_DIM)
    y += small.height + 10
    raster.draw_text(out, 16, y, "3X: HEADINGS 0, 2, 4, 6, 8 OF 16 (CLOCKWISE FROM UP; RENDERED FROM THE MODEL, THE KEY LIGHT FIXED TOP-LEFT)",
                     raster.LABEL_DIM)
    out.alpha_composite(big, (16, y + 12))
    y += 12 + big.height + 12
    notes = ("PROPOSED DATA: LOW-AIR PIECES WITH HEADINGS: 16 AND A PATH; STREAMS AS REPEAT (A CAR ABOUT EVERY 0.37 S PER SOUTHBOUND LANE). OWN SPEED "
             "111 PX/S SOUTH (300 PX/S ON SCREEN WITH THE LAYER'S 189) AND 109 NORTH (80 ON SCREEN), BOTH INSIDE THE 120 PX/S MOTION BUDGET.")
    raster.draw_text(out, 16, y, notes[:150], raster.LABEL_DIM)
    raster.draw_text(out, 16, y + 12, notes[150:], raster.LABEL_DIM)
    y += 34
    raster.draw_text(out, 16, y, "PLAY FIELD 1X OVER THE CHOSEN MEGACITY (PARALLAX-R03-B): CLEAR, LIGHT, LIGHT. THE VULCAN STREAM PASSES OVER THE CARS "
                     "AND KILLS SKITTERS; THE LOW-AIR SHADOW (9,13); 2X: SHOTS OVER A STREAM", raster.LABEL_DIM)
    for k, im in enumerate(shots):
        out.paste(im, (16 + k * (FIELD_W + 12), y + 12))
    out.paste(crop, (16 + 3 * (FIELD_W + 12), y + 12))
    path = OUT / f"traffic-r30-{variant}.png"
    out.convert("RGB").save(path, optimize=True)
    print(path.relative_to(ROOT))
    gif_frames = [sc.frame(float(t), fog=0.25).crop((60, 0, 420, FIELD_H)) for t in np.arange(0.0, 4.0, 0.08)]
    gif = OUT / f"traffic-r30-{variant}.gif"
    write_gif(gif_frames, gif, fps=12.5)
    print(gif.relative_to(ROOT), f"{gif.stat().st_size / 1e6:.1f} MB")


def main(args):
    OUT.mkdir(parents=True, exist_ok=True)
    parts = [a for a in args if a in ("billboard", "traffic")] or ["billboard", "traffic"]
    variants = [a for a in args if a in ("a", "b")] or ["a", "b"]
    for part in parts:
        for v in variants:
            (billboard_sheet if part == "billboard" else traffic_sheet)(v)


if __name__ == "__main__":
    main(sys.argv[1:])
