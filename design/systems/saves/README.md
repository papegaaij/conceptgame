---
title: Saves
design: approved
implementation: not-started
art: n/a
depends-on: [../../ui/main-menu, ../../ui/hangar]
updated: 2026-10-01
---

# Saves

## Summary

The game saves between levels only, in the hangar. There are 8 manual slots and 1 autosave
slot. "Load game" in the main menu lists them. There is no mid-level saving: a level is short
(4–7 minutes) and retries are free.

## Design

### When

- **Autosave**: every time the hangar opens (after a debrief, or after "back to hangar").
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
| `inventory` | Owned unfitted items with upgrade levels |
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

### Slot display

Each slot shows: act and level name, difficulty, credits, playtime, date, and a small icon for
the act.

## Implementation

- [ ] Save/load of the fields above with a version number
- [ ] Autosave on entering the hangar; 8 manual slots
- [ ] Continue = most recent save
- [ ] Slot list UI in load/save screens

## Decisions

- 2026-09-30: Save only in the hangar; 8 slots + autosave.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../reviews/acts-1-2/README.md).
