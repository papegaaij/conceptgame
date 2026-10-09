#!/usr/bin/env python3
"""Production audio: the recorded sound effects rebuilt from the Freesound originals
(design/art-direction/production, "SFX"; design/audio/sfx).

Outputs (assets/sfx/, OGG Vorbis q6, Git LFS):
  <name>.ogg   one per chosen recorded concept sound (design/audio/sfx/README.md, Concept art
               rows whose status starts with "chosen"), under the concept file's name, so the
               game's paths and the CREDITS.md rows stay the same. Each carries a SOURCE comment
               naming this script and the original file.

The treatment is the concept one, unchanged: tools/concept/audio/import_sfx.py's SOURCES entry
(offset, length, fades, loop, filters, peak or band levelling) is applied by its `process` to the
original file instead of the lossy HQ preview. Sounds with a treatment of their own bring it along
(DERIVED: tools/concept/audio/sfx_r24.py's PRODUCTION, the Coilwyrm's bursts: cut, levelled on
the loudest 100 ms, the head's slowed over a sub thump; sfx_r25.py's PRODUCTION, the Brood
Carrier's sounds and the cable snap: cut, slowed or reversed, levelled on the loudest 100 ms;
sfx_r27.py's PRODUCTION, the Coilwyrm's chain-cut tear: cut, slowed, levelled on the loudest
100 ms; sfx_r31.py's PRODUCTION, the arcology's collapse, a crash over a swelling rumble from two
originals (an entry's `pages`, in the order its build takes them), levelled to -15 LUFS, and the
Ravager's two pounces, a snarl over a synthesized whoosh; sfx_r32.py's PRODUCTION, the Wraith's
decloak, a rip over a velvet cloak, the Mote Swarm's insect chitter, five copies of one original over
a synthesized whoosh, and the lance, a synthesized whine into a thunder strike; sfx_r33.py's
PRODUCTION, Level 11's Kraken slam, churn, surfacing and death, the ship hit and sinking and the
frigate's flak, recordings over synthesized layers). One step comes first: an original whose decoded
samples exceed full scale (lossy originals and float WAVs, up to +18.7 dBFS for "Machine Gun 001")
is clipped at full scale, as every integer decoder plays it and as Freesound made the preview the
user chose from; levelled on its unclipped peak, such a sound came out up to 11 dB quieter. Originals come from the cache that
tools/concept/audio/freesound_fetch.py --download fills (~/.cache/terran-vanguard/freesound/,
<id>_<name>.<type>); a missing one stops the script with that hint. The concept files in
design/audio/sfx/concept/ stay as they are: they document what was chosen.

The check (always run after writing, or alone with --check) compares every output against its
chosen concept file: duration, sample peak, the 200 Hz-5 kHz band RMS that import_sfx levels on,
and the RMS of four spectrum bands; it flags a length difference above 10 ms and a peak or band
RMS difference above 1 dB.

Run: python3 tools/art/sfx_originals.py [--check] [name ...]   (~1 min for all)
"""
import re
import subprocess
import sys
import tempfile
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "concept" / "audio"))
from import_sfx import SOURCES, band_rms_db, process  # noqa: E402
from sfx_r24 import PRODUCTION as R24  # noqa: E402  (sounds with their own treatment: round 24)
from sfx_r25 import PRODUCTION as R25  # noqa: E402  (and round 25)
from sfx_r27 import PRODUCTION as R27  # noqa: E402  (and round 27's tear)
from sfx_r31 import PRODUCTION as R31  # noqa: E402  (and round 31's collapse and pounces)
from sfx_r32 import PRODUCTION as R32  # noqa: E402  (and round 32's decloak, swarm and lance)
from sfx_r33 import PRODUCTION as R33  # noqa: E402  (and round 33's Kraken, ships and flak)
from synth import SR, db, decode, write_ogg, write_wav  # noqa: E402

DERIVED = {**R24, **R25, **R27, **R31, **R32, **R33}
ROOT = Path(__file__).resolve().parents[2]
SFX_DOC = ROOT / "design" / "audio" / "sfx" / "README.md"
CONCEPT = ROOT / "design" / "audio" / "sfx" / "concept"
OUT = ROOT / "assets" / "sfx"
CACHE = Path.home() / ".cache" / "terran-vanguard" / "freesound"
SCRIPT = "tools/art/sfx_originals.py"
ROW = re.compile(r"^\| \[concept/([\w-]+)\.ogg\]\(concept/[\w-]+\.ogg\) \|.*\| ([^|]+) \|$", re.M)
SOUND_ID = re.compile(r"/sounds/(\d+)/")
BANDS = ((0, 200), (200, 2000), (2000, 5000), (5000, SR / 2))
MAX_LENGTH_DIFF = 0.010   # s
MAX_LEVEL_DIFF = 1.0      # dB


def chosen():
    """The SOURCES entries whose Concept art row in the SFX README is chosen."""
    status = {name: s.strip() for name, s in ROW.findall(SFX_DOC.read_text(encoding="utf-8"))}
    return [name for name in [*SOURCES, *DERIVED] if status.get(name, "").startswith("chosen")]


def original(page):
    sound = SOUND_ID.search(page)[1]
    files = sorted(p for p in CACHE.glob(f"{sound}_*") if not p.name.endswith(".part"))
    if not files:
        raise SystemExit(f"original {sound} is not cached: run "
                         "python3 tools/concept/audio/freesound_fetch.py --download")
    return sound, files[0]


def build(name):
    """A sound from its original, or from its originals (a DERIVED entry's `pages`, one per layer)."""
    src = SOURCES.get(name) or DERIVED[name]
    found = [original(page) for page in src.get("pages", [src["page"]])]
    note = (f"{SCRIPT} (production audio) from the Freesound original{'s' if len(found) > 1 else ''} "
            + ", ".join(f"{sound} {path.name}" for sound, path in found))
    with tempfile.TemporaryDirectory() as tmp:
        paths = []
        for i, (_, path) in enumerate(found):
            x = decode(path)
            if np.max(np.abs(x)) > 1:
                path = Path(tmp) / f"clipped-{i}.wav"
                write_wav(path, np.clip(x, -1, 1))
            paths.append(path)
        out = src["build"](*paths) if "build" in src else process(src, paths[0])
    write_ogg(OUT / f"{name}.ogg", out, max_peak_db=src["peak"], tags={"SOURCE": note})
    print(f"wrote assets/sfx/{name}.ogg  <- {', '.join(path.name for _, path in found)}")


def band_db(x, lo, hi):
    mono = x.mean(axis=0)
    spec = np.fft.rfft(mono)
    freqs = np.fft.rfftfreq(len(mono), 1 / SR)
    spec[(freqs < lo) | (freqs >= hi)] = 0
    return db(np.sqrt(np.mean(np.fft.irfft(spec, len(mono)) ** 2)) + 1e-12)


def samples(path):
    """The stream's length from its last granule position: ffmpeg's decoder output differs from it
    by up to ~1000 samples of end padding, which a player does not play."""
    out = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "stream=duration_ts", "-of", "csv=p=0",
                          str(path)], capture_output=True, check=True, text=True).stdout
    return int(out.strip())


def measure(path):
    x = decode(path)
    return dict(length=samples(path) / SR, peak=db(np.max(np.abs(x)) + 1e-12), band=band_rms_db(x),
                bands=[band_db(x, lo, hi) for lo, hi in BANDS])


def check(names):
    print(f"{'sound':32} {'len s':>11} {'peak dBFS':>13} {'band dB':>13}  "
          "spectrum <200 / 200-2k / 2-5k / >5k (final - concept, dB)")
    outliers = []
    for name in names:
        a, b = measure(CONCEPT / f"{name}.ogg"), measure(OUT / f"{name}.ogg")
        flags = [what for what, bad in (
            ("length", abs(a["length"] - b["length"]) > MAX_LENGTH_DIFF),
            ("peak", abs(a["peak"] - b["peak"]) > MAX_LEVEL_DIFF),
            ("band", abs(a["band"] - b["band"]) > MAX_LEVEL_DIFF)) if bad]
        spectrum = " / ".join(f"{y - x:+5.1f}" for x, y in zip(a["bands"], b["bands"]))
        print(f"{name:32} {a['length']:5.2f}{b['length']:6.2f} {a['peak']:6.1f}{b['peak']:7.1f} "
              f"{a['band']:6.1f}{b['band']:7.1f}  {spectrum}  {' '.join(flags).upper()}")
        if flags:
            outliers.append(name)
    print(f"{len(names)} sounds, {len(outliers)} outside the tolerances: {', '.join(outliers) or 'none'}")


def main(argv):
    only_check = "--check" in argv
    wanted = [a for a in argv if a != "--check"]
    names = chosen()
    unknown = set(wanted) - set(names)
    if unknown:
        raise SystemExit(f"not a chosen recorded sound: {', '.join(sorted(unknown))}")
    names = [n for n in names if not wanted or n in wanted]
    if not only_check:
        OUT.mkdir(parents=True, exist_ok=True)
        for name in names:
            build(name)
    check(names)


if __name__ == "__main__":
    main(sys.argv[1:])
