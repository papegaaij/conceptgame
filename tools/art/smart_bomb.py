#!/usr/bin/env python3
"""Production art: the Smart Bomb's flash and shockwave ring (design/player/specials, Smart Bomb
(L06); M4 part H batch, concept round 26), from the chosen round-09 specials sheet
(tools/concept/vfx_r09.py ``smartbomb_frames``: a pale blue halo, a white core and a cyan band
trailing inside the ring; the white flash over the play field).

Outputs (assets/sprites/, additive, premultiplied on black; shared atlas, as the special's):
  smart-bomb-ring.png        72x8: the ring's cross-section, x running outwards across the ring
                             (RING_INSIDE px inside the leading edge to RING_OUTSIDE px beyond it),
                             every row the same; the game draws the ring as a band of quads round
                             the circle with this profile across it (FarsideLooks.drawSmartBomb),
                             so the ring stays a crisp profile at every radius
  smart-bomb-burst_0..11     128x128: where the bomb went off, the blinding core: a white-hot
                             point, a pale blue bloom, a six-point star flare closing and a burst of
                             white sparks, 2 game steps a frame (0.4 s), drawn over the white field
                             flash (the flash itself stays the spec's white fill: 0.1 s at 80 %,
                             fading over 0.25 s, toned down by the flash reduction)
  design/player/specials/concept/smart-bomb-final-r26-a.png/.gif   review sheet and loop

Both are vfx_r08's 2D light fields (its Canvas and gauss), as the chosen sheet's ring was; the ring
profile is the sheet's own (gauss widths 14 / 3.5 / 4 px at the play field's scale), its levels cut
to 16 colours, the burst to 24.

Run: python3 tools/art/smart_bomb.py [--review]   (~5 s)
"""
import sys

import numpy as np
from PIL import Image

import artkit
from artkit import DESIGN, TAU, sprite

import vfx_r08 as v8  # noqa: E402  (concept script, imported unchanged)

SCRIPT = "smart_bomb.py"
BATCH = "M4 part H batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
REVIEW_ROUND = "r26"
STEP = 60

RING_INSIDE = 52                                # px of the profile inside the leading edge
RING_OUTSIDE = 20                               # px beyond it
RING = (RING_INSIDE + RING_OUTSIDE, 8)
HALO = (150, 220, 255)
CYAN = (160, 255, 255)                          # PS[3], the player-shot cyan of palette B
BURST = (128, 128)
BURST_FRAMES = 12
BURST_STEPS = 2
# the spec's numbers (design/player/specials data.yaml smart_bomb), for the review's timeline
RING_SECONDS, FLASH_SECONDS, FLASH_OPACITY, FADE_SECONDS = 0.35, 0.1, 0.8, 0.25


def ring_profile():
    """The cross-section: offset e = (distance - radius) per column, the concept's three terms."""
    w, h = RING
    cv = v8.Canvas(w, h)
    e = cv.x - RING_INSIDE                                    # px from the leading edge, + outwards
    cv.add(HALO, v8.gauss(e, 14) * 0.35)
    cv.add((255, 255, 255), v8.gauss(e, 3.5) * 1.1)
    cv.add(CYAN, v8.gauss(e + 9, 4) * 0.5)
    fade = np.clip(np.minimum(cv.x, w - cv.x) / 6.0, 0, 1)   # both ends fall to black
    cv.rgb *= fade[..., None]
    cv.a *= fade
    img = artkit.additive(cv.image())
    a = np.array(img)
    a[:] = a[h // 2]                                          # every row the same
    return Image.fromarray(a, "RGBA")


def burst(i):
    """Frame i of the core: white-hot point, pale blue bloom, six-point star closing, sparks."""
    w, h = BURST
    t = i / (BURST_FRAMES - 1)
    c = w / 2
    cv = v8.Canvas(w, h)
    d = cv.dist(c, c)
    fade = np.clip((w / 2 - d) / 8.0, 0, 1)
    flare = np.clip(1 - t * 1.15, 0, 1) ** 1.3
    cv.add((255, 255, 255), v8.gauss(d, 5 + 6 * t) * 1.6 * flare)
    cv.add(HALO, v8.gauss(d, 14 + 30 * artkit_ease(t)) * 0.65 * (1 - t) ** 1.4 * fade)
    for k in range(6):                                        # the star, its long arms horizontal
        ang = k * TAU / 12
        length = (56 if k % 3 == 0 else 30) * (0.5 + 0.5 * flare)
        dx, dy = np.cos(ang) * length, np.sin(ang) * length
        seg = cv.seg(c - dx, c - dy, c + dx, c + dy)
        along = np.clip(1 - d / length, 0, 1)
        cv.add((235, 248, 255), v8.gauss(seg, 0.9) * along * 1.3 * flare * fade)
        cv.add(HALO, v8.gauss(seg, 2.4) * along * 0.4 * flare * fade)
    rng = np.random.default_rng(2661)
    for _ in range(40):                                       # sparks thrown out with the ring
        a = rng.uniform(0, TAU)
        reach = rng.uniform(22, 58)
        s = rng.uniform(0.5, 1.1)
        r = 4 + reach * artkit_ease(t, 2.6)
        rp = 4 + reach * artkit_ease(max(t - 0.06, 0), 2.6)
        life = np.clip(1.1 - t * rng.uniform(0.9, 1.4), 0, 1) * (t > 0)
        seg = cv.seg(c + np.cos(a) * rp, c + np.sin(a) * rp, c + np.cos(a) * r, c + np.sin(a) * r)
        cv.add((255, 255, 255) if s > 0.85 else CYAN, v8.gauss(seg, s) * 1.2 * life * fade)
    return artkit.additive(cv.image())


def artkit_ease(t, k=2.0):
    return 1 - (1 - np.clip(t, 0, 1)) ** k


def quantize_bright(frames, colours, weight=12):
    """artkit.quantize_set with the bright pixels counted ``weight`` times when the palette is
    cut: a burst is mostly dim glow, so a plain median cut merges its white-hot core into the
    pale blue; still one palette for the set."""
    arrays = [np.array(f.convert("RGBA")) for f in frames]
    pixels = np.concatenate([a[a[..., 3] > 0][:, :3] for a in arrays])
    bright = pixels[pixels.max(axis=1) >= 170]
    pool = np.concatenate([pixels] + [bright] * weight)
    palette = Image.fromarray(pool.reshape(-1, 1, 3), "RGB").quantize(
        colors=colours, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE)
    out = []
    for a in arrays:
        rgb = Image.fromarray(np.ascontiguousarray(a[..., :3]), "RGB")
        mapped = np.array(rgb.quantize(palette=palette, dither=Image.Dither.NONE).convert("RGB"))
        result = np.dstack([mapped, a[..., 3]])
        result[a[..., 3] == 0] = 0
        out.append(Image.fromarray(result.astype(np.uint8), "RGBA"))
    return out


def build():
    artkit.write_frames("smart-bomb-ring", artkit.quantize_set([ring_profile()], 16), SOURCE, single=True)
    artkit.write_frames("smart-bomb-burst", quantize_bright([burst(i) for i in range(BURST_FRAMES)], 24), SOURCE)


# =========================================================================== review

def field_ring(size, cx, cy, radius, profile, gain=1.0):
    """The ring over a field as the game draws it: each pixel takes the profile at its offset
    from the leading edge (the quads' texture across the band)."""
    w, h = size
    yy, xx = np.mgrid[0:h, 0:w] + 0.5
    e = np.hypot(xx - cx, yy - cy) - radius
    col = np.clip(np.round(e + RING_INSIDE).astype(int), 0, profile.shape[1] - 1)
    inside = (e >= -RING_INSIDE) & (e < RING_OUTSIDE) & (np.hypot(xx - cx, yy - cy) >= 0)
    rgb = profile[profile.shape[0] // 2][col][..., :3].astype(np.float64) * inside[..., None] * gain
    return Image.fromarray(np.dstack([np.clip(rgb, 0, 255), np.full((h, w), 255)]).astype(np.uint8), "RGBA")


def review():
    ring = artkit.load_frames("smart-bomb-ring")[0]
    bursts = artkit.load_frames("smart-bomb-burst")
    profile = np.array(ring)
    # the play field: Level 06's dark far side would hide it; Level 01's Earth shows the colours
    earth = Image.open(artkit.ROOT / "assets" / "backdrop" / "level-01" / "earth.png").convert("RGBA").crop((0, 200, 480, 740))
    ship = artkit.load_frames("ship")[2]
    orb = artkit.load_frames("orb")
    needler = artkit.load_frames("needler")
    w, h = 480, 540
    sx, sy = 240, 430
    reach = np.hypot(max(sx, w - sx), max(sy, h - sy))
    enemies = [(120, 120), (330, 90), (400, 220), (90, 260)]
    rng = np.random.default_rng(2671)
    bullets = [(rng.uniform(30, 450), rng.uniform(40, 380)) for _ in range(26)]
    frames = []
    fps = 30
    for f in range(int(fps * 1.0)):
        step = f * 2 - 8                                       # the bomb goes off at step 0
        sec = step / STEP
        img = earth.copy()
        img = Image.blend(img, Image.new("RGBA", img.size, (0, 0, 0, 255)), 0.35)
        radius = reach * np.clip(sec / RING_SECONDS, 0, 1) if 0 <= sec <= RING_SECONDS else -1
        for ex, ey in enemies:
            dead = sec >= 0 and np.hypot(ex - sx, ey - sy) <= (reach if sec > RING_SECONDS else radius)
            if not dead:
                sprite.paste_center(img, needler[(f // 3) % len(needler)], ex, ey)
        for bx, by in bullets:
            if sec < 0:
                sprite.paste_center(img, orb[(f // 2) % len(orb)], bx, by + f)
        sprite.paste_center(img, ship, sx, sy)
        if radius > 0:
            fade = 1 - max(0.0, (sec / RING_SECONDS - 0.7) / 0.3) * 0.6
            img = artkit.add_light(img, field_ring((w, h), sx, sy, radius, profile, fade))
        age = step // BURST_STEPS
        if 0 <= step and age < len(bursts):
            img = artkit.add_light(img, bursts[age], (sx - 64, sy - 64))
        if sec >= 0:
            flash = FLASH_OPACITY if sec <= FLASH_SECONDS else FLASH_OPACITY * max(0, 1 - (sec - FLASH_SECONDS) / FADE_SECONDS)
            if flash > 0:
                img = Image.blend(img, Image.new("RGBA", img.size, (255, 255, 255, 255)), flash)
        frames.append(img)
    strip = [fr.resize((240, 270), Image.NEAREST) for fr in frames[2:22:2]]
    sheet = artkit.review_sheet("SMART BOMB - FINAL FLASH AND RING", [
        ("SMART-BOMB-RING: THE CROSS-SECTION (INSIDE -> OUTSIDE, LEADING EDGE AT 52 PX), ADDITIVE", [ring], 8, True),
        ("SMART-BOMB-BURST, 30 FPS (2 STEPS), ADDITIVE, AT THE BOMB POINT OVER THE FIELD FLASH", bursts, 2, True),
        ("ON A LEVEL 01 FIELD (DIMMED), EVERY 4TH STEP FROM THE PRESS: FLASH 0.1 S AT 80 % FADING 0.25 S, "
         "RING OVER THE FIELD IN 0.35 S, NEEDLERS AND BULLETS GONE AS IT PASSES", strip, 1, False),
        ("1X", bursts, 1, True)], width=1700, batch=BATCH)
    gif = [fr.resize((360, 405), Image.NEAREST) for fr in frames]
    artkit.save_review(sheet, gif, DESIGN / "player" / "specials" / "concept", "smart-bomb", fps=fps)


if __name__ == "__main__":
    artkit.REVIEW_ROUND = REVIEW_ROUND
    if "--review" not in sys.argv[1:]:
        build()
    review()
