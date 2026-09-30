#!/usr/bin/env python3
"""Concept round 02 music: five loopable themes in the style of the chosen round-01 sketches.

Outputs (design/audio/music/concept/):
  title-theme-r02-a.ogg    "Terran Vanguard"   title / main menu, 126 BPM, D minor
  hangar-theme-r02-a.ogg   "Dry Dock"          hangar, downtempo swing, 90 BPM, D dorian
  boss-theme-r02-a.ogg     "The Choir Descends" Vrell boss, 150 BPM, E minor / phrygian
  mars-theme-r02-a.ogg     "Red Dust Run"      Act 3 level theme A, 135 BPM, E phrygian dominant
  europa-theme-r02-a.ogg   "Thera Deep"        Act 4 level theme A, 125 BPM, F minor

Usage: python3 tools/concept/audio/music_r02.py [title|hangar|boss|mars|europa ...] [--out DIR]

Every theme is an intro (played once) followed by a loop section. The file is
intro + loop + a 2-bar fade-out tail (what would follow the loop end, faded, so the file also
ends cleanly in an ordinary player). The loop points are stored as Vorbis comments
LOOPSTART / LOOPLENGTH (in samples), which is the convention many engines read; a looping
player never reaches the tail.
To make the loop seamless, the loop is rendered twice and the second pass is kept, so reverb
and delay tails from the loop's end continue into its start. The intro joins it with a
one-beat crossfade. Tempos are chosen so that a 16th step is a whole number of samples,
which keeps the loop sample-exact.
"""
import sys
from functools import lru_cache
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
from music import (clap, crash, hat, hp, kick, ns, riser, sidechain, snare, taiko,  # noqa: E402
                   timpani, tom, transpose, v_arp, v_bass, v_brass, v_lead, v_pad,
                   v_strings_long, v_strings_short, widen)
from synth import (SR, Timeline, adsr, db, decode, delay, exp_decay, fade, master,  # noqa: E402
                   noise, note_freq, parse_track, phase, pulse, ramp, reverb, saturate, saw,
                   sine, supersaw, svf, triangle, write_ogg)

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "design" / "audio" / "music" / "concept"


# ================================================================ loop-aware song

class LoopSong:
    """Timeline on an integer-sample 16th grid with intro + loop rendering."""

    def __init__(self, bpm, intro_bars, loop_bars, swing=0.0, xf_steps=4):
        exact = 15 * SR / bpm
        self.step_n = int(round(exact))
        if abs(exact - self.step_n) > 1e-9:
            raise ValueError(f"{bpm} BPM does not give an integer-sample 16th step")
        self.step = self.step_n / SR
        self.bar = 16 * self.step
        self.intro, self.loop, self.xf_steps = intro_bars, loop_bars, xf_steps
        self.swing_n = int(round(swing * self.step_n))
        self.total_steps = 16 * (intro_bars + 2 * loop_bars) + xf_steps
        self.tl = Timeline(1.0)
        self.tl.n = self.total_steps * self.step_n

    def pos(self, bar, step=0):
        steps = bar * 16 + step
        return (steps * self.step_n + (self.swing_n if steps % 2 else 0)) / SR

    def live(self, bar, step=0):
        return bar * 16 + step < self.total_steps

    def hit(self, group, sig, bar, step=0, gain=1.0, position=0.0):
        if self.live(bar, step):
            self.tl.add(group, sig, self.pos(bar, step), position, gain)

    def play(self, group, voice, bars_tokens, start_bar, gain=1.0, position=0.0, octave=0,
             legato=0.9, **kw):
        for st, ln, name in parse_track(bars_tokens):
            if not self.live(start_bar, st):
                continue
            name = transpose(name, 12 * octave) if octave else name
            sig = voice(note_freq(name), round(ln * self.step * legato, 4), **kw)
            self.tl.add(group, sig, self.pos(start_bar, st), position, gain)

    def chord(self, group, voice, notes, bar, step=0, dur_steps=16, gain=1.0, spread=0.6, **kw):
        if not self.live(bar, step):
            return
        k = len(notes)
        for i, nm in enumerate(notes):
            p = (i / (k - 1) * 2 - 1) * spread if k > 1 else 0
            sig = voice(note_freq(nm), round(dur_steps * self.step, 4), **kw)
            self.tl.add(group, sig, self.pos(bar, step), p, gain / np.sqrt(k))

    def sections(self):
        """Base bars of the loop passes (the second pass is only partly needed)."""
        return [self.intro + k * self.loop for k in range(3)]

    def curve(self, points):
        """Per-sample automation from (bar, value) breakpoints. Loop-relative breakpoints
        are given for the first pass and repeated for the others; intro points use negative
        bars (bar -intro .. 0)."""
        xs, ys = [], []
        for bar, v in points:
            if bar < 0:
                xs.append((bar + self.intro) * 16 * self.step_n)
                ys.append(v)
        for base in self.sections():
            for bar, v in points:
                if bar >= 0:
                    xs.append((base + bar) * 16 * self.step_n)
                    ys.append(v)
        order = np.argsort(xs, kind="stable")
        return np.interp(np.arange(self.tl.n), np.array(xs)[order], np.array(ys)[order])

    def assemble(self, rendered):
        """File = intro + crossfade + second loop pass. Returns (audio, loop_start, loop_len)."""
        i_n = self.intro * 16 * self.step_n
        l_n = self.loop * 16 * self.step_n
        x_n = self.xf_steps * self.step_n
        w = 0.5 - 0.5 * np.cos(np.linspace(0, np.pi, x_n))
        xf = rendered[:, i_n:i_n + x_n] * (1 - w) + rendered[:, i_n + l_n:i_n + l_n + x_n] * w
        audio = np.concatenate([rendered[:, :i_n], xf,
                                rendered[:, i_n + l_n + x_n:i_n + 2 * l_n + x_n]], axis=1)
        return audio, i_n + x_n, l_n


def lp(stereo, cutoff, q=0.707):
    return np.vstack([svf(ch, cutoff, q=q) for ch in stereo])


# ================================================================ extra voices & samples

@lru_cache(None)
def v_choir(freq, dur, vowel="ah", voices=4):
    """Wordless ensemble voice: detuned saws through three vowel formants."""
    n = ns(dur + 0.6)
    t = np.arange(n) / SR
    rng = np.random.default_rng(int(freq * 10) % 9973)
    src = np.zeros(n)
    for i in range(voices):
        det = 1 + (i - (voices - 1) / 2) * 0.007
        vib = 1 + 0.008 * np.sin(2 * np.pi * (4.8 + 0.35 * i) * t + rng.uniform(0, 6.3)) \
            * np.clip(t / 0.5, 0, 1)
        src += saw(freq * det * vib, n, start=rng.uniform())
    src /= np.sqrt(voices)
    formants = {"ah": [(780, 1.0, 5), (1180, 0.55, 6), (2800, 0.22, 8)],
                "oo": [(360, 1.0, 4), (820, 0.35, 6), (2400, 0.08, 8)]}[vowel]
    out = sum(a * svf(src, f, q=q, mode="bp") / q * 3 for f, a, q in formants)
    out += svf(noise(n, rng), 2600, q=1.0, mode="bp") * 0.03
    return out * adsr(n, 0.2, 0.3, 0.85, 0.5, gate=dur)


@lru_cache(None)
def v_epiano(freq, dur, vel=1.0):
    """FM electric piano (DX-style tine)."""
    n = ns(dur + 0.7)
    t = np.arange(n) / SR
    ph, _ = phase(freq, n)
    index = 0.4 + 1.8 * vel * np.exp(-t / 0.3)
    car = np.sin(2 * np.pi * ph + index * np.sin(2 * np.pi * ph))
    tine = np.sin(2 * np.pi * ph * 14) * np.exp(-t / 0.01) * 0.12 * vel
    return (car + tine) * adsr(n, 0.002, 1.4, 0.3, 0.35, gate=dur) * (0.6 + 0.4 * vel)


@lru_cache(None)
def v_flute(freq, dur):
    n = ns(dur + 0.35)
    t = np.arange(n) / SR
    vib = 1 + 0.006 * np.sin(2 * np.pi * 5.2 * t) * np.clip((t - 0.25) / 0.3, 0, 1)
    x = triangle(freq * vib, n) * 0.8 + sine(freq * vib * 2, n) * 0.12
    breath = svf(noise(n, np.random.default_rng(51)), freq * 2, q=2.0, mode="bp") * 0.06
    return (svf(x, 2800, q=0.7) + breath) * adsr(n, 0.05, 0.2, 0.8, 0.25, gate=dur)


@lru_cache(None)
def v_reed(freq, dur):
    """Nasal double-reed lead (mizmar-like) with a scoop into each note."""
    n = ns(dur + 0.2)
    t = np.arange(n) / SR
    vib = 1 + 0.011 * np.sin(2 * np.pi * 6.0 * t) * np.clip((t - 0.15) / 0.25, 0, 1)
    f = freq * vib * ramp(n, 0.944, 1.0, tau=0.03)
    x = pulse(f, n, 0.18)
    x = svf(x, 1100, q=1.8, mode="bp") * 0.9 + svf(x, 3000, q=0.7) * 0.3
    return saturate(x * adsr(n, 0.015, 0.1, 0.85, 0.12, gate=dur) * 1.4, 1.5)


@lru_cache(None)
def v_reese(freq, dur):
    n = ns(dur + 0.05)
    x = (saw(freq * 1.006, n) + saw(freq * 0.994, n, start=0.37)) * 0.5
    x = svf(x, 600 + 1000 * exp_decay(n, 0.07), q=1.4) + sine(freq, n) * 0.45
    return saturate(x * adsr(n, 0.003, 0.1, 0.8, 0.05, gate=dur) * 1.5, 2.0)


@lru_cache(None)
def v_sub(freq, dur):
    n = ns(dur + 0.1)
    x = sine(freq, n) + 0.45 * triangle(freq * 2, n)
    return saturate(svf(x, 800) * adsr(n, 0.008, 0.25, 0.7, 0.08, gate=dur), 1.2)


@lru_cache(None)
def v_pluck(freq, dur, cutoff=2000.0):
    n = ns(dur + 0.2)
    x = pulse(freq, n, 0.35) * 0.7 + saw(freq * 1.004, n) * 0.4
    x = svf(x, cutoff * (0.25 + exp_decay(n, 0.05)), q=3.0)
    return x * adsr(n, 0.001, 0.15, 0.2, 0.12, gate=dur)


@lru_cache(None)
def v_dbass(freq, dur):
    n = ns(dur + 0.04)
    x = saw(freq, n) + 0.6 * pulse(freq, n, 0.3)
    x = saturate(svf(x, 380 + 2600 * exp_decay(n, 0.04), q=2.0) * 2.5, 3.0)
    return x * adsr(n, 0.002, 0.05, 0.8, 0.03, gate=dur)


@lru_cache(None)
def v_riff(freq, dur):
    n = ns(dur + 0.06)
    x = supersaw(freq, n, voices=3, detune=0.01, rng=np.random.default_rng(61))
    x += pulse(freq / 2, n, 0.25) * 0.4
    x = svf(x, 1500 + 3500 * exp_decay(n, 0.05), q=1.5)
    return saturate(x * adsr(n, 0.002, 0.06, 0.7, 0.04, gate=dur) * 2.0, 2.2)


@lru_cache(None)
def doum():
    n = ns(0.35)
    x = sine(ramp(n, 130, 85, tau=0.03), n) * exp_decay(n, 0.09)
    return fade(saturate(x * 1.3, 1.3), 0.0005, 0.02)


@lru_cache(None)
def tek():
    n = ns(0.12)
    x = svf(noise(n, np.random.default_rng(71)), 2600, q=2.0, mode="bp") * exp_decay(n, 0.012)
    x += sine(ramp(n, 520, 420), n) * exp_decay(n, 0.02) * 0.5
    return fade(x * 1.6, 0.0005, 0.01)


@lru_cache(None)
def rim():
    n = ns(0.1)
    x = svf(noise(n, np.random.default_rng(72)), 1800, q=4.0, mode="bp") * exp_decay(n, 0.006)
    x += sine(ramp(n, 900, 750), n) * exp_decay(n, 0.012) * 0.6
    return fade(x * 1.8, 0.0005, 0.01)


@lru_cache(None)
def shaker():
    n = ns(0.08)
    x = svf(noise(n, np.random.default_rng(73)), 7000, q=0.8, mode="hp")
    return fade(x * adsr(n, 0.012, 0.03, 0.2, 0.02), 0.0005, 0.01)


@lru_cache(None)
def clank(seed):
    """Distant metallic hangar clank (inharmonic partials)."""
    rng = np.random.default_rng(seed)
    n = ns(0.9)
    f0 = rng.uniform(180, 420)
    x = sum(a * sine(f0 * r, n) * exp_decay(n, tau)
            for r, a, tau in [(1, 1.0, 0.25), (2.31, 0.6, 0.15), (3.98, 0.4, 0.08), (6.1, 0.2, 0.05)])
    x += svf(noise(n, rng), 3000, q=1.0, mode="bp") * exp_decay(n, 0.01) * 0.8
    return fade(x, 0.0005, 0.05)


@lru_cache(None)
def sonar():
    n = ns(0.9)
    x = sine(ramp(n, 1420, 1390), n) * exp_decay(n, 0.28) * np.minimum(1, np.arange(n) / 30)
    return fade(x, 0.0005, 0.05)


@lru_cache(None)
def whale(f0, f1, seconds):
    n = ns(seconds)
    t = np.arange(n) / SR
    f = ramp(n, f0, f1) * (1 + 0.01 * np.sin(2 * np.pi * 3 * t))
    x = triangle(f, n) * np.sin(np.pi * t / seconds) ** 2
    return svf(x, f0 * 2.5, q=1.5, mode="bp")


@lru_cache(None)
def bubble(seed):
    rng = np.random.default_rng(seed)
    n = ns(0.05)
    x = sine(ramp(n, rng.uniform(300, 600), rng.uniform(1200, 2200)), n) * exp_decay(n, 0.012)
    return fade(x, 0.001, 0.005)


def wind(n, seed=81):
    """Desert-wind drone: band-passed noise with a slowly wandering centre and level."""
    rng = np.random.default_rng(seed)
    t = np.arange(n) / SR
    centre = 650 + 350 * np.sin(2 * np.pi * 0.07 * t) + 200 * np.sin(2 * np.pi * 0.19 * t + 1)
    level = 0.6 + 0.4 * np.sin(2 * np.pi * 0.11 * t + 2) ** 2
    left = svf(noise(n, rng), centre, q=2.5, mode="bp") * level
    right = svf(noise(n, rng), centre * 1.1, q=2.5, mode="bp") * level
    return np.vstack([left, right])


def bass_line(s, group, voice, root, bar, pattern, gain):
    """pattern: list of (step, semitones above root, length in steps)."""
    for st, semis, ln in pattern:
        s.hit(group, voice(note_freq(transpose(root, semis)), round(ln * s.step * 0.85, 4)),
              bar, st, gain)


# ================================================================ motifs
# Humanity's motif (main motif): scale degrees 1-5-8-9-b10, a rising 5-note brass phrase.
# The Vrell Choir motif: degrees 8-b7-b6-5, a tritone drop to b2, then down to 1.
MAIN_MOTIF_D = ["D4 - - - - - A4 - D5 - - - - - E5 -", "F5 - - - - - - - - - - - E5 - D5 -"]
CHOIR_MOTIF_E = ["E5 - - - D5 - - - C5 - - - B4 - - -", "F4 - - - - - - - E4 - - - - - - -"]


# ================================================================ 1 title: Terran Vanguard

T_CH = {
    "Dm": (["D3", "F3", "A3", "D4"], "D2", ["D4", "F4", "A4"]),
    "Bb": (["D3", "F3", "Bb3", "D4"], "Bb1", ["D4", "F4", "Bb4"]),
    "F": (["C3", "F3", "A3", "C4"], "F2", ["C4", "F4", "A4"]),
    "C": (["E3", "G3", "C4", "E4"], "C2", ["C4", "E4", "G4"]),
    "Gm": (["D3", "G3", "Bb3", "D4"], "G1", ["D4", "G4", "Bb4"]),
    "A": (["E3", "A3", "C#4", "E4"], "A1", ["C#4", "E4", "A4"]),
}
T_INTRO = ["Dm", "Dm", "Bb", "A"]
T_LOOP = (["Dm", "Bb", "F", "C", "Dm", "Bb", "Gm", "A"]      # A: motif in brass
          + ["F", "C", "Dm", "Bb", "F", "C", "Bb", "A"]      # B: major lift, supersaw lead
          + ["Dm", "Bb", "F", "A", "Dm", "Bb", "C", "A"])    # C: breakdown + build
T_MEL_A = MAIN_MOTIF_D + [
    "C5 - - - - - A4 - C5 - - - F5 - - -",
    "E5 - - - - - - - G5 - - - E5 - C5 -",
] + MAIN_MOTIF_D[:1] + [
    "F5 - - - D5 - - - F5 - - - Bb5 - - -",
    "A5 - - - G5 - - - D5 - - - G5 - - -",
    "A5 - - - - - - - C#5 - - - E5 - - -",
]
T_MEL_B = [
    "F4 - - - - - C5 - F5 - - - - - G5 -",
    "A5 - - - - - - - - - - - G5 - E5 -",
    "F5 - - - - - D5 - F5 - - - A5 - - -",
    "Bb5 - - - A5 - - - G5 - - - F5 - - -",
    "F4 - - - - - C5 - F5 - - - - - G5 -",
    "A5 - - - - - C6 - - - Bb5 - A5 - G5 -",
    "F5 - - - - - - - D5 - - - F5 - - -",
    "E5 - - - - - - - C#5 - - - - - - -",
]
T_MEL_C = [
    "D4 - - - - - - - - - - - A4 - - -",
    "D5 - - - - - - - E5 - - - - - - -",
    "F5 - - - - - - - - - - - - - - -",
    "E5 - - - - - - - C#5 - - - - - - -",
]
GALLOP = [0, 12, 12, 0, 12, 12, 0, 12, 0, 12, 12, 0, 12, 12, 7, 12]
ARP16 = [0, 1, 2, 3, 2, 1, 0, 1, 0, 1, 2, 3, 4, 3, 2, 1]


def title():
    s = LoopSong(126, 4, 24)
    kicks = []
    # intro: timpani, swelling strings, the motif called out slowly by the horns
    for bar, name in enumerate(T_INTRO):
        pad, root, _ = T_CH[name]
        s.chord("strings", v_strings_long, pad, bar, gain=0.3 + 0.1 * bar, spread=0.7)
        s.hit("perc", timpani("D2" if name == "Dm" else "A1"), bar, 0, 0.9)
    s.play("brass", v_brass, ["D4 - - - - - - - A4 - - - - - - -",
                              "D5 - - - - - - - E5 - - - F5 - - -",
                              "F5 - - - - - - - - - - - E5 - D5 -",
                              "E5 - - - - - - - C#5 - - - - - - -"], 0, gain=0.6)
    for st in range(16):
        s.hit("perc", snare(True), 3, st, 0.15 + 0.035 * st, 0.1)
    s.hit("fx", riser(s.bar), 3, 0, 0.3)
    for st, idx in enumerate(ARP16 * 2):
        _, _, tones = T_CH[T_INTRO[2 + st // 16]]
        ext = tones + [transpose(tones[0], 12), transpose(tones[1], 12)]
        s.hit("arp", v_arp(note_freq(ext[idx]), round(s.step * 0.7, 4), 900.0 + 150 * (st // 4)),
              2, st, 0.22, 0.35 if st % 2 else -0.35)

    for base in s.sections():
        for i, name in enumerate(T_LOOP):
            bar = base + i
            pad, root, tones = T_CH[name]
            ext = tones + [transpose(tones[0], 12), transpose(tones[1], 12)]
            sec = "A" if i < 8 else "B" if i < 16 else "C"
            breakdown = 16 <= i < 20
            s.chord("strings", v_strings_long, pad, bar, gain=0.4 if breakdown else 0.5, spread=0.7)
            if i in (0, 8, 20):
                s.hit("perc", crash(), bar, 0, 0.55, 0.3)
            if not breakdown:
                for beat in range(4):
                    s.hit("drums", kick(), bar, beat * 4, 0.9)
                    kicks.append(s.pos(bar, beat * 4))
                for st in (4, 12):
                    s.hit("drums", clap(), bar, st, 0.55)
                    s.hit("drums", snare(), bar, st, 0.35, 0.1)
                hats = range(16) if sec == "B" else range(2, 16, 4)
                for st in hats:
                    s.hit("drums", hat(), bar, st, 0.2 if sec == "B" and st % 4 != 2 else 0.35, 0.3)
            if sec == "A":
                for st in (0, 6, 10):
                    s.hit("perc", taiko(), bar, st, 0.6 if st else 0.8)
            if sec == "B":
                for st in (0, 8):
                    s.hit("perc", taiko(), bar, st, 0.55)
            if sec != "C" or i >= 20:  # orchestral gallop (quieter under the B lead)
                g = 0.7 if sec == "B" else 1.0
                for st, semis in enumerate(GALLOP):
                    s.hit("ost", v_strings_short(note_freq(transpose(root, 12 + semis)),
                                                 round(s.step * 0.75, 4)),
                          bar, st, g * (0.4 if st % 3 == 0 else 0.26), -0.25)
            if sec == "B":  # tracker rolling bass
                for st in range(16):
                    if st % 4:
                        nt = transpose(root, 12 if st % 4 == 3 else 0)
                        s.hit("bass", v_bass(note_freq(nt), round(s.step * 0.8, 4)), bar, st, 0.4)
            cutoff = 2400.0 if sec == "A" else 3800.0 if sec == "B" else 1600.0 + 400 * (i - 16)
            for st, idx in enumerate(ARP16):
                s.hit("arp", v_arp(note_freq(ext[idx]), round(s.step * 0.7, 4), round(cutoff, -1)),
                      bar, st, 0.18 if sec == "A" else 0.26, 0.35 if st % 2 else -0.35)
            if i >= 20:  # build: brass stabs getting denser, snare roll at the end
                stabs = range(0, 16, 4) if i < 22 else range(0, 16, 2)
                for st in stabs:
                    s.chord("brass", v_brass, tones, bar, st, dur_steps=1, gain=0.45, spread=0.4,
                            stab=True)
                if i == 23:
                    for st in range(16):
                        s.hit("drums", snare(True), bar, st, 0.2 + 0.03 * st, 0.05)
        s.play("brass", v_brass, T_MEL_A, base, gain=0.6, position=-0.1)
        s.play("brass", v_brass, T_MEL_A, base, gain=0.28, position=0.15, octave=-1)
        s.play("lead", v_lead, T_MEL_B, base + 8, gain=0.6, position=0.05)
        s.play("brass", v_brass, T_MEL_B, base + 8, gain=0.45, position=-0.15, octave=-1)
        s.play("strings_mel", v_strings_long, T_MEL_B, base + 8, gain=0.4, octave=-1, attack=0.05)
        s.play("brass", v_brass, T_MEL_C, base + 16, gain=0.55, octave=-1)

    tl = s.tl
    duck = sidechain(tl.n, kicks, depth=0.4)
    drums = hp(tl.group("drums"), 35)
    bass = hp(tl.group("bass"), 40) * duck
    arp = delay(tl.group("arp"), s.step * 3, feedback=0.35, mix=0.3, damp=3000, tail=0)[:, :tl.n]
    ost = reverb(hp(tl.group("ost"), 60), seconds=1.8, mix=0.2, seed=41)[:, :tl.n]
    strings = hp(tl.group("strings"), 110) * (0.6 + 0.4 * duck) + tl.group("strings_mel")
    strings = widen(reverb(strings, seconds=3.0, mix=0.38, seed=42)[:, :tl.n], 1.5)
    brass = reverb(hp(tl.group("brass"), 90), seconds=2.6, mix=0.28, seed=43)[:, :tl.n]
    lead = delay(tl.group("lead"), s.step * 3, feedback=0.3, mix=0.25, damp=4500, tail=0)[:, :tl.n]
    lead = widen(reverb(hp(lead, 180), seconds=2.0, mix=0.2, seed=44)[:, :tl.n], 1.4)
    perc = reverb(hp(tl.group("perc"), 38), seconds=2.6, mix=0.25, damping=0.8, seed=45)[:, :tl.n]
    return s, (drums * 0.85 + bass * 0.7 + arp * 0.5 + ost * 0.6 + strings * 0.55 + brass * 0.8
               + lead * 0.6 + perc * 0.75 + tl.group("fx"))


# ================================================================ 2 hangar: Dry Dock

H_CH = {  # rootless electric-piano voicings, bass root
    "Dm9": (["F3", "A3", "C4", "E4"], "D2"),
    "G13": (["F3", "A3", "B3", "E4"], "G1"),
    "Bbmaj9": (["D3", "F3", "A3", "C4"], "A#1"),
    "Am7": (["G3", "C4", "E4"], "A1"),
    "Gm9": (["F3", "A#3", "D4", "A4"], "G1"),
    "A7": (["G3", "C#4", "E4"], "A1"),
}
H_PROG = ["Dm9", "G13", "Dm9", "G13", "Bbmaj9", "Am7", "Gm9", "A7"]
H_LOOP = H_PROG + H_PROG + H_PROG[4:]            # A groove, B melody, C breakdown
H_MEL = [
    "A4 - - - - - - - D5 - - - E5 - F5 -",       # 5-8-9-b10: humanity's motif, relaxed
    "E5 - - - - - - - - - - - D5 - B4 -",
    "C5 - - - - - A4 - - - - - F4 - G4 -",
    "A4 - - - - - - - - - - - . . . .",
    "D5 - - - - - F5 - - - A5 - - - G5 -",
    "E5 - - - - - - - C5 - - - D5 - E5 -",
    "F5 - - - - - D5 - - - A#4 - - - C5 -",
    "C#5 - - - - - - - E5 - - - - - - -",
]
H_VIBES = [". . . . . . . . A5 - - - D6 - - -", "E6 - - - F6 - - - - - - - - - - -",
           ". . . . . . . . . . . . . . . .", "E6 - - - - - - - C#6 - - - - - - -"]
H_BASS = [(0, 0, 5), (7, 7, 2), (10, 0, 3), (14, 12, 2)]


def hangar():
    s = LoopSong(90, 2, 20, swing=0.3)
    for bar, name in enumerate(["Dm9", "G13"]):
        ep, root = H_CH[name]
        s.chord("ep", v_epiano, ep, bar, 0, dur_steps=14, gain=0.55, spread=0.5, vel=0.6)
        s.chord("pad", v_pad, ep, bar, 0, dur_steps=16, gain=0.2, bright=900.0)
        if bar == 1:
            for st in range(8, 16):
                s.hit("drums", shaker(), bar, st, 0.08 + 0.02 * (st - 8), 0.4)
    s.hit("amb", clank(1), 0, 6, 0.25, -0.7)
    s.hit("amb", clank(2), 1, 3, 0.2, 0.6)

    rng = np.random.default_rng(91)
    clank_slots = [(int(rng.integers(0, 20)), int(rng.integers(0, 16)), int(rng.integers(3, 30)),
                    float(rng.uniform(-0.8, 0.8))) for _ in range(9)]
    for base in s.sections():
        for i, name in enumerate(H_LOOP):
            bar = base + i
            ep, root = H_CH[name]
            breakdown = i >= 16
            s.chord("ep", v_epiano, ep, bar, 0, dur_steps=6, gain=0.5, spread=0.5, vel=0.8)
            s.chord("ep", v_epiano, ep, bar, 10, dur_steps=5, gain=0.38, spread=0.5, vel=0.55)
            s.chord("pad", v_pad, ep, bar, 0, dur_steps=16, gain=0.16, bright=900.0)
            bass_line(s, "bass", v_sub, root, bar,
                      H_BASS if not breakdown else [(0, 0, 12), (14, 12, 2)], 0.42)
            if not breakdown:
                for st in (0, 7, 10):
                    s.hit("drums", kick(110), bar, st, 0.7 if st == 0 else 0.45)
                for st in (4, 12):
                    s.hit("drums", snare(), bar, st, 0.32, 0.05)
                for st in range(0, 16, 2):
                    s.hit("drums", hat(), bar, st, 0.14 if st % 4 else 0.2, 0.3)
                s.hit("drums", hat(), bar, 15, 0.08, 0.3)
            else:
                for st in (4, 12):
                    s.hit("drums", rim(), bar, st, 0.3, -0.1)
            for st in range(16):
                s.hit("drums", shaker(), bar, st, 0.1 if st % 2 else 0.06, 0.4)
        s.play("lead", v_flute, H_MEL, base + 8, gain=0.4, position=0.1)
        s.play("vibes", v_epiano, H_VIBES, base + 16, gain=0.3, position=-0.2, vel=0.5)
        for b, st, seed, p in clank_slots:
            s.hit("amb", clank(seed), base + b, st, 0.18, p)

    tl = s.tl
    t = np.arange(tl.n) / SR
    trem = 1 + 0.25 * np.sin(2 * np.pi * 4.5 * t)  # suitcase auto-pan on the electric piano
    ep = tl.group("ep") * np.vstack([trem, 2 - trem]) + tl.group("vibes")
    ep = reverb(hp(ep, 120), seconds=1.6, mix=0.22, seed=51)[:, :tl.n]
    pad = reverb(hp(tl.group("pad"), 150), seconds=2.5, mix=0.3, seed=52)[:, :tl.n]
    drums = reverb(hp(tl.group("drums"), 35), seconds=0.9, mix=0.12, seed=53)[:, :tl.n]
    drums = lp(drums, 9000)
    bass = hp(tl.group("bass"), 35)
    lead = delay(tl.group("lead"), s.step * 6, feedback=0.3, mix=0.22, damp=3000, tail=0)[:, :tl.n]
    lead = reverb(lead, seconds=2.2, mix=0.25, seed=54)[:, :tl.n]
    amb = reverb(tl.group("amb"), seconds=3.5, mix=0.6, damping=0.8, seed=55)[:, :tl.n]
    return s, (ep * 0.7 + pad * 0.5 + drums * 0.8 + bass * 0.75 + lead * 0.65 + amb * 0.35)


# ================================================================ 3 boss: The Choir Descends

B_CH = {
    "Em": (["E3", "G3", "B3", "E4"], "E2", ["E4", "G4", "B4"]),
    "C": (["E3", "G3", "C4", "E4"], "C2", ["E4", "G4", "C5"]),
    "F": (["F3", "A3", "C4", "F4"], "F2", ["F4", "A4", "C5"]),
    "B": (["D#3", "F#3", "B3", "D#4"], "B1", ["D#4", "F#4", "B4"]),
    "Am": (["E3", "A3", "C4", "E4"], "A1", ["E4", "A4", "C5"]),
}
B_LOOP = (["Em", "Em", "C", "C", "F", "F", "B", "B"]            # A: choir motif
          + ["Em", "Em", "C", "C", "Am", "Am", "B", "B"]        # B: riff
          + ["Em", "Em", "C", "C", "F", "F", "B", "B"]          # A': motif + riff below
          + ["Em", "Em", "F", "F", "C", "C", "B", "B"])         # C: half-time, build
B_RIFF = [
    "E5 . E5 G5 . E5 F5 . E5 . A#5 . A5 . G5 F5",
    "E5 . E5 G5 . E5 F5 . E5 . A#5 . A5 . G5 F5",
    "E5 . E5 G5 . E5 F5 . E5 . C6 . B5 . G5 E5",
    "E5 . E5 G5 . E5 F5 . E5 . C6 . B5 . G5 E5",
    "A5 . A5 C6 . A5 A#5 . A5 . E6 . D6 . C6 A#5",
    "A5 . A5 C6 . A5 A#5 . A5 . E6 . D6 . C6 A#5",
    "B5 . B5 D#6 . B5 C6 . B5 . F6 . E6 . D#6 C6",
    "B5 . B5 D#6 . B5 C6 . B5 . F6 . E6 . D#6 C6",
]
B_BASS = [0, 0, 12, 0, 0, 0, 12, 1, 0, 0, 12, 0, 7, 0, 12, 1]
B_AUG = ["E4 - - - - - - - D4 - - - - - - -", "C4 - - - - - - - B3 - - - - - - -",
         "F3 - - - - - - - - - - - - - - -", "E3 - - - - - - - - - - - - - - -"]


def boss():
    s = LoopSong(150, 4, 32)
    kicks = []
    # intro: low drone, the Choir alone, taiko; then bass and snare roll into the fight
    s.chord("strings", v_strings_long, ["E2", "B2", "E3"], 0, dur_steps=64, gain=0.5)
    s.chord("choir", v_choir, ["E4", "B4"], 0, dur_steps=30, gain=0.35, vowel="oo")
    s.play("choir", v_choir, CHOIR_MOTIF_E, 0, gain=0.55)
    for bar in range(4):
        s.hit("perc", taiko(), bar, 0, 0.9)
        s.hit("perc", taiko(), bar, 10, 0.5)
    for bar in (2, 3):
        root = "E2" if bar == 2 else "B1"
        for st, semis in enumerate(B_BASS):
            s.hit("bass", v_dbass(note_freq(transpose(root, semis)), round(s.step * 0.8, 4)),
                  bar, st, 0.35 + 0.1 * (bar - 2))
    for st in range(32):
        s.hit("drums", snare(True), 2 + st // 16, st % 16, 0.12 + 0.02 * st, 0.05)
    s.hit("fx", riser(s.bar * 2), 2, 0, 0.3)

    for base in s.sections():
        for i, name in enumerate(B_LOOP):
            bar = base + i
            pad, root, stab = B_CH[name]
            sec = "A" if i < 8 else "B" if i < 16 else "A2" if i < 24 else "C"
            half = sec == "C" and i < 28
            if i % 8 == 0:
                s.hit("perc", crash(), bar, 0, 0.55, 0.3)
            if half:
                for st in (0, 10):
                    s.hit("drums", kick(), bar, st, 0.9)
                    kicks.append(s.pos(bar, st))
                s.hit("drums", snare(), bar, 8, 0.6, 0.05)
                for st in (0, 3, 6):
                    s.hit("perc", taiko(), bar, st, 0.7)
            else:
                for st in (0, 4, 8, 10, 12):
                    s.hit("drums", kick(), bar, st, 0.9 if st % 4 == 0 else 0.6)
                    kicks.append(s.pos(bar, st))
                for st in (4, 12):
                    s.hit("drums", snare(), bar, st, 0.55, 0.05)
                for st in (range(16) if sec != "A" else range(0, 16, 2)):
                    s.hit("drums", hat(), bar, st, 0.15 if st % 2 else 0.22, 0.3)
                for st in (0, 8):
                    s.hit("perc", taiko(), bar, st, 0.6)
                if i % 4 == 3:
                    for st, p in ((12, 160), (13, 140), (14, 110), (15, 90)):
                        s.hit("drums", tom(p), bar, st, 0.5, (st - 13.5) / 3)
            if i >= 28:
                for st in (range(0, 16, 2) if i < 30 else range(16)):
                    s.hit("drums", snare(True), bar, st, 0.2 + 0.04 * (i - 28) + 0.01 * st, 0.05)
                if i == 28:
                    s.hit("fx", riser(s.bar * 4), bar, 0, 0.3)
            # distorted bass ostinato
            if not half:
                for st, semis in enumerate(B_BASS):
                    s.hit("bass", v_dbass(note_freq(transpose(root, semis)), round(s.step * 0.8, 4)),
                          bar, st, 0.45)
            else:
                s.hit("bass", v_dbass(note_freq(root), round(s.step * 14, 4)), bar, 0, 0.45)
            # sustained strings + choir chords
            if i % 2 == 0:
                s.chord("strings", v_strings_long, pad, bar, dur_steps=32, gain=0.4, spread=0.7)
            if i % 2 == 0 and not (sec in ("A", "A2") and i % 8 in (0, 4)):
                s.chord("choir", v_choir, stab, bar, dur_steps=30, gain=0.35, spread=0.5)
            # orchestral stabs on a 3-3-2 grid in the A sections
            if sec in ("A", "A2") and i % 8 in (2, 3, 6, 7):
                for st in (0, 3, 6, 10):
                    s.chord("brass", v_brass, stab, bar, st, dur_steps=1, gain=0.5, spread=0.4,
                            stab=True)
            if sec == "B":  # high string tremolo
                for st in range(16):
                    s.hit("ost", v_strings_short(note_freq(transpose(stab[-1], 12)),
                                                 round(s.step * 0.7, 4)), bar, st, 0.18, 0.3)
        for off in (0, 4, 16, 20):  # the Choir motif, twice per A section
            s.play("choir", v_choir, CHOIR_MOTIF_E, base + off, gain=0.6)
            s.play("brass", v_brass, CHOIR_MOTIF_E, base + off, gain=0.3, octave=-1)
        s.play("riff", v_riff, B_RIFF, base + 8, gain=0.4, octave=-1, legato=0.8)
        s.play("riff", v_riff, B_RIFF, base + 16, gain=0.25, octave=-2, legato=0.8)
        s.play("brass", v_brass, B_AUG, base + 24, gain=0.55)

    tl = s.tl
    duck = sidechain(tl.n, kicks, depth=0.35, release=0.1)
    drums = hp(tl.group("drums"), 35)
    bass = hp(tl.group("bass"), 40) * duck
    choir = widen(reverb(hp(tl.group("choir"), 150), seconds=3.5, mix=0.45, seed=61)[:, :tl.n], 1.6)
    strings = reverb(hp(tl.group("strings") + tl.group("ost"), 60), seconds=2.8, mix=0.35,
                     seed=62)[:, :tl.n]
    brass = reverb(hp(tl.group("brass"), 80), seconds=2.4, mix=0.25, seed=63)[:, :tl.n]
    riff = delay(hp(tl.group("riff"), 120), s.step * 3, feedback=0.25, mix=0.2, damp=3500,
                 tail=0)[:, :tl.n]
    perc = reverb(hp(tl.group("perc"), 38), seconds=2.4, mix=0.22, damping=0.8, seed=64)[:, :tl.n]
    return s, (drums * 0.85 + bass * 0.6 + choir * 0.7 + strings * 0.5 + brass * 0.75
               + riff * 0.55 + perc * 0.8 + tl.group("fx"))


# ================================================================ 4 mars: Red Dust Run

M_CH = {  # pad voicing, bass root, arp notes (E phrygian dominant: E F G# A B C D)
    "E": (["E3", "G#3", "B3", "E4"], "E2", ["E4", "F4", "G#4", "B4", "E5", "B4", "G#4", "F4"]),
    "F": (["F3", "A3", "C4", "F4"], "F2", ["F4", "A4", "C5", "E5", "F5", "E5", "C5", "A4"]),
    "Dm": (["D3", "F3", "A3", "D4"], "D2", ["D4", "F4", "A4", "C5", "D5", "C5", "A4", "F4"]),
    "Am": (["E3", "A3", "C4", "E4"], "A1", ["A4", "B4", "C5", "E5", "A5", "E5", "C5", "B4"]),
    "G": (["D3", "G3", "B3", "D4"], "G1", ["G4", "B4", "D5", "F5", "G5", "F5", "D5", "B4"]),
}
M_LOOP = (["E", "F", "E", "Dm", "Am", "F", "Dm", "E"]           # A
          + ["Am", "G", "F", "E", "Am", "G", "F", "E"]          # B: Andalusian cadence
          + ["E", "E", "F", "F", "Dm", "Dm", "F", "F"])         # C: dust storm + build
M_MEL_A = [
    "E5 - - F5 G#5 - - - F5 - E5 - - - - -",
    "F5 - - - A5 - - - G#5 - F5 - E5 - F5 -",
    "G#5 - - - - - B5 - A5 - G#5 - F5 - - -",
    "F5 - - - - - E5 - D5 - - - . . . .",
    "A5 - - - - - C6 - B5 - A5 - G#5 - A5 -",
    "C6 - - - - - - - A5 - - - F5 - - -",
    "D5 - - - F5 - - - E5 - D5 - C5 - D5 -",
    "E5 - - - - - - - - - - - . . . .",
]
M_MEL_B = [
    "A5 - C6 - B5 - A5 - G#5 - A5 - B5 - C6 -",
    "B5 - - - - - - - G5 - A5 - B5 - - -",
    "A5 - - - C6 - - - B5 - A5 - G#5 - - -",
    "G#5 - - - - - - - E5 - - - - - - -",
    "E6 - - - D6 - C6 - B5 - C6 - D6 - E6 -",
    "D6 - - - - - B5 - G5 - - - D6 - - -",
    "C6 - - - A5 - - - F5 - A5 - B5 - C6 -",
    "B5 - - - - - G#5 - F5 - - - E5 - - -",
]
M_CALL = [
    "B4 - - - - - - - - - - - E5 - F5 -",
    "G#5 - - - - - - - - - - - F5 - E5 -",
    "F5 - - - - - - - - - - - . . . .",
    "A5 - - - - - G#5 - F5 - - - E5 - - -",
]
M_BREAK = {"kick": [0, 2, 10, 11], "snare": [4, 7, 9, 12, 15]}
M_BASS = [(0, 0, 3), (3, 0, 3), (6, 0, 2), (8, 0, 3), (11, 0, 3), (14, 12, 2)]
M_TRIBAL = [(0, "d"), (3, "t"), (4, "t"), (6, "d"), (8, "d"), (10, "t"), (11, "t"), (14, "t")]


def mars():
    s = LoopSong(135, 4, 24)
    drum_cut = [(-4, 900.0), (0, 12000.0), (16, 12000.0), (16.01, 700.0), (20, 700.0),
                (23.75, 12000.0), (24, 12000.0)]
    for bar in range(4):  # intro: wind, drone, hand drums, the lonely reed
        for st, kind in M_TRIBAL:
            s.hit("tribal", doum() if kind == "d" else tek(), bar, st, 0.6 if kind == "d" else 0.4,
                  -0.2 if kind == "d" else 0.3)
    s.chord("pad", v_strings_long, ["E2", "B2", "E3"], 0, dur_steps=64, gain=0.45)
    s.play("lead", v_reed, M_CALL, 0, gain=0.5, position=0.1)
    for st in range(8, 16):
        s.hit("drums", snare(True), 3, st, 0.2 + 0.05 * (st - 8), 0.1)

    for base in s.sections():
        for i, name in enumerate(M_LOOP):
            bar = base + i
            pad, root, arp = M_CH[name]
            sec = "A" if i < 8 else "B" if i < 16 else "C"
            storm = 16 <= i < 20
            if i in (0, 8):
                s.hit("perc", crash(), bar, 0, 0.5, 0.3)
            for st in M_BREAK["kick"]:
                s.hit("drums", kick(150), bar, st, 0.85 if st in (0, 10) else 0.6)
            for st in M_BREAK["snare"]:
                s.hit("drums", snare(st in (7, 9, 15)), bar, st, 0.6 if st in (4, 12) else 0.3, 0.05)
            for st in range(0, 16, 2):
                s.hit("drums", hat(st % 4 == 2), bar, st, 0.16 if st % 4 else 0.22, 0.3)
            if i % 4 == 3 and not storm:
                for st, p in ((12, 180), (13, 150), (14, 120), (15, 95)):
                    s.hit("drums", tom(p), bar, st, 0.45, (st - 13.5) / 3)
            for st, kind in M_TRIBAL:
                s.hit("tribal", doum() if kind == "d" else tek(), bar, st,
                      (0.45 if kind == "d" else 0.3) * (1.3 if storm else 1.0),
                      -0.2 if kind == "d" else 0.3)
            for st in range(16):
                s.hit("tribal", shaker(), bar, st, 0.08 if st % 2 else 0.12, -0.4)
            if not storm:
                bass_line(s, "bass", v_reese, root, bar, M_BASS, 0.5)
            else:
                s.hit("bass", v_reese(note_freq(root), round(s.step * 15, 4)), bar, 0, 0.4)
            s.chord("pad", v_strings_long, pad, bar, dur_steps=16, gain=0.3 if sec != "B" else 0.38,
                    spread=0.7)
            if sec == "B" or i >= 20:
                for st in range(16):
                    s.hit("arp", v_arp(note_freq(arp[st % 8]), round(s.step * 0.7, 4), 2800.0),
                          bar, st, 0.2, 0.35 if st % 2 else -0.35)
            if i == 23:
                for st in range(16):
                    s.hit("drums", snare(True), bar, st, 0.2 + 0.03 * st, 0.05)
        s.play("lead", v_reed, M_MEL_A, base, gain=0.5, position=0.1)
        s.play("lead", v_reed, M_MEL_B, base + 8, gain=0.48, position=0.1)
        s.play("lead", v_reed, M_CALL, base + 16, gain=0.45, position=-0.1)

    tl = s.tl
    cut = s.curve(drum_cut)
    drums = np.vstack([svf(ch, cut, q=0.9) for ch in hp(tl.group("drums"), 35)])
    drums = reverb(drums, seconds=0.8, mix=0.1, seed=71)[:, :tl.n]
    tribal = reverb(tl.group("tribal"), seconds=1.4, mix=0.25, seed=72)[:, :tl.n]
    bass = hp(tl.group("bass"), 38)
    pad = widen(reverb(hp(tl.group("pad"), 70), seconds=3.0, mix=0.4, seed=73)[:, :tl.n], 1.5)
    arp = delay(tl.group("arp"), s.step * 3, feedback=0.35, mix=0.3, damp=2500, tail=0)[:, :tl.n]
    lead = delay(tl.group("lead"), s.step * 6, feedback=0.3, mix=0.22, damp=3000, tail=0)[:, :tl.n]
    lead = reverb(lead, seconds=2.4, mix=0.28, seed=74)[:, :tl.n]
    air = wind(tl.n) * s.curve([(-4, 1.0), (0, 0.5), (16, 0.5), (16.01, 1.1), (20, 1.1),
                                (21, 0.5), (24, 0.5)])
    return s, (drums * 0.85 + tribal * 0.6 + bass * 0.7 + pad * 0.5 + arp * 0.45 + lead * 0.58
               + air * 0.35 + tl.group("perc") * 0.6)


# ================================================================ 5 europa: Thera Deep

E_CH = {  # pad voicing, bass root, arp notes
    "Fm9": (["F3", "G#3", "C4", "G4"], "F2", ["F4", "C5", "G#4", "C5", "G5", "C5", "G#4", "C5"]),
    "Db": (["C#3", "F3", "G#3", "C4"], "C#2", ["F4", "C5", "G#4", "C5", "F5", "C5", "G#4", "C5"]),
    "Bbm9": (["A#2", "C#3", "F3", "C4"], "A#1", ["F4", "C#5", "A#4", "C#5", "C5", "C#5", "A#4", "F4"]),
    "Csus": (["C3", "F3", "G3", "C4"], "C2", ["F4", "C5", "G4", "C5", "F5", "C5", "G4", "C5"]),
    "C": (["C3", "E3", "G3", "C4"], "C2", ["E4", "C5", "G4", "C5", "E5", "C5", "G4", "C5"]),
    "Eb": (["D#3", "G3", "A#3", "D#4"], "D#2", ["G4", "D#5", "A#4", "D#5", "G5", "D#5", "A#4", "D#5"]),
}
E_LOOP = (["Fm9", "Fm9", "Db", "Db", "Bbm9", "Bbm9", "Csus", "C"]    # A
          + ["Db", "Eb", "Fm9", "Fm9", "Db", "Eb", "Csus", "C"]      # B: melody, surfacing
          + ["Fm9", "Fm9", "Db", "Db", "Bbm9", "Bbm9", "Csus", "C"])  # C: deep breakdown + build
E_MEL = [
    "F5 - - - - - - - G#5 - - - G5 - - -",
    "G5 - - - - - - - - - - - A#5 - - -",
    "C6 - - - - - - - - - - - G#5 - G5 -",
    "F5 - - - - - - - - - - - . . . .",
    "F5 - - - - - - - G#5 - - - C6 - - -",
    "A#5 - - - - - - - G5 - - - D#5 - - -",
    "F5 - - - - - - - G5 - - - - - - -",
    "E5 - - - - - - - G5 - - - - - - -",
]
E_ECHO = ["C5 - - - - - - - . . . . . . . .", ". . . . . . . . G#4 - - - G4 - - -",
          "F4 - - - - - - - . . . . . . . .", ". . . . . . . . . . . . . . . ."]


def europa():
    s = LoopSong(125, 4, 24)
    kicks = []
    muffle = s.curve([(-4, 700.0), (0, 1800.0), (8, 1800.0), (12, 4800.0), (15, 4800.0),
                      (16, 1300.0), (20, 1300.0), (24, 1800.0)])
    # intro: pad, sonar, a distant whale, bubbles; the bass stirs
    for bar, name in enumerate(["Fm9", "Fm9", "Db", "Csus"]):
        pad, root, _ = E_CH[name]
        s.chord("pad", v_pad, pad, bar, dur_steps=16, gain=0.45, bright=1200.0)
        if bar >= 2:
            for st in range(16):
                s.hit("bass", v_bass(note_freq(transpose(root, 12 if st % 4 == 3 else 0)),
                                     round(s.step * 0.7, 4)), bar, st, 0.25 + 0.1 * (bar - 2))
    s.hit("ping", sonar(), 0, 0, 0.5)
    s.hit("ping", sonar(), 2, 0, 0.5)
    s.hit("fx", whale(180.0, 120.0, 2.8), 0, 8, 0.35, -0.5)
    s.hit("fx", riser(s.bar), 3, 0, 0.2)

    rng = np.random.default_rng(101)
    bubbles = [(int(rng.integers(0, 24)), int(rng.integers(0, 16)), int(rng.integers(0, 50)),
                float(rng.uniform(-0.9, 0.9))) for _ in range(40)]
    for base in s.sections():
        for i, name in enumerate(E_LOOP):
            bar = base + i
            pad, root, arp = E_CH[name]
            sec = "A" if i < 8 else "B" if i < 16 else "C"
            deep = 16 <= i < 20
            if not deep:
                for beat in range(4):
                    s.hit("drums", kick(120), bar, beat * 4, 0.85)
                    kicks.append(s.pos(bar, beat * 4))
                for st in range(2, 16, 4):
                    s.hit("drums", hat(True), bar, st, 0.18, 0.3)
                if sec == "B":
                    for st in (4, 12):
                        s.hit("drums", clap(), bar, st, 0.35)
            if i in (0, 8):
                s.hit("perc", crash(), bar, 0, 0.3, 0.3)
            if not deep:
                for st in range(16):
                    if st % 4:
                        nt = transpose(root, 12 if st % 4 == 3 else 0)
                        s.hit("bass", v_bass(note_freq(nt), round(s.step * 0.7, 4)), bar, st, 0.45)
            else:
                s.hit("bass", v_sub(note_freq(root), round(s.step * 15, 4)), bar, 0, 0.6)
            s.chord("pad", v_pad, pad, bar, dur_steps=16, gain=0.45, bright=1600.0)
            for st in range(16):
                s.hit("arp", v_pluck(note_freq(arp[st % 8]), round(s.step * 0.6, 4), 2200.0),
                      bar, st, 0.26 if not deep else 0.14, 0.4 if st % 2 else -0.4)
            if i % 2 == 0:
                s.hit("ping", sonar(), bar, 0, 0.4)
            if i == 19:
                s.hit("fx", riser(s.bar * 4), bar, 0, 0.25)
        s.play("lead", v_lead, E_MEL, base + 8, gain=0.4)
        s.play("lead", v_flute, E_MEL, base + 8, gain=0.25, octave=-1)
        s.play("lead", v_flute, E_ECHO, base + 16, gain=0.4)
        s.hit("fx", whale(160.0, 240.0, 3.2), base + 16, 4, 0.35, 0.5)
        s.hit("fx", whale(220.0, 130.0, 3.6), base + 18, 8, 0.3, -0.5)
        for b, st, seed, p in bubbles:
            s.hit("fx", bubble(seed), base + b, st, 0.2, p)

    tl = s.tl
    duck = sidechain(tl.n, kicks, depth=0.5, release=0.15)
    drums = hp(tl.group("drums"), 35)
    bass = hp(tl.group("bass"), 38) * duck
    pad = reverb(hp(tl.group("pad"), 120) * duck, seconds=4.0, mix=0.45, seed=81)[:, :tl.n]
    arp = delay(tl.group("arp"), s.step * 3, feedback=0.55, mix=0.45, damp=1800, tail=0)[:, :tl.n]
    lead = delay(tl.group("lead"), s.step * 6, feedback=0.5, mix=0.4, damp=2500, tail=0)[:, :tl.n]
    lead = widen(reverb(hp(lead, 150), seconds=4.0, mix=0.4, seed=82)[:, :tl.n], 1.5)
    under = drums * 0.8 + bass * 0.75 + widen(pad, 1.6) * 0.55 + arp * 0.4 + lead * 0.7 \
        + tl.group("perc") * 0.5
    under = np.vstack([svf(ch, muffle, q=1.1) for ch in under])  # the water
    ping = delay(tl.group("ping"), s.step * 6, feedback=0.6, mix=0.5, damp=2500, tail=0)[:, :tl.n]
    ping = reverb(ping, seconds=4.5, mix=0.6, seed=83)[:, :tl.n]
    fx = reverb(tl.group("fx"), seconds=4.0, mix=0.5, seed=84)[:, :tl.n]
    return s, under + ping * 0.25 + fx * 0.4


# ================================================================ main

SONGS = {
    "title": ("title-theme-r02-a", title),
    "hangar": ("hangar-theme-r02-a", hangar),
    "boss": ("boss-theme-r02-a", boss),
    "mars": ("mars-theme-r02-a", mars),
    "europa": ("europa-theme-r02-a", europa),
}


def seam_ratio(x, loop_start, loop_len):
    """Jump at the loop point relative to the typical sample-to-sample change nearby
    (about 1 means no audible discontinuity)."""
    end = loop_start + loop_len
    jump = np.abs(x[:, loop_start] - x[:, end - 1]).max()
    near = np.abs(np.diff(x[:, end - ns(0.05):end], axis=1))
    return jump / (np.percentile(near, 99) + 1e-12)


def add_tail(audio, loop_start, bars, bar_n):
    """Append what follows the loop end (the loop start again), faded out, so the file
    also ends cleanly in a player that ignores the loop points."""
    n = bars * bar_n
    w = 0.5 + 0.5 * np.cos(np.linspace(0, np.pi, n))
    return np.concatenate([audio, audio[:, loop_start:loop_start + n] * w], axis=1)


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
        audio[:, :ns(0.005)] *= np.linspace(0, 1, ns(0.005))  # fade-in only: the end loops
        path = write_ogg(out / f"{name}.ogg", audio,
                         tags={"LOOPSTART": loop_start, "LOOPLENGTH": loop_len})
        dec = decode(path)
        print(f"{path.name}: {audio.shape[1] / SR:.1f}s  loop {loop_start / SR:.3f}s + "
              f"{loop_len / SR:.3f}s  (samples {loop_start}+{loop_len})  "
              f"seam {seam_ratio(dec, loop_start, loop_len):.2f}  "
              f"peak {db(np.abs(dec).max()):.1f} dBFS")


if __name__ == "__main__":
    main(sys.argv[1:])
