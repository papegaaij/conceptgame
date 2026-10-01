#!/usr/bin/env python3
"""Concept round 07 - rotation and waterline fixes from the user's round 06 review.

Outputs (design/enemies/...):
  ground/concept/halo-platform-r07-a.{png,gif}  Halo Platform with a dense angle set: the six
                                                ring segments are rendered as one 6-fold model
                                                over a single 60 degree symmetry step at 128
                                                frames (0.47 degree each, 768 per full turn);
                                                turrets at 64 headings; speed-ups ramp smoothly
  naval/concept/driftjelly-r07-a.{png,gif}      Driftjelly with a real waterline: animated broken
                                                foam collar, ripple trains from the bell pulse,
                                                the submerged bell and tentacles visible through
                                                the water (tinted, darker, soft, wavy)

Art direction rules applied: "Smooth slow rotation" and "Water" (design/art-direction).
Models: render/r07_models.py (assembled ring, water-plane clipping) on top of r06_models.
The 128 ring frames render in parallel processes (~15 s each at factor 4) and are cached in
the system temp dir (conceptgame-cache/), so re-runs only redo the compositing.
Run: python3 tools/concept/enemies_r07.py [halo-platform] [driftjelly]   (no args = both)
"""
import os
import sys
import tempfile
import time
from concurrent.futures import ProcessPoolExecutor
from multiprocessing import get_context
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, str(Path(__file__).resolve().parent))
import enemies_r03 as e3  # noqa: E402
import enemies_r04 as e4  # noqa: E402
import enemies_r05 as r5  # noqa: E402
import enemies_r06 as e6  # noqa: E402
from render import enemy_rigs as rig  # noqa: E402
from render import r06_models as m6  # noqa: E402
from render import r07_models as m7  # noqa: E402
from render import raster, sdf, sprite  # noqa: E402
from render.config import ROOT  # noqa: E402

r5.SUB = "CONCEPT ROUND 07 - 960X540"
FW, FH, TAU = r5.FW, r5.FH, 2 * np.pi
paste, grid, source_panel, Bullets = r5.paste, r5.grid, e6.source_panel, r5.Bullets
CACHE = Path(tempfile.gettempdir()) / "conceptgame-cache"


def save_unit(category, slug, sheet, frames, fps):
    png = r5.out(category, f"{slug}-r07-a.png")
    sheet.convert("RGB").save(png, optimize=True)
    size = rig.write_gif(frames, r5.out(category, f"{slug}-r07-a.gif"), fps=fps)
    print(f"wrote {png.relative_to(ROOT)} and .gif ({size / 1e6:.1f} MB, {len(frames)} frames)")


def rim_raw(sp):
    """``enemies_r04.rim`` without its per-frame quantise (the ring shares one palette)."""
    a = np.array(sp)
    op = a[..., 3] > 0
    pad = np.pad(op, 1)
    left, up = ~pad[1:-1, :-2], ~pad[:-2, 1:-1]
    right, down = ~pad[1:-1, 2:], ~pad[2:, 1:-1]
    out = a.astype(np.float64)
    for mask, col, k in (((left | up) & op, e4.GOLD_RIM, 0.7),
                         ((right | down) & op & ~(left | up), e4.RED_RIM, 0.55)):
        out[mask, :3] = out[mask, :3] * (1 - k) + np.array(col) * k
    return Image.fromarray(out.astype(np.uint8), "RGBA")


def shared_palette(images, colors):
    """One median-cut palette over the opaque pixels of several frames."""
    pix = np.concatenate([np.array(im)[np.array(im)[..., 3] > 0][:, :3] for im in images])
    sample = Image.fromarray(pix.reshape(-1, 1, 3), "RGB")
    return sample.quantize(colors=colors, method=Image.Quantize.MEDIANCUT,
                           dither=Image.Dither.NONE)


def quantize_to(img, pal_img):
    arr = np.array(img)
    opaque = arr[..., 3] > 0
    rgb = Image.fromarray(arr[..., :3], "RGB").quantize(palette=pal_img, dither=Image.Dither.NONE)
    out = np.concatenate([np.array(rgb.convert("RGB")), arr[..., 3:4]], axis=-1)
    out[~opaque] = 0
    return Image.fromarray(out, "RGBA")


def smoothstep(x):
    x = np.clip(x, 0.0, 1.0)
    return x * x * (3 - 2 * x)


# =========================================================================== Halo Platform

PXU = 110 / 2.3                    # px per model unit, as the round 06 segments
HALO_R = 96                        # ring radius in px (round 06)
RING_N = 260                       # native canvas of the assembled ring
RING_STEPS = 128                   # frames per 60 degree symmetry step
RING_FACTOR = 4
STEP = m7.SEC / RING_STEPS
TUR_N, TUR_HEADINGS = 36, 64
halo_tur = rig.ModelSpaceAngleSprites(lambda: m6.halo_turret(), TUR_N, TUR_HEADINGS, factor=6,
                                      colors=24, post=e4.rim)
HALO_KILLS = {1: 3.0, 3: 5.0, 5: 6.6}      # turret index -> destroyed at (round 06 timing)
SHIELD_DROP = 6.6
BASE_SPEED, KILL_SPEEDUP, RAMP = 0.35, 0.25, 0.8      # rad/s, rad/s per kill, ramp seconds


def halo_speed(t):
    return BASE_SPEED + KILL_SPEEDUP * sum(smoothstep((t - k) / RAMP) for k in HALO_KILLS.values())


_TABLE_DT = 1 / 600
_ts = np.arange(0, 12, _TABLE_DT)
_ANG = np.concatenate([[0.0], np.cumsum([halo_speed(t) for t in _ts[:-1]]) * _TABLE_DT])


def halo_angle(t):
    """Ring rotation in screen radians (clockwise), integrated from the smoothly ramped speed."""
    return float(np.interp(t, _ts, _ANG))


def _ring_frame_worker(k):
    """Render ring frame ``k`` (model rotation k * STEP) before quantising; cached as PNG."""
    path = CACHE / f"halo-ring-r07-{RING_STEPS}-{RING_FACTOR}-{k:03d}.png"
    if path.exists():
        return k, np.array(Image.open(path).convert("RGBA"))
    scene0, mats = m7.halo_ring(HALO_R / PXU)
    rot = k * STEP
    scene = lambda p: scene0(sdf.rotate_z(p, rot))  # noqa: E731
    hi = sdf.render(scene, rig.model_space_materials(mats, rot),
                    (RING_N * RING_FACTOR, RING_N * RING_FACTOR), RING_N / PXU)
    img = sprite.sharpen(sprite.to_image(sprite.downsample(hi, RING_FACTOR), 0.5), 90)
    img = rim_raw(img)
    img.save(path)
    return k, np.array(img)


_RING = {}


def ring_frames():
    """All RING_STEPS frames, rendered in parallel, quantised to one shared 48-colour palette
    so nothing flickers between frames."""
    if _RING:
        return _RING
    CACHE.mkdir(parents=True, exist_ok=True)
    t0 = time.time()
    workers = max(1, min(16, (os.cpu_count() or 4) - 2))
    with ProcessPoolExecutor(workers, mp_context=get_context("fork")) as pool:
        raw = dict(pool.map(_ring_frame_worker, range(RING_STEPS)))
    imgs = [Image.fromarray(raw[k], "RGBA") for k in range(RING_STEPS)]
    pal = shared_palette([imgs[k] for k in range(0, RING_STEPS, RING_STEPS // 4)], 48)
    for k, im in enumerate(imgs):
        _RING[k] = quantize_to(im, pal)
    print(f"ring: {RING_STEPS} frames ready in {time.time() - t0:.0f} s ({workers} processes)")
    return _RING


def ring_index(rot):
    """Frame index and the quantised screen rotation actually shown for rotation ``rot``. A
    frame rendered with model rotation r shows segment j at screen angle -(j*60deg + r), so the
    frame for screen rotation rot is r = -rot (mod 60deg)."""
    k = int(round(((-rot) % m7.SEC) / STEP)) % RING_STEPS
    return k, -k * STEP


def draw_shield(img, cx, cy, rot, alpha):
    """Rotating red hex shield, drawn 4x supersampled so its slow turn stays smooth."""
    r, s = 58, 4
    size = (2 * r + 12) * s
    lay = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(lay)
    c = size / 2
    for k in range(6):
        a0 = rot * 0.3 + k * TAU / 6
        pts = [(c + np.cos(a0 + j * TAU / 6) * r * s, c + np.sin(a0 + j * TAU / 6) * r * s)
               for j in range(6)]
        d.line(pts + [pts[0]], fill=(230, 60, 50, int(150 * alpha)), width=s)
    d.ellipse([c - r * s, c - r * s, c + r * s, c + r * s],
              outline=(255, 120, 80, int(200 * alpha)), width=2 * s)
    lay = lay.resize((size // s, size // s), Image.BOX)
    img.alpha_composite(lay, (int(round(cx - lay.width / 2)), int(round(cy - lay.height / 2))))


def halo_draw(img, cx, cy, t, aim=None, frames=None):
    frames = frames or ring_frames()
    k, rot = ring_index(halo_angle(t))
    shield = 1.0 if t < SHIELD_DROP else max(0.0, 1 - (t - SHIELD_DROP) / 0.5)
    paste(img, e6.halo_core.get(0.0), cx, cy, "ground")
    if shield > 0:
        draw_shield(img, cx, cy, halo_angle(t), shield)
    paste(img, frames[k], cx, cy, "ground")
    tur_pos = []
    for j in range(6):
        a = rot + j * TAU / 6
        x, y = cx + np.cos(a) * HALO_R, cy + np.sin(a) * HALO_R
        dead = j in HALO_KILLS and t >= HALO_KILLS[j]
        if dead:
            e6.explosion(img, x, y, (t - HALO_KILLS[j]) / 0.6, 1.4)
            d = ImageDraw.Draw(img)
            d.ellipse([x - 9, y - 9, x + 9, y + 9], fill=(20, 12, 20, 255),
                      outline=(90, 40, 20, 255))
            continue
        ta = np.arctan2(aim[1] - y, aim[0] - x) if aim is not None else a
        paste(img, halo_tur.get(ta), x, y)
        tur_pos.append((j, x, y, ta))
    return tur_pos


def old_ring_crop(t, box):
    """The round 06 method (each segment a separate sprite at 32 headings) for comparison."""
    img = Image.new("RGBA", (RING_N, RING_N), (22, 24, 34, 255))
    c = RING_N / 2
    rot = halo_angle(t)
    for j in range(6):
        a = rot + j * TAU / 6
        paste(img, e6.halo_seg.get(a + np.pi / 2), c + np.cos(a) * HALO_R, c + np.sin(a) * HALO_R)
    return img.crop(box)


def new_ring_crop(t, box, frames):
    img = Image.new("RGBA", (RING_N, RING_N), (22, 24, 34, 255))
    k, _ = ring_index(halo_angle(t))
    paste(img, frames[k], RING_N / 2, RING_N / 2)
    return img.crop(box)


def step_chart(w=520, h=150):
    """Displayed rotation of the outer edge over 1.2 s at the base speed, 60 game frames per
    second: 32 headings (staircase, round 06) against 768 per turn (round 07)."""
    img = Image.new("RGBA", (w, h), (22, 24, 34, 255))
    d = ImageDraw.Draw(img)
    r_edge, dur = 110.0, 1.2
    ts = np.arange(0, dur, 1 / 60)
    true = BASE_SPEED * ts
    old = np.round(true / (TAU / 32)) * (TAU / 32)
    new = np.round(true / STEP) * STEP
    span = r_edge * true[-1]
    def pt(i, v):
        return 30 + (w - 40) * i / (len(ts) - 1), h - 20 - (h - 34) * (r_edge * v) / span
    d.line([(30, h - 20), (w - 10, h - 20)], fill=(70, 74, 90, 255))
    d.line([(30, 10), (30, h - 20)], fill=(70, 74, 90, 255))
    d.line([pt(i, v) for i, v in enumerate(old)], fill=(255, 110, 90, 255), width=2)
    d.line([pt(i, v) for i, v in enumerate(new)], fill=(120, 255, 160, 255), width=2)
    raster.draw_text(img, 36, 8, "32 HEADINGS (R06): EDGE JUMPS ABOUT 22 PX AT ONCE", (255, 130, 110))
    raster.draw_text(img, 36, 20, "768 PER TURN (R07): ABOUT 0.9 PX PER STEP", (130, 255, 170))
    raster.draw_text(img, 36, h - 14, "1.2 S AT 60 FPS, BASE SPEED, OUTER EDGE (110 PX)",
                     raster.LABEL_DIM)
    return img


def halo_platform():
    frames_ring = ring_frames()
    bg = e6.asc_deck(161)
    CX, CY = 240, 210
    fps = 25
    ts = [i / fps for i in range(int(9.0 * fps))]
    bullets = Bullets()
    shots = []
    for t0 in np.arange(0.8, 9.0, 0.45):
        tp = halo_draw(Image.new("RGBA", (FW, FH)), CX, CY, t0, r5.player_at(t0), frames_ring)
        if tp:
            j, x, y, ta = tp[int(t0 * 3) % len(tp)]
            shots.append((t0, x + np.cos(ta) * 20, y + np.sin(ta) * 20))
    for t0, x, y in shots:
        px, py = r5.player_at(t0)
        bullets.fire(t0, x, y, px, py, 200, "gold")
    frames = []
    for t in ts:
        f = bg.copy()
        halo_draw(f, CX, CY, t, r5.player_at(t), frames_ring)
        if t >= SHIELD_DROP + 0.5:
            raster.draw_text(f, 176, 330, "SHIELD DOWN - CORE EXPOSED", (255, 200, 120))
        bullets.draw(f, t)
        r5.draw_player(f, t)
        frames.append(f)

    still = Image.new("RGBA", (RING_N + 30, RING_N + 30), (22, 24, 34, 255))
    halo_draw(still, still.width / 2, still.height / 2, 0.0, (still.width / 2, still.height * 2),
              frames_ring)
    sym = grid([frames_ring[k] for k in range(0, RING_STEPS, 32)], 4, 1)
    box = (150, 6, 250, 106)                      # top-right part of the ring
    dt = (TAU / 32) / BASE_SPEED / 6              # six samples across one old 11.25 deg step
    samples = [0.2 + i * dt for i in range(6)]
    old_row = [old_ring_crop(t, box) for t in samples]
    new_row = [new_ring_crop(t, box, frames_ring) for t in samples]
    comp = grid(old_row + new_row, 6, 2, labels=[f"R06 {i + 1}" for i in range(6)]
                + [f"R07 {i + 1}" for i in range(6)])
    turs = [halo_tur.frame(k) for k in range(0, TUR_HEADINGS, 4)]
    panels = [("ASSEMBLED (1X): CORE + RING + 64-HEADING TURRETS, SHIELD", still),
              (f"RING: ONE 60 DEG STEP = {RING_STEPS} FRAMES; FRAMES 0/32/64/96 (1X)", sym),
              (f"SAME 6 MOMENTS {dt * 1000:.0f} MS APART (2X): R06 32 HEADINGS VS R07", comp),
              ("DISPLAYED ROTATION OVER TIME", step_chart()),
              ("TURRET: EVERY 4TH OF 64 HEADINGS (1X)", grid(turs, 16, 1))]
    notes = ["HALO PLATFORM (R07) - SMOOTH ROTATION WITH A DENSE ANGLE SET",
             "THE SIX RING SEGMENTS ARE RENDERED AS ONE 6-FOLD SYMMETRIC MODEL, SO ONE 60 DEG STEP",
             f"COVERS THE WHOLE TURN: {RING_STEPS} FRAMES OF 0.47 DEG (768 PER TURN), THE OUTER EDGE MOVES",
             "ABOUT 0.9 PX PER FRAME CHANGE INSTEAD OF 22 PX WITH 32 HEADINGS PER SEGMENT. ONE SHARED",
             "PALETTE FOR ALL RING FRAMES (NO FLICKER). TURRETS AT 64 HEADINGS (1.8 PX AT THEIR TIP).",
             "EACH KILL RAMPS THE SPEED UP OVER 0.8 S INSTEAD OF JUMPING. GIF AT 25 FPS."]
    return r5.build_sheet("HALO PLATFORM - SMOOTH ROTATING TURRET RING", panels, notes,
                          frames[150]), frames, fps


# =========================================================================== Driftjelly

JLY_N, WATERLINE = 40, 0.0
jly_above = rig.ModelSpaceAngleSprites(
    lambda pulse=0.0: m7.clipped(m6.driftjelly, WATERLINE, "above")(pulse=pulse), JLY_N, 1,
    factor=6, colors=28)
jly_below = rig.ModelSpaceAngleSprites(
    lambda pulse=0.0: m7.clipped(m6.driftjelly, WATERLINE, "below")(pulse=pulse), JLY_N, 1,
    factor=6, colors=28)
WATER = (0, 52, 92)
FOAM = (226, 242, 248)
PULSE_W, RIPPLE_LIFE, RIPPLE_SPEED, TRAIN = 2.5, 2.4, 20.0, (0.0, 0.28, 0.56)


def wobble(sp, t, amp=0.7, freq=0.5, speed=5.0):
    """Row-wise sub-pixel sway: the refraction of a gently moving surface."""
    a = np.array(sp).astype(np.float64)
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


def through_water(sp, t, depth=0.4, gain=1.25):
    """Seen through the surface: tinted towards the water, a little darker, softer and
    swaying - but still clearly readable just below the surface."""
    a = np.array(sp).astype(np.float64)
    a[..., :3] = np.clip(a[..., :3] * gain, 0, 255) * (1 - depth) * 0.9 + np.array(WATER) * depth
    a[..., 3] *= 0.9
    img = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")
    return wobble(img.filter(ImageFilter.GaussianBlur(0.4)), t)


def waterline_radius(sp):
    return float(np.sqrt((np.array(sp)[..., 3] > 0).sum() / np.pi))


def _n(theta, t, seed):
    """Smooth irregular 0..1 noise around a circle, drifting with time."""
    v = (np.sin(theta * 7 + t * 1.7 + seed) * np.sin(theta * 3 - t * 1.1 + seed * 2.1)
         + 0.5 * np.sin(theta * 13 + t * 2.9 + seed * 0.7))
    return np.clip(0.5 + 0.45 * v, 0, 1)


def foam_collar(img, cx, cy, rw, t, seed):
    """Broken, wobbling white foam where the bell breaks the surface, shedding specks."""
    s, R = 4, int(rw + 16)
    lay = Image.new("RGBA", (2 * R * s, 2 * R * s), (0, 0, 0, 0))
    d = ImageDraw.Draw(lay)
    c = R * s
    th = np.linspace(0, TAU, 110, endpoint=False) + 0.05 * np.sin(t * 0.8 + seed)
    v = _n(th, t, seed)
    for a, val in zip(th, v):
        if val < 0.38:
            continue                                        # gaps: the collar is never closed
        rr = rw - 0.4 + 1.6 * val + 0.6 * np.sin(a * 11 + t * 3.1 + seed)
        size = (0.35 + 0.65 * val) * s
        x, y = c + np.cos(a) * rr * s, c + np.sin(a) * rr * s
        d.ellipse([x - size, y - size, x + size, y + size], fill=FOAM + (int(70 + 150 * val),))
    rng = np.random.default_rng(seed)
    for k in range(12):                                     # specks drifting off the collar
        ang, ph = rng.uniform(0, TAU), rng.uniform(0, 1)
        age = (t * 0.55 + ph) % 1.0
        rr = rw + 2 + age * 9
        a = ang + 0.3 * age
        x, y = c + np.cos(a) * rr * s, c + np.sin(a) * rr * s
        sz = 0.5 * s
        d.ellipse([x - sz, y - sz, x + sz, y + sz], fill=FOAM + (int(150 * (1 - age)),))
    lay = lay.resize((2 * R, 2 * R), Image.BOX)
    img.alpha_composite(lay, (int(round(cx - R)), int(round(cy - R))))


def ripple(img, cx, cy, r, alpha, seed):
    """One uneven ripple: a light crest with a darker trough just outside, wobbling in radius
    and broken up along its length; drawn 4x supersampled."""
    if alpha <= 0.02:
        return
    s, R = 4, int(r + 8)
    lay = Image.new("RGBA", (2 * R * s, 2 * R * s), (0, 0, 0, 0))
    d = ImageDraw.Draw(lay)
    c = R * s
    th = np.linspace(0, TAU, 120, endpoint=True)
    wob = 1.4 * np.sin(3 * th + seed) + 0.9 * np.sin(5 * th + seed * 1.7) + 0.5 * np.sin(9 * th)
    brk = np.clip(0.45 + 0.55 * np.sin(4 * th + seed * 2.3) * np.sin(7 * th - seed), 0, 1)
    for rr_off, col, k in ((2.4, (0, 18, 40), 0.45), (0.0, (170, 215, 230), 0.6)):
        for i in range(len(th) - 1):
            a = alpha * k * brk[i]
            if a < 0.03:
                continue
            p0 = (c + np.cos(th[i]) * (r + wob[i] + rr_off) * s,
                  c + np.sin(th[i]) * (r + wob[i] + rr_off) * s)
            p1 = (c + np.cos(th[i + 1]) * (r + wob[i + 1] + rr_off) * s,
                  c + np.sin(th[i + 1]) * (r + wob[i + 1] + rr_off) * s)
            d.line([p0, p1], fill=col + (int(255 * a),), width=int(1.3 * s))
    lay = lay.resize((2 * R, 2 * R), Image.BOX)
    img.alpha_composite(lay, (int(round(cx - R)), int(round(cy - R))))


def jelly_pos(j, t, scroll):
    x = j["x"] + 12 * np.sin(t * 0.7 + j["ph"])
    y = (j["y"] + scroll * t) % (FH + 80) - 40
    return x, y


def jelly_pulse(j, t):
    return round(0.5 + 0.5 * np.sin(t * PULSE_W + j["ph"]), 1)


def draw_surfaced(img, j, t, scroll, seed):
    """Ripple trains (emitted at each bell contraction, centred where the jelly was, carried by
    the scrolling water), the submerged part through the water, the foam collar, the dome."""
    x, y = jelly_pos(j, t, scroll)
    period = TAU / PULSE_W
    n0 = int(np.floor((t * PULSE_W + j["ph"] - np.pi / 2) / TAU))
    for n in range(n0 - 1, n0 + 1):
        te0 = (np.pi / 2 - j["ph"] + TAU * n) / PULSE_W
        for k, off in enumerate(TRAIN):
            te = te0 + off
            age = t - te
            if 0 <= age < RIPPLE_LIFE:
                rx, _ = jelly_pos(j, te, scroll)
                r = 12 + RIPPLE_SPEED * age
                ripple(img, rx, y, r, (1 - age / RIPPLE_LIFE) ** 1.5 * (1 - 0.25 * k),
                       seed + 3 * n + k)
    pulse = jelly_pulse(j, t)
    above = jly_above.get(0.0, pulse=pulse)
    paste(img, through_water(jly_below.get(0.0, pulse=pulse), t + seed), x, y + 1)
    paste(img, above, x, y)
    foam_collar(img, x, y, waterline_radius(above), t, seed)
    return x, y, period


def driftjelly():
    bg = e6.ocean_bg(121)
    rng = np.random.default_rng(12)
    jellies = [dict(x=x, y=y, sub=s, ph=rng.uniform(0, 3), fired=None)
               for x, y, s in ((90, 120, False), (300, 60, True), (200, 250, False),
                               (390, 230, True), (120, 360, True), (330, 380, False),
                               (240, 470, True))]
    scroll = 30.0
    frames = []
    for t in r5.times(8.0):
        f = bg.copy()
        px, py = r5.player_at(t)
        for i, j in enumerate(jellies):
            if j["sub"]:
                x, y = jelly_pos(j, t, scroll)
                paste(f, wobble(e6.submerged(e6.jly.get(0.0, pulse=jelly_pulse(j, t))), t + i,
                                amp=0.5), x, y)
            else:
                x, y, _ = draw_surfaced(f, j, t, scroll, 17 * i + 5)
            near = np.hypot(px - x, py - y) < 96
            if near and j["fired"] is None:
                j["fired"] = (t, x, y)
            if j["fired"] is not None:
                s = t - j["fired"][0]
                if s < 1.4:
                    if s < 0.25:
                        raster.add_light(f, x, y, 30, (168, 255, 42), 1 - s * 4)
                    e6.ring_shots(f, j["fired"][1], j["fired"][2], 12 + 150 * s, 10, 0.2)
                elif s > 3.0:
                    j["fired"] = None
        r5.draw_player(f, t)
        frames.append(f)

    # sheet panels
    water = e6.ocean_bg(5)
    def cell(img_fn, w=56, h=56):
        c = water.crop((100, 100, 100 + w, 100 + h)).copy()
        img_fn(c, w / 2, h / 2)
        return c
    above0, below0 = jly_above.get(0.0, pulse=0.3), jly_below.get(0.0, pulse=0.3)
    split = [cell(lambda c, x, y: paste(c, above0, x, y)),
             cell(lambda c, x, y: paste(c, through_water(below0, 0.0), x, y + 1)),
             cell(lambda c, x, y: (paste(c, through_water(below0, 0.0), x, y + 1),
                                   paste(c, above0, x, y),
                                   foam_collar(c, x, y, waterline_radius(above0), 0.0, 5)))]
    demo = dict(x=60, y=60, ph=0.4)
    seq = []
    for t in np.arange(0.0, 1.8, 0.3):
        c = water.crop((40, 40, 160, 160)).copy()
        draw_surfaced(c, demo, float(t), 0.0, 5)
        seq.append(c)
    layers = Image.new("RGBA", (200, 90), (22, 24, 34, 255))
    layers.alpha_composite(water.crop((0, 0, 200, 90)))
    draw_surfaced(layers, dict(x=50, y=45, ph=0.4), 0.6, 0.0, 9)
    paste(layers, wobble(e6.submerged(e6.jly.get(0.0, pulse=0.3)), 0.0, amp=0.5), 150, 45)
    zone = Image.new("RGBA", (260, 250), (22, 24, 34, 255))
    zd = ImageDraw.Draw(zone)
    for a in np.arange(0, TAU, TAU / 48)[::2]:          # dashed: a diagram, not drawn in game
        zd.arc([120 - 96, 120 - 96, 120 + 96, 120 + 96], np.degrees(a), np.degrees(a + TAU / 48),
               fill=(168, 255, 42, 255))
    paste(zone, e6.jly.get(0.0, pulse=0.0), 120, 120)
    raster.draw_text(zone, 8, 226, "TRIGGER RADIUS 96 PX", raster.LABEL_DIM)
    raster.draw_text(zone, 8, 238, "(DIAGRAM ONLY, NOT IN GAME)", raster.LABEL_DIM)
    panels = [("SOURCE, 260 PX", source_panel(lambda: m6.driftjelly(0.3))),
              ("WATERLINE (3X): ABOVE / BELOW THROUGH WATER / COMPOSITE + FOAM",
               grid(split, 3, 3)),
              ("FOAM COLLAR + RIPPLE TRAIN EVERY 0.3 S (2X)", grid(seq, 6, 2)),
              ("SURFACED / SUBMERGED (1X)       ", layers),
              ("RING PULSE TRIGGER", zone)]
    notes = ["DRIFTJELLY (R07) - A REAL WATERLINE INSTEAD OF A DRAWN CIRCLE",
             "THE MODEL IS CUT AT THE WATER PLANE: THE DOME ABOVE IT IS DRAWN NORMALLY; THE LOWER",
             "BELL AND TENTACLES BELOW IT ARE SEEN THROUGH THE WATER (TINTED, DARKER, SOFT, SWAYING).",
             "A BROKEN, WOBBLING FOAM COLLAR SITS ON THE WATERLINE AND SHEDS SPECKS; EACH BELL",
             "CONTRACTION SENDS OUT A TRAIN OF THREE UNEVEN RIPPLES (LIGHT CREST, DARK TROUGH) THAT",
             "STAY WHERE THEY WERE MADE AS THE JELLY DRIFTS ON. SUBMERGED JELLIES: SUB LAYER, NO FOAM."]
    return r5.build_sheet("DRIFTJELLY - FLOATING MINE AT THE WATERLINE", panels, notes,
                          frames[70]), frames, r5.FPS


UNITS = {"halo-platform": ("ground", halo_platform), "driftjelly": ("naval", driftjelly)}


def main(args):
    for name in (args or list(UNITS)):
        cat, fn = UNITS[name]
        t0 = time.time()
        sheet, frames, fps = fn()
        save_unit(cat, name, sheet, frames, fps)
        print(f"{name}: {time.time() - t0:.0f} s")


if __name__ == "__main__":
    main(sys.argv[1:])
