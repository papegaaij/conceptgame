#!/usr/bin/env python3
"""Production art: the Wraith, Act 2's cloaked Vrell manta that loops behind the player
(design/enemies/air/wraith; M5 part D batch, concept round 32). Straight to production from its
chosen concept (user decision D10 = a): the round-06 model wraith-r06-a
(tools/concept/render/r06_models.wraith, imported unchanged).

Outputs (assets/sprites/):
  wraith_0..63          72x72, `16 angles`, the decloaked body on `air`: 16 headings x 4 wing-ripple
                        frames, indexed heading * 4 + frame (EnemyLooks.frame at 10 fps); heading k
                        flies k x 22.5 degrees clockwise from straight down (heading 0 down the
                        screen, heading 4 to the left, heading 8 up). The violet veins and eye glands
                        are emissive in the frame itself (no separate glow mask: an air unit's glow
                        is not drawn apart). A 1 px lavender-white light rim (creeper.light_rim) keeps
                        the dark rust membrane apart from Level 10's dawn sprawl.
  wraith-cloak_0..63    90x90 (1.25 x: the high-air scale, LevelRenderer.HIGH_AIR_SCALE, so the game
                        never scales it), additive: the cloaked shimmer, drawn INSTEAD of the body
                        while it is cloaked on `high-air`, in the body's order (heading * 4 + ripple
                        frame, 10 fps): a broken lavender glint running round the silhouette's edge,
                        faint refraction bands over the membrane and the veins and eyes as a faint
                        violet ghost. No shadow while cloaked.
  wraith-decloak_0..5   112x112, additive: the 0.4 s violet decloak flash (6 frames x 4 steps =
                        24 steps), centred on the unit, started when it decloaks; a white-violet core
                        from the eye glands, a violet bloom, an expanding ring and sparks
  wraith-death_0..9     96x96, additive: the `medium` death's violet flash, bloom and sparks with a
                        dark violet mist (4 steps per frame, with explosion-medium)
  wraith-tatters_0..9   96x96, solid: membrane tatters torn from both wings with their veins, the
                        bone spine in two, the whip tail in three, the veil tendrils and the body
                        halves, flung out, tumbling, shrivelling (4 steps per frame)
  design/enemies/air/wraith/concept/wraith-final-r32-a.png/.gif

How the game draws it (LevelRenderer / EnemyLooks, game side D6):
  cloaked (high-air)    wraith-cloak at the body's heading and ripple frame, additive, unscaled, at
                        full opacity; no body, no shadow; it is still hit by homing (a hit flashes
                        the shimmer white: draw it once more additively)
  decloak (0.4 s)       on `air` from the flash's start: wraith-decloak frame (age / 4); the body
                        frame drawn with opacity ramping 0 -> 1 over the 24 steps (batch colour
                        alpha), the shimmer fading 1 -> 0 over the first 12 steps; the shadow fades
                        in with the body
  decloaked             wraith_<heading * 4 + ripple> like any 16-angle unit, with its shadow
  death                 explosion-medium + wraith-death (additive) + wraith-tatters (solid, under
                        the glows), 4 steps per frame; the stat block's ×1.5 veins are struck, so
                        nothing marks them as a weak point

Run: python3 tools/art/wraith.py [--review]   (~2 min; --review only rebuilds the review files)
"""
import sys
from concurrent.futures import ProcessPoolExecutor
from dataclasses import replace

import numpy as np
from PIL import Image, ImageDraw

import artkit
from artkit import DESIGN, ROOT, TAU, sprite

import creeper  # noqa: E402  (tools/art: the light rim, the pieces' scene, the path helpers)
import vfx_r08 as v8  # noqa: E402  (concept script, imported unchanged)
import vrell_deaths as vd  # noqa: E402  (tools/art: the death pieces' pose and the motes)
import vrell_fx as fx  # noqa: E402  (tools/art: easing, torn outlines, timing)
from render import enemy_models as em  # noqa: E402
from render import r06_models as m6  # noqa: E402
from render.enemy_models import V_BODY, V_BONE, V_DARK, V_EYE, V_GLOW, V_SAC, V_SEAM  # noqa: E402
from render.enemy_rigs import model_space_materials  # noqa: E402
from render.sdf import Material, mirror_x, rotate_z, sd_capsule, sd_ellipsoid, sd_plate, union  # noqa: E402

# =========================================================================== parameters

SCRIPT = "wraith.py"
BATCH = "M5 part D batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
REVIEW_ROUND = "r32"
CONCEPT = DESIGN / "enemies" / "air" / "wraith" / "concept"
STEP = 60                                       # game steps per second

SIZE = (72, 72)                                 # the stat block's 72x72
EXTENT = 2.3                                    # model units across the sprite (the concept's)
HEADINGS = 16
RIPPLES = 4
FPS = 10                                        # EnemyLooks.ORGANIC_FPS
COLOURS = 40
RIM_STRENGTH = 0.8                              # the light rim at dawn (L08/L09's night rim was 1.0)

HIGH_AIR_SCALE = 1.25                           # LevelRenderer.HIGH_AIR_SCALE
CLOAK_SIZE = (90, 90)                           # 72 x 1.25: the same EXTENT draws the model 1.25 x
CLOAK_COLOURS = 16
GLINT = np.array([196, 178, 255], float)        # the edge glint: the concept's lavender (180, 160, 255), lifted
HAZE = np.array([150, 140, 220], float)         # the refraction bands
VEIN_GHOST = 0.28                               # the veins' and eyes' emission seen through the cloak

DECLOAK_SIZE = (112, 112)
DECLOAK_FRAMES = 6                              # 0.4 s = 24 steps at 4 steps a frame
DECLOAK_COLOURS = 24

DEATH_SIZE = (96, 96)
DEATH_FRAMES = 10
DEATH_STEPS = 4                                 # EnemyLooks.DEATH_FRAME_TICKS
DEATH_COLOURS = 32
DEATH_EXTENT = EXTENT * DEATH_SIZE[0] / SIZE[0]
VIOLET = np.array(em.hx(em.GLOWS["violet"])) * 255
VEIN = np.array((0.3, 0.32, 1.0)) * 255         # r06_models.wraith's blue-violet vein
WHITE_VIOLET = (240, 228, 255)


# =========================================================================== model

def rotation(k):
    return np.pi - TAU * k / HEADINGS                        # +Y forward -> heading k, as ravager.py


def wraith_mats(kind, phase):
    mats = m6.wraith(phase)[1]
    if kind == "glow":                                        # only the veins' and the eyes' emission
        black = Material((0.0, 0.0, 0.0), metal=0.0, shininess=1.0, spec=0.0)
        out = []
        for i, m in enumerate(mats):
            if i in (V_GLOW, V_EYE, V_SEAM):
                out.append(replace(m, albedo=(0.0, 0.0, 0.0), metal=0.0, spec=0.0, pattern=None))
            else:
                out.append(black)
        return out
    return mats


def model(k, j, kind="unit"):
    """(scene, mats) at heading k, ripple frame j."""
    rot = rotation(k)
    phase = TAU * j / RIPPLES
    base = m6.wraith(phase)[0]
    return (lambda p: base(rotate_z(p, rot))), model_space_materials(wraith_mats(kind, phase), rot)


def render_unit(k, j):
    img = artkit.native(*artkit.render_hi(*model(k, j), SIZE, EXTENT))
    return creeper.light_rim(img, RIM_STRENGTH)


# =========================================================================== cloak

def cloak(k, j):
    """The shimmer at heading k, ripple frame j: the silhouette at the high-air scale (1.25 x on a
    90 px canvas), its edge a broken glint that runs round it from frame to frame, faint refraction
    bands inside and the veins as a violet ghost; additive."""
    w, h = CLOAK_SIZE
    hi, factor = artkit.render_hi(*model(k, j, "glow"), CLOAK_SIZE, EXTENT, shadows=False)
    small = sprite.downsample(hi, factor)
    solid = small[..., 3] > 0.5
    inner = solid.copy()
    inner[1:, :] &= solid[:-1, :]
    inner[:-1, :] &= solid[1:, :]
    inner[:, 1:] &= solid[:, :-1]
    inner[:, :-1] &= solid[:, 1:]
    edge = solid & ~inner
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64)
    ang = np.arctan2(yy - h / 2, xx - w / 2)
    # three glint arcs running round the edge, a quarter turn per frame; a dim edge between them
    run = 0.5 + 0.5 * np.cos(3 * ang - TAU * j / RIPPLES - k * 0.7)
    glint = np.where(edge, 0.16 + 0.5 * run ** 3, 0.0)
    # refraction bands: thin diagonal ripples drifting over the membrane, stepped
    band = np.sin((xx + yy * 0.6) / 3.2 + TAU * j / RIPPLES) * np.sin((yy - xx * 0.4) / 5.0 - TAU * j / RIPPLES)
    bands = np.where(inner, np.where(band > 0.55, 0.11, np.where(band > 0.2, 0.05, 0.0)), 0.0)
    rgb = GLINT * glint[..., None] + HAZE * bands[..., None]
    rgb += np.clip(small[..., :3], 0, None) * 255 * VEIN_GHOST * solid[..., None]
    a = np.where(rgb.max(axis=-1) >= 1, 255, 0)
    return Image.fromarray(np.dstack([np.clip(rgb, 0, 255), a]).astype(np.uint8), "RGBA")


# =========================================================================== decloak flash

def decloak(i):
    """The decloak flash, frame i of 6: a white-violet core from the eye glands, a violet bloom over
    the body's span, an expanding ring and violet sparks; additive."""
    w, h = DECLOAK_SIZE
    t = i / (DECLOAK_FRAMES - 1)
    c = w / 2
    cv = v8.Canvas(w, h)
    d = cv.dist(c, c - 8)                                    # the eye glands sit forward of the centre
    dc = cv.dist(c, c)
    fade = np.clip((w / 2 - dc) / 5.0, 0, 1)
    peak = np.sin(np.pi * np.clip(0.25 + t * 0.9, 0, 1))      # brightest in frames 1-2
    cv.add(WHITE_VIOLET, v8.gauss(d, 3 + 7 * t) * 1.5 * peak)
    cv.add(VIOLET, v8.gauss(dc, 12 + 18 * fx.ease_out(t)) * 0.75 * (1 - t) ** 1.2 * fade)
    cv.add(VEIN, v8.gauss(dc, 22 + 14 * t) * 0.3 * (1 - t) * fade)
    ring = 10 + 40 * fx.ease_out(t, 2.0)
    tex = v8._cart_noise(w, 3211, cell=w * v8.SS // 7)
    cv.add(VIOLET, v8.gauss(np.abs(dc - ring), 1.4 + 2.0 * t) * (0.45 + 0.55 * tex) * 0.9 * (1 - t) ** 1.5 * fade)
    vd.motes(cv, np.random.default_rng(3212), c, t, 18, (16, 40), (0.5, 0.9), VIOLET, WHITE_VIOLET, fade,
             delay=0.1, gain=0.8)
    return artkit.additive(cv.image())


# =========================================================================== death

def death_glow(i):
    """The `medium` death: a white-violet flash from the body, a violet bloom, a shell of light
    breaking up, sparks, a dark violet mist thinning out."""
    w, h = DEATH_SIZE
    t = fx.t_of(i, DEATH_FRAMES)
    c = w / 2
    cv = v8.Canvas(w, h)
    d = cv.dist(c, c)
    fade = np.clip((w / 2 - d) / 4.0, 0, 1)
    tex = v8._cart_noise(w, 3221, cell=w * v8.SS // 5)
    churn = v8._shift(tex, t * w * 0.2, -t * w * 0.1)
    haze = np.clip(v8.gauss(d, 9 + 16 * fx.ease_out(t, 2.2)) * (0.5 + 0.9 * churn) - 0.2 - 0.6 * t, 0, 1)
    env = np.clip((i + 1) / 2.0, 0.4, 1) * (1 - t) ** 0.8
    cv.add((46, 26, 80), haze * 1.3 * env * fade)            # the mist's dark violet body
    cv.add(WHITE_VIOLET, v8.gauss(d, 3 + 6 * t) * 1.4 * np.clip(1 - t * 2.6, 0, 1))
    cv.add(VIOLET, v8.gauss(d, 8 + 12 * fx.ease_out(t)) * 0.6 * (1 - t) ** 1.5 * fade)
    ring = 6 + 22 * fx.ease_out(t, 2.2)
    cv.add(VEIN, v8.gauss(np.abs(d - ring), 1.3 + 1.6 * t) * (0.3 + 0.7 * tex) * 0.45 * (1 - t) ** 2 * fade)
    vd.motes(cv, np.random.default_rng(3222), c, t, 26, (12, 34), (0.5, 1.1), VIOLET, WHITE_VIOLET, fade)
    return artkit.additive(cv.image())


WING = [(0.0, 0.62), (0.32, 0.5), (0.86, 0.12), (1.08, -0.18), (0.82, -0.26),
        (0.4, -0.3), (0.12, -0.5), (0.0, -0.52)]           # r06_models.wraith's wing outline (half)


def death_pieces():
    """Membrane tatters from both wings (veined), the spine in two, the tail in three, the four veil
    tendrils, the body split fore and aft; each piece a list of (geometry, material slot)."""
    rng = np.random.default_rng(3231)
    pieces = []
    for side in (1, -1):                                      # membrane tatters along each wing
        for k in range(5):
            u = (k + 0.5) / 5
            cx = side * (0.3 + 0.65 * u)
            cy = 0.42 - 0.62 * u + rng.uniform(-0.06, 0.06)
            size = rng.uniform(0.28, 0.36) * (1.15 - 0.4 * u)
            poly = fx.jagged(rng, size, 7)
            bend = rng.uniform(0.8, 1.6)

            def tatter(q, cx=cx, cy=cy, poly=poly, bend=bend):
                r = q.copy()
                r[:, 0] -= cx
                r[:, 1] -= cy
                r[:, 2] -= 0.05 - bend * (r[:, 0] ** 2 + r[:, 1] ** 2)   # a curled scrap of membrane
                return sd_plate(r, poly, 0.0, 0.02, 0.008)
            c0 = (cx, cy, 0.05)
            pieces.append(dict(parts=[(tatter, V_SEAM)], c0=c0, dir=vd.outward(rng, c0, 0.4),
                               reach=rng.uniform(0.4, 0.75), ease=2.6, axis=rng.uniform(0, TAU),
                               tumble=rng.uniform(3, 6) * rng.choice([-1, 1]), spin=rng.uniform(-3, 3),
                               rise=0.3, shrink_at=0.6))
    for y0, y1, d in ((0.4, 0.0, 1.0), (0.0, -0.4, -1.0)):   # the bone spine snapped in two
        def spine(q, y0=y0, y1=y1):
            return sd_capsule(q, (0, y0, 0.22), (0, y1, 0.22), 0.04, 0.025)
        pieces.append(dict(parts=[(spine, V_BONE)], c0=(0.0, (y0 + y1) / 2, 0.22),
                           dir=(float(rng.uniform(-0.4, 0.4)), d), reach=rng.uniform(0.2, 0.35), ease=3.0,
                           axis=rng.uniform(0, TAU), tumble=rng.uniform(3, 5) * rng.choice([-1, 1]),
                           spin=rng.uniform(-3, 3), rise=0.3, shrink_at=0.65))
    for a, b in ((0, 2), (2, 4), (4, 5)):                    # the whip tail in three
        def tail(q, a=a, b=b):
            return union(*[(sd_capsule(q, (0, -0.48 - 0.13 * i, 0.08), (0, -0.61 - 0.13 * i, 0.06), 0.035, 0.025), 0)
                           for i in range(a, b)])[0]
        cy = -0.48 - 0.13 * (a + b) / 2
        pieces.append(dict(parts=[(tail, V_DARK)], c0=(0.0, cy, 0.07), dir=(float(rng.uniform(-0.6, 0.6)), -1.0),
                           reach=rng.uniform(0.2, 0.4), ease=2.6, axis=rng.uniform(0, TAU),
                           tumble=rng.uniform(2, 5) * rng.choice([-1, 1]), spin=rng.uniform(-3, 3),
                           rise=0.2, shrink_at=0.6))
    for side in (1, -1):                                      # the veil tendrils, torn loose
        for xo in (0.34, 0.62):
            def veil(q, x=side * xo):
                return sd_capsule(q, (x, -0.25, 0.04), (x, -0.81, 0.04), 0.04, 0.022)
            c0 = (side * xo, -0.53, 0.04)
            pieces.append(dict(parts=[(veil, V_SAC)], c0=c0, dir=vd.outward(rng, c0, 0.3),
                               reach=rng.uniform(0.3, 0.55), ease=2.6, axis=rng.uniform(0, TAU),
                               tumble=rng.uniform(3, 6) * rng.choice([-1, 1]), spin=rng.uniform(-3, 3),
                               rise=0.25, shrink_at=0.6))
    for cy, half, d in ((0.3, (0.2, 0.32, 0.13), 1.0), (-0.2, (0.18, 0.3, 0.12), -1.0)):   # the body split

        def chunk(q, cy=cy, half=half):
            return sd_ellipsoid(q, (0.0, cy, 0.1), half)
        pieces.append(dict(parts=[(chunk, V_BODY)], c0=(0.0, cy, 0.1), dir=(float(rng.uniform(-0.3, 0.3)), d),
                           reach=0.16, ease=3.0, axis=rng.uniform(0, TAU), tumble=1.2 * rng.choice([-1, 1]),
                           spin=rng.uniform(-1, 1), rise=0.1, shrink_at=0.2, least=0.1))
    pieces.append(dict(parts=[(lambda q: sd_ellipsoid(mirror_x(q), (0.1, 0.66, 0.06), (0.06, 0.12, 0.05)), V_DARK)],
                       c0=(0.0, 0.66, 0.06), dir=(0.0, 1.0), reach=0.3, ease=3.0, axis=rng.uniform(0, TAU),
                       tumble=3.0, spin=2.0, rise=0.2, shrink_at=0.6))   # the cephalic lobes
    return pieces


def death_tatters(i):
    # built facing +Y; heading 0 (down the screen) is a half turn
    mats = m6.wraith(0.0, glow=0.45)[1]
    scene, mats = creeper.pieces_scene(death_pieces(), mats, fx.t_of(i, DEATH_FRAMES), lambda p: rotate_z(p, np.pi))
    return artkit.native(*artkit.render_hi(scene, mats, DEATH_SIZE, DEATH_EXTENT))


# =========================================================================== build

def _job(job):
    name, args = job
    return {"unit": render_unit, "cloak": cloak, "decloak": decloak, "death": death_glow,
            "tatters": death_tatters}[name](*args)


def build():
    cells = [(k, j) for k in range(HEADINGS) for j in range(RIPPLES)]   # heading * 4 + frame
    with ProcessPoolExecutor() as pool:
        def run(name, arglist):
            return list(pool.map(_job, [(name, a) for a in arglist]))
        sets = [("wraith", run("unit", cells), COLOURS),
                ("wraith-cloak", run("cloak", cells), CLOAK_COLOURS),
                ("wraith-decloak", run("decloak", [(i,) for i in range(DECLOAK_FRAMES)]), DECLOAK_COLOURS),
                ("wraith-death", run("death", [(i,) for i in range(DEATH_FRAMES)]), DEATH_COLOURS),
                ("wraith-tatters", run("tatters", [(i,) for i in range(DEATH_FRAMES)]), DEATH_COLOURS)]
    for name, frames, colours in sets:
        frames = artkit.quantize_set(frames, colours)
        artkit.write_frames(name, frames, SOURCE)
        print(f"{name}: {len(frames)} frames, {frames[0].width}x{frames[0].height}")


# =========================================================================== review

def dawn_plate(w, h, x0=120, y0=300):
    """A stand-in for Level 10's sprawl at first light (its backdrop is not built yet): a crop of
    Level 08's avenues, lifted out of the night and graded toward a cool dawn with a warm cast."""
    src = Image.open(ROOT / "assets" / "backdrop" / "level-08" / "avenues.png").convert("RGB")
    a = np.array(src.crop((x0, y0, x0 + w, y0 + h))).astype(np.float64)
    lum = a.mean(axis=-1, keepdims=True)
    a = a * 1.35 + 34
    a = a * 0.72 + np.array([128, 122, 142]) * 0.28          # the cool morning haze
    a += np.clip(lum - 90, 0, None) * np.array([0.35, 0.18, 0.0])   # warm first light on the bright parts
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGB").convert("RGBA")


def shadow(frame, opacity=0.5):
    """The game's flyer shadow: the frame's silhouette at 85 %, flat dark, opacity scaled."""
    a = np.array(frame)[..., 3] > 127
    img = Image.fromarray(np.dstack([np.zeros(a.shape + (3,), np.uint8), (a * int(255 * opacity)).astype(np.uint8)]),
                          "RGBA")
    return img.resize((max(1, round(img.width * 0.85)), max(1, round(img.height * 0.85))), Image.NEAREST)


def with_alpha(frame, alpha):
    a = np.array(frame).copy()
    a[..., 3] = (a[..., 3] * alpha).astype(np.uint8)
    return Image.fromarray(a, "RGBA")


def draw_body(cell, frame, x, y, alpha=1.0, shadow_on=True):
    if shadow_on and alpha > 0:
        sh = shadow(frame, 0.5 * alpha)
        cell.alpha_composite(sh, (int(round(x + 21 - sh.width / 2)), int(round(y + 30 - sh.height / 2))))
    f = frame if alpha >= 1 else with_alpha(frame, alpha)
    cell.alpha_composite(f, (int(round(x - f.width / 2)), int(round(y - f.height / 2))))
    return cell


def add_centred(cell, glow, x, y, gain=1.0):
    if gain < 1:
        a = np.array(glow).astype(np.float64)
        a[..., :3] *= gain
        glow = Image.fromarray(a.astype(np.uint8), "RGBA")
    return artkit.add_light(cell, glow, (int(round(x - glow.width / 2)), int(round(y - glow.height / 2))))


def edge_warning(cell, x, t):
    """The 3 s rear edge warning as the review shows it: a blinking chevron at the bottom edge."""
    if int(t * 6) % 2:
        return
    d = ImageDraw.Draw(cell)
    hgt = cell.height
    d.polygon([(x - 12, hgt - 6), (x + 12, hgt - 6), (x, hgt - 22)], outline=(255, 80, 60, 255), fill=(110, 16, 12, 220))


def review():
    frames = artkit.load_frames("wraith")
    cl = artkit.load_frames("wraith-cloak")
    flash = artkit.load_frames("wraith-decloak")
    tatters = artkit.load_frames("wraith-tatters")
    burst = artkit.load_frames("wraith-death")
    boom = artkit.load_frames("explosion-medium")
    orb = artkit.load_frames("orb-medium")
    heading = [frames[k * RIPPLES:(k + 1) * RIPPLES] for k in range(HEADINGS)]
    cloaks = [cl[k * RIPPLES:(k + 1) * RIPPLES] for k in range(HEADINGS)]

    def on_plate(img, glow=False, size=(112, 112)):
        cell = dawn_plate(*size)
        x, y = size[0] / 2, size[1] / 2
        return add_centred(cell, img, x, y) if glow else draw_body(cell, img, x, y, shadow_on=False)

    # the decloak as drawn: shimmer fading over the first half, the flash, the body fading in
    sequence = []
    for age in range(0, DECLOAK_FRAMES * 4 + 8, 4):
        cell = dawn_plate(*DECLOAK_SIZE)
        c = DECLOAK_SIZE[0] / 2
        u = min(age / 24, 1.0)
        if age < 12:
            cell = add_centred(cell, cloaks[8][0], c, c, 1 - age / 12)
        cell = draw_body(cell, heading[8][0], c, c, u, shadow_on=False)
        if age < 24:
            cell = add_centred(cell, flash[age // 4], c, c)
        sequence.append(cell)
    layers = [(tatters, DEATH_STEPS, False, 0), (boom, 2, True, 0), (burst, DEATH_STEPS, True, 0)]
    together = fx.composite_strip(layers, DEATH_FRAMES * DEATH_STEPS, DEATH_SIZE, 1)
    sheet = artkit.review_sheet("WRAITH - FINAL SPRITES (PROPOSAL)", [
        ("HEADINGS 0-7 (CLOCKWISE FROM DOWN: 4 FLIES LEFT), RIPPLE FRAME 0", [h[0] for h in heading[:8]], 2, False),
        ("HEADINGS 8-15 (8 FLIES UP, 12 RIGHT)", [h[0] for h in heading[8:]], 2, False),
        (f"WING RIPPLE, HEADING 0 AND HEADING 8, {FPS} FPS", heading[0] + heading[8], 2, False),
        ("CLOAKED SHIMMER (ADDITIVE, 1.25X FOR HIGH-AIR), HEADING 0", cloaks[0], 2, True),
        ("CLOAKED SHIMMER OVER THE DAWN STAND-IN, HEADINGS 0 / 4 / 8 / 12", [on_plate(cloaks[k][0], True, (96, 96))
                                                                          for k in (0, 4, 8, 12)], 2, False),
        ("DECLOAK FLASH, 6 FRAMES X 4 STEPS = 0.4 S (ADDITIVE)", flash, 2, True),
        ("DECLOAK AS DRAWN, EVERY 4 STEPS: SHIMMER FADES, FLASH, BODY 0 -> 100 %", sequence, 2, False),
        (f"TATTERS, {STEP // DEATH_STEPS} FPS, SOLID", tatters, 2, False),
        (f"DEATH GLOW, {STEP // DEATH_STEPS} FPS, ADDITIVE", burst, 2, True),
        ("TOGETHER WITH EXPLOSION-MEDIUM, EVERY 2ND STEP; PIECES UNDER THE GLOWS", together[::2], 1, False),
        ("1X ON THE DAWN STAND-IN", [on_plate(h[0], size=(80, 80)) for h in heading[::2]], 1, False),
        ("1X", [h[0] for h in heading], 1, False)], width=1500, batch=BATCH)
    artkit.save_review(sheet, review_loop(heading, cloaks, flash, tatters, burst, boom, orb), CONCEPT, "wraith", fps=20)


def review_loop(heading, cloaks, flash, tatters, burst, boom, orb):
    """The Wraith's pass over a 240x300 strip of the dawn stand-in: it swoops down cloaked (the
    shimmer, 200 px/s), leaves the bottom edge, the 3 s edge warning, it rises back in from the
    bottom cloaked, decloaks (0.4 s flash), holds 2.5 s still facing up (standing still keeps its
    facing) and fires two 5-shot bursts straight up the screen as a fixed 40 degree fan (aim: up,
    aimed at nobody; the shots in turn from the fan's left edge to its right, 0.12 s apart,
    220 px/s), exits up a side lane decloaked (120 px/s) and is shot down."""
    fw, fh, fps = 240, 300, 20
    plate = dawn_plate(fw, 720, 150, 120)
    ship = artkit.load_frames("ship")[2]
    ship_xy = (120, 150)
    swoop = creeper.smooth_path([(150, -60), (150, 60), (128, 170), (110, 260), (100, 380)])
    rise = creeper.smooth_path([(170, 360), (168, 300), (165, 262)])
    exit_ = creeper.smooth_path([(165, 262), (200, 200), (212, 100), (214, -80)])
    t_swoop = swoop[1][-1] / 200
    t_warn = 3.0
    t_rise = rise[1][-1] / 120
    t0_rise = t_swoop + 0.4
    t_decloak = t0_rise + t_rise
    t_hold = t_decloak + 0.4
    t_exit = t_hold + 2.5
    t_die = t_exit + 1.3
    total = t_die + 0.9
    shots = []                                               # (fired at, angle off straight up)
    for burst_t in (t_hold + 0.5, t_hold + 1.7):
        for s in range(5):                                   # left edge (-20 deg) to right edge (+20 deg)
            shots.append((burst_t + 0.12 * s, np.radians(-20 + 40 * s / 4)))
    gif = []
    n = int(total * fps)
    for f in range(n):
        t = f / fps
        scroll = int(t * 190 * 0.5) % (720 - fh)            # the review scrolls at half speed
        cell = plate.crop((0, 720 - fh - scroll, fw, 720 - scroll))
        cell = draw_body(cell, ship, *ship_xy)
        ripple = int(t * FPS) % RIPPLES
        if t < t_swoop:
            pos, dv = creeper.along(swoop, t * 200)
            k = creeper.heading_of(*dv)
            cell = add_centred(cell, cloaks[k][ripple], *pos)
        elif t < t_decloak:
            if t > t_decloak - t_warn:
                edge_warning(cell, 170, t)
            if t >= t0_rise:
                pos, _ = creeper.along(rise, (t - t0_rise) * 120)
                cell = add_centred(cell, cloaks[8][ripple], *pos)
        elif t < t_exit:
            pos, _ = creeper.along(rise, rise[1][-1])
            age = int((t - t_decloak) * STEP)
            k = 8                                            # it holds facing up, the way it rose
            if age < 24:
                if age < 12:
                    cell = add_centred(cell, cloaks[k][ripple], *pos, 1 - age / 12)
                cell = draw_body(cell, heading[k][ripple], *pos, age / 24)
                cell = add_centred(cell, flash[age // 4], *pos)
            else:
                cell = draw_body(cell, heading[k][ripple], *pos)
        elif t < t_die:
            pos, dv = creeper.along(exit_, (t - t_exit) * 120)
            k = creeper.heading_of(*dv)
            cell = draw_body(cell, heading[k][ripple], *pos)
            last = pos
        else:
            age = int((t - t_die) * STEP)
            x, y = last
            for frames_, steps, glowing in ((tatters, DEATH_STEPS, False), (boom, 2, True), (burst, DEATH_STEPS, True)):
                fr = fx.timed(frames_, steps, age)
                if fr is None:
                    continue
                if glowing:
                    cell = add_centred(cell, fr, x, y)
                else:
                    cell.alpha_composite(fr, (int(round(x - fr.width / 2)), int(round(y - fr.height / 2))))
        hold = creeper.along(rise, rise[1][-1])[0]
        for s0, a in shots:                                  # the bursts: medium orbs fanned straight up
            if t >= s0:
                u = (t - s0) * 220
                x, y = hold[0] + np.sin(a) * u, hold[1] - np.cos(a) * u
                if -10 < y < fh + 10:
                    cell = add_centred(cell, orb[int(t * 15) % len(orb)], x, y)
        gif.append(sprite.enlarge(cell, 2))
    return gif


if __name__ == "__main__":
    artkit.REVIEW_ROUND = REVIEW_ROUND
    if "--review" not in sys.argv[1:]:
        build()
    review()
