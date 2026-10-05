#!/usr/bin/env python3
"""Concept round 23: the Daedalus perimeter beacon audition (Level 06, M4 part F, user decision D7).

Outputs (design/audio/voice/concept/):
  voice-perimeter-beacon-r23-<a|b>.ogg   the beacon's automated message from Level 06's radio
                                         chatter ("...Daedalus perimeter. All residents report to
                                         shelter... all residents report..."), spoken by Chatterbox
                                         with candidate reference clip a or b, through the
                                         public-address filter (pa() of tts_r18.py, next to its
                                         radio filter), OGG Vorbis q4, 44.1 kHz mono, -16 LUFS.

Reference clips: design/audio/voice/refs/ref-perimeter-beacon-r23-<a|b>.wav (Git LFS, both in
CREDITS.md), 12 s cuts from LibriVox chapter recordings (CC0 / public domain, sources in REFS),
made as in rounds 19 and 21 with
    ffmpeg -ss 60 -t 12 -i https://archive.org/download/<item>/<file> -ac 1 -ar 24000 <clip>.wav
and checked with their Whisper (faster-whisper base.en) transcript, the .txt next to each clip.
When the round closes, the chosen clip becomes ref-perimeter-beacon.wav and the other is deleted.

Setup: the Chatterbox venv of round 18 (~/.cache/tv-tts/venv-chatterbox, see tts_r18.py).
Rerun: python3 tools/concept/audio/tts_r23.py            (generate + post-process)
       python3 tools/concept/audio/tts_r23.py --post     (post-process only)
Raw WAVs go to ~/.cache/tv-tts/raw/r23/. Seeds are fixed, but GPU sampling is not bit-exact.
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
RAW = BASE / "raw" / "r23"

# An automated announcement: flat and even, little emotion.
AUTOMATED = {"exaggeration": 0.3, "cfg_weight": 0.5, "temperature": 0.6}
SPEAKER = "perimeter-beacon"
# Level 06's t=50 line (design/campaign/act-1-first-contact/level-06-farside/README.md, Radio
# chatter); the leading ellipsis (the loop is picked up mid-message) is not spoken.
TEXT = "Daedalus perimeter. All residents report to shelter... All residents report..."

# variant: (archive.org item, chapter file, cut start s, reader, licence)
REFS = {
    "a": ("time_machine_ms_librivox", "timemachine_01_wells_64kb.mp3", 60, "Mark F. Smith", "CC0"),
    "b": ("madame_midas_lb_librivox", "madamemidas_01_hume_64kb.mp3", 60, "Lucy Burgoyne", "PD"),
}
SEED = 23


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
        wav = model.generate(TEXT, audio_prompt_path=str(REFDIR / f"ref-{SPEAKER}-r23-{variant}.wav"),
                             **AUTOMATED)
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
        y = tts_r18.pa(x)
        tags = {"COMMENT": f"tools/concept/audio/tts_r23.py {SPEAKER} {variant}"}
        s.write_ogg(DEST / f"voice-{SPEAKER}-r23-{variant}.ogg", y, quality=4, tags=tags)
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
