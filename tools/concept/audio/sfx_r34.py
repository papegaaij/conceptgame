#!/usr/bin/env python3
"""Concept round 34 (M5 part F, Level 12 "Storm Front"): the storm's and the Lamprey's sounds (user
decisions F6 = a and F8 = a): a/b for the thunder (2-3 variants each), the gust, the Lamprey's latch and
its drain loop; one proposal for the shake-off and the sonde chime; and the rain-only storm bed cut
from the same CC0 source as the chosen ambience-storm-r08-a (which stays). Recorded layers (Freesound
originals, all already in CREDITS.md for earlier rounds) and synthesized layers mixed.

Outputs (design/audio/sfx/concept/, OGG Vorbis q6, 44.1 kHz stereo; each file starts at its event):

  thunder-{1,2,3}-r34-a.ogg  a "the storm's own thunder": three rolls cut from the storm bed's source
                             ("Rain and Thunder 4", FlatHill, CC0: its one big clap and two later
                             rolls), low-passed to drop the rain, a soft sub rumble under each; no
                             crack, a heavy distant roll that matches the bed. 1 the big clap and roll
                             (6.5 s), 2 and 3 shorter rolls (3.6 s)
  thunder-{1,2,3}-r34-b.ogg  b "close strikes": a close thunder strike ("Closeup Thunder Strike 01",
                             loganzsound, CC0) at three speeds (1.0, 0.86, 0.74: nearer to farther)
                             and pans, a sub rumble under each: a sharp crack, then the roll
                             The game plays variant (flash index mod 3) 0.3-1.5 s after the flash
                             (the delay fixed by the flash's index, the README).
  gust-r34-a.ogg             a synthesized: band-passed wind rising over the 1.0 s telegraph, a
                             hollow howl on top, holding through the 2.5 s gust with a flutter, then
                             falling; panned from the left (upwind) to the right; 3.6 s
  gust-r34-b.ogg             b recorded: a gust from an arctic wind recording ("Wind Artic Cold",
                             cobratronik, CC0) with the same envelope and pan, a low rumble under it
  lamprey-latch-r34-a.ogg    a "suck and bite": an inhaling suction rush, then the bite at 0.16 s (a
                             wet slime attack, qubodup, CC0) with teeth grinding in (flesh tear,
                             aust_paul, CC0) and a thump; 0.75 s
  lamprey-latch-r34-b.ogg    b "clamp on the hull": the bite lands at once: a wet splat (JustInvoke,
                             CC-BY) on a metallic thud (profoundsounds, CC0), teeth gnawing in, a
                             thump; 0.8 s
  lamprey-drain-r34-a.ogg    a seamless 2.4 s loop, synthesized: a low electric hum throbbing in
                             0.4 s gulps (the latch look a's swallow at 10 fps) with a sparse
                             crackle and a faint high whine; while any Lamprey drains
  lamprey-drain-r34-b.ogg    b seamless 2.4 s loop: a siphoning whine with crackle (zimbot's sonic
                             death ray, CC-BY, slowed to 0.45) over a sub hum swelling twice a loop
  lamprey-shake-r34-a.ogg    the shake-off (one proposal): a wet slap (JustInvoke, CC-BY) and a
                             short squeal (Wolfsinger's screech, CC-BY, sped up) whooshing away; 0.9 s
  sonde-chime-r34-a.ogg      the sonde collected (one proposal), synthesized: a three-blip telemetry
                             chirp into two bright bell tones (E6, B6); 0.95 s
  ambience-storm-rain-r34-a.ogg  the rain-only storm bed (F6 = a): the stretch of "Rain and Thunder 4"
                             after its thunder (21.0-32.5 s of the original), high-passed at 90 Hz to
                             drop the faint far rumble left in it, a seamless 10.0 s loop with a
                             1.5 s cross-fade, at the ambiences' level (200 Hz-5 kHz band RMS -36 dB,
                             peak ceiling -10 dBFS: import_sfx.py's round-08 settings; the ceiling
                             holds it at -38.8 dB band RMS). The same rain sits at -51 to -55 dB in the
                             chosen bed, whose normalisation was set by its thunder: the new bed is the
                             rain ~14 dB louder, at the other ambiences' level (the ocean's -36 dB)
Levels (sfx_r24.py's `level`: the loudest 100 ms of the 200 Hz-5 kHz band, limited at a ceiling;
the existing set in assets/sfx/): thunder 1 -14, 2 -16, 3 -17 dB (3 a -19: its roll is nearly all
below 200 Hz, so -17 in the band made it louder than 1 a: -14.8 LUFS) (under the large/huge explosions
-11/-12.5, above the distant flak -16 that the user found right for a far layer); gust -19 (a weather
cue between the edge warning -16 and the hit sounds -20); latch -16 and shake-off -17 (a warning the
player must act on, as the edge warning); drain -22 (a loop under everything, but not as quiet as the
sled whine -29: the user found quiet layers too quiet); sonde chime -16 (the pickups -15 to -17).
The loops are levelled without the limiter (it would break the seam) and checked for the seam.

Usage: python3 tools/concept/audio/sfx_r34.py [subject-or-file ...]   e.g. thunder-1 gust-r34-b.
Deterministic. Needs the cached Freesound originals (tools/concept/audio/freesound_fetch.py
--download); without one a layer falls back to the public HQ preview (printed).
"""
import os
import sys
import tempfile
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
from import_sfx import band_rms_db, make_loop, normalize  # noqa: E402
from sfx_r24 import level, resample, short_term_band_db  # noqa: E402
from sfx_r25 import fs, source  # noqa: E402
from sfx_r32 import loudest_db, recorded, report, thump  # noqa: E402
from sfx_r33 import rumble  # noqa: E402
from synth import SR, decode, exp_decay, noise, pan, ramp, sine, svf, undb, write_ogg  # noqa: E402

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "design" / "audio" / "sfx" / "concept"
SCRIPT = "tools/concept/audio/sfx_r34.py"
CC0 = "CC0 1.0"
CCBY = "CC-BY 4.0"
LOOP = 2.4                                      # s: the drain loop (6 gulps of 0.4 s)

STORM = dict(**fs("FlatHill", 237729, 237, 3839718), title="Rain and Thunder 4", author="FlatHill", licence=CC0)
STRIKE = dict(**fs("loganzsound", 840628, 840, 16682330), title="Closeup Thunder Strike 01", author="loganzsound",
              licence=CC0)
WIND = dict(**fs("cobratronik", 117136, 117, 732072), title="Wind__Artic__Cold.wav", author="cobratronik", licence=CC0)
SLIME = dict(**fs("qubodup", 751338, 751, 71257), title="Slime Attack 1", author="qubodup", licence=CC0)
TEAR = dict(**fs("aust_paul", 30928, 30, 15696), title="rip_tear FLESH!!!!.wav", author="aust_paul", licence=CC0)
SPLAT = dict(**fs("JustInvoke", 446115, 446, 758593), title="Wet Splat", author="JustInvoke", licence=CCBY)
THUD = dict(**fs("profoundsounds", 686282, 686, 14947773), title="slightmetallicthud.mp3", author="profoundsounds",
            licence=CC0)
SCREECH = dict(**fs("Wolfsinger", 25713, 25, 176969), title="inhuman screech.wav", author="Wolfsinger", licence=CCBY)
RAY = dict(**fs("zimbot", 177099, 177, 1449999), title="SonicDeathRay_1.2KHzWithCrackle.wav", author="zimbot",
           licence=CCBY)


def thunder_a(offset, length, at_pan, seed):
    return [dict(**STORM, cut=dict(offset=offset, length=length, fade=length * 0.45, fadein=0.15, highpass=30,
                                   lowpass=1800), at=0.0, pan=at_pan),
            dict(synth="rumble", at=0.0, gain=-9.0, length=length, low=38.0, peak=0.25, cutoff=(260.0, 90.0),
                 seed=seed)]


def thunder_b(speed, at_pan, length, seed):
    return [dict(**STRIKE, cut=dict(offset=0.0, length=length * speed, fade=length * speed * 0.45, highpass=30),
                 speed=speed, at=0.0, pan=at_pan),
            dict(synth="rumble", at=0.05, gain=-8.0, length=length, low=36.0, peak=0.12, cutoff=(300.0, 80.0),
                 seed=seed)]


# subject: level (dB, the loudest 100 ms of the 200 Hz-5 kHz band), ceiling (dBFS), and per variant its
# layers (as sfx_r33.py: a recorded layer has a source, the cut, optional speed, approach, pan and
# `env` (rise, fall) s; a synthesized layer `synth` and its parameters; each enters at `at`, the
# others `gain` dB from the first); `loop` marks a seamless loop of that length; `levels` overrides
# the level for one variant.
SOUNDS = {
    "thunder-1": dict(level=-14.0, ceiling=-1.5, variants={
        "a": thunder_a(2.3, 6.5, -0.2, 3401), "b": thunder_b(1.0, -0.25, 7.0, 3411)}),
    "thunder-2": dict(level=-16.0, ceiling=-1.5, variants={
        "a": thunder_a(12.0, 3.6, 0.3, 3402), "b": thunder_b(0.86, 0.3, 6.0, 3412)}),
    "thunder-3": dict(level=-17.0, levels={"a": -19.0}, ceiling=-1.5, variants={
        "a": thunder_a(17.6, 3.6, 0.0, 3403), "b": thunder_b(0.74, 0.05, 5.5, 3413)}),
    "gust": dict(level=-19.0, ceiling=-4.0, variants={
        "a": [dict(synth="gust", at=0.0, length=3.6, rise=1.0, hold=2.0, seed=3421)],
        "b": [dict(**WIND, cut=dict(offset=14.6, length=3.6, fade=0.05, fadein=0.02, highpass=120),
                   env=(1.0, 0.6), pan_sweep=(-0.7, 0.5), at=0.0),
              dict(synth="rumble", at=0.0, gain=-10.0, length=3.6, low=45.0, peak=0.45, cutoff=(220.0, 160.0),
                   seed=3422)]}),
    "lamprey-latch": dict(level=-16.0, ceiling=-2.0, variants={
        "a": [dict(**SLIME, cut=dict(offset=0.0, length=0.44, fade=0.2, highpass=90), speed=0.85, at=0.16),
              dict(synth="suction", at=0.0, gain=-4.0, length=0.17, seed=3431),
              dict(**TEAR, cut=dict(offset=0.1, length=0.45, fade=0.25, fadein=0.03, highpass=150), speed=1.1,
                   at=0.22, gain=-7.0),
              dict(synth="thump", at=0.16, gain=-6.0, length=0.35, start=90.0, end=45.0, tau=0.09)],
        "b": [dict(**SPLAT, cut=dict(offset=0.0, length=0.6, fade=0.3, highpass=90), at=0.0),
              dict(**THUD, cut=dict(offset=0.0, length=0.5, fade=0.25, highpass=60), speed=0.9, at=0.0,
                   gain=-2.0),
              dict(**TEAR, cut=dict(offset=0.2, length=0.55, fade=0.3, fadein=0.03, highpass=150), speed=0.95,
                   at=0.1, gain=-6.0),
              dict(synth="thump", at=0.0, gain=-5.0, length=0.4, start=80.0, end=40.0, tau=0.1)]}),
    "lamprey-drain": dict(level=-22.0, ceiling=-6.0, loop=LOOP, variants={
        "a": [dict(synth="hum", at=0.0, seed=3441)],
        "b": [dict(**RAY, loop_cut=dict(start=1.0, speed=0.45, lowpass=3200, highpass=150, xfade=0.3), at=0.0),
              dict(synth="sub", at=0.0, gain=-6.0, freq=50.0, swells=2)]}),
    "lamprey-shake": dict(level=-13.5, ceiling=-2.0, variants={
        "a": [dict(**SPLAT, cut=dict(offset=0.0, length=0.5, fade=0.25, highpass=100), speed=1.1, at=0.0),
              dict(**SCREECH, cut=dict(offset=0.2, length=0.6, fade=0.35, fadein=0.02, highpass=300), speed=1.35,
                   at=0.05, gain=-3.0, pan_sweep=(0.0, -0.6)),
              dict(synth="whoosh", at=0.02, gain=-9.0, length=0.8, start=1800.0, peak=2600.0, end=600.0, rise=0.08,
                   fall=0.7, pan=(0.0, -0.7), seed=3451)]}),
    "sonde-chime": dict(level=-16.0, ceiling=-4.0, variants={
        "a": [dict(synth="chime", at=0.0, seed=3461)]}),
}


# =========================================================================== synthesized layers

def gust(p):
    """Wind: noise in two bands whose centres wander (a slow random flutter), a hollow howl (a narrow
    band near 700 Hz gliding with the gust), rising over `rise` s (quadratic), holding `hold` s with
    the flutter, falling; panned from the left (upwind) to the right."""
    rng = np.random.default_rng(p["seed"])
    n = int(p["length"] * SR)
    t = np.arange(n) / SR
    lfo = svf(noise(n, rng), np.full(n, 3.0), q=0.7, mode="lp")
    lfo = lfo / (np.max(np.abs(lfo)) + 1e-9)
    rise, hold = p["rise"], p["hold"]
    env = np.where(t < rise, (t / rise) ** 2, np.where(t < rise + hold, 1.0, np.clip(1 - (t - rise - hold) / (p["length"] - rise - hold), 0, 1) ** 1.5))
    swell = np.clip(t / rise, 0, 1)
    body = svf(noise(n, rng), 500 + 700 * swell + 250 * lfo, q=0.8, mode="bp")
    air = svf(noise(n, rng), 2200 + 900 * swell + 500 * lfo, q=1.2, mode="bp")
    howl = svf(noise(n, rng), 560 + 220 * swell + 90 * lfo, q=9.0, mode="bp")
    x = (body / (np.std(body) + 1e-9) + 0.45 * air / (np.std(air) + 1e-9) + 0.6 * howl / (np.std(howl) + 1e-9))
    x = x * env * (0.8 + 0.2 * lfo)
    pos = -0.7 + 1.2 * np.clip(t / (rise + hold), 0, 1)
    return np.array([x * np.cos((pos + 1) * np.pi / 4), x * np.sin((pos + 1) * np.pi / 4)])


def suction(p):
    """An inhaling rush into the bite: band noise sweeping 3.2 kHz -> 700 Hz, swelling to its end
    (cut off by the bite, no pop)."""
    rng = np.random.default_rng(p["seed"])
    n = int(p["length"] * SR)
    t = np.arange(n) / SR
    x = svf(noise(n, rng), ramp(n, 3200.0, 700.0, curve="exp"), q=2.0, mode="bp")
    x = x * (t / t[-1]) ** 1.5
    x[-int(0.006 * SR):] *= np.linspace(1, 0, int(0.006 * SR))
    return np.vstack([x, x])


def periodic(length):
    """A helper for seamless loops: a time axis over three periods; process, then keep the middle."""
    n = int(round(length * SR))
    return n, np.arange(3 * n) / SR


def hum(p):
    """The drain a, a seamless loop: a 55 Hz electric hum (saw partials, every frequency a whole number
    of cycles in the loop) through a low-pass that opens with each 0.4 s gulp (a fast swell, a slow
    fall), a faint 1.1 kHz whine, and sparse crackle: clicks of band noise placed once and repeated
    every period. Built over three periods, the middle kept."""
    rng = np.random.default_rng(p["seed"])
    n, t = periodic(LOOP)
    f0 = round(55.0 * LOOP) / LOOP
    saw = sum(np.sin(2 * np.pi * f0 * k * t) / k for k in range(1, 18))
    gulp = (t % 0.4) / 0.4
    shape = np.where(gulp < 0.18, gulp / 0.18, np.exp(-(gulp - 0.18) / 0.3))
    x = svf(saw, 180 + 900 * shape, q=2.2, mode="lp") * (0.55 + 0.45 * shape)
    fw = round(1100.0 * LOOP) / LOOP
    x = x / (np.std(x) + 1e-9) + 0.08 * np.sin(2 * np.pi * fw * t) * (0.6 + 0.4 * shape)
    crack = np.zeros(n)
    for _ in range(22):
        at = rng.integers(0, n - 400)
        m = int(rng.uniform(0.002, 0.007) * SR)
        burst = rng.standard_normal(m) * np.exp(-np.arange(m) / (m / 4)) * rng.uniform(0.6, 1.6)
        crack[at:at + m] += burst
    crack = np.tile(crack, 3)
    crack = svf(crack, np.full(3 * n, 2800.0), q=1.0, mode="bp")
    x = x + 0.9 * crack / (np.max(np.abs(crack)) + 1e-9) * np.max(np.abs(x)) * 0.5
    x = x[n:2 * n]
    return np.vstack([x, x])


def sub(p):
    """A sub hum at `freq` Hz (whole cycles in the loop) swelling `swells` times a loop."""
    n, t = periodic(LOOP)
    f = round(p["freq"] * LOOP) / LOOP
    x = (np.sin(2 * np.pi * f * t) + 0.3 * np.sin(4 * np.pi * f * t)) * (0.6 + 0.4 * np.sin(2 * np.pi * p["swells"] / LOOP * t))
    x = x[n:2 * n]
    return np.vstack([x, x])


def chime(p):
    """The sonde: three telemetry blips rising (2.4, 2.8, 3.2 kHz, 28 ms, soft square), then two bright
    bell tones (E6, then B6 90 ms later; inharmonic partials 1, 2.76, 5.4, 8.93 decaying fast to slow)."""
    n = int(0.95 * SR)
    x = np.zeros(n)
    for i, f in enumerate((2400.0, 2800.0, 3200.0)):
        a, m = int((0.0 + i * 0.04) * SR), int(0.028 * SR)
        tt = np.arange(m) / SR
        blip = np.tanh(3 * np.sin(2 * np.pi * f * tt)) * np.sin(np.pi * tt / tt[-1]) ** 0.5
        x[a:a + m] += 0.22 * blip
    for at, f in ((0.13, 1318.5), (0.22, 1975.5)):
        a = int(at * SR)
        m = n - a
        tt = np.arange(m) / SR
        bell = sum(g * np.sin(2 * np.pi * f * r * tt) * np.exp(-tt / tau)
                   for r, g, tau in ((1.0, 1.0, 0.35), (2.76, 0.45, 0.16), (5.4, 0.22, 0.07), (8.93, 0.12, 0.035)))
        bell[:int(0.002 * SR)] *= np.linspace(0, 1, int(0.002 * SR))
        x[a:] += 0.6 * bell
    x[-int(0.03 * SR):] *= np.linspace(1, 0, int(0.03 * SR))
    st = np.vstack([x, x])
    st[0, :] = np.concatenate([np.zeros(int(0.004 * SR)), x[:-int(0.004 * SR)]])   # a little width
    return st


def whoosh(p):
    from sfx_r32 import whoosh as w32
    return w32(p)


SYNTHS = {"gust": gust, "suction": suction, "hum": hum, "sub": sub, "chime": chime, "rumble": rumble,
          "thump": thump, "whoosh": whoosh}


# =========================================================================== treatment

def loop_layer(layer, path):
    """A recorded layer cut as a seamless loop of LOOP s: slowed, filtered, then cross-faded."""
    c = layer["loop_cut"]
    x = np.clip(decode(path), -1, 1)
    x = x[:, int(c["start"] * SR):]
    x = resample(x, c["speed"])
    x = np.array([svf(svf(ch, c["highpass"], mode="hp"), c["highpass"], mode="hp") for ch in x])
    x = np.array([svf(svf(ch, c["lowpass"]), c["lowpass"]) for ch in x])
    x = x[:, int(0.2 * SR):]                     # past the filters' settling
    x = make_loop(x, 0.0, LOOP, c["xfade"])
    return x - x.mean(axis=1, keepdims=True)


def enveloped(st, rise, fall):
    n = st.shape[1]
    t = np.arange(n) / SR
    env = np.minimum(np.clip(t / rise, 0, 1) ** 2, np.clip((t[-1] - t) / fall, 0, 1) ** 1.5)
    return st * env


def swept(st, a, b):
    mono = st.mean(axis=0)
    pos = np.linspace(a, b, len(mono))
    return np.array([mono * np.cos((pos + 1) * np.pi / 4), mono * np.sin((pos + 1) * np.pi / 4)])


def level_loop(st, band_db, ceiling_db):
    st = st * undb(band_db - short_term_band_db(st))
    peak = 20 * np.log10(np.max(np.abs(st)) + 1e-12)
    return st * undb(min(0.0, ceiling_db - peak))


def recorded_layers(subject, v):
    return [layer for layer in SOUNDS[subject]["variants"][v] if "page" in layer]


def treat(subject, v):
    sound = SOUNDS[subject]

    def run(*paths):
        files = iter(paths)
        parts = []
        for layer in sound["variants"][v]:
            if "page" in layer:
                path = next(files)
                st = loop_layer(layer, path) if "loop_cut" in layer else recorded(layer, path)
                if "env" in layer:
                    st = enveloped(st, *layer["env"])
                if "pan" in layer:
                    st = pan(st.mean(axis=0), layer["pan"])
                if "pan_sweep" in layer:
                    st = swept(st, *layer["pan_sweep"])
            else:
                st = SYNTHS[layer["synth"]](layer)
            if parts:
                st = st * undb(loudest_db(parts[0][1]) + layer.get("gain", 0.0) - loudest_db(st))
            parts.append((int(layer["at"] * SR), st))
        out = np.zeros((2, max(at + st.shape[1] for at, st in parts)))
        for at, st in parts:
            out[:, at:at + st.shape[1]] += st
        if "loop" in sound:
            return level_loop(out[:, :int(round(sound["loop"] * SR))], sound["level"], sound["ceiling"])
        return level(out, sound.get("levels", {}).get(v, sound["level"]), sound["ceiling"])
    return run


def seam(path):
    """A loop's seam: the jump between its last and first sample against the typical step."""
    x = decode(path).mean(axis=0)
    step = np.median(np.abs(np.diff(x))) + 1e-12
    return abs(x[0] - x[-1]) / step


def build(subject, v, preview_cache):
    layers = recorded_layers(subject, v)
    with tempfile.TemporaryDirectory() as tmp:
        found = [(layer["author"], *source(layer, preview_cache, tmp)) for layer in layers]
        st = treat(subject, v)(*(path for _, path, _ in found))
    used = "; ".join(dict.fromkeys(f"{author} from {how}" for author, _, how in found))
    synths = [layer["synth"] for layer in SOUNDS[subject]["variants"][v] if "page" not in layer]
    if synths:
        used += ("; " if used else "") + ", ".join(dict.fromkeys(synths)) + " synthesized"
    name = f"{subject}-r34-{v}"
    tags = {"COMMENT": f"{SCRIPT} {name}: {used}"}
    if "loop" in SOUNDS[subject]:
        tags.update(LOOPSTART=0, LOOPLENGTH=st.shape[1])
    out = write_ogg(OUT / f"{name}.ogg", st, max_peak_db=SOUNDS[subject]["ceiling"], tags=tags)
    report(out, used)
    if "loop" in SOUNDS[subject]:
        print(f"  loop seam {seam(out):.1f} x the median step")


def build_bed(preview_cache):
    """The rain-only storm bed: the original after its thunder, high-passed, looped, levelled."""
    with tempfile.TemporaryDirectory() as tmp:
        path, how = source(STORM, preview_cache, tmp)
        x = decode(path)
    x = np.array([svf(svf(ch, 90.0, mode="hp"), 90.0, mode="hp") for ch in x])
    x = make_loop(x, 21.0, 10.0, 1.5)
    x -= x.mean(axis=1, keepdims=True)
    x = normalize(x, dict(band_rms=-36.0, peak=-10.0))
    name = "ambience-storm-rain-r34-a"
    out = write_ogg(OUT / f"{name}.ogg", x, max_peak_db=-10.0,
                    tags={"COMMENT": f"{SCRIPT} {name}: FlatHill from {how}", "LOOPSTART": 0,
                          "LOOPLENGTH": x.shape[1]})
    report(out, f"FlatHill from {how}")
    print(f"  band RMS {band_rms_db(decode(out)):.1f} dB, loop seam {seam(out):.1f} x the median step")


def main(argv):
    preview_cache = Path(os.environ.get("TMPDIR", tempfile.gettempdir())) / "conceptgame-sfx-cache"
    preview_cache.mkdir(parents=True, exist_ok=True)
    for subject, sound in SOUNDS.items():
        for v in sound["variants"]:
            if argv and subject not in argv and f"{subject}-r34-{v}" not in argv:
                continue
            build(subject, v, preview_cache)
    if not argv or "ambience-storm-rain" in argv or "ambience-storm-rain-r34-a" in argv:
        build_bed(preview_cache)


if __name__ == "__main__":
    main(sys.argv[1:])
