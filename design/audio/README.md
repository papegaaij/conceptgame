---
title: Audio
design: approved
implementation: in-progress
art: chosen
depends-on: [../campaign, ../world]
updated: 2026-10-02
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
| [music](music/README.md) | Track list, styles per act, loop and transition rules | approved | in-progress | chosen |
| [sfx](sfx/README.md) | Full sound-effect list with priorities, mixing rules | approved | in-progress | chosen |

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

Three buses with independent volume in Options, each times the master volume: **music**,
**sfx** (the effects volume) and **radio** (voice barks, squelch, typing blips). The menu and
other interface sounds play on the sfx bus at the effects volume; there is no separate interface
slider (user decision). Default mix: music −6 dB relative to sfx; the radio ducks music by 4 dB
while a message is shown.

### Formats

Engine-agnostic: OGG Vorbis 44.1 kHz for delivery; music with loop points; SFX mono unless
spatial width matters (explosions stereo). Source files (tracker modules, DAW projects)
are stored next to the concept files once production starts.

## Concept art

Round 01 audio proposals live in [music](music/README.md) and [sfx](sfx/README.md).

## Implementation

- [x] Audio buses with volume settings and ducking
- [ ] Music playback with loop points and crossfades (see [music](music/README.md))
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
