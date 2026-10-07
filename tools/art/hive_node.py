#!/usr/bin/env python3
"""Production art: the Hive Node, Act 2's hardened Vrell spawner mound (design/enemies/ground/hive-node;
M5 part C batch, concept round 31). Straight to production from its chosen concept (user decision
D9 = a): the round-06 model hive-node-r06-a (tools/concept/render/r06_models.hive_node, imported
unchanged) and the round-06 creep (tools/concept/enemies_r06.creep, its look redrawn at the
production bar).

Outputs (assets/sprites/; the size from the Hive Node's stat block, 76x76, `radial`, one heading):
  hive-node_0..23           76x76, ground layer: 6 iris states x 4 pulse frames, indexed
                            iris * 4 + pulse; iris 0 shut, 1-4 opening (0.2 .. 0.8), 5 open; the
                            pulse a breathing loop of the seven lobes (swell 0, 0.4, 1, 0.6)
  hive-node-glow_0..23      76x76, additive, same order: the throat, the polyps on the lobes and
                            the sac's veins alone, the throat dim while shut (30 %) and full while
                            open, with a soft teal halo over the iris growing with its opening (the
                            spawn's telegraph: the iris glows while it opens); drawn above the
                            low-air layer
  hive-node-creep_0..8      192x192, solid, ground (drawn under the node, centred on it): the
                            biomass creep patch; frame 0 alive (teal-black blobs, root tendrils with
                            teal veins), frames 1-8 its wither after the node's death (blobs shrink,
                            dry to grey-brown and crumble away, tendrils pull back, the veins go out),
                            8 the dry crust left
  hive-node-death_0..15     128x128, additive: the `large` organic burst from the iris: a
                            teal-white flash, teal ichor globules with short trails, a teal-black
                            spore mist that billows low and thins, motes (4 steps per frame)
  hive-node-tatters_0..15   128x128, solid: the seven bone armour plates with their spikes flung
                            out, the five iris petals, the lobes slumping and shrivelling where they
                            were (4 steps per frame)
  hive-node-stump           76x76, solid (single): the collapsed mound left for the remains' time:
                            the sac slumped flat and torn open, the lobes deflated and scorched,
                            three plate stubs, no glow
  design/enemies/ground/hive-node/concept/hive-node-final-r31-a.png/.gif

For Level 09's night city (the last hour of night, user decision D9; Level 08's lighting) the
teal-black chitin is lifted, the dark tendrils and petals mixed toward it, and the frames get the
Creeper's 1 px lavender-white light rim (tools/art/creeper.py light_rim). The model is radial and
never turns, so one heading is rendered under the fixed top-left key light.

Run: python3 tools/art/hive_node.py [--review]   (~2 min; --review only rebuilds the review files)
"""
import sys
from concurrent.futures import ProcessPoolExecutor
from dataclasses import replace

import numpy as np
from PIL import Image, ImageDraw

import artkit
from artkit import DESIGN, ROOT, TAU, sprite

import creeper  # noqa: E402  (tools/art: the night city's light rim, the multi-material pieces)
import vfx_r08 as v8  # noqa: E402  (concept script, imported unchanged)
import vrell_deaths as vd  # noqa: E402  (tools/art: the death pieces' pose and the motes)
import vrell_fx as fx  # noqa: E402  (tools/art: easing, timing)
from render import enemy_models as em  # noqa: E402
from render import raster  # noqa: E402
from render import r06_models as m6  # noqa: E402
from render.enemy_models import V_BODY, V_BONE, V_DARK, V_EYE, V_GLOW, V_SAC  # noqa: E402
from render.sdf import Material, sd_capsule, sd_cylinder_z, sd_ellipsoid, subtract, union  # noqa: E402

# =========================================================================== parameters

SCRIPT = "hive_node.py"
BATCH = "M5 part C batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
REVIEW_ROUND = "r31"
CONCEPT = DESIGN / "enemies" / "ground" / "hive-node" / "concept"
STEP = 60                                       # game steps per second

SIZE = (76, 76)                                 # stat block
EXTENT = 2.3                                    # model units across the sprite (the concept's)
PX = SIZE[0] / EXTENT                           # px per model unit (~33)
IRIS_STATES = 6                                 # 0 shut .. 5 open
PULSES = (0.0, 0.4, 1.0, 0.6)                   # the lobes' swell over the breathing loop
COLOURS = 40
GLOW_COLOURS = 16
# Readability on the night city: the teal-black lifted, the dark parts mixed toward it, a light rim
BODY_LIFT = 1.45                                # the lobes' and sac's albedo scale
DARK_LIFT = 0.4                                 # tendrils and petals mixed this far toward the lobes
BONE_LIFT = 1.0
THROAT_SHUT = 0.3                               # the throat's glow while shut (of its full glow)
HALO = 0.55                                     # the open iris's halo in the glow frames
TEAL = np.array(em.hx(em.GLOWS["teal"])) * 255

CREEP_SIZE = (192, 192)
CREEP_FRAMES = 9                                # 0 alive, 1-8 the wither
CREEP_STEPS = 15                                # 8 frames x 15 steps = the stat block's 2 s
CREEP_COLOURS = 24

DEATH_SIZE = (128, 128)
DEATH_FRAMES = 16
DEATH_STEPS = 4                                 # EnemyLooks.DEATH_FRAME_TICKS
DEATH_COLOURS = 32
DEATH_EXTENT = EXTENT * DEATH_SIZE[0] / SIZE[0]
STUMP_COLOURS = 24
STUMP_TONE = 0.7                                # scorched: albedo scale of the lifted mound

K = 7                                           # lobes
SEC = TAU / K


# =========================================================================== model

def iris_of(i):
    return i / (IRIS_STATES - 1)


def lifted_mats(iris=1.0, glow=1.0):
    """The round-04 teal-black scheme lifted for the night city; the throat (V_GLOW) glows by the
    iris's opening."""
    mats = m6.hive_node(0.0, 0.0)[1]
    mid = np.array(mats[V_BODY].albedo)
    for slot in (V_BODY, 2, V_SAC):
        mats[slot] = replace(mats[slot], albedo=tuple(np.minimum(1, np.array(mats[slot].albedo) * BODY_LIFT)))
    dark = np.array(mats[V_DARK].albedo)
    mats[V_DARK] = replace(mats[V_DARK], albedo=tuple(dark + (mid * BODY_LIFT - dark) * DARK_LIFT))
    mats[V_BONE] = replace(mats[V_BONE], albedo=tuple(np.minimum(1, np.array(mats[V_BONE].albedo) * BONE_LIFT)))
    throat = THROAT_SHUT + (1 - THROAT_SHUT) * iris
    mats[V_GLOW] = replace(mats[V_GLOW], emission=tuple(np.array(mats[V_GLOW].emission) * throat))
    return [replace(m, emission=tuple(np.array(m.emission) * glow)) for m in mats]


def node_mats(kind, iris):
    mats = lifted_mats(iris)
    if kind == "glow":                                        # throat, polyps and the sac's veins alone
        black = Material((0.0, 0.0, 0.0), metal=0.0, shininess=1.0, spec=0.0)
        return [replace(m, albedo=(0.0, 0.0, 0.0), metal=0.0, spec=0.0, pattern=None)
                if i in (V_GLOW, V_EYE, V_SAC) else black for i, m in enumerate(mats)]
    return mats


def model(i, p, kind="unit"):
    """(scene, mats) at iris state i, pulse frame p."""
    iris = iris_of(i)
    return m6.hive_node(iris, PULSES[p])[0], node_mats(kind, iris)


def halo(img, iris):
    """The open iris's soft teal light added to a straight-alpha glow render."""
    a = np.array(img.convert("RGBA")).astype(np.float64)
    rgb = a[..., :3] * a[..., 3:4] / 255
    h, w = rgb.shape[:2]
    yy, xx = np.mgrid[0:h, 0:w]
    d2 = (xx + 0.5 - w / 2) ** 2 + (yy + 0.5 - h / 2) ** 2
    sigma = 5 + 8 * iris
    rgb += TEAL * HALO * iris * np.exp(-d2 / (2 * sigma * sigma))[..., None]
    rgb = np.clip(rgb, 0, 255)
    return Image.fromarray(np.dstack([rgb, np.full((h, w), 255.0)]).astype(np.uint8), "RGBA")


def render_unit(i, p, kind):
    img = artkit.native(*artkit.render_hi(*model(i, p, kind), SIZE, EXTENT))
    if kind == "glow":
        return artkit.additive(halo(img, iris_of(i)))
    return creeper.light_rim(img)


# =========================================================================== creep

CREEP_SS = 4                                    # supersampling of the creep's drawing
CREEP_R = 86                                    # px: the patch's reach (the concept's 120 on its 480 px field, fitted)


def creep_layout():
    """The patch's blobs and tendrils, fixed: blobs (x, y, r, shade, crumble), tendrils as point
    lists from the mound outwards, each with the wither fraction its points have pulled back by."""
    rng = np.random.default_rng(3101)
    c = CREEP_SIZE[0] / 2
    blobs = []
    for _ in range(110):
        a, r = rng.uniform(0, TAU), CREEP_R * 0.9 * np.sqrt(rng.uniform(0.04, 1))
        s = rng.uniform(4, 12) * (1 - r / CREEP_R * 0.55)
        blobs.append((c + np.cos(a) * r, c + np.sin(a) * r * 0.9, s, rng.uniform(0.75, 1.15),
                      0.15 + 0.85 * (r / CREEP_R) ** 0.7 * rng.uniform(0.5, 1.0)))
    tendrils = []
    for j in range(9):
        a0 = j * TAU / 9 + rng.uniform(-0.2, 0.2)
        curl = rng.uniform(-0.6, 0.6)
        reach = CREEP_R * rng.uniform(0.85, 1.0)
        tendrils.append([(c + np.cos(a0 + curl * u * u) * reach * (0.25 + 0.75 * u),
                          c + np.sin(a0 + curl * u * u) * reach * (0.25 + 0.75 * u) * 0.9)
                         for u in np.linspace(0, 1, 28)])
    return blobs, tendrils


def mix(a, b, t):
    return tuple(int(round(x + (y - x) * t)) for x, y in zip(a, b))


MAT = (12, 36, 38)                              # the mat between the blobs
BLOB = (18, 52, 50)                             # the live biomass: teal-black, lifted for the night
BLOB_LIT = (34, 86, 80)
DRY = (44, 40, 36)                              # withered: grey-brown crust
DRY_LIT = (62, 58, 52)
TENDRIL = (12, 60, 54)
VEIN = (0, 160, 112)


def creep_frame(f):
    """The patch at wither fraction f/8 (0 alive)."""
    w = f / (CREEP_FRAMES - 1)
    blobs, tendrils = creep_layout()
    W, H = CREEP_SIZE[0] * CREEP_SS, CREEP_SIZE[1] * CREEP_SS
    S = CREEP_SS
    dry = fx.ease_out(w, 1.6)
    # the continuous mat under the blobs: noise over a radial falloff, receding as it withers
    yy, xx = np.mgrid[0:H, 0:W]
    rr = np.hypot(xx / S - CREEP_SIZE[0] / 2, (yy / S - CREEP_SIZE[1] / 2) / 0.9) / CREEP_R
    field = raster.fbm(W, H, 18 * S, 3102, octaves=4, period=False) * 0.7 + (1 - rr) * 0.75
    mat = field > 0.62 + 0.22 * w
    base = np.zeros((H, W, 4), np.uint8)
    base[mat] = mix(MAT, DRY, dry) + (255,)
    img = Image.fromarray(base, "RGBA")
    d = ImageDraw.Draw(img)
    for x, y, s, shade, crumble in blobs:                     # base blobs, then their key-lit tops
        if crumble < w * 0.8:
            continue                                          # crumbled away (the rim first)
        r = s * (1 - 0.45 * w) * S
        col = mix(tuple(int(c * shade) for c in BLOB), DRY, dry)
        d.ellipse([x * S - r, y * S - r, x * S + r, y * S + r], fill=col + (255,))
    for x, y, s, shade, crumble in blobs:
        if crumble < w * 0.8:
            continue
        r = s * (1 - 0.45 * w) * S
        col = mix(tuple(int(c * shade) for c in BLOB_LIT), DRY_LIT, dry)
        d.ellipse([x * S - r * 0.75, y * S - r * 0.75, x * S + r * 0.05, y * S + r * 0.05], fill=col + (255,))
    for pts in tendrils:                                      # tendrils pull back toward the mound
        keep = max(2, int(len(pts) * (1 - 0.7 * w)))
        line = [(x * S, y * S) for x, y in pts[:keep]]
        d.line(line, fill=mix(TENDRIL, DRY, dry) + (255,), width=int(5 * S * (1 - 0.3 * w)))
        vein = mix(VEIN, DRY_LIT, min(1.0, w * 1.6))
        if w < 1:
            d.line(line, fill=vein + (255,), width=max(1, int(1.2 * S)))
    a = np.array(img).astype(np.float64).reshape(CREEP_SIZE[1], S, CREEP_SIZE[0], S, 4)
    cover = a[..., 3].mean(axis=(1, 3)) / 255                 # box downsample, colour weighted by coverage
    rgb = (a[..., :3] * a[..., 3:4] / 255).mean(axis=(1, 3)) / np.maximum(cover, 1e-6)[..., None]
    alpha = cover >= 0.5
    rgb = np.where(alpha[..., None], np.round(rgb), 0)
    return Image.fromarray(np.dstack([np.clip(rgb, 0, 255), alpha * 255]).astype(np.uint8), "RGBA")


# =========================================================================== death

def death_glow(i):
    """The iris bursting: a teal-white flash, teal ichor globules with short trails, a teal-black
    spore mist billowing low and thinning, a few motes."""
    w, h = DEATH_SIZE
    t = fx.t_of(i, DEATH_FRAMES)
    rng = np.random.default_rng(3121)
    c = w / 2
    core = 0.42 * PX                                          # ~14 px: the open throat
    cv = v8.Canvas(w, h)
    d = cv.dist(c, c)
    fade = np.clip((w / 2 - d) / 6.0, 0, 1)
    tex = v8._cart_noise(w, 3122, cell=w * v8.SS // 6)
    churn = v8._shift(tex, t * w * 0.2, -t * w * 0.16)
    grow = fx.ease_out(t, 2.0)
    dens = v8.gauss(d, core * (1.4 + 2.0 * grow))
    haze = np.clip(dens * (0.5 + 0.9 * churn) - 0.2 - 0.5 * t, 0, 1)
    env = np.clip((i + 1) / 2.0, 0.4, 1) * (1 - t) ** 0.8
    cv.add((10, 56, 50), haze * 1.4 * env * fade)                    # the spore mist's teal-black body
    cv.add(TEAL, np.clip(haze - 0.45, 0, 1) * 0.55 * env * fade)
    flash = max(0.0, 1 - i / 5.0)
    cv.add((220, 255, 240), v8.gauss(d, core * (0.7 + 0.6 * t)) * 1.4 * flash)
    cv.add(TEAL, v8.gauss(d, core * (1.2 + 0.9 * t)) * 0.9 * flash)
    for _ in range(60):
        a, start, reach, size = rng.uniform(0, TAU), rng.uniform(0.4, 1.0), rng.uniform(0.7, 1.7), rng.uniform(0.6, 1.6)
        delay, decay = rng.uniform(0, 0.18), rng.uniform(0.8, 1.3)
        if t < delay:
            continue
        tt = np.clip((t - delay) / (1 - delay), 0, 1)

        def at(u, a=a, start=start, reach=reach):
            r = core * start + core * 1.9 * reach * fx.ease_out(u, 2.6)
            return c + np.cos(a) * r, c + np.sin(a) * r + 2.5 * u * u
        x, y = at(tt)
        xp, yp = at(max(tt - 0.1, 0.0))
        life = np.clip(1.1 - tt * decay, 0, 1)
        cv.add(TEAL, v8.gauss(cv.seg(xp, yp, x, y), size * 0.8) * 0.85 * life * fade)
        if size > 1.1:
            cv.add((210, 255, 240), v8.gauss(cv.dist(x, y), size * 0.6) * 1.0 * life * fade)
    vd.motes(cv, rng, c, t, 14, (18, 44), (0.5, 1.0), TEAL, (235, 255, 248), fade)
    return artkit.additive(cv.image())


def _turn(q, ang):
    """Points turned by -ang about the view axis (a part built on the +X axis placed at angle ang)."""
    c, s = np.cos(-ang), np.sin(-ang)
    r = q.copy()
    r[:, 0], r[:, 1] = c * q[:, 0] - s * q[:, 1], s * q[:, 0] + c * q[:, 1]
    return r


def death_pieces():
    """The seven bone plates with their spikes, the five iris petals and the seven lobes, which
    slump where they are and shrivel; every piece a list of (geometry, material slot)."""
    rng = np.random.default_rng(3131)
    pieces = []
    for k in range(K):                                       # plates and spikes between the lobes
        ang = k * SEC + SEC / 2

        def plate(q, ang=ang):
            r = _turn(q, ang)
            return union((sd_ellipsoid(r, (0.62, 0.0, 0.16), (0.16, 0.12, 0.2)), 0),
                         (sd_capsule(r, (0.7, 0.0, 0.28), (0.98, 0.0, 0.42), 0.06, 0.012), 0), k=0.04)[0]
        c0 = (0.75 * np.cos(ang), 0.75 * np.sin(ang), 0.25)
        pieces.append(dict(parts=[(plate, V_BONE)], c0=c0, dir=vd.outward(rng, c0, 0.3),
                           reach=rng.uniform(0.35, 0.7), ease=3.0, axis=rng.uniform(0, TAU),
                           tumble=rng.uniform(3, 6) * rng.choice([-1, 1]), spin=rng.uniform(-2.5, 2.5),
                           rise=0.4, shrink_at=0.7))
    for j in range(5):                                       # the iris petals
        ang = j * TAU / 5
        c, s = np.cos(ang), np.sin(ang)
        r0 = 0.34

        def petal(q, c=c, s=s, r0=r0):
            return sd_capsule(q, (c * (r0 + 0.12), s * (r0 + 0.12), 0.32), (c * r0, s * r0, 0.36), 0.07, 0.03)
        c0 = (c * (r0 + 0.06), s * (r0 + 0.06), 0.34)
        pieces.append(dict(parts=[(petal, V_DARK)], c0=c0, dir=vd.outward(rng, c0, 0.4),
                           reach=rng.uniform(0.5, 0.9), ease=2.6, axis=rng.uniform(0, TAU),
                           tumble=rng.uniform(5, 8) * rng.choice([-1, 1]), spin=rng.uniform(-4, 4),
                           rise=0.5, shrink_at=0.6))
    for k in range(K):                                       # the lobes slump outward and shrivel
        ang = k * SEC

        def lobe(q, ang=ang):
            return sd_ellipsoid(_turn(q, ang), (0.5, 0.0, 0.1), (0.3, 0.24, 0.26))
        c0 = (0.5 * np.cos(ang), 0.5 * np.sin(ang), 0.1)
        pieces.append(dict(parts=[(lobe, V_BODY)], c0=c0, dir=vd.outward(rng, c0, 0.2),
                           reach=rng.uniform(0.1, 0.2), ease=2.0, axis=ang + np.pi / 2,
                           tumble=rng.uniform(0.6, 1.0), spin=rng.uniform(-0.3, 0.3),
                           rise=0.0, shrink_at=0.2, least=0.15))
    return pieces


def death_tatters(i):
    scene, mats = creeper.pieces_scene(death_pieces(), lifted_mats(0.0, 0.3), fx.t_of(i, DEATH_FRAMES),
                                       lambda p: p)
    return artkit.native(*artkit.render_hi(scene, mats, DEATH_SIZE, DEATH_EXTENT))


def stump_scene():
    """The collapsed mound: the sac slumped flat and torn open, the lobes deflated, three plate
    stubs left of seven, the tendrils on the ground."""
    keep = (0, 3, 5)

    def scene(p):
        r = np.sqrt(p[:, 0] ** 2 + p[:, 1] ** 2)
        a = np.arctan2(p[:, 1], p[:, 0])
        af = (a + SEC / 2) % SEC - SEC / 2
        lp = np.stack([r * np.cos(af), r * np.sin(af), p[:, 2]], axis=-1)
        wob = 1 + 0.12 * np.sin(a * 3 + 1.0)                 # a lopsided slump
        lw = lp.copy()
        lw[:, 0] = 0.52 + (lp[:, 0] - 0.52) / wob
        d, m = union((sd_ellipsoid(p, (0, 0, -0.08), (0.62, 0.62, 0.18)), V_SAC),
                     (sd_ellipsoid(lw, (0.52, 0.0, -0.02), (0.27, 0.21, 0.13)) * 0.88, V_BODY), k=0.1)
        d, m = subtract((d, m), sd_cylinder_z(p, (0, 0, 0.1), 0.3, 0.2))   # torn open
        tl = lp.copy()
        tl[:, 1] = lp[:, 1] - 0.18 * np.clip(lp[:, 0] - 0.7, 0, None)
        d, m = union((d, m), (sd_capsule(tl, (0.7, 0.0, -0.05), (1.05, 0.0, -0.09), 0.07, 0.02), V_DARK), k=0.06)
        stubs = []
        for k in keep:
            q = _turn(p, k * SEC + SEC / 2)
            stubs.append((union((sd_ellipsoid(q, (0.62, 0.0, 0.06), (0.15, 0.11, 0.13)), 0),
                                (sd_capsule(q, (0.7, 0.0, 0.14), (0.8, 0.0, 0.2), 0.055, 0.035), 0), k=0.03)[0], V_BONE))
        return union((d, m), *stubs, k=0.03)
    return scene


def render_stump():
    mats = [replace(m, albedo=tuple(np.array(m.albedo) * STUMP_TONE)) for m in lifted_mats(0.0, 0.0)]
    img = artkit.native(*artkit.render_hi(stump_scene(), mats, SIZE, EXTENT))
    return creeper.light_rim(img, 0.5)


# =========================================================================== build

def _job(job):
    name, args = job
    return {"unit": render_unit, "creep": creep_frame, "death": death_glow, "tatters": death_tatters,
            "stump": render_stump}[name](*args)


def build():
    cycle = [(i, p) for i in range(IRIS_STATES) for p in range(len(PULSES))]   # iris * 4 + pulse
    with ProcessPoolExecutor() as pool:
        def run(name, arglist):
            return list(pool.map(_job, [(name, a) for a in arglist]))
        sets = [("hive-node", run("unit", [(i, p, "unit") for i, p in cycle]), COLOURS, False),
                ("hive-node-glow", run("unit", [(i, p, "glow") for i, p in cycle]), GLOW_COLOURS, False),
                ("hive-node-creep", run("creep", [(f,) for f in range(CREEP_FRAMES)]), CREEP_COLOURS, False),
                ("hive-node-death", run("death", [(i,) for i in range(DEATH_FRAMES)]), DEATH_COLOURS, False),
                ("hive-node-tatters", run("tatters", [(i,) for i in range(DEATH_FRAMES)]), DEATH_COLOURS, False),
                ("hive-node-stump", run("stump", [()]), STUMP_COLOURS, True)]
    for name, frames, colours, single in sets:
        artkit.write_frames(name, artkit.quantize_set(frames, colours), SOURCE, single=single)
        print(f"{name}: {len(frames)} frames, {frames[0].width}x{frames[0].height}")


# =========================================================================== review

def night_plate(w, h):
    """A crop of Level 08's avenues (the night city; Level 09 is set in the same last hour of night)."""
    src = Image.open(ROOT / "assets" / "backdrop" / "level-08" / "avenues.png").convert("RGBA")
    return src.crop((60, 200, 60 + w, 200 + h))


PERIOD = 4.0                                    # s between openings (stat block)
TELEGRAPH = 0.5                                 # s the iris takes to open (the telegraph)
HOLD = 0.5                                      # s it stays open after the spawn
CLOSE = 0.25                                    # s it takes to shut


def iris_at(t):
    """The iris's opening at t s into a cycle that spawns at TELEGRAPH (the proposed timing)."""
    u = t % PERIOD
    if u < TELEGRAPH:
        return u / TELEGRAPH
    if u < TELEGRAPH + HOLD:
        return 1.0
    if u < TELEGRAPH + HOLD + CLOSE:
        return 1 - (u - TELEGRAPH - HOLD) / CLOSE
    return 0.0


def frame_index(iris, t, pulse_fps=4):
    return int(round(iris * (IRIS_STATES - 1))) * len(PULSES) + int(t * pulse_fps) % len(PULSES)


def review():
    frames = artkit.load_frames("hive-node")
    glow = artkit.load_frames("hive-node-glow")
    creep = artkit.load_frames("hive-node-creep")
    tatters = artkit.load_frames("hive-node-tatters")
    burst = artkit.load_frames("hive-node-death")
    stump = artkit.load_frames("hive-node-stump")[0]
    boom = artkit.load_frames("explosion-large")
    skitter = artkit.load_frames("skitter")
    n_p = len(PULSES)
    iris_row = [frames[i * n_p] for i in range(IRIS_STATES)]
    pulse_row = frames[:n_p] + frames[(IRIS_STATES - 1) * n_p:]
    on_night = []
    for i in range(IRIS_STATES):
        cell = night_plate(96, 96)
        sprite.paste_center(cell, frames[i * n_p], 48, 48)
        on_night.append(artkit.add_light(cell, glow[i * n_p], (10, 10)))
    layers = [(tatters, DEATH_STEPS, False, 0), (boom, 2, True, 0), (burst, DEATH_STEPS, True, 0)]
    together = fx.composite_strip(layers, DEATH_FRAMES * DEATH_STEPS, DEATH_SIZE, 1)
    on_stump = []
    for k, frame in enumerate(together):
        cell = night_plate(*DEATH_SIZE)
        sprite.paste_center(cell, creep[min(len(creep) - 1, (2 * k) // CREEP_STEPS + 1)], 64, 64)
        sprite.paste_center(cell, stump, 64, 64)
        cell.alpha_composite(frame.convert("RGBA"))
        on_stump.append(cell)
    sheet = artkit.review_sheet("HIVE NODE - FINAL SPRITES (PROPOSAL)", [
        ("IRIS 0-5 (SHUT -> OPEN), PULSE FRAME 0", iris_row, 3, False),
        ("PULSE LOOP 0-3, IRIS SHUT AND OPEN", pulse_row, 2, False),
        ("GLOW MASKS, IRIS 0-5 (ADDITIVE: THROAT, POLYPS, VEINS, THE OPEN IRIS'S HALO)",
         [glow[i * n_p] for i in range(IRIS_STATES)], 3, True),
        ("IRIS 0-5 WITH THE GLOW ON THE NIGHT CITY", on_night, 2, False),
        (f"CREEP 0 ALIVE, 1-8 THE WITHER ({CREEP_STEPS} STEPS A FRAME = 2 S)", creep[::2], 1, False),
        ("STUMP (THE REMAINS)", [stump], 3, False),
        (f"TATTERS, {STEP // DEATH_STEPS} FPS, SOLID, EVERY 2ND FRAME", tatters[::2], 1, False),
        (f"DEATH GLOW, {STEP // DEATH_STEPS} FPS, ADDITIVE, EVERY 2ND FRAME", burst[::2], 1, True),
        ("WITH THE LARGE BURST OVER THE STUMP AND THE WITHERING CREEP, EVERY 4TH STEP", on_stump[::2], 1, False),
        ("1X", iris_row + [stump], 1, False)], width=1700, batch=BATCH)
    # the loop: the node on a night plaza in its creep, two openings 4 s apart, each releasing two
    # Skitters that arc out and away; then it is destroyed: burst, the creep withers, the stump stays
    fw, fh, fps = 240, 280, 20
    hx, hy = 120, 110
    plate = night_plate(fw, fh)
    die = 2 * PERIOD + 0.3
    n = int(fps * (die + 3.2))
    gif = []
    for f in range(n):
        t = f / fps
        step = int(t * STEP)
        cell = plate.copy()
        lights = []
        if t < die:
            sprite.paste_center(cell, creep[0], hx, hy)
            iris = iris_at(t)
            k = frame_index(iris, t)
            sprite.paste_center(cell, frames[k], hx, hy)
            lights.append((glow[k], (hx - SIZE[0] // 2, hy - SIZE[1] // 2)))
        else:
            age = step - int(die * STEP)
            sprite.paste_center(cell, creep[min(CREEP_FRAMES - 1, 1 + age // CREEP_STEPS)], hx, hy)
            sprite.paste_center(cell, stump, hx, hy)
            for frames_, steps, glowing, delay in layers:
                fr = fx.timed(frames_, steps, age - delay)
                if fr is None:
                    continue
                xy = (hx - fr.width // 2, hy - fr.height // 2)
                if glowing:
                    lights.append((fr, xy))
                else:
                    cell.alpha_composite(fr, xy)
        for c0 in (TELEGRAPH, PERIOD + TELEGRAPH):          # two Skitters per opening, fired out in an arc
            for side in (-1, 1):
                s = t - c0
                if not 0 <= s < 2.5:
                    continue
                x = hx + side * 55 * np.sin(min(s / 0.6, 1.0) * np.pi / 2) + side * 20 * max(0, s - 0.6)
                y = hy + 10 * s + 70 * max(0, s - 0.6) ** 1.4
                dx = side * (1 if s < 0.6 else 0.3)
                dy = 0.3 if s < 0.6 else 1.0
                hk = creeper.heading_of(dx, dy)
                fr = skitter[hk * 6 + (step // 6) % 6]
                sh = Image.fromarray(np.dstack([np.zeros((fr.height, fr.width, 3), np.uint8),
                                                ((np.array(fr)[..., 3] > 127) * 128).astype(np.uint8)]), "RGBA")
                sprite.paste_center(cell, sh, x + 21 * min(1, s / 0.4), y + 30 * min(1, s / 0.4))
                sprite.paste_center(cell, fr, x, y)
        for fr, xy in lights:
            cell = artkit.add_light(cell, fr, xy)
        gif.append(sprite.enlarge(cell, 2))
    artkit.save_review(sheet, gif, CONCEPT, "hive-node", fps=fps)


if __name__ == "__main__":
    artkit.REVIEW_ROUND = REVIEW_ROUND
    if "--review" not in sys.argv[1:]:
        build()
    review()
