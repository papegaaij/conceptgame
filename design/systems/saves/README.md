---
title: Saves
design: approved
implementation: in-progress
art: n/a
depends-on: [../../ui/main-menu, ../../ui/hangar]
updated: 2026-10-06
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
- **Debug runs never write a save**: a run started with a testing option (a level start, `--level`,
  `--loadout`, `--special`, `--escort`, `--act-end`, `--bench`, `--invulnerable`, `--debug-speed`)
  plays normally but writes neither the autosave nor a manual
  save, and never creates the `saves` directory; it still reads the saves (Continue, Load game).
  The hangar shows *AUTOSAVE OFF - DEBUG RUN* instead of *AUTOSAVED*, its quit dialog and the
  briefing's say *A DEBUG RUN WRITES NO SAVE.*, and the Save game screen shows *SAVING OFF IN A
  DEBUG RUN* and writes nothing when a slot is confirmed.

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
| `escort` | The escort slot (format version 3, M5 part A): `hired` (Rook has joined), `side` (`left`/`right`), `fitted` (the id of his fitted gun, none before he joins), `guns` (every gun he owns with its level, `{"item": "autocannon", "level": 1}`, the fitted one included) and `armour` (his current armour; repair is not automatic). See [wingmen](../../player/wingmen/README.md) |
| `retriesLeft` | Hard mode only |
| `grades` | Best grade per completed level |
| `dataCores` | Collected data cores (lore) |
| `storyFlags` | Branch-free story state (e.g. Rook's status, twist revealed) |
| `stats` | Kills, deaths, accuracy (for the debrief and an eventual stats screen); `levels`: per won level (by number) the credits it banked (earnings plus the grade bonus) and its kills, for the act summary of an act's last [debrief](../../ui/debrief/README.md) (format version 2, M4 part G) |

Settings (audio, controls, display) are stored separately and are not part of a save.

### Files

JSON, one file per slot (`autosave.json`, `slot-1.json` … `slot-8.json`) in a `saves` directory
next to the settings file (in the platform's config directory, or next to the file `--settings`
names). A save is written next to its file and moved into place, so a crash never leaves half a
save. A file of a newer or unknown format version, or with a missing, unknown or invalid field,
is shown as unreadable and never half loaded; older versions are migrated. Version 2 (M4 part
G) adds `stats.levels`; a version 1 save loads with no level recorded, and the act summary shows
what is recorded.

**Version 3** (M5 part A) adds `escort`. In a new campaign it reads
`{"hired": false, "side": "left", "fitted": null, "guns": [], "armour": 80}`; when the hangar opens
with Level 08 or later next, Rook is hired with the free Autocannon at L1 fitted and full armour
(`{"hired": true, …, "fitted": "autocannon", "guns": [{"item": "autocannon", "level": 1}],
"armour": 80}`). A version 2 save is migrated (version 1 first through 2): its `escort` is the new
campaign's, with Rook hired (Autocannon L1, full armour, left side) when its `nextLevel` is 8 or
later. The field is always present from version 3 on, and a version 3 save without it is
unreadable like any save with a missing field.

### Slot display

Each slot shows: act and level name, difficulty, credits, playtime, date, and a small icon for
the act.

## Implementation

- [x] Save/load of the fields above with a version number
- [x] Autosave on entering the hangar; 8 manual slots
- [x] Continue = most recent save
- [x] Slot list UI in load/save screens
- [x] Debug runs write no save: read-only save slots (`SaveSlots.readOnly`, chosen by
  `LaunchOptions.debugRun()`), so every autosave and manual save is skipped; tested in
  `DebugRunTest` and `SaveSlotsTest`
- [ ] `escort` field, with Rook (format version 3, migrated from version 2) — M5 part A
- [x] Per-level records in `stats.levels` (format version 2, migrated from version 1) for the act summary (M4 part G; `SaveFormat.VERSION` 2, `migrateFrom1`)

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
- 2026-10-02: M3 close-out (user decision): the `escort` field moves to M5 with Rook's escort slot; the document is done for M3.
- 2026-10-05: M4 part G (default stated with the user's D2): the act summary needs what each level
  brought, which the save did not keep (only campaign-wide kills and score). Each won level's
  banked credits and kills are recorded under `stats.levels` by level number; format version 2,
  with a migration from version 1 that records none, so old saves stay readable and the summary
  shows what is recorded. A new win of a level replaces its record.
- 2026-10-05: M4 part H docs reconciliation: `stats.levels` and the version 1 → 2 migration were
  built in part G; ticked. Only the `escort` field (M5) is open, so the document is `done` for M4.
- 2026-10-06: M5 part A (default stated with the user's decisions D2–D4, see
  [wingmen](../../player/wingmen/README.md#decisions)): format version 3 adds `escort` with `hired`,
  `side`, `fitted`, `guns` (each owned gun with its level, his escort inventory) and `armour`; a
  version 2 save gets Rook hired when its next level is 8 or later. Main-agent choices: the field is
  always present (not hired before Level 08) rather than absent, and the fitted gun is an id into
  `guns` rather than a separate entry, so a gun's level has one place. The document is
  `in-progress` again while M5 builds it.
- 2026-10-06: Debug runs never write saves (user decision): `--loadout` and `--special` went into
  the debug campaign's gear, so a hangar, failure, restart, game-over or act-end autosave after
  such a run overwrote the real autosave. Every debug run (`--level`, `--loadout`, `--special`,
  `--escort`, `--act-end`, `--start level`, `--bench`, `--invulnerable`, `--debug-speed`) now has
  read-only save slots: one flag of the run, so every write path (the hangar's autosave as it
  opens and at quit, a failure's and a restart's, the briefing's Escape, a manual save, a replay's
  grade) writes nothing. `--settings` and `--difficulty` alone are not debug options. The Save
  game screen still opens, says that saving is off and writes nothing (simpler than hiding the
  hangar's Save command).
