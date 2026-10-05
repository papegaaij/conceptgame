#!/usr/bin/env python3
"""Concept round 25: the Lifeboat Seven audition (Level 07, M4 part G).

Outputs (design/audio/voice/concept/):
  voice-lifeboat-seven-r25-<a|b>.ogg   Lifeboat Seven's t=22 line from Level 07's radio chatter
                                       ("Lifeboat Seven, crew of four, drifting. Aegis, that pod
                                       we're towing is yours if you cut it loose."), spoken by
                                       Chatterbox with candidate reference clip a or b, through
                                       radio filter b (radio() of tts_r18.py, its default), OGG
                                       Vorbis q4, 44.1 kHz mono, -16 LUFS; the neutral settings of
                                       round 19 (a stranded crew calling in, tired, not panicking).

Reference clips: design/audio/voice/refs/ref-lifeboat-seven-r25-<a|b>.wav (Git LFS, both in
CREDITS.md), 12 s cuts from LibriVox chapter recordings (public domain, sources in REFS; licences
checked on each archive.org item's `licenseurl`), made as in rounds 19, 21 and 23 with
    ffmpeg -ss 60 -t 12 -i https://archive.org/download/<item>/<file> -ac 1 -ar 24000 <clip>.wav
and checked with their Whisper (faster-whisper base.en) transcript, the .txt next to each clip.
Round 25 closed (2026-10-05): a was cast and its clip renamed ref-lifeboat-seven.wav; b's was deleted
(the command above with REFS's entry cuts it again, to rerun the audition).

Setup: the Chatterbox venv of round 18 (~/.cache/tv-tts/venv-chatterbox, see tts_r18.py).
Rerun: python3 tools/concept/audio/tts_r25.py            (generate + post-process)
       python3 tools/concept/audio/tts_r25.py --post     (post-process only)
Raw WAVs go to ~/.cache/tv-tts/raw/r25/. Seeds are fixed, but GPU sampling is not bit-exact.
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
RAW = BASE / "raw" / "r25"

NEUTRAL = {"exaggeration": 0.5, "cfg_weight": 0.5, "temperature": 0.7}
SPEAKER = "lifeboat-seven"
# Level 07's t=22 line (design/campaign/act-1-first-contact/level-07-brood-carrier/README.md, Radio
# chatter).
TEXT = "Lifeboat Seven, crew of four, drifting. Aegis, that pod we're towing is yours if you cut it loose."

# variant: (archive.org item, chapter file, cut start s, reader, licence)
REFS = {
    "a": ("far_from_the_madding_crowd_th_librivox", "farfromthemaddingcrowd_01_thomashardy_64kb.mp3", 60,
          "Tadhg Hynes", "PD"),
    "b": ("gulliver_ld_librivox", "gulliverstravels_01_swift_64kb.mp3", 60, "Lizzie Driver", "PD"),
}
SEED = 25


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
        wav = model.generate(TEXT, audio_prompt_path=str(REFDIR / f"ref-{SPEAKER}-r25-{variant}.wav"),
                             **NEUTRAL)
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
        tags = {"COMMENT": f"tools/concept/audio/tts_r25.py {SPEAKER} {variant}"}
        s.write_ogg(DEST / f"voice-{SPEAKER}-r25-{variant}.ogg", y, quality=4, tags=tags)
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
