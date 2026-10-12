#!/usr/bin/env python3
"""Production art: Level 13's small props, the CDF supply drone and the abandoned research hut
(design/allies, "CDF supply drone"; design/campaign/act-2-homefront/level-13-polar-relay, "Secrets and
pickups"; M5 part G batch, concept round 35, step G2; one version each, decision G9 = a, pending the
user's review). No concept: designed to the READMEs and to the chosen arctic scene's light
(scene-arctic-r09-a). Also the review stand-in for the halted relay frame (`HaltPlate`), which
relay.py, skimmer.py and whiteout_l13.py share until the production backdrop (tools/art/backdrop_l13.py,
step G5) exists.

Outputs (written to assets/sprites/ only with --assets, in the content step's time: no data claims
these names yet, and packAtlases fails on sprites no data claims):
  supply-drone_0..3        40x40 the drone seen from above, nose up (it does not turn on its arc): an
                           H-frame of two pale-grey rails with a CDF-blue stripe, four ducted fans at
                           their ends, an avionics pod at the nose, a cargo bay between the rails with
                           its clamps closed; the game draws the armour patch pickup in the bay
                           (pickup-armour-patch at 0.75 scale, centred on the frame) until the release;
                           four rotor-blur phases (suggested 20 fps)
  supply-drone-open_0..3   40x40 the same with the clamps swung open (after the release)
  supply-drone-lights_0..1 40x40 additive: the red (left) and green (right) navigation lights, with the
                           white belly strobe in frame 0 (blink 0.1 s every 0.8 s)
  research-hut_0..3        48x40 the abandoned CDF research hut on its floe: a rust-orange arched
                           prefab (a Quonset with ribs) drifted with snow on its windward side, grey end
                           walls, a square roof hatch with an amber beacon, a guyed radio mast, two fuel
                           drums and a sledge; 0 intact, 1 one hit (the hatch scorched), 2 two hits
                           (the hatch dented ajar), 3 opened (the hatch blown off, the dark hold); rule
                           7's light rim (secret targets)
  research-hut-glow_0..7   48x40 additive: the hatch beacon (GroundGlow: 0-3 a slow blink until the
                           first hit, 4-7 a hurried flicker after it), as Level 12's canister
  design/campaign/act-2-homefront/level-13-polar-relay/concept/l13-props-final-r35-a.png/.gif

Run: python3 tools/art/l13_props.py [--assets]   (~1 min; without --assets only the review files)
"""
import sys
from concurrent.futures import ProcessPoolExecutor

import numpy as np
from PIL import Image

import artkit
from artkit import DESIGN, TAU, sprite

import ground_targets as gt  # noqa: E402  (concept script, imported unchanged: the light rim of rule 7)
import scenes_r06 as s6  # noqa: E402  (concept script, imported unchanged: periodic masks, hillshade)
import scenes_r08 as s8  # noqa: E402  (concept script, imported unchanged: foam, the surface)
import scenes_r09 as s9  # noqa: E402  (concept script, imported unchanged: the chosen arctic scene)
import parallax_r02 as r02  # noqa: E402
import parallax_r03 as r03  # noqa: E402
from render import raster  # noqa: E402
from render.enemy_rigs import write_gif  # noqa: E402
from render.sdf import (Material, mirror_x, rotate_x, rotate_y, rotate_z, sd_box, sd_capsule,  # noqa: E402
                        sd_cylinder_x, sd_cylinder_z, sd_ellipsoid, sd_sphere, union)

SCRIPT = "l13_props.py"
BATCH = "M5 part G batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
REVIEW_ROUND = "r35"
LEVEL = DESIGN / "campaign" / "act-2-homefront" / "level-13-polar-relay"
CONCEPT = LEVEL / "concept"
COLOURS = 32

DRONE = (40, 40)
HUT = (48, 40)
BEACON_RGB = np.array([255, 176, 60], float)
NAV_RED = np.array([255, 60, 50], float)
NAV_GREEN = np.array([80, 255, 120], float)
STROBE_RGB = np.array([235, 245, 255], float)


def premultiplied(rgb, a):
    """An additive frame as the game blends it: rgb x alpha on black, alpha 255 wherever it adds."""
    out = np.clip(np.asarray(rgb, float) * a[..., None], 0, 255)
    alpha = np.where(out.max(axis=-1) >= 1, 255, 0)
    return Image.fromarray(np.dstack([np.round(out), alpha]).astype(np.uint8), "RGBA")


def steps(v, n):
    return np.floor(np.clip(v, 0, 1) * n + 0.5) / n


def glow_disc(size, points, scale=1.0):
    """An additive field of small lights: points [(x, y, radius, level, rgb)] in px."""
    w, h = size
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64) + 0.5
    acc = np.zeros((h, w, 3))
    for x, y, r, level, rgb in points:
        d = np.hypot(xx - x, yy - y)
        a = (np.clip(1 - d / r, 0, 1) ** 1.5 * 0.8 + np.clip(1 - d / 1.4, 0, 1) * 0.7) * level * scale
        acc = np.maximum(acc, steps(a, 5)[..., None] * np.asarray(rgb, float))
    alpha = np.where(acc.max(axis=-1) >= 1, 255, 0)
    return Image.fromarray(np.dstack([np.round(np.clip(acc, 0, 255)), alpha]).astype(np.uint8), "RGBA")


# =========================================================================== the halted frame (review stand-in)

class HaltScene(s9.ArcticScene):
    """The chosen arctic scene (scenes_r09.ArcticScene, its ice, water, foam and fog) re-cut for
    Level 13's halted frame: the relay's floe at the play field's centre (x 240, y 250 from the top,
    290 y-up), the leads cut along the Skimmers' lanes round it (G4 = a: the backdrop cuts the leads
    along the lanes, wide enough for the 40 px weave). A review stand-in only."""

    SHELF = (240, 250, 112)
    CARVE = 60                                         # px: a lane's half width (40 px weave + the hull)

    def lanes(self):
        """The review's lanes in screen px (the halted frame, y down): a west and an east lead near
        the side edges and a south crossing below the floe, rising to the right."""
        ys = np.arange(-80, 640, 20.0)
        west = [(64 + 12 * np.sin(y / 70.0) + 6 * np.sin(y / 23.0 + 0.7), y) for y in ys]
        east = [(416 + 10 * np.sin(y / 60.0 + 1.0) + 6 * np.sin(y / 31.0), y) for y in ys]
        xs = np.arange(-80, 560, 20.0)
        south = [(x, 486 - 0.08 * x + 10 * np.sin(x / 55.0 + 0.4)) for x in xs]
        return {"west": west, "east": east, "south": south}

    def _ice(self, per):
        """r09's ice with the lanes carved instead of its two leads (the same noise, shading and
        margins; the code follows scenes_r09.ArcticScene._ice)."""
        n = r02.periodic_fbm(s9.W, per, 96, 1311, octaves=4)
        cx, cy, R = self.SHELF
        d = np.hypot(s9.XX[:per] - cx, s6.periodic_dist(s9.YY[:per], cy, per)) / R
        n = n + np.clip(1.25 - d, 0, 1) * 0.9 + 0.17
        dist = np.full(n.shape, 1e9)
        pts = np.stack([s9.XX[:per].ravel(), s9.YY[:per].ravel()], 1).astype(np.float64)
        for lane, shift in [(lane, k * per) for lane in self.lanes().values() for k in (-1, 0, 1)]:
            p = np.array(lane) + np.array([0.0, shift])      # the ground repeats every `per` rows
            for a, b in zip(p[:-1], p[1:]):
                ab = b - a
                t = np.clip(((pts - a) @ ab) / (ab @ ab), 0, 1)
                dd = np.hypot(*(pts - (a + t[:, None] * ab)).T).reshape(n.shape)
                dist = np.minimum(dist, dd)
        rough = r02.periodic_fbm(s9.W, per, 48, 1314, octaves=3)
        rough = (rough - rough.mean()) / (rough.std() + 1e-9)
        n = np.where(dist < self.CARVE + 13 * rough, 0.0, n)
        ice = n > 0.58
        ice = s6.pblur_mask(ice.astype(np.float64), 1.2) > 0.5
        self.mask = ice
        hgt = s6.pblur_mask(ice.astype(np.float64), 3.0)
        rid = r02.periodic_fbm(s9.W, per, 24, 1312, octaves=2)
        hgt = hgt * (0.8 + 0.25 * rid) + np.where(np.abs(rid - 0.5) < 0.03, 0.15, 0) * ice
        shade = s6.hillshade(hgt * 30, 0.7)
        col = raster.ramp(s9.ICE9, np.clip(0.25 + 0.55 * hgt * (0.7 + 0.3 * rid), 0, 1))
        col = col * shade[..., None]
        rim = ice & ~(np.roll(ice, 2, 0) & np.roll(ice, 2, 1) & np.roll(ice, -1, 0) & np.roll(ice, -1, 1))
        col[rim] = col[rim] * 0.8
        col[~ice] = s9.ICE9[0][1]
        self.ice_tex = s6.smooth_tex(r02.posterize(raster.to_rgba_image(np.clip(col, 0, 255), ice * 255), 10))
        grow = s6.pblur_mask(ice.astype(np.float64), 6.0)
        margin = np.clip((grow - 0.05) * 3.0, 0, 1) * ~ice
        shade_w = np.roll(np.roll(s6.pblur_mask(ice.astype(np.float64), 2.0), 12, 0), 8, 1)
        under = np.zeros((per, s9.W, 3))
        under[:] = np.array((52, 124, 142))
        self.under = r03.step_alpha(raster.to_rgba_image(under, margin * 160), 5)
        sh = np.zeros((per, s9.W, 3))
        self.cast_sh = r03.step_alpha(raster.to_rgba_image(sh, shade_w * 120 * ~ice), 4)
        edge = ice & ~(np.roll(ice, 1, 0) & np.roll(ice, -1, 0) & np.roll(ice, 1, 1) & np.roll(ice, -1, 1))
        ys, xs = np.nonzero(edge)
        rng = np.random.default_rng(1313)
        keep = rng.random(len(xs)) < 0.45
        self.edge = np.stack([xs[keep], ys[keep]], 1).astype(np.float64)
        self.edge_ph = rng.uniform(0, TAU, len(self.edge))
        self.edge_k = rng.integers(1, 4, len(self.edge))
        self.station = None


class HaltPlate:
    """The halted frame's ground (water, ice, waterline foam; no units, no station) at loop frame f
    (the scene's 80-frame water loop), 480x540; cached per frame."""

    def __init__(self):
        self.scene = HaltScene()
        self.cache = {}

    def lanes(self):
        return self.scene.lanes()

    def ground(self, f=0):
        f = int(f) % s9.N
        if f in self.cache:
            return self.cache[f].copy()
        sc, off = self.scene, 0.0
        rows = sc.sea.rows(off)
        h = sc.sea.height(f, off, sc.sea_amp)
        light = sc.sea.light(h)
        body = sc.sea.colour(light)
        body = s8.through_surface(body, s9.scroll_tex(sc.under, off), sc.sea.mean(), 1.0)
        ground = raster.to_rgba_image(np.clip(body, 0, 255))
        ground.alpha_composite(s9.scroll_tex(sc.cast_sh, off))
        ground.alpha_composite(s9.scroll_tex(sc.ice_tex, off))
        foam = s8.Foam()
        inv = np.full(sc.TRAVEL, -1)
        inv[rows] = np.arange(s9.H)
        ey = inv[sc.edge[:, 1].astype(int)]
        vis = ey >= 0
        for (ex, _), yy, ph, k in zip(sc.edge[vis], ey[vis], sc.edge_ph[vis], sc.edge_k[vis]):
            val = 0.5 + 0.5 * np.sin(s9.th(f, k) + ph) * np.sin(s9.th(f, 2) + ph * 1.3)
            if val > 0.35:
                foam.dot(ex + 0.8 * np.sin(ph), yy + 0.8 * np.cos(ph), 0.8 + 0.7 * val, 0.75 * val)
        ground.alpha_composite(foam.done())
        self.cache[f] = ground
        return ground.copy()

    def fog(self, f=0, gain=1.0):
        """The scene's thin fog bank layer (22 %), for the review's atmosphere."""
        sc = self.scene
        img = s9.scroll_tex(sc.fog, sc.s.off("low-air", f))
        if gain != 1.0:
            a = np.array(img).astype(np.float64)
            a[..., 3] *= gain
            img = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")
        return img

    def snow(self, f=0):
        return self.scene.snow_layer(f)


# =========================================================================== the supply drone

D_HULL, D_STRIPE, D_DARK, D_DUCT, D_GLASS, D_CLAMP = range(6)


def drone_mats():
    return [Material((0.80, 0.82, 0.84), metal=0.3, shininess=50, spec=0.5),
            Material((0.16, 0.36, 0.70), metal=0.3, shininess=50, spec=0.5),
            Material((0.18, 0.20, 0.23), metal=0.5, shininess=40, spec=0.4),
            Material((0.46, 0.49, 0.53), metal=0.6, shininess=60, spec=0.6),
            Material((0.10, 0.16, 0.22), metal=0.9, shininess=120, spec=1.2),
            Material((0.95, 0.70, 0.18), metal=0.3, shininess=40, spec=0.4)]


DUCTS = [(-11.5, 10.5), (11.5, 10.5), (-11.5, -10.5), (11.5, -10.5)]
DUCT_R = 6.4


def drone_scene(open_):
    """Model units = px, y up the screen (the nose). Two rails, four ducts, a nose pod, the bay's
    four clamps (closed: pointing in; open: swung out)."""
    def scene(p):
        q = mirror_x(p)
        rail = sd_box(q, (7.0, 0.0, 2.0), (1.8, 11.0, 1.6), 0.8)
        stripe = sd_box(q, (7.0, 0.0, 3.4), (0.7, 7.0, 0.5), 0.2)
        arm = sd_box(q, (9.5, 10.5, 2.0), (2.6, 1.1, 0.9), 0.4)
        arm2 = sd_box(q, (9.5, -10.5, 2.0), (2.6, 1.1, 0.9), 0.4)
        ring = np.minimum.reduce([np.maximum(np.abs(np.hypot(q[:, 0] - 11.5, q[:, 1] - sy) - DUCT_R) - 1.1,
                                             np.abs(q[:, 2] - 2.4) - 1.6) - 0.3 for sy in (10.5, -10.5)])
        hub = np.minimum.reduce([sd_cylinder_z(q, (11.5, sy, 1.6), 1.4, 0.8) for sy in (10.5, -10.5)])
        spokes = np.minimum.reduce([sd_box(q, (11.5, sy, 1.4), (DUCT_R, 0.35, 0.3), 0.1) for sy in (10.5, -10.5)])
        nose = sd_ellipsoid(p, (0.0, 12.5, 2.4), (5.2, 4.2, 2.6))
        lens = sd_sphere(p, (0.0, 15.2, 3.2), 1.5)
        cross = sd_box(p, (0.0, 7.6, 1.6), (6.0, 1.1, 0.9), 0.4)
        tail = sd_box(p, (0.0, -11.4, 1.6), (6.0, 1.1, 0.9), 0.4)
        ang = 1.2 if open_ else 0.0
        clamps = []
        for sx in (1, -1):
            for sy in (1, -1):
                piv = np.array([sx * 5.4, sy * 5.6, 3.6])
                c = rotate_z(p - piv, sx * sy * ang)
                clamps.append(sd_box(c, (-sx * 2.0, 0.0, 0.0), (2.2, 0.9, 0.7), 0.3))
        clamp = np.minimum.reduce(clamps)
        return union((rail, D_HULL), (stripe, D_STRIPE), (np.minimum(arm, arm2), D_DARK), (ring, D_DUCT),
                     (hub, D_DARK), (spokes, D_DARK), (nose, D_HULL), (lens, D_GLASS),
                     (np.minimum(cross, tail), D_DARK), (clamp, D_CLAMP), k=0.4)
    return scene


def rotor_blur(img, phase):
    """Each duct's rotor as a translucent blur disc with two brighter blade streaks at ``phase``
    (radians), stepped alpha; drawn under the duct ring (the ring is solid on top)."""
    w, h = img.size
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64) + 0.5
    base = np.array(img).astype(np.float64)
    out = np.zeros_like(base)
    for i, (dx, dy) in enumerate(DUCTS):
        cx, cy = w / 2 + dx, h / 2 - dy
        d = np.hypot(xx - cx, yy - cy)
        ang = np.arctan2(yy - cy, xx - cx)
        spin = phase * (1 if i % 2 else -1)
        streak = np.clip(np.cos(2 * (ang - spin)), 0, 1) ** 10
        disc = (d < DUCT_R - 0.6) * (0.22 + 0.6 * streak * np.clip(d / DUCT_R, 0.3, 1))
        a = steps(disc, 3)
        col = np.array([150, 160, 170]) * (1 - streak[..., None] * 0.3) + np.array([210, 218, 226]) * streak[..., None] * 0.3
        sel = a > out[..., 3] / 255
        out[sel, :3] = col[sel]
        out[sel, 3] = a[sel] * 255
    res = Image.fromarray(out.astype(np.uint8), "RGBA")
    res.alpha_composite(img)
    return res


def drone_frame(open_, k):
    hi, factor = artkit.render_hi(drone_scene(open_), drone_mats(), DRONE, float(DRONE[0]), z_top=14.0, steps=150)
    body = artkit.native(hi, factor, crisp=60)
    return rotor_blur(body, TAU * k / 8 + 0.3)


def drone_lights(k):
    w, h = DRONE
    pts = [(w / 2 - 18.0, h / 2 - 10.5, 3.2, 0.9, NAV_RED), (w / 2 + 18.0, h / 2 - 10.5, 3.2, 0.9, NAV_GREEN)]
    if k == 0:
        pts.append((w / 2, h / 2 + 1.0, 6.0, 1.0, STROBE_RGB))
    return glow_disc(DRONE, pts)


# =========================================================================== the research hut

H_SHELL, H_RIB, H_WALL, H_SNOW, H_HATCH, H_DARK, H_DRUM, H_BEACON, H_WOOD, H_HOLD = range(10)


def hut_mats(beacon_on=False):
    return [Material((0.62, 0.24, 0.12), metal=0.2, shininess=30, spec=0.35),
            Material((0.44, 0.17, 0.09), metal=0.3, shininess=30, spec=0.35),
            Material((0.46, 0.48, 0.50), metal=0.4, shininess=40, spec=0.4),
            Material((0.86, 0.90, 0.95), metal=0.0, shininess=8, spec=0.15),
            Material((0.52, 0.54, 0.50), metal=0.6, shininess=50, spec=0.6),
            Material((0.12, 0.13, 0.15), metal=0.4, shininess=30, spec=0.3),
            Material((0.24, 0.36, 0.22), metal=0.4, shininess=40, spec=0.4),
            Material((0.3, 0.2, 0.1), emission=(2.2, 1.3, 0.4)) if beacon_on
            else Material((0.55, 0.36, 0.12), metal=0.2, shininess=90, spec=0.9),
            Material((0.40, 0.30, 0.20), metal=0.0, shininess=10, spec=0.1),
            Material((0.04, 0.04, 0.05), metal=0.0, shininess=4, spec=0.0)]


HATCH_AT = (2.0, 1.5)          # the roof hatch's centre (model px, y up)
BEACON_AT = (-4.0, 3.0)


def hut_scene(state):
    """Model units = px, y up. The Quonset along x, its windward (left) end drifted with snow."""
    def scene(p):
        shell_r, half = 8.5, 13.0
        body = np.maximum(sd_cylinder_x(p, (0.0, 0.0, 0.0), shell_r, half), -p[:, 2])
        ribs = np.where(np.abs(((p[:, 0] + 13) % 4.0) - 2.0) < 0.45, 1, 0)
        ends = np.maximum(sd_cylinder_x(p, (0.0, 0.0, 0.0), shell_r - 0.6, half + 0.8), -p[:, 2])
        door = sd_box(p, (half + 1.0, 0.0, 2.5), (0.8, 2.2, 2.6), 0.3)
        snow = np.minimum(sd_ellipsoid(p, (-14.5, -1.0, 0.5), (8.0, 11.0, 5.5)),
                          sd_ellipsoid(p, (-6.0, 5.5, 3.5), (10.0, 4.0, 4.5)))
        snow = np.minimum(snow, sd_ellipsoid(p, (4.0, 6.0, 6.0), (8.0, 2.5, 2.6)))
        hx, hy = HATCH_AT
        frame = sd_box(p, (hx, hy, shell_r - 0.2), (3.6, 3.6, 0.7), 0.3)
        items = [(body, H_SHELL), (ends, H_WALL), (door, H_DARK), (snow, H_SNOW)]
        if state == 0:
            items.append((sd_box(p, (hx, hy, shell_r + 0.4), (3.0, 3.0, 0.5), 0.3), H_HATCH))
        elif state == 1:
            items.append((sd_box(p, (hx, hy, shell_r + 0.4), (3.0, 3.0, 0.5), 0.3), H_HATCH))
        elif state == 2:
            lid = rotate_y(p - np.array([hx + 3.0, hy, shell_r + 0.4]), -0.55)
            items.append((sd_box(lid, (-3.0, 0.3, 0.0), (3.0, 3.0, 0.5), 0.3), H_HATCH))
            items.append((sd_box(p, (hx - 0.4, hy, shell_r + 0.1), (2.2, 2.6, 0.3), 0.1), H_HOLD))
        else:
            items.append((sd_box(p, (hx, hy, shell_r + 0.15), (2.9, 2.9, 0.3), 0.1), H_HOLD))
        items.append((frame, H_DARK))
        items.append((sd_sphere(p, (BEACON_AT[0], BEACON_AT[1], shell_r + 0.8), 1.3), H_BEACON))
        mast = sd_capsule(p, (10.0, -6.5, 0.0), (10.0, -6.5, 15.0), 0.45)
        dish = np.maximum(sd_sphere(p, (10.0, -6.5, 12.5), 2.6), -sd_sphere(p, (10.6, -6.0, 14.0), 2.6))
        guys = np.minimum.reduce([sd_capsule(p, (10.0, -6.5, 13.0), (10.0 + gx, -6.5 + gy, 0.0), 0.2)
                                  for gx, gy in ((6.0, 4.0), (5.0, -6.0), (-6.0, -5.0))])
        drums = np.minimum(sd_cylinder_z(p, (-11.0, -9.0, 1.8), 1.9, 1.8), sd_cylinder_z(p, (-7.2, -10.0, 1.8), 1.9, 1.8))
        sledge = np.minimum(sd_box(p, (17.5, 8.5, 1.4), (4.5, 2.2, 0.5), 0.3),
                            sd_box(mirror_x(p - np.array([17.5, 8.5, 0.0])), (2.2, 0.0, 0.6), (0.4, 3.0, 0.4), 0.2))
        items += [(np.minimum(mast, guys), H_DARK), (dish, H_WALL), (drums, H_DRUM), (sledge, H_WOOD)]
        d, m = union(*items, k=0.5)
        m = np.where((m == H_SHELL) & (ribs == 1), H_RIB, m)
        return d, m
    return scene


def hut_frame(state):
    hi, factor = artkit.render_hi(hut_scene(state), hut_mats(state == 0), HUT, float(HUT[0]), z_top=20.0,
                                  steps=170)
    arr = np.array(artkit.native(hi, factor, crisp=60)).astype(np.float64)
    hx, hy = HUT[0] / 2 + HATCH_AT[0], HUT[1] / 2 - HATCH_AT[1]
    if state >= 1:                                        # scorch round the hatch
        arr = gt.scorch(arr, [(hx, hy, 4.5 + 1.5 * state), (hx + 3, hy + 2, 2.5)], 3501 + state)
    return Image.fromarray(gt.light_rim(arr).astype(np.uint8), "RGBA")


def beacon_px():
    return HUT[0] / 2 + BEACON_AT[0], HUT[1] / 2 - BEACON_AT[1]


def hut_glow(k):
    level = (1.0, 0.5, 0.0, 0.0, 1.0, 0.0, 0.6, 0.0)[k]
    bx, by = beacon_px()
    return glow_disc(HUT, [(bx, by, 7.0, level, BEACON_RGB)])


# =========================================================================== build

def _job(job):
    kind, i = job
    if kind == "drone":
        return drone_frame(False, i)
    if kind == "drone-open":
        return drone_frame(True, i)
    if kind == "drone-lights":
        return drone_lights(i)
    if kind == "hut":
        return hut_frame(i)
    return hut_glow(i)


def sprite_sets():
    jobs = ([("drone", k) for k in range(4)] + [("drone-open", k) for k in range(4)]
            + [("drone-lights", k) for k in range(2)] + [("hut", s) for s in range(4)]
            + [("hut-glow", k) for k in range(8)])
    with ProcessPoolExecutor() as pool:
        out = list(pool.map(_job, jobs))
    drones = artkit.quantize_set(out[0:8], COLOURS)
    return {"supply-drone": drones[:4], "supply-drone-open": drones[4:8],
            "supply-drone-lights": artkit.quantize_set(out[8:10], 12),
            "research-hut": artkit.quantize_set(out[10:14], COLOURS),
            "research-hut-glow": artkit.quantize_set(out[14:22], 12)}


def write_assets(sets):
    for name, frames in sets.items():
        artkit.write_frames(name, frames, SOURCE)
        print(f"{name}: {len(frames)} frames, {frames[0].width}x{frames[0].height}")


# =========================================================================== review

def put(base, img, x, y):
    base.alpha_composite(img, (int(round(x - img.width / 2)), int(round(y - img.height / 2))))


def add(base, img, x, y):
    return artkit.add_light(base, img, (int(round(x - img.width / 2)), int(round(y - img.height / 2))))


def shadow(img, dx=6, dy=9, alpha=0.4):
    a = np.array(img)[..., 3] > 0
    sh = np.zeros(a.shape + (4,), np.uint8)
    sh[a, 3] = int(255 * alpha)
    return Image.fromarray(sh, "RGBA"), dx, dy


def scaled(img, s):
    w, h = max(1, int(round(img.width * s))), max(1, int(round(img.height * s)))
    return img.resize((w, h), Image.NEAREST)


def drone_arc(t, start=(240, 250), top=(240, 160), dur=4.0):
    """The drone's arc (README: lifts off the helipad, a slow arc over the relay, the release at the
    arc's top): a rise over 0.8 s with the scale 0.7 -> 1.0, then a half loop up and over to the
    release point; returns (x, y, scale, released)."""
    u = np.clip(t / dur, 0, 1)
    sx, sy = start
    tx, ty = top
    x = sx + 34 * np.sin(np.pi * u) * (1 - u) + (tx - sx) * u
    y = sy + (ty - sy) * (1 - np.cos(np.pi * u)) / 2 - 10 * np.sin(np.pi * u)
    return x, y, 0.7 + 0.3 * min(1.0, t / 0.8), u >= 1.0


def review(sets, relay_frames=None, relay_pad=(0, 0)):
    """The sheet and a loop: the hut on the floe of the approach (its beacon blinking, three hits,
    opened), and the drone lifting off the relay's helipad on its arc to the release point (the
    armour patch in its bay, then falling free as the pickup)."""
    artkit.REVIEW_ROUND = REVIEW_ROUND
    plate = HaltPlate()
    patch = artkit.load_frames("pickup-armour-patch")
    boom = artkit.load_frames("explosion-small")
    drone_bay = []
    for k in range(4):
        cell = artkit.checker(40, 40)
        put(cell, scaled(patch[0], 0.75), 20, 20)
        put(cell, sets["supply-drone"][k], 20, 20)
        drone_bay.append(cell)
    on_ice = []
    g0 = plate.ground(0)
    for s in range(4):
        cell = g0.crop((180, 300, 252, 360)).copy()
        put(cell, sets["research-hut"][s], 36, 30)
        if s == 0:
            cell = add(cell, sets["research-hut-glow"][0], 36, 30)
        on_ice.append(cell)
    sheet = artkit.review_sheet("LEVEL 13 PROPS - SUPPLY DRONE AND RESEARCH HUT (ONE VERSION EACH)", [
        ("SUPPLY DRONE 40X40, NOSE UP, CLAMPS CLOSED: 4 ROTOR-BLUR PHASES", sets["supply-drone"], 4, False),
        ("CLAMPS OPEN (AFTER THE RELEASE)", sets["supply-drone-open"], 4, False),
        ("NAV LIGHTS (ADDITIVE): RED LEFT, GREEN RIGHT, WHITE BELLY STROBE IN FRAME 0", sets["supply-drone-lights"], 4,
         True),
        ("AS DRAWN: THE ARMOUR PATCH PICKUP (0.75 SCALE) IN THE BAY UNDER THE DRONE", drone_bay, 4, False),
        ("RESEARCH HUT 48X40: INTACT, 1 HIT (SCORCHED), 2 HITS (HATCH AJAR), OPENED", sets["research-hut"], 4, False),
        ("HATCH BEACON (ADDITIVE): 0-3 SLOW BLINK, 4-7 HURRIED FLICKER AFTER THE FIRST HIT",
         sets["research-hut-glow"], 3, True),
        ("ON THE ICE (THE HALTED FRAME'S STAND-IN PLATE)", on_ice, 3, False),
    ], width=1300, batch=BATCH)
    gif = review_loop(plate, sets, patch, boom, relay_frames, relay_pad)
    artkit.save_review(sheet, gif, CONCEPT, "l13-props", fps=20)


def review_loop(plate, sets, patch, boom, relay_frames, relay_pad):
    fw, fh, fps = 240, 270, 20
    x0, y0 = 120, 110                                      # the crop: the relay's floe and above it
    hut_at = (300 + 36 - x0 + 50, 40 + 30 - y0 + 90)
    gif = []
    hits = (1.0, 1.6, 2.2)
    for i in range(int(6.5 * fps)):
        t = i / fps
        cell = plate.ground(i % 80).crop((x0, y0, x0 + fw, y0 + fh))
        hx, hy = 60, 70
        state = sum(t >= h for h in hits)
        put(cell, sets["research-hut"][state], hx, hy)
        if state < 3:
            k = (int(t * 8) % 4) + (4 if state else 0)
            cell = add(cell, sets["research-hut-glow"][k], hx, hy)
        for h in hits:
            age = int((t - h) * 60)
            if 0 <= age < 24:
                cell = add(cell, boom[min(age // 2, len(boom) - 1)], hx + 2, hy - 2)
        if relay_frames is not None:
            put(cell, relay_frames, 240 - x0, 250 - y0)
        pad = (240 - x0 + relay_pad[0], 250 - y0 + relay_pad[1])
        lt = t - 1.5
        if lt >= 0:
            x, y, s, released = drone_arc(lt, start=pad, top=(240 - x0, 160 - y0))
            k = int(t * fps) % 4
            dr = sets["supply-drone-open" if released else "supply-drone"][k]
            sh, dx, dy = shadow(scaled(dr, s), int(3 + 6 * s), int(4 + 9 * s))
            put(cell, sh, x + dx, y + dy)
            if not released:
                put(cell, scaled(patch[0], 0.75 * s), x, y)
            put(cell, scaled(dr, s), x, y)
            cell = add(cell, scaled(sets["supply-drone-lights"][0 if int(t * 10) % 8 == 0 else 1], s), x, y)
            if released:
                rt = lt - 4.0
                put(cell, patch[int(t * 10) % len(patch)], 240 - x0, 160 - y0 + 20 * rt)
        gif.append(sprite.enlarge(cell, 2))
    return gif


def main(args):
    sets = sprite_sets()
    if "--assets" in args:
        write_assets(sets)
    relay_img, pad = None, (0, 0)
    try:
        import relay  # tools/art: the relay's variant a for the drone's helipad
        relay_img = relay.preview_frame("a")
        pad = relay.helipad("a")
    except Exception as e:  # the relay script may not be built yet; the loop then shows the pad's spot
        print("relay preview unavailable:", e)
    review(sets, relay_img, pad)


if __name__ == "__main__":
    main(sys.argv[1:])
