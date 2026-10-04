#!/usr/bin/env python3
"""Production art: Level 05's Polyp Mortar with its lob, and the mass-driver sled (M4 part E batch).

Outputs (assets/sprites/):
  polyp-mortar_0..7.png   44x44 (its stat-block size; `orientation: fixed`): the idle pulse loop
                          the game plays at 10 fps, the acid mouth contracting and swelling and its
                          glow breathing with it; the chosen round-04 model (enemies_r04,
                          render/enemy_models.polyp_mortar_a, whose `anim` is the mouth)
  mortar-blob_0..3.png    16x16 the acid blob in flight: a wobbling lime glob, 1-bit body with a
                          stepped halo (drawn solid, scaled up to 1.6x at the top of its arc)
  mortar-marker.png       44x44 the impact marker (additive): a lime ring of radius 16 px (the
                          data's `impact` circle, a diameter of 32 px), eight inward ticks and a
                          centre dot; the game scales it to the lob's impact radius (1x for the
                          data's) and pulses its strength
  sled.png                24x48 the mass-driver sled racing up the rail: a lit hull with its hot
                          amber thrust collar and white plasma vents, facing up the screen
  sled-streak.png         32x200 its motion streak (additive): a hot white core fading to amber
                          over 160 px below the sled, with a bloom where the sled sits (top 40 px)
  sled-lamp.png           16x16 one rail lamp lit (additive): amber halo with a white-hot core,
                          drawn over Level 04's rail-lamp pixels while the lights chase and a sled runs
  polyp-mortar-death_0..11 64x64, additive: the mortar's death glow, a lime flash in the mouth and a
                          spray of glowing acid droplets flung out on a thinning haze
  polyp-mortar-tatters_0..11 64x64, solid: its nine tentacles torn off and tumbling outwards, five
                          dark shell shards of the tube; ray-marched from the chosen model's own
                          pieces and materials, posed per frame (vrell_fx.py's method)
                          The game plays both by name (EnemyLooks: `-death` glow, `-tatters`
                          pieces) with the small burst of its tier, 4 steps a frame (0.8 s)
  design/enemies/ground/polyp-mortar/concept/polyp-mortar-final-r21-a.png/.gif
  design/enemies/ground/polyp-mortar/concept/polyp-mortar-death-final-r21-a.png/.gif
  design/campaign/act-1-first-contact/level-05-crater-nest/concept/sled-final-r21-a.png/.gif

Run: python3 tools/art/l05_hazards.py [death] [--review]   (~20 s; `death` only the death sets)
"""
import sys
from concurrent.futures import ProcessPoolExecutor

import numpy as np
import yaml
from PIL import Image

import artkit
from artkit import DESIGN, TAU, sprite

import enemies_r04 as e4  # noqa: E402  (concept script, imported unchanged)
import vfx_r08 as v8  # noqa: E402
from render import enemy_models as em  # noqa: E402
from render import sdf  # noqa: E402
from render.sdf import Material, rotate_x, rotate_z, sd_box, sd_capsule, sd_cylinder_z, sd_ellipsoid, union  # noqa: E402

SCRIPT = "l05_hazards.py"
BATCH = "M4 part E batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
MORTAR = DESIGN / "enemies" / "ground" / "polyp-mortar"
LEVEL = DESIGN / "campaign" / "act-1-first-contact" / "level-05-crater-nest"
EXTENT = 2.3
PULSE = 8


def data(path):
    return yaml.safe_load((path / "data.yaml").read_text(encoding="utf-8"))


SIZE = tuple(data(MORTAR)["size"])
IMPACT = data(MORTAR)["attacks"][0]["mortar"]["impact"] // 2     # the data's circle is a diameter
LIME = em.scheme_colors("polyp-mortar")[3] * 255


def render_mortar(i):
    s = np.sin(TAU * i / PULSE)
    scene, mats = e4.R04["polyp-mortar-a"][4](anim=s, glow=1.0 + 0.25 * s)
    return artkit.native(*artkit.render_hi(scene, mats, SIZE, EXTENT))


# --------------------------------------------------------------------------- the lob

def blob_frames():
    out = []
    for f in range(4):
        cv = v8.Canvas(16, 16)
        x, y = cv.x - 8, cv.y - 8
        a = np.arctan2(y, x)
        r = np.hypot(x, y) / (4.6 * (1 + 0.12 * np.sin(3 * a + TAU * f / 4) + 0.06 * np.sin(2 * a - TAU * f / 4)))
        body = r < 1
        nz = np.sqrt(np.clip(1 - r ** 2, 0, 1))
        shade = np.clip(0.35 + 0.65 * (nz * 0.7 + (-x - y) / 9 * 0.3), 0, 1)
        col = np.array([60, 140, 20]) + (np.array([215, 255, 150]) - np.array([60, 140, 20])) * shade[..., None]
        halo = np.where(body, 0, v8.gauss(np.maximum(r - 1, 0) * 4.6, 2.2) * 0.8)
        cv.rgb = np.where(body[..., None], col / 255, np.array(LIME) / 255 * halo[..., None])
        cv.a = np.where(body, 1.0, halo)
        out.append(artkit.stepped_alpha(cv.image(), 4))
    return artkit.quantize_set(out, 16)


def marker():
    n = IMPACT * 2 + 12
    cv = v8.Canvas(n, n)
    c = n / 2
    d = cv.dist(c, c)
    a = np.arctan2(cv.y - c, cv.x - c)
    cv.add(tuple(LIME), v8.gauss(d - IMPACT, 1.4) * 1.0)
    cv.add(tuple(LIME), v8.gauss(d - IMPACT, 4.0) * 0.35)
    tick = (np.abs(((a / TAU * 8) + 0.5) % 1 - 0.5) < 0.06) & (d > IMPACT - 7) & (d < IMPACT - 2)
    cv.add((220, 255, 160), tick * 0.9)
    cv.add((220, 255, 160), v8.gauss(d, 1.8) * 0.9)
    return artkit.quantize_set([artkit.additive(cv.image())], 16)[0]


# --------------------------------------------------------------------------- the sled

HULL = Material((0.62, 0.64, 0.68), metal=0.6, shininess=60, spec=0.9)
DARK = Material((0.18, 0.19, 0.22), metal=0.4, shininess=30, spec=0.4)
AMBER = Material((0.30, 0.16, 0.04), metal=0.2, shininess=40, spec=0.6, emission=(1.0, 0.55, 0.12))
PLASMA = Material((0.3, 0.3, 0.3), emission=(1.3, 1.2, 1.0))
STRIPE = Material((0.85, 0.62, 0.10), metal=0.3, shininess=50, spec=0.7)


def sled_model():
    """Facing up the screen (+y): a wedge nose, the hull with hazard stripes, the dark clamp
    rails at its sides, an amber thrust collar and three white plasma vents at its tail."""
    def scene(p):
        items = [(sd_box(p, (0, 0.5, 2.4), (6.5, 15, 2.4), 1.6), 0),
                 (sd_capsule(p, (0, 14, 2.2), (0, 20, 1.6), 4.5, 1.5), 0),
                 (sd_box(p, (0, 4, 5.0), (3.6, 7, 1.0), 0.8), 4),
                 (sd_box(p, (0, -6, 5.0), (4.5, 1.2, 0.8), 0.3), 4)]
        items += [(sd_box(p, (sx * 7.5, 0, 1.6), (1.3, 13, 1.6), 0.5), 1) for sx in (-1, 1)]
        items += [(sd_box(p, (0, -15.5, 2.4), (6.0, 1.6, 2.3), 0.6), 2)]
        items += [(sd_cylinder_z(p, (x, -17.4, 2.4), 1.5, 1.6), 3) for x in (-3.6, 0.0, 3.6)]
        return union(*items)
    return scene, [HULL, DARK, AMBER, PLASMA, STRIPE]


def sled():
    scene, mats = sled_model()
    hi = sdf.render(scene, mats, (24 * 8, 48 * 8), 24.0, center=(0.0, 0.0), z_top=40.0, steps=110)
    return artkit.quantize_set([artkit.native(hi, 8)], 32)[0]


def streak():
    w, h = 32, 200
    cv = v8.Canvas(w, h)
    x = np.abs(cv.x - w / 2)
    below = np.clip((cv.y - 24) / (h - 24), 0, 1)          # 0 at the sled's tail, 1 at the end
    on = cv.y >= 24
    fade = (1 - below) ** 1.6 * on
    cv.add((255, 170, 60), v8.gauss(x, 5.5 + 3 * below) * 0.75 * fade)
    cv.add((255, 240, 210), v8.gauss(x, 1.8 + 0.8 * below) * 1.0 * fade)
    bloom = v8.gauss(np.hypot(cv.x - w / 2, (cv.y - 20) * 0.7), 12)
    cv.add((255, 200, 120), bloom * 0.7)
    return artkit.quantize_set([artkit.additive(cv.image())], 24)[0]


def lamp():
    cv = v8.Canvas(16, 16)
    d = cv.dist(8, 8)
    cv.add((255, 150, 40), v8.gauss(d, 4.2) * 0.95)
    cv.add((255, 245, 220), v8.gauss(d, 1.3) * 1.0)
    return artkit.quantize_set([artkit.additive(cv.image())], 12)[0]

# --------------------------------------------------------------------------- the death

DEATH = 12                                      # frames, 4 steps each (a small unit's rate)
DEATH_SIZE = 64
DEATH_EXTENT = EXTENT * DEATH_SIZE / SIZE[0]    # the mortar's model scale on the 64 px canvas
DPX = DEATH_SIZE / DEATH_EXTENT                 # px per model unit


def ease_out(t, k=2.0):
    return 1 - (1 - np.clip(t, 0, 1)) ** k


def death_glow(i):
    """Frame i of the death glow: a lime flash in the mouth, droplets of glowing acid flung out
    in low arcs, a thin haze; everything thins out."""
    t = i / (DEATH - 1)
    n = DEATH_SIZE
    c = n / 2
    rng = np.random.default_rng(2105)
    cv = v8.Canvas(n, n)
    d = cv.dist(c, c)
    flash = np.clip(1 - t * 3.5, 0, 1)
    cv.add((235, 255, 190), v8.gauss(d, 3 + 6 * t) * flash)
    cv.add(tuple(LIME), v8.gauss(d, 7 + 10 * ease_out(t)) * 0.55 * (1 - t) ** 1.5)
    fade = np.clip((n / 2 - d) / 4.0, 0, 1)
    for _ in range(44):
        a = rng.uniform(0, TAU)
        reach = rng.uniform(8, 27)
        size = rng.uniform(0.5, 1.2)
        delay = rng.uniform(0, 0.15)
        tt = np.clip((t - delay) / (1 - delay), 0, 1)
        if t < delay:
            continue
        r = 4 + reach * ease_out(tt, 2.4)
        x, y = c + np.cos(a) * r, c + np.sin(a) * r + 3 * tt ** 2      # sinking back a little
        life = np.clip(1.1 - tt * rng.uniform(0.9, 1.3), 0, 1)
        dd = cv.dist(x, y)
        cv.add((225, 255, 170) if size > 0.95 else tuple(LIME), v8.gauss(dd, size) * 1.4 * life * fade)
        cv.add(tuple(LIME), v8.gauss(dd, size * 2.5) * 0.2 * life * fade)
    return artkit.additive(cv.image())


def tatters_scene(t):
    """The mortar's pieces at time t (0..1): the nine tentacles torn off along their own axes,
    tumbling, and five dark shards of the tube, all shrinking at the end."""
    _, mats = e4.R04["polyp-mortar-a"][4](anim=0.0, glow=1.0)
    rng = np.random.default_rng(2106)
    tentacles = [(a, rng.uniform(0.5, 0.85), rng.uniform(3, 7) * rng.choice([-1, 1]))
                 for a in np.linspace(0.2, TAU + 0.2, 9, endpoint=False)]
    shards = [(rng.uniform(0, TAU), rng.uniform(0.45, 0.8), rng.uniform(0.12, 0.2)) for _ in range(5)]
    shrink = np.clip(1.0 - (t - 0.6) / 0.4 * 0.7, 0.3, 1.0)
    hop = 0.35 * np.sin(np.pi * min(t * 1.3, 1.0))
    mid = np.array([0.0, 0.68, 0.15])

    def scene(p):
        items = []
        for a, reach, tumble in tentacles:
            q = rotate_z(p, a).copy()
            q[:, 1] -= reach * ease_out(t, 2.0)
            q[:, 2] -= hop
            q = rotate_x(q - mid, tumble * t) / shrink + mid
            items.append((sd_capsule(q, (0, 0.45, 0.3), (0.0, 0.92, 0.0), 0.08, 0.02) * shrink, em.V_BODY))
        for a, reach, r in shards:
            out = 0.35 + reach * ease_out(t, 2.2)
            q = p - np.array([np.cos(a) * out, np.sin(a) * out, 0.1 + hop * 0.6])
            q = rotate_z(q, a + 3 * t) / shrink
            items.append((sd_ellipsoid(q, (0, 0, 0), (r * 1.4, r, r * 0.6)) * shrink, em.V_DARK))
        return union(*items)
    return scene, mats


def death_tatters(i):
    scene, mats = tatters_scene(i / (DEATH - 1))
    return artkit.native(*artkit.render_hi(scene, mats, (DEATH_SIZE, DEATH_SIZE), DEATH_EXTENT))


def build_death():
    with ProcessPoolExecutor() as pool:
        glow = list(pool.map(death_glow, range(DEATH)))
        pieces = list(pool.map(death_tatters, range(DEATH)))
    artkit.write_frames("polyp-mortar-death", artkit.quantize_set(glow, 24), SOURCE)
    artkit.write_frames("polyp-mortar-tatters", artkit.quantize_set(pieces, 24), SOURCE)


def review_death():
    artkit.REVIEW_ROUND = "r21"
    mortar = artkit.load_frames("polyp-mortar")
    burst = artkit.load_frames("explosion-small")
    glow = artkit.load_frames("polyp-mortar-death")
    pieces = artkit.load_frames("polyp-mortar-tatters")
    layers = [(pieces, 4, False), (burst, 2, True), (glow, 4, True)]
    together = []
    for age in range(0, 48, 4):
        cell = ground(DEATH_SIZE, DEATH_SIZE)
        for frames, steps, add in layers:
            if age // steps < len(frames):
                f = frames[age // steps]
                xy = ((DEATH_SIZE - f.width) // 2, (DEATH_SIZE - f.height) // 2)
                cell = artkit.add_light(cell, f, xy) if add else (cell.alpha_composite(f, xy) or cell)
        together.append(cell)
    sheet = artkit.review_sheet("POLYP MORTAR DEATH - FINAL EFFECTS", [
        ("POLYP-MORTAR-DEATH, 15 FPS (4 STEPS), ADDITIVE", glow, 2, True),
        ("POLYP-MORTAR-TATTERS, 15 FPS (4 STEPS), SOLID", pieces, 2, False),
        ("TOGETHER WITH THE SMALL BURST ON REGOLITH, EVERY 4TH STEP; TATTERS UNDER THE GLOWS", together, 2, False),
        ("1X", glow, 1, True), ("1X", pieces, 1, False)], width=1700, batch=BATCH)
    gif = []
    die = 20
    for step in range(0, 90, 2):
        cell = ground(120, 110)
        x, y = 60, 55
        if step < die:
            sprite.paste_center(cell, mortar[(step // 6) % len(mortar)], x, y)
        age = step - die
        for frames, steps, add in layers:
            if 0 <= age < len(frames) * steps:
                f = frames[age // steps]
                xy = (int(x - f.width / 2), int(y - f.height / 2))
                if add:
                    cell = artkit.add_light(cell, f, xy)
                else:
                    cell.alpha_composite(f, xy)
        gif.append(sprite.enlarge(cell, 3))
    artkit.save_review(sheet, gif, MORTAR / "concept", "polyp-mortar-death", fps=30)


# --------------------------------------------------------------------------- build and review

def build():
    with ProcessPoolExecutor() as pool:
        frames = artkit.quantize_set(list(pool.map(render_mortar, range(PULSE))), 32)
    artkit.write_frames("polyp-mortar", frames, SOURCE)
    artkit.write_frames("mortar-blob", blob_frames(), SOURCE)
    artkit.write_frames("mortar-marker", [marker()], SOURCE, single=True)
    artkit.write_frames("sled", [sled()], SOURCE, single=True)
    artkit.write_frames("sled-streak", [streak()], SOURCE, single=True)
    artkit.write_frames("sled-lamp", [lamp()], SOURCE, single=True)
    build_death()


def ground(w, h):
    """A plain regolith grey plate for the review: the sprites over a ground like Luna's."""
    return Image.new("RGBA", (w, h), (74, 72, 70, 255))


def review():
    artkit.REVIEW_ROUND = "r21"
    mortar = artkit.load_frames("polyp-mortar")
    blob = artkit.load_frames("mortar-blob")
    mark = artkit.load_frames("mortar-marker")[0]
    sheet = artkit.review_sheet("POLYP MORTAR - FINAL SPRITES", [
        ("IDLE PULSE LOOP, 10 FPS (ORIENTATION FIXED)", mortar, 4, False),
        ("1X", mortar, 1, False),
        ("ACID BLOB IN FLIGHT (SOLID, STEPPED HALO)", blob, 6, False),
        (f"IMPACT MARKER (ADDITIVE, RADIUS {IMPACT} PX)", [mark], 4, True)], batch=BATCH)
    gif = []
    for i in range(40):
        cell = ground(200, 150)
        sprite.paste_center(cell, mortar[i % PULSE], 40, 40)
        t = (i % 20) / 19
        tx, ty = 150, 105
        level = 0.55 + 0.45 * abs(np.sin(np.pi * t * (2 + 4 * t)))
        m = np.array(mark).astype(float)
        m[..., :3] *= level
        cell = artkit.add_light(cell, Image.fromarray(m.astype(np.uint8), "RGBA"),
                                (tx - mark.width // 2, ty - mark.height // 2))
        bx, by = 40 + (tx - 40) * t, 40 + (ty - 40) * t - np.sin(np.pi * t) * 50
        b = blob[(i // 2) % 4]
        sc = 1 + 0.6 * np.sin(np.pi * t)
        b = b.resize((round(b.width * sc), round(b.height * sc)), Image.NEAREST)
        sprite.paste_center(cell, b, bx, by)
        gif.append(sprite.enlarge(cell, 3))
    artkit.save_review(sheet, gif, MORTAR / "concept", "polyp-mortar", fps=10)

    sl = artkit.load_frames("sled")[0]
    st = artkit.load_frames("sled-streak")[0]
    lp = artkit.load_frames("sled-lamp")[0]
    rail = Image.open(artkit.ROOT / "assets" / "backdrop" / "level-05" / "sled-run_0.png").convert("RGBA")
    sheet = artkit.review_sheet("MASS-DRIVER SLED - FINAL SPRITES", [
        ("SLED (SOLID)", [sl], 4, False), ("MOTION STREAK (ADDITIVE)", [st], 2, True),
        ("RAIL LAMP LIT (ADDITIVE)", [lp], 6, True)], batch=BATCH)
    gif = []
    w, h = 120, 480
    for i in range(30):
        cell = ground(w, h)
        cell.alpha_composite(rail, ((w - rail.width) // 2, 0))
        lamps = [h - 1 - (20 + 60 * k) for k in range(8)]
        if i < 18:                                   # the chase: three sweeps up the rail
            head = h + 60 - ((i / 6) % 1) * (h + 120)
            for y in lamps:
                s = 0.25 + 0.75 * np.exp(-((y - head) / 45) ** 2)
                for dx in (-17, 17):
                    g = np.array(lp).astype(float)
                    g[..., :3] *= s
                    cell = artkit.add_light(cell, Image.fromarray(g.astype(np.uint8), "RGBA"),
                                            (w // 2 + dx - 8, y - 8))
        else:                                        # the sled: up the field in 0.4 s
            y = h + 40 - (i - 18) / 4 * (h + 240)
            for ly in lamps:
                for dx in (-17, 17):
                    cell = artkit.add_light(cell, lp, (w // 2 + dx - 8, ly - 8))
            cell = artkit.add_light(cell, st, (w // 2 - st.width // 2, int(y) - 20))
            sprite.paste_center(cell, sl, w // 2, y)
        gif.append(sprite.enlarge(cell, 2))
    artkit.save_review(sheet, gif, LEVEL / "concept", "sled", fps=10)


if __name__ == "__main__":
    args = sys.argv[1:]
    if "death" in args:
        if "--review" not in args:
            build_death()
        review_death()
    else:
        if "--review" not in args:
            build()
        review()
        review_death()
