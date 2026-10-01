---
title: Spore Bomber
design: review
implementation: not-started
art: chosen
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-01
---

# Spore Bomber

## Summary

A slow olive gas-bag on the `low-air` layer that drops drifting spore mines; the mines rise to the player's plane after a second. Spread fire clears them.

## Design

### Stat block

Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (1 = one Pulse Cannon L1 shot); bullet damage classes and speeds are defined in the balancing basis.

| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `low-air` (bomber); its spore mines rise to `air` |
| Size tier | `medium` |
| Size | 72×72 px, hitbox 52×56 |
| Parts | single |
| Orientation | `fixed` |
| HP | 24 (easy 18 / hard 31, from the global multipliers) |
| Armour / shield | none |
| Speed | 45 px/s |
| Movement | `straight` down the screen or `strafe` across at y = 100–300 px |
| Attack | `mine`: drops a spore every 1.6 s; spores drift 20 px/s in a random direction, rise to the player plane after 1.0 s (lime glow brightens), burst on contact (damage `medium` = 6) or after 8 s into a 6-bullet `ring` (90 px/s, damage `small` = 4). Spores are shootable, 1 HP |
| Formations | line abreast (3), convoy |
| Weak points | lime spore bulbs (×1.5) |
| Effective traits | `spread`, `area` |
| Credits | 25 (score 250 × chain); spores 1 each |
| Death | `medium` burst: olive membrane tatters, lime spore cloud (harmless) |
| First level / used in | L03; Act 1 and Act 2 area denial |
| Difficulty hooks | hard: spores burst into 8-bullet rings; easy: spores never burst on their own |

### Behaviour

- Being on `low-air`, the bomber does not collide with the player and is hit by all weapons.
- Spores on the low layer (first second) are not yet dangerous and cannot be hit; this teaches the layer model.

### Concept art

Chosen concept: [spore-bomber-r04-a.png](../concept/spore-bomber-r04-a.png) (listed in the [air](../README.md#concept-art) Concept art table).

## Implementation

- [ ] Low-air rendering and no collision
- [ ] Spore mine lifecycle: drop, rise after 1 s, contact burst, timed ring
- [ ] Spores shootable for 1 credit
- [ ] Stat block values loaded from data; global difficulty multipliers applied
- [ ] Death effect, bounty and score per this spec

## Decisions

- 2026-10-01: Promoted from the air roster to a full spec for the Acts 1–2 wrap-up.
