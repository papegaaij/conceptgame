---
title: Concept round 08 — Acts 1–2 completion
design: approved
implementation: n/a
art: chosen
updated: 2026-10-01
---

# Concept round 08 — Acts 1–2 completion

## Summary

What was produced towards Acts 1–2 before the batch was wrapped up early to save tokens. Open
[index.html](index.html) in a browser (regenerate with `python3 tools/concept/board.py 08`).
Not yet produced (code mostly written, see `tools/concept/README.md`): the storm, arctic,
Geneva and Luna far side scenes; explosions, enemy bullets, pickups, specials, edge warnings
and Rook's banking frames.

## Choices

| # | Choice | Variants | Part | Outcome |
|---|---|---|---|---|
| 1 | Open ocean scene (L11) | Convoy, wakes, surfaced and submerged jellies | [art-direction](../../art-direction/README.md) | deferred — finish the scene first → round 09 |
| 2 | Projectiles | 13 families with muzzle flashes, impacts and L1/L3/L5 patterns | [player/weapons](../../player/weapons/README.md) | accepted; beam needs an impact effect → round 09 |
| 3 | Ship banking and wing pods | 5 banking frames, five pod types on the wings | [player/ship](../../player/ship/README.md) | accepted |
| 4 | UI screens | Briefing, act title, debrief, mission failed, game over, pause, options, credits | [ui](../../ui/README.md) | accepted |
| 5 | HUD refresh | Escort box, overdrive, boss bar, edge warning (metal, unchanged style) | [ui/hud](../../ui/hud/README.md) | accepted |
| 6 | UI kit and fonts | Glass and metal widget kit; bitmap fonts 8×12 / 10×20 / 20×30 | [ui](../../ui/README.md) | accepted |
| 7 | Portraits | The Choir glyph, generic CDF officer, generic civilian | [story/characters](../../story/characters/README.md) | accepted |
| 8 | Re-renders | Scuttler, Coilwyrm, Leviathan with seams that turn with the body | [enemies](../../enemies/README.md) | accepted |
| 9 | Music | 8 cues (briefing, Act 2 B, mini-boss sting, boss warning, mission complete, act complete, mission failed, game over) and full-length versions of title, hangar, Afterburner, Coalition Rising, Homefront, The Choir Descends | [audio/music](../../audio/music/README.md) | accepted |
| 10 | Sound effects | 61 sounds: hits, shield/armour, enemy shots, screeches, Vrell spawns, specials, pickups by type, UI (incl. equip, upgrade, grade stamp) and radio, six ambience loops — verified and fixed in a completion pass | [audio/sfx](../../audio/sfx/README.md) | accepted except screech a+b, shield hit b, shield restore b, klaxon b (rejected) |

## Decisions

- 2026-10-01: Round opened.
- 2026-10-01: Batch wrapped up early at safe points to save tokens; remaining items listed above.
- 2026-10-01: User answered all choices; the ocean scene and the beam impact carry over to round 09. Round closed.
