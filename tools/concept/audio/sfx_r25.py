#!/usr/bin/env python3
"""Concept round 25 (M4 part G, Level 07): the Brood Carrier's sounds and the lifeboat tow's cable
snap, an a/b pair per sound, recorded (CC0 / CC-BY, Freesound).

Outputs (design/audio/sfx/concept/, OGG Vorbis q6, 44.1 kHz stereo):

  enemy-carrier-roar-r25-<v>.ogg       the carrier's roar: as it arrives (with the klaxon and the
                                       warning banner) and again, a little lower, as it turns
                                       broadside (the start of the turn frames)
  enemy-carrier-sac-open-r25-<v>.ogg   a bay sac's membrane parting as its window opens
  enemy-carrier-sac-close-r25-<v>.ogg  the sac sucking shut as its window closes (quieter)
  enemy-carrier-launch-r25-<v>.ogg     a Skitter or Needler spat out of an open sac (the
                                       simulation's BOSS_LAUNCHED, one per unit)
  enemy-carrier-sac-burst-r25-<v>.ogg  a bay sac destroyed: a big wet burst, played 10 % slower for
                                       its size (also the sacs' bursts in the death chain)
  enemy-carrier-iris-r25-<v>.ogg       the plate iris over the core opening (1 s) at the core phase
  secret-cable-snap-r25-<v>.ogg        the lifeboat's amber tow cable snapping on its third hit as
                                       the cargo pod falls free

Sources (the `SOUNDS` table; every one in CREDITS.md): each is cut from the Freesound **original**
when it is cached (tools/concept/audio/freesound_fetch.py --download, ~/.cache/terran-vanguard/
freesound/<id>_<name>.<type>; clipped at full scale first, as tools/art/sfx_originals.py does),
else from the public HQ preview (with a note). The cut is import_sfx.py's `process` (leading
silence trimmed, offset, length, fades, filters); then an optional speed change (resampled: lower
and longer below 1) or reversal, then the level.

Levels: the loudest 100 ms of the 200 Hz-5 kHz band (sfx_r24.py's `level`, limited at the
ceiling), set against the existing sounds measured the same way: the roar -19 dB (the Vrell
screeches and the Leviathan's cry measure -24 to -27, the klaxon -23: the boss is louder than its
escorts), ceiling -3 dBFS; a sac opening -24 dB (the Vrell spawns -23 / -27), its closing -28 dB,
ceiling -4 / -8 dBFS; a launch -24 dB, ceiling -6 dBFS (several play at once); a sac burst -15
dB (explosion-r02-a -15.2, the Coilwyrm's head burst -14.7), ceiling -1.5 dBFS; the iris -20
dB, ceiling -3 dBFS; the cable snap -17 dB (the salvage-large pickup -17.7, the edge warning
-16.2), ceiling -1.5 dBFS. A sharp transient meets its ceiling before its band reaches the level:
the script prints what each file reaches.

Nobody listened to these: they were picked by the sources' descriptions, ratings and tags and by
their envelopes, band balance and levels (printed by this script for every file it writes).

Usage: python3 tools/concept/audio/sfx_r25.py [subject-or-file ...]
       e.g. enemy-carrier-roar (both variants) or enemy-carrier-roar-r25-a. Deterministic.
"""
import os
import sys
import tempfile
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
from import_sfx import band_rms_db, fetch, process  # noqa: E402
from sfx_r24 import level, resample, short_term_band_db  # noqa: E402
from synth import SR, db, decode, write_ogg, write_wav  # noqa: E402

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "design" / "audio" / "sfx" / "concept"
CACHE = Path.home() / ".cache" / "terran-vanguard" / "freesound"
SCRIPT = "tools/concept/audio/sfx_r25.py"
CC0 = "CC0 1.0"
BY3 = "CC-BY 3.0"
BY4 = "CC-BY 4.0"


def fs(user, sound, preview_dir, preview_user):
    """A Freesound sound's page and HQ preview URLs."""
    return dict(page=f"https://freesound.org/people/{user}/sounds/{sound}/",
                preview=f"https://cdn.freesound.org/previews/{preview_dir}/{sound}_{preview_user}-hq.ogg")


# subject: its level (loudest 100 ms of the 200 Hz-5 kHz band, dB), ceiling (dBFS) and the two
# variants: source, title, author, licence, the cut (import_sfx.py's process keys) and options
# (speed: resampled playback speed; reverse: played backwards).
SOUNDS = {
    "enemy-carrier-roar": dict(level=-19.0, ceiling=-3.0, variants={
        # a huge creature's roar with a long echo tail: a 0.6 s swell, 2.5 s of roar, the echo
        "a": dict(**fs("noahpardo", 345735, 345, 6246023), title="Deep Roar Echo 2.wav",
                  author="noahpardo", licence=CC0,
                  cut=dict(offset=0.0, length=4.0, fade=1.4, highpass=30)),
        # a bear's roar mixed with a didgeridoo drone: a giant animal, a buzzing, whale-like body
        "b": dict(**fs("Noxdl", 204912, 204, 3048844), title="Didgeridoo Monster Roar",
                  author="Noxdl", licence=CC0,
                  cut=dict(offset=0.0, length=3.0, fade=0.8, highpass=30)),
    }),
    "enemy-carrier-sac-open": dict(level=-24.0, ceiling=-4.0, variants={
        # flesh being pulled apart close to the mic, the first of its three gestures
        "a": dict(**fs("KVV_Audio", 796506, 796, 12846320),
                  title="GOREFlsh_Flesh Manipulation 01_KVV AUDIO_FREE", author="KVV_Audio", licence=BY4,
                  cut=dict(offset=0.0, length=0.85, fade=0.25, highpass=80)),
        # something ripped out of a sucking mud: a long wet tear, its first 0.9 s
        "b": dict(**fs("LucasDuff", 516643, 516, 8272463), title="Squelch.mp3", author="LucasDuff",
                  licence=CC0, cut=dict(offset=0.25, length=0.9, fade=0.3, fadein=0.05, highpass=80)),
    }),
    "enemy-carrier-sac-close": dict(level=-28.0, ceiling=-8.0, variants={
        # the same recording's third gesture, shorter: the membrane folding back
        "a": dict(**fs("KVV_Audio", 796506, 796, 12846320),
                  title="GOREFlsh_Flesh Manipulation 01_KVV AUDIO_FREE", author="KVV_Audio", licence=BY4,
                  cut=dict(offset=4.4, length=0.6, fade=0.25, fadein=0.03, highpass=80)),
        # the opening's tear played backwards: a wet suck that ends shut
        "b": dict(**fs("LucasDuff", 516643, 516, 8272463), title="Squelch.mp3", author="LucasDuff",
                  licence=CC0, cut=dict(offset=0.45, length=0.6, fade=0.05, fadein=0.2, highpass=80),
                  reverse=True),
    }),
    "enemy-carrier-launch": dict(level=-24.0, ceiling=-6.0, variants={
        # a creature's throaty spit (made for a student game's boss)
        "a": dict(**fs("bananplyte", 452169, 452, 6175868), title="Spit 1 - The Ridge - Spanker",
                  author="bananplyte", licence=CC0, cut=dict(offset=0.0, length=0.5, fade=0.25, highpass=80)),
        # a slime monster's lunge: a layered wet slap, brighter
        "b": dict(**fs("qubodup", 751338, 751, 71257), title="Slime Attack 1", author="qubodup",
                  licence=CC0, cut=dict(offset=0.0, length=0.4, fade=0.2, highpass=80)),
    }),
    "enemy-carrier-sac-burst": dict(level=-15.0, ceiling=-1.5, variants={
        # "an exceptionally wet and fleshy explosion", 10 % slower
        "a": dict(**fs("SilverIllusionist", 470586, 470, 7395592), title="Headshot 2",
                  author="SilverIllusionist", licence=BY4,
                  cut=dict(offset=0.0, length=1.4, fade=0.6, highpass=50), speed=0.9),
        # ripped cardboard and poured water: a visceral tear with a wet tail, 10 % slower
        "b": dict(**fs("magnuswaker", 522159, 522, 11537497), title="Gutsy Spillage 1",
                  author="magnuswaker", licence=CC0,
                  cut=dict(offset=0.0, length=1.1, fade=0.5, highpass=50), speed=0.9),
    }),
    "enemy-carrier-iris": dict(level=-20.0, ceiling=-3.0, variants={
        # an alien hatch morphing open with a pressure release up front
        "a": dict(**fs("Paul368", 264061, 264, 2971294), title="SFX Door Open.wav", author="Paul368",
                  licence=CC0, cut=dict(offset=0.0, length=1.6, fade=0.6, highpass=80)),
        # a monster's body morphing: a grinding, organic churn, its first 1.3 s
        "b": dict(**fs("Division4884", 342336, 342, 6138200), title="Simple Mutate (Monster)",
                  author="Division4884", licence=CC0,
                  cut=dict(offset=0.1, length=1.3, fade=0.4, fadein=0.05, highpass=40)),
    }),
    "secret-cable-snap": dict(level=-17.0, ceiling=-1.5, variants={
        # a chain snapping: a sharp crack, the loose chain rattling after it
        "a": dict(**fs("CosmicEmbers", 161650, 161, 2895542), title="snapping-chain", author="CosmicEmbers",
                  licence=BY3, cut=dict(offset=0.0, length=1.2, fade=0.5, highpass=60)),
        # a guitar string snapping, 25 % slower: a heavier steel cable's twang and ring (the guitar
        # body's boom high-passed at 200 Hz first, 150 Hz after the slowdown)
        "b": dict(**fs("juskiddink", 58491, 58, 649468), title="Guitar string snaps.wav", author="juskiddink",
                  licence=BY4, cut=dict(offset=0.48, length=1.2, fade=0.7, fadein=0.02, highpass=200),
                  speed=0.75),
    }),
}


def original(page):
    """The cached Freesound original of a sound page, or None."""
    sound = page.rstrip("/").rsplit("/", 1)[1]
    files = sorted(p for p in CACHE.glob(f"{sound}_*") if not p.name.endswith(".part"))
    return files[0] if files else None


def source(src, preview_cache, tmp):
    """The file to cut: the original (clipped at full scale if it decodes beyond it), else the preview."""
    path = original(src["page"])
    if path is None:
        return fetch(src["preview"], preview_cache), "HQ preview (no cached original)"
    x = decode(path)
    if np.max(np.abs(x)) <= 1:
        return path, f"original {path.name}"
    out = Path(tmp) / f"{path.stem}-clipped.wav"
    write_wav(out, np.clip(x, -1, 1))
    return out, f"original {path.name} (clipped at full scale)"


def build(subject, v, preview_cache):
    sound = SOUNDS[subject]
    src = sound["variants"][v]
    with tempfile.TemporaryDirectory() as tmp:
        path, used = source(src, preview_cache, tmp)
        st = process(dict(src["cut"], peak=-1.0), path)
    if src.get("speed", 1.0) != 1.0:
        st = resample(st, src["speed"])
    if src.get("reverse"):
        st = st[:, ::-1].copy()
    st = level(st, sound["level"], sound["ceiling"])
    name = f"{subject}-r25-{v}"
    tags = {"COMMENT": f"{SCRIPT} {name} from {used}"}
    out = write_ogg(OUT / f"{name}.ogg", st, max_peak_db=sound["ceiling"], tags=tags)
    report(out, used)


def report(path, used):
    """The checks we can make without listening: length, peak, levels, DC, the edges, the envelope."""
    x = decode(path)
    edge = int(0.005 * SR)
    n = int(0.1 * SR)
    env = [db(np.sqrt(np.mean(x[:, k:k + n] ** 2)) + 1e-9) for k in range(0, x.shape[1], n)]
    mono = x.mean(axis=0)
    spec = np.abs(np.fft.rfft(mono)) ** 2
    freqs = np.fft.rfftfreq(len(mono), 1 / SR)
    total = spec.sum() + 1e-20
    low, mid = spec[freqs < 200].sum() / total, spec[(freqs >= 200) & (freqs < 5000)].sum() / total
    print(f"wrote {path.relative_to(ROOT)} ({used})\n"
          f"  {x.shape[1] / SR:.2f} s, peak {db(np.abs(x).max()):.1f} dBFS, loudest 100 ms band "
          f"{short_term_band_db(x):.1f} dB, band RMS {band_rms_db(x):.1f} dB, DC {np.mean(x):+.5f}, "
          f"edges {db(np.abs(x[:, :edge]).max()):.0f}/{db(np.abs(x[:, -edge:]).max()):.0f} dBFS, "
          f"energy <200 Hz {low:.0%}, 200 Hz-5 kHz {mid:.0%}\n"
          f"  envelope (100 ms RMS, dBFS): {' '.join(f'{e:.0f}' for e in env)}")


def main(argv):
    preview_cache = Path(os.environ.get("TMPDIR", tempfile.gettempdir())) / "conceptgame-sfx-cache"
    preview_cache.mkdir(parents=True, exist_ok=True)
    for subject, sound in SOUNDS.items():
        for v in sound["variants"]:
            if argv and subject not in argv and f"{subject}-r25-{v}" not in argv:
                continue
            build(subject, v, preview_cache)


if __name__ == "__main__":
    main(sys.argv[1:])
