#!/usr/bin/env python3
"""Concept round 18: the same five radio lines spoken by five text-to-speech engines.

Outputs (design/audio/concept/):
  radio-<engine>-<line>-r18-a.ogg   the line through the shared radio filter (mono, -16 LUFS)
  radio-chatterbox-<line>-r18-b.ogg the chosen engine's same takes through radio filter b: more
                                    static (a steady hiss bed and a little more crackle); filter b
                                    was chosen and is radio()'s default, the -a files keep filter a
  voice-<engine>-<line>-r18-a.ogg   the raw engine output, unfiltered (the Choir: after layering)
  engine: chatterbox, parler, dia, kokoro, bark; line: a-okafor, b-varga, c-rook, d-control,
  e-choir (the texts and the per-engine voice settings are in LINES and VOICES below, and in
  design/audio/concept/prompts.md).

Setup (outside the repo, everything under ~/.cache/tv-tts/; the system Python 3.14 is only used
for the post-process, which needs numpy and ffmpeg like the other tools here):
  python3 -m venv ~/.cache/tv-tts/uvenv && ~/.cache/tv-tts/uvenv/bin/pip install uv
  export UV_PYTHON_INSTALL_DIR=~/.cache/tv-tts/python UV_CACHE_DIR=~/.cache/tv-tts/uv-cache
  uv python install 3.11; then per engine `uv venv -p 3.11 ~/.cache/tv-tts/venv-<engine>` and
  `VIRTUAL_ENV=~/.cache/tv-tts/venv-<engine> uv pip install ...`:
    chatterbox  chatterbox-tts==0.1.7 (torch 2.6.0+cu124)    model ResembleAI/chatterbox
    parler      parler-tts 0.2.2 from git+https://github.com/huggingface/parler-tts.git,
                soundfile (torch 2.14.1+cu130, transformers 4.46.1)
                                                             model parler-tts/parler-tts-mini-v1
    dia         nari-tts 0.1.0 from git+https://github.com/nari-labs/dia.git, soundfile,
                faster-whisper (transcribes the reference clips) (torch 2.6.0+cu126)
                                                             model nari-labs/Dia-1.6B-0626, fp16
    kokoro      kokoro==0.9.4, soundfile, "transformers>=4.40" (torch 2.14.1+cu130)
                                                             model hexgrad/Kokoro-82M
    bark        suno-bark from git+https://github.com/suno-ai/bark.git, soundfile
                (torch 2.14.1+cu130)                         model suno/bark (full, CPU offload)
  Models download into HF_HOME=~/.cache/tv-tts/hf on the first run.
  Reference voices (Chatterbox and Dia clone them): 12 s cut at 60 s from LibriVox chapters,
  public domain (see REFS), into ~/.cache/tv-tts/refs/ref-<voice>.wav (24 kHz mono):
    ffmpeg -ss 60 -t 12 -i <chapter>_64kb.mp3 -ac 1 -ar 24000 ref-<voice>.wav

Rerun: python3 tools/concept/audio/tts_r18.py [engine ...]   (generate + post-process)
       python3 tools/concept/audio/tts_r18.py --post            (post-process only)
       python3 tools/concept/audio/tts_r18.py --post-b [engine ...]  (filter b, default chatterbox)
Raw engine WAVs and timings go to ~/.cache/tv-tts/raw/<engine>/. Seeds are fixed, but GPU
sampling is not bit-exact across drivers, so a rerun can differ slightly.
"""
import json
import os
import subprocess
import sys
import time
from pathlib import Path

BASE = Path.home() / ".cache" / "tv-tts"
RAW = BASE / "raw"
REFDIR = BASE / "refs"
ENGINES = ["chatterbox", "parler", "dia", "kokoro", "bark"]

# (speaker voice, subtitle text). Real lines from Act 1's data.yaml; the Choir's from its
# character README (Acts 3-4: "...many... we are many...").
LINES = {
    "a-okafor": ("okafor", "That's the yards. Not all of them. Enough. Come home, Aegis."),
    "b-varga": ("varga", "Walker on the rille rim. The claws are armour. Wait for it to turn, "
                         "then hit the glowing back."),
    "c-rook": ("rook", "You know, they told me you were the quiet type. Good. More airtime for me."),
    "d-control": ("control", "Walkers coming over the crater rims, both sides of the road!"),
    "e-choir": ("choir", "...many... we are many..."),
}

# LibriVox recordings (public domain), the readers' voices are cloned by Chatterbox and Dia.
REFS = {
    "okafor": "haunted_man_rg_librivox/hauntedman_1_dickens_64kb.mp3",  # Ruth Golding, CC0 1.0
    "varga": "deephaven_0812_bb_librivox/deephaven_01_jewett_64kb.mp3",  # Betsie Bush, PD
    "rook": "youknowme_al_0908_librivox/youknowmeal_01_lardner_64kb.mp3",  # Rick Rodstrom, PD
    "control": "hero_tales_from_american_history_dl_0905_librivox/"
               "herotales_01_lodgeroosevelt_64kb.mp3",  # David Leeson, PD
}
REF_OF = {"okafor": "okafor", "varga": "varga", "rook": "rook", "control": "control",
          "choir": "varga"}

# Per engine and voice: the settings; per engine and line: the text as the engine reads it.
VOICES = {
    "chatterbox": {  # exaggeration 0.25-2 (emotion intensity), cfg_weight (pace/adherence)
        "okafor": {"exaggeration": 0.45, "cfg_weight": 0.5, "temperature": 0.7},
        "varga": {"exaggeration": 0.5, "cfg_weight": 0.5, "temperature": 0.7},
        "rook": {"exaggeration": 0.8, "cfg_weight": 0.4, "temperature": 0.8},
        "control": {"exaggeration": 1.2, "cfg_weight": 0.3, "temperature": 0.8},
        "choir": {"exaggeration": 0.3, "cfg_weight": 0.3, "temperature": 0.7},
    },
    "parler": {  # a text description; named speakers keep the voice consistent
        "okafor": "Laura speaks in a low, grave and sad voice at a slow pace, steady and "
                  "controlled. The recording is very clear with no background noise.",
        "varga": "Lea speaks in a calm, precise and clear voice at a moderate pace. The "
                 "recording is very clear with no background noise.",
        "rook": "Jon speaks in a cheerful, playful and very expressive voice at a fast pace. "
                "The recording is very clear with no background noise.",
        "control": "Gary shouts urgently in a loud, tense and very expressive voice at a very "
                   "fast pace. The recording is very clear with no background noise.",
        "choir": "Jenna speaks in a very slow, breathy, monotone whisper. The recording is "
                 "very clear with no background noise.",
    },
    "kokoro": {  # voice id and speed only; no emotion control
        "okafor": ("bf_emma", 0.9), "varga": ("af_heart", 1.0), "rook": ("am_puck", 1.1),
        "control": ("am_fenrir", 1.2), "choir": ("af_bella", 0.8),
    },
    "bark": {  # history prompt; emotion through text cues ([sighs], [laughs], CAPS)
        "okafor": "v2/en_speaker_9", "varga": "v2/de_speaker_3", "rook": "v2/en_speaker_6",
        "control": "v2/en_speaker_3", "choir": "v2/en_speaker_9",
    },
    "dia": {  # clones the reference clip (its whisper transcript is prepended)
        "okafor": {}, "varga": {}, "rook": {}, "control": {}, "choir": {},
    },
}
TEXTS = {
    "dia": {
        "a-okafor": "[S1] (sighs) That's the yards. Not all of them. Enough. Come home, Aegis.",
        "c-rook": "[S1] You know, they told me you were the quiet type. Good. (laughs) More "
                  "airtime for me.",
        "d-control": "[S1] Walkers coming over the crater rims! Both sides of the road!",
    },
    "bark": {
        "a-okafor": "[sighs] That's the yards. Not all of them. Enough... Come home, Aegis.",
        "c-rook": "You know, they told me you were the quiet type. Good. [laughs] More airtime "
                  "for me.",
        "d-control": "WALKERS coming over the crater rims! BOTH SIDES OF THE ROAD!",
    },
}
SEED = 18


def text_for(engine, line):
    t = TEXTS.get(engine, {}).get(line, LINES[line][1])
    return t if engine != "dia" or t.startswith("[S1]") else "[S1] " + t


# ---------------------------------------------------------------- workers (run in the venvs)

def _save(path, wav, sr):
    import numpy as np
    import soundfile as sf
    sf.write(str(path), np.asarray(wav, dtype="float32").reshape(-1), sr)


def _seed(n):
    import random

    import numpy as np
    import torch
    random.seed(n)
    np.random.seed(n)
    torch.manual_seed(n)


def worker(engine, out):
    import torch
    t0 = time.time()
    gen = globals()["load_" + engine]()
    timing = {"load_s": round(time.time() - t0, 1), "lines": {}}
    for i, line in enumerate(LINES):
        _seed(SEED + i)
        t = time.time()
        wav, sr = gen(line, LINES[line][0], text_for(engine, line))
        el = time.time() - t
        _save(out / f"{line}.wav", wav, sr)
        dur = len(wav.reshape(-1)) / sr
        timing["lines"][line] = {"gen_s": round(el, 1), "audio_s": round(dur, 2),
                                 "rtf": round(el / dur, 2)}
        print(engine, line, timing["lines"][line], flush=True)
    if torch.cuda.is_available():
        timing["peak_vram_gb"] = round(torch.cuda.max_memory_allocated() / 2**30, 2)
    (out / "timing.json").write_text(json.dumps(timing, indent=1))


def load_chatterbox():
    from chatterbox.tts import ChatterboxTTS
    model = ChatterboxTTS.from_pretrained(device="cuda")

    def gen(line, voice, text):
        wav = model.generate(text, audio_prompt_path=str(REFDIR / f"ref-{REF_OF[voice]}.wav"),
                             **VOICES["chatterbox"][voice])
        return wav.cpu().numpy(), model.sr
    return gen


def load_parler():
    import torch
    from parler_tts import ParlerTTSForConditionalGeneration
    from transformers import AutoTokenizer
    mid = "parler-tts/parler-tts-mini-v1"
    model = ParlerTTSForConditionalGeneration.from_pretrained(
        mid, torch_dtype=torch.float16).to("cuda")
    tok = AutoTokenizer.from_pretrained(mid)

    def gen(line, voice, text):
        d = tok(VOICES["parler"][voice], return_tensors="pt").to("cuda")
        p = tok(text, return_tensors="pt").to("cuda")
        audio = model.generate(input_ids=d.input_ids, attention_mask=d.attention_mask,
                               prompt_input_ids=p.input_ids,
                               prompt_attention_mask=p.attention_mask)
        return audio.float().cpu().numpy(), model.config.sampling_rate
    return gen


def load_dia():
    import torch
    from dia.model import Dia
    from faster_whisper import WhisperModel
    transcripts = {}
    asr = WhisperModel("base.en", device="cpu", compute_type="int8")
    for v in set(REF_OF.values()):
        tf = REFDIR / f"ref-{v}.txt"
        if not tf.exists():
            segs, _ = asr.transcribe(str(REFDIR / f"ref-{v}.wav"))
            tf.write_text(" ".join(s.text.strip() for s in segs))
        transcripts[v] = tf.read_text().strip()
    del asr
    model = Dia.from_pretrained("nari-labs/Dia-1.6B-0626", compute_dtype="float16",
                                device=torch.device("cuda"))

    def gen(line, voice, text):
        ref = REF_OF[voice]
        wav = model.generate("[S1] " + transcripts[ref] + " " + text,
                             audio_prompt=str(REFDIR / f"ref-{ref}.wav"),
                             use_torch_compile=False, max_tokens=2048, cfg_scale=3.0, temperature=1.2,
                             top_p=0.95, cfg_filter_top_k=45)
        return wav, 44100
    return gen


def load_kokoro():
    import numpy as np
    from kokoro import KPipeline
    pipes = {}

    def gen(line, voice, text):
        vid, speed = VOICES["kokoro"][voice]
        lang = vid[0]
        if lang not in pipes:
            pipes[lang] = KPipeline(lang_code=lang, repo_id="hexgrad/Kokoro-82M")
        parts = [a.cpu().numpy() if hasattr(a, "cpu") else a
                 for _, _, a in pipes[lang](text, voice=vid, speed=speed)]
        return np.concatenate(parts), 24000
    return gen


def load_bark():
    from bark import SAMPLE_RATE, generate_audio, preload_models
    preload_models()

    def gen(line, voice, text):
        return generate_audio(text, history_prompt=VOICES["bark"][voice],
                              silent=True), SAMPLE_RATE
    return gen


# ---------------------------------------------------------------- post-process (system python)

def _synth():
    sys.path.insert(0, str(Path(__file__).resolve().parent))
    import synth
    return synth


def _ffmpeg_pitch(x, ratio, sr):
    """Pitch shift with the same duration (ffmpeg rubberband), mono float in and out."""
    import numpy as np
    r = subprocess.run(
        ["ffmpeg", "-hide_banner", "-loglevel", "error", "-f", "f32le", "-ar", str(sr), "-ac",
         "1", "-i", "-", "-af", f"rubberband=pitch={ratio}", "-f", "f32le", "-"],
        input=x.astype("<f4").tobytes(), capture_output=True, check=True).stdout
    y = np.frombuffer(r, dtype="<f4").astype(float)
    return np.pad(y, (0, max(0, len(x) - len(y))))[: len(x)]


def choir(x):
    """The Choir, identical for every engine: unison layers at -12/-5/0/+7 semitones, one
    reversed, staggered by 0-45 ms, over a low drone, with a long reverb."""
    import numpy as np
    s = _synth()
    sr = s.SR
    x = x / max(1e-9, np.max(np.abs(x)))
    n = len(x) + int(1.2 * sr)
    out = np.zeros(n)
    for k, (semi, g, d) in enumerate([(-12, 0.8, 0.0), (-5, 0.55, 0.015), (0, 0.45, 0.03),
                                      (7, 0.3, 0.045)]):
        y = _ffmpeg_pitch(x, 2 ** (semi / 12), sr) * g
        i = int(d * sr)
        out[i:i + len(y)] += y
    out[: len(x)] += x[::-1] * 0.22
    t = np.arange(n) / sr
    rng = np.random.default_rng(18)
    drone = (s.sine(55.0, n) + 0.6 * s.sine(82.4, n) * (0.6 + 0.4 * s.sine(0.23, n))
             + 0.5 * s.onepole_lp(s.noise(n, rng), 300)) * 0.12
    env = np.minimum(1, t / 0.4) * np.minimum(1, (t[-1] - t) / 0.6)
    out = out + drone * env
    wet = s.reverb(np.vstack([out, out]), seconds=2.2, mix=0.35, seed=18)[0][:n]
    return wet / max(1e-9, np.max(np.abs(wet)))


def radio(x, variant="b"):
    """The shared radio filter: 300-3400 Hz band-pass, saturation, static, a click in and a
    click out, -16 LUFS. Variant b, the default (chosen by the user in round 18 item 2: more
    static): a steady hiss bed about 10 dB louder than a, with a slow flutter, and about 2.5x the
    crackle. Variant a (the round's first filter, rejected): faint hiss and sparse crackle.
    Mono float at synth.SR in, (1, n) out."""
    import numpy as np
    s = _synth()
    sr = s.SR
    rng = np.random.default_rng(18)
    x = x / max(1e-9, np.max(np.abs(x))) * 0.8
    pad = int(0.15 * sr)
    x = np.concatenate([np.zeros(pad), x, np.zeros(pad)])
    n = len(x)
    if variant == "a":
        hiss = s.noise(n, rng) * 0.006
        crackle = (rng.random(n) < 0.0004) * rng.uniform(-1, 1, n) * 0.08
    else:
        flutter = 1 + 0.15 * s.sine(0.7, n) + 0.1 * s.sine(3.1, n)
        hiss = s.noise(n, rng) * 0.019 * flutter
        crackle = (rng.random(n) < 0.001) * rng.uniform(-1, 1, n) * 0.11
    y = x + hiss + crackle
    for _ in range(2):
        y = s.svf(y, 300, mode="hp")
        y = s.svf(y, 3400, mode="lp")
    y = s.saturate(y * 1.6, 2.0) * 0.8
    for at in (int(0.03 * sr), n - int(0.05 * sr)):
        m = int(0.004 * sr)
        click = s.noise(m, rng) * np.exp(-np.arange(m) / (0.0008 * sr)) * 0.6
        y[at:at + m] += s.svf(click, 2500, q=1.2, mode="bp") * 2.5
    y = s.master(y[None, :], target_lufs=-16.0, ceiling_db=-1.5)
    return y


def pa(x):
    """The public-address filter (round 23, the Daedalus perimeter beacon): an automated message
    from horn loudspeakers across a settlement, not a radio. 250-4000 Hz band-pass with the horns'
    resonances (1.1 and 2.6 kHz), a driven horn's saturation, a faint 60 Hz mains hum, then the
    same message from the farther speakers: slap echoes at 0.13, 0.29 and 0.47 s (each darker)
    and a 1.4 s hall reverb; no static and no squelch clicks (the radio filter's). -16 LUFS.
    Mono float at synth.SR in, (1, n) out, like radio()."""
    import numpy as np
    s = _synth()
    sr = s.SR
    rng = np.random.default_rng(23)
    x = x / max(1e-9, np.max(np.abs(x))) * 0.8
    x = np.concatenate([np.zeros(int(0.1 * sr)), x, np.zeros(int(1.2 * sr))])
    n = len(x)
    y = x
    for _ in range(2):
        y = s.svf(y, 250, mode="hp")
        y = s.svf(y, 4000, mode="lp")
    y = y + 0.9 * s.svf(y, 1100, q=2.0, mode="bp") + 0.6 * s.svf(y, 2600, q=3.0, mode="bp")
    y = s.saturate(y * 2.2, 2.5) * 0.7
    out = y.copy()
    for at, gain, cutoff in ((0.13, 0.42, 3000), (0.29, 0.24, 2200), (0.47, 0.13, 1600)):
        d = int(at * sr)
        out[d:] += s.svf(y, cutoff, mode="lp")[: n - d] * gain
    t = np.arange(n) / sr
    hum = (np.sin(2 * np.pi * 60 * t) + 0.5 * np.sin(2 * np.pi * 120 * t)
           + 0.25 * np.sin(2 * np.pi * 180 * t)) * 0.004 + s.noise(n, rng) * 0.0015
    out = out + s.svf(hum, 250, mode="hp")
    wet = s.reverb(np.vstack([out, out]), seconds=1.4, mix=0.28, damping=0.6, seed=23)[0][:n]
    return s.master(wet[None, :], target_lufs=-16.0, ceiling_db=-1.5)


def post():
    import numpy as np
    s = _synth()
    root = Path(__file__).resolve().parents[3]
    dest = root / "design" / "audio" / "concept"
    stats = {}
    for engine in ENGINES:
        for line in LINES:
            src = RAW / engine / f"{line}.wav"
            if not src.exists():
                continue
            x = s.decode(src)[0]
            if line == "e-choir":
                x = choir(x)
            tags = {"COMMENT": f"tools/concept/audio/tts_r18.py {engine} {line}"}
            raw = x / max(1e-9, np.max(np.abs(x))) * s.undb(-3)
            s.write_ogg(dest / f"voice-{engine}-{line}-r18-a.ogg", raw[None, :], quality=4,
                        tags=tags)
            y = radio(x, "a")
            s.write_ogg(dest / f"radio-{engine}-{line}-r18-a.ogg", y, quality=4, tags=tags)
            stats[f"{engine} {line}"] = round(s.lufs(y)[0], 1)
            print(engine, line, stats[f"{engine} {line}"], "LUFS", flush=True)


def post_b(engines):
    """Filter b on the existing raw takes (no new generation), radio-*-r18-b.ogg only."""
    import numpy as np
    s = _synth()
    dest = Path(__file__).resolve().parents[3] / "design" / "audio" / "concept"
    for engine in engines:
        for line in LINES:
            x = s.decode(RAW / engine / f"{line}.wav")[0]
            if line == "e-choir":
                x = choir(x)
            y = radio(x, "b")
            tags = {"COMMENT": f"tools/concept/audio/tts_r18.py --post-b {engine} {line}"}
            s.write_ogg(dest / f"radio-{engine}-{line}-r18-b.ogg", y, quality=4, tags=tags)
            print(engine, line, "b", round(s.lufs(y)[0], 1), "LUFS", flush=True)


def main(args):
    if args[:1] == ["--post-b"]:
        post_b(args[1:] or ["chatterbox"])
        return
    if args[:1] == ["--worker"]:
        out = RAW / args[1]
        out.mkdir(parents=True, exist_ok=True)
        worker(args[1], out)
        return
    if args[:1] != ["--post"]:
        env = dict(os.environ, HF_HOME=str(BASE / "hf"), SUNO_OFFLOAD_CPU="True",
                   TORCH_FORCE_NO_WEIGHTS_ONLY_LOAD="1", TOKENIZERS_PARALLELISM="false",
                   PYTORCH_CUDA_ALLOC_CONF="expandable_segments:True")
        for engine in args or ENGINES:
            py = BASE / f"venv-{engine}" / "bin" / "python"
            subprocess.run([str(py), __file__, "--worker", engine], env=env, check=True)
    post()


if __name__ == "__main__":
    main(sys.argv[1:])
