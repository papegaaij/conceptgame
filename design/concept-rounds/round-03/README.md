---
title: Concept round 03 — decoration, enemies, portraits, more audio
design: approved
implementation: n/a
art: chosen
updated: 2026-09-30
---

# Concept round 03 — decoration, enemies, portraits, more audio

## Summary

Follow-ups from [round 02](../round-02/README.md) — a decoration pass on the three parallax
scenes, a shot sound per weapon family and a full explosion ladder — plus the first enemy
concept art, briefing portraits and five more music themes. Open [index.html](index.html) in
a browser (regenerate with `python3 tools/concept/board.py 03`).

Reused round-02 sounds are not repeated on this board; the weapon-family and explosion-ladder
tables in [audio/sfx](../../audio/sfx/README.md) show which r02 file fills which slot. The
portrait faces are procedural placeholders: choose style, framing and colour treatment; the
polished portraits will come from the prompts in each character's `concept/prompts.md`.

## Choices

| # | Choice | Variants | Part | Outcome |
|---|---|---|---|---|
| 1 | Decoration pass | A orbit: cyclone + cloud decks · B city: trees, parks, lamp-lit fog · C canyon: dust storms, greenhouses, lichen — is this the right amount? | [art-direction](../../art-direction/README.md) | chosen — the heavy end; intensity must vary through a level (rule added) |
| 2 | Vrell design language | A "sleek chitin" (smooth, glossy, teal seams) · B "armoured brood" (plates, claws, spikes) · or a mix (e.g. B for ground/heavy, A for fliers). Compare Skitter, Needler, Spine Turret | [enemies](../../enemies/README.md) | mainly **A**, B where it adds character (Needler B) |
| 3 | Act 1 Vrell set | Skitter, Needler, Stinger, Spore Bomber, Brood Pod, Mantis, Spine Turret, Polyp Mortar | [enemies](../../enemies/README.md) | Skitter A, Needler B, Stinger, Spore Bomber, Brood Pod, Mantis, Spine Turret A, Polyp Mortar — more distinct colours needed → round 04 |
| 4 | Ascendancy look | Talon, Gilded Gunship, Rail Bunker — black & gold; proposed: a thin gold/red rim light so black hulls read on dark backgrounds | [enemies](../../enemies/README.md) | Talon, Gilded Gunship, Rail Bunker chosen; rim-light proposal not yet answered |
| 5 | Brood Carrier (Act 1 boss) | Multi-part boss, ~1 screen long (the roster said two — proposal: keep one) | [enemies/bosses](../../enemies/bosses/README.md) | chosen as shown |
| 6 | Portrait style | A pre-rendered 3D bust · B comm-screen pixel portrait (see the cast sheets) | [story/characters](../../story/characters/README.md) | **B**, with a bit more colour retained → round 04 |
| 7 | Weapon shot families | 12 families (pulse, vulcan, ballistic, laser, beam loop, missile, micro-missile, mortar, bomb, torpedo, mine, tesla, resonator) — keep, replace any? | [audio/sfx](../../audio/sfx/README.md) | all kept except both beam loops → new beam sources in round 04 |
| 8 | Explosion ladder | tiny ×3, small, medium, large, huge ×2, under-water ×2, water surface — keep, replace any? | [audio/sfx](../../audio/sfx/README.md) | all kept except huge-a, medium-a, tiny-c, underwater-b |
| 9 | Earth theme (Act 2) | "Homefront" | [audio/music](../../audio/music/README.md) | chosen |
| 10 | Belt theme (Act 5) | "Hollow Rock" — Vorne's motif surfaces | [audio/music](../../audio/music/README.md) | chosen |
| 11 | Jovian theme (Act 6) | "Eye of the Storm" | [audio/music](../../audio/music/README.md) | chosen |
| 12 | Ascendancy boss theme | "Iron Sovereign" — martial, no choir | [audio/music](../../audio/music/README.md) | chosen |
| 13 | Final boss theme | "Choir Heart" | [audio/music](../../audio/music/README.md) | chosen |

## Decisions

- 2026-09-30: Round opened.
- 2026-09-30: User answered all choices; follow-ups (enemy colours, portrait colour, beam sounds) go to round 04. Round closed.
