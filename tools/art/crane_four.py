#!/usr/bin/env python3
"""Production art: Crane Four, Level 02's hazard (design/campaign/act-1-first-contact/
level-02-shipyard-burning, Hazards; M4 part B batch).

Outputs (assets/sprites/ and assets/pivots/):
  crane-four-arm_0..60.png   the arm hanging from its pivot at 61 angles, -90 to +90 degrees in
                             3 degree steps (from straight down, positive to the right, as the
                             level data counts them), each its own render under the fixed key
                             light and cropped to the arm; the swing picks the nearest angle
  pivots/crane-four.json     per frame the pivot's px from the sprite's top left
  crane-four-light.png       9x9 red warning light (additive), blinked along the jib in the telegraph
  crane-four-clamp-light.png 15x15 amber clamp light (additive), lit while the arm swings
  crane-four-canister.png    18x24 the CDF supply canister on the hook, until the clamp lets go
  design/campaign/.../level-02-shipyard-burning/concept/crane-four-final-r15-a.png/.gif

The arm is a lattice jib of the station kit's look (render/station.py, Level 01's muted kit palette
of tools/concept/backdrop_l01.py): two rails, cross braces, a cable tray, the hook block and the
clamp at its tip; 1 world unit = 1 px, rendered at 4x. The canister never turns, so it is its own
sprite drawn at the tip.

Run: python3 tools/art/crane_four.py [--review]   (~40 s on 20 cores)
"""
import sys
from concurrent.futures import ProcessPoolExecutor

import numpy as np
import yaml
from PIL import Image

import artkit
from artkit import DESIGN, sprite

import backdrop_l01 as l01  # noqa: E402  (concept script, imported unchanged)
import vfx_r08 as v8  # noqa: E402
from render import sdf, station  # noqa: E402
from render.sdf import rotate_z, sd_box, sd_capsule, sd_cylinder_y, sd_sphere, union  # noqa: E402
from render.station import ACCENT, DARK, HULL, RED  # noqa: E402

SCRIPT = "crane_four.py"
SOURCE = artkit.source_note(SCRIPT, "M4 part B batch")
LEVEL = DESIGN / "campaign" / "act-1-first-contact" / "level-02-shipyard-burning"
FACTOR = 4
STEP = 3
ANGLES = list(range(-90, 91, STEP))
MARGIN = 14
COLOURS = 32
# The key light at a fixed place relative to the pivot, as for a 300 px scene, so every angle is
# lit from the same point.
LIGHT = np.asarray(sdf.KEY_POS) * (300 / 2.3)
# Where the warning lights sit along the jib, px from the pivot (the game draws them there).
LIGHTS = [60, 120, 180, 235]


def crane():
    data = yaml.safe_load((LEVEL / "data.yaml").read_text(encoding="utf-8"))
    return data["cranes"][0]


def arm_scene(length, width):
    """The jib hanging straight down from the pivot at the origin (y up), its tip at y = -length."""
    hw = width / 2 - 2
    bays = max(1, int(round(length / 26)))
    pitch = length / bays

    def scene(p):
        items = [(sd_box(p, (x, -length / 2, 0), (1.8, length / 2, 1.8), 0.5), DARK) for x in (-hw, hw)]
        for i in range(bays):
            y0 = -i * pitch
            items.append((sd_box(p, (0, y0, -1), (hw, 1.2, 1.0), 0.3), DARK))
            items.append((sd_capsule(p, (-hw, y0, -1), (hw, y0 - pitch, -1), 0.9), DARK))
        items.append((sd_box(p, (hw - 3.5, -length / 2, 2.2), (1.2, length / 2 - 6, 1.0), 0.3), HULL))
        items.append((sd_box(p, (0, 4, 1), (hw + 3, 6, 4), 1.0), HULL))               # the slewing ring
        items.append((sd_box(p, (0, -length + 4, 1.5), (hw + 1, 6, 3.5), 1.0), HULL))  # the hook block
        items.append((sd_box(p, (0, -length - 4, 2), (4, 3, 2.5), 0.8), ACCENT))      # the clamp
        for s in LIGHTS:
            items.append((sd_sphere(p, (-hw, -s, 2.6), 1.6), RED))
        return union(*items)
    return scene


def render_arm(angle_deg):
    spec = crane()
    length, width = spec["length"], spec["width"]
    a = np.radians(angle_deg)
    tip = (np.sin(a) * (length + 8), -np.cos(a) * (length + 8))
    x0, x1 = min(0, tip[0]) - MARGIN, max(0, tip[0]) + MARGIN
    y0, y1 = min(0, tip[1]) - MARGIN, max(0, tip[1]) + MARGIN
    w, h = int(np.ceil(x1 - x0)), int(np.ceil(y1 - y0))
    cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
    scene = arm_scene(length, width)
    # Turned by the angle about the pivot: a positive angle swings the tip to the right.
    turned = lambda p: scene(rotate_z(p, a))  # noqa: E731
    mats = station.materials(l01.KIT_PAL)
    key = (LIGHT - np.array([cx, cy, 0.0])) / (w / 2.3)
    hi = sdf.render(turned, mats, (w * FACTOR, h * FACTOR), float(w), center=(cx, cy), z_top=40.0, steps=150,
                    key_pos=key)
    img = sprite.make_sprite(hi, FACTOR, 64, crisp=60)
    # the pivot in the sprite: px from its top left (y down)
    return img, (round(-x0), round(y1))


def canister():
    def scene(p):
        return union((sd_cylinder_y(p, (0, 0, 0), 6.5, 9), HULL),
                     (sd_box(p, (0, 0, 6), (6.6, 2.2, 0.6), 0.3), ACCENT),
                     (sd_box(p, (0, 10, 0), (2.5, 1.5, 2.5), 0.4), DARK))
    return l01.part(scene, (18, 24), colors=20)


def glow(size, colour, core, radius):
    cv = v8.Canvas(size, size)
    d = cv.dist(size / 2, size / 2)
    cv.add(colour, v8.gauss(d, radius) * 1.2)
    cv.add(core, v8.gauss(d, radius * 0.35))
    return artkit.additive(cv.image())


def build():
    with ProcessPoolExecutor() as pool:
        results = list(pool.map(render_arm, ANGLES))
    frames = artkit.quantize_set([img for img, _ in results], COLOURS)
    artkit.write_frames("crane-four-arm", frames, SOURCE)
    artkit.write_pivots("crane-four", SOURCE, {
        "angles": ANGLES, "pivot": [list(pivot) for _, pivot in results], "lights": LIGHTS})
    artkit.write_frames("crane-four-light", [glow(9, (255, 40, 50), (255, 220, 220), 2.2)], SOURCE, single=True)
    artkit.write_frames("crane-four-clamp-light", [glow(15, (255, 170, 40), (255, 240, 200), 3.4)], SOURCE,
                        single=True)
    artkit.write_frames("crane-four-canister", [canister()], SOURCE, single=True)


def review():
    import json
    frames = artkit.load_frames("crane-four-arm")
    pivots = json.loads((artkit.PIVOTS / "crane-four.json").read_text(encoding="utf-8"))
    artkit.REVIEW_ROUND = "r15"
    light, clamp, can = (artkit.load_frames(n)[0] for n in
                         ("crane-four-light", "crane-four-clamp-light", "crane-four-canister"))
    sheet = artkit.review_sheet("CRANE FOUR - FINAL SPRITES", [
        ("ARM AT -90, -45, -15, 0, +15, +45, +90 DEGREES (61 ANGLES, 3 DEGREE STEPS)",
         [frames[ANGLES.index(a)] for a in (-90, -45, -15, 0, 15, 45, 90)], 1, False),
        ("WARNING LIGHT, CLAMP LIGHT (ADDITIVE)", [light, clamp], 6, True),
        ("CANISTER", [can], 6, False)], batch="M4 part B batch")
    spec = crane()
    gif = []
    w, h = 480, 300
    for i in range(60):                                  # telegraph, a swing to the right and back
        img = Image.new("RGBA", (w, h), artkit.PLATE)
        t = i / 60
        angle = -45 + 90 * (1 - np.cos(np.pi * min(1, t * 2))) / 2 if t < 0.5 else 45 - 90 * (1 - np.cos(np.pi * (t - 0.5) * 2)) / 2
        k = int(round((angle + 90) / STEP))
        px, py = pivots["pivot"][k]
        ox, oy = w / 2 - px, -10 - py + 20
        img.alpha_composite(frames[k], (int(ox), int(oy)))
        a = np.radians(ANGLES[k])
        tipx, tipy = w / 2 + np.sin(a) * spec["length"], 10 + np.cos(a) * spec["length"]
        sprite.paste_center(img, can, tipx, tipy + 12)
        img = artkit.add_light(img, clamp, (int(tipx - 7), int(tipy - 2)))
        if i % 10 < 5:
            for s in LIGHTS:
                img = artkit.add_light(img, light, (int(w / 2 + np.sin(a) * s - 4), int(10 + np.cos(a) * s - 4)))
        gif.append(sprite.enlarge(img, 2))
    artkit.save_review(sheet, gif, LEVEL / "concept", "crane-four", fps=20)


if __name__ == "__main__":
    if "--review" not in sys.argv[1:]:
        build()
    review()
