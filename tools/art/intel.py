#!/usr/bin/env python3
"""Production art: the hangar intel's sensor-L2 pictures (design/ui/hangar, design/player/systems
"Sensor levels and hangar intel": enemy types with portraits, boss name and silhouette).

Outputs (assets/sprites/intel/, packed onto the shared sprite pages as ``intel/<name>``):
  <enemy>.png        30x30 sensor portrait of an enemy type of a level's waves: skitter, needler,
                     stinger, spine-turret (Levels 01-02, UI batch); spore-bomber, whirl-seed
                     (Level 03, M4 part C batch); brood-pod, scuttler (Level 04, M4 part D batch); mantis,
                     coilwyrm (Level 06, M4 part F batch: the production models of tools/art/mantis.py,
                     nose down, and tools/art/coilwyrm.py, the head with a segment trailing); creeper
                     (Level 08, M5 part B batch: the production model of tools/art/creeper.py);
                     hive-node, ravager (Level 09, M5 part C batch: the production models of
                     tools/art/hive_node.py, the iris half open, and tools/art/ravager.py);
                     wraith, mote-swarm (Level 10, M5 part D batch: the production models of
                     tools/art/wraith.py, decloaked, nose down in ripple frame 1, and
                     tools/art/mote_swarm.py, a spike leading down, its slit at the middle glow);
                     driftjelly, reef-spitter (Level 11, M5 part E batch: the production models of
                     tools/art/driftjelly.py at rest, and tools/art/reef_spitter.py, the gun aiming down
                     on its kelp raft)
  boss-<boss>.png    40x40 sensor silhouette of a boss of Act 1: gorgon-frigate (L05 mid-boss),
                     brood-carrier (L07) (UI batch); leviathan (L03's set piece, the threat
                     profile's "unknown huge contact", M4 part C batch); harbour-kraken (L11's
                     mid-boss, M5 part E batch: the head and mantle of the `harbour-kraken-sub` sprite)
  design/ui/hangar/concept/intel-final-r13-a.png   review sheet of the UI batch's pictures
  design/ui/hangar/concept/intel-final-r16-a.png   review sheet of the M4 part C batch's
  design/ui/hangar/concept/intel-final-r17-a.png   review sheet of the M4 part D batch's
  design/ui/hangar/concept/intel-final-r23-a.png   review sheet of the M4 part F batch's
  design/ui/hangar/concept/intel-final-r30-a.png   review sheet of the M5 part B batch's
  design/ui/hangar/concept/intel-final-r31-a.png   review sheet of the M5 part C batch's
  design/ui/hangar/concept/intel-final-r32-a.png   review sheet of the M5 part D batch's
  design/ui/hangar/concept/intel-final-r33-a.png   review sheet of the M5 part E batch's

The names are the enemy's or boss's name as a slug (vanguard.game.render.Portraits.slug). A
portrait is the unit's chosen round-04 model (tools/concept/enemies_r04.py, imported unchanged) in
its own colours, nose down as it comes at the player (the Whirl Seed is the six-blade production
seed of tools/art/vrell_l03.py, as the game draws it, at rest; the Brood Pod and the Scuttler are
the production models of tools/art/vrell_l04.py, the pod between swells, the Scuttler walking down
at heading 0 in its first walk frame; the Creeper likewise from tools/art/creeper.py; the Hive Node from tools/art/hive_node.py with its
iris half open and glowing, the Ravager from tools/art/ravager.py running down in gallop frame 2; the Wraith from tools/art/wraith.py nose down, decloaked, the Mote from tools/art/mote_swarm.py), ray-marched at 8x through the sprite path
(1-bit alpha, unsharp mask) onto the intel's sensor plate: a dark teal screen with a dot grid, a
cyan scan line and corner brackets, lightly tinted cyan as the scan sees it, 32 colours. A boss
silhouette shows what L2 knows of it: the outline only, the shape filled flat in dark teal with a
bright cyan rim and the plate's grid running through it, from the chosen models (the Brood
Carrier of round 04, the Gorgon Frigate's bell, necks and heads of round 06 at rest, the
Leviathan's production model of tools/art/leviathan.py facing down with its parts on and the tail
straight).

Set pieces (the Leviathan) and other units outside a level's waves get no portrait: the intel lists
the waves' enemy types only. The Leviathan gets a silhouette instead, as a boss does: Level 03's
threat profile promises an "unknown huge contact" silhouette at L2. Its name follows the game's
``intel/boss-<slug>`` lookup.

Run: python3 tools/art/intel.py [name ...] [--review]   (~40 s; with names, e.g. spore-bomber or
boss-brood-carrier, only those pictures and the review sheets of their batches; --review only
rebuilds the sheets)
"""
import sys
from concurrent.futures import ProcessPoolExecutor

import numpy as np
from PIL import Image

import artkit
from artkit import DESIGN, ROOT, SPRITES, sprite

import coilwyrm  # noqa: E402  (the Coilwyrm's production models)
import creeper  # noqa: E402  (the Creeper's production model)
import reef_spitter  # noqa: E402  (the Reef Spitter's production gun and raft models)
import hive_node  # noqa: E402  (the Hive Node's production model)
import leviathan  # noqa: E402  (the Leviathan's production model)
import mantis  # noqa: E402  (the Mantis's production model)
import mote_swarm  # noqa: E402  (the Mote's production model)
import ravager  # noqa: E402  (the Ravager's production model)
import vrell_l03  # noqa: E402  (the Whirl Seed's production model)
import vrell_l04  # noqa: E402  (the Brood Pod's and the Scuttler's production models)
import wraith  # noqa: E402  (the Wraith's production model)

import bosses_r06  # noqa: E402  (concept scripts, imported unchanged)
import enemies_r03 as e3  # noqa: E402
import enemies_r04 as e4  # noqa: E402
from render import enemy_models as em  # noqa: E402
from render import r06_models as m6  # noqa: E402
from render import raster, sdf  # noqa: E402
from render.enemy_rigs import model_space_materials  # noqa: E402
from render.sdf import rotate_z  # noqa: E402

SCRIPT = "intel.py"
UI_BATCH = "UI batch"
# the batch each picture was made in (the Source note); the UI batch's are left out
BATCHES = {"spore-bomber": "M4 part C batch", "whirl-seed": "M4 part C batch",
           "boss-leviathan": "M4 part C batch",
           "brood-pod": "M4 part D batch", "scuttler": "M4 part D batch",
           "mantis": "M4 part F batch", "coilwyrm": "M4 part F batch",
           "creeper": "M5 part B batch",
           "hive-node": "M5 part C batch", "ravager": "M5 part C batch",
           "wraith": "M5 part D batch", "mote-swarm": "M5 part D batch",
           "driftjelly": "M5 part E batch", "reef-spitter": "M5 part E batch",
           "boss-harbour-kraken": "M5 part E batch"}
# the concept round that reviews a batch
ROUNDS = {UI_BATCH: "r13", "M4 part C batch": "r16", "M4 part D batch": "r17", "M4 part F batch": "r23",
          "M5 part B batch": "r30", "M5 part C batch": "r31", "M5 part D batch": "r32", "M5 part E batch": "r33"}
OUT = SPRITES / "intel"
CONCEPT = DESIGN / "ui" / "hangar" / "concept"
PORTRAIT = 30
SILHOUETTE = 40
COLOURS = 32
# enemy -> its chosen model, (scene, mats) at rest
ENEMIES = {
    "skitter": lambda: e4.R04["skitter-a"][4](0.0, 1.0),
    "needler": lambda: e4.R04["needler-a"][4](0.0, 1.0),
    "stinger": lambda: e4.R04["stinger-a"][4](0.0, 1.0),
    "spine-turret": lambda: e4.R04["spine-turret-a"][4](0.0, 1.0),
    "spore-bomber": lambda: e4.R04["spore-bomber-a"][4](0.0, 1.0),
    "whirl-seed": lambda: vrell_l03.seed_model(0.0),
    "brood-pod": lambda: vrell_l04.pod_model(0.0, 1.0),
    "scuttler": lambda: scuttler_model(),
    "mantis": lambda: mantis.turned(*mantis.model(0.3, 0.0, 1.0), 0.0),
    "coilwyrm": lambda: coilwyrm_model(),
    "creeper": lambda: creeper.model(0, 0),
    "hive-node": lambda: hive_node.model(3, 0),
    "ravager": lambda: ravager.model(0, 2),
    "wraith": lambda: wraith.model(0, 1),
    "mote-swarm": lambda: mote_swarm.model(0, 1.1),
    "driftjelly": lambda: m6.driftjelly(0.0),
}
BOSSES = ("gorgon-frigate", "brood-carrier", "leviathan", "harbour-kraken")
# portraits that are not one model: name -> the 8x render (an RGBA array), as sdf.render gives it

PLATE = np.array([4, 16, 28], float)
GRID = np.array([16, 60, 80], float)
SCAN = np.array([60, 220, 255], float)
SHAPE = np.array([10, 52, 66], float)


def scuttler_model():
    """The Scuttler at heading 0 (walking down the screen), walk frame 0, as vrell_l04 renders it."""
    rot = np.pi
    base = vrell_l04.scuttler_scene(0.0)
    return (lambda p: base(rotate_z(p, rot))), model_space_materials(vrell_l04.scuttler_mats("unit"), rot)


def coilwyrm_model():
    """The Coilwyrm's head nose down with its first segment trailing up behind it, slightly off line,
    the production models (tools/art/coilwyrm.py) at heading 0."""
    from render import archetype_models as am
    head, mats = coilwyrm.turned(*am.coil_head(0.45), 0.0)
    seg, _ = coilwyrm.turned(*am.coil_segment(), 0.0)
    parts = [(head, (0.0, -0.28), 0.86), (seg, (0.14, 0.72), 0.62)]

    def scene(p):
        out = []
        for model, (x, y), k in parts:
            d, m = model((p - np.array([x, y, 0.0])) / k)
            out.append((d * k, m))
        return sdf.union(*out[::-1])
    return scene, mats


def plate(n):
    """The sensor plate: dark teal with a dot grid, a scan line at two thirds and corner brackets."""
    yy, xx = np.mgrid[0:n, 0:n]
    img = np.empty((n, n, 3))
    img[...] = PLATE
    img[(xx % 5 == 2) & (yy % 5 == 2)] = GRID
    img[2 * n // 3] = PLATE * 0.5 + SCAN * 0.35
    arm = max(3, n // 8)
    for cy, cx in ((0, 0), (0, n - 1), (n - 1, 0), (n - 1, n - 1)):
        ys = slice(cy, cy + arm) if cy == 0 else slice(cy - arm + 1, cy + 1)
        xs = slice(cx, cx + arm) if cx == 0 else slice(cx - arm + 1, cx + 1)
        img[cy, xs] = SCAN
        img[ys, cx] = SCAN
    return img


def reef_spitter_hi():
    """The Reef Spitter: its gun (heading 0, aiming down the screen) on the kelp raft, the production
    models of tools/art/reef_spitter.py rendered together at the raft's scale, the gun over the raft."""
    size, extent = (PORTRAIT * 8, PORTRAIT * 8), reef_spitter.RAFT_EXTENT * 1.0
    raft_scene, raft_mats = reef_spitter.raft_model()
    raft = sdf.render(raft_scene, raft_mats, size, extent)
    gun_scene, gun_mats = m6.reef_gun(recoil=0.0, glow=1.0)
    rot = reef_spitter.gun_rotation(0)
    gun = sdf.render(lambda p: gun_scene(rotate_z(p, rot)), model_space_materials(gun_mats, rot), size, extent)
    ga, ra = gun[..., 3:4], raft[..., 3:4]
    alpha = ga + ra * (1 - ga)
    rgb = (gun[..., :3] * ga + raft[..., :3] * ra * (1 - ga)) / np.maximum(alpha, 1e-6)
    return np.concatenate([rgb, alpha], axis=-1)


HI = {"reef-spitter": reef_spitter_hi}


def portrait(slug):
    if slug in HI:
        hi = HI[slug]()
    else:
        scene, mats = ENEMIES[slug]()
        hi = sdf.render(scene, mats, (PORTRAIT * 8, PORTRAIT * 8), e3.EXTENT * 1.06)
    unit = np.array(artkit.native(hi, 8)).astype(np.float64)
    a = unit[..., 3:4] / 255
    rgb = unit[..., :3] * 0.82 + SCAN * 0.12 * (unit[..., :3].mean(-1, keepdims=True) / 255 + 0.3)
    card = plate(PORTRAIT) * (1 - a) + rgb * a
    img = Image.fromarray(np.clip(card, 0, 255).astype(np.uint8), "RGB").convert("RGBA")
    return artkit.quantize_set([img], COLOURS)[0]


def gorgon_mask():
    layer = Image.new("RGBA", (480, 480), (0, 0, 0, 0))
    bosses_r06.gorgon_compose(layer, 240, 190, np.pi / 2, 0.0, [np.pi / 2] * 3)
    return np.array(layer)[..., 3] > 127


def carrier_mask():
    scene, mats = em.brood_carrier(0.0, 1.0, 0.0, 0.0)
    hi = sdf.render(scene, mats, (e3.BW, e3.BH), e3.EXTENT)
    return hi[..., 3] > 0.5


def leviathan_mask():
    """The production Leviathan facing down (the second pass's view), every part on, the tail
    straight, at 2x its 300x480 frame: the outline only, so no shadows."""
    model, mats = leviathan.unit_model(0.0)
    rot = leviathan.ROT_DOWN
    w, h = leviathan.W * 2, leviathan.H * 2
    scene = lambda p: model(rotate_z(p, rot))  # noqa: E731
    hi = sdf.render(scene, model_space_materials(mats * (len(leviathan.PARTS) + 1), rot), (w, h),
                    leviathan.W / leviathan.S, shadows=False, steps=140)
    return hi[..., 3] > 0.5


def kraken_mask():
    """The Harbour Kraken's head and mantle facing down, the production `harbour-kraken-sub` sprite's
    outline (tools/art/harbour_kraken.py: what lies under the platform), the arms left out."""
    return np.array(Image.open(SPRITES / "harbour-kraken-sub.png").convert("RGBA"))[..., 3] > 127


def silhouette(slug):
    mask = {"gorgon-frigate": gorgon_mask, "brood-carrier": carrier_mask, "leviathan": leviathan_mask,
            "harbour-kraken": kraken_mask}[slug]()
    ys, xs = np.nonzero(mask)
    mask = mask[ys.min():ys.max() + 1, xs.min():xs.max() + 1]
    inner = SILHOUETTE - 6
    scale = inner / max(mask.shape)
    size = (max(1, round(mask.shape[1] * scale)), max(1, round(mask.shape[0] * scale)))
    small = np.array(Image.fromarray((mask * 255).astype(np.uint8)).resize(size, Image.BOX)) > 110
    shape = np.zeros((SILHOUETTE, SILHOUETTE), bool)
    y0, x0 = (SILHOUETTE - size[1]) // 2, (SILHOUETTE - size[0]) // 2
    shape[y0:y0 + size[1], x0:x0 + size[0]] = small
    card = plate(SILHOUETTE)
    yy, xx = np.mgrid[0:SILHOUETTE, 0:SILHOUETTE]
    card[shape] = SHAPE
    card[shape & ((xx % 5 == 2) | (yy % 5 == 2))] = SHAPE * 0.6 + SCAN * 0.25
    pad = np.pad(shape, 1)
    rim = shape & ~(pad[:-2, 1:-1] & pad[2:, 1:-1] & pad[1:-1, :-2] & pad[1:-1, 2:])
    card[rim] = SCAN
    return Image.fromarray(card.astype(np.uint8), "RGB").convert("RGBA")


def render(job):
    kind, slug = job
    return portrait(slug) if kind == "enemy" else silhouette(slug)


def picture_name(job):
    kind, slug = job
    return f"boss-{slug}" if kind == "boss" else slug


JOBS = [("enemy", slug) for slug in list(ENEMIES) + list(HI)] + [("boss", slug) for slug in BOSSES]


def batch_of(name):
    return BATCHES.get(name, UI_BATCH)


def build(names):
    OUT.mkdir(parents=True, exist_ok=True)
    jobs = [job for job in JOBS if not names or picture_name(job) in names]
    for old in OUT.glob("*.png") if not names else (OUT / f"{picture_name(job)}.png" for job in jobs):
        old.unlink(missing_ok=True)
    with ProcessPoolExecutor() as pool:
        for job, img in zip(jobs, pool.map(render, jobs)):
            name = picture_name(job)
            artkit.save_png(img, OUT / f"{name}.png", artkit.source_note(SCRIPT, batch_of(name)))
    print(f"{len(jobs)} intel pictures in {OUT.relative_to(ROOT)}")


def review(names):
    """The review sheets of the named pictures' batches; without names every batch's."""
    batches = {batch_of(name) for name in names or map(picture_name, JOBS)}
    for batch in sorted(batches, key=list(ROUNDS).index):
        review_batch(batch, [picture_name(job) for job in JOBS if batch_of(picture_name(job)) == batch])


def review_batch(batch, names):
    rnd = ROUNDS[batch]
    sheet = raster.sheet(16 + 6 * 180 + 16, 60 + 150 + 20, f"HANGAR INTEL (FINAL {rnd.upper()}): SENSOR L2 PICTURES",
                         f"PRODUCTION ART, {batch.upper()} - {rnd.upper()}")
    bosses = any(name.startswith("boss-") for name in names)
    raster.draw_text(sheet, 16, 38, f"ENEMY PORTRAITS {PORTRAIT}X{PORTRAIT}"
                                    + (f" AND BOSS SILHOUETTES {SILHOUETTE}X{SILHOUETTE}" if bosses else "")
                                    + ", AT 1X AND 3X", raster.LABEL)
    for i, name in enumerate(names):
        img = Image.open(OUT / f"{name}.png").convert("RGBA")
        x, y = 16 + i * 180, 56
        sheet.alpha_composite(img, (x, y))
        sheet.alpha_composite(sprite.enlarge(img, 3), (x + img.width + 6, y))
        raster.draw_text(sheet, x, y + 3 * img.height + 4,
                         f"{name.upper()} ({artkit.colour_count([img])} COL)", raster.LABEL_DIM)
    path = CONCEPT / f"intel-final-{rnd}-a.png"
    sheet.convert("RGB").save(path, optimize=True)
    print(f"review: {path.relative_to(ROOT)}")


if __name__ == "__main__":
    names = [a for a in sys.argv[1:] if not a.startswith("--")]
    unknown = set(names) - set(map(picture_name, JOBS))
    if unknown:
        sys.exit(f"unknown intel pictures: {', '.join(sorted(unknown))}")
    if "--review" not in sys.argv[1:]:
        build(names)
    review(names)
