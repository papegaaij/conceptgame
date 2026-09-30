#!/usr/bin/env python3
"""Concept round 04 - colour pass on the chosen round 03 enemies (role colours).

Same models and sheet layout as enemies_r03.py; only the materials change. Every Vrell unit gets
a chitin base by role family and a glow hue by kind of threat (ROLE_SCHEMES in
render/enemy_models.py, documented in design/enemies/README.md "Role colours"); the Ascendancy
stay black & gold with a per-unit secondary accent and a thin gold/red rim light. Vrell needles
are now yellow, as the bullet readability rules require.

Outputs (design/enemies/...):
  air/concept/{skitter,needler,stinger,spore-bomber,brood-pod,mantis,talon,gilded-gunship}-r04-a.png
  ground/concept/{spine-turret,polyp-mortar,rail-bunker}-r04-a.png
  bosses/concept/brood-carrier-r04-a.png
  concept/lineup-r04-a.png          lineup plus the role-colour legend

Run: python3 tools/concept/enemies_r04.py [slug ...]   (no args = everything, ~6 min)
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
import enemies_r03 as e3  # noqa: E402
from render import enemy_models as em  # noqa: E402
from render import raster, sprite  # noqa: E402
from render.config import ROOT  # noqa: E402

GOLD_RIM = (255, 200, 70)
RED_RIM = (230, 50, 40)

_orig_skitter = em.skitter_a
_orig_carrier = em.brood_carrier


def vrell(unit, fn, style):
    def model(anim=0.0, glow=1.0):
        scene, _ = fn(anim, glow)
        return scene, em.vrell_scheme_mats(unit, style, glow)
    model.__name__ = f"{fn.__name__}_r04"
    return model


def asc(unit, fn):
    def model(anim=0.0, glow=1.0):
        scene, mats = fn(anim, glow)
        new = em.asc_scheme_mats(unit, glow)
        new[em.A_GLASS] = mats[em.A_GLASS]      # keeps the gunship's charge glow
        return scene, new
    model.__name__ = f"{fn.__name__}_r04"
    return model


def carrier(anim=0.0, glow=1.0, bays_open=0.0, core_open=0.0):
    scene, _ = _orig_carrier(anim, glow, bays_open, core_open)
    return scene, em.brood_carrier_scheme_mats(glow, bays_open, core_open)


# slug -> (title, faction, category, native size, model, anim kind, header key)
R04 = {
    "skitter-a": ("SKITTER", "VRELL", "air", 30, vrell("skitter", _orig_skitter, "a"), "wing", "skitter"),
    "needler-a": ("NEEDLER", "VRELL", "air", 36, vrell("needler", em.needler_b, "b"), "claw", "needler"),
    "stinger-a": ("STINGER", "VRELL", "air", 36, vrell("stinger", em.stinger_a, "a"), "wing", "stinger"),
    "spore-bomber-a": ("SPORE BOMBER", "VRELL", "air", 48,
                       vrell("spore-bomber", em.spore_bomber_a, "a"), "pulse", "spore-bomber"),
    "brood-pod-a": ("BROOD POD", "VRELL", "air", 48, vrell("brood-pod", em.brood_pod_a, "a"),
                    "pulse", "brood-pod"),
    "mantis-a": ("MANTIS", "VRELL", "air", 60, vrell("mantis", em.mantis_a, "a"), "arms", "mantis"),
    "spine-turret-a": ("SPINE TURRET", "VRELL", "ground", 36,
                       vrell("spine-turret", em.spine_turret_a, "a"), "aim", "spine-turret"),
    "polyp-mortar-a": ("POLYP MORTAR", "VRELL", "ground", 36,
                       vrell("polyp-mortar", em.polyp_mortar_a, "a"), "pulse", "polyp-mortar"),
    "talon-a": ("TALON", "ASCENDANCY", "air", 36, asc("talon", em.talon), "bank", "talon"),
    "gilded-gunship-a": ("GILDED GUNSHIP", "ASCENDANCY", "air", 72,
                         asc("gilded-gunship", em.gilded_gunship), "charge", "gilded-gunship"),
    "rail-bunker-a": ("RAIL BUNKER", "ASCENDANCY", "ground", 42,
                      asc("rail-bunker", em.rail_bunker), "aim", "rail-bunker"),
}

ROLE_WORDS = {"plum": "SWARM FODDER", "rust": "FAST ATTACKERS / DIVERS",
              "bone": "RANGED GUNNERS", "olive": "BOMBERS / AREA DENIAL",
              "slate": "ROOTED GROUND UNITS", "teal-black": "SPAWNERS / CARRIERS"}
GLOW_WORDS = {"teal": "CONTACT / SPAWN", "violet": "AIMED SHOTS",
              "crimson": "LASERS / DIVES", "lime": "MINES / SPORES / ACID"}
ACC_WORDS = {"talon": "RED", "gilded-gunship": "WHITE", "rail-bunker": "GUNMETAL"}

for key, entry in R04.items():
    unit = entry[6]
    if unit in em.ROLE_SCHEMES:
        base, glow, weak = em.ROLE_SCHEMES[unit]
        e3.LANG[unit] = f"{base.upper()} CHITIN, {glow.upper()} GLOW"
        e3.LANG_NOTES[unit] = [
            f"ROLE COLOURS (R04): CHITIN = ROLE FAMILY ({base.upper()} = {ROLE_WORDS[base]})",
            f"GLOW = KIND OF THREAT ({glow.upper()} = {GLOW_WORDS[glow]})",
            f"WEAK POINT = BRIGHTEST {weak.upper()} GLOW. NO RESERVED HUES ON THE BODY."]
    else:
        e3.LANG[unit] = f"BLACK & GOLD + {ACC_WORDS[unit]} ACCENT, RIM LIGHT"
        e3.LANG_NOTES[unit] = [
            f"ASCENDANCY (R04): BLACK & GOLD WITH A {ACC_WORDS[unit]} SECONDARY ACCENT PER UNIT; A THIN",
            "GOLD (TOP-LEFT) / RED (BOTTOM-RIGHT) RIM LIGHT KEEPS BLACK HULLS READABLE ON DARK GROUND."]

RIM = {entry[4].__name__ for entry in R04.values() if entry[1] == "ASCENDANCY"}


def rim(sp):
    """Thin rim light on the silhouette: gold on edges facing the key light (top-left),
    red on edges facing away (bottom-right)."""
    a = np.array(sp)
    op = a[..., 3] > 0
    pad = np.pad(op, 1)
    left, up = ~pad[1:-1, :-2], ~pad[:-2, 1:-1]
    right, down = ~pad[1:-1, 2:], ~pad[2:, 1:-1]
    out = a.astype(np.float64)
    for mask, col, k in (((left | up) & op, GOLD_RIM, 0.7), ((right | down) & op & ~(left | up),
                                                             RED_RIM, 0.55)):
        out[mask, :3] = out[mask, :3] * (1 - k) + np.array(col) * k
    img = Image.fromarray(out.astype(np.uint8), "RGBA")
    return sprite.quantize(img, 32 if img.width <= 42 else 48)   # stay within 24-48 colours


_orig_render = e3.render


def render(fn, n, anim=0.0, glow=1.0, factor=e3.FACTOR, colors=None):
    hi, sp = _orig_render(fn, n, anim, glow, factor, colors)
    return (hi, rim(sp)) if fn.__name__ in RIM else (hi, sp)


_orig_thorn = e3.thorn


def thorn(img, x, y, ang, ln=7, ring=None):
    _orig_thorn(img, x, y, ang, ln, ring or e3.SHOTS[5])   # Vrell needles are yellow FFFF40


# route the round 03 sheet code through the round 04 models
e3.render = render
e3.thorn = thorn
e3.SUB = "CONCEPT ROUND 04 - 960X540"
e3.BOSS_TITLE = "BROOD CARRIER - R04: TEAL-BLACK, LIME WEAK POINTS"
e3.LINEUP_TITLE = "ENEMY LINEUP - ROUND 04 ROLE COLOURS, NATIVE SCALE"
e3.LINEUP_NOTE = "2X WITH NAMES (CHITIN = ROLE FAMILY, GLOW = KIND OF THREAT)"
e3.lineup_name = lambda key: R04[key][0]
e3.LINEUP_RESERVE = 190
em.ENEMIES = R04
em.skitter_a = R04["skitter-a"][4]
em.brood_carrier = carrier


def legend(img):
    """Append the role-colour legend under the lineup."""
    W = img.width
    body = np.array(img.convert("RGB")).astype(int)
    used = np.nonzero(np.abs(body - np.array(raster.SHEET_BG[:3])).sum(axis=(1, 2)) > 0)[0]
    img = img.crop((0, 0, W, int(used.max()) + 16))
    out = Image.new("RGBA", (W, img.height + 150), raster.SHEET_BG)
    out.alpha_composite(img, (0, 0))
    d = ImageDraw.Draw(out)
    y0 = img.height
    raster.draw_text(out, 16, y0, "ROLE COLOURS (DRAFT RULE, SEE DESIGN/ENEMIES)", raster.ACCENT)
    x = 16
    raster.draw_text(out, x, y0 + 16, "VRELL CHITIN = ROLE FAMILY", raster.LABEL_DIM)
    for i, (name, (mid, dark, bone)) in enumerate(em.CHITIN_BASES.items()):
        yy = y0 + 30 + i * 18
        for j, c in enumerate((dark, mid, bone)):
            d.rectangle([x + j * 16, yy, x + j * 16 + 13, yy + 13], fill=raster.hexrgb(c) + (255,))
        raster.draw_text(out, x + 54, yy + 3, f"{name.upper()}: {ROLE_WORDS[name]}", raster.LABEL)
    x = 440
    raster.draw_text(out, x, y0 + 16, "VRELL GLOW = KIND OF THREAT", raster.LABEL_DIM)
    for i, (name, c) in enumerate(em.GLOWS.items()):
        yy = y0 + 30 + i * 18
        d.rectangle([x, yy, x + 13, yy + 13], fill=raster.hexrgb(c) + (255,))
        raster.draw_text(out, x + 22, yy + 3, f"{name.upper()}: {GLOW_WORDS[name]}", raster.LABEL)
    x = 800
    raster.draw_text(out, x, y0 + 16, "ASCENDANCY: BLACK & GOLD + ACCENT", raster.LABEL_DIM)
    for i, (unit, c) in enumerate(em.ASC_ACCENTS.items()):
        yy = y0 + 30 + i * 18
        d.rectangle([x, yy, x + 13, yy + 13], fill=raster.hexrgb(c) + (255,))
        raster.draw_text(out, x + 22, yy + 3, f"{unit.upper()}: {ACC_WORDS[unit]}", raster.LABEL)
    raster.draw_text(out, x, y0 + 30 + 3 * 18 + 3, "RIM: GOLD TOP-LEFT, RED BOTTOM-RIGHT",
                     raster.LABEL)
    raster.draw_text(out, 440, y0 + 30 + 4 * 18 + 3,
                     "RESERVED, NEVER ON BODIES: MAGENTA + YELLOW + ORANGE (ENEMY BULLETS),",
                     raster.LABEL_DIM)
    raster.draw_text(out, 440, y0 + 30 + 5 * 18 + 3, "BLUE / WHITE / CYAN (PLAYER AND PLAYER SHOTS).",
                     raster.LABEL_DIM)
    return out


def main(args):
    for key, entry in R04.items():
        slug, category = key.rsplit("-", 1)[0], entry[2]
        if args and slug not in args:
            continue
        e3.save(e3.enemy_sheet(key), e3.out(category, f"{slug}-r04-a.png"))
    if not args or "brood-carrier" in args:
        e3.save(e3.boss_sheet(), e3.out("bosses", "brood-carrier-r04-a.png"))
    if not args or "lineup" in args:
        e3.save(legend(e3.lineup_sheet()), e3.out(None, "lineup-r04-a.png"))


if __name__ == "__main__":
    main(sys.argv[1:])
