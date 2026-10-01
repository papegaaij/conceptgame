#!/usr/bin/env python3
"""Concept round 08: synthesized pickups and UI sounds.

Pickups and UI stay synthesized (sfx README, "Sourcing"); they extend the family of the three
chosen round-01 pickups: pickup-r01-a (square arpeggio, power-ups), pickup-r01-b (bell chime,
rare upgrades / data core) and pickup-r01-c (rising PWM sweep with sparkles, credits/salvage).

Outputs (design/audio/sfx/concept/):
  pickup-salvage-small-r08-a.ogg   short, high PWM blip with one sparkle (pickup-r01-c family)
  pickup-salvage-large-r08-a.ogg   long PWM sweep up an octave further, sparkle shower
  pickup-shield-cell-r08-a.ogg     cool rising triangle arpeggio with a chorus shimmer
  pickup-armour-patch-r08-a.ogg    low square "clunk" + metallic ding (bolted plate)
  pickup-special-charge-r08-a.ogg  three rising square notes ending in a bell
  ui-menu-move-r08-a.ogg           soft triangle blip
  ui-menu-confirm-r08-a.ogg        two rising square notes
  ui-menu-back-r08-a.ogg           two falling square notes
  ui-shop-buy-r08-a.ogg            blip + coin sparkle ("ka-ching")
  ui-shop-sell-r08-a.ogg           descending coin blips
  ui-shop-denied-r08-a.ogg         low beating square buzz
  ui-typewriter-r08-a.ogg          tiny click-blip for briefing text (plays per character)
  ui-tally-tick-r08-a.ogg          short high tick for debrief counters
  ui-tally-total-r08-a.ogg         major-chord bell stinger when a total lands
  ui-equip-r08-a.ogg               hangar equip: low square clunk + latch click + two-note blip
  ui-upgrade-r08-a.ogg             hangar upgrade: short PWM rise + bell (one step up the ladder)
  ui-grade-stamp-r08-a.ogg         debrief grade stamp: punchy thump + paper slap + low bell

Usage: python3 tools/concept/audio/sfx_r08.py [name ...]
Deterministic: every sound uses its own fixed seed.
"""
import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
from synth import (SR, adsr, delay, exp_decay, fade, noise, normalize_peak, note_freq,  # noqa: E402
                   pan, pulse, ramp, sine, svf, triangle, write_ogg)

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "design" / "audio" / "sfx" / "concept"

PEAK_PICKUP, PEAK_UI, PEAK_TICK = -4.0, -8.0, -12.0


def n_of(sec):
    return int(sec * SR)


def st(mono, width=0.0):
    if not width:
        return pan(mono, 0)
    d = int(width * 0.012 * SR)
    return np.vstack([mono, np.concatenate([np.zeros(d), mono[:-d]])]) * 0.8


def sparkles(n, count, rng, lo=0.1, hi=0.8, level=0.35):
    out = np.zeros(n)
    for _ in range(count):
        at = int(rng.uniform(lo, hi) * n)
        ln = n_of(0.03)
        seg = sine(rng.uniform(3000, 6500), ln) * exp_decay(ln, 0.006)
        out[at:at + ln] += seg[: n - at] * level
    return out


def pwm_sweep(n, f0, f1, gate, rng_rate=9):
    t = np.arange(n) / SR
    f = ramp(n, f0, f1) * (1 + 0.03 * np.sin(2 * np.pi * 18 * t))
    x = pulse(f, n, width=0.5 + 0.35 * np.sin(2 * np.pi * rng_rate * t))
    return svf(x, ramp(n, 1500, 8500), q=1.5) * adsr(n, a=0.006, d=0.08, s=0.8, r=0.08, gate=gate)


def note(name, dur, wave="pulse", width=0.25, gate=None, d=0.06, s=0.3, r=0.04):
    n = n_of(dur)
    f = note_freq(name)
    osc = {"pulse": lambda: pulse(f, n, width=width), "tri": lambda: triangle(f, n),
           "sine": lambda: sine(f, n)}[wave]()
    return osc * adsr(n, a=0.002, d=d, s=s, r=r, gate=gate if gate is not None else dur * 0.6)


def seq(parts, gap, tail=0.15):
    """parts: list of mono arrays started every `gap` seconds."""
    step = n_of(gap)
    n = step * (len(parts) - 1) + max(len(p) for p in parts) + n_of(tail)
    out = np.zeros(n)
    for i, p in enumerate(parts):
        out[i * step:i * step + len(p)] += p
    return out


def bell(name, dur=0.7, level=1.0):
    """Struck bell; the last 30 % is a raised-cosine release so a short `dur` never ends in
    a click (a hard cut while still ringing showed up as a broadband line)."""
    n = n_of(dur)
    f0 = note_freq(name)
    b = sum(a * sine(f0 * r, n) * exp_decay(n, tau)
            for r, a, tau in [(1, 1.0, 0.25), (2.76, 0.4, 0.09), (5.4, 0.25, 0.05), (8.93, 0.12, 0.03)])
    rel = int(n * 0.3)
    env = np.ones(n)
    env[-rel:] = 0.5 + 0.5 * np.cos(np.linspace(0, np.pi, rel))
    return b * np.minimum(1, np.arange(n) / (0.001 * SR)) * env * level


# ---------------------------------------------------------------- pickups

def salvage_small():
    rng = np.random.default_rng(81)
    n = n_of(0.22)
    x = pwm_sweep(n, 660, 1760, gate=0.13) * 0.8 + sparkles(n, 2, rng, 0.3, 0.7)
    return fade(st(x, 0.15), 0.002, 0.03)


def salvage_large():
    rng = np.random.default_rng(82)
    n = n_of(0.75)
    x = pwm_sweep(n, 262, 2093, gate=0.5) * 0.7
    x += 0.35 * pwm_sweep(n, 524, 4186, gate=0.5)          # octave above for brightness
    x += sparkles(n, 18, rng, 0.15, 0.95, 0.4)
    return fade(delay(st(x, 0.25), 0.08, feedback=0.25, mix=0.18, damp=6000, tail=0.15), 0.002, 0.08)


def shield_cell():
    parts = [note(nt, 0.16, "tri", gate=0.09, s=0.4) for nt in ["G5", "B5", "D6", "G6"]]
    x = seq(parts, 0.045, tail=0.2)
    t = np.arange(len(x)) / SR
    shimmer = sine(note_freq("G7") * (1 + 0.004 * np.sin(2 * np.pi * 6 * t)), len(x)) * \
        ramp(len(x), 0.0, 1.0, "lin") * exp_decay(len(x), 0.25) * 0.25
    s = fade(st(svf(x, 6000) + shimmer, 0.3), 0.001, 0.12)  # shimmer would end in a click
    return fade(delay(s, 0.07, feedback=0.3, mix=0.22, damp=7000, tail=0.15), 0.001, 0.06)


def armour_patch():
    rng = np.random.default_rng(84)
    n = n_of(0.7)  # long enough for the ding to ring out
    clunk = pulse(ramp(n, 180, 90), n, width=0.4) * exp_decay(n, 0.04)
    clunk = svf(clunk + 0.5 * noise(n, rng) * exp_decay(n, 0.01), 1800)
    ding = np.zeros(n)
    at = n_of(0.07)
    ding[at:] = bell("A5", (n - at) / SR, 0.7)
    return fade(st(clunk * 0.9 + ding, 0.15), 0.001, 0.05)


def special_charge():
    parts = [note(nt, 0.1, "pulse", width=0.5, gate=0.06) for nt in ["E5", "A5", "E6"]]
    x = seq(parts, 0.06, tail=0.0)
    b = bell("A6", 0.6)
    out = np.zeros(len(x) + len(b))
    out[:len(x)] += svf(x, 6000)
    out[n_of(0.18):n_of(0.18) + len(b)] += b
    return fade(delay(st(out, 0.2), 0.1, feedback=0.3, mix=0.2, damp=6000, tail=0.1), 0.001, 0.08)


# ---------------------------------------------------------------- UI

def menu_move():
    n = n_of(0.05)
    x = triangle(ramp(n, 1500, 1300), n) * adsr(n, a=0.001, d=0.02, s=0.2, r=0.02, gate=0.02)
    return fade(st(x), 0.001, 0.01)


def menu_confirm():
    x = seq([note("E6", 0.07, gate=0.04), note("B6", 0.12, gate=0.06)], 0.06, tail=0.05)
    return fade(st(svf(x, 7000)), 0.001, 0.02)


def menu_back():
    x = seq([note("B5", 0.07, gate=0.04), note("E5", 0.12, gate=0.06)], 0.06, tail=0.05)
    return fade(st(svf(x, 5000)), 0.001, 0.02)


def shop_buy():
    rng = np.random.default_rng(91)
    x = seq([note("C6", 0.06, gate=0.035), note("G6", 0.25, gate=0.05, s=0.2)], 0.05, tail=0.1)
    x = svf(x, 7000) + sparkles(len(x), 6, rng, 0.25, 0.8, 0.45)
    return fade(delay(st(x, 0.2), 0.07, feedback=0.25, mix=0.15, damp=6000, tail=0.1), 0.001, 0.05)


def shop_sell():
    rng = np.random.default_rng(92)
    x = seq([note(nt, 0.07, gate=0.035) for nt in ["G6", "D6", "G5"]], 0.055, tail=0.1)
    x = svf(x, 6000) + sparkles(len(x), 3, rng, 0.1, 0.5, 0.3)
    return fade(st(x, 0.15), 0.001, 0.05)


def shop_denied():
    n = n_of(0.28)
    x = pulse(110, n, width=0.5) + pulse(116.5, n, width=0.35)
    x = svf(x, 1400) * adsr(n, a=0.003, d=0.05, s=0.8, r=0.05, gate=0.22)
    return fade(st(x), 0.002, 0.03)


def typewriter():
    rng = np.random.default_rng(93)
    n = n_of(0.03)
    x = pulse(2200, n, width=0.3) * exp_decay(n, 0.008) * 0.6
    x += svf(noise(n, rng), 5000, mode="hp") * exp_decay(n, 0.002) * 0.5
    return fade(st(x), 0.0005, 0.005)


def tally_tick():
    n = n_of(0.03)
    return fade(st(sine(3200, n) * exp_decay(n, 0.006)), 0.0005, 0.005)


def tally_total():
    chord = sum(bell(nt, 1.2, lv) for nt, lv in [("C6", 1.0), ("E6", 0.8), ("G6", 0.8), ("C7", 0.5)])
    hit = note("C5", 0.25, "pulse", width=0.5, gate=0.1)
    out = chord.copy()
    out[:len(hit)] += svf(hit, 3000) * 0.6
    return fade(delay(st(out, 0.3), 0.11, feedback=0.3, mix=0.2, damp=6000, tail=0.2), 0.001, 0.2)


def equip():
    """Hangar: an item is bolted onto a mount. Clunk of pickup-armour-patch, shorter and
    lower, a latch click, then the rising two-note blip of ui-menu-confirm, quieter."""
    rng = np.random.default_rng(94)
    n = n_of(0.36)
    clunk = pulse(ramp(n, 220, 110), n, width=0.4) * exp_decay(n, 0.03)
    clunk = svf(clunk + 0.4 * noise(n, rng) * exp_decay(n, 0.008), 1600)
    latch = np.zeros(n)
    at = n_of(0.05)
    ln = n_of(0.02)
    latch[at:at + ln] = svf(noise(ln, rng), 3500, q=2.0, mode="bp") * exp_decay(ln, 0.003)
    blip = seq([note("G5", 0.06, gate=0.03), note("D6", 0.1, gate=0.05)], 0.05, tail=0.03)
    out = clunk + latch * 0.8
    at = n_of(0.1)
    out[at:at + len(blip)] += svf(blip, 6000)[: n - at] * 0.45
    return fade(st(out, 0.15), 0.001, 0.04)


def upgrade():
    """Hangar: a part goes up one level. Short PWM rise (pickup-r01-c family) that lands on
    a bell a fifth up; brighter than ui-shop-buy, shorter than ui-tally-total."""
    rng = np.random.default_rng(95)
    n = n_of(0.75)
    rise = np.zeros(n)
    rise[:n_of(0.2)] = pwm_sweep(n_of(0.2), 523, 1568, gate=0.14) * 0.6
    ding = np.zeros(n)
    at = n_of(0.14)
    ding[at:] = bell("G6", (n - at) / SR, 0.9) + bell("D7", (n - at) / SR, 0.35)
    x = rise + ding + sparkles(n, 4, rng, 0.2, 0.5, 0.3)
    return fade(delay(st(x, 0.25), 0.09, feedback=0.25, mix=0.18, damp=6000, tail=0.1), 0.001, 0.08)


def grade_stamp():
    """Debrief: the grade letter is stamped onto the report. A punchy thump with a 200-400 Hz
    body (audible on small speakers, not only sub), a paper slap and a short low bell."""
    rng = np.random.default_rng(96)
    n = n_of(0.6)
    thump = sine(ramp(n, 160, 50, tau=0.03), n) * exp_decay(n, 0.07)
    body = svf(pulse(ramp(n, 300, 140, tau=0.04), n, width=0.45), 1200) * exp_decay(n, 0.04)
    slap = svf(noise(n, rng), 1800, q=0.9, mode="bp") * exp_decay(n, 0.012)
    ring = bell("C5", 0.6, 0.35)
    x = thump * 0.45 + body * 1.0 + slap * 1.0 + ring * 1.4
    return fade(delay(st(x, 0.1), 0.06, feedback=0.15, mix=0.12, damp=4000, tail=0.08), 0.0005, 0.08)


SOUNDS = {
    "pickup-salvage-small-r08-a": (salvage_small, PEAK_PICKUP),
    "pickup-salvage-large-r08-a": (salvage_large, PEAK_PICKUP - 2),
    "pickup-shield-cell-r08-a": (shield_cell, PEAK_PICKUP),
    "pickup-armour-patch-r08-a": (armour_patch, PEAK_PICKUP),
    "pickup-special-charge-r08-a": (special_charge, PEAK_PICKUP),
    "ui-menu-move-r08-a": (menu_move, PEAK_UI - 4),
    "ui-menu-confirm-r08-a": (menu_confirm, PEAK_UI),
    "ui-menu-back-r08-a": (menu_back, PEAK_UI),
    "ui-shop-buy-r08-a": (shop_buy, PEAK_UI),
    "ui-shop-sell-r08-a": (shop_sell, PEAK_UI),
    "ui-shop-denied-r08-a": (shop_denied, PEAK_UI - 2),
    "ui-typewriter-r08-a": (typewriter, PEAK_TICK),
    "ui-tally-tick-r08-a": (tally_tick, PEAK_TICK),
    "ui-tally-total-r08-a": (tally_total, PEAK_UI),
    "ui-equip-r08-a": (equip, PEAK_UI),
    "ui-upgrade-r08-a": (upgrade, PEAK_UI),
    "ui-grade-stamp-r08-a": (grade_stamp, PEAK_UI + 2),
}


def main(names):
    for name, (fn, peak) in SOUNDS.items():
        if names and name not in names:
            continue
        sig = fn()
        sig = sig - sig.mean(axis=1, keepdims=True)
        sig = fade(normalize_peak(sig, peak), 0.0005, 0.005)
        path = write_ogg(OUT / f"{name}.ogg", sig, max_peak_db=peak)
        print(f"{path.relative_to(ROOT)}  {sig.shape[1] / SR:.2f}s")


if __name__ == "__main__":
    main(sys.argv[1:])
