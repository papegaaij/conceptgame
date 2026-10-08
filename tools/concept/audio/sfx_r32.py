#!/usr/bin/env python3
"""Concept round 32 (M5 part D, Level 10 "Evacuation Corridor"): the Wraith's decloak, the Mote
Swarm's whoosh and the lance's strike, an a/b pair each (user decision D10 = a), recorded (CC0 /
CC-BY, Freesound) and synthesized layers mixed.

Outputs (design/audio/sfx/concept/, the unchosen variants in concept/rejected/; OGG Vorbis q6,
44.1 kHz stereo):

  wraith-decloak-r32-<v>.ogg  a Wraith dropping its cloak (the simulation's decloak event, as its
                              0.4 s violet flash starts): a short shimmer-crack; about 0.6 s
                              a: a synthesized violet shimmer (a cluster of high partials fluttering
                                 like refraction, swelling over 0.2 s) breaking into an ice crack at
                                 0.18 s, the shimmer ringing out under it
                              b: a sci-fi rip with a rough metallic texture over a velvet cloak
                                 swirled faster (the membrane snapping open), no synthesis
  mote-swarm-r32-<v>.ogg      a Mote Swarm entering, and again on its loop-back (one-shot, played
                              on both events): a rushing flutter of many small wings; about 1.7 s
                              a: a pigeon flock flying off, 40 % faster (smaller wings, a higher
                                 flutter) over a synthesized whoosh sweeping across: birdlike, "they
                                 steer like starlings"
                              b: a cicada's wing flap layered five times (offsets, speeds, pans
                                 spread) into an insect swarm's buzzing chitter that swells and
                                 fades over a synthesized whoosh: alien, insect-like
  lance-r32-<v>.ogg           the lance striking Lifeline Three (the scripted loss, t=118): a thin
                              descending whine into a sharp impact; the impact at LEAD (1.2 s),
                              so the game starts the sound LEAD before the lance hits (in the
                              glow, t 116-118); about 3 s
                              a: a synthesized whine (a thin sine falling 3.4 kHz -> 700 Hz with
                                 a dissonant partial and a fast vibrato: something alien and
                                 energetic) into a close thunder strike's crack and its roll
                              b: a firework's whistler, 25 % faster (falling about 3.0 -> 2.4 kHz:
                                 a projectile), into a piercing stab and a synthesized sub thump
                                 (a thorn spike punching through a hull)

Sources (the `SOUNDS` table; every one in CREDITS.md): each recorded layer is cut from the
Freesound **original** when it is cached (tools/concept/audio/freesound_fetch.py --download,
~/.cache/terran-vanguard/freesound/<id>_<name>.<type>; clipped at full scale first, as
sfx_r25.py does), else from the public HQ preview (with a note). The cut is import_sfx.py's
`process` (leading silence trimmed, offset, length, fades, filters, peak -1 dBFS), then an optional
speed change (resampled: lower and longer below 1). Each layer enters at `at` s and sits `gain` dB
from the variant's first (key) layer, both measured as the loudest 100 ms of the full band.

Levels, set against round 24/25/31's conventions (sfx_r24.py's `level`: the loudest 100 ms of the
200 Hz-5 kHz band, limited at a ceiling):
  decloak -22 dB, ceiling -6 dBFS: the Wraith's only cue, as the Ravager's pounce (-22 / -6); a
          rear ambush decloaks 2-4 Wraiths close together, hence the low ceiling.
  swarm   -24 dB, ceiling -6 dBFS: an entry cue as the carrier launch (-24 / -6) and a little above
          the Vrell screeches (-24 / -27); a swarm's 20 motes die on the `tiny` rung under it.
  lance   -17 dB, ceiling -1.5 dBFS: the level's one scripted story beat, as loud as the secret's
          cable snap (-17 / -1.5) and under the big explosions; the music ducks -6 dB under it.

Nobody listened to these: they were picked by the sources' descriptions, ratings and tags and by
their envelopes, band balance and levels (printed by this script for every file it writes).

Chosen (round 32, 2026-10-08): decloak b (its cloak layer CC-BY 4.0, F.M.Audio, on the credits
roll), swarm b, lance a. The unchosen decloak a, swarm a and lance b go to concept/rejected/.
PRODUCTION holds the chosen sounds' treatment for tools/art/sfx_originals.py, which applies it to the
Freesound originals and writes them to assets/sfx/ under the concept file's name with a SOURCE
comment (the same cut as the concept file, which was already cut from the originals). The lance's
impact stays at LEAD (1.2 s), so the game starts it at t 116.8 for the hit at 118.

Usage: python3 tools/concept/audio/sfx_r32.py [subject-or-file ...]
       e.g. lance (both variants) or mote-swarm-r32-b. Deterministic.
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
from synth import SR, db, decode, exp_decay, lufs, noise, pan, ramp, sine, svf, undb, write_ogg  # noqa: E402

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "design" / "audio" / "sfx" / "concept"
SCRIPT = "tools/concept/audio/sfx_r32.py"
CC0 = "CC0 1.0"
BY4 = "CC-BY 4.0"

LEAD = 1.2  # s: the lance's whine before its impact (the game starts the sound LEAD before the hit)

# subject: its level (loudest 100 ms of the 200 Hz-5 kHz band, dB), ceiling (dBFS) and the two
# variants, each a list of layers. A recorded layer has a source (fs(): page and preview), title,
# author, licence, the cut (import_sfx.py's process keys), optional speed and pan (-1 left .. +1
# right); a synthesized layer has `synth` (the generator's name) and its parameters. Every layer
# enters at `at` s; the first is the key layer, the others sit `gain` dB from its loudest 100 ms.
SOUNDS = {
    "wraith-decloak": dict(level=-22.0, ceiling=-6.0, variants={
        "a": [
            # an ice sheet broken: two sharp, glassy cracks 0.2 s apart and a crunch
            dict(**fs("InMotionAudio", 719973, 719, 14083205),
                 title="ICEBrk_Break04_InMotionAudio_FREESampleSunday", author="InMotionAudio", licence=CC0,
                 cut=dict(offset=0.0, length=0.5, fade=0.2, highpass=150), at=0.18),
            dict(synth="shimmer", at=0.0, gain=-6.0, length=0.62, swell=0.18, low=1800.0, high=5200.0,
                 partials=7, flutter=(22.0, 38.0), glide=0.85, seed=321),
        ],
        "b": [
            # a quick sci-fi rip with a rough metallic texture
            dict(**fs("GameAudio", 220164, 220, 4100837), title="Spacey Teleport Rip", author="GameAudio",
                 licence=CC0, cut=dict(offset=0.0, length=0.6, fade=0.25, highpass=120), at=0.0),
            # a velvet cloak swirled: fabric on fabric swooshing; its swish, 30 % faster (a membrane)
            dict(**fs("F.M.Audio", 556759, 556, 7805242), title="Swirling Velvet Cloak 2.wav",
                 author="F.M.Audio", licence=BY4,
                 cut=dict(offset=0.35, length=0.6, fade=0.25, fadein=0.03, highpass=200), speed=1.3,
                 at=0.0, gain=-5.0),
        ],
    }),
    "mote-swarm": dict(level=-24.0, ceiling=-6.0, variants={
        "a": [
            # a pigeon flock flying off: a swelling rush of wing flaps; 40 % faster (small wings)
            dict(**fs("TRP", 616623, 616, 97550),
                 title="121003 Pigeon flock fly away, wing flaps, Toronto.wav", author="TRP", licence=CC0,
                 cut=dict(offset=0.8, length=2.4, fade=0.9, fadein=0.15, highpass=180), speed=1.4, at=0.0),
            dict(synth="whoosh", at=0.0, gain=-9.0, length=1.6, start=600.0, peak=2400.0, end=900.0,
                 rise=0.45, fall=1.0, pan=(-0.5, 0.5), seed=322),
        ],
        "b": [
            # a cicada flapping its wings, very close: a fast, dry, buzzing flutter; five of them
            dict(**fs("kalhan", 482733, 482, 2161107), title="Insect Superfast Wing Flap", author="kalhan",
                 licence=CC0, cut=dict(offset=0.0, length=21.0, fade=0.05, highpass=250, lowpass=9000),
                 synth="swarm", at=0.0, length=1.7, rise=0.35, fall=1.1,
                 copies=[(2.0, 1.15, -0.6), (5.5, 0.95, 0.4), (9.0, 1.3, -0.2), (12.5, 1.05, 0.7),
                         (16.0, 0.85, -0.8)]),
            dict(synth="whoosh", at=0.0, gain=-8.0, length=1.6, start=400.0, peak=1800.0, end=600.0,
                 rise=0.4, fall=1.1, pan=(0.5, -0.5), seed=323),
        ],
    }),
    "lance": dict(level=-17.0, ceiling=-1.5, variants={
        "a": [
            # a close thunder strike: a sharp crack and a long roll; the strike and 2.6 s of roll
            dict(**fs("loganzsound", 840628, 840, 16682330), title="Closeup Thunder Strike 01",
                 author="loganzsound", licence=CC0,
                 cut=dict(offset=0.47, length=2.6, fade=1.4, fadein=0.005, highpass=40), at=LEAD),
            dict(synth="whine", at=0.0, gain=-7.0, length=LEAD, start=3400.0, end=700.0, ratio=1.41,
                 vibrato=(11.0, 0.012), approach=-20.0, seed=324),
        ],
        "b": [
            # a piercing anime-style stab (an arrow or dart hitting): its third, loudest take
            dict(**fs("Breviceps", 464839, 464, 9159316),
                 title="Anime Sound Effect - Piercing impact / Stabbing", author="Breviceps", licence=CC0,
                 cut=dict(offset=1.62, length=0.65, fade=0.3, fadein=0.003, highpass=90), at=LEAD),
            # a firework's whistler: a steady, thin whistle falling 2.6 -> 1.9 kHz; 25 % faster
            dict(**fs("magnuswaker", 555998, 555, 11537497), title="Whistling Firework", author="magnuswaker",
                 licence=CC0, cut=dict(offset=0.3, length=1.5, fade=0.02, fadein=0.6, highpass=400),
                 speed=1.25, at=0.0, gain=-7.0, approach=-14.0),
            dict(synth="thump", at=LEAD, gain=-4.0, length=0.6, start=95.0, end=38.0, tau=0.16),
        ],
    }),
}


def loudest_db(st, window=0.1):
    """The loudest `window` s of the full band, as RMS dB."""
    n = int(window * SR)
    hop = n // 4
    mono = st.mean(axis=0)
    return max(db(np.sqrt(np.mean(mono[i:i + n] ** 2)) + 1e-12)
               for i in range(0, max(1, len(mono) - n + 1), hop))


def recorded(layer, path):
    """A recorded layer cut from a (full-scale clipped) source file: the original or the preview."""
    st = process(dict(layer["cut"], peak=-1.0), path)
    if layer.get("speed", 1.0) != 1.0:
        st = resample(st, layer["speed"])
    if "approach" in layer:  # an approach: from `approach` dB to full over the layer (linear in dB)
        st = st * undb(layer["approach"] * (1 - np.linspace(0, 1, st.shape[1])))
    return st


def shimmer(p):
    """A violet shimmer: high partials (log-spaced low..high, jittered) each fluttering at its own
    rate like light through a refracting membrane, gliding down to `glide` x, swelling over `swell`
    s and ringing out; spread across the stereo field."""
    rng = np.random.default_rng(p["seed"])
    n = int(p["length"] * SR)
    t = np.arange(n) / SR
    out = np.zeros((2, n))
    freqs = np.geomspace(p["low"], p["high"], p["partials"]) * rng.uniform(0.97, 1.03, p["partials"])
    for k, f in enumerate(freqs):
        fr = f * ramp(n, 1.0, p["glide"], curve="exp")
        rate = rng.uniform(*p["flutter"])
        trem = 0.55 + 0.45 * np.sin(2 * np.pi * rate * t + rng.uniform(0, 2 * np.pi))
        out += pan(sine(fr, n, start=rng.uniform()) * trem / (1 + 0.15 * k), rng.uniform(-0.6, 0.6))
    swell = np.clip(t / p["swell"], 0, 1) ** 2
    ring = np.where(t < p["swell"], 1.0, np.exp(-(t - p["swell"]) / 0.14))
    out *= swell * ring
    out[:, -int(0.01 * SR):] *= np.linspace(1, 0, int(0.01 * SR))
    return out


def whoosh(p):
    """Band-passed noise, its centre sweeping start -> peak -> end over `rise` and `fall` s, panned
    from pan[0] to pan[1] (sfx_r31.py's whoosh with its timing as parameters)."""
    rng = np.random.default_rng(p["seed"])
    n = int(p["length"] * SR)
    t = np.arange(n) / SR
    rise, fall = p["rise"], p["fall"]
    centre = np.where(t < rise, p["start"] * (p["peak"] / p["start"]) ** (t / rise),
                      p["peak"] * (p["end"] / p["peak"]) ** np.clip((t - rise) / fall, 0, 1))
    x = svf(noise(n, rng), centre, q=1.4, mode="bp")
    x = x * np.where(t < rise, (t / rise) ** 2, np.clip(1 - (t - rise) / fall, 0, 1) ** 2)
    pos = p["pan"][0] + (p["pan"][1] - p["pan"][0]) * np.clip(t / (rise + fall), 0, 1)
    return np.array([x * np.cos((pos + 1) * np.pi / 4), x * np.sin((pos + 1) * np.pi / 4)])


def whine(p):
    """A thin falling whine: a sine start -> end (exponential), a dissonant partial at `ratio` x, a
    fast vibrato and a breath of band noise around it, swelling from `approach` dB to full (a thing
    falling closer); centred."""
    rng = np.random.default_rng(p["seed"])
    n = int(p["length"] * SR)
    t = np.arange(n) / SR
    rate, depth = p["vibrato"]
    f = ramp(n, p["start"], p["end"], curve="exp") * (1 + depth * np.sin(2 * np.pi * rate * t))
    x = sine(f, n) + 0.35 * sine(f * p["ratio"], n) + 0.25 * svf(noise(n, rng), f * 1.2, q=6.0, mode="bp")
    x *= undb(p["approach"] * (1 - t / p["length"]) ** 1.5)
    x[:int(0.02 * SR)] *= np.linspace(0, 1, int(0.02 * SR))
    x[-int(0.004 * SR):] *= np.linspace(1, 0, int(0.004 * SR))
    return np.vstack([x, x]) * 0.7


def thump(p):
    """A sub drop under the impact (sfx_r24.py's thump with its timing as parameters)."""
    n = int(p["length"] * SR)
    x = sine(ramp(n, p["start"], p["end"], tau=0.08), n) * exp_decay(n, p["tau"])
    x[:int(0.003 * SR)] *= np.linspace(0, 1, int(0.003 * SR))
    return np.vstack([x, x])


def swarm(p, path):
    """A recorded layer copied into a swarm: each copy cut at its offset, played at its speed and
    panned; summed, then swelling over `rise` s and fading over `fall` s."""
    n = int(p["length"] * SR)
    src = recorded(dict(p, cut=dict(p["cut"])), path)
    out = np.zeros((2, n))
    for offset, speed, position in p["copies"]:
        a = int(offset * SR)
        seg = resample(src[:, a:a + int(n * speed) + 2], speed)[:, :n]
        out[:, :seg.shape[1]] += pan(seg.mean(axis=0), position)
    t = np.arange(n) / SR
    env = np.where(t < p["rise"], (t / p["rise"]) ** 1.5,
                   np.clip(1 - (t - p["rise"]) / p["fall"], 0, 1) ** 1.6)
    return out * env


SYNTHS = {"shimmer": shimmer, "whoosh": whoosh, "whine": whine, "thump": thump}


def recorded_layers(subject, v):
    """A variant's layers that need a source file, in its order."""
    return [layer for layer in SOUNDS[subject]["variants"][v] if "page" in layer]


def treat(subject, v):
    """A variant's treatment of its (full-scale clipped) source files, one per recorded layer: cut,
    mix, level."""
    sound = SOUNDS[subject]

    def run(*paths):
        files = iter(paths)
        parts = []
        for layer in sound["variants"][v]:
            if "page" in layer:
                path = next(files)
                st = swarm(layer, path) if layer.get("synth") == "swarm" else recorded(layer, path)
            else:
                st = SYNTHS[layer["synth"]](layer)
            if parts:
                st = st * undb(loudest_db(parts[0][1]) + layer.get("gain", 0.0) - loudest_db(st))
            parts.append((int(layer["at"] * SR), st))
        out = np.zeros((2, max(at + st.shape[1] for at, st in parts)))
        for at, st in parts:
            out[:, at:at + st.shape[1]] += st
        return level(out, sound["level"], sound["ceiling"])
    return run


CHOSEN = {"wraith-decloak": ("b",), "mote-swarm": ("b",), "lance": ("a",)}

# The chosen sounds for tools/art/sfx_originals.py (its DERIVED sounds): the pages of their
# originals in the order the build takes them, the ceiling and the build.
PRODUCTION = {f"{subject}-r32-{v}": dict(
    page=recorded_layers(subject, v)[0]["page"],
    pages=[layer["page"] for layer in recorded_layers(subject, v)],
    peak=SOUNDS[subject]["ceiling"], build=treat(subject, v))
    for subject, variants in CHOSEN.items() for v in variants}


def build(subject, v, preview_cache):
    layers = recorded_layers(subject, v)
    with tempfile.TemporaryDirectory() as tmp:
        found = [(layer["author"], *source(layer, preview_cache, tmp)) for layer in layers]
        st = treat(subject, v)(*(path for _, path, _ in found))
    used = "; ".join(f"{author} from {how}" for author, _, how in found)
    synths = [layer["synth"] for layer in SOUNDS[subject]["variants"][v] if "page" not in layer]
    if synths:
        used += "; " + ", ".join(synths) + " synthesized"
    name = f"{subject}-r32-{v}"
    tags = {"COMMENT": f"{SCRIPT} {name}: {used}"}
    out_dir = OUT if v in CHOSEN[subject] else OUT / "rejected"
    out = write_ogg(out_dir / f"{name}.ogg", st, max_peak_db=SOUNDS[subject]["ceiling"], tags=tags)
    report(out, used)


def report(path, used):
    """The checks we can make without listening: length, peak, levels, DC, the edges, the envelope."""
    x = decode(path)
    edge = int(0.005 * SR)
    n = int(0.05 * SR)
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
          f"  envelope (50 ms RMS, dBFS): {' '.join(f'{e:.0f}' for e in env)}")


def main(argv):
    preview_cache = Path(os.environ.get("TMPDIR", tempfile.gettempdir())) / "conceptgame-sfx-cache"
    preview_cache.mkdir(parents=True, exist_ok=True)
    for subject, sound in SOUNDS.items():
        for v in sound["variants"]:
            if argv and subject not in argv and f"{subject}-r32-{v}" not in argv:
                continue
            build(subject, v, preview_cache)


if __name__ == "__main__":
    main(sys.argv[1:])
