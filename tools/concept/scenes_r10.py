#!/usr/bin/env python3
"""Concept round 10 - Geneva rebuilt as a city, the storm and the ocean within the motion budget.

User feedback on round 09: Geneva "is not very recognizable as a city"; the storm is "heavy on
the eyes - too much is moving at the same time", the ocean too, less badly. This script
subclasses the round-09 scenes (scenes_r09.py, which subclasses scenes_r08.py) and only adds.

Outputs (design/art-direction/concept/):
  scene-geneva-r10-a.png / .gif   L14 Geneva Concord under the Vrell canopy, seen from above as a
                                  city: avenues, side streets, a diagonal and squares as
                                  continuous gaps with sidewalks, zebra crossings, sodium lamps,
                                  parked and moving cars; perimeter blocks of row houses round
                                  courtyards (ridged, two-tone roofs lit from the top-left,
                                  chimneys, skylights, varied heights with cast shadows); the
                                  lake with the Jet d'Eau, the Rhone with the Pont du Mont-Blanc
                                  and the Ile Rousseau, the cathedral, the Concord rotunda on its
                                  roundabout, parks and tree rows; the cathedral towers, the
                                  Jet d'Eau plume and two modern towers lean away from the
                                  screen centre (perspective rule); the Spire's roots, creep and
                                  canopy shadow lie over the city
  scene-storm-r10-a.png / .gif    L12 Storm Front within the motion budget: slower, lower sea
                                  (waves and rain are the two strong elements), faint
                                  motion-blurred rain, scud that only sways, fainter crests and
                                  wind streaks, one short lightning flash; 140 px/s
  scene-ocean-r10-a.png / .gif    L11 Atlantic Convoy within the motion budget: slower, softer
                                  swell (waves and wakes are the two strong elements), fainter
                                  whitecaps and collars, slower smoke, fainter wisps

The storm and ocean GIFs are 4 s seamless loops at 25 fps (100 frames, exact 4 cs frame delays): the scenes are
still timed in the 80-frame units of scenes_r06 (every loop phase stays an integer), and each
GIF frame samples the scene at f = i * 80 / 100, so every speed in px/s is unchanged and only
the motion gets smoother. Geneva's GIF stays at 20 fps (80 frames, as in round 09): the detailed
city at 25 fps would not fit the ~8 MB GIF budget. Bullet colours stay reserved in the palette.
Rules: design/art-direction/README.md (layer model, Density, Decoration, Motion budget,
atmosphere intensity, Water rules, perspective rule for tall structures).
Run: python3 tools/concept/scenes_r10.py [geneva] [storm] [ocean] [--sheet]
"""
import subprocess
import sys
import tempfile
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, str(Path(__file__).resolve().parent))
import enemies_r06 as e6  # noqa: E402
import parallax_r02 as r02  # noqa: E402
import parallax_r03 as r03  # noqa: E402
import scenes_r06 as s6  # noqa: E402
import scenes_r08 as s8  # noqa: E402
import scenes_r09 as s9  # noqa: E402
from parallax_r02 import H, W, scroll_tex  # noqa: E402
from render import raster, sprite  # noqa: E402
from render.config import ROOT  # noqa: E402

OUT = s9.OUT
N, FPS, TAU = s9.N, s9.FPS, s9.TAU          # scene time base: 80 units per 4 s loop
XX, YY = s9.XX, s9.YY
th = s9.th
GIF_N, GIF_FPS = 100, 25                     # GIF: 100 frames at 25 fps = the same 4 s loop


# =========================================================================== smooth GIF

def make_gif10(scene, path, colors, max_mb=8.0, n=GIF_N, fps=GIF_FPS, min_colors=64):
    """Seamless 4 s loop sampled at ``fps``: GIF frame i shows scene time f = i * N / n, so
    frame n would be frame 0 again. Palette as scenes_r06.make_gif (bullet colours reserved)."""
    with tempfile.TemporaryDirectory() as tmp:
        frames = []
        for i in range(n):
            img = scene.compose(i * N / n)
            img.convert("RGB").save(Path(tmp) / f"f{i:03d}.png")
            if i % 10 == 0:
                frames.append(img)
        while True:
            s6.gif_palette(frames, colors, s6.key_sprites(scene)).save(Path(tmp) / "palette.png")
            cmd = ["ffmpeg", "-v", "error", "-y", "-framerate", str(fps),
                   "-i", str(Path(tmp) / "f%03d.png"), "-i", str(Path(tmp) / "palette.png"),
                   "-lavfi", "[0][1]paletteuse=dither=none:diff_mode=rectangle",
                   "-loop", "0", str(path)]
            subprocess.run(cmd, check=True)
            if path.stat().st_size <= max_mb * 1e6 or colors <= min_colors:
                return colors
            colors -= 8


def skitter_snake(img, casters, cast, f, x0, amp, delay, count=5, gap=0.045, span=760):
    """scenes_r06.skitter_snake for fractional scene time (GIF frames between scene units)."""
    for i in range(count):
        t = ((f / N) + delay - i * gap) % 1.0
        x = x0 + amp * np.sin(TAU * 1.5 * t)
        y = -70 + span * t
        if -40 < y < H + 40:
            sp = cast.unit("skitter-a", anim=[-1.0, 0.0, 1.0, 0.0][int(f // 2 + i) % 4])
            casters.append((sp, x, y))
            sprite.paste_center(img, sp, x, y)


# =========================================================================== B: storm (L12)

class StormScene(s9.StormScene):
    """Round 09 storm within the motion budget. Strong motion is kept for two elements only,
    the heaving sea and the rain; the rest is slow: the scud only sways (no longer races a
    screen width sideways per loop), the swell phases move half as fast, crests, wind streaks
    and rain are fainter, and the flash is a single short strike. Scroll 160 -> 140 px/s."""

    notes = dict(s9.StormScene.notes, **{
        "ground": "X1.0 SLOWER SEA, PLATFORM, BOAT",
        "low-air": "X1.5 DARK SCUD (24 %), SWAYS",
        "high-air": "X2.2 FAINT BLURRED RAIN, 1 FLASH"})
    TRAVEL = 560                 # 140 px/s (was 160)
    FLASH_AT = 52.0              # scene time of the single strike

    def __init__(self):
        s8.Scene8.__init__(self)                 # rebuilt here: the period changed (560)
        per = self.TRAVEL
        self.sea = s8.Sea(per, [(220, 1.25, 58, 1), (128, 0.9, 40, 2), (80, 0.6, 76, 2),
                                (48, 0.34, 24, 3), (30, 0.18, 64, 4)], 1201, s9.STORM_STOPS,
                          tones=7, light_gain=2.0, warp_gain=2.4, warp_cell=112)
        self.boat = s8.Vessel(s8.model("patrol"), 360, 380, (5, 8), 1.0, 1210, wake=1.2)
        self.capn = r02.periodic_fbm(W, per, 16, 1251, octaves=3).astype(np.float32)
        wind = r03.stretched(W, per, [56, 28, 14], 1252, 8).astype(np.float32)
        self.windline = np.clip(1 - np.abs(wind - 0.5) / 0.05, 0, 1)
        self.windgate = s9.tone_field(per, 1253, 112)
        self.tone = s9.tone_field(per, 1254, 140)
        self.brk = s9.tone_field(per, 1255, 28)
        self.churn = s9.vstreak(per, 28, 1256, k=3)
        lp = self.s.period("low-air")            # 840: the cells divide W and lp
        n = r03.noise(W, lp, [120, 60, 30, 15], 1221)
        tones = [(20, 24, 30), (32, 37, 44), (46, 51, 58), (60, 66, 72)]
        self.scud = r03.step_alpha(r02.posterize(r03.bank(n, 0.55, 0.12, tones, max_alpha=165,
                                                          light=5.0), 8), 4)
        rng = np.random.default_rng(1230)
        # rain in a 960 px tall field: fast drops fall 2 fields per loop with a 1:4 slant (one
        # screen width sideways), light drops 1 field per loop with a 1:2 slant - both integer
        # wraps, so the rain loops; each streak is as long as one GIF frame step (motion blur)
        self.rain = ([(rng.uniform(0, W), rng.uniform(0, 960), 2) for _ in range(60)] +
                     [(rng.uniform(0, W), rng.uniform(0, 960), 1) for _ in range(80)])

    def flash(self, f):
        a = (f - self.FLASH_AT) % N
        if a > N - 0.8:                          # quick rise
            return 0.26 * (1 - (N - a) / 0.8)
        return 0.26 * (1 - a / 4.0) ** 2 if a < 4.0 else 0.0

    def rain_layer(self, f):
        img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        hr = 960
        step = N / GIF_N                         # one GIF frame in scene time
        for x0, y0, cyc in self.rain:
            slant = 0.25 if cyc == 2 else 0.5
            vy = cyc * hr / N                    # px per scene unit
            vx = slant * vy
            y = (y0 + vy * f) % hr - 40
            x = (x0 + vx * f) % W
            if not -40 < y < H + 40:
                continue
            ln = max(6.0, vy * step)             # streak = the frame step: continuous motion
            a = 66 if cyc == 2 else 44
            dx, dy = vx / vy, 1.0
            for k, frac in enumerate((0.0, 0.5)):    # brighter head, fading tail
                d.line([x - dx * ln * frac, y - dy * ln * frac,
                        x - dx * ln * (frac + 0.5), y - dy * ln * (frac + 0.5)],
                       fill=(150, 166, 178, a if k == 0 else a // 2), width=1)
        fl = self.flash(f)
        if fl > 0:
            img.alpha_composite(Image.new("RGBA", (W, H), (205, 220, 255, int(200 * fl))))
        return img

    def layer_images(self, f):
        off = self.s.off("ground", f)
        rows = self.sea.rows(off)
        h = self.sea.height(f, off)
        bx, by = self.boat.pos(f)
        s9.broken_kelvin(h, bx, by - self.boat.half + 10, rows, self.brk, amp=0.75, decay=200)
        foam = s8.Foam()
        top = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        legs = []
        hs = 56
        for py in self.ys("ground", self.PLATFORM[1], f, margin=120):
            px = self.PLATFORM[0]
            for lx, ly in ((-hs, -hs), (hs, -hs), (-hs, hs), (hs, hs)):
                legs.append((px + lx, py + ly))
            plat = s8.model("platform")
            sprite.paste_center(top, sprite.shadow_of(plat, opacity=0.45, blur=2.5), px + 9, py + 12)
            sprite.paste_center(top, plat, px, py)
            if (f // 10) % 2 == 0:               # slower beacon blink (period 20 divides N)
                for lx, ly in ((-hs, -hs), (hs, -hs), (-hs, hs), (hs, hs)):
                    top = raster.add_light(top, px + lx, py + ly, 6, (255, 60, 40), 0.7)
        flow = (rows - int(round(self.TRAVEL * f / N))) % self.TRAVEL   # 1 period per loop
        wash = np.zeros((H, W), np.float32)
        for k, (lx, ly) in enumerate(legs):
            wash = np.maximum(wash, s9.wash_mask(lx, ly - 10, flow, self.churn, width0=8,
                                                 spread=0.16, length=90, soft=0.1))
            age = ((f / N * 2 + k * 0.27) % 1.0) * 1.2
            s8.ripple_train(h, lx, ly, age, 1.2, amp=0.45, lam=11, speed=30, seed=k)
            ang = np.linspace(0, TAU, 26, endpoint=False)
            pts = np.stack([np.cos(ang) * 10, np.sin(ang) * 9], 1)
            s8.collar(foam, f, pts, lx, ly, 70 + k, alpha=0.8, r=1.5, push=2.5)
            s8.stream(foam, f, lx, ly - 8, 0.3, -0.95, 22, 6, 12, 1, 5, 1.0, 2.0, 0.7, 90 + k,
                      wave=1.0)
        wash = np.maximum(wash, s9.wash_mask(bx, by + self.boat.half - 6, rows, self.churn,
                                             width0=self.boat.sp.width * 0.25, length=280))
        hw = self.boat.sp.width / 2 - 4
        bow = by - self.boat.half + 8
        for side in (-1, 1):
            s8.stream(foam, f, bx + side * 3, bow + 10, side * 0.4, 0.92, self.TRAVEL / 5, 4, 50,
                      1.0, 6.0, 0.8, 1.7, 0.75, 1210 + side, wave=0.8)
            s8.stream(foam, f, bx + side * hw, by, side * 0.1, 1.0, self.TRAVEL / 6, 4, 18,
                      0.4, 2.0, 0.6, 1.1, 0.45, 1215 + side)
            s8.stream(foam, f, bx + side * 4, bow, side * 1.0, 0.35, 34, 5, 16, 2, 7, 1.0, 2.0,
                      0.7, 1240 + side, wave=2)
        s8.collar(foam, f, self.boat.pts, bx, by, 1213, alpha=0.7, r=1.2)
        light = self.sea.light(h)
        body = self.sea.colour(light)
        tone = self.tone[rows]
        body = body * (0.84 + 0.30 * tone[..., None])
        caps = np.clip((h - 1.45) * 1.4, 0, 1) * np.clip((self.capn[rows] - 0.56) * 8, 0, 1)
        streak = self.windline[rows] * np.clip((self.windgate[rows] - 0.45) * 4, 0, 1)
        body = s9.apply_foam(body, streak * 0.3, (112, 128, 136), levels=3)
        body = s9.apply_foam(body, caps * 0.6, (168, 180, 186), levels=3)
        body = s9.apply_foam(body, wash * 0.8, (176, 190, 196), levels=4)
        ground = raster.to_rgba_image(np.clip(body, 0, 255))
        ground.alpha_composite(foam.done())
        ground.alpha_composite(top)
        self.boat.draw(ground, f)
        fl = self.flash(f)
        if fl > 0:                               # the strike lights the sea
            g = np.asarray(ground).astype(np.float32)
            g[..., :3] += np.array((150, 165, 190)) * fl
            ground = Image.fromarray(np.clip(g, 0, 255).astype(np.uint8), "RGBA")
        air = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        casters = []
        self.lampreys(air, casters, f)
        cx = -60 + 600 * ((f / N + 0.15) % 1.0)
        s6.seed_cluster(air, casters, f, cx, 150 + 50 * np.sin(th(f)))
        cx2 = -60 + 600 * ((f / N + 0.65) % 1.0)
        s6.seed_cluster(air, casters, f, cx2, 330 + 40 * np.sin(th(f) + 2), ph=1.3)
        nd = self.cast.unit("needler-a", anim=[-1.0, 0.0, 1.0, 0.0][int(f // 4) % 4])
        npos = lambda ff: (380 + 50 * np.sin(th(ff) + 0.5), 110 + 18 * np.sin(th(ff, 2)))
        casters.append((nd,) + npos(f))
        sprite.paste_center(air, nd, *npos(f))
        s6.aimed(air, f, lambda ff: (npos(ff)[0], npos(ff)[1] + 14), self.cast, period=40,
                 speed=6.5, kind="thorn", phase=6)
        self.cast.player(air, casters, f)
        shadows = r02.shadow_layer(casters, *s6.SHADOW["air"], opacity=0.36)
        low = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        a = (f - self.FLASH_AT) % N
        if a < 1.6:                              # the bolt shows for ~0.08 s
            s6.lightning(low, 300, -20, 330, 1250)
        scud = scroll_tex(self.scud, self.s.off("low-air", f))
        sway = int(round(16 * np.sin(th(f))))    # at most ~1 px per GIF frame
        scud = np.roll(np.asarray(scud), sway, axis=1)
        if fl > 0:
            scud = scud.astype(np.float32)
            scud[..., :3] += 120 * fl
            scud = np.clip(scud, 0, 255).astype(np.uint8)
        L = {"ground": ground, "shadows": shadows, "low-air": low, "air": air}
        L = r03.decorate(L, Image.fromarray(scud, "RGBA"), "ground", casters,
                         bank_shadow_opacity=0.24, flyer_opacity=0.3)
        L["high-air"] = self.rain_layer(f)
        return L


# =========================================================================== A: ocean (L11)

def smoke10(img, f, x, y, travel, seed=5):
    """The burning ship's smoke at half the round-09 particle speed (motion budget)."""
    fm = s8.Foam(color=(70, 70, 72))
    s8.stream(fm, f, x - 4, y + 6, 0.55, 0.84, 240, 1, 60, 3.0, 24.0, 3.0, 12.0, 0.7, seed,
              wave=3.0)
    s8.stream(fm, f, x - 4, y + 6, 0.55, 0.84, 120, 2, 20, 2.0, 7.0, 2.0, 6.0, 0.75, seed + 1,
              color=(40, 38, 38))
    img.alpha_composite(fm.done(levels=6))


class OceanScene(s9.OceanScene):
    """Round 09 ocean within the motion budget: the swell keeps its depth and colour field but
    its phases move about half as fast and its fine components are lower; whitecaps, collars
    and wisps are fainter, the smoke slower. Strong motion: the sea and the wakes."""

    notes = dict(s9.OceanScene.notes, **{"ground": "X1.0 CALMER SEA, CONVOY, WAKES",
                                          "high-air": "X2.0 THIN WISPS (<16 % ALPHA)"})

    def __init__(self):
        super().__init__()
        per = self.TRAVEL
        self.sea = s8.Sea(per, [(160, 1.0, 72, 1), (96, 0.7, 52, 2), (60, 0.45, 104, 2),
                                (40, 0.28, 34, 3), (26, 0.16, 84, 3)], 1101, s9.OCEAN_STOPS,
                          tones=7, light_gain=1.0)
        hp = self.s.period("high-air")
        self.wisps = s6.wisp_tex(hp, [192, 96, 48], 1142, (206, 214, 220), thresh=0.54,
                                 gain=2.6, max_a=40, stretch=3)

    def layer_images(self, f):
        # round 09's ocean with calmer caps, collars, ripples and smoke (same composition)
        off = self.s.off("ground", f)
        rows = self.sea.rows(off)
        h = self.sea.height(f, off)
        for sh in self.ships:
            x, y = sh.pos(f)
            s9.broken_kelvin(h, x, y - sh.half + 10, rows, self.brk, amp=0.55 * sh.wake)
        sub = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        top = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        foam = s8.Foam()
        kx = 250 + 30 * np.sin(th(f))
        for ky in self.ys("ground", 380, f, margin=220):
            s9.deep_shape(sub, kx, ky + 30 * np.sin(th(f) + 1), f)
        for j in self.JELLIES:
            x, y0, surf, ph = self.jelly_world(j, f)
            pulse = 0.5 + 0.5 * np.sin(th(f, 2) + ph)
            for y in self.ys("ground", y0, f, margin=40):
                if surf:
                    s8.jelly_surface(top, foam, sub, f, x, y, pulse, int(ph * 10), s8.OCEAN_WATER)
                    for k in range(2):
                        age = ((2 * f / N + ph / TAU + k * 0.5) % 1.0) * 2.0
                        s8.ripple_train(h, x, y, age, 1.6, amp=0.3, lam=8, seed=ph + k)
                else:
                    sp = e6.jly.get(0.0, pulse=round(pulse, 1))
                    sprite.paste_center(sub, s8.deep_tint(sp, s8.OCEAN_WATER,
                                                          self.jelly_sub_depth(), blur=0.8), x, y)
        for i, (rx, ry) in enumerate(self.RAFTS):
            for y in self.ys("ground", ry, f, margin=60):
                bob = 1.0 * np.sin(th(f, 2) + i)
                raft = e6.raft.get(0.0)
                sprite.paste_center(sub, s8.deep_tint(raft, s8.OCEAN_WATER, 0.3, blur=0.6,
                                                      alpha=0.8), rx + 2, y + 3)
                sprite.paste_center(top, raft, rx, y + bob)
                px, py, _ = self.cast.player_pos(f)
                sprite.paste_center(top, e6.gun.get(np.arctan2(py - y, px - rx)), rx, y + bob - 2)
                ang = np.linspace(0, TAU, 40, endpoint=False)
                pts = np.stack([np.cos(ang) * 40, np.sin(ang) * 36], 1)
                s8.collar(foam, f, pts, rx, y + bob, 40 + i, alpha=0.5, r=1.2)
                s8.ripple_train(h, rx, y, ((f / N * 2 + i * 0.5) % 1.0) * 1.6, 1.6, amp=0.28,
                                lam=9, seed=3 + i)
        wash = np.zeros((H, W), np.float32)
        for sh in self.ships:
            x, y = sh.pos(f)
            wash = np.maximum(wash, s9.wash_mask(x, y + sh.half - 8, rows, self.churn,
                                                 width0=sh.sp.width * 0.2, length=330 * sh.wake))
            self.ship_foam(foam, sh, f)
        light = self.sea.light(h)
        body = self.sea.colour(light)
        tone = self.tone[rows]
        body = body * (0.84 + 0.28 * tone[..., None]) + np.array((0, 6, 4)) * (tone[..., None] - 0.5)
        caps = np.clip((h - 1.15) * 1.6, 0, 1) * (self.capn[rows] > 0.64)
        body = s9.apply_foam(body, caps * 0.5, (176, 190, 198), levels=3)
        body = s8.through_surface(body, s8.wobble_layer(sub, f), self.sea.mean(), 0.9)
        body = s9.apply_foam(body, wash * 0.85, (192, 210, 210), levels=4)
        ground = raster.to_rgba_image(np.clip(body, 0, 255))
        ground.alpha_composite(foam.done())
        ground.alpha_composite(top)
        for sh in self.ships:
            sh.draw(ground, f)
        ground = self.fire(ground, f)
        air = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        casters = []
        skitter_snake(air, casters, self.cast, f, 250, 110, 0.35)

        def raft_src(i):
            def src(ff):
                rx, ry = self.RAFTS[i]
                y = (ry + self.s.off("ground", ff)) % self.TRAVEL
                return (rx, y) if 0 <= y <= H else None
            return src
        s8.fan_shots(air, f, raft_src(0), self.cast, period=40, speed=5.5, phase=8)
        s8.fan_shots(air, f, raft_src(1), self.cast, period=40, speed=5.5, phase=28)
        self.cast.player(air, casters, f)
        shadows = r02.shadow_layer(casters, *s6.SHADOW["air"], opacity=0.42)
        low = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        smoke10(low, f, *self.burning().pos(f), self.TRAVEL)
        L = {"ground": ground, "shadows": shadows, "low-air": low, "air": air}
        L = r03.decorate(L, scroll_tex(self.mist, self.s.off("low-air", f)), "ground", casters,
                         bank_shadow_opacity=0.22, flyer_opacity=0.3)
        L["high-air"] = scroll_tex(self.wisps, self.s.off("high-air", f))
        L["sub"] = sub
        return L

    def ship_foam(self, foam, sh, f):
        x, y = sh.pos(f)
        hw = sh.sp.width / 2 - 6
        bow = y - sh.half + 8
        for side in (-1, 1):
            s8.stream(foam, f, x + side * 3, bow + 12, side * 0.36, 0.93, self.TRAVEL / 4, 3, 60,
                      0.8, 5.0, 0.7, 1.5, 0.65 * sh.wake, sh.seed + side, wave=0.5)
            s8.stream(foam, f, x + side * hw, y, side * 0.1, 1.0, self.TRAVEL / 5, 4, 26,
                      0.4, 2.0, 0.6, 1.1, 0.4 * sh.wake, sh.seed + 5 + side, wave=0.3)
        s8.collar(foam, f, sh.pts, x, y, sh.seed + 3, alpha=0.55, r=1.1)


# =========================================================================== C: Geneva (L14)

CAM = 6.0                                   # camera height model of parallax B (r02)
PCX, PCY = W / 2, H * 0.55                  # perspective centre


def project(x, y, h):
    """Screen position of a point at height ``h`` above (x, y): pushed away from the centre."""
    k = CAM / (CAM - h)
    return PCX + (x - PCX) * k, PCY + (y - PCY) * k


def hash01(k, salt=0):
    """Deterministic 0..1 hash of integer keys (vectorised)."""
    with np.errstate(over="ignore"):
        k = np.asarray(k).astype(np.uint64) * np.uint64(0x9E3779B97F4A7C15)
        k = k + np.uint64((salt * 0x632BE5AB + 0x5BD1E995) & 0xFFFFFFFF)
        k ^= k >> np.uint64(29)
        k = k * np.uint64(0xBF58476D1CE4E5B9)
        k ^= k >> np.uint64(32)
    return (k % np.uint64(100003)).astype(np.float64) / 100003


def axis_dist(free, axis):
    """Distance along ``axis`` to the nearest free pixel, and whether it lies ahead (+).
    Wraps in y (axis 0); beyond the left/right screen edge counts as built."""
    if axis == 0:
        per = free.shape[0]
        f3 = np.concatenate([free, free, free], 0)
        d, s = axis_dist_raw(f3, 0)
        return d[per:2 * per], s[per:2 * per]
    return axis_dist_raw(free, 1)


def axis_dist_raw(free, axis):
    n = free.shape[axis]
    idx = np.arange(n).reshape((-1, 1) if axis == 0 else (1, -1))
    idx = np.broadcast_to(idx, free.shape)
    last = np.maximum.accumulate(np.where(free, idx, -10 ** 6), axis=axis)
    nxt = np.flip(np.minimum.accumulate(np.flip(np.where(free, idx, 10 ** 6), axis), axis=axis),
                  axis)
    back, ahead = idx - last, nxt - idx
    return np.minimum(back, ahead).astype(np.float64), ahead < back


def cheb_dist(free, cap=24):
    """Chessboard distance to the nearest free pixel (wraps in y), capped."""
    d = np.full(free.shape, float(cap))
    cur = free.copy()
    d[cur] = 0
    for t in range(1, cap):
        g = cur | np.roll(cur, 1, 0) | np.roll(cur, -1, 0)
        g2 = g.copy()
        g2[:, 1:] |= g[:, :-1]
        g2[:, :-1] |= g[:, 1:]
        new = g2 & ~cur
        d[new] = t
        cur = g2
    return d


def shift_x(a, dx, fill=0.0):
    if dx == 0:
        return a
    out = np.full_like(a, fill)
    out[:, dx:] = a[:, :-dx]
    return out


def cast_shadow(hgt, length=1.0, steps=14):
    """Shadow strength 0..1 cast down-right by a height field (light from the top-left).
    Wraps in y."""
    hs = np.zeros_like(hgt)
    for t in range(1, steps):
        dx, dy = int(round(0.6 * t)), int(round(0.8 * t))
        sh = shift_x(np.roll(hgt, dy, 0), dx)
        hs = np.maximum(hs, sh - t / length)
    return np.clip((hs - hgt) / 1.2, 0, 1)


def convex_hull(pts):
    pts = sorted(set((round(x, 2), round(y, 2)) for x, y in pts))
    if len(pts) < 3:
        return pts

    def cross(o, a, b):
        return (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0])
    lo, hi = [], []
    for p in pts:
        while len(lo) >= 2 and cross(lo[-2], lo[-1], p) <= 0:
            lo.pop()
        lo.append(p)
    for p in reversed(pts):
        while len(hi) >= 2 and cross(hi[-2], hi[-1], p) <= 0:
            hi.pop()
        hi.append(p)
    return lo[:-1] + hi[:-1]


def car_sprite(body, kind="car", scale=4):
    """A small car seen from above (facing up), 4x supersampled; headlight beams included."""
    w, l = (5, 9) if kind == "car" else (6, 12)
    S = scale
    img = Image.new("RGBA", ((w + 6) * S, (l + 16) * S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    ox, oy = 3 * S, 12 * S
    beam = Image.new("RGBA", img.size, (0, 0, 0, 0))
    bd = ImageDraw.Draw(beam)
    for sx in (1, w - 1):
        bd.polygon([(ox + sx * S, oy), (ox + (sx - 2.2) * S, oy - 11 * S),
                    (ox + (sx + 2.2) * S, oy - 11 * S)], fill=(255, 236, 170, 60))
    img.alpha_composite(beam.filter(ImageFilter.GaussianBlur(S)))
    d.rounded_rectangle([ox, oy, ox + w * S - 1, oy + l * S - 1], radius=S, fill=body + (255,))
    roof = tuple(min(255, int(c * 1.18)) for c in body)
    d.rectangle([ox + S, oy + 3 * S, ox + (w - 1) * S - 1, oy + (l - 3) * S], fill=roof + (255,))
    d.rectangle([ox + S, oy + 2 * S, ox + (w - 1) * S - 1, oy + 3 * S - 1], fill=(28, 32, 44, 255))
    for sx in (0, w - 1):
        d.rectangle([ox + sx * S, oy, ox + sx * S + S - 1, oy + S - 1], fill=(255, 240, 200, 255))
        d.rectangle([ox + sx * S, oy + (l - 1) * S, ox + sx * S + S - 1, oy + l * S - 1],
                    fill=(150, 30, 30, 255))
    return img


class GenevaScene(s9.GenevaScene):
    """Round 09 Geneva rebuilt as a European city seen from above (SimCity 2000 / late-90s
    pre-rendered): a street network of continuous gaps, perimeter blocks of row houses round
    courtyards, the landmarks, parks and trees; the Vrell overgrowth lies over it."""

    notes = dict(s9.GenevaScene.notes, **{
        "ground": "X1.0 CITY, LAKE, RHONE, ROOTS"})
    GIF_COLORS = 120
    GIF_RATE = 20                            # the detailed city at 25 fps would not fit 8 MB
    TEX_COLORS = 36                          # posterized city texture (keeps the GIF small)
    TEX_SMOOTH = 0                           # a mode filter would blur ridges and party walls
    PLAZA = (300, 150)
    HIVE = (381, 471)
    FOUNTAIN = (70, 246)
    AVE = (290, 312)                         # main avenue (x range), lanes at 296 and 306
    VSTREETS = [(170, 178), (228, 237), (372, 381), (436, 444)]
    HSTREETS = [(14, 28, 0, W)] + \
        [(y0, y0 + 9, 0, 290) for y0 in (84, 156, 232, 298, 444, 516)] + \
        [(y0, y0 + 9, 312, W) for y0 in (78, 146, 318, 520)]
    CATHEDRAL_SQ = (381, 436, 192, 306)
    HIVE_SQ = (336, 426, 434, 508)
    BASTIONS = (180, 284, 452, 538)
    TOWERS = [  # modern Concord buildings (x0, y0, x1, y1, height, roof)
        (186, 102, 220, 136, 0.55, "pad"), (448, 96, 478, 136, 0.78, "flat")]

    @staticmethod
    def river_c(x):
        return 388 + 5 * np.sin(np.asarray(x) / 60.0)

    @staticmethod
    def river_hw(x):
        return 21 - 4 * np.asarray(x) / W

    # ------------------------------------------------------------------ the city texture
    def _city(self, per):
        rng = np.random.default_rng(1501)
        yy, xx = np.mgrid[0:per, 0:W].astype(np.float64)
        shore = self.shore_x(yy)
        lake = xx < shore
        rc, rhw = self.river_c(xx), self.river_hw(xx)
        rdist = np.abs(yy - rc)
        river = (xx >= shore - 3) & (rdist < rhw)
        STREET, SQUARE, PARK, WATER, QUAY, PROM = 1, 2, 3, 4, 5, 6
        kind = np.zeros((per, W), np.int8)            # 0 = built land
        av0, av1 = self.AVE
        kind[:, av0:av1] = STREET
        # side streets with a few gaps (T-junctions): a segment between two cross streets
        # is left out now and then, which merges blocks into longer ones
        hs_bounds = sorted({0, per} | {y for y0, y1, _, _ in self.HSTREETS for y in (y0, y1)})
        for i, (x0, x1) in enumerate(self.VSTREETS):
            for j in range(len(hs_bounds) - 1):
                if hash01(i * 31 + j, 7) < 0.18:
                    continue
                kind[hs_bounds[j]:hs_bounds[j + 1], x0:x1] = STREET
        vs_bounds = sorted({0, W, av0, av1} | {x for x0, x1 in self.VSTREETS for x in (x0, x1)})
        for i, (y0, y1, xa, xb) in enumerate(self.HSTREETS):
            for j in range(len(vs_bounds) - 1):
                a, b = max(xa, vs_bounds[j]), min(xb, vs_bounds[j + 1])
                if a >= b or (i > 0 and hash01(i * 17 + j, 9) < 0.15):
                    continue
                kind[y0:y1, a:b] = STREET
        # the diagonal (Rue du Mont-Blanc): from the roundabout down to the lake bridge
        p0, p1 = np.array((292.0, 166.0)), np.array((116.0, 364.0))
        v = p1 - p0
        t = np.clip(((xx - p0[0]) * v[0] + (yy - p0[1]) * v[1]) / (v @ v), 0, 1)
        ddiag = np.hypot(xx - p0[0] - t * v[0], yy - p0[1] - t * v[1])
        kind[ddiag < 6.0] = STREET
        # the Concord roundabout: paved plaza round the rotunda, a ring road round it
        px, py = self.PLAZA
        rpl = np.hypot(xx - px, s6.periodic_dist(yy, py, per))
        kind[rpl < 82] = STREET
        kind[rpl < 68] = SQUARE
        x0, x1, y0, y1 = self.CATHEDRAL_SQ
        kind[y0:y1, x0:x1] = SQUARE
        cobble = np.zeros_like(lake)
        cobble[y0:y1, x0:x1] = True
        x0, x1, y0, y1 = self.HIVE_SQ
        kind[y0:y1, x0:x1] = SQUARE
        for x0, y0, x1, y1, _, _ in self.TOWERS:
            kind[y0 - 4:y1 + 4, x0 - 4:x1 + 4] = SQUARE
        x0, x1, y0, y1 = self.BASTIONS
        kind[y0:y1, x0:x1] = PARK
        small = (xx >= 240) & (xx < 282) & (yy >= 243) & (yy < 296)       # a planted square
        kind[small] = PARK
        jard = (xx >= shore + 18) & (xx < shore + 74) & (yy >= 306) & (yy < rc - rhw - 13)
        kind[jard] = PARK                                                 # Jardin Anglais
        # the Rhone: quay roads, stone quays, water; then the lakefront
        kind[(xx >= shore) & (rdist < rhw + 13)] = STREET
        kind[(xx >= shore) & (rdist < rhw + 3)] = QUAY
        kind[river] = WATER
        kind[lake] = WATER
        kind[(~lake) & (xx < shore + 18) & (kind != WATER)] = STREET      # quay road
        kind[(~lake) & (xx < shore + 6) & (kind != WATER)] = PROM         # promenade
        # bridges: Pont du Mont-Blanc continues the lakefront, the avenue bridge, a footbridge
        bridge = river & (((xx >= shore - 1) & (xx < shore + 19)) | ((xx >= av0 - 1) & (xx < av1 + 1)))
        isle = ((xx - 168) / 20) ** 2 + ((yy - self.river_c(168)) / 6.5) ** 2 < 1
        foot = river & (np.abs(yy - self.river_c(168)) < 1.6) & (xx > 120) & (xx < 150)
        kind[isle] = PARK
        kind[bridge | foot] = STREET

        built = kind == 0
        free = ~built
        dxd, sx = axis_dist(free, 1)
        dyd, sy = axis_dist(free, 0)
        cheb = cheb_dist(free)
        d = 0.5 * (np.minimum(dxd, dyd) + cheb)
        # block ids (for per-block parameters) from the street grid
        vcen = [av0 - 1] + [(a + b) / 2 for a, b in self.VSTREETS] + [av1 + 1]
        hcen = sorted({(a + b) / 2 for a, b, _, _ in self.HSTREETS} | {388})
        col = np.digitize(xx, sorted(vcen))
        row = np.digitize(yy, hcen) % len(hcen)
        cell = (col * 16 + row).astype(np.int64)
        ring = 13 + np.floor(hash01(cell, 1) * 5)                  # ring depth 13..17 px
        # house segmentation along the street fronts (row houses of 8..18 px frontage)
        widths = rng.choice([8, 9, 10, 11, 12, 13, 14, 15, 16, 18], size=600)
        table = np.searchsorted(np.cumsum(widths), np.arange(int(widths.sum())), side="right")
        L = len(table)
        offx = (hash01(cell, 2) * L).astype(np.int64)
        offy = (hash01(cell, 3) * L).astype(np.int64)
        xi, yi = xx.astype(np.int64), yy.astype(np.int64)
        corner = (dxd < ring) & (dyd < ring)
        hrow = dyd <= dxd
        key = np.where(corner, 1_000_000 + cell * 8 + sx * 2 + sy,
                       np.where(hrow, 2_000_000 + cell * 4000 + sy * 2000 + table[(xi + offx) % L],
                                3_000_000 + cell * 4000 + sx * 2000 + table[(yi + offy) % L]))
        court = built & (d > ring)
        # inner wings across some courtyards (a row of houses with its own ridge)
        wing = np.zeros_like(built)
        wing_rh = np.zeros_like(d)
        cx_cell = np.take(np.array([0] + [(a + b) / 2 for a, b in zip(sorted(vcen)[:-1],
                                                                       sorted(vcen)[1:])] + [W]),
                          np.clip(col, 0, len(vcen)))
        hsort = sorted(hcen)
        cy_cell = np.take(np.array([(hsort[-1] - per + hsort[0]) / 2] +
                                   [(a + b) / 2 for a, b in zip(hsort[:-1], hsort[1:])]),
                          np.clip(np.digitize(yy, hsort), 0, len(hsort) - 1))
        has_wing = hash01(cell, 4) < 0.7
        vert = hash01(cell, 13) < 0.5
        wdist = np.where(vert, np.abs(xx - cx_cell - (hash01(cell, 5) - 0.5) * 20),
                         np.abs(s6.periodic_dist(yy, cy_cell, per) - (hash01(cell, 5) - 0.5) * 16))
        wing = court & has_wing & (wdist < ring / 2)
        wing_rh = np.clip(ring / 2 - wdist, 0, None)
        court &= ~wing
        roofm = built & ~court
        pitch = 0.5
        rh = pitch * np.clip(ring / 2 - np.abs(np.minimum(d, ring) - ring / 2), 0, None)
        rh = np.where(wing, pitch * wing_rh, rh)
        key = np.where(wing, 4_000_000 + cell * 4000 +
                       np.where(vert, table[(yi + offy) % L], 2000 + table[(xi + offx) % L]), key)
        eave = 3.0 + 3.6 * hash01(key, 6) + 1.2 * hash01(cell, 7)
        # materials: the old town (right bank above the river) mostly tile, the 19th-century
        # quarters mostly slate and zinc mansards, a few verdigris copper roofs
        oldtown = (xx > av1) & (yy > 150) & (yy < 360)
        p_tile = np.where(oldtown, 0.62, 0.3)
        hm = hash01(key, 8)
        hb = hash01(cell, 9)
        mat = np.where(hm < p_tile * 0.6, 0, np.where(hm < p_tile, 1,
                       np.where(hm < p_tile + (1 - p_tile) * 0.48, 2,
                                np.where(hm < 0.97, 3, 4))))
        # each block has a dominant roof (rows were built together), a few houses differ
        dom = np.where(oldtown, np.where(hb < 0.55, 0, np.where(hb < 0.85, 1, 2)),
                       np.where(hb < 0.4, 3, np.where(hb < 0.75, 2, np.where(hb < 0.9, 1, 0))))
        mat = np.where(hash01(key, 14) < 0.72, dom, mat)
        # cathedral (Saint-Pierre): nave, transept and apse on its square, ridged roofs
        cxc = 409.0
        nave = (np.abs(xx - cxc) < 11) & (yy >= 210) & (yy < 292)
        trans = (np.abs(yy - 270) < 7.5) & (np.abs(xx - cxc) < 22)
        apse = np.hypot(xx - cxc, yy - 292) < 11
        cath = nave | trans | apse
        crh = np.maximum(np.where(nave, 0.62 * (11 - np.abs(xx - cxc)), 0),
                         np.where(trans, 0.62 * (7.5 - np.abs(yy - 270)), 0))
        crh = np.maximum(crh, np.where(apse & (yy >= 292),
                                       0.62 * (11 - np.hypot(xx - cxc, yy - 292)), 0))
        roofm |= cath
        rh = np.where(cath, crh, rh)
        eave = np.where(cath, 9.0, eave)
        mat = np.where(cath, 5, mat)
        key = np.where(cath, 9, key)
        # roof shading: two-tone slopes lit from the top-left, ridges between
        gy = (np.roll(rh, -1, 0) - np.roll(rh, 1, 0)) / 2
        gx = np.gradient(rh, axis=1)
        nrm = np.sqrt(gx * gx + gy * gy + 1)
        lam = (gx * 0.55 + gy * 0.65 + 0.75) / nrm / 1.135
        shade = np.round((0.40 + 0.85 * lam) * 10) / 10
        mats = np.array([(150, 88, 66), (128, 92, 74), (94, 98, 114), (114, 122, 136),
                         (80, 116, 104), (62, 72, 86)], np.float64)
        colr = mats[mat] * (0.95 + 0.10 * np.round(hash01(key, 10)))[..., None] * shade[..., None]
        noise = r02.periodic_fbm(W, per, 30, 1502, octaves=3)
        img = np.zeros((per, W, 3))
        img[:] = (50, 50, 60)                                                # asphalt
        dbx, _ = axis_dist(built | (kind == PARK), 1)
        dby, _ = axis_dist(built | (kind == PARK), 0)
        walk = (kind == STREET) & (np.minimum(dbx, dby) <= 2)
        img[walk] = (72, 70, 80)
        sq = kind == SQUARE
        pave = ((xi % 6 == 0) | (yi % 6 == 0))
        img[sq] = np.where(pave[sq][:, None], (78, 76, 86), (88, 86, 95))
        img[cobble] = np.where(((xi + yi) % 4 == 0)[cobble][:, None], (92, 88, 90), (102, 98, 98))
        img[rpl < 68] = np.where(((rpl[rpl < 68] % 9) < 1)[:, None], (80, 78, 88), (92, 90, 99))
        pk = kind == PARK
        img[pk] = np.array((36, 58, 40)) + np.round(noise[pk] * 2)[:, None] / 2 * np.array((10, 18, 10))
        img[kind == QUAY] = (100, 98, 106)
        img[kind == PROM] = np.where(pave[kind == PROM][:, None], (82, 80, 88), (90, 88, 96))
        img[court] = (50, 50, 58)
        garden = court & (hash01(cell, 11) < 0.55)
        img[garden] = (36, 52, 40)
        wv = r03.stretched(W, per, [30, 15], 1406, 4)
        wtr = kind == WATER
        lk = np.array((22, 34, 60)) + np.round(noise * 3)[..., None] / 3 * np.array((10, 14, 22))
        lk = lk + (np.abs(wv - 0.5) < 0.05)[..., None] * np.array((14, 18, 26))
        shallow = lake & (xx > shore - 6)
        lk = np.where(shallow[..., None], lk + np.array((8, 12, 16)), lk)
        img[wtr] = lk[wtr]
        ridge = roofm & ~cath & np.where(wing, wing_rh > ring / 2 - 0.8,
                                         np.abs(np.minimum(d, ring) - ring / 2) < 0.8)
        colr[ridge] *= 1.1
        colr[roofm & (d < 1.0)] *= 0.84                                    # eaves
        img[roofm] = colr[roofm]
        # party walls between houses: a thin dark line where one house meets the next
        party = roofm & ((key != np.roll(key, -1, 1)) & np.roll(roofm, -1, 1) |
                         (key != np.roll(key, -1, 0)) & np.roll(roofm, -1, 0)) & ~cath
        img[party] *= 0.78
        # chimneys and skylights (deterministic scatter on the roof slopes)
        hgt = np.where(roofm, eave + rh, 0.0)
        cand = roofm & ~cath & (d > 2) & (d < ring - 2)
        ys_, xs_ = np.nonzero(cand)
        pick = rng.random(len(ys_)) < 1 / 230
        for y, x in zip(ys_[pick], xs_[pick]):
            yr = np.arange(y, y + 2) % per
            xr = slice(x, min(W, x + 2))
            img[yr, xr] = (74, 68, 70)
            img[y, x] = (112, 104, 102)
            hgt[yr, xr] += 1.5
        pick = rng.random(len(ys_)) < 1 / 260
        for y, x in zip(ys_[pick], xs_[pick]):
            horiz = hrow[y, x]
            w_, h_ = (3, 2) if horiz else (2, 3)
            c = (176, 146, 96) if rng.random() < 0.2 else (66, 80, 100)
            img[np.arange(y, y + h_) % per, x:min(W, x + w_)] = c
        # cast shadows (towers excluded: they are drawn per frame with their own shadow)
        shad = cast_shadow(hgt, length=0.9)
        img *= (1 - 0.45 * shad)[..., None]
        bridge_m = bridge | foot
        img[bridge_m] = (64, 64, 72)
        rail = bridge_m & ~(np.roll(bridge_m, 1, 1) & np.roll(bridge_m, -1, 1))
        img[rail] = (116, 114, 122)
        bshadow = wtr & ~bridge_m & (np.roll(bridge_m, 3, 1) | np.roll(bridge_m, 2, 1))
        img[bshadow] *= 0.7
        # road markings: dashed centre line on the avenue, zebra crossings at junctions
        dash = (np.abs(xx - (av0 + av1) / 2 + 0.5) < 0.6) & ((yi // 5) % 2 == 0) & (kind == STREET)
        img[dash & ~walk] = (118, 116, 108)
        for y0, y1, xa, xb in self.HSTREETS:
            for yb in (y0 - 5, y1 + 1):
                for xa_, xb_ in [(av0 + 2, av1 - 2)] + [(a + 1, b - 1) for a, b in self.VSTREETS]:
                    if xb < xa_ or xa > xb_:
                        continue
                    zx = np.arange(xa_, xb_)
                    zx = zx[(zx - xa_) % 3 == 0]
                    yr = np.arange(yb, yb + 4) % per
                    sel = (kind[np.ix_(yr, zx)] == STREET)
                    blk = img[np.ix_(yr, zx)]
                    blk[sel] = (124, 122, 118)
                    img[np.ix_(yr, zx)] = blk
        out = raster.to_rgba_image(np.clip(img, 0, 255))
        d2 = ImageDraw.Draw(out)
        # parked cars along the side streets
        pcols = [(96, 62, 60), (70, 80, 98), (122, 122, 126), (60, 66, 62), (138, 132, 118),
                 (52, 56, 76)]
        for x0, x1 in self.VSTREETS:
            for side in (0, 1):
                y = int(rng.integers(0, 30))
                while y < per:
                    y += int(rng.integers(9, 30))
                    xc = x0 + 1 if side == 0 else x1 - 4
                    if all(kind[(y + k) % per, xc + 1] == STREET for k in range(7)):
                        c = pcols[int(rng.integers(len(pcols)))]
                        for dy in (-per, 0):
                            d2.rectangle([xc + 1, y + dy + 1, xc + 3, y + dy + 6], fill=(30, 30, 38))
                            d2.rectangle([xc, y + dy, xc + 2, y + dy + 5], fill=c)
                            d2.line([xc, y + dy + 1, xc + 2, y + dy + 1], fill=(30, 34, 46))
        for y0, y1, xa, xb in self.HSTREETS[1:]:
            x = xa + int(rng.integers(4, 30))
            while x < xb - 8:
                x += int(rng.integers(10, 34))
                yc = y0 + 1
                if x + 6 < W and all(kind[yc + 1, x + k] == STREET for k in range(7)):
                    c = pcols[int(rng.integers(len(pcols)))]
                    d2.rectangle([x + 1, yc + 1, x + 6, yc + 3], fill=(30, 30, 38))
                    d2.rectangle([x, yc, x + 5, yc + 2], fill=c)
        # the Jet d'Eau's jetty with its little lighthouse, moored boats in the harbour
        fx, fy = self.FOUNTAIN
        jx = float(self.shore_x(fy + 14))
        for dy in (-per, 0, per):
            d2.line([fx, fy + dy, jx + 2, fy + 14 + dy], fill=(104, 100, 108), width=3)
            d2.ellipse([fx - 3, fy - 3 + dy, fx + 3, fy + 3 + dy], fill=(118, 116, 124))
        for i in range(9):
            by_ = 280 + i * 9 + int(rng.integers(-2, 3))
            bx_ = float(self.shore_x(by_)) - 8 - 6 * (i % 2)
            for dy in (-per, 0):
                d2.ellipse([bx_ - 4, by_ + dy - 1.5, bx_ + 4, by_ + dy + 1.5], fill=(150, 152, 160))
                d2.point([(bx_ + 1, by_ + dy)], fill=(70, 74, 90))
        # flower clock in the Jardin Anglais (muted, away from the reserved hues)
        fcx, fcy = float(self.shore_x(330)) + 40, 330
        d2.ellipse([fcx - 8, fcy - 8, fcx + 8, fcy + 8], fill=(70, 96, 60))
        for k in range(12):
            a = TAU * k / 12
            c = [(150, 120, 80), (120, 70, 80), (190, 186, 170)][k % 3]
            d2.ellipse([fcx + 6 * np.cos(a) - 1.2, fcy + 6 * np.sin(a) - 1.2,
                        fcx + 6 * np.cos(a) + 1.2, fcy + 6 * np.sin(a) + 1.2], fill=c)
        d2.line([fcx, fcy, fcx + 4, fcy - 3], fill=(30, 30, 30))
        # trees: promenade, avenue, the diagonal, parks, gardens, squares
        greens = [(10, 22, 14), (20, 36, 24), (34, 56, 36), (50, 74, 48)]
        trees = [r03.foliage_sprite(sz, 1520 + k, greens, night=0.85)
                 for k, sz in enumerate((9, 11, 12, 13, 10, 14))]
        spots = []
        for y in range(4, per, 13):
            spots.append((float(self.shore_x(y)) + 3, y, 0))
        for y in range(6, per, 16):
            for x in (av0 + 1.5, av1 - 1.5):
                if rpl[y, int(x)] > 86 and not river[y, int(x)] and kind[y, int(x)] == STREET:
                    spots.append((x, y, 0))
        for k in range(12):
            tt = (k + 0.5) / 12
            x, y = p0 + v * tt
            nx_, ny_ = -v[1] / np.hypot(*v), v[0] / np.hypot(*v)
            for s_ in (-1, 1):
                spots.append((x + s_ * 5 * nx_, y + s_ * 5 * ny_, 0))
        for k in range(16):
            a = TAU * (k + 0.5) / 16
            spots.append((px + 64 * np.cos(a), py + 64 * np.sin(a), 0))
        parkm = (kind == PARK)
        tries = 0
        placed = []
        while tries < 2600:
            tries += 1
            x, y = rng.uniform(0, W), rng.uniform(0, per)
            if not parkm[int(y), int(x)] and not (garden[int(y), int(x)] and rng.random() < 0.08):
                continue
            if any((x - a) ** 2 + (s6.periodic_dist(y, b, per)) ** 2 < 64 for a, b in placed):
                continue
            placed.append((x, y))
        spots += [(x, y, 1) for x, y in placed]
        x0, x1, y0, y1 = self.CATHEDRAL_SQ
        for (x, y) in ((386, 230), (386, 250), (432, 236), (432, 300), (386, 296)):
            spots.append((x, y, 0))
        # paths through the Bastions (pale gravel) before the trees go on
        x0, x1, y0, y1 = self.BASTIONS
        for dy in (-per, 0):
            d2.line([(x0 + x1) / 2, y0 + dy, (x0 + x1) / 2, y1 + dy], fill=(104, 100, 92), width=3)
            d2.line([x0, y0 + dy, x1, y1 + dy], fill=(96, 92, 86), width=2)
            d2.line([x1, y0 + dy, x0, y1 + dy], fill=(96, 92, 86), width=2)
        for x, y, k in spots:
            t_ = trees[int(hash01(int(x * 7 + y * 13), 12) * len(trees))]
            for dy in (-per, 0, per):
                sprite.paste_center(out, sprite.shadow_of(t_, opacity=0.5, blur=1.0), x + 3, y + dy + 4)
                sprite.paste_center(out, t_, x, y + dy)
        # canopy shadow: mottled, but soft enough to keep the city readable
        arr = np.asarray(out).astype(np.float64)[..., :3]
        c1, c2, idx = s6.voronoi_periodic(W, per, 46, 1403)
        vein = np.clip(1 - (c2 - c1) / 9.0, 0, 1)
        holes = (idx % 4 == 0)
        lightm = s6.pblur_mask(np.clip(1 - vein, 0, 1) * np.where(holes, 1.0, 0.5), 7.0)
        arr = arr * (0.66 + 0.40 * lightm[..., None]) + np.array((14, 6, 26)) * (1 - lightm[..., None])
        # sodium street lamps: lamp heads and soft pools (after the canopy: they are lights)
        lamps = np.zeros((per, W))
        heads = []
        for y in range(3, per, 22):
            for x, ph in ((av0 + 0.5, 0), (av1 - 1.5, 11)):
                yy_ = (y + ph) % per
                if kind[yy_, int(x)] == STREET and rpl[yy_, int(x)] > 84:
                    heads.append((x, yy_))
        for y in range(10, per, 20):
            heads.append((float(self.shore_x(y)) + 6.5, y))
        for i, (x0, x1) in enumerate(self.VSTREETS):
            for y in range(5 + 9 * i, per, 34):
                if kind[y, x1 - 1] == STREET:
                    heads.append((x1 - 1, y))
        for y0, y1, xa, xb in self.HSTREETS:
            for x in range(xa + 9, xb, 48):
                if kind[y0, x] == STREET:
                    heads.append((x, y0))
        for k in range(12):
            a = TAU * k / 12
            heads.append((px + 81 * np.cos(a), (py + 81 * np.sin(a)) % per))
        for x, y in heads:
            lamps[int(y) % per, int(np.clip(x, 0, W - 1))] = 1
        pool = s6.pblur_mask(lamps, 3.0)
        pool = pool / (pool.max() or 1)
        arr = arr + np.clip(pool * 1.4, 0, 1)[..., None] * np.array((54, 38, 16))
        out = r02.posterize(raster.to_rgba_image(np.clip(arr, 0, 255)), self.TEX_COLORS)
        if self.TEX_SMOOTH:
            out = s6.smooth_tex(out, self.TEX_SMOOTH)
        dl = ImageDraw.Draw(out)
        for x, y in heads:
            dl.point([(x, y)], fill=(236, 204, 140))
        out = self._overgrowth10(out, per)
        out = self._landmarks10(out, per)
        self.city_kind = kind
        return out

    def _overgrowth10(self, img, per):
        """The Siege Spire's roots running down over the roofs from the city centre ahead, and
        creep round the Hive square: thinner and fewer than round 09, so the streets stay
        readable under them."""
        ss = 2
        lay = Image.new("RGBA", (W * ss, per * ss), (0, 0, 0, 0))
        glow = Image.new("RGBA", (W * ss, per * ss), (0, 0, 0, 0))
        d, g = ImageDraw.Draw(lay), ImageDraw.Draw(glow)
        rng = np.random.default_rng(1511)
        body, tip = (44, 38, 56), (0, 255, 154)
        for x0, ang, ln_, w in ((340, 1.85, 260, 8.0), (452, 1.7, 220, 6.5), (250, 1.35, 170, 5.5)):
            s6.branches(d, g, rng, x0, -20, ang, ln_, w, 2, body, tip, ss, per, step=6, jitter=0.22)
        out = img.copy()
        for cx, cy, r in ((self.HIVE[0], self.HIVE[1], 50), (322, 300, 22)):
            for dy in (-per, 0, per):
                e6.creep(out, cx, cy + dy, r, int(cx))
        lay = lay.resize((W, per), Image.BOX)
        glow = glow.resize((W, per), Image.BOX)
        out.alpha_composite(s6.pshadow(lay, 0.55, 1.2), (5, 7))
        out.alpha_composite(lay)
        seam = np.asarray(lay).copy()
        seam[..., :3] = (0, 150, 104)
        seam[..., 3] = (seam[..., 3] > 200) * 80
        out.alpha_composite(Image.fromarray(seam, "RGBA").filter(ImageFilter.MinFilter(3)))
        out.alpha_composite(s6.pmid(raster.glow(s6.ptile(glow), 2.0, 0.9), per))
        return out

    def _landmarks10(self, img, per):
        out = img.copy()
        rot = s8.model("rotunda")
        px, py = self.PLAZA
        d = ImageDraw.Draw(out)
        for dy in (-per, 0, per):
            for k in range(16):                          # ring of flagpoles, UTC blue flags
                a = TAU * (k + 0.25) / 16
                fx, fy = px + 58 * np.cos(a), py + dy + 58 * np.sin(a)
                d.line([fx, fy, fx + 4, fy + 1], fill=(60, 104, 190, 255), width=2)
                d.point([(fx, fy)], fill=(200, 200, 210, 255))
            sh = sprite.shadow_of(rot, opacity=0.55, blur=1.5)
            out.alpha_composite(sh, (int(px - rot.width / 2 + 8), int(py + dy - rot.height / 2 + 10)))
            sprite.paste_center(out, rot, px, py + dy)
        # flags on the Pont du Mont-Blanc
        for y in range(372, 406, 6):
            x = float(self.shore_x(y))
            for xx_ in (x, x + 18):
                d.point([(xx_, y)], fill=(200, 200, 210, 255))
                d.line([xx_, y, xx_ + 3, y + 1], fill=(60, 104, 190, 255) if y % 12 else
                       (176, 60, 60, 255), width=1)
        return out

    # ------------------------------------------------------------------ per-frame parts
    def _tall_box(self, d, ss, x0, y0, x1, y1, h, lit, dark, roof, wins=None, seed=0, h0=0.0):
        """A box standing on the ground, its roof pushed away from the screen centre: the walls
        that face the centre show (lit if they face up or left)."""
        base = [(x0, y0), (x1, y0), (x1, y1), (x0, y1)]
        b = [project(x, y, h0) for x, y in base]
        t = [project(x, y, h) for x, y in base]
        rng = np.random.default_rng(seed)
        for name, i, j in (("top", 0, 1), ("right", 1, 2), ("bottom", 2, 3), ("left", 3, 0)):
            quad = [b[i], b[j], t[j], t[i]]
            area = sum(quad[k][0] * quad[(k + 1) % 4][1] - quad[(k + 1) % 4][0] * quad[k][1]
                       for k in range(4))
            if area <= 0:
                continue
            col = lit if name in ("top", "left") else dark
            d.polygon([(x * ss, y * ss) for x, y in quad], fill=col)
            if wins:
                floors = max(2, int((h - h0) * 30))
                for fl in range(1, floors):
                    u = fl / floors
                    ax, ay = b[i][0] + (t[i][0] - b[i][0]) * u, b[i][1] + (t[i][1] - b[i][1]) * u
                    bx, by = b[j][0] + (t[j][0] - b[j][0]) * u, b[j][1] + (t[j][1] - b[j][1]) * u
                    d.line([ax * ss, ay * ss, bx * ss, by * ss],
                           fill=tuple(int(c * 0.8) for c in col), width=1)
                    nwin = int(max(abs(bx - ax), abs(by - ay)) / 3)
                    for wv in range(1, nwin):
                        if rng.random() < 0.12:
                            v = wv / nwin
                            wx, wy = ax + (bx - ax) * v, ay + (by - ay) * v
                            d.rectangle([wx * ss, wy * ss, wx * ss + 1, wy * ss + 1], fill=wins)
        d.polygon([(x * ss, y * ss) for x, y in t], fill=roof)
        return t

    def _pyramid(self, d, ss, x0, y0, x1, y1, h0, h1, lit, dark):
        base = [project(x, y, h0) for x, y in ((x0, y0), (x1, y0), (x1, y1), (x0, y1))]
        apex = project((x0 + x1) / 2, (y0 + y1) / 2, h1)
        for name, i, j in (("top", 0, 1), ("right", 1, 2), ("bottom", 2, 3), ("left", 3, 0)):
            tri = [base[i], base[j], apex]
            area = sum(tri[k][0] * tri[(k + 1) % 3][1] - tri[(k + 1) % 3][0] * tri[k][1]
                       for k in range(3))
            if area <= 0:
                continue
            d.polygon([(x * ss, y * ss) for x, y in tri], fill=lit if name in ("top", "left") else dark)

    def tall_layer(self, f):
        """The tall structures of the frame (cathedral towers and spire, the two Concord towers)
        and their cast shadows, drawn in perspective for this frame's scroll position."""
        ss = 2
        off = self.s.off("ground", f)
        shadow = Image.new("L", (W * ss, H * ss), 0)
        sd = ImageDraw.Draw(shadow)
        lay = Image.new("RGBA", (W * ss, H * ss), (0, 0, 0, 0))
        d = ImageDraw.Draw(lay)
        items = []
        for x0, y0, x1, y1, h, kind in self.TOWERS:
            for dy in self.ys("ground", 0, f, margin=200):
                items.append(("tower", x0, y0 + dy, x1, y1 + dy, h, kind))
        for dy in self.ys("ground", 0, f, margin=320):
            items.append(("ctower", 393, 204 + dy, 405, 216 + dy, 0.2, None))
            items.append(("ctower", 413, 204 + dy, 425, 216 + dy, 0.2, None))
            items.append(("spire", 405, 266 + dy, 413, 274 + dy, 0.16, None))
        for it in items:
            _, x0, y0, x1, y1, h, _ = it
            if y1 < -120 or y0 > H + 120:
                continue
            top = h if it[0] != "spire" else 0.5
            L = 40.0 * top
            pts = [(x, y) for x, y in ((x0, y0), (x1, y0), (x1, y1), (x0, y1))]
            pts += [(x + 0.6 * L, y + 0.8 * L) for x, y in pts]
            sd.polygon([(x * ss, y * ss) for x, y in convex_hull(pts)], fill=110)
        for it in sorted(items, key=lambda it: it[5]):
            typ, x0, y0, x1, y1, h, kind = it
            if y1 < -120 or y0 > H + 120:
                continue
            if typ == "tower":
                t = self._tall_box(d, ss, x0, y0, x1, y1, h, (98, 106, 126), (54, 58, 76),
                                   (84, 88, 102), wins=(176, 150, 100), seed=int(x0))
                (ax, ay), (bx, by) = t[0], t[2]
                d.rectangle([ax * ss + 3, ay * ss + 3, bx * ss - 3, by * ss - 3], outline=(110, 114, 128),
                            width=2)
                cxr, cyr = (ax + bx) / 2, (ay + by) / 2
                if kind == "pad":
                    d.ellipse([(cxr - 7) * ss, (cyr - 7) * ss, (cxr + 7) * ss, (cyr + 7) * ss],
                              outline=(150, 150, 156), width=2)
                    d.line([(cxr - 2.5) * ss, (cyr - 3) * ss, (cxr - 2.5) * ss, (cyr + 3) * ss],
                           fill=(150, 150, 156), width=2)
                    d.line([(cxr + 2.5) * ss, (cyr - 3) * ss, (cxr + 2.5) * ss, (cyr + 3) * ss],
                           fill=(150, 150, 156), width=2)
                    d.line([(cxr - 2.5) * ss, cyr * ss, (cxr + 2.5) * ss, cyr * ss],
                           fill=(150, 150, 156), width=2)
                else:
                    d.rectangle([(cxr - 5) * ss, (cyr - 4) * ss, (cxr + 2) * ss, (cyr + 3) * ss],
                                fill=(70, 74, 86))
                    d.rectangle([(bx - 4) * ss, (ay + 2) * ss, (bx - 2) * ss, (ay + 4) * ss],
                                fill=(200, 60, 50) if (f // 10) % 2 == 0 else (90, 40, 40))
            elif typ == "ctower":
                self._tall_box(d, ss, x0, y0, x1, y1, h, (122, 116, 114), (72, 68, 74),
                               (90, 86, 90), wins=None)
                self._pyramid(d, ss, x0, y0, x1, y1, h, h + 0.1, (88, 128, 112), (52, 80, 72))
            else:
                self._tall_box(d, ss, x0, y0, x1, y1, h, (120, 114, 112), (70, 66, 72),
                               (88, 84, 88), wins=None, h0=0.1)
                self._pyramid(d, ss, x0, y0, x1, y1, h, 0.5, (92, 134, 118), (50, 80, 72))
        shadow = shadow.filter(ImageFilter.GaussianBlur(1.5)).resize((W, H), Image.BOX)
        sh_img = Image.new("RGBA", (W, H), (8, 4, 16, 0))
        sh_img.putalpha(shadow)
        lay = lay.resize((W, H), Image.BOX)
        return sh_img, lay

    def car_path(self, yw, north):
        """Lane x on the avenue for a world y; round the roundabout counter-clockwise."""
        px, py = self.PLAZA
        lane = 306 if north else 296
        dy = s6.periodic_dist(yw, py, self.TRAVEL)
        R = 75.0
        if abs(dy) < R:
            ring = np.sqrt(R * R - dy * dy)
            return px + ring if north else px - ring
        return lane

    def draw_cars(self, ground, f):
        if not hasattr(self, "_cars"):
            cols = [(118, 120, 128), (96, 62, 60), (70, 84, 104), (140, 136, 124), (74, 92, 82)]
            self._cars = {c: car_sprite(c) for c in cols}
            self._apc = car_sprite((88, 96, 118), kind="apc")
            self._rot = {}
        per = self.TRAVEL
        jobs = []
        for i in range(4):                       # northbound: 4 cars 150 apart, 150 px per loop
            jobs.append((True, (60 + 150 * i - 150 * f / N) % per, i))
        for i in range(3):                       # southbound: 3 cars 200 apart, 200 px per loop
            jobs.append((False, (120 + 200 * i + 200 * f / N) % per, i + 4))
        for north, yw, i in jobs:
            sp0 = self._apc if i in (1, 5) else list(self._cars.values())[i % 5]
            dy = -1.0 if north else 1.0              # direction of travel in world y
            dx = self.car_path(yw + dy, north) - self.car_path(yw - dy, north)
            hd = np.degrees(np.arctan2(dx, -2 * dy))  # clockwise from screen-up
            key = (id(sp0), int(round(hd / 5.625)) % 64)
            if key not in self._rot:
                r = sp0.rotate(-key[1] * 5.625, resample=Image.BICUBIC, expand=True)
                self._rot[key] = r.resize((max(1, r.width // 4), max(1, r.height // 4)), Image.BOX)
            spr = self._rot[key]
            x = self.car_path(yw, north)
            for y in self.ys("ground", yw, f, margin=30):
                sprite.paste_center(ground, sprite.shadow_of(spr, opacity=0.35, blur=0.6), x + 1, y + 1.5)
                sprite.paste_center(ground, spr, x, y)

    def creeper_x(self, yw):
        return float(self.shore_x(yw)) + 12

    def layer_images(self, f):
        off = self.s.off("ground", f)
        ground = scroll_tex(self.ground_tex, off)
        self.draw_cars(ground, f)
        # the Hive Node on its square, iris breathing
        iris = round(0.5 + 0.5 * np.sin(th(f, 2)), 1)
        hv = e6.hive.get(0.0, iris=round(iris * 4) / 4, pulse=round(1 + np.sin(th(f, 4))) / 2)
        for hy in self.ys("ground", self.HIVE[1], f, margin=60):
            sprite.paste_center(ground, sprite.shadow_of(hv, opacity=0.5, blur=1.2), self.HIVE[0] + 6, hy + 8)
            sprite.paste_center(ground, hv, self.HIVE[0], hy)
        # a Creeper convoy marching along the lakefront quay road (planted feet)
        for k in range(3):
            yw = 40 + k * 200 + 200 * f / N
            x = self.creeper_x(yw)
            hd = float(np.arctan2(1.0, self.creeper_x(yw + 1) - x))
            for yy in self.ys("ground", yw, f, margin=50):
                sp = e6.crp_sprite(hd, yw)
                sprite.paste_center(ground, sprite.shadow_of(sp, opacity=0.5, blur=1.0), x + 4, yy + 6)
                sprite.paste_center(ground, sp, x, yy)
        sh_img, tall = self.tall_layer(f)
        ground.alpha_composite(sh_img)
        ground.alpha_composite(tall)
        # the Jet d'Eau: a tall plume, so it leans away from the centre, blown downwind
        fx, fyw = self.FOUNTAIN
        for fy in self.ys("ground", fyw, f, margin=80):
            tx, ty = project(fx, fy, 0.45)
            fm = s8.Foam(color=(222, 228, 236))
            ln = float(np.hypot(tx - fx, ty - fy)) + 1
            s8.stream(fm, f, fx, fy, tx - fx, ty - fy, ln, 5, 26, 0.5, 2.0, 1.0, 1.9, 0.9, 1462,
                      wave=0.4)
            s8.stream(fm, f, tx, ty, 0.45, 0.89, 46, 3, 30, 1.5, 9.0, 1.4, 3.2, 0.7, 1460, wave=2)
            s8.stream(fm, f, fx, fy, 0.45, 0.89, 24, 3, 14, 1.0, 5.0, 1.0, 2.2, 0.5, 1461)
            ground.alpha_composite(fm.done())
        air = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        casters = []
        skitter_snake(air, casters, self.cast, f, 330, 100, 0.1)
        npos = lambda ff: (140 + 50 * np.sin(th(ff)), 120 + 16 * np.sin(th(ff, 2)))
        nd = self.cast.unit("needler-a", anim=[-1.0, 0.0, 1.0, 0.0][int(f // 4) % 4])
        casters.append((nd,) + npos(f))
        sprite.paste_center(air, nd, *npos(f))
        s6.aimed(air, f, lambda ff: (npos(ff)[0], npos(ff)[1] + 14), self.cast, period=40,
                 speed=6.5, kind="thorn", phase=6)

        def hive_src(ff):
            y = (self.HIVE[1] + self.s.off("ground", ff)) % self.TRAVEL
            return (self.HIVE[0], y) if 0 <= y <= H else None
        s8.fan_shots(air, f, hive_src, self.cast, period=40, speed=5.0, n=5, spread=0.24,
                     phase=24, kind="acid")
        self.cast.player(air, casters, f)
        shadows = r02.shadow_layer(casters, *s6.SHADOW["air"], opacity=0.45)
        low = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        d = ImageDraw.Draw(low)
        for x0, y0, ph in self.spores:
            x = (x0 + 10 * np.sin(th(f) + ph)) % W
            y = (y0 + 1.35 * self.TRAVEL * f / N) % (H + 20) - 10
            d.ellipse([x - 1.5, y - 1.5, x + 1.5, y + 1.5], fill=(120, 230, 180, 150))
        L = {"ground": ground, "shadows": shadows, "low-air": low, "air": air}
        L = r03.decorate(L, scroll_tex(self.haze, self.s.off("low-air", f)), "ground", casters,
                         bank_shadow_opacity=0.22, flyer_opacity=0.3)
        L["high-air"] = scroll_tex(self.canopy, self.s.off("high-air", f))
        return L

    def key_list(self):
        return [s8.model("rotunda"), e6.hive.get(0.0, iris=0.5, pulse=0.5),
                e6.crp_sprite(np.pi / 2, 0.0)]


# =========================================================================== sheet + main

SCENES = {"geneva": GenevaScene, "storm": StormScene, "ocean": OceanScene}

NOTES = {
    "geneva": ("GENEVA (L14): A CITY FROM ABOVE - AVENUE, SIDE STREETS, A DIAGONAL AND SQUARES WITH "
               "SIDEWALKS, ZEBRA CROSSINGS, SODIUM LAMPS AND CARS; ROW HOUSES IN PERIMETER BLOCKS "
               "ROUND COURTYARDS (RIDGED TWO-TONE ROOFS, CHIMNEYS, SKYLIGHTS, CAST SHADOWS); THE "
               "LAKE AND JET D'EAU, THE RHONE WITH THE PONT DU MONT-BLANC AND ILE ROUSSEAU, SAINT-"
               "PIERRE, THE CONCORD ROTUNDA, THE BASTIONS. TOWERS, SPIRE AND PLUME LEAN AWAY FROM "
               "THE CENTRE. THE SPIRE'S ROOTS, CREEP AND CANOPY SHADOW LIE OVER THE CITY."),
    "storm": ("STORM (L12), MOTION BUDGET: TWO STRONG ELEMENTS - THE HEAVING SEA AND THE RAIN. THE "
              "SWELL MOVES HALF AS FAST AND LOWER, CRESTS AND WIND STREAKS ARE FAINTER, THE RAIN IS "
              "FAINT AND MOTION-BLURRED (NO JUMPS), THE SCUD ONLY SWAYS (24 %), ONE SHORT FLASH PER "
              "LOOP. 140 PX/S. GIF AT 25 FPS."),
    "ocean": ("OCEAN (L11), MOTION BUDGET: TWO STRONG ELEMENTS - THE SEA AND THE WAKES. THE SWELL "
              "KEEPS ITS DEPTH BUT MOVES ABOUT HALF AS FAST, FINE WAVES, WHITECAPS, COLLARS AND WISPS "
              "ARE FAINTER, THE SMOKE IS SLOWER. GIF AT 25 FPS."),
}


def make_sheet(scene, f=24):
    frame_img = scene.compose(f)
    layers = scene.layer_images(f)
    tw, th_ = 192, 216
    img = raster.sheet(1140, 690, f"SCENE {scene.slug.upper()} (R10): {scene.title}",
                       f"CONCEPT ROUND 10 - {scene.intensity}")
    speed = scene.TRAVEL * FPS / N
    raster.draw_text(img, 16, 38, f"PLAY FIELD 480X540 AT 1X - GROUND SCROLLS {speed:.0f} PX/S",
                     raster.LABEL_DIM)
    img.alpha_composite(frame_img, (16, 50))
    raster.draw_text(img, 516, 38, "LAYER BREAKDOWN, BACK TO FRONT (0.4X)", raster.LABEL_DIM)
    for i, key in enumerate(scene.layers):
        x = 516 + (i % 3) * (tw + 12)
        y = 50 + (i // 3) * (th_ + 44)
        cell = r02.checker(tw, th_)
        lay = layers[key]
        if key == "air":
            lay = layers["shadows"].copy()
            lay.alpha_composite(layers["air"])
        cell.alpha_composite(lay.resize((tw, th_), Image.BOX))
        img.alpha_composite(cell, (x, y + 12))
        raster.draw_text(img, x, y, f"{i + 1}. {key.upper()}", raster.ACCENT)
        raster.draw_text(img, x, y + th_ + 16, scene.notes[key], raster.LABEL_DIM)
    notes = [
        "FACTORS RELATIVE TO GROUND: " + ", ".join(f"{k.upper()} {v:g}" for k, v in
                                                   scene.FACTORS.items()) + ".",
        NOTES[scene.slug],
        "MOTION BUDGET: AT MOST TWO STRONGLY ANIMATED ELEMENTS, FINE REPETITIVE MOTION LOW IN "
        "CONTRAST, NO ELEMENT JUMPS MORE THAN ABOUT 2 PX PER FRAME RELATIVE TO ITS LAYER."
        if scene.slug != "geneva" else
        "PERSPECTIVE: TALL STRUCTURES ARE DRAWN PER FRAME WITH THE CAMERA-HEIGHT MODEL OF PARALLAX B; "
        "LOW HOUSES ARE ORTHOGRAPHIC. ATMOSPHERE INTENSITY: MEDIUM; HIGH-AIR UNDER 40 %.",
    ]
    y = 580
    for line in notes:
        for part in r02.wrap(line, 100):
            raster.draw_text(img, 516, y, part, raster.LABEL)
            y += 11
        y += 5
    return img


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    names = [a for a in sys.argv[1:] if a in SCENES] or list(SCENES)
    for slug in names:
        scene = SCENES[slug]()
        png = OUT / f"scene-{slug}-r10-a.png"
        make_sheet(scene).convert("RGB").save(png, optimize=True)
        print("wrote", png.relative_to(ROOT))
        if "--sheet" in sys.argv:
            continue
        gif = OUT / f"scene-{slug}-r10-a.gif"
        rate = getattr(scene, "GIF_RATE", GIF_FPS)
        cols = make_gif10(scene, gif, scene.GIF_COLORS, n=4 * rate, fps=rate)
        print("wrote", gif.relative_to(ROOT), f"{gif.stat().st_size / 1e6:.1f} MB, {cols} colours")


if __name__ == "__main__":
    main()
