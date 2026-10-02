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

1. Death explosion, slow-motion for 1 s, music cuts to the failure sting. The mission failed
   screen follows **3 s** after the destruction (the explosion, the slow motion and the start of
   the sting).
2. **Mission failed** screen with Okafor's portrait and a short line
   ("Pull back, Lancer. Regroup and try again."). Options:
   - **Retry**: restart the level immediately with the level-start state.
   - **Back to hangar**: return to the hangar with the level-start state, to change the
     loadout. Purchases there are normal purchases. Then launch again.
   - **Quit to main menu**: progress since the last save is lost (confirmation).
3. On hard, the screen shows the retries left (3 per level). The failure **uses its retry at
   once** and the autosave is written then, so quitting from this screen and continuing cannot
   give it back. With none left the campaign ends: game over screen, high-score entry, back to the
   main menu, where the last save can be loaded.
4. From the pause menu, **Restart mission** and **Abort to hangar** each use a retry on hard too
   (otherwise aborting just before dying would be a free retry); with no retry left both are
   disabled. A restart writes the autosave like a failure; an abort opens the hangar, which
   autosaves.

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

- On hard, the game over comes from an attempt with no retry left; the autosave written at the
  failure before it holds 0 retries, so Continue after a game over gives that level's last
  attempt again (before B2 it gave all three back). Delete or mark the autosave at game over, or
  accept "reload the last save" as it is?

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
- 2026-10-02: User decisions: Abort to hangar uses a hard-mode retry (as built); the used retries
  are written to the autosave the moment an attempt fails (`Campaign.fail()` uses the retry,
  `LevelScreen` autosaves; the pause menu's restart autosaves too); 3 s from the destruction to
  the mission failed screen. The three open questions are closed.
