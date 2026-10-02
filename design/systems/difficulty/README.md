---
title: Difficulty
design: approved
implementation: in-progress
art: n/a
depends-on: [../economy, ../retry]
updated: 2026-10-02
---

# Difficulty

## Summary

Easy, medium and hard are chosen when a new game starts and cannot be changed during the
campaign. They scale enemy toughness and bullet patterns, income, repair costs and the number of
retries. The levels themselves (layouts, enemy types) are the same on every difficulty.

## Design

<!-- data: difficulty -->
| Lever | Easy | Medium | Hard |
|---|---|---|---|
| Enemy HP | × 0.75 | × 1.0 | × 1.3 |
| Enemy bullet speed | × 0.8 | × 1.0 | × 1.15 |
| Enemy fire rate | × 0.7 | × 1.0 | × 1.3 |
| Bullets per `fan` / `ring` / `burst` | −25 % (at least −1) | base | +25 % (at least +1) |
| Aimed shots | at the player's current position, ±4° spread | at the player's current position | selected enemies lead the target |
| Formation size | −20 % | base | +20 % |
| Bullet budget (max enemy bullets on screen) | 60 | 120 | 200 |
| Player shield regen | × 1.25 | × 1.0 | × 1.0 |
| Credit income | × 1.25 | × 1.0 | × 0.9 |
| Score multiplier | × 0.75 | × 1.0 | × 1.5 |
| Armour repair in hangar | free | 5 cr / point | 10 cr / point |
| Retries per level | unlimited | unlimited | 3, then game over (reload last save) |
| Boss checkpoint | yes | yes | no |
| Intel | sensor level +1 | normal | normal |
<!-- /data -->

**Rounding.** Enemy HP on easy and hard is the medium HP times the lever, rounded half to even
(2.5 → 2, 3.5 → 4, 5.2 → 5) and at least 1; the easy/hard HP in the enemy stat blocks follow the
same rule.

**Aimed-shot spread.** Each aimed shot leaves at a uniformly random angle within ± the spread of
its aim (easy ±4°; medium and hard 0°, exact). The random angle comes from the level's seeded
random generator, so a replay fires the same shots. On hard, selected enemies (stat-block hooks)
also lead the target: they aim where the player will be when the shot arrives.

**Formation size.** A wave's count is count × (1 ± 20 %), rounded half to even and at least 1,
unless the level authors the count for that difficulty (Level 01's hard rear wave of 10 replaces
the lever). Likewise an authored `burst` for a difficulty is final and is not raised again by the
bullets-per-burst lever.

Density scaling uses **authored variants** where it matters: a pattern designer can mark
bullets as `medium+` or `hard-only`, rather than relying on a multiplier alone. This table is
the single source of the global levers; enemy stat blocks only add overrides (see
[enemies](../../enemies/README.md#difficulty-scaling-hooks)).

## Implementation

- [x] Difficulty stored in the save and applied through a single table
- [ ] Per-bullet difficulty tags in pattern data
- [x] Easy-mode intel bonus

## Open questions

- None open.

## Decisions

- 2026-09-30: Difficulty is fixed per campaign.
- 2026-09-30: Merged with the enemy scaling hooks: this table is the single source. Kept these
  values for HP and bullet speed; added bullets per pattern, formation size and bullet budget
  from enemies; aimed shots do not lead the player on medium (enemies' proposal).
- 2026-10-01: Game over only on hard, after 3 failed retries of a level (then reload a save); easy and medium have unlimited retries.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../reviews/acts-1-2/README.md).
- 2026-10-02: The levers moved into [data.yaml](data.yaml) (M2 data files); the table is rendered from it, and the enemy stat blocks derive their easy/hard HP from it.
- 2026-10-02: HP rounding for the difficulty multipliers (user decision): round half to even, at
  least 1, which matches every stat block.
- 2026-10-02: M2: the levers are applied when a level is built (`vanguard.content.SimSpecs`):
  enemy HP, fire rate (interval ÷ factor), bullet speed, formation size, bullet budget, shield
  regen, credit income and score; the stat blocks' hard hooks (circle Needlers lead the target).
  The difficulty comes from the `--difficulty` launch option until the new-game menu (M3); not
  stored in a save yet, so the first item stays open.
- 2026-10-02: Aimed-shot spread (user decision): easy ±4°, medium 0°, hard 0° plus target
  leading by selected enemies; a uniform random offset from the level's seeded generator
  (`aimed_spread_degrees` in [data.yaml](data.yaml), applied by `vanguard.sim.Sortie`).
- 2026-10-02: Formation size (user decision): count × (1 ± 20 %), rounded half to even, at least
  1, unless a level authors the count for that difficulty; an authored `burst` is not raised again
  by the bullets-per-burst lever.
- 2026-10-02: M3 part A: the difficulty is chosen in the new-game difficulty select
  ([main menu](../../ui/main-menu/README.md)) and the level is built at it; `--difficulty` stays
  for testing (the bench flies it, the select starts on it). It is not in a save yet (part B), so
  the first item stays open.
- 2026-10-02: M3 part B1: the difficulty is part of the campaign state and its saves; the level is
  built at the campaign's difficulty. Hard's 3 retries per level are counted by the campaign (see
  [retry](../retry/README.md)).
- 2026-10-02: M3 part B2: the hangar charges the repair cost per armour point and adds the sensor
  bonus (easy +1) to the intel's sensor level.
- 2026-10-02: "Reload last save" after hard's game over means the save made in the hangar right
  before the level was launched, with a fresh set of retries (user decision): Continue opens the
  hangar before the level; see [retry](../retry/README.md).
