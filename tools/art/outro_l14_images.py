#!/usr/bin/env python3
"""Production art: the Act 2 outro's four briefing images (design/campaign/act-2-homefront, Act
debrief; M5 part H, step H2), one per outro page, in the chosen briefing-r08-a look of
tools/art/briefing_images.py (its Board, layers, palettes, colours and labels, imported unchanged).
Commander Okafor speaks every page.

Outputs (672x240, the briefing screen's image size):
  act-2-outro-stump  p1: Geneva Concord at sunrise from above: the city (the chosen round-10 scene's
                     city texture, tools/concept/scenes_r10.py, without the Vrell overgrowth) graded
                     to a low sun over the lake, the water bright with its glitter; the Siege Spire's
                     withered stump on the Concord plaza where the trunk stood (the production stump
                     and dead roots of tools/art/siege_spire.py, placed by its pivots as the game
                     draws them after the lift, warmed by the sun, its long shadow away from the
                     sun; step H5, until then the concept model); the canopy's torn, withered
                     shreds at the top right; the tally: coordination lost, digging in, scattering,
                     the army clearing city by city
  act-2-outro-names  p2: a Nova Lagos street at dawn: a memorial wall of names (engraved rows, photos
                     and notes pinned between them) before the damaged skyline, hundreds of candles
                     along its foot, a few people standing with their heads bowed, one kneeling to
                     light a candle; the three counts that are still running
  act-2-outro-mars   p3: the long-range plot: Ares Landing's priority signal from Mars (a holographic
                     planet at the right edge in a rust ramp) to Earth, its pulse rings on the colony,
                     Tharsis Foundry cut off, Vrell contacts holding orbit; the signal's message and its
                     pulse train; HOLDING 21 DAYS
  act-2-outro-aegis  p4: the CDF coastal airbase's apron at dusk from above: Lancer's Stormhawk, Rook's
                     Ember and Aegis Two to Four on their pads, long shadows; behind them the heavy
                     transport bound for Mars (a ray-marched model built here from the render
                     package's primitives with the evacuation shuttle's materials, tools/art/shuttle.py),
                     its cargo ramp down; T-72 H
  design/campaign/act-2-homefront/level-14-siege-spire/concept/act-2-outro-final-r36-a.png
                     the review sheet: the four images at 1x, labelled

Composed as briefing_images composes its pages: the display layer posterized to 24 colours with
ordered dither, the lines and labels to 16, each sprite (the city photo, the stump, the transport,
the production Stormhawk and Ember sprites from assets/) on a palette of its own.

Run: python3 tools/art/outro_l14_images.py [stump|names|mars|aegis ...] [--assets] [--review]
(~10 s). By default all four are rendered into the review sheet only; --assets also writes the
named ones (all four if none is named) to assets/ui/briefing/act-2-outro-<name>.png, Source-tagged;
--review rebuilds the sheet from the files in assets/, rendering only those still missing there.
"""
import sys

import numpy as np
from PIL import Image, ImageDraw

import artkit
import briefing_images as bi
import scenes_r10 as s10  # noqa: E402  (concept script, imported unchanged: Geneva's city texture)
import shuttle  # noqa: E402  (tools/art: the evacuation shuttle's materials for the transport)
from artkit import DESIGN, ROOT, SPRITES, sprite
from briefing_images import AMBER, CYAN, CYAN_DIM, GREEN, GREY, H, RED, VIOLET, W, WHITE, Board

from render import raster, sdf  # noqa: E402
from render.sdf import (mirror_x, rotate_y, rotate_z, sd_box, sd_capsule, sd_cylinder_y,  # noqa: E402
                        sd_ellipsoid, sd_plate, sd_sphere, union)

SCRIPT = "outro_l14_images.py"
SOURCE = artkit.source_note(SCRIPT, "M5 part H batch")
ROUND = "r36"
OUT = bi.OUT
CONCEPT = DESIGN / "campaign" / "act-2-homefront" / "level-14-siege-spire" / "concept"
PREFIX = "act-2-outro-"
SPRITE_COLOURS = 48
SUN = (255, 196, 120)          # the low sun of the daylight section
RUST = [(0.0, (14, 4, 4)), (0.35, (70, 22, 12)), (0.7, (150, 62, 30)), (1.0, (232, 140, 84))]


class OutroBoard(Board):
    """The briefing Board with Mars as a planet of its own (the other kinds stay Board's), big
    labels and a top layer for the labels drawn over sprites."""

    def __init__(self):
        super().__init__()
        self.top = Image.new("RGBA", (W, H), (0, 0, 0, 0))

    def mars(self, cx, cy, r, seed=15):
        """A holographic Mars: dusky albedo regions, the Valles Marineris scar and the south polar
        cap through a rust ramp, lit from the top left as Board.planet lights, with a rim glow."""
        d = np.hypot(self.xx - cx, self.yy - cy)
        inside = d < r
        nx, ny = (self.xx - cx) / r, (self.yy - cy) / r
        lum = 0.45 + 0.55 * raster.fbm(W, H, max(8, int(r // 3)), seed, octaves=5, period=False)
        lum -= 0.25 * np.clip(raster.fbm(W, H, max(8, int(r // 1.5)), seed + 1, octaves=3, period=False) - 0.45, 0, 1) * 3
        canyon = np.abs(ny - 0.05 - 0.12 * np.sin(nx * 3.0)) < 0.025 * (1 - np.abs(nx + 0.1) * 1.6)
        lum = np.where(canyon & (np.abs(nx + 0.1) < 0.55), lum * 0.35, lum)
        cap = ny > 0.86
        light = np.clip(0.35 - 0.55 * nx - 0.55 * ny + 0.5 * np.sqrt(np.clip(1 - nx ** 2 - ny ** 2, 0, 1)), 0.08, 1)
        body = raster.ramp(RUST, np.clip(lum * light, 0, 1))
        body = np.where(cap[..., None], np.array([200, 190, 190]) * light[..., None], body)
        self.base = np.where(inside[..., None], body, self.base)
        self.base += (np.exp(-((d - r) / 3) ** 2) * 0.8)[..., None] * np.array([200, 90, 50])

    def big(self, x, y, text, colour, right=False):
        """A label at twice the font's size (the countdowns)."""
        if right:
            x -= raster.text_width(text, 2)
        raster.draw_text(self.over, x, y, text, colour + (255,), scale=2)

    # the labels and lines over a sprite (a photo, a model) go on a layer of their own, last

    def top_label(self, x, y, text, colour=CYAN, right=False):
        if right:
            x -= raster.text_width(text)
        raster.draw_text(self.top, x, y, text, colour + (255,))

    def top_line(self, points, colour=CYAN, width=1, alpha=255):
        ImageDraw.Draw(self.top).line([tuple(p) for p in points], fill=colour + (alpha,), width=width)

    def image(self):
        out = super().image().convert("RGBA")
        out.alpha_composite(self.top)
        return out.convert("RGB")


def palettised(img, colours=SPRITE_COLOURS):
    return artkit.quantize_set([img], colours)[0]


# --------------------------------------------------------------------------- p1: the stump

class _City(s10.GenevaScene):
    """The round-10 Geneva city texture with the Vrell overgrowth and the rotunda left out (the
    trunk broke the rotunda; its stump stands there now)."""

    def _overgrowth10(self, img, per):
        return img

    def _landmarks10(self, img, per):
        return img


def city_texture():
    city = object.__new__(_City)        # only the texture: the scene's layers are never built
    img = city._city(540)
    return img, city.city_kind


def stump_sprite(scale=0.40):
    """The Siege Spire's production stump (tools/art/siege_spire.py: assets/sprites/siege-spire-stump)
    over its six dead roots (siege-spire-root_1, 3, .. 11), placed by assets/pivots/siege-spire.json as
    the game draws them after the lift, scaled to the recon frame and warmed by the low sun."""
    import json
    piv = json.loads((ROOT / "assets" / "pivots" / "siege-spire.json").read_text(encoding="utf-8"))
    full = Image.new("RGBA", (440, 440), (0, 0, 0, 0))
    cx, cy = 220, 220

    def put(img, x, y):
        full.alpha_composite(img, (int(round(cx + x - img.width / 2)), int(round(cy + y - img.height / 2))))
    for i, r in enumerate(piv["roots"]):
        put(Image.open(SPRITES / f"siege-spire-root_{2 * i + 1}.png").convert("RGBA"), *r["offset"])
    put(Image.open(SPRITES / "siege-spire-stump.png").convert("RGBA"), 0, 0)
    size = round(440 * scale)
    a = np.array(full.resize((size, size), Image.BOX)).astype(np.float64)
    a[..., 3] = np.where(a[..., 3] >= 128, 255, 0)
    a[..., :3] = np.clip(a[..., :3] * np.array([1.12, 0.98, 0.84]) * 1.1, 0, 255)
    a[a[..., 3] == 0] = 0
    return palettised(Image.fromarray(a.astype(np.uint8), "RGBA"), 48)


def stump():
    """p1: Geneva Concord at sunrise, the withered stump on the Concord plaza, the lake bright."""
    b = OutroBoard()
    fx0, fy0, fw, fh = 8, 24, 440, 208          # the recon frame on the display
    cx0, cy0 = 10, 40                            # its top left in the city texture
    tex, kind = city_texture()
    rgb = np.array(tex).astype(np.float64)[cy0:cy0 + fh, cx0:cx0 + fw, :3]
    kind = kind[cy0:cy0 + fh, cx0:cx0 + fw]
    yy, xx = np.mgrid[0:fh, 0:fw].astype(np.float64)
    sx, sy = -30.0, -10.0                        # the sun, low over the far end of the lake
    ds = np.hypot(xx - sx, (yy - sy) * 1.4)
    warm = np.clip(1.25 - ds / 520, 0.62, 1.25)
    rgb = rgb * warm[..., None] * np.array([1.12, 0.96, 0.80])
    water = kind == 4
    sheen = np.clip(1.0 - ds / 330, 0, 1)
    glare = np.exp(-((np.arctan2(yy - sy, xx - sx) - 0.75) / 0.45) ** 2) * np.clip(1.1 - ds / 260, 0, 1)
    rng = np.random.default_rng(1414)
    glint = rng.random((fh, fw)) < 0.002 + 0.07 * glare ** 2
    tex_l = np.array(tex).astype(np.float64)[cy0:cy0 + fh, cx0:cx0 + fw, :3].mean(axis=-1, keepdims=True) / 90
    far, near = np.array([70, 92, 140]), np.array([250, 196, 132])
    k = np.clip(0.15 + 0.85 * sheen + 0.5 * glare, 0, 1)[..., None]
    lake = (far * (1 - k) + near * k) * (0.82 + 0.25 * tex_l)
    lake = np.where(glint[..., None], np.array([255, 240, 205]), lake)
    rgb = np.where(water[..., None], lake, rgb)
    # the canopy's withered shreds at the top right, torn open (holes the sun comes through)
    reach = np.clip(((xx - fw * 0.42) / (fw * 0.58)) * 0.8 + (1 - yy / (fh * 0.75)) * 0.7 - 0.55, 0, 1)
    noise = raster.fbm(fw, fh, 26, 1436, octaves=4, period=False)
    shred = (noise * reach > 0.30)
    vein = np.abs(raster.fbm(fw, fh, 14, 1437, octaves=2, period=False) - 0.5) < 0.035
    canopy = np.array([46, 30, 58]) * (0.8 + 0.4 * noise[..., None])
    canopy = np.where(vein[..., None], np.array([92, 70, 96]), canopy)
    edge = shred & ~(noise * reach > 0.36)
    alpha = np.where(shred, 0.78, 0.0)[..., None]
    rgb = rgb * (1 - alpha) + canopy * alpha
    rgb = np.where(edge[..., None], rgb * 0.6 + np.array([150, 110, 70]) * 0.4, rgb)
    # the stump on the plaza where the trunk stood, its long shadow (stretched away from the sun)
    # darkening the city before the city gets its palette
    sp = stump_sprite()
    qx, qy = 300 - cx0, 150 - cy0
    sh = np.array(sprite.shadow_of(sp, opacity=0.75, blur=1.5).getchannel("A").resize(
        (int(sp.width * 1.3), sp.height))).astype(np.float64) / 255
    shade = np.zeros((fh, fw))
    x0, y0 = int(qx + 22 - sh.shape[1] / 2), int(qy + 9 - sh.shape[0] / 2)
    shade[max(0, y0):y0 + sh.shape[0], max(0, x0):x0 + sh.shape[1]] = \
        sh[max(0, -y0):fh - y0, max(0, -x0):fw - x0]
    rgb = rgb * (1 - shade[..., None])
    city = palettised(raster.to_rgba_image(np.clip(rgb, 0, 255)), 64)
    b.sprites.append((city, fx0, fy0))
    px, py = fx0 + qx, fy0 + qy
    b.sprite(sp, px, py)
    b.title("GENEVA CONCORD - 06:40, SUNRISE")
    for x0, y0, sx_, sy_ in ((fx0, fy0, 1, 1), (fx0 + fw - 1, fy0, -1, 1), (fx0, fy0 + fh - 1, 1, -1),
                             (fx0 + fw - 1, fy0 + fh - 1, -1, -1)):
        b.top_line([(x0, y0 + sy_ * 8), (x0, y0), (x0 + sx_ * 8, y0)], CYAN)
    b.top_line([(px - 40, py + 46), (px - 70, py + 70), (px - 150, py + 70)], AMBER, 1, 255)
    b.top_label(px - 150, py + 73, "SIEGE SPIRE - DEAD", AMBER)
    b.top_label(px - 150, py + 83, "THE STUMP, WITHERED", WHITE)
    b.top_line([(fx0 + fw - 60, fy0 + 24), (fx0 + fw + 10, fy0 + 24)], VIOLET, 1, 255)
    b.top_label(fx0 + 10, fy0 + 160, "LAC LEMAN", WHITE)
    tx = 462
    b.label(W - 10, 9, "THE SIEGE SPIRE IS DEAD", AMBER, right=True)
    b.label(tx, 44, "CANOPY - TORN", VIOLET)
    b.label(tx, 54, "WITHERING", VIOLET)
    b.line([(tx, 74), (W - 12, 74)], CYAN_DIM, 1, 140)
    b.label(tx, 82, "VRELL FORCES - EARTH", WHITE)
    b.label(tx, 96, "COORDINATION: LOST", GREEN)
    b.label(tx, 106, "WITHIN THE HOUR", CYAN_DIM)
    b.label(tx, 124, "SOME - DIGGING IN", AMBER)
    b.label(tx, 134, "MOST - SCATTERING", AMBER)
    b.line([(tx, 154), (W - 12, 154)], CYAN_DIM, 1, 140)
    for i, city_name in enumerate(("NOVA LAGOS", "GENEVA CONCORD", "AND THE REST")):
        b.marker(tx + 4, 168 + 12 * i, "square", GREEN, 3)
        b.label(tx + 14, 165 + 12 * i, city_name, CYAN if i < 2 else CYAN_DIM)
    b.label(W - 10, H - 16, "CITY BY CITY", GREEN, right=True)
    return b


# --------------------------------------------------------------------------- p2: the names

def mourner(b, x, foot, tall, bow=0.35, kneel=False, seed=0):
    """A small figure, dark against the candlelight, head bowed (``bow``: how far the head drops
    forward), lit from below at its front; ``kneel`` lowers it and bends it forward."""
    dark = (14, 16, 30, 255)
    rim = AMBER + (150,)
    d = b.draw
    if kneel:
        tall *= 0.62
    w = max(3.0, tall * 0.3)
    r = max(1.6, tall * 0.11)
    hip, neck = foot - tall * (0.2 if kneel else 0.42), foot - tall * 0.8
    lean = tall * (0.18 if kneel else 0.0)
    if not kneel:
        d.line([(x - w * 0.25, foot), (x - w * 0.2, hip)], fill=dark, width=2)
        d.line([(x + w * 0.25, foot), (x + w * 0.2, hip)], fill=dark, width=2)
    else:
        d.line([(x - w * 0.6, foot), (x + w * 0.3, foot - 1), (x + w * 0.1, hip)], fill=dark, width=2)
    d.polygon([(x - w / 2, hip), (x + w / 2, hip), (x + w / 2 - lean * 0.2, neck), (x - w / 2 + lean, neck)], fill=dark)
    hx, hy = x - w * 0.1 + lean + bow * r * 1.4 * (0 if seed % 2 else 0.4), neck - r * (1.6 - bow)
    d.ellipse([hx - r, hy - r, hx + r, hy + r], fill=dark)
    d.line([(x - w / 2, hip), (x + w / 2, hip)], fill=rim, width=1)
    d.line([(x - w / 2 + 1, hip - 1), (x - w / 2 + lean + 1, neck + 2)], fill=AMBER + (90,), width=1)


def names():
    """p2: the memorial wall at dawn: names, candles, a few people, the skyline beyond."""
    b = OutroBoard()
    t = b.yy / H
    b.base = b.base * 0.9 + np.dstack([18 * (1 - t), 14 * (1 - t), 18 * (1 - t)])
    b.glow(520, -10, 220, (70, 54, 52), 1.0)                     # the first light, cold and grey
    b.glow(560, 0, 90, (90, 64, 40), 0.8)
    rng = np.random.default_rng(1402)
    top, base = 92, 170
    # the skyline beyond the wall, some towers broken off
    x = 0
    while x < W:
        w = int(rng.integers(18, 46))
        hgt = int(rng.integers(30, 76))
        broken = rng.random() < 0.35
        y0 = top - hgt
        pts = [(x, top), (x, y0 + (8 if broken else 0)), (x + w * 0.4, y0 + (0 if not broken else 14)),
               (x + w * 0.6, y0 + (4 if broken else 0)), (x + w, y0 + (16 if broken else 0)), (x + w, top)]
        bi.fill(b, pts, (12, 16, 34))
        b.line(pts[1:-1], CYAN_DIM, 1, 90 if not broken else 60)
        for _ in range(int(hgt / 9)):
            if rng.random() < 0.4:
                wx, wy = x + rng.uniform(3, w - 4), rng.uniform(y0 + 18, top - 6)
                b.draw.rectangle([wx, wy, wx + 1, wy], fill=(AMBER if rng.random() < 0.7 else CYAN_DIM) + (170,))
        x += w + int(rng.integers(0, 6))
    # the street in front, its lines running out to the bottom edge
    bi.fill(b, [(0, base), (W, base), (W, H), (0, H)], (10, 11, 24))
    for k in range(-6, 7):
        b.line([(336 + k * 54, base), (336 + k * 150, H)], CYAN_DIM, 1, 50)
    # the wall: panels of engraved names, photos and notes pinned between them
    wx0, wx1 = 36, W - 36
    bi.fill(b, [(wx0, top), (wx1, top), (wx1, base), (wx0, base)], (30, 36, 62))
    b.line([(wx0 - 2, top), (wx1 + 2, top)], CYAN, 1, 200)
    b.line([(wx0, base), (wx1, base)], CYAN_DIM, 1, 160)
    panels = 8
    pw = (wx1 - wx0) / panels
    for i in range(panels):
        px0 = wx0 + i * pw
        if i:
            b.line([(px0, top + 1), (px0, base - 1)], (20, 26, 48), 1, 255)
        for row, y in enumerate(range(top + 10, base - 6, 5)):
            if i == 3 and row == 0 or i == 4 and row == 0:
                continue
            xx = px0 + 6
            while True:
                n = rng.integers(10, 24)
                if xx + n > px0 + pw - 6:
                    break
                b.line([(xx, y), (xx + n, y)], WHITE, 1, int(rng.integers(110, 190)))
                xx += n + rng.integers(4, 8)
        for _ in range(int(rng.integers(1, 4))):                 # photos and notes
            nx, ny = px0 + rng.uniform(6, pw - 14), rng.uniform(top + 14, base - 22)
            if rng.random() < 0.5:
                b.draw.rectangle([nx, ny, nx + 6, ny + 8], fill=(150, 150, 160, 230), outline=(60, 60, 70, 255))
                b.draw.rectangle([nx + 2, ny + 2, nx + 4, ny + 5], fill=(70, 60, 60, 255))
            else:
                b.draw.polygon([(nx, ny), (nx + 9, ny + 1), (nx + 8, ny + 8), (nx - 1, ny + 7)], fill=(200, 196, 170, 220))
    b.label(W // 2 - raster.text_width("WE REMEMBER") // 2, top + 6, "WE REMEMBER", WHITE)
    # candles and flowers along the foot of the wall
    candles = []
    for x in np.arange(wx0 + 4, wx1 - 4, 3.2):
        for _ in range(int(rng.integers(1, 4))):
            candles.append((x + rng.uniform(-2, 2), base + 3 + rng.uniform(0, 14) ** 1.1))
    for x, y in candles:
        b.glow(x, y - 2, 4.5, (150, 92, 30), 0.55)
    b.glow(W / 2, base + 8, 300, (40, 26, 10), 0.5)
    for x, y in candles:
        b.draw.rectangle([x, y - 2, x, y], fill=(220, 214, 200, 255))
        b.draw.point([(x, y - 3)], fill=(255, 220, 120, 255))
    for _ in range(40):
        x, y = rng.uniform(wx0, wx1), base + rng.uniform(2, 10)
        b.draw.point([(x, y)], fill=(RED if rng.random() < 0.5 else WHITE) + (220,))
    # a few people, heads bowed; one kneels to light a candle
    for i, (x, tall, bow, kneel) in enumerate(((118, 44, 0.4, False), (136, 40, 0.5, False), (262, 46, 0.3, False),
                                              (304, 44, 0.6, True), (430, 43, 0.45, False),
                                              (448, 32, 0.5, False), (566, 45, 0.35, False))):
        mourner(b, x, 216 + (i % 3) * 3, tall, bow, kneel, i)
    b.glow(314, 206, 7, (220, 140, 40), 1.0)
    b.draw.point([(314, 205)], fill=(255, 230, 140, 255))
    b.title("NOVA LAGOS - IKOYI, DAWN")
    b.label(W - 10, 9, "REMEMBER THE NAMES", AMBER, right=True)
    b.label(10, 28, "NOVA LAGOS - COUNTING", WHITE)
    b.label(10, 38, "GENEVA CONCORD - COUNTING", WHITE)
    b.label(10, 48, "THE CDF - COUNTING", WHITE)
    b.label(W - 10, H - 16, "THEN GET SOME SLEEP", CYAN_DIM, right=True)
    return b


# --------------------------------------------------------------------------- p3: Mars

def mars():
    """p3: Ares Landing's priority signal from Mars to Earth on the long-range plot."""
    b = OutroBoard()
    mx, my, mr = 594, 156, 92
    b.glow(mx, my, 150, (60, 18, 10), 0.8)
    b.mars(mx, my, mr)
    b.planet(52, 168, 24, "earth", seed=19)
    b.ring(52, 168, 30, colour=CYAN, alpha=120)
    b.planet(92, 140, 6, "moon", seed=5)
    b.title("LONG RANGE - PRIORITY SIGNAL")
    b.label(W - 10, 9, "THREE WEEKS ALONE", AMBER, right=True)
    # Vrell holding orbit round Mars
    ro = mr + 20
    b.arc(mx, my, ro, ro, 140, 250, RED, 150)
    rng = np.random.default_rng(1503)
    for a in np.radians(np.linspace(150, 240, 9)):
        r = ro + rng.uniform(-4, 4)
        b.marker(mx + r * np.cos(a) + rng.uniform(-2, 2), my + r * np.sin(a), "chevron", RED, 3)
    for a in np.radians(rng.uniform(146, 244, 24)):
        r = mr + rng.uniform(8, 34)
        b.marker(mx + r * np.cos(a), my + r * np.sin(a), "dot", VIOLET, 1)
    b.label(372, 40, "VRELL - HOLDING ORBIT", RED)
    b.line([(470, 50), (mx + ro * np.cos(np.radians(222)), my + ro * np.sin(np.radians(222)))], RED, 1, 180)
    # Ares Landing and its pulse; Tharsis Foundry cut off
    ax, ay = 548, 138
    for k, r in enumerate((5, 10, 16, 23, 31)):
        b.ring(ax, ay, r, colour=RED if k % 2 else AMBER, alpha=235 - 40 * k)
    b.glow(ax, ay, 9, (200, 60, 30), 0.9)
    b.marker(ax, ay, "diamond", AMBER, 3)
    b.line([(ax - 8, ay - 6), (474, 86)], AMBER, 1, 220)
    b.label(372, 76, "ARES LANDING", AMBER)
    b.label(372, 86, "UNDER SIEGE", RED)
    tx, ty = 600, 200
    b.marker(tx, ty, "diamond", GREY, 3)
    b.line([(tx - 6, ty + 3), (474, 214)], GREY, 1, 220)
    b.label(372, 204, "THARSIS FOUNDRY", GREY)
    b.label(372, 214, "CUT OFF", RED)
    b.label(W - 10, H - 16, "MARS", CYAN_DIM, right=True)
    b.label(10, H - 16, "EARTH", CYAN_DIM)
    b.label(W - 10, 26, "HOLDING", AMBER, right=True)
    b.big(W - 10, 36, "21 DAYS", AMBER, right=True)
    # the signal: Mars to Earth, packets along it
    p0, p1, p2 = np.array([ax - 30.0, ay + 10]), np.array([320.0, 196]), np.array([86.0, 172])
    track = [tuple((1 - s) ** 2 * p0 + 2 * (1 - s) * s * p1 + s ** 2 * p2) for s in np.linspace(0, 1, 13)]
    b.dashed(track[:-1], AMBER, 2)
    b.arrow(*track[-2], *track[-1], AMBER, dashed=False)
    for s in (0.22, 0.47, 0.72):
        q = (1 - s) ** 2 * p0 + 2 * (1 - s) * s * p1 + s ** 2 * p2
        b.glow(*q, 5, (160, 110, 30), 0.9)
    b.label(150, 204, "PRIORITY SIGNAL - RECEIVED 1 H AGO", AMBER)
    # the message and its pulse train
    mx0, my0 = 112, 30
    b.draw.rectangle([mx0, my0, mx0 + 236, my0 + 100], outline=CYAN_DIM + (220,))
    b.draw.rectangle([mx0, my0, mx0 + 236, my0 + 12], fill=(30, 10, 10, 200))
    b.label(mx0 + 6, my0 + 3, "PRIORITY ONE", RED)
    b.label(mx0 + 230, my0 + 3, "ARES LANDING, MARS", WHITE, right=True)
    b.label(mx0 + 6, my0 + 20, "MARS UNDER SIEGE", RED)
    b.label(mx0 + 6, my0 + 30, "THE COLONIES HOLDING ALONE", AMBER)
    b.label(mx0 + 6, my0 + 40, "REQUEST: ALL AVAILABLE", WHITE)
    pts = []
    for i in range(0, 224):
        ph = i % 56
        y = my0 + 80 - (14 * np.exp(-((ph - 10) / 2.0) ** 2) - 8 * np.exp(-((ph - 16) / 1.6) ** 2)
                        + 4 * np.exp(-((ph - 22) / 1.4) ** 2) + 0.8 * np.sin(i * 1.7))
        pts.append((mx0 + 6 + i, y))
    b.line([(mx0 + 6, my0 + 80), (mx0 + 230, my0 + 80)], CYAN_DIM, 1, 90)
    b.line(pts, AMBER, 1, 240)
    b.label(mx0 + 6, my0 + 88, "REPEATING", CYAN_DIM)
    return b


# --------------------------------------------------------------------------- p4: Aegis ships out

TRANSPORT = (240, 126)               # the sprite's size, nose right
T_SCALE = 0.74                       # px per model unit


def transport_scene():
    """The heavy transport bound for Mars, nose up the model's +y (turned right when drawn): a long
    fuselage with a raised cargo spine, a broad delta on two engine nacelles, canards, twin fins,
    four main bells; the shuttle's white hull, CDF-blue spine stripe, lit windows, nav lights."""
    S = shuttle
    wing = [(14.0, 30.0), (30.0, 10.0), (78.0, -84.0), (80.0, -104.0), (40.0, -110.0), (14.0, -110.0)]
    canard = [(12.0, 92.0), (30.0, 76.0), (32.0, 70.0), (12.0, 74.0)]

    def scene(p):
        q = mirror_x(p)
        x, y = q[:, 0], q[:, 1]
        fus = sd_capsule(p, (0, -112, 9), (0, 112, 9), 15, 13)
        nose = sd_ellipsoid(p, (0, 118, 9), (13, 20, 10))
        spine = sd_box(p, (0, -10, 18), (10, 74, 5), 4)
        d, m = union((fus, S.HULL), (nose, S.HULL), (spine, S.HULL), k=4.0)
        wings = sd_plate(q, wing, 5.0, 2.6, 1.0, taper=lambda xx, yy: np.clip(1.25 - np.abs(xx) / 90, 0.35, 1))
        cans = sd_plate(q, canard, 8.0, 1.4, 0.6)
        nac = sd_capsule(q, (42, -40, 7), (42, -112, 7), 8, 7)
        fin = sd_box(rotate_y(q - np.array([26.0, -100.0, 18.0]), 0.35), (0, 0, 0), (1.4, 12, 12), 1.0)
        d, m = union((d, m), (wings, S.HULL), (cans, S.HULL), (nac, S.HULL), (fin, S.HULL), k=3.0)
        bells = [(sd_cylinder_y(p, (ex, -122, 9), 6.0, 5.0), S.DARK) for ex in (-8.0, 8.0)]
        bells += [(sd_cylinder_y(q, (42, -118, 7), 5.4, 4.0), S.DARK)]
        nozzles = [(sd_cylinder_y(p, (ex, -126, 9), 4.4, 1.2), S.CAP) for ex in (-8.0, 8.0)]
        nozzles += [(sd_cylinder_y(q, (42, -121, 7), 4.0, 1.0), S.CAP)]
        glass = sd_ellipsoid(p, (0, 122, 15), (7, 5, 3))
        d, m = union((d, m), *bells, k=1.0)
        d, m = union((d, m), *nozzles, (glass, S.GLASS), k=0.4)
        d, m = union((d, m), (sd_sphere(p, (-79, -102, 6), 2.0), S.NAV_RED), (sd_sphere(p, (79, -102, 6), 2.0), S.NAV_GREEN))
        z = p[:, 2]
        m = np.where((m == S.HULL) & (x < 2.2) & (y > -78) & (y < 60) & (z > 21), S.BLUE, m)
        m = np.where((m == S.HULL) & (y > 128), S.CAP, m)
        win = (np.abs(x - 12.5) < 1.2) & (((y + 90) % 6.0) < 2.6) & (y > -90) & (y < 100) & (z > 10) & (z < 20)
        m = np.where(win & (m == S.HULL), S.WINDOW, m)
        band = (np.abs(y - (-60 - 0.25 * (x - 30))) < 4) & (x > 34) & (x < 74) & (z < 9)
        m = np.where(band & (m == S.HULL), S.BLUE, m)
        return d, m

    def turned(p):                    # the nose to the right
        return scene(rotate_z(p, -np.pi / 2))
    return turned, shuttle.materials(lit=True)


def transport_sprite():
    w, h = TRANSPORT
    scene, mats = transport_scene()
    hi = sdf.render(scene, mats, (w * 2, h * 2), w / T_SCALE, z_top=40.0)
    return sprite.make_sprite(hi, 2, SPRITE_COLOURS, crisp=70)


def aegis():
    """p4: Aegis Wing on the apron at dusk, the Mars transport behind them, T-72 H."""
    b = OutroBoard()
    b.base *= 0.85
    b.glow(-80, 120, 300, (130, 58, 24), 1.1)                  # dusk, the sun gone down in the west
    ax0, ay0, ax1, ay1 = 20, 22, 560, 226
    bi.fill(b, [(ax0, ay0), (ax1, ay0), (ax1, ay1), (ax0, ay1)], (18, 24, 42), 0.85)
    for x in range(ax0, ax1, 36):
        b.line([(x, ay0), (x, ay1)], CYAN_DIM, 1, 40)
    for y in range(ay0, ay1, 36):
        b.line([(ax0, y), (ax1, y)], CYAN_DIM, 1, 40)
    b.draw.rectangle([ax0, ay0, ax1, ay1], outline=CYAN_DIM + (200,))
    for x, y in ((ax0 + 6, ay0 + 6), (ax1 - 6, ay0 + 6), (ax1 - 6, ay1 - 6), (ax0 + 6, ay1 - 6)):
        b.glow(x, y, 46, (120, 86, 36), 0.7)                     # the floodlights coming on
        b.marker(x, y, "dot", AMBER, 2)
    # the transport, its ramp down at the tail, the crew's path to it
    tx, ty = 320, 80
    tsp = transport_sprite()
    bi.shadow(b, tsp, tx, ty, 14, 9, 0.6)
    b.sprite(tsp, tx, ty)
    rx = tx - int(132 * T_SCALE)                                  # the tail, its ramp down behind it
    bi.fill(b, [(rx + 8, ty - 7), (rx + 8, ty + 7), (rx - 14, ty + 9), (rx - 14, ty - 9)], (70, 74, 86))
    b.glow(rx - 10, ty, 12, (120, 96, 56), 0.7)
    b.label(30, 36, "CDF HEAVY TRANSPORT", WHITE)
    b.label(30, 46, "BOUND FOR MARS", AMBER)
    b.line([(rx - 16, ty), (100, ty), (80, 56)], CYAN_DIM, 1, 160)
    b.label(104, ty + 4, "RAMP DOWN", CYAN_DIM)
    # the wing on its pads, long shadows away from the dusk
    pads = [("LANCER", "ship_2", CYAN), ("ROOK", "rook", AMBER), ("AEGIS TWO", "ship_2", GREY),
            ("AEGIS 3", "ship_2", GREY), ("AEGIS 4", "ship_2", GREY)]
    for i, (name, kind, colour) in enumerate(pads):
        x, y = 104 + 92 * i, 172
        b.draw.rectangle([x - 30, y - 30, x + 30, y + 30], outline=CYAN_DIM + (170,))
        img = bi.rook_craft() if kind == "rook" else bi.asset("ship_2")
        bi.shadow(b, img, x, y, 9, 5, 0.6)
        b.sprite(img, x, y)
        b.label(x - 28, y + 34, name, colour)
        if i < 2:
            b.bracket(x - 32, y - 32, x + 32, y + 32, colour, 5)
    b.title("CDF COASTAL AIRBASE - DUSK")
    b.label(W - 10, 9, "WE TAKE THE FIGHT TO THEM", AMBER, right=True)
    px = 574
    b.label(px, 40, "AEGIS WING", WHITE)
    b.label(px, 50, "SHIPS OUT", CYAN)
    b.big(px, 66, "T-72 H", AMBER)
    b.line([(px, 92), (W - 12, 92)], CYAN_DIM, 1, 140)
    b.label(px, 100, "EARTH", CYAN_DIM)
    b.label(px, 110, "TO MARS", RED)
    b.label(W - 10, H - 16, "AEGIS ACTUAL OUT", CYAN_DIM, right=True)
    return b


# --------------------------------------------------------------------------- out

IMAGES = {"stump": stump, "names": names, "mars": mars, "aegis": aegis}


def render(name):
    return IMAGES[name]().image()


def review(images):
    cols = 2
    rows = -(-len(images) // cols)
    sheet = raster.sheet(16 + cols * (W + 16), 56 + rows * (H + 30),
                         f"ACT 2 OUTRO IMAGES (FINAL {ROUND.upper()}): ONE PER PAGE",
                         f"PRODUCTION ART, M5 PART H - {ROUND.upper()}")
    raster.draw_text(sheet, 16, 38, f"{len(images)} IMAGES {W}X{H} AT 1X, AS THE BRIEFING SCREEN DRAWS THEM ABOVE THE TEXT",
                     raster.LABEL)
    for i, (name, img) in enumerate(images.items()):
        img = img.convert("RGBA")
        x, y = 16 + (i % cols) * (W + 16), 54 + (i // cols) * (H + 30)
        sheet.alpha_composite(img, (x, y))
        raster.draw_text(sheet, x, y + H + 6, f"{PREFIX}{name} ({artkit.colour_count([img])} COLOURS)".upper(),
                         raster.LABEL_DIM)
    CONCEPT.mkdir(parents=True, exist_ok=True)
    path = CONCEPT / f"act-2-outro-final-{ROUND}-a.png"
    sheet.convert("RGB").save(path, optimize=True)
    print(f"review: {path.relative_to(ROOT)}")


def main(args):
    flags = {a for a in args if a.startswith("--")}
    picked = [a.removeprefix(PREFIX) for a in args if not a.startswith("--")] or list(IMAGES)
    wrong = [n for n in picked if n not in IMAGES] + sorted(flags - {"--assets", "--review"})
    if wrong:
        sys.exit(f"unknown: {' '.join(wrong)} (names: {' '.join(IMAGES)}; flags: --assets --review)")
    images = {}
    if "--review" in flags:                                   # the sheet from the files in assets/
        for name in IMAGES:
            path = OUT / f"{PREFIX}{name}.png"
            if path.exists():
                images[name] = Image.open(path).convert("RGB")
    for name in IMAGES:
        if name not in images or ("--assets" in flags and name in picked):
            images[name] = render(name)
        if "--assets" in flags and name in picked:
            OUT.mkdir(parents=True, exist_ok=True)
            artkit.save_png(images[name], OUT / f"{PREFIX}{name}.png", SOURCE)
            print(f"wrote {(OUT / f'{PREFIX}{name}.png').relative_to(ROOT)}")
    review(images)


if __name__ == "__main__":
    main(sys.argv[1:])
