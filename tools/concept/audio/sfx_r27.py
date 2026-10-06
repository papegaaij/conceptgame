#!/usr/bin/env python3
"""Concept round 27: the game-saved sound (synthesized) and the Coilwyrm's chain-cut tear
(recorded, CC0 / CC-BY, Freesound), an a/b pair each.

Outputs (design/audio/sfx/concept/, the unchosen ones into concept/rejected/; OGG Vorbis q6,
44.1 kHz stereo):

  ui-save-r27-<v>.ogg               a save written: the hangar's autosave as it opens and a save to
                                    a slot (synthesized, the round-08 UI family: sfx_r08.py's notes,
                                    bells and sparkles; peak -8 dBFS like the other UI blips)
  enemy-coilwyrm-cut-r27-<v>.ogg    a Coilwyrm's chain cut through: the body torn in two as its rear
                                    part starts to grow a new head (the simulation's CHAIN_CUT)

The save sounds must not be mistaken for the menu confirm (two rising square notes, 0.23 s) or the
shop's purchase (two notes and a coin sparkle): a is a quick rising run of data ticks that lands on
a bright held fifth with a bell (0.7 s), b a soft latch click and two warm bells a fourth apart over
a swelling pad (0.9 s).

The tears are cut from the Freesound **originals** cached by
tools/concept/audio/freesound_fetch.py --download (sfx_r25.py's `source`: clipped at full scale
first where an original decodes beyond it), else from the public HQ preview (with a note). The cut
is import_sfx.py's `process` (leading silence trimmed, offset, length, fades, filters), then an
optional speed change (resampled: lower and longer below 1), then the level: the loudest 100 ms of
the 200 Hz-5 kHz band (sfx_r24.py's `level`, limited at the ceiling) at -20 dB, ceiling -3 dBFS,
between the Coilwyrm's segment burst (-17.3, which plays with the cut segment) and its regrowth
(-27.4); the placeholder it replaces, the Brood Pod's burst, measures -23.3.

Nobody listened to these: the synthesized ones were checked by their envelopes and levels, the
recorded ones picked by the sources' descriptions, ratings and tags and by their envelopes, band
balance and levels (printed by this script for every file it writes).

Chosen (round 27 closed, 2026-10-06): save b, tear a. PRODUCTION holds the chosen tear's treatment
for tools/art/sfx_originals.py, which applies it to the Freesound original and writes it to
assets/sfx/ under the concept file's name with a SOURCE comment (the same cut as the concept file,
which was already cut from the original); the save sound, synthesized, is copied into assets/ by
:pipeline:importPlaceholders.

Usage: python3 tools/concept/audio/sfx_r27.py [subject-or-file ...]
       e.g. ui-save (both variants) or enemy-coilwyrm-cut-r27-a. Deterministic.
"""
import os
import sys
import tempfile
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
from import_sfx import process  # noqa: E402
from sfx_r08 import PEAK_UI, bell, n_of, note, seq, sparkles, st  # noqa: E402
from sfx_r24 import level, resample  # noqa: E402
from sfx_r25 import fs, report, source  # noqa: E402
from synth import SR, adsr, delay, fade, noise, normalize_peak, svf, triangle, write_ogg  # noqa: E402

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "design" / "audio" / "sfx" / "concept"
SCRIPT = "tools/concept/audio/sfx_r27.py"
CC0 = "CC0 1.0"
BY3 = "CC-BY 3.0"


# ---------------------------------------------------------------- the save (synthesized)

def save_a():
    """A data write that lands: four quick rising pulse ticks (E6 G#6 B6 E7, 32 ms apart), then a
    bright triangle fifth (E6 + B6) held for 0.2 s over a bell an octave up, a short echo."""
    rng = np.random.default_rng(271)
    ticks = seq([note(nt, 0.035, "pulse", width=0.25, gate=0.015, s=0.1) for nt in ["E6", "G#6", "B6", "E7"]],
                0.032, tail=0.0)
    n = n_of(0.62)
    out = np.zeros(n)
    out[:len(ticks)] += svf(ticks, 7000) * 0.55
    at = n_of(0.13)
    held = n_of(0.42)
    chord = sum(triangle(f, held) for f in (1318.5, 1975.5)) * adsr(held, a=0.004, d=0.08, s=0.5, r=0.12, gate=0.22)
    out[at:at + held] += chord * 0.7
    out[at:] += bell("E7", (n - at) / SR, 0.45)
    out += sparkles(n, 2, rng, 0.25, 0.45, 0.2)
    return fade(delay(st(out, 0.2), 0.08, feedback=0.22, mix=0.15, damp=6000, tail=0.08), 0.001, 0.06)


def save_b():
    """A calm confirmation: a soft latch click, then two warm bells a fourth apart (A5, then D6
    80 ms later) over a triangle pad (D5 + A5) that swells in over 60 ms and fades."""
    rng = np.random.default_rng(272)
    n = n_of(0.82)
    click = np.zeros(n)
    ln = n_of(0.015)
    click[:ln] = svf(noise(ln, rng), 2800, q=1.8, mode="bp") * np.exp(-np.arange(ln) / (0.003 * SR))
    pad_n = n_of(0.7)
    pad = sum(triangle(f, pad_n) for f in (587.3, 880.0)) * adsr(pad_n, a=0.06, d=0.15, s=0.5, r=0.3, gate=0.3)
    out = click * 0.6
    at = n_of(0.02)
    out[at:at + pad_n] += svf(pad, 3500) * 0.35
    for name, start, lv in (("A5", 0.02, 0.8), ("D6", 0.10, 0.9)):
        s = n_of(start)
        out[s:] += bell(name, (n - s) / SR, lv)
    return fade(delay(st(out, 0.3), 0.11, feedback=0.25, mix=0.18, damp=5000, tail=0.1), 0.001, 0.1)


SYNTH = {"ui-save": {"a": save_a, "b": save_b}}
SYNTH_CHOSEN = {"ui-save": "b"}


# ---------------------------------------------------------------- the tear (recorded)

# subject: its level (loudest 100 ms of the 200 Hz-5 kHz band, dB), ceiling (dBFS) and the two
# variants: source, title, author, licence, the cut (import_sfx.py's process keys) and options
# (speed: resampled playback speed).
SOUNDS = {
    "enemy-coilwyrm-cut-r27": dict(level=-20.0, ceiling=-3.0, variants={
        # a monster tearing at flesh, organic Foley: a run of short wet rips, bright (most of its
        # energy at 1.6-6.4 kHz), its first 0.9 s 15 % slower for a bigger body
        "a": dict(**fs("aust_paul", 30928, 30, 15696), title="rip_tear FLESH!!!!.wav", author="aust_paul",
                  licence=CC0, cut=dict(offset=0.0, length=0.9, fade=0.35, highpass=80, lowpass=11000),
                  speed=0.85),
        # stale bread torn apart, the breaks layered: one juicy limb-tearing rip with a heavy low
        # body (half its energy below 80 Hz, high-passed at 90 Hz)
        "b": dict(**fs("dereklieu", 241822, 241, 4405300), title="Tearing Flesh", author="dereklieu",
                  licence=BY3, cut=dict(offset=0.0, length=0.7, fade=0.3, highpass=90)),
    }),
}

CHOSEN = {"enemy-coilwyrm-cut-r27": "a"}


def treat(subject, v):
    """A variant's treatment of a (full-scale clipped) source file: cut, speed, level."""
    sound = SOUNDS[subject]
    src = sound["variants"][v]

    def run(path):
        x = process(dict(src["cut"], peak=-1.0), path)
        if src.get("speed", 1.0) != 1.0:
            x = resample(x, src["speed"])
        return level(x, sound["level"], sound["ceiling"])
    return run


# The chosen tear for tools/art/sfx_originals.py (its DERIVED sounds): page, peak and build.
PRODUCTION = {f"{subject}-{v}": dict(page=SOUNDS[subject]["variants"][v]["page"],
                                     peak=SOUNDS[subject]["ceiling"], build=treat(subject, v))
              for subject, v in CHOSEN.items()}


def build_synth(subject, v):
    sig = SYNTH[subject][v]()
    sig = sig - sig.mean(axis=1, keepdims=True)
    sig = fade(normalize_peak(sig, PEAK_UI), 0.0005, 0.005)
    name = f"{subject}-r27-{v}"
    out_dir = OUT if SYNTH_CHOSEN.get(subject) == v else OUT / "rejected"
    out = write_ogg(out_dir / f"{name}.ogg", sig, max_peak_db=PEAK_UI, tags={"COMMENT": f"{SCRIPT} {name}"})
    report(out, "synthesized")


def build_recorded(subject, v, preview_cache):
    sound = SOUNDS[subject]
    with tempfile.TemporaryDirectory() as tmp:
        path, used = source(sound["variants"][v], preview_cache, tmp)
        x = treat(subject, v)(path)
    name = f"{subject}-{v}"
    out_dir = OUT if CHOSEN.get(subject) == v else OUT / "rejected"
    out = write_ogg(out_dir / f"{name}.ogg", x, max_peak_db=sound["ceiling"],
                    tags={"COMMENT": f"{SCRIPT} {name} from {used}"})
    report(out, used)


def main(argv):
    preview_cache = Path(os.environ.get("TMPDIR", tempfile.gettempdir())) / "conceptgame-sfx-cache"
    preview_cache.mkdir(parents=True, exist_ok=True)
    for subject, variants in SYNTH.items():
        for v in variants:
            if not argv or subject in argv or f"{subject}-r27-{v}" in argv:
                build_synth(subject, v)
    for subject, sound in SOUNDS.items():
        for v in sound["variants"]:
            if not argv or subject in argv or subject.removesuffix("-r27") in argv or f"{subject}-{v}" in argv:
                build_recorded(subject, v, preview_cache)


if __name__ == "__main__":
    main(sys.argv[1:])
