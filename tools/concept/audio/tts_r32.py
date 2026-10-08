#!/usr/bin/env python3
"""Concept round 32: the Lifeline and Lifeline Three auditions (Level 10, M5 part D, user decision
D12 = a).

Outputs (design/audio/voice/concept/):
  voice-lifeline-r32-<a|b>.ogg        Lifeline (Lifeline One's pilot, also the voice of Two, Four
                                      and Five and of the hit line): three of the lines from Level
                                      10's radio chatter, t=1 ("Eko Control, Lifeline One. Five
                                      birds, eleven hundred souls. We're going."), the first
                                      shuttle hit with {ally} = Two ("Lifeline Two, we're hit!
                                      Still flying. Please stay close!") and t=196 ("Corridor
                                      clear. We can see the sky. Thank you, Aegis. Thank you."),
                                      1 s apart, each through radio filter b on its own
  voice-lifeline-three-r32-<a|b>.ogg  Lifeline Three: its one line, t=116 ("Aegis, there's a light
                                      above the clouds. What is that—"), cut off by the lance: the
                                      take is cut hard where Whisper ends "that" (its word
                                      timing, which falls inside the word's tail: the word is
                                      clipped), a 0.3 s burst of static as loud as the voice
                                      follows and the channel goes dead (no closing click); through
                                      radio filter b
Both OGG Vorbis q4, 44.1 kHz mono; Chatterbox with candidate reference clip a or b at the neutral
settings of round 19, as the game would render them (data.yaml's `expressions.neutral`); the text as
spoken, as the game speaks it (the dash read as a comma, voice README).

Casting: Lifeline's candidates are male, Lifeline Three's female, so whichever pair is picked the
two shuttle pilots never sound alike; all four are higher or lower than the cast's nearest voices
(the pitches are in prompts.md) and none is in the cast or was auditioned before:
  Lifeline       a Atul Sharma (The Mysterious Aviator, Shute), b KevinS (Over the Ocean to Paris,
                 Dixon): readers of airline-age aviation novels
  Lifeline Three a Kehinde (Travels in West Africa, Kingsley), b Maria Kasper (The Curtiss Aviation
                 Book, Curtiss)

Reference clips: design/audio/voice/refs/ref-<speaker>-r32-<a|b>.wav (Git LFS, all in CREDITS.md),
12 s cuts from LibriVox chapter recordings (public domain, sources in REFS; licences checked on each
archive.org item's `licenseurl`), made as in rounds 19-31 with
    ffmpeg -ss 60 -t 12 -i https://archive.org/download/<item>/<file> -ac 1 -ar 24000 <clip>.wav
(`python3 tools/concept/audio/tts_r32.py --refs` cuts the missing ones) and checked with their
Whisper (faster-whisper base.en) transcript, the .txt next to each clip.

Setup: the Chatterbox venv of round 18 (~/.cache/tv-tts/venv-chatterbox, see tts_r18.py); Whisper
(faster-whisper) from the Dia venv (~/.cache/tv-tts/venv-dia), for the cut's word timing and the
read-back.
Rerun: python3 tools/concept/audio/tts_r32.py            (generate, time, post-process, read back)
       python3 tools/concept/audio/tts_r32.py --post     (post-process and read back only)
Raw WAVs and the word timings go to ~/.cache/tv-tts/raw/r32/. Seeds are fixed, but GPU sampling is
not bit-exact.
"""
import json
import os
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
REFDIR = ROOT / "design" / "audio" / "voice" / "refs"
DEST = ROOT / "design" / "audio" / "voice" / "concept"
BASE = Path.home() / ".cache" / "tv-tts"
RAW = BASE / "raw" / "r32"
WHISPER_PY = BASE / "venv-dia" / "bin" / "python"

NEUTRAL = {"exaggeration": 0.5, "cfg_weight": 0.5, "temperature": 0.7}
# Level 10's lines (design/campaign/act-2-homefront/level-10-evacuation-corridor/README.md, Radio
# chatter), as spoken: the hit line's {ally} as Two, Lifeline Three's dash read as a comma.
LINES = {
    "lifeline": [
        "Eko Control, Lifeline One. Five birds, eleven hundred souls. We're going.",
        "Lifeline Two, we're hit! Still flying. Please stay close!",
        "Corridor clear. We can see the sky. Thank you, Aegis. Thank you.",
    ],
    "lifeline-three": [
        "Aegis, there's a light above the clouds. What is that,",
    ],
}
CUT_WORD = {"lifeline-three": "that"}  # the speaker whose (last) line the lance cuts off, and where
GAP = 1.0      # s of silence between two filtered lines
STATIC = 0.3   # s of static after the cut
DEAD = 0.25    # s of dead channel (the static's tail) before the file ends

# speaker: variant: (archive.org item, chapter file, cut start s, reader, licence)
REFS = {
    "lifeline": {
        "a": ("mysteriousaviator_2510_librivox", "mysteriousaviator_01_shute_64kb.mp3", 60, "Atul Sharma",
              "Public Domain Mark 1.0"),
        "b": ("overtheoceantoparis_2404_librivox", "overtheoceantoparis_01_dixon_64kb.mp3", 60, "KevinS",
              "Public Domain Mark 1.0"),
    },
    "lifeline-three": {
        "a": ("travels_westafrica_0910_librivox", "travelsinwestafrica_01_kingsley_64kb.mp3", 60, "Kehinde",
              "public domain (LibriVox)"),
        "b": ("curtissaviationbook_2006_librivox", "curtissaviationbook_01_curtiss_64kb.mp3", 60, "Maria Kasper",
              "Public Domain Mark 1.0"),
    },
}
SEED = 32


def ref(speaker, variant):
    return REFDIR / f"ref-{speaker}-r32-{variant}.wav"


def refs():
    """Cut the missing reference clips and write their Whisper transcripts."""
    for speaker, variants in REFS.items():
        for variant, (item, name, start, _, _) in variants.items():
            out = ref(speaker, variant)
            if not out.exists():
                subprocess.run(["ffmpeg", "-nostdin", "-loglevel", "error", "-y", "-ss", str(start), "-t", "12",
                                "-i", f"https://archive.org/download/{item}/{name}", "-ac", "1", "-ar", "24000",
                                str(out)], check=True)
    run_whisper("--transcribe-refs")


def worker():
    import random

    import numpy as np
    import soundfile as sf
    import torch
    from chatterbox.tts import ChatterboxTTS
    model = ChatterboxTTS.from_pretrained(device="cuda")
    RAW.mkdir(parents=True, exist_ok=True)
    for s, speaker in enumerate(LINES):
        for i, variant in enumerate(REFS[speaker]):
            for j, text in enumerate(LINES[speaker]):
                for f in (random.seed, np.random.seed, torch.manual_seed):
                    f(SEED + 100 * s + 10 * i + j)
                wav = model.generate(text, audio_prompt_path=str(ref(speaker, variant)), **NEUTRAL)
                wav = wav.cpu().numpy().reshape(-1)
                sf.write(str(RAW / f"{speaker}-{variant}-{j + 1}.wav"), wav.astype("float32"), model.sr)
                print(speaker, variant, j + 1, round(len(wav) / model.sr, 2), "s", flush=True)


def whisper(mode):
    """In the Dia venv: the reference transcripts (.txt), the raw takes' word timings
    (RAW/timings.json) or the finished files' read-back."""
    from faster_whisper import WhisperModel
    asr = WhisperModel("base.en", device="cpu", compute_type="int8")

    def words(path):
        segs, _ = asr.transcribe(str(path), language="en", word_timestamps=True)
        return [(w.word.strip(), round(w.start, 3), round(w.end, 3)) for seg in segs for w in seg.words]

    if mode == "--transcribe-refs":
        for speaker, variants in REFS.items():
            for variant in variants:
                text = " ".join(w for w, _, _ in words(ref(speaker, variant)))
                ref(speaker, variant).with_suffix(".txt").write_text(text + "\n")
                print(ref(speaker, variant).name, ":", text)
    elif mode == "--time":
        timings = {}
        for speaker in LINES:
            for variant in REFS[speaker]:
                for j in range(len(LINES[speaker])):
                    name = f"{speaker}-{variant}-{j + 1}"
                    timings[name] = words(RAW / f"{name}.wav")
                    print("raw", name, ":", " ".join(w for w, _, _ in timings[name]))
        (RAW / "timings.json").write_text(json.dumps(timings, indent=1))
    else:
        for speaker in LINES:
            for variant in REFS[speaker]:
                path = DEST / f"voice-{speaker}-r32-{variant}.ogg"
                print("read back", path.name, ":", " ".join(w for w, _, _ in words(path)))


def run_whisper(mode):
    subprocess.run([str(WHISPER_PY), __file__, "--whisper", mode], check=True,
                   env=dict(os.environ, HF_HOME=str(BASE / "hf")))


def cut_off(x, sr, end, rng, s):
    """The take cut hard at `end` s (a 3 ms fade, no tail), then STATIC s of static as loud as the
    voice's peak: hiss and dense crackle, on at once, breaking up, held 0.12 s and dying, then DEAD
    s of fading hiss."""
    import numpy as np
    x = x[:int(end * sr)].copy()
    m = int(0.003 * sr)
    x[-m:] *= np.linspace(1, 0, m)
    peak = np.max(np.abs(x))
    n = int(STATIC * sr)
    t = np.arange(n) / sr
    hiss = s.noise(n, rng) * 0.7
    crackle = (rng.random(n) < 0.03) * rng.uniform(-1, 1, n) * 0.3
    gate = (s.svf(rng.uniform(0, 1, n), 30.0) > 0.45).astype(float) * 0.5 + 0.5  # breaking up
    env = np.where(t < 0.12, 1.0, np.exp(-(t - 0.12) / 0.07))  # held 0.12 s, then dying
    burst = (hiss + crackle) * gate * env * peak
    tail = s.noise(int(DEAD * sr), rng) * peak * 0.05 * np.linspace(1, 0, int(DEAD * sr))
    return np.concatenate([x, burst, tail])


def post():
    import numpy as np
    sys.path.insert(0, str(HERE))
    import tts_r18
    s = tts_r18._synth()
    timings = json.loads((RAW / "timings.json").read_text())
    DEST.mkdir(parents=True, exist_ok=True)
    for speaker in LINES:
        for variant in REFS[speaker]:
            parts = []
            for j in range(len(LINES[speaker])):
                name = f"{speaker}-{variant}-{j + 1}"
                x = s.decode(RAW / f"{name}.wav")[0]
                last = j == len(LINES[speaker]) - 1
                if speaker in CUT_WORD and last:
                    hits = [end for w, _, end in timings[name] if w.strip(",.!?—").lower() == CUT_WORD[speaker]]
                    end = hits[-1] if hits else timings[name][-1][2]
                    y = tts_r18.radio(cut_off(x, s.SR, end, np.random.default_rng(SEED), s))
                    # the channel dies: no closing click, the file ends DEAD s after the static
                    y = y[:, :int((0.15 + end + STATIC + DEAD) * s.SR)]
                    y[:, -int(0.002 * s.SR):] *= np.linspace(1, 0, int(0.002 * s.SR))
                    print(speaker, variant, f"cut at {end:.2f} s of the take", flush=True)
                else:
                    y = tts_r18.radio(x)
                parts += [y, np.zeros((1, int(GAP * s.SR)))]  # filter b
            y = np.concatenate(parts[:-1], axis=1)
            tags = {"COMMENT": f"tools/concept/audio/tts_r32.py {speaker} {variant}"}
            s.write_ogg(DEST / f"voice-{speaker}-r32-{variant}.ogg", y, quality=4, tags=tags)
            print(speaker, variant, round(y.shape[1] / s.SR, 2), "s", round(s.lufs(y)[0], 1), "LUFS", flush=True)


def main(args):
    if args[:1] == ["--worker"]:
        worker()
        return
    if args[:1] == ["--whisper"]:
        whisper(args[1])
        return
    if args[:1] == ["--refs"]:
        refs()
        return
    if args[:1] != ["--post"]:
        env = dict(os.environ, HF_HOME=str(BASE / "hf"), TOKENIZERS_PARALLELISM="false",
                   TORCH_FORCE_NO_WEIGHTS_ONLY_LOAD="1")
        py = BASE / "venv-chatterbox" / "bin" / "python"
        subprocess.run([str(py), __file__, "--worker"], env=env, check=True)
        run_whisper("--time")
    post()
    run_whisper("--read-back")


if __name__ == "__main__":
    main(sys.argv[1:])
