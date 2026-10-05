#!/usr/bin/env python3
"""Production art: Level 06's darkness (design/campaign/act-1-first-contact/level-06-farside,
Darkness rules, user decision D2; M4 part F batch).

Outputs (assets/sprites/):
  spine-turret-glow_0..31   40x40, additive: the Spine Turret's emissive parts only (the violet
                            barrel tip, the crown eye and the seams) per barrel heading, indexed like
                            spine-turret_0..31, with a tight stepped halo; drawn at full brightness
                            over the darkened ground after the light pass
  polyp-mortar-glow_0..7    44x44, additive: the Polyp Mortar's lime mouth and veins per frame of its
                            idle pulse, indexed like polyp-mortar_0..7, with a halo
  flare-shell_0..3          32x32, additive: the perimeter beacon's falling flare, a white-hot core
                            in an orange-white halo with sparks, 4 frames of flicker
  flare-pool                240x240: the flare's light pool in the light map, white with its
                            falloff ordered-dithered to 6 steps (stored as the light map takes it:
                            white, the light in the alpha; the game tints it and scales it to the
                            data's pool)
  headlight-cone            232x200: the ship's headlight in the light map, a 60-degree cone
                            200 px long pointing up, a brighter core, soft dithered sides and end
  design/campaign/act-1-first-contact/level-06-farside/concept/darkness-final-r23-a.png/.gif

The glows are the chosen production models (tools/art/vrell_ground.py's turret,
tools/art/l05_hazards.py's mortar; the round-04 models imported unchanged) rendered with every
material's albedo, specular and reflection off, so only their emission is left, through the same
render path (8x, 1-bit alpha), plus a halo stepped to 4 levels. The flare and the light shapes are
2D light fields (vfx_r08's Canvas) like the other effects.

Run: python3 tools/art/l06_darkness.py [--review]   (~30 s)
"""
import sys
from concurrent.futures import ProcessPoolExecutor
from dataclasses import replace

import numpy as np
from PIL import Image, ImageFilter

import artkit
from artkit import DESIGN, TAU, sprite

import enemies_r04 as e4  # noqa: E402  (concept script, imported unchanged)
import l05_hazards  # noqa: E402  (tools/art: the mortar's pulse)
import vfx_r08 as v8  # noqa: E402
import vrell_ground  # noqa: E402  (tools/art: the turret's size and extent)
from render import enemy_models as em  # noqa: E402

SCRIPT = "l06_darkness.py"
BATCH = "M4 part F batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
REVIEW_ROUND = "r23"
LEVEL = DESIGN / "campaign" / "act-1-first-contact" / "level-06-farside"
TURRET_HEADINGS = vrell_ground.HEADINGS
PULSE = l05_hazards.PULSE
FLARE_FRAMES, FLARE_SIZE = 4, 32
POOL = 240
CONE_LENGTH, CONE_ANGLE = 200, np.radians(60)
HALO = 2.2                                   # px, the glows' halo radius


def emission_only(mats):
    return [replace(m, albedo=(0.0, 0.0, 0.0), metal=0.0, spec=0.0, pattern=None) for m in mats]


def glow(scene, mats, size, extent):
    """The emission of a model through the sprite path, with a soft halo stepped to 4 levels."""
    hi, factor = artkit.render_hi(scene, emission_only(mats), size, extent, shadows=False, ambient=0.0, fill=0.0)
    core = np.array(artkit.native(hi, factor, crisp=0)).astype(np.float64)
    rgb = core[..., :3] * (core[..., 3:4] / 255)
    lum = rgb.max(axis=-1)
    rgb[lum < 40] = 0                                          # the dark body adds nothing
    blur = np.array(Image.fromarray(rgb.astype(np.uint8)).filter(ImageFilter.GaussianBlur(HALO))).astype(np.float64)
    halo = blur * 1.6
    out = np.maximum(rgb, halo)
    lit = out.max(axis=-1)
    alpha = np.where(rgb.max(axis=-1) > 0, 1.0, np.floor(np.clip(lit / 255 * 1.4, 0, 1) * 3 + 0.5) / 3)
    img = np.dstack([np.where(alpha[..., None] > 0, out / np.maximum(alpha[..., None], 1e-6), 0), alpha * 255])
    return artkit.additive(Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGBA"))


def turret_glow(k):
    scene, mats = e4.R04["spine-turret-a"][4](anim=TAU * k / TURRET_HEADINGS, glow=1.0)
    return glow(scene, mats, vrell_ground.size_of("spine-turret"), vrell_ground.EXTENT)


def mortar_glow(i):
    s = np.sin(TAU * i / PULSE)
    scene, mats = e4.R04["polyp-mortar-a"][4](anim=s, glow=1.0 + 0.25 * s)
    return glow(scene, mats, l05_hazards.SIZE, l05_hazards.EXTENT)


def flare_shell(i):
    """The flare: a white-hot core, an orange-white halo that flickers, sparks falling off it."""
    rng = np.random.default_rng(2320 + i)
    c = FLARE_SIZE / 2
    cv = v8.Canvas(FLARE_SIZE, FLARE_SIZE)
    d = cv.dist(c, c)
    flick = [1.0, 0.85, 0.95, 0.8][i]
    cv.add((255, 200, 130), v8.gauss(d, 7.5 * flick) * 0.55)
    cv.add((255, 236, 190), v8.gauss(d, 3.2) * 1.0)
    cv.add((255, 255, 245), v8.gauss(d, 1.4) * 1.6)
    for _ in range(5):
        a = rng.uniform(0.3, np.pi - 0.3)                     # below the core: sparks drop off
        r = rng.uniform(4, 12)
        cv.add((255, 190, 110), v8.gauss(cv.dist(c + np.cos(a) * r * 0.6, c + np.sin(a) * r), 0.7) * 1.2)
    return artkit.additive(cv.image())


def light_shape(intensity):
    """A light-map shape: white, its light in the alpha, posterized to 6 steps with 4x4 Bayer
    ordered dither (the falloff reads as the 90s stepped pool, never a smooth gradient band)."""
    a = artkit.ordered_dither(intensity, 6)
    img = np.dstack([np.full(a.shape + (3,), 255.0), a * 255])
    img[a <= 0] = 0
    return Image.fromarray(img.astype(np.uint8), "RGBA")


def flare_pool():
    yy, xx = np.mgrid[0:POOL, 0:POOL]
    r = np.hypot(xx + 0.5 - POOL / 2, yy + 0.5 - POOL / 2) / (POOL / 2)
    hot = np.clip(1 - r, 0, 1)
    return light_shape(np.clip(hot ** 1.3 * 1.05 + 0.25 * np.exp(-(r / 0.22) ** 2), 0, 1))


def headlight_cone():
    width = int(np.ceil(2 * CONE_LENGTH * np.tan(CONE_ANGLE / 2))) + 1
    width += width % 2
    yy, xx = np.mgrid[0:CONE_LENGTH, 0:width]
    along = (CONE_LENGTH - yy - 0.5) / CONE_LENGTH               # row 0 is the far end
    off = np.abs(np.arctan2(xx + 0.5 - width / 2, CONE_LENGTH - yy - 0.5)) / (CONE_ANGLE / 2)
    side = np.clip((1 - off) * 3.0, 0, 1)
    core = np.exp(-(off / 0.35) ** 2) * 0.25
    reach = np.clip(1 - along ** 2, 0, 1) * (0.85 + 0.15 * np.clip(along * 4, 0, 1))
    return light_shape(np.clip((side + core) * reach, 0, 1))


def _job(job):
    kind, i = job
    return {"turret": turret_glow, "mortar": mortar_glow, "flare": flare_shell}[kind](i)


def build():
    jobs = ([("turret", k) for k in range(TURRET_HEADINGS)] + [("mortar", i) for i in range(PULSE)]
            + [("flare", i) for i in range(FLARE_FRAMES)])
    with ProcessPoolExecutor() as pool:
        out = list(pool.map(_job, jobs))
    t, m = TURRET_HEADINGS, PULSE
    artkit.write_frames("spine-turret-glow", artkit.quantize_set(out[:t], 16), SOURCE)
    artkit.write_frames("polyp-mortar-glow", artkit.quantize_set(out[t:t + m], 16), SOURCE)
    artkit.write_frames("flare-shell", artkit.quantize_set(out[t + m:], 16), SOURCE)
    artkit.write_frames("flare-pool", [flare_pool()], SOURCE, single=True)
    artkit.write_frames("headlight-cone", [headlight_cone()], SOURCE, single=True)


# --------------------------------------------------------------------------- review

def ground(w, h, seed=6):
    """A stand-in regolith patch for the review (the level's tiles come from backdrop_l06.py)."""
    rng = np.random.default_rng(seed)
    base = np.array(Image.fromarray((rng.random((h // 8 + 1, w // 8 + 1)) * 255).astype(np.uint8))
                    .resize((w, h), Image.BICUBIC)).astype(np.float64) / 255
    g = 70 + 40 * base
    return np.dstack([g * 0.95, g * 0.95, g * 1.02])


def lit(ground_rgb, lights, ambient=0.06):
    """The light map as the game builds it (additive light shapes, tinted) multiplying the ground."""
    h, w, _ = ground_rgb.shape
    lm = np.full((h, w, 3), ambient)
    for img, (x, y), tint, k in lights:
        a = np.array(img).astype(np.float64) / 255
        x0, y0 = int(x - img.width / 2), int(y - img.height / 2)
        xs, ys = slice(max(0, x0), min(w, x0 + img.width)), slice(max(0, y0), min(h, y0 + img.height))
        part = a[ys.start - y0:ys.stop - y0, xs.start - x0:xs.stop - x0]
        lm[ys, xs] += part[..., :3] * part[..., 3:4] * np.array(tint) * k
    return np.clip(ground_rgb * np.clip(lm, 0, 1.4), 0, 255)


def review():
    artkit.REVIEW_ROUND = REVIEW_ROUND
    tg = artkit.load_frames("spine-turret-glow")
    mg = artkit.load_frames("polyp-mortar-glow")
    flare = artkit.load_frames("flare-shell")
    pool = artkit.load_frames("flare-pool")[0]
    cone = artkit.load_frames("headlight-cone")[0]
    turret = artkit.load_frames("spine-turret")
    mortar = artkit.load_frames("polyp-mortar")
    ship = artkit.load_frames("ship")[2]
    W, H = 480, 400
    base = ground(W, H)
    gif = []
    flare_tint, head_tint = (1.0, 0.78, 0.5), (0.72, 0.84, 1.0)
    units = [("t", 130, 120), ("t", 330, 170), ("m", 230, 250), ("m", 400, 80)]
    for i in range(60):
        t = i / 10
        sx, sy = 240 + 140 * np.sin(TAU * t / 6), 340
        fy = 30 + 36 * t
        img = Image.fromarray(np.dstack([base, np.full(base.shape[:2], 255)]).astype(np.uint8), "RGBA")
        for kind, x, y in units:          # the ground units under the light map
            fr = turret[int(round(np.arctan2(-(sx - x), sy - y) / TAU * 32)) % 32] if kind == "t" else mortar[i % 8]
            sprite.paste_center(img, fr, x, y)
        arr = np.array(img).astype(np.float64)
        arr[..., :3] = lit(arr[..., :3], [(pool, (200, fy), flare_tint, 1.0), (cone, (sx, sy - 18 - cone.height / 2), head_tint, 1.0)])
        img = Image.fromarray(arr.astype(np.uint8), "RGBA")
        for kind, x, y in units:          # the glows after the light pass
            if kind == "t":
                g = tg[int(round(np.arctan2(-(sx - x), sy - y) / TAU * 32)) % 32]
            else:
                g = mg[i % 8]
            img = artkit.add_light(img, g, (x - g.width // 2, y - g.height // 2))
        f = flare[i % 4]
        img = artkit.add_light(img, f, (200 - f.width // 2, int(fy) - f.height // 2))
        sprite.paste_center(img, ship, sx, sy)
        gif.append(img)
    sheet = artkit.review_sheet("LEVEL 06 DARKNESS - GLOW FRAMES, FLARE, LIGHT SHAPES", [
        ("IN THE DARK: GROUND AND GROUND UNITS UNDER THE LIGHT MAP (AMBIENT 6 %), FLARE POOL, HEADLIGHT, THEN THE GLOWS",
         [gif[0], gif[25]], 1, False),
        ("SPINE TURRET GLOW, EVERY 4TH OF 32 HEADINGS (ADDITIVE)", tg[::4], 3, True),
        ("POLYP MORTAR GLOW, THE 8 PULSE FRAMES (ADDITIVE)", mg, 3, True),
        ("FLARE SHELL, 4 FLICKER FRAMES (ADDITIVE)", flare, 3, True),
        ("FLARE POOL 240X240 AND HEADLIGHT CONE 232X200 (LIGHT-MAP SHAPES, WHITE ON BLACK)", [pool, cone], 1, True)],
        width=1400, batch=BATCH)
    artkit.save_review(sheet, gif, LEVEL / "concept", "darkness", fps=10)


if __name__ == "__main__":
    if "--review" not in sys.argv[1:]:
        build()
    review()
