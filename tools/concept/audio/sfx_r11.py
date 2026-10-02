#!/usr/bin/env python3
"""Concept round 11: synthesized launch-rail sounds and edge-warning tones.

Outputs (design/audio/sfx/concept/):
  launch-rail-r11-a.ogg     "Mag-lev": a linear-motor hum and whine rising with the ship's
                            speed, coil ticks passing faster and faster, a release clunk at
                            3.6 s, then the engine whoosh into open space (4.9 s)
  launch-rail-r11-b.ogg     "Catapult": a pressure hiss building, the shuttle rumbling over the
                            rail joints with a rattle, a heavy buffer clunk at 3.6 s and the
                            steam vent dying away (4.9 s)
  ui-edge-warning-r11-a.ogg "Triple chirp": three soft square blips (A5, A5, E6) on the edge
                            warning's flash cycle (8 game frames on, 8 off = 0.267 s)
  ui-edge-warning-r11-b.ogg "Contact ping": an upward chirp into a ringing ping, with a quieter
                            repeat one flash cycle later

Level 01's launch lasts 5 s and the rail is on screen for the first 4 s
(design/campaign/act-1-first-contact/level-01-break-at-dawn data.yaml); the release lands at 3.6 s.
Levelling as for the recorded sounds (import_sfx.py, band_rms): the 200 Hz-5 kHz band RMS is
normalised, transients above the ceiling are limited. Launch rail: -27 dB band RMS / -4 dBFS ceiling (the UI and
klaxon level, so it sits under the briefing's last radio line). Warning tones: -24 / -2 (the
player-damage level; the mixing rules put warnings with player damage at +2 dB).

Usage: python3 tools/concept/audio/sfx_r11.py [name ...]
Deterministic: every sound uses its own fixed seed.
"""
import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
from import_sfx import band_rms_db  # noqa: E402
from synth import SR, exp_decay, limiter, noise, pulse, reverb, saw, sine, svf, write_ogg  # noqa: E402

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "design" / "audio" / "sfx" / "concept"
RELEASE = 3.6          # s, the ship leaves the rail
LENGTH = 4.9
FLASH = 16 / 60        # the edge warning's flash period (8 on + 8 off game frames at 60 Hz)


def n_of(sec):
    return int(round(sec * SR))


def place(buf, sig, at, gain=1.0):
    i = n_of(at)
    m = min(len(sig), len(buf) - i)
    if m > 0:
        buf[i:i + m] += gain * sig[:m]


def bandpass(x, lo, hi):
    return svf(svf(x, hi), lo, mode="hp")


def stereo(mono, width=0.008, mix=0.0, seed=1):
    """Haas widening plus an optional short room, so a mono source is not dead centre."""
    d = n_of(width)
    st = np.vstack([mono, np.concatenate([np.zeros(d), mono[:-d]])]) * 0.85
    return reverb(st, seconds=0.9, mix=mix, seed=seed)[:, :st.shape[1]] if mix else st


def level(st, band_db, ceiling_db):
    """Band RMS to band_db as import_sfx.normalize() does, but transients above the ceiling
    go through the look-ahead limiter instead of turning the whole sound down (the clunks
    would otherwise leave the launch rail several dB under its band level)."""
    st = st - st.mean(axis=1, keepdims=True)
    for _ in range(3):  # limiting lowers the band level a little; make it up
        st = limiter(st * 10 ** ((band_db - band_rms_db(st)) / 20), ceiling_db)
    return st


def edges(st, fin=0.004, fout=0.25):
    st = st.copy()
    a, b = n_of(fin), n_of(fout)
    st[:, :a] *= np.linspace(0, 1, a)
    st[:, -b:] *= (0.5 + 0.5 * np.cos(np.linspace(0, np.pi, b))) ** 2
    return st


def clank(partials, tau, rng):
    """Inharmonic metal ring: partials (Hz, gain) with slightly different decays."""
    n = n_of(tau * 5)
    out = np.zeros(n)
    for f, g in partials:
        out += g * sine(f * (1 + rng.uniform(-0.004, 0.004)), n) * exp_decay(n, tau * rng.uniform(0.6, 1.2))
    return out


def thump(f0, f1, tau, n_sec=0.6):
    n = n_of(n_sec)
    f = f1 + (f0 - f1) * np.exp(-np.arange(n) / (SR * 0.04))
    return np.sin(2 * np.pi * np.cumsum(f) / SR) * exp_decay(n, tau)


def accel_ticks(total, release, start_rate, end_rate):
    """Times of the rail joints passing under a ship that accelerates evenly to the release:
    joint k passes when the distance travelled reaches k joints."""
    v0, v1 = start_rate, end_rate                      # joints per second at 0 and at the release
    a = (v1 - v0) / release
    times, k = [], 1
    while True:
        t = (-v0 + np.sqrt(v0 * v0 + 2 * a * k)) / a   # v0 t + a t^2 / 2 = k
        if t >= min(release, total):
            return times
        times.append(t)
        k += 1


# --------------------------------------------------------------------------- launch rail

def launch_maglev():
    rng = np.random.default_rng(1101)
    n = n_of(LENGTH)
    t = np.arange(n) / SR
    p = np.clip(t / RELEASE, 0, 1) ** 1.5
    on = np.clip((RELEASE + 0.05 - t) / 0.05, 0, 1)      # the motor lets go at the release
    out = np.zeros(n)
    f = 62 + 300 * p                                      # linear-motor hum follows the speed
    hum = 0.6 * saw(f, n) + 0.4 * pulse(f * 2, n, 0.3)
    hum = svf(hum, 500 + 3500 * p, q=1.1)
    out += hum * (0.25 + 0.75 * p) * on * np.clip(t / 0.25, 0, 1)
    whine = sine(f * 6.02 + 4 * np.sin(2 * np.pi * 5.5 * t), n)
    out += 0.16 * whine * p * on
    for at in accel_ticks(LENGTH, RELEASE, 5.0, 46.0):    # coil segments passing
        tk = bandpass(noise(n_of(0.018), rng), 1800, 4200) * exp_decay(n_of(0.018), 0.004)
        place(out, tk, at, 0.55 + 0.3 * at / RELEASE)
        place(out, thump(180, 120, 0.012, 0.03), at, 0.25)
    place(out, thump(110, 48, 0.12), RELEASE, 1.6)        # release clunk
    place(out, clank([(410, 1.0), (1130, 0.6), (2230, 0.45), (3480, 0.25)], 0.18, rng), RELEASE, 0.55)
    place(out, bandpass(noise(n_of(0.01), rng), 3000, 6000), RELEASE + 0.04, 0.7)  # latch
    wn = n_of(LENGTH - RELEASE)
    wt = np.arange(wn) / SR
    whoosh = noise(wn, rng)
    whoosh = svf(svf(whoosh, 900 + 2600 * np.clip(wt / 0.5, 0, 1), q=1.4), 300, mode="hp")
    place(out, whoosh * np.clip(wt / 0.08, 0, 1) * np.exp(-wt / 0.45), RELEASE + 0.02, 0.5)
    st = stereo(out, mix=0.12, seed=1102)
    return edges(level(st, -27.0, -4.0))


def launch_catapult():
    rng = np.random.default_rng(1111)
    n = n_of(LENGTH)
    t = np.arange(n) / SR
    p = np.clip(t / RELEASE, 0, 1)
    on = np.clip((RELEASE + 0.03 - t) / 0.03, 0, 1)
    out = np.zeros(n)
    hiss = noise(n, rng)
    hiss = svf(svf(hiss, 1200 + 3000 * p ** 1.3, q=1.2), 700, mode="hp")
    out += 0.35 * hiss * (0.15 + 0.85 * p ** 1.2) * on
    rumble = svf(noise(n, rng), 160 + 380 * p, q=1.6)
    out += 0.9 * rumble * (0.3 + 0.7 * p) * on
    out += 0.07 * sine(300 + 900 * p ** 1.4, n) * p * on   # pressure whistle
    for at in accel_ticks(LENGTH, RELEASE, 3.0, 30.0):     # rail joints under the shuttle
        place(out, thump(150, 90, 0.02, 0.06), at, 0.5 + 0.4 * at / RELEASE)
        if rng.random() < 0.7:                               # rattle
            place(out, clank([(1500, 0.5), (2700, 0.35), (3900, 0.2)], 0.012, rng),
                  at + rng.uniform(0.005, 0.03), 0.3)
    place(out, thump(95, 40, 0.16, 0.9), RELEASE, 1.8)       # buffer clunk, two stages
    place(out, thump(140, 60, 0.08), RELEASE + 0.07, 0.9)
    place(out, clank([(290, 1.0), (760, 0.7), (1610, 0.5), (2870, 0.3), (4100, 0.15)], 0.3, rng),
          RELEASE, 0.6)
    vn = n_of(LENGTH - RELEASE)
    vt = np.arange(vn) / SR
    vent = svf(svf(noise(vn, rng), 2400 - 900 * np.clip(vt / 1.2, 0, 1), q=0.9), 500, mode="hp")
    place(out, vent * np.clip(vt / 0.05, 0, 1) * np.exp(-vt / 0.5), RELEASE + 0.05, 0.45)
    st = stereo(out, mix=0.18, seed=1112)
    return edges(level(st, -27.0, -4.0))


# --------------------------------------------------------------------------- edge warning

def blip(freq, dur=0.075):
    n = n_of(dur)
    x = svf(pulse(freq, n, 0.3), 3500)
    env = np.minimum(np.clip(np.arange(n) / n_of(0.003), 0, 1), np.clip((n - np.arange(n)) / n_of(0.02), 0, 1))
    return x * env


def warning_chirp():
    out = np.zeros(n_of(3 * FLASH + 0.15))
    for i, f in enumerate((880.0, 880.0, 1318.5)):
        place(out, blip(f), i * FLASH, 1.0 if i < 2 else 1.1)
        place(out, blip(f * 2, 0.04), i * FLASH, 0.18)         # a bright edge on each blip
    return edges(level(stereo(out, 0.004, mix=0.1, seed=1121), -24.0, -2.0), 0.002, 0.08)


def ping(gain=1.0):
    n = n_of(0.45)
    t = np.arange(n) / SR
    f = 1600 - 900 * np.exp(-t / 0.02)                          # 700 Hz chirping up to 1.6 kHz
    tone = np.sin(2 * np.pi * np.cumsum(f) / SR)
    tone += 0.3 * np.sin(2 * np.pi * np.cumsum(f * 2.01) / SR) * np.exp(-t / 0.06)
    return gain * tone * np.clip(t / 0.002, 0, 1) * np.exp(-t / 0.12)


def warning_ping():
    out = np.zeros(n_of(FLASH + 0.5))
    place(out, ping(), 0.0)
    place(out, ping(0.45), FLASH)
    return edges(level(stereo(out, 0.006, mix=0.22, seed=1131), -24.0, -2.0), 0.002, 0.1)


SOUNDS = {
    "launch-rail-r11-a": launch_maglev,
    "launch-rail-r11-b": launch_catapult,
    "ui-edge-warning-r11-a": warning_chirp,
    "ui-edge-warning-r11-b": warning_ping,
}


def main(names):
    for name in names or SOUNDS:
        st = SOUNDS[name]()
        path = write_ogg(OUT / f"{name}.ogg", st, max_peak_db=-1.0)
        print(f"{path.relative_to(ROOT)}: {st.shape[1] / SR:.2f}s  band {band_rms_db(st):.1f} dB"
              f"  peak {20 * np.log10(np.abs(st).max()):.1f} dBFS")


if __name__ == "__main__":
    main(sys.argv[1:])
