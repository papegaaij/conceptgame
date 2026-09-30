#!/usr/bin/env python3
"""Concept round 03 music: five more loopable themes (same format as music_r02.py).

Outputs (design/audio/music/concept/):
  earth-theme-r03-a.ogg        "Homefront"          Act 2 A, 147 BPM, C minor
  belt-theme-r03-a.ogg         "Hollow Rock"        Act 5 A, 135 BPM, B minor
  jovian-theme-r03-a.ogg       "Eye of the Storm"   Act 6 A, 140 BPM, G minor
  ascendancy-boss-r03-a.ogg    "Iron Sovereign"     Ascendancy bosses, 147 BPM, F# minor
  final-boss-r03-a.ogg         "Choir Heart"        final boss, 150 BPM, E minor -> E major

Usage: python3 tools/concept/audio/music_r03.py [earth|belt|jovian|ascendancy|final ...] [--out DIR]

Loop format as in round 02: intro + loop + 2-bar fade-out tail, loop points in the
LOOPSTART / LOOPLENGTH Vorbis comments (samples), sample-exact loop.

Motifs (see design/audio/music/README.md):
  humanity's motif   1-5-8-9-b10 (major keys: 10)
  the Choir motif    8-b7-b6-5, tritone drop to b2, 1
  Vorne's motif      1-b3-5-7 (minor-major 7th arpeggio), dotted quarter, eighth, half, whole
"""
import sys
from functools import lru_cache
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
from music import (clap, crash, hat, hp, kick, ns, riser, sidechain, snare, taiko,  # noqa: E402
                   timpani, tom, transpose, v_arp, v_bass, v_brass, v_lead, v_pad,
                   v_strings_long, v_strings_short, widen)
from music_r02 import (LoopSong, add_tail, seam_ratio, v_choir, v_dbass,  # noqa: E402
                       v_flute, v_pluck, v_riff, wind)
from synth import (SR, adsr, db, decode, delay, exp_decay, fade, master, noise,  # noqa: E402
                   note_freq, pulse, ramp, reverb, saturate, saw, sine, supersaw,
                   svf, triangle, write_ogg)

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "design" / "audio" / "music" / "concept"


def figure(root, intervals, octave_up=0):
    return [transpose(root, 12 * octave_up + i) for i in intervals]


# ================================================================ extra voices & samples

@lru_cache(None)
def v_trumpet(freq, dur, stab=False):
    """Brighter, narrower brass than v_brass (the 'trumpet' section)."""
    n = ns(dur + (0.1 if stab else 0.22))
    t = np.arange(n) / SR
    vib = 1 + 0.005 * np.sin(2 * np.pi * 5.5 * t) * np.clip((t - 0.2) / 0.3, 0, 1)
    f = freq * vib * ramp(n, 0.96, 1.0, tau=0.018)
    x = supersaw(f, n, voices=3, detune=0.003, rng=np.random.default_rng(111))
    x += pulse(f, n, 0.3) * 0.5
    cut = 700 + 3800 * (1 - exp_decay(n, 0.015)) * (0.6 + 0.4 * exp_decay(n, 0.12))
    x = svf(x, cut, q=1.2)
    env = adsr(n, 0.003 if stab else 0.02, 0.1, 0.5 if stab else 0.8, 0.08 if stab else 0.18,
               gate=dur)
    return saturate(x * env * 1.4, 1.4)


@lru_cache(None)
def v_tremolo(freq, dur):
    """Long string note with 32nd-note amplitude tremolo (storm strings)."""
    n = ns(dur + 0.4)
    t = np.arange(n) / SR
    x = supersaw(freq, n, voices=5, detune=0.008, rng=np.random.default_rng(112))
    x = svf(x, 3400, q=0.7)
    trem = 0.55 + 0.45 * np.abs(np.sin(2 * np.pi * 6.0 * t))
    return x * trem * adsr(n, 0.08, 0.2, 0.85, 0.35, gate=dur)


@lru_cache(None)
def v_organ(freq, dur):
    """Cold additive organ (drawbars 16' 8' 5 1/3' 4' 2')."""
    n = ns(dur + 0.3)
    x = sum(a * sine(freq * r * (1 + 0.0015 * k), n)
            for k, (r, a) in enumerate([(0.5, 0.6), (1, 1.0), (1.5, 0.4), (2, 0.5), (4, 0.25)]))
    return x * adsr(n, 0.04, 0.1, 0.9, 0.25, gate=dur) * 0.4


@lru_cache(None)
def anvil(pitch=800.0, seed=1):
    """Metallic hit: inharmonic partials plus a click (industrial percussion)."""
    n = ns(0.5)
    rng = np.random.default_rng(seed)
    x = sum(a * sine(pitch * r, n) * exp_decay(n, tau)
            for r, a, tau in [(1, 1.0, 0.09), (2.76, 0.6, 0.05), (5.4, 0.4, 0.03), (8.93, 0.25, 0.02)])
    x += svf(noise(n, rng), 4500, q=0.9, mode="hp") * exp_decay(n, 0.006) * 1.2
    return fade(x, 0.0005, 0.02)


@lru_cache(None)
def steam():
    n = ns(0.45)
    x = svf(noise(n, np.random.default_rng(121)), 2500, q=0.7, mode="hp")
    return fade(x * adsr(n, 0.01, 0.1, 0.4, 0.25, gate=0.15), 0.001, 0.05)


@lru_cache(None)
def beep():
    n = ns(0.14)
    x = pulse(1000.0, n, 0.5) * 0.5
    return fade(svf(x, 3000) * adsr(n, 0.002, 0.02, 0.9, 0.02, gate=0.12), 0.001, 0.01)


def radio_static(n, seed=131):
    """Broadcast static: band-passed noise with crackle and slow fading."""
    rng = np.random.default_rng(seed)
    t = np.arange(n) / SR
    x = svf(noise(n, rng), 1900, q=0.8, mode="bp") * (0.5 + 0.5 * np.sin(2 * np.pi * 0.7 * t) ** 2)
    crackle = (rng.random(n) > 0.9993) * rng.uniform(-1, 1, n) * 3
    return np.vstack([x + crackle, x * 0.9 + crackle])


@lru_cache(None)
def thunder(seed):
    rng = np.random.default_rng(seed)
    n = ns(3.5)
    t = np.arange(n) / SR
    brown = np.cumsum(noise(n, rng))
    brown -= np.convolve(brown, np.ones(4096) / 4096, mode="same")
    brown /= np.max(np.abs(brown)) + 1e-9
    env = np.exp(-t / 1.1) * (0.6 + 0.4 * np.abs(np.sin(2 * np.pi * rng.uniform(1.5, 3) * t)))
    rumble = svf(brown, 220, q=0.8) * env * 2.0
    crack = svf(noise(n, rng), 3000, q=0.7, mode="hp") * exp_decay(n, 0.04) * 0.8
    return fade(saturate(rumble + crack, 1.3), 0.002, 0.3)


@lru_cache(None)
def heartbeat():
    """Lub-dub: two low thumps (the Choir Heart)."""
    n = ns(0.6)
    x = np.zeros(n)
    for at, f0, g in ((0.0, 75, 1.0), (0.2, 66, 0.7)):
        m = n - ns(at)
        x[ns(at):] += sine(ramp(m, f0, 50, tau=0.04), m) * exp_decay(m, 0.09) * g
    return fade(saturate(svf(x, 300) * 1.6, 1.5), 0.0005, 0.05)


def siren(n, start, length):
    """Air-raid siren: slow rise and fall between two pitches, placed at [start, start+length)."""
    out = np.zeros(n)
    m = min(length, n - start)
    t = np.arange(m) / SR
    f = 330 + 260 * (0.5 - 0.5 * np.cos(2 * np.pi * t / 3.2))
    x = (triangle(f, m) * 0.7 + saw(f, m) * 0.15) * np.sin(np.pi * np.arange(m) / m) ** 0.5
    out[start:start + m] = svf(x, 1600, q=0.8)
    return out


def mixdown(s, spec, kicks, depth=0.4, release=0.12):
    """Apply per-group processing and sum. spec: {group: dict(hp, rev, dly, wid, gain, duck)}."""
    tl = s.tl
    duck = sidechain(tl.n, kicks, depth=depth, release=release) if kicks else np.ones(tl.n)
    total = np.zeros((2, tl.n))
    for name, o in spec.items():
        x = tl.group(name)
        if not x.any():
            continue
        if o.get("hp"):
            x = hp(x, o["hp"])
        if o.get("duck"):
            x = x * duck
        if o.get("dly"):
            st, fb, mx, dmp = o["dly"]
            x = delay(x, s.step * st, feedback=fb, mix=mx, damp=dmp, tail=0)[:, :tl.n]
        if o.get("rev"):
            sec, mx = o["rev"]
            x = reverb(x, seconds=sec, mix=mx, damping=o.get("damp", 0.5),
                       seed=o.get("seed", 7))[:, :tl.n]
        if o.get("wid"):
            x = widen(x, o["wid"])
        total += x * o.get("gain", 1.0)
    return total


DRUMS = dict(hp=35, gain=0.85)
PERC = dict(hp=38, rev=(2.6, 0.25), damp=0.8, gain=0.75, seed=201)
FX = dict(gain=1.0)


# ================================================================ motifs

def human_motif(root, major=False):
    """Two bars of humanity's motif (1-5-8-9-b10/10) from root (octave 4 name)."""
    n = [transpose(root, i) for i in (0, 7, 12, 14, 16 if major else 15)]
    return [f"{n[0]} - - - - - {n[1]} - {n[2]} - - - - - {n[3]} -",
            f"{n[4]} - - - - - - - - - - - . . . ."]


def vorne_motif(root):
    """Two bars of Vorne's motif (1-b3-5-7), stately rhythm."""
    n = [transpose(root, i) for i in (0, 3, 7, 11)]
    return [f"{n[0]} - - - - - {n[1]} - {n[2]} - - - - - - -",
            f"{n[3]} - - - - - - - - - - - - - - -"]


def choir_motif(root):
    """Two bars of the Choir motif (8-b7-b6-5-b2-1) from root (the upper 8)."""
    n = [transpose(root, i) for i in (0, -2, -4, -5, -11, -12)]
    return [f"{n[0]} - - - {n[1]} - - - {n[2]} - - - {n[3]} - - -",
            f"{n[4]} - - - - - - - {n[5]} - - - - - - -"]


# ================================================================ 1 earth: Homefront

EA_CH = {  # pad voicing, bass root, third above the root (3 minor / 4 major)
    "Cm": (["C3", "Eb3", "G3", "C4"], "C2", 3),
    "Ab": (["C3", "Eb3", "Ab3", "C4"], "Ab1", 4),
    "Fm": (["C3", "F3", "Ab3", "C4"], "F2", 3),
    "G": (["D3", "G3", "B3", "D4"], "G1", 4),
    "Bb": (["D3", "F3", "Bb3", "D4"], "Bb1", 4),
}
EA_INTRO = ["Cm", "Cm", "Ab", "G"]
EA_LOOP = (["Cm", "Cm", "Ab", "Ab", "Fm", "Fm", "G", "G"]        # A: motif riff
           + ["Cm", "Ab", "Fm", "G", "Cm", "Ab", "Bb", "G"]      # B: horns
           + ["Ab", "Fm", "Cm", "G"]                             # C: embattled half-time
           + ["Cm", "Ab", "Fm", "G", "Cm", "Ab", "Bb", "G"])     # D: all in
EA_HORNS = human_motif("C4")[:1] + [
    "Eb5 - - - - - - - - - - - D5 - C5 -",
    "C5 - - - - - Ab4 - F4 - - - Ab4 - C5 -",
    "D5 - - - - - - - B4 - - - G4 - - -",
] + human_motif("C4")[:1] + [
    "F5 - - - - - Eb5 - C5 - - - Eb5 - F5 -",
    "G5 - - - F5 - - - D5 - - - F5 - - -",
    "G5 - - - - - - - B4 - - - D5 - - -",
]
EA_LAMENT = ["Eb5 - - - - - - - C5 - - - Ab4 - - -", "C5 - - - - - - - F4 - - - Ab4 - - -",
             "G4 - - - - - - - C5 - - - D5 - Eb5 -", "D5 - - - - - - - - - - - B4 - - -"]


def earth_riff(root, third):
    """Humanity's motif diminished into a relentless 16th figure: 1-5-8-9-3-9-8-5."""
    return figure(root, [0, 7, 12, 14, 12 + third, 14, 12, 7], octave_up=2)


def earth():
    s = LoopSong(147, 4, 28)
    kicks = []
    s.tl.add("fx", siren(s.tl.n, 0, int(4 * s.bar * SR)), 0, 0, 0.35)
    for bar, name in enumerate(EA_INTRO):
        pad, root, third = EA_CH[name]
        s.chord("choir", v_choir, pad[1:], bar, dur_steps=16, gain=0.3 + 0.08 * bar)
        s.hit("perc", timpani("C2" if name == "Cm" else "G1"), bar, 0, 0.8)
        if bar >= 2:
            for st, nm in enumerate(earth_riff(root, third) * 2):
                s.hit("riff", v_pluck(note_freq(nm), round(s.step * 0.6, 4), 1200.0 + 500 * (bar - 2)),
                      bar, st, 0.3, 0.3 if st % 2 else -0.3)
    s.play("brass", v_brass, human_motif("C4"), 0, gain=0.4)
    for st in range(16):
        s.hit("drums", snare(True), 3, st, 0.15 + 0.035 * st, 0.05)
    s.hit("fx", riser(s.bar), 3, 0, 0.25)

    for base in s.sections():
        for i, name in enumerate(EA_LOOP):
            bar = base + i
            pad, root, third = EA_CH[name]
            sec = "A" if i < 8 else "B" if i < 16 else "C" if i < 20 else "D"
            if i in (0, 8, 20):
                s.hit("perc", crash(), bar, 0, 0.55, 0.3)
            if sec == "C":
                s.hit("drums", kick(), bar, 0, 0.95)
                kicks.append(s.pos(bar, 0))
                s.hit("drums", snare(), bar, 8, 0.7, 0.05)
                for st in (0, 6):
                    s.hit("perc", taiko(), bar, st, 0.7)
            else:
                for st in (0, 3, 8, 10):
                    s.hit("drums", kick(), bar, st, 0.9 if st in (0, 8) else 0.65)
                    kicks.append(s.pos(bar, st))
                for st in (4, 12):
                    s.hit("drums", snare(), bar, st, 0.6, 0.05)
                for st in (7, 14):
                    s.hit("drums", snare(True), bar, st, 0.22, 0.05)
                for st in range(16):
                    s.hit("drums", hat(), bar, st, 0.2 if st % 2 == 0 else 0.12, 0.3)
                if sec == "A":
                    s.hit("perc", taiko(), bar, 0, 0.7)
            if sec == "A" and i % 4 == 0:  # orchestral hit
                s.chord("brass", v_brass, pad, bar, 0, dur_steps=3, gain=0.6, stab=True)
                s.hit("perc", timpani("C2" if root == "C2" else "F2"), bar, 0, 0.7)
            if i >= 26:
                for st in range(16):
                    s.hit("drums", snare(True), bar, st, 0.18 + 0.02 * st + 0.08 * (i - 26), 0.05)
                if i == 26:
                    s.hit("fx", riser(s.bar * 2), bar, 0, 0.3)
            # riff (strings), bass, choir
            riff = earth_riff(root, third)
            for st in range(16):
                nm = riff[st % 8] if sec != "B" else transpose(riff[st % 8], -12)
                if sec == "C":
                    s.hit("riff", v_pluck(note_freq(nm), round(s.step * 0.6, 4), 1400.0), bar, st, 0.22,
                          0.3 if st % 2 else -0.3)
                else:
                    g = 1.25 if sec == "A" else 1.0
                    s.hit("riff", v_strings_short(note_freq(nm), round(s.step * 0.7, 4)), bar, st,
                          g * (0.34 if st % 4 == 0 else 0.24), -0.2)
            if sec in ("A", "D"):
                for st in range(16):
                    s.hit("arp", v_arp(note_freq(riff[st % 8]), round(s.step * 0.6, 4), 3000.0),
                          bar, st, 0.14, 0.35 if st % 2 else -0.35)
            if sec == "A":  # low brass pushing 8ths
                for st in (0, 2, 4, 6, 8, 10, 12, 14):
                    s.hit("brass", v_brass(note_freq(transpose(root, 12)), round(s.step * 1.3, 4),
                                           stab=True), bar, st, 0.32 if st % 4 else 0.42)
            if sec != "C":
                for st in range(0, 16, 2):
                    nm = transpose(root, 12 if st % 8 == 6 else 0)
                    s.hit("bass", v_bass(note_freq(nm), round(s.step * 1.6, 4)), bar, st, 0.45)
            else:
                s.hit("bass", v_bass(note_freq(root), round(s.step * 14, 4)), bar, 0, 0.45)
            s.chord("choir", v_choir, pad[1:], bar, dur_steps=16, gain=0.45 if sec == "C" else 0.35)
            s.chord("strings", v_strings_long, pad, bar, dur_steps=16, gain=0.35)
        s.play("brass", v_brass, EA_HORNS, base + 8, gain=0.6, position=-0.1)
        s.play("brass", v_trumpet, EA_HORNS, base + 8, gain=0.25, position=0.15, octave=1)
        s.play("lead", v_lead, EA_LAMENT, base + 16, gain=0.5)
        s.play("brass", v_brass, EA_HORNS, base + 20, gain=0.6, position=-0.1)
        s.play("lead", v_lead, EA_HORNS, base + 20, gain=0.45, position=0.1, octave=1)

    return s, mixdown(s, {
        "drums": DRUMS, "perc": PERC, "fx": dict(rev=(3.0, 0.5), seed=202, gain=0.8),
        "riff": dict(hp=150, rev=(1.6, 0.2), seed=203, gain=0.6),
        "arp": dict(dly=(3, 0.3, 0.25, 3000), gain=0.45),
        "bass": dict(hp=40, duck=True, gain=0.7),
        "choir": dict(hp=150, rev=(3.2, 0.4), wid=1.5, seed=204, gain=0.55),
        "strings": dict(hp=110, duck=True, rev=(3.0, 0.35), wid=1.4, seed=205, gain=0.45),
        "brass": dict(hp=90, rev=(2.4, 0.28), seed=206, gain=0.8),
        "lead": dict(hp=180, dly=(3, 0.3, 0.25, 4500), rev=(2.2, 0.22), wid=1.4, seed=207, gain=0.6),
    }, kicks)


# ================================================================ 2 belt: Hollow Rock

BE_CH = {  # pad voicing, bass root, arp intervals (the machine plays Vorne's arpeggio)
    "Bm": (["B2", "D3", "F#3", "A#3"], "B1", [0, 3, 7, 11, 12, 11, 7, 3]),
    "G": (["B2", "D3", "G3", "B3"], "G1", [0, 4, 7, 11, 12, 11, 7, 4]),
    "Em": (["B2", "E3", "G3", "B3"], "E2", [0, 3, 7, 10, 12, 10, 7, 3]),
    "F#": (["A#2", "C#3", "F#3", "A#3"], "F#1", [0, 4, 7, 10, 12, 10, 7, 4]),
    "Gm": (["A#2", "D3", "G3", "A#3"], "G1", [0, 3, 7, 10, 12, 10, 7, 3]),
}
BE_LOOP = (["Bm", "Bm", "G", "G", "Em", "Em", "F#", "F#"]         # A: the machine
           + ["Bm", "Gm", "Bm", "F#", "Bm", "Gm", "Em", "F#"]     # B: Vorne's motif
           + ["Bm", "Bm", "Bm", "Bm"]                             # C: conveyor breakdown
           + ["G", "G", "F#", "F#", "Bm", "Gm", "Em", "F#"])      # D: build, motif tutti
BE_VORNE = vorne_motif("B3") + [
    "A#4 - - - - - F#4 - D4 - - - - - - -",
    "B3 - - - - - - - - - - - . . . .",
] + vorne_motif("B3") + [
    "G4 - - - - - E4 - B3 - - - - - - -",
    "A#3 - - - - - - - C#4 - - - - - - -",
]
BE_METAL = [(0, 900, 0.5), (3, 1350, 0.3), (6, 900, 0.35), (8, 1800, 0.25), (10, 1350, 0.3),
            (11, 900, 0.25), (14, 1800, 0.3)]


def belt():
    s = LoopSong(135, 4, 28)
    kicks = []
    s.tl.add("radio", radio_static(int(4 * s.bar * SR)) * np.linspace(1, 0.3, int(4 * s.bar * SR)),
             0, 0, 0.18)
    for st in (0, 2):
        s.hit("radio", beep(), 0, st, 0.25)
    s.chord("pad", v_pad, ["B1", "F#2", "B2"], 0, dur_steps=64, gain=0.45, bright=700.0)
    s.play("brass", v_brass, vorne_motif("B3"), 0, gain=0.55)
    s.play("brass", v_organ, vorne_motif("B3"), 0, gain=0.3, octave=-1)
    for bar in (2, 3):
        for st, p, g in BE_METAL:
            s.hit("metal", anvil(float(p)), bar, st, g * (0.5 + 0.25 * (bar - 2)), (p - 1350) / 1500)
        s.hit("perc", steam(), bar, 12, 0.3, 0.5)
    for st in range(8, 16):
        s.hit("drums", snare(True), 3, st, 0.2 + 0.06 * (st - 8), 0.05)

    for base in s.sections():
        for i, name in enumerate(BE_LOOP):
            bar = base + i
            pad, root, arp = BE_CH[name]
            sec = "A" if i < 8 else "B" if i < 16 else "C" if i < 20 else "D"
            conveyor = sec == "C"
            if i in (0, 8, 24):
                s.hit("perc", crash(), bar, 0, 0.45, 0.3)
            if not conveyor:
                for beat in range(4):
                    s.hit("drums", kick(130), bar, beat * 4, 0.95)
                    kicks.append(s.pos(bar, beat * 4))
                for st in (4, 12):
                    s.hit("drums", snare(), bar, st, 0.5, 0.05)
                    s.hit("metal", anvil(2200.0, 2), bar, st, 0.25, -0.2)
                for st in range(2, 16, 4):
                    s.hit("drums", hat(True), bar, st, 0.2, 0.3)
            for st, p, g in BE_METAL:
                s.hit("metal", anvil(float(p)), bar, st, g * (1.2 if conveyor else 0.8), (p - 1350) / 1500)
            if conveyor:
                for st in range(16):
                    s.hit("metal", anvil(3100.0, 3), bar, st, 0.1 if st % 2 else 0.16, 0.5)
            if i % 4 == 3:
                s.hit("perc", steam(), bar, 12, 0.35, 0.5 if i % 8 == 3 else -0.5)
            if i >= 26:
                for st in range(16):
                    s.hit("drums", snare(True), bar, st, 0.2 + 0.02 * st + 0.08 * (i - 26), 0.05)
            if i == 20:
                s.hit("fx", riser(s.bar * 4), bar, 0, 0.3)
            # distorted bass: syncopated 8ths
            if not conveyor:
                for st in (0, 3, 6, 8, 10, 13, 14):
                    nm = transpose(root, 12 if st == 14 else 0)
                    s.hit("bass", v_dbass(note_freq(nm), round(s.step * 1.4, 4)), bar, st, 0.42)
            else:
                s.hit("bass", v_dbass(note_freq(root), round(s.step * 15, 4)), bar, 0, 0.3)
            # the machine plays Vorne's arpeggio
            notes = figure(transpose(root, 24), arp)
            if sec == "A":  # distorted machine line under the arp: Vorne's arpeggio in 8ths
                for st in range(0, 16, 2):
                    s.hit("riff", v_riff(note_freq(transpose(notes[(st // 2) % 4], -12)),
                                         round(s.step * 1.4, 4)), bar, st, 0.3, 0.2)
            for st in range(16):
                s.hit("arp", v_pluck(note_freq(notes[st % 8]), round(s.step * 0.6, 4),
                                     1500.0 if sec in ("A", "D") else 900.0),
                      bar, st, 0.15 if sec == "C" else 0.3 if sec == "A" else 0.25,
                      0.35 if st % 2 else -0.35)
            s.chord("pad", v_pad, pad, bar, dur_steps=16, gain=0.4, bright=900.0)
            if sec in ("B", "D") and i % 2 == 0:
                s.hit("perc", timpani("B1" if name == "Bm" else "F#2" if name == "F#" else "G1"), bar, 0, 0.7)
        s.play("brass", v_brass, BE_VORNE, base + 8, gain=0.6, position=-0.1)
        s.play("brass", v_brass, BE_VORNE, base + 8, gain=0.3, position=0.15, octave=-1)
        s.play("organ", v_organ, BE_VORNE, base + 8, gain=0.25)
        s.play("horn", v_flute, human_motif("B3"), base + 17, gain=0.4, position=0.4)
        s.play("brass", v_brass, BE_VORNE[4:], base + 24, gain=0.65, position=-0.1)
        s.play("brass", v_trumpet, BE_VORNE[4:], base + 24, gain=0.35, position=0.15, octave=1)
        s.play("brass", v_brass, BE_VORNE[4:], base + 24, gain=0.35, octave=-1)

    return s, mixdown(s, {
        "drums": dict(hp=35, gain=0.9), "perc": PERC, "fx": FX,
        "metal": dict(hp=200, rev=(1.2, 0.2), seed=211, gain=0.55),
        "radio": dict(hp=300, gain=0.8),
        "bass": dict(hp=40, duck=True, gain=0.65),
        "arp": dict(dly=(3, 0.35, 0.3, 2500), gain=0.45),
        "riff": dict(hp=120, dly=(3, 0.25, 0.2, 3000), gain=0.5),
        "pad": dict(hp=100, duck=True, rev=(3.0, 0.4), wid=1.6, seed=212, gain=0.5),
        "brass": dict(hp=70, rev=(2.8, 0.3), seed=213, gain=0.8),
        "organ": dict(hp=60, rev=(3.0, 0.35), seed=214, gain=0.5),
        "horn": dict(hp=150, dly=(6, 0.4, 0.3, 2500), rev=(3.5, 0.45), seed=215, gain=0.7),
    }, kicks, depth=0.45)


# ================================================================ 3 jovian: Eye of the Storm

JO_CH = {  # pad voicing, bass root
    "Gm": (["G2", "D3", "G3", "Bb3", "D4"], "G1"),
    "GmF#": (["F#2", "D3", "G3", "Bb3", "D4"], "F#1"),
    "GmM7": (["G2", "D3", "F#3", "Bb3", "D4"], "G1"),
    "Eb": (["G2", "Eb3", "G3", "Bb3", "Eb4"], "Eb2"),
    "Bb": (["F2", "D3", "F3", "Bb3", "D4"], "Bb1"),
    "F": (["F2", "C3", "F3", "A3", "C4"], "F2"),
    "Cm": (["G2", "Eb3", "G3", "C4", "Eb4"], "C2"),
    "CmM7": (["G2", "Eb3", "G3", "B3", "Eb4"], "C2"),
    "D": (["F#2", "D3", "F#3", "A3", "D4"], "D2"),
    "Ab": (["Ab2", "Eb3", "Ab3", "C4", "Eb4"], "Ab1"),
}
JO_INTRO = ["Gm", "Gm", "Eb", "D"]
JO_LOOP = (["Gm", "Eb", "Bb", "F", "Gm", "Eb", "Cm", "D"]              # A: the hunt
           + ["GmM7", "GmM7", "Eb", "Eb", "CmM7", "CmM7", "D", "D"]    # B: Vorne vs Coalition
           + ["Gm", "Ab", "Gm", "Ab", "Eb", "Cm", "D", "D"]            # C: the storm
           + ["Gm", "GmF#", "Bb", "F", "Gm", "GmF#", "Cm", "D"])       # D: both motifs
JO_MEL = human_motif("G4")[:1] + [
    "Bb5 - - - - - - - - - - - A5 - G5 -",
    "F5 - - - - - D5 - F5 - - - Bb5 - - -",
    "A5 - - - - - - - C6 - - - A5 - F5 -",
] + human_motif("G4")[:1] + [
    "Bb5 - - - - - G5 - Eb5 - - - G5 - Bb5 -",
    "C6 - - - Bb5 - - - G5 - - - Eb5 - - -",
    "D5 - - - - - - - F#5 - - - A5 - - -",
]
JO_B_LOW = vorne_motif("G3") + [". " * 15 + ".", ". " * 15 + "."] + vorne_motif("C4") \
    + [". " * 15 + ".", ". " * 15 + "."]
JO_B_HIGH = [". " * 15 + ".", ". " * 15 + "."] + human_motif("Eb4", major=True) \
    + [". " * 15 + ".", ". " * 15 + "."] + human_motif("D4", major=True)
JO_STORM = ["G4 - - - - - - - - - - - - - - -", "G#4 - - - - - - - - - - - - - - -",
            "G4 - - - - - - - - - - - - - - -", "G#4 - - - - - - - - - - - G4 - - -"]
GALLOP = [0, 12, 12, 0, 12, 12, 0, 12, 0, 12, 12, 0, 12, 12, 7, 12]


def jovian():
    s = LoopSong(140, 4, 32)
    kicks = []
    for bar, name in enumerate(JO_INTRO):
        pad, root = JO_CH[name]
        s.chord("trem", v_tremolo, pad, bar, dur_steps=16, gain=0.35 + 0.08 * bar, spread=0.7)
    s.hit("fx", thunder(1), 0, 0, 0.6, -0.3)
    s.hit("fx", thunder(2), 2, 4, 0.5, 0.4)
    s.play("brass", v_brass, vorne_motif("G3"), 0, gain=0.45)
    s.play("brass", v_trumpet, human_motif("Eb4", major=True), 2, gain=0.3, position=0.3)
    for st in range(8, 16):
        s.hit("perc", timpani("D2", 0.4), 3, st, 0.25 + 0.08 * (st - 8))
    s.hit("fx", riser(s.bar), 3, 0, 0.25)

    for base in s.sections():
        for i, name in enumerate(JO_LOOP):
            bar = base + i
            pad, root = JO_CH[name]
            sec = "A" if i < 8 else "B" if i < 16 else "C" if i < 24 else "D"
            calm = sec == "C" and i < 20
            if i in (0, 8, 24):
                s.hit("perc", crash(), bar, 0, 0.55, 0.3)
                s.hit("perc", timpani("G1" if root in ("G1", "F#1") else "D2"), bar, 0, 0.8)
            if not calm:
                for beat in range(4):
                    s.hit("drums", kick(), bar, beat * 4, 0.9)
                    kicks.append(s.pos(bar, beat * 4))
                for st in (4, 12):
                    s.hit("drums", clap(), bar, st, 0.5)
                    s.hit("drums", snare(), bar, st, 0.35, 0.05)
                for st in range(16):
                    s.hit("drums", hat(), bar, st, 0.18 if st % 2 == 0 else 0.1, 0.3)
                for st in (0, 6, 10):
                    s.hit("perc", taiko(), bar, st, 0.6)
                if i % 4 == 3:
                    for st, p in ((12, 160), (13, 140), (14, 110), (15, 90)):
                        s.hit("drums", tom(p), bar, st, 0.5, (st - 13.5) / 3)
            else:
                s.hit("perc", taiko(), bar, 0, 0.7)
                if i % 2 == 0:
                    s.hit("fx", thunder(3 + i), bar, 0, 0.6, (-1) ** i * 0.4)
            if i in (22, 23):
                for st in range(16):
                    s.hit("drums", snare(True), bar, st, 0.2 + 0.02 * st + 0.08 * (i - 22), 0.05)
            if i == 20:
                s.hit("fx", riser(s.bar * 4), bar, 0, 0.3)
            if sec != "C" or not calm:
                for st, sm in enumerate(GALLOP):
                    s.hit("ost", v_strings_short(note_freq(transpose(root, 12 + sm)),
                                                 round(s.step * 0.75, 4)),
                          bar, st, 0.4 if st % 3 == 0 else 0.26, -0.25)
                for st in range(16):
                    if st % 4:
                        nm = transpose(root, 12 if st % 4 == 3 else 0)
                        s.hit("bass", v_bass(note_freq(nm), round(s.step * 0.8, 4)), bar, st, 0.38)
            else:
                s.hit("bass", v_bass(note_freq(root), round(s.step * 15, 4)), bar, 0, 0.4)
            if calm:
                s.chord("trem", v_tremolo, pad, bar, dur_steps=16, gain=0.55, spread=0.7)
            s.chord("pad", v_pad, pad, bar, dur_steps=16, gain=0.4, bright=2200.0)
            s.chord("choir", v_choir, pad[2:], bar, dur_steps=16, gain=0.3 if not calm else 0.45)
        s.play("brass", v_brass, JO_MEL, base, gain=0.6, position=-0.1)
        s.play("brass", v_trumpet, JO_MEL, base, gain=0.25, position=0.15)
        s.play("brass", v_brass, JO_B_LOW, base + 8, gain=0.7, octave=-1)
        s.play("brass", v_brass, JO_B_LOW, base + 8, gain=0.45)
        s.play("choir", v_choir, JO_B_LOW, base + 8, gain=0.4)
        s.play("lead", v_trumpet, JO_B_HIGH, base + 8, gain=0.55, position=0.2)
        s.play("choir", v_choir, JO_STORM, base + 16, gain=0.55)
        s.play("brass", v_trumpet, JO_MEL, base + 24, gain=0.55, position=0.15)
        s.play("lead", v_lead, JO_MEL, base + 24, gain=0.4, octave=1)
        s.play("brass", v_brass, vorne_motif("G3"), base + 24, gain=0.6, octave=-1)
        s.play("brass", v_brass, vorne_motif("G3"), base + 28, gain=0.6, octave=-1)

    tl = s.tl
    air = wind(tl.n, seed=221) * s.curve([(-4, 1.2), (0, 0.4), (16, 0.4), (16.01, 1.3),
                                          (20, 1.3), (22, 0.4), (32, 0.4)])
    mix = mixdown(s, {
        "drums": DRUMS, "perc": PERC, "fx": dict(hp=40, rev=(3.5, 0.35), seed=222, gain=0.8),
        "ost": dict(hp=60, rev=(1.8, 0.2), seed=223, gain=0.55),
        "trem": dict(hp=90, rev=(3.5, 0.4), wid=1.5, seed=224, gain=0.5),
        "bass": dict(hp=40, duck=True, gain=0.6),
        "pad": dict(hp=120, duck=True, rev=(3.0, 0.35), wid=1.6, seed=225, gain=0.45),
        "choir": dict(hp=140, rev=(3.8, 0.45), wid=1.6, seed=226, gain=0.55),
        "brass": dict(hp=60, rev=(2.8, 0.3), seed=227, gain=0.8),
        "lead": dict(hp=180, dly=(3, 0.3, 0.22, 4500), rev=(2.4, 0.25), wid=1.4, seed=228, gain=0.6),
    }, kicks)
    return s, mix + air * 0.3


# ================================================================ 4 ascendancy boss: Iron Sovereign

AS_CH = {  # pad voicing, bass root, riff intervals
    "F#mM7": (["F#2", "A2", "C#3", "F3"], "F#1", [0, 3, 7, 11, 12, 11, 7, 3]),
    "F#m": (["F#2", "A2", "C#3", "F#3"], "F#1", [0, 3, 7, 11, 12, 11, 7, 3]),
    "D": (["F#2", "A2", "D3", "F#3"], "D2", [0, 4, 7, 11, 12, 11, 7, 4]),
    "BmM7": (["F#2", "B2", "D3", "A#3"], "B1", [0, 3, 7, 11, 12, 11, 7, 3]),
    "Bm": (["F#2", "B2", "D3", "F#3"], "B1", [0, 3, 7, 11, 12, 11, 7, 3]),
    "C#": (["G#2", "C#3", "F3", "G#3"], "C#2", [0, 4, 7, 10, 12, 10, 7, 4]),
    "G": (["G2", "B2", "D3", "G3"], "G1", [0, 4, 7, 11, 12, 11, 7, 4]),
}
AS_LOOP = (["F#mM7", "F#m", "D", "D", "BmM7", "Bm", "C#", "C#"]    # A: march
           + ["F#m", "D", "Bm", "C#", "F#m", "D", "Bm", "C#"]      # B: machine
           + ["F#m", "G", "F#m", "G", "D", "Bm", "C#", "C#"]       # C: menace, build
           + ["F#mM7", "F#m", "D", "D", "BmM7", "Bm", "C#", "C#"])  # D: tutti
AS_TRUMPETS = vorne_motif("F#4") + [". " * 15 + ".", ". " * 15 + "."] + vorne_motif("B3") \
    + [". " * 15 + ".", ". " * 15 + "."]
AS_AUG = ["F#2 - - - - - - - - - - - - - - -", "A2 - - - - - - - - - - - - - - -",
          "C#3 - - - - - - - - - - - - - - -", "F3 - - - - - - - - - - - - - - -"]
# snare rudiment grid: accent 2 and 4, drags and rolls in between
AS_SNARE = [0.35, 0.12, 0.12, 0.22, 0.8, 0.12, 0.25, 0.12, 0.35, 0.12, 0.12, 0.22, 0.8, 0.25, 0.4, 0.6]


def ascendancy():
    s = LoopSong(147, 4, 32)
    kicks = []
    for bar in range(4):
        s.hit("perc", timpani("F#2" if bar < 2 else "C#2"), bar, 0, 0.9)
        s.chord("pad", v_pad, AS_CH["F#mM7" if bar < 2 else "C#"][0], bar, dur_steps=16, gain=0.4,
                bright=800.0)
        for st, v in enumerate(AS_SNARE):
            s.hit("drums", snare(True), bar, st, v * (0.4 + 0.15 * bar), 0.05)
    s.play("brass", v_brass, vorne_motif("F#3"), 0, gain=0.6)
    s.play("brass", v_brass, vorne_motif("C#3"), 2, gain=0.6)
    s.play("organ", v_organ, vorne_motif("F#3") + vorne_motif("C#3"), 0, gain=0.25, octave=-1)
    s.hit("fx", riser(s.bar), 3, 0, 0.25)

    for base in s.sections():
        for i, name in enumerate(AS_LOOP):
            bar = base + i
            pad, root, riff_iv = AS_CH[name]
            sec = "A" if i < 8 else "B" if i < 16 else "C" if i < 24 else "D"
            half = sec == "C" and i < 20
            if i in (0, 8, 24):
                s.hit("perc", crash(), bar, 0, 0.55, 0.3)
            if i % 2 == 0:
                s.hit("perc", timpani("F#2" if root == "F#1" else "C#2"), bar, 0, 0.7)
            if half:
                s.hit("drums", kick(), bar, 0, 0.95)
                kicks.append(s.pos(bar, 0))
                s.hit("drums", snare(), bar, 8, 0.7, 0.05)
                for st in range(0, 16, 2):
                    s.hit("drums", snare(True), bar, st, 0.12, -0.1)
            else:
                ks = (0, 4, 8, 12) if sec == "B" else (0, 8, 10)
                for st in ks:
                    s.hit("drums", kick(), bar, st, 0.9)
                    kicks.append(s.pos(bar, st))
                if sec == "B":
                    for st in (4, 12):
                        s.hit("drums", snare(), bar, st, 0.55, 0.05)
                        s.hit("metal", anvil(1900.0, 4), bar, st, 0.3, -0.2)
                    for st in range(2, 16, 4):
                        s.hit("metal", anvil(1200.0, 5), bar, st, 0.2, 0.4)
                    for st in range(16):
                        s.hit("drums", hat(), bar, st, 0.16 if st % 2 == 0 else 0.1, 0.3)
                else:
                    for st, v in enumerate(AS_SNARE):
                        s.hit("drums", snare(v < 0.5), bar, st, v * 0.7, 0.05)
            if i >= 20 and sec == "C" or i >= 30:
                for st in range(16):
                    s.hit("drums", snare(True), bar, st, 0.2 + 0.02 * st, 0.05)
            if i in (20, 30):
                s.hit("fx", riser(s.bar * (4 if i == 20 else 2)), bar, 0, 0.3)
            # march bass and low brass ostinato (8ths)
            if not half:
                for st in range(0, 16, 2):
                    nm = transpose(root, 12 if st in (6, 14) else 0)
                    s.hit("bass", v_dbass(note_freq(nm), round(s.step * 1.2, 4)), bar, st, 0.4)
                if sec in ("A", "D"):
                    for st in (0, 2, 4, 8, 10, 12):
                        s.hit("lowbrass", v_brass(note_freq(transpose(root, 12)), round(s.step * 1.2, 4),
                                                  stab=True), bar, st, 0.3)
            else:
                s.hit("bass", v_dbass(note_freq(root), round(s.step * 15, 4)), bar, 0, 0.35)
            if sec == "B" or sec == "D":
                notes = figure(transpose(root, 24), riff_iv)
                for st in range(16):
                    s.hit("riff", v_riff(note_freq(notes[st % 8]), round(s.step * 0.8, 4)),
                          bar, st, 0.4 if sec == "B" else 0.18, 0.3 if st % 2 else -0.3)
            if sec == "B":
                for st in (6, 14):
                    s.chord("brass", v_trumpet, [transpose(n, 12) for n in pad[1:]], bar, st,
                            dur_steps=1, gain=0.45, spread=0.4, stab=True)
            s.chord("pad", v_pad, pad, bar, dur_steps=16, gain=0.35, bright=1000.0)
            if sec in ("A", "D") and i % 4 in (2, 3):
                for st in (0, 2, 4):
                    s.chord("brass", v_trumpet, [transpose(n, 12) for n in pad[1:]], bar, st,
                            dur_steps=1 if st < 4 else 4, gain=0.45, spread=0.4, stab=True)
        s.play("brass", v_trumpet, AS_TRUMPETS, base, gain=0.6, position=0.1)
        s.play("brass", v_brass, AS_TRUMPETS, base, gain=0.35, octave=-1)
        s.play("brass", v_brass, AS_AUG, base + 16, gain=0.6)
        s.play("organ", v_organ, AS_AUG, base + 16, gain=0.3, octave=1)
        s.play("brass", v_trumpet, AS_TRUMPETS, base + 24, gain=0.65, position=0.1)
        s.play("lowbrass", v_brass, [". . . . . . . ." + " ." * 8] + AS_TRUMPETS[:-1], base + 24,
               gain=0.45, octave=-2)  # canon, a bar behind and two octaves down

    return s, mixdown(s, {
        "drums": dict(hp=35, rev=(1.0, 0.1), seed=231, gain=0.9), "perc": PERC, "fx": FX,
        "metal": dict(hp=200, rev=(1.2, 0.2), seed=232, gain=0.55),
        "bass": dict(hp=40, duck=True, gain=0.6),
        "lowbrass": dict(hp=60, rev=(2.0, 0.2), seed=233, gain=0.6),
        "riff": dict(hp=150, dly=(3, 0.25, 0.2, 3500), gain=0.5),
        "pad": dict(hp=100, duck=True, rev=(3.0, 0.35), wid=1.5, seed=234, gain=0.45),
        "brass": dict(hp=70, rev=(2.4, 0.27), seed=235, gain=0.8),
        "organ": dict(hp=60, rev=(3.0, 0.35), seed=236, gain=0.5),
    }, kicks, depth=0.35)


# ================================================================ 5 final boss: Choir Heart

FI_CH = {
    "Em": (["E3", "G3", "B3", "E4"], "E2"),
    "E": (["E3", "G#3", "B3", "E4"], "E2"),
    "C": (["E3", "G3", "C4", "E4"], "C2"),
    "F": (["F3", "A3", "C4", "F4"], "F2"),
    "B": (["D#3", "F#3", "B3", "D#4"], "B1"),
    "Am": (["E3", "A3", "C4", "E4"], "A1"),
    "D": (["D3", "F#3", "A3", "D4"], "D2"),
}
FI_INTRO = ["Em", "F", "Em", "B"]
FI_LOOP = (["Em", "Em", "C", "C", "F", "F", "B", "B"]          # A: the Choir
           + ["Em", "C", "Am", "B", "Em", "C", "D", "B"]       # B: humanity answers
           + ["Em", "F", "C", "B", "Em", "F", "Am", "B"]       # C: both at once
           + ["Em", "Em", "Em", "Em", "C", "C", "B", "B"]      # D: the heart, Vorne absorbed
           + ["E", "E", "C", "B"])                             # E: E major climax
FI_FANFARE = human_motif("E4")[:1] + [
    "G5 - - - - - - - - - - - F#5 - E5 -",
    "E5 - - - - - C5 - E5 - - - A5 - - -",
    "F#5 - - - - - - - D#5 - - - B4 - - -",
] + human_motif("E4")[:1] + [
    "G5 - - - - - E5 - G5 - - - C6 - - -",
    "A5 - - - - - F#5 - D5 - - - F#5 - A5 -",
    "B5 - - - - - - - D#5 - - - F#5 - - -",
]
FI_COUNTER = human_motif("E4")[:1] + [
    "G5 - - - - - - - - - - - - - - -",
    "G5 - - - - - E5 - C5 - - - E5 - G5 -",
    "F#5 - - - - - - - D#5 - - - B4 - - -",
] + human_motif("E4")[:1] + [
    "G5 - - - - - - - - - - - - - - -",
    "A5 - - - C6 - - - B5 - - - A5 - G5 -",
    "F#5 - - - - - - - D#5 - - - - - - -",
]
FI_CLIMAX = ["E4 - - - - - B4 - E5 - - - - - F#5 -", "G#5 - - - - - - - - - - - B5 - - -",
             "C6 - - - - - - - G5 - - - E5 - - -", "D#5 - - - - - - - F#5 - - - B5 - - -"]
FI_BASS = [0, 0, 12, 0, 0, 0, 12, 1, 0, 0, 12, 0, 7, 0, 12, 1]
FI_REST = ". " * 15 + "."


def final():
    s = LoopSong(150, 4, 36)
    kicks = []
    for bar, name in enumerate(FI_INTRO):
        pad, root = FI_CH[name]
        s.chord("organ", v_organ, pad, bar, dur_steps=16, gain=0.5, spread=0.5)
        s.hit("heart", heartbeat(), bar, 0, 0.9)
        s.hit("heart", heartbeat(), bar, 8, 0.8)
    s.play("choir", v_choir, choir_motif("E5"), 0, gain=0.6)
    s.play("brass", v_brass, choir_motif("E5"), 0, gain=0.4, octave=-1)
    s.play("brass", v_trumpet, human_motif("E4"), 2, gain=0.35, position=0.3)
    for st in range(16):
        s.hit("drums", snare(True), 3, st, 0.15 + 0.035 * st, 0.05)
    s.hit("fx", riser(s.bar), 3, 0, 0.3)

    for base in s.sections():
        for i, name in enumerate(FI_LOOP):
            bar = base + i
            pad, root = FI_CH[name]
            sec = "A" if i < 8 else "B" if i < 16 else "C" if i < 24 else "D" if i < 32 else "E"
            heart = sec == "D" and i < 28
            if i in (0, 8, 16, 20, 32):
                s.hit("perc", crash(), bar, 0, 0.6, 0.3)
            if i in (0, 16, 32) or (sec == "C" and i % 2 == 0):
                s.hit("perc", timpani("E2" if root == "E2" else "B1"), bar, 0, 0.8)
            if heart:
                for st in (0, 8):
                    s.hit("heart", heartbeat(), bar, st, 0.95)
            elif sec == "D":  # build
                for st in (0, 3, 6, 8, 11, 14):
                    s.hit("perc", taiko(), bar, st, 0.7)
                for st in range(0 if i >= 30 else 8, 16):
                    s.hit("drums", snare(True), bar, st, 0.2 + 0.02 * st + 0.08 * (i - 28), 0.05)
                if i == 28:
                    s.hit("fx", riser(s.bar * 4), bar, 0, 0.35)
            else:
                ks = (0, 4, 8, 12) if sec in ("B", "E") else (0, 4, 8, 10, 12)
                for st in ks:
                    s.hit("drums", kick(), bar, st, 0.9 if st % 4 == 0 else 0.6)
                    kicks.append(s.pos(bar, st))
                for st in (4, 12):
                    s.hit("drums", snare(), bar, st, 0.55, 0.05)
                    if sec in ("B", "E"):
                        s.hit("drums", clap(), bar, st, 0.45)
                for st in (range(16) if sec != "A" else range(0, 16, 2)):
                    s.hit("drums", hat(), bar, st, 0.16 if st % 2 else 0.22, 0.3)
                for st in ((0, 8) if sec == "A" else (0, 6, 10)):
                    s.hit("perc", taiko(), bar, st, 0.6)
                if sec == "A":
                    s.hit("heart", heartbeat(), bar, 0, 0.5)
                if i % 4 == 3:
                    for st, p in ((12, 160), (13, 140), (14, 110), (15, 90)):
                        s.hit("drums", tom(p), bar, st, 0.5, (st - 13.5) / 3)
            # bass
            if sec in ("A", "C"):
                for st, sm in enumerate(FI_BASS):
                    s.hit("bass", v_dbass(note_freq(transpose(root, sm)), round(s.step * 0.8, 4)),
                          bar, st, 0.4)
            elif sec in ("B", "E"):
                for st in range(16):
                    if st % 4:
                        nm = transpose(root, 12 if st % 4 == 3 else 0)
                        s.hit("bass", v_bass(note_freq(nm), round(s.step * 0.8, 4)), bar, st, 0.42)
            else:
                s.hit("bass", v_dbass(note_freq(root), round(s.step * 15, 4)), bar, 0, 0.3)
            # strings: gallop in B, 16th chord-tone figures in C and E
            if sec == "B":
                for st, sm in enumerate(GALLOP):
                    s.hit("ost", v_strings_short(note_freq(transpose(root, 12 + sm)),
                                                 round(s.step * 0.75, 4)),
                          bar, st, 0.38 if st % 3 == 0 else 0.25, -0.25)
            if sec in ("C", "E"):
                tones = [transpose(p, 12) for p in pad[1:]]
                for st in range(16):
                    s.hit("ost", v_strings_short(note_freq(tones[[0, 1, 2, 1][st % 4]]),
                                                 round(s.step * 0.7, 4)), bar, st, 0.2, 0.3)
            s.chord("strings", v_strings_long, pad, bar, dur_steps=16, gain=0.4 if not heart else 0.25)
            if sec not in ("A", "C") or i % 4 in (2, 3):
                s.chord("choir", v_choir, pad[1:], bar, dur_steps=16, gain=0.35,
                        vowel="oo" if heart else "ah")
            if sec == "E":
                s.chord("organ", v_organ, pad, bar, dur_steps=16, gain=0.35)
        for off in (0, 4, 16, 20):
            s.play("choir", v_choir, choir_motif("E5"), base + off, gain=0.6)
            s.play("brass", v_brass, choir_motif("E5"), base + off, gain=0.35, octave=-1)
        for off in (2, 6):  # orchestral stabs between the Choir statements
            for b in (0, 1):
                for st in (0, 3, 6, 10):
                    s.chord("brass", v_brass, FI_CH[FI_LOOP[off + b]][0][1:], base + off + b, st,
                            dur_steps=1, gain=0.45, spread=0.4, stab=True)
        s.play("brass", v_trumpet, FI_FANFARE, base + 8, gain=0.6, position=0.15)
        s.play("lead", v_lead, FI_FANFARE, base + 8, gain=0.4, position=-0.1)
        s.play("brass", v_trumpet, FI_COUNTER, base + 16, gain=0.6, position=0.2)
        s.play("brass", v_brass, FI_COUNTER, base + 16, gain=0.35, position=0.1, octave=-1)
        s.play("choir", v_choir, vorne_motif("E3"), base + 24, gain=0.55, vowel="oo")
        s.play("choir", v_choir, [FI_REST, FI_REST] + choir_motif("E4"), base + 24, gain=0.35,
               vowel="oo")
        s.play("brass", v_trumpet, FI_CLIMAX, base + 32, gain=0.65, position=0.15)
        s.play("brass", v_brass, FI_CLIMAX, base + 32, gain=0.45, octave=-1)
        s.play("lead", v_lead, FI_CLIMAX, base + 32, gain=0.45, octave=1)
        s.play("choir", v_choir, FI_CLIMAX, base + 32, gain=0.35)

    return s, mixdown(s, {
        "drums": DRUMS, "perc": PERC, "fx": FX,
        "heart": dict(hp=45, rev=(2.0, 0.2), seed=241, gain=0.9),
        "bass": dict(hp=40, duck=True, gain=0.55),
        "ost": dict(hp=80, rev=(1.8, 0.2), seed=242, gain=0.5),
        "strings": dict(hp=110, rev=(3.0, 0.35), wid=1.5, seed=243, gain=0.45),
        "choir": dict(hp=140, rev=(3.8, 0.45), wid=1.6, seed=244, gain=0.65),
        "brass": dict(hp=70, rev=(2.6, 0.28), seed=245, gain=0.8),
        "lead": dict(hp=180, dly=(3, 0.3, 0.22, 4500), rev=(2.2, 0.22), wid=1.4, seed=246, gain=0.55),
        "organ": dict(hp=50, rev=(3.5, 0.4), seed=247, gain=0.45),
    }, kicks, depth=0.35)


# ================================================================ main

SONGS = {
    "earth": ("earth-theme-r03-a", earth),
    "belt": ("belt-theme-r03-a", belt),
    "jovian": ("jovian-theme-r03-a", jovian),
    "ascendancy": ("ascendancy-boss-r03-a", ascendancy),
    "final": ("final-boss-r03-a", final),
}


def main(argv):
    out = OUT
    if "--out" in argv:
        i = argv.index("--out")
        out = Path(argv[i + 1])
        argv = argv[:i] + argv[i + 2:]
    for key in argv or SONGS:
        name, fn = SONGS[key]
        s, mix = fn()
        mix = mix - mix.mean(axis=1, keepdims=True)
        audio, loop_start, loop_len = s.assemble(mix)
        audio = add_tail(audio, loop_start, 2, 16 * s.step_n)
        audio = master(audio, -14.0, -2.0)
        audio[:, :ns(0.005)] *= np.linspace(0, 1, ns(0.005))
        path = write_ogg(out / f"{name}.ogg", audio,
                         tags={"LOOPSTART": loop_start, "LOOPLENGTH": loop_len})
        dec = decode(path)
        print(f"{path.name}: {audio.shape[1] / SR:.1f}s  loop {loop_start / SR:.3f}s + "
              f"{loop_len / SR:.3f}s  (samples {loop_start}+{loop_len})  "
              f"seam {seam_ratio(dec, loop_start, loop_len):.2f}  "
              f"peak {db(np.abs(dec).max()):.1f} dBFS", flush=True)


if __name__ == "__main__":
    main(sys.argv[1:])
