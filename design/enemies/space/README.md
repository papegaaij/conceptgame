---
title: Space enemies
design: draft
implementation: not-started
art: proposed
updated: 2026-09-30
---

# Space enemies

## Summary

Vacuum-only enemies for space levels: the belt, the gate and Vrell space. On the `space` layer
they behave like `air` units (the player's plane) but never appear in atmosphere or water. Many
use the space environment: clinging to asteroids, laying mines, draining energy or bending space.

## Roster

| Name | Faction | Layer | Tier | Role/ability | Formations | First level | Design |
|---|---|---|---|---|---|---|---|
| Asteroid Mite | Vrell | space | tiny | Clings to drifting asteroids and leaps off at the player (`dive`) from the sides as they pass. | swarm (on asteroids) | 29 | idea |
| Minelayer | Ascendancy | space | medium | Passes from the rear upward and leaves lines of proximity `mine`s ahead of the player. | column | 30 | idea |
| Shard Drone | Ascendancy | space | small | Groups of 3–4 connected by damaging `link` beams, forming moving barriers; killing one breaks its links. | line abreast, circle | 31 | idea |
| Void Leech | Vrell | space | small | `drift`s in from behind, `latch`es, and `drain`s **generator energy**, so heavy weapons stall. Punishes loadouts with little spare power. | stream, rear ambush | 33 | idea |
| Gate Warden | Vrell | space | medium | Shielded sentinel. Its shield opens only while it fires a `laser-line`; `shield-breaker` strips it at any time. | line abreast | 43 | idea |
| Rift Skater | Vrell | space | small | Phase-jumps (`teleport`) short distances along the distorted scroll; fires a `burst` after each jump. | stream | 44 | idea |
| Husk | Vrell (Silence-touched) | space | medium | A drifting derelict hollowed from inside. When damaged it cracks and sprays crystalline shards (`death-burst` + `fan`). | swarm (scattered) | 46 | idea |
| Leviathan | Vrell | high-air → air / space | huge | Whale-like creature several times the player's length: body, articulated tail and fins, belly spawn vents and two tentacle turrets. First pass drifts over on `high-air` while vents release Whirl Seed clusters; the second pass descends to the play plane, where the fins and turrets (`destroyable`) fire `fan`s and the vents become `vital` weak points. A set piece: at most one per level, announced by radio. Orientation 32 angles for the body, articulated parts ±30°. | solo set piece | 03 | idea |

## Design

- Space levels have no `ground` layer in the planetary sense. Station hulls, asteroid surfaces
  and reefs act as the `ground` layer (see [layer rules](../README.md)).
- The Void Leech ties enemies to the player's generator budget; the drain rules are in
  [generator](../../player/generator/README.md#enemy-drain-effects).

## Concept art

Concept [round 05](../../concept-rounds/round-05/README.md) — new archetypes, each a PNG sheet plus a GIF in the 480×540 play field; generator `tools/concept/enemies_r05.py`. Prompts: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/leviathan-r05-a.png](concept/leviathan-r05-a.png) | Leviathan — huge Vrell whale (bone/violet), ~480 px: body + 3 tail segments + fluke with a travelling wave + flapping fins, each part at 32 headings; dorsal turrets fire aimed orbs (sheet) | proposed |
| [concept/leviathan-r05-a.gif](concept/leviathan-r05-a.gif) | Leviathan — huge Vrell whale (bone/violet), ~480 px: body + 3 tail segments + fluke with a travelling wave + flapping fins, each part at 32 headings; dorsal turrets fire aimed orbs (motion) | proposed |

## Implementation

- [ ] Each enemy promoted to its own directory with a stat block before it is implemented.

## Open questions

- Should Husks (L46) show anything of the Silence visually, e.g. an unnatural black-glass
  interior? Proposal: yes, as the only visual hint of the Silence in the game.

## Decisions

- 2026-09-30: Roster of 7 space enemies drafted.
- 2026-09-30: Enemy variety pass: size tier column added to every row; new units Leviathan.
