---
title: Concept round 02 — parallax, recorded SFX, music themes
design: approved
implementation: n/a
art: chosen
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
| 1 | Parallax approach | A Earth orbit, fuller and faster (190 px/s) · B night megacity, calm (140 px/s) · C Mars canyon, balanced density (160 px/s). Pick the density/speed you like; all settings will follow it | [art-direction](../../art-direction/README.md) | all three liked — density follows level pace; add decoration (heavier clouds/mist/dust, vegetation) → round 03 |
| 2 | Revised layer model | New `far` layer, faster deep/low-air/high-air factors, ground speed guideline 120–240 px/s, density guideline (≈4–6 enemies, ≤15 bullets) | [art-direction](../../art-direction/README.md) | adopted with the scenes |
| 3 | Player shots (recorded) | A projectile · B sci-fi gun · C laser (CC-BY) · D machine-gun single shot · E autocannon — several may be kept, one per weapon type | [audio/sfx](../../audio/sfx/README.md) | all kept as a starting set; need a distinct sound per weapon type → round 03 |
| 4 | Explosions (recorded) | A small · B small–medium heavy · C medium · D large with debris (CC-BY) · E big boss — several may be kept, one per size | [audio/sfx](../../audio/sfx/README.md) | all kept; need more, from small pops to large booms → round 03 |
| 5 | Title theme | "Terran Vanguard" — heroic, orchestral + tracker, humanity's motif | [audio/music](../../audio/music/README.md) | chosen |
| 6 | Hangar theme | "Dry Dock" — swung downtempo tracker groove | [audio/music](../../audio/music/README.md) | chosen |
| 7 | Boss theme | "The Choir Descends" — Vrell boss, choir motif, 150 BPM | [audio/music](../../audio/music/README.md) | chosen |
| 8 | Mars level theme (Act 3) | "Red Dust Run" — phrygian-dominant breakbeat | [audio/music](../../audio/music/README.md) | chosen |
| 9 | Europa level theme (Act 4) | "Thera Deep" — under-water ambient trance | [audio/music](../../audio/music/README.md) | chosen |
| 10 | Rook's craft colours | A "Ember" dark slate + orange · B "Jade" pale lime + green | [player/wingmen](../../player/wingmen/README.md) | **A** Ember |
| 11 | Ship A at 48×48 | Confirm the re-render in palette B | [player/ship](../../player/ship/README.md) | confirmed |
| 12 | HUD A at 960×540 | Confirm the re-layout with 240 px panels | [ui/hud](../../ui/hud/README.md) | confirmed |

## Decisions

- 2026-09-30: Round opened.
- 2026-09-30: User answered all choices; follow-ups (parallax decoration, per-weapon shots, more explosions) go to round 03. Round closed.
