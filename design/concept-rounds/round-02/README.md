---
title: Concept round 02 — parallax, recorded SFX, music themes
design: review
implementation: n/a
art: proposed
updated: 2026-09-30
---

# Concept round 02 — parallax, recorded SFX, music themes

## Summary

Follow-up to [round 01](../round-01/README.md), at the new 960×540 resolution with the chosen
palette B, ship A and HUD A. Open [index.html](index.html) in a browser to see and hear
everything (regenerate with `python3 tools/concept/board.py 02`). The GIFs show the parallax
scroll in motion; the PNG sheets show the layer breakdown.

Shots and explosions are now **recorded sounds from Freesound** (CC0 / CC-BY, listed in
[CREDITS.md](../../../CREDITS.md)), trimmed and levelled by `tools/concept/audio/import_sfx.py`.
They are the public preview files; production assets will be rebuilt from the originals.

## Choices

| # | Choice | Variants | Part | Outcome |
|---|---|---|---|---|
| 1 | Parallax approach | A Earth orbit, fuller and faster (190 px/s) · B night megacity, calm (140 px/s) · C Mars canyon, balanced density (160 px/s). Pick the density/speed you like; all settings will follow it | [art-direction](../../art-direction/README.md) | open |
| 2 | Revised layer model | New `far` layer, faster deep/low-air/high-air factors, ground speed guideline 120–240 px/s, density guideline (≈4–6 enemies, ≤15 bullets) | [art-direction](../../art-direction/README.md) | open |
| 3 | Player shots (recorded) | A projectile · B sci-fi gun · C laser (CC-BY) · D machine-gun single shot · E autocannon — several may be kept, one per weapon type | [audio/sfx](../../audio/sfx/README.md) | open |
| 4 | Explosions (recorded) | A small · B small–medium heavy · C medium · D large with debris (CC-BY) · E big boss — several may be kept, one per size | [audio/sfx](../../audio/sfx/README.md) | open |
| 5 | Title theme | "Terran Vanguard" — heroic, orchestral + tracker, humanity's motif | [audio/music](../../audio/music/README.md) | open |
| 6 | Hangar theme | "Dry Dock" — swung downtempo tracker groove | [audio/music](../../audio/music/README.md) | open |
| 7 | Boss theme | "The Choir Descends" — Vrell boss, choir motif, 150 BPM | [audio/music](../../audio/music/README.md) | open |
| 8 | Mars level theme (Act 3) | "Red Dust Run" — phrygian-dominant breakbeat | [audio/music](../../audio/music/README.md) | open |
| 9 | Europa level theme (Act 4) | "Thera Deep" — under-water ambient trance | [audio/music](../../audio/music/README.md) | open |
| 10 | Rook's craft colours | A "Ember" dark slate + orange · B "Jade" pale lime + green | [player/wingmen](../../player/wingmen/README.md) | open |
| 11 | Ship A at 48×48 | Confirm the re-render in palette B | [player/ship](../../player/ship/README.md) | open |
| 12 | HUD A at 960×540 | Confirm the re-layout with 240 px panels | [ui/hud](../../ui/hud/README.md) | open |

## Decisions

- 2026-09-30: Round opened.
