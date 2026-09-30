#!/usr/bin/env python3
"""Concept round 01 - working title and logo, four candidates.

Outputs (design/ui/main-menu/concept/):
  logo-r01-a.png   STORMHAWK       - classic sky/ground chrome, italic, orange rim glow
  logo-r01-b.png   AEGIS WING      - gold bevel with blue outline and wing emblem
  logo-r01-c.png   LAST LINE 2185  - brushed steel with a red glowing year
  logo-r01-d.png   TERRAN VANGUARD - blue steel, deep 3D extrusion, cold glow

All logos: bevelled/lit text mask (light from the top-left), gradient fills, outlines and glows
over a starfield with nebula. Fonts: DejaVu Sans (falls back to other system fonts).
Run: python3 tools/concept/logos.py
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, str(Path(__file__).resolve().parent))
from render import raster  # noqa: E402
from render.config import out_path  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "design" / "ui" / "main-menu" / "concept"
W, H = 1000, 420


# --------------------------------------------------------------------------- building blocks

def text_mask(text, kind, size, tracking=0, skew=0.0):
    fnt = raster.font(kind, size)
    widths = [fnt.getbbox(c)[2] - fnt.getbbox(c)[0] if c != " " else size * 0.3 for c in text]
    total = int(sum(widths) + tracking * (len(text) - 1) + size)
    img = Image.new("L", (total, int(size * 1.5)), 0)
    d = ImageDraw.Draw(img)
    x = size * 0.5
    for c, w in zip(text, widths):
        if c != " ":
            d.text((x - fnt.getbbox(c)[0], size * 0.15), c, font=fnt, fill=255)
        x += w + tracking
    if skew:
        img = img.transform((img.width + int(abs(skew) * img.height), img.height), Image.AFFINE,
                            (1, skew, -abs(skew) * img.height if skew > 0 else 0, 0, 1, 0),
                            Image.BICUBIC)
    return img.crop(img.getbbox())


def pad(mask, p):
    out = Image.new("L", (mask.width + 2 * p, mask.height + 2 * p), 0)
    out.paste(mask, (p, p))
    return out


def bevel(mask, radius, strength=2.2):
    """Lighting term (float array, ~0..1.5) from a blurred height field of the mask."""
    hgt = np.array(mask.filter(ImageFilter.GaussianBlur(radius))).astype(np.float64) / 255
    gy, gx = np.gradient(hgt)
    nx, ny, nz = -gx * strength * radius, -gy * strength * radius, np.ones_like(hgt)
    n = np.sqrt(nx ** 2 + ny ** 2 + nz ** 2)
    lx, ly, lz = -0.55, -0.6, 0.58
    ndl = (nx * lx + ny * ly + nz * lz) / n
    hx, hy, hz = lx, ly, lz + 1
    hn = np.sqrt(hx ** 2 + hy ** 2 + hz ** 2)
    spec = np.clip((nx * hx + ny * hy + nz * hz) / (n * hn), 0, 1) ** 40
    return np.clip(ndl, 0, 1), spec


def vertical_fill(h, w, stops):
    t = np.linspace(0, 1, h)[:, None] * np.ones((1, w))
    return raster.ramp(stops, t)


def colorize(mask, rgb, alpha_scale=1.0):
    a = np.array(mask).astype(np.float64) * alpha_scale
    return raster.to_rgba_image(rgb, a)


def solid(mask, color, alpha_scale=1.0):
    rgb = np.zeros((mask.height, mask.width, 3))
    rgb[:] = color
    return colorize(mask, rgb, alpha_scale)


def dilate(mask, px):
    m = mask
    for _ in range(px):
        m = m.filter(ImageFilter.MaxFilter(3))
    return m


def surface(mask, stops, bevel_radius=4, spec_color=(255, 255, 255), texture=None,
            shade_min=0.45):
    """Gradient-filled, bevel-lit face of the lettering."""
    rgb = vertical_fill(mask.height, mask.width, stops)
    if texture is not None:
        rgb *= texture[..., None]
    ndl, spec = bevel(mask, bevel_radius)
    rgb = rgb * (shade_min + (1 - shade_min) * 1.3 * ndl)[..., None] + \
        np.array(spec_color) * spec[..., None] * 1.2
    return colorize(mask, rgb)


def background(seed, nebula=((40, 10, 80), (10, 40, 90))):
    bg = raster.starfield(W, H, seed, density=0.004)
    n = raster.fbm(W, H, 128, seed + 5, octaves=5, period=False)
    n2 = raster.fbm(W, H, 96, seed + 9, octaves=4, period=False)
    a = np.array(bg).astype(np.float64)
    neb = (np.clip(n - 0.45, 0, 1) * 2.2)[..., None] * np.array(nebula[0]) + \
          (np.clip(n2 - 0.5, 0, 1) * 2.2)[..., None] * np.array(nebula[1])
    a[..., :3] += neb
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


def lens_flare(img, x, y, color=(255, 220, 170), scale=1.0):
    img = raster.add_light(img, x, y, 40 * scale, color, 1.0)
    img = raster.add_light(img, x, y, 8 * scale, (255, 255, 255), 1.6)
    arr = np.array(img).astype(np.float64)
    arr[int(y) - 1:int(y) + 2, max(0, int(x - 220 * scale)):int(x + 220 * scale), :3] += \
        np.array(color) * 0.6
    arr[max(0, int(y - 60 * scale)):int(y + 60 * scale), int(x), :3] += np.array(color) * 0.35
    img = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), img.mode)
    for k, (r, c) in enumerate(((14, (120, 200, 255)), (24, (255, 140, 80)), (9, (180, 255, 180)))):
        img = raster.add_light(img, W / 2 + (W / 2 - x) * (0.4 + 0.3 * k),
                               H / 2 + (H / 2 - y) * (0.4 + 0.3 * k), r * scale, c, 0.25)
    return img


def place(bg, layer, cx, cy):
    bg.alpha_composite(layer, (int(cx - layer.width / 2), int(cy - layer.height / 2)))


def subtitle(img, text, y, color, kind="bold", size=26, tracking=10):
    m = pad(text_mask(text, kind, size, tracking=tracking), 6)
    glow = solid(m.filter(ImageFilter.GaussianBlur(4)), color, 0.8)
    place(img, glow, W / 2, y)
    place(img, solid(m, (235, 240, 250)), W / 2, y)


def label(img, key, title):
    raster.draw_text(img, 12, H - 18, f"LOGO {key.upper()}: {title}   (CONCEPT ROUND 01)",
                     (120, 130, 160))


# --------------------------------------------------------------------------- candidates

CHROME = [(0.0, (70, 110, 200)), (0.38, (210, 230, 255)), (0.5, (255, 255, 255)),
          (0.52, (60, 40, 40)), (0.62, (150, 80, 40)), (0.85, (255, 190, 110)),
          (1.0, (255, 240, 210))]


def logo_a():
    img = background(101)
    m = pad(text_mask("STORMHAWK", "bold-oblique", 118, tracking=4, skew=0.12), 60)
    outer = dilate(m, 7)
    place(img, solid(outer.filter(ImageFilter.GaussianBlur(14)), (255, 120, 30), 0.9), W / 2, 170)
    place(img, solid(outer, (20, 10, 30)), W / 2, 170)
    place(img, solid(dilate(m, 3), (255, 170, 60)), W / 2, 170)
    place(img, surface(m, CHROME, 5), W / 2, 170)
    img = lens_flare(img, W / 2 + 232, 128, scale=1.0)
    subtitle(img, "THE VRELL WAR", 290, (255, 140, 40))
    label(img, "a", "STORMHAWK")
    return img


GOLD = [(0.0, (120, 70, 10)), (0.3, (255, 220, 120)), (0.48, (255, 250, 210)),
        (0.5, (150, 90, 20)), (0.75, (230, 160, 50)), (1.0, (255, 230, 150))]


def wing_emblem(size=260):
    """Stylised eagle wings behind the title (bevelled mask)."""
    s = 4
    m = Image.new("L", (size * 2 * s, size * s // 2), 0)
    d = ImageDraw.Draw(m)
    cx = size * s
    for side in (-1, 1):
        for k in range(5):
            y0 = 20 * s + k * 16 * s
            length = (size - 30 - k * 30) * s
            d.polygon([(cx + side * 30 * s, y0), (cx + side * (30 * s + length), y0 - 20 * s + k * 6 * s),
                       (cx + side * (30 * s + length - 40 * s), y0 + 12 * s),
                       (cx + side * 30 * s, y0 + 12 * s)], fill=255)
    d.polygon([(cx - 26 * s, 10 * s), (cx + 26 * s, 10 * s), (cx, 110 * s)], fill=255)
    return m.resize((m.width // s, m.height // s), Image.LANCZOS)


def logo_b():
    img = background(202, nebula=((10, 30, 70), (30, 20, 60)))
    wings = pad(wing_emblem(), 16)
    place(img, solid(dilate(wings, 3), (20, 60, 140)), W / 2, 120)
    place(img, surface(wings, [(0, (70, 90, 120)), (0.5, (200, 210, 230)), (1, (90, 100, 130))], 3),
          W / 2, 120)
    m = pad(text_mask("AEGIS WING", "bold", 100, tracking=16), 60)
    outer = dilate(m, 6)
    place(img, solid(outer.filter(ImageFilter.GaussianBlur(12)), (40, 120, 255), 0.9), W / 2, 225)
    place(img, solid(outer, (10, 20, 60)), W / 2, 225)
    place(img, solid(dilate(m, 2), (90, 170, 255)), W / 2, 225)
    place(img, surface(m, GOLD, 5, spec_color=(255, 250, 220)), W / 2, 225)
    subtitle(img, "DEFENDERS OF SOL  -  2185", 322, (60, 140, 255), size=22, tracking=8)
    label(img, "b", "AEGIS WING")
    return img


STEEL = [(0.0, (120, 126, 140)), (0.45, (230, 234, 240)), (0.5, (90, 96, 110)),
         (1.0, (180, 186, 200))]


def logo_c():
    img = background(303, nebula=((70, 10, 10), (20, 20, 40)))
    m = pad(text_mask("LAST LINE", "bold-condensed", 140, tracking=6), 40)
    brushed = raster.value_noise(m.width, m.height, 3, 7, period=False)
    brushed = 0.85 + 0.3 * np.repeat(brushed[:, :1], m.width, axis=1) * \
        (0.7 + 0.3 * raster.value_noise(m.width, m.height, 40, 8, period=False))
    scratches = raster.fbm(m.width, m.height, 6, 9, octaves=2, period=False)
    tex = brushed * (1 - 0.25 * (scratches > 0.8))
    place(img, solid(dilate(m, 5), (10, 10, 14)), W / 2, 150)
    place(img, solid(dilate(m, 2), (120, 20, 20)), W / 2, 150)
    place(img, surface(m, STEEL, 3, texture=tex, shade_min=0.35), W / 2, 150)
    year = pad(text_mask("2185", "mono-bold", 96, tracking=14), 30)
    place(img, solid(year.filter(ImageFilter.GaussianBlur(10)), (255, 40, 20), 1.0), W / 2, 275)
    place(img, solid(year.filter(ImageFilter.GaussianBlur(3)), (255, 80, 40), 1.0), W / 2, 275)
    lines = np.array(year).astype(np.float64)
    lines[1::3] *= 0.55    # LED scanlines
    place(img, surface(Image.fromarray(lines.astype(np.uint8)),
                       [(0, (255, 200, 170)), (1, (255, 60, 30))], 2, shade_min=0.8), W / 2, 275)
    label(img, "c", "LAST LINE 2185")
    return img


BLUE_STEEL = [(0.0, (160, 200, 255)), (0.45, (240, 248, 255)), (0.5, (40, 70, 130)),
              (0.8, (90, 140, 220)), (1.0, (200, 230, 255))]


def logo_d():
    img = background(404, nebula=((10, 20, 90), (60, 10, 90)))
    m = pad(text_mask("TERRAN", "bold", 130, tracking=10), 40)
    v = pad(text_mask("VANGUARD", "bold", 92, tracking=16), 40)
    for mask, cy in ((m, 135), (v, 250)):
        # deep 3D extrusion towards the bottom-right (away from the light)
        depth = 16
        for k in range(depth, 0, -1):
            t = k / depth
            col = (int(20 + 30 * (1 - t)), int(30 + 50 * (1 - t)), int(70 + 90 * (1 - t)))
            place(img, solid(mask, col), W / 2 + k * 0.9, cy + k * 1.2)
        glow = solid(dilate(mask, 4).filter(ImageFilter.GaussianBlur(9)), (80, 170, 255), 0.7)
        place(img, glow, W / 2, cy)
        place(img, solid(dilate(mask, 2), (220, 240, 255)), W / 2, cy)
        place(img, surface(mask, BLUE_STEEL, 4), W / 2, cy)
    img = lens_flare(img, 190, 110, color=(170, 210, 255), scale=0.8)
    label(img, "d", "TERRAN VANGUARD")
    return img


LOGOS = {"a": logo_a, "b": logo_b, "c": logo_c, "d": logo_d}


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for key, fn in LOGOS.items():
        path = out_path(OUT, f"logo-r01-{key}.png")
        fn().convert("RGB").save(path, optimize=True)
        print("wrote", path.relative_to(ROOT))


if __name__ == "__main__":
    main()
