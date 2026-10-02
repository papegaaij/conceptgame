"""Shared production-art helpers (design/art-direction/production): the render path every
generator in tools/art/ follows, the frame writer with its ``Source`` chunk, pivot files and the
review sheets.

Render path (art direction, render pipeline): SDF render at 8x for sprites up to 64 px and 4x
above -> box downsample (colour weighted by coverage) -> 1-bit alpha at 50 % coverage -> mild
unsharp mask -> one median-cut palette of 24-48 colours shared by **all frames of a sprite**, so
a frame change never flickers between palettes. Additive effects are stored premultiplied on
black (alpha 1 where they add light), the way the game blends them (``GL_SRC_ALPHA, GL_ONE``).

The concept ``render/`` package (and the concept scripts that made the chosen looks) are imported
unchanged; they stay frozen.
"""
import json
import re
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw
from PIL.PngImagePlugin import PngInfo

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "concept"))
from render import raster, sdf, sprite  # noqa: E402
from render.config import ROOT  # noqa: E402
from render.enemy_rigs import write_gif  # noqa: E402

SPRITES = ROOT / "assets" / "sprites"
PIVOTS = ROOT / "assets" / "pivots"
DESIGN = ROOT / "design"
REVIEW_ROUND = "r12"
SOURCE_KEY = "Source"
TAU = 2 * np.pi


def source_note(script):
    return f"tools/art/{script} (production art, Level 01 batch)"


# --------------------------------------------------------------------------- render path

def factor_for(width, height):
    """Render factor of the quality bar: 8x up to 64 px, 4x above."""
    return 8 if max(width, height) <= 64 else 4


def render_hi(scene, mats, size, extent, **kw):
    """Ray-march a model for a ``size`` (w, h) native sprite; returns (render, factor)."""
    w, h = size
    factor = factor_for(w, h)
    return sdf.render(scene, mats, (w * factor, h * factor), extent, **kw), factor


def perspective(scene, camera):
    """``scene`` seen through a perspective camera ``camera`` model units above the sprite plane
    instead of the orthographic top view: what sits higher looks larger. The ray-marcher's
    straight-down rays then sample the model scaled by (camera - z) / camera; the distance bound
    is shrunk to stay safe under that mild warp. Used where a roll must read (banking frames)."""
    def warped(p):
        q = p.copy()
        q[:, :2] *= (camera - p[:, 2:3]) / camera
        d, m = scene(q)
        return d / 1.15, m
    return warped


def native(hi, factor, crisp=90):
    """Render -> unquantised native sprite: box downsample, 1-bit alpha, unsharp mask."""
    img = sprite.to_image(sprite.downsample(hi, factor), 0.5)
    return sprite.sharpen(img, crisp) if crisp else img


def quantize_set(frames, colors):
    """Map all frames to one median-cut palette (no dithering) built from their opaque pixels."""
    arrays = [np.array(f.convert("RGBA")) for f in frames]
    pixels = np.concatenate([a[a[..., 3] > 0][:, :3] for a in arrays])
    if len(pixels) == 0:
        return frames
    palette = Image.fromarray(pixels.reshape(-1, 1, 3), "RGB").quantize(
        colors=colors, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE)
    out = []
    for a in arrays:
        rgb = Image.fromarray(np.ascontiguousarray(a[..., :3]), "RGB")
        mapped = np.array(rgb.quantize(palette=palette, dither=Image.Dither.NONE).convert("RGB"))
        result = np.dstack([mapped, a[..., 3]])
        result[a[..., 3] == 0] = 0
        out.append(Image.fromarray(result.astype(np.uint8), "RGBA"))
    return out


def additive(img):
    """Straight-alpha glow -> premultiplied on black, alpha 1 wherever it adds light."""
    a = np.array(img.convert("RGBA")).astype(np.float64)
    rgb = np.round(a[..., :3] * a[..., 3:4] / 255)
    alpha = np.where(rgb.max(axis=-1) >= 1, 255, 0)
    return Image.fromarray(np.dstack([rgb, alpha]).astype(np.uint8), "RGBA")


def stepped_alpha(img, levels=4):
    """Glow halos around a solid body: the body keeps 1-bit alpha, the soft halo outside it is
    stepped to a few translucency levels (like 90s translucency tables) instead of a smooth ramp."""
    a = np.array(img.convert("RGBA"))
    alpha = a[..., 3] / 255
    steps = levels - 1
    a[..., 3] = np.where(alpha >= 0.999, 255, np.round(np.floor(alpha * steps + 0.5) / steps * 255))
    a[a[..., 3] == 0] = 0
    return Image.fromarray(a, "RGBA")


def colour_count(frames):
    """Distinct opaque colours over a frame set."""
    return len({tuple(c[:3]) for f in frames for c in np.array(f).reshape(-1, 4) if c[3] > 0})


BAYER4 = (np.array([[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]) + 0.5) / 16


def ordered_dither(values, levels):
    """Posterize an array of 0..1 values to ``levels`` steps with 4x4 Bayer ordered dithering
    (the art direction allows it on wide gradients and translucency steps, never on lit pixels)."""
    h, w = values.shape
    threshold = np.tile(BAYER4, (h // 4 + 1, w // 4 + 1))[:h, :w]
    steps = levels - 1
    return np.floor(np.clip(values, 0, 1) * steps + threshold).clip(0, steps) / steps


# --------------------------------------------------------------------------- output

def save_png(img, path, source):
    """Save a PNG with the ``Source`` text chunk that marks it as final art."""
    info = PngInfo()
    info.add_text(SOURCE_KEY, source)
    img.save(path, pnginfo=info, optimize=True)


def write_frames(name, frames, source, single=False):
    """Write a frame set under the names the game loads (``name_<i>.png``, or ``name.png`` for a
    single region) into assets/sprites, replacing whatever set had that name before."""
    pattern = re.compile(re.escape(name) + r"(_\d+)?\.png")
    for old in SPRITES.glob(f"{name}*.png"):
        if pattern.fullmatch(old.name):
            old.unlink()
    if single:
        save_png(frames[0], SPRITES / f"{name}.png", source)
        return
    for i, frame in enumerate(frames):
        save_png(frame, SPRITES / f"{name}_{i}.png", source)


def write_pivots(name, source, data):
    """A pivot file: per frame of a sprite the points other sprites attach to (px from the
    sprite's top left), in assets/pivots/<name>.json."""
    PIVOTS.mkdir(parents=True, exist_ok=True)
    (PIVOTS / f"{name}.json").write_text(
        json.dumps({"source": source, **data}, indent=1) + "\n", encoding="utf-8")


def load_frames(name):
    """A frame set back from assets/sprites, in index order (``name.png`` for a single one)."""
    single = SPRITES / f"{name}.png"
    if single.exists():
        return [Image.open(single).convert("RGBA")]
    files = [p for p in SPRITES.glob(f"{name}_*.png") if re.fullmatch(re.escape(name) + r"_\d+\.png", p.name)]
    files.sort(key=lambda p: int(p.stem.rsplit("_", 1)[1]))
    return [Image.open(p).convert("RGBA") for p in files]


# --------------------------------------------------------------------------- angle sets

def angle_set(frame, headings, phases=1):
    """Frames of a sprite pre-rendered at ``headings`` equal steps (0 = moving down the screen,
    turning clockwise as seen on screen) times ``phases`` animation frames, indexed
    ``heading * phases + phase``. ``frame(heading_rad, phase_index) -> PIL image`` renders one,
    from the model with the key light fixed (nothing lit is rotated afterwards)."""
    return [frame(TAU * k / headings, j) for k in range(headings) for j in range(phases)]


# --------------------------------------------------------------------------- review material

SHEET_BG = raster.SHEET_BG
PLATE = (10, 12, 26, 255)


def checker(w, h, cell=4):
    img = Image.new("RGBA", (w, h), (30, 34, 46, 255))
    d = ImageDraw.Draw(img)
    for y in range(0, h, cell):
        for x in range(0, w, cell):
            if (x // cell + y // cell) % 2:
                d.rectangle([x, y, x + cell - 1, y + cell - 1], fill=(38, 42, 56, 255))
    return img


def add_light(base, glow, xy=(0, 0)):
    """``base`` with an additive frame added at ``xy``, as the game blends it (GL_SRC_ALPHA, GL_ONE)."""
    layer = Image.new("RGBA", base.size, (0, 0, 0, 0))
    layer.paste(glow, xy)
    b = np.array(base).astype(np.int32)
    g = np.array(layer).astype(np.int32)
    b[..., :3] = np.minimum(255, b[..., :3] + g[..., :3] * g[..., 3:4] // 255)
    return Image.fromarray(b.astype(np.uint8), "RGBA")


def on_background(frame, zoom, glow=False):
    """A frame enlarged on a checker (solid sprites) or added onto the dark plate (glows)."""
    w, h = frame.width * zoom, frame.height * zoom
    big = sprite.enlarge(frame, zoom)
    if glow:
        return add_light(Image.new("RGBA", (w, h), PLATE), big)
    img = checker(w, h, 4 * zoom)
    img.alpha_composite(big)
    return img


def review_sheet(title, rows, width=1300):
    """Sheet of labelled frame rows: rows = [(label, frames, zoom, glow), ...]."""
    gap = 8
    heights = [24 + max(f.height for f in frames) * zoom + gap for _, frames, zoom, _ in rows]
    img = raster.sheet(width, 40 + sum(heights) + 10, title, f"PRODUCTION ART, LEVEL 01 BATCH - {REVIEW_ROUND.upper()}")
    y = 38
    for (text, frames, zoom, glow), h in zip(rows, heights):
        raster.draw_text(img, 16, y, f"{text}  ({len(frames)} FR, {frames[0].width}X{frames[0].height}, "
                                     f"{colour_count(frames)} COLOURS, {zoom}X)", raster.LABEL)
        x = 16
        for f in frames:
            cell = on_background(f, zoom, glow)
            if x + cell.width > width - 16:
                break
            img.alpha_composite(cell, (x, y + 14))
            x += cell.width + 6
        y += h
    return img


def review_paths(concept_dir, subject):
    concept_dir = Path(concept_dir)
    concept_dir.mkdir(parents=True, exist_ok=True)
    stem = f"{subject}-final-{REVIEW_ROUND}-a"
    return concept_dir / f"{stem}.png", concept_dir / f"{stem}.gif"


def save_review(sheet, gif_frames, concept_dir, subject, fps=10):
    png, gif = review_paths(concept_dir, subject)
    sheet.convert("RGB").save(png, optimize=True)
    write_gif(gif_frames, gif, fps=fps, colors=128)
    print(f"review: {png.relative_to(ROOT)}, {gif.relative_to(ROOT)}")
