---
title: Concept rounds
design: review
implementation: n/a
art: proposed
updated: 2026-09-30
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
| [round-08](round-08/README.md) | Acts 1–2 completion: Earth scenes, combat effects, remaining UI screens, portraits, re-renders, music cues, SFX | review | n/a | proposed |

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
| 08 | 2026-10-01 | open | Acts 1–2 completion |
