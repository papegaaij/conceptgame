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
original file instead of the lossy HQ preview. One step comes first: an original whose decoded
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
from synth import SR, db, decode, write_ogg, write_wav  # noqa: E402

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
    return [name for name in SOURCES if status.get(name, "").startswith("chosen")]


def original(src):
    sound = SOUND_ID.search(src["page"])[1]
    files = sorted(p for p in CACHE.glob(f"{sound}_*") if not p.name.endswith(".part"))
    if not files:
        raise SystemExit(f"original {sound} is not cached: run "
                         "python3 tools/concept/audio/freesound_fetch.py --download")
    return sound, files[0]


def build(name):
    src = SOURCES[name]
    sound, path = original(src)
    note = f"{SCRIPT} (production audio) from the Freesound original {sound} {path.name}"
    x = decode(path)
    with tempfile.TemporaryDirectory() as tmp:
        if np.max(np.abs(x)) > 1:
            path = Path(tmp) / "clipped.wav"
            write_wav(path, np.clip(x, -1, 1))
        out = process(src, path)
    write_ogg(OUT / f"{name}.ogg", out, max_peak_db=src["peak"], tags={"SOURCE": note})
    print(f"wrote assets/sfx/{name}.ogg  <- {path.name}")


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
