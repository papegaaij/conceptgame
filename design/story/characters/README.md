---
title: Characters
design: draft
implementation: n/a
art: none
updated: 2026-09-30
---

# Characters

## Summary

The cast the player meets in briefings, radio chatter and enemy transmissions. Small on
purpose: six voices, each instantly recognisable by portrait and speech style.

## Contents

| Part | Summary | Design | Impl | Art |
|---|---|---|---|---|
| [lancer](lancer/README.md) | The player: silent CDF pilot of the AF-12 Stormhawk | draft | n/a | n/a |
| [okafor](okafor/README.md) | Commander Adaeze Okafor, Aegis Wing CO, gives the briefings | draft | n/a | none |
| [rook](rook/README.md) | Lt. Kenji "Rook" Tanaka, the AI wingman, banter and warnings | draft | n/a | none |
| [varga](varga/README.md) | Dr. Elena Varga, intel officer and xenobiologist, hangar intel | draft | n/a | none |
| [vorne](vorne/README.md) | Chairman Silas Vorne, leader of the Ascendancy, the villain | draft | n/a | none |
| [the-choir](the-choir/README.md) | The collective voice of the Vrell | draft | n/a | none |

## Design

### Portrait rules (for all characters)

- Portraits are **pre-rendered 3D busts**, late-90s CGI style (think Wing Commander IV /
  Colony Wars briefing portraits), shown in a square frame in the HUD side panel and larger on
  briefing screens.
- Head-and-shoulders, three-quarter view facing the play field (left panel faces right, right
  panel faces left; mirror if needed).
- Each character has a neutral portrait plus 1–2 expressions (e.g. urgent, grim, smug).
- Frame colour identifies the side: CDF navy/blue, Ascendancy black/gold, Vrell teal static.
- Enemy transmissions add scanlines and static distortion.

### Who speaks when

| Situation | Speaker |
|---|---|
| Act and mission briefings | Okafor (Varga for intel-heavy missions, Vorne in Act 6 taunts) |
| Hangar intel panel | Varga |
| Wave warnings, banter, "on your six" | Rook |
| Low armour, objective changes | Okafor or Rook |
| Boss appears | Varga (analysis), Vorne or the Choir (transmission) |
| Enemy broadcasts | Vorne, the Choir |

## Decisions

- 2026-09-30: Six-character cast; pre-rendered 3D bust portraits with side-coded frames.
