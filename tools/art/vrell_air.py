#!/usr/bin/env python3
"""Production art: Level 01's Vrell flyers, the Skitter and the Needler (design/enemies/air).

Outputs (assets/sprites/, sizes from the parts' data.yaml):
  skitter_0..95.png  24x24, `orientation: 16 angles`: an angle set of 16 headings x 6 wing-beat
                     frames, indexed heading * 6 + frame; heading k flies k x 22.5 degrees
                     clockwise from straight down (10 fps in the game: 0.6 s per beat)
  needler_0..5.png   36x36, `orientation: fixed` (flies nose-down): one claw-snap cycle
  design/enemies/air/<slug>/concept/<slug>-final-r12-a.png/.gif   review sheet and loop

Models and role colours are the chosen round-04 ones (tools/concept/enemies_r04.py, models in
render/enemy_models.py). The Skitter's wing beat is rebuilt here from the concept model's parts so
the wings both lift (about the body axis) and sweep (about the wing root) over a figure-eight
stroke: from above, a lift alone is a small foreshortening and reads as a flicker. Every heading
is its own render with the key light fixed. The Needler's claw cycle is the concept model with a
wider swing, opening over three frames and snapping shut in one.

Run: python3 tools/art/vrell_air.py [skitter] [needler] [--review]   (~30 s)
"""
import sys
from concurrent.futures import ProcessPoolExecutor

import numpy as np
import yaml
from PIL import Image

import artkit
from artkit import DESIGN, TAU, sprite

import enemies_r04 as e4  # noqa: E402  (concept script, imported unchanged)
from render import enemy_models as em  # noqa: E402
from render.sdf import mirror_x, rotate_z, sd_capsule, sd_ellipsoid, sd_plate, sd_sphere, union  # noqa: E402

SCRIPT = "vrell_air.py"
SOURCE = artkit.source_note(SCRIPT)
EXTENT = 2.3
FRAMES = 6
SKITTER_HEADINGS = 16
SKITTER_COLOURS = 24
NEEDLER_COLOURS = 32
# The concept Skitter's scythe wing (render/enemy_models.skitter_a), hinged at its root.
SKITTER_WING = [(0.10, 0.20), (0.55, -0.05), (0.98, -0.62), (0.80, -0.60), (0.12, -0.25)]
WING_ROOT = (0.1, 0.0, 0.0)
# Needler claw cycle as the concept model's anim value (claw angle 0.18 x anim rad): opens over
# three frames, snaps shut in one, settles over two.
NEEDLER_CLAW = [0.0, -2.0, -3.5, 2.8, 2.0, 1.0]


def size_of(slug):
    data = yaml.safe_load((DESIGN / "enemies" / "air" / slug / "data.yaml").read_text(encoding="utf-8"))
    return tuple(data["size"])


def skitter_model(lift, sweep, heading):
    """The concept Skitter facing ``heading`` (radians clockwise from down), its wings lifted and
    swept by the given angles."""
    def scene(p):
        p = em._face_down(rotate_z(p, -heading))
        q = mirror_x(p)
        wing = em._hinge(em._hinge(q, WING_ROOT, sweep, axis="z"), WING_ROOT, lift)
        d, m = union(
            (sd_ellipsoid(p, (0, 0.05, 0), (0.2, 0.78, 0.17)), em.V_SEAM),
            (sd_capsule(p, (0, 0.55, 0.02), (0, 1.0, 0.0), 0.09, 0.012), em.V_BODY),
            (sd_plate(wing, SKITTER_WING, 0.0, 0.05, 0.02,
                      taper=lambda x, y: np.clip(1.1 - 0.9 * x, 0.25, 1)), em.V_BODY),
            k=0.1,
        )
        return union(
            (d, m),
            (sd_capsule(q, (0.08, -0.55, 0.0), (0.2, -0.95, -0.02), 0.05, 0.01), em.V_DARK),
            (sd_sphere(p, (0, 0.62, 0.1), 0.085), em.V_EYE),
            (sd_ellipsoid(q, (0.4, -0.1, 0.03), (0.1, 0.05, 0.03)), em.V_GLOW),
            k=0.03,
        )
    return scene, em.vrell_scheme_mats("skitter", "a")


def wing_beat(k):
    """Lift and sweep of beat frame ``k``: wings raised high and narrow at frame 0, spread wide
    at frame 3, swept forward on the way down and back on the way up."""
    a = TAU * k / FRAMES
    return -(0.4 + 0.75 * np.cos(a)), 0.3 * np.sin(a)


def render_skitter(heading, k):
    scene, mats = skitter_model(*wing_beat(k), heading)
    return artkit.native(*artkit.render_hi(scene, mats, size_of("skitter"), EXTENT))


def render_needler(k):
    scene, mats = e4.R04["needler-a"][4](anim=NEEDLER_CLAW[k], glow=1.0)
    return artkit.native(*artkit.render_hi(scene, mats, size_of("needler"), EXTENT))


def build(slug):
    with ProcessPoolExecutor() as pool:
        if slug == "skitter":
            jobs = [(TAU * h / SKITTER_HEADINGS, k) for h in range(SKITTER_HEADINGS) for k in range(FRAMES)]
            frames = artkit.quantize_set(list(pool.map(render_skitter, *zip(*jobs))), SKITTER_COLOURS)
        else:
            frames = artkit.quantize_set(list(pool.map(render_needler, range(FRAMES))), NEEDLER_COLOURS)
    artkit.write_frames(slug, frames, SOURCE)


def review_skitter():
    frames = artkit.load_frames("skitter")
    heading = [frames[h * FRAMES:(h + 1) * FRAMES] for h in range(SKITTER_HEADINGS)]
    sheet = artkit.review_sheet("SKITTER - FINAL SPRITES", [
        ("WING BEAT, ONE CYCLE (HEADING 0, FLYING DOWN)", heading[0], 6, False),
        ("WING BEAT, HEADING 5", heading[5], 6, False),
        ("16 HEADINGS (CLOCKWISE FROM DOWN), WINGS RAISED", [h[0] for h in heading], 3, False),
        ("16 HEADINGS, WINGS SPREAD", [h[3] for h in heading], 3, False),
        ("1X, 16 HEADINGS", [h[3] for h in heading], 1, False)])
    # a snake of five on a figure-eight, each showing the heading nearest its direction of flight
    steps = 72                                  # 30 fps; the beat advances every 3rd frame (10 fps)
    gif = []
    for i in range(steps):
        cell = Image.new("RGBA", (150, 150), artkit.PLATE)
        for j in range(5):
            t = TAU * (i - 4 * j) / steps
            x, y = 75 + 52 * np.sin(t), 75 + 40 * np.sin(2 * t)
            dx, dy = 52 * np.cos(t), 80 * np.cos(2 * t)                  # screen y down
            k = int(round(np.arctan2(-dx, dy) / TAU * SKITTER_HEADINGS)) % SKITTER_HEADINGS
            sprite.paste_center(cell, heading[k][(i // 3 + j) % FRAMES], x, y)
        gif.append(sprite.enlarge(cell, 3))
    artkit.save_review(sheet, gif, DESIGN / "enemies" / "air" / "skitter" / "concept", "skitter", fps=30)


def review_needler():
    frames = artkit.load_frames("needler")
    sheet = artkit.review_sheet("NEEDLER - FINAL SPRITES", [
        ("CLAW SNAP, ONE CYCLE", frames, 5, False),
        ("1X", frames, 1, False)])
    w, h = frames[0].size
    gif = []
    for i in range(FRAMES * 4):
        cell = Image.new("RGBA", (w * 3, h), artkit.PLATE)
        for j in range(3):                      # three units at their own phase, as in a formation
            cell.alpha_composite(frames[(i + j) % FRAMES], (j * w, 0))
        gif.append(sprite.enlarge(cell, 5))
    artkit.save_review(sheet, gif, DESIGN / "enemies" / "air" / "needler" / "concept", "needler", fps=10)


REVIEWS = {"skitter": review_skitter, "needler": review_needler}

if __name__ == "__main__":
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    for unit in args or list(REVIEWS):
        if "--review" not in sys.argv[1:]:
            build(unit)
        REVIEWS[unit]()
