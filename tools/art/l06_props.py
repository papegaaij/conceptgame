#!/usr/bin/env python3
"""Production art: Level 06's three props, the concepts chosen in concept round 23
(design/campaign/act-1-first-contact/level-06-farside, "Ground targets" and "Secrets and pickups";
art direction: readability rule 7, a damaged and a wrecked frame for ground structures).

Outputs (assets/sprites/, sizes from the `ground_targets` of Level 06's data.yaml):
  ore-cart_0..2.png             28x40 the abandoned ore cart on the rail (concept b: a closed steel
                                hopper, an olive roof with two amber/black hatched lid panels, a red
                                beacon, couplers): intact, damaged, wrecked (the top blown open)
  ore-cart-break_0..7.png       60x60 the damaged cart in six pieces that fly apart, tumble and
                                char, then crumble away over the last three frames (1-bit alpha)
  survey-cache_0..2.png         32x24 the CDF survey cache in the dark crater (concept a: a ribbed
                                olive field case, amber/black end bands, three retro-reflector
                                markers on the lid): closed, hit, opened (the lid blown off, three
                                instrument tubes and an amber module inside)
  survey-cache-glint_0..3.png   32x24, additive: the three markers glinting (a cross of light on
                                each), 4 frames of twinkle; the game draws it only while the
                                headlight or a flare lights the cache, so the secret stays dark
  data-core-terminal_0..1.png   40x32 the airlock terminal at Daedalus Gate (concept b: a steel wall
                                cabinet with two rows of amber LEDs and a round socket in an amber
                                ring holding the amber core orb): intact, released (the socket
                                empty, the LEDs dead)
  data-core-terminal-glow.png   40x32, additive: the intact cabinet's emission only (the LED rows
                                and the orb) with a stepped halo, drawn after the light pass like the
                                turret glow frames, so the LEDs stay lit in the dark
  pickup-data-core_0..7.png     26x26 the released core (concept b: an amber orb in a dark ring
                                with one rib), turning, with the pickups' pulsing outline and an
                                amber halo stepped to four translucency levels (8-frame loop)
  design/campaign/act-1-first-contact/level-06-farside/concept/l06-props-final-r23-a.png/.gif

The game draws the cart's frame 0 or 1 while it stands, plays the break-apart where it is destroyed
and leaves the wrecked frame there (as Level 04's targets, tools/art/l04_targets.py, whose
break-apart this reuses). The cache and the terminal are triggers: they show their hit frame after
the first hit and their last frame once spent, and stay. The models and materials are the concept
round's (tools/concept/props_r23.py, imported unchanged), rendered at the quality bar's 8x with
its light rim and scorch; the glow frame is the model with only its emission left
(tools/art/l06_darkness.py's way), the glint the concept's marker field. One palette per prop.

Run: python3 tools/art/l06_props.py [--review]   (~30 s)
"""
import sys
from concurrent.futures import ProcessPoolExecutor
from dataclasses import replace

import numpy as np
import yaml
from PIL import Image, ImageFilter

import artkit
from artkit import DESIGN, ROOT, TAU, sprite
from render.sdf import rotate_x, rotate_z, sd_box  # noqa: E402

import props_r23 as r23  # noqa: E402  (concept script, imported unchanged)
from l04_targets import pieces  # noqa: E402  (the break-apart's cells)
from loot_targets import crumbled  # noqa: E402  (the break-apart's burn-down)

SCRIPT = "l06_props.py"
BATCH = "M4 part F batch"
SOURCE = artkit.source_note(SCRIPT, BATCH)
REVIEW_ROUND = "r23"
LEVEL_DIR = DESIGN / "campaign" / "act-1-first-contact" / "level-06-farside"
DATA = {t["target"]: t for t in yaml.safe_load((LEVEL_DIR / "data.yaml").read_text(encoding="utf-8"))["ground_targets"]}
CART = tuple(DATA["ore cart"]["size"])
CACHE = tuple(DATA["survey cache"]["size"])
TERMINAL = tuple(DATA["airlock terminal"]["size"])
CORE, CORE_PAD, CORE_FRAMES = 16, 5, 8
CART_BREAK = 60
COLOURS = 32
BREAK_FRAMES, CRUMBLE_FRAMES = 8, 3
GLINT = (1.0, 0.8, 0.6, 0.8)          # the markers' twinkle over the 4 glint frames
HALO = 2.2                             # px, the glow frame's halo radius (l06_darkness.py's)
CORE_HALO = (255, 176, 60)
OUTLINE_DIM, OUTLINE_LIT = np.array([170, 120, 50]), np.array([255, 214, 140])


def render(scene, mats, size, rim=True):
    hi, factor = artkit.render_hi(scene, mats, size, float(size[0]), z_top=float(max(size)), steps=150)
    arr = np.array(artkit.native(hi, factor, crisp=60)).astype(np.float64)
    return r23.gt.light_rim(arr) if rim else arr


def to_img(arr):
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")


# --------------------------------------------------------------------------- the ore cart

def cart(s):
    """State s (0 intact, 1 damaged, 2 wrecked) as the concept's ore_cart finishes it."""
    return to_img(r23.damage(render(r23.cart_b(s), r23.materials(s), CART, rim=s < 2), s, 230))


def cart_break(f):
    """Frame f: every piece of the damaged model moved, spun and tumbled; its markings move with it."""
    base = r23.cart_b(1)
    base_mats = r23.materials(1)
    char = 1 - 0.07 * f
    transforms = []
    for centre, half, velocity, spin, tumble in pieces(CART, seed=6):
        offset = np.array([velocity[0] * (f + 1), velocity[1] * (f + 1), -0.4 * f])

        def to_local(p, c=centre, o=offset, sp=spin * (f + 1), t=tumble * (f + 1)):
            return rotate_x(rotate_z(p - c - o, sp), t) + c
        transforms.append((to_local, centre, half))
    mats = []
    for to_local, _, _ in transforms:
        for m in base_mats:
            pattern = (lambda p, n, f_=m.pattern, tl=to_local: f_(tl(p), n)) if m.pattern else None
            mats.append(replace(m, albedo=tuple(np.array(m.albedo) * char), pattern=pattern))
    count = len(base_mats)

    def scene(p):
        best_d, best_m = None, None
        for i, (to_local, centre, half) in enumerate(transforms):
            q = to_local(p)
            d, m = base(q)
            d = np.maximum(d, sd_box(q, (centre[0], centre[1], 0), (half[0], half[1], 20.0)))
            m = m + i * count
            if best_d is None:
                best_d, best_m = d, m
            else:
                closer = d < best_d
                best_d, best_m = np.where(closer, d, best_d), np.where(closer, m, best_m)
        return best_d, best_m
    hi, factor = artkit.render_hi(scene, mats, (CART_BREAK, CART_BREAK), float(CART_BREAK),
                                  z_top=float(CART_BREAK), steps=150)
    return crumbled(artkit.native(hi, factor, crisp=60), f - (BREAK_FRAMES - CRUMBLE_FRAMES) + 1)


# --------------------------------------------------------------------------- the survey cache

def cache(s):
    """State s (0 closed, 1 hit, 2 opened) as the concept's survey_cache (variant a) finishes it."""
    return to_img(r23.damage(render(r23.cache_a(s), r23.materials(s, leds=False), CACHE, rim=s < 2), s, 231))


def cache_glints():
    """The concept's lit marker glint (premultiplied on black), dimmed per twinkle frame."""
    base = np.array(r23.marker_glow("a", 0, True)).astype(np.float64)
    out = []
    for k in GLINT:
        rgb = np.round(base[..., :3] * k)
        alpha = np.where(rgb.max(axis=-1) >= 1, 255, 0)
        out.append(Image.fromarray(np.dstack([rgb, alpha]).astype(np.uint8), "RGBA"))
    return out


# --------------------------------------------------------------------------- the terminal and its core

def terminal(s):
    """State s (0 intact, 1 released) as the concept's data_core (variant b) renders it."""
    return to_img(render(r23.term_b(s), r23.materials(0, lit_core=s == 0, leds=s == 0), TERMINAL))


def terminal_glow():
    """The intact cabinet's emission (LED rows, orb) through the sprite path with a stepped halo,
    as tools/art/l06_darkness.py's glow (every material's albedo, specular and reflection off)."""
    mats = [replace(m, albedo=(0.0, 0.0, 0.0), metal=0.0, spec=0.0, pattern=None)
            for m in r23.materials(0, lit_core=True, leds=True)]
    hi, factor = artkit.render_hi(r23.term_b(0), mats, TERMINAL, float(TERMINAL[0]), z_top=float(max(TERMINAL)),
                                  steps=150, shadows=False, ambient=0.0, fill=0.0)
    core = np.array(artkit.native(hi, factor, crisp=0)).astype(np.float64)
    rgb = core[..., :3] * (core[..., 3:4] / 255)
    rgb[rgb.max(axis=-1) < 40] = 0                              # the dark body adds nothing
    blur = np.array(Image.fromarray(rgb.astype(np.uint8)).filter(ImageFilter.GaussianBlur(HALO))).astype(np.float64)
    out = np.maximum(rgb, blur * 1.6)
    lit = out.max(axis=-1)
    alpha = np.where(rgb.max(axis=-1) > 0, 1.0, np.floor(np.clip(lit / 255 * 1.4, 0, 1) * 3 + 0.5) / 3)
    img = np.dstack([np.where(alpha[..., None] > 0, out / np.maximum(alpha[..., None], 1e-6), 0), alpha * 255])
    return artkit.additive(Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGBA"))


def core(i):
    """The orb turned in the model (its rib through half a turn over the loop, the key light fixed)."""
    model = r23.core_b()
    turn = np.pi * i / CORE_FRAMES

    def scene(p):
        return model(rotate_z(p, turn))
    return to_img(render(scene, r23.materials(0), (CORE, CORE), rim=False))


def core_presentation(body, i):
    """pickups.py's pulse (rule 6) in amber: a 1 px outline and a soft halo brighten and fade."""
    w, h = body.width + 2 * CORE_PAD, body.height + 2 * CORE_PAD
    base = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    base.alpha_composite(body, (CORE_PAD, CORE_PAD))
    solid = np.array(base.getchannel("A")) > 0
    pulse = 0.5 + 0.5 * np.cos(i * TAU / CORE_FRAMES)
    mask = Image.fromarray((solid * 255).astype(np.uint8))
    halo = np.array(mask.filter(ImageFilter.GaussianBlur(2.6))) / 255 * (0.5 + 0.6 * pulse)
    outline = (np.array(mask.filter(ImageFilter.MaxFilter(3))) > 0) & ~solid
    out = np.zeros((h, w, 4))
    out[..., :3] = CORE_HALO
    out[..., 3] = np.clip(halo, 0, 1) * 255
    out[outline, :3] = OUTLINE_DIM + (OUTLINE_LIT - OUTLINE_DIM) * pulse
    out[outline, 3] = 255
    b = np.array(base)
    out[solid] = b[solid]
    return artkit.stepped_alpha(Image.fromarray(out.astype(np.uint8), "RGBA"))


# --------------------------------------------------------------------------- build

def _job(job):
    kind, i = job
    return {"cart": cart, "break": cart_break, "cache": cache, "terminal": terminal, "core": core}[kind](i)


def build():
    jobs = ([("cart", s) for s in range(3)] + [("break", f) for f in range(BREAK_FRAMES)]
            + [("cache", s) for s in range(3)] + [("terminal", s) for s in range(2)]
            + [("core", i) for i in range(CORE_FRAMES)])
    with ProcessPoolExecutor() as pool:
        out = list(pool.map(_job, jobs))
    carts, breaks, caches, terminals, cores = out[:3], out[3:11], out[11:14], out[14:16], out[16:]
    for name, frames, size in (("ore-cart", carts, CART), ("survey-cache", caches, CACHE),
                               ("data-core-terminal", terminals, TERMINAL)):
        for img in frames:
            if img.size != size:
                raise ValueError(f"{name}: rendered {img.size}, the data file says {size}")
    done = artkit.quantize_set(carts + breaks, COLOURS)            # one palette per prop
    artkit.write_frames("ore-cart", done[:3], SOURCE)
    artkit.write_frames("ore-cart-break", done[3:], SOURCE)
    artkit.write_frames("survey-cache", artkit.quantize_set(caches, COLOURS), SOURCE)
    artkit.write_frames("survey-cache-glint", artkit.quantize_set(cache_glints(), 16), SOURCE)
    artkit.write_frames("data-core-terminal", artkit.quantize_set(terminals, COLOURS), SOURCE)
    artkit.write_frames("data-core-terminal-glow", artkit.quantize_set([terminal_glow()], 16), SOURCE, single=True)
    bodies = artkit.quantize_set(cores, 24)
    artkit.write_frames("pickup-data-core", [core_presentation(b, i) for i, b in enumerate(bodies)], SOURCE)
    for name in ("ore-cart", "ore-cart-break", "survey-cache", "survey-cache-glint", "data-core-terminal",
                 "data-core-terminal-glow", "pickup-data-core"):
        frames = artkit.load_frames(name)
        print(f"{name}: {len(frames)} frames {frames[0].size}, {artkit.colour_count(frames)} colours")


# --------------------------------------------------------------------------- review

BACK = ROOT / "assets" / "backdrop" / "level-06"


def crop(name, box):
    return Image.open(BACK / f"{name}.png").convert("RGBA").crop(box)


def view(ground, light, solid, at, glows=()):
    """A play-field patch as the game draws it: the ground and the prop multiplied by the light map
    (the prop's shadow first), then the additive frames after the light pass."""
    x, y = at[0] - solid.width // 2, at[1] - solid.height // 2
    arr = r23.shadowed(r23.darken(ground, light), solid, x, y, light)
    for g in glows:
        arr = r23.add(arr, np.array(g).astype(np.float64), at[0] - g.width // 2, at[1] - g.height // 2)
    return to_img(arr)


def review():
    artkit.REVIEW_ROUND = REVIEW_ROUND
    carts, breaks = artkit.load_frames("ore-cart"), artkit.load_frames("ore-cart-break")
    caches, glints = artkit.load_frames("survey-cache"), artkit.load_frames("survey-cache-glint")
    terminals, glow = artkit.load_frames("data-core-terminal"), artkit.load_frames("data-core-terminal-glow")
    cores = artkit.load_frames("pickup-data-core")
    lamp, head, flare = r23.LAMP, r23.HEADLIGHT, np.array([1.0, 0.78, 0.5])
    # the ore rail runs at x 410 in Level 06's tile sets; the cart sits on it under a rail lamp
    rail = crop("dome-field", (410 - 90, 300, 410 + 90, 480))
    crater = crop("dark-crater", (10, 10, 190, 190))
    gate = crop("daedalus-gate", (150, 60, 330, 240))
    w, h = 180, 180
    dark = r23.light_map(w, h)
    views = [
        (view(rail, r23.light_map(w, h, pools=[(90, 70, 64, lamp)]), carts[0], (90, 90)), "CART UNDER A RAIL LAMP"),
        (view(rail, dark, carts[0], (90, 90)), "CART UNLIT (AMBIENT)"),
        (view(crater, dark, caches[0], (90, 90)), "CACHE IN THE DARK"),
        (view(crater, r23.light_map(w, h, cone=(90, 176, 200, np.radians(60))), caches[0], (90, 90), [glints[0]]),
         "CACHE IN THE HEADLIGHT"),
        (view(crater, r23.light_map(w, h, pools=[(110, 50, 120, flare)]), caches[1], (90, 90), [glints[1]]),
         "CACHE HIT, FLARE"),
        (view(gate, r23.light_map(w, h, pools=[(60, 40, 110, lamp)]), terminals[0], (90, 90), glow),
         "TERMINAL, DOME LIGHT"),
        (view(gate, dark, terminals[0], (90, 90), glow), "TERMINAL UNLIT, GLOW"),
    ]
    released = r23.shadowed(r23.darken(gate, r23.light_map(w, h, pools=[(60, 40, 110, lamp)])), terminals[1],
                            70, 74, r23.light_map(w, h, pools=[(60, 40, 110, lamp)]))
    released = to_img(released)
    released.alpha_composite(cores[0], (118, 100))
    views.append((released, "RELEASED, THE CORE"))
    rows = [("ORE CART: INTACT, DAMAGED, WRECKED", carts, 5, False),
            ("ORE CART: BREAK-APART", breaks, 2, False),
            ("SURVEY CACHE: CLOSED, HIT, OPENED", caches, 5, False),
            ("SURVEY CACHE: MARKER GLINT, 4 TWINKLE FRAMES, ONLY WHILE LIT (ADDITIVE)", glints, 5, True),
            ("DATA CORE TERMINAL: INTACT, RELEASED", terminals, 5, False),
            ("DATA CORE TERMINAL: GLOW FRAME, AFTER THE LIGHT PASS (ADDITIVE)", glow, 5, True),
            ("DATA CORE PICKUP: 8-FRAME LOOP", cores, 4, False),
            ("IN CONTEXT, 1X: LEVEL 06'S TILES UNDER THE LIGHT MAP, THE GLOWS AFTER IT", [v for v, _ in views[:4]], 1, False),
            ("IN CONTEXT, 1X (CONTINUED)", [v for v, _ in views[4:]], 1, False)]
    sheet = artkit.review_sheet("LEVEL 06 PROPS - FINAL SPRITES", rows, width=1000, batch=BATCH)
    # the GIF: the cache found by the headlight, shot open; the cart blown apart; the core released
    gif = []
    for i in range(48):
        cone_y = 260 - i * 3
        light = r23.light_map(w, h, cone=(90, cone_y, 200, np.radians(60)))
        lit = cone_y - 200 < 90 + 12
        state = 0 if i < 24 else 1 if i < 34 else 2
        cache_view = view(crater, light, caches[state], (90, 90), [glints[i % 4]] if lit and state < 2 else [])
        lamp_light = r23.light_map(w, h, pools=[(90, 70, 64, lamp)])
        if i < 30:
            cart_view = view(rail, lamp_light, carts[0 if i < 18 else 1], (90, 90))
        else:
            cart_view = view(rail, lamp_light, carts[2], (90, 90))
            if i - 30 < len(breaks):
                cart_view.alpha_composite(breaks[i - 30], (90 - CART_BREAK // 2, 90 - CART_BREAK // 2))
        gate_light = r23.light_map(w, h, pools=[(60, 40, 110, lamp)])
        term_view = view(gate, gate_light, terminals[0 if i < 30 else 1], (90, 90), glow if i < 30 else [])
        if i >= 30:
            term_view.alpha_composite(cores[i % CORE_FRAMES], (118, 100 + (i - 30)))
        frame = Image.new("RGBA", (3 * w + 16, h), (11, 14, 20, 255))
        for k, v in enumerate((cache_view, cart_view, term_view)):
            frame.alpha_composite(v, (k * (w + 8), 0))
        gif.append(sprite.enlarge(frame, 2))
    artkit.save_review(sheet, gif, LEVEL_DIR / "concept", "l06-props", fps=8)


if __name__ == "__main__":
    if "--review" not in sys.argv[1:]:
        build()
    review()
