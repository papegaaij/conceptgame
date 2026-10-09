#!/usr/bin/env python3
"""Concept round 33 (M5 part E, Level 11 "Atlantic Convoy"): the Harbour Kraken's, the convoy ships'
and the Driftjelly's sounds, one proposal each (no a/b: the user asked for an a/b only for two art
items this round), recorded (CC0, Freesound) and synthesized layers mixed. Every recorded layer is
CC0 except the byjoshberry waves (CC-BY, already on the credits roll).

Outputs (design/audio/sfx/concept/, OGG Vorbis q6, 44.1 kHz stereo); the offsets are the moment of
the event inside the file, so the game starts the file `offset` s before the event it belongs to:

  kraken-slam-r33-a.ogg      a slam arm hitting its lane: a rising rush (the arm rising out of the
                             water), then a heavy wet impact with a big splash and sub thump; about
                             2.2 s. The impact is at SLAM_IMPACT = 0.5 s (the arm's rise, a first
                             value of the Kraken data's `rise`): start the file when the arm starts
                             to rise, or (rise - 0.5) s earlier/later if the data moves the rise
  kraken-churn-r33-a.ogg     the lane telegraph's churn: a surge of waves swelling for 1.0 s
                             (the telegraph; hard 0.8 s: skip its first 0.2 s, the swell peaks at
                             the end); 1.0 s, no tail (the slam's rush takes over at the cut)
  kraken-surface-r33-a.ogg   the head surfacing (2.0 s, `SURFACE`): a deep swell rising under the
                             water, the crown breaking through (the layer flip at the midpoint,
                             1.0 s) and water streaming off; 2.75 s (the tail after the 2 s ends
                             as the head's settled)
  ship-hit-r33-a.ogg         a cargo ship hit (the ALLY_HIT cue, the slam's impact): hull metal
                             struck, water rushing in; impact at 0 s; 0.9 s
  ship-sink-r33-a.ogg        a ship sinking (its 10 sinking steps): a groan of steel, a muffled
                             boom below, waves washing over; starts with the first step; 3.8 s
  frigate-flak-r33-a.ogg     the frigate's distant flak (one ALLY_FLAK event, every 2 s while it is
                             on screen): two dull pops far away under the ocean ambience, the first
                             at 0 s, the second at 0.55 s; 1.6 s. Quiet: an ambience layer, not a cue
  driftjelly-pulse-r33-a.ogg a Driftjelly's proximity ring (the event the ring is released on,
                             72-96 px): a soft underwater bloop and a ripple; fully synthesized,
                             no source; 0.9 s. Reusing the enemy shot's small sound (the README's
                             default) stays possible
  kraken-death-r33-a.ogg     the Kraken's death (starts with the `huge` water burst, which the
                             game plays on top): a long, wet groan sinking away over churning
                             water; 5.2 s

Sources (the `SOUNDS` table; every one in CREDITS.md): each recorded layer is cut from the
Freesound **original** when it is cached (tools/concept/audio/freesound_fetch.py --download,
~/.cache/terran-vanguard/freesound/<id>_<name>.<type>; clipped at full scale first, as sfx_r25.py
does), else from the public HQ preview (with a note). The cut is import_sfx.py's `process`
(leading silence trimmed, offset, length, fades, filters, peak -1 dBFS), then an optional speed
change (resampled: lower and longer below 1), an approach (a swell in dB) and a pan. Each layer
enters at `at` s and sits `gain` dB from the file's first (key) layer, both measured as the loudest
100 ms of the full band.

Levels, set against the existing files (sfx_r24.py's `level`: the loudest 100 ms of the 200 Hz-5 kHz
band, limited at a ceiling; the existing set measured in assets/sfx/):
  slam      -15 dB, ceiling -1.5 dBFS: under the large/huge explosions (-11 / -12.5), above the hit
            sounds: the fight's signature hit, with the ship hit and the head's burst sometimes on
            top of it.
  churn     -24 dB, ceiling -6 dBFS: a warning under the music, as the carrier's launch (-24).
  surface   -21 dB, ceiling -4 dBFS: a swell, between the roars (-19 / -24) and the churn.
  ship hit  -18 dB, ceiling -3 dBFS: as the armour hit (-20) and the mortar impact (-21), a little
            louder: the convoy is the secondary objective and the radio names the hull.
  ship sink -20 dB, ceiling -3 dBFS: a slow groan under the hit's level.
  flak      -16 dB, ceiling -2 dBFS, low-passed at 4 kHz: raised twice after listening (-33 and -24 were too quiet
            under the ocean ambience and the music).
  jelly     -24 dB, ceiling -8 dBFS: the spawn sound's level is -27 dB, but a pure tone's loudness
            (-30 LUFS here against its -24) is lower at the same band level; up to six rings at once.
  death     -17 dB, ceiling -3 dBFS: under the `huge` burst (-12.5), which plays on top.

Picked by the sources' descriptions, ratings and tags and by their envelopes, band balance and levels
(printed by this script for every file it writes); the user listened in round 33 and accepted all
eight (churn, death, sink and flak after a rework). PRODUCTION hands the seven with a recorded layer
to tools/art/sfx_originals.py, which writes them to assets/sfx/ under the concept file's name.

Usage: python3 tools/concept/audio/sfx_r33.py [subject-or-file ...]
       e.g. kraken-slam or ship-hit-r33-a. Deterministic.
"""
import os
import sys
import tempfile
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
from sfx_r24 import level, resample  # noqa: E402
from sfx_r25 import fs, source  # noqa: E402
from sfx_r32 import loudest_db, recorded, report, thump, whoosh  # noqa: E402
from synth import SR, exp_decay, noise, pan, ramp, sine, svf, undb, write_ogg  # noqa: E402

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "design" / "audio" / "sfx" / "concept"
SCRIPT = "tools/concept/audio/sfx_r33.py"
CC0 = "CC0 1.0"
CCBY = "CC-BY 4.0"

SLAM_IMPACT = 0.5  # s: the slam file's impact (the arm's rise)

# subject: its level (loudest 100 ms of the 200 Hz-5 kHz band, dB), ceiling (dBFS) and the layers:
# a recorded layer has a source (fs(): page and preview), title, author, licence, the cut
# (import_sfx.py's process keys) and optional speed, approach (dB) and pan; a synthesized layer has
# `synth` (the generator's name) and its parameters. Every layer enters at `at` s; the first is the
# key layer, the others sit `gain` dB from its loudest 100 ms.
SOUNDS = {
    "kraken-slam": dict(level=-15.0, ceiling=-1.5, layers=[
        # a robot's heavy footstep: a thud with a metallic body (the arm's mass hitting the water)
        dict(**fs("AudioPapkin", 813053, 813, 8698658), title="Big robot footstep 002",
             author="AudioPapkin", licence=CC0,
             cut=dict(offset=0.0, length=1.2, fade=0.6, highpass=45), speed=0.8, at=SLAM_IMPACT),
        # a big splash, the whole of it
        dict(**fs("Bird_man", 316744, 316, 4745081), title="Big Splash.wav", author="Bird_man",
             licence=CC0, cut=dict(offset=0.0, length=1.7, fade=0.7, highpass=120), at=SLAM_IMPACT,
             gain=-1.5),
        # the arm rising out of the water: a rush climbing to the impact
        dict(synth="whoosh", at=0.0, gain=-9.0, length=SLAM_IMPACT + 0.1, start=300.0, peak=2200.0,
             end=1800.0, rise=SLAM_IMPACT, fall=0.1, pan=(0.0, 0.0), seed=331),
        dict(synth="thump", at=SLAM_IMPACT, gain=-2.0, length=1.2, start=85.0, end=32.0, tau=0.3),
        # spray and the water coming down after the impact
        dict(synth="whoosh", at=SLAM_IMPACT, gain=-11.0, length=1.6, start=3200.0, peak=3200.0,
             end=700.0, rise=0.02, fall=1.5, pan=(-0.4, 0.4), seed=332),
    ]),
    "kraken-churn": dict(level=-24.0, ceiling=-6.0, layers=[
        # a lane of water surging: waves breaking against a bow, swelling from -14 dB to full
        dict(**fs("byjoshberry", 435668, 435, 5409980), title="Ocean waves hitting bow of moving boat.", author="byjoshberry",
             licence=CCBY, cut=dict(offset=5.6, length=1.0, fade=0.04, fadein=0.02, highpass=90),
             approach=-14.0, at=0.0),
        # a second, slower wash under it
        dict(**fs("byjoshberry", 435668, 435, 5409980), title="Ocean waves hitting bow of moving boat.", author="byjoshberry",
             licence=CCBY, cut=dict(offset=14.6, length=0.85, fade=0.04, fadein=0.02, highpass=80,
                                    lowpass=2500),
             speed=0.85, approach=-12.0, at=0.0, gain=-3.0),
        dict(synth="rumble", at=0.0, gain=-7.0, length=1.0, low=55.0, peak=0.95, cutoff=(150.0, 600.0),
             seed=333),
    ]),
    "kraken-surface": dict(level=-21.0, ceiling=-4.0, layers=[
        # something big rising under the surface: a deep swell, its peak as the crown breaks
        dict(synth="rumble", at=0.0, length=2.7, low=48.0, peak=0.4, cutoff=(90.0, 420.0), seed=334),
        # water coming off a body rising from under it: runs and pours
        dict(**fs("adviseme333", 679410, 679, 14805886), title="Coming up from underwater",
             author="adviseme333", licence=CC0,
             cut=dict(offset=0.0, length=1.9, fade=1.2, fadein=0.25, highpass=120), at=0.85, gain=-1.0),
        dict(synth="whoosh", at=0.8, gain=-12.0, length=1.8, start=500.0, peak=2400.0, end=900.0,
             rise=0.3, fall=1.5, pan=(-0.5, 0.5), seed=335),
    ]),
    "ship-hit": dict(level=-18.0, ceiling=-3.0, layers=[
        # metal struck, ringing: a shovel's impact on steel
        dict(**fs("Sadiquecat", 718749, 718, 5287430), title="Metal Shovel Impact SFX (leading woosh)2",
             author="Sadiquecat", licence=CC0,
             cut=dict(offset=0.0, length=0.8, fade=0.35, highpass=250), speed=0.85, at=0.0),
        # a dull metallic thud under it (the hull's body)
        dict(**fs("profoundsounds", 686282, 686, 14947773), title="slightmetallicthud.mp3",
             author="profoundsounds", licence=CC0,
             cut=dict(offset=0.0, length=0.6, fade=0.3, highpass=60), speed=0.75, at=0.0, gain=-2.0),
        # water rushing in
        dict(**fs("jamesabels", 166966, 166, 2197514), title="Splash_002.wav", author="jamesabels",
             licence=CC0, cut=dict(offset=0.0, length=0.7, fade=0.4, highpass=200), at=0.06, gain=-4.0),
        dict(synth="thump", at=0.0, gain=-5.0, length=0.5, start=78.0, end=40.0, tau=0.12),
    ]),
    "ship-sink": dict(level=-20.0, ceiling=-3.0, layers=[
        # a steel hull groaning under strain: the loud creaks of a long take, slowly dying
        dict(**fs("kyles", 455757, 455, 612689),
             title="door wood old slow creaks open close or ship hull list2.flac", author="kyles",
             licence=CC0, cut=dict(offset=5.5, length=3.0, fade=1.4, fadein=0.3, highpass=90), speed=0.9,
             at=0.0),
        # the hull's break: a muffled boom far below
        dict(synth="thump", at=0.15, gain=-5.0, length=1.4, start=70.0, end=28.0, tau=0.35),
        # the sea closing over the hull: a breaking swell, then the wash rolling over the deck
        dict(**fs("byjoshberry", 435668, 435, 5409980), title="Ocean waves hitting bow of moving boat.", author="byjoshberry",
             licence=CCBY, cut=dict(offset=22.6, length=2.4, fade=1.0, fadein=0.5, highpass=100),
             speed=0.9, at=0.7, gain=-3.0, pan=-0.2),
        dict(**fs("byjoshberry", 435668, 435, 5409980), title="Ocean waves hitting bow of moving boat.", author="byjoshberry",
             licence=CCBY, cut=dict(offset=30.6, length=1.6, fade=1.0, fadein=0.4, highpass=100),
             speed=0.8, at=1.8, gain=-5.0, pan=0.25),
        dict(synth="rumble", at=0.4, gain=-9.0, length=2.9, low=42.0, peak=0.4, cutoff=(200.0, 120.0),
             seed=336),
    ]),
    "frigate-flak": dict(level=-16.0, ceiling=-2.0, layers=[
        # a distant shot: only the dull thump reaches us, low-passed, a little slowed
        dict(**fs("HenKonen", 682122, 682, 10938187), title="Distant Shot 4.wav", author="HenKonen",
             licence=CC0, cut=dict(offset=0.0, length=1.4, fade=0.7, highpass=80, lowpass=4000),
             speed=0.92, at=0.0, pan=-0.3),
        dict(**fs("HenKonen", 682120, 682, 10938187), title="Distant Shot 2.wav", author="HenKonen",
             licence=CC0, cut=dict(offset=0.0, length=1.2, fade=0.6, highpass=80, lowpass=4000),
             speed=1.1, at=0.55, gain=-3.0, pan=0.35),
    ]),
    "driftjelly-pulse": dict(level=-24.0, ceiling=-8.0, layers=[
        dict(synth="bloop", at=0.0, length=0.9, start=520.0, end=170.0, tau=0.12),
    ]),
    "kraken-death": dict(level=-17.0, ceiling=-3.0, layers=[
        # a sea creature's roar, slowed far down: a huge animal's long groan; its loudest stretch
        dict(**fs("Bikkit99", 837799, 837, 16586370), title="Sea Creature Roar", author="Bikkit99",
             licence=CC0, cut=dict(offset=2.4, length=3.8, fade=2.2, fadein=0.12, highpass=45), speed=0.8,
             at=0.1),
        # churning water around the sinking head
        dict(**fs("unfa", 532492, 532, 1038806), title="CO2 Water Jet", author="unfa", licence=CC0,
             cut=dict(offset=0.6, length=4.4, fade=2.0, fadein=0.3, highpass=80, lowpass=5000), at=0.2,
             gain=-6.0),
        dict(synth="rumble", at=0.0, gain=-4.0, length=5.2, low=40.0, peak=0.15, cutoff=(500.0, 100.0),
             seed=337),
    ]),
}


def rumble(p):
    """A deep swell: low-passed noise under a sub sine at `low` Hz, the cutoff gliding over `cutoff`,
    rising to its peak at `peak` x the length and falling away (a thing rising under water)."""
    rng = np.random.default_rng(p["seed"])
    n = int(p["length"] * SR)
    t = np.arange(n) / SR
    x = svf(noise(n, rng), ramp(n, *p["cutoff"], curve="lin"), q=0.9, mode="lp")
    x = x / (np.max(np.abs(x)) + 1e-9) + 0.9 * sine(low_wobble(n, p["low"], t), n)
    top = p["peak"] * p["length"]
    env = np.where(t < top, (t / top) ** 2, np.exp(-(t - top) / (0.45 * (p["length"] - top) + 0.05)))
    x = x * env
    x[:int(0.01 * SR)] *= np.linspace(0, 1, int(0.01 * SR))
    x[-int(0.02 * SR):] *= np.linspace(1, 0, int(0.02 * SR))
    return np.vstack([x, x])


def low_wobble(n, low, t):
    """The sub's frequency: `low` Hz rising a third over the take, with a slow wobble."""
    return low * (1.0 + 0.3 * t / t[-1]) * (1 + 0.03 * np.sin(2 * np.pi * 1.7 * t))


def bloop(p):
    """A soft underwater bloop: a sine falling start -> end (exponential approach), a quiet octave
    partial, a second, lower echo bloop 0.24 s later and a band of ripple noise after it."""
    rng = np.random.default_rng(338)
    n = int(p["length"] * SR)
    x = np.zeros(n)
    for delay, gain, shift in ((0.0, 1.0, 1.0), (0.24, 0.35, 0.8)):
        a = int(delay * SR)
        m = n - a
        f = ramp(m, p["start"] * shift, p["end"] * shift, tau=p["tau"])
        seg = (sine(f, m) + 0.2 * sine(f * 2, m)) * exp_decay(m, 0.16)
        seg[:int(0.004 * SR)] *= np.linspace(0, 1, int(0.004 * SR))
        x[a:] += gain * seg
    ripple = svf(noise(n, rng), ramp(n, 1800.0, 600.0), q=1.6, mode="bp")
    t = np.arange(n) / SR
    x += 0.25 * ripple * np.clip(t / 0.05, 0, 1) * np.exp(-t / 0.18)
    x[-int(0.02 * SR):] *= np.linspace(1, 0, int(0.02 * SR))
    return np.vstack([x, x])


SYNTHS = {"whoosh": whoosh, "thump": thump, "rumble": rumble, "bloop": bloop}


def recorded_layers(subject):
    """The layers that need a source file, in the order of the mix."""
    return [layer for layer in SOUNDS[subject]["layers"] if "page" in layer]


def treat(subject):
    """The treatment of a subject's (full-scale clipped) source files, one per recorded layer: cut,
    mix, level."""
    sound = SOUNDS[subject]

    def run(*paths):
        files = iter(paths)
        parts = []
        for layer in sound["layers"]:
            if "page" in layer:
                st = recorded(layer, next(files))
                if "pan" in layer:
                    st = pan(st.mean(axis=0), layer["pan"])
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


# The sounds the user accepted in round 33 (2026-10-09; slam, surface, ship hit and jelly pulse as
# first made, churn, death, sink and flak after their rework) that need a source file, for
# tools/art/sfx_originals.py (its DERIVED sounds): the pages of their originals in the order the build
# takes them, the ceiling and the build. The Driftjelly's pulse is fully synthesized, so its concept
# file is the production file (copied by :pipeline:copyPlaceholderSounds).
PRODUCTION = {f"{subject}-r33-a": dict(
    page=recorded_layers(subject)[0]["page"],
    pages=[layer["page"] for layer in recorded_layers(subject)],
    peak=SOUNDS[subject]["ceiling"], build=treat(subject))
    for subject in SOUNDS if recorded_layers(subject)}

def build(subject, preview_cache):
    layers = recorded_layers(subject)
    with tempfile.TemporaryDirectory() as tmp:
        found = [(layer["author"], *source(layer, preview_cache, tmp)) for layer in layers]
        st = treat(subject)(*(path for _, path, _ in found))
    used = "; ".join(dict.fromkeys(f"{author} from {how}" for author, _, how in found))
    synths = [layer["synth"] for layer in SOUNDS[subject]["layers"] if "page" not in layer]
    if synths:
        used += ("; " if used else "") + ", ".join(synths) + " synthesized"
    name = f"{subject}-r33-a"
    out = write_ogg(OUT / f"{name}.ogg", st, max_peak_db=SOUNDS[subject]["ceiling"],
                    tags={"COMMENT": f"{SCRIPT} {name}: {used}"})
    report(out, used)


def main(argv):
    preview_cache = Path(os.environ.get("TMPDIR", tempfile.gettempdir())) / "conceptgame-sfx-cache"
    preview_cache.mkdir(parents=True, exist_ok=True)
    for subject in SOUNDS:
        if argv and subject not in argv and f"{subject}-r33-a" not in argv:
            continue
        build(subject, preview_cache)


if __name__ == "__main__":
    main(sys.argv[1:])
