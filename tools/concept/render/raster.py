"""2D helpers for concept mockups: tileable noise, a 5x7 bitmap font, gradients, glows,
starfields and presentation-sheet labels. Everything is deterministic (seeded)."""
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

# --------------------------------------------------------------------------- noise


def value_noise(w, h, cell, seed, period=True):
    """Smooth value noise of size (h, w) with a lattice of ``cell`` pixels.

    With ``period`` the noise tiles seamlessly in both directions (w and h must be multiples
    of cell)."""
    rng = np.random.default_rng(seed)
    gw, gh = int(np.ceil(w / cell)), int(np.ceil(h / cell))
    grid = rng.random((gh + 1, gw + 1))
    if period:
        grid[-1, :] = grid[0, :]
        grid[:, -1] = grid[:, 0]
    ys = np.arange(h) / cell
    xs = np.arange(w) / cell
    y0 = np.floor(ys).astype(int)
    x0 = np.floor(xs).astype(int)
    fy = ys - y0
    fx = xs - x0
    fy = fy * fy * (3 - 2 * fy)
    fx = fx * fx * (3 - 2 * fx)
    a = grid[np.ix_(y0, x0)]
    b = grid[np.ix_(y0, x0 + 1)]
    c = grid[np.ix_(y0 + 1, x0)]
    d = grid[np.ix_(y0 + 1, x0 + 1)]
    top = a + (b - a) * fx[None, :]
    bot = c + (d - c) * fx[None, :]
    return top + (bot - top) * fy[:, None]


def fbm(w, h, cell, seed, octaves=5, gain=0.5, period=True):
    """Fractal sum of value noise, normalised to 0..1."""
    out = np.zeros((h, w))
    amp, total = 1.0, 0.0
    for o in range(octaves):
        c = max(1, cell >> o)
        out += amp * value_noise(w, h, c, seed + o * 101, period)
        total += amp
        amp *= gain
    out /= total
    lo, hi = out.min(), out.max()
    return (out - lo) / max(hi - lo, 1e-9)


# --------------------------------------------------------------------------- colour helpers


def hexrgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def lerp(a, b, t):
    return tuple(int(round(x + (y - x) * t)) for x, y in zip(a, b))


def ramp(stops, t):
    """Map array t (0..1) through colour stops [(pos, (r, g, b)), ...] -> float rgb array."""
    t = np.asarray(t, dtype=np.float64)
    pos = np.array([s[0] for s in stops])
    cols = np.array([s[1] for s in stops], dtype=np.float64)
    out = np.zeros(t.shape + (3,))
    for ch in range(3):
        out[..., ch] = np.interp(t, pos, cols[:, ch])
    return out


def to_rgba_image(rgb, alpha=None):
    rgb = np.clip(rgb, 0, 255).astype(np.uint8)
    if alpha is None:
        alpha = np.full(rgb.shape[:2], 255, np.uint8)
    else:
        alpha = np.clip(alpha, 0, 255).astype(np.uint8)
    return Image.fromarray(np.dstack([rgb, alpha]), "RGBA")


def glow(img, radius, strength=1.0):
    """Additive bloom of an RGBA image (returns a new image)."""
    blur = img.filter(ImageFilter.GaussianBlur(radius))
    a = np.array(img).astype(np.float64)
    b = np.array(blur).astype(np.float64)
    out = a.copy()
    out[..., :3] = a[..., :3] + b[..., :3] * (b[..., 3:4] / 255.0) * strength
    out[..., 3] = np.maximum(a[..., 3], b[..., 3] * strength)
    return Image.fromarray(np.clip(out, 0, 255).astype(np.uint8), "RGBA")


def add_light(dst, x, y, radius, color, intensity=1.0):
    """Additive radial light blob onto an RGB/RGBA image (in place via numpy)."""
    arr = np.array(dst).astype(np.float64)
    h, w = arr.shape[:2]
    x0, x1 = max(0, int(x - radius * 2)), min(w, int(x + radius * 2) + 1)
    y0, y1 = max(0, int(y - radius * 2)), min(h, int(y + radius * 2) + 1)
    if x0 >= x1 or y0 >= y1:
        return dst
    yy, xx = np.mgrid[y0:y1, x0:x1]
    d2 = ((xx - x) ** 2 + (yy - y) ** 2) / (radius * radius)
    f = np.exp(-d2 * 2.0) * intensity
    for ch in range(3):
        arr[y0:y1, x0:x1, ch] += f * color[ch]
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), dst.mode)


def starfield(w, h, seed, density=0.0025, colors=None):
    """Black sky with stars of varying brightness and a few tinted ones."""
    rng = np.random.default_rng(seed)
    img = Image.new("RGBA", (w, h), (4, 5, 12, 255))
    arr = np.array(img).astype(np.float64)
    n = int(w * h * density)
    colors = colors or [(255, 255, 255), (180, 200, 255), (255, 230, 190), (200, 220, 255)]
    for _ in range(n):
        x, y = rng.integers(0, w), rng.integers(0, h)
        b = rng.random() ** 2.5
        c = colors[rng.integers(0, len(colors))]
        arr[y, x, :3] = np.maximum(arr[y, x, :3], np.array(c) * (0.25 + 0.75 * b))
        if b > 0.85:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                xx, yy = (x + dx) % w, (y + dy) % h
                arr[yy, xx, :3] = np.maximum(arr[yy, xx, :3], np.array(c) * 0.35)
    return Image.fromarray(arr.astype(np.uint8), "RGBA")


# --------------------------------------------------------------------------- bitmap font

_GLYPHS = {
    "A": [".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"],
    "B": ["####.", "#...#", "#...#", "####.", "#...#", "#...#", "####."],
    "C": [".###.", "#...#", "#....", "#....", "#....", "#...#", ".###."],
    "D": ["####.", "#...#", "#...#", "#...#", "#...#", "#...#", "####."],
    "E": ["#####", "#....", "#....", "####.", "#....", "#....", "#####"],
    "F": ["#####", "#....", "#....", "####.", "#....", "#....", "#...."],
    "G": [".###.", "#...#", "#....", "#.###", "#...#", "#...#", ".####"],
    "H": ["#...#", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"],
    "I": [".###.", "..#..", "..#..", "..#..", "..#..", "..#..", ".###."],
    "J": ["..###", "...#.", "...#.", "...#.", "...#.", "#..#.", ".##.."],
    "K": ["#...#", "#..#.", "#.#..", "##...", "#.#..", "#..#.", "#...#"],
    "L": ["#....", "#....", "#....", "#....", "#....", "#....", "#####"],
    "M": ["#...#", "##.##", "#.#.#", "#.#.#", "#...#", "#...#", "#...#"],
    "N": ["#...#", "#...#", "##..#", "#.#.#", "#..##", "#...#", "#...#"],
    "O": [".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."],
    "P": ["####.", "#...#", "#...#", "####.", "#....", "#....", "#...."],
    "Q": [".###.", "#...#", "#...#", "#...#", "#.#.#", "#..#.", ".##.#"],
    "R": ["####.", "#...#", "#...#", "####.", "#.#..", "#..#.", "#...#"],
    "S": [".####", "#....", "#....", ".###.", "....#", "....#", "####."],
    "T": ["#####", "..#..", "..#..", "..#..", "..#..", "..#..", "..#.."],
    "U": ["#...#", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."],
    "V": ["#...#", "#...#", "#...#", "#...#", "#...#", ".#.#.", "..#.."],
    "W": ["#...#", "#...#", "#...#", "#.#.#", "#.#.#", "#.#.#", ".#.#."],
    "X": ["#...#", "#...#", ".#.#.", "..#..", ".#.#.", "#...#", "#...#"],
    "Y": ["#...#", "#...#", ".#.#.", "..#..", "..#..", "..#..", "..#.."],
    "Z": ["#####", "....#", "...#.", "..#..", ".#...", "#....", "#####"],
    "0": [".###.", "#...#", "#..##", "#.#.#", "##..#", "#...#", ".###."],
    "1": ["..#..", ".##..", "..#..", "..#..", "..#..", "..#..", ".###."],
    "2": [".###.", "#...#", "....#", "...#.", "..#..", ".#...", "#####"],
    "3": ["####.", "....#", "....#", ".###.", "....#", "....#", "####."],
    "4": ["...#.", "..##.", ".#.#.", "#..#.", "#####", "...#.", "...#."],
    "5": ["#####", "#....", "####.", "....#", "....#", "#...#", ".###."],
    "6": [".###.", "#....", "#....", "####.", "#...#", "#...#", ".###."],
    "7": ["#####", "....#", "...#.", "..#..", ".#...", ".#...", ".#..."],
    "8": [".###.", "#...#", "#...#", ".###.", "#...#", "#...#", ".###."],
    "9": [".###.", "#...#", "#...#", ".####", "....#", "....#", ".###."],
    " ": [".....", ".....", ".....", ".....", ".....", ".....", "....."],
    ".": [".....", ".....", ".....", ".....", ".....", ".##..", ".##.."],
    ",": [".....", ".....", ".....", ".....", ".##..", "..#..", ".#..."],
    ":": [".....", ".##..", ".##..", ".....", ".##..", ".##..", "....."],
    ";": [".....", ".##..", ".##..", ".....", ".##..", "..#..", ".#..."],
    "*": [".....", "#.#.#", ".###.", "#####", ".###.", "#.#.#", "....."],
    "&": [".##..", "#..#.", "#.#..", ".#...", "#.#.#", "#..#.", ".##.#"],
    "-": [".....", ".....", ".....", "#####", ".....", ".....", "....."],
    "+": [".....", "..#..", "..#..", "#####", "..#..", "..#..", "....."],
    "/": ["....#", "....#", "...#.", "..#..", ".#...", "#....", "#...."],
    "!": ["..#..", "..#..", "..#..", "..#..", "..#..", ".....", "..#.."],
    "?": [".###.", "#...#", "....#", "...#.", "..#..", ".....", "..#.."],
    "'": ["..#..", "..#..", ".#...", ".....", ".....", ".....", "....."],
    "\"": [".#.#.", ".#.#.", ".....", ".....", ".....", ".....", "....."],
    "%": ["##..#", "##..#", "...#.", "..#..", ".#...", "#..##", "#..##"],
    "(": ["...#.", "..#..", ".#...", ".#...", ".#...", "..#..", "...#."],
    ")": [".#...", "..#..", "...#.", "...#.", "...#.", "..#..", ".#..."],
    "<": ["...#.", "..#..", ".#...", "#....", ".#...", "..#..", "...#."],
    ">": [".#...", "..#..", "...#.", "....#", "...#.", "..#..", ".#..."],
    "=": [".....", ".....", "#####", ".....", "#####", ".....", "....."],
    "x": [".....", ".....", "#...#", ".#.#.", "..#..", ".#.#.", "#...#"],
    "#": [".#.#.", ".#.#.", "#####", ".#.#.", "#####", ".#.#.", ".#.#."],
    "_": [".....", ".....", ".....", ".....", ".....", ".....", "#####"],
    "[": [".###.", ".#...", ".#...", ".#...", ".#...", ".#...", ".###."],
    "]": [".###.", "...#.", "...#.", "...#.", "...#.", "...#.", ".###."],
    "|": ["..#..", "..#..", "..#..", "..#..", "..#..", "..#..", "..#.."],
}


def text_width(text, scale=1, spacing=1):
    return len(text) * (5 + spacing) * scale - spacing * scale


def draw_text(img, x, y, text, color, scale=1, spacing=1, shadow=None):
    """Draw text with the built-in 5x7 bitmap font (upper case; 'x' is a lower-case times).
    Returns the x position after the text."""
    ImageDraw.Draw(img)  # makes images created from numpy arrays writable
    px = img.load()
    w, h = img.size
    col = tuple(color) + ((255,) if len(color) == 3 else ())

    def put(gx, gy, c):
        for sy in range(scale):
            for sx in range(scale):
                xx, yy = gx + sx, gy + sy
                if 0 <= xx < w and 0 <= yy < h:
                    px[xx, yy] = c if img.mode == "RGBA" else c[:3]

    cx = x
    for ch in text:
        g = _GLYPHS.get(ch) or _GLYPHS.get(ch.upper()) or _GLYPHS["?"]
        for row, line in enumerate(g):
            for colno, v in enumerate(line):
                if v == "#":
                    if shadow is not None:
                        s = tuple(shadow) + ((255,) if len(shadow) == 3 else ())
                        put(cx + colno * scale + scale, y + row * scale + scale, s)
        for row, line in enumerate(g):
            for colno, v in enumerate(line):
                if v == "#":
                    put(cx + colno * scale, y + row * scale, col)
        cx += (5 + spacing) * scale
    return cx


# --------------------------------------------------------------------------- vector fonts

_FONT_CANDIDATES = {
    "bold": ["/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
             "/usr/share/fonts/truetype/ubuntu/Ubuntu-B.ttf",
             "/usr/share/fonts/TTF/DejaVuSans-Bold.ttf"],
    "bold-condensed": ["/usr/share/fonts/truetype/dejavu/DejaVuSansCondensed-Bold.ttf",
                       "/usr/share/fonts/truetype/ubuntu/Ubuntu-C.ttf"],
    "bold-oblique": ["/usr/share/fonts/truetype/dejavu/DejaVuSans-BoldOblique.ttf",
                     "/usr/share/fonts/truetype/ubuntu/Ubuntu-BI.ttf"],
    "serif-bold": ["/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf"],
    "mono-bold": ["/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf",
                  "/usr/share/fonts/truetype/ubuntu/UbuntuMono-B.ttf"],
}


def font(kind, size):
    """A TrueType font by kind (DejaVu preferred); falls back to Pillow's default font."""
    for path in _FONT_CANDIDATES.get(kind, []):
        if Path(path).exists():
            return ImageFont.truetype(path, size)
    return ImageFont.load_default(size)


# --------------------------------------------------------------------------- sheets

SHEET_BG = (18, 20, 28, 255)
LABEL = (200, 210, 230)
LABEL_DIM = (110, 120, 145)
ACCENT = (255, 170, 60)


def sheet(w, h, title, subtitle=None):
    """Blank presentation sheet with a title bar (bitmap font, scale 2)."""
    img = Image.new("RGBA", (w, h), SHEET_BG)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, w, 27], fill=(28, 32, 44, 255))
    d.line([0, 27, w, 27], fill=(60, 70, 95, 255))
    draw_text(img, 10, 7, title, ACCENT, scale=2)
    if subtitle:
        draw_text(img, w - text_width(subtitle) - 10, 11, subtitle, LABEL_DIM)
    return img


def frame(img, box, color=(60, 70, 95, 255)):
    ImageDraw.Draw(img).rectangle(box, outline=color)
