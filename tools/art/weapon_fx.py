#!/usr/bin/env python3
"""Production art: the effects of the Act 1 arsenal beyond the Pulse Cannon (design/player/weapons;
M4 part A batch). The Pulse Cannon's own `pulse` set is tools/art/pulse_cannon.py.

Outputs (assets/sprites/; glows additive, premultiplied on black; physical rounds solid):
  <weapon>-shot_<deg>.png        a straight-flying weapon's projectile at every angle its patterns
                                 use (degrees clockwise from up, mod 360; from the weapons'
                                 data.yaml with the pods' convergence and the side guns' mirroring):
                                 scatter-vulcan and autocannon-pod (tracers), side-splitter (bolts)
  lance-laser-shot_1..6.png      the lance at each level, 6 = overdrive (2 px core to L2, 3 to L4, 4 beyond)
  micro-missile-pod-shot_0..31   the micro-missile with its plume at 32 headings, k x 11.25 degrees
                                 clockwise from up, each rendered with the key light fixed
  bomb-rack-shot.png             the bomb, nose up; drawn shrinking as it falls
  hammer-mortar-shot.png         the shell, nose up; drawn growing and shrinking on its arc
  ballistic-muzzle_0..2.png      muzzle flashes of the ballistic and launcher kinds (the energy kind
  launcher-muzzle_0..2.png       is pulse-muzzle)
  ballistic-impact_0..3.png      impacts of the ballistic and explosive kinds (energy: pulse-impact),
  explosive-impact_0..3.png      additive like the explosions
  design/player/weapons/concept/weapons-final-r14-a.png/.gif

The looks are the chosen round-08 projectile families (tools/concept/vfx_r08.py: tracer,
energy_bolt, laser_lance, missile_model with_plume, bomb_model, shell_model, muzzle_frames,
impact_frames), each family on one palette. The tracers and bolts are 2D light fields, so each
angle is drawn at that angle rather than rotated; the missile is a lit model rendered per heading.

Run: python3 tools/art/weapon_fx.py [--review]   (~20 s)
"""
import sys

import numpy as np
import yaml
from PIL import Image

import artkit
from artkit import DESIGN, TAU, sprite

import vfx_r08 as v8  # noqa: E402  (concept script, imported unchanged)

SCRIPT = "weapon_fx.py"
BATCH = "M4 part A batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
REVIEW_ROUND = "r14"
COLOURS = 32
WEAPONS = DESIGN / "player" / "weapons"
MISSILE_HEADINGS = 32
LANCE_WIDTH = {1: 2, 2: 2, 3: 3, 4: 3, 5: 4, 6: 4}
STRAIGHT = {
    "scatter-vulcan": lambda a: v8.tracer(11, 1.0, angle=a),
    "autocannon-pod": lambda a: v8.tracer(8, 1.4, angle=a),
    "side-splitter": lambda a: v8.energy_bolt(10, 2.2, angle=a),
}


def angles(slug):
    """Every angle (whole degrees, mod 360) the weapon's projectiles leave at, as the game builds
    its muzzles (vanguard.content.SimSpecs.weapon): pods mirrored on the left and turned in by the
    convergence, side guns mirrored to the left."""
    data = yaml.safe_load((WEAPONS / slug / "data.yaml").read_text(encoding="utf-8"))
    converge = data.get("converge", 0)
    out = set()
    for level in data["levels"] + [data["overdrive"]]:
        for _, a in level["pattern"]:
            if data.get("pod"):
                out |= {a - converge, -a + converge}
            elif data.get("mirrored"):
                out |= {a, -a}
            else:
                out.add(a)
    return sorted({round(a) % 360 for a in out})


def missile(heading):
    """The micro-missile at ``heading`` (radians clockwise from up) with its plume behind it."""
    body = v8._obj(v8.missile_model, 10, 1.9, heading=-heading)
    size = 24
    cv = v8.Canvas(size, size)
    cx = cy = size / 2
    dx, dy = np.sin(heading), -np.cos(heading)
    tail_x, tail_y = cx - dx * 4, cy - dy * 4
    length = 6
    d = cv.seg(tail_x, tail_y, tail_x - dx * length, tail_y - dy * length)
    along = np.clip(((tail_x - cv.x) * dx + (tail_y - cv.y) * dy) / length, 0, 1)
    cv.add(v8.PS[3], v8.gauss(d, 1.5 * (1 - 0.5 * along)) * (1 - along) * 1.2)
    cv.add(v8.WHITE, v8.gauss(d, 0.6) * (1 - along) ** 2)
    out = cv.image()
    out.alpha_composite(body, ((size - body.width) // 2, (size - body.height) // 2))
    return out


def build():
    for slug, draw in STRAIGHT.items():
        degs = angles(slug)
        frames = artkit.quantize_set([artkit.additive(draw(np.radians(a))) for a in degs], COLOURS)
        write_angles(f"{slug}-shot", dict(zip(degs, frames)))
    lances = artkit.quantize_set([artkit.additive(v8.laser_lance(40, LANCE_WIDTH[lv])) for lv in range(1, 7)],
                                 COLOURS)
    write_angles("lance-laser-shot", dict(zip(range(1, 7), lances)))
    missiles = artkit.quantize_set([missile(TAU * k / MISSILE_HEADINGS) for k in range(MISSILE_HEADINGS)], COLOURS)
    artkit.write_frames("micro-missile-pod-shot", missiles, SOURCE)
    artkit.write_frames("bomb-rack-shot", [v8._obj(v8.bomb_model, 12, 2.0)], SOURCE, single=True)
    artkit.write_frames("hammer-mortar-shot", [v8._obj(v8.shell_model, 10, 1.7)], SOURCE, single=True)
    for kind in ("ballistic", "launcher"):
        frames = [artkit.additive(f) if kind == "ballistic" else f for f in v8.muzzle_frames(kind)]
        artkit.write_frames(f"{kind}-muzzle", artkit.quantize_set(frames, COLOURS), SOURCE)
    artkit.write_frames("ballistic-impact",
                        artkit.quantize_set([artkit.additive(f) for f in v8.impact_frames("ballistic")], COLOURS),
                        SOURCE)
    artkit.write_frames("explosive-impact",
                        artkit.quantize_set([artkit.additive(f) for f in v8.impact_frames("explosive")], COLOURS),
                        SOURCE)


def write_angles(name, frames):
    """``name_<index>.png`` for each index (an angle in degrees or a level), replacing the old set."""
    for old in artkit.SPRITES.glob(f"{name}_*.png"):
        old.unlink()
    for index, frame in frames.items():
        artkit.save_png(frame, artkit.SPRITES / f"{name}_{index}.png", SOURCE)


def indexed(name):
    files = sorted(artkit.SPRITES.glob(f"{name}_*.png"), key=lambda p: int(p.stem.rsplit("_", 1)[1]))
    return [Image.open(p).convert("RGBA") for p in files]


def review():
    artkit.REVIEW_ROUND = REVIEW_ROUND
    rows = [(f"{slug.upper()} SHOTS, EVERY ANGLE", indexed(f"{slug}-shot"), 4, True) for slug in STRAIGHT]
    rows += [
        ("LANCE LASER L1-L5 + OVERDRIVE", indexed("lance-laser-shot"), 4, True),
        ("MICRO-MISSILE, 32 HEADINGS", artkit.load_frames("micro-missile-pod-shot"), 3, False),
        ("BOMB, SHELL", artkit.load_frames("bomb-rack-shot") + artkit.load_frames("hammer-mortar-shot"), 6, False),
        ("BALLISTIC MUZZLE, IMPACT", artkit.load_frames("ballistic-muzzle") + artkit.load_frames("ballistic-impact"),
         6, True),
        ("LAUNCHER MUZZLE", artkit.load_frames("launcher-muzzle"), 6, False),
        ("EXPLOSIVE IMPACT", artkit.load_frames("explosive-impact"), 6, True),
    ]
    sheet = artkit.review_sheet("ACT 1 ARSENAL - FINAL EFFECTS", rows, batch=BATCH)
    artkit.save_review(sheet, flight_gif(), WEAPONS / "concept", "weapons", fps=20)


def flight_gif():
    """The ship firing a fan, pods and side guns in a loop, one game step per frame (enlarged 3x)."""
    ship = artkit.load_frames("ship")[2]
    shots = {slug: {int(p.stem.rsplit("_", 1)[1]): Image.open(p).convert("RGBA")
                    for p in artkit.SPRITES.glob(f"{slug}-shot_*.png")} for slug in STRAIGHT}
    missiles = artkit.load_frames("micro-missile-pod-shot")
    w, h = 200, 240
    ox, oy = w // 2 - 24, h - 60
    gif = []
    for i in range(36):
        img = Image.new("RGBA", (w, h), artkit.PLATE)
        img.alpha_composite(ship, (ox, oy))
        cx, cy = ox + 24, oy + 24
        for k in range(4):
            t = (i % 9 + 9 * k) / 60
            for a in (348, 0, 12):
                sp = shots["scatter-vulcan"][a]
                r = 700 * t
                x = cx + np.sin(np.radians(a)) * r - sp.width / 2
                y = cy - 21 - np.cos(np.radians(a)) * r - sp.height / 2
                img = artkit.add_light(img, sp, (int(x), int(y)))
        for k in range(3):
            t = (i % 12 + 12 * k) / 60
            for side, a in ((-16, 2), (16, 358)):
                sp = shots["autocannon-pod"][a]
                img = artkit.add_light(img, sp, (int(cx + side - sp.width / 2), int(cy + 3 - 1000 * t - sp.height / 2)))
        t = (i % 10) / 60
        for a, s in ((90, 1), (270, -1)):
            sp = shots["side-splitter"][a]
            img = artkit.add_light(img, sp, (int(cx + s * (6 + 800 * t) - sp.width / 2), int(cy + 3 - sp.height / 2)))
        k = (i * 2) % MISSILE_HEADINGS
        m = missiles[k]
        img.alpha_composite(m, (w - 40, 20))
        gif.append(sprite.enlarge(img, 3))
    return gif


if __name__ == "__main__":
    if "--review" not in sys.argv[1:]:
        build()
    review()
