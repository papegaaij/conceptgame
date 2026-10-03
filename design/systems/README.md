---
title: Game systems
design: approved
implementation: not-started
art: n/a
depends-on: [../player]
updated: 2026-10-03
---

# Game systems

## Summary

The rules around the action: how credits are earned and spent, how score and grades work,
what the difficulty levels change, what happens when the ship is destroyed, and how the game is
saved.

## Contents

| Part | Summary | Design | Impl | Art |
|---|---|---|---|---|
| [economy](economy/README.md) | Credits: sources, sinks, pricing curve, sell-back, per-level budget | approved | done | n/a |
| [scoring](scoring/README.md) | Score (separate from credits), chain multiplier, level-end bonuses, grades | approved | in-progress | n/a |
| [difficulty](difficulty/README.md) | What easy, medium and hard change | approved | done | n/a |
| [retry](retry/README.md) | Failure model: ship destroyed or primary objective failed → retry the level | approved | in-progress | n/a |
| [saves](saves/README.md) | Save slots, autosave, save contents | approved | done | n/a |

## Design

Campaign loop:

```
Main menu ─► New game (difficulty) ─► Intro briefing
                                         │
   ┌─────────────────────────────────────┘
   ▼
 Briefing (story) ─► Hangar (intel, shop, loadout, repair, save) ─► Level
   ▲                                                                 │
   │                         destroyed ◄─────────────────────────────┤
   │                           │ retry / back to hangar              │ completed
   │                           ▼                                     ▼
   └───────────────────────── Debrief (score, credits, grade) ◄──────┘
```

## Decisions

- 2026-09-30: Loop order is briefing → hangar → level → debrief, so the story sets up the
  mission before the player equips for it.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../reviews/acts-1-2/README.md).
