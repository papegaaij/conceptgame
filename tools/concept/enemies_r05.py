#!/usr/bin/env python3
"""Concept round 05 - new enemy archetypes: segmented, huge, walking, spinning, swarming.

Each unit gets a PNG sheet (source render, native sprite(s), parts breakdown, angle sheet where
relevant, animation frames, a frame from the play field) and a GIF of it moving in the 480x540
play field with the player for scale. Turning units use pre-rendered headings (16, or 32 for
large and slow ones) and the nearest frame is picked, as in the game. Colours follow the role
colours of round 04 (design/enemies/README.md "Role colours (draft)").

Outputs (design/enemies/...):
  air/concept/coilwyrm-r05-a.{png,gif}        Vrell serpent, 11 segments following the head
  space/concept/leviathan-r05-a.{png,gif}     Vrell whale, articulated tail and fins, turrets
  ground/concept/scuttler-r05-a.{png,gif}     Vrell six-legged walker, 16 angles x 6 phases
  air/concept/whirl-seed-r05-a.{png,gif}      spinning seed pods, spiralling clusters
  air/concept/mote-swarm-r05-a.{png,gif}      boids swarm that loops round to attack from behind
  ground/concept/warden-tank-r05-a.{png,gif}  Ascendancy tank, hull 16 / turret 32 angles
  ground/concept/strider-r05-a.{png,gif}      Ascendancy biped walker, aiming torso
  air/concept/buzzsaw-drone-r05-a.{png,gif}   Ascendancy spinning blade drone, ricochets
  air/concept/rail-serpent-r05-a.{png,gif}    Ascendancy segmented drone train
  concept/size-lineup-r05-a.png               everything at native scale, Mote to Leviathan

Models: render/archetype_models.py; rigs: render/enemy_rigs.py.
Run: python3 tools/concept/enemies_r05.py [name ...]   (no args = everything, ~15 min)
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
import enemies_r03 as e3  # noqa: E402
import enemies_r04 as e4  # noqa: E402  (role-colour models of round 04, rim light)
from render import archetype_models as am  # noqa: E402
from render import enemy_rigs as rig  # noqa: E402
from render import raster, sprite  # noqa: E402
from render.config import ROOT  # noqa: E402

DESIGN = ROOT / "design" / "enemies"
FW, FH = 480, 540
FPS = 20
SUB = "CONCEPT ROUND 05 - 960X540"
TAU = 2 * np.pi
SHOTS = e3.SHOTS
GOLD = e3.GOLD
rim = e4.rim


# --------------------------------------------------------------------------- shared bits

def player_at(t):
    return 240 + 70 * np.sin(t * 0.9), 488


def draw_player(img, t, shadow="air", shots=True):
    px, py = player_at(t)
    e3.place(img, e3.player_sprite(), px, py, shadow)
    if shots:
        for k in range(4):
            y = py - 40 - ((t * 420 + k * 105) % 420)
            if y > -10:
                e3.bolt(img, px - 10, y)
                e3.bolt(img, px + 10, y)
    return px, py


def paste(img, sp, x, y, shadow=None):
    if shadow:
        e3.place(img, sp, x, y, shadow)
    else:
        sprite.paste_center(img, sp, x, y)


def ghost(sp, alpha):
    a = sp.copy()
    a.putalpha(a.getchannel("A").point(lambda v: int(v * alpha)))
    return a


def coast(seed):
    from render import terrain
    from render.palette import B
    stops = terrain.coast_stops_from(B["EARTH ORBIT"], B["EARTH SURFACE"])
    return terrain.recede(terrain.earth_coast(FW, FH, seed=seed, period=False, stops=stops,
                                              cell=128), amount=0.35, darken=0.72)


class Bullets:
    """Straight-flying bullets spawned at given times; drawn with the readability rules."""

    def __init__(self):
        self.list = []          # (t0, x0, y0, vx, vy, kind)

    def fire(self, t0, x, y, tx, ty, speed=170, kind="orb"):
        d = np.hypot(tx - x, ty - y) or 1
        self.list.append((t0, x, y, (tx - x) / d * speed, (ty - y) / d * speed, kind))

    def draw(self, img, t):
        for t0, x, y, vx, vy, kind in self.list:
            if t < t0:
                continue
            bx, by = x + vx * (t - t0), y + vy * (t - t0)
            if -20 < bx < FW + 20 and -20 < by < FH + 20:
                if kind == "orb":
                    e3.orb(img, bx, by, 5)
                elif kind == "acid":
                    e3.orb(img, bx, by, 5, ring=SHOTS[5])
                elif kind == "gold":
                    e3.orb(img, bx, by, 4, ring=GOLD[3])
                elif kind == "shell":
                    e3.orb(img, bx, by, 5, ring=GOLD[3])


def grid(sprites, cols, scale=2, pad=6, labels=None):
    w = max(s.width for s in sprites) * scale
    h = max(s.height for s in sprites) * scale
    rows = (len(sprites) + cols - 1) // cols
    lab = 10 if labels else 0
    img = Image.new("RGBA", (cols * (w + pad), rows * (h + pad + lab)), raster.SHEET_BG)
    for i, s in enumerate(sprites):
        x, y = (i % cols) * (w + pad), (i // cols) * (h + pad + lab)
        cell = e3.checker(w, h)
        sprite.paste_center(cell, sprite.enlarge(s, scale), w / 2, h / 2)
        img.alpha_composite(cell, (x, y))
        if labels:
            raster.draw_text(img, x, y + h + 2, labels[i], raster.LABEL_DIM)
    return img


def source_panel(make, n, heading=np.pi / 2, extent=2.3, disp=260):
    scene, mats = make()
    from render import sdf
    r = rig.model_rotation(heading)
    hi = sdf.render(lambda p: scene(sdf.rotate_z(p, r)), mats, (disp, disp), extent)
    cell = e3.checker(disp, disp)
    cell.alpha_composite(sprite.to_image(hi))
    return cell


def build_sheet(title, panels, notes, game_frame, width=1300):
    """Flow ``panels`` [(label, image)] into the left area; the play-field frame (1x) sits
    at the right; notes underneath the panels."""
    gx = width - FW - 16
    x, y, row_h = 16, 50, 0
    placed = []
    avail = gx - 40
    fitted = []
    for lab, im in panels:
        if im.width > avail:        # never overlap the play-field frame
            k = avail / im.width
            im = im.resize((avail, int(im.height * k)), Image.LANCZOS)
            lab = lab + f" (SHOWN AT {k:.2f})"
        fitted.append((lab, im))
    for lab, im in fitted:
        if x + im.width > gx - 16 and x > 16:
            x, y, row_h = 16, y + row_h + 26, 0
        placed.append((x, y, lab, im))
        x += im.width + 18
        row_h = max(row_h, im.height)
    notes_y = y + row_h + 26
    height = max(notes_y + 14 * len(notes) + 20, 50 + FH + 30)
    img = raster.sheet(width, height, title, SUB)
    for px, py, lab, im in placed:
        raster.draw_text(img, px, py - 12, lab, raster.LABEL_DIM)
        img.alpha_composite(im, (px, py))
    for i, line in enumerate(notes):
        raster.draw_text(img, 16, notes_y + i * 14, line, raster.ACCENT if i == 0 else raster.LABEL)
    raster.draw_text(img, gx, 38, "PLAY FIELD 480X540 (1X), FRAME FROM THE GIF", raster.LABEL_DIM)
    img.alpha_composite(game_frame.convert("RGBA"), (gx, 50))
    return img


def out(category, name):
    d = DESIGN / category / "concept" if category else DESIGN / "concept"
    d.mkdir(parents=True, exist_ok=True)
    return d / name


def save_unit(category, slug, sheet, frames):
    png = out(category, f"{slug}-r05-a.png")
    sheet.convert("RGB").save(png, optimize=True)
    size = rig.write_gif(frames, out(category, f"{slug}-r05-a.gif"), fps=FPS)
    print(f"wrote {png.relative_to(ROOT)} and .gif ({size / 1e6:.1f} MB, {len(frames)} frames)")


def times(seconds):
    return [i / FPS for i in range(int(seconds * FPS))]


# --------------------------------------------------------------------------- 1 Coilwyrm

COIL_SIZES = [58] + [54, 52, 50, 48, 46, 44, 42, 39, 36, 33, 30, 27] + [32]   # head, 12 segs, tail
coil_head = rig.AngleSprites(lambda jaw=0.0: am.coil_head(jaw), COIL_SIZES[0], 16, colors=32)
coil_segs = {n: rig.AngleSprites(lambda: am.coil_segment(), n, 16) for n in set(COIL_SIZES[1:-1])}
coil_tail = rig.AngleSprites(lambda: am.coil_tail(), COIL_SIZES[-1], 16)


def coil_path(t):
    cx = 240 + 60 * np.sin(0.5 * t)
    cy = -150 + 72 * t
    return cx + 130 * np.cos(1.9 * t + 2.0), cy + 115 * np.sin(1.9 * t + 2.0)


def coil_draw(img, t, path=coil_path, shadow=None):
    spac = [0.5 * (COIL_SIZES[i] + COIL_SIZES[i + 1]) / 2 for i in range(len(COIL_SIZES) - 1)]
    pts, heads = rig.chain_at(path, t, spac)
    order = list(range(len(pts)))[::-1]
    for i in order:
        x, y = pts[i]
        if i == 0:
            sp = coil_head.get(heads[0], jaw=round(0.5 + 0.5 * np.sin(t * 6), 1))
        elif i == len(COIL_SIZES) - 1:
            sp = coil_tail.get(heads[i])
        else:
            sp = coil_segs[COIL_SIZES[i]].get(heads[i])
        paste(img, sp, x, y, shadow)
    return pts


def coilwyrm():
    bg = e3.orbit(FW, FH, 21)
    frames = []
    for t in times(8.0):
        f = bg.copy()
        coil_draw(f, t + 2.2)
        draw_player(f, t, "orbit")
        frames.append(f)
    heads = [coil_head.frame(k, jaw=0.5) for k in range(16)]
    segs = [coil_segs[44].frame(k) for k in range(16)]
    parts = [coil_head.get(np.pi / 2, jaw=0.0), coil_head.get(np.pi / 2, jaw=1.0)] + \
            [coil_segs[n].get(np.pi / 2) for n in (54, 44, 36, 27)] + [coil_tail.get(np.pi / 2)]
    chain = Image.new("RGBA", (440, 210), (22, 24, 34, 255))
    coil_draw(chain, 6.0, path=lambda tt: (60 * tt + 50, 105 + 55 * np.sin(tt * 1.5)))
    panels = [("SOURCE: HEAD (JAW OPEN), 260 PX", source_panel(lambda: am.coil_head(1.0), 44)),
              ("PARTS 2X: HEAD JAW 0/1, SEGMENTS 54/44/36/27, TAIL",
               grid(parts, 4, 2, labels=["HEAD", "JAW", "SEG54", "SEG44", "SEG36", "SEG27", "TAIL"])),
              ("HEAD: 16 HEADINGS (1X)", grid(heads, 8, 1)),
              ("SEGMENT: 16 HEADINGS (1X)", grid(segs, 8, 1)),
              ("CHAIN 1X: SEGMENTS FOLLOW THE HEAD'S PATH", chain)]
    notes = ["COILWYRM - VRELL SERPENT (RUST CHITIN, TEAL GLOW: FAST ATTACKER, CONTACT)",
             "HEAD 58 PX + 12 OVERLAPPING SEGMENTS (54 -> 27 PX) + TAIL; ABOUT 330 PX LONG ALONG ITS PATH.",
             "EACH SEGMENT SITS ON THE PATH THE HEAD ALREADY TRAVELLED AND TURNS TO ITS TANGENT",
             "(16 PRE-RENDERED HEADINGS PER PART, NEAREST FRAME). THE HEAD IS THE WEAK POINT; SEGMENTS",
             "ABSORB SHOTS. MOVEMENT: LOOPING SWIRL (PROLATE CYCLOID) DOWN THE SCREEN, JAW SNAPS."]
    return build_sheet("COILWYRM - SEGMENTED VRELL SERPENT", panels, notes, frames[70]), frames


# --------------------------------------------------------------------------- 2 Leviathan

S = am.LEVIATHAN_S
lev_body = rig.AngleSprites(lambda: am.leviathan_body(), 360, 32, extent=3.6, factor=2, colors=48)
lev_tail = [rig.AngleSprites(lambda i=i: am.leviathan_tail(i), 130, 32, extent=1.3, factor=3,
                             colors=32) for i in range(3)]
lev_fluke = rig.AngleSprites(lambda: am.leviathan_fluke(), 180, 32, extent=1.8, factor=3,
                             colors=32)
lev_fin = {s: rig.AngleSprites(lambda s=s: am.leviathan_fin(s), 230, 32, extent=2.3, factor=2,
                               colors=32) for s in (1, -1)}
LEV_H = 1.2                      # heading of the drift (radians, screen): down and to the right


def lev_draw(img, cx, cy, t, h=LEV_H, bullets=None, marks=False):
    """Composite the Leviathan rig centred at (cx, cy)."""
    flap = 0.32 * np.sin(2.0 * t + 1.0)
    # fins (under the body)
    for side in (1, -1):
        px, py = rig.local_to_screen(side * am.LEV_FIN_PIVOT[0], am.LEV_FIN_PIVOT[1], h, S)
        paste(img, lev_fin[side].get(h + side * flap), cx + px, cy + py)
    # tail chain (under the body)
    jx, jy = rig.local_to_screen(0, am.LEV_TAIL_PIVOT, h, S)
    jx, jy, hh = cx + jx, cy + jy, h
    joints = [(jx, jy)]
    for i, (ln, _, _) in enumerate(am.LEV_TAIL):
        hh = hh + 0.2 * np.sin(1.8 * t - 0.9 * i)
        paste(img, lev_tail[i].get(hh), jx, jy)
        dx, dy = rig.local_to_screen(0, -ln, hh, S)
        jx, jy = jx + dx, jy + dy
        joints.append((jx, jy))
    hh = hh + 0.25 * np.sin(1.8 * t - 2.7)
    paste(img, lev_fluke.get(hh), jx, jy)
    paste(img, lev_body.get(h), cx, cy)
    turrets = [(cx + a, cy + b) for a, b in
               (rig.local_to_screen(x, y, h, S) for x, y in am.LEV_TURRETS)]
    if marks:
        d = ImageDraw.Draw(img)
        for x, y in joints + [(cx + rig.local_to_screen(s * am.LEV_FIN_PIVOT[0],
                                                        am.LEV_FIN_PIVOT[1], h, S)[0],
                               cy + rig.local_to_screen(s * am.LEV_FIN_PIVOT[0],
                                                        am.LEV_FIN_PIVOT[1], h, S)[1])
                              for s in (1, -1)]:
            d.ellipse([x - 4, y - 4, x + 4, y + 4], outline=(255, 170, 60, 255), width=2)
        for x, y in turrets:
            d.rectangle([x - 5, y - 5, x + 5, y + 5], outline=(255, 255, 255, 255))
    return turrets


def leviathan():
    bg = e3.orbit(FW, FH, 33)
    T = 8.0
    start, end = np.array([40.0, -300.0]), np.array([420.0, 740.0])
    center = lambda t: start + (end - start) * (t / T)
    bullets = Bullets()
    for k, t0 in enumerate(np.arange(0.6, T, 0.7)):
        cx, cy = center(t0 + 1.0)
        lx, ly = am.LEV_TURRETS[k % 4]
        ox, oy = rig.local_to_screen(lx, ly, LEV_H, S)
        px, py = player_at(t0)
        bullets.fire(t0, cx + ox, cy + oy, px, py, 150)
    frames = []
    for t in times(T):
        f = bg.copy()
        cx, cy = center(t + 1.0)
        lev_draw(f, cx, cy, t)
        bullets.draw(f, t)
        draw_player(f, t, "orbit")
        frames.append(f)
    parts = [lev_body.get(np.pi / 2)]
    small = [lev_tail[i].get(np.pi / 2) for i in range(3)] + [lev_fluke.get(np.pi / 2),
                                                               lev_fin[1].get(np.pi / 2),
                                                               lev_fin[-1].get(np.pi / 2)]
    rigimg = Image.new("RGBA", (380, 640), (22, 24, 34, 255))
    lev_draw(rigimg, 190, 400, 0.4, h=np.pi / 2, marks=True)
    flex = Image.new("RGBA", (360, 200), (22, 24, 34, 255))
    for i, tt in enumerate((0.0, 0.9, 1.8)):
        tmp = Image.new("RGBA", (380, 640), (0, 0, 0, 0))
        lev_draw(tmp, 190, 400, tt, h=np.pi / 2)
        tmp = tmp.resize((104, 175), Image.LANCZOS)
        flex.alpha_composite(tmp, (10 + i * 118, 12))
    panels = [("BODY 1X (360 PX CANVAS, 32 HEADINGS)", grid(parts, 1, 1)),
              ("RIG 1X: PIVOTS (O) AND DORSAL TURRETS ([])", rigimg),
              ("PARTS 1X: TAIL 1-3, FLUKE, FINS R/L", grid(small, 3, 1,
                                                          labels=["T1", "T2", "T3", "FLUKE",
                                                                  "FIN R", "FIN L"])),
              ("TAIL + FIN FLEX OVER TIME (1/3 SCALE)", flex)]
    notes = ["LEVIATHAN - VRELL WHALE (BONE/IVORY HIDE, VIOLET GLOW: HUGE GUNNER)",
             "ABOUT 480 PX LONG WITH THE TAIL, 300 PX ACROSS THE FINS: MORE THAN A THIRD OF THE SCREEN.",
             "RIG: BODY + 3 TAIL SEGMENTS + FLUKE (WAVE DOWN THE CHAIN) + 2 PECTORAL FINS (FLAP), EACH",
             "PART PRE-RENDERED AT 32 HEADINGS. FOUR DORSAL TURRET VENTS FIRE AIMED ORBS; BLOWHOLE =",
             "WEAK POINT. DRIFTS SLOWLY ACROSS THE SCREEN OVER EARTH ORBIT (SPACE LAYER)."]
    return build_sheet("LEVIATHAN - HUGE ARTICULATED VRELL WHALE", panels, notes,
                       frames[80]), frames


# --------------------------------------------------------------------------- 3 Scuttler

SCUT_N = 64
SCUT_PHASES = 6
scut = rig.AngleSprites(lambda phase=0.0: am.scuttler(phase), SCUT_N, 16, factor=5)


def scut_sprite(heading, dist):
    k = int(round(dist / 30.0 * SCUT_PHASES)) % SCUT_PHASES
    return scut.get(heading, phase=k * TAU / SCUT_PHASES)


def scuttler():
    bg = coast(44)
    walkers = []
    for i, (x0, delay, amp) in enumerate(((130, 0.0, 90), (330, 1.4, 110), (220, 3.0, 80))):
        walkers.append((lambda t, x0=x0, d=delay, a=amp:
                        (x0 + a * np.sin(0.9 * (t - d)), -60 + 48 * (t - d))))
    bullets = Bullets()
    for i, t0 in enumerate((2.2, 3.6, 5.0, 6.4)):
        w = walkers[i % 3]
        x, y = w(t0)
        px, py = player_at(t0)
        bullets.fire(t0, x, y, px, py, 140, "acid")
    frames = []
    for t in times(9.0):
        f = bg.copy()
        for w in walkers:
            x, y = w(t)
            if -60 < y < FH + 60:
                h = rig.path_heading(w, t)
                dist = 48 * t * 1.1
                paste(f, scut_sprite(h, dist), x, y, "ground")
        bullets.draw(f, t)
        draw_player(f, t)
        frames.append(f)
    angles = [scut.frame(k, phase=0.0) for k in range(16)]
    cycle = [scut.get(np.pi / 2, phase=k * TAU / SCUT_PHASES) for k in range(SCUT_PHASES)]
    panels = [("SOURCE, 260 PX", source_panel(lambda: am.scuttler(0.6), SCUT_N)),
              ("16 HEADINGS (2X), PHASE 0", grid(angles, 8, 2)),
              ("WALK CYCLE: 6 FRAMES, TRIPOD GAIT (3X)", grid(cycle, 6, 3))]
    notes = ["SCUTTLER - VRELL GROUND WALKER (SLATE CHITIN, LIME GLOW: ROOTED/GROUND, ACID)",
             "64 PX, SIX LEGS IN A TRIPOD GAIT (L1 R2 L3 / R1 L2 R3). 16 HEADINGS X 6 WALK PHASES =",
             "96 FRAMES; THE WALK PHASE ADVANCES WITH DISTANCE WALKED SO FEET DON'T SLIDE. IT TURNS",
             "TO FACE ITS PATH, WEAVES DOWN THE SCREEN AND SPITS ACID BLOBS AT THE PLAYER.",
             "GROUND SCROLL IS LEFT OUT IN THE GIF SO ITS OWN MOTION IS EASY TO JUDGE."]
    return build_sheet("SCUTTLER - SIX-LEGGED VRELL WALKER", panels, notes, frames[90]), frames


# --------------------------------------------------------------------------- 4 Whirl Seed

seed = rig.AngleSprites(lambda: am.whirl_seed(), 26, 40, factor=8, sym=5)


def whirl_seed():
    bg = coast(55)
    clusters = [dict(p=np.array([90.0, -40.0]), v=np.array([150.0, 110.0]), ph=0.0),
                dict(p=np.array([400.0, -160.0]), v=np.array([-120.0, 130.0]), ph=1.3)]
    dt = 1 / FPS
    frames = []
    for t in times(7.0):
        f = bg.copy()
        for c in clusters:
            c["p"] = c["p"] + c["v"] * dt
            for ax, lim in ((0, FW), (1, FH)):
                if (c["p"][ax] < 40 and c["v"][ax] < 0) or (c["p"][ax] > lim - 40 and c["v"][ax] > 0):
                    if ax == 0 or c["p"][1] > 0:
                        c["v"][ax] *= -1
            r = 34 + 16 * np.sin(3.0 * t + c["ph"])
            for k in range(5):
                a = 2.6 * t + c["ph"] + k * TAU / 5
                x, y = c["p"] + r * np.array([np.cos(a), np.sin(a)])
                paste(f, seed.get(9.0 * t + k), x, y, "air")
        draw_player(f, t)
        frames.append(f)
    spin = [seed.frame(k) for k in range(8)]
    panels = [("SOURCE, 260 PX", source_panel(lambda: am.whirl_seed(), 26)),
              ("SPIN: 8 FRAMES COVER 72 DEG (5-FOLD SYMMETRY), 4X", grid(spin, 8, 4))]
    notes = ["WHIRL SEED - TINY SPINNING VRELL SEED POD (PLUM CHITIN, TEAL GLOW: FODDER, CONTACT)",
             "26 PX, RADIALLY SYMMETRIC: NO FRONT OR BACK, SO IT WORKS FROM ANY DIRECTION. SPINS FAST",
             "(8 FRAMES PER 72 DEG). CLUSTERS OF FIVE SPIRAL AROUND A CENTRE THAT BOUNCES OFF THE",
             "PLAY-FIELD EDGES; THE SPIRAL RADIUS BREATHES. A SPREAD WEAPON CLEARS A CLUSTER."]
    return build_sheet("WHIRL SEED - SPINNING SEED-POD CLUSTERS", panels, notes, frames[60]), frames


# --------------------------------------------------------------------------- 5 Mote Swarm

mote = {g: rig.AngleSprites(lambda g=g: am.mote(g), 16, 1, factor=8) for g in (0.8, 1.1, 1.5)}
MOTE_PATH = rig.catmull_rom(
    [(560, 60), (380, 120), (230, 230), (150, 150), (250, 80), (330, 190), (170, 330), (-60, 420),
     (-140, 620), (120, 700), (230, 600), (260, 380), (250, 120), (240, -120)],
    [0.0, 0.7, 1.3, 1.8, 2.3, 2.8, 3.3, 3.8, 4.1, 4.4, 4.7, 5.2, 5.8, 6.6])


def mote_swarm():
    bg = e3.orbit(FW, FH, 66)
    rng = np.random.default_rng(5)
    n = 30
    pos = np.array(MOTE_PATH(0.0)) + rng.normal(0, 30, (n, 2))
    vel = np.zeros((n, 2))
    offs = rng.normal(0, 1, (n, 2))
    dt = 1 / FPS
    frames = []
    for i, t in enumerate(times(7.0)):
        leader = np.array(MOTE_PATH(t))
        target = leader + offs * (26 + 10 * np.sin(t * 2 + np.arange(n))[:, None])
        steer = (target - pos) * 5.5 - vel * 1.8
        diff = pos[:, None, :] - pos[None, :, :]
        dist = np.linalg.norm(diff, axis=-1) + np.eye(n) * 1e6
        sep = (diff / dist[..., None] ** 2 * (dist < 18)[..., None]).sum(axis=1) * 900
        align = (vel.mean(axis=0) - vel) * 0.6
        vel = vel + (steer + sep + align) * dt
        sp = np.linalg.norm(vel, axis=1, keepdims=True)
        vel = np.where(sp > 440, vel / sp * 440, vel)
        pos = pos + vel * dt
        f = bg.copy()
        # edge warning before the rear attack
        m = pos.mean(axis=0)
        offscreen = m[0] < -20 or m[1] > FH + 20
        if t > 3.0 and offscreen and int(t * 8) % 2 == 0:   # returning from behind
            d = ImageDraw.Draw(f)
            for x0 in (120, 240):
                d.polygon([(x0 - 14, FH - 8), (x0 + 14, FH - 8), (x0, FH - 26)],
                          outline=(255, 70, 50, 255), fill=(90, 10, 10, 200))
            raster.draw_text(f, 150, FH - 40, "REAR!", (255, 90, 70))
        for k in range(n):
            g = (0.8, 1.1, 1.5)[(k + i // 2) % 3]
            paste(f, mote[g].frame(0), pos[k, 0], pos[k, 1])
        draw_player(f, t, "orbit")
        frames.append(f)
    flick = [mote[g].frame(0) for g in (0.8, 1.1, 1.5)]
    path_img = Image.new("RGBA", (300, 340), (22, 24, 34, 255))
    d = ImageDraw.Draw(path_img)
    ptsl = [MOTE_PATH(tt) for tt in np.linspace(0, 6.6, 300)]
    sc = 0.45
    d.rectangle([60, 60, 60 + FW * sc, 60 + FH * sc], outline=(90, 100, 130, 255))
    d.line([(60 + x * sc, 60 + y * sc) for x, y in ptsl], fill=(255, 170, 60, 255), width=2)
    raster.draw_text(path_img, 6, 6, "LEADER PATH (PLAY FIELD OUTLINED)", raster.LABEL_DIM)
    raster.draw_text(path_img, 6, 320, "EXITS LEFT, RE-ENTERS FROM BEHIND", raster.LABEL_DIM)
    panels = [("SOURCE, 200 PX", source_panel(lambda: am.mote(1.1), 16, disp=200)),
              ("NATIVE 16 PX, GLOW FLICKER 3 FRAMES (6X)", grid(flick, 3, 6)),
              ("SWARM ROUTE", path_img)]
    notes = ["MOTE SWARM - TINY VRELL FLOCK (RUST HUSK, CRIMSON GLOW: FAST, DIVES IN FROM THE REAR)",
             "16 PX EACH, 30 PER FLOCK. BOIDS STEERING (COHESION TO A LEADER PATH, SEPARATION,",
             "ALIGNMENT) GIVES THE SWIRL. THE FLOCK LOOPS IN FRONT, LEAVES THE SCREEN AT THE LEFT AND",
             "RETURNS FROM BEHIND THE PLAYER; AN EDGE WARNING BLINKS 1.3 S BEFORE (ENEMY RULES).",
             "RADIAL BODY: NO FRONT OR BACK, SO IT READS FROM EVERY DIRECTION."]
    return build_sheet("MOTE SWARM - FLOCK THAT ATTACKS FROM BEHIND", panels, notes,
                       frames[104]), frames


# --------------------------------------------------------------------------- 6 Warden Tank

hull = rig.AngleSprites(lambda tread=0.0: am.warden_hull(tread), 76, 16, factor=5, colors=32,
                        post=rim)
turret = rig.AngleSprites(lambda: am.warden_turret(), 76, 32, factor=5, colors=24, post=rim)
TANK_PATH = rig.catmull_rom([(-80, 120), (80, 150), (220, 210), (330, 170), (420, 240),
                             (380, 360), (240, 380), (120, 330), (40, 420), (-80, 460)],
                            [0, 1.5, 3.0, 4.2, 5.4, 6.6, 7.8, 9.0, 10.2, 11.5])


def tank_draw(img, t, aim):
    x, y = TANK_PATH(t)
    h = rig.path_heading(TANK_PATH, t)
    tr = round((t * 2.2) % 1.0, 1) >= 0.5
    paste(img, hull.get(h, tread=0.5 if tr else 0.0), x, y, "ground")
    ta = np.arctan2(aim[1] - y, aim[0] - x)
    paste(img, turret.get(ta), x, y)
    return x, y, ta


def warden_tank():
    bg = e3.regolith(FW, FH, 77, craters=8)
    d = ImageDraw.Draw(bg)
    for tt in np.linspace(0, 11.5, 900):          # tread marks along the route
        x, y = TANK_PATH(tt)
        h = rig.path_heading(TANK_PATH, tt)
        for side in (-1, 1):
            ox, oy = rig.local_to_screen(side * 0.62, 0, h, 76 / 2.3)
            d.ellipse([x + ox - 2, y + oy - 2, x + ox + 2, y + oy + 2], fill=(34, 28, 46, 255))
    bullets = Bullets()
    for t0 in np.arange(1.6, 11.0, 1.4):
        x, y = TANK_PATH(t0)
        px, py = player_at(t0)
        ta = np.arctan2(py - y, px - x)
        mx, my = x + np.cos(ta) * 38, y + np.sin(ta) * 38
        bullets.fire(t0, mx, my, px, py, 190, "shell")
    frames = []
    for t in times(11.5):
        f = bg.copy()
        tank_draw(f, t, player_at(t))
        bullets.draw(f, t)
        draw_player(f, t)
        frames.append(f)
    hulls = [hull.frame(k, tread=0.0) for k in range(16)]
    turs = [turret.frame(k) for k in range(32)]
    panels = [("SOURCE: HULL, 260 PX", source_panel(lambda: am.warden_hull(), 76)),
              ("HULL: 16 HEADINGS (1X)", grid(hulls, 8, 1)),
              ("TURRET: 32 HEADINGS (1X), INDEPENDENT", grid(turs, 16, 1)),
              ("TREAD FRAMES (3X)", grid([hull.get(np.pi / 2, tread=0.0),
                                          hull.get(np.pi / 2, tread=0.5)], 2, 3))]
    notes = ["WARDEN TANK - ASCENDANCY TRACKED TANK (BLACK & GOLD, WHITE ACCENT, RIM LIGHT)",
             "76 PX. THE HULL FOLLOWS A ROAD-LIKE PATH AND TURNS TO ITS TANGENT (16 HEADINGS, 2 TREAD",
             "FRAMES); THE TURRET IS A SEPARATE SPRITE AT 32 HEADINGS THAT TRACKS THE PLAYER AND",
             "FIRES GOLD SHELLS. TREAD MARKS STAY ON THE GROUND LAYER. REAR DECK = WEAK POINT."]
    return build_sheet("WARDEN TANK - HULL AND TURRET TURN INDEPENDENTLY", panels, notes,
                       frames[100]), frames


# --------------------------------------------------------------------------- 7 Strider

STR_N = 120
str_legs = rig.AngleSprites(lambda phase=0.0: am.strider_legs(phase), STR_N, 16, factor=3,
                            colors=40, post=rim)
str_torso = rig.AngleSprites(lambda: am.strider_torso(), STR_N, 32, factor=3, colors=40, post=rim)
STR_PHASES = 8


def strider_draw(img, x, y, t, aim):
    k = int(t * 3.2 * STR_PHASES / 2) % STR_PHASES
    legs = str_legs.get(np.pi / 2, phase=k * TAU / STR_PHASES)
    paste(img, legs, x, y, None)
    ta = np.arctan2(aim[1] - y, aim[0] - x)
    ta = float(np.clip(ta, np.pi / 2 - 1.0, np.pi / 2 + 1.0))
    paste(img, str_torso.get(ta), x, y)
    return ta


def strider():
    bg = e3.regolith(FW, FH, 88, craters=9)
    frames = []
    bullets = Bullets()
    pos = lambda t: (240 + 30 * np.sin(t * 0.8), -40 + 38 * t)
    for i, t0 in enumerate(np.arange(1.5, 10, 0.75)):
        x, y = pos(t0)
        px, py = player_at(t0)
        ta = float(np.clip(np.arctan2(py - y, px - x), np.pi / 2 - 1.0, np.pi / 2 + 1.0))
        side = 1 if i % 2 else -1
        ox, oy = rig.local_to_screen(side * 0.62, 0.9, ta, STR_N / 2.3)
        bullets.fire(t0, x + ox, y + oy, px, py, 200, "gold")
    for t in times(10.0):
        f = bg.copy()
        x, y = pos(t)
        k = int(t * 3.2 * STR_PHASES / 2) % STR_PHASES
        sh = str_legs.get(np.pi / 2, phase=k * TAU / STR_PHASES)
        shadow = sprite.shadow_of(sh, opacity=0.55, blur=1.5)
        sprite.paste_center(f, shadow, x + 12, y + 16)
        strider_draw(f, x, y, t, player_at(t))
        bullets.draw(f, t)
        draw_player(f, t)
        frames.append(f)
    cycle = [str_legs.get(np.pi / 2, phase=k * TAU / STR_PHASES) for k in range(STR_PHASES)]
    torsos = [str_torso.frame(k) for k in range(32)]
    combo = Image.new("RGBA", (STR_N * 2 + 20, STR_N + 10), (22, 24, 34, 255))
    for i, ang in enumerate((np.pi / 2 - 0.8, np.pi / 2 + 0.8)):
        tmp = Image.new("RGBA", (STR_N, STR_N), (0, 0, 0, 0))
        strider_draw(tmp, STR_N / 2, STR_N / 2, 0.3, (STR_N / 2 + np.cos(ang) * 100,
                                                      STR_N / 2 + np.sin(ang) * 100))
        combo.alpha_composite(tmp, (i * (STR_N + 20), 5))
    panels = [("SOURCE: TORSO + LEGS, 260 PX",
               source_panel(lambda: _strider_combined(), STR_N)),
              ("WALK CYCLE: 8 FRAMES (1X)", grid(cycle, 8, 1)),
              ("TORSO: 32 HEADINGS (1X), AIMS +-57 DEG", grid(torsos, 16, 1)),
              ("TORSO TWIST OVER THE LEGS (1X)", combo)]
    notes = ["STRIDER - ASCENDANCY BIPED WALKER MECH (BLACK & GOLD, RED ACCENT, RIM LIGHT)",
             "120 PX: FOUR TIMES THE AREA OF A GUNSHIP. LEGS WALK TOWARDS THE PLAYER (8-FRAME CYCLE,",
             "16 HEADINGS FOR TURNS); THE TORSO IS A SEPARATE SPRITE AT 32 HEADINGS THAT TWISTS UP TO",
             "+-57 DEG TO AIM ITS ARM CANNONS, FIRING LEFT AND RIGHT IN TURN. KNEES = WEAK POINTS."]
    return build_sheet("STRIDER - BIPEDAL ASCENDANCY WALKER", panels, notes, frames[110]), frames


def _strider_combined():
    from render.sdf import union as u
    s1, m = am.strider_legs(0.8)
    s2, _ = am.strider_torso()
    return (lambda p: u(s1(p), s2(p))), m


# --------------------------------------------------------------------------- 8 Buzzsaw Drone

saw = rig.AngleSprites(lambda: am.buzzsaw(), 42, 48, factor=6, colors=32, sym=6, post=rim)


def buzzsaw():
    bg = e3.hull_plating(FW, FH, 99)
    drones = [dict(p=np.array([60.0, -40.0]), v=np.array([230.0, 170.0])),
              dict(p=np.array([420.0, -200.0]), v=np.array([-190.0, 210.0]))]
    trail = {0: [], 1: []}
    dt = 1 / FPS
    frames = []
    for t in times(7.0):
        f = bg.copy()
        for i, c in enumerate(drones):
            c["p"] = c["p"] + c["v"] * dt
            if c["p"][0] < 24 or c["p"][0] > FW - 24:
                c["v"][0] *= -1
                c["p"][0] = np.clip(c["p"][0], 24, FW - 24)
            if (c["p"][1] > FH - 24 and c["v"][1] > 0) or (c["p"][1] < 24 and c["v"][1] < 0 and t > 1.5):
                c["v"][1] *= -1
            trail[i] = (trail[i] + [c["p"].copy()])[-4:]
            ang = 14.0 * t + i
            for j, q in enumerate(trail[i][:-1]):
                paste(f, ghost(saw.get(ang - 0.3 * (3 - j)), 0.15 + 0.12 * j), q[0], q[1])
            paste(f, saw.get(ang), c["p"][0], c["p"][1], "air")
        draw_player(f, t)
        frames.append(f)
    spin = [saw.frame(k) for k in range(8)]
    panels = [("SOURCE, 260 PX", source_panel(lambda: am.buzzsaw(), 42)),
              ("SPIN: 8 FRAMES COVER 60 DEG (6-FOLD SYMMETRY), 3X", grid(spin, 8, 3))]
    notes = ["BUZZSAW DRONE - ASCENDANCY SPINNING BLADE DRONE (BLACK & GOLD, GUNMETAL BLADES, RIM)",
             "42 PX, RADIALLY SYMMETRIC: WORKS FROM ANY DIRECTION. SPINS FAST AND RICOCHETS OFF THE",
             "PLAY-FIELD EDGES AT HIGH SPEED; A SHORT AFTER-IMAGE TRAIL SHOWS ITS DIRECTION. CONTACT",
             "DAMAGE ONLY; THE GOLD HUB IS THE WEAK POINT. PIERCING OR BEAM WEAPONS HELP."]
    return build_sheet("BUZZSAW DRONE - RICOCHETING SPINNING BLADE", panels, notes,
                       frames[50]), frames


# --------------------------------------------------------------------------- 9 Rail Serpent

rail_head = rig.AngleSprites(lambda: am.rail_car(True), 40, 16, factor=6, colors=28, post=rim)
rail_car = rig.AngleSprites(lambda: am.rail_car(False), 36, 16, factor=6, colors=28, post=rim)


def rail_path(t):
    return rig.catmull_rom([(520, 80), (300, 60), (120, 150), (160, 300), (360, 320),
                            (380, 180), (220, 120), (60, 260), (120, 460), (340, 520),
                            (560, 420)], [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10])(t)


def rail_serpent():
    bg = e3.regolith(FW, FH, 101, craters=6)
    frames = []
    for t in times(9.0):
        f = bg.copy()
        pts, heads = rig.chain_at(rail_path, t + 1.0, [30] * 8)
        for i in range(len(pts) - 1, -1, -1):
            sp = rail_head.get(heads[0]) if i == 0 else rail_car.get(heads[i])
            paste(f, sp, pts[i][0], pts[i][1], "air")
        draw_player(f, t)
        frames.append(f)
    heads_ = [rail_head.frame(k) for k in range(16)]
    panels = [("SOURCE: HEAD CAR, 260 PX", source_panel(lambda: am.rail_car(True), 40)),
              ("HEAD CAR: 16 HEADINGS (1X)", grid(heads_, 8, 1)),
              ("CARS 3X: HEAD / CAR", grid([rail_head.get(np.pi / 2), rail_car.get(np.pi / 2)], 2, 3))]
    notes = ["RAIL SERPENT - ASCENDANCY SEGMENTED DRONE TRAIN (BLACK & GOLD, RED ACCENT, RIM LIGHT)",
             "A 40 PX HEAD CAR PULLS 8 LINKED 36 PX DRONE CARS ALONG A WINDING ROUTE (SAME CHAIN RIG AS",
             "THE COILWYRM, BUT RIGID CARS AT EVEN SPACING). EACH CAR CAN BE SHOT OFF; THE TRAIN",
             "RE-LINKS. HUMAN-MADE MIRROR OF THE COILWYRM FOR THE ASCENDANCY ACTS."]
    return build_sheet("RAIL SERPENT - ASCENDANCY DRONE TRAIN", panels, notes, frames[90]), frames


# --------------------------------------------------------------------------- size lineup

def size_lineup(extra=(), title="SIZE LINEUP - ROUND 05 ARCHETYPES WITH ROUND 04 UNITS, 1X"):
    """``extra``: additional (name, sprite) items for the first row (round 05 follow-ups)."""
    W = 1300
    small = [("MOTE", mote[1.1].frame(0)), ("WHIRL SEED", seed.frame(0))]
    r04 = [(e4.R04[k][0], e3.render(e4.R04[k][4], e4.R04[k][3])[1]) for k in e4.R04]
    mid = [("SCUTTLER", scut.get(np.pi / 2, phase=0.0)), ("BUZZSAW", saw.frame(0)),
           ("RAIL CAR", rail_head.get(np.pi / 2)), ("WARDEN TANK", _tank_still()),
           ("STRIDER", _strider_still())]
    player = [("AF-12 PLAYER", e3.player_sprite())]
    row1 = sorted(small + player + r04 + mid + list(extra), key=lambda v: v[1].width * v[1].height)
    # split the sorted units into strips that fit the width (one strip unless extras overflow)
    rows, cur, xx = [], [], 10
    for name, sp in row1:
        if xx + sp.width > W - 32 - 10 and cur:
            rows.append(cur)
            cur, xx = [], 10
        cur.append((xx, name, sp))
        xx += sp.width + 12
    rows.append(cur)
    img = raster.sheet(W, 1010 + 200 * (len(rows) - 1), title, SUB)
    x, y = 16, 44
    raster.draw_text(img, x, y, "SMALL TO MEDIUM, SORTED BY AREA (1X)", raster.LABEL_DIM)
    for r, row in enumerate(rows):
        ys = y + r * 200
        strip = Image.new("RGBA", (W - 32, 150), (22, 24, 34, 255))
        for lx, name, sp in row:
            sprite.paste_center(strip, sp, lx + sp.width / 2, 75)
        img.alpha_composite(strip, (x, ys + 12))
        for i, (lx, name, sp) in enumerate(row):
            raster.draw_text(img, x + lx, ys + 166 + (i % 2) * 10, name[:12], raster.LABEL)
    # big row: coilwyrm chain, brood carrier (1/2 not allowed -> 1x), leviathan
    y2 = y + 200 * len(rows)
    raster.draw_text(img, x, y2, "LARGE (1X): COILWYRM (STRETCHED), LEVIATHAN, BROOD CARRIER",
                     raster.LABEL_DIM)
    big = Image.new("RGBA", (W - 32, 740), (22, 24, 34, 255))
    coil_draw(big, 7.0, path=lambda tt: (30 + 90 * tt, 110 + 22 * np.sin(tt * 1.3)))
    raster.draw_text(big, 300, 170, "COILWYRM (HEAD RIGHT)", raster.LABEL)
    raster.draw_text(big, 60, 860 - 300, "LEVIATHAN", raster.LABEL)
    lev_draw(big, 360, 430, 0.5, h=0.0)
    carrier = e3.carrier_render()
    sprite.paste_center(big, carrier, big.width - carrier.width / 2 - 20, 370)
    sprite.paste_center(big, e3.player_sprite(), 700, 600)
    raster.draw_text(big, 680, 630, "PLAYER", raster.LABEL)
    img.alpha_composite(big, (x, y2 + 12))
    return img


def _tank_still():
    tmp = Image.new("RGBA", (76, 76), (0, 0, 0, 0))
    sprite.paste_center(tmp, hull.get(np.pi / 2), 38, 38)
    sprite.paste_center(tmp, turret.get(np.pi / 2), 38, 38)
    return tmp


def _strider_still():
    tmp = Image.new("RGBA", (STR_N, STR_N), (0, 0, 0, 0))
    strider_draw(tmp, STR_N / 2, STR_N / 2, 0.0, (STR_N / 2, STR_N * 2))
    return tmp


# --------------------------------------------------------------------------- main

UNITS = {
    "coilwyrm": ("air", coilwyrm), "leviathan": ("space", leviathan),
    "scuttler": ("ground", scuttler), "whirl-seed": ("air", whirl_seed),
    "mote-swarm": ("air", mote_swarm), "warden-tank": ("ground", warden_tank),
    "strider": ("ground", strider), "buzzsaw-drone": ("air", buzzsaw),
    "rail-serpent": ("air", rail_serpent),
}


def main(args):
    for name, (cat, fn) in UNITS.items():
        if args and name not in args:
            continue
        sheet, frames = fn()
        save_unit(cat, name, sheet, frames)
    if not args or "size-lineup" in args:
        p = out(None, "size-lineup-r05-a.png")
        size_lineup().convert("RGB").save(p, optimize=True)
        print("wrote", p.relative_to(ROOT))


if __name__ == "__main__":
    main(sys.argv[1:])
