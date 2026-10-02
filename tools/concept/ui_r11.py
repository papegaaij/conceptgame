#!/usr/bin/env python3
"""Concept round 11: edge warnings that are harder to miss.

After playing Level 01 the user found the side and rear warnings easy to miss, even 3 s ahead.
Three looks, each in the HUD warning amber of the chosen round-09 warnings (FFC800, never a
bullet hue), drawn over a frame of Level 01's own backdrop pieces at play-field scale (480x540):

  design/ui/hud/concept/edge-warnings-r11-a.{png,gif}   A "big pulse": the round-09 bar and
      chevrons at about 3x the size (a 300 px bar, 20 px chevrons, 2x text) with a soft glow; it
      grows in over 0.2 s, then pulses smoothly between 45 % and 100 % on the flash cycle
  design/ui/hud/concept/edge-warnings-r11-b.{png,gif}   B "sweeping chevrons": a steady edge bar
      and three rows of chevrons that run in from the edge, 2 px per game frame, fading as they go
  design/ui/hud/concept/edge-warnings-r11-c.{png,gif}   C "edge glow band": the whole edge of the
      play field lights up as a 40 px amber band (stepped alpha, 25-55 %) with hazard ticks on the
      edge line, breathing on the flash cycle, and the chevrons and label in the middle

Each PNG: the side (left) and rear warnings at their brightest at 1x, and a 2x strip of one
flash cycle. Each GIF: 1.6 s of a left warning at 30 fps (the game runs at 60 Hz; the flash
cycle is 16 game frames, 0.267 s). The backdrop is still, so only the warning moves.

Run: python3 tools/concept/ui_r11.py [a] [b] [c]   (a few seconds each)
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, str(Path(__file__).resolve().parent))
from render import raster  # noqa: E402
from render.config import ROOT  # noqa: E402
from render.enemy_rigs import write_gif  # noqa: E402
from ui_r06 import tri  # noqa: E402

OUT = ROOT / "design" / "ui" / "hud" / "concept"
ASSETS = ROOT / "assets"
FW, FH = 480, 540
WARN = (255, 200, 0)
FLASH = 16 / 60
FPS = 30
LABEL = (150, 160, 185)


# --------------------------------------------------------------------------- the scene

def scene():
    """A still Level 01 frame: earth tile (deep), dock frames (ground), light banks, the ship,
    a Needler shot or two; made from the game's own placeholder assets."""
    bd = ASSETS / "backdrop" / "level-01"
    img = Image.new("RGBA", (FW, FH), (0, 0, 0, 255))
    img.alpha_composite(Image.open(bd / "earth.png").convert("RGBA").crop((0, 200, FW, 200 + FH)))
    dock = Image.open(bd / "dock-frames.png").convert("RGBA")
    img.alpha_composite(dock.crop((0, 60, FW, 60 + FH)) if dock.height >= 60 + FH else dock)
    banks = Image.open(bd / "banks-light.png").convert("RGBA")
    banks.putalpha(banks.getchannel("A").point(lambda v: v * 3 // 4))
    img.alpha_composite(banks.crop((0, 0, FW, min(FH, banks.height))))
    ship = Image.open(ASSETS / "sprites" / "ship_2.png").convert("RGBA")
    img.alpha_composite(ship, (FW // 2 - ship.width // 2, 420))
    orb = Image.open(ASSETS / "sprites" / "orb.png").convert("RGBA")
    for x, y in ((300, 210), (318, 236), (150, 120)):
        img.alpha_composite(orb, (x, y))
    return img


# --------------------------------------------------------------------------- the variants

def text_layer(size, xy, label, scale):
    lay = Image.new("RGBA", size, (0, 0, 0, 0))
    raster.draw_text(lay, int(xy[0]), int(xy[1]), label, WARN, scale=scale, shadow=(40, 26, 0))
    return lay


def with_alpha(layer, k):
    layer = layer.copy()
    layer.putalpha(layer.getchannel("A").point(lambda v: int(v * max(0.0, min(1.0, k)))))
    return layer


def chevrons(d, side, x_or_y, centre, size, alpha, rows=(-1, 1), gap=None):
    """A row of chevrons pointing away from the edge (towards where the enemies come from)."""
    gap = gap or size * 1.6
    direction = {"l": "l", "r": "r", "b": "d"}[side]
    for r in rows:
        if side in ("l", "r"):
            tri(d, x_or_y, centre + r * gap / 2 * 1.6, size, WARN + (int(alpha),), direction)
        else:
            tri(d, centre + r * gap / 2 * 1.6, x_or_y, size, WARN + (int(alpha),), direction)


def edge_point(side, inset):
    """Coordinate `inset` px in from the warned edge (x for side edges, y for the rear)."""
    return {"l": inset, "r": FW - 1 - inset, "b": FH - 1 - inset}[side]


def label_of(side):
    return {"l": "! LEFT", "r": "! RIGHT", "b": "! REAR"}[side]


def label_xy(side, inset, scale, rear_dx=80):
    """Beside the chevrons; the rear label sits right of them, low, clear of the ship."""
    tw = raster.text_width(label_of(side), scale)
    if side == "l":
        return inset, FH / 2 - 3.5 * scale
    if side == "r":
        return FW - inset - tw, FH / 2 - 3.5 * scale
    return FW / 2 + rear_dx, FH - 30 - 7 * scale


def pulse(t):
    """Smooth 45-100 % pulse on the flash cycle, brightest at t = 0."""
    return 0.45 + 0.55 * (0.5 + 0.5 * np.cos(2 * np.pi * t / FLASH))


def variant_a(side, t):
    """Big pulse: a long bar, large chevrons, 2x text, soft glow; grows in, then pulses."""
    lay = Image.new("RGBA", (FW, FH), (0, 0, 0, 0))
    glow = Image.new("RGBA", (FW, FH), (0, 0, 0, 0))
    d, gd = ImageDraw.Draw(lay), ImageDraw.Draw(glow)
    grow = min(1.0, t / 0.2)
    half = (150 if side != "b" else 170) * grow
    if side in ("l", "r"):
        x0 = 0 if side == "l" else FW - 6
        box = [x0, FH / 2 - half, x0 + 5, FH / 2 + half]
        gd.rectangle([box[0] - 6, box[1] - 6, box[2] + 6, box[3] + 6], fill=WARN + (150,))
    else:
        box = [FW / 2 - half, FH - 6, FW / 2 + half, FH - 1]
        gd.rectangle([box[0] - 6, box[1] - 6, box[2] + 6, box[3] + 6], fill=WARN + (150,))
    d.rectangle(box, fill=WARN + (255,))
    for i, aa in enumerate((1.0, 0.7, 0.42)):
        p = edge_point(side, 12 + i * 20)
        if side in ("l", "r"):
            chevrons(d, side, p + (20 if side == "l" else -20), FH / 2, 20, 255 * aa * grow, gap=40)
        else:
            chevrons(d, side, p - 20, FW / 2, 20, 255 * aa * grow, gap=50)
    glow = glow.filter(ImageFilter.GaussianBlur(5))
    out = Image.new("RGBA", (FW, FH), (0, 0, 0, 0))
    out.alpha_composite(glow)
    out.alpha_composite(lay)
    out.alpha_composite(text_layer((FW, FH), label_xy(side, 84, 2, 92), label_of(side), 2))
    return with_alpha(out, pulse(t) if t >= 0.2 else 1.0)


def variant_b(side, t):
    """Sweeping chevrons: chevrons run in from the edge (2 px per 60 Hz frame), fading."""
    out = Image.new("RGBA", (FW, FH), (0, 0, 0, 0))
    d = ImageDraw.Draw(out)
    span, speed, size = 96, 120.0, 13
    if side in ("l", "r"):
        x0 = 0 if side == "l" else FW - 4
        d.rectangle([x0, FH / 2 - 130, x0 + 3, FH / 2 + 130], fill=WARN + (210,))
    else:
        d.rectangle([FW / 2 - 150, FH - 4, FW / 2 + 150, FH - 1], fill=WARN + (210,))
    for k in range(3):                                    # three chevrons in flight per row
        s = ((t * speed / span) + k / 3) % 1.0            # 0 at the edge, 1 at the far end
        alpha = 255 * (1 - s) ** 0.8 * min(1.0, s * 6 + 0.2)
        p = edge_point(side, 8 + s * span)
        rows = (-1, 0, 1)
        if side in ("l", "r"):
            for r in rows:
                tri(d, p + (size if side == "l" else -size), FH / 2 + r * 70, size, WARN + (int(alpha),),
                    side)
        else:
            for r in rows:
                tri(d, FW / 2 + r * 90, p - size, size, WARN + (int(alpha),), "d")
    out.alpha_composite(text_layer((FW, FH), label_xy(side, 124, 2, 112), label_of(side), 2))
    return out


def variant_c(side, t):
    """Edge glow band: the whole edge lights up, stepped alpha, hazard ticks, breathing."""
    breathe = 0.25 + 0.30 * (0.5 + 0.5 * np.cos(2 * np.pi * t / FLASH))
    depth = 40
    if side in ("l", "r"):
        ramp = np.linspace(1, 0, depth) ** 1.6
        ramp = ramp if side == "l" else ramp[::-1]
        a = np.broadcast_to(ramp[None, :], (FH, depth))
        x0 = 0 if side == "l" else FW - depth
        box = (x0, 0)
    else:
        ramp = np.linspace(0, 1, depth) ** 1.6
        a = np.broadcast_to(ramp[:, None], (depth, FW))
        box = (0, FH - depth)
    a = np.round(a * breathe * 6) / 6                      # stepped alpha, 90s translucency
    band = raster.to_rgba_image(np.broadcast_to(np.array(WARN, float), a.shape + (3,)), a * 255)
    out = Image.new("RGBA", (FW, FH), (0, 0, 0, 0))
    out.alpha_composite(band, box)
    d = ImageDraw.Draw(out)
    line_a = int(255 * (0.6 + 0.4 * (0.5 + 0.5 * np.cos(2 * np.pi * t / FLASH))))
    if side in ("l", "r"):
        x0 = 0 if side == "l" else FW - 2
        d.rectangle([x0, 0, x0 + 1, FH], fill=WARN + (line_a,))
        for y in range(6, FH, 24):                         # hazard ticks on the edge line
            d.rectangle([x0 if side == "l" else x0 - 4, y, (x0 + 5) if side == "l" else x0 + 1, y + 9],
                        fill=WARN + (line_a,))
    else:
        d.rectangle([0, FH - 2, FW, FH - 1], fill=WARN + (line_a,))
        for x in range(6, FW, 24):
            d.rectangle([x, FH - 6, x + 9, FH - 1], fill=WARN + (line_a,))
    for i, aa in enumerate((1.0, 0.66, 0.36)):
        p = edge_point(side, 14 + i * 14)
        if side in ("l", "r"):
            chevrons(d, side, p + (12 if side == "l" else -12), FH / 2, 12, line_a * aa, gap=26)
        else:
            chevrons(d, side, p - 12, FW / 2, 12, line_a * aa, gap=34)
    out.alpha_composite(with_alpha(text_layer((FW, FH), label_xy(side, 66, 2, 56), label_of(side), 2),
                                   line_a / 255))
    return out


VARIANTS = {
    "a": (variant_a, "A - BIG PULSE: 300 PX BAR, 20 PX CHEVRONS, 2x TEXT, GLOW; GROWS IN, THEN PULSES 45-100 %"),
    "b": (variant_b, "B - SWEEPING CHEVRONS: STEADY BAR, CHEVRONS RUN IN AT 2 PX PER FRAME AND FADE"),
    "c": (variant_c, "C - EDGE GLOW BAND: 40 PX STEPPED AMBER BAND 25-55 %, HAZARD TICKS, CHEVRONS"),
}


# --------------------------------------------------------------------------- output

def peak_time(key):
    return {"a": 0.2, "b": 0.12, "c": 0.0}[key]


def sheet(key, bg):
    fn, title = VARIANTS[key]
    pad, top = 16, 40
    strip_t = [i * FLASH / 4 for i in range(4)]
    cw, ch = 110, 200                                       # close-up crop at 1x, shown at 2x
    w = pad * 3 + FW * 2
    h = top + FH + pad * 2 + 18 + ch * 2 + pad
    img = Image.new("RGBA", (w, h), (14, 18, 28, 255))
    raster.draw_text(img, pad, 12, "EDGE WARNINGS, ROUND 11 - " + title, WARN)
    for i, side in enumerate(("l", "b")):
        panel = bg.copy()
        panel.alpha_composite(fn(side, peak_time(key)))
        img.alpha_composite(panel, (pad + i * (FW + pad), top))
        raster.draw_text(img, pad + i * (FW + pad), top + FH + 6,
                         ("SIDE WAVE (LEFT), 1x" if side == "l" else "REAR WAVE (BOTTOM EDGE), 1x"), LABEL)
    y = top + FH + pad * 2 + 10
    raster.draw_text(img, pad, y - 4, "ONE FLASH CYCLE (0.267 S) IN 4 STEPS, 2x", LABEL)
    for i, t in enumerate(strip_t):
        panel = bg.copy()
        panel.alpha_composite(fn("l", t + (0.2 if key == "a" else 0)))
        crop = panel.crop((0, FH // 2 - ch // 2, cw, FH // 2 + ch // 2)).resize((cw * 2, ch * 2), Image.NEAREST)
        img.alpha_composite(crop, (pad + i * (cw * 2 + 12), y + 10))
    return img


def gif(key, bg):
    fn = VARIANTS[key][0]
    frames = []
    for i in range(int(1.6 * FPS)):
        f = bg.copy()
        f.alpha_composite(fn("l", i / FPS))
        frames.append(f)
    return frames


def main(keys):
    bg = scene()
    for key in keys or VARIANTS:
        path = OUT / f"edge-warnings-r11-{key}.png"
        sheet(key, bg).convert("RGB").save(path, optimize=True)
        size = write_gif(gif(key, bg), OUT / f"edge-warnings-r11-{key}.gif", fps=FPS, colors=128)
        print("wrote", path.relative_to(ROOT), "and the GIF", f"{size / 1e6:.1f} MB")


if __name__ == "__main__":
    main(sys.argv[1:])
