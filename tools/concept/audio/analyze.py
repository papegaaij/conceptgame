#!/usr/bin/env python3
"""Objective checks for generated audio (we cannot listen, so we measure).

Usage: python3 tools/concept/audio/analyze.py FILE... [--spectrogram OUTDIR]

Prints per file: duration, sample peak (dBFS), RMS (dBFS), DC offset, loudness (LUFS),
true peak (dBTP), and the largest sample in the first/last 5 ms (click check).
With --spectrogram, writes a log-frequency spectrogram PNG per file into OUTDIR
(keep those out of the repo; they are for inspection only).
"""
import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
from synth import SR, db, decode, lufs  # noqa: E402


def spectrogram(x, out_png, height=256, width=900):
    from PIL import Image
    nfft = 2048
    mono = np.pad(x.mean(axis=0), (0, max(0, nfft + 1 - x.shape[1])))  # very short UI ticks
    hop = max(64, len(mono) // width)
    win = np.hanning(nfft)
    frames = [mono[i:i + nfft] * win for i in range(0, max(1, len(mono) - nfft), hop)]
    spec = db(np.abs(np.fft.rfft(np.array(frames), axis=1)) + 1e-9).T  # (freq, time)
    freqs = np.fft.rfftfreq(nfft, 1 / SR)
    rows = np.geomspace(30, SR / 2, height)
    idx = np.clip(np.searchsorted(freqs, rows), 0, len(freqs) - 1)
    img = spec[idx][::-1]
    img = np.clip((img - (img.max() - 90)) / 90, 0, 1)
    rgb = np.stack([img ** 0.6, img ** 1.5, 0.3 + 0.7 * img ** 3], axis=-1)
    Image.fromarray((rgb * 255).astype(np.uint8)).resize((width, height)).save(out_png)


def main(argv):
    out_dir = None
    if "--spectrogram" in argv:
        i = argv.index("--spectrogram")
        out_dir = Path(argv[i + 1])
        out_dir.mkdir(parents=True, exist_ok=True)
        argv = argv[:i] + argv[i + 2:]
    print(f"{'file':34} {'dur':>6} {'peak':>6} {'rms':>6} {'dc':>8} {'lufs':>6} "
          f"{'tp':>6} {'head':>6} {'tail':>6}")
    for f in argv:
        x = decode(f)
        edge = int(0.005 * SR)
        loud, tp = lufs(x) if x.shape[1] > SR * 0.4 else (float("nan"), float("nan"))
        print(f"{Path(f).name:34} {x.shape[1] / SR:6.2f} {db(np.abs(x).max()):6.1f} "
              f"{db(np.sqrt(np.mean(x ** 2))):6.1f} {np.mean(x):8.5f} {loud:6.1f} {tp:6.1f} "
              f"{db(np.abs(x[:, :edge]).max()):6.1f} {db(np.abs(x[:, -edge:]).max()):6.1f}")
        if out_dir:
            spectrogram(x, out_dir / (Path(f).stem + ".png"))


if __name__ == "__main__":
    main(sys.argv[1:])
