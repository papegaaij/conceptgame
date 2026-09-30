#!/usr/bin/env python3
"""Concept round 03 - enemy concept sheets at 960x540 scale, palette B.

Outputs (design/enemies/...):
  air/concept/skitter-r03-{a,b}.png        Skitter, Vrell design language A / B
  air/concept/needler-r03-{a,b}.png        Needler, A / B
  air/concept/stinger-r03-a.png            Stinger (language A)
  air/concept/spore-bomber-r03-a.png       Spore Bomber (A)
  air/concept/brood-pod-r03-a.png          Brood Pod (A)
  air/concept/mantis-r03-a.png             Mantis (A)
  air/concept/talon-r03-a.png              Talon (Ascendancy fighter)
  air/concept/gilded-gunship-r03-a.png     Gilded Gunship (Ascendancy gunship)
  ground/concept/spine-turret-r03-{a,b}.png Spine Turret, A / B
  ground/concept/polyp-mortar-r03-a.png    Polyp Mortar (A)
  ground/concept/rail-bunker-r03-a.png     Rail Bunker (Ascendancy ground turret)
  bosses/concept/brood-carrier-r03-a.png   Brood Carrier (Act 1 boss), multi-part sheet
  concept/lineup-r03-a.png                 all of the above at native scale next to the player

Each enemy sheet: source render, native sprite (1x, 2x, 4x) with its palette, three animation
frames (3x) and an in-game view (2x) with formation, shadows, typical bullets and the player
ship for scale. Models: tools/concept/render/enemy_models.py.

Run: python3 tools/concept/enemies_r03.py [slug ...]   (no args = everything, ~6 min)
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
from render import enemy_models as em  # noqa: E402
from render import models, raster, sdf, sprite, terrain  # noqa: E402
from render.config import ROOT, SHIP_SIZE  # noqa: E402
from render.palette import B  # noqa: E402

DESIGN = ROOT / "design" / "enemies"
FACTOR = 8
EXTENT = 2.3
SUB = "CONCEPT ROUND 03 - 960X540"
SHOTS = B["ENEMY SHOTS"]            # 300030 c000c0 ff40ff ffc0ff ffffff ffff40
GOLD = B["ASCEND. GOLD"]
SHADOW = {"air": (21, 30), "low-air": (9, 13), "ground": (6, 8)}

LANG = {"a": "VRELL LANGUAGE A: SLEEK CHITIN", "b": "VRELL LANGUAGE B: ARMOURED BROOD"}
LANG_NOTES = {
    "a": ["LANGUAGE A - SLEEK CHITIN: SMOOTH, ELONGATED, GLOSSY VIOLET CHITIN; THIN GLOWING",
          "TEAL SEAMS ALONG THE SPINE; PINK EYE = WEAK POINT. READS FAST, AGILE, ALIEN."],
    "b": ["LANGUAGE B - ARMOURED BROOD: BULKY SEGMENTED CARAPACE PLATES, CLAWS, SPIKES; TEAL",
          "GLOW ONLY IN THE GAPS BETWEEN PLATES AND IN EYE CLUSTERS. READS HEAVY, INSECTOID."],
    None: ["ASCENDANCY: HUMAN-BUILT, ANGULAR FACETED BLACK HULLS WITH HEX PANELLING, GOLD",
           "CHINES AND ARMOUR, RED RUNNING LIGHTS, ORANGE-RED THRUST. NO CURVES, NO GLOW-VEINS."],
}

# slug-base -> (layer, first level, role, formation, attack, background, weak point)
INFO = {
    "skitter": ("air", "L01", "SWARM FODDER. PATH/SWOOP MOVEMENT, RAMS ON CONTACT. 1 HP.",
                "snake", None, "hull", "WHOLE BODY"),
    "needler": ("air", "L01", "BASIC GUNNER. HOVER OR SWOOP, SLOW AIMED THORN EVERY 2.5 S. 4 HP.",
                "v", "thorn", "hull", "EYE / THORN TIP"),
    "stinger": ("air", "L02", "DIVER: LOCKS ON, DIVES PAST THE PLAYER, 3-WAY FAN AT THE BOTTOM.",
                "dive", "fan", "hull", "STINGER GLOW"),
    "spore-bomber": ("low-air", "L03", "SLOW; DROPS DRIFTING SPORE MINES THAT RISE TO THE PLAYER "
                     "PLANE AFTER 1 S.", "line", "spores", "orbit", "SPORE BULBS"),
    "brood-pod": ("air", "L04", "PULSING SAC: BURSTS INTO 6 SKITTERS WHEN KILLED OR AFTER 8 S.",
                  "pod", "burst", "luna", "CROWN EYE"),
    "mantis": ("air", "L06", "ENTERS FROM A SIDE, HOLDS, SWEEPS A SHORT LASER ACROSS THE LOWER "
               "SCREEN.", "side", "sweep", "luna", "HEAD EMITTER"),
    "spine-turret": ("ground", "L02", "GROWN TURRET ON STATION HULLS: ROTATING AIMED THORNS.",
                     "nest", "thorn", "hull", "CENTRAL EYE"),
    "polyp-mortar": ("ground", "L05", "LOBS ACID AT THE PLAYER'S POSITION; IMPACT MARKED 1 S "
                     "AHEAD, BURSTS INTO A SMALL RING.", "nest2", "mortar", "luna", "ACID MOUTH"),
    "talon": ("air", "L29", "FAST INTERCEPTOR: SWOOP WITH PAIRED AIMED SHOTS; 2 HP HEX SHIELD.",
              "v", "pair", "rock", "ENGINES"),
    "gilded-gunship": ("air", "L30", "HEAVY: ARMOURED FRONT (-50%), RING BURST EVERY 3 S, EXPOSED "
                       "ENGINES TAKE X2.", "single", "ring", "rock", "REAR ENGINES"),
    "rail-bunker": ("ground", "L33", "HARDENED; CHARGES A SCREEN-LONG RAIL SHOT ALONG THE "
                    "PLAYER'S COLUMN (0.8 S TELEGRAPH).", "single-g", "rail", "rockhull",
                    "SENSOR / BARREL"),
}

ANIM = {
    "wing": ("WING BEAT", [(-1.0, 1.0), (0.0, 1.0), (1.0, 1.0)]),
    "claw": ("CLAW SNAP", [(-1.0, 1.0), (0.0, 1.0), (1.0, 1.0)]),
    "pulse": ("SAC PULSE + GLOW", [(-1.0, 0.7), (0.0, 1.0), (1.0, 1.4)]),
    "arms": ("ARMS OPEN FOR THE SWEEP", [(0.0, 1.0), (0.6, 1.2), (1.2, 1.4)]),
    "aim": ("AIM (32 DIRECTIONS)", [(-0.7, 1.0), (0.0, 1.0), (0.7, 1.0)]),
    "bank": ("BANK L / C / R", [(-0.45, 1.0), (0.0, 1.0), (0.45, 1.0)]),
    "charge": ("RING BURST CHARGE", [(0.0, 1.0), (0.5, 1.0), (1.0, 1.0)]),
}

_cache = {}

# Titles and labels (round 04 re-uses this module with its own values, see enemies_r04.py)
BOSS_TITLE = "BROOD CARRIER - ACT 1 BOSS (VRELL LANGUAGE A)"
LINEUP_TITLE = "ENEMY LINEUP - ROUND 03, NATIVE SCALE (1X AND 2X)"
LINEUP_NOTE = "2X WITH NAMES (A/B = VRELL DESIGN LANGUAGE A / B)"
LINEUP_RESERVE = 0          # px kept free at the right of each 2x row (for the boss thumbnail)


def lineup_name(key):
    return key.upper().replace("-", " ")


# --------------------------------------------------------------------------- rendering

def render(fn, n, anim=0.0, glow=1.0, factor=FACTOR, colors=None):
    key = (fn.__name__, n, round(anim, 3), round(glow, 3), factor)
    if key not in _cache:
        scene, mats = fn(anim, glow)
        hi = sdf.render(scene, mats, (n * factor, n * factor), EXTENT)
        cols = colors or (24 if n <= 42 else 32)
        _cache[key] = (hi, sprite.make_sprite(hi, factor, cols))
    return _cache[key]


def player_sprite():
    if "player" not in _cache:
        scene, mats = models.ship_a_model(0.0, palette=B.ship_colors())
        hi = sdf.render(scene, mats, (SHIP_SIZE * 8, SHIP_SIZE * 8), EXTENT)
        _cache["player"] = sprite.make_sprite(hi, 8, 28)
    return _cache["player"]


# --------------------------------------------------------------------------- backgrounds

def _hillshade(hgt, strength):
    gy, gx = np.gradient(hgt)
    return np.clip(1.0 + (gx + gy) * -strength, 0.5, 1.5)


def regolith(w, h, seed, ramp_name="ASTEROID BELT", craters=14):
    """Lunar / asteroid surface: fBm height field with craters, hill-shaded from the top-left."""
    rng = np.random.default_rng(seed)
    hgt = raster.fbm(w, h, 48, seed, octaves=6, period=False) * 0.6
    yy, xx = np.mgrid[0:h, 0:w]
    for _ in range(craters):
        cx, cy = rng.uniform(0, w), rng.uniform(0, h)
        r = rng.uniform(6, 34) * (1.6 if rng.random() < 0.15 else 1.0)
        d = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2) / r
        bowl = np.where(d < 1, -(1 - d * d) * 0.5, 0)
        rim = np.exp(-((d - 1.0) / 0.18) ** 2) * 0.28
        hgt = hgt + (bowl + rim) * min(1.0, r / 20)
    ramp = B[ramp_name]
    stops = [(0.0, ramp[0]), (0.3, ramp[1]), (0.55, ramp[2]), (0.8, ramp[3]), (1.0, ramp[4])]
    t = (hgt - hgt.min()) / (hgt.max() - hgt.min())
    rgb = raster.ramp(stops, t) * _hillshade(hgt * 30, 1.0)[..., None]
    rgb *= (0.95 + 0.1 * raster.fbm(w, h, 6, seed + 3, octaves=2, period=False))[..., None]
    return terrain.recede(raster.to_rgba_image(rgb), amount=0.2, darken=0.78)


def hull_plating(w, h, seed):
    """UTC station hull seen from above: plates, panel lines, rivets, vents, hazard marks."""
    rng = np.random.default_rng(seed)
    ramp = B["UTC HULL"]
    img = Image.new("RGBA", (w, h), ramp[1] + (255,))
    d = ImageDraw.Draw(img)
    pw, ph = 28, 20
    for y in range(-ph, h + ph, ph):
        off = (y // ph % 2) * (pw // 2)
        for x in range(-pw, w + pw, pw):
            v = rng.uniform(0.75, 1.1)
            c = tuple(int(min(255, a * v)) for a in raster.lerp(ramp[1], ramp[2], 0.55))
            d.rectangle([x + off, y, x + off + pw - 2, y + ph - 2], fill=c + (255,))
            d.line([x + off, y, x + off + pw - 2, y], fill=ramp[2] + (255,))
            d.line([x + off, y, x + off, y + ph - 2], fill=ramp[2] + (255,))
            d.line([x + off, y + ph - 2, x + off + pw - 2, y + ph - 2], fill=ramp[0] + (255,))
            if rng.random() < 0.3:
                for rx in (x + off + 3, x + off + pw - 5):
                    d.point((rx, y + 3), fill=ramp[3] + (255,))
    for _ in range(w * h // 9000):
        x, y = rng.integers(0, w - 40), rng.integers(0, h - 30)
        kind = rng.integers(0, 3)
        if kind == 0:   # vent grille
            d.rectangle([x, y, x + 30, y + 14], fill=ramp[0] + (255,))
            for i in range(x + 2, x + 30, 4):
                d.line([i, y + 2, i, y + 12], fill=ramp[1] + (255,))
        elif kind == 1:  # hazard stripe
            for i in range(0, 36, 6):
                d.polygon([(x + i, y), (x + i + 3, y), (x + i + 7, y + 6), (x + i + 4, y + 6)],
                          fill=(120, 90, 30, 255))
        else:           # hatch
            d.ellipse([x, y, x + 18, y + 18], outline=ramp[0] + (255,), fill=ramp[2] + (255,))
    grad = np.linspace(1.08, 0.86, w)[None, :] * np.linspace(1.05, 0.9, h)[:, None]
    arr = np.array(img).astype(np.float64)
    arr[..., :3] *= grad[..., None]
    img = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")
    return terrain.recede(img, amount=0.2, darken=0.72)


def orbit(w, h, seed):
    return terrain.recede(terrain.earth_from_orbit(w, h, seed=seed, period=False),
                          amount=0.1, darken=0.8)


def rock_field(w, h, seed):
    """Asteroid belt: starfield with a few large lumpy asteroids, lit from the top-left."""
    rng = np.random.default_rng(seed)
    base = raster.starfield(w, h, seed + 1, density=0.004)
    arr = np.array(base).astype(np.float64)
    tex = np.array(regolith(w, h, seed + 2, craters=18)).astype(np.float64)
    yy, xx = np.mgrid[0:h, 0:w]
    warp = raster.fbm(w, h, 24, seed + 4, octaves=4, period=False)
    for cx, cy, r in ((w * 0.18, h * 0.3, 70), (w * 0.95, h * 0.62, 90),
                      (w * 0.35, h * 0.95, 55), (w * 0.72, h * 0.08, 34)):
        cx += rng.uniform(-10, 10)
        dx, dy = (xx - cx) / r, (yy - cy) / r
        dist = np.sqrt(dx * dx + dy * dy) + (warp - 0.5) * 0.35
        inside = dist < 1.0
        nz = np.sqrt(np.clip(1 - dist ** 2, 0, 1))
        light = np.clip(-0.55 * dx - 0.6 * dy + 0.75 * nz, 0.08, 1.2)
        arr[inside, :3] = tex[inside, :3] * (0.35 + 0.8 * light[inside, None])
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")


BACKGROUNDS = {"hull": hull_plating, "luna": regolith, "orbit": orbit, "rock": rock_field,
               "rockhull": lambda w, h, s: hull_plating(w, h, s + 5)}


# --------------------------------------------------------------------------- bullets & fx

def orb(img, x, y, r=5, ring=None):
    """Enemy bullet: dark rim, saturated ring, white core (readability rules)."""
    d = ImageDraw.Draw(img)
    ring = ring or SHOTS[2]
    d.ellipse([x - r - 1, y - r - 1, x + r + 1, y + r + 1], fill=SHOTS[0] + (255,))
    d.ellipse([x - r, y - r, x + r, y + r], fill=ring + (255,))
    d.ellipse([x - r + 2, y - r + 2, x + r - 2, y + r - 2], fill=SHOTS[4] + (255,))


def thorn(img, x, y, ang, ln=7, ring=None):
    """Elongated Vrell thorn pointing along ``ang`` (radians, 0 = down)."""
    d = ImageDraw.Draw(img)
    ring = ring or SHOTS[2]
    dx, dy = np.sin(ang), np.cos(ang)
    px, py = -dy, dx
    pts = lambda s, w: [(x + dx * s, y + dy * s), (x + px * w, y + py * w),
                        (x - dx * s * 0.7, y - dy * s * 0.7), (x - px * w, y - py * w)]
    d.polygon(pts(ln + 2, 4.5), fill=SHOTS[0] + (255,))
    d.polygon(pts(ln, 3.2), fill=ring + (255,))
    d.line([(x - dx * 3, y - dy * 3), (x + dx * (ln - 2), y + dy * (ln - 2))],
           fill=SHOTS[4] + (255,), width=1)


def beam(img, x0, y0, x1, y1, ring=None):
    d = ImageDraw.Draw(img)
    ring = ring or SHOTS[2]
    d.line([x0, y0, x1, y1], fill=SHOTS[0] + (255,), width=7)
    d.line([x0, y0, x1, y1], fill=ring + (255,), width=5)
    d.line([x0, y0, x1, y1], fill=SHOTS[4] + (255,), width=2)


def dashed(img, x0, y0, x1, y1, color, dash=6):
    d = ImageDraw.Draw(img)
    n = int(max(abs(x1 - x0), abs(y1 - y0)) // dash)
    for i in range(0, n, 2):
        t0, t1 = i / n, min(1, (i + 1) / n)
        d.line([x0 + (x1 - x0) * t0, y0 + (y1 - y0) * t0, x0 + (x1 - x0) * t1,
                y0 + (y1 - y0) * t1], fill=color + (255,), width=2)


def bolt(img, x, y):
    d = ImageDraw.Draw(img)
    c = B["PLAYER SHOTS"]
    d.rectangle([x - 2, y - 8, x + 2, y + 8], fill=c[2] + (220,))
    d.rectangle([x - 1, y - 6, x + 1, y + 6], fill=c[4] + (255,))


def place(bg, sp, x, y, layer):
    if layer in SHADOW:            # no shadow over the deep layer (e.g. Earth from orbit)
        dx, dy = SHADOW[layer]
        sh = sprite.shadow_of(sp, opacity=0.62, blur=1.0,
                              scale=0.85 if layer != "ground" else 1.0)
        sprite.paste_center(bg, sh, x + dx, y + dy)
    sprite.paste_center(bg, sp, x, y)


# --------------------------------------------------------------------------- in-game view

def in_game(slug, fn, n, layer, formation, attack, bg_kind, seed=5):
    w, h = 240, 300
    bg = BACKGROUNDS[bg_kind](w, h, seed)
    shadow_layer = "orbit" if bg_kind in ("orbit", "rock") else layer   # no surface below
    player = player_sprite()
    px, py = 120, 262
    _, sp = render(fn, n)
    positions = []
    if formation == "snake":
        positions = [(60 + 22 * i, 40 + 18 * i + 10 * np.sin(i * 1.2)) for i in range(6)]
    elif formation == "v":
        positions = [(120, 70), (84, 46), (156, 46)]
    elif formation == "dive":
        positions = [(80, 60), (170, 120)]
    elif formation == "line":
        positions = [(70, 70), (170, 70)]
    elif formation == "pod":
        positions = [(120, 80)]
    elif formation == "side":
        positions = [(26, 120)]
    elif formation == "nest":
        positions = [(60, 70), (180, 90), (120, 150)]
    elif formation == "nest2":
        positions = [(64, 80), (184, 120)]
    elif formation in ("single", "single-g"):
        positions = [(120, 90)]

    for (x, y) in positions:
        if attack == "thorn" and layer == "ground":
            ang = np.arctan2(px - x, py - y)
            _, tsp = render(fn, n, anim=float(np.clip(ang, -1.2, 1.2)))
            place(bg, tsp, x, y, "ground")
        else:
            place(bg, sp, x, y, shadow_layer)
    if player is not None:
        place(bg, player, px, py, "air" if shadow_layer != "orbit" else "orbit")
        for yy in (150, 190, 226):
            bolt(bg, px - 10, yy)
            bolt(bg, px + 10, yy)

    # attack illustration
    if attack == "thorn":
        for (x, y) in positions:
            ang = np.arctan2(px - x, py - y)
            for s in (26, 58):
                thorn(bg, x + np.sin(ang) * s, y + np.cos(ang) * s, ang)
    elif attack == "fan":
        x, y = positions[1]
        for a in (-0.4, 0.0, 0.4):
            orb(bg, x + np.sin(a) * 30, y + np.cos(a) * 30, 4)
    elif attack == "spores":
        rng = np.random.default_rng(3)
        for (x, y) in positions:
            for i in range(3):
                sx, sy = x + rng.uniform(-18, 18), y + 40 + i * 30
                orb(bg, sx, sy, 4, ring=SHOTS[5])
    elif attack == "burst":
        _, sk = render(em.skitter_a, 30)
        for a in np.linspace(0.4, 2 * np.pi + 0.4, 6, endpoint=False):
            sprite.paste_center(bg, sk, 120 + np.sin(a) * 52, 80 + np.cos(a) * 44)
    elif attack == "sweep":
        x, y = positions[0]
        beam(bg, x + 14, y + 24, 200, 210)
    elif attack == "mortar":
        for (x, y), (tx, ty) in zip(positions, ((110, 230), (150, 252))):
            dashed(bg, tx - 14, ty, tx + 14, ty, SHOTS[2])
            ImageDraw.Draw(bg).ellipse([tx - 13, ty - 9, tx + 13, ty + 9],
                                       outline=SHOTS[2] + (255,), width=1)
            orb(bg, (x + tx) / 2, min(y, ty) + 30, 5, ring=SHOTS[5])
    elif attack == "pair":
        for (x, y) in positions:
            ang = np.arctan2(px - x, py - y)
            for s in (30, 70):
                for o in (-5, 5):
                    orb(bg, x + np.sin(ang) * s + o, y + np.cos(ang) * s, 3, ring=GOLD[3])
    elif attack == "ring":
        x, y = positions[0]
        for a in np.linspace(0, 2 * np.pi, 12, endpoint=False):
            orb(bg, x + np.sin(a) * 62, y + np.cos(a) * 50, 4, ring=GOLD[3])
    elif attack == "rail":
        x, y = positions[0]
        dashed(bg, px, y + 24, px, h, (255, 60, 40), dash=5)
        raster.add_light(bg, x, y + 26, 5, (255, 80, 40), 1.0)
    return bg


# --------------------------------------------------------------------------- sheets

def checker(w, h, cell=8):
    img = Image.new("RGBA", (w, h))
    d = ImageDraw.Draw(img)
    for y in range(0, h, cell):
        for x in range(0, w, cell):
            c = (30, 34, 46) if (x // cell + y // cell) % 2 == 0 else (38, 42, 56)
            d.rectangle([x, y, x + cell - 1, y + cell - 1], fill=c + (255,))
    return img


def label(img, x, y, text, color=raster.LABEL_DIM):
    raster.draw_text(img, x, y, text, color)


def wrap(text, width):
    words, lines, cur = text.split(), [], ""
    for wd in words:
        if len(cur) + len(wd) + 1 > width:
            lines.append(cur)
            cur = wd
        else:
            cur = f"{cur} {wd}".strip()
    return lines + ([cur] if cur else [])


def enemy_sheet(key):
    title, faction, category, n, fn, kind, style = em.ENEMIES[key]
    base = key.rsplit("-", 1)[0]
    layer, first, role, formation, attack, bg_kind, weak = INFO[base]
    hi, sp = render(fn, n)
    head = f"{title} - " + (LANG[style] if style else f"ASCENDANCY {category.upper()} UNIT")

    R = min(n * FACTOR, 300)
    big = 4
    game = in_game(key, fn, n, layer, formation, attack, bg_kind)
    left = max(16 + R + 20 + n * big + 14 + n * 2 + 16, 16 + 3 * (n * 3 + 10), 570)
    W, H = left + game.width * 2 + 16, 50 + max(R + 30 + n * 3 + 150, game.height * 2) + 20
    img = raster.sheet(W, H, head, SUB)
    label(img, 16, 38, f"SOURCE RENDER ({n * FACTOR} PX, SDF RAY-MARCHED)")
    src = checker(R, R)
    s = sprite.to_image(hi)
    if s.width != R:
        s = s.resize((R, R), Image.LANCZOS)
    src.alpha_composite(s)
    img.alpha_composite(src, (16, 50))

    x2 = 16 + R + 20
    ncol = len(sprite.palette_of(sp))
    label(img, x2, 38, f"NATIVE {n}X{n}, {ncol} COLOURS ({big}X)")
    cb = checker(n * big, n * big, cell=12)
    cb.alpha_composite(sprite.enlarge(sp, big))
    img.alpha_composite(cb, (x2, 50))
    x3 = x2 + n * big + 14
    label(img, x3, 50, "1X")
    img.alpha_composite(sp, (x3, 62))
    label(img, x3, 70 + n, "2X")
    img.alpha_composite(sprite.enlarge(sp, 2), (x3, 82 + n))
    py = 50 + n * big + 12
    label(img, x2, py, "SPRITE PALETTE")
    d = ImageDraw.Draw(img)
    for i, c in enumerate(sprite.palette_of(sp)):
        x = x2 + (i % 16) * 14
        y = py + 12 + (i // 16) * 14
        d.rectangle([x, y, x + 11, y + 11], fill=tuple(c) + (255,))

    # animation frames
    ay = 50 + max(R, py - 50 + 12 + ((ncol - 1) // 16 + 1) * 14 + 4) + 16
    aname, frames = ANIM[kind]
    label(img, 16, ay, f"ANIMATION HINT ({aname}, 3X)")
    for i, (a, g) in enumerate(frames):
        _, fsp = render(fn, n, anim=a, glow=g)
        cell = checker(n * 3, n * 3)
        cell.alpha_composite(sprite.enlarge(fsp, 3))
        img.alpha_composite(cell, (16 + i * (n * 3 + 10), ay + 12))

    # info
    iy = ay + 12 + n * 3 + 14
    lines = [f"FACTION {faction}   LAYER {layer.upper()}   SIZE {n}X{n}   FIRST {first}   "
             f"WEAK POINT {weak}"] + wrap("ROLE: " + role, 88) + [""] + LANG_NOTES[style]
    for i, line in enumerate(lines):
        raster.draw_text(img, 16, iy + i * 12, line, raster.LABEL if i else raster.ACCENT)

    gx = W - game.width * 2 - 16
    label(img, gx, 38, f"IN GAME (2X): FORMATION, SHADOWS, BULLETS, PLAYER FOR SCALE")
    img.alpha_composite(sprite.enlarge(game, 2), (gx, 50))
    return img


# --------------------------------------------------------------------------- boss

BW, BH = 288, 626          # Brood Carrier native size (x extent 2.3 units, y extent 5.0)
BF = 2                     # render factor for the full sprite


def carrier_render(rot=0.0, bays=0.0, core=0.0, factor=BF, size=(BW, BH), extent=EXTENT,
                   center=(0.0, 0.0)):
    key = ("carrier", rot, bays, core, factor, size, extent, center)
    if key not in _cache:
        scene0, mats = em.brood_carrier(0.0, 1.0, bays, core)
        scene = (lambda p: scene0(sdf.rotate_z(p, rot))) if rot else scene0
        hi = sdf.render(scene, mats, (size[0] * factor, size[1] * factor), extent,
                        center=center)
        _cache[key] = sprite.make_sprite(hi, factor, 48, crisp=70)
    return _cache[key]


def model_to_px(mx, my):
    """Model coordinates (before turning to face down) -> native sprite pixels."""
    s = BW / EXTENT
    return BW / 2 - mx * s, BH / 2 + my * s


def boss_sheet():
    full = carrier_render()
    W, H = 1300, 760
    img = raster.sheet(W, H, BOSS_TITLE, SUB)
    # 1) full sprite with callouts
    ox, oy = 40, 70
    label(img, 16, 38, f"FULL SPRITE 1X ({BW}X{BH}), FACING DOWN")
    img.alpha_composite(checker(BW, BH), (ox, oy))
    img.alpha_composite(full, (ox, oy))
    d = ImageDraw.Draw(img)
    call = [
        ("HEAD: MANDIBLE FAN TURRETS", (0.4, 2.3), "r"),
        ("EYE CLUSTER", (0.16, 2.2), "r"),
        ("LAUNCH BAYS X8: SPAWN SKITTERS", (0.72, 0.35), "r"),
        ("BAY SACS = WEAK POINTS (PHASE 2)", (0.74, -0.25), "r"),
        ("CORE UNDER PLATE IRIS (PHASE 3)", (0.0, 0.05), "r"),
        ("DORSAL PLATES (ARMOURED)", (0.2, -0.62), "r"),
        ("TRAILING TENDRILS", (0.36, -2.3), "r"),
        ("FLIGHT MEMBRANES", (1.0, 0.0), "r"),
    ]
    tx = ox + BW + 24
    pts = sorted(((model_to_px(-mx, my), text) for text, (mx, my), _ in call),
                 key=lambda v: v[0][1])
    last = oy - 40
    for (x, y), text in pts:          # labels sit level with their target, never crossing
        ly = max(oy + y - 4, last + 34)
        last = ly
        d.line([ox + x, oy + y, tx - 30, ly + 3], fill=(255, 170, 60, 200), width=1)
        d.line([tx - 30, ly + 3, tx - 4, ly + 3], fill=(255, 170, 60, 200), width=1)
        d.ellipse([ox + x - 3, oy + y - 3, ox + x + 3, oy + y + 3], outline=(255, 170, 60, 255))
        for j, part in enumerate(wrap(text, 24)):
            raster.draw_text(img, tx, ly + j * 10, part, raster.LABEL)

    # 2) play field with the broadside (phase 2), 1x
    fx, fy = 520, 50
    label(img, fx, 38, "PHASE 2 IN THE PLAY FIELD (480X540, 1X): BROADSIDE, SACS OPEN")
    field = orbit(480, 540, 12)
    broad = carrier_render(rot=np.pi / 2, bays=1.0, size=(BH, BW), extent=EXTENT * BH / BW)
    sprite.paste_center(field, broad, 240, 170)
    rng = np.random.default_rng(9)
    for bx, by in em.BROOD_BAYS[:4]:
        px_, py_ = model_to_px(bx, by)
        # rotate native coords by +90 deg around the sprite centre (broadside, facing down)
        cx = 240 + (py_ - BH / 2)
        cy = 170 + (px_ - BW / 2) + 30
        for a in (-0.5, -0.25, 0.0, 0.25, 0.5):
            dist = 40 + rng.uniform(0, 30)
            orb(field, cx + np.sin(a) * dist, cy + np.cos(a) * dist, 4)
    player = player_sprite()
    place(field, player, 240, 480, "orbit")
    for yy in (330, 380, 425):
        bolt(field, 230, yy)
        bolt(field, 250, yy)
    img.alpha_composite(field, (fx, fy + 12))

    # 3) detail crops: bay closed/open, core closed/open (2x)
    dx0 = fx + 480 + 20
    label(img, dx0, 38, "WEAK POINT DETAIL (2X)")
    ext = 0.7
    n = int(round(ext / EXTENT * BW))
    crops = [("BAY CLOSED", dict(bays=0.0), (-0.72, -0.35)),
             ("BAY OPEN", dict(bays=1.0), (-0.72, -0.35)),
             ("CORE CLOSED", dict(core=0.0), (0.0, -0.05)),
             ("CORE OPEN", dict(core=1.0), (0.0, -0.05))]
    for i, (text, kw, c) in enumerate(crops):
        cs = carrier_render(factor=4, size=(n, n), extent=ext, center=c, **kw)
        x = dx0 + (i % 2) * (n * 2 + 10)
        y = 62 + (i // 2) * (n * 2 + 30)
        cell = checker(n * 2, n * 2)
        cell.alpha_composite(sprite.enlarge(cs, 2))
        img.alpha_composite(cell, (x, y))
        label(img, x, y + n * 2 + 4, text)

    notes = ["PHASES (SEE DESIGN/ENEMIES/BOSSES):",
             "1  HULL PASSES OVERHEAD ON HIGH-AIR;",
             "   BAYS SPAWN SKITTERS + NEEDLERS.",
             "2  TURNS BROADSIDE; BAY SACS OPEN",
             "   BETWEEN FAN VOLLEYS (WEAK POINTS).",
             "3  IRIS OPENS; EXPOSED CORE FIRES",
             "   SPIRALS. PIERCING WEAPONS PAY.",
             "",
             f"SIZE: {BW}X{BH} IS ABOUT 1 SCREEN;",
             "THE DESIGN'S 'TWO SCREENS' WOULD",
             "SCALE THIS 1.7X (OPEN QUESTION).",
             "",
             "WEAK POINTS = BRIGHTEST GLOW."]
    ny = 62 + 2 * (n * 2 + 30) + 6
    for i, line in enumerate(notes):
        raster.draw_text(img, dx0, ny + i * 12, line, raster.LABEL if i else raster.ACCENT)
    return img


# --------------------------------------------------------------------------- lineup

def lineup_sheet():
    keys = list(em.ENEMIES)
    W, H = 1300, 860
    img = raster.sheet(W, H, LINEUP_TITLE, SUB)
    player = player_sprite()
    strips = [("DARK NEUTRAL", None), ("STATION HULL", hull_plating(W - 32, 90, 3)),
              ("LUNAR REGOLITH", regolith(W - 32, 90, 8)), ("EARTH FROM ORBIT", orbit(W - 32, 90, 4))]
    y = 40
    for name, bg in strips:
        label(img, 16, y, name + " (1X)")
        strip = bg.copy() if bg is not None else Image.new("RGBA", (W - 32, 90),
                                                             (22, 24, 34, 255))
        x = 40
        sprite.paste_center(strip, player, x, 45)
        x += 50
        for key in keys:
            _, sp = render(em.ENEMIES[key][4], em.ENEMIES[key][3])
            sprite.paste_center(strip, sp, x + sp.width / 2, 45)
            x += sp.width + 22
        img.alpha_composite(strip, (16, y + 12))
        y += 108
    # 2x labelled rows (wrap), then the boss at 1/4 scale
    label(img, 16, y, LINEUP_NOTE)
    x, row_y = 16, y + 14
    cell_h = 150
    for key in ["player"] + keys:
        sp = player if key == "player" else render(em.ENEMIES[key][4], em.ENEMIES[key][3])[1]
        big = sprite.enlarge(sp, 2)
        if x + big.width > W - 16 - LINEUP_RESERVE:
            x, row_y = 16, row_y + cell_h + 30
        cell = checker(big.width, cell_h)
        sprite.paste_center(cell, big, big.width / 2, cell_h / 2)
        img.alpha_composite(cell, (x, row_y))
        name = "AF-12 (PLAYER)" if key == "player" else lineup_name(key)
        for j, part in enumerate(wrap(name, max(5, big.width // 6))):
            raster.draw_text(img, x, row_y + cell_h + 4 + j * 10, part, raster.LABEL)
        x += big.width + 12
    boss = carrier_render()
    q = boss.resize((boss.width // 4, boss.height // 4), Image.NEAREST)
    bx = W - 16 - q.width - 10
    cell = checker(q.width + 10, cell_h)
    sprite.paste_center(cell, q, (q.width + 10) / 2, cell_h / 2)
    img.alpha_composite(cell, (bx, row_y))
    for j, part in enumerate(wrap("BROOD CARRIER 1/4 SCALE", 12)):
        raster.draw_text(img, bx - 80, row_y + 10 + j * 10, part, raster.LABEL)
    return img


# --------------------------------------------------------------------------- main

def out(category, name):
    d = DESIGN / category / "concept" if category else DESIGN / "concept"
    d.mkdir(parents=True, exist_ok=True)
    return d / name


def save(img, path):
    img.convert("RGB").save(path, optimize=True)
    print("wrote", path.relative_to(ROOT))


def main(args):
    for key, (title, faction, category, n, fn, kind, style) in em.ENEMIES.items():
        slug = key.rsplit("-", 1)[0]
        variant = key.rsplit("-", 1)[1]
        if args and slug not in args and key not in args:
            continue
        save(enemy_sheet(key), out(category, f"{slug}-r03-{variant}.png"))
    if not args or "brood-carrier" in args:
        save(boss_sheet(), out("bosses", "brood-carrier-r03-a.png"))
    if not args or "lineup" in args:
        save(lineup_sheet(), out(None, "lineup-r03-a.png"))


if __name__ == "__main__":
    main(sys.argv[1:])
