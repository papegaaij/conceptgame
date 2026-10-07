"""Production art: Level 08's low-air traffic (design/campaign/act-2-homefront/level-08-neon-skyline;
M5 part B batch), variant A "wedge cars" of concept round 30 (user choice): a civilian wedge sedan,
a box van, and the CDF gunship. Imported by backdrop_l08.py, which writes the frames as backdrop
pieces (assets/backdrop/level-08/<id>_<n>.png) and the review.

The models are the concept's (tools/concept/props_r30.py, imported unchanged: sedan_a, van_a,
gunship_a, their materials, the lamp positions and the muting), rendered here at 16 headings (`_0`
nose up the screen, clockwise in 22.5 degree steps) by turning the model under the fixed top-left
key light, 8x supersampled and box-reduced (artkit's render path), muted as the concept does
(82 % brightness, 75 % saturation: scenery recedes, no rim, no glint) and posterized to one
32-colour palette per heading set. A piece holds one paint (a stream repeats one piece): the
sedan in the concept's dusty slate, the van in its beige.

Lamps: the head and tail lights and the gunship's red/green nav lights get the concept's small
halo, baked into the frame (a backdrop piece is drawn alpha-blended, not additive): over the body
the light is added, outside it the halo is stored as its own colour at a stepped alpha (3 steps),
which over the night city reads as the concept's additive glow. The gunship's strobe is lit but
steady (a piece's images are its headings, so it cannot blink) and has no halo. No shadow is baked:
a low-air piece has none in the game, and one would darken the ground units passing under it.

Frames are square, big enough for the model at every heading (centred on the model's middle):
aircar-sedan 24x24, aircar-van 28x28, gunship 44x44.
"""
import numpy as np
from PIL import Image

import artkit
from artkit import sdf, sprite

import props_r30 as p30  # noqa: E402  (concept script, imported unchanged)

HEADINGS = 16
COLOURS = 32
CRAFT = {   # id -> (concept model, paint index or None, concept key, frame side px)
    "aircar-sedan": (p30.sedan_a, 0, "sedan", 24),
    "aircar-van": (p30.van_a, 1, "van", 28),
    "gunship": (p30.gunship_a, None, "gunship", 44),
}
HALO_KINDS = "htrg"            # head, tail, red and green nav lights; the strobe ("s") stays steady


def model(name):
    fn, paint, _, _ = CRAFT[name]
    return fn(paint) if paint is not None else fn()


def lamp_glow(name, deg, side):
    """The lamps' halos at heading ``deg`` on a ``side`` px frame: an (h, w, 3) light field, 0..255."""
    key = CRAFT[name][2]
    glow = np.zeros((side, side, 3))
    for dx, dy, r, kind, halo in p30.light_halos("a", key, deg):
        if kind in HALO_KINDS:
            p30.add(glow, halo, side / 2 + dx - r, side / 2 + dy - r)
    return glow


def bake(arr, glow, steps=3):
    """The halo on a straight-alpha frame: added over the body, a stepped translucent colour outside."""
    out = arr.copy()
    body = out[..., 3] >= 255
    out[body, :3] = np.minimum(255, out[body, :3] + glow[body])
    peak = glow.max(axis=-1)
    a = np.floor(np.clip(peak / 255, 0, 1) * steps + 0.5) / steps
    outside = ~body & (a > 0)
    colour = glow / np.maximum(peak, 1e-6)[..., None] * 255
    out[outside, :3] = colour[outside]
    out[outside, 3] = a[outside] * 255
    out[~body & (a <= 0)] = 0
    return out


def frame(name, k, n=HEADINGS):
    side = CRAFT[name][3]
    deg = k * 360 / n
    f = 8
    hi = sdf.render(p30.rotated(model(name), deg), p30.tr_materials(), (side * f, side * f), float(side),
                    z_top=float(side), steps=160)
    arr = np.array(artkit.native(hi, f, crisp=60)).astype(np.float64)
    arr = p30.mute(arr)
    return bake(arr, lamp_glow(name, deg, side))


def quantize(imgs, colours=COLOURS, lamp_weight=24):
    """One median-cut palette (no dithering) for the heading set, as artkit.quantize_set, with the
    few lamp pixels weighted up so the white strobe and the lamps keep their own colours."""
    arrays = [np.array(f) for f in imgs]
    pixels = np.concatenate([a[a[..., 3] > 0][:, :3] for a in arrays])
    lamps = pixels[(pixels.max(axis=1) > 150) | (np.ptp(pixels.astype(int), axis=1) > 90)]
    sample = np.concatenate([pixels] + [lamps] * lamp_weight)
    palette = Image.fromarray(sample.reshape(-1, 1, 3), "RGB").quantize(
        colors=colours, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE)
    out = []
    for a in arrays:
        rgb = Image.fromarray(np.ascontiguousarray(a[..., :3]), "RGB")
        mapped = np.array(rgb.quantize(palette=palette, dither=Image.Dither.NONE).convert("RGB"))
        result = np.dstack([mapped, a[..., 3]])
        result[a[..., 3] == 0] = 0
        out.append(Image.fromarray(result.astype(np.uint8), "RGBA"))
    return out


def frames(name, n=HEADINGS):
    imgs = [Image.fromarray(np.clip(frame(name, k, n), 0, 255).astype(np.uint8), "RGBA") for k in range(n)]
    return quantize(imgs)


if __name__ == "__main__":
    import sys
    for nm in sys.argv[1:] or list(CRAFT):
        fr = frames(nm)
        bbox = [f.getbbox() for f in fr]
        print(nm, fr[0].size, artkit.colour_count(fr), "colours; bboxes", bbox[0], bbox[2], bbox[4])
