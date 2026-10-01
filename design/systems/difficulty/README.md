---
title: Difficulty
design: draft
implementation: not-started
art: n/a
depends-on: [../economy, ../retry]
updated: 2026-10-01
---

# Difficulty

## Summary

Easy, medium and hard are chosen when a new game starts and cannot be changed during the
campaign. They scale enemy toughness and bullet patterns, income, repair costs and the number of
retries. The levels themselves (layouts, enemy types) are the same on every difficulty.

## Design

| Lever | Easy | Medium | Hard |
|---|---|---|---|
| Enemy HP | × 0.75 | × 1.0 | × 1.3 |
| Enemy bullet speed | × 0.8 | × 1.0 | × 1.15 |
| Enemy fire rate | × 0.7 | × 1.0 | × 1.3 |
| Bullets per `fan` / `ring` / `burst` | −25 % (at least −1) | base | +25 % (at least +1) |
| Aimed shots | at the player's current position, slight spread | at the player's current position | selected enemies lead the target, tighter spread |
| Formation size | −20 % | base | +20 % |
| Bullet budget (max enemy bullets on screen) | 60 | 120 | 200 |
| Player shield regen | × 1.25 | × 1.0 | × 1.0 |
| Credit income | × 1.25 | × 1.0 | × 0.9 |
| Score multiplier | × 0.75 | × 1.0 | × 1.5 |
| Armour repair in hangar | free | 5 cr / point | 10 cr / point |
| Retries per level | unlimited | unlimited | 3, then game over (reload last save) |
| Boss checkpoint | yes | yes | no |
| Intel | sensor level +1 | normal | normal |

Density scaling uses **authored variants** where it matters: a pattern designer can mark
bullets as `medium+` or `hard-only`, rather than relying on a multiplier alone. This table is
the single source of the global levers; enemy stat blocks only add overrides (see
[enemies](../../enemies/README.md#difficulty-scaling-hooks)).

## Implementation

- [ ] Difficulty stored in the save and applied through a single table
- [ ] Per-bullet difficulty tags in pattern data
- [ ] Easy-mode intel bonus

## Open questions

- None open.

## Decisions

- 2026-09-30: Difficulty is fixed per campaign.
- 2026-09-30: Merged with the enemy scaling hooks: this table is the single source. Kept these
  values for HP and bullet speed; added bullets per pattern, formation size and bullet budget
  from enemies; aimed shots do not lead the player on medium (enemies' proposal).
- 2026-10-01: Game over only on hard, after 3 failed retries of a level (then reload a save); easy and medium have unlimited retries.
