#!/usr/bin/env python3
"""Import third-party sound effects (CC0 / CC-BY) into the concept directories.

Every entry in SOURCES maps one Freesound sound to one concept file: the public HQ preview is
downloaded into a cache directory outside the repository, the leading silence is trimmed, the
sound is cut to `length` seconds with a `fade` second fade-out, peak-normalised to `peak` dBFS
and written as 44.1 kHz OGG Vorbis. Licences and credits are recorded in CREDITS.md.

The previews are lossy (~192 kbps); the production asset should be rebuilt from the original
file (Freesound login required) with the same settings.

Usage: python3 tools/concept/audio/import_sfx.py [--cache DIR] [name ...]
       (default cache: $TMPDIR/conceptgame-sfx-cache; names select entries, e.g. explosion-r02-d)
"""
import os
import subprocess
import sys
import tempfile
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
from synth import SR, db, decode, normalize_peak, write_ogg  # noqa: E402

ROOT = Path(__file__).resolve().parents[3]
SFX = ROOT / "design" / "audio" / "sfx" / "concept"

# name: source page, preview url, licence, offset into the (silence-trimmed) sound, length,
# fade-out, peak dBFS. Shots peak at -10 dBFS (they repeat constantly), explosions at -1.5.
SOURCES = {
    "player-shot-r02-a": dict(
        page="https://freesound.org/people/unfa/sounds/193427/",
        preview="https://cdn.freesound.org/previews/193/193427_1038806-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.25, fade=0.12, peak=-10.0),
    "player-shot-r02-b": dict(
        page="https://freesound.org/people/Bird_man/sounds/317136/",
        preview="https://cdn.freesound.org/previews/317/317136_4745081-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.28, fade=0.14, peak=-10.0),
    "player-shot-r02-c": dict(
        page="https://freesound.org/people/nsstudios/sounds/344276/",
        preview="https://cdn.freesound.org/previews/344/344276_2776777-hq.ogg",
        licence="CC-BY 4.0", offset=0.0, length=0.27, fade=0.10, peak=-10.0),
    "player-shot-r02-d": dict(
        page="https://freesound.org/people/pgi/sounds/212601/",
        preview="https://cdn.freesound.org/previews/212/212601_1654571-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.28, fade=0.16, peak=-10.0),
    "player-shot-r02-e": dict(
        page="https://freesound.org/people/qubodup/sounds/854186/",
        preview="https://cdn.freesound.org/previews/854/854186_71257-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.26, fade=0.12, peak=-10.0),  # first shot of the burst
    "explosion-r02-a": dict(
        page="https://freesound.org/people/bevibeldesign/sounds/315826/",
        preview="https://cdn.freesound.org/previews/315/315826_4557960-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.70, fade=0.30, peak=-1.5),
    "explosion-r02-b": dict(
        page="https://freesound.org/people/magnuswaker/sounds/523089/",
        preview="https://cdn.freesound.org/previews/523/523089_11537497-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.90, fade=0.40, peak=-1.5),
    "explosion-r02-c": dict(
        page="https://freesound.org/people/qubodup/sounds/182429/",
        preview="https://cdn.freesound.org/previews/182/182429_71257-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=1.70, fade=0.50, peak=-1.5),
    "explosion-r02-d": dict(
        page="https://freesound.org/people/juskiddink/sounds/108641/",
        preview="https://cdn.freesound.org/previews/108/108641_649468-hq.ogg",
        licence="CC-BY 4.0", offset=0.0, length=2.80, fade=0.90, peak=-1.5),
    "explosion-r02-e": dict(
        page="https://freesound.org/people/derplayer/sounds/587194/",
        preview="https://cdn.freesound.org/previews/587/587194_13123807-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=2.86, fade=0.80, peak=-1.5),
}


def fetch(url, cache):
    out = cache / url.rsplit("/", 1)[1]
    if not out.exists():
        subprocess.run(["curl", "-sS", "-L", "-f", "-A", "Mozilla/5.0", "-o", str(out), url],
                       check=True)
    return out


def trim_leading_silence(x, threshold_db=-40.0, pre=0.003):
    level = db(np.max(np.abs(x), axis=0) + 1e-12) - db(np.max(np.abs(x)))
    first = int(np.argmax(level > threshold_db))
    return x[:, max(0, first - int(pre * SR)):]


def process(src, raw):
    x = trim_leading_silence(decode(raw))
    start = int(src["offset"] * SR)
    x = x[:, start:start + int(src["length"] * SR)].copy()
    x -= x.mean(axis=1, keepdims=True)  # remove DC
    n_in, n_out = int(0.002 * SR), min(x.shape[1], int(src["fade"] * SR))
    x[:, :n_in] *= np.linspace(0, 1, n_in)
    x[:, -n_out:] *= (0.5 + 0.5 * np.cos(np.linspace(0, np.pi, n_out))) ** 2
    return normalize_peak(x, src["peak"])


def main(argv):
    cache = Path(os.environ.get("TMPDIR", tempfile.gettempdir())) / "conceptgame-sfx-cache"
    if "--cache" in argv:
        i = argv.index("--cache")
        cache = Path(argv[i + 1])
        del argv[i:i + 2]
    cache.mkdir(parents=True, exist_ok=True)
    for name, src in SOURCES.items():
        if argv and name not in argv:
            continue
        out = write_ogg(SFX / f"{name}.ogg", process(src, fetch(src["preview"], cache)),
                        max_peak_db=src["peak"])
        print(f"wrote {out.relative_to(ROOT)}  <- {src['page']} ({src['licence']})")


if __name__ == "__main__":
    main(sys.argv[1:])
