#!/usr/bin/env python3
"""Production art: the Skimmer, Level 13's flying-fish skiff that weaves through the leads between the
floes (design/enemies/naval/skimmer; M5 part G batch, concept round 35, step G2). Straight to
production from its chosen concept (decision G9 = a, pending the user's review): the round-06 model
(tools/concept/render/r06_models.skimmer, imported unchanged; the concept sheet is
tools/concept/enemies_r06.py, skimmer-r06-a), cut at the water plane by render/r07_models.clipped.

Water (E4 = c, the two-layer convention of tools/art/driftjelly.py and reef_spitter.py): the hull's
under-water part is drawn by the game's generic `sub` pass from a plain frame; the part above the
waterline with its broken foam collar is pre-rendered, and so is the wake (the foam streams of
tools/art/water_fx.py, here one per heading, since a Skimmer turns all the time on its weave).
The Skimmer skims: its fan fins and tail ride just above the surface (z = 0 cuts only the hull's
keel), so the above-water layer is nearly the whole body and the sub layer only shows the keel's
shadow-dark rim through the water.

Outputs (assets/sprites/; written only with --assets, in the content step's time (G4/G5): no data
claims the Skimmer yet, and packAtlases fails on sprites no data claims; 17.4 px per model unit, the
concept's):
  skimmer_0..31        40x40, `orientation: 16 angles` x 2 tail frames, index heading x 2 + tail
                       (heading k = k x 22.5 degrees clockwise from straight down, heading 0 moving
                       down the screen, each its own render under the fixed key light): the body above
                       the waterline with its broken foam collar; the tail beat swings the forked tail
                       left and right (the concept's two frames, phase 0 and pi, were the same pose:
                       sin 0 = sin pi; production uses +-pi/2), suggested 8 fps; the violet snout gland
                       and eyes emissive
  skimmer-sub_0..15    40x40, the whole plain model per heading (tail centred), drawn through the
                       `sub` pass under the surface layer
  skimmer-wake_0..127  64x64, the wake per heading (index heading x 8 + frame, an 8-frame loop at
                       20 fps), centred on the Skimmer: the bow wave peeling off along two V arms and a
                       churned trail streaming back from the stern; drawn on the surface under the
                       body (after the sub pass); stepped translucency, alpha-blended foam
  skimmer-sink_0..79   48x48, the death's flip and sink at 8 headings (index heading/2 x 10 + step,
                       the nearest of the 16 headings' even ones; 6 steps a frame, 1 s): the hull rolls
                       over onto its back (0 -> 180 degrees about its length), settles and goes under
                       (what is below the surface pre-rendered seen through the water), a collar along
                       the shrinking waterline, a foam ring spreading, a few flecks; the burst is the
                       generic `small` one with the water splash (explosion-water-small, shared, by unit:
                       G5 = a), drawn over the first steps
  design/enemies/naval/skimmer/concept/skimmer-final-r35-a.png/.gif

How the game draws it (step G6): skimmer-sub_<heading> through the `sub` pass; skimmer-wake_<heading x 8 +
frame> over it on the surface; skimmer_<heading x 2 + tail> on top. On the death: explosion-small and
explosion-water-small (the water splash by unit) at its centre, and skimmer-sink_<heading/2 x 10 + step>.

Run: python3 tools/art/skimmer.py [--assets]   (~2 min; without --assets only the review files)
"""
import sys
from concurrent.futures import ProcessPoolExecutor

import numpy as np
from PIL import Image

import artkit
from artkit import DESIGN, TAU, sprite

import driftjelly as dj  # noqa: E402  (tools/art: the sub-pass stand-in, the foam collar along a waterline)
import l13_props  # noqa: E402  (tools/art: the halted frame's stand-in plate)
import scenes_r08 as s8  # noqa: E402  (concept script, imported unchanged: loop-safe foam streams)
import vrell_fx as fx  # noqa: E402  (tools/art: timing)
import water_fx as wfx  # noqa: E402  (tools/art: Foam, waterline rendering, the sinking's foam ring)
from render import r06_models as m6  # noqa: E402
from render import r07_models as m7  # noqa: E402
from render.enemy_rigs import model_rotation, model_space_materials  # noqa: E402
from render.sdf import rotate_x, rotate_y, rotate_z  # noqa: E402

SCRIPT = "skimmer.py"
BATCH = "M5 part G batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
REVIEW_ROUND = "r35"
UNIT = DESIGN / "enemies" / "naval" / "skimmer"
CONCEPT = UNIT / "concept"
SLUG = "skimmer"

SIZE = (40, 40)
EXTENT = 2.3                                    # the concept's (17.4 px per model unit)
PX = SIZE[0] / EXTENT
HEADINGS = 16
TAILS = (-np.pi / 2, np.pi / 2)                 # the tail swung left, right
WATER = 0.0                                     # the water plane (model z): the keel below it
COLOURS = 32
WAKE, WAKE_FRAMES = (64, 64), 8
SINK, SINK_FRAMES, SINK_HEADINGS = (48, 48), 10, 8
SINK_EXTENT = EXTENT * SINK[0] / SIZE[0]
SPEED, WEAVE_AMP, WEAVE_T = 180.0, 40.0, 1.2    # the stat block's lane walker
X0, Y0 = 0, 230                                 # the review loop's crop of the halted frame


def rotation(k, n=HEADINGS):
    """Model +Y forward -> screen heading k of n (0 = moving down, clockwise)."""
    return model_rotation(np.pi / 2 + TAU * k / n)


def heading_vec(k, n=HEADINGS):
    a = np.pi / 2 + TAU * k / n                   # screen angle, y down
    return np.cos(a), np.sin(a)


def turned(make, rot):
    scene, mats = make()
    return (lambda p: scene(rotate_z(p, rot))), model_space_materials(mats, rot)


def render_above(k, tail):
    rot = rotation(k)
    scene, mats = turned(lambda: m7.clipped(m6.skimmer, WATER, "above")(phase=TAILS[tail]), rot)
    return artkit.native(*artkit.render_hi(scene, mats, SIZE, EXTENT))


def render_sub(k):
    rot = rotation(k)
    scene, mats = turned(lambda: m6.skimmer(0.0), rot)
    return artkit.native(*artkit.render_hi(scene, mats, SIZE, EXTENT))


def keel_mask(k):
    """The hull's waterline: the model's slice at the water plane, native px (1 inside)."""
    rot = rotation(k)
    scene, _ = m6.skimmer(0.0)
    w, h = SIZE
    ss = 4
    px = EXTENT / (w * ss)
    gx, gy = np.meshgrid((np.arange(w * ss) + 0.5 - w * ss / 2) * px, -(np.arange(h * ss) + 0.5 - h * ss / 2) * px)
    p = np.stack([gx.ravel(), gy.ravel(), np.full(gx.size, WATER + 0.01)], axis=-1)
    d, _ = scene(rotate_z(p, rot))
    inside = (d.reshape(gx.shape) < 0).astype(np.float64)
    return inside.reshape(h, ss, w, ss).mean(axis=(1, 3))


def above_frame(job):
    """The above-water body with its collar: the collar lies on the water round the keel's waterline,
    and laps a little over the hull's rim (two loop phases, one per tail frame)."""
    k, tail = job
    above = render_above(k, tail)
    mask = keel_mask(k)
    f = dj.Field(*SIZE)
    hx, hy = heading_vec(k)
    dj.edge_foam(f, mask, TAU * tail / 2 + 0.7 * k, 3601 + k, width=1.3, specks=8, reach=5.0,
                 centre=(SIZE[0] / 2 - hx * 2, SIZE[1] / 2 - hy * 2), inner=0.6)
    out = Image.new("RGBA", SIZE)
    out.alpha_composite(f.image())
    out.alpha_composite(above)
    return out


# =========================================================================== the wake

def wake_frame(job):
    """Frame j of heading k's wake loop: two V arms of bow spray peeling back from the snout and a
    churned trail from the stern, streaming back along the heading (water_fx.small_wake's streams
    turned to the heading)."""
    k, j = job
    w, h = WAKE
    foam = wfx.Foam(w, h)
    ff = wfx.loop_f(j, WAKE_FRAMES)
    fx_, fy = heading_vec(k)                        # forward on screen
    bx, by = -fx_, -fy                               # back
    nx, ny = -fy, fx_                                # side
    cx, cy = w / 2, h / 2
    bow = (cx + fx_ * 12.0, cy + fy * 12.0)
    stern = (cx + bx * 10.0, cy + by * 10.0)
    for side in (-1, 1):
        dx, dy = bx + side * 0.45 * nx, by + side * 0.45 * ny
        s8.stream(foam, ff, bow[0] + side * 3 * nx, bow[1] + side * 3 * ny, dx, dy, 30, 2, 34, 0.5, 2.6,
                  0.8, 1.5, 0.95, 3611 + k * 3 + side)
    s8.stream(foam, ff, stern[0], stern[1], bx, by, 24, 2, 40, 1.2, 5.5, 0.9, 1.9, 0.95, 3631 + k, wave=1.2)
    s8.stream(foam, ff, cx, cy, bx, by, 18, 1, 16, 4.0, 7.0, 0.7, 1.2, 0.6, 3641 + k,
              color=tuple(int(c) for c in wfx.FOAM_SHADE))
    return foam.image(4)


# =========================================================================== the sinking

def sink_model(t):
    """The skiff rolled ``t`` x 180 degrees about its length and lowered as it fills, nose dipping."""
    roll = np.pi * min(1.0, t / 0.55) ** 1.4
    drop = 0.06 + 0.5 * max(0.0, (t - 0.3) / 0.7) ** 1.3
    pitch = 0.25 * max(0.0, (t - 0.45) / 0.55)
    scene, mats = m6.skimmer(0.0, glow=max(0.0, 1 - t * 2.5))

    def posed(p):
        q = p.copy()
        q[:, 2] += drop
        q = rotate_x(rotate_y(q, roll), pitch)
        return scene(q)
    return posed, mats


def sink_frame(job):
    kk, i = job
    k = kk * 2
    t = (i + 1) / SINK_FRAMES
    rot = rotation(k)
    scene0, mats = sink_model(t)
    scene = lambda p: scene0(rotate_z(p, rot))          # noqa: E731
    mats = model_space_materials(mats, rot)
    body = wfx.waterline_render(scene, mats, SINK, SINK_EXTENT, z_top=3.0, steps=150, depth=0.5, crisp=70)
    pts = wfx.waterline_points(scene, SINK, SINK_EXTENT, step=2)
    foam = wfx.Foam(*SINK)
    solid = np.array(body)[..., 3]
    wfx.sink_foam(foam, wfx.loop_f(i, SINK_FRAMES), pts, SINK[0] / 2, SINK[1] / 2, t, 3651 + i, (13, 13), solid)
    out = Image.new("RGBA", SINK)
    if t < 0.98:
        out.alpha_composite(body)
    out.alpha_composite(foam.image(4))
    return out


# =========================================================================== build

def sprite_sets():
    above_jobs = [(k, tail) for k in range(HEADINGS) for tail in range(2)]
    wake_jobs = [(k, j) for k in range(HEADINGS) for j in range(WAKE_FRAMES)]
    sink_jobs = [(kk, i) for kk in range(SINK_HEADINGS) for i in range(SINK_FRAMES)]
    with ProcessPoolExecutor() as pool:
        above = list(pool.map(above_frame, above_jobs))
        sub = list(pool.map(render_sub, range(HEADINGS)))
        wake = list(pool.map(wake_frame, wake_jobs))
        sink = list(pool.map(sink_frame, sink_jobs))
    body = artkit.quantize_set(above + sub, COLOURS)
    return {SLUG: body[:len(above)], f"{SLUG}-sub": body[len(above):],
            f"{SLUG}-wake": artkit.quantize_set(wake, 8), f"{SLUG}-sink": artkit.quantize_set(sink, COLOURS)}


def write_assets(sets):
    for name, frames in sets.items():
        artkit.write_frames(name, frames, SOURCE)
        print(f"{name}: {len(frames)} frames, {frames[0].width}x{frames[0].height}")


# =========================================================================== review

def put(cell, img, x, y):
    cell.alpha_composite(img, (int(round(x - img.width / 2)), int(round(y - img.height / 2))))


def add(cell, img, x, y):
    return artkit.add_light(cell, img, (int(round(x - img.width / 2)), int(round(y - img.height / 2))))


def nearest(angle, n=HEADINGS):
    """Heading index (0 = down, clockwise) of a screen angle (y down)."""
    return int(round(((angle - np.pi / 2) % TAU) / (TAU / n))) % n


def draw_skimmer(cell, sets, x, y, k, tail, wf, t):
    put(cell, dj.sub_pass(sets[f"{SLUG}-sub"][k], t), x, y)
    put(cell, sets[f"{SLUG}-wake"][k * WAKE_FRAMES + wf], x, y)
    put(cell, sets[SLUG][k * 2 + tail], x, y)


class LaneWalker:
    """A Skimmer on a lane: 180 px/s along the polyline, the 40 px / 1.2 s weave across it."""

    def __init__(self, lane, t0, reverse=False, phase=0.0):
        p = np.array(lane[::-1] if reverse else lane, float)
        seg = np.hypot(*np.diff(p, axis=0).T)
        self.p, self.cum, self.t0, self.ph = p, np.concatenate([[0], np.cumsum(seg)]), t0, phase

    def at(self, t):
        s = (t - self.t0) * SPEED
        if s < 0 or s > self.cum[-1]:
            return None
        i = min(np.searchsorted(self.cum, s) - 1, len(self.p) - 2)
        i = max(i, 0)
        u = (s - self.cum[i]) / (self.cum[i + 1] - self.cum[i])
        a, b = self.p[i], self.p[i + 1]
        d = (b - a) / np.hypot(*(b - a))
        n = np.array([-d[1], d[0]])
        w = WEAVE_AMP * np.sin(TAU * (t - self.t0) / WEAVE_T + self.ph)
        return a + (b - a) * u + n * w

    def pose(self, t):
        p0, p1 = self.at(t), self.at(t + 0.02)
        if p0 is None or p1 is None:
            return None
        return p0, float(np.arctan2(p1[1] - p0[1], p1[0] - p0[0]))


def review(sets):
    artkit.REVIEW_ROUND = REVIEW_ROUND
    plate = l13_props.HaltPlate()
    boom = artkit.load_frames("explosion-small")
    splash = artkit.load_frames("explosion-water-small")
    g0 = plate.ground(0)
    lanes = plate.lanes()
    # as drawn: four headings in the west lead
    drawn = []
    for k in (0, 4, 8, 12):
        cell = g0.crop((0, 200, 72, 272)).copy()
        draw_skimmer(cell, sets, 36, 36, k, 0, 0, 0.3 * k)
        drawn.append(cell)
    sink_drawn = []
    for i in range(0, SINK_FRAMES, 1):
        cell = g0.crop((0, 300, 64, 364)).copy()
        put(cell, sets[f"{SLUG}-sink"][i], 32, 32)
        age = i * 6
        if age < 24:
            cell = add(cell, boom[min(age // 2, len(boom) - 1)], 32, 32)
            put(cell, splash[min(age // 2, len(splash) - 1)], 32, 32)
        sink_drawn.append(cell)
    sheet = artkit.review_sheet("SKIMMER - FINAL SPRITES (PROPOSAL, STRAIGHT FROM SKIMMER-R06-A)", [
        ("ABOVE THE WATERLINE WITH ITS FOAM COLLAR: 16 HEADINGS (0 = MOVING DOWN, CLOCKWISE), TAIL FRAME 0",
         sets[SLUG][0::2], 2, False),
        ("TAIL BEAT: HEADINGS 0 AND 4, TAIL LEFT / RIGHT", [sets[SLUG][0], sets[SLUG][1], sets[SLUG][8], sets[SLUG][9]],
         4, False),
        ("PLAIN BODY FOR THE SUB PASS (UNDER THE SURFACE LAYER)", sets[f"{SLUG}-sub"][0::2], 2, False),
        ("WAKE PER HEADING (64X64): HEADINGS 0-15, FRAME 0", sets[f"{SLUG}-wake"][0::WAKE_FRAMES], 1, False),
        ("WAKE LOOP, HEADING 2: 8 FRAMES AT 20 FPS", sets[f"{SLUG}-wake"][2 * WAKE_FRAMES:3 * WAKE_FRAMES], 2, False),
        ("AS DRAWN ON THE HALTED FRAME'S STAND-IN: SUB PASS + WAKE + BODY, HEADINGS 0 / 4 / 8 / 12", drawn, 3, False),
        ("DEATH: FLIP AND SINK (48X48, HEADING 0; 8 HEADINGS, 6 STEPS A FRAME)", sets[f"{SLUG}-sink"][:SINK_FRAMES], 2,
         False),
        ("AS DRAWN: WITH EXPLOSION-SMALL AND THE WATER SPLASH (EXPLOSION-WATER-SMALL) OVER THE FIRST STEPS", sink_drawn,
         2, False),
    ], width=1400, batch=BATCH)
    artkit.save_review(sheet, review_loop(plate, sets, lanes, boom, splash), CONCEPT, SLUG, fps=20)


def review_loop(plate, sets, lanes, boom, splash):
    """240x270 of the halted frame at 2x (the west lead, the floe's corner and the south crossing):
    Skimmers on their lanes at 180 px/s, weaving 40 px / 1.2 s, wakes per heading; one shot and sinking
    at 3.2 s."""
    fps, total = 20, 7.0
    walkers = [LaneWalker(lanes["west"], 0.0, phase=0.0), LaneWalker(lanes["west"], 0.9, reverse=True, phase=1.8),
               LaneWalker(lanes["south"], 0.4, phase=2.4), LaneWalker(lanes["west"], 2.4, phase=0.9),
               LaneWalker(lanes["south"], 3.3, reverse=True, phase=3.0)]
    dead_i, dead_t = 0, 2.2
    gif = []
    death = None
    for f in range(int(total * fps)):
        t = f / fps
        cell = plate.ground(f % 80).crop((X0, Y0, X0 + 240, Y0 + 270))
        for i, wk in enumerate(walkers):
            if i == dead_i and t >= dead_t:
                if death is None:
                    pos, ang = wk.pose(dead_t)
                    death = (pos, nearest(ang) // 2)
                (x, y), kk = death
                x, y = x - X0, y - Y0
                age = int((t - dead_t) * 60)
                step = age // 6
                if step < SINK_FRAMES:
                    put(cell, sets[f"{SLUG}-sink"][kk * SINK_FRAMES + step], x, y)
                if age < 24:
                    cell = add(cell, boom[min(age // 2, len(boom) - 1)], x, y)
                    put(cell, splash[min(age // 2, len(splash) - 1)], x, y)
                continue
            pose = wk.pose(t)
            if pose is None:
                continue
            (x, y), ang = pose
            draw_skimmer(cell, sets, x - X0, y - Y0, nearest(ang), int(t * 8) % 2, f % WAKE_FRAMES, t + i)
        gif.append(sprite.enlarge(cell, 2))
    return gif


def main(args):
    artkit.REVIEW_ROUND = REVIEW_ROUND
    sets = sprite_sets()
    if "--assets" in args:
        write_assets(sets)
    review(sets)


if __name__ == "__main__":
    main(sys.argv[1:])
