#!/usr/bin/env python3
"""Production art: the Salvage scanner's glint (design/player/systems, Salvage scanner; M5 part A
batch, concept round 28). Until now the scanner played the loot targets' 9x9 `glint` (three frames,
forwards and back), which reads small and subtle on a 32 px crate and is lost on a busy backdrop.
Two options, each the four frames the game plays in 0.3 s every 1.5 s at the object's centre,
additive (premultiplied on black, alpha 1 wherever it adds light), on one palette per option:

  a  "star glint" (23x23): a bigger four-point star, its long arms tapering to 10 px, short
     diagonal arms and a warm white core over a soft gold halo; it rises, flares (the diagonals
     longest in the third frame, a twinkle) and sinks back into the fading halo
  b  "scanner ping" (35x35): a thin gold ring that expands from the object's centre (radius 3, 7,
     11, 15 px) and fades as it grows, four brighter nodes on it at the diagonals (a sensor sweep's
     lock marks) and a white flash at the centre in the first two frames

Both are 2D light fields evaluated analytically at 4x and box-downsampled (art direction, render
pipeline: effects), then mapped to one median-cut palette (24 colours). The look stays in the
family of the loot targets' glint (warm white `FFF4D6`) with the gold of the salvage pickups.

Outputs:
  assets/sprites/glint-secret_0..3.png   the wired option (default a, provisional until round 28
                                         closes; `--variant b` writes b instead); a `glint` root
                                         name, so it lands on the shared sprite pages
                                         (vanguard.pipeline.SpriteUse) next to the loot glint
  design/player/systems/concept/secret-glint-r28-<a|b>.png/.gif  one review pair per option: the
                                         frames at 8x with the old glint, and the glint playing on
                                         Level 01's cargo container (station deck) and in the dark
                                         (Level 06), the old glint beside it, at 3x

The wired option's review is built from the files in assets/ (what the game loads), the other's
from the same finished frames in memory.

Run: python3 tools/art/secret_glint.py [--variant a|b] [--review]   (~2 s)
"""
import sys

import numpy as np
from PIL import Image

import artkit
from artkit import DESIGN, sprite

SCRIPT = "secret_glint.py"
BATCH = "M5 part A batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
REVIEW_ROUND = "r28"
NAME = "glint-secret"
CONCEPT = DESIGN / "player" / "systems" / "concept"
COLOURS = 24
SS = 4                                  # supersampling of the light fields
FRAMES = 4                              # SecretGlints.FRAMES: four frames in 0.3 s
WHITE = np.array([255, 246, 222]) / 255  # the loot glint's warm white
GOLD = np.array([255, 188, 84]) / 255    # the salvage pickups' gold
SIZES = {"a": 23, "b": 35}


def grid(size):
    """Sample centres at SS x SS per pixel, px from the sprite's centre (y down)."""
    c = (np.arange(size * SS) + 0.5) / SS - size / 2
    return np.meshgrid(c, c)


def to_frame(light, size):
    """An RGB light field (float, 1 = full) -> the native additive frame."""
    rgb = 1 - np.exp(-2.0 * np.maximum(light, 0))      # soft shoulder: bright cores do not clip flat
    rgb = rgb.reshape(size, SS, size, SS, 3).mean(axis=(1, 3))
    img = np.dstack([np.round(rgb * 255), np.full((size, size), 255)]).astype(np.uint8)
    return artkit.additive(Image.fromarray(img, "RGBA"))


def arm(along, across, length, width):
    """A tapering spike along one axis: full at the centre, nothing at ``length``."""
    taper = np.clip(1 - np.abs(along) / length, 0, 1) ** 1.6
    return taper * np.exp(-(across / (width * (0.35 + 0.65 * taper))) ** 2)


# (long arm length, diagonal arm length, core, halo radius, halo strength, brightness) per frame
STAR = [(5.0, 2.5, 0.8, 3.0, 0.22, 0.8),
        (10.5, 4.5, 1.0, 4.5, 0.30, 1.0),
        (8.0, 6.0, 0.9, 5.0, 0.26, 0.9),
        (4.5, 2.0, 0.6, 5.5, 0.18, 0.6)]


def star(f):
    size = SIZES["a"]
    x, y = grid(size)
    long_arm, diag, core, halo_r, halo, gain = STAR[f]
    r = np.hypot(x, y)
    u, v = (x + y) / np.sqrt(2), (x - y) / np.sqrt(2)
    spikes = np.maximum(arm(x, y, long_arm, 0.7), arm(y, x, long_arm, 0.7))
    spikes = np.maximum(spikes, 0.75 * np.maximum(arm(u, v, diag, 0.6), arm(v, u, diag, 0.6)))
    centre = core * np.exp(-(r / 1.2) ** 2)
    glow = halo * np.exp(-(r / halo_r) ** 2)
    white = np.maximum(spikes, centre) * 3.0
    light = white[..., None] * WHITE + glow[..., None] * GOLD
    return to_frame(light * gain, size)


# (ring radius, ring strength, centre flash, inner wash) per frame
PING = [(3.5, 0.80, 1.0, 0.20),
        (7.0, 0.75, 0.45, 0.14),
        (11.0, 0.55, 0.0, 0.09),
        (15.0, 0.35, 0.0, 0.05)]


def ping(f):
    size = SIZES["b"]
    x, y = grid(size)
    radius, strength, flash, wash = PING[f]
    r = np.hypot(x, y)
    ring = strength * np.exp(-((r - radius) / 0.6) ** 2)
    # Four lock marks on the ring at the diagonals, a little brighter and wider than the line.
    mark = 0.0
    for k in range(4):
        a = np.pi / 4 + k * np.pi / 2
        mark = np.maximum(mark, np.exp(-(np.hypot(x - radius * np.cos(a), y - radius * np.sin(a)) / 0.9) ** 2))
    marks = strength * 0.8 * mark
    inner = wash * np.clip(r / radius, 0, 1) ** 2 * (r < radius)
    centre = flash * np.exp(-(r / 1.1) ** 2)
    gold = 1.1 * ring + inner
    white = centre * 3.0 + marks * 2.5
    light = gold[..., None] * GOLD + white[..., None] * WHITE
    return to_frame(light, size)


DRAW = {"a": star, "b": ping}
TITLES = {"a": "OPTION A - STAR GLINT WITH A SOFT HALO", "b": "OPTION B - SCANNER PING (AN EXPANDING RING)"}


def frames_of(variant):
    return artkit.quantize_set([DRAW[variant](f) for f in range(FRAMES)], COLOURS)


def build(variant):
    artkit.write_frames(NAME, frames_of(variant), SOURCE)


# --------------------------------------------------------------------------- review

DECK = (70, 76, 104, 255)               # Level 01's station deck (loot_targets.py's review)
DARK = (8, 9, 14, 255)                  # Level 06's darkness
PANEL = (72, 56)
GIF_FPS = 20
PERIOD = 30                             # 1.5 s at 20 fps
SPARKLE = 6                             # 0.3 s


def old_frames():
    """The loot glint as SecretGlints played it before: rising, full, falling, full."""
    glint = artkit.load_frames("glint")
    return [glint[0], glint[1], glint[2], glint[1]]


def panel(background, crate, glint):
    img = Image.new("RGBA", PANEL, background)
    cx, cy = PANEL[0] // 2, PANEL[1] // 2
    sprite.paste_center(img, crate, cx, cy)
    if glint is not None:
        img = artkit.add_light(img, glint, (cx - glint.width // 2, cy - glint.height // 2))
    return img


def review(variant, wired):
    artkit.REVIEW_ROUND = REVIEW_ROUND
    new = artkit.load_frames(NAME) if variant == wired else frames_of(variant)
    old = old_frames()
    crate = artkit.load_frames("cargo-container")[0]
    lit = panel(DECK, crate, new[1])
    dark = panel(DARK, crate, new[1])
    sheet = artkit.review_sheet(f"SALVAGE SCANNER GLINT - {TITLES[variant]}", [
        ("NEW GLINT, 4 FRAMES IN 0.3 S EVERY 1.5 S (ADDITIVE)", new, 8, True),
        ("BEFORE: THE LOOT TARGETS' GLINT, PLAYED FORWARDS AND BACK", old, 8, True),
        ("FULL FRAME ON A CARGO CONTAINER: STATION DECK, IN THE DARK (4X)",
         [lit, dark, panel(DECK, crate, old[1]), panel(DARK, crate, old[1])], 4, False)],
        width=1300, batch=BATCH)
    gif = []
    for i in range(PERIOD):
        f = i * FRAMES // SPARKLE if i < SPARKLE else None
        row = Image.new("RGBA", (PANEL[0] * 2, PANEL[1] * 2))
        for col, frames in enumerate((new, old)):
            g = frames[f] if f is not None else None
            row.paste(panel(DECK, crate, g), (col * PANEL[0], 0))
            row.paste(panel(DARK, crate, g), (col * PANEL[0], PANEL[1]))
        gif.append(sprite.enlarge(row, 3))
    png = CONCEPT / f"secret-glint-{REVIEW_ROUND}-{variant}.png"
    out = png.with_suffix(".gif")
    CONCEPT.mkdir(parents=True, exist_ok=True)
    sheet.convert("RGB").save(png, optimize=True)
    artkit.write_gif(gif, out, fps=GIF_FPS, colors=128)
    print(f"review: {png.relative_to(artkit.ROOT)}, {out.relative_to(artkit.ROOT)}")


if __name__ == "__main__":
    args = sys.argv[1:]
    wired = args[args.index("--variant") + 1] if "--variant" in args else "a"
    if wired not in DRAW:
        sys.exit(f"unknown variant {wired!r}: a or b")
    if "--review" not in args:
        build(wired)
    for v in DRAW:
        review(v, wired)
