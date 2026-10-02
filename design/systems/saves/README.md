---
title: Saves
design: approved
implementation: in-progress
art: n/a
depends-on: [../../ui/main-menu, ../../ui/hangar]
updated: 2026-10-02
---

# Saves

## Summary

The game saves between levels only, in the hangar. There are 8 manual slots and 1 autosave
slot. "Load game" in the main menu lists them. There is no mid-level saving: a level is short
(4–7 minutes) and retries are free.

## Design

### When

- **Autosave**: every time the hangar opens (after a debrief, or after "back to hangar"), when the
  player quits from the hangar to the main menu, and when an attempt fails or is restarted (so a
  used hard-mode retry is kept, see [retry](../retry/README.md)). At a game over (hard, no retry
  left) the autosave holds the hangar state the level was last launched with and a fresh set of
  retries, so Continue starts the level over from the hangar.
- **Manual save**: from the hangar's Save tab, into one of 8 slots (overwrite with
  confirmation).
- **Continue** in the main menu loads the most recent save of any kind.

### Contents

A versioned document (engine-agnostic; for example JSON):

| Field | Notes |
|---|---|
| `version` | Save format version, for migrations |
| `created`, `playtime` | Timestamp and total time played |
| `difficulty` | easy / medium / hard |
| `nextLevel` | Level number 1–50 (and act) |
| `credits`, `score` | |
| `loadout` | Fitted item per slot, with upgrade level |
| `inventory` | Owned unfitted items with upgrade levels, by kind (front, rear, wing, generator, shield, plating, engine, utility) |
| `unlocks` | Shop items unlocked (by act, data cores or story) |
| `specials` | Charges per special type |
| `armour` | Current armour (repair is not automatic) |
| `escort` | Rook hired, his weapon and level, his armour |
| `retriesLeft` | Hard mode only |
| `grades` | Best grade per completed level |
| `dataCores` | Collected data cores (lore) |
| `storyFlags` | Branch-free story state (e.g. Rook's status, twist revealed) |
| `stats` | Kills, deaths, accuracy (for the debrief and an eventual stats screen) |

Settings (audio, controls, display) are stored separately and are not part of a save.

### Files

JSON, one file per slot (`autosave.json`, `slot-1.json` … `slot-8.json`) in a `saves` directory
next to the settings file (in the platform's config directory, or next to the file `--settings`
names). A save is written next to its file and moved into place, so a crash never leaves half a
save. A file of a newer or unknown format version, or with a missing, unknown or invalid field,
is shown as unreadable and never half loaded; older versions are migrated when they exist.

### Slot display

Each slot shows: act and level name, difficulty, credits, playtime, date, and a small icon for
the act.

## Implementation

- [x] Save/load of the fields above with a version number
- [x] Autosave on entering the hangar; 8 manual slots
- [x] Continue = most recent save
- [x] Slot list UI in load/save screens
- [ ] `escort` field, with Rook (Act 2)

## Decisions

- 2026-09-30: Save only in the hangar; 8 slots + autosave.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../reviews/acts-1-2/README.md).
- 2026-10-02: M3 part B1 (`vanguard.content.campaign`: `Campaign`, `SaveGame`, `SaveFormat`,
  `SaveSlots`). Format version 1 with every field above except `escort`, which comes with Rook
  (Act 2, a new format version); `created` is the time the save was written, so Continue takes the
  newest file of any slot. Autosave whenever the hangar opens: after the intro briefing, every
  debrief, Back to hangar and Abort, but not right after a save was loaded (it would only rewrite
  the same state). Manual saves from the hangar's Save game item into slots 1–8 (overwrite after a
  confirmation); the slot list is the one of the main menu's Load game. Storage per *Files* above;
  the document did not say where, so the saves sit next to the settings. Tests: round trip, newer,
  unknown and missing versions, malformed files, a failed write leaving the old save untouched.
- 2026-10-02: M3 part B2: the inventory is saved by kind (`{"WING": [{"item": "autocannon-pod",
  "level": 1}], …}`), because the shields' and the engines' model names are the same (`Mk I`…);
  still format version 1, as B1's format was never released (a B1 test save with an empty
  inventory list reads as unreadable). The autosave is also written when the player quits from the
  hangar (the quit dialog promises the visit is kept), when an attempt fails and when it is
  restarted (user decision: the used retry must not come back by quitting). Test: a fitted loadout
  with inventory, a new plating and its armour survive the round trip.
- 2026-10-02: Game over (user decision): the failure's autosave holds the pre-launch hangar
  state with the level's retries renewed instead of the failed level with 0 retries, so Continue
  opens the hangar before the level; no extra slot or format change (see
  [retry](../retry/README.md)).
