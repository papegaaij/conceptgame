---
title: Audio
design: draft
implementation: not-started
art: chosen
depends-on: [../campaign, ../world]
updated: 2026-09-30
---

# Audio

## Summary

Late-90s tracker-era sound: driving electronic music (trance, techno, drum & bass tinges) mixed
with synth-orchestral brass and strings for the big moments. Sound effects are punchy, a little
crunchy, and clearly readable. Radio chatter is shown as text with a squelch and a short
vocal bark.

## Contents

| Part | Summary | Design | Impl | Art |
|---|---|---|---|---|
| [music](music/README.md) | Track list, styles per act, loop and transition rules | draft | not-started | chosen |
| [sfx](sfx/README.md) | Full sound-effect list with priorities, mixing rules | draft | not-started | proposed |

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

Four buses with independent volume in Options: **music**, **sfx**, **radio** (voice barks,
squelch), **ui**. Default mix: music −6 dB relative to sfx; the radio ducks music by 4 dB while
a message is shown.

### Formats

Engine-agnostic: OGG Vorbis 44.1 kHz for delivery; music with loop points; SFX mono unless
spatial width matters (explosions stereo). Source files (tracker modules, DAW projects)
are stored next to the concept files once production starts.

## Concept art

Round 01 audio proposals live in [music](music/README.md) and [sfx](sfx/README.md).

## Implementation

- [ ] Audio buses with volume settings and ducking
- [ ] Music playback with loop points and crossfades (see [music](music/README.md))
- [ ] SFX playback with voice limits and priorities (see [sfx](sfx/README.md))

## Open questions

- Voice acting for radio chatter and briefings: none (text + blips), short barks only, or
  full voice? Recommendation: short barks ("Copy that", "Six o'clock!") plus text. That's very
  90s and cheap to produce.

## Decisions

- 2026-09-30: Tracker-era electronic + synth-orchestral direction; four mix buses.
