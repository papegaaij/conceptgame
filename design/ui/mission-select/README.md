---
title: Mission select
design: review
implementation: done
art: n/a
depends-on: [../main-menu, ../debrief, ../../systems/scoring, ../../systems/saves, ../../systems/economy]
updated: 2026-10-04
---

# Mission select

## Summary

Replay a mission the current campaign has already flown (user decision 2026-10-04): the
campaign's levels act by act with their best grades, the flown ones open, every later one locked
with its name hidden. A replay is a free flight for a better grade: it earns nothing and leaves the
campaign where it is. This is the "level-select of a replay mode" that
[scoring](../../systems/scoring/README.md) keeps the best grade per level for.

## Design

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                     ┌──────────────────────────────────────┐                 │
│                     │            MISSION SELECT            │                 │
│                     │           REPLAY - MEDIUM            │                 │
│                     │ ──────────────────────────────────── │                 │
│                     │ ACT I  FIRST CONTACT ─────────────── │                 │
│                     │    01  BREAK AT DAWN               B │                 │
│                     │    02  SHIPYARD BURNING           A+ │                 │
│                     │  ► 03  SPORE DRIFT                 C │                 │
│                     │    04  ???                    LOCKED │                 │
│                     │    …                                 │                 │
│                     │ ACT II  ??? ──────────────────────── │                 │
│                     │ NO CREDITS EARNED - BEST GRADE KEPT  │                 │
│                     └──────────────────────────────────────┘                 │
│                   UP/DOWN SELECT    ENTER FLY    ESC BACK                    │
└──────────────────────────────────────────────────────────────────────────────┘
```

- **Where:** the main menu's **Missions** item ([main menu](../main-menu/README.md)), disabled
  while the current campaign has flown no mission. The current campaign is the most recent save
  of any slot, the one Continue loads. Not in the hangar: the docs suggest no place there, and the
  hangar's command bar has no room to spare.
- **The list:** every act the game has data for, in order, as a header ("ACT I  FIRST CONTACT")
  with its levels below ("01  BREAK AT DAWN" and the best grade on the right, "-" if none). A level
  is **open** when the campaign has flown it (its number is below the save's next level) and it is
  built. Every other level is **locked**: dimmed, "???" for its name and "LOCKED" on the right,
  the campaign's next level too. An act whose first level the campaign has not reached shows "???"
  for its name. The list scrolls (14 rows) with the kit's scroll markers.
- **Controls:** up / down move between open missions (locked ones are skipped), confirm flies the
  selected one, Back returns to the main menu. The cursor starts on the last flown mission. Drawn
  with the glass kit over the dimmed title scene; the title theme plays on.
- **A replay** (main-agent choice, for the user to confirm; the design had no replay mode before,
  [campaign](../../campaign/README.md) left it for later):
  - it flies at the campaign's difficulty with the campaign's **current loadout** and special
    charges, at **full armour** (no repair bill, since nothing of the flight is kept);
  - it never changes the campaign's **progress** or next level, its credits, score, kills, armour,
    charges or retries, and it is never autosaved;
  - it earns **no credits** (no farming, see [economy](../../systems/economy/README.md)); the
    [debrief](../debrief/README.md) shows a REPLAY section with "NO CREDITS BANKED", the balance
    and the score in place of the credits;
  - it **keeps a better grade**: a grade that beats the level's best is written into the save the
    replay was started from, and the debrief shows NEW BEST; nothing else of that save changes
    (its write time neither, so it stays the current campaign);
  - its retries are unlimited, also on hard, so a replay never ends in a game over; the
    [pause](../pause/README.md) menu's abort reads ABORT REPLAY and the mission failed screen's
    BACK TO MISSIONS, both returning here, as the debrief does after a won replay. Quit to main menu
    works as in the campaign.

## Implementation

- [x] `vanguard.content.campaign.Missions`: the act-by-act list (open, locked and hidden names,
      hidden act names, best grades) and the save with a better grade
- [x] `Campaign.replay`: a copy of the save's campaign for one flown level (full armour, unlimited
      retries) that banks nothing, keeps its progress, records the grade and cannot be saved;
      `GameServices.save` writes nothing for a replay
- [x] `SaveSlots.mostRecentEntry`: the current campaign with its slot
- [x] `vanguard.game.screen.MissionSelectScreen`, opened from the main menu's Missions item; the
      better grade written into the source slot after a won replay (`keepGrade`)
- [x] Debrief, pause and mission failed screens route a replay back to the mission select; the
      debrief shows no credits for a replay
- [x] Tests: `MissionsTest` (the list's locked and open levels and act names, a better grade only,
      a replay earns nothing and keeps the progress, only flown levels)
- [x] Checked in a game capture: the list over the title scene with Levels 01–03 flown

## Open questions

- The replay rules above are main-agent choices: the current loadout at full armour, no credits,
  no progress, the better grade kept, unlimited retries. Should a replay start with the briefing
  (today it starts the level at once), or offer the hangar to change the loadout for the replay?

## Decisions

- 2026-10-04 (user decision): a mission select to replay flown missions, reachable from the main
  menu; later missions and the names of later missions and unreached acts are hidden. The replay
  rules are main-agent choices (conservative: nothing kept but a better grade), for the user to
  confirm.
- 2026-10-04 (user decision): the replay rules are confirmed as built: the current loadout at full armour, no credits, progress or charges change, only a better best grade is kept, unlimited retries.
