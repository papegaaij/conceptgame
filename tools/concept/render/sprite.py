"""Turn high-resolution renders into native-size palettised sprites, and compose them.

Pipeline (see design/art-direction): render at N x native size -> box-filter downsample with
coverage alpha -> hard 1-bit alpha edge -> median-cut palette of a few dozen colours.
"""
import numpy as np
from PIL import Image, ImageFilter


def downsample(rgba, factor):
    """Box-filter a float RGBA render by an integer factor (colour weighted by coverage)."""
    h, w, _ = rgba.shape
    h2, w2 = h // factor, w // factor
    a = rgba[: h2 * factor, : w2 * factor]
    a = a.reshape(h2, factor, w2, factor, 4)
    alpha = a[..., 3].mean(axis=(1, 3))
    prem = (a[..., :3] * a[..., 3:4]).sum(axis=(1, 3)) / (factor * factor)
    rgb = prem / np.maximum(alpha, 1e-6)[..., None]
    return np.concatenate([rgb, alpha[..., None]], axis=-1)


def to_image(rgba, alpha_threshold=None):
    """Float RGBA -> PIL RGBA. With a threshold the alpha becomes 1-bit (sprite colour key)."""
    arr = np.clip(rgba, 0, 1).copy()
    if alpha_threshold is not None:
        arr[..., 3] = (arr[..., 3] >= alpha_threshold).astype(np.float64)
    return Image.fromarray((arr * 255 + 0.5).astype(np.uint8), "RGBA")


def quantize(img, colors=24):
    """Quantise the opaque pixels of an RGBA sprite to a limited palette (no dithering)."""
    arr = np.array(img)
    opaque = arr[..., 3] > 0
    if not opaque.any():
        return img
    pix = arr[opaque][:, :3]
    sample = Image.fromarray(pix.reshape(-1, 1, 3), "RGB")
    pal_img = sample.quantize(colors=colors, method=Image.Quantize.MEDIANCUT,
                              dither=Image.Dither.NONE)
    rgb = Image.fromarray(arr[..., :3], "RGB").quantize(palette=pal_img,
                                                        dither=Image.Dither.NONE)
    out = np.array(rgb.convert("RGB"))
    result = np.concatenate([out, arr[..., 3:4]], axis=-1)
    result[~opaque] = 0
    return Image.fromarray(result, "RGBA")


def palette_of(img):
    """Distinct opaque colours of a sprite, sorted by luminance."""
    arr = np.array(img).reshape(-1, 4)
    cols = {tuple(c[:3]) for c in arr if c[3] > 0}
    return sorted(cols, key=lambda c: 0.3 * c[0] + 0.59 * c[1] + 0.11 * c[2])


def sharpen(img, percent=90):
    """Mild unsharp mask on the colour channels: gives the crisp look of 90s sprite renders."""
    rgb = img.convert("RGB").filter(ImageFilter.UnsharpMask(radius=1.0, percent=percent,
                                                            threshold=1))
    out = rgb.convert("RGBA")
    out.putalpha(img.getchannel("A"))
    return out


def make_sprite(rgba_hi, factor, colors=24, threshold=0.5, crisp=90):
    """High-res float render -> native-size quantised PIL sprite."""
    img = to_image(downsample(rgba_hi, factor), threshold)
    if crisp:
        img = sharpen(img, crisp)
    return quantize(img, colors)


def enlarge(img, scale):
    return img.resize((img.width * scale, img.height * scale), Image.NEAREST)


def shadow_of(sprite, opacity=0.45, blur=0.8, scale=1.0, color=(0, 0, 0)):
    """Drop shadow image for a sprite: flattened silhouette, optionally scaled and blurred."""
    a = sprite.getchannel("A")
    if scale != 1.0:
        a = a.resize((max(1, int(a.width * scale)), max(1, int(a.height * scale))),
                     Image.BILINEAR)
    if blur > 0:
        a = a.filter(ImageFilter.GaussianBlur(blur))
    a = a.point(lambda v: int(v * opacity))
    sh = Image.new("RGBA", a.size, color + (0,))
    sh.putalpha(a)
    return sh


def paste(dst, src, x, y):
    """Alpha-composite src with its top-left at (x, y) onto dst (in place), clipping edges."""
    x, y = int(round(x)), int(round(y))
    sx, sy = max(0, -x), max(0, -y)
    ex, ey = min(src.width, dst.width - x), min(src.height, dst.height - y)
    if ex <= sx or ey <= sy:
        return
    part = src if (sx, sy, ex, ey) == (0, 0, src.width, src.height) else src.crop((sx, sy, ex, ey))
    dst.alpha_composite(part, (x + sx, y + sy))


def paste_center(dst, src, x, y):
    """Alpha-composite src centred at (x, y) onto dst (in place), clipping edges."""
    paste(dst, src, x - src.width / 2, y - src.height / 2)
