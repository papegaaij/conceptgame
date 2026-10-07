#!/usr/bin/env python3
"""Concept round 30: the Ikoyi shelter civilian's audition (Level 08, M5 part B).

Outputs (design/audio/voice/concept/):
  voice-civilian-r30-<a|b>.ogg   both of the Civilian's lines from Level 08's radio chatter, t=113
                                 ("Shelter nine, Ikoyi! Walkers on the roofs, heading our way!")
                                 and the secondary objective's line ("Shelter nine here. The
                                 roofs are quiet. Thank you, Aegis."), 1 s apart, each spoken by
                                 Chatterbox with candidate reference clip a or b and put through
                                 radio filter b (radio() of tts_r18.py, its default: its own clicks
                                 and -16 LUFS); OGG Vorbis q4, 44.1 kHz mono; the neutral settings
                                 of round 19, as the game would render them (data.yaml's
                                 `expressions.neutral`).

Reference clips: design/audio/voice/refs/ref-civilian-r30-<a|b>.wav (Git LFS, both in CREDITS.md),
12 s cuts from LibriVox chapter recordings (public domain, sources in REFS; licences checked on each
archive.org item's `licenseurl`), made as in rounds 19, 21, 23 and 25 with
    ffmpeg -ss 60 -t 12 -i https://archive.org/download/<item>/<file> -ac 1 -ar 24000 <clip>.wav
and checked with their Whisper (faster-whisper base.en) transcript, the .txt next to each clip.
Round 30 closed (2026-10-07): b was cast and its clip renamed ref-civilian.wav; a's was deleted
(the command above with REFS's entry cuts it again, to rerun the audition), and a's audition was
moved to design/audio/voice/concept/rejected/ (a rerun writes it to concept/ again).

Setup: the Chatterbox venv of round 18 (~/.cache/tv-tts/venv-chatterbox, see tts_r18.py).
Rerun: python3 tools/concept/audio/tts_r30.py            (generate + post-process)
       python3 tools/concept/audio/tts_r30.py --post     (post-process only)
Raw WAVs go to ~/.cache/tv-tts/raw/r30/. Seeds are fixed, but GPU sampling is not bit-exact.
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
RAW = BASE / "raw" / "r30"

NEUTRAL = {"exaggeration": 0.5, "cfg_weight": 0.5, "temperature": 0.7}
SPEAKER = "civilian"
# Level 08's Civilian lines (design/campaign/act-2-homefront/level-08-neon-skyline/README.md, Radio
# chatter): t=113 and the secondary objective's line. Re-rendered on 2026-10-07 with the t=113
# line's current wording (the first renders read "Aegis, this is shelter nine in Ikoyi! They're on
# the roofs above us!").
LINES = [
    "Shelter nine, Ikoyi! Walkers on the roofs, heading our way!",
    "Shelter nine here. The roofs are quiet. Thank you, Aegis.",
]
GAP = 1.0  # s of silence between the two filtered lines

# variant: (archive.org item, chapter file, cut start s, reader, licence)
REFS = {
    "a": ("africanmyths_2601_librivox", "africanmyths_01_woodson_64kb.mp3", 60, "KirksVoice",
          "Public Domain Mark 1.0"),
    "b": ("yorubapeoples_2408_librivox", "yoruba_01_ellis_64kb.mp3", 60, "Faith Abiola-Ellison",
          "Public Domain Mark 1.0"),
}
SEED = 30


def worker():
    import random

    import numpy as np
    import soundfile as sf
    import torch
    from chatterbox.tts import ChatterboxTTS
    model = ChatterboxTTS.from_pretrained(device="cuda")
    RAW.mkdir(parents=True, exist_ok=True)
    for i, variant in enumerate(REFS):
        for j, text in enumerate(LINES):
            for f in (random.seed, np.random.seed, torch.manual_seed):
                f(SEED + 10 * i + j)
            wav = model.generate(text, audio_prompt_path=str(REFDIR / f"ref-{SPEAKER}-r30-{variant}.wav"),
                                 **NEUTRAL)
            wav = wav.cpu().numpy().reshape(-1)
            sf.write(str(RAW / f"{SPEAKER}-{variant}-{j + 1}.wav"), wav.astype("float32"), model.sr)
            print(SPEAKER, variant, j + 1, round(len(wav) / model.sr, 2), "s", flush=True)


def post():
    import numpy as np
    sys.path.insert(0, str(HERE))
    import tts_r18
    s = tts_r18._synth()
    DEST.mkdir(parents=True, exist_ok=True)
    for variant in REFS:
        parts = []
        for j in range(len(LINES)):
            x = s.decode(RAW / f"{SPEAKER}-{variant}-{j + 1}.wav")[0]
            parts += [tts_r18.radio(x), np.zeros((1, int(GAP * s.SR)))]  # filter b
        y = np.concatenate(parts[:-1], axis=1)
        tags = {"COMMENT": f"tools/concept/audio/tts_r30.py {SPEAKER} {variant}"}
        s.write_ogg(DEST / f"voice-{SPEAKER}-r30-{variant}.ogg", y, quality=4, tags=tags)
        print(SPEAKER, variant, round(y.shape[1] / s.SR, 2), "s", round(s.lufs(y)[0], 1), "LUFS", flush=True)


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
