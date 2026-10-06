#!/usr/bin/env python3
"""Production art: the effects of the Act 2 arsenal without water (design/player/weapons; M5 part A
batch): the Tail Gun, Fan Blaster, Hornet Launcher, Swivel Gun and Proximity Mines. The Act 1
arsenal's effects are tools/art/weapon_fx.py; the muzzle flashes and impacts these weapons share
with it (pulse, ballistic, launcher, explosive) are not redrawn.

Outputs (assets/sprites/; glows additive, premultiplied on black; physical rounds solid):
  tail-gun-shot_<deg>.png          the rear bolt at every angle its patterns use (degrees clockwise
  fan-blaster-shot_<deg>.png       from up, mod 360, from the weapons' data.yaml); the fan's bolt is
                                   shorter and rounder (its 6x6 hit box)
  hornet-launcher-shot_0..31       the Hornet missile with its plume at 32 headings, k x 11.25
                                   degrees clockwise from up, each rendered with the key light fixed
  hornet-launcher-smoke_0..5       a puff of its smoke trail, growing and thinning (solid, stepped
                                   translucency)
  swivel-gun-shot_0..31            the turret's tracer at 32 headings (it fires at any angle)
  proximity-mines-shot_0..3        the mine: 0 unarmed (sensor dark), 1..3 its armed sensor light
                                   pulsing (dim, half, full)
  proximity-mines-blast_0..11      the mine's blast, 104x104: the round-09 fireball with the player's
                                   blue shock ring running out to the 48 px blast radius
  design/player/weapons/<weapon>/concept/<weapon>-final-r28-a.png/.gif  one review pair per weapon

The looks are the chosen round-08 projectile families (tools/concept/vfx_r08.py: energy_bolt for
the `rear` family, tracer for `ballistic`, missile_model with_plume and smoke_puff for `missile`,
mine_model lit/unlit for `mine`) and the round-09 fireball (tools/concept/vfx_r09.py), each set on
one palette. Bolts and tracers are 2D light fields drawn at each angle rather than rotated; the
missile and the mine are lit models rendered per heading or light level.

Run: python3 tools/art/act2_weapon_fx.py [--review]   (~30 s)
"""
import sys

import numpy as np
from PIL import Image

import artkit
from artkit import DESIGN, TAU, sprite

import vfx_r08 as v8  # noqa: E402  (concept script, imported unchanged)
import vfx_r09 as v9  # noqa: E402  (concept script, imported unchanged)
import weapon_fx  # noqa: E402  (the Act 1 batch: its angle reader and writer)

SCRIPT = "act2_weapon_fx.py"
BATCH = "M5 part A batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
REVIEW_ROUND = "r28"
COLOURS = 32
WEAPONS = DESIGN / "player" / "weapons"
HEADINGS = 32
BOLTS = {
    "tail-gun": lambda a: v8.energy_bolt(10, 2.2, angle=a),
    "fan-blaster": lambda a: v8.energy_bolt(7, 2.5, angle=a),
}
SMOKE_FRAMES = 6
MINE_LIGHT = (0.0, 0.35, 0.7, 1.0)
BLAST_SIZE = 104
BLAST_RADIUS = 48      # the mine's blast at L1-L3 (data.yaml); L4-OD reach 52-60 px
BLAST_FRAMES = 12
BLAST_SEED = 21
FIREBALL = 84          # the fireball's frame inside the blast (no shock ring of its own below 90 px)
SHEET_WIDTH = 1300


def missile(heading):
    """The Hornet missile at ``heading`` (radians clockwise from up) with its plume behind it."""
    body = v8._obj(v8.missile_model, 16, 1.9, heading=-heading)
    size = 34
    cv = v8.Canvas(size, size)
    cx = cy = size / 2
    dx, dy = np.sin(heading), -np.cos(heading)
    tail_x, tail_y = cx - dx * 6.5, cy - dy * 6.5
    length = 9
    d = cv.seg(tail_x, tail_y, tail_x - dx * length, tail_y - dy * length)
    along = np.clip(((tail_x - cv.x) * dx + (tail_y - cv.y) * dy) / length, 0, 1)
    cv.add(v8.PS[3], v8.gauss(d, 2.2 * (1 - 0.5 * along)) * (1 - along) * 1.2)
    cv.add(v8.WHITE, v8.gauss(d, 0.9) * (1 - along) ** 2)
    out = cv.image()
    out.alpha_composite(body, ((size - body.width) // 2, (size - body.height) // 2))
    return out


def smoke(i):
    """Frame ``i`` of a trail puff: it swells and thins out."""
    t = i / (SMOKE_FRAMES - 1)
    puff = v8.smoke_puff(2.0 + 3.0 * t, 0.75 - 0.45 * t)
    canvas = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    canvas.alpha_composite(puff, ((16 - puff.width) // 2, (16 - puff.height) // 2))
    return artkit.stepped_alpha(canvas, 4)


def premultiplied(img):
    a = np.array(img.convert("RGBA")).astype(np.float64)
    return a[..., :3] * a[..., 3:4] / 255


def blast(i, fireballs):
    """Frame ``i`` of the mine's blast: the fireball, and the blue shock ring out to the radius."""
    t = i / (BLAST_FRAMES - 1)
    cv = v8.Canvas(BLAST_SIZE, BLAST_SIZE)
    c = BLAST_SIZE / 2
    d = cv.dist(c, c)
    grow = min(1.0, t / 0.4)
    radius = BLAST_RADIUS * (0.35 + 0.65 * (1 - (1 - grow) ** 2))
    fade = 1.0 if t <= 0.4 else max(0.0, 1 - (t - 0.4) / 0.45)
    ring = np.abs(d - radius)
    cv.add(v8.PS[2], v8.gauss(ring, 2.6) * 0.75 * fade)
    cv.add(v8.PS[3], v8.gauss(ring, 1.0) * 0.9 * fade)
    if i == 0:
        cv.add(v8.WHITE, v8.gauss(d, 7) * 1.4)
    light = premultiplied(cv.image())
    fire = fireballs[i]
    o = (BLAST_SIZE - fire.width) // 2
    light[o:o + fire.height, o:o + fire.width] += premultiplied(fire)
    rgb = np.clip(np.round(light), 0, 255)
    alpha = np.where(rgb.max(axis=-1) >= 1, 255, 0)
    return Image.fromarray(np.dstack([rgb, alpha]).astype(np.uint8), "RGBA")


def build():
    for slug, draw in BOLTS.items():
        degs = weapon_fx.angles(slug)
        frames = artkit.quantize_set([artkit.additive(draw(np.radians(a))) for a in degs], COLOURS)
        weapon_fx.write_angles(f"{slug}-shot", dict(zip(degs, frames)))
    missiles = artkit.quantize_set([missile(TAU * k / HEADINGS) for k in range(HEADINGS)], COLOURS)
    artkit.write_frames("hornet-launcher-shot", missiles, SOURCE)
    artkit.write_frames("hornet-launcher-smoke", artkit.quantize_set([smoke(i) for i in range(SMOKE_FRAMES)], 16),
                        SOURCE)
    tracers = [artkit.additive(v8.tracer(8, 1.4, angle=TAU * k / HEADINGS)) for k in range(HEADINGS)]
    artkit.write_frames("swivel-gun-shot", artkit.quantize_set(tracers, COLOURS), SOURCE)
    mines = [v8._obj(v8.mine_model, 12, 1.8, lit=lit) for lit in MINE_LIGHT]
    artkit.write_frames("proximity-mines-shot", artkit.quantize_set(mines, COLOURS), SOURCE)
    fireballs = [artkit.additive(f) for f in v9.explosion_frames9(FIREBALL, BLAST_FRAMES, BLAST_SEED)]
    blasts = [blast(i, fireballs) for i in range(BLAST_FRAMES)]
    artkit.write_frames("proximity-mines-blast", artkit.quantize_set(blasts, 48), SOURCE)


# --------------------------------------------------------------------------- review


def plate(w, h):
    return Image.new("RGBA", (w, h), artkit.PLATE)


def ship():
    return artkit.load_frames("ship")[2]


def put_light(img, sp, x, y):
    return artkit.add_light(img, sp, (int(round(x - sp.width / 2)), int(round(y - sp.height / 2))))


def put_solid(img, sp, x, y):
    img.alpha_composite(sp, (int(round(x - sp.width / 2)), int(round(y - sp.height / 2))))
    return img


def bolt_gif(slug, pattern, speed, rate, reach):
    """The ship flying up with the rear gun firing its pattern backwards, one step per frame."""
    shots = {int(p.stem.rsplit("_", 1)[1]): Image.open(p).convert("RGBA")
             for p in artkit.SPRITES.glob(f"{slug}-shot_*.png")}
    muzzle = artkit.load_frames("pulse-muzzle")
    w, h = 200, 240
    cx, cy = w // 2, 50
    period = round(60 / rate)
    gif = []
    for i in range(period * 6):
        img = plate(w, h)
        img.alpha_composite(ship(), (cx - 24, cy - 24))
        for k in range(8):
            age = i % period + k * period
            r = speed * age / 60
            if r > reach:
                continue
            for dx, a in pattern:
                sp = shots[round(a) % 360]
                img = put_light(img, sp, cx + dx + np.sin(np.radians(a)) * r, cy + 24 - np.cos(np.radians(a)) * r)
        frame = (i % period) // 2
        if frame < len(muzzle):
            img = put_light(img, muzzle[frame], cx, cy + 24)
        gif.append(sprite.enlarge(img, 3))
    return gif


def hornet_gif():
    """A volley of four Hornets leaving the ports, speeding up and curving onto a target."""
    missiles = artkit.load_frames("hornet-launcher-shot")
    puffs = artkit.load_frames("hornet-launcher-smoke")
    impact = artkit.load_frames("explosive-impact")
    w, h = 220, 260
    sx, sy = w / 2, h - 40
    tx, ty = w / 2 + 40, 50
    gif = []
    for i in range(48):
        img = plate(w, h)
        img.alpha_composite(ship(), (int(sx) - 24, int(sy) - 24))
        img.paste((255, 190, 40, 255), (int(tx) - 6, int(ty) - 6, int(tx) + 6, int(ty) + 6))
        trail = []
        hit = False
        for n, a0 in enumerate((-35, -12, 12, 35)):
            x, y = sx + (-10 if n % 2 == 0 else 10), sy - 21
            heading = np.radians(a0)
            for step in range(min(i, 40)):
                speed = 450 + 200 * min(1, step / 18)
                want = np.arctan2(tx - x, -(ty - y))
                delta = (want - heading + np.pi) % TAU - np.pi
                heading += np.clip(delta, -np.radians(220) / 60, np.radians(220) / 60)
                x += np.sin(heading) * speed / 60
                y -= np.cos(heading) * speed / 60
                if step % 3 == 0:
                    trail.append((x, y, step))
                if np.hypot(tx - x, ty - y) < 8:
                    hit = True
                    break
            else:
                k = round(heading / TAU * 32) % 32
                img = put_solid(img, missiles[k], x, y)
        for x, y, step in trail:
            age = (min(i, 40) - step) // 2
            if 0 <= age < len(puffs):
                img = put_solid(img, puffs[age], x, y)
        if hit and i < 46:
            img = put_light(img, impact[min((i - 20) // 2 % 4, 3)], tx, ty)
        gif.append(sprite.enlarge(img, 3))
    return gif


def swivel_gif():
    """The Stormhawk with two Swivel pods; a target circles the ship and the tracers follow it."""
    tracers = artkit.load_frames("swivel-gun-shot")
    left, right = artkit.load_frames("pod-swivel-left")[2], artkit.load_frames("pod-swivel-right")[2]
    import json
    pods = json.loads((artkit.PIVOTS / "pods.json").read_text(encoding="utf-8"))["offsets"]
    w, h = 220, 220
    cx, cy = w // 2, h // 2
    gif = []
    shots = []
    for i in range(60):
        img = plate(w, h)
        hull = ship()
        img.alpha_composite(hull, (cx - 24, cy - 24))
        img.alpha_composite(left, (cx - 24 + pods["pod-swivel-left"][2][0], cy - 24 + pods["pod-swivel-left"][2][1]))
        img.alpha_composite(right, (cx - 24 + pods["pod-swivel-right"][2][0], cy - 24 + pods["pod-swivel-right"][2][1]))
        ang = TAU * i / 60 + 1.2
        tx, ty = cx + np.sin(ang) * 80, cy - np.cos(ang) * 80
        img.paste((255, 190, 40, 255), (int(tx) - 5, int(ty) - 5, int(tx) + 5, int(ty) + 5))
        if i % 10 == 0:
            for px in (cx - 16, cx + 16):
                a = np.arctan2(tx - px, -(ty - (cy + 3)))
                shots.append([px, cy + 3, a])
        for s in shots:
            s[0] += np.sin(s[2]) * 700 / 60
            s[1] -= np.cos(s[2]) * 700 / 60
        shots = [s for s in shots if 0 < s[0] < w and 0 < s[1] < h]
        for x, y, a in shots:
            img = put_light(img, tracers[round(a / TAU * 32) % 32], x, y)
        gif.append(sprite.enlarge(img, 3))
    return gif


def mines_gif():
    """Mines dropping behind the ship, drifting to a halt, arming (the light pulses) and one going
    off in its blast as a target passes."""
    mines = artkit.load_frames("proximity-mines-shot")
    blasts = artkit.load_frames("proximity-mines-blast")
    w, h = 220, 240
    cx, cy = w // 2, 60
    gif = []
    for i in range(72):
        img = plate(w, h)
        img.alpha_composite(ship(), (cx - 24, cy - 24))
        target_y = h + 20 - i * 3.4
        boom = None
        for k, drop in enumerate((0, 24, 48)):
            age = i - drop
            if age < 0:
                continue
            drift = sum(60 * max(0, 1 - s / 30) / 60 for s in range(min(age, 30)))
            x, y = cx - 30 + 30 * k, cy + 26 + drift + k * 34
            if k == 1 and target_y - y < 40 and age >= 24:
                boom = boom or (x, y, i)
            if k == 1 and boom:
                continue
            frame = 0 if age < 24 else 1 + (age // 4) % 3
            img = put_solid(img, mines[frame], x, y)
        if boom is None or i - boom[2] < 0:
            img.paste((255, 190, 40, 255), (cx - 5, int(target_y) - 5, cx + 5, int(target_y) + 5))
        gif.append(img)
    out = []
    first = None
    for i, img in enumerate(gif):
        target_y = h + 20 - i * 3.4
        if first is None and target_y - (cy + 26 + 15 + 34) < 40 and i - 24 >= 24:
            first = i
        if first is not None and 0 <= i - first < len(blasts):
            img = put_light(img, blasts[i - first], cx, cy + 26 + 15 + 34)
        out.append(sprite.enlarge(img, 2))
    return out


def split(label, frames, zoom, glow):
    """A long frame set as several sheet rows (the sheet cuts a row at its width)."""
    per = max(1, (SHEET_WIDTH - 32) // (frames[0].width * zoom + 6))
    chunks = [frames[k:k + per] for k in range(0, len(frames), per)]
    return [(label if k == 0 else f"{label} (CONT.)", chunk, zoom, glow) for k, chunk in enumerate(chunks)]


def review():
    artkit.REVIEW_ROUND = REVIEW_ROUND

    def by_angle(name):
        return weapon_fx.indexed(name)

    sheets = {
        "tail-gun": ("TAIL GUN - FINAL EFFECTS", [
            ("REAR BOLT, EVERY ANGLE", by_angle("tail-gun-shot"), 6, True),
            ("PULSE MUZZLE, IMPACT (SHARED)", artkit.load_frames("pulse-muzzle") + artkit.load_frames("pulse-impact"),
             6, True)],
            bolt_gif("tail-gun", [(-8, 180), (0, 180), (8, 180)], 800, 8, 200)),
        "fan-blaster": ("FAN BLASTER - FINAL EFFECTS", [
            ("FAN BOLT, EVERY ANGLE", by_angle("fan-blaster-shot"), 6, True),
            ("PULSE MUZZLE, IMPACT (SHARED)", artkit.load_frames("pulse-muzzle") + artkit.load_frames("pulse-impact"),
             6, True)],
            bolt_gif("fan-blaster", [(0, a) for a in (150, 170, 180, 190, 210)], 650, 4, 390)),
        "hornet-launcher": ("HORNET LAUNCHER - FINAL EFFECTS", [
            ("HORNET, 32 HEADINGS", artkit.load_frames("hornet-launcher-shot"), 2, False),
            ("SMOKE TRAIL PUFF", artkit.load_frames("hornet-launcher-smoke"), 6, False),
            ("LAUNCHER MUZZLE (SHARED)", artkit.load_frames("launcher-muzzle"), 6, False),
            ("EXPLOSIVE IMPACT (SHARED)", artkit.load_frames("explosive-impact"), 4, True)],
            hornet_gif()),
        "swivel-gun": ("SWIVEL GUN - FINAL EFFECTS", [
            ("TURRET TRACER, 32 HEADINGS", artkit.load_frames("swivel-gun-shot"), 4, True),
            ("BALLISTIC MUZZLE, IMPACT (SHARED)",
             artkit.load_frames("ballistic-muzzle") + artkit.load_frames("ballistic-impact"), 6, True)],
            swivel_gif()),
        "proximity-mines": ("PROXIMITY MINES - FINAL EFFECTS", [
            ("MINE: UNARMED, ARMED LIGHT DIM / HALF / FULL", artkit.load_frames("proximity-mines-shot"), 8, False),
            ("MINE BLAST (48 PX RADIUS RING)", artkit.load_frames("proximity-mines-blast"), 1, True),
            ("LAUNCHER MUZZLE (SHARED)", artkit.load_frames("launcher-muzzle"), 6, False)],
            mines_gif()),
    }
    for slug, (title, rows, gif) in sheets.items():
        rows = [row for label, frames, zoom, glow in rows for row in split(label, frames, zoom, glow)]
        sheet = artkit.review_sheet(title, rows, width=SHEET_WIDTH, batch=BATCH)
        concept = WEAPONS / slug / "concept"
        concept.mkdir(exist_ok=True)
        artkit.save_review(sheet, gif, concept, slug, fps=20)


if __name__ == "__main__":
    if "--review" not in sys.argv[1:]:
        build()
    review()
