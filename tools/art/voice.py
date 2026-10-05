#!/usr/bin/env python3
"""The spoken radio lines and briefing pages (design/audio/voice): Chatterbox, offline.

Outputs (assets/voice/):
  <voice>/<key>.ogg   one file per spoken line, OGG Vorbis q4, 44.1 kHz mono, -16 LUFS, with a
                      COMMENT tag naming this tool, the voice, the key and the take's seed(s).
  The key (12 hex digits of a SHA-256 over the voice's settings, the text as spoken, the
  expression, the shout flag and the filter) comes from vanguard.content.voice.VoiceLines, which
  the game uses too: `./gradlew :pipeline:voiceLines` writes the list this script reads
  (pipeline/build/voice/lines.json). A line whose file exists is not rendered again; a file no
  line uses any more is deleted (--keep lists them instead).

Render: Chatterbox (ResembleAI/chatterbox, chatterbox-tts 0.1.7) in its venv
~/.cache/tv-tts/venv-chatterbox (set up as in tools/concept/audio/tts_r18.py; models in
~/.cache/tv-tts/hf), cloning the speaker's clip in design/audio/voice/refs/ with the settings of
the line's expression (or the shout row) from design/audio/voice/data.yaml. Long lines (the
briefing pages) are spoken sentence by sentence (chunks of up to CHUNK_WORDS words) and joined
with PAUSE s of silence. Each chunk is spoken with a fixed seed (from the key, or the speaker
table's pin); the generation is capped at MAX_FACTOR x the expected length from the word count
(WORDS_PER_SECOND), and a take that hits the cap, runs on past it or is shorter than MIN_FACTOR x
the expected length is retried with the next seed, up to TAKES seeds; every retry is logged
(~/.cache/tv-tts/raw/voice/render-log.json and stdout).

Post: the Choir's layering (choir() of tools/concept/audio/tts_r18.py) for a voice with
`layering: choir`; then radio filter b (radio() there) for a radio line, the same plus dropouts
and more crackle for a distorted one, the public-address filter (pa() there: horn band, slap
echoes, a hall) for a voice with `filter: pa` (the Level 06 perimeter beacon), and no filter for
a briefing page (dry: the same mastering, -16 LUFS, -1.5 dBFS ceiling).

Rerun: python3 tools/art/voice.py            render what is missing, delete unused files
       python3 tools/art/voice.py --keep     the same, but only list the unused files
       python3 tools/art/voice.py --list     only print what would be rendered or deleted
Seeds are fixed, but GPU sampling is not bit-exact across drivers, so a rerun can differ slightly.
"""
import json
import os
import re
import subprocess
import sys
import time
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
ASSETS = ROOT / "assets" / "voice"
REFDIR = ROOT / "design" / "audio" / "voice" / "refs"
LINES = ROOT / "pipeline" / "build" / "voice" / "lines.json"
BASE = Path.home() / ".cache" / "tv-tts"
RAW = BASE / "raw" / "voice"
LOG = RAW / "render-log.json"

WORDS_PER_SECOND = 2.6   # Chatterbox's pace at the neutral row, measured on rounds 18-19
MAX_FACTOR = 1.5         # the length cap: 1.5 x the expected length
MIN_FACTOR = 0.5         # shorter than this is a truncated take
TAKES = 8                # seeds tried per chunk
CHUNK_WORDS = 28
PAUSE = 0.35             # s between the chunks of a long line
TOKENS_PER_SECOND = 25   # Chatterbox's speech tokens
SENTENCE_PAUSE = 0.4     # s expected per sentence end


def expected_seconds(text):
    """The expected length: the words at the measured pace, a short pause per comma, colon or
    semicolon and a longer one per sentence end (a run of dots, an ellipsis, counts once), which
    terse lines of short sentences need ("Domes intact. Airlocks open. Nobody's home." hit the
    cap with every seed at 0.12 s a stop)."""
    words = len(text.split())
    commas = len(re.findall(r"[;:,]", text))
    ends = len(re.findall(r"[.!?]+", text))
    return 0.3 + words / WORDS_PER_SECOND + 0.12 * commas + SENTENCE_PAUSE * ends


def chunks(text):
    """The sentences of a line, merged into chunks of up to CHUNK_WORDS words."""
    sentences = [s for s in re.split(r"(?<=[.!?])\s+", text.strip()) if s]
    out = []
    for s in sentences:
        if out and len(out[-1].split()) + len(s.split()) <= CHUNK_WORDS:
            out[-1] += " " + s
        else:
            out.append(s)
    return out


def base_seed(line):
    return line["pin"] if line.get("pin") is not None else int(line["key"][:7], 16)


# ---------------------------------------------------------------- worker (runs in the venv)

def trim(wav, sr):
    """Leading and trailing silence cut (-45 dB under the peak, 50 ms frames, 60 ms kept)."""
    import numpy as np
    frame = int(0.05 * sr)
    n = len(wav) // frame
    if n == 0:
        return wav
    rms = np.sqrt(np.mean(wav[: n * frame].reshape(n, frame) ** 2, axis=1) + 1e-12)
    loud = np.nonzero(rms > rms.max() * 10 ** (-45 / 20))[0]
    if len(loud) == 0:
        return wav
    keep = int(0.06 * sr)
    return wav[max(0, loud[0] * frame - keep): min(len(wav), (loud[-1] + 1) * frame + keep)]


def worker(jobs_path):
    import random

    import numpy as np
    import soundfile as sf
    import torch
    from chatterbox.tts import ChatterboxTTS
    model = ChatterboxTTS.from_pretrained(device="cuda")
    state = {"cap": 1000}
    inference = model.t3.inference

    def capped(*args, **kwargs):
        kwargs["max_new_tokens"] = state["cap"]
        return inference(*args, **kwargs)
    model.t3.inference = capped
    jobs = json.loads(Path(jobs_path).read_text())
    log = {}
    for job in jobs:
        t0 = time.time()
        parts, entry = [], {"voice": job["voice"], "source": job["source"], "chunks": []}
        for c, text in enumerate(chunks(job["spoken"])):
            expected = expected_seconds(text)
            cap_s = max(2.0, MAX_FACTOR * expected)
            state["cap"] = int(cap_s * TOKENS_PER_SECOND)
            tries, best = [], None
            for take in range(TAKES):
                seed = base_seed(job) + 1000 * c + take
                for f in (random.seed, np.random.seed, torch.manual_seed):
                    f(seed)
                wav = model.generate(text, audio_prompt_path=str(REFDIR / f"{job['ref']}.wav"),
                                     exaggeration=job["exaggeration"], cfg_weight=job["cfg_weight"],
                                     temperature=job["temperature"])
                wav = wav.cpu().numpy().reshape(-1)
                raw_s = len(wav) / model.sr
                wav = trim(wav, model.sr)
                dur = len(wav) / model.sr
                hit_cap = raw_s >= 0.97 * cap_s
                why = ("hit the cap" if hit_cap else "ran on" if dur > cap_s
                       else "too short" if dur < MIN_FACTOR * expected else "")
                tries.append({"seed": seed, "seconds": round(dur, 2), "rejected": why})
                if not why:
                    best = (wav, seed)
                    break
                if best is None or abs(dur - expected) < abs(len(best[0]) / model.sr - expected):
                    best = (wav, seed)
            if len(tries) > 1:
                print(f"  retry {job['voice']} {job['key']} chunk {c}: "
                      + ", ".join(f"{t['seed']} {t['seconds']}s {t['rejected'] or 'kept'}"
                                  for t in tries), flush=True)
            entry["chunks"].append({"text": text, "expected": round(expected, 2), "cap": cap_s,
                                    "takes": tries, "resolved": not tries[-1]["rejected"]})
            parts.append(best[0])
            parts.append(np.zeros(int(PAUSE * model.sr), dtype=best[0].dtype))
        wav = np.concatenate(parts[:-1])
        sf.write(str(RAW / f"{job['key']}.wav"), wav.astype("float32"), model.sr)
        entry["seconds"] = round(len(wav) / model.sr, 2)
        entry["seeds"] = [ch["takes"][-1]["seed"] if ch["resolved"] else None
                          for ch in entry["chunks"]]
        log[job["key"]] = entry
        print(job["voice"], job["key"], entry["seconds"], "s",
              f"({time.time() - t0:.1f} s)", flush=True)
        (RAW / "worker-log.json").write_text(json.dumps(log, indent=1))


# ---------------------------------------------------------------- post-process (system Python)

def _filters():
    sys.path.insert(0, str(ROOT / "tools" / "concept" / "audio"))
    import tts_r18
    return tts_r18


def distorted(y, s):
    """A damaged channel on top of filter b: dropouts and more crackle."""
    import numpy as np
    rng = np.random.default_rng(23)
    x = y[0].copy()
    n = len(x)
    i = int(0.3 * s.SR)
    while i < n:
        i += int(rng.uniform(0.25, 0.9) * s.SR)
        m = int(rng.uniform(0.03, 0.12) * s.SR)
        x[i:i + m] *= 0.08
    x += (rng.random(n) < 0.002) * rng.uniform(-1, 1, n) * 0.15
    return s.master(x[None, :], target_lufs=-16.0, ceiling_db=-1.5)


def post(line, log_entry):
    import numpy as np
    r18 = _filters()
    s = r18._synth()
    x = s.decode(RAW / f"{line['key']}.wav")[0]
    if line["layering"] == "choir":
        x = r18.choir(x)
    if line["filter"] == "dry":
        x = x / max(1e-9, np.max(np.abs(x))) * 0.8
        pad = np.zeros(int(0.1 * s.SR))
        y = s.master(np.concatenate([pad, x, pad])[None, :], target_lufs=-16.0, ceiling_db=-1.5)
    else:
        y = r18.pa(x) if line["filter"] == "pa" else r18.radio(x)
        if line["filter"] == "distorted":
            y = distorted(y, s)
    seeds = ",".join(str(seed) for seed in log_entry.get("seeds", []))
    tags = {"COMMENT": f"tools/art/voice.py {line['voice']} {line['key']} seeds {seeds}",
            "SOURCE": "tools/art/voice.py"}
    s.write_ogg(ASSETS / line["path"].removeprefix("voice/"), y, quality=4, tags=tags)


def main(args):
    if args[:1] == ["--worker"]:
        RAW.mkdir(parents=True, exist_ok=True)
        worker(args[1])
        return
    subprocess.run([str(ROOT / "gradlew"), "-q", ":pipeline:voiceLines"], cwd=ROOT, check=True)
    lines = {line["path"]: line for line in json.loads(LINES.read_text())}
    todo = [line for path, line in lines.items() if not (ROOT / "assets" / path).exists()]
    unused = sorted(p for p in ASSETS.rglob("*.ogg")
                    if str(p.relative_to(ROOT / "assets")) not in lines)
    print(f"{len(lines)} lines, {len(todo)} to render, {len(unused)} unused files")
    for p in unused:
        print(("  unused " if "--keep" in args or "--list" in args else "  deleted ")
              + str(p.relative_to(ROOT)))
        if "--keep" not in args and "--list" not in args:
            p.unlink()
    if "--list" in args:
        for line in todo:
            print("  render", line["path"], line["spoken"][:60])
        return
    if todo:
        RAW.mkdir(parents=True, exist_ok=True)
        jobs = RAW / "jobs.json"
        jobs.write_text(json.dumps(todo, indent=1))
        env = dict(os.environ, HF_HOME=str(BASE / "hf"), TOKENIZERS_PARALLELISM="false",
                   TORCH_FORCE_NO_WEIGHTS_ONLY_LOAD="1")
        py = BASE / "venv-chatterbox" / "bin" / "python"
        subprocess.run([str(py), __file__, "--worker", str(jobs)], env=env, check=True)
        worker_log = json.loads((RAW / "worker-log.json").read_text())
        log = json.loads(LOG.read_text()) if LOG.exists() else {}
        log.update(worker_log)
        LOG.write_text(json.dumps(log, indent=1))
        for line in todo:
            post(line, worker_log[line["key"]])
        retried = [k for k, e in worker_log.items() if any(len(c["takes"]) > 1 for c in e["chunks"])]
        unresolved = [k for k, e in worker_log.items() if not all(c["resolved"] for c in e["chunks"])]
        print(f"rendered {len(todo)}, retried {len(retried)}, unresolved {len(unresolved)}: "
              + " ".join(unresolved))
    for d in ASSETS.glob("*"):
        if d.is_dir() and not any(d.iterdir()):
            d.rmdir()


if __name__ == "__main__":
    main(sys.argv[1:])
