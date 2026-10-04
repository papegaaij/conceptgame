#!/usr/bin/env python3
"""Concept round 21: the Driver Control audition (Level 05, M4 part E, user decision D4).

Outputs (design/audio/voice/concept/):
  voice-driver-control-r21-<a|b>.ogg   Driver Control's warning from Level 05's data ("Mass
                                       driver's still on automatic, Aegis. Sleds every five
                                       seconds. Watch the rail lights."), spoken by Chatterbox with
                                       candidate reference clip a or b, through radio filter b
                                       (radio() of tts_r18.py, its default), OGG Vorbis q4,
                                       44.1 kHz mono, -16 LUFS; settings as round 19's.

Reference clips: design/audio/voice/refs/ref-driver-control-r21-<a|b>.wav (Git LFS, both in
CREDITS.md), 12 s cuts from LibriVox chapter recordings (public domain, sources in REFS), made as
in round 19 with
    ffmpeg -ss 60 -t 12 -i https://archive.org/download/<item>/<file> -ac 1 -ar 24000 <clip>.wav
and checked with their Whisper (faster-whisper base.en) transcript, the .txt next to each clip.
When the round closes, the chosen clip becomes ref-driver-control.wav and the other is deleted.

Setup: the Chatterbox venv of round 18 (~/.cache/tv-tts/venv-chatterbox, see tts_r18.py).
Rerun: python3 tools/concept/audio/tts_r21.py            (generate + post-process)
       python3 tools/concept/audio/tts_r21.py --post     (post-process only)
Raw WAVs go to ~/.cache/tv-tts/raw/r21/. Seeds are fixed, but GPU sampling is not bit-exact.
"""
import os
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
REFDIR = ROOT / "design" / "audio" / "voice" / "refs"
DEST = ROOT / "design" / "audio" / "voice" / "concept"
BASE = Path.home() / ".cache" / "tv-tts"
RAW = BASE / "raw" / "r21"

FIERCE = {"exaggeration": 0.8, "cfg_weight": 0.4, "temperature": 0.8}
SPEAKER = "driver-control"
# Level 05's t=12.5 line (design/campaign/act-1-first-contact/level-05-crater-nest/data.yaml).
TEXT = "Mass driver's still on automatic, Aegis. Sleds every five seconds. Watch the rail lights."

# variant: (archive.org item, chapter file, cut start s, reader, licence)
REFS = {
    "a": ("invisible_man_librivox", "invisible_man_01-02_wells_64kb.mp3", 60, "Alex Foster", "PD"),
    "b": ("war_worlds_solo_librivox", "warofworlds_b1_ch01_wells_64kb.mp3", 60, "Rebecca", "PD"),
}
SEED = 21


def worker():
    import random

    import numpy as np
    import soundfile as sf
    import torch
    from chatterbox.tts import ChatterboxTTS
    model = ChatterboxTTS.from_pretrained(device="cuda")
    RAW.mkdir(parents=True, exist_ok=True)
    for i, variant in enumerate(REFS):
        for f in (random.seed, np.random.seed, torch.manual_seed):
            f(SEED + i)
        wav = model.generate(TEXT, audio_prompt_path=str(REFDIR / f"ref-{SPEAKER}-r21-{variant}.wav"),
                             **FIERCE)
        wav = wav.cpu().numpy().reshape(-1)
        sf.write(str(RAW / f"{SPEAKER}-{variant}.wav"), wav.astype("float32"), model.sr)
        print(SPEAKER, variant, round(len(wav) / model.sr, 2), "s", flush=True)


def post():
    sys.path.insert(0, str(HERE))
    import tts_r18
    s = tts_r18._synth()
    DEST.mkdir(parents=True, exist_ok=True)
    for variant in REFS:
        x = s.decode(RAW / f"{SPEAKER}-{variant}.wav")[0]
        y = tts_r18.radio(x)  # filter b
        tags = {"COMMENT": f"tools/concept/audio/tts_r21.py {SPEAKER} {variant}"}
        s.write_ogg(DEST / f"voice-{SPEAKER}-r21-{variant}.ogg", y, quality=4, tags=tags)
        print(SPEAKER, variant, round(s.lufs(y)[0], 1), "LUFS", flush=True)


def main(args):
    if args[:1] == ["--worker"]:
        worker()
        return
    if args[:1] != ["--post"]:
        env = dict(os.environ, HF_HOME=str(BASE / "hf"), TOKENIZERS_PARALLELISM="false",
                   TORCH_FORCE_NO_WEIGHTS_ONLY_LOAD="1")
        py = BASE / "venv-chatterbox" / "bin" / "python"
        subprocess.run([str(py), __file__, "--worker"], env=env, check=True)
    post()


if __name__ == "__main__":
    main(sys.argv[1:])
