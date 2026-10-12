#!/usr/bin/env python3
"""Production art: Level 12's props and weather looks (design/campaign/act-2-homefront/
level-12-storm-front, "Weather", "Objective", "Secrets and pickups"; M5 part F batch, concept round
34, straight to production, user decision F8 = a; no concept: designed to the README and to the
chosen storm scene's light, scene-storm-r10-a).

Sprites the game draws in Level 12 (assets/sprites/). The weather looks share the root ``weather``
(pipeline SpriteUse: claimed by a level with a ``weather`` block), the canister is the drifting
trigger's ``sprite`` (claimed through the level's ground targets, as Level 11's pod):
  weather-sonde_0..3          32x32 a CDF weather sonde (F7 = b: on the play plane, 24 px body): a pale
                              instrument sphere with a slate band, four stabiliser vanes and a mast
                              strobe, a 1 px light outline (the pickups' rule 6); a blink loop: 0 the
                              strobe on with its halo, 1 fading, 2-3 off (suggested 6 fps, 0.67 s)
  weather-sonde-collect_0..5  48x48 additive: the collect pop, a ring opening from the sonde with data
                              sparks flying out (pale cyan-white, 6 frames at 20 fps)
  supply-canister_0..2        48x48 the drifting CDF supply canister (F2 = a): Level 11's supply pod
                              (l11_props.pod_shape, olive drum, amber hazard ends, beacon dome) under a
                              drogue chute trailing up-screen on its lines; 0 intact, 1 hit (scraped, a
                              chute gore torn), 2 spent (the drum split open, the chute collapsed);
                              rule 7's light rim; the game's glint, hit flash and crate pickup as for
                              every trigger
  supply-canister-glow_0..7   48x48 additive: its amber beacon (GroundGlow: 0-3 until the first hit, a
                              slow blink; 4-7 after it, a hurried flicker), the "faint glint" the game
                              draws over the bank that hides the canister (F1 = a)
  weather-rain_0..3           8x32 additive: faint motion-blurred rain streaks slanting 1:4 to the
                              right (the storm's wind from the left), lengths 14/18/24/30 px, the head
                              brighter (presentation only; the game draws them on high-air, density
                              from the data's rain keys)
  weather-gust-arrow_0..5     64x40 additive: the gust telegraph at the upwind edge (pointing right: a
                              gust from the left; mirrored for one from the right): three streamlines
                              with a bright pulse running along them into a chevron; pale blue-white,
                              unlike the amber edge warning
  weather-gust-streak_0..1    160x6 additive: thin wind streaks the game sends across the screen with
                              the telegraph (the sea's streaks "brightening")
  weather-bolt_0..2           96x320 additive: forked lightning bolts (white core, blue glow) for the
                              sky flash, shown at most 0.1 s, never more than 3 flashes a second
  weather-flash               160x128 additive: a cloud lit from inside, drawn where a bolt strikes
                              over the banks for the 0.5 s reveal (the game fades it out)
  weather-glow_0..3           32x32 additive: the faint glow of a low-air body hidden under a bank
                              (F1 = a), a slow 4-frame pulse
Review: design/campaign/act-2-homefront/level-12-storm-front/concept/l12-props-final-r34-a.png/.gif
(the GIF over the Level 12 backdrop's tiles when tools/art/backdrop_l12.py has written them).

Run: python3 tools/art/l12_props.py [--review]   (~30 s)
"""
import sys
from concurrent.futures import ProcessPoolExecutor

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

import artkit
from artkit import DESIGN, ROOT, TAU, sprite

import ground_targets as gt  # noqa: E402  (concept script, imported unchanged: the light rim of rule 7)
import l11_props  # noqa: E402  (tools/art: the CDF supply pod's model and materials)
from render import raster  # noqa: E402
from render.sdf import Material, rotate_x, rotate_z, sd_box, sd_capsule, sd_ellipsoid, sd_sphere, union  # noqa: E402

SCRIPT = "l12_props.py"
BATCH = "M5 part F batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
REVIEW_ROUND = "r34"
CONCEPT = DESIGN / "campaign" / "act-2-homefront" / "level-12-storm-front" / "concept"
COLOURS = 32

SONDE = (32, 32)
COLLECT = (48, 48)
CANISTER = (48, 48)
RAIN = (8, 32)
ARROW = (64, 40)
STREAK = (160, 6)
BOLT = (96, 320)
FLASH = (160, 128)
GLOW = (32, 32)

RAIN_RGB = np.array([170, 186, 200], float)      # scenes_r10's rain (150, 166, 178), a step lighter for 40 %
WIND_RGB = np.array([196, 222, 240], float)
BOLT_CORE = np.array([255, 255, 255], float)
BOLT_GLOW = np.array([120, 150, 255], float)
CLOUD_LIT = np.array([176, 194, 232], float)
DATA_RGB = np.array([190, 236, 255], float)
BEACON_RGB = np.array([255, 176, 60], float)


def premultiplied(rgb, a):
    """An additive frame as the game blends it: rgb x alpha on black, alpha 255 wherever it adds."""
    out = np.clip(rgb * a[..., None], 0, 255)
    alpha = np.where(out.max(axis=-1) >= 1, 255, 0)
    return Image.fromarray(np.dstack([np.round(out), alpha]).astype(np.uint8), "RGBA")


def steps(v, n):
    return np.floor(np.clip(v, 0, 1) * n + 0.5) / n


# =========================================================================== the weather sonde

BODY, BAND, RIM, MAST, STROBE = range(5)


def sonde_mats(strobe):
    return [Material((0.95, 0.96, 0.97), metal=0.0, shininess=30, spec=0.4),
            Material((0.30, 0.44, 0.52), metal=0.3, shininess=40, spec=0.45),
            Material((0.40, 0.44, 0.50), metal=0.6, shininess=50, spec=0.6),
            Material((0.55, 0.57, 0.6), metal=0.7, shininess=60, spec=0.7),
            Material((0.3, 0.35, 0.4), emission=tuple(np.array((1.7, 2.0, 2.3)) * strobe)) if strobe
            else Material((0.32, 0.38, 0.44), metal=0.2, shininess=90, spec=0.9)]


def sonde_scene(p):
    """A CDF weather sonde seen from above, a round instrument buoy (24 px across): a pale float
    disc with a raised slate collar, a dark ring of sensor ports, the strobe dome in its centre and
    two short antenna whiskers; nothing spiky, so it never reads as a mine."""
    q = rotate_z(p, np.radians(20))
    r = np.hypot(q[:, 0], q[:, 1])
    disc = sd_ellipsoid(q, (0, 0, -3.0), (11.5, 11.5, 6.5))
    collar = np.maximum(np.abs(r - 6.4) - 0.8, np.abs(q[:, 2] - 3.0) - 1.0) - 0.3
    ports = np.where((np.cos(np.arctan2(q[:, 1], q[:, 0]) * 8) > 0.55) & (np.abs(r - 9.6) < 0.8), -1.0, 1.0)
    whisk = np.minimum(sd_capsule(q, (3.0, 3.0, 3.0), (8.5, 8.0, 5.5), 0.45),
                       sd_capsule(q, (-3.0, -3.0, 3.0), (-8.5, -8.0, 5.5), 0.45))
    strobe = sd_sphere(q, (0, 0, 3.4), 2.6)
    d, m = union((disc, BODY), (collar, BAND), (whisk, MAST), (strobe, STROBE))
    m = np.where((ports < 0) & (m == BODY), RIM, m)
    return d, m


def outline(img, colour=(206, 226, 240), alpha=150):
    """Rule 6's light outline: a 1 px translucent rim of pale light outside the silhouette."""
    a = np.array(img).astype(np.float64)
    solid = a[..., 3] > 0
    grown = solid.copy()
    for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        grown |= np.roll(np.roll(solid, dy, 0), dx, 1)
    ring = grown & ~solid
    a[ring, :3] = colour
    a[ring, 3] = alpha
    return Image.fromarray(a.astype(np.uint8), "RGBA")


def halo(img, centre, radius, strength, colour=(200, 226, 255)):
    """A strobe's halo baked round the solid body with stepped alpha (artkit.stepped_alpha's rule:
    the body stays 1-bit, the halo outside it gets a few translucency levels)."""
    w, h = img.size
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64)
    d = np.hypot(xx + 0.5 - centre[0], yy + 0.5 - centre[1]) / radius
    g = np.clip(1 - d, 0, 1) ** 1.6 * strength
    g = steps(g, 3)
    a = np.array(img).astype(np.float64)
    out = a.copy()
    outside = a[..., 3] == 0
    out[..., :3] = np.where(outside[..., None], np.array(colour, float), a[..., :3])
    out[..., 3] = np.where(outside, g * 255, a[..., 3])
    # on the body the strobe brightens it a little
    inside = ~outside
    out[inside, :3] = np.minimum(255, a[inside, :3] + np.array(colour) * (g[inside, None] * 0.35))
    return Image.fromarray(out.astype(np.uint8), "RGBA")


def sonde_frame(k):
    strobe = (1.0, 0.45, 0.0, 0.0)[k]
    hi, factor = artkit.render_hi(sonde_scene, sonde_mats(strobe), SONDE, float(SONDE[0]), z_top=16.0, steps=150)
    img = outline(artkit.native(hi, factor, crisp=60))
    if strobe:
        img = halo(img, strobe_px(), 9.0 if k == 0 else 6.0, 0.9 if k == 0 else 0.5)
    return img


def strobe_px():
    """The strobe's pixel centre (the mast is straight up, so the strobe sits over the body's centre)."""
    return SONDE[0] / 2, SONDE[1] / 2


def collect_frame(k, n=6):
    """The collect pop: a thin ring opening from 6 to 22 px, eight data sparks flying out and
    fading, a soft flash in the first two frames."""
    w, h = COLLECT
    u = k / n
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64) + 0.5
    d = np.hypot(xx - w / 2, yy - h / 2)
    r = 6 + 16 * (1 - (1 - u) ** 2)
    ring = np.clip(1 - np.abs(d - r) / 1.3, 0, 1) * (1 - u) ** 0.8
    flash = np.clip(1 - d / (10 - 4 * u), 0, 1) * max(0.0, 1 - 2.2 * u)
    a = np.maximum(ring, flash * 0.9)
    rng = np.random.default_rng(3401)
    for j in range(8):
        ang = TAU * j / 8 + rng.uniform(-0.2, 0.2)
        rr = 5 + 19 * u ** 0.8 * rng.uniform(0.8, 1.0)
        sx, sy = w / 2 + np.cos(ang) * rr, h / 2 + np.sin(ang) * rr
        a = np.maximum(a, np.clip(1 - np.hypot(xx - sx, yy - sy) / 1.4, 0, 1) * (1 - u) ** 0.6)
    return premultiplied(DATA_RGB, steps(a, 6))


# =========================================================================== the supply canister

POD_BODY, POD_HAZARD, POD_STEEL, POD_BEACON, CHITIN, VEIN, SCRAPE = range(7)
CHUTE_A, CHUTE_B, LINE, HOLE = 7, 8, 9, 10
POD_AT = (-5.0, -7.0)          # the drum's centre (model units = px, y up)
POD_TURN = np.radians(-22)
CHUTE_AT = (8.0, 11.0)         # the canopy's centre: up the screen and a little right of the drum


def canister_mats():
    mats = l11_props.pod_mats(beacon=False)
    return mats + [Material((0.26, 0.30, 0.19), metal=0.0, shininess=10, spec=0.1),
                   Material((0.86, 0.86, 0.80), metal=0.0, shininess=10, spec=0.1),
                   Material((0.62, 0.62, 0.58), metal=0.0, shininess=10, spec=0.1),
                   Material((0.10, 0.10, 0.10), metal=0.0, shininess=4, spec=0.0)]


def chute(p, state):
    """The drogue canopy (a shallow dome, olive and white gores) and its four lines to the drum's
    ends; hit, one gore torn open; spent, collapsed to a crumpled lump beside the drum."""
    c = np.array([CHUTE_AT[0], CHUTE_AT[1], 6.0])
    q = p - c
    if state == 2:
        lp = rotate_z(p - np.array([8.0, 9.0, 1.0]), 0.7)
        lump = sd_ellipsoid(lp, (0, 0, 0), (9.0, 4.6, 2.2))
        folds = np.where(np.sin(lp[:, 0] * 1.3) > 0.2, CHUTE_A, CHUTE_B)
        line = sd_capsule(p, (POD_AT[0] + 6, POD_AT[1] + 3, 0.5), (3.0, 6.0, 1.0), 0.45)
        return union((lump, folds), (line, LINE))
    dome = np.maximum(sd_ellipsoid(q, (0, 0, -1.0), (11.0, 11.0, 3.6)), -(q[:, 2] + 0.5))
    ang = np.arctan2(q[:, 1], q[:, 0])
    gore = np.where((np.floor((ang + np.pi) / (TAU / 8)) % 2) == 0, CHUTE_A, CHUTE_B)
    gore = np.where(np.hypot(q[:, 0], q[:, 1]) < 1.6, HOLE, gore)            # the apex vent
    ends = [np.array([POD_AT[0], POD_AT[1], 0.0]) + np.array([np.cos(POD_TURN), np.sin(POD_TURN), 0]) * s * 9.5
            for s in (-1, 1)]
    rim = [c + np.array([np.cos(a), np.sin(a), -1.0]) * np.array([10.0, 10.0, 1.0]) for a in np.radians([200, 250, 290, 340])]
    lines = np.full(len(p), 1e3)
    for j, r in enumerate(rim):
        lines = np.minimum(lines, sd_capsule(p, tuple(ends[0 if j < 2 else 1] + np.array([0, 0, 2.0])), tuple(r), 0.4))
    d, m = dome, gore
    if state == 1:     # a torn gore: a dark hole and a ragged flap
        hole = sd_ellipsoid(q, (4.0, 3.5, 2.5), (3.2, 2.2, 6.0))
        m = np.where((hole < 0) & (m >= 0), HOLE, m)
    return union((d, m), (lines, LINE))


def canister_scene(state):
    def scene(p):
        q = rotate_z(p - np.array([POD_AT[0], POD_AT[1], 0.0]), -POD_TURN)
        if state < 2:
            pod = l11_props.pod_shape(q, scraped=state == 1)
        else:  # split open: the two halves apart, the inside dark
            qa = rotate_z(q - np.array([-3.5, -2.0, 0.0]), 0.35)
            qb = rotate_z(q - np.array([4.0, 2.5, 0.0]), -0.3)
            a = l11_props.pod_shape(qa, scraped=True)
            b = l11_props.pod_shape(qb, scraped=True)
            cut_a = np.maximum(a[0], qa[:, 0] - 0.5)            # the left half, open to the right
            cut_b = np.maximum(b[0], -(qb[:, 0] - 0.5))         # the right half, open to the left
            open_a = sd_ellipsoid(qa, (0.6, 0, 0.4), (0.9, 4.4, 4.4))
            open_b = sd_ellipsoid(qb, (-0.6, 0, 0.4), (0.9, 4.4, 4.4))
            pod = union((cut_a, a[1]), (cut_b, b[1]), (np.minimum(open_a, open_b), HOLE))
        return union(pod, chute(p, state))
    return scene


def canister_frame(state):
    hi, factor = artkit.render_hi(canister_scene(state), canister_mats(), CANISTER, float(CANISTER[0]),
                                  z_top=24.0, steps=170)
    img = artkit.native(hi, factor, crisp=60)
    return Image.fromarray(gt.light_rim(np.array(img).astype(np.float64)).astype(np.uint8), "RGBA")


def beacon_px():
    """The beacon dome (pod_shape's (0, 0, 5)) on the frame: the drum's centre."""
    return CANISTER[0] / 2 + POD_AT[0], CANISTER[1] / 2 - POD_AT[1]


def canister_glow(k):
    """GroundGlow's halves at 8 fps: 0-3 until the first hit (on, half, off, off: a 0.5 s blink),
    4-7 after it (a hurried flicker: on, off, half, off)."""
    level = (1.0, 0.5, 0.0, 0.0, 1.0, 0.0, 0.6, 0.0)[k]
    w, h = CANISTER
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64) + 0.5
    bx, by = beacon_px()
    d = np.hypot(xx - bx, yy - by)
    a = (np.clip(1 - d / 7.0, 0, 1) ** 1.5 * 0.85 + np.clip(1 - d / 2.0, 0, 1) * 0.6) * level
    return premultiplied(BEACON_RGB, steps(a, 5))


# =========================================================================== rain, wind, lightning

def rain_frame(k):
    """A streak of length L slanting 1:4 (down and to the right), the head (bottom) brightest, the
    tail fading; anti-aliased, its light stepped to 5 levels."""
    w, h = RAIN
    length = (14, 18, 24, 30)[k]
    y1 = h / 2 + length / 2
    y0 = y1 - length
    x1 = w / 2 + length / 8
    x0 = x1 - length / 4
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64) + 0.5
    ax, ay = x1 - x0, y1 - y0
    t = np.clip(((xx - x0) * ax + (yy - y0) * ay) / (ax * ax + ay * ay), 0, 1)
    d = np.hypot(xx - (x0 + ax * t), yy - (y0 + ay * t))
    a = np.clip(1 - d / 0.9, 0, 1) * (0.25 + 0.75 * t ** 1.5)
    return premultiplied(RAIN_RGB, steps(a * 0.9, 5))


def arrow_frame(k, n=6):
    """Three streamlines (slightly bowed, as the gust's push) into a chevron at the right; a bright
    pulse runs along each line left to right over the loop, the chevron breathing with it."""
    w, h = ARROW
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64) + 0.5
    a = np.zeros((h, w))
    for j, (y, x0, x1) in enumerate(((11.0, 6, 44), (20.0, 2, 50), (29.0, 6, 44))):
        yl = y + 1.4 * np.sin((xx - x0) / (x1 - x0) * np.pi) * (1 if j == 0 else -1 if j == 2 else 0)
        on = (xx >= x0) & (xx <= x1)
        line = np.clip(1 - np.abs(yy - yl) / 0.8, 0, 1) * on
        u = (xx - x0) / (x1 - x0)
        pulse_at = ((k / n) + j * 0.17) % 1.0
        dist = np.minimum(np.abs(u - pulse_at), 1 - np.abs(u - pulse_at))
        pulse = np.exp(-(dist / 0.12) ** 2)
        a = np.maximum(a, line * (0.28 + 0.72 * pulse) * np.clip(u * 3, 0.2, 1))
    breathe = 0.75 + 0.25 * np.cos(TAU * k / n)
    tip, back = 60.0, 50.0
    for off in (0.0, -6.0):
        for sgn in (1, -1):
            x0_, y0_, x1_, y1_ = back + off, 20 + sgn * 11, tip + off, 20
            ax, ay = x1_ - x0_, y1_ - y0_
            t = np.clip(((xx - x0_) * ax + (yy - y0_) * ay) / (ax * ax + ay * ay), 0, 1)
            d = np.hypot(xx - (x0_ + ax * t), yy - (y0_ + ay * t))
            a = np.maximum(a, np.clip(1 - d / 1.2, 0, 1) * breathe * (1.0 if off == 0 else 0.55))
    return premultiplied(WIND_RGB, steps(a * 0.9, 6))


def streak_frame(k):
    w, h = STREAK
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64) + 0.5
    u = xx / w
    yl = h / 2 + (0.8 if k else -0.6) * np.sin(u * np.pi * (2 if k else 1))
    body = np.clip(1 - np.abs(yy - yl) / (0.7 + 0.5 * np.sin(u * np.pi)), 0, 1)
    a = body * np.sin(u * np.pi) ** 1.5 * (0.55 if k == 0 else 0.45)
    return premultiplied(WIND_RGB, steps(a, 5))


def bolt_path(rng, x0, y0, length, segs, wander):
    pts = [(x0, y0)]
    x, y = x0, y0
    for _ in range(segs):
        x, y = x + rng.normal(0, wander), y + length / segs
        pts.append((x, y))
    return pts


def bolt_frame(k):
    """A forked bolt from the top edge: the main channel jagged, two or three forks; a white 1-2 px
    core, a blue glow (two blurs), the light stepped to 6 levels (additive)."""
    w, h = BOLT
    rng = np.random.default_rng(3450 + k)
    core = Image.new("L", (w * 2, h * 2), 0)
    d = ImageDraw.Draw(core)
    main = bolt_path(rng, w + rng.uniform(-20, 20), 0, h * 2 * rng.uniform(0.86, 0.95), 18, 13)
    d.line([(x, y) for x, y in main], fill=255, width=3)
    for f in range(2 + k % 2):
        at = main[int(rng.integers(4, 13))]
        fork = bolt_path(rng, at[0], at[1], rng.uniform(90, 200), 8, 10)
        fork = [(x + (i * rng.uniform(1.5, 4.5) * (1 if f % 2 else -1)), y) for i, (x, y) in enumerate(fork)]
        d.line(fork, fill=170, width=2)
    core = core.resize((w, h), Image.BOX)
    c = np.array(core).astype(np.float64) / 255
    g1 = np.array(core.filter(ImageFilter.GaussianBlur(3))).astype(np.float64) / 255
    g2 = np.array(core.filter(ImageFilter.GaussianBlur(9))).astype(np.float64) / 255
    glow = np.clip(g1 * 1.6 + g2 * 2.4, 0, 1)
    cc = np.clip(c * 1.6, 0, 1)[..., None]
    rgb = BOLT_GLOW * (1 - cc) + BOLT_CORE * cc
    a = steps(np.clip(np.maximum(glow * 0.75, cc[..., 0]), 0, 1), 6)
    return premultiplied(rgb, a)


def flash_frame():
    """A cloud lit from inside: lumpy noise under a soft falloff, brightest a little above centre."""
    w, h = FLASH
    n = raster.fbm(w, h, 32, 3460, octaves=4, period=False)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64) + 0.5
    d = np.hypot((xx - w / 2) / (w / 2), (yy - h * 0.45) / (h / 2))
    body = np.clip((n * 1.1 + (1 - d) * 0.9 - 0.75) / 0.35, 0, 1)            # a lumpy cloud, not a disc
    light = np.clip(1 - d * 1.2, 0, 1) ** 0.8 * (0.5 + 0.5 * n)
    a = body * (0.25 + 0.75 * light)
    return premultiplied(CLOUD_LIT, steps(np.clip(a, 0, 1) * 0.85, 6))


def glow_frame(k):
    w, h = GLOW
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64) + 0.5
    d = np.hypot(xx - w / 2, yy - h / 2) / (w / 2)
    level = (0.42, 0.52, 0.42, 0.32)[k]
    return premultiplied(np.array([186, 196, 228], float), steps(np.clip(1 - d, 0, 1) ** 1.8 * level, 4))


# =========================================================================== build

def _job(job):
    kind, k = job
    return {"sonde": sonde_frame, "canister": canister_frame}[kind](k)


def sprite_sets():
    jobs = [("sonde", k) for k in range(4)] + [("canister", s) for s in range(3)]
    with ProcessPoolExecutor() as pool:
        res = list(pool.map(_job, jobs))
    sonde = artkit.quantize_set(res[:4], COLOURS)
    canister = artkit.quantize_set(res[4:], 40)
    return {
        "weather-sonde": sonde,
        "weather-sonde-collect": [collect_frame(k) for k in range(6)],
        "supply-canister": canister,
        "supply-canister-glow": [canister_glow(k) for k in range(8)],
        "weather-rain": [rain_frame(k) for k in range(4)],
        "weather-gust-arrow": [arrow_frame(k) for k in range(6)],
        "weather-gust-streak": [streak_frame(k) for k in range(2)],
        "weather-bolt": [bolt_frame(k) for k in range(3)],
        "weather-flash": artkit.quantize_set([flash_frame()], 16),
        "weather-glow": [glow_frame(k) for k in range(4)],
    }


SINGLE = {"weather-flash"}
NAMES = ["weather-sonde", "weather-sonde-collect", "supply-canister", "supply-canister-glow", "weather-rain",
         "weather-gust-arrow", "weather-gust-streak", "weather-bolt", "weather-flash", "weather-glow"]


def build():
    sets = sprite_sets()
    for name, frames in sets.items():
        artkit.write_frames(name, frames, SOURCE, single=name in SINGLE)
        print(f"{name}: {len(frames)} frame(s), {frames[0].width}x{frames[0].height}, {artkit.colour_count(frames)} colours")
    return sets


# =========================================================================== review

def storm_sea(w, h):
    """The Level 12 sea as the backdrop draws it (swell, chop, a medium bank tile) when
    backdrop_l12.py has written it, otherwise the storm's darkest tone."""
    folder = ROOT / "assets" / "backdrop" / "level-12"
    if not (folder / "swell.png").exists():
        return None
    swell = np.array(Image.open(folder / "swell.png").convert("RGBA")).astype(np.float64)
    chop = np.array(Image.open(folder / "chop.png").convert("RGBA")).astype(np.float64)
    banks = np.array(Image.open(folder / "scud-banks.png").convert("RGBA")).astype(np.float64)
    return swell, chop, banks


def sea_at(tiles, off, w, h):
    swell, chop, banks = tiles
    th = swell.shape[0]

    def rows(img, o):
        r = (np.arange(h) * 2 // 2 - int(o)) % th
        return img[r, :w]
    base = rows(swell, off * 0.85)[..., :3].copy()
    base = base * 0.9 + np.array([92, 104, 114]) * 0.1
    c = rows(chop, off)
    base = base * (1 - c[..., 3:4] / 255) + c[..., :3] * (c[..., 3:4] / 255)
    return base, rows(banks, off * 1.5)


def over(base, img, x, y):
    a = np.array(img).astype(np.float64)
    h, w = a.shape[:2]
    left, top = int(round(x - w / 2)), int(round(y - h / 2))
    H, W = base.shape[:2]
    y0, y1, x0, x1 = max(0, top), min(H, top + h), max(0, left), min(W, left + w)
    if y0 >= y1 or x0 >= x1:
        return
    src = a[y0 - top:y1 - top, x0 - left:x1 - left]
    k = src[..., 3:4] / 255
    base[y0:y1, x0:x1] = base[y0:y1, x0:x1] * (1 - k) + src[..., :3] * k


def add(base, img, x, y, strength=1.0):
    a = np.array(img).astype(np.float64)
    h, w = a.shape[:2]
    left, top = int(round(x - w / 2)), int(round(y - h / 2))
    H, W = base.shape[:2]
    y0, y1, x0, x1 = max(0, top), min(H, top + h), max(0, left), min(W, left + w)
    if y0 >= y1 or x0 >= x1:
        return
    src = a[y0 - top:y1 - top, x0 - left:x1 - left]
    base[y0:y1, x0:x1] = np.minimum(255, base[y0:y1, x0:x1] + src[..., :3] * src[..., 3:4] / 255 * strength)


def review_loop(sets):
    """240x300 of the storm at 1x scrolling at 140 px/s (shown 2x), 4 s at 20 fps: the canister
    drifting under a bank with its beacon glow on top, a lightning flash (bolt, the bank lit and
    seen through for 0.5 s), rain on high-air at 40 %, a gust telegraph at the left edge, a sonde
    drifting down at 60 px/s and collected by the ship."""
    w, h, n = 240, 300, 80
    tiles = storm_sea(w, h)
    ship = artkit.load_frames("ship")
    rng = np.random.default_rng(3490)
    drops = [(rng.uniform(0, w), rng.uniform(0, h), int(rng.integers(0, 4)), rng.uniform(0.8, 1.2)) for _ in range(70)]
    out = []
    flash_at = 30
    for i in range(n):
        t = i / 20
        off = t * 140
        if tiles is not None:
            base, bank = sea_at(tiles, off, w, h)
        else:
            base, bank = np.full((h, w, 3), (12, 22, 30), float), None
        # the canister under the bank (drawn first), its glow on top of the bank
        cx, cy = 168 - 14 * t, 40 + 64 * t
        state = 0 if i < 50 else 1
        over(base, sets["supply-canister"][state], cx, cy)
        a = (i - flash_at) / 10                            # the 0.5 s reveal after a flash
        reveal = 1 - a if 0 <= a < 1 else 0.0
        if bank is not None:
            k = bank[..., 3:4] / 255 * (1 - 0.6 * reveal)
            lit = bank[..., :3] + (np.array([190, 205, 235]) - bank[..., :3]) * (0.45 * reveal)
            base = base * (1 - k) + lit * k
        gl = sets["supply-canister-glow"][(i // 2.5).__int__() % 4 + (4 if state else 0)]
        add(base, gl, cx, cy)
        if 0 <= i - flash_at < 2:                          # the sky flash, <= 0.1 s
            add(base, sets["weather-bolt"][1], 70, 150, 1.0)
            base = np.minimum(255, base + np.array([150, 165, 190]) * 0.22)
        if 0 <= i - flash_at < 10:
            add(base, sets["weather-flash"][0], 70, 120, reveal)
        # a sonde drifting down, collected at i = 64
        sx, sy = 70, -16 + 60 * t
        if i < 64:
            over(base, sets["weather-sonde"][(i // 3) % 4], sx, sy)
        elif i < 70:
            add(base, sets["weather-sonde-collect"][i - 64], sx, sy)
        px = 120 - 50 * min(1.0, max(0.0, (i - 34) / 26))
        over(base, ship[0], px, 250 - 70 * min(1.0, max(0.0, (i - 40) / 24)))
        # the gust telegraph at the left edge for the last 1.5 s
        if i >= 50:
            add(base, sets["weather-gust-arrow"][(i // 2) % 6], 34, 150)
            for j, y in enumerate((60, 118, 196, 252)):
                add(base, sets["weather-gust-streak"][j % 2], -60 + ((i - 50) * 16 + j * 50) % 360, y, 0.6)
        # rain on high-air, additive at 40 %
        for x0, y0, v, sp in drops:
            y = (y0 + 480 * sp * t) % (h + 40) - 20
            x = (x0 + 120 * sp * t) % w
            add(base, sets["weather-rain"][v], x, y, 0.4)
        out.append(sprite.enlarge(Image.fromarray(np.clip(base, 0, 255).astype(np.uint8), "RGB").convert("RGBA"), 2))
    return out


def review(sets):
    rows = [
        ("WEATHER SONDE 32X32 (24 PX BODY): BLINK LOOP, STROBE ON / FADING / OFF / OFF", sets["weather-sonde"], 4, False),
        ("SONDE COLLECTED: RING AND DATA SPARKS (ADDITIVE)", sets["weather-sonde-collect"], 3, True),
        ("SUPPLY CANISTER UNDER ITS DROGUE: INTACT, HIT (SCRAPED, GORE TORN), SPENT (SPLIT OPEN)",
         sets["supply-canister"], 4, False),
        ("ITS BEACON GLOW (ADDITIVE, OVER THE BANK): 0-3 UNTIL HIT, 4-7 AFTER", sets["supply-canister-glow"], 2, True),
        ("RAIN STREAKS (ADDITIVE, DRAWN AT 40 %)", sets["weather-rain"], 5, True),
        ("GUST TELEGRAPH ARROW (ADDITIVE, A GUST FROM THE LEFT)", sets["weather-gust-arrow"], 3, True),
        ("WIND STREAKS (ADDITIVE)", sets["weather-gust-streak"], 2, True),
        ("LIGHTNING BOLTS (ADDITIVE, <= 0.1 S)", sets["weather-bolt"], 1, True),
        ("CLOUD LIT FROM INSIDE (ADDITIVE, THE 0.5 S REVEAL) AND A HIDDEN BODY'S GLOW",
         sets["weather-flash"] + [sprite.enlarge(f, 2) for f in sets["weather-glow"]], 1, True),
    ]
    sheet = artkit.review_sheet("LEVEL 12 PROPS AND WEATHER LOOKS - PRODUCTION SPRITES (ROUND 34)", rows, width=1500,
                                batch=BATCH)
    artkit.save_review(sheet, review_loop(sets), CONCEPT, "l12-props", fps=20)


if __name__ == "__main__":
    artkit.REVIEW_ROUND = REVIEW_ROUND
    sets = build() if "--review" not in sys.argv[1:] else {n: artkit.load_frames(n) for n in NAMES}
    review(sets)
