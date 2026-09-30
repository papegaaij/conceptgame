#!/usr/bin/env python3
"""Concept round 01 music sketches.

Outputs (design/audio/music/concept/):
  music-r01-a.ogg   "Afterburner" - tracker-style trance/techno level theme, 140 BPM, A minor
  music-r01-b.ogg   "Coalition Rising" - synth-orchestral action level theme, 132 BPM, D minor

Usage: python3 tools/concept/audio/music.py [a|b ...] [--out OUTDIR]
Both pieces are written as tracker-style patterns (one token per 16th step, see
synth.parse_track) and rendered with the numpy synth; mastered to about -14 LUFS with a
-2 dBFS ceiling (true peak about -1 dBTP). Deterministic (fixed seeds).
"""
import sys
from functools import lru_cache
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
from synth import (SR, Timeline, adsr, delay, exp_decay, fade, master, noise,  # noqa: E402
                   note_freq, note_number, parse_track, pulse, ramp, reverb, saturate,
                   saw, sine, supersaw, svf, write_ogg)

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "design" / "audio" / "music" / "concept"


def ns(sec):
    return max(1, int(sec * SR))


def transpose(name, semis):
    midi = note_number(name) + semis
    names = ["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"]
    return f"{names[midi % 12]}{midi // 12 - 1}"


# ================================================================ drum samples
# Rendered once (like samples in a tracker module) and re-triggered.

@lru_cache(None)
def kick(tone=160):
    n = ns(0.4)
    body = sine(ramp(n, tone, 52, tau=0.03), n) * exp_decay(n, 0.09)
    click = svf(noise(ns(0.004), np.random.default_rng(1)), 4000, mode="hp")
    body[: len(click)] += click * 0.5
    return fade(saturate(body * 1.4, 1.6), 0.0005, 0.02)


@lru_cache(None)
def clap():
    rng = np.random.default_rng(2)
    n = ns(0.3)
    x = np.zeros(n)
    for i, off in enumerate([0, 0.011, 0.022]):
        ln = n - ns(off)
        x[ns(off):] += noise(ln, rng) * exp_decay(ln, 0.006 if i < 2 else 0.09)
    return fade(svf(x, 1300, q=1.3, mode="bp") * 2.2, 0.0005, 0.02)


@lru_cache(None)
def snare(tight=False):
    rng = np.random.default_rng(3)
    n = ns(0.25 if tight else 0.35)
    tone = sine(ramp(n, 210, 175), n) * exp_decay(n, 0.04)
    rattle = svf(noise(n, rng), 3500, q=0.8, mode="bp") * exp_decay(n, 0.05 if tight else 0.11)
    return fade(saturate(tone * 0.8 + rattle * 1.6, 1.3), 0.0005, 0.02)


@lru_cache(None)
def hat(open_=False):
    rng = np.random.default_rng(4 if open_ else 5)
    n = ns(0.25 if open_ else 0.06)
    x = svf(noise(n, rng), 8500, q=0.9, mode="hp")
    return fade(x * exp_decay(n, 0.07 if open_ else 0.014), 0.0005, 0.01)


@lru_cache(None)
def crash():
    rng = np.random.default_rng(6)
    n = ns(2.2)
    x = svf(noise(n, rng), 5200, q=0.7, mode="hp") * 0.8
    x += svf(noise(n, rng), 7200, q=3.0, mode="bp") * 0.5
    return fade(x * exp_decay(n, 0.55), 0.001, 0.2)


@lru_cache(None)
def tom(pitch=120):
    n = ns(0.4)
    x = sine(ramp(n, pitch * 1.4, pitch, tau=0.03), n) * exp_decay(n, 0.12)
    x += svf(noise(n, np.random.default_rng(7)), 900, q=1.0, mode="bp") * exp_decay(n, 0.02)
    return fade(saturate(x * 1.3, 1.4), 0.0005, 0.03)


@lru_cache(None)
def taiko():
    n = ns(0.9)
    x = sine(ramp(n, 100, 60, tau=0.05), n) * exp_decay(n, 0.19)
    x += svf(noise(n, np.random.default_rng(8)), 600, q=0.8) * exp_decay(n, 0.05) * 1.5
    return fade(saturate(x * 1.5, 1.8), 0.0005, 0.05)


@lru_cache(None)
def timpani(note="D2", dur=1.6):
    n = ns(dur)
    f = note_freq(note)
    x = sum(a * sine(f * r * ramp(n, 1.02, 1.0, tau=0.05), n) * exp_decay(n, tau)
            for r, a, tau in [(1, 1.0, 0.7), (1.5, 0.5, 0.45), (1.98, 0.3, 0.3), (2.44, 0.15, 0.2)])
    x += svf(noise(n, np.random.default_rng(9)), 400, q=0.9) * exp_decay(n, 0.03) * 1.2
    return fade(x, 0.0005, 0.1)


@lru_cache(None)
def riser(seconds):
    n = ns(seconds)
    rng = np.random.default_rng(10)
    x = svf(noise(n, rng), ramp(n, 300, 9000), q=3.0) * np.linspace(0, 1, n) ** 2
    return fade(x, 0.01, 0.01)


# ================================================================ melodic voices

@lru_cache(None)
def v_bass(freq, dur):
    n = ns(dur + 0.05)
    x = saw(freq, n) * 0.7 + pulse(freq, n, 0.5) * 0.4
    x = svf(x, 350 + 2200 * exp_decay(n, 0.05), q=1.8)
    return saturate(x * adsr(n, 0.002, 0.08, 0.6, 0.04, gate=dur), 1.5)


@lru_cache(None)
def v_arp(freq, dur, cutoff):
    n = ns(dur + 0.08)
    x = pulse(freq, n, 0.3)
    x = svf(x, cutoff * (0.5 + exp_decay(n, 0.04)), q=2.5)
    return x * adsr(n, 0.001, 0.06, 0.3, 0.05, gate=dur)


@lru_cache(None)
def v_lead(freq, dur):
    n = ns(dur + 0.25)
    t = np.arange(n) / SR
    vib = 1 + 0.006 * np.sin(2 * np.pi * 5.5 * t) * np.clip((t - 0.18) / 0.2, 0, 1)
    f = freq * vib * ramp(n, 0.985, 1.0, tau=0.015)
    x = supersaw(f, n, voices=7, detune=0.014, rng=np.random.default_rng(11))
    x += pulse(f * 2, n, 0.5) * 0.15
    x = svf(x, 2200 + 3500 * exp_decay(n, 0.12), q=1.0)
    return x * adsr(n, 0.006, 0.25, 0.7, 0.2, gate=dur)


@lru_cache(None)
def v_pad(freq, dur, bright=1800.0):
    n = ns(dur + 0.7)
    x = supersaw(freq, n, voices=5, detune=0.01, rng=np.random.default_rng(12))
    x = svf(x, bright, q=0.8)
    return x * adsr(n, 0.25, 0.4, 0.8, 0.6, gate=dur)


@lru_cache(None)
def v_strings_short(freq, dur):
    n = ns(dur + 0.1)
    x = supersaw(freq, n, voices=3, detune=0.006, rng=np.random.default_rng(13))
    x = svf(x, 1800 + 1400 * exp_decay(n, 0.03), q=0.9)
    return x * adsr(n, 0.006, 0.08, 0.55, 0.07, gate=dur)


@lru_cache(None)
def v_strings_long(freq, dur, attack=0.18):
    n = ns(dur + 0.6)
    t = np.arange(n) / SR
    f = freq * (1 + 0.004 * np.sin(2 * np.pi * 5.2 * t + freq))
    x = supersaw(f, n, voices=5, detune=0.008, rng=np.random.default_rng(14))
    x = svf(x, 3200, q=0.7)
    return x * adsr(n, attack, 0.3, 0.85, 0.5, gate=dur)


@lru_cache(None)
def v_brass(freq, dur, stab=False):
    n = ns(dur + (0.12 if stab else 0.25))
    t = np.arange(n) / SR
    vib = 1 + 0.005 * np.sin(2 * np.pi * 5.0 * t) * np.clip((t - 0.25) / 0.3, 0, 1)
    f = freq * vib * ramp(n, 0.94, 1.0, tau=0.025)  # lip scoop into the note
    x = supersaw(f, n, voices=3, detune=0.004, rng=np.random.default_rng(15))
    x += pulse(f, n, 0.42) * 0.4
    cut = (500 + 2600 * (1 - exp_decay(n, 0.02)) * (0.55 + 0.45 * exp_decay(n, 0.15)))
    x = svf(x, cut * (1.3 if stab else 1.0), q=1.1)
    env = adsr(n, 0.004 if stab else 0.03, 0.12, 0.45 if stab else 0.8, 0.1 if stab else 0.2,
               gate=dur)
    return saturate(x * env * 1.3, 1.3)


# ================================================================ helpers

class Song:
    def __init__(self, bpm, bars, tail):
        self.step = 60 / bpm / 4
        self.bar = self.step * 16
        self.tl = Timeline(bars * self.bar + tail)

    def at(self, bar, step=0):
        return bar * self.bar + step * self.step

    def hit(self, group, sample, bar, step=0, gain=1.0, position=0.0):
        self.tl.add(group, sample, self.at(bar, step), position, gain)

    def play(self, group, voice, bars_tokens, start_bar=0, gain=1.0, position=0.0,
             octave=0, legato=0.9, **kw):
        for st, ln, name in parse_track(bars_tokens):
            name = transpose(name, 12 * octave) if octave else name
            sig = voice(note_freq(name), round(ln * self.step * legato, 4), **kw)
            self.tl.add(group, sig, self.at(start_bar) + st * self.step, position, gain)

    def chord(self, group, voice, notes, bar, step=0, dur_steps=16, gain=1.0, spread=0.6, **kw):
        k = len(notes)
        for i, nm in enumerate(notes):
            pos = (i / (k - 1) * 2 - 1) * spread if k > 1 else 0
            sig = voice(note_freq(nm), round(dur_steps * self.step, 4), **kw)
            self.tl.add(group, sig, self.at(bar, step), pos, gain / np.sqrt(k))


def hp(stereo, cutoff):
    return np.vstack([svf(ch, cutoff, q=0.707, mode="hp") for ch in stereo])


def widen(stereo, amount):
    mid, side = (stereo[0] + stereo[1]) / 2, (stereo[0] - stereo[1]) / 2 * amount
    return np.vstack([mid + side, mid - side])


def sidechain(n, times, depth=0.55, release=0.13):
    env = np.ones(n)
    m = ns(release * 4)
    shape = 1 - depth * np.exp(-np.arange(m) / (release * SR))
    shape[:ns(0.004)] = np.linspace(1, shape[ns(0.004)], ns(0.004))
    for t in times:
        i = int(t * SR)
        seg = env[i:i + m]
        env[i:i + m] = np.minimum(seg, shape[: len(seg)])
    return env


# ================================================================ A: trance / techno

A_CHORDS = {  # pad voicing, bass root, arp tones
    "Am": (["A3", "C4", "E4"], "A1", ["A4", "C5", "E5"]),
    "F": (["F3", "A3", "C4"], "F1", ["F4", "A4", "C5"]),
    "C": (["G3", "C4", "E4"], "C2", ["G4", "C5", "E5"]),
    "G": (["G3", "B3", "D4"], "G1", ["G4", "B4", "D5"]),
    "E": (["G#3", "B3", "E4"], "E1", ["G#4", "B4", "E5"]),
    "Dm": (["F3", "A3", "D4"], "D2", ["F4", "A4", "D5"]),
}
A_PROG = (["Am", "F", "C", "G"]                                   # intro   bars 0-3
          + ["Am", "F", "C", "G", "Am", "F", "G", "E"]            # A       bars 4-11
          + ["Dm", "Am", "F", "E", "Dm", "Am", "F", "E"])         # B       bars 12-19
A_LEAD = [
    # A section (bars 4-11)
    "A4 - - C5 - - E5 - D5 - C5 - B4 - C5 -",
    "A4 - - - - - . . F4 - A4 - C5 - A4 -",
    "G4 - - C5 - - E5 - G5 - - - E5 - D5 -",
    "D5 - - - B4 - - - G4 - - - . . . .",
    "A4 - - C5 - - E5 - D5 - C5 - B4 - C5 -",
    "A4 - - C5 - - F5 - E5 - C5 - A4 - C5 -",
    "D5 - - - - - B4 - D5 - G5 - - - F5 -",
    "E5 - - - - - - - G#5 - - - B5 - - -",
    # B section (bars 12-19)
    "F5 - - - E5 - D5 - A5 - - - - - F5 -",
    "E5 - - - - - - - C5 - D5 - E5 - - -",
    "F5 - - - G5 - A5 - C6 - - - A5 - F5 -",
    "G#5 - - - - - - - E5 - - - B4 - - -",
    "F5 - - - E5 - D5 - A5 - - - D6 - - -",
    "C6 - - - B5 - A5 - E5 - - - A5 - - -",
    "A5 - - - F5 - - - B5 - - - G5 - - -",
    "G#5 - - - - - - - - - - - . . . .",
]
ARP_STEPS = [0, 1, 2, 3, 2, 1, 0, 1, 0, 1, 2, 3, 4, 3, 2, 1]


def song_a():
    s = Song(140, 21, tail=1.5)
    kicks = []
    for bar, name in enumerate(A_PROG):
        pad, root, tones = A_CHORDS[name]
        ext = tones + [transpose(tones[0], 12), transpose(tones[1], 12)]
        intro, sec_b = bar < 4, bar >= 12
        build = bar in (18, 19)
        fill = bar in (3, 11)

        # drums
        for beat in range(4):
            if fill and beat >= 2:
                continue
            if bar < 2 or (bar == 19 and beat == 3):
                continue  # intro builds; one beat of silence before the final hit
            s.hit("drums", kick(), bar, beat * 4)
            kicks.append(s.at(bar, beat * 4))
        if bar >= 1:
            for st in range(2, 16, 4):
                s.hit("drums", hat(), bar, st, 0.45, 0.25)
        if not intro:
            for st in (4, 12):
                s.hit("drums", clap(), bar, st, 0.7, -0.05)
            for st in range(16):
                if st % 4 != 2:
                    s.hit("drums", hat(), bar, st, 0.12 + 0.08 * (st % 2 == 0), 0.3)
            for st in range(2, 16, 4):
                s.hit("drums", hat(True), bar, st, 0.22, -0.3)
        if bar in (4, 12):
            s.hit("drums", crash(), bar, 0, 0.5, 0.4)
        if fill:  # breakbeat fill over the last two beats
            for st, smp, g in [(8, kick(), 1.0), (10, snare(), 0.8), (11, kick(), 0.8),
                               (13, snare(True), 0.6), (14, snare(), 0.8), (15, snare(True), 0.9)]:
                s.hit("drums", smp, bar, st, g, 0.1 if smp is not kick() else 0)
                if smp is kick():
                    kicks.append(s.at(bar, st))
            s.hit("drums", tom(140), bar, 12, 0.6, -0.4)
            s.hit("drums", tom(100), bar, 14, 0.6, 0.4)
        if build:
            for st in range(16):
                vel = 0.15 + 0.6 * ((bar - 18) * 16 + st) / 32
                if not (bar == 19 and st >= 12):
                    s.hit("drums", snare(True), bar, st, vel, 0.05)
            if bar == 18:
                s.hit("fx", riser(s.bar * 2 - s.step * 4), bar, 0, 0.35)

        # bass: rolling 16ths, rest on the kick
        if 2 <= bar < 19:
            for st in range(16):
                if st % 4 == 0:
                    continue
                nt = root if st % 4 != 3 else transpose(root, 12)
                s.hit("bass", v_bass(note_freq(nt), round(s.step * 0.8, 4)), bar, st, 0.45)
        elif bar == 19:
            for st in range(12):
                if st % 4:
                    s.hit("bass", v_bass(note_freq(root), round(s.step * 0.8, 4)), bar, st, 0.55)

        # arp: filter opens during the intro
        cutoff = 500 + 3300 * min(1, (bar * 16) / 64) if intro else (4200 if sec_b else 3600)
        for st, idx in enumerate(ARP_STEPS):
            if bar == 19 and st >= 12:
                break
            s.hit("arp", v_arp(note_freq(ext[idx]), round(s.step * 0.7, 4), round(cutoff, -1)),
                  bar, st, 0.3, 0.35 if st % 2 else -0.35)

        # pad
        s.chord("pad", v_pad, pad, bar, dur_steps=16 if bar != 19 else 12,
                gain=0.35 if intro else 0.45, bright=1400.0 if intro else 2200.0)

    # lead melody over A and B
    s.play("lead", v_lead, A_LEAD, start_bar=4, gain=0.5)

    # final hit, bar 20
    s.hit("drums", kick(), 20)
    kicks.append(s.at(20))
    s.hit("drums", crash(), 20, 0, 0.6)
    s.chord("pad", v_pad, ["A3", "C4", "E4", "A4"], 20, dur_steps=12, gain=0.55, bright=2600.0)
    s.hit("bass", v_bass(note_freq("A1"), round(s.step * 10, 4)), 20, 0, 0.6)
    s.hit("lead", v_lead(note_freq("A5"), round(s.step * 10, 4)), 20, 0, 0.45)

    # mix
    tl = s.tl
    duck = sidechain(tl.n, kicks)
    drums = hp(tl.group("drums"), 35)
    bass = hp(tl.group("bass"), 40) * duck
    arp = delay(tl.group("arp"), s.step * 3, feedback=0.4, mix=0.35, damp=3000, tail=0)[:, :tl.n]
    pad = reverb(hp(tl.group("pad"), 160) * duck, seconds=2.5, mix=0.35, seed=21)[:, :tl.n]
    pad = widen(pad, 1.8)
    lead = delay(tl.group("lead"), s.step * 3, feedback=0.35, mix=0.3, damp=4500, tail=0)[:, :tl.n]
    lead = widen(reverb(hp(lead, 180), seconds=2.0, mix=0.22, seed=22)[:, :tl.n], 1.4)
    fx = tl.group("fx")
    mix = drums * 0.9 + bass * 0.8 + arp * 0.6 + pad * 0.55 + lead * 0.75 + fx
    mix = reverb(mix, seconds=1.0, mix=0.06, seed=23)[:, :tl.n]
    return mix


# ================================================================ B: synth-orchestral

B_CHORDS = {  # string pad voicing, ostinato root (octave 2), stab voicing
    "Dm": (["D3", "F3", "A3", "D4"], "D", ["D4", "F4", "A4"]),
    "Bb": (["D3", "F3", "Bb3", "D4"], "A#", ["D4", "F4", "A#4"]),
    "C": (["E3", "G3", "C4", "E4"], "C", ["E4", "G4", "C5"]),
    "Gm": (["D3", "G3", "A#3", "D4"], "G", ["D4", "G4", "A#4"]),
    "A": (["E3", "A3", "C#4", "E4"], "A", ["E4", "A4", "C#5"]),
    "F": (["C3", "F3", "A3", "C4"], "F", ["C4", "F4", "A4"]),
}
B_PROG = (["Dm", "Dm", "Bb", "A"]                                 # intro   bars 0-3
          + ["Dm", "Bb", "C", "Dm", "Dm", "Bb", "Gm", "A"]        # A       bars 4-11
          + ["Bb", "C", "F", "Dm", "Gm", "A", "Bb", "A"])         # B       bars 12-19
B_THEME = [
    # A section: horn motif (bars 4-11)
    "D4 - - - - - A4 - A4 - - - D5 - - -",
    "D5 - - - C5 - A#4 - C5 - - - - - - -",
    "C5 - - - - - G4 - C5 - - - E5 - - -",
    "F5 - - - E5 - D5 - - - - - - - . .",
    "D4 - - - - - A4 - A4 - - - D5 - - -",
    "F5 - - - - - D5 - A#4 - - - D5 - F5 -",
    "G5 - - - F5 - D5 - A#4 - - - G4 - - -",
    "A4 - - - - - - - C#5 - - - E5 - - -",
    # B section: theme lifts an octave into strings + brass (bars 12-19)
    "F5 - - - - - D5 - F5 - - - A#5 - - -",
    "A5 - - - G5 - - - E5 - - - C5 - - -",
    "F5 - - - - - - - A5 - - - C6 - - -",
    "A5 - - - - - - - . . . . D5 - F5 -",
    "G5 - - - - - A#5 - A5 - G5 - F5 - - -",
    "E5 - - - - - C#5 - E5 - - - A5 - - -",
    "A#5 - - - A5 - G5 - C6 - - - A#5 - - -",
    "E5 - - - - - - - A5 - - - C#6 - - -",
]
B_BARS = ["D2", "D3", "D3", "D2", "D3", "D3", "D2", "D3",
          "D2", "D3", "D3", "D2", "D3", "D3", "A2", "D3"]  # ostinato shape (on D)


def song_b():
    s = Song(132, 21, tail=2.5)
    for bar, name in enumerate(B_PROG):
        pad, root, stab = B_CHORDS[name]
        shift = (note_number(root + "2") - note_number("D2")) % 12
        shift = shift - 12 if shift > 6 else shift
        intro, sec_b = bar < 4, bar >= 12

        # string ostinato (galloping 16ths), from bar 0
        for st, nm in enumerate(B_BARS):
            vel = (0.5 if st % 3 == 0 else 0.32) * (0.65 if nm.endswith("2") else 1.0)
            s.hit("ost", v_strings_short(note_freq(transpose(nm, shift)), round(s.step * 0.75, 4)),
                  bar, st, vel * (0.8 if intro else 1.0), -0.25)
            if sec_b:  # upper octave doubling in B
                s.hit("ost", v_strings_short(note_freq(transpose(nm, shift + 12)),
                                             round(s.step * 0.75, 4)), bar, st, vel * 0.5, 0.3)

        # sustained strings
        s.chord("strings", v_strings_long, pad, bar, dur_steps=16,
                gain=(0.25 + 0.08 * bar) if intro else 0.55, spread=0.7)

        # percussion
        if intro:
            s.hit("perc", timpani("D2" if name == "Dm" else "A1"), bar, 0, 0.9)
            if bar == 3:  # timpani roll into the A section
                for st in range(8, 16):
                    s.hit("perc", timpani("A1", 0.4), bar, st, 0.25 + 0.08 * (st - 8))
                s.hit("fx", riser(s.bar / 2), bar, 8, 0.25)
        else:
            for st in (0, 3, 6, 8, 11, 14):
                s.hit("perc", taiko(), bar, st, 0.9 if st in (0, 8) else 0.6)
            for st in (4, 12):
                s.hit("perc", snare(), bar, st, 0.55, 0.1)
            if sec_b:
                for st in range(0, 16, 2):
                    s.hit("perc", hat(), bar, st, 0.12, 0.35)
            if bar in (4, 12):
                s.hit("perc", crash(), bar, 0, 0.55, 0.3)
                s.hit("perc", timpani("D2" if name == "Dm" else "A#1"), bar, 0, 0.8)
            if bar == 11 or bar == 19:  # snare roll into next section / final hit
                for st in range(8, 16):
                    s.hit("perc", snare(True), bar, st, 0.25 + 0.07 * (st - 8), 0.1)

        # brass stabs on the off-beats in B
        if sec_b:
            for st in (6, 14):
                s.chord("brass", v_brass, stab, bar, st, dur_steps=1, gain=0.55, spread=0.4,
                        stab=True)

    # intro horn call (bars 2-3) announcing the motif
    s.play("brass", v_brass, ["D4 - - - - - - - A4 - - - - - - -",
                              "A4 - - - - - - - C#5 - - - E5 - - -"], start_bar=2, gain=0.45)

    # theme: brass in A (with a lower octave), strings + brass in B
    s.play("brass", v_brass, B_THEME[:8], start_bar=4, gain=0.6, position=-0.1)
    s.play("brass", v_brass, B_THEME[:8], start_bar=4, gain=0.3, position=0.15, octave=-1)
    s.play("lead", v_strings_long, B_THEME[8:], start_bar=12, gain=0.55, position=0.1,
           attack=0.05)
    s.play("brass", v_brass, B_THEME[8:], start_bar=12, gain=0.5, position=-0.15, octave=-1)

    # final tutti D minor hit, bar 20
    s.chord("strings", v_strings_long, ["D3", "A3", "D4", "F4", "A4", "D5"], 20, dur_steps=14,
            gain=0.8, attack=0.02)
    s.chord("brass", v_brass, ["D3", "A3", "D4", "F4"], 20, dur_steps=10, gain=0.8)
    s.hit("lead", v_strings_long(note_freq("D6"), round(s.step * 14, 4), attack=0.02), 20, 0, 0.4)
    s.hit("perc", taiko(), 20, 0, 1.0)
    s.hit("perc", timpani("D2", 2.5), 20, 0, 1.0)
    s.hit("perc", crash(), 20, 0, 0.7)
    s.hit("ost", v_strings_short(note_freq("D2"), round(s.step * 12, 4)), 20, 0, 0.6)

    tl = s.tl
    ost = reverb(hp(tl.group("ost"), 45), seconds=1.8, mix=0.22, seed=31)[:, :tl.n]
    strings = hp(tl.group("strings"), 110) + tl.group("lead")
    strings = widen(reverb(strings, seconds=3.2, mix=0.4, seed=32)[:, :tl.n], 1.5)
    brass = reverb(hp(tl.group("brass"), 90), seconds=2.6, mix=0.3, seed=33)[:, :tl.n]
    perc = reverb(hp(tl.group("perc"), 38), seconds=2.8, mix=0.25, damping=0.8, seed=34)[:, :tl.n]
    fx = tl.group("fx")
    return ost * 0.7 + strings * 0.6 + brass * 0.8 + perc * 0.8 + fx


SONGS = {"a": ("music-r01-a", song_a), "b": ("music-r01-b", song_b)}


def main(argv):
    out = OUT
    if "--out" in argv:
        i = argv.index("--out")
        out = Path(argv[i + 1])
        argv = argv[:i] + argv[i + 2:]
    for key in argv or SONGS:
        name, fn = SONGS[key]
        mix = fn()
        mix = mix - mix.mean(axis=1, keepdims=True)
        mix = fade(master(mix, -14.0, -2.0), 0.005, 0.5)
        path = write_ogg(out / f"{name}.ogg", mix)
        print(f"{path}  {mix.shape[1] / SR:.1f}s")


if __name__ == "__main__":
    main(sys.argv[1:])
