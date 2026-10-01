"""Models for concept round 07 (rotation and waterline fixes).

- ``halo_ring``: the six Halo Platform ring segments (round 06 ``halo_segment``) assembled
  into one 6-fold symmetric model, so the whole ring can be pre-rendered at a dense angle set
  over a single 60 degree symmetry step (art direction: "Smooth slow rotation").
- ``clipped``: cut any model at a water plane (``z = zcut``) to render the parts above and
  below the surface separately (art direction: "Water").
"""
from dataclasses import replace

import numpy as np

from .r06_models import halo_segment
from .sdf import union

SEC = 2 * np.pi / 6


def _sector_local(p, R, k):
    """Local coordinates of model points ``p`` in the frame of ring segment ``k`` (integer
    array): local +X points to the ring centre, +Y along the tangent, z unchanged."""
    th = k * SEC
    c, s = np.cos(th), np.sin(th)
    dx, dy = p[:, 0] - R * c, p[:, 1] - R * s
    q = p.copy()
    q[:, 0] = -(dx * c + dy * s)
    q[:, 1] = -dx * s + dy * c
    return q


def _rot_normals(n, k):
    th = k * SEC
    c, s = np.cos(th), np.sin(th)
    m = n.copy()
    m[:, 0] = -(n[:, 0] * c + n[:, 1] * s)
    m[:, 1] = -n[:, 0] * s + n[:, 1] * c
    return m


def _nearest_sectors(p):
    a = np.arctan2(p[:, 1], p[:, 0]) / SEC
    k0 = np.round(a)
    k1 = np.where(a >= k0, k0 + 1, k0 - 1)        # the neighbour on the same side
    return k0, k1


def halo_ring(R, glow=1.0):
    """Six ``halo_segment`` blocks on a ring of radius ``R`` (model units), segment centres at
    model angles k * 60 degrees. Each point evaluates its own and the neighbouring segment, so
    joints and the slanted shadow rays stay correct. Surface patterns are evaluated in each
    segment's local frame, which keeps the model exactly 6-fold symmetric."""
    seg, mats = halo_segment(glow)

    def scene(p):
        k0, k1 = _nearest_sectors(p)
        return union(seg(_sector_local(p, R, k0)), seg(_sector_local(p, R, k1)), k=0.0)

    def local_pattern(fn):
        if fn is None:
            return None

        def f(p, n):
            k0, _ = _nearest_sectors(p)
            return fn(_sector_local(p, R, k0), _rot_normals(n, k0))
        return f
    mats = [replace(m, pattern=local_pattern(m.pattern),
                    emission_pattern=local_pattern(m.emission_pattern)) for m in mats]
    return scene, mats


def clipped(make, zcut, keep="above"):
    """Wrap a model factory so only the part above (``keep="above"``) or below the plane
    ``z = zcut`` remains; the cut face takes the material of the nearest primitive."""
    def factory(**kw):
        scene, mats = make(**kw)

        def cut_scene(p):
            d, m = scene(p)
            cut = (zcut - p[:, 2]) if keep == "above" else (p[:, 2] - zcut)
            return np.maximum(d, cut), m
        return cut_scene, mats
    return factory
