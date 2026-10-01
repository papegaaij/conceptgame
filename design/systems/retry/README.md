---
title: Retry
design: review
implementation: not-started
art: n/a
depends-on: [../../player/armor, ../../player/shields, ../difficulty, ../saves]
updated: 2026-10-01
---

# Retry

## Summary

When armour reaches zero the Stormhawk is destroyed and the level fails. The player retries
it with the state they had when it started: the loadout, credits, score, armour and special
charges are all restored. Whatever was earned in the failed attempt is lost. There are no lives.

## Design

### On destruction

1. Death explosion, slow-motion for 1 s, music cuts to the failure sting.
2. **Mission failed** screen with Okafor's portrait and a short line
   ("Pull back, Lancer. Regroup and try again."). Options:
   - **Retry**: restart the level immediately with the level-start state.
   - **Back to hangar**: return to the hangar with the level-start state, to change the
     loadout. Purchases there are normal purchases. Then launch again.
   - **Quit to main menu**: progress since the last save is lost (confirmation).
3. On hard, the screen shows the retries left (3 per level). With none left the
   campaign ends: game over screen, high-score entry, back to the main menu, where the last save
   can be loaded.

### Boss checkpoint (easy and medium)

When the boss warning starts, a checkpoint is recorded: armour, shield, special charges,
credits and score **at that moment**. Dying during the boss offers **Retry from boss**
(besides the normal options). Retrying from the boss does not count as a hard-mode retry
(there are no boss checkpoints on hard anyway).

### What is restored

| State | On retry |
|---|---|
| Loadout, inventory | Level-start state |
| Credits, score | Level-start values (attempt earnings discarded) |
| Armour | Level-start value (not full, unless it was full) |
| Special charges | Level-start count |
| Rook | Back in formation with his level-start armour |

### Settled rules

- **Game over** exists only on **hard**: after 3 failed retries of a level the campaign ends and
  the player reloads a save. Easy and medium retry without limit.
- **Armour on retry** is restored to its **level-start value, but at least 50 %** of maximum, so a
  save started on near-zero armour can never trap the player.

## Implementation

- [ ] Snapshot of player state at level start (and at boss checkpoint)
- [ ] Mission failed screen with the three (four) options
- [ ] Hard-mode retry counter and game over
- [ ] "Back to hangar" path that keeps the level as the next one

## Open questions

- None open.

## Decisions

- 2026-09-30: Armour bar and level retry; credits of the failed attempt are lost (user decision).
- 2026-10-01: Armour on retry: level-start value with a 50 % minimum. Game over only on hard after 3 retries.
