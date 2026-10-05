---
title: Concept rounds
design: review
implementation: n/a
art: chosen
updated: 2026-10-05
---

# Concept rounds

## Summary

A concept round is a batch of proposals (images, audio, text variants) that Claude puts in
front of the user to choose from. This directory is the inbox: every open round lists the
choices still to be made. The proposals themselves live in the `concept/` directory of the part
they belong to; a round only collects them.

## Contents

| Part | Summary | Design | Impl | Art |
|---|---|---|---|---|
| [round-01](round-01/README.md) | Style exploration: ship, palette, parallax, HUD, title, first SFX and music, story twist | approved | n/a | chosen |
| [round-02](round-02/README.md) | Parallax redo, recorded shots and explosions, five music themes, ship and HUD at 960×540, Rook's craft | approved | n/a | chosen |
| [round-03](round-03/README.md) | Decoration pass, weapon shot families, explosion ladder, enemy concepts, briefing portraits, five music themes | approved | n/a | chosen |
| [round-04](round-04/README.md) | Enemy colour pass, portraits with more colour, beam loops and remaining SFX | approved | n/a | chosen |
| [round-05](round-05/README.md) | Enemy variety: size range, multi-part serpent and leviathan, walkers, spinners, tank and mech, swirl and rear-attack movement | approved | n/a | chosen |
| [round-06](round-06/README.md) | More enemies and bosses, five setting scenes, main menu and hangar/shop screens | approved | n/a | chosen |
| [round-07](round-07/README.md) | Europa under water, Harbour Kraken, Halo Platform rotation, Driftjelly waterline, hangar in glass style | approved | n/a | chosen |
| [round-08](round-08/README.md) | Acts 1–2 completion: Earth scenes, combat effects, remaining UI screens, portraits, re-renders, music cues, SFX | approved | n/a | chosen |
| [round-09](round-09/README.md) | Remaining Acts 1–2 scenes and combat effects, beam impact | approved | n/a | chosen |
| [round-10](round-10/README.md) | Geneva redo, calmer and smoother storm and ocean | approved | n/a | chosen |
| [round-11](round-11/README.md) | Production track, first batch: launch rail, edge warnings and tones, Coalition Rising stems, Level 01 backdrop fixes | approved | n/a | chosen |
| [round-12](round-12/README.md) | Level 01 production batch: final sprites, effects, loot targets, backdrop and the recorded SFX rebuilt from the originals | approved | n/a | chosen |
| [round-13](round-13/README.md) | UI production batch: HUD, glass kit and screens, portraits with expressions, briefing images, intel pictures, themes, fonts, the north-arm fix | approved | n/a | chosen |
| [round-14](round-14/README.md) | M4 part A: the Act 1 arsenal's effects and the HUD's weapon rows | approved | n/a | chosen |
| [round-15](round-15/README.md) | M4 part B: Level 02's Stinger, Spine Turret, pickups, backdrop, Crane Four and Afterburner stems | approved | n/a | chosen |
| [round-16](round-16/README.md) | M4 part C: Level 03's Spore Bomber, Whirl Seed, debris, Leviathan, backdrop, large salvage, intel pictures and game captures | approved | n/a | chosen |
| [round-17](round-17/README.md) | M4 part D: Level 04's civilian crawler, Airstrike bomber, Luna backdrop, intel portraits, dugout and supply drop concepts, game captures and part D numbers | approved | n/a | chosen |
| [round-18](round-18/README.md) | Text-to-speech for the radio lines: Chatterbox chosen of five engines, radio filter b (more static) | approved | n/a | chosen |
| [round-19](round-19/README.md) | Casting the generic radio speakers of Act 1: two LibriVox reference voices each, through Chatterbox and filter b | approved | n/a | chosen |
| [round-20](round-20/README.md) | M4 briefing images: the four images of Levels 03 and 04, one per briefing page | approved | n/a | chosen |
| [round-21](round-21/README.md) | M4 part E: Level 05's Gorgon Frigate, Polyp Mortar and its death, sled, prop and sound concepts, briefing images, Driver Control audition, capture and part E numbers | approved | n/a | chosen |
| [round-22](round-22/README.md) | Level 05's own backdrop: the crater rims, battery patches, arena floor and burning nest | approved | n/a | chosen |
| [round-23](round-23/README.md) | M4 part F: Level 06's Mantis, Coilwyrm, darkness glows and light shapes, backdrop, intel portraits, beam, prop and sound concepts, perimeter beacon audition, capture and part F numbers | approved | n/a | chosen |
| [round-24](round-24/README.md) | The Coilwyrm's death: a ripple of wet bursts head to tail, 0.25 s apart; head burst c and segment burst b chosen | approved | n/a | chosen |
| [round-25](round-25/README.md) | M4 part G: Level 07's Brood Carrier, its turn, death and carcass, backdrop, lifeboat tow and sound concepts, Lifeboat Seven audition, tracks 18/22/24, briefing and outro images, voiced lines, master limiter, capture and part G numbers | approved | n/a | chosen |
| [round-26](round-26/README.md) | M4 part H, the Act 1 close-out: Vrell deaths, Smart Bomb, boss bar, title-card still, ship flames and damage, shield ring, shadows, medium bullet, HUD warnings, test fire, credits roll, music finals, wired SFX, low-armour line, Varga's intel lines, part H numbers and the L01–L06 voice listen-through | review | n/a | proposed |

## Design

How a round works:

1. Claude generates variants into the parts' `concept/` directories (`<subject>-rRR-<variant>`)
   and writes `concept/prompts.md` entries for them.
2. Claude writes `round-RR/README.md` with a **Choices** table (one row per decision) and builds
   the review board: `python3 tools/concept/board.py RR` → `round-RR/index.html`.
3. The user opens `index.html` in a browser, listens and looks, and answers per choice (pick a
   variant, combine, or ask for another iteration).
4. Claude records the outcome: chosen files stay, rejected files move to `concept/rejected/`,
   the part's Concept art table, `art` status and Decisions log are updated, and the round's
   Choices table gets the outcome. When every choice is answered, the round's `design` status
   becomes `approved` and it is marked closed below.

## Rounds

| Round | Opened | Status | Topic |
|---|---|---|---|
| 01 | 2026-09-30 | closed | Style exploration |
| 02 | 2026-09-30 | closed | Parallax redo, recorded shots/explosions, five music themes, ship at 960×540, Rook's craft |
| 03 | 2026-09-30 | closed | Decoration, enemies, portraits, weapon SFX, explosions, more music |
| 04 | 2026-09-30 | closed | Enemy colours, portrait colour, beam sounds |
| 05 | 2026-09-30 | closed | Enemy variety: sizes, multi-part, walkers, spinners, movement |
| 06 | 2026-10-01 | closed | More enemies and bosses, setting scenes, menu and shop |
| 07 | 2026-10-01 | closed | Water, rotation and hangar fixes |
| 08 | 2026-10-01 | closed | Acts 1–2 completion |
| 09 | 2026-10-01 | closed | Remaining Acts 1–2 scenes and effects |
| 10 | 2026-10-01 | closed | Geneva, storm and ocean revisions |
| 11 | 2026-10-02 | closed | Launch rail, edge warnings, music stems, Level 01 backdrop fixes |
| 12 | 2026-10-02 | closed | Level 01 production batch: final art review, recorded SFX rebuilt from the originals |
| 13 | 2026-10-02 | closed | UI production batch: final art review of U1–U3 |
| 14 | 2026-10-02 | closed | M4 part A: the Act 1 arsenal's effects (final art review) |
| 15 | 2026-10-02 | closed | M4 part B: Level 02 (final art review) |
| 16 | 2026-10-02 | closed | M4 part C: Level 03 (final art review) |
| 17 | 2026-10-03 | closed | M4 part D: Level 04 (final art review, dugout and supply drop concepts, part D numbers) |
| 18 | 2026-10-03 | closed | Text-to-speech for the radio lines: Chatterbox, radio filter b (more static) |
| 19 | 2026-10-03 | closed | Casting the generic radio speakers of Act 1 (Chatterbox, filter b): one reader per speaker |
| 20 | 2026-10-03 | closed | M4 briefing images: Levels 03 and 04, one per briefing page |
| 21 | 2026-10-04 | closed | M4 part E: Level 05 production art, prop and sound concepts, Driver Control audition |
| 22 | 2026-10-04 | closed | Level 05 backdrop (final art review) |
| 23 | 2026-10-04 | closed | M4 part F: Level 06 production art, beam, prop and sound concepts, perimeter beacon audition |
| 24 | 2026-10-05 | closed | The Coilwyrm's death bursts (four sound options, the ripple timing) |
| 25 | 2026-10-05 | closed | M4 part G: Level 07 and the act end (final art review, lifeboat and sound concepts, Lifeboat Seven audition, part G numbers) |
| 26 | 2026-10-05 | open | M4 part H: the Act 1 close-out (final art review, captures, music finals, SFX, low-armour line, intel lines, part H numbers, voice listen-through) |
