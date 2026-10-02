#!/usr/bin/env python3
"""Production art: the speakers' portraits of Acts 1-2 (design/story/characters, design/ui/briefing,
design/ui/hud), in the chosen style B with retained colour (round 04) and the round-08 extras.

Outputs (assets/sprites/portraits/, packed onto the shared sprite pages as ``portraits/<name>``):
  radio-<slug>-<expression>.png      72x72 radio portrait (HUD), for okafor, rook, varga in
                                     neutral, grim and fierce; generic-cdf and generic-civilian
                                     in neutral
  briefing-<slug>-<expression>.png   144x144 briefing portrait (the 72 px portrait at 2x with CRT
                                     scanlines), for the briefing speakers okafor and varga
  radio-the-choir-neutral_0..31.png  the Choir's interference glyph, a 32-frame loop (12 fps)
  design/story/characters/concept/portraits-final-r13-a.png/.gif   review sheet and loop

The busts are the concept SDF busts (tools/concept/portraits_r03.py, the generic speakers of
portraits_r08.py), imported unchanged; the expressions replace their face with this script's
``face``, the concept face with expression parameters: **grim** lowers the head a little, lowers
and draws in the inner brows, narrows the eyes and pulls the mouth's corners down; **fierce**
slants the brows hard, opens the eyes wide and opens the mouth (a shout, the upper teeth showing). Neutral is the concept face
unchanged. The style-B treatment is round 04's (portraits_r04.style_b2: the channel tint over the
character's own colours, soft ordered dither, outline, interference rows, a faint channel glow),
split so that **one 36-colour palette** serves all expressions of a character and an expression
change never shifts its colours; the interference rows are seeded per character, so they stay put
too. The Choir is round 08's glyph (portraits_r08.choir_frame), all 32 frames of its loop.

Vorne does not speak in Acts 1-2 and gets his portraits with Act 6.

Run: python3 tools/art/portraits.py [--review]   (~1 min; --review only rebuilds the sheet)
"""
import sys
from concurrent.futures import ProcessPoolExecutor
from contextlib import contextmanager

import numpy as np
from PIL import Image

import artkit
from artkit import ROOT, SPRITES, sprite

import portraits_r03 as r03  # noqa: E402  (concept scripts, imported unchanged)
import portraits_r04 as r04  # noqa: E402
import portraits_r08 as r08  # noqa: E402  (registers the generic speakers in r03.CAST)
from render import raster, sdf  # noqa: E402

SCRIPT = "portraits.py"
SOURCE = artkit.source_note(SCRIPT, "UI batch")
ROUND = "r13"
OUT = SPRITES / "portraits"
CONCEPT = ROOT / "design" / "story" / "characters" / "concept"
N = r04.N
EXPRESSIONS = ("neutral", "grim", "fierce")
# speaker slug -> (expressions, sizes): the main cast in three expressions, the minor speakers
# neutral; the briefing size only for who speaks in a briefing of Acts 1-2
SPEAKERS = {
    "okafor": (EXPRESSIONS, ("radio", "briefing")),
    "rook": (EXPRESSIONS, ("radio",)),
    "varga": (EXPRESSIONS, ("radio", "briefing")),
    "generic-cdf": (("neutral",), ("radio",)),
    "generic-civilian": (("neutral",), ("radio",)),
}
CHOIR = "the-choir"

# Face parameters per expression, as offsets on the concept face (world units; the head is about
# 0.6 across, 1 unit = 48 px at 72 px): the brows' inner and outer ends up (+) or down (-), the
# inner ends drawn towards the nose, the brows' radius, the upper and lower lids up (+) or down,
# and the head's extra pitch (+ lowers it: grim looks out from under the brows).
FACES = {
    "neutral": dict(brow_inner=0.0, brow_outer=0.0, brow_in=0.0, brow_r=0.013, lid=0.0, lower=0.0,
                    eyes=1.0, mouth=None, pitch=0.0),
    "grim": dict(brow_inner=-0.045, brow_outer=0.004, brow_in=0.016, brow_r=0.024, lid=-0.016, lower=0.008,
                 eyes=0.7, mouth="frown", pitch=0.07),
    "fierce": dict(brow_inner=-0.06, brow_outer=0.03, brow_in=0.02, brow_r=0.026, lid=0.01, lower=-0.002,
                   eyes=1.25, mouth="shout", pitch=0.03),
}


# --------------------------------------------------------------------------- the expressive face

def face(q, skin_k=0.06, jaw=1.0, mouth="neutral", eyes_open=1.0, expression="neutral"):
    """portraits_r03.face with an expression: the same head, the brows, lids and mouth moved by
    ``FACES[expression]``; the caller's mouth stays for neutral (Rook's grin, Vorne's smile)."""
    SKIN, EYE, IRIS, LIPS, BROW = r03.SKIN, r03.EYE, r03.IRIS, r03.LIPS, r03.BROW
    f = FACES[expression]
    mouth = f["mouth"] or mouth
    eyes_open *= f["eyes"]
    head = sdf.sd_ellipsoid(q, (0, 0.47, -0.02), (0.29, 0.37, 0.32))
    jawd = sdf.sd_ellipsoid(q, (0, 0.30, 0.03), (0.215 * jaw, 0.21, 0.25))
    parts = [
        (sdf.sd_ellipsoid(q, (-0.15, 0.40, 0.17), (0.085, 0.06, 0.08)), 0.05),
        (sdf.sd_ellipsoid(q, (0.15, 0.40, 0.17), (0.085, 0.06, 0.08)), 0.05),
        (sdf.sd_capsule(q, (0, 0.49, 0.29), (0, 0.375, 0.365), 0.028, 0.042), 0.03),
        (sdf.sd_ellipsoid(q, (0, 0.365, 0.325), (0.058, 0.03, 0.04)), 0.03),
        (sdf.sd_capsule(q, (-0.20, 0.54 + f["brow_inner"] * 0.4, 0.22), (0.20, 0.54 + f["brow_inner"] * 0.4, 0.22),
                        0.035), 0.05),
        (sdf.sd_ellipsoid(q, (0, 0.175, 0.19), (0.07, 0.05, 0.05)), 0.04),
        (sdf.sd_ellipsoid(q, (-0.29, 0.44, -0.03), (0.035, 0.075, 0.055)), 0.02),
        (sdf.sd_ellipsoid(q, (0.29, 0.44, -0.03), (0.035, 0.075, 0.055)), 0.02),
    ]
    skin = sdf.smin(head, jawd, skin_k)
    for d, k in parts:
        skin = sdf.smin(skin, d, k)
    for x in (-0.11, 0.11):
        skin = np.maximum(skin, -sdf.sd_ellipsoid(q, (x, 0.463, 0.31), (0.062, 0.03 * eyes_open + 0.01, 0.05)))
    inside = None
    if mouth == "grin":       # crooked grin: right corner up
        cut = np.minimum(sdf.sd_capsule(q, (-0.075, 0.268, 0.29), (0.0, 0.262, 0.31), 0.011),
                         sdf.sd_capsule(q, (0.0, 0.262, 0.31), (0.09, 0.29, 0.285), 0.013))
    elif mouth == "smile":    # faint, thin smile
        cut = np.minimum(sdf.sd_capsule(q, (-0.07, 0.272, 0.29), (0.0, 0.266, 0.31), 0.007),
                         sdf.sd_capsule(q, (0.0, 0.266, 0.31), (0.075, 0.28, 0.29), 0.007))
    elif mouth == "frown":    # tight line, both corners pulled down
        cut = np.minimum(sdf.sd_capsule(q, (-0.08, 0.243, 0.282), (0.0, 0.272, 0.31), 0.011),
                         sdf.sd_capsule(q, (0.0, 0.272, 0.31), (0.08, 0.243, 0.282), 0.011))
    elif mouth == "shout":    # open mouth: a dark cavity with the upper teeth along its top
        cut = sdf.sd_ellipsoid(q, (0, 0.252, 0.31), (0.075, 0.056, 0.07))
        inside = [(sdf.sd_ellipsoid(q, (0, 0.252, 0.255), (0.066, 0.05, 0.03)), IRIS),
                  (sdf.sd_box(q, (0, 0.293, 0.275), (0.055, 0.01, 0.02), 0.004), EYE)]
    else:
        cut = sdf.sd_capsule(q, (-0.07, 0.27, 0.29), (0.07, 0.27, 0.29), 0.008)
    lips = sdf.sd_ellipsoid(q, (0, 0.27, 0.28), (0.075, 0.03 + (0.016 if mouth == "shout" else 0), 0.035))
    skin = np.maximum(skin, -cut)
    items = [(skin, SKIN), (np.maximum(lips, -cut) + 0.004, LIPS)] + (inside or [])
    for s in (-1, 1):
        x = s * 0.11
        ball = sdf.sd_sphere(q, (x, 0.462, 0.25), 0.052)
        iris = sdf.sd_sphere(q, (x + 0.004, 0.462, 0.281), 0.029)
        lid = sdf.sd_capsule(q, (x - 0.05, 0.488 + f["lid"], 0.283), (x + 0.05, 0.49 + f["lid"], 0.28), 0.02)
        lower = sdf.sd_capsule(q, (x - 0.045, 0.438 + f["lower"], 0.286), (x + 0.045, 0.437 + f["lower"], 0.283),
                               0.012)
        items += [(ball, EYE), (iris, IRIS), (lid, SKIN), (lower, SKIN)]
        # the concept brow runs from (x - 0.06, 0.525) to (x + 0.06, 0.53); its inner end is the
        # one towards the nose
        ends = {-1: (x - 0.06, 0.525, 0.275), 1: (x + 0.06, 0.53, 0.268)}
        brow = []
        for side, (bx, by, bz) in ends.items():
            inner = side == -s
            dy = f["brow_inner"] if inner else f["brow_outer"]
            brow.append((bx - s * f["brow_in"] if inner else bx, by + dy, bz))
        items.append((sdf.sd_capsule(q, brow[0], brow[1], f["brow_r"]), BROW))
    return items, skin


@contextmanager
def expression(name):
    """The concept busts in ``name``'s expression: their scene functions look ``face`` and ``turn``
    up in the concept module when they are called, so both are swapped for the time of a render.
    The head's turn is the call that gives a pitch (the bodies turn by yaw alone); it gets the
    expression's extra pitch."""
    concept_face, concept_turn = r03.face, r03.turn
    extra = FACES[name]["pitch"]
    r03.face = lambda q, **kw: face(q, expression=name, **kw)
    r03.turn = lambda p, yaw, pitch=None, **kw: concept_turn(p, yaw, 0.0 if pitch is None else pitch + extra, **kw)
    try:
        yield
    finally:
        r03.face, r03.turn = concept_face, concept_turn


# --------------------------------------------------------------------------- style B, split

def figure(slug, busts):
    """portraits_r04.style_b2 up to its palette: the 72 px figure in the channel tint over the
    character's own colours with the soft dither, unquantised, and its 1-bit alpha."""
    bust, keyonly = busts
    ch = r03.CAST[slug]["channel"]
    ramp = channel_ramp(slug)
    small = sprite.downsample(bust, r03.RENDER // N)
    rgb, a = np.clip(small[..., :3], 0, 1), small[..., 3] > 0.5
    lum = rgb @ np.array([0.3, 0.55, 0.15])
    base = np.clip(rgb * 1.18 + 0.03, 0, 1)
    grey = base.mean(-1, keepdims=True)
    base = np.clip(grey + (base - grey) * 1.15, 0, 1)
    tint = r04.ramp_at(ramp, np.clip((lum - 0.02) / 0.75, 0, 1) ** 0.85)
    krgb = sprite.downsample(keyonly, r03.RENDER // N)[..., :3]
    mx, mn = krgb.max(-1), krgb.min(-1)
    sat = (mx - mn) / np.maximum(mx, 1e-6)
    bluish = (krgb[..., 2] > krgb[..., 0]) & (krgb[..., 2] > krgb[..., 1])
    accent = a & (sat > 0.5) & (mx > 0.35) & ~bluish
    w = np.where(accent, r04.ACCENT_TINT[ch], r04.TINT[ch])[..., None]
    col = base * (1 - w) + tint * w
    thr = r03.BAYER4[np.arange(N)[:, None] % 4, np.arange(N)[None, :] % 4] - 0.5
    col = np.clip(col + thr[..., None] * r04.DITHER, 0, 1)
    fig = Image.fromarray((col * 255 + 0.5).astype(np.uint8), "RGB").convert("RGBA")
    fig.putalpha(Image.fromarray((a * 255).astype(np.uint8)))
    return fig


def channel_ramp(slug):
    return np.array([raster.hexrgb(h) for h in r03.RAMPS[r03.CAST[slug]["channel"]]], dtype=np.float64) / 255


def screen(slug, fig):
    """portraits_r04.style_b2 after its palette: the quantised figure on the comm screen with its
    outline, interference and glow -> (72 px radio portrait, 144 px briefing portrait)."""
    ramp = channel_ramp(slug)
    arr = np.array(fig).astype(np.float64)
    a = arr[..., 3] > 0
    yy, xx = np.mgrid[0:N, 0:N]
    grid = (xx % 6 == 0) & (yy % 6 == 0)
    bg = np.where(grid[..., None], ramp[1] * 255 * 0.8, ramp[0] * 255)
    img = np.where(a[..., None], arr[..., :3], bg)
    pad = np.pad(a, 1)
    touch = (pad[:-2, 1:-1] | pad[2:, 1:-1] | pad[1:-1, :-2] | pad[1:-1, 2:]) & ~a
    img[touch] = ramp[0] * 255 * 0.4
    rng = np.random.default_rng(sum(map(ord, slug)))
    band = 50 + (sum(map(ord, slug)) % 14)
    img[band:band + 3] = np.clip(img[band:band + 3] * 0.75 + ramp[4] * 255 * 0.35, 0, 255)
    for y in rng.choice(np.arange(N - 8, N), 1, replace=False):
        img[y, rng.random(N) > 0.6] = ramp[-2] * 255
    for y in list(rng.choice(np.arange(2, 12), 1)) + list(rng.choice(np.arange(44, 66), 1)):
        img[y] = np.roll(img[y], int(rng.integers(1, 3)), axis=0)
    img = img * 0.94 + ramp[3] * 255 * 0.06
    radio = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGB").convert("RGBA")
    big = np.array(sprite.enlarge(radio, 2)).astype(np.float64)
    big[1::2, :, :3] *= 0.74
    return radio, Image.fromarray(np.clip(big, 0, 255).astype(np.uint8), "RGBA")


# --------------------------------------------------------------------------- build

def render(job):
    slug, name = job
    with expression(name):
        return figure(slug, r03.render_bust(slug))


def build():
    OUT.mkdir(parents=True, exist_ok=True)
    for old in OUT.glob("*.png"):
        old.unlink()
    jobs = [(slug, name) for slug, (names, _) in SPEAKERS.items() for name in names]
    with ProcessPoolExecutor() as pool:
        figures = dict(zip(jobs, pool.map(render, jobs)))
    count = 0
    for slug, (names, sizes) in SPEAKERS.items():
        shared = artkit.quantize_set([figures[slug, name] for name in names], r04.COLORS)
        for name, fig in zip(names, shared):
            radio, briefing = screen(slug, fig)
            artkit.save_png(radio, OUT / f"radio-{slug}-{name}.png", SOURCE)
            count += 1
            if "briefing" in sizes:
                artkit.save_png(briefing, OUT / f"briefing-{slug}-{name}.png", SOURCE)
                count += 1
    for k in range(r08.CHOIR_FRAMES):
        artkit.save_png(r08.choir_frame(k)[1], OUT / f"radio-{CHOIR}-neutral_{k}.png", SOURCE)
        count += 1
    print(f"{count} portrait files in {OUT.relative_to(ROOT)}")


# --------------------------------------------------------------------------- review

def load(name):
    return Image.open(OUT / f"{name}.png").convert("RGBA")


def review():
    cast = [slug for slug, (names, _) in SPEAKERS.items() if len(names) > 1]
    width = 16 + 3 * (144 + 12) + 3 * (72 + 8) + 3 * (144 + 8) + 16
    row = 172
    height = 60 + len(cast) * row + 150 + 100
    sheet = raster.sheet(width, height, "PORTRAITS (FINAL R13): NEUTRAL, GRIM, FIERCE",
                         f"PRODUCTION ART, UI BATCH - {ROUND.upper()}")
    raster.draw_text(sheet, 16, 38, "BRIEFING 144X144 AT 1X | RADIO 72X72 AT 1X | RADIO AT 2X; "
                                    "ONE 36-COLOUR PALETTE PER CHARACTER OVER ITS EXPRESSIONS", raster.LABEL)
    y = 56
    for slug in cast:
        names, sizes = SPEAKERS[slug]
        radios = [load(f"radio-{slug}-{n}") for n in names]
        raster.draw_text(sheet, 16, y, f"{r03.CAST[slug]['name']}  ({artkit.colour_count(radios)} COLOURS)",
                         raster.LABEL)
        x = 16
        for n in names:
            if "briefing" in sizes:
                sheet.alpha_composite(load(f"briefing-{slug}-{n}"), (x, y + 14))
            raster.draw_text(sheet, x, y + 14 + 146, n.upper(), raster.LABEL_DIM)
            x += 144 + 12
        for img in radios:
            sheet.alpha_composite(img, (x, y + 14))
            x += 72 + 8
        for img in radios:
            sheet.alpha_composite(sprite.enlarge(img, 2), (x, y + 14))
            x += 144 + 8
        y += row
    raster.draw_text(sheet, 16, y, "MINOR SPEAKERS, NEUTRAL: GENERIC CDF OFFICER, GENERIC CIVILIAN (1X, 2X)",
                     raster.LABEL)
    x = 16
    for slug in ("generic-cdf", "generic-civilian"):
        img = load(f"radio-{slug}-neutral")
        sheet.alpha_composite(img, (x, y + 14))
        sheet.alpha_composite(sprite.enlarge(img, 2), (x + 80, y + 14))
        x += 80 + 144 + 16
    y += 172 - 22
    raster.draw_text(sheet, 16, y, f"THE CHOIR: 8 OF THE {r08.CHOIR_FRAMES} LOOP FRAMES (72X72, 12 FPS IN THE GAME)",
                     raster.LABEL)
    for i in range(8):
        sheet.alpha_composite(load(f"radio-{CHOIR}-neutral_{i * 4}"), (16 + i * 80, y + 14))
    png = CONCEPT / f"portraits-final-{ROUND}-a.png"
    sheet.convert("RGB").save(png, optimize=True)
    frames = []
    for k in range(r08.CHOIR_FRAMES * 3):
        frame = Image.new("RGBA", (4 * (144 + 8) + 8, 160), (12, 14, 20, 255))
        name = EXPRESSIONS[k // r08.CHOIR_FRAMES]
        for i, slug in enumerate(cast):
            frame.alpha_composite(sprite.enlarge(load(f"radio-{slug}-{name}"), 2), (8 + i * 152, 8))
        frame.alpha_composite(sprite.enlarge(load(f"radio-{CHOIR}-neutral_{k % r08.CHOIR_FRAMES}"), 2), (8 + 3 * 152, 8))
        raster.draw_text(frame, 8, 148, name.upper(), raster.LABEL_DIM)
        frames.append(frame)
    gif = png.with_suffix(".gif")
    artkit.write_gif(frames, gif, fps=12, colors=128)
    print(f"review: {png.relative_to(ROOT)}, {gif.relative_to(ROOT)}")


if __name__ == "__main__":
    if "--review" not in sys.argv[1:]:
        build()
    review()
