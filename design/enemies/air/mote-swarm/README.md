---
title: Mote Swarm
design: approved
implementation: done
art: final
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-08
---

# Mote Swarm

## Summary

A flock of tiny ember-like motes that swirls in front of the player, sweeps off screen and loops back to dive from behind. Spread and area weapons shred it.

## Design

### Stat block

The numbers live in [data.yaml](data.yaml); the stat block is one mote's, a swarm is a wave of them. Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (a Pulse Cannon L1 shot does 2); bullet damage classes and speeds are defined in the balancing basis. The flock is a planned key (the [schemas](../../../tech/architecture/README.md#data-file-schemas)): it waits in the data's comment until the simulation reads it.

<!-- data: stat-block -->
| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `air` |
| Size tier | `tiny` |
| Size | 16×16 px, hitbox 10×10 |
| Parts | single |
| Orientation | `16 angles` |
| HP | 1 (easy 1 / hard 1, from the global multipliers) |
| Armour / shield | none |
| Speed | 200 px/s (flock cruise), 260 px/s diving after a loop-back |
| Movement | `flock` (separation 18 px, alignment, cohesion) round a leader point that flies the wave's authored route at 200 px/s (`path`): it swirls in front, exits at the bottom-left or bottom-right, re-enters from the bottom 1.5 s later (the edge warning 3 s ahead, starting while it exits) and dives up through the player's lane |
| Attack | `none` — contact (damage `tiny` = 6 per mote; destroyed by the impact) |
| Formations | swarm (6–24) (a wave of motes; base 20, easy 16, hard 24 by the formation lever; Level 10 flies smaller ones too) |
| Weak points | none (dies in one hit) |
| Effective traits | `spread`, `area`, `rear` |
| Credits | 2 (score 20 × chain) per mote; a swarm of 20 pays 40 |
| Death | `tiny` ember puff |
| First level / used in | L10; returns in Act 3 (L20) |
| Difficulty hooks | flock size via the global formation lever; hard: two loop-backs (level data) |
<!-- /data -->

### Behaviour

User decision D8 = a of M5 part D: **real flocking** (boids).

- **The swarm** (formation `swarm`): a wave of 6–24 motes. A **leader point** flies the wave's
  authored route (its `paths`, with an optional `loop_back`, as a chain's head) at 200 px/s; the
  motes start in a seeded cloud round the route's first point and each steers by **separation**
  (18 px), **alignment** and **cohesion** among its neighbours and a pull toward the leader point.
  The leader point is not a unit: shooting the motes never stops the route.
- **Determinism and cost**: a fixed iteration order (the motes in entry order), `StrictMath`, fixed
  arrays sized for 24 members (at most 576 pair checks a step), no allocation per step; a killed
  mote drops out and the rest close up. The same seed flies the same flock.
- **Loop-back**: after its route leaves the bottom-left or bottom-right edge, the leader point
  re-enters from the bottom **1.5 s** later and the flock dives up through the player's lane at
  260 px/s. The bottom edge is warned **3 s** ahead of the re-entry, so the warning starts while
  the swarm still exits (the chains' rule); the flock counts as a `rear` wave for Rook's Trail from
  the re-entry, and the attempt's first loop-back raises the radio event `first-loop-back`. On hard
  a swarm loops back **twice** (the level data's wave change); on easy as on medium.
- A swarm may also enter **from the bottom edge** (`from: rear`, warned 3 s ahead as any rear
  wave): Level 10's crossing swarms.
- **Each mote is an ordinary unit**: a kill, a bounty of 2 and a chain step of its own, so chains
  are easy to build on a swarm, a score opportunity. Motes never fire; the swarm's danger is its
  loop-back path. A mote that touches the ship, Rook or a shuttle deals `tiny` contact (6) and is
  destroyed by the impact, as a rammer.
- **Readability**: the leader routes keep the flock a readable cloud ("like starlings"), not a
  formation; the first values of the weights are tuned in the build for that.

### Concept art

Chosen concept: [mote-swarm-r05-a.png](../concept/mote-swarm-r05-a.png), [mote-swarm-r05-a.gif](../concept/mote-swarm-r05-a.gif) (listed in the [air](../README.md#concept-art) Concept art table). M5 part D takes it straight to production (user decision D10 = a, concept round 32): 16×16, 16 headings, a flicker, the ember puff, an intel portrait and a flock GIF.

The production files (M5 part D, straight to production per D10 = a; approved as final in concept round 32), generator notes in [concept/prompts.md](concept/prompts.md):

| File | What | Status |
|---|---|---|
| [concept/mote-swarm-final-r32-a.png](concept/mote-swarm-final-r32-a.png) | Final sprites (`tools/art/mote_swarm.py`): 16 headings × 3 flicker frames (`mote-swarm_0..47`, 16×16), the ember puff and tatters (24×24) | chosen |
| [concept/mote-swarm-final-r32-a.gif](concept/mote-swarm-final-r32-a.gif) | A 20-mote flock looping back | chosen |

## Implementation

- [x] Stat block values loaded from [data.yaml](data.yaml); global difficulty multipliers applied
- [x] Boids flock round a leader point flying the wave's authored route (D8 = a): separation 18 px,
      alignment, cohesion; deterministic and allocation-free (at most 24 members)
- [x] The `swarm` formation: from the top or `from: rear`; the loop-back (one, hard two) with the
      bottom-edge warning 3 s ahead, Rook's Trail and the radio event `first-loop-back`
- [x] Per-mote kills count toward chains; contact `tiny`, destroyed by the impact
- [x] Death effect, bounty and score per this spec
- [x] Production sprites (headings, flicker, ember puff) and intel portrait; the swarm sound
      (concept round 32's b)

## Decisions

- 2026-10-01: Promoted from the air roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
- 2026-10-08: M5 part D (user decisions of 2026-10-08): **D8 = a** boids: a `flock` group like the
  Coilwyrm's chain, its leader point flying an authored route from the level data (loop-backs
  included), the members steering by separation (18 px), alignment and cohesion toward it; fixed
  order, `StrictMath`, fixed arrays, at most 24 members; the route as data also ticks the
  Skitter's authored-paths item for snakes and swarms; rejected: b (authored paths with a fixed
  offset and a wobble: a moving formation, not a flock) and c (b with separation only). Stated
  defaults: the 1.5 s edge warning replaced by the 3 s rule (the 1.5 s off-screen gap kept, so the
  warning starts while the swarm exits); the numbers in [data.yaml](data.yaml); one mote's stat
  block, `swarm` its formation (6–24; `rear ambush` is the Wraith's), the draft's 12–30 narrowed to
  the 24 members a flock holds; easy as medium (one loop-back), hard two. Our readings, for review
  in round 32: the leader point as a route, not a unit; the weights' first values; a mote destroyed
  by contact with a shuttle. `design` goes to `review` for round 32.
- 2026-10-08: M5 part D simulation built (the movement, the cloak or the flock, the formations and
  their keys, read from the data file); its simulation items are ticked, the drawing, the sounds and
  the art follow.
- 2026-10-08: Concept round 32 closed (user): the production art (`mote-swarm_0..47`, the ember puff
  and tatters) and the intel portrait approved as **final**, the weak spots as they are (the muddy
  ember puff); the swarm sound **b** (an insect chitter over a whoosh, at the entry and each
  loop-back); the numbers and the build choices of round 32 (the flock tuning, the leader point a
  route, the motes removed 6 s after their route ends, `first-loop-back` at the re-entry) accepted.
  `design: approved`, `art: final`; every Implementation item is ticked, so the implementation is
  `done`.
