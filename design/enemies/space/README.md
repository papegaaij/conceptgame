---
title: Space enemies
design: draft
implementation: not-started
art: none
updated: 2026-09-30
---

# Space enemies

## Summary

Vacuum-only enemies for space levels: the belt, the gate and Vrell space. On the `space` layer
they behave like `air` units (the player's plane) but never appear in atmosphere or water. Many
use the space environment: clinging to asteroids, laying mines, draining energy or bending space.

## Roster

| Name | Faction | Layer | Role/ability | Formations | First level | Design |
|---|---|---|---|---|---|---|
| Asteroid Mite | Vrell | space | Clings to drifting asteroids and leaps off at the player (`dive`) from the sides as they pass. | swarm (on asteroids) | 29 | idea |
| Minelayer | Ascendancy | space | Passes from the rear upward and leaves lines of proximity `mine`s ahead of the player. | column | 30 | idea |
| Shard Drone | Ascendancy | space | Groups of 3–4 connected by damaging `link` beams, forming moving barriers; killing one breaks its links. | line abreast, circle | 31 | idea |
| Void Leech | Vrell | space | `drift`s in from behind, `latch`es, and `drain`s **generator energy**, so heavy weapons stall. Punishes loadouts with little spare power. | stream, rear ambush | 33 | idea |
| Gate Warden | Vrell | space | Shielded sentinel. Its shield opens only while it fires a `laser-line`; `shield-breaker` strips it at any time. | line abreast | 43 | idea |
| Rift Skater | Vrell | space | Phase-jumps (`teleport`) short distances along the distorted scroll; fires a `burst` after each jump. | stream | 44 | idea |
| Husk | Vrell (Silence-touched) | space | A drifting derelict hollowed from inside. When damaged it cracks and sprays crystalline shards (`death-burst` + `fan`). | swarm (scattered) | 46 | idea |

## Design

- Space levels have no `ground` layer in the planetary sense. Station hulls, asteroid surfaces
  and reefs act as the `ground` layer (see [layer rules](../README.md)).
- The Void Leech ties enemies to the player's generator budget; the drain rules are in
  [generator](../../player/generator/README.md#enemy-drain-effects).

## Implementation

- [ ] Each enemy promoted to its own directory with a stat block before it is implemented.

## Open questions

- Should Husks (L46) show anything of the Silence visually, e.g. an unnatural black-glass
  interior? Proposal: yes, as the only visual hint of the Silence in the game.

## Decisions

- 2026-09-30: Roster of 7 space enemies drafted.
