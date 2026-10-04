---
title: Reference voices
design: draft
implementation: done
art: chosen
depends-on: [..]
updated: 2026-10-04
---

# Reference voices

## Summary

The 12 s clips Chatterbox clones the speakers' voices from: LibriVox chapter readings whose
archive.org items are public domain or CC0, cut to mono 24 kHz WAV and stored in Git LFS. Each
`.txt` is the clip's Whisper (faster-whisper `base.en`) transcript, the check that the cut is the
reader's narration and not an intro or music. Every clip is in [CREDITS.md](../../../../CREDITS.md).

## Design

How the clips are chosen and used: [voice](../README.md#reference-voices).

| Clip | For | Reader | Source | Licence | Status |
|---|---|---|---|---|---|
| `ref-okafor.wav` | Okafor | Ruth Golding | [The Haunted Man and the Ghost's Bargain, ch. 1 (Dickens)](https://archive.org/details/haunted_man_rg_librivox) | CC0 1.0 | chosen (round 18) |
| `ref-varga.wav` | Varga and the Choir's base | Betsie Bush | [Deephaven, ch. 1 (Jewett)](https://archive.org/details/deephaven_0812_bb_librivox) | public domain | chosen (round 18) |
| `ref-rook.wav` | Rook | Rick Rodstrom | [You Know Me Al, ch. 1 (Lardner)](https://archive.org/details/youknowme_al_0908_librivox) | public domain | chosen (round 18) |
| `ref-yard-control.wav` | Yard Control | Mark Nelson | [A Princess of Mars, foreword (Burroughs)](https://archive.org/details/princess_mars_0810_librivox) | public domain | chosen (round 19) |
| `ref-dock.wav` | Dock One–Four | Kristin Hughes | [The Strange Case of Dr. Jekyll and Mr. Hyde, ch. 1 (Stevenson)](https://archive.org/details/jekyll_and_hyde_klh_0904_librivox) | public domain | chosen (round 19) |
| `ref-ring-control.wav` | Ring Control | Cori Samuel | [Frankenstein (1818), preface (Shelley)](https://archive.org/details/frankenstein_cs_librivox) | CC0 1.0 | chosen (round 19) |
| `ref-tranquility-control.wav` | Tranquility Control | Karen Savage | [The Scarlet Pimpernel, ch. 1 (Orczy)](https://archive.org/details/scarlet_pimpernel_ks_librivox) | public domain | chosen (round 19) |
| `ref-crawler-one.wav` | Crawler One | Kara Shallenberg | [Heidi, introduction (Spyri)](https://archive.org/details/heidi_solo_librivox) | public domain | chosen (round 19) |
| `ref-convoy.wav` | Convoy | Adrian Praetzellis | [Treasure Island, front matter (Stevenson)](https://archive.org/details/treasure_island_ap_librivox) | public domain | chosen (round 19) |
| `ref-hammer-lead.wav` | Hammer Lead | Gord Mackenzie | [Scaramouche, book 1 ch. 1 (Sabatini)](https://archive.org/details/scaramouche_gm_librivox) | public domain | chosen (round 19) |
| `ref-driver-control.wav` | Driver Control | Alex Foster | [The Invisible Man, ch. 1–2 (Wells)](https://archive.org/details/invisible_man_librivox) | public domain | chosen (round 21) |

Round 19's chosen candidates were renamed to `ref-<speaker>.wav`; the rejected candidates and
round 18's Tranquility Control clip (David Leeson), which no speaker uses any more, were deleted
with their CREDITS.md rows (their sources and cuts stay in `tools/concept/audio/tts_r19.py` and
`tts_r18.py`, which rebuild them).

## Implementation

- [x] Main cast clips (Okafor, Varga, Rook) in the repo, in CREDITS.md
- [x] One clip per generic speaker, cast in [round 19](../../../concept-rounds/round-19/README.md)
- [x] Driver Control's clip, cast in [round 21](../../../concept-rounds/round-21/README.md); the chosen
      candidate renamed `ref-driver-control.wav`, the other deleted with its CREDITS.md row

## Decisions

- 2026-10-03: Created: the round 18 clips moved here from `~/.cache/tv-tts/refs/`; the round 19
  candidates added (licences checked on each archive.org item's `licenseurl`).
- 2026-10-03: Round 19 closed: one clip per generic speaker (convoy a, crawler one b, dock b,
  Hammer Lead b, Ring Control b, Tranquility Control b, Yard Control a); the rejected candidates
  and `ref-control.wav` deleted with their CREDITS.md rows.
- 2026-10-04: Round 21 candidates for Driver Control added (licences checked on each archive.org
  item's `licenseurl`).
- 2026-10-04: Round 21 decided (user): Driver Control is Alex Foster (a), renamed
  `ref-driver-control.wav`; Rebecca's candidate (b) deleted with its CREDITS.md row (its source and
  cut stay in `tools/concept/audio/tts_r21.py`).
