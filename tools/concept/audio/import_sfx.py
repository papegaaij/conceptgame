#!/usr/bin/env python3
"""Import third-party sound effects (CC0 / CC-BY) into the concept directories.

Every entry in SOURCES maps one Freesound sound to one concept file: the public HQ preview is
downloaded into a cache directory outside the repository, the leading silence is trimmed, the
sound is cut to `length` seconds with a `fade` second fade-out, peak-normalised to `peak` dBFS
and written as 44.1 kHz OGG Vorbis. Licences and credits are recorded in CREDITS.md.

Optional treatments:
  loop=(start, length, xfade[, curve])
                               cut a seamless loop (for continuous beams): the `xfade` seconds
                               after the loop end are cross-faded into its start; no fades are
                               applied, so the file can be played looped. curve "power"
                               (default, equal-power, for uncorrelated noise) or "auto" (linear
                               when the two overlapping parts correlate > 0.5, e.g. tonal hums,
                               which would otherwise bulge by up to +3 dB at the seam).
  band_rms=dB                  normalise the 200 Hz-5 kHz band RMS to this level instead of the
                               peak, with `peak` as ceiling; used for sustained sounds so their
                               audible loudness matches the shots (round 03 beams were
                               peak-normalised but almost all sub-bass, hence inaudible).
  rejected=True                the user rejected the file; it is written to concept/rejected/.
  lowpass=Hz                   4-pole low-pass (two cascaded 2-pole SVFs) applied after the
                               cut, e.g. to derive a muffled under-water variant.

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
from synth import SR, db, decode, normalize_peak, svf, write_ogg  # noqa: E402

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
    # ---- concept round 03: per-weapon shot families (shot-<family>) and the explosion ladder
    # (explosion-<size>); see design/audio/sfx/README.md for the weapon/enemy mapping.
    "shot-vulcan-r03-b": dict(
        page="https://freesound.org/people/pgi/sounds/98331/",
        preview="https://cdn.freesound.org/previews/98/98331_1654571-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.30, fade=0.12, peak=-10.0),
    "shot-laser-r03-b": dict(
        page="https://freesound.org/people/michael_grinnell/sounds/512469/",
        preview="https://cdn.freesound.org/previews/512/512469_7372230-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.30, fade=0.14, peak=-10.0),
    "shot-beam-r03-a": dict(
        page="https://freesound.org/people/deleted_user_1941307/sounds/152322/",
        preview="https://cdn.freesound.org/previews/152/152322_1941307-hq.ogg",
        licence="CC0 1.0", loop=(0.20, 1.40, 0.10), peak=-12.0, rejected=True),
    "shot-beam-r03-b": dict(
        page="https://freesound.org/people/bolkmar/sounds/420364/",
        preview="https://cdn.freesound.org/previews/420/420364_2927958-hq.ogg",
        licence="CC-BY 4.0", loop=(0.40, 2.62, 0.08), peak=-12.0, rejected=True),
    "shot-missile-r03-a": dict(
        page="https://freesound.org/people/Jarusca/sounds/521377/",
        preview="https://cdn.freesound.org/previews/521/521377_10847299-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.90, fade=0.40, peak=-8.0),
    "shot-micromissile-r03-a": dict(
        page="https://freesound.org/people/Audionautics/sounds/171655/",
        preview="https://cdn.freesound.org/previews/171/171655_2451120-hq.ogg",
        licence="CC-BY 3.0", offset=0.0, length=0.45, fade=0.20, peak=-10.0),
    "shot-mortar-r03-a": dict(
        page="https://freesound.org/people/qubodup/sounds/184382/",
        preview="https://cdn.freesound.org/previews/184/184382_71257-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=1.00, fade=0.50, peak=-8.0),
    "shot-bomb-r03-a": dict(
        page="https://freesound.org/people/Daleonfire/sounds/506313/",
        preview="https://cdn.freesound.org/previews/506/506313_150886-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=1.20, fade=0.60, peak=-10.0),
    "shot-torpedo-r03-a": dict(
        page="https://freesound.org/people/jobro/sounds/35530/",
        preview="https://cdn.freesound.org/previews/35/35530_35187-hq.ogg",
        licence="CC-BY 3.0", offset=0.0, length=1.00, fade=0.50, peak=-8.0),
    "shot-mine-r03-a": dict(
        page="https://freesound.org/people/nicktermer/sounds/259553/",
        preview="https://cdn.freesound.org/previews/259/259553_2316086-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.90, fade=0.30, peak=-8.0),
    "shot-tesla-r03-a": dict(
        page="https://freesound.org/people/michael_grinnell/sounds/512471/",
        preview="https://cdn.freesound.org/previews/512/512471_7372230-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.22, fade=0.06, peak=-10.0),
    "shot-resonator-r03-a": dict(
        page="https://freesound.org/people/humanoide9000/sounds/422440/",
        preview="https://cdn.freesound.org/previews/422/422440_4361321-hq.ogg",
        licence="CC-BY 4.0", offset=0.0, length=0.60, fade=0.30, peak=-8.0),
    "explosion-tiny-r03-a": dict(
        page="https://freesound.org/people/Cyberios/sounds/145788/",
        preview="https://cdn.freesound.org/previews/145/145788_2483826-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.45, fade=0.20, peak=-4.0),
    "explosion-tiny-r03-b": dict(
        page="https://freesound.org/people/dinodilopho/sounds/328833/",
        preview="https://cdn.freesound.org/previews/328/328833_4732572-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.60, fade=0.30, peak=-4.0),
    "explosion-tiny-r03-c": dict(
        page="https://freesound.org/people/unfa/sounds/609588/",
        preview="https://cdn.freesound.org/previews/609/609588_1038806-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=0.60, fade=0.30, peak=-4.0, rejected=True),
    "explosion-small-r03-a": dict(
        page="https://freesound.org/people/lorenzgillner/sounds/271979/",
        preview="https://cdn.freesound.org/previews/271/271979_5169846-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=1.10, fade=0.40, peak=-1.5),
    "explosion-medium-r03-a": dict(
        page="https://freesound.org/people/1histori/sounds/401609/",
        preview="https://cdn.freesound.org/previews/401/401609_3767503-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=1.60, fade=0.60, peak=-1.5, rejected=True),
    "explosion-medium-r03-b": dict(
        page="https://freesound.org/people/mitchelk/sounds/136765/",
        preview="https://cdn.freesound.org/previews/136/136765_2482480-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=2.00, fade=0.80, peak=-1.5),
    "explosion-large-r03-a": dict(
        page="https://freesound.org/people/derplayer/sounds/587193/",
        preview="https://cdn.freesound.org/previews/587/587193_13123807-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=3.10, fade=1.00, peak=-1.5),
    "explosion-huge-r03-a": dict(
        page="https://freesound.org/people/tommccann/sounds/235968/",
        preview="https://cdn.freesound.org/previews/235/235968_4265427-hq.ogg",
        licence="CC0 1.0", offset=0.36, length=5.00, fade=2.00, peak=-1.0, rejected=True),
    "explosion-huge-r03-b": dict(
        page="https://freesound.org/people/unfa/sounds/189779/",
        preview="https://cdn.freesound.org/previews/189/189779_1038806-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=6.00, fade=2.50, peak=-1.0),
    "explosion-underwater-r03-a": dict(
        page="https://freesound.org/people/cubix/sounds/124544/",
        preview="https://cdn.freesound.org/previews/124/124544_276157-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=3.00, fade=1.20, peak=-1.5),
    "explosion-underwater-r03-b": dict(
        page="https://freesound.org/people/qubodup/sounds/182429/",
        preview="https://cdn.freesound.org/previews/182/182429_71257-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=1.70, fade=0.60, peak=-1.5, lowpass=500, rejected=True),
    "explosion-water-r03-a": dict(
        page="https://freesound.org/people/Sheyvan/sounds/519008/",
        preview="https://cdn.freesound.org/previews/519/519008_3248005-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=1.90, fade=0.70, peak=-1.5),
    # ---- concept round 04: audible beam loops (+ start/stop), Sonar Pulse, extra huge and
    # under-water explosions. Beams are normalised on their 200 Hz-5 kHz band RMS (see band_rms).
    "shot-beam-r04-a": dict(
        page="https://freesound.org/people/peepholecircus/sounds/169991/",
        preview="https://cdn.freesound.org/previews/169/169991_2747497-hq.ogg",
        licence="CC0 1.0", loop=(5.60, 2.60, 0.12, "auto"), band_rms=-30.0, peak=-3.0),
    "shot-beam-r04-b": dict(
        page="https://freesound.org/people/unfa/sounds/584191/",
        preview="https://cdn.freesound.org/previews/584/584191_1038806-hq.ogg",
        licence="CC0 1.0", loop=(0.00, 1.90, 0.10, "auto"), band_rms=-30.0, peak=-3.0),
    "shot-beam-r04-c": dict(
        page="https://freesound.org/people/zimbot/sounds/177100/",
        preview="https://cdn.freesound.org/previews/177/177100_1449999-hq.ogg",
        licence="CC-BY 4.0", loop=(1.00, 2.00, 0.10, "auto"), band_rms=-30.0, peak=-3.0),
    "shot-beam-start-r04-a": dict(
        page="https://freesound.org/people/Glitchedtones/sounds/375925/",
        preview="https://cdn.freesound.org/previews/375/375925_3294528-hq.ogg",
        licence="CC0 1.0", offset=1.10, length=0.90, fade=0.15, band_rms=-30.0, peak=-3.0),
    "shot-beam-stop-r04-a": dict(
        page="https://freesound.org/people/noirenex/sounds/159399/",
        preview="https://cdn.freesound.org/previews/159/159399_1656228-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=1.30, fade=0.60, band_rms=-30.0, peak=-3.0),
    "special-sonar-r04-a": dict(
        page="https://freesound.org/people/SamsterBirdies/sounds/539957/",
        preview="https://cdn.freesound.org/previews/539/539957_5487341-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=3.00, fade=1.00, peak=-4.0),
    "special-sonar-r04-b": dict(
        page="https://freesound.org/people/unfa/sounds/215415/",
        preview="https://cdn.freesound.org/previews/215/215415_1038806-hq.ogg",
        licence="CC0 1.0", offset=0.0, length=3.50, fade=1.50, peak=-4.0),
    "explosion-huge-r04-a": dict(
        page="https://freesound.org/people/sidohzen/sounds/165808/",
        preview="https://cdn.freesound.org/previews/165/165808_2872744-hq.ogg",
        licence="CC0 1.0", offset=1.63, length=6.00, fade=2.50, peak=-1.0),
    "explosion-underwater-r04-a": dict(
        page="https://freesound.org/people/mokasza/sounds/810765/",
        preview="https://cdn.freesound.org/previews/810/810765_17437502-hq.ogg",
        licence="CC-BY 4.0", offset=0.0, length=3.50, fade=1.20, peak=-1.5),
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


def make_loop(x, start, length, xfade, curve="power"):
    a, n, f = int(start * SR), int(length * SR), int(xfade * SR)
    seg = x[:, a:a + n + f].copy()
    head, tail = seg[:, :f].copy(), seg[:, n:n + f]
    linear = curve == "auto" and np.corrcoef(head.ravel(), tail.ravel())[0, 1] > 0.5
    if linear:
        fin = np.linspace(0, 1, f)
        fout = 1 - fin
    else:
        t = np.linspace(0, np.pi / 2, f)
        fin, fout = np.sin(t), np.cos(t)
    seg[:, :f] = head * fin + tail * fout
    return seg[:, :n]


def band_rms_db(x, lo=200.0, hi=5000.0):
    mono = x.mean(axis=0)
    spec = np.fft.rfft(mono)
    freqs = np.fft.rfftfreq(len(mono), 1 / SR)
    spec[(freqs < lo) | (freqs >= hi)] = 0
    return db(np.sqrt(np.mean(np.fft.irfft(spec, len(mono)) ** 2)) + 1e-12)


def normalize(x, src):
    if "band_rms" not in src:
        return normalize_peak(x, src["peak"])
    y = x * 10 ** ((src["band_rms"] - band_rms_db(x)) / 20)
    ceiling = 10 ** (src["peak"] / 20)
    return y * min(1.0, ceiling / np.max(np.abs(y)))


def process(src, raw):
    x = trim_leading_silence(decode(raw))
    if "loop" in src:
        x = make_loop(x, *src["loop"])
        x -= x.mean(axis=1, keepdims=True)
        return normalize(x, src)
    start = int(src["offset"] * SR)
    x = x[:, start:start + int(src["length"] * SR)].copy()
    x -= x.mean(axis=1, keepdims=True)  # remove DC
    if "lowpass" in src:
        x = np.array([svf(svf(ch, src["lowpass"]), src["lowpass"]) for ch in x])
    n_in, n_out = int(0.002 * SR), min(x.shape[1], int(src["fade"] * SR))
    x[:, :n_in] *= np.linspace(0, 1, n_in)
    x[:, -n_out:] *= (0.5 + 0.5 * np.cos(np.linspace(0, np.pi, n_out))) ** 2
    return normalize(x, src)


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
        out_dir = SFX / "rejected" if src.get("rejected") else SFX
        out = write_ogg(out_dir / f"{name}.ogg", process(src, fetch(src["preview"], cache)),
                        max_peak_db=src["peak"])
        print(f"wrote {out.relative_to(ROOT)}  <- {src['page']} ({src['licence']})")


if __name__ == "__main__":
    main(sys.argv[1:])
