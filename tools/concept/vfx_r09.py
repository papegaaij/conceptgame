#!/usr/bin/env python3
"""Concept round 09 - remaining combat effects at 960x540, palette B (builds on vfx_r08).

Outputs:
  design/player/weapons/concept/beam-impact-r09-a.png/.gif   Ion Beam contact flare, scorch,
                                                             heat shimmer; small / medium /
                                                             large targets and no target
  design/player/wingmen/concept/rook-craft-r09-a.png         Rook's craft (Ember), 5 bank frames
  design/enemies/concept/enemy-bullets-r09-a.png             enemy bullet set, Vrell + Ascendancy,
                                                             readability test over 8 scenes
  design/art-direction/concept/explosions-r09-a.png/.gif     size ladder, Vrell vs Ascendancy,
                                                             water surface / under water, hit
                                                             flash, shield hit, shield break
  design/player/concept/pickups-r09-a.png/.gif               salvage S/M/L, shield cell, armour
                                                             patch, overdrive, special charge,
                                                             data core
  design/player/specials/concept/specials-r09-a.png/.gif     Airstrike, Smart Bomb, Decoy Flares
  design/ui/hud/concept/edge-warnings-r09-a.png              side / rear edge warnings, threat
                                                             arrows, boss warning banner

vfx_r08 is imported and used as is (2D effect canvas, explosion / water / shield generators,
ship and Rook sprites); everything new lives here. Deterministic.

Run: python3 tools/concept/vfx_r09.py [beam rook bullets explosions pickups specials warnings]
     (no args = all)
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, str(Path(__file__).resolve().parent))
import vfx_r08 as v8  # noqa: E402
from vfx_r08 import (BANKS, ES, FH, FPS, FW, GOLD, LABEL, DIM, LIME, OUT, PS, SS,  # noqa: E402
                     TAU, WHITE, Canvas, gauss, label, on_checker, put, rnd, solid)
from render import raster, sprite  # noqa: E402
from render.config import ROOT, WINGMAN_SIZE  # noqa: E402
from render.enemy_rigs import write_gif  # noqa: E402
from render.sdf import (mirror_x, sd_box, sd_capsule, sd_cylinder_y,  # noqa: E402
                        sd_cylinder_z, sd_ellipsoid, sd_fin, sd_plate, sd_sphere, union)

import enemies_r04 as e4  # noqa: E402  (patches enemies_r03 to the round-04 role colours)
import enemies_r03 as e3  # noqa: E402

SUB = "CONCEPT ROUND 09 - 960X540"
SCRATCH = {}


def cached(key, fn):
    if key not in SCRATCH:
        SCRATCH[key] = fn()
    return SCRATCH[key]


ITEMS = {}


def item(name):
    def deco(fn):
        ITEMS[name] = fn
        return fn
    return deco


def enemy(key, n=None):
    """Round-04 enemy sprite (role colours, Ascendancy rim light) at its native size."""
    title, fac, cat, size, model, *_ = e4.R04[key]
    return cached(("enemy", key, n), lambda: e4.render(model, n or size)[1])


def carrier():
    """Brood Carrier (act 1 boss, 288x626 native) in its round-04 colours."""
    return cached("carrier", lambda: e3.carrier_render())


def player(bank=0):
    return v8.ship_sprite(bank)[1]


def bg_frame(gif, i):
    """Frame i of a chosen scene loop (80 frames, scrolls)."""
    key = ("bgf", gif)
    if key not in SCRATCH:
        im = Image.open(OUT["art"] / gif)
        frames = []
        for k in range(im.n_frames):
            im.seek(k)
            frames.append(im.convert("RGBA").copy())
        SCRATCH[key] = frames
    fr = SCRATCH[key]
    return fr[i % len(fr)].copy()


def _bg_tile(kind, seed):
    from render import terrain
    from render.palette import B
    if kind == "orbit":
        im = terrain.recede(terrain.earth_from_orbit(FW, FH, seed=seed, period=True),
                            amount=0.2, darken=0.72)
    elif kind == "city":
        im = terrain.recede(terrain.city_ground(FW, FH, seed=seed, period=True)[0],
                            amount=0.2, darken=0.8)
    elif kind == "coast":
        im = terrain.recede(terrain.earth_coast(FW, FH, seed=seed, period=True, cell=60,
                                                stops=terrain.coast_stops_from(
                                                    B["EARTH ORBIT"], B["EARTH SURFACE"])),
                            amount=0.35, darken=0.62)
    else:
        im = raster.starfield(FW, FH, seed, density=0.003)
    return im.convert("RGBA")


def _seamless(kind, seed):
    """The generators do not tile vertically at 540 px, so the scroll tile is the image plus
    its mirror (period 1080 px): no hard seam, only a soft mirror line."""
    a = np.array(_bg_tile(kind, seed))
    return np.concatenate([a, a[::-1]], axis=0)


def scroll_bg(kind, i, seed=7, speed=2, w=FW, h=FH):
    """Clean (sprite-free) background, scrolled down by ``speed`` px per frame."""
    tile = cached(("tile", kind, seed), lambda: _seamless(kind, seed))
    arr = np.roll(tile, int(i * speed) % tile.shape[0], axis=0)
    return Image.fromarray(arr[:h, :w].copy(), "RGBA")


def tint_sprite(sp, color, k):
    """Blend a sprite's colour towards ``color`` by k, keeping its alpha (hit flash)."""
    flat = Image.new("RGBA", sp.size, tuple(color) + (0,))
    flat.putalpha(sp.getchannel("A"))
    return Image.blend(sp, flat, k)


def panel_frame(img, box, color=(60, 70, 95, 255)):
    ImageDraw.Draw(img).rectangle(box, outline=color)


def notes(img, y, lines, color=LABEL):
    for i, line in enumerate(lines):
        label(img, 16, y + i * 12, line, color)


# =========================================================================== 1. beam impact

BEAM_W = 7          # Ion Beam L5 half-core width used in the round-08 GIF


def beam_segment(top, bottom, width=BEAM_W, t=0.0, taper=6):
    """Ion beam column from y=bottom (muzzle) up to y=top (contact point). The last ``taper``
    pixels narrow and whiten so the beam reads as boring into the target, not cut off."""
    h = max(int(bottom - top), 2)
    cv = Canvas(width * 4 + 8, h)
    cx = cv.w / 2
    dx = np.abs(cv.x - cx)
    y = cv.y
    wob = 1 + 0.15 * np.sin((y + top) * 0.35 + t * 40)
    # taper factor: 1 along the column, falls to ~0.45 at the contact end
    tp = np.clip(y / max(taper, 1), 0, 1) if taper else np.ones_like(y)
    k = 0.45 + 0.55 * tp
    cv.add(PS[1], gauss(dx, width * 1.6 * wob * k) * 0.7)
    cv.over(PS[2], solid(dx, width * 0.6 * wob * k) * 0.9)
    rip = (np.sin((y + top) * 0.22 + t * 60) > 0.6) * solid(dx, width * 0.75 * k)
    cv.add(PS[3], rip * 0.5)
    cv.over(WHITE, solid(dx, width * 0.25 * wob * (0.6 + 0.8 * (1 - tp)) + 0.01))
    if taper:
        cv.add(WHITE, gauss(dx, width * 0.9) * (1 - tp) * 1.2)
    return cv.image()


def beam_offscreen_segment(bottom, width=BEAM_W, t=0.0):
    """No target: the beam runs to the top edge and on out of the play field (no cap)."""
    return beam_segment(-4, bottom, width, t, taper=0)


def contact_frames(n=6, seed=31, power=1.0, scale=1.0):
    """Contact flare loop centred on the hit point (image centre): white-hot core, horizontal
    flare streak, a splash crescent spreading along the target surface and sparks thrown back
    and sideways (towards the ship, away from the target). ``scale`` shrinks the flare for
    small targets so it never swallows them."""
    rng = rnd(seed)
    W, H = 64, 52
    cx, cy = W / 2, 22
    s = scale
    sparks = [(rng.uniform(np.radians(15), np.radians(165)), rng.uniform(0.5, 1.0),
               rng.uniform(0, 1), rng.uniform(1.6, 3.6)) for _ in range(int(8 + 8 * s))]
    out = []
    for i in range(n):
        t = i / n
        cv = Canvas(W, H)
        d = cv.dist(cx, cy)
        pulse = 1 + 0.18 * np.sin(t * TAU * 2)
        cv.add(PS[1], gauss(d, 12 * s * pulse) * 0.4 * power)
        cv.add(PS[2], gauss(d, 5.5 * s * pulse) * 0.85 * power)
        # horizontal anamorphic streak + faint vertical
        sx = gauss(np.abs(cv.y - cy), 0.8) * gauss(np.abs(cv.x - cx), 18 * s * pulse)
        sv = gauss(np.abs(cv.x - cx), 0.7) * gauss(np.abs(cv.y - cy), 6 * s * pulse)
        cv.add(PS[3], (sx * 1.1 + sv * 0.6) * power)
        # splash crescent: energy spreading along the surface (flattened half-ring)
        for k in range(2):
            ph = (t + k * 0.5) % 1.0
            e = cv.ell(cx, cy + 1, (4 + ph * 14) * s, (1.6 + ph * 4) * s)
            band = gauss(e - 1, 0.18) * (cv.y >= cy - 2) * (1 - ph) ** 1.3
            cv.add(PS[3], band * 0.9 * power)
        # sparks: persistent particles, each looping with its own phase
        for a, sp, ph, ln in sparks:
            u = (t + ph) % 1.0
            r0 = (3 + u * 22 * sp) * s
            px = cx + np.cos(a) * r0 * 1.15
            py = cy + np.sin(a) * r0 * 0.85 + u * u * 6 * s
            L = ln * (1 - u * 0.6) * (0.5 + 0.5 * s)
            dd = cv.seg(px, py, px - np.cos(a) * L, py - np.sin(a) * L)
            col = WHITE if sp > 0.75 else PS[3]
            cv.add(col, gauss(dd, 0.45) * (1.4 - u) * power)
        cv.over(WHITE, solid(d, (1.2 + 1.4 * s) * pulse))
        out.append(cv.image())
    return out


FLARE_SCALE = {"s": 0.55, "m": 0.8, "l": 1.0}


def flare_set(size_px):
    k = "s" if size_px < 50 else "m" if size_px < 130 else "l"
    return cached(("flares", k), lambda: contact_frames(scale=FLARE_SCALE[k]))


def heat_glow(sp, cx, cy, heat):
    """Scorch + glow on the target around the contact point (cx, cy in sprite pixels).
    heat 0..1 grows while the beam stays on: a darkened scorch halo, then a white-cyan
    glow hugging the silhouette (masked by the sprite's alpha, so it never spills off it)."""
    arr = np.array(sp).astype(np.float64)
    k = float(np.clip(sp.width / 72, 0.45, 1.3))      # glow scales with the target size
    ys, xs = np.mgrid[0:sp.height, 0:sp.width]
    d = np.hypot(xs - cx, (ys - cy) * 1.3) / k
    scorch = (1 - 0.55 * heat * np.exp(-(d / (6 + 8 * heat)) ** 2))[..., None]
    rgb = arr[..., :3] * scorch
    glow = np.exp(-(d / (3 + 4 * heat)) ** 2)[..., None]
    rgb = rgb + (np.array(PS[3]) * 0.8 * glow + np.array(WHITE) * 0.5 * glow ** 3) * heat
    ring = np.exp(-((d - (5 + 6 * heat)) / 1.4) ** 2)[..., None]   # glowing edge of the scorch
    rgb = rgb + np.array(PS[2]) * ring * 0.35 * heat
    out = np.concatenate([np.clip(rgb, 0, 255), arr[..., 3:4]], axis=2)
    return Image.fromarray(out.astype(np.uint8), "RGBA")


def shimmer(img, cx, cy, t, rx=16, ry=22, amp=1.3):
    """Heat shimmer: rows around and in front of the contact point wobble sideways."""
    arr = np.array(img)
    out = arr.copy()
    H, W = arr.shape[:2]
    for y in range(int(cy - ry * 0.4), int(cy + ry)):
        if not 0 <= y < H:
            continue
        fy = 1 - abs(y - (cy + ry * 0.3)) / (ry * 0.7 + 1e-6)
        if fy <= 0:
            continue
        x0, x1 = int(max(0, cx - rx)), int(min(W, cx + rx))
        xs = np.arange(x0, x1)
        fx = np.clip(1 - np.abs(xs - cx) / rx, 0, 1)
        off = amp * fy * fx * np.sin(y * 0.9 + t * 26) * np.sin(xs * 0.35 + t * 13)
        src = np.clip(np.round(xs + off).astype(int), 0, W - 1)
        out[y, x0:x1] = arr[y, src]
    return Image.fromarray(out, img.mode)


def bottom_edge(sp, col):
    """Lowest opaque row of a sprite in column ``col`` (None if the column is empty)."""
    if not 0 <= col < sp.width:
        return None
    a = np.array(sp.getchannel("A"))[:, col]
    rows = np.nonzero(a > 128)[0]
    return int(rows.max()) if len(rows) else None


PEN = 4             # how far the beam bores into the silhouette before the flare


def beam_scene(img, bx, muzzle_y, targets, t, frame, heat_of=None, flares=None):
    """Draw the beam from (bx, muzzle_y) upward onto img. targets: list of dicts
    {sp, x, y (centre), name}. Returns the index of the hit target (or None) and the contact
    point. Targets are drawn by this function (heated if hit)."""
    hit, best = None, None
    for i, tg in enumerate(targets):
        sp = tg["sp"]
        left = int(round(tg["x"] - sp.width / 2))
        top = int(round(tg["y"] - sp.height / 2))
        e = bottom_edge(sp, int(round(bx)) - left)
        if e is None:
            continue
        ey = top + e
        if ey < muzzle_y and (best is None or ey > best):
            best, hit = ey, i
    for i, tg in enumerate(targets):
        sp = tg["sp"]
        if i == hit:
            left = tg["x"] - sp.width / 2
            top = tg["y"] - sp.height / 2
            heat = heat_of(i) if heat_of else 1.0
            sp = heat_glow(sp, bx - left, best - top - PEN, heat)
            if tg.get("flash"):
                sp = tint_sprite(sp, WHITE, 0.6)
        if tg.get("shadow", True):
            v8.shadow(img, sp, tg["x"], tg["y"], 21, 30, 0.5)
        put(img, sp, tg["x"], tg["y"])
    if hit is None:
        seg = beam_offscreen_segment(muzzle_y, t=t)
        sprite.paste(img, seg, bx - seg.width / 2, -4)
        return None, None
    cy = best - PEN
    seg = beam_segment(cy, muzzle_y, t=t)
    sprite.paste(img, seg, bx - seg.width / 2, cy)
    fl = (flares or flare_set(targets[hit]["sp"].width))[frame % 6]
    v8.additive(img, fl, bx, cy + (fl.height / 2 - 22))
    return hit, (bx, cy)


def beam_panel(w, h, bg, targets, bx, t=0.3, frame=0, heat=1.0, ship_y=None):
    img = bg.copy()
    ship = player(0)
    sy = ship_y or h - 40
    hit, pt = beam_scene(img, bx, sy - 22, targets, t, frame, heat_of=lambda i: heat)
    if pt:
        img = shimmer(img, pt[0], pt[1], t)
    v8.shadow(img, ship, bx, sy)
    put(img, ship, bx, sy)
    return img, pt


def beam_targets(kind, w):
    if kind == "small":
        return [{"sp": enemy("needler-a"), "x": w / 2 + 3, "y": 150}]
    if kind == "medium":
        return [{"sp": enemy("gilded-gunship-a"), "x": w / 2 - 6, "y": 140}]
    if kind == "large":
        c = carrier()
        return [{"sp": c, "x": w / 2 + 20, "y": 130 - c.height / 2, "shadow": False}]
    return []


def beam_sheet():
    W, H = 1300, 1330
    img = raster.sheet(W, H, "ION BEAM - IMPACT WHERE THE BEAM MEETS THE TARGET", SUB)
    label(img, 16, 38, "CONTACT FLARE - 6-FRAME LOOP AT 20 FPS (2X), ADDITIVE, CENTRED ON THE CONTACT POINT."
          " THREE SIZES: SMALL (< 50 PX) / MEDIUM / LARGE TARGETS")
    for r, k in enumerate("sml"):
        for i, f in enumerate(flare_set({"s": 36, "m": 72, "l": 288}[k])):
            img.alpha_composite(on_checker(f, 2), (16 + r * 432 + (i % 3) * 136,
                                                   52 + (i // 3) * 108))
    # anatomy
    y1 = 52 + 216 + 12
    label(img, 16, y1, "ANATOMY (4X) - BEAM BORING INTO THE GILDED GUNSHIP")
    gs = enemy("gilded-gunship-a")
    bg = scroll_bg("orbit", 0, seed=13, w=200, h=150)
    tg = [{"sp": gs, "x": 100, "y": 50}]
    pan, pt = beam_panel(200, 150, bg, tg, 100 - 6, t=0.21, frame=2, ship_y=400)
    crop = pan.crop((int(pt[0]) - 50, int(pt[1]) - 40, int(pt[0]) + 50, int(pt[1]) + 35))
    img.alpha_composite(sprite.enlarge(crop, 4), (16, y1 + 14))
    callouts = [
        "1  THE BEAM DOES NOT STOP AT THE SPRITE EDGE: IT BORES 4 PX INTO THE SILHOUETTE,",
        "   NARROWING AND TURNING WHITE-HOT OVER ITS LAST 6 PX (DRAWN ABOVE THE TARGET).",
        "2  CONTACT FLARE ON THE END POINT: WHITE CORE, CYAN BLOOM, A HORIZONTAL FLARE",
        "   STREAK AND A SPLASH CRESCENT THAT RUNS ALONG THE HULL (6-FRAME LOOP).",
        "3  SPARKS ARE THROWN BACK AND SIDEWAYS, TOWARDS THE SHIP, AND FALL AWAY.",
        "4  SCORCH: THE HULL DARKENS AROUND THE CONTACT POINT; A GLOWING RIM AND A",
        "   WHITE-CYAN HOT SPOT GROW OVER ABOUT 1 S WHILE THE BEAM STAYS ON. THE GLOW IS",
        "   MASKED TO THE SPRITE, SO IT NEVER SPILLS OFF THE SILHOUETTE.",
        "5  HEAT SHIMMER: A 1-2 PX SIDEWAYS ROW WOBBLE AROUND AND BELOW THE CONTACT POINT.",
        "6  NO TARGET: THE BEAM RUNS TO THE TOP EDGE AND OFF THE SCREEN, NO CAP, NO FLARE.",
        "",
        "ALL BEAM LIGHT STAYS BLUE / CYAN / WHITE (PLAYER-SHOT HUES): NO ORANGE HEAT GLOW,",
        "BECAUSE ORANGE IS A RESERVED ENEMY-BULLET HUE. THE CONTACT POINT IS THE LOWEST",
        "OPAQUE PIXEL OF THE TARGET IN THE BEAM'S COLUMN, SO IT FOLLOWS THE SILHOUETTE",
        "(GAPS BETWEEN WINGS LET THE BEAM THROUGH TO THE NEXT TARGET).",
    ]
    for i, line in enumerate(callouts):
        label(img, 440, y1 + 20 + i * 13, line, LABEL)
    gs = enemy("gilded-gunship-a")
    col = gs.width // 2 - 6
    e = bottom_edge(gs, col)
    hy = y1 + 20 + len(callouts) * 13 + 14
    label(img, 440, hy, "HEAT BUILD-UP ON THE TARGET WHILE THE BEAM STAYS ON (0 / 0.3 / 0.6 / 1 S, 2X)")
    for k, hv in enumerate((0.0, 0.33, 0.66, 1.0)):
        h_sp = heat_glow(gs, col, e - PEN, hv)
        img.alpha_composite(on_checker(h_sp, 2), (440 + k * 156, hy + 14))
    # four in-game panels
    y2 = y1 + 14 + 300 + 70
    pw, ph = 300, 420
    kinds = [("small", "SMALL: NEEDLER (36 PX)"), ("medium", "MEDIUM: GILDED GUNSHIP (72 PX)"),
             ("large", "LARGE: BROOD CARRIER BOW (288 PX WIDE)"),
             ("none", "NO TARGET: RUNS OFF THE TOP EDGE")]
    for k, (kind, title) in enumerate(kinds):
        x = 16 + k * (pw + 16)
        bg = scroll_bg(("orbit", "city", "space", "coast")[k], 0, seed=5 + k, w=pw, h=ph)
        tgs = beam_targets(kind, pw)
        pan, pt = beam_panel(pw, ph, bg, tgs, pw / 2, t=0.1 + k * 0.13, frame=k)
        label(img, x, y2, title)
        img.alpha_composite(pan, (x, y2 + 12))
        panel_frame(img, (x - 1, y2 + 11, x + pw, y2 + 12 + ph))
        iy = y2 + 12 + ph + 8
        if pt:
            crop = pan.crop((int(pt[0]) - 45, int(pt[1]) - 30, int(pt[0]) + 45, int(pt[1]) + 20))
            label(img, x, iy, "CONTACT POINT 3X")
        else:
            crop = pan.crop((int(pw / 2) - 45, 0, int(pw / 2) + 45, 50))
            label(img, x, iy, "TOP EDGE 3X: THE BEAM LEAVES THE SCREEN")
        img.alpha_composite(sprite.enlarge(crop, 3), (x, iy + 12))
    notes(img, H - 40, [
        "IN-GAME PANELS AT 1X OVER ORBIT, MEGACITY, DEEP SPACE AND COAST GROUND LAYERS. THE GIF SHOWS THE BEAM SWEEPING",
        "ACROSS TARGETS OF ALL THREE SIZES AND THROUGH THE GAPS BETWEEN THEM; A NEEDLER BURNS THROUGH AND EXPLODES."])
    return img


def beam_gif():
    n = 100
    targets_base = [
        {"sp": carrier(), "x": 250, "y": 120 - carrier().height / 2, "shadow": False},
        {"sp": enemy("gilded-gunship-a"), "x": 110, "y": 250},
        {"sp": enemy("gilded-gunship-a"), "x": 380, "y": 230},
    ]
    nd = enemy("needler-a")
    boom = cached("boom40", lambda: v8.explosion_frames(40, 12, 3, "vrell"))
    frames = []
    heat = {}
    needler_dead, spawn = None, 0
    for f in range(n):
        t = f / FPS
        img = scroll_bg("space", f, seed=11)
        bx = 240 + 190 * np.sin(t * 1.25 - 0.4)
        targets = [dict(tg) for tg in targets_base]
        # a needler crosses left -> right at y 345 and burns through after ~0.6 s of beam
        if needler_dead is not None and f - needler_dead == 24:
            spawn, needler_dead = f, None
        if needler_dead is None:
            targets.append({"sp": nd, "x": 40 + (f - spawn) * 5, "y": 345, "name": "n"})
        for i, tg in enumerate(targets):
            tg["y"] += 2 * np.sin(t * 3 + i)
            tg.setdefault("name", i)
            tg["flash"] = (f % 6 == 0)
        hit_name = None

        def heat_of(i, targets=targets):
            return min(1.0, heat.get(targets[i]["name"], 0) / 20)
        sy = 470
        ship = player(v8.bank_frame(190 * 1.25 * np.cos(t * 1.25 - 0.4)))
        hit, pt = beam_scene(img, bx, sy - 22, targets, t, f, heat_of=heat_of)
        if hit is not None:
            hit_name = targets[hit]["name"]
            heat[hit_name] = heat.get(hit_name, 0) + 1
            img = shimmer(img, pt[0], pt[1], t)
            if hit_name == "n" and heat[hit_name] > 12:
                needler_dead = f
                heat["n"] = 0
                SCRATCH["nd_pos"] = (targets[hit]["x"], targets[hit]["y"])
        for k in list(heat):
            if k != hit_name:
                heat[k] = max(0, heat[k] - 2)          # cools down when the beam moves off
        if needler_dead is not None and 0 <= f - needler_dead < len(boom):
            put(img, boom[f - needler_dead], *SCRATCH["nd_pos"])
        v8.shadow(img, ship, bx, sy)
        put(img, ship, bx, sy)
        label(img, 8, 8, "ION BEAM L5 - IMPACT", raster.ACCENT, scale=2)
        label(img, 8, 28, "FLARE, SCORCH, SHIMMER; RUNS OFF-SCREEN IN GAPS", LABEL)
        frames.append(img)
    return frames


# =========================================================================== 2. Rook's banking

def rook_sheet():
    data = [v8.rook_sprite(b) for b in BANKS]
    hi = data[2][0]
    frames = [d[1] for d in data]
    pl = [player(b) for b in BANKS]
    n = WINGMAN_SIZE
    img = raster.sheet(1300, 600, "ROOK'S CRAFT (EMBER) - 5 BANKING FRAMES", SUB)
    label(img, 16, 38, f"SOURCE RENDER ({n * 8} PX, CENTRE FRAME)")
    src = v8.checker(n * 8, n * 8)
    src.alpha_composite(sprite.to_image(hi))
    img.alpha_composite(src, (16, 50))
    x0 = 356
    label(img, x0, 38, "BANKING: HARD LEFT -28, LEFT -14, CENTRE, RIGHT +14, HARD RIGHT +28 DEG (4X)")
    for i, f in enumerate(frames):
        x = x0 + i * (n * 4 + 12)
        img.alpha_composite(on_checker(f, 4), (x, 50))
        label(img, x, 50 + n * 4 + 4, f"{BANKS[i]:+d} DEG")
    y1 = 50 + n * 4 + 26
    label(img, x0, y1, "1X: ROOK (40X40) BESIDE THE PLAYER'S STORMHAWK (48X48), SAME 5 ANGLES")
    strip = v8.checker(5 * 104 + 8, 64)
    for i in range(5):
        strip.alpha_composite(frames[i], (6 + i * 104, 12))
        strip.alpha_composite(pl[i], (6 + i * 104 + 46, 8))
    img.alpha_composite(strip, (x0, y1 + 12))
    label(img, x0, y1 + 90, "GREYSCALE CHECK (VALUE SEPARATION FROM THE PLAYER)")
    img.alpha_composite(strip.convert("L").convert("RGBA"), (x0, y1 + 102))
    # in game, 2x: escort formation banking left, then right
    g = scroll_bg("coast", 30, seed=34, w=190, h=150)
    for (rx, ry, px_, py_, b) in ((40, 98, 90, 70, 1), (125, 110, 165, 80, 3)):
        v8.shadow(g, frames[b], rx, ry)
        put(g, frames[b], rx, ry)
        v8.shadow(g, pl[b], px_, py_)
        put(g, pl[b], px_, py_)
    gx = 1300 - 16 - 380
    label(img, gx, y1, "IN GAME (2X): ESCORT BANKING LEFT, THEN RIGHT")
    img.alpha_composite(sprite.enlarge(g, 2), (gx, y1 + 12))
    notes(img, 400, [
        "EMBER SCHEME (CHOSEN IN ROUND 02) ON THE SHIP C AIRFRAME, 40X40.",
        "ROOK BANKS WITH THE SAME 5 ANGLES AS THE PLAYER (FRAMES CHANGE",
        "OVER ABOUT 6 GAME FRAMES), SO THE PAIR MOVES AS ONE IN FORMATION.",
        "SMALLER, DARKER AND WARMER THAN THE STORMHAWK: THE PLAYER STAYS",
        "DOMINANT AND THE TWO NEVER READ ALIKE.",
        "KEY LIGHT FIXED TOP-LEFT IN EVERY FRAME (SEPARATE RENDERS).",
    ])
    return img


@item("rook")
def do_rook():
    save_png(rook_sheet(), "wing", "rook-craft-r09-a.png")


# =========================================================================== 3. enemy bullets

ORANGE = (255, 122, 26)
CRIMSON = (255, 48, 56)
FAC = {
    "vrell": {"ring": ES[2], "deep": ES[1], "hi": ES[3], "rim": ES[0], "needle": ES[5],
              "needle_deep": (200, 170, 0), "core": WHITE},
    "asc": {"ring": ORANGE, "deep": (190, 60, 0), "hi": (255, 210, 140), "rim": (52, 14, 0),
            "needle": GOLD[4], "needle_deep": GOLD[2], "core": (255, 250, 225)},
}
ACID = {"ring": LIME, "deep": (70, 150, 0), "hi": (220, 255, 160), "rim": ES[0], "core": WHITE}


def rimmed(cv, d, c, glow=0.35, core=None, ring_key="ring", deep_key="deep", hi_key="hi"):
    """Bullet body from a signed distance field d (px, negative inside): soft outer glow,
    1 px dark rim, saturated ring, lighter inner band, white core (``core``: coverage)."""
    out = np.clip(d, 0, None)
    cv.add(c[ring_key], gauss(out, 2.4) * glow * (d > 0.5))
    cv.over(c["rim"], solid(d, 0.0))
    cv.over(c[deep_key], solid(d, -0.9))
    cv.over(c[ring_key], solid(d, -1.6))
    cv.over(c[hi_key], solid(d, -2.6) * 0.8)
    if core is not None:
        cv.over(c["core"], np.clip(core, 0, 1))


def b_orb(c, r, phase=0.0, size=None):
    n = size or int(np.ceil(2 * r + 6))
    cv = Canvas(n, n)
    m = n / 2
    d = cv.dist(m, m) - r
    rimmed(cv, d, c, core=solid(cv.dist(m - 0.4, m - 0.4), r * (0.42 + 0.08 * np.sin(phase))))
    return cv.image()


def b_large(c, r, phase=0.0):
    n = int(np.ceil(2 * r + 10))
    cv = Canvas(n, n)
    m = n / 2
    rr = r + 0.6 * np.sin(phase)
    d = cv.dist(m, m) - rr
    cv.add(c["ring"], gauss(np.clip(d, 0, None), 3.5) * (0.25 + 0.2 * np.sin(phase)) * (d > 0.5))
    rimmed(cv, d, c, core=solid(cv.dist(m - 0.5, m - 0.5), rr * (0.38 + 0.1 * np.sin(phase))))
    # darker swirl band inside the ring (reads as a heavy, slow blob)
    ang = np.arctan2(cv.y - m, cv.x - m) + phase
    band = (np.abs(cv.dist(m, m) - rr * 0.68) < 0.8) * (np.sin(ang * 3) > 0.2)
    cv.over(c["deep"], band * 0.6)
    return cv.image()


def b_needle(c, length=13, width=5, angle=0.0, pointed=True):
    pad = 5
    n = int(length + 2 * pad)
    cv = Canvas(n, n)
    m = n / 2
    ca, sa = np.sin(angle), np.cos(angle)          # angle 0 = flying down
    hl, hw = length / 2, width / 2

    if pointed:     # thorn: sharp front (down), blunter back
        pts = [(0, hl), (hw, -hl * 0.2), (hw * 0.6, -hl), (-hw * 0.6, -hl), (-hw, -hl * 0.2)]
    else:           # slug / flechette: capsule-ish octagon
        pts = [(0, hl), (hw * 0.8, hl * 0.6), (hw, -hl * 0.6), (hw * 0.5, -hl),
               (-hw * 0.5, -hl), (-hw, -hl * 0.6), (-hw * 0.8, hl * 0.6)]
    rot = [(m + x * np.cos(angle) - y * np.sin(angle), m + x * np.sin(angle) + y * np.cos(angle))
           for x, y in pts]
    d = cv.poly(rot)
    core = cv.seg(m - np.sin(-angle) * 0, m - hl * 0.45 * np.cos(angle),
                  m, m + hl * 0.35 * np.cos(angle))
    rimmed(cv, d, c, core=solid(core, 0.9), ring_key="needle", deep_key="needle_deep",
           hi_key="needle")
    return cv.image()


def b_ring(c, r=5.5, phase=0.0):
    n = int(np.ceil(2 * r + 8))
    cv = Canvas(n, n)
    m = n / 2
    dc = cv.dist(m, m)
    d = np.abs(dc - (r - 1.6)) - 1.6                 # annulus, 3.2 px wide
    ang = np.arctan2(cv.y - m, cv.x - m)
    rimmed(cv, d, c)
    hi = (np.abs(dc - (r - 1.6)) < 0.55) * (np.cos(ang * 2 - phase) > 0.3)
    cv.over(c["core"], hi)
    return cv.image()


def b_diamond(c, r=6.5, phase=0.0, tail=False):
    n = int(np.ceil(2 * r + 10)) + (6 if tail else 0)
    cv = Canvas(n, n)
    m = n / 2
    sq = 0.55 + 0.45 * abs(np.cos(phase))           # spin about the vertical axis
    pts = [(m, m + r * 1.1), (m + r * sq, m), (m, m - r * 1.1), (m - r * sq, m)]
    if tail:   # small exhaust flame trailing up (it flies down at the player)
        fl = cv.seg(m, m - r, m, m - r - 5)
        cv.add(c["hi"], gauss(fl, 1.0) * 0.9)
    d = cv.poly(pts)
    rimmed(cv, d, c, core=solid(cv.dist(m, m), 1.6 + 0.4 * abs(np.sin(phase * 2))))
    return cv.image()


def b_acid(phase=0.0, r=5.0):
    n = int(np.ceil(2 * r + 10))
    cv = Canvas(n, n)
    m = n / 2
    ang = np.arctan2(cv.y - m, cv.x - m)
    wob = 1 + 0.12 * np.sin(ang * 3 + phase) + 0.08 * np.sin(ang * 5 - phase * 2)
    d = cv.dist(m, m) - r * wob
    drip = cv.dist(m + 1.5 * np.sin(phase), m - r - 2.5) - 1.4     # trailing droplet
    d = np.minimum(d, drip)
    rimmed(cv, d, ACID, core=solid(cv.dist(m - 1, m - 1), r * 0.4))
    return cv.image()


def b_rail(c, length=20, width=5, phase=0.0):
    n = length + 12
    cv = Canvas(width * 4 + 6, n)
    m = cv.w / 2
    d = cv.seg(m, 6 + width / 2, m, n - 6 - width / 2) - width / 2
    cv.add(c["ring"], gauss(cv.seg(m, 2, m, 8), 1.5) * 0.5)        # hot wake behind
    rimmed(cv, d, c, core=solid(cv.seg(m, 8, m, n - 7), 1.0), ring_key="needle",
           deep_key="ring", hi_key="needle")
    return cv.image()


def b_mine(c, phase=0.0, spore=False):
    n = 22
    cv = Canvas(n, n)
    m = n / 2
    ang = np.arctan2(cv.y - m, cv.x - m)
    if spore:
        r = 5.6 + 0.5 * np.sin(phase)
        sp = np.clip(np.cos(ang * 6 + phase * 0.3), 0, 1) ** 6 * 2.2
        d = cv.dist(m, m) - r - sp
        cc = ACID
    else:
        d = np.minimum(cv.dist(m, m) - 5.5,
                       cv.dist(m, m) - 4 - (np.abs(np.cos(ang * 4)) ** 12) * 3.0)
        cc = c
    rimmed(cv, d, cc, glow=0.25 + 0.25 * (np.sin(phase) > 0),
           core=solid(cv.dist(m, m), 1.8 + (1.0 if np.sin(phase) > 0 else 0)))
    if not spore:   # dark metal body with an orange blink light in the middle
        body = solid(cv.dist(m, m) - 3.8, 0) * (1 - solid(cv.dist(m, m), 2.0))
        cv.over((40, 30, 34), body)
        cv.over((120, 100, 90), body * (cv.x + cv.y < 2 * m - 2) * 0.6)
    return cv.image()


def b_marker(c, phase=0.0, size=36):
    """Mortar impact marker on the ground layer (>= 1 s ahead): dashed ring + cross,
    tightening as the round comes down."""
    cv = Canvas(size, size)
    m = size / 2
    r = (size / 2 - 3) * (1 - 0.25 * phase)
    dc = cv.dist(m, m)
    ang = np.arctan2(cv.y - m, cv.x - m)
    dash = (np.sin(ang * 8 + phase * 4) > -0.2)
    ring = (np.abs(dc - r) < 0.9) * dash
    cv.over(c["rim"], (np.abs(dc - r) < 1.7) * dash * 0.8)
    cv.over(c["ring"], ring)
    for a in (0, np.pi / 2, np.pi, 1.5 * np.pi):
        tick = cv.seg(m + np.cos(a) * (r - 5), m + np.sin(a) * (r - 5),
                      m + np.cos(a) * (r + 2), m + np.sin(a) * (r + 2))
        cv.over(c["ring"], solid(tick, 0.8))
    cv.over(c["core"], solid(dc, 1.2))
    return cv.image()


def b_laser(c, length=60, phase="beam", width=4.0):
    """laser-line: telegraph (thin flickering line >= 0.8 s), charge (thicker, brighter),
    beam (white core, saturated sheath, dark rim)."""
    cv = Canvas(int(width * 6 + 6), length)
    m = cv.w / 2
    dx = np.abs(cv.x - m)
    if phase == "tele":
        dash = (np.sin(cv.y * 0.5) > -0.6)
        cv.over(c["rim"], solid(dx, 1.4) * 0.7)
        cv.over(c["ring"], solid(dx, 0.5) * dash)
    elif phase == "charge":
        cv.over(c["rim"], solid(dx, 2.0))
        cv.over(c["ring"], solid(dx, 1.2))
        cv.add(c["ring"], gauss(dx, 3.0) * 0.4)
        cv.over(c["core"], solid(dx, 0.4))
    else:
        cv.add(c["ring"], gauss(np.clip(dx - width, 0, None), 3.0) * 0.5 * (dx > width + 0.5))
        cv.over(c["rim"], solid(dx, width))
        cv.over(c["deep"], solid(dx, width - 0.9))
        cv.over(c["ring"], solid(dx, width - 1.6))
        cv.over(c["core"], solid(dx, (width - 1.6) * 0.5))
    return cv.image()


LASER_V = dict(FAC["vrell"], deep=(150, 0, 60), ring=(255, 64, 160))   # magenta-crimson


def bullet_set():
    """name -> (title, class line, {faction: [frames]} ) in sheet order."""
    V, A = FAC["vrell"], FAC["asc"]
    ph = [k * TAU / 4 for k in range(4)]
    rows = [
        ("ORB", "SMALL 4 DMG - STANDARD 140-170 PX/S - 9 PX - AIMED, FAN, SPIRAL",
         {"vrell": [b_orb(V, 4.5, p) for p in ph], "asc": [b_orb(A, 4.5, p) for p in ph]}),
        ("LARGE ORB", "MEDIUM 6 DMG - SLOW 90-120 PX/S - 13 PX, PULSES (4 FRAMES)",
         {"vrell": [b_large(V, 6.5, p) for p in ph], "asc": [b_large(A, 6.5, p) for p in ph]}),
        ("NEEDLE", "SMALL 4 DMG - FAST 190-260 PX/S - 5X13 - ELONGATED = FAST",
         {"vrell": [b_needle(V, angle=a) for a in (0, 0.4, -0.4, np.pi / 2)],
          "asc": [b_needle(A, pointed=False, angle=a) for a in (0, 0.4, -0.4, np.pi / 2)]}),
        ("RING", "SMALL 4 DMG - STANDARD - 11 PX HOLLOW, SPINS - RING + DEATH-BURST",
         {"vrell": [b_ring(V, phase=p) for p in ph], "asc": [b_ring(A, phase=p) for p in ph]}),
        ("HOMING", "SMALL 4 DMG - SLOW - 12 PX DIAMOND, SPINS - SHOOTABLE (1-3 HP)",
         {"vrell": [b_diamond(V, phase=p) for p in ph],
          "asc": [b_diamond(A, phase=p, tail=True) for p in ph]}),
        ("ACID / RAIL", "VRELL ACID: MEDIUM 6 - SLOW, LIME (AREA)   ASC RAIL: HEAVY 10 - FAST 260",
         {"vrell": [b_acid(p) for p in ph], "asc": [b_rail(A, phase=p) for p in ph]}),
        ("MINE", "CONTACT 10 - DRIFTS - 13-17 PX - SHOOTABLE; VRELL SPORE (LIME), ASC PROXIMITY",
         {"vrell": [b_mine(V, p, spore=True) for p in ph], "asc": [b_mine(A, p) for p in ph]}),
        ("MORTAR MARK", "HEAVY 10 DIRECT HIT, THEN A RING - GROUND MARKER >= 1 S AHEAD",
         {"vrell": [b_marker(V, k / 3) for k in range(4)],
          "asc": [b_marker(A, k / 3) for k in range(4)]}),
        ("LASER", "8 PER TOUCH - TELEGRAPH >= 0.8 S (LINE) / 0.6 S (SWEEP) - CHARGE - BEAM",
         {"vrell": [b_laser(LASER_V, 60, p) for p in ("tele", "charge", "beam")],
          "asc": [b_laser(A, 60, p) for p in ("tele", "charge", "beam")]}),
    ]
    return rows


def scatter(img, rows, seed, player_shots=True):
    """A typical mixed bullet field for the readability test."""
    rng = rnd(seed)
    w, h = img.size
    get = {r[0]: r[2] for r in rows}
    # Vrell fan from the top-left, Asc ring burst on the right, needles, homing, acid
    for k in range(5):
        a = np.radians(-40 + k * 20)
        put(img, get["ORB"]["vrell"][k % 4], 70 + np.sin(a) * (40 + k * 4), 40 + np.cos(a) * 50)
    for k in range(8):
        a = k * TAU / 8
        put(img, get["ORB"]["asc"][k % 4], w - 80 + np.cos(a) * 34, 70 + np.sin(a) * 34)
    for k in range(3):
        put(img, get["NEEDLE"]["vrell"][0], 130 + k * 14, 110 + k * 18)
        put(img, get["NEEDLE"]["asc"][0], w - 150 - k * 16, 140 + k * 12)
    put(img, get["LARGE ORB"]["vrell"][1], w / 2 - 20, 70)
    put(img, get["LARGE ORB"]["asc"][2], w / 2 + 40, 150)
    put(img, get["HOMING"]["vrell"][1], 40, 160)
    put(img, get["HOMING"]["asc"][2], w / 2 + 6, 30)
    put(img, get["ACID / RAIL"]["vrell"][0], 200, 165)
    put(img, get["RING"]["asc"][0], w - 30, 175)
    put(img, get["RING"]["vrell"][2], w / 2 - 70, 140)
    if player_shots:
        bolt = cached("pbolt", lambda: v8.energy_bolt(12, 2.6))
        for k in range(4):
            put(img, bolt, w / 2 - 8 + (k % 2) * 16, 120 + k * 22)


def bullets_sheet():
    rows = bullet_set()
    W = 1300
    rh = 64
    y0 = 64
    scene_h = 200
    H = y0 + rh * (len(rows) - 1) + 96 + 40 + 2 * (scene_h + 24) + 70
    img = raster.sheet(W, H, "ENEMY BULLETS - VRELL AND ASCENDANCY, WITH A READABILITY TEST", SUB)
    cols = {"vrell": 430, "asc": 870}
    label(img, 16, 38, "TYPE / CLASS")
    label(img, cols["vrell"], 38, "VRELL: MAGENTA ORBS, YELLOW NEEDLES, LIME AREA DENIAL - 1X / 4X FRAMES",
          ES[3])
    label(img, cols["asc"], 38, "ASCENDANCY: ORANGE ORBS, GOLD SLUGS - 1X / 4X FRAMES", (255, 190, 120))
    y = y0
    for name, cls, fr in rows:
        tall = name == "LASER"
        hh = 96 if tall else rh
        ImageDraw.Draw(img).line([16, y - 6, W - 16, y - 6], fill=(40, 46, 64, 255))
        label(img, 16, y, name, raster.ACCENT, scale=2)
        words = cls.split(" - ")
        line, ly = "", y + 20
        for wd in words:
            if raster.text_width(line + " - " + wd) > 400 and line:
                label(img, 16, ly, line, LABEL)
                line, ly = wd, ly + 11
            else:
                line = wd if not line else line + " - " + wd
        label(img, 16, ly, line, LABEL)
        for fac, x in cols.items():
            frames = fr[fac]
            put(img, frames[0], x + 14, y + hh / 2 - 6) if not tall else None
            xx = x + 34
            big = max(max(g.size) for ff in fr.values() for g in ff)
            for k, f in enumerate(frames):
                if tall:
                    img.alpha_composite(on_checker(f, 1, 2), (xx, y - 2))
                    label(img, xx, y + 64, ("TELEGRAPH", "CHARGE", "BEAM")[k], DIM)
                    xx += 76
                else:
                    z = 4 if big <= 22 else 3 if big <= 30 else 2
                    zz = on_checker(f, z, 1)
                    while zz.height > hh - 6 and z > 1:
                        z -= 1
                        zz = on_checker(f, z, 1)
                    img.alpha_composite(zz, (xx, y - 2))
                    xx += zz.width + 8
        y += hh
    # readability test
    y += 4
    label(img, 16, y, "READABILITY TEST AT 1X OVER THE CHOSEN SCENE LOOPS (WITH THEIR OWN SPRITES): "
          "VRELL + ASCENDANCY BULLETS AND PULSE CANNON SHOTS (PALE BLUE) FOR CONTRAST", LABEL)
    y += 14
    sw = (W - 32 - 3 * 12) // 4
    for k, (sname, gif) in enumerate(v8.SCENES):
        cx = 16 + (k % 4) * (sw + 12)
        cy = y + (k // 4) * (scene_h + 24)
        bg = v8.scene_frame(gif, 20).crop((240 - sw // 2, 160, 240 + sw // 2 + sw % 2, 160 + scene_h))
        scatter(bg, rows, k)
        img.alpha_composite(bg, (cx, cy + 12))
        label(img, cx, cy, sname)
    notes(img, H - 52, [
        "ALL ENEMY BULLETS: BRIGHT WHITE CORE, SATURATED RING, 1 PX DARK RIM (300030 VRELL / 340E00 ASC), A FAINT OUTER GLOW OF THE RING HUE.",
        "SHAPE ENCODES THREAT: SMALL ROUND = STANDARD, ELONGATED = FAST, LARGE PULSING = SLOW AND HEAVY, DIAMOND = HOMING (SHOOTABLE), HOLLOW RING = BURST.",
        "MINIMUM 8 PX; ENEMY BULLETS ARE DRAWN ABOVE EVERY LAYER EXCEPT THE HUD. PLAYER SHOTS STAY PALE BLUE / WHITE, SOFT AND RIMLESS.",
        "IN VRELL SPACE AND EUROPA (MAGENTA / TEAL BACKGROUND GLOW) THE BULLETS GET +2 PX AND A BRIGHTER RING, PER THE WORLD DOCS."])
    return img


@item("bullets")
def do_bullets():
    save_png(bullets_sheet(), "enemies", "enemy-bullets-r09-a.png")


# =========================================================================== 4. explosions

LADDER9 = [("TINY", 24, 12), ("SMALL", 40, 12), ("MEDIUM", 64, 14), ("LARGE", 96, 14),
           ("HUGE", 144, 16)]


def explosion_frames9(size, n, seed=1, style="fire", debris=True, shock=None):
    """Pre-rendered fireball sequence built from billowing puffs: white flash -> fireball ->
    cooling smoke, with sparks / chunks (fire, asc) or ichor drops / chitin shards (vrell).
    Round-09 copy of vfx_r08.explosion_frames with two fixes: no debris on the white flash
    frame (it showed as a dotted ring), and debris highlights no longer use the reserved
    magenta (chunks get a warm grey, Vrell shards a violet)."""
    rng = rnd(seed)
    R = size / 2
    ramp = v8.STYLE[style]
    tex = v8._cart_noise(size, seed)
    tex2 = v8._cart_noise(size, seed + 1, cell=max(6, size * SS // 8))
    shock = size >= 90 if shock is None else shock
    puffs = []
    for k in range(5 + size // 16):
        a = rng.uniform(0, TAU)
        r0 = rng.uniform(0.1, 0.5) * R if k else 0.0
        puffs.append((a, r0, rng.uniform(0.28, 0.46) * R, rng.uniform(0, 0.25)))
    parts = []
    for _ in range(int(5 + size * 0.22)):
        kinds = {"vrell": ["drop", "drop", "shard"], "asc": ["spark", "spark", "plate"],
                 "fire": ["spark", "chunk"]}[style]
        parts.append((rng.uniform(0, TAU), rng.uniform(0.45, 1.0), rng.choice(kinds),
                      rng.uniform(0.6, 1.4)))
    out = []
    for i in range(n):
        t = i / max(n - 1, 1)
        cv = Canvas(size, size)
        dens = np.zeros_like(cv.x)
        sdens = np.zeros_like(cv.x)
        for a, r0, rb, delay in puffs:
            tt = np.clip((t - delay * 0.4) / (1 - delay * 0.4), 0, 1)
            rad = rb * (0.4 + 0.8 * (1 - (1 - tt) ** 2.2))
            cx = R + np.cos(a) * r0 * (0.5 + 0.8 * tt)
            cy = R + np.sin(a) * r0 * (0.5 + 0.8 * tt)
            dd = cv.dist(cx, cy) / rad
            dens = np.maximum(dens, np.exp(-dd * dd * 1.4))
            sdens = np.maximum(sdens, np.exp(-(dd / 1.3) ** 2 * 1.4))
        churn = v8._shift(tex, t * size * 0.6, -t * size * 0.4)
        dn = dens * (0.55 + 0.9 * churn)
        sn = sdens * (0.55 + 0.9 * v8._shift(tex2, -t * size, t * size * 0.5))
        if t > 0.15:                                       # cooling smoke behind the fire
            st = (t - 0.15) / 0.85
            sm = np.clip((sn - 0.26) * 6, 0, 1) * (0.95 - 0.75 * st)
            shade = 0.7 + 0.6 * np.clip(v8._shift(tex2, -6, -6) - tex2 + 0.5, 0, 1)
            cv.over(np.clip(np.array(v8.SMOKE[style]) / 255 * shade[..., None], 0, 1), sm)
        heat = np.clip(dn * 1.3 - 0.05 - t * 1.05, 0, 1)
        cover = np.clip((dn - 0.3) * 8, 0, 1) * np.clip(heat * 3, 0, 1)
        lit = np.clip(0.9 + 0.8 * (v8._shift(churn, 5, 5) - churn), 0.65, 1.25)  # top-left light
        col = raster.ramp(ramp, heat) / 255.0 * lit[..., None]
        cv.rgb = cv.rgb * (1 - cover[..., None]) + np.clip(col, 0, 1) * cover[..., None]
        cv.a = cv.a * (1 - cover) + cover
        d = cv.dist(R, R) / R
        if i == 0:
            cv.add(WHITE, gauss(d, 0.42) * 1.5)
        elif i == 1:
            cv.add((255, 240, 200), gauss(d, 0.55) * 0.6)
        if shock and 0.05 < t < 0.6:
            rs = 0.3 + 1.1 * t
            band = gauss(d - rs, 0.1) * (0.4 + 0.8 * tex2)
            cv.add((210, 225, 255), band * 0.3 * (1 - t / 0.6))
        if debris and i > 0:
            for a, sp, kind, sz in parts:
                r0 = (0.15 + 0.95 * sp * (1 - (1 - t) ** 1.6)) * R
                px, py = R + np.cos(a) * r0, R + np.sin(a) * r0
                fade = np.clip(1.2 - t * 1.1, 0, 1)
                if kind == "spark":
                    ln = R * 0.2 * sz * (1 - t)
                    dd = cv.seg(px, py, px - np.cos(a) * ln, py - np.sin(a) * ln)
                    cv.add((255, 236, 170), gauss(dd, 0.5) * 1.3 * fade)
                elif kind == "drop":
                    dd = cv.dist(px, py)
                    cv.over(LIME if sz > 1 else (0, 200, 140), solid(dd, 0.6 + sz * 0.5) * fade)
                else:
                    c = {"chunk": (60, 50, 70), "plate": (90, 70, 30),
                         "shard": (160, 32, 168)}[kind]
                    s = 0.9 + sz * 0.9 * (size / 64) ** 0.5
                    rot = a + t * 6 * sz
                    pts = [(px + np.cos(rot + k * 2.1) * s, py + np.sin(rot + k * 2.1) * s * 0.7)
                           for k in range(3)]
                    dd = cv.poly(pts)
                    cv.over(c, solid(dd, 0.0) * fade)
                    cv.over({"plate": (230, 210, 150), "chunk": (170, 150, 140),
                             "shard": (190, 140, 255)}[kind],
                            solid(dd, 0.0) * (dd > -0.6) * fade * 0.6)
        out.append(cv.image())
    return out



def boom(size, n, seed, style="fire", **kw):
    return cached(("boom", size, n, seed, style), lambda: explosion_frames9(size, n, seed, style,
                                                                             **kw))


def under_burst(size, n, seed):
    """Under-water burst: vfx_r08's bubble cloud over a dark silt / murk cloud, with a
    stronger first-frames flash and a pale pressure ring, so it reads over busy Europa."""
    base = v8.water_frames(size, n, seed, under=True)
    tex = v8._cart_noise(size, seed + 3)
    out = []
    for i, fr in enumerate(base):
        t = i / max(n - 1, 1)
        cv = Canvas(size, size)
        R = size / 2
        d = cv.dist(R, R) / R
        murk = np.clip((0.35 + 0.55 * t - d) * 5, 0, 1) * (0.6 + 0.5 * tex) * (1 - t) ** 0.7
        cv.over((6, 26, 34), np.clip(murk, 0, 0.75))
        img = cv.image()
        img.alpha_composite(fr)
        fx = Canvas(size, size)
        fx.add((150, 255, 235), gauss(d, 0.3 + 0.2 * t) * max(0, 1.6 - t * 5))
        fx.add((200, 255, 250), gauss(d - (0.2 + 0.9 * t), 0.06) * (1 - t) * 0.8)
        v8.additive(img, fx.image(), size / 2, size / 2)
        out.append(img)
    return out


def splash(size, n, seed, under=False):
    if under:
        return cached(("water", size, n, seed, under), lambda: under_burst(size, n, seed))
    return cached(("water", size, n, seed, under), lambda: v8.water_frames(size, n, seed, under))


def plate(w, h, color):
    im = Image.new("RGBA", (w, h), tuple(color) + (255,))
    return im


def frame_row(img, x, y, frames, bgcol, maxw, z=1, idx=None):
    """Frames side by side on a dark plate; returns the row height."""
    fw = frames[0].width * z
    k = min(len(frames), max(1, (maxw + 4) // (fw + 4)))
    idx = idx or [int(round(i)) for i in np.linspace(0, len(frames) - 1, k)]
    for j, i in enumerate(idx):
        p_ = plate(fw, frames[0].height * z, bgcol)
        f = sprite.enlarge(frames[i], z) if z > 1 else frames[i]
        p_.alpha_composite(f)
        img.alpha_composite(p_, (x + j * (fw + 4), y))
        label(img, x + j * (fw + 4) + 2, y + 2, str(i + 1), DIM)
    return frames[0].height * z


def explosions_sheet():
    W, H = 1300, 1640
    img = raster.sheet(W, H, "EXPLOSIONS - SIZE LADDER, FACTIONS, WATER, HIT FLASH, SHIELD", SUB)
    SPACE = (10, 12, 26)
    x0, maxw = 130, W - 130 - 16
    y = 40
    label(img, 16, y, "SIZE LADDER - PRE-RENDERED FIREBALL, 12-16 FRAMES, ADDITIVE FLASH; DEBRIS, AND A SHOCKWAVE RING "
          "FROM LARGE UP (FRAME NUMBERS SHOWN; LONG ROWS SAMPLED)", LABEL)
    y += 16
    for k, (name, size, n) in enumerate(LADDER9):
        fr = boom(size, n, 10 + k)
        label(img, 16, y + 4, name, raster.ACCENT, scale=2)
        label(img, 16, y + 24, f"{size} PX, {n} FR", LABEL)
        h = frame_row(img, x0, y, fr, SPACE, maxw)
        y += max(h, 40) + 10
    y += 8
    label(img, 16, y, "VRELL ORGANIC (VIOLET -> TEAL-GREEN FIRE, ICHOR DROPS, CHITIN SHARDS)  VS  ASCENDANCY METAL "
          "(HOT ORANGE, BLACK SMOKE, GOLD PLATES, SPARKS) - MEDIUM 64 PX", LABEL)
    y += 16
    for name, style, seed in (("VRELL", "vrell", 21), ("ASCEND.", "asc", 22)):
        fr = boom(64, 14, seed, style)
        label(img, 16, y + 4, name, raster.ACCENT, scale=2)
        label(img, 16, y + 24, "64 PX, 14 FR", LABEL)
        frame_row(img, x0, y, fr, SPACE, maxw)
        y += 74
    fr = boom(96, 14, 23, "vrell")
    label(img, 16, y + 4, "VRELL L", raster.ACCENT, scale=2)
    label(img, 16, y + 24, "96 PX, 14 FR", LABEL)
    frame_row(img, x0, y, fr, SPACE, maxw)
    y += 106 + 8
    label(img, 16, y, "WATER: SURFACE HIT (WHITE COLUMN, SPRAY, FOAM, BROKEN RIPPLE ARCS)  /  UNDER WATER (CYAN "
          "FLASH, PRESSURE RING, DARK SILT CLOUD, BUBBLES RISING) - 64 PX, 12 FR", LABEL)
    y += 16
    for name, under, col in (("SURFACE", False, (16, 52, 92)), ("UNDER", True, (8, 58, 70))):
        fr = splash(64, 12, 31 + under, under)
        label(img, 16, y + 4, name, raster.ACCENT, scale=2)
        label(img, 16, y + 24, "64 PX, 12 FR", LABEL)
        frame_row(img, x0, y, fr, col, maxw)
        y += 74
    y += 8
    label(img, 16, y, "HIT FLASH - EVERY DAMAGED ENEMY: NORMAL -> WHITE SILHOUETTE -> HALF -> NORMAL "
          "(1-2 GAME FRAMES EACH), 2X", LABEL)
    y += 16
    x = x0
    for key in ("needler-a", "gilded-gunship-a"):
        for f in v8.hit_flash_frames(enemy(key)):
            z = on_checker(f, 2, 2)
            img.alpha_composite(z, (x, y))
            x += z.width + 4
        x += 16
    label(img, 16, y + 4, "HIT", raster.ACCENT, scale=2)
    y += 156
    ship = player(0)
    label(img, 16, y, "SHIELD HIT - HEX CELLS LIGHT UP AROUND THE HIT POINT, A RIPPLE RUNS OVER THE BUBBLE "
          "(6 FRAMES, 2X, PLAYER-BLUE)", LABEL)
    y += 16
    label(img, 16, y + 4, "SHIELD", raster.ACCENT, scale=2)
    label(img, 16, y + 24, "HIT, 6 FR", LABEL)
    frame_row(img, x0, y, v8.shield_frames(ship), SPACE, maxw, z=2)
    y += 2 * (ship.height + 28) + 12
    label(img, 16, y, "SHIELD BREAK - SHIELD AT ZERO: SHELL FLARES, CRACKS, SHATTERS INTO SHARDS "
          "(8 FRAMES, 6 SHOWN, 2X); ARMOUR NOW TAKES THE HITS", LABEL)
    y += 16
    label(img, 16, y + 4, "SHIELD", raster.ACCENT, scale=2)
    label(img, 16, y + 24, "BREAK, 8 FR", LABEL)
    frame_row(img, x0, y, v8.shield_break_frames(ship), SPACE, maxw, z=2, idx=[0, 1, 2, 3, 5, 7])
    y += 2 * (ship.height + 44) + 12
    notes(img, y, [
        "FIRE = UTC / HUMAN CRAFT, VEHICLES AND GROUND TARGETS. EXPLOSIONS ARE LIGHT, NOT BULLETS: THEY MAY COVER THE PLAY PLANE BRIEFLY,",
        "BUT ENEMY BULLETS ARE ALWAYS DRAWN ABOVE THEM. THE SHOCKWAVE RING IS PALE BLUE-WHITE AND FADES BY 60 % OF THE SEQUENCE."])
    return img.crop((0, 0, W, y + 40))


def explosions_gif():
    frames = []
    # segment 1: the ladder over deep space (staggered)
    plan = [(0, 24, 80, 150, 0), (4, 24, 130, 110, 0), (8, 40, 360, 140, 1), (14, 64, 240, 220, 2),
            (24, 96, 130, 330, 3), (34, 144, 330, 350, 4)]
    for f in range(56):
        img = scroll_bg("space", f, seed=21)
        for t0, size, x, y, k in plan:
            n = LADDER9[k][2]
            i = f - t0
            if 0 <= i < n:
                put(img, boom(size, n, 10 + k)[i], x, y)
            if 0 <= i < n + 6:
                label(img, int(x - size / 2), int(y + size / 2 + 2), f"{LADDER9[k][0]} {size}", LABEL)
        label(img, 8, 8, "EXPLOSION LADDER", raster.ACCENT, scale=2)
        label(img, 8, 28, "TINY 24 -> HUGE 144, 12-16 FRAMES", LABEL)
        frames.append(img)
    # segment 2: factions, hit flash, shield hit and break over the megacity
    nd, gs, tl = enemy("needler-a"), enemy("gilded-gunship-a"), enemy("talon-a")
    ship = player(0)
    sh_hit = v8.shield_frames(ship)
    sh_brk = v8.shield_break_frames(ship)
    hits = cached("hits", lambda: v8.impact_frames("energy"))
    for f in range(60):
        img = scroll_bg("orbit", f, seed=6)
        # Vrell needlers die left, Ascendancy talon + gunship die right
        units = [(nd, 110, 150, 6, "vrell", 64), (nd, 170, 220, 14, "vrell", 64),
                 (tl, 320, 160, 10, "asc", 64), (gs, 360, 260, 34, "asc", 96)]
        for sp, x, y, tdie, style, size in units:
            if f < tdie:
                fl = (sp is gs and f % 5 in (1, 2))
                spr = v8.hit_flash_frames(sp)[1 if f % 5 == 1 else 2] if fl else sp
                v8.shadow(img, spr, x, y, 21, 30, 0.45)
                put(img, spr, x, y)
                if sp is gs and f % 5 == 1:
                    put(img, hits[0], x + (f % 3 - 1) * 8, y + 30)
            else:
                fr = boom(size, 14, 40 + int(x), style)
                if f - tdie < 14:
                    put(img, fr[f - tdie], x, y)
        # the player: shield hit at f 12, a second hit at 30, shield break at 44
        sx, sy = 240, 450
        spr = None
        for t0, seq in ((12, sh_hit), (30, sh_hit), (44, sh_brk)):
            if 0 <= f - t0 < len(seq):
                spr = seq[f - t0]
        v8.shadow(img, ship, sx, sy)
        put(img, spr if spr is not None else ship, sx, sy)
        for t0, (bx, by0) in ((12, (226, 300)), (30, (262, 300)), (44, (240, 290))):
            u = (f - t0 + 8) / 8
            if 0 <= u < 1:
                put(img, b_orb(FAC["vrell"], 4.5), bx + (sx - bx) * u, by0 + (sy - 30 - by0) * u)
        label(img, 8, 8, "VRELL VS ASCENDANCY, HIT FLASH", raster.ACCENT, scale=2)
        label(img, 8, 28, "SHIELD HIT X2, THEN SHIELD BREAK", LABEL)
        frames.append(img)
    # segment 3: water surface, then under water
    for f in range(48):
        under = f >= 24
        img = bg_frame("scene-europa-r07-a.gif" if under else "scene-ocean-r08-a.gif", f * 2)
        g = f % 24
        for t0, x, y, size in ((0, 150, 190, 96), (5, 330, 280, 64), (10, 210, 400, 96)):
            fr = splash(size, 12, 31 + under + size, under)
            if 0 <= g - t0 < 12:
                put(img, fr[g - t0], x, y)
        label(img, 8, 8, "UNDER WATER" if under else "WATER SURFACE", raster.ACCENT, scale=2)
        label(img, 8, 28, "FLASH, PRESSURE RING, SILT, BUBBLES" if under else "COLUMN, SPRAY, FOAM, RIPPLES",
              LABEL)
        frames.append(img)
    return frames


@item("explosions")
def do_explosions():
    save_png(explosions_sheet(), "art", "explosions-r09-a.png")
    save_gif(explosions_gif(), "art", "explosions-r09-a.gif")


# =========================================================================== 5. pickups

FRIEND = v8.FRIEND
m_metal, m_glow = v8.m_metal, v8.m_glow


def _ring(p, r, w, z, h):
    rr = np.hypot(p[:, 0], p[:, 1])
    return np.maximum(np.abs(rr - r) - w, np.abs(p[:, 2] - z) - h)


def _hexagon(r, rot=np.pi / 6):
    return [(np.cos(rot + k * np.pi / 3) * r, np.sin(rot + k * np.pi / 3) * r) for k in range(6)]


def _chip(p, s=1.0, o=(0, 0, 0)):
    q = (p - np.array(o)) / s
    d = [(sd_plate(q, _hexagon(0.8), 0.0, 0.16, 0.05) * s, 0),
         (_ring(q, 0.52, 0.09, 0.19, 0.06) * s, 2),
         (sd_cylinder_z(q, (0, 0, 0.1), 0.26, 0.08) * s, 1)]
    return d


@v8.oriented
def chip_model(count=1):
    """Salvage: hexagonal credit chips (silver, dark centre, cyan-white inlay ring)."""
    mats = [m_metal((205, 210, 226)), m_metal((60, 66, 86)), m_glow((60, 230, 255), 1.3)]
    offs = {1: [(0, 0, 0, 1.0)],
            3: [(-0.3, -0.32, -0.12, 0.62), (0.3, -0.2, -0.04, 0.62), (0.0, 0.3, 0.06, 0.62)]}[count]

    def scene(p):
        items = []
        for ox, oy, oz, sc in offs:
            items += _chip(p, sc, (ox, oy, oz))
        return union(*items, k=0.0)
    return scene, mats


@v8.oriented
def crate_model():
    """Large salvage: a small armoured cargo crate with straps and a cyan-white stripe."""
    mats = [m_metal((110, 116, 134)), m_metal((44, 48, 62)), m_glow((60, 230, 255), 1.3)]

    def scene(p):
        return union(
            (sd_box(p, (0, 0, 0), (0.74, 0.6, 0.42), 0.08), 0),
            (sd_box(p, (0, 0.38, 0), (0.78, 0.07, 0.46), 0.02), 1),
            (sd_box(p, (0, -0.38, 0), (0.78, 0.07, 0.46), 0.02), 1),
            (sd_box(p, (0, 0, 0.43), (0.52, 0.06, 0.03), 0.01), 2),
            (sd_box(p, (0, 0, 0.43), (0.06, 0.24, 0.03), 0.01), 2))
    return scene, mats


@v8.oriented
def cell_model():
    """Shield cell: glass capsule of blue shield energy between silver end caps."""
    mats = [m_metal((210, 216, 230)), m_glow(PS[2], 1.9), m_metal((60, 66, 86))]

    def scene(p):
        return union(
            (sd_capsule(p, (0, -0.42, 0), (0, 0.42, 0), 0.36), 1),
            (sd_cylinder_y(p, (0, 0.6, 0), 0.42, 0.13), 0),
            (sd_cylinder_y(p, (0, -0.6, 0), 0.42, 0.13), 0),
            (sd_cylinder_y(p, (0, 0.8, 0), 0.18, 0.08), 2),
            (sd_cylinder_y(p, (0, 0.0, 0), 0.4, 0.05), 2))
    return scene, mats


@v8.oriented
def patch_model():
    """Armour patch: riveted steel plate with a raised green repair cross."""
    mats = [m_metal((150, 156, 170)), m_glow(FRIEND, 1.6), m_metal((70, 74, 90))]

    def scene(p):
        items = [(sd_box(p, (0, 0, 0), (0.72, 0.72, 0.12), 0.1), 0),
                 (sd_box(p, (0, 0, 0.15), (0.44, 0.14, 0.07), 0.03), 1),
                 (sd_box(p, (0, 0, 0.15), (0.14, 0.44, 0.07), 0.03), 1)]
        for sx in (-0.55, 0.55):
            for sy in (-0.55, 0.55):
                items.append((sd_sphere(p, (sx, sy, 0.1), 0.08), 2))
        return union(*items)
    return scene, mats


@v8.oriented
def overdrive_model():
    """Overdrive: two stacked chrome chevrons pointing forward, white-violet glowing cores."""
    mats = [m_metal((200, 204, 222)), m_glow((150, 100, 255), 1.3)]

    def chev(y, w):
        return [(0, y + 0.42), (w, y - 0.08), (w, y - 0.36), (0, y + 0.12), (-w, y - 0.36),
                (-w, y - 0.08)]

    def scene(p):
        return union(
            (sd_plate(p, chev(0.28, 0.78), 0.0, 0.13, 0.04), 0),
            (sd_plate(p, chev(-0.32, 0.78), 0.0, 0.13, 0.04), 0),
            (sd_plate(p, chev(0.3, 0.5), 0.12, 0.07, 0.02), 1),
            (sd_plate(p, chev(-0.3, 0.5), 0.12, 0.07, 0.02), 1))
    return scene, mats


@v8.oriented
def charge_model():
    """Special charge: a gold-trimmed ring around an amber four-point star."""
    mats = [m_metal((200, 204, 220)), m_glow((255, 196, 60), 2.0)]
    star = []
    for k in range(8):
        r = 0.52 if k % 2 == 0 else 0.17
        a = np.pi / 2 + k * np.pi / 4
        star.append((np.cos(a) * r, np.sin(a) * r))

    def scene(p):
        return union((_ring(p, 0.72, 0.12, 0.0, 0.12), 0),
                     (sd_plate(p, star, 0.0, 0.1, 0.02), 1))
    return scene, mats


@v8.oriented
def core_model():
    """Data core: a pale glowing crystal (octahedron) held by a dark metal clamp."""
    mats = [v8.mat((200, 250, 240), metal=0.2, shininess=90, spec=1.0,
                   emission=(0.35, 0.6, 0.55)), m_metal((60, 66, 86))]

    def scene(p):
        oct_ = (np.abs(p[:, 0]) + np.abs(p[:, 1]) * 0.62 + np.abs(p[:, 2]) - 0.62) * 0.55
        return union((oct_, 0), (sd_cylinder_y(p, (0, 0, 0), 0.5, 0.07), 1),
                     (sd_cylinder_y(p, (0, 0, 0), 0.36, 0.14), 1))
    return scene, mats


# key -> (title, effect, model, kwargs, native size)
PICKUPS = [
    ("salvage-s", "SALVAGE S", "+10 CREDITS - ENEMY DROPS", chip_model, {"count": 1}, 18),
    ("salvage-m", "SALVAGE M", "+50 CREDITS - ENEMY DROPS, CRATES", chip_model, {"count": 3}, 21),
    ("salvage-l", "SALVAGE L", "+200 CREDITS - CRATES, LARGE KILLS", crate_model, {}, 24),
    ("shield", "SHIELD CELL", "RESTORES 25 % SHIELD - FREQUENT", cell_model, {}, 22),
    ("armour", "ARMOUR PATCH", "RESTORES 10 ARMOUR - RARE (0-2 / LEVEL)", patch_model, {}, 22),
    ("overdrive", "OVERDRIVE", "ALL WEAPONS +1 LEVEL FOR 20 S - ~2 / LEVEL", overdrive_model, {}, 22),
    ("charge", "SPECIAL CHARGE", "+1 SPECIAL CHARGE - RARE", charge_model, {}, 22),
    ("core", "DATA CORE", "LORE + SHOP UNLOCK - HIDDEN AREAS", core_model, {}, 22),
]
NPK = 8                     # turntable frames (spin about the screen-vertical axis)
FLAT = ("salvage-s", "salvage-m", "armour", "overdrive", "charge")   # rock +-55 deg, never edge-on


def pickup_frames(key):
    def build():
        row = next(r for r in PICKUPS if r[0] == key)
        _, _, _, model, kw, n = row
        out = []
        for i in range(NPK):
            spin = (np.radians(55) * np.sin(i * TAU / NPK) if key in FLAT else i * TAU / NPK)
            scene, mats = model(tilt=-0.35, spin=spin, **kw)
            sp = v8.render_obj(scene, mats, n, 2.0, colors=24)
            out.append(pickup_fx(sp, i))
        return out
    return cached(("pickup", key), build)


def pickup_fx(sp, i):
    """Pickup presentation: soft white halo + 1 px light outline, both pulsing (8 frames)."""
    pad = 5
    w, h = sp.width + 2 * pad, sp.height + 2 * pad
    base = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    base.alpha_composite(sp, (pad, pad))
    a = base.getchannel("A").point(lambda v: 255 if v > 90 else 0)
    pulse = 0.5 + 0.5 * np.cos(i * TAU / NPK)
    halo_a = a.filter(ImageFilter.GaussianBlur(2.6)).point(lambda v: int(min(255, v * (0.5 + 0.6 * pulse))))
    halo = Image.new("RGBA", (w, h), (225, 245, 255, 0))
    halo.putalpha(halo_a)
    ring_a = Image.fromarray(np.clip(np.array(a.filter(ImageFilter.MaxFilter(3))).astype(int)
                                     - np.array(a), 0, 255).astype(np.uint8))
    ring = Image.new("RGBA", (w, h), (235, 250, 255, 0))
    ring.putalpha(ring_a.point(lambda v: int(v * (0.55 + 0.45 * pulse))))
    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    out.alpha_composite(halo)
    out.alpha_composite(ring)
    out.alpha_composite(base)
    return out


def pickups_sheet():
    W = 1300
    rh = 112
    H = 50 + rh * len(PICKUPS) + 380
    img = raster.sheet(W, H, "IN-LEVEL PICKUPS - TURNTABLE SPIN, PULSING LIGHT OUTLINE", SUB)
    label(img, 16, 38, "PICKUP / EFFECT")
    label(img, 300, 38, "4X")
    label(img, 420, 38, f"{NPK}-FRAME SPIN + PULSE LOOP (2X)")
    label(img, 1010, 38, "1X OVER ORBIT AND COAST")
    y = 54
    for k, (key, title, eff, model, kw, n) in enumerate(PICKUPS):
        fr = pickup_frames(key)
        ImageDraw.Draw(img).line([16, y - 6, W - 16, y - 6], fill=(40, 46, 64, 255))
        label(img, 16, y, title, raster.ACCENT, scale=2)
        label(img, 16, y + 22, eff, LABEL)
        label(img, 16, y + 36, f"{n} PX MODEL, {fr[0].width} PX WITH OUTLINE", DIM)
        img.alpha_composite(on_checker(fr[0], 4), (300, y))
        for i, f in enumerate(fr):
            img.alpha_composite(on_checker(f, 2), (420 + i * (f.width * 2 + 6), y + 10))
        for j, kind in enumerate(("orbit", "coast")):
            cell = scroll_bg(kind, k * 31, seed=4 + 5 * j, w=130, h=rh - 12)
            for i in range(3):
                put(cell, fr[i * 3], 24 + i * 40, 30 + (i % 2) * 30)
            img.alpha_composite(cell, (1010 + j * 140, y - 2))
        y += rh
    # confusion test
    y += 4
    label(img, 16, y, "NEVER CONFUSED WITH BULLETS - 1X AND 3X, COLOUR AND GREYSCALE: PICKUPS ARE LIT, SPINNING 3D OBJECTS "
          "WITH A WHITE OUTLINE; BULLETS ARE FLAT, ROUND, DARK-RIMMED GLOWS", LABEL)
    y += 14
    rows = bullet_set()
    get = {r[0]: r[2] for r in rows}
    test = scroll_bg("orbit", 0, seed=17, w=300, h=90)
    items = [pickup_frames("salvage-s")[0], get["ORB"]["vrell"][0], pickup_frames("shield")[1],
             get["ORB"]["asc"][0], pickup_frames("armour")[0], get["ACID / RAIL"]["vrell"][0],
             pickup_frames("salvage-m")[2], get["HOMING"]["asc"][0], pickup_frames("charge")[0],
             get["MINE"]["vrell"][0], pickup_frames("overdrive")[0], get["LARGE ORB"]["vrell"][0],
             pickup_frames("core")[0]]
    for i, sp in enumerate(items):
        put(test, sp, 18 + i * 22, 30 + (i % 2) * 30)
    img.alpha_composite(test, (16, y))
    img.alpha_composite(sprite.enlarge(test.crop((0, 0, 150, 90)), 3), (330, y))
    img.alpha_composite(test.convert("L").convert("RGBA"), (16, y + 100))
    img.alpha_composite(sprite.enlarge(test.crop((150, 0, 300, 90)), 3).convert("L").convert("RGBA"),
                        (800, y))
    notes(img, y + 284, [
        "RULES: 18-24 PX MODELS (+5 PX OUTLINE / HALO). EVERY PICKUP TURNS ON AN 8-FRAME LOOP (ABOUT 10 FPS): FLAT ONES ROCK +-55 DEG SO THEY ARE NEVER",
        "SEEN EDGE-ON (A THIN BRIGHT SLIVER WOULD READ AS A NEEDLE), SOLID ONES SPIN FULLY. THE WHITE OUTLINE + HALO PULSE ON THE SAME LOOP.",
        "NO PICKUP USES A BULLET HUE AS ITS MAIN COLOUR: BODIES ARE SILVER / STEEL, ONLY THE INLAYS CARRY THE TYPE COLOUR (SALVAGE CYAN, SHIELD BLUE,",
        "ARMOUR GREEN, OVERDRIVE VIOLET, CHARGE AMBER, DATA CORE PALE CRYSTAL). PICKUPS DRIFT DOWN AT 40 PX/S AND LEAVE AFTER 6 S, BLINKING IN THE",
        "LAST 1.5 S. A COLLECTED PICKUP POPS (WHITE RING) AND SHOWS ITS VALUE (HUD FLOATING NUMBERS, CAN BE TURNED OFF).",
    ])
    return img.crop((0, 0, W, y + 284 + 70))


def pickup_pop(n=6):
    out = []
    for i in range(n):
        t = i / (n - 1)
        cv = Canvas(40, 40)
        d = cv.dist(20, 20)
        cv.add(WHITE, gauss(d - (4 + 14 * t), 1.2) * (1 - t) * 1.4)
        cv.add((225, 245, 255), gauss(d, 5 * (1 - t) + 0.5) * (1 - t) * 1.2)
        out.append(cv.image())
    return out


def pickups_gif():
    frames = []
    nd, tl, gs = enemy("needler-a"), enemy("talon-a"), enemy("gilded-gunship-a")
    pop = pickup_pop()
    drops = [  # (unit sprite, x, y, death frame, pickup key, value text)
        (nd, 90, 120, 8, "salvage-s", "+10"), (tl, 200, 90, 14, "salvage-m", "+50"),
        (nd, 330, 140, 20, "shield", "SHIELD +25 %"), (gs, 380, 230, 30, "salvage-l", "+200"),
        (tl, 130, 220, 38, "overdrive", "OVERDRIVE"), (nd, 260, 170, 46, "armour", "ARMOUR +10"),
        (nd, 60, 300, 54, "charge", "SPECIAL +1"), (tl, 300, 60, 62, "core", "DATA CORE"),
    ]
    rows = bullet_set()
    get = {r[0]: r[2] for r in rows}
    n = 120
    # the ship steers towards the nearest pickup on screen (max 220 px/s)
    collected = {}
    sx, sy, vx = 240.0, 440.0, 0.0
    for f in range(n):
        t = f / FPS
        img = scroll_bg("orbit", f, seed=4)
        live = [(x, y + (f - tdie) * 2.0) for k, (sp, x, y, tdie, key, txt) in enumerate(drops)
                if f >= tdie + 4 and k not in collected and y + (f - tdie) * 2.0 < 520]
        if live:
            tx, ty = min(live, key=lambda q: np.hypot(q[0] - sx, q[1] - sy))
            ty = max(ty + 10, 300)
        else:
            tx, ty = 240, 440
        dx, dy = tx - sx, ty - sy
        dist = max(np.hypot(dx, dy), 1e-6)
        step = min(dist, 11.0)
        vx = dx / dist * step * FPS
        sx, sy = sx + dx / dist * step, sy + dy / dist * step
        for k, (sp, x, y, tdie, key, txt) in enumerate(drops):
            if f < tdie:
                v8.shadow(img, sp, x, y + 2 * np.sin(t * 3 + k), 21, 30, 0.45)
                put(img, sp, x, y + 2 * np.sin(t * 3 + k))
                continue
            age = f - tdie
            if age < 12:
                put(img, boom(40, 12, 50 + k, "vrell" if sp is nd else "asc")[age], x, y)
            if k in collected:
                ca = f - collected[k][0]
                if ca < 6:
                    put(img, pop[ca], *collected[k][1])
                if ca < 18:
                    px_, py_ = collected[k][1]
                    raster.draw_text(img, int(px_ - raster.text_width(txt) / 2), int(py_ - 16 - ca),
                                     txt, (255, 255, 0), shadow=(30, 20, 0))
                continue
            px_, py_ = x, y + age * 2.0
            fr = pickup_frames(key)
            if np.hypot(px_ - sx, py_ - (sy - 6)) < 26:
                collected[k] = (f, (px_, py_))
                continue
            put(img, fr[(age // 2) % NPK], px_, py_)
        # some enemy bullets falling through the field for contrast
        for b in range(10):
            bx = (37 + b * 47) % 470 + 5
            by = (f * 3.2 + b * 61) % 600 - 40
            key = ("ORB", "vrell") if b % 3 == 0 else ("ORB", "asc") if b % 3 == 1 else ("NEEDLE", "vrell")
            put(img, get[key[0]][key[1]][f % 4 if key[0] != "NEEDLE" else 0], bx, by)
        ship = player(v8.bank_frame(vx * 0.35))
        v8.shadow(img, ship, sx, sy)
        put(img, ship, sx, sy)
        label(img, 8, 8, "PICKUPS", raster.ACCENT, scale=2)
        label(img, 8, 28, "DROPPED, DRIFTING, COLLECTED - AMONG ENEMY BULLETS", LABEL)
        frames.append(img)
    return frames


@item("pickups")
def do_pickups():
    save_png(pickups_sheet(), "player", "pickups-r09-a.png")
    save_gif(pickups_gif(), "player", "pickups-r09-a.gif")


# =========================================================================== 6. specials

@v8.oriented
def bomber_model():
    """CDF bomber (Hammer flight): heavy straight-winged twin-engine bomber, grey hull, blue
    UTC wing bands and a dark canopy. Nose +Y."""
    HULL, DARK, BAND, GLASS, ENG = 0, 1, 2, 3, 4
    mats = [m_metal((150, 156, 170)), m_metal((64, 70, 88)), m_metal(v8.ACC_C[1]),
            v8.mat((30, 50, 90), metal=0.6, shininess=120, spec=1.2, emission=(0.05, 0.12, 0.25)),
            m_glow(PS[3], 1.8)]

    def scene(p):
        q = mirror_x(p)
        d, m = union(
            (sd_capsule(p, (0, -0.86, 0), (0, 0.78, 0), 0.19, 0.14), HULL),
            (sd_plate(q, [(0.12, 0.28), (1.02, 0.02), (1.02, -0.18), (0.12, -0.22)], 0.0, 0.05,
                      0.02), HULL),
            (sd_capsule(q, (0.46, -0.42, -0.06), (0.46, 0.26, -0.06), 0.105, 0.09), DARK),
            (sd_plate(q, [(0.05, -0.72), (0.42, -0.86), (0.42, -0.96), (0.05, -0.92)], 0.02,
                      0.03, 0.01), HULL),
            (sd_fin(q, [(-0.62, 0.05), (-0.92, 0.32), (-0.98, 0.32), (-0.9, 0.05)], 0.16, 0.025,
                    0.01), DARK),
            (sd_ellipsoid(p, (0, 0.5, 0.11), (0.1, 0.2, 0.08)), GLASS),
            (sd_cylinder_y(q, (0.46, -0.5, -0.06), 0.075, 0.03), ENG),
            k=0.03)
        band = (np.abs(q[:, 0] - 0.86) < 0.07) & (m == HULL) & (np.abs(p[:, 2]) < 0.1)
        m = np.where(band, BAND, m)
        return d, m
    return scene, mats


def bomber_sprite(n=64):
    return cached(("bomber", n), lambda: v8.render_obj(*bomber_model(), n, 2.3, colors=28))


def flame_frames(n=3):
    out = []
    for i in range(n):
        cv = Canvas(10, 22)
        ln = 12 + 4 * ((i * 7) % 3) / 2
        d = cv.seg(5, 3, 5, 3 + ln)
        cv.add(PS[2], gauss(d, 2.2) * 0.8)
        cv.over(PS[3], solid(cv.seg(5, 3, 5, 3 + ln * 0.5), 1.2) * 0.9)
        cv.over(WHITE, solid(cv.dist(5, 3.5), 1.2))
        out.append(cv.image())
    return out


def flare_sprite(i, age=1.0):
    """Decoy flare: white-hot magnesium core, flickering pale halo, sparkle spikes."""
    rng = rnd(100 + i)
    cv = Canvas(24, 24)
    d = cv.dist(12, 12)
    fl = 0.8 + 0.4 * rng.random()
    cv.add((255, 246, 230), gauss(d, 6.5 * fl) * 0.7)
    ang = np.arctan2(cv.y - 12, cv.x - 12) + rng.uniform(0, TAU)
    spikes = np.abs(np.cos(ang * 3)) ** 16 * gauss(d, 9 * fl)
    cv.add(WHITE, spikes * 0.9)
    cv.over(WHITE, solid(d, 2.2))
    return cv.image()


def smoke_trail(img, pts, alpha=0.5):
    for k, (x, y) in enumerate(pts):
        a = alpha * (1 - k / max(len(pts), 1))
        put(img, v8.smoke_puff(1.4 + k * 0.35, a), x, y)


def field_canvas_img(fn, ss=1):
    cv = Canvas(FW, FH, ss=ss)
    fn(cv)
    return cv.image()


def airstrike_frames():
    frames = []
    bomber = bomber_sprite()
    flames = flame_frames()
    bomb_sp = v8._obj(v8.bomb_model, 12, 2.0)
    tsp = [(enemy(k), x, y) for k, x, y in
           (("rail-bunker-a", 150, 160), ("spine-turret-a", 190, 280),
            ("polyp-mortar-a", 300, 220), ("rail-bunker-a", 285, 360))]
    air = [(enemy("needler-a"), 90, 120), (enemy("needler-a"), 380, 140)]
    sx, sy = 228, 470
    call, enter = 2, 2 + 12            # radio at f2, bombers enter 0.6 s later
    xs = [sx - 64, sx + 64]
    v_px = 30                          # 600 px/s at 20 fps
    drops = []                         # (x, y_release, frame_release)
    for bx in xs:
        for k in range(17):
            y_rel = FH + 30 - k * 36
            f_rel = enter + (FH + 30 - y_rel) / v_px
            drops.append((bx + (5 if k % 2 else -5), y_rel - 20, f_rel))
    dead = {}
    for f in range(48):
        img = scroll_bg("coast", f, seed=12, speed=3)
        for j, (sp, x, y) in enumerate(tsp):
            if j not in dead:
                put(img, sp, x, y)
        for sp, x, y in air:
            fl = any(0 <= f - (fr + 5) < 2 and abs(x - bx) < 60 for bx, yy, fr in drops if yy < y + 30
                     and yy > y - 30)
            spr = tint_sprite(sp, WHITE, 0.8) if fl else sp
            v8.shadow(img, spr, x, y, 21, 30, 0.45)
            put(img, spr, x, y)
        # bombs and blasts (blast radius 32 px -> 64 px fireball)
        for k, (bx, by, fr) in enumerate(drops):
            age = (f - fr) / FPS
            if age < 0:
                continue
            if age < 0.25:
                u = age / 0.25
                b = bomb_sp.resize((max(3, int(bomb_sp.width * (1 - 0.5 * u))),
                                    max(4, int(bomb_sp.height * (1 - 0.5 * u)))), Image.NEAREST)
                put(img, sprite.shadow_of(b, 0.5, 1), bx + 10 * (1 - u), by + 14 * (1 - u))
                put(img, b, bx, by)
            else:
                i = int((age - 0.25) * FPS)
                bf = boom(64, 14, 70 + k % 6)
                if i < len(bf):
                    put(img, bf[i], bx, by)
                for j, (sp, x, y) in enumerate(tsp):
                    if j not in dead and abs(x - bx) < 40 and abs(y - by) < 40 and i == 0:
                        dead[j] = f
        for j, f0 in dead.items():
            i = f - f0
            if 0 <= i < 16:
                put(img, boom(96, 16, 80 + j)[min(i, 15)], tsp[j][1], tsp[j][2])
        # bombers
        for bx in xs:
            by = FH + 40 - (f - enter) * v_px
            if f >= enter and by > -60:
                v8.shadow(img, bomber, bx, by, 34, 48, 0.5)
                put(img, flames[f % 3], bx - 15, by + 24)
                put(img, flames[(f + 1) % 3], bx + 15, by + 24)
                put(img, bomber, bx, by)
        ship = player(0)
        v8.shadow(img, ship, sx, sy)
        put(img, ship, sx, sy)
        if call <= f < call + 30:
            raster.draw_text(img, 120, 512, "HAMMER FLIGHT, INBOUND!", (0, 255, 102), shadow=(0, 30, 10))
        label(img, 8, 8, "AIRSTRIKE", raster.ACCENT, scale=2)
        label(img, 8, 28, "2 CDF BOMBERS, BOMB CARPET ~190 PX WIDE", LABEL)
        frames.append(img)
    return frames


def bullet_field(seed, n=34):
    rng = rnd(seed)
    rows = bullet_set()
    get = {r[0]: r[2] for r in rows}
    kinds = [("ORB", "vrell"), ("ORB", "asc"), ("NEEDLE", "vrell"), ("LARGE ORB", "vrell"),
             ("RING", "asc"), ("ORB", "vrell")]
    out = []
    for i in range(n):
        k = kinds[i % len(kinds)]
        out.append((get[k[0]][k[1]][i % 4 if k[0] != "NEEDLE" else 0], rng.uniform(20, 460),
                    rng.uniform(40, 400), rng.uniform(-20, 20), rng.uniform(60, 140)))
    return out


def sparkle(r=5, k=1.0):
    cv = Canvas(14, 14)
    d = cv.dist(7, 7)
    cross = np.minimum(np.abs(cv.x - 7), np.abs(cv.y - 7))
    cv.add(WHITE, (gauss(d, r * 0.4) + gauss(cross, 0.6) * gauss(d, r)) * k)
    return cv.image()


def smartbomb_frames():
    frames = []
    bullets = bullet_field(5)
    enemies = [(enemy("needler-a"), 120, 110), (enemy("talon-a"), 330, 90),
               (enemy("needler-a"), 400, 200), (enemy("gilded-gunship-a"), 220, 170),
               (enemy("talon-a"), 80, 260)]
    sx, sy = 240, 460
    f_on = 8
    rmax = np.hypot(max(sx, FW - sx), sy) + 10
    spark = [sparkle(5, 1.2), sparkle(4, 0.8), sparkle(3, 0.5)]
    for f in range(36):
        t = f / FPS
        img = scroll_bg("orbit", f, seed=19)
        age = (f - f_on) / FPS
        ring_r = rmax * np.clip(age / 0.35, 0, 1) if age >= 0 else -1
        for j, (sp, x, y) in enumerate(enemies):
            d = np.hypot(x - sx, y - sy)
            hit = ring_r >= d and age >= 0
            if hit:
                fh = int((age - d / rmax * 0.35) * FPS)
                if sp is enemies[3][0]:          # gunship survives (120 < its HP), flashes
                    spr = tint_sprite(sp, WHITE, 0.9) if fh < 2 else sp
                    v8.shadow(img, spr, x, y, 21, 30, 0.45)
                    put(img, spr, x, y)
                else:
                    bf = boom(40 if sp.width < 40 else 64, 12, 90 + j)
                    if fh < len(bf):
                        put(img, bf[fh], x, y)
                continue
            v8.shadow(img, sp, x, y, 21, 30, 0.45)
            put(img, sp, x, y)
        for k, (sp, x, y, vx, vy) in enumerate(bullets):
            bx, by = x + vx * t, y + vy * t
            d = np.hypot(bx - sx, by - sy)
            if age >= 0 and ring_r >= d:
                fh = int((age - d / rmax * 0.35) * FPS)
                if fh < 3:
                    put(img, spark[fh], bx, by)
                continue
            put(img, sp, bx, by)
        if age >= 0:
            def ring(cv, r=ring_r, a=age):
                d = cv.dist(sx, sy)
                fade = 1.0 if a < 0.35 else max(0, 1 - (a - 0.35) / 0.15)
                cv.add((150, 220, 255), gauss(d - r, 14) * 0.35 * fade)
                cv.add(WHITE, gauss(d - r, 3.5) * 1.1 * fade)
                cv.add(PS[3], gauss(d - r + 9, 4) * 0.5 * fade)
            if age < 0.5:
                img.alpha_composite(field_canvas_img(ring))
            # white flash: 0.1 s at 80 %, fading over 0.25 s
            fa = 0.8 if age < 0.1 else max(0.0, 0.8 * (1 - (age - 0.1) / 0.25))
            if fa > 0:
                img = Image.blend(img, Image.new("RGBA", img.size, (255, 255, 255, 255)), fa)
        ship = player(0)
        blink = age >= 0 and age < 1.0 and (f // 2) % 2 == 1
        v8.shadow(img, ship, sx, sy)
        if not blink:
            put(img, ship, sx, sy)
        label(img, 8, 8, "SMART BOMB", raster.ACCENT, scale=2)
        label(img, 8, 28, "FLASH + RING CLEARS ALL BULLETS, 120 DAMAGE", LABEL)
        frames.append(img)
    return frames


def decoy_frames():
    frames = []
    rows = bullet_set()
    get = {r[0]: r[2] for r in rows}
    hom = [get["HOMING"]["asc"], get["HOMING"]["vrell"]]
    launchers = [(enemy("gilded-gunship-a"), 130, 150), (enemy("needler-a"), 370, 170)]
    sx, sy = 240, 420
    f_fl = 10
    fan = [np.radians(a) for a in (-60, -20, 20, 60)]      # 120 deg fan, backwards (down)
    missiles = [  # (launch frame, x0, y0, kind)
        (0, 110, 200, 0), (2, 150, 190, 0), (3, 350, 210, 1), (6, 390, 200, 1), (8, 120, 180, 0)]
    mstate = {k: [x, y, 0.0] for k, (f0, x, y, kd) in enumerate(missiles)}
    mdead = {}

    def flare_pos(i, f):
        age = (f - f_fl) / FPS
        if age < 0:
            return None
        a = fan[i]
        tt = min(age, 0.6)
        dist = 300 * (tt - tt * tt / 1.2)                   # decelerates to a stop at 0.6 s
        return sx + np.sin(a) * dist, sy + 18 + np.cos(a) * dist
    for f in range(64):
        img = scroll_bg("space", f, seed=27)
        for sp, x, y in launchers:
            v8.shadow(img, sp, x, y, 21, 30, 0.45)
            put(img, sp, x, y)
        # flares
        fls = [flare_pos(i, f) for i in range(4)]
        for i, fp in enumerate(fls):
            if fp is None:
                continue
            trail = [flare_pos(i, f - k) for k in range(1, 9)]
            smoke_trail(img, [q for q in trail if q is not None][::1], 0.45)
            put(img, flare_sprite(i * 13 + f), *fp)
        # homing missiles: track the player, retarget to the nearest flare while any burns
        for k, (f0, x0, y0, kd) in enumerate(missiles):
            if f < f0 or k in mdead:
                if k in mdead and f - mdead[k][0] < 12:
                    put(img, boom(40, 12, 120 + k, "asc")[f - mdead[k][0]], *mdead[k][1])
                continue
            st = mstate[k]
            live = [q for q in fls if q is not None]
            tx, ty = (min(live, key=lambda q: np.hypot(q[0] - st[0], q[1] - st[1]))
                      if live else (sx, sy))
            dx, dy = tx - st[0], ty - st[1]
            dd = max(np.hypot(dx, dy), 1e-6)
            sp_ = 8.0
            st[0] += dx / dd * sp_
            st[1] += dy / dd * sp_
            if live and dd < 8:
                mdead[k] = (f, (st[0], st[1]))
                continue
            put(img, hom[kd][(f // 2) % 4], st[0], st[1])
        ship = player(0)
        v8.shadow(img, ship, sx, sy)
        put(img, ship, sx, sy)
        label(img, 8, 8, "DECOY FLARES", raster.ACCENT, scale=2)
        label(img, 8, 28, "4 FLARES, 120 DEG FAN; HOMING SHOTS RETARGET", LABEL)
        frames.append(img)
    return frames


def specials_sheet(seqs):
    W = 1300
    H = 1240
    img = raster.sheet(W, H, "SPECIALS - AIRSTRIKE, SMART BOMB, DECOY FLARES", SUB)
    bomber = bomber_sprite()
    label(img, 16, 38, "CDF BOMBER, 64 PX: 2X / 1X / FLAME 3X")
    img.alpha_composite(on_checker(bomber, 2), (16, 52))
    img.alpha_composite(on_checker(bomber, 1, 4), (16 + 128 + 8, 52))
    for i, fl in enumerate(flame_frames()):
        img.alpha_composite(on_checker(fl, 3), (16 + 128 + 90 + i * 36, 52))
    bomb_sp = v8._obj(v8.bomb_model, 12, 2.0)
    label(img, 400, 38, "BOMB, 0.25 S FALL (3X)")
    for k, u in enumerate((0, 0.33, 0.66, 1.0)):
        b = bomb_sp.resize((max(3, int(bomb_sp.width * (1 - 0.5 * u))),
                            max(4, int(bomb_sp.height * (1 - 0.5 * u)))), Image.NEAREST)
        img.alpha_composite(on_checker(b, 3, 4), (400 + k * 50, 52 + int(u * 20)))
    label(img, 640, 38, "DECOY FLARE, 4 FLICKER FRAMES (3X)")
    for k in range(4):
        img.alpha_composite(on_checker(flare_sprite(k), 3), (640 + k * 80, 52))
    label(img, 980, 38, "BULLET-CLEAR SPARKLE (3X)")
    for k, sp in enumerate([sparkle(5, 1.2), sparkle(4, 0.8), sparkle(3, 0.5)]):
        img.alpha_composite(on_checker(sp, 3), (980 + k * 50, 52))
    label(img, 400, 140, "BOMBER: GREY CDF HULL WITH UTC BLUE WING BANDS SO IT READS AS FRIENDLY; AIR LAYER,", LABEL)
    label(img, 400, 152, "LARGE SHADOW. BOMBS SHRINK AS THEY FALL AND BURST ON THE GROUND LAYER (64 PX FIREBALL",
          LABEL)
    label(img, 400, 164, "= 32 PX BLAST RADIUS). GROUND AND LOW-AIR TARGETS TAKE 100 PER BLAST, AIR TARGETS 20.",
          LABEL)
    y = 196
    titles = {
        "airstrike": ("AIRSTRIKE - CALL, 0.6 S LATER TWO BOMBERS ENTER AT PLAYER X -+64 AND CROSS IN 0.9 S; A BOMB EVERY 36 PX, "
                      "EACH BURSTS 0.25 S AFTER RELEASE", (6, 14, 20, 26, 34)),
        "smartbomb": ("SMART BOMB - WHITE FLASH 0.1 S AT 80 %, FADING 0.25 S; RING FROM THE SHIP COVERS THE FIELD IN 0.35 S AND "
                      "POPS EVERY BULLET IT PASSES; SHIP BLINKS 1 S", (6, 9, 11, 13, 20)),
        "decoy": ("DECOY FLARES - 4 FLARES IN A 120 DEG FAN BACKWARDS, 300 PX/S SLOWING TO A STOP IN 0.6 S, BURNING 4 S; "
                  "HOMING SHOTS RETARGET AND DETONATE ON THEM", (8, 14, 20, 28, 40)),
    }
    for key in ("airstrike", "smartbomb", "decoy"):
        title, idx = titles[key]
        fr = seqs[key]
        label(img, 16, y, title, LABEL)
        for k, i in enumerate(idx):
            th = fr[i].resize((FW // 2, FH // 2), Image.LANCZOS)
            img.alpha_composite(th, (16 + k * (FW // 2 + 12), y + 14))
            label(img, 16 + k * (FW // 2 + 12) + 4, y + 18 + FH // 2 - 14, f"T = {i / FPS:.2f} S", WHITE)
        y += FH // 2 + 34
    notes(img, y, [
        "TIMELINE THUMBNAILS AT 1/2 SCALE FROM THE GIF. THE SMART BOMB FLASH IS REDUCED BY THE FLASH-REDUCTION OPTION (RING ONLY).",
        "FLARES ARE WHITE-HOT WITH A NEUTRAL HALO AND GREY SMOKE: NO RESERVED BULLET HUE, AND NOTHING THAT LOOKS LIKE A PICKUP."])
    return img.crop((0, 0, W, y + 40))


@item("specials")
def do_specials():
    seqs = {"airstrike": airstrike_frames(), "smartbomb": smartbomb_frames(),
            "decoy": decoy_frames()}
    save_png(specials_sheet(seqs), "specials", "specials-r09-a.png")
    save_gif(seqs["airstrike"] + seqs["smartbomb"] + seqs["decoy"], "specials", "specials-r09-a.gif")


# =========================================================================== 7. edge warnings

WARN = (255, 200, 0)          # HUD amber used by the accepted round-08 edge warning
BOSS_RED = (255, 50, 60)
tri = __import__("ui_r06").tri


def edge_warning(side, k=1.0, length=60, label_text=None):
    """Edge warning block for one edge: a bright bar on the edge, three chevrons fading
    inwards, and '! LEFT' text. k = flash brightness (1 on, 0.45 dim; 0 = off frame)."""
    vertical = side in ("l", "r")
    w, h = (90, length + 20) if vertical else (length + 80, 70)
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    if k <= 0:
        return img
    d = ImageDraw.Draw(img)
    a = int(255 * k)
    col = WARN + (a,)
    glow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    if side == "l":
        gd.rectangle([0, 6, 8, h - 6], fill=WARN + (int(140 * k),))
        d.rectangle([0, 10, 3, h - 10], fill=col)
        for i, aa in enumerate((1.0, 0.66, 0.36)):
            for yy in (h / 2 - 14, h / 2 + 14):
                tri(d, 9 + i * 9, yy, 9, WARN + (int(a * aa),), "l")
        txt_xy = (40, h / 2 - 4)
    elif side == "r":
        gd.rectangle([w - 9, 6, w, h - 6], fill=WARN + (int(140 * k),))
        d.rectangle([w - 4, 10, w - 1, h - 10], fill=col)
        for i, aa in enumerate((1.0, 0.66, 0.36)):
            for yy in (h / 2 - 14, h / 2 + 14):
                tri(d, w - 10 - i * 9, yy, 9, WARN + (int(a * aa),), "r")
        txt_xy = (w - 40 - raster.text_width(label_text or "! RIGHT"), h / 2 - 4)
    else:  # bottom edge (rear)
        gd.rectangle([6, h - 9, w - 6, h], fill=WARN + (int(140 * k),))
        d.rectangle([10, h - 4, w - 10, h - 1], fill=col)
        for i, aa in enumerate((1.0, 0.66, 0.36)):
            for xx in (w / 2 - 22, w / 2 + 22):
                tri(d, xx, h - 10 - i * 9, 9, WARN + (int(a * aa),), "d")
        txt_xy = (w / 2 - raster.text_width(label_text or "! REAR") / 2, h - 50)
    glow = glow.filter(ImageFilter.GaussianBlur(3))
    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    out.alpha_composite(glow)
    out.alpha_composite(img)
    raster.draw_text(out, int(txt_xy[0]), int(txt_xy[1]),
                     label_text or {"l": "! LEFT", "r": "! RIGHT", "b": "! REAR"}[side],
                     WARN + (a,), shadow=(30, 20, 0))
    return out


def threat_arrow(direction, near=1.0, kind="enemy"):
    """Sensor suite (L2+) arrow for one off-screen threat: a small arrowhead whose size and
    brightness grow as it closes in; homing missiles get a red-orange ring."""
    s = int(7 + 5 * near)
    w = h = 2 * s + 10
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c = WARN if kind == "enemy" else (255, 90, 40)
    a = int(150 + 105 * near)
    cx, cy = w / 2, h / 2
    tri(d, cx - (s / 2 if direction == "r" else -s / 2 if direction == "l" else 0),
        cy - (s / 2 if direction == "d" else -s / 2 if direction == "u" else 0), s,
        (20, 12, 0, 200), direction)
    tri(d, cx - (s / 2 - 1 if direction == "r" else -(s / 2 - 1) if direction == "l" else 0),
        cy - (s / 2 - 1 if direction == "d" else -(s / 2 - 1) if direction == "u" else 0),
        s - 2, c + (a,), direction)
    if kind == "missile":
        d.ellipse([2, 2, w - 3, h - 3], outline=c + (a,))
    return img


def boss_banner(k=1.0, name="HARBOUR KRAKEN", w=FW, phase=0):
    """Boss warning: metal-framed band with scrolling amber/black hazard stripes, WARNING in
    red, the boss name below. k = flash level (the text blinks at 4 Hz with the klaxon)."""
    h = 92
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 8, w - 1, h - 9], fill=(16, 6, 8, 215))
    for y0 in (0, h - 8):                      # hazard stripes, scrolling with phase
        for x in range(-16, w + 16, 16):
            xx = x + (phase * 4) % 16 * (1 if y0 == 0 else -1)
            d.polygon([(xx, y0), (xx + 8, y0), (xx + 16, y0 + 8), (xx + 8, y0 + 8)],
                      fill=WARN + (255,))
        d.rectangle([0, y0, w - 1, y0 + 7], outline=(90, 70, 40, 255))
    d.line([0, 9, w - 1, 9], fill=(180, 170, 160, 255))           # metal bevel
    d.line([0, h - 10, w - 1, h - 10], fill=(70, 60, 60, 255))
    if k > 0:
        t = "WARNING"
        sc = 5
        tw = raster.text_width(t, sc)
        col = tuple(int(c * (0.5 + 0.5 * k)) for c in BOSS_RED)
        raster.draw_text(img, (w - tw) // 2, 18, t, col, scale=sc, shadow=(60, 0, 0))
    sub = f"{name} APPROACHING"
    raster.draw_text(img, (w - raster.text_width(sub, 2)) // 2, 62, sub, WARN, scale=2,
                     shadow=(40, 20, 0))
    return img


def wave_banner(text="WARNING - HOSTILES FROM THE REAR", w=FW - 40):
    h = 26
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, w - 1, h - 1], fill=(20, 14, 4, 200), outline=WARN + (255,))
    d.line([1, 1, w - 2, 1], fill=(255, 240, 180, 255))
    raster.draw_text(img, (w - raster.text_width(text, 1)) // 2, 9, text, WARN, shadow=(40, 25, 0))
    return img


def warnings_sheet():
    W, H = 1300, 1230
    img = raster.sheet(W, H, "EDGE WARNINGS, THREAT ARROWS AND THE BOSS WARNING BANNER", SUB)
    lx = 16
    y = 40
    label(img, lx, y, "SIDE WAVE - FLASH CYCLE 2X")
    label(img, lx, y + 11, "ON / DIM / OFF, 4 HZ, >= 1.5 S AHEAD", DIM)
    y += 26
    for i, k in enumerate((1.0, 0.45, 0.0)):
        box = Image.new("RGBA", (90, 80), (12, 16, 26, 255))
        box.alpha_composite(edge_warning("l", k))
        img.alpha_composite(sprite.enlarge(box, 1), (lx + i * 96, y))
    y += 88
    z = sprite.enlarge(edge_warning("l", 1.0), 2)
    bg = Image.new("RGBA", z.size, (12, 16, 26, 255))
    bg.alpha_composite(z)
    img.alpha_composite(bg, (lx, y))
    y += z.height + 10
    label(img, lx, y, "REAR WAVE (BOTTOM EDGE) 2X")
    y += 12
    z = sprite.enlarge(edge_warning("b", 1.0, length=70), 2)
    bg = Image.new("RGBA", z.size, (12, 16, 26, 255))
    bg.alpha_composite(z)
    img.alpha_composite(bg, (lx, y))
    y += z.height + 10
    label(img, lx, y, "THREAT ARROWS (SENSOR SUITE L2+) 2X")
    label(img, lx, y + 11, "FAR -> NEAR; RINGED = HOMING MISSILE", DIM)
    y += 26
    x = lx
    for near in (0.0, 0.5, 1.0):
        a = sprite.enlarge(threat_arrow("r", near), 2)
        bg = Image.new("RGBA", a.size, (12, 16, 26, 255))
        bg.alpha_composite(a)
        img.alpha_composite(bg, (x, y))
        x += a.width + 6
    a = sprite.enlarge(threat_arrow("u", 0.8, "missile"), 2)
    bg = Image.new("RGBA", a.size, (12, 16, 26, 255))
    bg.alpha_composite(a)
    img.alpha_composite(bg, (x, y))
    y += 76
    label(img, lx, y, "WAVE BANNER (WITH RADIO CALL)")
    y += 12
    img.alpha_composite(wave_banner("HOSTILES FROM THE REAR", 280), (lx, y))
    y += 40
    label(img, lx, y, "BOSS BANNER FLASH (1/2 SCALE)")
    y += 12
    for i, k in enumerate((1.0, 0.0)):
        bb = boss_banner(k, phase=i * 2).resize((FW // 2 + 40, 46), Image.NEAREST)
        img.alpha_composite(bb.crop((20, 0, 300, 46)), (lx, y + i * 52))
    # ---- 2 x 2 in-game panels at 1x
    px0 = W - 16 - 2 * FW - 12
    panels = []
    # A: Mantis pair entering from the left
    a = scroll_bg("coast", 40, seed=3)
    mt = enemy("mantis-a")
    for yy in (190, 300):
        put(a, mt.rotate(-90, expand=True), 4, yy)
    a.alpha_composite(edge_warning("l", 1.0, length=200), (0, 245 - 110))
    sh = player(0)
    v8.shadow(a, sh, 260, 450)
    put(a, sh, 260, 450)
    raster.draw_text(a, 112, 512, "ROOK: BOGEYS LEFT FLANK!", (0, 255, 102), shadow=(0, 30, 10))
    panels.append(("A - SIDE WAVE: MANTIS PAIR FROM THE LEFT, 1.5 S AHEAD", a))
    # B: rear ambush
    b = scroll_bg("orbit", 10, seed=8)
    for k, xx in enumerate((180, 240, 300)):
        sp = enemy("skitter-a").rotate(180)
        put(b, sp, xx, FH - 2 - (k % 2) * 6)
    b.alpha_composite(edge_warning("b", 1.0, length=260), ((FW - 340) // 2, FH - 70))
    b.alpha_composite(wave_banner(), (20, 180))
    v8.shadow(b, sh, 220, 380)
    put(b, sh, 220, 380)
    panels.append(("B - REAR AMBUSH: SKITTERS FROM BELOW, EDGE WARNING + BANNER", b))
    # C: boss warning
    c = scroll_bg("coast", 0, seed=11)
    arr = np.array(c).astype(np.float64)
    arr[..., :3] = arr[..., :3] * 0.7 + np.array([60, 0, 8]) * 0.3      # red alert tint pulse
    c = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")
    c.alpha_composite(boss_banner(1.0, phase=3), (0, 200))
    v8.shadow(c, sh, 240, 450)
    put(c, sh, 240, 450)
    panels.append(("C - BOSS WARNING: 5 S BANNER WITH THE 'RED ALERT' STINGER", c))
    # D: sensor suite threat arrows
    dd = scroll_bg("space", 0, seed=44)
    for t_ in ((enemy("talon-a"), 60, 140), (enemy("needler-a"), 400, 220)):
        v8.shadow(dd, t_[0], t_[1], t_[2], 21, 30, 0.4)
        put(dd, t_[0], t_[1], t_[2])
    for (x_, y_, dr, near, kind) in ((14, 300, "l", 0.3, "enemy"), (FW - 18, 120, "r", 0.9, "missile"),
                                     (330, 14, "u", 0.6, "enemy"), (120, FH - 14, "d", 0.2, "enemy")):
        put(dd, threat_arrow(dr, near, kind), x_, y_)
    v8.shadow(dd, sh, 240, 430)
    put(dd, sh, 240, 430)
    panels.append(("D - SENSOR SUITE L2+: ARROWS FOR SINGLE OFF-SCREEN THREATS", dd))
    for k, (title, pan) in enumerate(panels):
        x = px0 + (k % 2) * (FW + 12)
        yy = 40 + (k // 2) * (FH + 26)
        label(img, x, yy, title)
        img.alpha_composite(pan, (x, yy + 12))
        panel_frame(img, (x - 1, yy + 11, x + FW, yy + 12 + FH))
    notes(img, H - 40, [
        "AMBER (FFC800) IS THE HUD WARNING COLOUR FROM THE ACCEPTED ROUND-08 HUD; IT IS NEVER A BULLET HUE AT THIS SHAPE (BARS, CHEVRONS, TEXT).",
        "WARNINGS SIT ON THE HUD LAYER ABOVE EVERYTHING; THEY ARE ALWAYS SHOWN FOR SIDE AND REAR WAVES. THE BOSS BANNER CLEARS AFTER 5 S."])
    return img


@item("warnings")
def do_warnings():
    save_png(warnings_sheet(), "hud", "edge-warnings-r09-a.png")


# =========================================================================== main

def save_png(img, key, name):
    OUT[key].mkdir(parents=True, exist_ok=True)
    path = OUT[key] / name
    img.convert("RGB").save(path, optimize=True)
    print("wrote", path.relative_to(ROOT), img.size)


def save_gif(frames, key, name, colors=128):
    OUT[key].mkdir(parents=True, exist_ok=True)
    path = OUT[key] / name
    size = write_gif(frames, path, fps=FPS, colors=colors)
    print("wrote", path.relative_to(ROOT), f"{len(frames)} frames, {size / 1e6:.1f} MB")


@item("beam")
def do_beam():
    save_png(beam_sheet(), "weapons", "beam-impact-r09-a.png")
    save_gif(beam_gif(), "weapons", "beam-impact-r09-a.gif")


def main(args):
    todo = args or list(ITEMS)
    for name in todo:
        ITEMS[name]()


if __name__ == "__main__":
    main(sys.argv[1:])
