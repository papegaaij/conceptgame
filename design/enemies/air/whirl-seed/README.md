---
title: Whirl Seed
design: approved
implementation: done
art: final
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-03
---

# Whirl Seed

## Summary

A tiny six-fold spinning seed pod released in clusters by Leviathans and spawning reefs. Seeds spiral out, ricochet off the play-field edges and kill on contact; one hit pops them.

## Design

### Stat block

Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (a Pulse Cannon L1 shot does 2); bullet damage classes and speeds are defined in the balancing basis.

<!-- data: stat-block -->
| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `air` |
| Size tier | `tiny` |
| Size | 26×26 px, hitbox 16×16 |
| Parts | single |
| Orientation | `radial` (6-fold spinner, 8 frames per 60°) |
| HP | 1 (easy 1 / hard 1, from the global multipliers) |
| Armour / shield | none |
| Speed | 160 px/s after the spiral; spin 1.5 turns/s |
| Movement | `spiral-out` from the release point for 1.5 s (1 turn/s, radius growth 90 px/s, the release point drifting down 60 px/s), then on along its outward angle (turned downward), `ricochet` off the side edges up to 3 times; leaves through the bottom edge |
| Attack | `none` — contact (damage `tiny` = 6) |
| Formations | whirl cluster (5–8), stream |
| Weak points | none |
| Effective traits | `spread`, `area` |
| Credits | 3 (score 30 × chain) |
| Death | `tiny` pop: seed husk splits, teal glint |
| First level / used in | L03; the Leviathan's clusters; L12 (storm clusters); returns in Act 7 (L45) |
| Difficulty hooks | hard: each seed pops into a 3-bullet puff (90 px/s, `small` = 4) |
<!-- /data -->

### Behaviour

- A cluster releases its seeds within 0.3 s at 360°/n spacing, so the spiral reads as one shape.
- Ricochets only on the side edges; seeds never bounce back up from the bottom.
- The spiral: for 1.5 s each seed circles its release point at one turn per second while its
  radius grows at 90 px/s, and the release point drifts down at 60 px/s with the cluster. Then it
  flies on at 160 px/s along its outward angle, turned downward if it pointed up, bouncing off the
  side edges up to 3 times, and leaves through the bottom edge.

### Concept art

Chosen concept: [whirl-seed-r05-a.png](../concept/whirl-seed-r05-a.png), [whirl-seed-r05-a.gif](../concept/whirl-seed-r05-a.gif) (listed in the [air](../README.md#concept-art) Concept art table).

## Concept art

Production art for concept round 16 (M4 part C, the Level 03 batch), review files built from the final files in `assets/` by `tools/art/vrell_l03.py` (`--review` rebuilds only them); prompts: [concept/prompts.md](concept/prompts.md).

The death effect beyond the `tiny` pop (round 16 too) is rendered by `tools/art/vrell_fx.py` (`--review seed` rebuilds only its review files).

| File | What | Status |
|---|---|---|
| [concept/whirl-seed-final-r16-a.png](concept/whirl-seed-final-r16-a.png) | Final sprites: 8 spin frames covering 60° (26×26, 24 colours), each its own render under the fixed key light; six blades instead of the concept's five so the spin loops | chosen |
| [concept/whirl-seed-final-r16-a.gif](concept/whirl-seed-final-r16-a.gif) | The seed spinning, and a cluster of six spiralling out by the data's numbers | chosen |
| [concept/whirl-seed-death-final-r16-a.png](concept/whirl-seed-death-final-r16-a.png) | Final death effect: the husk splitting (`whirl-seed-husk_0..11`, 40×40, solid: the six blades tumbling off, the core in two halves, ray-marched from the production seed) and the teal glint (`whirl-seed-death_0..7`, 32×32, additive star flare and sparks), 30 fps, shown alone and with the tiny pop | chosen |
| [concept/whirl-seed-death-final-r16-a.gif](concept/whirl-seed-death-final-r16-a.gif) | Three spinning seeds popping in turn | chosen |
| [concept/whirl-seed-dive-capture-r16-a.gif](concept/whirl-seed-dive-capture-r16-a.gif) | Game capture of Level 03 (play field at 1×, 15 fps, 6.5 s, t≈66.5–73, the ship not firing): the clusters released under the Leviathan's first pass spiralling out and the seeds diving off the bottom edge, the sideways ones at least as steeply as the release point drifted | chosen |

## Implementation

- [x] Radial spin frames (the 8 frames over 60° at the spin's 1.5 turns/s)
- [x] Spiral-out then ricochet with a bounce limit
- [x] Hard-mode pop puff (no bullet within 72 px of the ship)
- [x] Stat block values loaded from data; global difficulty multipliers applied
- [x] Bounty and score per this spec
- [x] Death effect per this spec: the `tiny` pop
- [x] Death effect per this spec: the seed husk splitting (solid, with the pop) and the teal
      glint (additive, 4 steps after the pop)

## Decisions

- 2026-10-01: Promoted from the air roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
- 2026-10-02: M4 part C (user decision): the spiral-out path in numbers (1.5 s, one turn per second, radius +90 px/s, the release point drifting down at 60 px/s), then 160 px/s downward with up to 3 side ricochets.
- 2026-10-02: M4 part C, the simulation (choice for review): after the spiral a seed dives at
  least as steeply as its release point drifted (60 of 160 px/s, about 21° below the
  horizontal), so the seeds that end the spiral pointing sideways still leave through the bottom
  edge; the hard puff's speed and bullet class are in the data (`death_burst`).
- 2026-10-02: Production art (user choice): six blades instead of the chosen concept's five, so
  the 8 spin frames cover 60° and loop seamlessly; all blades share the body chitin (the
  concept's alternating dark blade would repeat only every 120°) and the teal core seam is
  toned down to keep the core's glow at the concept's size.
- 2026-10-02: Production art, the death effect (choice for review, round 16): the husk split is
  a solid set (`whirl-seed-husk`, with the pop) and the teal glint an additive one
  (`whirl-seed-death`, 4 game steps after the pop, so the pop's white flash does not swallow it).
- 2026-10-03: Concept round 16 closed (user decision): the production spin frames and the death effect (husk split and teal glint) approved as **final**, `art: final`.
- 2026-10-03: Concept round 16 closed again (user decision, choice 18): the simulation choice for review above accepted as it is (after the spiral a seed dives at least as steeply as its release point drifted), with the dive capture; the death effect's choice for review (the solid husk split with the pop, the additive teal glint 4 steps after it) accepted as well.
