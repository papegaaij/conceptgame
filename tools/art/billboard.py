#!/usr/bin/env python3
"""Production art: Level 08's flickering billboard, the concept chosen in concept round 30 (variant a,
"neon sky-sign"; design/campaign/act-2-homefront/level-08-neon-skyline, Ground targets and Secrets:
"Billboard cache").

Outputs (assets/sprites/; the sprite size is the concept's, the hit box comes from the `billboard`
ground target in Level 08's data.yaml):
  billboard_0..2.png        76x52 a dark panel tilted up toward the aircar lanes on a steel A-frame,
                            the unlit neon "LAGOS" (red glass) over "NEVER SLEEPS" (white glass),
                            four floodlights on its top edge, an amber/black hazard catwalk and
                            foot plates (the loot targets' matte cue) and the 1 px light rim:
                            intact; hit (scorched, a crack, the last S and "PS" tubes and one
                            floodlight dead); toppled (the panel down and slewed on its snapped
                            frame, its tubes shattered, the catwalk kicked aside and the CDF floor
                            stash under it sprung open). One palette over the three
  billboard-glow_0..31.png  76x52, additive: the neon's flicker baked as a loop, one frame per 1/8 s
                            step (the game cycles a trigger's -glow frames at 8 fps,
                            vanguard.game.render.GroundGlow): 0-15 the intact cycle, 16-31 the
                            cycle after the first hit (the dead tubes dark). The 32 frames are the
                            concept's step table over its eight glows (lit, buzz with "NEVER
                            SLEEPS" out, dim, off; the same after a hit): identical frames share one
                            atlas region (the packer aliases them)
  design/campaign/act-2-homefront/level-08-neon-skyline/concept/billboard-final-r30-a.png/.gif

The model, materials, lettering, scorch and glows are the concept round's (tools/concept/props_r30.py,
imported unchanged; its `render` is swapped for the quality bar's unquantised 8x render and one
shared palette, and its baked (5, 7) shadow is left out: art direction, render pipeline step 6). The
frames are moved so the standing sign's silhouette is centred on the sprite (the concept kept room
for the shadow at the bottom right), the glows with them.

The game draws the frame (hit from the first hit, toppled once spent, with the white hit flash),
the shared loot glint at its top-left quarter, then the glow frame of the step over it (additive)
until it is spent; the crate drops at its centre when the third hit topples it.

Run: python3 tools/art/billboard.py [--review]   (~1 min; the review needs backdrop_l08.py's images)
"""
import sys

import numpy as np
import yaml
from PIL import Image

import artkit
from artkit import DESIGN, sdf, sprite

import props_r30 as r30  # noqa: E402  (concept script, imported unchanged)

SCRIPT = "billboard.py"
BATCH = "M5 part B batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
REVIEW_ROUND = "r30"
VARIANT = "a"
LEVEL_DIR = DESIGN / "campaign" / "act-2-homefront" / "level-08-neon-skyline"
TARGET = next(g for g in yaml.safe_load((LEVEL_DIR / "data.yaml").read_text(encoding="utf-8"))["ground_targets"]
              if g["target"] == "billboard")
HIT_BOX = tuple(TARGET["size"])
HITS = TARGET["hits"]
SIZE = r30.BB
FACTOR = 8                       # the quality bar's 8x (the concept rendered this sprite at 6x)
COLOURS = 40
GLOW_COLOURS = 32
GLOW_FPS = 8                     # vanguard.game.render.GroundGlow.FRAMES_PER_SECOND
FLICKER = r30.FLICKER            # glow per step: 0-3 intact lit, buzz, dim, off; 4-7 after a hit
STEPS = len(FLICKER["intact"])


def _render(scene, mats, size, factor=None):
    """The concept's render at the quality bar: 8x, unquantised (one palette is cut over the set)."""
    w, h = size
    hi = sdf.render(scene, mats, (w * FACTOR, h * FACTOR), float(w), z_top=float(max(w, h)), steps=160)
    return np.array(artkit.native(hi, FACTOR, crisp=60)).astype(np.float64)


r30.render = _render
r30.bake_shadow = lambda arr, dx, dy, opacity=0.42: arr      # shadows are drawn at runtime, never baked


def to_img(arr):
    return Image.fromarray(np.clip(np.round(arr), 0, 255).astype(np.uint8), "RGBA")


def premultiplied(rgb):
    rgb = np.round(np.clip(rgb, 0, 255))
    alpha = np.where(rgb.max(axis=-1) >= 1, 255, 0)
    return Image.fromarray(np.dstack([rgb, alpha]).astype(np.uint8), "RGBA")


def centring(frame):
    """The (dx, dy) that centres the standing sign's opaque box on the sprite."""
    ys, xs = np.nonzero(frame[..., 3] > 0)
    w, h = SIZE
    return int(round((w - 1 - xs.min() - xs.max()) / 2)), int(round((h - 1 - ys.min() - ys.max()) / 2))


def moved(arr, dx, dy):
    """``arr`` shifted by (dx, dy) px; what would leave the canvas must be empty."""
    out = np.roll(arr, (dy, dx), axis=(0, 1))
    back = np.roll(out, (-dy, -dx), axis=(0, 1))
    lost = np.zeros(arr.shape[:2], bool)
    if dy > 0:
        lost[-dy:] = True
    elif dy < 0:
        lost[:-dy] = True
    if dx > 0:
        lost[:, -dx:] = True
    elif dx < 0:
        lost[:, :-dx] = True
    if np.any(back[lost] != 0):
        raise ValueError(f"moving by ({dx}, {dy}) would cut the sprite")
    return out


def build():
    frames = r30.bb_frames(VARIANT)                    # intact, hit, toppled (float RGBA, no shadow)
    glows = r30.bb_glows(VARIANT)                      # the eight additive light fields (float RGB)
    dx, dy = centring(frames[0])
    frames = [moved(f, dx, dy) for f in frames]
    glows = [moved(g, dx, dy) for g in glows]
    for arr in frames + glows:
        if arr.shape[:2] != SIZE[::-1]:
            raise ValueError(f"rendered {arr.shape[1]}x{arr.shape[0]}, expected {SIZE}")
    done = artkit.quantize_set([to_img(f) for f in frames], COLOURS)
    artkit.write_frames("billboard", done, SOURCE)
    lights = artkit.quantize_set([premultiplied(g) for g in glows], GLOW_COLOURS)
    loop = [lights[k] for k in FLICKER["intact"]] + [lights[k] for k in FLICKER["hit"]]
    artkit.write_frames("billboard-glow", loop, SOURCE)
    print(f"moved by ({dx}, {dy})")
    for name in ("billboard", "billboard-glow"):
        got = artkit.load_frames(name)
        print(f"{name}: {len(got)} frames {got[0].size}, {artkit.colour_count(got)} colours")


# --------------------------------------------------------------------------- review

SHIP_Y = 470
SHOT_SPEED = 700.0              # scatter vulcan (design/player/weapons)
PICKUP_DRIFT = 40.0             # design/player/data.yaml pickup_drift_speed
HIT_FLASH = 2 / 60              # LevelRenderer.HIT_FLASH_TICKS
GLINT_PERIOD, GLINT_STEP = 2.0, 3 / 60      # LevelRenderer.GLINT_PERIOD_TICKS, GLINT_FRAME_TICKS
T_ENTRY, X = TARGET["at"][0]


class Field:
    """Level 08's play field at the billboard as the game draws it: the backdrop (backdrop_l08.py's
    composite) with the billboard on the ground layer (its frame by hits taken, the white hit flash,
    the loot glint, the glow step over it), the low-air layer, then the shots, sparks, the crate and
    the ship, then high-air."""

    def __init__(self):
        import backdrop_l08 as bl
        self.bl = bl
        self.backdrop, source = bl.level_backdrop()
        if source == "data.yaml":
            bl.SECTION_OF = [s["tiles"] for s in bl.SECTIONS]
        self.placed = bl.expanded(self.backdrop)
        load = artkit.load_frames
        self.frames, self.glow = load("billboard"), load("billboard-glow")
        self.ship, self.flame, self.shot = load("ship")[2], load("engine-flame"), load("scatter-vulcan-shot")[0]
        self.spark, self.crate, self.glint = load("ballistic-impact"), load("pickup-crate"), load("glint")
        self.shots, self.hits = self.simulate()
        self.spent = self.hits[-1][0]

    def y(self, t):
        """The billboard's centre on the screen (rows down): its hit box's top enters at its t."""
        return self.bl.scroll_at(t) - self.bl.scroll_at(T_ENTRY) - HIT_BOX[1] / 2

    def ship_x(self, t):
        s = t - T_ENTRY
        if s < 1.6:
            return X - 70
        if s < 1.9:
            return X - 70 * (1 - (s - 1.6) / 0.3)
        return X

    SHOTS = [T_ENTRY + 0.9 + 0.08 * k for k in range(8)] + [T_ENTRY + 2.0 + 0.55 * k for k in range(4)]

    def simulate(self):
        shots, hits = [], []
        for t0 in self.SHOTS:
            x, y0 = self.ship_x(t0), SHIP_Y - 24
            t_end = t0 + (y0 + 20) / SHOT_SPEED
            if len(hits) < HITS:
                for k in range(1, 400):
                    t = t0 + k / 240
                    y = y0 - SHOT_SPEED * (t - t0)
                    if y < -20:
                        break
                    if abs(x - X) < HIT_BOX[0] / 2 and abs(y - self.y(t)) < HIT_BOX[1] / 2:
                        t_end = t
                        hits.append((t, x, y))
                        break
            shots.append((t0, x, t_end))
        return shots, hits

    def taken(self, t):
        return sum(1 for h in self.hits if h[0] <= t)

    def ground(self, img, t):
        by, taken = self.y(t), self.taken(t)
        spent = taken >= HITS
        frame = self.frames[2 if spent else 1 if taken else 0]
        if any(th <= t < th + HIT_FLASH for th, _, _ in self.hits):
            frame = r30.flash_white(frame)
        centre(img, frame, X, by)
        if not spent:
            g = (t + int(X) * 7 / 60) % GLINT_PERIOD
            if g < GLINT_STEP * len(self.glint):
                centre(img, self.glint[int(g / GLINT_STEP)], X - HIT_BOX[0] / 4, by - HIT_BOX[1] / 4, add=True)
            step = int(t * GLOW_FPS) % STEPS + (STEPS if taken else 0)
            centre(img, self.glow[step], X, by, add=True)

    def air(self, img, t):
        if t >= self.spent:
            s = t - self.spent
            centre(img, self.crate[int(t * 10) % len(self.crate)], X, self.y(self.spent) + PICKUP_DRIFT * s)
        for t0, x, t_end in self.shots:
            if t0 <= t < t_end:
                centre(img, self.shot, x, SHIP_Y - 24 - SHOT_SPEED * (t - t0), add=True)
        for th, x, y in self.hits:
            if th <= t < th + 0.2:
                centre(img, self.spark[min(3, int((t - th) / 0.05))], x, y, add=True)
        sx = self.ship_x(t)
        centre(img, self.flame[int(t * 20) % len(self.flame)], sx, SHIP_Y + 24, add=True)
        centre(img, self.ship, sx, SHIP_Y)

    def frame(self, t):
        """backdrop_l08.composite with the billboard on the ground and the flyers over low-air."""
        bl, backdrop, placed = self.bl, self.backdrop, self.placed
        frame = np.zeros((bl.SCREEN, bl.W, 4))
        frame[..., 3] = 255
        look_a, look_b, weight = bl.looks_at(backdrop, t)
        for layer in ("deep", "far"):
            bl.draw_layer_tiles(backdrop, frame, layer, bl.java_round(bl.scroll_at(t) * backdrop["scroll_factors"][layer]))
            bl.draw_pieces(backdrop, placed, frame, layer, t)
        veil = look_a["haze"] + (look_b["haze"] - look_a["haze"]) * weight
        hz = np.array([int(backdrop["haze_colour"][i:i + 2], 16) for i in (0, 2, 4)], float)
        frame[..., :3] = frame[..., :3] * (1 - veil) + hz * veil
        bl.draw_layer_tiles(backdrop, frame, "ground", bl.java_round(bl.scroll_at(t)))
        bl.draw_pieces(backdrop, placed, frame, "ground", t)
        bl.draw_towers(backdrop, placed, frame, t)
        frame = self.layer(frame, self.ground, t)
        bl.draw_layer_tiles(backdrop, frame, "low-air", bl.java_round(bl.scroll_at(t) * backdrop["scroll_factors"]["low-air"]))
        bl.draw_banks(backdrop, frame, look_a, look_b, weight, "banks", "low-air", t)
        bl.draw_pieces(backdrop, placed, frame, "low-air", t)
        frame = self.layer(frame, self.air, t)
        scroll = bl.java_round(bl.scroll_at(t) * backdrop["scroll_factors"]["high-air"])
        bl.draw_layer_tiles(backdrop, frame, "high-air", scroll, additive=0.4)
        bl.draw_pieces(backdrop, placed, frame, "high-air", t, additive=0.4)
        bl.draw_banks(backdrop, frame, look_a, look_b, weight, "wisps", "high-air", t, additive=0.4)
        return Image.fromarray(np.clip(frame[..., :3], 0, 255).astype(np.uint8), "RGB")

    @staticmethod
    def layer(frame, draw, t):
        img = Image.fromarray(np.clip(frame, 0, 255).astype(np.uint8), "RGBA")
        draw(img, t)
        return np.array(img).astype(np.float64)


def centre(img, spr, x, y, add=False):
    at = (int(round(x - spr.width / 2)), int(round(y - spr.height / 2)))
    if add:
        img.paste(artkit.add_light(img, spr, at))
    else:
        sprite.paste(img, spr, *at)


def lit(frame, glow):
    return artkit.add_light(frame, glow)


def review():
    artkit.REVIEW_ROUND = REVIEW_ROUND
    frames, glow = artkit.load_frames("billboard"), artkit.load_frames("billboard-glow")
    distinct = [glow[FLICKER["intact"].index(k)] for k in range(4)] + [glow[STEPS + FLICKER["hit"].index(k)]
                                                                         for k in range(4, 8)]
    field = Field()
    first, second = field.hits[0][0], field.hits[1][0]
    times = [T_ENTRY + 1.0, first, first + 0.3, second + 0.05, field.spent + 0.1, field.spent + 0.6]
    views = []
    for t in times:
        x0 = int(np.clip(X - 110, 0, field.bl.W - 220))
        views.append(field.frame(t).crop((x0, 0, x0 + 220, field.bl.SCREEN)).convert("RGBA"))
    rows = [("BILLBOARD: INTACT, HIT, TOPPLED (UNLIT GLASS; NO BAKED SHADOW)", frames, 3, False),
            ("BILLBOARD-GLOW: THE EIGHT DISTINCT GLOWS, INTACT LIT, BUZZ, DIM, OFF; THE SAME AFTER A HIT (ADDITIVE)",
             distinct, 2, True),
            ("LIT: INTACT + LIT, BUZZ, DIM; HIT + LIT, BUZZ, DIM", [lit(frames[0], distinct[k]) for k in range(3)]
             + [lit(frames[1], distinct[4 + k]) for k in range(3)], 2, False),
            ("BILLBOARD-GLOW_0..15 OVER THE INTACT FRAME: THE LOOP, 1/8 S A STEP (2 S)",
             [lit(frames[0], g) for g in glow[:STEPS]], 1, False),
            ("BILLBOARD-GLOW_16..31 OVER THE HIT FRAME: THE LOOP AFTER THE FIRST HIT",
             [lit(frames[1], g) for g in glow[STEPS:]], 1, False),
            ("IN THE LEVEL, 1X: APPROACHING, THE FIRST HIT (FLASH), HIT AND FLICKERING, THE SECOND HIT, TOPPLED "
             "(THE CRATE AT ITS CENTRE), THE CRATE DRIFTING", views, 1, False)]
    sheet = artkit.review_sheet("BILLBOARD - FINAL SPRITES (ROUND 30 VARIANT A)", rows, width=1400, batch=BATCH)
    gif = []
    for t in np.arange(T_ENTRY + 0.4, field.spent + 2.0, 0.08):
        gif.append(field.frame(float(t)).crop((int(X) - 180, 0, int(X) + 150, field.bl.SCREEN)).convert("RGB"))
    artkit.save_review(sheet, gif, LEVEL_DIR / "concept", "billboard", fps=12.5)
    print("times:", ", ".join(f"{t:.2f}" for t in times))


if __name__ == "__main__":
    if "--review" not in sys.argv[1:]:
        build()
    review()
