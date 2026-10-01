---
title: Siege Spire
design: approved
implementation: not-started
art: chosen
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-01
---

# Siege Spire

## Summary

The Act 2 boss: a slate citadel rooted in Geneva Concord under the Vrell canopy. Root turrets and mortars protect a trunk whose maw launches Wraiths; when the roots die, the spire tears itself free and rises for a final airborne phase.

## Design

### Stat block

Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (a Pulse Cannon L1 shot does 2); bullet damage classes and speeds are defined in the balancing basis.

| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `ground` (roots, trunk) → `air` (freed spire, phase 3) |
| Size tier | `huge` (act boss) |
| Size | trunk ≈ 160 px; root spread ≈ 420 px; freed spire 200×260 px |
| Parts | 4 root turret pods (`destroyable`), 2 acid-mortar roots (`destroyable`), trunk (`armoured`), launch maw (`destroyable`, phase 2), spire core (`vital`, phase 3) |
| Orientation | turret pods 32 angles; spire 32 angles when airborne |
| HP | turret pod 370 each (1 480); mortar root 430 each (860); maw 1 600; spire core 2 750; total 6 690 (easy ×0.75 / hard ×1.3) |
| Armour / shield | trunk armoured; roots and maw are ground targets (all weapons, `anti-ground` ×2) |
| Speed | rooted; freed spire drifts 50 px/s |
| Movement | see phases |
| Attack | see phases |
| Formations | turret nest |
| Weak points | lime maw (×2, phase 2) and lime spire core (×2, phase 3) |
| Effective traits | `anti-ground` (roots ×2), `rear` (Wraiths), `piercing` |
| Credits | turret pods 60 each, mortar roots 60 each, maw 120, spire 240 → 720 (≈ 30 % of the L14 budget, 2 410) |
| Death | `huge`: the spire cracks open, chained explosions, the canopy above withers and light breaks through; credit shower |
| First level / used in | L14 |
| Difficulty hooks | hard: phase 3 laser-sweep 180°; maw launches 3 Wraiths |

### Phases

| Phase | Ends at | Behaviour |
|---|---|---|
| 1 — Roots | roots 2 340 → 0 | Turret pods fire `aimed` violet orbs (150 px/s, `medium` = 6) every 1.6 s each, staggered; mortar roots lob acid every 4 s (marker 1.0 s, 10-bullet ring). The scroll creeps at 15 px/s so the roots come into range in turn. |
| 2 — Maw | maw 1 600 → 0 | The claw crown opens; the maw launches 2 Wraiths every 8 s (they loop behind the player: `rear` matters) and spits a 9-bullet `fan` (spread 80°, 140 px/s) every 2.5 s. Destroyed roots stay dead. |
| 3 — Free spire | spire 2 750 → 0 | The spire tears free with a shockwave (debris on the ground layer) and rises to `air`; it drifts across the upper half firing a telegraphed `laser-sweep` (120° arc, 0.6 s telegraph, `laser` = 8) every 5 s and `ring`s of 14 in between. The withered stump remains as scenery. |

### Arena

Geneva Concord under the Vrell canopy (L14). Target duration 120–180 s at medium; at the effective boss DPS (0.6 × 80 = 48) the total HP lasts ≈ 139 s.

### Behaviour

- The boss teaches the Act 2 lessons in order: ground targets (roots), rear awareness (Wraiths), then dodging in the open.
- Wraiths launched here use their own spec (rust with blue-violet veins) and pay the normal
  [Wraith](../../air/wraith/README.md) bounty at the Act 2 factor (48 per kill), on top of the
  boss bounty; levels budget them at the expected count (see
  [economy](../../../systems/economy/README.md#sources)).

### Concept art

Chosen concept: [siege-spire-r06-a.png](../concept/siege-spire-r06-a.png), [siege-spire-r06-a.gif](../concept/siege-spire-r06-a.gif) (listed in the [bosses](../README.md#concept-art) Concept art table).

## Implementation

- [ ] Rooted phase with independent root parts
- [ ] Wraith launches and maw fan
- [ ] Tear-free transition to the air layer and the laser-sweep phase
- [ ] Stat block values loaded from data; global difficulty multipliers applied
- [ ] Death effect, bounty and score per this spec

## Decisions

- 2026-10-01: Promoted from the bosses roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: HP rescaled to the lowered L14 reference DPS (×80/130): turret pods 600 → 370, mortar roots 700 → 430, maw 2 600 → 1 600, spire core 4 500 → 2 750, total 10 900 → 6 690; duration ≈ 139 s (was ≈ 140 s). Bounty unchanged.
- 2026-10-01: Maw-launched Wraiths pay the normal Wraith bounty at the Act 2 factor, budgeted at the expected count as in L14 (user decision).
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
