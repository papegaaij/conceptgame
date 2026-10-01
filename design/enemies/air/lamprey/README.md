---
title: Lamprey
design: approved
implementation: not-started
art: chosen
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-01
---

# Lamprey

## Summary

A rust eel with a teal-toothed mouth disc that homes in, latches onto the Stormhawk and drains its shield until shaken off with hard left–right movement.

## Design

### Stat block

Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (a Pulse Cannon L1 shot does 2); bullet damage classes and speeds are defined in the balancing basis.

| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `air` |
| Size tier | `small` |
| Size | 36×36 px, hitbox 20×28 |
| Parts | single |
| Orientation | 16 angles (4 swim frames, latched pose) |
| HP | 3 (easy 2 / hard 4, from the global multipliers); ×2 damage taken while latched |
| Armour / shield | none |
| Speed | 220 px/s, turn rate 180°/s |
| Movement | `chase` the player for up to 4 s, then `latch` on contact |
| Attack | `drain`: 8 shield points/s while latched (no armour damage, does not slow the ship); the player shakes it off with 4 direction reversals within 1.2 s, or by shooting it with side/rear/area fire |
| Formations | stream, swarm (3–6) |
| Weak points | mouth disc (×1.5) |
| Effective traits | `homing`, `side`, `rear`, `area` |
| Credits | 12 (score 120 × chain) |
| Death | `small` wet pop |
| First level / used in | L12; Act 2 |
| Difficulty hooks | hard: drain 12/s; up to 2 latched at once (easy and medium: 1) |

### Behaviour

- A shaken-off Lamprey is stunned for 1 s and drifts, an easy kill.
- While a Lamprey is latched, the HUD shows SHIELD DRAIN and the shield bar flickers.

### Concept art

Chosen concept: [lamprey-r06-a.png](../concept/lamprey-r06-a.png), [lamprey-r06-a.gif](../concept/lamprey-r06-a.gif) (listed in the [air](../README.md#concept-art) Concept art table).

## Implementation

- [ ] Chase with turn-rate limit
- [ ] Latch, drain, shake-off by direction reversals
- [ ] No speed penalty while latched
- [ ] Stat block values loaded from data; global difficulty multipliers applied
- [ ] Death effect, bounty and score per this spec

## Decisions

- 2026-10-01: Promoted from the air roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: HP rescaled to the lowered L12 reference DPS (5 → 3, ×73/110).
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
