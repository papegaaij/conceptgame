---
title: Skimmer
design: review
implementation: in-progress
art: chosen
depends-on: [../../../systems/difficulty, ../../../systems/economy, ../../../campaign/act-2-homefront/level-13-polar-relay]
updated: 2026-10-12
---

# Skimmer

## Summary

A rust flying-fish skiff with fan fins that weaves between ice floes leaving foam wakes and fires aimed shots, entering from every edge.

## Design

### Stat block

Values at **medium** from [data.yaml](data.yaml) (M5 part G, step G4) (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (a Pulse Cannon L1 shot does 2); bullet damage classes and speeds are defined in the balancing basis.

<!-- data: stat-block -->
| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `ground` (naval surface) |
| Size tier | `small` |
| Size | 40×40 px, hitbox 22×32 |
| Parts | single |
| Orientation | `16 angles` (16 headings × 2 tail frames, from its velocity) |
| HP | 5 (easy 4 / hard 6, from the global multipliers) |
| Armour / shield | none |
| Speed | 180 px/s (along its lane, on top of the scroll) |
| Movement | **lane walker**: follows its wave's lane (a ground-space polyline that scrolls with the ground and stands still in a halt) with a `sine` weave across it (amplitude 40 px, 1.2 s per swing); enters from any edge, incl. the rear and the sides (3 s edge warning) |
| Attack | `aimed` shot every 1.8 s (190 px/s, `small` = 4), 0.5 s after it comes on; under the level's `alternate` hook every second attack goes at the objective while it is closer than the ship |
| Formations | convoy, cross (from all edges) |
| Weak points | violet fin glow (drawn only) |
| Effective traits | `anti-ground`, `side`, `rear`, `spread` |
| Credits | 14 (score 140 × chain) |
| Death | `small` burst; the hull flips and sinks with the water splash (a presentation event) |
| First level / used in | L13; Act 2 |
| Difficulty hooks | hard: 2-shot bursts (one attack for the hook's count) |
<!-- /data -->

### Behaviour

- Leaves a foam V-wake per the water rules; wakes show where skimmers are heading before they fire.
- **Lane walker** (G4 = a, M5 part G): a wave gives each Skimmer a **lane**, a polyline in ground
  space like a walker's path. It moves along it at 180 px/s, the weave (40 px amplitude, 1.2 s period)
  an offset across the lane, and its heading is the nearest of the 16 angles to its velocity (the
  heading is drawn, not simulation state that decides armour: it has none). The lanes scroll with the
  ground while the scroll runs and stand still in a halt (Level 13's defend phase), and the backdrop
  cuts the leads along them, so a Skimmer never crosses the ice.
- **Entry**: from any edge, the rear and the sides included, each with the 3 s edge warning; the
  `cross` formation sends a group from each of the four edges, every unit on its own lane.
- **Under the `alternate` hook** (Level 13): each Skimmer counts its attacks; every second one is
  aimed at the relay while the relay is closer than the ship, the others at the ship. A hard 2-shot
  burst is **one** attack. A relay-aimed shot still hurts the ship it crosses
  ([target-the-objective hook](../../README.md#target-the-objective-hook)).
- **Death**: the `small` burst, then the hull flips and sinks, with the water splash. On a level
  where the water splash is chosen by unit (Level 13, G5 = a), a burst on a Skimmer is one of the
  few that use it.
- No weak-point multiplier: the violet fin glow is drawn only (single-part rule).

### Concept art

Chosen concept: [skimmer-r06-a.png](../concept/skimmer-r06-a.png), [skimmer-r06-a.gif](../concept/skimmer-r06-a.gif) (listed in the [naval](../README.md#concept-art) Concept art table).

The production files (M5 part G, step G2, straight to production per G9 = a; pending the user's review in
concept round 35), generator notes in [concept/prompts.md](concept/prompts.md):

| File | What | Status |
|---|---|---|
| [concept/skimmer-final-r35-a.png](concept/skimmer-final-r35-a.png), [.gif](concept/skimmer-final-r35-a.gif) | Production art, round 35 (`tools/art/skimmer.py`): 16 headings × 2 tail frames above the waterline with the collar, the sub-pass body, a wake per heading, the flip-and-sink; the loop on the halted frame's stand-in | proposed |

## Implementation

- [x] Lane walker with the sine weave, 16 headings from its velocity, any-edge entry (M5 part G, steps G3 and G4) —
      simulation in G3 (`WaveSpec.lanes`, `Enemy.laneWalking`, `SkimmerTest`), the waves' `lanes` and the
      `sine` from its data in G4 (`PartGUnitsDataTest`, `Level13Test`)
- [x] Aimed shot every 1.8 s, hard 2-shot bursts, counted as one attack under `alternate` (M5 part G, G3;
      `HookModesTest`, `SkimmerTest`)
- [ ] Foam wakes (E's wake sprites, a wake per heading) (M5 part G, G5 and G6)
- [ ] Flip-and-sink death with the water splash (M5 part G, G6)
- [x] `data.yaml` for the stat block (with the level's data; the loaders are strict) (M5 part G, G4)
- [ ] Production sprite `tools/art/skimmer.py`: 16 angles × 2 tail frames, the sink, the intel portrait (M5 part G, G5; final-ready when the user accepts it in round 35)
- [x] Stat block values loaded from data; global difficulty multipliers applied (M5 part G, G4)
- [ ] Death effect, bounty and score per this spec

## Decisions

- 2026-10-01: Promoted from the naval roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: HP rescaled to the lowered L13 reference DPS (8 → 5, ×76/120).
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
- 2026-10-11: M5 part G, step G1a: **G4 = a** (recommended; pending the user's review, the user is on
  vacation) the Skimmer is a lane walker on ground-space lanes with the weave as an offset, 16 headings
  from its velocity; the leads are cut along the lanes and do not drift. **G1 = a** (recommended;
  pending the user's review): under `alternate` every second attack goes at the relay while it is
  closer than the ship; a burst is one attack. Death flip-and-sink with the water splash by unit
  (G5 = a). `design: review` for the reworked behaviour; the stat block is unchanged.
- 2026-10-11: M5 part G, step G3 (the simulation; pending the user's review): a lane is a smooth curve
  through its points (a Catmull-Rom curve, as a flight path; the backdrop cuts the leads along the same
  curve); the weave is an offset across the lane, to the right of its direction on a positive swing; the
  Skimmer goes straight on past its lane's end until it leaves the play field, and a unit that never got
  onto the field is gone at its lane's end; its facing is the exact heading of its velocity over the ground
  (the scroll left out; the 16 headings are the drawing's), and its gun counts down only on the screen
  (first shot 0.5 s after it comes on, the stat block's delay). Its death raises `LANE_WALKER_DOWN` with its
  kind and facing for the flip and sink.
- 2026-10-12: M5 part G, step G4 (pending the user's review): the stat block in [data.yaml](data.yaml) (a
  `ground` unit with the `sine` weave; a lane walker by its waves' `lanes`; hard's 2-shot burst as the
  hook `burst: 2`), the stat block table rendered from it; the production sprites written to `assets/`
  (`tools/art/skimmer.py --assets`) and the intel portrait (`tools/art/intel.py skimmer`).
