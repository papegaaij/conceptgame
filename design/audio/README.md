---
title: Audio
design: approved
implementation: in-progress
art: chosen
depends-on: [../campaign, ../world]
updated: 2026-10-05
---

# Audio

## Summary

Late-90s tracker-era sound: driving electronic music (trance, techno, drum & bass tinges) mixed
with synth-orchestral brass and strings for the big moments. Sound effects are punchy, a little
crunchy, and clearly readable. Radio chatter is spoken (text-to-speech rendered offline, see
[voice](voice/README.md)) through a radio filter, with the text on screen as subtitles.

## Contents

| Part | Summary | Design | Impl | Art |
|---|---|---|---|---|
| [music](music/README.md) | Track list, styles per act, loop and transition rules | approved | done | chosen |
| [sfx](sfx/README.md) | Full sound-effect list with priorities, mixing rules | approved | in-progress | chosen |
| [voice](voice/README.md) | Spoken radio lines: Chatterbox text-to-speech rendered offline through the radio filter | draft | in-progress | chosen |

## Design

### Direction

- **Reference feel**: Tyrian's tracker soundtrack, Raptor's punchy SFX, the Wing Commander /
  Colony Wars orchestral bombast for story moments.
- **Instruments**: analogue-style synth bass, supersaw and pulse leads, 909/808-style drums,
  breakbeats, orchestral hits, synth strings and brass, choir pads for the Vrell.
- **Faction colours**:
  - UTC / human: major/modal heroic brass motifs, clean synths.
  - Vrell: detuned choir pads, metallic resonances, insect-like rhythmic clicks. "The Choir"
    theme is a wordless vocal line.
  - Jovian Ascendancy: cold, industrial, minor-key arpeggios, heavy distorted bass.
- **Settings** colour the act themes: e.g. the Europa act gets muffled, reverb-heavy drones and
  sonar pings.

### Mix groups

Four buses with independent volume in Options, each times the master volume: **music**,
**sfx** (the effects volume), **radio** (squelch, typing blips) and **voice** (the spoken radio
lines and briefing pages, [voice](voice/README.md)). The menu and
other interface sounds play on the sfx bus at the effects volume; there is no separate interface
slider (user decision). Default mix: music −6 dB relative to sfx; the radio ducks music by 4 dB
while a message is shown or its voice plays.

### Master limiter

The buses sum into one mix, and in a busy fight that sum passes full scale: an explosion on a
boss's roar on the music reaches +3 to +4 dB over it (peaks of 1.37 in Level 07 and 1.59 in Level 05,
about 0.003 % of the samples), which the sound card would clip. A **master limiter** on the final
mix holds every peak at full scale (0 dBFS); there is no headroom on the buses, so the mix keeps
its levels. The limiter looks 1 ms ahead and only acts at full scale: until a level's first over
the mix passes sample for sample, 1 ms late. After an over it releases slowly instead of at
once: about −0.35 dB half a second later, −0.15 dB after 2 s and under −0.05 dB after 4–5 s
(at most about −1 dB just after a loud over), so the mix sits on average 0.1 dB lower through a
fight, far below what the ear notices, and its peaks stay at full scale. How it is built:
[architecture](../tech/architecture/README.md#presentation-game).

### Formats

Engine-agnostic: OGG Vorbis 44.1 kHz for delivery; music with loop points; SFX mono unless
spatial width matters (explosions stereo). Source files (tracker modules, DAW projects)
are stored next to the concept files once production starts.

## Concept art

Round 01 audio proposals live in [music](music/README.md) and [sfx](sfx/README.md).

Concept [round 18](../concept-rounds/round-18/README.md) — spoken radio lines: five
text-to-speech engines read the same five Act 1 lines, each through one shared radio filter
(`tools/concept/audio/tts_r18.py`; settings, prompts and reference voices in
[concept/prompts.md](concept/prompts.md)). `radio-*` is the line as the game would play it,
`voice-*` the raw engine output. Chatterbox was chosen; the other engines' files are in
`concept/rejected/`. The `-r18-b` files are the same Chatterbox takes through a radio filter with
more static (round 18, item 2), which was chosen: the `radio-chatterbox-*-r18-a` files (filter a)
moved to `concept/rejected/`, and the raw `voice-chatterbox-*-r18-a` takes stay chosen. The
spoken lines are designed in [voice](voice/README.md).

| File | What | Status |
|---|---|---|
| [concept/rejected/radio-chatterbox-a-okafor-r18-a.ogg](concept/rejected/radio-chatterbox-a-okafor-r18-a.ogg) | Chatterbox TTS — Okafor, grim, through the radio filter (`tools/concept/audio/tts_r18.py`) | rejected — filter b chosen (more static) |
| [concept/voice-chatterbox-a-okafor-r18-a.ogg](concept/voice-chatterbox-a-okafor-r18-a.ogg) | Chatterbox TTS — Okafor, grim, raw, unfiltered (`tools/concept/audio/tts_r18.py`) | chosen |
| [concept/rejected/radio-chatterbox-b-varga-r18-a.ogg](concept/rejected/radio-chatterbox-b-varga-r18-a.ogg) | Chatterbox TTS — Varga, calm and precise, through the radio filter (`tools/concept/audio/tts_r18.py`) | rejected — filter b chosen (more static) |
| [concept/voice-chatterbox-b-varga-r18-a.ogg](concept/voice-chatterbox-b-varga-r18-a.ogg) | Chatterbox TTS — Varga, calm and precise, raw, unfiltered (`tools/concept/audio/tts_r18.py`) | chosen |
| [concept/rejected/radio-chatterbox-c-rook-r18-a.ogg](concept/rejected/radio-chatterbox-c-rook-r18-a.ogg) | Chatterbox TTS — Rook, joking, through the radio filter (`tools/concept/audio/tts_r18.py`) | rejected — filter b chosen (more static) |
| [concept/voice-chatterbox-c-rook-r18-a.ogg](concept/voice-chatterbox-c-rook-r18-a.ogg) | Chatterbox TTS — Rook, joking, raw, unfiltered (`tools/concept/audio/tts_r18.py`) | chosen |
| [concept/rejected/radio-chatterbox-d-control-r18-a.ogg](concept/rejected/radio-chatterbox-d-control-r18-a.ogg) | Chatterbox TTS — Tranquility Control, urgent shout, through the radio filter (`tools/concept/audio/tts_r18.py`) | rejected — filter b chosen (more static) |
| [concept/voice-chatterbox-d-control-r18-a.ogg](concept/voice-chatterbox-d-control-r18-a.ogg) | Chatterbox TTS — Tranquility Control, urgent shout, raw, unfiltered (`tools/concept/audio/tts_r18.py`) | chosen |
| [concept/rejected/radio-chatterbox-e-choir-r18-a.ogg](concept/rejected/radio-chatterbox-e-choir-r18-a.ogg) | Chatterbox TTS — the Choir, layered in post, through the radio filter (`tools/concept/audio/tts_r18.py`) | rejected — filter b chosen (more static) |
| [concept/voice-chatterbox-e-choir-r18-a.ogg](concept/voice-chatterbox-e-choir-r18-a.ogg) | Chatterbox TTS — the Choir, layered in post, raw, unfiltered (`tools/concept/audio/tts_r18.py`) | chosen |
| [concept/radio-chatterbox-a-okafor-r18-b.ogg](concept/radio-chatterbox-a-okafor-r18-b.ogg) | Chatterbox TTS — Okafor, grim, the same take through radio filter b: more static (`tools/concept/audio/tts_r18.py --post-b`) | chosen |
| [concept/radio-chatterbox-b-varga-r18-b.ogg](concept/radio-chatterbox-b-varga-r18-b.ogg) | Chatterbox TTS — Varga, calm and precise, the same take through radio filter b: more static (`tools/concept/audio/tts_r18.py --post-b`) | chosen |
| [concept/radio-chatterbox-c-rook-r18-b.ogg](concept/radio-chatterbox-c-rook-r18-b.ogg) | Chatterbox TTS — Rook, joking, the same take through radio filter b: more static (`tools/concept/audio/tts_r18.py --post-b`) | chosen |
| [concept/radio-chatterbox-d-control-r18-b.ogg](concept/radio-chatterbox-d-control-r18-b.ogg) | Chatterbox TTS — Tranquility Control, urgent shout, the same take through radio filter b: more static (`tools/concept/audio/tts_r18.py --post-b`) | chosen |
| [concept/radio-chatterbox-e-choir-r18-b.ogg](concept/radio-chatterbox-e-choir-r18-b.ogg) | Chatterbox TTS — the Choir, layered in post, the same take through radio filter b: more static (`tools/concept/audio/tts_r18.py --post-b`) | chosen |
| [concept/rejected/radio-parler-a-okafor-r18-a.ogg](concept/rejected/radio-parler-a-okafor-r18-a.ogg) | Parler-TTS mini TTS — Okafor, grim, through the radio filter (`tools/concept/audio/tts_r18.py`) | rejected — Chatterbox chosen |
| [concept/rejected/voice-parler-a-okafor-r18-a.ogg](concept/rejected/voice-parler-a-okafor-r18-a.ogg) | Parler-TTS mini TTS — Okafor, grim, raw, unfiltered (`tools/concept/audio/tts_r18.py`) | rejected — Chatterbox chosen |
| [concept/rejected/radio-parler-b-varga-r18-a.ogg](concept/rejected/radio-parler-b-varga-r18-a.ogg) | Parler-TTS mini TTS — Varga, calm and precise, through the radio filter (`tools/concept/audio/tts_r18.py`) | rejected — Chatterbox chosen |
| [concept/rejected/voice-parler-b-varga-r18-a.ogg](concept/rejected/voice-parler-b-varga-r18-a.ogg) | Parler-TTS mini TTS — Varga, calm and precise, raw, unfiltered (`tools/concept/audio/tts_r18.py`) | rejected — Chatterbox chosen |
| [concept/rejected/radio-parler-c-rook-r18-a.ogg](concept/rejected/radio-parler-c-rook-r18-a.ogg) | Parler-TTS mini TTS — Rook, joking, through the radio filter (`tools/concept/audio/tts_r18.py`) | rejected — Chatterbox chosen |
| [concept/rejected/voice-parler-c-rook-r18-a.ogg](concept/rejected/voice-parler-c-rook-r18-a.ogg) | Parler-TTS mini TTS — Rook, joking, raw, unfiltered (`tools/concept/audio/tts_r18.py`) | rejected — Chatterbox chosen |
| [concept/rejected/radio-parler-d-control-r18-a.ogg](concept/rejected/radio-parler-d-control-r18-a.ogg) | Parler-TTS mini TTS — Tranquility Control, urgent shout, through the radio filter (`tools/concept/audio/tts_r18.py`) | rejected — Chatterbox chosen |
| [concept/rejected/voice-parler-d-control-r18-a.ogg](concept/rejected/voice-parler-d-control-r18-a.ogg) | Parler-TTS mini TTS — Tranquility Control, urgent shout, raw, unfiltered (`tools/concept/audio/tts_r18.py`) | rejected — Chatterbox chosen |
| [concept/rejected/radio-parler-e-choir-r18-a.ogg](concept/rejected/radio-parler-e-choir-r18-a.ogg) | Parler-TTS mini TTS — the Choir, layered in post, through the radio filter (`tools/concept/audio/tts_r18.py`) | rejected — Chatterbox chosen |
| [concept/rejected/voice-parler-e-choir-r18-a.ogg](concept/rejected/voice-parler-e-choir-r18-a.ogg) | Parler-TTS mini TTS — the Choir, layered in post, raw, unfiltered (`tools/concept/audio/tts_r18.py`) | rejected — Chatterbox chosen |
| [concept/rejected/radio-dia-a-okafor-r18-a.ogg](concept/rejected/radio-dia-a-okafor-r18-a.ogg) | Dia 1.6B TTS — Okafor, grim, through the radio filter (`tools/concept/audio/tts_r18.py`) | rejected — Chatterbox chosen |
| [concept/rejected/voice-dia-a-okafor-r18-a.ogg](concept/rejected/voice-dia-a-okafor-r18-a.ogg) | Dia 1.6B TTS — Okafor, grim, raw, unfiltered (`tools/concept/audio/tts_r18.py`) | rejected — Chatterbox chosen |
| [concept/rejected/radio-dia-b-varga-r18-a.ogg](concept/rejected/radio-dia-b-varga-r18-a.ogg) | Dia 1.6B TTS — Varga, calm and precise, through the radio filter (`tools/concept/audio/tts_r18.py`) | rejected — Chatterbox chosen |
| [concept/rejected/voice-dia-b-varga-r18-a.ogg](concept/rejected/voice-dia-b-varga-r18-a.ogg) | Dia 1.6B TTS — Varga, calm and precise, raw, unfiltered (`tools/concept/audio/tts_r18.py`) | rejected — Chatterbox chosen |
| [concept/rejected/radio-dia-c-rook-r18-a.ogg](concept/rejected/radio-dia-c-rook-r18-a.ogg) | Dia 1.6B TTS — Rook, joking, through the radio filter (`tools/concept/audio/tts_r18.py`) | rejected — Chatterbox chosen |
| [concept/rejected/voice-dia-c-rook-r18-a.ogg](concept/rejected/voice-dia-c-rook-r18-a.ogg) | Dia 1.6B TTS — Rook, joking, raw, unfiltered (`tools/concept/audio/tts_r18.py`) | rejected — Chatterbox chosen |
| [concept/rejected/radio-dia-d-control-r18-a.ogg](concept/rejected/radio-dia-d-control-r18-a.ogg) | Dia 1.6B TTS — Tranquility Control, urgent shout, through the radio filter (`tools/concept/audio/tts_r18.py`) | rejected — Chatterbox chosen |
| [concept/rejected/voice-dia-d-control-r18-a.ogg](concept/rejected/voice-dia-d-control-r18-a.ogg) | Dia 1.6B TTS — Tranquility Control, urgent shout, raw, unfiltered (`tools/concept/audio/tts_r18.py`) | rejected — Chatterbox chosen |
| [concept/rejected/radio-dia-e-choir-r18-a.ogg](concept/rejected/radio-dia-e-choir-r18-a.ogg) | Dia 1.6B TTS — the Choir, layered in post, through the radio filter (`tools/concept/audio/tts_r18.py`) | rejected — Chatterbox chosen |
| [concept/rejected/voice-dia-e-choir-r18-a.ogg](concept/rejected/voice-dia-e-choir-r18-a.ogg) | Dia 1.6B TTS — the Choir, layered in post, raw, unfiltered (`tools/concept/audio/tts_r18.py`) | rejected — Chatterbox chosen |
| [concept/rejected/radio-kokoro-a-okafor-r18-a.ogg](concept/rejected/radio-kokoro-a-okafor-r18-a.ogg) | Kokoro TTS — Okafor, grim, through the radio filter (`tools/concept/audio/tts_r18.py`) | rejected — Chatterbox chosen |
| [concept/rejected/voice-kokoro-a-okafor-r18-a.ogg](concept/rejected/voice-kokoro-a-okafor-r18-a.ogg) | Kokoro TTS — Okafor, grim, raw, unfiltered (`tools/concept/audio/tts_r18.py`) | rejected — Chatterbox chosen |
| [concept/rejected/radio-kokoro-b-varga-r18-a.ogg](concept/rejected/radio-kokoro-b-varga-r18-a.ogg) | Kokoro TTS — Varga, calm and precise, through the radio filter (`tools/concept/audio/tts_r18.py`) | rejected — Chatterbox chosen |
| [concept/rejected/voice-kokoro-b-varga-r18-a.ogg](concept/rejected/voice-kokoro-b-varga-r18-a.ogg) | Kokoro TTS — Varga, calm and precise, raw, unfiltered (`tools/concept/audio/tts_r18.py`) | rejected — Chatterbox chosen |
| [concept/rejected/radio-kokoro-c-rook-r18-a.ogg](concept/rejected/radio-kokoro-c-rook-r18-a.ogg) | Kokoro TTS — Rook, joking, through the radio filter (`tools/concept/audio/tts_r18.py`) | rejected — Chatterbox chosen |
| [concept/rejected/voice-kokoro-c-rook-r18-a.ogg](concept/rejected/voice-kokoro-c-rook-r18-a.ogg) | Kokoro TTS — Rook, joking, raw, unfiltered (`tools/concept/audio/tts_r18.py`) | rejected — Chatterbox chosen |
| [concept/rejected/radio-kokoro-d-control-r18-a.ogg](concept/rejected/radio-kokoro-d-control-r18-a.ogg) | Kokoro TTS — Tranquility Control, urgent shout, through the radio filter (`tools/concept/audio/tts_r18.py`) | rejected — Chatterbox chosen |
| [concept/rejected/voice-kokoro-d-control-r18-a.ogg](concept/rejected/voice-kokoro-d-control-r18-a.ogg) | Kokoro TTS — Tranquility Control, urgent shout, raw, unfiltered (`tools/concept/audio/tts_r18.py`) | rejected — Chatterbox chosen |
| [concept/rejected/radio-kokoro-e-choir-r18-a.ogg](concept/rejected/radio-kokoro-e-choir-r18-a.ogg) | Kokoro TTS — the Choir, layered in post, through the radio filter (`tools/concept/audio/tts_r18.py`) | rejected — Chatterbox chosen |
| [concept/rejected/voice-kokoro-e-choir-r18-a.ogg](concept/rejected/voice-kokoro-e-choir-r18-a.ogg) | Kokoro TTS — the Choir, layered in post, raw, unfiltered (`tools/concept/audio/tts_r18.py`) | rejected — Chatterbox chosen |
| [concept/rejected/radio-bark-a-okafor-r18-a.ogg](concept/rejected/radio-bark-a-okafor-r18-a.ogg) | Bark TTS — Okafor, grim, through the radio filter (`tools/concept/audio/tts_r18.py`) | rejected — Chatterbox chosen |
| [concept/rejected/voice-bark-a-okafor-r18-a.ogg](concept/rejected/voice-bark-a-okafor-r18-a.ogg) | Bark TTS — Okafor, grim, raw, unfiltered (`tools/concept/audio/tts_r18.py`) | rejected — Chatterbox chosen |
| [concept/rejected/radio-bark-b-varga-r18-a.ogg](concept/rejected/radio-bark-b-varga-r18-a.ogg) | Bark TTS — Varga, calm and precise, through the radio filter (`tools/concept/audio/tts_r18.py`) | rejected — Chatterbox chosen |
| [concept/rejected/voice-bark-b-varga-r18-a.ogg](concept/rejected/voice-bark-b-varga-r18-a.ogg) | Bark TTS — Varga, calm and precise, raw, unfiltered (`tools/concept/audio/tts_r18.py`) | rejected — Chatterbox chosen |
| [concept/rejected/radio-bark-c-rook-r18-a.ogg](concept/rejected/radio-bark-c-rook-r18-a.ogg) | Bark TTS — Rook, joking, through the radio filter (`tools/concept/audio/tts_r18.py`) | rejected — Chatterbox chosen |
| [concept/rejected/voice-bark-c-rook-r18-a.ogg](concept/rejected/voice-bark-c-rook-r18-a.ogg) | Bark TTS — Rook, joking, raw, unfiltered (`tools/concept/audio/tts_r18.py`) | rejected — Chatterbox chosen |
| [concept/rejected/radio-bark-d-control-r18-a.ogg](concept/rejected/radio-bark-d-control-r18-a.ogg) | Bark TTS — Tranquility Control, urgent shout, through the radio filter (`tools/concept/audio/tts_r18.py`) | rejected — Chatterbox chosen |
| [concept/rejected/voice-bark-d-control-r18-a.ogg](concept/rejected/voice-bark-d-control-r18-a.ogg) | Bark TTS — Tranquility Control, urgent shout, raw, unfiltered (`tools/concept/audio/tts_r18.py`) | rejected — Chatterbox chosen |
| [concept/rejected/radio-bark-e-choir-r18-a.ogg](concept/rejected/radio-bark-e-choir-r18-a.ogg) | Bark TTS — the Choir, layered in post, through the radio filter (`tools/concept/audio/tts_r18.py`) | rejected — Chatterbox chosen |
| [concept/rejected/voice-bark-e-choir-r18-a.ogg](concept/rejected/voice-bark-e-choir-r18-a.ogg) | Bark TTS — the Choir, layered in post, raw, unfiltered (`tools/concept/audio/tts_r18.py`) | rejected — Chatterbox chosen |

## Implementation

- [x] Audio buses with volume settings and ducking
- [x] Master limiter on the final mix: no peak over full scale, levels unchanged below it
- [x] Music playback with loop points and crossfades (see [music](music/README.md))
- [ ] SFX playback with voice limits and priorities (see [sfx](sfx/README.md))

## Open questions

- None open.

## Decisions

- 2026-09-30: Tracker-era electronic + synth-orchestral direction; four mix buses.
- 2026-10-01: Voices: text and radio blips only for now — no voice acting.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../reviews/acts-1-2/README.md).
- 2026-10-02: M3 part A: buses with volumes (`vanguard.game.audio.Mixer`, `Bus`): music, effects
  and radio, each times the master volume, from the Audio tab of [options](../ui/options/README.md)
  and applied live; the menu sounds play on the effects bus, since the options document has no
  interface volume (the *Mix groups* above name a fourth `ui` bus: open question). The radio ducks
  the level music by 4 dB while a message is shown. The item stays open until the `ui` bus is
  settled.
- 2026-10-02: Menu sounds play at the effects volume (user decision): no separate `ui` bus or
  interface slider; *Mix groups* now names the three buses as built, so the buses item is done.
- 2026-10-03: Concept round 18 tests spoken radio lines (text-to-speech, generated offline;
  the subtitles stay) against the 2026-10-01 text-only decision; the user picks an engine, or
  none.
- 2026-10-03: Round 18 closed: **Chatterbox** chosen ("by far the best", user); Parler-TTS, Dia,
  Kokoro and Bark rejected (files in `concept/rejected/`). This **reverses the 2026-10-01 "no
  voice acting" decision**: the radio lines get synthesized speech by Chatterbox through the
  radio filter, and the text stays on screen as subtitles. Designed in [voice](voice/README.md).
  The user asked for more static on the radio: filter b (a steady hiss bed and a little more
  crackle) is in round 18, item 2.
- 2026-10-03: Round 18 item 2 decided by the user: radio filter **b** (more static). Filter a's
  `radio-chatterbox-*-r18-a` files moved to `concept/rejected/`; the raw Chatterbox takes stay
  chosen, since only the filter changed.
- 2026-10-05: Master limiter on the final mix (M4 part G): the Level 07 capture found the mix over
  full scale (peaks 1.2–1.46, clipped by the sound card), in every busy level. Bus headroom was
  rejected (it turns the quiet passages down too), and so was a tighter voice limit (it would cut
  sounds and still not bound the sum). The audio library's own output limiter is used: on the same
  recorded mixes of Levels 05 and 07 it brings the peaks from 1.59 and 1.37 to 1.00 and leaves the
  mix sample for sample until the first over; after an over it costs 0.1 dB on average for a few
  seconds. A limiter of our own that releases at once would need the whole mix rendered in
  software and replayed, about 20 ms more latency on every sound: not worth it for that 0.1 dB.
- 2026-10-05: Round 25 closed (user): the master limiter approved as built (measured, not judged by
  ear: a slow make-up release after an over, on average 0.1 dB lower through a fight); the music's
  crossfades into the boss cue are done, so the music playback item is ticked.
