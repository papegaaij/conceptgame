---
title: Concept round 29 — the Choir sings
design: approved
implementation: n/a
art: chosen
depends-on: [../../audio/voice, ../../audio/music, ../../story/characters]
updated: 2026-10-06
---

# Concept round 29 — the Choir sings

## Summary

The user found (2026-10-06) that the Choir's radio messages make no sound: Act 1's five Choir cues
are the stage direction `[the Choir sings]`, which the voice pipeline does not speak, and nothing
played in its place. The user's decision: make both kinds of sound and choose by ear, **a wordless
sung sting** in the Choir's voice (options a, b) or **a synthesized choir pad** like the music's
(options c, d), all by `tools/concept/audio/choir_r29.py`. Open [index.html](index.html) in a
browser (regenerate with `python3 tools/concept/board.py 29`). **Closed 2026-10-06: b chosen**, the
sung "ah" F3 → E3; it plays in the game with the Choir's subtitle: Level 01 at t=160
(`./gradlew :desktop:run --args="--level 1 --invulnerable"`), Level 03 at t=58.5, Level 05 at
t=144.5, Level 07 at t=94 and after the Brood Carrier dies.

## Choices

| # | Part | What is proposed | Review files | Weak spots (Claude) | Outcome |
|---|---|---|---|---|---|
| 1 | [The Choir's stage direction](../../audio/voice/README.md#the-choir) | pick **a**, **b**, **c** or **d**, played on the voice bus with the subtitle whenever a Choir line is only `[the Choir sings]` (the music ducks under it as under a spoken line). **Sung sting**, Varga's reference voice (the Choir's base) at the Choir's settings, then the Choir's layering (`choir()` of round 18: copies at −12, −5, 0, +7 semitones, a reversed copy, the 55/82 Hz drone, the 2.2 s reverb) and radio filter b: **a** "ooh" held on one note (E3), Chatterbox's own sustained vowel pitch-flattened (3.7 s); **b** "ah" sung as the Choir motif's landing, F3 falling to E3, built by stretching a half-second vowel (3.9 s). **Synthesized pad**, the music's choir voice (`v_choir`, the pads of "The Choir Descends") through the same drone, reverb and radio filter: **c** an "oo" chord E3 B3 E4 B4 held (3.9 s); **d** the motif's last two notes F4 → E4 sung "ah" over an "oo" pad, as in the boss theme's intro (4.0 s). All at −16 LUFS like the spoken lines | `voice-choir-sings-r29-a` … `-d` | not listened to (levels measured: all −16.0 LUFS ±0.1, peaks −7.1 to −9.8 dBFS, RMS −19.7 to −20.3 dB, next to Varga's lines at −16.0 LUFS and about −20 dB RMS); a and b are made from Chatterbox takes that were not meant to sing, so a may buzz or b may pulse where the stretched half-second repeats; the −12 copy of a and b (82 Hz) falls under the radio's 300 Hz high-pass, so only its overtones are heard; c and d may sound like a piece of the music rather than a voice on the radio; the cues are marked `distorted` (a damaged channel), which a spoken Choir line would get as dropouts on top of filter b; these sounds use filter b alone, as briefed | **b** (user): the sung "ah" in the Choir's voice, F3 falling to E3; the speaker table's `stage` and `:pipeline:copyPlaceholderStageSounds` name it, copied into `assets/voice/choir/` (option a's copy deleted); a, c and d moved to `concept/rejected/` (`choir_r29.py`'s `CHOSEN`) |

## Notes

- Review files: `voice-choir-sings-r29-a.ogg` … `-d.ogg` in
  [design/audio/voice/concept/](../../audio/voice/concept/prompts.md#voice-choir-sings); the
  `prompts.md` entry says how each is made. No third-party asset: the voice is Varga's reference
  clip (Betsie Bush, public domain, already in CREDITS.md), the pad is synthesized. Since the
  round closed, a, c and d are in `concept/rejected/`.
- The wiring (data-driven): the speaker table's new `stage` field names the sound a speaker's line
  plays when it is only a stage direction (`stage: voice-choir-sings-r29-a.ogg` on the Choir), a
  file in `assets/voice/choir/` copied by `./gradlew :pipeline:copyPlaceholderStageSounds` (part of
  `importPlaceholders`); `tools/art/voice.py` leaves it alone (it only deletes files named by a
  line's key). After the choice: the chosen file in the speaker table and the copy task, the others
  to `concept/rejected/`, the old copy in `assets/voice/choir/` deleted (done: b, 2026-10-06).
- Round number: on the `m5-act-2` branch round 28 is M5 part A's and the roadmap reserves round 29
  for M5 part B (Level 08 and the Act 2 intro); this round was opened on `m4-act-1`, so that plan
  shifts by one (part B's round becomes 30) when the branches merge.

## Decisions

- 2026-10-06: Opened with the user's decision to hear both kinds of sound for `[the Choir sings]`,
  two options each; option a wired provisionally.
- 2026-10-06: Closed (user): **b**, the sung "ah" falling F3 to E3, the Choir motif's landing, in
  the Choir's voice; rejected a (the held "ooh" on E3) and the synthesized pads c (the "oo" chord)
  and d (the motif over a pad). The rejected files are in `design/audio/voice/concept/rejected/`;
  the game plays b (`stage` in `design/audio/voice/data.yaml`, copied into `assets/voice/choir/`
  by `:pipeline:copyPlaceholderStageSounds`).
