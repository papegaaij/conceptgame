#!/usr/bin/env python3
"""Production art: Level 07's lifeboat tow, the concept chosen in concept round 25 (variant b,
"orange lifeboat capsule"; design/campaign/act-1-first-contact/level-07-brood-carrier, Secrets:
"Lifeboat tow").

Outputs (assets/sprites/, sizes from the `tows` entry of Level 07's data.yaml; one palette over the
boat, the pod and the cable):
  lifeboat_0..1.png             72x36 the rescue-orange pressure capsule lying across (white caps and
                                bands, a CDF blue band, a docking collar, four lit portholes, a tow
                                bridle): the two blue strobes lit, dark
  lifeboat-glow.png             72x36, additive: the two strobes' stepped blue halos, drawn over
                                the boat with its lit frame (1 Hz, 0.16 s)
  lifeboat-pod_0..47.png        44x44 the crate-pod (olive body, amber/black hazard lid, dark corner
                                posts, the bridle to its tow ring) at 24 headings, 15 degrees apart
                                clockwise on the screen (frames 0-23), then the same headings with the
                                lid blown off and the hold empty (24-47) for after the crate fell out.
                                Frame 0 hangs on the cable; the loose pod tumbles at 150 deg/s
                                (TowLooks.TUMBLE), so a frame lasts 0.1 s. The tumble is rendered from
                                the turned model with the key light fixed (nothing lit is rotated at
                                runtime); the canvas is larger than the 32x32 box so the corner posts
                                fit at 45 degrees
  lifeboat-cable_0..3.png       16x44 (over the 16x40 hit box, 2 px under the boat and the pod each)
                                the thin cable with the amber light strip and the hazard-striped
                                breakaway coupler: intact; 1 hit (the coupler dented and scorched, the
                                strip above it dark); 2 hits (split open at one side); cut (the cable
                                and the coupler's lower jaw hanging from the boat, the strip dark)
  lifeboat-cable-glow_0..1.png  16x44, additive: the light strip's soft amber line, both halves
                                (intact) and the lower half only (hit); the game pulses it at 1.2 Hz
                                with the batch colour while the cable holds
The cable's glint is the shared loot glint (`glint`, tools/art/loot_targets.py), every 2 s on the
coupler while the cable holds.
  design/campaign/act-1-first-contact/level-07-brood-carrier/concept/lifeboat-final-r25-a.png/.gif

The models and materials are the concept round's (tools/concept/props_r25.py, imported unchanged,
variant b), rendered at the quality bar's 8x, the pod with its light rim and the hit cable frames
with the concept's scorch. The glows are the concept's 2D light fields (its halo and strip_glow),
stored premultiplied on black.

Run: python3 tools/art/lifeboat.py [--review]   (~40 s)
"""
import sys
from concurrent.futures import ProcessPoolExecutor
from dataclasses import replace

import numpy as np
import yaml
from PIL import Image

import artkit
from artkit import DESIGN, ROOT, sprite
from render.sdf import rotate_z, sd_box, union  # noqa: E402

import props_r25 as r25  # noqa: E402  (concept script, imported unchanged)

SCRIPT = "lifeboat.py"
BATCH = "M4 part G batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
REVIEW_ROUND = "r25"
VARIANT = "b"
LEVEL_DIR = DESIGN / "campaign" / "act-1-first-contact" / "level-07-brood-carrier"
TOW = yaml.safe_load((LEVEL_DIR / "data.yaml").read_text(encoding="utf-8"))["tows"][0]
BOAT, POD, CABLE_BOX = tuple(TOW["boat"]), tuple(TOW["pod"]), tuple(TOW["cable"])
TETHER = TOW["tether"][1]
CABLE = (CABLE_BOX[0], CABLE_BOX[1] + 4)       # 2 px tucked under the boat and the pod each
POD_CANVAS = (44, 44)                          # the pod's corner posts reach 20.7 px from its centre
HEADINGS = 24
HITS = TOW["hits"]
COLOURS = 48
STROBE = (0.4, 0.75, 1.0)
STROBES = [(-15.0, 5.0), (15.0, 5.0)]          # model units from the boat's centre, y up
HALO_R = 6


def render(scene, mats, size, rim=False):
    hi, factor = artkit.render_hi(scene, mats, size, float(size[0]), z_top=float(max(size)), steps=160)
    arr = np.array(artkit.native(hi, factor, crisp=60)).astype(np.float64)
    return r25.gt.light_rim(arr) if rim else arr


def to_img(arr):
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")


# --------------------------------------------------------------------------- the boat

def boat(lit):
    return to_img(render(r25.boat_b(), r25.materials(VARIANT, beacon=lit), BOAT))


def boat_glow():
    """The concept's strobe halo (stepped, premultiplied) at both strobes, on the boat's canvas."""
    w, h = BOAT
    out = np.zeros((h, w, 3))
    halo = r25.halo(HALO_R, STROBE)
    for ox, oy in STROBES:
        r25.add(out, halo, w / 2 + ox - HALO_R, h / 2 - oy - HALO_R)
    return premultiplied(out)


def premultiplied(rgb):
    rgb = np.round(np.clip(rgb, 0, 255))
    alpha = np.where(rgb.max(axis=-1) >= 1, 255, 0)
    return Image.fromarray(np.dstack([rgb, alpha]).astype(np.uint8), "RGBA")


# --------------------------------------------------------------------------- the pod

def pod_open():
    """The crate-pod after the crate fell out: the hazard lid blown off, the hold an empty dark box
    inside the olive walls; the corner posts, bridle and tow ring as before."""
    def scene(p):
        body = sd_box(p, (0, 1.5, 0), (12.5, 12.5, 6.0), 1.5)
        hold = sd_box(p, (0, 1.5, 5.0), (9.6, 9.6, 6.5), 0.6)        # open at the top, floor at z -1.5
        floor = np.where(p[:, 2] < -0.8, r25.CHAR, r25.OLIVE)
        items = [(np.maximum(body, -hold), floor)]
        for sx in (-1, 1):
            for sy in (-1, 1):
                items.append((sd_box(p, (sx * 11.5, 1.5 + sy * 11.5, 0.8), (2.4, 2.4, 6.6), 0.5), r25.DARK))
            items.append((r25.sd_capsule(p, (sx * 9.5, -9.5, 4.0), (sx * 0.8, -14.2, 2.0), 0.6), r25.CABLE_M))
        items.append((r25.ring(p, (0, -14.3, 2.0), 1.7, 0.9, 0.6), r25.STEEL))
        return union(*items, k=0.3)
    return scene


def pod(k):
    """Heading k (0-23 closed, 24-47 open): the model turned clockwise on the screen by 15 degrees
    per heading; the materials' patterns (the lid's hazard stripes) turn with it."""
    heading = k % HEADINGS
    model = r25.pod_b() if k < HEADINGS else pod_open()
    turn = -2 * np.pi * heading / HEADINGS                    # rotate_z turns counter-clockwise

    def scene(p):
        return model(rotate_z(p, turn))
    mats = [replace(m, pattern=(lambda p, n, f=m.pattern: f(rotate_z(p, turn), n))) if m.pattern else m
            for m in r25.materials(VARIANT)]
    return to_img(render(scene, mats, POD_CANVAS, rim=True))


# --------------------------------------------------------------------------- the cable

def cable(hits):
    arr = render(r25.cable_b(hits), r25.materials(VARIANT), CABLE)
    if 1 <= hits < HITS:                          # the concept's scorch on the coupler (variant b)
        arr = r25.gt.scorch(arr, [(8, 22 + k * 2.5, 2.4 + hits * 0.5) for k in range(hits)], 250 + hits)
    return to_img(arr)


def cable_glow(upper):
    return premultiplied(r25.strip_glow(1.0, upper))


# --------------------------------------------------------------------------- build

def _job(job):
    kind, i = job
    return {"boat": lambda s: boat(s == 0), "pod": pod, "cable": cable}[kind](i)


def build():
    jobs = ([("boat", s) for s in range(2)] + [("cable", h) for h in range(HITS + 1)]
            + [("pod", k) for k in range(2 * HEADINGS)])
    with ProcessPoolExecutor() as pool:
        out = list(pool.map(_job, jobs))
    boats, cables, pods = out[:2], out[2:2 + HITS + 1], out[2 + HITS + 1:]
    for name, frames, size in (("lifeboat", boats, BOAT), ("lifeboat-cable", cables, CABLE),
                               ("lifeboat-pod", pods, POD_CANVAS)):
        for img in frames:
            if img.size != size:
                raise ValueError(f"{name}: rendered {img.size}, expected {size}")
    done = artkit.quantize_set(boats + cables + pods, COLOURS)          # one palette for the tow
    artkit.write_frames("lifeboat", done[:2], SOURCE)
    artkit.write_frames("lifeboat-cable", done[2:2 + HITS + 1], SOURCE)
    artkit.write_frames("lifeboat-pod", done[2 + HITS + 1:], SOURCE)
    artkit.write_frames("lifeboat-glow", artkit.quantize_set([boat_glow()], 16), SOURCE, single=True)
    artkit.write_frames("lifeboat-cable-glow", artkit.quantize_set([cable_glow(True), cable_glow(False)], 16),
                        SOURCE)
    for name in ("lifeboat", "lifeboat-glow", "lifeboat-pod", "lifeboat-cable", "lifeboat-cable-glow"):
        frames = artkit.load_frames(name)
        print(f"{name}: {len(frames)} frames {frames[0].size}, {artkit.colour_count(frames)} colours")


# --------------------------------------------------------------------------- review

FIELD_W, FIELD_H = r25.FIELD_W, r25.FIELD_H
FALL, SLIP, RELEASE_Y, CRATE_DRIFT = 60.0, 30.0, 160.0, 40.0   # vanguard.sim.Tow and the hidden crate
TUMBLE = 150.0                                                 # deg/s clockwise (TowLooks.TUMBLE)
STROBE_PERIOD, STROBE_ON = 1.0, 0.16                           # TowLooks
STRIP_HZ, GLINT_PERIOD, GLINT_STEP = 1.2, 2.0, 0.08


class Field:
    """Level 07's play field around the tow as the game draws it (TowLooks): the cable frame by
    hits taken, the pod hanging (frame 0) or tumbling loose on Tow's fall, the boat with its strobe
    frame, the glows added after each sprite; the shots, sparks and ship over it."""

    def __init__(self):
        import backdrop_l07 as bl
        self.bl = bl
        self.backdrop, source = bl.level_backdrop()
        if source == "data.yaml":
            bl.SECTION_OF = [s["tiles"] for s in bl.SECTIONS]
        self.boat = artkit.load_frames("lifeboat")
        self.boat_glow = artkit.load_frames("lifeboat-glow")[0]
        self.pod = artkit.load_frames("lifeboat-pod")
        self.cable = artkit.load_frames("lifeboat-cable")
        self.cable_glow = artkit.load_frames("lifeboat-cable-glow")
        load = artkit.load_frames
        self.ship, self.flame, self.shot = load("ship")[2], load("engine-flame"), load("scatter-vulcan-shot")[0]
        self.spark, self.crate, self.glint = load("ballistic-impact"), load("pickup-crate"), load("glint")
        self.shots, self.hits = r25.simulate()
        self.cut = self.hits[-1][0]
        cx, cy = r25.tow_at(self.cut)
        self.cut_pod = (cx, cy - TETHER)
        self.slip = SLIP if self.cut_pod[0] < FIELD_W / 2 else -SLIP
        s = 0.0
        while self.pod_at(self.cut + s)[1] < FIELD_H - RELEASE_Y:
            s += 1 / 60
        self.release = self.cut + s
        self.release_at = self.pod_at(self.release)

    def pod_at(self, t):
        s = t - self.cut
        x0, y0 = self.cut_pod
        return x0 + (r25.VX + self.slip) * s, y0 - r25.VY * s + FALL * s * s / 2

    def frame(self, t):
        base = np.array(self.bl.composite(self.backdrop, t).convert("RGBA")).astype(np.float64)
        img = to_img(base)
        bx, by = r25.tow_at(t)
        taken = sum(1 for h in self.hits if h[0] <= t)
        holding = taken < HITS
        cx, cy = bx, by - TETHER / 2
        cab = self.cable[min(taken, HITS - 1) if holding else HITS]
        centre(img, cab, cx, cy)
        if holding:
            pulse = 0.55 + 0.45 * np.sin(2 * np.pi * STRIP_HZ * (t - r25.T0))
            glow = self.cable_glow[0 if taken == 0 else 1]
            centre(img, dimmed(glow, pulse), cx, cy, add=True)
            g = (t - r25.T0) % GLINT_PERIOD
            if g < GLINT_STEP * len(self.glint):
                centre(img, self.glint[int(g / GLINT_STEP)], cx + 3, cy - 7, add=True)
            centre(img, self.pod[0], bx, by - TETHER)
        else:
            px, py = self.pod_at(t)
            heading = int(TUMBLE * (t - self.cut) / (360 / HEADINGS)) % HEADINGS
            centre(img, self.pod[heading + (HEADINGS if t >= self.release else 0)], px, py)
        lit = (t - r25.T0) % STROBE_PERIOD < STROBE_ON
        centre(img, self.boat[0 if lit else 1], bx, by)
        if lit:
            centre(img, self.boat_glow, bx, by, add=True)
        if t >= self.release:
            x, y = self.release_at
            centre(img, self.crate[int(t * 10) % len(self.crate)], x, y + CRATE_DRIFT * (t - self.release))
        for t0, x, t_end, _ in self.shots:
            if t0 <= t < t_end:
                centre(img, self.shot, x, r25.SHIP_Y - 24 - r25.SHOT_SPEED * (t - t0), add=True)
        for th, x, y in self.hits:
            if th <= t < th + 0.2:
                centre(img, self.spark[min(3, int((t - th) / 0.05))], x, y, add=True)
        sx = r25.ship_x(t)
        centre(img, self.flame[int(t * 20) % len(self.flame)], sx, r25.SHIP_Y + 24, add=True)
        centre(img, self.ship, sx, r25.SHIP_Y)
        return img


def dimmed(glow, k):
    a = np.array(glow).astype(np.float64)
    a[..., :3] *= k
    return Image.fromarray(a.astype(np.uint8), "RGBA")


def centre(img, spr, x, y, add=False):
    at = (int(round(x - spr.width / 2)), int(round(y - spr.height / 2)))
    if add:
        img.paste(artkit.add_light(img, spr, at))
    else:
        sprite.paste(img, spr, *at)


def review():
    artkit.REVIEW_ROUND = REVIEW_ROUND
    boats, glow = artkit.load_frames("lifeboat"), artkit.load_frames("lifeboat-glow")
    pods, cables = artkit.load_frames("lifeboat-pod"), artkit.load_frames("lifeboat-cable")
    cable_glows = artkit.load_frames("lifeboat-cable-glow")
    field = Field()
    first, second = field.hits[0][0], field.hits[1][0]
    times = [first - 0.3, first + 0.03, second + 0.03, field.cut + 0.03, field.cut + 0.5, field.release + 0.1,
             field.release + 0.8]
    labels = []
    views = []
    for t in times:
        bx, by = r25.tow_at(t)
        x0 = int(np.clip(bx - 110, 0, FIELD_W - 220))
        views.append(field.frame(t).crop((x0, 0, x0 + 220, FIELD_H)))
        labels.append(f"T={t:.2f}")
    rows = [("LIFEBOAT: STROBES LIT, DARK", boats, 5, False),
            ("LIFEBOAT GLOW: THE STROBES' HALO, WITH THE LIT FRAME (ADDITIVE)", glow, 5, True),
            ("LIFEBOAT CABLE: INTACT, 1 HIT, 2 HITS, CUT", cables, 5, False),
            ("LIFEBOAT CABLE GLOW: THE LIGHT STRIP, INTACT / HIT; PULSED AT 1.2 HZ (ADDITIVE)", cable_glows, 5, True),
            ("LIFEBOAT POD: HANGING (0) AND THE TUMBLE, 15 DEG CLOCKWISE PER FRAME, 0.1 S EACH AT 150 DEG/S",
             pods[:12], 3, False),
            ("LIFEBOAT POD: TUMBLE (CONTINUED)", pods[12:HEADINGS], 3, False),
            ("LIFEBOAT POD: EMPTY, THE LID BLOWN OFF, AFTER THE CRATE FELL OUT", pods[HEADINGS:HEADINGS + 12], 3, False),
            ("LIFEBOAT POD: EMPTY (CONTINUED)", pods[HEADINGS + 12:], 3, False),
            ("IN THE LEVEL, 1X: BEFORE THE HITS, 1 HIT, 2 HITS, THE CUT, THE POD TUMBLING, THE CRATE OUT, FALLING",
             views, 1, False)]
    sheet = artkit.review_sheet("LIFEBOAT TOW - FINAL SPRITES (ROUND 25 VARIANT B)", rows, width=1640, batch=BATCH)
    gif = []
    for t in np.arange(22.6, field.release + 2.5, 0.08):
        gif.append(field.frame(float(t)).crop((30, 0, 390, FIELD_H)).convert("RGB"))
    artkit.save_review(sheet, gif, LEVEL_DIR / "concept", "lifeboat", fps=12.5)
    print("times:", ", ".join(labels))


if __name__ == "__main__":
    if "--review" not in sys.argv[1:]:
        build()
    review()
