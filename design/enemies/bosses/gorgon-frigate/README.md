---
title: Gorgon Frigate
design: approved
implementation: not-started
art: chosen
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-01
---

# Gorgon Frigate

## Summary

The Act 1 mid-boss and the first multi-part enemy: a bone-white medusa-bell warship with three serpent necks whose cobra heads are turrets. Kill the heads one by one, then the crown opens over the lime core.

## Design

### Stat block

Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (a Pulse Cannon L1 shot does 2); bullet damage classes and speeds are defined in the balancing basis.

| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `air` |
| Size tier | `large` (mid-boss) |
| Size | bell ≈ 230 px; necks 5 segments each; total ≈ 300×320 px |
| Parts | bell (`armoured`), 3 necks of 5 `armoured` segments, 3 cobra heads (`destroyable` turrets), crown petals + core (`vital`, exposed in phase 3) |
| Orientation | bell 32 angles; neck segments and heads 32 angles |
| HP | head 220 each; core 840; total 1 500 (easy ×0.75 / hard ×1.3) |
| Armour / shield | bell and neck segments armoured |
| Speed | enters at 60 px/s, then `hover` with slow `sine` (amplitude 40 px, period 6 s) |
| Movement | holds the upper third; necks bend toward the player with lagged follow-through |
| Attack | see phases |
| Formations | carrier + escorts (Skitter streams between phases) |
| Weak points | heads' lime eyes (×1.5) and the core (lime, ×2) — boss weak points are always lime |
| Effective traits | `forward`, `piercing`, `spread` |
| Credits | heads 30 each, core 110 → 200 (≈ 15 % of the L05 budget, 1 311) |
| Death | `large` chained bursts across the bell, crown petals tear off, credit shower |
| First level / used in | L05 (mid-boss) |
| Difficulty hooks | hard: phase 2 head fires 5-shot bursts; core rings 16 bullets |

### Phases

| Phase | Ends at | Behaviour |
|---|---|---|
| 1 — Three heads | 100–60 % (until 1 head is left) | Each head fires a 3-shot `aimed` burst (0.15 s apart, 160 px/s, `small` = 4) in rotation, one head every 1.2 s. Skitter streams enter between bursts every 10 s. |
| 2 — Last head | until the last head dies | The remaining head is enraged: bursts every 0.8 s, 5-shot on hard; its neck lashes wider. |
| 3 — Core | core 840 → 0 | Crown petals open over the lime core; `ring` of 12 (110 px/s) every 2.0 s, alternating with a slow 3-arm `spiral` (3 s). Armour sparks on the bell remain. |

### Arena

Over the Vrell nest crater on Luna ([L05](../../../campaign/act-1-first-contact/level-05-crater-nest/README.md)), after the four nest batteries: the scroll slows to 30 px/s; no terrain collision. Target duration 45–75 s at medium.

### Behaviour

- The first boss-style health bar appears (mid-boss bars are shorter). A radio line introduces it.
- Destroyed heads leave smoking stumps that leak ichor; the neck goes limp.

### Concept art

Chosen concept: [gorgon-frigate-r06-a.png](../concept/gorgon-frigate-r06-a.png), [gorgon-frigate-r06-a.gif](../concept/gorgon-frigate-r06-a.gif) (listed in the [bosses](../README.md#concept-art) Concept art table).

## Implementation

- [ ] Neck chains with lagged follow-through and per-head HP
- [ ] Three phases with the listed thresholds
- [ ] Lime weak points; health bar; credit shower
- [ ] Stat block values loaded from data; global difficulty multipliers applied
- [ ] Death effect, bounty and score per this spec

## Decisions

- 2026-10-01: Promoted from the bosses roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: Arena set to the nest crater on Luna (L05), replacing the Earth–Moon convoy lane.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
