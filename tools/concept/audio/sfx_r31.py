#!/usr/bin/env python3
"""Concept round 31 (M5 part C, Level 09 "Arcology Fall"): the arcology's collapse and the Ravager's
pounce, an a/b pair each, recorded (CC0 / CC-BY, Freesound) with a synthesized whoosh under the
pounces.

Outputs (design/audio/sfx/concept/, the unchosen one into concept/rejected/; OGG Vorbis q6,
44.1 kHz stereo):

  collapse-r31-<v>.ogg         the Ndidi Arcology coming down (the simulation's collapse event):
                               a low rumble that swells under the 1.5 s shadow (the warning), then
                               a long crash from 1.5 s as the tower falls across the ground layer
                               (3 s) and a debris tail; about 5.5 s
                               a: cracking stone and earth swelling into a heavy, tumbling crash
                                  (a house collapse from a broadcaster's effects archive)
                               b: a steel frame rattling and groaning swelling into a deep, rolling
                                  cave-in with a long dust tail
  ravager-pounce-r31-<v>.ogg   a Ravager taking off on its pounce (0.75 s leap): a short snarl
                               and lunge over a body-sized whoosh; about 0.7 s
                               a: a rasping, dog-like attack snarl, 10 % slower, a quick swish
                               b: a beast's snapping attack roar, 20 % faster (a hound, not a
                                  dragon), a lower, heavier whoosh

Sources (the `SOUNDS` table; every one in CREDITS.md): each recorded layer is cut from the
Freesound **original** when it is cached (tools/concept/audio/freesound_fetch.py --download,
~/.cache/terran-vanguard/freesound/<id>_<name>.<type>; clipped at full scale first, as
sfx_r25.py does), else from the public HQ preview (with a note). The cut is import_sfx.py's
`process` (leading silence trimmed, offset, length, fades, filters, peak -1 dBFS), then an optional
speed change (resampled: lower and longer below 1).

The collapse: the rumble swells from -24 dB to full over the 1.5 s warning (a linear ramp in dB),
holds 0.3 s under the crash and fades out over 1.2 s; it sits `rumble_db` under the crash (the
loudest 100 ms of each layer, full band). The crash starts at 1.5 s. The pounce: the snarl starts
at once; the whoosh (band-passed noise, its centre sweeping up and back down, 0.12 s rise and a
0.35 s fall, panned across) sits `whoosh_db` under the snarl's loudest 100 ms.

Levels, set against the existing sounds measured the same way in assets/sfx/. The collapse: its
integrated loudness at -15 LUFS, ceiling -1.5 dBFS (explosion-huge-r04-a -15.1 LUFS,
explosion-r02-d "building collapse" -14.4, explosion-large-r03-a -11.2): a long, sub-heavy sound,
so the loudest 100 ms of its 200 Hz-5 kHz band (the round-24 measure) would leave b 5 LU louder than
a at the same band level; the script prints the band level each reaches. A pounce: the loudest
100 ms of the 200 Hz-5 kHz band (sfx_r24.py's `level`, limited at the ceiling) at -22 dB, ceiling
-6 dBFS (the Vrell screeches -24 / -27, the carrier launch -25, the mantis sweep -25: the pounce is
the Ravager's cue and the one ground attack that hurts, a little louder than a screech; a pack of
3-5 can pounce close together, hence the low ceiling).

Nobody listened to these: they were picked by the sources' descriptions, ratings and tags and by
their envelopes, band balance and levels (printed by this script for every file it writes).

Chosen (round 31, 2026-10-07): collapse a; both pounces, a and b (the game picks one at random per
pounce). The unchosen collapse b goes to concept/rejected/. PRODUCTION holds the chosen sounds'
treatment for tools/art/sfx_originals.py, which applies it to the Freesound originals (the
collapse's two: crash and rumble) and writes them to assets/sfx/ under the concept file's name
with a SOURCE comment (the same cut as the concept file, which was already cut from the originals).
The crash sits at WARNING (1.5 s) in the concept file. The production file (PRODUCTION, after round
31's collapse look c, 2026-10-07) has it at IMPACT (3.0 s: the 1.5 s lean, then the 1.5 s drop): the
game starts the sound with the collapse's warning, so the crash lands at the impact; the rumble swells
over those 3.0 s (cut 1.5 s longer, for its hold and fade). FlightSounds' crash offset follows IMPACT.

Usage: python3 tools/concept/audio/sfx_r31.py [subject-or-file ...]
       e.g. collapse (both variants) or ravager-pounce-r31-a. Deterministic.
"""
import os
import sys
import tempfile
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
from import_sfx import band_rms_db  # noqa: E402
from import_sfx import process  # noqa: E402
from sfx_r24 import level, resample, short_term_band_db  # noqa: E402
from sfx_r25 import fs, source  # noqa: E402
from synth import SR, db, decode, limiter, lufs, noise, svf, undb, write_ogg  # noqa: E402

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "design" / "audio" / "sfx" / "concept"
SCRIPT = "tools/concept/audio/sfx_r31.py"
CC0 = "CC0 1.0"
BY4 = "CC-BY 4.0"

WARNING = 1.5  # s: the arcology's shadow before the tower falls (Level 09's collapse set piece)
IMPACT = 3.0   # s: production (look c): the lean (1.5 s) and the drop (1.5 s), the crash at the impact

# subject: its level (loudest 100 ms of the 200 Hz-5 kHz band, dB), ceiling (dBFS) and the two
# variants. A collapse variant has a `rumble` and a `crash` layer (source, title, author, licence,
# the cut as import_sfx.py's process keys, optional speed) and `rumble_db`; a pounce variant has a
# `snarl` layer, its speed and the synthesized whoosh (`whoosh`: centre start/peak/end Hz, level).
SOUNDS = {
    "collapse": dict(lufs=-15.0, ceiling=-1.5, variants={
        "a": dict(
            # an earthquake cracking open soil and stone: a deep rumble with stony cracks in it
            rumble=dict(**fs("uagadugu", 222521, 222, 255863),
                        title="Cracking Earthquake (cracking soil, cracking stone)", author="uagadugu",
                        licence=CC0, cut=dict(offset=0.5, length=3.0, fade=0.05, highpass=25)),
            # a big crash, a house tumbling down (Yle's effects archive, 1980s tape): its first 4 s
            crash=dict(**fs("YleArkisto", 342891, 342, 4415905),
                       title="Sortuminen, talo sortuu / Big crash, a house tumbling down, collapse, mix",
                       author="YleArkisto", licence=BY4,
                       cut=dict(offset=0.0, length=4.0, fade=1.5, fadein=0.02, highpass=25)),
            rumble_db=-6.0),
        "b": dict(
            # a radiator shaken for earthquake sounds: a metal frame rattling and groaning
            rumble=dict(**fs("RutgerMuller", 51097, 51, 179538),
                        title="Sounds For Earthquakes - Radiator Metal Rumbling.wav", author="RutgerMuller",
                        licence=CC0, cut=dict(offset=0.3, length=3.0, fade=0.05, highpass=40), speed=0.8),
            # a tunnel explosion or collapse: 3 s of heavy, rolling roar and a long decaying tail
            crash=dict(**fs("tec_studio", 703070, 703, 1431924), title="Explosion or Collapse.wav",
                       author="tec_studio", licence=CC0,
                       cut=dict(offset=0.0, length=4.2, fade=1.8, fadein=0.03, highpass=45)),
            rumble_db=-4.0),
    }),
    "ravager-pounce": dict(level=-22.0, ceiling=-6.0, variants={
        "a": dict(
            # a goblin monster's attack snarl (a remix of a dog's snarl): its first lunge
            snarl=dict(**fs("qubodup", 442815, 442, 71257), title="Goblin Snarl", author="qubodup",
                       licence=CC0, cut=dict(offset=0.0, length=0.65, fade=0.3, highpass=90), speed=0.9),
            whoosh=dict(start=500.0, peak=2600.0, end=700.0, level=-9.0)),
        "b": dict(
            # a huge monster snarling and roaring: its second gesture, the attack, played faster
            snarl=dict(**fs("Breviceps", 466830, 466, 9159316), title="Dragon: Snarl, Roar + Attack",
                       author="Breviceps", licence=CC0,
                       cut=dict(offset=1.55, length=0.85, fade=0.35, fadein=0.01, highpass=90), speed=1.2),
            whoosh=dict(start=250.0, peak=1500.0, end=350.0, level=-7.0)),
    }),
}


def loudest_db(st, window=0.1):
    """The loudest `window` s of the full band, as RMS dB."""
    n = int(window * SR)
    hop = n // 4
    mono = st.mean(axis=0)
    return max(db(np.sqrt(np.mean(mono[i:i + n] ** 2)) + 1e-12)
               for i in range(0, max(1, len(mono) - n + 1), hop))


def cut(layer, path):
    """A recorded layer cut from a (full-scale clipped) source file: the original or the preview."""
    st = process(dict(layer["cut"], peak=-1.0), path)
    if layer.get("speed", 1.0) != 1.0:
        st = resample(st, layer["speed"])
    return st


def collapse(v, crash_path, rumble_path, at=WARNING):
    """A collapse variant from its crash and rumble source files, before levelling, the crash at
    ``at`` s (the concept's WARNING, production's IMPACT); the rumble cut ``at`` + 1.5 s long."""
    var = SOUNDS["collapse"]["variants"][v]
    rumble_layer = dict(var["rumble"], cut=dict(var["rumble"]["cut"], length=at + 1.5))
    rumble = cut(rumble_layer, rumble_path)
    crash = cut(var["crash"], crash_path)
    # the swell: -24 dB to full until the crash, held 0.3 s, faded out over 1.2 s
    n = rumble.shape[1]
    t = np.arange(n) / SR
    gain_db = np.where(t < at, -24.0 * (1 - t / at), 0.0)
    env = undb(gain_db)
    hold_end = at + 0.3
    tail = (t >= hold_end)
    env[tail] *= np.clip(0.5 + 0.5 * np.cos(np.pi * (t[tail] - hold_end) / 1.2), 0, 1) * (t[tail] < hold_end + 1.2)
    rumble = rumble * env
    rumble *= undb(loudest_db(crash) + var["rumble_db"] - loudest_db(rumble))
    start = int(at * SR)
    out = np.zeros((2, max(n, start + crash.shape[1])))
    out[:, :n] += rumble
    out[:, start:start + crash.shape[1]] += crash
    return out


def whoosh(w, n, seed):
    """Band-passed noise, its centre sweeping start -> peak -> end; 0.12 s rise, 0.35 s fall, panned across."""
    rng = np.random.default_rng(seed)
    t = np.arange(n) / SR
    rise, fall = 0.12, 0.35
    centre = np.where(t < rise, w["start"] * (w["peak"] / w["start"]) ** (t / rise),
                      w["peak"] * (w["end"] / w["peak"]) ** np.clip((t - rise) / fall, 0, 1))
    x = svf(noise(n, rng), centre, q=1.4, mode="bp")
    env = np.where(t < rise, (t / rise) ** 2, np.clip(1 - (t - rise) / fall, 0, 1) ** 2)
    x = x * env
    p = np.clip(t / (rise + fall), 0, 1) * 0.6 - 0.3  # -0.3 (left) to +0.3 (right)
    return np.array([x * np.cos((p + 1) * np.pi / 4), x * np.sin((p + 1) * np.pi / 4)])


def pounce(v, snarl_path):
    """A pounce variant from its snarl's source file, over the synthesized whoosh, before levelling."""
    var = SOUNDS["ravager-pounce"]["variants"][v]
    snarl = cut(var["snarl"], snarl_path)
    n = snarl.shape[1]
    w = whoosh(var["whoosh"], n, seed=31 + ord(v))
    w *= undb(loudest_db(snarl) + var["whoosh"]["level"] - loudest_db(w))
    return snarl + w


def level_lufs(st, target, ceiling_db):
    """Integrated loudness at `target` LUFS, limited at the ceiling (for a long sound whose loudest
    100 ms says little about how loud it plays)."""
    st = st - st.mean(axis=1, keepdims=True)
    for _ in range(4):  # limiting lowers the loudness a little; make it up
        st = limiter(st * undb(target - lufs(st)[0]), ceiling_db)
    return st


# A variant's recorded layers, in the order its treatment takes their source files.
LAYERS = {"collapse": ("crash", "rumble"), "ravager-pounce": ("snarl",)}


def treat(subject, v, at=WARNING):
    """A variant's treatment of its (full-scale clipped) source files, one per layer: cut, mix, level
    (a collapse's crash at ``at`` s)."""
    sound = SOUNDS[subject]

    def run(*paths):
        st = collapse(v, *paths, at=at) if subject == "collapse" else pounce(v, *paths)
        if "lufs" in sound:
            return level_lufs(st, sound["lufs"], sound["ceiling"])
        return level(st, sound["level"], sound["ceiling"])
    return run


CHOSEN = {"collapse": ("a",), "ravager-pounce": ("a", "b")}

# The chosen sounds for tools/art/sfx_originals.py (its DERIVED sounds): the pages of their
# originals in the order the build takes them, the ceiling and the build.
PRODUCTION = {f"{subject}-r31-{v}": dict(
    page=SOUNDS[subject]["variants"][v][LAYERS[subject][0]]["page"],
    pages=[SOUNDS[subject]["variants"][v][layer]["page"] for layer in LAYERS[subject]],
    peak=SOUNDS[subject]["ceiling"], build=treat(subject, v, at=IMPACT))
    for subject, variants in CHOSEN.items() for v in variants}


def build(subject, v, preview_cache):
    var = SOUNDS[subject]["variants"][v]
    with tempfile.TemporaryDirectory() as tmp:
        found = [(layer, *source(var[layer], preview_cache, tmp)) for layer in LAYERS[subject]]
        st = treat(subject, v)(*(path for _, path, _ in found))
    used = "; ".join(f"{layer} from {how}" for layer, _, how in found)
    if subject == "ravager-pounce":
        used += "; whoosh synthesized"
    name = f"{subject}-r31-{v}"
    out_dir = OUT if v in CHOSEN[subject] else OUT / "rejected"
    tags = {"COMMENT": f"{SCRIPT} {name}: {used}"}
    out = write_ogg(out_dir / f"{name}.ogg", st, max_peak_db=SOUNDS[subject]["ceiling"], tags=tags)
    report(out, used)


def report(path, used):
    """The checks we can make without listening: length, peak, levels, DC, the edges, the envelope."""
    x = decode(path)
    edge = int(0.005 * SR)
    n = int(0.1 * SR)
    env = [db(np.sqrt(np.mean(x[:, k:k + n] ** 2)) + 1e-9) for k in range(0, x.shape[1], n)]
    mono = x.mean(axis=0)
    spec = np.abs(np.fft.rfft(mono)) ** 2
    freqs = np.fft.rfftfreq(len(mono), 1 / SR)
    total = spec.sum() + 1e-20
    low, mid = spec[freqs < 200].sum() / total, spec[(freqs >= 200) & (freqs < 5000)].sum() / total
    print(f"wrote {path.relative_to(ROOT)} ({used})\n"
          f"  {x.shape[1] / SR:.2f} s, peak {db(np.abs(x).max()):.1f} dBFS, loudest 100 ms band "
          f"{short_term_band_db(x):.1f} dB, band RMS {band_rms_db(x):.1f} dB, {lufs(x)[0]:.1f} LUFS, "
          f"DC {np.mean(x):+.5f}, "
          f"edges {db(np.abs(x[:, :edge]).max()):.0f}/{db(np.abs(x[:, -edge:]).max()):.0f} dBFS, "
          f"energy <200 Hz {low:.0%}, 200 Hz-5 kHz {mid:.0%}\n"
          f"  envelope (100 ms RMS, dBFS): {' '.join(f'{e:.0f}' for e in env)}")


def main(argv):
    preview_cache = Path(os.environ.get("TMPDIR", tempfile.gettempdir())) / "conceptgame-sfx-cache"
    preview_cache.mkdir(parents=True, exist_ok=True)
    for subject, sound in SOUNDS.items():
        for v in sound["variants"]:
            if argv and subject not in argv and f"{subject}-r31-{v}" not in argv:
                continue
            build(subject, v, preview_cache)


if __name__ == "__main__":
    main(sys.argv[1:])
