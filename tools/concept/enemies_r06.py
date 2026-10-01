#!/usr/bin/env python3
"""Concept round 06 - the Act 2 introductions and the round 05 roster additions without art.

Each unit gets a PNG sheet (source render, native sprite(s), parts and angle grids where
relevant, animation frames, a frame from the play field) and a GIF of it moving in a fitting
480x540 play-field scene with the player for scale. Turning units use pre-rendered headings
and ``enemy_rigs.ModelSpaceAngleSprites`` so seams and patterns turn with the body. Colours
follow the adopted role colours (design/enemies/README.md "Role colours").

Outputs (design/enemies/...):
  ground/concept/creeper-r06-a.{png,gif}          six-legged salamander walker, 5-way fan
  ground/concept/hive-node-r06-a.{png,gif}        hardened spawner mound, iris spawns Skitters
  air/concept/wraith-r06-a.{png,gif}              cloaked manta ghost, decloaks behind the player
  air/concept/lamprey-r06-a.{png,gif}             eel chaser that latches and drains the shield
  naval/concept/driftjelly-r06-a.{png,gif}        floating jellyfish mine, ring pulse at 96 px
  naval/concept/reef-spitter-r06-a.{png,gif}      barnacle gun on a biomass raft, 3-way fan
  naval/concept/skimmer-r06-a.{png,gif}           fish-like skiff weaving between ice floes
  ground/concept/threadcrawler-r06-a.{png,gif}    centipede chain, travelling wave of spores
  ground/concept/halo-platform-r06-a.{png,gif}    Ascendancy rotating turret ring, shielded core
  ground/concept/dust-devil-r06-a.{png,gif}       spinning vortex organism, core opens on top
  naval/concept/spiral-nautilus-r06-a.{png,gif}   shell roller orbiting the player under Europa
  concept/lineup-r06-a.png                        the new units next to the player and r04/r05 units

Models: render/r06_models.py. Shared helpers: enemies_r05.py (sheet layout, player, bullets),
enemies_r05b.py (model-space source panel), enemy_rigs.py (angle sprites, chains, GIFs).
Run: python3 tools/concept/enemies_r06.py [name ...] [lineup]   (no args = everything)
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, str(Path(__file__).resolve().parent))
import enemies_r03 as e3  # noqa: E402
import enemies_r04 as e4  # noqa: E402
import enemies_r05 as r5  # noqa: E402
import enemies_r05b as r5b  # noqa: E402
from render import enemy_rigs as rig  # noqa: E402
from render import r06_models as m6  # noqa: E402
from render import raster, sprite, terrain  # noqa: E402
from render.config import ROOT  # noqa: E402
from render.palette import B  # noqa: E402

r5.SUB = "CONCEPT ROUND 06 - 960X540"       # sheet subtitle used by r5.build_sheet
FW, FH, FPS, TAU = r5.FW, r5.FH, r5.FPS, 2 * np.pi
SHOTS, GOLD = e3.SHOTS, e3.GOLD
times, grid, paste, Bullets = r5.times, r5.grid, r5.paste, r5.Bullets
source_panel = r5b.source_panel
VIOLET = (154, 77, 255)


def save_unit(category, slug, sheet, frames):
    png = r5.out(category, f"{slug}-r06-a.png")
    sheet.convert("RGB").save(png, optimize=True)
    size = rig.write_gif(frames, r5.out(category, f"{slug}-r06-a.gif"), fps=FPS)
    print(f"wrote {png.relative_to(ROOT)} and .gif ({size / 1e6:.1f} MB, {len(frames)} frames)")


def dim(sp, k):
    """Darken a sprite's colour (keeps alpha) - for things far from the light under water."""
    a = np.array(sp).astype(np.float64)
    a[..., :3] *= k
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


def tint(sp, col, k, alpha=1.0):
    a = np.array(sp).astype(np.float64)
    a[..., :3] = a[..., :3] * (1 - k) + np.array(col) * k
    a[..., 3] *= alpha
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


def ring_shots(img, x, y, radius, n=8, phase=0.0, r=4, ring=None):
    for a in np.linspace(0, TAU, n, endpoint=False) + phase:
        e3.orb(img, x + np.cos(a) * radius, y + np.sin(a) * radius, r, ring=ring)


def explosion(img, x, y, s, size=1.0):
    """Small explosion flash for kills (0 <= s <= 1)."""
    if not 0 <= s <= 1:
        return
    rr = (8 + 20 * s) * size
    raster.add_light(img, x, y, rr * 1.6, (255, 150, 60), 1.0 - s)
    d = ImageDraw.Draw(img)
    d.ellipse([x - rr * 0.6, y - rr * 0.6, x + rr * 0.6, y + rr * 0.6],
              fill=(255, 230, 160, int(230 * (1 - s))))
    for k in range(7):
        a = k * 0.9 + s
        dx, dy = np.cos(a) * rr * (0.6 + s), np.sin(a) * rr * (0.6 + s)
        d.ellipse([x + dx - 2, y + dy - 2, x + dx + 2, y + dy + 2], fill=(255, 200, 120, 255))


def edge_warning(img, t, xs, label="REAR!"):
    if int(t * 8) % 2:
        return
    d = ImageDraw.Draw(img)
    for x0 in xs:
        d.polygon([(x0 - 14, FH - 8), (x0 + 14, FH - 8), (x0, FH - 26)],
                  outline=(255, 70, 50, 255), fill=(90, 10, 10, 200))
    raster.draw_text(img, int(np.mean(xs)) - 14, FH - 40, label, (255, 90, 70))


# --------------------------------------------------------------------------- backgrounds

def city_bg(seed, ruin=False):
    """Night megacity rooftops (Act 2): street grid with sodium lights, roof blocks with vents,
    lit windows and rooftop gardens; ``ruin`` adds rubble and Vrell creep."""
    base, streets = terrain.city_ground(FW, FH, seed=seed, period=False, block=60, street=10)
    img = base.copy()
    d = ImageDraw.Draw(img)
    rng = np.random.default_rng(seed)
    for by in range(0, FH + 60, 60):
        for bx in range(0, FW + 60, 60):
            if rng.random() < 0.85:
                x0, y0 = bx + 12 + rng.integers(0, 4), by + 12 + rng.integers(0, 4)
                x1, y1 = bx + 58 - rng.integers(0, 6), by + 58 - rng.integers(0, 6)
                v = rng.uniform(0.8, 1.15)
                col = tuple(int(c * v) for c in (48, 50, 74))
                d.rectangle([x0, y0, x1, y1], fill=col + (255,), outline=(28, 28, 44, 255))
                d.line([x0, y0, x1, y0], fill=(70, 74, 104, 255))
                d.line([x0, y0, x0, y1], fill=(70, 74, 104, 255))
                if rng.random() < 0.35:     # rooftop garden
                    d.rectangle([x0 + 5, y0 + 5, x0 + 22, y1 - 6], fill=(22, 52, 30, 255))
                    for _ in range(6):
                        tx, ty = rng.integers(x0 + 6, x0 + 21), rng.integers(y0 + 6, y1 - 7)
                        d.ellipse([tx - 2, ty - 2, tx + 2, ty + 2], fill=(40, 96, 46, 255))
                for _ in range(rng.integers(0, 3)):  # vents
                    vx, vy = rng.integers(x0 + 4, x1 - 10), rng.integers(y0 + 4, y1 - 8)
                    d.rectangle([vx, vy, vx + 7, vy + 5], fill=(30, 30, 44, 255))
                for _ in range(rng.integers(2, 7)):  # lit windows / beacons
                    wx, wy = rng.integers(x0 + 2, x1 - 2), rng.integers(y0 + 2, y1 - 2)
                    d.point((wx, wy), fill=(255, 210, 120, 255))
    if ruin:
        for _ in range(60):
            x, y = rng.integers(0, FW), rng.integers(0, FH)
            r = rng.integers(2, 6)
            d.ellipse([x - r, y - r, x + r, y + r], fill=(30, 28, 34, 255))
    for x in range(0, FW, 60):         # sodium lamps along the streets
        for y in range(0, FH, 60):
            raster.add_light(img, x + 5, y + 5, 10, (255, 150, 60), 0.45)
    return terrain.recede(img, amount=0.15, darken=0.8)


def creep(img, cx, cy, radius, seed):
    """Vrell biomass creep spreading around a hive: dark teal-black blobs with teal veins."""
    rng = np.random.default_rng(seed)
    d = ImageDraw.Draw(img)
    for _ in range(90):
        a, r = rng.uniform(0, TAU), radius * np.sqrt(rng.uniform(0, 1))
        x, y = cx + np.cos(a) * r, cy + np.sin(a) * r * 0.85
        s = rng.uniform(5, 16) * (1 - r / radius * 0.6)
        d.ellipse([x - s, y - s, x + s, y + s], fill=(10, 34, 34, 235))
    for j in range(9):
        a0 = j * TAU / 9 + rng.uniform(-0.2, 0.2)
        curl = rng.uniform(-0.6, 0.6)
        pts = [(cx + np.cos(a0 + curl * u * u) * radius * (0.25 + 0.55 * u),
                cy + np.sin(a0 + curl * u * u) * radius * (0.25 + 0.55 * u) * 0.85)
               for u in np.linspace(0, 1, 24)]
        d.line(pts, fill=(8, 50, 46, 230), width=4)
        d.line(pts, fill=(0, 140, 96, 150), width=1)


def ocean_bg(seed, storm=False):
    """Open ocean seen from above (Act 2): deep blue-green with wave crests and foam; ``storm``
    is darker with whitecaps."""
    n1 = raster.fbm(FW, FH, 48, seed, octaves=5, period=False)
    n2 = raster.fbm(FW, FH, 10, seed + 1, octaves=3, period=False)
    if storm:
        stops = [(0.0, (4, 14, 24)), (0.5, (10, 34, 50)), (0.8, (24, 60, 76)), (1.0, (70, 110, 120))]
    else:
        stops = [(0.0, (0, 20, 50)), (0.5, (0, 44, 90)), (0.8, (10, 80, 130)), (1.0, (60, 150, 190))]
    rgb = raster.ramp(stops, np.clip(n1 * 0.75 + n2 * 0.25, 0, 1))
    swell = raster.fbm(FW, FH, 90, seed + 2, octaves=2, period=False)
    yy = np.arange(FH)[:, None]
    wave = (swell * 2.5 + yy / 46.0) % 1.0             # long swell lines across the screen
    crest = np.clip(1 - np.abs(wave - 0.5) / 0.5, 0, 1) ** 10 * np.clip((n1 - 0.35) * 3, 0, 1)
    k = 0.5 if storm else 0.35
    rgb = rgb * (1 - k * crest[..., None]) + np.array((150, 195, 210)) * (k * crest[..., None])
    if storm:
        caps = (crest > 0.8) & (n2 > 0.6)
        rgb[caps] = np.array((205, 222, 230))
    return terrain.recede(raster.to_rgba_image(rgb), amount=0.1, darken=0.85)


def floes_bg(seed):
    """Arctic sea (Act 2, L13): black-blue water between white-blue ice floes with shaded rims."""
    n = raster.fbm(FW, FH, 70, seed, octaves=5, period=False)
    ice = n > 0.52
    rgb = np.zeros((FH, FW, 3))
    rgb[:] = (6, 22, 40)
    water = raster.fbm(FW, FH, 12, seed + 4, octaves=3, period=False)
    rgb[~ice] = np.array((4, 20, 38)) + water[~ice, None] * np.array((10, 24, 34))
    shade = r5_hill(n * 40)
    icecol = raster.ramp([(0.0, (70, 110, 140)), (1.0, (150, 180, 200))],
                         np.clip((n - 0.52) * 4, 0, 1))
    rgb[ice] = (icecol * shade[..., None])[ice]
    rim = ice & ~(np.roll(ice, 2, 0) & np.roll(ice, 2, 1))
    rgb[rim] = rgb[rim] * 0.7
    return terrain.recede(raster.to_rgba_image(np.clip(rgb, 0, 255)), amount=0.2, darken=0.75), ice


def r5_hill(h):
    gy, gx = np.gradient(h)
    return np.clip(1.0 + (gx + gy) * -0.6, 0.6, 1.3)


def canyon_bg(seed):
    """Valles canyon floor (Mars, L16): rust regolith floor with darker layered walls."""
    img = e3.regolith(FW, FH, seed, ramp_name="MARS", craters=5)
    a = np.array(img).astype(np.float64)
    xx = np.arange(FW)[None, :]
    yy = np.arange(FH)[:, None]
    wall = 70 + 18 * np.sin(yy / 60.0 + 1.0)
    left = xx < wall
    right = xx > FW - (70 + 18 * np.sin(yy / 50.0))
    strata = 0.65 + 0.12 * np.sin((xx + yy * 0.3) / 6.0)
    mask = left | right
    a[..., :3] = np.where(mask[..., None], a[..., :3] * strata[..., None] * 0.7, a[..., :3])
    edge = (np.abs(xx - wall) < 3) | (np.abs(xx - (FW - (70 + 18 * np.sin(yy / 50.0)))) < 3)
    a[edge, :3] = a[edge, :3] * 1.35
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


def europa_bg(seed):
    """Under Europa's ice (Act 4): near-black blue water, faint light shafts from cracks in the
    ice above, drifting particles."""
    n = raster.fbm(FW, FH, 60, seed, octaves=4, period=False)
    rgb = raster.ramp([(0.0, (0, 4, 12)), (0.6, (0, 14, 34)), (1.0, (4, 34, 64))], n)
    xx = np.arange(FW)[None, :] + np.arange(FH)[:, None] * 0.35
    for c, w in ((120, 18), (330, 26)):
        shaft = np.exp(-((xx - c) / w) ** 2) * 0.35
        rgb = rgb + shaft[..., None] * np.array((20, 70, 90))
    img = raster.to_rgba_image(np.clip(rgb, 0, 255))
    rng = np.random.default_rng(seed)
    d = ImageDraw.Draw(img)
    for _ in range(140):
        x, y = rng.integers(0, FW), rng.integers(0, FH)
        d.point((x, y), fill=(60, 110, 130, 255))
    return img


def asc_deck(seed):
    """Ascendancy fortress deck (Callisto HQ): black-violet hex plating with gold trim and red
    running lights."""
    img = Image.new("RGBA", (FW, FH), (14, 10, 22, 255))
    d = ImageDraw.Draw(img)
    rng = np.random.default_rng(seed)
    s = 22
    for j in range(-1, FH // (s - 4) + 2):
        for i in range(-1, FW // s + 2):
            cx, cy = i * s * 1.0 + (j % 2) * s / 2, j * (s - 4)
            v = rng.uniform(0.85, 1.15)
            col = tuple(int(c * v) for c in (34, 26, 50))
            pts = [(cx + np.cos(a) * s * 0.55, cy + np.sin(a) * s * 0.55)
                   for a in np.linspace(0, TAU, 6, endpoint=False) + np.pi / 6]
            d.polygon(pts, fill=col + (255,), outline=(16, 10, 26, 255))
    for x in (40, FW - 40):
        d.rectangle([x - 4, 0, x + 4, FH], fill=(60, 40, 10, 255))
        d.line([x - 4, 0, x - 4, FH], fill=(192, 112, 0, 255))
        for y in range(10, FH, 40):
            raster.add_light(img, x, y, 4, (230, 50, 40), 0.9)
    return terrain.recede(img, amount=0.1, darken=0.85)


# --------------------------------------------------------------------------- 1 Creeper

CRP_N, CRP_PHASES, CRP_STRIDE = 60, 6, 34.0
crp = rig.ModelSpaceAngleSprites(lambda phase=0.0: m6.creeper(phase), CRP_N, 16, factor=5,
                                 colors=28)


def crp_sprite(h, dist):
    k = int(dist / CRP_STRIDE * CRP_PHASES) % CRP_PHASES
    return crp.get(h, phase=k * TAU / CRP_PHASES)


def creeper():
    bg = city_bg(81)
    road = rig.catmull_rom([(-70, 95), (95, 95), (160, 155), (160, 275), (215, 335), (335, 335),
                            (395, 395), (395, 520), (420, 640)],
                           [0, 1.6, 2.5, 3.8, 4.7, 6.2, 7.1, 8.4, 9.6])
    convoy = [0.0, 1.1, 2.2]                 # three Creepers on the same road, spaced in time
    bullets = Bullets()
    for i, t0 in enumerate((2.0, 3.6, 5.2, 6.8, 8.0)):
        lag = convoy[i % 3]
        x, y = road(t0 - lag)
        px, py = r5.player_at(t0)
        base = np.arctan2(py - y, px - x)
        for k in range(-2, 3):
            a = base + k * 0.24
            bullets.fire(t0, x, y, x + np.cos(a) * 100, y + np.sin(a) * 100, 130, "orb")
    dists = [0.0] * 3
    prev = [None] * 3
    frames = []
    for t in times(9.6):
        f = bg.copy()
        for i, lag in enumerate(convoy):
            tt = t - lag
            if tt < 0:
                continue
            x, y = road(tt)
            if prev[i] is not None:
                dists[i] += np.hypot(x - prev[i][0], y - prev[i][1])
            prev[i] = (x, y)
            if -40 < x < FW + 40 and -40 < y < FH + 40:
                paste(f, crp_sprite(rig.path_heading(road, tt), dists[i]), x, y, "ground")
        bullets.draw(f, t)
        r5.draw_player(f, t)
        frames.append(f)
    angles = [crp.frame(k, phase=0.0) for k in range(16)]
    cycle = [crp.get(np.pi / 2, phase=k * TAU / CRP_PHASES) for k in range(CRP_PHASES)]
    panels = [("SOURCE, 260 PX", source_panel(lambda: m6.creeper(0.7))),
              ("16 HEADINGS (1X)", grid(angles, 8, 1)),
              ("WALK CYCLE: 6 FRAMES, DIAGONAL GAIT (2X)", grid(cycle, 6, 2))]
    notes = ["CREEPER - VRELL SIX-LEGGED SALAMANDER (SLATE CHITIN, VIOLET GLOW: GROUND GUNNER)",
             "60 PX. A LONG, LOW LIZARD-LIKE BODY THAT SWAYS WITH ITS GAIT, THREE SPLAYED LEG PAIRS",
             "WITH STICKY TOE PADS (IT CLINGS TO ROOFS AND WALLS) AND A FRILLED FAN GLAND ON ITS HEAD",
             "WITH FIVE GLOWING PORES: THE 5-WAY FAN. 16 HEADINGS X 6 WALK PHASES, PHASE DRIVEN BY",
             "DISTANCE. CREEPERS CRAWL IN CONVOYS ALONG ROADS AND ROOFTOPS. GLAND = WEAK POINT."]
    return r5.build_sheet("CREEPER - SIX-LEGGED VRELL SALAMANDER", panels, notes, frames[100]), frames


# --------------------------------------------------------------------------- 2 Hive Node

HIVE_N = 76
hive = rig.ModelSpaceAngleSprites(lambda iris=0.0, pulse=0.0: m6.hive_node(iris, pulse),
                                  HIVE_N, 1, factor=5, colors=36)
SKIT = e4.R04["skitter-a"]


def skitter(anim):
    return e3.render(SKIT[4], SKIT[3], round(float(np.clip(anim, -1, 1)), 1))[1]


def hive_node():
    bg = city_bg(92, ruin=True)
    HX, HY = 240, 190
    creep(bg, HX, HY, 120, 3)
    spawns = []                              # (t0, side)
    for t0 in (1.2, 3.4, 5.6, 7.8):
        spawns += [(t0, -1), (t0 + 0.15, 1)]
    frames = []
    for t in times(9.6):
        f = bg.copy()
        # iris opens for 0.9 s around every spawn wave
        iris = 0.0
        for t0 in (1.2, 3.4, 5.6, 7.8):
            if t0 - 0.5 < t < t0 + 0.4:
                iris = max(iris, 1.0 - abs(t - t0) / 0.5)
        pulse = round(0.5 + 0.5 * np.sin(t * 4.0), 1)
        sp = hive.get(0.0, iris=round(iris * 4) / 4, pulse=pulse)
        e3.place(f, sp, HX, HY, "ground")
        if iris > 0.4:
            raster.add_light(f, HX, HY, 28, (0, 255, 154), iris * 0.6)
        for t0, side in spawns:
            s = t - t0
            if 0 <= s < 4.0:
                # burst up out of the iris, curve sideways, then dive at the player
                px, py = r5.player_at(t0 + 2.5)
                x = HX + side * (60 * np.sin(min(s, 1.0) * np.pi / 2)) + (px - HX) * max(0, s - 1) / 3
                y = HY - 30 * np.sin(min(s, 1.0) * np.pi) + (py + 60 - HY) * max(0, s - 1) / 3
                paste(f, skitter(np.sin(t * 30)), x, y, "air")
        r5.draw_player(f, t)
        frames.append(f)
    irises = [hive.get(0.0, iris=v, pulse=0.5) for v in (0.0, 0.25, 0.5, 0.75, 1.0)]
    pulses = [hive.get(0.0, iris=0.0, pulse=v) for v in (0.0, 0.5, 1.0)]
    panels = [("SOURCE, 260 PX (IRIS OPEN)", source_panel(lambda: m6.hive_node(1.0, 0.5))),
              ("IRIS: CLOSED -> OPEN, 5 FRAMES (1X)", grid(irises, 5, 1)),
              ("LOBE PULSE: 3 FRAMES (1X)", grid(pulses, 3, 1))]
    notes = ["HIVE NODE - HARDENED VRELL SPAWNER (TEAL-BLACK CHITIN, TEAL GLOW: SPAWNER)",
             "76 PX, RADIAL: SEVEN FLESHY LOBES BETWEEN BONE ARMOUR PLATES AND SPIKES, ROOT TENDRILS",
             "SPREADING INTO THE CREEP AROUND IT. EVERY 4 S (SHORTENED HERE) THE CENTRAL IRIS OPENS",
             "AND TWO SKITTERS BURST OUT. HARDENED: ONLY ANTI-GROUND OR AREA WEAPONS HURT IT; THE",
             "OPEN IRIS (GLOWING THROAT) TAKES DOUBLE DAMAGE. KILLING IT STOPS THE CREEP SPREADING."]
    return r5.build_sheet("HIVE NODE - HARDENED VRELL SPAWNER", panels, notes, frames[24]), frames


# --------------------------------------------------------------------------- 3 Wraith

WR_N, WR_PHASES = 72, 4
wr = rig.ModelSpaceAngleSprites(lambda phase=0.0: m6.wraith(phase), WR_N, 16, factor=4,
                                colors=32)
WR_PATH = rig.catmull_rom([(300, -80), (300, 120), (290, 300), (250, 470), (170, 530),
                           (110, 500), (140, 470), (220, 480), (270, 500), (320, 470),
                           (410, 330), (470, 120), (520, -60)],
                          [0, 1.0, 2.0, 2.9, 3.5, 3.9, 4.25, 4.6, 5.0, 5.6, 6.4, 7.2, 8.2])
DECLOAK = (4.3, 4.6)                       # shimmer -> visible
WR_FIRE = (4.8, 5.2, 5.6)


def shimmer(img, sp, x, y, t, strength=1.0):
    """Cloaked: no sprite, only a refraction shimmer of the background under its silhouette
    plus a faint edge glint."""
    a = np.array(sp.getchannel("A")) > 0
    h, w = a.shape
    x0, y0 = int(round(x - w / 2)), int(round(y - h / 2))
    if x0 < 0 or y0 < 0 or x0 + w > FW or y0 + h > FH:
        return
    region = np.array(img.crop((x0, y0, x0 + w, y0 + h))).astype(np.float64)
    yy, xx = np.mgrid[0:h, 0:w]
    dx = (3 * strength * np.sin(yy / 4.0 + t * 9)).astype(int)
    dy = (2 * strength * np.cos(xx / 5.0 + t * 7)).astype(int)
    src = region[np.clip(yy + dy, 0, h - 1), np.clip(xx + dx, 0, w - 1)]
    out = np.where(a[..., None], src * 1.06 + 6, region)
    edge = a & ~(np.roll(a, 1, 0) & np.roll(a, -1, 0) & np.roll(a, 1, 1) & np.roll(a, -1, 1))
    out[edge, :3] = out[edge, :3] * 0.6 + np.array((180, 160, 255)) * 0.4
    img.paste(Image.fromarray(np.clip(out, 0, 255).astype(np.uint8), "RGBA"), (x0, y0))


def wraith():
    orig = r5.player_at
    r5.player_at = lambda t: (240 + 60 * np.sin(t * 0.9), 380)   # room behind the player
    try:
        return _wraith()
    finally:
        r5.player_at = orig


def _wraith():
    bg = r5.coast(103)
    bullets = Bullets()
    for t0 in WR_FIRE:
        x, y = WR_PATH(t0)
        px, py = r5.player_at(t0)
        for k in range(3):                    # burst: three shots up the screen
            bullets.fire(t0 + k * 0.08, x, y, px + (k - 1) * 14, py, 230, "orb")
    frames = []
    for t in times(8.2):
        f = bg.copy()
        x, y = WR_PATH(t)
        h = rig.path_heading(WR_PATH, t)
        ph = int(t * 6) % WR_PHASES
        sp = wr.get(h, phase=ph * TAU / WR_PHASES)
        if t < DECLOAK[0]:
            shimmer(f, sp, x, y, t)
        elif t < DECLOAK[1]:
            s = (t - DECLOAK[0]) / (DECLOAK[1] - DECLOAK[0])
            shimmer(f, sp, x, y, t, 1 - s)
            paste(f, r5.ghost(sp, s), x, y)
            raster.add_light(f, x, y, 30, VIOLET, 1 - s)
        else:
            paste(f, sp, x, y, "air")
        if 3.2 < t < 4.6:
            edge_warning(f, t, (130, 250))
        bullets.draw(f, t)
        r5.draw_player(f, t)
        frames.append(f)
    angles = [wr.frame(k, phase=0.0) for k in range(16)]
    cycle = [wr.get(np.pi / 2, phase=k * TAU / WR_PHASES) for k in range(WR_PHASES)]
    cloak = Image.new("RGBA", (WR_N * 3 + 40, WR_N + 20), (22, 24, 34, 255))
    demo_bg = r5.coast(7).crop((0, 0, cloak.width, cloak.height))
    cloak.alpha_composite(demo_bg)
    sp0 = wr.get(np.pi / 2, phase=0.0)
    shimmer(cloak, sp0, WR_N / 2 + 10, cloak.height / 2, 0.3)
    paste(cloak, r5.ghost(sp0, 0.5), WR_N * 1.5 + 20, cloak.height / 2)
    paste(cloak, sp0, WR_N * 2.5 + 30, cloak.height / 2)
    panels = [("SOURCE, 260 PX", source_panel(lambda: m6.wraith(0.5))),
              ("16 HEADINGS (1X)", grid(angles, 8, 1)),
              ("WING RIPPLE: 4 FRAMES (1X)", grid(cycle, 4, 1)),
              ("CLOAKED -> DECLOAKING -> VISIBLE (1X)", cloak)]
    notes = ["WRAITH - CLOAKED VRELL MANTA (RUST MEMBRANE, VIOLET GLOW: AMBUSHER, BURSTS)",
             "72 PX MANTA-RAY-LIKE GHOST WITH RIPPLING MEMBRANE WINGS, VIOLET VEINS AND TRAILING VEILS.",
             "CLOAKED ON HIGH-AIR IT IS ONLY A REFRACTION SHIMMER; IT PASSES OVER THE PLAYER, TURNS",
             "BEHIND (EDGE WARNING), DECLOAKS IN A VIOLET FLASH AND FIRES BURSTS UP THE SCREEN, THEN",
             "LEAVES. 16 HEADINGS X 4 RIPPLE FRAMES. ONLY HITTABLE WHILE VISIBLE. EYES = WEAK POINT."]
    return r5.build_sheet("WRAITH - DECLOAKS BEHIND THE PLAYER", panels, notes, frames[100]), frames


# --------------------------------------------------------------------------- 4 Lamprey

LMP_N, LMP_PHASES = 36, 4
lmp = rig.ModelSpaceAngleSprites(lambda phase=0.0, latched=0.0: m6.lamprey(phase, latched),
                                 LMP_N, 16, factor=6, colors=24)
LATCH = (2.6, 5.4)                         # latched on the player from .. until shaken off


def lamprey_player(t):
    """The player shakes hard left-right while a Lamprey is latched on."""
    x, y = r5.player_at(t)
    if LATCH[0] + 0.6 < t < LATCH[1]:
        x = 240 + 110 * np.sin((t - LATCH[0] - 0.6) * 9.0)
    return x, y


def lamprey():
    bg = ocean_bg(111, storm=True)
    rng = np.random.default_rng(3)
    rain = [(rng.uniform(0, FW), rng.uniform(0, FH), rng.uniform(380, 520)) for _ in range(70)]
    # chasers: (start, delay); each homes on the player with a turn-rate limit
    chasers = [dict(p=np.array(p0, float), v=np.array(v0, float), t0=t0, dead=None)
               for p0, v0, t0 in (((-30, 80), (160, 60), 0.0), ((510, 40), (-150, 70), 0.4),
                                  ((-30, 220), (170, 0), 1.0), ((510, 160), (-170, 20), 1.6))]
    latcher = chasers[0]
    kills = {1: 3.2, 2: 4.0, 3: 6.2}
    dt = 1 / FPS
    frames = []
    drop = None
    for t in times(8.0):
        f = bg.copy()
        px, py = lamprey_player(t)
        for i, c in enumerate(chasers):
            if t < c["t0"]:
                continue
            if c["dead"] is not None:
                explosion(f, *c["dead"], (t - kills[i]) / 0.5)
                continue
            if i in kills and t >= kills[i]:
                c["dead"] = tuple(c["p"])
                continue
            if c is latcher and LATCH[0] <= t < LATCH[1]:
                c["p"] = np.array((px - 16, py - 6))
                c["v"] = np.array((0.0, -60.0))
            elif c is latcher and t >= LATCH[1]:
                if drop is None:
                    drop = t
                c["v"] = np.array((-90.0, 140.0))
                c["p"] = c["p"] + c["v"] * dt
            else:
                want = np.array((px, py)) - c["p"]
                want = want / (np.linalg.norm(want) + 1e-6) * 210
                c["v"] = c["v"] + (want - c["v"]) * 2.2 * dt
                c["p"] = c["p"] + c["v"] * dt
            h = float(np.arctan2(c["v"][1], c["v"][0]))
            lat = 1.0 if (c is latcher and LATCH[0] <= t < LATCH[1]) else 0.0
            sp = lmp.get(h, phase=(int(t * 10) % LMP_PHASES) * TAU / LMP_PHASES, latched=lat)
            paste(f, sp, c["p"][0], c["p"][1], None if lat else "air")
        d = ImageDraw.Draw(f)
        for x, y, v in rain:                 # rain streaks
            yy = (y + v * t) % FH
            d.line([(x, yy), (x - 3, yy + 9)], fill=(150, 180, 200, 140))
        if int(t * 13) == 37:
            f.alpha_composite(Image.new("RGBA", (FW, FH), (200, 220, 255, 70)))   # lightning
        e3.place(f, e3.player_sprite(), px, py, "air")
        if LATCH[0] <= t < LATCH[1]:
            r = 30 + 2 * np.sin(t * 20)
            d.ellipse([px - r, py - r, px + r, py + r], outline=(80, 200, 255, 160), width=2)
            raster.draw_text(f, int(px) - 34, int(py) + 30, "SHIELD DRAIN", (120, 210, 255))
            if t > LATCH[0] + 0.6:
                raster.draw_text(f, 150, 20, "SHAKE IT OFF: HARD LEFT-RIGHT!", (255, 200, 120))
        for k in range(4):
            y = py - 40 - ((t * 420 + k * 105) % 420)
            if y > -10:
                e3.bolt(f, px - 10, y)
                e3.bolt(f, px + 10, y)
        frames.append(f)
    angles = [lmp.frame(k, phase=0.0) for k in range(16)]
    cycle = [lmp.get(np.pi / 2, phase=k * TAU / LMP_PHASES) for k in range(LMP_PHASES)]
    cycle.append(lmp.get(np.pi / 2, phase=0.0, latched=1.0))
    panels = [("SOURCE, 260 PX", source_panel(lambda: m6.lamprey(0.8))),
              ("16 HEADINGS (2X)", grid(angles, 8, 2)),
              ("SWIM: 4 FRAMES + LATCHED POSE (3X)", grid(cycle, 5, 3))]
    notes = ["LAMPREY - VRELL EEL CHASER (RUST CHITIN, TEAL GLOW: FAST, CONTACT / LATCH)",
             "36 PX. A SINUOUS EEL WITH A ROUND ORAL DISC RINGED BY GLOWING HOOKED TEETH. LAMPREYS",
             "STREAM IN AND HOME ON THE PLAYER WITH A LIMITED TURN RATE; ONE THAT REACHES THE SHIP",
             "LATCHES ON AND DRAINS THE SHIELD UNTIL SHAKEN OFF BY HARD LEFT-RIGHT MOVEMENT, THEN",
             "TUMBLES AWAY. 16 HEADINGS X 4 SWIM FRAMES + LATCHED. SHOWN OVER THE STORM FRONT (L12)."]
    return r5.build_sheet("LAMPREY - LATCHES ON AND DRAINS THE SHIELD", panels, notes,
                          frames[80]), frames


# --------------------------------------------------------------------------- 5 Driftjelly

JLY_N = 40
jly = rig.ModelSpaceAngleSprites(lambda pulse=0.0: m6.driftjelly(pulse), JLY_N, 1, factor=6,
                                 colors=28)
SUB_TINT = (20, 90, 110)


def submerged(sp):
    """``sub`` layer look: blue-green tint, reduced contrast, slightly soft."""
    s = tint(sp, SUB_TINT, 0.45, 0.85)
    return s.filter(ImageFilter.GaussianBlur(0.5))


def driftjelly():
    bg = ocean_bg(121)
    rng = np.random.default_rng(12)
    jellies = [dict(x=x, y=y, sub=s, ph=rng.uniform(0, 3), fired=None)
               for x, y, s in ((90, 120, False), (300, 60, True), (200, 250, False),
                               (390, 230, True), (120, 360, True), (330, 380, False),
                               (240, 470, True))]
    frames = []
    scroll = 30.0
    for t in times(8.0):
        f = bg.copy()
        px, py = r5.player_at(t)
        d = ImageDraw.Draw(f)
        for j in jellies:
            x = j["x"] + 12 * np.sin(t * 0.7 + j["ph"])
            y = (j["y"] + scroll * t) % (FH + 80) - 40
            pulse = round(0.5 + 0.5 * np.sin(t * 2.5 + j["ph"]), 1)
            sp = jly.get(0.0, pulse=pulse)
            if j["sub"]:
                paste(f, submerged(sp), x, y)
            else:
                d.ellipse([x - 24, y - 20, x + 24, y + 20], outline=(150, 200, 210, 160))  # ripple
                paste(f, sp, x, y)
            near = np.hypot(px - x, py - y) < 96
            if near and j["fired"] is None:
                j["fired"] = (t, x, y)
            if j["fired"] is not None:
                s = t - j["fired"][0]
                if s < 1.4:
                    if s < 0.25:
                        raster.add_light(f, x, y, 30, (168, 255, 42), 1 - s * 4)
                    ring_shots(f, j["fired"][1], j["fired"][2], 12 + 150 * s, 10, 0.2)
                elif s > 3.0:
                    j["fired"] = None
        r5.draw_player(f, t)
        frames.append(f)
    pulses = [jly.get(0.0, pulse=v) for v in (0.0, 0.3, 0.6, 1.0)]
    zone = Image.new("RGBA", (240, 240), (22, 24, 34, 255))
    zd = ImageDraw.Draw(zone)
    zd.ellipse([120 - 96, 120 - 96, 120 + 96, 120 + 96], outline=(168, 255, 42, 255))
    paste(zone, jly.get(0.0, pulse=0.0), 120, 120)
    raster.draw_text(zone, 60, 226, "TRIGGER RADIUS 96 PX", raster.LABEL_DIM)
    layers = Image.new("RGBA", (130, 70), (22, 24, 34, 255))
    layers.alpha_composite(ocean_bg(5).crop((0, 0, 130, 70)))
    paste(layers, jly.get(0.0, pulse=0.3), 34, 35)
    paste(layers, submerged(jly.get(0.0, pulse=0.3)), 96, 35)
    panels = [("SOURCE, 260 PX", source_panel(lambda: m6.driftjelly(0.3))),
              ("BELL PULSE: 4 FRAMES (3X)", grid(pulses, 4, 3)),
              ("SURFACE / SUBMERGED (1X)", layers),
              ("RING PULSE TRIGGER (1X)", zone)]
    notes = ["DRIFTJELLY - FLOATING VRELL MINE ORGANISM (OLIVE BELL, LIME GLOW: AREA DENIAL)",
             "40 PX, RADIAL: A GLOSSY BELL WITH LIME VEINS, FRILLED RIM AND EIGHT TRAILING TENTACLES;",
             "THE BELL CONTRACTS IN A SLOW PULSE. JELLIES DRIFT WITH THE CURRENT, AT THE SURFACE OR",
             "JUST BELOW IT (SUB LAYER: BLUE-GREEN TINT, SOFTER). WHEN THE PLAYER COMES WITHIN 96 PX",
             "THE BELL FLASHES AND PULSES A RING OF SHOTS. SUBMERGED ONES NEED ANTI-SUB WEAPONS."]
    return r5.build_sheet("DRIFTJELLY - FLOATING MINE THAT PULSES A RING", panels, notes,
                          frames[70]), frames


# --------------------------------------------------------------------------- 6 Reef Spitter

RAFT_N, GUN_N = 84, 36
raft = rig.ModelSpaceAngleSprites(lambda: m6.reef_raft(), RAFT_N, 1, factor=4, colors=32)
gun = rig.ModelSpaceAngleSprites(lambda recoil=0.0: m6.reef_gun(recoil), GUN_N, 32, factor=6,
                                 colors=24)


def reef_spitter():
    bg = ocean_bg(131)
    rafts = [(110, 60), (340, 170), (190, 330)]
    bullets = Bullets()
    fire_times = {0: (1.0, 3.4, 5.8), 1: (1.8, 4.2, 6.6), 2: (2.6, 5.0, 7.4)}
    scroll = 40.0
    for i, (rx, ry) in enumerate(rafts):
        for t0 in fire_times[i]:
            x, y = rx, (ry + scroll * t0) % (FH + 140) - 70
            px, py = r5.player_at(t0)
            base = np.arctan2(py - y, px - x)
            for k in (-1, 0, 1):
                a = base + k * 0.3
                bullets.fire(t0, x + np.cos(a) * 22, y + np.sin(a) * 22,
                             x + np.cos(a) * 200, y + np.sin(a) * 200, 150, "orb")
    frames = []
    for t in times(8.0):
        f = bg.copy()
        for i, (rx, ry) in enumerate(rafts):
            y = (ry + scroll * t) % (FH + 140) - 70
            bob = 1.5 * np.sin(t * 2.0 + i)
            paste(f, raft.get(0.0), rx, y + bob)
            px, py = r5.player_at(t)
            aim = np.arctan2(py - y, px - rx)
            rec = any(0 <= t - t0 < 0.15 for t0 in fire_times[i])
            paste(f, gun.get(aim, recoil=1.0 if rec else 0.0), rx, y + bob - 2)
            if rec:
                raster.add_light(f, rx + np.cos(aim) * 22, y + np.sin(aim) * 22, 14, VIOLET, 0.9)
        bullets.draw(f, t)
        r5.draw_player(f, t)
        frames.append(f)
    guns = [gun.frame(k, recoil=0.0) for k in range(32)]
    combo = Image.new("RGBA", (RAFT_N + 10, RAFT_N + 10), (0, 0, 0, 0))
    paste(combo, raft.get(0.0), combo.width / 2, combo.height / 2)
    paste(combo, gun.get(np.pi / 2), combo.width / 2, combo.height / 2 - 2)
    panels = [("SOURCE: GUN, 260 PX", source_panel(lambda: m6.reef_gun())),
              ("SOURCE: RAFT, 260 PX", source_panel(lambda: m6.reef_raft())),
              ("GUN: 32 HEADINGS (1X)", grid(guns, 16, 1)),
              ("RAFT + GUN (2X), RECOIL (3X)", grid([combo], 1, 2)),
              ("", grid([gun.get(np.pi / 2), gun.get(np.pi / 2, recoil=1.0)], 2, 3))]
    notes = ["REEF SPITTER - VRELL BARNACLE GUN ON A BIOMASS RAFT (SLATE, VIOLET GLOW: TURRET)",
             "36 PX GUN ON AN 84 PX RAFT. THE RAFT IS A FLOATING MAT OF KELP LOBES AND AIR BLADDERS",
             "THAT BOBS AND DRIFTS WITH THE SCROLL; THE PLATED BARNACLE TURNS AT 32 HEADINGS TO AIM",
             "AND FIRES A 3-WAY FAN FROM ITS THREE MOUTHS (RECOIL FRAME). A SURFACE (GROUND-LAYER)",
             "TARGET: SINK THE RAFT WITH AREA WEAPONS OR SHOOT THE GUN (WEAK POINT: THE MOUTHS)."]
    return r5.build_sheet("REEF SPITTER - BARNACLE GUN ON A RAFT", panels, notes, frames[44]), frames


# --------------------------------------------------------------------------- 7 Skimmer

SKM_N = 40
skm = rig.ModelSpaceAngleSprites(lambda phase=0.0: m6.skimmer(phase), SKM_N, 16, factor=6,
                                 colors=28)


def skimmer():
    bg, ice = floes_bg(141)
    routes = [
        lambda t: (-50 + 120 * t, 120 + 60 * np.sin(t * 2.2)),
        lambda t: (FW + 50 - 110 * (t - 1.5), 300 + 55 * np.sin(t * 2.4 + 1)),
        lambda t: (160 + 70 * np.sin((t - 3.0) * 2.0), -50 + 120 * (t - 3.0)),
        lambda t: (340 + 70 * np.sin((t - 3.6) * 2.0 + 2), -50 + 120 * (t - 3.6)),
        lambda t: (-50 + 115 * (t - 5.0), 420 + 50 * np.sin(t * 2.0)),
    ]
    starts = [0.0, 1.5, 3.0, 3.6, 5.0]
    trails = [[] for _ in routes]
    bullets = Bullets()
    for i, t0 in enumerate((1.4, 2.6, 4.4, 5.2, 6.6)):
        x, y = routes[i](t0)
        px, py = r5.player_at(t0)
        bullets.fire(t0, x, y, px, py, 190, "orb")
    frames = []
    for t in times(9.0):
        f = bg.copy()
        d = ImageDraw.Draw(f)
        for i, (route, t0) in enumerate(zip(routes, starts)):
            if t < t0:
                continue
            x, y = route(t)
            trails[i] = (trails[i] + [(x, y)])[-26:]
            for k in range(1, len(trails[i])):  # foam wake, fading
                a = int(200 * k / len(trails[i]))
                (x0, y0), (x1, y1) = trails[i][k - 1], trails[i][k]
                d.line([(x0, y0), (x1, y1)], fill=(200, 230, 240, a), width=2 + k // 9)
            if -40 < x < FW + 40 and -40 < y < FH + 40:
                h = rig.path_heading(route, t)
                paste(f, skm.get(h, phase=np.pi * (int(t * 8) % 2)), x, y, "ground")
        bullets.draw(f, t)
        r5.draw_player(f, t)
        frames.append(f)
    angles = [skm.frame(k, phase=0.0) for k in range(16)]
    beat = [skm.get(np.pi / 2, phase=0.0), skm.get(np.pi / 2, phase=np.pi)]
    panels = [("SOURCE, 260 PX", source_panel(lambda: m6.skimmer(0.5))),
              ("16 HEADINGS (2X)", grid(angles, 8, 2)),
              ("TAIL BEAT: 2 FRAMES (3X)", grid(beat, 2, 3))]
    notes = ["SKIMMER - VRELL FLYING-FISH SKIFF (RUST CHITIN, VIOLET GLOW: FAST, AIMED SHOTS)",
             "40 PX. A TORPEDO BODY WITH LONG OUTRIGGER PECTORAL FINS THAT SKIM THE WATER AND A",
             "FORKED TAIL; A VIOLET GLAND ON THE SNOUT FIRES AIMED SHOTS. SKIMMERS WEAVE BETWEEN THE",
             "ICE FLOES IN SINE PATHS FROM EVERY EDGE (LEFT, RIGHT, TOP) AND LEAVE A FOAM WAKE THAT",
             "SHOWS WHERE THEY CAME FROM. 16 HEADINGS X 2 TAIL FRAMES. SHOWN AT POLAR RELAY (L13)."]
    return r5.build_sheet("SKIMMER - SKIFF WEAVING BETWEEN THE FLOES", panels, notes,
                          frames[96]), frames


# --------------------------------------------------------------------------- 8 Threadcrawler

TH_HEAD, TH_TAIL = 54, 36
TH_SEGS = [50, 50, 48, 48, 46, 44, 42, 40, 38, 36, 34, 32]
TH_PHASES = 4
th_head = rig.ModelSpaceAngleSprites(lambda jaw=0.0: m6.thread_head(jaw), TH_HEAD, 16,
                                     factor=5, colors=28)
th_seg = {n: rig.ModelSpaceAngleSprites(lambda phase=0.0: m6.thread_segment(phase), n, 16,
                                        factor=5, colors=24) for n in set(TH_SEGS)}
th_tail = rig.ModelSpaceAngleSprites(lambda: m6.thread_tail(), TH_TAIL, 16, factor=5, colors=20)
TH_PATH = rig.catmull_rom([(240, -260), (220, -60), (170, 90), (230, 200), (320, 290),
                           (290, 400), (190, 470), (200, 560), (260, 700)],
                          [0, 1.6, 2.8, 3.8, 4.8, 5.8, 6.8, 7.8, 9.4])


def th_gaps():
    return [28] + [s * 0.46 for s in TH_SEGS[:-1]] + [TH_SEGS[-1] * 0.55]


def thread_draw(img, t, path=TH_PATH, fire=None):
    pts, heads = rig.chain_at(path, t, th_gaps())
    dist = 60 * t
    n = len(pts)
    for i in range(n - 1, -1, -1):
        x, y = pts[i]
        if i == 0:
            sp = th_head.get(heads[0], jaw=round(0.5 + 0.5 * np.sin(t * 6), 1))
        elif i == n - 1 and n == len(TH_SEGS) + 2:
            sp = th_tail.get(heads[i])
        else:
            k = min(i - 1, len(TH_SEGS) - 1)
            ph = int((dist / 26.0 - i * 0.7) * TH_PHASES) % TH_PHASES
            sp = th_seg[TH_SEGS[k]].get(heads[i], phase=ph * TAU / TH_PHASES)
        paste(img, sp, x, y, "ground")
        if fire is not None and 0 < i < n - 1 and abs(fire - i) < 0.6:
            raster.add_light(img, x, y, 14, (168, 255, 42), 0.9)
    return pts


def threadcrawler():
    bg = canyon_bg(151)
    bullets = Bullets()
    waves = (3.0, 6.2)
    for w0 in waves:
        for i in range(1, len(TH_SEGS) + 1):
            t0 = w0 + i * 0.12
            pts, _ = rig.chain_at(TH_PATH, t0, th_gaps())
            if i < len(pts):
                x, y = pts[i]
                px, py = r5.player_at(t0)
                bullets.fire(t0, x, y, px, py, 95, "acid")
    frames = []
    for t in times(9.4):
        f = bg.copy()
        fire = None
        for w0 in waves:
            if w0 <= t < w0 + 0.12 * (len(TH_SEGS) + 1):
                fire = (t - w0) / 0.12
        thread_draw(f, t, fire=fire)
        bullets.draw(f, t)
        r5.draw_player(f, t)
        frames.append(f)
    heads_ = [th_head.frame(k, jaw=0.5) for k in range(16)]
    segs = [th_seg[50].frame(k, phase=0.0) for k in range(16)]
    legs = [th_seg[50].get(np.pi / 2, phase=k * TAU / TH_PHASES) for k in range(TH_PHASES)]
    chain = Image.new("RGBA", (560, 150), (22, 24, 34, 255))
    thread_draw(chain, 6.0, path=lambda tt: (40 + 80 * tt, 75 + 30 * np.sin(tt * 1.4)))
    panels = [("SOURCE: HEAD, 260 PX", source_panel(lambda: m6.thread_head(0.6))),
              ("HEAD: 16 HEADINGS (1X)", grid(heads_, 8, 1)),
              ("SEGMENT: 16 HEADINGS (1X)", grid(segs, 8, 1)),
              ("LEG RIPPLE: 4 FRAMES (2X)", grid(legs, 4, 2)),
              ("CHAIN 1X: SEGMENTS FOLLOW THE HEAD'S PATH, LEGS RIPPLE", chain)]
    notes = ["THREADCRAWLER - VRELL CENTIPEDE CHAIN (OLIVE CHITIN, LIME GLOW: SPORES, AREA)",
             "54 PX HEAD WITH CURVED FORCIPULES AND ANTENNAE, 12 OVERLAPPING LEGGED SEGMENTS (50 -> 32",
             "PX) AND A TAIL WITH CERCI, ABOUT 300 PX LONG. EVERY PART FOLLOWS THE HEAD'S PATH ALONG THE CANYON",
             "FLOOR (16 HEADINGS); THE LEG RIPPLE RUNS DOWN THE BODY (PHASE OFFSET PER SEGMENT). EACH",
             "SEGMENT'S SPORE PORE FIRES IN TURN: A TRAVELLING WAVE OF SLOW SPORES. HEAD = VITAL."]
    return r5.build_sheet("THREADCRAWLER - CENTIPEDE WITH A WAVE OF SPORES", panels, notes,
                          frames[132]), frames


# --------------------------------------------------------------------------- 9 Halo Platform

HALO_R = 96                                # ring radius in px
CORE_N, SEG_N, TUR_N = 104, 110, 36
halo_core = rig.ModelSpaceAngleSprites(lambda: m6.halo_core(), CORE_N, 1, factor=4, colors=40,
                                       post=e4.rim)
halo_seg = rig.ModelSpaceAngleSprites(lambda: m6.halo_segment(), SEG_N, 32, factor=4,
                                      colors=32, post=e4.rim)
halo_tur = rig.ModelSpaceAngleSprites(lambda: m6.halo_turret(), TUR_N, 32, factor=6, colors=24,
                                      post=e4.rim)
HALO_KILLS = {1: 3.0, 3: 5.0, 5: 6.6}      # turret index -> destroyed at
SHIELD_DROP = 6.6


def halo_angle(t):
    """Ring rotation; speeds up each time a turret dies."""
    a, last, speed = 0.0, 0.0, 0.5
    for k in sorted(HALO_KILLS.values()):
        if t <= k:
            break
        a += speed * (k - last)
        last, speed = k, speed + 0.35
    return a + speed * (t - last)


def halo_draw(img, cx, cy, t, aim=None, bullets=None):
    rot = halo_angle(t)
    shield = 1.0 if t < SHIELD_DROP else max(0.0, 1 - (t - SHIELD_DROP) / 0.5)
    paste(img, halo_core.get(0.0), cx, cy, "ground")
    if shield > 0:
        lay = Image.new("RGBA", img.size, (0, 0, 0, 0))
        d = ImageDraw.Draw(lay)
        r = 58
        for k in range(6):
            a0 = rot * 0.3 + k * TAU / 6
            pts = [(cx + np.cos(a0 + j * TAU / 6) * r, cy + np.sin(a0 + j * TAU / 6) * r)
                   for j in range(6)]
            d.polygon(pts, outline=(230, 60, 50, int(150 * shield)))
        d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=(255, 120, 80, int(200 * shield)),
                  width=2)
        img.alpha_composite(lay)
    tur_pos = []
    for k in range(6):
        a = rot + k * TAU / 6
        x, y = cx + np.cos(a) * HALO_R, cy + np.sin(a) * HALO_R
        paste(img, halo_seg.get(a + np.pi / 2), x, y, "ground")
        dead = k in HALO_KILLS and t >= HALO_KILLS[k]
        if dead:
            explosion(img, x, y, (t - HALO_KILLS[k]) / 0.6, 1.4)
            d = ImageDraw.Draw(img)
            d.ellipse([x - 9, y - 9, x + 9, y + 9], fill=(20, 12, 20, 255), outline=(90, 40, 20, 255))
            continue
        ta = np.arctan2(aim[1] - y, aim[0] - x) if aim is not None else a
        paste(img, halo_tur.get(ta), x, y)
        tur_pos.append((k, x, y, ta))
    return tur_pos


def halo_platform():
    bg = asc_deck(161)
    CX, CY = 240, 210
    bullets = Bullets()
    shots = []
    for t0 in np.arange(0.8, 9.0, 0.45):
        tp = halo_draw(Image.new("RGBA", (FW, FH)), CX, CY, t0, r5.player_at(t0))
        if tp:
            k, x, y, ta = tp[int(t0 * 3) % len(tp)]
            shots.append((t0, x + np.cos(ta) * 20, y + np.sin(ta) * 20))
    for t0, x, y in shots:
        px, py = r5.player_at(t0)
        bullets.fire(t0, x, y, px, py, 200, "gold")
    frames = []
    for t in times(9.0):
        f = bg.copy()
        halo_draw(f, CX, CY, t, r5.player_at(t))
        if t >= SHIELD_DROP + 0.5:
            raster.draw_text(f, 176, 330, "SHIELD DOWN - CORE EXPOSED", (255, 200, 120))
        bullets.draw(f, t)
        r5.draw_player(f, t)
        frames.append(f)
    segs = [halo_seg.frame(k) for k in range(0, 32, 2)]
    turs = [halo_tur.frame(k) for k in range(32)]
    still = Image.new("RGBA", (HALO_R * 2 + 80, HALO_R * 2 + 80), (22, 24, 34, 255))
    halo_draw(still, still.width / 2, still.height / 2, 0.0, (still.width / 2, still.height * 2))
    panels = [("SOURCE: CORE, 260 PX", source_panel(lambda: m6.halo_core())),
              ("ASSEMBLED (1X): CORE + 6 RING SEGMENTS + TURRETS, SHIELD", still),
              ("RING SEGMENT: 16 OF 32 HEADINGS (1X)", grid(segs, 8, 1)),
              ("TURRET: 32 HEADINGS (1X), AIMS INDEPENDENTLY", grid(turs, 16, 1))]
    notes = ["HALO PLATFORM - ASCENDANCY ROTATING TURRET RING (BLACK & GOLD, WHITE ACCENT, RIM LIGHT)",
             "ABOUT 300 PX ACROSS: A SHIELDED OCTAGONAL CORE (104 PX) WITH A RING OF SIX 110 PX",
             "SEGMENTS (32 HEADINGS) ROTATING AROUND IT, EACH CARRYING A 36 PX TURRET THAT AIMS AT THE",
             "PLAYER (32 HEADINGS) AND FIRES GOLD SHELLS AS IT SWINGS ROUND. EVERY DESTROYED TURRET",
             "SPEEDS THE RING UP; WITH HALF THE RING GONE THE RED HEX SHIELD DROPS AND THE CORE IS",
             "EXPOSED. FORTRESS CENTREPIECE (L41)."]
    return r5.build_sheet("HALO PLATFORM - ROTATING TURRET RING", panels, notes, frames[150]), frames


# --------------------------------------------------------------------------- 10 Dust Devil

DD_N = 72
dd = {c: rig.ModelSpaceAngleSprites(lambda c=c: m6.dust_devil(0.0, c), DD_N, 40, factor=4,
                                    colors=32, sym=5) for c in (0.0, 1.0)}


def dust_swirl(img, x, y, t, r=58, seed=0):
    """Translucent spiral of grit around the funnel (procedural, rotates with the spin)."""
    lay = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(lay)
    rng = np.random.default_rng(seed)
    for k in range(140):
        u = rng.uniform(0.3, 1.0)
        arm = k % 3
        a = -t * 5.0 + arm * TAU / 3 + u * 4.0 + rng.uniform(-0.25, 0.25)
        px, py = x + np.cos(a) * r * u, y + np.sin(a) * r * u * 0.9
        s = 1.5 + 3.5 * u
        d.ellipse([px - s, py - s, px + s, py + s], fill=(255, 205, 140, int(150 * (1.15 - u))))
    img.alpha_composite(lay.filter(ImageFilter.GaussianBlur(1.2)))


def dust_devil():
    bg = e3.regolith(FW, FH, 171, ramp_name="MARS", craters=8)
    devils = [rig.catmull_rom([(-60, 140), (100, 110), (180, 220), (120, 330), (230, 380),
                               (330, 300), (300, 170), (420, 120), (540, 200)],
                              [0, 1.2, 2.4, 3.6, 4.8, 6.0, 7.2, 8.4, 9.6]),
              rig.catmull_rom([(540, 40), (400, 80), (330, 200), (420, 300), (330, 420),
                               (200, 450), (120, 360), (-60, 300)],
                              [0, 1.4, 2.8, 4.2, 5.6, 7.0, 8.4, 9.6])]
    rng = np.random.default_rng(9)
    chips = [dict(p=np.array((rng.uniform(40, 440), rng.uniform(60, 420)))) for _ in range(6)]
    bullets = Bullets()
    for i, path in enumerate(devils):
        for t0 in np.arange(1.0 + i * 0.5, 9.0, 2.2):
            x, y = path(t0)
            for k in range(6):                       # spiral of grit
                a = t0 * 5 + k * TAU / 6
                bullets.fire(t0 + k * 0.05, x, y, x + np.cos(a) * 100, y + np.sin(a) * 100,
                             120, "acid")
    dt = 1 / FPS
    frames = []
    for t in times(9.6):
        f = bg.copy()
        d = ImageDraw.Draw(f)
        pos = [path(t) for path in devils]
        for c in chips:                               # loose pickups pulled towards the funnels
            for x, y in pos:
                v = np.array((x, y)) - c["p"]
                dist = np.linalg.norm(v)
                if dist < 150:
                    c["p"] = c["p"] + v / (dist + 1e-6) * 60 * dt
            cx, cy = c["p"]                           # credit chips: pulsing, light outline
            g = int(200 + 55 * np.sin(t * 8))
            d.polygon([(cx, cy - 6), (cx + 5, cy), (cx, cy + 6), (cx - 5, cy)],
                      fill=(g, g, g, 255), outline=(255, 255, 255, 255))
        for i, (x, y) in enumerate(pos):
            dust_swirl(f, x, y, t, r=70, seed=i)
            cyc = (t * 0.7 + i * 0.4) % 1.0
            core = 1.0 if cyc > 0.78 else 0.0                # exposed at the top of each cycle
            spin = 9.0 * t + i
            paste(f, dd[core].get(spin), x, y, "ground")
            if core:
                raster.add_light(f, x, y, 22, (168, 255, 42), 0.8)
        bullets.draw(f, t)
        r5.draw_player(f, t)
        frames.append(f)
    spin = [dd[0.0].frame(k) for k in range(8)]
    core = [dd[0.0].frame(0), dd[1.0].frame(0)]
    panels = [("SOURCE, 260 PX (CORE EXPOSED)", source_panel(lambda: m6.dust_devil(0.3, 1.0))),
              ("SPIN: 8 FRAMES COVER 72 DEG (5-FOLD SYMMETRY), 1X", grid(spin, 8, 1)),
              ("CORE: HIDDEN / EXPOSED (2X)", grid(core, 2, 2))]
    notes = ["DUST DEVIL - VRELL VORTEX ORGANISM (TEAL-BLACK VANES, LIME GLOW: GRIT, AREA)",
             "72 PX, RADIAL (5-FOLD): CURVED MEMBRANE VANES SPIRAL AROUND A FLESHY CORE; IT SPINS",
             "FAST INSIDE A SWIRL OF MARTIAN GRIT AND WANDERS THE PLAINS IN A SWIRL PATH, PULLING LOOSE",
             "PICKUPS (AND SLIGHTLY THE PLAYER) TOWARDS IT AND SPITTING SPIRALS OF GRIT. THE VANES PART",
             "AT THE TOP OF EACH SPIN CYCLE: ONLY THEN IS THE LIME CORE EXPOSED AND HITTABLE."]
    return r5.build_sheet("DUST DEVIL - SPINNING VORTEX ORGANISM", panels, notes, frames[66]), frames


# --------------------------------------------------------------------------- 11 Spiral Nautilus

NAU_N = 56
nau_shell = rig.ModelSpaceAngleSprites(lambda: m6.nautilus_shell(), NAU_N, 24, factor=5,
                                       colors=32)
nau_crown = rig.ModelSpaceAngleSprites(lambda phase=0.0: m6.nautilus_crown(1.0, phase), NAU_N,
                                       16, factor=5, colors=28)
UNCOIL = ((3.6, 4.8), (7.2, 8.4))


def light_level(x, y, px, py):
    """Under the ice: the player's headlight cone ahead (up the screen) and a small halo."""
    dx, dy = x - px, y - py
    halo = np.exp(-(dx * dx + dy * dy) / (2 * 90 ** 2))
    cone = np.exp(-(dx / (40 + 0.55 * max(0, -dy))) ** 2) * (dy < 0) * np.exp(dy / 420)
    return float(np.clip(0.28 + 0.9 * max(halo, cone), 0, 1.1))


def nautilus():
    bg0 = europa_bg(181)
    bullets = Bullets()
    pairs = [1, -1]                                   # orbit directions
    for t0, t1 in UNCOIL:
        for j, sgn in enumerate(pairs):
            for k in range(10):                        # spiral of shots while uncoiled
                tt = t0 + 0.2 + k * 0.09
                x, y = naut_pos(tt, sgn)
                a = sgn * tt * 4 + k * 0.6
                bullets.fire(tt, x, y, x + np.cos(a) * 100, y + np.sin(a) * 100, 120, "orb")
    frames = []
    dist = [0.0, 0.0]
    prev = [None, None]
    for t in times(9.0):
        px, py = r5.player_at(t)
        f = bg0.copy()
        # darkness: light map around the player (cone ahead)
        yy, xx = np.mgrid[0:FH, 0:FW]
        dx, dy = xx - px, yy - py
        halo = np.exp(-(dx * dx + dy * dy) / (2 * 90 ** 2))
        cone = np.exp(-(dx / (40 + 0.55 * np.maximum(0, -dy))) ** 2) * (dy < 0) * np.exp(dy / 420)
        lm = np.clip(0.35 + 1.2 * np.maximum(halo, cone), 0, 1.3)
        a = np.array(f).astype(np.float64)
        a[..., :3] *= lm[..., None]
        f = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")
        for j, sgn in enumerate(pairs):
            x, y = naut_pos(t, sgn)
            if prev[j] is not None:
                dist[j] += np.hypot(x - prev[j][0], y - prev[j][1])
            prev[j] = (x, y)
            roll = -sgn * dist[j] / (NAU_N * 0.4)       # rolling in its shell
            k = light_level(x, y, px, py)
            unc = any(t0 <= t < t1 for t0, t1 in UNCOIL)
            aim = np.arctan2(py - y, px - x)
            shell_h = aim if unc else roll                    # uncoiled: aperture faces the player
            paste(f, dim(nau_shell.get(shell_h), k), x, y)
            if unc:
                cx_, cy_ = x + np.cos(aim) * NAU_N * 0.42, y + np.sin(aim) * NAU_N * 0.42
                paste(f, dim(nau_crown.get(aim, phase=round(t * 6) % 6), min(1.0, k + 0.2)),
                      cx_, cy_)
            raster.add_light(f, x, y, 16, VIOLET, 0.35 if not unc else 0.8)   # bioluminescence
        bullets.draw(f, t)
        r5.draw_player(f, t, shadow=None)
        frames.append(f)
    spin = [nau_shell.frame(k) for k in range(0, 24, 3)]
    crowns = [nau_crown.frame(k, phase=0.0) for k in range(16)]
    unc = Image.new("RGBA", (NAU_N * 2 + 30, NAU_N + 30), (22, 24, 34, 255))
    unc = Image.new("RGBA", (NAU_N * 2 + 30, NAU_N + 60), (22, 24, 34, 255))
    paste(unc, nau_shell.get(0.0), NAU_N / 2 + 10, unc.height / 2 - 14)
    paste(unc, nau_shell.get(np.pi / 2), NAU_N * 1.5 + 20, unc.height / 2 - 14)
    paste(unc, nau_crown.get(np.pi / 2), NAU_N * 1.5 + 20, unc.height / 2 - 14 + NAU_N * 0.42)
    panels = [("SOURCE: SHELL, 260 PX", source_panel(lambda: m6.nautilus_shell())),
              ("SHELL ROLL: 8 OF 24 FRAMES (1X)", grid(spin, 8, 1)),
              ("TENTACLE CROWN: 16 HEADINGS (1X)", grid(crowns, 8, 1)),
              ("COILED / UNCOILED (1X)", unc)]
    notes = ["SPIRAL NAUTILUS - VRELL SHELL ROLLER (BONE SHELL, VIOLET GLOW: GUNNER, SPIRALS)",
             "56 PX. A COILED, TIGER-STRIPED SHELL THAT ROLLS (24 SPIN FRAMES, STRIPES IN MODEL SPACE)",
             "ALONG SPIRAL-IN PATHS AROUND THE PLAYER, PAIRS ORBITING IN OPPOSITE DIRECTIONS. IT",
             "UNCOILS - A TENTACLE CROWN (16 HEADINGS) FACING THE PLAYER - AND FIRES A SPIRAL OF",
             "SHOTS; THE SHELL IS ARMOURED, SO SHOOT IT WHILE UNCOILED. UNDER EUROPA'S ICE (L25) IT",
             "IS LIT ONLY BY THE HEADLIGHT CONE AND ITS OWN BIOLUMINESCENCE."]
    return r5.build_sheet("SPIRAL NAUTILUS - ROLLS, UNCOILS, FIRES A SPIRAL", panels, notes,
                          frames[85]), frames


def naut_pos(t, sgn):
    px, py = r5.player_at(t)
    r = max(95 if sgn > 0 else 150, 230 - 16 * t)       # pair keeps apart on the inner orbit
    a = sgn * (0.9 * t) + (0 if sgn > 0 else np.pi)
    cx, cy = 240, 300
    return cx + np.cos(a) * r, cy + np.sin(a) * r * 0.8


# --------------------------------------------------------------------------- lineup

def lineup():
    W = 1300
    still_halo = Image.new("RGBA", (HALO_R * 2 + 70, HALO_R * 2 + 70), (0, 0, 0, 0))
    halo_draw(still_halo, still_halo.width / 2, still_halo.height / 2, 0.0,
              (still_halo.width / 2, still_halo.height * 2))
    combo = Image.new("RGBA", (RAFT_N, RAFT_N), (0, 0, 0, 0))
    paste(combo, raft.get(0.0), RAFT_N / 2, RAFT_N / 2)
    paste(combo, gun.get(np.pi / 2), RAFT_N / 2, RAFT_N / 2 - 2)
    new = [("LAMPREY", lmp.get(np.pi / 2, phase=0.0)), ("DRIFTJELLY", jly.get(0.0, pulse=0.3)),
           ("SKIMMER", skm.get(np.pi / 2, phase=0.0)), ("CREEPER", crp.get(np.pi / 2, phase=0.0)),
           ("WRAITH", wr.get(np.pi / 2, phase=0.0)), ("DUST DEVIL", dd[0.0].frame(0)),
           ("HIVE NODE", hive.get(0.0, iris=0.0, pulse=0.5)), ("NAUTILUS", nau_shell.get(0.0)),
           ("REEF SPITTER", combo)]
    ref = [("AF-12 PLAYER", e3.player_sprite()), ("SKITTER (R04)", skitter(0.0)),
           ("SCUTTLER (R05)", r5.scut.get(np.pi / 2, phase=0.0)),
           ("RAVAGER (R05)", r5b.rav.get(np.pi / 2, phase=0.8, pose="run"))]
    row = sorted(new + ref, key=lambda v: v[1].width * v[1].height)
    img = raster.sheet(W, 860, "ENEMY LINEUP - ROUND 06 UNITS WITH THE PLAYER AND R04/R05 UNITS, 1X",
                       r5.SUB)
    raster.draw_text(img, 16, 44, "NEW UNITS (WHITE) AND REFERENCES (DIM), SORTED BY AREA (1X)",
                     raster.LABEL_DIM)
    strip = Image.new("RGBA", (W - 32, 130), (22, 24, 34, 255))
    x = 14
    labels = []
    for name, sp in row:
        paste(strip, sp, x + sp.width / 2, 65)
        labels.append((x + sp.width / 2, name))
        x += max(sp.width, 64) + 16
    img.alpha_composite(strip, (16, 56))
    for i, (cx, name) in enumerate(labels):
        col = raster.LABEL_DIM if "(" in name or "PLAYER" in name else raster.LABEL
        raster.draw_text(img, int(16 + cx - raster.text_width(name[:14]) / 2),
                         192 + (i % 2) * 10, name[:14], col)
    raster.draw_text(img, 16, 228, "LARGE (1X): THREADCRAWLER (STRETCHED), HALO PLATFORM, PLAYER",
                     raster.LABEL_DIM)
    big = Image.new("RGBA", (W - 32, 600), (22, 24, 34, 255))
    thread_draw(big, 6.0, path=lambda tt: (40 + 80 * tt, 150 + 40 * np.sin(tt * 1.3)))
    paste(big, still_halo, 960, 300)
    paste(big, e3.player_sprite(), 620, 450)
    raster.draw_text(big, 600, 480, "PLAYER", raster.LABEL)
    raster.draw_text(big, 40, 240, "THREADCRAWLER (HEAD RIGHT)", raster.LABEL)
    raster.draw_text(big, 880, 560, "HALO PLATFORM", raster.LABEL)
    img.alpha_composite(big, (16, 240))
    return img


# --------------------------------------------------------------------------- main

UNITS = {
    "creeper": ("ground", creeper), "hive-node": ("ground", hive_node),
    "wraith": ("air", wraith), "lamprey": ("air", lamprey),
    "driftjelly": ("naval", driftjelly), "reef-spitter": ("naval", reef_spitter),
    "skimmer": ("naval", skimmer), "threadcrawler": ("ground", threadcrawler),
    "halo-platform": ("ground", halo_platform), "dust-devil": ("ground", dust_devil),
    "spiral-nautilus": ("naval", nautilus),
}


def main(args):
    for name, (cat, fn) in UNITS.items():
        if args and name not in args:
            continue
        sheet, frames = fn()
        save_unit(cat, name, sheet, frames)
    if not args or "lineup" in args:
        p = r5.out(None, "lineup-r06-a.png")
        lineup().convert("RGB").save(p, optimize=True)
        print("wrote", p.relative_to(ROOT))


if __name__ == "__main__":
    main(sys.argv[1:])
