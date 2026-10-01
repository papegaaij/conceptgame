---
title: Needler
design: approved
implementation: not-started
art: chosen
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-01
---

# Needler

## Summary

The basic Vrell gunner: an ivory crab-like flyer (design language B) that hovers or swoops and fires slow aimed thorns. The first enemy that shoots back.

## Design

### Stat block

Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (1 = one Pulse Cannon L1 shot); bullet damage classes and speeds are defined in the balancing basis.

| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `air` |
| Size tier | `small` |
| Size | 36×36 px, hitbox 26×24 |
| Parts | single |
| Orientation | `fixed` |
| HP | 4 (easy 3 / hard 5, from the global multipliers) |
| Armour / shield | none |
| Speed | 120 px/s entering, 0 while hovering |
| Movement | `swoop` in, `hover` 2–4 s at y = 80–220 px, then exit down or to the side; in `circle` formations `orbit` a point (radius 90 px, 60°/s) |
| Attack | `aimed` yellow thorn every 2.5 s, 150 px/s, damage `small` = 4; first shot 0.8 s after it stops |
| Formations | V-wing (5), line abreast, pincer, circle (8) |
| Weak points | glowing violet eye cluster (×1.5) |
| Effective traits | `forward`, `spread` |
| Credits | 12 (score 120 × chain) |
| Death | `small` burst: ivory shards, violet flash |
| First level / used in | L01; recurring gunner through Acts 1–2 and spawned by the Brood Carrier |
| Difficulty hooks | hard: selected Needlers in `circle` formations lead the target |

### Behaviour

- The first shot is delayed after the Needler stops so a new player can kill it before it fires.
- Thorns are drawn yellow (needle colour) per the bullet readability rules.

### Concept art

Chosen concept: [needler-r04-a.png](../concept/needler-r04-a.png) (listed in the [air](../README.md#concept-art) Concept art table).

## Implementation

- [ ] Hover-and-fire behaviour with the 0.8 s first-shot delay
- [ ] Circle formation orbiting and breaking off
- [ ] Thorn bullet: yellow needle, 150 px/s
- [ ] Stat block values loaded from data; global difficulty multipliers applied
- [ ] Death effect, bounty and score per this spec

## Decisions

- 2026-10-01: Promoted from the air roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
