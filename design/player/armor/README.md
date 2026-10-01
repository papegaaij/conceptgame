---
title: Armour
design: review
implementation: not-started
art: none
depends-on: [../shields, ../../systems/retry, ../../systems/difficulty]
updated: 2026-09-30
---

# Armour

## Summary

Armour is the Stormhawk's hull points. It does not regenerate during a level (except through
auto-repair or armour patches). When it reaches zero the ship is destroyed and the level is
[retried](../../systems/retry/README.md).

## Design

| Plating level | Max armour | Price (first draft) | Available |
|---|---|---|---|
| Standard | 60 | starter | start |
| Composite I | 80 | 600 | act 1 |
| Composite II | 100 | 1 400 | act 2 |
| Composite III | 120 | 3 000 | act 3 |
| Composite IV | 140 | 6 000 | act 5 |
| Composite V | 160 | 11 000 | act 6 |

- Plating draws no power and has no speed penalty. Its price is the only cost.
- **Repairs** happen between levels in the hangar. Cost per point depends on difficulty (easy:
  free, medium: 5 cr, hard: 10 cr). See [difficulty](../../systems/difficulty/README.md).
  Repairing is optional: flying a level damaged is a legitimate way to save credits.
- In-level sources: armour patches (+10) and the [auto-repair module](../systems/README.md).
- Low-armour warnings at 30 % (smoke, beeping) and 15 % (sparks, faster beep, radio line).

## Implementation

- [ ] Armour value, max per plating level, damage and death at 0
- [ ] Hangar repair with difficulty-dependent cost, "repair all" button
- [ ] Low-armour warnings (visual, audio, radio)

## Decisions

- 2026-09-30: Plating has no downside besides price; the interesting trade-off is repair
  cost vs saving credits.
