#!/usr/bin/env python3
"""Production art: the civilian crawler, the CDF evacuation hauler Level 04 escorts (design/allies,
Civilian crawler; M4 part D batch).

Outputs (assets/sprites/, size and headings from design/allies/data.yaml):
  civilian-crawler_0..20.png       72x84 canvas, the 40x72 crawler driving UP the screen, 7 headings
                                   x 3 wheel frames, indexed heading x 3 + phase (as EnemyLooks
                                   indexes its angle sets). Heading k is turned -30 + 10k degrees,
                                   counted clockwise on screen from straight up: index 0 = -30
                                   (nose turned to the left, the road bending left as it climbs),
                                   3 = straight up, 6 = +30 (nose to the right). In the game's
                                   facing convention (clockwise from straight down) that is
                                   facing = 180 degrees + the heading angle. Phase 0 -> 1 -> 2 moves
                                   the tyre tread 1 px forward per frame (the wheels rolling ahead)
  civilian-crawler-wreck_0..6.png  72x84, per heading (same order) the burnt-out wreck: lights,
                                   windows and beacons dead, the hull darkened and charred in
                                   blast spots that turn with the hull; the fire and smoke are the
                                   game's effects
  civilian-crawler-pip.png         10x18 HUD pip: a top-down silhouette of the bus and its sled in
                                   flat white on transparent, tinted green, amber or dark by the
                                   game
  design/allies/concept/civilian-crawler-final-r17-a.png/.gif

The crawler is the chosen round-16 model (tools/concept/allies_r16.py variant c, `crawler_c`, with
its station-kit materials in Level 01's KIT_PAL and its emissive amber beacons; imported
unchanged): a pressurised six-wheeled bus with a glass nose towing a sled of four cargo pods.
1 model unit = 1 px, rendered at 4x. Each heading is its own render, the model turned about the
vehicle's centre (the canvas centre) under the key light at the concept's fixed position; the tyre
tread and the wreck's char are evaluated in the model's own frame, so they turn with the hull.
One 40-colour palette for the 21 driving frames, one 32-colour palette for the 7 wrecks.

Run: python3 tools/art/civilian_crawler.py [--review]   (~30 s on 20 cores)
"""
import sys
from concurrent.futures import ProcessPoolExecutor
from dataclasses import replace

import numpy as np
from PIL import Image

import artkit
from artkit import DESIGN, sprite

import allies_r16 as ar16  # noqa: E402  (concept script, imported unchanged)
from render import sdf  # noqa: E402
from render.sdf import rotate_z  # noqa: E402

SCRIPT = "civilian_crawler.py"
SOURCE = artkit.source_note(SCRIPT, "M4 part D batch")
artkit.REVIEW_ROUND = "r17"
BATCH = "M4 part D batch"
CONCEPT = DESIGN / "allies" / "concept"
HEADINGS = [-30, -20, -10, 0, 10, 20, 30]   # degrees clockwise on screen from straight up
PHASES = 3
CANVAS = (72, 84)                           # fits the 40x72 body turned 30 degrees
FACTOR = 4
Y_CENTRE = -0.65                            # model y of the vehicle's centre (nose 34.8, skids -36.1)
TREAD_PERIOD = 3.0                          # px of tread pattern; a frame moves it 1 px
WHEEL_R, WHEEL_YS, WHEEL_Z = 4.6, (8.0, 18.0, 28.0), -0.5
# char spots of the wreck in model px (x, y, radius): roof, glass nose, sled pods, one wheel side
CHAR = [(4.0, 24.0, 7.0), (-6.0, 12.0, 6.0), (-2.0, 33.0, 4.5), (6.5, -15.0, 6.0),
        (-7.5, -27.0, 5.5), (12.0, 4.0, 4.0), (-11.0, 26.0, 3.5)]


def to_model(p, heading):
    """World points -> the model's frame for a hull turned ``heading`` radians clockwise."""
    q = rotate_z(p, -heading)
    q[:, 1] += Y_CENTRE
    return q


def tread(heading, phase):
    """Tyre tread bars across the rolling direction, by arc length around the nearest wheel axle,
    shifted forward one px per phase (the top of a rolling wheel moves ahead)."""
    def pattern(p, n):
        q = to_model(p, heading)
        ys = np.asarray(WHEEL_YS)
        yc = ys[np.argmin(np.abs(q[:, 1:2] - ys[None, :]), axis=1)]
        arc = np.arctan2(q[:, 1] - yc, q[:, 2] - WHEEL_Z) * WHEEL_R
        return np.where(((arc - phase) / TREAD_PERIOD) % 1 < 0.45, 0.42, 1.0)
    return pattern


def charred(heading, base):
    """The wreck's soot: the whole hull darkened, blast spots near black with a speckled rim."""
    def pattern(p, n):
        q = to_model(p, heading)
        k = np.full(len(q), 0.62)
        for x, y, r in CHAR:
            d = np.hypot(q[:, 0] - x, q[:, 1] - y) / r
            speck = 0.8 + 0.2 * np.sin(q[:, 0] * 3.1 + q[:, 1] * 2.3) * np.sin(q[:, 1] * 4.7 - q[:, 0])
            k *= 1 - 0.78 * np.clip(1.25 - d, 0, 1) ** 1.5 * speck
        return k * (base(p, n) if base is not None else 1.0)
    return pattern


def render_frame(heading_deg, phase, wreck=False):
    heading = np.radians(heading_deg)
    scene, _ = ar16.crawler_c()
    mats = ar16.materials(lit=not wreck)
    if wreck:
        mats = [replace(m, pattern=charred(heading, m.pattern)) for m in mats]
    else:
        mats[ar16.TREAD] = replace(mats[ar16.TREAD], pattern=tread(heading, phase))
    w, h = CANVAS
    turned = lambda p: scene(to_model(p, heading))  # noqa: E731
    hi = sdf.render(turned, mats, (w * FACTOR, h * FACTOR), float(w), z_top=40.0, steps=160,
                    key_pos=sdf.KEY_POS * (72 / w))
    return artkit.native(hi, FACTOR, crisp=60)


PIP = ["..XXXXXX..",
       ".XXXXXXXX.",
       "XXXXXXXXXX",
       ".XXXXXXXX.",
       "XXXXXXXXXX",
       ".XXXXXXXX.",
       "XXXXXXXXXX",
       ".XXXXXXXX.",
       "....XX....",
       "XXXX..XXXX",
       "XXXX..XXXX",
       "XXXX..XXXX",
       "XXXX..XXXX",
       "..........",
       "XXXX..XXXX",
       "XXXX..XXXX",
       "XXXX..XXXX",
       "XXXX..XXXX"]


def pip():
    """The HUD pip: the bus (its wheels as notches down the sides) over the tow bar and the four
    cargo pods."""
    a = np.array([[255 if c == "X" else 0 for c in row] for row in PIP], dtype=np.uint8)
    img = np.zeros(a.shape + (4,), np.uint8)
    img[a > 0] = (255, 255, 255, 255)
    return Image.fromarray(img, "RGBA")


def build():
    jobs = [(hd, ph, False) for hd in HEADINGS for ph in range(PHASES)] + [(hd, 0, True) for hd in HEADINGS]
    with ProcessPoolExecutor() as pool:
        frames = list(pool.map(render_frame, *zip(*jobs)))
    n = len(HEADINGS) * PHASES
    artkit.write_frames("civilian-crawler", artkit.quantize_set(frames[:n], 40), SOURCE)
    artkit.write_frames("civilian-crawler-wreck", artkit.quantize_set(frames[n:], 32), SOURCE)
    artkit.write_frames("civilian-crawler-pip", [pip()], SOURCE, single=True)


def tinted(img, colour):
    a = np.array(img).astype(np.float64)
    a[..., :3] *= np.array(colour) / 255
    return Image.fromarray(a.astype(np.uint8), "RGBA")


def review():
    frames = artkit.load_frames("civilian-crawler")
    wrecks = artkit.load_frames("civilian-crawler-wreck")
    pip_img = artkit.load_frames("civilian-crawler-pip")[0]
    heads = [frames[k * PHASES:(k + 1) * PHASES] for k in range(len(HEADINGS))]
    sheet = artkit.review_sheet("CIVILIAN CRAWLER - FINAL SPRITES", [
        ("7 HEADINGS -30 TO +30, CLOCKWISE FROM UP (INDEX 0 = -30), PHASE 0", [h[0] for h in heads], 2, False),
        ("WHEEL FRAMES 0-2, STRAIGHT UP (TREAD 1 PX FORWARD PER FRAME)", heads[3], 4, False),
        ("WRECKS, 7 HEADINGS (FIRE AND SMOKE ARE GAME EFFECTS)", wrecks, 2, False),
        ("HUD PIP: WHITE, TINTED GREEN, AMBER, DARK", [pip_img] + [tinted(pip_img, c) for c in
                                                           ((90, 230, 110), (255, 176, 40), (70, 74, 90))], 6, False),
        ("1X: HEADINGS AND WRECKS", [h[0] for h in heads] + wrecks, 1, False)], batch=BATCH)

    # a column of five on a winding road, the ground scrolling down under the convoy
    w, per = 240, 600
    spacing, road_half = 84, 27

    def road_x(y):
        return w / 2 + 44 * np.sin(2 * np.pi * y / per) + 10 * np.sin(4 * np.pi * y / per + 1.1)

    strip = np.array(ar16.regolith((w, per), 1701, road_x, road_half))
    gif = []
    view_h, speed = 540, 10                     # 10 px a frame at 12 fps: the 120 px/s scroll
    for f in range(per // speed):
        scroll = f * speed
        ground = Image.fromarray(np.roll(strip, scroll, axis=0)[:view_h], "RGBA")
        for i in range(5):
            y = 150 + i * spacing                                  # Level 04's column
            world = y - scroll
            x = road_x(world)
            slope = (road_x(world + 0.5) - road_x(world - 0.5))   # dx per px down the screen
            angle = np.degrees(np.arctan(-slope))                  # nose turned right when x grows upwards
            k = int(np.clip(np.round(angle / 10) + 3, 0, 6))
            spr = heads[k][f % PHASES]
            sx, sy = x - spr.width / 2, y - spr.height / 2
            sprite.paste(ground, sprite.shadow_of(spr, opacity=0.55, blur=1.0), sx + ar16.SHADOW[0],
                         sy + ar16.SHADOW[1])
            sprite.paste(ground, spr, sx, sy)
        gif.append(ground)
    artkit.save_review(sheet, gif, CONCEPT, "civilian-crawler", fps=12)


if __name__ == "__main__":
    if "--review" not in sys.argv[1:]:
        build()
    review()
