#!/usr/bin/env python3
"""Concept round 35 (M5 part G, Level 13 "Polar Relay"): the Nansen Relay's sounds and the supply drone's
launch (decision G9 = a, pending the user's review): a/b for the relay hit, the integrity alarm and grid
online; one proposal each for the drone launch and the relay destroyed (the power-down). Recorded layers
(Freesound originals, every one already in CREDITS.md for earlier rounds, so no new rows) and
synthesized layers mixed.

Outputs (design/audio/sfx/concept/, OGG Vorbis q6, 44.1 kHz stereo; each file starts at its event):

  relay-hit-r35-a.ogg        a "struck plating": a heavy impact on sheet metal ("Impact on metal",
                             JoMungus, CC0) cut short, an electric spark over it ("ELECTRIC_ZAP_001",
                             JoelAudio, CC0) and a sub thump; 0.55 s (every relay-aimed hit, so short)
  relay-hit-r35-b.ogg        b "thud and signal glitch": a dull metallic thud ("slightmetallicthud",
                             profoundsounds, CC0) with a synthesized carrier glitch on top (a bit-crushed
                             data chirp stuttering out, the relay's link dropping for a moment); 0.5 s
  integrity-alarm-r35-a.ogg  a "station alert", synthesized: a hi-lo two-tone (988 / 740 Hz, soft square
                             through a speaker band) three times, 0.22 s a tone; 1.5 s
  integrity-alarm-r35-b.ogg  b "whoop", synthesized: three rising whoops (420 -> 1050 Hz, a buzzy saw with
                             an octave under it), 0.45 s apart; 1.5 s
                             The game plays it once at 50 % and once at 25 % (the README's
                             `ally-below` events), under Nansen's or Okafor's line.
  grid-online-r35-a.ogg      a "breaker chain", synthesized: five heavy breakers closing 0.2 s apart
                             (a clunk and a click each, rising), a mains hum swelling up under them and a
                             bright major chord (A4 C#5 E5 A5) blooming at the end; 2.6 s (the relay's
                             lights coming on in sequence at t=220)
  grid-online-r35-b.ogg      b "spin-up": a recorded power charge rising ("Machine Charge",
                             Glitchedtones, CC0) cut at its peak by a big contact clunk, a synthesized
                             rising whine and the same chord, quieter; 2.6 s
  drone-launch-r35-a.ogg     the supply drone lifting off the relay's helipad (one proposal),
                             synthesized: four rotors spinning up (a buzz at the blade rate, rising
                             from 40 to 190 Hz in 0.5 s, beating against each other), holding, then
                             receding as it climbs and swings over (a pan from centre to the right and
                             back to the left over its 4 s arc, cut at 2.2 s and faded); 2.2 s
  relay-destroyed-r35-a.ogg  the relay destroyed (one proposal): a synthesized power-down (a mains hum
                             and a high whine falling to nothing over 2.2 s) over a heavy blast and its
                             debris ("Big Boom", unfa, CC0) and metal collapsing ("Sounds For
                             Earthquakes - Radiator Metal Rumbling", RutgerMuller, CC0, its loud crash);
                             3.7 s (the mission fails on it; the radio's "Nansen is gone" follows)

Levels (sfx_r24.py's `level`: the loudest 100 ms of the 200 Hz-5 kHz band, limited at a ceiling; set
against the existing set in assets/sfx/, measured the same way): relay hit -19 dB (Level 11's ship hit
-18.3, the shield break -19.7: it plays often, so a little under the ship hit); integrity alarm -17 (a
warning the player must act on: the edge warning -16, the latch -16; louder than the low-armour beep
-20.3, which the user has not found too loud); grid online -17 (a release cue under the radio and the
jingle, between the overdrive start -20 and the edge warning); drone launch -20 (a presentation cue, at
the pickups' and overdrive's -20 to -23; not quieter: the user found quiet layers too quiet); relay
destroyed -15 (a big loss, between the ship sinking -20 and the explosions' -11 to -15). No bubble or
popping layers (the user disliked them in round 33).

Usage: python3 tools/concept/audio/sfx_r35.py [subject-or-file ...]   e.g. relay-hit grid-online-r35-b.
Deterministic. Needs the cached Freesound originals (tools/concept/audio/freesound_fetch.py
--download); without one a layer falls back to the public HQ preview (printed).
"""
import os
import sys
import tempfile
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
from sfx_r24 import level  # noqa: E402
from sfx_r25 import fs, source  # noqa: E402
from sfx_r32 import loudest_db, recorded, report, thump  # noqa: E402
from synth import SR, bitcrush, exp_decay, noise, pan, ramp, saw, sine, svf, undb, write_ogg  # noqa: E402

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "design" / "audio" / "sfx" / "concept"
SCRIPT = "tools/concept/audio/sfx_r35.py"
CC0 = "CC0 1.0"

IMPACT = dict(**fs("JoMungus", 726486, 726, 11865776), title="Impact on metal", author="JoMungus", licence=CC0)
ZAP = dict(**fs("JoelAudio", 136542, 136, 1206321), title="ELECTRIC_ZAP_001.wav", author="JoelAudio", licence=CC0)
THUD = dict(**fs("profoundsounds", 686282, 686, 14947773), title="slightmetallicthud.mp3", author="profoundsounds",
            licence=CC0)
CHARGE = dict(**fs("Glitchedtones", 375925, 375, 3294528), title="Machine Charge.wav", author="Glitchedtones",
              licence=CC0)
BOOM = dict(**fs("unfa", 189779, 189, 1038806), title="Big Boom", author="unfa", licence=CC0)
RADIATOR = dict(**fs("RutgerMuller", 51097, 51, 179538),
                title="Sounds For Earthquakes - Radiator Metal Rumbling.wav", author="RutgerMuller", licence=CC0)

CHORD = (440.0, 554.37, 659.26, 880.0)          # A major: grid online's bloom

# subject: level (dB, the loudest 100 ms of the 200 Hz-5 kHz band), ceiling (dBFS), and per variant its
# layers (as sfx_r34.py: a recorded layer has a source and the cut, optional speed and pan; a
# synthesized layer `synth` and its parameters; each enters at `at`, the others `gain` dB from the
# first).
SOUNDS = {
    "relay-hit": dict(level=-19.0, ceiling=-3.0, variants={
        "a": [dict(**IMPACT, cut=dict(offset=0.0, length=0.55, fade=0.3, highpass=70), at=0.0),
              dict(**ZAP, cut=dict(offset=0.05, length=0.3, fade=0.18, fadein=0.005, highpass=900), at=0.02,
                   gain=-8.0, pan=0.15),
              dict(synth="thump", at=0.0, gain=-6.0, length=0.3, start=95.0, end=45.0, tau=0.07)],
        "b": [dict(**THUD, cut=dict(offset=0.0, length=0.45, fade=0.25, highpass=60), at=0.0),
              dict(synth="glitch", at=0.03, gain=-5.0, length=0.42, seed=3501),
              dict(synth="thump", at=0.0, gain=-7.0, length=0.3, start=85.0, end=40.0, tau=0.08)]}),
    "integrity-alarm": dict(level=-17.0, ceiling=-3.0, variants={
        "a": [dict(synth="two_tone", at=0.0, length=1.5, high=988.0, low=740.0, tone=0.22, cycles=3)],
        "b": [dict(synth="whoop", at=0.0, length=1.5, start=420.0, end=1050.0, period=0.45, count=3)]}),
    "grid-online": dict(level=-17.0, ceiling=-2.0, variants={
        "a": [dict(synth="breakers", at=0.0, length=2.6, count=5, spacing=0.2, seed=3511),
              dict(synth="mains", at=0.0, gain=-6.0, length=2.6, rise=1.2),
              dict(synth="bloom", at=1.05, gain=-1.0, length=1.55, swell=0.25)],
        "b": [dict(**CHARGE, cut=dict(offset=0.25, length=1.25, fade=0.03, fadein=0.05, highpass=60), at=0.0),
              dict(synth="breakers", at=1.2, gain=-1.0, length=1.4, count=1, spacing=0.2, seed=3512),
              dict(synth="rise", at=0.0, gain=-10.0, length=1.25, start=300.0, end=1400.0),
              dict(synth="bloom", at=1.25, gain=-5.0, length=1.35, swell=0.15)]}),
    "drone-launch": dict(level=-20.0, ceiling=-4.0, variants={
        "a": [dict(synth="rotors", at=0.0, length=2.2, seed=3521)]}),
    "relay-destroyed": dict(level=-15.0, ceiling=-1.5, variants={
        "a": [dict(**BOOM, cut=dict(offset=0.0, length=3.6, fade=1.8, highpass=30), at=0.12),
              dict(synth="power_down", at=0.0, gain=-4.0, length=2.4),
              dict(**RADIATOR, cut=dict(offset=3.85, length=2.6, fade=1.4, fadein=0.01, highpass=90), at=0.55,
                   gain=-5.0, pan=-0.2),
              dict(synth="thump", at=0.12, gain=-3.0, length=0.9, start=70.0, end=30.0, tau=0.3)]}),
}


# =========================================================================== synthesized layers

def edge(x, fin=0.003, fout=0.01):
    x[..., :int(fin * SR)] *= np.linspace(0, 1, int(fin * SR))
    x[..., -int(fout * SR):] *= np.linspace(1, 0, int(fout * SR))
    return x


def glitch(p):
    """A carrier glitch: a 1.6 kHz data chirp (square-ish, stepping in pitch every 30 ms) bit-crushed
    and gated in a stutter that thins out, with band noise under it; the relay's link dropping."""
    rng = np.random.default_rng(p["seed"])
    n = int(p["length"] * SR)
    t = np.arange(n) / SR
    steps = rng.choice([1200.0, 1600.0, 2100.0, 900.0], size=int(p["length"] / 0.03) + 1)
    f = steps[(t / 0.03).astype(int)]
    tone = np.tanh(2.5 * sine(f, n))
    hiss = svf(noise(n, rng), np.full(n, 2600.0), q=1.2, mode="bp")
    x = 0.8 * tone + 0.5 * hiss / (np.std(hiss) + 1e-9) * 0.3
    gate = (rng.random(int(p["length"] / 0.022) + 1) < 0.75).astype(float)
    g = gate[(t / 0.022).astype(int)]
    g = svf(g, np.full(n, 300.0))                       # soften the gate's edges
    x = bitcrush(x * g * np.exp(-t / 0.16), bits=5, downsample=3)
    return edge(np.vstack([x, x * 0.9]))


def two_tone(p):
    """A hi-lo alert: a soft square (odd partials, rolled off) alternating high and low, `cycles` times,
    through a small speaker's band (450 Hz - 3.5 kHz)."""
    n = int(p["length"] * SR)
    t = np.arange(n) / SR
    k = (t / p["tone"]).astype(int)
    f = np.where(k % 2 == 0, p["high"], p["low"])
    on = k < 2 * p["cycles"]
    x = sum(sine(f * h, n) / h ** 1.3 for h in (1, 3, 5, 7))
    ph = (t % p["tone"]) / p["tone"]
    env = np.clip(ph / 0.04, 0, 1) * np.clip((1 - ph) / 0.06, 0, 1) * on
    x = x * env
    x = svf(svf(x, np.full(n, 450.0), mode="hp"), np.full(n, 3500.0))
    return edge(pan(x, 0.0))


def whoop(p):
    """Rising whoops: a buzzy saw (and its octave below) sweeping start -> end over each period's first
    80 %, swelling in and cut short, `count` times."""
    n = int(p["length"] * SR)
    t = np.arange(n) / SR
    ph = (t % p["period"]) / p["period"]
    on = (t / p["period"]).astype(int) < p["count"]
    u = np.clip(ph / 0.8, 0, 1)
    f = p["start"] * (p["end"] / p["start"]) ** u
    x = saw(f, n) * 0.6 + 0.5 * sine(f / 2, n)
    env = np.clip(ph / 0.08, 0, 1) * np.clip((0.86 - ph) / 0.06, 0, 1) * on
    x = svf(x * env, np.full(n, 4000.0))
    return edge(pan(x, 0.0))


def breakers(p):
    """Heavy breakers closing: per breaker a low clunk (a short falling sine and a noise burst through a
    low band) and a sharp click a few ms later, each a little higher than the one before."""
    rng = np.random.default_rng(p["seed"])
    n = int(p["length"] * SR)
    out = np.zeros((2, n))
    for i in range(p["count"]):
        a = int(i * p["spacing"] * SR)
        m = min(int(0.32 * SR), n - a)
        tt = np.arange(m) / SR
        up = 1.0 + 0.12 * i
        clunk = sine(ramp(m, 160.0 * up, 70.0 * up, tau=0.03), m) * np.exp(-tt / 0.06)
        burst = svf(noise(m, rng), np.full(m, 900.0 * up), q=1.5, mode="bp") * np.exp(-tt / 0.025)
        click = np.zeros(m)
        c0, c1 = int(0.008 * SR), int(0.012 * SR)
        click[c0:c0 + c1] = svf(noise(c1, rng), np.full(c1, 4500.0), q=0.8, mode="bp") * np.exp(-np.arange(c1) / 90)
        x = clunk + 0.5 * burst / (np.std(burst) + 1e-9) * 0.4 + 2.5 * click
        x[:int(0.001 * SR)] *= np.linspace(0, 1, int(0.001 * SR))
        position = -0.5 + (i / max(1, p["count"] - 1)) if p["count"] > 1 else 0.0
        out[:, a:a + m] += pan(x, position)
    return edge(out)


def mains(p):
    """A mains hum swelling up: 50 Hz saw partials through a low-pass that opens over `rise` s."""
    n = int(p["length"] * SR)
    t = np.arange(n) / SR
    x = saw(np.full(n, 50.0), n)
    u = np.clip(t / p["rise"], 0, 1)
    x = svf(x, 150 + 1400 * u ** 2, q=1.4) * (0.2 + 0.8 * u) * np.clip((p["length"] - t) / 0.6, 0, 1)
    return edge(np.vstack([x, x]))


def bloom(p):
    """A bright major chord blooming: each note a few detuned sines with a soft bell attack, swelling
    over `swell` s and ringing out."""
    n = int(p["length"] * SR)
    t = np.arange(n) / SR
    out = np.zeros((2, n))
    for k, f in enumerate(CHORD):
        for d, pos in ((0.997, -0.4), (1.003, 0.4)):
            out += pan(sine(np.full(n, f * d), n) + 0.25 * sine(np.full(n, 2 * f * d), n), pos) / (1 + 0.2 * k)
    env = np.clip(t / p["swell"], 0, 1) ** 2 * np.exp(-np.clip(t - p["swell"], 0, None) / 0.6)
    return edge(out * env, 0.01, 0.05)


def rise(p):
    """A thin whine rising start -> end with the charge."""
    n = int(p["length"] * SR)
    x = sine(ramp(n, p["start"], p["end"], curve="exp"), n) * np.linspace(0.3, 1.0, n)
    return edge(np.vstack([x, x]))


def rotors(p):
    """A small quad-rotor lifting off: four rotors' buzz (narrow pulses at the blade rate, a little
    detuned so they beat) spinning up from 40 to ~190 Hz in 0.5 s, a breath of downwash noise, then
    climbing away: quieter and duller, panned from the centre to the right and back across."""
    rng = np.random.default_rng(p["seed"])
    n = int(p["length"] * SR)
    t = np.arange(n) / SR
    spin = 40 + 150 * (1 - np.exp(-t / 0.18))
    x = np.zeros(n)
    for d in (0.985, 0.995, 1.006, 1.017):
        ph = (np.cumsum(spin * d) / SR) % 1.0
        x += np.exp(-ph / 0.08) - 0.08          # a soft pulse each blade pass
    x = svf(x, np.full(n, 2400.0))
    wash = svf(noise(n, rng), np.full(n, 900.0), q=0.7, mode="bp")
    x = x / (np.std(x) + 1e-9) + 0.35 * wash / (np.std(wash) + 1e-9) * np.clip(t / 0.3, 0, 1)
    away = np.clip((t - 0.8) / 1.4, 0, 1)
    x = svf(x, 5000 - 3200 * away) * (1 - 0.7 * away)
    pos = np.where(t < 1.2, 0.5 * t / 1.2, 0.5 - 1.2 * (t - 1.2) / 1.0)
    return edge(np.array([x * np.cos((pos + 1) * np.pi / 4), x * np.sin((pos + 1) * np.pi / 4)]), 0.02, 0.25)


def power_down(p):
    """A power-down: a mains hum (60 Hz saw) and a whine (1.3 kHz) falling to nothing, slowing like a
    generator losing its load, with a low-pass closing; ends in silence."""
    n = int(p["length"] * SR)
    t = np.arange(n) / SR
    u = t / p["length"]
    slow = (1 - u) ** 1.6
    hum = saw(60 * (0.15 + 0.85 * slow), n)
    whine = sine(1300 * (0.08 + 0.92 * slow), n)
    x = svf(hum, 200 + 1800 * slow, q=1.2) + 0.35 * whine
    x *= (1 - u) ** 0.8
    return edge(np.vstack([x, x]), 0.005, 0.05)


SYNTHS = {"thump": thump, "glitch": glitch, "two_tone": two_tone, "whoop": whoop, "breakers": breakers,
          "mains": mains, "bloom": bloom, "rise": rise, "rotors": rotors, "power_down": power_down}


# =========================================================================== treatment

def treat(subject, v):
    sound = SOUNDS[subject]

    def run(*paths):
        files = iter(paths)
        parts = []
        for layer in sound["variants"][v]:
            if "page" in layer:
                st = recorded(layer, next(files))
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
    name = f"{subject}-r35-{v}"
    out = write_ogg(OUT / f"{name}.ogg", st, max_peak_db=SOUNDS[subject]["ceiling"],
                    tags={"COMMENT": f"{SCRIPT} {name}: {used}"})
    report(out, used)


def main(argv):
    preview_cache = Path(os.environ.get("TMPDIR", tempfile.gettempdir())) / "conceptgame-sfx-cache"
    preview_cache.mkdir(parents=True, exist_ok=True)
    for subject, sound in SOUNDS.items():
        for v in sound["variants"]:
            if argv and subject not in argv and f"{subject}-r35-{v}" not in argv:
                continue
            build(subject, v, preview_cache)


if __name__ == "__main__":
    main(sys.argv[1:])
