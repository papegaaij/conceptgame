#!/usr/bin/env python3
"""Concept round 08 - extra radio portraits in the chosen style B with retained colour.

Outputs:
  design/story/characters/the-choir/concept/portrait-r08-a.png   the Choir: alien interference
  design/story/characters/the-choir/concept/portrait-r08-a.gif   glyph (not a face), pulsing
  design/story/characters/concept/generic-cdf-r08-a.png          generic CDF officer (convoy,
                                                                  relay and control speakers)
  design/story/characters/concept/generic-civilian-r08-a.png     generic civilian (shuttle and
                                                                  evacuation speakers)

The two generic speakers are SDF busts like the main cast (tools/concept/portraits_r03.py) run
through the round-04 style-B treatment (tools/concept/portraits_r04.py: channel tint blended
over the character's own colours, 36 colours, soft dither, interference, CRT scanlines at 2x).
They are deliberately plainer than the named cast and readable by silhouette: the officer wears
a beret and a boom mic, the civilian has loose shoulder-length hair and a padded jacket collar.

The Choir has no face: its channel shows a violet/teal interference glyph - a five-fold sigil
of bright tendrils inside rippling rings that breathe with the "song", broken by sync tears.
Same 72 px pixel treatment, palette and 2x scanlines, so it sits in the same radio panel.
Each sheet shows the 144x144 briefing portrait, the 72x72 HUD radio portrait in the HUD A frame
and a 3x enlargement, as in rounds 03-04.

Run: python3 tools/concept/portraits_r08.py [choir] [cdf] [civilian]   (~1 min)
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import hud_r02  # noqa: E402
import portraits_r03 as r03  # noqa: E402
import portraits_r04 as r04  # noqa: E402
from render import enemy_rigs as rig  # noqa: E402
from render import raster, sdf, sprite  # noqa: E402
from render.config import ROOT  # noqa: E402
from render.sdf import Material  # noqa: E402

ROUND = "CONCEPT ROUND 08"
N = r04.N
SKIN, EYE, IRIS, LIPS, HAIR, CLOTH, ACCENT, METAL, GLOW, EXTRA, BROW = range(11)
VIOLET = (192, 128, 255)
TEAL = (64, 224, 192)


# --------------------------------------------------------------------------- generic busts

def generic_cdf():
    """Generic CDF officer: buzz-cut dark hair under a tilted beret with a gold badge, boom mic,
    grey-green duty jacket with collar tabs. Plainer than the named cast."""
    mats = [
        Material((0.55, 0.36, 0.25), shininess=20, spec=0.3),            # skin
        Material((0.86, 0.84, 0.8), shininess=60, spec=0.6),             # eye white
        Material((0.08, 0.05, 0.03), shininess=90, spec=0.9),            # iris
        Material((0.42, 0.24, 0.2), shininess=30, spec=0.3),             # lips
        Material((0.07, 0.06, 0.05), shininess=8, spec=0.08, pattern=r03.noise_pattern(240, 0.5, 3)),
        Material((0.25, 0.3, 0.26), shininess=12, spec=0.15, pattern=r03.noise_pattern(90, 0.1)),
        Material((0.95, 0.72, 0.2), metal=0.8, shininess=80, spec=0.9),  # badge / tabs gold
        Material((0.16, 0.17, 0.2), metal=0.5, shininess=60, spec=0.6),  # headset
        Material((0.1, 0.1, 0.1), emission=(1.0, 0.25, 0.15)),           # mic LED (red: on air)
        Material((0.1, 0.2, 0.42), shininess=10, spec=0.1, pattern=r03.noise_pattern(150, 0.12, 5)),
        Material((0.08, 0.06, 0.05), shininess=10, spec=0.1),            # brows
    ]

    def scene(p):
        q = r03.turn(p, 0.24, 0.0)
        items, skin = r03.face(q, jaw=1.06)
        hair = sdf.sd_ellipsoid(q, (0, 0.5, -0.05), (0.3, 0.355, 0.325))
        hair = np.maximum(hair, -(q[:, 1] - 0.58 + 0.4 * np.maximum(q[:, 2], 0)))
        beret = sdf.sd_ellipsoid(q, (0.06, 0.72, -0.02), (0.36, 0.13, 0.34))
        beret = np.maximum(beret, -(q[:, 1] - 0.63 + 0.12 * (q[:, 0] - 0.05)))   # tilted rim
        badge = sdf.sd_cylinder_z(q, (-0.17, 0.7, 0.27), 0.05, 0.02)
        cup = sdf.sd_cylinder_x(q, (0.315, 0.44, -0.02), 0.07, 0.03)
        boom = sdf.sd_capsule(q, (0.32, 0.40, 0.03), (0.12, 0.28, 0.28), 0.012)
        mic = sdf.sd_sphere(q, (0.11, 0.275, 0.28), 0.024)
        led = sdf.sd_sphere(q, (0.33, 0.40, 0.04), 0.012)
        pb = r03.turn(p, 0.1)
        neck, torso = r03.body(pb, shoulders=0.82)
        collar = sdf.sd_cylinder_y(pb, (0, 0.03, -0.04), 0.2, 0.07)
        tabs = np.minimum(sdf.sd_box(pb, (-0.17, -0.02, 0.19), (0.05, 0.025, 0.012), 0.005),
                          sdf.sd_box(pb, (0.17, -0.02, 0.19), (0.05, 0.025, 0.012), 0.005))
        pocket = sdf.sd_box(pb, (-0.36, -0.42, 0.22), (0.11, 0.08, 0.02), 0.01)
        return sdf.union(*items, (hair, HAIR), (beret, EXTRA), (badge, ACCENT),
                         (sdf.smin(neck, skin, 0.05), SKIN), (torso, CLOTH), (collar, CLOTH),
                         (tabs, ACCENT), (pocket, CLOTH),
                         (cup, METAL), (boom, METAL), (mic, METAL), (led, GLOW))
    return scene, mats


def generic_civilian():
    """Generic civilian: loose shoulder-length brown hair, rust padded jacket with a high collar,
    a scarf, a smudge of soot. No uniform, no headset - a handheld radio clipped to the collar."""
    mats = [
        Material((0.8, 0.64, 0.52), shininess=18, spec=0.25),            # skin
        Material((0.86, 0.84, 0.8), shininess=60, spec=0.6),             # eye white
        Material((0.2, 0.12, 0.05), shininess=90, spec=0.9),             # iris
        Material((0.62, 0.36, 0.33), shininess=30, spec=0.3),            # lips
        Material((0.42, 0.25, 0.12), shininess=16, spec=0.2, pattern=r03.strand_pattern(90, 0.35)),
        Material((0.62, 0.2, 0.1), shininess=16, spec=0.2, pattern=r03.noise_pattern(70, 0.12)),
        Material((0.85, 0.75, 0.45), shininess=20, spec=0.2, pattern=r03.noise_pattern(120, 0.15, 7)),
        Material((0.12, 0.12, 0.14), metal=0.4, shininess=50, spec=0.5),  # radio
        Material((0.1, 0.1, 0.1), emission=(0.2, 1.0, 0.4)),             # radio LED
        Material((0.25, 0.22, 0.2), shininess=6, spec=0.05),             # soot smudge
        Material((0.2, 0.12, 0.07), shininess=10, spec=0.1),             # brows
    ]

    def scene(p):
        q = r03.turn(p, -0.2, 0.04)
        items, skin = r03.face(q, jaw=0.95, mouth="neutral")
        cap = sdf.sd_ellipsoid(q, (0, 0.5, -0.05), (0.315, 0.375, 0.335))
        cap = np.maximum(cap, -(q[:, 1] - 0.62 + 0.25 * np.maximum(q[:, 2], 0)))
        # side-swept fringe falling across the forehead (asymmetric, so it reads as hair)
        fringe = sdf.sd_capsule(q, (0.22, 0.7, 0.18), (-0.2, 0.58, 0.27), 0.06, 0.03)
        # loose hair hanging past the jaw onto the shoulders, uneven ends
        y = q[:, 1]
        wave = 0.02 * np.sin(q[:, 0] * 23 + y * 9) + 0.015 * np.sin(y * 31)
        fall_l = sdf.sd_capsule(q, (-0.25, 0.55, -0.02), (-0.33, 0.02, -0.06), 0.11, 0.075) + wave
        fall_r = sdf.sd_capsule(q, (0.26, 0.55, -0.02), (0.31, 0.08, -0.08), 0.1, 0.07) + wave
        back = sdf.sd_ellipsoid(q, (0, 0.3, -0.16), (0.36, 0.42, 0.2)) + wave
        hair = sdf.smin(sdf.smin(cap, back, 0.06), sdf.smin(fall_l, fall_r, 0.05), 0.06)
        hair = np.maximum(hair, -sdf.sd_ellipsoid(q, (0, 0.33, 0.24), (0.25, 0.42, 0.22)))  # keep the face clear
        hair = sdf.smin(hair, fringe, 0.04)
        smudge = sdf.sd_ellipsoid(q, (0.15, 0.36, 0.25), (0.05, 0.025, 0.04))
        pb = r03.turn(p, -0.08)
        neck, torso = r03.body(pb, shoulders=0.74)
        torso = sdf.smin(torso, sdf.sd_box(pb, (0, -0.55, -0.04), (0.6, 0.46, 0.24), 0.2), 0.08)
        collar = sdf.sd_box(pb, (0, -0.02, -0.05), (0.3, 0.1, 0.22), 0.1)      # padded collar
        collar = np.maximum(collar, -sdf.sd_cylinder_y(pb, (0, 0.0, -0.04), 0.15, 0.2))
        scarf = sdf.smin(sdf.sd_ellipsoid(pb, (0.02, -0.04, 0.12), (0.19, 0.09, 0.12)),
                         sdf.sd_capsule(pb, (0.08, -0.08, 0.2), (0.14, -0.4, 0.24), 0.05, 0.03), 0.04)
        radio = sdf.sd_box(pb, (0.3, -0.2, 0.25), (0.05, 0.08, 0.025), 0.01)
        led = sdf.sd_sphere(pb, (0.3, -0.11, 0.28), 0.013)
        return sdf.union(*items, (hair, HAIR), (smudge - 0.003, EXTRA),
                         (sdf.smin(neck, skin, 0.05), SKIN), (torso, CLOTH), (collar, CLOTH),
                         (scarf, ACCENT), (radio, METAL), (led, GLOW))
    return scene, mats


r03.CAST["generic-cdf"] = dict(
    build=generic_cdf, name="CDF OFFICER", role="CONVOY / RELAY / CTRL",
    key=(-1.8, 2.2, 3.2), rim=(2.4, 1.0, -1.2), rim_col=(0.45, 0.75, 1.0),
    bg=((4, 14, 20), (20, 60, 70)), fill_col=(0.4, 0.7, 0.9), channel="cdf", exposure=1.25)
r03.CAST["generic-civilian"] = dict(
    build=generic_civilian, name="CIVILIAN", role="SHUTTLE / EVACUEES",
    key=(-2.0, 2.0, 3.2), rim=(2.4, 0.8, -1.2), rim_col=(1.0, 0.7, 0.45),
    bg=((18, 10, 6), (70, 40, 20)), fill_col=(1.0, 0.7, 0.5), channel="cdf")
# radio lines and colours for the new speakers (the round-03 helpers only know the main cast)
EXTRA = {
    "generic-cdf": dict(line="CONVOY ACTUAL: TAKING FIRE, NEED COVER!",
                        name_col=hud_r02.AMBER, role_col=hud_r02.CYAN, line_col=hud_r02.LCD),
    "generic-civilian": dict(line="SHUTTLE 4: PLEASE, WE HAVE KIDS ABOARD!",
                             name_col=hud_r02.AMBER, role_col=hud_r02.CYAN, line_col=hud_r02.LCD),
    "the-choir": dict(line="[THE CHOIR SINGS]", name_col=VIOLET, role_col=TEAL, line_col=VIOLET,
                      name="THE CHOIR", role="VRELL TRANSMISSION"),
}
_orig_frame, _orig_radio = r03.briefing_frame, r03.hud_radio


def _meta(slug):
    c = r03.CAST.get(slug, {})
    e = EXTRA[slug]
    return e.get("name", c.get("name")), e.get("role", c.get("role")), e


def briefing_frame(slug, portrait):
    if slug not in EXTRA:
        return _orig_frame(slug, portrait)
    name, role, e = _meta(slug)
    w, h = 176, 214
    img = r03.metal_block(w, h, 40)
    r03.MET.well(img, (16, 16, 16 + 143, 16 + 143))
    img.alpha_composite(portrait, (16, 16))
    r03.MET.well(img, (16, 170, w - 17, 200))
    raster.draw_text(img, 21, 175, name[:22], e["name_col"])
    raster.draw_text(img, 21, 188, role[:22], e["role_col"])
    return img


def hud_radio(slug, portrait):
    if slug not in EXTRA:
        return _orig_radio(slug, portrait)
    name, role, e = _meta(slug)
    w, h = 240, 176
    img = r03.metal_block(w, h, 0)
    r03.MET.plate(img, 14, 10, "RADIO")
    r03.MET.well(img, (15, 27, 15 + 71, 27 + 71))
    img.alpha_composite(portrait, (15, 27))
    parts = name.split(" ", 1) + [""]
    hud_r02.txt(img, 96, 32, parts[0][:11], e["name_col"])
    hud_r02.txt(img, 96, 52, parts[1][:11], e["name_col"])
    r03.MET.well(img, (15, 108, w - 16, 164))
    for i, s in enumerate(hud_r02.wrap(e["line"], 17)[:3]):
        hud_r02.txt(img, 20, 112 + i * 17, s, e["line_col"])
    return img


r03.briefing_frame, r03.hud_radio = briefing_frame, hud_radio
r03.STYLE_TITLE["generic"] = "GENERIC SPEAKER, STYLE B, RETAINED COLOUR"
r03.STYLE_NOTE["generic"] = [
    "SAME BUST + STYLE-B PIPELINE AS THE MAIN CAST (ROUND 04): CHANNEL",
    "TINT OVER OWN COLOURS, 36 COLOURS, SOFT DITHER, CRT LINES AT 2X.",
    "DELIBERATELY PLAINER THAN THE NAMED CAST; READ BY SILHOUETTE",
    "(BERET + BOOM MIC / LOOSE HAIR + PADDED COLLAR).",
]
r03.STYLE_TITLE["choir"] = "VRELL CHANNEL: INTERFERENCE GLYPH, NO FACE"
r03.STYLE_NOTE["choir"] = [
    "NOT A FACE: A FIVE-FOLD SIGIL OF TENDRILS INSIDE RIPPLING RINGS",
    "THAT BREATHE WITH THE SONG, TORN BY SYNC GLITCHES. VIOLET/TEAL",
    "VRELL CHANNEL RAMP, SAME 72 PX PIXEL TREATMENT AND 2X SCANLINES.",
    "ANIMATED IN GAME (SEE THE GIF); SUBTITLE '[THE CHOIR SINGS]'.",
]


# --------------------------------------------------------------------------- the Choir glyph

CHOIR_RAMP = np.array([raster.hexrgb(h) for h in
                       ["06000e", "1c0634", "40106c", "7030b0", "a868f0", "40e0c0", "d8fff4"]],
                      dtype=np.float64)
CHOIR_FRAMES = 32


def choir_frame(k, n=N):
    """One 72 px frame of the Choir glyph at phase k / CHOIR_FRAMES (seamless loop)."""
    ph = k / CHOIR_FRAMES * 2 * np.pi
    yy, xx = (np.mgrid[0:n, 0:n] + 0.5) / n * 2 - 1
    r = np.sqrt(xx ** 2 + yy ** 2)
    th = np.arctan2(yy, xx)
    breathe = 0.5 + 0.5 * np.sin(ph * 2)                        # the "song" swelling twice per loop
    # rippling rings travelling outwards, bent by a 5-fold wobble
    wob = 0.07 * np.sin(5 * th + ph) + 0.03 * np.sin(3 * th - 2 * ph)
    rings = 0.5 + 0.5 * np.cos((r + wob) * 22 - ph * 4)
    rings *= np.exp(-((r - 0.55) / 0.42) ** 2)
    # five tendrils of the sigil, twisting with radius and slowly turning
    twist = th - 1.6 * r + ph / 5 * 2
    arms = np.abs(np.cos(2.5 * twist)) ** 18 * np.clip(1.1 - r, 0, 1) * (r > 0.08)
    core = np.exp(-(r / (0.13 + 0.04 * breathe)) ** 2)
    eye_ring = np.exp(-((r - (0.2 + 0.03 * breathe)) / 0.025) ** 2)
    v = 0.18 * rings + (0.55 + 0.35 * breathe) * arms + 0.9 * core + 0.5 * eye_ring
    v *= np.clip(1.25 - r, 0, 1)                                # fade to the screen edge
    # ordered dither, then the vrell ramp; peaks go teal/white
    thr = r03.BAYER4[np.arange(n)[:, None] % 4, np.arange(n)[None, :] % 4] - 0.5
    idx = np.clip(np.floor(v * (len(CHOIR_RAMP) - 1) * 1.05 + thr * 0.7 + 0.15), 0,
                  len(CHOIR_RAMP) - 1).astype(int)
    img = CHOIR_RAMP[idx].copy()
    # faint dot grid of the comm screen where the glyph is dark
    grid = ((np.arange(n)[None, :] % 6 == 0) & (np.arange(n)[:, None] % 6 == 0)) & (idx <= 1)
    img[grid] = CHOIR_RAMP[2] * 0.8
    # sync tears: rows shifted sideways, and a bright noise line (deterministic per frame)
    rng = np.random.default_rng(1000 + k)
    for y in rng.choice(np.arange(n), 2 + (k % 3), replace=False):
        img[y] = np.roll(img[y], int(rng.integers(-4, 5)), axis=0)
    if k % 4 == 0:
        y = int(rng.integers(0, n))
        img[y, rng.random(n) > 0.55] = CHOIR_RAMP[5]
    band = int((k * 3) % (n + 6)) - 3                          # rolling interference band
    lo, hi = max(band, 0), min(band + 3, n)
    if lo < hi:
        img[lo:hi] = np.clip(img[lo:hi] * 0.7 + CHOIR_RAMP[4] * 0.35, 0, 255)
    hud = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGB").convert("RGBA")
    brief = sprite.enlarge(hud, 2)
    arr = np.array(brief).astype(np.float64)
    arr[1::2, :, :3] *= 0.74
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA"), hud


def choir():
    slug = "the-choir"
    r03.CAST.setdefault(slug, dict(name="THE CHOIR", role="VRELL TRANSMISSION", channel="vrell"))
    brief, hud = choir_frame(6)
    out_dir = r03.CHAR_DIR / slug / "concept"
    out_dir.mkdir(parents=True, exist_ok=True)
    base = r03.character_sheet(slug, "choir", brief, hud, ROUND)
    # loop strip below the standard sheet: 8 of the 32 phases of the 72 px glyph
    sheet = Image.new("RGBA", (base.width, base.height + 110), base.getpixel((4, base.height - 4)))
    sheet.alpha_composite(base, (0, 0))
    raster.draw_text(sheet, 16, base.height + 4, "LOOP: 8 OF 32 FRAMES OF THE 72 PX GLYPH (1X), 12 FPS IN GAME",
                     raster.LABEL_DIM)
    for i in range(8):
        sheet.alpha_composite(choir_frame(i * 4)[1], (16 + i * 84, base.height + 18))
    png = out_dir / "portrait-r08-a.png"
    sheet.convert("RGB").save(png, optimize=True)
    frames = []
    for k in range(CHOIR_FRAMES):
        b, h = choir_frame(k)
        f = Image.new("RGBA", (176 + 16 + 480, 214 + 16), (12, 14, 20, 255))
        f.alpha_composite(briefing_frame(slug, b), (8, 8))
        f.alpha_composite(sprite.enlarge(hud_radio(slug, h), 2).crop((0, 0, 480, 214)),
                          (176 + 16, 8))
        frames.append(f)
    size = rig.write_gif(frames, out_dir / "portrait-r08-a.gif", fps=12, colors=64)
    print(f"wrote {png.relative_to(ROOT)} and .gif ({size / 1e3:.0f} kB)")


def generic(slug, fname):
    brief, hud = r04.style_b2(slug, r03.render_bust(slug))
    out_dir = r03.CHAR_DIR / "concept"
    png = out_dir / fname
    r03.character_sheet(slug, "generic", brief, hud, ROUND).convert("RGB").save(png, optimize=True)
    print("wrote", png.relative_to(ROOT))


def main(args):
    if not args or "choir" in args:
        choir()
    if not args or "cdf" in args:
        generic("generic-cdf", "generic-cdf-r08-a.png")
    if not args or "civilian" in args:
        generic("generic-civilian", "generic-civilian-r08-a.png")


if __name__ == "__main__":
    main(sys.argv[1:])
