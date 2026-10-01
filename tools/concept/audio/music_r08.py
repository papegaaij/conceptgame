#!/usr/bin/env python3
"""Concept round 08 music: the cues still missing for Acts 1-2 and full-length versions of
the chosen sketches.

Outputs (design/audio/music/concept/):
  New cues
    briefing-theme-r08-a.ogg        "Situation Room"    #3  briefing, 100 BPM, D minor, loop
    act2-b-theme-r08-a.ogg          "Firestorm"         #7  Act 2 B, 140 BPM, G minor, loop
    miniboss-sting-r08-a.ogg        "Contact Heavy"     #21 mini-boss stinger, ~4 s
    boss-warning-r08-a.ogg          "Red Alert"         #22 boss warning, 3 bars at 150 BPM
    mission-complete-r08-a.ogg      "Mission Complete"  #23 victory jingle, ~6 s
    act-complete-r08-a.ogg          "Act Complete"      #24 fanfare, ~16 s
    mission-failed-r08-a.ogg        "Mission Failed"    #25 downbeat sting, ~5 s
    game-over-r08-a.ogg             "Game Over"         #26 somber, ~20 s
  Full-length versions of chosen sketches (same material, more sections and variation)
    title-theme-full-r08-a.ogg      "Terran Vanguard"   126 BPM, D minor, 64-bar loop
    hangar-theme-full-r08-a.ogg     "Dry Dock"          90 BPM swing, D dorian, 56-bar loop
    afterburner-full-r08-a.ogg      "Afterburner"       140 BPM, A minor, 80-bar loop
    coalition-rising-full-r08-a.ogg "Coalition Rising"  135 BPM (was 132), D minor, 72-bar loop
    homefront-full-r08-a.ogg        "Homefront"         147 BPM, C minor, 80-bar loop
    choir-descends-full-r08-a.ogg   "The Choir Descends" 150 BPM, E minor, 72-bar loop

Usage: python3 tools/concept/audio/music_r08.py [name ...] [--out DIR]
       names: briefing act2b miniboss warning complete actcomplete failed gameover
              title hangar afterburner coalition homefront choir

Loops use the round-02 format (intro + loop + 2-bar fade tail, LOOPSTART / LOOPLENGTH Vorbis
comments, sample-exact). "Coalition Rising" moves from 132 to 135 BPM because a loop needs a
16th step of a whole number of samples. Stings are one-shot files with a short fade.
"""
import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
from music import (A_CHORDS, A_LEAD, ARP_STEPS, B_BARS, B_CHORDS, B_THEME, Song,  # noqa: E402
                   clap, crash, hat, hp, kick, ns, riser, sidechain, snare, taiko, timpani,
                   tom, transpose, v_arp, v_bass, v_brass, v_lead, v_pad, v_strings_long,
                   v_strings_short, widen)
from music_r02 import (ARP16, B_AUG, B_BASS, B_CH, B_RIFF, CHOIR_MOTIF_E, GALLOP,  # noqa: E402
                       H_BASS, H_CH, H_MEL, H_PROG, H_VIBES, T_CH, T_INTRO, T_MEL_A, T_MEL_B,
                       T_MEL_C, LoopSong, add_tail, bass_line, clank, rim, seam_ratio, shaker,
                       sonar, v_choir, v_dbass, v_epiano, v_flute, v_pluck, v_reese, v_riff,
                       v_sub)
from music_r03 import (EA_CH, EA_HORNS, EA_INTRO, EA_LAMENT, earth_riff,  # noqa: E402
                       human_motif, mixdown, radio_static, siren, thunder, v_tremolo,
                       v_trumpet)
from synth import (SR, db, decode, delay, fade, master, note_freq, parse_track,  # noqa: E402
                   pulse, ramp, reverb, svf, write_ogg)

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "design" / "audio" / "music" / "concept"
REST = ". " * 15 + "."


def shifted(bars, steps):
    """Delay a pattern by `steps` 16ths (repacked into 16-step bars)."""
    toks = ["."] * steps + sum((b.split() for b in bars), [])
    toks += ["."] * ((-len(toks)) % 16)
    return [" ".join(toks[i:i + 16]) for i in range(0, len(toks), 16)]


def plan_len(plan):
    return sum(len(c) for _, c in plan)


def walk(plan, base):
    """Yield (kind, section start bar, j, chord, bar) for a section plan from `base`."""
    off = 0
    for kind, chords in plan:
        for j, name in enumerate(chords):
            yield kind, base + off, j, name, base + off + j
        off += len(chords)


def starts(plan, base):
    """[(kind, start bar)] for every section."""
    out, off = [], 0
    for kind, chords in plan:
        out.append((kind, base + off))
        off += len(chords)
    return out


def drum_fill(s, bar, kicks):
    """Breakbeat fill over the last two beats (as in Afterburner)."""
    for st, smp, g in [(8, kick(), 1.0), (10, snare(), 0.8), (11, kick(), 0.8),
                       (13, snare(True), 0.6), (14, snare(), 0.8), (15, snare(True), 0.9)]:
        s.hit("drums", smp, bar, st, g)
        if smp is kick():
            kicks.append(s.pos(bar, st))
    s.hit("drums", tom(140), bar, 12, 0.6, -0.4)
    s.hit("drums", tom(100), bar, 14, 0.6, 0.4)


def snare_roll(s, bar, start=0.18, slope=0.035, eighths=False, group="drums"):
    for st in (range(0, 16, 2) if eighths else range(16)):
        s.hit(group, snare(True), bar, st, start + slope * st, 0.05)


# ================================================================ 1 title (full)

TF_CH = dict(T_CH, Eb=(["D#3", "G3", "A#3", "D#4"], "D#2", ["D#4", "G4", "A#4"]))
TF_A = ["Dm", "Bb", "F", "C", "Dm", "Bb", "Gm", "A"]
TF_B = ["F", "C", "Dm", "Bb", "F", "C", "Bb", "A"]
TF_PLAN = [("A", TF_A), ("B", TF_B), ("A2", TF_A),
           ("D", ["Gm", "Eb", "Bb", "F", "Gm", "Eb", "C", "A"]),
           ("C", ["Dm", "Bb", "F", "A", "Dm", "Bb", "C", "A"]),
           ("B2", TF_B), ("A3", TF_A),
           ("E", ["Dm", "Dm", "Bb", "A", "Gm", "Gm", "Bb", "A"])]
TF_COUNTER = ["A3 - - - - - - - F3 - - - - - - -", "F3 - - - - - - - D3 - - - - - - -",
              "C4 - - - - - - - A3 - - - - - - -", "G3 - - - - - - - E3 - - - - - - -",
              "F3 - - - - - - - A3 - - - - - - -", "A#3 - - - - - - - D4 - - - - - - -",
              "D4 - - - - - - - A#3 - - - - - - -", "C#4 - - - - - - - E4 - - - - - - -"]
TF_BRIDGE = ["G4 - - - - - A#4 - D5 - - - - - C5 -", "A#4 - - - - - - - G4 - - - A#4 - - -",
             "F5 - - - - - D5 - A#4 - - - D5 - F5 -", "A5 - - - - - - - G5 - - - F5 - - -",
             "G5 - - - - - F5 - D5 - - - A#4 - D5 -", "D#5 - - - - - - - G5 - - - A#5 - - -",
             "C6 - - - - - A#5 - G5 - - - E5 - - -", "C#5 - - - - - - - E5 - - - A5 - - -"]
TF_CALL = ["D4 - - - - - - - A4 - - - - - - -", "D5 - - - - - - - E5 - - - F5 - - -",
           "F5 - - - - - - - - - - - E5 - D5 -", "E5 - - - - - - - C#5 - - - - - - -"]
TF_CALL2 = ["G4 - - - - - - - D5 - - - - - - -", "D5 - - - - - - - F5 - - - G5 - - -",
            "F5 - - - - - - - - - - - D5 - - -", "E5 - - - - - - - C#5 - - - - - - -"]


def title_full():
    s = LoopSong(126, 4, plan_len(TF_PLAN))
    kicks = []
    # intro, as in round 02: timpani, swelling strings, the motif called out by the horns
    for bar, name in enumerate(T_INTRO):
        pad, root, _ = TF_CH[name]
        s.chord("strings", v_strings_long, pad, bar, gain=0.3 + 0.1 * bar, spread=0.7)
        s.hit("perc", timpani("D2" if name == "Dm" else "A1"), bar, 0, 0.9)
    s.play("brass", v_brass, TF_CALL, 0, gain=0.6)
    snare_roll(s, 3, 0.15)
    s.hit("fx", riser(s.bar), 3, 0, 0.3)
    for st, idx in enumerate(ARP16 * 2):
        _, _, tones = TF_CH[T_INTRO[2 + st // 16]]
        ext = tones + [transpose(tones[0], 12), transpose(tones[1], 12)]
        s.hit("arp", v_arp(note_freq(ext[idx]), round(s.step * 0.7, 4), 900.0 + 150 * (st // 4)),
              2, st, 0.22, 0.35 if st % 2 else -0.35)

    for base in s.sections():
        for kind, start, j, name, bar in walk(TF_PLAN, base):
            pad, root, tones = TF_CH[name]
            ext = tones + [transpose(tones[0], 12), transpose(tones[1], 12)]
            quiet = (kind == "C" and j < 4) or (kind == "E" and j < 4)
            big = kind in ("B2", "A3")
            s.chord("strings", v_strings_long, pad, bar, gain=0.4 if quiet else 0.5, spread=0.7)
            if (j == 0 and kind not in ("C", "E")) or (j == 4 and kind in ("C", "E")):
                s.hit("perc", crash(), bar, 0, 0.55, 0.3)
            if kind == "D":  # half-time bridge
                for st in (0, 10):
                    s.hit("drums", kick(), bar, st, 0.9)
                    kicks.append(s.pos(bar, st))
                s.hit("drums", clap(), bar, 8, 0.55)
                s.hit("drums", snare(), bar, 8, 0.4, 0.1)
                for st in range(2, 16, 4):
                    s.hit("drums", hat(), bar, st, 0.3, 0.3)
                for st in (0, 6, 10):
                    s.hit("perc", taiko(), bar, st, 0.7 if st else 0.85)
            elif not quiet:
                for beat in range(4):
                    s.hit("drums", kick(), bar, beat * 4, 0.9)
                    kicks.append(s.pos(bar, beat * 4))
                for st in (4, 12):
                    s.hit("drums", clap(), bar, st, 0.55)
                    s.hit("drums", snare(), bar, st, 0.35, 0.1)
                sixteen = kind in ("B", "B2", "A3")
                for st in (range(16) if sixteen else range(2, 16, 4)):
                    s.hit("drums", hat(), bar, st, 0.2 if sixteen and st % 4 != 2 else 0.35, 0.3)
                if kind in ("A", "A2", "A3"):
                    for st in (0, 6, 10):
                        s.hit("perc", taiko(), bar, st, 0.6 if st else 0.8)
                if kind in ("B", "B2"):
                    for st in (0, 8):
                        s.hit("perc", taiko(), bar, st, 0.55)
            if kind == "E" and j < 4:
                tim = {"Dm": "D2", "A": "A1", "Gm": "G1"}.get(name, "A#1")
                s.hit("perc", timpani(tim), bar, 0, 0.85)
            if not (kind == "C" and j < 4):  # orchestral gallop
                g = {"B": 0.7, "B2": 0.75, "D": 0.5, "E": 0.85}.get(kind, 1.0)
                for st, semis in enumerate(GALLOP):
                    s.hit("ost", v_strings_short(note_freq(transpose(root, 12 + semis)),
                                                 round(s.step * 0.75, 4)),
                          bar, st, g * (0.4 if st % 3 == 0 else 0.26), -0.25)
            if kind in ("B", "B2", "A3") or (kind == "E" and j >= 4):  # tracker rolling bass
                for st in range(16):
                    if st % 4:
                        nt = transpose(root, 12 if st % 4 == 3 else 0)
                        s.hit("bass", v_bass(note_freq(nt), round(s.step * 0.8, 4)), bar, st, 0.4)
            elif kind == "D":
                s.hit("bass", v_bass(note_freq(root), round(s.step * 14, 4)), bar, 0, 0.42)
            cutoff = {"A": 2400.0, "A2": 2800.0, "A3": 3200.0, "B": 3800.0, "B2": 4200.0,
                      "D": 2000.0}.get(kind, 1400.0 + 350 * j)
            for st, idx in enumerate(ARP16):
                s.hit("arp", v_arp(note_freq(ext[idx]), round(s.step * 0.7, 4), round(cutoff, -1)),
                      bar, st, 0.26 if kind in ("B", "B2") else 0.18, 0.35 if st % 2 else -0.35)
            if kind in ("A2", "D", "B2", "A3"):
                s.chord("choir", v_choir, pad[1:], bar, dur_steps=16, gain=0.3 if not big else 0.36)
            if (kind == "C" and j >= 4) or (kind == "E" and j >= 6):
                stabs = range(0, 16, 4) if (kind == "C" and j < 6) else range(0, 16, 2)
                for st in stabs:
                    s.chord("brass", v_brass, tones, bar, st, dur_steps=1, gain=0.45, spread=0.4,
                            stab=True)
            if j == 7 and kind in ("C", "E"):
                snare_roll(s, bar, 0.2, 0.03)
            if (kind == "C" and j == 4) or (kind == "E" and j == 6):
                s.hit("fx", riser(s.bar * (4 if kind == "C" else 2)), bar, 0, 0.3)
        for kind, st0 in starts(TF_PLAN, base):
            if kind == "A":
                s.play("brass", v_brass, T_MEL_A, st0, gain=0.6, position=-0.1)
                s.play("brass", v_brass, T_MEL_A, st0, gain=0.28, position=0.15, octave=-1)
            elif kind in ("B", "B2"):
                s.play("lead", v_lead, T_MEL_B, st0, gain=0.6, position=0.05)
                s.play("brass", v_brass, T_MEL_B, st0, gain=0.45, position=-0.15, octave=-1)
                s.play("strings_mel", v_strings_long, T_MEL_B, st0, gain=0.4, octave=-1, attack=0.05)
                if kind == "B2":
                    s.play("brass", v_trumpet, T_MEL_B, st0, gain=0.3, position=0.2)
            elif kind == "A2":
                s.play("strings_mel", v_strings_long, T_MEL_A, st0, gain=0.5, attack=0.05)
                s.play("brass", v_brass, TF_COUNTER, st0, gain=0.45, position=-0.2)
            elif kind == "D":
                s.play("lead", v_lead, TF_BRIDGE, st0, gain=0.55, position=0.05)
                s.play("brass", v_brass, TF_BRIDGE, st0, gain=0.4, position=-0.15, octave=-1)
            elif kind == "C":
                s.play("brass", v_brass, T_MEL_C, st0, gain=0.55, octave=-1)
            elif kind == "A3":
                s.play("brass", v_brass, T_MEL_A, st0, gain=0.6, position=-0.1)
                s.play("lead", v_lead, T_MEL_A, st0, gain=0.4, position=0.1)
                s.play("brass", v_brass, T_MEL_A, st0, gain=0.28, position=0.15, octave=-1)
            elif kind == "E":
                s.play("brass", v_brass, TF_CALL, st0, gain=0.5)
                s.play("brass", v_brass, TF_CALL2, st0 + 4, gain=0.5)
                s.play("brass", v_trumpet, TF_CALL2, st0 + 4, gain=0.22, position=0.2)

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
    choir = widen(reverb(hp(tl.group("choir"), 150), seconds=3.2, mix=0.4, seed=46)[:, :tl.n], 1.5)
    perc = reverb(hp(tl.group("perc"), 38), seconds=2.6, mix=0.25, damping=0.8, seed=45)[:, :tl.n]
    return s, (drums * 0.85 + bass * 0.7 + arp * 0.5 + ost * 0.6 + strings * 0.55 + brass * 0.8
               + lead * 0.6 + choir * 0.45 + perc * 0.75 + tl.group("fx"))


# ================================================================ 2 hangar (full)

HF_CH = dict(H_CH, C9=(["E3", "A#3", "D4", "G4"], "C2"))
HF_D = ["Bbmaj9", "Am7", "Gm9", "A7", "Bbmaj9", "C9", "Dm9", "A7"]
HF_PLAN = [("A", H_PROG), ("B", H_PROG), ("A2", H_PROG), ("D", HF_D), ("C", H_PROG[4:]),
           ("B2", H_PROG), ("E", H_PROG), ("C2", H_PROG[4:])]
HF_VIBES2 = [". . . . . . . . F5 - - - A5 - - -", "G5 - - - - - - - . . . . . . . .",
             ". . . . . . . . E5 - - - G5 - - -", "F5 - - - - - - - . . . . . . . .",
             ". . . . . . . . D5 - - - F5 - - -", "E5 - - - - - - - C5 - - - . . . .",
             ". . . . . . . . D5 - - - F5 - - -", "E5 - - - - - - - C#5 - - - . . . ."]
HF_SOLO = ["D5 - - F5 - - A5 - - - G5 - F5 - D5 -", "E5 - - - - - C5 - - - - - A4 - - -",
           "A#4 - - D5 - - F5 - A5 - - - G5 - - -", "E5 - - - C#5 - - - A4 - - - . . . .",
           "F5 - - A5 - - C6 - - - A5 - G5 - F5 -", "E5 - - - - - G5 - - - A#5 - - - - -",
           "A5 - - - - - F5 - - - E5 - D5 - - -", "C#5 - - - - - - - E5 - - - . . . ."]
HF_FRAG = ["A4 - - - - - - - D5 - - - E5 - F5 -", "E5 - - - - - - - - - - - . . . ."]
HF_WALK = [(0, 0, 3), (4, 7, 3), (8, 12, 3), (12, 7, 3)]


def hangar_full():
    s = LoopSong(90, 2, plan_len(HF_PLAN), swing=0.3)
    for bar, name in enumerate(["Dm9", "G13"]):
        ep, root = HF_CH[name]
        s.chord("ep", v_epiano, ep, bar, 0, dur_steps=14, gain=0.55, spread=0.5, vel=0.6)
        s.chord("pad", v_pad, ep, bar, 0, dur_steps=16, gain=0.2, bright=900.0)
        if bar == 1:
            for st in range(8, 16):
                s.hit("drums", shaker(), bar, st, 0.08 + 0.02 * (st - 8), 0.4)
    s.hit("amb", clank(1), 0, 6, 0.25, -0.7)
    s.hit("amb", clank(2), 1, 3, 0.2, 0.6)

    rng = np.random.default_rng(92)
    total = plan_len(HF_PLAN)
    clank_slots = [(int(rng.integers(0, total)), int(rng.integers(0, 16)), int(rng.integers(3, 40)),
                    float(rng.uniform(-0.8, 0.8))) for _ in range(26)]
    for base in s.sections():
        for kind, start, j, name, bar in walk(HF_PLAN, base):
            ep, root = HF_CH[name]
            breakdown = kind in ("C", "C2")
            soloing = kind == "D"
            s.chord("ep", v_epiano, ep, bar, 0, dur_steps=6, gain=0.4 if soloing else 0.5, spread=0.5,
                    vel=0.7 if soloing else 0.8)
            s.chord("ep", v_epiano, ep, bar, 10, dur_steps=5, gain=0.3 if soloing else 0.38,
                    spread=0.5, vel=0.55)
            s.chord("pad", v_pad, ep, bar, 0, dur_steps=16, gain=0.16, bright=900.0)
            if breakdown:
                pattern = [(0, 0, 12), (14, 12, 2)]
            elif kind == "E":
                pattern = HF_WALK
            else:
                pattern = H_BASS
            bass_line(s, "bass", v_sub, root, bar, pattern, 0.42)
            if breakdown:
                for st in (4, 12):
                    s.hit("drums", rim(), bar, st, 0.3, -0.1)
            elif kind == "E" and j < 4:  # half-time variant
                s.hit("drums", kick(110), bar, 0, 0.7)
                s.hit("drums", kick(110), bar, 10, 0.4)
                s.hit("drums", snare(), bar, 8, 0.32, 0.05)
                for st in (4, 12):
                    s.hit("drums", rim(), bar, st, 0.18, -0.1)
                for st in range(0, 16, 2):
                    s.hit("drums", hat(), bar, st, 0.12, 0.3)
            else:
                for st in (0, 7, 10):
                    s.hit("drums", kick(110), bar, st, 0.7 if st == 0 else 0.45)
                for st in (4, 12):
                    s.hit("drums", snare(), bar, st, 0.32, 0.05)
                for st in range(0, 16, 2):
                    s.hit("drums", hat(), bar, st, 0.14 if st % 4 else 0.2, 0.3)
                s.hit("drums", hat(), bar, 15, 0.08, 0.3)
                if kind == "D":  # extra rim clicks under the solo
                    for st in (3, 11):
                        s.hit("drums", rim(), bar, st, 0.12, 0.25)
            for st in range(16):
                s.hit("drums", shaker(), bar, st, 0.1 if st % 2 else 0.06, 0.4)
        for kind, st0 in starts(HF_PLAN, base):
            if kind == "B":
                s.play("lead", v_flute, H_MEL, st0, gain=0.4, position=0.1)
            elif kind == "B2":
                s.play("lead", v_flute, H_MEL, st0, gain=0.4, position=0.1)
                s.play("vibes", v_epiano, H_MEL, st0, gain=0.16, position=-0.25, octave=1, vel=0.45)
            elif kind == "A2":
                s.play("vibes", v_epiano, HF_VIBES2, st0, gain=0.3, position=-0.2, vel=0.5)
            elif kind == "D":
                s.play("solo", v_epiano, HF_SOLO, st0, gain=0.4, position=0.15, vel=0.9)
            elif kind in ("C", "C2"):
                s.play("vibes", v_epiano, H_VIBES, st0, gain=0.3, position=-0.2, vel=0.5)
            elif kind == "E":
                s.play("lead", v_flute, HF_FRAG, st0 + 4, gain=0.35, position=0.1)
        for b, st, seed, p in clank_slots:
            s.hit("amb", clank(seed), base + b, st, 0.18, p)

    tl = s.tl
    t = np.arange(tl.n) / SR
    trem = 1 + 0.25 * np.sin(2 * np.pi * 4.5 * t)
    ep = tl.group("ep") * np.vstack([trem, 2 - trem]) + tl.group("vibes")
    ep = reverb(hp(ep, 120), seconds=1.6, mix=0.22, seed=51)[:, :tl.n]
    pad = reverb(hp(tl.group("pad"), 150), seconds=2.5, mix=0.3, seed=52)[:, :tl.n]
    drums = reverb(hp(tl.group("drums"), 35), seconds=0.9, mix=0.12, seed=53)[:, :tl.n]
    drums = np.vstack([svf(ch, 9000) for ch in drums])
    bass = hp(tl.group("bass"), 35)
    lead = delay(tl.group("lead"), s.step * 6, feedback=0.3, mix=0.22, damp=3000, tail=0)[:, :tl.n]
    lead = reverb(lead, seconds=2.2, mix=0.25, seed=54)[:, :tl.n]
    solo = delay(tl.group("solo"), s.step * 3, feedback=0.25, mix=0.18, damp=3500, tail=0)[:, :tl.n]
    solo = reverb(hp(solo, 150), seconds=1.8, mix=0.22, seed=56)[:, :tl.n]
    amb = reverb(tl.group("amb"), seconds=3.5, mix=0.6, damping=0.8, seed=55)[:, :tl.n]
    return s, (ep * 0.7 + pad * 0.5 + drums * 0.8 + bass * 0.75 + lead * 0.65 + solo * 0.6
               + amb * 0.35)


# ================================================================ 3 afterburner (full)

AB_A = ["Am", "F", "C", "G", "Am", "F", "G", "E"]
AB_B = ["Dm", "Am", "F", "E", "Dm", "Am", "F", "E"]
AB_PLAN = [("A", AB_A), ("B", AB_B), ("BD", ["F", "G", "Am", "Am", "F", "G", "E", "E"]),
           ("UP", ["F", "G", "Am", "Am", "F", "G", "E", "E"]), ("A2", AB_A), ("B2", AB_B),
           ("TB", ["Dm", "Dm", "Am", "Am", "F", "F", "E", "E"]),
           ("C", ["C", "G", "Am", "F", "C", "G", "F", "E"]), ("A3", AB_A),
           ("OUT", ["Am", "F", "C", "G", "Am", "F", "C", "G"])]
AB_INTRO = ["Am", "F", "C", "G", "Am", "F", "C", "G"]
AB_BREAK = ["A4 - - - - - C5 - - - - - E5 - - -", "D5 - - - - - B4 - - - - - G4 - - -",
            "C5 - - - - - - - E5 - - - A5 - - -", "G5 - - - - - - - E5 - - - - - - -",
            "F5 - - - - - E5 - - - - - C5 - - -", "D5 - - - - - - - G5 - - - B4 - - -",
            "G#4 - - - - - - - B4 - - - E5 - - -", "G#5 - - - - - - - - - - - . . . ."]
AB_C = ["E5 - - G5 - - C6 - B5 - G5 - E5 - D5 -", "D5 - - - - - B4 - - - G4 - B4 - D5 -",
        "C5 - - E5 - - A5 - G5 - E5 - C5 - E5 -", "F5 - - - - - - - A5 - - - C6 - - -",
        "E5 - - G5 - - C6 - B5 - G5 - E5 - G5 -", "B5 - - - - - D6 - - - B5 - G5 - - -",
        "A5 - - - C6 - - - A5 - - - F5 - - -", "G#5 - - - - - - - B5 - - - E6 - - -"]
AB_TB_BASS = [(0, 0, 2), (2, 12, 1), (3, 0, 2), (6, 0, 2), (8, 0, 2), (10, 12, 1), (11, 7, 2),
              (14, 12, 2)]


def ab_groove(s, bar, kicks, open_hats=True):
    for beat in range(4):
        s.hit("drums", kick(), bar, beat * 4)
        kicks.append(s.pos(bar, beat * 4))
    for st in (4, 12):
        s.hit("drums", clap(), bar, st, 0.7, -0.05)
    for st in range(16):
        if st % 4 != 2:
            s.hit("drums", hat(), bar, st, 0.12 + 0.08 * (st % 2 == 0), 0.3)
        else:
            s.hit("drums", hat(), bar, st, 0.45, 0.25)
    if open_hats:
        for st in range(2, 16, 4):
            s.hit("drums", hat(True), bar, st, 0.22, -0.3)


def ab_bass(s, bar, root, gain=0.45):
    for st in range(16):
        if st % 4:
            nt = root if st % 4 != 3 else transpose(root, 12)
            s.hit("bass", v_bass(note_freq(nt), round(s.step * 0.8, 4)), bar, st, gain)


def afterburner_full():
    s = LoopSong(140, 8, plan_len(AB_PLAN))
    kicks = []
    for bar, name in enumerate(AB_INTRO):
        pad, root, tones = A_CHORDS[name]
        ext = tones + [transpose(tones[0], 12), transpose(tones[1], 12)]
        cutoff = 500 + 3300 * min(1, bar / 4)
        for st, idx in enumerate(ARP_STEPS):
            s.hit("arp", v_arp(note_freq(ext[idx]), round(s.step * 0.7, 4), round(cutoff, -1)),
                  bar, st, 0.3, 0.35 if st % 2 else -0.35)
        s.chord("pad", v_pad, pad, bar, gain=0.35 if bar < 4 else 0.42,
                bright=1400.0 if bar < 4 else 2000.0)
        if bar >= 1:
            for st in range(2, 16, 4):
                s.hit("drums", hat(), bar, st, 0.45, 0.25)
        if bar >= 4:
            for beat in range(4):
                if bar == 7 and beat >= 2:
                    continue
                s.hit("drums", kick(), bar, beat * 4)
                kicks.append(s.pos(bar, beat * 4))
            ab_bass(s, bar, root, 0.42)
        if bar >= 6:
            for st in (4, 12):
                s.hit("drums", clap(), bar, st, 0.6, -0.05)
    drum_fill(s, 7, kicks)
    s.hit("fx", riser(s.bar * 2), 6, 0, 0.3)

    for base in s.sections():
        for kind, start, j, name, bar in walk(AB_PLAN, base):
            pad, root, tones = A_CHORDS[name]
            ext = tones + [transpose(tones[0], 12), transpose(tones[1], 12)]
            groove = kind in ("A", "B", "A2", "B2", "C", "A3", "OUT")
            fill = j == 7 and kind in ("A", "B", "A2", "B2", "C", "OUT")
            if j == 0 and kind in ("A", "B", "A2", "B2", "C", "A3", "TB", "OUT", "BD"):
                s.hit("drums", crash(), bar, 0, 0.5 if kind != "BD" else 0.35, 0.4)
            if groove:
                if fill:
                    for beat in range(2):
                        s.hit("drums", kick(), bar, beat * 4)
                        kicks.append(s.pos(bar, beat * 4))
                    for st in range(0, 8):
                        if st % 4 != 2:
                            s.hit("drums", hat(), bar, st, 0.15, 0.3)
                    s.hit("drums", clap(), bar, 4, 0.7, -0.05)
                    drum_fill(s, bar, kicks)
                else:
                    ab_groove(s, bar, kicks, open_hats=kind not in ("OUT",))
                ab_bass(s, bar, root)
            elif kind == "UP":
                if j < 4:
                    for st in (0, 8):
                        s.hit("drums", kick(), bar, st, 0.85)
                        kicks.append(s.pos(bar, st))
                else:
                    for beat in range(4):
                        s.hit("drums", kick(), bar, beat * 4, 0.9)
                        kicks.append(s.pos(bar, beat * 4))
                    ab_bass(s, bar, root, 0.4)
                for st in range(2, 16, 4):
                    s.hit("drums", hat(), bar, st, 0.35, 0.25)
                if j >= 4:
                    snare_roll(s, bar, 0.12 + 0.12 * (j - 4), 0.012, eighths=j < 6)
                if j == 4:
                    s.hit("fx", riser(s.bar * 4 - s.step * 2), bar, 0, 0.35)
            elif kind == "TB":  # tracker breakbeat
                for st, g in ((0, 1.0), (2, 0.6), (10, 0.9), (11, 0.7)):
                    s.hit("drums", kick(), bar, st, g)
                    kicks.append(s.pos(bar, st))
                for st, g in ((4, 0.8), (7, 0.25), (9, 0.3), (12, 0.8), (15, 0.3)):
                    s.hit("drums", snare(st in (7, 9, 15)), bar, st, g, 0.05)
                for st in range(0, 16, 2):
                    s.hit("drums", hat(), bar, st, 0.3 if st % 4 == 2 else 0.18, 0.3)
                if j % 4 == 3:
                    for st in (12, 13, 14, 15):
                        s.hit("drums", snare(True), bar, st, 0.35 + 0.1 * (st - 12), 0.05)
                bass_line(s, "bass", v_dbass, transpose(root, 12), bar, AB_TB_BASS, 0.34)
                for st in (0, 6, 10):
                    s.chord("pad", v_pad, pad, bar, st, dur_steps=2, gain=0.35, bright=2600.0)
            # arp (every section except a thin breakdown start)
            if kind == "TB":
                sweep = 600 + 4200 * (0.5 - 0.5 * np.cos(np.pi * (j * 16) / 128))
                for st, idx in enumerate(ARP_STEPS):
                    c = sweep * (1 + 0.35 * np.sin(2 * np.pi * st / 16))
                    s.hit("arp", v_arp(note_freq(transpose(ext[idx], -12)), round(s.step * 0.6, 4),
                                       round(float(c), -2)), bar, st, 0.3, 0.3 if st % 2 else -0.3)
            else:
                if kind == "BD":
                    cutoff, g = 1800.0, 0.2
                elif kind == "UP":
                    cutoff, g = 800.0 + 3400 * j / 7, 0.28
                elif kind in ("B", "B2"):
                    cutoff, g = 4200.0, 0.3
                else:
                    cutoff, g = 3600.0, 0.3
                for st, idx in enumerate(ARP_STEPS):
                    s.hit("arp", v_arp(note_freq(ext[idx]), round(s.step * 0.7, 4), round(cutoff, -1)),
                          bar, st, g, 0.35 if st % 2 else -0.35)
            if kind == "B2":  # counter arpeggio an octave up
                for st, idx in enumerate(reversed(ARP_STEPS)):
                    s.hit("pluck", v_pluck(note_freq(transpose(ext[idx], 12)), round(s.step * 0.5, 4),
                                           3000.0), bar, st, 0.12, -0.5 if st % 2 else 0.5)
            if kind != "TB":
                bright = {"BD": 1600.0, "UP": 1200.0 + 1200 * j / 7}.get(kind, 2200.0)
                s.chord("pad", v_pad, pad, bar, dur_steps=16, gain=0.5 if kind == "BD" else 0.45,
                        bright=bright)
        for kind, st0 in starts(AB_PLAN, base):
            if kind in ("A", "A2", "A3"):
                s.play("lead", v_lead, A_LEAD[:8], st0, gain=0.5)
                if kind == "A2":
                    s.play("lead", v_lead, A_LEAD[:8], st0, gain=0.16, octave=1, position=0.2)
                if kind == "A3":
                    s.play("lead", v_lead, A_LEAD[:8], st0, gain=0.25, octave=-1, position=-0.2)
            elif kind in ("B", "B2"):
                s.play("lead", v_lead, A_LEAD[8:], st0, gain=0.5)
            elif kind == "BD":
                s.play("pluck", v_pluck, AB_BREAK, st0, gain=0.4, cutoff=2400.0)
            elif kind == "UP":
                s.play("lead", v_lead, AB_BREAK[4:], st0 + 4, gain=0.28)
            elif kind == "C":
                s.play("lead", v_lead, AB_C, st0, gain=0.48)

    tl = s.tl
    duck = sidechain(tl.n, kicks)
    drums = hp(tl.group("drums"), 35)
    bass = hp(tl.group("bass"), 40) * duck
    arp = delay(tl.group("arp"), s.step * 3, feedback=0.4, mix=0.35, damp=3000, tail=0)[:, :tl.n]
    pad = reverb(hp(tl.group("pad"), 160) * duck, seconds=2.5, mix=0.35, seed=21)[:, :tl.n]
    pad = widen(pad, 1.8)
    lead = delay(tl.group("lead"), s.step * 3, feedback=0.35, mix=0.3, damp=4500, tail=0)[:, :tl.n]
    lead = widen(reverb(hp(lead, 180), seconds=2.0, mix=0.22, seed=22)[:, :tl.n], 1.4)
    pluck = delay(tl.group("pluck"), s.step * 3, feedback=0.45, mix=0.4, damp=2500, tail=0)[:, :tl.n]
    pluck = reverb(pluck, seconds=2.4, mix=0.3, seed=24)[:, :tl.n]
    mix = (drums * 0.9 + bass * 0.8 + arp * 0.6 + pad * 0.55 + lead * 0.75 + pluck * 0.6
           + tl.group("fx"))
    return s, reverb(mix, seconds=1.0, mix=0.06, seed=23)[:, :tl.n]


# ================================================================ 4 coalition rising (full)

CR_A = ["Dm", "Bb", "C", "Dm", "Dm", "Bb", "Gm", "A"]
CR_B = ["Bb", "C", "F", "Dm", "Gm", "A", "Bb", "A"]
CR_PLAN = [("A", CR_A), ("B", CR_B), ("BT", ["Gm", "Dm", "Bb", "A", "Gm", "Dm", "Bb", "A"]),
           ("LY", ["F", "C", "Dm", "Bb", "Gm", "C", "F", "A"]), ("A2", CR_A), ("B2", CR_B),
           ("MM", CR_A), ("BU", ["Bb", "C", "Dm", "Dm", "Bb", "C", "A", "A"]),
           ("CALL", ["Dm", "Dm", "Bb", "A", "Dm", "Dm", "Bb", "A"])]
CR_BATTLE = ["G4 - - A#4 - - D5 - - - C5 - A#4 - A4 -", "A4 - - - - - F4 - - - - - D4 - - -",
             "D5 - - F5 - - A#5 - - - A5 - G5 - F5 -", "E5 - - - - - - - C#5 - - - A4 - - -",
             "G5 - - - - - F5 - D5 - - - A#4 - D5 -", "F5 - - - - - - - A5 - - - D6 - - -",
             "D6 - - - C6 - A#5 - - - F5 - A#5 - - -", "A5 - - - - - - - E5 - - - C#5 - - -"]
CR_LYRIC = ["A4 - - - - - - - C5 - - - F5 - - -", "E5 - - - - - - - G5 - - - - - - -",
            "F5 - - - - - D5 - - - - - A4 - - -", "A#4 - - - - - - - D5 - - - F5 - - -",
            "G5 - - - - - - - F5 - - - D5 - - -", "E5 - - - - - G5 - - - - - C6 - - -",
            "A5 - - - - - - - F5 - - - C5 - - -", "C#5 - - - - - - - E5 - - - - - - -"]
CR_MOTIF = (human_motif("D4") + ["C5 - - - - - - - E5 - - - G5 - - -",
                                 "F5 - - - - - E5 - D5 - - - - - . ."]
            + human_motif("D4") + ["G5 - - - - - F5 - D5 - - - A#4 - - -",
                                   "A4 - - - - - - - C#5 - - - E5 - - -"])
CR_RISE = ["D4 - - - F4 - - - A4 - - - D5 - - -", "E4 - - - G4 - - - C5 - - - E5 - - -",
           "F4 - - - A4 - - - D5 - - - F5 - - -", "A4 - - - D5 - - - F5 - - - A5 - - -",
           "D5 - - - F5 - - - A#5 - - - D6 - - -", "E5 - - - G5 - - - C6 - - - E6 - - -",
           "E5 - - - - - - - C#6 - - - - - - -", "A5 - - - - - - - - - - - . . . ."]
CR_CALL = TF_CALL


def coalition_full():
    s = LoopSong(135, 4, plan_len(CR_PLAN))
    for bar, name in enumerate(["Dm", "Dm", "Bb", "A"]):
        pad, root, stab = B_CHORDS[name]
        s.chord("strings", v_strings_long, pad, bar, gain=0.25 + 0.08 * bar, spread=0.7)
        s.hit("perc", timpani("D2" if name == "Dm" else "A1"), bar, 0, 0.9)
    s.play("brass", v_brass, ["D4 - - - - - - - A4 - - - - - - -",
                              "A4 - - - - - - - C#5 - - - E5 - - -"], 2, gain=0.45)
    for st in range(8, 16):
        s.hit("perc", timpani("A1", 0.4), 3, st, 0.25 + 0.08 * (st - 8))
    s.hit("fx", riser(s.bar / 2), 3, 8, 0.25)

    def ost_shift(root):
        from synth import note_number
        sh = (note_number(root + "2") - note_number("D2")) % 12
        return sh - 12 if sh > 6 else sh

    for bar, name in enumerate(["Dm", "Dm", "Bb", "A"]):
        sh = ost_shift(B_CHORDS[name][1])
        for st, nm in enumerate(B_BARS):
            vel = (0.5 if st % 3 == 0 else 0.32) * (0.65 if nm.endswith("2") else 1.0)
            s.hit("ost", v_strings_short(note_freq(transpose(nm, sh)), round(s.step * 0.75, 4)),
                  bar, st, vel * 0.8, -0.25)

    for base in s.sections():
        for kind, start, j, name, bar in walk(CR_PLAN, base):
            pad, root, stab = B_CHORDS[name]
            sh = ost_shift(root)
            soft = kind == "LY"
            upper = kind in ("B", "B2", "BT", "CALL", "A2")
            for st, nm in enumerate(B_BARS):
                vel = (0.5 if st % 3 == 0 else 0.32) * (0.65 if nm.endswith("2") else 1.0)
                vel *= 0.55 if soft else 0.7 + 0.04 * j if kind == "BU" else 1.0
                s.hit("ost", v_strings_short(note_freq(transpose(nm, sh)), round(s.step * 0.75, 4)),
                      bar, st, vel, -0.25)
                if upper:
                    s.hit("ost", v_strings_short(note_freq(transpose(nm, sh + 12)),
                                                 round(s.step * 0.75, 4)), bar, st, vel * 0.5, 0.3)
            s.chord("strings", v_strings_long, pad, bar, dur_steps=16, gain=0.45 if soft else 0.55,
                    spread=0.7)
            if j == 0 and kind not in ("LY", "BU"):
                s.hit("perc", crash(), bar, 0, 0.55, 0.3)
                s.hit("perc", timpani("D2" if name == "Dm" else "A#1" if name == "Bb" else "G1"),
                      bar, 0, 0.8)
            if kind == "LY":
                if j % 2 == 0:
                    s.hit("perc", timpani("F2" if name == "F" else "D2", 1.2), bar, 0, 0.45)
                for st in range(0, 16, 2):  # harp-like plucks
                    nt = stab[(st // 2) % 3]
                    s.hit("harp", v_pluck(note_freq(nt), round(s.step * 1.5, 4), 2600.0), bar, st,
                          0.22, 0.4 if st % 4 else -0.4)
            elif kind == "BT":
                for st, g in ((0, 0.95), (3, 0.7), (6, 0.8), (8, 0.9), (11, 0.7), (14, 0.8)):
                    s.hit("perc", taiko(), bar, st, g)
                for st in (4, 12):
                    s.hit("perc", snare(), bar, st, 0.6, 0.1)
                for st in (0, 3, 6, 10):
                    s.chord("brass", v_brass, stab, bar, st, dur_steps=1, gain=0.5, spread=0.4,
                            stab=True)
                if j % 4 == 3:
                    for st, p in ((12, 160), (13, 140), (14, 110), (15, 90)):
                        s.hit("perc", tom(p), bar, st, 0.55, (st - 13.5) / 3)
            elif kind == "BU":
                for st in (0, 4, 8, 12):
                    s.hit("perc", taiko(), bar, st, 0.6 + 0.04 * j)
                if j >= 4:
                    snare_roll(s, bar, 0.15 + 0.1 * (j - 4), 0.015, eighths=j < 6, group="perc")
                    for st in range(0, 16, 2 if j < 6 else 1):
                        s.hit("perc", timpani("A1" if name == "A" else "D2", 0.4), bar, st,
                              0.2 + 0.04 * (j - 4))
                if j == 4:
                    s.hit("fx", riser(s.bar * 4), bar, 0, 0.3)
            else:
                for st in (0, 3, 6, 8, 11, 14):
                    s.hit("perc", taiko(), bar, st, 0.9 if st in (0, 8) else 0.6)
                for st in (4, 12):
                    s.hit("perc", snare(), bar, st, 0.55, 0.1)
                if kind in ("B", "B2", "A2", "MM"):
                    for st in range(0, 16, 2):
                        s.hit("perc", hat(), bar, st, 0.12, 0.35)
                if j == 7 and kind in ("A", "B", "A2", "B2", "MM", "CALL"):
                    for st in range(8, 16):
                        s.hit("perc", snare(True), bar, st, 0.25 + 0.07 * (st - 8), 0.1)
            if kind in ("B", "B2", "A2"):
                for st in (6, 14):
                    s.chord("brass", v_brass, stab, bar, st, dur_steps=1, gain=0.5, spread=0.4,
                            stab=True)
            if kind in ("A2", "B2", "MM", "CALL"):
                s.chord("choir", v_choir, pad[1:], bar, dur_steps=16, gain=0.3)
        for kind, st0 in starts(CR_PLAN, base):
            if kind in ("A", "A2"):
                s.play("brass", v_brass, B_THEME[:8], st0, gain=0.6, position=-0.1)
                s.play("brass", v_brass, B_THEME[:8], st0, gain=0.3, position=0.15, octave=-1)
                if kind == "A2":
                    s.play("lead", v_strings_long, B_THEME[:8], st0, gain=0.3, octave=1, attack=0.05)
            elif kind in ("B", "B2"):
                s.play("lead", v_strings_long, B_THEME[8:], st0, gain=0.55, position=0.1, attack=0.05)
                s.play("brass", v_brass, B_THEME[8:], st0, gain=0.5, position=-0.15, octave=-1)
                if kind == "B2":
                    s.play("brass", v_trumpet, B_THEME[8:], st0, gain=0.3, position=0.2)
            elif kind == "BT":
                s.play("brass", v_trumpet, CR_BATTLE, st0, gain=0.45, position=0.1)
                s.play("brass", v_brass, CR_BATTLE, st0, gain=0.3, position=-0.15, octave=-1)
            elif kind == "LY":
                s.play("lead", v_strings_long, CR_LYRIC, st0, gain=0.45, attack=0.1)
                s.play("wood", v_flute, CR_LYRIC, st0, gain=0.25, position=0.2)
            elif kind == "MM":
                s.play("brass", v_brass, CR_MOTIF, st0, gain=0.6, position=-0.1)
                s.play("brass", v_trumpet, CR_MOTIF[4:], st0 + 4, gain=0.35, position=0.2)
                s.play("lead", v_strings_long, CR_MOTIF[2:4], st0 + 2, gain=0.35, attack=0.05)
            elif kind == "BU":
                s.play("lead", v_strings_long, CR_RISE, st0, gain=0.45, attack=0.05)
            elif kind == "CALL":
                s.play("brass", v_brass, CR_CALL, st0, gain=0.5)
                s.play("brass", v_trumpet, CR_CALL, st0 + 4, gain=0.35, position=0.2)
                s.play("brass", v_brass, CR_CALL, st0 + 4, gain=0.35, octave=-1)

    tl = s.tl
    ost = reverb(hp(tl.group("ost"), 45), seconds=1.8, mix=0.22, seed=31)[:, :tl.n]
    strings = hp(tl.group("strings"), 110) + tl.group("lead")
    strings = widen(reverb(strings, seconds=3.2, mix=0.4, seed=32)[:, :tl.n], 1.5)
    brass = reverb(hp(tl.group("brass"), 90), seconds=2.6, mix=0.3, seed=33)[:, :tl.n]
    perc = reverb(hp(tl.group("perc"), 38), seconds=2.8, mix=0.25, damping=0.8, seed=34)[:, :tl.n]
    choir = widen(reverb(hp(tl.group("choir"), 150), seconds=3.2, mix=0.4, seed=35)[:, :tl.n], 1.5)
    harp = delay(tl.group("harp"), s.step * 3, feedback=0.35, mix=0.3, damp=3000, tail=0)[:, :tl.n]
    harp = reverb(harp, seconds=2.4, mix=0.3, seed=36)[:, :tl.n]
    wood = reverb(tl.group("wood"), seconds=2.4, mix=0.3, seed=37)[:, :tl.n]
    return s, (ost * 0.7 + strings * 0.6 + brass * 0.8 + perc * 0.8 + choir * 0.45 + harp * 0.5
               + wood * 0.6 + tl.group("fx"))


# ================================================================ 5 homefront (full)

HO_CH = dict(EA_CH, Eb=(["D#3", "G3", "A#3", "D#4"], "D#2", 4))
HO_A = ["Cm", "Cm", "Ab", "Ab", "Fm", "Fm", "G", "G"]
HO_B = ["Cm", "Ab", "Fm", "G", "Cm", "Ab", "Bb", "G"]
HO_PLAN = [("A", HO_A), ("B", HO_B), ("A2", HO_A), ("C", ["Ab", "Fm", "Cm", "G"]),
           ("SG", ["Ab", "Bb", "Cm", "Cm", "Ab", "Bb", "G", "G"]),
           ("CA", ["Eb", "Bb", "Cm", "Ab", "Eb", "Bb", "Ab", "G"]),
           ("C2", ["Ab", "Fm", "Cm", "G"]), ("B2", HO_B), ("D", HO_B), ("A3", HO_A),
           ("TA", HO_A)]
HO_COUNTER = ["G5 - - - - - - - - - - - D#5 - - -", "D5 - - - - - - - C5 - - - - - - -",
              "C5 - - - - - - - - - - - D#5 - - -", "G#5 - - - - - - - G5 - - - - - - -",
              "F5 - - - - - - - - - - - G#5 - - -", "G5 - - - - - - - F5 - - - - - - -",
              "D5 - - - - - - - B4 - - - D5 - - -", "G5 - - - - - - - - - - - . . . ."]
HO_SIEGE = ["C4 - - - - - - - - - - - G4 - - -", "C5 - - - - - - - - - - - D5 - - -",
            "D#5 - - - - - - - - - - - - - - -", REST,
            "G#4 - - - - - - - - - - - D#5 - - -", "D5 - - - - - - - - - - - F5 - - -",
            "G5 - - - - - - - - - - - - - - -", "B4 - - - - - - - D5 - - - - - - -"]
HO_HOPE = (human_motif("D#4", major=True)
           + ["A#4 - - - - - - - C5 - - - D#5 - - -", "D#5 - - - - - D5 - C5 - - - - - - -"]
           + human_motif("D#4", major=True)
           + ["F5 - - - - - D#5 - C5 - - - D#5 - F5 -", "G5 - - - - - - - D5 - - - B4 - - -"])


def homefront_full():
    s = LoopSong(147, 4, plan_len(HO_PLAN))
    kicks = []
    s.tl.add("fx", siren(s.tl.n, 0, int(4 * s.bar * SR)), 0, 0, 0.35)
    for bar, name in enumerate(EA_INTRO):
        pad, root, third = HO_CH[name]
        s.chord("choir", v_choir, pad[1:], bar, dur_steps=16, gain=0.3 + 0.08 * bar)
        s.hit("perc", timpani("C2" if name == "Cm" else "G1"), bar, 0, 0.8)
        if bar >= 2:
            for st, nm in enumerate(earth_riff(root, third) * 2):
                s.hit("riff", v_pluck(note_freq(nm), round(s.step * 0.6, 4), 1200.0 + 500 * (bar - 2)),
                      bar, st, 0.3, 0.3 if st % 2 else -0.3)
    s.play("brass", v_brass, human_motif("C4"), 0, gain=0.4)
    snare_roll(s, 3, 0.15)
    s.hit("fx", riser(s.bar), 3, 0, 0.25)

    siren_bars = []
    for base in s.sections():
        for kind, start, j, name, bar in walk(HO_PLAN, base):
            pad, root, third = HO_CH[name]
            half = kind in ("C", "C2")
            siege = kind == "SG"
            if j == 0:
                s.hit("perc", crash(), bar, 0, 0.55, 0.3)
            if half:
                s.hit("drums", kick(), bar, 0, 0.95)
                kicks.append(s.pos(bar, 0))
                s.hit("drums", snare(), bar, 8, 0.7, 0.05)
                for st in (0, 6):
                    s.hit("perc", taiko(), bar, st, 0.7)
            elif siege:
                for st in (0, 10):
                    s.hit("drums", kick(), bar, st, 0.95)
                    kicks.append(s.pos(bar, st))
                s.hit("drums", snare(), bar, 8, 0.65, 0.05)
                for st in (0, 3, 6, 8, 11, 14):
                    s.hit("perc", taiko(), bar, st, 0.85 if st in (0, 8) else 0.6)
                for st in range(0, 16, 2):
                    s.hit("drums", hat(), bar, st, 0.15, 0.3)
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
                if kind in ("A", "A3"):
                    s.hit("perc", taiko(), bar, 0, 0.7)
            if (kind in ("A", "A3") and j % 4 == 0) or (siege and j % 2 == 0):
                s.chord("brass", v_brass, pad, bar, 0, dur_steps=3, gain=0.6, stab=True)
                s.hit("perc", timpani("C2" if root == "C2" else "F2"), bar, 0, 0.7)
            if kind == "TA":
                if j >= 6:
                    snare_roll(s, bar, 0.18 + 0.08 * (j - 6), 0.02)
                if j == 6:
                    s.hit("fx", riser(s.bar * 2), bar, 0, 0.3)
                if j == 4:
                    siren_bars.append(bar)
            riff = earth_riff(root, third)
            for st in range(16):
                low = kind in ("B", "B2", "D", "CA", "SG")
                nm = transpose(riff[st % 8], -12) if low else riff[st % 8]
                if half:
                    s.hit("riff", v_pluck(note_freq(nm), round(s.step * 0.6, 4), 1400.0), bar, st, 0.22,
                          0.3 if st % 2 else -0.3)
                else:
                    g = 1.25 if kind in ("A", "A3") else 0.6 if siege else 1.0
                    s.hit("riff", v_strings_short(note_freq(nm), round(s.step * 0.7, 4)), bar, st,
                          g * (0.34 if st % 4 == 0 else 0.24), -0.2)
            if kind in ("A", "D", "A3", "CA"):
                for st in range(16):
                    s.hit("arp", v_arp(note_freq(riff[st % 8]), round(s.step * 0.6, 4), 3000.0),
                          bar, st, 0.14, 0.35 if st % 2 else -0.35)
            if kind in ("A2", "A3"):
                for st in range(0, 16, 2):
                    s.hit("brass", v_brass(note_freq(transpose(root, 12)), round(s.step * 1.3, 4),
                                           stab=True), bar, st, 0.32 if st % 4 else 0.42)
            if half or siege:
                s.hit("bass", v_bass(note_freq(root), round(s.step * 14, 4)), bar, 0, 0.45)
            else:
                for st in range(0, 16, 2):
                    nm = transpose(root, 12 if st % 8 == 6 else 0)
                    s.hit("bass", v_bass(note_freq(nm), round(s.step * 1.6, 4)), bar, st, 0.45)
            s.chord("choir", v_choir, pad[1:], bar, dur_steps=16,
                    gain=0.45 if (half or siege) else 0.35)
            s.chord("strings", v_strings_long, pad, bar, dur_steps=16, gain=0.35)
        for kind, st0 in starts(HO_PLAN, base):
            if kind == "A":
                s.play("strings_mel", v_strings_long, HO_COUNTER, st0, gain=0.4, attack=0.08)
                s.play("choir", v_choir, HO_COUNTER, st0, gain=0.22, vowel="oo")
            elif kind == "B":
                s.play("brass", v_brass, EA_HORNS, st0, gain=0.6, position=-0.1)
                s.play("brass", v_trumpet, EA_HORNS, st0, gain=0.25, position=0.15, octave=1)
            elif kind == "A2":
                s.play("lead", v_lead, HO_COUNTER, st0, gain=0.4)
            elif kind == "C":
                s.play("lead", v_lead, EA_LAMENT, st0, gain=0.5)
            elif kind == "SG":
                s.play("choir", v_choir, HO_SIEGE, st0, gain=0.55)
                s.play("brass", v_brass, HO_SIEGE, st0, gain=0.35, octave=-1)
            elif kind == "CA":
                s.play("brass", v_trumpet, HO_HOPE, st0, gain=0.5, position=0.1)
                s.play("brass", v_brass, HO_HOPE, st0, gain=0.4, position=-0.15, octave=-1)
            elif kind == "C2":
                s.play("strings_mel", v_strings_long, EA_LAMENT, st0, gain=0.45, attack=0.08)
                s.play("choir", v_choir, EA_LAMENT, st0, gain=0.3, vowel="oo")
            elif kind == "B2":
                s.play("brass", v_brass, EA_HORNS, st0, gain=0.6, position=-0.1)
                s.play("brass", v_trumpet, EA_HORNS, st0, gain=0.35, position=0.15, octave=1)
            elif kind == "D":
                s.play("brass", v_brass, EA_HORNS, st0, gain=0.6, position=-0.1)
                s.play("lead", v_lead, EA_HORNS, st0, gain=0.45, position=0.1, octave=1)
            elif kind == "A3":
                s.play("brass", v_trumpet, HO_COUNTER, st0, gain=0.4, position=0.15)
                s.play("strings_mel", v_strings_long, HO_COUNTER, st0, gain=0.35, attack=0.08)
    for bar in siren_bars:  # siren swell in the turnaround
        i = int(round(s.pos(bar) * SR))
        if i < s.tl.n:
            s.tl.add("fx", siren(s.tl.n - i, 0, int(4 * s.bar * SR)), s.pos(bar), 0, 0.22)

    return s, mixdown(s, {
        "drums": dict(hp=35, gain=0.85), "perc": dict(hp=38, rev=(2.6, 0.25), damp=0.8, gain=0.75,
                                                      seed=201),
        "fx": dict(rev=(3.0, 0.5), seed=202, gain=0.8),
        "riff": dict(hp=150, rev=(1.6, 0.2), seed=203, gain=0.6),
        "arp": dict(dly=(3, 0.3, 0.25, 3000), gain=0.45),
        "bass": dict(hp=40, duck=True, gain=0.7),
        "choir": dict(hp=150, rev=(3.2, 0.4), wid=1.5, seed=204, gain=0.55),
        "strings": dict(hp=110, duck=True, rev=(3.0, 0.35), wid=1.4, seed=205, gain=0.45),
        "strings_mel": dict(hp=120, rev=(3.0, 0.35), wid=1.3, seed=208, gain=0.5),
        "brass": dict(hp=90, rev=(2.4, 0.28), seed=206, gain=0.8),
        "lead": dict(hp=180, dly=(3, 0.3, 0.25, 4500), rev=(2.2, 0.22), wid=1.4, seed=207, gain=0.6),
    }, kicks)


# ================================================================ 6 the choir descends (full)

CD_A = ["Em", "Em", "C", "C", "F", "F", "B", "B"]
CD_PLAN = [("A", CD_A), ("B", ["Em", "Em", "C", "C", "Am", "Am", "B", "B"]), ("A2", CD_A),
           ("C", ["Em", "Em", "F", "F", "C", "C", "B", "B"]),
           ("P2", ["Em", "F", "Em", "F", "C", "B", "C", "B"]), ("CH", CD_A),
           ("B2", ["Em", "Em", "C", "C", "Am", "Am", "B", "B"]),
           ("BR", ["Em", "Em", "F", "F", "C", "C", "B", "B"]), ("A3", CD_A)]
CD_SYNC = [(0, 0, 2), (2, 12, 1), (3, 0, 2), (5, 12, 1), (6, 0, 2), (8, 0, 1), (9, 12, 2),
           (11, 0, 2), (12, 12, 1), (14, 7, 1), (15, 1, 1)]


def choir_full():
    s = LoopSong(150, 4, plan_len(CD_PLAN))
    kicks = []
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

    canon = shifted(CHOIR_MOTIF_E, 8)
    for base in s.sections():
        for kind, start, j, name, bar in walk(CD_PLAN, base):
            pad, root, stab = B_CH[name]
            half = (kind == "C" and j < 4)
            heart = (kind == "BR" and j < 4)
            if j == 0 or (kind in ("C", "BR") and j == 4):
                s.hit("perc", crash(), bar, 0, 0.55, 0.3)
            if heart:
                for st in (0, 3):
                    s.hit("perc", taiko(), bar, st, 0.55 if st == 0 else 0.4)
            elif half:
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
                for st in (range(16) if kind != "A" else range(0, 16, 2)):
                    s.hit("drums", hat(), bar, st, 0.15 if st % 2 else 0.22, 0.3)
                if kind == "CH":
                    for st in (2, 6, 10, 14):
                        s.hit("drums", hat(True), bar, st, 0.2, -0.3)
                for st in (0, 8):
                    s.hit("perc", taiko(), bar, st, 0.6)
                if j % 4 == 3:
                    for st, p in ((12, 160), (13, 140), (14, 110), (15, 90)):
                        s.hit("drums", tom(p), bar, st, 0.5, (st - 13.5) / 3)
            if (kind == "C" and j >= 4) or (kind == "BR" and j >= 4):
                k = j - 4
                for st in (range(0, 16, 2) if k < 2 else range(16)):
                    s.hit("drums", snare(True), bar, st, 0.2 + 0.04 * k + 0.01 * st, 0.05)
                if k == 0:
                    s.hit("fx", riser(s.bar * 4), bar, 0, 0.3)
            # bass
            if half or heart:
                s.hit("bass", v_dbass(note_freq(root), round(s.step * 14, 4)), bar, 0,
                      0.45 if half else 0.3)
            elif kind == "CH":
                bass_line(s, "bass", v_dbass, root, bar, CD_SYNC, 0.45)
            else:
                for st, semis in enumerate(B_BASS):
                    s.hit("bass", v_dbass(note_freq(transpose(root, semis)), round(s.step * 0.8, 4)),
                          bar, st, 0.45)
            if j % 2 == 0:
                s.chord("strings", v_strings_long, pad, bar, dur_steps=32, gain=0.4, spread=0.7)
            motif_bar = kind in ("A", "A2", "A3", "CH") and j % 8 in (0, 4)
            if j % 2 == 0 and not motif_bar and kind != "BR":
                s.chord("choir", v_choir, stab, bar, dur_steps=30, gain=0.35, spread=0.5)
            if kind in ("A", "A2", "A3") and j % 8 in (2, 3, 6, 7):
                for st in (0, 3, 6, 10):
                    s.chord("brass", v_brass, stab, bar, st, dur_steps=1, gain=0.5, spread=0.4,
                            stab=True)
            if kind in ("B", "B2", "A3"):
                for st in range(16):
                    s.hit("ost", v_strings_short(note_freq(transpose(stab[-1], 12)),
                                                 round(s.step * 0.7, 4)), bar, st,
                          0.18 if kind != "A3" else 0.12, 0.3)
            if kind == "P2":  # tritone grind, choir stabs, brass blasts
                for st in range(16):
                    nm = transpose(root, 24 + (0, 6, 7, 6)[st % 4])
                    s.hit("ost", v_strings_short(note_freq(nm), round(s.step * 0.7, 4)), bar, st,
                          0.24 if st % 4 == 0 else 0.17, -0.3)
                for st in (2, 6, 10, 14):
                    s.chord("choir", v_choir, stab, bar, st, dur_steps=2, gain=0.3, spread=0.5)
                s.chord("brass", v_brass, stab, bar, 0, dur_steps=4, gain=0.55, spread=0.4)
        for kind, st0 in starts(CD_PLAN, base):
            if kind in ("A", "A2", "A3"):
                for off in (0, 4):
                    s.play("choir", v_choir, CHOIR_MOTIF_E, st0 + off, gain=0.65 if kind == "A3" else 0.6)
                    s.play("brass", v_brass, CHOIR_MOTIF_E, st0 + off, gain=0.35 if kind == "A3" else 0.3,
                           octave=-1)
                if kind in ("A2", "A3"):
                    s.play("riff", v_riff, B_RIFF, st0, gain=0.25, octave=-2, legato=0.8)
            elif kind == "B":
                s.play("riff", v_riff, B_RIFF, st0, gain=0.4, octave=-1, legato=0.8)
            elif kind == "B2":
                s.play("riff", v_riff, B_RIFF, st0, gain=0.4, octave=-1, legato=0.8)
                s.play("brass", v_brass, B_RIFF, st0, gain=0.22, octave=-2, legato=0.8)
            elif kind == "C":
                s.play("brass", v_brass, B_AUG, st0, gain=0.55)
            elif kind == "P2":
                s.play("brass", v_brass, CHOIR_MOTIF_E, st0 + 4, gain=0.4, octave=-1)
            elif kind == "CH":
                for off in (0, 4):
                    s.play("choir", v_choir, CHOIR_MOTIF_E, st0 + off, gain=0.55)
                    s.play("choir", v_choir, canon, st0 + off, gain=0.4, octave=-1, vowel="oo")
            elif kind == "BR":
                s.play("choir", v_choir, B_AUG, st0, gain=0.55, octave=1)
                s.play("brass", v_brass, B_AUG, st0 + 4, gain=0.5)

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


# ================================================================ 7 briefing: Situation Room

BR_CH = {  # pad voicing, bass root, telemetry tones
    "Dm": (["D3", "F3", "A3"], "D2", ["D5", "F5", "A5", "D6"]),
    "Bb": (["D3", "F3", "A#3"], "A#1", ["D5", "F5", "A#5", "D6"]),
    "Gm": (["D3", "G3", "A#3"], "G1", ["D5", "G5", "A#5", "D6"]),
    "A": (["C#3", "E3", "A3"], "A1", ["C#5", "E5", "A5", "C#6"]),
    "C": (["C3", "E3", "G3"], "C2", ["C5", "E5", "G5", "C6"]),
    "Eb": (["D#3", "G3", "A#3"], "D#2", ["D#5", "G5", "A#5", "D#6"]),
}
BR_PLAN = [("A", ["Dm", "Dm", "Bb", "Bb", "Gm", "Gm", "A", "A"]),
           ("B", ["Dm", "Dm", "Bb", "Bb", "Gm", "Gm", "A", "A"]),
           ("C", ["Bb", "C", "Dm", "Dm", "Bb", "C", "A", "A"]),
           ("D", ["Dm", "Eb", "Dm", "Eb", "Gm", "Gm", "A", "A"]),
           ("E", ["Dm", "Dm", "Bb", "A"])]
BR_FRAG = ["D4 - - - - - A4 - D5 - - - - - - -", REST, REST, REST,
           "A#3 - - - - - F4 - A#4 - - - - - - -", REST, "A3 - - - - - - - C#4 - - - E4 - - -", REST]


def briefing():
    s = LoopSong(100, 2, plan_len(BR_PLAN))
    for bar, name in enumerate(["Dm", "A"]):
        pad, root, _ = BR_CH[name]
        s.chord("pad", v_pad, pad, bar, dur_steps=16, gain=0.3, bright=700.0)
        for st in (0, 4, 8, 12):
            s.hit("drums", rim(), bar, st, 0.08 + 0.02 * bar, 0.2)
    s.hit("ping", sonar(), 0, 0, 0.18)
    rng = np.random.default_rng(301)
    total = plan_len(BR_PLAN)
    blips = [(int(rng.integers(0, total)), int(rng.integers(0, 16)), int(rng.integers(0, 4)),
              float(rng.uniform(-0.8, 0.8))) for _ in range(70)]
    for base in s.sections():
        for kind, start, j, name, bar in walk(BR_PLAN, base):
            pad, root, tones = BR_CH[name]
            s.chord("pad", v_pad, pad, bar, dur_steps=16, gain=0.3, bright=700.0)
            s.chord("strings", v_strings_long, [transpose(n, 12) for n in pad], bar, dur_steps=16,
                    gain=0.32 if kind in ("C", "D") else 0.18, spread=0.7)
            for st in (0, 4, 8, 12):  # clock tick
                s.hit("drums", rim(), bar, st, 0.1, 0.2)
            for st in range(16):
                s.hit("drums", shaker(), bar, st, 0.04 if st % 2 else 0.025, 0.4)
            if kind in ("B", "C", "D"):
                s.hit("drums", kick(100), bar, 0, 0.5)
                s.hit("drums", kick(100), bar, 8, 0.35)
                for st in range(0, 16, 2):
                    s.hit("bass", v_bass(note_freq(root), round(s.step * 1.2, 4)), bar, st,
                          0.24 if st % 4 == 0 else 0.17)
            if kind == "D":
                top = tones[1]
                s.hit("trem", v_tremolo(note_freq(top), round(s.step * 15, 4)), bar, 0, 0.18, 0.3)
                if j >= 4:
                    for st in (0, 8):
                        s.hit("perc", timpani("G1" if name == "Gm" else "A1", 1.2), bar, st, 0.35)
            if j % 4 == 0:
                s.hit("ping", sonar(), bar, 0, 0.15)
        for kind, st0 in starts(BR_PLAN, base):
            if kind == "C":
                s.play("brass", v_brass, BR_FRAG, st0, gain=0.35, position=-0.15)
        for b, st, k, p in blips:
            if b < total:
                bar = base + b
                name = [c for _, cs in BR_PLAN for c in cs][b]
                s.hit("blip", v_pluck(note_freq(BR_CH[name][2][k]), round(s.step * 0.4, 4), 3500.0),
                      bar, st, 0.1, p)
    s.tl.add("fx", radio_static(s.tl.n), 0, 0, 0.035)
    return s, mixdown(s, {
        "pad": dict(hp=80, rev=(3.5, 0.4), wid=1.6, seed=311, gain=0.6),
        "strings": dict(hp=150, rev=(3.5, 0.4), wid=1.5, seed=312, gain=0.45),
        "drums": dict(hp=40, rev=(1.2, 0.15), seed=313, gain=0.7),
        "bass": dict(hp=40, gain=0.6),
        "trem": dict(hp=200, rev=(3.0, 0.35), seed=314, gain=0.5),
        "perc": dict(hp=38, rev=(3.0, 0.3), seed=315, gain=0.7),
        "brass": dict(hp=90, rev=(3.0, 0.35), seed=316, gain=0.7),
        "ping": dict(dly=(6, 0.55, 0.45, 2500), rev=(4.0, 0.5), seed=317, gain=0.5),
        "blip": dict(dly=(3, 0.45, 0.4, 3000), rev=(2.5, 0.3), seed=318, gain=0.5),
        "fx": dict(hp=300, gain=1.0),
    }, [])


# ================================================================ 8 act 2 B: Firestorm

FS_CH = {  # pad voicing, bass root, third
    "Gm": (["G3", "A#3", "D4", "G4"], "G1", 3),
    "Eb": (["G3", "A#3", "D#4", "G4"], "D#2", 4),
    "Bb": (["F3", "A#3", "D4", "F4"], "A#1", 4),
    "F": (["F3", "A3", "C4", "F4"], "F2", 4),
    "Cm": (["G3", "C4", "D#4", "G4"], "C2", 3),
    "D": (["F#3", "A3", "D4", "F#4"], "D2", 4),
}
FS_A = ["Gm", "Gm", "Eb", "Eb", "Cm", "Cm", "D", "D"]
FS_B = ["Eb", "Bb", "F", "Gm", "Eb", "Bb", "Cm", "D"]
FS_PLAN = [("A", FS_A), ("B", FS_B), ("A2", FS_A), ("C", ["Cm", "Cm", "Eb", "Eb", "F", "F", "D", "D"]),
           ("D", FS_A), ("E", FS_B), ("F", ["Cm", "Eb", "Bb", "F", "Cm", "Eb", "D", "D"]),
           ("TR", ["Gm", "Eb", "Cm", "D", "Gm", "Eb", "Cm", "D"])]
FS_MEL_A = ["D5 - - - - - - - - - - - A#4 - - -", "A4 - - - - - - - G4 - - - - - - -",
            "G4 - - - - - - - - - - - A#4 - - -", "D#5 - - - - - - - D5 - - - - - - -",
            "C5 - - - - - - - - - - - D#5 - - -", "G5 - - - - - - - F5 - - - - - - -",
            "F#5 - - - - - - - D5 - - - A4 - - -", "D5 - - - - - - - - - - - . . . ."]
FS_MEL_B = (human_motif("G4")
            + ["F5 - - - - - D#5 - D5 - - - C5 - A4 -", "A#4 - - - - - - - D5 - - - G5 - - -"]
            + human_motif("G4")
            + ["C5 - - - - - D#5 - G5 - - - F5 - D#5 -", "F#5 - - - - - - - A5 - - - D6 - - -"])
FS_MEL_C = ["D#5 - - - - - - - - - - - D5 - C5 -", "C5 - - - - - - - G4 - - - - - - -",
            "G5 - - - - - - - - - - - F5 - D#5 -", "D#5 - - - - - - - A#4 - - - - - - -",
            "A4 - - - - - - - C5 - - - F5 - - -", "F5 - - - - - D#5 - D5 - - - C5 - - -",
            "D5 - - - - - - - F#5 - - - A5 - - -", "A5 - - - - - - - - - - - . . . ."]
FS_REESE = [(0, 0, 5), (6, 12, 2), (8, 0, 3), (11, 7, 2), (14, 10, 2)]


def fs_break(s, bar, kicks, fill=False):
    for st, g in ((0, 1.0), (2, 0.55), (10, 0.9), (11, 0.6)):
        s.hit("drums", kick(), bar, st, g)
        kicks.append(s.pos(bar, st))
    for st, g in ((4, 0.8), (7, 0.22), (9, 0.28), (12, 0.8), (15, 0.25)):
        if fill and st >= 12:
            continue
        s.hit("drums", snare(st in (7, 9, 15)), bar, st, g, 0.05)
    if fill:
        for st in (12, 13, 14, 15):
            s.hit("drums", snare(True), bar, st, 0.4 + 0.12 * (st - 12), 0.05)
    for st in range(0, 16, 2):
        s.hit("drums", hat(), bar, st, 0.3 if st % 4 == 2 else 0.18, 0.3)
    s.hit("drums", hat(True), bar, 14, 0.15, -0.3)


def firestorm():
    s = LoopSong(140, 4, plan_len(FS_PLAN))
    kicks = []
    s.tl.add("fx", radio_static(int(4 * s.bar * SR), 141), 0, 0, 0.08)
    for bar, name in enumerate(["Gm", "Gm", "Eb", "D"]):
        pad, root, third = FS_CH[name]
        s.chord("choir", v_choir, pad[1:], bar, dur_steps=16, gain=0.25 + 0.07 * bar, vowel="oo")
        s.hit("fx", thunder(140 + bar), bar, 0, 0.5 if bar % 2 == 0 else 0.3, (-0.5, 0.5)[bar % 2])
        for st in range(0, 16, 2):
            s.hit("drums", hat(), bar, st, 0.1 + 0.03 * bar, 0.3)
        if bar >= 2:
            for st in range(16):
                nm = transpose(root, 24 + (0, 7, 12, 15 if third == 3 else 16)[st % 4])
                s.hit("riff", v_pluck(note_freq(nm), round(s.step * 0.5, 4), 1000.0 + 600 * (bar - 2)),
                      bar, st, 0.28, 0.3 if st % 2 else -0.3)
    snare_roll(s, 3, 0.12)
    s.hit("fx", riser(s.bar * 2), 2, 0, 0.3)

    for base in s.sections():
        for kind, start, j, name, bar in walk(FS_PLAN, base):
            pad, root, third = FS_CH[name]
            half = kind == "C" and j < 6
            storm = kind == "F"
            if j == 0:
                s.hit("perc", crash(), bar, 0, 0.55, 0.3)
            if half:
                s.hit("drums", kick(), bar, 0, 0.95)
                kicks.append(s.pos(bar, 0))
                s.hit("drums", kick(), bar, 10, 0.6)
                kicks.append(s.pos(bar, 10))
                s.hit("drums", snare(), bar, 8, 0.7, 0.05)
                for st in range(0, 16, 4):
                    s.hit("drums", hat(), bar, st + 2, 0.2, 0.3)
            elif storm:
                for st, p in ((0, 150), (3, 120), (6, 100), (10, 130), (13, 95)):
                    s.hit("drums", tom(p), bar, st, 0.6, (p - 120) / 60)
                s.hit("drums", kick(), bar, 0, 1.0)
                kicks.append(s.pos(bar, 0))
                s.hit("drums", snare(), bar, 8, 0.6, 0.05)
                for st in range(16):
                    s.hit("drums", hat(), bar, st, 0.12, 0.3)
                if j in (0, 4):
                    s.hit("fx", thunder(150 + j), bar, 0, 0.45, -0.4 if j else 0.4)
            else:
                fs_break(s, bar, kicks, fill=(j % 4 == 3 and kind != "TR") or (kind == "TR" and j == 7))
            if kind == "C" and j >= 6:
                for st, g in ((0, 0.9), (4, 0.9), (8, 0.9), (12, 0.9)):
                    s.hit("drums", kick(), bar, st, g)
                    kicks.append(s.pos(bar, st))
                snare_roll(s, bar, 0.18 + 0.12 * (j - 6), 0.02, eighths=j == 6)
                if j == 6:
                    s.hit("fx", riser(s.bar * 2), bar, 0, 0.3)
            # orchestral hits and stab riff
            if (kind in ("A", "A2", "D") and j % 2 == 0) or storm:
                s.chord("brass", v_brass, pad, bar, 0, dur_steps=3, gain=0.6, stab=True)
                s.hit("perc", timpani("G1" if root in ("G1", "D2") else "C2"), bar, 0, 0.7)
            if kind in ("A", "A2", "D", "TR"):
                for st in (3, 6):
                    s.chord("brass", v_brass, pad[1:], bar, st, dur_steps=1, gain=0.42, spread=0.4,
                            stab=True)
                for st, semis in ((10, 12), (12, 19)):
                    s.hit("ost", v_strings_short(note_freq(transpose(root, 12 + semis)),
                                                 round(s.step * 1.5, 4)), bar, st, 0.35, -0.2)
            # 16th pluck figure (humanity's motif shape) in B / E / TR / C
            if kind in ("B", "E", "C", "A2"):
                fig = (0, 7, 12, 14, 12 + third, 14, 12, 7)
                for st in range(16):
                    nm = transpose(root, 24 + fig[st % 8])
                    s.hit("riff", v_pluck(note_freq(nm), round(s.step * 0.5, 4),
                                          1400.0 if kind == "C" else 2400.0),
                          bar, st, 0.18 if kind == "C" else 0.22, 0.3 if st % 2 else -0.3)
            if kind == "TR":  # acid arp, sweeping
                sweep = 700 + 3800 * (0.5 - 0.5 * np.cos(np.pi * j / 7))
                fig = (0, 12, 7, 12, 3 if third == 3 else 4, 12, 7, 15 if third == 3 else 16)
                for st in range(16):
                    c = sweep * (1 + 0.3 * np.sin(2 * np.pi * st / 16))
                    s.hit("arp", v_arp(note_freq(transpose(root, 24 + fig[st % 8])), round(s.step * 0.6, 4),
                                       round(float(c), -2)), bar, st, 0.26, 0.3 if st % 2 else -0.3)
            if storm:
                s.chord("trem", v_tremolo, [transpose(n, 12) for n in pad[1:]], bar, dur_steps=16,
                        gain=0.35)
                for st in (0, 6, 10):
                    s.chord("choir", v_choir, pad[1:], bar, st, dur_steps=3, gain=0.3)
            if kind in ("D", "E"):
                for st in range(0, 16, 2):
                    s.hit("brass", v_brass(note_freq(transpose(root, 12)), round(s.step * 1.3, 4),
                                           stab=True), bar, st, 0.3 if st % 4 else 0.4)
            # bass
            if half:
                s.hit("bass", v_reese(note_freq(transpose(root, 12)), round(s.step * 14, 4)), bar, 0, 0.38)
            elif kind == "TR":
                bass_line(s, "bass", v_dbass, transpose(root, 12), bar, AB_TB_BASS, 0.32)
            else:
                bass_line(s, "bass", v_reese, transpose(root, 12), bar, FS_REESE, 0.38)
            if not storm:
                s.chord("choir", v_choir, pad[1:], bar, dur_steps=16,
                        gain=0.4 if kind in ("C", "D", "E") else 0.28)
            s.chord("strings", v_strings_long, pad, bar, dur_steps=16, gain=0.3)
        for kind, st0 in starts(FS_PLAN, base):
            if kind == "A":
                s.play("strings_mel", v_strings_long, FS_MEL_A, st0, gain=0.4, attack=0.08)
                s.play("choir", v_choir, FS_MEL_A, st0, gain=0.2, vowel="oo")
            elif kind == "B":
                s.play("brass", v_brass, FS_MEL_B, st0, gain=0.6, position=-0.1)
                s.play("brass", v_trumpet, FS_MEL_B, st0, gain=0.25, position=0.15)
            elif kind == "A2":
                s.play("strings_mel", v_strings_long, FS_MEL_A, st0, gain=0.35, octave=1, attack=0.08)
            elif kind == "C":
                s.play("lead", v_lead, FS_MEL_C, st0, gain=0.45)
            elif kind == "D":
                s.play("lead", v_lead, FS_MEL_A, st0, gain=0.45)
                s.play("brass", v_trumpet, FS_MEL_A, st0, gain=0.25, position=0.2, octave=-1)
            elif kind == "E":
                s.play("brass", v_brass, FS_MEL_B, st0, gain=0.6, position=-0.1)
                s.play("lead", v_lead, FS_MEL_B, st0, gain=0.42, position=0.1)
                s.play("brass", v_trumpet, FS_MEL_B, st0, gain=0.28, position=0.2)

    return s, mixdown(s, {
        "drums": dict(hp=35, gain=0.85), "perc": dict(hp=38, rev=(2.6, 0.25), damp=0.8, gain=0.75,
                                                      seed=401),
        "fx": dict(rev=(3.0, 0.4), seed=402, gain=0.8),
        "riff": dict(hp=150, dly=(3, 0.25, 0.2, 3000), rev=(1.6, 0.2), seed=403, gain=0.55),
        "arp": dict(dly=(3, 0.35, 0.3, 3000), gain=0.45),
        "bass": dict(hp=38, duck=True, gain=0.65),
        "choir": dict(hp=150, rev=(3.2, 0.4), wid=1.5, seed=404, gain=0.5),
        "strings": dict(hp=110, duck=True, rev=(3.0, 0.35), wid=1.4, seed=405, gain=0.4),
        "strings_mel": dict(hp=120, rev=(3.0, 0.35), wid=1.3, seed=409, gain=0.5),
        "ost": dict(hp=80, rev=(1.8, 0.25), seed=410, gain=0.6),
        "trem": dict(hp=150, rev=(3.0, 0.4), wid=1.4, seed=411, gain=0.45),
        "brass": dict(hp=90, rev=(2.4, 0.28), seed=406, gain=0.8),
        "lead": dict(hp=180, dly=(3, 0.3, 0.25, 4500), rev=(2.2, 0.22), wid=1.4, seed=407, gain=0.6),
    }, kicks)


# ================================================================ stings (one-shot)

def _sting_mix(s, spec, kicks=()):
    tl = s.tl
    total = np.zeros((2, tl.n))
    for name, o in spec.items():
        x = tl.group(name)
        if not x.any():
            continue
        if o.get("hp"):
            x = hp(x, o["hp"])
        if o.get("rev"):
            sec, mx = o["rev"]
            x = reverb(x, seconds=sec, mix=mx, damping=o.get("damp", 0.5), seed=o.get("seed", 7))[:, :tl.n]
        if o.get("wid"):
            x = widen(x, o["wid"])
        total += x * o.get("gain", 1.0)
    return total


STING_SPEC = {
    "strings": dict(hp=60, rev=(3.0, 0.4), wid=1.5, seed=501, gain=0.6),
    "brass": dict(hp=80, rev=(2.6, 0.32), seed=502, gain=0.85),
    "choir": dict(hp=120, rev=(3.5, 0.45), wid=1.5, seed=503, gain=0.55),
    "perc": dict(hp=35, rev=(2.6, 0.28), damp=0.8, seed=504, gain=0.85),
    "fx": dict(rev=(2.5, 0.3), seed=505, gain=0.9),
    "lead": dict(hp=150, rev=(2.4, 0.3), wid=1.3, seed=506, gain=0.6),
    "low": dict(hp=30, rev=(2.0, 0.2), seed=507, gain=0.7),
}


def klaxon(freq, dur):
    n = ns(dur)
    t = np.arange(n) / SR
    x = pulse(freq, n, 0.5) * 0.6 + pulse(freq * 1.5, n, 0.3) * 0.25
    x = svf(x, 1800, q=1.5, mode="bp") * 2 + svf(x, 3500) * 0.3
    env = np.clip(t / 0.01, 0, 1) * np.clip((dur - t) / 0.03, 0, 1)
    return fade(x * env * (1 + 0.15 * np.sin(2 * np.pi * 30 * t)), 0.002, 0.01)


def miniboss_sting():
    s = Song(120, 2, tail=3.0)
    s.hit("fx", riser(1.0), 0, 0, 0.4)
    for st, g in ((8, 1.0), (11, 0.85), (14, 1.0)):
        s.chord("brass", v_brass, ["A3", "C4", "E4"], 0, st, dur_steps=2 if st < 14 else 14, gain=0.8,
                spread=0.4, stab=st < 14)
        s.hit("perc", timpani("A1", 1.4), 0, st, 0.9 * g)
        s.hit("perc", taiko(), 0, st, 0.9 * g)
        s.hit("low", v_dbass(note_freq("A1"), round(s.step * (2 if st < 14 else 12), 4)), 0, st, 0.3)
    s.hit("perc", crash(), 0, 14, 0.7)
    s.chord("choir", v_choir, ["A4", "C5", "E5", "A#5"], 0, 14, dur_steps=14, gain=0.5)
    s.chord("strings", v_strings_long, ["A2", "E3", "A3", "C4"], 0, 14, dur_steps=14, gain=0.6,
            attack=0.02)
    s.play("lead", v_strings_long, ["E6 - - - - - - - - - - - - - - -"], 1, gain=0.25, attack=0.02)
    return _sting_mix(s, STING_SPEC), 4.6


def boss_warning():
    s = Song(150, 3, tail=0.6)
    for beat in range(11):  # two-tone klaxon on every beat, the last beat silent
        s.hit("fx", klaxon(660.0 if beat % 2 == 0 else 880.0, 0.33), beat // 4, (beat % 4) * 4,
              0.32, -0.2 if beat % 2 else 0.2)
    s.hit("fx", riser(s.bar * 3 - s.step * 4), 0, 0, 0.45)
    s.chord("choir", v_choir, ["E3", "B3", "E4"], 0, 0, dur_steps=44, gain=0.45, vowel="oo")
    s.chord("strings", v_strings_long, ["E2", "B2", "E3"], 0, 0, dur_steps=44, gain=0.5)
    for bar, steps in ((0, (0, 8)), (1, (0, 4, 8, 12)), (2, (0, 2, 4, 6, 8, 10))):
        for st in steps:
            s.hit("perc", taiko(), bar, st, 0.55 + 0.12 * bar)
    for st in range(12):
        s.hit("perc", snare(True), 2, st, 0.2 + 0.05 * st, 0.05)
        s.hit("perc", timpani("E2", 0.35), 2, st, 0.2 + 0.03 * st)
    s.hit("low", v_dbass(note_freq("E1"), round(s.step * 44, 4)), 0, 0, 0.35)
    mix = _sting_mix(s, STING_SPEC)
    n_end = int(round(s.at(3) * SR))
    env = np.ones(mix.shape[1])
    env[n_end - ns(0.08):n_end] = np.linspace(1, 0.15, ns(0.08))
    env[n_end:] = 0.15 * np.exp(-np.arange(mix.shape[1] - n_end) / (0.15 * SR))
    return mix * env, s.at(3) + 0.5


def mission_complete():
    s = Song(126, 3, tail=2.5)
    for bar, chord, tim in ((0, ["D3", "F#3", "A3", "D4"], "D2"), (1, ["D3", "G3", "B3", "D4"], "G1")):
        s.chord("strings", v_strings_long, chord, bar, 0, dur_steps=8, gain=0.55, attack=0.03)
        s.hit("perc", timpani(tim, 1.0), bar, 0, 0.8)
    s.chord("strings", v_strings_long, ["C#3", "E3", "A3", "C#4"], 1, 8, dur_steps=8, gain=0.55,
            attack=0.03)
    s.hit("perc", timpani("A1", 1.0), 1, 8, 0.75)
    for st in range(8, 16):
        s.hit("perc", snare(True), 1, st, 0.25 + 0.07 * (st - 8), 0.05)
    mel = ["D4 - - - - - A4 - D5 - - - - - E5 -", "F#5 - - - - - - - E5 - - - D5 - E5 -",
           "F#5 - - - - - - - - - - - - - - -"]
    s.play("brass", v_brass, mel, 0, gain=0.6)
    s.play("brass", v_trumpet, mel, 0, gain=0.35, position=0.2)
    s.play("brass", v_brass, mel, 0, gain=0.3, octave=-1, position=-0.2)
    s.chord("strings", v_strings_long, ["D3", "A3", "D4", "F#4", "A4", "D5"], 2, 0, dur_steps=14,
            gain=0.75, attack=0.02)
    s.chord("choir", v_choir, ["D4", "F#4", "A4"], 2, 0, dur_steps=14, gain=0.45)
    s.chord("brass", v_brass, ["D3", "A3", "D4"], 2, 0, dur_steps=12, gain=0.6)
    s.hit("perc", timpani("D2", 2.5), 2, 0, 1.0)
    s.hit("perc", crash(), 2, 0, 0.7)
    s.hit("perc", taiko(), 2, 0, 0.9)
    s.hit("low", v_strings_short(note_freq("D2"), round(s.step * 12, 4)), 2, 0, 0.6)
    return _sting_mix(s, STING_SPEC), s.at(2) + 2.6


def act_complete():
    s = Song(108, 7, tail=3.0)
    chords = [("D", ["D3", "F#3", "A3", "D4"], "D2"), ("Bb", ["D3", "F3", "A#3", "D4"], "A#1"),
              ("G", ["D3", "G3", "B3", "D4"], "G1"), ("A", ["C#3", "E3", "A3", "C#4"], "A1"),
              ("Bb", ["D3", "F3", "A#3", "D4"], "A#1"), ("C", ["E3", "G3", "C4", "E4"], "C2"),
              ("D", ["D3", "A3", "D4", "F#4", "A4", "D5"], "D2")]
    for bar, (_, pad, tim) in enumerate(chords):
        s.chord("strings", v_strings_long, pad, bar, 0, dur_steps=16 if bar < 6 else 14,
                gain=0.45 + 0.05 * bar, attack=0.08 if bar < 6 else 0.02)
        if bar % 2 == 0:
            s.hit("perc", timpani(tim, 1.8), bar, 0, 0.65 + 0.04 * bar)
        if bar >= 4:
            s.chord("choir", v_choir, pad[1:4], bar, 0, dur_steps=16 if bar < 6 else 14, gain=0.4)
        if bar in (4, 5):
            for st in (0, 6, 10):
                s.hit("perc", taiko(), bar, st, 0.7)
    for st in range(16):
        s.hit("perc", snare(True), 5, st, 0.15 + 0.04 * st, 0.05)
    s.hit("perc", crash(), 4, 0, 0.6)
    s.hit("perc", crash(), 6, 0, 0.75)
    s.hit("perc", taiko(), 6, 0, 1.0)
    s.hit("fx", riser(s.bar), 5, 0, 0.3)
    mel = ["D4 - - - - - A4 - D5 - - - - - E5 -", "F5 - - - - - - - - - - - D5 - - -",
           "D5 - - - - - B4 - G4 - - - B4 - D5 -", "E5 - - - - - - - C#5 - - - A4 - - -",
           "A#4 - - - - - F5 - A#5 - - - - - C6 -", "D6 - - - - - - - E6 - - - - - - -",
           "D6 - - - - - - - - - - - - - - -"]
    s.play("brass", v_brass, mel[:4], 0, gain=0.6)
    s.play("brass", v_brass, mel[:4], 0, gain=0.28, octave=-1, position=-0.2)
    s.play("brass", v_trumpet, mel[4:], 4, gain=0.5, position=0.1)
    s.play("brass", v_brass, mel[4:], 4, gain=0.45, octave=-1, position=-0.15)
    s.play("lead", v_strings_long, mel[4:], 4, gain=0.35, octave=-1, attack=0.05)
    for st, idx in enumerate([0, 1, 2, 3, 2, 1, 0, 1] * 8):  # arp shimmer in the climax
        tones = ["D5", "F#5", "A5", "D6"]
        bar = 4 + st // 16
        if bar < 6:
            name = chords[bar][0]
            tones = {"Bb": ["D5", "F5", "A#5", "D6"], "C": ["E5", "G5", "C6", "E6"]}[name]
            s.hit("lead", v_arp(note_freq(tones[idx]), round(s.step * 0.6, 4), 3200.0), bar, st % 16,
                  0.12, 0.35 if st % 2 else -0.35)
    s.hit("low", v_strings_short(note_freq("D2"), round(s.step * 12, 4)), 6, 0, 0.6)
    return _sting_mix(s, STING_SPEC), s.at(6) + 3.0


def mission_failed():
    s = Song(80, 2, tail=3.0)
    s.play("brass", v_brass, ["D4 - - - C4 - - - A#3 - - - A3 - - -"], 0, gain=0.55)
    s.play("brass", v_brass, ["D3 - - - C3 - - - A#2 - - - A2 - - -"], 0, gain=0.45, position=-0.2)
    s.chord("strings", v_strings_long, ["D3", "F3", "A3"], 0, 0, dur_steps=16, gain=0.4)
    s.chord("strings", v_strings_long, ["D2", "A2", "D3", "F3"], 1, 0, dur_steps=12, gain=0.65,
            attack=0.02)
    s.chord("choir", v_choir, ["D4", "F4", "A4"], 1, 0, dur_steps=10, gain=0.35, vowel="oo")
    s.hit("perc", timpani("D2", 3.0), 1, 0, 1.0)
    s.hit("perc", taiko(), 1, 0, 0.8)
    s.hit("low", v_dbass(note_freq("D1"), round(s.step * 12, 4)), 1, 0, 0.4)
    mix = _sting_mix(s, STING_SPEC)
    cut = ramp(mix.shape[1], 7000.0, 350.0)  # power-down sweep
    return np.vstack([svf(ch, cut, q=0.9) for ch in mix]), s.at(1) + 2.8


def game_over():
    s = Song(72, 6, tail=4.0)
    chords = [(["D3", "F3", "A3", "D4"], "D2"), (["D3", "F3", "A#3", "D4"], "A#1"),
              (["D3", "G3", "A#3", "D4"], "G1"), (["C#3", "E3", "A3", "C#4"], "A1"),
              (["D3", "F3", "A3", "D4"], "D2"), (["D2", "A2", "D3", "F3"], "D2")]
    for bar, (pad, tim) in enumerate(chords):
        s.chord("strings", v_strings_long, pad, bar, 0, dur_steps=16, gain=0.4, attack=0.4)
        s.chord("choir", v_choir, pad[1:], bar, 0, dur_steps=16, gain=0.22, vowel="oo")
        if bar in (0, 4, 5):
            s.hit("perc", timpani(tim, 2.5), bar, 0, 0.5)
    mel = ["D4 - - - - - A4 - D5 - - - - - E5 -", "F5 - - - - - - - - - - - E5 - D5 -",
           "D5 - - - - - - - A#4 - - - G4 - - -", "A4 - - - - - - - C#5 - - - E5 - - -",
           "D5 - - - - - - - A4 - - - F4 - - -", "D4 - - - - - - - - - - - - - - -"]
    s.play("brass", v_brass, mel, 0, gain=0.45)
    s.play("lead", v_flute, mel[4:], 4, gain=0.25, octave=1)
    s.hit("low", v_strings_short(note_freq("D2"), round(s.step * 14, 4)), 5, 0, 0.4)
    return _sting_mix(s, STING_SPEC), s.at(5) + 4.0


# ================================================================ main

LOOPS = {
    "briefing": ("briefing-theme-r08-a", briefing),
    "act2b": ("act2-b-theme-r08-a", firestorm),
    "title": ("title-theme-full-r08-a", title_full),
    "hangar": ("hangar-theme-full-r08-a", hangar_full),
    "afterburner": ("afterburner-full-r08-a", afterburner_full),
    "coalition": ("coalition-rising-full-r08-a", coalition_full),
    "homefront": ("homefront-full-r08-a", homefront_full),
    "choir": ("choir-descends-full-r08-a", choir_full),
}
STINGS = {
    "miniboss": ("miniboss-sting-r08-a", miniboss_sting),
    "warning": ("boss-warning-r08-a", boss_warning),
    "complete": ("mission-complete-r08-a", mission_complete),
    "actcomplete": ("act-complete-r08-a", act_complete),
    "failed": ("mission-failed-r08-a", mission_failed),
    "gameover": ("game-over-r08-a", game_over),
}


def validate_patterns():
    for name, val in list(globals().items()):
        if isinstance(val, list) and val and all(isinstance(b, str) and len(b.split()) == 16
                                                 for b in val):
            parse_track(val)


def render_loop(key, out):
    name, fn = LOOPS[key]
    s, mix = fn()
    mix = mix - mix.mean(axis=1, keepdims=True)
    audio, loop_start, loop_len = s.assemble(mix)
    audio = add_tail(audio, loop_start, 2, 16 * s.step_n)
    audio = master(audio, -14.0, -2.0)
    audio[:, :ns(0.005)] *= np.linspace(0, 1, ns(0.005))
    path = write_ogg(out / f"{name}.ogg", audio, tags={"LOOPSTART": loop_start, "LOOPLENGTH": loop_len})
    dec = decode(path)
    print(f"{path.name}: {audio.shape[1] / SR:.1f}s  loop {loop_start / SR:.3f}s + {loop_len / SR:.3f}s"
          f"  (samples {loop_start}+{loop_len})  seam {seam_ratio(dec, loop_start, loop_len):.2f}"
          f"  peak {db(np.abs(dec).max()):.1f} dBFS", flush=True)


def render_sting(key, out):
    name, fn = STINGS[key]
    mix, length = fn()
    mix = mix[:, :int(length * SR)]
    mix = mix - mix.mean(axis=1, keepdims=True)
    mix = master(mix, -14.0, -2.0)
    mix = fade(mix, 0.003, min(0.6, length * 0.15))
    path = write_ogg(out / f"{name}.ogg", mix)
    dec = decode(path)
    print(f"{path.name}: {mix.shape[1] / SR:.2f}s  peak {db(np.abs(dec).max()):.1f} dBFS", flush=True)


def main(argv):
    out = OUT
    if "--out" in argv:
        i = argv.index("--out")
        out = Path(argv[i + 1])
        argv = argv[:i] + argv[i + 2:]
    validate_patterns()
    for key in argv or list(STINGS) + list(LOOPS):
        if key in LOOPS:
            render_loop(key, out)
        elif key in STINGS:
            render_sting(key, out)
        else:
            raise SystemExit(f"unknown piece {key!r}")


if __name__ == "__main__":
    main(sys.argv[1:])
