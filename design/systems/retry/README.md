---
title: Retry
design: approved
implementation: in-progress
art: n/a
depends-on: [../../player/armor, ../../player/shields, ../difficulty, ../saves]
updated: 2026-10-04
---

# Retry

## Summary

When armour reaches zero the Stormhawk is destroyed and the level fails; a failed primary
objective fails it the same way. The player retries
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
   - **Quit to main menu**: progress since the last save is lost (confirmation). Back (Esc, the
     gamepad's back button) asks the same question from any item; Back again or No stays.
3. On hard, the screen shows the retries left (3 per level). The failure **uses its retry at
   once** and the autosave is written then, so quitting from this screen and continuing cannot
   give it back. With none left the campaign ends: game over screen, high-score entry, back to the
   main menu. A game over goes back to the save made in the hangar right before the level was
   last launched, with a fresh set of retries: the autosave written at that failure holds the
   hangar state of the last launch (loadout, inventory, credits, charges and armour as launched)
   and the level's full retries, so **Continue** opens the hangar before the level and never
   resumes it with no retry left. Load game still offers the manual saves.
4. From the pause menu, **Restart mission** and **Abort to hangar** each use a retry on hard too
   (otherwise aborting just before dying would be a free retry); with no retry left both are
   disabled. A restart writes the autosave like a failure; an abort opens the hangar, which
   autosaves.

### On a failed primary objective

Some levels fail without the ship dying: their primary objective fails (Level 04's convoy is
lost, Level 05's battery scrolls past alive). This mirrors a wreck, for every such level:

1. The failure is reported at the moment it happens. The ship does not explode and there is no
   slow motion; from then on nothing can hurt it. The music cuts to the failure sting, and the
   mission failed screen follows **3 s** later over the frozen level, as after a wreck.
2. The screen shows the level's own failure line when it has one (Level 04: Okafor's "The convoy
   is gone, Lancer. Pull back."), otherwise the default line.
3. Everything else is as on destruction: the same options, a hard-mode retry used at once with the
   autosave, the attempt's earnings lost and the level-start state restored.

### Boss checkpoint (easy and medium)

When the boss warning starts (a mid-boss's sting, as in Level 05), a checkpoint is recorded:
armour, shield, special charges, credits and score **at that moment**, and the level's
**objective tallies** (Level 05's destroyed batteries and *Scorched crater* kills), so a
secondary objective that was still reachable stays reachable. Dying during the boss offers
**Retry from boss** (besides the normal options). It restarts at the warning with an **empty
field** (no leftover enemies or bullets): the boss's entrance and its intro radio replay, and a
new input recording starts there. Retrying from the boss does not count as a hard-mode retry
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
  the player goes back to the pre-launch hangar save (above). Easy and medium retry without
  limit.
- **Armour on retry** is restored to its **level-start value, but at least 50 %** of maximum, so a
  save started on near-zero armour can never trap the player.

## Implementation

- [x] Snapshot of player state at level start (the campaign state, see the M3 part B1 decision)
- [x] Snapshot at the boss checkpoint with the objective tallies, and *Retry from boss* restarting at the warning on an empty field — M4 part E (the Gorgon Frigate, Level 05)
- [x] Mission failed screen with the three (four) options
- [x] Back (Esc, the gamepad's back button) opens the quit question (`MissionFailedExitTest`)
- [x] Hard-mode retry counter and game over
- [x] "Back to hangar" path that keeps the level as the next one
- [x] A failed primary objective fails the level like a wreck (no explosion, the level's failure line) — M4 part D, reused by Level 05

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
- 2026-10-02: Game over on hard rolls back to the pre-launch hangar save (user decision). Built
  without a new save file: the campaign keeps the gear of the last launch (`Campaign.launch()`,
  called as a level's sortie starts; nothing else changes until the level ends), and
  `Campaign.fail()` with no retry left returns to it with the level's retries renewed;
  `LevelScreen` writes the autosave at every failure as before, so at a game over the autosave
  becomes the hangar before the level and Continue (the newest save) opens it. The statistics
  (deaths, playtime) go on. Rejected: a separate pre-launch save file next to the failure
  autosave (a second autosave to keep in step, and Continue would need to tell them apart) and
  deleting the autosave at game over (Continue would fall back to an older manual save). Test:
  `CampaignTest.aGameOverAutosavesTheHangarBeforeTheLastLaunchWithTheRetriesRenewed` (game over →
  autosave → Continue: the level before, 3 retries, credits, loadout and armour as launched). The
  game over screen says Continue returns to the hangar before the mission. The open question is
  closed.
- 2026-10-02: M3 close-out (user decision): the level-start snapshot is done (the campaign state is it, part B1), so the item is split; the boss checkpoint's snapshot moves to M4 with the first boss. The document is done for M3.
- 2026-10-02: Production art, UI batch part U3: the mission failed screen shows Okafor's grim
  portrait (`tools/art/portraits.py`).
- 2026-10-03: Failed primary objective (main-agent choice, M4 part D): a level whose primary
  objective fails (Level 04's convoy, Level 05's batteries) fails like a wreck: the failure sting,
  the mission failed screen 3 s later with the level's own failure line, a retry, the attempt's
  earnings lost. Generic, so Level 05 reuses it. Chosen here: no explosion or slow motion, and the
  ship cannot be hurt during the 3 s.
- 2026-10-03: M4 part D step 2: a failed primary objective is generic (`Sortie.primaryFailed()`,
  the `PRIMARY_FAILED` event): the ship flies on and nothing can hurt it, the level cannot complete,
  the presentation runs the wreck's flow without the explosion and the slow motion, and the mission
  failed screen shows the level's `mission-failed` radio line (its speaker and portrait) instead of
  the default one. Level 04's convoy is the first to use it.
- 2026-10-03: M4 part E (user decision): the boss checkpoint also keeps the objective tallies, so
  the secondary stays reachable after a boss retry; *Retry from boss* restarts at the boss
  warning (Level 05: the mini-boss sting) on an empty field. "When the boss warning starts" and
  Level 05's "at the mini-boss sting" are the same moment.
- 2026-10-03: M4 part E: the boss checkpoint is built. `Sortie` records it on the step before the
  boss arrives (the tally, the objective tallies, the radio cues already played, the secrets'
  triggers, shield, armour and the armour lost so far, the special's charges, the wave and ground
  schedules' place, the ground scroll and the random state; all preallocated), and
  `retryFromBoss()` restarts there as a new attempt on an empty field, so the boss's arrival, its
  timed radio and the music replay; a retry from the level start drops it until the boss is reached
  again. The mission failed screen lists RETRY FROM BOSS first once the attempt reached the
  checkpoint, on easy and medium only; it uses no retry. Tests: `BossTest` (sim) and
  `BossLevelTest`.
- 2026-10-04: Back on the mission failed screen (user decision): Esc and the gamepad's back
  button open the Quit to main menu question from any item, as choosing Quit does; Back again or
  No stays on the screen. Test: `MissionFailedExitTest`.
