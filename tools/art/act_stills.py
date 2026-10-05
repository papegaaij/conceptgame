#!/usr/bin/env python3
"""Production art: the still behind an act's title card (design/campaign/act-1-first-contact, Act
intro and outro: "over a still of the Gagarin shipyards at dawn, Earth's terminator behind";
design/ui/briefing, the act title card act-title-r08-a; M4 part H batch, concept round 26).

Outputs (assets/ui/, a texture of its own like the title scene, not packed):
  act-1-first-contact-still.png   960x540: the Gagarin yards from above at dawn, as Level 01 shows
                                  them, over Earth's day side with the sunrise terminator running
                                  across behind them (the night side and its city lights bottom
                                  left, the dawn band warm along the line); the south launch rail
                                  with Lancer's Stormhawk on it on the left, the north arm with
                                  Aegis Two launching beyond it, a bridge crane over the top, open
                                  dock frames and a burning platform along the bottom, the cruiser
                                  hull on the right; the middle kept open for the chrome lettering
  design/campaign/act-1-first-contact/concept/act-1-still-final-r26-a.png   review sheet: the still,
                                  and the title card as the game draws it over it (no GIF: a still)

The look is Level 01's final backdrop (tools/art/backdrop_l01.py, approved in round 12): Earth and
the terminator are its placeholder generator's functions (tools/concept/backdrop_l01.py, frozen,
imported by path) rendered at 960x540 and posterized with 4x4 ordered dither on the wide
gradients; the structures are its production pieces from assets/backdrop/level-01/ and the far
Stormhawks its 16-heading set, placed as the level places them (the far layer hazed and smaller);
Lancer's Stormhawk is tools/art/stormhawk.py's level frame. Nothing lit is mirrored: the mirrored
placements use the level's own -mirrored pieces. The game draws the still darkened behind the
lettering (BriefingScreen.drawTitleCard).

Run: python3 tools/art/act_stills.py [--review]   (~10 s; after backdrop_l01.py and stormhawk.py)
"""
import importlib.util
import sys
from pathlib import Path

import numpy as np
from PIL import Image

import artkit
from artkit import DESIGN, ROOT, raster, sprite

from parallax_r02 import haze  # noqa: E402  (concept script, imported unchanged)
from render import sdf  # noqa: E402

_spec = importlib.util.spec_from_file_location(
    "concept_backdrop_l01", Path(__file__).resolve().parents[1] / "concept" / "backdrop_l01.py")
l01 = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(l01)

SCRIPT = "act_stills.py"
SOURCE = artkit.source_note(SCRIPT, "M4 part H batch")
ROUND = "r26"
OUT = ROOT / "assets" / "ui"
PIECES = ROOT / "assets" / "backdrop" / "level-01"
CONCEPT = DESIGN / "campaign" / "act-1-first-contact" / "concept"
W, H = 960, 540
SEA = l01.SEA
DECK_COLOURS = 32
BRIGHTNESS = 0.55                                # BriefingScreen.STILL_BRIGHTNESS, for the review
BAR = 58                                         # the title card's letterbox bars


def piece(name):
    return Image.open(PIECES / f"{name}.png").convert("RGBA")


def earth():
    """Earth far below: the level's day side and its dawn overlay, laid out for the wide still:
    the terminator a gentle diagonal behind the lettering, night bottom left."""
    day = np.array(l01.earth_tile(W, H)).astype(np.float64)
    arr = np.zeros((H, W, 4))
    arr[..., :3] = day[..., :3]
    arr[..., 3] = 1
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float64)
    line = (yy - 300) * 0.9 - (xx - 430) * 0.42          # the terminator, day above and right
    inside = np.ones((H, W), bool)
    night = sdf.smoothstep(-50, 70, line) * inside
    l01.over(arr, np.array(SEA[0], float) + 6, night * 0.84)
    rng = np.random.default_rng(2681)
    towns = raster.fbm(W, H, 32, 2683, octaves=3, period=False)
    lights = (towns > 0.6) & (rng.random((H, W)) < 0.05) & (night > 0.5)
    l01.over(arr, np.array([210, 180, 120], float), lights * 0.9)
    band = np.exp(-(line / 60.0) ** 2)                    # dawn light along the terminator
    l01.over(arr, np.array([200, 150, 100], float), band * 0.28)
    core = np.exp(-((line - 8) / 18.0) ** 2)              # its brightest strip, day side
    l01.over(arr, np.array([226, 178, 124], float), core * 0.14)
    img = l01.finish_dithered(arr, colors=DECK_COLOURS)
    a = np.array(img).astype(np.float64)
    a[..., :3] *= 0.8                                    # the deep layer recedes, as in the level
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


def far_layer():
    """The north arm and Aegis Two launching from it, at the far layer's haze."""
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    arm = piece("north-arm")
    layer.alpha_composite(arm.crop((0, 40, 180, 40 + H)), (560, 0))
    for heading, (x, y) in ((1, (636, 190)), (2, (688, 140))):   # banking away to the right
        sprite.paste_center(layer, piece(f"stormhawk-far_{heading}"), x, y)
    return layer


def ground_layer():
    """The yard's structures at the ground layer, framing the open middle."""
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    rail = piece("launch-rail")
    layer.alpha_composite(rail.crop((0, 120, 200, 120 + H)), (40, 0))
    layer.alpha_composite(piece("cruiser-hull"), (W - 236, 40))
    dock = piece("dock-frame")
    dock_m = piece("dock-frame-mirrored")
    layer.alpha_composite(dock, (250, 360))
    layer.alpha_composite(dock_m, (500, 380))
    layer.alpha_composite(piece("platform-burning_1"), (380, 430))
    layer.alpha_composite(piece("crossbeam"), (0, 476))
    layer.alpha_composite(piece("crossbeam"), (480, 476))
    crane = piece("bridge-crane")
    layer.alpha_composite(crane, (230, 64))
    return layer


def low_layer():
    """Low-air pieces over the ground: a crane jib and a lattice beam, larger than the ground."""
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    layer.alpha_composite(piece("crane-jib-mirrored"), (640, 330))
    return layer


def still():
    img = earth()
    far = far_layer()
    img.alpha_composite(haze(far, SEA[2], 0.35))
    ground = ground_layer()
    shadow = sprite.shadow_of(ground, opacity=0.45, blur=1.2)   # the art direction's drop shadow
    img.alpha_composite(shadow, (6, 8))
    img.alpha_composite(ground)
    ship = artkit.load_frames("ship")[2]
    sprite.paste_center(img, sprite.shadow_of(ship, opacity=0.45, blur=1.0), 140 + 8, 392 + 10)
    sprite.paste_center(img, ship, 140, 392)                    # Lancer on the south rail
    flame = artkit.load_frames("engine-flame")
    if flame:
        f = flame[4]
        for dx in (-5, 5):
            img = artkit.add_light(img, f, (int(140 + dx - f.width / 2), int(392 + 18)))
    low = low_layer()
    img.alpha_composite(sprite.shadow_of(low, opacity=0.4, blur=1.5), (10, 14))
    img.alpha_composite(low)
    return img.convert("RGB").convert("RGBA")


def build():
    OUT.mkdir(parents=True, exist_ok=True)
    img = still()
    artkit.save_png(img, OUT / "act-1-first-contact-still.png", SOURCE)
    print(f"act-1-first-contact-still.png: {img.width}x{img.height}, {artkit.colour_count([img])} colours")


def title_card(img):
    """The title card as BriefingScreen draws it: the still darkened, the letterbox bars, the
    chrome lettering (without the settings line and the missions, which the game sets in its font)."""
    card = Image.new("RGBA", (W, H), (0, 0, 0, 255))
    dark = Image.blend(Image.new("RGBA", (W, H), (0, 0, 0, 255)), img, BRIGHTNESS)
    card.alpha_composite(dark)
    card.paste((0, 0, 0, 255), (0, 0, W, BAR))
    card.paste((0, 0, 0, 255), (0, H - BAR, W, H))
    letters = Image.open(OUT / "act-1-first-contact-title.png").convert("RGBA")
    card.alpha_composite(letters, ((W - letters.width) // 2, 138))
    return card


def review():
    img = Image.open(OUT / "act-1-first-contact-still.png").convert("RGBA")
    sheet = raster.sheet(1000, 40 + 2 * (H + 30) + 10, "ACT 1 TITLE-CARD STILL (FINAL R26): THE GAGARIN YARDS AT DAWN",
                         f"PRODUCTION ART, M4 PART H BATCH - {ROUND.upper()}")
    raster.draw_text(sheet, 16, 38, f"THE STILL, 960X540 AT 1X ({artkit.colour_count([img])} COLOURS)", raster.LABEL)
    sheet.alpha_composite(img, (16, 52))
    raster.draw_text(sheet, 16, 52 + H + 14, f"THE TITLE CARD OVER IT: DARKENED TO {int(BRIGHTNESS * 100)} %, LETTERBOX, "
                                             "CHROME LETTERING (THE GAME ADDS THE SETTINGS LINE AND THE MISSIONS)",
                     raster.LABEL)
    sheet.alpha_composite(title_card(img), (16, 52 + H + 28))
    CONCEPT.mkdir(parents=True, exist_ok=True)
    path = CONCEPT / f"act-1-still-final-{ROUND}-a.png"
    sheet.convert("RGB").save(path, optimize=True)
    print(f"review: {path.relative_to(ROOT)}")


if __name__ == "__main__":
    if "--review" not in sys.argv[1:]:
        build()
    review()
