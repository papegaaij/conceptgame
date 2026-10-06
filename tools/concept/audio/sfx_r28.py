#!/usr/bin/env python3
"""Concept round 28: the proximity mine's arming beep (synthesized), an a/b pair.

Outputs (design/audio/sfx/concept/, the unchosen one into concept/rejected/; OGG Vorbis q6,
44.1 kHz stereo):

  weapon-mine-arm-r28-<v>.ogg   a proximity mine arming, 0.4 s after its drop
                                (design/player/weapons/proximity-mines): the `mine` family's
                                "arming beep" (design/audio/sfx, Weapon sound families)

Up to six mines live at once and a mount drops two a second, so the beep is short (under 0.15 s),
dry (no echo or reverb tail to pile up), high and narrow-band (it sits above the shots' and
explosions' body instead of masking them), and quiet: peak -12 dBFS, the round-08 tick level of
the tally tick and the typewriter (sfx_r08.py's PEAK_TICK), below the UI blips' -8. The game plays
it at -10 dB in the flight mix (between the hits' -8 and the shots' -14; FlightSounds) and two at a
time at most.

  a  "armed chirp": two soft rising pulse blips a fifth apart (A6, then E7 45 ms later), low-passed
     so the pulse's edge stays a gentle buzz; electronic and friendly, reads as "ready" (0.10 s)
  b  "sensor ping": one sine ping that glides up a fifth in 15 ms and rings out on a faint metallic
     overtone (a 2.76 partial, the round-08 bells' ratio), a tiny latch click under its start
     (0.15 s)

Nobody listened to these: they were checked by their envelopes and levels (printed by this script
for every file it writes).

Chosen (round 28 closed, 2026-10-06): a, the armed chirp (CHOSEN); b is written to
concept/rejected/. The game plays a as a concept copy: `copyPlaceholderSounds` (pipeline) copies
it into assets/sfx/ like the other synthesized sounds, and `Sfx.MINE_ARM` names it.

Usage: python3 tools/concept/audio/sfx_r28.py [subject-or-file ...]
       e.g. weapon-mine-arm (both variants) or weapon-mine-arm-r28-a. Deterministic.
"""
import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
from sfx_r08 import PEAK_TICK, n_of, note, seq, st  # noqa: E402
from sfx_r25 import report  # noqa: E402
from synth import SR, exp_decay, fade, noise, normalize_peak, ramp, sine, svf, write_ogg  # noqa: E402

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "design" / "audio" / "sfx" / "concept"
SCRIPT = "tools/concept/audio/sfx_r28.py"


def mine_arm_a():
    """Two rising pulse blips a fifth apart (A6, E7), 28 ms each, 45 ms apart, the second a little
    louder; a 12.5 % pulse low-passed at 5.5 kHz under a sine an octave down for body."""
    blips = []
    for name, level in (("A6", 0.8), ("E7", 1.0)):
        pulse_part = note(name, 0.05, "pulse", width=0.125, gate=0.022, d=0.02, s=0.5, r=0.02)
        body = note(name, 0.05, "sine", gate=0.022, d=0.02, s=0.5, r=0.02)
        blips.append((svf(pulse_part, 5500) * 0.6 + body * 0.5) * level)
    out = seq(blips, 0.045, tail=0.012)
    return fade(st(out), 0.001, 0.008)


def mine_arm_b():
    """A sine ping gliding up a fifth (1.6 to 2.4 kHz) in 15 ms, then ringing out (tau 35 ms) with a
    faint 2.76 overtone (tau 12 ms) and a 4 ms band-passed noise click under its start."""
    rng = np.random.default_rng(281)
    n = n_of(0.15)
    glide = n_of(0.015)
    f = np.concatenate([ramp(glide, 1600.0, 2400.0), np.full(n - glide, 2400.0)])
    ping = sine(f, n) * exp_decay(n, 0.035)
    ping += 0.18 * sine(f * 2.76, n) * exp_decay(n, 0.012)
    attack = np.minimum(1, np.arange(n) / (0.0015 * SR))
    click = np.zeros(n)
    ln = n_of(0.004)
    click[:ln] = svf(noise(ln, rng), 4200, q=2.0, mode="bp") * np.exp(-np.arange(ln) / (0.001 * SR))
    return fade(st(ping * attack + click * 0.35), 0.001, 0.02)


SYNTH = {"weapon-mine-arm": {"a": mine_arm_a, "b": mine_arm_b}}
CHOSEN = {"weapon-mine-arm": "a"}


def build_synth(subject, v):
    sig = SYNTH[subject][v]()
    sig = sig - sig.mean(axis=1, keepdims=True)
    sig = fade(normalize_peak(sig, PEAK_TICK), 0.0005, 0.005)
    name = f"{subject}-r28-{v}"
    out_dir = OUT if CHOSEN.get(subject) == v else OUT / "rejected"
    out = write_ogg(out_dir / f"{name}.ogg", sig, max_peak_db=PEAK_TICK, tags={"COMMENT": f"{SCRIPT} {name}"})
    report(out, "synthesized")


def main(argv):
    for subject, variants in SYNTH.items():
        for v in variants:
            if not argv or subject in argv or f"{subject}-r28-{v}" in argv:
                build_synth(subject, v)


if __name__ == "__main__":
    main(sys.argv[1:])
