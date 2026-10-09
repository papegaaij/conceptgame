---
title: Reference voices
design: draft
implementation: in-progress
art: chosen
depends-on: [..]
updated: 2026-10-09
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
| `ref-perimeter-beacon.wav` | Daedalus perimeter beacon | Mark F. Smith | [The Time Machine (version 2), ch. 1 (Wells)](https://archive.org/details/time_machine_ms_librivox) | CC0 1.0 | chosen (round 23) |
| `ref-lifeboat-seven.wav` | Lifeboat Seven | Tadhg Hynes | [Far From the Madding Crowd, ch. 1 (Hardy)](https://archive.org/details/far_from_the_madding_crowd_th_librivox) | public domain | chosen (round 25) |
| `ref-civilian.wav` | The Ikoyi shelter civilian (Level 08) | Faith Abiola-Ellison | [The Yoruba-speaking Peoples of the Slave Coast of West Africa, selections, section 1 (Ellis)](https://archive.org/details/yorubapeoples_2408_librivox) | public domain (Public Domain Mark 1.0) | chosen (round 30) |
| `ref-kilo-lead.wav` | Kilo Lead (Level 09) | Aaron Bennett | [On Mamba Station: U.S. Marines in West Africa, ch. 1 (Antal, Vanden Berghe)](https://archive.org/details/onmambastation_2507_librivox) | public domain (Public Domain Mark 1.0) | chosen (round 31) |
| `ref-lifeline.wav` | Lifeline: Lifeline One, Two, Four and Five and the hit line (Level 10) | KevinS | [Over the Ocean to Paris, ch. 1 (Dixon)](https://archive.org/details/overtheoceantoparis_2404_librivox) | public domain (Public Domain Mark 1.0) | chosen (round 32) |
| `ref-lifeline-three.wav` | Lifeline Three (Level 10) | Maria Kasper | [The Curtiss Aviation Book, ch. 1 (Curtiss)](https://archive.org/details/curtissaviationbook_2006_librivox) | public domain (Public Domain Mark 1.0) | chosen (round 32) |
| `ref-atlas-control.wav` | Atlas Control (Level 11) | MaryAnn | [Eighteen Months in the War Zone: A Record of a Woman's Work, ch. 1 (Finzi)](https://archive.org/details/eighteenmonthsinthewarzone_1409_librivox) | public domain (Public Domain Mark 1.0) | chosen (round 33) |

Round 19's chosen candidates were renamed to `ref-<speaker>.wav`; the rejected candidates and
round 18's Tranquility Control clip (David Leeson), which no speaker uses any more, were deleted
with their CREDITS.md rows (their sources and cuts stay in `tools/concept/audio/tts_r19.py` and
`tts_r18.py`, which rebuild them).

## Implementation

- [x] Main cast clips (Okafor, Varga, Rook) in the repo, in CREDITS.md
- [x] One clip per generic speaker, cast in [round 19](../../../concept-rounds/round-19/README.md)
- [x] Driver Control's clip, cast in [round 21](../../../concept-rounds/round-21/README.md); the chosen
      candidate renamed `ref-driver-control.wav`, the other deleted with its CREDITS.md row
- [x] The perimeter beacon's clip, cast in [round 23](../../../concept-rounds/round-23/README.md); the chosen
      candidate renamed `ref-perimeter-beacon.wav`, the other deleted with its CREDITS.md row
- [x] Lifeboat Seven's clip, cast in [round 25](../../../concept-rounds/round-25/README.md); the chosen
      candidate renamed `ref-lifeboat-seven.wav`, the other deleted with its CREDITS.md row
- [x] The Ikoyi shelter civilian's clip (Level 08), cast in [round 30](../../../concept-rounds/round-30/README.md);
      the chosen candidate renamed `ref-civilian.wav`, the other deleted with its CREDITS.md row
- [x] The Kilo Lead's clip (Level 09), cast in [round 31](../../../concept-rounds/round-31/README.md);
      the chosen candidate renamed `ref-kilo-lead.wav`, the other deleted with its CREDITS.md row
- [x] Lifeline's and Lifeline Three's clips (Level 10), cast in [round 32](../../../concept-rounds/round-32/README.md);
      the chosen candidates renamed `ref-lifeline.wav` and `ref-lifeline-three.wav`, the others
      deleted with their CREDITS.md rows
- [x] Atlas Control's clip (Level 11), cast in [round 33](../../../concept-rounds/round-33/README.md);
      the chosen candidate renamed `ref-atlas-control.wav`, the other deleted with its CREDITS.md row

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
- 2026-10-04: Round 23 candidates for the Daedalus perimeter beacon added (licences checked on each
  archive.org item's `licenseurl`).
- 2026-10-05: Round 23 decided (user): the perimeter beacon is Mark F. Smith (a), renamed
  `ref-perimeter-beacon.wav`; Lucy Burgoyne's candidate (b) deleted with its CREDITS.md row (its
  source and cut stay in `tools/concept/audio/tts_r23.py`).
- 2026-10-05: Round 25 candidates for Lifeboat Seven added (Tadhg Hynes, Lizzie Driver; licences
  checked on each archive.org item's `licenseurl`).
- 2026-10-05: Round 25 decided (user): Lifeboat Seven is Tadhg Hynes (a), renamed
  `ref-lifeboat-seven.wav`; Lizzie Driver's candidate (b) deleted with its CREDITS.md row (its
  source and cut stay in `tools/concept/audio/tts_r25.py`).
- 2026-10-05: Every item is ticked (Act 1's speakers all cast, the last in round 25), so the implementation is `done`.
- 2026-10-06: Round 30 candidates for the Ikoyi shelter civilian (Level 08) added (KirksVoice, Faith
  Abiola-Ellison; licences checked on each archive.org item's `licenseurl`: Public Domain Mark 1.0).
- 2026-10-07: Round 30 decided (user): the Ikoyi shelter civilian is Faith Abiola-Ellison (b),
  renamed `ref-civilian.wav`; KirksVoice's candidate (a) deleted with its CREDITS.md row (its
  source and cut stay in `tools/concept/audio/tts_r30.py`).
- 2026-10-07: Round 31 candidates for the Kilo Lead (Level 09) added (Aaron Bennett, tombooker;
  licences checked on each archive.org item's `licenseurl`: Public Domain Mark 1.0).
- 2026-10-07: Round 31 decided (user): the Kilo Lead is Aaron Bennett (a), renamed
  `ref-kilo-lead.wav`; tombooker's candidate (b) deleted with its CREDITS.md row (its source and
  cut stay in `tools/concept/audio/tts_r31.py`).
- 2026-10-08: Round 32 candidates for Lifeline (Atul Sharma, KevinS) and Lifeline Three (Kehinde,
  Maria Kasper) added (Level 10); licences checked on each archive.org item's `licenseurl`.
- 2026-10-08: Round 32 decided (user): Lifeline is KevinS (b), renamed `ref-lifeline.wav`, and
  Lifeline Three is Maria Kasper (b), renamed `ref-lifeline-three.wav`; Atul Sharma's and Kehinde's
  candidates (a) deleted with their CREDITS.md rows (their sources and cuts stay in
  `tools/concept/audio/tts_r32.py`).
- 2026-10-08: Round 33 candidates for Atlas Control (Level 11) added (Alister, MaryAnn; licences
  checked on each archive.org item's `licenseurl`: Public Domain Mark 1.0).
- 2026-10-09: Round 33 decided (user): Atlas Control is MaryAnn (b), renamed
  `ref-atlas-control.wav`; Alister's candidate (a) deleted with its CREDITS.md row (its source and
  cut stay in `tools/concept/audio/tts_r33.py`).
