#!/usr/bin/env python3
"""Concept round 36 (M5 part H, Level 14 "Siege Spire"): the root field's and the Siege Spire's sounds
and the canopy's (H8 = a, pending the user's review): a/b for the root burst, the tear-free roar and
the canopy withering; one proposal each for the root growth and the canopy hum (a loop). Recorded
layers (Freesound originals, every one already in CREDITS.md for earlier rounds, so no new rows) and
synthesized layers mixed.

Outputs (design/audio/sfx/concept/, OGG Vorbis q6, 44.1 kHz stereo; each file starts at its event):

  root-burst-r36-a.ogg       a "stone gives way": the paving under stress for the 1 s telegraph (a
                             rumble swelling from the ground, "Cracking Earthquake (cracking soil,
                             cracking stone)", uagadugu, CC0, with synthesized cracks running faster
                             and a slowed crack, "cracking.wav", smidoid, CC-BY 4.0), then at 1.0 s the
                             root breaking out: a heavy masonry crunch ("building_collapse02_close.wav",
                             onteca, CC-BY 3.0), a sub thump and stones raining down ("Bricks/Stones/
                             Rocks/Gravel Falling", iwanPlays, CC0); 2.1 s
  root-burst-r36-b.ogg       b "the creep surges": wet and organic, little stone: a slow slurry surge
                             swelling up out of the gutters ("Squelch.mp3", LucasDuff, CC0, slowed, over
                             a synthesized slosh) and a root creaking under strain (a synthesized
                             stick-slip creak rising in pitch, "door wood old slow creaks ... ship hull
                             list2", kyles, CC0, slowed), then at 1.0 s the root tearing up through the
                             creep ("Tearing Flesh", dereklieu, CC-BY 3.0, slowed; "rip_tear FLESH!!!!",
                             aust_paul, CC0) over a softer thump; 2.0 s
  root-growth-r36-a.ogg      the gun growths growing out of the root over 1.5 s (one proposal): a
                             stretching creak ("door wood old slow creaks ...", kyles, CC0, slowed and
                             low-passed) over a synthesized swelling (a low tone gliding up and a
                             filtered breath of noise opening), ending at 1.45 s in a short dull knock
                             and a dry click, the growth set (armed); no bubbles, no pops; 1.75 s
  tear-free-roar-r36-a.ogg   a "tectonic rip": the ground tearing open ("Cracking Earthquake ...",
                             uagadugu, CC0, slowed; "building_collapse02_close.wav", onteca, CC-BY 3.0,
                             slowed), roots snapping ("cracking.wav", smidoid, CC-BY 4.0, and
                             "ICEBrk_Break04", InMotionAudio, CC0, both slowed to heavy snaps), the
                             shockwave at 0.35 s ("Big Boom", unfa, CC0, slowed, and a sub thump), a deep
                             creature roar under it ("Deep Roar Echo 2", noahpardo, CC0, slowed), the
                             synthesized rumble of the mass lifting, debris raining down ("Bricks/Stones/
                             Rocks/Gravel Falling", iwanPlays, CC0); 3.4 s
  tear-free-roar-r36-b.ogg   b "the creature bellows": a huge roar rising in pitch with the lift ("Sea
                             Creature Roar", Bikkit99, CC0, its speed gliding 0.7 -> 1.0, so it starts
                             a fifth lower and rises), a snarling roar slowed under it ("Dragon: Snarl,
                             Roar + Attack", Breviceps, CC0, its roar at 0.6 x), the roots tearing wet
                             ("Tearing Flesh", dereklieu, CC-BY 3.0, and "rip_tear FLESH!!!!",
                             aust_paul, CC0, slowed) and a synthesized air shockwave (a low noise blast
                             and a sub thump; no stone); 3.4 s
  canopy-withering-r36-a.ogg a "dry shrivel": a synthesized dry crackle spreading over the city like
                             leaves and paper drying (dense at 1-3 s, thinning out), the membrane tearing
                             slowly far above ("rip_tear FLESH!!!!", aust_paul, CC0, at half speed, and
                             "Tearing Flesh", dereklieu, CC-BY 3.0, at 0.45 x, low-passed), settling into
                             a soft wind ("Wind__Artic__Cold.wav", cobratronik, CC0, low-passed); 7.0 s
  canopy-withering-r36-b.ogg b "the dying exhale": a deep groan descending ("Simple Mutate (Monster)",
                             Division4884, CC0, its speed gliding 0.62 -> 0.4, low-passed), a vast
                             synthesized sigh (breath noise through two formants falling) and a sub tone
                             sinking from 62 to 31 Hz, fading into the same soft wind; 7.5 s
  canopy-hum-r36-a.ogg       the canopy's hum (one proposal), a seamless 8.0 s LOOP, synthesized: two
                             detuned harmonic voices on 46 Hz (46.00 / 46.25 Hz, beating slowly, their
                             harmonics peaking at 140-280 Hz so laptop speakers carry it), a throb six
                             times a loop (0.75 Hz), a 4 s breath swelling the upper harmonics and a
                             band of organic noise (90-500 Hz); every partial and modulator completes a
                             whole number of cycles in the loop and the noise is made circularly (an
                             inverse FFT), so the end joins the start with no crossfade and no click

Levels (sfx_r24.py's `level`: the loudest 100 ms of the 200 Hz-5 kHz band, limited at a ceiling; set
against the existing set in assets/sfx/, measured the same way): root burst -17 dB (a telegraph the
player must notice: the edge warning -16.2, the Kraken's slam -15.4, the collapse -15.7); root growth
-20 (under the burst, at the ship sinking's -20 and the carrier roar's -19); tear-free roar -14 (a big
boss moment: the large explosion -11.2, the huge explosion r04 -12.5, the Kraken's slam -15.4, its death
-17.1; a stays at -15.0, dense and held by the ceiling, but at -11.5 LUFS it is as loud as the large
explosion; b reaches -14.3); canopy withering -19 (a release under the radio: the carrier roar -19.0,
the ship sinking -20.0; not quieter, the user found quiet layers too quiet). The hum is levelled as the
ambience loops are (import_sfx.py's `band_rms`, the whole loop's 200 Hz-5 kHz RMS): the megacity
ambience (ambience-city-r08-a) and every other loop sit at -36 dB band RMS (the city's loudest 100 ms
-27.8); the hum is set 3 dB under, at -39 dB band RMS (its loudest 100 ms -33.7: steady, no siren
peaks; peak -20 dBFS, so no limiter touches the seam), under the city bed without vanishing on small
speakers. No bubble or popping layers (the user disliked them in round 33).

Usage: python3 tools/concept/audio/sfx_r36.py [subject-or-file ...]   e.g. root-burst canopy-hum-r36-a.
Deterministic. Needs the cached Freesound originals (tools/concept/audio/freesound_fetch.py
--download); without one a layer falls back to the public HQ preview (printed).
"""
import os
import sys
import tempfile
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
from import_sfx import band_rms_db  # noqa: E402
from sfx_r24 import level  # noqa: E402
from sfx_r25 import fs, source  # noqa: E402
from sfx_r32 import loudest_db, recorded, report, thump  # noqa: E402
from synth import SR, db, decode, noise, pan, ramp, saw, sine, svf, undb, write_ogg  # noqa: E402

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "design" / "audio" / "sfx" / "concept"
SCRIPT = "tools/concept/audio/sfx_r36.py"
CC0 = "CC0 1.0"
BY3 = "CC-BY 3.0"
BY4 = "CC-BY 4.0"

EARTHQUAKE = dict(**fs("uagadugu", 222521, 222, 255863),
                  title="Cracking Earthquake (cracking soil, cracking stone)", author="uagadugu", licence=CC0)
CRACKING = dict(**fs("smidoid", 49139, 49, 485393), title="cracking.wav", author="smidoid", licence=BY4)
COLLAPSE = dict(**fs("onteca", 197772, 197, 1011133), title="building_collapse02_close.wav", author="onteca",
                licence=BY3)
GRAVEL = dict(**fs("iwanPlays", 567249, 567, 7108319), title="Bricks/Stones/Rocks/Gravel Falling",
              author="iwanPlays", licence=CC0)
ICE = dict(**fs("InMotionAudio", 719973, 719, 14083205), title="ICEBrk_Break04_InMotionAudio_FREESampleSunday",
           author="InMotionAudio", licence=CC0)
BOOM = dict(**fs("unfa", 189779, 189, 1038806), title="Big Boom", author="unfa", licence=CC0)
DEEP_ROAR = dict(**fs("noahpardo", 345735, 345, 6246023), title="Deep Roar Echo 2.wav", author="noahpardo",
                 licence=CC0)
CREAKS = dict(**fs("kyles", 455757, 455, 612689),
              title="door wood old slow creaks open close or ship hull list2.flac", author="kyles", licence=CC0)
SQUELCH = dict(**fs("LucasDuff", 516643, 516, 8272463), title="Squelch.mp3", author="LucasDuff", licence=CC0)
TEARING = dict(**fs("dereklieu", 241822, 241, 4405300), title="Tearing Flesh", author="dereklieu", licence=BY3)
RIP = dict(**fs("aust_paul", 30928, 30, 15696), title="rip_tear FLESH!!!!.wav", author="aust_paul", licence=CC0)
SEA_ROAR = dict(**fs("Bikkit99", 837799, 837, 16586370), title="Sea Creature Roar", author="Bikkit99",
                licence=CC0)
DRAGON = dict(**fs("Breviceps", 466830, 466, 9159316), title="Dragon: Snarl, Roar + Attack", author="Breviceps",
              licence=CC0)
MUTATE = dict(**fs("Division4884", 342336, 342, 6138200), title="Simple Mutate (Monster)",
              author="Division4884", licence=CC0)
WIND = dict(**fs("cobratronik", 117136, 117, 732072), title="Wind__Artic__Cold.wav", author="cobratronik",
            licence=CC0)

LOOP = 8.0            # the canopy hum's loop length (s); every periodic part completes whole cycles in it

# subject: level (dB, the loudest 100 ms of the 200 Hz-5 kHz band) or, for a loop, `band_rms` (the whole
# loop's 200 Hz-5 kHz RMS, as import_sfx.py levels the ambience loops), ceiling (dBFS), and per variant
# its layers (as sfx_r35.py: a recorded layer has a source and the cut, optional speed, `glide` (a speed
# gliding from the first to the second value over the layer), `approach` (dB, rising to full) and pan;
# a synthesized layer `synth` and its parameters; each enters at `at`, the others `gain` dB from the
# first).
SOUNDS = {
    "root-burst": dict(level=-17.0, ceiling=-2.0, variants={
        "a": [dict(**EARTHQUAKE, cut=dict(offset=1.3, length=1.25, fade=0.3, fadein=0.08, highpass=50), at=0.0,
                   approach=-14.0),
              dict(synth="cracks", at=0.05, gain=-3.0, length=1.0, count=26, seed=3601),
              dict(**CRACKING, cut=dict(offset=0.0, length=0.6, fade=0.3, highpass=200), at=0.62, gain=-6.0,
                   speed=0.75, pan=-0.25),
              dict(**COLLAPSE, cut=dict(offset=1.0, length=1.1, fade=0.6, fadein=0.01, highpass=40), at=0.8,
                   gain=4.0),
              dict(synth="thump", at=1.0, gain=-1.5, length=0.6, start=75.0, end=32.0, tau=0.18),
              dict(**GRAVEL, cut=dict(offset=0.0, length=0.95, fade=0.55, fadein=0.02, highpass=150), at=1.12,
                   gain=-3.0, pan=0.2)],
        "b": [dict(synth="slosh", at=0.0, length=1.3, rise=1.0, seed=3602),
              dict(**SQUELCH, cut=dict(offset=0.3, length=0.95, fade=0.25, fadein=0.25, highpass=90), at=0.15,
                   gain=0.0, speed=0.8, approach=-8.0),
              dict(synth="creak", at=0.1, gain=-2.0, length=1.0, rate=(45.0, 140.0), res=(420.0, 780.0),
                   seed=3603),
              dict(**CREAKS, cut=dict(offset=1.55, length=0.75, fade=0.2, fadein=0.08, highpass=120), at=0.35,
                   gain=-4.0, speed=0.75, pan=0.2),
              dict(**TEARING, cut=dict(offset=0.0, length=0.74, fade=0.4, highpass=60), at=0.98, gain=5.0,
                   speed=0.75),
              dict(**RIP, cut=dict(offset=0.05, length=0.7, fade=0.35, fadein=0.03, highpass=300), at=1.05,
                   gain=-3.0, speed=0.85, pan=-0.2),
              dict(synth="thump", at=1.0, gain=-2.0, length=0.45, start=90.0, end=40.0, tau=0.12)]}),
    "root-growth": dict(level=-20.0, ceiling=-4.0, variants={
        "a": [dict(**CREAKS, cut=dict(offset=1.5, length=1.15, fade=0.25, fadein=0.25, highpass=90,
                                      lowpass=5000), at=0.0, speed=0.82, approach=-6.0),
              dict(synth="swell", at=0.0, gain=-1.0, length=1.5, seed=3611),
              dict(synth="creak", at=0.25, gain=-5.0, length=1.15, rate=(40.0, 110.0), res=(300.0, 560.0),
                   seed=3612),
              dict(synth="knock", at=1.45, gain=2.0, length=0.3, seed=3613)]}),
    "tear-free-roar": dict(level=-14.0, ceiling=-1.5, variants={
        "a": [dict(**EARTHQUAKE, cut=dict(offset=1.0, length=2.9, fade=1.2, fadein=0.12, highpass=90), at=0.0,
                   speed=0.85),
              dict(**COLLAPSE, cut=dict(offset=0.0, length=2.55, fade=0.9, fadein=0.05, highpass=45), at=0.05,
                   gain=0.0, speed=0.8, pan=-0.2),
              dict(**CRACKING, cut=dict(offset=0.0, length=0.75, fade=0.3, highpass=150), at=0.1, gain=-3.0,
                   speed=0.6, pan=0.3),
              dict(**ICE, cut=dict(offset=0.0, length=0.6, fade=0.3, highpass=150), at=0.55, gain=-5.0,
                   speed=0.55, pan=-0.35),
              dict(**BOOM, cut=dict(offset=0.0, length=2.6, fade=1.5, highpass=60), at=0.35, gain=-1.0, speed=0.85),
              dict(synth="thump", at=0.35, gain=-3.0, length=1.0, start=70.0, end=28.0, tau=0.4),
              dict(**DEEP_ROAR, cut=dict(offset=0.0, length=2.4, fade=1.0, fadein=0.15, highpass=70), at=0.3,
                   gain=-3.0, speed=0.78),
              dict(synth="lift", at=0.2, gain=-9.0, length=3.2, seed=3621),
              dict(**GRAVEL, cut=dict(offset=0.3, length=1.6, fade=0.9, fadein=0.05, highpass=150), at=1.5,
                   gain=-7.0, pan=0.3)],
        "b": [dict(**SEA_ROAR, cut=dict(offset=2.3, length=2.75, fade=0.8, fadein=0.2, highpass=40), at=0.12,
                   glide=(0.7, 1.0)),
              dict(**DRAGON, cut=dict(offset=1.45, length=1.6, fade=0.7, fadein=0.08, highpass=50), at=0.3,
                   gain=-5.0, speed=0.6, pan=0.15),
              dict(**TEARING, cut=dict(offset=0.0, length=0.74, fade=0.4, highpass=60), at=0.0, gain=0.0,
                   speed=0.62, pan=-0.2),
              dict(**RIP, cut=dict(offset=0.0, length=1.2, fade=0.5, fadein=0.02, highpass=250), at=0.2,
                   gain=-5.0, speed=0.7, pan=0.25),
              dict(**TEARING, cut=dict(offset=0.0, length=0.74, fade=0.4, highpass=60), at=0.75, gain=-5.0,
                   speed=0.5, pan=0.3),
              dict(synth="shockwave", at=0.3, gain=-1.0, length=2.2, seed=3622),
              dict(synth="thump", at=0.3, gain=-1.0, length=0.9, start=65.0, end=30.0, tau=0.35)]}),
    "canopy-withering": dict(level=-19.0, ceiling=-2.0, variants={
        "a": [dict(synth="crackle", at=0.0, length=6.0, seed=3631),
              dict(**RIP, cut=dict(offset=0.0, length=1.3, fade=0.6, fadein=0.15, highpass=200, lowpass=6000),
                   at=0.7, gain=-3.0, speed=0.5, pan=-0.3),
              dict(**TEARING, cut=dict(offset=0.0, length=0.74, fade=0.45, fadein=0.03, highpass=120,
                                       lowpass=3500), at=2.1, gain=-6.0, speed=0.45, pan=0.35),
              dict(**WIND, cut=dict(offset=20.0, length=5.6, fade=2.6, fadein=2.2, highpass=60, lowpass=2500),
                   at=1.4, gain=-9.0)],
        "b": [dict(**MUTATE, cut=dict(offset=0.2, length=2.6, fade=1.4, fadein=0.3, highpass=40, lowpass=2500),
                   at=0.0, glide=(0.62, 0.4)),
              dict(synth="sigh", at=0.1, gain=-2.0, length=6.5, seed=3632),
              dict(synth="groan", at=0.0, gain=-4.0, length=6.5),
              dict(**WIND, cut=dict(offset=34.0, length=5.6, fade=2.0, fadein=2.4, highpass=60, lowpass=2500),
                   at=1.9, gain=-5.0)]}),
    "canopy-hum": dict(band_rms=-39.0, ceiling=-6.0, variants={
        "a": [dict(synth="hum", at=0.0, length=LOOP, seed=3641)]}),
}


# =========================================================================== synthesized layers

def edge(x, fin=0.003, fout=0.01):
    x[..., :int(fin * SR)] *= np.linspace(0, 1, int(fin * SR))
    x[..., -int(fout * SR):] *= np.linspace(1, 0, int(fout * SR))
    return x


def unit(x):
    return x / (np.std(x) + 1e-12)


def cracks(p):
    """Paving cracking under stress: `count` short cracks coming faster and louder over the layer
    (their times bunching towards its end), each a few ms of band noise (1.2-6 kHz) with a duller tick
    under it (300-700 Hz), spread across the stereo field."""
    rng = np.random.default_rng(p["seed"])
    n = int(p["length"] * SR)
    out = np.zeros((2, n + int(0.05 * SR)))
    times = p["length"] * np.sort(rng.random(p["count"])) ** 0.55
    for k, t0 in enumerate(times):
        a = int(t0 * SR)
        m = int(rng.uniform(0.004, 0.018) * SR)
        burst = svf(noise(m, rng), np.full(m, rng.uniform(1200, 6000)), q=1.6, mode="bp")
        tick = svf(noise(m, rng), np.full(m, rng.uniform(300, 700)), q=2.0, mode="bp")
        x = (unit(burst) + 0.7 * unit(tick)) * np.exp(-np.arange(m) / (0.25 * m))
        x *= (0.35 + 0.65 * t0 / p["length"]) * rng.uniform(0.4, 1.0)
        out[:, a:a + m] += pan(x, rng.uniform(-0.6, 0.6))
    return edge(out[:, :n])


def slosh(p):
    """A slurry surging up: noise through a low band-pass whose centre drifts slowly (a smoothed random
    walk, 180-700 Hz: no fast wobble, nothing bubbly), swelling over `rise` s and cut back at the end."""
    rng = np.random.default_rng(p["seed"])
    n = int(p["length"] * SR)
    t = np.arange(n) / SR
    walk = np.cumsum(rng.normal(0, 1, n // 2205 + 2))
    walk = np.interp(np.arange(n) / 2205, np.arange(len(walk)), walk)
    centre = 180 * (700 / 180) ** np.clip(0.5 + 0.18 * walk + 0.3 * t / p["length"], 0, 1)
    chans = [unit(svf(noise(n, rng), centre, q=1.1, mode="bp")) for _ in range(2)]
    env = np.clip(t / p["rise"], 0, 1) ** 2 * np.clip((p["length"] - t) / 0.3, 0, 1)
    return edge(np.array(chans) * env)


def creak(p):
    """A root creaking under strain (stick-slip friction): an irregular pulse train whose rate rises
    `rate`[0] -> [1] Hz (fast enough to be a pitch, not single clicks), each pulse ringing two resonances
    that rise `res`[0] -> [1] Hz, low-passed at 2.8 kHz, swelling in."""
    rng = np.random.default_rng(p["seed"])
    n = int(p["length"] * SR)
    t = np.arange(n) / SR
    rate = ramp(n, *p["rate"]) * (1 + 0.25 * np.interp(t, np.linspace(0, p["length"], 40), rng.normal(0, 1, 40)))
    ph = np.cumsum(np.clip(rate, 5, None)) / SR
    pulses = np.diff(np.floor(ph), prepend=0.0) * rng.uniform(0.5, 1.0, n)
    res = ramp(n, *p["res"])
    x = unit(svf(pulses, res, q=9.0, mode="bp")) + 0.6 * unit(svf(pulses, res * 2.3, q=7.0, mode="bp"))
    x = svf(x, np.full(n, 2800.0))                     # a groan, not a ratchet of clicks
    env = np.clip(t / (0.6 * p["length"]), 0, 1) ** 1.5 * np.clip((p["length"] - t) / 0.12, 0, 1)
    return edge(pan(x * env, 0.0))


def swell(p):
    """A growth swelling out of the root: a low tone gliding up 55 -> 95 Hz (with its second and third
    harmonic, so it reads on small speakers) and a breath of noise whose low-pass opens 300 -> 1800 Hz,
    both swelling over the layer and stopping short (the knock follows)."""
    rng = np.random.default_rng(p["seed"])
    n = int(p["length"] * SR)
    t = np.arange(n) / SR
    f = ramp(n, 55.0, 95.0)
    tone = sine(f, n) + 0.5 * sine(2 * f, n) + 0.3 * sine(3 * f, n)
    breath = [unit(svf(noise(n, rng), ramp(n, 300.0, 1800.0), q=0.9)) for _ in range(2)]
    env = (t / p["length"]) ** 1.6 * np.clip((p["length"] - t) / 0.05, 0, 1)
    return edge((0.8 * unit(tone) + 0.5 * np.array(breath)) * env)


def knock(p):
    """The growth set: a short dull knock (a 170 -> 85 Hz sine and a band of noise at 900 Hz, 60 ms) and a
    dry click 15 ms later."""
    rng = np.random.default_rng(p["seed"])
    n = int(p["length"] * SR)
    tt = np.arange(n) / SR
    body = sine(ramp(n, 170.0, 85.0, tau=0.03), n) * np.exp(-tt / 0.06)
    thud = unit(svf(noise(n, rng), np.full(n, 900.0), q=1.5, mode="bp")) * np.exp(-tt / 0.02) * 0.3
    click = np.zeros(n)
    c0, c1 = int(0.015 * SR), int(0.006 * SR)
    click[c0:c0 + c1] = unit(svf(noise(c1, rng), np.full(c1, 3200.0), q=1.2, mode="bp")) * np.exp(
        -np.arange(c1) / (0.3 * c1)) * 0.35
    return edge(pan(body + thud + click, 0.0), 0.001, 0.02)


def lift(p):
    """A vast mass lifting: a low noise rumble (two decorrelated channels) whose band rises 60 -> 220 Hz
    over the layer as the spire climbs, swelling in over 0.6 s and fading over its last second."""
    rng = np.random.default_rng(p["seed"])
    n = int(p["length"] * SR)
    t = np.arange(n) / SR
    centre = ramp(n, 60.0, 220.0)
    chans = [unit(svf(noise(n, rng), centre, q=0.8, mode="bp")) for _ in range(2)]
    env = np.clip(t / 0.6, 0, 1) * np.clip((p["length"] - t) / 1.0, 0, 1)
    return edge(np.array(chans) * env, 0.01, 0.05)


def shockwave(p):
    """An air shockwave: a noise blast (fast attack) whose low-pass falls 2.5 kHz -> 120 Hz as it decays
    (tau 0.45 s), wide in stereo; no stone in it."""
    rng = np.random.default_rng(p["seed"])
    n = int(p["length"] * SR)
    t = np.arange(n) / SR
    cutoff = ramp(n, 2500.0, 120.0, tau=0.25)
    chans = [unit(svf(noise(n, rng), cutoff, q=0.7)) for _ in range(2)]
    env = np.clip(t / 0.01, 0, 1) * np.exp(-t / 0.45)
    return edge(np.array(chans) * env, 0.002, 0.05)


def crackle(p):
    """A dry crackle spreading over the city (leaves and paper drying): sparse micro-bursts (1-4 ms of
    noise, 1.2-5.5 kHz, a few duller at 0.6-1.2 kHz) in short clustered trains, their density swelling over the
    first 1.2 s, easing to 2.8 s and thinning out to a few by 4.6 s; a faint rustle bed (noise above 2.5 kHz) under
    them; every event at its own place across the stereo field."""
    rng = np.random.default_rng(p["seed"])
    n = int(p["length"] * SR)
    t = np.arange(n) / SR
    density = np.interp(t, [0, 1.2, 2.8, 4.6, p["length"]], [20.0, 240.0, 180.0, 25.0, 3.0])   # events a second
    out = np.zeros((2, n + int(0.01 * SR)))
    k = 0.0
    while k < p["length"]:
        k += rng.exponential(1.0 / np.interp(k, t, density))
        if k >= p["length"]:
            break
        for j in range(rng.integers(1, 5)):              # a short train: a crackle, not single clicks
            a = int((k + j * rng.uniform(0.003, 0.012)) * SR)
            m = int(rng.uniform(0.001, 0.004) * SR) + 4
            if a + m >= out.shape[1]:
                break
            fc = rng.uniform(1200, 5500) if rng.random() < 0.8 else rng.uniform(600, 1200)
            x = unit(svf(noise(m, rng), np.full(m, fc), q=1.3, mode="bp")) * np.hanning(m)
            out[:, a:a + m] += pan(x * rng.uniform(0.4, 1.0), rng.uniform(-0.85, 0.85))
    out = out[:, :n]
    rustle = np.array([unit(svf(noise(n, rng), np.full(n, 2500.0), mode="hp")) for _ in range(2)])
    out += 0.06 * rustle * density / density.max()
    return edge(out, 0.02, 0.4)


def sigh(p):
    """A vast exhale: breath noise through two formants falling (650 -> 330 Hz and 1150 -> 700 Hz) and a
    soft low band, swelling over 0.8 s and dying away over the rest."""
    rng = np.random.default_rng(p["seed"])
    n = int(p["length"] * SR)
    t = np.arange(n) / SR
    chans = []
    for _ in range(2):
        src = noise(n, rng)
        x = (unit(svf(src, ramp(n, 650.0, 330.0), q=3.0, mode="bp"))
             + 0.6 * unit(svf(src, ramp(n, 1150.0, 700.0), q=4.0, mode="bp"))
             + 0.5 * unit(svf(src, np.full(n, 180.0), q=0.8)))
        chans.append(x)
    env = np.clip(t / 0.8, 0, 1) ** 1.5 * np.exp(-np.clip(t - 0.8, 0, None) / 1.3)
    return edge(np.array(chans) * env, 0.01, 0.2)


def groan(p):
    """A low tone sinking from 62 to 31 Hz (a saw, low-passed at 400 Hz, with a slow vibrato), swelling in
    over 0.6 s and dying with the sigh."""
    n = int(p["length"] * SR)
    t = np.arange(n) / SR
    f = ramp(n, 62.0, 31.0) * (1 + 0.012 * np.sin(2 * np.pi * 3.1 * t))
    x = svf(saw(f, n), np.full(n, 400.0), q=1.2)
    env = np.clip(t / 0.6, 0, 1) * np.exp(-np.clip(t - 0.6, 0, None) / 1.6)
    return edge(pan(unit(x) * env, 0.0), 0.01, 0.2)


def hum(p):
    """The canopy hum, periodic in `length` s: two harmonic voices (46.00 and 46.25 Hz, harmonics 1-10
    with a spectral peak around 200 Hz) slowly beating, each partial with a slight slow vibrato; a throb
    six times a loop and a 4 s breath swelling the upper harmonics; a band of organic noise (90-500 Hz,
    a different spectrum per channel) made with an inverse FFT, so it is circular too. Every frequency is
    a multiple of 1/length, so the loop's end continues its start exactly."""
    rng = np.random.default_rng(p["seed"])
    L = p["length"]
    n = int(round(L * SR))
    t = np.arange(n) / SR
    base = 1.0 / L                                       # 0.125 Hz: every frequency is a multiple of it
    throb = 0.78 + 0.22 * (0.5 - 0.5 * np.cos(2 * np.pi * 6 * base * t)) ** 2
    breath = 0.5 - 0.5 * np.cos(2 * np.pi * 2 * base * t)
    out = np.zeros((2, n))
    for voice, (f0, position) in enumerate(((46.0, -0.35), (46.25, 0.35))):
        x = np.zeros(n)
        for h in range(1, 11):
            f = h * f0
            amp = (1.0 / h ** 0.6) * (1 + 1.6 * np.exp(-((f - 200.0) / 110.0) ** 2))
            vib = 0.003 * np.sin(2 * np.pi * rng.integers(1, 4) * base * t + rng.uniform(0, 2 * np.pi))
            ph = np.concatenate([[0.0], np.cumsum(f * (1 + vib))[:-1]]) / SR + rng.uniform()
            upper = 0.55 + 0.45 * breath if f > 150 else 1.0
            x += amp * upper * np.sin(2 * np.pi * ph)
        out += pan(x, position)
    freqs = np.fft.rfftfreq(n, 1 / SR)
    shape = np.where((freqs > 90) & (freqs < 500), 1.0, 0.0) * np.exp(-((freqs - 220) / 160) ** 2)
    for ch in range(2):
        spec = shape * np.exp(2j * np.pi * rng.random(len(freqs)))
        band = np.fft.irfft(spec, n)
        out[ch] += 0.35 * np.std(out[ch]) * unit(band) * (0.6 + 0.4 * breath)
    return out * throb


SYNTHS = {"thump": thump, "cracks": cracks, "slosh": slosh, "creak": creak, "swell": swell, "knock": knock,
          "lift": lift, "shockwave": shockwave, "crackle": crackle, "sigh": sigh, "groan": groan, "hum": hum}


# =========================================================================== treatment

def glide(st, s0, s1):
    """Played at a speed gliding from s0 to s1 over the output (pitch and tempo with it)."""
    n = int(st.shape[1] * 2 / (s0 + s1)) - 2
    u = np.arange(n) / n
    pos = n * (s0 * u + 0.5 * (s1 - s0) * u ** 2)
    return np.array([np.interp(pos, np.arange(st.shape[1]), ch) for ch in st])


def seam_db(st):
    """A loop's seam: the sample step across the end-to-start join against the RMS of the steps in the
    20 ms on either side of it (dB; about 0 means the join is like any other step, a click is +20 or more)."""
    k = int(0.02 * SR)
    around = np.concatenate([np.diff(st[:, -k:], axis=1), np.diff(st[:, :k], axis=1)], axis=1)
    join = np.max(np.abs(st[:, 0] - st[:, -1]))
    return db(join + 1e-12) - db(np.sqrt(np.mean(around ** 2)))


def treat(subject, v):
    sound = SOUNDS[subject]

    def run(*paths):
        files = iter(paths)
        parts = []
        for layer in sound["variants"][v]:
            if "page" in layer:
                path = next(files)
                st = recorded({k: val for k, val in layer.items() if k != "approach"}, path)
                if "glide" in layer:
                    st = glide(st, *layer["glide"])
                if "approach" in layer:
                    st = st * undb(layer["approach"] * (1 - np.linspace(0, 1, st.shape[1])))
                if "pan" in layer:
                    st = pan(st.mean(axis=0), layer["pan"])
            else:
                st = SYNTHS[layer["synth"]](layer)
            if parts:
                st = st * undb(loudest_db(parts[0][1]) + layer.get("gain", 0.0) - loudest_db(st))
            parts.append((int(layer["at"] * SR), st))
        out = np.zeros((2, max(at + st.shape[1] for at, st in parts)))
        for at, st in parts:
            out[:, at:at + st.shape[1]] += st
        if "band_rms" in sound:                      # a loop: one gain, no limiter (it would break the seam)
            out = out - out.mean(axis=1, keepdims=True)
            return out * undb(sound["band_rms"] - band_rms_db(out))
        return level(out, sound["level"], sound["ceiling"])
    return run


def build(subject, v, preview_cache):
    layers = [layer for layer in SOUNDS[subject]["variants"][v] if "page" in layer]
    with tempfile.TemporaryDirectory() as tmp:
        found = [(layer["author"], *source(layer, preview_cache, tmp)) for layer in layers]
        st = treat(subject, v)(*(path for _, path, _ in found))
    used = "; ".join(dict.fromkeys(f"{author} from {how}" for author, _, how in found))
    synths = [layer["synth"] for layer in SOUNDS[subject]["variants"][v] if "page" not in layer]
    if synths:
        used += ("; " if used else "") + ", ".join(dict.fromkeys(synths)) + " synthesized"
    name = f"{subject}-r36-{v}"
    out = write_ogg(OUT / f"{name}.ogg", st, max_peak_db=SOUNDS[subject]["ceiling"],
                    tags={"COMMENT": f"{SCRIPT} {name}: {used}"})
    report(out, used)
    if "band_rms" in SOUNDS[subject]:
        x = decode(out)
        print(f"  loop: {st.shape[1]} samples written, {x.shape[1]} decoded; band RMS {band_rms_db(x):.1f} dB; "
              f"seam {seam_db(st):+.1f} dB written, {seam_db(x[:, :st.shape[1]]):+.1f} dB decoded "
              f"(the join's step against the steps around it)")


def main(argv):
    preview_cache = Path(os.environ.get("TMPDIR", tempfile.gettempdir())) / "conceptgame-sfx-cache"
    preview_cache.mkdir(parents=True, exist_ok=True)
    for subject, sound in SOUNDS.items():
        for v in sound["variants"]:
            if argv and subject not in argv and f"{subject}-r36-{v}" not in argv:
                continue
            build(subject, v, preview_cache)


if __name__ == "__main__":
    main(sys.argv[1:])
