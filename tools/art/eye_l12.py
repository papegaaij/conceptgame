#!/usr/bin/env python3
"""The eye of Level 12's storm (design/campaign/act-2-homefront/level-12-storm-front, section 5 "The
Eye"; M5 part F batch, concept round 34): the turning cloud wall all round and the shaft of pale
light on the calm sea, two looks a/b for the user (decision F8 = a). This script is the eye's
**look generator**; step 5's backdrop_l12.py imports ``wall_ring(look)`` and ``shaft(look)`` for
the production images, so the chosen look is made once.

What the game draws (step 5/6): the eye's two pieces are **screen-fixed weather looks**, not
scrolling tiles: the sea (the `deep` calm swell) scrolls under them as everywhere.
  wall_ring(look)   RGBA RING x RING (760 px: the play field's 480 x 540 diagonal plus a margin),
                    centred on the play field's centre, turned clockwise at 3 deg/s about its
                    centre (the README; the storm's direction is still an open question). It is lit
                    **radially** (the light falls into the eye from the open sky above it, so every
                    side of the wall is lit the same way) so turning the image keeps the lighting
                    right: the one lit image that may be rotated. Opaque wall, a soft translucent
                    inner edge (mist at the wall's foot), posterized.
  shaft(look)       RGBA 480 x 540 additive light (premultiplied on black): the pool of pale light
                    on the sea and the faint haze of the shaft above it, screen-fixed; drawn over
                    the sea, under the ring.

Looks:
  a  "billowing wall, cool shaft": a towering wall of round billows, a gentle spiral in its
     texture, a bright silver rim where the light falls on its inner face; a cool silver-white
     pool off-centre (up and left, toward the break in the cloud) with a soft haze column.
  b  "banded spiral, warm shaft": a wall of stretched spiral bands with two thin feeder arms
     curling into the eye, darker and more ominous; a broad pale-gold pool in the centre with
     faint rays and slow glints on the swell.

Outputs: design/campaign/act-2-homefront/level-12-storm-front/concept/eye-r34-a.png/.gif and
eye-r34-b.png/.gif (a sheet: the composed eye, the ring and the shaft on their own, the ring turned
by 30 deg; a GIF: 6 s at 10 fps in real time, the sea scrolling under the turning wall, not a loop).
The sea is a stand-in (Level 11's swell tile, darkened and calmed); step 5 makes the eye's own sea.

Run: python3 tools/art/eye_l12.py [a] [b]   (~20 s)
"""
import sys

import numpy as np
from PIL import Image

import artkit
from artkit import DESIGN, ROOT, write_gif
from render import raster  # noqa: E402

REVIEW_ROUND = "r34"
CONCEPT = DESIGN / "campaign" / "act-2-homefront" / "level-12-storm-front" / "concept"
W, H = 480, 540                                 # the play field
RING = 760
TURN = 3.0                                      # deg/s, clockwise
SEA_SPEED = 140 * 0.85                          # the swell's scroll (Level 11's `deep` factor)

LOOKS = {
    "a": dict(inner=228, wobble=34, billow=1.0, stretch=1.0, spiral=0.55, arms=0, colours=28,
              base=(50, 57, 67), lit=(206, 214, 226), dark=(24, 28, 36), mist=(120, 132, 146),
              pool=(214, 226, 255), pool_at=(196, 214), pool_r=(92, 70), pool_gain=0.42, haze=0.12,
              rays=0.0, glints=0),
    "b": dict(inner=218, wobble=26, billow=0.55, stretch=3.0, spiral=1.4, arms=2, colours=28,
              base=(42, 45, 54), lit=(214, 196, 160), dark=(18, 20, 28), mist=(104, 104, 112),
              pool=(255, 226, 168), pool_at=(240, 262), pool_r=(130, 104), pool_gain=0.36, haze=0.06,
              rays=0.0, glints=40),
}


# =========================================================================== noise

def spectral_noise(shape, seed, beta=2.0, scale=(1.0, 1.0), low=2.0):
    """Periodic 1/f^beta noise on a grid (both axes wrap), its frequencies scaled per axis (an
    axis scale > 1 stretches the features along it); normalised to 0..1."""
    rng = np.random.default_rng(seed)
    fy = np.fft.fftfreq(shape[0])[:, None] * shape[0] / scale[0]
    fx = np.fft.rfftfreq(shape[1])[None, :] * shape[1] / scale[1]
    f = np.hypot(fy, fx)
    amp = np.where(f < low, 0.0, 1.0 / np.maximum(f, 1e-9) ** (beta / 2))
    spec = (rng.standard_normal(f.shape) + 1j * rng.standard_normal(f.shape)) * amp
    x = np.fft.irfft2(spec, s=shape)
    return (x - x.min()) / (x.max() - x.min())


def sample(grid, u, v):
    """Bilinear sample of a periodic grid at fractional rows u and columns v (both wrap)."""
    h, w = grid.shape
    u0, v0 = np.floor(u).astype(int), np.floor(v).astype(int)
    du, dv = u - u0, v - v0
    u0, v0 = u0 % h, v0 % w
    u1, v1 = (u0 + 1) % h, (v0 + 1) % w
    return (grid[u0, v0] * (1 - du) * (1 - dv) + grid[u0, v1] * (1 - du) * dv
            + grid[u1, v0] * du * (1 - dv) + grid[u1, v1] * du * dv)


def lerp(a, b, t):
    return np.array(a, float) + (np.array(b, float) - np.array(a, float)) * np.asarray(t)[..., None]


# =========================================================================== the wall

def wall_ring(look):
    """The cloud wall as an RGBA ring (see the module's notes): a height field of billows (a) or
    spiral bands (b) on a wall rising from the eye's edge, lit radially from the eye (a light from
    the centre and above, so the inner faces catch it, the same on every side)."""
    p = LOOKS[look]
    yy, xx = np.mgrid[0:RING, 0:RING].astype(np.float64) + 0.5
    dx, dy = xx - RING / 2, yy - RING / 2
    r = np.maximum(np.hypot(dx, dy), 1.0)
    th = np.arctan2(dy, dx)                                  # screen angle (y down: clockwise)
    seed = 3401 if look == "a" else 3411
    # the texture in (radius, arc length at the eye's edge), 0.14 cells a px; the angle wraps and the
    # spiral shears it with ln r; b's bands stretch along the arc
    k = 0.14
    n_th = int(2 * np.pi * p["inner"] * k)
    n_r = int(RING / 2 * k)
    big = spectral_noise((n_r, n_th), seed, beta=3.0, scale=(1.0, p["stretch"]), low=1.0)
    mid = spectral_noise((n_r, n_th), seed + 1, beta=2.6, scale=(1.0, p["stretch"]), low=2.0)
    shear = p["spiral"] * np.log(r / p["inner"])
    v = (th + shear) / (2 * np.pi) * n_th
    u = r * k
    puff = 1 - np.abs(sample(mid, u, v) - 0.5) * 2                     # round billow crowns
    tex = 0.55 * sample(big, u, v) + 0.45 * (puff * p["billow"] + sample(mid, u * 1.6, v * 1.6) * (1 - p["billow"]))
    # the eye's edge: a few slow lobes round the circle (no spikes)
    rng = np.random.default_rng(seed + 2)
    lobes = sum(rng.uniform(0.3, 1.0) / h * np.cos(h * th + rng.uniform(0, 2 * np.pi)) for h in range(2, 9))
    edge = p["inner"] + p["wobble"] * lobes + 22 * (sample(big, u * 0 + 5, v) - 0.5)
    s = (r - edge) / 70.0                                    # 0 at the foot, 1 at 70 px into the wall
    # the height: the wall rising over ~100 px, the texture's billows on it
    rise = np.clip(s, 0, 1.5)
    hgt = 70 * (1 - np.exp(-2.2 * np.clip(s, 0, None))) + 34 * tex * np.clip(s + 0.3, 0, 1)
    gy, gx = np.gradient(hgt)
    nz = 1.0
    norm = np.sqrt(gx ** 2 + gy ** 2 + nz)
    lx, ly, lz = -dx / r * 0.88, -dy / r * 0.88, 0.47         # toward the eye's centre and up
    lam = np.clip((-gx * lx - gy * ly + nz * lz) / norm, 0, 1)
    lit = np.clip((lam - 0.5) / 0.5, 0, 1) ** 1.4 * np.clip(1.2 - 0.35 * rise, 0.5, 1)
    rgb = lerp(p["dark"], p["base"], np.clip(0.3 + tex, 0, 1))
    rgb = rgb + (np.array(p["lit"], float) - rgb) * lit[..., None]
    # feeder arms (b): thin spiral bands curling into the eye
    arms = np.zeros_like(r)
    for j in range(p["arms"]):
        phase = (th + 2.0 * np.log(r / p["inner"]) + j * np.pi) % (2 * np.pi)
        band = np.exp(-((phase - np.pi) / 0.2) ** 2) * np.clip((r - (p["inner"] - 130)) / 130, 0, 1)
        arms = np.maximum(arms, band * np.clip(-s, 0, 1) * (0.3 + 0.7 * tex))
    cover = np.clip(s * 3.0 + 0.35 + (tex - 0.5) * 0.9, 0, 1)
    foot = np.clip(1 - s * 2.5, 0, 1)[..., None] * (s > -0.2)[..., None]
    rgb = rgb * (1 - foot * 0.5) + np.array(p["mist"]) * foot * 0.5         # mist at the wall's foot
    rgb = np.where((arms > cover)[..., None], lerp(p["mist"], p["lit"], 0.25 * tex), rgb)
    alpha = np.maximum(cover, arms * 0.6)
    a = np.round(np.clip(alpha, 0, 1) * 4) / 4                # stepped alpha: 0, 25, 50, 75, 100 %
    img = Image.fromarray(np.dstack([np.clip(rgb, 0, 255), a * 255]).astype(np.uint8), "RGBA")
    return artkit.quantize_set([img], p["colours"])[0]


# =========================================================================== the shaft

def shaft(look):
    """The light: a soft pool on the sea and the haze of the shaft over it; b adds faint rays.
    Additive, premultiplied on black (480 x 540)."""
    p = LOOKS[look]
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float64) + 0.5
    cx, cy = p["pool_at"]
    rx, ry = p["pool_r"]
    d = np.hypot((xx - cx) / rx, (yy - cy) / ry)
    tex = spectral_noise((H, W), 3421, beta=2.4)
    pool = np.exp(-d ** 2 * 1.6) * (0.8 + 0.4 * tex) * p["pool_gain"]
    # the haze of the shaft: a soft column from the pool toward the light (up and left)
    ax, ay = -0.45, -1.0
    norm = np.hypot(ax, ay)
    along = ((xx - cx) * ax + (yy - cy) * ay) / norm
    across = ((xx - cx) * -ay + (yy - cy) * ax) / norm
    haze = np.exp(-(across / (rx * 0.75)) ** 2) * np.clip(along / 260, 0, 1) ** 0.5 * np.exp(-along / 420)
    haze = haze * (along > 0) * p["haze"]
    light = pool + haze
    if p["rays"]:
        ang = np.arctan2(yy - cy, xx - cx)
        rays = (np.cos(ang * 9 + 1.3) * 0.5 + 0.5) ** 6 * np.exp(-d * 0.6) * p["rays"]
        light = light + rays
    light = np.round(light * 24) / 24                          # posterized
    rgb = np.array(p["pool"]) * light[..., None]
    alpha = (light > 0) * 255
    return Image.fromarray(np.dstack([np.clip(rgb, 0, 255), alpha]).astype(np.uint8), "RGBA")


def glints(look, t):
    """b: slow sun glints on the swell in the pool: points fading in and out over ~1.5 s each (an
    additive layer, at most a pixel's change a frame)."""
    p = LOOKS[look]
    img = Image.new("RGBA", (W, H), (0, 0, 0, 255))
    if not p["glints"]:
        return None
    rng = np.random.default_rng(3431)
    px = img.load()
    cx, cy = p["pool_at"]
    rx, ry = p["pool_r"]
    for _ in range(p["glints"]):
        a, rr = rng.uniform(0, 2 * np.pi), np.sqrt(rng.uniform(0, 1))
        x, y = int(cx + np.cos(a) * rx * rr * 0.9), int(cy + np.sin(a) * ry * rr * 0.9)
        k = max(0.0, np.sin(2 * np.pi * (t / rng.uniform(1.2, 1.8) + rng.uniform(0, 1))))
        v = int(200 * k * (1 - rr * 0.6))
        if v > 8:
            px[x, y] = (v, int(v * 0.9), int(v * 0.7), 255)
    return img


# =========================================================================== review

def calm_sea(y0):
    """A stand-in for the eye's calm dark sea: Level 11's swell tile, darkened toward the storm's
    slate (step 5 makes the eye's own sea)."""
    src = np.array(Image.open(ROOT / "assets" / "backdrop" / "level-11" / "swell.png").convert("RGB")).astype(np.float64)
    a = np.concatenate([src, src, src], axis=0)
    y0 = int(y0) % src.shape[0]
    a = a[src.shape[0] + src.shape[0] - y0 - H:src.shape[0] + src.shape[0] - y0, :W]
    lum = a.mean(axis=-1, keepdims=True)
    a = (lum + (a - lum) * 0.6) * 0.55 + np.array([10, 14, 20])
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGB").convert("RGBA")


def compose(look, ring, light, t):
    img = calm_sea(t * SEA_SPEED)
    img = artkit.add_light(img, light)
    g = glints(look, t)
    if g is not None:
        img = artkit.add_light(img, g)
    turned = ring.rotate(-TURN * t, resample=Image.BICUBIC)   # PIL turns counter-clockwise for +deg
    img.alpha_composite(turned, (int(W / 2 - RING / 2), int(H / 2 - RING / 2)))
    return img


def ring_preview(ring):
    img = Image.new("RGBA", ring.size, (14, 18, 26, 255))
    img.alpha_composite(ring)
    return img.resize((380, 380), Image.LANCZOS)


def light_preview(light):
    return artkit.add_light(Image.new("RGBA", (W, H), (0, 0, 0, 255)), light)


def review(look):
    artkit.REVIEW_ROUND = REVIEW_ROUND
    ring = wall_ring(look)
    light = shaft(look)
    title = {"a": "BILLOWING WALL, COOL SILVER SHAFT", "b": "BANDED SPIRAL WALL, WARM GOLD SHAFT"}[look]
    still = compose(look, ring, light, 0.0)
    later = compose(look, ring, light, 10.0)
    sheet = raster.sheet(1680, 620, f"THE EYE {look.upper()} - {title}",
                         f"LEVEL 12 SECTION 5 - CONCEPT ROUND 34 - {REVIEW_ROUND.upper()}")
    x = 16
    for label, img in (("THE EYE, PLAY FIELD 480X540 (1X), T=0", still),
                       (f"10 S LATER: THE WALL TURNED {TURN * 10:.0f} DEG CLOCKWISE, THE SEA SCROLLED", later),
                       ("THE WALL RING (760 PX AT 1/2, RADIALLY LIT)", ring_preview(ring)),
                       ("THE SHAFT (ADDITIVE, AT 1/2)", light_preview(light).resize((240, 270), Image.LANCZOS))):
        raster.draw_text(sheet, x, 40, label, raster.LABEL)
        sheet.alpha_composite(img, (x, 56))
        x += img.width + 16
    gif = [compose(look, ring, light, f / 10).convert("RGB") for f in range(60)]
    CONCEPT.mkdir(parents=True, exist_ok=True)
    png = CONCEPT / f"eye-{REVIEW_ROUND}-{look}.png"
    sheet.convert("RGB").save(png, optimize=True)
    write_gif(gif, CONCEPT / f"eye-{REVIEW_ROUND}-{look}.gif", fps=10, colors=128)
    print(f"review: {png.relative_to(ROOT)} (+ .gif)")


if __name__ == "__main__":
    for look in [a for a in sys.argv[1:] if a in LOOKS] or list(LOOKS):
        review(look)
