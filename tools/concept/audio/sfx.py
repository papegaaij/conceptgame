#!/usr/bin/env python3
"""Concept round 01 sound effects.

Outputs (design/audio/sfx/concept/):
  player-shot-r01-a.ogg   classic square-wave laser "pew"
  player-shot-r01-b.ogg   pulse cannon: thump + short noise snap
  player-shot-r01-c.ogg   plasma bolt: resonant-swept saw "zwip"
  explosion-r01-a.ogg     small, crunchy, bit-crushed 8-bit style pop
  explosion-r01-b.ogg     big boomy blast with sub drop and debris tail
  explosion-r01-c.ogg     sci-fi plasma detonation: resonant sweep + ring-mod whoom
  pickup-r01-a.ogg        4-note square arpeggio power-up
  pickup-r01-b.ogg        bright bell chime pair with echo
  pickup-r01-c.ogg        rising PWM sweep with sparkles (credits)

Usage: python3 tools/concept/audio/sfx.py [OUTDIR]
Deterministic: every sound uses its own fixed seed.
"""
import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
from synth import (SR, adsr, bitcrush, delay, exp_decay, fade, noise, normalize_peak,  # noqa: E402
                   note_freq, pan, pulse, ramp, reverb, saturate, saw, sine, svf,
                   triangle, write_ogg)

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "design" / "audio" / "sfx" / "concept"

# Repeated sounds must be quieter than one-off impacts (see prompts.md).
PEAK_SHOT, PEAK_PICKUP, PEAK_EXPLOSION = -10.0, -4.0, -1.5


def n_of(sec):
    return int(sec * SR)


def stereo(mono, width=0.0, seed=0):
    """Mono to stereo with an optional tiny Haas spread for width."""
    if not width:
        return pan(mono, 0)
    d = int(width * 0.012 * SR)
    left = mono
    right = np.concatenate([np.zeros(d), mono[:-d]]) if d else mono
    return np.vstack([left, right]) * 0.8


# ---------------------------------------------------------------- player shots

def shot_a():
    n = n_of(0.14)
    f = ramp(n, 1900, 380)
    x = pulse(f, n, width=ramp(n, 0.5, 0.2, "lin"))
    x = svf(x, ramp(n, 9000, 1500), q=0.9)
    x *= adsr(n, a=0.001, d=0.05, s=0.35, r=0.06, gate=0.07)
    return stereo(fade(x, 0.001, 0.01))


def shot_b():
    rng = np.random.default_rng(12)
    n = n_of(0.12)
    body = sine(ramp(n, 240, 70), n) * exp_decay(n, 0.035)
    snap = svf(noise(n, rng), 3200, q=1.4, mode="bp") * exp_decay(n, 0.008)
    tick = triangle(ramp(n, 1400, 600), n) * exp_decay(n, 0.012) * 0.4
    x = saturate(body * 1.2 + snap * 0.9 + tick, 1.5)
    return stereo(fade(x, 0.0005, 0.01))


def shot_c():
    n = n_of(0.2)
    t = np.arange(n) / SR
    f = ramp(n, 520, 1250, tau=0.02) * (1 + 0.04 * np.sin(2 * np.pi * 38 * t)) * ramp(n, 1, 0.55)
    x = saw(f, n) * 0.6 + saw(f * 1.5, n) * 0.3
    x = svf(x, ramp(n, 600, 6500, tau=0.03) * ramp(n, 1, 0.3), q=4.0)
    x *= adsr(n, a=0.002, d=0.08, s=0.4, r=0.06, gate=0.11)
    st = stereo(fade(x, 0.001, 0.01), width=0.4)
    st = delay(st, 0.045, feedback=0.25, mix=0.25, damp=3000, tail=0.03)
    return fade(st, 0.001, 0.02)


# ---------------------------------------------------------------- explosions

def explosion_a():
    rng = np.random.default_rng(21)
    n = n_of(0.62)
    x = svf(noise(n, rng), ramp(n, 7000, 250), q=1.2)
    thump = sine(ramp(n, 180, 45), n) * exp_decay(n, 0.06)
    x = (x * 1.3 + thump) * adsr(n, a=0.001, d=0.12, s=0.35, r=0.3, gate=0.25)
    x = bitcrush(saturate(x, 2.5), bits=6, downsample=4)
    x = svf(x, 7500, q=0.7)  # tame the crush aliasing a bit
    return stereo(fade(x, 0.0005, 0.04), width=0.3)


def explosion_b():
    rng = np.random.default_rng(22)
    n = n_of(1.45)
    t = np.arange(n) / SR
    sub = sine(ramp(n, 95, 28, tau=0.12), n) * exp_decay(n, 0.28)
    brown = np.cumsum(noise(n, rng))
    brown -= np.convolve(brown, np.ones(2048) / 2048, mode="same")  # remove drift
    brown /= np.max(np.abs(brown)) + 1e-9
    body = svf(brown, ramp(n, 2600, 180), q=0.8) * exp_decay(n, 0.35)
    crack = svf(noise(n, rng), 5000, q=0.7, mode="hp") * exp_decay(n, 0.03)
    debris = np.zeros(n)
    for _ in range(38):  # sparse crackles falling off over time
        at = int(rng.beta(1.3, 3.0) * n * 0.9)
        ln = rng.integers(200, 900)
        seg = svf(noise(ln, rng), rng.uniform(1800, 6000), q=2.0, mode="bp")
        seg *= exp_decay(ln, rng.uniform(0.002, 0.006))
        debris[at:at + ln] += seg[: n - at] * rng.uniform(0.15, 0.5) * (1 - at / n)
    x = saturate(sub * 1.1 + body * 1.2 + crack * 0.6, 1.8) + debris
    x *= np.minimum(1, t / 0.002)
    st = np.vstack([x, np.concatenate([np.zeros(n_of(0.007)), x[:-n_of(0.007)]])]) * 0.8
    st = reverb(st, seconds=1.2, mix=0.18, damping=0.7, seed=5)[:, :n]
    return fade(st, 0.0005, 0.15)


def explosion_c():
    rng = np.random.default_rng(23)
    n = n_of(1.25)
    t = np.arange(n) / SR
    whoom = svf(noise(n, rng), ramp(n, 300, 5000, tau=0.05) * ramp(n, 1, 0.12), q=6.0)
    whoom *= adsr(n, a=0.004, d=0.25, s=0.3, r=0.5, gate=0.45)
    tone = sine(ramp(n, 880, 55), n) * sine(ramp(n, 330, 140), n)  # ring modulation
    tone *= exp_decay(n, 0.25)
    shimmer = saw(ramp(n, 1600, 400), n) * (0.5 + 0.5 * np.sin(2 * np.pi * 23 * t))
    shimmer = svf(shimmer, 3000, q=2.5, mode="bp") * exp_decay(n, 0.18) * 0.5
    thump = sine(ramp(n, 120, 35, tau=0.08), n) * exp_decay(n, 0.12)
    x = saturate(whoom * 0.9 + tone * 0.7 + shimmer + thump, 1.4)
    st = np.vstack([x * (1 + 0.3 * np.sin(2 * np.pi * 3 * t)),
                    x * (1 - 0.3 * np.sin(2 * np.pi * 3 * t))]) * 0.75  # swirl
    st = delay(st, 0.11, feedback=0.3, mix=0.25, damp=2500, tail=0)[:, :n]
    return fade(st, 0.001, 0.15)


# ---------------------------------------------------------------- pickups

def pickup_a():
    notes = ["C6", "E6", "G6", "C7"]
    step = n_of(0.055)
    n = step * len(notes) + n_of(0.15)
    x = np.zeros(n)
    for i, nt in enumerate(notes):
        ln = n - i * step
        v = pulse(note_freq(nt), ln, width=0.25) * adsr(ln, a=0.002, d=0.08, s=0.3, r=0.05,
                                                         gate=0.05 if i < 3 else 0.12)
        x[i * step:] += v * (0.8 if i < 3 else 1.0)
    x = svf(x, 7000, q=0.7)
    st = delay(stereo(x), 0.09, feedback=0.3, mix=0.2, damp=5000, tail=0.2)
    return fade(st, 0.001, 0.05)


def pickup_b():
    n = n_of(0.75)
    x = np.zeros(n)
    for k, (nt, at) in enumerate([("E6", 0.0), ("B6", 0.07)]):
        f0 = note_freq(nt)
        ln = n - n_of(at)
        bell = sum(a * sine(f0 * r, ln) * exp_decay(ln, tau)
                   for r, a, tau in [(1, 1.0, 0.25), (2.76, 0.4, 0.09), (5.4, 0.25, 0.05),
                                     (8.93, 0.12, 0.03)])
        bell *= np.minimum(1, np.arange(ln) / (0.001 * SR))
        x[n_of(at):] += bell * (0.8 if k == 0 else 1.0)
    st = delay(stereo(x, width=0.3), 0.12, feedback=0.35, mix=0.25, damp=6000, tail=0)
    return fade(st[:, :n], 0.001, 0.1)


def pickup_c():
    rng = np.random.default_rng(33)
    n = n_of(0.5)
    t = np.arange(n) / SR
    f = ramp(n, 330, 1320) * (1 + 0.03 * np.sin(2 * np.pi * 18 * t))
    x = pulse(f, n, width=0.5 + 0.35 * np.sin(2 * np.pi * 9 * t))
    x = svf(x, ramp(n, 1200, 8000), q=1.5) * adsr(n, a=0.01, d=0.1, s=0.8, r=0.12, gate=0.34)
    spark = np.zeros(n)
    for _ in range(9):
        at = int(rng.uniform(0.1, 0.8) * n)
        ln = n_of(0.03)
        seg = sine(rng.uniform(3000, 6000), ln) * exp_decay(ln, 0.006)
        spark[at:at + ln] += seg[: n - at] * 0.35
    st = stereo(x * 0.8, width=0.2) + pan(spark, rng.uniform(-0.6, 0.6))
    return fade(st, 0.002, 0.03)


SOUNDS = {
    "player-shot-r01-a": (shot_a, PEAK_SHOT),
    "player-shot-r01-b": (shot_b, PEAK_SHOT),
    "player-shot-r01-c": (shot_c, PEAK_SHOT),
    "explosion-r01-a": (explosion_a, PEAK_EXPLOSION),
    "explosion-r01-b": (explosion_b, PEAK_EXPLOSION),
    "explosion-r01-c": (explosion_c, PEAK_EXPLOSION),
    "pickup-r01-a": (pickup_a, PEAK_PICKUP),
    "pickup-r01-b": (pickup_b, PEAK_PICKUP),
    "pickup-r01-c": (pickup_c, PEAK_PICKUP - 3),  # sustained tone: sounds louder
}


def main():
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else OUT
    for name, (fn, peak) in SOUNDS.items():
        sig = fn()
        sig = sig - sig.mean(axis=1, keepdims=True)
        sig = fade(normalize_peak(sig, peak), 0.0005, 0.005)
        path = write_ogg(out / f"{name}.ogg", sig)
        print(f"{path.relative_to(ROOT) if path.is_relative_to(ROOT) else path}  "
              f"{sig.shape[1] / SR:.2f}s")


if __name__ == "__main__":
    main()
