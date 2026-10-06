---
title: Coilwyrm
design: approved
implementation: done
art: final
depends-on: [../../../systems/difficulty, ../../../systems/economy, ../../../audio/sfx]
updated: 2026-10-06
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
| Movement | the head flies an authored `swirl`, `loop` or `figure-8` path from the wave; segments `chain` on the head's path history at 0.5 × segment length spacing; frequent `loop` + `rear-entry` (edge-warned like every rear entry) |
| Attack | head: 3-way `fan` every 2 s (spread 24°, 160 px/s, `small` = 4); contact damage `large` = 20 (head) / `small` = 10 (segments, tail) |
| Formations | snake (solo; on hard two chains cross, *pairs crossing*) |
| Weak points | head teal crest (×2) |
| Effective traits | `piercing`, `spread`, `rear` |
| Credits | 86 (score 860 × chain); segment 3 each, tail +10 if destroyed first: a perfect dismantle pays 96; a head-first kill pays the head only; a regrown head pays 20 |
| Death | head: `medium` burst (its teal flash and a deep wet burst), then the segments and the tail burst one after another down the body (0.25 s apart) as it drifts to rest, each with its glint, pieces and wet burst; the waiting members cannot be hit and pay nothing; a segment shot on its own bursts the same way |
| First level / used in | L06; returns in Act 7 (L44) |
| Difficulty hooks | hard: 14 segments; the regrown head fires a 5-way fan |
<!-- /data -->

### Behaviour

- **Splitting:** destroying a body segment splits the chain. The rear part grows a new head over 0.6 s (it cannot be damaged during regrowth) and becomes its own, faster chain. A regrown chain that is cut again does not regrow: the severed part dies from the cut backwards.
- Killing the original head first destroys the whole chain with the chained explosion and a time bonus (score only).
- **Chained death:** the head goes with its `medium` burst (teal flash, skull pieces) and its deep
  wet burst; then the body bursts as a ripple from the head down to the tail, one member every
  `pop_interval` (0.25 s: 13 bursts in about 3.25 s at medium, 15 in 3.75 s on hard), each with the
  member's own death (its glint and pieces) and the segment burst sound in the same step, pitched up
  as the members narrow down the taper (0.94 at the first segment to 1.2 at the last, the tail in
  between). The headless body drifts to a halt (about 60 px, at rest in 1.5 s) so the bursts happen
  in place on the field; it does not loop back. Until its burst a member stays drawn, intact, but
  it is **doomed**: no shot, blast or special hits it, it rams nothing and it pays nothing. The
  same ripple runs from a second cut backwards (those members follow the living front part) and
  when a regrown head dies. The sounds ([round 24](../../../concept-rounds/round-24/README.md))
  are in the [sound effects](../../../audio/sfx/README.md#concept-art).
- Segments overlap 20–30 % so the chain reads as one body in tight turns.
- **Counts as one enemy** for the campaign density and the kill ratio (user decision D1 of M4
  part F); its segments and tail pay their bounty but are not kills of their own.
- The regrown head has 20 HP and pays 20 (half the head). A head-first kill pays the head only
  (the chained death pays nothing more). Parts killed in one step (a Smart Bomb) do not regrow.
- Each wave authors its own path; a loop-back leaves through the bottom or top edge and re-enters
  from the bottom edge at the head's x after 6 s, warned 3 s ahead like every rear entry (4 s on
  easy). `REAR` entry is allowed for a chain.

## Concept art

Chosen concept: [coilwyrm-r08-a.png](../concept/coilwyrm-r08-a.png), [coilwyrm-r08-a.gif](../concept/coilwyrm-r08-a.gif) (round 08: head, segment and tail at 16 angles; it supersedes r05) (listed in the [air](../README.md#concept-art) Concept art table). Production review files and their brief: [concept/prompts.md](concept/prompts.md); accepted as final in [round 23](../../../concept-rounds/round-23/README.md).

| File | What | Status |
|---|---|---|
| [concept/coilwyrm-final-r23-a.png](concept/coilwyrm-final-r23-a.png) | Final sprites (`tools/art/coilwyrm.py`): the chain on a curve at the data's spacing, with 16 headings and at the concept's 0.5 spacing for comparison (0.5 is now the data's); the head (58 px, 4 jaw frames), the regrown head, the 12 segment sizes (54 to 27 px) and the tail (40 px), 48 headings each; the head's death (teal flash, additive; skull, horns, mandibles, solid), the segment pop's glint and the segment and tail pieces; one 40-colour palette over the chain | chosen |
| [concept/coilwyrm-final-r23-a.gif](concept/coilwyrm-final-r23-a.gif) | The chain swirling down the field at the round's 0.9 spacing, the jaw snapping | chosen |
| [concept/coilwyrm-death-capture-r24-a.gif](concept/coilwyrm-death-capture-r24-a.gif) | A capture of the game (Level 06, the first Coilwyrm, round 24's close): the head shot, its flash and burst, the body drifting to rest and bursting segment by segment 0.25 s apart; silent, 12 fps | chosen |
| [concept/coilwyrm-death-capture-r24-a.mp4](concept/coilwyrm-death-capture-r24-a.mp4) | The same 5 s with the game's sound (the head burst c, the segment bursts b), to check the sync | chosen |

## Implementation

- [x] Path-history chain with per-segment angles and tapering hit boxes; the head flies the
      wave's authored path; loop-back re-entering from the bottom edge at the head's x, warned
      3 s ahead (a wave's `warning`, 4 s on easy)
- [x] The authored `swirl`, `loop` and `figure-8` paths in Level 06's data
- [x] Counts as one enemy (density, kill ratio); segments pay bounty only
- [x] Split once with regrowth, second cut kills the severed part
- [x] Chained death explosion
- [x] Stat block values loaded from data; global difficulty multipliers applied
- [x] The head's weak point: every damage the head takes is multiplied by its part's `multiplier`
  (×2; shots, blasts, specials, ramming), like a set piece's part; segments, tail and a regrown head
  take plain damage (`EnemySpec.ChainSpec.headMultiplier`, `Enemy.damage`; tests `FarsideTest`,
  `FarsideLevelTest`); the Targeting computer's brackets mark the living head
- [x] Death effect, bounty and score per this spec (the head's teal flash and skull pieces, the
      segments' and tail's glint and pieces of `tools/art/coilwyrm.py` with their bursts and pops)
- [x] Production sprites drawn: head (4 jaw frames), regrown head, segments at their taper's
      sizes and tail, 48 headings each (`EnemyLooks.CHAIN_HEADINGS`, the segment's size by its hit
      box); review in round 23
- [x] Segment spacing 0.5 (overlapping bodies, a chain of about 330 px)
- [x] Chained death as a ripple from the head to the tail at the data's `pop_interval` (0.25 s),
      deterministic and allocation-free; the bursts pay nothing
- [x] Waiting members are doomed (`Chain.doomed`): unhit by shots, blasts and specials, no ramming;
      the headless body drifts to rest and does not loop back (`ChainDeathTest`)
- [x] Each chained burst plays the member's own death (glint, pieces) and its burst sound in the
      same step, pitched by the member's hit box width; the heads play the head burst
      (`Sfx.COILWYRM_BURST`, `COILWYRM_HEAD_BURST`)
- [x] Death sounds chosen in [round 24](../../../concept-rounds/round-24/README.md): segment burst
      b, head burst c, rebuilt from the Freesound originals (`tools/art/sfx_originals.py`)

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
- 2026-10-04: M4 part F batch (round 23): production sprites from the chosen round-08 models
  (`tools/art/coilwyrm.py`). 48 headings (7.5°) instead of the stat block's 16: neighbours on a
  curve take nearby tangents, and 16 steps showed as kinks; about 1.6 M px for the chain. The
  segments are rendered at the 12 sizes of the taper instead of one scaled sprite. **Gap**: at the
  data's spacing (0.9 × the mean sprite size, a chain of about 480 px) the round bodies (about 55 %
  of their sprite) do not overlap, so the chain reads as beads with gaps; the concept used 0.5
  (about 330 px, the 20–30 % overlap of *Behaviour*). The review shows both; the spacing is a data
  choice for the user (round 23).
- 2026-10-05: Round 23 outcome (user): the production art is accepted as final (`art: final`);
  the regrowth sound is **b** (the insect growl and chitter; a, wet slime, rejected).
- 2026-10-05: Segment spacing **0.5** instead of 0.9 (user decision on round 23's gap): the bodies
  overlap as the concept and *Behaviour* have them, a chain of about 330 px instead of 480.
- 2026-10-05: The chained death becomes a burst ripple (user: the old one sounded like one small
  pop): 0.05 s between bursts instead of 0.06 (3 steps instead of 4; about 0.65 s head to tail),
  each burst the member's own death look instead of the generic tiny explosion, and new wet burst
  sounds for the segments, the tail and the heads, offered as options a–d in
  [round 24](../../../concept-rounds/round-24/README.md); option a is wired in provisionally. Mix
  (agent choice): the ripple's bursts 4 dB under the explosions, pitch rising 1.8 % a burst from
  0.94 to at most 1.2 (smaller segments down the taper) with ±2 % random, six instances of the
  burst so the overlapping three or four are not cut; a segment or the tail shot on its own plays
  the same burst. Unchanged: the chained bursts pay nothing.
- 2026-10-05: Round 24 closed (user): head burst **c** (the messy splatter, slowed, CC-BY) and
  segment burst **b** (the fleshy burst); a and d rejected. "The effects too fast": the ripple is
  five times slower, `pop_interval` 0.25 s (15 steps) instead of 0.05. Agent choices for the slower
  ripple: the waiting members are doomed — drawn intact, but nothing hits them and they ram
  nothing, so they neither soak shots nor pay a bounty during the 3 s (rejected: keeping them
  hittable, which let a player farm or block shots on a dead body); the headless body drifts to a
  halt instead of flying on, so all 13 bursts happen on the field (at full speed it would have
  flown about 590 px, most of it off the bottom edge); the bursts no longer overlap, so they play at
  the explosion level (not 4 dB under); the pitch now follows the chain's taper by each member's hit
  box width instead of rising per burst in time (that rule reset after a 10-step pause, i.e. at
  every burst now); kept, as each burst is a separate event and the shrinking segments read as a
  rising series. A Smart Bomb's ring reaches the head first (nearest the ship), so the body then
  bursts in the ripple and pays nothing, as any head-first kill. The capture (round 24's close)
  shows the sound and the look of each burst within a frame (33 ms).
- 2026-10-05: M4 part H docs reconciliation: implementation `done`; every item is ticked.
- 2026-10-05: M4 part H balance pass (user decision): the bounty stays as it is, an **accepted
  exception** to the balancing basis: its parts total 86 (head 40, 12 segments × 3, tail 10)
  against the `large` class's 40–60, but it is a multi-part enemy and cutting it up segment by
  segment is extra work the player is paid for; a head-first kill pays the class's 40.
  `BalanceTest` lists it under `ACCEPTED` instead of pending a decision.
- 2026-10-06: Bug fixed (M5 part A): the head's ×2 (its part's `multiplier`, the teal crest) was in
  the data but never simulated, so since Level 06 the head took plain damage. It now takes every
  damage ×2, as a set piece's parts take theirs; the regrown head has no multiplier in the data and
  stays at ×1. A head-first kill takes half the damage it did (40 HP at medium: 20 points of
  hits). No test or balancing number moved: the head's HP, the bounties and the typical haul are
  unchanged, and Level 01's replay hash stays (no chain flies there).
