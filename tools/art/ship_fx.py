#!/usr/bin/env python3
"""Production art: the Stormhawk's shield ring and damage effects (design/player/ship, Sprite
requirements: shield hit, damage; design/player/shields; M4 part H batch, concept round 26).

Outputs (assets/sprites/):
  ship-shield_0..3.png   60x60 additive hex-shimmer ring around the hull on a shield hit, 4 frames
                         (2 game steps each): the hex cells light up around a hit at the front and
                         a ripple runs over the bubble, fading
  ship-smoke_0..5.png    16x16 smoke puff, normal blending, its life in 6 frames: a small dense
                         puff growing into a thin wide one; the game leaves them behind the ship as
                         a trail below 30 % armour
  ship-sparks_0..5.png   24x24 additive spark burst, 6 frames: white-blue electrical sparks flying
                         out from a point on the hull and fading; below 15 % armour
  design/player/shields/concept/shield-ring-final-r26-a.png/.gif
  design/player/ship/concept/ship-damage-final-r26-a.png/.gif

The ring is the chosen round-08 shield-hit shimmer (tools/concept/vfx_r08.py, shield_frames: hex
grid on the bubble, ripple and hit flare in the player's shield blues) drawn at the asset table's
60x60 without the hull, so the game lays it over the ship; the smoke is round 08's lavender-grey
puff (vfx_r08.smoke_puff), darkened to a damage smoke and given a lumpy outline, a violet-shadowed lower right and four stepped
translucency levels. Sparks avoid the reserved bullet hues (orange, yellow, magenta): they are
white with a pale shield-blue glow, an electrical fault rather than fire.

Run: python3 tools/art/ship_fx.py [--review]   (a few seconds)
"""
import json
import sys

import numpy as np
from PIL import Image

import artkit
from artkit import DESIGN, TAU, sprite

import vfx_r08 as v8  # noqa: E402  (concept script, imported unchanged)

SCRIPT = "ship_fx.py"
BATCH = "M4 part H batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
REVIEW_ROUND = "r26"

RING = 60
RING_FRAMES = 4
RING_RADIUS = 27.0
HEX_CELLS = 5.5                    # cells across the bubble's radius (round 08: 7 on a smaller bubble)
RING_HIT = (0.0, -0.85)            # the hit point on the bubble: the front (bullets come from ahead)
SMOKE = 16
SMOKE_FRAMES = 6
SPARKS = 24
SPARK_FRAMES = 6
SPARK_COUNT = 7
SMOKE_LIGHT = (138, 140, 160)
SMOKE_MID = (90, 90, 108)
SMOKE_DARK = (52, 46, 70)           # violet-shadowed lower right (palette B)
SPARK_GLOW = (150, 215, 255)


def ring_frames():
    """The round-08 shimmer on a round bubble of RING_RADIUS, without the hull (additive)."""
    out = []
    for i in range(RING_FRAMES):
        t = i / (RING_FRAMES - 1)
        cv = v8.Canvas(RING, RING)
        c = RING / 2
        e = cv.ell(c, c, RING_RADIUS, RING_RADIUS)
        u, v = (cv.x - c) / RING_RADIUS, (cv.y - c) / RING_RADIUS
        hx = u * HEX_CELLS
        hy = v * HEX_CELLS * 1.1547
        q = np.abs(((hx + 0.5 * (np.floor(hy) % 2)) % 1.0) - 0.5)
        r = np.abs((hy % 1.0) - 0.5)
        lines = np.clip(1 - np.minimum(q, r) * 9, 0, 1)
        hd = np.hypot(u - RING_HIT[0], v - RING_HIT[1])
        ripple = v8.gauss(hd - t * 1.8, 0.22) * (1 - t)
        local = v8.gauss(hd, 0.5) * (1 - t) ** 1.5
        shell = np.clip(1 - np.abs(e - 1) * 9, 0, 1) * (e < 1.08)
        inside = (e < 1).astype(float)
        amount = (shell * (0.25 + 1.2 * local + ripple)
                  + inside * lines * (0.9 * local + 0.7 * ripple)) * (1.0 - 0.35 * t)
        cv.add(v8.PS[2], amount * 0.9)
        cv.add(v8.WHITE, v8.gauss(hd, 0.18) * (1 - t) ** 2 * 1.4 * shell)
        out.append(artkit.additive(cv.image()))
    return artkit.quantize_set(out, 24)


def smoke_frames():
    """A puff growing and thinning over its life: three seeded blobs, lit from the top left."""
    rng = np.random.default_rng(26)
    blobs = [(rng.uniform(-0.35, 0.35), rng.uniform(-0.35, 0.35), rng.uniform(0.55, 0.8)) for _ in range(3)]
    out = []
    for i in range(SMOKE_FRAMES):
        t = i / (SMOKE_FRAMES - 1)
        radius = 2.8 + 4.4 * t
        opacity = 0.9 - 0.6 * t
        cv = v8.Canvas(SMOKE, SMOKE)
        c = SMOKE / 2
        body = np.zeros_like(cv.x)
        for bx, by, br in blobs:
            body = np.maximum(body, v8.solid(cv.dist(c + bx * radius, c + by * radius), br * radius + 0.6))
        light = (cv.x - c + cv.y - c) / (2 * radius)                      # -1 top left .. 1 bottom right
        cv.over(SMOKE_MID, body * opacity)
        cv.over(SMOKE_LIGHT, body * np.clip(-light * 1.6, 0, 1) * opacity * 0.8)
        cv.over(SMOKE_DARK, body * np.clip(light * 1.6, 0, 1) * opacity * 0.7)
        out.append(artkit.stepped_alpha(cv.image(), levels=5))
    return artkit.quantize_set(out, 16)


def spark_frames():
    """Streaks flying out from the centre and fading: white cores, a pale blue glow (additive)."""
    rng = np.random.default_rng(15)
    streaks = [(rng.uniform(0, TAU), rng.uniform(6.5, 10.5), rng.uniform(0.7, 1.0)) for _ in range(SPARK_COUNT)]
    out = []
    for i in range(SPARK_FRAMES):
        t = (i + 0.5) / SPARK_FRAMES
        cv = v8.Canvas(SPARKS, SPARKS)
        c = SPARKS / 2
        fade = (1 - t) ** 1.3
        for angle, reach, bright in streaks:
            head = reach * (1 - (1 - t) ** 2)
            tail = max(0.0, head - 2.5 - 2.0 * (1 - t))
            dx, dy = np.cos(angle), np.sin(angle) + 0.35 * t      # they droop as they fly
            d = cv.seg(c + dx * tail, c + dy * tail, c + dx * head, c + dy * head)
            cv.add(SPARK_GLOW, v8.gauss(d, 0.9) * 0.55 * fade * bright)
            cv.add(v8.WHITE, v8.solid(d, 0.45) * 1.2 * fade * bright)
        cv.add(v8.WHITE, v8.gauss(cv.dist(c, c), 1.6) * max(0.0, 1 - 2.5 * t) * 1.3)
        out.append(artkit.additive(cv.image()))
    return artkit.quantize_set(out, 16)


def build():
    artkit.write_frames("ship-shield", ring_frames(), SOURCE)
    artkit.write_frames("ship-smoke", smoke_frames(), SOURCE)
    artkit.write_frames("ship-sparks", spark_frames(), SOURCE)


# --------------------------------------------------------------------------- review

def draw_ship(img, cx, cy, bank=2, flame=0):
    """The hull centred on (cx, cy) with its cruise flames, as the game draws them: the flames
    added under the hull at the engine mounts."""
    hull = artkit.load_frames("ship")[bank]
    flames = artkit.load_frames("engine-flame")
    pivots = json.loads((artkit.PIVOTS / "ship.json").read_text(encoding="utf-8"))
    pods = json.loads((artkit.PIVOTS / "pods.json").read_text(encoding="utf-8"))
    ax, ay = pods["engine-flame"]["attach"]
    left, top = cx - hull.width // 2, cy - hull.height // 2
    for side in ("engine-left", "engine-right"):
        x, y = pivots["points"][side][bank]
        img = artkit.add_light(img, flames[flame], (left + x - ax, top + y - ay))
    img.alpha_composite(hull, (left, top))
    return img


def scene(i, ring, smoke, sparks):
    """A 120x120 plate: the ship flying with a smoke trail, sparks and, every 16 frames, a shield hit."""
    img = Image.new("RGBA", (120, 120), (24, 30, 58, 255))
    sx, sy = 60, 46
    for age in range(0, 36, 3):
        puff_tick = i - age
        k = min(SMOKE_FRAMES - 1, age // 6)
        wobble = 2.5 * np.sin(puff_tick * 0.7)
        sprite.paste_center(img, smoke[k], sx + 5 + wobble, sy + 22 + age * 1.6)
    img = draw_ship(img, sx, sy, flame=i % 3)
    burst = i % 10
    if burst < SPARK_FRAMES:
        spot = [(-9, 4), (11, -2), (2, 10)][i // 10 % 3]
        img = artkit.add_light(img, sparks[burst], (sx + spot[0] - SPARKS // 2, sy + spot[1] - SPARKS // 2))
    hit = i % 16
    if hit < 2 * RING_FRAMES:
        img = artkit.add_light(img, ring[hit // 2], (sx - RING // 2, sy - RING // 2))
    return img


def review():
    artkit.REVIEW_ROUND = REVIEW_ROUND
    ring = artkit.load_frames("ship-shield")
    smoke = artkit.load_frames("ship-smoke")
    sparks = artkit.load_frames("ship-sparks")
    hull = artkit.load_frames("ship")[2]
    over = []
    for frame in ring:
        cell = Image.new("RGBA", (RING, RING), artkit.PLATE)
        sprite.paste_center(cell, hull, RING / 2, RING / 2)
        over.append(artkit.add_light(cell, frame))
    sheet = artkit.review_sheet("STORMHAWK SHIELD RING - FINAL SPRITES", [
        ("SHIELD HIT: HEX-SHIMMER RING, 2 STEPS A FRAME (ADDITIVE)", ring, 6, True),
        ("OVER THE HULL, AS THE GAME DRAWS IT", over, 4, False),
        ("1X", ring, 1, True)], batch=BATCH)
    gif = [sprite.enlarge(artkit.add_light(Image.new("RGBA", (RING + 20, RING + 20), artkit.PLATE),
                                           over[i // 2 % RING_FRAMES] if i < 8 else hull_pad(hull),
                                           (10, 10)), 4) for i in range(16)]
    artkit.save_review(sheet, gif, DESIGN / "player" / "shields" / "concept", "shield-ring", fps=20)

    sheet = artkit.review_sheet("STORMHAWK DAMAGE - FINAL SPRITES", [
        ("SMOKE PUFF, ITS LIFE IN 6 FRAMES (BELOW 30 % ARMOUR; NORMAL BLENDING)", smoke, 8, False),
        ("SPARK BURST, 6 FRAMES (BELOW 15 % ARMOUR; ADDITIVE)", sparks, 6, True),
        ("IN FLIGHT: TRAIL, SPARKS AND A SHIELD HIT", [scene(i, ring, smoke, sparks) for i in (0, 3, 12, 21)], 3, False),
        ("1X", smoke + sparks, 1, False)], batch=BATCH)
    gif = [sprite.enlarge(scene(i, ring, smoke, sparks), 3) for i in range(48)]
    artkit.save_review(sheet, gif, DESIGN / "player" / "ship" / "concept", "ship-damage", fps=20)


def hull_pad(hull):
    """The hull on a transparent RING x RING cell (the GIF's frames without a hit)."""
    cell = Image.new("RGBA", (RING, RING), artkit.PLATE)
    sprite.paste_center(cell, hull, RING / 2, RING / 2)
    return cell


if __name__ == "__main__":
    if "--review" not in sys.argv[1:]:
        build()
    review()
