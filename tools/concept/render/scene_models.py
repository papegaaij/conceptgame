"""Models and background pieces for concept round 06 (setting scenes).

- Vrell sub-layer units for Europa: Driftjelly (floating mine organism) and Spiral Nautilus
  (coiled-shell cephalopod), built facing +Y like the round 05 archetypes, coloured with the
  role colours (added to ``enemy_models.ROLE_SCHEMES`` here, additively).
- Aurelia cloud-city parts (Jupiter): an Art Deco deck platform, a gilded spire, a tethered
  balloon, all as SDF models rendered once and kit-bashed in 2D.
- 2D helpers: lumpy asteroid sprites pre-rendered at several rotations, and a recolourable
  palette shim for the station kit (render.station) so the same parts can be Coalition grey,
  Ascendancy black & gold or Aurelia cream & gold.
"""
import numpy as np
from PIL import Image

from . import enemy_models as em
from . import raster, sdf, sprite
from .enemy_models import V_BODY, V_BONE, V_DARK, V_EYE, V_GLOW, V_SAC, V_SEAM
from .sdf import (Material, rotate_z, sd_capsule, sd_cylinder_z, sd_ellipsoid, sd_plate,
                  sd_sphere, union)

em.ROLE_SCHEMES.update({
    "driftjelly": ("olive", "lime", "lime"),            # floating mine: area denial
    "spiral-nautilus": ("bone", "violet", "violet"),    # ranged: fires spirals when uncoiled
})


# --------------------------------------------------------------------------- Europa units

def driftjelly(pulse=0.0, glow=1.0):
    """Floating mine organism: a wide domed bell with a scalloped glowing rim and a central
    organ, eight thin wavy tentacles trailing behind (-Y) as it drifts. ``pulse`` -1..1
    contracts the bell."""
    k = 1.0 - 0.1 * pulse

    def scene(p):
        bell = sd_ellipsoid(p, (0, 0.25, 0.0), (0.72 * k, 0.72 * k, 0.4))
        bell = np.maximum(bell, -(p[:, 2] + 0.04))
        items = [(bell, V_BODY),
                 (sd_ellipsoid(p, (0, 0.25, 0.2), (0.36 * k, 0.36 * k, 0.24)), V_SAC),
                 (sd_sphere(p, (0, 0.25, 0.38), 0.1), V_EYE)]
        for i in range(10):                      # scalloped rim with glow nodes
            a = 2 * np.pi * i / 10
            items.append((sd_sphere(p, (0.7 * k * np.cos(a), 0.25 + 0.7 * k * np.sin(a), 0.0),
                                    0.09), V_SEAM))
        for i in range(8):                       # wavy tentacles: chains of short capsules
            x0 = (i - 3.5) * 0.11 * k
            pts = [(x0 + 0.06 * np.sin(j * 1.3 + i + pulse * 1.5) * (j + 1), 0.0 - 0.17 * j,
                    -0.06 - 0.02 * j) for j in range(7)]
            for a, b in zip(pts[:-1], pts[1:]):
                items.append((sd_capsule(p, a, b, 0.035, 0.03), V_DARK if i % 2 else V_BONE))
        return union(*items, k=0.04)
    return scene, em.vrell_scheme_mats("driftjelly", "a", glow)


def spiral_nautilus(uncoil=0.0, glow=1.0):
    """Coiled-shell cephalopod seen from above: a round shell with a logarithmic spiral of
    chamber lines and dark growth stripes, the aperture at the front (+Y) with a crown of
    tentacles and a glowing eye. ``uncoil`` 0..1 extends the tentacles (it fires when
    uncoiled). Radial enough to roll (spin) without a fixed front while coiled."""
    growth = np.log(2.4) / (2 * np.pi)

    def spiral_pat(p, n):
        x, y = p[:, 0], p[:, 1] + 0.05
        r = np.hypot(x, y) + 1e-6
        th = np.arctan2(y, x)
        u = (th / (2 * np.pi) - np.log(r) / (2 * np.pi * growth)) % 1.0
        line = u < 0.09
        stripe = (np.sin(th * 9 + np.log(r) * 6) > 0.55) & (r > 0.25)
        return np.where(line, 0.35, np.where(stripe, 0.7, 1.0))
    mats = em.vrell_scheme_mats("spiral-nautilus", "a", glow)
    from dataclasses import replace
    mats[V_BONE] = replace(mats[V_BONE], pattern=spiral_pat)

    def scene(p):
        shell = sd_ellipsoid(p, (0, -0.05, 0.0), (0.66, 0.62, 0.4))
        items = [(shell, V_BONE),
                 (sd_ellipsoid(p, (0, 0.5, 0.05), (0.42, 0.18, 0.2)), V_DARK)]
        reach = 0.22 + 0.5 * uncoil
        for j in range(9):
            a = np.pi / 2 + (j - 4) * 0.2
            sx, sy = 0.32 * np.cos(a), 0.45 + 0.1 * np.sin(a)
            items.append((sd_capsule(p, (sx, sy, 0.08), (sx + reach * np.cos(a) * 0.8,
                                                         sy + reach * np.sin(a), 0.04),
                                     0.05, 0.022), V_BODY))
        return union(*items,
                     (sd_sphere(p, (0.2, 0.52, 0.2), 0.07), V_EYE),
                     (sd_sphere(p, (-0.2, 0.52, 0.2), 0.07), V_EYE),
                     (sd_ellipsoid(p, (0, 0.47, 0.16), (0.12, 0.06, 0.06)), V_GLOW),
                     k=0.03)
    return scene, mats


# --------------------------------------------------------------------------- Aurelia (Jupiter)

CREAM, GOLD_M, BLACK, RED_L, WARM, GLASS_G, DECK = range(7)


def aurelia_mats():
    def deco(p, n):            # Art Deco radial ribs and concentric bands on the deck
        r = np.hypot(p[:, 0], p[:, 1])
        ang = np.arctan2(p[:, 1], p[:, 0])
        ribs = (np.abs(np.sin(ang * 12)) < 0.08)
        bands = ((r * 0.11) % 1 < 0.07)
        return np.where(ribs | bands, 0.72, 1.0)

    def windows(p, n):
        return np.where((np.abs(np.sin(p[:, 0] * 0.9)) > 0.6) & (np.abs(np.sin(p[:, 1] * 0.9)) > 0.6),
                        1.0, 0.0)
    return [
        Material((0.93, 0.88, 0.76), metal=0.25, shininess=70, spec=0.6, pattern=deco),
        Material((1.0, 0.66, 0.0), metal=0.95, shininess=130, spec=1.3),
        Material((0.08, 0.05, 0.08), metal=0.8, shininess=140, spec=1.2),
        Material((0.1, 0.03, 0.03), emission=(1.7, 0.18, 0.12)),
        Material((0.2, 0.15, 0.1), emission=(1.5, 1.1, 0.55), emission_pattern=windows),
        Material((1.0, 0.78, 0.35), metal=0.85, shininess=160, spec=1.6,
                 emission=(0.25, 0.16, 0.04)),
        Material((0.62, 0.55, 0.47), metal=0.3, shininess=50, spec=0.5, pattern=deco),
    ]


def _octagon(r):
    return [(r * np.cos(a), r * np.sin(a)) for a in np.linspace(np.pi / 8, 2 * np.pi + np.pi / 8,
                                                                8, endpoint=False)]


def aurelia_deck(radius=78):
    """Octagonal deck platform with a gilded rim, a central gold-glass dome, four small domes,
    red rim lights and lit window bands. Units = native px."""
    def scene(p):
        items = [
            (sd_plate(p, _octagon(radius), 0.0, 5.0, 2.0), DECK),
            (sd_plate(p, _octagon(radius + 5), -4.0, 2.5, 1.0), GOLD_M),
            (sd_plate(p, _octagon(radius * 0.72), 5.0, 1.5, 1.0), CREAM),
            (np.maximum(sd_sphere(p, (0, 0, 0), radius * 0.36), -p[:, 2]), GLASS_G),
            (sd_cylinder_z(p, (0, 0, 2), radius * 0.4, 4.0), GOLD_M),
        ]
        for a in np.linspace(np.pi / 4, 2 * np.pi + np.pi / 4, 4, endpoint=False):
            cx, cy = 0.62 * radius * np.cos(a), 0.62 * radius * np.sin(a)
            items.append((np.maximum(sd_sphere(p, (cx, cy, 4), 11.0), -(p[:, 2] - 4)), CREAM))
            items.append((sd_cylinder_z(p, (cx, cy, 4), 12.5, 2.0), WARM))
            items.append((sd_sphere(p, (cx, cy, 15.5), 2.2), GOLD_M))
        for a in np.linspace(0, 2 * np.pi, 16, endpoint=False):
            items.append((sd_sphere(p, ((radius + 3) * np.cos(a), (radius + 3) * np.sin(a), 1.0),
                                    2.0), RED_L))
        return union(*items)
    return scene, aurelia_mats(), (2 * radius + 20, 2 * radius + 20)


def aurelia_spire(radius=16):
    """A gilded Art Deco spire seen from above: stepped tiers rising to a needle."""
    def scene(p):
        items = []
        for i, (r, z) in enumerate(((radius, 8), (radius * 0.75, 20), (radius * 0.5, 32),
                                    (radius * 0.3, 44))):
            items.append((sd_plate(p, _octagon(r), z, 6.0, 1.0), CREAM if i % 2 == 0 else GOLD_M))
        items.append((sd_capsule(p, (0, 0, 44), (0, 0, 70), 2.5, 0.6), GOLD_M))
        items.append((sd_sphere(p, (0, 0, 71), 1.6), RED_L))
        return union(*items)
    return scene, aurelia_mats(), (2 * radius + 10, 2 * radius + 10)


def aurelia_balloon(rx=26, ry=40):
    """Tethered lift balloon: cream envelope with gold bands and a gondola ring."""
    def bands(p, n):
        return np.where((np.abs(p[:, 0]) % 9 < 1.2), 0.62, 1.0)
    mats = aurelia_mats()
    mats[CREAM] = Material((0.95, 0.9, 0.78), metal=0.1, shininess=40, spec=0.45, pattern=bands)

    def scene(p):
        return union(
            (sd_ellipsoid(p, (0, 0, 0), (rx, ry, rx * 0.8)), CREAM),
            (sd_capsule(p, (0, -ry * 0.9, 0), (0, ry * 0.9, 0), 2.0), GOLD_M),
            (sd_sphere(p, (0, ry * 0.95, 0), 3.0), RED_L),
        )
    return scene, mats, (2 * rx + 8, 2 * ry + 8)


def render_px_model(model, factor=4, colors=40, crisp=60):
    """Render a (scene, mats, (w, h)) model whose units are native px."""
    scene, mats, (w, h) = model
    w, h = int(np.ceil(w)), int(np.ceil(h))
    hi = sdf.render(scene, mats, (w * factor, h * factor), float(w), z_top=float(max(w, h)) * 1.5,
                    steps=150)
    return sprite.make_sprite(hi, factor, colors, crisp=crisp)


# --------------------------------------------------------------------------- 2D pieces

class PaletteShim:
    """Duck-typed stand-in for render.palette.Palette for ``station.render_part``: overrides
    some (ramp, index) entries, everything else falls through to ``base``."""

    def __init__(self, base, overrides):
        self.base, self.over = base, overrides

    def f(self, name, i):
        if (name, i) in self.over:
            return tuple(c / 255 for c in self.over[(name, i)])
        return self.base.f(name, i)

    def __getitem__(self, name):
        return self.base[name]


def rock_sprites(r, seed, stops, rotations=16, haze=None, crater_count=8, elong=1.0,
                 factor=3):
    """Lumpy asteroid: a warped, cratered height field shaded as a sphere lit from the
    top-left, pre-rendered at ``rotations`` angles (the shape turns, the light stays).
    ``stops`` is a colour ramp, ``haze`` an optional (rgb, amount) depth cue."""
    rng = np.random.default_rng(seed)
    n = int(2 * r * factor) + 4
    yy, xx = np.mgrid[0:n, 0:n]
    c = n / 2
    warp = raster.fbm(n, n, max(4, n // 3), seed, octaves=3, period=False)
    detail = raster.fbm(n, n, max(3, n // 8), seed + 7, octaves=3, period=False)
    craters = [(rng.uniform(-0.6, 0.6), rng.uniform(-0.6, 0.6), rng.uniform(0.14, 0.34))
               for _ in range(crater_count)]
    out = []
    for k in range(rotations):
        ang = 2 * np.pi * k / rotations
        ca, sa = np.cos(ang), np.sin(ang)
        ux = ((xx - c) * ca + (yy - c) * sa) / (r * factor)       # model space (rotates)
        uy = (-(xx - c) * sa + (yy - c) * ca) / (r * factor) * elong
        # sample the warp/detail fields in model space so features turn with the rock
        sx = np.clip((ux * 0.5 + 0.5) * (n - 1), 0, n - 1).astype(int)
        sy = np.clip((uy * 0.5 + 0.5) * (n - 1), 0, n - 1).astype(int)
        wv, dv = warp[sy, sx], detail[sy, sx]
        dist = np.hypot(ux, uy) + (wv - 0.5) * 0.5
        inside = dist < 0.92
        nz = np.sqrt(np.clip(1 - (dist / 0.92) ** 2, 0, 1))
        hgt = nz * 0.9 + dv * 0.18
        for cx, cy, cr in craters:
            d = np.hypot(ux - cx, uy - cy) / cr
            hgt += np.where(d < 1, -(1 - d * d) * 0.25, 0) + np.exp(-((d - 1) / 0.2) ** 2) * 0.06
        gy, gx = np.gradient(hgt * 6)
        light = np.clip(0.55 - (gx + gy) * 1.4 + 0.25 * nz, 0.06, 1.25)
        light *= np.clip(0.55 + 0.6 * (-(xx - c) - (yy - c)) / (2 * r * factor) + 0.45, 0.25, 1.2)
        col = raster.ramp(stops, np.clip(dv * 0.9 + 0.1, 0, 1)) * light[..., None]
        if haze is not None:
            hc, amt = haze
            col = col * (1 - amt) + np.array(hc) * amt
        img = raster.to_rgba_image(col, inside * 255.0)
        img = img.resize((n // factor, n // factor), Image.BOX)
        a = np.array(img)
        a[..., 3] = np.where(a[..., 3] > 110, 255, 0)
        out.append(sprite.quantize(Image.fromarray(a, "RGBA"), 20))
    return out
