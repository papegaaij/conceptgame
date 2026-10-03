#!/usr/bin/env python3
"""Concept round 16: the Leviathan's whale-song cry, synthesized.

Outputs (design/audio/sfx/concept/):
  enemy-leviathan-cry-r16-a.ogg   "Whale song": a deep, mournful alien call of 2.4 s plus its
                                  space-sized tail (3.0 s in all), played when the Leviathan dies
                                  (design/enemies/space/leviathan: death "whale-song cry")

The voice is a glottal-like pulse train at a gliding fundamental (rising 92 -> 128 Hz in the first
half second, then a long fall to 52 Hz with one upward yodel break at 1.05 s and a widening slow
vibrato), shaped by three moving vocal formants ("oo" -> "aah" -> "oo", 320/780/1900 Hz at the
open vowel) so it reads as a throat, not a synth. A second voice a tritone and a few cents above,
entering at 0.3 s and gliding more slowly, beats against it for the alien colour; a faint
inharmonic whistle (7.3x) falls with it, a breathy blowhole rasp opens the call, and a sub
octave carries the weight. A long dark reverb gives it its size.

Levelled like the recorded enemy sounds (import_sfx.py, band_rms): 200 Hz-5 kHz band RMS
-30 dB (the Vrell screech and spawn sounds), transients limited at -6 dBFS, so the call sits
under the large explosion it plays with (band RMS -21 to -24 dB, peak -1.6 dBFS) and carries on
after it; its formants (300 Hz-2 kHz) sit in the explosion's gaps rather than its low boom.

Usage: python3 tools/concept/audio/sfx_r16.py
Deterministic: fixed seeds.
"""
import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
from import_sfx import band_rms_db  # noqa: E402
from sfx_r11 import edges, level, n_of  # noqa: E402
from synth import SR, lufs, noise, reverb, saw, sine, svf, write_ogg  # noqa: E402

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "design" / "audio" / "sfx" / "concept"
CALL = 2.4             # s, the voice
LENGTH = 3.0           # s, with the tail
BAND_DB = -30.0
CEILING_DB = -6.0


def interp(points, t):
    """Piecewise-linear curve through (time, value) points, sampled at t."""
    xs, ys = zip(*points)
    return np.interp(t, xs, ys)


def glottal(f0, n, rng, sharp=6.0):
    """A pulse train at the per-sample frequency f0: a smooth, slightly asymmetric pulse per
    cycle with a little cycle-to-cycle jitter (a throat, not an oscillator)."""
    jitter = 1 + 0.006 * svf(noise(n, rng), 30.0)
    ph = np.cumsum(f0 * jitter) / SR
    frac = ph - np.floor(ph)
    pulse = np.exp(-sharp * frac) * np.sin(np.pi * np.clip(frac * 1.35, 0, 1)) ** 2
    pulse = pulse - pulse.mean()
    buzz = saw(f0 * jitter, n)                                 # the bright part of the throat
    return pulse / (np.std(pulse) + 1e-9) * 0.5 + buzz / (np.std(buzz) + 1e-9) * 0.5


def formants(src, t, vowel_points):
    """The source through three moving band-pass resonances: ``vowel_points`` maps times to
    (f1, f2, f3) in Hz."""
    out = np.zeros_like(src)
    for j, (gain, q) in enumerate(((1.0, 5.0), (0.55, 7.0), (0.22, 9.0))):
        f = interp([(tt, v[j]) for tt, v in vowel_points], t)
        out += gain * svf(src, f, q=q, mode="bp")
    return out


def whale_cry():
    rng = np.random.default_rng(1601)
    n = n_of(CALL)
    t = np.arange(n) / SR
    # the fundamental: rise, long mournful fall with one yodel break, widening slow vibrato
    f0 = interp([(0.0, 92), (0.45, 128), (1.0, 104), (1.05, 121), (1.25, 108), (2.4, 52)], t)
    f0 = f0[0] + svf(f0 - f0[0], 12.0)                         # glide, never step
    f0 *= 1 + (0.004 + 0.018 * np.clip(t / CALL, 0, 1)) * np.sin(2 * np.pi * (3.6 + 0.8 * t) * t)
    vowel = [(0.0, (300, 700, 1700)), (0.5, (360, 850, 2000)), (0.9, (680, 1150, 2300)),
             (1.5, (520, 980, 2100)), (2.4, (290, 620, 1600))]   # "oo" -> "aah" -> "oo"
    env = np.clip(t / 0.22, 0, 1) ** 1.5 * np.clip((CALL - t) / 0.7, 0, 1) ** 1.3
    env *= 0.85 + 0.15 * np.sin(2 * np.pi * 0.9 * t + 0.4)     # a slow swell in the breath
    src = glottal(f0, n, rng)
    voice = (formants(src, t, vowel) + 3.0 * svf(src, 220.0, q=0.8)) * env   # the formants and the chest
    # the second voice: a tritone and a few cents above, later, gliding more slowly (it beats)
    f1 = interp([(0.0, 130), (0.6, 176), (1.3, 150), (2.4, 82)], t)
    f1 = (f1[0] + svf(f1 - f1[0], 8.0)) * 1.0035
    env1 = np.clip((t - 0.3) / 0.45, 0, 1) * np.clip((CALL - 0.15 - t) / 0.8, 0, 1)
    voice += 0.45 * formants(glottal(f1, n, rng, sharp=8.0), t, vowel) * env1
    # a faint inharmonic whistle falling with the call, the sub octave, the blowhole rasp
    voice += 0.03 * sine(f0 * 7.3, n) * env * np.clip((t - 0.2) / 0.5, 0, 1)
    voice += 0.05 * sine(f0 * 0.5, n) * env
    rasp = svf(svf(noise(n, rng), 900, q=0.9), 250, mode="hp")
    voice += 0.25 * rasp * np.clip(t / 0.05, 0, 1) * np.exp(-t / 0.35) * (0.6 + 0.4 * np.abs(np.sin(np.pi * f0 * t / 4)))
    voice = svf(svf(voice, 70.0, mode="hp"), 70.0, mode="hp")  # weight, not sub-bass (small speakers)
    full = np.zeros(n_of(LENGTH))
    full[:n] = voice
    d = n_of(0.011)                                            # a little width, then the size
    st = np.vstack([full, np.concatenate([np.zeros(d), full[:-d]])]) * 0.85
    st = reverb(st, seconds=2.6, mix=0.42, damping=0.8, seed=1602)[:, :n_of(LENGTH)]
    return edges(level(st, BAND_DB, CEILING_DB), 0.004, 0.45)


SOUNDS = {"enemy-leviathan-cry-r16-a": whale_cry}


def main(names):
    for name in names or SOUNDS:
        st = SOUNDS[name]()
        path = write_ogg(OUT / f"{name}.ogg", st, max_peak_db=CEILING_DB)
        loud, true_peak = lufs(st)
        print(f"{path.relative_to(ROOT)}: {st.shape[1] / SR:.2f}s  band {band_rms_db(st):.1f} dB"
              f"  peak {20 * np.log10(np.abs(st).max()):.1f} dBFS  {loud:.1f} LUFS  true peak {true_peak:.1f} dBTP")


if __name__ == "__main__":
    main(sys.argv[1:])
