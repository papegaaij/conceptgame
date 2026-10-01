---
title: Polyp Mortar
design: review
implementation: not-started
art: chosen
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-01
---

# Polyp Mortar

## Summary

A slate acid mouth ringed by tentacles that lobs acid blobs at the player's position; the impact point is marked a second ahead and bursts into a small ring.

## Design

### Stat block

Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (1 = one Pulse Cannon L1 shot); bullet damage classes and speeds are defined in the balancing basis.

| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `ground` |
| Size tier | `small` |
| Size | 44×44 px, hitbox 32×32 |
| Parts | single |
| Orientation | `fixed` |
| HP | 10 (easy 8 / hard 13, from the global multipliers) |
| Armour / shield | none |
| Speed | scrolls with the ground |
| Movement | `terrain` |
| Attack | `mortar` every 3.5 s at the player's position: lime impact marker shown 1.0 s ahead; the blob lands and bursts into an 8-bullet `ring` (110 px/s, `small` = 4); a direct hit by the blob deals `heavy` = 10 |
| Formations | turret nest (pairs or with Spine Turrets) |
| Weak points | lime mouth (×1.5) |
| Effective traits | `anti-ground` (×2), `area` |
| Credits | 15 (score 150 × chain) |
| Death | `small` wet burst; acid splash decal |
| First level / used in | L05; Acts 1–2 |
| Difficulty hooks | hard: 12-bullet ring |

### Behaviour

- The marker tracks the player's position at launch, not during flight: keep moving.

### Concept art

Chosen concept: [polyp-mortar-r04-a.png](../concept/polyp-mortar-r04-a.png) (listed in the [ground](../README.md#concept-art) Concept art table).

## Implementation

- [ ] Mortar lob with 1 s marker
- [ ] Ring on impact
- [ ] Direct-hit damage
- [ ] Stat block values loaded from data; global difficulty multipliers applied
- [ ] Death effect, bounty and score per this spec

## Decisions

- 2026-10-01: Promoted from the ground roster to a full spec for the Acts 1–2 wrap-up.
