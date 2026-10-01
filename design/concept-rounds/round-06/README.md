---
title: Concept round 06 — more enemies and bosses, setting scenes, menu and shop
design: approved
implementation: n/a
art: chosen
updated: 2026-10-01
---

# Concept round 06 — more enemies and bosses, setting scenes, menu and shop

## Summary

Concepts for the Act 2 enemies and the remaining round-05 roster additions, the Act 1 and
Act 2 mid-bosses and the Act 2 boss, parallax scenes for five more settings at `medium`
atmosphere, and the first main-menu, difficulty, load-game and hangar/shop screens. Open
[index.html](index.html) in a browser — the enemy, boss and scene GIFs show the motion
(regenerate with `python3 tools/concept/board.py 06`).

Notes for review:
- All boss weak points glow lime, following the Brood Carrier (proposed convention).
- The Wraith launched by the Siege Spire is bone/violet, while the Wraith's own sheet is rust
  with blue-violet veins; whichever you prefer becomes the Wraith's colour everywhere.
- The Driftjelly and Spiral Nautilus in the Europa scene are separate models from their enemy
  sheets; the chosen sheet design wins.
- The scene GIFs posterize the terrain to stay under 8 MB; the PNG sheets show full colour.

## Choices

| # | Choice | Variants | Part | Outcome |
|---|---|---|---|---|
| 1 | Act 2 enemies | Creeper (salamander walker), Hive Node, Wraith (manta ghost, rear decloak), Lamprey (latching eel), Driftjelly, Reef Spitter, Skimmer | [enemies](../../enemies/README.md) | all chosen |
| 2 | Round-05 additions | Threadcrawler (centipede chain), Halo Platform (Ascendancy turret ring), Dust Devil (vortex organism), Spiral Nautilus (rolling shell) | [enemies](../../enemies/README.md) | Threadcrawler, Dust Devil, Spiral Nautilus chosen; Halo Platform too jagged → round 07 |
| 3 | Gorgon Frigate (Act 1 mid-boss) | Medusa-bell warship with serpent-neck turrets | [enemies/bosses](../../enemies/bosses/README.md) | chosen |
| 4 | Harbour Kraken (Act 2 mid-boss) | Cephalopod around an offshore platform, telegraphed arm slams | [enemies/bosses](../../enemies/bosses/README.md) | rejected — doesn't surface from the depths, detached arms, circle waves → round 07 |
| 5 | Siege Spire (Act 2 boss) | Rooted citadel that tears free and rises | [enemies/bosses](../../enemies/bosses/README.md) | chosen |
| 6 | Boss weak-point colour | Lime on all bosses (as the Brood Carrier) | [enemies](../../enemies/README.md) | lime for all bosses — confirmed |
| 7 | Setting scenes | Luna · Europa under water · asteroid belt · Jupiter (Aurelia) · Vrell space | [art-direction](../../art-direction/README.md) | Luna, belt, Jupiter, Vrell space chosen; Europa not recognisably under water → round 07 |
| 8 | Main menu | A glass menu over a hero scene · B full-screen metal console (with matching difficulty and load-game screens) | [ui/main-menu](../../ui/main-menu/README.md) | **A** for main menu, difficulty and load game |
| 9 | Hangar / shop | A three columns (loadout / shop / intel) · B central ship schematic | [ui/hangar](../../ui/hangar/README.md) | layout **B** in the menu's glass style → round 07 |

## Decisions

- 2026-10-01: Round opened.
- 2026-10-01: User answered; follow-ups (Europa, Kraken, Halo Platform, Driftjelly waterline, hangar restyle) go to round 07. Round closed.
