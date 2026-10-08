#!/usr/bin/env python3
"""Production art, two variants: the look of Level 10's scripted loss, the light above the clouds
and the lance that strikes Lifeline Three (design/campaign/act-2-homefront/
level-10-evacuation-corridor, Scripted loss; M5 part D batch, concept round 32). Straight to
production with an a/b pick (user decision D10 = a); the user picked **b** (round 32, 2026-10-08),
so b is in the game and a's review files live in concept/rejected/.

  a  "Thorn spear": a soft violet light swells in the cloud deck just ahead of the shuttle, shafts
     and silver-lined cloud lumps round a pink-white core; at the hit a thin violet spear with
     backward thorns drops from the light onto the shuttle and a white core bursts into a thin
     violet ring with eight thorn spikes
  b  "Iris column": a teal vortex opens in the cloud deck straight above the shuttle, three spiral
     arms turning round a violet ring and a dark pupil; at the hit a broad column of teal-white
     light lands on the shuttle, collapses to a white point and rings out in rippling teal and
     violet shock rings

Outputs (the chosen variant, PRODUCTION = b, under the names the game loads; `--variant a` would
write a's under the same names instead; the other variant lives only in its review files, so the
atlas never carries an unclaimed second set). Every file is additive (premultiplied on black, alpha 1 where it
adds light, blended GL_SRC_ALPHA, GL_ONE), one palette per set:
  a: assets/sprites/cloud-glow_0..5   128x128, the light in the cloud deck, a 6-frame churn loop
                                      (10 fps); its centre GLOW_DY = 72 px above the shuttle
     assets/sprites/lance_0..5        24x88, the spear (4 steps a frame, 0.4 s): its top centre
                                      (12, 0) on the glow's centre, its tip (12, 72) on the shuttle
     assets/sprites/lance-flash_0..7  96x96, the ring flash centred on the hit (3 steps, 0.4 s)
  b: assets/sprites/cloud-glow_0..5   128x128, the iris vortex, a 6-frame loop (10 fps, a third of
                                      a turn each loop); centred on the shuttle (GLOW_DY = 0)
     assets/sprites/lance_0..11       128x128, the column, the collapse and the shock rings in one
                                      set centred on the hit (3 steps, 0.6 s)
                                      (b has no lance-flash: its rings are in the lance frames)
  design/campaign/act-2-homefront/level-10-evacuation-corridor/concept/loss-r32-b.png/.gif
                                      the review sheet and the scene in play (the chosen b; the
                                      rejected a's into concept/rejected/loss-r32-a.png/.gif)

How the game draws it (LossLooks, D6), the data's `scripted_loss {unit: 3, t: 118, glow: 2}`:
  LOSS_GLOW (t 116)   cloud-glow at 10 fps, additive, under the air layer (after low-air, before
                      the shuttles and bullets, so nothing that matters is covered), centred
                      GLOW_DY px above Lifeline Three's current position (it follows the sway); its
                      strength is the batch alpha intensity(tau) below, tau = t - 116 (b also scales
                      the sprite 0.55 -> 1.0 with smoothstep over the 2 s: an unlit additive effect
                      may be scaled)
  SCRIPTED_LOSS (118) the lance frames from the hit, additive, ABOVE the air layer, anchored where
                      the shuttle is at the hit (screen space: they do not follow the wreck); a's
                      lance-flash starts with it; Lifeline Three switches to its -wreck glide at once;
                      the glow fades 1 -> 0 over 0.8 s (ease out) at the hit position
  the lance sound     starts 1.2 s before the hit (t 116.8), inside the glow: its impact lands at 118
  intensity(tau)      env * (0.6 + 0.4 cos(2 pi phi)), env = 0.25 + 0.75 smoothstep(0, 1.6, tau),
                      phi = 1.25 tau + 0.625 tau^2: five pulses quickening from 1.25 to 3.75 Hz,
                      the last peak exactly at the hit

The review plays the scene on a dawn stand-in (wraith.dawn_plate: Level 10's backdrop was not built
yet) with a stand-in low-air smoke layer, the production shuttle a (tools/art/shuttle.py: its bank,
flame and -wreck frames, the wreck-smoke pivot) on the five stations' lane sway, and a timeline strip
(the glow at 116, the lance sound's start at 116.8, the hit at 118, the glide's end at 121).

Packing: SpriteUse claims `lance` and `cloud-glow` (LOSS_ROOTS) for a level with a `scripted_loss`
(as `collapse-puff` for a collapse), so they go into Level 10's atlas.

Run: python3 tools/art/lance_l10.py [--variant a|b] [--out DIR] [--review]   (~1 min; --out writes
the frames into DIR instead of assets/sprites; --review only rebuilds the review files, reading the
chosen variant's frames back from DIR)
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image

import artkit
from artkit import DESIGN, ROOT, TAU, sprite

import shuttle as sh  # noqa: E402  (tools/art: the production shuttle a and its draw helper)
import vrell_fx as fx  # noqa: E402  (tools/art: timing)
import wraith  # noqa: E402  (tools/art: the dawn stand-in plate)
from render import raster  # noqa: E402
from render.enemy_rigs import write_gif  # noqa: E402

# =========================================================================== parameters

SCRIPT = "lance_l10.py"
BATCH = "M5 part D batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
REVIEW_ROUND = "r32"
LEVEL = DESIGN / "campaign" / "act-2-homefront" / "level-10-evacuation-corridor"
CONCEPT = LEVEL / "concept"
PRODUCTION = "b"                                # the user's pick, round 32 (2026-10-08)
STEP = 60
SS = 4                                          # supersampling of the light fields

HIT_T = 118.0
GLOW_S = 2.0
SOUND_LEAD = 1.2                                # the lance sound's impact, seconds into its file
FADE_S = 0.8                                    # the glow's fade after the hit

GLOW_SIZE = (128, 128)
GLOW_FRAMES = 6
GLOW_FPS = 10
GLOW_COLOURS = 40

# per variant: glow offset above the shuttle, lance size, lance frames and steps, flash
VARIANTS = {
    "a": {"title": "THORN SPEAR", "glow_dy": 72, "lance": (24, 88), "lance_frames": 6, "lance_steps": 4,
          "flash": (96, 96), "flash_frames": 8, "flash_steps": 3, "colours": 32},
    "b": {"title": "IRIS COLUMN", "glow_dy": 0, "lance": (128, 128), "lance_frames": 12, "lance_steps": 3,
          "flash": None, "flash_frames": 0, "flash_steps": 0, "colours": 40},
}

VIOLET = np.array([0.60, 0.30, 1.00])           # Vrell glow 9a4dff
DEEP = np.array([0.40, 0.08, 0.80])             # c000ff, darkened
PINK = np.array([0.94, 0.63, 1.00])             # f0a0ff
WHITE_V = np.array([0.95, 0.90, 1.00])
TEAL = np.array([0.00, 1.00, 0.60])             # 00ff9a
TEAL_PALE = np.array([0.62, 1.00, 0.86])


# =========================================================================== light-field helpers

def grid(w, h):
    """Native-pixel coordinates of the supersampled pixel centres."""
    ys, xs = np.mgrid[0:h * SS, 0:w * SS]
    return (xs + 0.5) / SS, (ys + 0.5) / SS


def gauss(d, w):
    return np.exp(-(d / np.maximum(w, 1e-6)) ** 2)


def smooth(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)


def light(*terms):
    """Sum of (colour, amount-field) terms -> supersampled rgb light."""
    out = 0
    for colour, amount in terms:
        out = out + np.asarray(colour)[None, None, :] * np.clip(amount, 0, None)[..., None]
    return out


def to_additive(rgb, w, h):
    """Supersampled light -> native additive frame (premultiplied, alpha 1 where it adds light)."""
    native = np.clip(rgb.reshape(h, SS, w, SS, 3).mean(axis=(1, 3)), 0, 1)
    out = np.round(native * 255)
    alpha = np.where(out.max(axis=-1) >= 3, 255, 0)
    out[alpha == 0] = 0
    return Image.fromarray(np.dstack([out, alpha]).astype(np.uint8), "RGBA")


def periodic_noise(size, cell, seed, octaves=4):
    """Tileable fBm (0..1) on a square supersampled grid."""
    return raster.fbm(size, size, cell, seed, octaves=octaves, period=True)


def shifted(noise, dx, dy):
    return np.roll(np.roll(noise, int(round(dy)), axis=0), int(round(dx)), axis=1)


def segment_distance(x, y, x0, y0, x1, y1):
    px, py, bx, by = x - x0, y - y0, x1 - x0, y1 - y0
    t = np.clip((px * bx + py * by) / max(bx * bx + by * by, 1e-9), 0, 1)
    return np.hypot(px - bx * t, py - by * t), t


# =========================================================================== variant a

def glow_a(i):
    """The light swelling behind the cloud deck: a violet bloom, a pink-white core, cloud lumps
    that occlude it with silver edges, faint shafts; the cloud churns round a small circle so the
    six frames loop."""
    w, h = GLOW_SIZE
    x, y = grid(w, h)
    cx, cy = w / 2, h / 2
    d = np.hypot(x - cx, y - cy)
    th = np.arctan2(y - cy, x - cx)
    phase = TAU * i / GLOW_FRAMES
    n1 = shifted(periodic_noise(w * SS, 32 * SS, 1031), 3 * SS * np.cos(phase), 3 * SS * np.sin(phase))
    n2 = shifted(periodic_noise(w * SS, 12 * SS, 2063), -4 * SS * np.cos(phase), 4 * SS * np.sin(phase))
    cloud = 0.62 * n1 + 0.38 * n2
    dens = smooth(0.42, 0.78, cloud)
    silver = np.clip(4 * dens * (1 - dens), 0, 1) * smooth(0.3, 0.6, cloud)
    rays = sum(np.cos(k * th + s + 0.35 * np.sin(phase + k)) for k, s in ((5, 0.4), (9, 1.9), (14, 3.1)))
    rays = np.clip(0.5 + rays / 4.5, 0, 1) ** 3
    window = smooth(63, 46, d)
    rgb = light(
        (DEEP, 0.30 * gauss(d, 52)),
        (VIOLET, 0.62 * gauss(d, 30) * (1 - 0.6 * dens)),
        (PINK, 0.42 * silver * gauss(d, 38)),
        (VIOLET, 0.20 * rays * gauss(d, 50) * smooth(6, 18, d)),
        (WHITE_V, 0.80 * gauss(d, 8.5) * (1 - 0.35 * dens)),
        (PINK, 0.30 * gauss(d, 15)))
    return to_additive(rgb * window[..., None], w, h)


def lance_a(k):
    """The thorn spear from the glow's centre (top) to the shuttle (y = 72): thickest at its root,
    a needle tip, thorns pointing back up along the shaft. Frame 0 strikes, 1-2 burn, 3-5 break up."""
    v = VARIANTS["a"]
    w, h = v["lance"]
    x, y = grid(w, h)
    cx, tip = w / 2, 72.0
    core, halo, barbs, gain, broken = [(1.0, 3.0, 0.5, 1.0, 0), (1.6, 5.0, 1.0, 1.0, 0), (1.3, 5.5, 1.2, 0.95, 0),
                                       (0.4, 4.5, 0.8, 0.6, 1), (0.0, 3.5, 0.4, 0.35, 2), (0.0, 2.5, 0.0, 0.18, 3)][k]
    rng = np.random.default_rng(700 + k)
    along = np.clip(y / tip, 0, 1)
    taper = np.where(y <= tip, 1.0 - 0.8 * along ** 1.4, 0.0)
    dx = np.abs(x - cx - 0.5 * np.sin(y * 0.21 + k * 1.7) * (k >= 2))
    inside = (y >= 0) & (y <= tip + 1.5)
    crackle = 0.88 + 0.12 * np.sin(y * 0.23 + k * 2.3)
    halo_shaft = inside * crackle
    if broken:                                               # the core breaks into glowing segments
        crackle = crackle * smooth(-0.2, 0.6, np.sin(y * (0.31 + 0.04 * broken) + 1.3 * k) - 0.3 * broken)
    shaft = inside * crackle
    rgb = light(
        (DEEP, gain * 0.55 * gauss(dx, halo * 1.6 * (0.5 + taper)) * halo_shaft),
        (VIOLET, gain * 0.85 * gauss(dx, halo * 0.6 * (0.4 + taper)) * shaft),
        (PINK, gain * 0.9 * gauss(dx, max(core, 0.3) * taper + 0.25) * shaft * (core > 0)),
        (WHITE_V, gain * 1.1 * gauss(dx, max(core, 0.3) * 0.55 * taper + 0.1) * shaft * (core > 0)))
    for j, ty in enumerate((12, 23, 34, 45, 55, 63)):        # backward thorns, alternating sides
        side = 1 if j % 2 else -1
        if broken and rng.random() < 0.4 * broken:
            continue
        lift = 2.0 * broken
        length = 6.5 * (1 - 0.6 * ty / tip)
        dist, t = segment_distance(x, y, cx + side * 0.8, ty - lift, cx + side * length, ty - 7 - lift)
        rgb = rgb + light((PINK, barbs * gain * 0.9 * gauss(dist, 0.75) * (1 - 0.5 * t)),
                          (VIOLET, barbs * gain * 0.5 * gauss(dist, 1.8)))
    if k == 0:                                               # the strike's head at the tip
        rgb = rgb + light((WHITE_V, 1.2 * gauss(np.hypot(x - cx, y - tip), 2.6)),
                          (VIOLET, 0.6 * gauss(np.hypot(x - cx, (y - tip) * 0.6), 6)))
    if broken:                                               # motes shed from the shaft
        for _ in range(10):
            mx, my = cx + rng.normal(0, 3 + broken), rng.uniform(4, tip)
            rgb = rgb + light((PINK, 0.7 * gain * gauss(np.hypot(x - mx, y - my), 0.8)))
    return to_additive(rgb, w, h)


def flash_a(k):
    """The ring flash on the hit: a white-violet core, a thin violet ring with a pink inner edge
    running out to 44 px, eight thorn spikes (long and short) and motes."""
    v = VARIANTS["a"]
    w, h = v["flash"]
    n = v["flash_frames"]
    x, y = grid(w, h)
    cx, cy = w / 2, h / 2
    d = np.hypot(x - cx, y - cy)
    u = k / (n - 1)
    grow = fx.ease_out(u, 2.5)
    fade = (1 - u) ** 1.3
    radius = 9 + 35 * grow
    rgb = light(
        (WHITE_V, 1.15 * (1 - u) ** 2 * gauss(d, 9 * (1 - u) + 2.5)),
        (VIOLET, 0.6 * fade * gauss(d, 14 + 6 * u)),
        (VIOLET, 0.95 * fade * gauss(np.abs(d - radius), 1.4 + 1.2 * u)),
        (PINK, 0.7 * fade * gauss(np.abs(d - radius + 2.2), 0.9 + 0.6 * u)))
    for s in range(8):                                       # thorn spikes
        a = TAU * (s + 0.5) / 8 + 0.12
        reach = (18 if s % 2 else 30) * (0.45 + 0.55 * grow)
        inner = 4 + 10 * u
        dist, t = segment_distance(x, y, cx + inner * np.cos(a), cy + inner * np.sin(a),
                                   cx + reach * np.cos(a), cy + reach * np.sin(a))
        width = 1.1 * (1 - 0.7 * t)
        rgb = rgb + light((PINK, 0.9 * fade * gauss(dist, width)), (WHITE_V, 0.6 * fade * (1 - u) * gauss(dist, width * 0.45)))
    rng = np.random.default_rng(91)
    for _ in range(14):                                      # motes flung out
        a, sp = rng.uniform(0, TAU), rng.uniform(18, 40)
        r = 6 + sp * grow
        rgb = rgb + light((PINK, 0.8 * fade * gauss(np.hypot(x - cx - r * np.cos(a), y - cy - r * np.sin(a)), 0.8)))
    return to_additive(rgb * smooth(47, 40, d)[..., None], w, h)


# =========================================================================== variant b

def glow_b(i):
    """The iris vortex opening in the cloud deck straight above the shuttle: three logarithmic
    spiral arms of teal light through churning cloud round a violet ring and a dim violet pupil;
    the six frames turn it a third of a turn (the arms' period), so they loop."""
    w, h = GLOW_SIZE
    x, y = grid(w, h)
    cx, cy = w / 2, h / 2
    d = np.hypot(x - cx, y - cy)
    rho = (TAU / 3) * i / GLOW_FRAMES
    th = np.arctan2(y - cy, x - cx) - rho
    arm = (0.5 + 0.5 * np.cos(3 * th + 2.4 * np.log(np.maximum(d, 1) / 12))) ** 3
    # cloud texture in the vortex's own polar frame, turning with the arms (periodic in angle)
    cols = 384
    tex = raster.fbm(cols, 96, 24, 4111, octaves=4, period=True)
    ti = (np.mod(3 * th / TAU, 1) * cols).astype(int) % cols
    ri = np.clip((np.log(np.maximum(d, 1)) * 22).astype(int), 0, 95)
    cloud = tex[ri, ti]
    spiral_amount = arm * (0.45 + 0.75 * cloud) * smooth(28, 36, d) * gauss(d - 34, 26)
    ring = gauss(np.abs(d - 30), 2.6)
    window = smooth(63, 44, d)
    rgb = light(
        (DEEP, 0.32 * gauss(d, 46)),
        (TEAL, 0.85 * spiral_amount),
        (TEAL_PALE, 0.4 * spiral_amount * smooth(0.55, 0.9, cloud) * gauss(d - 32, 14)),
        (VIOLET, 0.9 * ring * (0.8 + 0.2 * np.cos(6 * th))),
        (WHITE_V, 0.35 * gauss(np.abs(d - 29), 1.0)),
        (VIOLET, 0.3 * gauss(d, 20)))
    return to_additive(rgb * window[..., None], w, h)


def lance_b(k):
    """The column, its collapse and the shock rings: frames 0-2 a broad disc of teal-white light
    with violet walls and streaks converging into it (the column seen from above), narrowing
    30 -> 14 px; 3 the white point; 4-11 three rippling rings (teal leading, violet trailing)
    running out to 60 px, a violet ember on the hit and teal motes."""
    v = VARIANTS["b"]
    w, h = v["lance"]
    n = v["lance_frames"]
    x, y = grid(w, h)
    cx, cy = w / 2, h / 2
    d = np.hypot(x - cx, y - cy)
    th = np.arctan2(y - cy, x - cx)
    rgb = 0
    if k <= 2:
        rc = (30, 23, 14)[k]
        disc = smooth(rc + 1.0, rc - 2.5, d)
        rgb = light(
            (TEAL_PALE, (0.2 + 0.15 * k) * disc),
            (TEAL, 0.5 * disc * smooth(rc * 0.5, rc, d)),
            (WHITE_V, (0.25 + 0.35 * k) * gauss(d, rc * 0.35)),
            (TEAL_PALE, 0.9 * gauss(np.abs(d - rc + 1.5), 1.4)),
            (VIOLET, 1.0 * gauss(np.abs(d - rc - 1.5), 2.0)),
            (DEEP, 0.45 * gauss(d, rc + 16)))
        for s in range(18):                                  # converging wall streaks
            a = TAU * s / 18 + 0.17 * (s % 3)
            r0 = rc + 3 + (2 - k) * 4 + 3 * (s % 2)
            r1 = r0 + 18 - 4 * k
            dist, t = segment_distance(x, y, cx + r0 * np.cos(a), cy + r0 * np.sin(a),
                                       cx + r1 * np.cos(a), cy + r1 * np.sin(a))
            rgb = rgb + light((TEAL, 0.55 * gauss(dist, 0.9) * (1 - t)), (VIOLET, 0.25 * gauss(dist, 2.2) * (1 - t)))
    elif k == 3:
        rgb = light((WHITE_V, 1.25 * gauss(d, 10)), (TEAL_PALE, 0.7 * gauss(d, 20)),
                    (VIOLET, 0.8 * gauss(np.abs(d - 20), 2.0)), (DEEP, 0.4 * gauss(d, 36)))
    else:
        u = (k - 4) / (n - 5)
        grow = fx.ease_out(u, 2.0)
        fade = (1 - u) ** 1.25
        lead = 22 + 40 * grow
        rgb = light((VIOLET, 0.55 * fade * gauss(d, 9)), (WHITE_V, 0.5 * (1 - u) ** 3 * gauss(d, 5)),
                    (DEEP, 0.3 * fade * gauss(d, 30)))
        for j, (off, col, amp) in enumerate(((0, TEAL_PALE, 0.95), (13, TEAL, 0.7), (25, VIOLET, 0.6))):
            r = lead - off * (0.7 + 0.3 * grow)
            if r <= 4:
                continue
            wobble = (1.6 + 1.4 * u) * np.sin(7 * th + 2.1 * k + j * 1.3)
            ring = gauss(np.abs(d - r - wobble), 1.3 + 1.6 * u + 0.4 * j)
            rgb = rgb + light((col, amp * fade * ring), (VIOLET, 0.25 * fade * ring * (j == 0)))
        rng = np.random.default_rng(311)
        for _ in range(16):                                  # teal motes riding the rings
            a, sp = rng.uniform(0, TAU), rng.uniform(0.5, 1.0)
            r = lead * sp
            rgb = rgb + light((TEAL_PALE, 0.8 * fade * gauss(np.hypot(x - cx - r * np.cos(a), y - cy - r * np.sin(a)), 0.8)))
    return to_additive(rgb * smooth(63, 54, d)[..., None], w, h)


# =========================================================================== build

def frame_sets(variant):
    v = VARIANTS[variant]
    glow = [(glow_a if variant == "a" else glow_b)(i) for i in range(GLOW_FRAMES)]
    sets = {"cloud-glow": artkit.quantize_set(glow, GLOW_COLOURS)}
    if variant == "a":
        lance = [lance_a(k) for k in range(v["lance_frames"])]
        flash = [flash_a(k) for k in range(v["flash_frames"])]
        both = artkit.quantize_set(lance + flash, v["colours"])
        sets["lance"], sets["lance-flash"] = both[:len(lance)], both[len(lance):]
    else:
        sets["lance"] = artkit.quantize_set([lance_b(k) for k in range(v["lance_frames"])], v["colours"])
    return sets


def write(sets, variant, out):
    """The frame sets into ``out`` (assets/sprites by default), replacing a previous variant's."""
    old_root = artkit.SPRITES
    artkit.SPRITES = Path(out)
    artkit.SPRITES.mkdir(parents=True, exist_ok=True)
    try:
        for name in ("cloud-glow", "lance", "lance-flash"):
            if name not in sets:                             # b has no flash: drop a's
                for p in artkit.SPRITES.glob(f"{name}_*.png"):
                    p.unlink()
                continue
            artkit.write_frames(name, sets[name], SOURCE + f", variant {variant}")
            print(f"{name}: {len(sets[name])} frames, {sets[name][0].width}x{sets[name][0].height} -> {artkit.SPRITES}")
    finally:
        artkit.SPRITES = old_root


# =========================================================================== timing (the game's)

def intensity(tau):
    """The glow's strength (batch alpha) tau s after LOSS_GLOW; the fade after the hit."""
    if tau < 0:
        return 0.0
    if tau <= GLOW_S:
        env = 0.25 + 0.75 * float(smooth(0, 1.6, tau))
        phi = 1.25 * tau + 0.625 * tau * tau
        return env * (0.6 + 0.4 * np.cos(TAU * phi))
    return max(0.0, 1 - fx.ease_out((tau - GLOW_S) / FADE_S, 2.0))


def glow_scale(variant, tau):
    if variant == "b":
        return 0.55 + 0.45 * float(smooth(0, GLOW_S, tau))
    return 1.0


# =========================================================================== review

STATIONS = [(240, 165, 9, 0.0), (168, 215, 11, 0.25), (312, 215, 10, 0.5), (168, 270, 12, 0.75), (312, 270, 8, 0.1)]
LOST = 2                                        # Lifeline Three
FIELD = (480, 540)
CROP = (70, 40, 410, 380)                       # the GIF's window on the play field (340 x 340, shown 2x)
T0, T1, FPS = 115.5, 121.3, 20


def station(n, t):
    sx, sy, period, phase = STATIONS[n]
    th = TAU * (t / period + phase)
    return sx + 16 * np.sin(th), sy + 4 * np.sin(2 * th)


def ground_plate():
    """The dawn stand-in (Level 10's backdrop is not built yet), the 960 px tile twice."""
    plate = wraith.dawn_plate(480, 960, 0, 0)
    tall = Image.new("RGBA", (480, 1920))
    tall.paste(plate, (0, 0))
    tall.paste(plate, (0, 960))
    return tall


def smoke_layer(t):
    """Section 3's low-air smoke as a stand-in: soft grey-violet banks at 1.35 x the scroll."""
    w, h = FIELD
    if not hasattr(smoke_layer, "tex"):
        tex = raster.fbm(480, 480, 96, 517, octaves=4, period=True)
        smoke_layer.tex = smooth(0.55, 0.85, tex)
    off = int(t * 190 * 1.35) % 480
    a = np.roll(smoke_layer.tex, off, axis=0)
    a = np.vstack([a, a])[:h, :w]
    img = np.zeros((h, w, 4), np.uint8)
    img[..., :3] = (150, 146, 168)
    img[..., 3] = (a * 0.55 * 255).astype(np.uint8)
    return Image.fromarray(img, "RGBA")


def gained(img, gain):
    a = np.array(img).astype(np.float64)
    a[..., :3] *= max(0.0, gain)
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


def scaled(img, s):
    if abs(s - 1) < 1e-3:
        return img
    return img.resize((max(1, round(img.width * s)), max(1, round(img.height * s))), Image.BILINEAR)


def scene(variant, sets, ssets, pv, t, plate, extras):
    """The play field at time t: ground, low-air smoke, the wreck under the air layer, the glow,
    the band of five shuttles, Rook and the ship, then the lance and the flash."""
    v = VARIANTS[variant]
    w, h = FIELD
    scroll = int(t * 190) % 960
    field = plate.crop((0, 1920 - h - scroll, w, 1920 - scroll))
    field.alpha_composite(smoke_layer(t))
    hit_xy = station(LOST, HIT_T)
    # the wreck's glide and smoke (under the air layer)
    if t >= HIT_T:
        age = (t - HIT_T) * STEP
        u = min(1.0, (t - HIT_T) / 3.0)
        if u < 1.0:
            step = min(sh.WRECK_STEPS - 1, int(age / 22.5))
            wx, wy = hit_xy[0] - 18 * u, hit_xy[1] + 142.5 * u * u
            puffs = extras.setdefault("puffs", [])
            last = extras.get("last_puff", -99)
            if age - last >= 6:
                sx, sy = pv["wreck-smoke"][step]
                puffs.append((t, wx - sh.SIZE[0] / 2 + sx, wy - sh.SIZE[1] / 2 + sy))
                extras["last_puff"] = age
            field = sh.draw_shuttle(field, ssets, pv, f"{sh.NAME}-wreck", step, wx, wy,
                                    shadow=(21 * (1 - u), 30 * (1 - u)), shadow_alpha=0.5 * max(0.0, 1 - step / 5))
    smoke = extras.setdefault("smoke", artkit.load_frames("ship-smoke"))
    for born, sx, sy in extras.get("puffs", []):
        fr = fx.timed(smoke, 6, int((t - born) * STEP))
        if fr is not None:
            field.alpha_composite(fr, (int(sx - fr.width / 2), int(sy + (t - born) * 95 - fr.height / 2)))
    # the glow (under the air layer)
    tau = t - (HIT_T - GLOW_S)
    gi = intensity(tau)
    if gi > 0.01:
        gx, gy = station(LOST, min(t, HIT_T))
        glow = scaled(sets["cloud-glow"][int(t * GLOW_FPS) % GLOW_FRAMES], glow_scale(variant, tau))
        field = wraith.add_centred(field, glow, gx, gy - v["glow_dy"], gain=gi)
    # the air layer: the band, Rook and the ship
    for n in range(5):
        if n == LOST and t >= HIT_T:
            continue
        x, y = station(n, t)
        field = sh.draw_shuttle(field, ssets, pv, sh.NAME, sh.BANKS.index(0), x, y, flame=int(t * 20) + n)
    ship, rook = extras.setdefault("ship", artkit.load_frames("ship")), extras.setdefault("rook", artkit.load_frames("rook"))
    for frames, (x, y) in ((ship, (250, 430)), (rook, (190, 452))):
        f = frames[len(frames) // 2]
        wraith.draw_body(field, f, x, y)
    # the lance and the flash (above the air layer)
    age = int(round((t - HIT_T) * STEP))
    if age >= 0:
        lance = fx.timed(sets["lance"], v["lance_steps"], age)
        if lance is not None:
            if variant == "a":
                field = artkit.add_light(field, lance, (int(round(hit_xy[0] - lance.width / 2)),
                                                        int(round(hit_xy[1] - 72))))
            else:
                field = wraith.add_centred(field, lance, *hit_xy)
        if "lance-flash" in sets:
            flash = fx.timed(sets["lance-flash"], v["flash_steps"], age)
            if flash is not None:
                field = wraith.add_centred(field, flash, *hit_xy)
    return field


def caption(img, t, scale=2):
    """The timeline under the scene: the glow, the sound's start, the hit, the radio line."""
    events = [(HIT_T - GLOW_S, "GLOW"), (HIT_T - SOUND_LEAD, "SOUND"), (HIT_T, "HIT"), (HIT_T + 3, "END")]
    bar = Image.new("RGBA", (img.width, 34 * scale // 2), (12, 12, 22, 255))
    raster.draw_text(bar, 8, 4, f"T = {t:6.2f} S", raster.LABEL, scale=scale)
    if HIT_T - GLOW_S <= t < HIT_T + 0.2:
        raster.draw_text(bar, 170, 4, "LIFELINE THREE: THERE'S A LIGHT ABOVE THE CLOUDS - WHAT IS THAT?"[:int((t - 116) * 30) + 1],
                         (220, 200, 255), scale=1)
    if t >= HIT_T + 0.2:
        raster.draw_text(bar, 170, 4, "LIFELINE THREE LOST (SCRIPTED)", (255, 150, 150), scale=1)
    x0, x1, y = 170, img.width - 16, 20
    for tt, name in events:
        px = x0 + (tt - T0) / (T1 - T0) * (x1 - x0)
        bar.paste((90, 90, 120, 255), (int(px), y - 2, int(px) + 1, y + 6))
        raster.draw_text(bar, int(px) + 3, y - 2, name, raster.LABEL_DIM)
    px = x0 + (t - T0) / (T1 - T0) * (x1 - x0)
    bar.paste((255, 255, 255, 255), (int(px) - 1, y + 7, int(px) + 2, y + 10))
    out = Image.new("RGBA", (img.width, img.height + bar.height))
    out.paste(img, (0, 0))
    out.paste(bar, (0, img.height))
    return out


def review(variant, sets):
    v = VARIANTS[variant]
    ssets, pv = sh.load_sets(), sh.pivots(sh.PRODUCTION)
    plate = ground_plate()
    extras = {}
    gif, stills = [], {}
    keys = [116.0, 116.9, 117.6, 117.95, 118.0, 118.07, 118.2, 118.4, 119.0, 120.5]
    for f in range(int((T1 - T0) * FPS)):
        t = T0 + f / FPS
        field = scene(variant, sets, ssets, pv, t, plate, extras)
        gif.append(caption(sprite.enlarge(field.crop(CROP), 2), t))
    extras = {}                                              # stills replay the puffs from the start
    t = T0
    for key in keys:
        while t < key - 1e-6:
            scene(variant, sets, ssets, pv, t, plate, extras)
            t += 1 / FPS
        stills[key] = scene(variant, sets, ssets, pv, key, plate, extras)
        t = key + 1 / FPS
    x, y = station(LOST, HIT_T)
    box = (int(x) - 100, int(y) - 120, int(x) + 70, int(y) + 110)
    crops = [stills[k].crop(box) for k in keys]
    for k, c in zip(keys, crops):
        raster.draw_text(c, 3, 3, f"{k:.2f}", (255, 255, 255))
    rows = [(f"CLOUD GLOW 6 FR LOOP AT {GLOW_FPS} FPS, ADDITIVE, UNDER THE AIR LAYER, "
             f"{'72 PX ABOVE' if v['glow_dy'] else 'CENTRED ON'} THE SHUTTLE; STRENGTH PULSES 116-118 (BATCH ALPHA)",
             sets["cloud-glow"], 2, True),
            (f"LANCE {len(sets['lance'])} FR AT {v['lance_steps']} STEPS FROM THE HIT, ADDITIVE, ABOVE THE AIR LAYER"
             + (", TOP ON THE GLOW'S CENTRE, TIP ON THE SHUTTLE" if variant == "a" else
                ", CENTRED ON THE HIT: 0-2 THE COLUMN, 3 THE POINT"), sets["lance"][:6], 4 if variant == "a" else 2, True)]
    if len(sets["lance"]) > 6:
        rows.append(("LANCE (CONTINUED): THE SHOCK RINGS", sets["lance"][6:], 2, True))
    if "lance-flash" in sets:
        rows.append((f"LANCE FLASH {len(sets['lance-flash'])} FR AT {v['flash_steps']} STEPS, CENTRED ON THE HIT",
                     sets["lance-flash"], 2, True))
    rows += [("IN PLAY AT 1X (DAWN STAND-IN): 116.0 GLOW, 116.9 SOUND RUNNING, PULSES, 118.0 HIT, RING/RIPPLE, 119-120.5 GLIDE",
              crops[:5], 1, False),
             ("", crops[5:], 1, False),
             ("IN PLAY AT 2X: THE PEAK BEFORE THE HIT, THE HIT, 0.2 S AFTER",
              [sprite.enlarge(stills[k].crop(box), 2).crop((0, 40, 340, 400)) for k in (117.95, 118.0, 118.2)], 1, False),
             ("GREYSCALE CHECK (THE HIT)", [crops[4].convert("L").convert("RGBA")], 1, False)]
    title = f"SCRIPTED LOSS {v['title']} - GLOW AND LANCE (ROUND 32 PROPOSAL {variant.upper()})"
    sheet = artkit.review_sheet(title, rows, width=1100, batch=BATCH)
    folder = CONCEPT if variant == PRODUCTION else CONCEPT / "rejected"   # the user's pick, round 32
    png, gif_path = folder / f"loss-r32-{variant}.png", folder / f"loss-r32-{variant}.gif"
    folder.mkdir(parents=True, exist_ok=True)
    sheet.convert("RGB").save(png, optimize=True)
    write_gif(gif, gif_path, fps=FPS, colors=128)
    print(f"review: {png.relative_to(ROOT)}, {gif_path.relative_to(ROOT)}")


def load_sets(root):
    old = artkit.SPRITES
    artkit.SPRITES = Path(root)
    try:
        sets = {n: artkit.load_frames(n) for n in ("cloud-glow", "lance", "lance-flash")}
    finally:
        artkit.SPRITES = old
    return {n: f for n, f in sets.items() if f}


if __name__ == "__main__":
    artkit.REVIEW_ROUND = REVIEW_ROUND
    args = sys.argv[1:]
    chosen = args[args.index("--variant") + 1] if "--variant" in args else PRODUCTION
    out = Path(args[args.index("--out") + 1]) if "--out" in args else artkit.SPRITES
    if "--review" in args:
        built = {chosen: load_sets(out)}
    else:
        built = {chosen: frame_sets(chosen)}
        write(built[chosen], chosen, out)
    for name in sorted(VARIANTS):
        review(name, built.get(name) or frame_sets(name))
