---
title: Retry
design: approved
implementation: in-progress
art: n/a
depends-on: [../../player/armor, ../../player/shields, ../difficulty, ../saves]
updated: 2026-10-02
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
- [x] Mission failed screen with the three (four) options
- [x] Hard-mode retry counter and game over
- [x] "Back to hangar" path that keeps the level as the next one

## Open questions

- Abort to hangar from the pause menu is "same as Back to hangar after a failure": does it use a
  hard-mode retry? Built: yes (like Restart), otherwise aborting just before dying would be a free
  retry; with no retry left, Restart and Abort are disabled.
- On hard, quitting to the main menu from the mission failed screen and continuing the autosave
  restores the retries the hangar visit had: progress since the last save is lost as the pause menu
  says, but so are the used retries. Accept, or store the retries outside the saves?
- From the ship's destruction to the mission failed screen: built as 3 s (the explosion, the 1 s
  slow motion and the start of the sting); the document gives no time.

## Decisions

- 2026-09-30: Armour bar and level retry; credits of the failed attempt are lost (user decision).
- 2026-10-01: Armour on retry: level-start value with a 50 % minimum. Game over only on hard after 3 retries.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../reviews/acts-1-2/README.md).
- 2026-10-01: M1 (no credits, menus or mission failed screen yet): at zero armour the ship explodes, the presentation runs a second of slow motion, the music cuts to the mission failed sting, and 4 s later the sortie restarts by itself with full shield and armour and the wave cycle from the beginning (`vanguard.sim.Sortie`). The 50 % armour floor and the level-start snapshot come with the campaign state in M3.
- 2026-10-02: M2: a destroyed ship restarts Level 01 from its launch with the attempt's
  credits, score, kills and radio discarded; armour and shield are still restored in full until the
  level-start snapshot comes with the campaign state (M3).
- 2026-10-02: M3 part B1. The campaign state (`vanguard.content.campaign.Campaign`) is the
  level-start snapshot: nothing in it changes during a level, so the credits, score, loadout and
  armour of a retry are those of the level start, and a won level banks its credits with the grade
  bonus. The armour floor is a number in [data.yaml](data.yaml) (`armour_floor: 0.5`). The
  simulation no longer restarts by itself: a destroyed ship waits (`Sortie.retry(armour)` starts
  the next attempt with the given armour; a wreck cannot complete the level), 3 s later the mission
  failed screen (mission-failed-r08-a) opens over the frozen level, tinted red: Okafor's portrait
  and line, Retry, Back to hangar, Quit to main menu (confirmation), what the attempt earned and, on
  hard, the retries left. Retry, Back to hangar, the pause menu's Restart and Abort each use a
  retry on hard and raise the armour to the floor; a failure with no retry left goes to the game
  over screen (the game over cue and the campaign's stats; back to the main menu, where the last
  save can be loaded). Not built: Retry from boss with the boss checkpoint (no boss yet), so the
  snapshot item stays open, and the game over screen's last transmission (no text yet) and top-10
  name entry. The Level 01 replay keeps its state hash (`e602b2264976076f`): the recorded run
  never loses the ship.
