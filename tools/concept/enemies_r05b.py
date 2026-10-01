#!/usr/bin/env python3
"""Concept round 05 follow-up - animal-like Vrell ground beasts.

The user found the round 05 ground units (Warden Tank, Strider) too mechanical for the Vrell;
those stay Ascendancy. These two give the Vrell ground units that look like animals:

  ground/concept/ravager-r05-a.{png,gif}     pack hunter: 8-frame gallop at 16 headings, pounce
  ground/concept/shellback-r05-a.{png,gif}   armoured beast: 6-frame walk, spore mortar, curls
                                             into a ball and rolls, then uncurls
  concept/size-lineup-r05-b.png              the round 05 size lineup with both beasts added

Models: render/beast_models.py (rendered with enemy_rigs.ModelSpaceAngleSprites so seams and
plates turn with the body). Sheet layout and helpers are shared with enemies_r05.py.
Run: python3 tools/concept/enemies_r05b.py [ravager] [shellback] [size-lineup]
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
import enemies_r03 as e3  # noqa: E402
import enemies_r05 as r5  # noqa: E402
from render import beast_models as bm  # noqa: E402
from render import enemy_rigs as rig  # noqa: E402
from render import raster, sdf, sprite  # noqa: E402
from render.config import ROOT  # noqa: E402

FW, FH, TAU = r5.FW, r5.FH, 2 * np.pi
SHOTS = e3.SHOTS


def source_panel(make, heading=np.pi / 2, disp=260):
    """Like enemies_r05.source_panel, with model-space patterns."""
    scene, mats = make()
    r = rig.model_rotation(heading)
    hi = sdf.render(lambda p: scene(sdf.rotate_z(p, r)), rig.model_space_materials(mats, r),
                    (disp, disp), 2.3)
    cell = e3.checker(disp, disp)
    cell.alpha_composite(sprite.to_image(hi))
    return cell


def shadow_at(img, sp, x, y, dx, dy, opacity=0.6):
    sh = sprite.shadow_of(sp, opacity=opacity, blur=1.2)
    sprite.paste_center(img, sh, x + dx, y + dy)


# --------------------------------------------------------------------------- Ravager

RAV_N = 56
RAV_PHASES = 8
RAV_STRIDE = 46.0                  # px travelled per gallop cycle
rav = rig.ModelSpaceAngleSprites(lambda phase=0.0, pose="run": bm.ravager(phase, pose),
                                 RAV_N, 16, factor=5, colors=28)
# leap frames rendered on larger canvases at the same extent = the beast drawn bigger
rav_leap = {n: rig.ModelSpaceAngleSprites(lambda: bm.ravager(0.0, "leap"), n, 16, factor=4,
                                          colors=28) for n in (64, 72, 80)}
LEAP_T = 0.75


def rav_run(heading, dist):
    k = int(dist / RAV_STRIDE * RAV_PHASES) % RAV_PHASES
    return rav.get(heading, phase=k * TAU / RAV_PHASES, pose="run")


class Ravager:
    """One pack member: runs along ``path`` until ``pounce`` (t0, target), leaps there in
    LEAP_T seconds, then runs along ``after``."""

    def __init__(self, path, pounce=None, after=None):
        self.path, self.pounce, self.after = path, pounce, after
        self.dist = 0.0
        self.prev = None

    def state(self, t):
        if self.pounce and t >= self.pounce[0]:
            t0, (tx, ty) = self.pounce
            if t < t0 + LEAP_T:
                sx, sy = self.path(t0)
                s = (t - t0) / LEAP_T
                x, y = sx + (tx - sx) * s, sy + (ty - sy) * s
                return x, y, float(np.arctan2(ty - sy, tx - sx)), np.sin(np.pi * s)
            return (*self.after(t), rig.path_heading(self.after, t), 0.0)
        return (*self.path(t), rig.path_heading(self.path, t), 0.0)

    def draw(self, img, t):
        x, y, h, lift = self.state(t)
        if self.prev is not None:
            self.dist += np.hypot(x - self.prev[0], y - self.prev[1])
        self.prev = (x, y)
        if not (-60 < x < FW + 60 and -60 < y < FH + 60):
            return
        if lift > 0.02:
            n = 64 if lift < 0.4 else (72 if lift < 0.8 else 80)
            sp = rav_leap[n].get(h)
            shadow_at(img, rav.get(h, phase=0.0, pose="run"), x, y, 6 + 26 * lift,
                      8 + 36 * lift, 0.6 - 0.25 * lift)
            sprite.paste_center(img, sp, x, y)
        else:
            sp = rav_run(h, self.dist)
            shadow_at(img, sp, x, y, 6, 8)
            sprite.paste_center(img, sp, x, y)


def ravager():
    bg = e3.regolith(FW, FH, 121, craters=9)
    cr = rig.catmull_rom
    pack = [
        Ravager(cr([(-60, 40), (80, 90), (200, 160), (290, 230)], [0.0, 0.8, 1.6, 2.3]),
                (2.3, (r5.player_at(3.05)[0], 452)),
                cr([(r5.player_at(3.05)[0], 452), (330, 540), (400, 640)], [3.05, 3.6, 4.2])),
        Ravager(cr([(-60, 100), (60, 160), (160, 240), (200, 300)], [0.25, 1.05, 1.85, 2.6]),
                (2.6, (r5.player_at(3.35)[0] - 36, 460)),
                cr([(r5.player_at(3.35)[0] - 36, 460), (150, 560), (90, 660)], [3.35, 3.9, 4.5])),
        Ravager(cr([(-60, 160), (90, 240), (260, 300), (420, 280), (560, 200)],
                   [0.5, 1.4, 2.4, 3.3, 4.2])),
        Ravager(cr([(-60, 210), (80, 310), (240, 370), (400, 340), (560, 270)],
                   [0.9, 1.8, 2.8, 3.7, 4.6])),
        Ravager(cr([(560, 50), (400, 110), (250, 150), (100, 130), (-60, 90)],
                   [3.6, 4.4, 5.2, 6.0, 6.8])),
        Ravager(cr([(560, 110), (410, 170), (260, 220), (120, 210), (-60, 170)],
                   [3.9, 4.7, 5.5, 6.3, 7.1])),
    ]
    frames = []
    for t in r5.times(7.2):
        f = bg.copy()
        # draw grounded members first, leapers on top
        order = sorted(pack, key=lambda r: r.state(t)[3])
        for r in order:
            r.draw(f, t)
        r5.draw_player(f, t)
        frames.append(f)
    angles = [rav.frame(k, phase=0.0, pose="run") for k in range(16)]
    cycle = [rav.get(np.pi / 2, phase=k * TAU / RAV_PHASES, pose="run") for k in range(RAV_PHASES)]
    leap = Image.new("RGBA", (320, 130), (22, 24, 34, 255))
    for i, (n, lift) in enumerate(((56, 0.0), (64, 0.3), (72, 0.6), (80, 1.0))):
        sp = rav.get(np.pi / 2, phase=0.0) if n == 56 else rav_leap[n].get(np.pi / 2)
        x = 40 + i * 78
        shadow_at(leap, rav.get(np.pi / 2, phase=0.0), x, 60, 6 + 26 * lift, 8 + 36 * lift,
                  0.6 - 0.25 * lift)
        sprite.paste_center(leap, sp, x, 60)
    panels = [("SOURCE, 260 PX", source_panel(lambda: bm.ravager(0.8, "run"))),
              ("16 HEADINGS (1X)", r5.grid(angles, 8, 1)),
              ("GALLOP: 8 FRAMES (2X)", r5.grid(cycle, 8, 2)),
              ("POUNCE 1X: GROUND -> APEX (BIGGER, SHADOW PUSHED AWAY)", leap)]
    notes = ["RAVAGER - VRELL PACK HUNTER (RUST CHITIN, TEAL GLOW: FAST ATTACKER, CONTACT)",
             "56 PX FOUR-LEGGED BEAST OF CHITIN AND SINEW: DEEP CHEST, LONG BALANCING TAIL, HINGED",
             "JAWS AROUND A GLOWING TEAL MAW. 8-FRAME ROTARY GALLOP AT 16 HEADINGS, PHASE DRIVEN BY",
             "DISTANCE RUN; PACKS OF 3-5 TURN SMOOTHLY ALONG THEIR PATHS. POUNCE: A 0.75 S LEAP TO",
             "LOW-AIR AT THE PLAYER'S GROUND POSITION - DRAWN 14-43 % BIGGER WITH THE SHADOW PUSHED",
             "AWAY, THEN IT LANDS AND RUNS ON. THE MAW IS THE WEAK POINT."]
    return r5.build_sheet("RAVAGER - VRELL PACK HUNTER", panels, notes, frames[58]), frames


# --------------------------------------------------------------------------- Shellback

SB_N = 112
SB_PHASES = 6
SB_STRIDE = 30.0
SB_R = 0.7 * SB_N / 2.3             # ball radius in px (for the roll angle)
sb = rig.ModelSpaceAngleSprites(
    lambda phase=0.0, pose="walk", roll=0.0, pulse=1.0: bm.shellback(phase, pose, roll, pulse),
    SB_N, 16, factor=3, colors=40)
ROLL_STEP = TAU / 5 / 8             # 8 roll frames cover one band pair (72 deg)


def sb_sprite(h, pose, dist, roll=0.0, pulse=1.0):
    if pose == "ball":
        k = int(round((roll % (TAU / 5)) / ROLL_STEP)) % 8
        return sb.get(h, pose="ball", roll=k * ROLL_STEP)
    if pose == "tuck":
        return sb.get(h, pose="tuck")
    k = int(dist / SB_STRIDE * SB_PHASES) % SB_PHASES
    return sb.get(h, phase=k * TAU / SB_PHASES, pose="walk", pulse=round(pulse, 1))


SB_PATH = rig.catmull_rom([(150, -90), (200, 40), (300, 140), (300, 230), (220, 300),
                           (140, 380), (180, 470), (320, 560), (420, 680)],
                          [0.0, 2.0, 4.0, 5.2, 6.0, 6.8, 7.6, 9.4, 11.4])
CURL = (5.0, 5.3, 7.7, 8.0)         # tuck, roll from, roll until, uncurl done


def sb_pose(t):
    if CURL[0] <= t < CURL[1] or CURL[2] <= t < CURL[3]:
        return "tuck"
    if CURL[1] <= t < CURL[2]:
        return "ball"
    return "walk"


def shellback():
    bg = e3.regolith(FW, FH, 133, ramp_name="MARS", craters=7)
    rng = np.random.default_rng(4)
    # ground targets on the rolling route: small Vrell-crushable rocks/wrecks
    targets = [(*SB_PATH(tt), rng.uniform(10, 14)) for tt in (5.6, 6.2, 6.7, 7.2)]
    crushed = set()
    shots = []                     # (t_launch, sx, sy, tx, ty)
    for t0 in (1.2, 3.0, 9.0, 10.6):
        x, y = SB_PATH(t0)
        h = rig.path_heading(SB_PATH, t0)
        vx, vy = rig.local_to_screen(0, -0.3, h, SB_N / 2.3)
        px, py = r5.player_at(t0 + 1.2)
        shots.append((t0, x + vx, y + vy, px, py - 10))
    frames, dist, roll, prev = [], 0.0, 0.0, None
    for t in r5.times(11.4):
        f = bg.copy()
        x, y = SB_PATH(t)
        h = rig.path_heading(SB_PATH, t)
        pose = sb_pose(t)
        if prev is not None:
            step = np.hypot(x - prev[0], y - prev[1])
            dist += step
            if pose == "ball":
                roll += step / SB_R
        prev = (x, y)
        d = ImageDraw.Draw(f)
        for i, (tx, ty, r) in enumerate(targets):
            if pose == "ball" and np.hypot(tx - x, ty - y) < SB_R:
                crushed.add(i)
            if i in crushed:
                d.ellipse([tx - r - 4, ty - r * 0.7 - 2, tx + r + 4, ty + r * 0.7 + 2],
                          fill=(70, 20, 6, 255))
                for a in range(5):
                    ang = a * 1.3 + i
                    d.point((tx + np.cos(ang) * (r + 6), ty + np.sin(ang) * (r + 4)),
                            fill=(150, 110, 90, 255))
            else:
                d.ellipse([tx - r + 3, ty - r * 0.8 + 4, tx + r + 3, ty + r * 0.8 + 4],
                          fill=(60, 16, 4, 255))
                d.ellipse([tx - r, ty - r * 0.8, tx + r, ty + r * 0.8], fill=(150, 80, 56, 255),
                          outline=(40, 12, 6, 255))
                d.ellipse([tx - r * 0.5, ty - r * 0.6, tx, ty - r * 0.1], fill=(180, 110, 80, 255))
        # mortar: vent swells before each launch
        pulse = 1.0
        for t0, sx, sy, tx, ty in shots:
            if t0 - 0.5 < t < t0:
                pulse = 1.0 + 0.35 * (1 - (t0 - t) / 0.5)
        sp = sb_sprite(h, pose, dist, roll, pulse)
        e3.place(f, sp, x, y, "ground")
        if pose == "ball" and int(t * 20) % 2 == 0:
            for k in range(3):     # dust behind the rolling ball
                bx, by = rig.local_to_screen(rng.uniform(-0.3, 0.3), -1.0 - 0.2 * k, h, SB_N / 2.3)
                raster.add_light(f, x + bx, y + by, 6, (60, 30, 20), 0.6)
        for t0, sx, sy, tx, ty in shots:
            if t0 <= t < t0 + 1.2:          # marker 1.2 s ahead, arcing blob
                s = (t - t0) / 1.2
                dd = ImageDraw.Draw(f)
                dd.ellipse([tx - 16, ty - 10, tx + 16, ty + 10], outline=SHOTS[5] + (255,), width=1)
                dd.ellipse([tx - 3, ty - 2, tx + 3, ty + 2], outline=SHOTS[5] + (255,))
                bx, by = sx + (tx - sx) * s, sy + (ty - sy) * s - np.sin(np.pi * s) * 90
                e3.orb(f, bx, by, int(4 + 4 * np.sin(np.pi * s)), ring=SHOTS[5])
            elif t0 + 1.2 <= t < t0 + 1.5:  # small ring burst on impact
                s = (t - t0 - 1.2) / 0.3
                for a in np.linspace(0, TAU, 8, endpoint=False):
                    e3.orb(f, tx + np.cos(a) * 30 * s, ty + np.sin(a) * 22 * s, 3, ring=SHOTS[5])
        r5.draw_player(f, t)
        frames.append(f)
    angles = [sb.frame(k, phase=0.0, pose="walk", pulse=1.0) for k in range(16)]
    cycle = [sb.get(np.pi / 2, phase=k * TAU / SB_PHASES, pose="walk", pulse=1.0)
             for k in range(SB_PHASES)]
    curl = [sb.get(np.pi / 2, phase=0.0, pose="walk", pulse=1.0), sb.get(np.pi / 2, pose="tuck")] + \
           [sb.get(np.pi / 2, pose="ball", roll=k * ROLL_STEP * 2) for k in range(4)]
    panels = [("SOURCE, 260 PX", source_panel(lambda: bm.shellback(0.5, "walk"))),
              ("16 HEADINGS (1X)", r5.grid(angles, 8, 1)),
              ("WALK: 6 FRAMES (1X)", r5.grid(cycle, 6, 1)),
              ("CURL: WALK, TUCK, BALL ROLLING (1X)", r5.grid(curl, 6, 1))]
    notes = ["SHELLBACK - LARGE ARMOURED VRELL BEAST (SLATE SCUTES, LIME GLOW: HEAVY GROUND, SPORES)",
             "112 PX TORTOISE/ARMADILLO BEAST: DOMED SHELL OF OVERLAPPING SCUTES OVER LIME-VEINED HIDE,",
             "ELEPHANTINE CLAWED FEET, BEAKED HEAD. HEAVY 6-FRAME DIAGONAL GAIT AT 16 HEADINGS. ITS BACK",
             "VENT SWELLS AND LOBS SPORE BLOBS IN AN ARC; THE IMPACT IS MARKED 1.2 S AHEAD. BADLY",
             "DAMAGED IT TUCKS IN, CURLS INTO A BANDED BALL AND ROLLS ALONG ITS PATH CRUSHING GROUND",
             "TARGETS, THEN UNCURLS. WEAK POINTS: THE VENT, AND THE BELLY WHILE IT UNCURLS."]
    return r5.build_sheet("SHELLBACK - ARMOURED VRELL BEAST THAT CURLS AND ROLLS", panels, notes,
                          frames[130]), frames


# --------------------------------------------------------------------------- lineup + main

def size_lineup_b():
    extra = [("RAVAGER", rav.get(np.pi / 2, phase=0.8, pose="run")),
             ("SHELLBACK", sb.get(np.pi / 2, phase=0.0, pose="walk", pulse=1.0)),
             ("CURLED BALL", sb.get(np.pi / 2, pose="ball", roll=0.0))]
    return r5.size_lineup(extra, "SIZE LINEUP - ROUND 05 + VRELL BEASTS (RAVAGER, SHELLBACK), 1X")


def main(args):
    if not args or "ravager" in args:
        sheet, frames = ravager()
        r5.save_unit("ground", "ravager", sheet, frames)
    if not args or "shellback" in args:
        sheet, frames = shellback()
        r5.save_unit("ground", "shellback", sheet, frames)
    if not args or "size-lineup" in args:
        p = r5.out(None, "size-lineup-r05-b.png")
        size_lineup_b().convert("RGB").save(p, optimize=True)
        print("wrote", p.relative_to(ROOT))


if __name__ == "__main__":
    main(sys.argv[1:])
