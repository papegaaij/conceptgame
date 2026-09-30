"""Tiny numpy signed-distance-field ray-marcher for pre-rendered CGI style sprites.

Models are built from SDF primitives in world units. The camera is orthographic and looks
straight down the -Z axis (top-down view); +Y is "up" on the screen (the ship's nose), +X is
right. Light comes from the top-left of the screen, as defined in design/art-direction.

A model is a function ``scene(p) -> (distance, material_index)`` where ``p`` has shape (N, 3).
Build it with the helpers below, e.g.::

    def scene(p):
        q = mirror_x(p)
        return union(
            (sd_ellipsoid(p, (0, 0, 0), (0.2, 1.0, 0.15)), 0),
            (sd_plate(q, WING, 0.0, 0.03), 1),
        )

``render(scene, materials, size, extent)`` returns a float RGBA image (premultiplied alpha is
*not* used; alpha is coverage 0/1 at render resolution). Use ``downsample`` + ``quantize`` in
render.sprite to turn the high-res render into a native-size palettised sprite.
"""
from dataclasses import dataclass, field
from typing import Callable, Optional

import numpy as np

# --------------------------------------------------------------------------- vector helpers


def vec(*v):
    return np.array(v, dtype=np.float64)


def norm(v):
    return v / np.maximum(np.linalg.norm(v, axis=-1, keepdims=True), 1e-9)


def length(v):
    return np.sqrt(np.sum(v * v, axis=-1))


def clamp(x, a, b):
    return np.minimum(np.maximum(x, a), b)


def smoothstep(a, b, x):
    t = clamp((x - a) / (b - a), 0.0, 1.0)
    return t * t * (3 - 2 * t)


# --------------------------------------------------------------------------- transforms


def mirror_x(p):
    """Mirror the model in the YZ plane (models are symmetric left/right)."""
    q = p.copy()
    q[:, 0] = np.abs(q[:, 0])
    return q


def rotate_y(p, angle):
    """Rotate points about the ship's long axis (Y): used for banking frames."""
    c, s = np.cos(angle), np.sin(angle)
    q = p.copy()
    q[:, 0] = c * p[:, 0] + s * p[:, 2]
    q[:, 2] = -s * p[:, 0] + c * p[:, 2]
    return q


def rotate_z(p, angle):
    """Rotate points about the view axis (heading)."""
    c, s = np.cos(angle), np.sin(angle)
    q = p.copy()
    q[:, 0] = c * p[:, 0] + s * p[:, 1]
    q[:, 1] = -s * p[:, 0] + c * p[:, 1]
    return q


def rotate_x(p, angle):
    """Rotate points about the X axis (pitch)."""
    c, s = np.cos(angle), np.sin(angle)
    q = p.copy()
    q[:, 1] = c * p[:, 1] + s * p[:, 2]
    q[:, 2] = -s * p[:, 1] + c * p[:, 2]
    return q


# --------------------------------------------------------------------------- primitives


def sd_sphere(p, c, r):
    return length(p - vec(*c)) - r


def sd_ellipsoid(p, c, r):
    """Approximate ellipsoid distance (good enough for marching with a safety factor)."""
    r = vec(*r)
    q = (p - vec(*c)) / r
    k0 = length(q)
    k1 = length(q / r)
    return k0 * (k0 - 1.0) / np.maximum(k1, 1e-9)


def sd_box(p, c, b, r=0.0):
    """Rounded box centred at c with half extents b and corner radius r."""
    q = np.abs(p - vec(*c)) - (vec(*b) - r)
    return length(np.maximum(q, 0.0)) + np.minimum(np.max(q, axis=-1), 0.0) - r


def sd_capsule(p, a, b, ra, rb=None):
    """Capsule / round cone from a to b with radius ra at a and rb at b."""
    rb = ra if rb is None else rb
    a, b = vec(*a), vec(*b)
    pa, ba = p - a, b - a
    h = clamp(np.sum(pa * ba, axis=-1) / np.dot(ba, ba), 0.0, 1.0)
    r = ra + (rb - ra) * h
    return length(pa - ba * h[:, None]) - r


def sd_cylinder_z(p, c, r, h):
    """Upright cylinder (axis Z) of radius r, half height h."""
    q = p - vec(*c)
    d = np.stack([np.sqrt(q[:, 0] ** 2 + q[:, 1] ** 2) - r, np.abs(q[:, 2]) - h], axis=-1)
    return np.minimum(np.max(d, axis=-1), 0.0) + length(np.maximum(d, 0.0))


def sd_cylinder_y(p, c, r, h):
    """Cylinder along Y of radius r, half length h."""
    q = p - vec(*c)
    d = np.stack([np.sqrt(q[:, 0] ** 2 + q[:, 2] ** 2) - r, np.abs(q[:, 1]) - h], axis=-1)
    return np.minimum(np.max(d, axis=-1), 0.0) + length(np.maximum(d, 0.0))


def sd_cylinder_x(p, c, r, h):
    """Cylinder along X of radius r, half length h."""
    q = p - vec(*c)
    d = np.stack([np.sqrt(q[:, 1] ** 2 + q[:, 2] ** 2) - r, np.abs(q[:, 0]) - h], axis=-1)
    return np.minimum(np.max(d, axis=-1), 0.0) + length(np.maximum(d, 0.0))


def sd_polygon2(px, py, poly):
    """Signed 2D distance to a closed polygon given as [(x, y), ...] (negative inside)."""
    v = np.asarray(poly, dtype=np.float64)
    d = (px - v[0, 0]) ** 2 + (py - v[0, 1]) ** 2
    s = np.ones_like(px)
    n = len(v)
    for i in range(n):
        j = i - 1
        ex, ey = v[j, 0] - v[i, 0], v[j, 1] - v[i, 1]
        wx, wy = px - v[i, 0], py - v[i, 1]
        t = clamp((wx * ex + wy * ey) / (ex * ex + ey * ey), 0.0, 1.0)
        bx, by = wx - ex * t, wy - ey * t
        d = np.minimum(d, bx * bx + by * by)
        c1 = py >= v[i, 1]
        c2 = py < v[j, 1]
        c3 = ex * wy > ey * wx
        flip = (c1 & c2 & c3) | (~c1 & ~c2 & ~c3)
        s = np.where(flip, -s, s)
    return s * np.sqrt(d)


def sd_plate(p, poly, z, half_thickness, round_=0.0, taper=None):
    """A flat plate: 2D polygon in the XY plane extruded along Z around height z.

    ``taper(x, y) -> factor`` optionally thins the plate (e.g. towards wing tips).
    Rounded edges via ``round_``.
    """
    d2 = sd_polygon2(p[:, 0], p[:, 1], poly) + round_
    h = half_thickness if taper is None else half_thickness * taper(p[:, 0], p[:, 1])
    dz = np.abs(p[:, 2] - z) - (h - round_ * 0.5)
    w = np.stack([d2, dz], axis=-1)
    return np.minimum(np.max(w, axis=-1), 0.0) + length(np.maximum(w, 0.0)) - round_


def sd_fin(p, poly_yz, x, half_thickness, round_=0.0):
    """A vertical fin: 2D polygon in the YZ plane extruded along X around x."""
    d2 = sd_polygon2(p[:, 1], p[:, 2], poly_yz) + round_
    dx = np.abs(p[:, 0] - x) - half_thickness
    w = np.stack([d2, dx], axis=-1)
    return np.minimum(np.max(w, axis=-1), 0.0) + length(np.maximum(w, 0.0)) - round_


# --------------------------------------------------------------------------- combinators


def smin(a, b, k):
    """Polynomial smooth minimum: blends two surfaces with radius k."""
    if k <= 0:
        return np.minimum(a, b)
    h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0)
    return b + (a - b) * h - k * h * (1.0 - h)


def union(*items, k=0.0):
    """Union of (distance, material) pairs. Distance is smooth-blended with radius k,
    the material is taken from the nearest surface."""
    d, m = items[0]
    d = np.asarray(d, dtype=np.float64)
    m = np.full(d.shape, m, dtype=np.int32) if np.isscalar(m) else m
    for d2, m2 in items[1:]:
        m2 = np.full(d.shape, m2, dtype=np.int32) if np.isscalar(m2) else m2
        m = np.where(d2 < d, m2, m)
        d = smin(d, d2, k)
    return d, m


def subtract(base, cut):
    """Carve distance ``cut`` out of (distance, material) ``base``."""
    d, m = base
    return np.maximum(d, -cut), m


# --------------------------------------------------------------------------- materials


@dataclass
class Material:
    """Surface description.

    albedo      base colour (0..1 rgb)
    metal       0 = dielectric, 1 = chrome-like; mixes in the environment reflection
    shininess   Blinn-Phong exponent
    spec        specular strength
    emission    self-illumination (rgb), e.g. engine glow, cockpit lights, Vrell glow
    pattern     optional ``f(p, n) -> multiplier array`` for panel lines, grids, spots
    """
    albedo: tuple
    metal: float = 0.0
    shininess: float = 40.0
    spec: float = 0.5
    emission: tuple = (0.0, 0.0, 0.0)
    pattern: Optional[Callable] = None
    emission_pattern: Optional[Callable] = None


def panel_lines(scale=7.0, width=0.06, depth=0.72, offset=0.0):
    """Recessed panel-line pattern in the XY plane (typical pre-rendered hull detail)."""
    def f(p, n):
        u = np.abs(((p[:, 1] + offset) * scale) % 1.0 - 0.5)
        v = np.abs((np.abs(p[:, 0]) * scale * 1.3 + 0.25) % 1.0 - 0.5)
        line = np.minimum(u, v)
        return np.where(line > 0.5 - width, depth, 1.0)
    return f


# --------------------------------------------------------------------------- lighting setup

KEY_LIGHT = norm(vec(-0.55, 0.6, 0.75))     # from the top-left of the screen, above
KEY_POS = vec(-2.2, 2.6, 3.2)               # point key light (in model-extent units)
FILL_LIGHT = norm(vec(0.7, -0.5, 0.35))     # weak cool fill from bottom-right
KEY_COLOR = vec(1.0, 0.97, 0.9)
FILL_COLOR = vec(0.35, 0.45, 0.65)
VIEW = vec(0.0, 0.0, 1.0)


def env_color(r):
    """Classic 90s chrome environment: bright sky above, dark horizon band, warm ground."""
    z = r[:, 2]
    y = r[:, 1]
    sky = np.array([0.55, 0.7, 0.95])
    zenith = np.array([0.95, 0.97, 1.0])
    horizon = np.array([0.12, 0.12, 0.16])
    ground = np.array([0.45, 0.33, 0.25])
    t_up = smoothstep(0.0, 0.9, z + 0.25 * y)[:, None]
    up = sky * (1 - t_up) + zenith * t_up
    t_dn = smoothstep(-0.05, -0.6, z + 0.25 * y)[:, None]
    dn = horizon * (1 - t_dn) + ground * t_dn
    band = smoothstep(-0.12, 0.08, z + 0.25 * y)[:, None]
    return dn * (1 - band) + up * band


# --------------------------------------------------------------------------- renderer


def _normals(scene, p, eps):
    k = np.array([[1, -1, -1], [-1, -1, 1], [-1, 1, -1], [1, 1, 1]], dtype=np.float64)
    n = np.zeros_like(p)
    for kk in k:
        d, _ = scene(p + kk * eps)
        n += kk * d[:, None]
    return norm(n)


def _soft_shadow(scene, p, light, tmin, tmax, k=10.0, steps=28):
    res = np.ones(len(p))
    t = np.full(len(p), tmin)
    for _ in range(steps):
        d, _ = scene(p + light * t[:, None])
        res = np.minimum(res, k * np.maximum(d, 0.0) / t)
        t = t + clamp(d, 0.01, 0.2)
    return clamp(res, 0.0, 1.0)


def _ambient_occlusion(scene, p, n, scale):
    occ = np.zeros(len(p))
    w = 1.0
    for i in range(1, 6):
        h = scale * i
        d, _ = scene(p + n * h)
        occ += (h - d) * w
        w *= 0.6
    return clamp(1.0 - 2.2 * occ / scale / 5.0, 0.0, 1.0)


def render(scene, materials, size, extent, center=(0.0, 0.0), z_top=3.0, steps=110,
           shadows=True, key_pos=None, exposure=1.0, ambient=0.22, fill=0.35):
    """Ray-march ``scene`` into a float RGBA image.

    size     (width, height) in pixels
    extent   world units covered by the image width (height follows the aspect ratio)
    key_pos  position of the point key light, relative to ``center`` and scaled by
             ``extent / 2.3`` (so the light sits in the same place for any model size).
             A point light instead of a sun gives the characteristic top-left to
             bottom-right gradient across flat hull plates of 90s pre-rendered sprites.
    """
    w, h = size
    light_pos = (KEY_POS if key_pos is None else vec(*key_pos)) * (extent / 2.3)
    light_pos = light_pos + vec(center[0], center[1], 0.0)
    px = extent / w
    xs = (np.arange(w) + 0.5 - w / 2) * px + center[0]
    ys = -(np.arange(h) + 0.5 - h / 2) * px + center[1]
    gx, gy = np.meshgrid(xs, ys)
    n_rays = w * h
    origin = np.stack([gx.ravel(), gy.ravel(), np.full(n_rays, z_top)], axis=-1)
    direction = vec(0.0, 0.0, -1.0)

    t = np.zeros(n_rays)
    hit = np.zeros(n_rays, dtype=bool)
    active = np.arange(n_rays)
    eps = px * 0.25
    t_max = 2 * z_top
    for _ in range(steps):
        if active.size == 0:
            break
        p = origin[active] + direction * t[active, None]
        d, _ = scene(p)
        t[active] += d * 0.8
        done_hit = d < eps
        done_miss = t[active] > t_max
        hit[active[done_hit]] = True
        active = active[~(done_hit | done_miss)]

    rgba = np.zeros((n_rays, 4))
    idx = np.nonzero(hit)[0]
    if idx.size == 0:
        return rgba.reshape(h, w, 4)
    p = origin[idx] + direction * t[idx, None]
    _, mat = scene(p)
    n = _normals(scene, p, px * 0.5)

    albedo = np.zeros((len(idx), 3))
    metal = np.zeros(len(idx))
    shin = np.zeros(len(idx))
    spec_k = np.zeros(len(idx))
    emis = np.zeros((len(idx), 3))
    for i, m in enumerate(materials):
        sel = mat == i
        if not np.any(sel):
            continue
        albedo[sel] = m.albedo
        metal[sel] = m.metal
        shin[sel] = m.shininess
        spec_k[sel] = m.spec
        e = np.broadcast_to(np.asarray(m.emission, dtype=np.float64), (sel.sum(), 3)).copy()
        if m.emission_pattern is not None:
            e = e * m.emission_pattern(p[sel], n[sel])[:, None]
        emis[sel] = e
        if m.pattern is not None:
            albedo[sel] *= m.pattern(p[sel], n[sel])[:, None]

    to_light = light_pos - p
    dist = np.linalg.norm(to_light, axis=-1)
    key = to_light / dist[:, None]
    atten = (np.linalg.norm(light_pos - vec(center[0], center[1], 0.0)) / dist) ** 2
    ndl = clamp(np.sum(n * key, axis=-1), 0.0, 1.0)
    sh = _soft_shadow(scene, p + n * px, key, px * 2, 3.0 * extent / 2.3) if shadows else 1.0
    ao = _ambient_occlusion(scene, p, n, px * 3)
    fillv = clamp(np.sum(n * FILL_LIGHT, axis=-1), 0.0, 1.0)
    sky = 0.5 + 0.5 * n[:, 2]
    edge = 0.45 + 0.55 * clamp(n[:, 2], 0, 1) ** 0.6   # darken silhouettes

    diffuse = (KEY_COLOR * (ndl * sh * atten)[:, None] * 1.25
               + FILL_COLOR * fillv[:, None] * fill
               + vec(0.55, 0.6, 0.7) * (sky * ambient)[:, None] * ao[:, None]) * edge[:, None]
    hvec = norm(key + VIEW)
    spec = (clamp(np.sum(n * hvec, axis=-1), 0.0, 1.0) ** shin) * spec_k * sh * atten
    refl = 2 * np.sum(n * VIEW, axis=-1)[:, None] * n - VIEW
    fres = 0.25 + 0.75 * (1 - clamp(np.sum(n * VIEW, axis=-1), 0, 1)) ** 3
    env = env_color(refl)

    base = albedo * diffuse
    chrome = env * albedo * 1.2 * (0.6 + 0.4 * ao)[:, None] * edge[:, None]
    mix = (metal * (0.55 + 0.45 * fres))[:, None]
    color = base * (1 - mix) + chrome * mix + spec[:, None] * KEY_COLOR + emis
    color = color * exposure * 0.72
    knee = 0.78  # linear up to the knee, soft roll-off above it (keeps the shading gradient)
    color = np.where(color < knee, color,
                     knee + (1 - knee) * (1 - np.exp(-(color - knee) / (1 - knee))))
    rgba[idx, :3] = clamp(color, 0.0, 1.0)
    rgba[idx, 3] = 1.0
    return rgba.reshape(h, w, 4)
