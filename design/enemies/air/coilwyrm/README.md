---
title: Coilwyrm
design: approved
implementation: in-progress
art: chosen
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-04
---

# Coilwyrm

## Summary

A rust-and-teal Vrell serpent of a head, twelve segments and a tail that swirls, loops and figure-eights across the screen, often coming back from the rear. The first segment chain: cut it and the tail end grows a new head — once.

## Design

### Stat block

The numbers are in [data.yaml](data.yaml); the table is rendered from it. Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (a Pulse Cannon L1 shot does 2); bullet damage classes and speeds are defined in the balancing basis.

<!-- data: stat-block -->
| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `air` |
| Size tier | `large` |
| Size | 58×58 px, hitbox 41×41 |
| Parts | head 58 px (`vital`), 12 body segments 54→27 px (`destroyable`), tail 40 px (`destroyable`, pays a bonus if destroyed first); hitboxes 70 % of the sprite |
| Orientation | `16 angles` per part type (head, segment, tail) |
| HP | head 40, each segment 4, tail 10; whole 98 (easy 74 / hard 127, from the global multipliers) |
| Armour / shield | none |
| Speed | 180 px/s (regrown chain 220 px/s) |
| Movement | the head flies an authored `swirl`, `loop` or `figure-8` path from the wave; segments `chain` on the head's path history at 0.9 × segment length spacing; frequent `loop` + `rear-entry` (edge-warned like every rear entry) |
| Attack | head: 3-way `fan` every 2 s (spread 24°, 160 px/s, `small` = 4); contact damage `large` = 20 (head) / `small` = 10 (segments, tail) |
| Formations | snake (solo; on hard two chains cross, *pairs crossing*) |
| Weak points | head teal crest (×2) |
| Effective traits | `piercing`, `spread`, `rear` |
| Credits | 86 (score 860 × chain); segment 3 each, tail +10 if destroyed first: a perfect dismantle pays 96; a head-first kill pays the head only; a regrown head pays 20 |
| Death | head: `medium` burst and a chained `small` pop down the body (0.06 s per segment); segments: `tiny` pops |
| First level / used in | L06; returns in Act 7 (L44) |
| Difficulty hooks | hard: 14 segments; the regrown head fires a 5-way fan |
<!-- /data -->

### Behaviour

- **Splitting:** destroying a body segment splits the chain. The rear part grows a new head over 0.6 s (it cannot be damaged during regrowth) and becomes its own, faster chain. A regrown chain that is cut again does not regrow: the severed part dies from the cut backwards.
- Killing the original head first destroys the whole chain with the chained explosion and a time bonus (score only).
- Segments overlap 20–30 % so the chain reads as one body in tight turns.
- **Counts as one enemy** for the campaign density and the kill ratio (user decision D1 of M4
  part F); its segments and tail pay their bounty but are not kills of their own.
- The regrown head has 20 HP and pays 20 (half the head). A head-first kill pays the head only
  (the chained death pays nothing more). Parts killed in one step (a Smart Bomb) do not regrow.
- Each wave authors its own path; a loop-back leaves through the bottom or top edge and re-enters
  from the bottom edge at the head's x after 6 s, warned 3 s ahead like every rear entry (4 s on
  easy). `REAR` entry is allowed for a chain.

### Concept art

Chosen concept: [coilwyrm-r08-a.png](../concept/coilwyrm-r08-a.png), [coilwyrm-r08-a.gif](../concept/coilwyrm-r08-a.gif) (round 08: head, segment and tail at 16 angles; it supersedes r05) (listed in the [air](../README.md#concept-art) Concept art table).

## Implementation

- [x] Path-history chain with per-segment angles and tapering hit boxes; the head flies the
      wave's authored path; loop-back re-entering from the bottom edge at the head's x, warned
      3 s ahead (a wave's `warning`, 4 s on easy)
- [ ] The authored `swirl`, `loop` and `figure-8` paths in Level 06's data
- [x] Counts as one enemy (density, kill ratio); segments pay bounty only
- [x] Split once with regrowth, second cut kills the severed part
- [x] Chained death explosion
- [x] Stat block values loaded from data; global difficulty multipliers applied
- [ ] Death effect, bounty and score per this spec (bounty, the tail's first bonus and score done;
      the production sprites and death art follow, placeholder shapes fly meanwhile)

## Decisions

- 2026-10-01: Promoted from the air roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
- 2026-10-04: M4 part F: the stat block moved into [data.yaml](data.yaml) (a `segment_chain`
  block, the tail's `first_bonus`, `path` movement and the hard hooks `segments` and
  `regrown_fan_count`; planned keys in the
  [schemas](../../../tech/architecture/README.md#data-file-schemas)). The concept link points at
  the chosen `coilwyrm-r08-a` instead of the superseded r05. The 1.5 s edge warning follows the
  code's 3 s minimum (main-agent choice).
- 2026-10-04: A Coilwyrm counts as one enemy for density and the kill ratio, its segments pay
  bounty only (user decision D1 of M4 part F); rejected: counting the segments as enemies (it
  games the density rule).
- 2026-10-04: M4 part F step 2 built the chain (`FarsideTest`, `FarsideLevelTest`); code choices
  (main agent): the members follow the head's path history at spacing × the mean length of two
  neighbours (head 58 px, tail 40 px), so the chain is about 480 px long; the cut rear part holds
  still for its 0.6 s regrowth, then the new head lunges straight at where the ship was at
  220 px/s and leaves; the tail's first bonus pays when it goes before any other part; the
  chained death and a severed part's pops pay nothing; the time bonus for a head-first kill has
  no number yet and is not paid.
