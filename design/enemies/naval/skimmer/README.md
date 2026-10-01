---
title: Skimmer
design: approved
implementation: not-started
art: chosen
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-01
---

# Skimmer

## Summary

A rust flying-fish skiff with fan fins that weaves between ice floes leaving foam wakes and fires aimed shots, entering from every edge.

## Design

### Stat block

Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (a Pulse Cannon L1 shot does 2); bullet damage classes and speeds are defined in the balancing basis.

| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `ground` (naval surface) |
| Size tier | `small` |
| Size | 40×40 px, hitbox 22×32 |
| Parts | single |
| Orientation | 16 angles × 2 tail frames |
| HP | 5 (easy 4 / hard 6, from the global multipliers) |
| Armour / shield | none |
| Speed | 180 px/s |
| Movement | `sine` weave (amplitude 40 px, period 1.2 s) along lanes between floes; enters from any edge, incl. the rear (edge warning) |
| Attack | `aimed` shot every 1.8 s (190 px/s, `small` = 4) |
| Formations | convoy, cross (from all edges) |
| Weak points | violet fin glow (×1.5) |
| Effective traits | `anti-ground` (×2), `side`, `rear`, `spread` |
| Credits | 14 (score 140 × chain) |
| Death | `small` burst; the hull flips and sinks with a splash |
| First level / used in | L13; Act 2 |
| Difficulty hooks | hard: 2-shot bursts |

### Behaviour

- Leaves a foam V-wake per the water rules; wakes show where skimmers are heading before they fire.

### Concept art

Chosen concept: [skimmer-r06-a.png](../concept/skimmer-r06-a.png), [skimmer-r06-a.gif](../concept/skimmer-r06-a.gif) (listed in the [naval](../README.md#concept-art) Concept art table).

## Implementation

- [ ] Sine weave along lanes, any-edge entry
- [ ] Foam wakes
- [ ] Stat block values loaded from data; global difficulty multipliers applied
- [ ] Death effect, bounty and score per this spec

## Decisions

- 2026-10-01: Promoted from the naval roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: HP rescaled to the lowered L13 reference DPS (8 → 5, ×76/120).
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
