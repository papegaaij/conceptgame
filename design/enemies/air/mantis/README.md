---
title: Mantis
design: approved
implementation: in-progress
art: chosen
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-04
---

# Mantis

## Summary

A bone-white Vrell sniper that enters from a side edge, holds position and sweeps a short laser across the lower screen. The first enemy that demands `side` or `spread` weapons.

## Design

### Stat block

The numbers are in [data.yaml](data.yaml); the table is rendered from it. Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (a Pulse Cannon L1 shot does 2); bullet damage classes and speeds are defined in the balancing basis.

<!-- data: stat-block -->
| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `air` |
| Size tier | `medium` |
| Size | 80×80 px, hitbox 40×56 |
| Parts | single |
| Orientation | `fixed` (facing inward; the sprite is mirrored per side) |
| HP | 26 (easy 20 / hard 34, from the global multipliers) |
| Armour / shield | none |
| Speed | 150 px/s entering and leaving, 0 while hovering |
| Movement | enters from the left or right edge (edge-warned like every side entry), `hover` at x = 40 px from that edge, y = 120–360 px, for 6 s (two sweeps), then exits the way it came |
| Attack | `laser-sweep` from its eye: a beam 300 px long and 6 px wide sweeps a 70° arc over 1.2 s, centred on the ship's bearing when the telegraph starts (clamped between straight inward and straight down, so the far side of the field stays safe); telegraphed 0.6 s by the crimson arc; every 3 s while hovering, the first 0.6 s after it settles; damage `laser` = 8 per touch (once per sweep) |
| Formations | single, pincer (the pincer one from each side) |
| Weak points | crimson thorax glow (×1.5) |
| Effective traits | `side`, `spread` |
| Credits | 30 (score 300 × chain) |
| Death | `medium` burst: bone shards, crimson flash |
| First level / used in | L06; Acts 1–2 |
| Difficulty hooks | hard: 90° arc, every 2.5 s |
<!-- /data -->

### Behaviour

- The Mantis hovers outside the reach of forward guns unless the player moves close to the edge — the intended dilemma.
- Never fires from off-screen; it must be fully visible before the first telegraph.
- **The beam** (user decision D4 of M4 part F): 300 px long and 6 px wide, from the eye. The
  sweep is centred on the ship's bearing when the 0.6 s telegraph starts, clamped between straight
  inward and straight down, so the far side of the field is out of reach: getting away from that
  side is a real answer. The crimson arc telegraph shows the full sweep (both edges and the
  length) and the beam is drawn while it sweeps; it hits the ship at most once per sweep.
- Two sweeps per 6 s hover (telegraphs 0.6 s and 3.6 s after it settles; on hard every 2.5 s, still
  two: a sweep starts only if it ends before the hover does). It enters alone from a side edge
  (`single` from a side, or one per edge in a pincer) with the 3 s side-entry warning of every side
  and rear entry, and leaves through the edge it came from.

### Concept art

Chosen concept: [mantis-r04-a.png](../concept/mantis-r04-a.png) (listed in the [air](../README.md#concept-art) Concept art table).

## Implementation

- [x] Side entry with edge warning (a single unit from a side edge), hover at x 40 from the edge
- [x] Laser sweep with 0.6 s arc telegraph: 300 px × 6 px beam centred on the ship's bearing,
      clamped inward-to-down, once per sweep; telegraph and beam drawn (placeholder strokes until
      the part's concept round)
- [x] Exit after 6 s
- [x] Stat block values loaded from data; global difficulty multipliers applied
- [ ] Death effect, bounty and score per this spec (bounty and score done; the bone-shard death
      comes with the production art, a placeholder shape flies meanwhile)

## Decisions

- 2026-10-01: Promoted from the air roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
- 2026-10-04: M4 part F: the stat block moved into [data.yaml](data.yaml) (`laser-sweep` with its
  `sweep` block, `hover` `edge_x` and `exit: back`, the hard hook `sweep_arc`; planned keys in the
  [schemas](../../../tech/architecture/README.md#data-file-schemas)). The 1.5 s edge warning
  follows the code's 3 s minimum for every side and rear entry (main-agent choice).
- 2026-10-04: The beam is 300 px long and sweeps centred on the ship's bearing, with the 0.6 s
  arc telegraph (user decision D4 of M4 part F); rejected: a full-width beam (the edge dilemma
  disappears, only a dodge) and a fixed inward-down sweep (predictable, weaker).
- 2026-10-04: M4 part F step 2 built the side hover, the sweep and its telegraph (`FarsideTest`,
  `FarsideLevelTest`). The hard sweep interval of 2.5 s is authored, so the fire-rate lever does
  not apply on top of it; on easy the 3 s interval is divided by the lever (main-agent choice).
