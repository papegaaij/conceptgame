#!/usr/bin/env python3
"""Production art: Level 01's in-level pickups (design/player, in-level pickups; art direction,
readability rule 6).

Outputs (assets/sprites/, 8-frame loops at about 10 fps; the object plus 5 px for the outline and
halo on every side):
  pickup-salvage-small_0..7.png  28x28 credit chip, rocking +-55 degrees
  pickup-shield-cell_0..7.png    32x32 shield cell, turning
  pickup-armour-patch_0..7.png   32x32 armour patch, rocking
  pickup-crate_0..7.png          34x34 hidden crate (salvage L), turning
  pickup-salvage-medium_0..7.png 31x31 three credit chips (salvage M), rocking (M4 part B batch)
  pickup-overdrive_0..7.png      32x32 overdrive, rocking (M4 part B batch)
  design/player/concept/pickups-final-r12-a.png/.gif

Models, spin and the pulsing presentation are the chosen round-09 ones (tools/concept/vfx_r09.py:
PICKUPS, pickup_fx): each frame is its own render at 8x with the key light fixed, then a 1 px
light outline whose brightness pulses and a soft white halo stepped to four translucency levels.
The crate carries its cyan cross on all four faces it shows while it turns (the concept had it on
the top only, so the side-on frames were a plain dark box).

Run: python3 tools/art/pickups.py [name ...] [--review]   (~10 s; with names only those pickups)
"""
import sys
from concurrent.futures import ProcessPoolExecutor

import numpy as np
from PIL import Image, ImageFilter

import artkit
from artkit import DESIGN, TAU, sprite

import vfx_r08 as v8  # noqa: E402  (concept scripts, imported unchanged)
import vfx_r09 as v9  # noqa: E402
from render.sdf import sd_box, union  # noqa: E402

SCRIPT = "pickups.py"
SOURCE = artkit.source_note(SCRIPT)
FRAMES = v9.NPK
EXTENT = 2.0
TILT = -0.35
PAD = 5
OUTLINE_DIM, OUTLINE_LIT = np.array([150, 176, 205]), np.array([235, 250, 255])
HALO = (225, 245, 255)
COLOURS = 24
# asset name -> round-09 pickup key
PICKUPS = {"salvage-small": "salvage-s", "shield-cell": "shield", "armour-patch": "armour", "crate": "salvage-l",
           "salvage-medium": "salvage-m", "overdrive": "overdrive"}
# the batch each was made in (the Source note)
BATCHES = {"salvage-medium": "M4 part B batch", "overdrive": "M4 part B batch"}
CRATE_GLOW = 2                    # the concept crate's glowing stripe material


@v8.oriented
def crate_model():
    """The concept crate with its cross on the bottom and on both ends as well as the top: the
    faces that turn towards the viewer while it spins about its long axis."""
    crate, mats = v9.crate_model()

    def scene(p):
        a = np.abs(p)
        return union(
            crate(p),
            (sd_box(a, (0, 0, 0.43), (0.52, 0.06, 0.03), 0.01), CRATE_GLOW),
            (sd_box(a, (0, 0, 0.43), (0.06, 0.24, 0.03), 0.01), CRATE_GLOW),
            (sd_box(a, (0.75, 0, 0), (0.03, 0.06, 0.3), 0.01), CRATE_GLOW),
            (sd_box(a, (0.75, 0, 0), (0.03, 0.24, 0.06), 0.01), CRATE_GLOW))
    return scene, mats


MODELS = {"salvage-l": crate_model}


def render(key, i):
    _, _, _, model, kw, n = next(r for r in v9.PICKUPS if r[0] == key)
    spin = np.radians(55) * np.sin(i * TAU / FRAMES) if key in v9.FLAT else i * TAU / FRAMES
    scene, mats = MODELS.get(key, model)(tilt=TILT, spin=spin, **kw)
    hi, factor = artkit.render_hi(scene, mats, (n, n), EXTENT)
    return artkit.native(hi, factor)


def presentation(body, i):
    """The pulse of rule 6: halo and outline brighten and fade over the loop."""
    w, h = body.width + 2 * PAD, body.height + 2 * PAD
    base = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    base.alpha_composite(body, (PAD, PAD))
    solid = np.array(base.getchannel("A")) > 0
    pulse = 0.5 + 0.5 * np.cos(i * TAU / FRAMES)
    mask = Image.fromarray((solid * 255).astype(np.uint8))
    halo = np.array(mask.filter(ImageFilter.GaussianBlur(2.6))) / 255 * (0.5 + 0.6 * pulse)
    outline = np.array(mask.filter(ImageFilter.MaxFilter(3))) > 0
    outline &= ~solid
    out = np.zeros((h, w, 4))
    out[..., :3] = HALO
    out[..., 3] = np.clip(halo, 0, 1) * 255
    out[outline, :3] = OUTLINE_DIM + (OUTLINE_LIT - OUTLINE_DIM) * pulse
    out[outline, 3] = 255
    b = np.array(base)
    out[solid] = b[solid]
    return artkit.stepped_alpha(Image.fromarray(out.astype(np.uint8), "RGBA"))


def build(names):
    jobs = [(PICKUPS[name], i) for name in names for i in range(FRAMES)]
    with ProcessPoolExecutor() as pool:
        bodies = list(pool.map(render, *zip(*jobs)))
    for j, name in enumerate(names):
        loop = artkit.quantize_set(bodies[j * FRAMES:(j + 1) * FRAMES], COLOURS)
        source = artkit.source_note(SCRIPT, BATCHES.get(name, "Level 01 batch"))
        artkit.write_frames(f"pickup-{name}", [presentation(b, i) for i, b in enumerate(loop)], source)


def review():
    sets = {name: artkit.load_frames(f"pickup-{name}") for name in PICKUPS}
    if any(name in BATCHES for name in sys.argv[1:]):
        review_batch({name: frames for name, frames in sets.items() if name in BATCHES})
        return
    rows = [(name.upper().replace("-", " "), frames, 4, False) for name, frames in sets.items()]
    sheet = artkit.review_sheet("PICKUPS - FINAL SPRITES", rows)
    gif = []
    for i in range(FRAMES * 3):
        img = Image.new("RGBA", (4 * 40, 48), (20, 28, 60, 255))
        for j, frames in enumerate(sets.values()):
            sprite.paste_center(img, frames[i % FRAMES], 20 + j * 40, 24)
        gif.append(sprite.enlarge(img, 4))
    artkit.save_review(sheet, gif, DESIGN / "player" / "concept", "pickups", fps=10)


def review_batch(sets):
    """The review files of a later batch's pickups alone."""
    artkit.REVIEW_ROUND = "r15"
    rows = [(name.upper().replace("-", " "), frames, 4, False) for name, frames in sets.items()]
    sheet = artkit.review_sheet("PICKUPS - FINAL SPRITES", rows, batch="M4 part B batch")
    gif = []
    for i in range(FRAMES * 3):
        img = Image.new("RGBA", (len(sets) * 40, 48), (20, 28, 60, 255))
        for j, frames in enumerate(sets.values()):
            sprite.paste_center(img, frames[i % FRAMES], 20 + j * 40, 24)
        gif.append(sprite.enlarge(img, 4))
    artkit.save_review(sheet, gif, DESIGN / "player" / "concept", "pickups", fps=10)


if __name__ == "__main__":
    names = [a for a in sys.argv[1:] if not a.startswith("--")]
    if "--review" not in sys.argv[1:]:
        build(names or list(PICKUPS))
    review()
