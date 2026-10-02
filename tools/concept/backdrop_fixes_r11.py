#!/usr/bin/env python3
"""Concept round 11: before/after board for the Level 01 backdrop placeholder fixes.

The fixes themselves live in backdrop_l01.py (earth-dawn: ordered dithering instead of hard
alpha/colour steps along the sunrise terminator; platform-burning: ragged venting plumes in a
dull fire ramp with smoke and embers instead of round orange blobs). This script only shows them:
"before" is read from git at BEFORE (the commit the round started from, via `git lfs smudge`),
"after" from assets/backdrop/level-01/.

Outputs (design/campaign/act-1-first-contact/level-01-break-at-dawn/concept/):
  backdrop-fixes-r11-a.png   the terminator at 2x over the earth tiles, before and after, and the
                             four burning-platform frames at 3x, before and after
  backdrop-fixes-r11-a.gif   the burning platform's 4-frame loop at 8 fps (the data file's rate),
                             before and after side by side at 3x

Run: python3 tools/concept/backdrop_fixes_r11.py
"""
import io
import subprocess
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from render import raster  # noqa: E402
from render.config import ROOT  # noqa: E402
from render.enemy_rigs import write_gif  # noqa: E402

BEFORE = "acd4d42"
PIECES = ROOT / "assets" / "backdrop" / "level-01"
OUT = ROOT / "design" / "campaign" / "act-1-first-contact" / "level-01-break-at-dawn" / "concept"
BG, LABEL, TITLE = (14, 18, 28, 255), (150, 160, 185), (255, 200, 0)


def before(name):
    rel = f"assets/backdrop/level-01/{name}.png"
    pointer = subprocess.run(["git", "show", f"{BEFORE}:{rel}"], cwd=ROOT, capture_output=True, check=True).stdout
    data = subprocess.run(["git", "lfs", "smudge", rel], cwd=ROOT, input=pointer, capture_output=True,
                          check=True).stdout
    return Image.open(io.BytesIO(data)).convert("RGBA")


def after(name):
    return Image.open(PIECES / f"{name}.png").convert("RGBA")


def over_earth(piece):
    earth = after("earth")
    out = Image.new("RGBA", piece.size, (0, 0, 0, 255))
    for y in range(0, piece.height, earth.height):
        out.alpha_composite(earth, (0, y))
    out.alpha_composite(piece)
    return out


def platform_frame(img, z=3):
    out = Image.new("RGBA", img.size, (24, 28, 44, 255))
    out.alpha_composite(img)
    return out.resize((img.width * z, img.height * z), Image.NEAREST)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    crop = (150, 1100, 480, 1330)                          # the terminator on earth-dawn
    dawn = [over_earth(f("earth-dawn")).crop(crop).resize(((crop[2] - crop[0]) * 2, (crop[3] - crop[1]) * 2),
                                                           Image.NEAREST) for f in (before, after)]
    plats = [[platform_frame(f(f"platform-burning_{i}")) for i in range(4)] for f in (before, after)]
    pad = 16
    pw = plats[0][0].width
    w = max(pad * 3 + dawn[0].width * 2, pad + 4 * (pw + 8))
    h = 40 + dawn[0].height + 30 + 2 * (pw + 30) + pad
    img = Image.new("RGBA", (w, h), BG)
    raster.draw_text(img, pad, 12, "LEVEL 01 BACKDROP PLACEHOLDER FIXES, ROUND 11 - BEFORE / AFTER", TITLE)
    for i, (d, lab) in enumerate(zip(dawn, ("BEFORE: HARD BANDS ALONG THE TERMINATOR",
                                            "AFTER: ORDERED DITHER, 16 ALPHA STEPS, 32 COLOURS"))):
        x = pad + i * (d.width + pad)
        img.alpha_composite(d, (x, 40))
        raster.draw_text(img, x, 40 + d.height + 6, "EARTH-DAWN 2x - " + lab, LABEL)
    y = 40 + dawn[0].height + 30
    for row, lab in zip(plats, ("PLATFORM-BURNING 3x, 4 FRAMES - BEFORE: ROUND ORANGE BLOBS",
                                "AFTER: RAGGED VENTING PLUMES, DULL FIRE RAMP, SMOKE, EMBERS")):
        raster.draw_text(img, pad, y, lab, LABEL)
        for i, fr in enumerate(row):
            img.alpha_composite(fr, (pad + i * (pw + 8), y + 14))
        y += pw + 30
    img.convert("RGB").save(OUT / "backdrop-fixes-r11-a.png", optimize=True)
    frames = []
    for k in range(16):                                    # 2 s at 8 fps
        f = Image.new("RGBA", (pw * 2 + 3 * pad, pw + 30), BG)
        raster.draw_text(f, pad, 8, "BEFORE", LABEL)
        raster.draw_text(f, pw + 2 * pad, 8, "AFTER", LABEL)
        f.alpha_composite(plats[0][k % 4], (pad, 22))
        f.alpha_composite(plats[1][k % 4], (pw + 2 * pad, 22))
        frames.append(f)
    size = write_gif(frames, OUT / "backdrop-fixes-r11-a.gif", fps=8, colors=128)
    print("wrote", (OUT / "backdrop-fixes-r11-a.png").relative_to(ROOT), "and the GIF", f"{size / 1e6:.2f} MB")


if __name__ == "__main__":
    main()
