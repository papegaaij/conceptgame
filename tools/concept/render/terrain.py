"""Procedural pre-rendered-looking backgrounds (deterministic, optionally tileable)."""
import numpy as np
from PIL import Image

from .raster import fbm, ramp, to_rgba_image, value_noise


def _shade(height, strength=6.0):
    """Hill shading from a height map with light from the top-left."""
    gy, gx = np.gradient(height)
    shade = 1.0 + (gx + gy) * -strength   # light from -x (left) and -y (top)
    return np.clip(shade, 0.55, 1.45)


def recede(img, amount=0.3, darken=0.8, tint=(40, 60, 90)):
    """Push a background back: darken, desaturate and tint it (sprites must pop, see
    art-direction readability rules)."""
    a = np.array(img).astype(np.float64)
    rgb = a[..., :3]
    grey = rgb.mean(axis=-1, keepdims=True)
    rgb = rgb * (1 - amount) + grey * amount
    rgb = rgb * darken + np.array(tint) * (1 - darken) * 0.5
    a[..., :3] = rgb
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


def earth_coast(w, h, seed=3, period=True, land_bias=0.52):
    """Daylight Earth surface seen from low altitude: sea, beaches, grass, forest, rock."""
    hgt = fbm(w, h, 64, seed, octaves=6, period=period)
    stops = [
        (0.00, (12, 34, 70)), (0.40, (22, 70, 112)), (land_bias - 0.02, (40, 118, 140)),
        (land_bias, (196, 186, 140)), (land_bias + 0.03, (92, 128, 60)),
        (0.70, (52, 92, 44)), (0.84, (98, 92, 76)), (1.00, (200, 200, 200)),
    ]
    rgb = ramp(stops, hgt)
    land = hgt > land_bias
    shade = _shade(hgt * 40, 1.0)
    rgb[land] *= shade[land, None]
    detail = fbm(w, h, 8, seed + 7, octaves=3, period=period)
    rgb *= (0.9 + 0.2 * detail)[..., None]
    # shallow-water foam line
    foam = np.abs(hgt - land_bias) < 0.006
    rgb[foam] = rgb[foam] * 0.4 + np.array([220, 230, 235]) * 0.6
    return to_rgba_image(rgb)


def earth_from_orbit(w, h, seed=11, period=True):
    """Earth far below (deep layer): continents, cloud swirls, atmospheric haze."""
    hgt = fbm(w, h, 128, seed, octaves=6, period=period)
    stops = [
        (0.00, (8, 26, 64)), (0.50, (18, 58, 110)), (0.555, (30, 90, 130)),
        (0.56, (150, 140, 100)), (0.62, (80, 110, 60)), (0.78, (120, 100, 70)),
        (1.0, (210, 205, 200)),
    ]
    rgb = ramp(stops, hgt)
    clouds = fbm(w, h, 64, seed + 50, octaves=6, period=period)
    swirl = value_noise(w, h, 32, seed + 90, period)
    c = np.clip((clouds * 0.8 + swirl * 0.3 - 0.6) * 2.6, 0, 0.85)
    rgb = rgb * (1 - c[..., None]) + np.array([225, 232, 244]) * c[..., None]
    # cloud shadows offset down-right
    cs = np.roll(np.roll(c, 3, axis=0), 3, axis=1)
    rgb *= (1 - 0.4 * cs * (1 - c))[..., None]
    haze = np.array([50, 90, 150])
    rgb = rgb * 0.55 + haze * 0.45            # depth: desaturate towards the atmosphere colour
    rgb *= 0.78
    return to_rgba_image(rgb)


def city_ground(w, h, seed=5, period=True, block=40, street=8):
    """Night-time megacity ground layer: dark lots, street grid with sodium lights."""
    rng = np.random.default_rng(seed)
    base = fbm(w, h, 32, seed, octaves=4, period=period)
    rgb = np.zeros((h, w, 3))
    rgb[:] = np.array([22, 24, 34])
    rgb *= (0.8 + 0.4 * base)[..., None]
    yy, xx = np.mgrid[0:h, 0:w]
    streets = ((xx % block) < street) | ((yy % block) < street)
    rgb[streets] = np.array([34, 34, 42])
    centre = (((xx % block) == street // 2) | ((yy % block) == street // 2)) & streets
    dash = (((xx + yy) // 3) % 2 == 0)
    rgb[centre & dash] = np.array([90, 84, 60])
    img = to_rgba_image(rgb)
    return img, streets
