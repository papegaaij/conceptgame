#!/usr/bin/env python3
"""Production art: the Driftjelly, Level 11's jellyfish mine that drifts on or below the surface
(design/enemies/naval/driftjelly; M5 part E batch, concept round 33). Straight to production from
its chosen concept (user decision E9 = a): the round-07 waterline look (tools/concept/enemies_r07.py)
on the round-06 model (tools/concept/render/r06_models.driftjelly, cut at the water plane by
render/r07_models.clipped), both imported unchanged.

Water (user decision E4 = c, the hybrid; design/art-direction, water rules): everything under the
surface is drawn by the game's generic `sub` pass (tint toward the water, darker, blurred, wavy);
what breaks the surface, its foam collar, its ripples and the surfacing steps are pre-rendered here.
So a jelly is always drawn in two layers: its plain body through the `sub` pass, and over it, on the
surface, the part above the waterline with its collar (nothing while submerged).

Outputs (assets/sprites/; 40x40 is the stat block's size, 17.4 px per model unit as the concept):
  driftjelly-sub_0..3       40x40, the whole plain body (bell, frills, eight tentacles) at the four
                            pulse frames (rest, contracting, contracted, relaxing): drawn through the
                            `sub` pass, submerged AND surfaced (it is the under-water part then)
  driftjelly_0..3           40x40, surfaced: the dome above the waterline (the model cut at z = 0)
                            with its broken foam collar, at the same four pulse frames (the collar at
                            four phases, so the four frames loop); drawn on the surface over the
                            matching driftjelly-sub frame
  driftjelly-pulse_0..3     40x40, additive: the lime veins' bloom at the four pulse frames, drawn over
                            the jelly after the `sub` pass (so the quickening pulse shows through the
                            water, the in-game warning; README, The ring)
  driftjelly-surface_0..5   40x40, the swap's surface layer, 6 frames x 6 steps = 0.6 s: a swell
                            rising over the bell, the crown breaking first, the dome coming up through
                            a growing collar, water streaming off; played forward to surface and
                            backward to dive, over driftjelly-sub_0 (the swap is drawn at rest pulse)
  driftjelly-ripple_0..11   104x104, the ripple train of one bell contraction: three uneven rings
                            (light crest, dark trough, broken up) 0.28 s apart, expanding at 22 px/s
                            and fading; 8 steps a frame (1.6 s), stepped translucency; started at the
                            contraction (pulse frame 2) where the jelly is and left there on the sea
                            (scrolling with it) as the jelly drifts on; surfaced jellies only
  driftjelly-death_0..9     64x64, additive: the `small` wet pop: a white-lime flash, lime droplets
                            flung out, a lime haze (4 steps a frame, with explosion-small)
  driftjelly-tatters_0..9   64x64, solid: the bell torn in shreds, tentacle pieces and the glow knob,
                            flung out, then darkening toward the water as they sink (4 steps a frame;
                            the generic on-water splash and ripple train come from the shared water
                            effects, tools/art/water_fx.py)
  design/enemies/naval/driftjelly/concept/driftjelly-final-r33-a.png/.gif

How the game draws it (game side, step E3c):
  submerged   driftjelly-sub_<pulse> in the `sub` pass; driftjelly-pulse_<pulse> added over it
  surfaced    driftjelly-sub_<pulse> in the `sub` pass; driftjelly_<pulse> over it on the surface
              layer; a driftjelly-ripple at each contraction (pulse frame 2)
  swap        driftjelly-sub_0 in the `sub` pass; driftjelly-surface_<step / 6> over it (surfacing
              0 -> 5, diving 5 -> 0); the layer flips at the middle (frame 3)
  death       explosion-small + driftjelly-death (additive) + driftjelly-tatters (solid), 4 steps a frame
The `sub` pass stand-in this script uses for its review (``sub_pass``) is the look the game's pass
aims at: rgb x (1 - k) x 0.85 + WATER_TINT x k with k = 0.45, alpha x 0.85, a 0.6 px blur and a
row-wise sway of 0.6 px.

Run: python3 tools/art/driftjelly.py [--review]   (~1 min; --review only rebuilds the review files)
"""
import sys
from concurrent.futures import ProcessPoolExecutor

import numpy as np
from PIL import Image, ImageFilter

import artkit
from artkit import DESIGN, TAU, sprite

import vfx_r08 as v8  # noqa: E402  (concept script, imported unchanged)
import vrell_deaths as vd  # noqa: E402  (tools/art: the death pieces' pose and the motes)
import vrell_fx as fx  # noqa: E402  (tools/art: easing, timing)
import creeper  # noqa: E402  (tools/art: pieces of several materials)
from render import enemy_models as em  # noqa: E402
from render import r06_models as m6  # noqa: E402
from render import r07_models as m7  # noqa: E402
from render.enemy_models import V_BONE, V_DARK, V_GLOW, V_SAC  # noqa: E402
from render.sdf import Material, sd_capsule, sd_plate, sd_sphere  # noqa: E402

# =========================================================================== parameters

SCRIPT = "driftjelly.py"
BATCH = "M5 part E batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
REVIEW_ROUND = "r33"
UNIT = DESIGN / "enemies" / "naval" / "driftjelly"
CONCEPT = UNIT / "concept"
SLUG = "driftjelly"
STEP = 60

SIZE = (40, 40)
EXTENT = 2.3                                    # the concept's (17.4 px per model unit)
PX = SIZE[0] / EXTENT
PULSES = [0.0, 0.5, 1.0, 0.5]                   # rest, contracting, contracted, relaxing
COLOURS = 32
SURFACE_FRAMES = 6
SURFACE_LEVELS = [0.78, 0.5, 0.34, 0.2, 0.08, 0.0]   # the water plane per swap frame (model z)
RIPPLE_SIZE, RIPPLE_FRAMES, RIPPLE_STEPS = (104, 104), 12, 8
RIPPLE_SPEED, RIPPLE_R0, TRAIN = 22.0, 11.0, (0.0, 0.28, 0.56)
DEATH_SIZE, DEATH_FRAMES = (64, 64), 10
DEATH_EXTENT = EXTENT * DEATH_SIZE[0] / SIZE[0]

LIME = np.array(em.hx(em.GLOWS["lime"])) * 255
WHITE_LIME = (236, 255, 210)

# --------------------------------------------------------------------------- water look (shared)

WATER_TINT = np.array([6, 40, 78], float)       # the `sub` pass stand-in's water colour
FOAM = np.array([226, 242, 248], float)
FOAM_SHADE = np.array([150, 196, 222], float)
CREST = np.array([170, 215, 232], float)
TROUGH = np.array([0, 18, 40], float)


def sub_pass(img, t=0.0, k=0.45, blur=0.6, sway=0.6):
    """The stand-in for the game's generic `sub` pass (E4 = c), for the review sheets only: tinted
    toward the water, darker, a little softer and fainter, swaying row by row."""
    a = np.array(img.convert("RGBA")).astype(np.float64)
    a[..., :3] = a[..., :3] * (1 - k) * 0.85 + WATER_TINT * k
    a[..., 3] *= 0.85
    out = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA").filter(ImageFilter.GaussianBlur(blur))
    return wobble(out, t, sway)


def wobble(img, t, amp=0.6, freq=0.5, speed=5.0):
    """Row-wise sub-pixel sway: the refraction of a gently moving surface."""
    a = np.array(img).astype(np.float64)
    h, w = a.shape[:2]
    shift = amp * np.sin(freq * np.arange(h) + speed * t)
    xs = np.arange(w)[None, :] - shift[:, None]
    x0 = np.floor(xs).astype(int)
    fr = (xs - x0)[..., None]

    def take(x):
        v = np.take_along_axis(a, np.clip(x, 0, w - 1)[..., None].repeat(4, -1), axis=1)
        v[(x < 0) | (x >= w)] = 0
        return v
    out = take(x0) * (1 - fr) + take(x0 + 1) * fr
    return Image.fromarray(np.clip(out, 0, 255).astype(np.uint8), "RGBA")


def stepped(rgb, alpha, levels=4):
    """Float rgb (0..255) and coverage (0..1) -> RGBA with the translucency stepped to a few levels
    (the 90s translucency tables; production rule for foam and water marks)."""
    steps = levels - 1
    a = np.floor(np.clip(alpha, 0, 1) * steps + 0.5) / steps
    out = np.dstack([np.clip(rgb, 0, 255), a * 255])
    out[a <= 0] = 0
    return Image.fromarray(out.astype(np.uint8), "RGBA")


class Field:
    """A 4x supersampled coverage/colour field in native pixel coordinates (foam, ripples, swells),
    composited back to native size with stepped translucency."""
    SS = 4

    def __init__(self, w, h):
        self.w, self.h = w, h
        ys, xs = np.mgrid[0:h * self.SS, 0:w * self.SS]
        self.x = (xs + 0.5) / self.SS
        self.y = (ys + 0.5) / self.SS
        self.rgb = np.zeros(self.x.shape + (3,))
        self.a = np.zeros(self.x.shape)

    def over(self, colour, cover):
        cover = np.clip(cover, 0, 1)
        self.rgb = self.rgb * (1 - cover[..., None]) + np.asarray(colour, float) * cover[..., None]
        self.a = self.a * (1 - cover) + cover

    def image(self, levels=4):
        s = self.SS
        a = self.a.reshape(self.h, s, self.w, s).mean(axis=(1, 3))
        prem = (self.rgb * self.a[..., None]).reshape(self.h, s, self.w, s, 3).mean(axis=(1, 3))
        rgb = prem / np.maximum(a, 1e-6)[..., None]
        return stepped(rgb, a, levels)


def ring_noise(theta, w, seed):
    """Smooth irregular 0..1 noise round a ring, periodic in the loop phase ``w`` (radians)."""
    v = (np.sin(7 * theta + w + seed) * np.sin(3 * theta - 2 * w + seed * 2.1)
         + 0.5 * np.sin(13 * theta + 3 * w + seed * 0.7))
    return np.clip(0.5 + 0.45 * v, 0, 1)


def foam_ring(field, cx, cy, radius, w, seed, width=1.6, specks=12, reach=8.0, gain=1.0, aspect=1.0):
    """A broken, wobbling foam collar round (cx, cy) at ``radius``, shedding specks that drift off;
    periodic in the loop phase ``w``. Never closed: the noise opens gaps."""
    th = np.linspace(0, TAU, max(48, int(radius * 7)), endpoint=False)
    v = ring_noise(th, w, seed)
    for a, val in zip(th, v):
        if val < 0.36:
            continue                                          # the gaps
        rr = radius - 0.3 + width * val + 0.6 * np.sin(a * 11 + w * 2 + seed)
        size = (0.45 + 0.75 * val) * width * 0.7
        x, y = cx + np.cos(a) * rr, cy + np.sin(a) * rr * aspect
        d = np.hypot(field.x - x, field.y - y)
        cover = (d <= size) * (0.35 + 0.65 * val) * gain
        field.over(FOAM * (0.75 + 0.25 * val) + FOAM_SHADE * (0.25 - 0.25 * val), cover)
    rng = np.random.default_rng(seed)
    for _ in range(specks):                                   # specks drifting off the collar
        ang, ph = rng.uniform(0, TAU), rng.uniform(0, 1)
        age = (w / TAU + ph) % 1.0
        rr = radius + 1.5 + age * reach
        a = ang + 0.3 * age
        x, y = cx + np.cos(a) * rr, cy + np.sin(a) * rr * aspect
        d = np.hypot(field.x - x, field.y - y)
        field.over(FOAM, (d <= 0.55) * (1 - age) * 0.9 * gain)


def edge_foam(field, mask, w, seed, width=1.6, gain=1.0, specks=12, reach=6.0, centre=None, inner=0.0):
    """A broken foam collar along the edge of an irregular waterline: ``mask`` (native, 0/1) is
    what is above the water; the foam lies on the water just outside it (``inner`` px also over its
    rim), gapped by noise round ``centre`` and broken into clumps, with specks drifting outward;
    periodic in the loop phase ``w``."""
    S = field.SS
    m = Image.fromarray((np.clip(mask, 0, 1) * 255).astype(np.uint8)).resize((field.w * S, field.h * S),
                                                                              Image.BILINEAR)
    hard = np.asarray(m, float) / 255 > 0.5
    soft = np.asarray(m.filter(ImageFilter.GaussianBlur(width * S)), float) / 255
    if centre is None:
        ys, xs = np.nonzero(mask > 0.5)
        centre = (xs.mean() + 0.5, ys.mean() + 0.5) if len(xs) else (field.w / 2, field.h / 2)
    cx, cy = centre
    th = np.arctan2(field.y - cy, field.x - cx)
    n = ring_noise(th, w, seed)
    band = np.clip((soft - 0.03) / 0.14, 0, 1) * (~hard)
    if inner > 0:
        shrink = np.asarray(m.filter(ImageFilter.GaussianBlur(inner * S)), float) / 255
        band = np.maximum(band, hard * np.clip((0.8 - shrink) / 0.25, 0, 1))
    clump = (np.sin(field.x * 1.3 + 2.5 * np.sin(field.y * 0.6 + w + seed))
             * np.sin(field.y * 1.1 + 2.0 * np.sin(field.x * 0.5 - w + seed * 0.3))
             + 0.6 * np.sin(field.x * 3.1 - field.y * 2.3 + 2 * w + seed * 1.7))
    cover = band * (n > 0.3) * (clump > -0.1) * (0.55 + 0.45 * n)
    shade = np.clip(1 - soft, 0, 1)[..., None]
    field.over(FOAM * (1 - 0.35 * shade) + FOAM_SHADE * 0.35 * shade, cover * gain)
    rng = np.random.default_rng(seed)
    ey, ex = np.nonzero((mask > 0.5) & ~(np.roll(mask > 0.5, 1, 0) & np.roll(mask > 0.5, -1, 0)
                                          & np.roll(mask > 0.5, 1, 1) & np.roll(mask > 0.5, -1, 1)))
    if len(ex) and specks:
        for k in rng.choice(len(ex), size=min(specks, len(ex)), replace=False):
            x0, y0 = ex[k] + 0.5, ey[k] + 0.5
            ang = np.arctan2(y0 - cy, x0 - cx)
            age = (w / TAU + rng.uniform(0, 1)) % 1.0
            x, y = x0 + np.cos(ang) * (1.5 + age * reach), y0 + np.sin(ang) * (1.5 + age * reach)
            d = np.hypot(field.x - x, field.y - y)
            field.over(FOAM, (d <= 0.6) * (1 - age) * 0.9 * gain)


def ripple_ring(field, cx, cy, r, alpha, seed, aspect=1.0):
    """One uneven ripple: a light crest with a darker trough just outside, wobbling in radius and
    broken up along its length."""
    if alpha <= 0.03:
        return
    dx, dy = field.x - cx, (field.y - cy) / aspect
    rad = np.hypot(dx, dy)
    th = np.arctan2(dy, dx)
    wob = 1.4 * np.sin(3 * th + seed) + 0.9 * np.sin(5 * th + seed * 1.7) + 0.5 * np.sin(9 * th)
    brk = np.clip(0.45 + 0.55 * np.sin(4 * th + seed * 2.3) * np.sin(7 * th - seed), 0, 1)
    rr = rad - wob
    field.over(TROUGH, np.clip(1.4 - np.abs(rr - r - 2.4) / 1.0, 0, 1) * alpha * 0.75 * brk)
    field.over(CREST, np.clip(1.4 - np.abs(rr - r) / 0.9, 0, 1) * alpha * 0.95 * brk)


def swell(field, cx, cy, radius, height, aspect=1.0, crest=0.75, trough=0.6):
    """A mound of displaced water lit by the top-left key light: a pale flank toward the light, a
    dark one away from it, translucent."""
    dx, dy = (field.x - cx) / radius, (field.y - cy) / (radius * aspect)
    h = np.exp(-(dx * dx + dy * dy))
    gx, gy = -2 * dx * h, -2 * dy * h                         # the slope (screen y down)
    lit = np.clip(-(gx * -0.7 + gy * -0.7), -1, 1) * height   # facing up-left is lit
    field.over(CREST, np.clip(lit, 0, 1) * crest)
    field.over(TROUGH, np.clip(-lit, 0, 1) * trough)


def ocean_plate(w, h, seed=121, x0=0, y0=0):
    """The review's sea: the concept's open-ocean ground (enemies_r06.ocean_bg, a stand-in until
    Level 11's backdrop exists), cropped."""
    import enemies_r06 as e6  # concept script, imported unchanged
    bg = e6.ocean_bg(seed).convert("RGBA")
    if w > bg.width or h > bg.height:
        big = Image.new("RGBA", (max(w, bg.width), max(h, bg.height)))
        for yy in range(0, big.height, bg.height):
            for xx in range(0, big.width, bg.width):
                big.paste(bg, (xx, yy))
        bg = big
    return bg.crop((x0, y0, x0 + w, y0 + h))


# =========================================================================== model renders

def render_body(pulse):
    scene, mats = m6.driftjelly(pulse)
    return artkit.native(*artkit.render_hi(scene, mats, SIZE, EXTENT))


def render_above(pulse, level):
    scene, mats = m7.clipped(m6.driftjelly, level, "above")(pulse=pulse)
    return artkit.native(*artkit.render_hi(scene, mats, SIZE, EXTENT))


def glow_mats(pulse):
    """Only the lime emission (the bell's veins and the knob), on black."""
    mats = m6.driftjelly(pulse)[1]
    black = Material((0.0, 0.0, 0.0), metal=0.0, shininess=1.0, spec=0.0)
    return [Material((0.0, 0.0, 0.0), metal=0.0, shininess=1.0, spec=0.0, emission=m.emission,
                     emission_pattern=m.emission_pattern) if i in (V_SAC, V_GLOW) else black
            for i, m in enumerate(mats)]


def render_pulse(i):
    """The veins' bloom: the emission alone, blurred into a halo, brighter as the bell contracts."""
    pulse = PULSES[i]
    scene, _ = m6.driftjelly(pulse)
    hi, factor = artkit.render_hi(scene, glow_mats(pulse), SIZE, EXTENT, shadows=False)
    em_ = sprite.downsample(hi, factor)[..., :3] * sprite.downsample(hi, factor)[..., 3:4]
    lum = em_.max(axis=-1)
    img = Image.fromarray((np.clip(lum, 0, 1) * 255).astype(np.uint8))
    halo = np.asarray(img.filter(ImageFilter.GaussianBlur(2.2)), float) / 255
    core = np.asarray(img.filter(ImageFilter.GaussianBlur(0.7)), float) / 255
    gain = 0.2 + 0.5 * pulse
    k = np.clip(core * 0.7 + halo * 1.1, 0, 1.0) * gain
    rgb = LIME[None, None, :] * k[..., None] + np.array(WHITE_LIME) * np.clip(core - 0.55, 0, 1)[..., None] * gain
    stepped_k = np.floor(np.clip(rgb / 255, 0, 1) * 6 + 0.5) / 6 * 255   # a few glow steps
    a = np.where(stepped_k.max(axis=-1) >= 1, 255, 0)
    return Image.fromarray(np.dstack([stepped_k, a]).astype(np.uint8), "RGBA")


def waterline_radius(above):
    return float(np.sqrt((np.array(above)[..., 3] > 0).sum() / np.pi))


def with_collar(above, i, n, seed):
    """``above`` with its foam collar (loop phase i of n) round the waterline: the collar lies on
    the water round the dome and laps over its rim."""
    w, h = above.size
    r = waterline_radius(above)
    f = Field(w, h)
    foam_ring(f, w / 2, h / 2, r, TAU * i / n, seed, width=1.6, specks=10, reach=6.0)
    foam = f.image()
    out = Image.new("RGBA", (w, h))
    out.alpha_composite(foam)
    out.alpha_composite(above)
    yy, xx = np.mgrid[0:h, 0:w]
    fa = np.array(foam)
    fa[np.hypot(xx + 0.5 - w / 2, yy + 0.5 - h / 2) < r - 1.2] = 0
    out.alpha_composite(Image.fromarray(fa, "RGBA"))
    return out


def surface_frame(j):
    """Swap frame j of 6: the water plane at SURFACE_LEVELS[j]; frame 0 is only the swell."""
    level = SURFACE_LEVELS[j]
    w, h = SIZE
    f = Field(w, h)
    rise = j / (SURFACE_FRAMES - 1)
    swell(f, w / 2, h / 2 - 0.5, 9.5 + 3 * rise, 0.9 - 0.5 * rise)
    if j == 0:
        foam_ring(f, w / 2, h / 2, 6.0, 0.4, 3301, width=1.0, specks=4, reach=3, gain=0.55)
        return f.image()
    above = render_above(0.0, level)
    r = waterline_radius(above)
    foam_ring(f, w / 2, h / 2, max(r, 2.5), 0.9 * j, 3301 + j, width=1.2 + 0.25 * j, specks=6 + 2 * j,
              reach=4 + 1.4 * j, gain=0.6 + 0.08 * j)
    out = f.image()
    out.alpha_composite(streaming(above, j))
    rim = Field(w, h)
    foam_ring(rim, w / 2, h / 2, max(r, 2.5), 0.9 * j + 1.3, 3311 + j, width=0.9, specks=0, gain=0.7)
    out.alpha_composite(rim.image())
    return out


def streaming(above, j):
    """Water streaming off a part that just surfaced: a pale wet sheen in thin runnels down the
    dome from the crown, strongest just after it broke the surface."""
    a = np.array(above).astype(np.float64)
    h, w = a.shape[:2]
    yy, xx = np.mgrid[0:h, 0:w]
    th = np.arctan2(yy - h / 2, xx - w / 2)
    rr = np.hypot(yy - h / 2, xx - w / 2)
    runnel = np.clip(np.sin(th * 9 + rr * 0.35 + j), 0, 1) ** 6
    k = (0.12 + 0.3 * runnel) * (0.5 + 0.5 * (j / (SURFACE_FRAMES - 1))) * (a[..., 3] > 0)
    a[..., :3] = a[..., :3] * (1 - k[..., None]) + np.array([175, 215, 242]) * k[..., None]
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


def ripple_frame(i):
    """Frame i of the contraction's ripple train."""
    w, h = RIPPLE_SIZE
    f = Field(w, h)
    life = RIPPLE_FRAMES * RIPPLE_STEPS / STEP
    t = (i + 0.5) * RIPPLE_STEPS / STEP
    for k, off in enumerate(TRAIN):
        age = t - off
        if age < 0:
            continue
        r = RIPPLE_R0 + RIPPLE_SPEED * age
        alpha = (1 - age / (life - off)) ** 1.5 * (1 - 0.22 * k)
        ripple_ring(f, w / 2, h / 2, r, alpha, 3320 + 7 * k)
    return f.image()


# =========================================================================== death

def death_glow(i):
    """The `small` wet pop: a white-lime flash, lime droplets flung out and falling back, a haze."""
    w, h = DEATH_SIZE
    t = fx.t_of(i, DEATH_FRAMES)
    c = w / 2
    cv = v8.Canvas(w, h)
    d = cv.dist(c, c)
    fade = np.clip((w / 2 - d) / 4.0, 0, 1)
    cv.add(WHITE_LIME, v8.gauss(d, 3 + 5 * t) * 1.4 * np.clip(1 - t * 2.8, 0, 1))
    cv.add(tuple(LIME), v8.gauss(d, 7 + 10 * fx.ease_out(t)) * 0.55 * (1 - t) ** 1.6 * fade)
    tex = v8._cart_noise(w, 3331, cell=w * v8.SS // 5)
    haze = np.clip(v8.gauss(d, 8 + 12 * fx.ease_out(t, 2.2)) * (0.5 + 0.8 * tex) - 0.25 - 0.6 * t, 0, 1)
    cv.add((60, 110, 20), haze * 0.9 * (1 - t) * fade)
    vd.motes(cv, np.random.default_rng(3332), c, t, 22, (10, 26), (0.5, 1.0), tuple(LIME), WHITE_LIME, fade,
             delay=0.05, gain=1.1)
    return artkit.additive(cv.image())


def death_pieces():
    """The bell torn in shreds (curled plates of the veined bell), the tentacles in pieces, the knob."""
    rng = np.random.default_rng(3341)
    pieces = []
    for k in range(7):
        a = TAU * k / 7 + rng.uniform(-0.25, 0.25)
        cx, cy = 0.32 * np.cos(a), 0.32 * np.sin(a)
        poly = fx.jagged(rng, rng.uniform(0.22, 0.3), 7)
        bend = rng.uniform(1.0, 2.0)

        def shred(q, cx=cx, cy=cy, poly=poly, bend=bend):
            r = q.copy()
            r[:, 0] -= cx
            r[:, 1] -= cy
            r[:, 2] -= 0.2 - bend * (r[:, 0] ** 2 + r[:, 1] ** 2)
            return sd_plate(r, poly, 0.0, 0.035, 0.012)
        c0 = (cx, cy, 0.2)
        pieces.append(dict(parts=[(shred, V_SAC)], c0=c0, dir=vd.outward(rng, c0, 0.4), reach=rng.uniform(0.45, 0.8),
                           ease=2.6, axis=rng.uniform(0, TAU), tumble=rng.uniform(3, 6) * rng.choice([-1, 1]),
                           spin=rng.uniform(-3, 3), rise=0.35, shrink_at=0.55))
    for j in range(8):
        ang = (j + 0.5) * TAU / 8
        ca, sa = np.cos(ang), np.sin(ang)

        def tentacle(q, ca=ca, sa=sa):
            return sd_capsule(q, (ca * 0.5, sa * 0.5, -0.05), (ca * 0.9, sa * 0.9, -0.1), 0.045, 0.025)
        c0 = (ca * 0.7, sa * 0.7, -0.07)
        pieces.append(dict(parts=[(tentacle, V_BONE)], c0=c0, dir=vd.outward(rng, c0, 0.3),
                           reach=rng.uniform(0.25, 0.5), ease=2.4, axis=rng.uniform(0, TAU),
                           tumble=rng.uniform(2, 5) * rng.choice([-1, 1]), spin=rng.uniform(-3, 3),
                           rise=0.15, shrink_at=0.6))
    pieces.append(dict(parts=[(lambda q: sd_sphere(q, (0.0, 0.0, 0.5), 0.13), V_GLOW)], c0=(0.0, 0.0, 0.5),
                       dir=(0.3, -0.95), reach=0.25, ease=3.0, axis=0.4, tumble=2.0, spin=1.0, rise=0.3,
                       shrink_at=0.5))
    pieces.append(dict(parts=[(lambda q: sd_sphere(q, (0.0, 0.0, 0.0), 0.2), V_DARK)], c0=(0.0, 0.0, 0.0),
                       dir=(0.0, 1.0), reach=0.05, ease=3.0, axis=0.0, tumble=0.5, spin=0.5, rise=0.0,
                       shrink_at=0.3, least=0.1))
    return pieces


def death_tatters(i):
    t = fx.t_of(i, DEATH_FRAMES)
    mats = m6.driftjelly(0.3, glow=0.6)[1]
    scene, mats = creeper.pieces_scene(death_pieces(), mats, t, lambda p: p)
    img = artkit.native(*artkit.render_hi(scene, mats, DEATH_SIZE, DEATH_EXTENT))
    sink = float(np.clip((t - 0.45) / 0.55, 0, 1))             # the shreds sink: toward the water
    if sink > 0:
        a = np.array(img).astype(np.float64)
        k = 0.7 * sink
        a[..., :3] = a[..., :3] * (1 - k) * (1 - 0.3 * sink) + WATER_TINT * k
        img = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")
    return img


# =========================================================================== build

def _job(job):
    kind, i = job
    if kind == "sub":
        return render_body(PULSES[i])
    if kind == "dome":
        return render_above(PULSES[i], 0.0)
    if kind == "pulse":
        return render_pulse(i)
    if kind == "surface":
        return surface_frame(i)
    if kind == "ripple":
        return ripple_frame(i)
    if kind == "death":
        return death_glow(i)
    return death_tatters(i)


def build():
    jobs = ([("sub", i) for i in range(4)] + [("dome", i) for i in range(4)] + [("pulse", i) for i in range(4)]
            + [("surface", i) for i in range(SURFACE_FRAMES)] + [("ripple", i) for i in range(RIPPLE_FRAMES)]
            + [("death", i) for i in range(DEATH_FRAMES)] + [("tatters", i) for i in range(DEATH_FRAMES)])
    with ProcessPoolExecutor() as pool:
        out = list(pool.map(_job, jobs))
    sub, dome, pulse = out[0:4], out[4:8], out[8:12]
    at = 12
    surface = out[at:at + SURFACE_FRAMES]
    at += SURFACE_FRAMES
    ripple = out[at:at + RIPPLE_FRAMES]
    at += RIPPLE_FRAMES
    death, tatters = out[at:at + DEATH_FRAMES], out[at + DEATH_FRAMES:]
    # one palette over the body, the domes and the swap (the parts must match); foam stays white
    surfaced = [with_collar(d, i, 4, 3350) for i, d in enumerate(dome)]
    body = artkit.quantize_set(sub + surfaced + surface, COLOURS)
    sets = [(f"{SLUG}-sub", body[0:4]), (SLUG, body[4:8]), (f"{SLUG}-surface", body[8:]),
            (f"{SLUG}-pulse", artkit.quantize_set(pulse, 12)), (f"{SLUG}-ripple", artkit.quantize_set(ripple, 8)),
            (f"{SLUG}-death", artkit.quantize_set(death, 24)), (f"{SLUG}-tatters", artkit.quantize_set(tatters, 24))]
    for name, frames in sets:
        artkit.write_frames(name, frames, SOURCE)
        print(f"{name}: {len(frames)} frames, {frames[0].width}x{frames[0].height}")


# =========================================================================== review

def put(cell, img, x, y):
    cell.alpha_composite(img, (int(round(x - img.width / 2)), int(round(y - img.height / 2))))


def add(cell, img, x, y):
    return artkit.add_light(cell, img, (int(round(x - img.width / 2)), int(round(y - img.height / 2))))


def draw_jelly(cell, art, x, y, state, pulse_i, t, step=0):
    """A jelly as the game draws it: the body through the `sub` pass stand-in, then the surface layer."""
    put(cell, sub_pass(art["sub"][pulse_i], t), x, y)
    if state == "sub":
        return add(cell, art["pulse"][pulse_i], x, y)
    if state == "swap":
        put(cell, art["surface"][step], x, y)
        return cell
    put(cell, art["dome"][pulse_i], x, y)
    return cell


def review():
    artkit.REVIEW_ROUND = REVIEW_ROUND
    art = {"sub": artkit.load_frames(f"{SLUG}-sub"), "dome": artkit.load_frames(SLUG),
           "pulse": artkit.load_frames(f"{SLUG}-pulse"), "surface": artkit.load_frames(f"{SLUG}-surface"),
           "ripple": artkit.load_frames(f"{SLUG}-ripple"), "death": artkit.load_frames(f"{SLUG}-death"),
           "tatters": artkit.load_frames(f"{SLUG}-tatters")}
    boom = artkit.load_frames("explosion-small")

    def on_sea(img, size=(56, 56), glow=False, seed=5):
        cell = ocean_plate(*size, seed=seed, x0=120, y0=120)
        return add(cell, img, size[0] / 2, size[1] / 2) if glow else (put(cell, img, size[0] / 2, size[1] / 2) or cell)

    layered = []
    for i in range(4):
        layered.append(draw_jelly(ocean_plate(56, 56, x0=120, y0=120), art, 28, 28, "surf", i, 0.3 * i))
    sub_drawn = [draw_jelly(ocean_plate(56, 56, x0=120, y0=120), art, 28, 28, "sub", i, 0.3 * i) for i in range(4)]
    swap = [draw_jelly(ocean_plate(56, 56, x0=120, y0=120), art, 28, 28, "swap", 0, 0.1 * j, j)
            for j in range(SURFACE_FRAMES)]
    ripples = [on_sea(r, RIPPLE_SIZE) for r in art["ripple"]]
    layers = [(art["tatters"], 4, False, 0), (boom, 2, True, 0), (art["death"], 4, True, 0)]
    together = fx.composite_strip(layers, DEATH_FRAMES * 4, DEATH_SIZE, 1)
    sheet = artkit.review_sheet("DRIFTJELLY - FINAL SPRITES (PROPOSAL)", [
        ("BODY FOR THE SUB PASS: 4 PULSE FRAMES (REST, CONTRACTING, CONTRACTED, RELAXING)", art["sub"], 3, False),
        ("SURFACED: THE DOME ABOVE THE WATERLINE WITH ITS FOAM COLLAR (SURFACE LAYER)", art["dome"], 3, False),
        ("PULSE BLOOM (ADDITIVE, OVER A SUBMERGED JELLY)", art["pulse"], 3, True),
        ("SWAP, SURFACE LAYER: SWELL, CROWN FIRST, COLLAR GROWING, WATER STREAMING OFF (6 STEPS A FRAME)",
         art["surface"], 3, False),
        ("AS DRAWN ON THE SEA STAND-IN: SURFACED (SUB-PASS BODY + DOME), PULSE FRAMES 0-3", layered, 3, False),
        ("AS DRAWN: SUBMERGED (SUB PASS + BLOOM), PULSE FRAMES 0-3", sub_drawn, 3, False),
        ("AS DRAWN: SURFACING 0 -> 5 (DIVING IS 5 -> 0)", swap, 3, False),
        ("RIPPLE TRAIN OF ONE CONTRACTION, 8 STEPS A FRAME, ON THE SEA STAND-IN", ripples[::2], 1, False),
        ("DEATH: WET POP (ADDITIVE)", art["death"], 2, True),
        ("DEATH: SHREDS, SINKING (SOLID)", art["tatters"], 2, False),
        ("TOGETHER WITH EXPLOSION-SMALL, EVERY 2ND STEP", together[::2], 1, False)], width=1400, batch=BATCH)
    artkit.save_review(sheet, review_loop(art, boom), CONCEPT, SLUG, fps=20)


def review_loop(art, boom):
    """A field of seven jellies drifting down with the sea at the scroll (30 px/s) plus a 15 px/s
    current, each on its own swap timer, surfaced ones leaving ripple trains at their contractions;
    one is shot and pops. 240x270 at 2x."""
    fw, fh, fps = 240, 270, 20
    plate = ocean_plate(fw, 540, seed=121)
    rng = np.random.default_rng(3360)
    jellies = [dict(x=x, y=y, sub=s, ph=rng.uniform(0, 1), swap=rng.uniform(1.0, 4.0))
               for x, y, s in ((50, 40, False), (150, 10, True), (110, 120, False), (200, 100, True),
                               (60, 190, True), (170, 200, False), (120, 250, True))]
    period, total, scroll, current = 2.0, 6.5, 30.0, 15.0
    ripples = []
    dead = (5, 5.0)
    gif = []
    for f in range(int(total * fps)):
        t = f / fps
        off = int(t * scroll) % (540 - fh)
        cell = plate.crop((0, 540 - fh - off, fw, 540 - off))
        for x0, y0, t0 in ripples:
            age = int((t - t0) * STEP)
            fr = fx.timed(art["ripple"], RIPPLE_STEPS, age)
            if fr is not None:
                put(cell, fr, x0, y0 + (t - t0) * scroll)
        for i, j in enumerate(jellies):
            x = j["x"] + 8 * np.sin(0.7 * t + j["ph"] * 6)
            y = (j["y"] + (scroll + current) * t) % (fh + 60) - 30
            if i == dead[0] and t >= dead[1]:
                if t - dead[1] < 0.02:
                    j["death"] = (x, y)
                dx, dy = j["death"]
                age = int((t - dead[1]) * STEP)
                for frames, steps, glow in ((art["tatters"], 4, False), (boom, 2, True), (art["death"], 4, True)):
                    fr = fx.timed(frames, steps, age)
                    if fr is not None:
                        cell = add(cell, fr, dx, dy + (t - dead[1]) * scroll) if glow else \
                            (put(cell, fr, dx, dy + (t - dead[1]) * scroll) or cell)
                continue
            phase = (t / period + j["ph"]) % 1.0
            pi = int(phase * 4) % 4
            swaps = int((t - j["swap"]) // 5.0) + 1 if t >= j["swap"] else 0   # a swap every 5 s
            sub = j["sub"] ^ (swaps % 2 == 1)
            since = (t - j["swap"]) % 5.0 if swaps else 9.0
            if since < 0.6:
                step = min(int(since / 0.6 * SURFACE_FRAMES), SURFACE_FRAMES - 1)
                step = SURFACE_FRAMES - 1 - step if sub else step   # diving, else surfacing
                cell = draw_jelly(cell, art, x, y, "swap", 0, t, step)
                continue
            state = "sub" if sub else "surf"
            cell = draw_jelly(cell, art, x, y, state, pi, t + i)
            if state == "surf" and pi == 2 and int(((t - 1 / fps) / period + j["ph"]) % 1.0 * 4) % 4 == 1:
                ripples.append((x, y, t))
        gif.append(sprite.enlarge(cell, 2))
    return gif


if __name__ == "__main__":
    artkit.REVIEW_ROUND = REVIEW_ROUND
    if "--review" not in sys.argv[1:]:
        build()
    review()
