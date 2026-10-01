"""Rig helpers for concept round 05: sprites pre-rendered at N headings, segment chains that
follow a path, local-to-screen transforms for articulated parts, and GIF encoding.

Heading convention (screen space, y down): heading 0 = moving right, pi/2 = moving down
(towards the player). Models from ``render.archetype_models`` face +Y (forward); the model
rotation that shows a forward-facing model at screen heading h is ``-h - pi/2`` around Z.
"""
import subprocess
import tempfile
from pathlib import Path

import numpy as np

from . import sdf, sprite

TAU = 2 * np.pi


def model_rotation(heading):
    return -heading - np.pi / 2


def local_to_screen(lx, ly, heading, scale):
    """Offset in screen px of a local model point (x right, y forward) for a part facing
    ``heading`` and drawn at ``scale`` px per model unit."""
    c, s = np.cos(heading), np.sin(heading)
    return scale * (ly * c - lx * s), scale * (ly * s + lx * c)


class AngleSprites:
    """Lazily pre-renders a model at ``count`` headings (16 for turning units, 32 for large or
    slow ones) and returns the nearest frame, like the game will.

    make(**kw) -> (scene, mats)   model factory; kw are animation parameters (phase, glow...)
    size       native canvas (w, h) or an int for square; the model origin is the centre
    extent     model units across the canvas width
    sym        rotational symmetry of the model (spinning parts need only 360/sym degrees)
    post       optional sprite post-process (e.g. the Ascendancy rim light)
    """

    def __init__(self, make, size, count=16, extent=2.3, factor=6, colors=24, sym=1,
                 post=None, crisp=90):
        self.make, self.count, self.extent, self.factor = make, count, extent, factor
        self.size = (size, size) if isinstance(size, int) else tuple(size)
        self.colors, self.sym, self.post, self.crisp = colors, sym, post, crisp
        self.cache = {}

    def index(self, heading):
        step = TAU / self.count
        k = int(round((heading % TAU) / step)) % self.count
        if self.sym > 1:
            k %= self.count // self.sym
        return k

    def frame(self, k, **kw):
        key = (k, tuple(sorted((a, round(float(b), 4)) for a, b in kw.items())))
        if key not in self.cache:
            scene0, mats = self.make(**kw)
            rot = model_rotation(k * TAU / self.count)
            scene = lambda p, s=scene0, r=rot: s(sdf.rotate_z(p, r))
            w, h = self.size
            hi = sdf.render(scene, mats, (w * self.factor, h * self.factor), self.extent)
            sp = sprite.make_sprite(hi, self.factor, self.colors, crisp=self.crisp)
            if self.post:
                sp = self.post(sp)
            self.cache[key] = sp
        return self.cache[key]

    def get(self, heading, **kw):
        return self.frame(self.index(heading), **kw)


def chain_positions(path, t, n, spacing, dt=0.004):
    """Positions and headings of ``n`` points spaced ``spacing`` px apart *along the path
    already travelled* by a head following ``path(t) -> (x, y)``. Point 0 is the head."""
    pts = [np.array(path(t), dtype=np.float64)]
    heads = []
    cur, tt, need, acc = pts[0], t, spacing, 0.0
    while len(pts) < n and tt > t - 60:
        tt -= dt
        nxt = np.array(path(tt), dtype=np.float64)
        seg = np.linalg.norm(nxt - cur)
        acc += seg
        if acc >= need:
            pts.append(nxt)
            acc = 0.0
        cur = nxt
    for i, pnt in enumerate(pts):
        ahead = pts[i - 1] if i > 0 else np.array(path(t + dt * 4))
        d = ahead - pnt
        heads.append(float(np.arctan2(d[1], d[0])))
    return pts, heads


def path_heading(path, t, dt=0.01):
    a, b = np.array(path(t - dt)), np.array(path(t + dt))
    d = b - a
    return float(np.arctan2(d[1], d[0]))


def write_gif(frames, path, fps=20, colors=128, max_mb=8.0):
    """Encode PIL frames to a GIF with one global palette (ffmpeg); retries with fewer colours
    until the file is at most ``max_mb``."""
    path = Path(path)
    with tempfile.TemporaryDirectory() as tmp:
        for i, f in enumerate(frames):
            f.convert("RGB").save(Path(tmp) / f"f{i:04d}.png")
        for cols in (colors, 96, 64, 48):
            cmd = ["ffmpeg", "-v", "error", "-y", "-framerate", str(fps),
                   "-i", str(Path(tmp) / "f%04d.png"),
                   "-vf", f"split[a][b];[a]palettegen=max_colors={cols}:stats_mode=full[p];"
                          "[b][p]paletteuse=dither=none:diff_mode=rectangle",
                   "-loop", "0", str(path)]
            subprocess.run(cmd, check=True)
            if path.stat().st_size <= max_mb * 1024 * 1024:
                break
    return path.stat().st_size


def chain_at(path, t, spacings, dt=0.003, horizon=30.0):
    """Like ``chain_positions`` but with an individual gap per link: ``spacings[i]`` is the
    distance along the travelled path between point i and point i+1. Returns (points,
    headings), headings pointing forward along the path."""
    targets = np.concatenate([[0.0], np.cumsum(spacings)])
    pts, heads = [], []
    prev = np.array(path(t), dtype=np.float64)
    acc, tt, j = 0.0, t, 0
    pts.append(prev)
    j = 1
    while j < len(targets) and tt > t - horizon:
        tt -= dt
        cur = np.array(path(tt), dtype=np.float64)
        acc += np.linalg.norm(cur - prev)
        prev = cur
        while j < len(targets) and acc >= targets[j]:
            pts.append(cur)
            j += 1
    for i, pnt in enumerate(pts):
        ahead = pts[i - 1] if i > 0 else np.array(path(t + dt * 5))
        d = ahead - pnt
        heads.append(float(np.arctan2(d[1], d[0])))
    return pts, heads


def catmull_rom(points, times):
    """Smooth path through ``points`` at ``times`` (Catmull-Rom, clamped at the ends)."""
    P = np.asarray(points, dtype=np.float64)
    T = np.asarray(times, dtype=np.float64)

    def path(t):
        t = float(np.clip(t, T[0], T[-1] - 1e-9)) if t < T[0] or t >= T[-1] else float(t)
        i = int(np.searchsorted(T, t, side="right") - 1)
        i = min(max(i, 0), len(T) - 2)
        u = (t - T[i]) / (T[i + 1] - T[i])
        p0, p1, p2, p3 = P[max(i - 1, 0)], P[i], P[i + 1], P[min(i + 2, len(P) - 1)]
        return 0.5 * ((2 * p1) + (-p0 + p2) * u + (2 * p0 - 5 * p1 + 4 * p2 - p3) * u * u
                      + (-p0 + 3 * p1 - 3 * p2 + p3) * u ** 3)
    return path


def model_space_materials(mats, rotation):
    """Copies of ``mats`` whose surface patterns are evaluated in model space instead of screen
    space, so seams, plates and veins turn with a rotated model."""
    from dataclasses import replace

    def wrap(fn):
        if fn is None:
            return None
        return lambda p, n, f=fn: f(sdf.rotate_z(p, rotation), sdf.rotate_z(n, rotation))
    return [replace(m, pattern=wrap(m.pattern), emission_pattern=wrap(m.emission_pattern))
            for m in mats]


class ModelSpaceAngleSprites(AngleSprites):
    """``AngleSprites`` whose material patterns rotate with the model (round 05 follow-ups);
    the base class keeps screen-space patterns so earlier renders stay reproducible."""

    def frame(self, k, **kw):
        key = (k, tuple(sorted((a, round(float(b), 4) if not isinstance(b, str) else b)
                               for a, b in kw.items())))
        if key not in self.cache:
            scene0, mats = self.make(**kw)
            rot = model_rotation(k * TAU / self.count)
            scene = lambda p, s=scene0, r=rot: s(sdf.rotate_z(p, r))
            w, h = self.size
            hi = sdf.render(scene, model_space_materials(mats, rot),
                            (w * self.factor, h * self.factor), self.extent)
            sp = sprite.make_sprite(hi, self.factor, self.colors, crisp=self.crisp)
            if self.post:
                sp = self.post(sp)
            self.cache[key] = sp
        return self.cache[key]
